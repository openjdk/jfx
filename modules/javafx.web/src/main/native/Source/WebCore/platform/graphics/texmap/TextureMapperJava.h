/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "BitmapTextureJava.h"
#include "ImageBuffer.h"
#include "TextureMapper.h"

#if USE(TEXTURE_MAPPER)
namespace WebCore {

class TextureMapperJava final : public TextureMapper {
    WTF_MAKE_FAST_ALLOCATED;
public:
    TextureMapperJava();

    // TextureMapper implementation
    void drawBorder(const Color&, float borderWidth, const FloatRect&, const TransformationMatrix&) final;
    void drawNumber(int number, const Color&, const FloatPoint&, const TransformationMatrix&) final;
    void drawTexture(const BitmapTexture&, const FloatRect& targetRect, const TransformationMatrix&, float opacity, unsigned exposedEdges) final;
    void drawSolidColor(const FloatRect&, const TransformationMatrix&, const Color&, bool) final;
    void beginClip(const TransformationMatrix&, const FloatRoundedRect&) final;
    void bindSurface(BitmapTexture* surface) final { m_currentSurface = surface;}
    void endClip() final { graphicsContext()->restore(); }
    IntRect clipBounds() final { return currentContext()->clipBounds(); }
    IntSize maxTextureSize() const final;
    Ref<BitmapTexture> createTexture() final { return BitmapTextureJava::create(); }
    Ref<BitmapTexture> createTexture(GCGLint) final { return createTexture(); }
    void clearColor(const Color&) final;

    inline GraphicsContext* currentContext()
    {
        return m_currentSurface ? static_cast<BitmapTextureJava*>(m_currentSurface.get())->graphicsContext() : graphicsContext();
    }

    void setGraphicsContext(GraphicsContext* context) { m_context = context; }
    GraphicsContext* graphicsContext() { return m_context; }
private:
    RefPtr<BitmapTexture> m_currentSurface;
    GraphicsContext* m_context;
};

}
#endif // USE(TEXTURE_MAPPER)
