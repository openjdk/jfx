/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#if COMPILER(GCC)
#pragma GCC diagnostic ignored "-Wunused-parameter"
#endif

#include <math.h>
#include <stdio.h>
#include <wtf/MathExtras.h>
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
#include "JavaEnv.h"
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


static void setGradient(Gradient &gradient, PlatformGraphicsContext* context, jint id)
{
    Vector<Gradient::ColorStop, 2> stops = gradient.getStops(); // TODO-java: recheck;
    int nStops = stops.size();

    AffineTransform gt = gradient.gradientSpaceTransform();
    FloatPoint p0(gt.mapPoint(gradient.p0()));
    FloatPoint p1(gt.mapPoint(gradient.p1()));

    context->rq().freeSpace(4 * 11 + 8 * nStops)
    << id
    << (jfloat)p0.x()
    << (jfloat)p0.y()
    << (jfloat)p1.x()
    << (jfloat)p1.y()
    << (jint)gradient.isRadial();

    if (gradient.isRadial()) {
        context->rq()
        << (jfloat)(gt.xScale()*gradient.startRadius())
        << (jfloat)(gt.xScale()*gradient.endRadius());
    }
    context->rq()
    << (jint)0 //is not proportional
    << (jint)gradient.spreadMethod()
    << (jint)nStops;

    for (int i = 0; i < nStops; i++) {
        Gradient::ColorStop cs = stops[i];
        int rgba =
            ((int)(cs.alpha * 255 + 0.5)) << 24 |
            ((int)(cs.red   * 255 + 0.5)) << 16 |
            ((int)(cs.green * 255 + 0.5)) << 8 |
            ((int)(cs.blue  * 255 + 0.5));

        context->rq()
        << (jint)rgba << (jfloat)cs.stop;
    }
}

class GraphicsContextPlatformPrivate : public PlatformGraphicsContext {
};

void GraphicsContext::platformInit(PlatformGraphicsContext* context) // TODO-java: , bool shouldUseContextColors) // todo tav new param
{
    m_data = static_cast<GraphicsContextPlatformPrivate *>(context);
}

PlatformGraphicsContext* GraphicsContext::platformContext() const
{
    return m_data;
}

void GraphicsContext::platformDestroy()
{
    delete m_data;
}

void GraphicsContext::savePlatformState()
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(4)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SAVESTATE;
}

void GraphicsContext::restorePlatformState()
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(4)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_RESTORESTATE;
}

// Draws a filled rectangle with a stroked border.
void GraphicsContext::drawRect(const FloatRect& rect, float borderThickness) // todo tav rect changed from IntRect to FloatRect
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(20)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWRECT
    << (jint)rect.x() << (jint)rect.y() << (jint)rect.width() << (jint)rect.height();
}

// This is only used to draw borders.
void GraphicsContext::drawLine(const FloatPoint& point1, const FloatPoint& point2) // todo tav points changed from IntPoint to FloatPoint
{
    if (paintingDisabled() || strokeStyle() == NoStroke)
        return;

    platformContext()->rq().freeSpace(20)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWLINE
    << (jint)point1.x() << (jint)point1.y() << (jint)point2.x() << (jint)point2.y();
}

// This method is only used to draw the little circles used in lists.
void GraphicsContext::drawEllipse(const FloatRect& rect)
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(20)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWELLIPSE
    << (jint)rect.x() << (jint)rect.y() << (jint)rect.width() << (jint)rect.height(); // TODO-java: float to int conversion
}

// FIXME: This function needs to be adjusted to match the functionality on the Mac side.
//void GraphicsContext::strokeArc(const IntRect& rect, int startAngle, int angleSpan)
//{
//    if (paintingDisabled() || strokeStyle() == NoStroke)
//        return;
//
//    platformContext()->rq().freeSpace(28)
//    << (jint)com_sun_webkit_graphics_GraphicsDecoder_STROKEARC
//    << (jint)rect.x() << (jint)rect.y() << (jint)rect.width() << (jint)rect.height()
//    << (jint)startAngle << (jint)angleSpan;
//}

