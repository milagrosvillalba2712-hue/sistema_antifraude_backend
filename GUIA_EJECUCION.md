# Guía de Ejecución — Sistema Antifraude Backend

## Requisitos verificados (ya los tenés)

- JDK 25 (`java -version`)
- Apache Maven (`mvn -version`)
- Docker Desktop (instalado y corriendo)
- PostgreSQL 16 (imagen docker)
- DBeaver (para ver las tablas)

---

## Paso 1 — Compilar el proyecto

```powershell
cd C:\Users\nicol\Projects\sistema_antifraude\sistema_antifraude_backend
mvn clean compile
```

**Resultado esperado:** `BUILD SUCCESS` sin errores. Aparece la carpeta `target/classes/com/antifraude/` con todos los `.class`.

> **Si falla:** verificá que Maven use JDK 25 con `mvn -version`. Los warnings de `sun.misc.Unsafe` son normales con Lombok, no son errores.

---

## Paso 2 — Iniciar PostgreSQL

```powershell
docker compose up -d
```

**Verificá que el contenedor está corriendo:**

```powershell
docker ps --filter name=antifraude-db
```

Deberías ver algo como:
```
CONTAINER ID   IMAGE         COMMAND                  CREATED          STATUS         PORTS                    NAMES
abc123def456   postgres:16   "docker-entrypoint.s…"   10 seconds ago   Up 9 seconds   0.0.0.0:5432->5432/tcp   antifraude-db
```

También podés verificar con:

```powershell
docker logs antifraude-db
```

Si ves `database system is ready to accept connections`, está listo.

---

## Paso 3 — Configurar DBeaver

1. Abrí DBeaver
2. Nueva conexión → PostgreSQL
3. Configurá:

| Campo | Valor |
|-------|-------|
| Host | `localhost` |
| Port | `5432` |
| Database | `antifraude` |
| Username | `postgres` |
| Password | `postgres` |

4. Test Connection → debería conectar exitosamente
5. Finish

> **Nota:** La base de datos `antifraude` se crea automáticamente al levantar el contenedor (via `POSTGRES_DB` en el `.env`). Las tablas se crean al ejecutar Spring Boot por primera vez (via Flyway).

---

## Paso 4 — Ejecutar la aplicación

### Opción A — Con Maven (recomendada)

```powershell
mvn spring-boot:run
```

La app arranca en `http://localhost:8080`. Vas a ver logs como:

```
2026-05-27T20:30:00.000Z  INFO 12345 --- [main] o.f.c.internal.database.DatabaseFactory  : Database: jdbc:postgresql://localhost:5432/antifraude (PostgreSQL 16.x)
2026-05-27T20:30:01.000Z  INFO 12345 --- [main] o.f.core.internal.command.DbMigrate      : Current version of schema "public": 1
2026-05-27T20:30:01.000Z  INFO 12345 --- [main] o.f.core.internal.command.DbMigrate      : Schema "public" is up to date. No migration necessary.
2026-05-27T20:30:02.000Z  INFO 12345 --- [main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080
```

### Opción B — Desde VS Code

Primero compilá con Maven (`mvn clean compile`), después:

1. Abrí `AntifraudeApplication.java`
2. Click en "Run" (▶) arriba del `main`
3. VS Code usa JDK 21 interno, pero las clases ya están compiladas en `target/`

---

## Paso 5 — Verificar las tablas en DBeaver

Una vez que la app arrancó (Flyway corrió las migraciones):

1. En DBeaver, expandí `antifraude` → `Schemas` → `public` → `Tables`
2. Deberías ver estas 8 tablas:

```
alertas
auditoria
consultas_externas
estadisticas_cliente
reglas_riesgo
reportes_ros
transacciones
usuarios
```

3. También los índices en `transacciones`:
   - `idx_transacciones_documento`
   - `idx_transacciones_fecha`
   - `idx_transacciones_score`

4. Hacé click derecho en cada tabla → View/Edit Data para ver los registros.

---

## Paso 6 — Probar que funciona

```powershell
# Sin token → debe dar 403
curl http://localhost:8080/api/dashboard

# Login
curl -X POST http://localhost:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"email":"admin@antifraude.com","password":"password"}'
```

> **Nota:** el login va a fallar porque no hay usuarios en la BD. Primero ejecutá los INSERTs de seed data desde DBeaver (ver sección siguiente).

---

## Seed Data (ejecutar en DBeaver)

```sql
INSERT INTO usuarios (nombre, email, password_hash, rol, activo)
VALUES ('Admin', 'admin@antifraude.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'ADMINISTRADOR', true);

INSERT INTO usuarios (nombre, email, password_hash, rol, activo)
VALUES ('Analista', 'analista@antifraude.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'ANALISTA', true);
```

El hash corresponde a la contraseña `password`.

---

## Troubleshooting

### Error: `ClassNotFoundException: com.antifraude.AntifraudeApplication`

**Causa:** No compilaste el proyecto. VS Code intenta ejecutar los `.java` fuente pero necesita los `.class`.

**Solución:** Siempre ejecutar primero:
```powershell
mvn clean compile
```

### Error: `Connection refused: localhost:5432`

**Causa:** PostgreSQL no está corriendo.

**Solución:**
```powershell
docker compose up -d
docker logs antifraude-db   # verificar que está ready
```

### Error: `Relation "public.usuarios" does not exist`

**Causa:** Flyway no corrió las migraciones.

**Solución:**
1. Verificar que la app arrancó sin errores
2. En DBeaver: `SELECT * FROM flyway_schema_history;`
3. Si la tabla está vacía, reiniciar la app

### Error: `FATAL: database "antifraude" does not exist`

**Causa:** El contenedor no creó la BD automáticamente.

**Solución:** Destruir y recrear el contenedor:
```powershell
docker compose down -v
docker compose up -d
```

### Error de Puerto 5432 ocupado

```powershell
netstat -ano | findstr :5432
```

Si otro proceso usa el puerto, cambiá `DB_PORT` en `.env` y reiniciá.

---

## Resumen de comandos

```powershell
# 1. Compilar
mvn clean compile

# 2. BD
docker compose up -d

# 3. Ejecutar
mvn spring-boot:run

# 4. Probar
curl http://localhost:8080/actuator/health
```

> Swagger UI: `http://localhost:8080/swagger-ui.html`
