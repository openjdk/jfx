/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.NamedArg;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.util.Callback;
import com.sun.javafx.property.PropertyReference;
import com.sun.javafx.scene.control.Logging;
import sun.util.logging.PlatformLogger;
import sun.util.logging.PlatformLogger.Level;


/**
 * A convenience implementation of the Callback interface, designed specifically
 * for use within the {@link TreeTableColumn}
 * {@link TreeTableColumn#cellValueFactoryProperty() cell value factory}. An example
 * of how to use this class is:
 *
 * <pre><code>
 * TreeTableColumn&lt;Person,String&gt; firstNameCol = new TreeTableColumn&lt;Person,String&gt;("First Name");
 * firstNameCol.setCellValueFactory(new TreeItemPropertyValueFactory&lt;Person,String&gt;("firstName"));
 * </code></pre>
 *
 * In this example, the "firstName" string is used as a reference to an assumed
 * <code>firstNameProperty()</code> method in the <code>Person</code> class type
 * (which is the class type of the TreeTableView). Additionally, this method must
 * return a {@link Property} instance. If a method meeting these requirements
 * is found, then the {@link javafx.scene.control.TreeTableCell} is populated with this ObservableValue<T>.
 * In addition, the TreeTableView will automatically add an observer to the
 * returned value, such that any changes fired will be observed by the TreeTableView,
 * resulting in the cell immediately updating.
 *
 * <p>If no method matching this pattern exists, there is fall-through support
 * for attempting to call get&lt;property&gt;() or is&lt;property&gt;() (that is,
 * <code>getFirstName()</code> or <code>isFirstName()</code> in the example
 * above). If a  method matching this pattern exists, the value returned from this method
 * is wrapped in a {@link ReadOnlyObjectWrapper} and returned to the TreeTableCell.
 * However, in this situation, this means that the TreeTableCell will not be able
 * to observe the ObservableValue for changes (as is the case in the first
 * approach above).
 *
 * <p>For reference (and as noted in the TreeTableColumn
 * {@link TreeTableColumn#cellValueFactory cell value factory} documentation), the
 * long form of the code above would be the following:
 *
 * <pre><code>
 * TreeTableColumn&lt;Person,String&gt; firstNameCol = new TreeTableColumn&lt;Person,String&gt;("First Name");
 * firstNameCol.setCellValueFactory(new Callback&lt;CellDataFeatures&lt;Person, String&gt;, ObservableValue&lt;String&gt;&gt;() {
 *     public ObservableValue&lt;String&gt; call(CellDataFeatures&lt;Person, String&gt; p) {
 *         // p.getValue() returns the TreeItem<Person> instance for a particular
 *         // TreeTableView row, and the second getValue() call returns the
 *         // Person instance contained within the TreeItem.
 *         return p.getValue().getValue().firstNameProperty();
 *     }
 *  });
 * }
 * </code></pre>
 *
 * @see TreeTableColumn
 * @see javafx.scene.control.TreeTableView
 * @see javafx.scene.control.TreeTableCell
 * @see PropertyValueFactory
 * @see MapValueFactory
 * @since JavaFX 8.0
 */
public class TreeItemPropertyValueFactory<S,T> implements Callback<TreeTableColumn.CellDataFeatures<S,T>, ObservableValue<T>> {

    private final String property;

    private Class columnClass;
    private String previousProperty;
    private PropertyReference<T> propertyRef;

    /**
     * Creates a default PropertyValueFactory to extract the value from a given
     * TableView row item reflectively, using the given property name.
     *
     * @param property The name of the property with which to attempt to
     *      reflectively extract a corresponding value for in a given object.
     */
    public TreeItemPropertyValueFactory(@NamedArg("property") String property) {
        this.property = property;
    }

    /** {@inheritDoc} */
    @Override public ObservableValue<T> call(CellDataFeatures<S, T> param) {
        TreeItem<S> treeItem = param.getValue();
        return getCellDataReflectively((T)treeItem.getValue());
    }

    /**
     * Returns the property name provided in the constructor.
     */
    public final String getProperty() { return property; }

    private ObservableValue<T> getCellDataReflectively(T rowData) {
        if (getProperty() == null || getProperty().isEmpty() || rowData == null) return null;

        try {
            // we attempt to cache the property reference here, as otherwise
            // performance suffers when working in large data models. For
            // a bit of reference, refer to RT-13937.
            if (columnClass == null || previousProperty == null ||
                    ! columnClass.equals(rowData.getClass()) ||
                    ! previousProperty.equals(getProperty())) {

                // create a new PropertyReference
                this.columnClass = rowData.getClass();
                this.previousProperty = getProperty();
                this.propertyRef = new PropertyReference<T>(rowData.getClass(), getProperty());
            }

            return propertyRef.getProperty(rowData);
        } catch (IllegalStateException e) {
            try {
                // attempt to just get the value
                T value = propertyRef.get(rowData);
                return new ReadOnlyObjectWrapper<T>(value);
            } catch (IllegalStateException e2) {
                // fall through to logged exception below
            }

            // log the warning and move on
            final PlatformLogger logger = Logging.getControlsLogger();
            if (logger.isLoggable(Level.WARNING)) {
               logger.finest("Can not retrieve property '" + getProperty() +
                        "' in TreeItemPropertyValueFactory: " + this +
                        " with provided class type: " + rowData.getClass(), e);
            }
        }

        return null;
    }
}
