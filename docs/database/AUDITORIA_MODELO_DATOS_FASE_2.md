# Auditoría integral del modelo de datos — Fase 2

Fecha: 2026-08-03  
Referencia: PostgreSQL 16, Java 17, Spring Boot 3.2, Flyway 10.8.1.

## Resultado ejecutivo

Se auditaron 67 entidades JPA, las migraciones legacy, la cadena canónica Flyway y los requisitos de operación on-premise. El esquema final contiene 74 tablas físicas, incluida la partición `transacciones_default`. Una base PostgreSQL vacía aplica cinco migraciones, Hibernate valida todas las entidades y la suite completa aprueba.

La arquitectura satisface el modelo acordado: cada cliente on-premise utiliza su propia base; las operaciones antifraude permanecen en esa base; el futuro control plane solo necesita identidad de instalación, lease firmado y contadores agregados. Las cuatro tablas locales añadidas no contienen documentos, transacciones, alertas ni casos.

## Fuentes contrastadas

- 67 entidades anotadas con `@Entity`.
- DDL core, ecosistema, hardening y alineación histórica.
- DDL generado por Hibernate para detectar drift completo.
- Esquema real creado desde cero mediante Testcontainers PostgreSQL 16.
- Requisitos de aislamiento, licencias, planes, contratos, consumo y auditoría.

## Hallazgos y correcciones

| Severidad | Hallazgo | Riesgo | Corrección |
|---|---|---|---|
| Crítica | Tres cadenas de esquema y versiones legacy incompatibles | Instalaciones no reproducibles | Flyway apunta solo a `db.productmigration`, con V1–V5 únicas |
| Crítica | Hibernate `update` y runners alteraban/cargaban datos al arrancar | Cambios silenciosos y credenciales demo | `ddl-auto=validate`; eliminados seis runners de esquema/seeds |
| Crítica | `Transaccion` usaba `IDENTITY` dentro de una PK compuesta | Hibernate no podía exportar el modelo | Secuencia explícita y PK `(id, fecha_transaccion)` conservada |
| Alta | 65 entidades iniciales no coincidían con varias tablas | Fallo de arranque y escrituras inválidas | Alineación integral de columnas, tipos, relaciones y tabla `producto` |
| Alta | Campos combinaban `@Transient` con `@Column`/relaciones | Auditoría perdida y consultas JPQL inválidas | Persistidos campos funcionales de reglas, listas, PEP, casos y disponibilidad |
| Alta | Repositorios consultaban atributos inexistentes | El contexto Spring no iniciaba | Consultas de alertas, disponibilidad y ejecución de reglas corregidas |
| Alta | Aspecto RLS se interceptaba a sí mismo | `StackOverflowError` | Servicio RLS excluido del pointcut y transacción redundante retirada |
| Alta | Flyway 9 no declaraba soporte para PostgreSQL 16 | Riesgo de compatibilidad | Flyway 10.8.1 y módulo PostgreSQL específico |
| Alta | No existía modelo local de activación/lease | Licenciamiento on-premise incompleto | Añadidas instalación, lease, consumo agregado y eventos sanitizados |
| Media | Roles de aplicación y lectura no estaban definidos | Backend podía terminar usando al propietario | Roles de grupo `regula_app` y `regula_readonly`, grants mínimos y sin LOGIN |

## Cadena Flyway canónica

| Versión | Responsabilidad |
|---|---|
| V1 | Core, empresa, usuarios, catálogos, personas, transacciones particionadas, alertas y evaluaciones |
| V2 | RBAC, KYC, fuentes/listas, screening, reglas, casos, evidencias, reportes y servicios externos |
| V3 | Auditoría, RLS forzado, índices, cifrado lógico y hardening |
| V4 | Convergencia completa JPA–DDL, tipos ISO `varchar`, relaciones auditables y secuencia transaccional |
| V5 | Identidad de instalación, lease local, consumo agregado, eventos de licencia y roles PostgreSQL |

## Resumen de tablas finales

