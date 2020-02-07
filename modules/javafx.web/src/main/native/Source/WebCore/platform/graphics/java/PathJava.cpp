/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include "Path.h"
#include "FloatRect.h"
#include "StrokeStyleApplier.h"
#include "PlatformContextJava.h"
#include "PlatformJavaClasses.h"
#include "NotImplemented.h"
#include "GraphicsContextJava.h"
#include "RQRef.h"
#include "GraphicsContext.h"
#include "ImageBuffer.h"

#include <wtf/text/WTFString.h>
#include <wtf/java/JavaRef.h>

#include "com_sun_webkit_graphics_WCPathIterator.h"


namespace WebCore {

static GraphicsContext& scratchContext()
{
    static std::unique_ptr<ImageBuffer> img = ImageBuffer::create(FloatSize(1.f, 1.f), Unaccelerated);
    static GraphicsContext &context = img->context();
    return context;
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



Path::Path()
    : m_path(createEmptyPath())
{}

Path::Path(const Path& p)
    : m_path(copyPath(p.platformPath()))
{}

Path::~Path()
{}

Path::Path(Path&& other)
{
    m_path = other.m_path;
    other.m_path = nullptr;
}

Path& Path::operator=(const Path &p)
{
    if (this != &p) {
        m_path = copyPath(p.platformPath());
    }
    return *this;
}

Path& Path::operator=(Path&& other)
{
    if (this == &other)
        return *this;

    m_path = other.m_path;
    other.m_path = nullptr;
    return *this;
}

bool Path::contains(const FloatPoint& p, WindRule rule) const
{
    ASSERT(m_path);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env), "contains",
        "(IDD)Z");
    ASSERT(mid);

    jboolean res = env->CallBooleanMethod(*m_path, mid, (jint)rule,
        (jdouble)p.x(), (jdouble)p.y());
    WTF::CheckAndClearException(env);

    return jbool_to_bool(res);
}

FloatRect Path::boundingRect() const
{
    return strokeBoundingRect(0);
}

FloatRect Path::strokeBoundingRect(StrokeStyleApplier *applier) const
{
    ASSERT(m_path);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env), "getBounds",
            "()Lcom/sun/webkit/graphics/WCRectangle;");
    ASSERT(mid);

    JLObject rect(env->CallObjectMethod(*m_path, mid));
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
        if (applier) {
            GraphicsContext& gc = scratchContext();
            gc.save();
            applier->strokeStyle(&gc);
            thickness = gc.strokeThickness();
            gc.restore();
            bounds.inflate(thickness / 2);
        }
        return bounds;
    } else {
        return FloatRect();
    }
}

void Path::clear()
{
    ASSERT(m_path);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env),
        "clear", "()V");
    ASSERT(mid);

    env->CallVoidMethod(*m_path, mid);
    WTF::CheckAndClearException(env);
}

bool Path::isEmpty() const
{
    ASSERT(m_path);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env),
                                            "isEmpty", "()Z");
    ASSERT(mid);

    jboolean res = env->CallBooleanMethod(*m_path, mid);
    WTF::CheckAndClearException(env);

    return jbool_to_bool(res);
}

bool Path::hasCurrentPoint() const
{
    ASSERT(m_path);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env),
                                            "hasCurrentPoint", "()Z");
    ASSERT(mid);

    jboolean res = env->CallBooleanMethod(*m_path, mid);
    WTF::CheckAndClearException(env);

    return jbool_to_bool(res);
}

FloatPoint Path::currentPoint() const
{
    //utatodo: return current point of subpath.
    float quietNaN = std::numeric_limits<float>::quiet_NaN();
    return FloatPoint(quietNaN, quietNaN);
}

void Path::moveTo(const FloatPoint &p)
{
    ASSERT(m_path);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env), "moveTo",
        "(DD)V");
    ASSERT(mid);

    env->CallVoidMethod(*m_path, mid, (jdouble)p.x(), (jdouble)p.y());
    WTF::CheckAndClearException(env);
}

