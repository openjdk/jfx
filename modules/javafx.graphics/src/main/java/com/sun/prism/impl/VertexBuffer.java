/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.impl;

import com.sun.javafx.geom.transform.AffineBase;
import com.sun.prism.paint.Color;
import java.util.Arrays;

public final class VertexBuffer {

    protected static final int VERTS_PER_QUAD  = 4;

    protected static final int FLOATS_PER_TC   = 2;
    protected static final int FLOATS_PER_VC   = 3;
    protected static final int FLOATS_PER_VERT = FLOATS_PER_VC + (2 * FLOATS_PER_TC);

    protected static final int BYTES_PER_VERT = 4;

    protected static final int VCOFF = 0;
    protected static final int TC1OFF = VCOFF  + FLOATS_PER_VC;
    protected static final int TC2OFF = TC1OFF + FLOATS_PER_TC;

    protected int capacity, index;

    protected byte r, g, b, a;

    protected byte  colorArray[];
    protected float coordArray[];

    private final BaseContext ownerCtx;

    public VertexBuffer(BaseContext owner, int maxQuads) {
        this.ownerCtx = owner;
        capacity = maxQuads * VERTS_PER_QUAD;
        index = 0;

        colorArray = new byte [capacity * BYTES_PER_VERT];
        coordArray = new float[capacity * FLOATS_PER_VERT];
    }

    public final void setPerVertexColor(Color c, float extraAlpha) {
        float ca = c.getAlpha() * extraAlpha;
        r = (byte)(c.getRed()   * ca * 0xff);
        g = (byte)(c.getGreen() * ca * 0xff);
        b = (byte)(c.getBlue()  * ca * 0xff);
        a = (byte)(               ca * 0xff);
    }

    public final void setPerVertexColor(float extraAlpha) {
        r = g = b = a = (byte)(extraAlpha * 0xff);
    }

    public final void updateVertexColors(int numVerts) {
        for (int i=0; i!=numVerts; ++i) {
            putColor(i);
        }
    }

    private void putColor(int idx) {
        int i = idx * BYTES_PER_VERT;
        colorArray[i+0] = r;
        colorArray[i+1] = g;
        colorArray[i+2] = b;
        colorArray[i+3] = a;
    }

    /**
     * Flushes (renders) all pending vertices (triangles) in the buffer to the
     * owner BaseContext.  This operation only applies to heavyweight
     * buffers; calling flush() on a lightweight buffer will result in an
     * exception.
     */
    public final void flush() {
        if (index > 0) {
            ownerCtx.drawQuads(coordArray, colorArray, index);
            index = 0;
        }
    }

    public final void rewind() {
        index = 0;
    }

    private void grow() {
        capacity *= 2;
        colorArray = Arrays.copyOf(colorArray, capacity * BYTES_PER_VERT);
        coordArray = Arrays.copyOf(coordArray, capacity * FLOATS_PER_VERT);
    }

    public final void addVert(float x, float y) {
        // unlike the other (private) addVert() variants, this checks capacity
        if (index == capacity) {
            grow();
        }

        int i = FLOATS_PER_VERT * index;
        coordArray[i+0] = x;
        coordArray[i+1] = y;
        coordArray[i+2] = 0f;
        putColor(index);
        index++;
    }

    public final void addVert(float x, float y, float tx, float ty) {
        // unlike the (private) addVert() variants, this checks capacity
        if (index == capacity) {
            grow();
        }

        int i = FLOATS_PER_VERT * index;
        coordArray[i+0] = x;
        coordArray[i+1] = y;
        coordArray[i+2] = 0f;
        coordArray[i+3] = tx;
        coordArray[i+4] = ty;
        putColor(index);
        index++;
    }

    public final void addVert(float x, float y, float t0x, float t0y, float t1x, float t1y) {
        // unlike the (private) addVert() variants, this checks capacity
        if (index == capacity) {
            grow();
        }

        int i = FLOATS_PER_VERT * index;
        coordArray[i+0] = x;
        coordArray[i+1] = y;
        coordArray[i+2] = 0f;
        coordArray[i+3] = t0x;
        coordArray[i+4] = t0y;
        coordArray[i+5] = t1x;
        coordArray[i+6] = t1y;
        putColor(index);
        index++;
    }

