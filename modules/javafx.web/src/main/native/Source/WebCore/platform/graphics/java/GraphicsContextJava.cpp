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

#include <math.h>
#include <stdio.h>
#include <wtf/MathExtras.h>
#include <variant>
#include <wtf/Vector.h>

#include "AffineTransform.h"
#include "DisplayListRecorder.h"
#include "Color.h"
#include "FloatRect.h"
#include "FloatSize.h"
#include "FloatRoundedRect.h"
#include "Font.h"
#include "FontRanges.h"
#include "GraphicsContext.h"
#include "GraphicsContextJava.h"
#include "Gradient.h"
#include "IntRect.h"
#include "ImageBuffer.h"
#include "PlatformJavaClasses.h"
#include "Logging.h"
#include "NotImplemented.h"
#include "Path.h"
#include "Pattern.h"
#include "PlatformContextJava.h"
#include "RenderingQueue.h"
#include "Font.h"
#include "TransformationMatrix.h"

#include "com_sun_webkit_graphics_GraphicsDecoder.h"
#include "com_sun_webkit_graphics_WCPath.h"


#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

namespace WebCore {

static void setGradient(Gradient &gradient,
    const AffineTransform& gradientSpaceTransformation, PlatformGraphicsContext* context, jint id)
{
    const Vector<GradientColorStop, 2> stops = gradient.stops().stops();
    int nStops = stops.size();

    FloatPoint p0, p1;
    float startRadius, endRadius;
    bool isRadialGradient = true;
    WTF::switchOn(gradient.data(),
            [&] (const Gradient::LinearData& data) -> void {
                isRadialGradient = false;
                p0 = data.point0;
                p1 = data.point1;
            },
            [&] (const Gradient::RadialData& data) -> void {
                p0 = data.point0;
                p1 = data.point1;
                startRadius = data.startRadius;
                endRadius = data.endRadius;
            },
            [&] (const Gradient::ConicData&) -> void {
                notImplemented();
            }
    );

    p0 = gradientSpaceTransformation.mapPoint(p0);
    p1 = gradientSpaceTransformation.mapPoint(p1);

    context->rq().freeSpace(4 * 11 + 20 * nStops)
    << id
    << (jfloat)p0.x()
    << (jfloat)p0.y()
    << (jfloat)p1.x()
    << (jfloat)p1.y()
    << (jint)isRadialGradient;

    if (isRadialGradient) {
        context->rq()
        << (jfloat)(gradientSpaceTransformation.xScale() * startRadius)
        << (jfloat)(gradientSpaceTransformation.xScale() * endRadius);
    }
    context->rq()
    << (jint)0 //is not proportional
    << (jint)gradient.spreadMethod()
    << (jint)nStops;

    for (const auto& cs : stops) {
        auto [r, g, b, a] = cs.color.toColorTypeLossy<SRGBA<float>>().resolved();
        context->rq()
        << r << g << b << a << (jfloat)cs.offset;
    }
}

static void flushImageRQ(PlatformGraphicsContext* context, const PlatformImagePtr& image)
{
    if (!image || !image->getRenderingQueue())
        return;

    auto rq = image->getRenderingQueue();

    if (!rq->isEmpty()) {
        // 1. Drawing is flushed to the buffered image's RenderQueue.
        rq->flushBuffer();

        // 2. The buffered image's RenderQueue is to be decoded.
        context->rq().freeSpace(8)
        << (jint)com_sun_webkit_graphics_GraphicsDecoder_DECODERQ
        << rq->getRQRenderingQueue();
    }
}

GraphicsContextJava::GraphicsContextJava(PlatformGraphicsContext* context) // TODO-java: , bool shouldUseContextColors) // todo tav new param
{
    m_platformContext = context;
}

PlatformGraphicsContext* GraphicsContextJava::platformContext()
{
    return m_platformContext;
}

GraphicsContextJava::~GraphicsContextJava()
{
    delete m_platformContext;
}

void GraphicsContextJava::save(GraphicsContextState::Purpose) {
    GraphicsContext::save();
    savePlatformState();
}

void GraphicsContextJava::savePlatformState()
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(4)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SAVESTATE;
}

void GraphicsContextJava::restore(GraphicsContextState::Purpose) {
    GraphicsContext::restore();
    restorePlatformState();
}

