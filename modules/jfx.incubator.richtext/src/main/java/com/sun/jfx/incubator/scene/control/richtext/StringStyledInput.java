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

package com.sun.jfx.incubator.scene.control.richtext;

import java.io.IOException;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledInput;
import jfx.incubator.scene.control.richtext.model.StyledSegment;

public class StringStyledInput implements StyledInput {
    private final String text;
    private final StyleAttributeMap attrs;
    private int offset;

    // TODO check for illegal chars (<0x20 except for \r \n \t)
    public StringStyledInput(String text, StyleAttributeMap a) {
        this.text = (text == null ? "" : text);
        this.attrs = a;
    }

    @Override
    public StyledSegment nextSegment() {
        if (offset < text.length()) {
            int c = text.charAt(offset);
            // is it a line break;?
            switch(c) {
            case '\n':
                offset++;
                return StyledSegment.LINE_BREAK;
            case '\r':
                c = charAt(++offset);
                switch(c) {
                case '\n':
                    offset++;
                    break;
                }
                return StyledSegment.LINE_BREAK;
            }

            int ix = indexOfLineBreak(offset);
            if (ix < 0) {
                String s = text.substring(offset);
                offset = text.length();
                return StyledSegment.of(s, attrs);
            } else {
                String s = text.substring(offset, ix);
                offset = ix;
                return StyledSegment.of(s, attrs);
            }
        }
        return null;
    }

    private int charAt(int index) {
        if (index < text.length()) {
            return text.charAt(index);
        }
        return -1;
    }

    private int indexOfLineBreak(int start) {
        int len = text.length();
        for(int i=start; i<len; i++) {
            char c = text.charAt(i);
            switch(c) {
            case '\r':
            case '\n':
                return i;
            // TODO we can check for invalid ctrl characters here,
            // or use a string builder to filter out unwanted chars
            }
        }
        return -1;
    }

    @Override
    public void close() throws IOException {
    }
}
