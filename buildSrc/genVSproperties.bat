REM Copyright (c) 2009, 2017, Oracle and/or its affiliates. All rights reserved.
REM DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
REM
REM This code is free software; you can redistribute it and/or modify it
REM under the terms of the GNU General Public License version 2 only, as
REM published by the Free Software Foundation.  Oracle designates this
REM particular file as subject to the "Classpath" exception as provided
REM by Oracle in the LICENSE file that accompanied this code.
REM
REM This code is distributed in the hope that it will be useful, but WITHOUT
REM ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
REM FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
REM version 2 for more details (a copy is included in the LICENSE file that
REM accompanied this code).
REM
REM You should have received a copy of the GNU General Public License version
REM 2 along with this work; if not, write to the Free Software Foundation,
REM Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
REM
REM Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
REM or visit www.oracle.com if you need additional information or have any
REM questions.

REM Windows bat file that runs vcvars32.bat for Visual Studio 2003
REM   and echos out a property file with the values of the environment
REM   variables we want, e.g. PATH, INCLUDE, LIB, and LIBPATH.

REM Clean out the current settings
set INCLUDE=
set LIB=
set LIBPATH=

REM Run the vsvars32.bat (12.0) / vcvars32.bat (15.0) file, sending it's output to neverland.
REM The current officially supported Visual Studio version is 15.0.
REM Handling of 11.0 and 14.0 is excluded here.
REM The previous officially supported VS version was 12.0
REM So, the search order is 150, then 120, then 100
set VSVER=150
set "VSVARS32FILE=C:\Program Files (x86)\Microsoft Visual Studio\2017\Professional\VC\Auxiliary\Build\vcvars32.bat"
if not "%VS150COMNTOOLS%"=="" (
    set "VS150COMNTOOLS=%VS150COMNTOOLS%"
) else (
  if exist "%VSVARS32FILE%" set "VS150COMNTOOLS=C:\Program Files (x86)\Microsoft Visual Studio\2017\Professional\VC\Auxiliary\Build"
)
set VSVARSDIR=%VS150COMNTOOLS%
if "%VSVARSDIR%"=="" set VSVER=120
if "%VSVARSDIR%"=="" set VSVARSDIR=%VS120COMNTOOLS%
if "%VSVARSDIR%"=="" set VSVER=100
if "%VSVARSDIR%"=="" set VSVARSDIR=%VS100COMNTOOLS%

REM We shouldn't depend on VSVARS32 as it's 32-bit only.
REM   However, this var is still used somewhere in FX (e.g.
REM   to build media), so we set it here.

if "%VSVER%"=="100" set VSVARS32=%VSVARSDIR%\vsvars32.bat
if "%VSVER%"=="120" set VSVARS32=%VSVARSDIR%\vsvars32.bat
if "%VSVER%"=="150" set VSVARS32=%VSVARSDIR%\vcvars32.bat
call "%VSVARS32%" > NUL

if "%VSVER%"=="100" set VCVARSALL=%VCINSTALLDIR%\vcvarsall.bat
if "%VSVER%"=="120" set VCVARSALL=%VCINSTALLDIR%\vcvarsall.bat
if "%VSVER%"=="150" set VCVARSALL=%VSVARSDIR%\vcvarsall.bat
call "%VCVARSALL%" %VCARCH% > NUL

REM Some vars are reset by vcvarsall.bat, so save them here.
set TEMPDEVENVDIR=%DEVENVDIR%
set TEMPVCINSTALLDIR=%VCINSTALLDIR%
set TEMPVSINSTALLDIR=%VSINSTALLDIR%
if NOT "%WINSDKPATH%"=="" call "%WINSDKPATH%\Bin\SetEnv.Cmd" %SDKARCH% %CONF% > NUL
set DEVENVDIR=%TEMPDEVENVDIR%
set VSINSTALLDIR=%TEMPVSINSTALLDIR%
set VCINSTALLDIR=%TEMPVCINSTALLDIR%

REM Create some vars that are not set with VS Express 2008
if "%MSVCDIR%"=="" set MSVCDIR=%VCINSTALLDIR%
REM Try using exe, com might be hanging in ssh environment?
REM     set DEVENVCMD=%DEVENVDIR%\devenv.exe
set DEVENVCMD=%DEVENVDIR%\devenv.com

REM Adjust for lack of devenv in express editions.  This needs more work.
REM VCExpress is the correct executable, but cmd line is different...
if not exist "%DEVENVCMD%" set DEVENVCMD=%DEVENVDIR%\VCExpress.exe

REM Echo out a properties file
echo ############################################################
echo # DO NOT EDIT: This is a generated file.
echo windows.vs.DEVENVDIR=%DEVENVDIR%@@ENDOFLINE@@
echo windows.vs.DEVENVCMD=%DEVENVCMD%@@ENDOFLINE@@
echo windows.vs.VCINSTALLDIR=%VCINSTALLDIR%@@ENDOFLINE@@
echo windows.vs.VSINSTALLDIR=%VSINSTALLDIR%@@ENDOFLINE@@
echo windows.vs.MSVCDIR=%MSVCDIR%@@ENDOFLINE@@
echo windows.vs.INCLUDE=%INCLUDE%@@ENDOFLINE@@
echo windows.vs.LIB=%LIB%@@ENDOFLINE@@
echo windows.vs.LIBPATH=%LIBPATH%@@ENDOFLINE@@
echo windows.vs.PATH=%PARFAIT_PATH%;%PATH%@@ENDOFLINE@@
echo windows.vs.VER=%VSVER%@@ENDOFLINE@@
echo windows.vs.VC_TOOLS_INSTALL_DIR=%VCToolsInstallDir%@@ENDOFLINE@@
echo windows.vs.VC_TOOLS_REDIST_DIR=%VCToolsRedistDir%@@ENDOFLINE@@
echo WINDOWS_SDK_DIR=%WindowsSdkDir%@@ENDOFLINE@@
echo WINDOWS_SDK_VERSION=%WindowsSDKVersion%@@ENDOFLINE@@
echo ############################################################
