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

#ifdef __APPLE__
#include <TargetConditionals.h>

#if TARGET_OS_MAC

#include <jni.h>
#include <com_sun_javafx_font_coretext_OS.h>

#import <CoreFoundation/CoreFoundation.h>

#if TARGET_OS_IPHONE
#import <CoreGraphics/CoreGraphics.h>
#import <CoreText/CoreText.h>
#else
#import <ApplicationServices/ApplicationServices.h>
#endif


#define OS_NATIVE(func) Java_com_sun_javafx_font_coretext_OS_##func

extern jboolean checkAndClearException(JNIEnv *env);

jboolean checkAndClearException(JNIEnv *env)
{
    jthrowable t = (*env)->ExceptionOccurred(env);
    if (!t) {
        return JNI_FALSE;
    }
    (*env)->ExceptionClear(env);
    return JNI_TRUE;
}

/**************************************************************************/
/*                                                                        */
/*                            Structs                                     */
/*                                                                        */
/**************************************************************************/
typedef struct CGAffineTransform_FID_CACHE {
    int cached;
    jclass clazz;
    jfieldID a, b, c, d, tx, ty;
    jmethodID init;
} CGAffineTransform_FID_CACHE;

CGAffineTransform_FID_CACHE CGAffineTransformFc;

void cacheCGAffineTransformFields(JNIEnv *env)
{
    if (CGAffineTransformFc.cached) return;
    jclass tmpClass = (*env)->FindClass(env, "com/sun/javafx/font/coretext/CGAffineTransform");
    if (checkAndClearException(env) || !tmpClass) {
        fprintf(stderr, "cacheCGAffineTransformFields error: JNI exception or tmpClass == NULL");
        return;
    }
    CGAffineTransformFc.clazz =  (jclass)(*env)->NewGlobalRef(env, tmpClass);
    CGAffineTransformFc.a = (*env)->GetFieldID(env, CGAffineTransformFc.clazz, "a", "D");
    if (checkAndClearException(env) || !CGAffineTransformFc.a) {
        fprintf(stderr, "cacheCGAffineTransformFields error: JNI exception or a == NULL");
        return;
    }
    CGAffineTransformFc.b = (*env)->GetFieldID(env, CGAffineTransformFc.clazz, "b", "D");
    if (checkAndClearException(env) || !CGAffineTransformFc.b) {
        fprintf(stderr, "cacheCGAffineTransformFields error: JNI exception or b == NULL");
        return;
    }
    CGAffineTransformFc.c = (*env)->GetFieldID(env, CGAffineTransformFc.clazz, "c", "D");
    if (checkAndClearException(env) || !CGAffineTransformFc.c) {
        fprintf(stderr, "cacheCGAffineTransformFields error: JNI exception or c == NULL");
        return;
    }
    CGAffineTransformFc.d = (*env)->GetFieldID(env, CGAffineTransformFc.clazz, "d", "D");
    if (checkAndClearException(env) || !CGAffineTransformFc.d) {
        fprintf(stderr, "cacheCGAffineTransformFields error: JNI exception or d == NULL");
        return;
    }
    CGAffineTransformFc.tx = (*env)->GetFieldID(env, CGAffineTransformFc.clazz, "tx", "D");
    if (checkAndClearException(env) || !CGAffineTransformFc.tx) {
        fprintf(stderr, "cacheCGAffineTransformFields error: JNI exception or tx == NULL");
        return;
    }
    CGAffineTransformFc.ty = (*env)->GetFieldID(env, CGAffineTransformFc.clazz, "ty", "D");
    if (checkAndClearException(env) || !CGAffineTransformFc.ty) {
        fprintf(stderr, "cacheCGAffineTransformFields error: JNI exception or ty == NULL");
        return;
    }
    CGAffineTransformFc.init = (*env)->GetMethodID(env, CGAffineTransformFc.clazz, "<init>", "()V");
    if (checkAndClearException(env) || !CGAffineTransformFc.init) {
        fprintf(stderr, "cacheCGAffineTransformFields error: JNI exception or init == NULL");
        return;
    }
    CGAffineTransformFc.cached = 1;
}

CGAffineTransform *getCGAffineTransformFields(JNIEnv *env, jobject lpObject, CGAffineTransform *lpStruct)
{
    if (!CGAffineTransformFc.cached) cacheCGAffineTransformFields(env);
    lpStruct->a = (*env)->GetDoubleField(env, lpObject, CGAffineTransformFc.a);
    lpStruct->b = (*env)->GetDoubleField(env, lpObject, CGAffineTransformFc.b);
    lpStruct->c = (*env)->GetDoubleField(env, lpObject, CGAffineTransformFc.c);
    lpStruct->d = (*env)->GetDoubleField(env, lpObject, CGAffineTransformFc.d);
    lpStruct->tx = (*env)->GetDoubleField(env, lpObject, CGAffineTransformFc.tx);
    lpStruct->ty = (*env)->GetDoubleField(env, lpObject, CGAffineTransformFc.ty);
    return lpStruct;
}

