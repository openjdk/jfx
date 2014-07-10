/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package hello;

import java.io.*;

/**
 * The class creates an OutputStream in BMP format from
 * integer array, width and height of the image.
 */

public class BMPOutputStream extends FilterOutputStream{


    BMPOutputStream(OutputStream out, int [] arr, int width, int height) {
        super(out);
        try {
            int lineByteWidth = ((width * 3 + 3) >> 2) << 2;
            out.write(0x42);
            out.write(0x4d);
            writeBMPInt(out, lineByteWidth * height + 0x36);
            writeBMPInt(out, 0);
            writeBMPInt(out, 0x36);
            writeBMPInt(out, 0x28);
            writeBMPInt(out, width);
            writeBMPInt(out, height);
            writeBMPShort(out, 0x01);
            writeBMPShort(out, 0x18);
            writeBMPInt(out, 0);
            writeBMPInt(out, lineByteWidth * height);
            writeBMPInt(out, 0xb13);
            writeBMPInt(out, 0xb13);
            writeBMPInt(out, 0);
            writeBMPInt(out, 0);
            out.flush();

            int yIncrement = height;

            byte[] line = new byte[lineByteWidth];

            for (int i = yIncrement - 1; i >= 0; i--) {
                java.util.Arrays.fill(line,(byte)0);
                int pixelRowStart = i * width;
                int byteOffsetInLine = 0;
                for (int imgX = 0; imgX < width; imgX++) {
                    int rgb = arr[pixelRowStart + imgX];
                    line[byteOffsetInLine++] = (byte) (rgb & 0xff);
                    line[byteOffsetInLine++] = (byte) ((rgb >> 8) & 0xff);
                    line[byteOffsetInLine++] = (byte) ((rgb >> 16) & 0xff);
                }
                out.write(line);
            }
            out.flush();
            out.close();

        } catch (Exception e) {}
    }

    private static void writeBMPInt(OutputStream out, int i) throws IOException {
        out.write(i & 0xff);
        out.write((i >> 8) & 0xff);
        out.write((i >> 16) & 0xff);
        out.write(i >> 24);
    }

    private static void writeBMPShort(OutputStream out, int i) throws IOException {
        out.write(i & 0xff);
        out.write((i >> 8) & 0xff);
    }
}
