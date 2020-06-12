/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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

#ifdef WIN32

#include <windows.h>
#include <stdio.h>
#include <stddef.h>
#include <stdlib.h>

#include <jni.h>
#include <com_sun_javafx_font_PrismFontFactory.h>

#define BSIZE (max(512, MAX_PATH+1))

/* Typically all local references held by a JNI function are automatically
 * released by JVM when the function returns. However, there is a limit to the
 * number of local references that can remain active. If the local references
 * continue to grow, it could result in out of memory error. Henceforth, we
 * invoke DeleteLocalRef on objects that are no longer needed for execution in
 * the JNI function.
 */
#define DeleteLocalReference(env, jniRef) \
    do { \
        if (jniRef != NULL) { \
            (*env)->DeleteLocalRef(env, jniRef); \
            jniRef = NULL; \
        } \
    } while (0)

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL JNI_OnLoad_javafx_font(JavaVM *vm, void *reserved) {
#ifdef JNI_VERSION_1_8
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_8) != JNI_OK) {
        return JNI_VERSION_1_4;
    }
    return JNI_VERSION_1_8;
#else
    return JNI_VERSION_1_4;
#endif // JNI_VERSION_1_8
}
#endif // STATIC_BUILD

JNIEXPORT jbyteArray JNICALL
Java_com_sun_javafx_font_PrismFontFactory_getFontPath(JNIEnv *env, jobject thiz)
{
    char windir[BSIZE];
    char sysdir[BSIZE];
    char fontpath[BSIZE*2];
    char *end;
    jbyteArray byteArrObj;
    int pathLen;
    unsigned char *data;

    /* Locate fonts directories relative to the Windows System directory.
     * If Windows System location is different than the user's window
     * directory location, as in a shared Windows installation,
     * return both locations as potential font directories
     */
    GetSystemDirectory(sysdir, BSIZE);
    end = strrchr(sysdir,'\\');
    if (end && (stricmp(end,"\\System") || stricmp(end,"\\System32"))) {
        *end = 0;
         strcat(sysdir, "\\Fonts");
    }

    GetWindowsDirectory(windir, BSIZE);
    if (strlen(windir) > BSIZE-7) {
        *windir = 0;
    } else {
        strcat(windir, "\\Fonts");
    }

    strcpy(fontpath,sysdir);
    if (stricmp(sysdir,windir)) {
        strcat(fontpath,";");
        strcat(fontpath,windir);
    }

    pathLen = strlen(fontpath);

    byteArrObj = (*env)->NewByteArray(env, pathLen);
    if (byteArrObj == NULL) {
        return (jbyteArray)NULL;
    }
    data = (*env)->GetByteArrayElements(env, byteArrObj, NULL);
    if (data == NULL) {
        return byteArrObj;
    }
    memcpy(data, fontpath, pathLen);
    (*env)->ReleaseByteArrayElements(env, byteArrObj, (jbyte*) data, (jint)0);

    return byteArrObj;
}

/* The code below is used to obtain information from the windows font APIS
 * and registry on which fonts are available and what font files hold those
 * fonts. The results are used to speed font lookup.
 */

typedef struct GdiFontMapInfo {
    JNIEnv *env;
    jstring family;
    jobject fontToFamilyMap;
    jobject familyToFontListMap;
    jobject list;
    jmethodID putMID;
    jmethodID containsKeyMID;
    jclass arrayListClass;
    jmethodID arrayListCtr;
    jmethodID addMID;
    jmethodID toLowerCaseMID;
    jobject locale;
    HDC screenDC;
} GdiFontMapInfo;

/* NT is W2K & XP, Vista, Win 7 etc. ie anything later than win9x */
static const char FONTKEY_NT[] =
    "Software\\Microsoft\\Windows NT\\CurrentVersion\\Fonts";


typedef struct CheckFamilyInfo {
  wchar_t *family;
  wchar_t* fullName;
  int isDifferent;
} CheckFamilyInfo;

static int CALLBACK CheckFontFamilyProcW(
  ENUMLOGFONTEXW *lpelfe,
  NEWTEXTMETRICEX *lpntme,
  int FontType,
  LPARAM lParam)
{
    CheckFamilyInfo *info = (CheckFamilyInfo*)lParam;
    info->isDifferent = wcscmp(lpelfe->elfLogFont.lfFaceName, info->family);

/*     if (!info->isDifferent) { */
/*         wprintf(LFor font %s expected family=%s instead got %s\n", */
/*                 lpelfe->elfFullName, */
/*                 info->family, */
/*                 lpelfe->elfLogFont.lfFaceName); */
/*         fflush(stdout); */
/*     } */
    return 0;
}

