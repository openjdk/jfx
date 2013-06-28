/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.fxml.builder;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.DefaultProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.Builder;

/**
 * JavaFX scene builder.
 */
@DefaultProperty("root")
public class JavaFXSceneBuilder implements Builder<Scene> {
    private Parent root = null;
    private double width = -1;
    private double height = -1;
    private Paint fill = Color.WHITE;
    private ArrayList<String> stylesheets = new ArrayList<String>();

    public Parent getRoot() {
        return root;
    }

    public void setRoot(Parent root) {
        this.root = root;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        if (width < -1) {
            throw new IllegalArgumentException();
        }

        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        if (height < -1) {
            throw new IllegalArgumentException();
        }

        this.height = height;
    }

    public Paint getFill() {
        return fill;
    }

    public void setFill(Paint fill) {
        if (fill == null) {
            throw new NullPointerException();
        }

        this.fill = fill;
    }

    public List<String> getStylesheets() {
        return stylesheets;
    }

    @Override
    public Scene build() {
        Scene scene = new Scene(root, width, height, fill);

        for (String stylesheet : stylesheets) {
            scene.getStylesheets().add(stylesheet);
        }

        return scene;
    }
}
