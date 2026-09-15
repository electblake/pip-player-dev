#requires -Version 7.4
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true
Set-Location (Split-Path -Parent $PSScriptRoot)

./gradlew.bat :app:assembleDebug :app:lintDebug --console=plain
New-Item -ItemType Directory -Path dist -Force | Out-Null
Copy-Item -LiteralPath 'app/build/outputs/apk/debug/app-debug.apk' -Destination 'dist/pip-player-dev.apk' -Force
$apkHash = (Get-FileHash -LiteralPath 'dist/pip-player-dev.apk' -Algorithm SHA256).Hash.ToLowerInvariant()
Set-Content -LiteralPath 'dist/pip-player-dev.apk.sha256' -Value "$apkHash  pip-player-dev.apk" -Encoding ascii
Get-Item -LiteralPath 'dist/pip-player-dev.apk','dist/pip-player-dev.apk.sha256'
