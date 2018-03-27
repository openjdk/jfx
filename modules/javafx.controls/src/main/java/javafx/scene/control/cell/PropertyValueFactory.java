/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.util.Callback;

import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;
import com.sun.javafx.property.PropertyReference;
import com.sun.javafx.scene.control.Logging;


/**
 * A convenience implementation of the Callback interface, designed specifically
 * for use within the {@link TableColumn}
 * {@link TableColumn#cellValueFactoryProperty() cell value factory}. An example
 * of how to use this class is:
 *
 * <pre><code>
 * TableColumn&lt;Person,String&gt; firstNameCol = new TableColumn&lt;Person,String&gt;("First Name");
 * firstNameCol.setCellValueFactory(new PropertyValueFactory&lt;Person,String&gt;("firstName"));
 * </code></pre>
 *
 * <p>
 * In this example, {@code Person} is the class type of the {@code TableView}
 * {@link TableView#itemsProperty() items} list.
 * The class {@code Person} must be declared public.
 * {@code PropertyValueFactory} uses the constructor argument,
 * {@code "firstName"}, to assume that {@code Person} has a public method
 * {@code firstNameProperty} with no formal parameters and a return type of
 * {@code ObservableValue<String>}.
 * </p>
 * <p>
 * If such a method exists, then it is invoked, and additionally assumed
 * to return an instance of {@code Property<String>}. The return value is used
 * to populate the {@link TableCell}. In addition, the {@code TableView} adds
 * an observer to the return value, such that any changes fired will be observed
 * by the {@code TableView}, resulting in the cell immediately updating.
 * </p>
 * <p>
 * If no such method exists, then {@code PropertyValueFactory}
 * assumes that {@code Person} has a public method {@code getFirstName} or
 * {@code isFirstName} with no formal parameters and a return type of
 * {@code String}. If such a method exists, then it is invoked, and its return
 * value is wrapped in a {@link ReadOnlyObjectWrapper}
 * and returned to the {@code TableCell}. In this situation,
 * the {@code TableCell} will not be able to observe changes to the property,
 * unlike in the first approach above.
 * </p>
 *
 * <p>For reference (and as noted in the TableColumn
 * {@link TableColumn#cellValueFactoryProperty()}  cell value factory} documentation), the
 * long form of the code above would be the following:
 * </p>
 *
 * <pre><code>
 * TableColumn&lt;Person,String&gt; firstNameCol = new TableColumn&lt;Person,String&gt;("First Name");
 * firstNameCol.setCellValueFactory(new Callback&lt;CellDataFeatures&lt;Person, String&gt;, ObservableValue&lt;String&gt;&gt;() {
 *     public ObservableValue&lt;String&gt; call(CellDataFeatures&lt;Person, String&gt; p) {
 *         // p.getValue() returns the Person instance for a particular TableView row
 *         return p.getValue().firstNameProperty();
 *     }
 *  });
 * }
 * </code></pre>
 *
 * <p><b>Deploying an Application as a Module</b></p>
 * <p>
 * If the referenced class is in a named module, then it must be reflectively
 * accessible to the {@code javafx.base} module.
 * A class is reflectively accessible if the module
 * {@link Module#isOpen(String,Module) opens} the containing package to at
 * least the {@code javafx.base} module.
 * Otherwise the {@link #call call(TableColumn.CellDataFeatures)} method
 * will log a warning and return {@code null}.
 * </p>
 * <p>
 * For example, if the {@code Person} class is in the {@code com.foo} package
 * in the {@code foo.app} module, the {@code module-info.java} might
 * look like this:
 * </p>
 *
<pre>{@code module foo.app {
    opens com.foo to javafx.base;
}}</pre>
 *
 * <p>
 * Alternatively, a class is reflectively accessible if the module
 * {@link Module#isExported(String) exports} the containing package
 * unconditionally.
 * </p>
 *
 * @see TableColumn
 * @see TableView
 * @see TableCell
 * @see TreeItemPropertyValueFactory
 * @see MapValueFactory
 * @param <S> The type of the class contained within the TableView.items list.
 * @param <T> The type of the class contained within the TableColumn cells.
 * @since JavaFX 2.0
 */
public class PropertyValueFactory<S,T> implements Callback<CellDataFeatures<S,T>, ObservableValue<T>> {

    private final String property;

    private Class<?> columnClass;
    private String previousProperty;
    private PropertyReference<T> propertyRef;

    /**
     * Creates a default PropertyValueFactory to extract the value from a given
     * TableView row item reflectively, using the given property name.
     *
     * @param property The name of the property with which to attempt to
     *      reflectively extract a corresponding value for in a given object.
     */
    public PropertyValueFactory(@NamedArg("property") String property) {
        this.property = property;
    }

    /** {@inheritDoc} */
    @Override public ObservableValue<T> call(CellDataFeatures<S,T> param) {
        return getCellDataReflectively(param.getValue());
    }

    /**
     * Returns the property name provided in the constructor.
     * @return the property name provided in the constructor
     */
    public final String getProperty() { return property; }

    private ObservableValue<T> getCellDataReflectively(S rowData) {
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

            if (propertyRef != null) {
                if (propertyRef.hasProperty()) {
                    return propertyRef.getProperty(rowData);
                } else {
                    T value = propertyRef.get(rowData);
                    return new ReadOnlyObjectWrapper<T>(value);
                }
            }
        } catch (RuntimeException e) {
            // log the warning and move on
            final PlatformLogger logger = Logging.getControlsLogger();
            if (logger.isLoggable(Level.WARNING)) {
               logger.warning("Can not retrieve property '" + getProperty() +
                        "' in PropertyValueFactory: " + this +
                        " with provided class type: " + rowData.getClass(), e);
            }
            propertyRef = null;
        }

        return null;
    }
}
