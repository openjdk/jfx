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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.scene.control.Cell;

import com.sun.javafx.css.StyleableDoubleProperty;
import com.sun.javafx.css.CssMetaData;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.scene.control.behavior.CellBehaviorBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.value.WritableValue;
import javafx.scene.control.SkinBase;


/**
 * A base skin implementation, specifically for ListCellSkin and TreeCellSkin.
 * This might not be a suitable base class for TreeCellSkin or some other
 * such skins.
 */
public class CellSkinBase<C extends Cell, B extends CellBehaviorBase<C>> extends LabeledSkinBase<C, B> {
    /**
     * The default cell size. For vertical ListView or a TreeView or TableView
     * this is the height, for a horizontal ListView this is the width. This
     * is settable from CSS
     */
    private DoubleProperty cellSize;
//    boolean cellSizeSet = false;

    public final double getCellSize() {
        return cellSize == null ? DEFAULT_CELL_SIZE : cellSize.get();
    }

    public final ReadOnlyDoubleProperty cellSizeProperty() {
        return (ReadOnlyDoubleProperty)cellSizePropertyImpl();
    }

    private DoubleProperty cellSizePropertyImpl() {
        if (cellSize == null) {
            cellSize = new StyleableDoubleProperty(DEFAULT_CELL_SIZE) {
                @Override public void set(double value) {
//                    // Commented this out due to RT-19794, because otherwise
//                    // cellSizeSet would be false when the default caspian.css
//                    // cell size was set. This would lead to 
//                    // ListCellSkin.computePrefHeight computing the pref height
//                    // of the cell (which is about 22px), rather than use the 
//                    // value provided by caspian.css (which is 24px).
//                    // cellSizeSet = true;//value != DEFAULT_CELL_SIZE;
                    super.set(value);
                    getSkinnable().requestLayout();
                }
                
                @Override public Object getBean() {
                    return CellSkinBase.this;
                }

                @Override public String getName() {
                    return "cellSize";
                }

                @Override public CssMetaData getCssMetaData() {
                    return StyleableProperties.CELL_SIZE;
                }
            }; 
        }
        return cellSize;
    }

    public CellSkinBase(final C control, final B behavior) {
        super (control, behavior);

        /**
         * The Cell does not typically want to block mouse events from going down
         * to the virtualized controls holding the cell. For example mouse clicks
         * on cells should also pass down to the ListView holding the cells.
         */
        consumeMouseEvents(false);
    }


    
    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

    static final double DEFAULT_CELL_SIZE = 24.0;

     /**
      * Super-lazy instantiation pattern from Bill Pugh.
      * @treatAsPrivate implementation detail
      */
     private static class StyleableProperties {
         private final static CssMetaData<Cell,Number> CELL_SIZE =
                new CssMetaData<Cell,Number>("-fx-cell-size",
                 SizeConverter.getInstance(), DEFAULT_CELL_SIZE) {

            @Override
            public void set(Cell node, Number value) {
                double size = value == null ? DEFAULT_CELL_SIZE : ((Number)value).doubleValue();
                // guard against a 0 or negative size
                super.set(node, size <= 0 ? DEFAULT_CELL_SIZE : size);
            }

            @Override
            public boolean isSettable(Cell n) {
                final CellSkinBase skin = (CellSkinBase) n.getSkin();
                return skin.cellSize == null || !skin.cellSize.isBound();
            }

            @Override
            public WritableValue<Number> getWritableValue(Cell n) {
                final CellSkinBase skin = (CellSkinBase) n.getSkin();
                return skin.cellSizePropertyImpl();
            }
        };

         private static final List<CssMetaData> STYLEABLES;
         static {

            final List<CssMetaData> styleables = 
                new ArrayList<CssMetaData>(SkinBase.getClassCssMetaData());
            Collections.addAll(styleables,
                CELL_SIZE
            );
            STYLEABLES = Collections.unmodifiableList(styleables);

         }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     */
    public static List<CssMetaData> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData> getCssMetaData() {
        return getClassCssMetaData();
    }

}
