# Build TAH Debug APK and verify output
param(
    [switch]$Install,
    [switch]$Clean
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TAH Android Build Runner" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$gradleArgs = @()
if ($Clean) {
    $gradleArgs += "clean"
}
$gradleArgs += ":app:assembleDebug"

$startTime = Get-Date
& ".\gradlew.bat" @gradleArgs
$exitCode = $LASTEXITCODE

$elapsed = (Get-Date) - $startTime
Write-Host ""
if ($exitCode -eq 0) {
    $apk = Get-ChildItem "app\build\outputs\apk\debug\*.apk" | Select-Object -First 1
    if ($apk) {
        $sizeMb = ($apk.Length / 1MB).ToString("F2")
        Write-Host " SUCCESS: Built $($apk.Name) ($sizeMb MB) in $($elapsed.TotalSeconds.ToString("F1"))s!" -ForegroundColor Green
        Write-Host " Location: $($apk.FullName)" -ForegroundColor DarkCyan

        if ($Install) {
            Write-Host " Installing to connected device via adb..." -ForegroundColor Yellow
            & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "$($apk.FullName)"
        }
    } else {
        Write-Host " BUILD SUCCESSFUL but APK not found in outputs" -ForegroundColor Yellow
    }
} else {
    Write-Host " BUILD FAILED with exit code $exitCode" -ForegroundColor Red
    exit $exitCode
}
