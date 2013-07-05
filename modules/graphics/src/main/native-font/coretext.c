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
    CGAffineTransformFc.clazz =  (jclass)(*env)->NewGlobalRef(env, tmpClass);
    CGAffineTransformFc.a = (*env)->GetFieldID(env, CGAffineTransformFc.clazz, "a", "D");
    CGAffineTransformFc.b = (*env)->GetFieldID(env, CGAffineTransformFc.clazz, "b", "D");
    CGAffineTransformFc.c = (*env)->GetFieldID(env, CGAffineTransformFc.clazz, "c", "D");
    CGAffineTransformFc.d = (*env)->GetFieldID(env, CGAffineTransformFc.clazz, "d", "D");
    CGAffineTransformFc.tx = (*env)->GetFieldID(env, CGAffineTransformFc.clazz, "tx", "D");
    CGAffineTransformFc.ty = (*env)->GetFieldID(env, CGAffineTransformFc.clazz, "ty", "D");
    CGAffineTransformFc.init = (*env)->GetMethodID(env, CGAffineTransformFc.clazz, "<init>", "()V");
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

typedef struct CFRange_FID_CACHE {
    int cached;
    jclass clazz;
    jfieldID location, length;
    jmethodID init;
} CFRange_FID_CACHE;

CFRange_FID_CACHE CFRangeFc;

void cacheCFRangeFields(JNIEnv *env)
{
    if (CFRangeFc.cached) return;
    jclass tmpClass = (*env)->FindClass(env, "com/sun/javafx/font/coretext/CFRange");
    CFRangeFc.clazz =  (jclass)(*env)->NewGlobalRef(env, tmpClass);
    CFRangeFc.location = (*env)->GetFieldID(env, CFRangeFc.clazz, "location", "J");
    CFRangeFc.length = (*env)->GetFieldID(env, CFRangeFc.clazz, "length", "J");
    CFRangeFc.init = (*env)->GetMethodID(env, CFRangeFc.clazz, "<init>", "()V");
    CFRangeFc.cached = 1;
}

CFRange *getCFRangeFields(JNIEnv *env, jobject lpObject, CFRange *lpStruct)
{
    if (!CFRangeFc.cached) cacheCFRangeFields(env);
    lpStruct->location = (*env)->GetLongField(env, lpObject, CFRangeFc.location);
    lpStruct->length = (*env)->GetLongField(env, lpObject, CFRangeFc.length);
    return lpStruct;
}

void setCFRangeFields(JNIEnv *env, jobject lpObject, CFRange *lpStruct)
{
    if (!CFRangeFc.cached) cacheCFRangeFields(env);
    (*env)->SetLongField(env, lpObject, CFRangeFc.location, (jlong)lpStruct->location);
    (*env)->SetLongField(env, lpObject, CFRangeFc.length, (jlong)lpStruct->length);
}

