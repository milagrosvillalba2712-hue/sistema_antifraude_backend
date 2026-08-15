param(
    [string]$CanonicalEnv = (Join-Path (Split-Path $PSScriptRoot -Parent) '.env.canonical'),
    [string]$MockRepo = (Join-Path (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent) 'sistema_antifraude_mock'),
    [int]$BackendPort = 18080
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
function Read-DotEnv([string]$path) {
    $result=@{}; Get-Content -LiteralPath $path | ForEach-Object { if ($_ -match '^([^#=]+)=(.*)$') { $result[$Matches[1].Trim()]=$Matches[2].Trim() } }; return $result
}
function Invoke-Json([string]$uri, [string]$body, [hashtable]$headers=@{}) {
    try {
        $response=Invoke-WebRequest -UseBasicParsing -Method Post -Uri $uri -Headers $headers -ContentType 'application/json' -Body $body -TimeoutSec 10
        return @{ Status=[int]$response.StatusCode; Body=$response.Content }
    } catch {
        if ($_.Exception.Response) { return @{ Status=[int]$_.Exception.Response.StatusCode; Body='' } }
        throw
    }
}
function Invoke-JsonGet([string]$uri, [hashtable]$headers=@{}) {
    try {
        $response=Invoke-WebRequest -UseBasicParsing -Method Get -Uri $uri -Headers $headers -TimeoutSec 10
        return @{ Status=[int]$response.StatusCode; Body=$response.Content }
    } catch {
        if ($_.Exception.Response) { return @{ Status=[int]$_.Exception.Response.StatusCode; Body='' } }
        throw
    }
}

$db=Read-DotEnv $CanonicalEnv
$mock=Read-DotEnv (Join-Path $MockRepo '.env')
$trustStore=(Resolve-Path (Join-Path $repo 'certificates\regula-external-truststore.p12')).Path
$jar=$null; $backend=$null; $log=Join-Path $repo 'target\thesis-backend.log'; $err=Join-Path $repo 'target\thesis-backend-error.log'

Push-Location $repo
try {
    $env:DB_HOST='localhost'; $env:DB_PORT=$db.CANONICAL_DB_PORT; $env:DB_NAME="$($db.DB_NAME)_demo"
    $env:DB_USER='regula_app_login'; $env:DB_PASSWORD=$db.DB_APP_PASSWORD
    $env:FLYWAY_DB_USER='regula_owner'; $env:FLYWAY_DB_PASSWORD=$db.DB_OWNER_PASSWORD
    $env:JWT_SECRET=$db.JWT_SECRET; $env:AES_SECRET=$db.AES_SECRET; $env:HMAC_SECRET=$db.HMAC_SECRET
    $env:CORS_ALLOWED_ORIGINS='http://localhost:5173,http://127.0.0.1:5173'
    $env:EXTERNAL_TRUSTSTORE="file:$($trustStore.Replace('\','/'))"; $env:EXTERNAL_TRUSTSTORE_PASSWORD=$db.EXTERNAL_TRUSTSTORE_PASSWORD
    $env:IDENTIFICACIONES_API_URL='https://localhost:8443'; $env:SANCIONES_API_URL='https://localhost:8443'; $env:PEP_API_URL='https://localhost:8443'
    $env:IDENTIFICACIONES_API_KEY=$mock.MOCK_OPERATIONAL_API_KEY; $env:SANCIONES_API_KEY=$mock.MOCK_OPERATIONAL_API_KEY; $env:PEP_API_KEY=$mock.MOCK_OPERATIONAL_API_KEY
    $env:SPRING_PROFILES_ACTIVE='prod,demo'; $env:SEED_ENABLED='false'; $env:SCHEDULER_ENABLED='false'
    $env:SERVER_PORT="$BackendPort"

    mvn -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo empaquetar el backend.' }
    $jar=Get-ChildItem target -Filter '*.jar' | Where-Object Name -NotLike '*.original' | Select-Object -First 1
    $backend=Start-Process -FilePath java -ArgumentList @('-jar',$jar.FullName) -WorkingDirectory $repo -WindowStyle Hidden -PassThru -RedirectStandardOutput $log -RedirectStandardError $err
    $ready=$false
    for ($i=0; $i -lt 45; $i++) {
        if ($backend.HasExited) { throw "Backend terminó durante el arranque. Revise $log" }
        try { Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:$BackendPort/api/auth/login" -Method Options -TimeoutSec 2 | Out-Null; $ready=$true; break } catch { if ($_.Exception.Response) { $ready=$true; break } }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw 'Backend no quedó disponible en 45 segundos.' }

    $tenantSql="set app.current_empresa_id='00000000-0000-0000-0000-000000000001';"
    $auditBaseline=docker exec -e "PGPASSWORD=$($db.DB_APP_PASSWORD)" regula-db-canonical psql -qAt -U regula_app_login -d "$($db.DB_NAME)_demo" -c "$tenantSql select coalesce(max(id),0) from consultas_externas;"

    $baseUrl="http://localhost:$BackendPort"
    $login=Invoke-Json "$baseUrl/api/auth/login" '{"email":"administrador@santaclara.local","password":"Regula2026!"}'
    if ($login.Status -ne 200) { throw "Login demo falló con HTTP $($login.Status)." }
    $token=($login.Body | ConvertFrom-Json).token
    if ([string]::IsNullOrWhiteSpace($token)) { throw 'Login no devolvió JWT.' }
    $auth=@{ Authorization="Bearer $token" }

    foreach ($case in @(@('100','IDENTIDAD'),@('200','SANCIONES'),@('300','PEP'))) {
        $response=Invoke-Json "$baseUrl/api/kyc/consultar" (@{identificadorDocumento=$case[0];tipoConsulta=$case[1]} | ConvertTo-Json -Compress) $auth
        if ($response.Status -ne 200) { throw "KYC $($case[1]) falló con HTTP $($response.Status). Body: $($response.Body)" }
    }
    $alertas=Invoke-JsonGet "$baseUrl/api/alertas?page=0&size=1&sort=recientes" $auth
    if ($alertas.Status -ne 200) { throw "Listado de alertas falló con HTTP $($alertas.Status)." }
    $alertasBody=$alertas.Body | ConvertFrom-Json
    $primeraAlerta=$alertasBody.content | Select-Object -First 1
    if (-not $primeraAlerta) { throw 'No hay alertas demo para validar detalle de cliente.' }
    $detalle=Invoke-JsonGet "$baseUrl/api/alertas/$($primeraAlerta.id)/detalle" $auth
    if ($detalle.Status -ne 200) { throw "Detalle de alerta $($primeraAlerta.id) falló con HTTP $($detalle.Status)." }
    $detalleBody=$detalle.Body | ConvertFrom-Json
    if ($detalleBody.cliente.fuente -ne 'MOCK_EXTERNO_REGULA') {
        throw "Detalle de cliente usa fuente inesperada: $($detalleBody.cliente.fuente)."
    }
    if ($detalleBody.cliente.personal.'Tipo De Documento' -eq 'Pendiente API externa' -or
        [string]::IsNullOrWhiteSpace($detalleBody.cliente.personal.'Tipo De Documento')) {
        throw 'Detalle de cliente no incluye Tipo De Documento desde el mock externo.'
    }
    if ($detalleBody.cliente.personal.'Fecha De Nacimiento' -eq 'Pendiente API externa' -or
        [string]::IsNullOrWhiteSpace($detalleBody.cliente.personal.'Fecha De Nacimiento')) {
        throw 'Detalle de cliente no incluye Fecha De Nacimiento desde el mock externo.'
    }
    if (@($detalleBody.historialTransaccional).Count -lt 15) {
        throw "Historial transaccional externo incompleto: $(@($detalleBody.historialTransaccional).Count) registros."
    }
    foreach ($document in 'not-found','rate-limit','server-error','unavailable','invalid-json') {
        # El breaker de la demostración permanece abierto dos segundos. Esperar
        # permite probar cada fallo contra el proveedor y también su recuperación.
        Start-Sleep -Seconds 3
        $response=Invoke-Json "$baseUrl/api/kyc/consultar" (@{identificadorDocumento=$document;tipoConsulta='IDENTIDAD'} | ConvertTo-Json -Compress) $auth
        if ($response.Status -eq 200) { throw "El escenario $document debía fallar controladamente." }
    }
    Start-Sleep -Seconds 3
    $watch=[Diagnostics.Stopwatch]::StartNew()
    $timeout=Invoke-Json "$baseUrl/api/kyc/consultar" '{"identificadorDocumento":"timeout","tipoConsulta":"IDENTIDAD"}' $auth
    $watch.Stop()
    if ($timeout.Status -eq 200 -or $watch.Elapsed.TotalSeconds -ge 5) { throw "Timeout inválido: HTTP $($timeout.Status), $($watch.Elapsed.TotalSeconds)s" }
    # El backend corta antes de cinco segundos; el mock termina su escenario
    # lento a los siete segundos y recién entonces persiste el journal.
    Start-Sleep -Seconds 8

    $sql="$tenantSql select correlation_id from consultas_externas where id > $auditBaseline and correlation_id is not null order by id;"
    $correlations=@(docker exec -e "PGPASSWORD=$($db.DB_APP_PASSWORD)" regula-db-canonical psql -qAt -U regula_app_login -d "$($db.DB_NAME)_demo" -c $sql)
    if ($correlations.Count -lt 9) { throw "Auditoría externa incompleta: $($correlations.Count) registros." }
    $mockContainer=docker compose -f (Join-Path $MockRepo 'docker-compose.yml') --env-file (Join-Path $MockRepo '.env') ps -q mock-api
    $journal=@(docker exec $mockContainer tail -n 100 /var/lib/regula-mock/audit/events.jsonl)
    foreach ($correlation in $correlations) {
        if (-not ($journal -match [regex]::Escape($correlation))) { throw "Correlation ID no encontrado en el mock: $correlation" }
    }
    $forbidden=docker exec -e "PGPASSWORD=$($db.DB_APP_PASSWORD)" regula-db-canonical psql -qAt -U regula_app_login -d "$($db.DB_NAME)_demo" -c "set app.current_empresa_id='00000000-0000-0000-0000-000000000001'; select count(*) from information_schema.columns where table_name='consultas_externas' and column_name in ('identificador_documento','respuesta_json','api_key');"
    if ($forbidden -ne '0') { throw 'La auditoría externa conserva columnas sensibles.' }
    $logText=(Get-Content -LiteralPath $log -Raw) + ($journal -join "`n")
    foreach ($secret in @($mock.MOCK_OPERATIONAL_API_KEY,$mock.MOCK_ADMIN_API_KEY,$db.DB_APP_PASSWORD,$db.DB_OWNER_PASSWORD,'not-found','rate-limit','server-error','unavailable','invalid-json','timeout')) {
        if ($logText.Contains($secret)) { throw 'Se detectó un secreto o documento de prueba en logs/journal.' }
    }
    Write-Output "KYC_AUDITS=$($correlations.Count)"
    Write-Output "ALERTA_DETALLE_CLIENTE=MOCK_EXTERNO_REGULA"
    Write-Output "ALERTA_HISTORIAL=$(@($detalleBody.historialTransaccional).Count)"
    Write-Output "TIMEOUT_SECONDS=$([math]::Round($watch.Elapsed.TotalSeconds,2))"
    Write-Output 'CORRELATION_IDS=MATCH'
    Write-Output 'E2E_GATE=PASS'
} finally {
    if ($backend -and -not $backend.HasExited) { Stop-Process -Id $backend.Id -Force }
    Pop-Location
}
