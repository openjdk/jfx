/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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
 * <p>
 * Component values can be interpolated in different ways, depending on the semantics of the component type:
 * <table class="striped">
 *     <caption><b>Interpolation types</b></caption>
 *     <tbody>
 *         <tr><td style="vertical-align: top"><a id="default" style="white-space: nowrap">default</a></td>
 *             <td>Component types that implement {@code Interpolatable} are interpolated by calling the
 *                 {@link #interpolate(Object, double)} method.</td>
 *         </tr>
 *         <tr><td style="vertical-align: top"><a id="linear" style="white-space: nowrap">linear</a></td>
 *             <td>Two components are combined by linear interpolation such that {@code t = 0} produces
 *                 the start value, {@code t = 1} produces the end value, and {@code 0 < t < 1} produces
 *                 {@code (1 - t) * start + t * end}. This interpolation type is usually applicable for
 *                 numeric components.</td>
 *         </tr>
 *         <tr><td style="vertical-align: top"><a id="discrete" style="white-space: nowrap">discrete</a></td>
 *             <td>If two components cannot be meaningfully combined, the intermediate component value
 *                 is equal to the start value for {@code t < 0.5} and equal to the end value for
 *                 {@code t >= 0.5}.</td>
 *         </tr>
 *         <tr><td style="vertical-align: top"><a id="pairwise" style="white-space: nowrap">pairwise</a></td>
 *             <td>Two lists are combined by pairwise interpolation. Paired list elements are interpolated
 *                 with rules as described in this table (substituting "component" for "element").
 *                 If the start list has fewer elements than the target list, the missing elements are copied
 *                 from the target list. If the start list has more elements than the target list, the excess
 *                 elements are discarded.
 *             </td>
 *         </tr>
 *     </tbody>
 * </table>
 * Some component types are interpolated in specific ways not covered here.
 * Refer to their respective documentation for more information.
 *
 * @param <T> the interpolatable value type
 * @since JavaFX 2.0
 */
@FunctionalInterface
public interface Interpolatable<T> {

    /**
     * Returns an intermediate value between the value of this {@code Interpolatable} and the specified
     * {@code endValue} using the linear interpolation factor {@code t}, ranging from 0 (inclusive)
     * to 1 (inclusive).
     * <p>
     * The returned value might not be a new instance; the implementation might also return one of the
     * two existing instances if the intermediate value would be equal to one of the existing values.
     * However, this is an optimization and applications should not assume any particular identity
     * of the returned value.
     *
     * @implSpec An implementation is not required to reject interpolation factors less than 0 or larger
     *           than 1, but this specification gives no meaning to values returned outside of this range.
     *           For example, an implementation might clamp the interpolation factor to [0..1], or it might
     *           continue the linear interpolation outside of this range.
     *
     * @param endValue the target value
     * @param t the interpolation factor
     * @throws NullPointerException if {@code endValue} is {@code null}
     * @return the intermediate value
     */
    T interpolate(T endValue, double t);
}
