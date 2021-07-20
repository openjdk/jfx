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

#include "config.h"
#include "TextureMapperJava.h"

#include "PlatformContextJava.h"
#include "BitmapTexturePool.h"
#include "GraphicsLayer.h"
#include "NotImplemented.h"
#include <wtf/RandomNumber.h>

#include "com_sun_webkit_graphics_GraphicsDecoder.h"

#if USE(TEXTURE_MAPPER)
namespace WebCore {

static const int s_maximumAllowedImageBufferDimension = 256;

std::unique_ptr<TextureMapper> TextureMapper::platformCreateAccelerated()
{
    return std::make_unique<TextureMapperJava>();
}

TextureMapperJava::TextureMapperJava()
{
    m_texturePool = std::make_unique<BitmapTexturePool>();
}

IntSize TextureMapperJava::maxTextureSize() const
{
    return IntSize(s_maximumAllowedImageBufferDimension, s_maximumAllowedImageBufferDimension);
}

void TextureMapperJava::beginClip(const TransformationMatrix& matrix, const FloatRoundedRect& rect)
{
    GraphicsContext* context = currentContext();
    if (!context)
        return;
    auto previousTransform = context->getCTM();
    context->save();
    context->concatCTM(matrix.toAffineTransform());
    context->clip(rect.rect());
    context->setCTM(previousTransform);
}

void TextureMapperJava::drawTexture(const BitmapTexture& texture, const FloatRect& targetRect, const TransformationMatrix& transform, float opacity, unsigned /* exposedEdges */)
{
    GraphicsContext* context = currentContext();
    if (!context)
        return;

    const BitmapTextureJava& textureImageBuffer = static_cast<const BitmapTextureJava&>(texture);
    ImageBuffer* image = textureImageBuffer.image();
    context->save();
    context->setCompositeOperation(isInMaskMode() ? CompositeOperator::DestinationIn : CompositeOperator::SourceOver);
    context->setAlpha(opacity);
    context->platformContext()->rq().freeSpace(68)
        << (jint)com_sun_webkit_graphics_GraphicsDecoder_SET_PERSPECTIVE_TRANSFORM
        << (float)transform.m11() << (float)transform.m12() << (float)transform.m13() << (float)transform.m14()
        << (float)transform.m21() << (float)transform.m22() << (float)transform.m23() << (float)transform.m24()
        << (float)transform.m31() << (float)transform.m32() << (float)transform.m33() << (float)transform.m34()
        << (float)transform.m41() << (float)transform.m42() << (float)transform.m43() << (float)transform.m44();
    context->drawImageBuffer(*image, targetRect);
    context->restore();
}

void TextureMapperJava::drawSolidColor(const FloatRect& rect, const TransformationMatrix& transform, const Color& color, bool)
{
    GraphicsContext* context = currentContext();
    if (!context)
        return;

    context->save();
    context->setCompositeOperation(isInMaskMode() ? CompositeOperator::DestinationIn : CompositeOperator::SourceOver);
    context->platformContext()->rq().freeSpace(68)
        << (jint)com_sun_webkit_graphics_GraphicsDecoder_SET_PERSPECTIVE_TRANSFORM
        << (float)transform.m11() << (float)transform.m12() << (float)transform.m13() << (float)transform.m14()
        << (float)transform.m21() << (float)transform.m22() << (float)transform.m23() << (float)transform.m24()
        << (float)transform.m31() << (float)transform.m32() << (float)transform.m33() << (float)transform.m34()
        << (float)transform.m41() << (float)transform.m42() << (float)transform.m43() << (float)transform.m44();

    context->fillRect(rect, color);
    context->restore();
}

void TextureMapperJava::drawBorder(const Color&, float /* borderWidth */, const FloatRect&, const TransformationMatrix&)
{
    notImplemented();
}

void TextureMapperJava::drawNumber(int /* number */, const Color&, const FloatPoint&, const TransformationMatrix&)
{
    notImplemented();
}

void TextureMapperJava::clearColor(const Color&)
{
    notImplemented();
}

}
#endif
