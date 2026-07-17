# MES Project Agent Instructions

## Project Context

This repository is a computer monitor manufacturing MES.

The system contains production orders, production lines, equipment,
materials, workers, dispatch records, quality records and production reports.

The AI feature is implemented with Spring AI and controlled MES tools.

## Repository Inspection

Before making changes:

1. Inspect the repository structure.
2. Read pom.xml or build.gradle.
3. Identify the Spring Boot version, Java version and Spring Security setup.
4. Identify controller, service, mapper/repository, entity and DTO conventions.
5. Identify database migration tooling.
6. Identify frontend framework and API request conventions.
7. Run existing tests before changing code.

Do not upgrade Spring Boot, Java, Spring Security, MyBatis, JPA or database
dependencies unless the task explicitly requires it.

## Architecture Rules

Use this dependency direction:

Controller
-> Application Service
-> Domain Service
-> Repository or Mapper

AI tools must call application/domain services.

AI tools must never:

- execute arbitrary SQL;
- accept SQL supplied by the model;
- execute shell commands;
- access the database through a generic database tool;
- bypass Spring Security;
- modify production data without explicit approval;
- directly call Mapper or Repository for write operations;
- expose API keys, passwords or tokens;
- claim that an operation succeeded when it failed.

## MES AI Safety Rules

Read-only operations may execute automatically.

Write operations must follow:

1. generate preview;
2. persist a pending plan;
3. display affected resources;
4. require explicit user confirmation;
5. recheck authorization;
6. recheck current factory state;
7. execute inside a transaction;
8. write an audit log.

The initial chat request must not expose production write tools to the LLM.

Confirmed plans are executed through a dedicated REST endpoint and domain
service, not directly by the language model.

Every write operation must support:

- permission checks;
- idempotency;
- optimistic locking or snapshot version checking;
- transaction rollback;
- audit logging;
- clear error reporting.

## Coding Conventions

Follow existing package names and project conventions.

Prefer:

- constructor injection;
- immutable request/response records where compatible;
- Bean Validation;
- typed enums instead of magic strings;
- explicit DTOs instead of exposing entities;
- structured exceptions and unified API error responses;
- small focused classes;
- Chinese business descriptions where the existing project uses Chinese;
- English class, method and field names.

Do not create duplicate service layers when an existing service can be reused.

## Testing

Every feature must include relevant tests.

At minimum:

- unit tests for dispatch rules;
- permission tests;
- transaction rollback tests;
- idempotency tests;
- stale snapshot tests;
- tool input validation tests;
- tests ensuring tools cannot bypass domain services.

Use mocks for LLM API calls in automated tests.

Never require a real paid model API to run the standard test suite.

## Database

Use the repository's existing migration mechanism.

Do not edit an already-applied migration.

Do not delete or rename existing columns without explicit approval.

New AI-related tables must use the same naming, primary-key, timestamp and
soft-delete conventions as the rest of the project.

## Secrets

API keys must come from environment variables or the project's existing
secret-management mechanism.

Never commit:

- real API keys;
- database passwords;
- JWT secrets;
- production URLs;
- private certificates.

## Definition of Done

Before declaring a task complete:

1. run the relevant backend tests;
2. run the frontend build if frontend code changed;
3. inspect the git diff;
4. summarize modified files;
5. list database migrations;
6. list security implications;
7. list remaining risks;
8. do not commit unless explicitly asked.