/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jfx.incubator.scene.control.richtext;

import java.io.IOException;
import com.sun.jfx.incubator.scene.control.richtext.util.RichUtils;
import jfx.incubator.scene.control.richtext.LineEnding;
import jfx.incubator.scene.control.richtext.model.StyledOutput;
import jfx.incubator.scene.control.richtext.model.StyledSegment;

public class StringBuilderStyledOutput implements StyledOutput {
    private final StringBuilder sb;
    private final String newline;

    public StringBuilderStyledOutput(LineEnding lineEnding) {
        sb = new StringBuilder(1024);
        newline = lineEnding.getText();
    }

    @Override
    public void consume(StyledSegment seg) {
        switch (seg.getType()) {
        case LINE_BREAK:
            sb.append(newline);
            break;
        case TEXT:
            String text = seg.getText();
            sb.append(text);
            break;
        }
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }
}
