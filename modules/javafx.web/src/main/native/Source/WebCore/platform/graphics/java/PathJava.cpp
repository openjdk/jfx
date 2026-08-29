/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "PathJava.h"
#include "FloatRect.h"
#include "PlatformContextJava.h"
#include "NotImplemented.h"
#include "GraphicsContextJava.h"
#include "RQRef.h"
#include "GraphicsContext.h"
#include "ImageBuffer.h"
#include "PathStream.h"
#include "WKJPlatformJava.h"

#include <wtf/text/WTFString.h>

namespace WebCore {

Ref<PathJava> PathJava::create()
{
    return adoptRef(*new PathJava);
}

Ref<PathJava> PathJava::create(std::span<const PathSegment> segments)
{
    auto pathJava = PathJava::create();

    for (auto& segment : segments)
        pathJava->addSegment(segment);
    return pathJava;
}

namespace {

/*
 * WCGraphicsManager.createWCPath(). The id the slot returns is owned by this frame, so it is
 * adopted into a WKJHandle and RQRef::create adds its own reference - which is exactly what
 * the JNI code did: adopt the local ref CallObjectMethod returned, then RQRef::create it.
 */
RefPtr<RQRef> createPath()
{
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->create_path)
        return nullptr;

    WKJHandle ref { cb->create_path() };
    ASSERT(ref);
    wkjCheckAndClearException();
    return RQRef::create(ref.get());
}

}

PlatformPathPtr PathJava::emptyPlatformPath()
{
    return createPath();
}

RefPtr<RQRef> createEmptyPath()
{
    return createPath();
}
static GraphicsContext& scratchContext()
{
    ImageBufferFormat format {
        PixelFormat::BGRA8,
        UseLosslessCompression::No
    };
    static auto img = ImageBuffer::create(FloatSize(1.f, 1.f), RenderingMode::Unaccelerated, RenderingPurpose::Unspecified, 1, DestinationColorSpace::SRGB(), format);
    static GraphicsContext &context = img->context();
    return context;
}

RefPtr<RQRef> copyPath(RefPtr<RQRef> p)
{
    if (!p) {
        return createEmptyPath();
    }

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->copy_path)
        return nullptr;

    WKJHandle ref { cb->copy_path(wkj_ref(*p)) };
    ASSERT(ref);
    wkjCheckAndClearException();

    return RQRef::create(ref.get());
}

Ref<PathJava> PathJava::create(RefPtr<RQRef>&& platformPath, RefPtr<PathStream>&& elementsStream)
{
    return adoptRef(*new PathJava(WTF::move(platformPath), WTF::move(elementsStream)));
}

PathJava::PathJava()
    : m_platformPath(createEmptyPath())
    , m_elementsStream(PathStream::create())
{
}

PathJava::PathJava(RefPtr<RQRef>&& platformPath, RefPtr<PathStream>&& elementsStream)
    : m_platformPath(WTF::move(platformPath))
    , m_elementsStream(WTF::move(elementsStream))
{
    ASSERT(m_platformPath);
}

Ref<PathImpl> PathJava::copy() const
{
    RefPtr<RQRef> platformPathCopy(copyPath(platformPath()));

    auto elementsStream = m_elementsStream ? RefPtr<PathImpl> { m_elementsStream->copy() } : nullptr;

    return PathJava::create(WTF::move(platformPathCopy), downcast<PathStream>(WTF::move(elementsStream)));
}

PlatformPathPtr PathJava::platformPath() const
{
    return m_platformPath.get();
}

bool PathJava::definitelyEqual(const PathImpl& otherImpl) const
{
    RefPtr otherAsPathJava = dynamicDowncast<PathJava>(otherImpl);
    if (!otherAsPathJava) {
        return false;
    }
    if (otherAsPathJava.get() == this)
        return true;
    return m_platformPath == otherAsPathJava->m_platformPath;
}
void PathJava::add(PathContinuousRoundedRect continuousRoundedRect)
{
    add(PathRoundedRect { FloatRoundedRect { continuousRoundedRect.rect, CornerRadii { continuousRoundedRect.cornerWidth, continuousRoundedRect.cornerHeight } }, PathRoundedRect::Strategy::PreferNative });
}


void PathJava::add(PathMoveTo moveto)
{
    ASSERT(m_platformPath);

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->path_move_to)
        return;

    cb->path_move_to(wkj_ref(*m_platformPath), moveto.point.x(), moveto.point.y());
    wkjCheckAndClearException();
}

