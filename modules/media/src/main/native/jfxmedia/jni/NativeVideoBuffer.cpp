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

#include "com_sun_media_jfxmedia_control_VideoDataBuffer.h"
#include "com_sun_media_jfxmedia_control_VideoFormat_FormatTypes.h"
#include "com_sun_media_jfxmediaimpl_NativeVideoBuffer.h"

#include <PipelineManagement/VideoFrame.h>
#include "JniUtils.h"

/*
 * Class:     com_sun_media_jfxmediaimpl_NativeVideoBuffer
 * Method:    nativeDisposeBuffer
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_NativeVideoBuffer_nativeDisposeBuffer
    (JNIEnv *env, jclass klass, jlong nativeHandle)
{
    CVideoFrame *frame = (CVideoFrame*)jlong_to_ptr(nativeHandle);
    if (frame) {
        delete frame;
    }
}

/*
 * Class:     com_sun_media_jfxmediaimpl_NativeVideoBuffer
 * Method:    nativeGetTimestamp
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_com_sun_media_jfxmediaimpl_NativeVideoBuffer_nativeGetTimestamp
    (JNIEnv *env, jobject obj, jlong nativeHandle)
{
    CVideoFrame *frame = (CVideoFrame*)jlong_to_ptr(nativeHandle);
    if (frame) {
        return frame->GetTime();
    }
    return 0.0;
}

/*
 * Class:     com_sun_media_jfxmediaimpl_NativeVideoBuffer
 * Method:    nativeGetBuffer
 * Signature: (J)Ljava/nio/ByteBuffer;
 *
 * WARNING: This method will create a new ByteBuffer object, you should cache this object to avoid multiple allocations.
 */
JNIEXPORT jobject JNICALL Java_com_sun_media_jfxmediaimpl_NativeVideoBuffer_nativeGetBuffer
    (JNIEnv *env, jobject obj, jlong nativeHandle)
{
    CVideoFrame *frame = (CVideoFrame*)jlong_to_ptr(nativeHandle);
    if (frame) {
        void *dataPtr = frame->GetData();
        jlong capacity = (jlong)frame->GetSize();
        return env->NewDirectByteBuffer(dataPtr, capacity);
    }
    return NULL;
}

/*
 * Class:     com_sun_media_jfxmediaimpl_NativeVideoBuffer
 * Method:    nativeGetFrameNumber
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_media_jfxmediaimpl_NativeVideoBuffer_nativeGetFrameNumber
    (JNIEnv *env, jobject obj, jlong nativeHandle)
{
    CVideoFrame *frame = (CVideoFrame*)jlong_to_ptr(nativeHandle);
    if (frame) {
        return (jlong)frame->GetFrameNumber();
    }
    return 0;
}

/*
 * Class:     com_sun_media_jfxmediaimpl_NativeVideoBuffer
 * Method:    nativeGetWidth
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_NativeVideoBuffer_nativeGetWidth
    (JNIEnv *env, jobject obj, jlong nativeHandle)
{
    CVideoFrame *frame = (CVideoFrame*)jlong_to_ptr(nativeHandle);
    if (frame) {
        return frame->GetWidth();
    }
    return 0;
}

/*
 * Class:     com_sun_media_jfxmediaimpl_NativeVideoBuffer
 * Method:    nativeGetHeight
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_NativeVideoBuffer_nativeGetHeight
    (JNIEnv *env, jobject obj, jlong nativeHandle)
{
    CVideoFrame *frame = (CVideoFrame*)jlong_to_ptr(nativeHandle);
    if (frame) {
        return frame->GetHeight();
    }
    return 0;
}

/*
 * Class:     com_sun_media_jfxmediaimpl_NativeVideoBuffer
 * Method:    nativeGetEncodedWidth
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_NativeVideoBuffer_nativeGetEncodedWidth
    (JNIEnv *env, jobject obj, jlong nativeHandle)
{
    CVideoFrame *frame = (CVideoFrame*)jlong_to_ptr(nativeHandle);
    if (frame) {
        return frame->GetEncodedWidth();
    }
    return 0;
}

/*
 * Class:     com_sun_media_jfxmediaimpl_NativeVideoBuffer
 * Method:    nativeGetEncodedHeight
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_NativeVideoBuffer_nativeGetEncodedHeight
    (JNIEnv *env, jobject obj, jlong nativeHandle)
{
    CVideoFrame *frame = (CVideoFrame*)jlong_to_ptr(nativeHandle);
    if (frame) {
        return frame->GetEncodedHeight();
    }
    return 0;
}

/*
 * Class:     com_sun_media_jfxmediaimpl_NativeVideoBuffer
 * Method:    nativeGetFormat
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_NativeVideoBuffer_nativeGetFormat
    (JNIEnv *env, jobject obj, jlong nativeHandle)
{
    CVideoFrame *frame = (CVideoFrame*)jlong_to_ptr(nativeHandle);
    if (frame) {
        // CVideoFrame types now match Java VideoFormat native types, so just pass it along
        return (jint)frame->GetType();
    }
    return 0;
}

/*
 * Class:     com_sun_media_jfxmediaimpl_NativeVideoBuffer
 * Method:    nativeHasAlpha
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_media_jfxmediaimpl_NativeVideoBuffer_nativeHasAlpha
    (JNIEnv *env, jobject obj, jlong nativeHandle)
{
    CVideoFrame *frame = (CVideoFrame*)jlong_to_ptr(nativeHandle);
    if (frame) {
        return frame->HasAlpha();
    }
    return JNI_FALSE;
}

/*
 * Class:     com_sun_media_jfxmediaimpl_NativeVideoBuffer
 * Method:    nativeGetPlaneCount
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_NativeVideoBuffer_nativeGetPlaneCount
    (JNIEnv *env, jobject obj, jlong nativeHandle)
{
    CVideoFrame *frame = (CVideoFrame*)jlong_to_ptr(nativeHandle);
    if (frame) {
        return frame->GetPlaneCount();
    }
    return 0;
}

/*
 * Class:     com_sun_media_jfxmediaimpl_NativeVideoBuffer
 * Method:    nativeGetPlaneOffsets
 * Signature: (J)[I
 */
