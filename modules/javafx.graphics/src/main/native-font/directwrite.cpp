/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <dwrite.h>
#include <d2d1.h>
#include <wincodec.h>
#include <vector>
#include <new>

#include <com_sun_javafx_font_directwrite_OS.h>

#define OS_NATIVE(func) Java_com_sun_javafx_font_directwrite_OS_##func

/* DirectWrite is not available on all platforms. */
typedef HRESULT (WINAPI*DWriteCreateFactoryProc)(
  DWRITE_FACTORY_TYPE factoryType,
  REFIID iid,
  IUnknown **factory
);

/* Direct2D is not available on all platforms. */
typedef HRESULT (WINAPI*D2D1CreateFactoryProc)(
  D2D1_FACTORY_TYPE factoryType,
  REFIID iid,
  const D2D1_FACTORY_OPTIONS *pFactoryOptions,
  void **factory
);

static jboolean checkAndClearException(JNIEnv* env)
{
    jthrowable t = env->ExceptionOccurred();
    if (!t) {
        return JNI_FALSE;
    }
    env->ExceptionClear();
    return JNI_TRUE;
}

/**************************************************************************/
/*                                                                        */
/*                            Structs                                     */
/*                                                                        */
/**************************************************************************/

typedef struct DWRITE_GLYPH_METRICS_FID_CACHE {
    int cached;
    jclass clazz;
    jfieldID leftSideBearing, advanceWidth, rightSideBearing, topSideBearing, advanceHeight, bottomSideBearing, verticalOriginY;
    jmethodID init;
} DWRITE_GLYPH_METRICS_FID_CACHE;

DWRITE_GLYPH_METRICS_FID_CACHE DWRITE_GLYPH_METRICSFc;

void cacheDWRITE_GLYPH_METRICSFields(JNIEnv *env)
{
    if (DWRITE_GLYPH_METRICSFc.cached) return;
    jclass tmpClass = env->FindClass("com/sun/javafx/font/directwrite/DWRITE_GLYPH_METRICS");
    if (checkAndClearException(env) || !tmpClass) {
        fprintf(stderr, "cacheDWRITE_GLYPH_METRICSFields error: JNI exception or tmpClass == NULL");
        return;
    }
    DWRITE_GLYPH_METRICSFc.clazz =  (jclass)env->NewGlobalRef(tmpClass);
    DWRITE_GLYPH_METRICSFc.leftSideBearing = env->GetFieldID(DWRITE_GLYPH_METRICSFc.clazz, "leftSideBearing", "I");
    if (checkAndClearException(env) || !DWRITE_GLYPH_METRICSFc.leftSideBearing) {
        fprintf(stderr, "cacheDWRITE_GLYPH_METRICSFields error: JNI exception or leftSideBearing == NULL");
        return;
    }
    DWRITE_GLYPH_METRICSFc.advanceWidth = env->GetFieldID(DWRITE_GLYPH_METRICSFc.clazz, "advanceWidth", "I");
    if (checkAndClearException(env) || !DWRITE_GLYPH_METRICSFc.advanceWidth) {
        fprintf(stderr, "cacheDWRITE_GLYPH_METRICSFields error: JNI exception or advanceWidth == NULL");
        return;
    }
    DWRITE_GLYPH_METRICSFc.rightSideBearing = env->GetFieldID(DWRITE_GLYPH_METRICSFc.clazz, "rightSideBearing", "I");
    if (checkAndClearException(env) || !DWRITE_GLYPH_METRICSFc.rightSideBearing) {
        fprintf(stderr, "cacheDWRITE_GLYPH_METRICSFields error: JNI exception or rightSideBearing == NULL");
        return;
    }
    DWRITE_GLYPH_METRICSFc.topSideBearing = env->GetFieldID(DWRITE_GLYPH_METRICSFc.clazz, "topSideBearing", "I");
    if (checkAndClearException(env) || !DWRITE_GLYPH_METRICSFc.topSideBearing) {
        fprintf(stderr, "cacheDWRITE_GLYPH_METRICSFields error: JNI exception or topSideBearing == NULL");
        return;
    }
    DWRITE_GLYPH_METRICSFc.advanceHeight = env->GetFieldID(DWRITE_GLYPH_METRICSFc.clazz, "advanceHeight", "I");
    if (checkAndClearException(env) || !DWRITE_GLYPH_METRICSFc.advanceHeight) {
        fprintf(stderr, "cacheDWRITE_GLYPH_METRICSFields error: JNI exception or advanceHeight == NULL");
        return;
    }
    DWRITE_GLYPH_METRICSFc.bottomSideBearing = env->GetFieldID(DWRITE_GLYPH_METRICSFc.clazz, "bottomSideBearing", "I");
    if (checkAndClearException(env) || !DWRITE_GLYPH_METRICSFc.bottomSideBearing) {
        fprintf(stderr, "cacheDWRITE_GLYPH_METRICSFields error: JNI exception or bottomSideBearing == NULL");
        return;
    }
    DWRITE_GLYPH_METRICSFc.verticalOriginY = env->GetFieldID(DWRITE_GLYPH_METRICSFc.clazz, "verticalOriginY", "I");
    if (checkAndClearException(env) || !DWRITE_GLYPH_METRICSFc.verticalOriginY) {
        fprintf(stderr, "cacheDWRITE_GLYPH_METRICSFields error: JNI exception or verticalOriginY == NULL");
        return;
    }
    DWRITE_GLYPH_METRICSFc.init = env->GetMethodID(DWRITE_GLYPH_METRICSFc.clazz, "<init>", "()V");
    if (checkAndClearException(env) || !DWRITE_GLYPH_METRICSFc.init) {
        fprintf(stderr, "cacheDWRITE_GLYPH_METRICSFields error: JNI exception or <init> == NULL");
        return;
    }
    DWRITE_GLYPH_METRICSFc.cached = 1;
}

DWRITE_GLYPH_METRICS *getDWRITE_GLYPH_METRICSFields(JNIEnv *env, jobject lpObject, DWRITE_GLYPH_METRICS *lpStruct)
{
    if (!DWRITE_GLYPH_METRICSFc.cached) cacheDWRITE_GLYPH_METRICSFields(env);
    lpStruct->leftSideBearing = env->GetIntField(lpObject, DWRITE_GLYPH_METRICSFc.leftSideBearing);
    lpStruct->advanceWidth = env->GetIntField(lpObject, DWRITE_GLYPH_METRICSFc.advanceWidth);
    lpStruct->rightSideBearing = env->GetIntField(lpObject, DWRITE_GLYPH_METRICSFc.rightSideBearing);
    lpStruct->topSideBearing = env->GetIntField(lpObject, DWRITE_GLYPH_METRICSFc.topSideBearing);
    lpStruct->advanceHeight = env->GetIntField(lpObject, DWRITE_GLYPH_METRICSFc.advanceHeight);
    lpStruct->bottomSideBearing = env->GetIntField(lpObject, DWRITE_GLYPH_METRICSFc.bottomSideBearing);
    lpStruct->verticalOriginY = env->GetIntField(lpObject, DWRITE_GLYPH_METRICSFc.verticalOriginY);
    return lpStruct;
}

void setDWRITE_GLYPH_METRICSFields(JNIEnv *env, jobject lpObject, DWRITE_GLYPH_METRICS *lpStruct)
{
    if (!DWRITE_GLYPH_METRICSFc.cached) cacheDWRITE_GLYPH_METRICSFields(env);
    env->SetIntField(lpObject, DWRITE_GLYPH_METRICSFc.leftSideBearing, (jint)lpStruct->leftSideBearing);
    env->SetIntField(lpObject, DWRITE_GLYPH_METRICSFc.advanceWidth, (jint)lpStruct->advanceWidth);
    env->SetIntField(lpObject, DWRITE_GLYPH_METRICSFc.rightSideBearing, (jint)lpStruct->rightSideBearing);
    env->SetIntField(lpObject, DWRITE_GLYPH_METRICSFc.topSideBearing, (jint)lpStruct->topSideBearing);
    env->SetIntField(lpObject, DWRITE_GLYPH_METRICSFc.advanceHeight, (jint)lpStruct->advanceHeight);
    env->SetIntField(lpObject, DWRITE_GLYPH_METRICSFc.bottomSideBearing, (jint)lpStruct->bottomSideBearing);
    env->SetIntField(lpObject, DWRITE_GLYPH_METRICSFc.verticalOriginY, (jint)lpStruct->verticalOriginY);
}

jobject newDWRITE_GLYPH_METRICS(JNIEnv *env, DWRITE_GLYPH_METRICS *lpStruct)
{
    jobject lpObject = NULL;
    if (!DWRITE_GLYPH_METRICSFc.cached) cacheDWRITE_GLYPH_METRICSFields(env);
    lpObject = env->NewObject(DWRITE_GLYPH_METRICSFc.clazz, DWRITE_GLYPH_METRICSFc.init);
    if (lpObject && lpStruct) setDWRITE_GLYPH_METRICSFields(env, lpObject, lpStruct);
    return lpObject;
}

typedef struct DWRITE_MATRIX_FID_CACHE {
    int cached;
    jclass clazz;
    jfieldID m11, m12, m21, m22, dx, dy;
    jmethodID init;
} DWRITE_MATRIX_FID_CACHE;

DWRITE_MATRIX_FID_CACHE DWRITE_MATRIXFc;

void cacheDWRITE_MATRIXFields(JNIEnv *env)
{
    if (DWRITE_MATRIXFc.cached) return;
    jclass tmpClass = env->FindClass("com/sun/javafx/font/directwrite/DWRITE_MATRIX");
    if (checkAndClearException(env) || !tmpClass) {
        fprintf(stderr, "cacheDWRITE_MATRIXFields error: JNI exception or tmpClass == NULL");
        return;
    }
    DWRITE_MATRIXFc.clazz =  (jclass)env->NewGlobalRef(tmpClass);
    DWRITE_MATRIXFc.m11 = env->GetFieldID(DWRITE_MATRIXFc.clazz, "m11", "F");
    if (checkAndClearException(env) || !DWRITE_MATRIXFc.m11) {
        fprintf(stderr, "cacheDWRITE_MATRIXFields error: JNI exception or m11 == NULL");
        return;
    }
    DWRITE_MATRIXFc.m12 = env->GetFieldID(DWRITE_MATRIXFc.clazz, "m12", "F");
    if (checkAndClearException(env) || !DWRITE_MATRIXFc.m12) {
        fprintf(stderr, "cacheDWRITE_MATRIXFields error: JNI exception or m12 == NULL");
        return;
    }
    DWRITE_MATRIXFc.m21 = env->GetFieldID(DWRITE_MATRIXFc.clazz, "m21", "F");
    if (checkAndClearException(env) || !DWRITE_MATRIXFc.m21) {
        fprintf(stderr, "cacheDWRITE_MATRIXFields error: JNI exception or m21 == NULL");
        return;
    }
    DWRITE_MATRIXFc.m22 = env->GetFieldID(DWRITE_MATRIXFc.clazz, "m22", "F");
    if (checkAndClearException(env) || !DWRITE_MATRIXFc.m22) {
        fprintf(stderr, "cacheDWRITE_MATRIXFields error: JNI exception or m22 == NULL");
        return;
    }
    DWRITE_MATRIXFc.dx = env->GetFieldID(DWRITE_MATRIXFc.clazz, "dx", "F");
    if (checkAndClearException(env) || !DWRITE_MATRIXFc.dx) {
        fprintf(stderr, "cacheDWRITE_MATRIXFields error: JNI exception or dx == NULL");
        return;
    }
    DWRITE_MATRIXFc.dy = env->GetFieldID(DWRITE_MATRIXFc.clazz, "dy", "F");
    if (checkAndClearException(env) || !DWRITE_MATRIXFc.dy) {
        fprintf(stderr, "cacheDWRITE_MATRIXFields error: JNI exception or dy == NULL");
        return;
    }
    DWRITE_MATRIXFc.init = env->GetMethodID(DWRITE_MATRIXFc.clazz, "<init>", "()V");
    if (checkAndClearException(env) || !DWRITE_MATRIXFc.init) {
        fprintf(stderr, "cacheDWRITE_MATRIXFields error: JNI exception or <init> == NULL");
        return;
    }
    DWRITE_MATRIXFc.cached = 1;
}

DWRITE_MATRIX *getDWRITE_MATRIXFields(JNIEnv *env, jobject lpObject, DWRITE_MATRIX *lpStruct)
{
    if (!DWRITE_MATRIXFc.cached) cacheDWRITE_MATRIXFields(env);
    lpStruct->m11 = env->GetFloatField(lpObject, DWRITE_MATRIXFc.m11);
    lpStruct->m12 = env->GetFloatField(lpObject, DWRITE_MATRIXFc.m12);
    lpStruct->m21 = env->GetFloatField(lpObject, DWRITE_MATRIXFc.m21);
    lpStruct->m22 = env->GetFloatField(lpObject, DWRITE_MATRIXFc.m22);
    lpStruct->dx = env->GetFloatField(lpObject, DWRITE_MATRIXFc.dx);
    lpStruct->dy = env->GetFloatField(lpObject, DWRITE_MATRIXFc.dy);
    return lpStruct;
}

void setDWRITE_MATRIXFields(JNIEnv *env, jobject lpObject, DWRITE_MATRIX *lpStruct)
{
    if (!DWRITE_MATRIXFc.cached) cacheDWRITE_MATRIXFields(env);
    env->SetFloatField(lpObject, DWRITE_MATRIXFc.m11, (jfloat)lpStruct->m11);
    env->SetFloatField(lpObject, DWRITE_MATRIXFc.m12, (jfloat)lpStruct->m12);
    env->SetFloatField(lpObject, DWRITE_MATRIXFc.m21, (jfloat)lpStruct->m21);
    env->SetFloatField(lpObject, DWRITE_MATRIXFc.m22, (jfloat)lpStruct->m22);
    env->SetFloatField(lpObject, DWRITE_MATRIXFc.dx, (jfloat)lpStruct->dx);
    env->SetFloatField(lpObject, DWRITE_MATRIXFc.dy, (jfloat)lpStruct->dy);
}

jobject newDWRITE_MATRIX(JNIEnv *env, DWRITE_MATRIX *lpStruct)
{
    jobject lpObject = NULL;
    if (!DWRITE_MATRIXFc.cached) cacheDWRITE_MATRIXFields(env);
    lpObject = env->NewObject(DWRITE_MATRIXFc.clazz, DWRITE_MATRIXFc.init);
    if (lpObject && lpStruct) setDWRITE_MATRIXFields(env, lpObject, lpStruct);
    return lpObject;
}

typedef struct DWRITE_GLYPH_RUN_FID_CACHE {
    int cached;
    jclass clazz;
    jfieldID fontFace, fontEmSize, glyphIndices, glyphAdvances, advanceOffset, ascenderOffset, isSideways, bidiLevel;
    jmethodID init;
} DWRITE_GLYPH_RUN_FID_CACHE;

DWRITE_GLYPH_RUN_FID_CACHE DWRITE_GLYPH_RUNFc;

