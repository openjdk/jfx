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

package com.sun.javafx.scene.paint;

import javafx.scene.image.Image;
import javafx.scene.paint.Paint;

import com.sun.javafx.tk.Toolkit;

/**
 * <p>The {@code ImagePattern} class fills a shape with an image pattern. The
 * user may specify the anchor rectangle, which defines the position,
 * width, and height of the image relative to the upper left corner of the
 * shape. If the shape is larger than the anchor rectangle, the image is tiled.
 * </p>
 *
 * <p>If the {@code proportional} variable is set to true (the default)
 * then the anchor rectangle should be specified relative to the unit
 * square (0.0->1.0) and will be stretched across the shape.
 * If the {@code proportional} variable is set to false, then the anchor
 * rectangle should be specified as absolute pixel values and the image
 * will be stretched to fit the anchor rectangle.</p>
 *
 * <p>The example below demonstrates the use of the {@code proportional}
 * variable.  The shapes on the top row use proportional coordinates
 * (the default) to specify the anchor rectangle.  The shapes on the
 * bottom row use absolute coordinates.  The flower image is stretched
 * to fill the entire triangle shape, while the dot pattern image is tiled
 * within the circle shape.</p>
 *
<pre><code>
import javafx.scene.Scene;
import javafx.scene.image.Image;
import com.sun.javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;

var dots:Image = Image {
    url: "{__DIR__}dots.png"
}

var flower:Image = Image {
    url: "{__DIR__}flower.png"
}

Stage {
    title: "Image Pattern"
    scene: Scene {
        width: 230
        height: 230
        content: [
            Polygon {
                translateX: 10
                translateY: 10
                points: [
                    50, 0,
                    100, 100,
                    0, 100
                ]
                fill: ImagePattern {
                    image: flower
                    x: 0
                    y: 0
                    width: 1
                    height: 1
                    proportional: true
                }
            },
            Polygon {
                translateX: 10
                translateY: 120
                points: [
                    50, 0,
                    100, 100,
                    0, 100
                ]
                fill: ImagePattern {
                    image: flower
                    x: 0
                    y: 0
                    width: 100
                    height: 100
                    proportional: false
                }
            },
            Circle {
                translateX: 120
                translateY: 10
                centerX: 50
                centerY: 50
                radius: 50
                fill: ImagePattern {
                    image: dots
                    x: 0.2
                    y: 0.2
                    width: 0.4
                    height: 0.4
                    proportional: true
                 }
            },
            Circle {
                translateX: 120
                translateY: 120
                centerX: 50
                centerY: 50
                radius: 50
                fill: ImagePattern {
                    image: dots
                    x: 20.0
                    y: 20.0
                    width: 40.0
                    height: 40.0
                    proportional: false
                }
            }
        ]
    }
}
</pre></code>
 * <p>The code above produces the following:</p>
 * <p><img src="doc-files/imagepattern.png"/></p>
 */
public class ImagePattern extends Paint {
    /**
     * The image to be used as a pattern.
     */
    private Image image;

    public final Image getImage() {
        return image;
    }
    
    /**
     * The x origin of the anchor rectangle.
     *
     * @default 0.0
     */
    private float x;

    public final float getX() {
        return x;
    }
    
    /**
     * The y origin of the anchor rectangle.
     *
     * @default 0.0
     */
    private float y;

    public final float getY() {
        return y;
    }
    
    /**
     * The width of the anchor rectangle.
     *
     * @default 1.0
     */
    private float width = 1f;

    public final float getWidth() {
        return width;
    }
    
    /**
     * The height of the anchor rectangle.
     *
     * @default 1.0
     */
    private float height = 1f;

    public final float getHeight() {
        return height;
    }
    
    /**
     * If proportional is true (the default), this value specifies a
     * point on a unit square that will be scaled to match the size of the
     * the shape that the image pattern fills.
     *
     * @default true
     */
    private boolean proportional = true;

    public final boolean isProportional() {
        return proportional;
    }

    private Object platformPaint;

    public ImagePattern(Image image) {
        this.image = image;
    }

    public ImagePattern(Image image, float x, float y, float width,
            float height, boolean proportional) {

        this.image = image;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.proportional = proportional;
    }

    /**
     * @treatAsPrivate implementation detail
     */
    @Override public Object impl_getPlatformPaint() {
        if (platformPaint == null) {
            platformPaint = Toolkit.getToolkit().getPaint(this);
        }
        return platformPaint;
    }
}
