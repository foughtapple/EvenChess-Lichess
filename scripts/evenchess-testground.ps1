[CmdletBinding()]
param(
    [string]$WslDistro = "Ubuntu",
    [string]$WslRepo = "/home/jayde/dev/lila-docker/repos/lila",
    [string]$MainUrl = "http://localhost:8080/",
    [int]$DockerWaitSeconds = 180,
    [string]$Action = "",
    [switch]$Menu
)

$ErrorActionPreference = "Stop"
$script:LastWslScriptExitCode = $null

$DefaultEceRoot = "/home/jayde/dev/lila-docker/repos/ece"
$DefaultEceUrl = "http://127.0.0.1:8787"
$DefaultClmUrl = "http://127.0.0.1:8790"
$EceRoot = if ($env:EVENCHESS_ENGINE_ROOT) { $env:EVENCHESS_ENGINE_ROOT } else { $DefaultEceRoot }
$EceUrl = if ($env:EVENCHESS_ENGINE_URL) { $env:EVENCHESS_ENGINE_URL.TrimEnd("/") } else { $DefaultEceUrl }
$ClmUrl = if ($env:ECE_CLM_URL) { $env:ECE_CLM_URL.TrimEnd("/") } else { $DefaultClmUrl }
$EceBindHost = if ($env:EVENCHESS_ENGINE_BIND_HOST) { $env:EVENCHESS_ENGINE_BIND_HOST } else { "0.0.0.0" }
$EceHealthUrl = "${EceUrl}/health"
$EceReadyUrl = "${EceUrl}/ready"
$EceBoardUrl = "${EceUrl}/v1/ece/board/quick"
$EceBoardDeepUrl = "${EceUrl}/v1/ece/board/deep"
$EceSettingsUrl = "${EceUrl}/ece/settings"
$ClmAppUrl = "${ClmUrl}/clm"
$ClmStatusUrl = "${ClmUrl}/api/clm/status"
$EceStartTimeoutSeconds = 45
$ClmStartTimeoutSeconds = 45
$EceDebugIoLogPath = if ($env:ECE_DEBUG_IO_LOG_PATH) {
    $env:ECE_DEBUG_IO_LOG_PATH
}
else {
    "$($EceRoot.TrimEnd('/'))/logs/ece-debug-io.json"
}
$EceDebugIoLogMaxEntries = if ($env:ECE_DEBUG_IO_LOG_MAX_ENTRIES) { $env:ECE_DEBUG_IO_LOG_MAX_ENTRIES } else { "100" }
$TestEceScriptPath = Join-Path $PSScriptRoot "evenchess-test-ece-server.js"
try {
    $EceUri = [Uri]$EceUrl
    $EceHost = if ($EceUri.Host) { $EceUri.Host } else { "127.0.0.1" }
    $EcePort = if ($EceUri.Port -gt 0) { $EceUri.Port } else { 8787 }
}
catch {
    $EceHost = "127.0.0.1"
    $EcePort = 8787
}
$DefaultLilaEceUrl = "http://host.docker.internal:$EcePort"
$LilaEceUrl = if ($env:EVENCHESS_LILA_ECE_URL) { $env:EVENCHESS_LILA_ECE_URL.TrimEnd("/") } else { $DefaultLilaEceUrl }
$TestEceBindHost = if ($env:EVENCHESS_TEST_ECE_BIND_HOST) { $env:EVENCHESS_TEST_ECE_BIND_HOST } else { "0.0.0.0" }
$localAppData = if ($env:LOCALAPPDATA) { $env:LOCALAPPDATA } else { Join-Path $env:USERPROFILE "AppData\Local" }
$EceStateDir = Join-Path $localAppData "EvenChess\TestGround"
$TestEcePidPath = Join-Path $EceStateDir "test-ece.pid"
$PanelScriptPath = Join-Path $PSScriptRoot "evenchess-testground-panel.js"
$PanelPidPath = Join-Path $EceStateDir "panel.pid"
$PanelHost = "127.0.0.1"
$PanelPort = 8791
$PanelUrl = "http://${PanelHost}:${PanelPort}/"
$PanelClmUrl = "${PanelUrl}clm"
$PanelVersion = "2026-06-03.1"
$UiBuildMetadataPath = Join-Path $EceStateDir "ui-build.json"
$WslRepoUser = if ($WslRepo -match "^/home/([^/]+)/") { $Matches[1] } else { "jayde" }
$WslLilaDockerRoot = if ($WslRepo -match "^(.*)/repos/lila/?$") { $Matches[1] } else { "/home/$WslRepoUser/dev/lila-docker" }
$ClmPort = 8790
try {
    $ClmUri = [Uri]$ClmUrl
    if ($ClmUri.Port -gt 0) {
        $ClmPort = $ClmUri.Port
    }
}
catch {
    $ClmPort = 8790
}
$EceDebugIoLogHostPath = if ($env:ECE_DEBUG_IO_LOG_HOST_PATH) {
    $env:ECE_DEBUG_IO_LOG_HOST_PATH
}
elseif ($EceDebugIoLogPath -match "^/") {
    "\\wsl$\$WslDistro$($EceDebugIoLogPath -replace '/', '\')"
}
else {
    $EceDebugIoLogPath
}
$ClmPidHostPath = if ($env:ECE_CLM_PID_HOST_PATH) {
    $env:ECE_CLM_PID_HOST_PATH
}
elseif ($EceRoot -match "^/") {
    "\\wsl$\$WslDistro$($EceRoot.TrimEnd('/') -replace '/', '\')\ECE_CLM\.ece-clm-local.pid"
}
else {
    Join-Path (Join-Path $EceRoot "ECE_CLM") ".ece-clm-local.pid"
}

function Write-Title {
    Clear-Host
    Write-Host "EvenChess Test Ground" -ForegroundColor Cyan
    Write-Host "WSL distro: $WslDistro"
    Write-Host "Repo:       $WslRepo"
    Write-Host "Site:       $MainUrl"
    Write-Host "ECE:        $EceUrl"
    Write-Host "ECE CLM:    $PanelClmUrl"
    Write-Host "Lila->ECE:  $LilaEceUrl"
    Write-Host ""
    Write-Host "s  start WSL/Docker + local stack"
    Write-Host "u  build UI assets/manifest"
    Write-Host "p  open browser control panel"
    Write-Host "t  status check"
    Write-Host "o  open site"
    Write-Host "c  stop containers only"
    Write-Host "e  start/check real ECE"
    Write-Host "m  start/check test ECE payload server"
    Write-Host "l  launch/open ECE CLM"
    Write-Host "r  stop ECE CLM"
    Write-Host "g  open ECE settings page"
    Write-Host "h  health-check ECE"
    Write-Host "b  call sample ECE board endpoint"
    Write-Host "v  stop real ECE"
    Write-Host "n  stop test ECE"
    Write-Host "x  stop containers + shut down WSL memory"
    Write-Host "q  quit this window"
    Write-Host ""
    Write-Host "Use x when you are done testing and want Task Manager's VmmemWSL memory released." -ForegroundColor Yellow
    Write-Host ""
}

function Start-DockerDesktop {
    $dockerDesktop = Join-Path $env:ProgramFiles "Docker\Docker\Docker Desktop.exe"
    if (-not (Test-Path -LiteralPath $dockerDesktop)) {
        $dockerDesktop = Join-Path ${env:ProgramFiles(x86)} "Docker\Docker\Docker Desktop.exe"
    }

    if (-not (Test-Path -LiteralPath $dockerDesktop)) {
        Write-Host "Docker Desktop executable was not found in Program Files." -ForegroundColor Yellow
        Write-Host "Open Docker Desktop manually, then run s again."
        return
    }

    $process = Get-Process -Name "Docker Desktop" -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        Write-Host "Starting Docker Desktop..." -ForegroundColor Cyan
        Start-Process -FilePath $dockerDesktop -WindowStyle Hidden
    }
    else {
        Write-Host "Docker Desktop is already running." -ForegroundColor DarkGray
    }
}

function Wait-DockerInWsl {
    $deadline = (Get-Date).AddSeconds($DockerWaitSeconds)

    Write-Host "Waiting for Docker to be available inside WSL $WslDistro..." -ForegroundColor Cyan
    while ((Get-Date) -lt $deadline) {
        & wsl.exe -d $WslDistro -- sh -lc "command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1"
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Docker is ready inside WSL." -ForegroundColor Green
            return $true
        }

        Write-Host "." -NoNewline
        Start-Sleep -Seconds 5
    }

    Write-Host ""
    Write-Host "Docker was not available inside WSL after $DockerWaitSeconds seconds." -ForegroundColor Yellow
    Write-Host "Open Docker Desktop and confirm Settings > Resources > WSL integration has Ubuntu enabled."
    return $false
}

function Test-DockerInWsl {
    & wsl.exe -d $WslDistro -- sh -lc "command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1"
    return $LASTEXITCODE -eq 0
}

function Start-DockerWsl {
    Start-DockerDesktop
    return Wait-DockerInWsl
}

