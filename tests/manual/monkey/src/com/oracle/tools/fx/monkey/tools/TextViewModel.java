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

package com.oracle.tools.fx.monkey.tools;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import javafx.scene.paint.Color;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.RichParagraph;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledTextModelViewOnlyBase;

public abstract class TextViewModel extends StyledTextModelViewOnlyBase {

    private static final StyleAttributeMap PLAIN = StyleAttributeMap.builder()
        .setFontFamily("Monospaced")
        .build();
    private static final StyleAttributeMap UNICODE = StyleAttributeMap.builder()
        .setTextColor(Color.BLUE)
        .setFontFamily("Monospaced")
        .build();
    private static final int WIDTH = 16;

    public static TextViewModel ofText(Color color, String text, boolean ascii) {
        ArrayList<String> lines = new ArrayList<>();
        if (text != null) {
            try (BufferedReader rd = new BufferedReader(new StringReader(text))) {
                String s;
                while ((s = rd.readLine()) != null) {
                    lines.add(s);
                }
            } catch (Exception e) {
                // ignore
            }
        }
        StyleAttributeMap style = StyleAttributeMap.builder()
            .setTextColor(color)
            .setFontFamily("Monospaced")
            .build();

        return new TextViewModel() {
            @Override
            public String getPlainText(int index) {
                if (ascii) {
                    return getParagraph(index).getPlainText();
                } else {
                    return lines.get(index);
                }
            }

            @Override
            public int size() {
                return lines.size();
            }

            @Override
            public RichParagraph getParagraph(int index) {
                String s = lines.get(index);
                if (ascii) {
                    return native2ascii(s);
                }
                return RichParagraph.builder().addSegment(s, style).build();
            }

            private RichParagraph native2ascii(String text) {
                var b = RichParagraph.builder();
                int len = text.length();
                int start = 0;
                for (int i = 0; i < len; i++) {
                    char c = text.charAt(i);
                    if ((c < 0x20) || (c > 0x7f)) {
                        if (start < i) {
                            b.addSegment(text.substring(start, i), style);
                        }
                        b.addSegment(escape(c), UNICODE);
                        start = i + 1;
                    }
                }
                if (start < len) {
                    b.addSegment(text.substring(start), style);
                }
                return b.build();
            }

            private String escape(int c) {
                return String.format("\\u%04x", c);
            }
        };
    }

    public static TextViewModel ofBytes(byte[] bytes) {
        return new TextViewModel() {
            private final StringBuilder sb = new StringBuilder(WIDTH * 3);

            @Override
            public String getPlainText(int index) {
                int off = index * WIDTH;
                int max = Math.min(bytes.length, off + WIDTH);
                int i = 0;
                for ( ; i < WIDTH; i++) {
                    if (off >= bytes.length) {
                        break;
                    }
                    if (i > 0) {
                        sb.append(' ');
                    }
                    sb.append(String.format("%02x", (0xff & bytes[off])));
                    if ((i & 0x07) == 0x07) {
                        sb.append(' ');
                    }
                    off++;
                }

                // separator
                int ct = 2 + ((WIDTH - i) * 3) + (i < (WIDTH / 2) ? 2 : 0); 
                for(int j=0; j<ct; j++) {
                    sb.append(' ');
                }

                // ascii block
                off = index * WIDTH;
                for (i = 0; i < WIDTH; i++) {
                    if (off >= bytes.length) {
                        break;
                    }
                    int c = (0xff & bytes[off]);
                    if ((c < 0x20) || (c > 0x7f)) {
                        c = '.';
                    }
                    sb.append((char)c);
                    off++;
                }
                String s = sb.toString();
                sb.setLength(0);
                return s;
            }

            @Override
            public int size() {
                return (bytes.length / WIDTH) + 1;
            }

            @Override
            public RichParagraph getParagraph(int index) {
                String s = getPlainText(index);
                var b = RichParagraph.builder();
                b.addSegment(s, PLAIN);
                return b.build();
            }
        };
    }

    private TextViewModel() {
    }

    @Override
    public StyleAttributeMap getStyleAttributeMap(StyleResolver resolver, TextPos pos, boolean forInsert) {
        return null;
    }
}