void setCGAffineTransformFields(JNIEnv *env, jobject lpObject, CGAffineTransform *lpStruct)
{
    if (!CGAffineTransformFc.cached) cacheCGAffineTransformFields(env);
    (*env)->SetDoubleField(env, lpObject, CGAffineTransformFc.a, (jdouble)lpStruct->a);
    (*env)->SetDoubleField(env, lpObject, CGAffineTransformFc.b, (jdouble)lpStruct->b);
    (*env)->SetDoubleField(env, lpObject, CGAffineTransformFc.c, (jdouble)lpStruct->c);
    (*env)->SetDoubleField(env, lpObject, CGAffineTransformFc.d, (jdouble)lpStruct->d);
    (*env)->SetDoubleField(env, lpObject, CGAffineTransformFc.tx, (jdouble)lpStruct->tx);
    (*env)->SetDoubleField(env, lpObject, CGAffineTransformFc.ty, (jdouble)lpStruct->ty);
}

jobject newCGAffineTransform(JNIEnv *env, CGAffineTransform *lpStruct)
{
    jobject lpObject = NULL;
    if (!CGAffineTransformFc.cached) cacheCGAffineTransformFields(env);
    lpObject = (*env)->NewObject(env, CGAffineTransformFc.clazz, CGAffineTransformFc.init);
    if (lpObject && lpStruct) setCGAffineTransformFields(env, lpObject, lpStruct);
    return lpObject;
}

typedef struct CGPoint_FID_CACHE {
    int cached;
    jclass clazz;
    jfieldID x, y;
    jmethodID init;
} CGPoint_FID_CACHE;

CGPoint_FID_CACHE CGPointFc;

void cacheCGPointFields(JNIEnv *env)
{
    if (CGPointFc.cached) return;
    jclass tmpClass = (*env)->FindClass(env, "com/sun/javafx/font/coretext/CGPoint");
    if (checkAndClearException(env) || !tmpClass) {
        fprintf(stderr, "cacheCGPointFields error: JNI exception or tmpClass == NULL");
        return;
    }
    CGPointFc.clazz =  (jclass)(*env)->NewGlobalRef(env, tmpClass);
    CGPointFc.x = (*env)->GetFieldID(env, CGPointFc.clazz, "x", "D");
    if (checkAndClearException(env) || !CGPointFc.x) {
        fprintf(stderr, "cacheCGPointFields error: JNI exception or x == NULL");
        return;
    }
    CGPointFc.y = (*env)->GetFieldID(env, CGPointFc.clazz, "y", "D");
    if (checkAndClearException(env) || !CGPointFc.y) {
        fprintf(stderr, "cacheCGPointFields error: JNI exception or y == NULL");
        return;
    }
    CGPointFc.init = (*env)->GetMethodID(env, CGPointFc.clazz, "<init>", "()V");
    if (checkAndClearException(env) || !CGPointFc.init) {
        fprintf(stderr, "cacheCGPointFields error: JNI exception or init == NULL");
        return;
    }
    CGPointFc.cached = 1;
}

CGPoint *getCGPointFields(JNIEnv *env, jobject lpObject, CGPoint *lpStruct)
{
    if (!CGPointFc.cached) cacheCGPointFields(env);
    lpStruct->x = (*env)->GetDoubleField(env, lpObject, CGPointFc.x);
    lpStruct->y = (*env)->GetDoubleField(env, lpObject, CGPointFc.y);
    return lpStruct;
}

void setCGPointFields(JNIEnv *env, jobject lpObject, CGPoint *lpStruct)
{
    if (!CGPointFc.cached) cacheCGPointFields(env);
    (*env)->SetDoubleField(env, lpObject, CGPointFc.x, (jdouble)lpStruct->x);
    (*env)->SetDoubleField(env, lpObject, CGPointFc.y, (jdouble)lpStruct->y);
}

jobject newCGPoint(JNIEnv *env, CGPoint *lpStruct)
{
    jobject lpObject = NULL;
    if (!CGPointFc.cached) cacheCGPointFields(env);
    lpObject = (*env)->NewObject(env, CGPointFc.clazz, CGPointFc.init);
    if (lpObject && lpStruct) setCGPointFields(env, lpObject, lpStruct);
    return lpObject;
}

typedef struct CGSize_FID_CACHE {
    int cached;
    jclass clazz;
    jfieldID width, height;
    jmethodID init;
} CGSize_FID_CACHE;

CGSize_FID_CACHE CGSizeFc;

void cacheCGSizeFields(JNIEnv *env)
{
    if (CGSizeFc.cached) return;
    jclass tmpClass = (*env)->FindClass(env, "com/sun/javafx/font/coretext/CGSize");
    if (checkAndClearException(env) || !tmpClass) {
        fprintf(stderr, "cacheCGSizeFields error: JNI exception or tmpClass == NULL");
        return;
    }
    CGSizeFc.clazz =  (jclass)(*env)->NewGlobalRef(env, tmpClass);
    CGSizeFc.width = (*env)->GetFieldID(env, CGSizeFc.clazz, "width", "D");
    if (checkAndClearException(env) || !CGSizeFc.width) {
        fprintf(stderr, "cacheCGSizeFields error: JNI exception or width == NULL");
        return;
    }
    CGSizeFc.height = (*env)->GetFieldID(env, CGSizeFc.clazz, "height", "D");
    if (checkAndClearException(env) || !CGSizeFc.height) {
        fprintf(stderr, "cacheCGSizeFields error: JNI exception or height == NULL");
        return;
    }
    CGSizeFc.init = (*env)->GetMethodID(env, CGSizeFc.clazz, "<init>", "()V");
    if (checkAndClearException(env) || !CGSizeFc.init) {
        fprintf(stderr, "cacheCGSizeFields error: JNI exception or init == NULL");
        return;
    }
    CGSizeFc.cached = 1;
}

