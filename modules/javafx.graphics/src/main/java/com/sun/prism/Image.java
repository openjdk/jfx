/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism;

import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;
import javafx.util.Pair;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageStorage;
import com.sun.javafx.image.BytePixelGetter;
import com.sun.javafx.image.BytePixelSetter;
import com.sun.javafx.image.ByteToBytePixelConverter;
import com.sun.javafx.image.ByteToIntPixelConverter;
import com.sun.javafx.image.IntPixelGetter;
import com.sun.javafx.image.IntPixelSetter;
import com.sun.javafx.image.IntToBytePixelConverter;
import com.sun.javafx.image.IntToIntPixelConverter;
import com.sun.javafx.image.PixelConverter;
import com.sun.javafx.image.PixelGetter;
import com.sun.javafx.image.PixelSetter;
import com.sun.javafx.image.PixelUtils;
import com.sun.javafx.image.impl.ByteBgra;
import com.sun.javafx.image.impl.ByteBgraPre;
import com.sun.javafx.image.impl.ByteGray;
import com.sun.javafx.image.impl.ByteGrayAlpha;
import com.sun.javafx.image.impl.ByteGrayAlphaPre;
import com.sun.javafx.image.impl.ByteRgb;
import com.sun.javafx.image.impl.ByteRgba;
import com.sun.javafx.tk.PlatformImage;
import com.sun.prism.impl.BufferUtil;

public class Image implements PlatformImage {
    static final javafx.scene.image.WritablePixelFormat<ByteBuffer> FX_ByteBgraPre_FORMAT =
        javafx.scene.image.PixelFormat.getByteBgraPreInstance();
    static final javafx.scene.image.WritablePixelFormat<IntBuffer> FX_IntArgbPre_FORMAT =
        javafx.scene.image.PixelFormat.getIntArgbPreInstance();
    static final javafx.scene.image.PixelFormat<ByteBuffer> FX_ByteRgb_FORMAT =
        javafx.scene.image.PixelFormat.getByteRgbInstance();

    private final Buffer pixelBuffer;
    private final int minX;
    private final int minY;
    private final int width;
    private final int height;
    private final int scanlineStride;
    private final PixelFormat pixelFormat;
    private final float pixelScale;
    private Serial serial = new Serial();

    public static Image fromIntArgbPreData(int[] pixels, int width, int height) {
        return new Image(PixelFormat.INT_ARGB_PRE, pixels, width, height);
    }

    public static Image fromIntArgbPreData(IntBuffer pixels, int width, int height) {
        return new Image(PixelFormat.INT_ARGB_PRE, pixels, width, height);
    }

    public static Image fromIntArgbPreData(IntBuffer pixels, int width, int height, int scanlineStride) {
        return new Image(PixelFormat.INT_ARGB_PRE, pixels, width, height, 0, 0, scanlineStride);
    }

    public static Image fromIntArgbPreData(IntBuffer pixels,
                                           int width, int height, int scanlineStride,
                                           float pixelScale)
    {
        return new Image(PixelFormat.INT_ARGB_PRE, pixels,
                         width, height, 0, 0, scanlineStride,
                         pixelScale);
    }

    public static Image fromByteBgraPreData(byte[] pixels, int width, int height) {
        return new Image(PixelFormat.BYTE_BGRA_PRE, pixels, width, height);
    }

    public static Image fromByteBgraPreData(byte[] pixels,
                                            int width, int height,
                                            float pixelScale)
    {
        return new Image(PixelFormat.BYTE_BGRA_PRE, ByteBuffer.wrap(pixels),
                         width, height, 0, 0, 0,
                         pixelScale);
    }

    public static Image fromByteBgraPreData(ByteBuffer pixels, int width, int height) {
        return new Image(PixelFormat.BYTE_BGRA_PRE, pixels, width, height);
    }

    public static Image fromPixelBufferPreData(PixelFormat pf, Buffer pixels, int width, int height) {
        return new Image(pf, pixels, width, height);
    }

    public static Image fromByteBgraPreData(ByteBuffer pixels, int width, int height, int scanlineStride) {
        return new Image(PixelFormat.BYTE_BGRA_PRE, pixels, width, height, 0, 0, scanlineStride);
    }

    public static Image fromByteBgraPreData(ByteBuffer pixels,
                                            int width, int height, int scanlineStride,
                                            float pixelScale)
    {
        return new Image(PixelFormat.BYTE_BGRA_PRE, pixels,
                         width, height, 0, 0, scanlineStride,
                         pixelScale);
    }

    public static Image fromByteRgbData(byte[] pixels, int width, int height) {
        return new Image(PixelFormat.BYTE_RGB, pixels, width, height);
    }

    public static Image fromByteRgbData(ByteBuffer pixels, int width, int height) {
        return new Image(PixelFormat.BYTE_RGB, pixels, width, height);
    }

    public static Image fromByteRgbData(ByteBuffer pixels, int width, int height, int scanlineStride) {
        return new Image(PixelFormat.BYTE_RGB, pixels, width, height, 0, 0, scanlineStride);
    }

    public static Image fromByteRgbData(ByteBuffer pixels,
                                        int width, int height, int scanlineStride,
                                        float pixelScale)
    {
        return new Image(PixelFormat.BYTE_RGB, pixels,
                         width, height, 0, 0, scanlineStride,
                         pixelScale);
    }

    public static Image fromByteGrayData(byte[] pixels, int width, int height) {
        return new Image(PixelFormat.BYTE_GRAY, pixels, width, height);
    }

