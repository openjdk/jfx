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
import java.io.InputStream;

/**
 * A factory which creates a loader for images stored in a given format.
 */
public interface ImageLoaderFactory {
    /**
     * Gets a description of the image format for which this factory can create
     * loaders.
     *
     * @return a description of the image format handled by this factory.
     */
    ImageFormatDescription getFormatDescription();

    /**
     * Creates a loader for the specified stream. This stream must contain a
     * sequence of bytes stored in the format handled by this factory, and must
     * be positioned at the first byte of the signature for this format.
     *
     * @param input a stream containing an image in the supported format.
     * @return a loader capable of decoding the supplied stream into an image.
     * @throws <IOException> if there is an error creating the loader.
     */
    ImageLoader createImageLoader(InputStream input) throws IOException;
}
