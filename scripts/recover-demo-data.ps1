param(
    [string]$EnvFile = (Join-Path (Split-Path $PSScriptRoot -Parent) '.env.canonical'),
    [string]$BackupDirectory = (Join-Path (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent) 'backups')
)

$ErrorActionPreference='Stop'
$cfg=@{}; Get-Content -LiteralPath $EnvFile | ForEach-Object {
    if($_ -match '^([^#=]+)=(.*)$'){$cfg[$Matches[1].Trim()]=$Matches[2].Trim()}
}
$database="$($cfg.DB_NAME)_demo"
if($database -notmatch '^[a-z][a-z0-9_]{0,55}_demo$'){
    throw 'La recuperacion solo admite una base terminada en _demo.'
}

$stamp=Get-Date -Format 'yyyyMMdd_HHmmss'
$backup=Join-Path $BackupDirectory "$database-before-recovery-$stamp.dump"
New-Item -ItemType Directory -Path $BackupDirectory -Force | Out-Null

docker exec -e "PGPASSWORD=$($cfg.POSTGRES_ADMIN_PASSWORD)" regula-db-canonical `
    pg_dump -U postgres -d $database -Fc -f "/tmp/$database-before-recovery-$stamp.dump"
if($LASTEXITCODE -ne 0){throw 'No se pudo respaldar la base demo; recuperacion cancelada.'}
docker cp "regula-db-canonical:/tmp/$database-before-recovery-$stamp.dump" $backup
if($LASTEXITCODE -ne 0){throw 'No se pudo copiar el respaldo; recuperacion cancelada.'}
$hash=(Get-FileHash -LiteralPath $backup -Algorithm SHA256).Hash

& (Join-Path $PSScriptRoot 'bootstrap-demo-database.ps1') -EnvFile $EnvFile
if($LASTEXITCODE -ne 0){throw 'Flyway no pudo aplicar la recuperacion.'}
& (Join-Path $PSScriptRoot 'verify-database.ps1') -EnvFile $EnvFile -DatabaseName $database -ExpectDemo
& (Join-Path $PSScriptRoot 'verify-demo-data.ps1') -EnvFile $EnvFile

Write-Output "RECOVERY_BACKUP=$backup"
Write-Output "RECOVERY_BACKUP_SHA256=$hash"
Write-Output 'RECOVERY_GATE=PASS'