    public static Image fromByteGrayData(ByteBuffer pixels, int width, int height) {
        return new Image(PixelFormat.BYTE_GRAY, pixels, width, height);
    }

    public static Image fromByteGrayData(ByteBuffer pixels, int width, int height, int scanlineStride) {
        return new Image(PixelFormat.BYTE_GRAY, pixels, width, height, 0, 0, scanlineStride);
    }

    public static Image fromByteGrayData(ByteBuffer pixels,
                                         int width, int height, int scanlineStride,
                                         float pixelScale)
    {
        return new Image(PixelFormat.BYTE_GRAY, pixels,
                         width, height, 0, 0, scanlineStride,
                         pixelScale);
    }

    public static Image fromByteAlphaData(byte[] pixels, int width, int height) {
        return new Image(PixelFormat.BYTE_ALPHA, pixels, width, height);
    }

    public static Image fromByteAlphaData(ByteBuffer pixels, int width, int height) {
        return new Image(PixelFormat.BYTE_ALPHA, pixels, width, height);
    }

    public static Image fromByteAlphaData(ByteBuffer pixels, int width, int height, int scanlineStride) {
        return new Image(PixelFormat.BYTE_ALPHA, pixels, width, height, 0, 0, scanlineStride);
    }

    public static Image fromByteApple422Data(byte[] pixels, int width, int height) {
        return new Image(PixelFormat.BYTE_APPLE_422, pixels, width, height);
    }

    public static Image fromByteApple422Data(ByteBuffer pixels, int width, int height) {
        return new Image(PixelFormat.BYTE_APPLE_422, pixels, width, height);
    }

    public static Image fromByteApple422Data(ByteBuffer pixels, int width, int height, int scanlineStride) {
        return new Image(PixelFormat.BYTE_APPLE_422, pixels, width, height, 0, 0, scanlineStride);
    }

    public static Image fromFloatMapData(FloatBuffer pixels, int width, int height) {
        return new Image(PixelFormat.FLOAT_XYZW, pixels, width, height);
    }

    /*
     * This method wraps ImageFrame data to com.sum.prism.Image.
     * The data buffer will be shared between objects.
     * It does not duplicate the memory, except in L8A8 case.
     * If it necessary, it does in-place format conversion like RGBA->BGRA
     *
     * @param frame ImageFrame to convert.
     * @return New Image instance.
     */
    public static Image convertImageFrame(ImageFrame frame) {
        ByteBuffer buffer = (ByteBuffer) frame.getImageData();
        ImageStorage.ImageType type = frame.getImageType();
        int w = frame.getWidth(), h = frame.getHeight();
        int scanBytes = frame.getStride();
        float ps = frame.getPixelScale();

        switch (type) {
            case GRAY:
                return Image.fromByteGrayData(buffer, w, h, scanBytes, ps);

            case RGB:
                return Image.fromByteRgbData(buffer, w, h, scanBytes, ps);

            case RGBA:
                // Bgra => BgrePre is same operation as Rgba => RgbaPre
                // TODO: 3D - need a way to handle pre versus non-Pre
                ByteBgra.ToByteBgraPreConverter().convert(buffer, 0, scanBytes,
                                                          buffer, 0, scanBytes,
                                                          w, h);
                /* NOBREAK */
            case RGBA_PRE:
                ByteRgba.ToByteBgraConverter().convert(buffer, 0, scanBytes,
                                                       buffer, 0, scanBytes,
                                                       w, h);
                return Image.fromByteBgraPreData(buffer, w, h, scanBytes, ps);

            case GRAY_ALPHA:
                // TODO: 3D - need a way to handle pre versus non-Pre
                ByteGrayAlpha.ToByteGrayAlphaPreConverter().convert(buffer, 0, scanBytes,
                                                                    buffer, 0, scanBytes,
                                                                    w, h);
                /* NOBREAK */
            case GRAY_ALPHA_PRE:
                if (scanBytes != w * 2) {
                    throw new AssertionError("Bad stride for GRAY_ALPHA");
                };
                byte newbuf[] = new byte[w * h * 4];
                ByteGrayAlphaPre.ToByteBgraPreConverter().convert(buffer, 0, scanBytes,
                                                                  newbuf, 0, w*4,
                                                                  w, h);
                return Image.fromByteBgraPreData(newbuf, w, h, ps);
            default:
                throw new RuntimeException("Unknown image type: " + type);
        }
    }

    private Image(PixelFormat pixelFormat, int[] pixels,
                  int width, int height)
    {
        this(pixelFormat, IntBuffer.wrap(pixels), width, height, 0, 0, 0, 1.0f);
    }

    private Image(PixelFormat pixelFormat, byte[] pixels,
                  int width, int height)
    {
        this(pixelFormat, ByteBuffer.wrap(pixels), width, height, 0, 0, 0, 1.0f);
    }

    private Image(PixelFormat pixelFormat, Buffer pixelBuffer,
                  int width, int height)
    {
        this(pixelFormat, pixelBuffer, width, height, 0, 0, 0, 1.0f);
    }

    private Image(PixelFormat pixelFormat, Buffer pixelBuffer,
                  int width, int height, int minX, int minY, int scanlineStride)
    {
        this(pixelFormat, pixelBuffer, width, height, minX, minY, scanlineStride, 1.0f);
    }

