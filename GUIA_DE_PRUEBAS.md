# Guía de Pruebas — Sistema Antifraude Backend

## 1. Inicio Rápido

```bash
# 1. Iniciar PostgreSQL
docker compose up -d

# 2. Compilar
mvn clean compile

# 3. Iniciar backend
mvn spring-boot:run
```

El servidor arranca en `http://localhost:8080`. Swagger disponible en `http://localhost:8080/swagger-ui.html`.

---

## 2. Seed Data

Ejecuta estos INSERTs en PostgreSQL para tener datos iniciales:

```sql
-- Usuario ADMIN
INSERT INTO usuarios (nombre, email, password_hash, rol, activo)
VALUES ('Admin', 'admin@antifraude.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'ADMINISTRADOR', true);

-- Usuario ANALISTA
INSERT INTO usuarios (nombre, email, password_hash, rol, activo)
VALUES ('Analista', 'analista@antifraude.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'ANALISTA', true);

-- Regla de riesgo
INSERT INTO reglas_riesgo (nombre, descripcion, tipo_regla, severidad, condicion, activa, creada_por)
VALUES ('Monto elevado > $10,000', 'Dispara alerta si el monto supera 10,000 USD',
        'MONTO', 'ALTA', 'monto > 10000', true, 1);
```

> Los password hash corresponden a la contraseña `password`.

---

## 3. Autenticación

### Login

**Request:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@antifraude.com",
    "password": "password"
  }'
```

**Response (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "email": "admin@antifraude.com",
  "rol": "ADMINISTRADOR"
}
```

Guarda el `token` — lo usarás como `Authorization: Bearer <token>` en el resto de endpoints.

### Flujo de autenticación

```
Cliente                     Servidor
  │                           │
  │── POST /api/auth/login ──→│  Validar credenciales
  │    {email, password}      │  Generar JWT
  │←── 200 {token, rol} ─────│  Registrar LOGIN en auditoría
  │                           │
  │── GET /api/transacciones ─→│  Validar JWT del header
  │    Authorization: Bearer ·│  Extraer email del token
  │←── 200 [...] ────────────│  Cargar UserDetails
```

---

## 4. Gestión de Reglas de Riesgo

Todas las rutas `/api/reglas/*` requieren JWT.

### Listar reglas

```bash
curl http://localhost:8080/api/reglas \
  -H "Authorization: Bearer <token>"
```

**Response:**
```json
[
  {
    "id": 1,
    "nombre": "Monto elevado > $10,000",
    "descripcion": "Dispara alerta si el monto supera 10,000 USD",
    "tipoRegla": "MONTO",
    "severidad": "ALTA",
    "condicion": "monto > 10000",
    "activa": true,
    "creadaPor": 1,
    "fechaCreacion": "2026-05-27T20:00:00",
    "fechaModificacion": null
  }
]
```

### Crear regla

```bash
curl -X POST http://localhost:8080/api/reglas \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Transferencia internacional",
    "descripcion": "Alerta si el canal es transferencia internacional",
    "tipoRegla": "CANAL",
    "severidad": "MEDIA",
    "condicion": "canal == TRANSFERENCIA_INTERNACIONAL",
    "activa": true
  }'
```

### Activar/Desactivar

```bash
curl -X POST http://localhost:8080/api/reglas/1/toggle \
  -H "Authorization: Bearer <token>"
```

---

## 5. Ingesta y Procesamiento de Transacciones

**Flujo completo:** `POST /api/transacciones` → crea la transacción → evalúa reglas Drools → asigna score de riesgo → genera alertas si corresponde.

### Transacción normal (score bajo)

