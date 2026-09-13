/*
 * Copyright (c) 2010, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.pgstub.StubToolkit;

public class TextTest {

    private static final double EPSILON = 0.00001;

    @Test
    public void testCtors() {
        Text t1 = new Text();
        assertEquals("", t1.getText());
        Text t2 = new Text("test content");
        assertEquals("test content", t2.getText());
        Text t3 = new Text(10, 20, "2");
        assertEquals(10f, t3.getX(), 0);
        assertEquals(20f, t3.getY(), 0);
        assertEquals("2", t3.getText());
    }

    @Test
    public void testSettingNullText() {
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

    @Test
    public void testDefaultTextNotNull() {
        Text t = new Text();
        assertEquals("", t.getText());
        assertEquals("", t.textProperty().get());
    }

    @Test
    public void testStoreFont() {
        Text t = new Text();
        Font f = new Font(44);
        assertEquals(Font.getDefault(), t.getFont());
        t.setFont(f);
        assertTrue(t.getBaselineOffset() > (f.getSize() / 2.0));
    }

 // Commented out as StubFontLoader only knows about Amble and its
 // also not a given that the Font.getDefault() matches the default font
 // on a Text node anyway, as CSS defaults are applied to the Text node.
/*
    @Test
    public void testPropertyPropagation_font() throws Exception {
        final Text node = new Text();
        NodeTest.testObjectPropertyPropagation(node, "font", Font.getDefault(), new Font(44));
    }
*/

//     @Test
//     public void testPropertyPropagation_textOrigin() throws Exception {
//         final Text node = new Text();
//         NodeTest.testObjectPropertyPropagation(node, "textOrigin", "textOrigin",
//                 VPos.BASELINE, VPos.TOP, new NodeTest.ObjectValueConvertor() {
//                     @Override
//                     public Object toSg(Object pgValue) {
//                         return VPos.values()[((Number)pgValue).intValue()];
//                     }
//                 });
//     }

//     @Test
//     public void testPropertyPropagation_boundsType() throws Exception {
//         final Text node = new Text();
//         NodeTest.testObjectPropertyPropagation(node, "boundsType", "textBoundsType",
//                 TextBoundsType.LOGICAL, TextBoundsType.VISUAL, new NodeTest.ObjectValueConvertor() {
//                     @Override
//                     public Object toSg(Object pgValue) {
//                         return TextBoundsType.values()[((Number)pgValue).intValue()];
//                     }
//                 });
//     }

//     @Test
//     public void testPropertyPropagation_textAlignment() throws Exception {
//         final Text node = new Text();
//         NodeTest.testObjectPropertyPropagation(node, "textAlignment", "textAlignment",
//                 TextAlignment.LEFT, TextAlignment.CENTER, new NodeTest.ObjectValueConvertor() {
//                     @Override
//                     public Object toSg(Object pgValue) {
//                         return TextAlignment.values()[(((Number)pgValue).intValue())];
//                     }
//                 });
//     }

//     @Test
//     public void testPropertyPropagation_visible() throws Exception {
//         final Text node = new Text();
//         NodeTest.testBooleanPropertyPropagation(node, "visible", false, true);
//     }

//     @Test
//     public void testPropertyPropagation_text() throws Exception {
//         final Text node = new Text();
//         NodeTest.testObjectPropertyPropagation(node, "text", "text", "Hello", "World");
//     }

//     @Test
//     public void testPropertyPropagation_strikethrough() throws Exception {
//         final Text node = new Text();
//         NodeTest.testBooleanPropertyPropagation(node, "strikethrough", false, true);
//     }

//     @Test
//     public void testPropertyPropagation_underline() throws Exception {
//         final Text node = new Text();
//         NodeTest.testBooleanPropertyPropagation(node, "underline", false, true);
//     }

//     @Test
//     public void testPropertyPropagation_x() throws Exception {
//         final Text node = new Text();
//         NodeTest.testDoublePropertyPropagation(node, "x", 100, 200);
//     }

