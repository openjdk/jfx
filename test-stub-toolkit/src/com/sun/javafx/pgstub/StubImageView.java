/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
/*
 * StubImageView.fx
 */

package com.sun.javafx.pgstub;

import javafx.geometry.Rectangle2D;

import com.sun.javafx.sg.PGImageView;

public class StubImageView extends StubShape implements PGImageView {

    // for tests
    private Object image;
    private float x;
    private float y;
    private boolean smooth;

    private float fitWidth;
    private float fitHeight;
    private Rectangle2D viewport;
    private boolean preserveRatio;

    @Override
    public void setImage(Object image) {
        this.image = image;
    }

    public Object getImage() {
        return image;
    }

    @Override
    public void setX(float x) {
        this.x = x;
    }

    public float getX() {
        return x;
    }

    @Override
    public void setY(float y) {
        this.y = y;
    }

    public float getY() {
        return y;
    }

    @Override
    public void setViewport(float fitWidth, float fitHeight,
                            float vx, float vy, float vw, float vh,
                            boolean preserveRatio) {
        this.fitWidth = fitWidth;
        this.fitHeight = fitHeight;
        this.viewport = new Rectangle2D(vx, vy, vw, vh);
        this.preserveRatio = preserveRatio;
    }

    @Override
    public void setSmooth(boolean smooth) {
        this.smooth = smooth;
    }

    public boolean isSmooth() { return this.smooth; }

    public float getFitWidth() {
        return fitWidth;
    }

    public float getFitHeight() {
        return fitHeight;
    }

    public Rectangle2D getViewport() {
        return viewport;
    }

    public boolean isPreserveRatio() {
        return preserveRatio;
    }
}
