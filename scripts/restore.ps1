param(
    [Parameter(Mandatory = $true)][string]$InputFile
)

$ErrorActionPreference = 'Stop'
$required = 'DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_USER', 'PGPASSWORD'
foreach ($name in $required) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Falta la variable requerida $name"
    }
}
if (-not (Test-Path -LiteralPath $InputFile)) { throw "No existe el backup $InputFile" }

pg_restore --clean --if-exists --no-owner --no-acl --exit-on-error --host=$env:DB_HOST --port=$env:DB_PORT --username=$env:DB_USER --dbname=$env:DB_NAME $InputFile
if ($LASTEXITCODE -ne 0) { throw 'pg_restore no pudo restaurar el backup' }

