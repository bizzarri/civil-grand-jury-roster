param(
 [Parameter(Mandatory=$true)][string]$AndroidJar,
 [string]$JavaHome = $env:JAVA_HOME,
 [string]$JsonJar,
 [string]$TestDirectory = (Join-Path $PSScriptRoot '.build/tests')
)
$ErrorActionPreference = 'Stop'
if (-not $JavaHome) { $JavaHome = Split-Path (Split-Path (Get-Command javac).Source) }
$AndroidJar = (Resolve-Path -LiteralPath $AndroidJar).Path
New-Item -ItemType Directory -Force -Path $TestDirectory | Out-Null
$TestDirectory = (Resolve-Path -LiteralPath $TestDirectory).Path
$expectedHash = '3CF6CD6892E32E2B4C1C39E0F52F5248A2F5B37646FDFBB79A66B46B618414ED'
if (-not $JsonJar) {
 $JsonJar = Join-Path $TestDirectory 'json-20240303.jar'
 if (-not (Test-Path -LiteralPath $JsonJar)) {
  Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar' -OutFile $JsonJar
 }
}
$JsonJar = (Resolve-Path -LiteralPath $JsonJar).Path
if ((Get-FileHash -LiteralPath $JsonJar -Algorithm SHA256).Hash -ne $expectedHash) { throw 'JSON test dependency hash mismatch; use org.json:json:20240303 from Maven Central.' }
$classes = Join-Path $TestDirectory ('classes-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $classes | Out-Null
$main = Join-Path $PSScriptRoot 'app/src/main/java/com/mauri/juryroster'
$sources = @('RosterCore.java','MeetingBook.java','CourtFile.java','HandoffCrypto.java','Store.java') | ForEach-Object { Join-Path $main $_ }
$sources += @(Get-ChildItem (Join-Path $PSScriptRoot 'tests') -Filter '*.java' | ForEach-Object { $_.FullName })
& "$JavaHome/bin/javac.exe" -encoding UTF-8 -cp "$JsonJar;$AndroidJar" -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
foreach ($suite in @('CoreTests','PersistenceTests','EmailTests','ConfigTests','FilterTests','OnboardingTests','MeetingTests')) {
 & "$JavaHome/bin/java.exe" -cp "$JsonJar;$classes;$AndroidJar" "com.mauri.juryroster.$suite"
 if ($LASTEXITCODE -ne 0) { throw "$suite failed." }
}
Write-Output 'All public synthetic-data test suites passed.'
