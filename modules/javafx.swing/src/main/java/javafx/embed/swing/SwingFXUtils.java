/*
 * Copyright (c) 2012, 2026, Oracle and/or its affiliates. All rights reserved.
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

package javafx.embed.swing;

import java.awt.image.BufferedImage;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import com.sun.javafx.util.ImageUtils;

/**
 * This class provides utility methods for converting data types between
 * Swing/AWT and JavaFX formats.
 * @since JavaFX 2.2
 */
public class SwingFXUtils {
    private SwingFXUtils() {} // no instances

    /**
     * Snapshots the specified {@link BufferedImage} and stores a copy of
     * its pixels into a JavaFX {@link Image} object, creating a new
     * object if needed.
     * The returned {@code Image} will be a static snapshot of the state
     * of the pixels in the {@code BufferedImage} at the time the method
     * completes.  Further changes to the {@code BufferedImage} will not
     * be reflected in the {@code Image}.
     * <p>
     * The optional JavaFX {@link WritableImage} parameter may be reused
     * to store the copy of the pixels.
     * A new {@code Image} will be created if the supplied object is null,
     * is too small or of a type which the image pixels cannot be easily
     * converted into.
     *
     * @param bimg the {@code BufferedImage} object to be converted
     * @param wimg an optional {@code WritableImage} object that can be
     *        used to store the returned pixel data
     * @return an {@code Image} object representing a snapshot of the
     *         current pixels in the {@code BufferedImage}.
     * @since JavaFX 2.2
     */
    public static WritableImage toFXImage(BufferedImage bimg, WritableImage wimg) {
        return ImageUtils.toFXImage(bimg, wimg);
    }

    /**
     * Snapshots the specified JavaFX {@link Image} object and stores a
     * copy of its pixels into a {@link BufferedImage} object, creating
     * a new object if needed.
     * The method will only convert a JavaFX {@code Image} that is readable
     * as per the conditions on the
     * {@link Image#getPixelReader() Image.getPixelReader()}
     * method.
     * If the {@code Image} is not readable, as determined by its
     * {@code getPixelReader()} method, then this method will return null.
     * If the {@code Image} is a writable, or other dynamic image, then
     * the {@code BufferedImage} will only be set to the current state of
     * the pixels in the image as determined by its {@link PixelReader}.
     * Further changes to the pixels of the {@code Image} will not be
     * reflected in the returned {@code BufferedImage}.
     * <p>
     * The optional {@code BufferedImage} parameter may be reused to store
     * the copy of the pixels.
     * A new {@code BufferedImage} will be created if the supplied object
     * is null, is too small or of a type which the image pixels cannot
     * be easily converted into.
     *
     * @param img the JavaFX {@code Image} to be converted
     * @param bimg an optional {@code BufferedImage} object that may be
     *        used to store the returned pixel data
     * @return a {@code BufferedImage} containing a snapshot of the JavaFX
     *         {@code Image}, or null if the {@code Image} is not readable.
     * @since JavaFX 2.2
     */
    public static BufferedImage fromFXImage(Image img, BufferedImage bimg) {
        return ImageUtils.fromFXImage(img, bimg);
    }
}
