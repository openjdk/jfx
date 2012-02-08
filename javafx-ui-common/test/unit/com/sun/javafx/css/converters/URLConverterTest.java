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

import com.sun.javafx.css.ParsedValue;
import java.net.MalformedURLException;
import java.net.URL;
import javafx.scene.text.Font;
import org.junit.*;
import static org.junit.Assert.*;


public class URLConverterTest {

    public URLConverterTest() {
    }
    /**
     * Test of getInstance method, of class URLConverter.
     */
    @Test
    public void testGetInstance() {
        URLConverter result = URLConverter.getInstance();
        assertNotNull(result);
    }

    /**
     * Test of convert method, of class URLConverter.
     */
    @Test
    public void testConvertWithNullBaseURL() {
        
        ParsedValue[] values = new ParsedValue[] {
            new ParsedValue<String,String>("javafx/scene/package.html", null),
            new ParsedValue<URL,URL>(null,null)
        };
        ParsedValue<ParsedValue[], String> value = 
            new ParsedValue<ParsedValue[], String>(values, URLConverter.getInstance());
                
        Font font = null;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String expResult = cl.getResource("javafx/scene/package.html").toExternalForm();
        String result = value.convert(font);
        assertEquals(expResult, result);
    }

    public void testConvertWithBaseURL() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL base = cl.getResource("javafx/..");        
        ParsedValue[] values = new ParsedValue[] {
            new ParsedValue<String,String>("javafx/scene/package.html", null),
            new ParsedValue<URL,URL>(base,null)
        };
        ParsedValue<ParsedValue[], String> value = 
            new ParsedValue<ParsedValue[], String>(values, URLConverter.getInstance());
                
        Font font = null;
        String expResult = cl.getResource("javafx/scene/package.html").toExternalForm(); 
        String result = value.convert(font);
        assertEquals(expResult, result);
    }
    
    @Test
    public void testConvertWithAbsoluteURLAndNullBaseURL() {
        
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String expResult = cl.getResource("javafx/scene/package.html").toExternalForm();
        ParsedValue[] values = new ParsedValue[] {
            new ParsedValue<String,String>(expResult, null),
            new ParsedValue<URL,URL>(null,null)
        };
        ParsedValue<ParsedValue[], String> value = 
            new ParsedValue<ParsedValue[], String>(values, URLConverter.getInstance());
                
        Font font = null;
        String result = value.convert(font);
        assertEquals(expResult, result);
    }

    @Test
    public void testConvertWithAbsoluteURLWithBaseURL() {
        
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL baseURL = cl.getResource("javafx/..");
        String expResult = cl.getResource("javafx/scene/package.html").toExternalForm();
        ParsedValue[] values = new ParsedValue[] {
            new ParsedValue<String,String>(expResult, null),
            new ParsedValue<URL,URL>(baseURL,null)
        };
        ParsedValue<ParsedValue[], String> value = 
            new ParsedValue<ParsedValue[], String>(values, URLConverter.getInstance());
                
        Font font = null;
        String result = value.convert(font);
        assertEquals(expResult, result);
    }
    
}
