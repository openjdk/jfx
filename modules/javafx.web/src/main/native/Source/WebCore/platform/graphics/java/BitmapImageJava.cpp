/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "BitmapImage.h"
#include "NotImplemented.h"
#include "GraphicsContext.h"
#include "ImageObserver.h"
#include "PlatformJavaClasses.h"
#include "GraphicsContextJava.h"
#include "PlatformContextJava.h"
#include "ImageDecoderJava.h"
#include "RenderingQueue.h"
#include "SharedBuffer.h"

namespace WebCore {

Ref<Image> BitmapImage::createFromName(const char* name)
{
    Ref<BitmapImage> img(create());

    WC_GETJAVAENV_CHKRET(env, WTFMove(img));

#if USE(IMAGEIO)
    static jmethodID midLoadFromResource = env->GetMethodID(
        PG_GetGraphicsImageDecoderClass(env),
        "loadFromResource",
        "(Ljava/lang/String;)V");
    ASSERT(midLoadFromResource);

    SharedBufferBuilder bufferBuilder;
    //RefPtr<SharedBuffer> dataBuffer(SharedBuffer::create());
    //img->m_source->ensureDecoderAvailable(dataBuffer.get());
    //img->m_source->ensureDecoderAvailable(bufferBuilder.take().ptr());    //revisit
  /*  env->CallVoidMethod(
        static_cast<ImageDecoderJava*>(img->m_source->m_decoder.get())->nativeDecoder(),
        midLoadFromResource,
        (jstring)String::fromLatin1(name).toJavaString(env));
    WTF::CheckAndClearException(env); */

    // we have to make this call in order to initialize
    // internal flags that indicates the image readiness
   // img->encodedDataStatus();

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

    SharedBufferBuilder bufferBuilder;
    //RefPtr<SharedBuffer> dataBuffer(SharedBuffer::create());
    JLString resourceName(String::fromLatin1(name).toJavaString(env));
    ASSERT(resourceName);

    env->CallVoidMethod(
        PL_GetGraphicsManager(env),
        midLoadFromResource,
        (jstring)resourceName,
        ptr_to_jlong((bufferBuilder.get()).get()));
    WTF::CheckAndClearException(env);
    //From the upper call we got a callback [Java_com_sun_webkit_graphics_WCGraphicsManager_append]
    //that fills the buffer.
    img->setData(WTFMove(dataBuffer), true);
#endif
    return WTFMove(img);
}


} // namespace WebCore

using namespace WebCore;
extern "C" {

JNIEXPORT void JNICALL Java_com_sun_webkit_graphics_WCGraphicsManager_append
    (JNIEnv *env, jclass, jlong sharedBufferPtr, jbyteArray jbits, jint count)
{
    ASSERT(sharedBufferPtr);
    SharedBufferBuilder* pBuffer = static_cast<SharedBufferBuilder*>jlong_to_ptr(sharedBufferPtr);

    void *cbits = env->GetPrimitiveArrayCritical(jbits, 0);
    pBuffer->append(std::span<const uint8_t>(static_cast<const uint8_t*>(cbits), count));
    env->ReleasePrimitiveArrayCritical(jbits, cbits, JNI_ABORT);
}

}//extern "C"