void GraphicsContext::fillRect(const FloatRect& rect, const Color& color)
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(24)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_FILLRECT_FFFFI
    << rect.x() << rect.y()
    << rect.width() << rect.height()
    << (jint)color.rgb();
}

void GraphicsContext::fillRect(const FloatRect& rect)
{
    if (paintingDisabled())
        return;

    if (m_state.fillPattern && m_state.fillPattern->tileImage()) {
        Image *img = m_state.fillPattern->tileImage();
        FloatRect destRect(
            rect.x(),
            rect.y(),
            m_state.fillPattern->repeatX() ? rect.width() : img->width(),
            m_state.fillPattern->repeatY() ? rect.height() : img->height());
        img->drawPattern(
            *this,
            FloatRect(0., 0., img->width(), img->height()),
            m_state.fillPattern->getPatternSpaceTransform(),
            FloatPoint(),
            FloatSize(),
            CompositeCopy, //any
            destRect);
    } else {
        if (m_state.fillGradient) {
            setGradient(
                *m_state.fillGradient,
                platformContext(),
                com_sun_webkit_graphics_GraphicsDecoder_SET_FILL_GRADIENT);
        }

        platformContext()->rq().freeSpace(20)
        << (jint)com_sun_webkit_graphics_GraphicsDecoder_FILLRECT_FFFF
        << rect.x() << rect.y()
        << rect.width() << rect.height();
    }
}

void GraphicsContext::clip(const FloatRect& rect)
{
    if (paintingDisabled())
        return;

    m_state.clipBounds.intersect(m_state.transform.mapRect(rect));
    platformContext()->rq().freeSpace(20)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SETCLIP_IIII
    << (jint)rect.x() << (jint)rect.y() << (jint)rect.width() << (jint)rect.height();
}

void GraphicsContext::clipToImageBuffer(ImageBuffer& buffer, const FloatRect& destRect)
{
    notImplemented();
}

IntRect GraphicsContext::clipBounds() const
{
    // Transformation has inverse effect on clip bounds.
    return enclosingIntRect(m_state.transform.inverse()->mapRect(m_state.clipBounds));
}

void GraphicsContext::drawFocusRing(const Path&, float width, float offset, const Color&)
{
    //utaTODO: IMPLEMENT!!!
}

void GraphicsContext::drawFocusRing(const Vector<FloatRect>& rects, float width, float offset, const Color& color)
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

    platformContext()->rq().freeSpace(24 * toDraw.size());
    for (size_t i = 0; i < toDraw.size(); i++) {
        IntRect focusRect = toDraw[i];
        platformContext()->rq() << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWFOCUSRING
        << (jint)focusRect.x() << (jint)focusRect.y()
        << (jint)focusRect.width() << (jint)focusRect.height()
        << (jint)color.rgb();
    }
}

void GraphicsContext::updateDocumentMarkerResources()
{
  //    NotImplemented(); // todo tav implement
}

void GraphicsContext::drawLinesForText(const FloatPoint&, const DashArray& widths, bool printing, bool doubleLines) {
    notImplemented();
}

