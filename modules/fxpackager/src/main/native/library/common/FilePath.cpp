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

#ifdef WINDOWS
#include <ShellAPI.h>
#endif //WINDOWS

#ifdef POSIX
#include <sys/stat.h>
#endif //POSIX


bool FilePath::FileExists(const TString FileName) {
    bool result = false;
#ifdef WINDOWS
    WIN32_FIND_DATA FindFileData;
    HANDLE handle = FindFirstFile(FileName.data(), &FindFileData);

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

    if ((stat(StringToFileSystemString(FileName), &buf) == 0) && (S_ISREG(buf.st_mode) != 0)) {
        result = true;
    }
#endif //POSIX
    return result;
}

bool FilePath::DirectoryExists(const TString DirectoryName) {
    bool result = false;
#ifdef WINDOWS
    WIN32_FIND_DATA FindFileData;
    HANDLE handle = FindFirstFile(DirectoryName.data(), &FindFileData);

    if (handle != INVALID_HANDLE_VALUE) {
        if (FILE_ATTRIBUTE_DIRECTORY & FindFileData.dwFileAttributes) {
            result = true;
        }

        FindClose(handle);
    }
#endif //WINDOWS
#ifdef POSIX
    struct stat buf;

    if ((stat(StringToFileSystemString(DirectoryName), &buf) == 0) && (S_ISDIR(buf.st_mode) != 0)) {
        result = true;
    }
#endif //POSIX
    return result;
}

#ifdef WINDOWS
std::string GetLastErrorAsString() {
    //Get the error message, if any.
    DWORD errorMessageID = ::GetLastError();

    if (errorMessageID == 0) {
        return "No error message has been recorded";
    }

    LPSTR messageBuffer = NULL;
    size_t size = FormatMessageA(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
                                 NULL, errorMessageID, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), (LPSTR)&messageBuffer, 0, NULL);

    std::string message(messageBuffer, size);

    // Free the buffer.
    LocalFree(messageBuffer);

    return message;
}
#endif //WINDOWS

bool FilePath::DeleteFile(const TString FileName) {
    bool result = false;

    if (FileExists(FileName) == true) {
#ifdef WINDOWS
        TString lFileName = FileName;
        FileAttributes attributes(lFileName);

        if (attributes.Contains(faReadOnly) == true) {
            attributes.Remove(faReadOnly);
        }

        result = ::DeleteFile(lFileName.data()) == TRUE;
#endif //WINDOWS
#ifdef POSIX
        if (unlink(StringToFileSystemString(FileName)) == 0) {
            result = true;
        }
#endif //POSIX
    }

    return result;
}

bool FilePath::DeleteDirectory(const TString DirectoryName) {
    bool result = false;

    if (DirectoryExists(DirectoryName) == true) {
#ifdef WINDOWS
        SHFILEOPSTRUCTW fos = {0};
        DynamicBuffer<TCHAR> lDirectoryName(DirectoryName.size() + 2);
        memcpy(lDirectoryName.GetData(), DirectoryName.data(), (DirectoryName.size() + 2) * sizeof(TCHAR));
        lDirectoryName[DirectoryName.size() + 1] = NULL; // Double null terminate for SHFileOperation.

        // Delete the folder and everything inside.
        fos.wFunc = FO_DELETE;
        fos.pFrom = lDirectoryName.GetData();
        fos.fFlags = FOF_NO_UI;
        result = SHFileOperation(&fos) == 0;
#endif //WINDOWS
#ifdef POSIX
        if (unlink(StringToFileSystemString(DirectoryName)) == 0) {
            result = true;
        }
#endif //POSIX
    }

    return result;
}

TString FilePath::IncludeTrailingSeparater(const TString value) {
    TString result = value;

    if (value.size() > 0) {
        TString::iterator i = result.end();
        i--;

        if (*i != TRAILING_PATHSEPARATOR) {
            result += TRAILING_PATHSEPARATOR;
        }
    }

    return result;
}

TString FilePath::IncludeTrailingSeparater(const char* value) {
    TString lvalue = PlatformString(value).toString();
    return IncludeTrailingSeparater(lvalue);
}

TString FilePath::IncludeTrailingSeparater(const wchar_t* value) {
    TString lvalue = PlatformString(value).toString();
    return IncludeTrailingSeparater(lvalue);
}

