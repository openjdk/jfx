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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import javafx.scene.input.DataFormat;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;
import com.sun.jfx.incubator.scene.control.richtext.Converters;
import com.sun.jfx.incubator.scene.control.richtext.RichTextFormatHandlerHelper;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;

/**
 * A DataFormatHandler for use with attribute-based rich text models.
 * <p>
 * The handler uses a simple text-based format:<p>
 * (*) denotes an optional element.
 * <pre>
 * PARAGRAPH[]
 *
 * PARAGRAPH: {
 *     PARAGRAPH_ATTRIBUTE[]*,
 *     TEXT_SEGMENT[],
 *     "\n"
 * }
 *
 * PARAGRAPH_ATTRIBUTE: {
 *     "{!"
 *     (name)
 *     ATTRIBUTE_VALUE[]*
 *     "}"
 * }
 *
 * ATTRIBUTE: {
 *     "{"
 *     (name)
 *     ATTRIBUTE_VALUE[]*
 *     "}"
 * }
 *
 * ATTRIBUTE_VALUE: {
 *     |
 *     (value)
 * }
 *
 * TEXT_SEGMENT: {
 *     ATTRIBUTE[]*
 *     (text string with escaped special characters)
 * }
 * </pre>
 * Attribute sequences are further deduplicated, using a single {number} token
 * which specifies the index into the list of unique sets of attributes.
 * Paragraph attribute sets are treated as separate from the segment attrubite sets.
 * <p>
 * The following characters are escaped in text segments: {,%,}
 * The escape format is %XX where XX is a hexadecimal value.
 * <p>
 * Example:
 * <pre>
 * {c|ff00ff}text{b}bold{!rtl}\n
 * {1}line 2{!0}\n
 * </pre>
 *
 * @since 24
 */
public class RichTextFormatHandler extends DataFormatHandler {
    static { initAccessor(); }

    private static final boolean DEBUG = false;

    /** The data format identifier */
    public static final DataFormat DATA_FORMAT = new DataFormat("application/x-com-oracle-editable-rich-text");

    private static final StringConverter<Boolean> BOOLEAN_CONVERTER = Converters.booleanConverter();
    private static final StringConverter<Color> COLOR_CONVERTER = Converters.colorConverter();
    private static final StringConverter<ParagraphDirection> DIRECTION_CONVERTER = Converters.paragraphDirectionConverter();
    private static final DoubleStringConverter DOUBLE_CONVERTER = new DoubleStringConverter();
    private static final StringConverter<String> STRING_CONVERTER = Converters.stringConverter();
    private static final StringConverter<TextAlignment> TEXT_ALIGNMENT_CONVERTER = Converters.textAlignmentConverter();
    // String -> Handler
    // StyleAttribute -> Handler
    private final HashMap<Object,Handler> handlers = new HashMap<>(64);
    private static final RichTextFormatHandler instance = new RichTextFormatHandler();

    /**
     * Constructor.
     */
    private RichTextFormatHandler() {
        super(DATA_FORMAT);

        addHandlerBoolean(StyleAttributeMap.BOLD, "b");
        addHandler(StyleAttributeMap.BACKGROUND, "bg", COLOR_CONVERTER);
        addHandlerString(StyleAttributeMap.BULLET, "bullet");
        addHandlerString(StyleAttributeMap.FONT_FAMILY, "ff");
        addHandler(StyleAttributeMap.FIRST_LINE_INDENT, "firstIndent", DOUBLE_CONVERTER);
        addHandler(StyleAttributeMap.FONT_SIZE, "fs", DOUBLE_CONVERTER);
        addHandlerBoolean(StyleAttributeMap.ITALIC, "i");
        addHandler(StyleAttributeMap.LINE_SPACING, "lineSpacing", DOUBLE_CONVERTER);
        addHandler(StyleAttributeMap.PARAGRAPH_DIRECTION, "dir", DIRECTION_CONVERTER);
        addHandler(StyleAttributeMap.SPACE_ABOVE, "spaceAbove", DOUBLE_CONVERTER);
        addHandler(StyleAttributeMap.SPACE_BELOW, "spaceBelow", DOUBLE_CONVERTER);
        addHandler(StyleAttributeMap.SPACE_LEFT, "spaceLeft", DOUBLE_CONVERTER);
        addHandler(StyleAttributeMap.SPACE_RIGHT, "spaceRight", DOUBLE_CONVERTER);
        addHandlerBoolean(StyleAttributeMap.STRIKE_THROUGH, "ss");
        addHandler(StyleAttributeMap.TEXT_ALIGNMENT, "alignment", TEXT_ALIGNMENT_CONVERTER);
        addHandler(StyleAttributeMap.TEXT_COLOR, "tc", COLOR_CONVERTER);
        addHandlerBoolean(StyleAttributeMap.UNDERLINE, "u");
    }

