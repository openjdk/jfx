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

package test.javafx.scene.control.skin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import javafx.beans.InvalidationListener;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.IndexedCellShim;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.skin.VirtualFlowShim;
import javafx.scene.control.skin.VirtualFlowShim.ArrayLinkedListShim;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.javafx.scene.control.SkinStub;

/**
 * Tests for the VirtualFlow class. VirtualFlow is the guts of the ListView,
 * TreeView, and TableView implementations.
 */
public class VirtualFlowTest {
    // The following 4 vars are used when testing the
    private ArrayLinkedListShim<CellStub> list;
    private CellStub a;
    private CellStub b;
    private CellStub c;
    private int prefSizeCounter;

    // The VirtualFlow we are going to test. By default, there are 100 cells
    // and each cell is 100 wide and 25 tall, except for the 30th cell, which
    // is 200 wide and 100 tall.
    private VirtualFlowShim<IndexedCell> flow;


    @BeforeEach
    public void setUp() {
        prefSizeCounter = 0;
        list = new ArrayLinkedListShim<>();
        a = new CellStub(flow, "A");
        b = new CellStub(flow, "B");
        c = new CellStub(flow, "C");

        flow = new VirtualFlowShim();
//        flow.setManaged(false);
        flow.setVertical(true);
        flow.setCellFactory(p -> new CellStub(flow) {
            @Override
            protected double computeMinWidth(double height) {
                return computePrefWidth(height);
            }

            @Override
            protected double computeMaxWidth(double height) {
                return computePrefWidth(height);
            }

            @Override
            protected double computePrefWidth(double height) {
                prefSizeCounter++;
                return flow.isVertical() ? (getIndex() == 29 ? 200 : 100) : (getIndex() == 29 ? 100 : 25);
            }

            @Override
            protected double computeMinHeight(double width) {
                return computePrefHeight(width);
            }

            @Override
            protected double computeMaxHeight(double width) {
                return computePrefHeight(width);
            }

            @Override
            protected double computePrefHeight(double width) {
                prefSizeCounter++;
                return flow.isVertical() ? (getIndex() == 29 ? 100 : 25) : (getIndex() == 29 ? 200 : 100);
            }
        });
        flow.setCellCount(100);
        flow.resize(300, 300);
        pulse();
        // Need a second pulse() call is because this parent can be made
        // "layout" dirty again by its children
        pulse();
    }

    private void pulse() {
        flow.layout();
    }

    /**
     * Asserts that the items in the control LinkedList and the ones in the
     * list are exactly the same.
     */
    private void assertMatch(List<IndexedCell> control, AbstractList<IndexedCell> list) {
        assertEquals(
                     control.size(), list.size(),
                     "The control and list did not have the same sizes. " +
                     "Expected " + control.size() + " but was " + list.size());
        int index = 0;
        Iterator<IndexedCell> itr = control.iterator();
        while (itr.hasNext()) {
            IndexedCell cell = itr.next();
            IndexedCell cell2 = list.get(index);
            assertSame(
                       cell, cell2,
                       "The control and list did not have the same item at " +
                       "index " + index + ". Expected " + cell + " but was " + cell2);
            index++;
        }
    }

    /**
     * Asserts that only the minimal number of cells are used.
     */
    public <T extends IndexedCell> void assertMinimalNumberOfCellsAreUsed(VirtualFlowShim<T> flow) {
        pulse();
        IndexedCell firstCell = VirtualFlowShim.<T>cells_getFirst(flow.cells);
        IndexedCell lastCell = VirtualFlowShim.<T>cells_getLast(flow.cells);
        if (flow.isVertical()) {
            // First make sure that enough cells were created
            assertTrue(
                       firstCell.getLayoutY() <= 0,
                       "There is a gap between the top of the viewport and the first cell");
            assertTrue(
                       lastCell.getLayoutY() + lastCell.getHeight() >= flow.getViewportLength(),
                       "There is a gap between the bottom of the last cell and the bottom of the viewport");

            // Now make sure that no extra cells were created.
            if (VirtualFlowShim.cells_size(flow.cells) > 3) {
                IndexedCell secondLastCell = VirtualFlowShim.<T>cells_get(flow.cells, VirtualFlowShim.cells_size(flow.cells) - 2);
                IndexedCell secondCell = VirtualFlowShim.<T>cells_get(flow.cells, 1);
                assertFalse(
                            secondCell.getLayoutY() <= 0,
                            "There are more cells created before the start of " +
                            "the flow than necessary");
                assertFalse(
                            secondLastCell.getLayoutY() + secondLastCell.getHeight() >= flow.getViewportLength(),
                            "There are more cells created after the end of the " +
                            "flow than necessary");
            }
        } else {
            // First make sure that enough cells were created
            assertTrue(
                       firstCell.getLayoutX() <= 0,
                       "There is a gap between the left of the viewport and the first cell");
            assertTrue(
                       lastCell.getLayoutX() + lastCell.getWidth() >= flow.getViewportLength(),
                       "There is a gap between the right of the last cell and the right of the viewport");

            // Now make sure that no extra cells were created.
            if (VirtualFlowShim.cells_size(flow.cells) > 3) {
                IndexedCell secondLastCell = VirtualFlowShim.<T>cells_get(flow.cells, VirtualFlowShim.cells_size(flow.cells) - 2);
                IndexedCell secondCell = VirtualFlowShim.<T>cells_get(flow.cells, 1);
                assertFalse(
                            secondCell.getLayoutX() <= 0,
                            "There are more cells created before the start of " +
                            "the flow than necessary");
                assertFalse(
                            secondLastCell.getLayoutX() + secondLastCell.getWidth() >= flow.getViewportLength(),
                            "There are more cells created after the end of the " +
                            "flow than necessary");
            }
        }
    }


    /***************************************************************************
     *                          Tests for VirtualFlow                          *
     *                                                                         *
     *  These tests are broken out into several broad categories:              *
     *      - general layout (position of scroll bars, viewport, etc)          *
     *      - cell layout                                                      *
     *      - cell life cycle (creation, configuration, reuse, etc)            *
     *      - position (stable view, adjusts when necessary, etc)              *
     *      - pixel scrolling (cells are reused, position updated, etc)        *
     *                                                                         *
     *  - Test that the preferred width of a vertical flow takes into account  *
     *    the preferred width of the cells that are visible                    *
     *  - Test the same for horizontal when working with a horizontal flow     *
     *  - Test that cells are laid out as expected in a vertical flow          *
     *  - Test the same for a horizontal flow                                  *
     *  - Test that the width of cells in a vertical flow adjusts based on the *
     *    width of the flow's content area                                     *
     *  - Test the same for the height of cells in a horizontal flow           *
     *  - Test that changing the number of cells (up and down) also adjusts    *
     *    the position such that it is "stable", unless that is not possible   *
     *  - Test that after changing the cell factory, things are rebuilt        *
     *  - Test that after changing the cell config function, things are        *
     *    reconfigured.                                                        *
     *  - Test that functions which add to the pile and so forth work as       *
     *    expected.                                                            *
     *  - Test the layout of the scroll bars in various combinations, along    *
     *    with the corner region and so forth.                                 *
     *                                                                         *
     **************************************************************************/

    //--------------------------------------------------------------------------
    //
    //  General Layout
    //
    //--------------------------------------------------------------------------

    /**
     * In this test there are no cells. The VirtualFlow should be laid out such
     * that the scroll bars and corner are not visible, and the clip view fills
     * the entire width/height of the VirtualFlow.
     */
    @Test public void testGeneralLayout_NoCells() {
        flow.setCellCount(0);
        pulse();
        assertFalse(flow.shim_getHbar().isVisible(), "The hbar should have been invisible");
        assertFalse(flow.shim_getVbar().isVisible(), "The vbar should have been invisible");
        assertFalse(flow.get_corner().isVisible(), "The corner should have been invisible");
        assertEquals(flow.getWidth(), flow.get_clipView_getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.get_clipView_getHeight(), 0.0);
        assertMinimalNumberOfCellsAreUsed(flow);

        flow.setVertical(false);
        pulse();
        assertFalse(flow.shim_getHbar().isVisible(), "The hbar should have been invisible");
        assertFalse(flow.shim_getVbar().isVisible(), "The vbar should have been invisible");
        assertFalse(flow.get_corner().isVisible(), "The corner should have been invisible");
//        assertEquals(flow.getWidth(), flow.get_clipView_getWidth(), 0.0);
        assertEquals(flow.getWidth(),
                flow.get_clipView_getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.get_clipView_getHeight(), 0.0);
        assertMinimalNumberOfCellsAreUsed(flow);
    }

    /**
     * When we have a few cells, not enough to fill the viewport, then we need
     * to make sure there is no virtual scroll bar. In this test case the cells
     * are not wider than the viewport so no horizontal bar either.
     */
    @Test public void testGeneralLayout_FewCells() {
        flow.setCellCount(3);
        pulse();
        assertFalse(flow.shim_getHbar().isVisible(), "The hbar should have been invisible");
        assertFalse(flow.shim_getVbar().isVisible(), "The vbar should have been invisible");
        assertFalse(flow.get_corner().isVisible(), "The corner should have been invisible");
        assertEquals(flow.getWidth(), flow.get_clipView_getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.get_clipView_getHeight(), 0.0);
        assertEquals(12, VirtualFlowShim.cells_size(flow.cells)); // we stil have 12 cells (300px / 25px), even if only three filled cells exist
        assertMinimalNumberOfCellsAreUsed(flow);

        flow.setVertical(false);
        pulse();
        assertFalse(flow.shim_getHbar().isVisible(), "The hbar should have been invisible");
        assertFalse(flow.shim_getVbar().isVisible(), "The vbar should have been invisible");
        assertFalse(flow.get_corner().isVisible(), "The corner should have been invisible");
        assertEquals(flow.getWidth(), flow.get_clipView_getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.get_clipView_getHeight(), 0.0);
//        assertEquals(3, VirtualFlowShim.cells_size(flow.cells));
        assertMinimalNumberOfCellsAreUsed(flow);
    }

