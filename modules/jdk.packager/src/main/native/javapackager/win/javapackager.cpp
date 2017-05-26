/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include <stdlib.h>
#include <string>
#include <windows.h>

#include "IconSwap.h"
#include "VersionInfoSwap.h"

//#define _DEBUG

#ifdef _DEBUG
#include <iostream>
#include <sstream>
#endif

using namespace std;

#define MAX_KEY_LENGTH 255
#define MAX_VALUE_NAME 16383

bool from_string(int &result, string &str) {
    const char *p = str.c_str();
    int res = 0;
    for (int index = 0;; index++) {
        char c = str[index];
        if (c == 0 && index > 0) {
            result = res;
            return true;
        }
        if (c < '0' || c > '9')
            return false;
        res = res * 10 + (c - '0');
    }
}

void PrintCSBackupAPIErrorMessage(DWORD dwErr) {

    char wszMsgBuff[512]; // Buffer for text.

    DWORD dwChars; // Number of chars returned.

    // Try to get the message from the system errors.
    dwChars = FormatMessageA(FORMAT_MESSAGE_FROM_SYSTEM |
            FORMAT_MESSAGE_IGNORE_INSERTS,
            NULL,
            dwErr,
            0,
            wszMsgBuff,
            512,
            NULL);

    if (0 == dwChars) {
        // The error code did not exist in the system errors.
        // Try ntdsbmsg.dll for the error code.

        HINSTANCE hInst;

        // Load the library.
        hInst = LoadLibraryA("ntdsbmsg.dll");
        if (NULL == hInst) {
#ifdef _DEBUG
            cerr << "cannot load ntdsbmsg.dll\n";
#endif
            return;
        }

        // Try getting message text from ntdsbmsg.
        dwChars = FormatMessageA(FORMAT_MESSAGE_FROM_HMODULE |
                FORMAT_MESSAGE_IGNORE_INSERTS,
                hInst,
                dwErr,
                0,
                wszMsgBuff,
                512,
                NULL);

        // Free the library.
        FreeLibrary(hInst);
    }

    // Display the error message, or generic text if not found.
#ifdef _DEBUG
    cerr << "Error value: " << dwErr << " Message: " << ((dwChars > 0) ? wszMsgBuff : "Error message not found.") << endl;
#endif
}

class JavaVersion {
public:
    int v1;
    int v2;
    int v3;
    std::wstring home;
    std::wstring path;

    JavaVersion(int pv1, int pv2, int pv3) {
        v1 = pv1;
        v2 = pv2;
        v3 = pv3;
    }

    bool operator>(const JavaVersion &other) const {
        if (v1 > other.v1)
            return true;
        if (v1 == other.v1) {
            if (v2 > other.v2)
                return true;
            if (v2 == other.v2)
                return v3 > other.v3;
        }
        return false;
    }

    bool operator>=(const JavaVersion &other) const {
        if (v1 > other.v1)
            return true;
        if (v1 == other.v1) {
            if (v2 > other.v2)
                return true;
            if (v2 == other.v2)
                return v3 >= other.v3;
        }
        return false;
    }

    bool operator<(const JavaVersion &other) const {
        if (v1 < other.v1)
            return true;
        if (v1 == other.v1) {
            if (v2 < other.v2)
                return true;
            if (v2 == other.v2)
                return v3 < other.v3;
        }
        return false;
    }
};

class EnvironmentVariable {
private:
    std::wstring FValue;

public:
    EnvironmentVariable(std::wstring Name) {
        wchar_t* value;
        size_t requiredSize;

        _wgetenv_s(&requiredSize, NULL, 0, Name.data());

        if (requiredSize != 0) {
            value = (wchar_t*)malloc(requiredSize * sizeof(wchar_t));
            if (value)
            {
                // Get the value of the LIB environment variable.
                _wgetenv_s(&requiredSize, value, requiredSize, Name.data());
                FValue = value;
            }
        }
    }

    std::wstring get() {
        return FValue;
    }

    bool exists() {
        return !FValue.empty();
    }
};

bool checkJavaHome(HKEY key, const char * sKey, const char * jv, JavaVersion *version) {
    char p[MAX_KEY_LENGTH];
    HKEY hKey;
    bool result = false;
    int res;

    strcpy_s(p, MAX_KEY_LENGTH, sKey);
    strcat_s(p, MAX_KEY_LENGTH - strlen(p), "\\");
    strcat_s(p, MAX_KEY_LENGTH - strlen(p), jv);

    if (RegOpenKeyExA(key,
            p,
            0,
            KEY_READ,
            &hKey) == ERROR_SUCCESS
            ) {
        DWORD ot = REG_SZ;
        DWORD size = 255;
        wchar_t data[MAX_PATH] = { 0 };
        if ((res = RegQueryValueEx(hKey, L"JavaHome", NULL, &ot, (BYTE *)data, &size)) == ERROR_SUCCESS) {
            version->home = data;
            std::wstring ldata = std::wstring(data) + L"\\bin\\java.exe";
            version->path = data;
            result = GetFileAttributes(data) != 0xFFFFFFFF;
        }
        else {
            PrintCSBackupAPIErrorMessage(res);
            result = false;
        }
        RegCloseKey(hKey);
    }
    else {
#ifdef _DEBUG
        cerr << "Can not open registry key" << endl;
#endif
        result = false;
    }

    return result;
}

