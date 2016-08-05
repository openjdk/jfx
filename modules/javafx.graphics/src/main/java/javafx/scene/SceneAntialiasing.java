/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

import javafx.scene.shape.Shape;

/**
 * The JavaFX {@code SceneAntialiasing} class specifies the level of
 * anti-aliasing desired. Scene anti-aliasing is primarily used when rendering
 * 3D primitives, which are otherwise rendered aliased.
 * <p>
 * {@code SceneAntialiasing} is unrelated to
 * {@link Shape#setSmooth Shape.setSmooth}. Unlike
 * {@link Shape#setSmooth Shape.setSmooth}, {@code SceneAntialiasing} affects
 * the smoothness of the entire rendered scene whereas
 * {@link Shape#setSmooth Shape.setSmooth} is a rendering hint that applies to
 * an individual 2D Shape.
 * <p>
 * Note: In order for {@code SceneAntialiasing} to have an affect, the underlying
 * system must support:
 * {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
 * and anti-aliasing.
 * </p>
 * @since JavaFX 8.0
 */
public final class SceneAntialiasing {
    /**
     * Disables anti-aliasing
     */
    public static final SceneAntialiasing DISABLED = new SceneAntialiasing("DISABLED");
    /*
     * Enables anti-aliasing optimizing for performance over quality
     */
//    public static final SceneAntialiasing FASTEST  = new SceneAntialiasing("FASTEST");
    /**
     * Enables anti-aliasing optimizing for a balance of quality and performance
     */
    public static final SceneAntialiasing BALANCED = new SceneAntialiasing("BALANCED");
    /*
     * Enables anti-aliasing optimizing for quality over performance
     */
//    public static final SceneAntialiasing NICEST   = new SceneAntialiasing("NICEST");

    private final String val;

    private SceneAntialiasing(String value) {
        val = value;
    }

    @Override
    public String toString() {
        return val;
    }
}