void GraphicsContextJava::restorePlatformState()
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(4)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_RESTORESTATE;
}

// Draws a filled rectangle with a stroked border.
void GraphicsContextJava::drawRect(const FloatRect& rect, float borderThickness = 1) // todo tav rect changed from IntRect to FloatRect
{
    UNUSED_PARAM(borderThickness);
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(20)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWRECT
    << (jint)rect.x() << (jint)rect.y() << (jint)rect.width() << (jint)rect.height();
}

// This is only used to draw borders.
void GraphicsContextJava::drawLine(const FloatPoint& point1, const FloatPoint& point2) // todo tav points changed from IntPoint to FloatPoint
{
    if (paintingDisabled() || strokeStyle() == StrokeStyle::NoStroke)
        return;

    platformContext()->rq().freeSpace(20)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWLINE
    << (jint)point1.x() << (jint)point1.y() << (jint)point2.x() << (jint)point2.y();
}

// This method is only used to draw the little circles used in lists.
void GraphicsContextJava::drawEllipse(const FloatRect& rect)
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(20)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWELLIPSE
    << (jint)rect.x() << (jint)rect.y() << (jint)rect.width() << (jint)rect.height(); // TODO-java: float to int conversion
}

// FIXME: This function needs to be adjusted to match the functionality on the Mac side.
//void GraphicsContextJava::strokeArc(const IntRect& rect, int startAngle, int angleSpan)
//{
//    if (paintingDisabled() || strokeStyle() == NoStroke)
//        return;
//
//    platformContext()->rq().freeSpace(28)
//    << (jint)com_sun_webkit_graphics_GraphicsDecoder_STROKEARC
//    << (jint)rect.x() << (jint)rect.y() << (jint)rect.width() << (jint)rect.height()
//    << (jint)startAngle << (jint)angleSpan;
//}

void GraphicsContextJava::fillRect(const FloatRect& rect, const Color& color)
{
    if (paintingDisabled())
        return;

    auto [r, g, b, a] = color.toColorTypeLossy<SRGBA<float>>().resolved();
    platformContext()->rq().freeSpace(36)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_FILLRECT_FFFFI
    << rect.x() << rect.y()
    << rect.width() << rect.height()
    << r << g << b << a;
}

void GraphicsContextJava::fillRect(const FloatRect& rect, RequiresClipToRect requiresClip)
{
    if (paintingDisabled())
        return;

    if (fillPattern()) {
        auto image = fillPattern()->tileNativeImage()->platformImage();

        FloatRect destRect(rect.x(), rect.y(),
            fillPattern()->repeatX() ? rect.width() : image->size().width(),
            fillPattern()->repeatY() ? rect.height() : image->size().height());
        drawPlatformPattern(image, destRect,
            FloatRect(0., 0., image->size().width(), image->size().height()),
            fillPattern()->patternSpaceTransform(), FloatPoint(), FloatSize(), {CompositeOperator::Copy});
    } else {
        if (fillGradient()) {
            setGradient(
                *fillGradient(),
                fillGradientSpaceTransform(),
                platformContext(),
                com_sun_webkit_graphics_GraphicsDecoder_SET_FILL_GRADIENT);
        }

        platformContext()->rq().freeSpace(20)
        << (jint)com_sun_webkit_graphics_GraphicsDecoder_FILLRECT_FFFF
        << rect.x() << rect.y()
        << rect.width() << rect.height();
    }
}

void GraphicsContextJava::fillRect(const FloatRect&, Gradient&, const AffineTransform&, RequiresClipToRect RequiresClipToRect)
{
    notImplemented();
}

void GraphicsContextJava::resetClip()
{
    notImplemented();
}

void GraphicsContextJava::clip(const FloatRect& rect)
{
    if (paintingDisabled())
        return;

    m_state.clipBounds.intersect(m_state.transform.mapRect(rect));
    platformContext()->rq().freeSpace(20)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SETCLIP_IIII
    << (jint)rect.x() << (jint)rect.y() << (jint)rect.width() << (jint)rect.height();
}

IntRect GraphicsContextJava::clipBounds() const
{
    // Transformation has inverse effect on clip bounds.
    return enclosingIntRect(m_state
                                .transform
                                .inverse()
                                .value_or(AffineTransform())
                                .mapRect(m_state.clipBounds));
}

