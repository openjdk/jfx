/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <type_traits>
#include <cmath>
#include <limits>
#include <utility>

namespace javamath
{
    template <class T>
    T hypot(T x, T y, T z) noexcept
    {
        static_assert(std::is_floating_point_v<T>);
        x = std::abs(x);
        y = std::abs(y);
        z = std::abs(z);

        constexpr T infValue = std::numeric_limits<T>::infinity();
        if (x == infValue || y == infValue || z == infValue) {
            return infValue;
        }

        if (y > x) {
            std::swap(x, y);
        }

        if (z > x) {
            std::swap(x, z);
        }

        constexpr T epsValue = std::numeric_limits<T>::epsilon();
        if (x * epsValue >= y && x * epsValue >= z) {
            return x;
        }

        const auto yx = y / x;
        const auto zx = z / x;

        return x * std::sqrt(1 + yx * yx + zx * zx);
    }
}
