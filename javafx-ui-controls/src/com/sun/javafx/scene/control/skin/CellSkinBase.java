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
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.scene.control.behavior.CellBehaviorBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.value.WritableValue;


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
    boolean cellSizeSet = false;

    public final double getCellSize() {
        return cellSize == null ? DEFAULT_CELL_SIZE : cellSize.get();
    }

    public final ReadOnlyDoubleProperty cellSizeProperty() {
        return (ReadOnlyDoubleProperty)cellSizePropertyImpl();
    }

    private DoubleProperty cellSizePropertyImpl() {
        if (cellSize == null) {
            cellSize = new StyleableDoubleProperty(DEFAULT_CELL_SIZE) {

                @Override
                public void set(double value) {
                    cellSizeSet = value != DEFAULT_CELL_SIZE;
                    super.set(value);
                }
                
                @Override
                public Object getBean() {
                    return CellSkinBase.this;
                }

                @Override
                public String getName() {
                    return "cellSize";
                }

                @Override
                public StyleableProperty getStyleableProperty() {
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
      * @treatasprivate implementation detail
      */
     private static class StyleableProperties {
         private final static StyleableProperty<CellSkinBase,Number> CELL_SIZE =
                new StyleableProperty<CellSkinBase,Number>("-fx-cell-size",
                 SizeConverter.getInstance(), DEFAULT_CELL_SIZE) {

            @Override
            public void set(CellSkinBase node, Number value) {
                double size = value == null ? DEFAULT_CELL_SIZE : ((Number)value).doubleValue();
                // guard against a 0 or negative size
                super.set(node, size <= 0 ? DEFAULT_CELL_SIZE : size);
            }

            @Override
            public boolean isSettable(CellSkinBase n) {
                return n.cellSize == null || !n.cellSize.isBound();
            }

            @Override
            public WritableValue<Number> getWritableValue(CellSkinBase n) {
                return n.cellSizePropertyImpl();
            }
        };

         private static final List<StyleableProperty> STYLEABLES;
         static {

            final List<StyleableProperty> styleables = 
                new ArrayList<StyleableProperty>(SkinBase.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                CELL_SIZE
            );
            STYLEABLES = Collections.unmodifiableList(styleables);

         }
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return StyleableProperties.STYLEABLES;
    };

}
