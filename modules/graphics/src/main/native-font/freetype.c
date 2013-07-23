/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

#if defined __linux__
#if defined _ENABLE_PANGO

#include <jni.h>
#include <com_sun_javafx_font_pango_OS.h>
#include <dlfcn.h>
#include <ft2build.h>
#include FT_FREETYPE_H
#include FT_OUTLINE_H
#include FT_LCD_FILTER_H

#define OS_NATIVE(func) Java_com_sun_javafx_font_pango_OS_##func

/**************************************************************************/
/*                                                                        */
/*                            Structs                                     */
/*                                                                        */
/**************************************************************************/

typedef struct FT_Matrix_FID_CACHE {
    int cached;
    jfieldID xx, xy, yx, yy;
} FT_Matrix_FID_CACHE;

FT_Matrix_FID_CACHE FT_MatrixFc;

void cacheFT_MatrixFields(JNIEnv *env, jobject lpObject)
{
    if (FT_MatrixFc.cached) return;
    jclass clazz = (*env)->GetObjectClass(env, lpObject);
    FT_MatrixFc.xx = (*env)->GetFieldID(env, clazz, "xx", "J");
    FT_MatrixFc.xy = (*env)->GetFieldID(env, clazz, "xy", "J");
    FT_MatrixFc.yx = (*env)->GetFieldID(env, clazz, "yx", "J");
    FT_MatrixFc.yy = (*env)->GetFieldID(env, clazz, "yy", "J");
    FT_MatrixFc.cached = 1;
}

FT_Matrix *getFT_MatrixFields(JNIEnv *env, jobject lpObject, FT_Matrix *lpStruct)
{
    if (!FT_MatrixFc.cached) cacheFT_MatrixFields(env, lpObject);
    lpStruct->xx = (FT_Fixed)(*env)->GetLongField(env, lpObject, FT_MatrixFc.xx);
    lpStruct->xy = (FT_Fixed)(*env)->GetLongField(env, lpObject, FT_MatrixFc.xy);
    lpStruct->yx = (FT_Fixed)(*env)->GetLongField(env, lpObject, FT_MatrixFc.yx);
    lpStruct->yy = (FT_Fixed)(*env)->GetLongField(env, lpObject, FT_MatrixFc.yy);
    return lpStruct;
}

typedef struct FT_Bitmap_FID_CACHE {
    int cached;
    jfieldID rows, width, pitch, buffer, num_grays, pixel_mode, palette_mode, palette;
} FT_Bitmap_FID_CACHE;

FT_Bitmap_FID_CACHE FT_BitmapFc;

void cacheFT_BitmapFields(JNIEnv *env, jobject lpObject)
{
    if (FT_BitmapFc.cached) return;
    jclass clazz = (*env)->GetObjectClass(env, lpObject);
    FT_BitmapFc.rows = (*env)->GetFieldID(env, clazz, "rows", "I");
    FT_BitmapFc.width = (*env)->GetFieldID(env, clazz, "width", "I");
    FT_BitmapFc.pitch = (*env)->GetFieldID(env, clazz, "pitch", "I");
    FT_BitmapFc.buffer = (*env)->GetFieldID(env, clazz, "buffer", "J");
    FT_BitmapFc.num_grays = (*env)->GetFieldID(env, clazz, "num_grays", "S");
    FT_BitmapFc.pixel_mode = (*env)->GetFieldID(env, clazz, "pixel_mode", "B");
    FT_BitmapFc.palette_mode = (*env)->GetFieldID(env, clazz, "palette_mode", "C");
    FT_BitmapFc.palette = (*env)->GetFieldID(env, clazz, "palette", "J");
    FT_BitmapFc.cached = 1;
}

