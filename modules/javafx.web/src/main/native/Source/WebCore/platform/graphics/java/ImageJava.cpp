/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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
#include <wkj_constants.h>

#include "NotImplemented.h"

#include "BitmapImage.h"
#include "Image.h"
#include "ImageObserver.h"
#include "ImageBuffer.h"
#include "FloatRect.h"
#include "GraphicsContext.h"
#include "TransformationMatrix.h"
#include "GraphicsContextJava.h"
#include "WKJPlatformJava.h"
#include "PlatformContextJava.h"
#include "Logging.h"

class ImageBuffer;

namespace WebCore {

void Image::drawImage(GraphicsContext& gc, const FloatRect &dstRect, const FloatRect &srcRect,
                       CompositeOperator compositeOperator, BlendMode)
{
    if (gc.paintingDisabled()) {
        return;
    }

    auto nativeImage = currentNativeImage();
    if (!nativeImage) {
        return;
    }

    CompositeOperator oldCompositeOperator = gc.compositeOperation();
    gc.setCompositeOperation(compositeOperator);

    gc.platformContext()->rq().freeSpace(72)
    << (int32_t)com_sun_webkit_graphics_GraphicsDecoder_DRAWIMAGE
    << nativeImage->platformImage()->getImage()
    << dstRect.x() << dstRect.y()
    << dstRect.width() << dstRect.height()
    << srcRect.x() << srcRect.y()
    << srcRect.width() << srcRect.height();

    gc.setCompositeOperation(oldCompositeOperator);

    if (imageObserver())
        imageObserver()->didDraw(*this);
}

Ref<Image> ImageAdapter::loadPlatformResource(const char *name)
{
    return BitmapImage::createFromName(name);
}

void ImageAdapter::invalidate()
{
}
#if !USE(IMAGEIO)
// USE(IMAGEIO) is unconditionally on in this port (Source/cmake/OptionsJava.cmake sets
// USE_IMAGEIO TRUE), so this branch is not compiled. It is converted rather than deleted so
// that it stays translatable, and so that graphics.create_frame keeps a caller on record.
NativeImagePtr ImageFrame::asNewNativeImage() const
{
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->create_frame)
        return nullptr;

    // Java wraps m_bytes without copying, exactly as NewDirectByteBuffer did.
    WKJHandle frame { cb->create_frame(width(), height(), m_bytes,
                                       static_cast<int64_t>(width()) * height() * sizeof(PixelData)) };
    ASSERT(frame);
    if (wkjCheckAndClearException() || !frame) {
        return nullptr;
    }

    return RQRef::create(frame.get());
}
#endif
} // namespace WebCore
