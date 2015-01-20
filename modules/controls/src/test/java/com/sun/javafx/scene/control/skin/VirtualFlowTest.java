/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.LinkedList;

import javafx.beans.InvalidationListener;
import javafx.event.Event;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.SkinStub;
import javafx.scene.input.ScrollEvent;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.javafx.scene.CssFlags;
import com.sun.javafx.scene.control.skin.VirtualFlow.ArrayLinkedList;
import java.util.List;
import javafx.util.Callback;

/**
 * Tests for the VirtualFlow class. VirtualFlow is the guts of the ListView,
 * TreeView, and TableView implementations.
 */
public class VirtualFlowTest {
    // The following 4 vars are used when testing the
    private ArrayLinkedList<CellStub> list;
    private CellStub a;
    private CellStub b;
    private CellStub c;

    // The VirtualFlow we are going to test. By default, there are 100 cells
    // and each cell is 100 wide and 25 tall, except for the 30th cell, which
    // is 200 wide and 100 tall.
    private VirtualFlow<IndexedCell> flow;
//    private Scene scene;

    @Before public void setUp() {
        list = new ArrayLinkedList<CellStub>();
        a = new CellStub(flow, "A");
        b = new CellStub(flow, "B");
        c = new CellStub(flow, "C");

        flow = new VirtualFlow();
//        flow.setManaged(false);
        flow.setVertical(true);
        flow.setCreateCell(p -> new CellStub(flow) {
            @Override protected double computeMinWidth(double height) { return computePrefWidth(height); }
            @Override protected double computeMaxWidth(double height) { return computePrefWidth(height); }
            @Override protected double computePrefWidth(double height) {
                return flow.isVertical() ? (c.getIndex() == 29 ? 200 : 100) : (c.getIndex() == 29 ? 100 : 25);
            }

            @Override protected double computeMinHeight(double width) { return computePrefHeight(width); }
            @Override protected double computeMaxHeight(double width) { return computePrefHeight(width); }
            @Override protected double computePrefHeight(double width) {
                return flow.isVertical() ? (c.getIndex() == 29 ? 100 : 25) : (c.getIndex() == 29 ? 200 : 100);
            }
        });
        flow.setCellCount(100);
        flow.resize(300, 300);
        pulse();
    }

    private void pulse() {
//        flow.impl_processCSS(true);
        flow.layout();
    }

    /**
     * Asserts that the items in the control LinkedList and the ones in the
     * list are exactly the same.
     */
    private void assertMatch(List<IndexedCell> control, ArrayLinkedList<IndexedCell> list) {
        assertEquals("The control and list did not have the same sizes. " +
                     "Expected " + control.size() + " but was " + list.size(),
                     control.size(), list.size());
        int index = 0;
        Iterator<IndexedCell> itr = control.iterator();
        while (itr.hasNext()) {
            IndexedCell cell = itr.next();
            IndexedCell cell2 = list.get(index);
            assertSame("The control and list did not have the same item at " +
                       "index " + index + ". Expected " + cell + " but was " + cell2,
                       cell, cell2);
            index++;
        }
    }

