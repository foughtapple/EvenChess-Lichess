[CmdletBinding()]
param(
    [string]$ShortcutName = "EvenChess Test Ground",
    [string]$ScriptPath = "\\wsl$\Ubuntu\home\jayde\dev\lila-docker\repos\lila\scripts\evenchess-testground.ps1",
    [string]$LauncherPath = "",
    [switch]$SkipDesktopShortcut
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $ScriptPath)) {
    throw "Launcher script not found: $ScriptPath"
}

$resolvedLauncherPath = if ($LauncherPath) {
    $LauncherPath
}
else {
    Join-Path (Split-Path -Parent $ScriptPath) "evenchess-testground-launcher.vbs"
}

if (-not (Test-Path -LiteralPath $resolvedLauncherPath)) {
    throw "Hidden launcher wrapper not found: $resolvedLauncherPath"
}

$wscriptExe = Join-Path $env:SystemRoot "System32\wscript.exe"
$arguments = "`"$resolvedLauncherPath`""
$startMenuDir = Join-Path $env:APPDATA "Microsoft\Windows\Start Menu\Programs"
$startMenuShortcut = Join-Path $startMenuDir "$ShortcutName.lnk"
$desktopShortcut = Join-Path ([Environment]::GetFolderPath("Desktop")) "$ShortcutName.lnk"
$iconLocation = "$env:SystemRoot\System32\shell32.dll,167"

function New-Shortcut {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $shell = New-Object -ComObject WScript.Shell
    $shortcut = $shell.CreateShortcut($Path)
    $shortcut.TargetPath = $wscriptExe
    $shortcut.Arguments = $arguments
    $shortcut.WorkingDirectory = $env:USERPROFILE
    $shortcut.IconLocation = $iconLocation
    $shortcut.WindowStyle = 1
    $shortcut.Description = "Start, stop, open, and check the EvenChess-Lichess local test ground."
    $shortcut.Save()
}

New-Item -ItemType Directory -Force -Path $startMenuDir | Out-Null
New-Shortcut -Path $startMenuShortcut

if (-not $SkipDesktopShortcut) {
    New-Shortcut -Path $desktopShortcut
}

Write-Host "Created Start Menu shortcut:"
Write-Host "  $startMenuShortcut"

if (-not $SkipDesktopShortcut) {
    Write-Host "Created Desktop shortcut:"
    Write-Host "  $desktopShortcut"
}

Write-Host ""
Write-Host "To pin it to the taskbar:"
Write-Host "  1. Open Start."
Write-Host "  2. Search for '$ShortcutName'."
Write-Host "  3. Right-click it and choose 'Pin to taskbar'."
