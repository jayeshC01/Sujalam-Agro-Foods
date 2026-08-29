# Sujalam Agro Foods — Backend API

Production-ready RESTful backend service for **Sujalam Agro Foods**, an e-commerce platform specializing in premium agro products, dry fruits, cold-pressed oils, grains, and spices.

Built with **Java 17**, **Spring Boot 3.3**, **MySQL 8**, **Spring Security**, and **Firebase Admin SDK**.

---

## 🚀 Key Architectural Features & Highlights

### 1. 🔐 Security & Identity Management
* **Firebase Authentication:** Validates client-side Firebase ID Tokens (JWT) inside [`FirebaseAuthenticationFilter`](src/main/java/com/gryffindor/excalibur/config/FirebaseAuthenticationFilter.java).
* **Stateless Sessions:** Token-based security without server-side HTTP session state.
* **Role-Based Access Control (RBAC):** Strict segregation between public catalog browsing (`permitAll`), customer self-service (`ROLE_USER`), and administrative operations (`ROLE_ADMIN`).
* **Security EntryPoints & Error Delegation:** `AuthenticationEntryPoint` (401) and `AccessDeniedHandler` (403) delegate to Spring's `HandlerExceptionResolver`, ensuring security filter errors return consistent JSON responses.

### 2. 🛡️ Centralized Error Handling & Request Tracing
* **Per-Request Correlation ID (`X-Request-Id`):** Injected via [`RequestLoggingFilter`](src/main/java/com/gryffindor/excalibur/config/RequestLoggingFilter.java) into SLF4J MDC, response headers, and error response bodies for end-to-end log traceability.
* **Sanitized Server Errors:** Global `@RestControllerAdvice` in [`ErrorHandler`](src/main/java/com/gryffindor/excalibur/resources/ErrorHandler.java) guarantees internal database passwords, stack traces, and class names are never leaked in 500 error messages.
* **Strongly-Typed Domain Exceptions:** Clear validation errors via `InvalidOrderStatusTransitionException` and `InvalidRequestException` returning HTTP 400 Bad Request.
* **Database Outage Resilience:** Catches `DataAccessException` to return clean `503 Service Unavailable` with structured logging.

### 3. 📦 Inventory & Order Lifecycle Management
* **Stock Reservation & Atomic Decrements:** Prevents stock overselling during concurrent checkouts.
* **Order State Machine:** Validates status moves (`PENDING` → `COMPLETED` / `CANCELED`) via `OrderStatus.canTransitionTo()`.
* **Automatic Stock Restoration:** Cancelling an order automatically restores reserved inventory units to stock in a single transaction.
* **Idempotency Protection:** `POST /orders` requires an `Idempotency-Key` header with SHA-256 payload hashing to prevent accidental duplicate orders on network retries.

### 4. 📧 Transactional Asynchronous Notifications
* **Decoupled Event Publishing:** Orders trigger `OrderPlacedEvent` and `OrderStatusUpdatedEvent` via Spring's `ApplicationEventPublisher`.
* **Transactional Event Listeners:** Processed asynchronously after DB transaction commit (`TransactionPhase.AFTER_COMMIT`).
* **Thymeleaf HTML Templates & Exponential Backoff:** Sends rich customer invoices and admin alerts via Brevo SMTP with `@Retryable` backoff policies.

### 5. 🩺 Observability & Cloud Readiness
* **Spring Boot Actuator:** Preconfigured for container orchestrators (Google Cloud Run, Kubernetes, AWS ALB).
  * Liveness probe: `/actuator/health/liveness`
  * Readiness probe: `/actuator/health/readiness`
* **Interactive API Documentation:** Powered by **Swagger UI / Springdoc OpenAPI 3** at `/swagger-ui.html`.

---

## 🛠️ Technology Stack

