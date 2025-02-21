/*
 * Copyright (c) 2017, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "config.h"

#include "ImageDecoderJava.h"

#include "NotImplemented.h"
#include "SharedBuffer.h"
#include "SharedBuffer.h"
#include "PlatformJavaClasses.h"
#include "Logging.h"

namespace WebCore {

#ifndef NDEBUG
  struct ImageDecoderCounter {
    static int created;
    static int deleted;

    ~ImageDecoderCounter() {
      if ((created - deleted) != 0) {
          fprintf(stderr, "LEAK: %d image sources (%d - %d)\n",
                (created - deleted), created, deleted);
      }
    }
  };
  int ImageDecoderCounter::created = 0;
  int ImageDecoderCounter::deleted = 0;
  static ImageDecoderCounter sourceCounter;
#endif

ImageDecoderJava::ImageDecoderJava()
{
#ifndef NDEBUG
    ++ImageDecoderCounter::created;
#endif

    JNIEnv* env = WTF::GetJavaEnv();
    if (!env) {
        return;
    }

    static jmethodID midGetImageDecoder = env->GetMethodID(
        PG_GetGraphicsManagerClass(env),
        "getImageDecoder",
        "()Lcom/sun/webkit/graphics/WCImageDecoder;");
    ASSERT(midGetImageDecoder);

    m_nativeDecoder = JLObject(env->CallObjectMethod(
        PL_GetGraphicsManager(env),
        midGetImageDecoder));

    WTF::CheckAndClearException(env);
}

ImageDecoderJava::~ImageDecoderJava()
{
#ifndef NDEBUG
    ++ImageDecoderCounter::deleted;
#endif
    JNIEnv* env = WTF::GetJavaEnv();
    // [env] could be NULL in case of deallocation static BitmapImage objects
    if (!env || !m_nativeDecoder) {
        return;
    }

    static jmethodID midDestroy = env->GetMethodID(
            PG_GetGraphicsImageDecoderClass(env),
            "destroy",
            "()V");
    ASSERT(midDestroy);

    env->CallVoidMethod(m_nativeDecoder, midDestroy);
    WTF::CheckAndClearException(env);
}

void ImageDecoderJava::setData(const FragmentedSharedBuffer& data, bool allDataReceived)
{
    JNIEnv* env = WTF::GetJavaEnv();
    if (!env || !m_nativeDecoder) {
        return;
    }

    static jmethodID midAddImageData = env->GetMethodID(
        PG_GetGraphicsImageDecoderClass(env),
        "addImageData",
        "([B)V");
    ASSERT(midAddImageData);

    while (m_receivedDataSize < data.size()) {
        const auto& someData = data.getSomeData(m_receivedDataSize);
        unsigned length = someData.size();
        JLByteArray jArray(env->NewByteArray(length));
        if (jArray && !WTF::CheckAndClearException(env)) {
            // not OOME in Java
            env->SetByteArrayRegion(jArray, 0, length, (const jbyte*)someData.span().data());
            env->CallVoidMethod(m_nativeDecoder, midAddImageData, (jbyteArray)jArray);
            WTF::CheckAndClearException(env);
        }
        m_receivedDataSize += length;
    }

    if (allDataReceived) {
        m_isAllDataReceived = true;
        env->CallVoidMethod(m_nativeDecoder, midAddImageData, 0);
        WTF::CheckAndClearException(env);
    }
}

bool ImageDecoderJava::isSizeAvailable() const
{
    JNIEnv* env = WTF::GetJavaEnv();
    if (!env || !m_nativeDecoder) {
        return { };
    }

    static jmethodID midGetImageSize = env->GetMethodID(
        PG_GetGraphicsImageDecoderClass(env),
        "getImageSize",
        "()[I");
    ASSERT(midGetImageSize);

    JLocalRef<jintArray> jsize((jintArray)env->CallObjectMethod(
                m_nativeDecoder, midGetImageSize));
    WTF::CheckAndClearException(env);

    jint* size = (jint*)env->GetPrimitiveArrayCritical((jintArray)jsize, 0);
    m_size.setWidth(size[0]);
    m_size.setHeight(size[1]);
    env->ReleasePrimitiveArrayCritical(jsize, size, 0);

    return m_size.width();
}

size_t ImageDecoderJava::frameCount() const
{
    JNIEnv* env = WTF::GetJavaEnv();
    if (!env || !m_nativeDecoder) {
        return { };
    }

    static jmethodID midGetFrameCount = env->GetMethodID(
        PG_GetGraphicsImageDecoderClass(env),
        "getFrameCount",
        "()I");
    ASSERT(midGetFrameCount);

    jint count = env->CallIntMethod(m_nativeDecoder, midGetFrameCount);
    WTF::CheckAndClearException(env);

    return count < 1
        ? 1
        : count;
}

PlatformImagePtr ImageDecoderJava::createFrameImageAtIndex(size_t idx, SubsamplingLevel, const DecodingOptions&)
{
    JNIEnv* env = WTF::GetJavaEnv();
    if (!env || !m_nativeDecoder) {
        return { };
    }

    static jmethodID midGetFrame = env->GetMethodID(
        PG_GetGraphicsImageDecoderClass(env),
        "getFrame",
        "(I)Lcom/sun/webkit/graphics/WCImageFrame;");
    ASSERT(midGetFrame);

    JLObject frame(env->CallObjectMethod(
        m_nativeDecoder,
        midGetFrame,
        idx));
    WTF::CheckAndClearException(env);

    if(!frame)
        return nullptr;

    static jmethodID midGetSize = env->GetMethodID(
        PG_GetImageFrameClass(env),
        "getSize",
        "()[I");
    ASSERT(midGetSize);
    JLocalRef<jintArray> jsize((jintArray)env->CallObjectMethod(
                        jobject(frame),
                        midGetSize));
    if (!jsize) {
        return ImageJava::create(RQRef::create(frame), nullptr, 0, 0);
    }

    jint* size = (jint*)env->GetPrimitiveArrayCritical((jintArray)jsize, 0);
    IntSize frameSize(size[0], size[1]);
    env->ReleasePrimitiveArrayCritical(jsize, size, 0);

    return ImageJava::create(RQRef::create(frame), nullptr, frameSize.width(), frameSize.height());
}

WTF::Seconds ImageDecoderJava::frameDurationAtIndex(size_t idx) const
{
    JNIEnv* env = WTF::GetJavaEnv();
    if (!env || !m_nativeDecoder) {
        return { };
    }
    static jmethodID midGetDuration = env->GetMethodID(
        PG_GetGraphicsImageDecoderClass(env),
        "getFrameDuration",
        "(I)I");
    ASSERT(midGetDuration);
    jint duration = env->CallIntMethod(
                        m_nativeDecoder,
                        midGetDuration,
                        idx);
    return WTF::Seconds::fromMilliseconds(duration);
}

EncodedDataStatus ImageDecoderJava::encodedDataStatus() const
{
    if (isSizeAvailable())
        m_encodedDataStatus = EncodedDataStatus::SizeAvailable;

    return m_encodedDataStatus;
}

IntSize ImageDecoderJava::size() const
{
    return m_size;
}

IntSize ImageDecoderJava::frameSizeAtIndex(size_t idx, SubsamplingLevel) const
{
    JNIEnv* env = WTF::GetJavaEnv();
    if (!env || !m_nativeDecoder) {
        return { };
    }
    static jmethodID midGetFrameSize = env->GetMethodID(
        PG_GetGraphicsImageDecoderClass(env),
        "getFrameSize",
        "(I)[I");
    ASSERT(midGetFrameSize);
    JLocalRef<jintArray> jsize((jintArray)env->CallObjectMethod(
                        m_nativeDecoder,
                        midGetFrameSize,
                        idx));
    if (!jsize) {
        return m_size;
    }

    jint* size = (jint*)env->GetPrimitiveArrayCritical((jintArray)jsize, 0);
    IntSize frameSize(size[0], size[1]);
    env->ReleasePrimitiveArrayCritical(jsize, size, 0);

    return frameSize;
}

bool ImageDecoderJava::frameAllowSubsamplingAtIndex(size_t) const
{
    notImplemented();
    return true;
}

bool ImageDecoderJava::frameHasAlphaAtIndex(size_t) const
{
    // FIXME-java: Read it from ImageMetadata
    return true;
}

bool ImageDecoderJava::frameIsCompleteAtIndex(size_t idx) const
{
    JNIEnv* env = WTF::GetJavaEnv();
    if (!env || !m_nativeDecoder) {
        return { };
    }
    static jmethodID midGetFrameIsComplete = env->GetMethodID(
        PG_GetGraphicsImageDecoderClass(env),
        "getFrameCompleteStatus",
        "(I)Z");
    ASSERT(midGetFrameIsComplete);
    return (bool)env->CallBooleanMethod(m_nativeDecoder,
            midGetFrameIsComplete,
            idx);
}

unsigned ImageDecoderJava::frameBytesAtIndex(size_t idx, SubsamplingLevel samplingLevel) const
{
    auto frameSize = frameSizeAtIndex(idx, samplingLevel);
    return (frameSize.area() * 4);
}

RepetitionCount ImageDecoderJava::repetitionCount() const
{
    return RepetitionCountInfinite;
}

String ImageDecoderJava::filenameExtension() const
{
    JNIEnv* env = WTF::GetJavaEnv();
    if (!env || !m_nativeDecoder) {
        return { };
    }

    static jmethodID midGetFileExtention = env->GetMethodID(
        PG_GetGraphicsImageDecoderClass(env),
        "getFilenameExtension",
        "()Ljava/lang/String;");
    ASSERT(midGetFileExtention);

    JLString ext((jstring)env->CallObjectMethod(
        m_nativeDecoder,
        midGetFileExtention));
    WTF::CheckAndClearException(env);

    return String(env, ext);
}

std::optional<IntPoint> ImageDecoderJava::hotSpot() const
{
    notImplemented();
    return { };
}

size_t ImageDecoderJava::bytesDecodedToDetermineProperties() const
{
    // Set to match value used for CoreGraphics.
    return 13088;
}
} // namespace WebCore
