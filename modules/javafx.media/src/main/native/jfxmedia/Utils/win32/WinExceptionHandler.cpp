/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include <Windows.h>
#include <string.h>
#include <Strsafe.h>

#include <gst/gst.h>

#include <Utils/win32/WinExceptionHandler.h>

#define BUFFER_SIZE 1024

static bool isFile = false;
static bool isConsole = false;
static bool disableDefaultHandler = false;

void LogMessage(HANDLE hFile, char *buffer)
{
    DWORD written = 0;

    if (isFile && hFile != INVALID_HANDLE_VALUE)
        WriteFile(hFile, buffer, strlen(buffer), &written, NULL);

    if (isConsole)
        g_print(buffer);
}

HANDLE GetFileHandle()
{
    if (!isFile)
        return INVALID_HANDLE_VALUE;

    TCHAR buffer[MAX_PATH] = {0};
    HANDLE hFile = INVALID_HANDLE_VALUE;
    DWORD pid = GetCurrentProcessId();

    HRESULT hr = StringCchPrintf(buffer, MAX_PATH, "jfxm_err_pid%d.log", pid);
    if (FAILED(hr))
        return INVALID_HANDLE_VALUE;

    return CreateFile(buffer, GENERIC_WRITE, 0, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
}

LONG WINAPI JFXMUnhandledExceptionFilter(struct _EXCEPTION_POINTERS *ExceptionInfo)
{
    HANDLE hFile = INVALID_HANDLE_VALUE;
    char buffer[BUFFER_SIZE] = {0};
    HRESULT hr = S_OK;

    hFile = GetFileHandle();

    LogMessage(hFile, "#\n");
    LogMessage(hFile, "# A fatal error has been detected by the JavaFX Media Runtime:\n");
    LogMessage(hFile, "#\n");

    if (ExceptionInfo && ExceptionInfo->ExceptionRecord)
    {
        hr = StringCchPrintf(buffer, BUFFER_SIZE, "# Exception Code: 0x%X Exception Address: 0x%p\n", ExceptionInfo->ExceptionRecord->ExceptionCode, ExceptionInfo->ExceptionRecord->ExceptionAddress);
        if (SUCCEEDED(hr))
        {
            LogMessage(hFile, buffer);
        }
    }

    if (hFile != INVALID_HANDLE_VALUE)
        CloseHandle(hFile);

    if (disableDefaultHandler)
        return EXCEPTION_EXECUTE_HANDLER;

    return EXCEPTION_CONTINUE_SEARCH;
}

bool GetSettings(char *var)
{
    TCHAR buffer[MAX_PATH] = {0};

    DWORD dwRet = GetEnvironmentVariable(var, buffer, MAX_PATH);
    if (dwRet == 1 && buffer[0] == '1')
        return true;

    return false;
}

void SetExceptionHandler()
{
    isFile = GetSettings("JFXM_EXCEPTION_HANDLER_FILE");
    isConsole = GetSettings("JFXM_EXCEPTION_HANDLER_CONSOLE");

    if (isFile || isConsole)
    {
        disableDefaultHandler = GetSettings("JFXM_EXCEPTION_DISABLE_DEFAULT_HANDLER");
        SetUnhandledExceptionFilter(JFXMUnhandledExceptionFilter);
    }
}
