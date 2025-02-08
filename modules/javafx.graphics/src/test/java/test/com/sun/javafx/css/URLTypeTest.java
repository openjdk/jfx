/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.css;

import com.sun.javafx.css.ParsedValueImpl;
import javafx.css.ParsedValue;
import javafx.scene.text.Font;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javafx.css.converter.StringConverter;
import javafx.css.converter.URLConverter;


public class URLTypeTest {

    public URLTypeTest() {
    }

    static final String absClassName = "/test/com/sun/javafx/css/URLTypeTest.class";
    static final String classURL = URLTypeTest.class.getResource("URLTypeTest.class").toExternalForm();

    final String baseURL = "http://a/b/c/d;p?q";

    // from rfc3986, section 5
    final String[][] testPairs = new String[][] {
        {"file:h"           ,  "file:h"},
        {"g"             ,  "http://a/b/c/g"},
        {"./g"           ,  "http://a/b/c/g"},
        {"g/"            ,  "http://a/b/c/g/"},
        // The following is relative to classloader root, and since it won't be found, will return null
        {"/g"            ,  null},
        // The following is relative to classloader root and will be resolved as such
        {absClassName    ,  classURL},
        {"//g"           ,  "http://g"},
        // actual is http://a/b/c/?y - bug in java.net.URI?       {"?y"            ,  "http://a/b/c/d;p?y"},
        {"g?y"           ,  "http://a/b/c/g?y"},
        {"#s"            ,  "http://a/b/c/d;p?q#s"},
        {"g#s"           ,  "http://a/b/c/g#s"},
        {"g?y#s"         ,  "http://a/b/c/g?y#s"},
        {";x"            ,  "http://a/b/c/;x"},
        {"g;x"           ,  "http://a/b/c/g;x"},
        {"g;x?y#s"       ,  "http://a/b/c/g;x?y#s"},
        // empty string causes URISyntaxException, so converter returns null {""              ,  "http://a/b/c/d;p?q"},
        {"", null}, // not part of the rfc test suite - converter returns null if resolving base against empty string
        {"."             ,  "http://a/b/c/"},
        {"./"            ,  "http://a/b/c/"},
        {".."            ,  "http://a/b/"},
        {"../"           ,  "http://a/b/"},
        {"../g"          ,  "http://a/b/g"},
        {"../.."         ,  "http://a/"},
        {"../../"        ,  "http://a/"},
        {"../../g"       ,  "http://a/g"}
      };

    /**
     * Test of convert method, of class URLType.
     */
    @Test
    public void testConvert() {
        //System.out.println("convert");
        ParsedValue<ParsedValue[],String>[] urls = new ParsedValue[testPairs.length];

        for(int n=0; n<testPairs.length; n++) {
            ParsedValue[] values = new ParsedValue[] {
                new ParsedValueImpl<>(testPairs[n][0], StringConverter.getInstance()),
                new ParsedValueImpl<String, String>(baseURL, null)
            };
            urls[n] = new ParsedValueImpl<>(values, URLConverter.getInstance());
        }

        ParsedValue<ParsedValue<ParsedValue[],String>[],String[]> value =
                new ParsedValueImpl<>(urls, URLConverter.SequenceConverter.getInstance());

        Font font = null;
        String[] result = value.convert(font);
        assertEquals(testPairs.length, result.length);
        for(int n=0; n<result.length; n++) {
            String msg = "[" + n + "]" + "resolve \'" + testPairs[n][0] + "\'";
            assertEquals(testPairs[n][1], result[n], msg);
        }
    }

}
