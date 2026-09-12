# Run all Android unit tests with summary
param(
    [switch]$Coverage,
    [switch]$Info
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TAH Android Test Runner" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$gradleArgs = @(":app:testDebugUnitTest")
if ($Info) {
    $gradleArgs += "--info"
}

$startTime = Get-Date
& ".\gradlew.bat" @gradleArgs
$exitCode = $LASTEXITCODE

$elapsed = (Get-Date) - $startTime
Write-Host ""
if ($exitCode -eq 0) {
    Write-Host " SUCCESS: All unit tests passed in $($elapsed.TotalSeconds.ToString("F1"))s!" -ForegroundColor Green
    $reportPath = "app\build\reports\tests\testDebugUnitTest\index.html"
    if (Test-Path $reportPath) {
        Write-Host " Report: file://$((Get-Item $reportPath).FullName)" -ForegroundColor DarkGray
    }
} else {
    Write-Host " FAILURE: Unit tests failed with exit code $exitCode" -ForegroundColor Red
    $reportPath = "app\build\reports\tests\testDebugUnitTest\index.html"
    if (Test-Path $reportPath) {
        Write-Host " Report: file://$((Get-Item $reportPath).FullName)" -ForegroundColor Yellow
    }
    exit $exitCode
}