//     @Test
//     public void testPropertyPropagation_y() throws Exception {
//         final Text node = new Text();
//         NodeTest.testDoublePropertyPropagation(node, "y", 100, 200);
//     }

//     @Test
//     public void testPropertyPropagation_wrappingWidth() throws Exception {
//         final Text node = new Text();
//         NodeTest.testDoublePropertyPropagation(node, "wrappingWidth", 100, 200);
//     }

//     @Test
//     public void testBoundPropertySync_X() throws Exception {
//         NodeTest.assertDoublePropertySynced(
//                 new Text(1.0, 2.0, "The Text"),
//                 "x", "x", 10.0);
//     }

//     @Test
//     public void testBoundPropertySync_Y() throws Exception {
//         NodeTest.assertDoublePropertySynced(
//                 new Text(1.0, 2.0, "The Text"),
//                 "y", "y", 20.0);
//     }

//     @Test
//     public void testBoundPropertySync_Text() throws Exception {
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
//     @Test
//     public void testBoundPropertySync_Font() throws Exception {
//         List<String> fontNames = Font.getFontNames();
//         String fontName = fontNames.get(fontNames.size() - 1);
//         NodeTest.assertObjectPropertySynced(
//                 new Text(1.0, 2.0, "The Text"),
//                 "font", "font", new Font(fontName, 22));
//     }
// */

//     @Test
//     public void testBoundPropertySync_BoundsType() throws Exception {
//         NodeTest.assertObjectPropertySynced(
//                 new Text(1.0, 2.0, "The Text"),
//                 "boundsType", "textBoundsType", TextBoundsType.VISUAL);
//     }


//     @Test
//     public void testBoundPropertySync_WrappingWidth() throws Exception {
//         NodeTest.assertDoublePropertySynced(
//                 new Text(1.0, 2.0, "The Text"),
//                 "wrappingWidth", "wrappingWidth", 50);
//     }


//     @Test
//     public void testBoundPropertySync_Underline() throws Exception {
//         NodeTest.assertBooleanPropertySynced(
//                 new Text(1.0, 2.0, "The Text"),
//                 "underline", "underline", true);
//     }

//     @Test
//     public void testBoundPropertySync_Strikethrough() throws Exception {
//         NodeTest.assertBooleanPropertySynced(
//                 new Text(1.0, 2.0, "The Text"),
//                 "strikethrough", "strikethrough", true);
//     }

