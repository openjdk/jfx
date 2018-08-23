choco install ant
choco install vswhere
choco install zip
$env:WINSDK_DIR = (Get-ItemProperty -Path "HKLM:\SOFTWARE\Wow6432Node\Microsoft\Windows Kits\Installed Roots").KitsRoot10
$env:VCINSTALLDIR = "$(vswhere -legacy -latest -property installationPath)\VC\Auxiliary\Build"
$msvcToolsVer = Get-Content "$env:VCINSTALLDIR\Microsoft.VCToolsVersion.default.txt"
if ([string]::IsNullOrWhitespace($msvcToolsVer)) {
  # The MSVC tools version file can have txt *or* props extension.
  $msvcToolsVer = Get-Content "$env:VCINSTALLDIR\Microsoft.VCToolsVersion.default.props"
}
$env:MSVC_VER = $msvcToolsVer
$env:VS150COMNTOOLS = $env:VCINSTALLDIR
$env:VSVARS32FILE = "$env:VCINSTALLDIR\vcvars32.bat"
refreshenv
if ($env:APPVEYOR -eq "true") {
  .\gradlew all test -PCOMPILE_WEBKIT=false -PCONF=DebugNative --stacktrace -x :web:test --info --no-daemon
} else {
  .\gradlew all test -PCOMPILE_WEBKIT=false -PCONF=DebugNative --stacktrace -x :web:test --info
}