function Stop-DockerWsl {
    Write-Host ""
    Write-Host "Stopping Docker Desktop and docker-desktop WSL engine..." -ForegroundColor Cyan

    $dockerCli = Join-Path $env:ProgramFiles "Docker\Docker\DockerCli.exe"
    if (Test-Path -LiteralPath $dockerCli -PathType Leaf) {
        try {
            $shutdown = Start-Process -FilePath $dockerCli -ArgumentList @("-Shutdown") -WindowStyle Hidden -PassThru
            if (-not $shutdown.WaitForExit(15000)) {
                Stop-Process -Id $shutdown.Id -Force -ErrorAction SilentlyContinue
                Write-Host "DockerCli shutdown did not finish within 15 seconds; continuing with process stop." -ForegroundColor Yellow
            }
        }
        catch {
            Write-Host "DockerCli shutdown failed: $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }

    Stop-Process -Name "Docker Desktop", "com.docker.backend", "com.docker.build", "com.docker.proxy", "docker-agent", "docker-sandbox", "docker", "vpnkit" -Force -ErrorAction SilentlyContinue
    & wsl.exe --terminate docker-desktop 2>$null

    Write-Host "Docker stop requested." -ForegroundColor Green
    return $true
}

function Invoke-WslScript {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ScriptName,

        [switch]$NoPause
    )

    Write-Host ""
    Write-Host "Running $ScriptName in WSL..." -ForegroundColor Cyan
    & wsl.exe -d $WslDistro --cd $WslRepo -- bash "./scripts/$ScriptName"
    $code = $LASTEXITCODE
    Write-Host ""
    if ($code -eq 0) {
        Write-Host "$ScriptName finished successfully." -ForegroundColor Green
    }
    else {
        Write-Host "$ScriptName exited with code $code." -ForegroundColor Yellow
    }
    $script:LastWslScriptExitCode = $code
    if (-not $NoPause) {
        Write-Host "Press Enter to return to the menu."
        [void][Console]::ReadLine()
    }
}

function Quote-Bash {
    param(
        [Parameter(Mandatory = $true)]
        [AllowEmptyString()]
        [string]$Value
    )

    return "'" + ($Value -replace "'", "'\''") + "'"
}

function Test-WslPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,

        [string]$Type = "e"
    )

    $quotedPath = Quote-Bash $Path
    & wsl.exe -d $WslDistro -- bash -lc "test -$Type $quotedPath"
    return $LASTEXITCODE -eq 0
}

function Invoke-WslEceScript {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ScriptName
    )

    $quotedRoot = Quote-Bash $EceRoot
    $quotedDebugPath = Quote-Bash $EceDebugIoLogPath
    $quotedMaxEntries = Quote-Bash $EceDebugIoLogMaxEntries
    $quotedPort = Quote-Bash ([string]$EcePort)
    $quotedBindHost = Quote-Bash $EceBindHost
    $command = "cd $quotedRoot && ECE_HOST=$quotedBindHost ECE_PORT=$quotedPort ECE_DEBUG_IO_LOG=1 ECE_DEBUG_IO_LOG_PATH=$quotedDebugPath ECE_DEBUG_IO_LOG_MAX_ENTRIES=$quotedMaxEntries bash scripts/$ScriptName"

    $output = & wsl.exe -d $WslDistro -- bash -lc $command 2>&1
    $code = $LASTEXITCODE
    foreach ($line in $output) {
        Write-Host $line
    }
    return $code
}

function Invoke-WslClmCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Command
    )

    $quotedRoot = Quote-Bash $EceRoot
    $quotedPort = Quote-Bash ([string]$ClmPort)
    $wrappedCommand = "cd $quotedRoot && ECE_CLM_HOST=127.0.0.1 ECE_CLM_PORT=$quotedPort $Command"
    $output = & wsl.exe -d $WslDistro -- bash -lc $wrappedCommand 2>&1
    $code = $LASTEXITCODE
    foreach ($line in $output) {
        Write-Host $line
    }
    return $code
}

function Invoke-WslClmScript {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ScriptName
    )

    return Invoke-WslClmCommand -Command "bash ECE_CLM/scripts/$ScriptName"
}

function Get-WslFileContent {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $hostPath = if ($Path -match "^/") { "\\wsl$\$WslDistro$($Path -replace '/', '\')" } else { $Path }
    if (Test-Path -LiteralPath $hostPath -PathType Leaf) {
        return Get-Content -LiteralPath $hostPath -Raw
    }

    return $null
}

function Repair-WslGeneratedAssetOwnership {
    Write-Host ""
    Write-Host "Repairing generated UI asset ownership in WSL..." -ForegroundColor Cyan
    & wsl.exe -d $WslDistro -u root --cd $WslRepo -- bash -lc "chown -R '${WslRepoUser}:${WslRepoUser}' public ui/.build ui/*/dist ui/*/tsconfig.tsbuildinfo ui/lib/css/theme/gen 2>/dev/null || true"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Generated asset ownership repair exited with code $LASTEXITCODE." -ForegroundColor Yellow
        return $false
    }

    return $true
}

function Get-RoundAssetHash {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Value
    )

    if ($null -eq $Value) {
        return ""
    }
    if ($Value -is [string]) {
        return $Value
    }

    $hashProperty = $Value.PSObject.Properties["hash"]
    if ($hashProperty) {
        return [string]$hashProperty.Value
    }

    return ""
}

function Get-EvenChessUiAssetStatus {
    $manifestPath = "\\wsl$\$WslDistro$($WslRepo -replace '/', '\')\public\compiled\manifest.json"
    $result = [ordered]@{
        ok = $false
        manifestPath = $manifestPath
        updatedAt = ""
        jsHash = ""
        cssHash = ""
        hasLevelShell = $false
        hasBoardOverlayRenderer = $false
        hasFeatureToggleCss = $false
        hasBoardOverlayCss = $false
        error = ""
    }

    try {
        if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
            $result.error = "Missing public/compiled/manifest.json."
            return [pscustomobject]$result
        }

        $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
        $result.updatedAt = (Get-Item -LiteralPath $manifestPath).LastWriteTime.ToString("yyyy-MM-dd HH:mm:ss zzz")
        $result.jsHash = Get-RoundAssetHash -Value $manifest.js.round
        $result.cssHash = Get-RoundAssetHash -Value $manifest.css.round

        $repoHostPath = "\\wsl$\$WslDistro$($WslRepo -replace '/', '\')"
        $jsPath = if ($result.jsHash) { Join-Path $repoHostPath "public\compiled\round.$($result.jsHash).js" } else { "" }
        $cssPath = if ($result.cssHash) { Join-Path $repoHostPath "public\css\round.$($result.cssHash).css" } else { "" }
        $jsText = if ($jsPath -and (Test-Path -LiteralPath $jsPath -PathType Leaf)) { Get-Content -LiteralPath $jsPath -Raw } else { "" }
        $cssText = if ($cssPath -and (Test-Path -LiteralPath $cssPath -PathType Leaf)) { Get-Content -LiteralPath $cssPath -Raw } else { "" }

        $result.hasLevelShell = $jsText.Contains("EvenChess Levels")
        $result.hasBoardOverlayRenderer = $jsText.Contains("evenchess-board-overlay")
        $result.hasFeatureToggleCss = $cssText.Contains("evenchess-live__feature-toggle")
        $result.hasBoardOverlayCss = $cssText.Contains("evenchess-board-overlay")
        $result.ok = [bool]($result.jsHash -and $result.cssHash -and $result.hasLevelShell -and $result.hasBoardOverlayRenderer -and $result.hasFeatureToggleCss -and $result.hasBoardOverlayCss)
    }
    catch {
        $result.error = $_.Exception.Message
    }

    return [pscustomobject]$result
}

function Get-EvenChessGitVersion {
    $version = (& wsl.exe -d $WslDistro --cd $WslRepo -- bash -lc "git rev-parse --short HEAD 2>/dev/null || true").Trim()
    if ($LASTEXITCODE -ne 0 -or -not $version) {
        return "unknown"
    }

    $dirty = (& wsl.exe -d $WslDistro --cd $WslRepo -- bash -lc "git diff --quiet -- . 2>/dev/null; if [ `$? -eq 0 ]; then echo clean; else echo dirty; fi").Trim()
    if ($dirty -eq "dirty") {
        return "$version-dirty"
    }

    return $version
}

function Write-EvenChessUiBuildMetadata {
    param(
        [Parameter(Mandatory = $true)]
        [object]$AssetStatus
    )

    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $UiBuildMetadataPath) | Out-Null
    $metadata = [ordered]@{
        builtAt = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss zzz")
        gitVersion = Get-EvenChessGitVersion
        manifestUpdatedAt = $AssetStatus.updatedAt
        roundJsHash = $AssetStatus.jsHash
        roundCssHash = $AssetStatus.cssHash
        manifestPath = $AssetStatus.manifestPath
    }
    $metadata | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $UiBuildMetadataPath -Encoding UTF8
}

