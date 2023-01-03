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

package com.sun.webkit.text;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class TextCodec {
    private final Charset charset;

    // The list of aliases where Java mappings are not compatible with WebKit.
    private static final Map<String, String> RE_MAP = Map.of(
        "ISO-10646-UCS-2", "UTF-16");

    /**
     * This could throw a runtime exception (see the documentation for the
     * Charset.forName.)  JNI code should handle the exception.
     */
    private TextCodec(String encoding) {
        charset = Charset.forName(encoding);
    }

    private byte[] encode(char[] data) {
        ByteBuffer bb = charset.encode(CharBuffer.wrap(data));
        byte[] encoded = new byte[bb.remaining()];
        bb.get(encoded);
        return encoded;
    }

    private String decode(byte[] data) {
        CharBuffer cb = charset.decode(ByteBuffer.wrap(data));
        char[] decoded = new char[cb.remaining()];
        cb.get(decoded);
        return new String(decoded);
    }

    /**
     * Returns an array of charset alias/name pairs.
     *
     * The aliases are stored at the even array positions, names are at the
     * following odd positions.
     *
     * @return  an array of charset alias/name pairs
     */
    private static String[] getEncodings() {
        List<String> encodings = new ArrayList<>();
        Map<String, Charset> ac = Charset.availableCharsets();
        for (Map.Entry<String, Charset> entry: ac.entrySet()) {
            String e = entry.getKey();
            encodings.add(e);
            encodings.add(e);
            Charset c = entry.getValue();
            for (String a : c.aliases()) {
                // 8859_1 is rejected in TextEncodingRegistry.cpp:isUndesiredAlias()
                // See also https://bugs.webkit.org/show_bug.cgi?id=43554
                if (a.equals("8859_1")) continue;

                encodings.add(a);
                String r = RE_MAP.get(a);
                encodings.add(r == null ? e : r);
            }
        }
        return encodings.toArray(new String[0]);
    }
}
