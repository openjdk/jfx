/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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
#include "PlatformJavaClasses.h"
#include "NotImplemented.h"
#include "GraphicsContextJava.h"
#include "RQRef.h"
#include "GraphicsContext.h"
#include "ImageBuffer.h"
#include "PathStream.h"

#include <wtf/text/WTFString.h>
#include <wtf/java/JavaRef.h>

#include "com_sun_webkit_graphics_WCPathIterator.h"

namespace WebCore {

Ref<PathJava> PathJava::create()
{
    return adoptRef(*new PathJava);
}

Ref<PathJava> PathJava::create(const PathStream& stream)
{
    auto pathJava = PathJava::create();

    for (auto& segment : stream.segments())
        pathJava->addSegment(segment);
    return pathJava;
}

Ref<PathJava> PathJava::create(const PathSegment& segment)
{
    auto pathJava = PathJava::create();
    pathJava->addSegment(segment);
    return pathJava;
}

RefPtr<RQRef> createEmptyPath()
{
    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID mid = env->GetMethodID(PG_GetGraphicsManagerClass(env),
        "createWCPath", "()Lcom/sun/webkit/graphics/WCPath;");
    ASSERT(mid);

    JLObject ref(env->CallObjectMethod(PL_GetGraphicsManager(env), mid));
    ASSERT(ref);
    WTF::CheckAndClearException(env);
    return RQRef::create(ref);
}

static GraphicsContext& scratchContext()
{
    static auto img = ImageBuffer::create(FloatSize(1.f, 1.f), RenderingPurpose::Unspecified, 1, DestinationColorSpace::SRGB(), PixelFormat::BGRA8);
    static GraphicsContext &context = img->context();
    return context;
}

RefPtr<RQRef> copyPath(RefPtr<RQRef> p)
{
    if (!p) {
        return createEmptyPath();
    }
    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetGraphicsManagerClass(env),
        "createWCPath",
        "(Lcom/sun/webkit/graphics/WCPath;)Lcom/sun/webkit/graphics/WCPath;");
    ASSERT(mid);

    JLObject ref(env->CallObjectMethod(PL_GetGraphicsManager(env), mid, (jobject)*p));
    ASSERT(ref);
    WTF::CheckAndClearException(env);

    return RQRef::create(ref);
}

Ref<PathJava> PathJava::create(RefPtr<RQRef>&& platformPath, RefPtr<PathStream>&& elementsStream)
{
    return adoptRef(*new PathJava(WTFMove(platformPath), WTFMove(elementsStream)));
}

PathJava::PathJava()
    : m_platformPath(createEmptyPath())
    , m_elementsStream(PathStream::create())
{
}

PathJava::PathJava(RefPtr<RQRef>&& platformPath, RefPtr<PathStream>&& elementsStream)
    : m_platformPath(WTFMove(platformPath))
    , m_elementsStream(WTFMove(elementsStream))
{
    ASSERT(m_platformPath);
}

Ref<PathImpl> PathJava::copy() const
{
    RefPtr<RQRef> platformPathCopy(copyPath(platformPath()));

    auto elementsStream = m_elementsStream ? RefPtr<PathImpl> { m_elementsStream->copy() } : nullptr;

    return PathJava::create(WTFMove(platformPathCopy), downcast<PathStream>(WTFMove(elementsStream)));
}

PlatformPathPtr PathJava::platformPath() const
{
    return m_platformPath.get();
}


void PathJava::add(PathMoveTo moveto)
{
    ASSERT(m_platformPath);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env), "moveTo",
        "(DD)V");
    ASSERT(mid);

    env->CallVoidMethod(*m_platformPath, mid, (jdouble)moveto.point.x(), (jdouble)moveto.point.y());
    WTF::CheckAndClearException(env);
}

void PathJava::add(PathLineTo lineTo)
{
    ASSERT(m_platformPath);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env), "addLineTo",
        "(DD)V");
    ASSERT(mid);

    env->CallVoidMethod(*m_platformPath, mid, (jdouble)lineTo.point.x(), (jdouble)lineTo.point.y());
    WTF::CheckAndClearException(env);
}

void PathJava::add(PathQuadCurveTo quadTo)
{
    ASSERT(m_platformPath);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env), "addQuadCurveTo",
                                            "(DDDD)V");
    ASSERT(mid);

    env->CallVoidMethod(*m_platformPath, mid, (jdouble)quadTo.controlPoint.x(), (jdouble)quadTo.controlPoint.y(), (jdouble)quadTo.endPoint.x(), (jdouble)quadTo.endPoint.y());
    WTF::CheckAndClearException(env);
}

void PathJava::add(PathBezierCurveTo bezierTo)
{
    ASSERT(m_platformPath);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env),
        "addBezierCurveTo", "(DDDDDD)V");
    ASSERT(mid);

    env->CallVoidMethod(*m_platformPath, mid,
                        (jdouble)bezierTo.controlPoint1.x(), (jdouble)bezierTo.controlPoint1.y(),
                        (jdouble)bezierTo.controlPoint2.x(), (jdouble)bezierTo.controlPoint2.y(),
                        (jdouble)bezierTo.endPoint.x(), (jdouble)bezierTo.endPoint.y());
    WTF::CheckAndClearException(env);
}