void GraphicsContextJava::clipToImageBuffer(ImageBuffer&, const FloatRect&)
{

}

void GraphicsContextJava::drawFocusRing(const Path&, float, const Color&)
{
    //utaTODO: IMPLEMENT!!!
}

void GraphicsContextJava::drawFocusRing(const Vector<FloatRect>& rects, float offset, float, const Color& color)
{
    if (paintingDisabled())
        return;

    unsigned rectCount = rects.size();
    // We can't draw all the focus rects because webkit can have several rings
    // nested into each other. We can't draw a union of all the rects as well
    // as it results in the problems like 6683162. An alternative could be to
    // construct a Path object, add all the focus rings to it and then
    // "flatten" it, but it can only be done with Area classes which are not
    // available here. That's why a simple algorithm here: unite all the
    // intersecting rects, while leaving standalone rects as is.
    Vector<IntRect> toDraw;
    for (unsigned i = 0; i < rectCount; i++) {
        IntRect focusRect = enclosingIntRect(rects[i]);
        focusRect.inflate(offset);
        bool needAdd = true;
        for (size_t j = 0; j < toDraw.size(); j++) {
            IntRect rect = toDraw[j];
            if (rect.contains(focusRect)) {
                needAdd = false;
                break;
            } else if (focusRect.contains(rect)) {
                toDraw.remove(j);
            } else if (rect.intersects(focusRect)) {
                focusRect.unite(rect);
                toDraw.remove(j);
            }
        }
        if (needAdd) {
            toDraw.append(focusRect);
        }
    }

    platformContext()->rq().freeSpace(36 * toDraw.size());
    for (size_t i = 0; i < toDraw.size(); i++) {
        IntRect focusRect = toDraw[i];
        auto [r, g, b, a] = color.toColorTypeLossy<SRGBA<float>>().resolved();
        platformContext()->rq() << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWFOCUSRING
        << (jint)focusRect.x() << (jint)focusRect.y()
        << (jint)focusRect.width() << (jint)focusRect.height()
        << r << g << b << a;
    }
}

void GraphicsContextJava::drawLinesForText(const FloatPoint& origin, float thickness, const DashArray& widths, bool, bool, StrokeStyle stroke) {

    if (paintingDisabled())
        return;

    if (widths.size() == 0)
        return;

    // This is a workaround for http://bugs.webkit.org/show_bug.cgi?id=15659
    StrokeStyle savedStrokeStyle = strokeStyle();
    setStrokeStyle(stroke);
    float savedStrokeThickness = strokeThickness();
    setStrokeThickness(thickness);

    FloatPoint startPoint = origin + FloatPoint(0, thickness / 2);
    FloatPoint endPoint = startPoint + FloatPoint(widths.last(), 0);
    drawLine(
        IntPoint(startPoint.x(), startPoint.y()),
        IntPoint(endPoint.x(), endPoint.y()));

    setStrokeStyle(savedStrokeStyle);
    setStrokeThickness(savedStrokeThickness);

}

void GraphicsContextJava::drawLineForText(const FloatRect& rect, bool printing, bool doubleLines, StrokeStyle stroke)
{
    drawLinesForText(rect.location(), rect.height(), { rect.width() }, printing, doubleLines, stroke);
}

static inline void drawLineTo(GraphicsContext &gc, IntPoint &curPos, double x, double y)
{
    IntPoint endPoint(x, y);
    gc.drawLine(curPos, endPoint);
    curPos = endPoint;
}