static int DifferentFamily(wchar_t *family, wchar_t* fullName,
                           GdiFontMapInfo *fmi) {
    LOGFONTW lfw;
    CheckFamilyInfo info;

    /* If fullName can't be stored in the struct, assume correct family */
    if (wcslen((LPWSTR)fullName) >= LF_FACESIZE) {
        return 0;
    }

    memset(&info, 0, sizeof(CheckFamilyInfo));
    info.family = family;
    info.fullName = fullName;
    info.isDifferent = 0;

    memset(&lfw, 0, sizeof(lfw));
    wcscpy(lfw.lfFaceName, fullName);
    lfw.lfCharSet = DEFAULT_CHARSET;
    EnumFontFamiliesExW(fmi->screenDC, &lfw,
                        (FONTENUMPROCW)CheckFontFamilyProcW,
                        (LPARAM)(&info), 0L);

    return info.isDifferent;
}

/* Callback for call to EnumFontFamiliesEx in the EnumFamilyNames function.
 * Expects to be called once for each face name in the family specified
 * in the call. We extract the full name for the font which is expected
 * to be in the "system encoding" and create canonical and lower case
 * Java strings for the name which are added to the maps. The lower case
 * name is used as key to the family name value in the font to family map,
 * the canonical name is one of the"list" of members of the family.
 */
static int CALLBACK EnumFontFacesInFamilyProcW(
  ENUMLOGFONTEXW *lpelfe,
  NEWTEXTMETRICEX *lpntme,
  int FontType,
  LPARAM lParam)
{
    GdiFontMapInfo *fmi = (GdiFontMapInfo*)lParam;
    JNIEnv *env = fmi->env;
    jstring fullname, fullnameLC;

    /* Exceptions indicate critical errors such that program cannot continue
     * with further execution. Henceforth, the function returns immediately
     * on pending exceptions. In these situations, the function also returns
     * 0 indicating windows API to stop further enumeration and callbacks.
     *
     * The JNI functions do not clear the pending exceptions. This allows the
     * caller (Java code) to check and handle exceptions in the best possible
     * way.
     */
    if ((*env)->ExceptionCheck(env)) {
        return 0;
    }

    /* Both Vista and XP return DEVICE_FONTTYPE for OTF fonts */
    if (FontType != TRUETYPE_FONTTYPE && FontType != DEVICE_FONTTYPE) {
        return 1;
    }

    /* Windows has font aliases and so may enumerate fonts from
     * the aliased family if any actual font of that family is installed.
     * To protect against it ignore fonts which aren't enumerated under
     * their true family.
     */
    if (DifferentFamily(lpelfe->elfLogFont.lfFaceName,
                        lpelfe->elfFullName, fmi))  {
      return 1;
    }

    fullname = (*env)->NewString(env, lpelfe->elfFullName,
                                 wcslen((LPWSTR)lpelfe->elfFullName));
    if (fullname == NULL) {
        (*env)->ExceptionClear(env);
        return 1;
    }

    (*env)->CallBooleanMethod(env, fmi->list, fmi->addMID, fullname);
    if ((*env)->ExceptionCheck(env)) {
        /* Delete the created reference before return */
        DeleteLocalReference(env, fullname);
        return 0;
    }

    fullnameLC = (*env)->CallObjectMethod(env, fullname,
                                          fmi->toLowerCaseMID, fmi->locale);
    /* Delete the created reference after its usage */
    DeleteLocalReference(env, fullname);
    if ((*env)->ExceptionCheck(env)) {
        return 0;
    }

    (*env)->CallObjectMethod(env, fmi->fontToFamilyMap,
                             fmi->putMID, fullnameLC, fmi->family);
    /* Delete the created reference after its usage */
    DeleteLocalReference(env, fullnameLC);
    if ((*env)->ExceptionCheck(env)) {
        return 0;
    }

    return 1;
}

