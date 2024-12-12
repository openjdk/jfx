/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import javafx.scene.input.DataFormat;
import com.sun.jfx.incubator.scene.control.richtext.HtmlStyledOutput;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;

/**
 * This partial {@link DataFormatHandler} supports export of styled text in a simple HTML format.
 *
 * @since 24
 */
public class HtmlExportFormatHandler extends DataFormatHandler {
    /** when true, style attributes are inlined, this seems to work better in Thunderbird */
    private static final boolean INLINE_STYLES = true;
    private static final HtmlExportFormatHandler instance = new HtmlExportFormatHandler();

    /** The constructor */
    private HtmlExportFormatHandler() {
        super(DataFormat.HTML);
    }

    /**
     * Returns the singleton instance of {@code HtmlExportFormatHandler}.
     * @return the singleton instance of {@code HtmlExportFormatHandler}
     */
    public static final HtmlExportFormatHandler getInstance() {
        return instance;
    }

    @Override
    public StyledInput createStyledInput(String input, StyleAttributeMap attr) {
        throw new UnsupportedOperationException("import from HTML is not supported by this DataFormatHandler");
    }

    @Override
    public Object copy(StyledTextModel model, StyleResolver resolver, TextPos start, TextPos end) throws IOException {
        StringWriter wr = new StringWriter(65536);
        export(model, resolver, start, end, wr);
        return wr.toString();
    }

    @Override
    public void save(StyledTextModel model, StyleResolver resolver, TextPos start, TextPos end, OutputStream out)
        throws IOException {
        Charset ascii = Charset.forName("ASCII");
        OutputStreamWriter wr = new OutputStreamWriter(out, ascii);
        export(model, resolver, start, end, wr);
    }

    private void export(StyledTextModel model, StyleResolver resolver, TextPos start, TextPos end, Writer wr)
        throws IOException {
        HtmlStyledOutput out = new HtmlStyledOutput(resolver, wr, INLINE_STYLES);
        // collect styles
        model.export(start, end, out.firstPassBuilder());

        out.writePrologue();
        model.export(start, end, out);
        out.writeEpilogue();
        out.flush();
    }
}
