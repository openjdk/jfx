/*
* Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.Scene;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Ignore;


public class RuleTest {
    
    public RuleTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Test
    public void testGetSelectors() {
        List<Selector> expResult = new ArrayList<Selector>();
        expResult.add(Selector.createSelector("One.two#three"));
        expResult.add(Selector.createSelector("Four.five#six"));
        Rule instance = new Rule(expResult, Collections.EMPTY_LIST);
        List result = instance.getSelectors();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetDeclarations() {
        List<Declaration> expResult = new ArrayList<Declaration>();
        expResult.add(new Declaration("one", new ParsedValue<String,String>("one", null), false));
        expResult.add(new Declaration("two", new ParsedValue<String,String>("two", null), false));
        expResult.add(new Declaration("three", new ParsedValue<String,String>("three", null), false));
        Rule instance = new Rule(Collections.EMPTY_LIST, expResult);
        List result = instance.getDeclarations();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetStylesheet() {
        Rule instance = new Rule(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        Stylesheet expResult = new Stylesheet();
        expResult.getRules().add(instance);
        Stylesheet result = instance.getStylesheet();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetOriginAfterSettingOriginAfterAddingRuleToStylesheet() {
        Rule instance = new Rule(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        Stylesheet stylesheet = new Stylesheet();
        stylesheet.getRules().add(instance);
        stylesheet.setOrigin(Origin.INLINE);
        Origin expResult = Origin.INLINE;
        Origin result = instance.getOrigin();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetOriginAfterSettingOriginBeforeAddingRuleToStylesheet() {
        Rule instance = new Rule(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        Stylesheet stylesheet = new Stylesheet();
        stylesheet.setOrigin(Origin.INLINE);
        stylesheet.getRules().add(instance);
        Origin expResult = Origin.INLINE;
        Origin result = instance.getOrigin();
        assertEquals(expResult, result);
    }
    
    @Test
    public void testGetOriginWithoutAddingRuleToStylesheet() {
        Rule instance = new Rule(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        Origin result = instance.getOrigin();
        assertNull(result);
    }

    @Test
    public void testGetOriginAfterRemovingRuleFromStylesheet() {
        Rule instance = new Rule(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        Stylesheet stylesheet = new Stylesheet();
        stylesheet.getRules().add(instance);
        stylesheet.setOrigin(Origin.INLINE);
        stylesheet.getRules().remove(instance);        
        Origin result = instance.getOrigin();
        assertNull(result);
    }
    
    @Ignore @Test
    public void testMatches_Node() {
        System.out.println("matches");
        Node node = null;
        Rule instance = null;
        List expResult = null;
        List result = instance.matches(node);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    @Ignore @Test
    public void testMatches_Scene() {
        System.out.println("matches");
        Scene scene = null;
        Rule instance = null;
        List expResult = null;
        List result = instance.matches(scene);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    @Ignore @Test
    public void testMightApply() {
        System.out.println("mightApply");
        String className = "";
        String id = "";
        List<String> styleClasses = null;
        Rule instance = null;
        boolean expResult = false;
        boolean result = instance.mightApply(className, id, styleClasses);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    @Ignore @Test
    public void testApplies() {
        System.out.println("applies");
        Node node = null;
        Rule instance = null;
        boolean expResult = false;
        boolean result = instance.applies(node);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    @Ignore @Test
    public void testToString() {
        System.out.println("toString");
        Rule instance = null;
        String expResult = "";
        String result = instance.toString();
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    @Ignore @Test
    public void testWriteBinary() throws Exception {
        System.out.println("writeBinary");
        DataOutputStream os = null;
        StringStore stringStore = null;
        Rule instance = null;
        instance.writeBinary(os, stringStore);
        fail("The test case is a prototype.");
    }

    @Ignore @Test
    public void testReadBinary() throws Exception {
        System.out.println("readBinary");
        DataInputStream is = null;
        String[] strings = null;
        Rule expResult = null;
        Rule result = Rule.readBinary(is, strings);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }
}