    /**
     * Returns the singleton instance of {@code RtfFormatHandler}.
     * @return the singleton instance of {@code RtfFormatHandler}
     */
    public static final RichTextFormatHandler getInstance() {
        return instance;
    }

    private static void initAccessor() {
        RichTextFormatHandlerHelper.setAccessor(new RichTextFormatHandlerHelper.Accessor() {
            @Override
            public StyledOutput createStyledOutput(RichTextFormatHandler h, StyleResolver r, Writer wr) {
                return h.createStyledOutput(r, wr);
            }
        });
    }

    @Override
    public StyledInput createStyledInput(String input, StyleAttributeMap attr) {
        return new RichStyledInput(input);
    }

    @Override
    public Object copy(StyledTextModel m, StyleResolver r, TextPos start, TextPos end) throws IOException {
        StringWriter wr = new StringWriter();
        StyledOutput so = createStyledOutput(r, wr);
        m.export(start, end, so);
        return wr.toString();
    }

    @Override
    public void save(StyledTextModel m, StyleResolver r, TextPos start, TextPos end, OutputStream out) throws IOException {
        Charset cs = Charset.forName("utf-8");
        Writer wr = new OutputStreamWriter(out, cs);
        StyledOutput so = createStyledOutput(r, wr);
        m.export(start, end, so);
    }

    private StyledOutput createStyledOutput(StyleResolver r, Writer wr) {
        Charset cs = Charset.forName("utf-8");
        boolean buffered = isBuffered(wr);
        if (buffered) {
            return new RichStyledOutput(r, wr);
        } else {
            wr = new BufferedWriter(wr);
            return new RichStyledOutput(r, wr);
        }
    }

    private static boolean isBuffered(Writer x) {
        return
            (x instanceof BufferedWriter) ||
            (x instanceof StringWriter);
    }

    /** attribute handler */
    static class Handler<T> {
        private final String id;
        private final StyleAttribute<T> attribute;
        private final StringConverter<T> converter;

        public Handler(StyleAttribute<T> attribute, String id, StringConverter<T> converter) {
            this.id = id;
            this.attribute = attribute;
            this.converter = converter;
        }

        public String getId() {
            return id;
        }

        public StyleAttribute<T> getStyleAttribute() {
            return attribute;
        }

        public boolean isAllowed(T value) {
            return true;
        }

        public String write(T value) {
            return converter.toString(value);
        }

        public T read(String s) {
            return converter.fromString(s);
        }
    }

    private <T> void addHandler(StyleAttribute<T> a, String id, StringConverter<T> converter) {
        addHandler(new Handler<T>(a, id, converter));
    }

    private <T> void addHandler(Handler<T> h) {
        handlers.put(h.getStyleAttribute(), h);
        handlers.put(h.getId(), h);
    }

    private void addHandlerBoolean(StyleAttribute<Boolean> a, String id) {
        addHandler(new Handler<Boolean>(a, id, BOOLEAN_CONVERTER) {
            @Override
            public boolean isAllowed(Boolean value) {
                return Boolean.TRUE.equals(value);
            }
        });
    }

    private void addHandlerString(StyleAttribute<String> a, String id) {
        addHandler(new Handler<String>(a, id, STRING_CONVERTER));
    }

    private static void log(Object x) {
        if (DEBUG) {
            System.err.println(x);
        }
    }

    /** exporter */
    private class RichStyledOutput implements StyledOutput {
        private final StyleResolver resolver;
        private final Writer wr;
        private HashMap<StyleAttributeMap, Integer> styles = new HashMap<>();

        public RichStyledOutput(StyleResolver r, Writer wr) {
            this.resolver = r;
            this.wr = wr;
        }

