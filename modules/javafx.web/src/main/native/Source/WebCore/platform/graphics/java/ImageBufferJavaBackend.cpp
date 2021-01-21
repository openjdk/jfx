/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

namespace WebCore {

std::unique_ptr<ImageBufferJavaBackend> ImageBufferJavaBackend::create(
    const FloatSize& size, float resolutionScale, ColorSpace colorSpace, const HostWindow*)
{
    IntSize backendSize = calculateBackendSize(size, resolutionScale);
    if (backendSize.isEmpty())
        return nullptr;

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID midCreateImage = env->GetMethodID(
        PG_GetGraphicsManagerClass(env),
        "createRTImage",
        "(II)Lcom/sun/webkit/graphics/WCImage;");
    ASSERT(midCreateImage);

    auto image = RQRef::create(JLObject(env->CallObjectMethod(
        PL_GetGraphicsManager(env),
        midCreateImage,
        (jint) ceilf(resolutionScale * size.width()),
        (jint) ceilf(resolutionScale * size.height())
    )));
    WTF::CheckAndClearException(env);

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
    WTF::CheckAndClearException(env);

    auto context = makeUnique<GraphicsContext>(new PlatformContextJava(wcRenderQueue, true));

    return std::unique_ptr<ImageBufferJavaBackend>(new ImageBufferJavaBackend(
        size, backendSize, resolutionScale, colorSpace, WTFMove(image), WTFMove(context)));
}

std::unique_ptr<ImageBufferJavaBackend> ImageBufferJavaBackend::create(
    const FloatSize& size, const GraphicsContext&)
{
    return ImageBufferJavaBackend::create(size, 1, ColorSpace::SRGB, nullptr);
}

ImageBufferJavaBackend::ImageBufferJavaBackend(
    const FloatSize& logicalSize,
    const IntSize& backendSize,
    float resolutionScale,
    ColorSpace colorSpace,
    RefPtr<RQRef> image,
    std::unique_ptr<GraphicsContext>&& context)
    : ImageBufferBackend(logicalSize, backendSize, resolutionScale, colorSpace)
    , m_image(WTFMove(image))
    , m_context(WTFMove(context))
{
}

JLObject ImageBufferJavaBackend::getWCImage() const
{
    return m_image->cloneLocalCopy();
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

    JLObject byteBuffer(env->CallObjectMethod(getWCImage(), midGetBGRABytes));
    WTF::CheckAndClearException(env);

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

NativeImagePtr ImageBufferJavaBackend::copyNativeImage(BackingStoreCopy) const
{
    return m_image;
}

RefPtr<Image> ImageBufferJavaBackend::copyImage(BackingStoreCopy, PreserveResolution) const
{
    return BufferImage::create(m_image, m_context->platformContext()->rq_ref(),
        m_backendSize.width(), m_backendSize.height());
}

void ImageBufferJavaBackend::draw(GraphicsContext& context, const FloatRect& destRect,
    const FloatRect& srcRect, const ImagePaintingOptions& options)
{
    RefPtr<Image> imageCopy = copyImage();
    context.drawImage(*imageCopy, destRect, srcRect, options);
}

void ImageBufferJavaBackend::drawPattern(GraphicsContext& context, const FloatRect& destRect,
    const FloatRect& srcRect, const AffineTransform& patternTransform,
    const FloatPoint& phase, const FloatSize& spacing, const ImagePaintingOptions& options)
{
    RefPtr<Image> imageCopy = copyImage();
    imageCopy->drawPattern(context, destRect, srcRect, patternTransform, phase, spacing, options);
}

String ImageBufferJavaBackend::toDataURL(const String& mimeType, Optional<double>, PreserveResolution) const
{
    if (MIMETypeRegistry::isSupportedImageMIMETypeForEncoding(mimeType)) {
        // RenderQueue need to be processed before pixel buffer extraction.
        // For that purpose it has to be in actual state.
        context().platformContext()->rq().flushBuffer();

        JNIEnv* env = WTF::GetJavaEnv();

        static jmethodID midToDataURL = env->GetMethodID(
                PG_GetImageClass(env),
                "toDataURL",
                "(Ljava/lang/String;)Ljava/lang/String;");
        ASSERT(midToDataURL);

        JLString data((jstring) env->CallObjectMethod(
                getWCImage(),
                midToDataURL,
                (jstring) JLString(mimeType.toJavaString(env))));

        WTF::CheckAndClearException(env);
        if (data) {
            return String(env, data);
        }
    }
    return "data:,";
}

Vector<uint8_t> ImageBufferJavaBackend::toData(const String& mimeType, Optional<double>) const
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

        WTF::CheckAndClearException(env);
        if (jdata) {
            uint8_t* dataArray = (uint8_t*)env->GetPrimitiveArrayCritical((jbyteArray)jdata, 0);
            Vector<uint8_t> data;
            data.append(dataArray, env->GetArrayLength(jdata));
            env->ReleasePrimitiveArrayCritical(jdata, dataArray, 0);
            return data;
        }
    }
    return { };
}

Vector<uint8_t> ImageBufferJavaBackend::toBGRAData() const
{
    return { };
}

RefPtr<ImageData> ImageBufferJavaBackend::getImageData(AlphaPremultiplication outputFormat, const IntRect& srcRect) const
{
    return ImageBufferBackend::getImageData(outputFormat, srcRect, getData());
}

void ImageBufferJavaBackend::putImageData(AlphaPremultiplication inputFormat, const ImageData& imageData,
    const IntRect& srcRect, const IntPoint& destPoint, AlphaPremultiplication destFormat)
{
    ImageBufferBackend::putImageData(inputFormat, imageData, srcRect, destPoint, destFormat, getData());
    update();
}

} // namespace WebCore
