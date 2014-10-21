/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
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


#include "FilePath.h"

#include <algorithm>
#include <list>

#ifdef POSIX
#include <sys/stat.h>
#endif //POSIX


bool FilePath::FileExists(const TString FileName) {
    bool result = false;
#ifdef WINDOWS
    WIN32_FIND_DATA FindFileData;
    HANDLE handle = FindFirstFile(FileName.c_str(), &FindFileData);

    if (handle != INVALID_HANDLE_VALUE) {
        if (FILE_ATTRIBUTE_DIRECTORY & FindFileData.dwFileAttributes) {
            result = true;
        }
        else {
            result = true;
        }

        FindClose(handle);
    }
#endif //WINDOWS
#ifdef POSIX
    struct stat buf;

    if ((stat(PlatformString(FileName), &buf) == 0) && (S_ISREG(buf.st_mode) != 0)) {
        result = true;
    }
#endif //POSIX
    return result;
}

bool FilePath::DirectoryExists(const TString DirectoryName) {
    bool result = false;
#ifdef WINDOWS
    WIN32_FIND_DATA FindFileData;
    HANDLE handle = FindFirstFile(DirectoryName.c_str(), &FindFileData);

    if (handle != INVALID_HANDLE_VALUE) {
        if (FILE_ATTRIBUTE_DIRECTORY & FindFileData.dwFileAttributes) {
            result = true;
        }

        FindClose(handle);
    }
#endif //WINDOWS
#ifdef POSIX
    struct stat buf;

    if ((stat(PlatformString(DirectoryName), &buf) == 0) && (S_ISDIR(buf.st_mode) != 0)) {
        result = true;
    }
#endif //POSIX
    return result;
}

TString FilePath::IncludeTrailingSlash(const TString value) {
    TString result = value;
    TString::iterator i = result.end();
    i--;

    if (*i != TRAILING_SLASH) {
        result += TRAILING_SLASH;
    }

    return result;
}

TString FilePath::IncludeTrailingSlash(const char* value) {
    TString lvalue = PlatformString(value).toString();
    return IncludeTrailingSlash(lvalue);
}

TString FilePath::IncludeTrailingSlash(const wchar_t* value) {
    TString lvalue = PlatformString(value).toString();
    return IncludeTrailingSlash(lvalue);
}

TString FilePath::ExtractFilePath(TString Path) {
#ifdef WINDOWS
    TString result;
    size_t slash = Path.find_last_of(TRAILING_SLASH);
    if (slash != TString::npos)
        result = Path.substr(0, slash);
    return result;
#endif //WINDOWS
#ifdef POSIX
    return dirname(PlatformString(Path));
#endif //POSIX
}

TString FilePath::ExtractFileExt(TString Path) {
    TString result;
    size_t dot = Path.find_last_of('.');

    if (dot != TString::npos) {
        result  = Path.substr(dot, Path.size() - dot);
    }

    return result;
}

TString FilePath::ExtractFileName(TString Path) {
#ifdef WINDOWS
    TString result;

    size_t slash = Path.find_last_of(TRAILING_SLASH);
    if (slash != TString::npos)
        result = Path.substr(slash + 1, Path.size() - slash - 1);

    return result;
#endif // WINDOWS
#ifdef POSIX
    return basename(PlatformString(Path));
#endif //POSIX
}

TString FilePath::ChangeFileExt(TString Path, TString Extension) {
    TString result;
    size_t dot = Path.find_last_of('.');

    if (dot != TString::npos) {
        result = Path.substr(0, dot) + Extension;
    }

    if (result.empty() == true) {
        result = Path;
    }

    return result;
}

TString FilePath::FixPathSeparatorForPlatform(TString Path) {
    TString result = Path;
    std::replace(result.begin(), result.end(), ' ', PATH_SEPARATOR);
    std::replace(result.begin(), result.end(), BAD_PATH_SEPARATOR, PATH_SEPARATOR);
    return result;
}

TString FilePath::PathSeparator() {
    TString result;
    result = PATH_SEPARATOR;
    return result;
}

bool FilePath::CreateDirectory(TString Path) {
    bool result = false;

    std::list<TString> paths;
    TString lpath = Path;

    while (lpath.empty() == false && DirectoryExists(lpath) == false) {
        paths.push_front(lpath);
        lpath = ExtractFilePath(lpath);
    }

    for (std::list<TString>::iterator iterator = paths.begin(); iterator != paths.end(); iterator++) {
        lpath = *iterator;

#ifdef WINDOWS
        if (_wmkdir(PlatformString(lpath)) == 0) {
#endif // WINDOWS
#ifdef POSIX
        //TODO Is this the correct permissions?
        if (mkdir(PlatformString(lpath), S_IRWXU | S_IRWXG | S_IROTH | S_IXOTH) == 0) {
#endif //POSIX
            result = true;
        }
        else {
            result = false;
            break;
        }
    }

    return result;
}