void cacheDWRITE_GLYPH_RUNFields(JNIEnv *env)
{
    if (DWRITE_GLYPH_RUNFc.cached) return;
    jclass tmpClass = env->FindClass("com/sun/javafx/font/directwrite/DWRITE_GLYPH_RUN");
    if (checkAndClearException(env) || !tmpClass) {
        fprintf(stderr, "cacheDWRITE_GLYPH_RUNFields error: JNI exception or tmpClass == NULL");
        return;
    }
    DWRITE_GLYPH_RUNFc.clazz =  (jclass)env->NewGlobalRef(tmpClass);
    DWRITE_GLYPH_RUNFc.fontFace = env->GetFieldID(DWRITE_GLYPH_RUNFc.clazz, "fontFace", "J");
    if (checkAndClearException(env) || !DWRITE_GLYPH_RUNFc.fontFace) {
        fprintf(stderr, "cacheDWRITE_GLYPH_RUNFields error: JNI exception or fontFace == NULL");
        return;
    }
    DWRITE_GLYPH_RUNFc.fontEmSize = env->GetFieldID(DWRITE_GLYPH_RUNFc.clazz, "fontEmSize", "F");
    if (checkAndClearException(env) || !DWRITE_GLYPH_RUNFc.fontEmSize) {
        fprintf(stderr, "cacheDWRITE_GLYPH_RUNFields error: JNI exception or fontEmSize == NULL");
        return;
    }
    DWRITE_GLYPH_RUNFc.glyphIndices = env->GetFieldID(DWRITE_GLYPH_RUNFc.clazz, "glyphIndices", "S");
    if (checkAndClearException(env) || !DWRITE_GLYPH_RUNFc.glyphIndices) {
        fprintf(stderr, "cacheDWRITE_GLYPH_RUNFields error: JNI exception or glyphIndices == NULL");
        return;
    }
    DWRITE_GLYPH_RUNFc.glyphAdvances = env->GetFieldID(DWRITE_GLYPH_RUNFc.clazz, "glyphAdvances", "F");
    if (checkAndClearException(env) || !DWRITE_GLYPH_RUNFc.glyphAdvances) {
        fprintf(stderr, "cacheDWRITE_GLYPH_RUNFields error: JNI exception or glyphAdvances == NULL");
        return;
    }
    DWRITE_GLYPH_RUNFc.advanceOffset = env->GetFieldID(DWRITE_GLYPH_RUNFc.clazz, "advanceOffset", "F");
    if (checkAndClearException(env) || !DWRITE_GLYPH_RUNFc.advanceOffset) {
        fprintf(stderr, "cacheDWRITE_GLYPH_RUNFields error: JNI exception or advanceOffset == NULL");
        return;
    }
    DWRITE_GLYPH_RUNFc.ascenderOffset = env->GetFieldID(DWRITE_GLYPH_RUNFc.clazz, "ascenderOffset", "F");
    if (checkAndClearException(env) || !DWRITE_GLYPH_RUNFc.ascenderOffset) {
        fprintf(stderr, "cacheDWRITE_GLYPH_RUNFields error: JNI exception or ascenderOffset == NULL");
        return;
    }
    DWRITE_GLYPH_RUNFc.isSideways = env->GetFieldID(DWRITE_GLYPH_RUNFc.clazz, "isSideways", "Z");
    if (checkAndClearException(env) || !DWRITE_GLYPH_RUNFc.isSideways) {
        fprintf(stderr, "cacheDWRITE_GLYPH_RUNFields error: JNI exception or isSideways == NULL");
        return;
    }
    DWRITE_GLYPH_RUNFc.bidiLevel = env->GetFieldID(DWRITE_GLYPH_RUNFc.clazz, "bidiLevel", "I");
    if (checkAndClearException(env) || !DWRITE_GLYPH_RUNFc.bidiLevel) {
        fprintf(stderr, "cacheDWRITE_GLYPH_RUNFields error: JNI exception or bidiLevel == NULL");
        return;
    }
    DWRITE_GLYPH_RUNFc.init = env->GetMethodID(DWRITE_GLYPH_RUNFc.clazz, "<init>", "()V");
    if (checkAndClearException(env) || !DWRITE_GLYPH_RUNFc.init) {
        fprintf(stderr, "cacheDWRITE_GLYPH_RUNFields error: JNI exception or <init> == NULL");
        return;
    }
    DWRITE_GLYPH_RUNFc.cached = 1;
}

DWRITE_GLYPH_RUN *getDWRITE_GLYPH_RUNFields(JNIEnv *env, jobject lpObject, DWRITE_GLYPH_RUN *lpStruct)
{
    if (!DWRITE_GLYPH_RUNFc.cached) cacheDWRITE_GLYPH_RUNFields(env);
    lpStruct->fontFace = (IDWriteFontFace *)env->GetLongField(lpObject, DWRITE_GLYPH_RUNFc.fontFace);
    lpStruct->fontEmSize = env->GetFloatField(lpObject, DWRITE_GLYPH_RUNFc.fontEmSize);
    ((jshort*)lpStruct->glyphIndices)[0] = env->GetShortField(lpObject, DWRITE_GLYPH_RUNFc.glyphIndices);
    ((float*)lpStruct->glyphAdvances)[0] = env->GetFloatField(lpObject, DWRITE_GLYPH_RUNFc.glyphAdvances);
    ((float)lpStruct->glyphOffsets[0].advanceOffset) = env->GetFloatField(lpObject, DWRITE_GLYPH_RUNFc.advanceOffset);
    ((float)lpStruct->glyphOffsets[0].ascenderOffset) = env->GetFloatField(lpObject, DWRITE_GLYPH_RUNFc.ascenderOffset);
    lpStruct->isSideways = env->GetBooleanField(lpObject, DWRITE_GLYPH_RUNFc.isSideways);
    lpStruct->bidiLevel = env->GetIntField(lpObject, DWRITE_GLYPH_RUNFc.bidiLevel);
    return lpStruct;
}

typedef struct DWRITE_SCRIPT_ANALYSIS_FID_CACHE {
    int cached;
    jclass clazz;
    jfieldID script, shapes;
    jmethodID init;
} DWRITE_SCRIPT_ANALYSIS_FID_CACHE;

DWRITE_SCRIPT_ANALYSIS_FID_CACHE DWRITE_SCRIPT_ANALYSISFc;

void cacheDWRITE_SCRIPT_ANALYSISFields(JNIEnv *env)
{
    if (DWRITE_SCRIPT_ANALYSISFc.cached) return;
    jclass tmpClass = env->FindClass("com/sun/javafx/font/directwrite/DWRITE_SCRIPT_ANALYSIS");
    if (checkAndClearException(env) || !tmpClass) {
        fprintf(stderr, "cacheDWRITE_SCRIPT_ANALYSISFields error: JNI exception or tmpClass == NULL");
        return;
    }
    DWRITE_SCRIPT_ANALYSISFc.clazz =  (jclass)env->NewGlobalRef(tmpClass);
    DWRITE_SCRIPT_ANALYSISFc.script = env->GetFieldID(DWRITE_SCRIPT_ANALYSISFc.clazz, "script", "S");
    if (checkAndClearException(env) || !DWRITE_SCRIPT_ANALYSISFc.script) {
        fprintf(stderr, "cacheDWRITE_SCRIPT_ANALYSISFields error: JNI exception or script == NULL");
        return;
    }
    DWRITE_SCRIPT_ANALYSISFc.shapes = env->GetFieldID(DWRITE_SCRIPT_ANALYSISFc.clazz, "shapes", "I");
    if (checkAndClearException(env) || !DWRITE_SCRIPT_ANALYSISFc.shapes) {
        fprintf(stderr, "cacheDWRITE_SCRIPT_ANALYSISFields error: JNI exception or shapes == NULL");
        return;
    }
    DWRITE_SCRIPT_ANALYSISFc.init = env->GetMethodID(DWRITE_SCRIPT_ANALYSISFc.clazz, "<init>", "()V");
    if (checkAndClearException(env) || !DWRITE_SCRIPT_ANALYSISFc.init) {
        fprintf(stderr, "cacheDWRITE_SCRIPT_ANALYSISFields error: JNI exception or init == NULL");
        return;
    }
    DWRITE_SCRIPT_ANALYSISFc.cached = 1;
}

DWRITE_SCRIPT_ANALYSIS *getDWRITE_SCRIPT_ANALYSISFields(JNIEnv *env, jobject lpObject, DWRITE_SCRIPT_ANALYSIS *lpStruct)
{
    if (!DWRITE_SCRIPT_ANALYSISFc.cached) cacheDWRITE_SCRIPT_ANALYSISFields(env);
    lpStruct->script = env->GetShortField(lpObject, DWRITE_SCRIPT_ANALYSISFc.script);
    lpStruct->shapes = (DWRITE_SCRIPT_SHAPES)env->GetIntField(lpObject, DWRITE_SCRIPT_ANALYSISFc.shapes);
    return lpStruct;
}

void setDWRITE_SCRIPT_ANALYSISFields(JNIEnv *env, jobject lpObject, DWRITE_SCRIPT_ANALYSIS *lpStruct)
{
    if (!DWRITE_SCRIPT_ANALYSISFc.cached) cacheDWRITE_SCRIPT_ANALYSISFields(env);
    env->SetShortField(lpObject, DWRITE_SCRIPT_ANALYSISFc.script, (jshort)lpStruct->script);
    env->SetIntField(lpObject, DWRITE_SCRIPT_ANALYSISFc.shapes, (jint)lpStruct->shapes);
}

jobject newDWRITE_SCRIPT_ANALYSIS(JNIEnv *env, DWRITE_SCRIPT_ANALYSIS *lpStruct)
{
    jobject lpObject = NULL;
    if (!DWRITE_SCRIPT_ANALYSISFc.cached) cacheDWRITE_SCRIPT_ANALYSISFields(env);
    lpObject = env->NewObject(DWRITE_SCRIPT_ANALYSISFc.clazz, DWRITE_SCRIPT_ANALYSISFc.init);
    if (lpObject && lpStruct) setDWRITE_SCRIPT_ANALYSISFields(env, lpObject, lpStruct);
    return lpObject;
}

typedef struct RECT_FID_CACHE {
    int cached;
    jclass clazz;
    jfieldID left, top, right, bottom;
    jmethodID init;
} RECT_FID_CACHE;

RECT_FID_CACHE RECTFc;

void cacheRECTFields(JNIEnv *env)
{
    if (RECTFc.cached) return;
    jclass tmpClass = env->FindClass("com/sun/javafx/font/directwrite/RECT");
    if (checkAndClearException(env) || !tmpClass) {
        fprintf(stderr, "cacheRECTFields error: JNI exception or tmpClass == NULL");
        return;
    }
    RECTFc.clazz =  (jclass)env->NewGlobalRef(tmpClass);
    RECTFc.left = env->GetFieldID(RECTFc.clazz, "left", "I");
    if (checkAndClearException(env) || !RECTFc.left) {
        fprintf(stderr, "cacheRECTFields error: JNI exception or left == NULL");
        return;
    }
    RECTFc.top = env->GetFieldID(RECTFc.clazz, "top", "I");
    if (checkAndClearException(env) || !RECTFc.top) {
        fprintf(stderr, "cacheRECTFields error: JNI exception or top == NULL");
        return;
    }
    RECTFc.right = env->GetFieldID(RECTFc.clazz, "right", "I");
    if (checkAndClearException(env) || !RECTFc.right) {
        fprintf(stderr, "cacheRECTFields error: JNI exception or right == NULL");
        return;
    }
    RECTFc.bottom = env->GetFieldID(RECTFc.clazz, "bottom", "I");
    if (checkAndClearException(env) || !RECTFc.bottom) {
        fprintf(stderr, "cacheRECTFields error: JNI exception or bottom == NULL");
        return;
    }
    RECTFc.init = env->GetMethodID(RECTFc.clazz, "<init>", "()V");
    if (checkAndClearException(env) || !RECTFc.init) {
        fprintf(stderr, "cacheRECTFields error: JNI exception or init == NULL");
        return;
    }
    RECTFc.cached = 1;
}

RECT *getRECTFields(JNIEnv *env, jobject lpObject, RECT *lpStruct)
{
    if (!RECTFc.cached) cacheRECTFields(env);
    lpStruct->left = env->GetIntField(lpObject, RECTFc.left);
    lpStruct->top = env->GetIntField(lpObject, RECTFc.top);
    lpStruct->right = env->GetIntField(lpObject, RECTFc.right);
    lpStruct->bottom = env->GetIntField(lpObject, RECTFc.bottom);
    return lpStruct;
}

void setRECTFields(JNIEnv *env, jobject lpObject, RECT *lpStruct)
{
    if (!RECTFc.cached) cacheRECTFields(env);
    env->SetIntField(lpObject, RECTFc.left, (jint)lpStruct->left);
    env->SetIntField(lpObject, RECTFc.top, (jint)lpStruct->top);
    env->SetIntField(lpObject, RECTFc.right, (jint)lpStruct->right);
    env->SetIntField(lpObject, RECTFc.bottom, (jint)lpStruct->bottom);
}

jobject newRECT(JNIEnv *env, RECT *lpStruct)
{
    jobject lpObject = NULL;
    if (!RECTFc.cached) cacheRECTFields(env);
    lpObject = env->NewObject(RECTFc.clazz, RECTFc.init);
    if (lpObject && lpStruct) setRECTFields(env, lpObject, lpStruct);
    return lpObject;
}

typedef struct D2D1_PIXEL_FORMAT_FID_CACHE {
    int cached;
    jclass clazz;
    jfieldID format, alphaMode;
    jmethodID init;
} D2D1_PIXEL_FORMAT_FID_CACHE;

D2D1_PIXEL_FORMAT_FID_CACHE D2D1_PIXEL_FORMATFc;

void cacheD2D1_PIXEL_FORMATFields(JNIEnv *env)
{
    if (D2D1_PIXEL_FORMATFc.cached) return;
    jclass tmpClass = env->FindClass("com/sun/javafx/font/directwrite/D2D1_PIXEL_FORMAT");
    if (checkAndClearException(env) || !tmpClass) {
        fprintf(stderr, "cacheD2D1_PIXEL_FORMATFields error: JNI exception or tmpClass == NULL");
        return;
    }
    D2D1_PIXEL_FORMATFc.clazz =  (jclass)env->NewGlobalRef(tmpClass);
    D2D1_PIXEL_FORMATFc.format = env->GetFieldID(D2D1_PIXEL_FORMATFc.clazz, "format", "I");
    if (checkAndClearException(env) || !D2D1_PIXEL_FORMATFc.format) {
        fprintf(stderr, "cacheD2D1_PIXEL_FORMATFields error: JNI exception or format == NULL");
        return;
    }
    D2D1_PIXEL_FORMATFc.alphaMode = env->GetFieldID(D2D1_PIXEL_FORMATFc.clazz, "alphaMode", "I");
    if (checkAndClearException(env) || !D2D1_PIXEL_FORMATFc.alphaMode) {
        fprintf(stderr, "cacheD2D1_PIXEL_FORMATFields error: JNI exception or alphaMode == NULL");
        return;
    }
    D2D1_PIXEL_FORMATFc.init = env->GetMethodID(D2D1_PIXEL_FORMATFc.clazz, "<init>", "()V");
    if (checkAndClearException(env) || !D2D1_PIXEL_FORMATFc.init) {
        fprintf(stderr, "cacheD2D1_PIXEL_FORMATFields error: JNI exception or init == NULL");
        return;
    }
    D2D1_PIXEL_FORMATFc.cached = 1;
}

D2D1_PIXEL_FORMAT *getD2D1_PIXEL_FORMATFields(JNIEnv *env, jobject lpObject, D2D1_PIXEL_FORMAT *lpStruct)
{
    if (!D2D1_PIXEL_FORMATFc.cached) cacheD2D1_PIXEL_FORMATFields(env);
    lpStruct->format = (DXGI_FORMAT)env->GetIntField(lpObject, D2D1_PIXEL_FORMATFc.format);
    lpStruct->alphaMode = (D2D1_ALPHA_MODE)env->GetIntField(lpObject, D2D1_PIXEL_FORMATFc.alphaMode);
    return lpStruct;
}

