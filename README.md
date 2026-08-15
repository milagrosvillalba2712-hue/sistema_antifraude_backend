# Sistema Antifraude - Backend

API REST para el sistema de prevención de fraude. Spring Boot 3.2.0 con motor de reglas Drools embebido.

## Características

- Autenticación JWT con BCrypt y AES-256-GCM
- Gestión de alertas con asignación automática y rebalanceo
- CRUD de reglas de negocio (Drools)
- Consulta KYC en tiempo real (identidad, PEP, sanciones)
- Exportación de reportes ROS (CSV)
- Dashboard con KPIs agregados
- Gestión de usuarios (solo ADMINISTRADOR)
- Perfil de usuario con estados de disponibilidad
- Auditoría automática de transacciones

## Stack Tecnológico

- **Java 17+** con Spring Boot 3.2.0
- **Spring Data JPA** + **Hibernate** (ddl-auto: update)
- **Spring Security** con JWT (jjwt 0.12.3)
- **PostgreSQL 16** como base de datos
- **Drools 8.44** para motor de reglas de fraude
- **SpringDoc OpenAPI** para documentación Swagger
- **Lombok** para reducir boilerplate
- **Docker Compose** para base de datos

## Requisitos Previos

- Java 17 o superior
- Maven 3.8+
- Docker (opcional, para PostgreSQL)

## Instalación

```bash
# Clonar repositorio
git clone https://github.com/milagrosvillalba2712-hue/sistema_antifraude_backend.git
cd sistema_antifraude_backend

# Instalar dependencias
mvn clean install
```

## Variables de Entorno

Crear archivo `.env` en la raíz:

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=antifraude
DB_USER=postgres
DB_PASSWORD=postgres
JWT_SECRET=tu_clave_secreta_jwt
AES_SECRET=tu_clave_secreta_aes
EXTERNAL_API_KEY=mock_api_key
```

## Ejecución

```bash
# 1. Iniciar PostgreSQL con Docker
docker compose up -d

# 2. Compilar el proyecto
mvn clean compile

# 3. Ejecutar la aplicación
mvn spring-boot:run
```

La API estará disponible en: `http://localhost:8080`

## Documentación API

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API Docs: `http://localhost:8080/api-docs`

## Estructura del Proyecto

```
src/main/java/com/antifraude/
├── alerts/          # Alerta, HistorialAsignacion, EstadisticaCargaAnalista
├── assignment/      # Asignación automática y rebalanceo de alertas
├── audit/           # Auditoría automática de transacciones
├── auth/            # Autenticación JWT
├── config/          # CORS, Drools, DataInitializer
├── dashboard/       # KPIs agregados
├── drools/          # Motor de reglas de fraude
├── dto/             # Request/Response records
├── exception/       # Manejador global de excepciones
├── external/        # Cliente API externa (mock server)
├── kyc/             # Consulta de identidad
├── profile/         # Perfil y disponibilidad de usuarios
├── reports/         # Exportación CSV de reportes ROS
├── rules/           # CRUD de reglas de riesgo
├── security/        # JWT, Filtros, SecurityConfig
├── transactions/    # Transacciones y estadísticas
└── users/           # Gestión de usuarios
```

## Migraciones SQL

Ubicación: `src/main/resources/db/migration/`

- `V1__init.sql` — Esquema completo inicial
- `V2__seed.sql` — Usuarios de prueba (admin/analista)
- `V3__add_new_tables.sql` — Perfiles, disponibilidad, historial de asignaciones

## Credenciales de Prueba

| Rol | Email | Contraseña |
|-----|-------|------------|
| Administrador | admin@antifraude.com | password |
| Analista | analista@antifraude.com | password |

## Endpoints Principales

| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| POST | `/api/auth/login` | Login y obtener JWT | No |
| GET | `/api/dashboard` | KPIs agregados | Sí |
| GET/PUT | `/api/alerts` | Gestión de alertas | Sí |
| POST | `/api/alerts/{id}/asignar` | Asignar alerta | Sí |
| POST | `/api/alerts/{id}/reasignar` | Reasignar alerta | Sí |
| POST | `/api/alerts/rebalancear` | Rebalancear carga | Sí |
| GET | `/api/alerts/{id}/timeline` | Timeline de alerta | Sí |
| GET/POST/PUT/DELETE | `/api/rules` | CRUD reglas de riesgo | ADMIN |
| GET | `/api/kyc/{documento}` | Consulta KYC | Sí |
| GET | `/api/reports/ros/export` | Exportar CSV | Sí |
| GET/POST | `/api/admin/users` | Gestión usuarios | ADMIN |
| GET/PUT | `/api/profile` | Perfil usuario | Sí |
| GET/PUT | `/api/disponibilidad` | Estado disponibilidad | Sí |

## Roles

- **ADMINISTRADOR**: Acceso completo a todos los módulos
- **ANALISTA**: Dashboard, alertas, KYC, reportes, perfil

## Licencia

Proyecto privado - Sistema Antifraude
