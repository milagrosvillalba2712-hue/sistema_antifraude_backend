# AGENTS.md — sistema_antifraude_backend

Spring Boot 3.2.0 + Java 17+ backend for a fraud prevention system. Monolithic modularized with Drools embedded.

## Stack

- **Java 17+** (env has JDK 25 — set `java.version` to 17 in pom.xml, Lombok 1.18.46+ required for JDK 25)
- **Spring Boot 3.2.0**, Spring Data JPA, Spring Security, Spring Validation
- **PostgreSQL 16**, Flyway migrations (`V{number}__{name}.sql` in `db/migration/`)
- **Drools 8.44** (kie-container via `DroolsConfig`, rules in `resources/rules/*.drl`)
- **JWT** (jjwt 0.12.3), **BCrypt**, **AES-256-GCM** (CryptoUtil in `config/`)
- **SpringDoc OpenAPI** at `/swagger-ui.html`
- **Lombok** — annotation processing configured in `maven-compiler-plugin` with `<arg>-proc:full</arg>` (required for JDK 23+)

## Build & Run

```bash
docker compose up -d          # starts PostgreSQL
mvn clean compile             # Lombok + annotation processing enabled
mvn spring-boot:run           # starts on :8080
```

## Package Structure

```
com.antifraude
├── config/          # CorsConfig, DroolsConfig
├── security/        # JwtTokenProvider, JwtAuthFilter, SecurityConfig, CustomUserDetailsService
├── auth/            # AuthController, AuthService (login, JWT generation)
├── users/           # Usuario entity, Repository, Service (CRUD + BCrypt)
├── transactions/    # Transaccion + EstadisticasCliente entities, Repositories, Service, Controller
├── alerts/          # Alerta entity, Repository, Service, Controller
├── rules/           # ReglaRiesgo entity, Repository, Service, Controller
├── audit/           # Auditoria entity, Repository, Service (auto-logging)
├── kyc/             # KycController, KycService (external identity/PEP/sanctions check)
├── reports/         # ReporteRos entity, Repository, Service, Controller (CSV export)
├── dashboard/       # DashboardController, DashboardService (aggregated KPIs)
├── external/        # ExternalApiClient (RestTemplate), ConsultaExterna entity
├── drools/          # DroolsService (evaluate rules, generate alerts)
└── dto/             # Request/Response records (LoginRequest, TransaccionRequest, etc.)
```

## Key Conventions

- Entities use Lombok `@Data @NoArgsConstructor @AllArgsConstructor @Builder`
- Services are `@Service @Transactional`, injected via constructor
- Controllers use `@RestController` with `@RequestMapping("/api/{domain}")`
- `/api/auth/**` is public; everything else requires JWT Bearer token
- `ROLE_ADMINISTRADOR` required for `/api/admin/**`
- Flyway manages schema — `ddl-auto: validate` (no auto-DDL)
- `application.yml` reads secrets from env vars (`DB_PASSWORD`, `JWT_SECRET`, `AES_SECRET`, `EXTERNAL_API_KEY`)
- `ExternalApiClient` points to mock server at `http://localhost:3001`

## Testing

```bash
mvn test
# Single test: mvn test -Dtest=AntifraudeApplicationTests
```

## Migrations

All in `src/main/resources/db/migration/`:
- `V1__init.sql` — full schema (usuarios, transacciones, alertas, reglas_riesgo, auditoria, estadisticas_cliente, consultas_externas, reportes_ros)
- `V2__seed.sql` — seed users (admin/analista, password: `password`)

New migration naming: `V{next_number}__{name}.sql`. Flyway manages schema; `ddl-auto: validate` means **no auto-DDL** — always add a migration, never rely on Hibernate to create/alter tables.

## Environment

`.env` file at root with `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `AES_SECRET`, `EXTERNAL_API_KEY`.

All vars have defaults in `application.yml`, so the app boots without `.env` using localhost PostgreSQL with `postgres/postgres` credentials. Change secrets for anything beyond local dev.
