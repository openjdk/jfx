/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "D3D12Common.hpp"

#include "D3D12Matrix.hpp"


namespace D3D12 {
namespace Internal {

/**
 * A 2D Bounding Box used to track simple Render Target dirty zones
 */
struct BBox
{
    // These are absolute coordinates in texture's space, or in other words:
    //   width = max.x - min.x; height = max.y - min.y;
    //
    // Mapping to D3D12_RECT:
    //    min.x == left, min.y == top
    //    max.x == right, max.y == bottom
    Coords_XY_FLOAT min;
    Coords_XY_FLOAT max;

    BBox() // initializes an invalid BBox
        : BBox(std::numeric_limits<float>::max(), std::numeric_limits<float>::max(), 0.0f, 0.0f)
    {
    }

    BBox(float minx, float miny, float maxx, float maxy)
        : min{minx, miny}
        , max{maxx, maxy}
    {
    }

    inline void operator=(const BBox& other)
    {
        min.x = other.min.x;
        min.y = other.min.y;
        max.x = other.max.x;
        max.y = other.max.y;
    }

    inline void Merge(const BBox& other)
    {
        if (!other.Valid()) return;

        // dirty bbox applies to RTTs only, so its dimensions cannot be less than 0
        // otherwise further checks we do (ex. Inside() ) might not work
        BBox maxed(
            std::max(other.min.x, 0.0f),
            std::max(other.min.y, 0.0f),
            std::max(other.max.x, 0.0f),
            std::max(other.max.y, 0.0f)
        );

        min.x = std::min(min.x, maxed.min.x);
        min.y = std::min(min.y, maxed.min.y);
        max.x = std::max(max.x, maxed.max.x);
        max.y = std::max(max.y, maxed.max.y);
    }

    // Merge to a single vertex; this WILL strip coordinates to just X/Y pair
    inline void Merge(const Coords_XYZW_FLOAT& coords)
    {
        Merge(BBox(coords.x, coords.y, coords.x, coords.y));
    }

    inline void Merge(float minx, float miny, float maxx, float maxy)
    {
        Merge(BBox(minx, miny, maxx, maxy));
    }

    inline bool Inside(const float minx, const float miny, const float maxx, const float maxy) const
    {
        return std::round(min.x) >= minx && std::round(min.y) >= miny &&
               std::round(max.x) <= maxx && std::round(max.y) <= maxy;
    }

    inline bool Inside(const D3D12_RECT& rect) const
    {
        return Inside(
            static_cast<const float>(rect.left),
            static_cast<const float>(rect.top),
            static_cast<const float>(rect.right),
            static_cast<const float>(rect.bottom)
        );
    }

    inline bool Inside(const BBox& other) const
    {
        return Inside(other.min.x, other.min.y, other.max.x, other.max.y);
    }

    // if @p rect (ex. scissor rect) is smaller than bbox, shrinks the bbox to fit the scissor
    inline void Min(const D3D12_RECT& rect)
    {
        // dirty bbox applies to RTTs only, so its dimensions cannot be less than 0
        // otherwise further checks we do (ex. Inside() ) might not work
        BBox maxed(
            std::max(static_cast<float>(rect.left), 0.0f),
            std::max(static_cast<float>(rect.top), 0.0f),
            std::max(static_cast<float>(rect.right), 0.0f),
            std::max(static_cast<float>(rect.bottom), 0.0f)
        );

        if (maxed.min.x > min.x) min.x = maxed.min.x;
        if (maxed.min.y > min.y) min.y = maxed.min.y;
        if (maxed.max.x < max.x) max.x = maxed.max.x;
        if (maxed.max.y < max.y) max.y = maxed.max.y;
    }

    inline bool Valid() const
    {
        // bbox is valid only when it's max coords are higher than min coords
        // we don't consider equal coords as valid either, it would mean 0 width
        // or 0 height which is not an actual area
        return (min.x <= max.x) && (min.y <= max.y);
    }

    // Transforms BBox in-place according to provided matrix.
    // This will essentially multiply the Box vertices by the matrix and replace
    // them with a new set of BBox vertices
    inline void Transform(const Matrix<float>& matrix)
    {
        // form all bbox vertices
        Coords_XYZW_FLOAT bboxVerts[4] =
        {
            { min.x, min.y, 0.0f, 1.0f},
            { max.x, min.y, 0.0f, 1.0f},
            { min.x, max.y, 0.0f, 1.0f},
            { max.x, max.y, 0.0f, 1.0f},
        };

        // transform all four verts
        for (uint32_t i = 0; i < 4; ++i)
        {
            bboxVerts[i] = matrix.Mul(bboxVerts[i]);
        }

        // form a new BBox from transformed vertices
        min.x = min.y = std::numeric_limits<float>::max();
        max.x = max.y = 0.0f;
        for (uint32_t i = 0; i < 4; ++i)
        {
            Merge(bboxVerts[i].x, bboxVerts[i].y, bboxVerts[i].x, bboxVerts[i].y);
        }
    }

    inline std::string ToString() const
    {
        std::stringstream ss;
        ss.precision(2);
        ss << std::fixed << '{' << min.x << ", " << min.y << "; " << max.x << ", " << max.y << '}';
        return ss.str();
    }
};

} // namespace Internal
} // namespace D3D12
