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

package javafx.incubator.scene.control.rich.skin;

import java.text.DecimalFormat;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.incubator.scene.control.rich.SideDecorator;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;

/**
 * Side decorator that shows model 1-based line (paragraph) numbers.
 */
public class LineNumberDecorator implements SideDecorator {
    private final DecimalFormat format;
    private final Background background;

    /**
     * Creates an instance using Western-style group separator.
     */
    public LineNumberDecorator() {
        this("#,##0");
    }

    /**
     * Creates an instance using specified pattern for {@link DecimalFormat}.
     * @param pattern DecimalFormat pattern to use
     */
    public LineNumberDecorator(String pattern) {
        format = new DecimalFormat(pattern);
        background = new Background(new BackgroundFill(Color.gray(0.5, 0.5), null, null));
    }

    @Override
    public double getPrefWidth(double viewWidth) {
        // no set width, must request a measurer Node
        return 0;
    }

    @Override
    public Node getNode(int ix, boolean forMeasurement) {
        if (forMeasurement) {
            // for measurer node only: allow for extra digit(s) in the bottom rows
            ix += 300;
        }

        String s = format.format(ix + 1);
        if(forMeasurement) {
            // account for some variability with proportional font
            s += " ";
        }

        Label t = new Label(s);
        // label needs to fill all available space
        t.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        // do not interfere with vflow layout
        t.setMinHeight(1);
        t.setPrefHeight(1);
        // numbers should be right aligned
        t.setAlignment(Pos.TOP_RIGHT);
        // not required; one may style the left side pane instead
        t.setBackground(background);
        t.setOpacity(1.0);
        return t;
    }
}