void Path::addLineTo(const FloatPoint &p)
{
    ASSERT(m_path);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env), "addLineTo",
        "(DD)V");
    ASSERT(mid);

    env->CallVoidMethod(*m_path, mid, (jdouble)p.x(), (jdouble)p.y());
    WTF::CheckAndClearException(env);
}

void Path::addQuadCurveTo(const FloatPoint &cp, const FloatPoint &p)
{
    ASSERT(m_path);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env), "addQuadCurveTo",
                                            "(DDDD)V");
    ASSERT(mid);

    env->CallVoidMethod(*m_path, mid, (jdouble)cp.x(), (jdouble)cp.y(), (jdouble)p.x(), (jdouble)p.y());
    WTF::CheckAndClearException(env);
}

void Path::addBezierCurveTo(const FloatPoint & controlPoint1,
                            const FloatPoint & controlPoint2,
                            const FloatPoint & controlPoint3)
{
    ASSERT(m_path);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env),
        "addBezierCurveTo", "(DDDDDD)V");
    ASSERT(mid);

    env->CallVoidMethod(*m_path, mid,
                        (jdouble)controlPoint1.x(), (jdouble)controlPoint1.y(),
                        (jdouble)controlPoint2.x(), (jdouble)controlPoint2.y(),
                        (jdouble)controlPoint3.x(), (jdouble)controlPoint3.y());
    WTF::CheckAndClearException(env);
}

void Path::addArcTo(const FloatPoint & p1, const FloatPoint & p2, float radius)
{
    ASSERT(m_path);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env), "addArcTo",
        "(DDDDD)V");
    ASSERT(mid);

    env->CallVoidMethod(*m_path, mid,
                        (jdouble)p1.x(), (jdouble)p1.y(),
                        (jdouble)p2.x(), (jdouble)p2.y(), (jdouble)radius);
    WTF::CheckAndClearException(env);
}

void Path::closeSubpath()
{
    ASSERT(m_path);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env),
        "closeSubpath", "()V");
    ASSERT(mid);

    env->CallVoidMethod(*m_path, mid);
    WTF::CheckAndClearException(env);
}

void Path::addArc(const FloatPoint & p, float radius, float startAngle,
                  float endAngle, bool clockwise)
{
    ASSERT(m_path);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env), "addArc",
        "(DDDDDZ)V");
    ASSERT(mid);

    env->CallVoidMethod(*m_path, mid, (jdouble)p.x(), (jdouble)p.y(),
        (jdouble)radius, (jdouble)startAngle, (jdouble)endAngle,
        bool_to_jbool(clockwise));
    WTF::CheckAndClearException(env);
}

void Path::addRect(const FloatRect& r)
{
    ASSERT(m_path);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env), "addRect",
        "(DDDD)V");
    ASSERT(mid);

    env->CallVoidMethod(*m_path, mid, (jdouble)r.x(), (jdouble)r.y(),
                              (jdouble)r.width(), (jdouble)r.height());
    WTF::CheckAndClearException(env);
}

void Path::addEllipse(FloatPoint, float, float, float, float, float, bool)
{
    notImplemented();
}

void Path::addPath(const Path&, const AffineTransform&)
{
    notImplemented();
}

void Path::addEllipse(const FloatRect& r)
{
    ASSERT(m_path);

    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env), "addEllipse",
        "(DDDD)V");
    ASSERT(mid);

    env->CallVoidMethod(*m_path, mid,
                        (jdouble)r.x(), (jdouble)r.y(),
                        (jdouble)r.width(), (jdouble)r.height());
    WTF::CheckAndClearException(env);
}

void Path::translate(const FloatSize &sz)
{
    ASSERT(m_path);

    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env), "translate",
        "(DD)V");
    ASSERT(mid);

    env->CallVoidMethod(*m_path, mid,
                        (jdouble)sz.width(), (jdouble)sz.height());
    WTF::CheckAndClearException(env);
}

void Path::transform(const AffineTransform &at)
{
    ASSERT(m_path);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env),
        "transform", "(DDDDDD)V");
    ASSERT(mid);

    env->CallVoidMethod(*m_path, mid,
                        (jdouble)at.a(), (jdouble)at.b(),
                        (jdouble)at.c(), (jdouble)at.d(),
                        (jdouble)at.e(), (jdouble)at.f());
    WTF::CheckAndClearException(env);
}

