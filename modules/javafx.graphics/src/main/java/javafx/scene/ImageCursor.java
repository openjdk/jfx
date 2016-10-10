/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoublePropertyBase;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectPropertyBase;
import javafx.geometry.Dimension2D;
import javafx.scene.image.Image;

import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.cursor.ImageCursorFrame;
import com.sun.javafx.tk.Toolkit;
import java.util.Arrays;
import javafx.beans.NamedArg;


/**
 * A custom image representation of the mouse cursor. On platforms that don't
 * support custom cursors, {@code Cursor.DEFAULT} will be used in place of the
 * specified ImageCursor.
 *
 * <p>Example:
 * <pre>
import javafx.scene.*;
import javafx.scene.image.*;

Image image = new Image("mycursor.png");

Scene scene = new Scene(400, 300);
scene.setCursor(new ImageCursor(image,
                                image.getWidth() / 2,
                                image.getHeight() /2));
 * </pre>
 *
 * @since JavaFX 2.0
 */
public class ImageCursor extends Cursor {
    /**
     * The image to display when the cursor is active. If the image is null,
     * {@code Cursor.DEFAULT} will be used.
     *
     * @defaultValue null
     */
    private ObjectPropertyImpl<Image> image;

    public final Image getImage() {
        return image == null ? null : image.get();
    }

    public final ReadOnlyObjectProperty<Image> imageProperty() {
        return imagePropertyImpl();
    }

    private ObjectPropertyImpl<Image> imagePropertyImpl() {
        if (image == null) {
            image = new ObjectPropertyImpl<Image>("image");
        }

        return image;
    }

    /**
     * The X coordinate of the cursor's hot spot. This hotspot represents the
     * location within the cursor image that will be displayed at the mouse
     * position. This must be in the range of [0,image.width-1]. A value
     * less than 0 will be set to 0. A value greater than
     * image.width-1 will be set to image.width-1.
     *
     * @defaultValue 0
     */
    private DoublePropertyImpl hotspotX;

    public final double getHotspotX() {
        return hotspotX == null ? 0.0 : hotspotX.get();
    }

    public final ReadOnlyDoubleProperty hotspotXProperty() {
        return hotspotXPropertyImpl();
    }

    private DoublePropertyImpl hotspotXPropertyImpl() {
        if (hotspotX == null) {
            hotspotX = new DoublePropertyImpl("hotspotX");
        }

        return hotspotX;
    }

    /**
     * The Y coordinate of the cursor's hot spot. This hotspot represents the
     * location within the cursor image that will be displayed at the mouse
     * position. This must be in the range of [0,image.height-1]. A value
     * less than 0 will be set to 0. A value greater than
     * image.height-1 will be set to image.height-1.
     *
     * @defaultValue 0
     */
    private DoublePropertyImpl hotspotY;

    public final double getHotspotY() {
        return hotspotY == null ? 0.0 : hotspotY.get();
    }

    public final ReadOnlyDoubleProperty hotspotYProperty() {
        return hotspotYPropertyImpl();
    }

    private DoublePropertyImpl hotspotYPropertyImpl() {
        if (hotspotY == null) {
            hotspotY = new DoublePropertyImpl("hotspotY");
        }

        return hotspotY;
    }

    private CursorFrame currentCursorFrame;

    /**
     * Stores the first cursor frame. For non-animated cursors there is only one
     * frame and so the {@code restCursorFrames} is {@code null}.
     */
    private ImageCursorFrame firstCursorFrame;

    /**
     * Maps platform images to cursor frames. It doesn't store the first cursor
     * frame and so it needs to be created only for animated cursors.
     */
    private Map<Object, ImageCursorFrame> otherCursorFrames;

    /**
     * Indicates whether the image cursor is currently in use. The active cursor
     * is bound to the image and invalidates its platform cursor when the image
     * changes.
     */
    private int activeCounter;

    /**
     * Constructs a new empty {@code ImageCursor} which will look as
     * {@code Cursor.DEFAULT}.
     */
    public ImageCursor() {
    }

    /**
     * Constructs an {@code ImageCursor} from the specified image. The cursor's
     * hot spot will default to the upper left corner.
     *
     * @param image the image
     */
    public ImageCursor(@NamedArg("image") final Image image) {
        this(image, 0f, 0f);
    }

    /**
     * Constructs an {@code ImageCursor} from the specified image and hotspot
     * coordinates.
     *
     * @param image the image
     * @param hotspotX the X coordinate of the cursor's hot spot
     * @param hotspotY the Y coordinate of the cursor's hot spot
     */
    public ImageCursor(@NamedArg("image") final Image image,
                       @NamedArg("hotspotX") double hotspotX,
                       @NamedArg("hotspotY") double hotspotY) {
        if ((image != null) && (image.getProgress() < 1)) {
            DelayedInitialization.applyTo(
                    this, image, hotspotX, hotspotY);
        } else {
            initialize(image, hotspotX, hotspotY);
        }
    }