static inline float areaOfTriangleFormedByPoints(const FloatPoint& p1, const FloatPoint& p2, const FloatPoint& p3)
{
    return p1.x() * (p2.y() - p3.y()) + p2.x() * (p3.y() - p1.y()) + p3.x() * (p1.y() - p2.y());
}

void PathJava::add(PathArcTo arcTo)
{
    ASSERT(m_platformPath);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env), "addArcTo",
        "(DDDDD)V");
    ASSERT(mid);

    env->CallVoidMethod(*m_platformPath, mid,
                        (jdouble)arcTo.controlPoint1.x(), (jdouble)arcTo.controlPoint1.y(),
                        (jdouble)arcTo.controlPoint2.x(), (jdouble)arcTo.controlPoint2.y(), (jdouble)arcTo.radius);
    WTF::CheckAndClearException(env);
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

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env), "addArc",
        "(DDDDDZ)V");
    ASSERT(mid);

    env->CallVoidMethod(*m_platformPath, mid, (jdouble)arc.center.x(), (jdouble)arc.center.y(),
        (jdouble)arc.radius, (jdouble)arc.startAngle, (jdouble)arc.endAngle,
        bool_to_jbool(clockwise));
    WTF::CheckAndClearException(env);
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

    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env), "addEllipse",
        "(DDDD)V");
    ASSERT(mid);

    env->CallVoidMethod(*m_platformPath, mid,
                        (jdouble)ellipseInRect.rect.x(), (jdouble)ellipseInRect.rect.y(),
                        (jdouble)ellipseInRect.rect.width(), (jdouble)ellipseInRect.rect.height());
    WTF::CheckAndClearException(env);
}

void PathJava::add(PathRect rect)
{
    ASSERT(m_platformPath);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env), "addRect",
        "(DDDD)V");
    ASSERT(mid);

    env->CallVoidMethod(*m_platformPath, mid, (jdouble)rect.rect.x(), (jdouble)rect.rect.y(),
                              (jdouble)rect.rect.width(), (jdouble)rect.rect.height());
    WTF::CheckAndClearException(env);
}

void PathJava::add(PathRoundedRect roundedRect)
{
    addBeziersForRoundedRect(roundedRect.roundedRect);
}

void PathJava::add(PathCloseSubpath)
{
    ASSERT(m_platformPath);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env),
        "closeSubpath", "()V");
    ASSERT(mid);

    env->CallVoidMethod(*m_platformPath, mid);
    WTF::CheckAndClearException(env);
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

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env),
                                            "isEmpty", "()Z");
    ASSERT(mid);

    jboolean res = env->CallBooleanMethod(*m_platformPath, mid);
    WTF::CheckAndClearException(env);

    return jbool_to_bool(res);
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

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env),
        "transform", "(DDDDDD)V");
    ASSERT(mid);

    env->CallVoidMethod(*m_platformPath, mid,
                        (jdouble)transform.a(), (jdouble)transform.b(),
                        (jdouble)transform.c(), (jdouble)transform.d(),
                        (jdouble)transform.e(), (jdouble)transform.f());
    WTF::CheckAndClearException(env);
    return true;
}

bool PathJava::contains(const FloatPoint &point, WindRule rule) const
{
    if (isEmpty() || !std::isfinite(point.x()) || !std::isfinite(point.y()))
        return false;

    ASSERT(m_platformPath);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env), "contains",
        "(IDD)Z");
    ASSERT(mid);

    jboolean res = env->CallBooleanMethod(*m_platformPath, mid, (jint)rule,
        (jdouble)point.x(), (jdouble)point.y());
    WTF::CheckAndClearException(env);

    return jbool_to_bool(res);
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

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env), "strokeContains",
        "(DDDDIID[D)Z");

    ASSERT(mid);

    size_t size = strokeStyle == StrokeStyle::SolidStroke ? 0 : dashes.size();
    JLocalRef<jdoubleArray> dashArray(env->NewDoubleArray(size));
    env->SetDoubleArrayRegion(dashArray, 0, size, dashes.data());

    jboolean res = env->CallBooleanMethod(*m_platformPath, mid, (jdouble)p.x(),
        (jdouble)p.y(), (jdouble) thickness, (jdouble) miterLimit,
        (jint) cap, (jint) join, (jdouble) dashOffset, (jdoubleArray) dashArray);

    WTF::CheckAndClearException(env);

    return jbool_to_bool(res);
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

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env), "getBounds",
            "()Lcom/sun/webkit/graphics/WCRectangle;");
    ASSERT(mid);

    JLObject rect(env->CallObjectMethod(*m_platformPath, mid));
    WTF::CheckAndClearException(env);
    if (rect) {
        static jfieldID rectxFID = env->GetFieldID(PG_GetRectangleClass(env), "x", "F");
        ASSERT(rectxFID);
        static jfieldID rectyFID = env->GetFieldID(PG_GetRectangleClass(env), "y", "F");
        ASSERT(rectyFID);
        static jfieldID rectwFID = env->GetFieldID(PG_GetRectangleClass(env), "w", "F");
        ASSERT(rectwFID);
        static jfieldID recthFID = env->GetFieldID(PG_GetRectangleClass(env), "h", "F");
        ASSERT(recthFID);

        FloatRect bounds(
            float(env->GetFloatField(rect, rectxFID)),
            float(env->GetFloatField(rect, rectyFID)),
            float(env->GetFloatField(rect, rectwFID)),
            float(env->GetFloatField(rect, recthFID)));
        WTF::CheckAndClearException(env);

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
