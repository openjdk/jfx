/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.oracle.demo.richtext.notebook;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;
import com.oracle.demo.richtext.codearea.JavaSyntaxAnalyzer.Line;
import com.oracle.demo.richtext.codearea.JavaSyntaxAnalyzer.Type;
import jfx.incubator.scene.control.richtext.SyntaxDecorator;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.CodeTextModel;
import jfx.incubator.scene.control.richtext.model.RichParagraph;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * Super simple (and therefore not always correct) syntax decorator for JSON
 * which works one line at a time.
 *
 * @author Andy Goryachev
 */
public class SimpleJsonDecorator implements SyntaxDecorator {
    private static final StyleAttributeMap NORMAL = mkStyle(Color.BLACK);
    private static final StyleAttributeMap NUMBER = mkStyle(Color.MAGENTA);
    private static final StyleAttributeMap STRING = mkStyle(Color.BLUE);

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

    private static StyleAttributeMap mkStyle(Color c) {
        return StyleAttributeMap.builder().setTextColor(c).build();
    }

    private static record Seg(StyleAttributeMap style, String text) {
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
            StyleAttributeMap type = toStyleAttrs(state);
            addSegment(type);
        }

        private StyleAttributeMap toStyleAttrs(State s) {
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

        private void addSegment(StyleAttributeMap style) {
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
