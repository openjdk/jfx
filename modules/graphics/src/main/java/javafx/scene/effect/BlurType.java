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

package javafx.scene.effect;

/**
 * Represents the type of blur algorithm that is used to soften
 * a {@code Shadow} effect.
 * @since JavaFX 2.0
 */
public enum BlurType {

    /**
     * A single pass of a box filter is used to blur the shadow.
     */
    ONE_PASS_BOX,

    /**
     * Two passes of a box filter are used to blur the shadow for a slightly
     * smoother effect.
     */
    TWO_PASS_BOX,

    /**
     * Three passes of a box filter are used to blur the shadow for an
     * effect that is almost as smooth as a Gaussian filter.
     */
    THREE_PASS_BOX,

    /**
     * A Gaussian blur kernel is used to blur the shadow with very high
     * quality.
     */
    GAUSSIAN,

}
