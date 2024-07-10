/*
 * Copyright (c) 2015, 2024, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.control.skin;

import java.util.List;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Skin;
import javafx.scene.layout.StackPane;

public class VirtualFlowShim<T extends IndexedCell> extends VirtualFlow<T> {

    public final ArrayLinkedList<T> cells = super.cells;
    public final ObservableList<Node> sheetChildren = super.sheetChildren;

    @Override
    public void setViewportLength(double value) {
        super.setViewportLength(value);
    }

    @Override
    public double getCellLength(int index) {
        return super.getCellLength(index);
    }

    @Override
    public double getCellPosition(T cell) {
        return super.getCellPosition(cell);
    }

    @Override
    public double getCellSize(int idx) {
        return super.getCellSize(idx);
    }

    @Override
    public void setCellDirty(int idx) {
        super.setCellDirty(idx);
    }

    @Override
    public void recreateCells() {
        super.recreateCells();
    }

    public double shim_getMaxPrefBreadth() {
        return super.getMaxPrefBreadth();
    }

    public ScrollBar shim_getHbar() {
        return super.getHbar();
    }

    public ScrollBar shim_getVbar() {
        return super.getVbar();
    }

    public int shim_computeCurrentIndex() {
        return super.computeCurrentIndex();
    }

    public ClippedContainer get_clipView() {
        return super.clipView;
    }

    public double get_clipView_getWidth() {
        return super.clipView.getWidth();
    }

    public double get_clipView_getHeight() {
        return super.clipView.getHeight();
    }

    public double get_clipView_getX() {
        return - super.clipView.getLayoutX();
    }


    public StackPane get_corner() {
        return super.corner;
    }

    public T get_accumCell() {
        return super.accumCell;
    }

    @Override
    public boolean addTrailingCells(boolean fillEmptyCells) {
        return super.addTrailingCells(fillEmptyCells);
    }

    @Override
    public void addLeadingCells(int currentIndex, double startOffset) {
        super.addLeadingCells(currentIndex, startOffset);
    }

    //------------------- statics --------------------

    /**
     * Returns the VirtualFlow managed by the given skin.
     */
    public static <T extends IndexedCell<?>> VirtualFlow<T> getVirtualFlow(Skin<?> skin) {
        return ((VirtualContainerBase<?, T>) skin).getVirtualFlow();
    }

    /**
     * Returns the list of cells displayed in the viewport of the flow.
     *
     * @see VirtualFlow#getCells()
     */
    public static <T extends IndexedCell<?>> List<T> getCells(VirtualFlow<T> flow) {
        return flow.getCells();
    }

    /**
     * Returns the vertical scrollbar of the given flow.
     */
    public static ScrollBar getVBar(VirtualFlow<?> flow) {
        return flow.getVbar();
    }

    /**
     * Returns the horizontal scrollbar of the given flow.
     */
    public static ScrollBar getHBar(VirtualFlow<?> flow) {
        return flow.getHbar();
    }

    public static <T> T cells_getFirst(VirtualFlow.ArrayLinkedList<T> list) {
        return list.getFirst();
    }

    public static <T> T cells_getLast(VirtualFlow.ArrayLinkedList<T> list) {
        return list.getLast();
    }

    public static <T> T cells_get(VirtualFlow.ArrayLinkedList<T> list, int i) {
        return list.get(i);
    }

    public static int cells_size(VirtualFlow.ArrayLinkedList<?> list) {
        return list.size();
    }



    public static class ArrayLinkedListShim<T> extends VirtualFlow.ArrayLinkedList<T> {

        @Override
        public T getFirst() {
            return super.getFirst();
        }

        @Override
        public T getLast() {
            return super.getLast();
        }

        @Override
        public void addFirst(T cell) {
            super.addFirst(cell);
        }

        @Override
        public void addLast(T cell) {
            super.addLast(cell);
        }

        @Override
        public int size() {
            return super.size();
        }

        @Override
        public boolean isEmpty() {
            return super.isEmpty();
        }

        @Override
        public T get(int index) {
            return super.get(index);
        }

        @Override
        public void clear() {
            super.clear();
        }

        @Override
        public T removeFirst() {
            return super.removeFirst();
        }

        @Override
        public T removeLast() {
            return super.removeLast();
        }

        @Override
        public T remove(int index) {
            return super.remove(index);
        }

    }

}
