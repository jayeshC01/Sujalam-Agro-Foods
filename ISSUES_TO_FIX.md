# Issues To Fix

Prioritized gap list from the MVP readiness scan. Not tracked in git — local working notes only.

Replaces the earlier `KNOWN_ISSUES.md`; every still-open item from that file is carried over below
and cross-referenced as `(was KNOWN_ISSUES #n)`.

---

## Minimum cut to MVP

Shortest line to "a frontend can be built against this and a real customer can order":

**P0-1 CORS → P0-2 env config → P0-3 Dockerfile/deploy → P0-4 admin bootstrap → P1-8 admin
new-order notification → P1-11 delivery/GST in total → P3-31 OpenAPI docs.**

P2 is a hardening pass for the week after launch, except **P2-18** and **P2-19**, which are a few
lines each and should be folded into the P0 work.

---

## Decision: Open-Session-In-View stays OFF

`spring.jpa.open-in-view=false` is set deliberately. Do not turn it back on to make a
`LazyInitializationException` go away — fix the read path instead. Reasons, in order of weight:

1. **OSIV hides the N+1 (P2-24).** With it on, the per-order `getUser()` and per-item
   `getProduct()` loads fire during Jackson serialization — after the service method returned,
   outside any transaction, invisible to service-layer profiling. ~50 queries per order page and
   nothing in the code looks wrong.
2. **The database can't afford it.** The session lives for the whole request and every render-time
   lazy load reaches back for a connection while serializing to a possibly-slow mobile client. On a
   small `max_connections` (see P0-5), render-phase pool churn under concurrency is the most likely
   way this app falls over.
3. **It is a one-way door.** Once code assumes OSIV, turning it off means auditing every endpoint.
   At ~1,800 lines the audit was one pass and three annotations. It only gets more expensive.
4. **It keeps the API contract honest.** With OSIV on, returning an entity straight from a
   controller accidentally works — so eventually someone does, `@JsonIgnore` starts appearing on
   entities, and the JSON shape becomes the DB schema. The current DTOs are clean because the
   environment does not allow the shortcut.

### The two rules that make this cost nothing

Deciding case-by-case ("does *this* method touch a lazy field?") is error-prone — the answer changes
whenever someone edits a DTO. Apply these mechanically instead:

1. **Every service method carries `@Transactional`** — `readOnly = true` for reads, plain for
   writes. No analysis, no exceptions. Beyond lazy loading this buys one connection checkout instead
   of N, and an atomic count+fetch on paginated queries. Four methods still violate this — see
   P2-34.
2. **Map to a DTO inside that method; never return an entity from a controller.** Already true
   everywhere today — it is what makes rule 1 sufficient.

### Audit status (as of the OSIV switch)

All 14 endpoints were checked and are correct under `open-in-view=false`. `Order.orderDetails` is
the only lazy association in the entire model; every other association is `@ManyToOne` (EAGER by
default) or an `@Embeddable`. No entity, proxy, or persistent collection escapes into a response
body — `OrderResponse.from()` copies into an immutable list inside the transaction, so Jackson only
ever touches plain POJOs. This audit is **static**: it was not verified by running anything, because
the suite cannot verify it (see P2-35).

---

## P0 — Blocks shipping at all

### P0-1. No CORS configuration
Nothing in `SecurityConfig.java` or a `WebMvcConfigurer`. Any browser frontend is blocked on every
request. Single biggest thing making the API unusable from a real client.
**Fix:** `CorsConfigurationSource` bean + `http.cors(...)` in the security chain, allowed origins
driven by a property so local/staging/prod differ.

### P0-2. No deployable configuration
`application.properties` carries no datasource URL, mail credentials, or Firebase key — they exist
only in the gitignored `application-local.properties`. No env-var binding, no prod profile. The app
cannot boot anywhere except the dev laptop.
**Fix:** `${DB_URL}`/`${DB_USER}`/`${DB_PASSWORD}`/`${MAIL_*}` placeholders in
`application.properties`, an `application-prod.properties`, and a documented env contract.

