/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

import javafx.css.converter.BooleanConverter;
import javafx.css.converter.ColorConverter;
import javafx.css.converter.DeriveColorConverter;
import javafx.css.converter.DeriveSizeConverter;
import javafx.css.converter.DurationConverter;
import javafx.css.converter.EffectConverter;
import javafx.css.converter.EnumConverter;
import javafx.css.converter.FontConverter;
import javafx.css.converter.InsetsConverter;
import javafx.css.converter.LadderConverter;
import javafx.css.converter.PaintConverter;
import javafx.css.converter.SizeConverter;
import javafx.css.converter.StopConverter;
import javafx.css.converter.StringConverter;
import javafx.css.converter.URLConverter;
import javafx.geometry.Insets;
import javafx.scene.effect.Effect;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.util.Duration;

import com.sun.javafx.scene.layout.region.CornerRadiiConverter;
import com.sun.javafx.util.Logging;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;


/**
 * StyleConverter converts {@code ParsedValue<F,T>}
 * from type {@code F} to type {@code T}. The
 * {@link CssMetaData} API requires a {@code StyleConverter} which is used
 * when computing a value for the {@code StyleableProperty}. There are
 * a number of predefined converters which are accessible by the static
 * methods of this class.
 *
 * {@code F} is the type of the parsed value and {@code T} is the converted type of
 * the ParsedValueImpl. For example, a converter from String to Color would
 * be declared
 * <p>&nbsp;&nbsp;&nbsp;&nbsp;
 * {@code public Color convert(ParsedValueImpl<String,Color> value, Font font)}
 * </p>
 *
 * @param <F> the type of the parsed value
 * @param <T> the converted type of the ParsedValueImpl
 *
 * @see ParsedValue
 * @see StyleableProperty
 * @since JavaFX 8.0
 */
public class StyleConverter<F, T> {

    /**
     * Creates a {@code StyleConverter}.
     */
    public StyleConverter() {
    }

    /**
     * Convert from the parsed CSS value to the target property type.
     *
     * @param value        The {@link ParsedValue} to convert
     * @param font         The {@link Font} to use when converting a
     * <a href="http://www.w3.org/TR/css3-values/#relative-lengths">relative</a>
     * value.
     * @return the converted target property type
     */
    @SuppressWarnings("unchecked")
    public T convert(ParsedValue<F,T> value, Font font) {
        // unchecked!
        return (T) value.getValue();
    }

    /**
     * Return a {@code StyleConverter} that converts {@literal "true" or "false"} to {@code Boolean}.
     * @return A {@code StyleConverter} that converts {@literal "true" or "false"} to {@code Boolean}
     * @see Boolean#valueOf(java.lang.String)
     */
    public static StyleConverter<String,Boolean> getBooleanConverter() {
        return BooleanConverter.getInstance();
    }

    /**
     * Return a {@code StyleConverter} that converts a String representation of
     * a duration to a {@link Duration}.
     * @return A {@code StyleConverter} that converts a String
     * representation of a duration to a {@link Duration}
     *
     * @since JavaFX 8u40
     */
    public static StyleConverter<?,Duration> getDurationConverter() {
        return DurationConverter.getInstance();
    }

    /**
     * Return a {@code StyleConverter} that converts a String representation of
     * a web color to a {@code Color}.
     * @return A {@code StyleConverter} that converts a String
     * representation of a web color to a {@code Color}
     * @see Color#web(java.lang.String)
     */
    public static StyleConverter<String,Color> getColorConverter() {
        return ColorConverter.getInstance();
    }

    /**
     * Return a {@code StyleConverter} that converts a parsed representation
     * of an {@code Effect} to an {@code Effect}
     * @return A {@code StyleConverter} that converts a parsed representation
     * of an {@code Effect} to an {@code Effect}
     * @see Effect
     */
    public static StyleConverter<ParsedValue[], Effect> getEffectConverter() {
        return EffectConverter.getInstance();
    }

    /**
     * Return a {@code StyleConverter} that converts a String representation
     * of an {@code Enum} to an {@code Enum}.
     * @param <E> the type of the {@code Enum}
     * @param enumClass the enum Class
     * @return A {@code StyleConverter} that converts a String representation
     * of an {@code Enum} to an {@code Enum}
     * @see Enum#valueOf(java.lang.Class, java.lang.String)
     */
    public static <E extends Enum<E>> StyleConverter<String, E> getEnumConverter(Class<E> enumClass) {
        // TODO: reuse EnumConverter instances
        EnumConverter<E> converter;
        converter = new EnumConverter<>(enumClass);
        return converter;
    }