JavaVersion * parseName(const char * jName) {
    string s(jName);

    if (s.length() == 0) {
        return NULL;
    }

    string n;
    string::size_type pos;

    pos = s.find_first_of(".");
    if (pos != string::npos) {
        n = s.substr(0, pos);
        s = s.substr(pos + 1);
    }
    else {
        n = s;
        s = "";
    }

    int v1 = 0;

    if (n.length() > 0) {
        if (!from_string(v1, n))
            return NULL;
    }


    pos = s.find_first_of(".");
    if (pos != string::npos) {
        n = s.substr(0, pos);
        s = s.substr(pos + 1);
    }
    else {
        n = s;
        s = "";
    }

    int v2 = 0;

    if (n.length() > 0) {
        if (!from_string(v2, n))
            return NULL;
    }


    unsigned int nn = s.length();
    for (unsigned int i = 0; i < s.length(); i++) {
        string c = s.substr(i, 1);
        int tmp;
        if (!from_string(tmp, c)) {
            nn = i;
            break;
        }
    }

    n = s.substr(0, nn);
    if (nn < s.length()) {
        s = s.substr(nn + 1);
    }
    else s = "";

    int v3 = 0;

    if (n.length() > 0) {
        if (!from_string(v3, n))
            v3 = 0;
    }

    int v4 = 0;

    //update version
    if (s.length() > 0) {
        nn = s.length();
        for (unsigned int i = 0; i < s.length(); i++) {
            string c = s.substr(i, 1);
            int tmp;
            if (!from_string(tmp, c)) {
                nn = i;
                break;
            }
        }

        n = s.substr(0, nn);

        if (n.length() > 0) {
            if (!from_string(v4, n))
                v4 = 0;
        }
    }

    return new JavaVersion(v2, v3, v4);
}

JavaVersion * GetMaxVersion(HKEY key, const char * sKey) {
    HKEY hKey;
    JavaVersion * result = NULL;

    if (RegOpenKeyExA(key,
            sKey,
            0,
            KEY_READ,
            &hKey) == ERROR_SUCCESS
            ) {
        DWORD retCode;
        char achClass[MAX_PATH]; // buffer for class name
        DWORD cchClassName = MAX_PATH; // size of class string


        DWORD cchValue = MAX_VALUE_NAME;
        DWORD cSubKeys = 0; // number of subkeys
        DWORD cbMaxSubKey; // longest subkey size
        DWORD cchMaxClass; // longest class string
        DWORD cValues; // number of values for key
        DWORD cchMaxValue; // longest value name
        DWORD cbMaxValueData; // longest value data
        DWORD cbSecurityDescriptor; // size of security descriptor
        FILETIME ftLastWriteTime; // last write time

        retCode = RegQueryInfoKeyA(
                hKey, // key handle
                achClass, // buffer for class name
                &cchClassName, // size of class string
                NULL, // reserved
                &cSubKeys, // number of subkeys
                &cbMaxSubKey, // longest subkey size
                &cchMaxClass, // longest class string
                &cValues, // number of values for this key
                &cchMaxValue, // longest value name
                &cbMaxValueData, // longest value data
                &cbSecurityDescriptor, // security descriptor
                &ftLastWriteTime); // last write time

        if (cSubKeys) {
            for (unsigned int i = 0; i < cSubKeys; i++) {
                char achKey[MAX_KEY_LENGTH]; // buffer for subkey name
                DWORD cbName = MAX_KEY_LENGTH;
                retCode = RegEnumKeyExA(hKey, i,
                        achKey,
                        &cbName,
                        NULL,
                        NULL,
                        NULL,
                        &ftLastWriteTime);

                if (retCode == ERROR_SUCCESS) {
#ifdef _DEBUG
                    cout << achKey << endl;
#endif
                    JavaVersion * nv = parseName(achKey);

                    bool isHome = checkJavaHome(key, sKey, achKey, nv);
#ifdef _DEBUG
                    wcout << nv->home << " " << isHome << endl;
#endif

                    if (isHome)
                    if (result == NULL) {
                        result = nv;
#ifdef _DEBUG
                        cout << "NEW" << endl;
#endif
                    }
                    else {
                        if (nv != NULL) {
                            if (*nv > *result) {
#ifdef _DEBUG
                                cout << "REPLACE" << endl;
#endif
                                delete result;
                                result = nv;
                            }
                            else {
#ifdef _DEBUG
                                cout << "NO" << endl;
#endif
                                delete nv;
                            }
                        }
                    }
                }
            }
        }

        RegCloseKey(hKey);
    }

    return result;
}
// *****************************************************************************

int fileExists(const std::wstring& path) {
    WIN32_FIND_DATA ffd;
    HANDLE hFind;

    hFind = FindFirstFile(path.data(), &ffd);
    if (hFind == INVALID_HANDLE_VALUE)
        return FALSE;

    FindClose(hFind);
    return (ffd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) == 0;
}

