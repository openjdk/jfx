/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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

#if defined (__linux__) && ! defined (ANDROID_NDK)

#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <strings.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <unistd.h>
#include <limits.h>

#include <dlfcn.h>
#include <fontconfig/fontconfig.h>

#include <jni.h>

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL
JNI_OnLoad_javafx_font(JavaVM * vm, void * reserved) {
#ifdef JNI_VERSION_1_8
    //min. returned JNI_VERSION required by JDK8 for builtin libraries
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_8) != JNI_OK) {
        return JNI_VERSION_1_4;
    }
    return JNI_VERSION_1_8;
#else
    return JNI_VERSION_1_4;
#endif
}
#endif

/*
 * We are not explicitly linking against fontconfig. This isn't so
 * relevant for desktop platforms any more but could help on embedded
 * platforms where it may not exist.
 */

static void* openFontConfig() {

    char *homeEnv;
    static char *homeEnvStr = "HOME="; /* must be static */
    void* libfontconfig = NULL;

    /* 64 bit sparc should pick up the right version from the lib path.
     * New features may be added to libfontconfig, this is expected to
     * be compatible with old features, but we may need to start
     * distinguishing the library version, to know whether to expect
     * certain symbols - and functionality - to be available.
     * Also add explicit search for .so.1 in case .so symlink doesn't exist.
     */
    libfontconfig = dlopen("libfontconfig.so.1", RTLD_LOCAL|RTLD_LAZY);
    if (libfontconfig == NULL) {
        libfontconfig = dlopen("libfontconfig.so", RTLD_LOCAL|RTLD_LAZY);
        if (libfontconfig == NULL) {
            return NULL;
        }
    }

    /* Version 1.0 of libfontconfig crashes if HOME isn't defined in
     * the environment. This should generally never happen, but we can't
     * control it, and can't control the version of fontconfig, so iff
     * its not defined we set it to an empty value which is sufficient
     * to prevent a crash. I considered unsetting it before exit, but
     * it doesn't appear to work on Solaris, so I will leave it set.
     */
    homeEnv = getenv("HOME");
    if (homeEnv == NULL) {
        putenv(homeEnvStr);
    }

    return libfontconfig;
}

typedef void* (FcFiniFuncType)();

static void closeFontConfig(void* libfontconfig, jboolean fcFini) {

  /* NB FcFini is not in (eg) the Solaris 10 version of fontconfig. Its not
   * clear if this means we are really leaking resources in those cases
   * but it seems we should call this function when its available.
   * But since the Swing GTK code may be still accessing the lib, its probably
   * safest for now to just let this "leak" rather than potentially
   * concurrently free global data still in use by other code.
   */
#if 0
    if (fcFini) { /* release resources */
        FcFiniFuncType FcFini = (FcFiniFuncType)dlsym(libfontconfig, "FcFini");

        if (FcFini != NULL) {
            (*FcFini)();
        }
    }
#endif
    dlclose(libfontconfig);
}

typedef FcConfig* (*FcInitLoadConfigFuncType)();
typedef FcPattern* (*FcPatternBuildFuncType)(FcPattern *orig, ...);
typedef FcObjectSet* (*FcObjectSetFuncType)(const char *first, ...);
typedef FcFontSet* (*FcFontListFuncType)(FcConfig *config,
                                         FcPattern *p,
                                         FcObjectSet *os);
typedef FcResult (*FcPatternGetBoolFuncType)(const FcPattern *p,
                                               const char *object,
                                               int n,
                                               FcBool *b);
typedef FcResult (*FcPatternGetIntegerFuncType)(const FcPattern *p,
                                                const char *object,
                                                int n,
                                                int *i);
typedef FcResult (*FcPatternGetStringFuncType)(const FcPattern *p,
                                               const char *object,
                                               int n,
                                               FcChar8 ** s);
typedef void (*FcPatternDestroyFuncType)(FcPattern *p);
typedef void (*FcFontSetDestroyFuncType)(FcFontSet *s);
typedef FcPattern* (*FcNameParseFuncType)(const FcChar8 *name);
typedef FcBool (*FcPatternAddStringFuncType)(FcPattern *p,
                                             const char *object,
                                             const FcChar8 *s);