CGSize *getCGSizeFields(JNIEnv *env, jobject lpObject, CGSize *lpStruct)
{
    if (!CGSizeFc.cached) cacheCGSizeFields(env);
    lpStruct->width = (*env)->GetDoubleField(env, lpObject, CGSizeFc.width);
    lpStruct->height = (*env)->GetDoubleField(env, lpObject, CGSizeFc.height);
    return lpStruct;
}

void setCGSizeFields(JNIEnv *env, jobject lpObject, CGSize *lpStruct)
{
    if (!CGSizeFc.cached) cacheCGSizeFields(env);
    (*env)->SetDoubleField(env, lpObject, CGSizeFc.width, (jdouble)lpStruct->width);
    (*env)->SetDoubleField(env, lpObject, CGSizeFc.height, (jdouble)lpStruct->height);
}

jobject newCGSize(JNIEnv *env, CGSize *lpStruct)
{
    jobject lpObject = NULL;
    if (!CGSizeFc.cached) cacheCGSizeFields(env);
    lpObject = (*env)->NewObject(env, CGSizeFc.clazz, CGSizeFc.init);
    if (lpObject && lpStruct) setCGSizeFields(env, lpObject, lpStruct);
    return lpObject;
}

typedef struct CGRect_FID_CACHE {
    int cached;
    jclass clazz;
    jfieldID origin, size;
    jmethodID init;
} CGRect_FID_CACHE;

CGRect_FID_CACHE CGRectFc;

void cacheCGRectFields(JNIEnv *env)
{
    if (CGRectFc.cached) return;
    jclass tmpClass = (*env)->FindClass(env, "com/sun/javafx/font/coretext/CGRect");
    if (checkAndClearException(env) || !tmpClass) {
        fprintf(stderr, "cacheCGRectFields error: JNI exception or tmpClass == NULL");
        return;
    }
    CGRectFc.clazz =  (jclass)(*env)->NewGlobalRef(env, tmpClass);
    CGRectFc.origin = (*env)->GetFieldID(env, CGRectFc.clazz, "origin", "Lcom/sun/javafx/font/coretext/CGPoint;");
    if (checkAndClearException(env) || !CGRectFc.origin) {
        fprintf(stderr, "cacheCGRectFields error: JNI exception or origin == NULL");
        return;
    }
    CGRectFc.size = (*env)->GetFieldID(env, CGRectFc.clazz, "size", "Lcom/sun/javafx/font/coretext/CGSize;");
    if (checkAndClearException(env) || !CGRectFc.size) {
        fprintf(stderr, "cacheCGRectFields error: JNI exception or size == NULL");
        return;
    }
    CGRectFc.init = (*env)->GetMethodID(env, CGRectFc.clazz, "<init>", "()V");
    if (checkAndClearException(env) || !CGRectFc.init) {
        fprintf(stderr, "cacheCGRectFields error: JNI exception or init == NULL");
        return;
    }
    CGRectFc.cached = 1;
}

CGRect *getCGRectFields(JNIEnv *env, jobject lpObject, CGRect *lpStruct)
{
    if (!CGRectFc.cached) cacheCGRectFields(env);
    {
    jobject lpObject1 = (*env)->GetObjectField(env, lpObject, CGRectFc.origin);
    if (lpObject1 != NULL) getCGPointFields(env, lpObject1, &lpStruct->origin);
    }
    {
    jobject lpObject1 = (*env)->GetObjectField(env, lpObject, CGRectFc.size);
    if (lpObject1 != NULL) getCGSizeFields(env, lpObject1, &lpStruct->size);
    }
    return lpStruct;
}

void setCGRectFields(JNIEnv *env, jobject lpObject, CGRect *lpStruct)
{
    if (!CGRectFc.cached) cacheCGRectFields(env);
    {
    jobject lpObject1 = (*env)->GetObjectField(env, lpObject, CGRectFc.origin);
    if (lpObject1 != NULL) setCGPointFields(env, lpObject1, &lpStruct->origin);
    }
    {
    jobject lpObject1 = (*env)->GetObjectField(env, lpObject, CGRectFc.size);
    if (lpObject1 != NULL) setCGSizeFields(env, lpObject1, &lpStruct->size);
    }
}

jobject newCGRect(JNIEnv *env, CGRect *lpStruct)
{
    jobject lpObject = NULL;
    if (!CGRectFc.cached) cacheCGRectFields(env);
    lpObject = (*env)->NewObject(env, CGRectFc.clazz, CGRectFc.init);
    if (lpObject && lpStruct) setCGRectFields(env, lpObject, lpStruct);
    return lpObject;
}

/**************************************************************************/
/*                                                                        */
/*                            Functions                                   */
/*                                                                        */
/**************************************************************************/

JNIEXPORT jlong JNICALL OS_NATIVE(kCFAllocatorDefault)
    (JNIEnv *env, jclass that)
{
    return (jlong)kCFAllocatorDefault;
}

JNIEXPORT jlong JNICALL OS_NATIVE(CFStringCreateWithCharacters__J_3CJ)
    (JNIEnv *env, jclass that, jlong arg0, jcharArray arg1, jlong arg2)
{
    jchar *lparg1=NULL;
    jlong rc = 0;
    if (arg1) if ((lparg1 = (*env)->GetCharArrayElements(env, arg1, NULL)) == NULL) goto fail;
    rc = (jlong)CFStringCreateWithCharacters((CFAllocatorRef)arg0, (UniChar*)lparg1, (CFIndex)arg2);
fail:
    if (arg1 && lparg1) (*env)->ReleaseCharArrayElements(env, arg1, lparg1, 0);
    return rc;
}