```bash
curl -X POST http://localhost:8080/api/transacciones \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionUuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "identificadorDocumento": "DNI12345678",
    "cuentaOrigen": "ES1234567890123456789012",
    "cuentaDestino": "ES9876543210987654321098",
    "monto": 1500.00,
    "moneda": "EUR",
    "canal": "WEB",
    "tipoTransaccion": "TRANSFERENCIA",
    "ipOrigen": "192.168.1.100",
    "paisOrigen": "NACIONAL",
    "fechaTransaccion": "2026-05-27T10:30:00"
  }'
```

**Response esperada:** `scoreRiesgo ≈ 0`, `estado: "APROBADA"`

### Transacción sospechosa (score alto → alerta)

```bash
curl -X POST http://localhost:8080/api/transacciones \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionUuid": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "identificadorDocumento": "DNI87654321",
    "cuentaOrigen": "ES1111111111111111111111",
    "cuentaDestino": "ES2222222222222222222222",
    "monto": 25000.00,
    "moneda": "USD",
    "canal": "TRANSFERENCIA_INTERNACIONAL",
    "tipoTransaccion": "TRANSFERENCIA",
    "ipOrigen": "45.33.32.156",
    "paisOrigen": "INTERNACIONAL",
    "fechaTransaccion": "2026-05-27T03:15:00"
  }'
```

**Response esperada:** `scoreRiesgo ≈ 75` (30 monto + 20 internacional + 25 canal), `estado: "SOSPECHOSA"`, alertas `ALTA` generadas.

### Estados de transacción

| Score | Estado |
|-------|--------|
| 0–39 | APROBADA |
| 40–69 | REVISION |
| 70+ | SOSPECHOSA |

### Consultar transacciones

```bash
# Listar todas
curl http://localhost:8080/api/transacciones \
  -H "Authorization: Bearer <token>"

# Por estado
curl http://localhost:8080/api/transacciones/estado/SOSPECHOSA \
  -H "Authorization: Bearer <token>"

# Por documento
curl http://localhost:8080/api/transacciones/documento/DNI12345678 \
  -H "Authorization: Bearer <token>"

# Por ID
curl http://localhost:8080/api/transacciones/1 \
  -H "Authorization: Bearer <token>"
```

---

## 6. Gestión de Alertas

Las alertas se generan automáticamente al procesar transacciones con `scoreRiesgo >= 70`.

### Listar alertas

```bash
curl http://localhost:8080/api/alertas \
  -H "Authorization: Bearer <token>"
```

### Filtrar por estado

```bash
curl http://localhost:8080/api/alertas/estado/PENDIENTE \
  -H "Authorization: Bearer <token>"
```

### Asignar alerta a analista

```bash
curl -X POST http://localhost:8080/api/alertas/1/asignar \
  -H "Authorization: Bearer <token>"
```
Asigna al analista autenticado. Registra `ASIGNAR_ALERTA` en auditoría.

### Resolver alerta

```bash
curl -X POST http://localhost:8080/api/alertas/1/resolver \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "observacion": "Cliente justificó la transferencia con factura comercial"
  }'
```

### Flujo completo de alertas

```
Transacción score>=70
       │
       ▼
  Alerta CREADA (estado=PENDIENTE)
       │
       ▼
  ANALISTA asigna ──→ Alerta ASIGNADA
       │
       ▼
  ANALISTA investiga
       │
       ├──→ RESUELTA (justificada)
       └──→ DESCARTADA (falso positivo)
```

### Estados de alerta

| Estado | Descripción |
|--------|-------------|
| PENDIENTE | Sin asignar |
| ASIGNADA | En investigación |
| RESUELTA | Investigación completada |
| DESCARTADA | Falso positivo |

---

## 7. KYC — Consulta a APIs Externas

Simula consultas de identidad, listas PEP y sancionados contra el mock server.

**Requisito:** Tener corriendo el mock server en `http://localhost:3001`.

```bash
curl -X POST http://localhost:8080/api/kyc/consultar \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "identificadorDocumento": "DNI12345678",
    "tipoConsulta": "PEP"
  }'
```

