/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javafx.embed.swt;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

import java.lang.reflect.Method;
import java.lang.reflect.Field;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.Image;
import javafx.scene.image.WritablePixelFormat;

/**
 * This class provides utility methods for converting data types between
 * SWT and JavaFX formats.
 * @since JavaFX 2.2
 */
public class SWTFXUtils {
    private SWTFXUtils() {} // no instances

    /**
     * Snapshots the specified {@link ImageData} and stores a copy of
     * its pixels into a JavaFX {@link Image} object, creating a new
     * object if needed.
     * The returned {@code Image} will be a static snapshot of the state
     * of the pixels in the {@code ImageData} at the time the method
     * completes.  Further changes to the {@code ImageData} will not
     * be reflected in the {@code Image}.
     * <p>
     * The optional JavaFX {@link WritableImage} parameter may be reused
     * to store the copy of the pixels.
     * A new {@code Image} will be created if the supplied object is null,
     * is too small or of a type which the image pixels cannot be easily
     * converted into.
     *
     * @param imageData the {@code ImageData} object to be converted
     * @param image an optional {@code WritableImage} object that can be
     *        used to store the returned pixel data
     * @return an {@code Image} object representing a snapshot of the
     *         current pixels in the {@code ImageData}, or null if
     *         the {@code Image} is not readable.
     */
    public static WritableImage toFXImage(ImageData imageData,
                                          WritableImage image) {
        byte[] data = convertImage(imageData);
        if (data == null) return null;
        int width = imageData.width;
        int height = imageData.height;
        if (image != null) {
            int iw = (int) image.getWidth();
            int ih = (int) image.getHeight();
            if (iw < width || ih < height) {
                image = null;
            } else if (width < iw || height < ih) {
                int empty[] = new int[iw];
                PixelWriter pw = image.getPixelWriter();
                PixelFormat<IntBuffer> pf = PixelFormat.getIntArgbPreInstance();
                if (width < iw) {
                    pw.setPixels(width, 0, iw-width, height, pf, empty, 0, 0);
                }
                if (height < ih) {
                    pw.setPixels(0, height, iw, ih-height, pf, empty, 0, 0);
                }
            }
        }
        if (image == null) {
            image = new WritableImage(width, height);
        }
        PixelWriter pw = image.getPixelWriter();
        int scan = width * 4;
        PixelFormat<ByteBuffer> pf = PixelFormat.getByteBgraInstance();
        pw.setPixels(0, 0, width, height, pf, data, 0, scan);
        return image;
    }

    /**
     * Snapshots the specified JavaFX {@link Image} object and stores a
     * copy of its pixels into a new {@link ImageData} object.
     * The method will only convert a JavaFX {@code Image} that is readable
     * as per the conditions on the
     * {@link Image#getPixelReader() Image.getPixelReader()}
     * method.
     * If the {@code Image} is not readable, as determined by its
     * {@code getPixelReader()} method, then this method will return null.
     * If the {@code Image} is a writable, or other dynamic image, then
     * the {@code ImageData} will only be set to the current state of
     * the pixels in the image as determined by its {@link PixelReader}.
     * Further changes to the pixels of the {@code Image} will not be
     * reflected in the returned {@code ImageData}.
     * <p>
     * The optional {@code ImageData} parameter may be reused to store
     * the copy of the pixels.
     * A new {@code ImageData} will be created if the supplied object
     * is null, is too small or of a type which the image pixels cannot
     * be easily converted into.
     *
     * @param image the JavaFX {@code Image} to be converted
     * @param imageData an optional {@code ImageData} object that may be
     *        used to store the returned pixel data
     * @return a {@code ImageData} containing a snapshot of the JavaFX
     *         {@code Image}, or null if the {@code Image} is not readable.
     */
    public static ImageData fromFXImage(Image image, ImageData imageData) {
        PixelReader pr = image.getPixelReader();
        if (pr == null) {
            return null;
        }
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        int bpr = width * 4;
        int dataSize = bpr * height;
        byte[] buffer = new byte[dataSize];
        WritablePixelFormat<ByteBuffer> pf = PixelFormat.getByteBgraInstance();
        pr.getPixels(0, 0, width, height, pf, buffer, 0, bpr);
        byte[] alphaData = new byte[width * height];
        for (int y = 0, offset = 0, alphaOffset = 0; y < height; y++) {
            for (int x = 0; x < width; x++, offset += 4) {
                byte alpha = buffer[offset+3];
                buffer[offset+3] = 0;
                alphaData[alphaOffset++] = alpha;
            }
        }
        PaletteData palette = new PaletteData(0xFF00, 0xFF0000, 0xFF000000);
        imageData = new ImageData(width, height, 32, palette, 4, buffer);
        imageData.alphaData = alphaData;
        return imageData;
    }