    /**
     * Tests the case of a few cells (so not requiring a vertical scroll bar),
     * but the cells are wider than the viewport so a horizontal scroll bar is
     * required.
     */
    @Test public void testGeneralLayout_FewCellsButWide() {
        flow.setCellCount(3);
        flow.resize(50, flow.getHeight());
        pulse();
        assertTrue(flow.shim_getHbar().isVisible(), "The hbar should have been visible");
        assertFalse(flow.shim_getVbar().isVisible(), "The vbar should have been invisible");
        assertFalse(flow.get_corner().isVisible(), "The corner should have been invisible");
        assertEquals(flow.getWidth(), flow.get_clipView_getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.get_clipView_getHeight() + flow.shim_getHbar().getHeight(), 0.0);
        assertEquals(flow.shim_getHbar().getLayoutY(), flow.getHeight() - flow.shim_getHbar().getHeight(), 0.0);
        assertMinimalNumberOfCellsAreUsed(flow);

        flow.setVertical(false);
        flow.resize(300, 50);
        pulse();
        assertFalse(flow.shim_getHbar().isVisible(), "The hbar should have been invisible");
        assertTrue(flow.shim_getVbar().isVisible(), "The vbar should have been visible");
        assertFalse(flow.get_corner().isVisible(), "The corner should have been invisible");
        assertEquals(flow.getWidth(), flow.get_clipView_getWidth() + flow.shim_getVbar().getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.get_clipView_getHeight(), 0.0);
        assertEquals(flow.shim_getVbar().getLayoutX(), flow.getWidth() - flow.shim_getVbar().getWidth(), 0.0);
        assertMinimalNumberOfCellsAreUsed(flow);
    }

    /**
     * Tests that having a situation where the hbar in a vertical flow is
     * necessary (due to wide cells) will end up hiding the hbar if the flow
     * becomes wide enough that the hbar is no longer necessary.
     */
    @Test public void testGeneralLayout_FewCellsButWide_ThenNarrow() {
        flow.setCellCount(3);
        flow.resize(50, flow.getHeight());
        pulse();
        flow.resize(300, flow.getHeight());
        pulse();
        assertFalse(flow.shim_getHbar().isVisible(), "The hbar should have been invisible");
        assertFalse(flow.shim_getVbar().isVisible(), "The vbar should have been invisible");
        assertFalse(flow.get_corner().isVisible(), "The corner should have been invisible");
        assertEquals(flow.getWidth(), flow.get_clipView_getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.get_clipView_getHeight(), 0.0);
        assertMinimalNumberOfCellsAreUsed(flow);

        flow.setVertical(false);
        flow.resize(300, 50);
        pulse();
        flow.resize(flow.getWidth(), 300);
        pulse();
        assertFalse(flow.shim_getHbar().isVisible(), "The hbar should have been invisible");
        assertFalse(flow.shim_getVbar().isVisible(), "The vbar should have been invisible");
        assertFalse(flow.get_corner().isVisible(), "The corner should have been invisible");
        assertEquals(flow.getWidth(), flow.get_clipView_getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.get_clipView_getHeight(), 0.0);
        assertMinimalNumberOfCellsAreUsed(flow);
    }

    /**
     * Tests that when there are many cells then the vbar in a vertical flow
     * is used.
     * <p>
     * Note, this test uncovered a bug where the bottom of the last cell was
     * exactly on the bottom edge of the flow and the vbar was not made visible.
     * Be sure to test for this explicitly some time!
     */
    @Test public void testGeneralLayout_ManyCells() {
        assertFalse(flow.shim_getHbar().isVisible(), "The hbar should have been invisible");
        assertTrue(flow.shim_getVbar().isVisible(), "The vbar should have been visible");
        assertFalse(flow.get_corner().isVisible(), "The corner should have been invisible");
        assertEquals(flow.getWidth(), flow.get_clipView_getWidth() + flow.shim_getVbar().getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.get_clipView_getHeight(), 0.0);
        assertEquals(flow.shim_getVbar().getLayoutX(), flow.getWidth() - flow.shim_getVbar().getWidth(), 0.0);
        assertMinimalNumberOfCellsAreUsed(flow);

        flow.setVertical(false);
        pulse();
        assertTrue(flow.shim_getHbar().isVisible(), "The hbar should have been visible");
        assertFalse(flow.shim_getVbar().isVisible(), "The vbar should have been invisible");
        assertFalse(flow.get_corner().isVisible(), "The corner should have been invisible");
        assertEquals(flow.getWidth(), flow.get_clipView_getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.get_clipView_getHeight() + flow.shim_getHbar().getHeight(), 0.0);
        assertEquals(flow.shim_getHbar().getLayoutY(), flow.getHeight() - flow.shim_getHbar().getHeight(), 0.0);
        assertMinimalNumberOfCellsAreUsed(flow);
    }

    /**
     * Test that after having only a few cells, if I then have many cells, that
     * the vbar is shown appropriately.
     */
    @Test public void testGeneralLayout_FewCells_ThenMany() {
        flow.setCellCount(3);
        pulse();
        flow.setCellCount(100);
        pulse();
        assertFalse(flow.shim_getHbar().isVisible(), "The hbar should have been invisible");
        assertTrue(flow.shim_getVbar().isVisible(), "The vbar should have been visible");
        assertFalse(flow.get_corner().isVisible(), "The corner should have been invisible");
        assertEquals(flow.getWidth(), flow.get_clipView_getWidth() + flow.shim_getVbar().getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.get_clipView_getHeight(), 0.0);
        assertEquals(flow.shim_getVbar().getLayoutX(), flow.getWidth() - flow.shim_getVbar().getWidth(), 0.0);
        assertMinimalNumberOfCellsAreUsed(flow);

        flow.setVertical(false);
        flow.setCellCount(3);
        pulse();
        flow.setCellCount(100);
        pulse();
        assertTrue(flow.shim_getHbar().isVisible(), "The hbar should have been visible");
        assertFalse(flow.shim_getVbar().isVisible(), "The vbar should have been invisible");
        assertFalse(flow.get_corner().isVisible(), "The corner should have been invisible");
        assertEquals(flow.getWidth(), flow.get_clipView_getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.get_clipView_getHeight() + flow.shim_getHbar().getHeight(), 0.0);
        assertEquals(flow.shim_getHbar().getLayoutY(), flow.getHeight() - flow.shim_getHbar().getHeight(), 0.0);
        assertMinimalNumberOfCellsAreUsed(flow);
    }

    /**
     * Test the case where there are many cells and they are wider than the
     * viewport. We should have the hbar, vbar, and corner region in this case.
     */
    @Test public void testGeneralLayout_ManyCellsAndWide() {
        flow.resize(50, flow.getHeight());
        pulse();
        assertTrue(flow.shim_getHbar().isVisible(), "The hbar should have been visible");
        assertTrue(flow.shim_getVbar().isVisible(), "The vbar should have been visible");
        assertTrue(flow.get_corner().isVisible(), "The corner should have been visible");
        assertEquals(flow.getWidth(), flow.get_clipView_getWidth() + flow.shim_getVbar().getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.get_clipView_getHeight() + flow.shim_getHbar().getHeight(), 0.0);
        assertEquals(flow.shim_getVbar().getLayoutX(), flow.getWidth() - flow.shim_getVbar().getWidth(), 0.0);
        assertEquals(flow.shim_getVbar().getWidth(), flow.get_corner().getWidth(), 0.0);
        assertEquals(flow.shim_getHbar().getHeight(), flow.get_corner().getHeight(), 0.0);
        assertEquals(flow.shim_getHbar().getWidth(), flow.getWidth() - flow.get_corner().getWidth(), 0.0);
        assertEquals(flow.shim_getVbar().getHeight(), flow.getHeight() - flow.get_corner().getHeight(), 0.0);
        assertEquals(flow.get_corner().getLayoutX(), flow.getWidth() - flow.get_corner().getWidth(), 0.0);
        assertEquals(flow.get_corner().getLayoutY(), flow.getHeight() - flow.get_corner().getHeight(), 0.0);
        assertMinimalNumberOfCellsAreUsed(flow);

        flow.setVertical(false);
        flow.resize(300, 50);
        pulse();
        assertTrue(flow.shim_getHbar().isVisible(), "The hbar should have been visible");
        assertTrue(flow.shim_getVbar().isVisible(), "The vbar should have been visible");
        assertTrue(flow.get_corner().isVisible(), "The corner should have been visible");
        assertEquals(flow.getWidth(), flow.get_clipView_getWidth() + flow.shim_getVbar().getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.get_clipView_getHeight() + flow.shim_getHbar().getHeight(), 0.0);
        assertEquals(flow.shim_getVbar().getLayoutX(), flow.getWidth() - flow.shim_getVbar().getWidth(), 0.0);
        assertEquals(flow.shim_getVbar().getWidth(), flow.get_corner().getWidth(), 0.0);
        assertEquals(flow.shim_getHbar().getHeight(), flow.get_corner().getHeight(), 0.0);
        assertEquals(flow.shim_getHbar().getWidth(), flow.getWidth() - flow.get_corner().getWidth(), 0.0);
        assertEquals(flow.shim_getVbar().getHeight(), flow.getHeight() - flow.get_corner().getHeight(), 0.0);
        assertEquals(flow.get_corner().getLayoutX(), flow.getWidth() - flow.get_corner().getWidth(), 0.0);
        assertEquals(flow.get_corner().getLayoutY(), flow.getHeight() - flow.get_corner().getHeight(), 0.0);
        assertMinimalNumberOfCellsAreUsed(flow);
    }