* **Language:** Java 17 LTS
* **Framework:** Spring Boot 3.3.4
* **Security:** Spring Security 6 + Firebase Admin SDK 9.4.1
* **Database & Persistence:** MySQL 8, Spring Data JPA, Hibernate 6
* **API Documentation:** Springdoc OpenAPI 3 (Swagger UI 2.6.0)
* **Templating & Email:** Thymeleaf 3 + Spring Mail (Brevo SMTP) + Spring Retry
* **Testing:** JUnit 5, Mockito, AssertJ, Testcontainers MySQL, JaCoCo
* **Build Tool:** Gradle 8.10.2

---

## 📖 API Documentation (Swagger UI)

When running the application locally, visit:
👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

OpenAPI Specification JSON:
👉 **[http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)**

### Testing Authenticated Endpoints in Swagger UI:
1. Obtain a Firebase ID Token for your test user.
2. Click the **Authorize 🔓** button in Swagger UI.
3. Paste the token into the `BearerAuth` value input and click **Authorize**.

---

## 🚦 Core API Endpoints Summary

### 🛒 Products (`/product`, `/products`, `/admin/product/**`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/products` | Public | Paginated product list with search, category, and sorting |
| `GET` | `/product/{id}` | Public | Get product details by ID |
| `GET` | `/admin/products` | Admin | List all products (including archived/inactive) |
| `POST` | `/admin/product` | Admin | Create a new product |
| `PUT` | `/admin/product/{id}` | Admin | Update product details & pricing |
| `POST` | `/admin/product/{id}/restock` | Admin | Restock product inventory |
| `POST` | `/admin/product/{id}/write-off`| Admin | Write off damaged/expired stock |
| `DELETE`| `/admin/product/{id}` | Admin | Soft-delete product |
| `POST` | `/admin/product/{id}/restore` | Admin | Restore inactive product |

### 🛍️ Orders (`/order/**`, `/orders/**`, `/admin/orders/**`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/orders` | Customer | Place an order (requires `Idempotency-Key` header) |
| `GET` | `/orders` | Customer | List authenticated customer's order history |
| `GET` | `/order/{id}` | Owner / Admin | Get order details by ID |
| `POST` | `/orders/{id}/cancel` | Owner / Admin | Cancel a pending order & restore stock |
| `GET` | `/admin/orders` | Admin | List all customer orders with filters |
| `PATCH`| `/admin/orders/{id}/status` | Admin | Update order status (`COMPLETED`, `CANCELED`) |

### 👤 Customers & Identity (`/customer/**`, `/admin/customer/**`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/customer/register` | Authenticated | Register customer profile after Firebase signup |
| `GET` | `/customer/me` | Customer | Get current authenticated profile |
| `PUT` | `/customer/me` | Customer | Update name & phone number |
| `DELETE`| `/customer/me` | Customer | Deactivate own account |
| `GET` | `/customers` | Admin | Paginated list of customers |
| `POST` | `/admin/customer/{id}/block` | Admin | Block customer account |
| `POST` | `/admin/customer/{id}/restore` | Admin | Unblock customer account |
| `POST` | `/admin/register` | Admin | Create a new administrator account |

---

## 💻 Local Setup & Development

### 1. Prerequisites
* **JDK 17** installed (`java -version`)
* **MySQL 8** running locally or via Docker
* Firebase Service Account JSON (or mock Firebase profile for tests)

### 2. Environment Configuration
Copy the example environment configuration:
```bash
cp .env.example .env
```
Provide your local database and mail settings in `application-local.properties` (or set environment variables):
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/sujalam_db
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

### 3. Run the Application
```bash
./gradlew bootRun
```
The server will start on port `8080`.

### 4. Run Test Suite
```bash
./gradlew test
```
Generates JaCoCo test coverage report at `build/reports/jacoco/test/html/index.html`.

---

## 🚢 Production Deployment (Cloud Run / Docker)

Build the runnable JAR:
```bash
./gradlew bootJar
```
Inject production secrets via environment variables:
* `DB_URL`
* `DB_USERNAME`
* `DB_PASSWORD`
* `SMTP_HOST`
* `SMTP_PORT`
* `SMTP_USERNAME`
* `SMTP_PASSWORD`
* `GOOGLE_APPLICATION_CREDENTIALS`