void GraphicsContext::drawLineForText(const FloatPoint& origin, float width, bool printing, bool doubleLines)
{
    if (paintingDisabled() || width <= 0)
        return;

    // This is a workaround for http://bugs.webkit.org/show_bug.cgi?id=15659
    StrokeStyle savedStrokeStyle = strokeStyle();
    setStrokeStyle(SolidStroke);

    FloatPoint endPoint = origin + FloatPoint(width, 0);
    drawLine(
        IntPoint(origin.x(), origin.y()),
        IntPoint(endPoint.x(), endPoint.y()));

    setStrokeStyle(savedStrokeStyle);
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

void GraphicsContext::drawLineForDocumentMarker(const FloatPoint& origin, float width, DocumentMarkerLineStyle style)
{
    savePlatformState(); //fake stroke
    switch (style) { // TODO-java: DocumentMarkerAutocorrectionReplacementLineStyle not handled in switch
    case DocumentMarkerSpellingLineStyle:
        {
            static Color red(255, 0, 0);
            setStrokeColor(red);
        }
        break;
    case DocumentMarkerGrammarLineStyle:
        {
            static Color green(0, 255, 0);
            setStrokeColor(green);
        }
        break;
    default:
        {
        }
    }
    drawErrorUnderline(*this, origin.x(), origin.y(), width, cMisspellingLineThickness);
    restorePlatformState(); //fake stroke
}

FloatRect GraphicsContext::roundToDevicePixels(const FloatRect& frect, RoundingMode)
{
    FloatRect result;
    result.setX(static_cast<float>(round(frect.x())));
    result.setY(static_cast<float>(round(frect.y())));
    result.setWidth(static_cast<float>(round(frect.width())));
    result.setHeight(static_cast<float>(round(frect.height())));
    return result;
}

void GraphicsContext::translate(float x, float y)
{
    if (paintingDisabled())
        return;

    m_state.transform.translate(x, y);
    platformContext()->rq().freeSpace(12)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_TRANSLATE
    << x << y;
}

void GraphicsContext::setPlatformFillColor(const Color& col)
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(8)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SETFILLCOLOR
    << (jint)col.rgb();
}



const Vector<Gradient::ColorStop, 2>& Gradient::getStops() const
{
    return m_stops;
}

void GraphicsContext::setPlatformTextDrawingMode(TextDrawingModeFlags mode)
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(16)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SET_TEXT_MODE
    << (jint)(mode & TextModeFill)
    << (jint)(mode & TextModeStroke)
    << (jint)0;
    //utatodo:
    //<< (jint)(mode & TextModeClip);
}

void GraphicsContext::setPlatformStrokeStyle(StrokeStyle style)
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(8)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SETSTROKESTYLE
    << (jint)style;
}

void GraphicsContext::setPlatformStrokeColor(const Color& col)
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(8)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SETSTROKECOLOR
    << (jint)col.rgb();
}

void GraphicsContext::setPlatformStrokeThickness(float strokeThickness)
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(8)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SETSTROKEWIDTH
    << strokeThickness;
}

void GraphicsContext::setPlatformImageInterpolationQuality(InterpolationQuality interpolationQuality)
{
    notImplemented();
}

void GraphicsContext::setPlatformShouldAntialias(bool b)
{
    notImplemented();
}

void GraphicsContext::setURLForRect(const URL& link, const IntRect& destRect)
{
    notImplemented();
}

void GraphicsContext::concatCTM(const AffineTransform& at)
{
    if (paintingDisabled())
        return;

    m_state.transform.multiply(at);
    platformContext()->rq().freeSpace(28)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_CONCATTRANSFORM_FFFFFF
    << (float)at.a() << (float)at.b() << (float)at.c() << (float)at.d() << (float)at.e() << (float)at.f();
}

//void GraphicsContext::addInnerRoundedRectClip(const IntRect& r, int thickness)
//{
//    if (paintingDisabled())
//        return;
//
//    FloatRect rect(r);
//    Path path;
//    path.addEllipse(rect);
//    rect.inflate(-thickness);
//    path.addEllipse(rect);
//    clipPath(path, RULE_EVENODD);
//}

void GraphicsContext::setPlatformShadow(const FloatSize& s, float blur, const Color& color)
{
    if (paintingDisabled())
        return;

    float width = s.width();
    float height = s.height();
    if (shadowsIgnoreTransforms()) {
        // Meaning that this graphics context is associated with a CanvasRenderingContext
        // We flip the height since JavaFX Prism and HTML5 Canvas have opposite Y axis
        height = -height;
    }

    platformContext()->rq().freeSpace(20)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SETSHADOW
    << width << height << blur << (jint)color.rgb();
}

void GraphicsContext::clearPlatformShadow()
{
    setPlatformShadow(FloatSize(0, 0), 0, Color());
}

bool GraphicsContext::supportsTransparencyLayers()
{
    return true;
}

