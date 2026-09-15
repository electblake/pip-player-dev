#requires -Version 7.4
param([Parameter(Mandatory)][string]$Serial)
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true
Set-Location (Split-Path -Parent $PSScriptRoot)

& "$env:ANDROID_HOME/platform-tools/adb.exe" -s $Serial install -r 'dist/pip-player-dev.apk'
& "$env:ANDROID_HOME/platform-tools/adb.exe" -s $Serial shell am start -n 'link.pip.player.dev/link.pip.player.MainActivity'
