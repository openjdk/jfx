/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
 */

#include "config.h"

#include "BitmapImage.h"
#include "NotImplemented.h"
#include "GraphicsContext.h"
#include "ImageObserver.h"
#include <wtf/java/JavaEnv.h>
#include "GraphicsContextJava.h"
#include "PlatformContextJava.h"
#include "ImageDecoderJava.h"
#include "RenderingQueue.h"
#include "SharedBuffer.h"

namespace WebCore {

void BitmapImage::invalidatePlatformData()
{
}

RefPtr<Image> BitmapImage::createFromName(const char* name)
{
    WC_GETJAVAENV_CHKRET(env, NULL);

    RefPtr<BitmapImage> img(create());

#if USE(IMAGEIO)
    static jmethodID midLoadFromResource = env->GetMethodID(
        PG_GetGraphicsImageDecoderClass(env),
        "loadFromResource",
        "(Ljava/lang/String;)V");
    ASSERT(midLoadFromResource);

    RefPtr<SharedBuffer> dataBuffer(SharedBuffer::create());
    img->m_source.ensureDecoderAvailable(dataBuffer.get());
    env->CallVoidMethod(
        img->m_source.m_decoder->nativeDecoder(),
        midLoadFromResource,
        (jstring)String(name).toJavaString(env));
    CheckAndClearException(env);

    // we have to make this call in order to initialize
    // internal flags that indicates the image readiness
    img->isSizeAvailable();

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
    img->setData(WTFMove(dataBuffer), true);
#endif
    return img;
}


} // namespace WebCore

using namespace WebCore;
extern "C" {

JNIEXPORT void JNICALL Java_com_sun_webkit_graphics_WCGraphicsManager_append
    (JNIEnv *env, jclass, jlong sharedBufferPtr, jbyteArray jbits, jint count)
{
    ASSERT(sharedBufferPtr);
    SharedBuffer* pBuffer = static_cast<SharedBuffer*>jlong_to_ptr(sharedBufferPtr);

    void *cbits = env->GetPrimitiveArrayCritical(jbits, 0);
    pBuffer->append(static_cast<char*>(cbits), count);
    env->ReleasePrimitiveArrayCritical(jbits, cbits, JNI_ABORT);
}

}//extern "C"

