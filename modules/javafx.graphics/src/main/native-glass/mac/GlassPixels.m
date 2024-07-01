/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

#import "common.h"
#import "com_sun_glass_ui_Pixels_Format.h"
#import "com_sun_glass_ui_mac_MacPixels.h"

#import "GlassMacros.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

/*
 * Class:     com_sun_glass_ui_mac_MacPixels
 * Method:    _initIDs
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_mac_MacPixels__1initIDs
(JNIEnv *env, jclass jClass)
{
    LOG("Java_com_sun_glass_ui_mac_MacPixels__1initIDs");

    if (jPixelsAttachData == NULL)
    {
        jPixelsAttachData =  (*env)->GetMethodID(env, jClass, "attachData", "(J)V");
    }

    // http://developer.apple.com/library/mac/#documentation/GraphicsImaging/Conceptual/OpenGL-MacProgGuide/opengl_designstrategies/opengl_designstrategies.html%23//apple_ref/doc/uid/TP40001987-CH2-SW17
    // GL_BGRA + GL_UNSIGNED_INT_8_8_8_8_REV == ARGB (big) == BGRA (little)
    return com_sun_glass_ui_Pixels_Format_BYTE_BGRA_PRE;
}

NSImage* getImage(u_int8_t* data, int jWidth, int jHeight, int jOffset) {
    NSImage* image = NULL;
    CGImageRef cgImage = NULL;
    if ((data != NULL) && (jWidth > 0) && (jHeight > 0)) {
        CGColorSpaceRef colorSpace = CGColorSpaceCreateWithName(kCGColorSpaceSRGB);
        {
            size_t width = (size_t) jWidth;
            size_t height = (size_t) jHeight;

            CGDataProviderRef provider = CGDataProviderCreateWithData(NULL,
                                            data + jOffset, width * height * 4, NULL);
            if (provider != NULL) {
                cgImage = CGImageCreate(width, height, 8, 32, 4 * width, colorSpace,
                        kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Little,
                        provider, NULL, 1, kCGRenderingIntentDefault);
                CGDataProviderRelease(provider);
            }
        }
        CGColorSpaceRelease(colorSpace);
        if (cgImage != NULL) {
            image = [[NSImage alloc] initWithCGImage : cgImage size : NSMakeSize(jWidth, jHeight)];
            CGImageRelease(cgImage);
        } else {
            image = nil;
        }
    }
    return image;
}

void attachCommon
(JNIEnv *env, jobject jPixels, jlong jPtr, jint jWidth, jint jHeight, jobject jBuffer, jbyteArray jArray, jint jOffset)
{
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    {
        u_int8_t *data = NULL;
        if (jArray != NULL)
        {
            jboolean isCopy = JNI_FALSE;
            data = (*env)->GetPrimitiveArrayCritical(env, jArray, &isCopy);
        }
        else
        {
            data = (*env)->GetDirectBufferAddress(env, jBuffer);
        }

        NSImage **nsImage = (NSImage**)jlong_to_ptr(jPtr);
        *nsImage = getImage(data, jWidth, jHeight, jOffset);

        if (jArray != NULL)
        {
            (*env)->ReleasePrimitiveArrayCritical(env, jArray, data, JNI_ABORT);
        }
    }
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacPixels
 * Method:    _attachInt
 * Signature: (JIILjava/nio/IntBuffer;[II)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacPixels__1attachInt
(JNIEnv *env, jobject jPixels, jlong jPtr, jint jWidth, jint jHeight, jobject jBuffer, jintArray jArray, jint jOffset)
{
    LOG("Java_com_sun_glass_ui_mac_MacPixels__1attachInt");

    if (!jPtr) return;
    if (!(jArray || jBuffer)) return;
    if (jOffset < 0) return;
    if (jWidth <= 0 || jHeight <= 0) return;

    if (jOffset > (INT_MAX / 4)) {
        return;
    }

    if (jWidth > (((INT_MAX - 4 * jOffset) / 4) / jHeight))
    {
        return;
    }

    jsize numElem;
    if (jArray != NULL) {
        numElem = (*env)->GetArrayLength(env, jArray);
    } else {
        numElem = (*env)->GetDirectBufferCapacity(env, jBuffer);
    }

    if ((jWidth * jHeight + jOffset) > numElem)
    {
        return;
    }

    attachCommon(env, jPixels, jPtr, jWidth, jHeight, jBuffer, jArray, 4 * jOffset);
}

/*
 * Class:     com_sun_glass_ui_mac_MacPixels
 * Method:    _attachByte
 * Signature: (JIILjava/nio/ByteBuffer;[BI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacPixels__1attachByte
(JNIEnv *env, jobject jPixels, jlong jPtr, jint jWidth, jint jHeight, jobject jBuffer, jbyteArray jArray, jint jOffset)
{
    LOG("Java_com_sun_glass_ui_mac_MacPixels__1attachByte");

    if (!jPtr) return;
    if (!(jArray || jBuffer)) return;
    if (jOffset < 0) return;
    if (jWidth <= 0 || jHeight <= 0) return;

    if (jWidth > (((INT_MAX - jOffset) / 4) / jHeight))
    {
        return;
    }

    jsize numElem;
    if (jArray != NULL) {
        numElem = (*env)->GetArrayLength(env, jArray);
    } else {
        numElem = (*env)->GetDirectBufferCapacity(env, jBuffer);
    }

    if ((4 * jWidth * jHeight + jOffset) > numElem)
    {
        return;
    }

    attachCommon(env, jPixels, jPtr, jWidth, jHeight, jBuffer, jArray, jOffset);
}
