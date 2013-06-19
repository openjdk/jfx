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

//Define Windows compatibility requirements
//XP or later
#define WINVER 0x0501
#define _WIN32_WINNT 0x0501

#include <Windows.h>

#include <shellapi.h> //DO NOT REMOVE, necessary for Gradle builds

#include <tchar.h>
#include <stdio.h>
#include <stdlib.h>
#include <malloc.h>
#include <memory.h>
#include <direct.h>
#include <process.h>

#include "jni.h"


/*
   This is launcher program for application package on Windows.

   Basic approach:
      - we read app/package.cfg file to find out details on what and how to launch
         (package.cfg is property file)
      - load JVM with requested JVM settings (client JVM if availble, server otherwise)
      - load embedded launcher class com.javafx.main.Main class and run main()
      - wait for JVM to exit and then exit from WinMain
      - support a way to "debug" application by setting env variable
        or passing "/Debug" option on command line
      - TODO: default directory is set to user's Documents and Settings
      - TODO: application folder is added to the library path (so LoadLibrary()) works

   Limitations and future work:
      - Running Java code in primordial thread may cause problems
        (example: can not use custom stack size).
        Solution used by java launcher is to create a new thread to invoke JVM.
        See CR 6316197 for more information.
      - Reuse code between windows/linux launchers and borrow more code from
        java.exe launcher implementation.
*/

//TODO:
//  Ideally we should be detecting max path length in runtime and reporting error
// if package was installed too deep in the file hierarchy.
// We also need to rewrite code to stop using fixed size buffers as it was proven
// we could fail to launch if buffer is small because strings are getting truncated.
//
// Quick fix for 2u2 is to increase buffer size.
// This will resolve buffer capacity related issues but application
// may still fail to launch without any feeback to the user if he installs
// it too deep. This is rare case but in the future we should be handling it better
#define LAUNCHER_MAXPATH  10000

//debug hook to print JVM messages into console
static bool isDebug = FALSE;

static jint JNICALL vfprintfHook(FILE *fp, const char *format, va_list args) {
   char buffer[20480];
   int len;
   HANDLE hConsole;
   DWORD wasWritten;

   len = _vsnprintf_s(buffer, sizeof(buffer), sizeof(buffer), format, args);
   if (len <= 0) {
        return len;
   }

   hConsole = GetStdHandle(STD_OUTPUT_HANDLE);
   if (hConsole == INVALID_HANDLE_VALUE) {
        return false;
   }

   //JVM will always pass us ASCII
   WriteConsoleA(hConsole, buffer, strlen(buffer), &wasWritten, NULL);

   return (jint) len;
}

/* helpers to popup error message */
static void showError(TCHAR *msg, TCHAR *msg2) {
    MessageBox(0, msg, msg2 != NULL ? msg2 : msg, MB_ICONERROR | MB_OK);
}

bool fileExists(TCHAR* name) {
  struct _stat buf;
  return (_tstat(name, &buf) == 0);
}

void makeFullFileName(TCHAR* basedir, TCHAR *relative_path, TCHAR *fullpath, int buffer_size) {
    fullpath[0] = 0;
    _tcscat_s(fullpath, buffer_size - _tcslen(fullpath), basedir);
    _tcscat_s(fullpath, buffer_size - _tcslen(fullpath), relative_path);
}

//constructs full file name for file in the package
// and check for it existance
bool getFileInPackage(TCHAR* basedir, TCHAR *relative_path, TCHAR *fullpath, int buffer_size) {
    makeFullFileName(basedir, relative_path, fullpath, buffer_size);
    return fileExists(fullpath);
}

#define MAINJAR_FOLDER        _T("\\app\\")
#define CONFIG_FILE           _T("\\app\\package.cfg")
#define CONFIG_MAINJAR_KEY    _T("app.mainjar")
#define CONFIG_MAINCLASS_KEY  _T("app.mainclass")
#define CONFIG_CLASSPATH_KEY  _T("app.classpath")
#define CONFIG_APP_ID_KEY     _T("app.preferences.id");

//remove trailing end of line character
//modifies buffer in place
void strip_endofline(TCHAR *buf) {
    size_t ln = _tcslen(buf);

    while (ln > 0 && (buf[ln-1] == '\r' || buf[ln-1] == '\n')) {
        buf[ln-1] = 0;
        ln--;
    }
}

