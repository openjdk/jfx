/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.demo.rich.notebook;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;
import com.oracle.demo.rich.codearea.JavaSyntaxAnalyzer.Line;
import com.oracle.demo.rich.codearea.JavaSyntaxAnalyzer.Type;
import jfx.incubator.scene.control.rich.CodeTextModel;
import jfx.incubator.scene.control.rich.SyntaxDecorator;
import jfx.incubator.scene.control.rich.TextPos;
import jfx.incubator.scene.control.rich.model.RichParagraph;
import jfx.incubator.scene.control.rich.model.StyleAttrs;

/**
 * Super simple (and therefore not always correct) syntax decorator for JSON
 * which works one line at a time. 
 */
public class SimpleJsonDecorator implements SyntaxDecorator {
    private static final StyleAttrs NORMAL = mkStyle(Color.BLACK);
    private static final StyleAttrs NUMBER = mkStyle(Color.MAGENTA);
    private static final StyleAttrs STRING = mkStyle(Color.BLUE);

    public SimpleJsonDecorator() {
    }

    @Override
    public void handleChange(CodeTextModel m, TextPos start, TextPos end, int top, int added, int bottom) {
    }

    @Override
    public RichParagraph createRichParagraph(CodeTextModel model, int index) {
        String text = model.getPlainText(index);
        List<Seg> segments = new Analyzer(text).parse();
        RichParagraph.Builder b = RichParagraph.builder();
        for (Seg seg : segments) {
            b.addSegment(seg.text, seg.style);
        }
        return b.build();
    }

    private static StyleAttrs mkStyle(Color c) {
        return StyleAttrs.builder().setTextColor(c).build();
    }

    private static record Seg(StyleAttrs style, String text) {
    }

    private enum State {
        NUMBER,
        STRING,
        TEXT,
        VALUE,
    }

    private static class Analyzer {
        private final String text;
        private final ArrayList<Seg> segments = new ArrayList<>();
        private static final int EOF = -1;
        private int start;
        private int pos;
        private State state = State.TEXT;

        public Analyzer(String text) {
            this.text = text;
        }

        private int peek(int delta) {
            int ix = pos + delta;
            if ((ix >= 0) && (ix < text.length())) {
                return text.charAt(ix);
            }
            return EOF;
        }

        private void addSegment() {
            StyleAttrs type = toStyleAttrs(state);
            addSegment(type);
        }

        private StyleAttrs toStyleAttrs(State s) {
            switch (s) {
            case STRING:
                return STRING;
            case NUMBER:
                return NUMBER;
            case VALUE:
            default:
                return NORMAL;
            }
        }

        private void addSegment(StyleAttrs style) {
            if (pos > start) {
                String s = text.substring(start, pos);
                segments.add(new Seg(style, s));
                start = pos;
            }
        }

        private Error err(String text) {
            return new Error(text + " state=" + state + " pos=" + pos);
        }

        private int parseNumber() {
            int ix = indexOfNonNumber();
            if (ix < 0) {
                return 0;
            }
            String s = text.substring(pos, pos + ix);
            try {
                Double.parseDouble(s);
                return ix;
            } catch (NumberFormatException e) {
            }
            return 0;
        }

        private int indexOfNonNumber() {
            int i = 0;
            for (;;) {
                int c = peek(i);
                switch (c) {
                case EOF:
                    return i;
                // we'll parse integers only for now case '.':
                case '-':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    i++;
                    continue;
                default:
                    return i;
                }
            }
        }

        public List<Seg> parse() {
            start = 0;
            for (;;) {
                int c = peek(0);
                switch (c) {
                case EOF:
                    addSegment();
                    return segments;
                case '"':
                    switch (state) {
                    case TEXT:
                    case VALUE:
                        addSegment();
                        state = State.STRING;
                        pos++;
                        break;
                    case STRING:
                        pos++;
                        addSegment();
                        state = State.TEXT;
                        break;
                    default:
                        throw err("state must be either TEXT, STRING, or VALUE");
                    }
                    break;
                case '=':
                    state = State.VALUE;
                    break;
                //case '.':
                case '-':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    switch (state) {
                    case VALUE:
                        int len = parseNumber();
                        if (len > 0) {
                            addSegment();
                            state = State.NUMBER;
                            pos += len;
                            addSegment();
                        }
                        break;
                    }
                    break;
                default:
                    break;
                }

                pos++;
            }
        }
    }
}
