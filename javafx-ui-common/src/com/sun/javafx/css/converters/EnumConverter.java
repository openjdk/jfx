/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.css.converters;

import com.sun.javafx.css.StringStore;
import com.sun.javafx.css.StyleConverterImpl;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javafx.css.ParsedValue;
import javafx.scene.text.Font;

public final class EnumConverter<E extends Enum<E>> extends StyleConverterImpl<String, E> {

    private final Class<E> enumClass;

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
            return Enum.valueOf(enumClass, string.toUpperCase());
        } catch (IllegalArgumentException e) {
            // may throw another IllegalArgumentException
            return Enum.valueOf(enumClass, string);
        }
    }

    @Override
    public void writeBinary(DataOutputStream os, StringStore ss) throws IOException {
        super.writeBinary(os,ss);
        int index = ss.addString(enumClass.getName());
        os.writeShort(index);
    }

    @SuppressWarnings("unchecked") // Pending RT-27146
    public EnumConverter(DataInputStream is, String[] strings) {
        Class<E> eclass = null;
        try {
            int index = is.readShort();
            String cname = strings[index];
            // Unchecked!
            eclass = (Class<E>)Class.forName(cname);
        } catch (IOException ioe) {
            System.err.println("EnumConveter caught: " + ioe);
        } catch (ClassNotFoundException cnfe) {
            System.err.println("EnumConveter caught: " + cnfe.toString());
        }
        enumClass = eclass;
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
        return "EnumConveter[" + enumClass.getName() + "]";
    }
}
