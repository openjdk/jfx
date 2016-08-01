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

package com.sun.javafx.font;

import com.sun.javafx.geom.transform.BaseTransform;


class PrismFont implements PGFont {

    private String name;
    private float fontSize;
    protected FontResource fontResource;
    private int features;

    PrismFont(FontResource fontResource, String name, float size) {
        this.fontResource = fontResource;
        this.name = name;
        this.fontSize = size;
    }

    public String getFullName() {
        return fontResource.getFullName();
    }

    public String getFamilyName() {
        return fontResource.getFamilyName();
    }

    public String getStyleName() {
        return fontResource.getStyleName();
    }

    /*
     * Returns the features the user has requested.
     * (kerning, ligatures, etc)
     */
    @Override public int getFeatures() {
        return features;
    }

    public String getName() {
        return name;
    }

    public float getSize() {
        return fontSize;
    }

    public FontStrike getStrike(BaseTransform transform) {
        return fontResource.getStrike(fontSize, transform);
    }

    public FontStrike getStrike(BaseTransform transform,
                                int smoothingType) {
        return fontResource.getStrike(fontSize, transform, smoothingType);
    }

    public FontResource getFontResource() {
        return fontResource;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PrismFont)) {
            return false;
        }
        final PrismFont other = (PrismFont) obj;

        // REMIND: When fonts can be rendered other than as greyscale
        // and generally differ in ways other than the point size
        // we need to update this method.
        return
            this.fontSize == other.fontSize &&
            this.fontResource.equals(other.fontResource);
    }

    private int hash;
    @Override
    public int hashCode() {
        if (hash != 0) {
            return hash;
        }
        else {
            hash = 497 + Float.floatToIntBits(fontSize);
            hash = 71 * hash + fontResource.hashCode();
            return hash;
        }
    }
}
