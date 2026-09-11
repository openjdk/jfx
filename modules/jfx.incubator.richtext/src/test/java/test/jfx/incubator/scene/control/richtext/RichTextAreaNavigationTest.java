/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
package test.jfx.incubator.scene.control.richtext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.jfx.incubator.scene.control.richtext.CaretInfo;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.RichTextModel;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.skin.RichTextAreaSkin;
import test.jfx.incubator.scene.control.richtext.support.RTUtil;
import test.jfx.incubator.scene.util.StageLoader;
import test.jfx.incubator.scene.util.TUtil;

/**
 * Tests RichTextArea navigation logic using StubTextLayout.
 */
public class RichTextAreaNavigationTest {

    private RichTextArea control;
    private StageLoader stageLoader;

    @BeforeEach
    public void beforeEach() {
        TUtil.setUncaughtExceptionHandler();
        control = new RichTextArea();
        control.setSkin(new RichTextAreaSkin(control));
        stageLoader = new StageLoader(control);
    }

    @AfterEach
    public void afterEach() {
        if (stageLoader != null) {
            stageLoader.dispose();
            stageLoader = null;
        }
        TUtil.removeUncaughtExceptionHandler();
    }

    // test the handling of the paragraph spacing JDK-8390913
    @Test
    public void testDown() {
        RichTextModel m = new RichTextModel();
        StyleAttributeMap a = StyleAttributeMap.builder()
            .setFontSize(20.0)
            .setBold(true)
            .setSpaceAbove(12.0)
            .setSpaceBelow(8.0)
            .build();
        control.setModel(m);
        control.appendText("This is the heading\nThis is some text.");
        control.applyStyle(TextPos.ofLeading(0, 0), control.getParagraphEnd(0), a);
        control.select(TextPos.ofLeading(0, 2));
        TextPos p1 = control.getCaretPosition();
        CaretInfo ci1 = RichTestUtil.getCaretInfo(control, p1);
        assertNotNull(ci1);

        control.moveDown();
        TextPos p2 = control.getCaretPosition();
        CaretInfo ci2 = RichTestUtil.getCaretInfo(control, p2);
        assertNotNull(ci2);

        assertEquals(1, p2.index());
        assertTrue(ci2.getMaxY() > ci1.getMaxY());
    }

    private record Params(
        double width,
        double startX,
        boolean wrap,
        double spaceAbove,
        double lineSpacing,
        double spaceBelow
    ) { }

    private static List<Params> parameters() {
        boolean[] booleans = { true, false };

        ArrayList<Params> params = new ArrayList<>();
        for (double width : new double[] { 100.0, 500.0 }) {
            for (double startX : new double[] { 0.0, 0.3, 1.0 }) {
                for (boolean wrap : booleans) {
                    for (double spaceAbove : new double[] { 0.0, 11.1 }) {
                        for (double lineSpacing : new double[] { 0.0, 12.2 }) {
                            for (double spaceBelow : new double[] { 0.0, 13.3 }) {
                                params.add(new Params(
                                    width,
                                    width * startX,
                                    wrap,
                                    spaceAbove,
                                    lineSpacing,
                                    spaceBelow));
                            }
                        }
                    }
                }
            }
        }
        return params;
    }

    private static RichTextModel createModel(Params p) {
        Random r = new Random();
        long seed = r.nextLong();
        r = new Random(seed);
        IO.println("seed=" + seed);

        int lines = 128;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines; i++) {
            if (i > 0) {
                sb.append("\n");
            }

            if (r.nextDouble() > 0.3) {
                int wordCount = r.nextInt(8);
                if (r.nextDouble() > 0.6) {
                    wordCount *= 3;
                }
                wordCount++;
                for (int j = 0; j < wordCount; j++) {
                    if (j > 0) {
                        if (r.nextDouble() > 0.1) {
                            sb.append(' ');
                        } else {
                            sb.append('\t');
                        }
                    }
                    int len = 1 + r.nextInt(10);
                    for (int w = 0; w < len; w++) {
                        sb.append("w");
                    }
                }
            }
        }
        String text = sb.toString();
        RichTextModel m = new RichTextModel();
        m.replace(null, TextPos.ZERO, TextPos.ZERO, text);
        StyleAttributeMap.Builder b = StyleAttributeMap.builder();
        if (p.spaceAbove > 0) {
            b.setSpaceAbove(p.spaceAbove);
        }
        if (p.lineSpacing > 0) {
            b.setLineSpacing(p.lineSpacing);
        }
        if (p.spaceBelow > 0) {
            b.setSpaceBelow(p.spaceBelow);
        }
        m.applyStyle(TextPos.ZERO, m.getDocumentEnd(), b.build(), false);
        return m;
    }

    /// tests vertical navigation in a randomly created text model with all the combinations of
    /// control width, start cursor position, text wrap, space above/below, and line spacing.
    @ParameterizedTest
    @MethodSource("parameters")
    public void navigateUpDown(Params p) {
        RichTextModel m = createModel(p);
        control.setModel(m);
        stageLoader.getStage().setWidth(p.width);
        control.setPrefWidth(p.width);
        control.setWrapText(p.wrap);

        RTUtil.firePulse();
        assertEquals(p.width, control.getWidth(), 5);

        Stage stage = stageLoader.getStage();
        double x = stage.getX() + p.startX;
        double y = stage.getY() + stage.getHeight() / 2.0;
        TextPos pos = control.getTextPosition(x, y);
        control.select(pos);

        int count = 0;
        count += navigate(p, false, false);
        count += navigate(p, true, false);
        count += navigate(p, false, false);
        assertTrue(count > 0);

        count = 0;
        count += navigate(p, false, true);
        count += navigate(p, true, true);
        count += navigate(p, false, true);
        assertTrue(count > 0);
    }

    private int navigate(Params p, boolean down, boolean page) {
        TextPos end = control.getDocumentEnd();
        assertNotNull(end);

        int count = 0;
        // this loop should exit as soon as it hits the beginning or end of the document,
        // but let's set a limit, just in case
        while (count < 10_000) {
            TextPos p0 = control.getCaretPosition();

            if (down) {
                if (page) {
                    control.pageDown();
                } else {
                    control.moveDown();
                }
            } else {
                if (page) {
                    control.pageUp();
                } else {
                    control.moveUp();
                }
            }
            RTUtil.firePulse();

            TextPos p1 = control.getCaretPosition();
            assertNotNull(p1);
            if (p1.equals(TextPos.ZERO)) {
                break;
            } else if (p1.equals(end)) {
                break;
            }
            // did we actually move?
            assertFalse(p0.equals(p1), (down ? "down" : "up") + " p0=" + p0 + " p1=" + p1 + " params=" + p);
            count++;
        }
        return count;
    }
}
