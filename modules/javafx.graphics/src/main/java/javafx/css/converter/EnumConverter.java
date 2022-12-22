/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package javafx.css.converter;

import com.sun.javafx.util.Logging;
import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.scene.text.Font;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Converter to convert a string representation of an {@code Enum} to an {@code Enum}.
 * @since 9
 */
public final class EnumConverter<E extends Enum<E>> extends StyleConverter<String, E> {

    // package for unit testing
    final Class<E> enumClass;

    /**
     * Creates an {@code EnumConvertor} object.
     * @param enumClass enum class
     */
    public EnumConverter(Class<E> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public E convert(ParsedValue<String, E> value, Font not_used) {
        if (enumClass == null) {
            return null;
        }
        String string = value.getValue();
        final int dotPos = string.lastIndexOf('.');
        if (dotPos > -1) {
            string = string.substring(dotPos + 1);
        }
        try {
            string = string.replace('-', '_');
            return Enum.valueOf(enumClass, string.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            // may throw another IllegalArgumentException
            return Enum.valueOf(enumClass, string);
        }
    }

    @Override
    public void writeBinary(DataOutputStream os, StringStore sstore) throws IOException {
        super.writeBinary(os,sstore);
        String ename = enumClass.getName();
        int index = sstore.addString(ename);
        os.writeShort(index);
    }


    /**
     * Reads binary {@code StyleConverter} data from a given {@code DataInputStream}.
     * @param is {@code DataInputStream} to read {@code StyleConverter} data from
     * @param strings string array containing StyleConverter details
     * @return a {@code StyleConverter} from read binary data
     * @throws IOException if reading from {@code DataInputStream} fails
     */
    public static StyleConverter<?,?> readBinary(DataInputStream is, String[] strings)
            throws IOException {

        short index = is.readShort();
        String ename = 0 <= index && index <= strings.length ? strings[index] : null;

        if (ename == null || ename.isEmpty()) return null;

        if (converters == null || converters.containsKey(ename) == false) {
            StyleConverter<?,?> converter = getInstance(ename);

            if (converter == null) {
                final PlatformLogger logger = Logging.getCSSLogger();
                if (logger.isLoggable(Level.SEVERE)) {
                    logger.severe("could not deserialize EnumConverter for " + ename);
                }
            }

            if (converters == null) converters = new HashMap<>();
            converters.put(ename, converter);
            return converter;
        }
        return converters.get(ename);
    }

    private static Map<String,StyleConverter<?,?>> converters;

    /**
     * Gets an {@code EnumConverter} instance for a given enum name.
     * @param ename enum name
     * @return an {@code EnumConverter} instance for a given enum name.
     */
    static public StyleConverter<?,?> getInstance(final String ename) {

        StyleConverter<?,?> converter = null;

        switch (ename) {
        case "com.sun.javafx.cursor.CursorType" :
            converter = new EnumConverter<>(com.sun.javafx.cursor.CursorType.class);
            break;
        case "javafx.scene.layout.BackgroundRepeat" :
        case "com.sun.javafx.scene.layout.region.Repeat" :
            converter = new EnumConverter<>(javafx.scene.layout.BackgroundRepeat.class);
            break;
        case "javafx.geometry.HPos" :
            converter = new EnumConverter<>(javafx.geometry.HPos.class);
            break;
        case "javafx.geometry.Orientation" :
            converter = new EnumConverter<>(javafx.geometry.Orientation.class);
            break;
        case "javafx.geometry.Pos" :
            converter = new EnumConverter<>(javafx.geometry.Pos.class);
            break;
        case "javafx.geometry.Side" :
            converter = new EnumConverter<>(javafx.geometry.Side.class);
            break;
        case "javafx.geometry.VPos" :
            converter = new EnumConverter<>(javafx.geometry.VPos.class);
            break;
        case "javafx.scene.effect.BlendMode" :
            converter = new EnumConverter<>(javafx.scene.effect.BlendMode.class);
            break;
        case "javafx.scene.effect.BlurType" :
            converter = new EnumConverter<>(javafx.scene.effect.BlurType.class);
            break;
        case "javafx.scene.paint.CycleMethod" :
            converter = new EnumConverter<>(javafx.scene.paint.CycleMethod.class);
            break;
        case "javafx.scene.shape.ArcType" :
            converter = new EnumConverter<>(javafx.scene.shape.ArcType.class);
            break;
        case "javafx.scene.shape.StrokeLineCap" :
            converter = new EnumConverter<>(javafx.scene.shape.StrokeLineCap.class);
            break;
        case "javafx.scene.shape.StrokeLineJoin" :
            converter = new EnumConverter<>(javafx.scene.shape.StrokeLineJoin.class);
            break;
        case "javafx.scene.shape.StrokeType" :
            converter = new EnumConverter<>(javafx.scene.shape.StrokeType.class);
            break;
        case "javafx.scene.text.FontPosture" :
            converter = new EnumConverter<>(javafx.scene.text.FontPosture.class);
            break;
        case "javafx.scene.text.FontSmoothingType" :
            converter = new EnumConverter<>(javafx.scene.text.FontSmoothingType.class);
            break;
        case "javafx.scene.text.FontWeight" :
            converter = new EnumConverter<>(javafx.scene.text.FontWeight.class);
            break;
        case "javafx.scene.text.TextAlignment" :
            converter = new EnumConverter<>(javafx.scene.text.TextAlignment.class);
            break;

        default :
            //
            // Enum types that are not in the javafx-ui-common source tree.
            //
            // Because the parser doesn't explicitly know about these enums
            // outside of the javafx-ui-common package, I don't expect these
            // EnumConverters to have been persisted. For example, the
            // -fx-text-overrun and -fx-content-display properties, will yield
            // a ParsedValue<String,String> with a null converter.
            //
            // If assertions are disabled, then null is returned. The StyleHelper
            // code will use the StyleableProperty's converter in this case.
            //
            assert false : "EnumConverter<"+ ename + "> not expected";

            final PlatformLogger logger = Logging.getCSSLogger();
            if (logger.isLoggable(Level.SEVERE)) {
                logger.severe("EnumConverter : converter Class is null for : "+ename);
            }
            break;
        }

        return converter;
    }


    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (other == null || !(other instanceof EnumConverter)) return false;
        return (enumClass.equals(((EnumConverter)other).enumClass));
    }

    @Override
    public int hashCode() {
        return enumClass.hashCode();
    }

    @Override
    public String toString() {
        return "EnumConverter[" + enumClass.getName() + "]";
    }
}