    /**
     * Tests that when the vertical flag changes, it results in layout
     */
    @Test public void testGeneralLayout_VerticalChangeResultsInNeedsLayout() {
        assertFalse(flow.isNeedsLayout());
        flow.setVertical(false);
        assertTrue(flow.isNeedsLayout());
    }

    /**
     * Tests that the range of the non-virtual scroll bar is valid
     */
    @Test public void testGeneralLayout_NonVirtualScrollBarRange() {
        flow.resize(50, flow.getHeight());
        pulse();
        assertEquals(0, flow.shim_getHbar().getMin(), 0.0);
        assertEquals(flow.shim_getMaxPrefBreadth() - flow.get_clipView_getWidth(), flow.shim_getHbar().getMax(), 0.0);
        assertEquals((flow.get_clipView_getWidth()/flow.shim_getMaxPrefBreadth()) * flow.shim_getHbar().getMax(), flow.shim_getHbar().getVisibleAmount(), 0.0);
        flow.setPosition(.28f);
        pulse();
        assertEquals(0, flow.shim_getHbar().getMin(), 0.0);
        assertEquals(flow.shim_getMaxPrefBreadth() - flow.get_clipView_getWidth(), flow.shim_getHbar().getMax(), 0.0);
        assertEquals((flow.get_clipView_getWidth()/flow.shim_getMaxPrefBreadth()) * flow.shim_getHbar().getMax(), flow.shim_getHbar().getVisibleAmount(), 0.0);

        flow.setVertical(false);
        flow.setPosition(0);
        flow.resize(300, 50);
        pulse();
        assertEquals(0, flow.shim_getVbar().getMin(), 0.0);
        assertEquals(flow.shim_getMaxPrefBreadth() - flow.get_clipView_getHeight(), flow.shim_getVbar().getMax(), 0.0);
        assertEquals((flow.get_clipView_getHeight()/flow.shim_getMaxPrefBreadth()) * flow.shim_getVbar().getMax(), flow.shim_getVbar().getVisibleAmount(), 0.0);
        flow.setPosition(.28);
        pulse();
        assertEquals(0, flow.shim_getVbar().getMin(), 0.0);
        assertEquals(flow.shim_getMaxPrefBreadth() - flow.get_clipView_getHeight(), flow.shim_getVbar().getMax(), 0.0);
        assertEquals((flow.get_clipView_getHeight()/flow.shim_getMaxPrefBreadth()) * flow.shim_getVbar().getMax(), flow.shim_getVbar().getVisibleAmount(), 0.0);
    }

    /**
     * Tests that the maxPrefBreadth is computed correctly for the first page of cells.
     * In our test case, the first page of cells have a uniform pref.
     */
    @Test public void testGeneralLayout_maxPrefBreadth() {
        assertEquals(100, flow.shim_getMaxPrefBreadth(), 0.0);
    }

    /**
     * Tests that even after the first computation of max pref, that it is
     * updated when we encounter a new cell (while scrolling for example) that
     * has a larger pref.
     */
    @Disabled
    @Test public void testGeneralLayout_maxPrefBreadthUpdatedWhenEncounterLargerPref() {
        flow.setPosition(.28);
        pulse();
        assertEquals(200, flow.shim_getMaxPrefBreadth(), 0.0);
    }

    /**
     * Tests that if we encounter cells or pages of cells with smaller prefs
     * than the max pref that we will keep the max pref the same.
     */
    @Disabled
    @Test public void testGeneralLayout_maxPrefBreadthRemainsSameWhenEncounterSmallerPref() {
        flow.setPosition(.28);
        pulse();
        flow.setPosition(.8);
        pulse();
        assertEquals(200, flow.shim_getMaxPrefBreadth(), 0.0);
    }

    /**
     * Tests that changes to the vertical property will clear the maxPrefBreadth
     */
    @Test public void testGeneralLayout_VerticalChangeClearsmaxPrefBreadth() {
        flow.setVertical(false);
        assertEquals(-1, flow.shim_getMaxPrefBreadth(), 0.0);
    }

    /**
     * Tests that changes to the cell count will not affect maxPrefBreadth.
     */
    @Disabled
    @Test public void testGeneralLayout_maxPrefBreadthUnaffectedByCellCountChanges() {
        flow.setCellCount(10);
        pulse();
        assertEquals(100, flow.shim_getMaxPrefBreadth(), 0.0);
        flow.setCellCount(100);
        pulse();
        flow.setPosition(.28);
        pulse();
        assertEquals(200, flow.shim_getMaxPrefBreadth(), 0.0);
        flow.setCellCount(10);
        pulse();
        assertEquals(200, flow.shim_getMaxPrefBreadth(), 0.0);
    }

    /**
     * Tests that as we scroll, if the non-virtual scroll bar is visible, then
     * as we update maxPrefBreadth it will not affect the non-virtual scroll bar's
     * value <b>unless</b> the value is such that the scroll bar is scrolled
     * all the way to the end, in which case it will remain scrolled to the
     * end.
     */
    @Test public void testGeneralLayout_maxPrefBreadthScrollBarValueInteraction() {
        flow.resize(50, flow.getHeight());
        flow.shim_getHbar().setValue(30);
        pulse();
        flow.setPosition(.28);
        pulse();
//        assertEquals(30, flow.shim_getHbar().getValue(), 0.0);

        // Reset the test and this time check what happens when we are scrolled
        // to the very right
        flow.setPosition(0);
        flow.setVertical(false);
        flow.setVertical(true);
        pulse();
        assertEquals(100, flow.shim_getMaxPrefBreadth(), 0.0);
        flow.shim_getHbar().setValue(flow.shim_getHbar().getMax()); // scroll to the end
        flow.setPosition(.28);
        pulse();
        assertEquals(flow.shim_getHbar().getMax(), flow.shim_getHbar().getValue(), 0.0);

        flow.setVertical(false);
        flow.setPosition(0);
        flow.shim_getHbar().setValue(0);
        flow.resize(300, 50);
        pulse();
        flow.shim_getVbar().setValue(30);
        pulse();
        flow.setPosition(.28);
        pulse();
        assertEquals(30, flow.shim_getVbar().getValue(), 0.0);

        // Reset the test and this time check what happens when we are scrolled
        // to the very right
        flow.setPosition(0);
        flow.setVertical(true);
        flow.setVertical(false);
        pulse();
        assertEquals(100, flow.shim_getMaxPrefBreadth(), 0.0);
        flow.shim_getVbar().setValue(flow.shim_getVbar().getMax()); // scroll to the end
        flow.setPosition(.28);
        pulse();
        assertEquals(flow.shim_getVbar().getMax(), flow.shim_getVbar().getValue(), 0.0);
    }

    @Test public void testGeneralLayout_ScrollToEndOfVirtual_BarStillVisible() {
        assertTrue(flow.shim_getVbar().isVisible(), "The vbar was expected to be visible");
        flow.setPosition(1);
        pulse();
        assertTrue(flow.shim_getVbar().isVisible(), "The vbar was expected to be visible");

        flow.setPosition(0);
        flow.setVertical(false);
        pulse();
        assertTrue(flow.shim_getHbar().isVisible(), "The hbar was expected to be visible");
        flow.setPosition(1);
        pulse();
        assertTrue(flow.shim_getHbar().isVisible(), "The hbar was expected to be visible");
    }

    // Need to test all the resize operations and make sure the position of
    // nodes is as expected, that they don't get shifted etc.

    // Test: Scroll to the bottom, expand size out, then make smaller. The
    // thumb/scroll is not consistent right now.

    // TODO figure out and deal with what happens when orientation changes
    // to the hbar.value and vbar.value. Do they just switch? Probably not?
    // Probably reset the non-virtual direction and swap the virtual one over.
    // So if vbar was .5 and hbar was 30, when we set vertical = false, then
    // we change the hbar to .5 and the vbar to 0. However this has to be done
    // at the same time that the "virtual" property of the scroll bars is
    // changed

    //--------------------------------------------------------------------------
    //
    //  Cell Layout
    //
    //--------------------------------------------------------------------------

    /**
     * Test to make sure that we are virtual -- that all cells are not being
     * created.
     */
    @Test public void testCellLayout_NotAllCellsAreCreated() {
        // due to the initial size of the VirtualFlow and the number of cells
        // and their heights, we should have more cells than we have space to
        // fit them and so only enough cells should be created to meet our
        // needs and not any more than that
        assertTrue(VirtualFlowShim.cells_size(flow.cells) < flow.getCellCount(), "All of the cells were created");
        assertMinimalNumberOfCellsAreUsed(flow);
    }

    /**
     * Tests the size and position of all the cells to make sure they were
     * laid out properly.
     */
    @Test public void testCellLayout_CellSizes_AfterLayout() {
        double offset = 0.0;
        for (int i = 0; i < VirtualFlowShim.cells_size(flow.cells); i++) {
            IndexedCell cell = VirtualFlowShim.<IndexedCell>cells_get(flow.cells, i);
            assertEquals(25, cell.getHeight(), 0.0);
            assertEquals(offset, cell.getLayoutY(), 0.0);
            offset += cell.getHeight();
        }

        offset = 0.0;
        flow.setVertical(false);
        pulse();
        for (int i = 0; i < VirtualFlowShim.cells_size(flow.cells); i++) {
            IndexedCell cell = VirtualFlowShim.<IndexedCell>cells_get(flow.cells, i);
            assertEquals(25, cell.getWidth(), 0.0);
            assertEquals(offset, cell.getLayoutX(), 0.0);
            offset += cell.getWidth();
        }
    }