bool hasEnding(std::wstring const &fullString, std::wstring const &ending) {
    if (fullString.length() >= ending.length()) {
        return (0 == fullString.compare(fullString.length() - ending.length(), ending.length(), ending));
    }
    else {
        return false;
    }
}

#define TRAILING_PATHSEPARATOR '\\'

std::wstring ExtractFilePath(std::wstring Path) {
    std::wstring result;
    size_t slash = Path.find_last_of(TRAILING_PATHSEPARATOR);
    if (slash != std::wstring::npos)
        result = Path.substr(0, slash);
    return result;
}

std::wstring GetCurrentExecutableName() {
    TCHAR FileName[MAX_PATH];
    GetModuleFileName(NULL, FileName, MAX_PATH);
    return FileName;
}

int wmain(int argc, wchar_t* argv[]) {
    wchar_t buf[MAX_PATH];
    GetModuleFileName(NULL, buf, MAX_PATH);
    std::wstring javafxhome = buf;

    javafxhome.erase(javafxhome.rfind(L"\\"));

    std::wstring fxlib = javafxhome + L"\\..\\lib\\";

    std::wstring javacmd;
    std::wstring javahome;

    std::wstring exe = GetCurrentExecutableName();

    if (exe.length() <= 0) {
        JavaVersion * jv2 = GetMaxVersion(HKEY_LOCAL_MACHINE, "SOFTWARE\\JavaSoft\\Java Development Kit");
        if (jv2 != NULL) {
            javacmd = jv2->path;
            javahome = jv2->home;
        }
        else {
            javacmd = L"java.exe";
        }
    } else {
        javacmd = ExtractFilePath(exe) + L"\\java.exe";
    }

    std::wstring cmd = L"\"" + javacmd + L"\"";
    if (javahome.length() > 0) {
        SetEnvironmentVariable(L"JAVA_HOME", javahome.c_str());
    }

    std::wstring memory = L"-Xmx512M";
    std::wstring debug = L"";
    std::wstring args = L"";

    for (int i = 1; i < argc; i++) {
        std::wstring argument = argv[i];
        std::wstring debug_arg = L"-J-Xdebug:";
        std::wstring icon_swap_arg = L"--icon-swap";
        std::wstring version_swap_arg = L"--version-swap";

        if (argument.find(L"-J-Xmx", 0) == 0) {
            memory = argument.substr(2, argument.length() - 2);
        }
        else if (argument.find(debug_arg, 0) == 0) {
            std::wstring address = argument.substr(debug_arg.length(), argument.length() - debug_arg.length());
            debug = L"-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + address;
        }
        else if (argument.find(icon_swap_arg, 0) == 0) {
            if (argc != 4) {
                fwprintf(stderr, TEXT("Usage: javapackager.exe --icon-swap [Icon File Name] [Executable File Name]\n"));
                return 1;
            }

            wprintf(L"Icon File Name: %s\n", argv[i + 1]);
            wprintf(L"Executable File Name: %s\n", argv[i + 2]);

            if (ChangeIcon(argv[i + 1], argv[i + 2]) == true) {
                return 0;
            }
            else {
                fwprintf(stderr, TEXT("failed\n"));
                return 1;
            }
        }
        else if (argument.find(version_swap_arg, 0) == 0) {
            if (argc != 4) {
                fwprintf(stderr, TEXT("Usage: javapackager.exe --version-swap [Property File Name] [Executable File Name]\n"));
                return 1;
            }

            fwprintf(stdout, TEXT("Resource File Name: %s\n"), argv[i + 1]);
            fwprintf(stdout, TEXT("Executable File Name: %s\n"), argv[i + 2]);

            VersionInfoSwap vs(argv[i + 1], argv[i + 2]);

            if (vs.PatchExecutable()) {
                return 0;
            }
            else {
                fwprintf(stderr, TEXT("failed\n"));
                return 1;
            }
        }
        else {
            args = args + L" \"" + argv[i] + L"\"";
        }
    }


    cmd += debug +
        L" " + memory +
        L" -Djavafx.home=\"" + javafxhome + L"\"" +
        L" -classpath \"" + fxlib + L"ant-javafx.jar\"" +
        L" -m jdk.packager/com.sun.javafx.tools.packager.Main" +
        L" " + args;

#ifdef _DEBUG
    printf ("%s", cmd.c_str());
#endif

    STARTUPINFO start;
    PROCESS_INFORMATION pi;
    memset(&start, 0, sizeof (start));
    start.cb = sizeof(start);

    if (!CreateProcess(NULL, (wchar_t *) cmd.data(),
            NULL, NULL, TRUE, NORMAL_PRIORITY_CLASS, NULL, NULL, &start, &pi)) {
#ifdef _DEBUG
        fprintf(stderr, "Cannot start java.exe");
#endif
        return EXIT_FAILURE;
    }

    WaitForSingleObject(pi.hProcess, INFINITE);
    unsigned long exitCode;
    GetExitCodeProcess(pi.hProcess, &exitCode);

    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);

    return exitCode;
}
