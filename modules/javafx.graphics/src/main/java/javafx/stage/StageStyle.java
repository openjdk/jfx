/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javafx.stage;

/**
 * This enum defines the possible styles for a {@code Stage}.
 * @since JavaFX 2.0
 */
public enum StageStyle {

    /**
     * Defines a normal {@code Stage} style with a solid white background and platform decorations.
     */
    DECORATED,

    /**
     * Defines a {@code Stage} style with a solid white background and no decorations.
     */
    UNDECORATED,

    /**
     * Defines a {@code Stage} style with a transparent background and no decorations.
     * This is a conditional feature; to check if it is supported use
     * {@link javafx.application.Platform#isSupported(javafx.application.ConditionalFeature)}.
     * If the feature is not supported by the platform, this style downgrades
     * to {@code StageStyle.UNDECORATED}
     */
    TRANSPARENT,

    /**
     * Defines a {@code Stage} style with a solid white background and minimal
     * platform decorations used for a utility window.
     */
    UTILITY,

    /**
     * Defines a {@code Stage} style with platform decorations and eliminates the border between
     * client area and decorations. The client area background is unified with the decorations.
     * This is a conditional feature, to check if it is supported see
     * {@link javafx.application.Platform#isSupported(javafx.application.ConditionalFeature)}.
     * If the feature is not supported by the platform, this style downgrades to {@code StageStyle.DECORATED}
     * <p>
     * NOTE: To see the effect, the {@code Scene} covering the {@code Stage} should have {@code Color.TRANSPARENT}
     * @since JavaFX 8.0
     */
    UNIFIED
}