/* Callback for EnumFontFamiliesEx in populateFontFileNameMap.
 * Expects to be called for every charset of every font family.
 * If this is the first time we have been called for this family,
 * add a new mapping to the familyToFontListMap from this family to a
 * list of its members. To populate that list, further enumerate all faces
 * in this family for the matched charset. This assumes that all fonts
 * in a family support the same charset, which is a fairly safe assumption
 * and saves time as the call we make here to EnumFontFamiliesEx will
 * enumerate the members of this family just once each.
 * Because we set fmi->list to be the newly created list the call back
 * can safely add to that list without a search.
 */
static int CALLBACK EnumFamilyNamesW(
  ENUMLOGFONTEXW *lpelfe,    /* pointer to logical-font data */
  NEWTEXTMETRICEX *lpntme,  /* pointer to physical-font data */
  int FontType,             /* type of font */
  LPARAM lParam )           /* application-defined data */
{
    GdiFontMapInfo *fmi = (GdiFontMapInfo*)lParam;
    JNIEnv *env = fmi->env;
    jstring familyLC;
    int slen;
    LOGFONTW lfw;
    jboolean mapHasKey;

    /* Exceptions indicate critical errors such that program cannot continue
     * with further execution. Henceforth, the function returns immediately
     * on pending exceptions. In these situations, the function also returns
     * 0 indicating windows API to stop further enumeration and callbacks.
     *
     * The JNI functions do not clear the pending exceptions. This allows the
     * caller (Java code) to check and handle exceptions in the best possible
     * way.
     */
    if ((*env)->ExceptionCheck(env)) {
        return 0;
    }

    /* Both Vista and XP return DEVICE_FONTTYPE for OTF fonts */
    if (FontType != TRUETYPE_FONTTYPE && FontType != DEVICE_FONTTYPE) {
        return 1;
    }
/*     wprintf(L"FAMILY=%s charset=%d FULL=%s\n", */
/*          lpelfe->elfLogFont.lfFaceName, */
/*          lpelfe->elfLogFont.lfCharSet, */
/*          lpelfe->elfFullName); */
/*     fflush(stdout); */

    /* Windows lists fonts which have a vmtx (vertical metrics) table twice.
     * Once using their normal name, and again preceded by '@'. These appear
     * in font lists in some windows apps, such as wordpad. We don't want
     * these so we skip any font where the first character is '@'
     */
    if (lpelfe->elfLogFont.lfFaceName[0] == L'@') {
            return 1;
    }
    slen = wcslen(lpelfe->elfLogFont.lfFaceName);
    fmi->family = (*env)->NewString(env,lpelfe->elfLogFont.lfFaceName, slen);
    if (fmi->family == NULL) {
        (*env)->ExceptionClear(env);
        return 1;
    }

    familyLC = (*env)->CallObjectMethod(env, fmi->family,
                                        fmi->toLowerCaseMID, fmi->locale);
    /* Delete the created reference after its usage */
    if ((*env)->ExceptionCheck(env)) {
        DeleteLocalReference(env, fmi->family);
        return 0;
    }

    /* check if already seen this family with a different charset */
    mapHasKey = (*env)->CallBooleanMethod(env,
                                          fmi->familyToFontListMap,
                                          fmi->containsKeyMID,
                                          familyLC);

    if ((*env)->ExceptionCheck(env)) {
        /* Delete the created references before return */
        DeleteLocalReference(env, fmi->family);
        DeleteLocalReference(env, familyLC);
        return 0;
    } else if (mapHasKey) {
        /* Delete the created references before return */
        DeleteLocalReference(env, fmi->family);
        DeleteLocalReference(env, familyLC);
        return 1;
    }

    fmi->list = (*env)->NewObject(env,
                                  fmi->arrayListClass, fmi->arrayListCtr, 4);
    if (fmi->list == NULL) {
        /* Delete the created references before return */
        DeleteLocalReference(env, fmi->family);
        DeleteLocalReference(env, familyLC);
        return 0;
    }

    (*env)->CallObjectMethod(env, fmi->familyToFontListMap,
                             fmi->putMID, familyLC, fmi->list);
    /* Delete the created reference after its usage */
    DeleteLocalReference(env, familyLC);
    if ((*env)->ExceptionCheck(env)) {
        /* Delete the created reference before return */
        DeleteLocalReference(env, fmi->family);
        DeleteLocalReference(env, fmi->list);
        return 0;
    }

    memset(&lfw, 0, sizeof(lfw));
    wcscpy(lfw.lfFaceName, lpelfe->elfLogFont.lfFaceName);
    lfw.lfCharSet = lpelfe->elfLogFont.lfCharSet;
    EnumFontFamiliesExW(fmi->screenDC, &lfw,
                        (FONTENUMPROCW)EnumFontFacesInFamilyProcW,
                        lParam, 0L);

    /* Delete the created reference after its usage in the enum function */
    DeleteLocalReference(env, fmi->family);
    DeleteLocalReference(env, fmi->list);
    return 1;
}