JNIEXPORT jlong JNICALL OS_NATIVE(CTFontCreateWithName)
    (JNIEnv *env, jclass that, jlong arg0, jdouble arg1, jobject arg2)
{
    CGAffineTransform _arg2, *lparg2=NULL;
    jlong rc = 0;
    if (arg2) if ((lparg2 = getCGAffineTransformFields(env, arg2, &_arg2)) == NULL) goto fail;
    rc = (jlong)CTFontCreateWithName((CFStringRef)arg0, (CGFloat)arg1, (CGAffineTransform*)lparg2);
fail:
    /* In only */
//    if (arg2 && lparg2) setCGAffineTransformFields(env, arg2, lparg2);
    return rc;
}

JNIEXPORT void JNICALL OS_NATIVE(CFRelease)
    (JNIEnv *env, jclass that, jlong arg0)
{
    CFRelease((CFTypeRef)arg0);
}

JNIEXPORT jlong JNICALL OS_NATIVE(CFURLCreateWithFileSystemPath)
    (JNIEnv *env, jclass that, jlong arg0, jlong arg1, jlong arg2, jboolean arg3)
{
    return (jlong)CFURLCreateWithFileSystemPath((CFAllocatorRef)arg0, (CFStringRef)arg1, (CFURLPathStyle)arg2, (Boolean)arg3);
}

JNIEXPORT jboolean JNICALL OS_NATIVE(CTFontManagerRegisterFontsForURL)
    (JNIEnv *env, jclass that, jlong arg0, jint arg1, jlong arg2)
{
    return (jboolean)CTFontManagerRegisterFontsForURL((CFURLRef)arg0, (CTFontManagerScope)arg1, (CFErrorRef*)arg2);
}

JNIEXPORT jlong JNICALL OS_NATIVE(CTFontCreatePathForGlyph)
    (JNIEnv *env, jclass that, jlong arg0, jshort arg1, jobject arg2)
{
    CGAffineTransform _arg2, *lparg2=NULL;
    jlong rc = 0;
    if (arg2) if ((lparg2 = getCGAffineTransformFields(env, arg2, &_arg2)) == NULL) goto fail;
    rc = (jlong)CTFontCreatePathForGlyph((CTFontRef)arg0, (CGGlyph)arg1, (CGAffineTransform*)lparg2);
fail:
    /* In Only */
//    if (arg2 && lparg2) setCGAffineTransformFields(env, arg2, lparg2);
    return rc;
}

JNIEXPORT void JNICALL OS_NATIVE(CGPathRelease)
    (JNIEnv *env, jclass that, jlong arg0)
{
    CGPathRelease((CGPathRef)arg0);
}

JNIEXPORT jlong JNICALL OS_NATIVE(CGColorSpaceCreateDeviceRGB)
    (JNIEnv *env, jclass that)
{
    return (jlong)CGColorSpaceCreateDeviceRGB();
}

JNIEXPORT jlong JNICALL OS_NATIVE(CGColorSpaceCreateDeviceGray)
    (JNIEnv *env, jclass that)
{
    return (jlong)CGColorSpaceCreateDeviceGray();
}

JNIEXPORT jlong JNICALL OS_NATIVE(CGBitmapContextCreate)
    (JNIEnv *env, jclass that, jlong arg0, jlong arg1, jlong arg2, jlong arg3, jlong arg4, jlong arg5, jint arg6)
{
    return (jlong)CGBitmapContextCreate((void*)arg0, (size_t)arg1, (size_t)arg2, (size_t)arg3, (size_t)arg4, (CGColorSpaceRef)arg5, (CGBitmapInfo)arg6);
}

JNIEXPORT void JNICALL OS_NATIVE(CGContextSetAllowsFontSmoothing)
    (JNIEnv *env, jclass that, jlong arg0, jboolean arg1)
{
    CGContextSetAllowsFontSmoothing((CGContextRef)arg0, (_Bool)arg1);
}

JNIEXPORT void JNICALL OS_NATIVE(CGContextSetAllowsAntialiasing)
    (JNIEnv *env, jclass that, jlong arg0, jboolean arg1)
{
    CGContextSetAllowsAntialiasing((CGContextRef)arg0, (_Bool)arg1);
}

JNIEXPORT void JNICALL OS_NATIVE(CGContextSetAllowsFontSubpixelPositioning)
    (JNIEnv *env, jclass that, jlong arg0, jboolean arg1)
{
    CGContextSetAllowsFontSubpixelPositioning((CGContextRef)arg0, (_Bool)arg1);
}

JNIEXPORT void JNICALL OS_NATIVE(CGContextSetAllowsFontSubpixelQuantization)
    (JNIEnv *env, jclass that, jlong arg0, jboolean arg1)
{
    CGContextSetAllowsFontSubpixelQuantization((CGContextRef)arg0, (_Bool)arg1);
}

JNIEXPORT void JNICALL OS_NATIVE(CGContextSetRGBFillColor)
    (JNIEnv *env, jclass that, jlong arg0, jdouble arg1, jdouble arg2, jdouble arg3, jdouble arg4)
{
    CGContextSetRGBFillColor((CGContextRef)arg0, (CGFloat)arg1, (CGFloat)arg2, (CGFloat)arg3, (CGFloat)arg4);
}

