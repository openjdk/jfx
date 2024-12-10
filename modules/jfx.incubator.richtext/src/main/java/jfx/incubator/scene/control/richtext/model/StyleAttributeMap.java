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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import com.sun.jfx.incubator.scene.control.richtext.CssStyles;
import com.sun.jfx.incubator.scene.control.richtext.StyleAttributeMapHelper;

/**
 * This immutable object contains a map of {@link StyleAttribute}s.
 *
 * @since 24
 */
public final class StyleAttributeMap {
    /** Paragraph background color attribute. */
    public static final StyleAttribute<Color> BACKGROUND = new StyleAttribute<>("BACKGROUND", Color.class, true);

    /** Bullet point paragraph attribute. */
    public static final StyleAttribute<String> BULLET = new StyleAttribute<>("BULLET", String.class, true);

    /** Bold typeface text attribute. */
    public static final StyleAttribute<Boolean> BOLD = new StyleAttribute<>("BOLD", Boolean.class, false);

    /** First line indent paragraph attribute, in pixels. */
    public static final StyleAttribute<Double> FIRST_LINE_INDENT = new StyleAttribute<>("FIRST_LINE_INDENT", Double.class, true);

    /** Font family text attribute. */
    public static final StyleAttribute<String> FONT_FAMILY = new StyleAttribute<>("FONT_FAMILY", String.class, false);

    /** Font size text attribute, in pixels. */
    public static final StyleAttribute<Double> FONT_SIZE = new StyleAttribute<>("FONT_SIZE", Double.class, false);

    /** Italic type face text attribute. */
    public static final StyleAttribute<Boolean> ITALIC = new StyleAttribute<>("ITALIC", Boolean.class, false);

    /** Line spacing paragraph attribute. */
    public static final StyleAttribute<Double> LINE_SPACING = new StyleAttribute<>("LINE_SPACING", Double.class, true);

    /** Paragraph direction attribute.  This attribute is considered only when text wrapping is enabled. */
    public static final StyleAttribute<ParagraphDirection> PARAGRAPH_DIRECTION = new StyleAttribute<>("PARAGRAPH_DIRECTION", ParagraphDirection.class, true);

    /** Space above (top padding) paragraph attribute. */
    public static final StyleAttribute<Double> SPACE_ABOVE = new StyleAttribute<>("SPACE_ABOVE", Double.class, true);

    /** Space below (bottom padding) paragraph attribute. */
    public static final StyleAttribute<Double> SPACE_BELOW = new StyleAttribute<>("SPACE_BELOW", Double.class, true);

    /** Space to the left (left padding) paragraph attribute. */
    public static final StyleAttribute<Double> SPACE_LEFT = new StyleAttribute<>("SPACE_LEFT", Double.class, true);

    /** Space to the right (right padding) paragraph attribute. */
    public static final StyleAttribute<Double> SPACE_RIGHT = new StyleAttribute<>("SPACE_RIGHT", Double.class, true);

    /** Strike-through text attribute. */
    public static final StyleAttribute<Boolean> STRIKE_THROUGH = new StyleAttribute<>("STRIKE_THROUGH", Boolean.class, false);

    /** Text alignment paragraph attribute. */
    public static final StyleAttribute<TextAlignment> TEXT_ALIGNMENT = new StyleAttribute<>("TEXT_ALIGNMENT", TextAlignment.class, true);

    /** Text color attribute. */
    public static final StyleAttribute<Color> TEXT_COLOR = new StyleAttribute<>("TEXT_COLOR", Color.class, false);

    /** Underline text attribute. */
    public static final StyleAttribute<Boolean> UNDERLINE = new StyleAttribute<>("UNDERLINE", Boolean.class, false);

    /** Empty attribute set. */
    public static final StyleAttributeMap EMPTY = new StyleAttributeMap(Collections.emptyMap());

    private static final String[] EMPTY_ARRAY = new String[0];

    private final Map<StyleAttribute<?>,Object> attributes;
    static { initAccessor(); }

    private StyleAttributeMap(Map<StyleAttribute<?>,Object> a) {
        this.attributes = Collections.unmodifiableMap(a);
    }

    /**
     * Convenience method creates the instance with a single attribute.
     *
     * @param <V> the attribute value type
     * @param attribute the attribute
     * @param value the attribute value
     * @return the new instance
     */
    public static <V> StyleAttributeMap of(StyleAttribute<V> attribute, V value) {
        return new Builder().set(attribute, value).build();
    }

    /**
     * This convenience method creates an instance from an inline style and a number of
     * CSS style names.
     *
     * @param style the inline style, will not be applied when null
     * @param names style names
     * @return the new instance
     */
    static StyleAttributeMap fromStyles(String style, String... names) {
        if ((style == null) && (names == null)) {
            return StyleAttributeMap.EMPTY;
        } else if (names == null) {
            names = EMPTY_ARRAY;
        }
        return new Builder().set(CssStyles.CSS, new CssStyles(style, names)).build();
    }

