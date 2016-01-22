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

package com.sun.scenario.effect.impl.prism;

import com.sun.prism.Image;
import com.sun.scenario.effect.Filterable;

/**
 * This is a special class that is only used for the purposes of converting
 * a Prism image (from Image.platformImage) into a Filterable (see
 * PrismToolkit.toFilterable()) that can then be passed to
 * PrRenderer.createImageData().  All of this is only used by the Identity
 * effect; eventually we should figure out a more straightforward solution.
 */
public class PrImage implements Filterable {

    private final Image image;

    private PrImage(Image image) {
        this.image = image;
    }

    public static PrImage create(Image image) {
        return new PrImage(image);
    }

    public Image getImage() {
        return image;
    }

    public Object getData() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getContentWidth() {
        return image.getWidth();
    }

    public int getContentHeight() {
        return image.getHeight();
    }

    public int getPhysicalWidth() {
        return image.getWidth();
    }

    public int getPhysicalHeight() {
        return image.getHeight();
    }

    public float getPixelScale() {
        return image.getPixelScale();
    }

    public int getMaxContentWidth() {
        return image.getWidth();
    }

    public int getMaxContentHeight() {
        return image.getHeight();
    }

    public void setContentWidth(int contentW) {
        throw new UnsupportedOperationException("Not supported.");
    }

    public void setContentHeight(int contentH) {
        throw new UnsupportedOperationException("Not supported");
    }

    public void lock() {
    }

    public void unlock() {
    }

    public boolean isLost() {
        return false;
    }

    public void flush() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
