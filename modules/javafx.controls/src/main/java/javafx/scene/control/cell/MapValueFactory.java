/*
 * Copyright (c) 2012, 2025, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.cell;

import java.util.Map;

import javafx.beans.NamedArg;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyFloatWrapper;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.util.Callback;

/**
 * A convenience implementation of the Callback interface, designed specifically
 * for use within the {@link TableColumn}
 * {@link TableColumn#cellValueFactoryProperty() cell value factory}. An example
 * of how to use this class is:
 *
 * <pre><code>
 * {@literal ObservableList<Map> personsMapList = ...
 *
 * TableColumn<Map, String> firstNameColumn = new TableColumn<Map, String>("First Name");
 * firstNameColumn.setCellValueFactory(new MapValueFactory<String>("firstName"));
 *
 * TableView<Map> table = new TableView<Map>(personMapList);
 * tableView.getColumns().setAll(firstNameColumn);}
 * </code></pre>
 *
 * <p>In this example, there is a list of Map instances, where each Map instance
 * represents a single row in the TableView. The "firstName" string is used as a
 * key into this map, and the value corresponding to this key is returned, if
 * one exists. If the value is an {@link ObservableValue}, then this is returned
 * directly, otherwise the value is wrapped in a {@link ReadOnlyObjectWrapper}.
 *
 * @see TableColumn
 * @see TableView
 * @see TableCell
 * @see PropertyValueFactory
 * @param <T> The type of the class contained within the TableColumn cells.
 * @since JavaFX 2.2
 */
public class MapValueFactory<T> implements Callback<CellDataFeatures<Map,T>, ObservableValue<T>> {

    private final Object key;

    /**
     * Creates a default MapValueFactory, which will use the provided key to
     * lookup the value for cells in the {@link TableColumn} in which this
     * MapValueFactory is installed (via the
     * {@link TableColumn#cellValueFactoryProperty() cell value factory} property.
     *
     * @param key The key to use to lookup the value in the {@code Map}.
     */
    public MapValueFactory(final @NamedArg("key") Object key) {
        this.key = key;
    }

    @Override public ObservableValue<T> call(CellDataFeatures<Map, T> cdf) {
        Map map = cdf.getValue();
        Object value = map.get(key);

        // ideally the map will contain observable values directly, and in which
        // case we can just return this observable value.
        if (value instanceof ObservableValue) {
            return (ObservableValue)value;
        }

        // TODO
        // If we are here, the value in the map for the given key is not observable,
        // but perhaps the Map is an ObservableMap. If this is the case, we
        // can add a listener to the map for the given key, and possibly observe
        // it for changes and return these
//        if (map instanceof ObservableMap) {
//            ObservableMap oMap = (ObservableMap) map;
//            // ....
//        }

        // Often time there is special case code to deal with specific observable
        // value types, so we try to wrap in the most specific type.
        if (value instanceof Boolean) {
            return (ObservableValue<T>) new ReadOnlyBooleanWrapper((Boolean)value);
        } else if (value instanceof Integer) {
            return (ObservableValue<T>) new ReadOnlyIntegerWrapper((Integer)value);
        } else if (value instanceof Float) {
            return (ObservableValue<T>) new ReadOnlyFloatWrapper((Float)value);
        } else if (value instanceof Long) {
            return (ObservableValue<T>) new ReadOnlyLongWrapper((Long)value);
        } else if (value instanceof Double) {
            return (ObservableValue<T>) new ReadOnlyDoubleWrapper((Double)value);
        } else if (value instanceof String) {
            return (ObservableValue<T>) new ReadOnlyStringWrapper((String)value);
        }

        // fall back to an object wrapper
        return new ReadOnlyObjectWrapper<>((T)value);
    }
}