    private static int blitSrc;
    private static boolean blitSrcCache;
    private static int BLIT_SRC() throws Exception {
        if (!blitSrcCache) {
            blitSrc = readValue("BLIT_SRC");
            blitSrcCache = true;
        }
        return blitSrc;
    }

    private static int alphaOpaque;
    private static boolean alphaOpaqueCache;
    private static int ALPHA_OPAQUE() throws Exception {
        if (!alphaOpaqueCache) {
            alphaOpaque = readValue("ALPHA_OPAQUE");
            alphaOpaqueCache = true;
        }
        return alphaOpaque;
    }

    private static int msbFirst;
    private static boolean msbFirstCache;
    private static int MSB_FIRST() throws Exception {
        if (!msbFirstCache) {
            msbFirst = readValue("MSB_FIRST");
            msbFirstCache = true;
        }
        return msbFirst;
    }

    @SuppressWarnings("removal")
    private static int readValue(final String name) throws Exception {
        final Class<?> clazz = ImageData.class;
        return AccessController.doPrivileged(
                (PrivilegedExceptionAction<Integer>) () -> {
                    Field field = clazz.getDeclaredField(name);
                    field.setAccessible(true);
                    return field.getInt(clazz);
                });
    }

    private static Method blitDirect;
    @SuppressWarnings("removal")
    private static void blit(int op,
            byte[] srcData, int srcDepth, int srcStride, int srcOrder,
            int srcX, int srcY, int srcWidth, int srcHeight,
            int srcRedMask, int srcGreenMask, int srcBlueMask,
            int alphaMode, byte[] alphaData, int alphaStride,
            int alphaX, int alphaY,
            byte[] destData, int destDepth, int destStride, int destOrder,
            int destX, int destY, int destWidth, int destHeight,
            int destRedMask, int destGreenMask, int destBlueMask,
            boolean flipX, boolean flipY) throws Exception {
        final Class<?> clazz = ImageData.class;
        if (blitDirect == null) {
            Class<?> I = Integer.TYPE, B = Boolean.TYPE, BA = byte[].class;
            final Class<?>[] argClasses = {I,
                    BA, I, I, I,
                    I, I, I, I,
                    I, I, I,
                    I, BA, I, I, I,
                    BA, I, I, I,
                    I, I, I, I,
                    I, I, I, B, B};
            blitDirect = AccessController.doPrivileged(
                    (PrivilegedExceptionAction<Method>) () -> {
                        Method method = clazz.
                            getDeclaredMethod("blit", argClasses);
                        method.setAccessible(true);
                        return method;
                    });
        }
        if (blitDirect != null) {
            blitDirect.invoke(clazz, op,
                    srcData, srcDepth, srcStride, srcOrder,
                    srcX, srcY, srcWidth, srcHeight,
                    srcRedMask, srcGreenMask, srcBlueMask,
                    alphaMode, alphaData, alphaStride, alphaX, alphaY,
                    destData, destDepth, destStride, destOrder,
                    destX, destY, destWidth, destHeight,
                    destRedMask, destGreenMask, destBlueMask,
                    flipX, flipY);
        }
    }

    private static Method blitPalette;
    @SuppressWarnings("removal")
    private static void blit(int op,
        byte[] srcData, int srcDepth, int srcStride, int srcOrder,
        int srcX, int srcY, int srcWidth, int srcHeight,
        byte[] srcReds, byte[] srcGreens, byte[] srcBlues,
        int alphaMode, byte[] alphaData, int alphaStride,
        int alphaX, int alphaY,
        byte[] destData, int destDepth, int destStride, int destOrder,
        int destX, int destY, int destWidth, int destHeight,
        int destRedMask, int destGreenMask, int destBlueMask,
        boolean flipX, boolean flipY) throws Exception {
        final Class<?> clazz = ImageData.class;
        if (blitPalette == null) {
            Class<?> I = Integer.TYPE, B = Boolean.TYPE, BA = byte[].class;
            final Class<?>[] argClasses = {I,
                    BA, I, I, I,
                    I, I, I, I,
                    BA, BA, BA,
                    I, BA, I, I, I,
                    BA, I, I, I,
                    I, I, I, I,
                    I, I, I, B, B};
            blitPalette = AccessController.doPrivileged(
                    (PrivilegedExceptionAction<Method>) () -> {
                        Method method = clazz.
                            getDeclaredMethod("blit", argClasses);
                        method.setAccessible(true);
                        return method;
                    });
        }
        if (blitPalette != null) {
            blitPalette.invoke(clazz, op,
                    srcData, srcDepth, srcStride, srcOrder,
                    srcX, srcY, srcWidth, srcHeight,
                    srcReds, srcGreens, srcBlues,
                    alphaMode, alphaData, alphaStride, alphaX, alphaY,
                    destData, destDepth, destStride, destOrder,
                    destX, destY, destWidth, destHeight,
                    destRedMask, destGreenMask, destBlueMask,
                    flipX, flipY);
        }
    }

