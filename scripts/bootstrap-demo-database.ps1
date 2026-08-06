param(
    [string]$EnvFile = (Join-Path (Split-Path $PSScriptRoot -Parent) '.env.canonical')
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$cfg=@{}; Get-Content -LiteralPath $EnvFile | ForEach-Object { if ($_ -match '^([^#=]+)=(.*)$') { $cfg[$Matches[1].Trim()]=$Matches[2].Trim() } }
$demoDatabase = "$($cfg.DB_NAME)_demo"
if ($demoDatabase -notmatch '^[a-z][a-z0-9_]{0,62}$') { throw 'Nombre de base demo inválido.' }

Push-Location $repo
try {
    docker cp scripts/provision-postgresql-roles.sql regula-db-canonical:/tmp/provision-postgresql-roles.sql
    docker exec -e "PGPASSWORD=$($cfg.POSTGRES_ADMIN_PASSWORD)" regula-db-canonical psql -U postgres -d postgres `
        --set="db_name=$demoDatabase" --set="owner_password=$($cfg.DB_OWNER_PASSWORD)" `
        --set="app_password=$($cfg.DB_APP_PASSWORD)" --set="readonly_password=$($cfg.DB_READONLY_PASSWORD)" `
        --file=/tmp/provision-postgresql-roles.sql
    if ($LASTEXITCODE -ne 0) { throw 'Fallo el bootstrap de la base demo.' }

    try {
        docker exec -e "PGPASSWORD=$($cfg.POSTGRES_ADMIN_PASSWORD)" regula-db-canonical psql -U postgres -d postgres -v ON_ERROR_STOP=1 -c `
            'ALTER ROLE regula_owner CREATEROLE; GRANT regula_batch TO regula_owner WITH ADMIN OPTION'
        $env:DB_HOST='localhost'; $env:DB_PORT=$cfg.CANONICAL_DB_PORT; $env:DB_NAME=$demoDatabase
        $env:DB_USER='regula_owner'; $env:DB_PASSWORD=$cfg.DB_OWNER_PASSWORD
        $env:JWT_SECRET=$cfg.JWT_SECRET; $env:AES_SECRET=$cfg.AES_SECRET; $env:HMAC_SECRET=$cfg.HMAC_SECRET
        $env:SEED_ENABLED='false'; $env:SCHEDULER_ENABLED='false'; $env:SPRING_PROFILES_ACTIVE='demo'
        mvn -q -DskipTests package
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo empaquetar el backend.' }
        $jar = Get-ChildItem target -Filter '*.jar' | Where-Object Name -NotLike '*.original' | Select-Object -First 1
        & java -jar $jar.FullName --spring.main.web-application-type=none --spring.main.banner-mode=off
        if ($LASTEXITCODE -ne 0) { throw 'Flyway no pudo crear el perfil demo.' }
    } finally {
        docker exec -e "PGPASSWORD=$($cfg.POSTGRES_ADMIN_PASSWORD)" regula-db-canonical psql -U postgres -d postgres -v ON_ERROR_STOP=1 -c `
            'REVOKE ADMIN OPTION FOR regula_batch FROM regula_owner; REVOKE regula_batch FROM regula_owner; ALTER ROLE regula_owner NOCREATEROLE'
    }
    Write-Output "DEMO_DATABASE=$demoDatabase"
    Write-Output 'DEMO_BOOTSTRAP=PASS'
} finally { Pop-Location }
