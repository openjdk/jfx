param (
[switch]$nocygwin = $false,
[parameter(ValueFromRemainingArguments)][String[]]$args
)

choco install ant
choco install vswhere
choco install zip

$vsRoot = "$(vswhere -latest -requires Microsoft.VisualStudio.Workload.NativeDesktop -property installationPath)"
if ([string]::IsNullOrWhitespace($vsRoot)) {
  $vs = "$(vswhere -latest -property installationPath)"
  if ([string]::IsNullOrWhitespace($vs)) {
    choco install visualstudio2017community
    choco install visualstudio2017-workload-nativedesktop
  }
  else {
    choco install visualstudio2017-workload-nativedesktop
  }
  $vsRoot = "$(vswhere -latest -requires Microsoft.VisualStudio.Workload.NativeDesktop -property installationPath)"
}

$winSdk = (Get-ItemProperty -Path "HKLM:\SOFTWARE\Wow6432Node\Microsoft\Windows Kits\Installed Roots").KitsRoot10 2>$null
if ([string]::IsNullOrWhitespace($winSdk)) {
  choco install windows-sdk-7.1
  $winSdk = ((Get-ItemProperty -Path "HKLM:\SOFTWARE\Wow6432Node\Microsoft\Windows Kits\Installed Roots" -ErrorAction Stop).KitsRoot10)
}

# Cygwin required for chmod
if ($nocygwin -eq $false) {
  $cygwinPath = (Get-ItemProperty -Path "HKLM:\SOFTWARE\Cygwin\setup").rootdir 2>$null
  if ([string]::IsNullOrWhitespace($cygwinPath)) {
    choco install cygwin
    $cygwinPath = (Get-ItemProperty -Path "HKLM:\SOFTWARE\Cygwin\setup"  -ErrorAction Stop).rootdir
  }
} else {
  Write-Output "Skipping cygwin install!"
}

if ($env:Path -NotLike "*$($cygwinPath)*") {
  $env:Path += ";$($cygwinPath)\bin"
}
$env:WINSDK_DIR = $winSdk
$env:VCINSTALLDIR = "$($vsRoot)\VC\Auxiliary\Build"

$msvcToolsVer = Get-Content "$env:VCINSTALLDIR\Microsoft.VCToolsVersion.default.txt"
$msvcRedistVer = Get-Content "$env:VCINSTALLDIR\Microsoft.VCRedistVersion.default.txt"
if ([string]::IsNullOrWhitespace($msvcToolsVer)) {
  # The MSVC tools version file can have txt *or* props extension.
  $msvcToolsVer = Get-Content "$env:VCINSTALLDIR\Microsoft.VCToolsVersion.default.props"
}
$env:MSVC_VER = $msvcToolsVer
$env:MSVC_REDIST_VER = $msvcRedistVer

$files = Get-ChildItem "$($vsRoot)\VC\Redist\MSVC\$($msvcRedistVer)\x86\*.CRT\"
$env:WINDOWS_CRT_VER = $files[0].Name.replace("Microsoft.VC","").replace(".CRT","")

$env:VS150COMNTOOLS = $env:VCINSTALLDIR
$env:VSVARS32FILE = "$env:VCINSTALLDIR\vcvars32.bat"
refreshenv
if ($env:APPVEYOR -eq "true") {
  .\gradlew all test -PCOMPILE_WEBKIT=false -PCONF=DebugNative --stacktrace -x :web:test --info --no-daemon
  if ($lastexitcode -ne 0) {
    exit $lastexitcode
  }
} else {
  if ($noCygwin) {
    .\gradlew all test -PCOMPILE_WEBKIT=false -PCONF=Debug -PUSE_CYGWIN=false --stacktrace -x :web:test --info --no-daemon $args
  } else {
    .\gradlew all test -PCOMPILE_WEBKIT=false -PCONF=Debug --stacktrace -x :web:test --info --no-daemon $args
  }
}
