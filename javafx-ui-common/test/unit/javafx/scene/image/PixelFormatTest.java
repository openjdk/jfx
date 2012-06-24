/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import javafx.scene.paint.Color;
import org.junit.Test;

public final class PixelFormatTest {
    static Color derive(Color c, double opacity) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), c.getOpacity() * opacity);
    }

    static Color testColors[] = {
        Color.WHITE,
        Color.BLACK,
        Color.RED,
        Color.GREEN,
        Color.BLUE,
        Color.TRANSPARENT,
        derive(Color.WHITE, 0.5),
        derive(Color.BLACK, 0.5),
        derive(Color.RED, 0.5),
        derive(Color.GREEN, 0.5),
        derive(Color.BLUE, 0.5),
    };

    static void checkArgb(int argb1, int argb2, double delta) {
        assertEquals("alpha", (argb1 >> 24) & 0xff, (argb2 >> 24) & 0xff);
        assertEquals("red",   (argb1 >> 16) & 0xff, (argb2 >> 16) & 0xff, delta);
        assertEquals("green", (argb1 >>  8) & 0xff, (argb2 >>  8) & 0xff, delta);
        assertEquals("blue",  (argb1      ) & 0xff, (argb2      ) & 0xff, delta);
    }

    void test(PixelFormat<ByteBuffer> pfb, ByteBuffer bbuf, Color c,
              int aoff, int roff, int goff, int boff)
    {
        int alpha = (int) (c.getOpacity() * 255f);
        int red   = (int) (c.getRed() * 255f);
        int green = (int) (c.getGreen() * 255f);
        int blue  = (int) (c.getBlue() * 255f);
        int argb = (alpha << 24) | (red << 16) | (green << 8) | blue;
        if (aoff < 0) {
            if (alpha < 255) return;
            bbuf.put(roff, (byte) red);
            bbuf.put(goff, (byte) green);
            bbuf.put(boff, (byte) blue);
        } else {
            assertTrue(pfb.isWritable());
            ((WritablePixelFormat<ByteBuffer>) pfb).setArgb(bbuf, 0, 0, 0, argb);
            assertEquals(alpha, bbuf.get(aoff) & 0xff);
            if (pfb.isPremultiplied() && alpha < 255) {
                red   = (red   * alpha + 127) / 255;
                green = (green * alpha + 127) / 255;
                blue  = (blue  * alpha + 127) / 255;
            }
        }
        if (pfb.isPremultiplied()) {
            assertEquals(red,   bbuf.get(roff) & 0xff, 1.0);
            assertEquals(green, bbuf.get(goff) & 0xff, 1.0);
            assertEquals(blue,  bbuf.get(boff) & 0xff, 1.0);
        } else {
            assertEquals(red,   bbuf.get(roff) & 0xff);
            assertEquals(green, bbuf.get(goff) & 0xff);
            assertEquals(blue,  bbuf.get(boff) & 0xff);
        }
        int argbtest = pfb.getArgb(bbuf, 0, 0, 0);
        if (pfb.isPremultiplied() && alpha < 255) {
            if (alpha > 0) {
                int halfa = (alpha >> 1);
                red   = (red   * 255 + halfa) / alpha;
                green = (green * 255 + halfa) / alpha;
                blue  = (blue  * 255 + halfa) / alpha;
                argb = (alpha << 24) | (red << 16) | (green << 8) | blue;
            } else {
                argb = 0;
            }
        }
        checkArgb(argb, argbtest, pfb.isPremultiplied() ? 1.0 : 0.0);
    }

    void test(WritablePixelFormat<IntBuffer> wpfi, IntBuffer ibuf, Color c,
              int ashift, int rshift, int gshift, int bshift)
    {
        int alpha = (int) (c.getOpacity() * 255f);
        int red   = (int) (c.getRed() * 255f);
        int green = (int) (c.getGreen() * 255f);
        int blue  = (int) (c.getBlue() * 255f);
        int argb = (alpha << 24) | (red << 16) | (green << 8) | blue;
        wpfi.setArgb(ibuf, 0, 0, 0, argb);
        if (wpfi.isPremultiplied() && alpha < 255) {
            red   = (red   * alpha + 127) / 255;
            green = (green * alpha + 127) / 255;
            blue  = (blue  * alpha + 127) / 255;
        }
        assertEquals(alpha, (ibuf.get(0) >> ashift) & 0xff);
        assertEquals(red,   (ibuf.get(0) >> rshift) & 0xff);
        assertEquals(green, (ibuf.get(0) >> gshift) & 0xff);
        assertEquals(blue,  (ibuf.get(0) >> bshift) & 0xff);
        int argbtest = wpfi.getArgb(ibuf, 0, 0, 0);
        if (wpfi.isPremultiplied() && alpha < 255) {
            if (alpha > 0) {
                int halfa = (alpha >> 1);
                red   = (red   * 255 + halfa) / alpha;
                green = (green * 255 + halfa) / alpha;
                blue  = (blue  * 255 + halfa) / alpha;
                argb = (alpha << 24) | (red << 16) | (green << 8) | blue;
            } else {
                argb = 0;
            }
        }
        checkArgb(argb, argbtest, wpfi.isPremultiplied() ? 1.0 : 0.0);
    }

    void testColors(PixelFormat<ByteBuffer> wpfb, ByteBuffer bbuf,
                    int aoff, int roff, int goff, int boff)
    {
        for (Color c : testColors) {
            test(wpfb, bbuf, c, aoff, roff, goff, boff);
        }
    }

    void testColors(WritablePixelFormat<IntBuffer> wpfi, IntBuffer ibuf,
                    int ashift, int rshift, int gshift, int bshift)
    {
        for (Color c : testColors) {
            test(wpfi, ibuf, c, ashift, rshift, gshift, bshift);
        }
    }

    @Test
    public void testByteBgraGetSetArgb() {
        WritablePixelFormat<ByteBuffer> byteBgra = PixelFormat.getByteBgraInstance();
        ByteBuffer bbuf = ByteBuffer.allocate(4);
        testColors(byteBgra, bbuf, 3, 2, 1, 0);
    }

    @Test
    public void testByteBgraPreGetSetArgb() {
        WritablePixelFormat<ByteBuffer> byteBgraPre = PixelFormat.getByteBgraPreInstance();
        ByteBuffer bbuf = ByteBuffer.allocate(4);
        testColors(byteBgraPre, bbuf, 3, 2, 1, 0);
    }

    @Test
    public void testByteRgbGetArgb() {
        PixelFormat<ByteBuffer> byteRgb = PixelFormat.getByteRgbInstance();
        ByteBuffer bbuf = ByteBuffer.allocate(4);
        testColors(byteRgb, bbuf, -1, 0, 1, 2);
    }

    @Test
    public void testIntArgbGetSetArgb() {
        WritablePixelFormat<IntBuffer> intArgb = PixelFormat.getIntArgbInstance();
        IntBuffer ibuf = IntBuffer.allocate(1);
        testColors(intArgb, ibuf, 24, 16, 8, 0);
    }

    @Test
    public void testIntArgbPreGetSetArgbPre() {
        WritablePixelFormat<IntBuffer> intArgbPre = PixelFormat.getIntArgbPreInstance();
        IntBuffer ibuf = IntBuffer.allocate(1);
        testColors(intArgbPre, ibuf, 24, 16, 8, 0);
    }

    int[] getPalette(boolean premultiplied) {
        int numcolors = Math.min(testColors.length, 256);
        int colors[] = new int[numcolors];
        for (int i = 0; i < numcolors; i++) {
            Color c = testColors[i];
            int alpha = (int) (c.getOpacity() * 255f);
            int red   = (int) (c.getRed() * 255f);
            int green = (int) (c.getGreen() * 255f);
            int blue  = (int) (c.getBlue() * 255f);
            if (premultiplied && alpha < 255) {
                red   = (red   * alpha + 127) / 255;
                green = (green * alpha + 127) / 255;
                blue  = (blue  * alpha + 127) / 255;
            }
            colors[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }
        return colors;
    }

    @Test
    public void testByteIndexedGetArgb() {
        int palette[] = getPalette(false);
        PixelFormat<ByteBuffer> byteIndexed = PixelFormat.createByteIndexedInstance(palette);
        ByteBuffer bbuf = ByteBuffer.allocate(1);
        for (int i = 0; i < 256; i++) {
            bbuf.put(0, (byte) i);
            int argb = byteIndexed.getArgb(bbuf, 0, 0, 0);
            checkArgb((i < palette.length) ? palette[i] : 0, argb, 0.0);
        }
    }

    @Test
    public void testByteIndexedPreGetArgb() {
        int palette[] = getPalette(false);
        int palettepre[] = getPalette(true);
        PixelFormat<ByteBuffer> byteIndexed = PixelFormat.createByteIndexedPremultipliedInstance(palettepre);
        ByteBuffer bbuf = ByteBuffer.allocate(1);
        for (int i = 0; i < 256; i++) {
            bbuf.put(0, (byte) i);
            int argb = byteIndexed.getArgb(bbuf, 0, 0, 0);
            checkArgb((i < palette.length) ? palette[i] : 0, argb, 1.0);
        }
    }
}