    private void addVertNoCheck(float x, float y) {
        // note: assumes caller has already checked capacity
        int i = FLOATS_PER_VERT * index;
        coordArray[i+0] = x;
        coordArray[i+1] = y;
        coordArray[i+2] = 0f;
        putColor(index);
        index++;
    }

    private void addVertNoCheck(float x, float y, float tx, float ty) {
        // note: assumes caller has already checked capacity
        int i = FLOATS_PER_VERT * index;
        coordArray[i+0] = x;
        coordArray[i+1] = y;
        coordArray[i+2] = 0f;
        coordArray[i+3] = tx;
        coordArray[i+4] = ty;
        putColor(index);
        index++;
    }

    private void addVertNoCheck(float x, float y, float t0x, float t0y, float t1x, float t1y) {
        // note: assumes caller has already checked capacity
        int i = FLOATS_PER_VERT * index;
        coordArray[i+0] = x;
        coordArray[i+1] = y;
        coordArray[i+2] = 0f;
        coordArray[i+3] = t0x;
        coordArray[i+4] = t0y;
        coordArray[i+5] = t1x;
        coordArray[i+6] = t1y;
        putColor(index);
        index++;
    }

    private void ensureCapacityForQuad() {
        if (index + VERTS_PER_QUAD > capacity) {
            ownerCtx.drawQuads(coordArray, colorArray, index);
            index = 0;
        }
    }

    public final void addQuad(float dx1, float dy1, float dx2, float dy2) {
        ensureCapacityForQuad();

        addVertNoCheck(dx1, dy1);
        addVertNoCheck(dx1, dy2);
        addVertNoCheck(dx2, dy1);
        addVertNoCheck(dx2, dy2);
    }

    public final void addQuad(
            float dx1, float dy1, float dx2, float dy2,
            float t1x1, float t1y1, float t1x2, float t1y2,
            float t2x1, float t2y1, float t2x2, float t2y2)
    {
        ensureCapacityForQuad();

        addVertNoCheck(dx1, dy1, t1x1, t1y1, t2x1, t2y1);
        addVertNoCheck(dx1, dy2, t1x1, t1y2, t2x1, t2y2);
        addVertNoCheck(dx2, dy1, t1x2, t1y1, t2x2, t2y1);
        addVertNoCheck(dx2, dy2, t1x2, t1y2, t2x2, t2y2);
    }

    public final void addMappedQuad(
            float dx1, float dy1, float dx2, float dy2,
            float tx11, float ty11, float tx21, float ty21,
            float tx12, float ty12, float tx22, float ty22)
    {
        ensureCapacityForQuad();

        addVertNoCheck(dx1, dy1, tx11, ty11);
        addVertNoCheck(dx1, dy2, tx12, ty12);
        addVertNoCheck(dx2, dy1, tx21, ty21);
        addVertNoCheck(dx2, dy2, tx22, ty22);
    }

    public final void addMappedQuad(
            float dx1, float dy1, float dx2, float dy2,
            float ux11, float uy11, float ux21, float uy21,
            float ux12, float uy12, float ux22, float uy22,
            float vx11, float vy11, float vx21, float vy21,
            float vx12, float vy12, float vx22, float vy22)
    {
        ensureCapacityForQuad();

        addVertNoCheck(dx1, dy1, ux11, uy11, vx11, vy11);
        addVertNoCheck(dx1, dy2, ux12, uy12, vx12, vy12);
        addVertNoCheck(dx2, dy1, ux21, uy21, vx21, vy21);
        addVertNoCheck(dx2, dy2, ux22, uy22, vx22, vy22);
    }

    public final void addQuad(
            float dx1, float dy1, float dx2, float dy2,
            float tx1, float ty1, float tx2, float ty2,
            AffineBase tx)
    {
        addQuad(dx1, dy1, dx2, dy2, tx1, ty1, tx2, ty2);

        if (tx != null) {
            int i = FLOATS_PER_VERT * index - FLOATS_PER_VERT;
            tx.transform(coordArray, i+VCOFF, coordArray, i+TC2OFF, 1);
            i -= FLOATS_PER_VERT;
            tx.transform(coordArray, i+VCOFF, coordArray, i+TC2OFF, 1);
            i -= FLOATS_PER_VERT;
            tx.transform(coordArray, i+VCOFF, coordArray, i+TC2OFF, 1);
            i -= FLOATS_PER_VERT;
            tx.transform(coordArray, i+VCOFF, coordArray, i+TC2OFF, 1);
        }
    }

