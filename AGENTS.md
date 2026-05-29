# AGENTS.md - jungschi-back

Quarkus (Java 21) REST backend for a Swiss youth-camp registration system. Uses Gradle, Hibernate ORM with Panache (active-record pattern), Flyway migrations, PostgreSQL (prod) / H2 (test), and Keycloak OIDC.

## Key Commands

```bash
./gradlew quarkusDev   # Dev mode with live reload; uses Postgres on 54329 and Keycloak on 8180
./gradlew test         # Run all tests (uses H2 in-memory DB automatically)
./gradlew build        # Produces build/quarkus-app/quarkus-run.jar
```

Dev UI: http://localhost:8080/q/dev/ (only in dev mode)
Local database from the parent repo: `docker compose up -d postgres`

## Architecture

```
ch.denic0la
├── model/            # PanacheEntity domain objects (active-record, public fields)
├── controller/       # User-facing REST endpoints (/api/*)
├── team/             # Team-admin endpoints (/api/team/*) – @RolesAllowed("Jungschiteam")
└── CurrentUserProvisioningService.java  # OIDC → AppUser provisioning
```

**Domain model:**
- `AppUser` (PK = email) stores OIDC subject, profile claims, roles, and last-seen data
- `Household` links primary and secondary contacts and owns participant access
- `Participant` belongs to a `Household` and has one `HealthStats`, one `CampStats`, and many `IntoleranceSelection`s
- `Camp`, `Room`, `SignUp`, `CampParticipant`, and `CampParticipantMedication` model camp registration
- `GlobalIntoleranceDefinitions` – seeded reference data (food intolerances, allergies)

## Critical Patterns

**User provisioning:** Mutating user-facing endpoints call `provisioningService.ensureCurrentUser()`, which creates or updates an `AppUser` from JWT/UserInfo claims. Read-only endpoints may use `getCurrentUser()` and should fail clearly if the user does not exist.

**Household access:** Participant data is scoped through `Household`, not direct user ownership. Use `CurrentUserProvisioningService` helpers such as `findHouseholdForContact`, `isHouseholdContact`, `canReadParticipant`, and `canWriteParticipant` instead of open-coded checks.

```java
if (!provisioningService.canWriteParticipant(participant, currentUser)) {
    throw new NotFoundException();
}
```

**Roles:**
- `guardian`: regular family/contact user.
- `ADMIN`: can read/write participant data and administer broadly.
- `Jungschiteam`: can read team participant views.
- `Sanitaet`: can read health-relevant participant data.

Keep role spelling aligned with `src/main/resources/dev-realm.json`.

**Participant creation:** Creating a participant currently ensures a household for the current user and attaches the participant to that household.

**Team/admin routes:** Team-facing endpoints live in `team/` and are role-gated, for example:
```java
@RolesAllowed({"Jungschiteam", "ADMIN", "Sanitaet"})
```

**DTOs as records:** Each controller defines its own DTOs as inner Java records. No shared DTO classes.

**Security is active in dev mode:** `quarkus.security.auth.enabled-in-dev-mode=true`. Dev Keycloak uses `src/main/resources/dev-realm.json`.

## Configuration

- `src/main/resources/application.yaml` – container image / packaging settings
- `src/main/resources/application.properties` – datasource, OIDC, Flyway, CORS
  - `%test.*` profile uses H2 + OIDC disabled
  - `%dev.*` profile auto-starts Keycloak DevServices
  - `%prod.*` profile reads `KEYCLOAK_URL` and `CORS_ORIGINS` env vars

## Database Migrations

Flyway SQL files live in `src/main/resources/db/migration/`. Follow the current versioned naming pattern, keep migrations append-only, and keep SQL aligned with Hibernate validation. Hibernate schema strategy is `validate`; it does not auto-create production schema.

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
| `GET /api/camps/**` | Authenticated user, role-aware visibility |
| `GET /api/rooms/**` | Authenticated user, role-aware visibility |
| `GET /api/global-definitions/food-intolerances` | Any authenticated |
| `GET /api/global-definitions/allergies` | Any authenticated |
| `GET/PUT /api/users/me` | Authenticated user |
| `GET /api/team/participants/**` | `Jungschiteam`, `ADMIN`, or `Sanitaet` |

