/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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
#include "ImageBufferJavaBackend.h"

#include "BufferImageJava.h"
#include "GraphicsContext.h"
#include "ImageData.h"
#include "MIMETypeRegistry.h"
#include "PlatformContextJava.h"
#include "GraphicsContextJava.h"
namespace WebCore {

std::unique_ptr<ImageBufferJavaBackend> ImageBufferJavaBackend::create(
    const Parameters& parameters, const ImageBuffer::CreationContext&)
{
    IntSize backendSize = ImageBufferBackend::calculateBackendSize(parameters);
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
        (jint) ceilf(parameters.resolutionScale * parameters.logicalSize.width()),
        (jint) ceilf(parameters.resolutionScale * parameters.logicalSize.height())
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

std::unique_ptr<ImageBufferJavaBackend> ImageBufferJavaBackend::create(
    const Parameters& parameters, const GraphicsContext&)
{
    return ImageBufferJavaBackend::create(parameters, nullptr);
}

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
            data.append(dataArray, env->GetArrayLength(jdata));
            env->ReleasePrimitiveArrayCritical(jdata, dataArray, 0);
            return data;
        }
    }
    return { };
}

void *ImageBufferJavaBackend::getData() const
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

GraphicsContext& ImageBufferJavaBackend::context() const
{
    return *m_context;
}

void ImageBufferJavaBackend::flushContext()
{
}

IntSize ImageBufferJavaBackend::backendSize() const
{
    return m_backendSize;
}

RefPtr<NativeImage> ImageBufferJavaBackend::copyNativeImage(BackingStoreCopy) const
{
    return NativeImage::create((m_image.get()));
}

RefPtr<PixelBuffer> ImageBufferJavaBackend::getPixelBuffer(const PixelBufferFormat& outputFormat, const IntRect& srcRect, const ImageBufferAllocator& allocator) const
{
    void *data = getData();
    if (!data)
        return nullptr;

    return getPixelBuffer(outputFormat, srcRect, data,allocator);
}

void ImageBufferJavaBackend::putPixelBuffer(const PixelBuffer& sourcePixelBuffer,
    const IntRect& srcRect, const IntPoint& dstPoint, AlphaPremultiplication destFormat)
{
    void *data = getData();
    if (!data)
        return;

    putPixelBuffer(sourcePixelBuffer, srcRect, dstPoint, destFormat, data);
    update();
}

RefPtr<PixelBuffer> ImageBufferJavaBackend::getPixelBuffer(const PixelBufferFormat& outputFormat, const IntRect& srcRect, void* data, const ImageBufferAllocator& allocator) const
{
    return ImageBufferBackend::getPixelBuffer(outputFormat, srcRect, data, allocator);
}

void ImageBufferJavaBackend::putPixelBuffer(const PixelBuffer& sourcePixelBuffer,
    const IntRect& srcRect, const IntPoint& dstPoint, AlphaPremultiplication destFormat, void* data)
{
    ImageBufferBackend::putPixelBuffer(sourcePixelBuffer, srcRect, dstPoint, destFormat, data);
    update();
}

size_t ImageBufferJavaBackend::calculateMemoryCost(const Parameters& parameters)
{
    IntSize backendSize = calculateBackendSize(parameters);
    return ImageBufferBackend::calculateMemoryCost(backendSize, calculateBytesPerRow(backendSize));
}

unsigned ImageBufferJavaBackend::calculateBytesPerRow(const IntSize& backendSize)
{
    ASSERT(!backendSize.isEmpty());
    return CheckedUint32(backendSize.width()) * 4;
}

unsigned ImageBufferJavaBackend::bytesPerRow() const
{
    IntSize backendSize = calculateBackendSize(m_parameters);
    return calculateBytesPerRow(backendSize);
}

} // namespace WebCore
