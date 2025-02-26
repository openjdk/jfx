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

#pragma once

#include <jni.h>
#include "PlatformJavaClasses.h"
#include "GraphicsContext.h"

extern jmethodID WCGM_getWCFont_mID;
extern jmethodID WCGM_createBufferedContext_mID;
extern jmethodID WCGM_createWCPath_mID;
extern jmethodID WCGM_createWCPath_L_mID;
extern jmethodID WCGM_createWCImage_mID;

extern jmethodID WCF_getXHeight_mID;
extern jmethodID WCF_getFontMetrics_mID;
extern jmethodID WCF_getGlyphCodes_mID;
extern jmethodID WCF_drawString_mID;
extern jmethodID WCF_getStringLength_mID;
extern jmethodID WCF_getStringBounds_mID;
extern jmethodID WCF_getGlyphWidth_mID;
extern jmethodID WCF_getOffsetForPosition_mID;
extern jmethodID WCF_hash_mID;
extern jmethodID WCF_compare_mID;
extern jmethodID WCF_getXHeight_mID;
extern jmethodID WCF_getAscent_mID;
extern jmethodID WCF_getDescent_mID;
extern jmethodID WCF_getHeight_mID;
extern jmethodID WCF_hasUniformLineMetrics_mID;
extern jmethodID WCGC_beginPaint_mID;
extern jmethodID WCGC_endPaint_mID;
extern jmethodID WCGC_getImage_mID;
extern jmethodID WCGC_drawImage_mID;
extern jmethodID WCGC_drawIcon_mID;
extern jmethodID WCGC_drawPattern_mID;

extern jmethodID WCP_contains_mID;
extern jmethodID WCP_clear_mID;
extern jmethodID WCP_moveTo_mID;
extern jmethodID WCP_addLineTo_mID;
extern jmethodID WCP_addBezierCurveTo_mID;
extern jmethodID WCP_addArcTo_mID;
extern jmethodID WCP_closeSubpath_mID;
extern jmethodID WCP_addArc_mID;
extern jmethodID WCP_addRect_mID;
extern jmethodID WCP_addEllipse_mID;

namespace WebCore {

class WEBCORE_EXPORT GraphicsContextJava final : public GraphicsContext {

public:
    GraphicsContextJava(PlatformGraphicsContext* context);
    virtual ~GraphicsContextJava();

    bool hasPlatformContext() const override { return true; }
    PlatformGraphicsContext* platformContext() override;

    void savePlatformState();
    void restorePlatformState();
    void save(GraphicsContextState::Purpose = GraphicsContextState::Purpose::SaveRestore) override;
    void restore(GraphicsContextState::Purpose = GraphicsContextState::Purpose::SaveRestore) override;

    void drawRect(const FloatRect& rect, float) override;
    void drawLine(const FloatPoint& point1, const FloatPoint& point2) override;
    void drawEllipse(const FloatRect& rect) override;
    void drawFocusRing(const Path&, float outlineWidth, const Color&) override;
    void drawFocusRing(const Vector<FloatRect>& rects, float outlineOffset, float outlineWidth, const Color& color) override;
    void drawLinesForText(const FloatPoint& origin, float thickness, const DashArray& widths, bool printing, bool, StrokeStyle) override;
    void drawLineForText(const FloatRect& rect, bool printing, bool doubleLines, StrokeStyle stroke);
    void drawDotsForDocumentMarker(const FloatRect& rect, DocumentMarkerLineStyle style) override;
    void drawPlatformImage(const PlatformImagePtr& image, const FloatRect& destRect, const FloatRect& srcRect,
                            ImagePaintingOptions options); //-> Seems like renamed now to drawNativeImage
    void drawPlatformPattern(const PlatformImagePtr& image, const FloatRect& destRect, const FloatRect& tileRect,
                                const AffineTransform& patternTransform, const FloatPoint& phase, const FloatSize&, ImagePaintingOptions);
                                // > Seems like renamed to drawPattern

    void fillRect(const FloatRect&, RequiresClipToRect = RequiresClipToRect::Yes) override;
    void fillRect(const FloatRect& rect, const Color& color) override;
    void fillRect(const FloatRect&, Gradient&, const AffineTransform&, RequiresClipToRect = RequiresClipToRect::Yes) override;
    void fillPath(const Path& path) override;
    void fillRoundedRect(const FloatRoundedRect& rect, const Color& color, BlendMode) override;
    void fillRectWithRoundedHole(const FloatRect& frect, const FloatRoundedRect& roundedHoleRect, const Color& color) override;

    void strokeRect(const FloatRect& rect, float lineWidth) override;
    void strokePath(const Path& path) override;

    void resetClip() override;
    void clip(const FloatRect& rect) override;
    IntRect clipBounds() const override;
    void clipToImageBuffer(ImageBuffer&, const FloatRect&) override;
    void clipPath(const Path &path, WindRule) override;
    void clipOut(const Path& path) override;
    void clipOut(const FloatRect& rect) override;
    void clearRect(const FloatRect& rect) override;
    void canvasClip(const Path& path, WindRule fillRule);
    void beginTransparencyLayer(float opacity) override;
    void endTransparencyLayer() override;

    void translate(float x, float y) override;
    void rotate(float radians) override;
    void scale(const FloatSize& size) override;

    void concatCTM(const AffineTransform& at) override;
    void setCTM(const AffineTransform& tm) override;
    AffineTransform getCTM(IncludeDeviceScale) const override;

    void setLineDash(const DashArray& dashes, float dashOffset) override;
    void setLineCap(LineCap cap) override;
    void setLineJoin(LineJoin join) override;
    void setMiterLimit(float limit) override;
    void setPlatformFillColor(const Color& color);
    void setPlatformTextDrawingMode(TextDrawingModeFlags mode);
    void setPlatformShadow(const FloatSize& s, float blur, const Color& color);
    void setPlatformStrokeStyle(StrokeStyle style);
    void setPlatformStrokeColor(const Color& color);
    void setPlatformStrokeThickness(float strokeThickness);
    void setPlatformImageInterpolationQuality(InterpolationQuality);
    void setPlatformShouldAntialias(bool);
    void setPlatformAlpha(float alpha);
    void setPlatformCompositeOperation(CompositeOperator op, BlendMode);
    void setURLForRect(const URL&, const FloatRect&) override;

    PlatformGraphicsContext* m_platformContext;

    void didUpdateState(GraphicsContextState&) override;
    void fillRoundedRectImpl(const FloatRoundedRect&, const Color&) override;
    void drawNativeImageInternal(NativeImage&, const FloatRect& destRect, const FloatRect& srcRect, ImagePaintingOptions = { }) override;
    /*void drawPattern(NativeImage&, const FloatSize& imageSize, const FloatRect& destRect, const FloatRect& tileRect,
                            const AffineTransform& patternTransform, const FloatPoint& phase, const FloatSize& spacing,
                            const ImagePaintingOptions& = { }) override;
    */
     void drawPattern(NativeImage&, const FloatRect& destRect, const FloatRect& tileRect, const AffineTransform& patternTransform,
                             const FloatPoint& phase, const FloatSize& spacing, ImagePaintingOptions = { }) override;
};

}
