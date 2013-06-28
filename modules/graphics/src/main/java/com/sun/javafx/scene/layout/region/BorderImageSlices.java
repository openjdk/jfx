/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.layout.region;

import javafx.scene.layout.BorderWidths;

/**
 * A helper class during the conversion process.
 */
public class BorderImageSlices {

    /**
     * Using EMPTY results in no border-image being drawn since the slices are zero. You probably
     * want to use {@link BorderImageSlices#DEFAULT}
     */
    public static final BorderImageSlices EMPTY = new BorderImageSlices(BorderWidths.EMPTY, false);

    /**
     * Default border-image-slice is 100%
     * @see <a href="http://www.w3.org/TR/css3-background/#the-border-image-slice">border-image-slice</a>
     */
    public static final BorderImageSlices DEFAULT = new BorderImageSlices(BorderWidths.FULL, false);

    public BorderWidths widths;
    public boolean filled;

    public BorderImageSlices(BorderWidths widths, boolean filled) {
        this.widths = widths;
        this.filled = filled;
    }
}