#define JAVA_RUNTIME_SUBKEY _T("SOFTWARE\\JavaSoft\\Java Runtime Environment")
#define BUFFER_SIZE 256

// try to find current Java Home from registry
// HKLM\Software\JavaSoft\Java Runtime Environment\CurrentVersion
// HKLM\Software\JavaSoft\Java Runtime Environment\[CurrentVersion]\JavaHome
// return TRUE if found, and path is set in lpszJavaHome
// return FALSE otherwise
bool getSystemJRE(LPTSTR szHomeBin, unsigned long buflen) {
    HKEY hKey, vKey;
    DWORD dwType, dwCount = BUFFER_SIZE*sizeof(TCHAR);
    TCHAR versionString[BUFFER_SIZE], fullKey[2*BUFFER_SIZE];

    if (RegOpenKeyEx(HKEY_LOCAL_MACHINE, JAVA_RUNTIME_SUBKEY, 0, KEY_READ, &hKey) == ERROR_SUCCESS) {
        if (RegQueryValueEx(hKey, _T("CurrentVersion"), NULL, &dwType, (LPBYTE) versionString, &dwCount) == ERROR_SUCCESS) {
            _stprintf_s(fullKey, 2*BUFFER_SIZE, _T("%s\\%s"), JAVA_RUNTIME_SUBKEY, versionString);
            if (RegOpenKeyEx(HKEY_LOCAL_MACHINE, fullKey, 0, KEY_READ, &vKey) == ERROR_SUCCESS) {
                dwCount = buflen * sizeof(TCHAR);
                if (RegQueryValueEx(vKey, _T("JavaHome"), NULL, &dwType, (LPBYTE) szHomeBin, &dwCount) == ERROR_SUCCESS) {
                    if (fileExists(szHomeBin)) {
                        return true;
                    } else {
                        showError(szHomeBin, _T("System JRE does not exist at this location!"));
                        return false;
                    }
                } else {
                    showError(fullKey, _T("System JRE not found (registry)!"));
                    return false;
                }
            } else {
                showError(fullKey, _T("Failed to open registry key!!"));
                return false;
            }
        } else {
            showError(JAVA_RUNTIME_SUBKEY, _T("No value for CurrentVersion."));
            return false;
        }
    } else {
        showError(JAVA_RUNTIME_SUBKEY, _T("Failed to open registry key!"));
        return false;
    }

    return false;
}

//REWRITE: this is inefficient. We better read and parse file once
bool getConfigValue(TCHAR* basedir, TCHAR* lookupKey, TCHAR* outValue, int buf_size) {
    TCHAR config[LAUNCHER_MAXPATH] = {0};
    TCHAR buffer[LAUNCHER_MAXPATH*2];
    TCHAR *value;
    FILE *fp;

    *outValue = 0;

    if (!getFileInPackage(basedir, CONFIG_FILE, config, LAUNCHER_MAXPATH)) {
        showError(config, _T("Configuration file is not found!"));
        return false;
    }

    //scan file for the key
    errno_t err = _tfopen_s(&fp, config, _T("r"));
     if (err) {
         return false;
     }

     while (_fgetts(buffer, LAUNCHER_MAXPATH*2, fp)) {
        value = _tcschr(buffer, '=');
        if (value != NULL) {
          //end key on the '=', value will point to the value string
          *value = 0;
          value++;

          if (!_tcscmp(buffer, lookupKey)) { //found it
             fclose(fp);
             strip_endofline(value);
             _tcscpy_s(outValue, buf_size, value);
             return true;
          }
        }

     }
     fclose(fp);

     return false;
}

bool getAppFolder(TCHAR* basedir, TCHAR* appFolder, int buffer_size) {
    return getFileInPackage(basedir, MAINJAR_FOLDER, appFolder, LAUNCHER_MAXPATH);
}

