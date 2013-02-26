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

package com.sun.prism.sw;

import com.sun.openpisces.AlphaConsumer;
import com.sun.openpisces.Renderer;
import com.sun.pisces.PiscesRenderer;

final class DirectRTPiscesAlphaConsumer implements AlphaConsumer {

    private byte alpha_map[];
    private int outpix_xmin;
    private int outpix_ymin;
    private int w;
    private int h;
    private int rowNum;

    private PiscesRenderer pr;

    void initConsumer(Renderer renderer, PiscesRenderer pr) {
        outpix_xmin = renderer.getOutpixMinX();
        outpix_ymin = renderer.getOutpixMinY();
        w = renderer.getOutpixMaxX() - outpix_xmin;
        if (w < 0) { w = 0; }
        h = renderer.getOutpixMaxY() - outpix_ymin;
        if (h < 0) { h = 0; }
        rowNum = 0;
        this.pr = pr;
    }

    @Override
    public int getOriginX() {
        return outpix_xmin;
    }

    @Override
    public int getOriginY() {
        return outpix_ymin;
    }

    @Override
    public int getWidth() {
        return w;
    }

    @Override
    public int getHeight() {
        return h;
    }

    @Override
    public void setMaxAlpha(int maxalpha) {
        if ((alpha_map == null) || (alpha_map.length != maxalpha+1)) {
            alpha_map = new byte[maxalpha+1];
            for (int i = 0; i <= maxalpha; i++) {
                alpha_map[i] = (byte) ((i*255 + maxalpha/2)/maxalpha);
            }
        }
    }

    @Override
    public void setAndClearRelativeAlphas(int[] alphaDeltas, int pix_y, int firstdelta, int lastdelta) {
        pr.emitAndClearAlphaRow(alpha_map, alphaDeltas, pix_y, firstdelta, lastdelta, rowNum);
        rowNum++;
    }
}