### P0-3. No deployment artifact or pipeline
No Dockerfile, no CD. `.github/workflows/gradle.yml` builds and stops.
**Fix:** multi-stage Dockerfile (JDK 17 build → JRE runtime), plus a deploy job on `master`.

### P0-4. No first-admin bootstrap
`/admin/**` requires `hasRole(ADMIN)`, so admin #1 can only exist via a manual DB insert. A fresh
deploy has no working admin and the step is not reproducible. *(was KNOWN_ISSUES "Deferred")*
**Fix:** one-shot bootstrap — a startup seeding hook keyed on a `BOOTSTRAP_ADMIN_EMAIL` env var, or
a documented, versioned SQL seed.

### P0-5. Production database
`sql12.freesqldatabase.com` free shared tier. Fine for dev, not something to launch on.
**Fix:** pick a managed MySQL (Cloud SQL / RDS / PlanetScale) before go-live.

---

## P1 — Holes in the core order flow

### P1-8. The business is never notified of a new order
`EmailService` only mails the customer. The shop owner has no way to learn an order came in short of
polling `/orders`. For a COD food business this is an MVP blocker.
**Fix:** send an admin notification alongside the customer confirmation, recipient from a property.

### P1-9. No email on status change
No "confirmed"/"shipped"/"cancelled" mail. The customer hears from you exactly once.

### P1-10. Order status model is too thin
`PENDING → COMPLETED/CANCELED` only. No CONFIRMED/SHIPPED/OUT_FOR_DELIVERY, so there is nothing to
render on an order-tracking screen.
**Fix:** extend `OrderStatus` and its `canTransitionTo()` table.

### P1-11. No delivery charge, GST/tax, or minimum order value
`orderTotal` is the raw sum of line subtotals. An Indian food business needs GST and shipping on the
order and on the invoice.
**Fix:** add `subTotal` / `taxAmount` / `deliveryCharge` / `grandTotal` to `Order`, computed
server-side; enforce a minimum order value at creation.

### P1-12. Payment is COD hardcoded in email copy
`Order` has no `paymentMode`/`paymentStatus` field at all. Even if COD-only is the MVP decision,
model both fields now or you will be migrating live order rows later.

### P1-13. No product search, filter, or sort
`GET /products` is a raw page — no category filter, no name search, no in-stock-only.
**Fix:** optional `category`/`q`/`inStock`/`sort` request params backed by a Specification or derived
queries.

### P1-14. No profile update, no saved addresses
`RegisterUser` is write-once; no endpoint changes name or phone. Shipping address is re-typed on
every order.
**Fix:** `PUT /customer/me`, plus an `Address` book table keyed to the user.

### P1-15. Order confirmation email is sent synchronously inside the transaction
`OrderService.java:140` — Brevo's SMTP round-trip is held inside the DB transaction and sits in the
user's response latency.
**Fix:** publish a domain event, handle it `@Async` on `@TransactionalEventListener(AFTER_COMMIT)`.

**Trap when you do this:** `EmailService.buildOrderConfirmationBody()` walks
`order.getOrderDetails()` and `item.getProduct()`. That works today only because the call sits
inside `addOrder`'s transaction. After the move the `Order` will be **detached**, and with OSIV off
there is no safety net — it will throw `LazyInitializationException`. Pass a pre-built DTO into the
async handler, not the entity.

### P1-16. No idempotency on order creation
A double-tap on "Place Order" creates two orders and decrements stock twice.
**Fix:** accept an `Idempotency-Key` header, unique-indexed per user, returning the original order on
replay.

### P1-17. Products are hard-deleted
`ProductService.deleteProduct` removes the row; anything referenced by order history must survive.
**Fix:** soft delete via an `active` flag (same pattern already present on `User`), filtered out of
the public catalog. Also resolves P2-23.

