/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "ImageObserver.h"
#include "BufferImageJava.h"
#include "com_sun_webkit_graphics_GraphicsDecoder.h"

namespace WebCore {

BufferImage::BufferImage(PassRefPtr<RQRef> rqoImage, PassRefPtr<RenderingQueue> rq, int w, int h)
    : Image(),
      m_width(w),
      m_height(h),
      m_rq(rq),
      m_rqoImage(rqoImage)
{}

NativeImagePtr BufferImage::nativeImageForCurrentFrame()
{
    m_rq->flushBuffer();
    return m_rqoImage;
}

void BufferImage::flushImageRQ(GraphicsContext *gc)
{
    if (!gc || gc->paintingDisabled()) {
        return;
    }

    RenderingQueue &rqScreen = gc->platformContext()->rq();

    if (!m_rq->isEmpty()) {
        // 1. Drawing is flushed to the buffered image's RenderQueue.
        m_rq->flushBuffer();

        // 2. The buffered image's RenderQueue is to be decoded.
        rqScreen.freeSpace(8)
        << (jint)com_sun_webkit_graphics_GraphicsDecoder_DECODERQ
        << m_rq->getRQRenderingQueue();
    }
}

void BufferImage::drawPattern(GraphicsContext *gc, const FloatRect& srcRect, const AffineTransform& patternTransform,
                        const FloatPoint& phase, ColorSpace cs, CompositeOperator co, const FloatRect& destRect)
{
    flushImageRQ(gc);
    Image::drawPattern(gc, srcRect, patternTransform,
                        phase, cs, co, destRect);
}

void BufferImage::draw(GraphicsContext* gc, const FloatRect& dstRect,
                       const FloatRect& srcRect, ColorSpace cs, CompositeOperator co, BlendMode bm, ImageOrientationDescription)
{
    flushImageRQ(gc);
    Image::drawImage(gc, dstRect, srcRect, cs, co, bm);
}


} // namespace WebCore
