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

package com.sun.prism.image;

import com.sun.prism.Graphics;
import com.sun.prism.Texture;

public class Coords {
    float x0, y0, x1, y1;
    float u0, v0, u1, v1;

    public Coords(float w, float h, ViewPort v) {
        x0 = 0; x1 = w;
        y0 = 0; y1 = h;
        u0 = v.u0; u1 = v.u1;
        v0 = v.v0; v1 = v.v1;
    }

    public Coords() {
    }

    public void draw(Texture t, Graphics g, float x, float y) {
        g.drawTexture(t,
                      x + x0, y + y0, x + x1, y + y1,
                      u0, v0, u1, v1);
    }

    // returns x corresponding for u
    public float getX(float u) {
        return (x0 * (u1 - u) + x1 * (u - u0)) / (u1 - u0);
    }

    // returns y corresponding for v
    public float getY(float v) {
        return (y0 * (v1 - v) + y1 * (v - v0)) / (v1 - v0);
    }
}
