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

/*
 * Class ViewPort represents a portion of the Image being rendered
 * expressed in texture pixel coordinates, where the whole image is
 * [0..Image.getWidth()]x[0..Image.getHeight()]
 */

public class ViewPort {
    public float u0, v0, u1, v1;

    public ViewPort(float u, float v, float du, float dv) {
        u0 = u; u1 = u + du;
        v0 = v; v1 = v + dv;
    }

    public ViewPort getScaledVersion(float pixelScale) {
        if (pixelScale == 1.0f) {
            return this;
        }
        float newu0 = u0 * pixelScale;
        float newv0 = v0 * pixelScale;
        float newu1 = u1 * pixelScale;
        float newv1 = v1 * pixelScale;
        return new ViewPort(newu0, newv0, newu1 - newu0, newv1 - newv0);
    }

    /*
     * This methods calculate linear fuctions RelX(u) and RelY(v)
     * defined as follows:
     *     RelX(u0) = 0; RelX(u1) = 1;
     *     RelY(v0) = 0; RelY(v1) = 1;
     * which maps source : [u0..u1] x [v0..v1] -> [0..1] x [0..1]
     */
    public float getRelX(float u) { return (u - u0) / (u1 - u0); }
    public float getRelY(float v) { return (v - v0) / (v1 - v0); }

    public Coords getClippedCoords(float iw, float ih, float w, float h) /*const*/ {
        Coords cr = new Coords(w, h, this);

        if (u1 > iw || u0 < 0) {
            if (u0 >= iw || u1 <= 0) return null;

            if (u1 > iw) {
                cr.x1 = w * getRelX(iw);
                cr.u1 = iw;
            }
            if (u0 < 0) {
                cr.x0 = w * getRelX(0);
                cr.u0 = 0;
            }
        }

        if (v1 > ih || v0 < 0) {
            if (v0 >= ih || v1 <= 0) return null;

            if (v1 > ih) {
                cr.y1 = h * getRelY(ih);
                cr.v1 = ih;
            }
            if (v0 < 0) {
                cr.y0 = h * getRelY(0);
                cr.v0 = 0;
            }
        }

        return cr;
    }
}
