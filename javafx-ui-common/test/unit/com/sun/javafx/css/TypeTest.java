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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;


public class TypeTest {

    // Key key = TypeTest.getKeyByName("-fx-cursor", Node.impl_CSS_KEYS);
    public static StyleablePropertyMetaData getStyleablePropertyMetaDataByName(String name, StyleablePropertyMetaData[] keys) {
        StyleablePropertyMetaData keyForName = null;
        for (StyleablePropertyMetaData k : keys) {
            if (k.getProperty().equals(name)) {
                keyForName = k;
                break;
            }
        }
        assertNotNull(keyForName);
        return keyForName;
    }

    // ParsedValue value = TypeTest.getValueFor(stylesheet, "-fx-cursor")
    public static ParsedValue getValueFor(Stylesheet stylesheet, String property ) {
        for (Rule rule : stylesheet.getRules()) {
            for (Declaration decl : rule.getDeclarations()) {
                if (property.equals(decl.getProperty())) {
                    return decl.getParsedValue();
                }
            }
        }
        fail("getValueFor " + property);
        return null;
    }

    public TypeTest() {
    }

    @Test
    public void testType() {
        // All the tests have been stubbed out for now (since
        // the other tests implicitly or explicitly test Type).
        // But a unit test has to have a runnable method.
    }

    /**
     * Test of ENUM method, of class Type.
     */
//    @Test
//    public void testENUM() {
//        System.out.println("ENUM");
//        Class type = null;
//        Type expResult = null;
//        Type result = Type.ENUM(type);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of convert method, of class Type.
     */
//    @Test
//    public void testConvert_Value_Font() {
//        System.out.println("convert");
//        ParsedValue<Font> value = null;
//        Font font = null;
//        Type instance = new Type();
//        Object expResult = null;
//        Object result = instance.convert(value, font);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of convert method, of class Type.
     */
//    @Test
//    public void testConvert_KeyArr() {
//        System.out.println("convert");
//        Key[] keys = null;
//        Type instance = new Type();
//        Object expResult = null;
//        Object result = instance.convert(keys);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of writeCssSerializable method, of class Type.
     */
//    @Test
//    public void testWriteCssSerializable() throws Exception {
//        System.out.println("writeCssSerializable");
//        DataOutputStream stream = null;
//        StringStore ss = null;
//        CssSerializable cs = null;
//        Type instance = new Type();
//        instance.writeCssSerializable(stream, ss, cs);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of readCssSerializable method, of class Type.
     */
//    @Test
//    public void testReadCssSerializable() throws Exception {
//        System.out.println("readCssSerializable");
//        DataInputStream stream = null;
//        String[] strings = null;
//        Type instance = new Type();
//        Object expResult = null;
//        Object result = instance.readCssSerializable(stream, strings);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of writePaint method, of class Type.
     */
//    @Test
//    public void testWritePaint() throws Exception {
//        System.out.println("writePaint");
//        DataOutputStream stream = null;
//        Paint paint = null;
//        Type.writePaint(stream, paint);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of readPaint method, of class Type.
     */
//    @Test
//    public void testReadPaint() throws Exception {
//        System.out.println("readPaint");
//        DataInputStream stream = null;
//        Paint expResult = null;
//        Paint result = Type.readPaint(stream);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of writeColor method, of class Type.
     */
//    @Test
//    public void testWriteColor() throws Exception {
//        System.out.println("writeColor");
//        DataOutputStream stream = null;
//        Color color = null;
//        Type.writeColor(stream, color);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of readColor method, of class Type.
     */
//    @Test
//    public void testReadColor() throws Exception {
//        System.out.println("readColor");
//        DataInputStream stream = null;
//        Color expResult = null;
//        Color result = Type.readColor(stream);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of writeLinearGradient method, of class Type.
     */
//    @Test
//    public void testWriteLinearGradient() throws Exception {
//        System.out.println("writeLinearGradient");
//        DataOutputStream stream = null;
//        LinearGradient lg = null;
//        Type.writeLinearGradient(stream, lg);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of readLinearGradient method, of class Type.
     */
//    @Test
//    public void testReadLinearGradient() throws Exception {
//        System.out.println("readLinearGradient");
//        DataInputStream stream = null;
//        LinearGradient expResult = null;
//        LinearGradient result = Type.readLinearGradient(stream);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of writeRadialGradient method, of class Type.
     */
//    @Test
//    public void testWriteRadialGradient() throws Exception {
//        System.out.println("writeRadialGradient");
//        DataOutputStream stream = null;
//        RadialGradient rg = null;
//        Type.writeRadialGradient(stream, rg);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of readRadialGradient method, of class Type.
     */
//    @Test
//    public void testReadRadialGradient() throws Exception {
//        System.out.println("readRadialGradient");
//        DataInputStream stream = null;
//        RadialGradient expResult = null;
//        RadialGradient result = Type.readRadialGradient(stream);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of readCycleMethod method, of class Type.
     */
//    @Test
//    public void testReadCycleMethod() throws Exception {
//        System.out.println("readCycleMethod");
//        DataInputStream stream = null;
//        CycleMethod expResult = null;
//        CycleMethod result = Type.readCycleMethod(stream);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of writeStop method, of class Type.
     */
//    @Test
//    public void testWriteStop() throws Exception {
//        System.out.println("writeStop");
//        DataOutputStream stream = null;
//        Stop stop = null;
//        Type.writeStop(stream, stop);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of readStop method, of class Type.
     */
//    @Test
//    public void testReadStop() throws Exception {
//        System.out.println("readStop");
//        DataInputStream stream = null;
//        Stop expResult = null;
//        Stop result = Type.readStop(stream);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of isMightHaveLookups method, of class Type.
     */
//    @Test
//    public void testIsMightHaveLookups() {
//        System.out.println("isMightHaveLookups");
//        Type instance = new Type();
//        boolean expResult = false;
//        boolean result = instance.isMightHaveLookups();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

}