typedef void (*FcDefaultSubstituteFuncType)(FcPattern *p);
typedef FcBool (*FcConfigSubstituteFuncType)(FcConfig *config,
                                             FcPattern *p,
                                             FcMatchKind kind);
typedef FcPattern* (*FcFontMatchFuncType)(FcConfig *config,
                                          FcPattern *p,
                                          FcResult *result);
typedef FcFontSet* (*FcFontSetCreateFuncType)();
typedef FcBool (*FcFontSetAddFuncType)(FcFontSet *s, FcPattern *font);

typedef FcResult (*FcPatternGetCharSetFuncType)(FcPattern *p,
                                                const char *object,
                                                int n,
                                                FcCharSet **c);
typedef FcFontSet* (*FcFontSortFuncType)(FcConfig *config,
                                         FcPattern *p,
                                         FcBool trim,
                                         FcCharSet **csp,
                                         FcResult *result);

typedef FcCharSet* (*FcCharSetUnionFuncType)(const FcCharSet *a,
                                             const FcCharSet *b);
typedef FcChar32 (*FcCharSetSubtractCountFuncType)(const FcCharSet *a,
                                                   const FcCharSet *b);

JNIEXPORT jboolean JNICALL
Java_com_sun_javafx_font_FontConfigManager_getFontConfig
(JNIEnv *env, jclass obj, jstring localeStr,
 jobjectArray fcCompFontArray, jboolean includeFallbacks) {

    FcNameParseFuncType FcNameParse;
    FcPatternAddStringFuncType FcPatternAddString;
    FcConfigSubstituteFuncType FcConfigSubstitute;
    FcDefaultSubstituteFuncType  FcDefaultSubstitute;
    FcFontMatchFuncType FcFontMatch;
    FcPatternGetStringFuncType FcPatternGetString;
    FcPatternDestroyFuncType FcPatternDestroy;
    FcPatternGetCharSetFuncType FcPatternGetCharSet;
    FcFontSortFuncType FcFontSort;
    FcFontSetDestroyFuncType FcFontSetDestroy;
    FcCharSetUnionFuncType FcCharSetUnion;
    FcCharSetSubtractCountFuncType FcCharSetSubtractCount;

    int i, arrlen;
    jstring fcNameStr, jstr;
    const char *locale, *fcName;
    FcPattern *pattern;
    FcResult result;
    void* libfontconfig;
    jfieldID fcNameFID, fcFirstFontFID, fcAllFontsFID;
    jfieldID familyNameFID, styleNameFID, fullNameFID, fontFileFID;
    jmethodID fcFontCons;
    jclass fcCompFontClass, fcFontClass;


    // Deleting local refs as we go along so this should be plenty
    // Unlikely to matter even if it fails.
    (*env)->EnsureLocalCapacity(env, 64);
    if ((*env)->ExceptionOccurred(env)) {
        return JNI_FALSE;
    }
    fcCompFontClass =
        (*env)->FindClass(env,
                       "com/sun/javafx/font/FontConfigManager$FcCompFont");
    if ((*env)->ExceptionOccurred(env)) {
        return JNI_FALSE;
    }
    fcFontClass =
         (*env)->FindClass(env,
                       "com/sun/javafx/font/FontConfigManager$FontConfigFont");
    if ((*env)->ExceptionOccurred(env)) {
        return JNI_FALSE;
    }
    if (fcCompFontArray == NULL ||
        fcCompFontClass == NULL ||
        fcFontClass == NULL)
    {
        return JNI_FALSE;
    }

    fcNameFID = (*env)->GetFieldID(env, fcCompFontClass,
                                   "fcName", "Ljava/lang/String;");
    if ((*env)->ExceptionOccurred(env)) {
        return JNI_FALSE;
    }
    fcFirstFontFID =
        (*env)->GetFieldID(env, fcCompFontClass, "firstFont",
                  "Lcom/sun/javafx/font/FontConfigManager$FontConfigFont;");
    if ((*env)->ExceptionOccurred(env)) {
        return JNI_FALSE;
    }
    fcAllFontsFID =
        (*env)->GetFieldID(env, fcCompFontClass, "allFonts",
                  "[Lcom/sun/javafx/font/FontConfigManager$FontConfigFont;");
    if ((*env)->ExceptionOccurred(env)) {
        return JNI_FALSE;
    }
    fcFontCons = (*env)->GetMethodID(env, fcFontClass, "<init>", "()V");
    if ((*env)->ExceptionOccurred(env)) {
        return JNI_FALSE;
    }
    familyNameFID = (*env)->GetFieldID(env, fcFontClass,
                                      "familyName", "Ljava/lang/String;");
    if ((*env)->ExceptionOccurred(env)) {
        return JNI_FALSE;
    }
    styleNameFID = (*env)->GetFieldID(env, fcFontClass,
                                      "styleStr", "Ljava/lang/String;");
    if ((*env)->ExceptionOccurred(env)) {
        return JNI_FALSE;
    }
    fullNameFID = (*env)->GetFieldID(env, fcFontClass,
                                     "fullName", "Ljava/lang/String;");
    if ((*env)->ExceptionOccurred(env)) {
        return JNI_FALSE;
    }
    fontFileFID = (*env)->GetFieldID(env, fcFontClass,
                                     "fontFile", "Ljava/lang/String;");
    if ((*env)->ExceptionOccurred(env)) {
        return JNI_FALSE;
    }
    if (fcNameFID == NULL ||
        fcFirstFontFID == NULL ||
        fcAllFontsFID == NULL ||
        fcFontCons == NULL ||
        familyNameFID == NULL ||
        styleNameFID == NULL ||
        fullNameFID == NULL ||
        fontFileFID == NULL)
    {
        return JNI_FALSE;
    }

    if ((libfontconfig = openFontConfig()) == NULL) {
        return JNI_FALSE;
    }

    FcNameParse = (FcNameParseFuncType)dlsym(libfontconfig, "FcNameParse");
    FcPatternAddString =
        (FcPatternAddStringFuncType)dlsym(libfontconfig, "FcPatternAddString");
    FcConfigSubstitute =
        (FcConfigSubstituteFuncType)dlsym(libfontconfig, "FcConfigSubstitute");
    FcDefaultSubstitute = (FcDefaultSubstituteFuncType)
        dlsym(libfontconfig, "FcDefaultSubstitute");
    FcFontMatch = (FcFontMatchFuncType)dlsym(libfontconfig, "FcFontMatch");
    FcPatternGetString =
        (FcPatternGetStringFuncType)dlsym(libfontconfig, "FcPatternGetString");
    FcPatternDestroy =
        (FcPatternDestroyFuncType)dlsym(libfontconfig, "FcPatternDestroy");
    FcPatternGetCharSet =
        (FcPatternGetCharSetFuncType)dlsym(libfontconfig,
                                           "FcPatternGetCharSet");
    FcFontSort =
        (FcFontSortFuncType)dlsym(libfontconfig, "FcFontSort");
    FcFontSetDestroy =
        (FcFontSetDestroyFuncType)dlsym(libfontconfig, "FcFontSetDestroy");
    FcCharSetUnion =
        (FcCharSetUnionFuncType)dlsym(libfontconfig, "FcCharSetUnion");
    FcCharSetSubtractCount =
        (FcCharSetSubtractCountFuncType)dlsym(libfontconfig,
                                              "FcCharSetSubtractCount");

    if (FcNameParse          == NULL ||
        FcPatternAddString   == NULL ||
        FcConfigSubstitute   == NULL ||
        FcDefaultSubstitute  == NULL ||
        FcFontMatch          == NULL ||
        FcPatternGetString   == NULL ||
        FcPatternDestroy     == NULL ||
        FcPatternGetCharSet  == NULL ||
        FcFontSetDestroy     == NULL ||
        FcCharSetUnion       == NULL ||
        FcCharSetSubtractCount == NULL) {/* problem with the library: return.*/
        closeFontConfig(libfontconfig, JNI_FALSE);
        return JNI_FALSE;
    }

    locale = (*env)->GetStringUTFChars(env, localeStr, 0);

    arrlen = (*env)->GetArrayLength(env, fcCompFontArray);
    for (i=0; i<arrlen; i++) {
        FcFontSet* fontset;
        int fn, j, fontCount, nfonts;
        unsigned int minGlyphs;
        FcChar8 **family, **styleStr, **fullname, **file;
        jarray fcFontArr;
        jobject fcCompFontObj;

        fcCompFontObj = (*env)->GetObjectArrayElement(env, fcCompFontArray, i);
        if ((*env)->ExceptionOccurred(env)) {
            return JNI_FALSE;
        }
        fcNameStr =
            (jstring)((*env)->GetObjectField(env, fcCompFontObj, fcNameFID));
        fcName = (*env)->GetStringUTFChars(env, fcNameStr, 0);
        if (fcName == NULL) {
            continue;
        }
        pattern = (*FcNameParse)((FcChar8 *)fcName);
        if (pattern == NULL) {
            (*env)->ReleaseStringUTFChars(env, fcNameStr, (const char*)fcName);
            closeFontConfig(libfontconfig, JNI_FALSE);
            return JNI_FALSE;
        }

        /* locale may not usually be necessary as fontconfig appears to apply
         * this anyway based on the user's environment. However we want
         * to use the value of the JDK startup locale so this should take
         * care of it.
         */
        if (locale != NULL) {
            (*FcPatternAddString)(pattern, FC_LANG, (unsigned char*)locale);
        }
        (*FcConfigSubstitute)(NULL, pattern, FcMatchPattern);
        (*FcDefaultSubstitute)(pattern);
        fontset = (*FcFontSort)(NULL, pattern, FcTrue, NULL, &result);
        if (fontset == NULL) {
            (*FcPatternDestroy)(pattern);
            (*env)->ReleaseStringUTFChars(env, fcNameStr, (const char*)fcName);
            closeFontConfig(libfontconfig, JNI_FALSE);
            return JNI_FALSE;
        }

        /* fontconfig returned us "nfonts". It may include Type 1 fonts
         * but we are going to skip those.
         * Next create separate C arrays of length nfonts for family file etc.
         * Inspect the returned fonts and the ones we like (adds enough glyphs)
         * are added to the arrays and we increment 'fontCount'.
         */
        nfonts = fontset->nfont;
        family   = (FcChar8**)calloc(nfonts, sizeof(FcChar8*));
        styleStr = (FcChar8**)calloc(nfonts, sizeof(FcChar8*));
        fullname = (FcChar8**)calloc(nfonts, sizeof(FcChar8*));
        file     = (FcChar8**)calloc(nfonts, sizeof(FcChar8*));
        if (family == NULL || styleStr == NULL ||
            fullname == NULL || file == NULL) {
            if (family != NULL) {
                free(family);
            }
            if (styleStr != NULL) {
                free(styleStr);
            }
            if (fullname != NULL) {
                free(fullname);
            }
            if (file != NULL) {
                free(file);
            }
            (*FcPatternDestroy)(pattern);
            (*FcFontSetDestroy)(fontset);
            (*env)->ReleaseStringUTFChars(env, fcNameStr, (const char*)fcName);
            closeFontConfig(libfontconfig, JNI_FALSE);
            return JNI_FALSE;
        }
        fontCount = 0;
        minGlyphs = 20;
        FcCharSet *unionCharset = NULL;
        for (j=0; j<nfonts; j++) {
            FcPattern *fontPattern = fontset->fonts[j];
            FcChar8 *fontformat;
            FcCharSet *charset;

            fontformat = NULL;
            (*FcPatternGetString)(fontPattern, FC_FONTFORMAT, 0, &fontformat);
            /* We only want OpenType fonts for Java FX :
             * ie TrueType and CFF format fonts.
             */
            if ((fontformat != NULL) &&
                ((strcmp((char*)fontformat, "TrueType") != 0) &&
                 (strcmp((char*)fontformat, "CFF") != 0)))
            {
                continue;
            }
            result = (*FcPatternGetCharSet)(fontPattern,
                                            FC_CHARSET, 0, &charset);
            if (result != FcResultMatch) {
                free(family);
                free(fullname);
                free(styleStr);
                free(file);
                (*FcPatternDestroy)(pattern);
                (*FcFontSetDestroy)(fontset);
                (*env)->ReleaseStringUTFChars(env,
                                              fcNameStr, (const char*)fcName);
                closeFontConfig(libfontconfig, JNI_FALSE);
                return JNI_FALSE;
            }

            /* We don't want 20 or 30 fonts, so once we hit 10 fonts,
             * then require that they really be adding value. Too many
             * adversely affects load time for minimal value-add.
             * This is still likely far more than we've had in the past.
             */
            if (j==10) {
                minGlyphs = 50;
            }
            if (unionCharset == NULL) {
                unionCharset = charset;
            } else {
                if ((*FcCharSetSubtractCount)(charset, unionCharset)
                    > minGlyphs) {
                    unionCharset = (* FcCharSetUnion)(unionCharset, charset);
                } else {
                    continue;
                }
            }

            fontCount++; // found a font we will use.
            (*FcPatternGetString)(fontPattern, FC_FILE, 0, &file[j]);
            (*FcPatternGetString)(fontPattern, FC_FAMILY, 0, &family[j]);
            (*FcPatternGetString)(fontPattern, FC_STYLE, 0, &styleStr[j]);
            (*FcPatternGetString)(fontPattern, FC_FULLNAME, 0, &fullname[j]);
            if (!includeFallbacks) {
                break;
            }
            if (fontCount == 254) {
                /* Upstream Java code currently stores this in a byte;
                 * And we need one slot free for when this sequence is
                 * used as a fallback sequeunce.
                 */
                break;
            }
        }

        /* Once we get here 'fontCount' is the number of returned fonts
         * we actually want to use, so we create 'fcFontArr' of that length.
         * The non-null entries of "family[]" etc are those fonts.
         * Then loop again over all nfonts adding just those non-null ones
         * to 'fcFontArr'. If its null (we didn't want the font)
         * then we don't enter the main body.
         * So we should never get more than 'fontCount' entries.
         */
        if (includeFallbacks) {
            fcFontArr =
                (*env)->NewObjectArray(env, fontCount, fcFontClass, NULL);
            (*env)->SetObjectField(env,
                                   fcCompFontObj, fcAllFontsFID, fcFontArr);
        } else {
            fcFontArr = NULL;
        }
        fn=0;

        for (j=0;j<nfonts;j++) {
            if (family[j] != NULL) {
                jobject fcFont =
                    (*env)->NewObject(env, fcFontClass, fcFontCons);
                jstr = (*env)->NewStringUTF(env, (const char*)family[j]);
                (*env)->SetObjectField(env, fcFont, familyNameFID, jstr);
                (*env)->DeleteLocalRef(env, jstr);
                if (file[j] != NULL) {
                    jstr = (*env)->NewStringUTF(env, (const char*)file[j]);
                    (*env)->SetObjectField(env, fcFont, fontFileFID, jstr);
                    (*env)->DeleteLocalRef(env, jstr);
                }
                if (styleStr[j] != NULL) {
                    jstr = (*env)->NewStringUTF(env, (const char*)styleStr[j]);
                    (*env)->SetObjectField(env, fcFont, styleNameFID, jstr);
                    (*env)->DeleteLocalRef(env, jstr);
                }
                if (fullname[j] != NULL) {
                    jstr = (*env)->NewStringUTF(env, (const char*)fullname[j]);
                    (*env)->SetObjectField(env, fcFont, fullNameFID, jstr);
                    (*env)->DeleteLocalRef(env, jstr);
                }
                if (fn==0) {
                    (*env)->SetObjectField(env, fcCompFontObj,
                                           fcFirstFontFID, fcFont);
                }
                if (includeFallbacks && fcFontArr != NULL) {
                    (*env)->SetObjectArrayElement(env, fcFontArr, fn++,fcFont);
                    (*env)->DeleteLocalRef(env, fcFont);
                } else {
                    (*env)->DeleteLocalRef(env, fcFont);
                    break;
                }
            }
        }
        if (fcFontArr != NULL) {
            (*env)->DeleteLocalRef(env, fcFontArr);
        }
        (*env)->ReleaseStringUTFChars (env, fcNameStr, (const char*)fcName);
        (*env)->DeleteLocalRef(env, fcNameStr);
        (*FcFontSetDestroy)(fontset);
        (*FcPatternDestroy)(pattern);
        free(family);
        free(styleStr);
        free(fullname);
        free(file);
    }

    /* release resources and close the ".so" */

    if (locale) {
        (*env)->ReleaseStringUTFChars (env, localeStr, (const char*)locale);
    }
    closeFontConfig(libfontconfig, JNI_TRUE);
    return JNI_TRUE;
}


