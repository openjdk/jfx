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

package com.sun.prism;

import com.sun.javafx.geom.Rectangle;

public interface Presentable extends RenderTarget {
    /**
     * display the indicated region to the user.
     * @param dirtyregion display region or null for full area
     * @return true if the provided region was successfully displayed,
     * false otherwise
     */
    public boolean prepare(Rectangle dirtyregion);

    /**
     * present the prepared region to the user.
     */
    public boolean present();

    /**
     * query whether the presentable needs to be recreated on resize
     * @return true if the presentable must be recreated on resize;
     */
    public boolean recreateOnResize();

    public float getPixelScaleFactor();
}