void setFT_BitmapFields(JNIEnv *env, jobject lpObject, FT_Bitmap *lpStruct)
{
    if (!FT_BitmapFc.cached) cacheFT_BitmapFields(env, lpObject);
    (*env)->SetIntField(env, lpObject, FT_BitmapFc.rows, (jint)lpStruct->rows);
    (*env)->SetIntField(env, lpObject, FT_BitmapFc.width, (jint)lpStruct->width);
    (*env)->SetIntField(env, lpObject, FT_BitmapFc.pitch, (jint)lpStruct->pitch);
    (*env)->SetLongField(env, lpObject, FT_BitmapFc.buffer, (jlong)lpStruct->buffer);
    (*env)->SetShortField(env, lpObject, FT_BitmapFc.num_grays, (jshort)lpStruct->num_grays);
    (*env)->SetByteField(env, lpObject, FT_BitmapFc.pixel_mode, (jbyte)lpStruct->pixel_mode);
    (*env)->SetCharField(env, lpObject, FT_BitmapFc.palette_mode, (jchar)lpStruct->palette_mode);
    (*env)->SetLongField(env, lpObject, FT_BitmapFc.palette, (jlong)lpStruct->palette);
}

typedef struct FT_Glyph_Metrics_FID_CACHE {
    int cached;
    jfieldID width, height, horiBearingX, horiBearingY, horiAdvance, vertBearingX, vertBearingY, vertAdvance;
} FT_Glyph_Metrics_FID_CACHE;

FT_Glyph_Metrics_FID_CACHE FT_Glyph_MetricsFc;

void cacheFT_Glyph_MetricsFields(JNIEnv *env, jobject lpObject)
{
    if (FT_Glyph_MetricsFc.cached) return;
    jclass clazz = (*env)->GetObjectClass(env, lpObject);
    FT_Glyph_MetricsFc.width = (*env)->GetFieldID(env, clazz, "width", "J");
    FT_Glyph_MetricsFc.height = (*env)->GetFieldID(env, clazz, "height", "J");
    FT_Glyph_MetricsFc.horiBearingX = (*env)->GetFieldID(env, clazz, "horiBearingX", "J");
    FT_Glyph_MetricsFc.horiBearingY = (*env)->GetFieldID(env, clazz, "horiBearingY", "J");
    FT_Glyph_MetricsFc.horiAdvance = (*env)->GetFieldID(env, clazz, "horiAdvance", "J");
    FT_Glyph_MetricsFc.vertBearingX = (*env)->GetFieldID(env, clazz, "vertBearingX", "J");
    FT_Glyph_MetricsFc.vertBearingY = (*env)->GetFieldID(env, clazz, "vertBearingY", "J");
    FT_Glyph_MetricsFc.vertAdvance = (*env)->GetFieldID(env, clazz, "vertAdvance", "J");
    FT_Glyph_MetricsFc.cached = 1;
}

void setFT_Glyph_MetricsFields(JNIEnv *env, jobject lpObject, FT_Glyph_Metrics *lpStruct)
{
    if (!FT_Glyph_MetricsFc.cached) cacheFT_Glyph_MetricsFields(env, lpObject);
    (*env)->SetLongField(env, lpObject, FT_Glyph_MetricsFc.width, (jlong)lpStruct->width);
    (*env)->SetLongField(env, lpObject, FT_Glyph_MetricsFc.height, (jlong)lpStruct->height);
    (*env)->SetLongField(env, lpObject, FT_Glyph_MetricsFc.horiBearingX, (jlong)lpStruct->horiBearingX);
    (*env)->SetLongField(env, lpObject, FT_Glyph_MetricsFc.horiBearingY, (jlong)lpStruct->horiBearingY);
    (*env)->SetLongField(env, lpObject, FT_Glyph_MetricsFc.horiAdvance, (jlong)lpStruct->horiAdvance);
    (*env)->SetLongField(env, lpObject, FT_Glyph_MetricsFc.vertBearingX, (jlong)lpStruct->vertBearingX);
    (*env)->SetLongField(env, lpObject, FT_Glyph_MetricsFc.vertBearingY, (jlong)lpStruct->vertBearingY);
    (*env)->SetLongField(env, lpObject, FT_Glyph_MetricsFc.vertAdvance, (jlong)lpStruct->vertAdvance);
}

typedef struct FT_GlyphSlotRec_FID_CACHE {
    int cached;
    jclass clazz;
    jfieldID metrics, linearHoriAdvance, linearVertAdvance, advance_x, advance_y, format, bitmap, bitmap_left, bitmap_top;
    jmethodID init;
} FT_GlyphSlotRec_FID_CACHE;

FT_GlyphSlotRec_FID_CACHE FT_GlyphSlotRecFc;

