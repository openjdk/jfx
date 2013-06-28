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

package com.sun.javafx.iio.common;

/**
 * Factory class for creating <code>PushbroomScaler</code> instances.
 */
public class ScalerFactory {
    private ScalerFactory() {}

    /**
     * Instantiates a new <code>PushbroomScaler</code> object.
     *
     * @param sourceWidth The source image width
     * @param sourceHeight The source image height
     * @param numBands The number of components per pixel in the images
     * @param destWidth The destination image width
     * @param destHeight The destination image height
     * @throws IllegalArgumentException if any of the integral parameters is non-positive.
     */
    public static PushbroomScaler createScaler(int sourceWidth, int sourceHeight, int numBands,
            int destWidth, int destHeight, boolean isSmooth) {
        if (sourceWidth <= 0 || sourceHeight <= 0 || numBands <= 0 ||
                destWidth <= 0 || destHeight <= 0) {
            throw new IllegalArgumentException();
        }

        PushbroomScaler scaler = null;

        boolean isMagnifying = destWidth > sourceWidth || destHeight > sourceHeight;

        if (isMagnifying) {
            if (isSmooth) {
                // RT-27408
                // TODO: bpb 2009-10-05 Need SmoothMagnifier class; use RoughScaler for now.
                scaler = new RoughScaler(sourceWidth, sourceHeight, numBands,
                        destWidth, destHeight);
            } else {
                scaler = new RoughScaler(sourceWidth, sourceHeight, numBands,
                        destWidth, destHeight);
            }
        } else { // minifying
            if (isSmooth) {
                scaler = new SmoothMinifier(sourceWidth, sourceHeight, numBands,
                        destWidth, destHeight);
            } else {
                scaler = new RoughScaler(sourceWidth, sourceHeight, numBands,
                        destWidth, destHeight);
            }
        }

        return scaler;
    }
}