void PathJava::add(PathLineTo lineTo)
{
    ASSERT(m_platformPath);

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->path_add_line_to)
        return;

    cb->path_add_line_to(wkj_ref(*m_platformPath), lineTo.point.x(), lineTo.point.y());
    wkjCheckAndClearException();
}

void PathJava::add(PathQuadCurveTo quadTo)
{
    ASSERT(m_platformPath);

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->path_add_quad_curve_to)
        return;

    cb->path_add_quad_curve_to(wkj_ref(*m_platformPath),
                               quadTo.controlPoint.x(), quadTo.controlPoint.y(),
                               quadTo.endPoint.x(), quadTo.endPoint.y());
    wkjCheckAndClearException();
}

void PathJava::add(PathBezierCurveTo bezierTo)
{
    ASSERT(m_platformPath);

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->path_add_bezier_curve_to)
        return;

    cb->path_add_bezier_curve_to(wkj_ref(*m_platformPath),
                                 bezierTo.controlPoint1.x(), bezierTo.controlPoint1.y(),
                                 bezierTo.controlPoint2.x(), bezierTo.controlPoint2.y(),
                                 bezierTo.endPoint.x(), bezierTo.endPoint.y());
    wkjCheckAndClearException();
}

static inline float areaOfTriangleFormedByPoints(const FloatPoint& p1, const FloatPoint& p2, const FloatPoint& p3)
{
    return p1.x() * (p2.y() - p3.y()) + p2.x() * (p3.y() - p1.y()) + p3.x() * (p1.y() - p2.y());
}

void PathJava::add(PathArcTo arcTo)
{
    ASSERT(m_platformPath);

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->path_add_arc_to)
        return;

    cb->path_add_arc_to(wkj_ref(*m_platformPath),
                        arcTo.controlPoint1.x(), arcTo.controlPoint1.y(),
                        arcTo.controlPoint2.x(), arcTo.controlPoint2.y(), arcTo.radius);
    wkjCheckAndClearException();
}

void PathJava::add(PathArc arc)
{
    ASSERT(m_platformPath);
    bool clockwise = false;
    const RotationDirection direction = arc.direction;
    if (direction == RotationDirection::Counterclockwise) {
        clockwise = true;
    } else if (direction == RotationDirection::Clockwise) {
        clockwise = false;
    }

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->path_add_arc)
        return;

    cb->path_add_arc(wkj_ref(*m_platformPath), arc.center.x(), arc.center.y(),
                     arc.radius, arc.startAngle, arc.endAngle, clockwise ? 1 : 0);
    wkjCheckAndClearException();
}
void PathJava::add(PathClosedArc closedArc)
{
    notImplemented();
}

void PathJava::add(PathEllipse ellipse)
{
    notImplemented();
}

void PathJava::add(PathEllipseInRect ellipseInRect)
{
    ASSERT(m_platformPath);

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->path_add_ellipse)
        return;

    cb->path_add_ellipse(wkj_ref(*m_platformPath),
                         ellipseInRect.rect.x(), ellipseInRect.rect.y(),
                         ellipseInRect.rect.width(), ellipseInRect.rect.height());
    wkjCheckAndClearException();
}

void PathJava::add(PathRect rect)
{
    ASSERT(m_platformPath);

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->path_add_rect)
        return;

    cb->path_add_rect(wkj_ref(*m_platformPath), rect.rect.x(), rect.rect.y(),
                      rect.rect.width(), rect.rect.height());
    wkjCheckAndClearException();
}

void PathJava::add(PathRoundedRect roundedRect)
{
    for (auto& segment : PathImpl::beziersForRoundedRect(roundedRect.roundedRect))
        addSegment(segment);
}

void PathJava::add(PathCloseSubpath)
{
    ASSERT(m_platformPath);

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->path_close_subpath)
        return;

    cb->path_close_subpath(wkj_ref(*m_platformPath));
    wkjCheckAndClearException();
}

void PathJava::addPath(const PathJava& path, const AffineTransform& transform)
{
    notImplemented();
}

void PathJava::applySegments(const PathSegmentApplier& applier) const
{
    applyElements([&](const PathElement& pathElement) {
        switch (pathElement.type) {
        case PathElement::Type::MoveToPoint:
            applier({ PathMoveTo { pathElement.points[0] } });
            break;

        case PathElement::Type::AddLineToPoint:
            applier({ PathLineTo { pathElement.points[0] } });
            break;

        case PathElement::Type::AddQuadCurveToPoint:
            applier({ PathQuadCurveTo { pathElement.points[0], pathElement.points[1] } });
            break;

        case PathElement::Type::AddCurveToPoint:
            applier({ PathBezierCurveTo { pathElement.points[0], pathElement.points[1], pathElement.points[2] } });
            break;

        case PathElement::Type::CloseSubpath:
            applier({ PathCloseSubpath { } });
            break;
        }
    });
}

