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

package javafx.scene.control.rich.model;

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
import com.sun.javafx.scene.control.rich.D;
import com.sun.javafx.scene.control.rich.RichUtils;

/**
 * An immutable object containing style attributes.
 */
public class StyleAttrs {
    /** an instance with no attributes set */
    public static final StyleAttrs EMPTY = new StyleAttrs(Collections.emptyMap());

    /** Bold typeface attribute */
    public static final StyleAttribute BOLD = new StyleAttribute("BOLD", Boolean.class) {
        @Override
        public void buildStyle(StringBuilder sb, Object value) {
            if (Boolean.TRUE.equals(value)) {
                sb.append("-fx-font-weight:bold; ");
            } else {
                sb.append("-fx-font-weight:normal; ");
            }
        }
    };

    public static final StyleAttribute CSS = new StyleAttribute("CSS", CssStyles.class) {
        @Override
        public void buildStyle(StringBuilder sb, Object value) {
            D.p(); // FIX
        }
    };

    /** Font family attribute */
    public static final StyleAttribute FONT_FAMILY = new StyleAttribute("FONT_FAMILY", String.class) {
        @Override
        public void buildStyle(StringBuilder sb, Object value) {
            sb.append("-fx-font-family:'").append(value).append("'; ");
        }
    };
    
    /** Font size attribute, in percent, relative to the base font size. */
    public static final StyleAttribute FONT_SIZE = new StyleAttribute("FONT_SIZE", Integer.class) {
        @Override
        public void buildStyle(StringBuilder sb, Object value) {
            int n = (Integer)value;
            sb.append("-fx-font-size:").append(n).append("%; ");
        }
    };

    /** Italic type face attribute */
    public static final StyleAttribute ITALIC = new StyleAttribute("ITALIC", Boolean.class) {
        @Override
        public void buildStyle(StringBuilder sb, Object value) {
            if (Boolean.TRUE.equals(value)) {
                sb.append("-fx-font-style:italic; ");
            }
        }
    };

    /** Strike-through style attribute */
    public static final StyleAttribute STRIKE_THROUGH = new StyleAttribute("STRIKE_THROUGH", Boolean.class) {
        @Override
        public void buildStyle(StringBuilder sb, Object value) {
            if (Boolean.TRUE.equals(value)) {
                sb.append("-fx-strikethrough:true; ");
            }
        }
    };

    /** Text color attrbute */
    public static final StyleAttribute TEXT_COLOR = new StyleAttribute("TEXT_COLOR", Color.class) {
        @Override
        public void buildStyle(StringBuilder sb, Object value) {
            if (value != null) {
                String color = RichUtils.toCssColor((Color)value);
                sb.append("-fx-fill:").append(color).append("; ");
            }
        }
    };

    /** Underline style attribute */
    public static final StyleAttribute UNDERLINE = new StyleAttribute("UNDERLINE", Boolean.class) {
        @Override
        public void buildStyle(StringBuilder sb, Object value) {
            if (Boolean.TRUE.equals(value)) {
                sb.append("-fx-underline:true; ");
            }
        }
    };
    
    private final HashMap<StyleAttribute,Object> attributes;
    private transient String style;
    
    private StyleAttrs(Map<StyleAttribute,Object> a) {
        this.attributes = new HashMap<>(a);
    }

