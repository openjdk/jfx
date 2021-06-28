/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.css.converters;

import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.scene.text.Font;
import com.sun.javafx.css.ParsedValueImpl;
import javafx.css.converter.URLConverter;
import org.junit.Test;

import static org.junit.Assert.*;


public class URLConverterTest {

    public URLConverterTest() {
    }
    /**
     * Test of getInstance method, of class URLConverter.
     */
    @Test
    public void testGetInstance() {
        StyleConverter<ParsedValue[],String> result = URLConverter.getInstance();
        assertNotNull(result);
    }

    /**
     * Test of convert method, of class URLConverter.
     */
    @Test
    public void testConvertWithNullBaseURL() {

        ParsedValue[] values = new ParsedValue[] {
            new ParsedValueImpl<String,String>("test/javafx/css/converter/some.txt", null),
            new ParsedValueImpl<String,String>(null,null)
        };
        ParsedValueImpl<ParsedValue[], String> value =
            new ParsedValueImpl<ParsedValue[], String>(values, URLConverter.getInstance());

        Font font = null;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String expResult = cl.getResource("test/javafx/css/converter/some.txt").toExternalForm();
        String result = value.convert(font);
        assertEquals(expResult, result);
    }

    public void testConvertWithBaseURL() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String base = cl.getResource("com/..").toExternalForm();
        ParsedValue[] values = new ParsedValue[] {
            new ParsedValueImpl<String,String>("test/javafx/css/converter/some.txt", null),
            new ParsedValueImpl<String,String>(base,null)
        };
        ParsedValueImpl<ParsedValue[], String> value =
            new ParsedValueImpl<ParsedValue[], String>(values, URLConverter.getInstance());

        Font font = null;
        String expResult = cl.getResource("test/javafx/css/converter/some.txt").toExternalForm();
        String result = value.convert(font);
        assertEquals(expResult, result);
    }

    @Test
    public void testConvertWithAbsoluteURLAndNullBaseURL() {

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String expResult = cl.getResource("test/javafx/css/converter/some.txt").toExternalForm();
        ParsedValue[] values = new ParsedValue[] {
            new ParsedValueImpl<String,String>(expResult, null),
            new ParsedValueImpl<String,String>(null,null)
        };
        ParsedValueImpl<ParsedValue[], String> value =
            new ParsedValueImpl<ParsedValue[], String>(values, URLConverter.getInstance());

        Font font = null;
        String result = value.convert(font);
        assertEquals(expResult, result);
    }

    @Test
    public void testConvertWithAbsoluteURLWithBaseURL() {

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String baseURL = cl.getResource("com/..").toExternalForm();
        String expResult = cl.getResource("test/javafx/css/converter/some.txt").toExternalForm();
        ParsedValue[] values = new ParsedValue[] {
            new ParsedValueImpl<String,String>(expResult, null),
            new ParsedValueImpl<String,String>(baseURL,null)
        };
        ParsedValueImpl<ParsedValue[], String> value =
            new ParsedValueImpl<ParsedValue[], String>(values, URLConverter.getInstance());

        Font font = null;
        String result = value.convert(font);
        assertEquals(expResult, result);
    }

    @Test
    public void testConvertWithDataURI() {
        String dataUri = "data:text/plain;charset=utf-8;base64,SGVsbG8sIFdvcmxkIQ==";

        ParsedValue[] values = new ParsedValue[] { new ParsedValueImpl<String,String>(dataUri, null) };
        ParsedValueImpl<ParsedValue[], String> value = new ParsedValueImpl<>(values, URLConverter.getInstance());

        String result = value.convert(null);
        assertEquals(dataUri, result);
    }

}
