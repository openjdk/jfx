/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.rich.model;

import java.io.IOException;
import java.io.OutputStream;
import javafx.scene.control.rich.StyleResolver;
import javafx.scene.control.rich.TextPos;
import javafx.scene.input.DataFormat;

/**
 * Facilitates import/export of styled text into/from a StyledTextModel.
 */
public abstract class DataFormatHandler {
    /**
     * Creates a StyledInput for the given source object obtained from the system Clipboard.
     * This method returns null if a StyledInput cannot be created, or an I/O error occurs.
     *
     * @param src input object obtained from the Clipboard
     * @return StyledInput generated according to this data format, or null
     */
    // TODO throw UnsupportedMethodException if import operation is not supported?
    public abstract StyledInput createStyledInput(Object src);

    /**
     * Creates an object to be put into the Clipboard for the given text range.
     * The caller must guarantee that the {@code start} precedes the {@code end} position.
     * <p>
     * Typically, the implementation creates an instance of {@link StyledOutput} and calls
     * {@link StyledTextModel#exportText(TextPos, TextPos, StyledOutput)} method.
     *
     * @param model source model
     * @param resolver view-specific style resolver
     * @param start start text position
     * @param end end text position
     * @return an object to be placed to the Clipboard
     * @throws IOException when an I/O error occurs
     */
    // TODO throw UnsupportedMethodException if export operation is not supported?
    public abstract Object copy(StyledTextModel model, StyleResolver resolver, TextPos start, TextPos end)
        throws IOException;

    /**
     * Save the text range in the handler's format to the output stream (e.g. save to file).
     * The caller must guarantee that the {@code start} precedes the {@code end} position.
     * It is the responsibility of the caller to close the {@code OutputStream}.
     * <p>
     * Typically, the implementation creates an instance of {@link StyledOutput} and calls
     * {@link StyledTextModel#exportText(TextPos, TextPos, StyledOutput)} method.
     *
     * @param model source model
     * @param resolver view-specific style resolver
     * @param start start text position
     * @param end end text position
     * @param out target {@code OutputStream}
     * @throws IOException when an I/O error occurs
     */
    // TODO throw UnsupportedMethodException if export operation is not supported?
    public abstract void save(
        StyledTextModel model,
        StyleResolver resolver,
        TextPos start,
        TextPos end,
        OutputStream out) throws IOException;

    private final DataFormat format;

    /**
     * Creates a DataHandler instance for the specified format.
     * @param f data format
     */
    public DataFormatHandler(DataFormat f) {
        this.format = f;
    }

    /**
     * Returns the {@link DataFormat} associated with this handler.
     * @return the data format
     */
    public final DataFormat getDataFormat() {
        return format;
    }
}