    private static Method getByteOrderMethod;
    @SuppressWarnings("removal")
    private static int getByteOrder(ImageData image) throws Exception {
        final Class<?> clazz = ImageData.class;
        if (getByteOrderMethod != null) {
            getByteOrderMethod = AccessController.doPrivileged(
                    (PrivilegedExceptionAction<Method>) () -> {
                        Method method = clazz.getDeclaredMethod("getByteOrder");
                        method.setAccessible(true);
                        return method;
                    });
        }
        if (getByteOrderMethod != null) {
            return (Integer)getByteOrderMethod.invoke(image);
        }
        return MSB_FIRST();
    }

    private static byte[] convertImage(ImageData image) {
        byte[] buffer = null;
        try {
            PaletteData palette = image.palette;
            if (!(((image.depth == 1 || image.depth == 2 ||
                    image.depth == 4 || image.depth == 8) &&
                    !palette.isDirect) ||
                    ((image.depth == 8) || (image.depth == 16 ||
                    image.depth == 24 || image.depth == 32)
                    && palette.isDirect))) {
                    return null;
            }

            final int BLIT_SRC = BLIT_SRC();
            final int ALPHA_OPAQUE = ALPHA_OPAQUE();
            final int MSB_FIRST = MSB_FIRST();

            int width = image.width;
            int height = image.height;
            int byteOrder = getByteOrder(image);
            int ao = 3;
            int redMask = 0xFF00;
            int greenMask = 0xFF0000;
            int blueMask = 0xFF000000;
            int dataSize = width * height * 4;
            int bpr = width * 4;
            buffer = new byte[dataSize];

            if (palette.isDirect) {
                blit(BLIT_SRC,
                    image.data, image.depth, image.bytesPerLine, byteOrder,
                    0, 0, width, height,
                    palette.redMask, palette.greenMask, palette.blueMask,
                    ALPHA_OPAQUE, null, 0, 0, 0,
                    buffer, 32, bpr, MSB_FIRST, 0, 0, width, height,
                    redMask, greenMask, blueMask,
                    false, false);
            } else {
                RGB[] rgbs = palette.getRGBs();
                int length = rgbs.length;
                byte[] srcReds = new byte[length];
                byte[] srcGreens = new byte[length];
                byte[] srcBlues = new byte[length];
                for (int i = 0; i < rgbs.length; i++) {
                    RGB rgb = rgbs[i];
                    if (rgb == null) continue;
                    srcReds[i] = (byte)rgb.red;
                    srcGreens[i] = (byte)rgb.green;
                    srcBlues[i] = (byte)rgb.blue;
                }
                blit(BLIT_SRC,
                    image.data, image.depth, image.bytesPerLine, byteOrder,
                    0, 0, width, height, srcReds, srcGreens, srcBlues,
                    ALPHA_OPAQUE, null, 0, 0, 0,
                    buffer, 32, bpr, MSB_FIRST, 0, 0, width, height,
                    redMask, greenMask, blueMask,
                    false, false);
            }

            /* Initialize transparency */
            int transparency = image.getTransparencyType();
            boolean hasAlpha = transparency != SWT.TRANSPARENCY_NONE;
            if (transparency == SWT.TRANSPARENCY_MASK ||
                image.transparentPixel != -1) {
                ImageData maskImage = image.getTransparencyMask();
                byte[] maskData = maskImage.data;
                int maskBpl = maskImage.bytesPerLine;
                int offset = 0, maskOffset = 0;
                for (int y = 0; y<height; y++) {
                    for (int x = 0; x<width; x++) {
                        int m = maskData[maskOffset + (x >> 3)];
                        int v = 1 << (7 - (x & 0x7));
                        buffer[offset + ao] = (m & v) != 0 ? (byte)0xff : 0;
                        offset += 4;
                    }
                    maskOffset += maskBpl;
                }
            } else {
                if (image.alpha != -1) {
                    hasAlpha = true;
                    int alpha = image.alpha;
                    byte a = (byte)alpha;
                    for (int offset=0; offset<buffer.length; offset+=4) {
                        buffer[offset + ao] = a;
                    }
                } else if (image.alphaData != null) {
                    hasAlpha = true;
                    byte[] alphaData = new byte[image.alphaData.length];
                    System.arraycopy(image.alphaData, 0,
                                     alphaData, 0, alphaData.length);
                    int offset = 0, alphaOffset = 0;
                    for (int y = 0; y<height; y++) {
                        for (int x = 0; x<width; x++) {
                            buffer[offset + ao] = alphaData[alphaOffset];
                            offset += 4;
                            alphaOffset += 1;
                        }
                    }
                }
            }
            if (!hasAlpha) {
                for (int offset=0; offset<buffer.length; offset+=4) {
                    buffer[offset + ao] = (byte)0xFF;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return buffer;
    }
}
