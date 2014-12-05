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

package com.sun.javafx.iio;

import com.sun.javafx.iio.gif.GIFImageLoader2;
import java.awt.image.*;
import java.io.*;
import static org.junit.Assert.*;
import org.junit.Test;


class TestStream extends InputStream {
    byte data[];
    int  p = 0;

    public TestStream(byte data[]) {
        this.data = data;
    }

    @Override
    public int read() throws IOException {
        return p < data.length ? (int)data[p++] & 0xff : -1;
    }
}

public class GIFLoaderTest {

    @Test
    public void testCtorNPE() {
        try {
            new GIFImageLoader2(null);
        } catch (NullPointerException ex) {
           return; // PASSED
        } catch (IOException ioEx) {
            fail("unexpected IOException:" + ioEx.toString());
        }
        fail("expected NPE after constructor invocation with null");
    }



    @Test
    public void testCtorReadBadHeader1()  {
        final byte tooShortHeaderData[] = {
            0,1,2,3,4
        };

        try {
            new GIFImageLoader2(new TestStream(tooShortHeaderData));
        } catch (EOFException ex) {
            return; // PASSED
        } catch (IOException ioEx) {
            fail("unexpected IOException:" + ioEx.toString());
        }
        fail("expected EOF exception for streams lesser then 13 bytes");
    }

    @Test
    public void testCtorReadBadHeader2()  {
        final byte tooShortHeaderData[] = {
            'G', 'I', 'F', '8', '9', 'a',
            0, 0, 0, 0, 0, 0
        };

        try {
            new GIFImageLoader2(new TestStream(tooShortHeaderData));
        } catch (EOFException ex) {
            return; // PASSED
        } catch (IOException ioEx) {
            fail("unexpected IOException:" + ioEx.toString());
        }
        fail("expected EOF exception for streams lesser then 13 bytes");
    }

    @Test
    public void testCtorReadGoodHeader()  {
        final byte _87HeaderData[] = {
            'G', 'I', 'F', '8', '7', 'a',
            1, 0, 1, 0, 0, 0, 0
        };
        final byte _89HeaderData[] = {
            'G', 'I', 'F', '8', '9', 'a',
            1, 0, 1, 0, 0, 0, 0
        };
        try {
            new GIFImageLoader2(new TestStream(_87HeaderData));
            new GIFImageLoader2(new TestStream(_89HeaderData));
        } catch (IOException ioEx) {
            ioEx.printStackTrace(System.out);
            fail("unexpected IOException:" + ioEx.toString());

        }
    }

    @Test (timeout=2000)
    public void testCtorReadBadExtension()  {
        final byte badGifData[] = {
            'G', 'I', 'F', '8', '9', 'a',
            1, 0, 1, 0, -112, 0, 0, -18, 51, 34,
            0, 0, 0, 44, 0, 0, 0, 0, 1, 0, 1, 0,
            0, 2, 2, 4, 1, 43, 48
        };

        // Create a loader using the data containing a bad GIF extension
        GIFImageLoader2 loader = null;
        try {
            loader = new GIFImageLoader2(new TestStream(badGifData));
        } catch (IOException ioEx) {
            fail("unexpected IOException:" + ioEx.toString());
        }
        assertNotNull(loader);

        // Now try to load the image; it should get an EOFException
        try {
            loader.load(0, 1, 1, true, true);
        } catch (EOFException ex) {
            return; // PASSED
        } catch (IOException ioEx) {
            fail("unexpected IOException:" + ioEx.toString());
        }
        fail("expected EOF exception for streams with bad extension");
    }

    private void compareBGRaAndIndexed(byte dataRGBA[], byte dataIndexed[], int paletteBGRA[]) {
        assertEquals(dataIndexed.length*4, dataRGBA.length);
        for (int i = 0, j = 0, e = dataIndexed.length; i < e; j += 4, ++i) {
            int r = dataRGBA[j+0] & 0xFF, g = dataRGBA[j+1] & 0xFF,
                b = dataRGBA[j+2] & 0xFF, a = dataRGBA[j+3] & 0xFF;
            int x = b + (g<<8) + (r<<16) + (a<<24);
            int y = paletteBGRA[dataIndexed[i] & 0xFF];

            if ((x != y) && (((x & 0xFF000000) != 0) || ((y & 0xFF000000) != 0))) {
                fail("colors are different : JDK: " + Integer.toHexString(y)
                        + ", JavaFX: " + Integer.toHexString(x));
            }
        }
    }