/* It looks like TrueType fonts have " (TrueType)" tacked on the end of their
 * name, so we can try to use that to distinguish TT from other fonts.
 * However if a program "installed" a font in the registry the key may
 * not include that. We could also try to "pass" fonts which have no "(..)"
 * at the end. But that turns out to pass a few .FON files that MS supply.
 * If there's no parenthesised type string, we could next try to infer
 * the file type from the file name extension. Since the MS entries that
 * have no type string are very few, and have odd names like "MS-DOS CP 437"
 * and would never return a Java Font anyway its currently OK to put these
 * in the font map, although clearly the returned names must never percolate
 * up into a list of available fonts returned to the application.
 * Additionally for TTC font files the key looks like
 * Font 1 & Font 2 (TrueType)
 * or sometimes even :
 * Font 1 & Font 2 & Font 3 (TrueType)
 * Also if a Font has a name for this locale that name also
 * exists in the registry using the appropriate platform encoding.
 * What do we do then?
 *
 * Note: OpenType fonts seems to have " (TrueType)" suffix on Vista
 *   but " (OpenType)" on XP.
 */
static BOOL RegistryToBaseTTNameW(LPWSTR name) {
    static const wchar_t TTSUFFIX[] = L" (TrueType)";
    static const wchar_t OTSUFFIX[] = L" (OpenType)";
    int TTSLEN = wcslen(TTSUFFIX);
    wchar_t *suffix;

    int len = wcslen(name);
    if (len == 0) {
        return FALSE;
    }
    if (name[len-1] != L')') {
        return FALSE;
    }
    if (len <= TTSLEN) {
        return FALSE;
    }
    /* suffix length is the same for truetype and opentype fonts */
    suffix = name + (len - TTSLEN);
    // REMIND : renable OpenType (.otf) some day.
    if (wcscmp(suffix, TTSUFFIX) == 0 /*|| wcscmp(suffix, OTSUFFIX) == 0*/) {
        suffix[0] = L'\0'; /* truncate name */
        return TRUE;
    }
    return FALSE;
}

static void registerFontW(GdiFontMapInfo *fmi, jobject fontToFileMap,
                          LPWSTR name, LPWSTR data) {

    wchar_t *ptr1, *ptr2;
    jstring fontStr;
    jstring fontStrLC;
    JNIEnv *env = fmi->env;
    size_t dslen = wcslen(data);
    jstring fileStr = (*env)->NewString(env, data, (jsize)dslen);
    if (fileStr == NULL) {
        (*env)->ExceptionClear(env);
        return;
    }

    /* TTC or ttc means it may be a collection. Need to parse out
     * multiple font face names separated by " & "
     * By only doing this for fonts which look like collections based on
     * file name we are adhering to MS recommendations for font file names
     * so it seems that we can be sure that this identifies precisely
     * the MS-supplied truetype collections.
     * This avoids any potential issues if a TTF file happens to have
     * a & in the font name (I can't find anything which prohibits this)
     * and also means we only parse the key in cases we know to be
     * worthwhile.
     */

    if ((data[dslen-1] == L'C' || data[dslen-1] == L'c') &&
        (ptr1 = wcsstr(name, L" & ")) != NULL) {
        ptr1+=3;
        while (ptr1 >= name) { /* marginally safer than while (true) */
            while ((ptr2 = wcsstr(ptr1, L" & ")) != NULL) {
                ptr1 = ptr2+3;
            }
            fontStr = (*env)->NewString(env, ptr1, wcslen(ptr1));
            if (fontStr == NULL) {
                (*env)->ExceptionClear(env);
                /* Delete the created reference before return */
                DeleteLocalReference(env, fileStr);
                return;
            }
            fontStrLC = (*env)->CallObjectMethod(env, fontStr,
                                                 fmi->toLowerCaseMID,
                                                 fmi->locale);
            /* Delete the created reference after its usage */
            DeleteLocalReference(env, fontStr);
            if ((*env)->ExceptionCheck(env)) {
                /* Delete the created reference before return */
                DeleteLocalReference(env, fileStr);
                return;
            }
            (*env)->CallObjectMethod(env, fontToFileMap, fmi->putMID,
                                     fontStrLC, fileStr);
            /* Delete the reference after its usage */
            DeleteLocalReference(env, fontStrLC);
            if ((*env)->ExceptionCheck(env)) {
                /* Delete the created reference before return */
                DeleteLocalReference(env, fileStr);
                return;
            }
            if (ptr1 == name) {
                break;
            } else {
                *(ptr1-3) = L'\0';
                ptr1 = name;
            }
        }
    } else {
        fontStr = (*env)->NewString(env, name, wcslen(name));
        if (fontStr == NULL) {
            (*env)->ExceptionClear(env);
            /* Delete the created reference before return */
            DeleteLocalReference(env, fileStr);
            return;
        }
        fontStrLC = (*env)->CallObjectMethod(env, fontStr,
                                           fmi->toLowerCaseMID, fmi->locale);
        /* Delete the created reference after its usage */
        DeleteLocalReference(env, fontStr);
        if ((*env)->ExceptionCheck(env)) {
            /* Delete the created reference before return */
            DeleteLocalReference(env, fileStr);
            return;
        }
        (*env)->CallObjectMethod(env, fontToFileMap, fmi->putMID,
                                 fontStrLC, fileStr);
        /* Delete the created reference after its usage */
        DeleteLocalReference(env, fontStrLC);
        if ((*env)->ExceptionCheck(env)) {
            /* Delete the created reference before return */
            DeleteLocalReference(env, fileStr);
            return;
        }
    }

    /* Delete the created reference after its usage */
    DeleteLocalReference(env, fileStr);
}