void setD2D1_PIXEL_FORMATFields(JNIEnv *env, jobject lpObject, D2D1_PIXEL_FORMAT *lpStruct)
{
    if (!D2D1_PIXEL_FORMATFc.cached) cacheD2D1_PIXEL_FORMATFields(env);
    env->SetIntField(lpObject, D2D1_PIXEL_FORMATFc.format, (jint)lpStruct->format);
    env->SetIntField(lpObject, D2D1_PIXEL_FORMATFc.alphaMode, (jint)lpStruct->alphaMode);
}

jobject newD2D1_PIXEL_FORMAT(JNIEnv *env, D2D1_PIXEL_FORMAT *lpStruct)
{
    jobject lpObject = NULL;
    if (!D2D1_PIXEL_FORMATFc.cached) cacheD2D1_PIXEL_FORMATFields(env);
    lpObject = env->NewObject(D2D1_PIXEL_FORMATFc.clazz, D2D1_PIXEL_FORMATFc.init);
    if (lpObject && lpStruct) setD2D1_PIXEL_FORMATFields(env, lpObject, lpStruct);
    return lpObject;
}

typedef struct D2D1_RENDER_TARGET_PROPERTIES_FID_CACHE {
    int cached;
    jclass clazz;
    jfieldID type, pixelFormat, dpiX, dpiY, usage, minLevel;
    jmethodID init;
} D2D1_RENDER_TARGET_PROPERTIES_FID_CACHE;

D2D1_RENDER_TARGET_PROPERTIES_FID_CACHE D2D1_RENDER_TARGET_PROPERTIESFc;

void cacheD2D1_RENDER_TARGET_PROPERTIESFields(JNIEnv *env)
{
    if (D2D1_RENDER_TARGET_PROPERTIESFc.cached) return;
    jclass tmpClass = env->FindClass("com/sun/javafx/font/directwrite/D2D1_RENDER_TARGET_PROPERTIES");
    if (checkAndClearException(env) || !tmpClass) {
        fprintf(stderr, "cacheD2D1_RENDER_TARGET_PROPERTIESFields error: JNI exception or tmpClass == NULL");
        return;
    }
    D2D1_RENDER_TARGET_PROPERTIESFc.clazz =  (jclass)env->NewGlobalRef(tmpClass);
    D2D1_RENDER_TARGET_PROPERTIESFc.type = env->GetFieldID(D2D1_RENDER_TARGET_PROPERTIESFc.clazz, "type", "I");
    if (checkAndClearException(env) || !D2D1_RENDER_TARGET_PROPERTIESFc.type) {
        fprintf(stderr, "cacheD2D1_RENDER_TARGET_PROPERTIESFields error: JNI exception or type == NULL");
        return;
    }
    D2D1_RENDER_TARGET_PROPERTIESFc.pixelFormat = env->GetFieldID(D2D1_RENDER_TARGET_PROPERTIESFc.clazz, "pixelFormat", "Lcom/sun/javafx/font/directwrite/D2D1_PIXEL_FORMAT;");
    if (checkAndClearException(env) || !D2D1_RENDER_TARGET_PROPERTIESFc.pixelFormat) {
        fprintf(stderr, "cacheD2D1_RENDER_TARGET_PROPERTIESFields error: JNI exception or pixelFormat == NULL");
        return;
    }
    D2D1_RENDER_TARGET_PROPERTIESFc.dpiX = env->GetFieldID(D2D1_RENDER_TARGET_PROPERTIESFc.clazz, "dpiX", "F");
    if (checkAndClearException(env) || !D2D1_RENDER_TARGET_PROPERTIESFc.dpiX) {
        fprintf(stderr, "cacheD2D1_RENDER_TARGET_PROPERTIESFields error: JNI exception or dpiX == NULL");
        return;
    }
    D2D1_RENDER_TARGET_PROPERTIESFc.dpiY = env->GetFieldID(D2D1_RENDER_TARGET_PROPERTIESFc.clazz, "dpiY", "F");
    if (checkAndClearException(env) || !D2D1_RENDER_TARGET_PROPERTIESFc.dpiY) {
        fprintf(stderr, "cacheD2D1_RENDER_TARGET_PROPERTIESFields error: JNI exception or dpiY == NULL");
        return;
    }
    D2D1_RENDER_TARGET_PROPERTIESFc.usage = env->GetFieldID(D2D1_RENDER_TARGET_PROPERTIESFc.clazz, "usage", "I");
    if (checkAndClearException(env) || !D2D1_RENDER_TARGET_PROPERTIESFc.usage) {
        fprintf(stderr, "cacheD2D1_RENDER_TARGET_PROPERTIESFields error: JNI exception or usage == NULL");
        return;
    }
    D2D1_RENDER_TARGET_PROPERTIESFc.minLevel = env->GetFieldID(D2D1_RENDER_TARGET_PROPERTIESFc.clazz, "minLevel", "I");
    if (checkAndClearException(env) || !D2D1_RENDER_TARGET_PROPERTIESFc.minLevel) {
        fprintf(stderr, "cacheD2D1_RENDER_TARGET_PROPERTIESFields error: JNI exception or minLevel == NULL");
        return;
    }
    D2D1_RENDER_TARGET_PROPERTIESFc.init = env->GetMethodID(D2D1_RENDER_TARGET_PROPERTIESFc.clazz, "<init>", "()V");
    if (checkAndClearException(env) || !D2D1_RENDER_TARGET_PROPERTIESFc.init) {
        fprintf(stderr, "cacheD2D1_RENDER_TARGET_PROPERTIESFields error: JNI exception or init == NULL");
        return;
    }
    D2D1_RENDER_TARGET_PROPERTIESFc.cached = 1;
}

D2D1_RENDER_TARGET_PROPERTIES *getD2D1_RENDER_TARGET_PROPERTIESFields(JNIEnv *env, jobject lpObject, D2D1_RENDER_TARGET_PROPERTIES *lpStruct)
{
    if (!D2D1_RENDER_TARGET_PROPERTIESFc.cached) cacheD2D1_RENDER_TARGET_PROPERTIESFields(env);
    lpStruct->type = (D2D1_RENDER_TARGET_TYPE)env->GetIntField(lpObject, D2D1_RENDER_TARGET_PROPERTIESFc.type);
    {
    jobject lpObject1 = env->GetObjectField(lpObject, D2D1_RENDER_TARGET_PROPERTIESFc.pixelFormat);
    if (lpObject1 != NULL) getD2D1_PIXEL_FORMATFields(env, lpObject1, &lpStruct->pixelFormat);
    }
    lpStruct->dpiX = env->GetFloatField(lpObject, D2D1_RENDER_TARGET_PROPERTIESFc.dpiX);
    lpStruct->dpiY = env->GetFloatField(lpObject, D2D1_RENDER_TARGET_PROPERTIESFc.dpiY);
    lpStruct->usage = (D2D1_RENDER_TARGET_USAGE)env->GetIntField(lpObject, D2D1_RENDER_TARGET_PROPERTIESFc.usage);
    lpStruct->minLevel = (D2D1_FEATURE_LEVEL)env->GetIntField(lpObject, D2D1_RENDER_TARGET_PROPERTIESFc.minLevel);
    return lpStruct;
}

void setD2D1_RENDER_TARGET_PROPERTIESFields(JNIEnv *env, jobject lpObject, D2D1_RENDER_TARGET_PROPERTIES *lpStruct)
{
    if (!D2D1_RENDER_TARGET_PROPERTIESFc.cached) cacheD2D1_RENDER_TARGET_PROPERTIESFields(env);
    env->SetIntField(lpObject, D2D1_RENDER_TARGET_PROPERTIESFc.type, (jint)lpStruct->type);
    {
    jobject lpObject1 = env->GetObjectField(lpObject, D2D1_RENDER_TARGET_PROPERTIESFc.pixelFormat);
    if (lpObject1 != NULL) setD2D1_PIXEL_FORMATFields(env, lpObject1, &lpStruct->pixelFormat);
    }
    env->SetFloatField(lpObject, D2D1_RENDER_TARGET_PROPERTIESFc.dpiX, (jfloat)lpStruct->dpiX);
    env->SetFloatField(lpObject, D2D1_RENDER_TARGET_PROPERTIESFc.dpiY, (jfloat)lpStruct->dpiY);
    env->SetIntField(lpObject, D2D1_RENDER_TARGET_PROPERTIESFc.usage, (jint)lpStruct->usage);
    env->SetIntField(lpObject, D2D1_RENDER_TARGET_PROPERTIESFc.minLevel, (jint)lpStruct->minLevel);
}

jobject newD2D1_RENDER_TARGET_PROPERTIES(JNIEnv *env, D2D1_RENDER_TARGET_PROPERTIES *lpStruct)
{
    jobject lpObject = NULL;
    if (!D2D1_RENDER_TARGET_PROPERTIESFc.cached) cacheD2D1_RENDER_TARGET_PROPERTIESFields(env);
    lpObject = env->NewObject(D2D1_RENDER_TARGET_PROPERTIESFc.clazz, D2D1_RENDER_TARGET_PROPERTIESFc.init);
    if (lpObject && lpStruct) setD2D1_RENDER_TARGET_PROPERTIESFields(env, lpObject, lpStruct);
    return lpObject;
}

typedef struct D2D1_COLOR_F_FID_CACHE {
    int cached;
    jclass clazz;
    jfieldID r, g, b, a;
    jmethodID init;
} D2D1_COLOR_F_FID_CACHE;

D2D1_COLOR_F_FID_CACHE D2D1_COLOR_FFc;

void cacheD2D1_COLOR_FFields(JNIEnv *env)
{
    if (D2D1_COLOR_FFc.cached) return;
    jclass tmpClass = env->FindClass("com/sun/javafx/font/directwrite/D2D1_COLOR_F");
    if (checkAndClearException(env) || !tmpClass) {
        fprintf(stderr, "cacheD2D1_COLOR_FFields error: JNI exception or tmpClass == NULL");
        return;
    }
    D2D1_COLOR_FFc.clazz =  (jclass)env->NewGlobalRef(tmpClass);
    D2D1_COLOR_FFc.r = env->GetFieldID(D2D1_COLOR_FFc.clazz, "r", "F");
    if (checkAndClearException(env) || !D2D1_COLOR_FFc.r) {
        fprintf(stderr, "cacheD2D1_COLOR_FFields error: JNI exception or r == NULL");
        return;
    }
    D2D1_COLOR_FFc.g = env->GetFieldID(D2D1_COLOR_FFc.clazz, "g", "F");
    if (checkAndClearException(env) || !D2D1_COLOR_FFc.g) {
        fprintf(stderr, "cacheD2D1_COLOR_FFields error: JNI exception or g == NULL");
        return;
    }
    D2D1_COLOR_FFc.b = env->GetFieldID(D2D1_COLOR_FFc.clazz, "b", "F");
    if (checkAndClearException(env) || !D2D1_COLOR_FFc.b) {
        fprintf(stderr, "cacheD2D1_COLOR_FFields error: JNI exception or b == NULL");
        return;
    }
    D2D1_COLOR_FFc.a = env->GetFieldID(D2D1_COLOR_FFc.clazz, "a", "F");
    if (checkAndClearException(env) || !D2D1_COLOR_FFc.a) {
        fprintf(stderr, "cacheD2D1_COLOR_FFields error: JNI exception or a == NULL");
        return;
    }
    D2D1_COLOR_FFc.init = env->GetMethodID(D2D1_COLOR_FFc.clazz, "<init>", "()V");
    if (checkAndClearException(env) || !D2D1_COLOR_FFc.init) {
        fprintf(stderr, "cacheD2D1_COLOR_FFields error: JNI exception or init == NULL");
        return;
    }
    D2D1_COLOR_FFc.cached = 1;
}

D2D1_COLOR_F *getD2D1_COLOR_FFields(JNIEnv *env, jobject lpObject, D2D1_COLOR_F *lpStruct)
{
    if (!D2D1_COLOR_FFc.cached) cacheD2D1_COLOR_FFields(env);
    lpStruct->r = env->GetFloatField(lpObject, D2D1_COLOR_FFc.r);
    lpStruct->g = env->GetFloatField(lpObject, D2D1_COLOR_FFc.g);
    lpStruct->b = env->GetFloatField(lpObject, D2D1_COLOR_FFc.b);
    lpStruct->a = env->GetFloatField(lpObject, D2D1_COLOR_FFc.a);
    return lpStruct;
}

void setD2D1_COLOR_FFields(JNIEnv *env, jobject lpObject, D2D1_COLOR_F *lpStruct)
{
    if (!D2D1_COLOR_FFc.cached) cacheD2D1_COLOR_FFields(env);
    env->SetFloatField(lpObject, D2D1_COLOR_FFc.r, (jfloat)lpStruct->r);
    env->SetFloatField(lpObject, D2D1_COLOR_FFc.g, (jfloat)lpStruct->g);
    env->SetFloatField(lpObject, D2D1_COLOR_FFc.b, (jfloat)lpStruct->b);
    env->SetFloatField(lpObject, D2D1_COLOR_FFc.a, (jfloat)lpStruct->a);
}

jobject newD2D1_COLOR_F(JNIEnv *env, D2D1_COLOR_F *lpStruct)
{
    jobject lpObject = NULL;
    if (!D2D1_COLOR_FFc.cached) cacheD2D1_COLOR_FFields(env);
    lpObject = env->NewObject(D2D1_COLOR_FFc.clazz, D2D1_COLOR_FFc.init);
    if (lpObject && lpStruct) setD2D1_COLOR_FFields(env, lpObject, lpStruct);
    return lpObject;
}

typedef struct D2D1_POINT_2F_FID_CACHE {
    int cached;
    jclass clazz;
    jfieldID x, y;
    jmethodID init;
} D2D1_POINT_2F_FID_CACHE;

D2D1_POINT_2F_FID_CACHE D2D1_POINT_2FFc;

void cacheD2D1_POINT_2FFields(JNIEnv *env)
{
    if (D2D1_POINT_2FFc.cached) return;
    jclass tmpClass = env->FindClass("com/sun/javafx/font/directwrite/D2D1_POINT_2F");
    if (checkAndClearException(env) || !tmpClass) {
        fprintf(stderr, "cacheD2D1_POINT_2FFields error: JNI exception or tmpClass == NULL");
        return;
    }
    D2D1_POINT_2FFc.clazz =  (jclass)env->NewGlobalRef(tmpClass);
    D2D1_POINT_2FFc.x = env->GetFieldID(D2D1_POINT_2FFc.clazz, "x", "F");
    if (checkAndClearException(env) || !D2D1_POINT_2FFc.x) {
        fprintf(stderr, "cacheD2D1_POINT_2FFields error: JNI exception or x == NULL");
        return;
    }
    D2D1_POINT_2FFc.y = env->GetFieldID(D2D1_POINT_2FFc.clazz, "y", "F");
    if (checkAndClearException(env) || !D2D1_POINT_2FFc.y) {
        fprintf(stderr, "cacheD2D1_POINT_2FFields error: JNI exception or y == NULL");
        return;
    }
    D2D1_POINT_2FFc.init = env->GetMethodID(D2D1_POINT_2FFc.clazz, "<init>", "()V");
    if (checkAndClearException(env) || !D2D1_POINT_2FFc.init) {
        fprintf(stderr, "cacheD2D1_POINT_2FFields error: JNI exception or init == NULL");
        return;
    }
    D2D1_POINT_2FFc.cached = 1;
}

