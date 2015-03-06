/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "BufferImageJava.h"
#include <wtf/text/CString.h>
#include "GraphicsContext.h"
#include "ImageBuffer.h"
#include "ImageData.h"
#include "MIMETypeRegistry.h"
#include "NotImplemented.h"

#include "PlatformContextJava.h"
#include "GraphicsContext.h"
#include "ImageBufferData.h"

namespace WebCore {

ImageBufferData::ImageBufferData(
    const IntSize& size,
    ImageBuffer &rq_holder
) : m_rq_holder(rq_holder)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID midCreateImage = env->GetMethodID(
        PG_GetGraphicsManagerClass(env),
        "createRTImage",
        "(II)Lcom/sun/webkit/graphics/WCImage;");
    ASSERT(midCreateImage);

    m_image = RQRef::create(JLObject(env->CallObjectMethod(
        PL_GetGraphicsManager(env),
        midCreateImage,
        size.width(),
        size.height()
    )));
    CheckAndClearException(env);
}

JLObject ImageBufferData::getWCImage() const
{
    return m_image->cloneLocalCopy();
}

unsigned char *ImageBufferData::data() const
{
    JNIEnv* env = WebCore_GetJavaEnv();

    //RenderQueue need to be processed before pixel buffer extraction.
    //For that purpose it has to be in actual state.
    m_rq_holder.context()->platformContext()->rq().flushBuffer();

    static jmethodID midGetBGRABytes = env->GetMethodID(
        PG_GetImageClass(env),
        "getPixelBuffer",
        "()Ljava/nio/ByteBuffer;");
    ASSERT(midGetBGRABytes);

    JLObject byteBuffer(env->CallObjectMethod(getWCImage(), midGetBGRABytes));
    CheckAndClearException(env);

    return byteBuffer
        ? (unsigned char *) env->GetDirectBufferAddress(byteBuffer)
        : NULL;
}

void ImageBufferData::update()
{
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID midUpdateByteBuffer = env->GetMethodID(
        PG_GetImageClass(env),
        "drawPixelBuffer",
        "()V");
    ASSERT(midUpdateByteBuffer);

    env->CallObjectMethod(getWCImage(), midUpdateByteBuffer);
    CheckAndClearException(env);
}