    /**
     * Test the widths of the cells when the viewport is wider than the
     * max pref width/height. They should be uniform, and should be the
     * width of the viewport.
     */
    @Test public void testCellLayout_ViewportWiderThanmaxPrefBreadth() {
        // Note that the pref width of everything is 100, but the actual
        // available width is much larger (300 - hbar.width or
        // 300 - vbar.height) and so the non-virtual dimension of the cell
        // should be larger than the max pref
        double expected = flow.get_clipView_getWidth();
        for (int i = 0; i < VirtualFlowShim.cells_size(flow.cells); i++) {
            IndexedCell cell = VirtualFlowShim.<IndexedCell>cells_get(flow.cells, i);
            assertEquals(expected, cell.getWidth(), 0.0);
        }

        flow.setVertical(false);
        pulse();
        expected = flow.get_clipView_getHeight();
        for (int i = 0; i < VirtualFlowShim.cells_size(flow.cells); i++) {
            IndexedCell cell = VirtualFlowShim.<IndexedCell>cells_get(flow.cells, i);
            assertEquals(expected, cell.getHeight(), 0.0);
        }
    }

    /**
     * Test the widths of the cells when the viewport is shorter than the
     * max pref width/height. They should be uniform, and should be the max
     * pref.
     */
    @Test public void testCellLayout_ViewportShorterThanmaxPrefBreadth() {
        flow.resize(50, flow.getHeight());
        pulse();
        assertEquals(100, flow.shim_getMaxPrefBreadth(), 0.0);
        for (int i = 0; i < VirtualFlowShim.cells_size(flow.cells); i++) {
            IndexedCell cell = VirtualFlowShim.<IndexedCell>cells_get(flow.cells, i);
            assertEquals(flow.shim_getMaxPrefBreadth(), cell.getWidth(), 0.0);
        }

        flow.setVertical(false);
        flow.resize(flow.getWidth(), 50);
        pulse();
        assertEquals(100, flow.shim_getMaxPrefBreadth(), 0.0);
        for (int i = 0; i < VirtualFlowShim.cells_size(flow.cells); i++) {
            IndexedCell cell = VirtualFlowShim.<IndexedCell>cells_get(flow.cells, i);
            assertEquals(flow.shim_getMaxPrefBreadth(), cell.getHeight(), 0.0);
        }
    }

    /**
     * Test that when we scroll and encounter a cell which has a larger pref
     * than we have previously encountered (happens in this test when we visit
     * the cell for item #29), then the max pref is updated and the cells are
     * all resized to match.
     */
    @Disabled
    @Test public void testCellLayout_ScrollingFindsCellWithLargemaxPrefBreadth() {
        flow.resize(50, flow.getHeight());
        flow.setPosition(.28); // happens to position such that #29 is visible
        pulse();
        assertEquals(200, flow.shim_getMaxPrefBreadth(), 0.0);
        for (int i = 0; i < VirtualFlowShim.cells_size(flow.cells); i++) {
            IndexedCell cell = VirtualFlowShim.<IndexedCell>cells_get(flow.cells, i);
            assertEquals(flow.shim_getMaxPrefBreadth(), cell.getWidth(), 0.0);
        }

        flow.setVertical(false);
        flow.resize(flow.getWidth(), 50);
        // NOTE Run this test without the pulse and it fails!
        pulse();
        flow.setPosition(.28);
        pulse();
        assertEquals(200, flow.shim_getMaxPrefBreadth(), 0.0);
        for (int i = 0; i < VirtualFlowShim.cells_size(flow.cells); i++) {
            IndexedCell cell = VirtualFlowShim.<IndexedCell>cells_get(flow.cells, i);
            assertEquals(flow.shim_getMaxPrefBreadth(), cell.getHeight(), 0.0);
        }
    }

    /**
     * Checks that the initial set of cells (the first page of cells) are
     * indexed starting with cell #0 and working up from there.
     */
    @Test public void testCellLayout_CellIndexes_FirstPage() {
        for (int i = 0; i < VirtualFlowShim.cells_size(flow.cells); i++) {
            assertEquals(i, VirtualFlowShim.<IndexedCell>cells_get(flow.cells, i).getIndex());
        }
    }

    /**
     * The bug here is that if layout() is called on the flow numerous times,
     * but nothing substantially has changed, we should reuse the cells in the
     * same order they were before. We had a bug where when you clicked on the
     * ListView, the click wouldn't register. This was because by clicking we
     * were giving focus to the ListView, which caused a layout(), and then the
     * cells location was shuffled. It didn't look like it to the user, but that
     * is what happened. As a result, when the mouse release took place, the
     * event was delivered to a different cell than expected and misbehavior
     * took place.
     */
    @Test public void testCellLayout_LayoutWithoutChangingThingsUsesCellsInSameOrderAsBefore() {
        List<IndexedCell> cells = new LinkedList<>();
        for (int i = 0; i < VirtualFlowShim.cells_size(flow.cells); i++) {
            cells.add(VirtualFlowShim.<IndexedCell>cells_get(flow.cells, i));
        }
        assertMatch(cells, flow.cells); // sanity check
        flow.requestLayout();
        pulse();
        assertMatch(cells, flow.cells);
        flow.setPosition(1);
        pulse();
        cells.clear();
        for (int i = 0; i < VirtualFlowShim.cells_size(flow.cells); i++) {
            cells.add(VirtualFlowShim.<IndexedCell>cells_get(flow.cells, i));
        }
        flow.requestLayout();
        pulse();
        assertMatch(cells, flow.cells);
    }


    @Test public void testCellLayout_BiasedCellAndLengthBar() {
        flow.setCellFactory(param -> new CellStub(flow) {
            @Override
            protected double computeMinWidth(double height) {
                return 0;
            }

            @Override
            protected double computeMaxWidth(double height) {
                return Double.MAX_VALUE;
            }

            @Override
            protected double computePrefWidth(double height) {
                return 200;
            }

            @Override
            protected double computeMinHeight(double width) {
                return 0;
            }

            @Override
            protected double computeMaxHeight(double width) {
                return Double.MAX_VALUE;
            }

            @Override
            protected double computePrefHeight(double width) {
                return getIndex() == 0 ? 100 - 5 * (Math.floorDiv((int) width - 200, 10)) : 100;
            }
        });
        flow.setCellCount(3);
        flow.recreateCells(); // This help to override layoutChildren() in flow.setCellCount()
        flow.shim_getVbar().setPrefWidth(20); // Since Skins are not initialized, we set the pref width explicitly
        flow.requestLayout();
        pulse();
        assertEquals(300, VirtualFlowShim.<IndexedCell>cells_get(flow.cells, 0).getWidth(), 1e-100);
        assertEquals(50, VirtualFlowShim.<IndexedCell>cells_get(flow.cells, 0).getHeight(), 1e-100);

        flow.resize(200, 300);

        flow.requestLayout();
        pulse();
        assertEquals(200, VirtualFlowShim.<IndexedCell>cells_get(flow.cells, 0).getWidth(), 1e-100);
        assertEquals(100, VirtualFlowShim.<IndexedCell>cells_get(flow.cells, 0).getHeight(), 1e-100);

    }

    //--------------------------------------------------------------------------
    //
    //  Cell Life Cycle
    //
    //--------------------------------------------------------------------------

    @Test public void testCellLifeCycle_CellsAreCreatedOnLayout() {
        // when the flow was first created in setUp we do a layout()
        assertTrue(VirtualFlowShim.cells_size(flow.cells) > 0, "The cells didn't get created");
    }


    //--------------------------------------------------------------------------
    //
    //  Position
    //
    //--------------------------------------------------------------------------

    //--------------------------------------------------------------------------
    //
    //  Pixel Scrolling
    //
    //--------------------------------------------------------------------------

    //--------------------------------------------------------------------------
    //
    //  Cell Count Changes
    //
    //--------------------------------------------------------------------------

    // want to test that the view remains stable when the cell count changes

    // LIST VIEW: test that using a bunch of nodes as items to a ListView works
    // LIST VIEW: test that inserting an item moves the selected index to keep in sync
    // test that dynamically changing all of the contents causes them to refresh

    // test that when the number of cells change, that things are laid out
//    @Test public void testCellCountChanges_FirstRowIsRemoved() {
//
//    }
//
//    @Test public void testCellCountChanges_MiddleRowIsRemoved() {
//
//    }
//
//    @Test public void testCellCountChanges_LastRowIsRemoved() {
//
//    }
//
//--    @Test public void testCellCountChanges_SelectedRowRemoved() {
//--
//--    }
//--
//--    @Test public void testCellCountChanges_NonSelectedRowRemoved() {
//--
//--    }
//
//    @Test public void testCellCountChanges_FirstRowIsAdded() {
//
//    }
//
//    @Test public void testCellCountChanges_MiddleRowIsAdded() {
//
//    }
//
//    @Test public void testCellCountChanges_LastRowIsAdded() {
//
//    }
//
//--    @Test public void testCellCountChanges_RowIsAddedBeforeSelectedRow() {
//--
//--    }
//--
//--    @Test public void testCellCountChanges_RowIsAddedAfterSelectedRow() {
//--
//--    }

    //--------------------------------------------------------------------------
    //
    //  VirtualFlow State Changes
    //
    //--------------------------------------------------------------------------

    /**
     * Tests that when the createCell method changes, it results in layout
     */
    @Test public void testCreateCellFunctionChangesResultInNeedsLayoutAndNoCellsAndNoAccumCell() {
        assertFalse(flow.isNeedsLayout());
        flow.getCellLength(49); // forces accum cell to be created
        assertNotNull(flow.get_accumCell(), "Accum cell was null");
        flow.setCellFactory(p -> new CellStub(flow));
        assertTrue(flow.isNeedsLayout());
        assertNull(flow.get_accumCell(), "accumCell didn't get cleared");
    }


