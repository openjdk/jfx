/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.behavior.CellBehaviorBase;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.control.IndexedCell;
import javafx.scene.shape.Rectangle;

/**
 */
public abstract class TableCellSkinBase<C extends IndexedCell, B extends CellBehaviorBase<C>> extends CellSkinBase<C,B> {
    
    protected abstract ReadOnlyDoubleProperty columnWidthProperty(); // tableColumn.widthProperty()
    protected abstract BooleanProperty columnVisibleProperty(); // tableColumn.visibleProperty()
    
    // This property is set on the cell when we want to know its actual
    // preferred width, not the width of the associated TableColumn.
    // This is primarily used in NestedTableColumnHeader such that user double
    // clicks result in the column being resized to fit the widest content in 
    // the column
    static final String DEFER_TO_PARENT_PREF_WIDTH = "deferToParentPrefWidth";
    boolean isDeferToParentForPrefWidth = false;
    
    private InvalidationListener columnWidthListener = new InvalidationListener() {
        @Override public void invalidated(Observable valueModel) {
            getSkinnable().requestLayout();
        }
    };
    
    private WeakInvalidationListener weakColumnWidthListener =
            new WeakInvalidationListener(columnWidthListener);
    
    public TableCellSkinBase(final C control, final B behavior) {
        super(control, behavior);
        
        // init(control) should not be called here - it should be called by the
        // subclass after initialising itself. This is to prevent NPEs (for 
        // example, getVisibleLeafColumns() throws a NPE as the control itself
        // is not yet set in subclasses).
    }
    
    protected void init(C control) {
        // RT-22038
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(control.widthProperty());
        clip.heightProperty().bind(control.heightProperty());
        getSkinnable().setClip(clip);
        // --- end of RT-22038
        
        ReadOnlyDoubleProperty columnWidthProperty = columnWidthProperty();
        if (columnWidthProperty != null) {
            columnWidthProperty.addListener(
                new WeakInvalidationListener(weakColumnWidthListener));
        }

        registerChangeListener(control.visibleProperty(), "VISIBLE");
        
        if (control.getProperties().containsKey(DEFER_TO_PARENT_PREF_WIDTH)) {
            isDeferToParentForPrefWidth = true;
        }
    }
    
    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        if ("VISIBLE".equals(p)) {
            getSkinnable().setVisible(columnVisibleProperty().get());
        }
    }
    
    @Override protected void layoutChildren(final double x, final double y,
            final double w, final double h) {
        // fit the cell within this space
        // FIXME the subtraction of bottom padding isn't right here - but it
        // results in better visuals, so I'm leaving it in place for now.
        layoutLabelInArea(x, y, w, h - getSkinnable().getPadding().getBottom());
    }

    @Override protected double computePrefWidth(double height) {
        if (isDeferToParentForPrefWidth) {
            return super.computePrefWidth(height);
        }
        return columnWidthProperty().get();
    }
}
