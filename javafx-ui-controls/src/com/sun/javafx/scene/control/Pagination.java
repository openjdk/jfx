/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.javafx.scene.control;

import com.sun.javafx.css.StyleableIntegerProperty;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.Stylesheet.Origin;
import com.sun.javafx.css.converters.SizeConverter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.DefaultProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.WritableValue;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.util.Callback;

@DefaultProperty("pages")
public class Pagination<T> extends Control {

    private static final int DEFAULT_NUMBER_OF_VISIBLE_PAGES = 10;

    public static final String STYLE_CLASS_BULLET = "bullet";

    /**
     * Constructs a new Pagination.
     */
    public Pagination(int numberOfItems) {
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        setNumberOfItems(numberOfItems);
    }

    private Pagination() {
    }

    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * The number of visible page indicators
     */
    public final IntegerProperty numberOfVisiblePagesProperty() {
        if (numberOfVisiblePages == null) {
            numberOfVisiblePages = new StyleableIntegerProperty(DEFAULT_NUMBER_OF_VISIBLE_PAGES) {
                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.NUMBER_OF_VISIBLE_PAGES;
                }

                @Override
                public Object getBean() {
                    return Pagination.this;
                }

                @Override
                public String getName() {
                    return "numberOfVisiblePages";
                }
            };
        }
        return numberOfVisiblePages;
    }

    private IntegerProperty numberOfVisiblePages;
    public final void setNumberOfVisiblePages(int value) {
        if (value < 1) {
            value = 1;
        }
        numberOfVisiblePagesProperty().set(value);
    }

    public final int getNumberOfVisiblePages() {
        return numberOfVisiblePages == null ? DEFAULT_NUMBER_OF_VISIBLE_PAGES : numberOfVisiblePages.get();
    }

    /**
     * The max number of items per page
     */
    public final IntegerProperty itemsPerPage = new SimpleIntegerProperty(this, "itemsPerPage", 10);
    public final void setItemsPerPage(int value) { 
        if (value < 1) {
            value = 1;
        }        
        itemsPerPage.set(value); 
    }
    public final int getItemsPerPage() { return itemsPerPage.get(); }
    public final IntegerProperty itemsPerPageProperty() { return itemsPerPage; }

    /**
     * The total number of items
     */
    public final IntegerProperty numberOfItems = new SimpleIntegerProperty(this, "numberOfItems", 1);
    public final void setNumberOfItems(int value) { numberOfItems.set(value); }
    public final int getNumberOfItems() { return numberOfItems.get(); }
    public final IntegerProperty numberOfItemsProperty() { return numberOfItems; }

    /**
     * The current page index
     */
    public final IntegerProperty pageIndex = new SimpleIntegerProperty(this, "pageIndex", 0);
    public final void setPageIndex(int value) { 
        if (value < 0) {
            value = 0;
        }                
        pageIndex.set(value); 
    }
    public final int getPageIndex() { return pageIndex.get(); }
    public final IntegerProperty pageIndexProperty() { return pageIndex; }

    /**
     * The page callback
     */
    private ObjectProperty<Callback<Integer, Node>> pageFactory =
            new SimpleObjectProperty<Callback<Integer, Node>>(this, "pageFactory");
    public final void setPageFactory(Callback<Integer, Node> value) { pageFactoryProperty().set(value); }
    public final Callback<Integer, Node> getPageFactory() {return pageFactoryProperty().get(); }
    public ObjectProperty<Callback<Integer, Node>> pageFactoryProperty() { return pageFactory; }

    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "pagination";

    private static class StyleableProperties {
        private static final StyleableProperty<Pagination,Number> NUMBER_OF_VISIBLE_PAGES =
            new StyleableProperty<Pagination,Number>("-fx-number-of-visible-pages",
                SizeConverter.getInstance(), DEFAULT_NUMBER_OF_VISIBLE_PAGES) {

            @Override
            public void set(Pagination node, Number value, Origin origin) {
                super.set(node, value.intValue(), origin);
            }

            @Override
            public boolean isSettable(Pagination n) {
                return n.numberOfVisiblePages == null || !n.numberOfVisiblePages.isBound();
            }

            @Override
            public WritableValue<Number> getWritableValue(Pagination n) {
                return n.numberOfVisiblePagesProperty();
            }
        };
        private static final List<StyleableProperty> STYLEABLES;
        static {
            final List<StyleableProperty> styleables =
                new ArrayList<StyleableProperty>(Control.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                NUMBER_OF_VISIBLE_PAGES
            );
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return Pagination.StyleableProperties.STYLEABLES;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an experimental API that is not intended for general use and is subject to change in future versions
     */
    @Deprecated
    public List<StyleableProperty> impl_getStyleableProperties() {
        return impl_CSS_STYLEABLES();
    }
}
