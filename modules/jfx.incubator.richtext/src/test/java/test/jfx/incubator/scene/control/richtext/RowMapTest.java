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

import com.sun.javafx.tk.Toolkit;
import javafx.scene.Scene;
import javafx.scene.shape.PathElement;
import javafx.stage.Stage;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.RichTextAreaShim;
import jfx.incubator.scene.control.richtext.TextPos;
import com.sun.jfx.incubator.scene.control.richtext.CellArrangement;
import jfx.incubator.scene.control.richtext.skin.RichTextAreaSkin;
import jfx.incubator.scene.control.richtext.skin.RowMap;
import com.sun.jfx.incubator.scene.control.richtext.TextCell;
import com.sun.jfx.incubator.scene.control.richtext.VFlow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.jfx.incubator.scene.util.TUtil;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RowMapTest {
    private static final int NUM_PARAGRAPHS = 20;
    private static final int HIDDEN_PARAGRAPHS = 4;
    private RichTextArea control;
    private RowMap rowMap;
    private Stage stage;

    @BeforeEach
    public void beforeEach() {
        TUtil.setUncaughtExceptionHandler();

        control = new RichTextArea();
        TestRichTextAreaSkin skin = new TestRichTextAreaSkin(control);
        rowMap = skin.getTestRowMap();
        control.setSkin(skin);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < NUM_PARAGRAPHS; i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append("line ").append(i);
        }
        control.appendText(sb.toString());
    }

    @AfterEach
    public void afterEach() {
        if (stage != null) {
            stage.hide();
            stage = null;
        }
        TUtil.removeUncaughtExceptionHandler();
    }

    @Test
    public void countVisibleRowsTest() {
        Scene scene = new Scene(control, 300, 200);
        stage = new Stage();
        stage.setScene(scene);
        stage.show();

        Toolkit.getToolkit().firePulse();
        VFlow vFlow = RichTextAreaShim.vflow(control);

        int visibleRowCount = rowMap.getRowCount(NUM_PARAGRAPHS);
        assertEquals(16, visibleRowCount, "Expected 16 visible rows, but rowMap got " + visibleRowCount);
        visibleRowCount = vFlow.getRowCount();
        assertEquals(16, visibleRowCount, "Expected 16 visible rows, but vflow got " + visibleRowCount);
    }

    @Test
    public void mapIndexToRowsTest() {
        // hidden paragraphs are 0, 5, 10, 15, so the expected view rows for model indices 0-19 are:
        int[] expectedViewRows = {0, 0, 1, 2, 3, 4, 4, 5, 6, 7, 8, 8, 9, 10, 11, 12, 12, 13, 14, 15};
        for (int i = 0; i < NUM_PARAGRAPHS; i++) {
            int viewRow = rowMap.getViewRow(i);
            assertEquals(expectedViewRows[i], viewRow, "Expected view row " + expectedViewRows[i] + " for model index " + i + ", but got " + viewRow);
        }
    }

    @Test
    public void mapRowsToIndexTest() {
        // hidden paragraphs are 0, 5, 10, 15, so the expected model indices are the visible ones:
        int[] expectedModelIndices = {1, 2, 3, 4, 6, 7, 8, 9, 11, 12, 13, 14, 16, 17, 18, 19};
        for (int i = 0; i < NUM_PARAGRAPHS - HIDDEN_PARAGRAPHS; i++) {
            int modelIndex = rowMap.getModelIndex(i);
            assertEquals(expectedModelIndices[i], modelIndex, "Expected model index " + expectedModelIndices[i] + " for view row " + i + ", but got " + modelIndex);
            assertFalse(rowMap.isHidden(modelIndex), "Expected model index " + modelIndex + " to be visible, but it is hidden");
        }
    }

    @Test
    public void mapInvariantTest() {
        for (int i = 0; i < NUM_PARAGRAPHS; i++) {
            if (!rowMap.isHidden(i)) {
                int viewRow = rowMap.getViewRow(i);
                int modelIndex = rowMap.getModelIndex(viewRow);
                assertEquals(i, modelIndex, "Expected model index " + i + " for view row " + viewRow + ", but got " + modelIndex);
            }
        }
    }

    @Test
    public void arrangementTest() {
        StringBuilder sb = new StringBuilder();
        for (int i = NUM_PARAGRAPHS; i < 100; i++) {
            if (i > NUM_PARAGRAPHS) {
                sb.append("\n");
            }
            sb.append("line ").append(i + NUM_PARAGRAPHS);
        }
        control.appendText(sb.toString());
        Scene scene = new Scene(control, 300, 200);
        stage = new Stage();
        stage.setScene(scene);
        stage.show();

        Toolkit.getToolkit().firePulse();
        VFlow vFlow = RichTextAreaShim.vflow(control);
        CellArrangement arrangement = RichTextAreaShim.arrangement(control);
        assertNull(arrangement.getCell(0), "Expected cell for row 0 to be null, but got " + arrangement.getCell(0));
        TextCell cell = arrangement.getCell(1);
        assertNotNull(cell, "Expected cell for row 1 to be not null, but got " + cell);
        assertEquals(1, cell.getIndex(), "Expected cell for row 1 to have index 1, but got " + cell.getIndex());

        control.select(TextPos.ofLeading(0, 0));
        assertNull(vFlow.getCaretInfo(), "Expected caret info to be null for hidden paragraph, but got " + vFlow.getCaretInfo());
        control.select(TextPos.ofLeading(1, 0));
        assertNotNull(vFlow.getCaretInfo(), "Expected caret info to be not null for visible paragraph, but got " + vFlow.getCaretInfo());
        control.select(TextPos.ofLeading(4, 1), TextPos.ofLeading(6, 3));
        List<PathElement> rangeShape = vFlow.getRangeShape(TextPos.ofLeading(4, 1), TextPos.ofLeading(6, 3));
        assertNotNull(rangeShape, "Expected range shape to be not null for visible paragraphs, but got " + rangeShape);
        assertFalse(rangeShape.isEmpty(), "Expected range shape to be not empty for visible paragraphs, but got " + rangeShape);
        control.select(TextPos.ofLeading(4, 10));
        control.selectParagraphDown();
        assertEquals(6, control.getCaretPosition().index(), "Expected caret to move to paragraph 6, but got " + control.getCaretPosition().index());

    }

    private static class TestRowMap extends RowMap {

        private final TreeSet<Integer> hiddenParagraphs;

        public TestRowMap(TreeSet<Integer> hiddenParagraphs) {
            this.hiddenParagraphs = hiddenParagraphs;
        }

        @Override
        public int getViewRowImpl(int modelIndex) {
            return modelIndex - hiddenParagraphs.headSet(modelIndex).size();
        }

        @Override
        public int getModelIndexImpl(int row) {
            int modelIndex = row;
            for (int hiddenIndex : hiddenParagraphs) {
                if (hiddenIndex <= modelIndex) {
                    modelIndex++;
                } else {
                    break;
                }
            }
            return modelIndex;
        }

        @Override
        public int getRowCountImpl(int modelParagraphCount) {
            return modelParagraphCount - hiddenParagraphs.size();
        }

        @Override
        public boolean isHiddenImpl(int modelIndex) {
            return hiddenParagraphs.contains(modelIndex);
        }
    }

    private static class TestRichTextAreaSkin extends RichTextAreaSkin {

        private TestRowMap testRowMap;
        public TestRichTextAreaSkin(RichTextArea control) {
            super(control);
        }

        @Override
        protected RowMap createRowMap() {
            testRowMap = new TestRowMap(new TreeSet<>(Set.of(0, 5, 10, 15)));
            return testRowMap;
        }

        public TestRowMap getTestRowMap() {
            return testRowMap;
        }
    }

}