    //--------------------------------------------------------------------------
    //
    //  Tests on specific functions
    //
    //--------------------------------------------------------------------------

    @Test public void test_getCellLength() {
        assertEquals(100, flow.getCellCount());
        for (int i = 0; i < 50; i++) {
            if (i != 29) assertEquals(25, flow.getCellLength(i), 0.0);
        }
        flow.setVertical(false);
        flow.requestLayout();
        pulse();
        assertEquals(100, flow.getCellCount());
        for (int i = 0; i < 50; i++) {
            if (i != 29) assertEquals(25, flow.getCellLength(i), 0.0, "Bad index: " + i);
        }
    }

    /*
    ** if we scroll the flow by a number of LINES,
    ** without having done anything to select a cell
    ** the flow should scroll.
    */
    @Test public void testInitialScrollEventActuallyScrolls() {
        /*
        ** re-initialize this, as it must be the first
        ** interaction with the flow
        */
        flow = new VirtualFlowShim();
        flow.setVertical(true);
        flow.setCellFactory(p -> new CellStub(flow) {
            @Override
            protected double computeMinWidth(double height) {
                return computePrefWidth(height);
            }

            @Override
            protected double computeMaxWidth(double height) {
                return computePrefWidth(height);
            }

            @Override
            protected double computePrefWidth(double height) {
                return flow.isVertical() ? (c.getIndex() == 29 ? 200 : 100) : (c.getIndex() == 29 ? 100 : 25);
            }

            @Override
            protected double computeMinHeight(double width) {
                return computePrefHeight(width);
            }

            @Override
            protected double computeMaxHeight(double width) {
                return computePrefHeight(width);
            }

            @Override
            protected double computePrefHeight(double width) {
                return flow.isVertical() ? (c.getIndex() == 29 ? 100 : 25) : (c.getIndex() == 29 ? 200 : 100);
            }
        });

        flow.setCellCount(100);
        flow.resize(300, 300);
        pulse();

        double originalValue = flow.getPosition();

        Event.fireEvent(flow,
              new ScrollEvent(ScrollEvent.SCROLL,
                          0.0, -10.0, 0.0, -10.0,
                          false, false, false, false, true, false,
                          0, 0,
                          0, 0,
                          ScrollEvent.HorizontalTextScrollUnits.NONE, 0.0,
                          ScrollEvent.VerticalTextScrollUnits.LINES, -1.0,
                          0, null));

        assertTrue(originalValue != flow.getPosition());
    }

    @Test public void test_RT_36507() {
        flow = new VirtualFlowShim();
        flow.setVertical(true);
        // Worst case scenario is that the cells have height = 0.
        // The code should prevent creating more than 100 of these zero height cells
        // (since viewportLength is 100).
        // An "INFO: index exceeds maxCellCount" message should print out.
        flow.setCellFactory(p -> new CellStub(flow) {
            @Override
            protected double computeMaxHeight(double width) {
                return 0;
            }

            @Override
            protected double computePrefHeight(double width) {
                return 0;
            }

            @Override
            protected double computeMinHeight(double width) {
                return 0;
            }

        });
        flow.setCellCount(10);
        flow.setViewportLength(100);
        flow.addLeadingCells(1, 0);
        flow.sheetChildren.addListener((InvalidationListener) (o) -> {
            int count = ((List) o).size();
            assertTrue(count <= 100, Integer.toString(count));
        });
        flow.addTrailingCells(true);
    }

    private int rt36556_instanceCount;
    @Test public void test_rt36556() {
        rt36556_instanceCount = 0;
        flow = new VirtualFlowShim();
        flow.setVertical(true);
        flow.setCellFactory(p -> {
            rt36556_instanceCount++;
            return new CellStub(flow);
        });
        flow.setCellCount(100);
        flow.resize(300, 300);
        pulse();
        final int cellCountAtStart = rt36556_instanceCount;
        flow.scrollPixels(10000);
        pulse();
        assertEquals(cellCountAtStart, rt36556_instanceCount);
        assertNull(flow.getVisibleCell(0));
        assertMinimalNumberOfCellsAreUsed(flow);
    }

    @Test public void test_rt36556_scrollto() {
        rt36556_instanceCount = 0;
        flow = new VirtualFlowShim();
        flow.setVertical(true);
        flow.setCellFactory(p -> {
            rt36556_instanceCount++;
            return new CellStub(flow);
        });
        flow.setCellCount(100);
        flow.resize(300, 300);
        pulse();
        final int cellCountAtStart = rt36556_instanceCount;
        flow.scrollTo(80);
        pulse();
        assertEquals(cellCountAtStart, rt36556_instanceCount);
        assertNull(flow.getVisibleCell(0));
        assertMinimalNumberOfCellsAreUsed(flow);
    }

    @Test public void test_RT39035() {
        flow.scrollPixels(250);
        pulse();
        flow.scrollPixels(500);
        pulse();
        assertTrue(flow.getPosition() < 1.0);
        assertMinimalNumberOfCellsAreUsed(flow);
    }

    @Test public void test_RT37421() {
        flow.setPosition(0.98);
        pulse();
        flow.scrollPixels(100);
        pulse();
        assertEquals(1.0, flow.getPosition(), 0.0);
        assertMinimalNumberOfCellsAreUsed(flow);
    }

    @Test public void test_RT39568() {
        flow.shim_getHbar().setPrefHeight(16);
        flow.resize(50, flow.getHeight());
        flow.setPosition(1);
        pulse();
        assertTrue(flow.shim_getHbar().isVisible(), "The hbar should have been visible");
        assertMinimalNumberOfCellsAreUsed(flow);
        assertEquals(flow.getViewportLength()-25.0, VirtualFlowShim.<IndexedCell>cells_getLast(flow.cells).getLayoutY(), 0.0);
    }

    private void assertLastCellInsideViewport(boolean vertical) {
        flow.setVertical(vertical);
        flow.resize(400, 400);

        int total = 10000;
        flow.setCellCount(total);
        pulse();

        int count = 9000;
        flow.setPosition(0d);
        pulse();
        flow.setPosition(((double)count) / total);
        pulse();

        //simulate 500 right key strokes
        for (int i = 0; i < 500; i++) {
            count++;
            flow.scrollTo(count);
            pulse();
        }

        IndexedCell vc = flow.getCell(count);

        double cellPosition = flow.getCellPosition(vc);
        double cellLength = flow.getCellLength(count);
        double viewportLength = flow.getViewportLength();

        assertEquals(viewportLength, (cellPosition + cellLength), 0.1, "Last cell must end on viewport size");
    }

    @Test
    public void testScrollToTopOfLastLargeCell() {
        double flowHeight = 150;
        int cellCount = 2;

        flow = new VirtualFlowShim<>();
        flow.setCellFactory(p -> new CellStub(flow) {
            @Override
            protected double computePrefHeight(double width) {
                return getIndex() == cellCount -1 ? 200 : 100;
            }

            @Override
            protected double computeMinHeight(double width) {
                return computePrefHeight(width);
            }

            @Override
            protected double computeMaxHeight(double width) {
                return computePrefHeight(width);
            }
        });
        flow.setVertical(true);

        flow.resize(50,flowHeight);
        flow.setCellCount(cellCount);
        pulse();

        flow.scrollToTop(cellCount - 1);
        pulse();

        IndexedCell<?> cell = flow.getCell(cellCount - 1);
        double cellPosition = flow.getCellPosition(cell);

        assertEquals(0, cellPosition, 0.1, "Last cell must be aligned to top of the viewport");
    }

    @Test
    public void testImmediateScrollTo() {
        flow.setCellCount(100);
        flow.scrollTo(90);
        pulse();
        IndexedCell vc = flow.getVisibleCell(90);
        assertNotNull(vc);
    }

    @Test
    // see JDK-8197536
    public void testScrollOneCell() {
        assertLastCellInsideViewport(true);
    }

    @Test
    // see JDK-8197536
    public void testScrollOneCellHorizontal() {
        assertLastCellInsideViewport(false);
    }

    @Test
    // see JDK-8178297
    public void testPositionCellRemainsConstant() {
        flow.setVertical(true);
        flow.setCellCount(20);
        flow.resize(300, 300);
        flow.scrollPixels(10);
        pulse();

        IndexedCell vc = flow.getCell(0);
        double cellPosition = flow.getCellPosition(vc);
        assertEquals(-10d, cellPosition, 0d, "Wrong first cell position");

        for (int i = 1; i < 10; i++) {
            flow.setCellCount(20 + i);
            pulse();
            vc = flow.getCell(0);
            cellPosition = flow.getCellPosition(vc);
            assertEquals(-10d, cellPosition, 0d, "Wrong first cell position after inserting " + i + " cells");
        }
    }

    @Test
    // see JDK-8306447
    public void testPositionCellRemainsConstantWithManyItems() {
        flow.setVertical(true);
        flow.setCellCount(100);
        flow.resize(300, 300);
        // scroll up and down, to populate the size cache
        for (int i = 0; i < 20; i++) {
            flow.scrollPixels(1);
            pulse();
            flow.scrollPixels(-1);
            pulse();
        }
        flow.scrollPixels(911);
        pulse();

        IndexedCell vc = flow.getCell(33);
        double cellPosition = flow.getCellPosition(vc);
        // cell 33 should be at (32 x 25 + 1 x 100) - 911 = -11
        assertEquals(-11d, cellPosition, 0d, "Wrong first cell position");

        for (int i = 1; i < 10; i++) {
            flow.setCellCount(100 + i);
            pulse();
            vc = flow.getCell(33);
            cellPosition = flow.getCellPosition(vc);
            assertEquals(-11d, cellPosition, 0d, "First cell position changed after adding " + i + " cells on large irregular list");
        }
    }



