param(
    [Parameter(Mandatory=$true)][string]$InputFile,
    [string]$DatabaseName='antifraude_legacy_recovered',
    [string]$EnvFile=(Join-Path (Split-Path $PSScriptRoot -Parent) '.env.canonical')
)

$ErrorActionPreference='Stop'
if(-not (Test-Path -LiteralPath $InputFile)){throw "No existe el backup $InputFile"}
if($DatabaseName -notmatch '^[a-z][a-z0-9_]{0,45}_recovered$'){
    throw 'La base de recuperacion debe terminar en _recovered.'
}
$cfg=@{}; Get-Content -LiteralPath $EnvFile | ForEach-Object {
    if($_ -match '^([^#=]+)=(.*)$'){$cfg[$Matches[1].Trim()]=$Matches[2].Trim()}
}
$exists=docker exec -e "PGPASSWORD=$($cfg.POSTGRES_ADMIN_PASSWORD)" regula-db-canonical `
    psql -U postgres -d postgres -qAt -c "select 1 from pg_database where datname='$DatabaseName'"
if($exists -eq '1'){throw "La base $DatabaseName ya existe; no se sobrescribira."}

$containerFile='/tmp/'+[IO.Path]::GetFileName($InputFile)
docker cp $InputFile "regula-db-canonical:$containerFile"
if($LASTEXITCODE -ne 0){throw 'No se pudo copiar el backup al contenedor.'}
docker exec -e "PGPASSWORD=$($cfg.POSTGRES_ADMIN_PASSWORD)" regula-db-canonical `
    createdb -U postgres $DatabaseName
if($LASTEXITCODE -ne 0){throw 'No se pudo crear la base de recuperacion.'}
docker exec -e "PGPASSWORD=$($cfg.POSTGRES_ADMIN_PASSWORD)" regula-db-canonical `
    pg_restore -U postgres -d $DatabaseName --no-owner --no-acl --exit-on-error $containerFile
if($LASTEXITCODE -ne 0){throw 'La restauracion fallo; revise la base aislada creada.'}

docker exec -e "PGPASSWORD=$($cfg.POSTGRES_ADMIN_PASSWORD)" regula-db-canonical `
    psql -U postgres -d $DatabaseName -At -c `
    "select 'tables='||count(*) from information_schema.tables where table_schema='public' and table_type='BASE TABLE'; select 'users='||count(*) from usuarios; select 'transactions='||count(*) from transacciones;"
Write-Output "LEGACY_RECOVERY_DATABASE=$DatabaseName"
Write-Output 'LEGACY_RECOVERY_GATE=PASS'
