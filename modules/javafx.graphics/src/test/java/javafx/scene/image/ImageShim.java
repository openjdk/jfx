/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;

public class ImageShim extends Image {

    public ImageShim(String url, boolean background) {
        super(url, background);
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    public InputStream shim_getInputSource() {
        return super.getInputSource();
    }

    @Override
    public void pixelsDirty() {
        super.pixelsDirty();
    }

    public void shim_setProgress(double value) {
        super.setProgress(value);
    }

    //-------------------------------------------

    public static void dispose(Image image) {
        image.dispose();
    }

    public static InputStream getInputSource(Image image) {
        return image.getInputSource();
    }

    public static void pixelsDirty(Image image) {
        image.pixelsDirty();
    }

    public static void setProgress(Image image, double value) {
        image.setProgress(value);
    }

}