ImageBuffer::ImageBuffer(
    const IntSize& size,
    float resolutionScale,
    ColorSpace,
    RenderingMode,
    bool& success
)
    : m_data(size, *this)
    , m_size(size)
    , m_logicalSize(size)
    , m_resolutionScale(1)
{
    // RT-10059: ImageBufferData construction may fail if the requested
    // image size is too large. In that case we exit immediately,
    // automatically reporting the failure to ImageBuffer::create().
    if (!m_data.m_image) {
        return;
    }

    JNIEnv* env = WebCore_GetJavaEnv();
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
    CheckAndClearException(env);

    m_context = adoptPtr(
        new GraphicsContext(
            new PlatformContextJava(wcRenderQueue, true)));
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

GraphicsContext* ImageBuffer::context() const
{
    return m_context.get();
}

PassRefPtr<Image> ImageBuffer::copyImage(BackingStoreCopy copyBehavior, ScaleBehavior scaleBehavior) const
{
    //utatodo: seems [copyBehavior] is the rest of [drawsUsingCopy]
    return BufferImage::create(
        m_data.m_image,
        m_context->platformContext()->rq_ref(),
        m_size.width(), m_size.height());
}

BackingStoreCopy ImageBuffer::fastCopyImageMode()
{
    return CopyBackingStore; // todo tav revise
}

void ImageBuffer::platformTransformColorSpace(const Vector<int> &lookUpTable)
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

PassRefPtr<Uint8ClampedArray> getImageData(
    const Multiply multiplied,
    const ImageBufferData &idata,
    const IntRect& rect,
    const IntSize& size)
{
    // This code was adapted from the CG implementation

    float area = 4.0f * rect.width() * rect.height();
    if (area > static_cast<float>(std::numeric_limits<int>::max()))
        return 0;

    RefPtr<Uint8ClampedArray> result = Uint8ClampedArray::createUninitialized(rect.width() * rect.height() * 4);
    unsigned char* data = result->data();

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
    unsigned char* dstRows = data + desty * dstBytesPerRow + destx * 4;

    unsigned srcBytesPerRow = 4 * size.width();
    unsigned char* srcRows =
            idata.data() + originy * srcBytesPerRow + originx * 4;

    for (int y = 0; y < height; ++y) {
        unsigned char *pd = dstRows;
        unsigned char *ps = srcRows;
        for (int x = 0; x < width; x++) {
            unsigned char alpha = ps[3];
            if (multiplied == Unmultiplied && alpha && alpha!=255) {
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

PassRefPtr<Uint8ClampedArray> ImageBuffer::getUnmultipliedImageData(const IntRect& rect, CoordinateSystem) const
{
    return getImageData(Unmultiplied, m_data, rect, m_size);
}

PassRefPtr<Uint8ClampedArray> ImageBuffer::getPremultipliedImageData(const IntRect& rect, CoordinateSystem) const
{
    return getImageData(Premultiplied, m_data, rect, m_size);
}

void ImageBuffer::putByteArray(
    Multiply multiplied,
    Uint8ClampedArray* source,
    const IntSize& sourceSize,
    const IntRect& sourceRect,
    const IntPoint& destPoint,
    CoordinateSystem)
{
    // This code was adapted from the CG implementation

    ASSERT(sourceRect.width() > 0);
    ASSERT(sourceRect.height() > 0);

    int originx = sourceRect.x();
    int destx = destPoint.x() + sourceRect.x();
    ASSERT(destx >= 0);
    ASSERT(destx < m_size.width());
    ASSERT(originx >= 0);
    ASSERT(originx <= sourceRect.maxX());

    int endx = destPoint.x() + sourceRect.maxX();
    ASSERT(endx <= m_size.width());
    int width = endx - destx;

    int originy = sourceRect.y();
    int desty = destPoint.y() + sourceRect.y();
    ASSERT(desty >= 0);
    ASSERT(desty < m_size.height());
    ASSERT(originy >= 0);
    ASSERT(originy <= sourceRect.maxY());

    int endy = destPoint.y() + sourceRect.maxY();
    ASSERT(endy <= m_size.height());
    int height = endy - desty;

    if (width <= 0 || height <= 0)
        return;

    unsigned srcBytesPerRow = 4 * sourceSize.width();
    unsigned char* srcRows =
            source->data() + originy * srcBytesPerRow + originx * 4;
    unsigned dstBytesPerRow = 4 * m_size.width();
    unsigned char* dstRows =
            m_data.data() + desty * dstBytesPerRow + destx * 4;

    for (int y = 0; y < height; ++y) {
        unsigned char *pd = dstRows;
        unsigned char *ps = srcRows;
        for (int x = 0; x < width; x++) {
            int alpha = ps[3]; //have to be [int] for right multiply casting
            if (multiplied == Unmultiplied && alpha != 255) {
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

void ImageBuffer::clip(GraphicsContext*, const FloatRect&) const
{
    notImplemented();
}

void ImageBuffer::draw(
    GraphicsContext* context,
    ColorSpace styleColorSpace,
    const FloatRect& destRect,
    const FloatRect& srcRect,
    CompositeOperator op,
    BlendMode bm,
    bool useLowQualityScale)
{
    RefPtr<Image> imageCopy = copyImage();
    context->drawImage(
        imageCopy.get(),
        styleColorSpace,
        destRect,
        srcRect,
        op,
        bm,
        DoNotRespectImageOrientation,
        useLowQualityScale);
}

void ImageBuffer::drawPattern(
    GraphicsContext* context,
    const FloatRect& srcRect,
    const AffineTransform& patternTransform,
    const FloatPoint& phase,
    ColorSpace styleColorSpace,
    CompositeOperator op,
    const FloatRect& destRect,
    BlendMode bm) // todo tav new param
{
    RefPtr<Image> imageCopy = copyImage();
    imageCopy->drawPattern(
        context,
        srcRect,
        patternTransform,
        phase,
        styleColorSpace,
        op,
        destRect);
}

String ImageBuffer::toDataURL(const String& mimeType, const double* quality, CoordinateSystem) const
{
    if (MIMETypeRegistry::isSupportedImageMIMETypeForEncoding(mimeType)) {
        //RenderQueue need to be processed before pixel buffer extraction.
        //For that purpose it has to be in actual state.
        context()->platformContext()->rq().flushBuffer();

        JNIEnv* env = WebCore_GetJavaEnv();

        static jmethodID midToDataURL = env->GetMethodID(
                PG_GetImageClass(env),
                "toDataURL",
                "(Ljava/lang/String;)Ljava/lang/String;");
        ASSERT(midToDataURL);

        JLString data((jstring) env->CallObjectMethod(
                m_data.getWCImage(),
                midToDataURL,
                (jstring) JLString(mimeType.toJavaString(env))));

        CheckAndClearException(env);
        if (data) {
            return String(env, data);
        }
    }
    return "data:,";
}

}
