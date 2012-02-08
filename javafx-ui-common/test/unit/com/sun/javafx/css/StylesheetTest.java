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
package com.sun.javafx.css;

import com.sun.javafx.css.Stylesheet.Origin;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import org.junit.*;
import static org.junit.Assert.*;


public class StylesheetTest {

    URL testURL = null;
    
    public StylesheetTest() {
        testURL = getClass().getResource("HonorDeveloperSettingsTest.css");
    }

    /**
     * Test of getUrl method, of class Stylesheet.
     */
    @Test
    public void testGetUrl() {
        Stylesheet instance = new Stylesheet();
        URL expResult = null;
        URL result = instance.getUrl();
        assertEquals(expResult, result);
        
        instance = new Stylesheet(testURL);
        result = instance.getUrl();
        assertEquals(testURL, result);
    }

    /**
     * Test of getSource method, of class Stylesheet.
     */
    @Test
    public void testGetStylesheetSourceGetterAndSetter() {
        Stylesheet instance = new Stylesheet();
        Origin expResult = Origin.AUTHOR;
        Origin result = instance.getOrigin();
        assertEquals(expResult, result);
        
        instance.setOrigin(Origin.INLINE);
        expResult = Origin.INLINE;
        result = instance.getOrigin();
        assertEquals(expResult, result);

        instance.setOrigin(Origin.USER);
        expResult = Origin.USER;
        result = instance.getOrigin();
        assertEquals(expResult, result);

        instance.setOrigin(Origin.USER_AGENT);
        expResult = Origin.USER_AGENT;
        result = instance.getOrigin();
        assertEquals(expResult, result);
    }

    /**
     * Test of addRule method, of class Stylesheet.
     */
    @Test
    public void testStylesheetAddAndGetRule() {
        Rule rule = new Rule(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        Stylesheet instance = new Stylesheet();
        instance.getRules().add(rule);
        instance.getRules().add(rule);
        instance.getRules().add(rule);
        instance.getRules().add(rule);
        instance.getRules().add(rule);
        List<Rule> rules = instance.getRules();
        assert(rules.size() == 5);
        for(Rule r : rules) assertEquals(r, rule);
    }
    
    @Test
    public void testAddingRuleSetsStylesheetOnRule() {
        Rule rule = new Rule(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        Stylesheet instance = new Stylesheet();
        instance.getRules().add(rule);
        assert(rule.getStylesheet() == instance);        
    }

    @Test
    public void testRemovingRuleSetsStylesheetNullOnRule() {
        Rule rule = new Rule(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        Stylesheet instance = new Stylesheet();
        instance.getRules().add(rule);
        instance.getRules().remove(rule);
        assertNull(rule.getStylesheet());
    }
    
    /**
     * Test of equals method, of class Stylesheet.
     */
    @Test
    public void testStylesheetEquals() {
        Object obj = new Stylesheet();
        Stylesheet instance = new Stylesheet();
        boolean expResult = true;
        boolean result = instance.equals(obj);
        assertEquals(expResult, result);

        obj = new Stylesheet(testURL);
        instance = new Stylesheet(testURL);
        expResult = true;
        result = instance.equals(obj);
        assertEquals(expResult, result);

        obj = new Stylesheet();
        instance = new Stylesheet(testURL);
        expResult = false;
        result = instance.equals(obj);
        assertEquals(expResult, result);
        
        obj = instance = new Stylesheet(testURL);
        expResult = true;
        result = instance.equals(obj);
        assertEquals(expResult, result);
        
    }

    /**
     * Test of toString method, of class Stylesheet.
     */
    @Test
    public void testStylesheetToString() {
        Stylesheet instance = new Stylesheet();
        String expResult = "/*  */";
        String result = instance.toString();
        assertEquals(expResult, result);

        instance = new Stylesheet(testURL);
        expResult = "/* " + testURL.toExternalForm() + " */";
        result = instance.toString();
        assertEquals(expResult, result);

        instance = new Stylesheet(testURL);
        Rule rule = new Rule(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        instance.getRules().add(rule);
        expResult = "/* " + testURL.toExternalForm() + " */\n{\n}\n";
        result = instance.toString();
        assertEquals(expResult, result);
    }

    /**
     * Test of writeBinary method, of class Stylesheet.
    @Test
    public void testWriteAndReadBinary() throws Exception {
        DataOutputStream os = null;
        StringStore stringStore = null;
        Stylesheet instance = new Stylesheet(testURL);
        instance.writeBinary(os, stringStore);
    }
     */

    /**
     * Test of loadBinary method, of class Stylesheet.
    @Test
    public void testLoadBinary() {
        System.out.println("loadBinary");
        URL url = null;
        Stylesheet expResult = null;
        Stylesheet result = Stylesheet.loadBinary(url);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
     */
}
