/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package jfx.incubator.scene.control.richtext.model;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import com.sun.jfx.incubator.scene.control.richtext.EmbeddedImageHelper;
import com.sun.jfx.incubator.scene.control.richtext.RequiresComplexLayout;
import com.sun.jfx.incubator.scene.control.richtext.VFlow;

/**
 * An attribute which allows an image to be embedded into the {@link RichTextModel}.
 * @since 27
 */
public final class EmbeddedImage {

    /**
     * Sentinel value which can be passed to either {@code targetWidth} or {@code targetHeight}
     * to indicate that the rendered image dimension should be computed according to the image intrinsic aspect ratio.
     */
    public static final double AUTO = 0.0;

    /**
     * Sentinel value which can be passed to {@code targetWidth}
     * to indicate that the rendered image width should not exceed the view's wrapped text width.
     */
    public static final double FIT_WIDTH = -1.0;

    /**
     * Sentinel value which can be passed to {@code targetWidth}
     * to indicate that the rendered image width should always fit the view's wrapped text width.
     */
    public static final double FIT_WIDTH_ALWAYS = -2.0;

    static {
        EmbeddedImageHelper.setAccessor(new EmbeddedImageHelper.Accessor() {
            @Override
            public byte[] getBytes(EmbeddedImage im) {
                return im.bytes;
            }

            @Override
            public EmbeddedImage create(
                byte[] bytes,
                double width,
                double height,
                double targetWidth,
                double targetHeight,
                boolean keepAspectRatio
            ) {
                return new EmbeddedImage(bytes, width, height, targetWidth, targetHeight, keepAspectRatio);
            }
        });
    }

    private final byte[] bytes;
    private final double width;
    private final double height;
    private final double targetWidth;
    private final double targetHeight;
    private final boolean keepAspectRatio;

    /// Private constructor that DOES NOT make a defensive copy of the bytes.
    private EmbeddedImage(
        byte[] bytes,
        double width,
        double height,
        double targetWidth,
        double targetHeight,
        boolean keepAspectRatio
    ) {
        this.bytes = bytes;
        this.width = width;
        this.height = height;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.keepAspectRatio = keepAspectRatio;
    }

    /**
     * Creates a new EmbeddedImage instance while making a defensive copy of the {@code bytes} array.
     * <p>
     * Any negative value passed to either {@code targetWidth} or {@code targetHeight}, other than a declared
     * sentinel value, causes the image to be rendered as if {@link #AUTO} had been passed.
     *
     * @param bytes the image source
     * @param width the original image width
     * @param height the original image height
     * @param targetWidth the target image width, or {@link #FIT_WIDTH}, {@link #FIT_WIDTH_ALWAYS}, or {@link #AUTO}
     * @param targetHeight the target image height, or {@link #AUTO}
     * @param keepAspectRatio whether to preserve the image aspect ratio
     * @return the new instance
     */
    public static EmbeddedImage of(
        byte[] bytes,
        double width,
        double height,
        double targetWidth,
        double targetHeight,
        boolean keepAspectRatio
    ) {
        byte[] b = Arrays.copyOf(bytes, bytes.length);
        return new EmbeddedImage(b, width, height, targetWidth, targetHeight, keepAspectRatio);
    }

    /**
     * Returns the original image width.
     * @return the image width
     */
    public double getWidth() {
        return width;
    }

    /**
     * Returns the original image height.
     * @return the image height
     */
    public double getHeight() {
        return height;
    }

    /**
     * Returns the target image width specification: positive when specifying the final width,
     * or {@link #AUTO} to determine the value from {@link #isKeepAspectRatio()} and {@link #getWidth()},
     * or {@link #FIT_WIDTH} to ensure the image does not exceed the viewport width,
     * or {@link #FIT_WIDTH_ALWAYS} to always fit the viewport width.
     * @return the image target width
     */
    public double getTargetWidth() {
        return targetWidth;
    }

    /**
     * Returns the target image height specification: positive when specifying the final height,
     * or {@link #AUTO} to determine the value from {@link #isKeepAspectRatio()} and {@link #getHeight()}.
     * @return the image target height
     */
    public double getTargetHeight() {
        return targetHeight;
    }

