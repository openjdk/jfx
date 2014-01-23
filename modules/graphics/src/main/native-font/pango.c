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
#include <com_sun_javafx_font_freetype_OSPango.h>
#include <pango/pango.h>
#include <pango/pangoft2.h>
#include <dlfcn.h>

#define OS_NATIVE(func) Java_com_sun_javafx_font_freetype_OSPango_##func

/**************************************************************************/
/*                                                                        */
/*                            Structs                                     */
/*                                                                        */
/**************************************************************************/

typedef struct PangoGlyphInfo_FID_CACHE {
    int cached;
    jclass clazz;
    jfieldID glyph, width, x_offset, y_offset;
    jmethodID init;
} PangoGlyphInfo_FID_CACHE;

PangoGlyphInfo_FID_CACHE PangoGlyphInfoFc;

void cachePangoGlyphInfoFields(JNIEnv *env)
{
    if (PangoGlyphInfoFc.cached) return;
    jclass tmpClass = (*env)->FindClass(env, "com/sun/javafx/font/freetype/PangoGlyphInfo");
    PangoGlyphInfoFc.clazz =  (jclass)(*env)->NewGlobalRef(env, tmpClass);
    PangoGlyphInfoFc.glyph = (*env)->GetFieldID(env, PangoGlyphInfoFc.clazz, "glyph", "I");
    PangoGlyphInfoFc.width = (*env)->GetFieldID(env, PangoGlyphInfoFc.clazz, "width", "I");
    PangoGlyphInfoFc.x_offset = (*env)->GetFieldID(env, PangoGlyphInfoFc.clazz, "x_offset", "I");
    PangoGlyphInfoFc.y_offset = (*env)->GetFieldID(env, PangoGlyphInfoFc.clazz, "y_offset", "I");
    PangoGlyphInfoFc.init = (*env)->GetMethodID(env, PangoGlyphInfoFc.clazz, "<init>", "()V");
    PangoGlyphInfoFc.cached = 1;
}

void setPangoGlyphInfoFields(JNIEnv *env, jobject lpObject, PangoGlyphInfo *lpStruct)
{
    if (!PangoGlyphInfoFc.cached) cachePangoGlyphInfoFields(env);
    (*env)->SetIntField(env, lpObject, PangoGlyphInfoFc.glyph, (jint)lpStruct->glyph);
    (*env)->SetIntField(env, lpObject, PangoGlyphInfoFc.width, (jint)lpStruct->geometry.width);
    (*env)->SetIntField(env, lpObject, PangoGlyphInfoFc.x_offset, (jint)lpStruct->geometry.x_offset);
    (*env)->SetIntField(env, lpObject, PangoGlyphInfoFc.y_offset, (jint)lpStruct->geometry.y_offset);
}

jobject newPangoGlyphInfo(JNIEnv *env, PangoGlyphInfo *lpStruct)
{
    jobject lpObject = NULL;
    if (!PangoGlyphInfoFc.cached) cachePangoGlyphInfoFields(env);
    lpObject = (*env)->NewObject(env, PangoGlyphInfoFc.clazz, PangoGlyphInfoFc.init);
    if (lpObject && lpStruct) setPangoGlyphInfoFields(env, lpObject, lpStruct);
    return lpObject;
}

typedef struct PangoGlyphString_FID_CACHE {
    int cached;
    jclass clazz;
    jfieldID num_glyphs, glyphs, log_clusters, offset, length, num_chars, font;
    jmethodID init;
} PangoGlyphString_FID_CACHE;

PangoGlyphString_FID_CACHE PangoGlyphStringFc;

