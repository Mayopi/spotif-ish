# Auth, Session, and Security Patterns

## Auth Flows
Two separate Google flows are intentionally used:

1. Sign-in flow (`GoogleSignInProvider`):
- Uses Credential Manager.
- Retrieves Google ID token.
- Sends ID token to backend `POST /v1/auth/google`.

2. Drive grant flow (`GoogleDriveAuthorizationProvider`):
- Uses Google Identity AuthorizationClient.
- Retrieves server auth code (`drive.readonly` scope).
- Sends code to backend `POST /v1/drive/connect`.

Do not merge these flows; they solve different backend contracts.

## Session Ownership
Session lifecycle is centralized in:

- `data/auth/AuthRepository`
- `data/auth/SessionStore`

Rules:

1. Only `AuthRepository` should call auth endpoints directly.
2. Session persistence must go through `SessionStore`.
3. Session state exposed as `StateFlow<Session?>` for reactive auth gating.
4. On refresh failure, clear session and force sign-in.

## Token Transport Rules
Two token injection paths exist:

- API calls: `AuthInterceptor` adds `Authorization: Bearer <accessToken>`.
- Media stream playback: player module reads token through `PlaybackTokenSource` and sets HTTP request headers in Media3 datasource.

This avoids cross-module auth coupling while keeping playback authenticated.

## Storage Rules
Session persistence expectations:

- Default: `EncryptedSharedPreferences` with `MasterKey`.
- Fallback: plain shared prefs when keystore fails (startup resilience path).
- Keep in-memory mirror (`MutableStateFlow`) as single runtime source.

Never store raw tokens in logs, UI state, or plain text files.

## Endpoint Auth Rules
Use `@NoAuth` for endpoints that cannot include bearer auth:

- `/v1/auth/google`
- `/v1/auth/refresh`

Interceptor behavior:

- Skip `@NoAuth` requests.
- Attach bearer for others when session exists.
- On `401`, refresh once and replay original request.

## Do / Don’t
Do:

- Keep auth behavior in dedicated providers/repositories.
- Validate required BuildConfig secrets (`GOOGLE_WEB_CLIENT_ID`) before auth operations.
- Clear session on irrecoverable auth failures.

Don’t:

- Call auth endpoints directly from ViewModel/screen.
- Share token handling logic across unrelated modules.
- Assume expiry timestamps are authoritative; rely on 401 refresh path.

## Best-Practice Upgrades
Recommended next improvements:

1. Add telemetry hooks for refresh failure reasons (without token leakage).
2. Add proactive refresh window (optional) to reduce first 401 latency.
3. Add encrypted migration/repair workflow for corrupted secure storage scenarios.
