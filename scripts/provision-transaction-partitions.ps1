param(
    [string]$EnvFile = (Join-Path (Split-Path $PSScriptRoot -Parent) '.env.canonical'),
    [string]$DatabaseName,
    [ValidateRange(12,24)]
    [int]$MesesAdelante = 24
)

# Rutina de INSTALADOR / actualización (matriz F/#10): aprovisiona 12-24 meses de
# particiones mensuales de `transacciones` con rol de mantenimiento (regula_owner),
# no con la app (regula_app_login no tiene DDL) ni con pg_cron. Idempotente.
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $EnvFile)) { throw "No existe $EnvFile" }
$cfg=@{}; Get-Content -LiteralPath $EnvFile | ForEach-Object { if ($_ -match '^([^#=]+)=(.*)$') { $cfg[$Matches[1].Trim()]=$Matches[2].Trim() } }
if ([string]::IsNullOrWhiteSpace($cfg.DB_NAME)) { throw 'Falta DB_NAME en el .env' }
if ([string]::IsNullOrWhiteSpace($cfg.DB_OWNER_PASSWORD) -or $cfg.DB_OWNER_PASSWORD -match '^replace_') { throw 'Falta DB_OWNER_PASSWORD en el .env' }

$target = $cfg.DB_NAME
if (-not [string]::IsNullOrWhiteSpace($DatabaseName)) {
    if ($DatabaseName -notmatch '^[a-z][a-z0-9_]{0,62}$') { throw 'DatabaseName no es un identificador PostgreSQL seguro.' }
    $target = $DatabaseName
}

docker cp (Join-Path $PSScriptRoot 'provision-transaction-partitions.sql') regula-db-canonical:/tmp/provision-transaction-partitions.sql
if ($LASTEXITCODE -ne 0) { throw 'No se pudo copiar el script al contenedor.' }

docker exec -e "PGPASSWORD=$($cfg.DB_OWNER_PASSWORD)" regula-db-canonical psql -h localhost -U regula_owner -d $target `
    -v ON_ERROR_STOP=1 -v meses_adelante=$MesesAdelante --file=/tmp/provision-transaction-partitions.sql
if ($LASTEXITCODE -ne 0) { throw "Fallo el aprovisionamiento de particiones en $target." }

$margen = docker exec -e "PGPASSWORD=$($cfg.DB_OWNER_PASSWORD)" regula-db-canonical psql -h localhost -U regula_owner -d $target -qAt -c 'SELECT public.fn_meses_particion_disponibles();'
Write-Output "PARTITIONS_DB=$target"
Write-Output "HORIZON_MONTHS=$margen"
Write-Output "PARTITION_PROVISION=PASS"