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
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import javafx.scene.image.Image;
import javafx.scene.input.DataFormat;
import com.sun.jfx.incubator.scene.control.richtext.EmbeddedImageHelper;
import com.sun.jfx.incubator.scene.control.richtext.util.RichUtils;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;

/**
 * Facilitates importing of file lists into the RichTextModel.
 * @since 27
 */
public class FileListFormatHandler extends DataFormatHandler {

    private static final FileListFormatHandler instance = new FileListFormatHandler();

    /**
     * Constructor.
     */
    public FileListFormatHandler() {
        super(DataFormat.FILES);
    }

    /**
     * Returns the singleton instance of {@code FileListFormatHandler}.
     * @return the singleton instance of {@code FileListFormatHandler}
     */
    public static final FileListFormatHandler getInstance() {
        return instance;
    }

    @Override
    public StyledInput createStyledInput(Object input, StyleAttributeMap attr) throws IOException {
        List<File> files = (List<File>)input;
        return new FileListStyledInput(files);
    }

    @Override
    public Object copy(StyledTextModel model, StyleResolver r, TextPos start, TextPos end) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save(StyledTextModel model, StyleResolver r, TextPos start, TextPos end, OutputStream out) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Inserts the dropped files as inline images into the {@link RichTextArea}.
     * If a file cannot be loaded as an image, the file name is inserted instead.
     * This method clears the existing selection.
     *
     * @param t the target control
     * @param p the text position
     * @param files the list of files to be inserted
     */
    public static void handleDrop(RichTextArea t, TextPos p, List<File> files) {
        FileListStyledInput in = new FileListStyledInput(files);
        t.clearSelection();
        t.replaceText(p, p, in);
    }

    private static class FileListStyledInput implements StyledInput {

        private final List<File> files;
        private int index;

        FileListStyledInput(List<File> files) {
            this.files = Collections.unmodifiableList(files);
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public StyledSegment nextSegment() {
            long maxMemory = Runtime.getRuntime().maxMemory();
            while (index < files.size()) {
                File f = files.get(index++);
                if (f.isFile()) {
                    try {
                        // reject files that are guaranteed to be too large for this JVM
                        // the drop operation might still cause OOME or take too much time
                        if (f.length() > maxMemory) {
                            throw new Exception("File is too large: " + f);
                        }
                        
                        byte[] b = Files.readAllBytes(f.toPath());
                        Image im = new Image(new ByteArrayInputStream(b), false);
                        if (!im.isError()) {
                            double w = im.getWidth();
                            double h = im.getHeight();
                            EmbeddedImage em = EmbeddedImageHelper.create(b, w, h, EmbeddedImage.FIT_WIDTH, EmbeddedImage.AUTO, true);
                            StyleAttributeMap a = StyleAttributeMap.of(StyleAttributeMap.EMBEDDED_IMAGE, em);
                            return StyledSegment.of(" ", a);
                        }
                    } catch (Throwable e) {
                        RichUtils.log(e);
                    }
                }
                // in case of any error, insert the file name into the document
                return StyledSegment.of(f.getName());
            }
            return null;
        }
    }
}
