/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.tk.ImageLoader;
import com.sun.javafx.tk.PlatformImage;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.NamedArg;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.paint.Color;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

/**
 * The {@code WritableImage} class represents a custom graphical image
 * that is constructed from pixels supplied by the application, and possibly
 * from {@code PixelReader} objects from any number of sources, including
 * images read from a file or URL.
 * @since JavaFX 2.2
 */
public class WritableImage extends Image {

    static {
        Toolkit.setWritableImageAccessor(new Toolkit.WritableImageAccessor() {
            @Override public void loadTkImage(WritableImage wimg, Object loader) {
                wimg.loadTkImage(loader);
            }

            @Override public Object getTkImageLoader(WritableImage wimg) {
                return wimg.getTkImageLoader();
            }
        });
    }

    private ImageLoader tkImageLoader;

    /**
     * Constructs an empty image of the specified dimensions.
     * The image will initially be filled with transparent pixels.
     * Images constructed this way will always be readable and writable
     * so the corresponding getPixelReader() and getPixelWriter() will
     * always return valid objects.
     * The dimensions must both be positive numbers <code>(&gt;&nbsp;0)</code>.
     *
     * @param width the desired width of the writable image
     * @param height the desired height of the desired image
     * @throws IllegalArgumentException if either dimension is negative or zero.
     */
    public WritableImage(@NamedArg("width") int width, @NamedArg("height") int height) {
        super(width, height);
    }

    private PixelBuffer<? extends Buffer> pixelBuffer = null;

    /**
     * Constructs a {@code WritableImage} using the specified {@code PixelBuffer}.
     * The {@code Buffer} provided by the {@code PixelBuffer} will be used
     * directly as the pixel data for this image.
     * The {@code PixelBuffer} can be shared by multiple {@code WritableImage}s.
     * Images constructed this way are readable using {@code Image.getPixelReader()},
     * but are not writable using {@code WritableImage.getPixelWriter()}.
     *
     * @param pixelBuffer the {@code PixelBuffer} used to construct this image
     * @throws NullPointerException if {@code pixelBuffer} is {@code null}
     * @since 13
     */
    public WritableImage(@NamedArg("PixelBuffer") PixelBuffer<? extends Buffer> pixelBuffer) {
        super(validatePixelBuffer(pixelBuffer));
        pixelBuffer.addImage(this);
        this.pixelBuffer = pixelBuffer;
    }

    /**
     * Constructs an image of the specified dimensions, initialized from
     * the indicated {@link PixelReader}.
     * The image will initially be filled with data returned from the
     * {@code PixelReader}.
     * If the {@code PixelReader} accesses a surface that does not contain
     * the necessary number of pixel rows and columns then an
     * {@link ArrayIndexOutOfBoundsException} will be thrown.
     * Images constructed this way will always be readable and writable
     * so the corresponding getPixelReader() and getPixelWriter() will
     * always return valid objects.
     * The dimensions must both be positive numbers <code>(&gt;&nbsp;0)</code>.
     *
     * @param reader the {@code PixelReader} to construct from
     * @param width the desired width of the writable image and the
     *        width of the region to be read from the {@code reader}
     * @param height the desired height of the desired image and the
     *        width of the region to be read from the {@code reader}
     * @throws ArrayIndexOutOfBoundsException if the {@code reader} does
     *         not access a surface of at least the requested dimensions
     * @throws IllegalArgumentException if either dimension is negative or zero.
     */
    public WritableImage(@NamedArg("reader") PixelReader reader, @NamedArg("width") int width, @NamedArg("height") int height) {
        super(width, height);
        getPixelWriter().setPixels(0, 0, width, height, reader, 0, 0);
    }

    /**
     * Constructs an image of the specified dimensions, initialized from
     * the indicated region of the {@link PixelReader}.
     * The image will initially be filled with data returned from the
     * {@code PixelReader} for the specified region.
     * If the {@code PixelReader} accesses a surface that does not contain
     * the necessary number of pixel rows and columns then an
     * {@link ArrayIndexOutOfBoundsException} will be thrown.
     * Images constructed this way will always be readable and writable
     * so the corresponding getPixelReader() and getPixelWriter() will
     * always return valid objects.
     * The dimensions must both be positive numbers <code>(&gt;&nbsp;0)</code>.
     *
     * @param reader the {@code PixelReader} to construct from
     * @param x the X coordinate of the upper left corner of the region to
     *        read from the {@code reader}
     * @param y the Y coordinate of the upper left corner of the region to
     *        read from the {@code reader}
     * @param width the desired width of the writable image and the
     *        width of the region to be read from the {@code reader}
     * @param height the desired height of the desired image and the
     *        width of the region to be read from the {@code reader}
     * @throws ArrayIndexOutOfBoundsException if the {@code reader} does
     *         not access a surface containing at least the indicated region
     * @throws IllegalArgumentException if either dimension is negative or zero.
     */
    public WritableImage(@NamedArg("reader") PixelReader reader,
                         @NamedArg("x") int x, @NamedArg("y") int y, @NamedArg("width") int width, @NamedArg("height") int height)
    {
        super(width, height);
        getPixelWriter().setPixels(0, 0, width, height, reader, x, y);
    }