void cachePangoGlyphStringFields(JNIEnv *env)
{
    if (PangoGlyphStringFc.cached) return;
    jclass tmpClass = (*env)->FindClass(env, "com/sun/javafx/font/freetype/PangoGlyphString");
    PangoGlyphStringFc.clazz =  (jclass)(*env)->NewGlobalRef(env, tmpClass);
    PangoGlyphStringFc.num_glyphs = (*env)->GetFieldID(env, PangoGlyphStringFc.clazz, "num_glyphs", "I");
    PangoGlyphStringFc.glyphs = (*env)->GetFieldID(env, PangoGlyphStringFc.clazz, "glyphs", "[Lcom/sun/javafx/font/freetype/PangoGlyphInfo;");
    PangoGlyphStringFc.log_clusters = (*env)->GetFieldID(env, PangoGlyphStringFc.clazz, "log_clusters", "[I");
    PangoGlyphStringFc.offset = (*env)->GetFieldID(env, PangoGlyphStringFc.clazz, "offset", "I");
    PangoGlyphStringFc.length = (*env)->GetFieldID(env, PangoGlyphStringFc.clazz, "length", "I");
    PangoGlyphStringFc.num_chars = (*env)->GetFieldID(env, PangoGlyphStringFc.clazz, "num_chars", "I");
    PangoGlyphStringFc.font = (*env)->GetFieldID(env, PangoGlyphStringFc.clazz, "font", "J");
    PangoGlyphStringFc.init = (*env)->GetMethodID(env, PangoGlyphStringFc.clazz, "<init>", "()V");
    PangoGlyphStringFc.cached = 1;
}

/**************************************************************************/
/*                                                                        */
/*                           Functions                                    */
/*                                                                        */
/**************************************************************************/

/** Custom **/
JNIEXPORT jlong JNICALL OS_NATIVE(pango_1itemize)
    (JNIEnv *env, jclass that, jlong arg0, jlong arg1, jint arg2, jint arg3, jlong arg4, jlong arg5)
{
    return (jlong)pango_itemize((PangoContext *)arg0, (const char *)arg1, arg2, arg3, (PangoAttrList *)arg4, (PangoAttrIterator *)arg5);
}

JNIEXPORT jobject JNICALL OS_NATIVE(pango_1shape)
    (JNIEnv *env, jclass that, jlong str, jlong pangoItem)
{
    if (!pangoItem) return NULL;
    PangoItem *item = (PangoItem *)pangoItem;
    PangoAnalysis analysis = item->analysis;
    const gchar *text= (const gchar *)(str + item->offset);
    PangoGlyphString *glyphs = pango_glyph_string_new();

    pango_shape(text, item->length, &analysis, glyphs);
    int count = glyphs->num_glyphs;
    if(count == 0) return NULL;

    if (!PangoGlyphInfoFc.cached) cachePangoGlyphInfoFields(env);
    jobjectArray infoArray = (*env)->NewObjectArray(env, count, PangoGlyphInfoFc.clazz, NULL);
    jintArray clusterArray = (*env)->NewIntArray(env, count);
    jobject result = NULL;
    if (infoArray && clusterArray) {
        jint cluster[count];
        int i;
        for (i = 0; i < count; i++) {
            jobject info = newPangoGlyphInfo(env, &glyphs->glyphs[i]);
            (*env)->SetObjectArrayElement(env, infoArray, i, info);
            /* translate byte index to char index */
            cluster[i] = (jint)g_utf8_pointer_to_offset(text, text + glyphs->log_clusters[i]);
        }
        (*env)->SetIntArrayRegion(env, clusterArray, 0, count, cluster);
        if (!PangoGlyphStringFc.cached) cachePangoGlyphStringFields(env);
        result = (*env)->NewObject(env, PangoGlyphStringFc.clazz, PangoGlyphStringFc.init);
        if (result) {
            (*env)->SetIntField(env, result, PangoGlyphStringFc.num_glyphs, count);

            /* Set custom fields */
            (*env)->SetObjectField(env, result, PangoGlyphStringFc.glyphs, infoArray);
            (*env)->SetObjectField(env, result, PangoGlyphStringFc.log_clusters, clusterArray);
            (*env)->SetIntField(env, result, PangoGlyphStringFc.offset, item->offset);
            (*env)->SetIntField(env, result, PangoGlyphStringFc.length, item->length);
            (*env)->SetIntField(env, result, PangoGlyphStringFc.num_chars, item->num_chars);
            (*env)->SetLongField(env, result, PangoGlyphStringFc.font, (jlong)analysis.font);
        }
    }
    pango_glyph_string_free(glyphs);
    return result;
}