void Path::apply(const PathApplierFunction& function) const
{
    ASSERT(m_path);

    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetPathClass(env),
        "getPathIterator", "()Lcom/sun/webkit/graphics/WCPathIterator;");
    ASSERT(mid);

    JLObject iter(env->CallObjectMethod(*m_path, mid));
    WTF::CheckAndClearException(env);

    if (iter) {
        static jmethodID midIsDone = env->GetMethodID(PG_GetPathIteratorClass(env),
            "isDone", "()Z");
        ASSERT(midIsDone);

        static jmethodID midNext = env->GetMethodID(PG_GetPathIteratorClass(env),
            "next", "()V");
        ASSERT(midNext);

        static jmethodID midCurrentSegment = env->GetMethodID(PG_GetPathIteratorClass(env),
            "currentSegment", "([D)I");
        ASSERT(midCurrentSegment);

        PathElement pelement;
        FloatPoint points[3];
        pelement.points = points;

        JLocalRef<jdoubleArray> coords(env->NewDoubleArray(6));
        while(JNI_FALSE == env->CallBooleanMethod(iter, midIsDone)) {
            jint type = env->CallBooleanMethod(
                iter,
                midCurrentSegment,
                (jdoubleArray)coords);
            jboolean isCopy = JNI_FALSE;
            jdouble *data = env->GetDoubleArrayElements(coords, &isCopy);
            switch (type) {
            case com_sun_webkit_graphics_WCPathIterator_SEG_MOVETO:
                pelement.type = PathElementMoveToPoint;
                pelement.points[0] = FloatPoint(data[0],data[1]);
                function(pelement);
                break;
            case com_sun_webkit_graphics_WCPathIterator_SEG_LINETO:
                pelement.type = PathElementAddLineToPoint;
                pelement.points[0] = FloatPoint(data[0],data[1]);
                function(pelement);
                break;
            case com_sun_webkit_graphics_WCPathIterator_SEG_QUADTO:
                pelement.type = PathElementAddQuadCurveToPoint;
                pelement.points[0] = FloatPoint(data[0],data[1]);
                pelement.points[1] = FloatPoint(data[2],data[3]);
                function(pelement);
                break;
            case com_sun_webkit_graphics_WCPathIterator_SEG_CUBICTO:
                pelement.type = PathElementAddCurveToPoint;
                pelement.points[0] = FloatPoint(data[0],data[1]);
                pelement.points[1] = FloatPoint(data[2],data[3]);
                pelement.points[2] = FloatPoint(data[4],data[5]);
                function(pelement);
                break;
            case com_sun_webkit_graphics_WCPathIterator_SEG_CLOSE:
                pelement.type = PathElementCloseSubpath;
                function(pelement);
                break;
            }
            env->ReleaseDoubleArrayElements(coords, data, JNI_ABORT);
            env->CallVoidMethod(iter, midNext);
        }
        WTF::CheckAndClearException(env);
    }
}

bool Path::strokeContains(StrokeStyleApplier *applier, const FloatPoint& p) const
{
    ASSERT(m_path);

    GraphicsContext& gc = scratchContext();
    gc.save();

    // Stroke style is set to SolidStroke if the path is not dashed, else it
    // is unchanged. Setting it to NoStroke enables us to detect the switch.
    gc.setStrokeStyle(NoStroke);

    if (applier) {
        applier->strokeStyle(&gc);
    }

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

    size_t size = strokeStyle == SolidStroke ? 0 : dashes.size();
    JLocalRef<jdoubleArray> dashArray(env->NewDoubleArray(size));
    env->SetDoubleArrayRegion(dashArray, 0, size, dashes.data());

    jboolean res = env->CallBooleanMethod(*m_path, mid, (jdouble)p.x(),
        (jdouble)p.y(), (jdouble) thickness, (jdouble) miterLimit,
        (jint) cap, (jint) join, (jdouble) dashOffset, (jdoubleArray) dashArray);

    WTF::CheckAndClearException(env);

    return jbool_to_bool(res);
}

}