bool PathJava::applyElements(const PathElementApplier& applier) const
{
    // need to implement this method after looking into cairo implementation
    return true;
}


bool PathJava::isEmpty() const
{
    ASSERT(m_platformPath);

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->path_is_empty)
        return false;

    int32_t res = cb->path_is_empty(wkj_ref(*m_platformPath));
    wkjCheckAndClearException();

    return res != 0;
}

FloatPoint PathJava::currentPoint() const
{
    //utatodo: return current point of subpath.
    float quietNaN = std::numeric_limits<float>::quiet_NaN();
    return FloatPoint(quietNaN, quietNaN);
}

bool PathJava::transform(const AffineTransform& transform)
{
    ASSERT(m_platformPath);

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->path_transform)
        return true;

    cb->path_transform(wkj_ref(*m_platformPath),
                       transform.a(), transform.b(), transform.c(),
                       transform.d(), transform.e(), transform.f());
    wkjCheckAndClearException();
    return true;
}

bool PathJava::contains(const FloatPoint &point, WindRule rule) const
{
    if (isEmpty() || !std::isfinite(point.x()) || !std::isfinite(point.y()))
        return false;

    ASSERT(m_platformPath);

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->path_contains)
        return false;

    int32_t res = cb->path_contains(wkj_ref(*m_platformPath), static_cast<int32_t>(rule),
                                    point.x(), point.y());
    wkjCheckAndClearException();

    return res != 0;
}

bool PathJava::strokeContains(const FloatPoint& p, const Function<void(GraphicsContext&)>& strokeStyleApplier) const
{
    ASSERT(m_platformPath);
    ASSERT(strokeStyleApplier);

    GraphicsContext& gc = scratchContext();
    gc.save();

    // Stroke style is set to SolidStroke if the path is not dashed, else it
    // is unchanged. Setting it to NoStroke enables us to detect the switch.
    gc.setStrokeStyle(StrokeStyle::NoStroke);
    strokeStyleApplier(gc);

    float thickness = gc.strokeThickness();
    StrokeStyle strokeStyle = gc.strokeStyle();
    float miterLimit = gc.platformContext()->miterLimit();
    LineCap cap = gc.platformContext()->lineCap();
    LineJoin join = gc.platformContext()->lineJoin();
    float dashOffset = gc.platformContext()->dashOffset();
    DashArray dashes = gc.platformContext()->dashArray();

    gc.restore();

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->path_stroke_contains)
        return false;

    // A solid stroke passed a zero-length double[]; it now passes a count of 0.
    size_t size = strokeStyle == StrokeStyle::SolidStroke ? 0 : dashes.size();

    int32_t res = cb->path_stroke_contains(wkj_ref(*m_platformPath), p.x(), p.y(),
                                           thickness, miterLimit,
                                           static_cast<int32_t>(cap), static_cast<int32_t>(join),
                                           dashOffset, dashes.span().data(),
                                           static_cast<int32_t>(size));

    wkjCheckAndClearException();

    return res != 0;
}

FloatRect PathJava::fastBoundingRect() const
{
    return boundingRect();
}

FloatRect PathJava::boundingRect() const
{
    return strokeBoundingRect(nullptr);
}

FloatRect PathJava::strokeBoundingRect(const Function<void(GraphicsContext&)>& strokeStyleApplier) const
{
    ASSERT(m_platformPath);

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->path_get_bounds)
        return FloatRect();

    // The four floats replace a WCRectangle whose x/y/w/h fields the JNI code read back with
    // GetFieldID; a 0 return is the null-rectangle case that gave an empty FloatRect.
    float xywh[4] = { 0.f, 0.f, 0.f, 0.f };
    int32_t haveBounds = cb->path_get_bounds(wkj_ref(*m_platformPath), xywh);
    wkjCheckAndClearException();

    if (haveBounds) {
        FloatRect bounds(xywh[0], xywh[1], xywh[2], xywh[3]);

        float thickness;
        if (strokeStyleApplier) {
            GraphicsContext& gc = scratchContext();
            gc.save();
            strokeStyleApplier(gc);
            thickness = gc.strokeThickness();
            gc.restore();
            bounds.inflate(thickness / 2);
        }
        return bounds;
    } else {
        return FloatRect();
    }
}

} // namespace WebCore
