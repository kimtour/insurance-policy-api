# Insurance Policy API

A production-style Spring Boot REST API for managing insurance policies. Built to demonstrate backend engineering practices across API design, validation, persistence, security, testing, containerisation, Kubernetes, and CI/CD.

**Repository:** [github.com/kimtour/insurance-policy-api](https://github.com/kimtour/insurance-policy-api)

---

## Table of Contents

1. [Architecture](#architecture)
2. [Technology Stack](#technology-stack)
3. [Project Structure](#project-structure)
4. [API Reference](#api-reference)
5. [Running Locally](#running-locally)
6. [Docker Compose](#docker-compose)
7. [Kubernetes](#kubernetes)
8. [Configuration](#configuration)
9. [Database Migrations](#database-migrations)
10. [Security](#security)
11. [Automated Tests](#automated-tests)
12. [CI Pipeline](#ci-pipeline)
13. [Engineering Decisions](#engineering-decisions)
14. [Production Improvements](#production-improvements)
15. [Interview Walkthrough](#interview-walkthrough)

---

## Architecture

```
                         ┌─────────────────────────────────────────┐
                         │            Spring Boot Application        │
                         │                                           │
  HTTP + JWT             │  ┌──────────────┐   ┌─────────────────┐  │
Client ─────────────────►│  │  Controllers  │──►│    Services     │  │
                         │  │  AuthCtrl     │   │  PolicyService  │  │
                         │  │  PolicyCtrl   │   │  • business     │  │
                         │  └──────┬───────┘   │    rules        │  │
                         │         │            │  • DTO mapping  │  │
                         │  ┌──────▼───────┐   └───────┬─────────┘  │
                         │  │  Spring       │           │            │
                         │  │  Security     │   ┌───────▼─────────┐  │
                         │  │  • JWT filter │   │   Repository    │  │
                         │  │  • stateless  │   │  Spring Data    │  │
                         │  └──────────────┘   │  JPA + Hibernate │  │
                         │                      └───────┬─────────┘  │
                         │  ┌──────────────┐           │            │
                         │  │  Global       │           │            │
                         │  │  Exception    │   ┌───────▼─────────┐  │
                         │  │  Handler      │   │   PostgreSQL     │  │
                         │  └──────────────┘   │  Flyway schema   │  │
                         └─────────────────────┴─────────────────-┘
```

### Request lifecycle

```
POST /api/policies  (with Bearer token)
        │
        ▼
Spring Security JWT filter
  valid? ──── no ───► 401 Unauthorized
        │
        ▼ yes
PolicyController.createPolicy()
  @Valid ──── fails ─► GlobalExceptionHandler ──► 400 { field: message }
        │
        ▼ passes
PolicyService.createPolicy()
  duplicate? ── yes ─► IllegalArgumentException ──► 400 { error: message }
  endDate before startDate? ── yes ─► same
        │
        ▼ passes all rules
PolicyRepository.save()
        │
        ▼
PostgreSQL (Flyway-managed schema)
        │
        ▼
201 Created  { policy JSON }
```

---

## Technology Stack

| Area | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1.1 |
| API | Spring Web MVC |
| Persistence | Spring Data JPA / Hibernate 7 |
| Database | PostgreSQL 17 |
| Migrations | Flyway |
| Test database | H2 (in-memory, isolated) |
| Security | Spring Security, JWT, OAuth2 Resource Server |
| Validation | Jakarta Validation (Bean Validation 3) |
| Build | Maven + Maven Wrapper |
| Testing | JUnit 5, Mockito, MockMvc |
| Monitoring | Spring Boot Actuator |
| Containers | Docker (multi-stage), Docker Compose |
| Orchestration | Kubernetes (Docker Desktop) |
| CI | GitHub Actions |

---

## Project Structure

```
insurance-policy-api/
├── src/
│   ├── main/
│   │   ├── java/com/sam/insurance/
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java       # POST /api/auth/login
│   │   │   │   ├── HelloController.java      # GET /hello (smoke test)
│   │   │   │   └── PolicyController.java     # CRUD /api/policies
│   │   │   ├── dto/
│   │   │   │   ├── AuthRequest.java
│   │   │   │   ├── AuthResponse.java
│   │   │   │   ├── PolicyRequest.java        # validated input
│   │   │   │   └── PolicyResponse.java       # API output shape
│   │   │   ├── exception/
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── model/
│   │   │   │   ├── Policy.java               # JPA entity
│   │   │   │   └── PolicyStatus.java         # PENDING/ACTIVE/CANCELLED/EXPIRED
│   │   │   ├── repository/
│   │   │   │   └── PolicyRepository.java
│   │   │   ├── security/
│   │   │   │   ├── JwtService.java
│   │   │   │   └── SecurityConfig.java
│   │   │   └── service/
│   │   │       └── PolicyService.java
│   │   └── resources/
│   │       ├── application.properties        # production config (no secrets)
│   │       ├── application-local.properties  # local overrides (gitignored)
│   │       └── db/migration/
│   │           └── V1__create_policy_table.sql
│   └── test/
│       ├── java/com/sam/insurance/
│       │   ├── controller/
│       │   │   ├── AuthControllerTest.java
│       │   │   └── PolicyControllerTest.java
│       │   ├── service/
│       │   │   └── PolicyServiceTest.java
│       │   └── InsurancePolicyApiApplicationTests.java
│       └── resources/
│           └── application.properties        # H2, Flyway disabled
├── k8s/
│   ├── namespace.yaml
│   ├── postgres.yaml                         # PVC + Deployment + Service
│   ├── api-config.yaml                       # ConfigMap (non-secret values)
│   ├── api.yaml                              # Deployment + Service
│   ├── secrets.example.yaml                  # placeholder template (committed)
│   └── secrets.local.yaml                    # real dev values (gitignored)
├── .github/workflows/ci.yml
├── compose.yaml
├── Dockerfile
└── pom.xml
```

---

## API Reference

### Authentication

#### Login

```
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "username": "sam",
  "password": "password123"
}
```

**200 OK**

```json
{
  "token": "eyJhbGciOiJSUzI1NiJ9...",
  "tokenType": "Bearer"
}
```

**401 Unauthorized** (wrong credentials)

```json
{
  "error": "Invalid username or password"
}
```

**Full curl example**

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"sam","password":"password123"}' \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

echo $TOKEN
```

---

### Policy Endpoints

All endpoints require `Authorization: Bearer <token>`.

#### Create a policy

```
POST /api/policies
Authorization: Bearer <token>
Content-Type: application/json
```

```bash
curl -i -X POST http://localhost:8080/api/policies \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "policyNumber": "POL-001",
    "customerName": "Alice Johnson",
    "customerEmail": "alice@example.com",
    "premium": 1250.00,
    "status": "ACTIVE",
    "startDate": "2026-01-01",
    "endDate": "2026-12-31"
  }'
```

**201 Created**

```json
{
  "id": 1,
  "policyNumber": "POL-001",
  "customerName": "Alice Johnson",
  "customerEmail": "alice@example.com",
  "premium": 1250.00,
  "status": "ACTIVE",
  "startDate": "2026-01-01",
  "endDate": "2026-12-31"
}
```

**400 Bad Request** (validation failure)

```json
{
  "customerEmail": "must be a well-formed email address",
  "premium": "must be greater than 0"
}
```

**400 Bad Request** (duplicate policy number)

```json
{
  "error": "Policy number already exists: POL-001"
}
```

#### Get all policies

```bash
curl -i http://localhost:8080/api/policies \
  -H "Authorization: Bearer $TOKEN"
```

**200 OK** — returns a JSON array of policies.

#### Get a policy by ID

```bash
curl -i http://localhost:8080/api/policies/1 \
  -H "Authorization: Bearer $TOKEN"
```

**200 OK** or **404 Not Found**.

#### Update a policy

```bash
curl -i -X PUT http://localhost:8080/api/policies/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "policyNumber": "POL-001",
    "customerName": "Alice Johnson",
    "customerEmail": "alice@example.com",
    "premium": 1400.00,
    "status": "ACTIVE",
    "startDate": "2026-01-01",
    "endDate": "2026-12-31"
  }'
```

**200 OK** or **404 Not Found**.

#### Delete a policy

```bash
curl -i -X DELETE http://localhost:8080/api/policies/1 \
  -H "Authorization: Bearer $TOKEN"
```

**204 No Content** or **404 Not Found**.

#### Without a token

```bash
curl -i http://localhost:8080/api/policies
# HTTP/1.1 401 Unauthorized
```

---

### Observability Endpoints

These are unauthenticated.

```bash
curl http://localhost:8080/actuator/health/liveness
# {"status":"UP"}

curl http://localhost:8080/actuator/health/readiness
# {"status":"UP"}   (includes database check)
```

---

## Running Locally

**Requirements:** Java 25, PostgreSQL 17.

```bash
# 1. Set Java 25
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
export PATH="$JAVA_HOME/bin:$PATH"

# 2. Run tests
./mvnw clean test

# 3. Start with local profile (reads application-local.properties)
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

In another terminal:

```bash
# Smoke test
curl http://localhost:8080/hello

# Health
curl http://localhost:8080/actuator/health

# Get a token and call the API
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"sam","password":"password123"}' \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

curl -i http://localhost:8080/api/policies \
  -H "Authorization: Bearer $TOKEN"
```

---

## Docker Compose

Starts the API and PostgreSQL together with a named volume.

```bash
# Build and start
docker compose build
docker compose up -d

# View status
docker compose ps

# Tail logs
docker compose logs -f insurance-api

# Stop
docker compose down

# Stop and remove volume (full reset)
docker compose down -v
```

After startup, the same curl commands in [Running Locally](#running-locally) work on `localhost:8080`.

---

## Kubernetes

Manifests live in `k8s/`. The namespace is `insurance`. The API runs two replicas; PostgreSQL is a single stateful workload backed by a PVC.

### First-time setup

```bash
# 1. Create namespace
kubectl apply -f k8s/namespace.yaml

# 2. Apply secrets (local file, not committed)
kubectl apply -f k8s/secrets.local.yaml

# 3. Apply ConfigMap and workloads
kubectl apply -f k8s/api-config.yaml
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/api.yaml

# 4. Watch pods come up
kubectl get pods -n insurance -w
```

Expected:

```
insurance-api-xxxxx   1/1   Running   0
insurance-api-xxxxx   1/1   Running   0
postgres-xxxxx        1/1   Running   0
```

### Accessing the API

```bash
kubectl port-forward service/insurance-api 8080:8080 -n insurance
```

### Health probes

The API uses three distinct probes:

| Probe | Endpoint | Purpose |
|---|---|---|
| `startupProbe` | `/actuator/health/liveness` | Wait for JVM + Spring startup (~40s) |
| `readinessProbe` | `/actuator/health/readiness` | Ready to serve traffic; includes DB check |
| `livenessProbe` | `/actuator/health/liveness` | JVM is alive; restart if stuck |

### Rebuilding and redeploying

Docker Desktop Kubernetes uses its own containerd store, so you must load the image after every `docker build`.

```bash
# 1. Build
docker build -t insurance-policy-api-insurance-api:latest .

# 2. Save and import into the K8s node
docker save insurance-policy-api-insurance-api:latest -o /tmp/insurance-api.tar

kubectl debug node/desktop-control-plane \
  --image=busybox:1.36 --profile=sysadmin \
  --attach=false -- sleep 3600

kubectl cp /tmp/insurance-api.tar \
  <debug-pod>:/host/tmp/insurance-api.tar

kubectl exec <debug-pod> -- \
  chroot /host /usr/local/bin/ctr -n k8s.io images import \
  /tmp/insurance-api.tar

kubectl delete pod <debug-pod>

# 3. Rollout
kubectl rollout restart deployment/insurance-api -n insurance
kubectl rollout status deployment/insurance-api -n insurance
```

### Secrets handling

| File | Committed | Contains |
|---|---|---|
| `k8s/secrets.example.yaml` | ✅ Yes | Placeholder values only |
| `k8s/secrets.local.yaml` | ❌ No (gitignored) | Real development credentials |

In CI or production, secrets are injected by a pipeline, secret manager, or sealed-secret controller — never stored in the repository.

### Useful Kubernetes commands

```bash
# Check pods
kubectl get pods -n insurance

# Stream API logs
kubectl logs -n insurance deployment/insurance-api -f

# Check Flyway migration ran
kubectl exec -n insurance deployment/postgres \
  -- psql -U insurance_user -d insurance \
  -c 'SELECT version, description, success FROM flyway_schema_history;'

# Inspect PostgreSQL tables
kubectl exec -n insurance deployment/postgres \
  -- psql -U insurance_user -d insurance -c '\dt'

# Scale API down
kubectl scale deployment insurance-api --replicas=0 -n insurance

# Scale API back up
kubectl scale deployment insurance-api --replicas=2 -n insurance
```

---

## Configuration

The application uses Spring profiles to separate runtime environments.

| Profile | Source | Purpose |
|---|---|---|
| (default) | `application.properties` | Production — no secret fallbacks |
| `local` | `application-local.properties` | Developer machine |
| test | `src/test/resources/application.properties` | H2, Flyway disabled |

Production `application.properties` contains **no fallback values** for secrets:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
app.jwt.secret=${JWT_SECRET}
```

If the environment does not supply these, the application refuses to start. This is intentional — it prevents a misconfigured deployment from silently running with development credentials.

---

## Database Migrations

Flyway owns the schema. Hibernate only validates it.

```
Developer writes V2__add_coverage_type.sql
        │
        ▼
git commit + push
        │
        ▼
CI builds the JAR (Flyway SQL packaged inside)
        │
        ▼
Application starts
        │
        ▼
Flyway reads flyway_schema_history
  V1 already applied ──► skip
  V2 not applied ──────► apply V2 SQL
        │
        ▼
Hibernate validates entity mappings match schema
        │
        ▼
Application ready
```

Migration files live in `src/main/resources/db/migration/`. Naming follows the Flyway convention: `V<version>__<description>.sql`.

Current migrations:

| Version | File | Description |
|---|---|---|
| 1 | `V1__create_policy_table.sql` | Creates `policy` with constraints |

The database also enforces business rules directly:

```sql
CONSTRAINT chk_policy_status  CHECK (status IN ('PENDING','ACTIVE','CANCELLED','EXPIRED'))
CONSTRAINT chk_policy_dates   CHECK (end_date > start_date)
CONSTRAINT chk_policy_premium CHECK (premium > 0)
```

This gives three independent protection layers: API validation → service rules → database constraints.

---

## Security

### Login flow

```
POST /api/auth/login  { username, password }
        │
        ▼
AuthenticationManager
  └── InMemoryUserDetailsService (bcrypt)
        │
    valid? ── no ──► AuthenticationException
        │               └──► GlobalExceptionHandler
        │                    └──► 401 { "error": "Invalid username or password" }
        ▼ yes
JwtService.generateToken(username)
        │
        ▼
200 { "token": "...", "tokenType": "Bearer" }
```

### Protected request flow

```
GET /api/policies
Authorization: Bearer <jwt>
        │
        ▼
Spring Security OAuth2 resource server filter
  valid signature + not expired? ── no ──► 401
        │
        ▼ yes
PolicyController
```

Sessions are stateless (`SessionCreationPolicy.STATELESS`). Any API replica can validate any token without shared session storage.

Production requires `JWT_SECRET` to be injected via environment; no fallback is configured.

---

## Automated Tests

```
./mvnw clean test
```

**21 tests, 0 failures.**

| Class | Type | Coverage |
|---|---|---|
| `PolicyServiceTest` | Unit | Service rules: create, duplicate detection, date validation, update, delete, not-found |
| `PolicyControllerTest` | MockMvc | All 5 endpoints, validation errors, 401 without token, 404 not found |
| `AuthControllerTest` | MockMvc | Invalid credentials → 401 + JSON error body |
| `InsurancePolicyApiApplicationTests` | Integration | Application context loads with H2 |

Tests use H2 in-memory with `spring.flyway.enabled=false`. Flyway is only active against PostgreSQL in the real environments.

---

## CI Pipeline

`.github/workflows/ci.yml` runs on every push and pull request to `main`.

```
git push
    │
    ▼
GitHub-hosted Ubuntu runner
    │
    ▼
actions/checkout@v4
    │
    ▼
actions/setup-java@v4  (Temurin 25, Maven cache)
    │
    ▼
chmod +x mvnw
    │
    ▼
./mvnw clean test       ← 21 tests
    │
    ▼
./mvnw clean package    ← produces JAR
    │
    ▼
docker build            ← validates Dockerfile
    │
    ▼
✅ CI passes  or  ❌ fails fast
```

Deployment is intentionally separate from CI. That separation makes it straightforward to promote a known-good JAR to staging and then production without mixing build and release concerns.

---

## Engineering Decisions

### Flyway + Hibernate validate
Flyway migrations are deterministic, versioned, and auditable. Every schema change is a committed SQL file with a version number. `ddl-auto=validate` means Hibernate checks that the entity mappings match the schema at startup, catching mismatches before they reach production.

### BigDecimal for premiums
`float` and `double` cannot represent most decimal fractions exactly. `BigDecimal` mapped to `NUMERIC(12,2)` stores and retrieves money without rounding errors. This is standard for any financial application.

### DTOs separate from entities
`PolicyRequest` and `PolicyResponse` are the HTTP contract. `Policy` is the persistence contract. Keeping them separate means adding an internal field to the entity does not accidentally expose it through the API, and changing the API shape does not require entity migration.

### Stateless JWT
No server-side session storage. Each token is self-contained and can be validated by any replica using the shared signing key. This makes horizontal scaling straightforward.

### PostgreSQL runtime, H2 for tests
Tests run against H2 in milliseconds without requiring a running database server. The actual Flyway-managed PostgreSQL behaviour is validated in the Kubernetes environment.

### Multi-stage Docker build
The `builder` stage contains the JDK and Maven. The runtime stage contains only the JRE and the JAR. The final image is smaller and has a reduced attack surface.

### Two Kubernetes replicas
Demonstrates that the application is stateless and horizontally scalable. In a real deployment, PostgreSQL would be a managed cloud service rather than a Kubernetes Deployment.

### Dedicated Kubernetes health probes
Using `/actuator/health/liveness` for the startup and liveness probes, and `/actuator/health/readiness` (which includes `db`) for the readiness probe, means:
- Kubernetes waits through the full ~40s Spring startup before enforcing liveness
- Traffic is only sent to a pod that can reach the database
- A JVM crash triggers a restart; a slow database query does not

---

## Production Improvements

The following would be added before a real production release:

- **Identity provider:** replace in-memory users with Keycloak, Auth0, or similar; add refresh tokens and token revocation
- **TLS + Ingress:** terminate HTTPS at an ingress controller; internal traffic over mTLS
- **Rate limiting:** prevent credential-stuffing on `/api/auth/login`
- **OpenAPI / Swagger UI:** `springdoc-openapi` for auto-generated, interactive API documentation
- **Structured logging:** JSON log lines with correlation IDs, shipped to a log aggregator
- **Metrics and tracing:** Micrometer + Prometheus + Grafana; distributed tracing with OpenTelemetry
- **Managed PostgreSQL:** RDS, Cloud SQL, or similar; connection pooling via PgBouncer
- **External secret management:** Vault, AWS Secrets Manager, or Kubernetes Sealed Secrets
- **Vulnerability scanning:** container image scanning in CI (Trivy or Grype)
- **Registry publishing:** push signed images to a private registry as part of CD
- **Automated deployment:** ArgoCD or Flux for GitOps-style Kubernetes delivery
- **Autoscaling:** HorizontalPodAutoscaler on CPU/RPS; database read replicas
- **Network policies:** restrict pod-to-pod traffic to only necessary paths

---

## Interview Walkthrough

> I built an insurance policy management API with Java 25 and Spring Boot 4. It exposes JWT-secured CRUD endpoints following a controller-service-repository architecture. PostgreSQL is the runtime database; Flyway owns deterministic schema migrations and Hibernate validates entity mappings at startup without touching the schema. Tests use isolated H2 and run in milliseconds without a database server. I added validation at the API and service layers, centralised exception handling that returns clean error shapes, SLF4J logging, and 21 tests covering unit, MockMvc controller, security, and application-context scenarios. The application uses a multi-stage Docker build, runs with PostgreSQL through Docker Compose, and deploys to Kubernetes with two API replicas, persistent PostgreSQL storage, dedicated startup/readiness/liveness probes, and secrets injected from outside the repository. GitHub Actions runs tests, packages the JAR, and validates the Docker build on every push to main.

### Common follow-up questions

**Why Flyway instead of Hibernate `ddl-auto=update`?**
> `update` is convenient in development but dangerous in production. It cannot safely drop columns, rename things, or run data migrations. Flyway gives every schema change a version number and a SQL file that is committed to the repository, reviewed in pull requests, and applied exactly once per environment. The history is stored in `flyway_schema_history` and is auditable.

**Why JWT? Why not sessions?**
> Sessions require either sticky routing or a shared session store. JWT tokens are self-contained: any replica can validate any token using the signing key. This makes horizontal scaling simple and removes a stateful dependency from the API tier.

**Why two Kubernetes replicas?**
> It demonstrates that the application is genuinely stateless. Because sessions are not stored in the API process, you can run any number of replicas without coordination. In a real deployment you would also combine this with a HorizontalPodAutoscaler.

**What happens if PostgreSQL goes down?**
> The readiness probe includes the `db` health indicator. If the database is unreachable, the readiness probe fails and Kubernetes stops sending traffic to those pods. The pods stay alive (liveness is separate), so they recover automatically when the database comes back without a restart.

**Why BigDecimal for premium?**
> Floating-point types like `double` cannot exactly represent most decimal values. `1.10 + 2.20` can equal `3.3000000000000003` in floating-point. For money, that is unacceptable. `BigDecimal` mapped to `NUMERIC(12,2)` stores and retrieves exact decimal values.

**Why separate DTOs from entities?**
> Entities are the persistence contract. DTOs are the API contract. Keeping them separate means an internal field on the entity is never accidentally serialised into the response, and changing the API response shape does not require a database migration. It also makes input validation straightforward — you validate the DTO before it ever touches the entity.

**How would you deploy this to a cloud provider?**
> I would push the Docker image to a private registry (ECR, GCR, or similar), store secrets in a managed secret service (Secrets Manager or Vault), and deploy the Kubernetes manifests through a GitOps pipeline (ArgoCD). PostgreSQL would be a managed service rather than a Kubernetes Deployment. The CI workflow already produces a versioned image tagged with the Git SHA; the CD step would promote that image through environments.

**What would you add before putting this in production?**
> TLS at the ingress layer, a real identity provider with token refresh and revocation, rate limiting on the login endpoint, structured JSON logging with correlation IDs, metrics and distributed tracing, container image scanning in CI, network policies between pods, and autoscaling.
