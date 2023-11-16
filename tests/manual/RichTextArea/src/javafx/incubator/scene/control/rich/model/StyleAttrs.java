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

package javafx.incubator.scene.control.rich.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import com.sun.javafx.scene.control.rich.RichUtils;

/**
 * An immutable object containing style attributes.
 */
public class StyleAttrs {

    /** an instance with no attributes set */
    public static final StyleAttrs EMPTY = new StyleAttrs(Collections.emptyMap());

    /** Paragraph background attribute */
    public static final StyleAttribute<Color> BACKGROUND = new StyleAttribute<>("BACKGROUND", Color.class, true);

    /** Bullet point paragraph attribute */
    public static final StyleAttribute<String> BULLET = new StyleAttribute<>("BULLET", String.class, true);

    /** Bold typeface attribute */
    public static final StyleAttribute<Boolean> BOLD = new StyleAttribute<>("BOLD", Boolean.class, false);

    /** This special attribute contains CSS direct style and style names */
    public static final StyleAttribute<CssStyles> CSS = new StyleAttribute<>("CSS", CssStyles.class, false);

    /** First line indent paragraph attribute, in points. */
    public static final StyleAttribute<Double> FIRST_LINE_INDENT = new StyleAttribute<>("FIRST_LINE_INDENT", Double.class, false);

    /** Font family attribute */
    public static final StyleAttribute<String> FONT_FAMILY = new StyleAttribute<>("FONT_FAMILY", String.class, false);

    /** Font size attribute, in points. */
    public static final StyleAttribute<Double> FONT_SIZE = new StyleAttribute<>("FONT_SIZE", Double.class, false);

    /** Italic type face attribute */
    public static final StyleAttribute<Boolean> ITALIC = new StyleAttribute<>("ITALIC", Boolean.class, false);

    /** Line spacing paragraph attribute */
    public static final StyleAttribute<Double> LINE_SPACING = new StyleAttribute<>("LINE_SPACING", Double.class, true);

    /** Paragraph right-to-left orientation attribute */
    public static final StyleAttribute<Boolean> RIGHT_TO_LEFT = new StyleAttribute<>("RIGHT_TO_LEFT", Boolean.class, true);

    /** Space above the paragraph (top padding) attribute */
    public static final StyleAttribute<Double> SPACE_ABOVE = new StyleAttribute<>("SPACE_ABOVE", Double.class, true);

    /** Space below the paragraph (bottom padding) attribute */
    public static final StyleAttribute<Double> SPACE_BELOW = new StyleAttribute<>("SPACE_BELOW", Double.class, true);

    /** Space to the left of the paragraph (bottom padding) attribute */
    public static final StyleAttribute<Double> SPACE_LEFT = new StyleAttribute<>("SPACE_LEFT", Double.class, true);

    /** Space to the right of the paragraph (bottom padding) attribute */
    public static final StyleAttribute<Double> SPACE_RIGHT = new StyleAttribute<>("SPACE_RIGHT", Double.class, true);

    /** Strike-through style attribute */
    public static final StyleAttribute<Boolean> STRIKE_THROUGH = new StyleAttribute<>("STRIKE_THROUGH", Boolean.class, false);

    /** Paragraph text alignment attribute */
    public static final StyleAttribute<TextAlignment> TEXT_ALIGNMENT = new StyleAttribute<>("TEXT_ALIGNMENT", TextAlignment.class, true);

    /** Text color attrbute */
    public static final StyleAttribute<Color> TEXT_COLOR = new StyleAttribute<>("TEXT_COLOR", Color.class, false);

    /** Underline style attribute */
    public static final StyleAttribute<Boolean> UNDERLINE = new StyleAttribute<>("UNDERLINE", Boolean.class, false);
    
    private final HashMap<StyleAttribute<?>,Object> attributes;
    private transient String style;
    
    private StyleAttrs(Map<StyleAttribute<?>,Object> a) {
        this.attributes = new HashMap<>(a);
    }

    /**
     * Convenience method creates an instance with a single attribute.
     * @param attribute the attribute
     * @param value the attribute value
     * @return the new instance
     */
    public static <X> StyleAttrs of(StyleAttribute<X> attribute, X value) {
        return new Builder().set(attribute, value).build();
    }

    /**
     * Convenience method creates an instance from a direct style and a number of
     * CSS style names.
     * @param style the direct style, can be null
     * @param names style names
     * @return the new instance
     */
    public static StyleAttrs fromCss(String style, String... names) {
        if ((style == null) && (names == null)) {
            return StyleAttrs.EMPTY;
        } else if (names == null) {
            names = new String[0];
        }
        return new Builder().set(CSS, new CssStyles(style, names)).build();
    }

