/*
 * Copyright (c) 2009, 2026, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.iio.common.ImageLoaderImpl;
import com.sun.javafx.iio.common.ImageTools;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;

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
     * The IJG library decoder for this reader, an opaque handle owned by the
     * javafx_iio library. It is assigned in the constructor and then is passed
     * in to every JPEGNative call. It is set to NULL by dispose to avoid
     * disposing twice.
     */
    private MemorySegment decoder = MemorySegment.NULL;
    /**
     * The stream the decoder pulls compressed bytes from, through the read and
     * skip callbacks of {@link JPEGNative}.
     */
    private final InputStream input;
    /**
     * Scratch array for the read callback, sized to the decoder's request; the
     * per-reader stream buffer the JNI decoder kept.
     */
    private byte[] streamBuffer;
    /**
     * The exception a callback caught while native code was on the stack. The
     * callback returns an error to the decoder instead of throwing, the
     * decoder aborts, and {@link JPEGNative} rethrows this from the downcall.
     */
    private Throwable pendingUpcallFailure;
    /**
     * This loader's registration under the process-wide callback table of
     * {@link JPEGNative}: the id the decoder hands back as {@code user}.
     * Registered before the decoder is created, unregistered after it is
     * disposed.
     */
    private final JPEGNative.Callbacks callbacks;
    /** Set from the image header read by the decoder */
    private int inWidth;
    /** Set from the image header read by the decoder */
    private int inHeight;
    /**
     * Set from the image header read by the decoder.  A modified
     * IJG+NIFTY colorspace code.
     */
    private int inColorSpaceCode;
    /**
     * Set from the image header read by the decoder.  A modified
     * IJG+NIFTY colorspace code.
     */
    private int outColorSpaceCode;
    /** Set from the image header read by the decoder */
    private byte[] iccData;
    /** Set by setOutputAttributes after starting decompression. */
    private int outWidth;
    /** Set by setOutputAttributes after starting decompression. */
    private int outHeight;
    private ImageType outImageType;

    private boolean isDisposed = false;

    private Lock accessLock = new Lock();

    /*
     * Called when the image header has been read.
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
     * Called after starting decompression.
     */
    private void setOutputAttributes(int width, int height) {
        this.outWidth = width;
        this.outHeight = height;
    }

    /*
     * Called by the decoder's update_progress callback (see JPEGNative), once
     * per scanline before it is read and once more with the output height.
     */
    void updateImageProgress(int outLinesDecoded) {
        updateImageProgress(100.0F * outLinesDecoded / outHeight);
    }

    /*
     * Called by the decoder's emit_warning callback (see JPEGNative). The
     * message is null when the stream ended before the EOI marker.
     */
    void emitDecoderWarning(String message) {
        emitWarning(message);
    }

    /* The stream the decoder's read and skip callbacks pull from. */
    InputStream stream() {
        return input;
    }

    /* Scratch for the read callback, holding at least capacity bytes. */
    byte[] streamBuffer(int capacity) {
        if (streamBuffer == null || streamBuffer.length < capacity) {
            streamBuffer = new byte[capacity];
        }
        return streamBuffer;
    }

    /* Stashes the exception a callback caught; the decoder is aborting. */
    void setPendingUpcallFailure(Throwable failure) {
        pendingUpcallFailure = failure;
    }

    /* Takes the stashed exception, leaving the slot empty. */
    Throwable takePendingUpcallFailure() {
        Throwable failure = pendingUpcallFailure;
        pendingUpcallFailure = null;
        return failure;
    }

    JPEGImageLoader(InputStream input) throws IOException {
        super(JPEGDescriptor.getInstance());
        if (input == null) {
            throw new IllegalArgumentException("input == null!");
        }
        this.input = input;
        this.callbacks = JPEGNative.register(this);

        try {
            initDecompressor();
        } catch (IOException | RuntimeException | Error e) {
            dispose();
            throw e;
        }

        if (this.decoder.address() == 0L) {
            dispose();
            throw new IOException("Unable to initialize JPEG decompressor");
        }
    }

    /*
     * Creates the decoder and reads the image header. A tables-only datastream
     * leaves the header width 0, for which no input attributes are set.
     */
    private void initDecompressor() throws IOException {
        JPEGNative.Header header = JPEGNative.create(this, callbacks);
        this.decoder = header.decoder();
        if (header.width() != 0) {
            setInputAttributes(header.width(), header.height(),
                    header.jpegColorSpace(), header.outColorSpace(),
                    header.numComponents(), JPEGNative.iccProfile(decoder));
        }
    }

    @Override
    public synchronized void dispose() {
        if(!accessLock.isLocked() && !isDisposed) {
            isDisposed = true;
            if (decoder.address() != 0L) {
                JPEGNative.dispose(decoder);
                decoder = MemorySegment.NULL;
            }
            // Only now: the id must resolve for as long as a downcall on the decoder can call back.
            callbacks.unregister();
        }
    }

    @Override
    public ImageFrame load(int imageIndex, double w, double h, boolean preserveAspectRatio, boolean smooth,
                           float screenPixelScale, float imagePixelScale) throws IOException {
        ImageTools.validateMaxDimensions(w, h, imagePixelScale);

        if (imageIndex != 0) {
            return null;
        }

        accessLock.lock();

        // Determine output image dimensions.
        int[] widthHeight = ImageTools.computeDimensions(
            inWidth, inHeight, (int)(w * imagePixelScale), (int)(h * imagePixelScale), preserveAspectRatio);
        int width = widthHeight[0];
        int height = widthHeight[1];

        ImageMetadata md = new ImageMetadata(null, true,
                null, null, null, null, null,
                width, height, null, null, null);

        updateImageMetadata(md);

        ByteBuffer buffer = null;

        int outNumComponents;
        try {
            JPEGNative.Geometry geometry = JPEGNative.startDecompression(this, decoder,
                    outColorSpaceCode, width, height);
            setOutputAttributes(geometry.width(), geometry.height());
            outNumComponents = geometry.components();

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
            JPEGNative.decompress(this, decoder, listeners != null && !listeners.isEmpty(), array);
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
                width, height, width * outNumComponents, imagePixelScale, md);
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
