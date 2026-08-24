# Issues To Fix

Prioritized gap list from the MVP readiness scan. Not tracked in git — local working notes only.

Replaces the earlier `KNOWN_ISSUES.md`; every still-open item from that file is carried over below
and cross-referenced as `(was KNOWN_ISSUES #n)`.

---

## Minimum cut to MVP

Shortest line to "a frontend can be built against this and a real customer can order":

**P0-1 CORS → P0-2 env config → P0-3 Dockerfile/deploy → P0-4 admin bootstrap → P3-31 OpenAPI docs.**

P2 is a hardening pass for the week after launch, except **P2-18** and **P2-19**, which are a few
lines each and should be folded into the P0 work.

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

### P1-10. Order status model is too thin
`PENDING → COMPLETED/CANCELED` only. No CONFIRMED/SHIPPED/OUT_FOR_DELIVERY, so there is nothing to
render on an order-tracking screen.
**Fix:** extend `OrderStatus` and its `canTransitionTo()` table.

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