D2D1_POINT_2F *getD2D1_POINT_2FFields(JNIEnv *env, jobject lpObject, D2D1_POINT_2F *lpStruct)
{
    if (!D2D1_POINT_2FFc.cached) cacheD2D1_POINT_2FFields(env);
    lpStruct->x = env->GetFloatField(lpObject, D2D1_POINT_2FFc.x);
    lpStruct->y = env->GetFloatField(lpObject, D2D1_POINT_2FFc.y);
    return lpStruct;
}

void setD2D1_POINT_2FFields(JNIEnv *env, jobject lpObject, D2D1_POINT_2F *lpStruct)
{
    if (!D2D1_POINT_2FFc.cached) cacheD2D1_POINT_2FFields(env);
    env->SetFloatField(lpObject, D2D1_POINT_2FFc.x, (jfloat)lpStruct->x);
    env->SetFloatField(lpObject, D2D1_POINT_2FFc.y, (jfloat)lpStruct->y);
}

jobject newD2D1_POINT_2F(JNIEnv *env, D2D1_POINT_2F *lpStruct)
{
    jobject lpObject = NULL;
    if (!D2D1_POINT_2FFc.cached) cacheD2D1_POINT_2FFields(env);
    lpObject = env->NewObject(D2D1_POINT_2FFc.clazz, D2D1_POINT_2FFc.init);
    if (lpObject && lpStruct) setD2D1_POINT_2FFields(env, lpObject, lpStruct);
    return lpObject;
}

typedef struct D2D1_MATRIX_3X2_F_FID_CACHE {
    int cached;
    jclass clazz;
    jfieldID _11, _12, _21, _22, _31, _32;
    jmethodID init;
} D2D1_MATRIX_3X2_F_FID_CACHE;

D2D1_MATRIX_3X2_F_FID_CACHE D2D1_MATRIX_3X2_FFc;

void cacheD2D1_MATRIX_3X2_FFields(JNIEnv *env)
{
    if (D2D1_MATRIX_3X2_FFc.cached) return;
    jclass tmpClass = env->FindClass("com/sun/javafx/font/directwrite/D2D1_MATRIX_3X2_F");
    if (checkAndClearException(env) || !tmpClass) {
        fprintf(stderr, "cacheD2D1_MATRIX_3X2_FFields error: JNI exception or tmpClass == NULL");
        return;
    }
    D2D1_MATRIX_3X2_FFc.clazz =  (jclass)env->NewGlobalRef(tmpClass);
    D2D1_MATRIX_3X2_FFc._11 = env->GetFieldID(D2D1_MATRIX_3X2_FFc.clazz, "_11", "F");
    if (checkAndClearException(env) || !D2D1_MATRIX_3X2_FFc._11) {
        fprintf(stderr, "cacheD2D1_MATRIX_3X2_FFields error: JNI exception or _11 == NULL");
        return;
    }
    D2D1_MATRIX_3X2_FFc._12 = env->GetFieldID(D2D1_MATRIX_3X2_FFc.clazz, "_12", "F");
    if (checkAndClearException(env) || !D2D1_MATRIX_3X2_FFc._12) {
        fprintf(stderr, "cacheD2D1_MATRIX_3X2_FFields error: JNI exception or _12 == NULL");
        return;
    }
    D2D1_MATRIX_3X2_FFc._21 = env->GetFieldID(D2D1_MATRIX_3X2_FFc.clazz, "_21", "F");
    if (checkAndClearException(env) || !D2D1_MATRIX_3X2_FFc._21) {
        fprintf(stderr, "cacheD2D1_MATRIX_3X2_FFields error: JNI exception or _21 == NULL");
        return;
    }
    D2D1_MATRIX_3X2_FFc._22 = env->GetFieldID(D2D1_MATRIX_3X2_FFc.clazz, "_22", "F");
    if (checkAndClearException(env) || !D2D1_MATRIX_3X2_FFc._22) {
        fprintf(stderr, "cacheD2D1_MATRIX_3X2_FFields error: JNI exception or _22 == NULL");
        return;
    }
    D2D1_MATRIX_3X2_FFc._31 = env->GetFieldID(D2D1_MATRIX_3X2_FFc.clazz, "_31", "F");
    if (checkAndClearException(env) || !D2D1_MATRIX_3X2_FFc._31) {
        fprintf(stderr, "cacheD2D1_MATRIX_3X2_FFields error: JNI exception or _31 == NULL");
        return;
    }
    D2D1_MATRIX_3X2_FFc._32 = env->GetFieldID(D2D1_MATRIX_3X2_FFc.clazz, "_32", "F");
    if (checkAndClearException(env) || !D2D1_MATRIX_3X2_FFc._32) {
        fprintf(stderr, "cacheD2D1_MATRIX_3X2_FFields error: JNI exception or _32 == NULL");
        return;
    }
    D2D1_MATRIX_3X2_FFc.init = env->GetMethodID(D2D1_MATRIX_3X2_FFc.clazz, "<init>", "()V");
    if (checkAndClearException(env) || !D2D1_MATRIX_3X2_FFc.init) {
        fprintf(stderr, "cacheD2D1_MATRIX_3X2_FFields error: JNI exception or init == NULL");
        return;
    }
    D2D1_MATRIX_3X2_FFc.cached = 1;
}

D2D1_MATRIX_3X2_F *getD2D1_MATRIX_3X2_FFields(JNIEnv *env, jobject lpObject, D2D1_MATRIX_3X2_F *lpStruct)
{
    if (!D2D1_MATRIX_3X2_FFc.cached) cacheD2D1_MATRIX_3X2_FFields(env);
    lpStruct->_11 = env->GetFloatField(lpObject, D2D1_MATRIX_3X2_FFc._11);
    lpStruct->_12 = env->GetFloatField(lpObject, D2D1_MATRIX_3X2_FFc._12);
    lpStruct->_21 = env->GetFloatField(lpObject, D2D1_MATRIX_3X2_FFc._21);
    lpStruct->_22 = env->GetFloatField(lpObject, D2D1_MATRIX_3X2_FFc._22);
    lpStruct->_31 = env->GetFloatField(lpObject, D2D1_MATRIX_3X2_FFc._31);
    lpStruct->_32 = env->GetFloatField(lpObject, D2D1_MATRIX_3X2_FFc._32);
    return lpStruct;
}

void setD2D1_MATRIX_3X2_FFields(JNIEnv *env, jobject lpObject, D2D1_MATRIX_3X2_F *lpStruct)
{
    if (!D2D1_MATRIX_3X2_FFc.cached) cacheD2D1_MATRIX_3X2_FFields(env);
    env->SetFloatField(lpObject, D2D1_MATRIX_3X2_FFc._11, (jfloat)lpStruct->_11);
    env->SetFloatField(lpObject, D2D1_MATRIX_3X2_FFc._12, (jfloat)lpStruct->_12);
    env->SetFloatField(lpObject, D2D1_MATRIX_3X2_FFc._21, (jfloat)lpStruct->_21);
    env->SetFloatField(lpObject, D2D1_MATRIX_3X2_FFc._22, (jfloat)lpStruct->_22);
    env->SetFloatField(lpObject, D2D1_MATRIX_3X2_FFc._31, (jfloat)lpStruct->_31);
    env->SetFloatField(lpObject, D2D1_MATRIX_3X2_FFc._32, (jfloat)lpStruct->_32);
}

jobject newD2D1_MATRIX_3X2_F(JNIEnv *env, D2D1_MATRIX_3X2_F *lpStruct)
{
    jobject lpObject = NULL;
    if (!D2D1_MATRIX_3X2_FFc.cached) cacheD2D1_MATRIX_3X2_FFields(env);
    lpObject = env->NewObject(D2D1_MATRIX_3X2_FFc.clazz, D2D1_MATRIX_3X2_FFc.init);
    if (lpObject && lpStruct) setD2D1_MATRIX_3X2_FFields(env, lpObject, lpStruct);
    return lpObject;
}
/**************************************************************************/
/*                                                                        */
/*                            Functions                                   */
/*                                                                        */
/**************************************************************************/

JNIEXPORT jboolean JNICALL OS_NATIVE(CoInitializeEx)
    (JNIEnv *env, jclass that, jint arg0)
{
    HRESULT hr = CoInitializeEx(NULL, (DWORD)arg0);

    /* This means COM has been initialized with a different concurrency model.
     * This should never happen. */
    if (hr == RPC_E_CHANGED_MODE) return JNI_FALSE;

    return JNI_TRUE;
}

JNIEXPORT void JNICALL OS_NATIVE(CoUninitialize)
    (JNIEnv *env, jclass that)
{
    CoUninitialize();
}

