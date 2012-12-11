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
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.text.Font;
import com.sun.javafx.css.converters.EnumConverter;

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
    public T convert(Map<CssMetaData,Object> convertedValues) {
        return null;
    }

    private static StyleConverter CONVERTER = new StyleConverter();

    /** This converter simply returns value.getValue() */
    public static StyleConverter getInstance() {
        return CONVERTER;
    }

    protected StyleConverter() { }

    public void writeBinary(DataOutputStream os, StringStore sstore)
            throws IOException {

        String cname = getClass().getName();
        int index = sstore.addString(cname);
        os.writeShort(index);
    }

    // map of StyleConverter class name to StyleConverter
    private static Map<String,StyleConverter> tmap;

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
                    Method getInstanceMethod = cl.getMethod("getInstance");
                    converter = (StyleConverter) getInstanceMethod.invoke(null);
                }
            } catch (ClassNotFoundException cnfe) {
                // Class.forName failed
                System.err.println(cnfe.toString());
            } catch (Exception nsme) {
                // Class.forName failed
                System.err.println(nsme.toString());
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

}
