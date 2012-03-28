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
package javafx.scene.text;

import static org.junit.Assert.assertEquals;
import javafx.geometry.VPos;
import javafx.scene.NodeTest;

import org.junit.Test;

public class TextTest {
    
    @Test public void testCtors() {
        Text t1 = new Text();
        assertEquals("", t1.getText());
        Text t2 = new Text("test content");
        assertEquals("test content", t2.getText());
        Text t3 = new Text(10, 20, "2");
        assertEquals(10f, t3.getX(), 0);
        assertEquals(20f, t3.getY(), 0);
        assertEquals("2", t3.getText());
    }
    
    @Test public void testSettingNullText() {
        Text t = new Text();
        t.setText(null);
        assertEquals("", t.getText());
        t.textProperty().set(null);
        assertEquals("", t.getText());
        t.setText("1");
        assertEquals("1", t.getText());
        assertEquals("1", t.textProperty().get());
        t.setText(null);
        assertEquals("", t.getText());
        t.textProperty().set(null);
        assertEquals("", t.getText());
    }

    @Test public void testDefaultTextNotNull() {
        Text t = new Text();
        assertEquals("", t.getText());
        assertEquals("", t.textProperty().get());
    }
    
    @Test public void testStoreFont() {
        Text t = new Text();
        Font f = new Font(44);
        assertEquals(Font.getDefault(), t.getFont());
        t.setFont(f);
        assertEquals(44f, t.getBaselineOffset(), 0);
    }
    
 // Commented out as StubFontLoader only knows about Amble and its
 // also not a given that the Font.getDefault() matches the default font
 // on a Text node anyway, as CSS defaults are applied to the Text node.
/*
    @Test public void testPropertyPropagation_font() throws Exception {
        final Text node = new Text();
        NodeTest.testObjectPropertyPropagation(node, "font", Font.getDefault(), new Font(44));
    }
*/

//     @Test public void testPropertyPropagation_textOrigin() throws Exception {
//         final Text node = new Text();
//         NodeTest.testObjectPropertyPropagation(node, "textOrigin", "textOrigin",
//                 VPos.BASELINE, VPos.TOP, new NodeTest.ObjectValueConvertor() {
//                     @Override
//                     public Object toSg(Object pgValue) {
//                         return VPos.values()[((Number)pgValue).intValue()];
//                     }
//                 });
//     }
    
//     @Test public void testPropertyPropagation_boundsType() throws Exception {
//         final Text node = new Text();
//         NodeTest.testObjectPropertyPropagation(node, "boundsType", "textBoundsType",
//                 TextBoundsType.LOGICAL, TextBoundsType.VISUAL, new NodeTest.ObjectValueConvertor() {
//                     @Override
//                     public Object toSg(Object pgValue) {
//                         return TextBoundsType.values()[((Number)pgValue).intValue()];
//                     }
//                 });
//     }
    
//     @Test public void testPropertyPropagation_textAlignment() throws Exception {
//         final Text node = new Text();
//         NodeTest.testObjectPropertyPropagation(node, "textAlignment", "textAlignment", 
//                 TextAlignment.LEFT, TextAlignment.CENTER, new NodeTest.ObjectValueConvertor() {
//                     @Override
//                     public Object toSg(Object pgValue) {
//                         return TextAlignment.values()[(((Number)pgValue).intValue())];
//                     }
//                 });
//     }
    
//     @Test public void testPropertyPropagation_visible() throws Exception {
//         final Text node = new Text();
//         NodeTest.testBooleanPropertyPropagation(node, "visible", false, true);
//     }

//     @Test public void testPropertyPropagation_text() throws Exception {
//         final Text node = new Text();
//         NodeTest.testObjectPropertyPropagation(node, "text", "text", "Hello", "World");
//     }

//     @Test public void testPropertyPropagation_strikethrough() throws Exception {
//         final Text node = new Text();
//         NodeTest.testBooleanPropertyPropagation(node, "strikethrough", false, true);
//     }

//     @Test public void testPropertyPropagation_underline() throws Exception {
//         final Text node = new Text();
//         NodeTest.testBooleanPropertyPropagation(node, "underline", false, true);
//     }

//     @Test public void testPropertyPropagation_x() throws Exception {
//         final Text node = new Text();
//         NodeTest.testDoublePropertyPropagation(node, "x", 100, 200);
//     }

//     @Test public void testPropertyPropagation_y() throws Exception {
//         final Text node = new Text();
//         NodeTest.testDoublePropertyPropagation(node, "y", 100, 200);
//     }

//     @Test public void testPropertyPropagation_wrappingWidth() throws Exception {
//         final Text node = new Text();
//         NodeTest.testDoublePropertyPropagation(node, "wrappingWidth", 100, 200);
//     }

//     @Test public void testBoundPropertySync_X() throws Exception {
//         NodeTest.assertDoublePropertySynced(
//                 new Text(1.0, 2.0, "The Text"),
//                 "x", "x", 10.0);
//     }

//     @Test public void testBoundPropertySync_Y() throws Exception {
//         NodeTest.assertDoublePropertySynced(
//                 new Text(1.0, 2.0, "The Text"),
//                 "y", "y", 20.0);
//     }

//     @Test public void testBoundPropertySync_Text() throws Exception {
//         NodeTest.assertStringPropertySynced(
//                 new Text(1.0, 2.0, "The Text"),
//                 "text", "text", "The Changed Text");
//     }

//     // The StubFontLoader is not adequate. SansSerif is the default font
//     // family. But StubFontLoader is hard coded with some knowledge of
//     // Amble so we end up with a null reference for its the PGFont
//     // and it sets null on the PGText node. StubFontLoader needs to be
//     // replaced with the real font loader.
// /*
//     @Test public void testBoundPropertySync_Font() throws Exception {
//         List<String> fontNames = Font.getFontNames();
//         String fontName = fontNames.get(fontNames.size() - 1);
//         NodeTest.assertObjectPropertySynced(
//                 new Text(1.0, 2.0, "The Text"),
//                 "font", "font", new Font(fontName, 22));
//     }
// */

//     @Test public void testBoundPropertySync_BoundsType() throws Exception {
//         NodeTest.assertObjectPropertySynced(
//                 new Text(1.0, 2.0, "The Text"),
//                 "boundsType", "textBoundsType", TextBoundsType.VISUAL);
//     }

    
//     @Test public void testBoundPropertySync_WrappingWidth() throws Exception {
//         NodeTest.assertDoublePropertySynced(
//                 new Text(1.0, 2.0, "The Text"),
//                 "wrappingWidth", "wrappingWidth", 50);
//     }
    

//     @Test public void testBoundPropertySync_Underline() throws Exception {
//         NodeTest.assertBooleanPropertySynced(
//                 new Text(1.0, 2.0, "The Text"),
//                 "underline", "underline", true);
//     }

//     @Test public void testBoundPropertySync_Strikethrough() throws Exception {
//         NodeTest.assertBooleanPropertySynced(
//                 new Text(1.0, 2.0, "The Text"),
//                 "strikethrough", "strikethrough", true);
//     }

//     @Test public void testBoundPropertySync_TextAlignment() throws Exception {
//         NodeTest.assertObjectPropertySynced(
//                 new Text(1.0, 2.0, "The Text"),
//                 "textAlignment", "textAlignment", TextAlignment.RIGHT);
//     }

}
