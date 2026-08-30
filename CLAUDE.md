# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

EVE Ref (https://everef.net/) is a reference site for the game EVE Online. This repo holds:

- A **Java backend** (`src/`) — a multi-command CLI application that scrapes EVE's ESI API and other
  sources, builds reference/market/industry data, and serves an HTTP API.
- A **Nuxt 3 frontend** (`ui/`) — the public website, consumes the API via generated OpenAPI clients.
- **VuePress docs** (`docs/`) — published to docs.everef.net.
- A **`database` Gradle subproject** — Flyway migrations + jOOQ code generation.

## Build & Test

The `Makefile` is the entry point for most tasks; it wraps Gradle and the `ui/` npm scripts.

Java build requires jOOQ classes to exist first. `make` targets that need them depend on
`generate-database`, which runs `./gradlew database:compileJava database:generateJooq`. **Run
`make generate-database` before compiling or testing Java directly**, or the build fails on missing
generated jOOQ sources.

- Full test suite: `make test` (Java + UI). Java only: `make test-java`. UI only: `make test-ui`.
- Single Java test: `./gradlew test --tests "com.autonomouslogic.everef.cli.SomeTest"`
  (run `make generate-database` first).
- Format (required before committing): `make format` — runs `./gradlew spotlessApply` (palantir Java
  format, **indentation is tabs**, not spaces) plus UI eslint `--fix`.
- Lint check: `make lint` (`spotlessCheck` + UI eslint).
- Build distribution: `make dist` (`./gradlew distTar`). Docker image: `make docker`.

### UI & docs

The UI and docs run in Docker via the Makefile so a local Node install isn't required:
`make init-ui`, `make dev-ui` (serves on :3000), `make build-ui`, `make dev-docs`.
`make specs` regenerates the OpenAPI/reference-data specs from Java and then the UI's API client.

### Postgres

`make postgres-start` / `postgres-stop` run a local Postgres 15 container.
`make postgres-migrate-test` starts it, runs Flyway migrations, and stops it.

## Architecture

### Command dispatch

Entry point is `Main.java`. It builds the Dagger graph (`MainComponent.create()`), runs everything on
a **virtual thread**, and delegates to `CommandRunner`.

`CommandRunner.runCommand(args)` takes exactly one argument — the command name — and maps it to a
`Command` via a large `switch` in `createCommand(...)`. Each command is a Dagger `Provider<>` injected
into `CommandRunner`. **Adding a command means: create a class implementing `Command`, inject its
`Provider` into `CommandRunner`, and add a `case` to the switch.** Command names are kebab-case
(e.g. `scrape-market-orders`); the classes live under `src/main/java/com/autonomouslogic/everef/cli/`.

`Command` (in `cli/Command.java`) is a `Runnable` with a `run()` method and a default `getName()`.

### Decorators

Before running, every command is wrapped by decorators in `cli/decorator/`, applied in
`CommandRunner.decorateCommand`: `HealthcheckDecorator` (pings a healthcheck URL, plus periodic pings
while running), `SlackDecorator` (start/failure notifications), `SentryDecorator` (error capture).
Cross-cutting run-lifecycle concerns belong here, not in individual commands.

### Concurrency

The codebase is mid-migration **from RxJava3 (`Completable`) toward synchronous code on virtual
threads**. Commands now implement `run()` synchronously and use `.blockingAwait()` where RxJava chains
remain. `Main` installs an RxJava global error handler that tolerates network errors and fatals on
others. Prefer plain synchronous code + virtual threads for new work; use `ConcurrentHashMap` and
atomic check-and-put where `VirtualThreads.parallel()` shares state.

### Dependency injection

Dagger 2, annotation-processor based. `MainComponent` is the root component. Modules live in
`inject/` (`AwsModule`, `S3Module`, `EsiModule`, `OkHttpModule`, `JacksonModule`, etc.). Most services
and commands are `@Singleton` with `@Inject`-annotated constructors.

### Configuration

All config goes through `config/Configs.java` — static `Config<T>` fields (from
`com.autonomouslogic.commons`) read from environment variables, with optional defaults and
`getRequired()`. `local.env` holds local dev values. Add new tunables as fields here rather than
reading env vars ad hoc.

### Data layer

- `database` subproject: Flyway migrations are **Java-based** under
  `database/.../db/migrations/` (e.g. `V1__InitialSetup.java`); jOOQ generates typed access from the
  migrated schema. Supports both Postgres (prod) and H2.
- `db/` package: `DbAccess`, `DbAdapter`, and DAOs (`BaseDao`, `MarketHistoryDao`).
- `mvstore/`: H2 MVStore used for large local key-value/caching during scrapes.
- `s3/`, AWS SDK v2: data artifacts published to S3; DynamoDB used for some lookups.

### Reference data & API

- `refdata/`: POJO models (`InventoryType`, `Blueprint`, `Region`, etc.) for EVE static data, built by
  the `build-ref-data` command from ESI, the SDE, and Hoboleaks. Spec in `spec/reference-data.yaml`.
- `api/`: the `api` command runs a **Helidon** webserver exposing endpoints (search, industry cost,
  etc.). API spec is `spec/eve-ref-api.yaml`; models are generated via the OpenAPI generator into the
  `openapi` package.
- Jackson is the serialization workhorse (JSON/YAML/CSV). Note the build pins **two** Jackson lines:
  Jackson 3 (`tools.jackson.*`) as primary, and Jackson 2 (`com.fasterxml.jackson.*`) kept only for
  swagger-core and the dynamo-mapper — keep imports on the right one.

## Conventions

- Java 21, Lombok (`@Log4j2`, `@Inject`, `@SneakyThrows` etc. are used widely).
- Logging is Log4j2 → SLF4J; JSON layout available.
- Commits follow Conventional Commits (`fix:`, `feat:`, etc.) and semantic-release; PR numbers are
  appended to commit subjects. Commit messages must be a **single line** — no body. **Never** add a
  `Co-Authored-By: Claude` trailer (or any co-author trailer).
- Run `make format` before committing — CI enforces `spotlessCheck` and tabs.
- Keep this `CLAUDE.md` up-to-date: whenever you change anything it already documents (build steps,
  architecture, conventions, commands, etc.), update the relevant section in the same change so the
  docs never drift from the code.
