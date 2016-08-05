/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.layout;

/**
 * Enumeration of options for repeating images in backgrounds
 * @since JavaFX 8.0
 */
public enum BackgroundRepeat {
    /**
     * The image is repeated as often as needed to cover the area.
     */
    REPEAT,
    /**
     * The image is repeated as often as will fit within the area without being
     * clipped and then the images are spaced out to fill the area. The first
     * and last images touch the edges of the area.
     */
    SPACE,
    /**
     * The image is repeated as often as will fit within the area. If it
     * doesn't fit a whole number of times, it is reduced in size until it does.
     */
    ROUND,
    /**
     * The image is placed once and not repeated
     */
    NO_REPEAT
}
