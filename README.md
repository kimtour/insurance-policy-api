# Insurance Policy API

A Spring Boot REST API for managing insurance policies, built as a practical backend engineering project covering API design, validation, persistence, security, automated testing, containerization, Kubernetes, observability, and CI.

## Features

- Insurance policy CRUD operations
- Request and business-rule validation
- Duplicate policy-number protection
- PostgreSQL persistence with Flyway migrations
- JWT authentication and stateless Spring Security
- BCrypt password hashing
- Centralized exception handling
- SLF4J and Logback logging
- Spring Boot Actuator health groups
- JUnit, Mockito, and MockMvc tests
- Docker multi-stage build and Docker Compose
- Kubernetes deployments, services, probes, and persistent PostgreSQL storage
- GitHub Actions CI

## Technology Stack

| Area | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1.1 |
| API | Spring Web MVC |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL 17 |
| Migrations | Flyway |
| Test database | H2 |
| Security | Spring Security, JWT / OAuth2 Resource Server |
| Validation | Jakarta Validation |
| Build and tests | Maven, JUnit, Mockito, MockMvc |
| Monitoring | Spring Boot Actuator |
| Containers | Docker, Docker Compose |
| Orchestration | Kubernetes |
| CI | GitHub Actions |

## Architecture

```text
Client -- HTTP / JWT --> Controllers --> Services --> Spring Data JPA --> PostgreSQL
                              |              |
                              +--> validation and exception handling
```

The application follows a controller-service-repository architecture. Controllers handle HTTP concerns, services enforce business rules and map DTOs, repositories persist entities, and Flyway owns the database schema.

## Policy Model

A policy contains `id`, `policyNumber`, `customerName`, `customerEmail`, `premium`, `status`, `startDate`, and `endDate`. Supported statuses are `PENDING`, `ACTIVE`, `CANCELLED`, and `EXPIRED`. Monetary values use `BigDecimal` and PostgreSQL `NUMERIC(12,2)`.

## API Endpoints

### Authentication

```text
POST /api/auth/login
```

```json
{"username":"sam","password":"password123"}
```

Successful login returns a Bearer JWT. Invalid credentials return `401` with `{"error":"Invalid username or password"}`.

### Protected Policy API

```text
POST   /api/policies
GET    /api/policies
GET    /api/policies/{id}
PUT    /api/policies/{id}
DELETE /api/policies/{id}
```

Protected requests require `Authorization: Bearer <token>`. Successful deletion returns `204 No Content`.

## Validation and Persistence

Request validation checks required fields, email format, positive premium, dates, and status. Service rules require `endDate` to be after `startDate` and `policyNumber` to be unique. Database constraints provide a final protection layer for uniqueness, valid status values, dates, and positive premiums.

Flyway migrations are stored in `src/main/resources/db/migration`. `V1__create_policy_table.sql` creates the policy schema, while Hibernate uses `ddl-auto=validate` and does not mutate runtime schemas.

## Security

The login flow authenticates the in-memory development user through `AuthenticationManager`, then issues a signed JWT. Protected requests are validated by the OAuth2 resource-server support. Sessions are stateless, so multiple API replicas can serve requests without shared session storage. Production deployments must inject `JWT_SECRET`; no production fallback is configured.

## Configuration

Production configuration reads `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` from the environment. Local PostgreSQL settings belong in the ignored `src/main/resources/application-local.properties` file:

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Tests use isolated H2 and disable Flyway in `src/test/resources/application.properties`.

## Automated Tests

The suite includes service unit tests, repository mocking, MockMvc controller tests, validation tests, security tests, and an application-context test.

```bash
./mvnw clean test
```

The current suite contains 21 tests.

## Running Locally

Requirements: Java 25 and PostgreSQL 17.

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
export PATH="$JAVA_HOME/bin:$PATH"
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
curl http://localhost:8080/actuator/health
```

## Docker Compose

```bash
docker compose build
docker compose up -d
docker compose ps
docker compose logs insurance-api
docker compose down
```

The API connects to the Compose PostgreSQL service at `postgres:5432`; the database uses a persistent Docker volume.

## Kubernetes

Manifests are under `k8s/`. They define the namespace, API deployment and service, PostgreSQL deployment and service, PVC, ConfigMap, health probes, and secret templates.

Create development secrets locally without committing them:

```bash
kubectl apply -f k8s/secrets.local.yaml
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/api-config.yaml
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/api.yaml
kubectl get pods -n insurance
```

The API normally runs two replicas while PostgreSQL is a single persistent development workload. The API uses dedicated probes:

```text
startupProbe  -> /actuator/health/liveness
readinessProbe -> /actuator/health/readiness, including database readiness
livenessProbe -> /actuator/health/liveness
```

For local access:

```bash
kubectl port-forward service/insurance-api 8080:8080 -n insurance
curl -i http://localhost:8080/actuator/health/liveness
curl -i http://localhost:8080/actuator/health/readiness
```

`k8s/secrets.example.yaml` contains placeholders suitable for reference. `k8s/secrets.local.yaml` is ignored and must never contain production credentials.

## CI Pipeline

GitHub Actions runs on pushes and pull requests targeting `main`:

```text
Checkout -> Java 25 -> Maven tests -> Maven package -> Docker build
```

The workflow is `.github/workflows/ci.yml`. A failed test, package, or Docker build fails CI. Deployment is intentionally kept outside this workflow so CI and CD remain separate concerns.

## Engineering Decisions

- **Flyway plus Hibernate validation:** migrations are deterministic, versioned, auditable, and reproducible; Hibernate verifies mappings rather than changing production schemas.
- **BigDecimal:** avoids binary floating-point rounding errors for money.
- **DTOs:** keeps the HTTP contract separate from persistence entities.
- **JWT:** stateless authentication works naturally with multiple API replicas.
- **PostgreSQL plus H2:** runtime behavior uses PostgreSQL while tests remain fast and isolated.
- **Docker multi-stage build:** build tooling stays out of the runtime image.
- **Kubernetes replicas:** improves API availability in this development deployment; production PostgreSQL would normally be managed outside a simple Kubernetes Deployment.

## Production Improvements

Further hardening could include a production identity provider, refresh-token policy, TLS and ingress, rate limiting, OpenAPI documentation, structured centralized logging, metrics and tracing, managed PostgreSQL, vulnerability scanning, registry publishing, automated deployment, autoscaling, network policies, and external secret management.

## Interview Walkthrough

> I built an insurance policy management API with Java 25 and Spring Boot. It exposes secured CRUD endpoints using a controller-service-repository architecture. PostgreSQL is the runtime database, Flyway owns deterministic schema migrations, and tests use isolated H2. I added validation at the API and service layers, JWT authentication with Spring Security, centralized exception handling, logging, and 21 JUnit, Mockito, and MockMvc tests. The application uses a multi-stage Docker build, runs with PostgreSQL through Docker Compose, and deploys locally to Kubernetes with multiple API replicas, persistent storage, dedicated health probes, and secret injection. GitHub Actions runs tests, packages the application, and validates the Docker build on every push to `main`.

Useful follow-ups include why JWT, why two replicas, what happens when PostgreSQL fails, why BigDecimal, how to deploy to a cloud provider, and what would change for production.

## Repository

[https://github.com/kimtour/insurance-policy-api](https://github.com/kimtour/insurance-policy-api)