JNIEXPORT void JNICALL OS_NATIVE(CGContextFillRect)
    (JNIEnv *env, jclass that, jlong arg0, jobject arg1)
{
    CGRect _arg1, *lparg1=NULL;
    /* In Only */
    if (arg1) if ((lparg1 = getCGRectFields(env, arg1, &_arg1)) == NULL) return;
    CGContextFillRect((CGContextRef)arg0, *lparg1);
}

JNIEXPORT void JNICALL OS_NATIVE(CGContextTranslateCTM)
    (JNIEnv *env, jclass that, jlong arg0, jdouble arg1, jdouble arg2)
{
    CGContextTranslateCTM((CGContextRef)arg0, (CGFloat)arg1, (CGFloat)arg2);
}

JNIEXPORT void JNICALL OS_NATIVE(CGContextRelease)
    (JNIEnv *env, jclass that, jlong arg0)
{
    CGContextRelease((CGContextRef)arg0);
}

JNIEXPORT void JNICALL OS_NATIVE(CGColorSpaceRelease)
    (JNIEnv *env, jclass that, jlong arg0)
{
    CGColorSpaceRelease((CGColorSpaceRef)arg0);
}

JNIEXPORT jlong JNICALL OS_NATIVE(kCFTypeDictionaryKeyCallBacks)
    (JNIEnv *env, jclass that)
{
    return (jlong)&kCFTypeDictionaryKeyCallBacks;
}

JNIEXPORT jlong JNICALL OS_NATIVE(kCFTypeDictionaryValueCallBacks)
    (JNIEnv *env, jclass that)
{
    return (jlong)&kCFTypeDictionaryValueCallBacks;
}

JNIEXPORT jlong JNICALL OS_NATIVE(CFDictionaryCreateMutable)
    (JNIEnv *env, jclass that, jlong arg0, jlong arg1, jlong arg2, jlong arg3)
{
    return (jlong)CFDictionaryCreateMutable((CFAllocatorRef)arg0, (CFIndex)arg1, (CFDictionaryKeyCallBacks*)arg2, (CFDictionaryValueCallBacks*)arg3);
}

JNIEXPORT void JNICALL OS_NATIVE(CFDictionaryAddValue)
    (JNIEnv *env, jclass that, jlong arg0, jlong arg1, jlong arg2)
{
    CFDictionaryAddValue((CFMutableDictionaryRef)arg0, (void*)arg1, (void*)arg2);
}

JNIEXPORT jlong JNICALL OS_NATIVE(CFDictionaryGetValue)
    (JNIEnv *env, jclass that, jlong arg0, jlong arg1)
{
    return (jlong)CFDictionaryGetValue((CFDictionaryRef)arg0, (void*)arg1);
}

JNIEXPORT jlong JNICALL OS_NATIVE(kCTFontAttributeName)
    (JNIEnv *env, jclass that)
{
    return (jlong)kCTFontAttributeName;
}

JNIEXPORT jlong JNICALL OS_NATIVE(kCTParagraphStyleAttributeName)
    (JNIEnv *env, jclass that)
{
    return (jlong)kCTParagraphStyleAttributeName;
}

JNIEXPORT jlong JNICALL OS_NATIVE(CFAttributedStringCreate)
    (JNIEnv *env, jclass that, jlong arg0, jlong arg1, jlong arg2)
{
    return (jlong)CFAttributedStringCreate((CFAllocatorRef)arg0, (CFStringRef)arg1, (CFDictionaryRef)arg2);
}

JNIEXPORT jlong JNICALL OS_NATIVE(CTLineCreateWithAttributedString)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jlong)CTLineCreateWithAttributedString((CFAttributedStringRef)arg0);
}

JNIEXPORT jlong JNICALL OS_NATIVE(CTLineGetGlyphRuns)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jlong)CTLineGetGlyphRuns((CTLineRef)arg0);
}

JNIEXPORT jdouble JNICALL OS_NATIVE(CTLineGetTypographicBounds)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jdouble)CTLineGetTypographicBounds((CTLineRef)arg0, NULL, NULL, NULL);
}

JNIEXPORT jlong JNICALL OS_NATIVE(CTLineGetGlyphCount)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jlong)CTLineGetGlyphCount((CTLineRef)arg0);
}

JNIEXPORT jlong JNICALL OS_NATIVE(CFArrayGetCount)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jlong)CFArrayGetCount((CFArrayRef)arg0);
}

JNIEXPORT jlong JNICALL OS_NATIVE(CFArrayGetValueAtIndex)
    (JNIEnv *env, jclass that, jlong arg0, jlong arg1)
{
    return (jlong)CFArrayGetValueAtIndex((CFArrayRef)arg0, (CFIndex)arg1);
}

JNIEXPORT jlong JNICALL OS_NATIVE(CTRunGetGlyphCount)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jlong)CTRunGetGlyphCount((CTRunRef)arg0);
}

JNIEXPORT jlong JNICALL OS_NATIVE(CTRunGetAttributes)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jlong)CTRunGetAttributes((CTRunRef)arg0);
}

/**************************************************************************/
/*                                                                        */
/*                           Custom Functions                             */
/*                                                                        */
/**************************************************************************/