JNIEXPORT jstring JNICALL OS_NATIVE(pango_1font_1description_1get_1family)
    (JNIEnv *env, jclass that, jlong arg0)
{
    const char *family = pango_font_description_get_family((PangoFontDescription *)arg0);
    return (*env)->NewStringUTF(env, family);
}

JNIEXPORT void JNICALL OS_NATIVE(pango_1font_1description_1set_1family)
    (JNIEnv *env, jclass that, jlong arg0, jstring arg1)
{
    if (arg1) {
        const char *text = (*env)->GetStringUTFChars(env, arg1, NULL);
        if (text) {
            pango_font_description_set_family((PangoFontDescription *)arg0, text);
            (*env)->ReleaseStringUTFChars(env, arg1, text);
        }
    }
}

#define LIB_FONTCONFIG "libfontconfig.so.1"
JNIEXPORT jboolean JNICALL OS_NATIVE(FcConfigAppFontAddFile)
    (JNIEnv *env, jclass that, jlong arg0, jstring arg1)
{
    static void *fp = NULL;
    if (!fp) {
        void* handle = dlopen(LIB_FONTCONFIG, RTLD_LAZY);
        if (handle) fp = dlsym(handle, "FcConfigAppFontAddFile");
    }
    jboolean rc = 0;
    if (arg1) {
        const char *text = (*env)->GetStringUTFChars(env, arg1, NULL);
        if (text) {
//            rc = (jboolean)FcConfigAppFontAddFile(arg0, text);
            if (fp) {
                rc = (jboolean)((jboolean (*)(jlong, const char *))fp)(arg0, text);
            }
            (*env)->ReleaseStringUTFChars(env, arg1, text);
        }
    }
    return rc;
}

/** one to one **/
JNIEXPORT jlong JNICALL OS_NATIVE(pango_1font_1describe)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jlong)pango_font_describe((PangoFont *)arg0);
}

JNIEXPORT jlong JNICALL OS_NATIVE(pango_1attr_1fallback_1new)
    (JNIEnv *env, jclass that, jboolean arg0)
{
    return (jlong)pango_attr_fallback_new(arg0);
}

JNIEXPORT jint JNICALL OS_NATIVE(pango_1font_1description_1get_1stretch)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jint)pango_font_description_get_stretch((PangoFontDescription *)arg0);
}

JNIEXPORT jint JNICALL OS_NATIVE(pango_1font_1description_1get_1style)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jint)pango_font_description_get_style((PangoFontDescription *)arg0);
}

JNIEXPORT jint JNICALL OS_NATIVE(pango_1font_1description_1get_1weight)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jint)pango_font_description_get_weight((PangoFontDescription *)arg0);
}

JNIEXPORT jlong JNICALL OS_NATIVE(pango_1ft2_1font_1map_1new)
    (JNIEnv *env, jclass that)
{
    return (jlong)pango_ft2_font_map_new();
}

JNIEXPORT jlong JNICALL OS_NATIVE(pango_1font_1map_1create_1context)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jlong)pango_font_map_create_context((PangoFontMap *)arg0);
}

JNIEXPORT void JNICALL OS_NATIVE(g_1object_1unref)
    (JNIEnv *env, jclass that, jlong arg0)
{
    g_object_unref((gpointer)arg0);
}

JNIEXPORT jlong JNICALL OS_NATIVE(pango_1font_1description_1new)
    (JNIEnv *env, jclass that)
{
    return (jlong)pango_font_description_new();
}

JNIEXPORT void JNICALL OS_NATIVE(pango_1font_1description_1set_1absolute_1size)
    (JNIEnv *env, jclass that, jlong arg0, jdouble arg1)
{
    pango_font_description_set_absolute_size((PangoFontDescription *)arg0, arg1);
}

JNIEXPORT void JNICALL OS_NATIVE(pango_1font_1description_1set_1stretch)
    (JNIEnv *env, jclass that, jlong arg0, jint arg1)
{
    pango_font_description_set_stretch((PangoFontDescription *)arg0, (PangoStretch)arg1);
}

