/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

#include "PathImpl.h"
#include "PathStream.h"
#include "PlatformPath.h"
#include "RQRef.h"
#include "WindRule.h"

namespace WebCore {

class GraphicsContext;
class PathStream;

class PathJava final : public PathImpl {
public:
    static UniqueRef<PathJava> create();
    static UniqueRef<PathJava> create(const PathStream&);
    static UniqueRef<PathJava> create(RefPtr<RQRef>&&, std::unique_ptr<PathStream>&& = nullptr);

    PathJava();
    PathJava(RefPtr<RQRef>&&, std::unique_ptr<PathStream>&&);

    PlatformPathPtr platformPath() const;

    bool operator==(const PathImpl&) const final;

    void addPath(const PathJava&, const AffineTransform&);

    bool applyElements(const PathElementApplier&) const final;

    bool transform(const AffineTransform&) final;

    bool contains(const FloatPoint&, WindRule) const;
    bool strokeContains(const FloatPoint&, const Function<void(GraphicsContext&)>& strokeStyleApplier) const;

    FloatRect strokeBoundingRect(const Function<void(GraphicsContext&)>& strokeStyleApplier) const;

private:
    UniqueRef<PathImpl> clone() const final;

    void moveTo(const FloatPoint&) final;

    void addLineTo(const FloatPoint&) final;
    void addQuadCurveTo(const FloatPoint& controlPoint, const FloatPoint& endPoint) final;
    void addBezierCurveTo(const FloatPoint& controlPoint1, const FloatPoint& controlPoint2, const FloatPoint& endPoint) final;
    void addArcTo(const FloatPoint& point1, const FloatPoint& point2, float radius) final;

    void addArc(const FloatPoint&, float radius, float startAngle, float endAngle, RotationDirection) final;
    void addEllipse(const FloatPoint&, float radiusX, float radiusY, float rotation, float startAngle, float endAngle, RotationDirection) final;
    void addEllipseInRect(const FloatRect&) final;
    void addRect(const FloatRect&) final;
    void addRoundedRect(const FloatRoundedRect&, PathRoundedRect::Strategy) final;

    void closeSubpath() final;

    void applySegments(const PathSegmentApplier&) const final;

    bool isEmpty() const final;

    FloatPoint currentPoint() const final;

    FloatRect fastBoundingRect() const final;
    FloatRect boundingRect() const final;

    RefPtr<RQRef> m_platformPath;
    std::unique_ptr<PathStream> m_elementsStream;
};

} // namespace WebCore

SPECIALIZE_TYPE_TRAITS_BEGIN(WebCore::PathJava)
    static bool isType(const WebCore::PathImpl& pathImpl) { return !pathImpl.isPathStream(); }
SPECIALIZE_TYPE_TRAITS_END()