    public final void addSuperQuad(
            float dx1, float dy1, float dx2, float dy2,
            float tx1, float ty1, float tx2, float ty2,
            boolean isText)
    {
//        ensureCapacityForQuad();
        int idx = index;
        if (idx + VERTS_PER_QUAD > capacity) {
            ownerCtx.drawQuads(coordArray, colorArray, idx);
            idx = index = 0;
        }

        int i = FLOATS_PER_VERT * idx;
        float farr[] = coordArray;

        float text = isText ? 1 : 0;
        float image = isText ? 0 : 1;

//        addVertNoCheck(dx1, dy1, tx1, ty1);
        farr[  i] = dx1; farr[++i] = dy1; farr[++i] = 0;
        farr[++i] = tx1; farr[++i] = ty1;
        farr[++i] = image; farr[++i] = text; i++;
//        addVertNoCheck(dx1, dy2, tx1, ty2);
        farr[  i] = dx1; farr[++i] = dy2; farr[++i] = 0;
        farr[++i] = tx1; farr[++i] = ty2;
        farr[++i] = image; farr[++i] = text; i++;
//        addVertNoCheck(dx2, dy1, tx2, ty1);
        farr[  i] = dx2; farr[++i] = dy1; farr[++i] = 0;
        farr[++i] = tx2; farr[++i] = ty1;
        farr[++i] = image; farr[++i] = text; i++;
//        addVertNoCheck(dx2, dy2, tx2, ty2);
        farr[  i] = dx2; farr[++i] = dy2; farr[++i] = 0;
        farr[++i] = tx2; farr[++i] = ty2;
        farr[++i] = image; farr[++i] = text; i++;

        byte barr[] = colorArray;
        byte r = this.r, g = this.g, b = this.b, a = this.a;
        int j = BYTES_PER_VERT * idx;
        barr[  j] = r; barr[++j] = g; barr[++j] = b; barr[++j] = a;
        barr[++j] = r; barr[++j] = g; barr[++j] = b; barr[++j] = a;
        barr[++j] = r; barr[++j] = g; barr[++j] = b; barr[++j] = a;
        barr[++j] = r; barr[++j] = g; barr[++j] = b; barr[++j] = a;

        index = idx + VERTS_PER_QUAD;
    }

    public final void addQuad(
            float dx1, float dy1, float dx2, float dy2,
            float tx1, float ty1, float tx2, float ty2)
    {
//        ensureCapacityForQuad();
        int idx = index;
        if (idx + VERTS_PER_QUAD > capacity) {
            ownerCtx.drawQuads(coordArray, colorArray, idx);
            idx = index = 0;
        }

        int i = FLOATS_PER_VERT * idx;
        float farr[] = coordArray;

//        addVertNoCheck(dx1, dy1, tx1, ty1);
        farr[  i] = dx1; farr[++i] = dy1; farr[++i] = 0;
        farr[++i] = tx1; farr[++i] = ty1;
        i += 3;
//        addVertNoCheck(dx1, dy2, tx1, ty2);
        farr[  i] = dx1; farr[++i] = dy2; farr[++i] = 0;
        farr[++i] = tx1; farr[++i] = ty2;
        i += 3;
//        addVertNoCheck(dx2, dy1, tx2, ty1);
        farr[  i] = dx2; farr[++i] = dy1; farr[++i] = 0;
        farr[++i] = tx2; farr[++i] = ty1;
        i += 3;
//        addVertNoCheck(dx2, dy2, tx2, ty2);
        farr[  i] = dx2; farr[++i] = dy2; farr[++i] = 0;
        farr[++i] = tx2; farr[++i] = ty2;

        byte barr[] = colorArray;
        byte r = this.r, g = this.g, b = this.b, a = this.a;
        int j = BYTES_PER_VERT * idx;
        barr[  j] = r; barr[++j] = g; barr[++j] = b; barr[++j] = a;
        barr[++j] = r; barr[++j] = g; barr[++j] = b; barr[++j] = a;
        barr[++j] = r; barr[++j] = g; barr[++j] = b; barr[++j] = a;
        barr[++j] = r; barr[++j] = g; barr[++j] = b; barr[++j] = a;

        index = idx + VERTS_PER_QUAD;
    }

