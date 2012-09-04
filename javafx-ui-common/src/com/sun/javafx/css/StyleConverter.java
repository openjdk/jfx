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

package com.sun.javafx.css;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.text.Font;
import com.sun.javafx.css.converters.EnumConverter;
import com.sun.javafx.css.converters.PaintConverter;

/**
 * Converter converts ParsedValue&lt;F,T&gt; from type F to type T.
 * F is the type of the parsed value, T is the converted type of
 * the ParsedValue. For example, a converter from String to Color would
 * be declared
 * <p>&nbsp;&nbsp;&nbsp;&nbsp;
 * <code>public Color convert(ParsedValue&lt;String,Color&gt; value, Font font)</code>
 * </p>
 */
public class StyleConverter<F, T> {

    /**
     * Convert from the parsed css value to the target property type.
     *
     * The default returns the value contained in ParsedValue.
     *
     * @param value        The value to convert
     * @param font         The font parameter is used to convert those types that are relative
     * to or inherited from a parent font.
     */
    public T convert(ParsedValue<F,T> value, Font font) {
        return (T) value.getValue();
    }

    /**
     * Convert from the constituent values to the target property type.
     * Implemented by Types that have Keys with subKeys.
     */
    public T convert(Map<StyleableProperty,Object> convertedValues) {
        return null;
    }

    private static class Holder {
        private static StyleConverter CONVERTER = new StyleConverter();
    }

    /** This converter simply returns value.getValue() */
    public static StyleConverter getInstance() {
        return Holder.CONVERTER;
    }

    protected StyleConverter() {
    }


    public void writeBinary(DataOutputStream os, StringStore sstore)
            throws IOException {

        String cname = getClass().getName();
        int index = sstore.addString(cname);
        os.writeShort(index);
    }

    // map of StyleConverter class name to StyleConverter
    static Map<String,StyleConverter> tmap;

    public static StyleConverter readBinary(DataInputStream is, String[] strings)
            throws IOException {

        int index = is.readShort();
        String cname = strings[index];

        // Make a new entry in tmap, if necessary
        if (tmap == null || !tmap.containsKey(cname)) {
            StyleConverter converter = null;
            try {
                Class cl = Class.forName(cname);
                // is cl assignable from EnumType.class?
                if (EnumConverter.class.isAssignableFrom(cl)) {
                    converter = new EnumConverter(is, strings);
                } else {
                    converter = getInstance(cl);
                }
            } catch (ClassNotFoundException cnfe) {
                // Class.forName failed
                System.err.println(cnfe.toString());
            }
            if (converter == null) {
                System.err.println("could not deserialize " + cname);
            }
            if (tmap == null) tmap = new HashMap<String,StyleConverter>();
            tmap.put(cname, converter);
            return converter;
        }
        return tmap.get(cname);
    }
    
    public String writeJava() {
        return getClass().getCanonicalName() + ".getInstance()";
    }