void cacheFT_GlyphSlotRecFields(JNIEnv *env)
{
    if (FT_GlyphSlotRecFc.cached) return;
    jclass tmpClass = (*env)->FindClass(env, "com/sun/javafx/font/pango/FT_GlyphSlotRec");
    FT_GlyphSlotRecFc.clazz =  (jclass)(*env)->NewGlobalRef(env, tmpClass);
    FT_GlyphSlotRecFc.metrics = (*env)->GetFieldID(env, FT_GlyphSlotRecFc.clazz, "metrics", "Lcom/sun/javafx/font/pango/FT_Glyph_Metrics;");
    FT_GlyphSlotRecFc.linearHoriAdvance = (*env)->GetFieldID(env, FT_GlyphSlotRecFc.clazz, "linearHoriAdvance", "J");
    FT_GlyphSlotRecFc.linearVertAdvance = (*env)->GetFieldID(env, FT_GlyphSlotRecFc.clazz, "linearVertAdvance", "J");
    FT_GlyphSlotRecFc.advance_x = (*env)->GetFieldID(env, FT_GlyphSlotRecFc.clazz, "advance_x", "J");
    FT_GlyphSlotRecFc.advance_y = (*env)->GetFieldID(env, FT_GlyphSlotRecFc.clazz, "advance_y", "J");
    FT_GlyphSlotRecFc.format = (*env)->GetFieldID(env, FT_GlyphSlotRecFc.clazz, "format", "I");
    FT_GlyphSlotRecFc.bitmap = (*env)->GetFieldID(env, FT_GlyphSlotRecFc.clazz, "bitmap", "Lcom/sun/javafx/font/pango/FT_Bitmap;");
    FT_GlyphSlotRecFc.bitmap_left = (*env)->GetFieldID(env, FT_GlyphSlotRecFc.clazz, "bitmap_left", "I");
    FT_GlyphSlotRecFc.bitmap_top = (*env)->GetFieldID(env, FT_GlyphSlotRecFc.clazz, "bitmap_top", "I");
    FT_GlyphSlotRecFc.init = (*env)->GetMethodID(env, FT_GlyphSlotRecFc.clazz, "<init>", "()V");
    FT_GlyphSlotRecFc.cached = 1;
}

void setFT_GlyphSlotRecFields(JNIEnv *env, jobject lpObject, FT_GlyphSlotRec *lpStruct)
{
    if (!FT_GlyphSlotRecFc.cached) cacheFT_GlyphSlotRecFields(env);
    {
    jobject lpObject1 = (*env)->GetObjectField(env, lpObject, FT_GlyphSlotRecFc.metrics);
    if (lpObject1 != NULL) setFT_Glyph_MetricsFields(env, lpObject1, &lpStruct->metrics);
    }
    (*env)->SetLongField(env, lpObject, FT_GlyphSlotRecFc.linearHoriAdvance, (jlong)lpStruct->linearHoriAdvance);
    (*env)->SetLongField(env, lpObject, FT_GlyphSlotRecFc.linearVertAdvance, (jlong)lpStruct->linearVertAdvance);
    (*env)->SetLongField(env, lpObject, FT_GlyphSlotRecFc.advance_x, (jlong)lpStruct->advance.x);
    (*env)->SetLongField(env, lpObject, FT_GlyphSlotRecFc.advance_y, (jlong)lpStruct->advance.y);
    (*env)->SetIntField(env, lpObject, FT_GlyphSlotRecFc.format, (jint)lpStruct->format);
    {
    jobject lpObject1 = (*env)->GetObjectField(env, lpObject, FT_GlyphSlotRecFc.bitmap);
    if (lpObject1 != NULL) setFT_BitmapFields(env, lpObject1, &lpStruct->bitmap);
    }
    (*env)->SetIntField(env, lpObject, FT_GlyphSlotRecFc.bitmap_left, (jint)lpStruct->bitmap_left);
    (*env)->SetIntField(env, lpObject, FT_GlyphSlotRecFc.bitmap_top, (jint)lpStruct->bitmap_top);
}

jobject newFT_GlyphSlotRec(JNIEnv *env, FT_GlyphSlotRec *lpStruct)
{
    jobject lpObject = NULL;
    if (!FT_GlyphSlotRecFc.cached) cacheFT_GlyphSlotRecFields(env);
    lpObject = (*env)->NewObject(env, FT_GlyphSlotRecFc.clazz, FT_GlyphSlotRecFc.init);
    if (lpObject && lpStruct) setFT_GlyphSlotRecFields(env, lpObject, lpStruct);
    return lpObject;
}

