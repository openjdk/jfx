/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.tk.Toolkit;
import javafx.geometry.Rectangle2D;
import javafx.util.Callback;

import java.lang.ref.WeakReference;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * The {@code PixelBuffer} class represents pixel data that is constructed from
 * a {@link Buffer java.nio.Buffer} supplied by the application.
 * A {@link WritableImage} can use this {@code PixelBuffer} directly without copying the pixel data.
 * This {@code PixelBuffer} can be shared among multiple {@code WritableImage}s.
 * Pixel data should be stored either in an {@link IntBuffer} using a {@link PixelFormat} of type
 * {@code INT_ARGB_PRE} or in a {@link ByteBuffer} using a {@link PixelFormat} of type {@code BYTE_BGRA_PRE}.
 * When the {@code Buffer} is updated using the {@link #updateBuffer PixelBuffer.updateBuffer} method,
 * all {@code WritableImage}s that were created using this {@code PixelBuffer} are redrawn.
 * <p>
 * Example code that shows how to create a {@code PixelBuffer}:
 * <pre>{@code  // Creating a PixelBuffer using BYTE_BGRA_PRE pixel format.
 * ByteBuffer byteBuffer = ByteBuffer.allocateDirect(width * height * 4);
 * PixelFormat<ByteBuffer> pixelFormat = PixelFormat.getByteBgraPreInstance();
 * PixelBuffer<ByteBuffer> pixelBuffer = new PixelBuffer<>(width, height, byteBuffer, pixelFormat);
 * Image img = new WritableImage(pixelBuffer);
 *
 * // Creating a PixelBuffer using INT_ARGB_PRE pixel format.
 * IntBuffer intBuffer = IntBuffer.allocate(width * height);
 * PixelFormat<IntBuffer> pixelFormat = PixelFormat.getIntArgbPreInstance();
 * PixelBuffer<IntBuffer> pixelBuffer = new PixelBuffer<>(width, height, intBuffer, pixelFormat);
 * Image img = new WritableImage(pixelBuffer);}</pre>
 *
 * @param <T> the type of {@code Buffer} that stores the pixel data.
 *           Only {@code ByteBuffer} and {@code IntBuffer} are supported.
 * @see WritableImage#WritableImage(PixelBuffer)
 * @since 13
 */
public class PixelBuffer<T extends Buffer> {

    private final T buffer;
    private final int width;
    private final int height;
    private final PixelFormat<T> pixelFormat;
    private final List<WeakReference<WritableImage>> imageRefs;

    /**
     * Constructs a {@code PixelBuffer} using the specified {@code Buffer} and {@code PixelFormat}.
     * The type of the specified {@code PixelFormat} must be either {@code PixelFormat.Type.INT_ARGB_PRE}
     * or {@code PixelFormat.Type.BYTE_BGRA_PRE}.
     * <p>The constructor does not allocate memory to store the pixel data. The application must provide
     * a buffer with sufficient memory for the combination of dimensions {@code (width, height)} and the type
     * of {@code PixelFormat}. The {@code PixelFormat.Type.INT_ARGB_PRE} requires an {@code IntBuffer} with
     * minimum capacity of {@code width * height}, and the {@code PixelFormat.Type.BYTE_BGRA_PRE} requires
     * a {@code ByteBuffer} with minimum capacity of {@code width * height * 4}.
     *
     * @param width       width in pixels of this {@code PixelBuffer}
     * @param height      height in pixels of this {@code PixelBuffer}
     * @param buffer      the buffer that stores the pixel data
     * @param pixelFormat the format of pixels in the {@code buffer}
     * @throws IllegalArgumentException if either {@code width} or {@code height}
     *                                  is negative or zero, or if the type of {@code pixelFormat}
     *                                  is unsupported, or if {@code buffer} does
     *                                  not have sufficient memory, or if the type of {@code buffer}
     *                                  and {@code pixelFormat} do not match
     * @throws NullPointerException     if {@code buffer} or {@code pixelFormat} is {@code null}
     */
    public PixelBuffer(int width, int height, T buffer, PixelFormat<T> pixelFormat) {
        Objects.requireNonNull(buffer, "buffer must not be null.");
        Objects.requireNonNull(pixelFormat, "pixelFormat must not be null.");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("PixelBuffer dimensions must be positive (w,h > 0)");
        }
        switch (pixelFormat.getType()) {
            case BYTE_BGRA_PRE:
                if (buffer.capacity() / width / 4 < height) {
                    throw new IllegalArgumentException("Insufficient memory allocated for ByteBuffer.");
                }
                if (!(buffer instanceof ByteBuffer)) {
                    throw new IllegalArgumentException("PixelFormat<ByteBuffer> requires a ByteBuffer.");
                }
                break;
            case INT_ARGB_PRE:
                if (buffer.capacity() / width < height) {
                    throw new IllegalArgumentException("Insufficient memory allocated for IntBuffer.");
                }
                if (!(buffer instanceof IntBuffer)) {
                    throw new IllegalArgumentException("PixelFormat<IntBuffer> requires an IntBuffer.");
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported PixelFormat: " + pixelFormat.getType());
        }
        this.buffer = buffer;
        this.width = width;
        this.height = height;
        this.pixelFormat = pixelFormat;
        this.imageRefs = new LinkedList<>();
    }

    /**
     * Returns the {@code buffer} of this {@code PixelBuffer}.
     *
     * @return the {@code buffer} of this {@code PixelBuffer}
     */
    public T getBuffer() {
        return buffer;
    }

    /**
     * Returns the {@code width} of this {@code PixelBuffer}.
     *
     * @return the {@code width} of this {@code PixelBuffer}
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the {@code height} of this {@code PixelBuffer}.
     *
     * @return the {@code height} of this {@code PixelBuffer}
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the {@code PixelFormat} of this {@code PixelBuffer}.
     *
     * @return the {@code PixelFormat} of this {@code PixelBuffer}
     */
    public PixelFormat<T> getPixelFormat() {
        return pixelFormat;
    }

    /**
     * Invokes the specified {@code Callback} method and updates the dirty region of
     * all {@code WritableImage}s that were created using this {@code PixelBuffer}.
     * The {@code Callback} method is expected to update the buffer and
     * return a {@code Rectangle2D} that encloses the dirty region, or
     * return {@code null} to indicate that the entire buffer is dirty.
     * <p>This method must be called on the JavaFX Application Thread.
     * <p>Example code that shows how to use this method:
     * <pre>{@code  Callback<PixelBuffer<ByteBuffer>, Rectangle2D> callback = pixelBuffer -> {
     *     ByteBuffer buffer = pixelBuffer.getBuffer();
     *     // Update the buffer.
     *     return new Rectangle2D(x, y, dirtyWidth, dirtyHeight);
     * };
     * pixelBuffer.updateBuffer(callback);}</pre>
     *
     * @param callback the {@code Callback} method that updates the buffer
     * @throws IllegalStateException if this method is called on a thread
     *                               other than the JavaFX Application Thread.
     * @throws NullPointerException  if {@code callback} is {@code null}
     **/
    public void updateBuffer(Callback<PixelBuffer<T>, Rectangle2D> callback) {
        Toolkit.getToolkit().checkFxUserThread();
        Objects.requireNonNull(callback, "callback must not be null.");
        Rectangle2D rect2D = callback.call(this);
        if (rect2D != null) {
            if (rect2D.getWidth() > 0 && rect2D.getHeight() > 0) {
                int x1 = (int) Math.floor(rect2D.getMinX());
                int y1 = (int) Math.floor(rect2D.getMinY());
                int x2 = (int) Math.ceil(rect2D.getMaxX());
                int y2 = (int) Math.ceil(rect2D.getMaxY());
                bufferDirty(new Rectangle(x1, y1, x2 - x1, y2 - y1));
            }
        } else {
            bufferDirty(null);
        }
    }

    private void bufferDirty(Rectangle rect) {
        Iterator<WeakReference<WritableImage>> iter = imageRefs.iterator();
        while (iter.hasNext()) {
            final WritableImage image = iter.next().get();
            if (image != null) {
                image.bufferDirty(rect);
            } else {
                iter.remove();
            }
        }
    }

    void addImage(WritableImage image) {
        imageRefs.add(new WeakReference<>(image));
        imageRefs.removeIf(imageRef -> (imageRef.get() == null));
    }
}