JNIEXPORT jlong JNICALL OS_NATIVE(CFStringCreateWithCharacters__J_3CJJ)
    (JNIEnv *env, jclass that, jlong arg0, jcharArray arg1, jlong arg2, jlong arg3)
{
    jchar *lparg1=NULL;
    jlong rc = 0;
    if (arg1) if ((lparg1 = (*env)->GetPrimitiveArrayCritical(env, arg1, NULL)) == NULL) goto fail;
    UniChar* str = lparg1 + arg2;
    rc = (jlong)CFStringCreateWithCharacters((CFAllocatorRef)arg0, str, (CFIndex)arg3);
fail:
    if (arg1 && lparg1) (*env)->ReleasePrimitiveArrayCritical(env, arg1, lparg1, 0);
    return rc;
}

JNIEXPORT jint JNICALL OS_NATIVE(CTRunGetGlyphs)
    (JNIEnv *env, jclass that, jlong runRef, jint slotMask, jint start, jintArray bufferRef)
{
    CTRunRef run = (CTRunRef)runRef;
    const CGGlyph * glyphs = CTRunGetGlyphsPtr(run);
    int i = 0;
    if (glyphs) {
        jint* buffer = (*env)->GetPrimitiveArrayCritical(env, bufferRef, NULL);
        if (buffer) {
            CFIndex count = CTRunGetGlyphCount(run);
            while(i < count) {
                buffer[start + i] = slotMask | (glyphs[i] & 0xFFFF);
                i++;
            }
            (*env)->ReleasePrimitiveArrayCritical(env, bufferRef, buffer, 0);
        }
    }
    return i;
}

JNIEXPORT jint JNICALL OS_NATIVE(CTRunGetPositions)
    (JNIEnv *env, jclass that, jlong runRef, jint start, jfloatArray bufferRef)
{
    CTRunRef run = (CTRunRef)runRef;
    const CGPoint* positions = CTRunGetPositionsPtr(run);
    int j = 0;
    if (positions) {
        jfloat* buffer = (*env)->GetPrimitiveArrayCritical(env, bufferRef, NULL);
        if (buffer) {
            CFIndex count = CTRunGetGlyphCount(run);
            int i = 0;
            while (i < count) {
                CGPoint pos = positions[i++];
                buffer[start + j++] = pos.x;
                buffer[start + j++] = pos.y;
            }
            (*env)->ReleasePrimitiveArrayCritical(env, bufferRef, buffer, 0);
        }
    }
    return j;
}

JNIEXPORT jint JNICALL OS_NATIVE(CTRunGetStringIndices)
    (JNIEnv *env, jclass that, jlong runRef, jint start, jintArray bufferRef)
{
    CTRunRef run = (CTRunRef)runRef;
    const CFIndex* indices = CTRunGetStringIndicesPtr(run);
    int i = 0;
    if (indices) {
        jint* buffer = (*env)->GetPrimitiveArrayCritical(env, bufferRef, NULL);
        if (buffer) {
            CFIndex count = CTRunGetGlyphCount(run);
            while(i < count) {
                buffer[start + i] = indices[i];
                i++;
            }
            (*env)->ReleasePrimitiveArrayCritical(env, bufferRef, buffer, 0);
        }
    }
    return i;
}

JNIEXPORT jstring JNICALL OS_NATIVE(CTFontCopyAttributeDisplayName)
    (JNIEnv *env, jclass that, jlong arg0)
{
    CFStringRef stringRef = CTFontCopyAttribute((CTFontRef)arg0, kCTFontDisplayNameAttribute);
    if (stringRef == NULL) return NULL;
    CFIndex length = CFStringGetLength(stringRef);
    UniChar buffer[length];
    CFStringGetCharacters(stringRef, CFRangeMake(0, length), buffer);
    CFRelease(stringRef);
    return (*env)->NewString(env, (jchar *)buffer, length);
}

JNIEXPORT jbyteArray JNICALL OS_NATIVE(CGBitmapContextGetData)
    (JNIEnv *env, jclass that, jlong arg0, jint dstWidth, jint dstHeight, jint bpp)
{
    jbyteArray result = NULL;
    if (dstWidth < 0) return NULL;
    if (dstHeight < 0) return NULL;
    if (bpp != 8 && bpp != 24) return NULL;
    CGContextRef context = (CGContextRef)arg0;
    if (context == NULL) return NULL;
    jbyte *srcData = (jbyte*)CGBitmapContextGetData(context);

    if (srcData) {
        /* Use one byte per pixel for grayscale */
        size_t srcWidth = CGBitmapContextGetWidth(context);
        if (srcWidth < dstWidth) return NULL;
        size_t srcHeight =  CGBitmapContextGetHeight(context);
        if (srcHeight < dstHeight) return NULL;
        size_t srcBytesPerRow = CGBitmapContextGetBytesPerRow(context);
        size_t srcStep = CGBitmapContextGetBitsPerPixel(context) / 8;
        int srcOffset = (srcHeight - dstHeight) * srcBytesPerRow;


        //bits per pixel, either 8 for gray or 24 for LCD.
        int dstStep = bpp / 8;
        size_t size = dstWidth * dstHeight * dstStep;
        jbyte* data = (jbyte*)calloc(size, sizeof(jbyte));
        if (data == NULL) return NULL;

        int x, y, sx;
        int dstOffset = 0;
        for (y = 0; y < dstHeight; y++) {
            for (x = 0, sx = 0; x < dstWidth; x++, dstOffset += dstStep, sx += srcStep) {
                if (dstStep == 1) {
                    /* BGRA or Gray to Gray */
                    data[dstOffset] = 0xFF - srcData[srcOffset + sx];
                } else {
                    /* BGRA to RGB */
                    data[dstOffset]     = 0xFF - srcData[srcOffset + sx + 2];
                    data[dstOffset + 1] = 0xFF - srcData[srcOffset + sx + 1];
                    data[dstOffset + 2] = 0xFF - srcData[srcOffset + sx];
                }
            }
            srcOffset += srcBytesPerRow;
        }

        result = (*env)->NewByteArray(env, size);
        if (result) {
            (*env)->SetByteArrayRegion(env, result, 0, size, data);
        }
        free(data);
    }
    return result;
}

