/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.javafx.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.robot.Robot;
import javafx.scene.text.CaretInfo;
import javafx.scene.text.Font;
import javafx.scene.text.LayoutInfo;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.text.TextLineInfo;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.util.Util;

/*
 * Tests new LayoutInfo API in the TextFlow.
 */
public class TextFlow_TextLayout_Test {
    private static final int WIDTH = 500;
    private static final int HEIGHT = 500;
    private static final int FONT_SIZE = 24;
    private static final double LINE_SPACING = 33;
    private static final double EPS = 0.1;
    private static final CountDownLatch startupLatch = new CountDownLatch(1);
    private static Robot robot;
    private static TextFlow textFlow;
    private static volatile Stage stage;
    private static volatile Scene scene;

    // testing caret info
    @Test
    public void testCaretInfo() {
        setText("__________\n", "______\n", "_\n");
        waitForIdle();
        LayoutInfo la = textFlow.getLayoutInfo();

        CaretInfo ci = la.caretInfoAt(0, true);

        // caret is one line
        assertEquals(1, ci.getSegmentCount());

        // basic size checks
        Rectangle2D r = ci.getSegmentAt(0);
        assertTrue(r.getMinX() >= 0);
        assertTrue(r.getMinY() >= 0);
        assertTrue(r.getWidth() < 0.1);
        assertTrue(r.getHeight() >= FONT_SIZE);

        // caret at the end
        int len = textLength();
        ci = la.caretInfoAt(len - 1, false);
        assertEquals(1, ci.getSegmentCount());
        Rectangle2D r2 = ci.getSegmentAt(0);

        // basic checks
        assertTrue(r2.getMinY() > r.getMinY());
        assertTrue(r2.getMinX() > r.getMinX());
    }

    // testing layout bounds
    @Test
    public void testBounds() {
        setText("__\n", "____\n", "______");
        apply((f) -> {
            f.setLineSpacing(LINE_SPACING);
        });
        waitForIdle();
        LayoutInfo la = textFlow.getLayoutInfo();

        Rectangle2D r0 = la.getLogicalBounds(false);
        Rectangle2D r1 = la.getLogicalBounds(true);

        // non-empty
        assertTrue((r0.getWidth() > 0) && (r0.getHeight() > 0));
        assertTrue((r1.getWidth() > 0) && (r1.getHeight() > 0));

        // same width
        assertEquals(r0.getWidth(), r1.getWidth(), EPS);

        // one is taller by one line spacing
        assertEquals(r0.getHeight() + LINE_SPACING, r1.getHeight(), EPS);
    }

    // testing text lines
    @Test
    public void testTextLines() {
        setText("__\n", "____\n", "______");
        waitForIdle();
        LayoutInfo la = textFlow.getLayoutInfo();

        // spacing = 0

        assertEquals(3, la.getTextLineCount());
        List<TextLineInfo> ls = la.getTextLines(false);
        assertNotNull(ls);
        assertEquals(3, ls.size());
        TextLineInfo l0 = la.getTextLine(0, false);
        TextLineInfo l1 = la.getTextLine(1, false);
        TextLineInfo l2 = la.getTextLine(2, false);
        assertEquals(l0, ls.get(0));
        assertEquals(l1, ls.get(1));
        assertEquals(l2, ls.get(2));

        // position check
        assertEquals(0, l0.start());
        assertEquals(3, l0.end());
        assertEquals(3, l1.start());
        assertEquals(8, l1.end());
        assertEquals(8, l2.start());
        assertEquals(14, l2.end());

        // geometry check
        assertEquals(0, l0.bounds().getMinX(), EPS);
        assertEquals(0, l0.bounds().getMinY(), EPS);
        assertTrue(l0.bounds().getWidth() < l1.bounds().getWidth());
        assertTrue(l1.bounds().getWidth() < l2.bounds().getWidth());
        assertTrue(l0.bounds().getMinY() < l1.bounds().getMinY());
        assertTrue(l1.bounds().getMinY() < l2.bounds().getMinY());

        // line spacing
        apply((f) -> {
            f.setLineSpacing(LINE_SPACING);
        });
        waitForIdle();

        List<TextLineInfo> LS = la.getTextLines(true);
        assertNotNull(LS);
        TextLineInfo L0 = la.getTextLine(0, true);
        TextLineInfo L1 = la.getTextLine(1, true);
        TextLineInfo L2 = la.getTextLine(2, true);
        assertEquals(L0, LS.get(0));
        assertEquals(L1, LS.get(1));
        assertEquals(L2, LS.get(2));

        // widths should be same
        assertEquals(l0.bounds().getWidth(), L0.bounds().getWidth());
        assertEquals(l1.bounds().getWidth(), L1.bounds().getWidth());
        assertEquals(l2.bounds().getWidth(), L2.bounds().getWidth());

        // heights should differ
        assertEquals(l0.bounds().getHeight() + LINE_SPACING, L0.bounds().getHeight(), EPS);
        assertEquals(l1.bounds().getHeight() + LINE_SPACING, L1.bounds().getHeight(), EPS);
        assertEquals(l2.bounds().getHeight() + LINE_SPACING, L2.bounds().getHeight(), EPS);
    }