        @Override
        public void consume(StyledSegment seg) throws IOException {
            switch (seg.getType()) {
            case INLINE_NODE:
                // TODO
                log("ignoring embedded node");
                break;
            case LINE_BREAK:
                wr.write("\n");
                break;
            case PARAGRAPH_ATTRIBUTES:
                {
                    StyleAttributeMap attrs = seg.getStyleAttributeMap(resolver);
                    emitAttributes(attrs, true);
                }
                break;
            case REGION:
                // TODO
                break;
            case TEXT:
                {
                    StyleAttributeMap attrs = seg.getStyleAttributeMap(resolver);
                    emitAttributes(attrs, false);

                    String text = seg.getText();
                    text = encode(text);
                    wr.write(text);
                }
                break;
            }
        }

        private void emitAttributes(StyleAttributeMap attrs, boolean forParagraph) throws IOException {
            if ((attrs != null) && (!attrs.isEmpty())) {
                Integer num = styles.get(attrs);
                if (num == null) {
                    // new style, gets numbered and added to the cache
                    int sz = styles.size();
                    styles.put(attrs, Integer.valueOf(sz));

                    ArrayList<StyleAttribute<?>> as = new ArrayList<>(attrs.getAttributes());
                    // sort by name to make serialized output stable
                    // the overhead is very low since this is done once per style
                    Collections.sort(as, new Comparator<StyleAttribute<?>>() {
                        @Override
                        public int compare(StyleAttribute<?> a, StyleAttribute<?> b) {
                            String sa = a.getName();
                            String sb = b.getName();
                            return sa.compareTo(sb);
                        }
                    });

                    for (StyleAttribute<?> a : as) {
                        Handler h = handlers.get(a);
                        try {
                            if (h != null) {
                                Object v = attrs.get(a);
                                if (h.isAllowed(v)) {
                                    wr.write('{');
                                    if (forParagraph) {
                                        wr.write('!');
                                    }
                                    wr.write(h.getId());
                                    String ss = h.write(v);
                                    if (ss != null) {
                                        wr.write('|');
                                        wr.write(encode(ss));
                                    }
                                    wr.write('}');
                                }
                                continue;
                            }
                        } catch (Exception e) {
                            log(e);
                        }
                        // ignoring this attribute
                        log("failed to emit " + a + ", skipping");
                    }
                } else {
                    // cached style, emit the id
                    wr.write('{');
                    if (forParagraph) {
                        wr.write('!');
                    }
                    wr.write(String.valueOf(num));
                    wr.write('}');
                }
            } else if (forParagraph) {
                // this special token clears the paragraph attributes
                wr.write("{!}");
            }
        }

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
            if (ix > 0) {
                sb.append(text.substring(0, ix));
            }