JNIEXPORT jintArray JNICALL Java_com_sun_media_jfxmediaimpl_NativeVideoBuffer_nativeGetPlaneOffsets
    (JNIEnv *env, jobject obj, jlong nativeHandle)
{
    CVideoFrame *frame = (CVideoFrame*)jlong_to_ptr(nativeHandle);
    if (frame) {
        jint count = frame->GetPlaneCount();
        jintArray offsets = env->NewIntArray(count);
        jint *offsetArray = new jint[count];

        for (int ii=0; ii < count; ii++) {
            offsetArray[ii] = frame->GetOffsetForPlane(ii);
        }

        env->SetIntArrayRegion(offsets, 0, count, offsetArray);
        delete [] offsetArray;

        return offsets;
    }
    return NULL;
}

/*
 * Class:     com_sun_media_jfxmediaimpl_NativeVideoBuffer
 * Method:    nativeGetPlaneStrides
 * Signature: (J)[I
 */
JNIEXPORT jintArray JNICALL Java_com_sun_media_jfxmediaimpl_NativeVideoBuffer_nativeGetPlaneStrides
    (JNIEnv *env, jobject obj, jlong nativeHandle)
{
    CVideoFrame *frame = (CVideoFrame*)jlong_to_ptr(nativeHandle);
    if (frame) {
        jint count = (jint)frame->GetPlaneCount();
        // Sanity check plane count, never more than four or less than 1
        if (count > 4 || count < 1) {
            return NULL;
        }

        jintArray strides = env->NewIntArray(count);
        jint *strideArray = new jint[count];

        for (int ii=0; ii < count; ii++) {
            strideArray[ii] = frame->GetStrideForPlane(ii);
        }

        env->SetIntArrayRegion(strides, 0, count, strideArray);
        delete [] strideArray;

        return strides;
    }
    return NULL;
}

/*
 * Class:     com_sun_media_jfxmediaimpl_NativeVideoBuffer
 * Method:    nativeConvertToFormat
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_media_jfxmediaimpl_NativeVideoBuffer_nativeConvertToFormat
    (JNIEnv *env, jobject obj, jlong nativeHandle, jint newFormat)
{
    CVideoFrame *frame = (CVideoFrame*)jlong_to_ptr(nativeHandle);
    if (frame) {
        return ptr_to_jlong(frame->ConvertToFormat((CVideoFrame::FrameType)newFormat));
    }
    return 0;
}

/*
 * Class:     com_sun_media_jfxmediaimpl_NativeVideoBuffer
 * Method:    nativeSetDirty
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_NativeVideoBuffer_nativeSetDirty
    (JNIEnv *env, jobject obj, jlong nativeHandle)
{
    CVideoFrame *frame = (CVideoFrame*)jlong_to_ptr(nativeHandle);
    if (frame) {
        frame->SetFrameDirty(true);
    }
}
