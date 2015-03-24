/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "NotImplemented.h"

#include "ImageObserver.h"
#include "BitmapImage.h"
#include "ImageSource.h"
#include "SharedBuffer.h"
#include "IntRect.h"
#include "JavaEnv.h"
#include "Logging.h"
#include "MemoryCache.h"

namespace WebCore {

#ifndef NDEBUG
  struct ImageSourceCounter {
    static int created;
    static int deleted;

    ~ImageSourceCounter() {
      if ((created - deleted) != 0) {
        fprintf(stderr, "LEAK: %d image sources (%d - %d)\n",
                (created - deleted), created, deleted);
      }
    }
  };
  int ImageSourceCounter::created = 0;
  int ImageSourceCounter::deleted = 0;
  static ImageSourceCounter sourceCounter;
#endif

ImageSource::ImageSource(AlphaOption alphaOption, GammaAndColorProfileOption gammaAndColorProfileOption)
    : m_alphaOption(alphaOption)
    , m_gammaAndColorProfileOption(gammaAndColorProfileOption)
    , m_dataSize(0)
    , m_frameInfos(1)
{
#ifndef NDEBUG
    ++ImageSourceCounter::created;
#endif

    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID midGetImageDecoder = env->GetMethodID(
        PG_GetGraphicsManagerClass(env),
        "getImageDecoder",
        "()Lcom/sun/webkit/graphics/WCImageDecoder;");
    ASSERT(midGetImageDecoder);

    m_decoder = JLObject(env->CallObjectMethod(
        PL_GetGraphicsManager(env),
        midGetImageDecoder));

    CheckAndClearException(env);
}

ImageSource::~ImageSource()
{
#ifndef NDEBUG
    ++ImageSourceCounter::deleted;
#endif
    clear(true);
}

void ImageSource::setData(SharedBuffer *data, bool allDataReceived)
{
    ASSERT(m_decoder);
    ASSERT(data);
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID midAddImageData = env->GetMethodID(
        PG_GetGraphicsImageDecoderClass(env),
        "addImageData",
        "([B)V");
    ASSERT(midAddImageData);

    size_t dataSize = data->size();
    if (dataSize) {
        jbyte* pData = (jbyte*) data->data();
        ASSERT(pData);

        JLByteArray jArray( m_dataSize < dataSize
            ? env->NewByteArray(dataSize - m_dataSize)
            : NULL);
        if (jArray && !CheckAndClearException(env)) {
            // not OOME in Java
            env->SetByteArrayRegion(jArray, 0, dataSize - m_dataSize, pData + m_dataSize);
            env->CallVoidMethod(m_decoder, midAddImageData, (jbyteArray)jArray);
            if (!CheckAndClearException(env)) {
                m_dataSize = dataSize;
            }
        }
    }

    if (allDataReceived) {
        env->CallVoidMethod(m_decoder, midAddImageData, 0);
        CheckAndClearException(env);
    }
}

bool ImageSource::isSizeAvailable()
{
    ASSERT(m_decoder);
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID midGetImageSize = env->GetMethodID(
        PG_GetGraphicsImageDecoderClass(env),
        "getImageSize",
        "([I)V");
    ASSERT(midGetImageSize);

    JLocalRef<jintArray> jbuf(env->NewIntArray(2));
    CheckAndClearException(env); // OOME
    ASSERT(jbuf);

    env->CallVoidMethod(m_decoder, midGetImageSize, (jintArray)jbuf);
    CheckAndClearException(env);

    jint *buf = (jint*)env->GetPrimitiveArrayCritical(jbuf, 0);
    m_imageSize.setWidth(buf[0]);
    m_imageSize.setHeight(buf[1]);
    env->ReleasePrimitiveArrayCritical(jbuf, buf, 0);

    return m_imageSize.width();
}

size_t ImageSource::frameCount() const
{
    JNIEnv* env = WebCore_GetJavaEnv();
    ASSERT(m_decoder);

    static jmethodID midGetFrameCount = env->GetMethodID(
        PG_GetGraphicsImageDecoderClass(env),
        "getFrameCount",
        "()I");
    ASSERT(midGetFrameCount);

    jint count = env->CallIntMethod(m_decoder, midGetFrameCount);
    CheckAndClearException(env);

    return count < 1
        ? 1
        : count;
}

PassNativeImagePtr ImageSource::createFrameAtIndex(size_t idx, float*/* scale = 0*/)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    ASSERT(m_decoder);

