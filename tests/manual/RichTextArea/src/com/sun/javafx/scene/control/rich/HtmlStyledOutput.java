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

package com.sun.javafx.scene.control.rich;

import java.io.IOException;
import java.io.Writer;
import java.util.Base64;
import java.util.HashMap;
import javafx.scene.Node;
import javafx.incubator.scene.control.rich.StyleResolver;
import javafx.incubator.scene.control.rich.model.StyleAttribute;
import javafx.incubator.scene.control.rich.model.StyleAttrs;
import javafx.incubator.scene.control.rich.model.StyledOutput;
import javafx.incubator.scene.control.rich.model.StyledSegment;
import javafx.incubator.scene.control.util.Util;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * A {@link StyledOutput} which generates HTML output.
 */
// TODO should 'monospaced' paragraphs use <pre> ?
// TODO should we size down font on windows?
public class HtmlStyledOutput implements StyledOutput {
    // a synthetic attribute used only in Key
    private static final StyleAttribute<Key> SS_AND_UNDERLINE = new StyleAttribute<>("SS_AND_UNDERLINE", Key.class, false);
    private final StyleResolver resolver;
    private final Writer wr;
    private final boolean inlineStyles;
    private record Key(StyleAttribute attr, Object value) { }
    private final HashMap<Key,Val> styles = new HashMap<>();

    public HtmlStyledOutput(StyleResolver resolver, Writer wr, boolean inlineStyles) {
        this.resolver = resolver;
        this.wr = wr;
        this.inlineStyles = inlineStyles;
    }

    @Override
    public void append(StyledSegment seg) throws IOException {
        switch (seg.getType()) {
        case INLINE_NODE:
            Node n = seg.getInlineNodeGenerator().get();
            writeInlineNode(n);
            break;
        case LINE_BREAK:
            // TODO perhaps use a boolean flag to emit separate p and /p tags
            wr.write("<p/>\n");
            break;
        case TEXT:
            StyleAttrs a = seg.getStyleAttrs(resolver);
            boolean div = ((a != null) && (!a.isEmpty()));
            if(div) {
                wr.write("<span style='");
                writeAttributes(a);
                wr.write("'>");
            }
            String text = seg.getText();
            String encoded = encode(text);
            wr.write(encoded);
            if(div) {
                wr.write("</span>");
            }
            break;
        case REGION:
            Region r = seg.getParagraphNodeGenerator().get();
            writeParagraph(r);
            break;
        }
    }

    private void writeAttributes(StyleAttrs attrs) throws IOException {
        boolean sp = false;
        for (StyleAttribute a : attrs.getAttributes()) {
            Object v = attrs.get(a);
            if (v != null) {
                Key k = createKey(attrs, a, v);
                if (k != null) {
                    if (sp) {
                        wr.write(' ');
                    } else {
                        sp = true;
                    }

                    Val val = styles.get(k);
                    if (inlineStyles) {
                        wr.write(val.css);
                    } else {
                        wr.write(val.name);
                    }
                }
            }
        }
    }

    /**
     * Special handing is required since STRIKE_THROUGH and UNDERLINE are mapped to
     * the same text-decoration CSS property.
     * @returns the new key or null if this attribute must be skipped.
     */
    private static Key createKey(StyleAttrs attrs, StyleAttribute a, Object v) {
        if (a == StyleAttrs.STRIKE_THROUGH) {
            if (attrs.isStrikeThrough() && attrs.isUnderline()) {
                a = SS_AND_UNDERLINE;
            }
        } else if (a == StyleAttrs.UNDERLINE) {
            if (attrs.isStrikeThrough() && attrs.isUnderline()) {
                return null;
            }
        }
        return new Key(a, v);
    }

    private void writeParagraph(Region n) throws IOException {
        WritableImage im = resolver.snapshot(n);
        byte[] bytes = Util.writePNG(im);
        int w = (int)im.getWidth();
        int h = (int)im.getHeight();
        byte[] b = Util.writePNG(im);
        String base64 = Base64.getEncoder().encodeToString(b);
        wr.write("<p><img src=\"data:image/png;base64,");
        wr.write(base64);
        wr.write("\" width=");
        wr.write(String.valueOf(w));
        wr.write(" height=");
        wr.write(String.valueOf(h));
        wr.write("></p>");
    }
    
    private void writeInlineNode(Node n) throws IOException {
        WritableImage im = resolver.snapshot(n);
        byte[] bytes = Util.writePNG(im);
        int w = (int)im.getWidth();
        int h = (int)im.getHeight();
        byte[] b = Util.writePNG(im);
        String base64 = Base64.getEncoder().encodeToString(b);
        wr.write("<img src=\"data:image/png;base64,");
        wr.write(base64);
        wr.write("\" width=");
        wr.write(String.valueOf(w));
        wr.write(" height=");
        wr.write(String.valueOf(h));
        wr.write(">");
    }
    