//
// Draws an error underline that looks like one of:
//
//              H       E                H
//     /\      /\      /\        /\      /\               -
//   A/  \    /  \    /  \     A/  \    /  \              |
//    \   \  /    \  /   /D     \   \  /    \             |
//     \   \/  C   \/   /        \   \/   C  \            | height = heightSquares * square
//      \      /\  F   /          \  F   /\   \           |
//       \    /  \    /            \    /  \   \G         |
//        \  /    \  /              \  /    \  /          |
//         \/      \/                \/      \/           -
//         B                         B
//         |---|
//       unitWidth = (heightSquares - 1) * square
//
// The x, y, width, height passed in give the desired bounding box;
// x/width are adjusted to make the underline a integer number of units
// wide.
//
static inline void drawErrorUnderline(GraphicsContext &gc, double x, double y, double width, double height)
{
    static const double heightSquares = 2.5;

    double square = height / heightSquares;
    double halfSquare = 0.5 * square;

    double unitWidth = (heightSquares - 1.0) * square;
    int widthUnits = static_cast<int>((width + 0.5 * unitWidth) / unitWidth);

    x += 0.5 * (width - widthUnits * unitWidth);
    width = widthUnits * unitWidth;

    double bottom = y + height;
    double top = y;

    // Bottom of squiggle
    IntPoint curPos(x - halfSquare, top + halfSquare); // A

    int i = 0;
    for (i = 0; i < widthUnits; i += 2) {
        double middle = x + (i + 1) * unitWidth;
        double right = x + (i + 2) * unitWidth;

        drawLineTo(gc, curPos, middle, bottom); // B

        if (i + 2 == widthUnits)
            drawLineTo(gc, curPos, right + halfSquare, top + halfSquare); // D
        else if (i + 1 != widthUnits)
            drawLineTo(gc, curPos, right, top + square); // C
    }

    // Top of squiggle
    for (i -= 2; i >= 0; i -= 2) {
        double left = x + i * unitWidth;
        double middle = x + (i + 1) * unitWidth;
        double right = x + (i + 2) * unitWidth;

        if (i + 1 == widthUnits)
            drawLineTo(gc, curPos, middle + halfSquare, bottom - halfSquare); // G
        else {
            if (i + 2 == widthUnits)
                drawLineTo(gc, curPos, right, top); // E

            drawLineTo(gc, curPos, middle, bottom - halfSquare); // F
        }

        drawLineTo(gc, curPos, left, top); // H
    }
}

void GraphicsContextJava::drawDotsForDocumentMarker(const FloatRect& rect, DocumentMarkerLineStyle style)
{
    savePlatformState(); //fake stroke
    switch (style.mode) { // TODO-java: DocumentMarkerAutocorrectionReplacementLineStyle not handled in switch
        case DocumentMarkerLineStyleMode::Spelling:
        {
            static Color red = SRGBA<uint8_t> { 255, 0, 0 };
            setStrokeColor(red);
        }
        break;
        case DocumentMarkerLineStyleMode::Grammar:
        {
            static Color green = SRGBA<uint8_t> { 0, 255, 0 };
            setStrokeColor(green);
        }
        break;
    default:
        {
        }
    }
    drawErrorUnderline(*this, rect.x(), rect.y(), rect.width(), rect.height());
    restorePlatformState(); //fake stroke
}

void GraphicsContextJava::translate(float x, float y)
{
    if (paintingDisabled())
        return;

    m_state.transform.translate(x, y);
    platformContext()->rq().freeSpace(12)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_TRANSLATE
    << x << y;
}

void GraphicsContextJava::setPlatformFillColor(const Color& color)
{
    if (paintingDisabled())
        return;

    auto [r, g, b, a] = color.toColorTypeLossy<SRGBA<float>>().resolved();
    platformContext()->rq().freeSpace(20)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SETFILLCOLOR
    << r << g << b << a;
}

void GraphicsContextJava::setPlatformTextDrawingMode(TextDrawingModeFlags mode)
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(16)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SET_TEXT_MODE
    << (jint)(mode.contains(TextDrawingMode::Fill))
    << (jint)(mode.contains(TextDrawingMode::Stroke))
    << (jint)0;
    //utatodo:
    //<< (jint)(mode & TextModeClip);
}

void GraphicsContextJava::setPlatformStrokeStyle(StrokeStyle style)
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(8)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SETSTROKESTYLE
    << (jint)style;
}

void GraphicsContextJava::setPlatformStrokeColor(const Color& color)
{
    if (paintingDisabled())
        return;

    auto [r, g, b, a] = color.toColorTypeLossy<SRGBA<float>>().resolved();
    platformContext()->rq().freeSpace(20)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SETSTROKECOLOR
    << r << g << b << a;
}

void GraphicsContextJava::setPlatformStrokeThickness(float strokeThickness)
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(8)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SETSTROKEWIDTH
    << strokeThickness;
}

void GraphicsContextJava::setPlatformImageInterpolationQuality(InterpolationQuality)
{
    notImplemented();
}

void GraphicsContextJava::setPlatformShouldAntialias(bool)
{
    notImplemented();
}