    /**
     * Asserts that only the minimal number of cells are used.
     */
    public <T extends IndexedCell> void assertMinimalNumberOfCellsAreUsed(VirtualFlow<T> flow) {
        pulse();
        IndexedCell firstCell = flow.cells.getFirst();
        IndexedCell lastCell = flow.cells.getLast();
        if (flow.isVertical()) {
            // First make sure that enough cells were created
            assertTrue("There is a gap between the top of the viewport and the first cell",
                       firstCell.getLayoutY() <= 0);
            assertTrue("There is a gap between the bottom of the last cell and the bottom of the viewport",
                       lastCell.getLayoutY() + lastCell.getHeight() >= flow.getViewportLength());

            // Now make sure that no extra cells were created.
            if (flow.cells.size() > 3) {
                IndexedCell secondLastCell = flow.cells.get(flow.cells.size() - 2);
                IndexedCell secondCell = flow.cells.get(1);
                assertFalse("There are more cells created before the start of " +
                            "the flow than necessary",
                            secondCell.getLayoutY() <= 0);
                assertFalse("There are more cells created after the end of the " +
                            "flow than necessary",
                            secondLastCell.getLayoutY() + secondLastCell.getHeight() >= flow.getViewportLength());
            }
        } else {
            // First make sure that enough cells were created
            assertTrue("There is a gap between the left of the viewport and the first cell",
                       firstCell.getLayoutX() <= 0);
            assertTrue("There is a gap between the right of the last cell and the right of the viewport",
                       lastCell.getLayoutX() + lastCell.getWidth() >= flow.getViewportLength());

            // Now make sure that no extra cells were created.
            if (flow.cells.size() > 3) {
                IndexedCell secondLastCell = flow.cells.get(flow.cells.size() - 2);
                IndexedCell secondCell = flow.cells.get(1);
                assertFalse("There are more cells created before the start of " +
                            "the flow than necessary",
                            secondCell.getLayoutX() <= 0);
                assertFalse("There are more cells created after the end of the " +
                            "flow than necessary",
                            secondLastCell.getLayoutX() + secondLastCell.getWidth() >= flow.getViewportLength());
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

    ////////////////////////////////////////////////////////////////////////////
    //
    //  General Layout
    //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * In this test there are no cells. The VirtualFlow should be laid out such
     * that the scroll bars and corner are not visible, and the clip view fills
     * the entire width/height of the VirtualFlow.
     */
    @Test public void testGeneralLayout_NoCells() {
        flow.setCellCount(0);
        pulse();
        assertFalse("The hbar should have been invisible", flow.getHbar().isVisible());
        assertFalse("The vbar should have been invisible", flow.getVbar().isVisible());
        assertFalse("The corner should have been invisible", flow.corner.isVisible());
        assertEquals(flow.getWidth(), flow.clipView.getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.clipView.getHeight(), 0.0);
        assertMinimalNumberOfCellsAreUsed(flow);

        flow.setVertical(false);
        pulse();
        assertFalse("The hbar should have been invisible", flow.getHbar().isVisible());
        assertFalse("The vbar should have been invisible", flow.getVbar().isVisible());
        assertFalse("The corner should have been invisible", flow.corner.isVisible());
        assertEquals(flow.getWidth(), flow.clipView.getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.clipView.getHeight(), 0.0);
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
        assertFalse("The hbar should have been invisible", flow.getHbar().isVisible());
        assertFalse("The vbar should have been invisible", flow.getVbar().isVisible());
        assertFalse("The corner should have been invisible", flow.corner.isVisible());
        assertEquals(flow.getWidth(), flow.clipView.getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.clipView.getHeight(), 0.0);
        assertEquals(12, flow.cells.size()); // we stil have 12 cells (300px / 25px), even if only three filled cells exist
        assertMinimalNumberOfCellsAreUsed(flow);

        flow.setVertical(false);
        pulse();
        assertFalse("The hbar should have been invisible", flow.getHbar().isVisible());
        assertFalse("The vbar should have been invisible", flow.getVbar().isVisible());
        assertFalse("The corner should have been invisible", flow.corner.isVisible());
        assertEquals(flow.getWidth(), flow.clipView.getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.clipView.getHeight(), 0.0);
//        assertEquals(3, flow.cells.size());
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
        assertTrue("The hbar should have been visible", flow.getHbar().isVisible());
        assertFalse("The vbar should have been invisible", flow.getVbar().isVisible());
        assertFalse("The corner should have been invisible", flow.corner.isVisible());
        assertEquals(flow.getWidth(), flow.clipView.getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.clipView.getHeight() + flow.getHbar().getHeight(), 0.0);
        assertEquals(flow.getHbar().getLayoutY(), flow.getHeight() - flow.getHbar().getHeight(), 0.0);
        assertMinimalNumberOfCellsAreUsed(flow);

        flow.setVertical(false);
        flow.resize(300, 50);
        pulse();
        assertFalse("The hbar should have been invisible", flow.getHbar().isVisible());
        assertTrue("The vbar should have been visible", flow.getVbar().isVisible());
        assertFalse("The corner should have been invisible", flow.corner.isVisible());
        assertEquals(flow.getWidth(), flow.clipView.getWidth() + flow.getVbar().getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.clipView.getHeight(), 0.0);
        assertEquals(flow.getVbar().getLayoutX(), flow.getWidth() - flow.getVbar().getWidth(), 0.0);
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
        assertFalse("The hbar should have been invisible", flow.getHbar().isVisible());
        assertFalse("The vbar should have been invisible", flow.getVbar().isVisible());
        assertFalse("The corner should have been invisible", flow.corner.isVisible());
        assertEquals(flow.getWidth(), flow.clipView.getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.clipView.getHeight(), 0.0);
        assertMinimalNumberOfCellsAreUsed(flow);

        flow.setVertical(false);
        flow.resize(300, 50);
        pulse();
        flow.resize(flow.getWidth(), 300);
        pulse();
        assertFalse("The hbar should have been invisible", flow.getHbar().isVisible());
        assertFalse("The vbar should have been invisible", flow.getVbar().isVisible());
        assertFalse("The corner should have been invisible", flow.corner.isVisible());
        assertEquals(flow.getWidth(), flow.clipView.getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.clipView.getHeight(), 0.0);
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
        assertFalse("The hbar should have been invisible", flow.getHbar().isVisible());
        assertTrue("The vbar should have been visible", flow.getVbar().isVisible());
        assertFalse("The corner should have been invisible", flow.corner.isVisible());
        assertEquals(flow.getWidth(), flow.clipView.getWidth() + flow.getVbar().getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.clipView.getHeight(), 0.0);
        assertEquals(flow.getVbar().getLayoutX(), flow.getWidth() - flow.getVbar().getWidth(), 0.0);
        assertMinimalNumberOfCellsAreUsed(flow);

        flow.setVertical(false);
        pulse();
        assertTrue("The hbar should have been visible", flow.getHbar().isVisible());
        assertFalse("The vbar should have been invisible", flow.getVbar().isVisible());
        assertFalse("The corner should have been invisible", flow.corner.isVisible());
        assertEquals(flow.getWidth(), flow.clipView.getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.clipView.getHeight() + flow.getHbar().getHeight(), 0.0);
        assertEquals(flow.getHbar().getLayoutY(), flow.getHeight() - flow.getHbar().getHeight(), 0.0);
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
        assertFalse("The hbar should have been invisible", flow.getHbar().isVisible());
        assertTrue("The vbar should have been visible", flow.getVbar().isVisible());
        assertFalse("The corner should have been invisible", flow.corner.isVisible());
        assertEquals(flow.getWidth(), flow.clipView.getWidth() + flow.getVbar().getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.clipView.getHeight(), 0.0);
        assertEquals(flow.getVbar().getLayoutX(), flow.getWidth() - flow.getVbar().getWidth(), 0.0);
        assertMinimalNumberOfCellsAreUsed(flow);

        flow.setVertical(false);
        flow.setCellCount(3);
        pulse();
        flow.setCellCount(100);
        pulse();
        assertTrue("The hbar should have been visible", flow.getHbar().isVisible());
        assertFalse("The vbar should have been invisible", flow.getVbar().isVisible());
        assertFalse("The corner should have been invisible", flow.corner.isVisible());
        assertEquals(flow.getWidth(), flow.clipView.getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.clipView.getHeight() + flow.getHbar().getHeight(), 0.0);
        assertEquals(flow.getHbar().getLayoutY(), flow.getHeight() - flow.getHbar().getHeight(), 0.0);
        assertMinimalNumberOfCellsAreUsed(flow);
    }

    /**
     * Test the case where there are many cells and they are wider than the
     * viewport. We should have the hbar, vbar, and corner region in this case.
     */
    @Test public void testGeneralLayout_ManyCellsAndWide() {
        flow.resize(50, flow.getHeight());
        pulse();
        assertTrue("The hbar should have been visible", flow.getHbar().isVisible());
        assertTrue("The vbar should have been visible", flow.getVbar().isVisible());
        assertTrue("The corner should have been visible", flow.corner.isVisible());
        assertEquals(flow.getWidth(), flow.clipView.getWidth() + flow.getVbar().getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.clipView.getHeight() + flow.getHbar().getHeight(), 0.0);
        assertEquals(flow.getVbar().getLayoutX(), flow.getWidth() - flow.getVbar().getWidth(), 0.0);
        assertEquals(flow.getVbar().getWidth(), flow.corner.getWidth(), 0.0);
        assertEquals(flow.getHbar().getHeight(), flow.corner.getHeight(), 0.0);
        assertEquals(flow.getHbar().getWidth(), flow.getWidth() - flow.corner.getWidth(), 0.0);
        assertEquals(flow.getVbar().getHeight(), flow.getHeight() - flow.corner.getHeight(), 0.0);
        assertEquals(flow.corner.getLayoutX(), flow.getWidth() - flow.corner.getWidth(), 0.0);
        assertEquals(flow.corner.getLayoutY(), flow.getHeight() - flow.corner.getHeight(), 0.0);
        assertMinimalNumberOfCellsAreUsed(flow);

        flow.setVertical(false);
        flow.resize(300, 50);
        pulse();
        assertTrue("The hbar should have been visible", flow.getHbar().isVisible());
        assertTrue("The vbar should have been visible", flow.getVbar().isVisible());
        assertTrue("The corner should have been visible", flow.corner.isVisible());
        assertEquals(flow.getWidth(), flow.clipView.getWidth() + flow.getVbar().getWidth(), 0.0);
        assertEquals(flow.getHeight(), flow.clipView.getHeight() + flow.getHbar().getHeight(), 0.0);
        assertEquals(flow.getVbar().getLayoutX(), flow.getWidth() - flow.getVbar().getWidth(), 0.0);
        assertEquals(flow.getVbar().getWidth(), flow.corner.getWidth(), 0.0);
        assertEquals(flow.getHbar().getHeight(), flow.corner.getHeight(), 0.0);
        assertEquals(flow.getHbar().getWidth(), flow.getWidth() - flow.corner.getWidth(), 0.0);
        assertEquals(flow.getVbar().getHeight(), flow.getHeight() - flow.corner.getHeight(), 0.0);
        assertEquals(flow.corner.getLayoutX(), flow.getWidth() - flow.corner.getWidth(), 0.0);
        assertEquals(flow.corner.getLayoutY(), flow.getHeight() - flow.corner.getHeight(), 0.0);
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
        assertEquals(0, flow.getHbar().getMin(), 0.0);
        assertEquals(flow.getMaxPrefBreadth() - flow.clipView.getWidth(), flow.getHbar().getMax(), 0.0);
        assertEquals((flow.clipView.getWidth()/flow.getMaxPrefBreadth()) * flow.getHbar().getMax(), flow.getHbar().getVisibleAmount(), 0.0);
        flow.setPosition(.28f);
        pulse();
        assertEquals(0, flow.getHbar().getMin(), 0.0);
        assertEquals(flow.getMaxPrefBreadth() - flow.clipView.getWidth(), flow.getHbar().getMax(), 0.0);
        assertEquals((flow.clipView.getWidth()/flow.getMaxPrefBreadth()) * flow.getHbar().getMax(), flow.getHbar().getVisibleAmount(), 0.0);

        flow.setVertical(false);
        flow.setPosition(0);
        flow.resize(300, 50);
        pulse();
        assertEquals(0, flow.getVbar().getMin(), 0.0);
        assertEquals(flow.getMaxPrefBreadth() - flow.clipView.getHeight(), flow.getVbar().getMax(), 0.0);
        assertEquals((flow.clipView.getHeight()/flow.getMaxPrefBreadth()) * flow.getVbar().getMax(), flow.getVbar().getVisibleAmount(), 0.0);
        flow.setPosition(.28);
        pulse();
        assertEquals(0, flow.getVbar().getMin(), 0.0);
        assertEquals(flow.getMaxPrefBreadth() - flow.clipView.getHeight(), flow.getVbar().getMax(), 0.0);
        assertEquals((flow.clipView.getHeight()/flow.getMaxPrefBreadth()) * flow.getVbar().getMax(), flow.getVbar().getVisibleAmount(), 0.0);
    }

    /**
     * Tests that the maxPrefBreadth is computed correctly for the first page of cells.
     * In our test case, the first page of cells have a uniform pref.
     */
    @Test public void testGeneralLayout_maxPrefBreadth() {
        assertEquals(100, flow.getMaxPrefBreadth(), 0.0);
    }

    /**
     * Tests that even after the first computation of max pref, that it is
     * updated when we encounter a new cell (while scrolling for example) that
     * has a larger pref.
     */
    @Ignore
    @Test public void testGeneralLayout_maxPrefBreadthUpdatedWhenEncounterLargerPref() {
        flow.setPosition(.28);
        pulse();
        assertEquals(200, flow.getMaxPrefBreadth(), 0.0);
    }

    /**
     * Tests that if we encounter cells or pages of cells with smaller prefs
     * than the max pref that we will keep the max pref the same.
     */
    @Ignore
    @Test public void testGeneralLayout_maxPrefBreadthRemainsSameWhenEncounterSmallerPref() {
        flow.setPosition(.28);
        pulse();
        flow.setPosition(.8);
        pulse();
        assertEquals(200, flow.getMaxPrefBreadth(), 0.0);
    }

    /**
     * Tests that changes to the vertical property will clear the maxPrefBreadth
     */
    @Test public void testGeneralLayout_VerticalChangeClearsmaxPrefBreadth() {
        flow.setVertical(false);
        assertEquals(-1, flow.getMaxPrefBreadth(), 0.0);
    }

    /**
     * Tests that changes to the cell count will not affect maxPrefBreadth.
     */
    @Ignore
    @Test public void testGeneralLayout_maxPrefBreadthUnaffectedByCellCountChanges() {
        flow.setCellCount(10);
        pulse();
        assertEquals(100, flow.getMaxPrefBreadth(), 0.0);
        flow.setCellCount(100);
        pulse();
        flow.setPosition(.28);
        pulse();
        assertEquals(200, flow.getMaxPrefBreadth(), 0.0);
        flow.setCellCount(10);
        pulse();
        assertEquals(200, flow.getMaxPrefBreadth(), 0.0);
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
        flow.getHbar().setValue(30);
        pulse();
        flow.setPosition(.28);
        pulse();
        assertEquals(30, flow.getHbar().getValue(), 0.0);

        // Reset the test and this time check what happens when we are scrolled
        // to the very right
        flow.setPosition(0);
        flow.setVertical(false);
        flow.setVertical(true);
        pulse();
        assertEquals(100, flow.getMaxPrefBreadth(), 0.0);
        flow.getHbar().setValue(flow.getHbar().getMax()); // scroll to the end
        flow.setPosition(.28);
        pulse();
        assertEquals(flow.getHbar().getMax(), flow.getHbar().getValue(), 0.0);

        flow.setVertical(false);
        flow.setPosition(0);
        flow.getHbar().setValue(0);
        flow.resize(300, 50);
        pulse();
        flow.getVbar().setValue(30);
        pulse();
        flow.setPosition(.28);
        pulse();
        assertEquals(30, flow.getVbar().getValue(), 0.0);

        // Reset the test and this time check what happens when we are scrolled
        // to the very right
        flow.setPosition(0);
        flow.setVertical(true);
        flow.setVertical(false);
        pulse();
        assertEquals(100, flow.getMaxPrefBreadth(), 0.0);
        flow.getVbar().setValue(flow.getVbar().getMax()); // scroll to the end
        flow.setPosition(.28);
        pulse();
        assertEquals(flow.getVbar().getMax(), flow.getVbar().getValue(), 0.0);
    }

    @Test public void testGeneralLayout_ScrollToEndOfVirtual_BarStillVisible() {
        assertTrue("The vbar was expected to be visible", flow.getVbar().isVisible());
        flow.setPosition(1);
        pulse();
        assertTrue("The vbar was expected to be visible", flow.getVbar().isVisible());

        flow.setPosition(0);
        flow.setVertical(false);
        pulse();
        assertTrue("The hbar was expected to be visible", flow.getHbar().isVisible());
        flow.setPosition(1);
        pulse();
        assertTrue("The hbar was expected to be visible", flow.getHbar().isVisible());
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

    ////////////////////////////////////////////////////////////////////////////
    //
    //  Cell Layout
    //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Test to make sure that we are virtual -- that all cells are not being
     * created.
     */
    @Test public void testCellLayout_NotAllCellsAreCreated() {
        // due to the initial size of the VirtualFlow and the number of cells
        // and their heights, we should have more cells than we have space to
        // fit them and so only enough cells should be created to meet our
        // needs and not any more than that
        assertTrue("All of the cells were created", flow.cells.size() < flow.getCellCount());
        assertMinimalNumberOfCellsAreUsed(flow);
    }

    /**
     * Tests the size and position of all the cells to make sure they were
     * laid out properly.
     */
    @Test public void testCellLayout_CellSizes_AfterLayout() {
        double offset = 0.0;
        for (int i = 0; i < flow.cells.size(); i++) {
            IndexedCell cell = flow.cells.get(i);
            assertEquals(25, cell.getHeight(), 0.0);
            assertEquals(offset, cell.getLayoutY(), 0.0);
            offset += cell.getHeight();
        }

        offset = 0.0;
        flow.setVertical(false);
        pulse();
        for (int i = 0; i < flow.cells.size(); i++) {
            IndexedCell cell = flow.cells.get(i);
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
        double expected = flow.clipView.getWidth();
        for (int i = 0; i < flow.cells.size(); i++) {
            IndexedCell cell = flow.cells.get(i);
            assertEquals(expected, cell.getWidth(), 0.0);
        }

        flow.setVertical(false);
        pulse();
        expected = flow.clipView.getHeight();
        for (int i = 0; i < flow.cells.size(); i++) {
            IndexedCell cell = flow.cells.get(i);
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
        assertEquals(100, flow.getMaxPrefBreadth(), 0.0);
        for (int i = 0; i < flow.cells.size(); i++) {
            IndexedCell cell = flow.cells.get(i);
            assertEquals(flow.getMaxPrefBreadth(), cell.getWidth(), 0.0);
        }

        flow.setVertical(false);
        flow.resize(flow.getWidth(), 50);
        pulse();
        assertEquals(100, flow.getMaxPrefBreadth(), 0.0);
        for (int i = 0; i < flow.cells.size(); i++) {
            IndexedCell cell = flow.cells.get(i);
            assertEquals(flow.getMaxPrefBreadth(), cell.getHeight(), 0.0);
        }
    }

    /**
     * Test that when we scroll and encounter a cell which has a larger pref
     * than we have previously encountered (happens in this test when we visit
     * the cell for item #29), then the max pref is updated and the cells are
     * all resized to match.
     */
    @Ignore
    @Test public void testCellLayout_ScrollingFindsCellWithLargemaxPrefBreadth() {
        flow.resize(50, flow.getHeight());
        flow.setPosition(.28); // happens to position such that #29 is visible
        pulse();
        assertEquals(200, flow.getMaxPrefBreadth(), 0.0);
        for (int i = 0; i < flow.cells.size(); i++) {
            IndexedCell cell = flow.cells.get(i);
            assertEquals(flow.getMaxPrefBreadth(), cell.getWidth(), 0.0);
        }

        flow.setVertical(false);
        flow.resize(flow.getWidth(), 50);
        // NOTE Run this test without the pulse and it fails!
        pulse();
        flow.setPosition(.28);
        pulse();
        assertEquals(200, flow.getMaxPrefBreadth(), 0.0);
        for (int i = 0; i < flow.cells.size(); i++) {
            IndexedCell cell = flow.cells.get(i);
            assertEquals(flow.getMaxPrefBreadth(), cell.getHeight(), 0.0);
        }
    }

    /**
     * Checks that the initial set of cells (the first page of cells) are
     * indexed starting with cell #0 and working up from there.
     */
    @Test public void testCellLayout_CellIndexes_FirstPage() {
        for (int i = 0; i < flow.cells.size(); i++) {
            assertEquals(i, flow.cells.get(i).getIndex());
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
        List<IndexedCell> cells = new LinkedList<IndexedCell>();
        for (int i = 0; i < flow.cells.size(); i++) {
            cells.add(flow.cells.get(i));
        }
        assertMatch(cells, flow.cells); // sanity check
        flow.requestLayout();
        pulse();
        assertMatch(cells, flow.cells);
        flow.setPosition(1);
        pulse();
        cells.clear();
        for (int i = 0; i < flow.cells.size(); i++) {
            cells.add(flow.cells.get(i));
        }
        flow.requestLayout();
        pulse();
        assertMatch(cells, flow.cells);
    }


    @Test
    public void testCellLayout_BiasedCellAndLengthBar() {
        flow.setCreateCell(param -> new CellStub(flow) {
            @Override protected double computeMinWidth(double height) { return 0; }
            @Override protected double computeMaxWidth(double height) { return Double.MAX_VALUE; }
            @Override protected double computePrefWidth(double height) {
                return 200;
            }

            @Override protected double computeMinHeight(double width) { return 0; }
            @Override protected double computeMaxHeight(double width) { return Double.MAX_VALUE; }
            @Override protected double computePrefHeight(double width) {
                return getIndex() == 0 ? 100 - 5 *(Math.floorDiv((int)width - 200, 10)) : 100;
            }
        });
        flow.setCellCount(3);
        flow.recreateCells(); // This help to override layoutChildren() in flow.setCellCount()
        flow.getVbar().setPrefWidth(20); // Since Skins are not initialized, we set the pref width explicitly
        flow.requestLayout();
        pulse();
        assertEquals(300, flow.cells.get(0).getWidth(), 1e-100);
        assertEquals(50, flow.cells.get(0).getHeight(), 1e-100);

        flow.resize(200, 300);

        flow.requestLayout();
        pulse();
        assertEquals(200, flow.cells.get(0).getWidth(), 1e-100);
        assertEquals(100, flow.cells.get(0).getHeight(), 1e-100);

    }

    ////////////////////////////////////////////////////////////////////////////
    //
    //  Cell Life Cycle
    //
    ////////////////////////////////////////////////////////////////////////////

    @Test public void testCellLifeCycle_CellsAreCreatedOnLayout() {
        // when the flow was first created in setUp we do a layout()
        assertTrue("The cells didn't get created", flow.cells.size() > 0);
    }

//    /**
//     * During layout the order and contents of cells will change. We need
//     * to make sure that CSS for cells is applied at this time. To test this,
//     * I just set the position and perform a new pulse. Since layout happens
//     * after the CSS updates are applied, if the test fails, then there will
//     * be cells left in a state where they need their CSS applied.
//     */
//    @Test public void testCellLifeCycle_CSSUpdatesHappenDuringLayout() {
//        flow.setPosition(.35);
//        pulse();
//        for (int i = 0; i < flow.cells.size(); i++) {
//            IndexedCell cell = flow.cells.get(i);
//            assertEquals(CssFlags.CLEAN, cell.impl_getCSSFlags());
//        }
//    }

    ////////////////////////////////////////////////////////////////////////////
    //
    //  Position
    //
    ////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////
    //
    //  Pixel Scrolling
    //
    ////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////
    //
    //  Cell Count Changes
    //
    ////////////////////////////////////////////////////////////////////////////

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
////    @Test public void testCellCountChanges_SelectedRowRemoved() {
////
////    }
////
////    @Test public void testCellCountChanges_NonSelectedRowRemoved() {
////
////    }
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
////    @Test public void testCellCountChanges_RowIsAddedBeforeSelectedRow() {
////
////    }
////
////    @Test public void testCellCountChanges_RowIsAddedAfterSelectedRow() {
////
////    }

    ////////////////////////////////////////////////////////////////////////////
    //
    //  VirtualFlow State Changes
    //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Tests that when the createCell method changes, it results in layout
     */
    @Test public void testCreateCellFunctionChangesResultInNeedsLayoutAndNoCellsAndNoAccumCell() {
        assertFalse(flow.isNeedsLayout());
        flow.getCellLength(49); // forces accum cell to be created
        assertNotNull("Accum cell was null", flow.accumCell);
        flow.setCreateCell(p -> new CellStub(flow));
        assertTrue(flow.isNeedsLayout());
        assertNull("accumCell didn't get cleared", flow.accumCell);
    }


    ////////////////////////////////////////////////////////////////////////////
    //
    //  Tests on specific functions
    //
    ////////////////////////////////////////////////////////////////////////////

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
            if (i != 29) assertEquals("Bad index: " + i, 25, flow.getCellLength(i), 0.0);
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
        flow = new VirtualFlow();
        flow.setVertical(true);
        flow.setCreateCell(p -> new CellStub(flow) {
            @Override protected double computeMinWidth(double height) { return computePrefWidth(height); }
            @Override protected double computeMaxWidth(double height) { return computePrefWidth(height); }
            @Override protected double computePrefWidth(double height) {
                return flow.isVertical() ? (c.getIndex() == 29 ? 200 : 100) : (c.getIndex() == 29 ? 100 : 25);
            }

            @Override protected double computeMinHeight(double width) { return computePrefHeight(width); }
            @Override protected double computeMaxHeight(double width) { return computePrefHeight(width); }
            @Override protected double computePrefHeight(double width) {
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

    @Test
    public void test_RT_36507() {
        flow = new VirtualFlow();
        flow.setVertical(true);
        // Worst case scenario is that the cells have height = 0.
        // The code should prevent creating more than 100 of these zero height cells
        // (since viewportLength is 100).
        // An "INFO: index exceeds maxCellCount" message should print out.
        flow.setCreateCell(p -> new CellStub(flow) {
            @Override
            protected double computeMaxHeight(double width) { return 0; }
            @Override
            protected double computePrefHeight(double width) { return 0; }
            @Override
            protected double computeMinHeight(double width) { return 0; }

        });
        flow.setCellCount(10);
        flow.setViewportLength(100);
        flow.addLeadingCells(1, 0);
        flow.sheetChildren.addListener((InvalidationListener) (o) -> {
            int count = ((List) o).size();
            assertTrue(Integer.toString(count), count <= 100);
        });
        flow.addTrailingCells(true);
    }

    private int rt36556_instanceCount;
    @Test
    public void test_rt36556() {
        rt36556_instanceCount = 0;
        flow = new VirtualFlow();
        flow.setVertical(true);
        flow.setCreateCell(p -> {
            rt36556_instanceCount++;
            return new CellStub(flow);
        });
        flow.setCellCount(100);
        flow.resize(300, 300);
        pulse();
        final int cellCountAtStart = rt36556_instanceCount;
        flow.adjustPixels(10000);
        pulse();
        assertEquals(cellCountAtStart, rt36556_instanceCount);
        assertNull(flow.getVisibleCell(0));
        assertMinimalNumberOfCellsAreUsed(flow);
    }

    @Test
    public void test_rt36556_scrollto() {
        rt36556_instanceCount = 0;
        flow = new VirtualFlow();
        flow.setVertical(true);
        flow.setCreateCell(p -> {
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
    
    @Test
    public void test_RT39035() {
        flow.adjustPixels(250);
        pulse();
        flow.adjustPixels(500);
        pulse();
        assertTrue(flow.getPosition() < 1.0);
        assertMinimalNumberOfCellsAreUsed(flow);
    }

    @Test
    public void test_RT37421() {
        flow.setPosition(0.98);
        pulse();
        flow.adjustPixels(100);
        pulse();
        assertEquals(1.0, flow.getPosition(), 0.0);
        assertMinimalNumberOfCellsAreUsed(flow);
    }

    @Test
    public void test_RT39568() {
        flow.getHbar().setPrefHeight(16);
        flow.resize(50, flow.getHeight());
        flow.setPosition(1);
        pulse();
        assertTrue("The hbar should have been visible", flow.getHbar().isVisible());
        assertMinimalNumberOfCellsAreUsed(flow);
        assertEquals(flow.getViewportLength()-25.0, flow.cells.getLast().getLayoutY(), 0.0);
    }
}

class CellStub extends IndexedCell {
    String s;
    VirtualFlow flow;

    public CellStub(VirtualFlow flow) { init(flow); }
    public CellStub(VirtualFlow flow, String s) { init(flow); this.s = s; }
    
    private void init(VirtualFlow flow) {
        this.flow = flow;
        setSkin(new SkinStub<CellStub>(this));
        updateItem(this, false);
    }

    @Override
    public void updateIndex(int i) {
        super.updateIndex(i);
        
        s = "Item " + getIndex();
//        updateItem(getIndex(), getIndex() >= flow.getCellCount());
    }
}
