/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.webkit.prism;

import com.sun.prism.paint.ImagePattern;
import com.sun.webkit.graphics.WCImage;
import com.sun.webkit.graphics.WCRectangle;
import com.sun.webkit.graphics.WCTransform;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;

final class WCPattern {
    private final WCImage image;
    private final WCRectangle rect;
    private final WCTransform patternTransform;

    WCPattern(WCImage image, WCRectangle rect, WCTransform patternTransform) {
        this.image = image;
        this.rect = rect;
        this.patternTransform = patternTransform;
    }

    public ImagePattern getPlatformPattern() {
        double m[] = this.patternTransform.getMatrix();
        Affine3D at = new Affine3D(new Affine2D(m[0], m[1], m[2], m[3], m[4], m[5]));

        return new ImagePattern(
                ((PrismImage)this.image).getImage(),
                this.rect.getX(),
                this.rect.getY(),
                this.rect.getWidth(),
                this.rect.getHeight(),
                at, false, false);
    }
}