    private void compareBGRaAndIndexed(byte dataRGBA[], int dataIndexed[], int paletteBGRA[]) {
        assertEquals(dataIndexed.length*4, dataRGBA.length);
        for (int i = 0, j = 0, e = dataIndexed.length; i < e; j += 4, ++i) {
            int r = dataRGBA[j+0] & 0xFF, g = dataRGBA[j+1] & 0xFF,
                b = dataRGBA[j+2] & 0xFF, a = dataRGBA[j+3] & 0xFF;
            int x = b + (g<<8) + (r<<16) + (a<<24);
            int y = paletteBGRA[dataIndexed[i] & 0xFF];
            if ((x != y) && (((x & 0xFF000000) != 0) || ((y & 0xFF000000) != 0))) {
                fail("colors are different : JDK: " + Integer.toHexString(y)
                        + ", JavaFX: " + Integer.toHexString(x));
            }
        }
    }


    private void compareImageFrameAndBImage(ImageFrame f, BufferedImage bimg) {
        byte dataRGBA[] = (byte[])f.getImageData().array();
        assertEquals(dataRGBA.length, f.getHeight() * f.getWidth() * 4);
        assertEquals(f.getImageType(), ImageStorage.ImageType.RGBA);

        assertEquals(f.getHeight(), bimg.getHeight());
        assertEquals(f.getWidth(), bimg.getWidth());

        if (bimg.getColorModel() instanceof IndexColorModel) {
            IndexColorModel idx = (IndexColorModel)bimg.getColorModel();
            int rgb[] = new int [256];
            idx.getRGBs(rgb);
            Raster r = bimg.getData();
            DataBuffer db = r.getDataBuffer();
            assertTrue( db instanceof DataBufferByte);
            DataBufferByte bdb = (DataBufferByte)db;
            assertEquals(bdb.getNumBanks(), 1);
            byte dataIndexed[] = bdb.getData(0);
            int bitsPerPixel = idx.getPixelSize();
            if (bitsPerPixel == 8) {
                assertEquals(dataIndexed.length, f.getHeight()*f.getWidth());
                compareBGRaAndIndexed(dataRGBA, dataIndexed, rgb);
            } else {
                int rgbData[] = new int[bimg.getWidth() * bimg.getHeight()];
                r.getPixels(0, 0, bimg.getWidth(), bimg.getHeight(), rgbData);
                compareBGRaAndIndexed(dataRGBA, rgbData, rgb);
            }
        } else {
            Raster r = bimg.getData();
//            System.out.println("" + r.getWidth() + "," + r.getHeight() + "," + r.getNumBands()
//                    + "," + r.getDataBuffer() + "," + r.getSampleModel());
            fail("Unexpected image form AWT");
        }
    }

    private void testReadGIFFile(String fname) throws IOException  {
        InputStream i = this.getClass().getResourceAsStream(fname);
        InputStream testStream = ImageTestHelper.createStutteringInputStream(i);
        ImageLoader l = new GIFImageLoader2(testStream);
        ImageFrame f = l.load(0, 0, 0, true, false);
        InputStream i2 = this.getClass().getResourceAsStream(fname);
        BufferedImage bimg = javax.imageio.ImageIO.read(i2);

        compareImageFrameAndBImage(f, bimg);
    }

    @Test
    public void testReadGIFFile() throws Exception  {
        for (String s : fileList) {
            try {
                testReadGIFFile(s);
            } catch (Exception ex) {
                System.err.println("Failure in test file " + s);
                throw ex;
            }
        }
    }

//    public static void main(String[] args) throws IOException {
//        new GIFLoaderTest().testReadGIFFile();
//    }