void GraphicsContextJava::setURLForRect(const URL&, const FloatRect&)
{
    notImplemented();
}

void GraphicsContextJava::concatCTM(const AffineTransform& at)
{
    if (paintingDisabled())
        return;

    m_state.transform.multiply(at);
    platformContext()->rq().freeSpace(28)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_CONCATTRANSFORM_FFFFFF
    << (float)at.a() << (float)at.b() << (float)at.c() << (float)at.d() << (float)at.e() << (float)at.f();
}

//void GraphicsContextJava::addInnerRoundedRectClip(const IntRect& r, int thickness)
//{
//    if (paintingDisabled())
//        return;
//
//    FloatRect rect(r);
//    Path path;
//    path.addEllipse(rect);
//    rect.inflate(-thickness);
//    path.addEllipse(rect);
//    clipPath(path, WindRule::EvenOdd);
//}

void GraphicsContextJava::setPlatformShadow(const FloatSize& s, float blur, const Color& color)
{
    if (paintingDisabled())
        return;

    float width = s.width();
    float height = s.height();
#if USE(CG)
    if (shadowsIgnoreTransforms()) {
        // Meaning that this graphics context is associated with a CanvasRenderingContext
        // We flip the height since JavaFX Prism and HTML5 Canvas have opposite Y axis
        height = -height;
    }
#endif

    auto [r, g, b, a] = color.toColorTypeLossy<SRGBA<float>>().resolved();
    platformContext()->rq().freeSpace(32)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SETSHADOW
    << width << height << blur << r << g << b << a;;
}

void GraphicsContextJava::beginTransparencyLayer(float opacity)
{
    GraphicsContext::beginTransparencyLayer(opacity);

    if (paintingDisabled())
      return;

    platformContext()->rq().freeSpace(8)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_BEGINTRANSPARENCYLAYER
    << opacity;
}

void GraphicsContextJava::endTransparencyLayer()
{
    if (paintingDisabled())
      return;

    platformContext()->rq().freeSpace(4)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_ENDTRANSPARENCYLAYER;

    GraphicsContext::endTransparencyLayer();
}

void GraphicsContextJava::clearRect(const FloatRect& rect)
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(20)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_CLEARRECT_FFFF
    << rect.x() << rect.y()
    << rect.width() << rect.height();
}

void GraphicsContextJava::strokeRect(const FloatRect& rect, float lineWidth)
{
    if (paintingDisabled())
        return;

    if (strokeGradient()) {
        setGradient(
            *strokeGradient(),
            strokeGradientSpaceTransform(),
            platformContext(),
            com_sun_webkit_graphics_GraphicsDecoder_SET_STROKE_GRADIENT);
    }

    platformContext()->rq().freeSpace(24)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_STROKERECT_FFFFF
    << rect.x() << rect.y() << rect.width() << rect.height() << lineWidth;
}

void GraphicsContextJava::setLineDash(const DashArray& dashes, float dashOffset)
{
    if (paintingDisabled()) {
      return;
    }
    size_t size = dashes.size();

    platformContext()->rq().freeSpace((3 + size) * 4)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SET_LINE_DASH
    << dashOffset
    << (jint)size;

    for (size_t i = 0; i < size; i++) {
        platformContext()->rq()
        << (float) dashes.at(i);
    }

    platformContext()->setLineDash(dashes, dashOffset);
}

void GraphicsContextJava::setLineCap(LineCap cap)
{
    if (paintingDisabled()) {
      return;
    }

    platformContext()->rq().freeSpace(8)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SET_LINE_CAP
    << (jint)cap;

    platformContext()->setLineCap(cap);
}

void GraphicsContextJava::setLineJoin(LineJoin join)
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(8)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SET_LINE_JOIN
    << (jint)join;

    platformContext()->setLineJoin(join);
}

void GraphicsContextJava::setMiterLimit(float limit)
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(8)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SET_MITER_LIMIT
    << (jfloat)limit;

    platformContext()->setMiterLimit(limit);
}

void GraphicsContextJava::setPlatformAlpha(float alpha)
{
    platformContext()->rq().freeSpace(8)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SETALPHA
    << alpha;
}

void GraphicsContextJava::setPlatformCompositeOperation(CompositeOperator op, BlendMode)
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(8)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SETCOMPOSITE
    << (jint)op;
    //utatodo: add BlendMode
}