    @Test
    // see JDK-8252811
    public void testSheetChildrenRemainsConstant() {
        flow.setVertical(true);
        flow.setCellCount(20);
        flow.resize(300, 300);
        pulse();

        int sheetChildrenSize = flow.sheetChildren.size();
        assertEquals(12, sheetChildrenSize, "Wrong number of sheet children");

        for (int i = 1; i < 50; i++) {
            flow.setCellCount(20 + i);
            pulse();
            sheetChildrenSize = flow.sheetChildren.size();
            assertEquals(12, sheetChildrenSize, "Wrong number of sheet children after inserting " + i + " items");
        }

        for (int i = 1; i < 50; i++) {
            flow.setCellCount(70 - i);
            pulse();
            sheetChildrenSize = flow.sheetChildren.size();
            assertEquals(12, sheetChildrenSize, "Wrong number of sheet children after removing " + i + " items");
        }

        flow.setCellCount(0);
        pulse();
        sheetChildrenSize = flow.sheetChildren.size();
        assertEquals(12, sheetChildrenSize, "Wrong number of sheet children after removing all items");
    }

    @Test
    // See JDK-8291908
    public void test_noEmptyTrailingCells() {
        flow = new VirtualFlowShim();
        flow.setVertical(true);
        flow.setCellFactory(p -> new CellStub(flow) {
            @Override
            protected double computeMaxHeight(double width) {
                return computePrefHeight(width);
            }

            @Override
            protected double computePrefHeight(double width) {
                return (getIndex() > 100) ? 1 : 20;
            }

            @Override
            protected double computeMinHeight(double width) {
                return computePrefHeight(width);
            }

        });
        flow.setCellCount(100);
        flow.setViewportLength(1000);
        flow.resize(100, 1000);
        pulse();
        flow.sheetChildren.addListener((InvalidationListener) (o) -> {
            int count = ((List) o).size();
            assertTrue(count < 101, Integer.toString(count));
        });
        flow.scrollTo(99);
        pulse();
    }

    @Test
    public void testVerticalChangeShouldResetIndex() {
        List<IndexedCell<?>> cells = new ArrayList<>();

        flow = new VirtualFlowShim<>();
        flow.setVertical(true);
        flow.setCellFactory(p -> {
            CellStub cellStub = new CellStub(flow);
            cells.add(cellStub);
            return cellStub;
        });
        flow.setCellCount(100);
        flow.setViewportLength(100);
        flow.resize(100, 100);
        pulse();

        flow.setVertical(false);

        for (IndexedCell<?> cell : cells) {
            assertEquals(-1, cell.getIndex());
        }
    }

    @Test
    public void testCellFactoryChangeShouldResetIndex() {
        List<IndexedCell<?>> cells = new ArrayList<>();

        flow = new VirtualFlowShim<>();
        flow.setVertical(true);
        flow.setCellFactory(p -> {
            CellStub cellStub = new CellStub(flow);
            cells.add(cellStub);
            return cellStub;
        });
        flow.setCellCount(100);
        flow.setViewportLength(100);
        flow.resize(100, 100);
        pulse();

        flow.setCellFactory(p -> new CellStub(flow));

        pulse();

        for (IndexedCell<?> cell : cells) {
            assertEquals(-1, cell.getIndex());
        }
    }

    @Test
    public void testRecreateCellsChangeShouldResetIndex() {
        List<IndexedCell<?>> cells = new ArrayList<>();

        flow = new VirtualFlowShim<>();
        flow.setVertical(true);
        flow.setCellFactory(p -> {
            CellStub cellStub = new CellStub(flow);
            cells.add(cellStub);
            return cellStub;
        });
        flow.setCellCount(100);
        flow.setViewportLength(100);
        flow.resize(100, 100);
        pulse();

        flow.recreateCells();

        List<IndexedCell<?>> currentCells = new ArrayList<>(cells);

        pulse();

        for (IndexedCell<?> cell : currentCells) {
            assertEquals(-1, cell.getIndex());
        }
    }

    private ArrayLinkedListShim<GraphicalCellStub> circlelist = new ArrayLinkedListShim<>();

    private VirtualFlowShim createCircleFlow() {
        // The second VirtualFlow we are going to test, with 7 cells. Each cell
        // contains a Circle with a radius that varies between cells.
        VirtualFlowShim<IndexedCell> circleFlow;
        circleFlow = new VirtualFlowShim();

        circleFlow.setVertical(true);
        circleFlow.setCellFactory(p -> new GraphicalCellStub() {
            @Override
            protected double computeMinWidth(double height) {
                return computePrefWidth(height);
            }

            @Override
            protected double computeMaxWidth(double height) {
                return computePrefWidth(height);
            }

            @Override
            protected double computePrefWidth(double height) {
                return super.computePrefWidth(height);
            }

            @Override
            protected double computeMinHeight(double width) {
                return computePrefHeight(width);
            }

            @Override
            protected double computeMaxHeight(double width) {
                return computePrefHeight(width);
            }

        });
        circleFlow.setCellCount(7);
        circleFlow.resize(300, 300);
        circleFlow.layout();
        circleFlow.layout();
        return circleFlow;
    }

    // when moving the flow in one direction, the position of the flow
    // should not increase in the opposite direction
    @Test public void testReverseOrder() {
        double orig = flow.getPosition();
        flow.scrollPixels(10);
        double pos = flow.getPosition();
        assertFalse(pos < orig, "Moving in positive direction should not decrease position");
        flow.scrollPixels(-50);
        double neg = flow.getPosition();
        assertFalse(neg > pos, "Moving in negative direction should not increase position");
    }

    @Test public void testReverseOrderForCircleFlow() {
        VirtualFlowShim vf = createCircleFlow();
        double orig = vf.getPosition();
        vf.scrollPixels(10);
        double pos = vf.getPosition();
        assertFalse(pos < orig, "Moving in positive direction should not decrease position");
        vf.scrollPixels(-50);
        double neg = vf.getPosition();
        assertFalse(neg > pos, "Moving in negative direction should not increase position");
    }

    @Test public void testGradualMoveForCircleFlow() {
        VirtualFlowShim vf = createCircleFlow();
        vf.resize(600,400);
        ScrollBar sb = vf.shim_getVbar();
        double s0 = sb.getLayoutY();
        double s1 = s0;
        double position = vf.getPosition();
        double newPosition = 0d;
        double delta = 0;
        double newDelta = 0;
        vf.layout();
        for (int i = 0; i < 50; i++) {
            vf.scrollPixels(10);
            vf.layout();
            newPosition = vf.getPosition();
            s1 = sb.getLayoutY();
            newDelta = newPosition - position;
            // System.err.println("s0 = "+s0+", s1 = "+s1);
            // System.err.println("newDelta = "+newDelta+", delta = "+delta);
            if (i > 0) {
                double diff = Math.abs((newDelta-delta)/newDelta);
                // System.err.println("diff = "+diff);
                // maximum 10% difference allowed
                assertTrue(diff < 0.1, "Too much variation while scrolling (from "+s0+" to "+s1+")");
            }
            // System.err.println("S1 = "+s1);
            // System.err.println("pos = "+vf.getPosition());
            assertFalse(s1 < s0, "Thumb moving in the wrong direction at index ");
            s0 = s1;
            delta = newDelta;
            position = newPosition;
        }
    }

    @Test public void testAccumCellInvisible() {
        VirtualFlowShim vf = createCircleFlow();
        Node cell = vf.get_accumCell();
        if (cell != null) assertFalse(cell.isVisible());
        vf.resize(600,400);
        for (int i = 0; i < 50; i++) {
            vf.scrollPixels(1);
            cell = vf.get_accumCell();
            if (cell != null) assertFalse(cell.isVisible());
        }
    }

    @Test public void testScrollBarClipSyncWhileInvisibleOrNoScene() {
        flow.setCellCount(3);
        flow.resize(50, flow.getHeight());
        pulse();

        flow.setVisible(true);
        Scene scene = new Scene(flow);
        // sync works with both scene in place and flow visible
        assertEquals(flow.shim_getHbar().getValue(), flow.get_clipView_getX(), 0);
        flow.shim_getHbar().setValue(42);
        assertEquals(flow.shim_getHbar().getValue(), flow.get_clipView_getX(), 0);

        // sync works with flow invisible
        flow.setVisible(false);
        flow.shim_getHbar().setValue(21);
        flow.setVisible(true);
        assertEquals(flow.shim_getHbar().getValue(), flow.get_clipView_getX(), 0);

        // sync works with no scene
        scene.setRoot(new HBox());
        assertEquals(null, flow.getScene());
        flow.shim_getHbar().setValue(10);
        scene.setRoot(flow);
        assertEquals(flow.shim_getHbar().getValue(), flow.get_clipView_getX(), 0);
    }

