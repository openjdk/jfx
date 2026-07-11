/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
#include "GraphicsContext.h"
#if USE(TEXTURE_MAPPER)
namespace WebCore {

class TextureMapperJava : public TextureMapper, public ThreadSafeRefCounted<TextureMapperJava>  {
    WTF_DEPRECATED_MAKE_FAST_ALLOCATED(TextureMapperJava);
public:
    TextureMapperJava();

    // TextureMapper implementation
    void drawBorder(const Color&, float borderWidth, const FloatRect&, const TransformationMatrix&);
    void drawNumber(int number, const Color&, const FloatPoint&, const TransformationMatrix&);
    void drawTexture(const BitmapTextureJava&, const FloatRect& targetRect, const TransformationMatrix&, float opacity, unsigned exposedEdges);
    void drawSolidColor(const FloatRect&, const TransformationMatrix&, const Color&, bool);
    void beginClip(const TransformationMatrix&, const FloatRoundedRect&);
    void bindSurface(BitmapTextureJava* surface) { m_currentSurface = surface;}
    void endClip() { graphicsContext()->restore(); }
    IntRect clipBounds() { return currentContext()->clipBounds(); }
    IntSize maxTextureSize() const;
    Ref<BitmapTextureJava> createTexture() { return BitmapTextureJava::create(); }
    BitmapTextureJava* currentSurface() { return m_currentSurface ? m_currentSurface.get() : nullptr; }
    Ref<BitmapTextureJava> createTexture(GCGLint) { return createTexture(); }
    void setDepthRange(double zNear, double zFar);
    void clearColor(const Color&);

    inline GraphicsContext* currentContext()
    {
        return m_currentSurface ? m_currentSurface->graphicsContext() : graphicsContext();
    }

    static Ref<TextureMapperJava> create()
    {
        return adoptRef(*new TextureMapperJava());
    }

    void setGraphicsContext(GraphicsContext* context) { m_context = context; }
    GraphicsContext* graphicsContext() { return m_context; }
private:
    RefPtr<BitmapTextureJava> m_currentSurface;
    GraphicsContext* m_context;
};

}
#endif // USE(TEXTURE_MAPPER)
