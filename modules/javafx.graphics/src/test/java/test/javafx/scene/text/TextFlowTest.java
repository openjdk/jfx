/*
 * Copyright (c) 2019, 2025, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.pgstub.StubToolkit;

public class TextFlowTest {

    private static final double EPSILON = 0.00001;

    @Test
    public void testTabSize() {
        Toolkit tk = Toolkit.getToolkit();

        assertTrue(tk instanceof StubToolkit);  // Ensure it's StubToolkit

        VBox root = new VBox();
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setWidth(500);
        stage.setHeight(200);

        try {
            Text text1 = new Text("\tfirst");
            Text text2 = new Text("\tsecond");
            TextFlow textFlow = new TextFlow(text1, text2);
            textFlow.setPrefWidth(TextFlow.USE_COMPUTED_SIZE);
            textFlow.setMaxWidth(TextFlow.USE_PREF_SIZE);
            root.getChildren().addAll(textFlow);
            stage.show();
            tk.firePulse();
            assertEquals(8, textFlow.getTabSize());
            // initial width with default 8-space tab
            double widthT8 = textFlow.getBoundsInLocal().getWidth();
            text1.setTabSize(4);
            text2.setTabSize(3);
            // StubToolkit is reusing a StubTextLayout ?
            tk.getTextLayoutFactory().disposeLayout(tk.getTextLayoutFactory().getLayout());
            // Tab size of contained text nodes should not have any effect.
            tk.firePulse();
            assertEquals(widthT8, textFlow.getBoundsInLocal().getWidth(), 0.0);

            textFlow.setTabSize(1);
            tk.firePulse();
            // width with tab at 1 spaces
            double widthT1 = textFlow.getBoundsInLocal().getWidth();
            assertTrue(widthT1 < widthT8);

            textFlow.setTabSize(20);
            tk.firePulse();
            double widthT20 = textFlow.getBoundsInLocal().getWidth();
            assertTrue(widthT20 > widthT8);

            assertEquals(20, textFlow.getTabSize());
            assertEquals(20, textFlow.tabSizeProperty().get());

            textFlow.tabSizeProperty().set(10);
            tk.firePulse();
            double widthT10 = textFlow.getBoundsInLocal().getWidth();
            assertTrue(widthT10 > widthT8);
            assertTrue(widthT10 < widthT20);

            assertEquals(10, textFlow.getTabSize());
            assertEquals(10, textFlow.tabSizeProperty().get());

            // tab size of contained text nodes isn't modified by TextFlow
            assertEquals(4, text1.getTabSize());
            assertEquals(3, text2.getTabSize());

            // Test clamping
            textFlow.tabSizeProperty().set(0);
            assertEquals(0, textFlow.tabSizeProperty().get());
            assertEquals(0, textFlow.getTabSize());
            tk.firePulse();
            double widthT0Clamp = textFlow.getBoundsInLocal().getWidth();
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

        TextFlow f = new TextFlow(text("01234\n56789"));
        checkNear(f.caretShape(first, true), -1, -1, 2, 14);
        checkNear(f.caretShape(second, true), -1, 11, 2, 14);
        checkNear(f.getCaretShape(first, true), -1, -1, 2, 14);
        checkNear(f.getCaretShape(second, true), -1, 11, 2, 14);

        f.setPadding(new Insets(100));
        // legacy implementation accounts for no insets
        checkNear(f.caretShape(first, true), -1, -1, 2, 14);
        checkNear(f.caretShape(second, true), -1, 11, 2, 14);
        // new implementation accounts for insets
        checkNear(f.getCaretShape(first, true), 99, 99, 2, 14);
        checkNear(f.getCaretShape(second, true), 99, 111, 2, 14);

        f.setLineSpacing(50);
        // legacy implementation accounts neither for insets nor line spacing
        checkNear(f.caretShape(first, true), -1, -1, 2, 14);
        checkNear(f.caretShape(second, true), -1, 61, 2, 14);
        // new implementation accounts for insets and line spacing
        checkNear(f.getCaretShape(first, true), 99, 99, 2, 14);
        checkNear(f.getCaretShape(second, true), 99, 161, 2, 14);
    }

    @Test
    public void caretShapeNonText() {
        TextFlow f = new TextFlow(text("\t"));
        checkNear(f.caretShape(0, true), -1, -1, 2, 14);
        checkNear(f.caretShape(0, false), 95, -1, 2, 14);

        Region r = new Region();
        r.setMaxWidth(100);
        r.setMinWidth(100);
        f.getChildren().add(r);

        checkNear(f.caretShape(1, true), 95, -1, 2, 14);
        checkNear(f.caretShape(1, false), 195, -1, 2, 14);
    }

    @Test
    public void hitInfo() {
        double first = 0;
        double second = 12;
        double padding = 100;
        double lineSpacing = 50;

        TextFlow f = new TextFlow(text("01234\n56789"));
        checkNear(f.hitTest(new Point2D(0, first)), 0, true, 0);
        checkNear(f.hitTest(new Point2D(0, second)), 6, true, 6);
        checkNear(f.getHitInfo(new Point2D(0, first)), 0, true, 0);
        checkNear(f.getHitInfo(new Point2D(0, second)), 6, true, 6);

        f.setPadding(new Insets(padding));
        // legacy implementation accounts for no insets
        checkNear(f.hitTest(new Point2D(padding, padding + first)), 11, false, 12);
        checkNear(f.hitTest(new Point2D(padding, padding + second)), 11, false, 12);
        // new implementation accounts for insets
        checkNear(f.getHitInfo(new Point2D(padding, padding + first)), 0, true, 0);
        checkNear(f.getHitInfo(new Point2D(padding, padding + second)), 6, true, 6);

        f.setLineSpacing(lineSpacing);
        // legacy implementation accounts neither for insets nor line spacing
        checkNear(f.hitTest(new Point2D(padding, padding + first)), 10, false, 11);
        checkNear(f.hitTest(new Point2D(padding, padding + second)), 10, false, 11);
        // new implementation accounts for insets and line spacing
        checkNear(f.getHitInfo(new Point2D(padding, padding + first)), 0, true, 0);
        checkNear(f.getHitInfo(new Point2D(padding, padding + lineSpacing + second)), 6, true, 6);
    }

    @Test
    public void rangeShape() {
        TextFlow f = new TextFlow(text("01234\n56789"));
        checkNear(f.rangeShape(0, 10), -1, -1, 62, 26);
        checkNear(f.getRangeShape(0, 10, false), -1, -1, 62, 26);
        checkNear(f.getRangeShape(0, 10, true), -1, -1, 62, 26);

        f.setPadding(new Insets(100));
        // legacy implementation accounts for no insets
        checkNear(f.rangeShape(0, 10), -1, -1, 62, 26);
        // new implementation accounts for insets
        checkNear(f.getRangeShape(0, 10, false), 99, 99, 62, 26);
        checkNear(f.getRangeShape(0, 10, true), 99, 99, 62, 26);

        f.setLineSpacing(50);
        // legacy implementation accounts neither for insets nor line spacing
        checkNear(f.rangeShape(0, 10), -1, -1, 62, 76);
        // new implementation accounts for insets and line spacing
        checkNear(f.getRangeShape(0, 10, false), 99, 99, 62, 76);
        checkNear(f.getRangeShape(0, 10, true), 99, 99, 62, 126);
    }

    @Test
    public void strikeThroughShape() {
        TextFlow f = new TextFlow(text("01234567890"));
        checkNear(f.getStrikeThroughShape(0, 10), -1, 8.6, 122, 3);

        f.setPadding(new Insets(100));
        checkNear(f.getStrikeThroughShape(0, 10), 99, 108.6, 122, 3);
    }

    @Test
    public void underlineShape() {
        TextFlow f = new TextFlow(text("01234567890"));
        checkNear(f.underlineShape(0, 10), -1, 9.6, 122, 3);
        checkNear(f.getUnderlineShape(0, 10), -1, 9.6, 122, 3);

        f.setPadding(new Insets(100));
        checkNear(f.underlineShape(0, 10), -1, 9.6, 122, 3);
        checkNear(f.getUnderlineShape(0, 10), 99, 109.6, 122, 3);
    }
}
