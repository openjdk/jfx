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

#include <jni.h>
#include <stdlib.h>
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <math.h>

#include "PrismES2Defs.h"

/*
 * strJavaToC
 *
 * Returns a copy of the specified Java String object as a new,
 * null-terminated "C" string. The caller must free this string.
 */
char *strJavaToC(JNIEnv *env, jstring str)
{
    const char *strUTFBytes;    /* Array of UTF-8 bytes */
    char *cString = NULL;       /* Null-terminated "C" string */

    if (str == NULL) {
        return NULL;
    }

    strUTFBytes = (*env)->GetStringUTFChars(env, str, NULL);
    if (strUTFBytes == NULL) {
        /* Just return, since GetStringUTFChars will throw OOM if it returns NULL */
        return NULL;
    }

#if WIN32
    cString = _strdup(strUTFBytes);
#else
    cString = strdup(strUTFBytes);
#endif

    (*env)->ReleaseStringUTFChars(env, str, strUTFBytes);
    if (cString == NULL) {
        fprintf(stderr, "Out Of Memory Error");
        return NULL;
    }

    return cString;
}

/*
 * Extract the version numbers from a copy of the version string.
 * Upon return, numbers[0] contains major version number
 * numbers[1] contains minor version number
 * Note that the passed in version string is modified.
 */
void extractVersionInfo(char *versionStr, int *numbers)
{
    char *majorNumStr;
    char *minorNumStr;
    char *next_token = NULL;

    if ((versionStr == NULL) || (numbers == NULL)) {
        return;
    }

    numbers[0] = numbers[1] = -1;
#if WIN32
    majorNumStr = strtok_s(versionStr, (char *)".", &next_token);
    minorNumStr = strtok_s(0, (char *)".", &next_token);
#else
    majorNumStr = strtok(versionStr, (char *)".");
    minorNumStr = strtok(0, (char *)".");
#endif
    if (majorNumStr != NULL)
        numbers[0] = atoi(majorNumStr);
    if (minorNumStr != NULL)
        numbers[1] = atoi(minorNumStr);
}

/*
 * check if the extension is supported
 */
int isExtensionSupported(const char *allExtensions, const char *extension)
{
    const char *start;
    const char *where, *terminator;

    if ((allExtensions == NULL) || (extension == NULL)) {
        return 0;
    }

    /* Extension names should not have spaces. */
    where = (const char *) strchr(extension, ' ');
    if (where || *extension == '\0')
        return 0;

    /*
     * It takes a bit of care to be fool-proof about parsing the
     * OpenGL extensions string. Don't be fooled by sub-strings,
     * etc.
     */
    start = allExtensions;
    for (;;) {
        where = (const char *) strstr((const char *) start, extension);
        if (!where)
            break;
        terminator = where + strlen(extension);
        if (where == start || *(where - 1) == ' ')
            if (*terminator == ' ' || *terminator == '\0')
                return 1;
        start = terminator;
    }
    return 0;
}
/*
 * Class:     com_sun_prism_es2_GLFactory
 * Method:    nIsGLExtensionSupported
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_es2_GLFactory_nIsGLExtensionSupported
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jstring glExtStr) {
    char *extStr;
    jboolean result;
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (glExtStr == NULL)) {
        return JNI_FALSE;
    }
    extStr = strJavaToC(env, glExtStr);
    result = isExtensionSupported(ctxInfo->glExtensionStr, extStr) ? JNI_TRUE : JNI_FALSE;

    if (extStr != NULL) {
        free(extStr);
    }
    return result;
}

/*
 * Class:     com_sun_prism_es2_GLFactory
 * Method:    nGetGLVendor
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_prism_es2_GLFactory_nGetGLVendor
(JNIEnv *env, jclass class, jlong nativeCtxInfo) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return NULL;
    }
    return (*env)->NewStringUTF(env, ctxInfo->vendorStr);
}

/*
 * Class:     com_sun_prism_es2_GLFactory
 * Method:    nGetGLRenderer
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_prism_es2_GLFactory_nGetGLRenderer
(JNIEnv *env, jclass class, jlong nativeCtxInfo) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->rendererStr == NULL)) {
        return NULL;
    }
    return (*env)->NewStringUTF(env, ctxInfo->rendererStr);
}

/*
 * Class:     com_sun_prism_es2_GLFactory
 * Method:    nGetGLVersion
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_prism_es2_GLFactory_nGetGLVersion
(JNIEnv *env, jclass class, jlong nativeCtxInfo) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->versionStr == NULL)) {
        return NULL;
    }
    return (*env)->NewStringUTF(env, ctxInfo->versionStr);
}