    @Test public void testChangingCellSize() {
        int[] heights = {100, 100, 100, 100, 100, 100, 100, 100, 100};
        VirtualFlowShim<IndexedCell> flow = new VirtualFlowShim();
        flow.setVertical(true);
        flow.setCellFactory(p -> new CellStub(flow) {
            @Override public void updateIndex(int i) {
                super.updateIndex(i);
                if ((i > -1) &&(i < heights.length)){
                    this.setPrefHeight(heights[i]);
                }
            }
           @Override public void updateItem(Object ic, boolean empty) {
               super.updateItem(ic, empty);
               if (ic instanceof Integer) {
                   Integer idx = (Integer)ic;
                   if (idx > -1) {
                       this.setMinHeight(heights[idx]);
                       this.setPrefHeight(heights[idx]);
                   }
               }
            }
        });
        flow.setCellCount(heights.length);
        flow.setViewportLength(400);
        flow.resize(400, 400);
        flow.layout();
IndexedCell firstCell = VirtualFlowShim.cells_getFirst(flow.cells);
        // Before scrolling, top-cell must have index 0
assertEquals(0, firstCell.getIndex());
        // We now scroll to item with index 3
        flow.scrollToTop(3);
        flow.layout();
        firstCell = VirtualFlowShim.cells_getFirst(flow.cells);
        // After scrolling, top-cell must have index 3
        // index(pixel);
        // 3 (0); 4 (100); 5 (200); 6 (300)
        assertEquals(3, firstCell.getIndex());
        IndexedCell thirdCell = VirtualFlowShim.cells_get(flow.cells, 3);
        double l3y = thirdCell.getLayoutY();
        // the third visible cell must be at 3 x 100 = 300
        assertEquals(l3y, 300, 0.1);
        assertEquals(6, thirdCell.getIndex());
        assertEquals(300, thirdCell.getLayoutY(), 1.);


        for (int i = 0 ; i < heights.length; i++) {
            heights[i] = 220;
            flow.setCellDirty(i);
        }
        flow.setCellCount(heights.length);
        flow.layout();
        firstCell = VirtualFlowShim.cells_get(flow.cells, 0);
        // After resizing, top-cell must still have index 3
        assertEquals(3, firstCell.getIndex());
        assertEquals(0, firstCell.getLayoutY(),1);
        IndexedCell secondCell = VirtualFlowShim.cells_get(flow.cells, 1);
        assertEquals(4, secondCell.getIndex());
        assertEquals(220, secondCell.getLayoutY(),1);
        // And now scroll down 10 pixels
        flow.scrollPixels(10);
        flow.layout();
        firstCell = VirtualFlowShim.cells_get(flow.cells, 0);
        // After resizing, top-cell must still have index 3
        assertEquals(3, firstCell.getIndex());
        assertEquals(-10, firstCell.getLayoutY(),1);
    }

    /**
     * The VirtualFlow should never call the compute height methods when a fixed cell size is set.
     * If it is called the height will be wrong for some cells.
     */
    @Test
    public void testComputeHeightShouldNotBeUsedWhenFixedCellSizeIsSet() {
        int cellSize = 24;

        flow = new VirtualFlowShim<>();
        flow.setFixedCellSize(cellSize);
        flow.setCellFactory(p -> new CellStub(flow) {

            @Override
            protected double computeMinHeight(double width) {
                return 1337;
            }

            @Override
            protected double computeMaxHeight(double width) {
                return 1337;
            }

            @Override
            protected double computePrefHeight(double width) {
                return 1337;
            }
        });
        flow.setCellCount(100);
        flow.resize(cellSize * 10, cellSize * 10);

        pulse();
        pulse();

        for (int i = 0; i < 10; i++) {
            IndexedCell<?> cell = flow.getCell(i);
            double cellPosition = flow.getCellPosition(cell);
            int expectedPosition = i * cellSize;
            assertEquals(expectedPosition, cellPosition, 0d);
            assertEquals(cellSize, cell.getHeight(), 0d);

            assertNotEquals(cellSize, cell.getWidth(), 0d);
        }

        flow.scrollPixels(cellSize * 10);

        for (int i = 10; i < 20; i++) {
            IndexedCell<?> cell = flow.getCell(i);
            double cellPosition = flow.getCellPosition(cell);
            int expectedPosition = (i - 10) * cellSize;
            assertEquals(expectedPosition, cellPosition, 0d);
            assertEquals(cellSize, cell.getHeight(), 0d);

            assertNotEquals(cellSize, cell.getWidth(), 0d);
        }
    }

    /**
     * The VirtualFlow should never call the compute width methods when a fixed cell size is set.
     * If it is called the width will be wrong for some cells.
     */
    @Test
    public void testComputeWidthShouldNotBeUsedWhenFixedCellSizeIsSet() {
        int cellSize = 24;

        flow = new VirtualFlowShim<>();
        flow.setVertical(false);
        flow.setFixedCellSize(cellSize);
        flow.setCellFactory(p -> new CellStub(flow) {

            @Override
            protected double computeMinWidth(double height) {
                return 1337;
            }

            @Override
            protected double computeMaxWidth(double height) {
                return 1337;
            }

            @Override
            protected double computePrefWidth(double height) {
                return 1337;
            }
        });
        flow.setCellCount(100);
        flow.resize(cellSize * 10, cellSize * 10);

        pulse();
        pulse();

        for (int i = 0; i < 10; i++) {
            IndexedCell<?> cell = flow.getCell(i);
            double cellPosition = flow.getCellPosition(cell);
            int expectedPosition = i * cellSize;
            assertEquals(expectedPosition, cellPosition, 0d);
            assertEquals(cellSize, cell.getWidth(), 0d);

            assertNotEquals(cellSize, cell.getHeight(), 0d);
        }

        flow.scrollPixels(cellSize * 10);

        for (int i = 10; i < 20; i++) {
            IndexedCell<?> cell = flow.getCell(i);
            double cellPosition = flow.getCellPosition(cell);
            int expectedPosition = (i - 10) * cellSize;
            assertEquals(expectedPosition, cellPosition, 0d);
            assertEquals(cellSize, cell.getWidth(), 0d);

            assertNotEquals(cellSize, cell.getHeight(), 0d);
        }
    }

    /**
     * The VirtualFlow should never call the compute height methods when a fixed cell size is set.
     */
    @Test
    public void testComputeHeightShouldNotBeCalledWhenFixedCellSizeIsSet() {
        int cellSize = 24;

        flow = new VirtualFlowShim<>();
        flow.setFixedCellSize(cellSize);
        flow.setCellFactory(p -> new CellStub(flow) {

            @Override
            protected double computeMinHeight(double width) {
                fail();
                return 1337;
            }

            @Override
            protected double computeMaxHeight(double width) {
                fail();
                return 1337;
            }

            @Override
            protected double computePrefHeight(double width) {
                fail();
                return 1337;
            }
        });
        flow.setCellCount(100);
        flow.resize(cellSize * 10, cellSize * 10);

        // Trigger layout and see if the computeXXX method are called above.
        pulse();
        pulse();
    }

    /**
     * The VirtualFlow should never call the compute width methods when a fixed cell size is set.
     */
    @Test
    public void testComputeWidthShouldNotBeCalledWhenFixedCellSizeIsSet() {
        int cellSize = 24;

        flow = new VirtualFlowShim<>();
        flow.setVertical(false);
        flow.setFixedCellSize(cellSize);
        flow.setCellFactory(p -> new CellStub(flow) {

            @Override
            protected double computeMinWidth(double height) {
                fail();
                return 1337;
            }

            @Override
            protected double computeMaxWidth(double height) {
                fail();
                return 1337;
            }

            @Override
            protected double computePrefWidth(double height) {
                fail();
                return 1337;
            }
        });
        flow.setCellCount(100);
        flow.resize(cellSize * 10, cellSize * 10);

        // Trigger layout and see if the computeXXX method are called above.
        pulse();
        pulse();
    }

    @Test
    public void testLowerCellCount() {
        flow.setCellCount(10000);
        int idx = flow.shim_computeCurrentIndex();
        assertEquals(0, idx);

        assertTrue(prefSizeCounter < 500);
        int cntr = prefSizeCounter;
        flow.scrollTo(9999);
        pulse();
        idx = flow.shim_computeCurrentIndex();
        assertTrue(idx < 10000);
        int newCounter = prefSizeCounter - cntr;
        assertTrue(newCounter < 100);
        cntr = prefSizeCounter;

        flow.setCellCount(5000);
        idx = flow.shim_computeCurrentIndex();
        assertTrue(idx < 5000);
        newCounter = prefSizeCounter - cntr;
        assertTrue(newCounter < 100);
        cntr = prefSizeCounter;

        pulse();
        idx = flow.shim_computeCurrentIndex();
        assertTrue(idx < 5000);
        newCounter = prefSizeCounter - cntr;

        assertTrue(newCounter < 100);

    }

    @Test
    public void testAddCellWithBigCurrentOne() {
        int idx = flow.shim_computeCurrentIndex();
        assertEquals(0, idx);
        for (int i = 0; i < 20; i++) {
            flow.scrollPixels(40);
            pulse();
        }
        pulse();
        idx = flow.shim_computeCurrentIndex();
        assertEquals(29, idx);
        flow.setCellCount(101);
        pulse();
        idx = flow.shim_computeCurrentIndex();
        assertEquals(29, idx);
    }

    /**
     * Scrolling via the trough (-> {@link com.sun.javafx.scene.control.VirtualScrollBar#adjustValue(double)}) should
     * not throw any exception.
     * This happened in the past when scrolling up (more) when we already only see the uppermost cell with the index 0.
     * This index was subtracted by 1, leading to an {@link IndexOutOfBoundsException}.
     *
     * @see <a href="https://bugs.openjdk.org/browse/JDK-8311983">JDK-8311983</a>
     */
    @Test
    public void testScrollBarValueAdjustmentShouldNotThrowIOOBE() {
        flow = new VirtualFlowShim<>();
        flow.setFixedCellSize(512);
        flow.setCellFactory(fw -> new CellStub(flow));
        flow.setCellCount(2);
        flow.resize(250, 300);

        pulse();

        // Scroll down.
        flow.shim_getVbar().adjustValue(0.9605263157894737);
        // Scroll up.
        flow.shim_getVbar().adjustValue(0.05263157894736842);

        // This should not throw any exception. It used to throw an IndexOutOfBoundsException.
        flow.shim_getVbar().adjustValue(0.05263157894736842);
    }

