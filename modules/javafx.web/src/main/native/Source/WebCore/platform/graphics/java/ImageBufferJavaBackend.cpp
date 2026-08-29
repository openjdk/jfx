/*
 * Copyright (c) 2020, 2026, Oracle and/or its affiliates. All rights reserved.
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
#include "WKJPlatformJava.h"
namespace WebCore {

std::unique_ptr<ImageBufferJavaBackend> ImageBufferJavaBackend::create(
    const Parameters& parameters, const ImageBufferCreationContext&)
{
    IntSize backendSize = parameters.backendSize;
    if (backendSize.isEmpty())
        return nullptr;

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->create_rt_image || !cb->create_buffered_context_rq)
        return nullptr;

    WKJHandle imageObj { cb->create_rt_image(
        (int32_t) ceilf(parameters.resolutionScale * parameters.backendSize.width()),
        (int32_t) ceilf(parameters.resolutionScale * parameters.backendSize.height())) };

    if (wkjCheckAndClearException() || !imageObj) {
        return nullptr;
    }

    auto image = RQRef::create(imageObj.get());

    WKJHandle wcRenderQueue { cb->create_buffered_context_rq(wkj_ref(*image)) };
    ASSERT(wcRenderQueue);
    if (wkjCheckAndClearException() || !wcRenderQueue) {
        return nullptr;
    }

    auto context = makeUnique<GraphicsContextJava>(new PlatformContextJava(wcRenderQueue.get(), true));

    auto platformImage = ImageJava::create(image, context->platformContext()->rq_ref(),
        backendSize.width(), backendSize.height());

    return std::unique_ptr<ImageBufferJavaBackend>(new ImageBufferJavaBackend(
        parameters, WTF::move(platformImage), WTF::move(context), backendSize));
}

/*std::unique_ptr<ImageBufferJavaBackend> ImageBufferJavaBackend::create(
    const Parameters& parameters, const GraphicsContext&)
{
    return ImageBufferJavaBackend::create(parameters, nullptr);
}*/

ImageBufferJavaBackend::ImageBufferJavaBackend(
    const Parameters& parameters, PlatformImagePtr image, std::unique_ptr<GraphicsContext>&& context, IntSize backendSize)
    : ImageBufferBackend(parameters)
    , m_image(WTF::move(image))
    , m_context(WTF::move(context))
    , m_backendSize(backendSize)
{
}

/*
 * The Java WCImage, borrowed from the RQRef that owns it for the lifetime of this backend.
 * The JNI version minted a local ref per call; borrowing costs no registry traffic and has
 * the same reach, because every caller uses it inside one expression.
 */
wkj_ref ImageBufferJavaBackend::getWCImage() const
{
    return wkj_ref(*m_image->getImage());
}

Vector<uint8_t> ImageBufferJavaBackend::toDataJava(const String& mimeType, std::optional<double>)
{
    if (MIMETypeRegistry::isSupportedImageMIMETypeForEncoding(mimeType)) {
        // RenderQueue need to be processed before pixel buffer extraction.
        // For that purpose it has to be in actual state.
        context().platformContext()->rq().flushBuffer();

        const WKJHostGraphics* cb = wkjGraphics();
        if (!cb || !cb->image_to_data)
            return { };

        WKJStringArg mime(mimeType);

        /*
         * WCImage.toData(String) returned a byte[]; the slot copies into a buffer this side
         * provides. The first guess is the raw BGRA size, which every encoder this path
         * supports comes in under, so the WKJ_STR_OVERFLOW retry - which would re-encode on
         * the Java side - is not the normal path.
         */
        int32_t capacity = static_cast<int32_t>(bytesPerRow()) * m_backendSize.height() + 4096;
        Vector<uint8_t> data(static_cast<size_t>(capacity));
        int32_t length = 0;

        int32_t status = cb->image_to_data(getWCImage(), mime.data(), mime.length(),
                                           data.span().data(), capacity, &length);
        if (status == WKJ_STR_OVERFLOW && length > 0) {
            data.grow(static_cast<size_t>(length));
            status = cb->image_to_data(getWCImage(), mime.data(), mime.length(),
                                       data.span().data(), length, &length);
        }
        wkjCheckAndClearException();

        if (status == WKJ_STR_OK) {
            data.shrink(static_cast<size_t>(length));
            return data;
        }
    }
    return { };
}

std::pair<void*, size_t> ImageBufferJavaBackend::getDataAndSize()
{
    //RenderQueue need to be processed before pixel buffer extraction.
    //For that purpose it has to be in actual state.
    context().platformContext()->rq().flushBuffer();

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->image_get_pixel_buffer)
        return {nullptr, 0};

    // Replaces getPixelBuffer() plus GetDirectBufferAddress/GetDirectBufferCapacity. The
    // memory belongs to the Java image and outlives this call, exactly as it did when the
    // local ref to the ByteBuffer went out of scope while the address stayed in use.
    int64_t capacity = 0;
    void* data = cb->image_get_pixel_buffer(getWCImage(), &capacity);
    if (wkjCheckAndClearException() || !data || capacity <= 0)
        return {nullptr, 0};

    return {data, static_cast<size_t>(capacity)};
}

void ImageBufferJavaBackend::update() const
{
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->image_draw_pixel_buffer)
        return;

    cb->image_draw_pixel_buffer(getWCImage());
    wkjCheckAndClearException();
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
    auto [data, size] = getDataAndSize();
    if (!data || size == 0)
        return;
    std::span<const uint8_t> spanData(static_cast<const uint8_t*>(data), size);
    return getPixelBuffer(srcRect, spanData, destination);

}

void ImageBufferJavaBackend::getPixelBuffer(const IntRect& srcRect, std::span<const uint8_t> data, PixelBuffer& destination)
{
    return ImageBufferBackend::getPixelBuffer(srcRect, data,destination);
}

void ImageBufferJavaBackend::putPixelBuffer(const PixelBufferSourceView& sourcePixelBuffer, const IntRect& srcRect, const IntPoint& destPoint, AlphaPremultiplication destFormat, std::span<uint8_t> destination)
{
    ImageBufferBackend::putPixelBuffer(sourcePixelBuffer, srcRect, destPoint, destFormat, destination);
    update();
}

void ImageBufferJavaBackend::putPixelBuffer(const PixelBufferSourceView& sourcePixelBuffer, const IntRect& srcRect, const IntPoint& destPoint, AlphaPremultiplication destFormat) //override
{
    auto [data, size] = getDataAndSize();
    if (!data || size == 0)
        return;
    std::span<uint8_t> spanData(static_cast<uint8_t*>(data), size);
    putPixelBuffer(sourcePixelBuffer, srcRect, destPoint, destFormat, spanData);
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
