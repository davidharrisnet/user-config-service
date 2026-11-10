# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Layout

The git root is a thin wrapper (README, LICENSE, `.gitignore`); the actual Gradle project lives in the nested `user-config-service/` directory. Run all Gradle commands from there.

This is a freshly generated Spring Initializr skeleton: the only code is `DemoApplication` and a `contextLoads` test. Gradle project name (`settings.gradle.kts`), package (`com.example.demo`) and `spring.application.name` are still the Initializr defaults (`demo`), not `user-config-service`.

## Stack

- Java 21 (Gradle toolchain), Gradle 9.8 via wrapper, Kotlin DSL build script
- Spring Boot 4.1 — note Boot 4 starter names differ from Boot 3 (`spring-boot-starter-webmvc`, `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`), and auto-configuration classes moved to per-module packages (e.g. `org.springframework.boot.jdbc.autoconfigure`)
- Spring Data JPA (Hibernate) with SQLite (`org.xerial:sqlite-jdbc`) as the runtime driver
- Lombok (compile-only + annotation processor, for both main and test)

## Commands

A `Makefile` at the git root wraps the common Gradle commands (`make` lists targets; `make build-notest` skips tests; `make test TEST=Class.method` runs one test). Direct Gradle equivalents, from `user-config-service/`:

```bash
./gradlew build                     # compile + test + package
./gradlew test                      # all tests
./gradlew test --tests 'com.example.demo.DemoApplicationTests'                # one class
./gradlew test --tests 'com.example.demo.DemoApplicationTests.contextLoads'   # one method
./gradlew bootRun                   # run the app
```

Test reports: `build/reports/tests/test/index.html`; raw results with stack traces in `build/test-results/test/*.xml`.

## Persistence

- SQLite file `user-config.db` (created in the working directory; `*.db` is gitignored). URL carries `?foreign_keys=true` because SQLite enforces FKs only when enabled per connection.
- **Schema is owned by `src/main/resources/schema.sql`, not Hibernate** (`ddl-auto=none`, `spring.sql.init.mode=always`). Hibernate's `SQLiteDialect` cannot `ALTER TABLE`, so with `ddl-auto` it silently drops foreign keys and multi-column unique constraints. When adding or changing an entity, update `schema.sql` to match; `CREATE TABLE IF NOT EXISTS` means changes to existing tables need a manual migration or a deleted `.db` file.
- Reference data (the `fruit` table, fixed ids 1–6) is seeded by `src/main/resources/data.sql` using `INSERT OR IGNORE`, so it is idempotent on every startup. A user's fruit selection lives in the `user_settings_fruit` join table (SQLite has no array type), mapped as an ordered `@ManyToMany` `List` on `UserSettings` with `@OrderColumn` → `position` column (0-based, PK is `(user_settings_id, position)`). `findAllById` does not preserve argument order; build ordered lists explicitly.
- Data rules: **no column is nullable** (enforced in both `schema.sql` and the entities). Every `User` owns exactly one `UserSettings` (created in the `User` constructor, cascaded on save). Create users through `UserService.createUser`, which applies the defaults: `results_size` 20, `font_size` 12, and all fruits in `data.sql` (id) order. `new User(...)` alone gives an empty fruit list.
- Fruit model: **one shared fruit list for all users; each user has only a personal ordering of it** (always every active fruit, never a subset). Fruits are never deleted: retire with `fruit.active = 0`, add with a new never-reused id. `UserService` reconciles on read (retired fruits hidden, newly added ones appended in id order) and on `updateFruitOrder` (submitted ids must equal the active set exactly, else `FruitListOutOfDateException` → 409; retired fruits keep their saved slot so reactivation restores them in place).
- REST (`UserSettingsController`, keyed by numeric user id): `GET /users` (list, via `UserSummary` constructor projection so settings aren't loaded per user), `POST /users` (create, 201), `GET /users/{id}/settings`, `POST /users/{id}/result_size`, `/font_size`, `/fruits` (`{"fruit_ids": [...]}`). Every settings call returns the full settings JSON. Service methods return a fully loaded `SettingsSnapshot` because `spring.jpa.open-in-view=false`; don't touch lazy associations in the controller. Errors are `ProblemDetail`: 400 invalid input, 404 unknown user, 409 stale fruit list or duplicate username/email. No auth yet.
- Swagger UI (springdoc-openapi 3.x, the Boot 4 line): `make run`, then http://localhost:8080/swagger-ui.html; OpenAPI JSON at `/v3/api-docs`. Request examples come from `@Schema(example=...)` on the request records.
- SQLite constraint violations surface as `JpaSystemException`, not `DataIntegrityViolationException`; catch `DataAccessException`.
- Repository tests use `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` (there is no embedded DB to swap in) and point `spring.datasource.url` at `build/test.db`.
