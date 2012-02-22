/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import javafx.collections.MapChangeListener;
import javafx.scene.control.Control;
import javafx.scene.control.IndexedCell;

import com.sun.javafx.scene.control.behavior.BehaviorBase;

/**
 * Parent class to control skins whose contents are virtualized and scrollable.
 * This class handles the interaction with the VirtualFlow class, which is the
 * main class handling the virtualization of the contents of this container.
 *
 * @since JavaFX 1.3
 * @profile common
 */
public abstract class VirtualContainerBase<C extends Control, B extends BehaviorBase<C>, I extends IndexedCell> extends SkinBase<C, B> {

    public static final String SCROLL_TO_INDEX = "VirtualContainerBase.scrollToIndex";

    public VirtualContainerBase(C control, B behavior) {
        super(control, behavior);

        control.getProperties().addListener(new MapChangeListener<Object, Object>() {
            @Override
            public void onChanged(Change<? extends Object, ? extends Object> c) {
                if (c.wasAdded() && SCROLL_TO_INDEX.equals(c.getKey())) {
                    Object row = c.getValueAdded();
                    if (row instanceof Integer) {
                        scrollTo((Integer)row);
                    }

                    c.getMap().remove(SCROLL_TO_INDEX);
                }
            }
        });
    }

    /**
     * The virtualized container which handles the layout and scrolling of
     * all the cells.
     */
    protected VirtualFlow flow;

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
     * Returns the total number of items in this container, including those
     * that are currently hidden because they are out of view.
     */
    public abstract int getItemCount();

    protected void scrollTo(int index) {
        if (/*index < 0 || index >= getItemCount() ||*/ getItemCount() == 0) return;
        
        boolean posSet = false;

        // special-case the 'greater than row count' condition to have it
        // be perfectly at position 1
        if (index >= getItemCount() - 1) {
            flow.setPosition(1);
            posSet = true;
        } else if (index < 0) {
            flow.setPosition(0);
            posSet = true;
        }
        
//        IndexedCell lastVisibleCell = flow.getLastVisibleCell();
//        if (lastVisibleCell != null) {
//            int lastVisibleIndex = lastVisibleCell.getIndex();
//            System.out.println(index + " >= " + lastVisibleIndex);
//            if (index == lastVisibleIndex) {
//                flow.setPosition(1);
//                posSet = true;
//            }
//        }
        
        if (! posSet) {
            // otherwise just use the default maths
            flow.setPosition(index / (double) getItemCount());
        }
        
        flow.requestLayout();
    }
    
    double getMaxCellWidth(int rowsToCount) {
        return getInsets().getLeft() + flow.getMaxCellWidth(rowsToCount) + getInsets().getRight();
    }
    
    double getVirtualFlowPreferredHeight(int rows) {
        double height = 1.0;
        
        for (int i = 0; i < rows && i < getItemCount(); i++) {
            height += flow.getCellLength(i);
        }
        
        return height + getInsets().getTop() + getInsets().getBottom();
    }
}
