<#
.SYNOPSIS
    Build FPS_Modpack_Optimized.mrpack for Minecraft 1.21.11 / Fabric.

.DESCRIPTION
    Resolves every mod in optimization-mods.json against the Modrinth API, pulls in
    their required dependencies, merges them with the verified base index in pack\,
    and writes a finished .mrpack you can import into the Modrinth App or Prism.

    Needs nothing but Windows PowerShell 5.1 (already on your PC) and internet.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\build-pack.ps1
#>
[CmdletBinding()]
param(
    [string] $Out,
    [string] $Mc
)

$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$Api  = 'https://api.modrinth.com/v2'
$Ua   = 'fps-modpack-builder/2.0 (github.com/stashoffman010-droid/claude)'
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
if (-not $Out) { $Out = Join-Path $Root 'dist' }

function Invoke-Api {
    param([string] $Path, [hashtable] $Query)
    $url = "$Api$Path"
    if ($Query) {
        $pairs = foreach ($k in $Query.Keys) {
            "$k=" + [uri]::EscapeDataString([string]$Query[$k])
        }
        $url += '?' + ($pairs -join '&')
    }
    Invoke-RestMethod -Uri $url -Headers @{ 'User-Agent' = $Ua } -TimeoutSec 60
}

function Resolve-Project {
    param([string] $Slug, [string] $Name)
    try {
        return (Invoke-Api -Path "/project/$([uri]::EscapeDataString($Slug))").slug
    } catch {
        $code = $null
        if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
        if ($code -ne 404) { throw }
    }
    # Stale slug: fall back to a Modrinth search so a renamed project self-heals.
    $facets = '[["project_type:mod"],["categories:fabric"]]'
    $hits = (Invoke-Api -Path '/search' -Query @{ query = $Name; facets = $facets; limit = 3 }).hits
    if (-not $hits -or $hits.Count -eq 0) { return $null }
    Write-Host "    slug '$Slug' is stale -> resolved by search to '$($hits[0].slug)'" -ForegroundColor DarkYellow
    return $hits[0].slug
}

function Select-Version {
    param([string] $Project, [string] $McVersion, [string] $Loader)
    $versions = Invoke-Api -Path "/project/$([uri]::EscapeDataString($Project))/version" -Query @{
        loaders       = "[""$Loader""]"
        game_versions = "[""$McVersion""]"
    }
    if (-not $versions -or @($versions).Count -eq 0) { return $null }
    $rank = @{ release = 0; beta = 1; alpha = 2 }
    @($versions) | Sort-Object `
        @{ Expression = { if ($rank.ContainsKey($_.version_type)) { $rank[$_.version_type] } else { 3 } } }, `
        @{ Expression = { $_.date_published }; Descending = $true } |
        Select-Object -First 1
}

function New-Entry {
    param($Version, [string] $Side, [bool] $Enabled)
    $f = @($Version.files) | Where-Object { $_.primary } | Select-Object -First 1
    if (-not $f) { $f = @($Version.files)[0] }
    $path = "mods/$($f.filename)"
    if (-not $Enabled) { $path += '.disabled' }
    $serverEnv = if ($Side -eq 'both') { 'required' } else { 'unsupported' }
    [ordered]@{
        path      = $path
        hashes    = [ordered]@{ sha1 = $f.hashes.sha1; sha512 = $f.hashes.sha512 }
        env       = [ordered]@{ client = 'required'; server = $serverEnv }
        downloads = @($f.url)
        fileSize  = $f.size
    }
}

function Get-Key { param([string] $Path)
    $n = [IO.Path]::GetFileName($Path).ToLowerInvariant()
    if ($n.EndsWith('.disabled')) { $n = $n.Substring(0, $n.Length - 9) }
    $n
}

# ---------------------------------------------------------------- inputs ----
$manifest = Get-Content (Join-Path $Root 'optimization-mods.json') -Raw | ConvertFrom-Json
if (-not $Mc) { $Mc = $manifest.minecraft }
$loader = $manifest.loader

$basePath = Join-Path (Join-Path $Root 'pack') 'modrinth.index.json'
$base = Get-Content $basePath -Raw | ConvertFrom-Json

$have    = New-Object 'System.Collections.Generic.HashSet[string]'
$haveSha = New-Object 'System.Collections.Generic.HashSet[string]'
foreach ($f in $base.files) {
    [void]$have.Add((Get-Key $f.path))
    [void]$haveSha.Add($f.hashes.sha1)
}

Write-Host "Building for Minecraft $Mc / $loader"
Write-Host "Base pack: $(@($base.files).Count) verified files`n"

$added   = New-Object System.Collections.ArrayList
$missing = New-Object System.Collections.ArrayList
$seen    = New-Object 'System.Collections.Generic.HashSet[string]'
$queue   = New-Object System.Collections.Queue