    /**
     * Return a {@code StyleConverter} that converts a parsed representation
     * of a {@code Font} to an {@code Font}.
     * @return A {@code StyleConverter} that converts a parsed representation
     * of a {@code Font} to an {@code Font}
     * @see Font#font(java.lang.String, javafx.scene.text.FontWeight, javafx.scene.text.FontPosture, double)
     */
    public static StyleConverter<ParsedValue[], Font> getFontConverter() {
        return FontConverter.getInstance();
    }

    /**
     * Return a {@code StyleConverter} that converts a {@literal [<length> |
     * <percentage>]}{1,4} to an {@code Insets}.
     * @return A {@code StyleConverter} that converts a {@literal [<length> |
     * <percentage>]}{1,4} to an {@code Insets}
     */
    public static StyleConverter<ParsedValue[], Insets> getInsetsConverter() {
        return InsetsConverter.getInstance();
    }

    /**
     * Return a {@code StyleConverter} that converts a parsed representation
     * of a {@code Paint} to a {@code Paint}.
     * @return A {@code StyleConverter} that converts a parsed representation
     * of a {@code Paint} to a {@code Paint}
     */
    public static StyleConverter<ParsedValue<?, Paint>, Paint> getPaintConverter() {
        return PaintConverter.getInstance();
    }

    /**
     * CSS length and number values are parsed into a Size object that is
     * converted to a Number before the value is applied. If the property is
     * a {@code Number} type other than {@code Double}, the set method
     * of ({@code CssMetaData} can be overridden to convert the {@code Number}
     * to the correct type. For example, if the property is an {@code IntegerProperty}:
     * <pre><code>
     *     {@literal @}Override public void set(MyNode node, Number value, Origin origin) {
     *         if (value != null) {
     *             super.set(node, value.intValue(), origin);
     *         } else {
     *             super.set(node, value, origin);
     *         }
     *     }
     * </code></pre>
     * @return A {@code StyleConverter} that converts a parsed representation
     * of a CSS length or number value to a {@code Number} that is an instance
     * of {@code Double}
     */
    public static StyleConverter<?, Number> getSizeConverter() {
        return SizeConverter.getInstance();
    }

    /**
     * A converter for quoted strings which may have embedded unicode characters.
     * @return A {@code StyleConverter} that converts a representation of a
     * CSS string value to a {@code String}
     */
    public static StyleConverter<String,String> getStringConverter() {
        return StringConverter.getInstance();
    }

    /**
     * A converter for URL strings.
     * @return A {@code StyleConverter} that converts a representation of a
     * CSS URL value to a {@code String}
     */
    public static StyleConverter<ParsedValue[], String> getUrlConverter() {
        return URLConverter.getInstance();
    }




    /**
     * Convert from the constituent values to the target property type.
     * Implemented by Types that have Keys with subKeys.
     *
     * @param convertedValues the constituent values
     * @return the target property type
     * @since 9
     */
    public T convert(Map<CssMetaData<? extends Styleable, ?>,Object> convertedValues) {
        return null;
    }

    /**
     * Write binary data.
     * @param os the data output stream
     * @param sstore the string store
     * @throws java.io.IOException the exception
     * @since 9
     */
    public void writeBinary(DataOutputStream os, StringStore sstore)
            throws IOException {

        String cname = getClass().getName();
        int index = sstore.addString(cname);
        os.writeShort(index);
    }

    private static Map<ParsedValue, Object> cache;

