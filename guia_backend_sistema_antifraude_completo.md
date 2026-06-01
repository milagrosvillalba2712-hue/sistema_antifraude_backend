
# Guía Backend - Sistema Antifraude

## Requisitos Previos

Las siguientes herramientas YA se encuentran instaladas y configuradas en el entorno de desarrollo:

- Docker Desktop
- Docker Compose
- JDK 17+
- Apache Maven
- Git
- Node.js
- npm
- Postman

Por lo tanto, este documento no incluye instrucciones detalladas de instalación de dichas herramientas y se enfoca únicamente en:

- Configuración del proyecto
- Estructura de repositorios
- Variables de entorno
- Inicialización de contenedores
- Ejecución local
- Flujo de desarrollo backend
- Plan de implementación

---

## Estructura de Repositorios

C:\Users\nicol\Projects\

├── sistema_antifraude_backend/      # API REST + Motor Drools
├── sistema_antifraude_frontend/     # Frontend React.js
├── sistema_antifraude_mock/         # APIs externas simuladas
└── sistema_antifraude_infra/        # Docker Compose + Infraestructura

---

## Arquitectura Recomendada

Se recomienda utilizar una arquitectura monolítica modularizada basada en:

- Java 17
- Spring Boot
- Spring Security + JWT
- Drools
- PostgreSQL
- Docker
- React.js
- Maven

El backend debe separarse por dominios y módulos internos:

- auth
- users
- alerts
- transactions
- rules
- audit
- kyc
- external-api
- reports
- dashboard

---

## Objetivo General

Diseñar e implementar un sistema antifraude con monitoreo normativo en tiempo real para detectar operaciones sospechosas y optimizar procesos regulatorios.

---

## Objetivos Técnicos

- Implementar API REST segura
- Integrar Drools
- Procesar transacciones en tiempo real
- Implementar JWT + RBAC
- Gestionar alertas
- Integrar APIs mock externas
- Generar reportes ROS
- Implementar trazabilidad y auditoría

---

## Requerimientos Funcionales Principales

- Ingesta de transacciones JSON
- Evaluación automática de reglas
- Gestión dinámica de reglas
- Dashboard estadístico
- Gestión de alertas
- KYC
- Consumo HTTPS de APIs externas
- Reportes CSV
- Auditoría completa

---

## Requerimientos No Funcionales

- Latencia <= 3 segundos
- 20 TPS mínimos
- BCrypt
- AES-256-GCM
- JWT
- RBAC
- Diseño responsive
- Protección contra SQL Injection
- Variables de entorno para secretos

---

## Instalación y Configuración del Proyecto

### 1. Clonar repositorios

```bash
git clone <repo_backend>
git clone <repo_frontend>
git clone <repo_mock>
git clone <repo_infra>
```

### 2. Backend

Entrar al repositorio backend:

```bash
cd sistema_antifraude_backend
```

### 3. Configurar variables de entorno

Crear archivo `.env`

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=antifraude
DB_USER=postgres
DB_PASSWORD=postgres

JWT_SECRET=jwt_secret

AES_SECRET=aes_secret