    // TODO unit test!
    private static String encode(String text) {
        if (text == null) {
            return "";
        }

        int ix = indexOfSpecialChar(text);
        if (ix < 0) {
            return text;
        }

        int len = text.length();
        StringBuilder sb = new StringBuilder(len + 32);
        sb.append(text, 0, ix);

        for (int i = ix; i < len; i++) {
            char c = text.charAt(i);
            if (c < 0x20) {
                switch (c) {
                case '\t':
                    sb.append("<pre>\t</pre>");
                    break;
                default:
                    sb.append("&#");
                    sb.append(nibbleChar(c >> 4));
                    sb.append(nibbleChar(c));
                    sb.append(';');
                }
            } else {
                switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&#34;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                default:
                    sb.append(c);
                    break;
                }
            }
        }

        return sb.toString();
    }

    private static int indexOfSpecialChar(String text) {
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c < 0x20) {
                return i;
            } else {
                switch (c) {
                case '<':
                case '>':
                case '"':
                case '\'':
                case '&':
                    return i;
                default:
                    continue;
                }
            }
        }
        return -1;
    }
    
    private static char nibbleChar(int x) {
        return "0123456789abcdef".charAt(x & 0x0f);
    }

    @Override
    public void flush() throws IOException {
        wr.flush();
    }
    
    @Override
    public void close() throws IOException {
        wr.close();
    }
    
    private static class Val {
        public final String name;
        public final String css;

        public Val(String name, String css) {
            this.name = name;
            this.css = css;
        }
    }

    public StyledOutput firstPassBuilder() {
        return new StyledOutput() {
            @Override
            public void append(StyledSegment seg) throws IOException {
                switch (seg.getType()) {
                case TEXT:
                    StyleAttrs attrs = seg.getStyleAttrs(resolver);
                    if ((attrs != null) && (!attrs.isEmpty())) {
                        for (StyleAttribute a : attrs.getAttributes()) {
                            Object v = attrs.get(a);
                            if (v != null) {
                                Key k = createKey(attrs, a, v);
                                if (k != null) {
                                    if (!styles.containsKey(k)) {
                                        String css = createCss(k.attr, v);
                                        if (css != null) {
                                            String name = ".S" + styles.size();
                                            Val val = new Val(name, css);
                                            styles.put(k, val);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
                case PARAGRAPH_ATTRIBUTES:
                    // TODO
                    break;
                }
            }

            @Override
            public void flush() throws IOException {
            }
            
            @Override
            public void close() throws IOException {
            }
        };
    }

    private static String createCss(StyleAttribute a, Object v) {
        if (a == StyleAttrs.BOLD) {
            return  "font-weight: bold;";
        } else if (a == StyleAttrs.FONT_FAMILY) {
            return "font-family: \"" + encodeFontFamily(v.toString()) + "\";";
        } else if (a == StyleAttrs.FONT_SIZE) {
            return "font-size: " + v + "pt;";
        } else if (a == StyleAttrs.ITALIC) {
            return  "font-style: italic;";
        } else if (a == StyleAttrs.STRIKE_THROUGH) {
            return "text-decoration: line-through;";
        } else if (a == StyleAttrs.TEXT_COLOR) {
            return "color: " + RichUtils.toWebColor((Color)v) + ";";
        } else if (a == StyleAttrs.UNDERLINE) {
            return "text-decoration: underline;";
        } else if(a == SS_AND_UNDERLINE) {
            return "text-decoration: line-through underline;";
        } else {
            return null;
        }
    }

    private static String encodeFontFamily(String name) {
        switch (name.toLowerCase()) {
        case "monospaced":
            return "monospace";
        case "system":
        case "sans-serif":
            return "sans-serif";
        case "serif":
            return "serif";
        case "cursive":
            return "cursive";
        case "fantasy":
            return "fantasy";
        }
        return encode(name);
    }

    public void writePrologue() throws IOException {
        wr.write("<html>");
        if (!inlineStyles) {
            wr.write("<head><style>\n");
            for (Val v : styles.values()) {
                wr.write(v.name);
                wr.write(" { ");
                wr.write(v.css);
                wr.write(" }\n");
            }
            wr.write("</style></head>\n");
        }
        wr.write("<body>\n");
    }

    public void writeEpilogue() throws IOException {
        wr.write("\n</body></html>\n");
    }
}