    // package for unit test purposes
    public static StyleConverter getInstance(final Class converterClass) {

        StyleConverter styleConverter = null;
        // TODO: giant if-then-else block
        if (com.sun.javafx.css.StyleConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.StyleConverter.getInstance();

        } else if (com.sun.javafx.css.converters.BooleanConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.BooleanConverter.getInstance();

        } else if (com.sun.javafx.css.converters.ColorConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.ColorConverter.getInstance();

        } else if (com.sun.javafx.css.converters.CursorConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.CursorConverter.getInstance();

        } else if (com.sun.javafx.css.converters.EffectConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.EffectConverter.getInstance();
        } else if (com.sun.javafx.css.converters.EffectConverter.DropShadowConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.EffectConverter.DropShadowConverter.getInstance();
        } else if (com.sun.javafx.css.converters.EffectConverter.InnerShadowConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.EffectConverter.InnerShadowConverter.getInstance();

        // enum is handled differently

        } else if (com.sun.javafx.css.converters.FontConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.FontConverter.getInstance();
        } else if (com.sun.javafx.css.converters.FontConverter.StyleConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.FontConverter.StyleConverter.getInstance();
        } else if (com.sun.javafx.css.converters.FontConverter.WeightConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.FontConverter.WeightConverter.getInstance();
        } else if (com.sun.javafx.css.converters.FontConverter.SizeConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.FontConverter.SizeConverter.getInstance();

        }  else if (com.sun.javafx.css.converters.InsetsConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.InsetsConverter.getInstance();
        }  else if (com.sun.javafx.css.converters.InsetsConverter.SequenceConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.InsetsConverter.SequenceConverter.getInstance();

        }  else if (com.sun.javafx.css.converters.PaintConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.PaintConverter.getInstance();
        }  else if (com.sun.javafx.css.converters.PaintConverter.SequenceConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.PaintConverter.SequenceConverter.getInstance();
        }  else if (com.sun.javafx.css.converters.PaintConverter.LinearGradientConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.PaintConverter.LinearGradientConverter.getInstance();
        }  else if (com.sun.javafx.css.converters.PaintConverter.RadialGradientConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.PaintConverter.RadialGradientConverter.getInstance();
        }  else if (PaintConverter.ImagePatternConverter.class == converterClass) {
            styleConverter = PaintConverter.ImagePatternConverter.getInstance();
        }  else if (PaintConverter.RepeatingImagePatternConverter.class == converterClass) {
            styleConverter = PaintConverter.RepeatingImagePatternConverter.getInstance();

        }  else if (com.sun.javafx.css.converters.SizeConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.SizeConverter.getInstance();
        }  else if (com.sun.javafx.css.converters.SizeConverter.SequenceConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.SizeConverter.SequenceConverter.getInstance();

        }  else if (com.sun.javafx.css.converters.StringConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.StringConverter.getInstance();
        }  else if (com.sun.javafx.css.converters.StringConverter.SequenceConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.StringConverter.SequenceConverter.getInstance();

        }  else if (com.sun.javafx.css.converters.URLConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.URLConverter.getInstance();
        }  else if (com.sun.javafx.css.converters.URLConverter.SequenceConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.converters.URLConverter.SequenceConverter.getInstance();

        // Region stuff
        }  else if (com.sun.javafx.scene.layout.region.BackgroundFillConverter.class == converterClass) {
            styleConverter = com.sun.javafx.scene.layout.region.BackgroundFillConverter.getInstance();

        }  else if (com.sun.javafx.scene.layout.region.BackgroundImageConverter.class == converterClass) {
            styleConverter = com.sun.javafx.scene.layout.region.BackgroundImageConverter.getInstance();
        }  else if (com.sun.javafx.scene.layout.region.BackgroundImage.BackgroundPositionConverter.class == converterClass) {
            styleConverter = com.sun.javafx.scene.layout.region.BackgroundImage.BackgroundPositionConverter.getInstance();
        }  else if (com.sun.javafx.scene.layout.region.BackgroundImage.BackgroundRepeatConverter.class == converterClass) {
            styleConverter = com.sun.javafx.scene.layout.region.BackgroundImage.BackgroundRepeatConverter.getInstance();
        }  else if (com.sun.javafx.scene.layout.region.BackgroundImage.BackgroundSizeConverter.class == converterClass) {
            styleConverter = com.sun.javafx.scene.layout.region.BackgroundImage.BackgroundSizeConverter.getInstance();
        }  else if (com.sun.javafx.scene.layout.region.BackgroundImage.LayeredBackgroundPositionConverter.class == converterClass) {
            styleConverter = com.sun.javafx.scene.layout.region.BackgroundImage.LayeredBackgroundPositionConverter.getInstance();
        }  else if (com.sun.javafx.scene.layout.region.BackgroundImage.LayeredBackgroundSizeConverter.class == converterClass) {
            styleConverter = com.sun.javafx.scene.layout.region.BackgroundImage.LayeredBackgroundSizeConverter.getInstance();

        }  else if (com.sun.javafx.scene.layout.region.BorderImageConverter.class == converterClass) {
            styleConverter = com.sun.javafx.scene.layout.region.BorderImageConverter.getInstance();
        }  else if (com.sun.javafx.scene.layout.region.BorderImage.RepeatConverter.class == converterClass) {
            styleConverter = com.sun.javafx.scene.layout.region.BorderImage.RepeatConverter.getInstance();
        }  else if (com.sun.javafx.scene.layout.region.BorderImage.SliceConverter.class == converterClass) {
            styleConverter = com.sun.javafx.scene.layout.region.BorderImage.SliceConverter.getInstance();
        }  else if (com.sun.javafx.scene.layout.region.BorderImage.SliceSequenceConverter.class == converterClass) {
            styleConverter = com.sun.javafx.scene.layout.region.BorderImage.SliceSequenceConverter.getInstance();

        }  else if (com.sun.javafx.scene.layout.region.StrokeBorderConverter.class == converterClass) {
            styleConverter = com.sun.javafx.scene.layout.region.StrokeBorderConverter.getInstance();
        }  else if (com.sun.javafx.scene.layout.region.StrokeBorder.BorderPaintConverter.class == converterClass) {
            styleConverter = com.sun.javafx.scene.layout.region.StrokeBorder.BorderPaintConverter.getInstance();
        }  else if (com.sun.javafx.scene.layout.region.StrokeBorder.BorderStyleConverter.class == converterClass) {
            styleConverter = com.sun.javafx.scene.layout.region.StrokeBorder.BorderStyleConverter.getInstance();
        }  else if (com.sun.javafx.scene.layout.region.StrokeBorder.BorderStyleSequenceConverter.class == converterClass) {
            styleConverter = com.sun.javafx.scene.layout.region.StrokeBorder.BorderStyleSequenceConverter.getInstance();
        }  else if (com.sun.javafx.scene.layout.region.StrokeBorder.LayeredBorderPaintConverter.class == converterClass) {
            styleConverter = com.sun.javafx.scene.layout.region.StrokeBorder.LayeredBorderPaintConverter.getInstance();
        }  else if (com.sun.javafx.scene.layout.region.StrokeBorder.LayeredBorderStyleConverter.class == converterClass) {
            styleConverter = com.sun.javafx.scene.layout.region.StrokeBorder.LayeredBorderStyleConverter.getInstance();

        }  else if (com.sun.javafx.scene.layout.region.Margins.Converter.class == converterClass) {
            styleConverter = com.sun.javafx.scene.layout.region.Margins.Converter.getInstance();
        }  else if (com.sun.javafx.scene.layout.region.Margins.SequenceConverter.class == converterClass) {
            styleConverter = com.sun.javafx.scene.layout.region.Margins.SequenceConverter.getInstance();

        // parser stuff
        }  else if (com.sun.javafx.css.parser.DeriveColorConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.parser.DeriveColorConverter.getInstance();
        }  else if (com.sun.javafx.css.parser.DeriveSizeConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.parser.DeriveSizeConverter.getInstance();
        }  else if (com.sun.javafx.css.parser.LadderConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.parser.LadderConverter.getInstance();
        }  else if (com.sun.javafx.css.parser.StopConverter.class == converterClass) {
            styleConverter = com.sun.javafx.css.parser.StopConverter.getInstance();
        }

        return styleConverter;
    }

}
