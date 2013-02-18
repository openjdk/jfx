/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.impl.state;

public interface LinearConvolvePeer {
    /**
     * Returns the number of power of 2 scales along the X axis.
     * Positive numbers mean to scale the image larger by the indicated
     * factors of 2.0.
     * Negative numbers mean to scale the image smaller by the indicated
     * factors of 0.5.
     * Overall the image will be scaled by {@code pow(2.0, getPow2ScaleX())}.
     * <p>
     * @param kernel the {@code LinearConvolveKernel} instance for the operation.
     * @return the power of 2.0 by which to scale the source image along the
     *         X axis.
     */
    public int getPow2ScaleX(LinearConvolveKernel kernel);

    /**
     * Returns the number of power of 2 scales along the Y axis.
     * Positive numbers mean to scale the image larger by the indicated
     * factors of 2.0.
     * Negative numbers mean to scale the image smaller by the indicated
     * factors of 0.5.
     * Overall the image will be scaled by {@code pow(2.0, getPow2ScaleY())}.
     * <p>
     * @param kernel the {@code LinearConvolveKernel} instance for the operation.
     * @return the power of 2.0 by which to scale the source image along the
     *         Y axis.
     */
    public int getPow2ScaleY(LinearConvolveKernel kernel);
}
