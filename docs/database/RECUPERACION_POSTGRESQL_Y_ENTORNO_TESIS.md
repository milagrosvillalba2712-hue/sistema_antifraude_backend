# Recuperación PostgreSQL y entorno de tesis

## Estado validado

- PostgreSQL de referencia: 16.14 (compatible con la referencia 16).
- Flyway: 10.8.1.
- Cadena productiva: V1–V8, sin versiones duplicadas.
- Esquema productivo: 74 tablas de dominio más `flyway_schema_history`.
- Perfil productivo: sin empresas ni usuarios demo.
- Perfil académico: base separada `antifraude_demo`, con siete escenarios sintéticos.
- Backend: Java 17 y `ddl-auto=validate`.
- Mock: HTTPS con CA académica privada, API Keys separadas y journal sanitizado.

V7 elimina `respuesta_json` de `consultas_externas`. V8 permite que un usuario ya autenticado descubra únicamente su propia asignación de tenant antes de emitir el JWT; las escrituras continúan exigiendo `empresa_id`.

## Base legacy preservada

- Etiqueta lógica: `legacy-demo-pre-flyway`.
- Volumen conservado: `sistema_antifraude_backend_pgdata`.
- Backup local ignorado por Git: `backups/legacy-demo-pre-flyway.dump`.
- SHA-256: `D4929982B34E88F8835F4A7C146E8DD83F077E4503A627FF4548A63727385337`.
- Inventario observado: PostgreSQL 16.14, 99 relaciones físicas, 68 tablas lógicas y 31 particiones.
- Conteos restaurados: 6 empresas, 24 usuarios, 11 transacciones, 10 alertas, 10 casos y 120 eventos de auditoría.

El dump fue restaurado en una base temporal y los conteos coincidieron. La base temporal de comprobación fue retirada después del gate; el dump y el volumen original permanecen intactos.

### Rollback temporal a legacy

1. Detener el backend para impedir escrituras.
2. Levantar únicamente el servicio `postgres` de `docker-compose.yml`, que usa el volumen legacy.
3. Configurar el backend contra el puerto legacy y sus credenciales anteriores.
4. No ejecutar Flyway sobre esa base: no contiene `flyway_schema_history`.
5. Para volver al esquema canónico, detener el backend y apuntarlo otra vez a `postgres-canonical` (puerto local 5433).

Cambiar de volumen es el mecanismo de rollback. No se transforma ni elimina el volumen legacy.

## Arranque reproducible

1. Copiar `.env.canonical.example` a `.env.canonical` y generar valores Base64URL aleatorios.
2. Ejecutar con Java 17:

   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap-database.ps1
   powershell -ExecutionPolicy Bypass -File .\scripts\verify-database.ps1
   ```

3. Para la defensa, crear la base académica separada:

   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap-demo-database.ps1
   ```

4. En el repositorio mock, generar certificados/secretos y ejecutar `verify-mock.ps1`.
5. Ejecutar la aceptación conjunta:

   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\verify-thesis-environment.ps1
   ```

## Gates obtenidos el 5 de agosto de 2026

- Mock Maven: 6/6 pruebas.
- Mock: contenedor saludable; CA válida aceptada; CA desconocida y hostname incorrecto rechazados.
- Base: 74 tablas; V1–V8; 44 tablas con RLS forzado.
- Privilegios: aplicación sin DDL, readonly sin escritura, lectura cruzada RLS rechazada.
- Backend: 22/22 pruebas con Testcontainers.
- Datos: producción vacía; demo con 7 transacciones, 6 alertas, 6 casos, 6 resoluciones y 1 ROS.
- E2E: 9 consultas auditadas; correlation IDs coincidentes; timeout total 4,46 segundos.
- Frontend: build TypeScript/Vite aprobado.

## Datos académicos y carga

El contrato de conteos está en `docs/database/demo-data-manifest.json`. Las cuatro tablas locales de licenciamiento y `consultas_externas` permanecen vacías hasta que los flujos runtime las utilicen.

PaySim se carga explícitamente, con un máximo de 100.000 filas, mediante `scripts/load-paysim.ps1`. Nunca se ejecuta como migración Flyway ni en el perfil productivo.

## Secretos

Los `.env`, certificados privados, truststores y backups permanecen fuera de Git. Si un secreto fue compartido previamente, debe considerarse revocado. Ninguna API Key, contraseña, documento plano o respuesta externa completa forma parte de este documento, los logs de aceptación o `consultas_externas`.