    /**
     * This convenience method creates an instance from an inline style.
     *
     * @param style the inline style, can be null
     * @return the new instance
     */
    static StyleAttributeMap fromInlineStyle(String style) {
        if (style == null) {
            return StyleAttributeMap.EMPTY;
        }
        return new Builder().set(CssStyles.CSS, new CssStyles(style, null)).build();
    }

    @Override
    public boolean equals(Object x) {
        if (x == this) {
            return true;
        } else if (x instanceof StyleAttributeMap s) {
            return attributes.equals(s.attributes);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return attributes.hashCode() + (31 * StyleAttributeMap.class.hashCode());
    }

    /**
     * Returns {@code true} if this instance contains no attributes.
     * @return true is no attributes are present
     */
    public boolean isEmpty() {
        return attributes.isEmpty();
    }

    /**
     * Returns the attribute value, or null if no such attribute is present.
     *
     * @param <V> the attribute value type
     * @param a attribute
     * @return attribute value or null
     */
    public <V> V get(StyleAttribute<V> a) {
        return (V)attributes.get(a);
    }

    /**
     * Returns the set of {@link StyleAttribute}s.
     * @return attribute set
     */
    public Set<StyleAttribute<?>> getAttributes() {
        return attributes.keySet();
    }

    /**
     * Returns an immutable {@link Set} view of the mappings contained in this map.
     * @return a set view of the mappings contained in this attribute map
     */
    public Set<Map.Entry<StyleAttribute<?>, Object>> getAttributeEntrySet() {
        return attributes.entrySet();
    }

    /**
     * Returns true if the attribute is present; false otherwise.
     *
     * @param a the attribute
     * @return true if the attribute is present
     */
    public boolean contains(StyleAttribute<?> a) {
        return attributes.containsKey(a);
    }

    /**
     * Creates a new StyleAttributeMap instance by first copying attributes from this instance,
     * then adding (and/or overwriting) the attributes from the specified instance.
     *
     * @param attrs the attributes to combine
     * @return the new instance combining the attributes
     */
    public StyleAttributeMap combine(StyleAttributeMap attrs) {
        return new Builder().
            merge(this).
            merge(attrs).
            build();
    }

    /**
     * Returns true if the specified attribute contains {@code Boolean.TRUE},
     * false in any other case.
     *
     * @param a the attribute
     * @return true if the attribute value is {@code Boolean.TRUE}
     */
    public boolean getBoolean(StyleAttribute<Boolean> a) {
        Object v = attributes.get(a);
        return Boolean.TRUE.equals(v);
    }

    /**
     * Returns the value of the specified attribute, or defaultValue if the specified attribute
     * is not present or is not a {@link Number}.
     *
     * @param a the attribute
     * @param defaultValue the default value
     * @return the attribute value
     */
    public double getDouble(StyleAttribute<? extends Number> a, double defaultValue) {
        Object v = attributes.get(a);
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return defaultValue;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("{");
        boolean sep = false;
        for (StyleAttribute<?> a : attributes.keySet()) {
            if (sep) {
                sb.append(",");
            } else {
                sep = true;
            }
            Object v = get(a);
            sb.append(a);
            sb.append('=');
            sb.append(v);
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * This convenience method returns the value of {@link #BACKGROUND} attribute, or null.
     * @return the background color attribute value
     */
    public Color getBackground() {
        return get(BACKGROUND);
    }

    /**
     * This convenience method returns the value of {@link #BULLET} attribute, or null.
     * @return the bullet paragraph attribute value
     */
    public String getBullet() {
        return get(BULLET);
    }

    /**
     * This convenience method returns the value of the {@link #FIRST_LINE_INDENT} attribute.
     * @return the first line indent value in points
     */
    public Double getFirstLineIndent() {
        return get(FIRST_LINE_INDENT);
    }

    /**
     * This convenience method returns the value of the {@link #FONT_SIZE} attribute.
     * @return the font size
     */
    public final Double getFontSize() {
        return get(FONT_SIZE);
    }

    /**
     * This convenience method returns true if the value of the {@link #FONT_FAMILY} attribute is {@code Boolean.TRUE},
     * false otherwise.
     * @return the font family name
     */
    public String getFontFamily() {
        return get(FONT_FAMILY);
    }

    /**
     * This convenience method returns the value of the {@link #LINE_SPACING} attribute, or null.
     * @return the line spacing value
     */
    public Double getLineSpacing() {
        return get(LINE_SPACING);
    }

    /**
     * This convenience method returns the value of the {@link #SPACE_ABOVE} attribute, or null.
     * @return the space above paragraph attribute value
     */
    public Double getSpaceAbove() {
        return get(SPACE_ABOVE);
    }

    /**
     * This convenience method returns the value of the {@link #SPACE_BELOW} attribute, or null.
     * @return the space below paragraph attribute value
     */
    public Double getSpaceBelow() {
        return get(SPACE_BELOW);
    }

    /**
     * This convenience method returns the value of the {@link #SPACE_LEFT} attribute, or null.
     * @return the space left paragraph attribute value
     */
    public Double getSpaceLeft() {
        return get(SPACE_LEFT);
    }

    /**
     * This convenience method returns the value of the {@link #SPACE_RIGHT} attribute, or null.
     * @return the space right paragraph attribute value
     */
    public Double getSpaceRight() {
        return get(SPACE_RIGHT);
    }

    /**
     * This convenience method returns the value of {@link #TEXT_ALIGNMENT} attribute, or null.
     * @return the paragraph alignment attribute value
     */
    public TextAlignment getTextAlignment() {
        return get(TEXT_ALIGNMENT);
    }

    /**
     * This convenience method returns the value of {@link #TEXT_COLOR} attribute, or null.
     * @return the text color attribute value
     */
    public Color getTextColor() {
        return get(TEXT_COLOR);
    }

    /**
     * This convenience method returns true if the value of {@link #BOLD} attribute is {@code Boolean.TRUE},
     * false otherwise.
     * @return the bold attribute value
     */
    public boolean isBold() {
        return getBoolean(BOLD);
    }

    /**
     * This convenience method returns true if the value of {@link #ITALIC} attribute is {@code Boolean.TRUE},
     * false otherwise.
     * @return the italic attribute value
     */
    public boolean isItalic() {
        return getBoolean(ITALIC);
    }

    /**
     * This convenience method returns the value of {@link #PARAGRAPH_DIRECTION} paragraph attribute,
     * or null if the value is not set.
     * @return the paragraph direction attribute value, or null
     */
    public ParagraphDirection getParagraphDirection() {
        return get(PARAGRAPH_DIRECTION);
    }

    /**
     * This convenience method returns true if the value of {@link #STRIKE_THROUGH} attribute is {@code Boolean.TRUE},
     * false otherwise.
     * @return the strike through attribute value
     */
    public boolean isStrikeThrough() {
        return getBoolean(STRIKE_THROUGH);
    }

    /**
     * This convenience method returns true if the value of {@link #UNDERLINE} attribute is {@code Boolean.TRUE},
     * false otherwise.
     * @return the underline attribute value
     */
    public boolean isUnderline() {
        return getBoolean(UNDERLINE);
    }

    private StyleAttributeMap filterAttributes(boolean isParagraph) {
        Builder b = null;
        for (StyleAttribute<?> a : attributes.keySet()) {
            if (a.isParagraphAttribute() == isParagraph) {
                if (b == null) {
                    b = StyleAttributeMap.builder();
                }
                Object v = attributes.get(a);
                b.setUnguarded(a, v);
            }
        }
        return (b == null) ? null : b.build();
    }

    private static void initAccessor() {
        StyleAttributeMapHelper.setAccessor(new StyleAttributeMapHelper.Accessor() {
            @Override
            public StyleAttributeMap filterAttributes(StyleAttributeMap ss, boolean isParagraph) {
                return ss.filterAttributes(isParagraph);
            }
        });
    }

    /**
     * Creates a new Builder instance.
     * @return the new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /** StyleAttributeMap are immutable, so a Builder is required to create a new instance */
    public static class Builder {
        private final HashMap<StyleAttribute<?>,Object> attributes = new HashMap<>(4);

        private Builder() {
        }

        /**
         * Creates an immutable instance of {@link StyleAttributeMap} with the attributes set by this Builder.
         * @return the new instance
         */
        public StyleAttributeMap build() {
            return new StyleAttributeMap(attributes);
        }

        /**
         * Sets the value for the specified attribute.
         * This method will throw an {@code IllegalArgumentException} if the value cannot be cast to the
         * type specified by the attribute.
         *
         * @param <V> the attribute value type
         * @param a the attribute
         * @param value the attribute value
         * @return this Builder instance
         */
        public <V> Builder set(StyleAttribute<V> a, V value) {
            if (value == null) {
                attributes.put(a, null);
            } else if (value.getClass().isAssignableFrom(a.getType())) {
                attributes.put(a, value);
            } else {
                throw new IllegalArgumentException(a + " requires value of type " + a.getType());
            }
            return this;
        }

        private Builder setUnguarded(StyleAttribute<?> a, Object value) {
            attributes.put(a, value);
            return this;
        }

        /**
         * Merges the specified attributes with the attributes in this instance.
         * The new values override any existing ones.
         * @param attrs the attributes to merge, may be null
         * @return this Builder instance
         */
        public Builder merge(StyleAttributeMap attrs) {
            if (attrs != null) {
                for (StyleAttribute<?> a : attrs.attributes.keySet()) {
                    Object v = attrs.get(a);
                    setUnguarded(a, v);
                }
            }
            return this;
        }

        /**
         * Sets the paragraph background attribute to the specified color.
         * It is recommended to specify a translucent background color in order to avoid obstructing
         * the selection and the current line highlights.
         * @param color the color
         * @return this Builder instance
         */
        public Builder setBackground(Color color) {
            set(BACKGROUND, color);
            return this;
        }

        /**
         * Sets the bold attribute.
         * @param on true for bold typeface
         * @return this Builder instance
         */
        public Builder setBold(boolean on) {
            set(BOLD, Boolean.valueOf(on));
            return this;
        }

        /**
         * Sets the BULLET attribute.
         * @param bullet the bullet character
         * @return this Builder instance
         */
        public Builder setBullet(String bullet) {
            set(BULLET, bullet);
            return this;
        }

        /**
         * Sets the FIRST_LINE_INDENT attribute.
         * @param size the first line indent value
         * @return this Builder instance
         */
        public Builder setFirstLineIndent(double size) {
            set(FIRST_LINE_INDENT, size);
            return this;
        }

        /**
         * Sets the font family attribute.
         * @param name the font family name
         * @return this Builder instance
         */
        public Builder setFontFamily(String name) {
            set(FONT_FAMILY, name);
            return this;
        }

        /**
         * Sets the font size attribute.
         * @param size the font size in points
         * @return this Builder instance
         */
        public Builder setFontSize(double size) {
            set(FONT_SIZE, size);
            return this;
        }

        /**
         * Sets the line spacing paragraph attribute.
         * @param value the line spacing value
         * @return this Builder instance
         */
        public Builder setLineSpacing(double value) {
            set(LINE_SPACING, value);
            return this;
        }

        /**
         * Sets the italic attribute.
         * @param on true for italic typeface
         * @return this Builder instance
         */
        public Builder setItalic(boolean on) {
            set(ITALIC, Boolean.valueOf(on));
            return this;
        }

        /**
         * Sets the paragraph direction attribute.
         * @param d the paragraph direction
         * @return this Builder instance
         */
        public Builder setRTL(ParagraphDirection d) {
            set(PARAGRAPH_DIRECTION, d);
            return this;
        }

        /**
         * Sets the space above paragraph attribute.
         * This method also sets SPACE attribute to Boolean.TRUE.
         * @param value the space amount
         * @return this Builder instance
         */
        public Builder setSpaceAbove(double value) {
            set(SPACE_ABOVE, value);
            return this;
        }

        /**
         * Sets the space below paragraph attribute.
         * This method also sets SPACE attribute to Boolean.TRUE.
         * @param value the space amount
         * @return this Builder instance
         */
        public Builder setSpaceBelow(double value) {
            set(SPACE_BELOW, value);
            return this;
        }

        /**
         * Sets the space left paragraph attribute.
         * This method also sets SPACE attribute to Boolean.TRUE.
         * @param value the space amount
         * @return this Builder instance
         */
        public Builder setSpaceLeft(double value) {
            set(SPACE_LEFT, value);
            return this;
        }

        /**
         * Sets the space right paragraph attribute.
         * This method also sets SPACE attribute to Boolean.TRUE.
         * @param value the space amount
         * @return this Builder instance
         */
        public Builder setSpaceRight(double value) {
            set(SPACE_RIGHT, value);
            return this;
        }

        /**
         * Sets the strike-through attribute.
         * @param on true for strike-through typeface
         * @return this Builder instance
         */
        public Builder setStrikeThrough(boolean on) {
            set(STRIKE_THROUGH, Boolean.valueOf(on));
            return this;
        }

        /**
         * Sets the text alignment attribute to the specified color.
         * @param a the alignment
         * @return this Builder instance
         */
        public Builder setTextAlignment(TextAlignment a) {
            set(TEXT_ALIGNMENT, a);
            return this;
        }

        /**
         * Sets the text color attribute to the specified color.
         * @param color the color
         * @return this Builder instance
         */
        public Builder setTextColor(Color color) {
            set(TEXT_COLOR, color);
            return this;
        }

        /**
         * Sets the underline attribute.
         * @param on true for underline
         * @return this Builder instance
         */
        public Builder setUnderline(boolean on) {
            set(UNDERLINE, Boolean.valueOf(on));
            return this;
        }
    }
}
