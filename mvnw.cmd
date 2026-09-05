<# : batch portion
@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script, compatible with Maven Wrapper 3.3.x.
@REM ----------------------------------------------------------------------------
@set "MVNW_SCRIPT_PATH=%~f0"
@powershell -NoProfile -ExecutionPolicy Bypass -Command "& ([ScriptBlock]::Create((Get-Content -Raw $env:MVNW_SCRIPT_PATH)))" %*
@exit /b %errorlevel%
: end batch / begin PowerShell #>

$ErrorActionPreference = "Stop"
$projectBaseDir = Split-Path -Parent $env:MVNW_SCRIPT_PATH
$propertiesPath = Join-Path $projectBaseDir ".mvn\wrapper\maven-wrapper.properties"
$properties = Get-Content $propertiesPath | ConvertFrom-StringData
$distributionUrl = $properties.distributionUrl
$archiveName = Split-Path $distributionUrl -Leaf
$mavenHome = Join-Path $env:USERPROFILE ".m2\wrapper\dists\$($archiveName -replace '-bin.zip','')"
$mavenCommand = Join-Path $mavenHome "bin\mvn.cmd"

if (-not (Test-Path $mavenCommand)) {
    New-Item -ItemType Directory -Force -Path (Split-Path $mavenHome) | Out-Null
    $temporaryArchive = Join-Path ([System.IO.Path]::GetTempPath()) $archiveName
    Invoke-WebRequest -Uri $distributionUrl -OutFile $temporaryArchive
    $temporaryDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ([System.Guid]::NewGuid().ToString())
    Expand-Archive $temporaryArchive $temporaryDirectory
    $expandedDirectory = Get-ChildItem $temporaryDirectory -Directory | Select-Object -First 1
    Move-Item $expandedDirectory.FullName $mavenHome
    Remove-Item $temporaryArchive
    Remove-Item $temporaryDirectory -Recurse
}

& $mavenCommand -f (Join-Path $projectBaseDir "pom.xml") @args
exit $LASTEXITCODE