    /**
     * Convenience method creates an instance with a single attribute.
     * @param attribute the attribute
     * @param value the attribute value
     * @return the new instance
     */
    public static StyleAttrs of(StyleAttribute attribute, Object value) {
        return new Builder().set(attribute, value).create();
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
        return new Builder().set(CSS, new CssStyles(style, names)).create();
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
    public Object get(StyleAttribute a) {
        return attributes.get(a);
    }

    /**
     * Returns the set of attributes.
     * @return attribute set
     */
    public Set<StyleAttribute> getAttributes() {
        return new HashSet<>(attributes.keySet());
    }

    /**
     * Converts the attributes into a single direct style string and returns the resulting (can be null).
     * @return the style string
     */
    public String getStyle() {
        if (style == null) {
            style = createStyleString();
        }
        return style;
    }

    private String createStyleString() {
        if (attributes.size() == 0) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder(32);
        for(StyleAttribute a: attributes.keySet()) {
            Object v = attributes.get(a);
            a.buildStyle(sb, v);
        }
        return sb.toString();
    }

    /** 
     * Creates a new StyleAttrs instance by first copying attrirutes from this instance,
     * then adding (and/or overwriting) the attributes from the specified instance.
     * @param attrs the attributes to combine
     * @return a new instance combining the attributes
     */
    public StyleAttrs combine(StyleAttrs attrs) {
        return 
            new Builder().
            merge(this).
            merge(attrs).
            create();
    }

    /**
     * Returns true if the specified attribute has a boolean value of {@code Boolean.TRUE},
     * false otherwise.
     *
     * @param a the attribute
     * @return true if the attribute value is {@code Boolean.TRUE}
     */
    public boolean getBoolean(StyleAttribute a) {
        Object v = attributes.get(a);
        return Boolean.TRUE.equals(v);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("[");
        boolean sep = false;
        for (StyleAttribute a : attributes.keySet()) {
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
     * This convenience method returns the value of {@link #CSS} attribute, or null.
     * @return the css style attribute value
     */
    public final CssStyles getCssStyles() {
        return (CssStyles)get(CSS);
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
     * This convenience method returns true if the value of {@link #UNDERLINE} attribute is {@code Boolean.TRUE},
     * false otherwise.
     * @return the underline attribute value
     */
    public final boolean isUnderline() {
        return getBoolean(UNDERLINE);
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
     * This convenience method returns true if the value of {@link #FONT_SIZE} attribute is {@code Boolean.TRUE},
     * false otherwise.
     * @return the font size
     */
    public final Integer getFontSize() {
        return (Integer)get(FONT_SIZE);
    }

    /**
     * This convenience method returns true if the value of {@link #FONT_FAMILY} attribute is {@code Boolean.TRUE},
     * false otherwise.
     * @return the font family name
     */
    public final String getFontFamily() {
        return (String)get(FONT_FAMILY);
    }

    /**
     * Creates a style attributes instance from a Text node.
     * @param textNode the text node
     * @return a StyleAttrs instance
     */
    public static StyleAttrs from(Text textNode) {
        StyleAttrs.Builder b = StyleAttrs.builder();
        Font f = textNode.getFont();
        String st = f.getStyle().toLowerCase(Locale.US);
        boolean bold = st.contains("bold");
        boolean italic = st.contains("italic"); // oblique? any other names?

        if (bold) {
            b.set(BOLD, true);
        }

        if (italic) {
            b.set(ITALIC, true);
        }

        if (textNode.isStrikethrough()) {
            b.set(STRIKE_THROUGH, true);
        }

        if (textNode.isUnderline()) {
            b.set(UNDERLINE, true);
        }

        String family = f.getFamily();
        b.set(FONT_FAMILY, family);

        double sz = f.getSize();
        // TODO we could use a default font in the rich text area
        int size = (int)Math.round(sz / 0.12); // in percent relative to size 12
        if (size != 100) {
            b.set(FONT_SIZE, size);
        }

        Paint x = textNode.getFill();
        if (x instanceof Color c) {
            // we do not support gradients (although we could get the first color, for example)
            b.set(TEXT_COLOR, c);
        }

        return b.create();
    }

    /**
     * Creates a new Builder instance.
     * @return the new instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /** StyleAttrs are immutable, so a Builder is required to create a new instance */
    public static class Builder {
        private final HashMap<StyleAttribute,Object> attributes = new HashMap<>(4);

        private Builder() {
        }

        /**
         * Creates an immutable instance of {@link StyleAttrs}.
         * @return the new instance
         */
        public StyleAttrs create() {
            return new StyleAttrs(attributes);
        }
        
        /**
         * Sets a boolean attribute.
         * @param a the attribute
         * @param value the attribute value
         * @return a new Builder instance
         */
        public Builder set(StyleAttribute a, boolean value) {
            return set(a, Boolean.valueOf(value));
        }

        /**
         * Sets the value for the specified attribute.
         * This method will throw an {@code IllegalArgumentException} if the value cannot be cast to the
         * type specified by the attribute.
         *
         * @param a the attribute
         * @param value the attribute value
         * @return a new Builder instance
         */
        public Builder set(StyleAttribute a, Object value) {
            if (value == null) {
                attributes.put(a, null);
            } else if (value.getClass().isAssignableFrom(a.getType())) {
                attributes.put(a, value);
            } else {
                throw new IllegalArgumentException(a + " requires value of type " + a.getType());
            }
            return this;
        }

        /** 
         * Merges the specified attributes with the attributes in this instance.
         * The new values override any existing ones.
         * @param attrs the attributes to merge
         * @return a new Builder instance
         */
        public Builder merge(StyleAttrs attrs) {
            for (StyleAttribute a : attrs.attributes.keySet()) {
                Object v = attrs.get(a);
                set(a, v);
            }
            return this;
        }
    }
}
