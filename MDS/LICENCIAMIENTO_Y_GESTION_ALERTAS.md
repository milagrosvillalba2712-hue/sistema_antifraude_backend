# Licenciamiento SaaS Y Gestion Formal De Alertas

## Resumen
Se implemento una primera version funcional para convertir Regula en una plataforma SaaS multiempresa con licenciamiento anual, roles dinamicos, permisos granulares, motor de reglas mas dinamico y un flujo formal de investigacion de alertas.

La implementacion unifica el acceso: `usuarios` queda como identidad pura y el modelo operativo de roles/permisos vive en `usuario_empresa`, `rol`, `permiso` y `rol_permiso`.

## Nuevas Entidades SaaS Y RBAC

### `empresa`
Representa al cliente/tenant que contrata Regula.

Ejemplo: `DEMO - Empresa Demo Regula`.

Relaciones:
- Tiene suscripciones, contratos, pagos y consumo.
- Se asocia a usuarios mediante `usuario_empresa`.
- Se agrega como `empresa_id` nullable en entidades operativas principales para evolucionar hacia aislamiento por tenant.

### `plan_licencia`
Define planes vendibles y limites de uso.

Ejemplo: `ANUAL_PRO`, 50 usuarios, 100000 transacciones mensuales, modulos de motor, alertas, KYC, reportes y auditoria.

Relaciones:
- Una `suscripcion` usa un `plan_licencia`.

### `suscripcion`
Vigencia anual contratada por una empresa.

Ejemplo: empresa demo con fecha de inicio actual y fecha fin a un ano.

Relaciones:
- Pertenece a `empresa`.
- Usa `plan_licencia`.
- Puede tener contratos y pagos asociados.

### `contrato`
Documento comercial/legal que respalda una suscripcion.

Ejemplo: `CTR-DEMO-2026`.

Relaciones:
- Pertenece a `empresa`.
- Puede referenciar una `suscripcion`.

### `pago`
Historial financiero de la empresa cliente.

Ejemplo: `PAY-DEMO-001`, USD 12000, estado `PAGADO`.

Relaciones:
- Pertenece a `empresa`.
- Puede asociarse a una `suscripcion`.

### `uso_suscripcion`
Consumo mensual por empresa.

Campos principales:
- usuarios activos
- transacciones procesadas
- consultas KYC
- alertas generadas
- reportes generados

Uso esperado: comparar consumo contra limites del plan.

### `rol`, `permiso`, `rol_permiso`
Modelo RBAC dinamico.

Roles iniciales:
- `ADMIN_GENERAL`
- `ADMIN_EMPRESA`
- `GERENTE_SUPERVISOR`
- `ANALISTA`
- `AUDITOR`

Permisos ejemplo:
- `EMPRESAS_VER`
- `LICENCIAS_GESTIONAR`
- `REGLAS_EDITAR`
- `ALERTAS_RESOLVER`
- `AUDITORIA_VER`

### `usuario_empresa`
Une usuario, empresa y rol dinamico.

Ejemplo:
- `admin.empresa@demo.com` -> Empresa Demo -> `ADMIN_EMPRESA`
- `analista1@demo.com` -> Empresa Demo -> `ANALISTA`
- `admin.general@regula.com` -> Global -> `ADMIN_GENERAL`

## Pantallas Nuevas

### Admin General
Ruta: `/admin-general`

Permiso requerido: `EMPRESAS_VER`.

Muestra:
- empresas suscritas
- planes
- suscripciones
- pagos
- consumo
- roles
- permisos

Objetivo: administrar la infraestructura comercial y operativa completa de Regula.

### Admin Empresa
Ruta: `/admin-empresa`

Permiso requerido: `LICENCIAS_VER`.

Muestra:
- suscripcion activa
- pagos de la empresa
- consumo mensual
- empresa asociada al usuario

Objetivo: permitir que el cliente administrador revise su licencia, pagos y uso.

## Autenticacion Y Permisos

El login ahora devuelve el rol calculado desde `usuario_empresa`:

```json
{
  "token": "...",
  "tipo": "Bearer",
  "email": "admin.empresa@demo.com",
  "rol": "ADMIN_EMPRESA",
  "empresaId": 1,
  "rolId": 2,
  "permisos": ["LICENCIAS_VER", "PAGOS_VER", "USUARIOS_VER"]
}
```

El frontend guarda permisos en sesion y las rutas nuevas usan `requiredPermissions`.

## Motor De Reglas Dinamico

Se agrego:

`GET /api/rule-engine/facts`

Ejemplo de respuesta:

```json
[
  {
    "fact": "monto",
    "etiqueta": "Monto de la transaccion",
    "tipo": "NUMERICO",
    "catalogo": null,
    "operadores": [">", ">=", "<", "<=", "between"]
  },
  {
    "fact": "canal",
    "etiqueta": "Canal utilizado",
    "tipo": "CATALOGO",
    "catalogo": "canal",
    "operadores": ["==", "!=", "in"]
  }
]
```

Flujo en Constructor:
1. Usuario elige `Dato`.
2. La UI calcula operadores permitidos.
3. Si el dato es catalogo, el valor se carga desde la entidad correspondiente.
4. Si el dato es numerico, muestra input numerico.
5. Si el dato es booleano/existencia, muestra selector Si/No.
6. La vista previa se muestra en lenguaje comun.

Ejemplo:

```json
{
  "combinador": "ALL",
  "items": [
    { "fact": "monto", "operador": ">", "valor": 10000 },
    { "fact": "moneda", "operador": "==", "valor": "USD" }
  ]
}
```

Vista previa:

`Si Monto de la transaccion es mayor que 10000 y Moneda es igual a USD, sumar 40 puntos y sugerir REVISION_MANUAL.`

## Auditoria

`auditoria_sistema` fue ampliada con:
- `empresa_id`
- `user_agent`
- `valor_anterior_json`
- `valor_nuevo_json`

El CRUD generico de Motor de Reglas registra:
- creacion
- edicion
- eliminacion

Esto permite mostrar quien cambio una entidad, cuando y que valores se modificaron.

## Gestion Formal De Alertas

### Nuevas entidades

#### `resolucion_alerta`
Guarda el cierre formal de una alerta.

Campos:
- resultado
- conclusion
- decision
- justificacion
- evidencia
- contacto con cliente
- fondos retenidos
- movimiento liberable
- requiere ROS
- requiere bloqueo
- requiere escalamiento legal

#### `consulta_kyc_alerta`
Preparada para registrar consultas KYC por alerta.

#### `decision_caso`
Registra decisiones formales vinculadas a caso/alerta.

#### `aprobacion_supervisor`
Preparada para aprobaciones de cierre o escalamiento.

### Endpoints principales

```http
GET /api/alertas/analistas-disponibles
GET /api/alertas/{id}/detalle
POST /api/alertas/{id}/autoasignarme
POST /api/alertas/{id}/asignar
POST /api/alertas/{id}/resolver-formal
```

### Detalle de alerta

La vista muestra:
- datos de alerta
- transaccion completa
- regla que disparo la alerta
- datos basicos de cliente/KYC
- historial transaccional del cliente, hasta 15 registros
- servicios externos con estado `API externa no disponible`
- timeline
- resolucion formal si existe

### Resolucion formal

Resultados soportados:
- `FRAUDE_CONFIRMADO`
- `FALSO_POSITIVO`
- `OPERACION_JUSTIFICADA`
- `ESCALAR`
- `ROS_REQUERIDO`

Ejemplo:

```json
{
  "resultado": "FRAUDE_CONFIRMADO",
  "conclusion": "Operacion no reconocida por el cliente.",
  "decision": "Retener fondos y escalar a supervisor.",
  "justificacion": "Monto atipico, canal web e IP no habitual.",
  "evidenciaDescripcion": "Captura de validacion telefonica y trazas de IP.",
  "contactoCliente": "Cliente confirma desconocer la transaccion.",
  "fondosRetenidos": true,
  "movimientoLiberable": false,
  "requiereRos": true,
  "requiereBloqueo": true,
  "requiereEscalamientoLegal": true
}
```

## Seeds De Prueba

Se cargan:
- empresa demo
- plan anual pro
- suscripcion, contrato, pago y consumo
- roles y permisos iniciales
- relacion usuario-empresa-rol
- 10 analistas demo con disponibilidad
- 15 transacciones historicas para documento `12345678`

Credenciales utiles:
- Admin General: `admin.general1@regula.com`, `admin.general2@regula.com`, `admin.general3@regula.com`
- Admin Empresa: `admin.empresa1@demo.com`, `admin.empresa2@demo.com`, `admin.empresa3@demo.com`
- Gerente Supervisor: `supervisor1@demo.com`, `supervisor2@demo.com`, `supervisor3@demo.com`
- Analista: `analista1@demo.com` a `analista10@demo.com`
- Auditor: `auditor1@demo.com`, `auditor2@demo.com`, `auditor3@demo.com`
- Password para todos: `password`

## Pruebas Realizadas

Backend:

```bash
mvn test
```

Resultado: exitoso.

Frontend:

```bash
npm.cmd exec tsc -- --noEmit -p tsconfig.app.json
npm.cmd run build
```

Resultado: exitoso.

## Pruebas Manuales Sugeridas

1. Iniciar sesion como `admin.general@regula.com`.
2. Validar `/admin-general`.
3. Iniciar sesion como `admin.empresa@demo.com`.
4. Validar `/admin-empresa`.
5. Iniciar sesion como `supervisor@demo.com`.
6. Abrir `/rule-engine`.
7. En Constructor elegir `canal`; validar que Valor muestre canales.
8. Elegir `monto`; validar operadores numericos.
9. Crear una regla y revisar auditoria en `auditoria_sistema`.
10. Generar una transaccion de alto riesgo por `POST /api/transacciones`.
11. Abrir `/alerts`.
12. Asignar alerta a un analista disponible.
13. Revisar detalle, transaccion e historial del cliente.
14. Resolver formalmente con evidencia y decision.

## Limitaciones Y Deuda Tecnica

- En bases existentes puede quedar una columna fisica antigua `usuarios.rol`; ya no esta mapeada por JPA ni usada por seguridad. `SchemaCleanupRunner` elimina sus restricciones CHECK/NOT NULL para que no interfiera con el modelo unificado.
- `empresa_id` se agrego en entidades operativas principales como nullable. Falta completar filtros obligatorios por empresa en todos los endpoints.
- La creacion automatica de caso/reporte desde resolucion formal queda preparada como siguiente paso de negocio.
- Servicios externos estan representados en UI, pero todavia devuelven estado no disponible.
- Flyway sigue deshabilitado; falta consolidar una migracion limpia para ambientes productivos.