JNIEXPORT void JNICALL OS_NATIVE(pango_1font_1description_1set_1style)
    (JNIEnv *env, jclass that, jlong arg0, jint arg1)
{
    pango_font_description_set_style((PangoFontDescription *)arg0, (PangoStyle)arg1);
}

JNIEXPORT void JNICALL OS_NATIVE(pango_1font_1description_1set_1weight)
    (JNIEnv *env, jclass that, jlong arg0, jint arg1)
{
    pango_font_description_set_weight((PangoFontDescription *)arg0, (PangoWeight)arg1);
}

JNIEXPORT jlong JNICALL OS_NATIVE(pango_1attr_1list_1new)
    (JNIEnv *env, jclass that)
{
    return (jlong)pango_attr_list_new();
}

JNIEXPORT jlong JNICALL OS_NATIVE(pango_1attr_1font_1desc_1new)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jlong)pango_attr_font_desc_new((const PangoFontDescription *)arg0);
}

JNIEXPORT void JNICALL OS_NATIVE(pango_1attr_1list_1insert)
    (JNIEnv *env, jclass that, jlong arg0, jlong arg1)
{
    pango_attr_list_insert((PangoAttrList *)arg0, (PangoAttribute *)arg1);
}

JNIEXPORT jint JNICALL OS_NATIVE(g_1list_1length)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jint)g_list_length((GList *)arg0);
}

JNIEXPORT jlong JNICALL OS_NATIVE(g_1list_1nth_1data)
    (JNIEnv *env, jclass that, jlong arg0, jint arg1)
{
    return (jlong)g_list_nth_data((GList *)arg0, (guint)arg1);
}

JNIEXPORT void JNICALL OS_NATIVE(pango_1item_1free)
    (JNIEnv *env, jclass that, jlong arg0)
{
    pango_item_free((PangoItem *)arg0);
}

JNIEXPORT void JNICALL OS_NATIVE(g_1list_1free)
    (JNIEnv *env, jclass that, jlong arg0)
{
    g_list_free((GList *)arg0);
}

JNIEXPORT jlong JNICALL OS_NATIVE(g_1utf8_1offset_1to_1pointer)
    (JNIEnv *env, jclass that, jlong arg0, jlong arg1)
{
    return (jlong)g_utf8_offset_to_pointer((const gchar *)arg0, (glong)arg1);
}

JNIEXPORT jlong JNICALL OS_NATIVE(g_1utf8_1pointer_1to_1offset)
    (JNIEnv *env, jclass that, jlong arg0, jlong arg1)
{
    return (jlong)g_utf8_pointer_to_offset((const gchar *)arg0, (const gchar *)arg1);
}

JNIEXPORT jlong JNICALL OS_NATIVE(g_1utf16_1to_1utf8)
    (JNIEnv *env, jclass that, jcharArray arg0)
{
    jsize length = (*env)->GetArrayLength(env, arg0);
    void *ch = (*env)->GetPrimitiveArrayCritical(env, arg0, 0);
    jlong str = (jlong)g_utf16_to_utf8((const gunichar2 *)ch, length, NULL, NULL, NULL);
    (*env)->ReleasePrimitiveArrayCritical(env, arg0, ch, 0);
    return str;
}

JNIEXPORT void JNICALL OS_NATIVE(g_1free)
    (JNIEnv *env, jclass that, jlong arg0)
{
    g_free((gpointer)arg0);
}

JNIEXPORT void JNICALL OS_NATIVE(pango_1attr_1list_1unref)
    (JNIEnv *env, jclass that, jlong arg0)
{
    pango_attr_list_unref((PangoAttrList *)arg0);
}

JNIEXPORT void JNICALL OS_NATIVE(pango_1font_1description_1free)
    (JNIEnv *env, jclass that, jlong arg0)
{
    pango_font_description_free((PangoFontDescription *)arg0);
}

#endif /* ENABLE_PANGO */
#endif /* __linux__ */