//Constructs full path to the main jar file
//return false if not found
bool getMainJar(TCHAR* basedir, TCHAR* jar, int buffer_size) {
    TCHAR jarname[LAUNCHER_MAXPATH] = {0};
    TCHAR jar_relative[LAUNCHER_MAXPATH] = {0};
    TCHAR jar_full[LAUNCHER_MAXPATH] = {0};

    if (!getConfigValue(basedir, CONFIG_MAINJAR_KEY, jarname, LAUNCHER_MAXPATH)) {
        return false;
    }

    _tcscat_s(jar_relative, LAUNCHER_MAXPATH, MAINJAR_FOLDER);
    _tcscat_s(jar_relative, LAUNCHER_MAXPATH - _tcslen(jar_relative), jarname);

    bool ret = getFileInPackage(basedir, jar_relative, jar_full, LAUNCHER_MAXPATH);

    _tcscat_s(jar, LAUNCHER_MAXPATH - _tcslen(jar), jar_full);

    return ret;
}

// Private typedef for function pointer casting
typedef jint (JNICALL *JVM_CREATE)(JavaVM **, JNIEnv **, void *);

bool getJvmPath(TCHAR* basedir, TCHAR *jvmPath, int buffer_size) {
    jvmPath[0] = 0;
    if (!getFileInPackage(basedir, _T("\\runtime\\jre\\bin\\client\\jvm.dll"),
            jvmPath, LAUNCHER_MAXPATH)) {
        if (!getFileInPackage(basedir, _T("\\runtime\\jre\\bin\\server\\jvm.dll"),
                jvmPath, LAUNCHER_MAXPATH)) {
            return false;
        }

    }
    return true;
}

bool getSystemJvmPath(TCHAR *jvmPath, int buffer_size) {
    TCHAR basedir[LAUNCHER_MAXPATH];
    if (!getSystemJRE(basedir, LAUNCHER_MAXPATH)) {
        return false;
    }

    jvmPath[0] = 0;
    if (!getFileInPackage(basedir, _T("\\bin\\client\\jvm.dll"),
            jvmPath, LAUNCHER_MAXPATH)) {
        if (!getFileInPackage(basedir, _T("\\bin\\server\\jvm.dll"),
                jvmPath, LAUNCHER_MAXPATH)) {
            return false;
        }

    }
    return true;
}

//count how many args should be skipped before we get to app args
static int countNumberOfSystemArguments(int argCount, LPTSTR *szArgList) {
    if (szArgList && argCount > 1) {
        //path to .exe is at 0 index
        if (!_tcsicmp(_T("/Debug"), szArgList[1])) {
            return 2;
        }
    }
    return 1;
}

/*
 * Replace a pattern in a string (not regex, straight replace) with another
 * string.
 *
 * @param str
 * @param pattern
 * @param replaceWith
 * @return either original str or a new str (via strdup) that replaces the
 *         pattern with the replaceWith string
 */
TCHAR *replaceStr(TCHAR *str, TCHAR *pattern, TCHAR *replaceWith) {
	TCHAR buffer[MAX_PATH*2] = {0};
    TCHAR *p;

    //Return orig if str is not in orig.
    if(!(p = wcsstr(str, pattern))) {
		return wcsdup(str);
    }

    int loc = p-str;
    if (loc >= sizeof(buffer)) {
        return wcsdup(str);
    }

    wcsncpy(buffer, str, loc); // Copy characters from 'str' start to 'orig' st$
    buffer[loc] = '\0';

    int remaingBufferSize = sizeof(buffer) - loc;
    int len = _snwprintf(buffer+(loc), remaingBufferSize, _T("%s%s"), replaceWith, p + wcslen(pattern));
    if(len > remaingBufferSize ) {
        return wcsdup(str);
    }
    return wcsdup(buffer);
}


//Assumes are values passed in are big enough

int splitOptionIntoNameValue(TCHAR* argValue, TCHAR* optionName, int nameSize,
        TCHAR* optionValue, int valueSize) {
    TCHAR* ptr;

    ptr = wcsstr(argValue, _T("##"));
    if (ptr != NULL) {
        int len = wcslen(ptr);
        if (len > 0 && len < valueSize) {
            ptr++;
            ptr++;
            wcscpy(optionValue, ptr);

            int argLen = wcslen(argValue);
            if ((argLen - len + 1) < nameSize) {
                wmemcpy(optionName, argValue, argLen - len); //Does this work correctly for wide chars
                optionName[argLen - len] = '\0';
                return TRUE;
            }
        }
    }
    return FALSE;
}

#define MAX_KEY_LENGTH  80
#define MAX_VALUE_LENGTH  8192

