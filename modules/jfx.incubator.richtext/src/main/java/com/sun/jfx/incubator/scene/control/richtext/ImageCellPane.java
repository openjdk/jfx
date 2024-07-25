/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jfx.incubator.scene.control.richtext;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

/**
 * Content pane for RichParagraph that shows a single image.
 * The image gets resized if it cannot fit into available width.
 */
public class ImageCellPane extends Pane {
    private final Image image;
    private final ImageView imageView;
    private static final Insets PADDING = new Insets(1, 1, 1, 1);

    /**
     * The constructor.
     * @param image the image
     */
    public ImageCellPane(Image image) {
        this.image = image;

        imageView = new ImageView(image);
        imageView.setSmooth(true);
        imageView.setPreserveRatio(true);
        getChildren().add(imageView);

        setPadding(PADDING);
        getStyleClass().add("image-cell-pane");
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double sc;
        if (width < image.getWidth()) {
            sc = width / image.getWidth();
        } else {
            sc = 1.0;
        }
        imageView.setScaleX(sc);
        imageView.setScaleY(sc);

        double x0 = snappedLeftInset();
        double y0 = snappedTopInset();
        layoutInArea(
            imageView,
            x0,
            y0,
            image.getWidth() * sc,
            image.getHeight() * sc,
            0,
            PADDING,
            true,
            false,
            HPos.CENTER,
            VPos.CENTER
        );
    }

    @Override
    protected double computePrefHeight(double w) {
        double pad = snappedTopInset() + snappedBottomInset();
        if (w != -1) {
            if (w < image.getWidth()) {
                return pad + (image.getHeight() * w / image.getWidth());
            }
        }
        return pad + (image.getHeight());
    }
}