JNIEXPORT void JNICALL OS_NATIVE(CGRectApplyAffineTransform)
    (JNIEnv *env, jclass that, jobject arg0, jobject arg1)
{
    CGRect _arg0, *lparg0=NULL;
    CGAffineTransform _arg1, *lparg1=NULL;
    if (arg0) if ((lparg0 = getCGRectFields(env, arg0, &_arg0)) == NULL) goto fail;
    if (arg1) if ((lparg1 = getCGAffineTransformFields(env, arg1, &_arg1)) == NULL) goto fail;
    _arg0 = CGRectApplyAffineTransform(*lparg0, *lparg1);
fail:
    /* In Only */
//    if (arg1 && lparg1) setCGAffineTransformFields(env, arg1, lparg1);
    if (arg0 && lparg0) setCGRectFields(env, arg0, lparg0);
}

JNIEXPORT void JNICALL OS_NATIVE(CTFontDrawGlyphs)
    (JNIEnv *env, jclass that, jlong arg0, jshort arg1, jdouble arg2, jdouble arg3, jlong contextRef)
{
    /* Custom: only takes one glyph at the time */
    CGGlyph glyphs[] = {arg1};
    CGPoint pos[] = {CGPointMake(arg2, arg3)};
    CTFontDrawGlyphs((CTFontRef)arg0, glyphs, pos, 1, (CGContextRef)contextRef);
}

JNIEXPORT jboolean JNICALL OS_NATIVE(CTFontGetBoundingRectForGlyphUsingTables)
    (JNIEnv *env, jclass that, jlong arg1, jshort arg2, jshort arg3, jintArray arg4)
{
    /* The following code is based on scalerMethods.c#getGlyphBoundingBoxNative */
    CTFontRef fontRef = (CTFontRef)arg1;
    CTFontTableOptions options = kCTFontTableOptionNoOptions;
    CFDataRef tableData;
    CFIndex length;

    /* indexToLocFormat is stored in Java for performance */
//    tableData = CTFontCopyTable(fontRef, kCTFontTableHead, options);
//    const UInt8 * head = CFDataGetBytePtr(tableData);
//    UInt16 indexToLocFormat = CFSwapInt16BigToHost(*((SInt16*)(head + 50)));
//    printf("here0 indexToLocFormat=%u \n", indexToLocFormat); fflush(stdout);
//    CFRelease(tableData);
    UInt16 indexToLocFormat = arg3;

    tableData = CTFontCopyTable(fontRef, kCTFontTableLoca, options);
    if (tableData == NULL) return FALSE;
    length = CFDataGetLength(tableData);
    UInt32 offset1 = 0, offset2 = 0;
    UInt32 index = arg2 & 0xFFFF;
    if (indexToLocFormat) {
        const UInt32 * loca = (const UInt32 *)CFDataGetBytePtr(tableData);
        if (loca != NULL && (index + 1) < (length / 4)) {
            offset1 = CFSwapInt32BigToHost(loca[index]);
            offset2 = CFSwapInt32BigToHost(loca[index + 1]);
        }
    } else {
        const UInt16 * loca = (const UInt16 *)CFDataGetBytePtr(tableData);
        if (loca != NULL && (index + 1) < (length / 2)) {
            offset1 = CFSwapInt16BigToHost(loca[index]) << 1;
            offset2 = CFSwapInt16BigToHost(loca[index + 1]) << 1;
        }
    }
    CFRelease(tableData);
    jboolean result = FALSE;
    if (offset2 > offset1 && (offset2 - offset1) >= 10) {
        tableData = CTFontCopyTable(fontRef, kCTFontTableGlyf, options);
        if (tableData == NULL) return FALSE;
        length = CFDataGetLength(tableData);
        const UInt8 * ptr = CFDataGetBytePtr(tableData);
        if (ptr != NULL && (offset1 + 10) < length) {
            const SInt16 * glyf = (const SInt16 *)(ptr + offset1);
            /*
             * CFSwapInt16BigToHost returns an unsigned short, need
             * to cast back to signed short before assigning to jint.
             */
            jint data[] = {
                (SInt16)CFSwapInt16BigToHost(glyf[1]),
                (SInt16)CFSwapInt16BigToHost(glyf[2]),
                (SInt16)CFSwapInt16BigToHost(glyf[3]),
                (SInt16)CFSwapInt16BigToHost(glyf[4]),
            };
            (*env)->SetIntArrayRegion(env, arg4, 0, 4, data);
            result = TRUE;
        }
        CFRelease(tableData);
    }
    return result;
}

JNIEXPORT jdouble JNICALL OS_NATIVE(CTFontGetAdvancesForGlyphs)
    (JNIEnv *env, jclass that, jlong arg0, jint arg1, jshort arg2, jobject arg3)
{
    /* Custom: only takes one glyph at the time */
    jdouble rc = 0;
    CGGlyph glyphs[] = {arg2};
    CGSize _arg3, *lparg3=NULL;
    if (arg3) if ((lparg3 = getCGSizeFields(env, arg3, &_arg3)) == NULL) goto fail;
    rc = (jdouble)CTFontGetAdvancesForGlyphs((CTFontRef)arg0, (CTFontOrientation)arg1, glyphs, lparg3, 1);
fail:
    if (arg3 && lparg3) setCGSizeFields(env, arg3, &_arg3);
    return rc;
}