TCHAR* convertIdToPath(TCHAR* id) {
    int len = (wcslen(id) + 1) * sizeof (TCHAR);
    TCHAR* path = (TCHAR*) calloc(len, sizeof (TCHAR));
    *path = '\0';
    TCHAR *returnValue = path;

    TCHAR ch = *id;
    int index = 0;
    while (ch != 0) {
        if (ch == '.') {
            *path = '\\';
        } else {
            *path = ch;
        }
        id++;
        ch = *id;
        path++;
    }
    *path = '\0';
    return returnValue;
}

//assume regKey big enough to hold largest key

LONG createRegKey(TCHAR* appid, HKEY *hKey) {
    TCHAR buf[MAX_PATH] = {0};
    wcscat(buf, _T("SOFTWARE\\JavaSoft\\Prefs\\"));
    TCHAR* path = convertIdToPath(appid);
    wcscat(buf, path);
    wcscat(buf, _T("\\JVMOptions"));
    free(path);
    LONG success = RegOpenKeyEx(HKEY_CURRENT_USER, buf, 0, KEY_READ | KEY_WOW64_64KEY, hKey);
    return success;
}

TCHAR* convertKeyToWinReg(TCHAR* key) {
    TCHAR* windowsName = (TCHAR*) calloc((wcslen(key) + 1)*2, sizeof (TCHAR)); //All caps could double size
    *windowsName = '\0';
    TCHAR *returnValue = windowsName;

    TCHAR ch = *key;
    int index = 0;
    while (ch != 0) {
        if (ch == '\\') {
            *windowsName = '//';
        } else if (ch == '/') {
            *windowsName = '\\';
        } else if ((ch >= 'A') && (ch <= 'Z')) {
            *windowsName++ = '/';
            *windowsName = ch;
        } else {
            *windowsName = ch;
        }
        key++;
        ch = *key;
        windowsName++;
    }
    *windowsName = '\0';
    return returnValue;
}

TCHAR* getJvmUserArg(TCHAR* appid, TCHAR* argvalue) {
    TCHAR optionName[MAX_PATH] = {0};
    TCHAR optionValue[MAX_PATH] = {0};
    HKEY hKey = 0;
    HKEY vKey = 0;
    DWORD dwType, dwCount = MAX_VALUE_LENGTH * sizeof (TCHAR);
    TCHAR userValue[MAX_VALUE_LENGTH] = {0};
    TCHAR* result = NULL;

    if (splitOptionIntoNameValue(argvalue, optionName, sizeof (optionName), optionValue, sizeof (optionValue))) {
        LONG success = createRegKey(appid, &hKey);
        TCHAR* regOptionName = convertKeyToWinReg(optionName);
        if (success == ERROR_SUCCESS) {
            success = RegQueryValueEx(hKey, regOptionName, NULL, &dwType, (LPBYTE) userValue, &dwCount);
            if (success == ERROR_SUCCESS) {
                wcscat(optionName, userValue);
                result = wcsdup(optionName);
            }
        }
        free(regOptionName);

        //If not found in registry just combine the two and return it
        if (result == NULL) {
            TCHAR* concatenated = (TCHAR*) calloc((wcslen(optionName) + wcslen(optionValue) + 1), sizeof (TCHAR));
            wcscat(concatenated, optionName);
            wcscat(concatenated, optionValue);
            result = concatenated;
        }
    }        //This should not occur, but if there is no delimeter just treat as complete option
    else {
        result = wcsdup(argvalue);
    }
    return result;
}


#define MAX_OPTIONS 100
#define MAX_OPTION_NAME 50