jobject newCFRange(JNIEnv *env, CFRange *lpStruct)
{
    jobject lpObject = NULL;
    if (!CFRangeFc.cached) cacheCFRangeFields(env);
    lpObject = (*env)->NewObject(env, CFRangeFc.clazz, CFRangeFc.init);
    if (lpObject && lpStruct) setCFRangeFields(env, lpObject, lpStruct);
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
    CGPointFc.clazz =  (jclass)(*env)->NewGlobalRef(env, tmpClass);
    CGPointFc.x = (*env)->GetFieldID(env, CGPointFc.clazz, "x", "D");
    CGPointFc.y = (*env)->GetFieldID(env, CGPointFc.clazz, "y", "D");
    CGPointFc.init = (*env)->GetMethodID(env, CGPointFc.clazz, "<init>", "()V");
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
    CGSizeFc.clazz =  (jclass)(*env)->NewGlobalRef(env, tmpClass);
    CGSizeFc.width = (*env)->GetFieldID(env, CGSizeFc.clazz, "width", "D");
    CGSizeFc.height = (*env)->GetFieldID(env, CGSizeFc.clazz, "height", "D");
    CGSizeFc.init = (*env)->GetMethodID(env, CGSizeFc.clazz, "<init>", "()V");
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
    CGRectFc.clazz =  (jclass)(*env)->NewGlobalRef(env, tmpClass);
    CGRectFc.origin = (*env)->GetFieldID(env, CGRectFc.clazz, "origin", "Lcom/sun/javafx/font/coretext/CGPoint;");
    CGRectFc.size = (*env)->GetFieldID(env, CGRectFc.clazz, "size", "Lcom/sun/javafx/font/coretext/CGSize;");
    CGRectFc.init = (*env)->GetMethodID(env, CGRectFc.clazz, "<init>", "()V");
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

JNIEXPORT jint JNICALL OS_NATIVE(CTRunGetStatus)
    (JNIEnv *env, jclass that, jlong arg0)
{
    return (jint)CTRunGetStatus((CTRunRef)arg0);
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

JNIEXPORT jfloatArray JNICALL OS_NATIVE(CTRunGetAdvancesPtr)
    (JNIEnv *env, jclass that, jlong arg0)
{
    CTRunRef run = (CTRunRef)arg0;
    const CGSize* advances = CTRunGetAdvancesPtr(run);
    if (advances) {
        CFIndex count = CTRunGetGlyphCount(run);
        jfloatArray result = (*env)->NewFloatArray(env, count * 2);
        if (result) {
            int i, j;
            jfloat data[count*2];
            for(i = 0, j = 0; i < count; i++) {
                CGSize advance = advances[i];
                data[j++] = advance.width;
                data[j++] = advance.height;
            }
            (*env)->SetFloatArrayRegion(env, result, 0, count * 2, data);
            return result;
        }
    }
    return NULL;
}

JNIEXPORT jintArray JNICALL OS_NATIVE(CTRunGetGlyphsPtr)
    (JNIEnv *env, jclass that, jlong arg0)
{
    CTRunRef run = (CTRunRef)arg0;
    const CGGlyph * glyphs = CTRunGetGlyphsPtr(run);
    if (glyphs) {
        CFIndex count = CTRunGetGlyphCount(run);
        jintArray result = (*env)->NewIntArray(env, count);
        if (result) {
            int i;
            jint data[count];
            for(i = 0; i < count; i++) {
                data[i] = glyphs[i];
            }
            (*env)->SetIntArrayRegion(env, result, 0, count, data);
            return result;
        }
    }
    return NULL;
}

JNIEXPORT jfloatArray JNICALL OS_NATIVE(CTRunGetPositionsPtr)
    (JNIEnv *env, jclass that, jlong arg0)
{
    CTRunRef run = (CTRunRef)arg0;
    const CGPoint* positions = CTRunGetPositionsPtr(run);
    if (positions) {
        CFIndex count = CTRunGetGlyphCount(run);
        jfloatArray result = (*env)->NewFloatArray(env, count * 2 + 2);
        if (result) {
            int i, j;
            CGFloat x = positions[0].x;
            CGFloat y = positions[0].y;
            jfloat data[count * 2 + 2];
            for(i = 0, j = 0; i < count; i++) {
                CGPoint pos = positions[i];
                data[j++] = pos.x - x;
                data[j++] = pos.y - y;
            }
            data[j++] = CTRunGetTypographicBounds(run, CFRangeMake(0, 0), NULL, NULL, NULL);
            data[j++] = 0;
            (*env)->SetFloatArrayRegion(env, result, 0, count * 2 + 2, data);
            return result;
        }
    }
    return NULL;
}

JNIEXPORT jintArray JNICALL OS_NATIVE(CTRunGetStringIndicesPtr)
    (JNIEnv *env, jclass that, jlong arg0)
{
    CTRunRef run = (CTRunRef)arg0;
    const CFIndex* indices = CTRunGetStringIndicesPtr(run);
    if (indices) {
        CFIndex count = CTRunGetGlyphCount(run);
        CFIndex start = CTRunGetStringRange(run).location;
        jintArray result = (*env)->NewIntArray(env, count);
        if (result) {
            int i;
            jint data[count];
            for(i = 0; i < count; i++) {
                data[i] = indices[i] - start;
            }
            (*env)->SetIntArrayRegion(env, result, 0, count, data);
            return result;
        }
    }
    return NULL;
}

JNIEXPORT jobject JNICALL OS_NATIVE(CTRunGetStringRange)
    (JNIEnv *env, jclass that, jlong arg0)
{
    CTRunRef run = (CTRunRef)arg0;
    CFRange result = CTRunGetStringRange(run);
    return newCFRange(env, &result);
}

JNIEXPORT jstring JNICALL OS_NATIVE(CTFontCopyDisplayName)
    (JNIEnv *env, jclass that, jlong arg0)
{
    CFStringRef stringRef = CTFontCopyDisplayName((CTFontRef)arg0);

    /* Copied from MacFontFinder#createJavaString */
    CFIndex length = CFStringGetLength(stringRef);
    UniChar buffer[length];
    CFStringGetCharacters(stringRef, CFRangeMake(0, length), buffer);
    CFRelease(stringRef);
    return (*env)->NewString(env, (jchar *)buffer, length);
}

JNIEXPORT jbyteArray JNICALL OS_NATIVE(CGBitmapContextGetData__J)
    (JNIEnv *env, jclass that, jlong arg0)
{
    jbyteArray result = NULL;
    CGContextRef context = (CGContextRef)arg0;
    void* data = CGBitmapContextGetData(context);
    if (data) {
        size_t size = CGBitmapContextGetBytesPerRow(context) * CGBitmapContextGetHeight(context);
        result = (*env)->NewByteArray(env, size);
        if (result) {
            (*env)->SetByteArrayRegion(env, result, 0, size, data);
        }
    }
    return result;
}

JNIEXPORT jbyteArray JNICALL OS_NATIVE(CGBitmapContextGetData__JIII)
    (JNIEnv *env, jclass that, jlong arg0, jint dstWidth, jint dstHeight, jint bpp)
{
    jbyteArray result = NULL;
    CGContextRef context = (CGContextRef)arg0;
    jbyte *srcData = (jbyte*)CGBitmapContextGetData(context);

    if (srcData) {
        /* Use one byte per pixel for grayscale */
        size_t srcWidth = CGBitmapContextGetWidth(context);
        size_t srcHeight =  CGBitmapContextGetHeight(context);
        size_t srcBytesPerRow = CGBitmapContextGetBytesPerRow(context);
        size_t srcStep = CGBitmapContextGetBitsPerPixel(context) / 8;
        int srcOffset = (srcHeight - dstHeight) * srcBytesPerRow;


        //bits per pixel, either 8 for gray or 24 for LCD.
        int dstStep = bpp / 8;
        size_t size = dstWidth * dstHeight * dstStep;
        jbyte data[size];

        int x, y, sx;
        int dstOffset = 0;
        for (y = 0; y < dstHeight; y++) {
            for (x = 0, sx = 0; x < dstWidth; x++, dstOffset += dstStep, sx += srcStep) {
                if (dstStep == 1) {
                    /* BGRA or Gray to Gray*/
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
    }
    return result;
}

JNIEXPORT void JNICALL OS_NATIVE(CGPointApplyAffineTransform)
    (JNIEnv *env, jclass that, jobject arg0, jobject arg1)
{
    CGPoint _arg0, *lparg0=NULL;
    CGAffineTransform _arg1, *lparg1=NULL;
    if (arg0) if ((lparg0 = getCGPointFields(env, arg0, &_arg0)) == NULL) goto fail;
    if (arg1) if ((lparg1 = getCGAffineTransformFields(env, arg1, &_arg1)) == NULL) goto fail;
    _arg0 = CGPointApplyAffineTransform(*lparg0, *lparg1);
fail:
    /* In Only */
//    if (arg1 && lparg1) setCGAffineTransformFields(env, arg1, lparg1);
    if (arg0 && lparg0) setCGPointFields(env, arg0, lparg0);
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
    (JNIEnv *env, jclass that, jlong arg0, jshort arg1, jdouble arg2, jdouble arg3, jlong arg4, jlong arg5)
{
    /* Custom: only takes one glyph at the time */
    CGGlyph glyphs[] = {arg1};
    CGPoint pos[] = {CGPointMake(arg2, arg3)};
    CTFontDrawGlyphs((CTFontRef)arg0, glyphs, pos, 1, (CGContextRef)arg5);
}

JNIEXPORT jobject JNICALL OS_NATIVE(CTFontGetBoundingRectsForGlyphs)
    (JNIEnv *env, jclass that, jlong arg1, jint arg2, jshort arg3, jobject arg4, jlong arg5)
{
    /* Custom: only takes one glyph at the time */
    jobject rc = NULL;
    CGRect result;
    CGGlyph glyphs[] = {arg3};
    CGRect _arg4, *lparg4=NULL;
    if (arg4) if ((lparg4 = getCGRectFields(env, arg4, &_arg4)) == NULL) goto fail;
    result = CTFontGetBoundingRectsForGlyphs((CTFontRef)arg1, (CTFontOrientation)arg2, glyphs, lparg4, 1);
    rc = newCGRect(env, &result);
fail:
    if (arg4 && lparg4) setCGRectFields(env, arg4, &_arg4);
    return rc;
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
    if (indexToLocFormat) {
        const UInt32 * loca = (const UInt32 *)CFDataGetBytePtr(tableData);
        if (loca != NULL && length / 4 > arg2) {
            offset1 = CFSwapInt32BigToHost(loca[arg2]);
            offset2 = CFSwapInt32BigToHost(loca[arg2 + 1]);
        }
    } else {
        const UInt16 * loca = (const UInt16 *)CFDataGetBytePtr(tableData);
        if (loca != NULL && length / 2 > arg2) {
            offset1 = CFSwapInt16BigToHost(loca[arg2]) << 1;
            offset2 = CFSwapInt16BigToHost(loca[arg2 + 1]) << 1;
        }
    }
    CFRelease(tableData);

    if (offset2 > offset1 && (offset2 - offset1) >= 10) {
        tableData = CTFontCopyTable(fontRef, kCTFontTableGlyf, options);
        if (tableData == NULL) return FALSE;
        length = CFDataGetLength(tableData);
        const UInt8 * ptr = CFDataGetBytePtr(tableData);
        if (ptr != NULL && length > (offset1 + 10)) {
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
        }
        CFRelease(tableData);
    }
    return TRUE;
}

JNIEXPORT jdouble JNICALL OS_NATIVE(CTFontGetAdvancesForGlyphs)
    (JNIEnv *env, jclass that, jlong arg0, jint arg1, jshort arg2, jobject arg3, jlong arg4)
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

JNIEXPORT jobject JNICALL OS_NATIVE(CGAffineTransformInvert)
    (JNIEnv *env, jclass that, jobject arg0)
{
    jobject rc = NULL;
    CGAffineTransform result;
    CGAffineTransform _arg0, *lparg0=NULL;
    if (arg0) if ((lparg0 = getCGAffineTransformFields(env, arg0, &_arg0)) == NULL) goto fail;
    result = CGAffineTransformInvert(*lparg0);
    rc = newCGAffineTransform(env, &result);
fail:
    /* In Only */
//    if (arg0 && lparg0) setCGAffineTransformFields(env, arg0, lparg0);
    return rc;
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

#endif /* TARGET_OS_MAC */
#endif /* __APPLE__ */

