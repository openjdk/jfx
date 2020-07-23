/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

import javafx.event.EventHandler;
import javafx.scene.control.Control;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ScrollToEvent;
import javafx.scene.control.SkinBase;

/**
 * Parent class to control skins whose contents are virtualized and scrollable.
 * This class handles the interaction with the VirtualFlow class, which is the
 * main class handling the virtualization of the contents of this container.
 *
 * @since 9
 */
public abstract class VirtualContainerBase<C extends Control, I extends IndexedCell> extends SkinBase<C> {

    /***************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    private boolean itemCountDirty;

    /**
     * The virtualized container which handles the layout and scrolling of
     * all the cells.
     */
    private final VirtualFlow<I> flow;

    private EventHandler<? super ScrollToEvent<Integer>> scrollToEventHandler;


    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     *
     * @param control the control
     */
    public VirtualContainerBase(final C control) {
        super(control);
        flow = createVirtualFlow();

        scrollToEventHandler = event -> {
            // Fix for RT-24630: The row count in VirtualFlow was incorrect
            // (normally zero), so the scrollTo call was misbehaving.
            if (itemCountDirty) {
                // update row count before we do a scroll
                updateItemCount();
                itemCountDirty = false;
            }
            flow.scrollToTop(event.getScrollTarget());
        };
        control.addEventHandler(ScrollToEvent.scrollToTopIndex(), scrollToEventHandler);
    }



    /***************************************************************************
     *                                                                         *
     * Abstract API                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Returns the total number of items in this container, including those
     * that are currently hidden because they are out of view.
     * @return the total number of items in this container
     */
    protected abstract int getItemCount();

    /**
     * This method is called when it is possible that the item count has changed (i.e. scrolling has occurred,
     * the control has resized, etc). This method should recalculate the item count and store that for future
     * use by the {@link #getItemCount} method.
     */
    protected abstract void updateItemCount();



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * Create the virtualized container that handles the layout and scrolling of
     * all the cells. This enables skin subclasses to provide
     * a custom {@link VirtualFlow} implementation.
     * If not overridden, this method intantiates a default VirtualFlow instance.
     * @return newly created VirtualFlow instance
     * @since 10
     */
    protected VirtualFlow<I> createVirtualFlow() {
        return new VirtualFlow<>();
    }

    /**
     * {@inheritDoc} <p>
     * Overridden to remove EventHandler.
     */
    @Override
    public void dispose() {
        if (getSkinnable() == null) return;
        getSkinnable().removeEventHandler(ScrollToEvent.scrollToTopIndex(), scrollToEventHandler);
        super.dispose();
    }

    /**
     * Get the virtualized container.
     * Subclasses can invoke this method to get the VirtualFlow instance.
     * @return the virtualized container
     * @since 10
     */
    protected final VirtualFlow<I> getVirtualFlow() {
        return flow;
    }

    /**
     * Call this method to indicate that the item count should be updated on the next pulse.
     */
    protected final void markItemCountDirty() {
        itemCountDirty = true;
    }

    /** {@inheritDoc} */
    @Override protected void layoutChildren(double x, double y, double w, double h) {
        checkState();
    }

    /***************************************************************************
     *                                                                         *
     * Private methods                                                         *
     *                                                                         *
     **************************************************************************/

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

    void checkState() {
        if (itemCountDirty) {
            updateItemCount();
            itemCountDirty = false;
        }
    }

    void requestRebuildCells() {
        flow.rebuildCells();
    }

}
