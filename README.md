# Apex Financial — High-Concurrency Core Banking Engine & Digital Portal

A high-performance, enterprise-grade core banking application built with **Spring Boot 3 (Java 21)** and **React 18 (Vite + Tailwind CSS)**. 

Engineered as an **append-only financial ledger**, the system uses **pessimistic row-locking (`SELECT ... FOR UPDATE`)** to guarantee strict ACID compliance and prevent double-spending under heavy concurrent load. It features Java 21 Virtual Threads, idempotency safeguards, stateless JWT authentication with refresh token rotation, and a real-time digital banking interface.

---

## Technical Characteristics & Architecture Highlights

### 1. Zero Stale Balance Column (Append-Only Ledger)
- The database schema intentionally does **not** store a mutable `balance` column on account rows.
- Account balance is dynamically derived in `TransactionService.calculateBalance` by streaming all `COMPLETED` transaction entries touching an account and folding signed amounts (`+amount` for credits, `-amount` for debits) into an exact `BigDecimal` total.
- **Benefit**: 100% auditable truth where balances can never desynchronize from transaction records.

### 2. Pessimistic Row-Locking (`SELECT ... FOR UPDATE`) & Double-Spending Protection
- Before computing balance or executing a transfer/withdrawal, `AccountRepository.findByIdForUpdate` issues an explicit `SELECT ... FOR UPDATE` (`PESSIMISTIC_WRITE`) query.
- Concurrent transactions attempting to act on the same account physically block until the lock-holding transaction commits or rolls back, making double-spending mathematically impossible.

### 3. Global Lock Ordering (Deadlock Avoidance)
- To prevent deadlocks when Account A transfers to Account B while Account B simultaneously transfers to Account A, `TransactionService.transferFunds` always acquires row locks in **strict ascending Account ID order** (`Math.min(src, dest)` first, then `Math.max(src, dest)`).
- **Benefit**: Eliminates circular-wait deadlock conditions regardless of transfer direction.

### 4. Java 21 Virtual Threads (`Project Loom`)
- Tomcat's HTTP execution mechanism is configured with `spring.threads.virtual.enabled=true`.
- Threads blocked on pessimistic database row locks run on lightweight virtual threads rather than OS platform threads, enabling the backend to handle thousands of concurrent lock-waiting connections with minimal memory overhead.

### 5. Idempotency Key Replay Protection
- Financial request endpoints (`/api/transactions/transfer`, `/deposit`, `/withdraw`) take a unique client-generated `idempotencyKey` (`UUID`).
- If a network retry occurs, the system detects the key prior to acquiring locks and returns the original transaction record without re-executing fund movement.

### 6. Stateless JWT Authentication & Refresh Token Rotation
- **Access Tokens**: Short-lived (15-minute) signed JWTs.
- **Refresh Tokens**: Cryptographically random strings stored in PostgreSQL with instant single-use rotation (`/api/auth/refresh`) and explicit revocation (`/api/auth/logout`).
- **Security**: Strict role-based authorization (`CUSTOMER` vs `ADMIN`) via Spring Security method annotations (`@PreAuthorize`).

### 7. Modern Commercial Banking UI & Lock Engine Visualizer
- Modern React dashboard (**Apex Financial Portal**) styled with Tailwind CSS and Lucide icons.
- **Real-Time Security Visualizer**: Displays live database lock states (`idle` → `locking` → `success`/`error`) corresponding to the fetch lifecycle of backend transactions.
- **Live Activity Stream**: Real-time transaction polling ticker diffing new ledger entries.

---

## Features Showcase

### Customer Features
- **Account Registration & Authentication**: Secure sign-up and login with automatic `CUSTOMER` role assignment.
- **Multi-Account Creation**: Customers can open and manage multiple bank accounts under a single profile.
- **Fund Transfers**: Transfer money securely between any valid account with automatic balance & constraint validation.
- **Deposits & Withdrawals**: Credit or debit account funds with instant ledger updates.
- **Transaction History**: View paginated ledger entries (`PagedResponse`) with timestamps, transaction types, routes, and amounts.

### Enterprise Admin Features
- **System-Wide Account Audit**: View all customer accounts and real-time computed balances across the entire banking system.
- **User Provisioning**: Provision new platform users with custom role assignments (`CUSTOMER` or `ADMIN`).

---

## Tech Stack

| Layer | Technology / Library |
|---|---|
| **Language & Runtime** | Java 21 (OpenJDK) |
| **Framework** | Spring Boot 3.3.6 |
| **Security** | Spring Security 6, JJWT 0.12.6, BCrypt Password Hashing |
| **Database & ORM** | PostgreSQL 16, Spring Data JPA, Hibernate 6, Flyway Migrations |
| **Concurrency** | Java 21 Virtual Threads, Pessimistic Row Locks (`SELECT ... FOR UPDATE`) |
| **Frontend Framework** | React 18, Vite 8 |
| **Styling & UI** | Tailwind CSS, Lucide React Icons |
| **Testing** | JUnit 5, Mockito, Testcontainers (PostgreSQL 16), H2 Database |

---

## How to Run the Project

### Prerequisites

Ensure you have the following installed on your system:
- **Java 21** (`java -version` should output `21`)
- **Maven 3.9+** (`mvn -version`)
- **Node.js 18+** & **npm** (`node -v`, `npm -v`)
- **Docker & Docker Compose** (for running PostgreSQL locally)

---

### Step 1: Clone the Repository

```bash
git clone https://github.com/your-username/high-concurrency-ledger.git
cd high-concurrency-ledger
```

---

### Step 2: Start PostgreSQL Database

Launch the PostgreSQL container via Docker Compose:

```bash
cd backend
docker compose up -d
```

This starts PostgreSQL on `localhost:5432` with database `ledger_db`.

---

### Step 3: Run the Backend API

From the `backend` directory, build and run the Spring Boot application:

```bash
# Build the project
mvn clean install -DskipTests

# Run the backend API server
mvn spring-boot:run
```

- Flyway will automatically execute migrations (`V1__init_schema.sql`, `V2__seed_admin_user.sql`).
- The backend will start on **`http://localhost:8080`**.
- Verify backend health:
  ```bash
  curl http://localhost:8080/actuator/health
  # Returns: {"status":"UP"}
  ```

---

### Step 4: Run the Frontend Portal

Open a second terminal window, navigate to the `frontend` directory, and start the Vite dev server:

```bash
cd frontend

# Install frontend dependencies
npm install

# Start the dev server
npm run dev
```

- Open your browser to **`http://localhost:5173`**.

---

### Step 5: Run Automated Tests

To execute the backend unit and integration test suite:

```bash
cd backend
mvn clean test
```

- Unit tests (`TransactionServiceTest`, `AccountServiceTest`, `AuthServiceTest`) run in-memory via Mockito.
- Concurrency test (`TransactionServiceConcurrencyTest`) tests multi-threaded withdrawals against real PostgreSQL (runs automatically when Docker is active).

---

## API Endpoints Reference

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

## Initial Credentials

- **Seeded Admin Account**:
  - Username: `admin`
  - Password: `admin123`
- **Customer Account**:
  - Click **Register Account** on the frontend login page to create custom credentials instantly.
