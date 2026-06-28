# Money Tracker — API Documentation

Base URL (dev): `http://localhost:8080`
All request/response bodies are JSON. All dates are ISO-8601.

> **Interactive docs:** run the app and open **`/swagger-ui.html`** to try every
> endpoint live (raw spec at `/v3/api-docs`). Click **Authorize** and paste a
> `sessionToken` to call protected routes. This file is the prose companion.

---

## 1. How authentication works

This API uses **opaque tokens**, not raw JWTs on the client. There are two:

| Token | Lifetime (default) | Purpose | Sent as |
|-------|--------------------|---------|---------|
| `sessionToken` | **60 min** (`APP_SESSION_TTL_MINUTES`) | Short-lived; authorizes API calls | `Authorization: Bearer <sessionToken>` |
| `refreshToken` | **30 days** (`APP_REFRESH_TTL_DAYS`) | Long-lived; obtains a new session without re-login | JSON body of `/api/auth/refresh` |

**Flow**

1. `register` / `login` → returns both tokens. Internally a signed JWT is minted
   and stored in a `sessions` row; the JWT itself never leaves the server.
2. Call protected endpoints with `Authorization: Bearer <sessionToken>`.
3. When a call returns `401`, POST the `refreshToken` to `/api/auth/refresh`.
   You get a **new** `sessionToken` **and a new** `refreshToken`.
4. `logout` revokes the session (and the refresh token if you send it).

**Refresh-token rotation + theft detection.** Each refresh token is single-use.
On `/api/auth/refresh` the old one is revoked and a new one issued. If a token
that was *already rotated away* is presented again (e.g. stolen and replayed),
the server treats it as compromise and **revokes the user's entire refresh-token
family** — both the attacker's and the victim's refresh tokens stop working, and
re-login is required. The refresh family also has an absolute cap: rotation keeps
the original 30-day expiry, so a chain can't be renewed forever.

**Client storage:** keep **both** tokens in `expo-secure-store`. On `401`, try
one refresh; if that also fails, clear both and route to login.

---

## 2. Response envelope

Every endpoint returns the same shape.

**Success**
```json
{
  "success": true,
  "data": { /* endpoint-specific */ },
  "timestamp": "2026-06-27T08:34:08.353Z"
}
```

