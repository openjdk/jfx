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
import javafx.scene.input.DataFormat;
import com.sun.jfx.incubator.scene.control.richtext.EmbeddedImageHelper;
import com.sun.jfx.incubator.scene.control.richtext.SegmentStyledInput;
import com.sun.jfx.incubator.scene.control.richtext.util.RichUtils;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;

/**
 * Facilitates importing of images into the RichTextModel.
 * The image is imported via lossless compression (PNG).
 *
 * @since 28
 */
public class ImageFormatHandler extends DataFormatHandler {

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
     * {@inheritDoc}
     *
     * <p>The type of {@code input} must be {@code Image}.
     */
    @Override
    public StyledInput createStyledInput(Object input, StyleAttributeMap attr) throws IOException {
        Image im = (Image)input;
        double w = im.getWidth();
        double h = im.getHeight();
        byte[] b = RichUtils.writePNG(im);
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
}
