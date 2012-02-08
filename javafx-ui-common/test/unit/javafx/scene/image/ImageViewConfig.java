/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.image;

import javafx.geometry.Rectangle2D;

public final class ImageViewConfig {
    private final Image image;
    private final float x;
    private final float y;
    private final Rectangle2D viewport;
    private final float fitWidth;
    private final float fitHeight;
    private final boolean preserveRatio;

    private ImageViewConfig(final Image image,
                            final float x,
                            final float y,
                            final Rectangle2D viewport,
                            final float fitWidth,
                            final float fitHeight,
                            final boolean preserveRatio) {
        this.image = image;
        this.x = x;
        this.y = y;
        this.viewport = viewport;
        this.fitWidth = fitWidth;
        this.fitHeight = fitHeight;
        this.preserveRatio = preserveRatio;
    }

    public void applyTo(final ImageView imageView) {
        imageView.setImage(image);
        imageView.setX(x);
        imageView.setY(y);
        imageView.setViewport(viewport);
        imageView.setFitWidth(fitWidth);
        imageView.setFitHeight(fitHeight);
        imageView.setPreserveRatio(preserveRatio);
    }

    public static ImageViewConfig config(final Image image,
                                         final float x,
                                         final float y) {
        return new ImageViewConfig(image, x, y, null, 0, 0, false);
    }

    public static ImageViewConfig config(final Image image,
                                         final float x,
                                         final float y,
                                         final float fitWidth,
                                         final float fitHeight,
                                         final boolean preserveRatio) {
        return new ImageViewConfig(image, x, y, null, fitWidth, fitHeight,
                                   preserveRatio);
    }

    public static ImageViewConfig config(final Image image,
                                         final float x,
                                         final float y,
                                         final float vpX,
                                         final float vpY,
                                         final float vpWidth,
                                         final float vpHeight,
                                         final float fitWidth,
                                         final float fitHeight,
                                         final boolean preserveRatio) {
        return new ImageViewConfig(image, x, y,
                                   new Rectangle2D(vpX, vpY, vpWidth, vpHeight),
                                   fitWidth, fitHeight, preserveRatio);
    }
}