    public final void addQuadVO(float topopacity, float botopacity,
            float dx1, float dy1, float dx2, float dy2,
            float tx1, float ty1, float tx2, float ty2)
    {
        int idx = index;
        if (idx + VERTS_PER_QUAD > capacity) {
            ownerCtx.drawQuads(coordArray, colorArray, idx);
            idx = index = 0;
        }

        int i = FLOATS_PER_VERT * idx;
        float farr[] = coordArray;

        // addVertNoCheck(dx1, dy1, tx1, ty1, topopacity);
        farr[  i] = dx1; farr[++i] = dy1; farr[++i] = 0;
        farr[++i] = tx1; farr[++i] = ty1;
        i += 3;

        // addVertNoCheck(dx1, dy2, tx1, ty2, botopacity);
        farr[  i] = dx1; farr[++i] = dy2; farr[++i] = 0;
        farr[++i] = tx1; farr[++i] = ty2;
        i += 3;

        // addVertNoCheck(dx2, dy1, tx2, ty1, topopacity);
        farr[  i] = dx2; farr[++i] = dy1; farr[++i] = 0;
        farr[++i] = tx2; farr[++i] = ty1;
        i += 3;

        // addVertNoCheck(dx2, dy2, tx2, ty2, botopacity);
        farr[  i] = dx2; farr[++i] = dy2; farr[++i] = 0;
        farr[++i] = tx2; farr[++i] = ty2;

        byte barr[] = colorArray;
        int j = BYTES_PER_VERT * idx;

        byte to = (byte)(topopacity * 0xff);
        byte bo = (byte)(botopacity * 0xff);

        barr[  j] = to; barr[++j] = to; barr[++j] = to; barr[++j] = to;
        barr[++j] = bo; barr[++j] = bo; barr[++j] = bo; barr[++j] = bo;
        barr[++j] = to; barr[++j] = to; barr[++j] = to; barr[++j] = to;
        barr[++j] = bo; barr[++j] = bo; barr[++j] = bo; barr[++j] = bo;

        index = idx + VERTS_PER_QUAD;
    }

    public final void addMappedPgram(
            float dx11, float dy11, float dx21, float dy21,
            float dx12, float dy12, float dx22, float dy22,
            float ux11, float uy11, float ux21, float uy21,
            float ux12, float uy12, float ux22, float uy22,
            float vx11, float vy11, float vx22, float vy22,
            AffineBase tx)
    {
        addMappedPgram(dx11, dy11, dx21, dy21, dx12, dy12, dx22, dy22,
                       ux11, uy11, ux21, uy21, ux12, uy12, ux22, uy22,
                       vx11, vy11, vx22, vy11, vx11, vy22, vx22, vy22);

        int i = FLOATS_PER_VERT * index - FLOATS_PER_VERT;
        tx.transform(coordArray, i+TC2OFF, coordArray, i+TC2OFF, 1);
        i -= FLOATS_PER_VERT;
        tx.transform(coordArray, i+TC2OFF, coordArray, i+TC2OFF, 1);
        i -= FLOATS_PER_VERT;
        tx.transform(coordArray, i+TC2OFF, coordArray, i+TC2OFF, 1);
        i -= FLOATS_PER_VERT;
        tx.transform(coordArray, i+TC2OFF, coordArray, i+TC2OFF, 1);
    }