/* Obtain all the fontname -> filename mappings.
 * This is called once and the results returned to Java code which can
 * use it for lookups to reduce or avoid the need to search font files.
 */
JNIEXPORT void JNICALL
Java_com_sun_javafx_font_PrismFontFactory_populateFontFileNameMap
(JNIEnv *env, jclass obj, jobject fontToFileMap,
 jobject fontToFamilyMap, jobject familyToFontListMap, jobject locale)
{
#define MAX_BUFFER (FILENAME_MAX+1)
    const wchar_t wname[MAX_BUFFER];
    const char data[MAX_BUFFER];

    DWORD type;
    LONG ret;
    HKEY hkeyFonts;
    DWORD dwNameSize;
    DWORD dwDataValueSize;
    DWORD nval;
    DWORD dwNumValues, dwMaxValueNameLen, dwMaxValueDataLen;
    DWORD numValues = 0;
    jclass classIDHashMap;
    jclass classIDString;
    jmethodID putMID;
    GdiFontMapInfo fmi;
    LOGFONTW lfw;

    /* Check we were passed all the maps we need, and do lookup of
     * methods for JNI up-calls
     */
    if (fontToFileMap == NULL ||
        fontToFamilyMap == NULL ||
        familyToFontListMap == NULL) {
        return;
    }
    classIDHashMap = (*env)->FindClass(env, "java/util/HashMap");
    if (classIDHashMap == NULL) {
        return;
    }
    putMID = (*env)->GetMethodID(env, classIDHashMap, "put",
                 "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    if (putMID == NULL) {
        return;
    }

    fmi.env = env;
    fmi.fontToFamilyMap = fontToFamilyMap;
    fmi.familyToFontListMap = familyToFontListMap;
    fmi.putMID = putMID;
    fmi.locale = locale;
    fmi.containsKeyMID = (*env)->GetMethodID(env, classIDHashMap, "containsKey",
                                             "(Ljava/lang/Object;)Z");
    if (fmi.containsKeyMID == NULL) {
        return;
    }

    fmi.arrayListClass = (*env)->FindClass(env, "java/util/ArrayList");
    if (fmi.arrayListClass == NULL) {
        return;
    }
    fmi.arrayListCtr = (*env)->GetMethodID(env, fmi.arrayListClass,
                                              "<init>", "(I)V");
    if (fmi.arrayListCtr == NULL) {
        return;
    }
    fmi.addMID = (*env)->GetMethodID(env, fmi.arrayListClass,
                                     "add", "(Ljava/lang/Object;)Z");
    if (fmi.addMID == NULL) {
        return;
    }
    classIDString = (*env)->FindClass(env, "java/lang/String");
    if (classIDString == NULL) {
        return;
    }
    fmi.toLowerCaseMID =
        (*env)->GetMethodID(env, classIDString, "toLowerCase",
                            "(Ljava/util/Locale;)Ljava/lang/String;");
    if (fmi.toLowerCaseMID == NULL) {
        return;
    }

    /* This HDC is initialised and released in this populate family map
     * JNI entry point, and used within the call which would otherwise
     * create many DCs.
     */
    fmi.screenDC = GetDC(NULL);
    if (fmi.screenDC == NULL) {
        return;
    }

    /* Enumerate fonts via GDI to build maps of fonts and families */
    memset(&lfw, 0, sizeof(lfw));
    lfw.lfCharSet = DEFAULT_CHARSET;  /* all charsets */
    wcscpy(lfw.lfFaceName, L"");      /* one face per family (CHECK) */
    EnumFontFamiliesExW(fmi.screenDC, &lfw,
                        (FONTENUMPROCW)EnumFamilyNamesW,
                        (LPARAM)(&fmi), 0L);

    /* Use the windows registry to map font names to files */
    ret = RegOpenKeyEx(HKEY_LOCAL_MACHINE,
                       FONTKEY_NT, 0L, KEY_READ, &hkeyFonts);
    if (ret != ERROR_SUCCESS) {
        ReleaseDC(NULL, fmi.screenDC);
        fmi.screenDC = NULL;
        return;
    }

    ret = RegQueryInfoKeyW(hkeyFonts, NULL, NULL, NULL, NULL, NULL, NULL,
                           &dwNumValues, &dwMaxValueNameLen,
                           &dwMaxValueDataLen, NULL, NULL);

    if (ret != ERROR_SUCCESS ||
        dwMaxValueNameLen >= MAX_BUFFER ||
        dwMaxValueDataLen >= MAX_BUFFER) {
        RegCloseKey(hkeyFonts);
        ReleaseDC(NULL, fmi.screenDC);
        fmi.screenDC = NULL;
        return;
    }
    for (nval = 0; nval < dwNumValues; nval++ ) {
        dwNameSize = MAX_BUFFER;
        dwDataValueSize = MAX_BUFFER;
        ret = RegEnumValueW(hkeyFonts, nval, (LPWSTR)wname, &dwNameSize,
                            NULL, &type, (LPBYTE)data, &dwDataValueSize);

        if (ret != ERROR_SUCCESS) {
            break;
        }
        if (type != REG_SZ) { /* REG_SZ means a null-terminated string */
            continue;
        }

        if (!RegistryToBaseTTNameW((LPWSTR)wname) ) {
            /* If the filename ends with ".ttf" or ".otf" also accept it.
             * REMIND : in fact not accepting .otf's for now as the
             * upstream code isn't expecting them.
             * Not expecting to need to do this for .ttc files.
             * Also note this code is not mirrored in the "A" (win9x) path.
             */
            LPWSTR dot = wcsrchr((LPWSTR)data, L'.');
            if (dot == NULL || ((wcsicmp(dot, L".ttf") != 0)
                                /* && (wcsicmp(dot, L".otf") != 0) */)) {
                continue;  /* not a TT font... */
            }
        }
        registerFontW(&fmi, fontToFileMap, (LPWSTR)wname, (LPWSTR)data);
    }
    RegCloseKey(hkeyFonts);
    ReleaseDC(NULL, fmi.screenDC);
    fmi.screenDC = NULL;
}

