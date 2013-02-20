/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect;

import com.sun.javafx.geom.transform.BaseTransform;

/**
 * An interface for supplying an object which can simplify effect
 * chains by rendering some intermediate images directly from the
 * implementations of the effects.
 */
public interface ImageDataRenderer {
    /**
     * Renders the indicated {@link ImageData} to the output with the
     * indicated transform using BILINEAR filtering.
     * This method should only be used by an {@link Effect} if there is only
     * a single output or if there are multiple outputs which would have
     * otherwise been combined into a single output using the Porter-Duff
     * SrcOver composite mode.
     * If this method is used to render all image results of an effect
     * operation then the effect may return null as its output result.
     * This method should not save a reference to or modify the reference
     * count on the {@code ImageData} argument.
     */
    public void renderImage(ImageData image,
                            BaseTransform transform,
                            FilterContext fctx);
}