    @Test
    public void testScrollBarValueAdjustmentShouldScrollOneDown() {
        flow = new VirtualFlowShim<>();
        flow.setFixedCellSize(512);
        flow.setCellFactory(fw -> new CellStub(flow));
        flow.setCellCount(5);
        flow.resize(250, 300);

        pulse();

        assertEquals(0, flow.getLastVisibleCell().getIndex());

        // Scroll down.
        flow.shim_getVbar().adjustValue(1);
        pulse();

        assertEquals(1, flow.getLastVisibleCell().getIndex());
    }

    @Test
    public void testScrollBarValueAdjustmentShouldScrollOneUp() {
        flow = new VirtualFlowShim<>();
        flow.setFixedCellSize(512);
        flow.setCellFactory(fw -> new CellStub(flow));
        flow.setCellCount(5);
        flow.resize(250, 300);

        pulse();

        assertEquals(0, flow.getFirstVisibleCell().getIndex());

        // Scroll completely down.
        flow.shim_getVbar().setValue(1.0);
        pulse();

        assertEquals(4, flow.getFirstVisibleCell().getIndex());

        // Scroll up.
        flow.shim_getVbar().adjustValue(0.0);
        pulse();

        assertEquals(3, flow.getFirstVisibleCell().getIndex());
    }

    @Test
    public void testScrollBarValueAdjustmentMovementUp() {
        testScrollBarValueAdjustment(1, 1.0, 0.2, () -> -flow.getViewportLength());
        testScrollBarValueAdjustment(3, 1.0, 0.2, () -> -flow.getViewportLength());
        testScrollBarValueAdjustment(1, 0.5, 0.2, () -> -flow.getViewportLength());
        testScrollBarValueAdjustment(3, 0.5, 0.2, () -> -flow.getViewportLength());
    }

    @Test
    public void testScrollBarValueAdjustmentMovementDown() {
        testScrollBarValueAdjustment(1, 0.0, 0.8, () -> flow.getViewportLength());
        testScrollBarValueAdjustment(3, 0.0, 0.8, () -> flow.getViewportLength());
        testScrollBarValueAdjustment(1, 0.5, 0.8, () -> flow.getViewportLength());
        testScrollBarValueAdjustment(3, 0.5, 0.8, () -> flow.getViewportLength());
    }

    public void testScrollBarValueAdjustment(int cellCount, double position, double adjust, DoubleSupplier targetMovement) {
        flow = new VirtualFlowShim<>();
        class C extends CellStub {
            public C(VirtualFlowShim flow) {
                super(flow);
            }

            @Override
            protected double computePrefHeight(double width) {
                return getIndex() == 0 ? 1000 : 100;
            }

            @Override
            protected double computeMinHeight(double width) {
                return computePrefHeight(width);
            }

            @Override
            protected double computeMaxHeight(double width) {
                return computePrefHeight(width);
            }
        }
        flow.setCellFactory(fw -> new C(flow));
        flow.setCellCount(cellCount);
        flow.resize(256, 200);

        flow.setPosition(position);
        pulse();

        Supplier<double[]> cellPositionsCalculater = () -> {
            double[] positions = new double[cellCount];
            IndexedCell<?> cell = flow.getFirstVisibleCell();
            positions[cell.getIndex()] = flow.getCellPosition(cell);
            for (int i = cell.getIndex() + 1; i < cellCount; i++) {
                positions[i] = positions[i - 1] + flow.getCellSize(i - 1);
            }
            for (int i = cell.getIndex() - 1; i >= 0; i--) {
                positions[i] = positions[i + 1] - flow.getCellSize(i);
            }
            return positions;
        };

        double[] positionsBefore = cellPositionsCalculater.get();

        flow.shim_getVbar().adjustValue(adjust);
        pulse();

        double[] positionsAfter = cellPositionsCalculater.get();

        for (int i = 0; i < positionsBefore.length; i++) {
            assertEquals(targetMovement.getAsDouble(), positionsBefore[i] - positionsAfter[i], 0.1);
        }
    }

    /**
     * The first cell should always be the same when scrolling down just a little bit.
     * This is mainly a regression test to check that no leading cells are added where they should not be.
     *
     * @see <a href="https://bugs.openjdk.org/browse/JDK-8316590">JDK-8316590</a>
     */
    @Test
    public void testFirstCellShouldBeTheSameOnScroll() {
        flow = new VirtualFlowShim<>();
        flow.setCellFactory(fw -> new CellStub(flow));
        flow.setCellCount(50);
        flow.resize(250, 300);

        pulse();

        IndexedCell<?> cell1 = flow.cells_get(flow.cells, 0);

        flow.scrollPixels(0.01);
        IndexedCell<?> cell2 = flow.cells_get(flow.cells, 0);

        assertSame(cell1, cell2);
    }

    @Test
    public void testFirstCellShouldHaveTheSameIndexOnScroll() {
        flow = new VirtualFlowShim<>();
        flow.setCellFactory(fw -> new CellStub(flow));
        flow.setCellCount(50);
        flow.resize(250, 300);

        pulse();

        IndexedCell<?> cell1 = flow.cells_get(flow.cells, 0);
        assertEquals(0, cell1.getIndex());

        flow.scrollPixels(0.01);

        assertEquals(0, cell1.getIndex());
    }

    /**
     * The first cell should always be at the same position (when visible), no matter if we scroll down or not.
     *
     * @see <a href="https://bugs.openjdk.org/browse/JDK-8316590">JDK-8316590</a>
     */
    @Test
    public void testFirstCellShouldBeAtPosition0OnScroll() {
        flow = new VirtualFlowShim<>();
        flow.setCellFactory(fw -> new CellStub(flow));
        flow.setCellCount(50);
        flow.resize(250, 300);

        pulse();

        IndexedCell<?> cell1 = flow.cells_get(flow.cells, 0);
        assertEquals(0.0, cell1.getLayoutY(), 0);

        flow.scrollPixels(0.01);

        assertEquals(0.0, cell1.getLayoutY(), 0);
    }

    @Test
    public void testScrollingXIsSnapped() {
        StageLoader loader = new StageLoader(flow);
        flow.resize(50, flow.getHeight());

        pulse();

        double newValue = 25.125476811;
        double snappedNewValue = flow.snapPositionX(newValue);
        flow.shim_getHbar().setValue(newValue);

        double layoutX = flow.get_clipView().getLayoutX();

        assertEquals(-snappedNewValue, layoutX, 0.0);

        loader.dispose();
    }

    @Test
    public void testScrollingYIsSnapped() {
        flow.setVertical(false);

        StageLoader loader = new StageLoader(flow);
        flow.resize(50, flow.getHeight());

        pulse();

        double newValue = 25.125476811;
        double snappedNewValue = flow.snapPositionY(newValue);
        flow.shim_getVbar().setValue(newValue);

        double layoutY = flow.get_clipView().getLayoutY();

        assertEquals(-snappedNewValue, layoutY, 0.0);

        loader.dispose();
    }

    @Test
    public void testSheetChildrenAreAlwaysTheAmountOfVisibleCells() {
        flow = new VirtualFlowShim<>();
        flow.setFixedCellSize(24);
        flow.setCellFactory(fw -> new CellStub(flow));
        flow.setCellCount(20);

        flow.resize(250, 240);
        pulse();

        assertEquals(10, flow.sheetChildren.size());
        assertEquals(flow.cells, flow.sheetChildren);

        flow.resize(250, 480);
        pulse();

        assertEquals(20, flow.sheetChildren.size());
        assertEquals(flow.cells, flow.sheetChildren);

        flow.resize(250, 240);
        pulse();

        assertEquals(10, flow.sheetChildren.size());
        assertEquals(flow.cells, flow.sheetChildren);
    }

}

class GraphicalCellStub extends IndexedCellShim<Node> {
    static List<Circle> circleList = List.of(
        new Circle(10),
        new Circle(20),
        new Circle(100),
        new Circle(30),
        new Circle(50),
        new Circle(200),
        new Circle(60)
    );

    private int idx = -1;
    Node myItem = null;

    public GraphicalCellStub() { init(); }

    private void init() {
        // System.err.println("Init vf cell "+this);
        setSkin(new SkinStub<>(this));
    }

    @Override
    public void updateItem(Node item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            setGraphic(item);
        }
    }

    @Override
    public void updateIndex(int i) {
        super.updateIndex(i);
        if ((i > -1) && (circleList.size() > i)) {
            this.idx = i;
            updateItem(circleList.get(i), false);
        } else {
            updateItem(null, true);
        }
    }

    @Override
    protected double computePrefHeight(double width) {
        double answer = super.computePrefHeight(width);
        if ((idx > -1) && (idx < circleList.size())) {
            answer = 2 * circleList.get(idx).getRadius() + 6;
        }
        return answer;
    }

    @Override
    public String toString() {
        return "GraphicCell with item = "+myItem+" at "+super.toString();
    }
}

class CellStub extends IndexedCellShim {
    String s;
    VirtualFlowShim flow;

    public CellStub(VirtualFlowShim flow) { init(flow); }
    public CellStub(VirtualFlowShim flow, String s) { init(flow); this.s = s; }

    private void init(VirtualFlowShim flow) {
        this.flow = flow;
        setSkin(new SkinStub<>(this));
        updateItem(this, false);
    }

    @Override
    public void updateIndex(int i) {
        super.updateIndex(i);

        s = "Item " + getIndex();
        updateItem(getIndex(), getIndex() >= flow.getCellCount());
    }
}
