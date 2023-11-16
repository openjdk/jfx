/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.demo.rich;

import javafx.scene.Node;
import javafx.incubator.scene.control.rich.SideDecorator;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

public class DemoColorSideDecorator implements SideDecorator {
    public DemoColorSideDecorator() {
    }

    @Override
    public double getPrefWidth(double viewWidth) {
        return 20.0;
    }

    @Override
    public Node getNode(int modelIndex, boolean forMeasurement) {
        int num = 36;
        double a = 360.0 * (modelIndex % num) / num;
        Color c = Color.hsb(a, 0.5, 1.0);

        Region r = new Region();
        r.setOpacity(1.0);
        r.setBackground(new Background(new BackgroundFill(c, null, null)));
        return r;
    }
}