void GraphicsContext::beginPlatformTransparencyLayer(float opacity)
{
    if (paintingDisabled())
      return;

    platformContext()->rq().freeSpace(8)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_BEGINTRANSPARENCYLAYER
    << opacity;
}

void GraphicsContext::endPlatformTransparencyLayer()
{
    if (paintingDisabled())
      return;

    platformContext()->rq().freeSpace(4)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_ENDTRANSPARENCYLAYER;
}

void GraphicsContext::clearRect(const FloatRect& rect)
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(20)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_CLEARRECT_FFFF
    << rect.x() << rect.y()
    << rect.width() << rect.height();
}

void GraphicsContext::strokeRect(const FloatRect& rect, float lineWidth)
{
    if (paintingDisabled())
        return;

    if (m_state.strokeGradient) {
        setGradient(
            *m_state.strokeGradient,
            platformContext(),
            com_sun_webkit_graphics_GraphicsDecoder_SET_STROKE_GRADIENT);
    }

    platformContext()->rq().freeSpace(24)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_STROKERECT_FFFFF
    << rect.x() << rect.y() << rect.width() << rect.height() << lineWidth;
}

void GraphicsContext::setLineDash(const DashArray& dashes, float dashOffset)
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
        << dashes.at(i);
    }
}

void GraphicsContext::setLineCap(LineCap cap)
{
    if (paintingDisabled()) {
      return;
    }

    platformContext()->rq().freeSpace(8)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SET_LINE_CAP
    << (jint)cap;
}

void GraphicsContext::setLineJoin(LineJoin join)
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(8)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SET_LINE_JOIN
    << (jint)join;
}

void GraphicsContext::setMiterLimit(float limit)
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(8)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SET_MITER_LIMIT
    << (jfloat)limit;
}

void GraphicsContext::setPlatformAlpha(float alpha)
{
    platformContext()->rq().freeSpace(8)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SETALPHA
    << alpha;
}

void GraphicsContext::setPlatformCompositeOperation(CompositeOperator op, BlendMode bm)
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(8)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SETCOMPOSITE
    << (jint)op;
    //utatodo: add BlendMode
}

