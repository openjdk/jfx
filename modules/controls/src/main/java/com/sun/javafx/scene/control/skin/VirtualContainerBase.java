/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.event.EventHandler;
import javafx.scene.control.Control;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ScrollToEvent;

import com.sun.javafx.scene.control.behavior.BehaviorBase;

/**
 * Parent class to control skins whose contents are virtualized and scrollable.
 * This class handles the interaction with the VirtualFlow class, which is the
 * main class handling the virtualization of the contents of this container.
 *
 * @profile common
 */
public abstract class VirtualContainerBase<C extends Control, B extends BehaviorBase<C>, I extends IndexedCell> extends BehaviorSkinBase<C, B> {
    
    protected boolean rowCountDirty;

    public VirtualContainerBase(final C control, B behavior) {
        super(control, behavior);
        flow = createVirtualFlow();
        
        control.addEventHandler(ScrollToEvent.scrollToTopIndex(), new EventHandler<ScrollToEvent<Integer>>() {
            @Override public void handle(ScrollToEvent<Integer> event) {
                // Fix for RT-24630: The row count in VirtualFlow was incorrect
                // (normally zero), so the scrollTo call was misbehaving.
                if (rowCountDirty) {
                    // update row count before we do a scroll
                    updateRowCount();
                    rowCountDirty = false;
                }
                flow.scrollTo(event.getScrollTarget());
            }
        });        
    }

    /**
     * The virtualized container which handles the layout and scrolling of
     * all the cells.
     */
    protected final VirtualFlow<I> flow;

    /**
     * Returns a Cell available to be used in the virtual flow. This means you
     * may return either a previously used, but now unrequired cell, or alternatively
     * create a new Cell instance.
     *
     * Preference is obviously given to reusing cells whenever possible, to keep
     * performance costs down.
     */
    public abstract I createCell();

    /**
     * This enables skin subclasses to provide a custom VirtualFlow implementation,
     * rather than have VirtualContainerBase instantiate the default instance.
     */
    protected VirtualFlow<I> createVirtualFlow() {
        return new VirtualFlow<I>();
    }

    /**
     * Returns the total number of items in this container, including those
     * that are currently hidden because they are out of view.
     */
    public abstract int getItemCount();
    
    protected abstract void updateRowCount();

    double getMaxCellWidth(int rowsToCount) {
        return snappedLeftInset() + flow.getMaxCellWidth(rowsToCount) + snappedRightInset();
    }
    
    double getVirtualFlowPreferredHeight(int rows) {
        double height = 1.0;
        
        for (int i = 0; i < rows && i < getItemCount(); i++) {
            height += flow.getCellLength(i);
        }

        return height + snappedTopInset() + snappedBottomInset();
    }

    @Override protected void layoutChildren(double x, double y, double w, double h) {
        checkState();
    }

    protected void checkState() {
        if (rowCountDirty) {
            updateRowCount();
            rowCountDirty = false;
        }
    }
}
