# AGENTS.md — sistema_antifraude_backend

Spring Boot 3.2.0 + Java 17 backend for a fraud prevention system. Drools embedded with `RiskContext` architecture.

## Build & Run

```bash
docker compose up -d          # starts 3 PostgreSQL containers: postgres (:5432 legacy),
                              #   postgres-canonical (:5433, CP's DB), postgres-banco (:5434, banco's DB)
mvn clean compile
mvn spring-boot:run           # :8080, vars from .env (DB_PORT=5434 points to banco DB)
mvn test                      # 24 tests incl. PostgreSQL lifecycle, schema contract, crypto, Drools
mvn test -Dtest=AntifraudeApplicationTests
```

## Package Map

```
com.antifraude
├── common/entity/      30+ domain entities (Pais, Moneda, Canal, Producto, Persona, ...)
├── common/repository/  27 JPA repos
├── config/             ClientIpResolver, DroolsConfig, ForwardedHeaderConfig, JpaAuditingConfig (CORS lives in SecurityConfig)
├── security/           SecurityConfig, JwtTokenProvider, JwtAuthFilter, handlers, clienteExterno/ (ExternalClientApiKeyFilter, ClienteExternoService)
├── admin/clienteExterno/  AdminClienteExternoController (CRUD API keys M2M)
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
│   └── similarity/     NameSimilarity (fuzzy matching de nombres: bigramas + Levenshtein)
├── external/           ExternalClientsConfig + RestClient clients → https://localhost:8443 (mock TLS): IdentificacionesClient, BcpSancionesClient, SepreladPepClient, ExternalInvestigationClient, ProviderHttpClient
├── audit/              Auditoria entity, auto-logging
├── logging/            AppLogAppender (cola BlockingQueue) + AppLogPersistenceJob (persiste app_log, RLS-aware)
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
- **Env vars**: `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD`, `JWT_SECRET`, `AES_SECRET`, `HMAC_SECRET`, `EXTERNAL_TRUSTSTORE(_PASSWORD)`, `IDENTIFICACIONES_API_KEY`/`SANCIONES_API_KEY`/`PEP_API_KEY`, `LICENSES_CONTROL_PLANE_URL`/`LICENSES_CONTROL_PLANE_API_KEY` (no `APP_` prefix), `FLYWAY_DB_USER`/`FLYWAY_DB_PASSWORD`, `EXTERNAL_CLIENTS_ENABLED`
- **Scheduling**: `@EnableScheduling` on main class — `AssignmentScheduler` (5min auto-assign, 1min rebalance) + `DisponibilidadScheduler` (1min availability) + `LicensingJobCoordinator` (1min, jobs on-prem de `admin_empresa_configuracion_local`) + `AppLogPersistenceJob` (1s) + `LogRetentionJob` (job on-prem).

## Security

### Autenticacion para Usuarios (JWT)

| Role | Access |
|------|--------|
| `ADMINISTRADOR` | Everything |
| `SUPERVISOR` | `/api/reglas/**`, `/api/simulador/**`, `/api/escenarios/**` |
| `ANALISTA` | `/api/casos/**`, `/api/reportes/**` |
| `AUDITOR` | `/api/auditoria/**` |

### Autenticacion M2M/B2B para Entidades Externas (API Key)

Para integraciones de bancos externos (Machine-to-Machine), el endpoint `/api/transacciones` acepta `X-API-Key` en lugar de JWT:

```
POST /api/transacciones
Headers:
  X-API-Key: <key-del-cliente-externo>
```

**Tabla**: `cliente_externo` — almacena API keys con bcrypt hash, scopes, rate limit, fecha expiracion.

**Scopes disponibles**:
- `TRANSACCIONES_WRITE` — POST /api/transacciones
- `TRANSACCIONES_READ` — GET /api/transacciones/*
- `ALERTAS_READ` — GET /api/alertas/*
- `KYC_READ` — POST /api/kyc/consultar

**Admin endpoints** (requiere `CLIENTES_EXTERNOS_GESTIONAR`):
- `POST /api/admin/clientes-externos` — crear (devuelve key una sola vez)
- `GET /api/admin/clientes-externos` — listar
- `PATCH /api/admin/clientes-externos/{id}` — actualizar
- `POST /api/admin/clientes-externos/{id}/rotar` — rotar key
- `DELETE /api/admin/clientes-externos/{id}` — revocar
- `GET /api/admin/clientes-externos/{id}/auditoria` — ver log de uso

**Filtros en la cadena** (orden):
1. `ExternalClientApiKeyFilter` (si `X-API-Key` presente)
2. `JwtAuthenticationFilter` (si `Authorization: Bearer` presente)
3. `TenantTransactionFilter`
4. `LicenciaFilter`

`/api/auth/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/api-docs/**` permitAll. `/api/admin/**` requires ADMINISTRADOR. All other paths require authentication. Lockout after 5 failed attempts (15 min).

## Drools Architecture

Drools receives a `RiskContext` pre-built by `RiskContextBuilder` (no BD/API access from rules):

```
Transaccion → RiskContextBuilder.build(tx) → RiskContext → KieSession.fireAllRules() → RiskResult
```

- DRL files in `resources/rules/domain/` (`riesgo-monto.drl`, `riesgo-pais.drl`, etc.) + fallback `fraud-rules.drl` (`salience -100`)
- Two eval paths in `DroolsService`:
  - `evaluar(RiskContext)` — new architecture, returns `RiskResult`. **Pure evaluation: no persistence side-effects** (safe for `/api/simulador/evaluar`, which uses a transient `Transaccion`). `EjecucionRegla` audit rows are skipped when `transaccion.getId() == null` (simulator).
  - `evaluarTransaccion(Transaccion)` — legacy, returns `BigDecimal` score; still creates alerts internally
- **Alert creation is the caller's job, not `evaluar`'s**: `TransaccionService.procesarTransaccion` calls `droolsService.crearAlertasDesdeResultado(...)` (public, guarded to persisted txs) when `result.requiereAccionInmediata()`. This avoids the old bug where the simulator tried to persist `Alerta`/`EjecucionRegla` referencing a transient `Transaccion` (`TransientPropertyValueException` → `InvalidDataAccessApiUsageException`/500), and the request-wide `TenantTransactionFilter` tx getting marked rollback-only → `UnexpectedRollbackException`.
- `evaluar` captures fired **DRL static rules** via an `AgendaEventListener` (rule name + score delta from `ScoreTracker`) and merges them with guided DB rules into `RiskResult.reglasDisparadas`; each carries `origen` = `"DROOLS"` | `"CONFIGURABLE"`
- List scoring in `riesgo-listas.drl` scales by `ListaFact.scoreConfianza` (0–100); `RiskContextBuilder` sets `ListaFact.tipoLista` to the bucket `"NEGRA"/"GRIS"/"BLANCA"` so the rules fire
- **Fuzzy name screening**: `ListScreeningService` does exact match first, then `NameSimilarity` (bigramas+Levenshtein) over active subjects/aliases and client-control NOMBRE elements (whitelist excluded); threshold/flag via `app.screening.name-fuzzy-threshold` (default 70) / `app.screening.name-fuzzy-enabled`
- `procesarTransaccion` persists the detail to `transacciones.reglas_disparadas_json` / `screening_result_json`
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
| POST | `/api/transacciones` | Creates + evaluates; returns `TransaccionEvaluacionResponse` (result-focused: reglasDisparadas con `origen` DROOLS/CONFIGURABLE, screening con similitud). GET endpoints still return `TransaccionResponse` |
| GET | `/api/admin-empresa/logs` | Admin logs (app_log): query `nivel`, `busca`, `desde`, `hasta`, `page`, `size`, `empresa=all`. **Pagination uses `offset = page * size`** (0-based). Response `{ total, page, size, logs[] }` |

## App logs (app_log)

- **Escritura asíncrona**: `AppLogAppender` (logback custom appender) encola eventos en `com.antifraude.logging` y `AppLogPersistenceJob` los drena cada segundo (lote máx. 200, nunca propaga errores).
- **Schema**: tablas creadas por migración canónica `V38__app_log_table` (Java) + `db/product-support/v38_app_log_table.sql`; `app_log` tiene **`FORCE ROW LEVEL SECURITY`** con política `tenant_isolation_app_log` (`empresa_id IS NULL OR empresa_id = NULLIF(current_setting('app.current_empresa_id'),'')::uuid`).
- **Gotcha RLS en el job**: el hilo del scheduler NO corre dentro de un request, así que no hay GUC. `AppLogPersistenceJob` abre una tx `REQUIRES_NEW` y por fila hace `SELECT set_config('app.current_empresa_id', ?, true)` (vía `queryForObject` — `update()` falla con «Se retornó un resultado cuando no se esperaba ninguno»), pasando `""` cuando `empresa_id` es null (evita fallo de inferencia de tipos con `null`).
- **Lectura**: el endpoint exige GUC; filas con `empresa_id` no-null quedan ocultas desde psql a menos que la sesión haga `SET app.current_empresa_id='<uuid>'` (cada `docker exec` es sesión nueva). FK `app_log_empresa_id_fkey → empresa(id)`.
- **Normalización IP**: `ClientIpResolver` normaliza loopback IPv6 `::1` → `127.0.0.1` antes de persistir.
- **Limpieza**: job on-prem `LOG_RETENTION_PURGE` (`com.antifraude.licensing.LogRetentionJob`, patrón `LicensingJob`, autodescubierto por `LicensingJobRunner`) borra `api_evento` (`fecha_evento`) y `app_log` (`fecha`) con antigüedad > `diasRetencion` (leído de su fila `detalle_json`, fallback `app.logs.retention-days=30`), fila por empresa y globales sin empresa. Seed demo: `db/demo/R__admin_empresa_dashboard_population.sql`.

## Gotchas

- **README endpoints are wrong** — README lists non-existent paths; trust `@RequestMapping` annotations
- **Mixed Spanish/English controller paths** — easy to guess wrong
- **`common/entity/`** has 30+ entities (Pais, Moneda, Canal, etc.) migrated during refactoring
- **`AES_SECRET`** used by `AesGcmCryptoService`; `HMAC_SECRET` used by `LicenseCryptoService`/`HmacHashService`
- **External clients** hit the mock at `https://localhost:8443` (TLS truststore pinned via `EXTERNAL_TRUSTSTORE`), NOT `localhost:3001`
- **`.env.example` exists** (repo root); `.env` itself is gitignored — prod credentials must be supplied externally
- **Demo seeds** only run with the `demo` profile from `db/demo`; normal/productive startup never loads them
- **Request-wide transaction**: `TenantTransactionFilter` wraps every authenticated request in a `TransactionTemplate` so `SET LOCAL app.current_empresa_id` (RLS) survives. Consequence: any exception that bubbles through a `@Transactional` service marks the shared tx rollback-only and the filter's commit throws `UnexpectedRollbackException` (turning an intended 400 into a 500). Mitigations: `TransaccionService` uses `@Transactional(noRollbackFor = BusinessException.class)`; caught non-blocking persistence (e.g. rule-exec audit) must not reference transient entities.
- **Demo roles** (Santaclara): active users resolve via `usuario_empresa` → modern roles `ADMINISTRADOR`/`SUPERVISOR`/`ANALISTA`/`AUDITOR`. Legacy roles `ADMIN_EMPRESA`/`GERENTE_SUPERVISOR`/`ADMIN_GENERAL` are deactivated (`rol.activo=false`, 0 permisos). Note RLS hides `usuario_empresa` rows unless `app.current_empresa_id` GUC is set.
