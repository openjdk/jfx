/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef BufferImageSkiaJava_h
#define BufferImageSkiaJava_h

#include "Image.h"
#include "SkBitmap.h"

namespace WebCore {

class BufferImageSkiaJava : public Image {
public:
    static PassRefPtr<BufferImageSkiaJava> create(const SkBitmap*);

    virtual IntSize size() const {
        return IntSize(m_image->width(), m_image->height());
    }

    virtual void destroyDecodedData(bool destroyAll = true) { }

    virtual unsigned decodedSize() const {
        return 0;
    }

    virtual void draw(GraphicsContext*, const FloatRect& dstRect, const FloatRect& srcRect, ColorSpace styleColorSpace, CompositeOperator);

private:
    const SkBitmap* m_image;

    BufferImageSkiaJava(const SkBitmap* src);
};

} // namespace WebCore

#endif  // BufferImageSkiaJava_h