    /**
     * Clear the cache.
     * @since 9
     */
    public static void clearCache() {
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Get the cached value for the specified key.
     * @param key the key
     * @return the cached value
     * @since 9
     */
    protected T getCachedValue(ParsedValue key) {
        if (cache != null) {
            return (T)cache.get(key);
        }
        return null;
    }

    /**
     * Cache the value for the specified key.
     * @param key the key
     * @param value the value
     * @since 9
     */
    protected void cacheValue(ParsedValue key, Object value) {
        if (cache == null) cache = new WeakHashMap<>();
        cache.put(key, value);
    }

    // map of StyleConverter class name to StyleConverter
    private static Map<String,StyleConverter<?, ?>> tmap;

    /**
     * Read binary data stream.
     * @param is the data input stream
     * @param strings the strings
     * @return the style converter
     * @throws java.io.IOException the exception
     * @since 9
     */
    @SuppressWarnings("rawtypes")
    public static StyleConverter<?,?> readBinary(DataInputStream is, String[] strings)
            throws IOException {

        int index = is.readShort();
        String cname = strings[index];

        if (cname == null || cname.isEmpty()) return null;

        if (cname.startsWith("com.sun.javafx.css.converters.")) {
            // JavaFX 9: converter classes were moved from
            // com.sun.javafx.css.converters.* to javafx.css.converter.*
            // Note: the word 'converters' has become 'converter'.
            cname = "javafx.css.converter." + cname.substring("com.sun.javafx.css.converters.".length());
        }
        if (cname.startsWith("javafx.css.converter.EnumConverter")) {
            return (StyleConverter)javafx.css.converter.EnumConverter.readBinary(is, strings);
        }

        // Make a new entry in tmap, if necessary
        if (tmap == null || !tmap.containsKey(cname)) {
            StyleConverter<?,?> converter = getInstance(cname);
            if (converter == null) {
                final PlatformLogger logger = Logging.getCSSLogger();
                if (logger.isLoggable(Level.SEVERE)) {
                    logger.severe("could not deserialize " + cname);
                }
            }
            if (converter == null) {
                System.err.println("could not deserialize " + cname);
            }
            if (tmap == null) tmap = new HashMap<String,StyleConverter<?,?>>();
            tmap.put(cname, converter);
            return converter;
        }
        return tmap.get(cname);
    }

    // package for unit test purposes
    static StyleConverter<?,?> getInstance(final String converterClass) {

        StyleConverter<?,?> styleConverter = null;

        switch(converterClass) {
        case "javafx.css.converter.BooleanConverter" :
            styleConverter = javafx.css.converter.BooleanConverter.getInstance();
            break;
        case "javafx.css.converter.ColorConverter" :
            styleConverter = javafx.css.converter.ColorConverter.getInstance();
            break;
        case "javafx.css.converter.CursorConverter" :
            styleConverter = javafx.css.converter.CursorConverter.getInstance();
            break;
        case "javafx.css.converter.EffectConverter" :
            styleConverter = javafx.css.converter.EffectConverter.getInstance();
            break;
        case "javafx.css.converter.EffectConverter$DropShadowConverter" :
            styleConverter = javafx.css.converter.EffectConverter.DropShadowConverter.getInstance();
            break;
        case "javafx.css.converter.EffectConverter$InnerShadowConverter" :
            styleConverter = javafx.css.converter.EffectConverter.InnerShadowConverter.getInstance();
            break;
        case "javafx.css.converter.FontConverter" :
            styleConverter = javafx.css.converter.FontConverter.getInstance();
            break;
        case "javafx.css.converter.FontConverter$FontStyleConverter" :
        case "javafx.css.converter.FontConverter$StyleConverter" :
            styleConverter = javafx.css.converter.FontConverter.FontStyleConverter.getInstance();
            break;
        case "javafx.css.converter.FontConverter$FontWeightConverter" :
        case "javafx.css.converter.FontConverter$WeightConverter" :
            styleConverter = javafx.css.converter.FontConverter.FontWeightConverter.getInstance();
            break;
        case "javafx.css.converter.FontConverter$FontSizeConverter" :
        case "javafx.css.converter.FontConverter$SizeConverter" :
            styleConverter = javafx.css.converter.FontConverter.FontSizeConverter.getInstance();
            break;

        case "javafx.css.converter.InsetsConverter" :
            styleConverter = javafx.css.converter.InsetsConverter.getInstance();
            break;
        case "javafx.css.converter.InsetsConverter$SequenceConverter" :
            styleConverter = javafx.css.converter.InsetsConverter.SequenceConverter.getInstance();
            break;

        case "javafx.css.converter.PaintConverter" :
            styleConverter = javafx.css.converter.PaintConverter.getInstance();
            break;
        case "javafx.css.converter.PaintConverter$SequenceConverter" :
            styleConverter = javafx.css.converter.PaintConverter.SequenceConverter.getInstance();
            break;
        case "javafx.css.converter.PaintConverter$LinearGradientConverter" :
            styleConverter = javafx.css.converter.PaintConverter.LinearGradientConverter.getInstance();
            break;
        case "javafx.css.converter.PaintConverter$RadialGradientConverter" :
            styleConverter = javafx.css.converter.PaintConverter.RadialGradientConverter.getInstance();
            break;

        case "javafx.css.converter.SizeConverter" :
            styleConverter = javafx.css.converter.SizeConverter.getInstance();
            break;
        case "javafx.css.converter.SizeConverter$SequenceConverter" :
            styleConverter = javafx.css.converter.SizeConverter.SequenceConverter.getInstance();
            break;

        case "javafx.css.converter.StringConverter" :
            styleConverter = javafx.css.converter.StringConverter.getInstance();
            break;
        case "javafx.css.converter.StringConverter$SequenceConverter" :
            styleConverter = javafx.css.converter.StringConverter.SequenceConverter.getInstance();
            break;
        case "javafx.css.converter.URLConverter" :
            styleConverter = javafx.css.converter.URLConverter.getInstance();
            break;
        case "javafx.css.converter.URLConverter$SequenceConverter" :
            styleConverter = javafx.css.converter.URLConverter.SequenceConverter.getInstance();
            break;

        // Region stuff  - including 2.x class names
        case "com.sun.javafx.scene.layout.region.BackgroundPositionConverter" :
        case "com.sun.javafx.scene.layout.region.BackgroundImage$BackgroundPositionConverter" :
            styleConverter = com.sun.javafx.scene.layout.region.BackgroundPositionConverter.getInstance();
            break;
        case "com.sun.javafx.scene.layout.region.BackgroundSizeConverter" :
        case "com.sun.javafx.scene.layout.region.BackgroundImage$BackgroundSizeConverter" :
            styleConverter = com.sun.javafx.scene.layout.region.BackgroundSizeConverter.getInstance();
            break;
        case "com.sun.javafx.scene.layout.region.BorderImageSliceConverter" :
        case "com.sun.javafx.scene.layout.region.BorderImage$SliceConverter" :
            styleConverter = com.sun.javafx.scene.layout.region.BorderImageSliceConverter.getInstance();
            break;
        case "com.sun.javafx.scene.layout.region.BorderImageWidthConverter" :
            styleConverter = com.sun.javafx.scene.layout.region.BorderImageWidthConverter.getInstance();
            break;
        case "com.sun.javafx.scene.layout.region.BorderImageWidthsSequenceConverter" :
            styleConverter = com.sun.javafx.scene.layout.region.BorderImageWidthsSequenceConverter.getInstance();
            break;
        case "com.sun.javafx.scene.layout.region.BorderStrokeStyleSequenceConverter" :
        case "com.sun.javafx.scene.layout.region.StrokeBorder$BorderStyleSequenceConverter" :
            styleConverter = com.sun.javafx.scene.layout.region.BorderStrokeStyleSequenceConverter.getInstance();
            break;
        case "com.sun.javafx.scene.layout.region.BorderStyleConverter" :
        case "com.sun.javafx.scene.layout.region.StrokeBorder$BorderStyleConverter" :
            styleConverter = com.sun.javafx.scene.layout.region.BorderStyleConverter.getInstance();
            break;
        case "com.sun.javafx.scene.layout.region.LayeredBackgroundPositionConverter" :
        case "com.sun.javafx.scene.layout.region.BackgroundImage$LayeredBackgroundPositionConverter" :
            styleConverter = com.sun.javafx.scene.layout.region.LayeredBackgroundPositionConverter.getInstance();
            break;
        case "com.sun.javafx.scene.layout.region.LayeredBackgroundSizeConverter" :
        case "com.sun.javafx.scene.layout.region.BackgroundImage$LayeredBackgroundSizeConverter" :
            styleConverter = com.sun.javafx.scene.layout.region.LayeredBackgroundSizeConverter.getInstance();
            break;
        case "com.sun.javafx.scene.layout.region.LayeredBorderPaintConverter" :
        case "com.sun.javafx.scene.layout.region.StrokeBorder$LayeredBorderPaintConverter" :
           styleConverter = com.sun.javafx.scene.layout.region.LayeredBorderPaintConverter.getInstance();
            break;
        case "com.sun.javafx.scene.layout.region.LayeredBorderStyleConverter" :
        case "com.sun.javafx.scene.layout.region.StrokeBorder$LayeredBorderStyleConverter" :
            styleConverter = com.sun.javafx.scene.layout.region.LayeredBorderStyleConverter.getInstance();
            break;
        case "com.sun.javafx.scene.layout.region.RepeatStructConverter" :
        case "com.sun.javafx.scene.layout.region.BackgroundImage$BackgroundRepeatConverter" :
        case "com.sun.javafx.scene.layout.region.BorderImage$RepeatConverter" :
            styleConverter = com.sun.javafx.scene.layout.region.RepeatStructConverter.getInstance();
            break;
        case "com.sun.javafx.scene.layout.region.SliceSequenceConverter" :
        case "com.sun.javafx.scene.layout.region.BorderImage$SliceSequenceConverter" :
            styleConverter = com.sun.javafx.scene.layout.region.SliceSequenceConverter.getInstance();
            break;
        case "com.sun.javafx.scene.layout.region.StrokeBorderPaintConverter" :
        case "com.sun.javafx.scene.layout.region.StrokeBorder$BorderPaintConverter" :
            styleConverter = com.sun.javafx.scene.layout.region.StrokeBorderPaintConverter.getInstance();
            break;
        case "com.sun.javafx.scene.layout.region.Margins$Converter" :
            styleConverter = com.sun.javafx.scene.layout.region.Margins.Converter.getInstance();
            break;
        case "com.sun.javafx.scene.layout.region.Margins$SequenceConverter" :
            styleConverter = com.sun.javafx.scene.layout.region.Margins.SequenceConverter.getInstance();
            break;
        case "javafx.scene.layout.CornerRadiiConverter" :  // Fix for RT-39665
        case "com.sun.javafx.scene.layout.region.CornerRadiiConverter" :
            styleConverter = CornerRadiiConverter.getInstance();
            break;

        // parser stuff
        case "javafx.css.converter.DeriveColorConverter":
        case "com.sun.javafx.css.parser.DeriveColorConverter" :
            styleConverter = DeriveColorConverter.getInstance();
            break;
        case "javafx.css.converter.DeriveSizeConverter":
        case "com.sun.javafx.css.parser.DeriveSizeConverter" :
            styleConverter = DeriveSizeConverter.getInstance();
            break;
        case "javafx.css.converter.LadderConverter":
        case "com.sun.javafx.css.parser.LadderConverter" :
            styleConverter = LadderConverter.getInstance();
            break;
        case "javafx.css.converter.StopConverter":
        case "com.sun.javafx.css.parser.StopConverter" :
            styleConverter = StopConverter.getInstance();
            break;

            default :
            final PlatformLogger logger = Logging.getCSSLogger();
            if (logger.isLoggable(Level.SEVERE)) {
                logger.severe("StyleConverter : converter Class is null for : "+converterClass);
            }
            break;
        }

        return styleConverter;
    }


    /**
     * The StringStore class
     * @since 9
     */
    public static class StringStore {
        private final Map<String,Integer> stringMap = new HashMap<String,Integer>();
        public final List<String> strings = new ArrayList<String>();

        /**
         * Creates a {@code StringStore}.
         */
        public StringStore() {
        }

        public int addString(String s) {
            Integer index = stringMap.get(s);
            if (index == null) {
                index = strings.size();
                strings.add(s);
                stringMap.put(s,index);
            }
            return index;
        }

        public void writeBinary(DataOutputStream os) throws IOException {
            os.writeShort(strings.size());
            if (stringMap.containsKey(null)) {
                Integer index = stringMap.get(null);
                os.writeShort(index);
            } else {
                os.writeShort(-1);
            }
            for (int n=0; n<strings.size(); n++) {
                String s = strings.get(n);
                if (s == null) continue;
                os.writeUTF(s);
            }
        }

        // TODO: this isn't parallel with writeBinary
        public static String[] readBinary(DataInputStream is) throws IOException {
            int nStrings = is.readShort();
            int nullIndex = is.readShort();
            String[] strings = new String[nStrings];
            java.util.Arrays.fill(strings, null);
            for (int n=0; n<nStrings; n++) {
                if (n == nullIndex) continue;
                strings[n] = is.readUTF();
            }
            return strings;
        }
    }
}