**Error**
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": { "email": "must be a well-formed email address" }
  },
  "timestamp": "2026-06-27T08:34:09.444Z"
}
```

### Error codes

| HTTP | `error.code` | When |
|------|--------------|------|
| 400 | `VALIDATION_ERROR` | Body failed bean validation (`details` = field → message) |
| 400 | `BAD_REQUEST` | Business rule violation (e.g. email already registered) |
| 401 | `UNAUTHORIZED` | Missing/expired/revoked token, or bad credentials |
| 403 | `FORBIDDEN` | Authenticated but not allowed |
| 404 | `NOT_FOUND` | Resource missing or not owned by you |
| 500 | `INTERNAL_ERROR` | Unexpected server error |
| 503 | `FX_UNAVAILABLE` | The exchange-rate provider couldn't be reached (currency endpoints) |

> **Ownership = 404.** Every resource is scoped to the logged-in user. Asking for
> someone else's account/transaction returns `404`, never another user's data.

---

## 3. Auth endpoints

### POST `/api/auth/register` — _public_
Creates a user, seeds 9 default categories, and opens a session.

Request:
```json
{ "email": "you@mail.com", "password": "supersecret1", "displayName": "You" }
```
Rules: valid email; password 8–100 chars; displayName 1–80 chars.

Response `201`:
```json
{
  "success": true,
  "data": {
    "sessionToken": "N5ze6NT_azL2w4j-Zvdt5hMQMFmK68lz3y56pHNiYKU",
    "expiresAt": "2026-06-27T09:34:08Z",
    "refreshToken": "Y0ogFbdvEQOga0jG-DtzKdjpZZfwYYT5LeoSbqZDG1g",
    "refreshExpiresAt": "2026-07-27T08:34:08Z",
    "user": { "id": "0c9c…", "email": "you@mail.com",
              "displayName": "You", "baseCurrency": "USD", "role": "USER" }
  }
}
```

### POST `/api/auth/login` — _public_
```json
{ "email": "you@mail.com", "password": "supersecret1" }
```
Response `200`: same `data` shape as register. Bad credentials → `401 UNAUTHORIZED`
(same message whether the email or the password is wrong, by design).

### POST `/api/auth/refresh` — _public_
Exchanges a refresh token for a fresh session and a rotated refresh token. The
expired/old `sessionToken` is **not** needed here — the refresh token is the credential.
```json
{ "refreshToken": "Y0ogFbdvEQOga0jG-DtzKdjpZZfwYYT5LeoSbqZDG1g" }
```
Response `200`: same shape as login but `user` is `null`. Errors → `401 UNAUTHORIZED`:
- `"Invalid refresh token"` — unknown token
- `"Refresh token has expired"` — past its 30-day cap
- `"Refresh token has already been used"` — replayed/rotated token; **the whole family is now revoked**

### POST `/api/auth/logout` — _auth_
Revokes the session tied to the bearer token. Optionally also revoke the refresh
token by sending it in the body (recommended on real logout):
```json
{ "refreshToken": "Y0ogFbdvEQOga0jG-DtzKdjpZZfwYYT5LeoSbqZDG1g" }
```
Response `200`, `data: null`.

### GET `/api/auth/me` — _auth_
Returns the current user object (same shape as `data.user` above).

### PATCH `/api/me/base-currency` — _auth_
Switches the user's base currency **and re-denominates all of their money into
it at the current market rate**: every account's `balance` (and its `currency`),
all of that account's transaction `amount`s, and any budgets/periods. The `from`
rate is each account's existing currency; conversion uses live rates (frankfurter).
```json
{ "currency": "INR" }
```
- `currency`: a 3-letter ISO code that must be in `GET /api/currencies`.

Response `200`: the updated user object (`baseCurrency` now reflects the change).
Re-fetch accounts/transactions afterwards — their amounts have changed. Errors:
`400 BAD_REQUEST` for an unsupported code, `503 FX_UNAVAILABLE` if rates can't be
fetched (nothing is changed in that case — the switch is atomic).

> Conversion is a single multiply per amount, so per-row rounding can differ by a
> cent from the converted balance. Acceptable for a personal tracker.

---

## 3b. Currencies  `/api/currencies` — _auth_

### GET `/api/currencies`
Supported currencies for the client's base-currency dropdown, sorted by code.
```json
{ "success": true, "data": [
  { "code": "AUD", "name": "Australian Dollar" },
  { "code": "EUR", "name": "Euro" },
  { "code": "INR", "name": "Indian Rupee" },
  { "code": "USD", "name": "United States Dollar" }
] }
```

---

## 4. Accounts  `/api/accounts` — _all auth_

An account is a money container (cash, bank, card…) with a running `balance`
that the transaction endpoints keep in sync automatically.

| Method | Path | Body | Result |
|--------|------|------|--------|
| GET | `/api/accounts` | — | List your accounts |
| GET | `/api/accounts/{id}` | — | One account |
| POST | `/api/accounts` | AccountRequest | Create |
| PUT | `/api/accounts/{id}` | AccountRequest | Update name/type/currency |
| POST | `/api/accounts/{id}/archive?archived=true` | — | Archive / unarchive |
| DELETE | `/api/accounts/{id}` | — | Delete |

**AccountRequest**
```json
{ "name": "Cash Wallet", "type": "CASH", "currency": "USD", "openingBalance": 100.00 }
```
- `type`: `CASH | BANK | CARD | WALLET | SAVINGS | INVESTMENT`
- `currency`: 3-letter ISO code; defaults to the user's base currency
- `openingBalance`: optional, defaults `0`. (On update, balance is left untouched.)

**AccountResponse**
```json
{ "id": "7729…", "name": "Cash Wallet", "type": "CASH",
  "currency": "USD", "balance": 74.50, "archived": false }