    /**
     * Gets the supported cursor size that is closest to the specified preferred
     * size. A value of (0,0) is returned if the platform does not support
     * custom cursors.
     *
     * <p>
     * Note: if an image is used whose dimensions don't match a supported size
     * (as returned by this method), the implementation will resize the image to
     * a supported size. This may result in a loss of quality.
     *
     * <p>
     * Note: These values can vary between operating systems, graphics cards and
     * screen resolution, but at the time of this writing, a sample Windows
     * Vista machine returned 32x32 for all requested sizes, while sample Mac
     * and Linux machines returned the requested size up to a maximum of 64x64.
     * Applications should provide a 32x32 cursor, which will work well on all
     * platforms, and may optionally wish to provide a 64x64 cursor for those
     * platforms on which it is supported.
     *
     * @param preferredWidth the preferred width of the cursor
     * @param preferredHeight the preferred height of the cursor
     * @return the supported cursor size
     */
    public static Dimension2D getBestSize(double preferredWidth,
                                          double preferredHeight) {
        return Toolkit.getToolkit().getBestCursorSize((int) preferredWidth,
                                                      (int) preferredHeight);
    }

    /**
     * Returns the maximum number of colors supported in a custom image cursor
     * palette.
     *
     * <p>
     * Note: if an image is used which has more colors in its palette than the
     * supported maximum, the implementation will attempt to flatten the
     * palette to the maximum. This may result in a loss of quality.
     *
     * <p>
     * Note: These values can vary between operating systems, graphics cards and
     * screen resolution,  but at the time of this writing, a sample Windows
     * Vista machine returned 256, a sample Mac machine returned
     * Integer.MAX_VALUE, indicating support for full color cursors, and
     * a sample Linux machine returned 2. Applications may want to target these
     * three color depths for an optimal cursor on each platform.
     *
     * @return the maximum number of colors supported in a custom image cursor
     *      palette
     */
    public static int getMaximumColors() {
        return Toolkit.getToolkit().getMaximumCursorColors();
    }

    /**
     * Creates a custom image cursor from one of the specified images. This function
     * will choose the image whose size most closely matched the best cursor size.
     * The hotpotX of the returned ImageCursor is scaled by
     * chosenImage.width/images[0].width and the hotspotY is scaled by
     * chosenImage.height/images[0].height.
     * <p>
     * On platforms that don't support custom cursors, {@code Cursor.DEFAULT} will
     * be used in place of the returned ImageCursor.
     *
     * @param images a sequence of images from which to choose, in order of preference
     * @param hotspotX the X coordinate of the hotspot within the first image
     *        in the images sequence
     * @param hotspotY the Y coordinate of the hotspot within the first image
     *        in the images sequence
     * @return a cursor created from the best image
     */
    public static ImageCursor chooseBestCursor(
            final Image[] images, final double hotspotX, final double hotspotY) {
        final ImageCursor imageCursor = new ImageCursor();

        if (needsDelayedInitialization(images)) {
            DelayedInitialization.applyTo(
                    imageCursor, images, hotspotX, hotspotY);
        } else {
            imageCursor.initialize(images, hotspotX, hotspotY);
        }

        return imageCursor;
    }

    @Override CursorFrame getCurrentFrame() {
        if (currentCursorFrame != null) {
            return currentCursorFrame;
        }

        final Image cursorImage = getImage();

        if (cursorImage == null) {
            currentCursorFrame = Cursor.DEFAULT.getCurrentFrame();
            return currentCursorFrame;
        }

        final Object cursorPlatformImage = Toolkit.getImageAccessor().getPlatformImage(cursorImage);
        if (cursorPlatformImage == null) {
            currentCursorFrame = Cursor.DEFAULT.getCurrentFrame();
            return currentCursorFrame;
        }

        if (firstCursorFrame == null) {
            firstCursorFrame =
                    new ImageCursorFrame(cursorPlatformImage,
                                         cursorImage.getWidth(),
                                         cursorImage.getHeight(),
                                         getHotspotX(),
                                         getHotspotY());
            currentCursorFrame = firstCursorFrame;
        } else if (firstCursorFrame.getPlatformImage() == cursorPlatformImage) {
            currentCursorFrame = firstCursorFrame;
        } else {
            if (otherCursorFrames == null) {
                otherCursorFrames = new HashMap<Object, ImageCursorFrame>();
            }

            currentCursorFrame = otherCursorFrames.get(cursorPlatformImage);
            if (currentCursorFrame == null) {
                // cursor frame not created yet
                final ImageCursorFrame newCursorFrame =
                        new ImageCursorFrame(cursorPlatformImage,
                                             cursorImage.getWidth(),
                                             cursorImage.getHeight(),
                                             getHotspotX(),
                                             getHotspotY());

                otherCursorFrames.put(cursorPlatformImage, newCursorFrame);
                currentCursorFrame = newCursorFrame;
            }
        }

        return currentCursorFrame;
     }