JNIEXPORT jstring JNICALL
Java_com_sun_javafx_font_PrismFontFactory_regReadFontLink(JNIEnv *env, jclass obj, jstring lpFontName)
{
    LONG lResult;
    BYTE* buf;
    DWORD dwBufSize = sizeof(buf);
    DWORD dwType = REG_MULTI_SZ;
    HKEY hKey;
    LPCWSTR fontpath = NULL;
    jstring linkStr;

    LPWSTR lpSubKey = L"SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\FontLink\\SystemLink";
    lResult = RegOpenKeyExW (HKEY_LOCAL_MACHINE, lpSubKey, 0, KEY_READ, &hKey);
    if (lResult != ERROR_SUCCESS)
    {
        return (jstring)NULL;
    }

    fontpath = (*env)->GetStringChars(env, lpFontName, (jboolean*) NULL);

    //get the buffer size
    lResult = RegQueryValueExW(hKey, fontpath, 0, &dwType, NULL, &dwBufSize);
    if ((lResult == ERROR_SUCCESS) && (dwBufSize > 0)) {
        buf = malloc( dwBufSize );
        if (buf == NULL) {
            (*env)->ReleaseStringChars(env, lpFontName, fontpath);
            RegCloseKey (hKey);
            return (jstring)NULL;
        }
        lResult = RegQueryValueExW(hKey, fontpath, 0, &dwType, (BYTE*)buf,
                                   &dwBufSize);
        (*env)->ReleaseStringChars(env, lpFontName, fontpath);
        RegCloseKey (hKey);

        if (lResult != ERROR_SUCCESS) {
            free(buf);
            return (jstring)NULL;
        }
    } else {
        return (jstring)NULL;
    }

    linkStr = (*env)->NewString(env, (LPWSTR)buf, dwBufSize/sizeof(WCHAR));
    free(buf);
    return linkStr;
}