---

## P2 — Security & operations

### P2-18. `User.active` is declared and never read
Deactivating a user does nothing — they still authenticate and place orders.
**Fix:** check it in `FirebaseAuthenticationFilter` / `MemberIdentityHandlerService.getLoggedInUser()`.

### P2-19. `emailVerified` is captured then ignored
`FirebasePrincipal.emailVerified` is read off the token and never used. Unverified emails can
register and order, and the confirmation mail goes to an unverified address.
**Fix:** require a verified email at registration and at order creation.

### P2-20. Filter-layer 401/403 bypass `ErrorHandler`
No `AuthenticationEntryPoint`/`AccessDeniedHandler` is configured, so unauthenticated calls get
Spring's default empty body instead of the app's `ErrorResponse` JSON — an inconsistent contract for
the frontend.

### P2-21. `FirebaseAuthenticationFilter` only catches `FirebaseAuthException`
`FirebaseAuthenticationFilter.java:76` — a transient network error reaching Google's cert endpoint
escapes the filter chain. Servlet filters run before the `DispatcherServlet`, so
`@RestControllerAdvice` never sees it and the client gets a raw container error page.
*(was KNOWN_ISSUES #6)*
**Fix:** catch `Exception`, log, clear the context, and write the standard JSON error shape.

### P2-37. Global `IllegalArgumentException` handler leaks raw messages and hides server bugs
`ErrorHandler.java:106-110` — the handler catches the *entire* `IllegalArgumentException`
hierarchy and echoes `exception.getMessage()` straight back as a 400. That's broader than the one
call site it was written for (`OrderService.applyStatusChange`, line 192): `NumberFormatException`,
`Enum.valueOf`, `UUID.fromString`, Spring's `Assert.notNull`/`PageRequest.of` bounds checks, and
Hibernate/Jackson edge cases all extend or throw `IllegalArgumentException`. Any of those now leak
raw JDK/Spring internals to the client and get logged at WARN instead of ERROR — silently downgrading
real server bugs out of 500-level alerting, and breaking the leak-hardening convention that
`handleGenericRuntimeError`/`handleGenericError` deliberately enforce (guarded by
`...returnsInternalServerErrorWithoutLeakingRawMessage` tests, which use a `password=hunter2` payload
to make the intent explicit). Not exploitable from any known call site today — no other code throws
`IllegalArgumentException` directly — so this is a latent hardening gap, not a live vulnerability.
*(ultrareview nit, not an MVP blocker.)*
**Fix:** throw a domain-specific `InvalidOrderStatusTransitionException` from `applyStatusChange` and
handle that instead of catching all of `IllegalArgumentException`.

### P2-23. Product-delete FK violation returns a nonsensical message
The FK violation is caught by the same `DataIntegrityViolationException` handler used for duplicate
names, which always returns *"A record with the same unique value already exists."*
*(was KNOWN_ISSUES #3)* — superseded if P1-17 (soft delete) lands.

### P2-24. N+1 queries on every order listing
`OrderResponse.from()` triggers a SELECT per order for `.getUser()` and per line item for
`.getProduct()` — both are `@ManyToOne` with no `fetch` set, so EAGER by default, with no
`JOIN FETCH` behind them. A page of 10 orders with 3 items each is ~50 queries instead of 1–2.
*(was KNOWN_ISSUES #5)*

**More urgent now:** with OSIV off, all of it runs inside one short transaction on a single
connection checkout, so the cost is concentrated rather than spread across the request.

**Fix:** `@EntityGraph` (or `JOIN FETCH`) on `OrderRepository.findAll`, `findById`, and
`getOrderByUserId`.

**Why this is the highest-leverage item in P2:** it closes two problems at once. Once the order
graph arrives fully loaded, there is *no lazy access left in the read path* — the
`@Transactional(readOnly = true)` boundaries on the three order read methods stop being
load-bearing and become belt-and-braces. Right now a single annotation is the only thing between
a working endpoint and a `LazyInitializationException`. See the OSIV decision note above.

### P2-25. No schema migrations
`ddl-auto=update` is the migration strategy. The first production schema change is a coin flip.
**Fix:** Flyway, baselined against the current schema, then `ddl-auto=validate`.

### P2-26. No Actuator / health endpoint
Cloud Run, Railway, ALB, and k8s all want a readiness probe. There isn't one.

### P2-27. No rate limiting
Public endpoints (`/products`, `/customer/register`) are unthrottled; no request-size caps.

### P2-28. Live secrets in a plaintext file
Brevo SMTP key, Firebase web API key, DB password in `application-local.properties`. Gitignored, so
not leaked to git, but they belong in env vars / a secret manager before deploy.

### P2-29. `/customer/orders` is unpaginated and returns 204 on empty
Every other list endpoint returns an empty page. A customer with 500 orders pulls all of them, and
the frontend has to special-case the shape.

### P2-30. `POST /orders` returns 200, not 201

### P2-34. Four read methods still lack `@Transactional(readOnly = true)`
`ProductService.findById`, `ProductService.findAllProduct`, `UserService.getCustomer`,
`UserService.getAllCustomers`. They work today only because `User` and `Product` have no
associations, so each repository call runs in its own session from `SimpleJpaRepository` and the
detached entities are fully materialized. Nothing breaks — but it violates rule 1 of the OSIV
decision above, and it costs:

- `getCustomer` does two round trips (`getLoggedInUser()`, then `findById`) in **two separate
  sessions and connection checkouts**.
- `getAllCustomers` and `findAllProduct` run the count query and the content query in **two separate
  transactions**, so `totalElements` can disagree with `content` if a row is inserted between them.

**Fix:** add `@Transactional(readOnly = true)` to all four. Also lets the JDBC driver flag the
connection read-only. ~5 minutes, and it establishes the rule uniformly so the question stops
coming up.

### P2-36. Owner-or-admin authorization block duplicated in `OrderService`
`getOrderById` (`OrderService.java:75-78`) and `cancelOrder` (`OrderService.java:175-178`) contain a
byte-for-byte-identical authz check (fetch current user, compare role/owner, throw
`AccessDeniedException`) — only the exception message differs. `MemberIdentityHandlerService`
already centralizes the admin-only variant via `requireAdmin()`; a sibling
`requireOwnerOrAdmin(ownerId, resourceName)` would collapse both call sites to one line and remove
the risk of the two rules drifting apart (e.g. a future SUPPORT role added to one check and
forgotten in the other). *(ultrareview nit, not an MVP blocker — quality/reuse only.)*

### P2-35. No regression guard for the lazy-loading contract
The entire test suite is Mockito unit tests (80 tests, 0 failures). The only two Spring-context
tests — `SujalamAgroFoodsBackendApplicationTests` and `ProductStockConcurrencyTest` — are
`@Disabled` pending Docker. Mocked repositories return plain POJOs, never Hibernate proxies, so the
suite is **structurally incapable** of throwing `LazyInitializationException`.

Consequence: the moment someone adds a lazy association or returns an entity from a controller,
nothing catches it. The OSIV decision above is currently enforced by code review alone.

**Fix:** one `@SpringBootTest` + `MockMvc` integration test hitting `GET /order/{id}`,
`GET /orders`, and `GET /customer/orders` against a real DB. The Testcontainers config is already
written; CI's `ubuntu-latest` has Docker, so the two `@Disabled` tests can likely be re-enabled in
the same pass.

---

## P3 — Hygiene

### P3-31. No README, no API documentation
No springdoc/OpenAPI. `spring-restdocs` is on the classpath but generates nothing. Frontend devs are
reading Java to learn the contract.

### P3-32. No `.env.example` or documented run instructions
Onboarding a second developer requires you in the loop.

### P3-33. No staging environment or post-deploy smoke test
