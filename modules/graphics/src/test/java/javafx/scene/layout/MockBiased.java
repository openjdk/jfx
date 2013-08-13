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

package javafx.scene.layout;

import javafx.geometry.Orientation;


public class MockBiased extends Region {
    private double prefWidth;
    private double prefHeight;
    private double area;
    private Orientation bias;

    public MockBiased(Orientation bias, double prefWidth, double prefHeight) {
        this.bias = bias;
        this.prefWidth = prefWidth;
        this.prefHeight = prefHeight;
        this.area = prefWidth*prefHeight;
    }

    @Override public Orientation getContentBias() {
        return bias;
    }
    @Override protected double computeMinWidth(double height) {
        return bias == Orientation.HORIZONTAL? 10 :
            area/(height != -1? height : prefHeight(-1));
    }
    @Override protected double computeMinHeight(double width) {
        return bias == Orientation.VERTICAL? 10 :
           area/(width != -1? width : prefWidth(-1));
    }
    @Override protected double computePrefWidth(double height) {
        return bias == Orientation.HORIZONTAL? prefWidth :
            area/(height != -1? height : prefHeight(-1));
    }
    @Override protected double computePrefHeight(double width) {
        return bias == Orientation.VERTICAL? prefHeight :
            area/(width != -1? width : prefWidth(-1));
    }
    @Override protected double computeMaxWidth(double height) {
        return bias == Orientation.HORIZONTAL? area :
            area/(height != -1? height : prefHeight(-1));
    }
    @Override protected double computeMaxHeight(double width) {
        return bias == Orientation.VERTICAL? area :
            area/(width != -1? width : prefWidth(-1));
    }


}
