# Insurance Policy API

A Spring Boot REST API for managing insurance policies. The project demonstrates API design, validation, PostgreSQL persistence, JWT security, automated testing, Flyway migrations, Docker, Kubernetes health management, and GitHub Actions CI.

## Technology

- Java 25
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA / Hibernate
- PostgreSQL 17
- H2 for automated tests
- Flyway
- Spring Security
- JWT / OAuth2 Resource Server
- JUnit, Mockito, MockMvc
- Docker and Docker Compose
- Kubernetes
- GitHub Actions

## Architecture

```text
Client
  |
  | HTTP + JWT
  v
Controller
  |
  v
Service
  |
  v
Repository
  |
  v
PostgreSQL
```

The API is intentionally layered so HTTP concerns, business rules, and persistence remain separate.

## Policy API

All policy endpoints require a Bearer token.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/auth/login` | Obtain a JWT |
| POST | `/api/policies` | Create a policy |
| GET | `/api/policies` | List policies |
| GET | `/api/policies/{id}` | Get one policy |
| PUT | `/api/policies/{id}` | Update a policy |
| DELETE | `/api/policies/{id}` | Delete a policy |

Example policy request:

```json
{
  "policyNumber": "POL-001",
  "customerName": "John Doe",
  "customerEmail": "john@example.com",
  "premium": 2500.00,
  "status": "ACTIVE",
  "startDate": "2026-09-22",
  "endDate": "2027-09-21"
}
```

Supported policy statuses are `PENDING`, `ACTIVE`, `CANCELLED`, and `EXPIRED`.

## Validation and business rules

The API validates required fields, email format, positive premiums, valid dates, and duplicate policy numbers. Monetary values use `BigDecimal` and PostgreSQL `NUMERIC(12,2)`.

Invalid business input returns HTTP 400. Invalid login credentials return HTTP 401 with a stable response:

```json
{
  "error": "Invalid username or password"
}
```

## Database migrations

Flyway owns runtime schema creation and upgrades. Hibernate is configured with:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

The initial migration is:

```text
src/main/resources/db/migration/V1__create_policy_table.sql
```

Flyway records applied migrations in `flyway_schema_history`.

Tests continue to use an isolated H2 database with Flyway disabled.

## Configuration

The default runtime configuration expects external values:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
```

No production JWT or database password fallback is embedded in `application.properties`.

For local execution, activate the local profile and supply the sensitive values through environment variables:

```bash
export DB_PASSWORD=insurance_password
export JWT_SECRET='<base64-hs256-secret>'
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

## Testing

Run:

```bash
./mvnw clean test
```

The suite covers service logic, controller behavior, validation, authenticated requests, anonymous rejection, invalid login handling, and application startup.

## Docker

Build and start:

```bash
docker compose build
docker compose up -d
```

Inspect:

```bash
docker compose ps
docker compose logs insurance-api
```

Stop without deleting the database volume:

```bash
docker compose down
```

## Kubernetes

Kubernetes manifests are in `k8s/`.

The local topology is:

```text
insurance-api Service
        |
   +----+----+
   |         |
API Pod   API Pod
   |         |
   +----+----+
        |
postgres Service
        |
PostgreSQL Pod
        |
PersistentVolumeClaim
```

The API uses three distinct health checks:

- `startupProbe` -> `/actuator/health/liveness`
- `readinessProbe` -> `/actuator/health/readiness`
- `livenessProbe` -> `/actuator/health/liveness`

Readiness includes database health, while liveness focuses on whether the application process is alive.

### Kubernetes secrets

Tracked Kubernetes files do not contain the real local password or JWT secret.

Copy the template:

```bash
cp k8s/secrets.example.yaml k8s/secrets.local.yaml
```

Replace the placeholder values in `k8s/secrets.local.yaml`, then apply it:

```bash
kubectl apply -f k8s/secrets.local.yaml
```

The local secret file is ignored by Git.

Deploy the remaining resources:

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/api-config.yaml
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/api.yaml
```

Check:

```bash
kubectl get pods -n insurance
```

For local access:

```bash
kubectl port-forward service/insurance-api 8080:8080 -n insurance
```

Health endpoints:

```bash
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness
```

## CI

`.github/workflows/ci.yml` runs on pushes and pull requests targeting `main`.

The pipeline:

```text
Checkout
   |
Java 25
   |
Maven tests
   |
Package JAR
   |
Docker build
```

## Production considerations

The project is a practical engineering demonstration. A production deployment would normally also use:

- an external identity provider instead of the demo in-memory user
- a managed PostgreSQL service
- a cloud secret manager
- TLS and ingress
- container registry publishing
- dependency and container vulnerability scanning
- metrics, tracing, and centralized logs
- horizontal pod autoscaling
- network policies
- automated deployment promotion between environments
