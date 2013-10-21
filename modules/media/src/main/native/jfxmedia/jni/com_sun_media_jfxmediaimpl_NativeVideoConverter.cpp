/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef __NATIVE_VIDEO_CONVERTER__H
#define __NATIVE_VIDEO_CONVERTER__H

#include <com_sun_media_jfxmedia_control_VideoDataBuffer.h>
#include <com_sun_media_jfxmediaimpl_NativeVideoConverter.h>

using namespace std;

static bool klassMethodsInitialized;

// com.sun.media.jfxmedia.control.VideoDataBuffer
static jmethodID vdbCtorID;                 // (Lcom/sun/media/jfxmedia/control/MediaDataDisposer;Ljava/nio/ByteBuffer;DJIIIILcom/sun/media/jfxmedia/VideoDataBuffer$Format;I[I[I)V
static jmethodID vdbGetBufferID;            // ()Ljava/nio/ByteBuffer;
static jmethodID vdbGetWidthID;             // ()I
static jmethodID vdbGetHeightID;            // ()I
static jmethodID vdbGetEncodedWidthID;      // ()I
static jmethodID vdbGetEncodedHeightID;     // ()I
static jmethodID vdbGetPlaneCountID;        // ()I
static jmethodID vdbGetPlaneOffsetsID;      // ()[I
static jmethodID vdbGetPlaneStridesID;      // ()[I
static jmethodID vdbGetFormatID;            // ()Lcom/sun/media/jfxmedia/control/VideoDataBuffer$Format;
static jmethodID vdbGetFrameNumberID;       // ()J

// com.sun.media.jfxmedia.control.VideoDataBuffer.Format
static jmethodID formatGetNativeTypeID;     // ()I

static void initialize_jni_methods(JNIEnv *env) {
    if (!klassMethodsInitialized) {
        jclass vdbClass = env->FindClass("com/sun/media/jfxmedia/control/VideoDataBuffer");
        jclass formatClass = env->FindClass("com/sun/media/jfxmedia/control/VideoDataBuffer/Format");

        if (!vdbClass) {
            throw "Internal Error: Can't find VideoDataBuffer class";
        }
        if (!formatClass) {
            throw "Internal Error: Can't find VideoDataBuffer.Format class";
        }

        vdbCtorID = env->GetMethodID(vdbClass, "<init>", "(Lcom/sun/media/jfxmedia/control/MediaDataDisposer;Ljava/nio/ByteBuffer;DJIIIILcom/sun/media/jfxmedia/VideoDataBuffer$Format;I[I[I)V");
        if (!vdbCtorID) {
            throw "Internal Error: Can't get VideoDataBuffer constructor.");
        }

        vdbGetBufferID = env->GetMethodID(vdbClass, "getBuffer", "()Ljava/nio/Buffer;");
        if (!vdbGetBufferID) {
            throw "Internal Error: Can't find VideoDataBuffer.getBuffer()";
        }

        vdbGetWidthID = env->GetMethodID(vdbClass, "getWidth", "()I");
        if (!vdbGetWidthID) {
            throw "Internal Error: Can't find VideoDataBuffer.getWidth()";
        }

        vdbGetHeightID = env->GetMethodID(vdbClass, "getHeight", "()I");
        if (!vdbGetHeightID) {
            throw "Internal Error: Can't find VideoDataBuffer.getHeight()";
        }

        vdbGetEncodedWidthID = env->GetMethodID(vdbClass, "getEncodedWidth", "()I");
        if (!vdbGetEncodedWidthID) {
            throw "Internal Error: Can't find VideoDataBuffer.getEncodedWidth()";
        }

        vdbGetEncodedHeightID = env->GetMethodID(vdbClass, "getEncodedHeight", "()I");
        if (!vdbGetEncodedHeightID) {
            throw "Internal Error: Can't find VideoDataBuffer.getEncodedHeight()";
        }

        vdbGetPlaneCountID = env->GetMethodID(vdbClass, "getPlaneCount", "()I");
        if (!vdbGetPlaneCountID) {
            throw "Internal Error: Can't find VideoDataBuffer.getPlaneCount()";
        }

        vdbGetPlaneOffsetsID = env->GetMethodID(vdbClass, "getPlaneOffsets", "()[I");
        if (!vdbGetPlaneOffsetsID) {
            throw "Internal Error: Can't find VideoDataBuffer.getPlaneOffsets()";
        }

        vdbGetPlaneStridesID = env->GetMethodID(vdbClass, "getPlaneStrides", "()[I");
        if (!vdbGetPlaneStridesID) {
            throw "Internal Error: Can't find VideoDataBuffer.getPlaneStrides()";
        }

        vdbGetFormatID = env->GetMethodID(vdbClass, "getFormat", "()Lcom/sun/media/jfxmedia/control/VideoDataBuffer$Format;");
        if (!vdbGetFormatID) {
            throw "Internal Error: Can't find VideoDataBuffer.getFormat()";
        }

        vdbGetFrameNumberID = env->GetMethodID(vdbClass, "getFrameNumber", "()J");
        if (!vdbGetFrameNumberID) {
            throw "Internal Error: Can't find VideoDataBuffer.getFrameNumber()";
        }


        formatGetNativeTypeID = env->GetMethodID(formatClass, "getNativeType", "()I");
        if (!formatGetNativeTypeID) {
            throw "Internal Error: Can't find VideoDataBuffer.Format.getNativeType()";
        }

        klassMethodsInitialized = true;
    }
}