            for (int i = ix; i < len; i++) {
                char c = text.charAt(i);
                if (isSpecialChar(c)) {
                    sb.append(String.format("%%%02X", (int)c));
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private static int indexOfSpecialChar(String text) {
            int len = text.length();
            for (int i = 0; i < len; i++) {
                char c = text.charAt(i);
                if (isSpecialChar(c)) {
                    return i;
                }
            }
            return -1;
        }

        private static boolean isSpecialChar(char c) {
            switch (c) {
            case '{':
            case '}':
            case '%':
            case '|':
                return true;
            }
            return false;
        }

        @Override
        public void flush() throws IOException {
            wr.flush();
        }

        @Override
        public void close() throws IOException {
            wr.close();
        }
    }

    /** importer */
    private class RichStyledInput implements StyledInput {
        private final String text;
        private int index;
        private StringBuilder sb;
        private final ArrayList<StyleAttributeMap> styles = new ArrayList<>();
        private int line = 1;

        public RichStyledInput(String text) {
            this.text = text;
        }

        @Override
        public StyledSegment nextSegment() {
            try {
                int c = charAt(0);
                switch (c) {
                case -1:
                    return null;
                case '\n':
                    index++;
                    line++;
                    return StyledSegment.LINE_BREAK;
                case '{':
                    StyleAttributeMap a = parseAttributes(true);
                    if (a != null) {
                        if (a.isEmpty()) {
                            a = null;
                        }
                        return StyledSegment.ofParagraphAttributes(a);
                    } else {
                        a = parseAttributes(false);
                        String text = decodeText();
                        return StyledSegment.of(text, a);
                    }
                }
                String text = decodeText();
                return StyledSegment.of(text);
            } catch (IOException e) {
                err(e);
                return null;
            }
        }

        @Override
        public void close() throws IOException {
        }

        private StyleAttributeMap parseAttributes(boolean forParagraph) throws IOException {
            StyleAttributeMap.Builder b = null;
            for (;;) {
                int c = charAt(0);
                if (c != '{') {
                    break;
                }
                c = charAt(1);
                if (forParagraph) {
                    if (c == '!') {
                        index++;
                    } else {
                        break;
                    }
                } else {
                    if (c == '!') {
                        throw err("unexpected paragraph attribute");
                    }
                }
                index++;

                int ix = text.indexOf('}', index);
                if (ix < 0) {
                    throw err("missing }");
                }
                String s = text.substring(index, ix);
                if (s.length() == 0) {
                    if (forParagraph) {
                        index = ix + 1;
                        // special token clears paragraph attributes
                        return StyleAttributeMap.EMPTY;
                    } else {
                        throw err("empty attribute name");
                    }
                }
                int n = parseStyleNumber(s);
                if (n < 0) {
                    // parse the attribute
                    String name;
                    String args;
                    int j = s.indexOf('|');
                    if (j < 0) {
                        name = s;
                        args = null;
                    } else {
                        name = s.substring(0, j);
                        args = s.substring(j + 1);
                    }

                    Handler h = handlers.get(name);
                    if (h == null) {
                        // silently ignore the attribute
                        log("ignoring attribute: " + name);
                    } else {
                        Object v = h.read(args);
                        StyleAttribute a = h.getStyleAttribute();
                        if (a.isParagraphAttribute() != forParagraph) {
                            throw err("paragraph type mismatch");
                        }
                        if (b == null) {
                            b = StyleAttributeMap.builder();
                        }
                        b.set(a, v);
                    }
                    index = ix + 1;
                } else {
                    index = ix + 1;
                    // get style from cache
                    return styles.get(n);
                }
            }
            if (b == null) {
                return null;
            }
            StyleAttributeMap attrs = b.build();
            styles.add(attrs);
            return attrs;
        }

        private int charAt(int delta) {
            int ix = index + delta;
            if (ix >= text.length()) {
                return -1;
            }
            return text.charAt(ix);
        }

        private String decodeText() throws IOException {
            int start = index;
            for(;;) {
                int c = charAt(0);
                switch(c) {
                case '\n':
                case '{':
                case -1:
                    return text.substring(start, index);
                case '%':
                    return decodeText(start, index);
                }
                index++;
            }
        }

        private String decodeText(int start, int ix) throws IOException {
            if (sb == null) {
                sb = new StringBuilder();
            }
            if (ix > start) {
                sb.append(text, start, ix);
            }
            for (;;) {
                int c = charAt(0);
                switch (c) {
                case '\n':
                case '{':
                case -1:
                    String s = sb.toString();
                    sb.setLength(0);
                    return s;
                case '%':
                    index++;
                    int ch = decodeHexByte();
                    sb.append((char)ch);
                    break;
                }
                index++;
            }
        }

        private int decodeHexByte() throws IOException {
            int ch = decodeHex(charAt(0)) << 4;
            index++;
            ch += decodeHex(charAt(0));
            return ch;
        }

        private static int decodeHex(int ch) throws IOException {
            int c = ch - '0'; // 0...9
            if ((c >= 0) && (c <= 9)) {
                return c;
            }
            c = ch - 55; // handle A...F
            if ((c >= 10) && (c <= 15)) {
                return c;
            }
            c = ch - 97; // handle a...f
            if ((c >= 10) && (c <= 15)) {
                return c;
            }
            throw new IOException("not a hex char:" + ch);
        }

        private int parseStyleNumber(String s) throws IOException {
            if (Character.isDigit(s.charAt(0))) {
                int n;
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    throw err("invalid style number " + s);
                }
            }
            return -1;
        }

        private IOException err(Object text) {
            return new IOException("malformed input: " + text + ", line " + line);
        }
    }
}