    /**
     * Indicates whether the aspect ratio of this image is preserved when scaling to fit the image within the document.
     * @return true if the aspect ratio is preserved
     */
    public boolean isKeepAspectRatio() {
        return keepAspectRatio;
    }

    @Override
    public String toString() {
        return
            "EmbeddedImage{" +
            "\"" + width + " x " + height + "\"" +
            " targetWidth=" + targetWidth +
            " targetHeight=" + targetHeight +
            " keepAspectRatio=" + keepAspectRatio +
            "}";
    }

    @Override
    public boolean equals(Object x) {
        if (x == this) {
            return true;
        } else if (x instanceof EmbeddedImage im) {
            return
                (width == im.width) &&
                (height == im.height) &&
                (targetWidth == im.targetWidth) &&
                (targetHeight == im.targetHeight) &&
                (keepAspectRatio == im.keepAspectRatio) &&
                Arrays.equals(bytes, im.bytes);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = EmbeddedImage.class.hashCode();
        h = 31 * h + Arrays.hashCode(bytes);
        h = 31 * h + Double.hashCode(width);
        h = 31 * h + Double.hashCode(height);
        h = 31 * h + Double.hashCode(targetWidth);
        h = 31 * h + Double.hashCode(targetHeight);
        h = 31 * h + Boolean.hashCode(keepAspectRatio);
        return h;
    }

    /**
     * Creates a copy of this {@code EmbeddedImage} with the specified target width, height, and aspect ratio.
     * @param targetWidth the new target width
     * @param targetHeight the new target height
     * @param keepAspectRatio whether to keep the aspect ratio
     * @return the new instance
     */
    public EmbeddedImage copy(double targetWidth, double targetHeight, boolean keepAspectRatio) {
        return new EmbeddedImage(bytes, width, height, targetWidth, targetHeight, keepAspectRatio);
    }

    /**
     * Creates the Node to be inserted into RichTextArea.
     * @return the node instance
     */
    public Node createNode() {
        Image im = new Image(new ByteArrayInputStream(bytes));
        if ((targetWidth == FIT_WIDTH) || (targetWidth == FIT_WIDTH_ALWAYS)) {
            return new Tracking(im);
        } else {
            return new Fixed(im);
        }
    }

    /// Image Container with a fixed-size image.
    private final class Fixed extends Label {

        public Fixed(Image im) {
            ImageView view = new ImageView(im);
            view.setSmooth(true);
            view.setPreserveRatio(keepAspectRatio);
            view.setFitWidth(normalize(targetWidth));
            view.setFitHeight(normalize(targetHeight));

            setGraphic(view);
            setMaxWidth(USE_PREF_SIZE);
            setMinWidth(2);
            setMinHeight(2);
        }

        private static double normalize(double x) {
            return (x < 0.0) ? 0.0 : x;
        }
    }

    /// Image Container that tracks the document width.
    private final class Tracking extends Label implements RequiresComplexLayout {

        private final ImageView view;

        public Tracking(Image im) {
            view = new ImageView(im);
            // view.setStyle("tracking-image"); // TODO for testing? no css should touch this node
            view.setSmooth(true);
            view.setPreserveRatio(keepAspectRatio);

            setGraphic(view);
            setMaxWidth(Double.MAX_VALUE);
            setMinWidth(2);
            setMinHeight(2);
        }

        @Override
        public void updateVFlowContext(VFlow f) {
            double av = f.availableWidth();
            double fw = computeFitWidth(av);
            double fh = computeFitHeight();
            view.setFitWidth(fw);
            view.setFitHeight(fh);
            // takes the full paragraph
            setPrefWidth(av);
        }

        private double computeFitWidth(double av) {
            if (av >= 0.0) {
                if (targetWidth == FIT_WIDTH_ALWAYS) {
                    return av;
                } else if (width > av) {
                    return av;
                }
            }
            return width;
        }

        private double computeFitHeight() {
            if (!keepAspectRatio) {
                if (targetHeight > 0.0) {
                    return targetHeight;
                } else {
                    return height;
                }
            }
            return 0.0;
        }
    }
}
