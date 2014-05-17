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

#ifdef __APPLE__

#include <TargetConditionals.h>

#include <jni.h>
#include <com_sun_javafx_font_MacFontFinder.h>

#if TARGET_OS_IPHONE /* iOS */

#import <CoreText/CoreText.h>

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

#else /* MAC OS X */

#import <CoreFoundation/CoreFoundation.h>
#import <ApplicationServices/ApplicationServices.h>

#endif

extern jboolean checkAndClearException(JNIEnv *env);

jstring createJavaString(JNIEnv *env, CFStringRef stringRef)
{
    CFIndex length = CFStringGetLength(stringRef);
    UniChar buffer[length];
    CFStringGetCharacters(stringRef, CFRangeMake(0, length), buffer);
    jstring jStr = (*env)->NewString(env, (jchar *)buffer, length);
    if (checkAndClearException(env) || !jStr) {
        fprintf(stderr, "createJavaString error: JNI exception or jStr == NULL");
        return NULL;
    }
    return jStr;
}

/*
 * Class:     com_sun_javafx_font_MacFontFinder
 * Method:    getSystemFontSize
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_com_sun_javafx_font_MacFontFinder_getSystemFontSize
  (JNIEnv *env, jclass obj)
{
    CTFontRef font = CTFontCreateUIFontForLanguage(
                         kCTFontSystemFontType, 
                         0.0, //get system font with default size
                         NULL);
    jfloat systemFontDefaultSize = (jfloat) CTFontGetSize (font);
    CFRelease(font);
    return systemFontDefaultSize;
}

/*
 * Class:     com_sun_javafx_font_MacFontFinder
 * Method:    getFont
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_javafx_font_MacFontFinder_getFont
  (JNIEnv *env, jclass obj, jint type)
{
    CTFontRef font = CTFontCreateUIFontForLanguage(type, 0, NULL);
    CFStringRef family = CTFontCopyFamilyName(font);
    jstring jfamily = createJavaString(env, family);
    CFRelease(family);
    CFRelease(font);
    return jfamily;
}

CFIndex addCTFontDescriptor(CTFontDescriptorRef fd, JNIEnv *env, jobjectArray result, CFIndex index)
{
    if (fd) {
        CFStringRef name = CTFontDescriptorCopyAttribute(fd, kCTFontDisplayNameAttribute);
        CFStringRef family = CTFontDescriptorCopyAttribute(fd, kCTFontFamilyNameAttribute);
        CFURLRef url = CTFontDescriptorCopyAttribute(fd, kCTFontURLAttribute);
        CFStringRef file = url ? CFURLCopyFileSystemPath(url, kCFURLPOSIXPathStyle) : NULL;
        if (name && family && file) {
            jstring jname = createJavaString(env, name);
            jstring jfamily = createJavaString(env, family);
            jstring jfile = createJavaString(env, file);
            if (jname && jfamily && jfile) {
                (*env)->SetObjectArrayElement(env, result, index++, jname);
                checkAndClearException(env);
                (*env)->SetObjectArrayElement(env, result, index++, jfamily);
                checkAndClearException(env);
                (*env)->SetObjectArrayElement(env, result, index++, jfile);
                checkAndClearException(env);
            }
        }
        if (name) CFRelease(name);
        if (family) CFRelease(family);
        if (url) CFRelease(url);
        if (file) CFRelease(file);
    }
    return index;
}

/*
 * Class:     com_sun_javafx_font_MacFontFinder
 * Method:    getFontData
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_com_sun_javafx_font_MacFontFinder_getFontData
(JNIEnv *env, jclass obj)
{
    /* No caching as this method is only invoked once */
    jclass jStringClass = (*env)->FindClass(env, "java/lang/String");
    if (checkAndClearException(env) || !jStringClass) {
        fprintf(stderr, "getFontData error: JNI exception or jStringClass == NULL");
        return NULL;
    }

    CTFontCollectionRef collection = CTFontCollectionCreateFromAvailableFonts(NULL);
    CFArrayRef fonts = CTFontCollectionCreateMatchingFontDescriptors(collection);
    CFRelease(collection);

    CFIndex count = CFArrayGetCount(fonts);
    jobjectArray result = (*env)->NewObjectArray(env, (count + 2) * 3, jStringClass, NULL);
    if (checkAndClearException(env) || !result) {
        fprintf(stderr, "getFontData error: JNI exception or result == NULL");
        /* out of memory */
        CFRelease(fonts);
        return NULL;
    }

    CFIndex i = 0, j = 0;
    while (i < count) {
        CTFontDescriptorRef fd = (CTFontDescriptorRef)CFArrayGetValueAtIndex(fonts, i++);
        j = addCTFontDescriptor(fd, env, result, j);
    }
    CFRelease(fonts);

    /* Sometimes a font name starting with dot (internal font, e.g. ".Helvetica NeueUI")
     * is returned as a system UI font, but such font is not available in the collection
     * of available fonts. Thus, it is safer to always add the system font manually
     * to the list so JavaFX can find it. If the UI font is added twice it gets
     * handled in Java.
     */
    CTFontRef font = CTFontCreateUIFontForLanguage(kCTFontSystemFontType, 0, NULL);
    CTFontDescriptorRef fd = CTFontCopyFontDescriptor(font);
    j = addCTFontDescriptor(fd, env, result, j);
    CFRelease(fd);
    CFRelease(font);

    /* Also add the EmphasizedSystemFont as it might make the bold version
     * for the system font available to JavaFX.
     */
    font = CTFontCreateUIFontForLanguage(kCTFontEmphasizedSystemFontType, 0, NULL);
    fd = CTFontCopyFontDescriptor(font);
    j = addCTFontDescriptor(fd, env, result, j);
    CFRelease(fd);
    CFRelease(font);

    return result;
}

#endif /* __APPLE__ */
