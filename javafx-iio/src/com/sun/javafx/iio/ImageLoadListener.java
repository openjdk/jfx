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

/**
 * An interface defining methods which may be used to monitor image loading.
 */
public interface ImageLoadListener {
    /**
     * Receives updates of progress in image loading.
     *
     * @param loader the <code>ImageLoader</code> used to load the image.
     * @param percentageComplete the percentage <code>[0.0,100.0]</code>
     * of image data loaded.
     */
    void imageLoadProgress(ImageLoader loader, float percentageComplete);

    /**
     * Invoked if a warning occurs.
     *
     * @param loader the <code>ImageLoader</code> used to load the image.
     * @param message a message indicating the nature of the error.
     */
    public void imageLoadWarning(ImageLoader loader, String message);

    /**
     * Invoked when the meta data of the loading image is ready.
     *
     * @param loader the <code>ImageLoader</code> used to load the image.
     * @param metadata of the image.
     */
    public void imageLoadMetaData(ImageLoader loader, ImageMetadata metadata);

}
