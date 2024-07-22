/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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

#pragma once

#include "GraphicsContext.h"
#include "Image.h"
#include "IntSize.h"
#include "PlatformJavaClasses.h"

namespace WebCore {

class RenderingQueue;

//BufferImage is an image renderer. That provides the functionality
//of canvas buffer drawing.

class BufferImage : public Image {
public:
    static RefPtr<BufferImage> create(PlatformImagePtr image)
    {
        return adoptRef(new BufferImage(image));
    }

    void destroyDecodedData(bool = true) override { }

    //utatodo: callback to Java
    bool currentFrameKnownToBeOpaque() const override { return false; /*!m_data->m_bitmap->hasAlpha() ;*/}

    FloatSize size(ImageOrientation = ImageOrientation::Orientation::FromImage) const override { return m_image->size(); }

    ImageDrawResult draw(GraphicsContext&, const FloatRect& dstRect,
        const FloatRect& srcRect, ImagePaintingOptions = { }) final;

    void drawPattern(GraphicsContext&, const FloatRect& destRect, const FloatRect& srcRect, const AffineTransform& patternTransform,
        const FloatPoint& phase, const FloatSize& spacing, ImagePaintingOptions = { }) final;

    RefPtr<NativeImage> nativeImage(const DestinationColorSpace& = DestinationColorSpace::SRGB()) override;

    RefPtr<NativeImage> nativeImageForCurrentFrame() override;

private:
    BufferImage(PlatformImagePtr);

    void flushImageRQ(GraphicsContext& gc);

    RefPtr<ImageJava> m_image;
};

} // namespace WebCore
