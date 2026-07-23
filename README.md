# Apex Financial — Core Banking Engine & Digital Portal

[![Docker Image](https://img.shields.io/badge/Docker-sudhirsm13%2Fapex--backend-blue?logo=docker)](https://hub.docker.com/r/sudhirsm13/apex-backend)
[![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.3.6-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![React 18](https://img.shields.io/badge/React-18-blue?logo=react)](https://react.dev/)

An enterprise-grade, high-concurrency core banking system and digital portal built with **Spring Boot 3 (Java 21)** and **React 18 (Vite + Tailwind CSS)**.

Engineered as an **append-only financial ledger**, the system uses **pessimistic row-locking (`SELECT ... FOR UPDATE`)** to guarantee strict ACID compliance and prevent double-spending under concurrent transfer loads.

---

## ⚡ Quick Start (1-Line Launch)

You can launch the entire core banking engine, PostgreSQL database, and digital web portal with a **single command** using pre-built Docker Hub containers:

```bash
docker compose up -d
```

- **Digital Web Portal**: `http://localhost` (Port 80)
- **Core API Server**: `http://localhost:8080`
- **PostgreSQL Database**: `localhost:5432`

*To stop the containers:* `docker compose down`

---

## 🛠️ System Architecture & Engineering Highlights

### 1. Append-Only Ledger (Zero Stale Balance Column)
- The database schema intentionally does **not** store a mutable `balance` column on account rows.
- Account balances are calculated on-demand in `TransactionService.calculateBalance` by streaming all `COMPLETED` ledger entries and folding signed amounts (`+amount` for credits, `-amount` for debits) using exact `BigDecimal` arithmetic.
- **Benefit**: 100% auditable financial history where balances can never desynchronize from transaction records.

### 2. Pessimistic Row-Locking (`SELECT ... FOR UPDATE`)
- Before computing balance or executing a transfer/withdrawal, `AccountRepository.findByIdForUpdate` issues an explicit `SELECT ... FOR UPDATE` (`PESSIMISTIC_WRITE`) query at the database level.
- Concurrent transactions attempting to act on the same account physically block until the lock-holding transaction commits or rolls back, making double-spending impossible.

### 3. Global Lock Ordering (Deadlock Avoidance)
- To prevent deadlocks when Account A transfers to Account B while Account B simultaneously transfers to Account A, `TransactionService.transferFunds` always acquires row locks in **strict ascending Account ID order** (`Math.min(src, dest)` first, then `Math.max(src, dest)`).
- **Benefit**: Eliminates circular-wait deadlock conditions regardless of transfer direction.

### 4. Java 21 Virtual Threads (`Project Loom`)
- Tomcat's HTTP execution mechanism is configured with `spring.threads.virtual.enabled=true`.
- Threads blocked waiting on pessimistic database row locks run on lightweight virtual threads rather than OS platform threads, enabling high concurrency under lock contention with minimal memory overhead.

### 5. Idempotency Key Replay Protection
- Financial request endpoints (`/api/transactions/transfer`, `/deposit`, `/withdraw`) accept an optional client-generated `idempotencyKey` (`UUID`).
- If a network retry occurs, the system detects the key prior to acquiring locks and returns the original transaction record without moving money twice.

### 6. Stateless Dual-Token JWT Auth & Refresh Token Rotation
- **Access Tokens**: Short-lived (15-minute) signed JWTs.
- **Refresh Tokens**: Cryptographically random strings stored in PostgreSQL with instant single-use rotation (`/api/auth/refresh`) and explicit revocation (`/api/auth/logout`).
- **Security**: Role-based authorization (`CUSTOMER` vs `ADMIN`) enforced via Spring Security `@PreAuthorize`.

### 7. Real-Time Telemetry Banking Portal
- Modern React dashboard (**Apex Financial Portal**) styled with Tailwind CSS.
- **Live Security Visualizer**: Displays live database lock states (`idle` → `locking` → `secured`/`rejected`) corresponding to the fetch lifecycle of backend transactions.
- **Live Activity Stream**: Real-time transaction polling ticker diffing new ledger entries.

---

## 💻 Tech Stack

| Layer | Technology / Library |
|---|---|
| **Language & Runtime** | Java 21 (OpenJDK) |
| **Framework** | Spring Boot 3.3.6 |
| **Security** | Spring Security 6, JJWT 0.12.6, BCrypt Password Hashing |
| **Database & ORM** | PostgreSQL 16, Spring Data JPA, Hibernate 6, Flyway Migrations |
| **Concurrency** | Java 21 Virtual Threads, Pessimistic Row Locks (`SELECT ... FOR UPDATE`) |
| **Frontend Framework** | React 18, Vite 8 |
| **Styling & UI** | Tailwind CSS, Lucide React Icons |
| **Testing** | JUnit 5, Mockito, Testcontainers (PostgreSQL 16) |

---

## 🏃 Local Development (From Source)

### Prerequisites
- **Java 21** (`java -version`)
- **Maven 3.9+** (`mvn -version`)
- **Node.js 18+** & **npm** (`node -v`)
- **Docker** (for local PostgreSQL)

### 1. Start PostgreSQL Container
```bash
docker run -d --name apex-postgres -p 5432:5432 -e POSTGRES_DB=ledger_db -e POSTGRES_USER=ledger_user -e POSTGRES_PASSWORD=changeme_prod_password postgres:16-alpine
```

### 2. Run Backend API Server
```bash
cd backend
mvn clean install -DskipTests
mvn spring-boot:run
```
- API will start on **`http://localhost:8080`**.
- Health check: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`

### 3. Run Frontend Web Portal
```bash
cd frontend
npm install
npm run dev
```
- Web Portal opens on **`http://localhost:5173`**.

---

## 🧪 Running Automated Tests

To execute the backend unit and integration test suite:

```bash
cd backend
mvn clean test
```

- **Unit Tests**: `AccountServiceTest`, `AuthServiceTest`, `TransactionServiceTest` run via Mockito.
- **Concurrency Test**: `TransactionServiceConcurrencyTest` tests multi-threaded withdrawals against a real PostgreSQL container via Testcontainers.

---

## 📡 API Endpoints Reference

| Method | Path | Security | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register new customer account |
| `POST` | `/api/auth/login` | Public | Authenticate & receive JWT access + refresh tokens |
| `POST` | `/api/auth/refresh` | Public | Exchange refresh token for new rotated access token |
| `POST` | `/api/auth/logout` | Public | Revoke active refresh token |
| `POST` | `/api/accounts` | Authenticated | Open new bank account for authenticated customer |
| `GET` | `/api/accounts/mine` | Authenticated | List all accounts owned by logged-in customer |
| `GET` | `/api/accounts/{id}` | Owner / Admin | Get specific account details |
| `GET` | `/api/accounts/{id}/balance` | Owner / Admin | Get calculated account balance |
| `POST` | `/api/transactions/deposit` | Owner / Admin | Credit funds to account |
| `POST` | `/api/transactions/withdraw` | Owner / Admin | Debit funds from account (pessimistic lock) |
| `POST` | `/api/transactions/transfer` | Owner / Admin | Atomic transfer between accounts (deadlock-free pessimistic lock) |
| `GET` | `/api/transactions/account/{id}` | Owner / Admin | Get paginated transaction history (`?page=0&size=20`) |
| `POST` | `/api/admin/users` | Admin Only | Provision new user (`CUSTOMER` or `ADMIN`) |
| `GET` | `/api/admin/accounts` | Admin Only | System-wide account & balance overview |
| `GET` | `/actuator/health` | Public | Health check endpoint |

---

## 🔑 Initial Credentials

- **Seeded Admin Account**:
  - Username: `admin`
  - Password: `admin123`
- **Customer Account**:
  - Click **Register Account** on the frontend login page to create custom credentials.
