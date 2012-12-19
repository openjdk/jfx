/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.sg;

/**
 * A special purpose interface used for the regions that are used by UI
 * controls so as to minimize the number of nodes in the scene graph.
 */
public interface PGRegion extends PGGroup {
    /**
     * Specified by the Region whenever its border changes. Note that
     * the reason the type is "Object" is because javafx-sg-common
     * doesn't have a build dependency on Region, however NGRegion does,
     * so it can simply cast the instance to a javafx.scene.layout.Border.
     *
     * @param border    The border, may be null.
     */
    public void updateBorder(Object border);

    /**
     * Specified by the Region whenever its background changes. Note that
     * the reason the type is "Object" is because javafx-sg-common
     * doesn't have a build dependency on Region, however NGRegion does,
     * so it can simply cast the instance to a javafx.scene.layout.Background.
     *
     * @param background    The background, may be null.
     */
    public void updateBackground(Object background);

    /**
     * Specified by the Region whenever its shape changes. Note that
     * the reason the type is "Object" is because javafx-sg-common
     * doesn't have a build dependency on Region, however NGRegion does,
     * so it can simply cast the instance to a javafx.scene.shape.Shape.
     *
     * @param shape    The shape, may be null.
     * @param scaleShape whether to scale the shape
     * @param positionShape whether to center the shape
     */
    public void updateShape(Object shape, boolean scaleShape, boolean positionShape);

    /**
     * The opaque insets as specified either by the user, or as determined by
     * inspecting the backgrounds. The Region is responsible for determining
     * the opaque insets. The NGRegion will further refine this by taking
     * into account the clip, effect, etc.
     *
     * @param top       The top, if NaN then there is no opaque inset at all
     * @param right     The right, must not be NaN or Infinity, etc.
     * @param bottom    The bottom, must not be NaN or Infinity, etc.
     * @param left      The left, must not be NaN or Infinity, etc.
     */
    public void setOpaqueInsets(float top, float right, float bottom, float left);

    /**
     * Called when the width / height of the Region have changed.
     *
     * @param width     The width of the region, not including insets or outsets
     * @param height    The height of the region, not including insets or outsets
     */
    public void setSize(float width, float height);
}
