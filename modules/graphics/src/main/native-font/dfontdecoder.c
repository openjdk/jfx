/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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
#include <com_sun_javafx_font_DFontDecoder.h>

#import <CoreFoundation/CoreFoundation.h>

#if TARGET_OS_IPHONE
#import <CoreGraphics/CoreGraphics.h>
#import <CoreText/CoreText.h>
#else
#import <ApplicationServices/ApplicationServices.h>
#endif


JNIEXPORT jlong JNICALL Java_com_sun_javafx_font_DFontDecoder_createCTFont
(JNIEnv *env, jclass clazz, jstring fontName)
{
    if (fontName == NULL) return 0l;
    CFIndex numChars = (*env)->GetStringLength(env, fontName);
    const jchar *fontBuffer = (*env)->GetStringChars(env, fontName, NULL);
    if (fontBuffer == NULL) return 0l;
    CFStringRef fontNameRef = CFStringCreateWithCharacters(kCFAllocatorDefault,
                                                           fontBuffer,
                                                           numChars);
    (*env)->ReleaseStringChars(env, fontName, fontBuffer);

    CTFontDescriptorRef descriptor = NULL;
    CTFontCollectionRef collection = CTFontCollectionCreateFromAvailableFonts(NULL);
    CFArrayRef fonts = CTFontCollectionCreateMatchingFontDescriptors(collection);
    CFRelease(collection);
    CFIndex count = CFArrayGetCount(fonts);
    CFIndex i = 0;
    while (i < count && descriptor == NULL) {
        CTFontDescriptorRef fd = (CTFontDescriptorRef)CFArrayGetValueAtIndex(fonts, i++);
        if (fd) {
            CFStringRef fdNameRef = CTFontDescriptorCopyAttribute(fd, kCTFontDisplayNameAttribute);
            if (fdNameRef) {
                if (CFStringCompare(fdNameRef, fontNameRef, 0) == kCFCompareEqualTo) {
                    descriptor = fd;
                }
                CFRelease(fdNameRef);
            }
        }
    }

    CTFontRef fontRef = NULL;
    if (descriptor) {
        fontRef = CTFontCreateWithFontDescriptor(descriptor, 0, NULL);
    }
    CFRelease(fonts);
    CFRelease(fontNameRef);
    return (jlong)fontRef;
}

JNIEXPORT void JNICALL Java_com_sun_javafx_font_DFontDecoder_releaseCTFont
(JNIEnv *env, jclass clazz, jlong fontPtr)
{
    CTFontRef fontRef = (CTFontRef)fontPtr;
    CFRelease(fontRef);
}

JNIEXPORT jint JNICALL Java_com_sun_javafx_font_DFontDecoder_getCTFontFormat
(JNIEnv *env, jclass clazz, jlong fontPtr)
{
    CTFontRef fontRef = (CTFontRef)fontPtr;
    CFNumberRef formatRef = CTFontCopyAttribute(fontRef, kCTFontFormatAttribute);
    CTFontFormat formatValue;
    CFNumberGetValue(formatRef, kCFNumberIntType, &formatValue);
    CFRelease(formatRef);
    switch (formatValue) {
        case kCTFontFormatOpenTypePostScript:
            return 0x4f54544f; // 'otto' - OpenType CFF font
        case kCTFontFormatOpenTypeTrueType:
            return 0x00010000; // 'v1tt' - MS TrueType font
        case kCTFontFormatTrueType:
            return 0x74727565; // 'true' - Mac TrueType font
        case kCTFontFormatPostScript:
        case kCTFontFormatBitmap:
        case kCTFontFormatUnrecognized:
        default:
            return 0;
    }
}

JNIEXPORT jintArray JNICALL Java_com_sun_javafx_font_DFontDecoder_getCTFontTags
(JNIEnv *env, jclass clazz, jlong fontPtr)
{
    CTFontRef fontRef = (CTFontRef)fontPtr;
    CTFontTableOptions  options = kCTFontTableOptionNoOptions;
    CFArrayRef tags = CTFontCopyAvailableTables(fontRef, options);
    CFIndex count = CFArrayGetCount(tags);
    jintArray intArrObj = (*env)->NewIntArray(env, count);
    if (intArrObj == NULL) {
        CFRelease(tags);
        return intArrObj;
    }
    jint* data = (*env)->GetIntArrayElements(env, intArrObj, NULL);
    if (data == NULL) {
        CFRelease(tags);
        return intArrObj;
    }
    int i;
    for (i = 0; i < count; i++) {
        data[i] = (uintptr_t)CFArrayGetValueAtIndex(tags, i);
    }
    CFRelease(tags);
    (*env)->ReleaseIntArrayElements(env, intArrObj, data, (jint)0);
    return intArrObj;
}

JNIEXPORT jbyteArray JNICALL Java_com_sun_javafx_font_DFontDecoder_getCTFontTable
(JNIEnv *env, jclass clazz, jlong fontPtr, jint tag)
{
    CTFontRef fontRef = (CTFontRef)fontPtr;
    CTFontTableTag cttag = (CTFontTableTag)tag;
    CTFontTableOptions options = kCTFontTableOptionNoOptions;
    CFDataRef tableData = CTFontCopyTable(fontRef, cttag, options);
    CFIndex length = CFDataGetLength(tableData);
    jbyteArray byteArrObj = (*env)->NewByteArray(env, length);
    if (byteArrObj == NULL) {
        CFRelease(tableData);
        return byteArrObj;
    }
    jbyte* data = (*env)->GetByteArrayElements(env, byteArrObj, NULL);
    if (data == NULL) {
        CFRelease(tableData);
        return byteArrObj;
    }
    const UInt8 *src = CFDataGetBytePtr(tableData);
    memcpy(data, src, length);
    (*env)->ReleaseByteArrayElements(env, byteArrObj, data, 0);
    CFRelease(tableData);
    return byteArrObj;
}

#endif /* TARGET_OS_MAC */
#endif /* __APPLE__ */
