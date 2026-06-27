# Money Tracker — Backend

A modular Spring Boot 4 (Java 21) backend for a personal money-management app.
Auth uses **opaque session tokens** backed by server-side signed JWTs, so the
client never holds the JWT itself.

## Auth model — "session only to the frontend"

```
register/login
   │
   ▼
AuthService ──► JwtService.issue()      mints a signed HS256 JWT (userId, email, role, exp)
   │                                     ── the JWT NEVER leaves the server
   ▼
SessionService.createSession()
   • generates a random 256-bit opaque token
   • stores SHA-256(token) + the JWT in the `sessions` table
   • returns ONLY the opaque token to the client
   │
   ▼
client stores token in expo-secure-store, sends:  Authorization: Bearer <token>
   │
   ▼
SessionAuthenticationFilter
   • SHA-256 the token, look up the session
   • check not revoked / not expired
   • verify the stored JWT signature  ──► populate SecurityContext with AuthPrincipal
```

Benefits: JWT claims stay private, sessions are **revocable** (logout / revoke-all),
and it works cleanly on React Native where cookie handling is unreliable.

### Refresh tokens

Login/register also return a long-lived **refresh token** (opaque, hashed,
30-day cap). `POST /api/auth/refresh` swaps it for a new short-lived session
(60 min) **and rotates the refresh token** (single-use). Replaying an
already-rotated token is treated as theft and **revokes the user's whole refresh
family** ([RefreshTokenService](src/main/java/com/codewithsubhra/money_tracker_backend/security/refresh/RefreshTokenService.java) +
[RefreshFamilyRevoker](src/main/java/com/codewithsubhra/money_tracker_backend/security/refresh/RefreshFamilyRevoker.java),
which commits the revocation in its own transaction so the rejection can't undo it).

## Module layout

```
common/        BaseEntity, ApiResponse envelope, exceptions + global handler
security/      JwtService, Session entity/service, auth filter, SecurityConfig, CORS
user/          User entity, Role, repository/service
auth/          register / login / logout / me
account/       money accounts (cash, bank, card, …) with running balance
category/      income/expense categories (defaults seeded on register)
transaction/   income/expense entries; keep account balances in sync; search + summary
```

Every domain query is scoped by the authenticated user id, so users can only ever
read or mutate their own data.

## Running locally

```bash
./mvnw spring-boot:run          # dev profile (H2 in-memory, H2 console at /h2-console)
./mvnw test                     # context + tests
./mvnw -DskipTests package      # build jar -> target/*.jar
```

**Interactive API docs (Swagger UI):** with the app running, open
<http://localhost:8080/swagger-ui.html>. Raw spec: `/v3/api-docs`. Click
**Authorize**, paste the `sessionToken` from a login/register response, and call
any endpoint from the browser.

Profiles: `dev` (default, H2) and `prod` (Postgres). Override prod via env:
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and **always** set `APP_JWT_SECRET`
(≥ 32 bytes) and `APP_CORS_ORIGINS`.

## API

All responses use the envelope `{ success, data, error, timestamp }`.
Protected endpoints require `Authorization: Bearer <sessionToken>`.

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| POST | `/api/auth/register` | no | Create account, returns session + refresh tokens |
| POST | `/api/auth/login` | no | Returns session + refresh tokens |
| POST | `/api/auth/refresh` | no | Rotate refresh token → new session + refresh |
| POST | `/api/auth/logout` | yes | Revoke current session (and refresh, if sent) |
| GET  | `/api/auth/me` | yes | Current user |
| GET/POST | `/api/accounts` | yes | List / create accounts |
| GET/PUT/DELETE | `/api/accounts/{id}` | yes | Read / update / delete |
| POST | `/api/accounts/{id}/archive?archived=` | yes | Archive toggle |
| GET/POST | `/api/categories` | yes | List (`?type=INCOME\|EXPENSE`) / create |
| PUT/DELETE | `/api/categories/{id}` | yes | Update / delete |
| GET | `/api/transactions` | yes | Paged search (`accountId,type,from,to,page,size`) |
| GET | `/api/transactions/summary` | yes | Income/expense/net for a date range |
| GET/POST | `/api/transactions` | yes | List / create (adjusts account balance) |
| GET/PUT/DELETE | `/api/transactions/{id}` | yes | Read / update / delete |

### Quick start

```bash
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"you@mail.com","password":"supersecret1","displayName":"You"}'
# -> copy data.sessionToken, then:
curl localhost:8080/api/auth/me -H "Authorization: Bearer <sessionToken>"
```

## Notes for the Expo client

- Store **both** `sessionToken` and `refreshToken` in `expo-secure-store`, not AsyncStorage.
- Send the session as `Authorization: Bearer <sessionToken>` on every request.
- On `401`, call `/api/auth/refresh` once with the refresh token, save the new
  pair, and retry. If the refresh also fails, clear both tokens and route to login.
