# High-Concurrency Financial Ledger

A core banking engine built as an immutable, append-only ledger with
pessimistic row-locking to prevent double-spending under concurrent load,
plus stateless JWT authentication and role-based authorization.

## Architecture decisions

1. **No mutable `balance` column.** `Account` has no balance field at all.
   Every balance is computed on demand in `TransactionService.calculateBalance`
   by streaming the account's `Transaction` history and folding signed
   amounts (`+amount` for credits, `-amount` for debits) into a total with
   `BigDecimal::add`. The ledger is append-only and fully auditable — you
   can always recompute "truth" from history.

2. **Pessimistic locking, not optimistic.** `AccountRepository.findByIdForUpdate`
   issues `SELECT ... FOR UPDATE` via `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
   `TransactionService.transferFunds` locks both the source and destination
   account rows *before* computing the sender's balance, so no concurrent
   transaction can read a stale balance or move money out from under it.
   This is proven against a real Postgres instance in
   `TransactionServiceConcurrencyTest` (see "Running tests" below).

3. **Deadlock avoidance via consistent lock ordering.** `transferFunds`
   always locks the lower account ID first, regardless of transfer
   direction, so opposing simultaneous transfers (A→B and B→A) can't
   circular-wait on each other.

4. **Virtual threads (Java 21).** `spring.threads.virtual.enabled=true` plus
   a `VirtualThreadConfig` bean mean request threads (including ones
   blocked waiting on a pessimistic DB lock) and `@Async` background work
   run on cheap virtual threads instead of a bounded platform pool.

5. **Stateless JWT auth.** No server-side sessions. `JwtAuthenticationFilter`
   validates the `Authorization: Bearer <token>` header on every request and
   populates the Spring Security context; `SecurityConfig` enforces
   `/api/auth/**` as public and everything else as authenticated.
   `@EnableMethodSecurity` + `@PreAuthorize` support role checks
   (`ROLE_ADMIN` vs `ROLE_CUSTOMER`).

6. **Ownership enforcement.** A CUSTOMER can only see/act on accounts they
   own; an ADMIN can act on any account (`AccountService.verifyOwnershipOrAdmin`,
   used by every account/transaction controller method).

7. **Idempotency keys.** `TransferRequest`/`AmountRequest` accept an optional
   `idempotencyKey`. If a client retries the same request (e.g. after a
   timeout) with the same key, `TransactionService` returns the original
   transaction instead of moving money twice.

8. **Consistent error responses.** `GlobalExceptionHandler` maps domain
   exceptions to correct HTTP codes: `404` account not found, `409`
   insufficient funds, `400` invalid request / validation errors, `403`
   ownership violation, `401` bad credentials / refresh-token problems.

9. **Refresh tokens with rotation and revocation.** Access tokens are
   short-lived JWTs (15 min). Refresh tokens are opaque, random strings
   stored in the `refresh_tokens` table (`RefreshToken` entity) — NOT JWTs
   — specifically so they can be revoked with a single `UPDATE ... SET
   revoked = true`, which a self-contained JWT can't do without a
   blacklist. Every `/api/auth/refresh` call ROTATES the token: the
   presented one is revoked and a new one issued, so a stolen-and-replayed
   refresh token only works once — the legitimate client's next refresh
   attempt fails loudly, signaling theft.

10. **Rate limiting on auth endpoints.** `RateLimitingFilter` is a small,
    dependency-free, in-memory fixed-window limiter (10 requests/minute
    per client) scoped only to `/api/auth/*` via `RateLimitingConfig`,
    blunting brute-force login/registration attempts. Documented
    limitation: it's per-instance, not distributed — see "Not included."

11. **Pagination on transaction history.** `GET /api/transactions/account/{id}`
    takes `page`/`size` query params (capped at 100) and returns a
    `PagedResponse`. Deliberately kept separate from `calculateBalance`,
    which always folds over the *complete* unpaginated history — a
    partial page would silently produce a wrong balance.

12. **Admin user provisioning.** `POST /api/admin/users` is the one
    legitimate way (besides Flyway seed data) to create another ADMIN —
    gated by class-level `@PreAuthorize("hasRole('ADMIN')")` on
    `AdminController`. Self-service `/api/auth/register` can never create
    an ADMIN, by construction (see `AuthService.register`).

## Project layout

```
src/main/java/com/ledger/
  entity/        User, Account, Transaction, RefreshToken + enums
  repository/    Spring Data JPA repositories (incl. the pessimistic-lock query)
  service/       TransactionService, AccountService, AuthService, RefreshTokenService
  security/      JWT filter, JwtService, SecurityConfig, UserDetails adapter
  controller/    AuthController, AccountController, TransactionController, AdminController
  dto/           request/response records
  exception/     domain exceptions + GlobalExceptionHandler
  config/        VirtualThreadConfig, RateLimitingFilter, RateLimitingConfig
src/main/resources/
  application.yml
  db/migration/  Flyway SQL (schema, seeded admin user, refresh_tokens table)
src/test/java/com/ledger/service/
  TransactionServiceConcurrencyTest.java   (Testcontainers Postgres)
```

## Prerequisites

- **JDK 21** (`java -version` should show 21)
- **Maven 3.9+** (or use the included wrapper if you add one — instructions below assume `mvn` is on your PATH)
- **Docker** — for Postgres locally, and for the Testcontainers-based concurrency test

## Step-by-step: put the files on disk and run locally

### 1. Create the project folder and copy in the files

Recreate this exact folder structure on your machine (paths below are
relative to a root folder, e.g. `high-concurrency-ledger/`):

```
high-concurrency-ledger/
├── pom.xml
├── docker-compose.yml
├── .env.example
├── .gitignore
├── README.md
└── src/
    ├── main/
    │   ├── java/com/ledger/...        (all .java files, matching their package folders)
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/
    │           ├── V1__init_schema.sql
    │           └── V2__seed_admin_user.sql
    └── test/
        └── java/com/ledger/service/TransactionServiceConcurrencyTest.java
```

Every Java file's `package com.ledger.xxx;` declaration tells you exactly
which folder it belongs in — e.g. `package com.ledger.security;` →
`src/main/java/com/ledger/security/JwtService.java`. Create each folder to
match.

### 2. Start Postgres

From the project root:

```bash
docker compose up -d
```

This starts Postgres 16 on `localhost:5432` with database `ledger_db`,
user `ledger_user`, password `changeme` (override via env vars — see
`.env.example`).

### 3. Set environment variables

```bash
cp .env.example .env
# then either `export $(cat .env | xargs)` or load it however your shell/IDE supports,
# or just export directly:
export DB_USERNAME=ledger_user
export DB_PASSWORD=changeme
export JWT_SECRET=$(openssl rand -base64 48)
```

### 4. Build and run

```bash
mvn clean install -DskipTests
mvn spring-boot:run
```

Flyway runs automatically on startup and creates the schema plus a seeded
admin user (`admin` / `admin123`) — see `V1__init_schema.sql` and
`V2__seed_admin_user.sql`.

The app starts on `http://localhost:8080`.

### 5. Verify it's up

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

## Trying the API end-to-end

```bash
# Register a customer
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}' | tee alice.json

TOKEN=$(python3 -c "import json;print(json.load(open('alice.json'))['accessToken'])")
REFRESH=$(python3 -c "import json;print(json.load(open('alice.json'))['refreshToken'])")

# Create an account for alice
curl -s -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer $TOKEN" | tee account.json

ACCOUNT_ID=$(python3 -c "import json;print(json.load(open('account.json'))['id'])")

# Deposit funds
curl -s -X POST http://localhost:8080/api/transactions/deposit \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"accountId\":$ACCOUNT_ID,\"amount\":500.00}"

# Check balance
curl -s http://localhost:8080/api/accounts/$ACCOUNT_ID/balance \
  -H "Authorization: Bearer $TOKEN"

# Paginated history
curl -s "http://localhost:8080/api/transactions/account/$ACCOUNT_ID?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"

# Access token expired (15 min)? Exchange the refresh token for a new one.
# Note: this ROTATES the refresh token — save the new one from the response.
curl -s -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}"

# Log out (revoke the current refresh token)
curl -s -X POST http://localhost:8080/api/auth/logout \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}"
```

Register a second user (`bob`), create an account for him, grab his account
ID, then transfer between them:

```bash
curl -s -X POST http://localhost:8080/api/transactions/transfer \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"sourceAccountId\":$ACCOUNT_ID,\"destinationAccountId\":$BOB_ACCOUNT_ID,\"amount\":100.00}"
```

### Trying the admin endpoints

Log in as the seeded admin (`admin` / `admin123`), then create another admin
or list every account in the system:

```bash
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | python3 -c "import json,sys;print(json.load(sys.stdin)['accessToken'])")

curl -s -X POST http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"username":"ops-admin","password":"password123","role":"ADMIN"}'

curl -s http://localhost:8080/api/admin/accounts \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### API summary

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | public, rate-limited | Create a CUSTOMER account |
| POST | `/api/auth/login` | public, rate-limited | Get an access + refresh token |
| POST | `/api/auth/refresh` | public, rate-limited | Exchange a refresh token for a new access token (rotates it) |
| POST | `/api/auth/logout` | public, rate-limited | Revoke a refresh token |
| POST | `/api/accounts` | Bearer | Create an account for yourself |
| GET | `/api/accounts/mine` | Bearer | List your accounts with balances |
| GET | `/api/accounts/{id}` | Bearer (owner/admin) | Get one account + balance |
| GET | `/api/accounts/{id}/balance` | Bearer (owner/admin) | Just the balance |
| POST | `/api/transactions/deposit` | Bearer (owner/admin) | Deposit into an account |
| POST | `/api/transactions/withdraw` | Bearer (owner/admin) | Withdraw from an account |
| POST | `/api/transactions/transfer` | Bearer (owner/admin) | Move funds between two accounts |
| GET | `/api/transactions/account/{id}?page=0&size=20` | Bearer (owner/admin) | Paginated ledger history for an account |
| POST | `/api/admin/users` | Bearer (ADMIN only) | Create a user with any role |
| GET | `/api/admin/accounts` | Bearer (ADMIN only) | Every account in the system, with balances |
| GET | `/actuator/health` | public | Liveness check |

All three auth-endpoint groups above (`register`/`login`/`refresh`/`logout`)
share a single rate-limit bucket per client IP: 10 requests/minute.
Exceeding it returns `429 Too Many Requests` with a `Retry-After: 60` header.

## Running tests

```bash
mvn test
```

`TransactionServiceConcurrencyTest` requires Docker (it spins up a real
Postgres container via Testcontainers) and fires 10 concurrent withdrawal
requests against an account that can only afford one, asserting exactly
one succeeds and the final balance is never negative. This is the direct,
executable proof that the pessimistic-locking strategy works.

## Not included in this drop

- Distributed rate limiting (current limiter is in-memory per-instance — fine for one node, not for a multi-instance deployment behind a load balancer; a real deployment would move this to Redis)
- Refresh token cleanup job (expired/revoked rows accumulate in `refresh_tokens` — a scheduled `@Scheduled` deletion job would be the next step)
- Structured audit logging separate from application logs