JNIEXPORT jboolean JNICALL
Java_com_sun_javafx_font_FontConfigManager_populateMapsNative
(JNIEnv *env, jclass obj,
 jobject fontToFileMap,
 jobject fontToFamilyNameMap,
 jobject familyToFontListMap,
 jobject locale
 )
{
    void *libfontconfig;
    const char *lang;
    int langLen, f;
    FcPatternBuildFuncType FcPatternBuild;
    FcObjectSetFuncType FcObjectSetBuild;
    FcFontListFuncType FcFontList;
    FcPatternGetStringFuncType FcPatternGetString;
    FcFontSetDestroyFuncType FcFontSetDestroy;
    FcPattern *pattern;
    FcObjectSet *objset;
    FcFontSet *fontSet;
    jclass classID, arrayListClass;
    jmethodID arrayListCtr, addMID, getMID;
    jmethodID toLowerCaseMID;
    jmethodID putMID, containsKeyMID;
    jboolean debugFC = getenv("PRISM_FONTCONFIG_DEBUG") != NULL;

    if (fontToFileMap == NULL ||
        fontToFamilyNameMap == NULL ||
        familyToFontListMap == NULL ||
        locale == NULL)
    {
        if (debugFC) {
            fprintf(stderr, "Null arg to native fontconfig lookup");
        }
        return JNI_FALSE;
    }
    if ((libfontconfig = openFontConfig()) == NULL) {
        if (debugFC) {
            fprintf(stderr,"Could not open libfontconfig\n");
        }
        return JNI_FALSE;
    }

    FcPatternBuild     =
        (FcPatternBuildFuncType)dlsym(libfontconfig, "FcPatternBuild");
    FcObjectSetBuild   =
        (FcObjectSetFuncType)dlsym(libfontconfig, "FcObjectSetBuild");
    FcFontList         =
        (FcFontListFuncType)dlsym(libfontconfig, "FcFontList");
    FcPatternGetString =
        (FcPatternGetStringFuncType)dlsym(libfontconfig, "FcPatternGetString");
    FcFontSetDestroy   =
        (FcFontSetDestroyFuncType)dlsym(libfontconfig, "FcFontSetDestroy");

    if (FcPatternBuild     == NULL ||
        FcObjectSetBuild   == NULL ||
        FcPatternGetString == NULL ||
        FcFontList         == NULL ||
        FcFontSetDestroy   == NULL) { /* problem with the library: return. */
        if (debugFC) {
           fprintf(stderr,"Could not find symbols in libfontconfig\n");
        }
        closeFontConfig(libfontconfig, JNI_FALSE);
        return JNI_FALSE;
    }

    // Deleting local refs as we go along so this should be plenty
    // Unlikely to matter even if it fails.
    (*env)->EnsureLocalCapacity(env, 64);
    if ((*env)->ExceptionOccurred(env)) {
        return JNI_FALSE;
    }
    classID = (*env)->FindClass(env, "java/util/HashMap");
    if ((*env)->ExceptionOccurred(env) || classID == NULL) {
        return JNI_FALSE;
    }
    getMID = (*env)->GetMethodID(env, classID, "get",
                 "(Ljava/lang/Object;)Ljava/lang/Object;");
    if ((*env)->ExceptionOccurred(env) || getMID == NULL) {
        return JNI_FALSE;
    }
    putMID = (*env)->GetMethodID(env, classID, "put",
                 "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    if ((*env)->ExceptionOccurred(env) || putMID == NULL) {
        return JNI_FALSE;
    }

    containsKeyMID = (*env)->GetMethodID(env, classID, "containsKey",
                                             "(Ljava/lang/Object;)Z");
    if ((*env)->ExceptionOccurred(env) || containsKeyMID == NULL) {
        return JNI_FALSE;
    }

    arrayListClass = (*env)->FindClass(env, "java/util/ArrayList");
    if ((*env)->ExceptionOccurred(env) || arrayListClass == NULL) {
        return JNI_FALSE;
    }
    arrayListCtr = (*env)->GetMethodID(env, arrayListClass,
                                              "<init>", "(I)V");
    if ((*env)->ExceptionOccurred(env) || arrayListCtr == NULL) {
        return JNI_FALSE;
    }
    addMID = (*env)->GetMethodID(env, arrayListClass,
                                 "add", "(Ljava/lang/Object;)Z");
    if ((*env)->ExceptionOccurred(env) || addMID == NULL) {
        return JNI_FALSE;
    }

    classID = (*env)->FindClass(env, "java/lang/String");
    if ((*env)->ExceptionOccurred(env) || classID == NULL) {
        return JNI_FALSE;
    }
    toLowerCaseMID =
        (*env)->GetMethodID(env, classID, "toLowerCase",
                            "(Ljava/util/Locale;)Ljava/lang/String;");
    if ((*env)->ExceptionOccurred(env) || toLowerCaseMID == NULL) {
        return JNI_FALSE;
    }
    pattern = (*FcPatternBuild)(NULL, FC_OUTLINE, FcTypeBool, FcTrue, NULL);
    objset = (*FcObjectSetBuild)(FC_FAMILY, FC_FAMILYLANG,
                                 FC_FULLNAME, FC_FULLNAMELANG,
                                 FC_FILE, FC_FONTFORMAT, NULL);
    fontSet = (*FcFontList)(NULL, pattern, objset);

    if (debugFC) {
        fprintf(stderr,"Fontconfig found %d fonts\n", fontSet->nfont);
    }

    for (f=0; f < fontSet->nfont; f++) {
        int n=0, done=0;
        FcPattern *fp = fontSet->fonts[f];

        FcChar8 *family = NULL;
        FcChar8 *familyEN = NULL;
        FcChar8 *familyLang = NULL;
        FcChar8 *fullName = NULL;
        FcChar8 *fullNameEN = NULL;
        FcChar8 *fullNameLang = NULL;
        FcChar8 *file;
        FcResult res;
        jstring jFileStr;
        jstring jFamilyStr, jFamilyStrLC;
        jstring jFullNameStr, jFullNameStrLC;
        jobject jList;
        FcChar8 *format = NULL;

        /* We only want TrueType & OpenType fonts for Java FX */
        format = NULL;
        if ((*FcPatternGetString)(fp, FC_FONTFORMAT, 0, &format)
            != FcResultMatch) {
            continue;
        }
        if (format == NULL ||
            ((strcmp((char*)format, "TrueType") != 0) &&
             (strcmp((char*)format, "CFF") != 0))) {
            continue;
        }
        if ((*FcPatternGetString)(fp, FC_FILE, 0, &file) != FcResultMatch) {
            continue;
        } else {
            char pathname[PATH_MAX+1];
            char* path = realpath((char*)file, pathname);
            if (path == NULL) {
                continue;
            } else {
                file = (FcChar8*)path;
            }
        }
        n=0;
        while (!done) {
            family = NULL;
            familyLang = NULL;
            fullName = NULL;
            fullNameLang = NULL;

            if (((*FcPatternGetString)(fp, FC_FAMILY, n, &family)
                == FcResultMatch) &&
                ((*FcPatternGetString)(fp, FC_FAMILYLANG, n, &familyLang)
                == FcResultMatch) &&
                (family != NULL && familyLang != NULL) &&
                (familyEN == NULL || (strcmp((char*)familyLang, "en") == 0)))
            {
                familyEN = family;
            }
            if (((*FcPatternGetString)(fp, FC_FULLNAME, n, &fullName)
                == FcResultMatch) &&
                ((*FcPatternGetString)(fp, FC_FULLNAMELANG, n, &fullNameLang)
                == FcResultMatch) &&
                (fullName != NULL && fullNameLang != NULL) &&
                (fullNameEN == NULL ||
                 (strcmp((char*)fullNameLang,"en") == 0)))
            {
                fullNameEN = fullName;
            }
            if (family == NULL && fullName == NULL) {
                done = 1;
                break;
            }
            n++;
        }

        if (debugFC) {
            fprintf(stderr,"Read FC font family=%s fullname=%s file=%s\n",
                    (familyEN == NULL) ? "null" : (char*)familyEN,
                    (fullNameEN == NULL) ? "null" : (char*)fullNameEN,
                    (file == NULL) ? "null" : (char*)file);
            fflush(stderr);
        }

        /* We set the names from the first found names for a font, updating
         * to the English ones as they are found. If these are null
         * we must not have found any name, so we'd better skip.
         */
        if (familyEN == NULL || fullNameEN == NULL || file == NULL) {
            if (debugFC) {
                fprintf(stderr,"FC: Skipping on error for above font\n");
                fflush(stderr);
            }
            continue;
        }

        jFileStr = (*env)->NewStringUTF(env, (const char*)file);
        jFamilyStr = (*env)->NewStringUTF(env, (const char*)familyEN);
        jFullNameStr = (*env)->NewStringUTF(env, (const char*)fullNameEN);

        if (jFileStr == NULL || jFamilyStr == NULL || jFullNameStr == NULL) {
            if (debugFC) {
                fprintf(stderr,"Failed to create string object");
            }
            continue;
        }

        jFamilyStrLC = (*env)->CallObjectMethod(env, jFamilyStr,
                                                toLowerCaseMID, locale);
        if ((*env)->ExceptionOccurred(env)) {
            return JNI_FALSE;
        }
        jFullNameStrLC = (*env)->CallObjectMethod(env, jFullNameStr,
                                                  toLowerCaseMID, locale);
        if ((*env)->ExceptionOccurred(env)) {
            return JNI_FALSE;
        }
        if (jFamilyStrLC == NULL || jFullNameStrLC == NULL) {
            if (debugFC) {
                fprintf(stderr,"Failed to create lower case string object");
                fflush(stderr);
            }
            continue;
        }

        (*env)->CallObjectMethod(env, fontToFileMap, putMID,
                                 jFullNameStrLC, jFileStr);
        if ((*env)->ExceptionOccurred(env)) {
            return JNI_FALSE;
        }
        (*env)->CallObjectMethod(env, fontToFamilyNameMap, putMID,
                                 jFullNameStrLC, jFamilyStr);
        if ((*env)->ExceptionOccurred(env)) {
            return JNI_FALSE;
        }
        jList = (*env)->CallObjectMethod(env, familyToFontListMap,
                                         getMID, jFamilyStrLC);
        if ((*env)->ExceptionOccurred(env)) {
            return JNI_FALSE;
        }
        if (jList == NULL) {
            jList = (*env)->NewObject(env, arrayListClass, arrayListCtr, 4);
            if ((*env)->ExceptionOccurred(env)) {
                return JNI_FALSE;
            }
            (*env)->CallObjectMethod(env, familyToFontListMap,
                                     putMID, jFamilyStrLC, jList);
            if ((*env)->ExceptionOccurred(env)) {
                return JNI_FALSE;
            }
        }
        if (jList == NULL) {
            if (debugFC) {
                fprintf(stderr,"Fontconfig: List is null\n");
                fflush(stderr);
            }
            continue;
        }
        (*env)->CallObjectMethod(env, jList, addMID, jFullNameStr);
        if ((*env)->ExceptionOccurred(env)) {
            return JNI_FALSE;
        }
        /* Now referenced from the passed in maps, so can delete local refs. */
        (*env)->DeleteLocalRef(env, jFileStr);
        (*env)->DeleteLocalRef(env, jFamilyStr);
        (*env)->DeleteLocalRef(env, jFamilyStrLC);
        (*env)->DeleteLocalRef(env, jFullNameStr);
        (*env)->DeleteLocalRef(env, jFullNameStrLC);
        (*env)->DeleteLocalRef(env, jList);

    }
    if (debugFC) {
        fprintf(stderr,"Done enumerating fontconfig fonts\n");
        fflush(stderr);
    }
    (*FcFontSetDestroy)(fontSet);
    closeFontConfig(libfontconfig, JNI_TRUE);

    return JNI_TRUE;
}


#endif /* __linux__ */
