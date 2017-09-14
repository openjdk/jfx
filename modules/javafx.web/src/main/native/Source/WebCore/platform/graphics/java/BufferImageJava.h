/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
 */

#pragma once

#include "GraphicsContext.h"
#include "Image.h"
#include "IntSize.h"
#include <wtf/java/JavaEnv.h>

namespace WebCore {

class RenderingQueue;

//BufferImage is an image renderer. That provides the functionality
//of canvas buffer drawing.

class BufferImage : public Image {
public:
    static RefPtr<BufferImage> create(
        RefPtr<RQRef> rqoImage,
        RefPtr<RenderingQueue> rq,
        int w, int h)
    {
        return adoptRef(new BufferImage(rqoImage, rq, w, h));
    }

    void destroyDecodedData(bool = true) override { }

    //utatodo: callback to Java
    bool currentFrameKnownToBeOpaque() const override { return false; /*!m_data->m_bitmap->hasAlpha() ;*/}

    FloatSize size() const override { return FloatSize(m_width, m_height); }

    void draw(GraphicsContext& gc, const FloatRect& dstRect,
              const FloatRect& srcRect, CompositeOperator op, BlendMode bm, ImageOrientationDescription) override;

    void drawPattern(GraphicsContext&, const FloatRect& destRect, const FloatRect& srcRect, const AffineTransform& patternTransform,
        const FloatPoint& phase, const FloatSize& spacing, CompositeOperator, BlendMode = BlendModeNormal) override;

    NativeImagePtr nativeImageForCurrentFrame(const GraphicsContext* = nullptr) override;

private:
    BufferImage(RefPtr<RQRef> rqoImage, RefPtr<RenderingQueue> rq, int w, int h);

    void flushImageRQ(GraphicsContext& gc);

    int m_width, m_height;
    RefPtr<RenderingQueue> m_rq;
    RefPtr<RQRef> m_rqoImage;
};

} // namespace WebCore
