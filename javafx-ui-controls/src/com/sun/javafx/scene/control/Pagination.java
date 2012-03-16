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
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SingleSelectionModel;
import javafx.util.Callback;

@DefaultProperty("pages")
public class Pagination<T> extends Control {

    private static final int DEFAULT_NUMBER_OF_VISIBLE_PAGES = 10;
    
    public static final String STYLE_CLASS_BULLET = "bullet";

    /**
     * Constructs a new Pagination.
     */
    public Pagination() {
        this(FXCollections.<T>observableArrayList());
    }

    public Pagination(ObservableList<T> items) {
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        setSelectionModel(new PaginationSelectionModel(this));
        setItems(items);
    }

    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // The number of visible page indicators
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
        numberOfVisiblePagesProperty().set(value);
    }

    public final int getNumberOfVisiblePages() {        
        return numberOfVisiblePages == null ? DEFAULT_NUMBER_OF_VISIBLE_PAGES : numberOfVisiblePages.get();
    }

    // --- Pages
    private ObjectProperty<ObservableList<T>> items;

    public final void setItems(ObservableList<T> value) {
        itemsProperty().set(value);
    }

    public final ObservableList<T> getItems() {
        return items == null ? null : items.get();
    }

    public final ObjectProperty<ObservableList<T>> itemsProperty() {
        if (items == null) {
            items = new SimpleObjectProperty<ObservableList<T>>(this, "items") {
                @Override protected void invalidated() {
                }
            };
        }
        return items;
    }

    private ObjectProperty<Callback<ListView<T>, ListCell<T>>> cellFactory =
            new SimpleObjectProperty<Callback<ListView<T>, ListCell<T>>>(this, "cellFactory");
    public final void setCellFactory(Callback<ListView<T>, ListCell<T>> value) { cellFactoryProperty().set(value); }
    public final Callback<ListView<T>, ListCell<T>> getCellFactory() {return cellFactoryProperty().get(); }
    public ObjectProperty<Callback<ListView<T>, ListCell<T>>> cellFactoryProperty() { return cellFactory; }

    private ObjectProperty<SingleSelectionModel<T>> selectionModel = new SimpleObjectProperty<SingleSelectionModel<T>>(this, "selectionModel");

    /**
     * <p>Sets the model used for page selection.  By changing the model you can alter
     * how the pages are selected and which pages are first or last.</p>
     */
    public final void setSelectionModel(SingleSelectionModel<T> value) { selectionModel.set(value); }

    /**
     * <p>Gets the model used for page selection.</p>
     */
    public final SingleSelectionModel<T> getSelectionModel() { return selectionModel.get(); }

    /**
     * The selection model used for selecting pages.
     */
    public final ObjectProperty<SingleSelectionModel<T>> selectionModelProperty() { return selectionModel; }

    static class PaginationSelectionModel<T> extends SingleSelectionModel<T> {
        private final Pagination pagination;

        public PaginationSelectionModel(final Pagination<T> pagination) {
            if (pagination == null) {
                throw new NullPointerException("Pagination can not be null");
            }
            this.pagination = pagination;

            selectedIndexProperty().addListener(new InvalidationListener() {
                @Override public void invalidated(Observable valueModel) {
                    // we used to lazily retrieve the selected item, but now we just
                    // do it when the selection changes.
                    setSelectedItem(getModelItem(getSelectedIndex()));
                }
            });

            /*
             * The following two listeners are used in conjunction with
             * SelectionModel.select(T obj) to allow for a developer to select
             * an item that is not actually in the data model. When this occurs,
             * we actively try to find an index that matches this object, going
             * so far as to actually watch for all changes to the items list,
             * rechecking each time.
             */

            this.pagination.itemsProperty().addListener(weakItemsObserver);
            if (pagination.getItems() != null) {
                this.pagination.getItems().addListener(weakItemsContentObserver);
            }
        }

        // watching for changes to the items list content
        private final ListChangeListener<T> itemsContentObserver = new ListChangeListener<T>() {
            @Override public void onChanged(Change<? extends T> c) {
                if (pagination.getItems() == null || pagination.getItems().isEmpty()) {
                    setSelectedIndex(-1);
                } else if (getSelectedIndex() == -1 && getSelectedItem() != null) {
                    int newIndex = pagination.getItems().indexOf(getSelectedItem());
                    if (newIndex != -1) {
                        setSelectedIndex(newIndex);
                    }
                }

                while (c.next()) {
                    if (c.getFrom() <= getSelectedIndex() && getSelectedIndex()!= -1 && (c.wasAdded() || c.wasRemoved())) {
                        int shift = c.wasAdded() ? c.getAddedSize() : -c.getRemovedSize();
                        clearAndSelect(getSelectedIndex() + shift);
                    }
                }
            }
        };

        // watching for changes to the items list
        private final ChangeListener<ObservableList<T>> itemsObserver = new ChangeListener<ObservableList<T>>() {
            @Override
            public void changed(ObservableValue<? extends ObservableList<T>> valueModel,
                ObservableList<T> oldList, ObservableList<T> newList) {
                    updateItemsObserver(oldList, newList);
            }
        };

        private WeakListChangeListener weakItemsContentObserver =
                new WeakListChangeListener(itemsContentObserver);

        private WeakChangeListener weakItemsObserver =
                new WeakChangeListener(itemsObserver);

        private void updateItemsObserver(ObservableList<T> oldList, ObservableList<T> newList) {
            // update listeners
            if (oldList != null) {
                oldList.removeListener(weakItemsContentObserver);
            }
            if (newList != null) {
                newList.addListener(weakItemsContentObserver);
            }

            // when the items list totally changes, we should clear out
            // the selection and focus
            setSelectedIndex(-1);
        }

        @Override protected T getModelItem(int index) {
            final ObservableList<T> items = pagination.getItems();
            if (items == null) return null;
            if (index < 0 || index >= items.size()) return null;
            return items.get(index);
        }

        @Override protected int getItemCount() {
            final ObservableList<T> items = pagination.getItems();
            return items == null ? 0 : items.size();
        }
    }

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