//     @Test
//     public void testBoundPropertySync_TextAlignment() throws Exception {
//         NodeTest.assertObjectPropertySynced(
//                 new Text(1.0, 2.0, "The Text"),
//                 "textAlignment", "textAlignment", TextAlignment.RIGHT);
//     }

    @Test
    public void toStringShouldReturnNonEmptyString() {
        String s = new Text().toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    // Test for JDK-8130738
    @Test
    public void testTabSize() {
        // Test is unstable until JDK-8236728 is fixed
        assumeTrue(Boolean.getBoolean("unstable.test"));

        Toolkit tk = Toolkit.getToolkit();

        assertTrue(tk instanceof StubToolkit);  // Ensure it's StubToolkit

        HBox root = new HBox();
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setWidth(300);
        stage.setHeight(200);

        try {
            Text text = new Text("\tHello");
            root.getChildren().addAll(text);
            stage.show();
            tk.firePulse();
            assertEquals(8, text.getTabSize());
            // initial width with default 8-space tab
            double widthT8 = text.getBoundsInLocal().getWidth();
            text.setTabSize(1);
            tk.firePulse();
            // width with tab at 1 spaces
            double widthT1 = text.getBoundsInLocal().getWidth();
            // approximate width of a single space
            double widthSpace = (widthT8 - widthT1) / 7;
            assertTrue(widthSpace > 0);
            text.setTabSize(4);
            tk.firePulse();
            // width with tab at 4 spaces
            double widthT4 = text.getBoundsInLocal().getWidth();
            double expected = widthT8 - 4 * widthSpace;
            // should be approximately 4 space-widths shorter
            assertEquals(expected, widthT4, 0.5);
            assertEquals(4, text.getTabSize());
            assertEquals(4, text.tabSizeProperty().get());

            text.tabSizeProperty().set(5);
            assertEquals(5, text.tabSizeProperty().get());
            assertEquals(5, text.getTabSize());
            tk.firePulse();
            double widthT5 = text.getBoundsInLocal().getWidth();
            expected = widthT8 - 3 * widthSpace;
            assertEquals(expected, widthT5, 0.5);

            // Test clamping
            text.tabSizeProperty().set(0);
            assertEquals(0, text.tabSizeProperty().get());
            assertEquals(0, text.getTabSize());
            tk.firePulse();
            double widthT0Clamp = text.getBoundsInLocal().getWidth();
            // values < 1 are treated as 1
            assertEquals(widthT1, widthT0Clamp, 0.5);
        } finally {
            stage.hide();
        }
    }

    private static Text text(String text) {
        Text t = new Text(text);
        t.setFont(new Font("System", 12.0));
        return t;
    }

    // new StubTextLayout generates prodictable text shapes
    private static void checkNear(PathElement[] em, double ex, double ey, double ew, double eh) {
        Bounds b = new Path(em).getBoundsInLocal();
        double x = b.getMinX();
        double y = b.getMinY();
        double w = b.getWidth();
        double h = b.getHeight();
        assertEquals(ex, x, EPSILON);
        assertEquals(ey, y, EPSILON);
        assertEquals(ew, w, EPSILON);
        assertEquals(eh, h, EPSILON);
    }

    private void checkNear(HitInfo h, int expectedCharIndex, boolean expectedLeading, int expectedInsert) {
        assertEquals(expectedCharIndex, h.getCharIndex());
        assertEquals(expectedLeading, h.isLeading());
        assertEquals(expectedInsert, h.getInsertionIndex());
    }

    @Test
    public void caretShape() {
        int first = 0;
        int second = 6;

        Text t = text("01234\n56789");

        // top
        t.setTextOrigin(VPos.TOP);
        t.setLineSpacing(0);
        checkNear(t.caretShape(first, true), -1, -1, 2, 14);
        checkNear(t.caretShape(second, true), -1, 11, 2, 14);
        t.setLineSpacing(50);
        checkNear(t.caretShape(first, true), -1, -1, 2, 14);
        checkNear(t.caretShape(second, true), -1, 61, 2, 14);

        // baseline
        t.setTextOrigin(VPos.BASELINE);
        t.setLineSpacing(0);
        checkNear(t.caretShape(first, true), -1, -10.6, 2, 14);
        checkNear(t.caretShape(second, true), -1, 1.4, 2, 14);
        t.setLineSpacing(50);
        checkNear(t.caretShape(first, true), -1, -10.6, 2, 14);
        checkNear(t.caretShape(second, true), -1, 51.4, 2, 14);

        // center
        t.setTextOrigin(VPos.CENTER);
        t.setLineSpacing(0);
        checkNear(t.caretShape(first, true), -1, -13, 2, 14);
        checkNear(t.caretShape(second, true), -1, -1, 2, 14);
        t.setLineSpacing(50);
        checkNear(t.caretShape(first, true), -1, -38, 2, 14);
        checkNear(t.caretShape(second, true), -1, 24, 2, 14);

        // bottom
        t.setTextOrigin(VPos.BOTTOM);
        t.setLineSpacing(0);
        checkNear(t.caretShape(first, true), -1, -25, 2, 14);
        checkNear(t.caretShape(second, true), -1, -13, 2, 14);
        t.setLineSpacing(50);
        checkNear(t.caretShape(first, true), -1, -75, 2, 14);
        checkNear(t.caretShape(second, true), -1, -13, 2, 14);
    }

    @Test
    public void hitInfo() {
        double first = 0;
        double second = 12;
        double baseline = -9;
        double height = -24;
        double lineSpacing = 50;

        Text t = text("01234\n56789");

        // top
        t.setTextOrigin(VPos.TOP);
        t.setLineSpacing(0);
        checkNear(t.hitTest(new Point2D(0, first)), 0, true, 0);
        checkNear(t.hitTest(new Point2D(0, second)), 6, true, 6);
        t.setLineSpacing(lineSpacing);
        checkNear(t.hitTest(new Point2D(0, first)), 0, true, 0);
        checkNear(t.hitTest(new Point2D(0, lineSpacing + second)), 6, true, 6);

        // baseline
        t.setTextOrigin(VPos.BASELINE);
        t.setLineSpacing(0);
        checkNear(t.hitTest(new Point2D(0, baseline + first)), 0, true, 0);
        checkNear(t.hitTest(new Point2D(0, baseline + second)), 6, true, 6);
        t.setLineSpacing(lineSpacing);
        checkNear(t.hitTest(new Point2D(0, baseline + first)), 0, true, 0);
        checkNear(t.hitTest(new Point2D(0, baseline + lineSpacing + second)), 6, true, 6);

        // center
        t.setTextOrigin(VPos.CENTER);
        t.setLineSpacing(0);
        checkNear(t.hitTest(new Point2D(0, height / 2 + first)), 0, true, 0);
        checkNear(t.hitTest(new Point2D(0, height / 2 + second)), 6, true, 6);
        t.setLineSpacing(lineSpacing);
        checkNear(t.hitTest(new Point2D(0, height / 2 + first)), 0, true, 0);
        checkNear(t.hitTest(new Point2D(0, height / 2 + lineSpacing + second)), 6, true, 6);

        // bottom
        t.setTextOrigin(VPos.BOTTOM);
        t.setLineSpacing(0);
        checkNear(t.hitTest(new Point2D(0, height + first)), 0, true, 0);
        checkNear(t.hitTest(new Point2D(0, height + second)), 6, true, 6);
        t.setLineSpacing(lineSpacing);
        checkNear(t.hitTest(new Point2D(0, height + first)), 0, true, 0);
        checkNear(t.hitTest(new Point2D(0, height + lineSpacing + second)), 6, true, 6);
    }

    @Test
    public void rangeShape() {
        Text t = text("01234\n56789");
        double lineSpacing = 50;

        // top
        t.setTextOrigin(VPos.TOP);
        t.setLineSpacing(0);
        checkNear(t.rangeShape(0, 10), -1, -1, 62, 26);
        checkNear(t.getRangeShape(0, 10, false), -1, -1, 62, 26);
        checkNear(t.getRangeShape(0, 10, true), -1, -1, 62, 26);
        t.setLineSpacing(lineSpacing);
        checkNear(t.rangeShape(0, 10), -1, -1, 62, 76);
        checkNear(t.getRangeShape(0, 10, false), -1, -1, 62, 76);
        checkNear(t.getRangeShape(0, 10, true), -1, -1, 62, 126);

        // baseline
        t.setTextOrigin(VPos.BASELINE);
        t.setLineSpacing(0);
        checkNear(t.rangeShape(0, 10), -1, -10.6, 62, 26);
        checkNear(t.getRangeShape(0, 10, false), -1, -10.6, 62, 26);
        checkNear(t.getRangeShape(0, 10, true), -1, -10.6, 62, 26);
        t.setLineSpacing(lineSpacing);
        checkNear(t.rangeShape(0, 10), -1, -10.6, 62, 76);
        checkNear(t.getRangeShape(0, 10, false), -1, -10.6, 62, 76);
        checkNear(t.getRangeShape(0, 10, true), -1, -10.6, 62, 126);

        // center
        t.setTextOrigin(VPos.CENTER);
        t.setLineSpacing(0);
        checkNear(t.rangeShape(0, 10), -1, -13, 62, 26);
        checkNear(t.getRangeShape(0, 10, false), -1, -13, 62, 26);
        checkNear(t.getRangeShape(0, 10, true), -1, -13, 62, 26);
        t.setLineSpacing(lineSpacing);
        checkNear(t.rangeShape(0, 10), -1, -38, 62, 76);
        checkNear(t.getRangeShape(0, 10, false), -1, -38, 62, 76);
        checkNear(t.getRangeShape(0, 10, true), -1, -38, 62, 126);

        // bottom
        t.setTextOrigin(VPos.BOTTOM);
        t.setLineSpacing(0);
        checkNear(t.rangeShape(0, 10), -1, -25, 62, 26);
        checkNear(t.getRangeShape(0, 10, false), -1, -25, 62, 26);
        checkNear(t.getRangeShape(0, 10, true), -1, -25, 62, 26);
        t.setLineSpacing(lineSpacing);
        checkNear(t.rangeShape(0, 10), -1, -75, 62, 76);
        checkNear(t.getRangeShape(0, 10, false), -1, -75, 62, 76);
        checkNear(t.getRangeShape(0, 10, true), -1, -75, 62, 126);
    }

    @Test
    public void strikeThroughShape() {
        Text t = text("01234\n56789");
        double lineSpacing = 50;

        // top
        t.setTextOrigin(VPos.TOP);
        t.setLineSpacing(0);
        checkNear(t.getStrikeThroughShape(0, 10), -1, 8.6, 62, 15);
        t.setLineSpacing(lineSpacing);
        checkNear(t.getStrikeThroughShape(0, 10), -1, 8.6, 62, 65);

        // baseline
        t.setTextOrigin(VPos.BASELINE);
        t.setLineSpacing(0);
        checkNear(t.getStrikeThroughShape(0, 10), -1, -1, 62, 15);
        t.setLineSpacing(lineSpacing);
        checkNear(t.getStrikeThroughShape(0, 10), -1, -1, 62, 65);

        // center
        t.setTextOrigin(VPos.CENTER);
        t.setLineSpacing(0);
        checkNear(t.getStrikeThroughShape(0, 10), -1, -3.4, 62, 15);
        t.setLineSpacing(lineSpacing);
        checkNear(t.getStrikeThroughShape(0, 10), -1, -28.4, 62, 65);

        // bottom
        t.setTextOrigin(VPos.BOTTOM);
        t.setLineSpacing(0);
        checkNear(t.getStrikeThroughShape(0, 10), -1, -15.4, 62, 15);
        t.setLineSpacing(lineSpacing);
        checkNear(t.getStrikeThroughShape(0, 10), -1, -65.4, 62, 65);
    }

    @Test
    public void underlineShape() {
        Text t = text("01234\n56789");
        double lineSpacing = 50;

        // top
        t.setTextOrigin(VPos.TOP);
        t.setLineSpacing(0);
        checkNear(t.underlineShape(0, 10), -1, 9.6, 62, 15);
        t.setLineSpacing(lineSpacing);
        checkNear(t.underlineShape(0, 10), -1, 9.6, 62, 65);

        // baseline
        t.setTextOrigin(VPos.BASELINE);
        t.setLineSpacing(0);
        checkNear(t.underlineShape(0, 10), -1, 0, 62, 15);
        t.setLineSpacing(lineSpacing);
        checkNear(t.underlineShape(0, 10), -1, 0, 62, 65);

        // center
        t.setTextOrigin(VPos.CENTER);
        t.setLineSpacing(0);
        checkNear(t.underlineShape(0, 10), -1, -2.4, 62, 15);
        t.setLineSpacing(lineSpacing);
        checkNear(t.underlineShape(0, 10), -1, -27.4, 62, 65);

        // bottom
        t.setTextOrigin(VPos.BOTTOM);
        t.setLineSpacing(0);
        checkNear(t.underlineShape(0, 10), -1, -14.4, 62, 15);
        t.setLineSpacing(lineSpacing);
        checkNear(t.underlineShape(0, 10), -1, -64.4, 62, 65);
    }
}