void GraphicsContext::strokePath(const Path& path)
{
    if (paintingDisabled())
        return;

    if (m_state.strokeGradient) {
        setGradient(
            *m_state.strokeGradient,
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
    << jint(wrule == RULE_EVENODD
       ? com_sun_webkit_graphics_WCPath_RULE_EVENODD
       : com_sun_webkit_graphics_WCPath_RULE_NONZERO)
    << jint(isOut);
}

void GraphicsContext::canvasClip(const Path& path, WindRule fillRule)
{
    clipPath(path, fillRule);
}

void GraphicsContext::clipPath(const Path &path, WindRule wrule)
{
    setClipPath(*this, m_state, path, wrule, false);
}

void GraphicsContext::clipOut(const Path& path)
{
    setClipPath(*this, m_state, path, RULE_EVENODD, true);
}

void GraphicsContext::clipOut(const FloatRect& rect)
{
    Path path;
    path.addRoundedRect(rect, FloatSize());
    clipOut(path);
}

void GraphicsContext::drawPattern(Image& image, const FloatRect& tileRect, const AffineTransform& patternTransform, const FloatPoint& phase, const FloatSize& spacing, CompositeOperator op, const FloatRect& destRect, BlendMode blendMode)
{
    if (paintingDisabled())
        return;

    if (isRecording()) {
        m_displayListRecorder->drawPattern(image, tileRect, patternTransform, phase, spacing, op, destRect, blendMode);
        return;
    }

    auto surface = image.nativeImageForCurrentFrame();
    if (!surface) // If it's too early we won't have an image yet.
        return;

    image.drawPattern(*this, tileRect, patternTransform, phase, spacing,
                       op, destRect, blendMode);
}

void GraphicsContext::fillPath(const Path& path)
{
    if (paintingDisabled())
        return;

    if (m_state.fillPattern && m_state.fillPattern->tileImage()) {
        savePlatformState(); //fake clip isolation
        clipPath(path, m_state.fillRule);
        FloatRect rect(path.boundingRect());

        Image *img = m_state.fillPattern->tileImage();
        FloatRect destRect(
            rect.x(),
            rect.y(),
            m_state.fillPattern->repeatX() ? rect.width() : img->width(),
            m_state.fillPattern->repeatY() ? rect.height() : img->height());
        img->drawPattern(
            *this,
            FloatRect(0., 0., img->width(), img->height()),
            m_state.fillPattern->getPatternSpaceTransform(),
            FloatPoint(),
            FloatSize(),
            CompositeCopy, //any
            destRect);
        restorePlatformState();
    } else {
        if (m_state.fillGradient) {
            setGradient(
                *m_state.fillGradient,
                platformContext(),
                com_sun_webkit_graphics_GraphicsDecoder_SET_FILL_GRADIENT);
        }

        platformContext()->rq().freeSpace(12)
        << (jint)com_sun_webkit_graphics_GraphicsDecoder_FILL_PATH
        << copyPath(path.platformPath())
        << (jint)fillRule();
    }
}

void GraphicsContext::rotate(float radians)
{
    if (paintingDisabled())
        return;

    m_state.transform.rotate(radians);
    platformContext()->rq().freeSpace(2 * 4)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_ROTATE
    << radians;

}

void GraphicsContext::scale(const FloatSize& size)
{
    if (paintingDisabled())
        return;

    m_state.transform.scale(size.width(), size.height());
    platformContext()->rq().freeSpace(12)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SCALE
    << size.width() << size.height();
}

void GraphicsContext::fillRoundedRect(const FloatRoundedRect& rect, const Color& color, BlendMode) // todo tav Int to Float
{
    if (paintingDisabled())
        return;

    platformContext()->rq().freeSpace(56)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_FILL_ROUNDED_RECT
    << (jfloat)rect.rect().x() << (jfloat)rect.rect().y()
    << (jfloat)rect.rect().width() << (jfloat)rect.rect().height()
    << (jfloat)rect.radii().topLeft().width() << (jfloat)rect.radii().topLeft().height()
    << (jfloat)rect.radii().topRight().width() << (jfloat)rect.radii().topRight().height()
    << (jfloat)rect.radii().bottomLeft().width() << (jfloat)rect.radii().bottomLeft().height()
    << (jfloat)rect.radii().bottomRight().width() << (jfloat)rect.radii().bottomRight().height()
    << (jint)color.rgb();
}

void GraphicsContext::fillRectWithRoundedHole(const FloatRect& frect, const FloatRoundedRect& roundedHoleRect, const Color& color)
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

    setFillRule(RULE_EVENODD);
    setFillColor(color);

    fillPath(path);

    setFillRule(oldFillRule);
    setFillColor(oldFillColor);
}

#if ENABLE(3D_RENDERING) && USE(TEXTURE_MAPPER)
TransformationMatrix GraphicsContext::get3DTransform() const
{
    // FIXME: Can we approximate the transformation better than this?
    return getCTM().toTransformationMatrix();
}

void GraphicsContext::concat3DTransform(const TransformationMatrix& transform)
{
    concatCTM(transform.toAffineTransform());
}

void GraphicsContext::set3DTransform(const TransformationMatrix& transform)
{
    setCTM(transform.toAffineTransform());
}
#endif

//utatodo: do we need the Java-only m_state.transform?
AffineTransform GraphicsContext::getCTM(IncludeDeviceScale) const
{
    return m_state.transform;
}


void GraphicsContext::setCTM(const AffineTransform& tm)
{
    if (paintingDisabled())
        return;

    m_state.transform = tm;
    platformContext()->rq().freeSpace(28)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_SET_TRANSFORM
    << (float)tm.a() << (float)tm.b() << (float)tm.c() << (float)tm.d() << (float)tm.e() << (float)tm.f();
}

void Gradient::platformDestroy()
{
}

void Gradient::fill(GraphicsContext *gc, const FloatRect &rect)
{
    gc->setFillGradient(*this);
    gc->fillRect(rect);
}

} // namespace WebCore
