/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.paint;

import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.Image;

public final class ImagePattern extends Paint {

    private final Image image;
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final BaseTransform patternTransform;

    public ImagePattern(Image image,
                        float x, float y, float width, float height,
                        BaseTransform patternTransform,
                        boolean proportional, boolean isMutable)
    {
        super(Type.IMAGE_PATTERN, proportional, isMutable);
        if (image == null) {
            throw new IllegalArgumentException("Image must be non-null");
        }
        this.image = image;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        if (patternTransform != null) {
            this.patternTransform = patternTransform.copy();
        } else {
            this.patternTransform = BaseTransform.IDENTITY_TRANSFORM;
        }
    }

    public ImagePattern(Image image,
                        float x, float y, float width, float height,
                        boolean proportional, boolean isMutable)
    {
        this(image, x, y, width, height, null, proportional, isMutable);
    }

    public Image getImage() {
        return image;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public BaseTransform getPatternTransformNoClone() {
        return patternTransform;
    }

    public boolean isOpaque() {
        return image.isOpaque();
    }
}
