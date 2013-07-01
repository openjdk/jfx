/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.text;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class TextCodec {
    private final Charset charset;

    // The list of aliases where Java mappings are not compatible with WebKit.
    private static final Map<String, String> reMap =
            new HashMap<String, String>();
    static {
        reMap.put("ISO-10646-UCS-2", "UTF-16");
    }
        
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
        List<String> encodings = new ArrayList<String>();
        Map<String, Charset> ac = Charset.availableCharsets();
        for (Map.Entry<String, Charset> entry: ac.entrySet()) {
            String e = entry.getKey();
            encodings.add(e);
            encodings.add(e);
            Charset c = entry.getValue();
            for (String a : c.aliases()) {
                // 8859_1 is blacklisted in TextEncodingRegistry.cpp:isUndesiredAlias()
                // See also https://bugs.webkit.org/show_bug.cgi?id=43554
                if (a.equals("8859_1")) continue;

                encodings.add(a);
                String r = reMap.get(a);
                encodings.add(r == null ? e : r);
            }
        }
        return encodings.toArray(new String[0]);
    }
}
