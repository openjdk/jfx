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

package javafx.incubator.scene.control.rich.model;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import javafx.incubator.scene.control.rich.StyleResolver;
import javafx.incubator.scene.control.rich.TextPos;
import javafx.scene.input.DataFormat;
import com.sun.javafx.scene.control.rich.StringBuilderStyledOutput;

/**
 * {@link DataFormatHandler} which operates with plain text. 
 */
public class PlainTextFormatHandler extends DataFormatHandler {
    /** The constructor. */
    public PlainTextFormatHandler() {
        super(DataFormat.PLAIN_TEXT);
    }

    @Override
    public StyledInput createStyledInput(Object src) {
        String text = (src == null) ? "" : src.toString();
        return StyledInput.of(text, StyleAttrs.EMPTY);
    }

    @Override
    public Object copy(StyledTextModel m, StyleResolver resolver, TextPos start, TextPos end) throws IOException {
        StringBuilderStyledOutput out = new StringBuilderStyledOutput();
        m.exportText(start, end, out);
        return out.getOutput();
    }

    @Override
    public void save(StyledTextModel m, StyleResolver resolver, TextPos start, TextPos end, OutputStream out) throws IOException {
        Charset charset = Charset.forName("utf-8");
        byte[] newline = System.getProperty("line.separator").getBytes(charset);

        StyledOutput so = new StyledOutput() {
            @Override
            public void append(StyledSegment seg) throws IOException {
                switch (seg.getType()) {
                case LINE_BREAK:
                    out.write(newline);
                    break;
                case TEXT:
                    String text = seg.getText();
                    byte[] b = text.getBytes(charset);
                    out.write(b);
                    break;
                }
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }
            
            @Override
            public void close() throws IOException {
                out.close();
            }
        };
        m.exportText(start, end, so);
        out.flush();
    }
}
