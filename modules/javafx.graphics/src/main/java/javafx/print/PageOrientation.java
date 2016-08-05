/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.print;

/**
 * Specifies the orientation of the media sheet for printed pages.
 * @since JavaFX 8.0
 */
public enum PageOrientation {

    /**
     * The printable area's origin is at the top left of the paper.
     * X coordinates will increase across the short edge of the paper.
     * Y coordinates will increase along the long edge of the paper.
     */
    PORTRAIT,

    /**
     * The printable area's origin is at the bottom left of the paper.
     * X coordinates will increase along the long edge of the paper.
     * Y coordinates will increase along the short edge of the paper.
     */
    LANDSCAPE,

    /**
     * The printable area's origin is at the bottom right of the paper.
     * X coordinates will increase across the short edge of the paper.
     * Y coordinates will increase along the long edge of the paper.
     * i.e. rotated 180 degrees from <code>PORTRAIT</code>.
     */
    REVERSE_PORTRAIT,

    /**
     * The printable area's origin is at the top right of the paper.
     * X coordinates will increase along the long edge of the paper.
     * Y coordinates will increase along the short edge of the paper.
     * i.e. rotated 180 degrees from <code>LANDSCAPE</code>.
     */

    REVERSE_LANDSCAPE,
}
