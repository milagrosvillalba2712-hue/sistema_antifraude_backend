# AGENTS.md — sistema_antifraude_backend

Spring Boot 3.2.0 + Java 17 backend for a fraud prevention system. Drools embedded with `RiskContext` architecture.

## Build & Run

```bash
docker compose up -d          # starts PostgreSQL 16
mvn clean compile
mvn spring-boot:run           # :8080, all vars default to localhost/postgrespostgres
mvn test                      # 24 tests incl. PostgreSQL lifecycle, schema contract, crypto, Drools
mvn test -Dtest=AntifraudeApplicationTests
```

## Package Map

```
com.antifraude
├── common/entity/      30+ domain entities (Pais, Moneda, Canal, Producto, Persona, ...)
├── common/repository/  27 JPA repos
├── config/             ClientIpResolver, DroolsConfig, ForwardedHeaderConfig, JpaAuditingConfig (CORS lives in SecurityConfig)
├── security/           SecurityConfig, JwtTokenProvider, JwtAuthFilter, handlers
├── auth/               AuthController (POST /api/auth/login)
├── users/              Usuario entity, Service, AdminController (/api/admin/users)
├── transactions/       Transaccion + Controller (/api/transacciones)
├── alerts/             Alerta + HistorialAsignacion + Controller (/api/alertas)
├── assignment/         AssignmentEngine, scheduler, controller (/api/assignment)
├── profile/            PerfilUsuario, DisponibilidadUsuario, controllers
├── kyc/                KycController (POST /api/kyc/consultar)
├── reports/            ReporteRos + Controller (GET /api/reportes/ros/{alertaId} → CSV)
├── rules/              ReglaRiesgo + EjecucionRegla + Controller (/api/reglas)
├── cases/              Caso entity, Service, Controller (/api/casos)
├── dashboard/          DashboardController, DashboardService
├── drools/             DroolsService, RiskContext, RiskResult, RiskContextBuilder, facts
├── external/           ExternalClientsConfig + RestClient clients → https://localhost:8443 (mock TLS): IdentificacionesClient, BcpSancionesClient, SepreladPepClient, ExternalInvestigationClient, ProviderHttpClient
├── audit/              Auditoria entity, auto-logging
├── motor/              MotorController (/api/motor/historial)
├── escenarios/         EscenarioController (/api/escenarios)
├── catalog/            CatalogoController (/api/catalogos/{paises,monedas,canales,productos})
├── exception/          GlobalExceptionHandler
└── dto/                Request/Response records
```

## Conventions

- **Entities**: `@Data @NoArgsConstructor @AllArgsConstructor @Builder` (Lombok), `-proc:full` in compiler plugin
- **Services**: `@Service @Transactional`, constructor injection
- **Controllers**: `@RestController @RequestMapping("/api/...")` — paths are Spanish (`/api/alertas`, `/api/reglas`, `/api/casos`, `/api/transacciones`, `/api/reportes`) except English: `/api/assignment`, `/api/profile`, `/api/availability`, `/api/auth`, `/api/admin/users`, `/api/dashboard`
- **Security**: `hasRole("ADMINISTRADOR")` — DB stores role name (e.g. `ADMINISTRADOR`), Spring Security prepends `ROLE_`
- **Schema**: Flyway is the only schema owner; canonical Java migrations live in `db.productmigration` and Hibernate runs with `ddl-auto: validate`.
- **Seeds**: no Java startup seed/schema runner exists. Legacy SQL is historical input only; demo data must use an explicit demo-only Flyway location/profile.
- **Env vars**: `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD`, `JWT_SECRET`, `AES_SECRET`, `HMAC_SECRET`, `EXTERNAL_TRUSTSTORE(_PASSWORD)`, `IDENTIFICACIONES_API_KEY`/`SANCIONES_API_KEY`/`PEP_API_KEY`, `LICENSES_CONTROL_PLANE_URL`/`LICENSES_CONTROL_PLANE_API_KEY` (no `APP_` prefix), `FLYWAY_DB_USER`/`FLYWAY_DB_PASSWORD`
- **Scheduling**: `@EnableScheduling` on main class — `AssignmentScheduler` (5min auto-assign, 1min rebalance) + `DisponibilidadScheduler` (1min availability)

## Security

| Role | Access |
|------|--------|
| `ADMINISTRADOR` | Everything |
| `SUPERVISOR` | `/api/reglas/**`, `/api/simulador/**`, `/api/escenarios/**` |
| `ANALISTA` | `/api/casos/**`, `/api/reportes/**` |
| `AUDITOR` | `/api/auditoria/**` |

`/api/auth/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/api-docs/**` permitAll. `/api/admin/**` requires ADMINISTRADOR. All other paths require authentication. Lockout after 5 failed attempts (15 min).

## Drools Architecture

Drools receives a `RiskContext` pre-built by `RiskContextBuilder` (no BD/API access from rules):

```
Transaccion → RiskContextBuilder.build(tx) → RiskContext → KieSession.fireAllRules() → RiskResult
```

- DRL files in `resources/rules/domain/` (`riesgo-monto.drl`, `riesgo-pais.drl`, etc.) + fallback `fraud-rules.drl` (`salience -100`)
- Two eval paths in `DroolsService`:
  - `evaluar(RiskContext)` — new architecture, returns `RiskResult`
  - `evaluarTransaccion(Transaccion)` — legacy, returns `BigDecimal` score
- If no rules fire, hardcoded default score applied
- Alerts generated automatically when score >= 70

## Notable Endpoints (non-obvious facts)

| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/kyc/consultar` | POST with body, NOT `GET /api/kyc/{documento}` (README is wrong) |
| GET | `/api/reportes/ros/{alertaId}` | Returns CSV (Content-Disposition attachment) |
| POST | `/api/simulador/evaluar` | Evaluates without persisting |
| PATCH | `/api/casos/{id}/estado?estado=X` | State change via query param |
| PATCH | `/api/casos/{id}/asignar?analistaId=X` | Assign via query param |
| GET | `/api/alertas/estado/{estado}` | Filter alerts by state (path param, not query) |
| POST | `/api/alertas/{id}/asignar` | Assign; defaults to current user if body absent |
| GET | `/api/availability` | English path (disponibilidad) |
| POST | `/api/transacciones` | Creates + evaluates in one call |

## Gotchas

- **README endpoints are wrong** — README lists non-existent paths; trust `@RequestMapping` annotations
- **Mixed Spanish/English controller paths** — easy to guess wrong
- **`common/entity/`** has 30+ entities (Pais, Moneda, Canal, etc.) migrated during refactoring
- **`AES_SECRET`** used by `AesGcmCryptoService`; `HMAC_SECRET` used by `LicenseCryptoService`/`HmacHashService`
- **External clients** hit the mock at `https://localhost:8443` (TLS truststore pinned via `EXTERNAL_TRUSTSTORE`), NOT `localhost:3001`
- **`.env.example` exists** (repo root); `.env` itself is gitignored — prod credentials must be supplied externally
- **Demo seeds** only run with the `demo` profile from `db/demo`; normal/productive startup never loads them