/**************************************************************************/
/*                                                                        */
/*                           Functions                                    */
/*                                                                        */
/**************************************************************************/

JNIEXPORT jobject JNICALL OS_NATIVE(getGlyphSlot)(JNIEnv *env, jclass that, jlong facePtr)
{
    jobject result = NULL;
    if (facePtr) {
        FT_Face face = (FT_Face)facePtr;
        FT_GlyphSlot slot = face->glyph;
        if (slot) {
            result = newFT_GlyphSlotRec(env, slot);
        }
    }
    return result;
}

JNIEXPORT jbyteArray JNICALL OS_NATIVE(getBitmapData)(JNIEnv *env, jclass that, jlong facePtr)
{
    if (!facePtr) return NULL;
    FT_Face face = (FT_Face)facePtr;
    FT_GlyphSlot slot = face->glyph;
    if (!slot) return NULL;
    FT_Bitmap bitmap = slot->bitmap;
    unsigned char* src = bitmap.buffer;
    if (!src) return NULL;
    size_t size = bitmap.pitch * bitmap.rows;
    jbyteArray result = (*env)->NewByteArray(env, size);
    if (result) {
        unsigned char* dst = (*env)->GetPrimitiveArrayCritical(env, result, NULL);
        if (dst) {
            memcpy(dst, src, size);
            (*env)->ReleasePrimitiveArrayCritical(env, result, dst, 0);
        }
    }
    return result;
}

JNIEXPORT void JNICALL OS_NATIVE(FT_1Set_1Transform)
    (JNIEnv *env, jclass that, jlong arg0, jobject arg1, jlong arg2, jlong arg3)
{
    FT_Vector *lpDelta = NULL;
    if (arg2 || arg3) {
        FT_Vector vec = {arg2, arg3};
        lpDelta = &vec;
    }
    if (arg1) {
        FT_Matrix matrix, *lpMatrix = NULL;
        lpMatrix = getFT_MatrixFields(env, arg1, &matrix);
        if (lpMatrix) {
            FT_Set_Transform((FT_Face)arg0, lpMatrix, lpDelta);
        }
    }
}

#define LIB_FREETYPE "libfreetype.so"
JNIEXPORT jint JNICALL OS_NATIVE(FT_1Library_1SetLcdFilter)
    (JNIEnv *env, jclass that, jlong arg0, jint arg1)
{
//  return (jint)FT_Library_SetLcdFilter((FT_Library)arg0, (FT_LcdFilter)arg1);
    static void *fp = NULL;
    if (!fp) {
        void* handle = dlopen(LIB_FREETYPE, RTLD_LAZY);
        if (handle) fp = dlsym(handle, "FT_Library_SetLcdFilter");
    }
    jint rc = 0;
    if (fp) {
        rc = (jint)((jint (*)(jlong, jint))fp)(arg0, arg1);
    }
    return rc;
}

JNIEXPORT jint JNICALL OS_NATIVE(FT_1Done_1Face)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jint)FT_Done_Face((FT_Face)arg0);
}

JNIEXPORT void JNICALL OS_NATIVE(FT_1Library_1Version)
    (JNIEnv *env, jclass that, jlong arg0, jintArray arg1, jintArray arg2, jintArray arg3)
{
    jint *lparg1=NULL;
    jint *lparg2=NULL;
    jint *lparg3=NULL;
    if (arg1) if ((lparg1 = (*env)->GetIntArrayElements(env, arg1, NULL)) == NULL) goto fail;
    if (arg2) if ((lparg2 = (*env)->GetIntArrayElements(env, arg2, NULL)) == NULL) goto fail;
    if (arg3) if ((lparg3 = (*env)->GetIntArrayElements(env, arg3, NULL)) == NULL) goto fail;
    FT_Library_Version((FT_Library)arg0, lparg1, lparg2, lparg3);
fail:
    if (arg3 && lparg3) (*env)->ReleaseIntArrayElements(env, arg3, lparg3, 0);
    if (arg2 && lparg2) (*env)->ReleaseIntArrayElements(env, arg2, lparg2, 0);
    if (arg1 && lparg1) (*env)->ReleaseIntArrayElements(env, arg1, lparg1, 0);
}

