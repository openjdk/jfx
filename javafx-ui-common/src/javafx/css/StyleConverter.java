/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.css;

import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.ColorConverter;
import com.sun.javafx.css.converters.EffectConverter;
import com.sun.javafx.css.converters.EnumConverter;
import com.sun.javafx.css.converters.FontConverter;
import com.sun.javafx.css.converters.InsetsConverter;
import com.sun.javafx.css.converters.PaintConverter;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.css.converters.StringConverter;
import com.sun.javafx.css.converters.URLConverter;
import javafx.geometry.Insets;
import javafx.scene.effect.Effect;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

/**
 * Converter converts {@code ParsedValue<F,T>} from type F to type T. the
 * {@link CssMetaData} API requires a {@code StyleConverter} which is used
 * when computing a value for the {@see StyleableProperty}. There are
 * a number of predefined converters which are accessible by the static 
 * methods of this class.
 * @see ParsedValue
 * @see StyleableProperty
 */
public class StyleConverter<F, T> {

    /**
     * Convert from the parsed CSS value to the target property type. 
     *
     * @param value        The {@link ParsedValue} to convert
     * @param font         The {@link Font} to use when converting a
     * <a href="http://www.w3.org/TR/css3-values/#relative-lengths">relative</a>
     * value.
     */
    @SuppressWarnings("unchecked")
    public T convert(ParsedValue<F,T> value, Font font) {
        // unchecked!
        return (T) value.getValue();
    }

    /** 
     * @return A {@code StyleConverter} that converts &quot;true&quot; or &quot;false&quot; to {@code Boolean}
     * @see Boolean#valueOf(java.lang.String) 
     */
    public static StyleConverter<String,Boolean> getBooleanConverter() {
        return BooleanConverter.getInstance();
    }
    
    /** 
     * @return A {@code StyleConverter} that converts a String 
     * representation of a web color to a {@code Color}
     * @see Color#web(java.lang.String) 
     */
    public static StyleConverter<String,Color> getColorConverter() {
        return ColorConverter.getInstance();
    }
    
    /** 
     * @return A {@code StyleConverter} that converts a parsed representation
     * of an {@code Effect} to an {@code Effect}
     * @see Effect
     */
    public static StyleConverter<ParsedValue[], Effect> getEffectConverter() {
        return EffectConverter.getInstance();
    }
    
    /** 
     * @return A {@code StyleConverter} that converts a String representation
     * of an {@code Enum} to an {@code Enum}
     * @see Enum#valueOf(java.lang.Class, java.lang.String) 
     */
    public static <E extends Enum<E>> StyleConverter<String, ? extends Enum<?>> getEnumConverter(Class<E> enumClass) {        
        // TODO: reuse EnumConverter instances
        EnumConverter<E> converter;
        converter = new EnumConverter<E>(enumClass);
        return converter;
    }    

    /** 
     * @return A {@code StyleConverter} that converts a parsed representation
     * of a {@code Font} to an {@code Font}. 
     * @see Font#font(java.lang.String, javafx.scene.text.FontWeight, javafx.scene.text.FontPosture, double)  
     */
    public static StyleConverter<ParsedValue[], Font> getFontConverter() {
        return FontConverter.getInstance();
    }
    
    /** 
     * @return A {@code StyleConverter} that converts a [&lt;length&gt; |
     * &lt;percentage&gt;]{1,4} to an {@code Insets}. 
     */
    public static StyleConverter<ParsedValue[], Insets> getInsetsConverter() {
        return InsetsConverter.getInstance();
    }

    /** 
     * @return A {@code StyleConverter} that converts a parsed representation
     * of a {@code Paint} to a {@code Paint}.
     */
    public static StyleConverter<ParsedValue<?, Paint>, Paint> getPaintConverter() {
        return PaintConverter.getInstance();
    }
    
    /** 
     * CSS length and number values are parsed into a Size object that is
     * converted to a Number before the value is applied. If the property is
     * a {@code Number} type other than Double, the 
     * {@link CssMetaData#set(javafx.scene.Node, java.lang.Object, javafx.css.Origin) set}
     * method of ({@code CssMetaData} can be over-ridden to convert the Number
     * to the correct type. For example, if the property is an {@code IntegerProperty}:
     * <code><pre>
     *     {@literal @}Override public void set(MyNode node, Number value, Origin origin) {
     *         if (value != null) {
     *             super.set(node, value.intValue(), origin);
     *         } else {
     *             super.set(node, value, origin);
     *         }
     *     }
     * </pre></code>
     * @return A {@code StyleConverter} that converts a parsed representation
     * of a CSS length or number value to a {@code Number} that is an instance
     * of {@code Double}. 
     */
    public static StyleConverter<?, Number> getSizeConverter() {
        return SizeConverter.getInstance();
    }
    
    /** 
     * A converter for quoted strings which may have embedded unicode characters.
     * @return A {@code StyleConverter} that converts a representation of a
     * CSS string value to a {@code String}.
     */
    public static StyleConverter<String,String> getStringConverter() {
        return StringConverter.getInstance();
    }
    
    /** 
     * A converter for URL strings.
     * @return A {@code StyleConverter} that converts a representation of a
     * CSS URL value to a {@code String}.
     */
    public static StyleConverter<ParsedValue[], String> getUrlConverter() {
        return URLConverter.getInstance();
    }
    

}
