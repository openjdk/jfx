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


jstring createJavaString(JNIEnv *env, CFStringRef stringRef)
{
    CFIndex length = CFStringGetLength(stringRef);
    UniChar buffer[length];
    CFStringGetCharacters(stringRef, CFRangeMake(0, length), buffer);
    return (*env)->NewString(env, (jchar *)buffer, length);
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
    if (jStringClass == NULL) return NULL;

    CTFontCollectionRef collection = CTFontCollectionCreateFromAvailableFonts(NULL);
    CFArrayRef fonts = CTFontCollectionCreateMatchingFontDescriptors(collection);
    CFRelease(collection);

#if TARGET_OS_IPHONE /* iOS */
    /* Sometimes a font name starting with dot (internal font, e.g. ".Helvetica NeueUI")
     * is returned as a system UI font, but such font is not available in the colection
     * of available fonts, thus this font has to be added to the array, otherwise
     * first font from the array is returned if default font is requested from JFX
     */
    CTFontRef font = CTFontCreateUIFontForLanguage(kCTFontSystemFontType, 0, NULL);
    CFStringRef fullName = CTFontCopyFullName(font);
    CFStringRef dot = CFSTR(".");
    CFComparisonResult res = CFStringCompareWithOptions(fullName, dot, CFRangeMake(0, 1), 0);
    // if font name starts with dot
    if (res == kCFCompareEqualTo) {
        CTFontDescriptorRef fd = CTFontCopyFontDescriptor(font);
        CFMutableArrayRef fontsMutableArray =
            CFArrayCreateMutableCopy(kCFAllocatorDefault, CFArrayGetCount(fonts) + 1, fonts);
        CFArrayAppendValue(fontsMutableArray, fd);
        CFRelease(fd);
        CFRelease(fonts);
        fonts = fontsMutableArray;
    }
    CFRelease(font);
    CFRelease(fullName);
#endif

    CFIndex count = CFArrayGetCount(fonts);
    jobjectArray result = (*env)->NewObjectArray(env, count * 3, jStringClass, NULL);
    if (result == NULL) {
        /* out of memory */
        CFRelease(fonts);
        return NULL;
    }

    CFIndex i = 0, j = 0;
    while (i < count) {
        CTFontDescriptorRef fd = (CTFontDescriptorRef)CFArrayGetValueAtIndex(fonts, i++);
        if (fd) {
            CFStringRef name = CTFontDescriptorCopyAttribute(fd, kCTFontDisplayNameAttribute);
            CFStringRef family = CTFontDescriptorCopyAttribute(fd, kCTFontFamilyNameAttribute);
            CFURLRef url = CTFontDescriptorCopyAttribute(fd, kCTFontURLAttribute);
            CFStringRef file = url ? CFURLCopyFileSystemPath(url, kCFURLPOSIXPathStyle) : NULL;
            if (name && family && file) {
                jstring jname = createJavaString(env, name);
                jstring jfamily = createJavaString(env, family);
                jstring jfile = createJavaString(env, file);
                (*env)->SetObjectArrayElement(env, result, j++, jname);
                (*env)->SetObjectArrayElement(env, result, j++, jfamily);
                (*env)->SetObjectArrayElement(env, result, j++, jfile);
            }
            if (name) CFRelease(name);
            if (family) CFRelease(family);
            if (url) CFRelease(url);
            if (file) CFRelease(file);
        }
    }
    CFRelease(fonts);
    return result;
}

#endif /* __APPLE__ */