function Test-EvenChessUiAssetsReady {
    $status = Get-EvenChessUiAssetStatus
    if ($status.ok) {
        Write-Host ""
        Write-Host "UI assets are built. Round JS $($status.jsHash), CSS $($status.cssHash), manifest $($status.updatedAt)." -ForegroundColor Green
        return $true
    }

    Write-Host ""
    Write-Host "EvenChess UI assets are missing or stale." -ForegroundColor Yellow
    if ($status.error) {
        Write-Host "Reason: $($status.error)" -ForegroundColor Yellow
    }
    else {
        Write-Host "Round JS hash:             $($status.jsHash)"
        Write-Host "Round CSS hash:            $($status.cssHash)"
        Write-Host "Has level shell:           $($status.hasLevelShell)"
        Write-Host "Has board overlay renderer:$($status.hasBoardOverlayRenderer)"
        Write-Host "Has feature toggle CSS:    $($status.hasFeatureToggleCss)"
        Write-Host "Has board overlay CSS:     $($status.hasBoardOverlayCss)"
    }
    Write-Host "Use the Build UI Assets button first, then Launch EvenChess." -ForegroundColor Yellow
    return $false
}

function Build-EvenChessUiAssets {
    if ($env:EVENCHESS_SKIP_UI_BUILD -eq "1") {
        Write-Host ""
        Write-Host "Skipping UI build because EVENCHESS_SKIP_UI_BUILD=1." -ForegroundColor Yellow
        return $true
    }

    if (-not (Repair-WslGeneratedAssetOwnership)) {
        return $false
    }

    Write-Host ""
    Write-Host "Building EvenChess UI assets and manifest..." -ForegroundColor Cyan
    & wsl.exe -d $WslDistro --cd $WslRepo -- bash -lc "CI=true ./ui/build -n -k"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "UI asset build exited with code $LASTEXITCODE." -ForegroundColor Yellow
        return $false
    }

    $assetStatus = Get-EvenChessUiAssetStatus
    if ($assetStatus.ok) {
        Write-EvenChessUiBuildMetadata -AssetStatus $assetStatus
        Write-Host "UI assets and manifest are current. Round JS $($assetStatus.jsHash), CSS $($assetStatus.cssHash)." -ForegroundColor Green
    }
    else {
        Write-Host "UI build finished, but the EvenChess round asset check still failed." -ForegroundColor Yellow
        if ($assetStatus.error) {
            Write-Host "Reason: $($assetStatus.error)" -ForegroundColor Yellow
        }
        return $false
    }

    return $true
}

function Open-MainSite {
    $configuredUrl = $MainUrl
    $envText = Get-WslFileContent -Path "$($WslLilaDockerRoot.TrimEnd('/'))/.env"
    if ($envText -and $envText -match "(?m)^LILA_URL=(.+)$") {
        $configuredUrl = $Matches[1].Trim().TrimEnd("/") + "/"
    }

    Write-Host ""
    Write-Host "Opening $configuredUrl" -ForegroundColor Cyan
    Start-Process $configuredUrl
}

function Open-EceSettingsPage {
    Write-Host ""
    Write-Host "Opening ECE settings page at $EceSettingsUrl" -ForegroundColor Cyan
    Start-Process $EceSettingsUrl
}

function Test-PanelHealth {
    try {
        $status = Invoke-RestMethod -Method Get -Uri "${PanelUrl}api/ping" -TimeoutSec 3 -ErrorAction Stop
        return $status.panelVersion -eq $PanelVersion
    }
    catch {
        return $false
    }
}

function Test-PanelClmProxyRoute {
    try {
        Invoke-WebRequest -Method Get -Uri "${PanelUrl}api/clm/status" -TimeoutSec 3 -UseBasicParsing -ErrorAction Stop | Out-Null
        return $true
    }
    catch {
        $response = $_.Exception.Response
        if ($null -eq $response) {
            return $false
        }

        $statusCode = [int]$response.StatusCode
        return $statusCode -ne 404
    }
}

function Test-PanelReachable {
    try {
        $response = Invoke-WebRequest -Method Get -Uri $PanelUrl -TimeoutSec 3 -UseBasicParsing -ErrorAction Stop
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 500
    }
    catch {
        return $false
    }
}

