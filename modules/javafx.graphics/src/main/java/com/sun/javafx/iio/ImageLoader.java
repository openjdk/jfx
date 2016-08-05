/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.iio;

import java.io.IOException;

/**
 * A loader for images stored in a given format.
 */
public interface ImageLoader {
    /**
     * Gets a description of the image format supported by this loader.
     *
     * @return a description of the image format handled by this loader.
     */
    ImageFormatDescription getFormatDescription();

//    String getInput();

//    void abort();
    /**
     * Disposes of any resources (such as native libraries) held by this loader.
     * Any further invocation of any methods on this object are undefined.
     */
    void dispose();

    /**
     * Adds a listener to monitor this loader.
     *
     * @param listener the listener to add.
     */
    void addListener(ImageLoadListener listener);

    /**
     * Removes a listener from the list of those monitoring this loader.
     * @param listener the listener to remove.
     */
    void removeListener(ImageLoadListener listener);

    /**
     * Loads the image at a given index in an image stream. If no image exists
     * at that index <code>null</code> will be returned.
     *
     * @param imageIndex the zero-relative index of the image to load.
     * @param width the desired image width. If non-positive,
     * an <code>IllegalArgumentException</code> will be thrown.
     * @param height the desired image height. If non-positive,
     * an <code>IllegalArgumentException</code> will be thrown.
     * @param preserveAspectRatio whether to preserve the width-to-height ratio
     * of the image.
     * @param smooth whether to use a smooth downscaling algorithm.
     * @return the image at the specified index or <code>null</code> on error.
     */
    public ImageFrame load(int imageIndex, int width, int height,
            boolean preserveAspectRatio, boolean smooth) throws IOException;
}
