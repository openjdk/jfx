/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.javafx.experiments.height2normal;

import javafx.geometry.Point3D;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

/**
 * Util class to convert height maps into normal maps
 */
public class Height2NormalConverter {
    public static Image convertToNormals(Image heightMap, boolean invert, double scale) {
        final int w = (int)heightMap.getWidth();
        final int h = (int)heightMap.getHeight();
        final byte[] heightPixels = new byte[w*h*4];
        final byte[] normalPixels = new byte[w*h*4];
        // get pixels
        final PixelReader reader = heightMap.getPixelReader();
        reader.getPixels(0, 0, w, h, PixelFormat.getByteBgraInstance(),heightPixels,0,w*4);
        if (invert) {
            for (int y=0; y<h; y++) {
                for (int x=0; x<w; x++) {
                    final int pixelIndex = (y*w*4) + (x*4);
                    heightPixels[pixelIndex] = (byte)(255-Byte.toUnsignedInt(heightPixels[pixelIndex]));
                    heightPixels[pixelIndex+1] = (byte)(255-Byte.toUnsignedInt(heightPixels[pixelIndex+1]));
                    heightPixels[pixelIndex+2] = (byte)(255-Byte.toUnsignedInt(heightPixels[pixelIndex+2]));
                    heightPixels[pixelIndex+3] = heightPixels[pixelIndex+3];
                }
            }
        }
        // generate normal map
        for (int y=0; y<h; y++) {
            for (int x=0; x<w; x++) {
                final int yAbove = Math.max(0,y-1);
                final int yBelow = Math.min(h - 1, y + 1);
                final int xLeft = Math.max(0, x - 1);
                final int xRight = Math.min(w - 1, x + 1);
                final int pixelIndex = (y*w*4) + (x*4);
                final int pixelAboveIndex = (yAbove*w*4) + (x*4);
                final int pixelBelowIndex = (yBelow*w*4) + (x*4);
                final int pixelLeftIndex = (y*w*4) + (xLeft*4);
                final int pixelRightIndex = (y*w*4) + (xRight*4);
                final int pixelAboveHeight = Byte.toUnsignedInt(heightPixels[pixelAboveIndex]);
                final int pixelBelowHeight = Byte.toUnsignedInt(heightPixels[pixelBelowIndex]);
                final int pixelLeftHeight = Byte.toUnsignedInt(heightPixels[pixelLeftIndex]);
                final int pixelRightHeight = Byte.toUnsignedInt(heightPixels[pixelRightIndex]);

                Point3D pixelAbove = new Point3D(x,yAbove,pixelAboveHeight);
                Point3D pixelBelow = new Point3D(x,yBelow,pixelBelowHeight);
                Point3D pixelLeft = new Point3D(xLeft,y,pixelLeftHeight);
                Point3D pixelRight = new Point3D(xRight,y,pixelRightHeight);
                Point3D H = pixelLeft.subtract(pixelRight);
                Point3D V = pixelAbove.subtract(pixelBelow);
                Point3D normal = H.crossProduct(V);
                // normalize normal
                normal = new Point3D(
                        normal.getX()/w,
                        normal.getY()/h,
                        normal.getZ()
                );
                // it seems there is lots of ways to calculate the Z element of normal map, 3 options here
//                normalPixels[pixelIndex] = (byte)((normal.getZ()*128)+128); // Option 1
                normalPixels[pixelIndex] = (byte)(255-(normal.getZ() * scale)); // Option 2
//                normalPixels[pixelIndex] = (byte)255; // Option 3
                normalPixels[pixelIndex+1] = (byte)((normal.getY()*128)+128);
                normalPixels[pixelIndex+2] = (byte)((normal.getX()*128)+128);
                normalPixels[pixelIndex+3] = (byte)255;
            }
        }
        // create output image
        final WritableImage outImage = new WritableImage(w,h);
        outImage.getPixelWriter().setPixels(0,0,w,h,PixelFormat.getByteBgraInstance(),normalPixels,0,w*4);
        return outImage;
    }
}
