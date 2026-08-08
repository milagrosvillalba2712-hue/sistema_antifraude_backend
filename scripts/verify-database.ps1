param(
    [string]$EnvFile = (Join-Path (Split-Path $PSScriptRoot -Parent) '.env.canonical'),
    [string]$DatabaseName,
    [switch]$ExpectDemo
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $EnvFile)) { throw "No existe $EnvFile" }
$cfg=@{}; Get-Content -LiteralPath $EnvFile | ForEach-Object { if ($_ -match '^([^#=]+)=(.*)$') { $cfg[$Matches[1].Trim()]=$Matches[2].Trim() } }
if (-not [string]::IsNullOrWhiteSpace($DatabaseName)) {
    if ($DatabaseName -notmatch '^[a-z][a-z0-9_]{0,62}$') { throw 'DatabaseName no es un identificador PostgreSQL seguro.' }
    $cfg.DB_NAME=$DatabaseName
}
$expected = @('empresa','usuarios','usuario_empresa','rol','permiso','rol_permiso','perfil_usuario','disponibilidad_usuario','horario_laboral_usuario','plan_licencia','suscripcion','contrato','pago','uso_suscripcion','instalacion_local','licencia_local','consumo_licencia_local','evento_licencia_local','persona','documento','perfil_cliente','cliente_pep','cliente_observado','consulta_kyc_alerta','consultas_externas','pais','moneda','nivel_riesgo','tipo_documento','tipo_transaccion','canal_transaccion','producto','banco_emisor','procesadora_tarjeta','empe_operador','transacciones','transacciones_default','evaluaciones_riesgo','ejecucion_reglas','transaccion_detalle_snapshot','escenario','accion','reglas_riesgo','control_importe','control_frecuencia','horario_riesgo','calendario_riesgo','fuente_datos_riesgo','lista_regulatoria','elemento_lista','sujeto_riesgo','sujeto_riesgo_alias','sujeto_riesgo_documento','sujeto_riesgo_relacion','pais_riesgo','alertas_antifraude','hallazgo_alerta','coincidencia_lista_alerta','cliente_snapshot_alerta','historial_asignacion','estadistica_carga_analista','resolucion_alerta','aprobacion_supervisor','decision_caso','caso','caso_alerta','actuacion','comentario_caso','evidencia','evidencia_alerta','historial_estado_caso','reportes_ros','auditoria_sistema','servicio_externo')

