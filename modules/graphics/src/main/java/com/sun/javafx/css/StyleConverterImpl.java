/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.css.CssMetaData;
import javafx.css.StyleConverter;
import javafx.css.Styleable;

import com.sun.javafx.Logging;
import sun.util.logging.PlatformLogger;
import sun.util.logging.PlatformLogger.Level;

/**
 * Converter converts ParsedValueImpl&lt;F,T&gt; from type F to type T.
 * F is the type of the parsed value, T is the converted type of
 * the ParsedValueImpl. For example, a converter from String to Color would
 * be declared
 * <p>&nbsp;&nbsp;&nbsp;&nbsp;
 * <code>public Color convert(ParsedValueImpl&lt;String,Color&gt; value, Font font)</code>
 * </p>
 */
public class StyleConverterImpl<F, T> extends StyleConverter<F, T> {

    /**
     * Convert from the constituent values to the target property type.
     * Implemented by Types that have Keys with subKeys.
     */
    public T convert(Map<CssMetaData<? extends Styleable, ?>,Object> convertedValues) {
        return null;
    }

    protected StyleConverterImpl() {
        super();
    }

    public void writeBinary(DataOutputStream os, StringStore sstore)
            throws IOException {

        String cname = getClass().getName();
        int index = sstore.addString(cname);
        os.writeShort(index);
    }

    // map of StyleConverter class name to StyleConverter
    private static Map<String,StyleConverter<?, ?>> tmap;

    @SuppressWarnings("rawtypes")
    public static StyleConverter<?,?> readBinary(DataInputStream is, String[] strings)
            throws IOException {

        int index = is.readShort();
        String cname = strings[index];

        if (cname == null || cname.isEmpty()) return null;

        if (cname.startsWith("com.sun.javafx.css.converters.EnumConverter")) {
            return (StyleConverter)com.sun.javafx.css.converters.EnumConverter.readBinary(is, strings);
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
        case "com.sun.javafx.css.converters.BooleanConverter" :
            styleConverter = com.sun.javafx.css.converters.BooleanConverter.getInstance();
            break;
        case "com.sun.javafx.css.converters.ColorConverter" :
            styleConverter = com.sun.javafx.css.converters.ColorConverter.getInstance();
            break;
        case "com.sun.javafx.css.converters.CursorConverter" :
            styleConverter = com.sun.javafx.css.converters.CursorConverter.getInstance();
            break;
        case "com.sun.javafx.css.converters.EffectConverter" :
            styleConverter = com.sun.javafx.css.converters.EffectConverter.getInstance();
            break;
        case "com.sun.javafx.css.converters.EffectConverter$DropShadowConverter" :
            styleConverter = com.sun.javafx.css.converters.EffectConverter.DropShadowConverter.getInstance();
            break;
        case "com.sun.javafx.css.converters.EffectConverter$InnerShadowConverter" :
            styleConverter = com.sun.javafx.css.converters.EffectConverter.InnerShadowConverter.getInstance();
            break;
        case "com.sun.javafx.css.converters.FontConverter" :
            styleConverter = com.sun.javafx.css.converters.FontConverter.getInstance();
            break;
        case "com.sun.javafx.css.converters.FontConverter$FontStyleConverter" :
        case "com.sun.javafx.css.converters.FontConverter$StyleConverter" :
            styleConverter = com.sun.javafx.css.converters.FontConverter.FontStyleConverter.getInstance();
            break;
        case "com.sun.javafx.css.converters.FontConverter$FontWeightConverter" :
        case "com.sun.javafx.css.converters.FontConverter$WeightConverter" :
            styleConverter = com.sun.javafx.css.converters.FontConverter.FontWeightConverter.getInstance();
            break;
        case "com.sun.javafx.css.converters.FontConverter$FontSizeConverter" :
        case "com.sun.javafx.css.converters.FontConverter$SizeConverter" :
            styleConverter = com.sun.javafx.css.converters.FontConverter.FontSizeConverter.getInstance();
            break;

        case "com.sun.javafx.css.converters.InsetsConverter" :
            styleConverter = com.sun.javafx.css.converters.InsetsConverter.getInstance();
            break;
        case "com.sun.javafx.css.converters.InsetsConverter$SequenceConverter" :
            styleConverter = com.sun.javafx.css.converters.InsetsConverter.SequenceConverter.getInstance();
            break;

        case "com.sun.javafx.css.converters.PaintConverter" :
            styleConverter = com.sun.javafx.css.converters.PaintConverter.getInstance();
            break;
        case "com.sun.javafx.css.converters.PaintConverter$SequenceConverter" :
            styleConverter = com.sun.javafx.css.converters.PaintConverter.SequenceConverter.getInstance();
            break;
        case "com.sun.javafx.css.converters.PaintConverter$LinearGradientConverter" :
            styleConverter = com.sun.javafx.css.converters.PaintConverter.LinearGradientConverter.getInstance();
            break;
        case "com.sun.javafx.css.converters.PaintConverter$RadialGradientConverter" :
            styleConverter = com.sun.javafx.css.converters.PaintConverter.RadialGradientConverter.getInstance();
            break;

        case "com.sun.javafx.css.converters.SizeConverter" :
            styleConverter = com.sun.javafx.css.converters.SizeConverter.getInstance();
            break;
        case "com.sun.javafx.css.converters.SizeConverter$SequenceConverter" :
            styleConverter = com.sun.javafx.css.converters.SizeConverter.SequenceConverter.getInstance();
            break;

        case "com.sun.javafx.css.converters.StringConverter" :
            styleConverter = com.sun.javafx.css.converters.StringConverter.getInstance();
            break;
        case "com.sun.javafx.css.converters.StringConverter$SequenceConverter" :
            styleConverter = com.sun.javafx.css.converters.StringConverter.SequenceConverter.getInstance();
            break;
        case "com.sun.javafx.css.converters.URLConverter" :
            styleConverter = com.sun.javafx.css.converters.URLConverter.getInstance();
            break;
        case "com.sun.javafx.css.converters.URLConverter$SequenceConverter" :
            styleConverter = com.sun.javafx.css.converters.URLConverter.SequenceConverter.getInstance();
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

        // parser stuff
        case "com.sun.javafx.css.parser.DeriveColorConverter" :
            styleConverter = com.sun.javafx.css.parser.DeriveColorConverter.getInstance();
            break;
        case "com.sun.javafx.css.parser.DeriveSizeConverter" :
            styleConverter = com.sun.javafx.css.parser.DeriveSizeConverter.getInstance();
            break;
        case "com.sun.javafx.css.parser.LadderConverter" :
            styleConverter = com.sun.javafx.css.parser.LadderConverter.getInstance();
            break;
        case "com.sun.javafx.css.parser.StopConverter" :
            styleConverter = com.sun.javafx.css.parser.StopConverter.getInstance();
            break;

            default :
            final PlatformLogger logger = Logging.getCSSLogger();
            if (logger.isLoggable(Level.SEVERE)) {
                logger.severe("StyleConverterImpl : converter Class is null for : "+converterClass);
            }
            break;
        }

        return styleConverter;
    }
}