typedef  unsigned short  LANGID;


#define LANGID_JA_JP   0x411
#define LANGID_ZH_CN   0x0804
#define LANGID_ZH_SG   0x1004
#define LANGID_ZH_TW   0x0404
#define LANGID_ZH_HK   0x0c04
#define LANGID_ZH_MO   0x1404
#define LANGID_KO_KR   0x0412
#define LANGID_US      0x409

static const wchar_t EUDCKEY_JA_JP[] = L"EUDC\\932";
static const wchar_t EUDCKEY_ZH_CN[] = L"EUDC\\936";
static const wchar_t EUDCKEY_ZH_TW[] = L"EUDC\\950";
static const wchar_t EUDCKEY_KO_KR[] = L"EUDC\\949";
static const wchar_t EUDCKEY_DEFAULT[] = L"EUDC\\1252";


JNIEXPORT jstring JNICALL
Java_com_sun_javafx_font_PrismFontFactory_getEUDCFontFile(JNIEnv *env, jclass cl) {
    int    rc;
    HKEY   key;
    DWORD  type;
    WCHAR  fontPathBuf[MAX_PATH + 1];
    DWORD  fontPathLen = MAX_PATH + 1;
    WCHAR  tmpPath[MAX_PATH + 1];
    LPWSTR fontPath = fontPathBuf;
    LPWSTR eudcKey = NULL;

    LANGID langID = GetSystemDefaultLangID();

    //lookup for encoding ID, EUDC only supported in
    //codepage 932, 936, 949, 950 (and unicode)
    if (langID == LANGID_JA_JP) {
        eudcKey = EUDCKEY_JA_JP;
    } else if (langID == LANGID_ZH_CN || langID == LANGID_ZH_SG) {
        eudcKey = EUDCKEY_ZH_CN;
    } else if (langID == LANGID_ZH_HK || langID == LANGID_ZH_TW ||
               langID == LANGID_ZH_MO) {
        eudcKey = EUDCKEY_ZH_TW;
    } else if (langID == LANGID_KO_KR) {
        eudcKey = EUDCKEY_KO_KR;
    } else if (langID == LANGID_US) {
        eudcKey = EUDCKEY_DEFAULT;
    } else {
        return NULL;
    }

    rc = RegOpenKeyExW(HKEY_CURRENT_USER, eudcKey, 0, KEY_READ, &key);
    if (rc != ERROR_SUCCESS) {
        return NULL;
    }
    rc = RegQueryValueExW(key,
                         L"SystemDefaultEUDCFont",
                         0,
                         &type,
                         (LPBYTE)fontPath,
                         &fontPathLen);
    RegCloseKey(key);
    fontPathLen /= sizeof(WCHAR);
    if (rc != ERROR_SUCCESS || type != REG_SZ ||
        (fontPathLen > MAX_PATH)) {
        return NULL;
    }

    fontPath[fontPathLen] = L'\0';
    if (wcsstr(fontPath, L"%SystemRoot%") == fontPath) {
        //if the fontPath includes %SystemRoot%
        LPWSTR systemRoot = _wgetenv(L"SystemRoot");
        // Subtract 12, being the length of "SystemRoot".
        if ((systemRoot == NULL) ||
           (fontPathLen-12 +wcslen(systemRoot) > MAX_PATH)) {
                return NULL;
        }
        wcscpy(tmpPath, systemRoot);
        wcscat(tmpPath, (wchar_t *)(fontPath+12));
        fontPath = tmpPath;
        fontPathLen = wcslen(tmpPath);

    } else if (wcscmp(fontPath, L"EUDC.TTE") == 0) {
        //else to see if it only inludes "EUDC.TTE"
        WCHAR systemRoot[MAX_PATH];
        UINT ret = GetWindowsDirectoryW(systemRoot, MAX_PATH);
        if ( ret != 0) {
            if (ret + 16 > MAX_PATH) {
                return NULL;
            }
            wcscpy(fontPath, systemRoot);
            wcscat(fontPath, L"\\FONTS\\EUDC.TTE");
            fontPathLen = wcslen(fontPath);
        }
        else {
            return NULL;
        }
    }
    return (*env)->NewString(env, (LPWSTR)fontPath, fontPathLen);
}