JNIEXPORT jobject JNICALL OS_NATIVE(CGPathGetPathBoundingBox)
    (JNIEnv *env, jclass that, jlong arg0)
{
    CGRect result = CGPathGetPathBoundingBox((CGPathRef)arg0);
    return newCGRect(env, &result);
}

JNIEXPORT jlong JNICALL OS_NATIVE(CTParagraphStyleCreate)
    (JNIEnv *env, jclass that, jint arg0)
{
    CTWritingDirection dir = (CTWritingDirection)arg0;
    CTParagraphStyleSetting settings[] = {
        {kCTParagraphStyleSpecifierBaseWritingDirection, sizeof(dir), &dir}
    };
    return (jlong)CTParagraphStyleCreate(settings, sizeof(settings) / sizeof(settings[0]));
}

/***********************************************/
/*                Glyph Outline                */
/***********************************************/

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

void pathApplierFunctionFast(void *i, const CGPathElement *e) {
    PathData *info = (PathData *)i;
    if (info->numTypes == info->lenTypes) {
        info->lenTypes += DEFAULT_LEN_TYPES;
        info->pointTypes = (jbyte*)realloc(info->pointTypes, info->lenTypes * sizeof(jbyte));
    }
    jint type;
    int coordCount = 0;
    switch (e->type) {
    case kCGPathElementMoveToPoint:
        type = 0;
        coordCount = 1;
        break;
    case kCGPathElementAddLineToPoint:
        type = 1;
        coordCount = 1;
        break;
    case kCGPathElementAddQuadCurveToPoint:
        type = 2;
        coordCount = 2;
        break;
    case kCGPathElementAddCurveToPoint:
        type = 3;
        coordCount = 3;
        break;
    case kCGPathElementCloseSubpath:
        type = 4;
        coordCount = 0;
        break;
    }
    info->pointTypes[info->numTypes++] = type;

    if (info->numCoords + (coordCount * 2) > info->lenCoords) {
        info->lenCoords += DEFAULT_LEN_COORDS;
        info->pointCoords = (jfloat*)realloc(info->pointCoords, info->lenCoords * sizeof(jfloat));
    }
    int j;
    for (j = 0; j < coordCount; j++) {
        CGPoint pt = e->points[j];
        info->pointCoords[info->numCoords++] = pt.x;
        info->pointCoords[info->numCoords++] = pt.y;
    }
}

JNIEXPORT jobject JNICALL OS_NATIVE(CGPathApply)
    (JNIEnv *env, jclass that, jlong arg0)
{
    jobject path2D = NULL;
    PathData data;
    data.pointTypes = (jbyte*)malloc(sizeof(jbyte) * DEFAULT_LEN_TYPES);
    data.numTypes = 0;
    data.lenTypes = DEFAULT_LEN_TYPES;
    data.pointCoords = (jfloat*)malloc(sizeof(jfloat) * DEFAULT_LEN_COORDS);
    data.numCoords = 0;
    data.lenCoords = DEFAULT_LEN_COORDS;
    
    CGPathApply((CGPathRef)arg0, &data, pathApplierFunctionFast);

    static jclass path2DClass = NULL;
    static jmethodID path2DCtr = NULL;
    if (path2DClass == NULL) {
        jclass tmpClass = (*env)->FindClass(env, "com/sun/javafx/geom/Path2D");
        if ((*env)->ExceptionOccurred(env) || !tmpClass) {   
            fprintf(stderr, "OS_NATIVE error: JNI exception or tmpClass == NULL");
            goto fail;
        }
        path2DClass = (jclass)(*env)->NewGlobalRef(env, tmpClass);
        path2DCtr = (*env)->GetMethodID(env, path2DClass, "<init>", "(I[BI[FI)V");
        if ((*env)->ExceptionOccurred(env) || !path2DCtr) {   
            fprintf(stderr, "OS_NATIVE error: JNI exception or path2DCtr == NULL");
            goto fail;
        }
    }

    jbyteArray types = (*env)->NewByteArray(env, data.numTypes);  
    jfloatArray coords = (*env)->NewFloatArray(env, data.numCoords);
    if (types && coords) {
        (*env)->SetByteArrayRegion(env, types, 0, data.numTypes, data.pointTypes);
        if ((*env)->ExceptionOccurred(env)) {   
            fprintf(stderr, "OS_NATIVE error: JNI exception");
            goto fail;
        }
        (*env)->SetFloatArrayRegion(env, coords, 0, data.numCoords, data.pointCoords);
        if ((*env)->ExceptionOccurred(env)) {   
            fprintf(stderr, "OS_NATIVE error: JNI exception");
            goto fail;
        }
        path2D = (*env)->NewObject(env, path2DClass, path2DCtr,
                                   0 /*winding rule*/,
                                   types, data.numTypes,
                                   coords, data.numCoords);
        if ((*env)->ExceptionOccurred(env) || !path2D) {   
            goto fail;
        }
    }
fail:
    free(data.pointTypes);
    free(data.pointCoords);
    return path2D;
}

#endif /* TARGET_OS_MAC */
#endif /* __APPLE__ */

