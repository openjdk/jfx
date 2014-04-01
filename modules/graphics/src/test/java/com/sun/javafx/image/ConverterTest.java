/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.image;

import com.sun.javafx.image.impl.ByteArgb;
import com.sun.javafx.image.impl.ByteBgr;
import com.sun.javafx.image.impl.ByteBgra;
import com.sun.javafx.image.impl.ByteBgraPre;
import com.sun.javafx.image.impl.ByteGray;
import com.sun.javafx.image.impl.ByteGrayAlpha;
import com.sun.javafx.image.impl.ByteGrayAlphaPre;
import com.sun.javafx.image.impl.ByteRgb;
import com.sun.javafx.image.impl.ByteRgba;
import com.sun.javafx.image.impl.IntArgb;
import com.sun.javafx.image.impl.IntArgbPre;
import static junit.framework.Assert.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.paint.Color;
import org.junit.Test;

/**
 */
public class ConverterTest {
    static ByteBuffer heapByteBuffer(int off, int len) {
        ByteBuffer bbuf = ByteBuffer.allocate(off + len);
        if (off > 0) {
            bbuf.position(off);
            bbuf = bbuf.slice();
        }
        return bbuf;
    }

    static ByteBuffer directByteBuffer(int off, int len) {
        ByteBuffer bbuf = ByteBuffer.allocateDirect(off + len);
        if (off > 0) {
            bbuf.position(off);
            bbuf = bbuf.slice();
        }
        return bbuf;
    }

    static IntBuffer heapIntBuffer(int off, int len) {
        IntBuffer ibuf = IntBuffer.allocate(off + len);
        if (off > 0) {
            ibuf.position(off);
            ibuf = ibuf.slice();
        }
        return ibuf;
    }

    static IntBuffer directIntBuffer(int off, int len) {
        IntBuffer ibuf = ByteBuffer.allocateDirect((off + len) * 4).asIntBuffer();
        if (off > 0) {
            ibuf.position(off);
            ibuf = ibuf.slice();
        }
        return ibuf;
    }