    static jmethodID midGetFrame = env->GetMethodID(
        PG_GetGraphicsImageDecoderClass(env),
        "getFrame",
        "(I[I)Lcom/sun/webkit/graphics/WCImageFrame;");
    ASSERT(midGetFrame);

    JLocalRef<jintArray> jbuf(env->NewIntArray(5));
    CheckAndClearException(env); // OOME
    ASSERT(jbuf);

    JLObject frame(env->CallObjectMethod(
        m_decoder,
        midGetFrame,
        idx,
        (jintArray)jbuf));
    CheckAndClearException(env);

    if (m_frameInfos.size() <= idx)
        m_frameInfos.grow(idx + 1);

    jint *buf = (jint*)env->GetPrimitiveArrayCritical(jbuf, 0);
    m_frameInfos[idx].complete = buf[0];
    m_frameInfos[idx].size.setWidth(buf[1]);
    m_frameInfos[idx].size.setHeight(buf[2]);
    m_frameInfos[idx].duration = buf[3] / 1000.0f;
    m_frameInfos[idx].hasAlpha = buf[4];
    env->ReleasePrimitiveArrayCritical(jbuf, buf, 0);

    return RQRef::create(frame);
}

float ImageSource::frameDurationAtIndex(size_t idx)
{
    ASSERT(idx < m_frameInfos.size());
    return m_frameInfos[idx].duration;
}

IntSize ImageSource::size(ImageOrientationDescription d) const
{
    // The JPEG and TIFF decoders need to be taught how to read EXIF, XMP, or IPTC data.
    if (d.respectImageOrientation() == RespectImageOrientation)
        notImplemented();

    return m_imageSize;
}

IntSize ImageSource::frameSizeAtIndex(
    size_t idx,
    ImageOrientationDescription d) const
{
    // The JPEG and TIFF decoders need to be taught how to read EXIF, XMP, or IPTC data.
    if (d.respectImageOrientation() == RespectImageOrientation)
        notImplemented();

    ASSERT(idx < m_frameInfos.size());
    return m_frameInfos[idx].size;
}

bool ImageSource::frameHasAlphaAtIndex(size_t idx)
{
    ASSERT(idx < m_frameInfos.size());
    return m_frameInfos[idx].hasAlpha;
}

bool ImageSource::frameIsCompleteAtIndex(size_t idx)
{
    ASSERT(idx < m_frameInfos.size());
    return m_frameInfos[idx].complete;
}

unsigned ImageSource::frameBytesAtIndex(size_t idx) const
{
    //utatodo: need support for variable frame size.
    ASSERT(idx < m_frameInfos.size());
    return m_frameInfos[idx].size.width() * m_frameInfos[idx].size.height() * 4;
}

int ImageSource::repetitionCount()
{
    return cAnimationLoopInfinite;
}

void ImageSource::clear(
    bool destroyAll,
    size_t clearBeforeFrame,
    SharedBuffer* data,
    bool allDataReceived)
{
        if (destroyAll) {
                JNIEnv* env = WebCore_GetJavaEnv();
                // [env] could be NULL in case of deallocation static BitmapImage objects
                if (!env)
                        return;

                static jmethodID midDestroy = env->GetMethodID(
                        PG_GetGraphicsImageDecoderClass(env),
                        "destroy",
                        "()V");
                ASSERT(midDestroy);

                env->CallVoidMethod(m_decoder, midDestroy);
                CheckAndClearException(env);
        }

        if (data) {
                setData(data, allDataReceived);
        }
}

bool ImageSource::initialized() const
{
    notImplemented();
    return true;
}

String ImageSource::filenameExtension() const
{
    JNIEnv* env = WebCore_GetJavaEnv();
    ASSERT(m_decoder);

    static jmethodID midGetFileExtention = env->GetMethodID(
        PG_GetGraphicsImageDecoderClass(env),
        "getFilenameExtension",
        "()Ljava/lang/String;");
    ASSERT(midGetFileExtention);

    JLString ext((jstring)env->CallObjectMethod(
        m_decoder,
        midGetFileExtention));
    CheckAndClearException(env);

    return String(env, ext);
}

bool ImageSource::getHotSpot(IntPoint&) const
{
    notImplemented();
    return false;
}

size_t ImageSource::bytesDecodedToDetermineProperties() const
{
    notImplemented();
    return 0;
}

ImageOrientation ImageSource::orientationAtIndex(size_t index) const
{
    // The JPEG and TIFF decoders need to be taught how to read EXIF, XMP, or IPTC data.
    notImplemented();
    return DefaultImageOrientation;
}

} // namespace WebCore
