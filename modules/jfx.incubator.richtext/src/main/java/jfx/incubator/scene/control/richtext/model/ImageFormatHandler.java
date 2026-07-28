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

import java.io.IOException;
import java.io.OutputStream;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.input.DataFormat;
import com.sun.jfx.incubator.scene.control.richtext.EmbeddedImageHelper;
import com.sun.jfx.incubator.scene.control.richtext.SegmentStyledInput;
import com.sun.jfx.incubator.scene.control.richtext.util.RichUtils;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;

/**
 * Facilitates importing of images into the RichTextModel.
 * @since 28
 */
public class ImageFormatHandler extends DataFormatHandler {

    private static final int SMALL_SIZE = 128;
    private static final int SMALL_CHANGE_THRESHOLD = 130050; // 1/8th of full scale
    private static final ImageFormatHandler instance = new ImageFormatHandler();

    /**
     * Constructor.
     */
    private ImageFormatHandler() {
        super(DataFormat.IMAGE);
    }

    /**
     * Returns the singleton instance of {@code ImageFormatHandler}.
     * @return the singleton instance of {@code ImageFormatHandler}
     */
    public static final ImageFormatHandler getInstance() {
        return instance;
    }

    /**
     * Creates an ImageFormatHandler instance which imports images using either
     * a lossless (PNG) or lossy (JPEG) encoding.
     *
     * @param lossless determines whether to create a lossless handler
     * @return the instance
     */
    public static final ImageFormatHandler create(boolean lossless) {
        return new ImageFormatHandler() {
            @Override
            byte[] toByteArray(Image im) throws IOException {
                return lossless ?
                    RichUtils.writePNG(im) :
                    RichUtils.writeJPG(im);
            }
        };
    }

    /**
     * {@inheritDoc}
     *
     * <p>The type of {@code input} must be {@code Image}.
     */
    @Override
    public StyledInput createStyledInput(Object input, StyleAttributeMap attr) throws IOException {
        Image im = (Image)input;
        double w = im.getWidth();
        double h = im.getHeight();
        byte[] b = toByteArray(im);
        if ((b == null) || (b.length == 0)) {
            // TODO there seems to be a bug writing certain images to JPG
            throw new IOException("Failed to store image");
        }
        EmbeddedImage em = EmbeddedImageHelper.create(b, w, h, EmbeddedImage.AUTO, EmbeddedImage.AUTO, true);
        StyleAttributeMap a = StyleAttributeMap.of(StyleAttributeMap.EMBEDDED_IMAGE, em);
        return new SegmentStyledInput(StyledSegment.of(" ", a));
    }

    @Override
    public Object copy(StyledTextModel m, StyleResolver r, TextPos start, TextPos end) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save(StyledTextModel m, StyleResolver r, TextPos start, TextPos end, OutputStream out) throws IOException {
        throw new UnsupportedOperationException();
    }

    byte[] toByteArray(Image im) throws IOException {
        boolean isPhoto = detectPhoto(im);
        RichUtils.log("photo={0} fmt={1}", isPhoto, im.getPixelReader().getPixelFormat());

        // FIX storing as jpg is broken, see JDK-8388450
        isPhoto = false;

        return isPhoto ?
            RichUtils.writeJPG(im) :
            RichUtils.writePNG(im);
    }

    private static int diff(int a, int b) {
        int d = diffChannel(a, b); // r
        a >>= 8;
        b >>= 8;
        d += diffChannel(a, b); // g
        a >>= 8;
        b >>= 8;
        d += diffChannel(a, b); // b
        return d;
    }

    private static int diffChannel(int a, int b) {
        int d = (a & 0xff) - (b & 0xff);
        return d < 0 ? -d : d;
    }

    private boolean detectPhoto(Image im) {
        int w = (int)im.getWidth();
        int h = (int)im.getHeight();

        // small image -> png
        if ((w < SMALL_SIZE) && (h < SMALL_SIZE)) {
            return false;
        }

        PixelReader rd = im.getPixelReader();

        // TODO we could check the presence of alpha channel, but JDK-8388511

        int parts = 4;
        int stepx = w / parts;
        int stepy = h / parts;
        int x;
        int y;

        int same = 0;
        int jump = 0;

        x = stepx;
        for (int ix = 1; ix < parts; ix++) {
            int prev = 0;
            for (y = 0; y < h; y++) {
                int argb = rd.getArgb(x, y);

                // alpha
                if((argb & 0xff000000) != 0xff000000) {
                    // alpha channel -> png
                    return false;
                }

                if (prev == argb) {
                    same++;
                } else {
                    int diff = diff(argb, prev);
                    if (diff < SMALL_CHANGE_THRESHOLD) {
                        jump++;
                    }
                }
                prev = argb;
            }
            x += stepx;
        }

        y = stepy;
        for (int iy = 1; iy < parts; iy++) {
            int prev = 0;
            for (x = 0; x < w; x++) {
                int argb = rd.getArgb(x, y);

                // alpha
                if((argb & 0xff000000) != 0xff000000) {
                    // alpha channel -> png
                    return false;
                }

                if (prev == argb) {
                    same++;
                } else {
                    int diff = diff(argb, prev);
                    if (diff < SMALL_CHANGE_THRESHOLD) {
                        jump++;
                    }
                }
                prev = argb;
            }
            y += stepy;
        }

        // more small changes than same color pixels -> jpg
        return jump > same;
    }
}