    static Color derive(Color c, double opacity) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), c.getOpacity() * opacity);
    }

    static int RgbToGray(int red, int green, int blue) {
        return (int) (red * .3 + green * .59 + blue * .11);
    }

    static int grayify(int argb) {
        int alpha = (argb >> 24) & 0xff;
        int red   = (argb >> 16) & 0xff;
        int green = (argb >>  8) & 0xff;
        int blue  = (argb      ) & 0xff;
        int gray  = RgbToGray(red, green, blue);
        return (alpha << 24) | (gray << 16) | (gray << 8) | gray;
    }

    static int getArgb(Color c) {
        int alpha = (int) (c.getOpacity() * 255f);
        int red   = (int) (c.getRed()     * 255f);
        int green = (int) (c.getGreen()   * 255f);
        int blue  = (int) (c.getBlue()    * 255f);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    static int getArgbPre(Color c) {
        double a = c.getOpacity();
        int alpha = (int) (a * 255f);
        int red   = (int) (a * c.getRed()   * 255f);
        int green = (int) (a * c.getGreen() * 255f);
        int blue  = (int) (a * c.getBlue()  * 255f);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    static class ByteFormat {
        private final BytePixelGetter getter;
        private final BytePixelSetter setter;
        private final int aoff, roff, goff, boff, grayoff;
        private final int ncomp;

        public ByteFormat(BytePixelGetter getter,
                          int aoff, int roff, int goff, int boff)
        {
            if (getter == null) throw new NullPointerException("getter must not be null");

            this.getter = getter;
            this.setter = null;
            this.grayoff = -1;
            this.aoff = aoff;
            this.roff = roff;
            this.goff = goff;
            this.boff = boff;
            this.ncomp = (aoff < 0) ? 3 : 4;
        }

        public ByteFormat(BytePixelGetter getter, BytePixelSetter setter,
                          int aoff, int roff, int goff, int boff)
        {
            if (getter == null) throw new NullPointerException("getter must not be null");
            if (setter == null) throw new NullPointerException("setter must not be null");

            this.getter = getter;
            this.setter = setter;
            this.grayoff = -1;
            this.aoff = aoff;
            this.roff = roff;
            this.goff = goff;
            this.boff = boff;
            this.ncomp = (aoff < 0) ? 3 : 4;
        }

        public ByteFormat(BytePixelGetter getter,
                          int aoff, int grayoff)
        {
            if (getter == null) throw new NullPointerException("getter must not be null");

            this.getter = getter;
            this.setter = null;
            this.grayoff = grayoff;
            this.aoff = aoff;
            this.roff = -1;
            this.goff = -1;
            this.boff = -1;
            this.ncomp = (aoff < 0) ? 1 : 2;
        }

        public ByteFormat(BytePixelGetter getter, BytePixelSetter setter,
                          int aoff, int grayoff)
        {
            if (getter == null) throw new NullPointerException("getter must not be null");
            if (setter == null) throw new NullPointerException("setter must not be null");

            this.getter = getter;
            this.setter = setter;
            this.grayoff = grayoff;
            this.aoff = aoff;
            this.roff = -1;
            this.goff = -1;
            this.boff = -1;
            this.ncomp = (aoff < 0) ? 1 : 2;
        }

        public int getNcomp() {
            return ncomp;
        }

        public int getArgb(byte barr[], int off) {
            int alpha = (aoff < 0) ? 255 : (barr[off + aoff] & 0xff);
            int red, green, blue;
            if (isGray()) {
                red = green = blue = barr[off + grayoff] & 0xff;
            } else {
                red   = barr[off + roff] & 0xff;
                green = barr[off + goff] & 0xff;
                blue  = barr[off + boff] & 0xff;
            }
            if (alpha < 255 && alpha > 0 && getter.getAlphaType() == AlphaType.PREMULTIPLIED) {
                int halfa = alpha >> 1;
                red   = (red   >= alpha) ? 255 : (red   * 255 + halfa) / alpha;
                green = (green >= alpha) ? 255 : (green * 255 + halfa) / alpha;
                blue  = (blue  >= alpha) ? 255 : (blue  * 255 + halfa) / alpha;
            }
            return (alpha << 24) | (red << 16) | (green << 8) | blue;
        }

        public int getArgb(ByteBuffer bbuf, int off) {
            int alpha = (aoff < 0) ? 255 : (bbuf.get(off + aoff) & 0xff);
            int red, green, blue;
            if (isGray()) {
                red = green = blue = bbuf.get(off + grayoff) & 0xff;
            } else {
                red   = bbuf.get(off + roff) & 0xff;
                green = bbuf.get(off + goff) & 0xff;
                blue  = bbuf.get(off + boff) & 0xff;
            }
            if (alpha < 255 && alpha > 0 && getter.getAlphaType() == AlphaType.PREMULTIPLIED) {
                int halfa = alpha >> 1;
                red   = (red   >= alpha) ? 255 : (red   * 255 + halfa) / alpha;
                green = (green >= alpha) ? 255 : (green * 255 + halfa) / alpha;
                blue  = (blue  >= alpha) ? 255 : (blue  * 255 + halfa) / alpha;
            }
            return (alpha << 24) | (red << 16) | (green << 8) | blue;
        }

        public void setArgb(byte barr[], int off, int argb) {
            int alpha = (argb >> 24) & 0xff;
            int red   = (argb >> 16) & 0xff;
            int green = (argb >>  8) & 0xff;
            int blue  = (argb      ) & 0xff;
            if (getter.getAlphaType() == AlphaType.PREMULTIPLIED) {
                red   = (red   * alpha + 127) / 255;
                green = (green * alpha + 127) / 255;
                blue  = (blue  * alpha + 127) / 255;
            }
            if (aoff >= 0) {
                barr[off + aoff] = (byte) alpha;
            }
            if (isGray()) {
                int gray = RgbToGray(red, green, blue);
                barr[off + grayoff] = (byte) gray;
            } else {
                barr[off + roff] = (byte) red;
                barr[off + goff] = (byte) green;
                barr[off + boff] = (byte) blue;
            }
        }

        public void setArgb(ByteBuffer bbuf, int off, int argb) {
            int alpha = (argb >> 24) & 0xff;
            int red   = (argb >> 16) & 0xff;
            int green = (argb >>  8) & 0xff;
            int blue  = (argb      ) & 0xff;
            if (getter.getAlphaType() == AlphaType.PREMULTIPLIED) {
                red   = (red   * alpha + 127) / 255;
                green = (green * alpha + 127) / 255;
                blue  = (blue  * alpha + 127) / 255;
            }
            if (aoff >= 0) {
                bbuf.put(off + aoff, (byte) alpha);
            }
            if (isGray()) {
                int gray = RgbToGray(red, green, blue);
                bbuf.put(off + grayoff, (byte) gray);
            } else {
                bbuf.put(off + roff, (byte) red);
                bbuf.put(off + goff, (byte) green);
                bbuf.put(off + boff, (byte) blue);
            }
        }

        public BytePixelGetter getGetter() {
            return getter;
        }

        public BytePixelSetter getSetter() {
            return setter;
        }

        public boolean isGray() {
            return grayoff >= 0;
        }

        public int getGrayOff() {
            return grayoff;
        }

        public int getAoff() {
            return aoff;
        }

        public int getRoff() {
            return roff;
        }

        public int getGoff() {
            return goff;
        }

        public int getBoff() {
            return boff;
        }

        @Override
        public String toString() {
            if (getter == null) {
                return "ByteFormat{" + "setter=" + setter + '}';
            } else if (setter == null) {
                return "ByteFormat{" + "getter=" + getter + '}';
            } else if (getter == setter) {
                return "ByteFormat{" + "accessor=" + getter + '}';
            } else {
                return "ByteFormat{" + "getter=" + getter + ", setter=" + setter + '}';
            }
        }
    }

    static class IntFormat {
        private final IntPixelGetter getter;
        private final IntPixelSetter setter;
        private final int ashift, rshift, gshift, bshift;

        public IntFormat(IntPixelGetter getter, IntPixelSetter setter,
                         int ashift, int rshift, int gshift, int bshift)
        {
            if (getter == null) throw new NullPointerException("getter must not be null");
            if (setter == null) throw new NullPointerException("setter must not be null");

            this.getter = getter;
            this.setter = setter;
            this.ashift = ashift;
            this.rshift = rshift;
            this.gshift = gshift;
            this.bshift = bshift;
        }

        private int convertPixel(int pixel) {
            int alpha = (pixel >> ashift) & 0xff;
            int red   = (pixel >> rshift) & 0xff;
            int green = (pixel >> gshift) & 0xff;
            int blue  = (pixel >> bshift) & 0xff;
            if (alpha > 0 && alpha < 255 && getter.getAlphaType() == AlphaType.PREMULTIPLIED) {
                int halfa = alpha >> 1;
                red   = (red   >= alpha) ? 255 : (red   * 255 + halfa) / alpha;
                green = (green >= alpha) ? 255 : (green * 255 + halfa) / alpha;
                blue  = (blue  >= alpha) ? 255 : (blue  * 255 + halfa) / alpha;
            }
            return (alpha << 24) | (red << 16) | (green << 8) | blue;
        }

        private int convertArgb(int argb) {
            int alpha = (argb >> 24) & 0xff;
            int red   = (argb >> 16) & 0xff;
            int green = (argb >>  8) & 0xff;
            int blue  = (argb      ) & 0xff;
            if (alpha < 255 && getter.getAlphaType() == AlphaType.PREMULTIPLIED) {
                red   = (red   * alpha + 127) / 255;
                green = (green * alpha + 127) / 255;
                blue  = (blue  * alpha + 127) / 255;
            }
            return (alpha << ashift) | (red << rshift) | (green << gshift) | (blue << bshift);
        }

        public int getArgb(int iarr[], int off) {
            return convertPixel(iarr[off]);
        }

        public int getArgb(IntBuffer ibuf, int off) {
            return convertPixel(ibuf.get(off));
        }

        public void setArgb(int iarr[], int off, int argb) {
            iarr[off] = convertArgb(argb);
        }

        public void setArgb(IntBuffer ibuf, int off, int argb) {
            ibuf.put(off, convertArgb(argb));
        }

        public IntPixelGetter getGetter() {
            return getter;
        }

        public IntPixelSetter getSetter() {
            return setter;
        }

        public int getAshift() {
            return ashift;
        }

        public int getRshift() {
            return rshift;
        }

        public int getGshift() {
            return gshift;
        }

        public int getBshift() {
            return bshift;
        }

        @Override
        public String toString() {
            if (getter == null) {
                return "IntFormat{" + "setter=" + setter + '}';
            } else if (setter == null) {
                return "IntFormat{" + "getter=" + getter + '}';
            } else if (getter == setter) {
                return "IntFormat{" + "accessor=" + getter + '}';
            } else {
                return "IntFormat{" + "getter=" + getter + ", setter=" + setter + '}';
            }
        }
    }

    
    static ByteFormat ByteFormats[] = {
        new ByteFormat(ByteArgb.getter,    ByteArgb.setter,     0, 1, 2, 3),
        new ByteFormat(ByteBgra.getter,    ByteBgra.setter,     3, 2, 1, 0),
        new ByteFormat(ByteBgraPre.getter, ByteBgraPre.setter,  3, 2, 1, 0),
        new ByteFormat(ByteRgba.getter,    ByteRgba.setter,     3, 0, 1, 2),
        new ByteFormat(ByteRgb.getter,                         -1, 0, 1, 2),
        new ByteFormat(ByteBgr.getter,     ByteBgr.setter,     -1, 2, 1, 0),

        new ByteFormat(ByteGray.getter,         ByteGray.setter,        -1, 0),
        new ByteFormat(ByteGrayAlpha.getter,    ByteGrayAlpha.setter,    1, 0),
        new ByteFormat(ByteGrayAlphaPre.getter, ByteGrayAlphaPre.setter, 1, 0),
    };

    static IntFormat IntFormats[] = {
        new IntFormat(IntArgb.getter,    IntArgb.setter,    24, 16, 8, 0),
        new IntFormat(IntArgbPre.getter, IntArgbPre.setter, 24, 16, 8, 0),
    };

    static Color TestColors[] = {
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

    static Color OpaqueTestColors[] = {
        Color.WHITE,
        Color.BLACK,
        Color.RED,
        Color.GREEN,
        Color.BLUE,
        Color.CYAN,
        Color.MAGENTA,
        Color.YELLOW,
    };

    static void checkArgb(int argb1, int argb2, double delta) {
        assertEquals("alpha", (argb1 >> 24) & 0xff, (argb2 >> 24) & 0xff);
        assertEquals("red",   (argb1 >> 16) & 0xff, (argb2 >> 16) & 0xff, delta);
        assertEquals("green", (argb1 >>  8) & 0xff, (argb2 >>  8) & 0xff, delta);
        assertEquals("blue",  (argb1      ) & 0xff, (argb2      ) & 0xff, delta);
    }

    void testget(ByteFormat bfmt, ByteBuffer bbuf, byte barr[], Color c) {
        int refnon = getArgb(c);
        int refpre = getArgbPre(c);
        bfmt.setArgb(bbuf, 0, refnon);
        bfmt.setArgb(barr, 0, refnon);
        BytePixelGetter bpg = bfmt.getGetter();
        boolean premult = (bpg.getAlphaType() == AlphaType.PREMULTIPLIED);
        double delta = 0.0;
        if (bfmt.isGray()) {
            refnon = grayify(refnon);
            refpre = grayify(refpre);
            delta += 1.0;
        }
        checkArgb(refnon, bpg.getArgb   (bbuf, 0), delta + (premult ? 1.0 : 0.0));
        checkArgb(refpre, bpg.getArgbPre(bbuf, 0), delta + (premult ? 0.0 : 1.0));
        checkArgb(refnon, bpg.getArgb   (barr, 0), delta + (premult ? 1.0 : 0.0));
        checkArgb(refpre, bpg.getArgbPre(barr, 0), delta + (premult ? 0.0 : 1.0));
    }

    void testget(IntFormat ifmt, IntBuffer ibuf, int iarr[], Color c) {
        int refnon = getArgb(c);
        int refpre = getArgbPre(c);
        ifmt.setArgb(ibuf, 0, refnon);
        ifmt.setArgb(iarr, 0, refnon);
        IntPixelGetter bpg = ifmt.getGetter();
        boolean premult = (bpg.getAlphaType() == AlphaType.PREMULTIPLIED);
        checkArgb(refnon, bpg.getArgb   (ibuf, 0), premult ? 1.0 : 0.0);
        checkArgb(refpre, bpg.getArgbPre(ibuf, 0), premult ? 0.0 : 1.0);
        checkArgb(refnon, bpg.getArgb   (iarr, 0), premult ? 1.0 : 0.0);
        checkArgb(refpre, bpg.getArgbPre(iarr, 0), premult ? 0.0 : 1.0);
    }

    void testset(ByteFormat bfmt, ByteBuffer bbuf, byte barr[], Color c) {
        int refnon = getArgb(c);
        int refpre = getArgbPre(c);
        BytePixelSetter bps = bfmt.getSetter();
        bps.setArgb   (bbuf, 0, refnon);
        bps.setArgbPre(bbuf, 4, refpre);
        bps.setArgb   (barr, 0, refnon);
        bps.setArgbPre(barr, 4, refpre);
        boolean premult = (bps.getAlphaType() == AlphaType.PREMULTIPLIED);
        double delta = 0.0;
        if (bfmt.isGray()) {
            refnon = grayify(refnon);
            delta += 1.0;
        }
        checkArgb(refnon, bfmt.getArgb(bbuf, 0), delta + (premult ? 1.0 : 0.0));
        checkArgb(refnon, bfmt.getArgb(bbuf, 4), delta + (premult ? 1.0 : 1.0));
        checkArgb(refnon, bfmt.getArgb(barr, 0), delta + (premult ? 1.0 : 0.0));
        checkArgb(refnon, bfmt.getArgb(barr, 4), delta + (premult ? 1.0 : 1.0));
    }

    void testset(IntFormat ifmt, IntBuffer ibuf, int iarr[], Color c) {
        int refnon = getArgb(c);
        int refpre = getArgbPre(c);
        IntPixelSetter ips = ifmt.getSetter();
        ips.setArgb   (ibuf, 0, refnon);
        ips.setArgbPre(ibuf, 1, refpre);
        ips.setArgb   (iarr, 0, refnon);
        ips.setArgbPre(iarr, 1, refpre);
        boolean premult = (ips.getAlphaType() == AlphaType.PREMULTIPLIED);
        double delta = 0.0;
        checkArgb(refnon, ifmt.getArgb(ibuf, 0), delta + (premult ? 1.0 : 0.0));
        checkArgb(refnon, ifmt.getArgb(ibuf, 1), delta + (premult ? 1.0 : 1.0));
        checkArgb(refnon, ifmt.getArgb(iarr, 0), delta + (premult ? 1.0 : 0.0));
        checkArgb(refnon, ifmt.getArgb(iarr, 1), delta + (premult ? 1.0 : 1.0));
    }

    @Test
    public void testByteAccessors() {
        testByteAccessors(heapByteBuffer(0, 8));
        testByteAccessors(heapByteBuffer(1, 8));
        testByteAccessors(directByteBuffer(0, 8));
        testByteAccessors(directByteBuffer(1, 8));
    }

    private void testByteAccessors(ByteBuffer bbuf) {
        byte barr[] = new byte[8];
        for (ByteFormat bfmt : ByteFormats) {
            BytePixelGetter getter = bfmt.getGetter();
            BytePixelSetter setter = bfmt.getSetter();
            if (getter != null && setter != null) {
                assertEquals(getter.getAlphaType(), setter.getAlphaType());
            }
            Color testColors[] = (getter.getAlphaType() == AlphaType.OPAQUE
                                  ? OpaqueTestColors : TestColors);
            for (Color c : testColors) {
                if (getter != null) {
                    testget(bfmt, bbuf, barr, c);
                }
                if (setter != null) {
                    testset(bfmt, bbuf, barr, c);
                }
            }
        }
    }

    static final int FxColors[] = {
        0x00000000,
        0xffff0000,
        0xff00ff00,
        0xff0000ff,
        0xffffffff
    };

    static final PixelFormat FxFormats[] = {
        PixelFormat.getByteBgraInstance(),
        PixelFormat.getByteBgraPreInstance(),
        PixelFormat.getByteRgbInstance(),
        PixelFormat.getIntArgbInstance(),
        PixelFormat.getIntArgbPreInstance(),
        PixelFormat.createByteIndexedInstance(FxColors),
        PixelFormat.createByteIndexedPremultipliedInstance(FxColors)
    };

    static final WritablePixelFormat FxWritableFormats[] = {
        WritablePixelFormat.getByteBgraInstance(),
        WritablePixelFormat.getByteBgraPreInstance(),
        WritablePixelFormat.getIntArgbInstance(),
        WritablePixelFormat.getIntArgbPreInstance()
    };

    static void checkAllTypesTested(PixelFormat<?> formats[], boolean writable) {
        if (writable) {
            for (PixelFormat<?> pf : formats) {
                assertTrue(pf.isWritable());
            }
        }
        for (PixelFormat.Type type : PixelFormat.Type.values()) {
            if (type == PixelFormat.Type.BYTE_INDEXED ||
                type == PixelFormat.Type.BYTE_RGB)
            {
                // Non-writable type
                if (writable) continue;
            }
            boolean found = false;
            for (PixelFormat<?> pf : formats) {
                if (pf.getType() == type) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
    }

    @Test
    public void ensureFXConverters() {
        checkAllTypesTested(FxFormats, false);
        checkAllTypesTested(FxWritableFormats, true);
        for (PixelFormat<?> pf : FxFormats) {
            PixelGetter<?> getter = PixelUtils.getGetter(pf);
            assertNotNull(getter);
            for (WritablePixelFormat<?> wpf : FxWritableFormats) {
                PixelSetter<?> setter = PixelUtils.getSetter(wpf);
                assertNotNull(setter);
                PixelConverter<?, ?> converter = PixelUtils.getConverter(getter, setter);
                assertNotNull(converter);
            }
        }
    }

    @Test
    public void ensureJ2DConverters() {
        assertNotNull(PixelUtils.getConverter(ByteGray.getter, ByteGray.setter));
        assertNotNull(PixelUtils.getConverter(ByteBgr.getter, ByteBgr.setter));
        assertNotNull(PixelUtils.getConverter(IntArgbPre.getter, IntArgbPre.setter));
        assertNotNull(PixelUtils.getConverter(ByteBgraPre.getter, IntArgbPre.setter));
    }

    @Test
    public void testIntAccessors() {
        testIntAccessors(heapIntBuffer(0, 2));
        testIntAccessors(heapIntBuffer(1, 2));
        testIntAccessors(directIntBuffer(0, 2));
        testIntAccessors(directIntBuffer(1, 2));
    }
    
    private void testIntAccessors(IntBuffer ibuf) {
        int iarr[] = new int[2];
        for (IntFormat ifmt : IntFormats) {
            IntPixelGetter getter = ifmt.getGetter();
            IntPixelSetter setter = ifmt.getSetter();
            if (getter != null && setter != null) {
                assertEquals(getter.getAlphaType(), setter.getAlphaType());
            }
            Color testColors[] = (getter.getAlphaType() == AlphaType.OPAQUE
                                  ? OpaqueTestColors : TestColors);
            for (Color c : testColors) {
                if (getter != null) {
                    testget(ifmt, ibuf, iarr, c);
                }
                if (setter != null) {
                    testset(ifmt, ibuf, iarr, c);
                }
            }
        }
    }

    static boolean isGeneral(PixelConverter pc) {
        Class enclosing = pc.getClass().getEnclosingClass();
        return (enclosing != null &&
                enclosing.getName().equals("com.sun.javafx.image.impl.General"));
    }

    static void clear(ByteBuffer hbuf, ByteBuffer dbuf, byte arr[]) {
        assertEquals(hbuf.capacity(), dbuf.capacity());
        assertEquals(hbuf.capacity(), arr.length);
        byte bv = (byte) (Math.random() * 255);
        for (int i = 0; i < arr.length; i++) {
            hbuf.put(i, (byte) bv);
            dbuf.put(i, (byte) bv);
            arr[i] = bv;
        }
    }

    static void clear(IntBuffer hbuf, IntBuffer dbuf, int arr[]) {
        assertEquals(hbuf.capacity(), dbuf.capacity());
        assertEquals(hbuf.capacity(), arr.length);
        int iv = (int) (Math.random() * 0xffffffff);
        for (int i = 0; i < arr.length; i++) {
            hbuf.put(i, iv);
            dbuf.put(i, iv);
            arr[i] = iv;
        }
    }

    @Test
    public void testB2BConverterTypes() {
        testB2BConverterTypes(0);
        testB2BConverterTypes(1);
    }
    
    private void testB2BConverterTypes(int off) {
        ByteBuffer srchbuf = heapByteBuffer(off, 4 * TestColors.length);
        ByteBuffer srcdbuf = directByteBuffer(off, 4 * TestColors.length);
        byte srcarr[] = new byte[4 * TestColors.length];
        ByteBuffer dsthbuf = heapByteBuffer(off, 4 * TestColors.length);
        ByteBuffer dstdbuf = directByteBuffer(off, 4 * TestColors.length);
        byte dstarr[] = new byte[4 * TestColors.length];
        for (ByteFormat bfmtgetter : ByteFormats) {
            BytePixelGetter bpg = bfmtgetter.getGetter();
            for (ByteFormat bfmtsetter : ByteFormats) {
                BytePixelSetter bps = bfmtsetter.getSetter();
                if (bps == null) continue;
                ByteToBytePixelConverter b2bpc =
                    PixelUtils.getB2BConverter(bpg, bps);
                if (bfmtsetter.getNcomp() < 4 && b2bpc == null) continue;
                if (!isGeneral(b2bpc)) {
                    PixelConverter pc = PixelUtils.getConverter(bpg, bps);
                    assertEquals(b2bpc, pc);
                }
                assertEquals(b2bpc.getGetter(), bpg);
                assertEquals(b2bpc.getSetter(), bps);
                Color testColors[] = ((bpg.getAlphaType() == AlphaType.OPAQUE ||
                                       bps.getAlphaType() == AlphaType.OPAQUE)
                                      ? OpaqueTestColors : TestColors);
                int srcncomp = bfmtgetter.getNcomp();
                for (int i = 0; i < testColors.length; i++) {
                    bfmtgetter.setArgb(srchbuf, i * srcncomp, getArgb(testColors[i]));
                    bfmtgetter.setArgb(srcdbuf, i * srcncomp, getArgb(testColors[i]));
                    bfmtgetter.setArgb(srcarr,  i * srcncomp, getArgb(testColors[i]));
                }
                int dstncomp = bfmtsetter.getNcomp();
                double delta = 0.0;
                if (bpg.getAlphaType() == AlphaType.PREMULTIPLIED) delta += 1.0;
                if (bps.getAlphaType() == AlphaType.PREMULTIPLIED) delta += 1.0;
                boolean isgray = bfmtgetter.isGray() || bfmtsetter.isGray();
                if (isgray) {
                    delta += 1.0;
                }
                b2bpc.convert(srchbuf, 0, 0, dsthbuf, 0, 0, testColors.length, 1);
                b2bpc.convert(srchbuf, 0, 0, dstdbuf, 0, 0, testColors.length, 1);
                b2bpc.convert(srchbuf, 0, 0, dstarr,  0, 0, testColors.length, 1);
                for (int i = 0; i < testColors.length; i++) {
                    int refargb = getArgb(testColors[i]);
                    if (isgray) refargb = grayify(refargb);
                    checkArgb(refargb, bfmtsetter.getArgb(dsthbuf, i * dstncomp), delta);
                    checkArgb(refargb, bfmtsetter.getArgb(dstdbuf, i * dstncomp), delta);
                    checkArgb(refargb, bfmtsetter.getArgb(dstarr,  i * dstncomp), delta);
                }
                clear(dsthbuf, dstdbuf, dstarr);
                b2bpc.convert(srcdbuf, 0, 0, dsthbuf, 0, 0, testColors.length, 1);
                b2bpc.convert(srcdbuf, 0, 0, dstdbuf, 0, 0, testColors.length, 1);
                b2bpc.convert(srcdbuf, 0, 0, dstarr,  0, 0, testColors.length, 1);
                for (int i = 0; i < testColors.length; i++) {
                    int refargb = getArgb(testColors[i]);
                    if (isgray) refargb = grayify(refargb);
                    checkArgb(refargb, bfmtsetter.getArgb(dsthbuf, i * dstncomp), delta);
                    checkArgb(refargb, bfmtsetter.getArgb(dstdbuf, i * dstncomp), delta);
                    checkArgb(refargb, bfmtsetter.getArgb(dstarr,  i * dstncomp), delta);
                }
                clear(dsthbuf, dstdbuf, dstarr);
                b2bpc.convert(srcarr, 0, 0, dsthbuf, 0, 0, testColors.length, 1);
                b2bpc.convert(srcarr, 0, 0, dstdbuf, 0, 0, testColors.length, 1);
                b2bpc.convert(srcarr, 0, 0, dstarr,  0, 0, testColors.length, 1);
                for (int i = 0; i < testColors.length; i++) {
                    int refargb = getArgb(testColors[i]);
                    if (isgray) refargb = grayify(refargb);
                    checkArgb(refargb, bfmtsetter.getArgb(dsthbuf, i * dstncomp), delta);
                    checkArgb(refargb, bfmtsetter.getArgb(dstdbuf, i * dstncomp), delta);
                    checkArgb(refargb, bfmtsetter.getArgb(dstarr,  i * dstncomp), delta);
                }
            }
        }
    }

    @Test
    public void testB2IConverterTypes() {
        testB2IConverterTypes(0);
        testB2IConverterTypes(1);
    }

    private void testB2IConverterTypes(int off) {
        ByteBuffer srchbuf = heapByteBuffer(off, 4 * TestColors.length);
        ByteBuffer srcdbuf = directByteBuffer(off, 4 * TestColors.length);
        byte srcarr[] = new byte[4 * TestColors.length];
        IntBuffer dsthbuf = heapIntBuffer(off, TestColors.length);
        IntBuffer dstdbuf = directIntBuffer(off,TestColors.length);
        int dstarr[] = new int[TestColors.length];
        for (ByteFormat bfmtgetter : ByteFormats) {
            BytePixelGetter bpg = bfmtgetter.getGetter();
            for (IntFormat ifmtsetter : IntFormats) {
                IntPixelSetter ips = ifmtsetter.getSetter();
                if (ips == null) continue;
                ByteToIntPixelConverter b2ipc =
                    PixelUtils.getB2IConverter(bpg, ips);
                // Should not be null - so far all int formats are full color+alpha
                if (!isGeneral(b2ipc)) {
                    PixelConverter pc = PixelUtils.getConverter(bpg, ips);
                    assertEquals(b2ipc, pc);
                }
                assertEquals(b2ipc.getGetter(), bpg);
                assertEquals(b2ipc.getSetter(), ips);
                Color testColors[] = ((bpg.getAlphaType() == AlphaType.OPAQUE ||
                                       ips.getAlphaType() == AlphaType.OPAQUE)
                                      ? OpaqueTestColors : TestColors);
                int srcncomp = bfmtgetter.getNcomp();
                for (int i = 0; i < testColors.length; i++) {
                    bfmtgetter.setArgb(srchbuf, i * srcncomp, getArgb(testColors[i]));
                    bfmtgetter.setArgb(srcdbuf, i * srcncomp, getArgb(testColors[i]));
                    bfmtgetter.setArgb(srcarr,  i * srcncomp, getArgb(testColors[i]));
                }
                double delta = 0.0;
                if (bpg.getAlphaType() == AlphaType.PREMULTIPLIED) delta += 1.0;
                if (ips.getAlphaType() == AlphaType.PREMULTIPLIED) delta += 1.0;
                boolean isgray = bfmtgetter.isGray();
                if (isgray) {
                    delta += 1.0;
                }
                b2ipc.convert(srchbuf, 0, 0, dsthbuf, 0, 0, testColors.length, 1);
                b2ipc.convert(srchbuf, 0, 0, dstdbuf, 0, 0, testColors.length, 1);
                b2ipc.convert(srchbuf, 0, 0, dstarr,  0, 0, testColors.length, 1);
                for (int i = 0; i < testColors.length; i++) {
                    int refargb = getArgb(testColors[i]);
                    if (isgray) refargb = grayify(refargb);
                    checkArgb(refargb, ifmtsetter.getArgb(dsthbuf, i), delta);
                    checkArgb(refargb, ifmtsetter.getArgb(dstdbuf, i), delta);
                    checkArgb(refargb, ifmtsetter.getArgb(dstarr,  i), delta);
                }
                clear(dsthbuf, dstdbuf, dstarr);
                b2ipc.convert(srcdbuf, 0, 0, dsthbuf, 0, 0, testColors.length, 1);
                b2ipc.convert(srcdbuf, 0, 0, dstdbuf, 0, 0, testColors.length, 1);
                b2ipc.convert(srcdbuf, 0, 0, dstarr,  0, 0, testColors.length, 1);
                for (int i = 0; i < testColors.length; i++) {
                    int refargb = getArgb(testColors[i]);
                    if (isgray) refargb = grayify(refargb);
                    checkArgb(refargb, ifmtsetter.getArgb(dsthbuf, i), delta);
                    checkArgb(refargb, ifmtsetter.getArgb(dstdbuf, i), delta);
                    checkArgb(refargb, ifmtsetter.getArgb(dstarr,  i), delta);
                }
                clear(dsthbuf, dstdbuf, dstarr);
                b2ipc.convert(srcarr, 0, 0, dsthbuf, 0, 0, testColors.length, 1);
                b2ipc.convert(srcarr, 0, 0, dstdbuf, 0, 0, testColors.length, 1);
                b2ipc.convert(srcarr, 0, 0, dstarr,  0, 0, testColors.length, 1);
                for (int i = 0; i < testColors.length; i++) {
                    int refargb = getArgb(testColors[i]);
                    if (isgray) refargb = grayify(refargb);
                    checkArgb(refargb, ifmtsetter.getArgb(dsthbuf, i), delta);
                    checkArgb(refargb, ifmtsetter.getArgb(dstdbuf, i), delta);
                    checkArgb(refargb, ifmtsetter.getArgb(dstarr,  i), delta);
                }
            }
        }
    }

    @Test
    public void testI2BConverterTypes() {
        testI2BConverterTypes(0);
        testI2BConverterTypes(1);
    }

    private void testI2BConverterTypes(int off) {
        IntBuffer srchbuf = heapIntBuffer(off, TestColors.length);
        IntBuffer srcdbuf = directIntBuffer(off, TestColors.length);
        int srcarr[] = new int[TestColors.length];
        ByteBuffer dsthbuf = heapByteBuffer(off, 4 * TestColors.length);
        ByteBuffer dstdbuf = directByteBuffer(off, 4 * TestColors.length);
        byte dstarr[] = new byte[4 * TestColors.length];
        for (IntFormat ifmtgetter : IntFormats) {
            IntPixelGetter ipg = ifmtgetter.getGetter();
            for (ByteFormat bfmtsetter : ByteFormats) {
                BytePixelSetter bps = bfmtsetter.getSetter();
                if (bps == null) continue;
                IntToBytePixelConverter i2bpc =
                    PixelUtils.getI2BConverter(ipg, bps);
                if (bfmtsetter.getNcomp() < 4 && i2bpc == null) continue;
                if (!isGeneral(i2bpc)) {
                    PixelConverter pc = PixelUtils.getConverter(ipg, bps);
                    assertEquals(i2bpc, pc);
                }
                assertEquals(i2bpc.getGetter(), ipg);
                assertEquals(i2bpc.getSetter(), bps);
                Color testColors[] = ((ipg.getAlphaType() == AlphaType.OPAQUE ||
                                       bps.getAlphaType() == AlphaType.OPAQUE)
                                      ? OpaqueTestColors : TestColors);
                for (int i = 0; i < testColors.length; i++) {
                    ifmtgetter.setArgb(srchbuf, i, getArgb(testColors[i]));
                    ifmtgetter.setArgb(srcdbuf, i, getArgb(testColors[i]));
                    ifmtgetter.setArgb(srcarr,  i, getArgb(testColors[i]));
                }
                int dstncomp = bfmtsetter.getNcomp();
                double delta = 0.0;
                if (ipg.getAlphaType() == AlphaType.PREMULTIPLIED) delta += 1.0;
                if (bps.getAlphaType() == AlphaType.PREMULTIPLIED) delta += 1.0;
                boolean isgray = bfmtsetter.isGray();
                if (isgray) {
                    delta += 1.0;
                }
                i2bpc.convert(srchbuf, 0, 0, dsthbuf, 0, 0, testColors.length, 1);
                i2bpc.convert(srchbuf, 0, 0, dstdbuf, 0, 0, testColors.length, 1);
                i2bpc.convert(srchbuf, 0, 0, dstarr,  0, 0, testColors.length, 1);
                for (int i = 0; i < testColors.length; i++) {
                    int refargb = getArgb(testColors[i]);
                    if (isgray) refargb = grayify(refargb);
                    checkArgb(refargb, bfmtsetter.getArgb(dsthbuf, i * dstncomp), delta);
                    checkArgb(refargb, bfmtsetter.getArgb(dstdbuf, i * dstncomp), delta);
                    checkArgb(refargb, bfmtsetter.getArgb(dstarr,  i * dstncomp), delta);
                }
                clear(dsthbuf, dstdbuf, dstarr);
                i2bpc.convert(srcdbuf, 0, 0, dsthbuf, 0, 0, testColors.length, 1);
                i2bpc.convert(srcdbuf, 0, 0, dstdbuf, 0, 0, testColors.length, 1);
                i2bpc.convert(srcdbuf, 0, 0, dstarr,  0, 0, testColors.length, 1);
                for (int i = 0; i < testColors.length; i++) {
                    int refargb = getArgb(testColors[i]);
                    if (isgray) refargb = grayify(refargb);
                    checkArgb(refargb, bfmtsetter.getArgb(dsthbuf, i * dstncomp), delta);
                    checkArgb(refargb, bfmtsetter.getArgb(dstdbuf, i * dstncomp), delta);
                    checkArgb(refargb, bfmtsetter.getArgb(dstarr,  i * dstncomp), delta);
                }
                clear(dsthbuf, dstdbuf, dstarr);
                i2bpc.convert(srcarr, 0, 0, dsthbuf, 0, 0, testColors.length, 1);
                i2bpc.convert(srcarr, 0, 0, dstdbuf, 0, 0, testColors.length, 1);
                i2bpc.convert(srcarr, 0, 0, dstarr,  0, 0, testColors.length, 1);
                for (int i = 0; i < testColors.length; i++) {
                    int refargb = getArgb(testColors[i]);
                    if (isgray) refargb = grayify(refargb);
                    checkArgb(refargb, bfmtsetter.getArgb(dsthbuf, i * dstncomp), delta);
                    checkArgb(refargb, bfmtsetter.getArgb(dstdbuf, i * dstncomp), delta);
                    checkArgb(refargb, bfmtsetter.getArgb(dstarr,  i * dstncomp), delta);
                }
            }
        }
    }

    @Test
    public void testI2IConverterTypes() {
        testI2IConverterTypes(0);
        testI2IConverterTypes(1);
    }

    private void testI2IConverterTypes(int off) {
        IntBuffer srchbuf = heapIntBuffer(off, TestColors.length);
        IntBuffer srcdbuf = directIntBuffer(off, TestColors.length);
        int srcarr[] = new int[TestColors.length];
        IntBuffer dsthbuf = heapIntBuffer(off, TestColors.length);
        IntBuffer dstdbuf = directIntBuffer(off, TestColors.length);
        int dstarr[] = new int[TestColors.length];
        for (IntFormat ifmtgetter : IntFormats) {
            IntPixelGetter ipg = ifmtgetter.getGetter();
            for (IntFormat ifmtsetter : IntFormats) {
                IntPixelSetter ips = ifmtsetter.getSetter();
                if (ips == null) continue;
                IntToIntPixelConverter i2ipc =
                    PixelUtils.getI2IConverter(ipg, ips);
                // Should not be null - so far all int formats are full color+alpha
                if (!isGeneral(i2ipc)) {
                    PixelConverter pc = PixelUtils.getConverter(ipg, ips);
                    assertEquals(i2ipc, pc);
                }
                assertEquals(i2ipc.getGetter(), ipg);
                assertEquals(i2ipc.getSetter(), ips);
                Color testColors[] = ((ipg.getAlphaType() == AlphaType.OPAQUE ||
                                       ips.getAlphaType() == AlphaType.OPAQUE)
                                      ? OpaqueTestColors : TestColors);
                for (int i = 0; i < testColors.length; i++) {
                    ifmtgetter.setArgb(srchbuf, i, getArgb(testColors[i]));
                    ifmtgetter.setArgb(srcdbuf, i, getArgb(testColors[i]));
                    ifmtgetter.setArgb(srcarr,  i, getArgb(testColors[i]));
                }
                double delta = 0.0;
                if (ipg.getAlphaType() == AlphaType.PREMULTIPLIED) delta += 1.0;
                if (ips.getAlphaType() == AlphaType.PREMULTIPLIED) delta += 1.0;
                i2ipc.convert(srchbuf, 0, 0, dsthbuf, 0, 0, testColors.length, 1);
                i2ipc.convert(srchbuf, 0, 0, dstdbuf, 0, 0, testColors.length, 1);
                i2ipc.convert(srchbuf, 0, 0, dstarr,  0, 0, testColors.length, 1);
                for (int i = 0; i < testColors.length; i++) {
                    int refargb = getArgb(testColors[i]);
                    checkArgb(refargb, ifmtsetter.getArgb(dsthbuf, i), delta);
                    checkArgb(refargb, ifmtsetter.getArgb(dstdbuf, i), delta);
                    checkArgb(refargb, ifmtsetter.getArgb(dstarr,  i), delta);
                }
                clear(dsthbuf, dstdbuf, dstarr);
                i2ipc.convert(srcdbuf, 0, 0, dsthbuf, 0, 0, testColors.length, 1);
                i2ipc.convert(srcdbuf, 0, 0, dstdbuf, 0, 0, testColors.length, 1);
                i2ipc.convert(srcdbuf, 0, 0, dstarr,  0, 0, testColors.length, 1);
                for (int i = 0; i < testColors.length; i++) {
                    int refargb = getArgb(testColors[i]);
                    checkArgb(refargb, ifmtsetter.getArgb(dsthbuf, i), delta);
                    checkArgb(refargb, ifmtsetter.getArgb(dstdbuf, i), delta);
                    checkArgb(refargb, ifmtsetter.getArgb(dstarr,  i), delta);
                }
                clear(dsthbuf, dstdbuf, dstarr);
                i2ipc.convert(srcarr, 0, 0, dsthbuf, 0, 0, testColors.length, 1);
                i2ipc.convert(srcarr, 0, 0, dstdbuf, 0, 0, testColors.length, 1);
                i2ipc.convert(srcarr, 0, 0, dstarr,  0, 0, testColors.length, 1);
                for (int i = 0; i < testColors.length; i++) {
                    int refargb = getArgb(testColors[i]);
                    checkArgb(refargb, ifmtsetter.getArgb(dsthbuf, i), delta);
                    checkArgb(refargb, ifmtsetter.getArgb(dstdbuf, i), delta);
                    checkArgb(refargb, ifmtsetter.getArgb(dstarr,  i), delta);
                }
            }
        }
    }
}
