package backend.secureauthapi.security.filter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import backend.secureauthapi.exception.ErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PREFIX = "AUTH:";
    private static final String API_PREFIX = "API:";

    private final Cache<String, Bucket> cache;
    private final ObjectMapper objectMapper;

    @Value("${rate-limit.auth.requests-per-minute:10}")
    private Integer authLimitPerMinute;

    @Value("${rate-limit.api.requests-per-minute:60}")
    private Integer apiLimitPerMinute;

    public RateLimitFilter(Cache<String, Bucket> rateLimitCache, ObjectMapper objectMapper) {
        this.cache = rateLimitCache;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String clientIp = extractClientIp(request);
        String requestPath = request.getRequestURI();
        boolean authEndpoint = requestPath.startsWith("/api/auth/");

        int limitPerMinute = authEndpoint ? authLimitPerMinute : apiLimitPerMinute;
        String keyPrefix = authEndpoint ? AUTH_PREFIX : API_PREFIX;

        String cacheKey = keyPrefix + clientIp;
        Bucket bucket = cache.get(cacheKey, key -> createNewBucket(limitPerMinute));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        addRateLimitHeaders(response, limitPerMinute, probe);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .errorCode("RATE_LIMIT_EXCEEDED")
                .message("Too many requests. Please try again later.")
                .path(requestPath)
                .timestamp(Instant.now())
                .build();

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    private void addRateLimitHeaders(HttpServletResponse response, int limitPerMinute, ConsumptionProbe probe) {
        long waitSeconds = Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));

        response.setHeader("RateLimit-Limit", String.valueOf(limitPerMinute));
        response.setHeader("RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
        response.setHeader("RateLimit-Reset", String.valueOf(waitSeconds));

        if (!probe.isConsumed()) {
            response.setHeader("Retry-After", String.valueOf(waitSeconds));
            response.setHeader("X-RateLimit-Reset", String.valueOf(waitSeconds));
        }
    }

    private Bucket createNewBucket(int requestsPerMinute) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(requestsPerMinute)
                .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isBlank()) {
            return xForwarded.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }
}
