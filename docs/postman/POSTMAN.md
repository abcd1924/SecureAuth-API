# Postman Collection - SecureAuthAPI

This folder contains an import-ready Postman setup for local API testing.

## Files

- `SecureAuthAPI.postman_collection.json`
- `SecureAuthAPI.postman_environment.json`

## Import Steps

1. Open Postman.
2. Import `SecureAuthAPI.postman_collection.json`.
3. Import `SecureAuthAPI.postman_environment.json`.
4. Select the `SecureAuth - Local` environment.

## Recommended Execution Order

1. `Authentication > register` (optional if user already exists)
2. `Authentication > login`
3. `User Profile > Get Me`
4. `Authentication > refresh` (token rotation check)
5. `Admin Management > List Users` (ADMIN token required)

## Authentication Workflow

- `login` and `refresh` requests automatically store `access_token` and `refresh_token` in environment variables.
- Protected routes use Bearer token `{{access_token}}`.

## Setup and Testing Helpers

The `Setup and Testing` folder includes helper requests to validate security behavior:

- `Get Me - Unauthorized` expects `401`
- `List Users - Forbidden` expects `403` when using a non-admin token

## Environment Variables

Required variables in the environment:

- `baseUrl`
- `test_email`
- `test_password`
- `access_token`
- `refresh_token`
- `user_id`
- `admin_user_id`
- `role_to_assign`
- `active_status`

## Troubleshooting

- If protected requests return `401`, run `Authentication > login` again.
- If admin requests return `403`, log in with an ADMIN account.
- Ensure API is running on `http://localhost:8080` or update `baseUrl`.
