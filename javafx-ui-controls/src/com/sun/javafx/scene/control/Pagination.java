/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
import javafx.beans.property.*;
import javafx.beans.value.WritableValue;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.util.Callback;

@DefaultProperty("pages")
public class Pagination extends Control {

    private static final int DEFAULT_PAGE_INDICATOR_COUNT = 10;

    public static final String STYLE_CLASS_BULLET = "bullet";

    /**
     * Value for indicating that the page count is indeterminate.
     *
     * @see #setPageCount
     */
    public static final int INDETERMINATE = -1;

    /**
     * Constructs a new Pagination.
     */
    public Pagination(int pageCount, int pageIndex) {
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        setPageCount(pageCount);
        setCurrentPageIndex(pageIndex);
    }

    public Pagination(int pageCount) {
        this(pageCount, 0);
    }

    public Pagination() {
        this(INDETERMINATE, 0);
    }

    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * The number of page indicators
     */
    private IntegerProperty pageIndicatorCount;
    public final void setPageIndicatorCount(int value) { pageIndicatorCountProperty().set(value); }
    public final int getPageIndicatorCount() {
        return pageIndicatorCount == null ? DEFAULT_PAGE_INDICATOR_COUNT : pageIndicatorCount.get();
    }
    public final IntegerProperty pageIndicatorCountProperty() {
        if (pageIndicatorCount == null) {
            pageIndicatorCount = new StyleableIntegerProperty(DEFAULT_PAGE_INDICATOR_COUNT) {
                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.PAGE_INDICATOR_COUNT;
                }

                @Override
                public Object getBean() {
                    return Pagination.this;
                }

                @Override
                public String getName() {
                    return "pageIndicatorCount";
                }
            };
        }
        return pageIndicatorCount;
    }

    /**
     * The total number of pages
     */
    private int oldPageCount = 1;
    private IntegerProperty pageCount = new SimpleIntegerProperty(this, "pageCount", 1) {
        @Override protected void invalidated() {
            if (getPageCount() < INDETERMINATE || getPageCount() == 0) {
                setPageCount(oldPageCount);
            }
            oldPageCount = getPageCount();
        }
    };
    public final void setPageCount(int value) { pageCount.set(value); }
    public final int getPageCount() { return pageCount.get(); }
    public final IntegerProperty pageCountProperty() { return pageCount; }

    /**
     * The current page index
     */
    private int oldPageIndex;
    private final IntegerProperty currentPageIndex = new SimpleIntegerProperty(this, "currentPageIndex", 0) {
        @Override protected void invalidated() {
            if (getCurrentPageIndex() < 0) {
                setCurrentPageIndex(oldPageIndex);
            }
            oldPageIndex = getCurrentPageIndex();
        }
    };
    public final void setCurrentPageIndex(int value) { currentPageIndex.set(value); }
    public final int getCurrentPageIndex() { return currentPageIndex.get(); }
    public final IntegerProperty currentPageIndexProperty() { return currentPageIndex; }

    /**
     * The page callback
     */
    private ObjectProperty<Callback<Integer, Node>> pageFactory =
            new SimpleObjectProperty<Callback<Integer, Node>>(this, "pageFactory");
    public final void setPageFactory(Callback<Integer, Node> value) { pageFactory.set(value); }
    public final Callback<Integer, Node> getPageFactory() {return pageFactory.get(); }
    public final ObjectProperty<Callback<Integer, Node>> pageFactoryProperty() { return pageFactory; }

    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "pagination";

    private static class StyleableProperties {
        private static final StyleableProperty<Pagination,Number> PAGE_INDICATOR_COUNT =
            new StyleableProperty<Pagination,Number>("-fx-page-indicator-count",
                SizeConverter.getInstance(), DEFAULT_PAGE_INDICATOR_COUNT) {

            @Override
            public void set(Pagination node, Number value, Origin origin) {
                super.set(node, value.intValue(), origin);
            }

            @Override
            public boolean isSettable(Pagination n) {
                return n.pageIndicatorCount == null || !n.pageIndicatorCount.isBound();
            }

            @Override
            public WritableValue<Number> getWritableValue(Pagination n) {
                return n.pageIndicatorCountProperty();
            }
        };
        private static final List<StyleableProperty> STYLEABLES;
        static {
            final List<StyleableProperty> styleables =
                new ArrayList<StyleableProperty>(Control.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                PAGE_INDICATOR_COUNT
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
