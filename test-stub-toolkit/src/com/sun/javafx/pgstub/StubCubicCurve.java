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

/*
 * StubCubicCurve.fx
 */

package com.sun.javafx.pgstub;

import com.sun.javafx.sg.PGCubicCurve;

public class StubCubicCurve extends StubShape implements PGCubicCurve {
    private float x1;
    private float y1;
    private float x2;
    private float y2;
    private float ctrlX1;
    private float ctrlY1;
    private float ctrlX2;
    private float ctrlY2;

    public float getCtrlX1() {return ctrlX1;}
    public float getCtrlX2() {return ctrlX2;}
    public float getCtrlY1() {return ctrlY1;}
    public float getCtrlY2() {return ctrlY2;}
    public float getX1() {return x1;}
    public float getX2() {return x2;}
    public float getY1() {return y1;}
    public float getY2() {return y2;}

    @Override
    public void updateCubicCurve(float x1, float y1, float x2, float y2, float ctrlx1, float ctrly1, float ctrlx2, float ctrly2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.ctrlX1 = ctrlx1;
        this.ctrlY1 = ctrly1;
        this.ctrlX2 = ctrlx2;
        this.ctrlY2 = ctrly2;
    }
}