void GraphicsContextJava::strokePath(const Path& path)
{
    if (paintingDisabled())
        return;

    if (strokeGradient()) {
        setGradient(
            *strokeGradient(),
            strokeGradientSpaceTransform(),
            platformContext(),
            com_sun_webkit_graphics_GraphicsDecoder_SET_STROKE_GRADIENT);
    }

    platformContext()->rq().freeSpace(12)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_STROKE_PATH
    << copyPath(path.platformPath())
    << (jint)fillRule();
}

static void setClipPath(
    GraphicsContext &gc,
    GraphicsContextState& state,
    const Path& path,
    WindRule wrule,
    bool isOut)
{
    if (gc.paintingDisabled() || path.isEmpty())
        return;

    state.clipBounds.intersect(state.transform.mapRect(path.fastBoundingRect()));
    gc.platformContext()->rq().freeSpace(16)
    << jint(com_sun_webkit_graphics_GraphicsDecoder_CLIP_PATH)
    << copyPath(path.platformPath())
    << jint(wrule == WindRule::EvenOdd
       ? com_sun_webkit_graphics_WCPath_RULE_EVENODD
       : com_sun_webkit_graphics_WCPath_RULE_NONZERO)
    << jint(isOut);
}

void GraphicsContextJava::canvasClip(const Path& path, WindRule fillRule)
{
    clipPath(path, fillRule);
}

void GraphicsContextJava::clipPath(const Path &path, WindRule wrule)
{
    setClipPath(*this, m_state, path, wrule, false);
}

void GraphicsContextJava::clipOut(const Path& path)
{
    setClipPath(*this, m_state, path, WindRule::EvenOdd, true);
}

void GraphicsContextJava::clipOut(const FloatRect& rect)
{
    Path path;
    path.addRoundedRect(rect, FloatSize());
    clipOut(path);
}

void GraphicsContextJava::drawPlatformImage(const PlatformImagePtr& image, const FloatRect& destRect, const FloatRect& srcRect, ImagePaintingOptions options)
{
    if (!image || !image->getImage())
        return;

    savePlatformState();
    setCompositeOperation(options.compositeOperator(), options.blendMode());

    FloatRect adjustedSrcRect(srcRect);
    FloatRect adjustedDestRect(destRect);

    if (options.orientation() != ImageOrientation::Orientation::None) {
        // ImageOrientation expects the origin to be at (0, 0).
        translate(destRect.x(), destRect.y());
        adjustedDestRect.setLocation(FloatPoint());
        concatCTM(options.orientation().transformFromDefault(adjustedDestRect.size()));
        if (options.orientation().usesWidthAsHeight()) {
            // The destination rectangle will have it's width and height already reversed for the orientation of
            // the image, as it was needed for page layout, so we need to reverse it back here.
            adjustedDestRect.setSize(adjustedDestRect.size().transposedSize());
        }
    }

    platformContext()->rq().freeSpace(72)
        << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWIMAGE
        << image->getImage()
        << adjustedDestRect.x() << adjustedDestRect.y()
        << adjustedDestRect.width() << adjustedDestRect.height()
        << adjustedSrcRect.x() << adjustedSrcRect.y()
        << adjustedSrcRect.width() << adjustedSrcRect.height();
    restorePlatformState();
}

void GraphicsContextJava::drawPlatformPattern(const PlatformImagePtr& image, const FloatRect& destRect, const FloatRect& tileRect, const AffineTransform& patternTransform, const FloatPoint& phase, const FloatSize&,ImagePaintingOptions)
{
    if (paintingDisabled() || !patternTransform.isInvertible())
        return;

    JNIEnv* env = WTF::GetJavaEnv();

    if (tileRect.isEmpty()) {
        return;
    }

    flushImageRQ(platformContext(), image);

    TransformationMatrix tm = patternTransform.toTransformationMatrix();

    static jmethodID mid = env->GetMethodID(PG_GetGraphicsManagerClass(env),
                "createTransform",
                "(DDDDDD)Lcom/sun/webkit/graphics/WCTransform;");
    ASSERT(mid);
    JLObject transform(env->CallObjectMethod(PL_GetGraphicsManager(env), mid,
                tm.a(), tm.b(), tm.c(), tm.d(), tm.e(), tm.f()));
    ASSERT(transform);
    WTF::CheckAndClearException(env);

    platformContext()->rq().freeSpace(13 * 4)
        << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWPATTERN
        << image->getImage()
        << tileRect.x() << tileRect.y() << tileRect.width() << tileRect.height()
        << RQRef::create(transform)
        << phase.x() << phase.y()
        << destRect.x() << destRect.y() << destRect.width() << destRect.height();
}

