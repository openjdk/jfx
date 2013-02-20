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

package com.sun.javafx.pgstub;

import com.sun.javafx.sg.PGRectangle;

public class StubRectangle extends StubShape implements PGRectangle {
    // for tests
    private float x;
    private float y;
    private float width;
    private float height;
    private float arcWidth;
    private float arcHeight;

    public void setX(float x) {this.x = x;}
    public void setY(float y) {this.y = y;}
    public void setWidth(float width) {this.width = width;}
    public void setHeight(float height) {this.height = height;}
    public void setArcWidth(float arcWidth) {this.arcWidth = arcWidth;}
    public void setArcHeight(float arcHeight) {this.arcHeight = arcHeight;}
    public float getArcHeight() {return arcHeight;}
    public float getArcWidth() {return arcWidth;}
    public float getHeight() {return height;}
    public float getWidth() {return width;}
    public float getX() {return x;}
    public float getY() {return y;}

    @Override
    public void updateRectangle(float x, float y, float width, float height, float arcWidth, float arcHeight) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.arcWidth = arcWidth;
        this.arcHeight = arcHeight;
    }
}
