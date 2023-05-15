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
     * Creates a StyledInput for the given source.
     * TODO explain
     *
     * @param src
     * @return
     */
    public abstract StyledInput getStyledInput(Object src);
    
    /**
     * Creates an object to be put into Clipboard for the given text range.
     * The caller guarantees that the {@code start} precedes the {@code end} position.
     *
     * - may throw an exception when out of memory, or detect the condition and bail out somehow
     *
     * TODO explain
     *
     * @param model 
     * @param resolver TODO
     * @param start 
     * @param end 
     * @return
     * @throws IOException 
     */
    public abstract Object copy(StyledTextModel model, StyleResolver resolver, TextPos start, TextPos end) throws IOException;
    
    /**
     * Save the text range to the output stream (e.g. save to file).
     *
     * TODO may need to specify additional options:
     * - encoding (platform, utf-8)?
     * - line separator
     *
     * TODO explain
     *
     * @throws IOException 
     */
    public abstract void save(StyledTextModel model, StyleResolver resolver, TextPos start, TextPos end, OutputStream out) throws IOException;
    
    private final DataFormat format;

    public DataFormatHandler(DataFormat f) {
        this.format = f;
    }
    
    public DataFormat getDataFormat() {
        return format;
    }
}