JNIEXPORT jint JNICALL OS_NATIVE(FT_1Done_1FreeType)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jint)FT_Done_FreeType((FT_Library)arg0);
}

JNIEXPORT jint JNICALL OS_NATIVE(FT_1Init_1FreeType)
    (JNIEnv *env, jclass that, jlongArray arg0)
{
    jlong *lparg0=NULL;
    jint rc = 0;
    if (arg0) if ((lparg0 = (*env)->GetLongArrayElements(env, arg0, NULL)) == NULL) goto fail;
    rc = (jint)FT_Init_FreeType((FT_Library  *)lparg0);
fail:
    if (arg0 && lparg0) (*env)->ReleaseLongArrayElements(env, arg0, lparg0, 0);
    return rc;
}

JNIEXPORT jint JNICALL OS_NATIVE(FT_1Load_1Glyph)
    (JNIEnv *env, jclass that, jlong arg0, jint arg1, jint arg2)
{
    return (jint)FT_Load_Glyph((FT_Face)arg0, (FT_UInt)arg1, (FT_Int32)arg2);
}

JNIEXPORT jint JNICALL OS_NATIVE(FT_1New_1Face)
    (JNIEnv *env, jclass that, jlong arg0, jbyteArray arg1, jlong arg2, jlongArray arg3)
{
    jbyte *lparg1=NULL;
    jlong *lparg3=NULL;
    jint rc = 0;
    if (arg1) if ((lparg1 = (*env)->GetByteArrayElements(env, arg1, NULL)) == NULL) goto fail;
    if (arg3) if ((lparg3 = (*env)->GetLongArrayElements(env, arg3, NULL)) == NULL) goto fail;
    rc = (jint)FT_New_Face((FT_Library)arg0, (const char*)lparg1, (FT_Long)arg2, (FT_Face  *)lparg3);
fail:
    if (arg3 && lparg3) (*env)->ReleaseLongArrayElements(env, arg3, lparg3, 0);
    if (arg1 && lparg1) (*env)->ReleaseByteArrayElements(env, arg1, lparg1, 0);
    return rc;
}

JNIEXPORT jint JNICALL OS_NATIVE(FT_1Set_1Char_1Size)
    (JNIEnv *env, jclass that, jlong arg0, jlong arg1, jlong arg2, jint arg3, jint arg4)
{
    return (jint)FT_Set_Char_Size((FT_Face)arg0, (FT_F26Dot6)arg1, (FT_F26Dot6)arg2, (FT_UInt)arg3, (FT_UInt)arg4);
}

/***********************************************/
/*                Glyph Outline                */
/***********************************************/

#define F26DOT6TOFLOAT(n) (float)n/64.0;
static const int DEFAULT_LEN_TYPES = 10;
static const int DEFAULT_LEN_COORDS = 50;
typedef struct _PathData {
    jbyte* pointTypes;
    int numTypes;
    int lenTypes;
    jfloat* pointCoords;
    int numCoords;
    int lenCoords;
} PathData;

static PathData* checkSize(void* user, int coordCount)
{
    PathData* info = (PathData *)user;
    if (info->numTypes == info->lenTypes) {
        info->lenTypes += DEFAULT_LEN_TYPES;
        info->pointTypes = (jbyte*)realloc(info->pointTypes, info->lenTypes * sizeof(jbyte));
    }
    if (info->numCoords + (coordCount * 2) > info->lenCoords) {
        info->lenCoords += DEFAULT_LEN_COORDS;
        info->pointCoords = (jfloat*)realloc(info->pointCoords, info->lenCoords * sizeof(jfloat));
    }
    return info;
}

static int JFX_Outline_MoveToFunc(const FT_Vector*   to,
                                  void*              user)
{
    PathData *info = checkSize(user, 1);
    info->pointTypes[info->numTypes++] = 0;
    info->pointCoords[info->numCoords++] = F26DOT6TOFLOAT(to->x);
    info->pointCoords[info->numCoords++] = -F26DOT6TOFLOAT(to->y);
    return 0;
}

