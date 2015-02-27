/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "NotImplemented.h"

#include "BitmapImage.h"
#include "GraphicsContext.h"
#include "ImageObserver.h"
#include "JavaEnv.h"
#include "GraphicsContextJava.h"

#include "PlatformContextJava.h"
#include "RenderingQueue.h"
#include "SharedBuffer.h"

#include "Logging.h"

namespace WebCore {

bool FrameData::clear(bool clearMetadata)
{
    if (clearMetadata)
        m_haveMetadata = false;

    if (m_frame) {
#if USE(IMAGEIO)
        JNIEnv* env = WebCore_GetJavaEnv();
        static jmethodID midDestroyDecodedData = env->GetMethodID(
                JLClass(env->GetObjectClass(*m_frame)),
                "destroyDecodedData",
                "()V");
        ASSERT(midDestroyDecodedData);
        env->CallVoidMethod(*m_frame, midDestroyDecodedData);
        CheckAndClearException(env);
#endif
        m_frame = 0;
        return true;
    }
    return false;
}

void BitmapImage::invalidatePlatformData()
{
}

void BitmapImage::checkForSolidColor()
{
    notImplemented();
    m_checkedForSolidColor = true;
}

void BitmapImage::draw(GraphicsContext *gc, const FloatRect &dstRect, const FloatRect &srcRect,
                       ColorSpace cs, CompositeOperator co, BlendMode bm, ImageOrientationDescription id) // todo tav new param
{
    Image::drawImage(gc, dstRect, srcRect, cs, co, bm);
    startAnimation();
}

PassRefPtr<Image> BitmapImage::createFromName(const char* name)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    if (!env)
        return NULL;

    RefPtr<BitmapImage> img(create());

#if USE(IMAGEIO)
    static jmethodID midLoadFromResource = env->GetMethodID(
        PG_GetGraphicsImageDecoderClass(env),
        "loadFromResource",
        "(Ljava/lang/String;)V");
    ASSERT(midLoadFromResource);

    env->CallVoidMethod(
        img->m_source.m_decoder,
        midLoadFromResource,
        (jstring)String(name).toJavaString(env));
    CheckAndClearException(env);

    // we have to make this call in order to initialize
    // internal flags that indicates the image readiness
    bool isSizeAvailable = img->isSizeAvailable();

    // Absence if the image size indicates some problem with
    // the availability of the resource referred by the name.
    // It should never happen if resources are set up correctly,
    // however it does happen after OOME
//    ASSERT(isSizeAvailable);
#else
    static jmethodID midLoadFromResource = env->GetMethodID(
       PG_GetGraphicsManagerClass(env),
       "fwkLoadFromResource",
       "(Ljava/lang/String;J)V");
    ASSERT(midLoadFromResource);

    RefPtr<SharedBuffer> dataBuffer(SharedBuffer::create());
    JLString resourceName(String(name).toJavaString(env));
    ASSERT(resourceName);

    env->CallVoidMethod(
        PL_GetGraphicsManager(env),
        midLoadFromResource,
        (jstring)resourceName,
        ptr_to_jlong(dataBuffer.get()));
    CheckAndClearException(env);
    //From the upper call we got a callback [Java_com_sun_webkit_graphics_WCGraphicsManager_append]
    //that fills the buffer.
    img->setData(dataBuffer, true);
#endif

    return img;
}


} // namespace WebCore

using namespace WebCore;
extern "C" {

JNIEXPORT void JNICALL Java_com_sun_webkit_graphics_WCGraphicsManager_append
    (JNIEnv *env, jclass cls, jlong sharedBufferPtr, jbyteArray jbits, jint count)
{
    ASSERT(sharedBufferPtr);
    SharedBuffer* pBuffer = static_cast<SharedBuffer*>jlong_to_ptr(sharedBufferPtr);

    void *cbits = env->GetPrimitiveArrayCritical(jbits, 0);
    pBuffer->append(static_cast<char*>(cbits), count);
    env->ReleasePrimitiveArrayCritical(jbits, cbits, JNI_ABORT);
}

}//extern "C"

