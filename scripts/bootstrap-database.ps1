param(
    [string]$EnvFile = (Join-Path (Split-Path $PSScriptRoot -Parent) '.env.canonical')
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
if (-not (Test-Path -LiteralPath $EnvFile)) { throw "No existe $EnvFile" }
$cfg = @{}
Get-Content -LiteralPath $EnvFile | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') { $cfg[$Matches[1].Trim()] = $Matches[2].Trim() }
}
$required = 'POSTGRES_ADMIN_PASSWORD','CANONICAL_DB_PORT','DB_NAME','DB_OWNER_PASSWORD','DB_APP_PASSWORD','DB_READONLY_PASSWORD'
foreach ($name in $required) {
    if ([string]::IsNullOrWhiteSpace($cfg[$name]) -or $cfg[$name] -match '^replace_') { throw "Falta $name" }
    if ($cfg[$name] -match '[\$\s]') { throw "$name contiene caracteres no seguros" }
}
if ($cfg.DB_NAME -notmatch '^[a-z][a-z0-9_]{0,62}$') { throw 'DB_NAME no es un identificador PostgreSQL seguro.' }

Push-Location $repo
try {
    docker compose --env-file $EnvFile up -d postgres-canonical
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar PostgreSQL canonico.' }
    $healthy = $false
    for ($i=0; $i -lt 30; $i++) {
        $status = docker inspect --format '{{.State.Health.Status}}' regula-db-canonical
        if ($status -eq 'healthy') { $healthy = $true; break }
        Start-Sleep -Seconds 2
    }
    if (-not $healthy) { throw 'PostgreSQL canonico no alcanzo estado healthy.' }

    docker cp scripts/provision-postgresql-roles.sql regula-db-canonical:/tmp/provision-postgresql-roles.sql
    docker exec -e "PGPASSWORD=$($cfg.POSTGRES_ADMIN_PASSWORD)" regula-db-canonical psql -U postgres -d postgres `
        --set="db_name=$($cfg.DB_NAME)" --set="owner_password=$($cfg.DB_OWNER_PASSWORD)" `
        --set="app_password=$($cfg.DB_APP_PASSWORD)" --set="readonly_password=$($cfg.DB_READONLY_PASSWORD)" `
        --file=/tmp/provision-postgresql-roles.sql
    if ($LASTEXITCODE -ne 0) { throw 'Fallo el bootstrap de roles/base.' }

    # V3 documenta un rol global existente. Los privilegios se conceden solo
    # durante Flyway y se revocan incluso si la migracion o Hibernate fallan.
    $migrationExit = 1
    try {
        docker exec -e "PGPASSWORD=$($cfg.POSTGRES_ADMIN_PASSWORD)" regula-db-canonical `
            psql -U postgres -d postgres -v ON_ERROR_STOP=1 -c `
            'ALTER ROLE regula_owner CREATEROLE; GRANT regula_batch TO regula_owner WITH ADMIN OPTION'
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo abrir la ventana temporal CREATEROLE.' }

        $env:DB_HOST='localhost'; $env:DB_PORT=$cfg.CANONICAL_DB_PORT; $env:DB_NAME=$cfg.DB_NAME
        $env:DB_USER='regula_owner'; $env:DB_PASSWORD=$cfg.DB_OWNER_PASSWORD
        $env:JWT_SECRET=$cfg.JWT_SECRET; $env:AES_SECRET=$cfg.AES_SECRET; $env:HMAC_SECRET=$cfg.HMAC_SECRET
        $env:SEED_ENABLED='false'; $env:SCHEDULER_ENABLED='false'
        mvn -q -DskipTests package
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo empaquetar el backend.' }
        $jar = Get-ChildItem -LiteralPath (Join-Path $repo 'target') -Filter '*.jar' | Where-Object Name -NotLike '*.original' | Select-Object -First 1
        if (-not $jar) { throw 'No se encontro el jar del backend.' }
        & java -jar $jar.FullName --spring.main.web-application-type=none --spring.main.banner-mode=off
        $migrationExit = $LASTEXITCODE
    } finally {
        docker exec -e "PGPASSWORD=$($cfg.POSTGRES_ADMIN_PASSWORD)" regula-db-canonical `
            psql -U postgres -d postgres -v ON_ERROR_STOP=1 -c `
            'REVOKE ADMIN OPTION FOR regula_batch FROM regula_owner; REVOKE regula_batch FROM regula_owner; ALTER ROLE regula_owner NOCREATEROLE'
        if ($LASTEXITCODE -ne 0) { throw 'No se pudieron revocar los privilegios temporales de regula_owner.' }
    }
    if ($migrationExit -ne 0) { throw 'Flyway/Hibernate no pudo inicializar la base canonica.' }
    Write-Output 'DATABASE_BOOTSTRAP=PASS'
} finally {
    Pop-Location
}
