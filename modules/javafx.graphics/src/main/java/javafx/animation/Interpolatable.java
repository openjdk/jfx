/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

package javafx.animation;

/**
 * A value that can be interpolated. It defines a single {@link #interpolate(Object, double)}
 * method, which returns an intermediate value between the value of this {@code Interpolatable}
 * and the specified target value.
 *
 * @since JavaFX 2.0
 */
@FunctionalInterface
public interface Interpolatable<T> {

    /**
     * Returns an intermediate value between the value of this {@code Interpolatable} and the specified
     * {@code endValue} using the linear interpolation factor {@code t}, ranging from 0 (inclusive)
     * to 1 (inclusive).
     * <p>
     * If the linear interpolation factor {@code t} is less than or equal to 0, a value equal to the
     * value of this {@code Interpolatable} is returned; if the fraction is larger than or equal to 1,
     * a value equal to {@code endValue} is returned.
     * <p>
     * The returned value may not be a new instance; it can also be either of the two existing instances
     * if the intermediate value would be equal to one of the existing values. However, applications
     * should not assume any particular identity of the returned value.
     *
     * @param endValue the target value
     * @param t the interpolation factor
     * @return the intermediate value
     */
    T interpolate(T endValue, double t);
}
