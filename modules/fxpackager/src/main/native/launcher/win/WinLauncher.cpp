/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


#include <Windows.h>
#include <Shellapi.h>
#include <locale.h>
#include <tchar.h>
#include <string>

#define PACKAGER_LIBRARY TEXT("packager.dll")

typedef bool (*start_launcher)(int argc, TCHAR* argv[]);
typedef void (*stop_launcher)();

std::wstring GetTitle() {
    std::wstring result;
    wchar_t buffer[MAX_PATH];
    GetModuleFileName(NULL, buffer, MAX_PATH - 1);
    buffer[MAX_PATH - 1] = '\0';
    result = buffer;
    size_t slash = result.find_last_of('\\');
    
    if (slash != std::wstring::npos)
        result = result.substr(slash + 1, result.size() - slash - 1);
    
    return result;
}

int APIENTRY _tWinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance,
                       LPTSTR lpCmdLine, int nCmdShow) {
    int result = 1;
    TCHAR **argv;
    int argc;

    // [RT-31061] otherwise UI can be left in back of other windows.
    ::AllowSetForegroundWindow(ASFW_ANY);

    ::setlocale(LC_ALL, "en_US.utf8");
    argv = CommandLineToArgvW(GetCommandLine(), &argc);

    HMODULE library = ::LoadLibrary(PACKAGER_LIBRARY);
    
    if (library == NULL) {
        std::wstring title = GetTitle();
        std::wstring description = std::wstring(PACKAGER_LIBRARY) + std::wstring(TEXT(" not found."));
        MessageBox(NULL, description.data(), title.data(), MB_ICONERROR | MB_OK);
    }
    else {
        start_launcher start = (start_launcher)GetProcAddress(library, "start_launcher");
        stop_launcher stop = (stop_launcher)GetProcAddress(library, "stop_launcher");

        if (start(argc, argv) == true) {
            result = 0;

            if (stop != NULL) {
                stop();
            }
        }

        ::FreeLibrary(library);
    }

    if (argv != NULL) {
        LocalFree(argv);
    }

    return result;
}