EXTERNAL_API_KEY=mock_api_key
```

### 4. Levantar infraestructura

```bash
docker compose up -d
```

### 5. Ejecutar backend

```bash
mvn clean install
mvn spring-boot:run
```

### 6. Ejecutar frontend

```bash
npm install
npm run dev
```

### 7. Validar endpoints

Usar Postman para:

- Login
- JWT
- Gestión de reglas
- Ingesta de transacciones
- Alertas
- KYC
- Reportes

---

## Estructura Recomendada Backend

```text
src/main/java/com/antifraude
|
├── config
├── security
├── auth
├── users
├── transactions
├── alerts
├── rules
├── audit
├── dashboard
├── reports
├── external
├── kyc
├── drools
├── repositories
├── services
├── controllers
└── dto
```

---

## Plan de Implementación

### Fase 1 - Infraestructura

- Configurar Docker
- Configurar PostgreSQL
- Configurar variables de entorno
- Crear repositorios

### Fase 2 - Seguridad

- JWT
- Roles
- BCrypt
- Protección de endpoints

### Fase 3 - Persistencia

- Entidades
- Repositories
- Migraciones

### Fase 4 - Motor de Reglas

- Integración Drools
- Reglas dinámicas
- Evaluación de fraude

### Fase 5 - Alertas

- Gestión de alertas
- Prioridades
- Auditoría

### Fase 6 - APIs Externas

- HTTPS
- API Key
- Retries
- Timeout

### Fase 7 - Dashboard y Reportes

- KPIs
- Métricas
- CSV ROS

### Fase 8 - Testing

- JMeter
- Integración
- Stress testing

---

## Nota Importante Sobre el Entorno

Dado que Docker, Java, Maven, Node.js, npm, Git y Postman ya se encuentran instalados, el onboarding del desarrollador backend debe centrarse exclusivamente en:

- Configuración del proyecto
- Ejecución de contenedores
- Desarrollo backend
- Integración de módulos
- Testing
- Seguridad
- Drools
- APIs externas
- Auditoría

---

# Estructura de Base de Datos (Modelo Preliminar DER)

## Consideraciones Arquitectónicas

La base de datos principal del sistema antifraude debe almacenar exclusivamente:

- Transacciones procesadas
- Alertas generadas
- Reglas de negocio
- Auditoría
- Usuarios y roles
- Estadísticas transaccionales
- Reportes generados

NO debe almacenar:

- Datos maestros reales de clientes
- Listas PEP reales
- Listas de sancionados reales
- Datos provenientes directamente de organismos externos

Los datos provenientes de APIs externas deben consultarse en tiempo real mediante el módulo mock/API client.

---

# Modelo Relacional Propuesto

## Tabla: usuarios

```sql
CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(30) NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    intentos_fallidos INT DEFAULT 0,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

Propósito:
- Gestión de autenticación
- RBAC (ADMINISTRADOR / ANALISTA)
- JWT Authentication

---

## Tabla: reglas_riesgo

```sql
CREATE TABLE reglas_riesgo (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    tipo_regla VARCHAR(50),
    severidad VARCHAR(20),
    condicion TEXT NOT NULL,
    activa BOOLEAN DEFAULT TRUE,
    creada_por BIGINT REFERENCES usuarios(id),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP
);
```

Propósito:
- Persistencia de reglas dinámicas Drools
- Parametrización de riesgo
- Activación/desactivación en tiempo real

---

## Tabla: transacciones

```sql
CREATE TABLE transacciones (
    id BIGSERIAL PRIMARY KEY,
    transaction_uuid UUID NOT NULL,
    identificador_documento VARCHAR(30),
    cuenta_origen TEXT NOT NULL,
    cuenta_destino TEXT NOT NULL,
    monto NUMERIC(18,2) NOT NULL,
    moneda VARCHAR(10),
    canal VARCHAR(30),
    tipo_transaccion VARCHAR(50),
    ip_origen VARCHAR(100),
    pais_origen VARCHAR(100),
    fecha_transaccion TIMESTAMP NOT NULL,
    estado VARCHAR(30),
    score_riesgo NUMERIC(5,2),
    procesada BOOLEAN DEFAULT FALSE,
    fecha_procesamiento TIMESTAMP
);
```

Notas importantes:
- cuenta_origen y cuenta_destino deben almacenarse cifradas con AES-256-GCM.
- transaction_uuid evita duplicados.
- Se recomienda indexar identificador_documento y fecha_transaccion.

Índices sugeridos:

```sql
CREATE INDEX idx_transacciones_documento
ON transacciones(identificador_documento);

CREATE INDEX idx_transacciones_fecha
ON transacciones(fecha_transaccion);

CREATE INDEX idx_transacciones_score
ON transacciones(score_riesgo);
```

---

## Tabla: alertas

```sql
CREATE TABLE alertas (
    id BIGSERIAL PRIMARY KEY,
    transaccion_id BIGINT REFERENCES transacciones(id),
    regla_id BIGINT REFERENCES reglas_riesgo(id),
    prioridad VARCHAR(20),
    estado VARCHAR(30),
    observacion TEXT,
    asignado_a BIGINT REFERENCES usuarios(id),
    fecha_generacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_resolucion TIMESTAMP
);
```