    // testing selection shape
    @Test
    public void testSelection() {
        setText("__\n", "____\n", "______");
        waitForIdle();
        LayoutInfo la = textFlow.getLayoutInfo();

        // spacing = 0

        int len = textLength();
        List<Rectangle2D> ss = la.getSelectionGeometry(0, len, false);
        assertEquals(3, ss.size());
        Rectangle2D s0 = ss.get(0);
        Rectangle2D s1 = ss.get(1);
        Rectangle2D s2 = ss.get(2);

        // geometry check
        assertEquals(0, s0.getMinX(), EPS);
        assertEquals(0, s0.getMinY(), EPS);

        assertTrue(s0.getWidth() < s1.getWidth());
        assertTrue(s1.getWidth() < s2.getWidth());

        assertTrue(s0.getMinY() < s1.getMinY());
        assertTrue(s1.getMinY() < s2.getMinY());

        assertTrue(s0.getHeight() >= FONT_SIZE);
        assertEquals(s0.getHeight(), s1.getHeight(), EPS);
        assertEquals(s1.getHeight(), s2.getHeight(), EPS);

        // line spacing
        apply((f) -> {
            f.setLineSpacing(LINE_SPACING);
        });
        waitForIdle();

        List<Rectangle2D> SS = la.getSelectionGeometry(0, len, true);
        assertEquals(3, ss.size());
        Rectangle2D S0 = SS.get(0);
        Rectangle2D S1 = SS.get(1);
        Rectangle2D S2 = SS.get(2);

        // geometry check
        assertEquals(0, S0.getMinX(), EPS);
        assertEquals(0, S0.getMinY(), EPS);

        assertTrue(S0.getWidth() < S1.getWidth());
        assertTrue(S1.getWidth() < S2.getWidth());

        assertTrue(S0.getMinY() < S1.getMinY());
        assertTrue(S1.getMinY() < S2.getMinY());

        assertTrue(S0.getHeight() >= FONT_SIZE);
        assertEquals(S0.getHeight(), S1.getHeight(), EPS);
        assertEquals(S1.getHeight(), S2.getHeight(), EPS);

        // includes line space
        assertEquals(s0.getHeight() + LINE_SPACING, S0.getHeight(), EPS);
    }

    // testing strike-through shape
    @Test
    public void testStrikeThrough() {
        setText("__\n", "____\n", "______");
        waitForIdle();
        LayoutInfo la = textFlow.getLayoutInfo();

        int len = textLength();
        List<Rectangle2D> ss = la.getStrikeThroughGeometry(0, len);
        assertEquals(3, ss.size());
        Rectangle2D s0 = ss.get(0);
        Rectangle2D s1 = ss.get(1);
        Rectangle2D s2 = ss.get(2);

        // geometry check
        assertEquals(0, s0.getMinX(), EPS);

        assertTrue(s0.getWidth() < s1.getWidth());
        assertTrue(s1.getWidth() < s2.getWidth());

        assertTrue(s0.getMinY() < s1.getMinY());
        assertTrue(s1.getMinY() < s2.getMinY());

        assertTrue(s0.getHeight() > 0);
        assertEquals(s0.getHeight(), s1.getHeight(), EPS);
        assertEquals(s1.getHeight(), s2.getHeight(), EPS);
    }

    // testing underline shape
    @Test
    public void testUnderline() {
        setText("__\n", "____\n", "______");
        waitForIdle();
        LayoutInfo la = textFlow.getLayoutInfo();

        int len = textLength();
        List<Rectangle2D> ss = la.getUnderlineGeometry(0, len);
        assertEquals(3, ss.size());
        Rectangle2D s0 = ss.get(0);
        Rectangle2D s1 = ss.get(1);
        Rectangle2D s2 = ss.get(2);

        // geometry check
        assertEquals(0, s0.getMinX(), EPS);

        assertTrue(s0.getWidth() < s1.getWidth());
        assertTrue(s1.getWidth() < s2.getWidth());

        assertTrue(s0.getMinY() < s1.getMinY());
        assertTrue(s1.getMinY() < s2.getMinY());

        assertTrue(s0.getHeight() > 0);
        assertEquals(s0.getHeight(), s1.getHeight(), EPS);
        assertEquals(s1.getHeight(), s2.getHeight(), EPS);
    }

    // testing IOOBE exceptions
    @Test
    public void testIOOBExceptions() {
        setText("__________\n", "______\n", "_\n");
        waitForIdle();
        LayoutInfo la = textFlow.getLayoutInfo();
        assertThrows(IndexOutOfBoundsException.class, () -> la.getTextLine(-1, true));
        assertThrows(IndexOutOfBoundsException.class, () -> la.getTextLine(la.getTextLineCount(), true));

        CaretInfo ci = la.caretInfoAt(0, true);
        assertThrows(IndexOutOfBoundsException.class, () -> ci.getSegmentAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> ci.getSegmentAt(ci.getSegmentCount()));
    }

    private void setText(String... segments) {
        Util.runAndWait(() -> {
            textFlow.getChildren().clear();
            for (String s : segments) {
                Text t = new Text(s);
                t.setFont(new Font(FONT_SIZE));
                textFlow.getChildren().add(t);
            }
        });
    }

    private void waitForIdle() {
        Util.waitForIdle(scene);
    }

    public static int textLength() {
        int len = 0;
        for (Node n : textFlow.getChildrenUnmodifiable()) {
            if (n instanceof Text t) {
                len += t.getText().length();
            } else {
                // treat non-Text nodes as having 1 character
                len++;
            }
        }
        return len;
    }

    private void apply(Consumer<TextFlow> c) {
        Util.runAndWait(() -> {
            c.accept(textFlow);
        });
    }

    @BeforeEach
    public void beforeEach() {
        apply((t) -> {
            t.setLineSpacing(0);
        });
    }

    @BeforeAll
    public static void beforeAll() {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    public static void afterAll() {
        Util.shutdown();
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage st) {
            robot = new Robot();
            stage = st;

            textFlow = new TextFlow();
            scene = new Scene(textFlow, WIDTH, HEIGHT);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setOnShown(event -> Platform.runLater(startupLatch::countDown));
            stage.setAlwaysOnTop(true);
            stage.show();
        }
    }
}
