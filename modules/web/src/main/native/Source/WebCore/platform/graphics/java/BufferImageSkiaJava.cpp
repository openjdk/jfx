/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "PlatformContextJava.h"
#include "PlatformContextSkiaJava.h"
#include "BufferImageSkiaJava.h"
#include "SkCanvas.h"
#include "NotImplemented.h"

namespace WebCore {


BufferImageSkiaJava::BufferImageSkiaJava(const SkBitmap* src) : m_image(src) { }

PassRefPtr<BufferImageSkiaJava> BufferImageSkiaJava::create(
    const SkBitmap* src) 
{
    BufferImageSkiaJava* res = new BufferImageSkiaJava(src);
    return adoptRef(res);
}

void BufferImageSkiaJava::draw(
    GraphicsContext* gc, const FloatRect& dstRect, const FloatRect& srcRect, 
    ColorSpace styleColorSpace, CompositeOperator op) 
{
    if (!gc) return;

    SkIRect skSize;
    skSize.set(srcRect.x(),srcRect.y(), 
               srcRect.x() + srcRect.width(), srcRect.y() + srcRect.height());

    gc->platformContext()->canvas()->drawBitmapRect(*m_image, &skSize, 
        SkRect::MakeXYWH(dstRect.x(), dstRect.y(), 
                         dstRect.width(), dstRect.height()));
}

}