Estados sugeridos:
- PENDIENTE
- ASIGNADA
- INVESTIGANDO
- RESUELTA
- DESCARTADA

---

## Tabla: auditoria

```sql
CREATE TABLE auditoria (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT REFERENCES usuarios(id),
    accion VARCHAR(100) NOT NULL,
    descripcion TEXT,
    direccion_ip VARCHAR(100),
    entidad_afectada VARCHAR(100),
    entidad_id BIGINT,
    fecha_evento TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

Acciones mínimas:
- LOGIN
- LOGOUT
- CREAR_REGLA
- MODIFICAR_REGLA
- ACTIVAR_REGLA
- DESACTIVAR_REGLA
- ASIGNAR_ALERTA
- RESOLVER_ALERTA

---

## Tabla: estadisticas_cliente

```sql
CREATE TABLE estadisticas_cliente (
    id BIGSERIAL PRIMARY KEY,
    identificador_documento VARCHAR(30) UNIQUE,
    monto_promedio_semanal NUMERIC(18,2),
    frecuencia_diaria NUMERIC(10,2),
    horario_habitual_inicio TIME,
    horario_habitual_fin TIME,
    ultima_actualizacion TIMESTAMP
);
```

Propósito:
- Behavioral analytics
- Detección de anomalías
- Comparación contra comportamiento histórico

---

## Tabla: consultas_externas

```sql
CREATE TABLE consultas_externas (
    id BIGSERIAL PRIMARY KEY,
    identificador_documento VARCHAR(30),
    tipo_consulta VARCHAR(50),
    resultado BOOLEAN,
    usuario_id BIGINT REFERENCES usuarios(id),
    fecha_consulta TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

Tipos:
- IDENTIDAD
- PEP
- SANCIONADOS

Importante:
- No almacenar respuestas completas externas.
- Solo trazabilidad de consultas.

---

## Tabla: reportes_ros

```sql
CREATE TABLE reportes_ros (
    id BIGSERIAL PRIMARY KEY,
    alerta_id BIGINT REFERENCES alertas(id),
    generado_por BIGINT REFERENCES usuarios(id),
    nombre_archivo VARCHAR(255),
    fecha_generacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

Propósito:
- Generación de Reportes de Operaciones Sospechosas
- Exportación CSV

---

# Relaciones Principales

```text
usuarios
 ├── auditoria
 ├── reglas_riesgo
 ├── alertas
 └── reportes_ros

transacciones
 └── alertas

reglas_riesgo
 └── alertas

alertas
 └── reportes_ros
```

---

# Recomendaciones Técnicas

## ORM

Utilizar:
- Spring Data JPA
- Hibernate

Evitar:
- SQL manual
- concatenación de queries

---

## Migraciones

Se recomienda utilizar:

- Flyway (preferido)
o
- Liquibase

Estructura sugerida:

```text
src/main/resources/db/migration
|
├── V1__init.sql
├── V2__usuarios.sql
├── V3__reglas.sql
├── V4__transacciones.sql
├── V5__alertas.sql
└── V6__auditoria.sql
```

---

# Recomendación de Paquetes Backend

```text
com.antifraude
|
├── entities
├── repositories
├── services
├── controllers
├── dto
├── security
├── config
├── rules
├── audit
├── alerts
├── transactions
└── external
```

---

# Consideraciones de Seguridad

## Obligatorio

- BCrypt para passwords
- AES-256-GCM para cuentas
- JWT
- RBAC
- HTTPS
- Variables de entorno
- Protección SQL Injection

## Variables .env

```env
JWT_SECRET=
AES_SECRET=
DB_PASSWORD=
EXTERNAL_API_KEY=
SSL_KEYSTORE_PASSWORD=
```

---

# Recomendación Arquitectónica Importante

Se recomienda mantener:

- Monolito modularizado
- Separación clara por dominio
- Drools embebido en Spring Boot
- PostgreSQL como única fuente persistente principal

NO se recomienda inicialmente:
- Microservicios
- Kubernetes
- Event sourcing
- Kafka

Debido a:
- Complejidad innecesaria para MVP académico
- Mayor tiempo de implementación
- Sobrecarga operacional
