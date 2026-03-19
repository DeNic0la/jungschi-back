# Project Guidelines

## Project Overview
This is the backend for **jungschi-back**, a Quarkus-based application (likely for Swiss youth groups, "Jungschi"). It manages users, participants, health statistics, and intolerance selections for camps or events.

### Tech Stack:
- **Language**: Java 21
- **Framework**: [Quarkus](https://quarkus.io/) (Supersonic Subatomic Java)
- **Build Tool**: Gradle
- **Database**: PostgreSQL (with [Flyway](https://quarkus.io/guides/flyway) for migrations)
- **Persistence**: Hibernate ORM with [Panache](https://quarkus.io/guides/hibernate-orm-panache) (Active Record pattern)
- **Authentication**: Keycloak / OIDC (Keycloak realm export/config for dev included)
- **API**: JAX-RS (Jakarta REST) with RESTEasy Reactive and Jackson for JSON.
- **Monitoring**: SmallRye Health.

## Project Structure
- `src/main/java/ch/denic0la/controller`: REST API controllers. DTOs (using Java Records) are often defined within the controller class if they are endpoint-specific.
- `src/main/java/ch/denic0la/model`: Panache entities. Using public fields as per the Panache Active Record pattern.
- `src/main/resources`:
    - `application.yml` / `application.properties`: Configuration.
    - `db/migration`: Flyway SQL migration files (V1__, V2__, etc.).
    - `quarkus-realm.json`: Keycloak realm export/configuration for development.
- `src/main/docker`: Dockerfiles for JVM, Native, and Micro builds.
- `src/test/java/ch/denic0la`: JUnit tests using `@QuarkusTest` and REST Assured.

## Instructions for Junie

### Coding Standards:
- **Entities**: Follow the Panache Active Record pattern. Use `PanacheEntity` for auto-generated IDs or `PanacheEntityBase` if custom IDs are needed (like `oidc_subject` for `AppUser`). Fields should be `public`.
- **Controllers**: Use JAX-RS annotations. Annotate with `@Path`, `@Produces(MediaType.APPLICATION_JSON)`, and `@Consumes(MediaType.APPLICATION_JSON)`.
- **Transactions**: Use `@Transactional` on methods that perform database write operations.
- **DTOs**: Use Java `record` for DTOs.
- **Migrations**: If you change the model/entities, you **MUST** create a new Flyway migration script in `src/main/resources/db/migration` using the next version number (e.g., `V3__...sql`).

### Running Tests:
- Run all tests: `./gradlew test`
- Run a specific test: `./gradlew test --tests "ch.denic0la.controller.UserControllerTest"`
- **Requirement**: Always run tests before submitting any code changes to ensure no regressions.

### Building the Project:
- Build the project: `./gradlew build`
- Build should pass before any submission.

### Development Mode:
- Quarkus Dev Mode can be started with `./gradlew quarkusDev`. This is useful for manual verification but not strictly necessary for Junie unless requested.

## Style Preferences:
- Use English for code (classes, variables, etc.), though the project context might be Swiss German.
- Follow existing indentation (4 spaces) and naming conventions (camelCase for variables/methods, PascalCase for classes).
- Use Jakarta annotations for REST and Persistence.
