param(
    [Parameter(Mandatory = $true)][string]$OutputFile
)

$ErrorActionPreference = 'Stop'
$required = 'DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_USER', 'PGPASSWORD'
foreach ($name in $required) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Falta la variable requerida $name"
    }
}

pg_dump --format=custom --no-owner --no-acl --host=$env:DB_HOST --port=$env:DB_PORT --username=$env:DB_USER --file=$OutputFile $env:DB_NAME
if ($LASTEXITCODE -ne 0) { throw 'pg_dump no pudo crear el backup' }

