/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include <wtf/text/WTFString.h>

#include "ImageObserver.h"
#include "BufferImageJava.h"
#include "PlatformContextJava.h"

#include "com_sun_webkit_graphics_GraphicsDecoder.h"

namespace WebCore {

BufferImage::BufferImage(PlatformImagePtr image)
    : Image(), m_image(image)
{}

RefPtr<NativeImage> BufferImage::nativeImage(const DestinationColorSpace&)
{
    return currentNativeImage();
}

RefPtr<NativeImage> BufferImage::currentNativeImage()
{
    m_image->getRenderingQueue()->flushBuffer();
    return NativeImage::create(m_image.get());
}

void BufferImage::flushImageRQ(GraphicsContext& gc)
{
    if (gc.paintingDisabled()) {
        return;
    }

    RenderingQueue& rqScreen = gc.platformContext()->rq();
    auto rq = m_image->getRenderingQueue();

    if (!rq->isEmpty()) {
        // 1. Drawing is flushed to the buffered image's RenderQueue.
        rq->flushBuffer();

        // 2. The buffered image's RenderQueue is to be decoded.
        rqScreen.freeSpace(8)
        << (jint)com_sun_webkit_graphics_GraphicsDecoder_DECODERQ
        << rq->getRQRenderingQueue();
    }
}

ImageDrawResult BufferImage::draw(GraphicsContext& gc, const FloatRect& dstRect,
        const FloatRect& srcRect, ImagePaintingOptions options)
{
    flushImageRQ(gc);
    Image::drawImage(gc, dstRect, srcRect, options.compositeOperator(), options.blendMode());
    return ImageDrawResult::DidDraw;
}

void BufferImage::drawPattern(GraphicsContext& gc, const FloatRect& destRect, const FloatRect& srcRect, const AffineTransform& patternTransform,
        const FloatPoint& phase, const FloatSize& spacing, ImagePaintingOptions options)
{
    flushImageRQ(gc);
    Image::drawPattern(gc, destRect, srcRect, patternTransform,
                        phase, spacing, options);
}

} // namespace WebCore