```

---

## 5. Categories  `/api/categories` — _all auth_

Labels for transactions. Nine are seeded at registration (Salary, Food & Dining,
Transport, …).

| Method | Path | Body |
|--------|------|------|
| GET | `/api/categories` | — (optional `?type=INCOME` or `EXPENSE`) |
| POST | `/api/categories` | CategoryRequest |
| PUT | `/api/categories/{id}` | CategoryRequest |
| DELETE | `/api/categories/{id}` | — |

**CategoryRequest**
```json
{ "name": "Groceries", "type": "EXPENSE", "icon": "🛒", "color": "#f39c12" }
```
- `type`: `INCOME | EXPENSE` (required)
- `icon`, `color`: optional display hints for the client

**CategoryResponse**
```json
{ "id": "529f…", "name": "Groceries", "type": "EXPENSE", "icon": "🛒", "color": "#f39c12" }
```

---

## 6. Transactions  `/api/transactions` — _all auth_

Creating, updating, or deleting a transaction **automatically adjusts the linked
account's balance** (`INCOME` adds, `EXPENSE` subtracts). `amount` is always a
positive magnitude — direction comes from `type`.

### GET `/api/transactions` — paged search
Query params (all optional):

| Param | Type | Meaning |
|-------|------|---------|
| `accountId` | UUID | Filter to one account |
| `type` | `INCOME`\|`EXPENSE` | Filter by direction |
| `from` | date `YYYY-MM-DD` | On/after this date |
| `to` | date `YYYY-MM-DD` | On/before this date |
| `page` | int (default 0) | Page index |
| `size` | int (default 20, max 100) | Page size |

Results are sorted newest-first. Response `data` is a Spring `Page`:
```json
{
  "success": true,
  "data": {
    "content": [ TransactionResponse, … ],
    "totalElements": 42, "totalPages": 3,
    "number": 0, "size": 20, "first": true, "last": false
  }
}
```

### GET `/api/transactions/summary`
Aggregates income/expense over an optional `from`/`to` window.
```json
{ "success": true, "data": { "totalIncome": 0, "totalExpense": 25.50, "net": -25.50 } }
```

### GET `/api/transactions/{id}` — single transaction

### POST `/api/transactions` — create
```json
{
  "accountId": "7729…",          // required, must be yours
  "categoryId": "68f8…",         // optional
  "type": "EXPENSE",             // INCOME | EXPENSE
  "amount": 25.50,               // > 0
  "occurredOn": "2026-06-27",    // required
  "note": "Lunch"                // optional, ≤ 255 chars
}
```

### PUT `/api/transactions/{id}` — update
Same body. The old balance effect is reversed and the new one applied (handles
moving a transaction to a different account).

### DELETE `/api/transactions/{id}`
Removes it and reverses its balance effect.

**TransactionResponse**
```json
{
  "id": "c9a2…", "accountId": "7729…", "accountName": "Cash Wallet",
  "categoryId": "68f8…", "categoryName": "Health",
  "type": "EXPENSE", "amount": 25.50, "occurredOn": "2026-06-27", "note": "Lunch"
}
```

---

## 7. End-to-end example (curl)

```bash
B=http://localhost:8080

# 1) Register and capture the token
TOKEN=$(curl -s -X POST $B/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"you@mail.com","password":"supersecret1","displayName":"You"}' \
  | sed -n 's/.*"sessionToken":"\([^"]*\)".*/\1/p')

# 2) Create an account
ACC=$(curl -s -X POST $B/api/accounts -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Cash Wallet","type":"CASH","currency":"USD","openingBalance":100}')

# 3) Add an expense -> account balance drops to 74.50
curl -s -X POST $B/api/transactions -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"accountId":"<ACC_ID>","type":"EXPENSE","amount":25.50,"occurredOn":"2026-06-27","note":"Lunch"}'

# 4) Monthly summary
curl -s "$B/api/transactions/summary?from=2026-06-01&to=2026-06-30" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 8. Public vs protected

**Public (no token):** `POST /api/auth/register`, `POST /api/auth/login`,
`POST /api/auth/refresh`, `GET /actuator/health`, and `/h2-console/**` (dev only).
**Everything else requires** `Authorization: Bearer <sessionToken>`.

CORS allowed origins are controlled by `APP_CORS_ORIGINS` (comma-separated;
`*` in dev). Set it to your app's origin in production.