TString FilePath::ExtractFilePath(TString Path) {
#ifdef WINDOWS
    TString result;
    size_t slash = Path.find_last_of(TRAILING_PATHSEPARATOR);
    if (slash != TString::npos)
        result = Path.substr(0, slash);
    return result;
#endif //WINDOWS
#ifdef POSIX
    return dirname(StringToFileSystemString(Path));
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

    size_t slash = Path.find_last_of(TRAILING_PATHSEPARATOR);
    if (slash != TString::npos)
        result = Path.substr(slash + 1, Path.size() - slash - 1);

    return result;
#endif // WINDOWS
#ifdef POSIX
    return basename(StringToFileSystemString(Path));
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

TString FilePath::FixPathForPlatform(TString Path) {
    TString result = Path;
    std::replace(result.begin(), result.end(), BAD_TRAILING_PATHSEPARATOR, TRAILING_PATHSEPARATOR);
    return result;
}

TString FilePath::FixPathSeparatorForPlatform(TString Path) {
    TString result = Path;
    std::replace(result.begin(), result.end(), BAD_PATH_SEPARATOR, PATH_SEPARATOR);
    return result;
}

TString FilePath::PathSeparator() {
    TString result;
    result = PATH_SEPARATOR;
    return result;
}

bool FilePath::CreateDirectory(TString Path, bool ownerOnly) {
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
        if (_wmkdir(lpath.data()) == 0) {
#endif // WINDOWS
#ifdef POSIX
        mode_t mode = S_IRWXU;
        if (!ownerOnly) {
            mode |= S_IRWXG | S_IROTH | S_IXOTH;
        }
        if (mkdir(StringToFileSystemString(lpath), mode) == 0) {
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

void FilePath::ChangePermissions(TString FileName, bool ownerOnly) {
#ifdef POSIX
    mode_t mode = S_IRWXU;
    if (!ownerOnly) {
        mode |= S_IRWXG | S_IROTH | S_IXOTH;
    }
    chmod(FileName.data(), mode);
#endif // POSIX
}

//--------------------------------------------------------------------------------------------------

#include <algorithm>

FileAttributes::FileAttributes(const TString FileName, bool FollowLink) {
    FFileName = FileName;
    FFollowLink = FollowLink;
    ReadAttributes();
}

bool FileAttributes::WriteAttributes() {
    bool result = false;

#ifdef WINDOWS
    DWORD attributes = 0;

    for (std::vector<FileAttribute>::const_iterator iterator = FAttributes.begin();
         iterator != FAttributes.end(); iterator++) {
        switch (*iterator) {
            case faArchive: {
                attributes = attributes & FILE_ATTRIBUTE_ARCHIVE;
                break;
            }
            case faCompressed: {
                attributes = attributes & FILE_ATTRIBUTE_COMPRESSED;
                break;
            }
            case faDevice: {
                attributes = attributes & FILE_ATTRIBUTE_DEVICE;
                break;
            }
            case faDirectory: {
                attributes = attributes & FILE_ATTRIBUTE_DIRECTORY;
                break;
            }
            case faEncrypted: {
                attributes = attributes & FILE_ATTRIBUTE_ENCRYPTED;
                break;
            }
            case faHidden: {
                attributes = attributes & FILE_ATTRIBUTE_HIDDEN;
                break;
            }
//            case faIntegrityStream: {
//                attributes = attributes & FILE_ATTRIBUTE_INTEGRITY_STREAM;
//                break;
//            }
            case faNormal: {
                attributes = attributes & FILE_ATTRIBUTE_NORMAL;
                break;
            }
            case faNotContentIndexed: {
                attributes = attributes & FILE_ATTRIBUTE_NOT_CONTENT_INDEXED;
                break;
            }
//            case faNoScrubData: {
//                attributes = attributes & FILE_ATTRIBUTE_NO_SCRUB_DATA;
//                break;
//            }
            case faOffline: {
                attributes = attributes & FILE_ATTRIBUTE_OFFLINE;
                break;
            }
            case faSystem: {
                attributes = attributes & FILE_ATTRIBUTE_SYSTEM;
                break;
            }
            case faSymbolicLink: {
                attributes = attributes & FILE_ATTRIBUTE_REPARSE_POINT;
                break;
            }
            case faSparceFile: {
                attributes = attributes & FILE_ATTRIBUTE_SPARSE_FILE;
                break;
            }
            case faReadOnly: {
                attributes = attributes & FILE_ATTRIBUTE_READONLY;
                break;
            }
            case faTemporary: {
                attributes = attributes & FILE_ATTRIBUTE_TEMPORARY;
                break;
            }
            case faVirtual: {
                attributes = attributes & FILE_ATTRIBUTE_VIRTUAL;
                break;
            }
        }
    }

    if (::SetFileAttributes(FFileName.data(), attributes) != 0) {
        result = true;
    }
#endif // WINDOWS
#ifdef POSIX
    mode_t attributes = 0;

    for (std::vector<FileAttribute>::const_iterator iterator = FAttributes.begin();
         iterator != FAttributes.end(); iterator++) {
        switch (*iterator) {
            case faBlockSpecial: {
                attributes |= S_IFBLK;
                break;
            }
            case faCharacterSpecial: {
                attributes |= S_IFCHR;
                break;
            }
            case faFIFOSpecial: {
                attributes |= S_IFIFO;
                break;
            }
            case faNormal: {
                attributes |= S_IFREG;
                break;
            }
            case faDirectory: {
                attributes |= S_IFDIR;
                break;
            }
            case faSymbolicLink: {
                attributes |= S_IFLNK;
                break;
            }
            case faSocket: {
                attributes |= S_IFSOCK;
                break;
            }

            // Owner
            case faReadOnly: {
                attributes |= S_IRUSR;
                break;
            }
            case  faWriteOnly: {
                attributes |= S_IWUSR;
                break;
            }
            case faReadWrite: {
                attributes |= S_IRUSR;
                attributes |= S_IWUSR;
                break;
            }
            case faExecute: {
                attributes |= S_IXUSR;
                break;
            }

            // Group
            case faGroupReadOnly: {
                attributes |= S_IRGRP;
                break;
            }
            case  faGroupWriteOnly: {
                attributes |= S_IWGRP;
                break;
            }
            case faGroupReadWrite: {
                attributes |= S_IRGRP;
                attributes |= S_IWGRP;
                break;
            }
            case faGroupExecute: {
                attributes |= S_IXGRP;
                break;
            }

            // Others
            case faOthersReadOnly: {
                attributes |= S_IROTH;
                break;
            }
            case  faOthersWriteOnly: {
                attributes |= S_IWOTH;
                break;
            }
            case faOthersReadWrite: {
                attributes |= S_IROTH;
                attributes |= S_IWOTH;
                break;
            }
            case faOthersExecute: {
                attributes |= S_IXOTH;
                break;
            }
        }
    }

    if (chmod(FFileName.data(), attributes) == 0) {
        result = true;
    }
#endif //POSIX

    return result;
}

#define S_ISRUSR(m)    (((m) & S_IRWXU) == S_IRUSR)
#define S_ISWUSR(m)    (((m) & S_IRWXU) == S_IWUSR)
#define S_ISXUSR(m)    (((m) & S_IRWXU) == S_IXUSR)

#define S_ISRGRP(m)    (((m) & S_IRWXG) == S_IRGRP)
#define S_ISWGRP(m)    (((m) & S_IRWXG) == S_IWGRP)
#define S_ISXGRP(m)    (((m) & S_IRWXG) == S_IXGRP)

#define S_ISROTH(m)    (((m) & S_IRWXO) == S_IROTH)
#define S_ISWOTH(m)    (((m) & S_IRWXO) == S_IWOTH)
#define S_ISXOTH(m)    (((m) & S_IRWXO) == S_IXOTH)

bool FileAttributes::ReadAttributes() {
    bool result = false;

#ifdef WINDOWS
    DWORD attributes = ::GetFileAttributes(FFileName.data());

    if (attributes != INVALID_FILE_ATTRIBUTES) {
        result = true;

        if (attributes | FILE_ATTRIBUTE_ARCHIVE) { FAttributes.push_back(faArchive); }
        if (attributes | FILE_ATTRIBUTE_COMPRESSED) { FAttributes.push_back(faCompressed); }
        if (attributes | FILE_ATTRIBUTE_DEVICE) { FAttributes.push_back(faDevice); }
        if (attributes | FILE_ATTRIBUTE_DIRECTORY) { FAttributes.push_back(faDirectory); }
        if (attributes | FILE_ATTRIBUTE_ENCRYPTED) { FAttributes.push_back(faEncrypted); }
        if (attributes | FILE_ATTRIBUTE_HIDDEN) { FAttributes.push_back(faHidden); }
        //if (attributes | FILE_ATTRIBUTE_INTEGRITY_STREAM) { FAttributes.push_back(faIntegrityStream); }
        if (attributes | FILE_ATTRIBUTE_NORMAL) { FAttributes.push_back(faNormal); }
        if (attributes | FILE_ATTRIBUTE_NOT_CONTENT_INDEXED) { FAttributes.push_back(faNotContentIndexed); }
        //if (attributes | FILE_ATTRIBUTE_NO_SCRUB_DATA) { FAttributes.push_back(faNoScrubData); }
        if (attributes | FILE_ATTRIBUTE_SYSTEM) { FAttributes.push_back(faSystem); }
        if (attributes | FILE_ATTRIBUTE_OFFLINE) { FAttributes.push_back(faOffline); }
        if (attributes | FILE_ATTRIBUTE_REPARSE_POINT) { FAttributes.push_back(faSymbolicLink); }
        if (attributes | FILE_ATTRIBUTE_SPARSE_FILE) { FAttributes.push_back(faSparceFile); }
        if (attributes | FILE_ATTRIBUTE_READONLY ) { FAttributes.push_back(faReadOnly); }
        if (attributes | FILE_ATTRIBUTE_TEMPORARY) { FAttributes.push_back(faTemporary); }
        if (attributes | FILE_ATTRIBUTE_VIRTUAL) { FAttributes.push_back(faVirtual); }
    }
#endif // WINDOWS
#ifdef POSIX
    struct stat status;

    if (stat(StringToFileSystemString(FFileName), &status) == 0) {
        result = true;

        if (S_ISBLK(status.st_mode) != 0) { FAttributes.push_back(faBlockSpecial); }
        if (S_ISCHR(status.st_mode) != 0) { FAttributes.push_back(faCharacterSpecial); }
        if (S_ISFIFO(status.st_mode) != 0) { FAttributes.push_back(faFIFOSpecial); }
        if (S_ISREG(status.st_mode) != 0) { FAttributes.push_back(faNormal); }
        if (S_ISDIR(status.st_mode) != 0) { FAttributes.push_back(faDirectory); }
        if (S_ISLNK(status.st_mode) != 0) { FAttributes.push_back(faSymbolicLink); }
        if (S_ISSOCK(status.st_mode) != 0) { FAttributes.push_back(faSocket); }

        // Owner
        if (S_ISRUSR(status.st_mode) != 0) {
            if (S_ISWUSR(status.st_mode) != 0) { FAttributes.push_back(faReadWrite); }
            else { FAttributes.push_back(faReadOnly); }
        }
        else if (S_ISWUSR(status.st_mode) != 0) { FAttributes.push_back(faWriteOnly); }

        if (S_ISXUSR(status.st_mode) != 0) { FAttributes.push_back(faExecute); }

        // Group
        if (S_ISRGRP(status.st_mode) != 0) {
            if (S_ISWGRP(status.st_mode) != 0) { FAttributes.push_back(faGroupReadWrite); }
            else { FAttributes.push_back(faGroupReadOnly); }
        }
        else if (S_ISWGRP(status.st_mode) != 0) { FAttributes.push_back(faGroupWriteOnly); }

        if (S_ISXGRP(status.st_mode) != 0) { FAttributes.push_back(faGroupExecute); }


        // Others
        if (S_ISROTH(status.st_mode) != 0) {
            if (S_ISWOTH(status.st_mode) != 0) { FAttributes.push_back(faOthersReadWrite); }
            else { FAttributes.push_back(faOthersReadOnly); }
        }
        else if (S_ISWOTH(status.st_mode) != 0) { FAttributes.push_back(faOthersWriteOnly); }

        if (S_ISXOTH(status.st_mode) != 0) { FAttributes.push_back(faOthersExecute); }

        if (FFileName.size() > 0 && FFileName[0] == '.') {
            FAttributes.push_back(faHidden);
        }
    }
#endif //POSIX

    return result;
}

bool FileAttributes::Valid(const FileAttribute Value) {
    bool result = false;

    switch (Value) {
#ifdef WINDOWS
        case faHidden:
#endif // WINDOWS
#ifdef POSIX
        case faReadWrite:
        case faWriteOnly:
        case faExecute:

        case faGroupReadWrite:
        case faGroupWriteOnly:
        case faGroupReadOnly:
        case faGroupExecute:

        case faOthersReadWrite:
        case faOthersWriteOnly:
        case faOthersReadOnly:
        case faOthersExecute:
#endif //POSIX

        case faReadOnly: {
            result = true;
            break;
        }
    }

    return result;
}

void FileAttributes::Append(FileAttribute Value) {
    if (Valid(Value) == true) {
#ifdef POSIX
        if ((Value == faReadOnly && Contains(faWriteOnly) == true) ||
            (Value == faWriteOnly && Contains(faReadOnly) == true)) {
            Value = faReadWrite;
        }
#endif //POSIX

        FAttributes.push_back(Value);
        WriteAttributes();
    }
}

bool FileAttributes::Contains(FileAttribute Value) {
    bool result = false;

    std::vector<FileAttribute>::const_iterator iterator = std::find(FAttributes.begin(), FAttributes.end(), Value);

    if (iterator != FAttributes.end()) {
        result = true;
    }

    return result;
}

void FileAttributes::Remove(FileAttribute Value) {
    if (Valid(Value) == true) {
#ifdef POSIX
        if (Value == faReadOnly && Contains(faReadWrite) == true) {
            Append(faWriteOnly);
            Remove(faReadWrite);
        }
        else if (Value == faWriteOnly && Contains(faReadWrite) == true) {
            Append(faReadOnly);
            Remove(faReadWrite);
        }
#endif //POSIX

        std::vector<FileAttribute>::iterator iterator = std::find(FAttributes.begin(), FAttributes.end(), Value);

        if (iterator != FAttributes.end()) {
            FAttributes.erase(iterator);
            WriteAttributes();
        }
    }
}