    public final void addMappedPgram(
            float dx11, float dy11, float dx21, float dy21,
            float dx12, float dy12, float dx22, float dy22,
            float ux11, float uy11, float ux21, float uy21,
            float ux12, float uy12, float ux22, float uy22,
            float vx, float vy)
    {
        int idx = index;
        if (idx + VERTS_PER_QUAD > capacity) {
            ownerCtx.drawQuads(coordArray, colorArray, idx);
            idx = index = 0;
        }

        int i = FLOATS_PER_VERT * idx;
        float farr[] = coordArray;

        //addVertNoCheck(dx11, dy11, ux11, uy11, vx, vy);
        farr[i]   = dx11; farr[++i] = dy11; farr[++i] = 0;
        farr[++i] = ux11; farr[++i] = uy11;
        farr[++i] = vx; farr[++i] = vy;

        //addVertNoCheck(dx12, dy12, ux12, uy12, vx, vy);
        farr[++i] = dx12; farr[++i] = dy12; farr[++i] = 0;
        farr[++i] = ux12; farr[++i] = uy12;
        farr[++i] = vx; farr[++i] = vy;

        //addVertNoCheck(dx21, dy21, ux21, uy21, vx, vy);
        farr[++i] = dx21; farr[++i] = dy21; farr[++i] = 0;
        farr[++i] = ux21; farr[++i] = uy21;
        farr[++i] = vx; farr[++i] = vy;

            //addVertNoCheck(dx22, dy22, ux22, uy22, vx, vy);
        farr[++i] = dx22; farr[++i] = dy22; farr[++i] = 0;
        farr[++i] = ux22; farr[++i] = uy22;
        farr[++i] = vx; farr[++i] = vy;

        byte barr[] = colorArray;
        byte r = this.r, g = this.g, b = this.b, a = this.a;
        int j = BYTES_PER_VERT * idx;
        barr[  j] = r; barr[++j] = g; barr[++j] = b; barr[++j] = a;
        barr[++j] = r; barr[++j] = g; barr[++j] = b; barr[++j] = a;
        barr[++j] = r; barr[++j] = g; barr[++j] = b; barr[++j] = a;
        barr[++j] = r; barr[++j] = g; barr[++j] = b; barr[++j] = a;

        index = idx + VERTS_PER_QUAD;
    }

    public final void addMappedPgram(
            float dx11, float dy11, float dx21, float dy21,
            float dx12, float dy12, float dx22, float dy22,
            float ux11, float uy11, float ux21, float uy21,
            float ux12, float uy12, float ux22, float uy22,
            float vx11, float vy11, float vx21, float vy21,
            float vx12, float vy12, float vx22, float vy22)
    {
        int idx = index;
        if (idx + VERTS_PER_QUAD > capacity) {
            ownerCtx.drawQuads(coordArray, colorArray, idx);
            idx = index = 0;
        }

        int i = FLOATS_PER_VERT * idx;
        float farr[] = coordArray;

        //addVertNoCheck(dx11, dy11, ux11, uy11, vx, vy);
        farr[i]   = dx11; farr[++i] = dy11; farr[++i] = 0;
        farr[++i] = ux11; farr[++i] = uy11;
        farr[++i] = vx11; farr[++i] = vy11;

        //addVertNoCheck(dx12, dy12, ux12, uy12, vx, vy);
        farr[++i] = dx12; farr[++i] = dy12; farr[++i] = 0;
        farr[++i] = ux12; farr[++i] = uy12;
        farr[++i] = vx12; farr[++i] = vy12;

        //addVertNoCheck(dx21, dy21, ux21, uy21, vx, vy);
        farr[++i] = dx21; farr[++i] = dy21; farr[++i] = 0;
        farr[++i] = ux21; farr[++i] = uy21;
        farr[++i] = vx21; farr[++i] = vy21;

        //addVertNoCheck(dx22, dy22, ux22, uy22, vx, vy);
        farr[++i] = dx22; farr[++i] = dy22; farr[++i] = 0;
        farr[++i] = ux22; farr[++i] = uy22;
        farr[++i] = vx22; farr[++i] = vy22;

        byte barr[] = colorArray;
        byte r = this.r, g = this.g, b = this.b, a = this.a;
        int j = BYTES_PER_VERT * idx;
        barr[  j] = r; barr[++j] = g; barr[++j] = b; barr[++j] = a;
        barr[++j] = r; barr[++j] = g; barr[++j] = b; barr[++j] = a;
        barr[++j] = r; barr[++j] = g; barr[++j] = b; barr[++j] = a;
        barr[++j] = r; barr[++j] = g; barr[++j] = b; barr[++j] = a;

        index = idx + VERTS_PER_QUAD;
    }
}
