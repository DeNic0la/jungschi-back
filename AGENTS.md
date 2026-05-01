# AGENTS.md – jungschi-back

Quarkus (Java 21) REST backend for a Swiss youth-camp registration system. Uses Gradle, Hibernate ORM with Panache (active-record pattern), Flyway migrations, PostgreSQL (prod) / H2 (test), and Keycloak OIDC.

## Key Commands

```bash
./gradlew quarkusDev   # Dev mode with live reload; auto-starts Keycloak on port 8180
./gradlew test         # Run all tests (uses H2 in-memory DB automatically)
./gradlew build        # Produces build/quarkus-app/quarkus-run.jar
```

Dev UI: http://localhost:8080/q/dev/ (only in dev mode)

## Architecture

```
ch.denic0la
├── model/            # PanacheEntity domain objects (active-record, public fields)
├── controller/       # User-facing REST endpoints (/api/*)
├── team/             # Team-admin endpoints (/api/team/*) – @RolesAllowed("Jungschiteam")
└── CurrentUserProvisioningService.java  # OIDC → AppUser provisioning
```

**Domain model:**
- `AppUser` (PK = `oidcSubject`) → owns many `Participant`s
- `Participant` → has one `HealthStats`, one `CampStats`, many `IntoleranceSelection`s
- `GlobalIntoleranceDefinitions` – seeded reference data (food intolerances, allergies)

## Critical Patterns

**User provisioning:** Every mutating endpoint calls `provisioningService.ensureCurrentUser()`, which auto-creates an `AppUser` on first login from JWT/UserInfo claims. Read-only endpoints use `getCurrentUser()` (throws if user doesn't exist). Never access `AppUser` directly without going through this service.

**Data isolation:** All user data is scoped to `oidcSubject`. Queries always filter by the current user's OIDC subject, e.g.:
```java
Participant.list("user.oidcSubject", user.oidcSubject)
```

**Two access tiers:**
- Regular users: `controller/` package – data scoped to self
- Team admins: `team/` package – `@RolesAllowed("Jungschiteam")` – sees all participants

**DTOs as records:** Each controller defines its own DTOs as inner Java records. No shared DTO classes.

**Security is active in dev mode:** `quarkus.security.auth.enabled-in-dev-mode=true`. Dev Keycloak uses `src/main/resources/quarkus-realm.json`.

## Configuration

- `src/main/resources/application.yaml` – container image / packaging settings
- `src/main/resources/application.properties` – datasource, OIDC, Flyway, CORS
  - `%test.*` profile uses H2 + OIDC disabled
  - `%dev.*` profile auto-starts Keycloak DevServices
  - `%prod.*` profile reads `KEYCLOAK_URL` and `CORS_ORIGINS` env vars

## Database Migrations

Flyway SQL files in `src/main/resources/db/migration/`. Follow existing naming: `V{n}__{Description}.sql`. Schema strategy is `validate` (Hibernate does not auto-DDL).

## Testing

Tests use `@QuarkusTest` + `@TestSecurity` + `@OidcSecurity` to mock auth without a running Keycloak:
```java
@TestSecurity(user = "test-user", roles = {"user"})
@OidcSecurity(claims = {
    @Claim(key = "sub", value = "some-oidc-sub"),
    @Claim(key = "preferred_username", value = "testuser")
})
```
Use a unique `sub` value per test class to avoid cross-test data leakage (H2 is shared per test run). RestAssured (`given()...when()...then()`) is the assertion style.

## API Surface

| Path | Who |
|---|---|
| `GET/POST/PUT/DELETE /api/participants/**` | Authenticated user (own data) |
| `PUT /api/participants/{id}/health-stats` | Authenticated user |
| `PUT /api/participants/{id}/camp-stats` | Authenticated user |
| `GET/POST/DELETE /api/participants/{id}/intolerances` | Authenticated user |
| `GET /api/global-definitions/food-intolerances` | Any authenticated |
| `GET /api/global-definitions/allergies` | Any authenticated |
| `GET/PUT /api/users/me` | Authenticated user |
| `GET /api/team/participants/**` | `Jungschiteam` role only |