JNIEXPORT jlong JNICALL OS_NATIVE(_1WICCreateImagingFactory)
    (JNIEnv *env, jclass that)
{
    IWICImagingFactory* result = NULL;
    HRESULT hr = CoCreateInstance(
            CLSID_WICImagingFactory,
            NULL,
            CLSCTX_INPROC_SERVER,
            IID_PPV_ARGS(&result)
            );

    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

JNIEXPORT jlong JNICALL OS_NATIVE(_1D2D1CreateFactory)
    (JNIEnv *env, jclass that, jint arg0)
{
    HRESULT hr = E_FAIL;
    ID2D1Factory* result = NULL;
    HMODULE module = LoadLibrary(TEXT("d2d1.dll"));
    D2D1CreateFactoryProc createProc = NULL;
    if (module) {
        createProc = (D2D1CreateFactoryProc)GetProcAddress(module, "D2D1CreateFactory");
    }
    if (createProc) {
        D2D1_FACTORY_OPTIONS options;
        options.debugLevel = D2D1_DEBUG_LEVEL_NONE;
        hr = createProc((D2D1_FACTORY_TYPE)arg0,
                        __uuidof(ID2D1Factory),
                        &options,
                        reinterpret_cast<void**>(&result));
    }
    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

JNIEXPORT jlong JNICALL OS_NATIVE(_1DWriteCreateFactory)
    (JNIEnv *env, jclass that, jint arg0)
{
    HRESULT hr = E_FAIL;
    IDWriteFactory* result = NULL;
    HMODULE module = LoadLibrary(TEXT("dwrite.dll"));
    DWriteCreateFactoryProc createProc = NULL;
    if (module) {
        createProc = (DWriteCreateFactoryProc)GetProcAddress(module, "DWriteCreateFactory");
    }
    if (createProc) {
        hr = createProc((DWRITE_FACTORY_TYPE)arg0,
                        __uuidof(IDWriteFactory),
                        reinterpret_cast<IUnknown**>(&result));
    }
    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

/* IUnknown */
JNIEXPORT jint JNICALL OS_NATIVE(AddRef)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return ((IUnknown *)arg0)->AddRef();
}

JNIEXPORT jint JNICALL OS_NATIVE(Release)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return ((IUnknown *)arg0)->Release();
}

/***********************************************/
/*         Text Source and Sink                */
/***********************************************/

class JFXTextAnalysisSink : public IDWriteTextAnalysisSink, public IDWriteTextAnalysisSource {
public:
    JFXTextAnalysisSink(
        JNIEnv *env,
        jcharArray text,
        jint start,
        jint length,
        jcharArray locale,
        jint direction,
        jlong numberSubstitution
    );
    ~JFXTextAnalysisSink();

/* IDWriteTextAnalysisSink */
public:
    IFACEMETHOD (SetScriptAnalysis) (
        UINT32 textPosition,
        UINT32 textLength,
        DWRITE_SCRIPT_ANALYSIS const* scriptAnalysis);

    IFACEMETHOD (SetLineBreakpoints) (
        UINT32 textPosition,
        UINT32 textLength,
        const DWRITE_LINE_BREAKPOINT* lineBreakpoints);

    IFACEMETHOD (SetBidiLevel) (
        UINT32 textPosition,
        UINT32 textLength,
        UINT8 explicitLevel,
        UINT8 resolvedLevel);

    IFACEMETHOD (SetNumberSubstitution) (
        UINT32 textPosition,
        UINT32 textLength,
        IDWriteNumberSubstitution* numberSubstitution);

/* IDWriteTextAnalysisSource */
    IFACEMETHOD (GetTextAtPosition) (
        UINT32 textPosition,
        OUT WCHAR const** textString,
        OUT UINT32* textLength);

    IFACEMETHOD (GetTextBeforePosition) (
        UINT32 textPosition,
        OUT WCHAR const** textString,
        OUT UINT32* textLength);

    IFACEMETHOD_(DWRITE_READING_DIRECTION, GetParagraphReadingDirection) ();

    IFACEMETHOD (GetLocaleName) (
        UINT32 textPosition,
        OUT UINT32* textLength,
        OUT WCHAR const** localeName);

    IFACEMETHOD (GetNumberSubstitution) (
        UINT32 textPosition,
        OUT UINT32* textLength,
        OUT IDWriteNumberSubstitution** numberSubstitution);

/* IUnknown */
public:
    IFACEMETHOD_(ULONG, AddRef) ();
    IFACEMETHOD_(ULONG,  Release) ();
    IFACEMETHOD(QueryInterface) (
                IID const& riid,
                void** ppvObject);

public:
    BOOL Next();
    UINT32 GetStart();
    UINT32 GetLength();
    DWRITE_SCRIPT_ANALYSIS* GetAnalysis();

private:
    struct Run {
        UINT32 start;
        UINT32 length;
        DWRITE_SCRIPT_ANALYSIS analysis;
    };
    ULONG cRefCount_;
    UINT32 textLength_;
    const WCHAR* text_;
    const WCHAR* locale_;
    IDWriteNumberSubstitution* numberSubstitution_;
    DWRITE_READING_DIRECTION readingDirection_;
    std::vector<Run> runs_;
    INT32 position_;
};

JFXTextAnalysisSink::JFXTextAnalysisSink(
        JNIEnv *env,
        jcharArray text,
        jint start,
        jint length,
        jcharArray locale,
        jint direction,
        jlong numberSubstitution
    )
:   cRefCount_(0),
    position_(-1),
    textLength_(length),
    numberSubstitution_((IDWriteNumberSubstitution*)numberSubstitution),
    readingDirection_((DWRITE_READING_DIRECTION)direction)
{
    text_ = new (std::nothrow) WCHAR [textLength_];
    env->GetCharArrayRegion(text, start, textLength_, (jchar*)text_);
    UINT32 localeLength = env->GetArrayLength(locale);
    locale_ = new (std::nothrow) WCHAR [localeLength];
    env->GetCharArrayRegion(locale, 0, localeLength, (jchar*)locale_);
    if (numberSubstitution_) numberSubstitution_->AddRef();
}

JFXTextAnalysisSink::~JFXTextAnalysisSink() {
    delete [] text_;
    delete [] locale_;
    if (numberSubstitution_) {
        numberSubstitution_->Release();
    }
    text_ = NULL;
    locale_ = NULL;
    numberSubstitution_ = NULL;
}

/* IDWriteTextAnalysisSink */
IFACEMETHODIMP JFXTextAnalysisSink::SetScriptAnalysis(
    UINT32 textPosition,
    UINT32 textLength,
    DWRITE_SCRIPT_ANALYSIS const* scriptAnalysis) {
    runs_.resize(runs_.size() + 1);
    Run& run  = runs_.back();
    run.start = textPosition;
    run.length = textLength;
    run.analysis = *scriptAnalysis;
    return S_OK;
}

IFACEMETHODIMP JFXTextAnalysisSink::SetLineBreakpoints(
    UINT32 textPosition,
    UINT32 textLength,
    const DWRITE_LINE_BREAKPOINT* lineBreakpoints) {
    return S_OK;
}

IFACEMETHODIMP JFXTextAnalysisSink::SetBidiLevel(
    UINT32 textPosition,
    UINT32 textLength,
    UINT8 explicitLevel,
    UINT8 resolvedLevel) {
    return S_OK;
}

IFACEMETHODIMP JFXTextAnalysisSink::SetNumberSubstitution(
    UINT32 textPosition,
    UINT32 textLength,
    IDWriteNumberSubstitution* numberSubstitution) {
    return S_OK;
}

/* IDWriteTextAnalysisSource */
IFACEMETHODIMP JFXTextAnalysisSink::GetTextAtPosition(
    UINT32 textPosition,
    WCHAR const** textString,
    UINT32* textLength) {
    if (textPosition < textLength_) {
        *textString = &text_[textPosition];
        *textLength = textLength_ - textPosition;
    } else {
        *textString = NULL;
        *textLength = 0;
    }
    return S_OK;
}

IFACEMETHODIMP JFXTextAnalysisSink::GetTextBeforePosition(
    UINT32 textPosition,
    OUT WCHAR const** textString,
    OUT UINT32* textLength) {
    if (textPosition == 0 || textPosition > textLength_) {
        *textString = NULL;
        *textLength = 0;
    } else {
        *textString = text_;
        *textLength = textPosition;
    }
    return S_OK;
}

IFACEMETHODIMP_(DWRITE_READING_DIRECTION) JFXTextAnalysisSink::GetParagraphReadingDirection() {
    return readingDirection_;
}

IFACEMETHODIMP JFXTextAnalysisSink::GetLocaleName(
    UINT32 textPosition,
    OUT UINT32* textLength,
    OUT WCHAR const** localeName) {
    fflush(stdout);
    *localeName = locale_;
    *textLength = textLength_ - textPosition;
    return S_OK;
}

IFACEMETHODIMP JFXTextAnalysisSink::GetNumberSubstitution(
    UINT32 textPosition,
    OUT UINT32* textLength,
    OUT IDWriteNumberSubstitution** numberSubstitution) {

    if (numberSubstitution_ != NULL)
        numberSubstitution_->AddRef();

    *numberSubstitution = numberSubstitution_;
    *textLength = textLength_ - textPosition;

    return S_OK;
}

BOOL JFXTextAnalysisSink::Next() {
    position_++;
    return ((UINT32)position_) < runs_.size();
}

UINT32 JFXTextAnalysisSink::GetStart() {
    if (((UINT32)position_) >= runs_.size()) return 0;
    return runs_[position_].start;
}

UINT32 JFXTextAnalysisSink::GetLength() {
    if (((UINT32)position_) >= runs_.size()) return 0;
    return runs_[position_].length;
}

DWRITE_SCRIPT_ANALYSIS* JFXTextAnalysisSink::GetAnalysis() {
    if (((UINT32)position_) >= runs_.size()) return NULL;
    return &runs_[position_].analysis;
}

/* IUnknown */
IFACEMETHODIMP_(ULONG) JFXTextAnalysisSink::AddRef() {
    return InterlockedIncrement(&cRefCount_);
}

IFACEMETHODIMP_(ULONG) JFXTextAnalysisSink::Release() {
    ULONG newCount = InterlockedDecrement(&cRefCount_);
    if (newCount == 0) {
        delete this;
        return 0;
    }
    return newCount;
}

IFACEMETHODIMP JFXTextAnalysisSink::QueryInterface(IID const& riid, void** ppvObject) {
    if (__uuidof(IDWriteTextAnalysisSink) == riid) {
        *ppvObject = this;
    } else if (__uuidof(IDWriteTextAnalysisSource) == riid) {
        *ppvObject = this;
    } else if (__uuidof(IUnknown) == riid) {
        *ppvObject = this;
    } else {
        *ppvObject = NULL;
        return E_FAIL;
    }
    this->AddRef();
    return S_OK;
}

JNIEXPORT jlong JNICALL OS_NATIVE(_1NewJFXTextAnalysisSink)
(JNIEnv *env, jclass that, jcharArray arg0, jint arg1, jint arg2, jcharArray arg3, jint arg4, jlong arg5)
{
    if (arg0 == NULL || arg3 == NULL) return 0L;
    return (jlong) new (std::nothrow) JFXTextAnalysisSink(env, arg0, arg1, arg2, arg3, arg4, arg5);
}

JNIEXPORT jboolean JNICALL OS_NATIVE(Next)
(JNIEnv *env, jclass that, jlong arg0) {
    return ((JFXTextAnalysisSink*)arg0)->Next();
}

JNIEXPORT jint JNICALL OS_NATIVE(GetStart)
(JNIEnv *env, jclass that, jlong arg0) {
    return ((JFXTextAnalysisSink*)arg0)->GetStart();
}

JNIEXPORT jint JNICALL OS_NATIVE(GetLength)
(JNIEnv *env, jclass that, jlong arg0) {
    return ((JFXTextAnalysisSink*)arg0)->GetLength();
}

JNIEXPORT jobject JNICALL OS_NATIVE(GetAnalysis)
(JNIEnv *env, jclass that, jlong arg0) {
    return newDWRITE_SCRIPT_ANALYSIS(env, ((JFXTextAnalysisSink*)arg0)->GetAnalysis());
}

/***********************************************/
/*                Text Renderer                */
/***********************************************/

class JFXTextRenderer : public IDWriteTextRenderer
{
public:
    JFXTextRenderer();
    ~JFXTextRenderer();

/* IDWriteTextRenderer */
public:
    IFACEMETHOD(DrawGlyphRun)(
        void* clientDrawingContext,
        FLOAT baselineOriginX,
        FLOAT baselineOriginY,
        DWRITE_MEASURING_MODE measuringMode,
        DWRITE_GLYPH_RUN const* glyphRun,
        DWRITE_GLYPH_RUN_DESCRIPTION const* glyphRunDescription,
        IUnknown* clientDrawingEffect);

    IFACEMETHOD(DrawUnderline)(
        void* clientDrawingContext,
        FLOAT baselineOriginX,
        FLOAT baselineOriginY,
        DWRITE_UNDERLINE const* underline,
        IUnknown* clientDrawingEffect);

    IFACEMETHOD(DrawStrikethrough)(
        void* clientDrawingContext,
        FLOAT baselineOriginX,
        FLOAT baselineOriginY,
        DWRITE_STRIKETHROUGH const* strikethrough,
        IUnknown* clientDrawingEffect);

    IFACEMETHOD(DrawInlineObject)(
        void* clientDrawingContext,
        FLOAT originX,
        FLOAT originY,
        IDWriteInlineObject* inlineObject,
        BOOL isSideways,
        BOOL isRightToLeft,
        IUnknown* clientDrawingEffect);

/* IDWritePixelSnapping */
public:
    IFACEMETHOD(IsPixelSnappingDisabled)(
        void* clientDrawingContext,
        BOOL* isDisabled);

    IFACEMETHOD(GetCurrentTransform)(
        void* clientDrawingContext,
        DWRITE_MATRIX* transform);

    IFACEMETHOD(GetPixelsPerDip)(
        void* clientDrawingContext,
        FLOAT* pixelsPerDip);

/* IUnknown */
public:
    IFACEMETHOD_(ULONG, AddRef) ();
    IFACEMETHOD_(ULONG,  Release) ();
    IFACEMETHOD(QueryInterface) (
                IID const& riid,
                void** ppvObject);

public:
    BOOL Next();
    UINT32 GetStart();
    UINT32 GetLength();
    UINT32 GetGlyphCount();
    UINT32 GetTotalGlyphCount();
    IDWriteFontFace* GetFontFace();
    const UINT16* GetClusterMap();
    const UINT16* GetGlyphIndices();
    const FLOAT*  GetGlyphAdvances();
    const DWRITE_GLYPH_OFFSET* GetGlyphOffsets();

private:
    ULONG cRefCount_;
    struct Run {
        DWRITE_GLYPH_RUN glyphRun;
        DWRITE_GLYPH_RUN_DESCRIPTION glyphRunDescription;
    };
    std::vector<Run> runs_;
    INT32 position_;
    INT32 totalGlyphCount_;
};

JFXTextRenderer::JFXTextRenderer()
: cRefCount_(0),
  position_(-1),
  totalGlyphCount_(0) {
}

JFXTextRenderer::~JFXTextRenderer() {
}

/* IDWriteTextRenderer */
IFACEMETHODIMP JFXTextRenderer::DrawGlyphRun (
        void* clientDrawingContext,
        FLOAT baselineOriginX,
        FLOAT baselineOriginY,
        DWRITE_MEASURING_MODE measuringMode,
        DWRITE_GLYPH_RUN const* glyphRun,
        DWRITE_GLYPH_RUN_DESCRIPTION const* glyphRunDescription,
        IUnknown* clientDrawingEffect)
{
    runs_.resize(runs_.size() + 1);
    Run& run  = runs_.back();
    run.glyphRun = *glyphRun;
    run.glyphRunDescription = *glyphRunDescription;
    totalGlyphCount_ += glyphRun->glyphCount;
    return S_OK;
}

IFACEMETHODIMP JFXTextRenderer::DrawUnderline (
        void* clientDrawingContext,
        FLOAT baselineOriginX,
        FLOAT baselineOriginY,
        DWRITE_UNDERLINE const* underline,
        IUnknown* clientDrawingEffect)
{
    return S_OK;
}

IFACEMETHODIMP JFXTextRenderer::DrawStrikethrough (
        void* clientDrawingContext,
        FLOAT baselineOriginX,
        FLOAT baselineOriginY,
        DWRITE_STRIKETHROUGH const* strikethrough,
        IUnknown* clientDrawingEffect)
{
    return S_OK;
}

IFACEMETHODIMP JFXTextRenderer::DrawInlineObject (
        void* clientDrawingContext,
        FLOAT originX,
        FLOAT originY,
        IDWriteInlineObject* inlineObject,
        BOOL isSideways,
        BOOL isRightToLeft,
        IUnknown* clientDrawingEffect)
{
    return S_OK;
}

/* IDWritePixelSnapping */
IFACEMETHODIMP JFXTextRenderer::IsPixelSnappingDisabled (
        void* clientDrawingContext,
        BOOL* isDisabled)
{
    *isDisabled = FALSE;
    return S_OK;
}

IFACEMETHODIMP JFXTextRenderer::GetCurrentTransform (
        void* clientDrawingContext,
        DWRITE_MATRIX* transform)
{
    const DWRITE_MATRIX ident = {1.0, 0.0, 0.0, 1.0, 0.0, 0.0};
    *transform = ident;
    return S_OK;
}

IFACEMETHODIMP JFXTextRenderer::GetPixelsPerDip (
        void* clientDrawingContext,
        FLOAT* pixelsPerDip)
{
    *pixelsPerDip = 1.0f;
    return S_OK;
}

/* IUnknown */
IFACEMETHODIMP_(ULONG) JFXTextRenderer::AddRef() {
    return InterlockedIncrement(&cRefCount_);
}

IFACEMETHODIMP_(ULONG) JFXTextRenderer::Release() {
    ULONG newCount = InterlockedDecrement(&cRefCount_);
    if (newCount == 0) {
        delete this;
        return 0;
    }
    return newCount;
}

IFACEMETHODIMP JFXTextRenderer::QueryInterface(IID const& riid, void** ppvObject) {
    if (__uuidof(IDWriteTextRenderer) == riid) {
        *ppvObject = this;
    } else if (__uuidof(IDWritePixelSnapping) == riid) {
        *ppvObject = this;
    } else if (__uuidof(IUnknown) == riid) {
        *ppvObject = this;
    } else {
        *ppvObject = NULL;
        return E_FAIL;
    }
    this->AddRef();
    return S_OK;
}

BOOL JFXTextRenderer::Next() {
    position_++;
    return ((UINT32)position_) < runs_.size();
}

UINT32 JFXTextRenderer::GetStart() {
    if (((UINT32)position_) >= runs_.size()) return 0;
    return runs_[position_].glyphRunDescription.textPosition;
}

UINT32 JFXTextRenderer::GetLength() {
    if (((UINT32)position_) >= runs_.size()) return 0;
    return runs_[position_].glyphRunDescription.stringLength;
}

UINT32 JFXTextRenderer::GetGlyphCount() {
    if (((UINT32)position_) >= runs_.size()) return 0;
    return runs_[position_].glyphRun.glyphCount;
}

UINT32 JFXTextRenderer::GetTotalGlyphCount() {
    return totalGlyphCount_;
}

IDWriteFontFace* JFXTextRenderer::GetFontFace() {
    if (((UINT32)position_) >= runs_.size()) return NULL;
    return runs_[position_].glyphRun.fontFace;
}

const FLOAT* JFXTextRenderer::GetGlyphAdvances() {
    if (((UINT32)position_) >= runs_.size()) return NULL;
    return runs_[position_].glyphRun.glyphAdvances;
}

const DWRITE_GLYPH_OFFSET* JFXTextRenderer::GetGlyphOffsets() {
    if (((UINT32)position_) >= runs_.size()) return NULL;
    return runs_[position_].glyphRun.glyphOffsets;
}

const UINT16* JFXTextRenderer::GetGlyphIndices() {
    if (((UINT32)position_) >= runs_.size()) return NULL;
    return runs_[position_].glyphRun.glyphIndices;
}

const UINT16* JFXTextRenderer::GetClusterMap() {
    if (((UINT32)position_) >= runs_.size()) return NULL;
    return runs_[position_].glyphRunDescription.clusterMap;
}

JNIEXPORT jlong JNICALL OS_NATIVE(_1NewJFXTextRenderer)
(JNIEnv *env, jclass that)
{
    return (jlong) new (std::nothrow) JFXTextRenderer();
}

JNIEXPORT jboolean JNICALL OS_NATIVE(JFXTextRendererNext)
(JNIEnv *env, jclass that, jlong arg0) {
    return ((JFXTextRenderer*)arg0)->Next();
}

JNIEXPORT jint JNICALL OS_NATIVE(JFXTextRendererGetStart)
(JNIEnv *env, jclass that, jlong arg0) {
    return ((JFXTextRenderer*)arg0)->GetStart();
}

JNIEXPORT jint JNICALL OS_NATIVE(JFXTextRendererGetLength)
(JNIEnv *env, jclass that, jlong arg0) {
    return ((JFXTextRenderer*)arg0)->GetLength();
}

JNIEXPORT jint JNICALL OS_NATIVE(JFXTextRendererGetGlyphCount)
(JNIEnv *env, jclass that, jlong arg0) {
    return ((JFXTextRenderer*)arg0)->GetGlyphCount();
}

JNIEXPORT jint JNICALL OS_NATIVE(JFXTextRendererGetTotalGlyphCount)
(JNIEnv *env, jclass that, jlong arg0) {
    return ((JFXTextRenderer*)arg0)->GetTotalGlyphCount();
}

JNIEXPORT jlong JNICALL OS_NATIVE(JFXTextRendererGetFontFace)
(JNIEnv *env, jclass that, jlong arg0) {
    return (jlong)((JFXTextRenderer*)arg0)->GetFontFace();
}

JNIEXPORT jint JNICALL OS_NATIVE(JFXTextRendererGetGlyphIndices)
(JNIEnv *env, jclass that, jlong arg0, jintArray arg1, jint start, jint slot) {
    if (!arg1) return 0;
    jint* data = env->GetIntArrayElements(arg1, NULL);
    if (!data) return 0;

    JFXTextRenderer* renderer = (JFXTextRenderer*)arg0;
    // Type cast unsigned int to int. It is safe to assume that GetGlyphCount will never exceed max of jint
    jint glyphCount = (jint) renderer->GetGlyphCount();
    jint length = env->GetArrayLength(arg1);
    jint copiedCount = length - start > glyphCount ? glyphCount : length - start;

    const UINT16* indices = renderer->GetGlyphIndices();
    UINT32 i;
    for (i = 0; i < copiedCount; i++) {
        data[i + start] = (indices[i] | slot);
    }
    env->ReleaseIntArrayElements(arg1, data, NULL);
    return copiedCount;
}

JNIEXPORT jint JNICALL OS_NATIVE(JFXTextRendererGetGlyphAdvances)
(JNIEnv *env, jclass that, jlong arg0, jfloatArray arg1, jint start) {
    if (!arg1) return 0;
    jfloat* data = env->GetFloatArrayElements(arg1, NULL);
    if (!data) return 0;

    JFXTextRenderer* renderer = (JFXTextRenderer*)arg0;
    // Type cast unsigned int to int. It is safe to assume that GetGlyphCount will never exceed max of jint
    jint glyphCount = (jint) renderer->GetGlyphCount();
    jint length = env->GetArrayLength(arg1);
    jint copiedCount = length - start > glyphCount ? glyphCount : length - start;

    const FLOAT* advances = renderer->GetGlyphAdvances();
    UINT32 i;
    for (i = 0; i < copiedCount; i++) {
        data[i + start] = advances[i];
    }
    env->ReleaseFloatArrayElements(arg1, data, NULL);
    return copiedCount;
}

JNIEXPORT jint JNICALL OS_NATIVE(JFXTextRendererGetGlyphOffsets)
(JNIEnv *env, jclass that, jlong arg0, jfloatArray arg1, jint start) {
    if (!arg1) return 0;
    jfloat* data = env->GetFloatArrayElements(arg1, NULL);
    if (!data) return 0;

    JFXTextRenderer* renderer = (JFXTextRenderer*)arg0;
    // Type cast unsigned int to int. It is safe to assume the result will never exceed max of jint
    jint offsetCount = (jint) renderer->GetGlyphCount() * 2;
    jint length = env->GetArrayLength(arg1);
    jint copiedCount = length - start > offsetCount ? offsetCount : length - start;

    const DWRITE_GLYPH_OFFSET* offsets = renderer->GetGlyphOffsets();
    UINT32 i = 0, j = 0;
    while (i < copiedCount) {
        DWRITE_GLYPH_OFFSET offset = offsets[j++];
        data[start + i++] = offset.advanceOffset;
        data[start + i++] = offset.ascenderOffset;
    }
    env->ReleaseFloatArrayElements(arg1, data, NULL);
    return copiedCount;
}

JNIEXPORT jint JNICALL OS_NATIVE(JFXTextRendererGetClusterMap)
(JNIEnv *env, jclass that, jlong arg0, jshortArray arg1, jint start, jint glyphStart) {
    if (!arg1) return 0;
    jshort* data = env->GetShortArrayElements(arg1, NULL);
    if (!data) return 0;

    JFXTextRenderer* renderer = (JFXTextRenderer*)arg0;
    // Type cast unsigned int to int. It is safe to assume that GetLength will never exceed max of jint
    jint textLength = (jint) renderer->GetLength();
    jint length = env->GetArrayLength(arg1);
    jint copiedCount = length - start > textLength ? textLength : length - start;

    const UINT16* map = renderer->GetClusterMap();
    UINT32 i;
    /* Adding start to the result as in Java it needs the cluster map
     * relative to start of the TextRun and the cluster map computed
     * by DirectWrite has it relative to the DWRITE_GLYPH_RUN.
     */
    for (i = 0; i < copiedCount; i++) {
        data[i + start] = map[i] + (jshort)glyphStart;
    }
    env->ReleaseShortArrayElements(arg1, data, NULL);
    return copiedCount;
}

/***********************************************/
/*                Glyph Outline                */
/***********************************************/

class JFXGeometrySink : public IDWriteGeometrySink {
public:
    JFXGeometrySink();
    ~JFXGeometrySink();
    int numTypes();
    int numCoords();
    jbyte* types();
    jfloat* coords();

/* IDWriteGeometrySink */
public:
    IFACEMETHOD_(void, SetFillMode)(
        D2D1_FILL_MODE fillMode);

    IFACEMETHOD_(void, SetSegmentFlags)(
        D2D1_PATH_SEGMENT vertexFlags);

    IFACEMETHOD_(void, BeginFigure)(
        D2D1_POINT_2F startPoint,
        D2D1_FIGURE_BEGIN figureBegin);

    IFACEMETHOD_(void, AddLines)(
        CONST D2D1_POINT_2F *points,
        UINT32 pointsCount);

    IFACEMETHOD_(void, AddBeziers)(
        CONST D2D1_BEZIER_SEGMENT *beziers,
        UINT32 beziersCount);

    IFACEMETHOD_(void, EndFigure)(
        D2D1_FIGURE_END figureEnd);

    IFACEMETHOD(Close)();

/* IUnknown */
public:
    IFACEMETHOD_(ULONG, AddRef) ();
    IFACEMETHOD_(ULONG, Release) ();
    IFACEMETHOD(QueryInterface) (
        IID const& riid,
        void** ppvObject
    );

private:
    ULONG cRefCount_;
    std::vector<jbyte> vtypes_;
    std::vector<jfloat> vcoords_;
};

JFXGeometrySink::JFXGeometrySink()
: cRefCount_(0) {
}

JFXGeometrySink::~JFXGeometrySink() {
}

int JFXGeometrySink::numTypes() {
    return vtypes_.size();
}

int JFXGeometrySink::numCoords() {
    return vcoords_.size();
}

jbyte* JFXGeometrySink::types() {
    return vtypes_.data();
}

jfloat* JFXGeometrySink::coords() {
    return vcoords_.data();
}

/* IDWriteGeometrySink */
IFACEMETHODIMP_(void) JFXGeometrySink::SetFillMode(D2D1_FILL_MODE fillMode) {
    /* ignored */
}

IFACEMETHODIMP_(void) JFXGeometrySink::SetSegmentFlags(D2D1_PATH_SEGMENT vertexFlags) {
    /* ignored */
}

IFACEMETHODIMP_(void) JFXGeometrySink::BeginFigure(D2D1_POINT_2F startPoint, D2D1_FIGURE_BEGIN figureBegin) {
    /* Handle as move to point, ignore figureBegin (hollow/filled) */
    vtypes_.push_back(0);
    vcoords_.push_back(startPoint.x);
    vcoords_.push_back(startPoint.y);
}

IFACEMETHODIMP_(void) JFXGeometrySink::AddLines(CONST D2D1_POINT_2F *points, UINT32 pointsCount) {
    UINT i;
    for (i = 0; i < pointsCount; i++) {
        D2D1_POINT_2F pt = points[i];
        vtypes_.push_back(1);
        vcoords_.push_back(pt.x);
        vcoords_.push_back(pt.y);
    }
}

IFACEMETHODIMP_(void) JFXGeometrySink::AddBeziers(CONST D2D1_BEZIER_SEGMENT *beziers, UINT32 beziersCount) {
    UINT i;
    for (i = 0; i < beziersCount; i++) {
        /* The API for simplified geometry sink does not have quad bezier curve (type2),
         * which I suspect is done using cubic bezier with point1==point2 */
        D2D1_BEZIER_SEGMENT b = beziers[i];
        vtypes_.push_back(3);
        vcoords_.push_back(b.point1.x);
        vcoords_.push_back(b.point1.y);
        vcoords_.push_back(b.point2.x);
        vcoords_.push_back(b.point2.y);
        vcoords_.push_back(b.point3.x);
        vcoords_.push_back(b.point3.y);
    }
}

IFACEMETHODIMP_(void) JFXGeometrySink::EndFigure (D2D1_FIGURE_END figureEnd) {
    /* Handle as close subpath */
    vtypes_.push_back(4);
}

IFACEMETHODIMP JFXGeometrySink::Close () {
    return S_OK;
}

/* IUnknown */
IFACEMETHODIMP_(ULONG) JFXGeometrySink::AddRef() {
    return InterlockedIncrement(&cRefCount_);
}

IFACEMETHODIMP_(ULONG) JFXGeometrySink::Release() {
    ULONG newCount = InterlockedDecrement(&cRefCount_);
    if (newCount == 0) {
        delete this;
        return 0;
    }
    return newCount;
}

IFACEMETHODIMP JFXGeometrySink::QueryInterface(IID const& riid, void** ppvObject) {
    if (__uuidof(IDWriteGeometrySink) == riid) {
        *ppvObject = this;
    } else if (__uuidof(ID2D1SimplifiedGeometrySink) == riid) {
        *ppvObject = this;
    } else if (__uuidof(IUnknown) == riid) {
        *ppvObject = this;
    } else {
        *ppvObject = NULL;
        return E_FAIL;
    }
    this->AddRef();
    return S_OK;
}

/* IDWriteFontFace */
JNIEXPORT jobject JNICALL OS_NATIVE(GetGlyphRunOutline)
    (JNIEnv *env, jclass that, jlong arg0, jfloat arg1, jshort arg2, jboolean arg3)
{
    HRESULT hr = E_FAIL;
    jobject result = NULL;
    const UINT32  glyphCount = 1;
    const UINT16 glyphIndices[glyphCount] = {arg2};
    JFXGeometrySink* sink = new (std::nothrow) JFXGeometrySink();
    if (sink == NULL) return NULL;

    hr = ((IDWriteFontFace *)arg0)->GetGlyphRunOutline(arg1, glyphIndices, NULL, NULL, glyphCount, arg3, FALSE, sink);

    static jclass path2DClass = NULL;
    static jmethodID path2DCtr = NULL;
    if (path2DClass == NULL) {
        jclass tmpClass = env->FindClass("com/sun/javafx/geom/Path2D");
        if (env->ExceptionOccurred() || !tmpClass) {
            fprintf(stderr, "OS_NATIVE error: JNI exception or tmpClass == NULL");
            goto fail;
        }
        path2DClass = (jclass)env->NewGlobalRef(tmpClass);
        path2DCtr = env->GetMethodID(path2DClass, "<init>", "(I[BI[FI)V");
        if (env->ExceptionOccurred() || !path2DCtr) {
            fprintf(stderr, "OS_NATIVE error: JNI exception or path2DCtr == NULL");
            goto fail;
        }
    }

    if (SUCCEEDED(hr)) {
        jbyteArray types = env->NewByteArray(sink->numTypes());
        jfloatArray coords = env->NewFloatArray(sink->numCoords());
        if (types && coords) {
            env->SetByteArrayRegion(types, 0, sink->numTypes(), sink->types());
            env->SetFloatArrayRegion(coords, 0, sink->numCoords(), sink->coords());
            result = env->NewObject(path2DClass, path2DCtr,
                                    0 /*winding rule*/,
                                    types, sink->numTypes(),
                                    coords, sink->numCoords());
          }
    }
fail:
    delete sink;
    return result;
}

JNIEXPORT jobject JNICALL OS_NATIVE(GetDesignGlyphMetrics)
    (JNIEnv *env, jclass that, jlong arg0, jshort arg1, jboolean arg2)
{
    HRESULT hr = E_FAIL;
    jobject result = NULL;
    const UINT32  glyphCount = 1;
    const UINT16 glyphIndices[glyphCount] = {arg1};
    DWRITE_GLYPH_METRICS glyphMetrics[glyphCount];

    hr = ((IDWriteFontFace *)arg0)->GetDesignGlyphMetrics(glyphIndices, glyphCount, glyphMetrics, arg2);
    if (SUCCEEDED(hr)) {
        result = newDWRITE_GLYPH_METRICS(env, &glyphMetrics[0]);
    }
    return result;
}

/* IDWriteFactory */
JNIEXPORT jlong JNICALL OS_NATIVE(CreateTextAnalyzer)
    (JNIEnv *env, jclass that, jlong arg0)
{
    IDWriteTextAnalyzer* result = NULL;
    HRESULT hr = ((IDWriteFactory *)arg0)->CreateTextAnalyzer(&result);
    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

JNIEXPORT jlong JNICALL OS_NATIVE(CreateTextFormat)
    (JNIEnv *env, jclass that, jlong arg0, jcharArray arg1, jlong arg2, jint arg3,
    jint arg4, jint arg5, jfloat arg6, jcharArray arg7)
{
    HRESULT hr = E_FAIL;
    IDWriteTextFormat* result = NULL;
    jchar *lparg1 = NULL;
    jchar *lparg7 = NULL;
    if (arg1) if ((lparg1 = env->GetCharArrayElements(arg1, NULL)) == NULL) goto fail;
    if (arg7) if ((lparg7 = env->GetCharArrayElements(arg7, NULL)) == NULL) goto fail;
    hr = ((IDWriteFactory *)arg0)->CreateTextFormat((const WCHAR *)lparg1,
                                                    (IDWriteFontCollection *)arg2,
                                                    (DWRITE_FONT_WEIGHT)arg3,
                                                    (DWRITE_FONT_STYLE)arg4,
                                                    (DWRITE_FONT_STRETCH)arg5,
                                                    arg6,
                                                    (const WCHAR *)lparg7,
                                                    &result);
fail:
    if (arg1 && lparg1) env->ReleaseCharArrayElements(arg1, lparg1, 0);
    if (arg7 && lparg7) env->ReleaseCharArrayElements(arg7, lparg7, 0);
    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

JNIEXPORT jlong JNICALL OS_NATIVE(CreateFontFileReference)
    (JNIEnv *env, jclass that, jlong arg0, jcharArray arg1)
{
    HRESULT hr = E_FAIL;
    IDWriteFontFile* result = NULL;
    jchar *lparg1 = NULL;
    if (arg1) if ((lparg1 = env->GetCharArrayElements(arg1, NULL)) == NULL) goto fail;
    hr = ((IDWriteFactory *)arg0)->CreateFontFileReference((const WCHAR *)lparg1, NULL, &result);
fail:
    if (arg1 && lparg1) env->ReleaseCharArrayElements(arg1, lparg1, 0);
    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

JNIEXPORT jlong JNICALL OS_NATIVE(CreateFontFace__JIJII)
    (JNIEnv *env, jclass that, jlong arg0, jint arg1, jlong arg3, jint arg4, jint arg5)
{
    IDWriteFontFace* result = NULL;
    const UINT32  numberOfFiles = 1;
    IDWriteFontFile* fontFileArray[numberOfFiles] = {(IDWriteFontFile*)arg3};
    HRESULT hr = ((IDWriteFactory *)arg0)->CreateFontFace((DWRITE_FONT_FACE_TYPE)arg1,
                                                          numberOfFiles,
                                                          fontFileArray,
                                                          (UINT32)arg4,
                                                          (DWRITE_FONT_SIMULATIONS)arg5,
                                                          &result);

    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

JNIEXPORT jlong JNICALL OS_NATIVE(CreateTextLayout)
    (JNIEnv *env, jclass that, jlong arg0, jcharArray arg1, jint start, jint count, jlong arg4,
    jfloat arg5, jfloat arg6)
{
    HRESULT hr = E_FAIL;
    IDWriteTextLayout* result = NULL;
    jchar *lparg1 = NULL;
    if (arg1) if ((lparg1 = env->GetCharArrayElements(arg1, NULL)) == NULL) goto fail;
    if (start + count > env->GetArrayLength(arg1)) goto fail;

    const WCHAR * text = (const WCHAR *)(lparg1 + start);

    hr = ((IDWriteFactory *)arg0)->CreateTextLayout(text,
                                                    (UINT32)count,
                                                    (IDWriteTextFormat *)arg4,
                                                    (FLOAT)arg5,
                                                    (FLOAT)arg6,
                                                    &result);
fail:
    if (arg1 && lparg1) env->ReleaseCharArrayElements(arg1, lparg1, 0);

    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

JNIEXPORT jlong JNICALL OS_NATIVE(GetSystemFontCollection)
    (JNIEnv *env, jclass that, jlong arg0, jboolean arg1)
{
    IDWriteFontCollection* result = NULL;
    HRESULT hr = ((IDWriteFactory *)arg0)->GetSystemFontCollection(&result, arg1);
    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

JNIEXPORT jlong JNICALL OS_NATIVE(CreateGlyphRunAnalysis)
    (JNIEnv *env, jclass that, jlong arg0, jobject arg1, jfloat arg2, jobject arg3, jint arg4, jint arg5, jfloat arg6, jfloat arg7)
{
    HRESULT hr = E_FAIL;
    IDWriteGlyphRunAnalysis* result = NULL;
    DWRITE_GLYPH_RUN _arg1, *lparg1 = NULL;
    DWRITE_MATRIX _arg3, *lparg3 = NULL;
    _arg1.glyphCount = 1;
    _arg1.glyphIndices = new (std::nothrow) UINT16 [1];
    _arg1.glyphAdvances = new (std::nothrow) FLOAT [1];
    _arg1.glyphOffsets = new (std::nothrow) DWRITE_GLYPH_OFFSET [1];

    /* In Only */
    if (arg1) if ((lparg1 = getDWRITE_GLYPH_RUNFields(env, arg1, &_arg1)) == NULL) goto fail;
    if (arg3) if ((lparg3 = getDWRITE_MATRIXFields(env, arg3, &_arg3)) == NULL) goto fail;
    hr = ((IDWriteFactory *)arg0)->CreateGlyphRunAnalysis(lparg1,
                                                          (FLOAT)arg2,
                                                          lparg3,
                                                          (DWRITE_RENDERING_MODE)arg4,
                                                          (DWRITE_MEASURING_MODE)arg5,
                                                          (FLOAT)arg6,
                                                          (FLOAT)arg7,
                                                          &result);

fail:
    delete [] _arg1.glyphIndices;
    delete [] _arg1.glyphAdvances;
    delete [] _arg1.glyphOffsets;
    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

/* IDWriteFontFile */
JNIEXPORT jint JNICALL OS_NATIVE(Analyze)
    (JNIEnv *env, jclass that, jlong arg0, jbooleanArray arg1, jintArray arg2, jintArray arg3, jintArray arg4)
{
    if (arg0 == NULL) return E_FAIL;
    IDWriteFontFile* fontFile = (IDWriteFontFile*)arg0;
    BOOL isSupportedFontType;
    DWRITE_FONT_FILE_TYPE fontFileType;
    DWRITE_FONT_FACE_TYPE fontFaceType;
    UINT32 numberOfFaces;
    HRESULT hr = fontFile->Analyze(&isSupportedFontType,
                                   &fontFileType,
                                   &fontFaceType,
                                   &numberOfFaces);

    if (arg1 && env->GetArrayLength(arg1) == 1) {
        jboolean *lparg1 = env->GetBooleanArrayElements(arg1, NULL);
        if (lparg1) {
            lparg1[0] = isSupportedFontType;
            env->ReleaseBooleanArrayElements(arg1, lparg1, 0);
        }
    }
    if (arg2 && env->GetArrayLength(arg2) == 1) {
        jint *lparg2 = env->GetIntArrayElements(arg2, NULL);
        if (lparg2) {
            lparg2[0] = fontFileType;
            env->ReleaseIntArrayElements(arg2, lparg2, 0);
        }
    }
    if (arg3 && env->GetArrayLength(arg3) == 1) {
        jint *lparg3 = env->GetIntArrayElements(arg3, NULL);
        if (lparg3) {
            lparg3[0] = fontFaceType;
            env->ReleaseIntArrayElements(arg3, lparg3, 0);
        }
    }
    if (arg4 && env->GetArrayLength(arg4) == 1) {
        jint *lparg4 = env->GetIntArrayElements(arg4, NULL);
        if (lparg4) {
            lparg4[0] = numberOfFaces;
            env->ReleaseIntArrayElements(arg4, lparg4, 0);
        }
    }
    return hr;
}

/* IDWriteFont */
JNIEXPORT jlong JNICALL OS_NATIVE(CreateFontFace__J)
    (JNIEnv *env, jclass that, jlong arg0)
{
    IDWriteFontFace* result = NULL;
    HRESULT hr = ((IDWriteFont *)arg0)->CreateFontFace(&result);
    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

JNIEXPORT jlong JNICALL OS_NATIVE(GetFaceNames)
    (JNIEnv *env, jclass that, jlong arg0)
{
    IDWriteLocalizedStrings* result = NULL;
    HRESULT hr = ((IDWriteFont *)arg0)->GetFaceNames(&result);
    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

JNIEXPORT jlong JNICALL OS_NATIVE(GetFontFamily__J)
    (JNIEnv *env, jclass that, jlong arg0)
{
    IDWriteFontFamily* result = NULL;
    HRESULT hr = ((IDWriteFont *)arg0)->GetFontFamily(&result);
    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

JNIEXPORT jint JNICALL OS_NATIVE(GetStretch)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jint)((IDWriteFont *)arg0)->GetStretch();
}
JNIEXPORT jint JNICALL OS_NATIVE(GetStyle)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jint)((IDWriteFont *)arg0)->GetStyle();
}

JNIEXPORT jint JNICALL OS_NATIVE(GetWeight)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jint)((IDWriteFont *)arg0)->GetWeight();
}

JNIEXPORT jlong JNICALL OS_NATIVE(GetInformationalStrings)
    (JNIEnv *env, jclass that, jlong arg0, jint arg1)
{
    IDWriteLocalizedStrings* result = NULL;
    BOOL exists = FALSE;
    HRESULT hr = ((IDWriteFont *)arg0)->GetInformationalStrings((DWRITE_INFORMATIONAL_STRING_ID)arg1,
                                                                &result,
                                                                &exists);
    return SUCCEEDED(hr) && exists ? (jlong)result : NULL;
}

JNIEXPORT jint JNICALL OS_NATIVE(GetSimulations)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jint)((IDWriteFont *)arg0)->GetSimulations();
}

/* IDWriteFontList */
JNIEXPORT jint JNICALL OS_NATIVE(GetFontCount)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return ((IDWriteFontList *)arg0)->GetFontCount();
}

JNIEXPORT jlong JNICALL OS_NATIVE(GetFont)
    (JNIEnv *env, jclass that, jlong arg0, jint arg1)
{
    IDWriteFont* result = NULL;
    HRESULT hr = ((IDWriteFontList *)arg0)->GetFont(arg1, &result);
    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

/* IDWriteLocalizedStrings */
JNIEXPORT jcharArray JNICALL OS_NATIVE(GetString)
    (JNIEnv *env, jclass that, jlong arg0, jint arg1, jint arg2)
{
    jcharArray result = NULL;
    WCHAR* buffer = new (std::nothrow) WCHAR[arg2];
    HRESULT hr = ((IDWriteLocalizedStrings *)arg0)->GetString(arg1, buffer, arg2);
    if (SUCCEEDED(hr)) {
        result = env->NewCharArray(arg2);
        if (result) {
            env->SetCharArrayRegion(result, 0, arg2, (const jchar*)buffer);
        }
    }
    delete [] buffer;
    return result;
}

JNIEXPORT jint JNICALL OS_NATIVE(GetStringLength)
    (JNIEnv *env, jclass that, jlong arg0, jint arg1)
{
    UINT32 result = 0;
    HRESULT hr = ((IDWriteLocalizedStrings *)arg0)->GetStringLength(arg1, &result);
    return SUCCEEDED(hr) ? result : 0;
}

JNIEXPORT jint JNICALL OS_NATIVE(FindLocaleName)
    (JNIEnv *env, jclass that, jlong arg0, jcharArray arg1)
{
    HRESULT hr = E_FAIL;
    jchar *lparg1 = NULL;
    UINT32 result = 0;
    BOOL exists = FALSE;
    if (arg1) if ((lparg1 = env->GetCharArrayElements(arg1, NULL)) == NULL) goto fail;
    hr = ((IDWriteLocalizedStrings *)arg0)->FindLocaleName((const WCHAR *) lparg1, &result, &exists);
fail:
    if (arg1 && lparg1) env->ReleaseCharArrayElements(arg1, lparg1, 0);
    return SUCCEEDED(hr) && exists ? result : -1;
}

/* IDWriteFontFamily */
JNIEXPORT jlong JNICALL OS_NATIVE(GetFamilyNames)
    (JNIEnv *env, jclass that, jlong arg0)
{
    IDWriteLocalizedStrings* result = NULL;
    HRESULT hr = ((IDWriteFontFamily *)arg0)->GetFamilyNames(&result);
    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

JNIEXPORT jlong JNICALL OS_NATIVE(GetFirstMatchingFont)
    (JNIEnv *env, jclass that, jlong arg0, jint arg1, jint arg2, jint arg3)
{
    IDWriteFont* result = NULL;
    HRESULT hr = ((IDWriteFontFamily *)arg0)->GetFirstMatchingFont((DWRITE_FONT_WEIGHT)arg1, (DWRITE_FONT_STRETCH)arg2, (DWRITE_FONT_STYLE)arg3, &result);
    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

/* IDWriteFontCollection */
JNIEXPORT jint JNICALL OS_NATIVE(GetFontFamilyCount)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return ((IDWriteFontCollection *)arg0)->GetFontFamilyCount();
}

JNIEXPORT jlong JNICALL OS_NATIVE(GetFontFamily__JI)
    (JNIEnv *env, jclass that, jlong arg0, jint arg1)
{
    IDWriteFontFamily* result = NULL;
    HRESULT hr = ((IDWriteFontCollection *)arg0)->GetFontFamily(arg1, &result);
    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

JNIEXPORT jint JNICALL OS_NATIVE(FindFamilyName)
    (JNIEnv *env, jclass that, jlong arg0, jcharArray arg1)
{
    HRESULT hr = E_FAIL;
    jchar *lparg1 = NULL;
    UINT32 result = 0;
    BOOL exists = FALSE;
    if (arg1) if ((lparg1 = env->GetCharArrayElements(arg1, NULL)) == NULL) goto fail;
    hr = ((IDWriteFontCollection *)arg0)->FindFamilyName((const WCHAR *) lparg1, &result, &exists);
fail:
    if (arg1 && lparg1) env->ReleaseCharArrayElements(arg1, lparg1, 0);
    return SUCCEEDED(hr) && exists ? result : -1;
}

JNIEXPORT jlong JNICALL OS_NATIVE(GetFontFromFontFace)
    (JNIEnv *env, jclass that, jlong arg0, jlong arg1)
{
    IDWriteFont* result = NULL;
    HRESULT hr = ((IDWriteFontCollection *)arg0)->GetFontFromFontFace((IDWriteFontFace *)arg1, &result);
    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

/* IDWriteGlyphRunAnalysis */
JNIEXPORT jbyteArray JNICALL OS_NATIVE(CreateAlphaTexture)
    (JNIEnv *env, jclass that, jlong arg0, jint arg1, jobject arg2)
{
    jbyteArray result = NULL;
    RECT _arg2, *lparg2 = NULL;
    /* In Only */
    if (arg2) lparg2 = getRECTFields(env, arg2, &_arg2);
    if (!lparg2) return NULL;
    DWRITE_TEXTURE_TYPE textureType = (DWRITE_TEXTURE_TYPE)arg1;
    UINT32 width = lparg2->right - lparg2->left;
    UINT32 height = lparg2->bottom - lparg2->top;
    UINT32 bpp = textureType == DWRITE_TEXTURE_CLEARTYPE_3x1 ? 3 : 1;
    UINT32 bufferSize = width * height * bpp;
    BYTE * buffer = new (std::nothrow) BYTE[bufferSize];
    HRESULT hr = ((IDWriteGlyphRunAnalysis *)arg0)->CreateAlphaTexture(textureType, lparg2, buffer, bufferSize);
    if (SUCCEEDED(hr)) {
        result = env->NewByteArray(bufferSize);
        if (result) {
            env->SetByteArrayRegion(result, 0, bufferSize, (jbyte*)buffer);
        }
    }
    delete [] buffer;
    return result;
}

JNIEXPORT jobject JNICALL OS_NATIVE(GetAlphaTextureBounds)
    (JNIEnv *env, jclass that, jlong arg0, jint arg1)
{
    jobject result = NULL;
    RECT rect;
    HRESULT hr = ((IDWriteGlyphRunAnalysis *)arg0)->GetAlphaTextureBounds((DWRITE_TEXTURE_TYPE)arg1, &rect);
    if (SUCCEEDED(hr)) {
        result = newRECT(env, &rect);
    }
    return result;
}

/* IDWriteTextAnalyzer */
JNIEXPORT jint JNICALL OS_NATIVE(AnalyzeScript)
    (JNIEnv *env, jclass that, jlong arg0, jlong arg1, jint arg2, jint arg3, jlong arg4)
{
    JFXTextAnalysisSink* source = (JFXTextAnalysisSink *)arg1;
    JFXTextAnalysisSink* sink = (JFXTextAnalysisSink *)arg4;
    IDWriteTextAnalyzer* analyzer = (IDWriteTextAnalyzer *)arg0;
    return analyzer->AnalyzeScript(source, arg2, arg3, sink);
}

JNIEXPORT jint JNICALL OS_NATIVE(GetGlyphs)
    (JNIEnv *env, jclass that, jlong arg0, jcharArray arg1, jint textStart, jint arg2, jlong arg3, jboolean arg4,
    jboolean arg5, jobject arg6, jcharArray arg7, jlong arg8, jlongArray arg9, jintArray arg10,
    jint arg11, jint arg12, jshortArray arg13, jshortArray arg14, jshortArray arg15, jshortArray arg16, jintArray arg17)
{
    HRESULT hr = E_FAIL;
    jchar *lparg1=NULL;
    DWRITE_SCRIPT_ANALYSIS _arg6, *lparg6=NULL;
    jchar *lparg7=NULL;
    jlong *lparg9=NULL;
    jint *lparg10=NULL;
    jshort *lparg13=NULL;
    jshort *lparg14=NULL;
    jshort *lparg15=NULL;
    jshort *lparg16=NULL;
    jint *lparg17=NULL;

    if (arg1) if ((lparg1 = env->GetCharArrayElements(arg1, NULL)) == NULL) goto fail;
    if (arg6) if ((lparg6 = getDWRITE_SCRIPT_ANALYSISFields(env, arg6, &_arg6)) == NULL) goto fail;
    if (arg7) lparg7 = env->GetCharArrayElements(arg7, NULL); /*Optional*/
    if (arg9) if ((lparg9 = env->GetLongArrayElements(arg9, NULL)) == NULL) goto fail;
    if (arg10) if ((lparg10 = env->GetIntArrayElements(arg10, NULL)) == NULL) goto fail;
    if (arg13) if ((lparg13 = env->GetShortArrayElements(arg13, NULL)) == NULL) goto fail;
    if (arg14) if ((lparg14 = env->GetShortArrayElements(arg14, NULL)) == NULL) goto fail;
    if (arg15) if ((lparg15 = env->GetShortArrayElements(arg15, NULL)) == NULL) goto fail;
    if (arg16) if ((lparg16 = env->GetShortArrayElements(arg16, NULL)) == NULL) goto fail;
    if (arg17) if ((lparg17 = env->GetIntArrayElements(arg17, NULL)) == NULL) goto fail;
    const WCHAR* text = (const WCHAR*)(lparg1 + textStart);

    hr = ((IDWriteTextAnalyzer *)arg0)->GetGlyphs(text,
                                                  (UINT32)arg2,
                                                  (IDWriteFontFace *)arg3,
                                                  (BOOL)arg4,
                                                  (BOOL)arg5,
                                                  (const DWRITE_SCRIPT_ANALYSIS *)lparg6,
                                                  (const WCHAR *)lparg7,
                                                  (IDWriteNumberSubstitution *)arg8,
                                                  (const DWRITE_TYPOGRAPHIC_FEATURES **)lparg9,
                                                  (const UINT32 *)lparg10,
                                                  (UINT32)arg11,
                                                  (UINT32)arg12,
                                                  (UINT16 *)lparg13,
                                                  (DWRITE_SHAPING_TEXT_PROPERTIES *)lparg14,
                                                  (UINT16 *)lparg15,
                                                  (DWRITE_SHAPING_GLYPH_PROPERTIES *)lparg16,
                                                  (UINT32 *)lparg17);

fail:
    if (arg1 && lparg1) env->ReleaseCharArrayElements(arg1, lparg1, 0);
    if (arg7 && lparg7) env->ReleaseCharArrayElements(arg7, lparg7, 0);
    if (arg9 && lparg9) env->ReleaseLongArrayElements(arg9, lparg9, 0);
    if (arg10 && lparg10) env->ReleaseIntArrayElements(arg10, lparg10, 0);
    if (arg13 && lparg13) env->ReleaseShortArrayElements(arg13, lparg13, 0);
    if (arg14 && lparg14) env->ReleaseShortArrayElements(arg14, lparg14, 0);
    if (arg15 && lparg15) env->ReleaseShortArrayElements(arg15, lparg15, 0);
    if (arg16 && lparg16) env->ReleaseShortArrayElements(arg16, lparg16, 0);
    if (arg17 && lparg17) env->ReleaseIntArrayElements(arg17, lparg17, 0);
    return hr;
}

JNIEXPORT jint JNICALL OS_NATIVE(GetGlyphPlacements)
    (JNIEnv *env, jclass that, jlong arg0, jcharArray arg1, jshortArray arg2,
    jshortArray arg3, jint textStart, jint arg4, jshortArray arg5, jshortArray arg6, jint arg7,
    jlong arg8, jfloat arg9, jboolean arg10, jboolean arg11, jobject arg12,
    jcharArray arg13, jlongArray arg14, jintArray arg15, jint arg16,
    jfloatArray arg17, jfloatArray arg18) {

    HRESULT hr = E_FAIL;
    jchar *lparg1=NULL;
    jshort *lparg2=NULL;
    jshort *lparg3=NULL;
    jshort *lparg5=NULL;
    jshort *lparg6=NULL;
    DWRITE_SCRIPT_ANALYSIS _arg12, *lparg12=NULL;
    jchar *lparg13=NULL;
    jlong *lparg14=NULL;
    jint *lparg15=NULL;
    jfloat *lparg17=NULL;
    jfloat *lparg18=NULL;

    if (arg1) if ((lparg1 = env->GetCharArrayElements(arg1, NULL)) == NULL) goto fail;
    if (arg2) if ((lparg2 = env->GetShortArrayElements(arg2, NULL)) == NULL) goto fail;
    if (arg3) if ((lparg3 = env->GetShortArrayElements(arg3, NULL)) == NULL) goto fail;
    if (arg5) if ((lparg5 = env->GetShortArrayElements(arg5, NULL)) == NULL) goto fail;
    if (arg6) if ((lparg6 = env->GetShortArrayElements(arg6, NULL)) == NULL) goto fail;
    if (arg12) if ((lparg12 = getDWRITE_SCRIPT_ANALYSISFields(env, arg12, &_arg12)) == NULL) goto fail;
    if (arg13) lparg13 = env->GetCharArrayElements(arg13, NULL); /* Optional */
    if (arg14) if ((lparg14 = env->GetLongArrayElements(arg14, NULL)) == NULL) goto fail;
    if (arg15) if ((lparg15 = env->GetIntArrayElements(arg15, NULL)) == NULL) goto fail;
    if (arg17) if ((lparg17 = env->GetFloatArrayElements(arg17, NULL)) == NULL) goto fail;
    if (arg18) if ((lparg18 = env->GetFloatArrayElements(arg18, NULL)) == NULL) goto fail;
    const WCHAR* text = (const WCHAR*)(lparg1 + textStart);

    hr = ((IDWriteTextAnalyzer *)arg0)->GetGlyphPlacements(text,
                                                           (const UINT16 *)lparg2,
                                                           (DWRITE_SHAPING_TEXT_PROPERTIES *)lparg3,
                                                           (UINT32)arg4,
                                                           (const UINT16 *)lparg5,
                                                           (const DWRITE_SHAPING_GLYPH_PROPERTIES *)lparg6,
                                                           (UINT32)arg7,
                                                           (IDWriteFontFace *)arg8,
                                                           (FLOAT)arg9,
                                                           (BOOL)arg10,
                                                           (BOOL)arg11,
                                                           (const DWRITE_SCRIPT_ANALYSIS *)lparg12,
                                                           (const WCHAR *)lparg13,
                                                           (const DWRITE_TYPOGRAPHIC_FEATURES **)lparg14,
                                                           (const UINT32 *)lparg15,
                                                           (UINT32)arg16,
                                                           (FLOAT *)lparg17,
                                                           (DWRITE_GLYPH_OFFSET *)lparg18);

fail:
    if (arg1 && lparg1) env->ReleaseCharArrayElements(arg1, lparg1, 0);
    if (arg2 && lparg2) env->ReleaseShortArrayElements(arg2, lparg2, 0);
    if (arg3 && lparg3) env->ReleaseShortArrayElements(arg3, lparg3, 0);
    if (arg5 && lparg5) env->ReleaseShortArrayElements(arg5, lparg5, 0);
    if (arg6 && lparg6) env->ReleaseShortArrayElements(arg6, lparg6, 0);
    if (arg13 && lparg13) env->ReleaseCharArrayElements(arg13, lparg13, 0);
    if (arg14 && lparg14) env->ReleaseLongArrayElements(arg14, lparg14, 0);
    if (arg15 && lparg15) env->ReleaseIntArrayElements(arg15, lparg15, 0);
    if (arg17 && lparg17) env->ReleaseFloatArrayElements(arg17, lparg17, 0);
    if (arg18 && lparg18) env->ReleaseFloatArrayElements(arg18, lparg18, 0);

    return hr;
}

/*IDWriteTextLayout*/
JNIEXPORT jint JNICALL OS_NATIVE(Draw)
    (JNIEnv *env, jclass that, jlong arg0, jlong arg1, jlong arg2, jfloat arg3, jfloat arg4)
{
    return ((IDWriteTextLayout *)arg0)->Draw((void*) arg1, (IDWriteTextRenderer *)arg2, arg3, arg4);
}

/* IWICImagingFactory*/
JNIEXPORT jlong JNICALL OS_NATIVE(CreateBitmap)
    (JNIEnv *env, jclass that, jlong arg0, jint arg1, jint arg2, jint arg3, jint arg4)
{

    IWICBitmap* result = NULL;
    GUID pixelFormat;
    switch (arg3) {
    case com_sun_javafx_font_directwrite_OS_GUID_WICPixelFormat8bppGray:pixelFormat = GUID_WICPixelFormat8bppGray; break;
    case com_sun_javafx_font_directwrite_OS_GUID_WICPixelFormat8bppAlpha:pixelFormat = GUID_WICPixelFormat8bppAlpha; break;
    case com_sun_javafx_font_directwrite_OS_GUID_WICPixelFormat16bppGray: pixelFormat = GUID_WICPixelFormat16bppGray; break;
    case com_sun_javafx_font_directwrite_OS_GUID_WICPixelFormat24bppRGB: pixelFormat = GUID_WICPixelFormat24bppRGB; break;
    case com_sun_javafx_font_directwrite_OS_GUID_WICPixelFormat24bppBGR: pixelFormat = GUID_WICPixelFormat24bppBGR; break;
    case com_sun_javafx_font_directwrite_OS_GUID_WICPixelFormat32bppBGR: pixelFormat = GUID_WICPixelFormat32bppBGR; break;
    case com_sun_javafx_font_directwrite_OS_GUID_WICPixelFormat32bppBGRA: pixelFormat = GUID_WICPixelFormat32bppBGRA; break;
    case com_sun_javafx_font_directwrite_OS_GUID_WICPixelFormat32bppPBGRA: pixelFormat = GUID_WICPixelFormat32bppPBGRA; break;
    case com_sun_javafx_font_directwrite_OS_GUID_WICPixelFormat32bppGrayFloat: pixelFormat = GUID_WICPixelFormat32bppGrayFloat; break;
    case com_sun_javafx_font_directwrite_OS_GUID_WICPixelFormat32bppRGBA: pixelFormat = GUID_WICPixelFormat32bppRGBA; break;
    case com_sun_javafx_font_directwrite_OS_GUID_WICPixelFormat32bppPRGBA: pixelFormat = GUID_WICPixelFormat32bppPRGBA; break;
    }
    HRESULT hr = ((IWICImagingFactory *)arg0)->CreateBitmap(arg1, arg2, (REFWICPixelFormatGUID)pixelFormat, (WICBitmapCreateCacheOption)arg4, &result);
    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

/*IWICBitmap*/
JNIEXPORT jlong JNICALL OS_NATIVE(Lock)
    (JNIEnv *env, jclass that, jlong arg0, jint arg1, jint arg2, jint arg3, jint arg4, jint arg5)
{
    HRESULT hr = E_FAIL;
    IWICBitmapLock* result = NULL;
    const WICRect rcLock = {arg1, arg2, arg3, arg4};
    hr = ((IWICBitmap *)arg0)->Lock(&rcLock, arg5, &result);
    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

/*IWICBitmapLock*/
JNIEXPORT jbyteArray JNICALL OS_NATIVE(GetDataPointer)
    (JNIEnv *env, jclass that, jlong arg0)
{
    jbyteArray result = NULL;
    UINT cbBufferSize = 0;
    BYTE *pv = NULL;
    HRESULT hr = ((IWICBitmapLock *)arg0)->GetDataPointer(&cbBufferSize, &pv);
    if (SUCCEEDED(hr)) {
        result = env->NewByteArray(cbBufferSize);
        if (result) {
            env->SetByteArrayRegion(result, 0, cbBufferSize, (const jbyte*)pv);
        }
    }
    return result;
}

JNIEXPORT jint JNICALL OS_NATIVE(GetStride)
    (JNIEnv *env, jclass that, jlong arg0)
{
    UINT result = 0;
    HRESULT hr = ((IWICBitmapLock *)arg0)->GetStride(&result);
    return SUCCEEDED(hr) ? result : NULL;
}


/*ID2D1Factory */
JNIEXPORT jlong JNICALL OS_NATIVE(CreateWicBitmapRenderTarget)
    (JNIEnv *env, jclass that, jlong arg0, jlong arg1, jobject arg2)
{
    HRESULT hr = E_FAIL;
    ID2D1RenderTarget* result = NULL;
    D2D1_RENDER_TARGET_PROPERTIES _arg2, *lparg2=NULL;
    if (arg2) if ((lparg2 = getD2D1_RENDER_TARGET_PROPERTIESFields(env, arg2, &_arg2)) == NULL) goto fail;
    hr = ((ID2D1Factory *)arg0)->CreateWicBitmapRenderTarget((IWICBitmap *)arg1, lparg2, &result);
fail:
    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

/*ID2D1RenderTarget*/
JNIEXPORT void JNICALL OS_NATIVE(BeginDraw)
    (JNIEnv *env, jclass that, jlong arg0)
{
    ((ID2D1RenderTarget *)arg0)->BeginDraw();
}

JNIEXPORT jint JNICALL OS_NATIVE(EndDraw)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jint) ((ID2D1RenderTarget *)arg0)->EndDraw();
}

JNIEXPORT void JNICALL OS_NATIVE(Clear)
    (JNIEnv *env, jclass that, jlong arg0, jobject arg1)
{
    D2D1_COLOR_F _arg1, *lparg1=NULL;
    if (arg1) if ((lparg1 = getD2D1_COLOR_FFields(env, arg1, &_arg1)) == NULL) return;
    ((ID2D1RenderTarget *)arg0)->Clear(lparg1);
}

JNIEXPORT void JNICALL OS_NATIVE(SetTextAntialiasMode)
    (JNIEnv *env, jclass that, jlong arg0, jint arg1)
{
    ((ID2D1RenderTarget *)arg0)->SetTextAntialiasMode((D2D1_TEXT_ANTIALIAS_MODE)arg1);
}

JNIEXPORT void JNICALL OS_NATIVE(SetTransform)
    (JNIEnv *env, jclass that, jlong arg0, jobject arg1)
{
    D2D1_MATRIX_3X2_F _arg1, *lparg1=NULL;
    if (arg1) if ((lparg1 = getD2D1_MATRIX_3X2_FFields(env, arg1, &_arg1)) == NULL) return;
    ((ID2D1RenderTarget *)arg0)->SetTransform(lparg1);
}

JNIEXPORT void JNICALL OS_NATIVE(DrawGlyphRun)
    (JNIEnv *env, jclass that, jlong arg0, jobject arg1, jobject arg2, jlong arg3, jint arg4)
{
    D2D1_POINT_2F _arg1, *lparg1=NULL;
    DWRITE_GLYPH_RUN _arg2, *lparg2=NULL;
    _arg2.glyphCount = 1;
    _arg2.glyphIndices = new (std::nothrow) UINT16 [1];
    _arg2.glyphAdvances = new (std::nothrow) FLOAT [1];
    _arg2.glyphOffsets = new (std::nothrow) DWRITE_GLYPH_OFFSET [1];
    if (arg1) if ((lparg1 = getD2D1_POINT_2FFields(env, arg1, &_arg1)) == NULL) goto fail;
    if (arg2) if ((lparg2 = getDWRITE_GLYPH_RUNFields(env, arg2, &_arg2)) == NULL) goto fail;
    ((ID2D1RenderTarget *)arg0)->DrawGlyphRun(_arg1, lparg2, (ID2D1Brush *)arg3, (DWRITE_MEASURING_MODE)arg4);
fail:
    delete [] _arg2.glyphIndices;
    delete [] _arg2.glyphAdvances;
    delete [] _arg2.glyphOffsets;
}

JNIEXPORT jlong JNICALL OS_NATIVE(CreateSolidColorBrush)
    (JNIEnv *env, jclass that, jlong arg0, jobject arg1)
{
    HRESULT hr = E_FAIL;
    ID2D1SolidColorBrush* result = NULL;
    D2D1_COLOR_F _arg1, *lparg1=NULL;
    if (arg1) if ((lparg1 = getD2D1_COLOR_FFields(env, arg1, &_arg1)) == NULL) goto fail;
    hr = ((ID2D1RenderTarget *)arg0)->CreateSolidColorBrush(_arg1, &result);
fail:
    return SUCCEEDED(hr) ? (jlong)result : NULL;
}

#endif /* WIN32 */
