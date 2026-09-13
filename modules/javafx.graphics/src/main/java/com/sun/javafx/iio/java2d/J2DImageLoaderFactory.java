/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.iio.java2d;

import com.sun.javafx.iio.ImageFormatDescription;
import com.sun.javafx.iio.ImageLoader;
import com.sun.javafx.iio.ImageLoaderFactory;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public class J2DImageLoaderFactory implements ImageLoaderFactory {

    private static J2DImageLoaderFactory theInstance;

    private J2DImageLoaderFactory() {}

    public static synchronized J2DImageLoaderFactory getInstance() {
        if (theInstance == null) {
            theInstance = new J2DImageLoaderFactory();
        }

        return theInstance;
    }

    @Override
    public ImageFormatDescription getFormatDescription() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImageLoader createImageLoader(InputStream input) throws IOException {
        boolean oldUseCache = ImageIO.getUseCache();
        ImageIO.setUseCache(false);

        try {
            ImageInputStream stream = ImageIO.createImageInputStream(input);
            if (stream != null) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
                ImageReader reader = readers.hasNext() ? readers.next() : null;
                if (reader == null) {
                    stream.close();
                    return null;
                }

                // J2DImageLoader is responsible for closing the ImageInputStream after
                // it has finished reading from it.
                return new J2DImageLoader(reader, stream);
            }

            return null;
        } finally {
            ImageIO.setUseCache(oldUseCache);
        }
    }

}
