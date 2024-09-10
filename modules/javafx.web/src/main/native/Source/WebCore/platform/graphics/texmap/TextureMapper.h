/*
 Copyright (C) 2010 Nokia Corporation and/or its subsidiary(-ies)

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

#pragma once
#if PLATFORM(JAVA)
#include "BitmapTexture.h"
#include "Color.h"
#include "IntRect.h"
#include "IntSize.h"
#include "TransformationMatrix.h"

/*
    TextureMapper is a mechanism that enables hardware acceleration of CSS animations (accelerated compositing) without
    a need for a platform specific scene-graph library like CoreAnimations or QGraphicsView.
*/

namespace WebCore {

class BitmapTexturePool;
class GraphicsLayer;
class TextureMapper;
class FilterOperations;
class FloatRoundedRect;

class TextureMapper {
    WTF_MAKE_FAST_ALLOCATED;
public:
    enum PaintFlag {
        PaintingMirrored = 1 << 0,
    };

    enum WrapMode {
        StretchWrap,
        RepeatWrap
    };

    typedef unsigned PaintFlags;

    WEBCORE_EXPORT static std::unique_ptr<TextureMapper> create();

    explicit TextureMapper();
    virtual ~TextureMapper();

    enum ExposedEdges {
        NoEdges = 0,
        LeftEdge = 1 << 0,
        RightEdge = 1 << 1,
        TopEdge = 1 << 2,
        BottomEdge = 1 << 3,
        AllEdges = LeftEdge | RightEdge | TopEdge | BottomEdge,
    };

    virtual void drawBorder(const Color&, float borderWidth, const FloatRect&, const TransformationMatrix&) = 0;
    virtual void drawNumber(int number, const Color&, const FloatPoint&, const TransformationMatrix&) = 0;

    virtual void drawTexture(const BitmapTexture&, const FloatRect& target, const TransformationMatrix& modelViewMatrix = TransformationMatrix(), float opacity = 1.0f, unsigned exposedEdges = AllEdges) = 0;
    virtual void drawSolidColor(const FloatRect&, const TransformationMatrix&, const Color&, bool) = 0;
    virtual void clearColor(const Color&) = 0;

    // makes a surface the target for the following drawTexture calls.
    virtual void bindSurface(BitmapTexture* surface) = 0;
    virtual BitmapTexture* currentSurface() = 0;
    virtual void beginClip(const TransformationMatrix&, const FloatRoundedRect&) = 0;
    virtual void endClip() = 0;
    virtual IntRect clipBounds() = 0;
    virtual Ref<BitmapTexture> createTexture() = 0;
    virtual void setDepthRange(double zNear, double zFar) = 0;

    virtual void beginPainting(PaintFlags = 0, BitmapTexture* = nullptr) { }
    virtual void endPainting() { }

    void setMaskMode(bool m) { m_isMaskMode = m; }

    virtual IntSize maxTextureSize() const = 0;

    virtual RefPtr<BitmapTexture> acquireTextureFromPool(const IntSize&, const BitmapTexture::Flags = BitmapTexture::SupportsAlpha);
#if USE(GRAPHICS_LAYER_WC)
    WEBCORE_EXPORT void releaseUnusedTexturesNow();
#endif

    void setPatternTransform(const TransformationMatrix& p) { m_patternTransform = p; }
    void setWrapMode(WrapMode m) { m_wrapMode = m; }

protected:
    std::unique_ptr<BitmapTexturePool> m_texturePool;

    bool isInMaskMode() const { return m_isMaskMode; }
    WrapMode wrapMode() const { return m_wrapMode; }
    const TransformationMatrix& patternTransform() const { return m_patternTransform; }

private:
#if USE(TEXTURE_MAPPER_GL) || PLATFORM(JAVA)
    static std::unique_ptr<TextureMapper> platformCreateAccelerated();
#else
    static std::unique_ptr<TextureMapper> platformCreateAccelerated()
    {
        return nullptr;
    }
#endif
    bool m_isMaskMode { false };
    TransformationMatrix m_patternTransform;
    WrapMode m_wrapMode { StretchWrap };
};

}

#else
#if USE(TEXTURE_MAPPER)

#include "BitmapTexturePool.h"
#include "ClipStack.h"
#include "Color.h"
#include "FilterOperation.h"
#include "IntRect.h"
#include "IntSize.h"
#include "TextureMapperGLHeaders.h"
#include "TransformationMatrix.h"
#include <array>
#include <wtf/OptionSet.h>
#include <wtf/Vector.h>

// TextureMapper is a mechanism that enables hardware acceleration of CSS animations (accelerated compositing) without
// a need for a platform specific scene-graph library like CoreAnimation.

namespace WebCore {

class GraphicsLayer;
class TextureMapperGLData;
class TextureMapperShaderProgram;
class FilterOperations;
class FloatRoundedRect;
enum class TextureMapperFlags : uint16_t;

class TextureMapper {
    WTF_MAKE_FAST_ALLOCATED;
public:
    enum class WrapMode : uint8_t {
        Stretch,
        Repeat
    };

    WEBCORE_EXPORT static std::unique_ptr<TextureMapper> create();

    TextureMapper();
    WEBCORE_EXPORT ~TextureMapper();

    enum class AllEdgesExposed : bool { No, Yes };
    enum class FlipY : bool { No, Yes };

    WEBCORE_EXPORT void drawBorder(const Color&, float borderWidth, const FloatRect&, const TransformationMatrix&);
    void drawNumber(int number, const Color&, const FloatPoint&, const TransformationMatrix&);

