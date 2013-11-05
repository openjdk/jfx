/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.ipack.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class Util {
    private static final char[] HEX_SYMBOLS = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
        };

    private Util() {
    }

    public static String hex(final byte[] value) {
        final char[] buffer = new char[value.length * 2];
        for (int i = 0; i < value.length; ++i) {
            Util.addHex8(buffer, i * 2, value[i]);
        }
        return String.valueOf(buffer);
    }

    public static String hex32(final int value) {
        final char[] buffer = new char[8];
        addHex32(buffer, 0, value);
        return String.valueOf(buffer);
    }

    public static String hex16(final int value) {
        final char[] buffer = new char[4];
        addHex16(buffer, 0, value);
        return String.valueOf(buffer);
    }

    public static String hex8(final int value) {
        final char[] buffer = new char[2];
        addHex8(buffer, 0, value);
        return String.valueOf(buffer);
    }

    public static void dumpHex(final byte[] data) {
        dumpHex(data, 0, data.length);
    }

    public static void dumpHex(final byte[] data,
                               final int offset,
                               final int length) {
        final int fullLinesMax = offset + length & ~15;
        int i;

        for (i = offset; i < fullLinesMax; i += 16) {
            dumpHexLine(data, i, 16);
        }

        if (i != (offset + length)) {
            dumpHexLine(data, i, offset + length - i);
        }
    }

    public static String readString(final DataInput dataInput,
                                    final int numBytes) throws IOException {
        final byte[] buffer = new byte[numBytes];
        dataInput.readFully(buffer);
        return new String(buffer);
    }

    public static String readString(final DataInput dataInput)
            throws IOException {
        final StringBuilder sb = new StringBuilder();
        byte b;
        while ((b = dataInput.readByte()) != 0) {
            sb.append((char) (b & 0xff));
        }

        return sb.toString();
    }

    public static void writeString(final DataOutput dataOutput,
                                   final String value,
                                   final int size,
                                   final char paddingChar)
            throws IOException {
        final byte[] valueBytes = value.getBytes();
        final int numValueBytesToStore = (valueBytes.length < size)
                                                 ? valueBytes.length
                                                 : size;
        dataOutput.write(valueBytes, 0, numValueBytesToStore);

        if (numValueBytesToStore == size) {
            return;
        }

        final byte paddingByte = (byte) paddingChar;
        for (int i = size - numValueBytesToStore; i > 0; --i) {
            dataOutput.writeByte(paddingByte);
        }
    }

    public static void addHex32(final char[] buffer,
                                final int offset,
                                final int value) {
        addHex16(buffer, offset, value >> 16);
        addHex16(buffer, offset + 4, value);
    }

    public static void addHex16(final char[] buffer,
                                final int offset,
                                final int value) {
        addHex8(buffer, offset, value >> 8);
        addHex8(buffer, offset + 2, value);
    }

    public static void addHex8(final char[] buffer,
                               final int offset,
                               final int value) {
        buffer[offset] = HEX_SYMBOLS[(value >> 4) & 0xf];
        buffer[offset + 1] = HEX_SYMBOLS[value & 0xf];
    }

    private static void dumpHexLine(final byte[] data,
                                    final int offset,
                                    final int length) {
        if (length > 0) {
            final char[] lineBuffer = new char[length * 2 + length - 1];
            addHex8(lineBuffer, 0, data[offset]);
            for (int i = 1; i < length; ++i) {
                lineBuffer[3 * i - 1] = ' ';
                addHex8(lineBuffer, 3 * i, data[offset + i]);
            }

            System.out.print(String.valueOf(lineBuffer));
        }
        System.out.println();
    }
}
