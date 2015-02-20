/*
 Copyright (C) 2012 Nokia Corporation and/or its subsidiary(-ies)

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Library General Public
 License as published by the Free Software Foundation; either
 version 2 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Library General Public License for more details.

 You should have received a copy of the GNU Library General Public License
 along with this library; see the file COPYING.LIB.  If not, write to
 the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 Boston, MA 02110-1301, USA.
 */

#include "config.h"

#if USE(TEXTURE_MAPPER)
#include "TextureMapperBackingStore.h"

#include "GraphicsLayer.h"
#include "ImageBuffer.h"
#include "TextureMapper.h"

#if USE(GRAPHICS_SURFACE)
#include "GraphicsSurface.h"
#include "TextureMapperGL.h"
#endif

//utatodo: move to TextureMapperTile.cpp
#if 0 && PLATFORM(JAVA)
#include "TextureMapperImageBuffer.h"
#endif

namespace WebCore {

//utatodo: move to TextureMapperTile.cpp
#if 0 && PLATFORM(JAVA)
void TextureMapperTile::updateContents(TextureMapper* textureMapper, GraphicsLayer* layer, const IntRect& dirtyRect)
{
    IntRect rect = enclosingIntRect(m_rect);
    IntRect targetRect = rect;
    targetRect.intersect(dirtyRect);
    if (targetRect.isEmpty())
        return;

    if (!m_texture) {
        m_texture = textureMapper->createTexture();
        m_texture->reset(rect.size(), BitmapTexture::SupportsAlpha);
    }

    BitmapTextureImageBuffer* texture = dynamic_cast<BitmapTextureImageBuffer*>(m_texture.get());
    ASSERT(texture);
    
    GraphicsContext* context = texture->graphicsContext();
    context->save();
    context->setImageInterpolationQuality(textureMapper->imageInterpolationQuality());
    context->setTextDrawingMode(textureMapper->textDrawingMode());
    context->translate(-rect.x(), -rect.y());
    context->clip(targetRect);
    context->clearRect(targetRect);

    layer->paintGraphicsLayerContents(*context, targetRect);

    context->restore();
}

void TextureMapperTiledBackingStore::updateContents(TextureMapper* textureMapper, GraphicsLayer* layer, const FloatSize& totalSize, const IntRect& dirtyRect)
{
    createOrDestroyTilesIfNeeded(totalSize, IntSize(2048, 2048), true);
    for (size_t i = 0; i < m_tiles.size(); ++i)
        m_tiles[i].updateContents(textureMapper, layer, dirtyRect);
}
#endif

unsigned TextureMapperBackingStore::calculateExposedTileEdges(const FloatRect& totalRect, const FloatRect& tileRect)
{
    unsigned exposedEdges = TextureMapper::NoEdges;
    if (!tileRect.x())
        exposedEdges |= TextureMapper::LeftEdge;
    if (!tileRect.y())
        exposedEdges |= TextureMapper::TopEdge;
    if (tileRect.width() + tileRect.x() >= totalRect.width())
        exposedEdges |= TextureMapper::RightEdge;
    if (tileRect.height() + tileRect.y() >= totalRect.height())
        exposedEdges |= TextureMapper::BottomEdge;
    return exposedEdges;
}

}
#endif
