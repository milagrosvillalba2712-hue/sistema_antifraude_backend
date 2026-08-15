param(
    [Parameter(Mandatory=$true)][string]$InputFile,
    [Parameter(Mandatory=$true)][string]$Token,
    [string]$ApiUrl = 'http://localhost:8080/api/transacciones',
    [ValidateRange(1,100000)][int]$Limit = 100000
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $InputFile -PathType Leaf)) { throw "No existe $InputFile" }
$rows = @(Import-Csv -LiteralPath $InputFile | Select-Object -First $Limit)
if (-not $rows.Count) { throw 'El CSV PaySim no contiene filas.' }
$required = 'step','type','amount','nameOrig','nameDest'
foreach ($column in $required) {
    if ($rows[0].PSObject.Properties.Name -notcontains $column) { throw "Falta columna PaySim: $column" }
}

$headers = @{ Authorization = "Bearer $Token"; 'Content-Type' = 'application/json' }
$loaded = 0
foreach ($row in $rows) {
    $body = @{
        transactionUuid = [guid]::NewGuid().ToString()
        identificadorDocumento = "PAYSIM-$($row.nameOrig)"
        cuentaOrigen = $row.nameOrig
        cuentaDestino = $row.nameDest
        monto = [decimal]$row.amount
        moneda = 'PYG'
        canal = 'API_DEMO'
        tipoTransaccion = $row.type
        paisOrigen = 'PY'
        paisDestino = 'PY'
        fechaTransaccion = ([datetime]'2026-01-01').AddMinutes([int]$row.step).ToString('s')
    } | ConvertTo-Json -Compress
    Invoke-RestMethod -Method Post -Uri $ApiUrl -Headers $headers -Body $body | Out-Null
    $loaded++
}
Write-Output "PAYSIM_LOADED=$loaded"