    private void invalidateCurrentFrame() {
        currentCursorFrame = null;
    }

    @Override
    void activate() {
        if (++activeCounter == 1) {
            bindImage(getImage());
            invalidateCurrentFrame();
        }
    }

    @Override
    void deactivate() {
        if (--activeCounter == 0) {
            unbindImage(getImage());
        }
    }

    private void initialize(final Image[] images,
                            final double hotspotX,
                            final double hotspotY) {
        final Dimension2D dim = getBestSize(1f, 1f);

        // If no valid image or if custom cursors are not supported, leave
        // the default image cursor
        if ((images.length == 0) || (dim.getWidth() == 0f)
                                 || (dim.getHeight() == 0f)) {
            return;
        }

        // If only a single image, use it to construct a custom cursor
        if (images.length == 1) {
            initialize(images[0], hotspotX, hotspotY);
            return;
        }

        final Image bestImage = findBestImage(images);
        final double scaleX = bestImage.getWidth() / images[0].getWidth();
        final double scaleY = bestImage.getHeight() / images[0].getHeight();

        initialize(bestImage, hotspotX * scaleX, hotspotY * scaleY);
    }

    private void initialize(Image newImage,
                            double newHotspotX,
                            double newHotspotY) {
        final Image oldImage = getImage();
        final double oldHotspotX = getHotspotX();
        final double oldHotspotY = getHotspotY();

        if ((newImage == null) || (newImage.getWidth() < 1f)
                               || (newImage.getHeight() < 1f)) {
            // If image is invalid set the hotspot to 0
            newHotspotX = 0f;
            newHotspotY = 0f;
        } else {
            if (newHotspotX < 0f) {
                newHotspotX = 0f;
            }
            if (newHotspotX > (newImage.getWidth() - 1f)) {
                newHotspotX = newImage.getWidth() - 1f;
            }
            if (newHotspotY < 0f) {
                newHotspotY = 0f;
            }
            if (newHotspotY > (newImage.getHeight() - 1f)) {
                newHotspotY = newImage.getHeight() - 1f;
            }
        }

        imagePropertyImpl().store(newImage);
        hotspotXPropertyImpl().store(newHotspotX);
        hotspotYPropertyImpl().store(newHotspotY);

        if (oldImage != newImage) {
            if (activeCounter > 0) {
                unbindImage(oldImage);
                bindImage(newImage);
            }

            invalidateCurrentFrame();
            image.fireValueChangedEvent();
        }

        if (oldHotspotX != newHotspotX) {
            hotspotX.fireValueChangedEvent();
        }

        if (oldHotspotY != newHotspotY) {
            hotspotY.fireValueChangedEvent();
        }
    }

    private InvalidationListener imageListener;

    private InvalidationListener getImageListener() {
        if (imageListener == null) {
            imageListener = valueModel -> invalidateCurrentFrame();
        }

        return imageListener;
    }

    private void bindImage(final Image toImage) {
        if (toImage == null) {
            return;
        }

        Toolkit.getImageAccessor().getImageProperty(toImage).addListener(getImageListener());
    }

    private void unbindImage(final Image fromImage) {
        if (fromImage == null) {
            return;
        }

        Toolkit.getImageAccessor().getImageProperty(fromImage).removeListener(getImageListener());
    }

    private static boolean needsDelayedInitialization(final Image[] images) {
        for (final Image image: images) {
            if (image.getProgress() < 1) {
                return true;
            }
        }

        return false;
    }