void GraphicsContextJava::fillPath(const Path& path)
{
    if (paintingDisabled())
        return;

    if (fillPattern()) {
        savePlatformState(); //fake clip isolation
        clipPath(path, fillRule());
        FloatRect rect(path.boundingRect());

        auto image = fillPattern()->tileNativeImage()->platformImage();

        FloatRect destRect(rect.x(), rect.y(),
            fillPattern()->repeatX() ? rect.width() : image->size().width(),
            fillPattern()->repeatY() ? rect.height() : image->size().height());
        drawPlatformPattern(image, destRect,
            FloatRect(0., 0., image->size().width(), image->size().height()),
            fillPattern()->patternSpaceTransform(), FloatPoint(), FloatSize(), {CompositeOperator::Copy});

        restorePlatformState();
    } else {
        if (fillGradient()) {
            setGradient(
                *fillGradient(),
                fillGradientSpaceTransform(),
                platformContext(),
                com_sun_webkit_graphics_GraphicsDecoder_SET_FILL_GRADIENT);
        }

        platformContext()->rq().freeSpace(12)
        << (jint)com_sun_webkit_graphics_GraphicsDecoder_FILL_PATH
        << copyPath(path.platformPath())
        << (jint)fillRule();
    }
}

void GraphicsContextJava::rotate(float radians)
{
    if (paintingDisabled())
        return;

    m_state.transform.rotate(radians);
    platformContext()->rq().freeSpace(2 * 4)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_ROTATE
    << radians;

}

void GraphicsContextJava::scale(const FloatSize& size)
{
    if (paintingDisabled())
        return;

    m_state.transform.scale(size.width(), size.height());
    platformContext()->rq().freeSpace(12)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SCALE
    << size.width() << size.height();
}

void GraphicsContextJava::fillRoundedRect(const FloatRoundedRect& rect, const Color& color, BlendMode blendMode) // todo tav Int to Float
{
    UNUSED_PARAM(blendMode);
    if (paintingDisabled())
        return;

    if (rect.radii().topLeft().width() == rect.radii().topRight().width() &&
        rect.radii().topRight().width() == rect.radii().bottomRight().width() &&
        rect.radii().bottomRight().width() == rect.radii().bottomLeft().width() &&
        rect.radii().topLeft().height() == rect.radii().topRight().height() &&
        rect.radii().topRight().height() == rect.radii().bottomRight().height() &&
        rect.radii().bottomRight().height() == rect.radii().bottomLeft().height()) {
        auto [r, g, b, a] = color.toColorTypeLossy<SRGBA<float>>().resolved();
        platformContext()->rq().freeSpace(68)
        << (jint)com_sun_webkit_graphics_GraphicsDecoder_FILL_ROUNDED_RECT
        << (jfloat)rect.rect().x() << (jfloat)rect.rect().y()
        << (jfloat)rect.rect().width() << (jfloat)rect.rect().height()
        << (jfloat)rect.radii().topLeft().width() << (jfloat)rect.radii().topLeft().height()
        << (jfloat)rect.radii().topRight().width() << (jfloat)rect.radii().topRight().height()
        << (jfloat)rect.radii().bottomLeft().width() << (jfloat)rect.radii().bottomLeft().height()
        << (jfloat)rect.radii().bottomRight().width() << (jfloat)rect.radii().bottomRight().height()
        << r << g << b << a;
    }
    else {
        WindRule oldFillRule = fillRule();
        Color oldFillColor = fillColor();

        setFillRule(WindRule::EvenOdd);
        setFillColor(color);

        Path roundedRectPath;
        roundedRectPath.addRoundedRect(rect);
        fillPath(roundedRectPath);

        setFillRule(oldFillRule);
        setFillColor(oldFillColor);
    }
}