    @Override
    boolean isAnimation() {
        return true;
    }

    @Override
    boolean pixelsReadable() {
        return true;
    }

    void bufferDirty(Rectangle rect) {
        getWritablePlatformImage().bufferDirty(rect);
        pixelsDirty();
    }

    private static PixelBuffer<? extends Buffer> validatePixelBuffer(PixelBuffer<? extends Buffer> pixelBuffer) {
        return (Objects.requireNonNull(pixelBuffer, "pixelBuffer must not be null."));
    }

    private PixelWriter writer;
    /**
     * This method returns a {@code PixelWriter} that provides access to
     * write the pixels of the image. This method is not supported for
     * images constructed using a {@code PixelBuffer}.
     *
     * @return the {@code PixelWriter} for writing pixels to the image
     * @throws UnsupportedOperationException if this image was created using a {@code PixelBuffer}
     */
    public final PixelWriter getPixelWriter() {
        if (pixelBuffer != null) {
            throw new UnsupportedOperationException("PixelWriter is not supported with PixelBuffer");
        }
        if (getProgress() < 1.0 || isError()) {
            return null;
        }
        if (writer == null) {
            writer = new PixelWriter() {
                ReadOnlyObjectProperty<PlatformImage> pimgprop =
                    acc_platformImageProperty();

                @Override
                public PixelFormat getPixelFormat() {
                    PlatformImage pimg = getWritablePlatformImage();
                    return pimg.getPlatformPixelFormat();
                }

                @Override
                public void setArgb(int x, int y, int argb) {
                    getWritablePlatformImage().setArgb(x, y, argb);
                    pixelsDirty();
                }

                @Override
                public void setColor(int x, int y, Color c) {
                    if (c == null) throw new NullPointerException("Color cannot be null");
                    int a = (int) Math.round(c.getOpacity() * 255);
                    int r = (int) Math.round(c.getRed()     * 255);
                    int g = (int) Math.round(c.getGreen()   * 255);
                    int b = (int) Math.round(c.getBlue()    * 255);
                    setArgb(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                }

                @Override
                public <T extends Buffer>
                    void setPixels(int x, int y, int w, int h,
                                   PixelFormat<T> pixelformat,
                                   T buffer, int scanlineStride)
                {
                    if (pixelformat == null) throw new NullPointerException("PixelFormat cannot be null");
                    if (buffer == null) throw new NullPointerException("Buffer cannot be null");
                    PlatformImage pimg = getWritablePlatformImage();
                    pimg.setPixels(x, y, w, h, pixelformat,
                                   buffer, scanlineStride);
                    pixelsDirty();
                }

                @Override
                public void setPixels(int x, int y, int w, int h,
                                      PixelFormat<ByteBuffer> pixelformat,
                                      byte buffer[], int offset, int scanlineStride)
                {
                    if (pixelformat == null) throw new NullPointerException("PixelFormat cannot be null");
                    if (buffer == null) throw new NullPointerException("Buffer cannot be null");
                    PlatformImage pimg = getWritablePlatformImage();
                    pimg.setPixels(x, y, w, h, pixelformat,
                                   buffer, offset, scanlineStride);
                    pixelsDirty();
                }

                @Override
                public void setPixels(int x, int y, int w, int h,
                                      PixelFormat<IntBuffer> pixelformat,
                                      int buffer[], int offset, int scanlineStride)
                {
                    if (pixelformat == null) throw new NullPointerException("PixelFormat cannot be null");
                    if (buffer == null) throw new NullPointerException("Buffer cannot be null");
                    PlatformImage pimg = getWritablePlatformImage();
                    pimg.setPixels(x, y, w, h, pixelformat,
                                   buffer, offset, scanlineStride);
                    pixelsDirty();
                }

                @Override
                public void setPixels(int writex, int writey, int w, int h,
                                      PixelReader reader, int readx, int ready)
                {
                    if (reader == null) throw new NullPointerException("Reader cannot be null");
                    PlatformImage pimg = getWritablePlatformImage();
                    pimg.setPixels(writex, writey, w, h, reader, readx, ready);
                    pixelsDirty();
                }
            };
        }
        return writer;
    }

    private void loadTkImage(Object loader) {
        if (!(loader instanceof ImageLoader)) {
            throw new IllegalArgumentException("Unrecognized image loader: "
                    + loader);
        }
        ImageLoader tkLoader = (ImageLoader)loader;
        if (tkLoader.getWidth() != (int)this.getWidth()
                || tkLoader.getHeight() != (int)this.getHeight())
        {
            throw new IllegalArgumentException("Size of loader does not match size of image");
        }

        super.setPlatformImage(tkLoader.getFrame(0));
        this.tkImageLoader = tkLoader;
    }

    private Object getTkImageLoader() {
        return tkImageLoader;
    }
}