    WEBCORE_EXPORT void drawTexture(const BitmapTexture&, const FloatRect& target, const TransformationMatrix& modelViewMatrix = TransformationMatrix(), float opacity = 1.0f, AllEdgesExposed = AllEdgesExposed::Yes);
    void drawTexture(GLuint texture, OptionSet<TextureMapperFlags>, const FloatRect& targetRect, const TransformationMatrix& modelViewMatrix, float opacity, AllEdgesExposed = AllEdgesExposed::Yes);
    void drawTexturePlanarYUV(const std::array<GLuint, 3>& textures, const std::array<GLfloat, 16>& yuvToRgbMatrix, OptionSet<TextureMapperFlags>, const FloatRect& targetRect, const TransformationMatrix& modelViewMatrix, float opacity, std::optional<GLuint> alphaPlane, AllEdgesExposed = AllEdgesExposed::Yes);
    void drawTextureSemiPlanarYUV(const std::array<GLuint, 2>& textures, bool uvReversed, const std::array<GLfloat, 16>& yuvToRgbMatrix, OptionSet<TextureMapperFlags>, const FloatRect& targetRect, const TransformationMatrix& modelViewMatrix, float opacity, AllEdgesExposed = AllEdgesExposed::Yes);
    void drawTexturePackedYUV(GLuint texture, const std::array<GLfloat, 16>& yuvToRgbMatrix, OptionSet<TextureMapperFlags>, const FloatRect& targetRect, const TransformationMatrix& modelViewMatrix, float opacity, AllEdgesExposed = AllEdgesExposed::Yes);
    void drawTextureExternalOES(GLuint texture, OptionSet<TextureMapperFlags>, const FloatRect&, const TransformationMatrix& modelViewMatrix, float opacity);
    void drawSolidColor(const FloatRect&, const TransformationMatrix&, const Color&, bool);
    void clearColor(const Color&);

    // makes a surface the target for the following drawTexture calls.
    void bindSurface(BitmapTexture* surface);
    BitmapTexture* currentSurface();
    void beginClip(const TransformationMatrix&, const FloatRoundedRect&);
    WEBCORE_EXPORT void beginPainting(FlipY = FlipY::No, BitmapTexture* = nullptr);
    WEBCORE_EXPORT void endPainting();
    void endClip();
    IntRect clipBounds();
    IntSize maxTextureSize() const { return IntSize(2000, 2000); }
    void setDepthRange(double zNear, double zFar);
    void setMaskMode(bool m) { m_isMaskMode = m; }
    void setWrapMode(WrapMode m) { m_wrapMode = m; }
    void setPatternTransform(const TransformationMatrix& p) { m_patternTransform = p; }

    RefPtr<BitmapTexture> applyFilters(RefPtr<BitmapTexture>&, const FilterOperations&, bool defersLastPass);

    WEBCORE_EXPORT RefPtr<BitmapTexture> acquireTextureFromPool(const IntSize&, OptionSet<BitmapTexture::Flags>);

#if USE(GRAPHICS_LAYER_WC)
    WEBCORE_EXPORT void releaseUnusedTexturesNow();
#endif

private:
    bool isInMaskMode() const { return m_isMaskMode; }
    const TransformationMatrix& patternTransform() const { return m_patternTransform; }

    enum class Direction { X, Y };

    RefPtr<BitmapTexture> applyFilter(RefPtr<BitmapTexture>&, const RefPtr<const FilterOperation>&, bool defersLastPass);
    RefPtr<BitmapTexture> applyBlurFilter(RefPtr<BitmapTexture>&, const BlurFilterOperation&);
    RefPtr<BitmapTexture> applyDropShadowFilter(RefPtr<BitmapTexture>&, const DropShadowFilterOperation&);
    RefPtr<BitmapTexture> applySinglePassFilter(RefPtr<BitmapTexture>&, const RefPtr<const FilterOperation>&, bool shouldDefer);

    void drawTextureCopy(const BitmapTexture& sourceTexture, const FloatRect& sourceRect, const FloatRect& targetRect);
    void drawBlurred(const BitmapTexture& sourceTexture, const FloatRect&, float radius, Direction, bool alphaBlur = false);
    void drawFilterPass(const BitmapTexture& sourceTexture, const BitmapTexture* contentTexture, const FilterOperation&, int pass);
    void drawTexturedQuadWithProgram(TextureMapperShaderProgram&, uint32_t texture, OptionSet<TextureMapperFlags>, const FloatRect&, const TransformationMatrix& modelViewMatrix, float opacity);
    void drawTexturedQuadWithProgram(TextureMapperShaderProgram&, const Vector<std::pair<GLuint, GLuint> >& texturesAndSamplers, OptionSet<TextureMapperFlags>, const FloatRect&, const TransformationMatrix& modelViewMatrix, float opacity);
    void draw(const FloatRect&, const TransformationMatrix& modelViewMatrix, TextureMapperShaderProgram&, GLenum drawingMode, OptionSet<TextureMapperFlags>);
    void drawUnitRect(TextureMapperShaderProgram&, GLenum drawingMode);
    void drawEdgeTriangles(TextureMapperShaderProgram&);

    bool beginScissorClip(const TransformationMatrix&, const FloatRect&);
    bool beginRoundedRectClip(const TransformationMatrix&, const FloatRoundedRect&);
    void bindDefaultSurface();
    ClipStack& clipStack();
    TextureMapperGLData& data() { return *m_data; }

    void updateProjectionMatrix();

    BitmapTexturePool m_texturePool;
    bool m_isMaskMode { false };
    TransformationMatrix m_patternTransform;
    WrapMode m_wrapMode { WrapMode::Stretch };
    TextureMapperGLData* m_data;
    ClipStack m_clipStack;
};

} // namespace WebCore

#endif // USE(TEXTURE_MAPPER)
#endif
