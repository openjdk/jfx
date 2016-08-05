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

package com.sun.prism.image;

import com.sun.prism.Graphics;
import com.sun.prism.Image;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;

public abstract class CompoundImage {
    /*
     * Border used to pad each tile of the image to avoid filtering artifacts.
     * A single pixel is enough to avoid artifacts for BILINEAR filtering.
     */
    public static final int BORDER_SIZE_DEFAULT = 1;

    /*
     * uSubdivision and vSubdivision contain cutting points, where
     * the huge image is cut into a grid of uSections by vSections tiles.
     * The dimensions of the arrays are uSections+1 and vSections+1 accordingly.
     * 0 and image.getWH are always the first and the last values.
     * Actual sub-images are extended (padded) by the appropriate border value.
     */
    protected final int uSubdivision[], u0[], u1[];
    protected final int vSubdivision[], v0[], v1[];
    protected final int uSections, vSections;
    protected final int uBorderSize, vBorderSize;

    protected Image tiles[];

    public CompoundImage(Image image, int maxSize) {
        this(image, maxSize, BORDER_SIZE_DEFAULT);
    }

    public CompoundImage(Image image, int maxSize, int borderSize) {
        // constrain border size by 1/4th of maxSize
        if (4 * borderSize >= maxSize) borderSize = maxSize / 4;

        int imgW = image.getWidth();
        int imgH = image.getHeight();

        uBorderSize = (imgW <= maxSize) ? 0 : borderSize;
        vBorderSize = (imgH <= maxSize) ? 0 : borderSize;

        uSubdivision = subdivideUVs(imgW, maxSize, uBorderSize);
        vSubdivision = subdivideUVs(imgH, maxSize, vBorderSize);

        uSections = uSubdivision.length - 1;
        vSections = vSubdivision.length - 1;

        u0 = new int[uSections]; u1 = new int[uSections];
        v0 = new int[vSections]; v1 = new int[vSections];

        // subdivide initial image
        tiles = new Image[uSections * vSections];

        for (int y = 0; y != vSections; ++y) {
            v0[y] = vSubdivision[y] - uBorder(y);
            v1[y] = vSubdivision[y + 1] + dBorder(y);
        }

        for (int x = 0; x != uSections; ++x) {
            u0[x] = uSubdivision[x] - lBorder(x);
            u1[x] = uSubdivision[x + 1] + rBorder(x);
        }

        for (int y = 0; y != vSections; ++y) {
            for (int x = 0; x != uSections; ++x) {
                // System.out.println("["+y+"]["+x+"] : ("+u0[x]+","+v0[y]+")-("+u1[x]+","+v1[y]+")");
                tiles[y * uSections + x] =
                        image.createSubImage(u0[x], v0[y], u1[x] - u0[x], v1[y] - v0[y]);
            }
        }
    }

    private int lBorder(int i) { return i > 0 ? uBorderSize : 0; }
    private int rBorder(int i) { return (i < uSections - 1) ? uBorderSize : 0; }
    private int uBorder(int i) { return i > 0 ? vBorderSize : 0; }
    private int dBorder(int i) { return (i < vSections - 1) ? vBorderSize : 0; }

    private static int [] subdivideUVs(int size, int maxSize, int borderSize) {
        // for border cases maximum content size is  maxSize-borderSize
        // for inner cases maximum content size is  maxSize-borderSize*2

        int contSize = maxSize - borderSize * 2;
        int nImages = ((size - borderSize * 2) + contSize - 1) / contSize;

        int data[] = new int[nImages+1];

        data[0] = 0;
        data[nImages] = size;

        for (int i = 1; i < nImages; ++i) {
            data[i] = borderSize + contSize*i;
        }

        return data;
    }

    abstract protected Texture getTile(int x, int y, ResourceFactory factory);

    public void drawLazy(Graphics g, Coords crd, float x, float y) {
        new CompoundCoords(this, crd).draw(g, this, x, y);
    }
}
