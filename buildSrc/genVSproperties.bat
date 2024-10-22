REM Copyright (c) 2009, 2024, Oracle and/or its affiliates. All rights reserved.
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

setlocal ENABLEDELAYEDEXPANSION

REM Windows bat file that runs vcvarsall.bat for Visual Studio
REM and echoes out a property file with the values of the environment
REM variables we want, e.g. PATH, INCLUDE, LIB, and LIBPATH.

REM Clean out the current settings
set INCLUDE=
set LIB=
set LIBPATH=

REM The current officially supported Visual Studio version is VS 2022

REM Try the following in order of priority:
REM 1. The VSCOMNTOOLS env var
REM 2. The legacy VS150COMNTOOLS env var
REM 3. Look in standard locations for Visual Studio (2022,2019,2017)


set AUXBUILD=VC\Auxiliary\Build

if not "%VSCOMNTOOLS%"=="" (
    set "VSTOOLSDIR=%VSCOMNTOOLS%"
) else if not "%VS150COMNTOOLS%"=="" (
    set "VSTOOLSDIR=%VS150COMNTOOLS%"
) else (
    for %%a in (2022, 2019, 2017) do (
        set year=%%a
        for %%b in (Enterprise, Professional, Community, BuildTools) do (
            set edition=%%b
            for %%c in ("Program Files", "Program Files (x86)") do (
                set ProgramFiles=%%~c
                set "TMPDIR=C:\!ProgramFiles!\Microsoft Visual Studio\!year!\!edition!\%AUXBUILD%"
                if exist "!TMPDIR!" (
                    set "VSTOOLSDIR=!TMPDIR!"
                    goto FOUNDVS
                )
            )
        )
    )
)

:FOUNDVS

if "%VSTOOLSDIR%"=="" exit
if not exist "%VSTOOLSDIR%" exit

call "%VSTOOLSDIR%\vcvarsall.bat" %VCARCH% > NUL

REM Set legacy MSVCDIR variable in case some Makefiles still need it
if "%MSVCDIR%"=="" set MSVCDIR=%VCINSTALLDIR%

REM Echo out a properties file
echo ############################################################
echo # DO NOT EDIT: This is a generated file.
echo windows.vs.DEVENVDIR=%DEVENVDIR%@@ENDOFLINE@@
echo windows.vs.VCINSTALLDIR=%VCINSTALLDIR%@@ENDOFLINE@@
echo windows.vs.VSINSTALLDIR=%VSINSTALLDIR%@@ENDOFLINE@@
echo windows.vs.MSVCDIR=%MSVCDIR%@@ENDOFLINE@@
echo windows.vs.INCLUDE=%INCLUDE%@@ENDOFLINE@@
echo windows.vs.LIB=%LIB%@@ENDOFLINE@@
echo windows.vs.LIBPATH=%LIBPATH%@@ENDOFLINE@@
echo windows.vs.PATH=%PARFAIT_PATH%;%PATH%@@ENDOFLINE@@
echo windows.vs.VC_TOOLS_INSTALL_DIR=%VCToolsInstallDir%@@ENDOFLINE@@
echo windows.vs.VC_TOOLS_REDIST_DIR=%VCToolsRedistDir%@@ENDOFLINE@@
echo WINDOWS_SDK_DIR=%WindowsSdkDir%@@ENDOFLINE@@
echo WINDOWS_SDK_VERSION=%WindowsSDKVersion%@@ENDOFLINE@@
echo ############################################################