static void throw_internal_error_exception(JNIEnv *env, const char *errorString)
{
    jclass ieeKlass = env->FindClass("java/lang/InternalError");
    if (ieeKlass) {
        // FIXME: can we just pass NULL for the message string?
        env->ThrowNew(ieeKlass, (NULL != errorString) ? errorString : "");
        env->DeleteLocalRef(ieeKlass);
    }
}

static bool exception_check(JNIEnv *env)
{
    if (env->ExceptionOccurred()) {
        env->ExceptionDescribe();
        // Don't clear the exception, let it get back to the JVM
        return true;
    }
    return false;
}


/*
    !!!!!!!!!!!!!!!!!!!!!!!
    WARNING:
      DO NOT ALLOW THESE OBJECTS TO SURVIVE BEYOND THE SCOPE OF A SINGLE JNI CALL!
      They MUST be deleted before you return control to or detach from the JVM.
    !!!!!!!!!!!!!!!!!!!!!!!
*/

class NativeVideoDataBuffer {
public:
    jint width;
    jint height;
    jint encodedWidth;
    jint encodedHeight;
    jint formatType;
    jlong frameNumber;

    unsigned char *buffer;
    jlong bufferSize;

    jint planeCount;
    jint planeOffsets[4];
    jint planeStrides[4];

    unsigned char *planes[4]; // convenience

    NativeVideoDataBuffer();
    NativeVideoDataBuffer(JNIEnv *env, jobject vdb);
    ~NativeVideoDataBuffer();

    // Returns a new VideoDataBuffer using the contents of this buffer
    jobject CreateJavaPeer(JNIEnv *env);
};

NativeVideoDataBuffer::NativeVideoDataBuffer() :
    width(0),
    height(0),
    encodedWidth(0),
    encodedHeight(0),
    formatType(0),
    frameNumber(0),
    buffer(NULL),
    bufferSize(0),
    planeCount(0)
{
    planeOffsets[0] = planeOffsets[1] = planeOffsets[2] = planeOffsets[3] = 0;
    planeStrides[0] = planeStrides[1] = planeStrides[2] = planeStrides[3] = 0;
    planes[0] = planes[1] = planes[2] = planes[3] = NULL;
}

NativeVideoDataBuffer::NativeVideoDataBuffer(JNIEnv *env, jobject vdbObject) :
    width(0),
    height(0),
    encodedWidth(0),
    encodedHeight(0),
    formatType(0),
    frameNumber(0),
    buffer(NULL),
    bufferSize(0),
    planeCount(0)
{
    jobject videoBuffer;
    jobject formatObject;
    jintArray tempArray;

    planeOffsets[0] = planeOffsets[1] = planeOffsets[2] = planeOffsets[3] = 0;
    planeStrides[0] = planeStrides[1] = planeStrides[2] = planeStrides[3] = 0;
    planes[0] = planes[1] = planes[2] = planes[3] = NULL;

    if (!klassMethodsInitialized) {
        initialize_jni_methods(env);
    }

    width = env->CallIntMethod(vdbObject, vdbGetWidthID);
    if (exception_check(env)) {
        throw "Unable to get video image width.";
    }

    height = env->CallIntMethod(vdbObject, vdbGetHeightID);
    if (exception_check(env)) {
        throw "Unable to get video image height.";
    }

    encodedWidth = env->CallIntMethod(vdbObject, vdbGetEncodedWidthID);
    if (exception_check(env)) {
        throw "Unable to get video encoded width.";
    }

    encodedHeight = env->CallIntMethod(vdbObject, vdbGetEncodedHeightID);
    if (exception_check(env)) {
        throw "Unable to get video encoded height.";
    }

    frameNumber = env->CallLongMethod(vdbObject, vdbGetFrameNumberID);
    if (exception_check(env)) {
        throw "Unable to get video frame number.";
    }

    formatObject = env->CallObjectMethod(vdbObject, vdbGetFormatID);
    if (exception_check(env) || !formatObject) {
        throw "Unable to determine source Format.";
    }

    formatType = env->CallIntMethod(formatObject, formatGetNativeTypeID);
    if (exception_check(env)) {
        throw "Can't get source format type.";
    }

    planeCount = env->CallIntMethod(vdbObject, vdbGetPlaneCountID);
    if (exception_check(env) || planeCount < 0 || planeCount > 4) {
        throw "Invalid plane count.";
    }

    tempArray = (jintArray)env->CallObjectMethod(vdbObject, vdbGetPlaneOffsetsID);
    if (exception_check(env)) {
        throw "Unable to get plane offsets.";
    }
    env->GetIntArrayRegion(tempArray, 0, planeCount, planeOffsets);
    if (exception_check(env)) {
        throw "Unable to get plane offsets (elements).";
    }
    env->DeleteLocalRef(tempArray);

    tempArray = (jintArray)env->CallObjectMethod(vdbObject, vdbGetPlaneStridesID);
    if (exception_check(env)) {
        throw "Unable to get plane strides.";
    }
    env->GetIntArrayRegion(tempArray, 0, planeCount, planeStrides);
    if (exception_check(env)) {
        throw "Unable to get plane strides (elements).";
    }
    env->DeleteLocalRef(tempArray);

    videoBuffer = env->CallObjectMethod(vdbObject, vdbGetBufferID);
    if (exception_check(env) || !videoBuffer) {
        throw "Unable to get video buffer.";
    }
    bufferSize = env->GetDirectBufferCapacity(videoBuffer);
    if (!bufferSize) {
        throw "Video buffer is not a direct buffer, cannot process buffer.";
    }

    buffer = env->GetDirectBufferAddress(videoBuffer);
    if (!buffer) {
        throw "Native video buffer is not accessible."
    }

    planes[0] = buffer + planeOffsets[0];
    if (planeCount > 1) {
        planes[1] = buffer + planeOffsets[1];
    }
    if (planeCount > 2) {
        planes[2] = buffer + planeOffsets[2];
    }
    if (planeCount > 3) {
        planes[3] = buffer + planeOffsets[3];
    }
}

