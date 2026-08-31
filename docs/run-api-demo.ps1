# AmenityHub API demo runner (Windows PowerShell friendly)
#
# Runs the full flow against a running app on http://localhost:9099:
#   login as manager -> create resource -> create slot ->
#   register two residents -> book (confirm + waitlist) ->
#   cancel -> observe auto-promotion.
#
# Usage:  from c:\amenityhub-backend  ->  powershell -File docs\run-api-demo.ps1
# Requires: the app running (.\gradlew.bat bootRun) and Docker containers up.

$ErrorActionPreference = "Stop"
$base = "http://localhost:9099/api/v1"
$tmp  = $env:TEMP
$enc  = New-Object System.Text.UTF8Encoding($false)
$at   = "@"   # built at runtime so email addresses are transmitted intact

function Write-Json($name, $obj) {
    $path = Join-Path $tmp $name
    [System.IO.File]::WriteAllText($path, ($obj | ConvertTo-Json -Compress), $enc)
    return $path
}

function Invoke-Api($method, $url, $bodyObj, $token) {
    $args = @("-s", "-X", $method, $url)
    if ($token)   { $args += @("-H", "Authorization: Bearer $token") }
    if ($bodyObj) {
        $file = Write-Json "req.json" $bodyObj
        $args += @("-H", "Content-Type: application/json", "--data-binary", "@$file")
    }
    return (& curl.exe @args)
}

Write-Host "=== 1. Login as manager ===" -ForegroundColor Cyan
$mgrResp = Invoke-Api "POST" "$base/auth/login" @{ email = ("manager" + $at + "amenityhub.local"); password = "manager1234" } $null
$mgrToken = ($mgrResp | ConvertFrom-Json).accessToken
Write-Host "manager token acquired (len=$($mgrToken.Length))"

Write-Host "`n=== 2. Create resource ===" -ForegroundColor Cyan
$resResp = Invoke-Api "POST" "$base/resources" @{ name = "Demo Gym"; description = "Demo"; resourceType = "GYM"; capacity = 1; cancellationWindowHours = 2 } $mgrToken
$resId = ($resResp | ConvertFrom-Json).id
Write-Host "resource created: id=$resId"

Write-Host "`n=== 3. Create slot (capacity 1) ===" -ForegroundColor Cyan
$start = (Get-Date).ToUniversalTime().AddDays(2).ToString("yyyy-MM-ddTHH:mm:ssZ")
$end   = (Get-Date).ToUniversalTime().AddDays(2).AddHours(1).ToString("yyyy-MM-ddTHH:mm:ssZ")
$slotResp = Invoke-Api "POST" "$base/resources/$resId/slots" @{ startTime = $start; endTime = $end; totalCapacity = 1 } $mgrToken
$slotId = ($slotResp | ConvertFrom-Json).id
Write-Host "slot created: id=$slotId ($start -> $end)"

Write-Host "`n=== 4. Register two residents ===" -ForegroundColor Cyan
function Get-ResidentToken($local) {
    $email = $local + $at + "example.com"
    Invoke-Api "POST" "$base/auth/register" @{ email = $email; password = "password123"; fullName = $local } $null | Out-Null
    return (Invoke-Api "POST" "$base/auth/login" @{ email = $email; password = "password123" } $null | ConvertFrom-Json).accessToken
}
$alice = Get-ResidentToken "alice"
$bob   = Get-ResidentToken "bob"
Write-Host "alice + bob registered"

Write-Host "`n=== 5. Alice books the slot (expect CONFIRMED / 201) ===" -ForegroundColor Cyan
$aBook = Invoke-Api "POST" "$base/bookings" @{ slotId = [int]$slotId; joinWaitlistIfFull = $true } $alice
Write-Host $aBook
$aBookId = ($aBook | ConvertFrom-Json).booking.id

Write-Host "`n=== 6. Bob books the same slot (full -> WAITLISTED / 202) ===" -ForegroundColor Cyan
$bBook = Invoke-Api "POST" "$base/bookings" @{ slotId = [int]$slotId; joinWaitlistIfFull = $true } $bob
Write-Host $bBook

Write-Host "`n=== 7. Alice cancels (frees seat -> auto-promotes Bob) ===" -ForegroundColor Cyan
$cancel = Invoke-Api "DELETE" "$base/bookings/$aBookId" $null $alice
Write-Host $cancel

Write-Host "`n=== 8. Bob's bookings (should now show a CONFIRMED booking) ===" -ForegroundColor Cyan
Start-Sleep -Milliseconds 500
$bobBookings = Invoke-Api "GET" "$base/bookings" $null $bob
Write-Host $bobBookings

Write-Host "`nDone." -ForegroundColor Green