void GraphicsContextJava::fillRectWithRoundedHole(const FloatRect& frect, const FloatRoundedRect& roundedHoleRect, const Color& color)
{
    if (paintingDisabled())
        return;

    const IntRect rect = enclosingIntRect(frect);
    Path path;
    path.addRect(rect);

    if (!roundedHoleRect.radii().isZero())
        path.addRoundedRect(roundedHoleRect);
    else
        path.addRect(roundedHoleRect.rect());

    WindRule oldFillRule = fillRule();
    Color oldFillColor = fillColor();

    setFillRule(WindRule::EvenOdd);
    setFillColor(color);

    fillPath(path);

    setFillRule(oldFillRule);
    setFillColor(oldFillColor);
}

//utatodo: do we need the Java-only m_state.transform?
AffineTransform GraphicsContextJava::getCTM(IncludeDeviceScale) const
{
    return m_state.transform;
}

void GraphicsContextJava::didUpdateState(GraphicsContextState& state)
{
    if (state.changes() & GraphicsContextState::Change::StrokeThickness) {
        setPlatformStrokeThickness(strokeThickness());
    }

    if (state.changes() & GraphicsContextState::Change::StrokeStyle) {
        setPlatformStrokeStyle(strokeStyle());
    }

    if (state.changes() & GraphicsContextState::Change::TextDrawingMode){
        setPlatformTextDrawingMode(textDrawingMode());
    }

    if (state.changes() & GraphicsContextState::Change::CompositeMode) {
        setPlatformCompositeOperation(compositeOperation(), blendMode());
    }

    if (state.changes() & GraphicsContextState::Change::StrokeBrush) {
        setPlatformStrokeColor(strokeColor());
    }

    if (state.changes() & GraphicsContextState::Change::Alpha) {
        setPlatformAlpha(alpha());
    }

    if (state.changes() & GraphicsContextState::Change::DropShadow) {
        auto dropShadowOpt = state.dropShadow();
        if (dropShadowOpt.has_value()) {
            const auto& dropShadow = dropShadowOpt.value();
            setPlatformShadow(dropShadow.offset,dropShadow.radius, dropShadow.color);
        } else {
            float clr = 0.0f;
            platformContext()->rq().freeSpace(32)
            << (jint)com_sun_webkit_graphics_GraphicsDecoder_SETSHADOW
            << clr << clr << clr << clr << clr << clr << clr;
        }
    }

    if (state.changes() & GraphicsContextState::Change::FillBrush) {
        setPlatformFillColor(fillColor());
    }
}

void GraphicsContextJava::fillRoundedRectImpl(const FloatRoundedRect& rect, const Color& color)
{
    fillRoundedRect(rect, color, BlendMode::Normal);
}

void GraphicsContextJava::drawNativeImageInternal(NativeImage& image, const FloatRect& destRect, const FloatRect& srcRect, ImagePaintingOptions options)
{
    /* flush ImageRq  to decode previous recorded  command buffer */
    flushImageRQ(platformContext(), image.platformImage());
    drawPlatformImage(image.platformImage(), destRect, srcRect, options);
}

/*void GraphicsContextJava::drawPattern(NativeImage& image, const FloatSize& imageSize, const FloatRect& destRect, const FloatRect& tileRect,
                            const AffineTransform& patternTransform, const FloatPoint& phase, const FloatSize& spacing,
                            const ImagePaintingOptions& imagePaintingOptions)
*/
void GraphicsContextJava::drawPattern(NativeImage& image, const FloatRect& destRect, const FloatRect& tileRect, const AffineTransform& patternTransform,
                       const FloatPoint& phase, const FloatSize& spacing, ImagePaintingOptions imagePaintingOptions){
    drawPlatformPattern(image.platformImage(), destRect, tileRect, patternTransform, phase, spacing, imagePaintingOptions);
}

void GraphicsContextJava::setCTM(const AffineTransform& tm)
{
    if (paintingDisabled())
        return;

    m_state.transform = tm;
    platformContext()->rq().freeSpace(28)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SET_TRANSFORM
    << (float)tm.a() << (float)tm.b() << (float)tm.c() << (float)tm.d() << (float)tm.e() << (float)tm.f();
}

void Gradient::stopsChanged()
{
}

void Gradient::fill(GraphicsContext& gc, const FloatRect& rect)
{
    gc.setFillGradient(*this);
    gc.fillRect(rect);
}

} // namespace WebCore