**Response esperada (mock):**
```json
{
  "identificadorDocumento": "DNI12345678",
  "tipoConsulta": "PEP",
  "resultado": false,
  "mensaje": "Sin coincidencias en PEP"
}
```

Tipos de consulta disponibles: `IDENTIDAD`, `PEP`, `SANCIONADOS`.

---

## 8. Dashboard — KPIs Agregados

```bash
curl http://localhost:8080/api/dashboard \
  -H "Authorization: Bearer <token>"
```

**Response:**
```json
{
  "totalTransacciones": 5,
  "transaccionesSospechosas": 2,
  "alertasPendientes": 2,
  "alertasResueltas": 1,
  "promedioScoreRiesgo": 35.2,
  "transaccionesPorEstado": {
    "APROBADA": 3,
    "REVISION": 0,
    "SOSPECHOSA": 2
  },
  "alertasPorPrioridad": {
    "ALTA": 1,
    "MEDIA": 2
  }
}
```

---

## 9. Reportes ROS (CSV)

```bash
curl http://localhost:8080/api/reportes/ros/1 \
  -H "Authorization: Bearer <token>" \
  -o reporte_ros_1.csv
```

Descarga un CSV con:
```csv
ID_ALERTA,ID_TRANSACCION,REGLA,PRIORIDAD,FECHA
1,2,Monto elevado > $10,000,ALTA,2026-05-27T20:01:00
```

---

## 10. Prueba End-to-End (Script Completo)

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@antifraude.com","password":"password"}' \
  | jq -r '.token')

# 2. Crear regla
curl -s -X POST http://localhost:8080/api/reglas \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Test alta","tipoRegla":"MONTO","severidad":"ALTA","condicion":"monto > 5000","activa":true}'

# 3. Ingerir transacción normal
curl -s -X POST http://localhost:8080/api/transacciones \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"transactionUuid":"e5f6a7b8-c9d0-1234-abcd-ef5678901234","identificadorDocumento":"DNI999","cuentaOrigen":"ES1","cuentaDestino":"ES2","monto":500,"moneda":"EUR","canal":"WEB","tipoTransaccion":"TRANSFERENCIA","paisOrigen":"NACIONAL","fechaTransaccion":"2026-05-27T12:00:00"}'

# 4. Ingerir transacción sospechosa
curl -s -X POST http://localhost:8080/api/transacciones \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"transactionUuid":"f6a7b8c9-d0e1-2345-abcd-ef6789012345","identificadorDocumento":"DNI888","cuentaOrigen":"ES3","cuentaDestino":"ES4","monto":50000,"moneda":"USD","canal":"TRANSFERENCIA_INTERNACIONAL","tipoTransaccion":"TRANSFERENCIA","paisOrigen":"INTERNACIONAL","fechaTransaccion":"2026-05-27T02:00:00"}'

# 5. Dashboard
curl -s http://localhost:8080/api/dashboard \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

## 11. Estructura de la Base de Datos

```
usuarios ──┬── auditoria
           ├── reglas_riesgo
           ├── alertas (asignado_a)
           └── reportes_ros

transacciones ──── alertas
reglas_riesgo ──── alertas
alertas ────────── reportes_ros
```

---

## 12. Notas Importantes

- **Doble UUID**: `transactionUuid` es único (idempotencia). Reintentar con el mismo UUID devuelve error.
- **Cuentas cifradas**: `cuentaOrigen`/`cuentaDestino` deben almacenarse cifradas con AES-256-GCM en producción (pendiente implementar `CryptoUtil`).
- **Auditoría automática**: cada login, asignación, resolución y toggle de regla se registra en `auditoria`.
- **Mock server**: `ExternalApiClient` apunta a `http://localhost:3001`. Debes tener el repositorio `sistema_antifraude_mock` corriendo para pruebas KYC.
- **Ids de prueba**: Los IDs en este documento suponen una BD fresca. Ajusta según los IDs reales de tu BD.
