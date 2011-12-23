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
 * StubQuadCurve.fx
 */

package com.sun.javafx.pgstub;

import com.sun.javafx.sg.PGQuadCurve;

public class StubQuadCurve extends StubShape implements PGQuadCurve {
    private float x1, y1, x2, y2, ctrlX, ctrlY;

    public float getCtrlX() {
        return ctrlX;
    }

    public float getCtrlY() {
        return ctrlY;
    }

    public float getX1() {
        return x1;
    }

    public float getX2() {
        return x2;
    }

    public float getY1() {
        return y1;
    }

    public float getY2() {
        return y2;
    }
    
    @Override
    public void updateQuadCurve(float x1, float y1, float x2, float y2, float ctrlx, float ctrly) {
    	this.x1 = x1;
    	this.y1 = y1;
    	this.x2 = x2;
    	this.y2 = y2;
    	this.ctrlX = ctrlx;
    	this.ctrlY = ctrly;
    }
    
    public void setX1(float x1) {this.x1 = x1; }
    public void setY1(float y1) {this.y1 = y1;}
    public void setX2(float x2) {this.x2 = x2; }
    public void setY2(float y2) {this.y2 = y2; }
    public void setCtrlX(float ctrlx) {this.ctrlX = ctrlx; }
    public void setCtrlY(float ctrly) {this.ctrlY = ctrly; }
}