bool startJVM(TCHAR* basedir, TCHAR* appFolder, TCHAR* jar, int argCount, LPTSTR *szArgList) {
    TCHAR jvmPath[LAUNCHER_MAXPATH+1] = {0};
    JavaVMInitArgs jvmArgs;
    JavaVMOption options[MAX_OPTIONS+1];
    JVM_CREATE createProc;
    JNIEnv* env;
    JavaVM* jvm = NULL;
    char jarASCII[LAUNCHER_MAXPATH] = {0};
    char classpath[LAUNCHER_MAXPATH*2] = {0};
    char mainclassASCII[LAUNCHER_MAXPATH] = {0},
        appClasspath[LAUNCHER_MAXPATH] = {0};
    size_t outlen = 0;
    jclass cls;
    jmethodID mid;
    TCHAR argname[MAX_OPTION_NAME + 1] = {0};
    TCHAR argvalue[LAUNCHER_MAXPATH] = {0},
    mainclass[LAUNCHER_MAXPATH] = {0};
    CHAR  argvalueASCII[LAUNCHER_MAXPATH] = {0};
    HMODULE msvcrtdll;
    bool runtimeBundled;
    TCHAR tmpPath[LAUNCHER_MAXPATH] = {0};
    TCHAR appid[LAUNCHER_MAXPATH] = {0};

    memset(&options, 0, sizeof(JavaVMOption)*(MAX_OPTIONS + 1));
    memset(&jvmArgs, 0, sizeof(JavaVMInitArgs));

    makeFullFileName(basedir, _T("\\runtime"), tmpPath, sizeof(tmpPath)/sizeof(TCHAR));
    runtimeBundled = fileExists(tmpPath);
    if (runtimeBundled) {
       if (!getJvmPath(basedir, jvmPath, LAUNCHER_MAXPATH)) {
            showError(_T("jvm.dll is not found in bundled runtime."), jvmPath);
            return false;
       }
       //make sure msvcr100 is loaded (or we may fail if copy of it is not installed into system)
       makeFullFileName(basedir, _T("runtime\\jre\\bin\\msvcr100.dll"), tmpPath, sizeof(tmpPath)/sizeof(TCHAR));
       msvcrtdll = ::LoadLibrary(tmpPath);
    } else {
        if (!getSystemJvmPath(jvmPath, LAUNCHER_MAXPATH)) {
            showError(_T("No bundled runtime and can not find system JRE."), jvmPath);
            return false;
        }
       //make sure msvcr100 is loaded (or we may fail if copy of it is not installed into system)
       makeFullFileName(basedir, _T("\\bin\\msvcr100.dll"), tmpPath, sizeof(tmpPath)/sizeof(TCHAR));
       msvcrtdll = ::LoadLibrary(tmpPath);
    }

    // Dynamically load the JVM
    HMODULE jvmLibHandle = LoadLibrary(jvmPath);
    if (jvmLibHandle == NULL) {
        DWORD dwErr = GetLastError();
        showError(_T("Error loading jvm.dll"), jvmPath);
        return false;
    }

    //convert argument to ASCII string as this is what CreateJVM needs
    wcstombs_s(&outlen, jarASCII, LAUNCHER_MAXPATH, jar, (size_t) wcslen(jar) + 1);
    strcpy_s(classpath, LAUNCHER_MAXPATH*2, "-Djava.class.path=");
    strcat_s(classpath, LAUNCHER_MAXPATH, jarASCII);

    if (getConfigValue(basedir, CONFIG_CLASSPATH_KEY, argvalue, LAUNCHER_MAXPATH)) {
           size_t inLen = (size_t) wcslen(argvalue);
           //convert argument to ASCII string as this is what CreateJVM needs
           wcstombs_s(&outlen, argvalueASCII, sizeof(argvalueASCII), argvalue, inLen + 1);
           //compress spaces and replaces them with ;
           {
               char *in = argvalueASCII;
               char *out = argvalueASCII;
               bool needSemicolon = false;

               while (*in != 0) {
                   if (*in == ' ') {
                       if (needSemicolon) {
                          *out = ';';
                          out++;
                          needSemicolon = false;
                       }
                   } else {
                       needSemicolon = true;
                       *out = *in;
                       out++;
                   }
                   in++;
               }
               *out = 0;
           }
           if (strlen(argvalueASCII) > 0) {
               strcat_s(classpath, LAUNCHER_MAXPATH, ";");
               strcat_s(classpath, LAUNCHER_MAXPATH, argvalueASCII);
           }
    }

    // Set up the VM init args
    jvmArgs.version = JNI_VERSION_1_2;

    options[0].optionString = _strdup(classpath);

    int cnt = 1;
    if (isDebug) {
       options[cnt].optionString = _strdup("vfprintf");
       options[cnt].extraInfo    = vfprintfHook;
       cnt++;
    }

    //Note: should not try to quote the path. Spaces are fine here
    _stprintf_s(argvalue, _T("-Djava.library.path=%s"), appFolder);
    wcstombs_s(&outlen, argvalueASCII, sizeof(argvalueASCII),
               argvalue, wcslen(argvalue) + 1);
    options[cnt].optionString = _strdup(argvalueASCII);
    cnt++;

    //add app specific JVM parameters
    int idx = 1;
    int found = 0;
    do {
       _stprintf_s(argname, MAX_OPTION_NAME, _T("jvmarg.%d"), idx);
       found = getConfigValue(basedir, argname, argvalue, LAUNCHER_MAXPATH);
       if (found) {

            TCHAR* option = replaceStr(argvalue, _T("$APPDIR"), basedir);
            if (wcstombs_s(&outlen, argvalueASCII, sizeof (argvalueASCII), option, wcslen(option) + 1) != 0) {
                showError(_T("Failed converting JVM Argument to ASCII"), argname);
                free(option);
                return false;
            }
            free(option);
            char* jvmOption = _strdup(argvalueASCII);
            options[cnt].optionString = jvmOption;
            idx++;
            cnt++;
        }
    } while (found && idx < MAX_OPTIONS);

    found = getConfigValue(basedir, _T("app.id"), appid, LAUNCHER_MAXPATH);
    if (found) {
        _stprintf_s(argvalue, _T("-Dapp.id=%s"), appid);
        wcstombs_s(&outlen, argvalueASCII, sizeof (argvalueASCII),
                argvalue, wcslen(argvalue) + 1);
        options[cnt].optionString = _strdup(argvalueASCII);
        cnt++;

        idx = 1;
        do {
            _stprintf_s(argname, MAX_OPTION_NAME, _T("jvmuserarg.%d"), idx);
            found = getConfigValue(basedir, argname, argvalue, LAUNCHER_MAXPATH);
            if (found) {

                TCHAR* option = getJvmUserArg(appid, argvalue);
                if (option != NULL) {
                    if (wcstombs_s(&outlen, argvalueASCII, sizeof (argvalueASCII), option, wcslen(option) + 1) != 0) {
                        showError(_T("Failed converting JVM Argument to ASCII"), argname);
                        free(option);
                        return false;
                    }
                    free(option);
                    char* jvmOption = _strdup(argvalueASCII);
                    options[cnt].optionString = jvmOption;
                    cnt++;
                }
                idx++;
            }
        } while (found && idx < MAX_OPTIONS);
    }

    jvmArgs.version = 0x00010002;
    jvmArgs.options = options;
    jvmArgs.nOptions = cnt;
    jvmArgs.ignoreUnrecognized = JNI_TRUE;

    // Create the JVM
    // NB: need to use ASCII string as UNICODE is not supported
    createProc = (JVM_CREATE) GetProcAddress(jvmLibHandle, "JNI_CreateJavaVM");
    if (createProc == NULL) {
        showError(_T("Failed to locate JNI_CreateJavaVM"), jvmPath);
        return false;
    }

    if ((*createProc)(&jvm, &env, &jvmArgs) < 0) {
        showError(_T("Failed to create JVM"), jvmPath);
        return false;
    }

    if (!getConfigValue(basedir, CONFIG_MAINCLASS_KEY, mainclass, LAUNCHER_MAXPATH)) {
        showError(_T("Package error"), _T("No main class specified. Nothing to launch"));
        return false;
    } else {
           size_t inLen = (size_t) wcslen(mainclass);
           //convert argument to ASCII string as this is what CreateJVM needs
           wcstombs_s(&outlen, mainclassASCII, sizeof(mainclassASCII), mainclass, inLen + 1);
    }

    cls = env->FindClass(mainclassASCII);
    if (cls != NULL) {
        mid = env->GetStaticMethodID(cls, "main", "([Ljava/lang/String;)V");
         if (mid != NULL) {
            jclass stringClass = env->FindClass("java/lang/String");
            //prepare app arguments if any. Skip value at index 0 - this is path to executable ...
            //NOTE:
            //  - what if user run in non-English/UTF-8 locale? do we need to convert args?
            //  - extend to pass jvm args and debug args (allow them in front, use marker option to separate them?)
            int startArgIndex = countNumberOfSystemArguments(argCount, szArgList);
            jobjectArray args = env->NewObjectArray(argCount - startArgIndex, stringClass, NULL);
            for(int i=startArgIndex; i<argCount; i++) {
                size_t inLen = (size_t) wcslen(szArgList[i]);
                env->SetObjectArrayElement(args, i-startArgIndex, env->NewString((jchar*)szArgList[i], inLen));
            }
            env->CallStaticVoidMethod(cls, mid, args);
        } else {
            showError(_T("no main method in the main class!"), mainclass);
            return false;
        }
    } else {
        showError(_T("no main class."), mainclass);
        return false;
    }

    if (env->ExceptionOccurred()) {
        showError(_T("Failed due to exception from main class."), mainclass);
        env->ExceptionDescribe();
    }

    // If application main() exits quickly but application is run on some other thread
    //  (e.g. Swing app performs invokeLater() in main and exits)
    // then if we return execution to tWinMain it will exit.
    // This will cause process to exit and application will not actually run.
    //
    // To avoid this we are trying to detach jvm from current thread (java.exe does the same)
    // Because we are doing this on the main JVM thread (i.e. one that was used to create JVM)
    // this call will spawn "Destroy Java VM" java thread that will shut JVM once there are
    // no non-daemon threads running, and then return control here.
    // I.e. this will happen when EDT and other app thread will exit.
    if (jvm->DetachCurrentThread() != 0) {
        showError(_T("Detach failed."), NULL);
    }
    jvm->DestroyJavaVM();

    return true;
}

