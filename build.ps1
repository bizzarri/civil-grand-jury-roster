param(
 [string]$BuildTools,
 [string]$AndroidJar,
 [string]$JavaHome = $env:JAVA_HOME,
 [string]$BuildDirectory = (Join-Path $PSScriptRoot '.build')
)
$ErrorActionPreference = 'Stop'
if (-not $BuildTools -or -not $AndroidJar) { throw 'Pass -BuildTools (SDK build-tools/36.0.0 folder) and -AndroidJar (SDK platforms/android-36/android.jar).' }
if (-not $JavaHome) { $JavaHome = Split-Path (Split-Path (Get-Command javac).Source) }
if (-not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin/javac.exe'))) { throw 'JavaHome must point to a JDK, not a JRE.' }
$BuildTools = (Resolve-Path -LiteralPath $BuildTools).Path
$AndroidJar = (Resolve-Path -LiteralPath $AndroidJar).Path
New-Item -ItemType Directory -Force -Path $BuildDirectory | Out-Null
$BuildDirectory = (Resolve-Path -LiteralPath $BuildDirectory).Path
$classes = Join-Path $BuildDirectory 'classes'
$dex = Join-Path $BuildDirectory 'dex'
New-Item -ItemType Directory -Force -Path $classes,$dex | Out-Null
function Checked([string]$Program,[string[]]$Arguments) {
 & $Program @Arguments
 if ($LASTEXITCODE -ne 0) { throw "$Program failed with exit code $LASTEXITCODE" }
}
$main = Join-Path $PSScriptRoot 'app/src/main'
Checked "$BuildTools/aapt2.exe" @('compile','--dir',"$main/res",'-o',"$BuildDirectory/resources.zip")
Checked "$BuildTools/aapt2.exe" @('link','-o',"$BuildDirectory/unsigned.apk",'-I',$AndroidJar,'--manifest',"$main/AndroidManifest.xml",'--version-code','4','--version-name','1.3','--min-sdk-version','26','--target-sdk-version','36',"$BuildDirectory/resources.zip")
$sources = @(Get-ChildItem "$main/java" -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
Checked "$JavaHome/bin/javac.exe" (@('-encoding','UTF-8','-source','8','-target','8','-bootclasspath',($AndroidJar + ';' + "$BuildTools/core-lambda-stubs.jar"),'-d',$classes) + $sources)
Checked "$JavaHome/bin/jar.exe" @("-J-Djava.io.tmpdir=$BuildDirectory",'cf',"$BuildDirectory/classes.jar",'-C',$classes,'.')
Checked "$JavaHome/bin/java.exe" @('-cp',"$BuildTools/lib/d8.jar",'com.android.tools.r8.D8','--lib',$AndroidJar,'--min-api','26','--output',$dex,"$BuildDirectory/classes.jar")
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::Open("$BuildDirectory/unsigned.apk",[IO.Compression.ZipArchiveMode]::Update)
try {
 Get-ChildItem $dex -Filter '*.dex' | ForEach-Object { [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,$_.FullName,$_.Name) | Out-Null }
} finally { $archive.Dispose() }
Checked "$BuildTools/zipalign.exe" @('-f','-p','4',"$BuildDirectory/unsigned.apk","$BuildDirectory/aligned.apk")
$key = Join-Path $BuildDirectory 'development.keystore'
if (-not (Test-Path -LiteralPath $key)) {
 Checked "$JavaHome/bin/keytool.exe" @('-genkeypair','-keystore',$key,'-storepass','android','-keypass','android','-alias','androiddebugkey','-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=Jury Roster Development,O=Local,C=US')
}
$distribution = Join-Path $PSScriptRoot 'dist'
New-Item -ItemType Directory -Force -Path $distribution | Out-Null
$apk = Join-Path $distribution 'CivilGrandJuryRoster-1.3.apk'
Checked "$JavaHome/bin/java.exe" @('-jar',"$BuildTools/lib/apksigner.jar",'sign','--ks',$key,'--ks-pass','pass:android','--key-pass','pass:android','--out',$apk,"$BuildDirectory/aligned.apk")
Checked "$JavaHome/bin/java.exe" @('-jar',"$BuildTools/lib/apksigner.jar",'verify','--verbose',$apk)
Checked "$BuildTools/zipalign.exe" @('-c','4',$apk)
Write-Output "Built and verified: $apk"




