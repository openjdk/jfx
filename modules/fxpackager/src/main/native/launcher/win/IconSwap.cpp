/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

// iconswap.cpp : Defines the entry point for the console application.
//

//Define Windows compatibility requirements
//XP or later
#define WINVER 0x0501
#define _WIN32_WINNT 0x0501

#include <tchar.h>
#include <stdio.h>
#include <windows.h>
#include <stdlib.h>
#include <iostream>
#include <malloc.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <io.h>
#include <strsafe.h>

// http://msdn.microsoft.com/en-us/library/ms997538.aspx

typedef struct _ICONDIRENTRY {
    BYTE bWidth;
    BYTE bHeight;
    BYTE bColorCount;
    BYTE bReserved;
    WORD wPlanes;
    WORD wBitCount;
    DWORD dwBytesInRes;
    DWORD dwImageOffset;
} ICONDIRENTRY, * LPICONDIRENTRY;

typedef struct _ICONDIR {
    WORD idReserved;
    WORD idType;
    WORD idCount;
    ICONDIRENTRY idEntries[1];
} ICONDIR, * LPICONDIR;

// #pragmas are used here to insure that the structure's
// packing in memory matches the packing of the EXE or DLL.
#pragma pack(push)
#pragma pack(2)
typedef struct _GRPICONDIRENTRY {
    BYTE bWidth;
    BYTE bHeight;
    BYTE bColorCount;
    BYTE bReserved;
    WORD wPlanes;
    WORD wBitCount;
    DWORD dwBytesInRes;
    WORD nID;
} GRPICONDIRENTRY, * LPGRPICONDIRENTRY;
#pragma pack(pop)

#pragma pack(push)
#pragma pack(2)
typedef struct _GRPICONDIR {
    WORD idReserved;
    WORD idType;
    WORD idCount;
    GRPICONDIRENTRY idEntries[1];
} GRPICONDIR, * LPGRPICONDIR;
#pragma pack(pop)

void PrintError()
{
    LPVOID message;
    DWORD error = GetLastError();

    FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM |
        FORMAT_MESSAGE_IGNORE_INSERTS, NULL, error,
        MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
        (LPTSTR) &message, 0, NULL);

    wprintf(L"%s\n", message);
    LocalFree(message);
}

bool ChangeIcon(_TCHAR* iconFileName, _TCHAR* executableFileName)
{
    bool result = false;

    DWORD dwData = 1;
    WORD language = MAKELANGID(LANG_ENGLISH, SUBLANG_DEFAULT);

    _TCHAR* iconExtension = wcsrchr(iconFileName, '.');
    if (iconExtension == NULL || wcscmp(iconExtension, L".ico") != 0) {
        wprintf(L"Unknown icon format - please provide .ICO file.\n");
        return result;
    }

    HANDLE icon = CreateFile(iconFileName, GENERIC_READ, 0, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    if (icon == INVALID_HANDLE_VALUE) {
        PrintError();
        return result;
    }

    // Reading .ICO file
    LPICONDIR lpid = (LPICONDIR)malloc(sizeof(ICONDIR));

    DWORD dwBytesRead;
    ReadFile(icon, &lpid->idReserved, sizeof(WORD), &dwBytesRead, NULL);
    ReadFile(icon, &lpid->idType, sizeof(WORD), &dwBytesRead, NULL);
    ReadFile(icon, &lpid->idCount, sizeof(WORD), &dwBytesRead, NULL);

    lpid = (LPICONDIR)realloc(lpid, (sizeof(WORD) * 3) + (sizeof(ICONDIRENTRY) * lpid->idCount));

    ReadFile(icon, &lpid->idEntries[0], sizeof(ICONDIRENTRY) * lpid->idCount, &dwBytesRead, NULL);
    LPGRPICONDIR lpgid;
    lpgid = (LPGRPICONDIR)malloc(sizeof(GRPICONDIR));

    lpgid->idReserved = lpid->idReserved;
    lpgid->idType = lpid->idType;
    lpgid->idCount = lpid->idCount;
    lpgid = (LPGRPICONDIR)realloc(lpgid, (sizeof(WORD) * 3) + (sizeof(GRPICONDIRENTRY) * lpgid->idCount));

    for(int i = 0; i < lpgid->idCount; i++)
    {
        lpgid->idEntries[i].bWidth = lpid->idEntries[i].bWidth;
        lpgid->idEntries[i].bHeight = lpid->idEntries[i].bHeight;
        lpgid->idEntries[i].bColorCount = lpid->idEntries[i].bColorCount;
        lpgid->idEntries[i].bReserved = lpid->idEntries[i].bReserved;
        lpgid->idEntries[i].wPlanes = lpid->idEntries[i].wPlanes;
        lpgid->idEntries[i].wBitCount = lpid->idEntries[i].wBitCount;
        lpgid->idEntries[i].dwBytesInRes = lpid->idEntries[i].dwBytesInRes;
        lpgid->idEntries[i].nID = i + 1;
    }

    // Store images in .EXE
    HANDLE update = BeginUpdateResource( executableFileName, FALSE );
    if (update == NULL) {
        PrintError();
        return result;
    }

    for(int i = 0; i < lpid->idCount; i++)
    {
        LPBYTE lpBuffer = (LPBYTE)malloc(lpid->idEntries[i].dwBytesInRes);
        SetFilePointer(icon, lpid->idEntries[i].dwImageOffset, NULL, FILE_BEGIN);
        ReadFile(icon, lpBuffer, lpid->idEntries[i].dwBytesInRes, &dwBytesRead, NULL);
        if (!UpdateResource(update, RT_ICON, MAKEINTRESOURCE(lpgid->idEntries[i].nID),
                           language, &lpBuffer[0], lpid->idEntries[i].dwBytesInRes))
        {
            PrintError();
            return result;
        }
        free(lpBuffer);
    }
    CloseHandle(icon);
    if (!UpdateResource(update, RT_GROUP_ICON,  MAKEINTRESOURCE(1), language,
                        &lpgid[0], (sizeof(WORD) * 3) + (sizeof(GRPICONDIRENTRY) * lpgid->idCount)))
    {
        PrintError();
        return result;
    }

    if (EndUpdateResource(update, FALSE) == FALSE) {
        PrintError();
        return result;
    }

    result = true;
    return result;
}

int _tmain(int argc, _TCHAR* argv[])
{
	bool printUsage = true;

	if (argc == 3)
	{
		wprintf(L"Icon File Name: %s\n", argv[1]);
		wprintf(L"Executable File Name: %s\n", argv[2]);

		if (ChangeIcon(argv[1], argv[2]) == true)
			printUsage = false;
		else
			printf("failed\n");
	}

	if (printUsage == true)
	{
		printf("Usage: iconswap.exe [Icon File Name] [Executable File Name]\n");
	}

	return 0;
}
