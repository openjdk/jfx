/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.control.IndexedCell;
import javafx.scene.control.skin.VirtualFlowShim;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Tests some protected methods of the VirtualFlow when overriding it.
 */
public class VirtualFlowSubClassTest {

    // The following 4 vars are used when testing the
    private VirtualFlowShim.ArrayLinkedListShim<CellStub> list;
    private CellStub a;
    private CellStub b;
    private CellStub c;

    // The VirtualFlow we are going to test. By default, there are 100 cells
    // and each cell is 100 wide and 25 tall, except for the 30th cell, which
    // is 200 wide and 100 tall.
    private SubVirtualFlow<IndexedCell> flow;

    @Before public void setUp() {
        list = new VirtualFlowShim.ArrayLinkedListShim<>();
        a = new CellStub(flow, "A");
        b = new CellStub(flow, "B");
        c = new CellStub(flow, "C");

        flow = new SubVirtualFlow();
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
        // Need a second pulse() call is because this parent can be made
        // "layout" dirty again by its children
        pulse();
    }

    private void pulse() {
        flow.layout();
    }

    /**
     * Tests the protected getFirstVisibleCellWithinViewport method returns the right value, especially when scrolling a
     * bit.
     */
    @Test public void test_getFirstVisibleCellWithinViewport() {
        assertEquals(flow.getFirstVisibleCell(), flow.getFirstVisibleCellWithinViewport());
        assertEquals(0, flow.getFirstVisibleCellWithinViewport().getIndex());
        flow.scrollPixels(10);
        assertFalse(flow.getFirstVisibleCell().equals(flow.getFirstVisibleCellWithinViewport()));
        assertEquals(1, flow.getFirstVisibleCellWithinViewport().getIndex());

    }

    /**
     * Tests the protected getLastVisibleCellWithinViewport method returns the right value, especially when scrolling a
     * bit.
     */
    @Test public void test_getLastVisibleCellWithinViewport() {
        assertEquals(flow.getLastVisibleCell(), flow.getLastVisibleCellWithinViewport());
        int lastIndex = flow.getLastVisibleCell().getIndex();
        assertEquals(lastIndex, flow.getLastVisibleCellWithinViewport().getIndex());
        flow.scrollPixels(10);
        assertFalse(flow.getLastVisibleCell().equals(flow.getLastVisibleCellWithinViewport()));
        assertEquals(lastIndex, flow.getLastVisibleCellWithinViewport().getIndex());
        assertEquals(lastIndex + 1, flow.getLastVisibleCell().getIndex());

    }

    /**
     * A subClass of the VirtualFlow that give access to some methods for testing purpose.
     *
     * @param <T>
     */
    class SubVirtualFlow<T extends IndexedCell> extends VirtualFlowShim {

        @Override
        public IndexedCell getLastVisibleCellWithinViewport() {
            return super.getLastVisibleCellWithinViewport();
        }

        @Override
        public IndexedCell getFirstVisibleCellWithinViewport() {
            return super.getFirstVisibleCellWithinViewport();
        }
    }
}
