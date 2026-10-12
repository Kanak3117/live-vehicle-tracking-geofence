$base = "http://localhost:8080"

# --- login: device token for pings, employee token for reading ETA ---
function Get-Token($user, $pass) {
    $body = @{ username = $user; password = $pass } | ConvertTo-Json
    (Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType "application/json" -Body $body).token
}

$deviceHeaders   = @{ Authorization = "Bearer " + (Get-Token "device" "device123") }
$employeeHeaders = @{ Authorization = "Bearer " + (Get-Token "employee" "employee123") }

for ($i = 0; $i -le 60; $i++) {
    $lat = [math]::Round(28.6020 + 0.0004 * $i, 6)           # ~44 m per step, ~10 m/s
    $ts  = (Get-Date).ToUniversalTime().ToString("yyyy-MM-dd'T'HH:mm:ss.fff'Z'")

    $body = @{
        deviceId  = "DEV-001"
        lat       = $lat
        lon       = 77.2090
        bearing   = 0
        speed     = 36
        timestamp = $ts
    } | ConvertTo-Json

    $r = Invoke-RestMethod -Method Post -Uri "$base/api/pings" -Headers $deviceHeaders -ContentType "application/json" -Body $body
    Write-Host ("ping {0,2}  lat={1}  status={2}" -f $i, $lat, $r.status)

    if ($i % 10 -eq 0) {
        try {
            $eta = Invoke-RestMethod -Uri "$base/api/vehicles/1/eta" -Headers $employeeHeaders
            $eta.pickups | ForEach-Object {
                Write-Host ("    ETA: {0} m left, {1} s, passed={2}" -f [math]::Round($_.remainingMeters), $_.etaSeconds, $_.passed) -ForegroundColor Cyan
            }
        } catch { Write-Host "    ETA not available yet" -ForegroundColor Yellow }
    }
    Start-Sleep -Seconds 1
}