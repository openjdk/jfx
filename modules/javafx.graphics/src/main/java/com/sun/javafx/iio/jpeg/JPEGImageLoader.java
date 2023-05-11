/*
 * Copyright (c) 2009, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.iio.jpeg;

import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageMetadata;
import com.sun.javafx.iio.ImageStorage.ImageType;
import com.sun.glass.utils.NativeLibLoader;
import com.sun.javafx.iio.common.ImageLoaderImpl;
import com.sun.javafx.iio.common.ImageTools;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class JPEGImageLoader extends ImageLoaderImpl {

    // IJG Color codes.
    public static final int JCS_UNKNOWN = 0;       // error/unspecified
    public static final int JCS_GRAYSCALE = 1;     // monochrome
    public static final int JCS_RGB = 2;           // red/green/blue
    public static final int JCS_YCbCr = 3;         // Y/Cb/Cr (also known as YUV)
    public static final int JCS_CMYK = 4;          // C/M/Y/K
    public static final int JCS_YCC = 5;           // PhotoYCC
    public static final int JCS_RGBA = 6;          // RGB-Alpha
    public static final int JCS_YCbCrA = 7;        // Y/Cb/Cr/Alpha
    // 8 and 9 were old "Legacy" codes which the old code never identified
    // on reading anyway.  Support for writing them is being dropped, too.
    public static final int JCS_YCCA = 10;         // PhotoYCC-Alpha
    public static final int JCS_YCCK = 11;         // Y/Cb/Cr/K
    /**
     * The following variable contains a pointer to the IJG library
     * structure for this reader.  It is assigned in the constructor
     * and then is passed in to every native call.  It is set to 0
     * by dispose to avoid disposing twice.
     */
    private long structPointer = 0L;
    /** Set by setInputAttributes native code callback */
    private int inWidth;
    /** Set by setInputAttributes native code callback */
    private int inHeight;
    /**
     * Set by setInputAttributes native code callback.  A modified
     * IJG+NIFTY colorspace code.
     */
    private int inColorSpaceCode;
    /**
     * Set by setInputAttributes native code callback.  A modified
     * IJG+NIFTY colorspace code.
     */
    private int outColorSpaceCode;
    /** Set by setInputAttributes native code callback */
    private byte[] iccData;
    /** Set by setOutputAttributes native code callback. */
    private int outWidth;
    /** Set by setOutputAttributes native code callback. */
    private int outHeight;
    private ImageType outImageType;

    private boolean isDisposed = false;

    private Lock accessLock = new Lock();

    /** Sets up static C structures. */
    private static native void initJPEGMethodIDs(Class inputStreamClass);

    private static native void disposeNative(long structPointer);

    /** Sets up per-reader C structure and returns a pointer to it. */
    private native long initDecompressor(InputStream stream) throws IOException;

    /** Sets output color space and scale factor.
     *  Returns number of components which native decoder
     *  will produce for requested output color space.
     */
    private native int startDecompression(long structPointer,
            int outColorSpaceCode, int scaleNum, int scaleDenom);

    private native boolean decompressIndirect(long structPointer, boolean reportProgress, byte[] array) throws IOException;

    static {
        @SuppressWarnings("removal")
        var dummy = AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            NativeLibLoader.loadLibrary("javafx_iio");
            return null;
        });
        initJPEGMethodIDs(InputStream.class);
    }

    /*
     * Called by the native code when the image header has been read.
     */
    private void setInputAttributes(int width,
            int height,
            int colorSpaceCode,
            int outColorSpaceCode,
            int numComponents,
            byte[] iccData) {
        this.inWidth = width;
        this.inHeight = height;
        this.inColorSpaceCode = colorSpaceCode;
        this.outColorSpaceCode = outColorSpaceCode;
        this.iccData = iccData;

        // Set outImageType.
        switch (outColorSpaceCode) {
            case JCS_GRAYSCALE:
                this.outImageType = ImageType.GRAY;
                break;
            case JCS_YCbCr:
            case JCS_YCC:
            case JCS_RGB:
                this.outImageType = ImageType.RGB;
                break;
            case JCS_CMYK:
            case JCS_YCbCrA:
            case JCS_YCCA:
            case JCS_YCCK:
            case JCS_RGBA:
                this.outImageType = ImageType.RGBA_PRE;
                break;
            case JCS_UNKNOWN:
                switch (numComponents) {
                    case 1:
                        this.outImageType = ImageType.GRAY;
                        break;
                    case 3:
                        this.outImageType = ImageType.RGB;
                        break;
                    case 4:
                        this.outImageType = ImageType.RGBA_PRE;
                        break;
                    default:
                        assert false;
                }
                break;
            default:
                assert false;
                break;
        }
    }

    /*
     * Called by the native code after starting decompression.
     */
    private void setOutputAttributes(int width, int height) {
        this.outWidth = width;
        this.outHeight = height;
    }

    private void updateImageProgress(int outLinesDecoded) {
        updateImageProgress(100.0F * outLinesDecoded / outHeight);
    }

    JPEGImageLoader(InputStream input) throws IOException {
        super(JPEGDescriptor.getInstance());
        if (input == null) {
            throw new IllegalArgumentException("input == null!");
        }

        try {
            this.structPointer = initDecompressor(input);
        } catch (IOException e) {
            dispose();
            throw e;
        }

        if (this.structPointer == 0L) {
            throw new IOException("Unable to initialize JPEG decompressor");
        }
    }

    @Override
    public synchronized void dispose() {
        if(!accessLock.isLocked() && !isDisposed && structPointer != 0L) {
            isDisposed = true;
            disposeNative(structPointer);
            structPointer = 0L;
        }
    }

    @Override
    public ImageFrame load(int imageIndex, int width, int height, boolean preserveAspectRatio, boolean smooth) throws IOException {
        if (imageIndex != 0) {
            return null;
        }

        accessLock.lock();

        // Determine output image dimensions.
        int[] widthHeight = ImageTools.computeDimensions(inWidth, inHeight, width, height, preserveAspectRatio);
        width = widthHeight[0];
        height = widthHeight[1];

        ImageMetadata md = new ImageMetadata(null, true,
                null, null, null, null, null,
                width, height, null, null, null);

        updateImageMetadata(md);

        ByteBuffer buffer = null;

        int outNumComponents;
        try {
            outNumComponents = startDecompression(structPointer,
                    outColorSpaceCode, width, height);

            if (outWidth < 0 || outHeight < 0 || outNumComponents < 0) {
               throw new IOException("negative dimension.");
            }
            if (outWidth > (Integer.MAX_VALUE / outNumComponents)) {
               throw new IOException("bad width.");
            }
            int scanlineStride = outWidth * outNumComponents;
            if (scanlineStride > (Integer.MAX_VALUE / outHeight)) {
               throw new IOException("bad height.");
            }

            byte[] array = new byte[scanlineStride*outHeight];
            buffer = ByteBuffer.wrap(array);
            decompressIndirect(structPointer, listeners != null && !listeners.isEmpty(), buffer.array());
        } catch (IOException e) {
            throw e;
        } catch (Throwable t) {
            throw new IOException(t);
        } finally {
            accessLock.unlock();
            dispose();
        }

        if (buffer == null) {
            throw new IOException("Error decompressing JPEG stream!");
        }

        // Check whether the decompressed image has been scaled to the correct
        // dimensions. If not, downscale it here. Note outData, outHeight, and
        // outWidth refer to the image as returned by the decompressor. This
        // image might have been downscaled from the original source by a factor
        // of N/8 where 1 <= N <=8.
        if (outWidth != width || outHeight != height) {
            buffer = ImageTools.scaleImage(buffer,
                    outWidth, outHeight, outNumComponents, width, height, smooth);
        }

        return new ImageFrame(outImageType, buffer,
                width, height, width * outNumComponents, null, md);
    }

    private static class Lock {
        private boolean locked;

        public Lock() {
            locked = false;
        }

        public synchronized boolean isLocked() {
            return locked;
        }

        public synchronized void lock() {
            if (locked) {
                throw new IllegalStateException("Recursive loading is not allowed.");
            }
            locked = true;
        }

        public synchronized void unlock() {
            if (!locked) {
                throw new IllegalStateException("Invalid loader state.");
            }
            locked = false;
        }
    }
}
