/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "ImageBufferBackend.h"
#include <wtf/IsoMalloc.h>

namespace WebCore {

class ImageBufferJavaBackend : public ImageBufferBackend {
public:
    static std::unique_ptr<ImageBufferJavaBackend> create(
        const FloatSize&, float resolutionScale, ColorSpace, const HostWindow*);
    static std::unique_ptr<ImageBufferJavaBackend> create(const FloatSize&, const GraphicsContext&);

    JLObject getWCImage() const;
    void* getData() const;
    void update() const;

    GraphicsContext& context() const override;
    void flushContext() override;

    NativeImagePtr copyNativeImage(BackingStoreCopy = CopyBackingStore) const override;
    RefPtr<Image> copyImage(BackingStoreCopy = CopyBackingStore, PreserveResolution = PreserveResolution::No) const override;

    void draw(GraphicsContext&, const FloatRect& destRect, const FloatRect& srcRect,
        const ImagePaintingOptions&) override;
    void drawPattern(GraphicsContext&, const FloatRect& destRect, const FloatRect& srcRect,
        const AffineTransform& patternTransform, const FloatPoint& phase,
        const FloatSize& spacing, const ImagePaintingOptions&) override;

    String toDataURL(const String& mimeType, Optional<double> quality, PreserveResolution) const override;
    Vector<uint8_t> toData(const String& mimeType, Optional<double> quality) const override;
    Vector<uint8_t> toBGRAData() const override;

    RefPtr<ImageData> getImageData(AlphaPremultiplication outputFormat, const IntRect&) const override;
    void putImageData(AlphaPremultiplication inputFormat, const ImageData&,
        const IntRect& srcRect, const IntPoint& destPoint, AlphaPremultiplication destFormat) override;

protected:
    ImageBufferJavaBackend(const FloatSize&, const IntSize&, float resolutionScale, ColorSpace,
        RefPtr<RQRef>, std::unique_ptr<GraphicsContext>&&);

    ColorFormat backendColorFormat() const override { return ColorFormat::BGRA; }

    RefPtr<RQRef> m_image;
    std::unique_ptr<GraphicsContext> m_context;
};

} // namespace WebCore