| Dominio | Tablas | Finalidad |
|---|---|---|
| Organización, acceso y RBAC | `empresa`, `usuarios`, `usuario_empresa`, `rol`, `permiso`, `rol_permiso`, `perfil_usuario`, `disponibilidad_usuario`, `horario_laboral_usuario` | Cliente local, usuarios, roles, permisos y capacidad operativa |
| Planes y relación comercial local | `plan_licencia`, `suscripcion`, `contrato`, `pago`, `uso_suscripcion` | Plan contratado, vigencia, referencia contractual/pago y uso mensual |
| Instalación y lease | `instalacion_local`, `licencia_local`, `consumo_licencia_local`, `evento_licencia_local` | Identidad criptográfica, lease firmado, límites agregados y auditoría sanitizada |
| Personas y KYC | `persona`, `documento`, `perfil_cliente`, `cliente_pep`, `cliente_observado`, `consulta_kyc_alerta`, `consultas_externas` | Identidad local, documentos cifrados/hasheados, perfil y resultados KYC |
| Catálogos | `pais`, `moneda`, `nivel_riesgo`, `tipo_documento`, `tipo_transaccion`, `canal_transaccion`, `producto`, `banco_emisor`, `procesadora_tarjeta`, `empe_operador` | Dimensiones de referencia y ecosistema financiero paraguayo |
| Transacciones y evaluación | `transacciones`, `transacciones_default`, `evaluaciones_riesgo`, `ejecucion_reglas`, `transaccion_detalle_snapshot` | Hecho particionado, resultados, ejecución y evidencia inmutable |
| Motor de reglas | `escenario`, `accion`, `reglas_riesgo`, `control_importe`, `control_frecuencia`, `horario_riesgo`, `calendario_riesgo` | Configuración versionable de detección y controles |
| Fuentes y screening | `fuente_datos_riesgo`, `lista_regulatoria`, `elemento_lista`, `sujeto_riesgo`, `sujeto_riesgo_alias`, `sujeto_riesgo_documento`, `sujeto_riesgo_relacion`, `pais_riesgo` | Listas, sujetos, aliases, documentos, relaciones y países de riesgo |
| Alertas | `alertas_antifraude`, `hallazgo_alerta`, `coincidencia_lista_alerta`, `cliente_snapshot_alerta`, `historial_asignacion`, `estadistica_carga_analista`, `resolucion_alerta`, `aprobacion_supervisor`, `decision_caso` | Hallazgos, asignación, resolución, aprobación y trazabilidad |
| Casos y evidencia | `caso`, `caso_alerta`, `actuacion`, `comentario_caso`, `evidencia`, `evidencia_alerta`, `historial_estado_caso` | Investigación, actuaciones, comentarios, archivos y cambios de estado |
| Reportes y auditoría | `reportes_ros`, `auditoria_sistema`, `servicio_externo` | ROS, auditoría transversal y configuración de proveedores externos |

## Comprobaciones automatizadas

- Base vacía aplica exactamente V1–V5 y no existen versiones duplicadas.
- Hibernate inicia con `ddl-auto=validate` sobre PostgreSQL 16.
- `transacciones` sigue siendo tabla particionada.
- RLS está habilitado y forzado.
- `regula_app` solo ve la empresa configurada; la prueba inserta dos empresas y verifica conteo igual a uno.
- Las cuatro tablas locales de licencia existen y no contienen columnas de documentos, transacciones, alertas o casos.
- Los repositorios Spring Data se construyen y el servicio de alertas ejecuta consultas reales sin error.

## Límites deliberados

- La base local conserva referencias contractuales y de pago porque el backend actual ya las administra; un CRM/facturador completo sigue fuera del producto.
- El control plane central no forma parte de esta fase. V5 solo prepara el cache local del lease y contadores agregados.
- Las columnas de compatibilidad incorporadas en V4 permiten que el código actual funcione mientras se consolida gradualmente el vocabulario del dominio; no se deben volver a introducir migraciones o seeds legacy al runtime.
- Las credenciales LOGIN para propietario, aplicación y soporte deben crearse en el instalador o gestor de secretos. Las migraciones solo crean roles de grupo sin contraseña.

## Evidencia de aceptación

Comando: `mvn test -q`  
Resultado: aprobado, 18 pruebas, 0 fallos y 0 errores.  
Entorno probado: Eclipse Temurin Java 17.0.20 y PostgreSQL 16.14 en Testcontainers; Flyway validó base vacía, upgrade V4→V5, separación demo/productivo y restauración con `pg_dump`/`pg_restore`.