NativeVideoDataBuffer::~NativeVideoDataBuffer()
{

}

jobject NativeVideoDataBuffer::CreateJavaPeer(JNIEnv *env)
{
    jobject javaPeer;

    // do some validation first
    if (!buffer) {
        return NULL;
    }
}

/*
 * Class:     com_sun_media_jfxmediaimpl_NativeVideoConverter
 * Method:    nativeConvert
 * Signature: (Lcom/sun/media/jfxmedia/control/VideoDataBuffer;Lcom/sun/media/jfxmedia/control/VideoDataBuffer$Format;)Lcom/sun/media/jfxmedia/control/VideoDataBuffer;
 */
extern "C" JNIEXPORT jobject JNICALL
Java_com_sun_media_jfxmediaimpl_NativeVideoConverter_nativeConvert__Lcom_sun_media_jfxmedia_control_VideoDataBuffer_2Lcom_sun_media_jfxmedia_control_VideoDataBuffer_00024Format_2
    (JNIEnv *env, jclass klass, jobject vdbObject, jobject formatEnum)
{
    jobject destBuffer = 0;
    jint destType;
    NativeVideoDataBuffer *sourceBuf;

    try {
        if (!klassMethodsInitialized) {
            initialize_jni_methods(env);
        }
        sourceBuf = new NativeVideoDataBuffer(env, vdbObject);
    }
    catch (char *errString) {
        throw_internal_error_exception(env, errString);
        return NULL;
    }
    catch (...) {
        throw_internal_error_exception(env, "Internal Error: can't create VideoDataBuffer native peer");
        return NULL;
    }

    // alloc out buffer
    // convert
    // create out buffer

    delete sourceBuf;
    return destBuffer;
}

/*
 * Class:     com_sun_media_jfxmediaimpl_NativeVideoConverter
 * Method:    nativeConvert
 * Signature: (Lcom/sun/media/jfxmedia/control/VideoDataBuffer;Lcom/sun/media/jfxmedia/control/VideoDataBuffer;)V
 */
extern "C" JNIEXPORT void JNICALL
Java_com_sun_media_jfxmediaimpl_NativeVideoConverter_nativeConvert__Lcom_sun_media_jfxmedia_control_VideoDataBuffer_2Lcom_sun_media_jfxmedia_control_VideoDataBuffer_2
    (JNIEnv *env, jclass klass, jobject sourceVDBObject, jobject destVDBObject)
{
    NativeVideoDataBuffer *sourceBuf = NULL;
    NativeVideoDataBuffer *destBuf = NULL;

    try {
        if (!klassMethodsInitialized) {
            initialize_jni_methods(env);
        }
        sourceBuf = new NativeVideoDataBuffer(env, sourceVDBObject);
        destBuf = new NativeVideoDataBuffer(env, destVDBObject);
    }
    catch (char *errString) {
        throw_internal_error_exception(env, errString);
        return;
    }
    catch (...) {
        throw_internal_error_exception(env, "Internal Error: can't create VideoDataBuffer native peer");
        return;
    }


    delete sourceBuf;
    delete destBuf;
}

#endif //__NATIVE_VIDEO_CONVERTER__H