foreach ($m in $manifest.mods) {
    $enabled = $true
    if ($null -ne $m.enabled) { $enabled = [bool]$m.enabled }
    $side = if ($m.side) { $m.side } else { 'client' }
    $queue.Enqueue([pscustomobject]@{ Slug = $m.slug; Name = $m.name; Side = $side; Enabled = $enabled; IsDep = $false })
}

while ($queue.Count -gt 0) {
    $item  = $queue.Dequeue()
    $label = '  ' + $(if ($item.IsDep) { 'dep ' } else { '' }) + $item.Name

    $project = Resolve-Project -Slug $item.Slug -Name $item.Name
    if (-not $project) {
        Write-Host "${label}: NOT FOUND on Modrinth" -ForegroundColor Yellow
        [void]$missing.Add($item.Name); continue
    }
    if ($seen.Contains($project)) { continue }
    [void]$seen.Add($project)

    $version = Select-Version -Project $project -McVersion $Mc -Loader $loader
    if (-not $version) {
        Write-Host "${label}: no $loader build for $Mc" -ForegroundColor Yellow
        [void]$missing.Add($item.Name); continue
    }

    $entry = New-Entry -Version $version -Side $item.Side -Enabled $item.Enabled
    if ($have.Contains((Get-Key $entry.path)) -or $haveSha.Contains($entry.hashes.sha1)) {
        Write-Host "${label}: already in base pack, skipping" -ForegroundColor DarkGray
        continue
    }

    [void]$added.Add($entry)
    [void]$have.Add((Get-Key $entry.path))
    [void]$haveSha.Add($entry.hashes.sha1)
    $flag = if ($item.Enabled) { '' } else { '  [shipped disabled]' }
    Write-Host "${label}: $($version.version_number)$flag" -ForegroundColor Green

    foreach ($dep in $version.dependencies) {
        if ($dep.dependency_type -ne 'required') { continue }
        if (-not $dep.project_id -or $seen.Contains($dep.project_id)) { continue }
        $queue.Enqueue([pscustomobject]@{
            Slug = $dep.project_id; Name = $dep.project_id
            Side = $item.Side; Enabled = $true; IsDep = $true })
    }
}

# ---------------------------------------------------------------- output ----
$allFiles = New-Object System.Collections.ArrayList
foreach ($f in $base.files) {
    [void]$allFiles.Add([ordered]@{
        path      = $f.path
        hashes    = [ordered]@{ sha1 = $f.hashes.sha1; sha512 = $f.hashes.sha512 }
        env       = [ordered]@{ client = $f.env.client; server = $f.env.server }
        downloads = @($f.downloads)
        fileSize  = $f.fileSize
    })
}
foreach ($e in $added) { [void]$allFiles.Add($e) }
$sorted = @($allFiles | Sort-Object { $_.path.ToLowerInvariant() })

$index = [ordered]@{
    formatVersion = 1
    game          = 'minecraft'
    versionId     = $base.versionId
    name          = $base.name
    summary       = $base.summary
    dependencies  = [ordered]@{ minecraft = $Mc; 'fabric-loader' = $base.dependencies.'fabric-loader' }
    files         = $sorted
}

New-Item -ItemType Directory -Force -Path $Out | Out-Null
$dest = Join-Path $Out "FPS_Modpack_Optimized_$($base.versionId).mrpack"
if (Test-Path $dest) { Remove-Item $dest -Force }

$stage = Join-Path ([IO.Path]::GetTempPath()) ("mrpack-" + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $stage | Out-Null
try {
    $json = $index | ConvertTo-Json -Depth 24
    [IO.File]::WriteAllText((Join-Path $stage 'modrinth.index.json'), $json + "`n", (New-Object Text.UTF8Encoding $false))
    $overrides = Join-Path (Join-Path $Root 'pack') 'overrides'
    if (Test-Path $overrides) {
        Copy-Item $overrides -Destination (Join-Path $stage 'overrides') -Recurse -Force
    }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Compression.ZipFile]::CreateFromDirectory($stage, $dest)
} finally {
    Remove-Item $stage -Recurse -Force -ErrorAction SilentlyContinue
}

$totalBytes = 0L
foreach ($f in $sorted) { $totalBytes += [int64]$f.fileSize }
$totalMb = [math]::Round($totalBytes / 1MB)
Write-Host "`nAdded $($added.Count) optimization mods ($($sorted.Count) files total, ~$totalMb MB to download)"
if ($missing.Count -gt 0) {
    Write-Host "`nCould not resolve for this version:" -ForegroundColor Yellow
    foreach ($m in $missing) { Write-Host "  - $m" -ForegroundColor Yellow }
}
Write-Host "`nWrote $dest" -ForegroundColor Cyan
Write-Host 'Import it in the Modrinth App or Prism Launcher: it will download every mod itself.'
