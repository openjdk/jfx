/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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
#include <wtf/text/StringBuilder.h>
#include "ImageBufferJavaBackend.h"

#include "BufferImageJava.h"
#include "GraphicsContext.h"
#include "ImageData.h"
#include "ImageBuffer.h"
#include "MIMETypeRegistry.h"
#include "PlatformContextJava.h"
#include "GraphicsContextJava.h"
namespace WebCore {

std::unique_ptr<ImageBufferJavaBackend> ImageBufferJavaBackend::create(
    const Parameters& parameters, const ImageBufferCreationContext&)
{
    IntSize backendSize = parameters.backendSize;
    if (backendSize.isEmpty())
        return nullptr;

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID midCreateImage = env->GetMethodID(
        PG_GetGraphicsManagerClass(env),
        "createRTImage",
        "(II)Lcom/sun/webkit/graphics/WCImage;");
    ASSERT(midCreateImage);

    jobject imageObj = env->CallObjectMethod(
        PL_GetGraphicsManager(env),
        midCreateImage,
        (jint) ceilf(parameters.resolutionScale * parameters.backendSize.width()),
        (jint) ceilf(parameters.resolutionScale * parameters.backendSize.height())
    );

    if (WTF::CheckAndClearException(env) || !imageObj) {
        return nullptr;
    }

    auto image = RQRef::create(JLObject(imageObj));

    static jmethodID midCreateBufferedContextRQ = env->GetMethodID(
        PG_GetGraphicsManagerClass(env),
        "createBufferedContextRQ",
        "(Lcom/sun/webkit/graphics/WCImage;)Lcom/sun/webkit/graphics/WCRenderQueue;");
    ASSERT(midCreateBufferedContextRQ);

    JLObject wcRenderQueue(env->CallObjectMethod(
        PL_GetGraphicsManager(env),
        midCreateBufferedContextRQ,
        (jobject)(image->cloneLocalCopy())));
    ASSERT(wcRenderQueue);
    if (WTF::CheckAndClearException(env) || !wcRenderQueue) {
        return nullptr;
    }

    auto context = makeUnique<GraphicsContextJava>(new PlatformContextJava(wcRenderQueue, true));

    auto platformImage = ImageJava::create(image, context->platformContext()->rq_ref(),
        backendSize.width(), backendSize.height());

    return std::unique_ptr<ImageBufferJavaBackend>(new ImageBufferJavaBackend(
        parameters, WTFMove(platformImage), WTFMove(context), backendSize));
}

/*std::unique_ptr<ImageBufferJavaBackend> ImageBufferJavaBackend::create(
    const Parameters& parameters, const GraphicsContext&)
{
    return ImageBufferJavaBackend::create(parameters, nullptr);
}*/

ImageBufferJavaBackend::ImageBufferJavaBackend(
    const Parameters& parameters, PlatformImagePtr image, std::unique_ptr<GraphicsContext>&& context, IntSize backendSize)
    : ImageBufferBackend(parameters)
    , m_image(WTFMove(image))
    , m_context(WTFMove(context))
    , m_backendSize(backendSize)
{
}

JLObject ImageBufferJavaBackend::getWCImage() const
{
    return m_image->getImage()->cloneLocalCopy();
}

Vector<uint8_t> ImageBufferJavaBackend::toDataJava(const String& mimeType, std::optional<double>)
{
    if (MIMETypeRegistry::isSupportedImageMIMETypeForEncoding(mimeType)) {
        // RenderQueue need to be processed before pixel buffer extraction.
        // For that purpose it has to be in actual state.
        context().platformContext()->rq().flushBuffer();

        JNIEnv* env = WTF::GetJavaEnv();

        static jmethodID midToData = env->GetMethodID(
                PG_GetImageClass(env),
                "toData",
                "(Ljava/lang/String;)[B");
        ASSERT(midToData);

        JLocalRef<jbyteArray> jdata((jbyteArray)env->CallObjectMethod(
                getWCImage(),
                midToData,
                (jstring) JLString(mimeType.toJavaString(env))));

        if (!WTF::CheckAndClearException(env) && jdata) {
            uint8_t* dataArray = (uint8_t*)env->GetPrimitiveArrayCritical((jbyteArray)jdata, 0);
            Vector<uint8_t> data;
            std::span<uint8_t> span(dataArray, env->GetArrayLength(jdata));
            data.append(span);
            env->ReleasePrimitiveArrayCritical(jdata, dataArray, 0);
            return data;
        }
    }
    return { };
}

void* ImageBufferJavaBackend::getData()
{
    JNIEnv* env = WTF::GetJavaEnv();

    //RenderQueue need to be processed before pixel buffer extraction.
    //For that purpose it has to be in actual state.
    context().platformContext()->rq().flushBuffer();

    static jmethodID midGetBGRABytes = env->GetMethodID(
        PG_GetImageClass(env),
        "getPixelBuffer",
        "()Ljava/nio/ByteBuffer;");
    ASSERT(midGetBGRABytes);

    jobject pixelBuf = env->CallObjectMethod(getWCImage(), midGetBGRABytes);
    if (WTF::CheckAndClearException(env) || !pixelBuf) {
        return NULL;
    }
    JLObject byteBuffer(pixelBuf);

    return env->GetDirectBufferAddress(byteBuffer);
}

void ImageBufferJavaBackend::update() const
{
    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID midUpdateByteBuffer = env->GetMethodID(
        PG_GetImageClass(env),
        "drawPixelBuffer",
        "()V");
    ASSERT(midUpdateByteBuffer);

    env->CallObjectMethod(getWCImage(), midUpdateByteBuffer);
    WTF::CheckAndClearException(env);
}

GraphicsContext& ImageBufferJavaBackend::context()
{
    return *m_context;
}

void ImageBufferJavaBackend::flushContext()
{
}


RefPtr<NativeImage> ImageBufferJavaBackend::copyNativeImage()
{
    return NativeImage::create((m_image.get()));
}

RefPtr<NativeImage> ImageBufferJavaBackend::createNativeImageReference()
{
     return copyNativeImage();
}

void ImageBufferJavaBackend::getPixelBuffer(const IntRect& srcRect, PixelBuffer& destination) //overide method
{
    void *data = getData();
    if (!data)
        return;
    return getPixelBuffer(srcRect, static_cast<const uint8_t*>(data), destination);

}

void ImageBufferJavaBackend::getPixelBuffer(const IntRect& srcRect, const uint8_t* data, PixelBuffer& destination)
{
    return ImageBufferBackend::getPixelBuffer(srcRect, data,destination);
}

void ImageBufferJavaBackend::putPixelBuffer(const PixelBuffer& sourcePixelBuffer, const IntRect& srcRect, const IntPoint& destPoint, AlphaPremultiplication destFormat, uint8_t* destination)
{
    ImageBufferBackend::putPixelBuffer(sourcePixelBuffer, srcRect, destPoint, destFormat, destination);
    update();
}

void ImageBufferJavaBackend::putPixelBuffer(const PixelBuffer& sourcePixelBuffer, const IntRect& srcRect, const IntPoint& destPoint, AlphaPremultiplication destFormat) //override
{
    void *data = getData();
    if (!data)
        return;
    putPixelBuffer(sourcePixelBuffer, srcRect, destPoint, destFormat, static_cast<uint8_t*>(data));
    update();
}

size_t ImageBufferJavaBackend::calculateMemoryCost(const Parameters& parameters)
{
    IntSize backendSize = parameters.backendSize;
    return ImageBufferBackend::calculateMemoryCost(backendSize, calculateBytesPerRow(backendSize));
}

unsigned ImageBufferJavaBackend::calculateBytesPerRow(const IntSize& backendSize)
{
    ASSERT(!backendSize.isEmpty());
    return CheckedUint32(backendSize.width()) * 4;
}

unsigned ImageBufferJavaBackend::bytesPerRow() const
{
    IntSize backendSize = m_backendSize;
    return calculateBytesPerRow(backendSize);
}

String ImageBufferJavaBackend::debugDescription() const
{
     StringBuilder builder;
     builder.append(WTF::String::fromUTF8("ImageBufferBackendJava"));
     return builder.toString();
}

bool ImageBufferJavaBackend::canMapBackingStore() const
{
    return true;
}

} // namespace WebCore