    private Image(PixelFormat pixelFormat, Buffer pixelBuffer,
                  int width, int height, int minX, int minY,
                  int scanlineStride, float pixelScale)
    {
        if (pixelFormat == PixelFormat.MULTI_YCbCr_420) {
            throw new IllegalArgumentException("Format not supported "+pixelFormat.name());
        }
        if (scanlineStride == 0) {
            scanlineStride = width * pixelFormat.getBytesPerPixelUnit();
        }

        if (pixelBuffer == null) {
            throw new IllegalArgumentException("Pixel buffer must be non-null");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image dimensions must be > 0");
        }
        if (minX < 0 || minY < 0) {
            throw new IllegalArgumentException("Image minX and minY must be >= 0");
        }
        if (((minX+width)*pixelFormat.getBytesPerPixelUnit()) > scanlineStride) {
            throw new IllegalArgumentException("Image scanlineStride is too small");
        }
        if (scanlineStride % pixelFormat.getBytesPerPixelUnit() != 0) {
            throw new IllegalArgumentException(
                "Image scanlineStride must be a multiple of the pixel stride");
        }
        this.pixelFormat = pixelFormat;
        this.pixelBuffer = pixelBuffer;
        this.width = width;
        this.height = height;
        this.minX = minX;
        this.minY = minY;
        this.scanlineStride = scanlineStride;
        this.pixelScale = pixelScale;
    }

    public PixelFormat getPixelFormat() {
        return pixelFormat;
    }

    public PixelFormat.DataType getDataType() {
        return pixelFormat.getDataType();
    }

    public int getBytesPerPixelUnit() {
        return pixelFormat.getBytesPerPixelUnit();
    }

