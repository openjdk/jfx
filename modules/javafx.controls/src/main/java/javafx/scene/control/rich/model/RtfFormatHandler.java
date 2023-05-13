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
import java.nio.charset.Charset;
import javafx.scene.control.rich.StyleResolver;
import javafx.scene.control.rich.TextPos;
import javafx.scene.input.DataFormat;

public class RtfFormatHandler extends DataFormatHandler {
    public RtfFormatHandler() {
        super(DataFormat.RTF);
    }

    @Override
    public StyledInput getStyledInput(Object src) {
        if (src == null) {
            return StyledInput.of("", StyleInfo.NONE);
        }

        // TODO parse RTF
        String text = src.toString();
        System.err.println(text);
        return StyledInput.of(text);
    }

    @Override
    public Object copy(StyledTextModel m, StyleResolver resolver, TextPos start, TextPos end) throws IOException {
        StringBuilder sb = new StringBuilder(65536);
        RtfStyledOutput r = new RtfStyledOutput(resolver) {
            protected void write(String s) throws IOException {
                sb.append(s);
            }
        };
        m.exportText(start, end, r.firstPassBuilder());
        
        r.writePrologue();
        m.exportText(start, end, r);
        r.writeEpilogue();
        
        String rtf = sb.toString();
        System.out.println(rtf); // FIX
        return rtf;
    }

    @Override
    public void save(StyledTextModel m, StyleResolver resolver, TextPos start, TextPos end, OutputStream out) throws IOException {
        RtfStyledOutput r = new RtfStyledOutput(resolver) {
            private static final Charset ascii = Charset.forName("ASCII");

            protected void write(String s) throws IOException {
                out.write(s.getBytes(ascii));
            }
        };
        m.exportText(start, end, r.firstPassBuilder());
        
        r.writePrologue();
        m.exportText(start, end, r);
        r.writeEpilogue();
        
        out.flush();
    }
}
