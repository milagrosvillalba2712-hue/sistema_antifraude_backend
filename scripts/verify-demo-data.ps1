param(
    [string]$EnvFile = (Join-Path (Split-Path $PSScriptRoot -Parent) '.env.canonical')
)

$ErrorActionPreference='Stop'
$cfg=@{}; Get-Content -LiteralPath $EnvFile | ForEach-Object { if ($_ -match '^([^#=]+)=(.*)$') { $cfg[$Matches[1].Trim()]=$Matches[2].Trim() } }
$database="$($cfg.DB_NAME)_demo"
if ($database -notmatch '^[a-z][a-z0-9_]{0,62}$') { throw 'Nombre de base demo invalido.' }
$tenant='00000000-0000-0000-0000-000000000001'
$minimums=[ordered]@{
  empresa=1; usuarios=28; usuario_empresa=28; rol=7; permiso=25; rol_permiso=25
  perfil_usuario=28; disponibilidad_usuario=14; horario_laboral_usuario=15
  plan_licencia=3; plan_plan_precios_rol=12; suscripcion=1; contrato=1; pago=1; uso_suscripcion=8
  instalacion_local=1; licencia_local=1; evento_licencia_local=2
  pais=7; moneda=4; nivel_riesgo=4; tipo_documento=3; tipo_transaccion=17
  canal_transaccion=12; producto=5; banco_emisor=5; procesadora_tarjeta=3; empe_operador=4
  persona=67; documento=67; perfil_cliente=67; cliente_pep=2; cliente_observado=3
  fuente_datos_riesgo=3; lista_regulatoria=3; elemento_lista=15; sujeto_riesgo=40
  sujeto_riesgo_alias=40; sujeto_riesgo_documento=40; sujeto_riesgo_relacion=5; pais_riesgo=3
  escenario=13; accion=13; reglas_riesgo=21; control_importe=12; control_frecuencia=12
  horario_riesgo=12; calendario_riesgo=12; transacciones=87; evaluaciones_riesgo=80
  ejecucion_reglas=80; transaccion_detalle_snapshot=80; alertas_antifraude=6
  hallazgo_alerta=6; coincidencia_lista_alerta=6; cliente_snapshot_alerta=6
  consulta_kyc_alerta=6; historial_asignacion=6; caso=6; caso_alerta=6; actuacion=6
  comentario_caso=6; evidencia=6; evidencia_alerta=6; historial_estado_caso=6
  resolucion_alerta=6; aprobacion_supervisor=6; decision_caso=6; reportes_ros=1
  estadistica_carga_analista=3; servicio_externo=3; consultas_externas=12; auditoria_sistema=2
}

$rows=[ordered]@{}
foreach($table in $minimums.Keys) {
  if ($table -notmatch '^[a-z_]+$') { throw "Tabla invalida en manifiesto: $table" }
  $sql="select set_config('app.current_empresa_id','$tenant',false); select count(*) from $table;"
  $result=@(docker exec -e "PGPASSWORD=$($cfg.DB_OWNER_PASSWORD)" regula-db-canonical psql -qAt -U regula_owner -d $database -c $sql)
  if ($LASTEXITCODE -ne 0) { throw "No se pudo contar $table" }
  $count=[long]$result[-1]
  $rows[$table]=$count
  if ($count -lt $minimums[$table]) { throw "$table contiene $count filas; minimo esperado $($minimums[$table])" }
}

$companyCount=docker exec -e "PGPASSWORD=$($cfg.DB_OWNER_PASSWORD)" regula-db-canonical psql -qAt -U regula_owner -d $database -c "select count(*) from empresa;"
if ($companyCount -ne '1') { throw "La instalacion on-premise demo debe contener una empresa; contiene $companyCount" }
$plainDocuments=docker exec -e "PGPASSWORD=$($cfg.DB_OWNER_PASSWORD)" regula-db-canonical psql -qAt -U regula_owner -d $database -c "select count(*) from documento where numero_documento_enc is not null;"
if ($plainDocuments -ne '0') { throw 'El seed demo almacena documentos reversibles; se esperaban solamente hashes.' }
$unsafeExternal=docker exec -e "PGPASSWORD=$($cfg.DB_OWNER_PASSWORD)" regula-db-canonical psql -qAt -U regula_owner -d $database -c "select count(*) from information_schema.columns where table_name='consultas_externas' and column_name in ('identificador_documento','respuesta_json','api_key');"
if ($unsafeExternal -ne '0') { throw 'consultas_externas contiene columnas sensibles prohibidas.' }

Write-Output "DEMO_DATABASE=$database"
Write-Output "MANIFEST_TABLES=$($minimums.Count)"
Write-Output "TRANSACTIONS=$($rows.transacciones)"
Write-Output "USERS=$($rows.usuarios)"
Write-Output "PEOPLE=$($rows.persona)"
Write-Output "EXTERNAL_AUDITS=$($rows.consultas_externas)"
Write-Output 'SINGLE_COMPANY=PASS'
Write-Output 'SYNTHETIC_PRIVACY=PASS'
Write-Output 'DEMO_DATA_GATE=PASS'
