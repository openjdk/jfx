/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef BufferImageJava_h
#define BufferImageJava_h

#include "GraphicsContext.h"
#include "Image.h"
#include "IntSize.h"
#include "JavaEnv.h"

namespace WebCore {

class RenderingQueue;

//BufferImage is an image renderer. That provides the functionality
//of canvas buffer drawing.

class BufferImage : public Image {
public:
    static PassRefPtr<BufferImage> create(
        PassRefPtr<RQRef> rqoImage,
        PassRefPtr<RenderingQueue> rq,
        int w, int h)
    {
        return adoptRef(new BufferImage(rqoImage, rq, w, h));
    }

    void destroyDecodedData(bool destroyAll = true) override { }

    //utatodo: callback to Java
    bool currentFrameKnownToBeOpaque() override { return false; /*!m_data->m_bitmap->hasAlpha() ;*/}

    FloatSize size() const override { return FloatSize(m_width, m_height); }

    void draw(GraphicsContext& gc, const FloatRect& dstRect,
              const FloatRect& srcRect, CompositeOperator op, BlendMode bm, ImageOrientationDescription) override;

    void drawPattern(GraphicsContext& gc, const FloatRect& srcRect, const AffineTransform& patternTransform,
                     const FloatPoint& phase, const FloatSize& spacing, CompositeOperator, const FloatRect& destRect, BlendMode = BlendModeNormal) override;

    virtual NativeImagePtr nativeImageForCurrentFrame();

private:
    BufferImage(PassRefPtr<RQRef> rqoImage, PassRefPtr<RenderingQueue> rq, int w, int h);

    void flushImageRQ(GraphicsContext& gc);

    int m_width, m_height;
    RefPtr<RenderingQueue> m_rq;
    RefPtr<RQRef> m_rqoImage;
};

}

#endif