static BOOL getSysParams(NONCLIENTMETRICSW* ncmetrics) {

    OSVERSIONINFOEX osvi;
    int cbsize;

    ZeroMemory(&osvi, sizeof(OSVERSIONINFOEX));
    osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFOEX);
    if (!(GetVersionEx((OSVERSIONINFO *)&osvi))) {
        return FALSE;
    }

    // See JDK bug 6944516: specify correct size for ncmetrics on Windows XP
    // Microsoft recommend to subtract the size of the 'iPaddedBorderWidth'
    // field when running on XP. Yuck.
    if (osvi.dwMajorVersion < 6) { // 5 is XP, 6 is Vista.
        cbsize = offsetof(NONCLIENTMETRICSW, iPaddedBorderWidth);
    } else {
        cbsize = sizeof(*ncmetrics);
    }
    ZeroMemory(ncmetrics, cbsize);
    ncmetrics->cbSize = cbsize;

    return SystemParametersInfoW(SPI_GETNONCLIENTMETRICS,
                                 ncmetrics->cbSize, ncmetrics, FALSE);
}


/*
 * Class:     Java_com_sun_javafx_font_PrismFontFactory
 * Method:    getLCDContrastWin32
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_javafx_font_PrismFontFactory_getLCDContrastWin32
  (JNIEnv *env, jobject klass) {

    unsigned int fontSmoothingContrast;
    static const int fontSmoothingContrastDefault = 1300;

    return SystemParametersInfo(SPI_GETFONTSMOOTHINGCONTRAST, 0,
        &fontSmoothingContrast, 0) ? fontSmoothingContrast : fontSmoothingContrastDefault;
}

JNIEXPORT jfloat JNICALL
Java_com_sun_javafx_font_PrismFontFactory_getSystemFontSizeNative(JNIEnv *env, jclass cl)
{
    NONCLIENTMETRICSW ncmetrics;

    if (getSysParams(&ncmetrics)) {
        HWND hWnd = GetDesktopWindow();
        HDC hDC = GetDC(hWnd);
        int dpiY = GetDeviceCaps(hDC, LOGPIXELSY);
        ReleaseDC(hWnd, hDC);
        return (-ncmetrics.lfMessageFont.lfHeight)
             * ((float) USER_DEFAULT_SCREEN_DPI) / dpiY;
    } else {
        return 12.0f;
    }
}

JNIEXPORT jstring JNICALL
Java_com_sun_javafx_font_PrismFontFactory_getSystemFontNative(JNIEnv *env, jclass cl) {

    NONCLIENTMETRICSW ncmetrics;

    if (getSysParams(&ncmetrics)) {
        int len = wcslen(ncmetrics.lfMessageFont.lfFaceName);
        return (*env)->NewString(env, ncmetrics.lfMessageFont.lfFaceName, len);
    } else {
        return NULL;
    }
}


JNIEXPORT jshort JNICALL
Java_com_sun_javafx_font_PrismFontFactory_getSystemLCID(JNIEnv *env, jclass cl)
{
    LCID lcid = GetSystemDefaultLCID();
    DWORD value;

    int ret = GetLocaleInfoW(lcid,
                             LOCALE_ILANGUAGE | LOCALE_RETURN_NUMBER,
                             (LPTSTR)&value,
                             sizeof(value) / sizeof(TCHAR));
    return (jshort)value;
}

#endif /* WIN32 */