    public Buffer getPixelBuffer() {
        return pixelBuffer;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getScanlineStride() {
        return scanlineStride;
    }

    @Override
    public float getPixelScale() {
        return pixelScale;
    }

    public int getRowLength() {
        // Note that the constructor ensures that scanlineStride is a
        // multiple of pixelStride, so the following should be safe
        return scanlineStride / pixelFormat.getBytesPerPixelUnit();
    }

    public boolean isTightlyPacked() {
        return minX == 0 && minY == 0 && width == getRowLength();
    }

    /**
     * Returns a new {@code Image} instance that shares the underlying pixel
     * buffer of this {@code Image}.  The new image will have the same
     * scanline stride, pixel format, etc of the original image, except
     * with the provided minX/minY and dimensions.
     *
     * @param x the x offset of the upper-left corner of the new subimage,
     * relative to the minX of this image
     * @param y the y offset of the upper-left corner of the new subimage,
     * relative to the minY of this image
     * @param w the width of the new subimage
     * @param h the height of the new subimage
     * @return a new {@code Image} representing a sub-region of this image
     */
    public Image createSubImage(int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Subimage dimensions must be > 0");
        }
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Subimage minX and minY must be >= 0");
        }
        if (x+w > this.width) {
            throw new IllegalArgumentException(
                "Subimage minX+width must be <= width of parent image");
        }
        if (y+h > this.height) {
            throw new IllegalArgumentException(
                "Subimage minY+height must be <= height of parent image");
        }
        Image subimg = new Image(pixelFormat, pixelBuffer, w, h,
                                 minX+x, minY+y, scanlineStride);
        subimg.serial = this.serial;
        return subimg;
    }

    /**
     * Returns a new {@code Image} instance with a newly allocated pixel
     * buffer that contains a tightly packed copy of this image's pixels.
     * This method is useful in cases where having extra pixels at the
     * end of a scanline is not desirable.  The new image will have the same
     * pixel format, width, and height of the original image, except with
     * a new scanline stride and with {@code minX == minY == 0}.
     *
     * @return a new {@code Image} this is a tightly packed copy of this image
     */
    public Image createPackedCopy() {
        int newBytesPerRow = width * pixelFormat.getBytesPerPixelUnit();
        Buffer newBuf = createPackedBuffer(pixelBuffer, pixelFormat,
                                           minX, minY, width, height,
                                           scanlineStride);
        return new Image(pixelFormat, newBuf, width, height, 0, 0, newBytesPerRow);
    }

    /**
     * Returns a {@code Image} instance with a newly allocated pixel
     * buffer that contains a tightly packed copy of this image's pixels or
     * if this image is already tightly packed, itself.
     *
     * @see #createPackedCopy()
     * @return a new {@code Image} this is a tightly packed copy of this image
     * or itself if this image is packed already.
     */
    public Image createPackedCopyIfNeeded() {
        int newBytesPerRow = width * pixelFormat.getBytesPerPixelUnit();
        // if the image is packed already, return itself
        if (newBytesPerRow == scanlineStride && minX == 0 && minY == 0) {
            return this;
        }
        return createPackedCopy();
    }

    /**
     * Returns a new {@code Buffer} instance that contains a tightly packed
     * copy of the given {@code Buffer}'s pixel data.  This method is useful
     * in cases where having extra pixels at the end of a scanline is not
     * desirable.
     *
     * @param pixels the buffer containing the pixels to copy
     * @param format the format of the given buffer
     * @param minX the x offset of the upper-left corner of the pixel region
     * @param minY the y offset of the upper-left corner of the pixel region
     * @param width the width of the pixel region to be copied, in pixels
     * @param height the height of the pixel region to be copied, in pixels
     * @param scanlineStride the scanline stride of the given buffer, in bytes
     * @return a new, tightly packed copy of the given {@code Buffer}
     */
    public static Buffer createPackedBuffer(Buffer pixels,
                                            PixelFormat format,
                                            int minX, int minY,
                                            int width, int height,
                                            int scanlineStride)
    {
        if (scanlineStride % format.getBytesPerPixelUnit() != 0) {
            throw new IllegalArgumentException(
                "Image scanlineStride must be a multiple of the pixel stride");
        }
        if (format == PixelFormat.MULTI_YCbCr_420) {
            throw new IllegalArgumentException("Format unsupported "+format);
        }

        int elemsPerPixel = format.getElemsPerPixelUnit();
        int oldRowLength = scanlineStride / format.getBytesPerPixelUnit();
        int oldElemsPerRow = oldRowLength * elemsPerPixel;
        int newElemsPerRow = width * elemsPerPixel;
        int newSizeInElems = newElemsPerRow * height;
        int oldpos = (minX*elemsPerPixel) + (minY*oldElemsPerRow);
        int newpos = 0;

        Buffer newBuf;
        switch (format.getDataType()) {
        case BYTE:
            ByteBuffer oldbbuf = (ByteBuffer)pixels;
            ByteBuffer newbbuf = BufferUtil.newByteBuffer(newSizeInElems);
            for (int y = 0; y < height; y++) {
                oldbbuf.limit(oldpos + newElemsPerRow);
                oldbbuf.position(oldpos);
                newbbuf.limit(newpos + newElemsPerRow);
                newbbuf.position(newpos);
                newbbuf.put(oldbbuf);
                oldpos += oldElemsPerRow;
                newpos += newElemsPerRow;
            }
            newBuf = newbbuf;
            break;
        case INT:
            IntBuffer oldibuf = (IntBuffer)pixels;
            IntBuffer newibuf = BufferUtil.newIntBuffer(newSizeInElems);
            for (int y = 0; y < height; y++) {
                oldibuf.limit(oldpos + newElemsPerRow);
                oldibuf.position(oldpos);
                newibuf.limit(newpos + newElemsPerRow);
                newibuf.position(newpos);
                newibuf.put(oldibuf);
                oldpos += oldElemsPerRow;
                newpos += newElemsPerRow;
            }
            newBuf = newibuf;
            break;
        case FLOAT:
            FloatBuffer oldfbuf = (FloatBuffer)pixels;
            FloatBuffer newfbuf = BufferUtil.newFloatBuffer(newSizeInElems);
            for (int y = 0; y < height; y++) {
                oldfbuf.limit(oldpos + newElemsPerRow);
                oldfbuf.position(oldpos);
                newfbuf.limit(newpos + newElemsPerRow);
                newfbuf.position(newpos);
                newfbuf.put(oldfbuf);
                oldpos += oldElemsPerRow;
                newpos += newElemsPerRow;
            }
            newBuf = newfbuf;
            break;
        default:
            throw new InternalError("Unknown data type");
        }

        pixels.limit(pixels.capacity());
        pixels.rewind();
        newBuf.limit(newBuf.capacity());
        newBuf.rewind();

        return newBuf;
    }

    /*
     * This function is used to create a format that can be used for system icons.
     * It takes the shrunken image's bytebuffer.
     * @return a new INT_ARGB PRE image
     */
    public Image iconify(ByteBuffer iconBuffer, int twidth, int theight) {
        if (pixelFormat == PixelFormat.MULTI_YCbCr_420) {
            throw new IllegalArgumentException("Format not supported "+pixelFormat);
        }

        //grab the number of bytes per pixel, used for determining if
        //the image has alpha
        int tnumBands = this.getBytesPerPixelUnit();

        //compute the new scanlinestride of the small image
        int tscanlineStride = twidth * tnumBands;

        ByteToIntPixelConverter converter;
        if (tnumBands == 1) {
            converter = ByteGray.ToIntArgbPreConverter();
        } else if (pixelFormat == PixelFormat.BYTE_BGRA_PRE) {
            converter = ByteBgraPre.ToIntArgbPreConverter();
        } else { // BYTE_RGB
            converter = ByteRgb.ToIntArgbPreConverter();
        }

        //new int array for holding new int formatted image data
        int[] newImage = new int[twidth*theight];
        converter.convert(iconBuffer, 0, tscanlineStride,
                          newImage, 0, twidth,
                          twidth, theight);

        //returns the new icon image in INT_ARGB_PRE format.
        return new Image(PixelFormat.INT_ARGB_PRE, newImage, twidth, theight);
    }

    @Override
    public String toString() {
        return super.toString()+
            " [format=" + pixelFormat + " width=" + width + " height=" + height+
            " scanlineStride=" + scanlineStride +
            " minX=" + minX + " minY=" + minY +
            " pixelBuffer=" + pixelBuffer +
            " bpp=" + getBytesPerPixelUnit() + "]";
    }

    public Serial getSerial() {
        return serial;
    }

    private void updateSerial() {
        updateSerial(null);
    }

    private void updateSerial(Rectangle rect) {
        serial.update(rect);
    }

    public static class Serial {
        private int id;
        private Rectangle dirtyRegion;

        Serial() {
            id = 0;
            dirtyRegion = null;
        }

        public synchronized Pair<Integer, Rectangle> getIdRect() {
            // Called on quantumRenderer-0
            return new Pair(id, (dirtyRegion == null)? null : new Rectangle(dirtyRegion));
        }

        public synchronized void update(Rectangle rect) {
            // Called on FX Application thread
            id++;
            dirtyRegion = rect;
        }
    }

    public Image promoteByteRgbToByteBgra() {
        ByteBuffer oldbuf = (ByteBuffer) pixelBuffer;
        ByteBuffer newbuf = ByteBuffer.allocate(width * height * 4);
        int oldpos = minY * scanlineStride + minX * 3;
        ByteRgb.ToByteBgraPreConverter().convert(oldbuf, oldpos, scanlineStride,
                                                 newbuf, 0, width * 4,
                                                 width, height);
        return new Image(PixelFormat.BYTE_BGRA_PRE, newbuf,
                         width, height, 0, 0, width * 4, getPixelScale());
    }

    private Accessor<?> pixelaccessor;
    private Accessor<?> getPixelAccessor() {
        if (pixelaccessor == null) {
            switch (getPixelFormat()) {
                case BYTE_ALPHA:
                case BYTE_APPLE_422:
                case FLOAT_XYZW:
                case MULTI_YCbCr_420:
                default:
                    pixelaccessor = new UnsupportedAccess();
                    break;
                case BYTE_GRAY:
                    pixelaccessor = new ByteAccess(getGrayFXPixelFormat(),
                                                   ByteGray.getter, null,
                                                   (ByteBuffer) pixelBuffer, 1);
                    break;
                case BYTE_RGB:
                    pixelaccessor = new ByteRgbAccess((ByteBuffer) pixelBuffer);
                    break;
                case BYTE_BGRA_PRE:
                    pixelaccessor = new ByteAccess(FX_ByteBgraPre_FORMAT,
                                                   (ByteBuffer) pixelBuffer, 4);
                    break;
                case INT_ARGB_PRE:
                    pixelaccessor = new IntAccess(FX_IntArgbPre_FORMAT,
                                                  (IntBuffer) pixelBuffer);
                    break;
            }
            if (pixelScale != 1.0f) {
                pixelaccessor = new ScaledAccessor<>(pixelaccessor, pixelScale);
            }
        }
        return pixelaccessor;
    }

    @Override
    public void bufferDirty(Rectangle rect) {
        updateSerial(rect);
    }

    @Override
    public javafx.scene.image.PixelFormat<?> getPlatformPixelFormat() {
        return getPixelAccessor().getPlatformPixelFormat();
    }

    @Override
    public boolean isWritable() {
        return getPixelAccessor().isWritable();
    }

    @Override
    public PlatformImage promoteToWritableImage() {
        return getPixelAccessor().promoteToWritableImage();
    }

    @Override
    public int getArgb(int x, int y) {
        return getPixelAccessor().getArgb(x, y);
    }

    @Override
    public void setArgb(int x, int y, int argb) {
        getPixelAccessor().setArgb(x, y, argb);
        updateSerial();
    }

    @Override
    public <T extends Buffer>
        void getPixels(int x, int y, int w, int h,
                       javafx.scene.image.WritablePixelFormat<T> pixelformat,
                       T pixels, int scanlineBytes)
    {
        getPixelAccessor().getPixels(x, y, w, h, pixelformat,
                                     pixels, scanlineBytes);
    }

    @Override
    public void getPixels(int x, int y, int w, int h,
                          WritablePixelFormat<ByteBuffer> pixelformat,
                          byte[] pixels, int offset, int scanlineBytes)
    {
        getPixelAccessor().getPixels(x, y, w, h, pixelformat,
                                     pixels, offset, scanlineBytes);
    }

    @Override
    public void getPixels(int x, int y, int w, int h,
                          WritablePixelFormat<IntBuffer> pixelformat,
                          int[] pixels, int offset, int scanlineInts)
    {
        getPixelAccessor().getPixels(x, y, w, h, pixelformat,
                                     pixels, offset, scanlineInts);
    }

    @Override
    public <T extends Buffer>
        void setPixels(int x, int y, int w, int h,
                       javafx.scene.image.PixelFormat<T> pixelformat,
                       T pixels, int scanlineBytes)
    {
        getPixelAccessor().setPixels(x, y, w, h, pixelformat,
                                     pixels, scanlineBytes);
        updateSerial();
    }

    @Override
    public void setPixels(int x, int y, int w, int h,
                          javafx.scene.image.PixelFormat<ByteBuffer> pixelformat,
                          byte[] pixels, int offset, int scanlineBytes)
    {
        getPixelAccessor().setPixels(x, y, w, h, pixelformat,
                                     pixels, offset, scanlineBytes);
        updateSerial();
    }

    @Override
    public void setPixels(int x, int y, int w, int h,
                            javafx.scene.image.PixelFormat<IntBuffer> pixelformat,
                            int[] pixels, int offset, int scanlineInts)
    {
        getPixelAccessor().setPixels(x, y, w, h, pixelformat,
                                     pixels, offset, scanlineInts);
        updateSerial();
    }

    @Override
    public void setPixels(int dstx, int dsty, int w, int h,
                          PixelReader reader, int srcx, int srcy)
    {
        getPixelAccessor().setPixels(dstx, dsty, w, h, reader, srcx, srcy);
        updateSerial();
    }

    public boolean isOpaque() {
        return pixelFormat.isOpaque();
    }

    abstract class Accessor<I extends Buffer> {
        public abstract int getArgb(int x, int y);

        public abstract void setArgb(int x, int y, int argb);

        public abstract javafx.scene.image.PixelFormat<I> getPlatformPixelFormat();

        public abstract boolean isWritable();

        public abstract PlatformImage promoteToWritableImage();

        public abstract <T extends Buffer>
            void getPixels(int x, int y, int w, int h,
                           WritablePixelFormat<T> pixelformat,
                           T pixels, int scanlineElems);

        public abstract
            void getPixels(int x, int y, int w, int h,
                           WritablePixelFormat<ByteBuffer> pixelformat,
                           byte[] pixels, int offset, int scanlineBytes);

        public abstract
            void getPixels(int x, int y, int w, int h,
                           WritablePixelFormat<IntBuffer> pixelformat,
                           int[] pixels, int offset, int scanlineInts);

        public abstract <T extends Buffer>
            void setPixels(int x, int y, int w, int h,
                           javafx.scene.image.PixelFormat<T> pixelformat,
                           T pixels, int scanlineBytes);

        public abstract
            void setPixels(int x, int y, int w, int h,
                           javafx.scene.image.PixelFormat<ByteBuffer> pixelformat,
                           byte[] pixels, int offset, int scanlineBytes);

        public abstract
            void setPixels(int x, int y, int w, int h,
                           javafx.scene.image.PixelFormat<IntBuffer> pixelformat,
                           int[] pixels, int offset, int scanlineInts);

        public abstract
            void setPixels(int dstx, int dsty, int w, int h,
                           PixelReader reader, int srcx, int srcy);
    }

    class ScaledAccessor<I extends Buffer> extends Accessor<I> {
        Accessor<I> theDelegate;
        float pixelScale;

        ScaledAccessor(Accessor<I> delegate, float pixelScale) {
            this.theDelegate = delegate;
            this.pixelScale = pixelScale;
        }

        private int scale(int v) {
            return (int) ((v + 0.5f) * pixelScale);
        }

        @Override
        public int getArgb(int x, int y) {
            return theDelegate.getArgb(scale(x), scale(y));
        }

        @Override
        public void setArgb(int x, int y, int argb) {
            throw new UnsupportedOperationException("Pixel setting for scaled images not supported yet");
//            theDelegate.setArgb(scale(x), scale(y), argb);
        }

        @Override
        public javafx.scene.image.PixelFormat<I> getPlatformPixelFormat() {
            return theDelegate.getPlatformPixelFormat();
        }

        @Override
        public boolean isWritable() {
            return theDelegate.isWritable();
        }

        @Override
        public PlatformImage promoteToWritableImage() {
            throw new UnsupportedOperationException("Pixel setting for scaled images not supported yet");
//            return theDelegate.promoteToWritableImage();
        }

        @Override
        public <T extends Buffer>
            void getPixels(int x, int y, int w, int h,
                           WritablePixelFormat<T> pixelformat,
                           T pixels, int scanlineElems)
        {
            PixelSetter<T> setter = PixelUtils.getSetter(pixelformat);
            int offset = pixels.position();
            int numElem = setter.getNumElements();
            for (int rely = 0; rely < h; rely++) {
                int sy = scale(y + rely);
                int rowoff = offset;
                for (int relx = 0; relx < w; relx++) {
                    int sx = scale(x + relx);
                    setter.setArgb(pixels, rowoff, theDelegate.getArgb(sx, sy));
                    rowoff += numElem;
                }
                offset += scanlineElems;
            }
        }

        @Override
        public void getPixels(int x, int y, int w, int h,
                              WritablePixelFormat<ByteBuffer> pixelformat,
                              byte[] pixels, int offset, int scanlineBytes)
        {
            ByteBuffer bb = ByteBuffer.wrap(pixels);
            bb.position(offset);
            getPixels(x, y, w, h, pixelformat, bb, scanlineBytes);
        }

        @Override
        public void getPixels(int x, int y, int w, int h,
                              WritablePixelFormat<IntBuffer> pixelformat,
                              int[] pixels, int offset, int scanlineInts)
        {
            IntBuffer ib = IntBuffer.wrap(pixels);
            ib.position(offset);
            getPixels(x, y, w, h, pixelformat, ib, scanlineInts);
        }

        @Override
        public <T extends Buffer>
            void setPixels(int x, int y, int w, int h,
                           javafx.scene.image.PixelFormat<T> pixelformat,
                           T pixels, int scanlineElems)
        {
            throw new UnsupportedOperationException("Pixel setting for scaled images not supported yet");
//            PixelGetter<T> getter = PixelUtils.getGetter(pixelformat);
//            int offset = pixels.position();
//            int numElem = getter.getNumElements();
//            for (int rely = 0; rely < h; rely++) {
//                int sy = scale(y + rely);
//                int rowoff = offset;
//                for (int relx = 0; relx < w; relx++) {
//                    int sx = scale(x + relx);
//                    theDelegate.setArgb(sx, sy, getter.getArgb(pixels, rowoff));
//                    rowoff += numElem;
//                }
//                offset += scanlineElems;
//            }
        }

        @Override
        public void setPixels(int x, int y, int w, int h,
                              javafx.scene.image.PixelFormat<ByteBuffer> pixelformat,
                              byte[] pixels, int offset, int scanlineBytes)
        {
            throw new UnsupportedOperationException("Pixel setting for scaled images not supported yet");
        }

        @Override
        public void setPixels(int x, int y, int w, int h,
                              javafx.scene.image.PixelFormat<IntBuffer> pixelformat,
                              int[] pixels, int offset, int scanlineInts)
        {
            throw new UnsupportedOperationException("Pixel setting for scaled images not supported yet");
        }

        @Override
        public void setPixels(int dstx, int dsty, int w, int h,
                              PixelReader reader, int srcx, int srcy)
        {
            throw new UnsupportedOperationException("Pixel setting for scaled images not supported yet");
        }
    }

    static <I extends Buffer> PixelSetter<I>
        getSetterIfWritable(javafx.scene.image.PixelFormat<I> theFormat)
    {
        if (theFormat instanceof WritablePixelFormat) {
            return PixelUtils.getSetter((WritablePixelFormat) theFormat);
        }
        return null;
    }

    abstract class BaseAccessor<I extends Buffer> extends Accessor<I> {
        javafx.scene.image.PixelFormat<I> theFormat;
        PixelGetter<I> theGetter;
        PixelSetter<I> theSetter;
        I theBuffer;
        int pixelElems;
        int scanlineElems;
        int offsetElems;

        BaseAccessor(javafx.scene.image.PixelFormat<I> theFormat, I buffer, int pixelStride) {
            this(theFormat, PixelUtils.getGetter(theFormat), getSetterIfWritable(theFormat),
                 buffer, pixelStride);
        }

        BaseAccessor(javafx.scene.image.PixelFormat<I> theFormat,
                     PixelGetter<I> getter, PixelSetter<I> setter,
                     I buffer, int pixelStride)
        {
            this.theFormat = theFormat;
            this.theGetter = getter;
            this.theSetter = setter;
            this.theBuffer = buffer;
            this.pixelElems = pixelStride;
            this.scanlineElems = scanlineStride / pixelFormat.getDataType().getSizeInBytes();
            this.offsetElems = minY * scanlineElems + minX * pixelStride;
        }

        public int getIndex(int x, int y) {
            if (x < 0 || y < 0 || x >= width || y >= height) {
                throw new IndexOutOfBoundsException(x + ", " + y);
            }
            return offsetElems + y * scanlineElems + x * pixelElems;
        }

        public I getBuffer() {
            return theBuffer;
        }

        public PixelGetter<I> getGetter() {
            if (theGetter == null) {
                throw new UnsupportedOperationException("Unsupported Image type");
            }
            return theGetter;
        }

        public PixelSetter<I> getSetter() {
            if (theSetter == null) {
                throw new UnsupportedOperationException("Unsupported Image type");
            }
            return theSetter;
        }

        @Override
        public javafx.scene.image.PixelFormat<I> getPlatformPixelFormat() {
            return theFormat;
        }

        @Override
        public boolean isWritable() {
            return theSetter != null;
        }

        @Override
        public PlatformImage promoteToWritableImage() {
            return Image.this;
        }

        @Override
        public int getArgb(int x, int y) {
            return getGetter().getArgb(getBuffer(), getIndex(x, y));
        }

        @Override
        public void setArgb(int x, int y, int argb) {
            getSetter().setArgb(getBuffer(), getIndex(x, y), argb);
        }

        @Override
        public <T extends Buffer>
            void getPixels(int x, int y, int w, int h,
                           WritablePixelFormat<T> pixelformat,
                           T dstbuf, int dstScanlineElems)
        {
            PixelSetter<T> setter = PixelUtils.getSetter(pixelformat);
            PixelConverter<I, T> converter =
                PixelUtils.getConverter(getGetter(), setter);
            int dstoff = dstbuf.position();
            converter.convert(getBuffer(), getIndex(x, y), scanlineElems,
                              dstbuf, dstoff, dstScanlineElems,
                              w, h);
        }

        @Override
        public <T extends Buffer>
            void setPixels(int x, int y, int w, int h,
                           javafx.scene.image.PixelFormat<T> pixelformat,
                           T srcbuf, int srcScanlineBytes)
        {
            PixelGetter<T> getter = PixelUtils.getGetter(pixelformat);
            PixelConverter<T, I> converter =
                PixelUtils.getConverter(getter, getSetter());
            int srcoff = srcbuf.position();
            converter.convert(srcbuf, srcoff, srcScanlineBytes,
                              getBuffer(), getIndex(x, y), scanlineElems,
                              w, h);
        }
    }

    class ByteAccess extends BaseAccessor<ByteBuffer> {
        ByteAccess(javafx.scene.image.PixelFormat<ByteBuffer> fmt,
                   PixelGetter<ByteBuffer> getter, PixelSetter<ByteBuffer> setter,
                   ByteBuffer buffer, int numbytes)
        {
            super(fmt, getter, setter, buffer, numbytes);
        }

        ByteAccess(javafx.scene.image.PixelFormat<ByteBuffer> fmt,
                   ByteBuffer buffer, int numbytes)
        {
            super(fmt, buffer, numbytes);
        }

        @Override
        public void getPixels(int x, int y, int w, int h,
                              WritablePixelFormat<ByteBuffer> pixelformat,
                              byte[] dstarr, int dstoff, int dstScanlineBytes)
        {
            BytePixelSetter setter = PixelUtils.getByteSetter(pixelformat);
            ByteToBytePixelConverter b2bconverter =
                PixelUtils.getB2BConverter(getGetter(), setter);
            b2bconverter.convert(getBuffer(), getIndex(x, y), scanlineElems,
                                 dstarr, dstoff, dstScanlineBytes,
                                 w, h);
        }

        @Override
        public void getPixels(int x, int y, int w, int h,
                              WritablePixelFormat<IntBuffer> pixelformat,
                              int[] dstarr, int dstoff, int dstScanlineInts)
        {
            IntPixelSetter setter = PixelUtils.getIntSetter(pixelformat);
            ByteToIntPixelConverter b2iconverter =
                PixelUtils.getB2IConverter(getGetter(), setter);
            b2iconverter.convert(getBuffer(), getIndex(x, y), scanlineElems,
                                 dstarr, dstoff, dstScanlineInts,
                                 w, h);
        }

        @Override
        public void setPixels(int x, int y, int w, int h,
                              javafx.scene.image.PixelFormat<ByteBuffer> pixelformat,
                              byte srcarr[], int srcoff, int srcScanlineBytes)
        {
            BytePixelGetter getter = PixelUtils.getByteGetter(pixelformat);
            ByteToBytePixelConverter b2bconverter =
                PixelUtils.getB2BConverter(getter, getSetter());
            b2bconverter.convert(srcarr, srcoff, srcScanlineBytes,
                                 getBuffer(), getIndex(x, y), scanlineElems,
                                 w, h);
        }

        @Override
        public void setPixels(int x, int y, int w, int h,
                              javafx.scene.image.PixelFormat<IntBuffer> pixelformat,
                              int srcarr[], int srcoff, int srcScanlineInts)
        {
            IntPixelGetter getter = PixelUtils.getIntGetter(pixelformat);
            IntToBytePixelConverter i2bconverter =
                PixelUtils.getI2BConverter(getter, getSetter());
            i2bconverter.convert(srcarr, srcoff, srcScanlineInts,
                                 getBuffer(), getIndex(x, y), scanlineElems,
                                 w, h);
        }

        @Override
        public void setPixels(int dstx, int dsty, int w, int h,
                              PixelReader reader, int srcx, int srcy) {
            ByteBuffer b = theBuffer.duplicate();
            b.position(b.position() + getIndex(dstx, dsty));
            reader.getPixels(srcx, srcy, w, h,
                             (WritablePixelFormat) theFormat,
                             b, scanlineElems);
        }
    }

    class IntAccess extends BaseAccessor<IntBuffer> {
        IntAccess(javafx.scene.image.PixelFormat<IntBuffer> fmt, IntBuffer buffer) {
            super(fmt, buffer, 1);
        }

        @Override
        public void getPixels(int x, int y, int w, int h,
                              WritablePixelFormat<ByteBuffer> pixelformat,
                              byte dstarr[], int dstoff, int dstScanlineBytes)
        {
            BytePixelSetter setter = PixelUtils.getByteSetter(pixelformat);
            IntToBytePixelConverter i2bconverter =
                PixelUtils.getI2BConverter(getGetter(), setter);
            i2bconverter.convert(getBuffer(), getIndex(x, y), scanlineElems,
                                 dstarr, dstoff, dstScanlineBytes,
                                 w, h);
        }

        @Override
        public void getPixels(int x, int y, int w, int h,
                              WritablePixelFormat<IntBuffer> pixelformat,
                              int dstarr[], int dstoff, int dstScanlineInts)
        {
            IntPixelSetter setter = PixelUtils.getIntSetter(pixelformat);
            IntToIntPixelConverter i2iconverter =
                PixelUtils.getI2IConverter(getGetter(), setter);
            i2iconverter.convert(getBuffer(), getIndex(x, y), scanlineElems,
                                 dstarr, dstoff, dstScanlineInts,
                                 w, h);
        }

        @Override
        public void setPixels(int x, int y, int w, int h,
                              javafx.scene.image.PixelFormat<ByteBuffer> pixelformat,
                              byte srcarr[], int srcoff, int srcScanlineBytes)
        {
            BytePixelGetter getter = PixelUtils.getByteGetter(pixelformat);
            ByteToIntPixelConverter b2iconverter =
                PixelUtils.getB2IConverter(getter, getSetter());
            b2iconverter.convert(srcarr, srcoff, srcScanlineBytes,
                                 getBuffer(), getIndex(x, y), scanlineElems,
                                 w, h);
        }

        @Override
        public void setPixels(int x, int y, int w, int h,
                              javafx.scene.image.PixelFormat<IntBuffer> pixelformat,
                              int srcarr[], int srcoff, int srcScanlineInts)
        {
            IntPixelGetter getter = PixelUtils.getIntGetter(pixelformat);
            IntToIntPixelConverter i2iconverter =
                PixelUtils.getI2IConverter(getter, getSetter());
            i2iconverter.convert(srcarr, srcoff, srcScanlineInts,
                                 getBuffer(), getIndex(x, y), scanlineElems,
                                 w, h);
        }

        @Override
        public void setPixels(int dstx, int dsty, int w, int h,
                              PixelReader reader, int srcx, int srcy) {
            IntBuffer b = theBuffer.duplicate();
            b.position(b.position() + getIndex(dstx, dsty));
            reader.getPixels(srcx, srcy, w, h,
                             (WritablePixelFormat) theFormat,
                             b, scanlineElems);
        }
    }

    static javafx.scene.image.PixelFormat<ByteBuffer> FX_ByteGray_FORMAT;
    static javafx.scene.image.PixelFormat<ByteBuffer> getGrayFXPixelFormat() {
        if (FX_ByteGray_FORMAT == null) {
            int grays[] = new int[256];
            int gray = 0xff000000;
            for (int i = 0; i < 256; i++) {
                grays[i] = gray;
                gray += 0x00010101;
            }
            FX_ByteGray_FORMAT =
                javafx.scene.image.PixelFormat.createByteIndexedPremultipliedInstance(grays);
        }
        return FX_ByteGray_FORMAT;
    }

    class UnsupportedAccess extends ByteAccess {
        private UnsupportedAccess() {
            super(null, null, null, null, 0);
        }
    }

    class ByteRgbAccess extends ByteAccess {
        public ByteRgbAccess(ByteBuffer buffer) {
            super(FX_ByteRgb_FORMAT, buffer, 3);
        }

        @Override
        public PlatformImage promoteToWritableImage() {
            return promoteByteRgbToByteBgra();
        }
    }
}
