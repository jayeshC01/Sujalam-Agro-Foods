# Sujalam Agro Foods — Backend

Spring Boot 3.3 / Java 17 REST API for the Sujalam Agro Foods store, running on
MySQL / TiDB.

## Key features

- **E-commerce API** — product catalog with search and pagination; admin product
  CRUD and inventory control (restock, write-off, soft-delete / restore); customer
  registration and self-service profiles; order checkout, status lifecycle, and
  cancellation rules.
- **Idempotent checkout** — an `Idempotency-Key` header, a stored request hash, and
  a unique constraint make a retried order return the original instead of creating
  a duplicate.
- **Optimistic locking** — a `@Version` column on every entity prevents lost
  updates under concurrent stock or order changes.
- **Firebase authentication** — a servlet filter verifies the Firebase ID token on
  each request and resolves it to a local `users` row; URL and method rules gate
  `/admin/**` to `role = ADMIN`.
- **Rate limiting** — per-client Bucket4j token buckets in front of the API.
- **CORS** — independent origin policies for the public API and for `/admin/**`.
- **Consistent error responses** — a central `@RestControllerAdvice` maps domain
  exceptions to structured JSON (`code`, `message`, `requestId`); the
  authentication entry-point and access-denied handlers feed the same formatter.
- **Reliable email** — order confirmations and admin alerts are published after the
  database commit and sent on a background executor with retry/backoff, so a mail
  outage never fails the request.
- **Flyway schema management** — versioned SQL migrations with Hibernate in
  `ddl-auto=validate`; the app never mutates its own schema.
- **Observability** — a request-id MDC logging filter for log correlation and
  Actuator health / liveness / readiness endpoints.

## API endpoints

Authenticated requests carry a Firebase ID token: `Authorization: Bearer <token>`.
`/admin/**` and a few method-secured routes additionally require the caller's
Firebase UID to map to a `users` row with `role = ADMIN`. The first admin is
seeded directly in the database.

### Public — no token

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/products` | Product catalog (paginated, `?category=&q=&page=&size=&sortBy=&sortDirection=`) |
| GET | `/product/{id}` | Single active product |
| GET | `/actuator/health`, `/actuator/info` | Service probes |

### Authenticated — any valid token

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/customer/register` | Create the caller's customer profile |
| GET | `/customer/me` | Own profile |
| PUT | `/customer/me` | Update own profile |
| DELETE | `/customer/me` | Deactivate own account |
| GET | `/customer/{id}` | Fetch a customer by id |
| POST | `/orders` | Place an order — requires an `Idempotency-Key` header |
| GET | `/orders` | Own orders (paginated) |
| GET | `/order/{id}` | Single order |
| POST | `/orders/{id}/cancel` | Cancel an order (subject to status rules) |

### Admin — `role = ADMIN`

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/customers` | List / search customers (paginated) |
| POST | `/admin/register` | Register a new admin |
| DELETE | `/admin/customer/{id}` | Deactivate a customer |
| POST | `/admin/customer/{id}/block` | Block a customer |
| POST | `/admin/customer/{id}/restore` | Reactivate a customer |
| GET | `/admin/products` | List products including inactive (paginated) |
| GET | `/admin/product/{id}` | Single product, any status |
| POST | `/admin/product` | Create a product |
| PUT | `/admin/product/{id}` | Update a product |
| POST | `/admin/product/{id}/restock?quantity=` | Increase stock |
| POST | `/admin/product/{id}/write-off?quantity=` | Decrease stock |
| DELETE | `/admin/product/{id}` | Soft-delete a product |
| POST | `/admin/product/{id}/restore` | Restore a soft-deleted product |
| GET | `/admin/orders` | All orders (paginated) |
| PATCH | `/admin/orders/{id}/status` | Advance an order's status |

## Prerequisites

**Docker only.** The image builds and runs the JDK; the database runs as a
container. No local Java or MySQL needed.

- **macOS**
  ```
  brew install --cask docker        # Docker Desktop
  # or headless:
  brew install colima docker docker-compose docker-buildx && colima start --cpu 4 --memory 8
  ```
- **Linux**
  ```
  curl -fsSL https://get.docker.com | sh
  sudo usermod -aG docker "$USER" && newgrp docker
  ```
- **Windows** (PowerShell, requires WSL2)
  ```
  winget install -e --id Docker.DockerDesktop
  ```

Compose v2 and Buildx ship with Docker Desktop and the Linux install script.

## Run

### 1. Create `.env` in the repo root

```dotenv
# Leave the DB_* block blank to use the bundled local TiDB container.
DB_URL=
DB_USER=
DB_PASSWORD=

# Leave SPRING_MAIL_* blank for local dev — SMTP then points at a dead port and
# nothing is sent. Fill them in for real delivery.
SPRING_MAIL_HOST=
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=
SPRING_MAIL_PASSWORD=
APP_MAIL_FROM=noreply@example.com
APP_MAIL_ADMIN=admin@example.com
MANAGEMENT_HEALTH_MAIL_ENABLED=false

# Absolute path ON THE HOST to the Firebase service account JSON.
GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/firebase-adminsdk.json

CORS_ALLOWED_ORIGINS=http://localhost:3000
CORS_ADMIN_ALLOWED_ORIGINS=http://admin.localhost:3000
```

`.env` is gitignored. Every key has a default in `docker-compose.yml`, so an empty
`.env` still boots (bundled TiDB, mail disabled).

### 2. Start / verify

```
make start                               # docker compose up -d --build
curl localhost:8080/actuator/health      # -> {"status":"UP", ...}
make logs                                # docker compose logs -f
make stop                                # docker compose down
```

## Build & test

```
make build        # spotlessApply + build + JaCoCo
make test         # tests + coverage (needs Docker for Testcontainers)
make format       # spotlessApply
```