static int JFX_Outline_LineToFunc(const FT_Vector*   to,
                                  void* user)
{
    PathData *info =  checkSize(user, 1);
    info->pointTypes[info->numTypes++] = 1;
    info->pointCoords[info->numCoords++] = F26DOT6TOFLOAT(to->x);
    info->pointCoords[info->numCoords++] = -F26DOT6TOFLOAT(to->y);
    return 0;
}

static int JFX_Outline_ConicToFunc(const FT_Vector*  control,
                                   const FT_Vector*  to,
                                   void*             user )
{
    PathData *info = checkSize(user, 2);
    info->pointTypes[info->numTypes++] = 2;
    info->pointCoords[info->numCoords++] = F26DOT6TOFLOAT(control->x);
    info->pointCoords[info->numCoords++] = -F26DOT6TOFLOAT(control->y);
    info->pointCoords[info->numCoords++] = F26DOT6TOFLOAT(to->x);
    info->pointCoords[info->numCoords++] = -F26DOT6TOFLOAT(to->y);
    return 0;
}

static int JFX_Outline_CubicToFunc(const FT_Vector*  control1,
                                   const FT_Vector*  control2,
                                   const FT_Vector*  to,
                                   void*             user)
{
    PathData *info = checkSize(user, 3);
    info->pointTypes[info->numTypes++] = 3;
    info->pointCoords[info->numCoords++] = F26DOT6TOFLOAT(control1->x);
    info->pointCoords[info->numCoords++] = -F26DOT6TOFLOAT(control1->y);
    info->pointCoords[info->numCoords++] = F26DOT6TOFLOAT(control2->x);
    info->pointCoords[info->numCoords++] = -F26DOT6TOFLOAT(control2->y);
    info->pointCoords[info->numCoords++] = F26DOT6TOFLOAT(to->x);
    info->pointCoords[info->numCoords++] = -F26DOT6TOFLOAT(to->y);
    return 0;
}

static const  FT_Outline_Funcs JFX_Outline_Funcs =
{
    JFX_Outline_MoveToFunc,
    JFX_Outline_LineToFunc,
    JFX_Outline_ConicToFunc,
    JFX_Outline_CubicToFunc,
    0, 0
};

JNIEXPORT jobject JNICALL OS_NATIVE(FT_1Outline_1Decompose)
    (JNIEnv *env, jclass that, jlong arg0)
{
    FT_Face face = (FT_Face)arg0;
    if (face == NULL) return NULL;
    FT_GlyphSlot slot = face->glyph;
    if (slot == NULL) return NULL;
    FT_Outline* outline = &slot->outline;
    if (outline == NULL) return NULL;

    jobject path2D = NULL;
    PathData data;
    data.pointTypes = (jbyte*)malloc(sizeof(jbyte) * DEFAULT_LEN_TYPES);
    data.numTypes = 0;
    data.lenTypes = DEFAULT_LEN_TYPES;
    data.pointCoords = (jfloat*)malloc(sizeof(jfloat) * DEFAULT_LEN_COORDS);
    data.numCoords = 0;
    data.lenCoords = DEFAULT_LEN_COORDS;

    /* Decompose outline */
    FT_Outline_Decompose(outline, &JFX_Outline_Funcs, &data);

    static jclass path2DClass = NULL;
    static jmethodID path2DCtr = NULL;
    if (path2DClass == NULL) {
        jclass tmpClass = (*env)->FindClass(env, "com/sun/javafx/geom/Path2D");
        path2DClass = (jclass)(*env)->NewGlobalRef(env, tmpClass);
        path2DCtr = (*env)->GetMethodID(env, path2DClass, "<init>", "(I[BI[FI)V");
    }

    jbyteArray types = (*env)->NewByteArray(env, data.numTypes);
    jfloatArray coords = (*env)->NewFloatArray(env, data.numCoords);
    if (types && coords) {
        (*env)->SetByteArrayRegion(env, types, 0, data.numTypes, data.pointTypes);
        (*env)->SetFloatArrayRegion(env, coords, 0, data.numCoords, data.pointCoords);
        path2D = (*env)->NewObject(env, path2DClass, path2DCtr,
                                   0 /*winding rule*/,
                                   types, data.numTypes,
                                   coords, data.numCoords);
    }
    free(data.pointTypes);
    free(data.pointCoords);
    return path2D;
}

#endif /* ENABLE_PANGO */
#endif /* __linux__ */