    @Override
    public boolean equals(Object x) {
        if (x == this) {
            return true;
        } else if (x instanceof StyleAttrs s) {
            return attributes.equals(s.attributes);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return attributes.hashCode() + (31 * StyleAttrs.class.hashCode());
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
     * @param a attribute
     * @return attribute value or null
     */
    public <X> X get(StyleAttribute<X> a) {
        return (X)attributes.get(a);
    }

    /**
     * Returns the set of attributes.
     * @return attribute set
     */
    public Set<StyleAttribute<?>> getAttributes() {
        return new HashSet<>(attributes.keySet());
    }

    /**
     * Returns true if the attribute is present; false otherwise.
     * @param a the attribute
     * @return true if the attribute is present
     */
    public boolean contains(StyleAttribute<?> a) {
        return attributes.containsKey(a);
    }

    /** 
     * Creates a new StyleAttrs instance by first copying attributes from this instance,
     * then adding (and/or overwriting) the attributes from the specified instance.
     * @param attrs the attributes to combine
     * @return the new instance combining the attributes
     */
    public StyleAttrs combine(StyleAttrs attrs) {
        return 
            new Builder().
            merge(this).
            merge(attrs).
            build();
    }

    /**
     * Returns true if the specified attribute has a boolean value of {@code Boolean.TRUE},
     * false otherwise.
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
     * is not present.
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
        sb.append("[");
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
        sb.append("]");
        return sb.toString();
    }

    /**
     * This convenience method returns the value of {@link #BACKGROUND} attribute, or null.
     * @return the background color attribute value
     */
    public final Color getBackground() {
        return (Color)get(BACKGROUND);
    }

    /**
     * This convenience method returns the value of {@link #BULLET} attribute, or null.
     * @return the bullet paragraph attribute value
     */
    public final String getBullet() {
        return (String)get(BULLET);
    }

    /**
     * This convenience method returns the value of {@link #CSS} attribute, or null.
     * @return the css style attribute value
     */
    public final CssStyles getCssStyles() {
        return (CssStyles)get(CSS);
    }

    /**
     * This convenience method returns the value of the {@link #FIRST_LINE_INDENT} attribute.
     * @return the first line indent value in points
     */
    public final Double getFirstLineIndent() {
        return (Double)get(FIRST_LINE_INDENT);
    }

    /**
     * This convenience method returns the value of the {@link #FONT_SIZE} attribute.
     * @return the font size
     */
    public final Double getFontSize() {
        return (Double)get(FONT_SIZE);
    }

    /**
     * This convenience method returns true if the value of the {@link #FONT_FAMILY} attribute is {@code Boolean.TRUE},
     * false otherwise.
     * @return the font family name
     */
    public final String getFontFamily() {
        return (String)get(FONT_FAMILY);
    }

    /**
     * This convenience method returns the value of the {@link #LINE_SPACING} attribute, or null.
     * @return the line spacing value
     */
    public Double getLineSpacing() {
        return (Double)get(LINE_SPACING);
    }
    
    /**
     * This convenience method returns the value of the {@link #SPACE_ABOVE} attribute, or null.
     * @return the space above paragraph attribute value
     */
    public Double getSpaceAbove() {
        return (Double)get(SPACE_ABOVE);
    }

    /**
     * This convenience method returns the value of the {@link #SPACE_BELOW} attribute, or null.
     * @return the space below paragraph attribute value
     */
    public Double getSpaceBelow() {
        return (Double)get(SPACE_BELOW);
    }

    /**
     * This convenience method returns the value of the {@link #SPACE_LEFT} attribute, or null.
     * @return the space left paragraph attribute value
     */
    public Double getSpaceLeft() {
        return (Double)get(SPACE_LEFT);
    }

    /**
     * This convenience method returns the value of the {@link #SPACE_RIGHT} attribute, or null.
     * @return the space right paragraph attribute value
     */
    public Double getSpaceRight() {
        return (Double)get(SPACE_RIGHT);
    }

    /**
     * This convenience method returns the value of {@link #TEXT_ALIGNMENT} attribute, or null.
     * @return the paragraph alignment attribute value
     */
    public final TextAlignment getTextAlignment() {
        return (TextAlignment)get(TEXT_ALIGNMENT);
    }

    /**
     * This convenience method returns the value of {@link #TEXT_COLOR} attribute, or null.
     * @return the text color attribute value
     */
    public final Color getTextColor() {
        return (Color)get(TEXT_COLOR);
    }

    /**
     * This convenience method returns true if the value of {@link #BOLD} attribute is {@code Boolean.TRUE},
     * false otherwise.
     * @return the bold attribute value
     */
    public final boolean isBold() {
        return getBoolean(BOLD);
    }

    /**
     * This convenience method returns true if the value of {@link #ITALIC} attribute is {@code Boolean.TRUE},
     * false otherwise.
     * @return the italic attribute value
     */
    public final boolean isItalic() {
        return getBoolean(ITALIC);
    }

    /**
     * This convenience method returns true if the value of {@link #RIGHT_TO_LEFT} paragraph attribute is {@code Boolean.TRUE},
     * false otherwise.
     * @return the right-to-left paragraph attribute value
     */
    public final boolean isRTL() {
        return getBoolean(RIGHT_TO_LEFT);
    }

    /**
     * This convenience method returns true if the value of {@link #STRIKE_THROUGH} attribute is {@code Boolean.TRUE},
     * false otherwise.
     * @return the strike through attribute value
     */
    public final boolean isStrikeThrough() {
        return getBoolean(STRIKE_THROUGH);
    }

    /**
     * This convenience method returns true if the value of {@link #UNDERLINE} attribute is {@code Boolean.TRUE},
     * false otherwise.
     * @return the underline attribute value
     */
    public final boolean isUnderline() {
        return getBoolean(UNDERLINE);
    }

    /**
     * Returns a new StyleAttrs instance which contains only character attributes,
     * or null if no character attributes found.
     * @return the instance
     */
    public StyleAttrs getCharacterAttrs() {
        return filterAttributes(false);
    }

    /**
     * Returns a new StyleAttrs instance which contains only paragraph attributes,
     * or null if no paragraph attributes found.
     * @return the instance
     */
    public StyleAttrs getParagraphAttrs() {
        return filterAttributes(true);
    }
    
    private StyleAttrs filterAttributes(boolean isParagraph) {
        Builder b = null;
        for(StyleAttribute<?> a: attributes.keySet()) {
            if(a.isParagraphAttribute() == isParagraph) {
                if(b == null) {
                    b = StyleAttrs.builder();
                }
                Object v = attributes.get(a);
                b.setUnguarded(a, v);
            }
        }
        return (b == null) ? null : b.build();
    }


    /**
     * Creates an instance of StyleAttrs which contains character attributes found in the Text node.
     * @param textNode the text node
     * @return the StyleAttrs instance
     */
    public static StyleAttrs from(Text textNode) {
        StyleAttrs.Builder b = StyleAttrs.builder();
        Font f = textNode.getFont();
        String st = f.getStyle().toLowerCase(Locale.US);
        boolean bold = RichUtils.isBold(st);
        boolean italic = RichUtils.isItalic(st);

        if (bold) {
            b.setBold(true);
        }

        if (italic) {
            b.setItalic(true);
        }

        if (textNode.isStrikethrough()) {
            b.setStrikeThrough(true);
        }

        if (textNode.isUnderline()) {
            b.setUnderline(true);
        }

        String family = f.getFamily();
        b.setFontFamily(family);

        double sz = f.getSize();
        if (sz != 12.0) {
            b.setFontSize(sz);
        }

        Paint x = textNode.getFill();
        if (x instanceof Color c) {
            // we do not support gradients (although we could get the first color, for example)
            b.setTextColor(c);
        }

        return b.build();
    }

    /**
     * Creates a new Builder instance.
     * @return the new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new Builder, populated with attributes from this StyleAttrs instance.
     * @return the new Builder instance
     */
    public Builder toBuilder() {
        return new Builder().merge(this);
    }
    
    /** StyleAttrs are immutable, so a Builder is required to create a new instance */
    public static class Builder {
        private final HashMap<StyleAttribute<?>,Object> attributes = new HashMap<>(4);

        private Builder() {
        }

        /**
         * Creates an immutable instance of {@link StyleAttrs} with the attributes set by this Builder.
         * @return the new instance
         */
        public StyleAttrs build() {
            return new StyleAttrs(attributes);
        }
        
        /**
         * Sets the value for the specified attribute.
         * This method will throw an {@code IllegalArgumentException} if the value cannot be cast to the
         * type specified by the attribute.
         *
         * @param a the attribute
         * @param value the attribute value
         * @return this Builder instance
         */
        public <X> Builder set(StyleAttribute<X> a, X value) {
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
         * @param attrs the attributes to merge
         * @return this Builder instance
         */
        public Builder merge(StyleAttrs attrs) {
            for (StyleAttribute<?> a : attrs.attributes.keySet()) {
                Object v = attrs.get(a);
                setUnguarded(a, v);
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
         * Sets the right-to-left paragraph attribute.
         * @param on true for RTL
         * @return this Builder instance
         */
        public Builder setRTL(boolean on) {
            set(RIGHT_TO_LEFT, Boolean.valueOf(on));
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