    final static String fileList[] = {
        "gif/adam_7_interlacing/basi0g01.gif",
        "gif/adam_7_interlacing/basi0g02.gif",
        "gif/adam_7_interlacing/basi0g04.gif",
        "gif/adam_7_interlacing/basi0g08.gif",
        "gif/adam_7_interlacing/basi0g16.gif",
        "gif/adam_7_interlacing/basi2c08.gif",
        "gif/adam_7_interlacing/basi2c16.gif",
        "gif/adam_7_interlacing/basi3p01.gif",
        "gif/adam_7_interlacing/basi3p02.gif",
        "gif/adam_7_interlacing/basi3p04.gif",
        "gif/adam_7_interlacing/basi3p08.gif",
        "gif/adam_7_interlacing/basi4a08.gif",
        "gif/adam_7_interlacing/basi4a16.gif",
        "gif/adam_7_interlacing/basi6a08.gif",
        "gif/adam_7_interlacing/basi6a16.gif",
        "gif/base/basn0g01.gif",
        "gif/base/basn0g02.gif",
        "gif/base/basn0g04.gif",
        "gif/base/basn0g08.gif",
        "gif/base/basn0g16.gif",
        "gif/base/basn2c08.gif",
        "gif/base/basn2c16.gif",
        "gif/base/basn3p01.gif",
        "gif/base/basn3p02.gif",
        "gif/base/basn3p04.gif",
        "gif/base/basn3p08.gif",
        "gif/base/basn4a08.gif",
        "gif/base/basn4a16.gif",
        "gif/base/basn6a08.gif",
        "gif/base/basn6a16.gif",
        "gif/background/bgai4a08.gif",
        "gif/background/bgai4a16.gif",
        "gif/background/bgan6a08.gif",
        "gif/background/bgan6a16.gif",
        "gif/background/bgbn4a08.gif",
        "gif/background/bggn4a16.gif",
        "gif/background/bgwn6a08.gif",
        "gif/background/bgyn6a16.gif",
        "gif/ancillary_chunks/ccwn2c08.gif",
        "gif/ancillary_chunks/ccwn3p08.gif",
        "gif/ancillary_chunks/cdfn2c08.gif",
        "gif/ancillary_chunks/cdhn2c08.gif",
        "gif/ancillary_chunks/cdsn2c08.gif",
        "gif/ancillary_chunks/cdun2c08.gif",
        "gif/ancillary_chunks/ch1n3p04.gif",
        "gif/ancillary_chunks/ch2n3p08.gif",
        "gif/ancillary_chunks/cm0n0g04.gif",
        "gif/ancillary_chunks/cm7n0g04.gif",
        "gif/ancillary_chunks/cm9n0g04.gif",
        "gif/ancillary_chunks/cs3n2c16.gif",
        "gif/ancillary_chunks/cs3n3p08.gif",
        "gif/ancillary_chunks/cs5n2c08.gif",
        "gif/ancillary_chunks/cs5n3p08.gif",
        "gif/ancillary_chunks/cs8n2c08.gif",
        "gif/ancillary_chunks/cs8n3p08.gif",
        "gif/ancillary_chunks/ct0n0g04.gif",
        "gif/ancillary_chunks/ct1n0g04.gif",
        "gif/ancillary_chunks/cten0g04.gif",
        "gif/ancillary_chunks/ctfn0g04.gif",
        "gif/ancillary_chunks/ctgn0g04.gif",
        "gif/ancillary_chunks/cthn0g04.gif",
        "gif/ancillary_chunks/ctjn0g04.gif",
        "gif/filtering/f00n0g08.gif",
        "gif/filtering/f00n2c08.gif",
        "gif/filtering/f01n0g08.gif",
        "gif/filtering/f01n2c08.gif",
        "gif/filtering/f02n0g08.gif",
        "gif/filtering/f02n2c08.gif",
        "gif/filtering/f03n0g08.gif",
        "gif/filtering/f03n2c08.gif",
        "gif/filtering/f04n0g08.gif",
        "gif/filtering/f04n2c08.gif",
        "gif/filtering/f99n0g04.gif",
        "gif/gamma/g03n0g16.gif",
        "gif/gamma/g03n2c08.gif",
        "gif/gamma/g03n3p04.gif",
        "gif/gamma/g04n0g16.gif",
        "gif/gamma/g04n2c08.gif",
        "gif/gamma/g04n3p04.gif",
        "gif/gamma/g05n0g16.gif",
        "gif/gamma/g05n2c08.gif",
        "gif/gamma/g05n3p04.gif",
        "gif/gamma/g07n0g16.gif",
        "gif/gamma/g07n2c08.gif",
        "gif/gamma/g07n3p04.gif",
        "gif/gamma/g10n0g16.gif",
        "gif/gamma/g10n2c08.gif",
        "gif/gamma/g10n3p04.gif",
        "gif/gamma/g25n0g16.gif",
        "gif/gamma/g25n2c08.gif",
        "gif/gamma/g25n3p04.gif",
        "gif/chunk_ordering/oi1n0g16.gif",
        "gif/chunk_ordering/oi1n2c16.gif",
        "gif/chunk_ordering/oi2n0g16.gif",
        "gif/chunk_ordering/oi2n2c16.gif",
        "gif/chunk_ordering/oi4n0g16.gif",
        "gif/chunk_ordering/oi4n2c16.gif",
        "gif/chunk_ordering/oi9n0g16.gif",
        "gif/chunk_ordering/oi9n2c16.gif",
        "gif/add_palets/pp0n2c16.gif",
        "gif/add_palets/pp0n6a08.gif",
        "gif/add_palets/ps1n0g08.gif",
        "gif/add_palets/ps1n2c16.gif",
        "gif/add_palets/ps2n0g08.gif",
        "gif/add_palets/ps2n2c16.gif",
        "gif/odd_sizes/s01i3p01.gif",
        "gif/odd_sizes/s01n3p01.gif",
        "gif/odd_sizes/s02i3p01.gif",
        "gif/odd_sizes/s02n3p01.gif",
        "gif/odd_sizes/s03i3p01.gif",
        "gif/odd_sizes/s03n3p01.gif",
        "gif/odd_sizes/s04i3p01.gif",
        "gif/odd_sizes/s04n3p01.gif",
        "gif/odd_sizes/s05i3p02.gif",
        "gif/odd_sizes/s05n3p02.gif",
        "gif/odd_sizes/s06i3p02.gif",
        "gif/odd_sizes/s06n3p02.gif",
        "gif/odd_sizes/s07i3p02.gif",
        "gif/odd_sizes/s07n3p02.gif",
        "gif/odd_sizes/s08i3p02.gif",
        "gif/odd_sizes/s08n3p02.gif",
        "gif/odd_sizes/s09i3p02.gif",
        "gif/odd_sizes/s09n3p02.gif",
        "gif/odd_sizes/s32i3p04.gif",
        "gif/odd_sizes/s32n3p04.gif",
        "gif/odd_sizes/s33i3p04.gif",
        "gif/odd_sizes/s33n3p04.gif",
        "gif/odd_sizes/s34i3p04.gif",
        "gif/odd_sizes/s34n3p04.gif",
        "gif/odd_sizes/s35i3p04.gif",
        "gif/odd_sizes/s35n3p04.gif",
        "gif/odd_sizes/s36i3p04.gif",
        "gif/odd_sizes/s36n3p04.gif",
        "gif/odd_sizes/s37i3p04.gif",
        "gif/odd_sizes/s37n3p04.gif",
        "gif/odd_sizes/s38i3p04.gif",
        "gif/odd_sizes/s38n3p04.gif",
        "gif/odd_sizes/s39i3p04.gif",
        "gif/odd_sizes/s39n3p04.gif",
        "gif/odd_sizes/s40i3p04.gif",
        "gif/odd_sizes/s40n3p04.gif",
        "gif/transparency/tbbn0g04.gif",
        "gif/transparency/tbbn2c16.gif",
        "gif/transparency/tbbn3p08.gif",
        "gif/transparency/tbgn2c16.gif",
        "gif/transparency/tbgn3p08.gif",
        "gif/transparency/tbrn2c08.gif",
        "gif/transparency/tbwn0g16.gif",
        "gif/transparency/tbwn3p08.gif",
        "gif/transparency/tbyn3p08.gif",
        "gif/transparency/tp0n0g08.gif",
        "gif/transparency/tp0n2c08.gif",
        "gif/transparency/tp0n3p08.gif",
        "gif/transparency/tp1n3p08.gif",
        "gif/corrupted/xc1n0g08.gif",
        "gif/corrupted/xc9n2c08.gif",
        "gif/corrupted/xcrn0g04.gif",
        "gif/corrupted/xcsn0g01.gif",
        "gif/corrupted/xd0n2c08.gif",
        "gif/corrupted/xd3n2c08.gif",
        "gif/corrupted/xd9n2c08.gif",
        "gif/corrupted/xdtn0g01.gif",
        "gif/corrupted/xhdn0g08.gif",
        "gif/corrupted/xlfn0g04.gif",
        "gif/corrupted/xs1n0g01.gif",
        "gif/corrupted/xs2n0g01.gif",
        "gif/corrupted/xs4n0g01.gif",
        "gif/corrupted/xs7n0g01.gif",
        "gif/zlib_compression_level/z00n2c08.gif",
        "gif/zlib_compression_level/z03n2c08.gif",
        "gif/zlib_compression_level/z06n2c08.gif",
        "gif/zlib_compression_level/z09n2c08.gif"
    };
}