    // Utility function to select the best image
    private static Image findBestImage(final Image[] images) {
        // Check for exact match and return the first such match
        for (final Image image: images) {
            final Dimension2D dim = getBestSize((int) image.getWidth(),
                                                (int) image.getHeight());
            if ((dim.getWidth() == image.getWidth())
                    && (dim.getHeight() == image.getHeight())) {
                return image;
            }
        }

        // No exact match, check for closest match without down-scaling
        // (i.e., smallest scale >= 1.0)
        Image bestImage = null;
        double bestRatio = Double.MAX_VALUE;
        for (final Image image: images) {
            if ((image.getWidth() > 0) && (image.getHeight() > 0)) {
                final Dimension2D dim = getBestSize(image.getWidth(),
                                                    image.getHeight());
                final double ratioX = dim.getWidth() / image.getWidth();
                final double ratioY = dim.getHeight() / image.getHeight();
                if ((ratioX >= 1) && (ratioY >= 1)) {
                    final double ratio = Math.max(ratioX, ratioY);
                    if (ratio < bestRatio) {
                        bestImage = image;
                        bestRatio = ratio;
                    }
                }
            }
        }
        if (bestImage != null) {
            return bestImage;
        }

        // Still no match, check for closest match alowing for down-scaling
        // (i.e., smallest up-scale or down-scale >= 1.0)
        for (final Image image: images) {
            if ((image.getWidth() > 0) && (image.getHeight() > 0)) {
                final Dimension2D dim = getBestSize(image.getWidth(),
                                                    image.getHeight());
                if ((dim.getWidth() > 0) && (dim.getHeight() > 0)) {
                    double ratioX = dim.getWidth() / image.getWidth();
                    if (ratioX < 1) {
                        ratioX = 1 / ratioX;
                    }
                    double ratioY = dim.getHeight() / image.getHeight();
                    if (ratioY < 1) {
                        ratioY = 1 / ratioY;
                    }
                    final double ratio = Math.max(ratioX, ratioY);
                    if (ratio < bestRatio) {
                        bestImage = image;
                        bestRatio = ratio;
                    }
                }
            }
        }
        if (bestImage != null) {
            return bestImage;
        }

        return images[0];
    }

    private final class DoublePropertyImpl extends ReadOnlyDoublePropertyBase {
        private final String name;

        private double value;

        public DoublePropertyImpl(final String name) {
            this.name = name;
        }

        public void store(final double value) {
            this.value = value;
        }

        @Override
        public void fireValueChangedEvent() {
            super.fireValueChangedEvent();
        }

        @Override
        public double get() {
            return value;
        }

        @Override
        public Object getBean() {
            return ImageCursor.this;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private final class ObjectPropertyImpl<T>
            extends ReadOnlyObjectPropertyBase<T> {
        private final String name;

        private T value;

        public ObjectPropertyImpl(final String name) {
            this.name = name;
        }

        public void store(final T value) {
            this.value = value;
        }

        @Override
        public void fireValueChangedEvent() {
            super.fireValueChangedEvent();
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public Object getBean() {
            return ImageCursor.this;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private static final class DelayedInitialization
            implements InvalidationListener {
        private final ImageCursor targetCursor;

        private final Image[] images;
        private final double hotspotX;
        private final double hotspotY;

        private final boolean initAsSingle;

        private int waitForImages;

        private DelayedInitialization(final ImageCursor targetCursor,
                                      final Image[] images,
                                      final double hotspotX,
                                      final double hotspotY,
                                      final boolean initAsSingle) {
            this.targetCursor = targetCursor;
            this.images = images;
            this.hotspotX = hotspotX;
            this.hotspotY = hotspotY;
            this.initAsSingle = initAsSingle;
        }


        public static void applyTo(final ImageCursor imageCursor,
                                   final Image[] images,
                                   final double hotspotX,
                                   final double hotspotY) {
            final DelayedInitialization delayedInitialization =
                    new DelayedInitialization(imageCursor,
                                              Arrays.copyOf(images, images.length),
                                              hotspotX,
                                              hotspotY,
                                              false);
            delayedInitialization.start();
        }

        public static void applyTo(final ImageCursor imageCursor,
                                   final Image image,
                                   final double hotspotX,
                                   final double hotspotY) {
            final DelayedInitialization delayedInitialization =
                    new DelayedInitialization(imageCursor,
                                              new Image[] { image },
                                              hotspotX,
                                              hotspotY,
                                              true);
            delayedInitialization.start();
        }

        private void start() {
            for (final Image image: images) {
                if (image.getProgress() < 1) {
                    ++waitForImages;
                    image.progressProperty().addListener(this);
                }
            }
        }

        private void cleanupAndFinishInitialization() {
            for (final Image image: images) {
                image.progressProperty().removeListener(this);
            }

            if (initAsSingle) {
                targetCursor.initialize(images[0], hotspotX, hotspotY);
            } else {
                targetCursor.initialize(images, hotspotX, hotspotY);
            }
        }

        @Override
        public void invalidated(Observable valueModel) {
            if (((ReadOnlyDoubleProperty)valueModel).get() == 1) {
                if (--waitForImages == 0) {
                    cleanupAndFinishInitialization();
                }
            }
        }
    }
}
