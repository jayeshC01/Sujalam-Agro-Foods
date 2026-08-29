# Issues To Fix

Prioritized gap list from the MVP readiness scan. Not tracked in git — local working notes only.


## Minimum cut to MVP

Shortest line to "a frontend can be built against this and a real customer can order":

**P0-1 CORS → P0-2 env config → P0-3 Dockerfile/deploy → P0-4 admin bootstrap → P3-31 OpenAPI docs.**

P2 is a hardening pass for the week after launch, except **P2-19**, which is a few
lines and should be folded into the P0 work.

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
---

## P2 — Security & operations

### P2-19. `emailVerified` is captured then ignored
`FirebasePrincipal.emailVerified` is read off the token and never used. Unverified emails can
register and order, and the confirmation mail goes to an unverified address.
**Fix:** require a verified email at registration and at order creation.

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

### P2-25. No schema migrations
`ddl-auto=update` is the migration strategy. The first production schema change is a coin flip.
**Fix:** Flyway, baselined against the current schema, then `ddl-auto=validate`.

### P2-27. No rate limiting
Public endpoints (`/products`, `/customer/register`) are unthrottled; no request-size caps.

### P2-28. Live secrets in a plaintext file
Brevo SMTP key, Firebase web API key, DB password in `application-local.properties`. Gitignored, so
not leaked to git, but they belong in env vars / a secret manager before deploy.

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
