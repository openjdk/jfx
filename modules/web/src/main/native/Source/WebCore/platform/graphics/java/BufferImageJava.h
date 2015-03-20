/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef BufferImageJava_h
#define BufferImageJava_h

#include "GraphicsContext.h"
#include "Image.h"
#include "IntSize.h"
#include "JavaEnv.h"

namespace WebCore {

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

    virtual void destroyDecodedData(bool destroyAll = true) { }
    virtual unsigned decodedSize() const { return 0; }

    //utatodo: callback to Java
    virtual bool currentFrameKnownToBeOpaque() { return false; /*!m_data->m_bitmap->hasAlpha() ;*/}

    virtual IntSize size() const { return IntSize(m_width, m_height); }

    virtual void draw(GraphicsContext* gc, const FloatRect& dstRect,
                      const FloatRect& srcRect, ColorSpace styleColorSpace,
                      CompositeOperator op, BlendMode bm, ImageOrientationDescription);

    virtual void drawPattern(GraphicsContext* gc, const FloatRect& srcRect, const AffineTransform& patternTransform,
                             const FloatPoint& phase, ColorSpace styleColorSpace, CompositeOperator, const FloatRect& destRect);

    virtual NativeImagePtr nativeImageForCurrentFrame();

private:
    BufferImage(PassRefPtr<RQRef> rqoImage, PassRefPtr<RenderingQueue> rq, int w, int h);

    void flushImageRQ(GraphicsContext *gc);

    int m_width, m_height;
    RefPtr<RQRef> m_rqoImage;
    RefPtr<RenderingQueue> m_rq;
};

}

#endif