//Ways to enable debugs output:
//   - set env variable JAVAFX_LAUNCHER_DEBUG
//   - pass /Debug on the command line
static void enableDebugIfNeeded(int argCount, LPTSTR *szArgList) {
    TCHAR* buffer = NULL;
    size_t bufferLen = 0;

    //see if first arg is /Debug
    //Only allow system command to be in front!
    if (szArgList && argCount > 1) {
        if (!_tcsicmp(_T("/Debug"), szArgList[1])) {
            isDebug = true;
            return;
        }
    }

    //Check if env variable is set
    _tdupenv_s(&buffer, &bufferLen, _T("JAVAFX_LAUNCHER_DEBUG"));
    if (buffer != NULL) { //env variable set
        isDebug = true;
        free(buffer);
    }
}

int APIENTRY _tWinMain(HINSTANCE hInstance,
                     HINSTANCE hPrevInstance,
                     LPTSTR    lpCmdLine,
                     int       nCmdShow)
{
    TCHAR basedir[LAUNCHER_MAXPATH] = {0};
    TCHAR java[LAUNCHER_MAXPATH] = {0};
    TCHAR appFolder[LAUNCHER_MAXPATH] = {0};
    TCHAR java_escaped[LAUNCHER_MAXPATH] = {0};
    TCHAR jar[LAUNCHER_MAXPATH] = {0};
    LPTSTR *szArgList;
    int argCount;

    // Parse command line arguments to see if /Debug is there
    szArgList = CommandLineToArgvW(GetCommandLine(), &argCount);

    enableDebugIfNeeded(argCount, szArgList);

    if (isDebug) {
        AllocConsole();
        SetConsoleOutputCP(CP_UTF8);
    }

    if (GetModuleFileNameW(NULL, basedir, LAUNCHER_MAXPATH) != 0) {
        TCHAR *end = _tcsrchr(basedir, '\\');
        if (end != NULL) {
            *end = 0;

            if (!getMainJar(basedir, jar, LAUNCHER_MAXPATH)) {
                showError(
                    (jar[0] == 0) ? _T("Failed to parse package configuration file") : jar,
                    _T("Failed to find main application jar!"));
                return -1;
            }

            getAppFolder(basedir, appFolder, LAUNCHER_MAXPATH);

            //DO Launch
            //this will concatenate arguments using space,
            // we need to make sure spaces are properly escaped if we have any
            _tchdir(appFolder);

            if (!startJVM(basedir, appFolder, jar, argCount, szArgList)) {
                showError(_T("Failed to launch JVM"), NULL);
                return -1;
            }
        }
    }

    if (szArgList != NULL) {
        LocalFree(szArgList);
    }

    if (isDebug) {
      showError(_T("Exiting application"), NULL);
    }
    return 1;
}