$actual = @(docker exec -e "PGPASSWORD=$($cfg.DB_OWNER_PASSWORD)" regula-db-canonical psql -h localhost -U regula_owner -d $cfg.DB_NAME -At -c "select tablename from pg_tables where schemaname='public' and tablename <> 'flyway_schema_history' order by tablename")
$missing = @($expected | Where-Object { $_ -notin $actual })
if ($missing.Count) { throw "Tablas faltantes: $($missing -join ', ')" }
$history = docker exec -e "PGPASSWORD=$($cfg.DB_OWNER_PASSWORD)" regula-db-canonical psql -h localhost -U regula_owner -d $cfg.DB_NAME -At -c "select string_agg(version,',' order by installed_rank) from flyway_schema_history where success and version is not null"
if ($history -ne '1,2,3,4,5,6,7,8') { throw "Historial Flyway inesperado: $history" }
$demoCount = docker exec -e "PGPASSWORD=$($cfg.DB_OWNER_PASSWORD)" regula-db-canonical psql -h localhost -U regula_owner -d $cfg.DB_NAME -At -c "select count(*) from empresa where codigo='REGULA_DEMO'"
if ($ExpectDemo -and $demoCount -eq '0') { throw 'Se esperaba el perfil demo.' }
if (-not $ExpectDemo -and $demoCount -ne '0') { throw 'Produccion contiene datos demo.' }
$rls = docker exec -e "PGPASSWORD=$($cfg.DB_OWNER_PASSWORD)" regula-db-canonical psql -h localhost -U regula_owner -d $cfg.DB_NAME -At -c "select count(*) from pg_class c join pg_namespace n on n.oid=c.relnamespace where n.nspname='public' and relrowsecurity and relforcerowsecurity"
$roles = docker exec -e "PGPASSWORD=$($cfg.POSTGRES_ADMIN_PASSWORD)" regula-db-canonical psql -U postgres -d postgres -At -c "select count(*) from pg_roles where rolname in ('regula_owner','regula_app','regula_app_login','regula_readonly','regula_readonly_login','regula_batch')"
if ($roles -ne '6') { throw 'No existen los seis roles PostgreSQL esperados.' }
$roleFlags = docker exec -e "PGPASSWORD=$($cfg.POSTGRES_ADMIN_PASSWORD)" regula-db-canonical psql -U postgres -d postgres -At -c "select count(*) from pg_roles where rolname in ('regula_owner','regula_app_login','regula_readonly_login') and not rolsuper and not rolcreatedb and not rolcreaterole and not rolbypassrls"
if ($roleFlags -ne '3') { throw 'Owner/app/readonly conservan privilegios globales prohibidos.' }
$appGrants = docker exec -e "PGPASSWORD=$($cfg.DB_APP_PASSWORD)" regula-db-canonical psql -h localhost -U regula_app_login -d $cfg.DB_NAME -At -c "select has_schema_privilege(current_user,'public','CREATE'),has_table_privilege(current_user,'empresa','SELECT,INSERT,UPDATE,DELETE')"
if ($appGrants -ne 'f|t') { throw "Grants de regula_app_login invalidos: $appGrants" }
$readonlyGrants = docker exec -e "PGPASSWORD=$($cfg.DB_READONLY_PASSWORD)" regula-db-canonical psql -h localhost -U regula_readonly_login -d $cfg.DB_NAME -At -c "select has_schema_privilege(current_user,'public','CREATE'),has_table_privilege(current_user,'empresa','SELECT'),has_table_privilege(current_user,'empresa','INSERT,UPDATE,DELETE')"
if ($readonlyGrants -ne 'f|t|f') { throw "Grants de regula_readonly_login invalidos: $readonlyGrants" }
$rlsCrossTenant = docker exec -e "PGPASSWORD=$($cfg.DB_APP_PASSWORD)" regula-db-canonical psql -qAt -h localhost -U regula_app_login -d $cfg.DB_NAME -v ON_ERROR_STOP=1 -c "begin; insert into empresa(id,codigo,nombre) values ('10000000-0000-0000-0000-000000000001','RLS_GATE_A','RLS gate A'),('20000000-0000-0000-0000-000000000002','RLS_GATE_B','RLS gate B'); set local app.current_empresa_id='10000000-0000-0000-0000-000000000001'; insert into auditoria_sistema(empresa_id,accion,entidad_afectada) values ('10000000-0000-0000-0000-000000000001','RLS_GATE','verification'); set local app.current_empresa_id='20000000-0000-0000-0000-000000000002'; select count(*) from auditoria_sistema where accion='RLS_GATE'; rollback;"
if ($rlsCrossTenant -ne '0') { throw "RLS permitio acceso cruzado: $rlsCrossTenant" }
$externalColumns = docker exec -e "PGPASSWORD=$($cfg.DB_OWNER_PASSWORD)" regula-db-canonical psql -h localhost -U regula_owner -d $cfg.DB_NAME -At -c "select count(*) from information_schema.columns where table_schema='public' and table_name='consultas_externas' and column_name in ('proveedor','documento_hash','correlation_id','status_http','duracion_ms','intentos','resultado_funcional','categoria_error')"
if ($externalColumns -ne '8') { throw 'V6 de auditoria externa esta incompleta.' }
$forbidden = docker exec -e "PGPASSWORD=$($cfg.DB_OWNER_PASSWORD)" regula-db-canonical psql -h localhost -U regula_owner -d $cfg.DB_NAME -At -c "select count(*) from information_schema.columns where table_schema='public' and table_name='consultas_externas' and column_name in ('identificador_documento','respuesta_json','api_key')"
if ($forbidden -ne '0') { throw 'consultas_externas conserva columnas sensibles prohibidas.' }
Write-Output "DOMAIN_TABLES=$($expected.Count)"
Write-Output "FLYWAY=$history"
Write-Output "RLS_FORCED=$rls"
Write-Output 'APP_DDL=DENIED'
Write-Output 'READONLY_WRITE=DENIED'
Write-Output 'RLS_CROSS_TENANT=DENIED'
Write-Output "DATABASE_GATE=PASS"