function Stop-TestGroundPanelProcesses {
    param(
        [int]$OnlyPort = 0
    )

    try {
        $processes = Get-CimInstance Win32_Process -Filter "Name = 'node.exe'" -ErrorAction Stop
        foreach ($processInfo in $processes) {
            $commandLine = $processInfo.CommandLine
            if (-not $commandLine -or $commandLine -notlike "*evenchess-testground-panel.js*") {
                continue
            }

            if ($OnlyPort -gt 0 -and $commandLine -notlike "*--port $OnlyPort*") {
                continue
            }

            Stop-Process -Id $processInfo.ProcessId -Force -ErrorAction SilentlyContinue
            if ($OnlyPort -gt 0) {
                Write-Host "Stopped stale Test Ground panel process PID $($processInfo.ProcessId) on port $OnlyPort." -ForegroundColor DarkGray
            }
            else {
                Write-Host "Stopped stale Test Ground panel process PID $($processInfo.ProcessId)." -ForegroundColor DarkGray
            }
        }
    }
    catch {
        Write-Host "Could not scan for stale Test Ground panel processes: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

function Stop-ReachablePanel {
    try {
        Invoke-RestMethod -Method Post -Uri "${PanelUrl}api/shutdown" -TimeoutSec 3 -ErrorAction Stop | Out-Null
        Start-Sleep -Seconds 1
        return $true
    }
    catch {
        return $false
    }
}

function Test-PanelProcessIdentity {
    param(
        [Parameter(Mandatory = $true)]
        [System.Diagnostics.Process]$Process
    )

    $nameOk = $Process.ProcessName -ieq "node" -or $Process.ProcessName -ieq "node.exe"
    if (-not $nameOk) {
        return $false
    }

    try {
        $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId=$($Process.Id)" -ErrorAction Stop
        $commandLine = $processInfo.CommandLine
        if ($commandLine) {
            return ($commandLine -like "*evenchess-testground-panel.js*")
        }
    }
    catch {
        return $false
    }

    return $false
}

function Start-TestGroundPanel {
    Write-Host ""
    Write-Host "Starting/opening EvenChess Test Ground browser panel..." -ForegroundColor Cyan

    Stop-TestGroundPanelProcesses -OnlyPort 8790

    if ((Test-PanelHealth) -and (Test-PanelClmProxyRoute)) {
        Write-Host "Panel is already reachable at $PanelUrl" -ForegroundColor Green
        Start-Process $PanelUrl
        return $true
    }

    if (Test-PanelReachable) {
        Write-Host "An older Test Ground panel is already using $PanelUrl; restarting it." -ForegroundColor Yellow
        Stop-ReachablePanel | Out-Null
        if (Test-PanelReachable) {
            Stop-TestGroundPanelProcesses -OnlyPort $PanelPort
            Start-Sleep -Seconds 1
        }
    }

    if (-not (Test-Path -LiteralPath $PanelScriptPath -PathType Leaf)) {
        Write-Host "Panel script not found: $PanelScriptPath" -ForegroundColor Yellow
        return $false
    }

    $nodeCommand = Get-Command "node" -ErrorAction SilentlyContinue
    if ($null -eq $nodeCommand) {
        Write-Host "Node.js was not found on PATH. Install Node.js or use the text menu." -ForegroundColor Yellow
        return $false
    }

    New-Item -ItemType Directory -Force -Path $EceStateDir | Out-Null
    $env:ECE_HOST = $EceHost
    $env:ECE_PORT = "$EcePort"
    $env:EVENCHESS_ENGINE_ROOT = $EceRoot
    $env:EVENCHESS_ENGINE_ROOT_HOST_PATH = if ($EceRoot -match "^/") { "\\wsl$\$WslDistro$($EceRoot -replace '/', '\')" } else { $EceRoot }
    $env:EVENCHESS_ENGINE_URL = $EceUrl
    $env:EVENCHESS_LILA_ECE_URL = $LilaEceUrl
    $env:ECE_CLM_URL = $ClmUrl
    $env:ECE_CLM_PID_HOST_PATH = $ClmPidHostPath
    $env:ECE_DEBUG_IO_LOG_PATH = $EceDebugIoLogPath
    $env:ECE_DEBUG_IO_LOG_HOST_PATH = $EceDebugIoLogHostPath
    $env:ECE_DEBUG_IO_LOG_MAX_ENTRIES = $EceDebugIoLogMaxEntries

    try {
        $process = Start-Process `
            -FilePath $nodeCommand.Source `
            -ArgumentList @($PanelScriptPath, "--host", $PanelHost, "--port", "$PanelPort") `
            -WorkingDirectory $env:USERPROFILE `
            -WindowStyle Hidden `
            -PassThru
        Set-Content -LiteralPath $PanelPidPath -Value $process.Id -Encoding ASCII
        Write-Host "Started panel process PID $($process.Id)." -ForegroundColor Green
    }
    catch {
        Write-Host "Failed to start panel: $($_.Exception.Message)" -ForegroundColor Yellow
        return $false
    }

    $deadline = (Get-Date).AddSeconds(10)
    while ((Get-Date) -lt $deadline) {
        if (Test-PanelHealth) {
            Start-Process $PanelUrl
            return $true
        }
        Start-Sleep -Milliseconds 300
    }

    Write-Host "Panel did not become reachable at $PanelUrl." -ForegroundColor Yellow
    return $false
}

function Stop-TestGroundPanel {
    Write-Host ""
    Write-Host "Stopping EvenChess Test Ground browser panel..." -ForegroundColor Cyan

    if (Test-PanelReachable) {
        if (Stop-ReachablePanel) {
            Remove-Item -LiteralPath $PanelPidPath -Force -ErrorAction SilentlyContinue
            Write-Host "Stopped reachable panel at $PanelUrl." -ForegroundColor Green
            return
        }
    }

    $storedPid = Get-EcePidFromState -Path $PanelPidPath

    if ($null -eq $storedPid) {
        Remove-Item -LiteralPath $PanelPidPath -Force -ErrorAction SilentlyContinue
        Write-Host "No Test Ground panel PID found." -ForegroundColor DarkGray
        return
    }

    $process = Get-Process -Id $storedPid -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        Write-Host "Stored panel PID $storedPid is not running." -ForegroundColor DarkGray
        Remove-Item -LiteralPath $PanelPidPath -Force -ErrorAction SilentlyContinue
        return
    }

    if (-not (Test-PanelProcessIdentity -Process $process)) {
        Write-Host "Refusing to stop PID $storedPid because it does not look like the Test Ground panel process." -ForegroundColor Yellow
        return
    }

    try {
        Stop-Process -Id $storedPid -ErrorAction Stop
        Start-Sleep -Seconds 1
        Write-Host "Stopped panel process PID $storedPid." -ForegroundColor Green
    }
    catch {
        Write-Host "Failed to stop panel PID ${storedPid}: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    finally {
        Remove-Item -LiteralPath $PanelPidPath -Force -ErrorAction SilentlyContinue
    }
}

function Get-JsonProperty {
    param(
        [Parameter(Mandatory = $true)]
        [AllowNull()]
        [object]$InputObject,

        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    if ($null -eq $InputObject) {
        return $null
    }

    $property = $InputObject.PSObject.Properties[$Name]
    if ($null -eq $property) {
        return $null
    }

    return $property.Value
}

function Get-JsonPath {
    param(
        [Parameter(Mandatory = $true)]
        [AllowNull()]
        [object]$InputObject,

        [Parameter(Mandatory = $true)]
        [string[]]$Path
    )

    $current = $InputObject
    foreach ($part in $Path) {
        $current = Get-JsonProperty -InputObject $current -Name $part
        if ($null -eq $current) {
            return $null
        }
    }

    return $current
}

function Select-FirstValue {
    param(
        [Parameter(Mandatory = $true)]
        [AllowNull()]
        [object[]]$Values
    )

    foreach ($value in $Values) {
        if ($null -ne $value -and "$value" -ne "") {
            return $value
        }
    }

    return "unknown"
}

function Test-EceHealth {
    try {
        return Invoke-RestMethod -Method Get -Uri $EceHealthUrl -TimeoutSec 5 -ErrorAction Stop
    }
    catch {
        return $null
    }
}

function Show-EceHealth {
    $health = Test-EceHealth
    Write-Host ""
    Write-Host "ECE health" -ForegroundColor Cyan

    if ($null -eq $health) {
        Write-Host "  reachable: no" -ForegroundColor Yellow
        Write-Host "  url:       $EceHealthUrl"
        return $false
    }

    $service = Select-FirstValue @(
        (Get-JsonProperty -InputObject $health -Name "service"),
        (Get-JsonPath -InputObject $health -Path @("data", "service"))
    )
    $mode = Select-FirstValue @(
        (Get-JsonProperty -InputObject $health -Name "mode"),
        (Get-JsonPath -InputObject $health -Path @("data", "mode"))
    )
    $openaiConfigured = Select-FirstValue @(
        (Get-JsonProperty -InputObject $health -Name "openai_configured"),
        (Get-JsonPath -InputObject $health -Path @("config", "openai_configured")),
        (Get-JsonPath -InputObject $health -Path @("data", "openai_configured"))
    )
    $stockfishConfigured = Select-FirstValue @(
        (Get-JsonProperty -InputObject $health -Name "stockfish_configured"),
        (Get-JsonPath -InputObject $health -Path @("config", "stockfish_configured")),
        (Get-JsonPath -InputObject $health -Path @("data", "stockfish_configured"))
    )

    Write-Host "  reachable:             yes" -ForegroundColor Green
    Write-Host "  service:               $service"
    Write-Host "  mode:                  $mode"
    Write-Host "  openai_configured:     $openaiConfigured"
    Write-Host "  stockfish_configured:  $stockfishConfigured"
    Write-Host "  debug_io_log_path:     $EceDebugIoLogPath"
    return $true
}

function Test-ClmStatus {
    try {
        $statusUrl = "http://127.0.0.1:${ClmPort}/api/clm/status"
        $output = & wsl.exe -d $WslDistro -- bash -lc "curl -fsS --max-time 5 '$statusUrl'" 2>$null
        if ($LASTEXITCODE -ne 0 -or -not $output) {
            return $null
        }

        return ($output -join "`n") | ConvertFrom-Json
    }
    catch {
        return $null
    }
}

function Show-ClmStatus {
    $status = Test-ClmStatus
    Write-Host ""
    Write-Host "ECE CLM status" -ForegroundColor Cyan

    if ($null -eq $status) {
        Write-Host "  reachable: no" -ForegroundColor Yellow
        Write-Host "  url:       $ClmStatusUrl"
        Write-Host "  app:       $ClmAppUrl"
        return $false
    }

    $service = Select-FirstValue @(
        (Get-JsonProperty -InputObject $status -Name "service"),
        (Get-JsonPath -InputObject $status -Path @("data", "service"))
    )
    $localOnly = Select-FirstValue @(
        (Get-JsonProperty -InputObject $status -Name "local_only"),
        (Get-JsonPath -InputObject $status -Path @("data", "local_only"))
    )
    $database = Get-JsonProperty -InputObject $status -Name "database"
    $counts = Get-JsonProperty -InputObject $database -Name "counts"
    $positions = Select-FirstValue @(
        (Get-JsonProperty -InputObject $counts -Name "positions")
    )
    $labels = Select-FirstValue @(
        (Get-JsonProperty -InputObject $counts -Name "validated_labels")
    )
    $ollama = Get-JsonProperty -InputObject $status -Name "ollama"
    $ollamaStatus = Select-FirstValue @(
        (Get-JsonProperty -InputObject $ollama -Name "status"),
        (Get-JsonProperty -InputObject $ollama -Name "error")
    )

    Write-Host "  reachable:        yes" -ForegroundColor Green
    Write-Host "  service:          $service"
    Write-Host "  local_only:       $localOnly"
    Write-Host "  app:              $ClmAppUrl"
    Write-Host "  positions:        $positions"
    Write-Host "  validated labels: $labels"
    Write-Host "  ollama:           $ollamaStatus"
    return $true
}

function Grant-LocalAdminSettingsAccess {
    Write-Host ""
    Write-Host "Granting local Admin account access to EvenChess bot/settings admin pages..." -ForegroundColor Cyan

    if (-not (Test-DockerInWsl)) {
        Write-Host "Docker is not ready inside WSL. Start WSL/Docker first, then run this action again." -ForegroundColor Yellow
        return $false
    }

    $mongoScript = @'
const userId = "admin";
const role = "ROLE_SETTINGS";
const result = db.user4.updateOne({ _id: userId }, { $addToSet: { roles: role } });
const user = db.user4.findOne({ _id: userId }, { _id: 1, username: 1, roles: 1 });
printjson({ result, user });
'@

    $mongoScript | & wsl.exe -d $WslDistro -- bash -lc "cd /home/jayde/dev/lila-docker && docker compose exec -T mongodb mongosh lichess --quiet"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Could not update local Admin permissions. Mongo command exited with code $LASTEXITCODE." -ForegroundColor Yellow
        return $false
    }

    Write-Host "Local Admin now has ROLE_SETTINGS. Refresh or sign out/in if the current session still shows access denied." -ForegroundColor Green
    return $true
}

function Open-EceClmPage {
    if (-not (Test-PanelHealth)) {
        Start-TestGroundPanel | Out-Null
    }

    Start-Process $PanelClmUrl
}

function Start-EceClm {
    Write-Host ""
    Write-Host "Starting/opening ECE Composer Learning Model..." -ForegroundColor Cyan

    if ($null -ne (Test-ClmStatus)) {
        Write-Host "ECE CLM is already reachable at $ClmAppUrl." -ForegroundColor Green
        Show-ClmStatus | Out-Null
        Open-EceClmPage
        return $true
    }

    if (-not (Test-WslPath -Path $EceRoot -Type "d")) {
        Write-Host "ECE root not found: $EceRoot" -ForegroundColor Yellow
        return $false
    }

    if (-not (Test-WslPath -Path "$($EceRoot.TrimEnd('/'))/ECE_CLM/scripts/start-ece-clm-linux.sh" -Type "f")) {
        Write-Host "ECE CLM start script not found: $($EceRoot.TrimEnd('/'))/ECE_CLM/scripts/start-ece-clm-linux.sh" -ForegroundColor Yellow
        return $false
    }

    Write-Host "Starting ECE CLM through bash ECE_CLM/scripts/start-ece-clm-linux.sh." -ForegroundColor Cyan
    $startCode = Invoke-WslClmScript -ScriptName "start-ece-clm-linux.sh"
    if ($startCode -ne 0) {
        Write-Host "ECE CLM start script exited with code $startCode." -ForegroundColor Yellow
        return $false
    }

    $deadline = (Get-Date).AddSeconds($ClmStartTimeoutSeconds)
    Write-Host "Waiting for $ClmStatusUrl"
    while ((Get-Date) -lt $deadline) {
        if ($null -ne (Test-ClmStatus)) {
            Write-Host "ECE CLM reachable." -ForegroundColor Green
            Show-ClmStatus | Out-Null
            Open-EceClmPage
            return $true
        }

        Write-Host "." -NoNewline
        Start-Sleep -Seconds 2
    }

    Write-Host ""
    Write-Host "ECE CLM was not reachable after ${ClmStartTimeoutSeconds}s: $ClmStatusUrl" -ForegroundColor Yellow
    return $false
}

function Stop-EceClm {
    Write-Host ""
    Write-Host "Stopping ECE Composer Learning Model..." -ForegroundColor Cyan
    $stopScript = "$($EceRoot.TrimEnd('/'))/ECE_CLM/scripts/stop-ece-clm-linux.sh"

    if (-not (Test-WslPath -Path $stopScript -Type "f")) {
        Write-Host "ECE CLM stop script not found: $stopScript" -ForegroundColor Yellow
        Write-Host "Stop CLM manually with: cd $EceRoot && bash ECE_CLM/scripts/stop-ece-clm-linux.sh"
        return $false
    }

    $code = Invoke-WslClmScript -ScriptName "stop-ece-clm-linux.sh"
    if ($code -ne 0) {
        Write-Host "ECE CLM stop script exited with code $code." -ForegroundColor Yellow
        return $false
    }

    Start-Sleep -Seconds 1
    if ($null -ne (Test-ClmStatus)) {
        Write-Host "ECE CLM stop script completed, but CLM is still reachable at $ClmStatusUrl." -ForegroundColor Yellow
        return $false
    }

    Write-Host "ECE CLM is no longer reachable." -ForegroundColor Green
    return $true
}

function Test-EceReady {
    try {
        return Invoke-RestMethod -Method Get -Uri $EceReadyUrl -TimeoutSec 5 -ErrorAction Stop
    }
    catch {
        return $null
    }
}

function Set-TestEceDebugEnvironment {
    $env:ECE_DEBUG_IO_LOG = "1"
    $env:ECE_DEBUG_IO_LOG_PATH = $EceDebugIoLogHostPath
    $env:ECE_DEBUG_IO_LOG_MAX_ENTRIES = $EceDebugIoLogMaxEntries
}

function Clear-EceDebugLog {
    $cleared = $false
    $hostPath = $EceDebugIoLogHostPath
    $wslPath = $EceDebugIoLogPath

    if (-not [string]::IsNullOrWhiteSpace($hostPath)) {
        try {
            $hostDir = Split-Path -Parent $hostPath
            if ([string]::IsNullOrWhiteSpace($hostDir)) {
                $hostDir = "."
            }

            New-Item -ItemType Directory -Force -Path $hostDir | Out-Null
            Set-Content -LiteralPath $hostPath -Value "[]`n" -Encoding ASCII
            $cleared = $true
            Write-Host "ECE debug IO log cleared at host path: $hostPath" -ForegroundColor DarkGray
        }
        catch {
            Write-Host "Could not clear host debug log path ${hostPath}: $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }

    if ($wslPath -match "^/" -and $hostPath -ne $wslPath -and $env:WINDIR) {
        try {
            $quotedWslPath = Quote-Bash $wslPath
            $quotedWslDir = Quote-Bash (Split-Path -Parent $wslPath)
            & wsl.exe -d $WslDistro -- bash -lc "mkdir -p $quotedWslDir; printf '[]`n' > $quotedWslPath"
            if ($LASTEXITCODE -eq 0) {
                $cleared = $true
                Write-Host "ECE debug IO log cleared in WSL: $wslPath" -ForegroundColor DarkGray
            }
            else {
                Write-Host "Could not clear WSL debug log path $wslPath (wsl exited with code $LASTEXITCODE)." -ForegroundColor Yellow
            }
        }
        catch {
            Write-Host "Could not clear WSL debug log path ${wslPath}: $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }

    return $cleared
}

function Start-RealEce {
    Write-Host ""
    Write-Host "Starting/checking EvenChess Engine..." -ForegroundColor Cyan

    $existingHealth = Test-EceHealth
    if ($null -ne $existingHealth) {
        if ($null -ne (Get-EcePidFromState -Path $TestEcePidPath)) {
            Write-Host "Test ECE is already running on $EceUrl. Stop Test ECE before starting Real ECE." -ForegroundColor Yellow
            Show-EceHealth | Out-Null
            return $false
        }

        if (Test-EceHealthLooksLikeTestServer -Health $existingHealth) {
            Write-Host "Test ECE is already running on $EceUrl. Stop Test ECE before starting Real ECE." -ForegroundColor Yellow
            Show-EceHealth | Out-Null
            return $false
        }

        Write-Host "ECE endpoint already running. Treating it as Linux Real ECE because it was not started as the Test Ground test server." -ForegroundColor Green
        Show-EceHealth | Out-Null
        return $true
    }

    if (-not (Test-WslPath -Path $EceRoot -Type "d")) {
        Write-Host "ECE root not found: $EceRoot" -ForegroundColor Yellow
        return $false
    }

    if (-not (Test-WslPath -Path "$($EceRoot.TrimEnd('/'))/scripts/start-ece-linux.sh" -Type "f")) {
        Write-Host "Linux ECE start script not found: $($EceRoot.TrimEnd('/'))/scripts/start-ece-linux.sh" -ForegroundColor Yellow
        return $false
    }

    Write-Host "Starting Linux ECE through bash scripts/start-ece-linux.sh with debug IO logging enabled." -ForegroundColor Cyan
    if (-not (Clear-EceDebugLog)) {
        Write-Host "Warning: debug IO log could not be cleared before starting Real ECE." -ForegroundColor Yellow
    }
    $code = Invoke-WslEceScript -ScriptName "start-ece-linux.sh"
    if ($code -ne 0) {
        Write-Host "Linux ECE start script exited with code $code." -ForegroundColor Yellow
        return $false
    }

    $deadline = (Get-Date).AddSeconds($EceStartTimeoutSeconds)
    Write-Host "Waiting for $EceHealthUrl"
    while ((Get-Date) -lt $deadline) {
        $health = Test-EceHealth
        if ($null -ne $health) {
            Write-Host "ECE reachable." -ForegroundColor Green
            Show-EceHealth | Out-Null
            return $true
        }

        Write-Host "." -NoNewline
        Start-Sleep -Seconds 2
    }

    Write-Host ""
    Write-Host "ECE was not reachable after ${EceStartTimeoutSeconds}s: $EceHealthUrl" -ForegroundColor Yellow
    return $false
}

function Start-TestEce {
    Write-Host ""
    Write-Host "Starting/checking EvenChess test ECE payload server..." -ForegroundColor Cyan

    $existingHealth = Test-EceHealth
    if ($null -ne $existingHealth) {
        if (($null -ne (Get-EcePidFromState -Path $TestEcePidPath)) -or (Test-EceHealthLooksLikeTestServer -Health $existingHealth)) {
            $testProcess = Find-TestEceProcess
            $commandLine = ""
            if ($null -ne $testProcess) {
                try {
                    $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId=$($testProcess.Id)" -ErrorAction Stop
                    $commandLine = $processInfo.CommandLine
                }
                catch {
                    $commandLine = ""
                }
            }

            if ($TestEceBindHost -eq "0.0.0.0" -and $commandLine -and $commandLine -notlike "*--host 0.0.0.0*") {
                Write-Host "Existing Test ECE was not started with Docker-host access; restarting it." -ForegroundColor Yellow
                Stop-TestEce
                Start-Sleep -Seconds 1
            }
            else {
                Write-Host "Test ECE is already running on $EceUrl." -ForegroundColor Green
                Show-EceHealth | Out-Null
                return $true
            }
        }
        else {
            Write-Host "ECE endpoint already running on $EceUrl, but it was not launched as the Test Ground test ECE." -ForegroundColor Yellow
            Write-Host "Stop Real ECE before starting Test ECE." -ForegroundColor Yellow
            Show-EceHealth | Out-Null
            return $false
        }
    }

    if (-not (Test-Path -LiteralPath $TestEceScriptPath -PathType Leaf)) {
        Write-Host "Test ECE server script not found: $TestEceScriptPath" -ForegroundColor Yellow
        return $false
    }

    $nodeCommand = Get-Command "node" -ErrorAction SilentlyContinue
    if ($null -eq $nodeCommand) {
        Write-Host "Node.js was not found on PATH. Install Node.js or start a test ECE manually." -ForegroundColor Yellow
        return $false
    }

    Set-TestEceDebugEnvironment
    if (-not (Clear-EceDebugLog)) {
        Write-Host "Warning: debug IO log could not be cleared before starting Test ECE." -ForegroundColor Yellow
    }
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $EceDebugIoLogHostPath) | Out-Null
    New-Item -ItemType Directory -Force -Path $EceStateDir | Out-Null

    try {
        Write-Host "Binding Test ECE on $TestEceBindHost for Docker-host access; Windows URL remains $EceUrl." -ForegroundColor Cyan
        $process = Start-Process `
            -FilePath $nodeCommand.Source `
            -ArgumentList @($TestEceScriptPath, "--host", $TestEceBindHost, "--port", "$EcePort") `
            -WorkingDirectory $PSScriptRoot `
            -WindowStyle Hidden `
            -PassThru
        Set-Content -LiteralPath $TestEcePidPath -Value $process.Id -Encoding ASCII
        Write-Host "Started test ECE process PID $($process.Id)." -ForegroundColor Green
    }
    catch {
        Write-Host "Failed to start test ECE: $($_.Exception.Message)" -ForegroundColor Yellow
        return $false
    }

    $deadline = (Get-Date).AddSeconds($EceStartTimeoutSeconds)
    Write-Host "Waiting for $EceHealthUrl"
    while ((Get-Date) -lt $deadline) {
        $health = Test-EceHealth
        if ($null -ne $health) {
            Write-Host "Test ECE reachable." -ForegroundColor Green
            Show-EceHealth | Out-Null
            return $true
        }

        if ($process.HasExited) {
            Write-Host ""
            Write-Host "Test ECE process exited before health became reachable." -ForegroundColor Yellow
            Remove-Item -LiteralPath $TestEcePidPath -Force -ErrorAction SilentlyContinue
            return $false
        }

        Write-Host "." -NoNewline
        Start-Sleep -Seconds 2
    }

    Write-Host ""
    Write-Host "Test ECE was not reachable after ${EceStartTimeoutSeconds}s: $EceHealthUrl" -ForegroundColor Yellow
    return $false
}

function Get-EcePidFromState {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return $null
    }

    $pidText = (Get-Content -LiteralPath $Path -ErrorAction SilentlyContinue | Select-Object -First 1)
    if (-not $pidText) {
        return $null
    }

    $parsedPid = 0
    if ([int]::TryParse($pidText.Trim(), [ref]$parsedPid)) {
        return $parsedPid
    }

    return $null
}

function Test-EceHealthLooksLikeTestServer {
    param(
        [Parameter(Mandatory = $true)]
        [AllowNull()]
        [object]$Health
    )

    if ($null -eq $Health) {
        return $false
    }

    $mode = Select-FirstValue @(
        (Get-JsonProperty -InputObject $Health -Name "mode"),
        (Get-JsonPath -InputObject $Health -Path @("data", "mode"))
    )
    $testPayload = Select-FirstValue @(
        (Get-JsonProperty -InputObject $Health -Name "test_payload"),
        (Get-JsonPath -InputObject $Health -Path @("data", "test_payload"))
    )

    return "$mode" -eq "test-ground-mock" -or "$testPayload" -eq "True" -or "$testPayload" -eq "true"
}

function Find-TestEceProcess {
    try {
        $processes = Get-CimInstance Win32_Process -Filter "Name = 'node.exe'" -ErrorAction Stop
        foreach ($process in $processes) {
            $commandLine = $process.CommandLine
            if ($commandLine -and $commandLine -like "*evenchess-test-ece-server.js*") {
                return Get-Process -Id $process.ProcessId -ErrorAction SilentlyContinue
            }
        }
    }
    catch {
        return $null
    }

    return $null
}

function Test-TestEceProcessIdentity {
    param(
        [Parameter(Mandatory = $true)]
        [System.Diagnostics.Process]$Process
    )

    $nameOk = $Process.ProcessName -ieq "node" -or $Process.ProcessName -ieq "node.exe"
    if (-not $nameOk) {
        return $false
    }

    try {
        $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId=$($Process.Id)" -ErrorAction Stop
        $commandLine = $processInfo.CommandLine
        if ($commandLine) {
            return ($commandLine -like "*evenchess-test-ece-server.js*")
        }
    }
    catch {
        return $false
    }

    return $false
}

function Stop-RealEce {
    Write-Host ""
    Write-Host "Stopping Linux EvenChess Engine..." -ForegroundColor Cyan
    $manualStop = "Stop ECE with: cd $EceRoot && bash scripts/stop-ece-linux.sh"
    $stopScript = "$($EceRoot.TrimEnd('/'))/scripts/stop-ece-linux.sh"
    $health = Test-EceHealth

    if (($null -ne (Get-EcePidFromState -Path $TestEcePidPath)) -or (($null -ne $health) -and (Test-EceHealthLooksLikeTestServer -Health $health))) {
        Write-Host "Test ECE is running on $EceUrl. Use Stop Test ECE; Real ECE stop was not attempted." -ForegroundColor Yellow
        return
    }

    if (-not (Test-WslPath -Path $stopScript -Type "f")) {
        Write-Host "Linux ECE stop script not found: $stopScript" -ForegroundColor Yellow
        Write-Host $manualStop
        return
    }

    $code = Invoke-WslEceScript -ScriptName "stop-ece-linux.sh"
    if ($code -eq 0) {
        Start-Sleep -Seconds 1
        if ($null -ne (Test-EceHealth)) {
            Write-Host "Linux ECE lifecycle stop script completed, but ECE is still reachable at $EceHealthUrl." -ForegroundColor Yellow
            Write-Host $manualStop
            return
        }

        Write-Host "Linux ECE lifecycle stop script completed and ECE is no longer reachable." -ForegroundColor Green
        return
    }

    Write-Host "Linux ECE lifecycle stop script exited with code $code." -ForegroundColor Yellow
    Write-Host $manualStop
}

function Stop-TestEce {
    Write-Host ""
    Write-Host "Stopping EvenChess test ECE payload server..." -ForegroundColor Cyan
    $manualStop = "Stop the node evenchess-test-ece-server.js process manually if it was not launched by this Test Ground session."
    $storedPid = Get-EcePidFromState -Path $TestEcePidPath

    if ($null -eq $storedPid) {
        $fallbackProcess = Find-TestEceProcess
        if ($null -ne $fallbackProcess) {
            try {
                Stop-Process -Id $fallbackProcess.Id -ErrorAction Stop
                Start-Sleep -Seconds 1
                Remove-Item -LiteralPath $TestEcePidPath -Force -ErrorAction SilentlyContinue
                Write-Host "Stopped test ECE process PID $($fallbackProcess.Id)." -ForegroundColor Green
                return
            }
            catch {
                Write-Host "Failed to stop discovered test ECE PID $($fallbackProcess.Id): $($_.Exception.Message)" -ForegroundColor Yellow
                Write-Host $manualStop
                return
            }
        }

        Remove-Item -LiteralPath $TestEcePidPath -Force -ErrorAction SilentlyContinue
        Write-Host "No Test Ground test ECE PID found." -ForegroundColor DarkGray
        $health = Test-EceHealth
        if ($null -ne $health) {
            if (Test-EceHealthLooksLikeTestServer -Health $health) {
                Write-Host "Test ECE is reachable, but its process could not be found from this session." -ForegroundColor Yellow
                Write-Host $manualStop
            }
            else {
                Write-Host "A non-test ECE endpoint is still reachable. Use Stop Real ECE if it was launched by the ECE lifecycle scripts." -ForegroundColor Yellow
            }
        }
        return
    }

    $process = Get-Process -Id $storedPid -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        Write-Host "Stored test ECE PID $storedPid is not running." -ForegroundColor DarkGray
        Remove-Item -LiteralPath $TestEcePidPath -Force -ErrorAction SilentlyContinue
        return
    }

    if (-not (Test-TestEceProcessIdentity -Process $process)) {
        Write-Host "Refusing to stop PID $storedPid because it does not look like the test ECE process." -ForegroundColor Yellow
        Write-Host $manualStop
        return
    }

    try {
        Stop-Process -Id $storedPid -ErrorAction Stop
        Start-Sleep -Seconds 1
        Write-Host "Stopped test ECE process PID $storedPid." -ForegroundColor Green
    }
    catch {
        Write-Host "Failed to stop test ECE PID ${storedPid}: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    finally {
        Remove-Item -LiteralPath $TestEcePidPath -Force -ErrorAction SilentlyContinue
    }
}

function Get-EceSampleBoardBody {
    $samplePath = "$($EceRoot.TrimEnd('/'))/fixtures/ece-v1-sample-input.json"
    $sample = Get-WslFileContent -Path $samplePath
    if ($sample) {
        return $sample
    }

    return @'
{
  "request": {
    "mode": "board_state",
    "request_id": "test-ground-sample-board",
    "input_fen": "rnbqkbnr/pppp1ppp/5n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
    "rating_type": "ecr",
    "white_rating_input": 1200,
    "black_rating_input": 1180,
    "white_level": 10,
    "black_level": 10,
    "use_ai": 1,
    "deep_requested": true,
    "requested_deep_modules": ["stockfish", "lichess_eval_cache", "syzygy", "maia", "ai_text"],
    "custom": {
      "opening": 1,
      "instructions": 0
    }
  }
}
'@
}

function Invoke-EceSampleBoard {
    Write-Host ""
    Write-Host "Calling sample ECE board quick endpoint..." -ForegroundColor Cyan

    if ($null -eq (Test-EceHealth)) {
        Write-Host "ECE is not reachable at $EceHealthUrl." -ForegroundColor Yellow
        return $false
    }

    try {
        $body = Get-EceSampleBoardBody
        $response = Invoke-RestMethod -Method Post -Uri $EceBoardUrl -Body $body -ContentType "application/json" -TimeoutSec 20 -ErrorAction Stop
    }
    catch {
        Write-Host "ECE board call failed: $($_.Exception.Message)" -ForegroundColor Yellow
        return $false
    }

    $schemaName = Select-FirstValue @(
        (Get-JsonProperty -InputObject $response -Name "schema_name"),
        (Get-JsonPath -InputObject $response -Path @("schema", "name")),
        (Get-JsonPath -InputObject $response -Path @("meta", "schema_name"))
    )
    $schemaVersion = Select-FirstValue @(
        (Get-JsonProperty -InputObject $response -Name "schema_version"),
        (Get-JsonPath -InputObject $response -Path @("schema", "version")),
        (Get-JsonPath -InputObject $response -Path @("meta", "schema_version"))
    )
    $positionHash = Select-FirstValue @(
        (Get-JsonProperty -InputObject $response -Name "position_hash"),
        (Get-JsonPath -InputObject $response -Path @("position", "hash")),
        (Get-JsonPath -InputObject $response -Path @("input", "position_hash"))
    )
    $sideToMove = Select-FirstValue @(
        (Get-JsonProperty -InputObject $response -Name "side_to_move"),
        (Get-JsonPath -InputObject $response -Path @("position", "side_to_move")),
        (Get-JsonPath -InputObject $response -Path @("input", "side_to_move"))
    )
    $diagnosticsStatus = Select-FirstValue @(
        (Get-JsonPath -InputObject $response -Path @("diagnostics", "status")),
        (Get-JsonProperty -InputObject $response -Name "status")
    )
    $sideOutputs = Get-JsonProperty -InputObject $response -Name "side_outputs"
    $whiteExists = $null -ne (Get-JsonProperty -InputObject $sideOutputs -Name "white")
    $blackExists = $null -ne (Get-JsonProperty -InputObject $sideOutputs -Name "black")

    Write-Host "  schema:                  $schemaName / $schemaVersion"
    Write-Host "  position_hash:           $positionHash"
    Write-Host "  side_to_move:            $sideToMove"
    Write-Host "  diagnostics_status:      $diagnosticsStatus"
    Write-Host "  side_outputs.white:      $whiteExists"
    Write-Host "  side_outputs.black:      $blackExists"

    $quickContext = Get-JsonProperty -InputObject $response -Name "quick_context"
    $contextId = Get-JsonProperty -InputObject $quickContext -Name "context_id"
    $deepStatus = Get-JsonProperty -InputObject $quickContext -Name "deep_status"
    Write-Host "  quick_context.status:    $deepStatus"

    if ($contextId) {
        $echo = Get-JsonProperty -InputObject $response -Name "request_echo"
        $quickRequestId = Get-JsonProperty -InputObject $echo -Name "request_id"
        $inputFen = Get-JsonProperty -InputObject $echo -Name "input_fen"
        $whiteLevel = Get-JsonProperty -InputObject $echo -Name "white_level"
        $blackLevel = Get-JsonProperty -InputObject $echo -Name "black_level"
        $deepBody = @{
            request = @{
                mode = "board_deep"
                request_id = "test-ground-sample-board-deep"
                quick_request_id = $quickRequestId
                quick_context_id = $contextId
                input_fen = $inputFen
                white_level = $whiteLevel
                black_level = $blackLevel
                use_ai = 1
                requested_deep_modules = @("stockfish", "lichess_eval_cache", "syzygy", "maia", "ai_text")
            }
        } | ConvertTo-Json -Depth 8

        try {
            $deepResponse = Invoke-RestMethod -Method Post -Uri $EceBoardDeepUrl -Body $deepBody -ContentType "application/json" -TimeoutSec 30 -ErrorAction Stop
            $deepDiagnosticsStatus = Select-FirstValue @(
                (Get-JsonPath -InputObject $deepResponse -Path @("diagnostics", "status")),
                (Get-JsonProperty -InputObject $deepResponse -Name "status")
            )
            $addenda = Get-JsonProperty -InputObject $deepResponse -Name "side_output_addenda"
            $whiteAddendum = $null -ne (Get-JsonProperty -InputObject $addenda -Name "white")
            $blackAddendum = $null -ne (Get-JsonProperty -InputObject $addenda -Name "black")
            Write-Host "  deep diagnostics_status: $deepDiagnosticsStatus"
            Write-Host "  addenda.white:           $whiteAddendum"
            Write-Host "  addenda.black:           $blackAddendum"
        }
        catch {
            Write-Host "ECE deep call failed: $($_.Exception.Message)" -ForegroundColor Yellow
            return $false
        }
    }
    return $true
}

function Start-TestGround {
    Start-DockerDesktop
    if (Wait-DockerInWsl) {
        if (-not (Test-EvenChessUiAssetsReady)) {
            Write-Host ""
            Write-Host "Start was not attempted because the UI assets are not ready." -ForegroundColor Yellow
            Write-Host "Press Enter to return to the menu."
            [void][Console]::ReadLine()
            return
        }

        Invoke-WslScript -ScriptName "evenchess-local-start.sh" -NoPause
        if ($script:LastWslScriptExitCode -eq 0) {
            Open-MainSite
        }
        else {
            Write-Host ""
            Write-Host "The browser was not opened because the start script did not finish cleanly." -ForegroundColor Yellow
        }

        Write-Host ""
        Write-Host "Press Enter to return to the menu."
        [void][Console]::ReadLine()
    }
    else {
        Write-Host ""
        Write-Host "Start was not attempted because Docker is not ready in WSL." -ForegroundColor Yellow
        Write-Host "Press Enter to return to the menu."
        [void][Console]::ReadLine()
    }
}

function Start-TestGroundNoPause {
    Start-DockerDesktop
    if (-not (Wait-DockerInWsl)) {
        Write-Host ""
        Write-Host "Start was not attempted because Docker is not ready in WSL." -ForegroundColor Yellow
        return $false
    }

    if (-not (Test-EvenChessUiAssetsReady)) {
        Write-Host ""
        Write-Host "Start was not attempted because the UI assets are not ready." -ForegroundColor Yellow
        return $false
    }

    Invoke-WslScript -ScriptName "evenchess-local-start.sh" -NoPause
    if ($script:LastWslScriptExitCode -ne 0) {
        Write-Host ""
        Write-Host "The browser was not opened because the start script did not finish cleanly." -ForegroundColor Yellow
        return $false
    }

    Open-MainSite
    return $true
}

function Start-EvenChessNoPause {
    if (-not (Test-DockerInWsl)) {
        Write-Host ""
        Write-Host "EvenChess was not launched because Docker is not ready inside WSL $WslDistro." -ForegroundColor Yellow
        Write-Host "Use Start WSL/Docker first, then launch EvenChess."
        return $false
    }

    if (-not (Test-EvenChessUiAssetsReady)) {
        Write-Host ""
        Write-Host "EvenChess launch was not attempted because the UI assets are not ready." -ForegroundColor Yellow
        return $false
    }

    Invoke-WslScript -ScriptName "evenchess-local-start.sh" -NoPause
    if ($script:LastWslScriptExitCode -ne 0) {
        Write-Host ""
        Write-Host "EvenChess launch script did not finish cleanly." -ForegroundColor Yellow
        return $false
    }

    Open-MainSite
    return $true
}

function Show-TestGroundStatus {
    Invoke-WslScript -ScriptName "evenchess-local-status.sh" -NoPause
    Show-EceHealth | Out-Null
    Show-ClmStatus | Out-Null
    Write-Host ""
    Write-Host "Press Enter to return to the menu."
    [void][Console]::ReadLine()
}

function Show-TestGroundStatusNoPause {
    Invoke-WslScript -ScriptName "evenchess-local-status.sh" -NoPause
    Show-EceHealth | Out-Null
    Show-ClmStatus | Out-Null
}

function Stop-TestGroundAndShutdownWsl {
    Stop-EceClm | Out-Null
    Stop-RealEce
    Stop-TestEce
    Invoke-WslScript -ScriptName "evenchess-local-stop.sh" -NoPause
    $code = $script:LastWslScriptExitCode

    Write-Host ""
    if ($code -ne 0) {
        Write-Host "The container stop command did not finish cleanly; WSL will still be shut down to release memory." -ForegroundColor Yellow
    }

    Stop-DockerWsl | Out-Null

    Write-Host "Shutting down WSL to release VmmemWSL memory..." -ForegroundColor Cyan
    & wsl.exe --shutdown
    $shutdownCode = $LASTEXITCODE
    if ($shutdownCode -eq 0) {
        Write-Host "WSL shutdown requested successfully." -ForegroundColor Green
    }
    else {
        Write-Host "wsl --shutdown exited with code $shutdownCode." -ForegroundColor Yellow
    }

    Write-Host ""
    Write-Host "Press Enter to return to the menu, or close this window."
    [void][Console]::ReadLine()
}

function Stop-TestGroundAndShutdownWslNoPause {
    Stop-EceClm | Out-Null
    Stop-RealEce
    Stop-TestEce
    Invoke-WslScript -ScriptName "evenchess-local-stop.sh" -NoPause
    $code = $script:LastWslScriptExitCode

    Write-Host ""
    if ($code -ne 0) {
        Write-Host "The container stop command did not finish cleanly; WSL will still be shut down to release memory." -ForegroundColor Yellow
    }

    Stop-DockerWsl | Out-Null

    Write-Host "Shutting down WSL to release VmmemWSL memory..." -ForegroundColor Cyan
    & wsl.exe --shutdown
    $shutdownCode = $LASTEXITCODE
    if ($shutdownCode -eq 0) {
        Write-Host "WSL shutdown requested successfully." -ForegroundColor Green
        return $true
    }

    Write-Host "wsl --shutdown exited with code $shutdownCode." -ForegroundColor Yellow
    return $false
}

function Invoke-TestGroundAction {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RequestedAction
    )

    switch ($RequestedAction.Trim().ToLowerInvariant()) {
        { $_ -in @("start-docker", "start-wsl-docker", "docker-start") } {
            if (Start-DockerWsl) { return 0 }
            return 1
        }
        { $_ -in @("stop-docker", "stop-wsl-docker", "docker-stop") } {
            if (Stop-DockerWsl) { return 0 }
            return 1
        }
        { $_ -in @("launch-evenchess", "evenchess", "launch") } {
            if (Start-EvenChessNoPause) { return 0 }
            return 1
        }
        { $_ -in @("build-ui", "ui-build", "build-assets") } {
            if (Build-EvenChessUiAssets) { return 0 }
            return 1
        }
        { $_ -in @("start-stack", "start", "stack") } {
            if (Start-TestGroundNoPause) { return 0 }
            return 1
        }
        { $_ -in @("status", "stack-status") } {
            Show-TestGroundStatusNoPause
            return 0
        }
        { $_ -in @("open-site", "open") } {
            Open-MainSite
            return 0
        }
        { $_ -in @("open-ece-settings", "ece-settings", "settings-ece") } {
            Open-EceSettingsPage
            return 0
        }
        { $_ -in @("stop-containers", "containers", "stop") } {
            Invoke-WslScript -ScriptName "evenchess-local-stop.sh" -NoPause
            if ($script:LastWslScriptExitCode -eq 0) { return 0 }
            return 1
        }
        { $_ -in @("shutdown", "stop-and-shutdown") } {
            if (Stop-TestGroundAndShutdownWslNoPause) { return 0 }
            return 1
        }
        { $_ -in @("start-panel", "panel", "open-panel") } {
            if (Start-TestGroundPanel) { return 0 }
            return 1
        }
        { $_ -in @("stop-panel", "panel-stop") } {
            Stop-TestGroundPanel
            return 0
        }
        { $_ -in @("start-real-ece", "real-ece", "ece") } {
            if (Start-RealEce) { return 0 }
            return 1
        }
        { $_ -in @("start-test-ece", "test-ece", "mock-ece", "mock") } {
            if (Start-TestEce) { return 0 }
            return 1
        }
        { $_ -in @("launch-clm", "start-clm", "clm") } {
            if (Start-EceClm) { return 0 }
            return 1
        }
        { $_ -in @("stop-clm", "clm-stop") } {
            if (Stop-EceClm) { return 0 }
            return 1
        }
        { $_ -in @("clm-status", "status-clm") } {
            if (Show-ClmStatus) { return 0 }
            return 1
        }
        { $_ -in @("grant-admin-access", "admin-access", "grant-settings") } {
            if (Grant-LocalAdminSettingsAccess) { return 0 }
            return 1
        }
        { $_ -in @("stop-real-ece", "stop-ece-real") } {
            Stop-RealEce
            return 0
        }
        { $_ -in @("stop-test-ece", "stop-mock-ece", "stop-mock") } {
            Stop-TestEce
            return 0
        }
        { $_ -in @("health", "ece-health") } {
            if (Show-EceHealth) { return 0 }
            return 1
        }
        { $_ -in @("sample-board", "board") } {
            if (Invoke-EceSampleBoard) { return 0 }
            return 1
        }
        default {
            Write-Host "Unknown Test Ground action: $RequestedAction" -ForegroundColor Yellow
            Write-Host "Allowed actions: start-docker, stop-docker, launch-evenchess, build-ui, start-stack, status, open-site, open-ece-settings, stop-containers, shutdown, start-panel, stop-panel, start-real-ece, start-test-ece, stop-real-ece, stop-test-ece, launch-clm, stop-clm, clm-status, grant-admin-access, health, sample-board."
            return 2
        }
    }
}

if (-not $Action.Trim()) {
    Clear-EceDebugLog | Out-Null
}

if ($Action.Trim()) {
    exit (Invoke-TestGroundAction -RequestedAction $Action)
}

if (-not $Menu) {
    if (Start-TestGroundPanel) {
        exit 0
    }

    Write-Host ""
    Write-Host "Panel startup failed. Run with -Menu to use the text menu." -ForegroundColor Yellow
    exit 1
}

while ($true) {
    Write-Title
    $choice = Read-Host "Choose"
    switch ($choice.Trim().ToLowerInvariant()) {
        { $_ -in @("s", "start", "1") } {
            Start-TestGround
        }
        { $_ -in @("p", "panel", "browser-panel") } {
            Start-TestGroundPanel | Out-Null
            Write-Host ""
            Write-Host "Press Enter to return to the menu."
            [void][Console]::ReadLine()
        }
        { $_ -in @("t", "status", "2") } {
            Show-TestGroundStatus
        }
        { $_ -in @("u", "build-ui", "ui-build") } {
            Build-EvenChessUiAssets | Out-Null
            Write-Host ""
            Write-Host "Press Enter to return to the menu."
            [void][Console]::ReadLine()
        }
        { $_ -in @("o", "open", "3") } {
            Open-MainSite
        }
        { $_ -in @("g", "ece-settings", "open-ece-settings") } {
            Open-EceSettingsPage
        }
        { $_ -in @("c", "containers", "stop", "4") } {
            Invoke-WslScript -ScriptName "evenchess-local-stop.sh"
        }
        { $_ -in @("e", "engine", "real-ece", "6") } {
            Start-RealEce | Out-Null
            Write-Host ""
            Write-Host "Press Enter to return to the menu."
            [void][Console]::ReadLine()
        }
        { $_ -in @("m", "mock", "test-ece", "test", "10") } {
            Start-TestEce | Out-Null
            Write-Host ""
            Write-Host "Press Enter to return to the menu."
            [void][Console]::ReadLine()
        }
        { $_ -in @("l", "clm", "start-clm", "launch-clm") } {
            Start-EceClm | Out-Null
            Write-Host ""
            Write-Host "Press Enter to return to the menu."
            [void][Console]::ReadLine()
        }
        { $_ -in @("r", "stop-clm", "clm-stop") } {
            Stop-EceClm | Out-Null
            Write-Host ""
            Write-Host "Press Enter to return to the menu."
            [void][Console]::ReadLine()
        }
        { $_ -in @("h", "health", "7") } {
            Show-EceHealth | Out-Null
            Write-Host ""
            Write-Host "Press Enter to return to the menu."
            [void][Console]::ReadLine()
        }
        { $_ -in @("b", "board", "sample", "8") } {
            Invoke-EceSampleBoard
            Write-Host ""
            Write-Host "Press Enter to return to the menu."
            [void][Console]::ReadLine()
        }
        { $_ -in @("v", "stop-real-ece", "engine-stop", "9") } {
            Stop-RealEce
            Write-Host ""
            Write-Host "Press Enter to return to the menu."
            [void][Console]::ReadLine()
        }
        { $_ -in @("n", "stop-test-ece", "mock-stop", "11") } {
            Stop-TestEce
            Write-Host ""
            Write-Host "Press Enter to return to the menu."
            [void][Console]::ReadLine()
        }
        { $_ -in @("x", "shutdown", "5") } {
            Stop-TestGroundAndShutdownWsl
        }
        { $_ -in @("q", "quit", "exit") } {
            break
        }
        default {
            Write-Host "Unknown option: $choice" -ForegroundColor Yellow
            Start-Sleep -Seconds 1
        }
    }
}
