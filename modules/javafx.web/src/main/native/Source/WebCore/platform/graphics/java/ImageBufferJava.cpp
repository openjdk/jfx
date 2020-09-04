/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "BufferImageJava.h"

#include <wtf/text/CString.h>
#include <JavaScriptCore/JSCInlines.h>
#include <JavaScriptCore/TypedArrayInlines.h>
#include <JavaScriptCore/Uint8ClampedArray.h>
#include "GraphicsContext.h"
#include "ImageBuffer.h"
#include "ImageData.h"
#include "MIMETypeRegistry.h"
#include "NotImplemented.h"

#include "PlatformContextJava.h"
#include "GraphicsContext.h"
#include "IntRect.h"
#include "ImageBufferData.h"



namespace WebCore {

ImageBufferData::ImageBufferData(
    const FloatSize& size,
    ImageBuffer &rq_holder,
    float resolutionScale)
  : m_rq_holder(rq_holder)
{
    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID midCreateImage = env->GetMethodID(
        PG_GetGraphicsManagerClass(env),
        "createRTImage",
        "(II)Lcom/sun/webkit/graphics/WCImage;");
    ASSERT(midCreateImage);

    m_image = RQRef::create(JLObject(env->CallObjectMethod(
        PL_GetGraphicsManager(env),
        midCreateImage,
        (jint) ceilf(resolutionScale * size.width()),
        (jint) ceilf(resolutionScale * size.height())
    )));
    WTF::CheckAndClearException(env);
}

JLObject ImageBufferData::getWCImage() const
{
    return m_image->cloneLocalCopy();
}

unsigned char *ImageBufferData::data() const
{
    JNIEnv* env = WTF::GetJavaEnv();

    //RenderQueue need to be processed before pixel buffer extraction.
    //For that purpose it has to be in actual state.
    m_rq_holder.context().platformContext()->rq().flushBuffer();

    static jmethodID midGetBGRABytes = env->GetMethodID(
        PG_GetImageClass(env),
        "getPixelBuffer",
        "()Ljava/nio/ByteBuffer;");
    ASSERT(midGetBGRABytes);

    JLObject byteBuffer(env->CallObjectMethod(getWCImage(), midGetBGRABytes));
    WTF::CheckAndClearException(env);

    return byteBuffer
        ? (unsigned char *) env->GetDirectBufferAddress(byteBuffer)
        : NULL;
}

void ImageBufferData::update()
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

ImageBuffer::ImageBuffer(
    const FloatSize& size,
    float resolutionScale,
    ColorSpace,
    RenderingMode,
    const HostWindow*,
    bool& success
)
    : m_data(size, *this, resolutionScale)
    , m_logicalSize(size)
    , m_resolutionScale(resolutionScale)
{
    // RT-10059: ImageBufferData construction may fail if the requested
    // image size is too large. In that case we exit immediately,
    // automatically reporting the failure to ImageBuffer::create().
    if (!m_data.m_image) {
        return;
    }

    float scaledWidth = ceilf(resolutionScale * size.width());
    float scaledHeight = ceilf(resolutionScale * size.height());

    // FIXME: Should we automatically use a lower resolution? //XXX: copy-paste from ImageBufferCG.cpp
    if (!FloatSize(scaledWidth, scaledHeight).isExpressibleAsIntSize())
        return;

    m_size = IntSize(scaledWidth, scaledHeight);

    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID midCreateBufferedContextRQ = env->GetMethodID(
        PG_GetGraphicsManagerClass(env),
        "createBufferedContextRQ",
        "(Lcom/sun/webkit/graphics/WCImage;)Lcom/sun/webkit/graphics/WCRenderQueue;");
    ASSERT(midCreateBufferedContextRQ);

    JLObject wcRenderQueue(env->CallObjectMethod(
        PL_GetGraphicsManager(env),
        midCreateBufferedContextRQ,
        (jobject)m_data.getWCImage()));
    ASSERT(wcRenderQueue);
    WTF::CheckAndClearException(env);

    m_data.m_context = std::make_unique<GraphicsContext>(new PlatformContextJava(wcRenderQueue, true));
    success = true;
}

ImageBuffer::~ImageBuffer()
{
}

/*
size_t ImageBuffer::dataSize() const
{
    return m_size.width() * m_size.height() * 4;
}
*/

GraphicsContext& ImageBuffer::context() const
{
    return *m_data.m_context.get();
}

RefPtr<Image> ImageBuffer::copyImage(BackingStoreCopy, PreserveResolution) const
{
    //utatodo: seems [copyBehavior] is the rest of [drawsUsingCopy]
    return BufferImage::create(
        m_data.m_image,
        m_data.m_context->platformContext()->rq_ref(),
        m_size.width(), m_size.height());
}

void ImageBuffer::platformTransformColorSpace(const std::array<uint8_t, 256>&)
{
    notImplemented();
/*
    uint8* rowData = reinterpret_cast<uint8*>(m_data.m_bitmap.Bits());
    unsigned bytesPerRow = m_data.m_bitmap.BytesPerRow();
    unsigned rows = m_size.height();
    unsigned columns = m_size.width();
    for (unsigned y = 0; y < rows; y++) {
        uint8* pixel = rowData;
        for (unsigned x = 0; x < columns; x++) {
            // lookUpTable doesn't seem to support a LUT for each color channel
            // separately (judging from the other ports). We don't need to
            // convert from/to pre-multiplied color space since BBitmap storage
            // is not pre-multiplied.
            pixel[0] = lookUpTable[pixel[0]];
            pixel[1] = lookUpTable[pixel[1]];
            pixel[2] = lookUpTable[pixel[2]];
            // alpha stays unmodified.
            pixel += 4;
        }
        rowData += bytesPerRow;
    }
*/
}

RefPtr<Uint8ClampedArray> getImageData(
    const AlphaPremultiplication multiplied,
    const ImageBufferData& idata,
    const IntRect& rect,
    const IntSize& size)
{
    // This code was adapted from the CG implementation

    if (!idata.data())
        return nullptr;

    Checked<unsigned, RecordOverflow> area = 4;
    area *= rect.width();
    area *= rect.height();
    if (area.hasOverflowed())
        return nullptr;

    auto result = Uint8ClampedArray::tryCreateUninitialized(area.unsafeGet());
    uint8_t* resultData = result ? result->data() : nullptr;
    if (!resultData)
        return nullptr;

    if (rect.x() < 0 || rect.y() < 0
            || rect.maxX() > size.width() || rect.maxY() > size.height())
        result->zeroFill();

    int originx = rect.x();
    int destx = 0;
    if (originx < 0) {
        destx = -originx;
        originx = 0;
    }
    int endx = rect.maxX();
    if (endx > size.width())
        endx = size.width();
    int width = endx - originx;

    int originy = rect.y();
    int desty = 0;
    if (originy < 0) {
        desty = -originy;
        originy = 0;
    }
    int endy = rect.maxY();
    if (endy > size.height())
        endy = size.height();
    int height = endy - originy;

    if (width <= 0 || height <= 0)
        return result;

    unsigned dstBytesPerRow = 4 * rect.width();
    unsigned char* dstRows = resultData + desty * dstBytesPerRow + destx * 4;

    unsigned srcBytesPerRow = 4 * size.width();
    unsigned char* srcRows =
            idata.data() + originy * srcBytesPerRow + originx * 4;

    for (int y = 0; y < height; ++y) {
        unsigned char *pd = dstRows;
        unsigned char *ps = srcRows;
        for (int x = 0; x < width; x++) {
            unsigned char alpha = ps[3];
            if (multiplied == AlphaPremultiplication::Unpremultiplied && alpha && alpha!=255) {
                // Unmultiply and convert BGRA to RGBA
                pd[0] = (ps[2] * 255) / alpha;
                pd[1] = (ps[1] * 255) / alpha;
                pd[2] = (ps[0] * 255) / alpha;
                pd[3] = alpha;
            } else {
                // Convert BGRA to RGBA
                pd[0] = ps[2];
                pd[1] = ps[1];
                pd[2] = ps[0];
                pd[3] = alpha;
            }
            pd += 4;
            ps += 4;
        }
        srcRows += srcBytesPerRow;
        dstRows += dstBytesPerRow;
    }


    return result;
}

RefPtr<Uint8ClampedArray> ImageBuffer::getUnmultipliedImageData(const IntRect& rect, IntSize* pixelArrayDimensions, CoordinateSystem coordinateSystem) const
{
    IntRect srcRect = rect;
    if (coordinateSystem == LogicalCoordinateSystem)
        srcRect.scale(m_resolutionScale);

    if (pixelArrayDimensions)
        *pixelArrayDimensions = srcRect.size();

    return getImageData(AlphaPremultiplication::Unpremultiplied, m_data, srcRect, m_size);
}

RefPtr<Uint8ClampedArray> ImageBuffer::getPremultipliedImageData(const IntRect& rect, IntSize* pixelArrayDimensions, CoordinateSystem coordinateSystem) const
{
    IntRect srcRect = rect;
    if (coordinateSystem == LogicalCoordinateSystem)
        srcRect.scale(m_resolutionScale);

    if (pixelArrayDimensions)
        *pixelArrayDimensions = srcRect.size();

    return getImageData(AlphaPremultiplication::Premultiplied, m_data, srcRect, m_size);
}

void ImageBuffer::putByteArray(
    const Uint8ClampedArray& source,
    AlphaPremultiplication multiplied,
    const IntSize& sourceSize,
    const IntRect& sourceRect,
    const IntPoint& destPoint,
    CoordinateSystem coordinateSystem)
{
    // This code was adapted from the CG implementation

    IntRect scaledSourceRect = sourceRect;
    IntSize scaledSourceSize = sourceSize;
    if (coordinateSystem == LogicalCoordinateSystem) {
        scaledSourceRect.scale(m_resolutionScale);
        scaledSourceSize.scale(m_resolutionScale);
    }

    ASSERT(scaledSourceRect.width() > 0);
    ASSERT(scaledSourceRect.height() > 0);

    int originx = scaledSourceRect.x();
    int destx = destPoint.x() + scaledSourceRect.x();
    ASSERT(destx >= 0);
    ASSERT(destx < m_size.width());
    ASSERT(originx >= 0);
    ASSERT(originx <= scaledSourceRect.maxX());

    int endx = destPoint.x() + scaledSourceRect.maxX();
    ASSERT(endx <= m_size.width());
    int width = endx - destx;

    int originy = scaledSourceRect.y();
    int desty = destPoint.y() + scaledSourceRect.y();
    ASSERT(desty >= 0);
    ASSERT(desty < m_size.height());
    ASSERT(originy >= 0);
    ASSERT(originy <= scaledSourceRect.maxY());

    int endy = destPoint.y() + scaledSourceRect.maxY();
    ASSERT(endy <= m_size.height());
    int height = endy - desty;

    if (width <= 0 || height <= 0)
        return;

    unsigned srcBytesPerRow = 4 * scaledSourceSize.width();
    unsigned char* srcRows =
            source.data() + originy * srcBytesPerRow + originx * 4;
    unsigned dstBytesPerRow = 4 * m_size.width();
    unsigned char* dstRows =
            m_data.data() + desty * dstBytesPerRow + destx * 4;

    for (int y = 0; y < height; ++y) {
        unsigned char *pd = dstRows;
        unsigned char *ps = srcRows;
        for (int x = 0; x < width; x++) {
            int alpha = ps[3]; //have to be [int] for right multiply casting
            if (multiplied == AlphaPremultiplication::Unpremultiplied && alpha != 255) {
                // Premultiply and convert RGBA to BGRA
                pd[0] = static_cast<unsigned char>((ps[2] * alpha + 254) / 255);
                pd[1] = static_cast<unsigned char>((ps[1] * alpha + 254) / 255);
                pd[2] = static_cast<unsigned char>((ps[0] * alpha + 254) / 255);
                pd[3] = static_cast<unsigned char>(alpha);
            } else {
                // Convert RGBA to BGRA
                pd[0] = ps[2];
                pd[1] = ps[1];
                pd[2] = ps[0];
                pd[3] = alpha;
            }
            pd += 4;
            ps += 4;
        }
        dstRows += dstBytesPerRow;
        srcRows += srcBytesPerRow;
    }

    m_data.update();
}

void ImageBuffer::drawConsuming(std::unique_ptr<ImageBuffer> imageBuffer, GraphicsContext& destContext, const FloatRect& destRect, const FloatRect& srcRect, const ImagePaintingOptions& options)
{
    imageBuffer->draw(destContext, destRect, srcRect, options);
}

void ImageBuffer::draw(
    GraphicsContext& context,
    const FloatRect& destRect,
    const FloatRect& srcRect,
    const ImagePaintingOptions& options)
{
    RefPtr<Image> imageCopy = copyImage();
    context.drawImage(
        *imageCopy,
        destRect,
        srcRect,
        options
        );
}

void ImageBuffer::drawPattern(
    GraphicsContext& context,
    const FloatRect& destRect,
    const FloatRect& srcRect,
    const AffineTransform& patternTransform,
    const FloatPoint& phase,
    const FloatSize& spacing,
    const ImagePaintingOptions& options) // todo tav new param
{
    RefPtr<Image> imageCopy = copyImage();
    imageCopy->drawPattern(
        context,
        destRect,
        srcRect,
        patternTransform,
        phase,
        spacing,
        options);
}

RefPtr<Image> ImageBuffer::sinkIntoImage(std::unique_ptr<ImageBuffer> imageBuffer, PreserveResolution preserveResolution)
{
    return imageBuffer->copyImage(DontCopyBackingStore, preserveResolution);
}

String ImageBuffer::toDataURL(const String& mimeType, Optional<double>, PreserveResolution) const
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
                m_data.getWCImage(),
                midToDataURL,
                (jstring) JLString(mimeType.toJavaString(env))));

        WTF::CheckAndClearException(env);
        if (data) {
            return String(env, data);
        }
    }
    return "data:,";
}

Vector<uint8_t> ImageBuffer::toData(const String& mimeType, Optional<double>) const
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
                m_data.getWCImage(),
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

}
