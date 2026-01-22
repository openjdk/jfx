/*
 * Copyright (c) 2023, 2026, Oracle and/or its affiliates. All rights reserved.
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
import jfx.incubator.scene.control.richtext.LineEnding;
import jfx.incubator.scene.control.richtext.model.StyledOutput;
import jfx.incubator.scene.control.richtext.model.StyledSegment;

public class StringBuilderStyledOutput implements StyledOutput {
    private final int limit;
    private final StringBuilder sb;
    private final String newline;

    public StringBuilderStyledOutput(StringBuilder sb, LineEnding lineEnding, int limit) {
        // TODO throw IOException on running over limit
        this.limit = limit;
        this.sb = sb;
        newline = lineEnding.getText();
    }

    public StringBuilderStyledOutput(LineEnding lineEnding) {
        this(new StringBuilder(1024), lineEnding, Integer.MAX_VALUE);
    }

    @Override
    public void consume(StyledSegment seg) throws IOException {
        switch (seg.getType()) {
        case LINE_BREAK:
            append(newline, false);
            break;
        case TEXT:
            String text = seg.getText();
            // append as much text as possible
            // potentially risking breaking up code points and grapheme clusters
            append(text, true);
            break;
        }
    }

    private void append(String text, boolean allowPartial) throws IOException {
        if (limit < Integer.MAX_VALUE) {
            int available = limit - sb.length();
            if (text.length() > available) {
                if (allowPartial) {
                    sb.append(text, 0, available);
                }
                throw new IOException("limit exceeded");
            }
        }
        sb.append(text);
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
