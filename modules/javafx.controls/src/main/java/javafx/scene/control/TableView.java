/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.IntPredicate;
import com.sun.javafx.collections.MappingChange;
import com.sun.javafx.collections.NonIterableChange;
import com.sun.javafx.logging.PlatformLogger.Level;
import com.sun.javafx.scene.control.ConstrainedColumnResize;
import com.sun.javafx.scene.control.Logging;
import com.sun.javafx.scene.control.Properties;
import com.sun.javafx.scene.control.ReadOnlyUnbackedObservableList;
import com.sun.javafx.scene.control.SelectedCellsMap;
import com.sun.javafx.scene.control.TableColumnComparatorBase.TableColumnComparator;
import com.sun.javafx.scene.control.behavior.TableCellBehavior;
import com.sun.javafx.scene.control.behavior.TableCellBehaviorBase;
import javafx.beans.DefaultProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.collections.transformation.SortedList;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.SizeConverter;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.layout.Region;
import javafx.util.Callback;

/**
 * The TableView control is designed to visualize an unlimited number of rows
 * of data, broken out into columns. A TableView is therefore very similar to the
 * {@link ListView} control, with the addition of support for columns. For an
 * example on how to create a TableView, refer to the 'Creating a TableView'
 * control section below.
 *
 * <p>The TableView control has a number of features, including:
 * <ul>
 * <li>Powerful {@link TableColumn} API:
 *   <ul>
 *   <li>Support for {@link TableColumn#cellFactoryProperty() cell factories} to
 *      easily customize {@link Cell cell} contents in both rendering and editing
 *      states.
 *   <li>Specification of {@link TableColumn#minWidthProperty() minWidth}/
 *      {@link TableColumn#prefWidthProperty() prefWidth}/
 *      {@link TableColumn#maxWidthProperty() maxWidth},
 *      and also {@link TableColumn#resizableProperty() fixed width columns}.
 *   <li>Width resizing by the user at runtime.
 *   <li>Column reordering by the user at runtime.
 *   <li>Built-in support for {@link TableColumn#getColumns() column nesting}
 *   </ul>
 * <li>Different {@link #columnResizePolicyProperty() resizing policies} to
 *      dictate what happens when the user resizes columns.
 * <li>Support for {@link #getSortOrder() multiple column sorting} by clicking
 *      the column header (hold down Shift keyboard key whilst clicking on a
 *      header to sort by multiple columns).
 * </ul>
 *
 * <p>Note that TableView is intended to be used to visualize data - it is not
 * intended to be used for laying out your user interface. If you want to lay
 * your user interface out in a grid-like fashion, consider the
 * {@link javafx.scene.layout.GridPane} layout instead.</p>
 *
 * <h2>Creating a TableView</h2>
 *
 * <p>
 * Creating a TableView is a multi-step process, and also depends on the
 * underlying data model needing to be represented. For this example we'll use
 * an {@literal ObservableList<Person>}, as it is the simplest way of showing data in a
 * TableView. The {@code Person} class will consist of a first
 * name and last name properties. That is:
 *
 * <pre> {@code public class Person {
 *     private StringProperty firstName;
 *     public void setFirstName(String value) { firstNameProperty().set(value); }
 *     public String getFirstName() { return firstNameProperty().get(); }
 *     public StringProperty firstNameProperty() {
 *         if (firstName == null) firstName = new SimpleStringProperty(this, "firstName");
 *         return firstName;
 *     }
 *
 *     private StringProperty lastName;
 *     public void setLastName(String value) { lastNameProperty().set(value); }
 *     public String getLastName() { return lastNameProperty().get(); }
 *     public StringProperty lastNameProperty() {
 *         if (lastName == null) lastName = new SimpleStringProperty(this, "lastName");
 *         return lastName;
 *     }
 *
 *     public Person(String firstName, String lastName) {
 *         setFirstName(firstName);
 *         setLastName(lastName);
 *     }
 * }}</pre>
 *
 * <p>The data we will use for this example is:
 *
 * <pre> {@code List<Person> members = List.of(
 *     new Person("William", "Reed"),
 *     new Person("James", "Michaelson"),
 *     new Person("Julius", "Dean"));}</pre>
 *
 * <p>Firstly, we need to create a data model. As mentioned,
 * for this example, we'll be using an {@literal ObservableList<Person>}:
 *
 * <pre> {@code ObservableList<Person> teamMembers = FXCollections.observableArrayList(members);}</pre>
 *
 * <p>Then we create a TableView instance:
 *
 * <pre> {@code TableView<Person> table = new TableView<>();
 * table.setItems(teamMembers);}</pre>
 *
 * <p>With the items set as such, TableView will automatically update whenever
 * the <code>teamMembers</code> list changes. If the items list is available
 * before the TableView is instantiated, it is possible to pass it directly into
 * the constructor:
 *
 * <pre> {@code TableView<Person> table = new TableView<>(teamMembers);}</pre>
 *
 * <p>At this point we now have a TableView hooked up to observe the
 * <code>teamMembers</code> observableList. The missing ingredient
 * now is the means of splitting out the data contained within the model and
 * representing it in one or more {@link TableColumn TableColumn} instances. To
 * create a two-column TableView to show the firstName and lastName properties,
 * we extend the last code sample as follows:
 *
 * <pre> {@code TableColumn<Person, String> firstNameCol = new TableColumn<>("First Name");
 * firstNameCol.setCellValueFactory(new PropertyValueFactory<>(members.get(0).firstNameProperty().getName())));
 * TableColumn<Person, String> lastNameCol = new TableColumn<>("Last Name");
 * lastNameCol.setCellValueFactory(new PropertyValueFactory<>(members.get(0).lastNameProperty().getName())));
 *
 * table.getColumns().setAll(firstNameCol, lastNameCol);}</pre>
 *
 * <img src="doc-files/TableView.png" alt="Image of the TableView control">
 *
 * <p>With the code shown above we have fully defined the minimum properties
 * required to create a TableView instance. Running this code  will result in the
 * TableView being
 * shown with two columns for firstName and lastName. Any other properties of the
 * Person class will not be shown, as no TableColumns are defined.
 *
 * <h3>TableView support for classes that don't contain properties</h3>
 *
 * <p>The code shown above is the shortest possible code for creating a TableView
 * when the domain objects are designed with JavaFX properties in mind
 * (additionally, {@link javafx.scene.control.cell.PropertyValueFactory} supports
 * normal JavaBean properties too, although there is a caveat to this, so refer
 * to the class documentation for more information). When this is not the case,
 * it is necessary to provide a custom cell value factory. More information
 * about cell value factories can be found in the {@link TableColumn} API
 * documentation, but briefly, here is how a TableColumn could be specified:
 *
 * <pre> {@code firstNameCol.setCellValueFactory(new Callback<CellDataFeatures<Person, String>, ObservableValue<String>>() {
 *     public ObservableValue<String> call(CellDataFeatures<Person, String> p) {
 *         // p.getValue() returns the Person instance for a particular TableView row
 *         return p.getValue().firstNameProperty();
 *     }
 * });
 *
 * // or with a lambda expression:
 * firstNameCol.setCellValueFactory(p -> p.getValue().firstNameProperty());}</pre>
 *
 * <h3>TableView Selection / Focus APIs</h3>
 * <p>To track selection and focus, it is necessary to become familiar with the
 * {@link SelectionModel} and {@link FocusModel} classes. A TableView has at most
 * one instance of each of these classes, available from
 * {@link #selectionModelProperty() selectionModel} and
 * {@link #focusModelProperty() focusModel} properties respectively.
 * Whilst it is possible to use this API to set a new selection model, in
 * most circumstances this is not necessary - the default selection and focus
 * models should work in most circumstances.
 *
 * <p>The default {@link SelectionModel} used when instantiating a TableView is
 * an implementation of the {@link MultipleSelectionModel} abstract class.
 * However, as noted in the API documentation for
 * the {@link MultipleSelectionModel#selectionModeProperty() selectionMode}
 * property, the default value is {@link SelectionMode#SINGLE}. To enable
 * multiple selection in a default TableView instance, it is therefore necessary
 * to do the following:
 *
 * <pre> {@code tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);}</pre>
 *
 * <h3>Customizing TableView Visuals</h3>
 * <p>The visuals of the TableView can be entirely customized by replacing the
 * default {@link #rowFactoryProperty() row factory}. A row factory is used to
 * generate {@link TableRow} instances, which are used to represent an entire
 * row in the TableView.
 *
 * <p>In many cases, this is not what is desired however, as it is more commonly
 * the case that cells be customized on a per-column basis, not a per-row basis.
 * It is therefore important to note that a {@link TableRow} is not a
 * {@link TableCell}. A  {@link TableRow} is simply a container for zero or more
 * {@link TableCell}, and in most circumstances it is more likely that you'll
 * want to create custom TableCells, rather than TableRows. The primary use case
 * for creating custom TableRow instances would most probably be to introduce
 * some form of column spanning support.
 *
 * <p>You can create custom {@link TableCell} instances per column by assigning
 * the appropriate function to the TableColumn
 * {@link TableColumn#cellFactoryProperty() cell factory} property.
 *
 * <p>See the {@link Cell} class documentation for a more complete
 * description of how to write custom Cells.
 *
 * <h4>Warning: Nodes should not be inserted directly into the TableView cells</h4>
 * {@code TableView} allows for it's cells to contain elements of any type, including
 * {@link Node} instances. Putting nodes into
 * the TableView cells is <strong>strongly discouraged</strong>, as it can
 * lead to unexpected results.
 *
 * <p>Important points to note:
 * <ul>
 * <li>Avoid inserting {@code Node} instances directly into the {@code TableView} cells or its data model.</li>
 * <li>The recommended approach is to put the relevant information into the items list, and
 * provide a custom {@link TableColumn#cellFactoryProperty() cell factory} to create the nodes for a
 * given cell and update them on demand using the data stored in the item for that cell.</li>
 * <li>Avoid creating new {@code Node}s in the {@code updateItem} method of a custom {@link TableColumn#cellFactoryProperty() cell factory}.</li>
 * </ul>
 * <p>The following minimal example shows how to create a custom cell factory for {@code TableView} containing {@code Node}s:
 * <pre> {@code
 *  class CustomColor {
 *    private SimpleObjectProperty<Color> color;
 *
 *    CustomColor(Color col) {
 *      this.color = new SimpleObjectProperty<Color>(col);
 *    }
 *    public Color getColor() { return color.getValue(); }
 *    public void setColor(Color c) { color.setValue(c); }
 *    public SimpleObjectProperty<Color> colorProperty() { return color; }
 *  }
 *
 *  TableView<CustomColor> tableview = new TableView<CustomColor>();
 *
 *  ObservableList<CustomColor> colorList = FXCollections.observableArrayList();
 *  colorList.addAll(
 *      new CustomColor(Color.RED),
 *      new CustomColor(Color.GREEN),
 *      new CustomColor(Color.BLUE));
 *
 *  TableColumn<CustomColor, Color> col = new TableColumn<CustomColor, Color>("Color");
 *  col.setCellValueFactory(data -> data.getValue().colorProperty());
 *
 *  col.setCellFactory(p -> {
 *    return new TableCell<CustomColor, Color> () {
 *        private final Rectangle rectangle;
 *        {
 *            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
 *            rectangle = new Rectangle(10, 10);
 *        }
 *
 *        @Override
 *        protected void updateItem(Color item, boolean empty) {
 *          super.updateItem(item, empty);
 *
 *          if (item == null || empty) {
 *              setGraphic(null);
 *          } else {
 *              rectangle.setFill(item);
 *              setGraphic(rectangle);
 *          }
 *        }
 *     };
 *  });
 *
 *  tableview.getColumns().add(col);
 *  tableview.setItems(colorList); }</pre>
 *
 * <p> This example has an anonymous custom {@code TableCell} class in the custom cell factory.
 * Note that the {@code Rectangle} ({@code Node}) object needs to be created in the instance initialization block
 * or the constructor of the custom {@code TableCell} class and updated/used in its {@code updateItem} method.
 *
 * <h3>Sorting</h3>
 * <p>Prior to JavaFX 8.0, the TableView control would treat the
 * {@link #getItems() items} list as the view model, meaning that any changes to
 * the list would be immediately reflected visually. TableView would also modify
 * the order of this list directly when a user initiated a sort. This meant that
 * (again, prior to JavaFX 8.0) it was not possible to have the TableView return
 * to an unsorted state (after iterating through ascending and descending
 * orders).</p>
 *
 * <p>Starting with JavaFX 8.0 (and the introduction of {@link SortedList}), it
 * is now possible to have the collection return to the unsorted state when
 * there are no columns as part of the TableView
 * {@link #getSortOrder() sort order}. To do this, you must create a SortedList
 * instance, and bind its
 * {@link javafx.collections.transformation.SortedList#comparatorProperty() comparator}
 * property to the TableView {@link #comparatorProperty() comparator} property,
 * list so:</p>
 *
 * <pre> {@code // create a SortedList based on the provided ObservableList
 * SortedList sortedList = new SortedList(FXCollections.observableArrayList(2, 1, 3));
 *
 * // create a TableView with the sorted list set as the items it will show
 * final TableView<Integer> tableView = new TableView<>(sortedList);
 *
 * // bind the sortedList comparator to the TableView comparator
 * sortedList.comparatorProperty().bind(tableView.comparatorProperty());
 *
 * // Don't forget to define columns!}</pre>
 *
 * <h3>Editing</h3>
 * <p>This control supports inline editing of values, and this section attempts to
 * give an overview of the available APIs and how you should use them.</p>
 *
 * <p>Firstly, cell editing most commonly requires a different user interface
 * than when a cell is not being edited. This is the responsibility of the
 * {@link Cell} implementation being used. For TableView, it is highly
 * recommended that editing be
 * {@link javafx.scene.control.TableColumn#cellFactoryProperty() per-TableColumn},
 * rather than {@link #rowFactoryProperty() per row}, as more often than not
 * you want users to edit each column value differently, and this approach allows
 * for editors specific to each column. It is your choice whether the cell is
 * permanently in an editing state (e.g. this is common for {@link CheckBox} cells),
 * or to switch to a different UI when editing begins (e.g. when a double-click
 * is received on a cell).</p>
 *
 * <p>To know when editing has been requested on a cell,
 * simply override the {@link javafx.scene.control.Cell#startEdit()} method, and
 * update the cell {@link javafx.scene.control.Cell#textProperty() text} and
 * {@link javafx.scene.control.Cell#graphicProperty() graphic} properties as
 * appropriate (e.g. set the text to null and set the graphic to be a
 * {@link TextField}). Additionally, you should also override
 * {@link Cell#cancelEdit()} to reset the UI back to its original visual state
 * when the editing concludes. In both cases it is important that you also
 * ensure that you call the super method to have the cell perform all duties it
 * must do to enter or exit its editing mode.</p>
 *
 * <p>Once your cell is in an editing state, the next thing you are most probably
 * interested in is how to commit or cancel the editing that is taking place. This is your
 * responsibility as the cell factory provider. Your cell implementation will know
 * when the editing is over, based on the user input (e.g. when the user presses
 * the Enter or ESC keys on their keyboard). When this happens, it is your
 * responsibility to call {@link Cell#commitEdit(Object)} or
 * {@link Cell#cancelEdit()}, as appropriate.</p>
 *
 * <p>When you call {@link Cell#commitEdit(Object)} an event is fired to the
 * TableView, which you can observe by adding an {@link EventHandler} via
 * {@link TableColumn#setOnEditCommit(javafx.event.EventHandler)}. Similarly,
 * you can also observe edit events for
 * {@link TableColumn#setOnEditStart(javafx.event.EventHandler) edit start}
 * and {@link TableColumn#setOnEditCancel(javafx.event.EventHandler) edit cancel}.</p>
 *
 * <p>By default the TableColumn edit commit handler is non-null, with a default
 * handler that attempts to overwrite the property value for the
 * item in the currently-being-edited row. It is able to do this as the
 * {@link Cell#commitEdit(Object)} method is passed in the new value, and this
 * is passed along to the edit commit handler via the
 * {@link javafx.scene.control.TableColumn.CellEditEvent CellEditEvent} that is
 * fired. It is simply a matter of calling
 * {@link javafx.scene.control.TableColumn.CellEditEvent#getNewValue()} to
 * retrieve this value.
 *
 * <p>It is very important to note that if you call
 * {@link TableColumn#setOnEditCommit(javafx.event.EventHandler)} with your own
 * {@link EventHandler}, then you will be removing the default handler. Unless
 * you then handle the writeback to the property (or the relevant data source),
 * nothing will happen. You can work around this by using the
 * {@link TableColumn#addEventHandler(javafx.event.EventType, javafx.event.EventHandler)}
 * method to add a {@link TableColumn#editCommitEvent()} {@link EventType} with
 * your desired {@link EventHandler} as the second argument. Using this method,
 * you will not replace the default implementation, but you will be notified when
 * an edit commit has occurred.</p>
 *
 * <p>Hopefully this summary answers some of the commonly asked questions.
 * Fortunately, JavaFX ships with a number of pre-built cell factories that
 * handle all the editing requirements on your behalf. You can find these
 * pre-built cell factories in the javafx.scene.control.cell package.</p>
 *
 * @see TableColumn
 * @see TablePosition
 * @param <S> The type of the objects contained within the TableView items list.
 * @since JavaFX 2.0
 */
@DefaultProperty("items")
public class TableView<S> extends Control {

    /* *************************************************************************
     *                                                                         *
     * Static properties and methods                                           *
     *                                                                         *
     **************************************************************************/

    // strings used to communicate via the TableView properties map between
    // the control and the skin. Because they are private here, the strings
    // are also duplicated in the TableViewSkin class - so any changes to these
    // strings must also be duplicated there
    static final String SET_CONTENT_WIDTH = "TableView.contentWidth";

    /**
     * <p>Very simple resize policy that just resizes the specified column by the
     * provided delta and shifts all other columns (to the right of the given column)
     * further to the right (when the delta is positive) or to the left (when the
     * delta is negative).
     *
     * <p>It also handles the case where we have nested columns by sharing the new space,
     * or subtracting the removed space, evenly between all immediate children columns.
     * Of course, the immediate children may themselves be nested, and they would
     * then use this policy on their children.
     */
    public static final Callback<ResizeFeatures, Boolean> UNCONSTRAINED_RESIZE_POLICY = new Callback<>() {
        @Override public String toString() {
            return "unconstrained-resize";
        }

        @Override public Boolean call(ResizeFeatures prop) {
            double result = TableUtil.resize(prop.getColumn(), prop.getDelta());
            return Double.compare(result, 0.0) == 0;
        }
    };

    /**
     * A resize policy that adjusts other columns in order to fit the table width.
     * During UI adjustment, proportionately resizes all columns to preserve the total width.
     * <p>
     * When column constraints make it impossible to fit all the columns into the allowed area,
     * the columns are either clipped, or an empty space appears.  This policy disables the horizontal
     * scroll bar.
     *
     * @since 20
     */
    public static final Callback<TableView.ResizeFeatures, Boolean> CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS =
        ConstrainedColumnResize.forTable(ConstrainedColumnResize.ResizeMode.AUTO_RESIZE_ALL_COLUMNS);

    /**
     * A resize policy that adjusts the last column in order to fit the table width.
     * During UI adjustment, resizes the last column only to preserve the total width.
     * <p>
     * When column constraints make it impossible to fit all the columns into the allowed area,
     * the columns are either clipped, or an empty space appears.  This policy disables the horizontal
     * scroll bar.
     *
     * @since 20
     */
    public static final Callback<TableView.ResizeFeatures, Boolean> CONSTRAINED_RESIZE_POLICY_LAST_COLUMN =
        ConstrainedColumnResize.forTable(ConstrainedColumnResize.ResizeMode.AUTO_RESIZE_LAST_COLUMN);

    /**
     * A resize policy that adjusts the next column in order to fit the table width.
     * During UI adjustment, resizes the next column the opposite way.
     * <p>
     * When column constraints make it impossible to fit all the columns into the allowed area,
     * the columns are either clipped, or an empty space appears.  This policy disables the horizontal
     * scroll bar.
     *
     * @since 20
     */
    public static final Callback<TableView.ResizeFeatures, Boolean> CONSTRAINED_RESIZE_POLICY_NEXT_COLUMN =
        ConstrainedColumnResize.forTable(ConstrainedColumnResize.ResizeMode.AUTO_RESIZE_NEXT_COLUMN);

    /**
     * A resize policy that adjusts subsequent columns in order to fit the table width.
     * During UI adjustment, proportionally resizes subsequent columns to preserve the total width.
     * <p>
     * When column constraints make it impossible to fit all the columns into the allowed area,
     * the columns are either clipped, or an empty space appears.  This policy disables the horizontal
     * scroll bar.
     *
     * @since 20
     */
    public static final Callback<TableView.ResizeFeatures, Boolean> CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS =
        ConstrainedColumnResize.forTable(ConstrainedColumnResize.ResizeMode.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

    /**
     * A resize policy that adjusts columns, starting with the next one, in order to fit the table width.
     * During UI adjustment, resizes the next column to preserve the total width.  When the next column
     * cannot be further resized due to a constraint, the following column gets resized, and so on.
     * <p>
     * When column constraints make it impossible to fit all the columns into the allowed area,
     * the columns are either clipped, or an empty space appears.  This policy disables the horizontal
     * scroll bar.
     *
     * @since 20
     */
    public static final Callback<TableView.ResizeFeatures, Boolean> CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN =
        ConstrainedColumnResize.forTable(ConstrainedColumnResize.ResizeMode.AUTO_RESIZE_FLEX_HEAD);

    /**
     * A resize policy that adjusts columns, starting with the last one, in order to fit the table width.
     * During UI adjustment, resizes the last column to preserve the total width.  When the last column
     * cannot be further resized due to a constraint, the column preceding the last one gets resized, and so on.
     * <p>
     * When column constraints make it impossible to fit all the columns into the allowed area,
     * the columns are either clipped, or an empty space appears.  This policy disables the horizontal
     * scroll bar.
     *
     * @since 20
     */
    public static final Callback<TableView.ResizeFeatures, Boolean> CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN =
        ConstrainedColumnResize.forTable(ConstrainedColumnResize.ResizeMode.AUTO_RESIZE_FLEX_TAIL);

    /**
     * <p>Simple policy that ensures the width of all visible leaf columns in
     * this table sum up to equal the width of the table itself.
     *
     * <p>When the user resizes a column width with this policy, the table automatically
     * adjusts the width of the right hand side columns. When the user increases a
     * column width, the table decreases the width of the rightmost column until it
     * reaches its minimum width. Then it decreases the width of the second
     * rightmost column until it reaches minimum width and so on. When all right
     * hand side columns reach minimum size, the user cannot increase the size of
     * resized column any more.
     *
     * @deprecated Use {@link #CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN} instead.
     */
    @Deprecated(since="20")
    public static final Callback<ResizeFeatures, Boolean> CONSTRAINED_RESIZE_POLICY =
        CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN;

    /**
     * The default {@link #sortPolicyProperty() sort policy} that this TableView
     * will use if no other policy is specified. The sort policy is a simple
     * {@link Callback} that accepts a TableView as the sole argument and expects
     * a Boolean response representing whether the sort succeeded or not. A Boolean
     * response of true represents success, and a response of false (or null) will
     * be considered to represent failure.
     * @since JavaFX 8.0
     */
    public static final Callback<TableView, Boolean> DEFAULT_SORT_POLICY = new Callback<>() {
        @Override
        public Boolean call(TableView table) {
            try {
                ObservableList<?> itemsList = table.getItems();
                if (itemsList instanceof SortedList) {
                    // it is the responsibility of the SortedList to bind to the
                    // comparator provided by the TableView. However, we don't
                    // want to fail the sort (which would put the UI in an
                    // inconsistent state), so we return true here, but only if
                    // the SortedList has its comparator bound to the TableView
                    // comparator property.
                    SortedList sortedList = (SortedList) itemsList;
                    boolean comparatorsBound = sortedList.comparatorProperty().
                            isEqualTo(table.comparatorProperty()).get();

                    if (! comparatorsBound) {
                        // this isn't a good situation to be in, so lets log it
                        // out in case the developer is unaware
                        if (Logging.getControlsLogger().isLoggable(Level.INFO)) {
                            String s = "TableView items list is a SortedList, but the SortedList " +
                                    "comparator should be bound to the TableView comparator for " +
                                    "sorting to be enabled (e.g. " +
                                    "sortedList.comparatorProperty().bind(tableView.comparatorProperty());).";
                            Logging.getControlsLogger().info(s);
                        }
                    }
                    return comparatorsBound;
                } else {
                    if (itemsList == null || itemsList.isEmpty()) {
                        // sorting is not supported on null or empty lists
                        return true;
                    }

                    Comparator comparator = table.getComparator();
                    if (comparator == null) {
                        return true;
                    }

                    // otherwise we attempt to do a manual sort, and if successful
                    // we return true
                    FXCollections.sort(itemsList, comparator);
                    return true;
                }
            } catch (UnsupportedOperationException e) {
                // TODO might need to support other exception types including:
                // ClassCastException - if the class of the specified element prevents it from being added to this list
                // NullPointerException - if the specified element is null and this list does not permit null elements
                // IllegalArgumentException - if some property of this element prevents it from being added to this list

                // If we are here the list does not support sorting, so we gracefully
                // fail the sort request and ensure the UI is put back to its previous
                // state. This is handled in the code that calls the sort policy.

                return false;
            }
        }
    };



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default TableView control with no content.
     *
     * <p>Refer to the {@link TableView} class documentation for details on the
     * default state of other properties.
     */
    public TableView() {
        this(FXCollections.<S>observableArrayList());
    }

    /**
     * Creates a TableView with the content provided in the items ObservableList.
     * This also sets up an observer such that any changes to the items list
     * will be immediately reflected in the TableView itself.
     *
     * <p>Refer to the {@link TableView} class documentation for details on the
     * default state of other properties.
     *
     * @param items The items to insert into the TableView, and the list to watch
     *          for changes (to automatically show in the TableView).
     */
    public TableView(ObservableList<S> items) {
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        setAccessibleRole(AccessibleRole.TABLE_VIEW);

        // we quite happily accept items to be null here
        setItems(items);

        // install default selection and focus models
        // it's unlikely this will be changed by many users.
        setSelectionModel(new TableViewArrayListSelectionModel<>(this));
        setFocusModel(new TableViewFocusModel<>(this));

        // we watch the columns list, such that when it changes we can update
        // the leaf columns and visible leaf columns lists (which are read-only).
        getColumns().addListener(weakColumnsObserver);

        // watch for changes to the sort order list - and when it changes run
        // the sort method.
        getSortOrder().addListener((ListChangeListener<TableColumn<S, ?>>) c -> {
            doSort(TableUtil.SortEventType.SORT_ORDER_CHANGE, c);
        });

        // We're watching for changes to the content width such
        // that the resize policy can be run if necessary. This comes from
        // TreeViewSkin.
        getProperties().addListener(new MapChangeListener<>() {
            @Override
            public void onChanged(Change<? extends Object, ? extends Object> c) {
                if (c.wasAdded() && SET_CONTENT_WIDTH.equals(c.getKey())) {
                    if (c.getValueAdded() instanceof Number) {
                        setContentWidth((Double) c.getValueAdded());
                    }
                    getProperties().remove(SET_CONTENT_WIDTH);
                }
            }
        });

        pseudoClassStateChanged(PseudoClass.getPseudoClass(getColumnResizePolicy().toString()), true);

        isInited = true;
    }



    /* *************************************************************************
     *                                                                         *
     * Instance Variables                                                      *
     *                                                                         *
     **************************************************************************/

    // this is the only publicly writable list for columns. This represents the
    // columns as they are given initially by the developer.
    private final ObservableList<TableColumn<S,?>> columns = FXCollections.observableArrayList();

    // Finally, as convenience, we also have an observable list that contains
    // only the leaf columns that are currently visible.
    private final ObservableList<TableColumn<S,?>> visibleLeafColumns = FXCollections.observableArrayList();
    private final ObservableList<TableColumn<S,?>> unmodifiableVisibleLeafColumns = FXCollections.unmodifiableObservableList(visibleLeafColumns);


    // Allows for multiple column sorting based on the order of the TableColumns
    // in this observableArrayList. Each TableColumn is responsible for whether it is
    // sorted using ascending or descending order.
    private ObservableList<TableColumn<S,?>> sortOrder = FXCollections.observableArrayList();

    // width of VirtualFlow minus the vbar width
    private double contentWidth;

    // Used to minimise the amount of work performed prior to the table being
    // completely initialised. In particular it reduces the amount of column
    // resize operations that occur, which slightly improves startup time.
    private boolean isInited = false;



    /* *************************************************************************
     *                                                                         *
     * Callbacks and Events                                                    *
     *                                                                         *
     **************************************************************************/

    private final ListChangeListener<TableColumn<S,?>> columnsObserver = new ListChangeListener<>() {
        @Override public void onChanged(Change<? extends TableColumn<S,?>> c) {
            final List<TableColumn<S,?>> columns = getColumns();

            // Fix for RT-39822 - don't allow the same column to be installed twice
            while (c.next()) {
                if (c.wasAdded()) {
                    List<TableColumn<S,?>> duplicates = new ArrayList<>();
                    for (TableColumn<S,?> addedColumn : c.getAddedSubList()) {
                        if (addedColumn == null) continue;

                        int count = 0;
                        for (TableColumn<S,?> column : columns) {
                            if (addedColumn == column) {
                                count++;
                            }
                        }

                        if (count > 1) {
                            duplicates.add(addedColumn);
                        }
                    }

                    if (!duplicates.isEmpty()) {
                        String titleList = "";
                        for (TableColumn<S,?> dupe : duplicates) {
                            titleList += "'" + dupe.getText() + "', ";
                        }
                        throw new IllegalStateException("Duplicate TableColumns detected in TableView columns list with titles " + titleList);
                    }
                }
            }
            c.reset();

            // Fix for RT-15194: Need to remove removed columns from the
            // sortOrder list.
            List<TableColumn<S,?>> toRemove = new ArrayList<>();
            while (c.next()) {
                final List<? extends TableColumn<S, ?>> removed = c.getRemoved();
                final List<? extends TableColumn<S, ?>> added = c.getAddedSubList();

                if (c.wasRemoved()) {
                    toRemove.addAll(removed);
                    for (TableColumn<S,?> tc : removed) {
                        tc.setTableView(null);
                    }
                }

                if (c.wasAdded()) {
                    toRemove.removeAll(added);
                    for (TableColumn<S,?> tc : added) {
                        tc.setTableView(TableView.this);
                    }
                }

                // set up listeners
                TableUtil.removeColumnsListener(removed, weakColumnsObserver);
                TableUtil.addColumnsListener(added, weakColumnsObserver);

                TableUtil.removeTableColumnListener(c.getRemoved(),
                        weakColumnVisibleObserver,
                        weakColumnSortableObserver,
                        weakColumnSortTypeObserver,
                        weakColumnComparatorObserver);
                TableUtil.addTableColumnListener(c.getAddedSubList(),
                        weakColumnVisibleObserver,
                        weakColumnSortableObserver,
                        weakColumnSortTypeObserver,
                        weakColumnComparatorObserver);
            }

            // We don't maintain a bind for leafColumns, we simply call this update
            // function behind the scenes in the appropriate places.
            updateVisibleLeafColumns();

            sortOrder.removeAll(toRemove);

            // Fix for RT-38892.
            final TableViewFocusModel<S> fm = getFocusModel();
            final TableViewSelectionModel<S> sm = getSelectionModel();
            c.reset();

            // we need to collect together all removed and all added columns, because
            // the code below works on the actually removed columns. If we perform
            // the code within this while loop, we'll be deselecting columns that
            // should be deselected (because they have just moved place, for example).
            List<TableColumn<S,?>> removed = new ArrayList<>();
            List<TableColumn<S,?>> added = new ArrayList<>();
            while (c.next()) {
                if (c.wasRemoved()) {
                    removed.addAll(c.getRemoved());
                }
                if (c.wasAdded()) {
                    added.addAll(c.getAddedSubList());
                }
            }
            removed.removeAll(added);

            // Fix for focus - we simply move focus to a cell to the left
            // of the focused cell if the focused cell was located within
            // a column that has been removed.
            if (fm != null) {
                TablePosition<S, ?> focusedCell = fm.getFocusedCell();
                boolean match = false;
                for (TableColumn<S, ?> tc : removed) {
                    match = focusedCell != null && focusedCell.getTableColumn() == tc;
                    if (match) {
                        break;
                    }
                }

                if (match) {
                    int matchingColumnIndex = lastKnownColumnIndex.getOrDefault(focusedCell.getTableColumn(), 0);
                    int newFocusColumnIndex =
                            matchingColumnIndex == 0 ? 0 :
                            Math.min(getVisibleLeafColumns().size() - 1, matchingColumnIndex - 1);
                    fm.focus(focusedCell.getRow(), getVisibleLeafColumn(newFocusColumnIndex));
                }
            }

            // Fix for selection - we remove selection from all cells that
            // were within the removed column.
            if (sm != null) {
                List<TablePosition> selectedCells = new ArrayList<>(sm.getSelectedCells());
                for (TablePosition selectedCell : selectedCells) {
                    boolean match = false;
                    for (TableColumn<S, ?> tc : removed) {
                        match = selectedCell != null && selectedCell.getTableColumn() == tc;
                        if (match) break;
                    }

                    if (match) {
                        // we can't just use the selectedCell.getTableColumn(), as that
                        // column no longer exists and therefore its index is not correct.
                        int matchingColumnIndex = lastKnownColumnIndex.getOrDefault(selectedCell.getTableColumn(), -1);
                        if (matchingColumnIndex == -1) continue;

                        if (sm instanceof TableViewArrayListSelectionModel) {
                            // Also, because the table column no longer exists in the columns
                            // list at this point, we can't just call:
                            // sm.clearSelection(selectedCell.getRow(), selectedCell.getTableColumn());
                            // as the tableColumn would map to an index of -1, which means that
                            // selection will not be cleared. Instead, we have to create
                            // a new TablePosition with a fixed column index and use that.
                            TablePosition<S,?> fixedTablePosition =
                                    new TablePosition<>(TableView.this,
                                            selectedCell.getRow(),
                                            selectedCell.getTableColumn());
                            fixedTablePosition.fixedColumnIndex = matchingColumnIndex;

                            ((TableViewArrayListSelectionModel)sm).clearSelection(fixedTablePosition);
                        } else {
                            sm.clearSelection(selectedCell.getRow(), selectedCell.getTableColumn());
                        }
                    }
                }
            }


            // update the lastKnownColumnIndex map
            lastKnownColumnIndex.clear();
            for (TableColumn<S,?> tc : getColumns()) {
                int index = getVisibleLeafIndex(tc);
                if (index > -1) {
                    lastKnownColumnIndex.put(tc, index);
                }
            }
        }
    };

    private final WeakHashMap<TableColumn<S,?>, Integer> lastKnownColumnIndex = new WeakHashMap<>();

    private final InvalidationListener columnVisibleObserver = valueModel -> {
        updateVisibleLeafColumns();
    };

    private final InvalidationListener columnSortableObserver = valueModel -> {
        Object col = ((Property<?>)valueModel).getBean();
        if (! getSortOrder().contains(col)) return;
        doSort(TableUtil.SortEventType.COLUMN_SORTABLE_CHANGE, col);
    };

    private final InvalidationListener columnSortTypeObserver = valueModel -> {
        Object col = ((Property<?>)valueModel).getBean();
        if (! getSortOrder().contains(col)) return;
        doSort(TableUtil.SortEventType.COLUMN_SORT_TYPE_CHANGE, col);
    };

    private final InvalidationListener columnComparatorObserver = valueModel -> {
        Object col = ((Property<?>)valueModel).getBean();
        if (! getSortOrder().contains(col)) return;
        doSort(TableUtil.SortEventType.COLUMN_COMPARATOR_CHANGE, col);
    };

    /* proxy pseudo-class state change from selectionModel's cellSelectionEnabledProperty */
    private final InvalidationListener cellSelectionModelInvalidationListener = o -> {
        final boolean isCellSelection = ((BooleanProperty)o).get();
        pseudoClassStateChanged(PSEUDO_CLASS_CELL_SELECTION,  isCellSelection);
        pseudoClassStateChanged(PSEUDO_CLASS_ROW_SELECTION,  !isCellSelection);
    };


    private final WeakInvalidationListener weakColumnVisibleObserver =
            new WeakInvalidationListener(columnVisibleObserver);

    private final WeakInvalidationListener weakColumnSortableObserver =
            new WeakInvalidationListener(columnSortableObserver);

    private final WeakInvalidationListener weakColumnSortTypeObserver =
            new WeakInvalidationListener(columnSortTypeObserver);

    private final WeakInvalidationListener weakColumnComparatorObserver =
            new WeakInvalidationListener(columnComparatorObserver);

    private final WeakListChangeListener<TableColumn<S,?>> weakColumnsObserver =
            new WeakListChangeListener<>(columnsObserver);

    private final WeakInvalidationListener weakCellSelectionModelInvalidationListener =
            new WeakInvalidationListener(cellSelectionModelInvalidationListener);



    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/


    // --- Items
    /**
     * The underlying data model for the TableView. Note that it has a generic
     * type that must match the type of the TableView itself.
     * @return the items property
     */
    public final ObjectProperty<ObservableList<S>> itemsProperty() { return items; }
    private ObjectProperty<ObservableList<S>> items =
        new SimpleObjectProperty<>(this, "items") {
            WeakReference<ObservableList<S>> oldItemsRef;

            @Override protected void invalidated() {
                final ObservableList<S> oldItems = oldItemsRef == null ? null : oldItemsRef.get();
                final ObservableList<S> newItems = getItems();

                // Fix for RT-36425
                if (newItems != null && newItems == oldItems) {
                    return;
                }

                // Fix for RT-35763
                if (! (newItems instanceof SortedList)) {
                    getSortOrder().clear();
                }

                oldItemsRef = new WeakReference<>(newItems);
            }
        };
    public final void setItems(ObservableList<S> value) { itemsProperty().set(value); }
    public final ObservableList<S> getItems() {return items.get(); }


    // --- Table menu button visible
    private BooleanProperty tableMenuButtonVisible;
    /**
     * This controls whether a menu button is available when the user clicks
     * in a designated space within the TableView, within which is a radio menu
     * item for each TableColumn in this table. This menu allows for the user to
     * show and hide all TableColumns easily.
     * @return the tableMenuButtonVisible property
     */
    public final BooleanProperty tableMenuButtonVisibleProperty() {
        if (tableMenuButtonVisible == null) {
            tableMenuButtonVisible = new SimpleBooleanProperty(this, "tableMenuButtonVisible");
        }
        return tableMenuButtonVisible;
    }
    public final void setTableMenuButtonVisible (boolean value) {
        tableMenuButtonVisibleProperty().set(value);
    }
    public final boolean isTableMenuButtonVisible() {
        return tableMenuButtonVisible == null ? false : tableMenuButtonVisible.get();
    }


    // --- Column Resize Policy
    private ObjectProperty<Callback<ResizeFeatures, Boolean>> columnResizePolicy;
    public final void setColumnResizePolicy(Callback<ResizeFeatures, Boolean> callback) {
        columnResizePolicyProperty().set(callback);
    }
    public final Callback<ResizeFeatures, Boolean> getColumnResizePolicy() {
        return columnResizePolicy == null ? UNCONSTRAINED_RESIZE_POLICY : columnResizePolicy.get();
    }

    /**
     * Called when the user completes a column-resize operation. The two most common
     * policies are available as static functions in the TableView class:
     * {@link #UNCONSTRAINED_RESIZE_POLICY} and {@link #CONSTRAINED_RESIZE_POLICY}.
     * @return columnResizePolicy property
     */
    public final ObjectProperty<Callback<ResizeFeatures, Boolean>> columnResizePolicyProperty() {
        if (columnResizePolicy == null) {
            columnResizePolicy = new SimpleObjectProperty<>(this, "columnResizePolicy", UNCONSTRAINED_RESIZE_POLICY) {
                private Callback<ResizeFeatures, Boolean> oldPolicy;

                @Override protected void invalidated() {
                    if (isInited) {
                        get().call(new ResizeFeatures(TableView.this, null, 0.0));

                        if (oldPolicy != null) {
                            PseudoClass state = PseudoClass.getPseudoClass(oldPolicy.toString());
                            pseudoClassStateChanged(state, false);
                        }
                        if (get() != null) {
                            PseudoClass state = PseudoClass.getPseudoClass(get().toString());
                            pseudoClassStateChanged(state, true);
                        }
                        oldPolicy = get();
                    }
                }
            };
        }
        return columnResizePolicy;
    }


    // --- Row Factory
    private ObjectProperty<Callback<TableView<S>, TableRow<S>>> rowFactory;

    /**
     * A function which produces a TableRow. The system is responsible for
     * reusing TableRows. Return from this function a TableRow which
     * might be usable for representing a single row in a TableView.
     * <p>
     * Note that a TableRow is <b>not</b> a TableCell. A TableRow is
     * simply a container for a TableCell, and in most circumstances it is more
     * likely that you'll want to create custom TableCells, rather than
     * TableRows. The primary use case for creating custom TableRow
     * instances would most probably be to introduce some form of column
     * spanning support.
     * <p>
     * You can create custom TableCell instances per column by assigning the
     * appropriate function to the cellFactory property in the TableColumn class.
     * @return rowFactory property
     */
    public final ObjectProperty<Callback<TableView<S>, TableRow<S>>> rowFactoryProperty() {
        if (rowFactory == null) {
            rowFactory = new SimpleObjectProperty<>(this, "rowFactory");
        }
        return rowFactory;
    }
    public final void setRowFactory(Callback<TableView<S>, TableRow<S>> value) {
        rowFactoryProperty().set(value);
    }
    public final Callback<TableView<S>, TableRow<S>> getRowFactory() {
        return rowFactory == null ? null : rowFactory.get();
    }


    // --- Placeholder Node
    private ObjectProperty<Node> placeholder;
    /**
     * This Node is shown to the user when the table has no content to show.
     * This may be the case because the table model has no data in the first
     * place, that a filter has been applied to the table model, resulting
     * in there being nothing to show the user, or that there are no currently
     * visible columns.
     * @return placeholder property
     */
    public final ObjectProperty<Node> placeholderProperty() {
        if (placeholder == null) {
            placeholder = new SimpleObjectProperty<>(this, "placeholder");
        }
        return placeholder;
    }
    public final void setPlaceholder(Node value) {
        placeholderProperty().set(value);
    }
    public final Node getPlaceholder() {
        return placeholder == null ? null : placeholder.get();
    }


    // --- Selection Model
    private ObjectProperty<TableViewSelectionModel<S>> selectionModel
            = new SimpleObjectProperty<>(this, "selectionModel") {

        TableViewSelectionModel<S> oldValue = null;

        @Override protected void invalidated() {

            if (oldValue != null) {
                oldValue.clearSelection();
                oldValue.cellSelectionEnabledProperty().removeListener(weakCellSelectionModelInvalidationListener);

                if (oldValue instanceof TableViewArrayListSelectionModel) {
                    ((TableViewArrayListSelectionModel)oldValue).dispose();
                }
            }

            oldValue = get();

            if (oldValue != null) {
                oldValue.cellSelectionEnabledProperty().addListener(weakCellSelectionModelInvalidationListener);
                // fake an invalidation to ensure updated pseudo-class state
                weakCellSelectionModelInvalidationListener.invalidated(oldValue.cellSelectionEnabledProperty());
            }
        }
    };

    /**
     * The SelectionModel provides the API through which it is possible
     * to select single or multiple items within a TableView, as  well as inspect
     * which items have been selected by the user. Note that it has a generic
     * type that must match the type of the TableView itself.
     * @return selectionModel property
     */
    public final ObjectProperty<TableViewSelectionModel<S>> selectionModelProperty() {
        return selectionModel;
    }
    public final void setSelectionModel(TableViewSelectionModel<S> value) {
        selectionModelProperty().set(value);
    }

    public final TableViewSelectionModel<S> getSelectionModel() {
        return selectionModel.get();
    }


    // --- Focus Model
    private ObjectProperty<TableViewFocusModel<S>> focusModel;
    public final void setFocusModel(TableViewFocusModel<S> value) {
        focusModelProperty().set(value);
    }
    public final TableViewFocusModel<S> getFocusModel() {
        return focusModel == null ? null : focusModel.get();
    }
    /**
     * Represents the currently-installed {@link TableViewFocusModel} for this
     * TableView. Under almost all circumstances leaving this as the default
     * focus model will suffice.
     * @return focusModel property
     */
    public final ObjectProperty<TableViewFocusModel<S>> focusModelProperty() {
        if (focusModel == null) {
            focusModel = new SimpleObjectProperty<>(this, "focusModel");
        }
        return focusModel;
    }


//    // --- Span Model
//    private ObjectProperty<SpanModel<S>> spanModel
//            = new SimpleObjectProperty<SpanModel<S>>(this, "spanModel") {
//
//        @Override protected void invalidated() {
//            ObservableList<String> styleClass = getStyleClass();
//            if (getSpanModel() == null) {
//                styleClass.remove(CELL_SPAN_TABLE_VIEW_STYLE_CLASS);
//            } else if (! styleClass.contains(CELL_SPAN_TABLE_VIEW_STYLE_CLASS)) {
//                styleClass.add(CELL_SPAN_TABLE_VIEW_STYLE_CLASS);
//            }
//        }
//    };
//
//    public final ObjectProperty<SpanModel<S>> spanModelProperty() {
//        return spanModel;
//    }
//    public final void setSpanModel(SpanModel<S> value) {
//        spanModelProperty().set(value);
//    }
//
//    public final SpanModel<S> getSpanModel() {
//        return spanModel.get();
//    }

    // --- Editable
    private BooleanProperty editable;
    public final void setEditable(boolean value) {
        editableProperty().set(value);
    }
    public final boolean isEditable() {
        return editable == null ? false : editable.get();
    }
    /**
     * Specifies whether this TableView is editable - only if the TableView, the
     * TableColumn (if applicable) and the TableCells within it are both
     * editable will a TableCell be able to go into their editing state.
     * @return the editable property
     */
    public final BooleanProperty editableProperty() {
        if (editable == null) {
            editable = new SimpleBooleanProperty(this, "editable", false);
        }
        return editable;
    }


    // --- Fixed cell size
    private DoubleProperty fixedCellSize;

    /**
     * Sets the new fixed cell size for this control. Any value greater than
     * zero will enable fixed cell size mode, whereas a zero or negative value
     * (or Region.USE_COMPUTED_SIZE) will be used to disabled fixed cell size
     * mode.
     *
     * @param value The new fixed cell size value, or a value less than or equal
     *              to zero (or Region.USE_COMPUTED_SIZE) to disable.
     * @since JavaFX 8.0
     */
    public final void setFixedCellSize(double value) {
        fixedCellSizeProperty().set(value);
    }

    /**
     * Returns the fixed cell size value. A value less than or equal to zero is
     * used to represent that fixed cell size mode is disabled, and a value
     * greater than zero represents the size of all cells in this control.
     *
     * @return A double representing the fixed cell size of this control, or a
     *      value less than or equal to zero if fixed cell size mode is disabled.
     * @since JavaFX 8.0
     */
    public final double getFixedCellSize() {
        return fixedCellSize == null ? Region.USE_COMPUTED_SIZE : fixedCellSize.get();
    }
    /**
     * Specifies whether this control has cells that are a fixed height (of the
     * specified value). If this value is less than or equal to zero,
     * then all cells are individually sized and positioned. This is a slow
     * operation. Therefore, when performance matters and developers are not
     * dependent on variable cell sizes it is a good idea to set the fixed cell
     * size value. Generally cells are around 24px, so setting a fixed cell size
     * of 24 is likely to result in very little difference in visuals, but a
     * improvement to performance.
     *
     * <p>To set this property via CSS, use the -fx-fixed-cell-size property.
     * This should not be confused with the -fx-cell-size property. The difference
     * between these two CSS properties is that -fx-cell-size will size all
     * cells to the specified size, but it will not enforce that this is the
     * only size (thus allowing for variable cell sizes, and preventing the
     * performance gains from being possible). Therefore, when performance matters
     * use -fx-fixed-cell-size, instead of -fx-cell-size. If both properties are
     * specified in CSS, -fx-fixed-cell-size takes precedence.</p>
     *
     * @return fixedCellSize property
     * @since JavaFX 8.0
     */
    public final DoubleProperty fixedCellSizeProperty() {
        if (fixedCellSize == null) {
            fixedCellSize = new StyleableDoubleProperty(Region.USE_COMPUTED_SIZE) {
                @Override public CssMetaData<TableView<?>,Number> getCssMetaData() {
                    return StyleableProperties.FIXED_CELL_SIZE;
                }

                @Override public Object getBean() {
                    return TableView.this;
                }

                @Override public String getName() {
                    return "fixedCellSize";
                }
            };
        }
        return fixedCellSize;
    }


    // --- Editing Cell
    private ReadOnlyObjectWrapper<TablePosition<S,?>> editingCell;
    private void setEditingCell(TablePosition<S,?> value) {
        editingCellPropertyImpl().set(value);
    }
    public final TablePosition<S,?> getEditingCell() {
        return editingCell == null ? null : editingCell.get();
    }

    /**
     * Represents the current cell being edited, or null if
     * there is no cell being edited.
     * @return the editingCell property
     */
    public final ReadOnlyObjectProperty<TablePosition<S,?>> editingCellProperty() {
        return editingCellPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<TablePosition<S,?>> editingCellPropertyImpl() {
        if (editingCell == null) {
            editingCell = new ReadOnlyObjectWrapper<>(this, "editingCell");
        }
        return editingCell;
    }


    // --- Comparator (built via sortOrder list, so read-only)
    /**
     * The comparator property is a read-only property that is representative of the
     * current state of the {@link #getSortOrder() sort order} list. The sort
     * order list contains the columns that have been added to it either programmatically
     * or via a user clicking on the headers themselves.
     * @since JavaFX 8.0
     */
    private ReadOnlyObjectWrapper<Comparator<S>> comparator;
    private void setComparator(Comparator<S> value) {
        comparatorPropertyImpl().set(value);
    }
    public final Comparator<S> getComparator() {
        return comparator == null ? null : comparator.get();
    }
    public final ReadOnlyObjectProperty<Comparator<S>> comparatorProperty() {
        return comparatorPropertyImpl().getReadOnlyProperty();
    }
    private ReadOnlyObjectWrapper<Comparator<S>> comparatorPropertyImpl() {
        if (comparator == null) {
            comparator = new ReadOnlyObjectWrapper<>(this, "comparator");
        }
        return comparator;
    }


    // --- sortPolicy
    /**
     * The sort policy specifies how sorting in this TableView should be performed.
     * For example, a basic sort policy may just call
     * {@code FXCollections.sort(tableView.getItems())}, whereas a more advanced
     * sort policy may call to a database to perform the necessary sorting on the
     * server-side.
     *
     * <p>TableView ships with a {@link TableView#DEFAULT_SORT_POLICY default
     * sort policy} that does precisely as mentioned above: it simply attempts
     * to sort the items list in-place.
     *
     * <p>It is recommended that rather than override the {@link TableView#sort() sort}
     * method that a different sort policy be provided instead.
     * @since JavaFX 8.0
     */
    private ObjectProperty<Callback<TableView<S>, Boolean>> sortPolicy;
    public final void setSortPolicy(Callback<TableView<S>, Boolean> callback) {
        sortPolicyProperty().set(callback);
    }
    @SuppressWarnings("unchecked")
    public final Callback<TableView<S>, Boolean> getSortPolicy() {
        return sortPolicy == null ?
                (Callback<TableView<S>, Boolean>)(Object) DEFAULT_SORT_POLICY :
                sortPolicy.get();
    }
    @SuppressWarnings("unchecked")
    public final ObjectProperty<Callback<TableView<S>, Boolean>> sortPolicyProperty() {
        if (sortPolicy == null) {
            sortPolicy = new SimpleObjectProperty<>(
                    this, "sortPolicy", (Callback<TableView<S>, Boolean>)(Object) DEFAULT_SORT_POLICY) {
                @Override protected void invalidated() {
                    sort();
                }
            };
        }
        return sortPolicy;
    }


    // onSort
    /**
     * Called when there's a request to sort the control.
     * @since JavaFX 8.0
     */
    private ObjectProperty<EventHandler<SortEvent<TableView<S>>>> onSort;

    public void setOnSort(EventHandler<SortEvent<TableView<S>>> value) {
        onSortProperty().set(value);
    }

    public EventHandler<SortEvent<TableView<S>>> getOnSort() {
        if( onSort != null ) {
            return onSort.get();
        }
        return null;
    }

    public ObjectProperty<EventHandler<SortEvent<TableView<S>>>> onSortProperty() {
        if( onSort == null ) {
            onSort = new ObjectPropertyBase<>() {
                @Override protected void invalidated() {
                    EventType<SortEvent<TableView<S>>> eventType = SortEvent.sortEvent();
                    EventHandler<SortEvent<TableView<S>>> eventHandler = get();
                    setEventHandler(eventType, eventHandler);
                }

                @Override public Object getBean() {
                    return TableView.this;
                }

                @Override public String getName() {
                    return "onSort";
                }
            };
        }
        return onSort;
    }


    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/
    /**
     * The TableColumns that are part of this TableView. As the user reorders
     * the TableView columns, this list will be updated to reflect the current
     * visual ordering.
     *
     * <p>Note: to display any data in a TableView, there must be at least one
     * TableColumn in this ObservableList.</p>
     * @return the columns
     */
    public final ObservableList<TableColumn<S,?>> getColumns() {
        return columns;
    }

    /**
     * The sortOrder list defines the order in which {@link TableColumn} instances
     * are sorted. An empty sortOrder list means that no sorting is being applied
     * on the TableView. If the sortOrder list has one TableColumn within it,
     * the TableView will be sorted using the
     * {@link TableColumn#sortTypeProperty() sortType} and
     * {@link TableColumn#comparatorProperty() comparator} properties of this
     * TableColumn (assuming
     * {@link TableColumn#sortableProperty() TableColumn.sortable} is true).
     * If the sortOrder list contains multiple TableColumn instances, then
     * the TableView is firstly sorted based on the properties of the first
     * TableColumn. If two elements are considered equal, then the second
     * TableColumn in the list is used to determine ordering. This repeats until
     * the results from all TableColumn comparators are considered, if necessary.
     *
     * @return An ObservableList containing zero or more TableColumn instances.
     */
    public final ObservableList<TableColumn<S,?>> getSortOrder() {
        return sortOrder;
    }

    /**
     * Scrolls the TableView so that the given index is visible within the viewport.
     * @param index The index of an item that should be visible to the user.
     */
    public void scrollTo(int index) {
       ControlUtils.scrollToIndex(this, index);
    }

    /**
     * Scrolls the TableView so that the given object is visible within the viewport.
     * @param object The object that should be visible to the user.
     * @since JavaFX 8.0
     */
    public void scrollTo(S object) {
        if( getItems() != null ) {
            int idx = getItems().indexOf(object);
            if( idx >= 0 ) {
                ControlUtils.scrollToIndex(this, idx);
            }
        }
    }

    /**
     * Called when there's a request to scroll an index into view using {@link #scrollTo(int)}
     * or {@link #scrollTo(Object)}
     * @since JavaFX 8.0
     */
    private ObjectProperty<EventHandler<ScrollToEvent<Integer>>> onScrollTo;

    public void setOnScrollTo(EventHandler<ScrollToEvent<Integer>> value) {
        onScrollToProperty().set(value);
    }

    public EventHandler<ScrollToEvent<Integer>> getOnScrollTo() {
        if( onScrollTo != null ) {
            return onScrollTo.get();
        }
        return null;
    }

    public ObjectProperty<EventHandler<ScrollToEvent<Integer>>> onScrollToProperty() {
        if( onScrollTo == null ) {
            onScrollTo = new ObjectPropertyBase<>() {
                @Override
                protected void invalidated() {
                    setEventHandler(ScrollToEvent.scrollToTopIndex(), get());
                }
                @Override
                public Object getBean() {
                    return TableView.this;
                }

                @Override
                public String getName() {
                    return "onScrollTo";
                }
            };
        }
        return onScrollTo;
    }

    /**
     * Scrolls the TableView so that the given column is visible within the viewport.
     * @param column The column that should be visible to the user.
     * @since JavaFX 8.0
     */
    public void scrollToColumn(TableColumn<S, ?> column) {
        ControlUtils.scrollToColumn(this, column);
    }

    /**
     * Scrolls the TableView so that the given index is visible within the viewport.
     * @param columnIndex The index of a column that should be visible to the user.
     * @since JavaFX 8.0
     */
    public void scrollToColumnIndex(int columnIndex) {
        if( getColumns() != null ) {
            ControlUtils.scrollToColumn(this, getColumns().get(columnIndex));
        }
    }

    /**
     * Called when there's a request to scroll a column into view using {@link #scrollToColumn(TableColumn)}
     * or {@link #scrollToColumnIndex(int)}
     * @since JavaFX 8.0
     */
    private ObjectProperty<EventHandler<ScrollToEvent<TableColumn<S, ?>>>> onScrollToColumn;

    public void setOnScrollToColumn(EventHandler<ScrollToEvent<TableColumn<S, ?>>> value) {
        onScrollToColumnProperty().set(value);
    }

    public EventHandler<ScrollToEvent<TableColumn<S, ?>>> getOnScrollToColumn() {
        if( onScrollToColumn != null ) {
            return onScrollToColumn.get();
        }
        return null;
    }

    public ObjectProperty<EventHandler<ScrollToEvent<TableColumn<S, ?>>>> onScrollToColumnProperty() {
        if( onScrollToColumn == null ) {
            onScrollToColumn = new ObjectPropertyBase<>() {
                @Override protected void invalidated() {
                    EventType<ScrollToEvent<TableColumn<S, ?>>> type = ScrollToEvent.scrollToColumn();
                    setEventHandler(type, get());
                }

                @Override public Object getBean() {
                    return TableView.this;
                }

                @Override public String getName() {
                    return "onScrollToColumn";
                }
            };
        }
        return onScrollToColumn;
    }

    /**
     * Applies the currently installed resize policy against the given column,
     * resizing it based on the delta value provided.
     * @param column the column
     * @param delta the delta
     * @return true if column resize is allowed
     */
    public boolean resizeColumn(TableColumn<S,?> column, double delta) {
        if (column == null || Double.compare(delta, 0.0) == 0) return false;

        boolean allowed = getColumnResizePolicy().call(new ResizeFeatures<>(TableView.this, column, delta));
        if (!allowed) return false;

        return true;
    }

    /**
     * Causes the cell at the given row/column view indexes to switch into
     * its editing state, if it is not already in it, and assuming that the
     * TableView and column are also editable.
     *
     * <p><strong>Note:</strong> This method will cancel editing if the given row
     * value is less than zero and the given column is null.</p>
     * @param row the row
     * @param column the column
     */
    public void edit(int row, TableColumn<S,?> column) {
        if (!isEditable() || (column != null && ! column.isEditable())) {
            return;
        }

        if (row < 0 && column == null) {
            setEditingCell(null);
        } else {
            setEditingCell(new TablePosition<>(this, row, column));
        }
    }

    /**
     * Returns an unmodifiable list containing the currently visible leaf columns.
     * @return an unmodifiable list containing the currently visible leaf columns
     */
    public ObservableList<TableColumn<S,?>> getVisibleLeafColumns() {
        return unmodifiableVisibleLeafColumns;
    }

    /**
     * Returns the position of the given column, relative to all other
     * visible leaf columns.
     * @param column the column
     * @return the position of the given column, relative to all other
     * visible leaf columns
     */
    public int getVisibleLeafIndex(TableColumn<S,?> column) {
        return visibleLeafColumns.indexOf(column);
    }

    /**
     * Returns the TableColumn in the given column index, relative to all other
     * visible leaf columns.
     * @param column the column
     * @return the TableColumn in the given column index, relative to all other
     * visible leaf columns
     */
    public TableColumn<S,?> getVisibleLeafColumn(int column) {
        if (column < 0 || column >= visibleLeafColumns.size()) return null;
        return visibleLeafColumns.get(column);
    }

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new TableViewSkin<>(this);
    }

    /**
     * The sort method forces the TableView to re-run its sorting algorithm. More
     * often than not it is not necessary to call this method directly, as it is
     * automatically called when the {@link #getSortOrder() sort order},
     * {@link #sortPolicyProperty() sort policy}, or the state of the
     * TableColumn {@link TableColumn#sortTypeProperty() sort type} properties
     * change. In other words, this method should only be called directly when
     * something external changes and a sort is required.
     * @since JavaFX 8.0
     */
    public void sort() {
        final ObservableList<? extends TableColumnBase<S,?>> sortOrder = getSortOrder();

        // update the Comparator property
        final Comparator<S> oldComparator = getComparator();
        setComparator(sortOrder.isEmpty() ? null : new TableColumnComparator(sortOrder));

        // fire the onSort event and check if it is consumed, if
        // so, don't run the sort
        SortEvent<TableView<S>> sortEvent = new SortEvent<>(TableView.this, TableView.this);
        fireEvent(sortEvent);
        if (sortEvent.isConsumed()) {
            // if the sort is consumed we could back out the last action (the code
            // is commented out right below), but we don't as we take it as a
            // sign that the developer has decided to handle the event themselves.

            // sortLock = true;
            // TableUtil.handleSortFailure(sortOrder, lastSortEventType, lastSortEventSupportInfo);
            // sortLock = false;
            return;
        }

        TableViewSelectionModel<S> selectionModel = getSelectionModel();
        final List<TablePosition> prevState = selectionModel == null ?
                null :
                new ArrayList<>(selectionModel.getSelectedCells());

        // we set makeAtomic to true here, so that we don't fire intermediate
        // sort events - instead we send a single permutation event at the end
        // of this method.
        if (selectionModel != null) {
            selectionModel.startAtomic();
        }

        // get the sort policy and run it
        Callback<TableView<S>, Boolean> sortPolicy = getSortPolicy();
        if (sortPolicy == null) return;
        Boolean success = sortPolicy.call(this);

        if (selectionModel != null) {
            selectionModel.stopAtomic();
        }

        if (success == null || ! success) {
            // the sort was a failure. Need to backout if possible
            sortLock = true;
            TableUtil.handleSortFailure(sortOrder, lastSortEventType, lastSortEventSupportInfo);
            setComparator(oldComparator);
            sortLock = false;
        } else {
            // sorting was a success, now we possibly fire an event on the
            // selection model that the items list has 'permutated' to a new ordering

            // FIXME we should support alternative selection model implementations!
            if (selectionModel instanceof TableViewArrayListSelectionModel) {
                final TableViewArrayListSelectionModel<S> sm = (TableViewArrayListSelectionModel<S>)selectionModel;
                final ObservableList<TablePosition<S,?>> newState = (ObservableList<TablePosition<S,?>>)(Object)sm.getSelectedCells();

                List<TablePosition<S, ?>> removed = new ArrayList<>();
                if (prevState != null) {
                    for (TablePosition<S, ?> prevItem : prevState) {
                        if (!newState.contains(prevItem)) {
                            removed.add(prevItem);
                        }
                    }
                }

                if (!removed.isEmpty()) {
                    // the sort operation effectively permutates the selectedCells list,
                    // but we cannot fire a permutation event as we are talking about
                    // TablePosition's changing (which may reside in the same list
                    // position before and after the sort). Therefore, we need to fire
                    // a single add/remove event to cover the added and removed positions.
                    int itemCount = prevState == null ? 0 : prevState.size();
                    ListChangeListener.Change<TablePosition<S, ?>> c = new NonIterableChange.GenericAddRemoveChange<>(0, itemCount, removed, newState);
                    sm.fireCustomSelectedCellsListChangeEvent(c);
                }
            }
        }
    }

    /**
     * Calling {@code refresh()} forces the TableView control to recreate and
     * repopulate the cells necessary to populate the visual bounds of the control.
     * In other words, this forces the TableView to update what it is showing to
     * the user. This is useful in cases where the underlying data source has
     * changed in a way that is not observed by the TableView itself.
     *
     * @since JavaFX 8u60
     */
    public void refresh() {
        getProperties().put(Properties.RECREATE, Boolean.TRUE);
    }



    /* *************************************************************************
     *                                                                         *
     * Private Implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private boolean sortLock = false;
    private TableUtil.SortEventType lastSortEventType = null;
    private Object[] lastSortEventSupportInfo = null;

    private void doSort(final TableUtil.SortEventType sortEventType, final Object... supportInfo) {
        if (sortLock) {
            return;
        }

        this.lastSortEventType = sortEventType;
        this.lastSortEventSupportInfo = supportInfo;
        sort();
        this.lastSortEventType = null;
        this.lastSortEventSupportInfo = null;
    }


    // --- Content width
    private void setContentWidth(double contentWidth) {
        this.contentWidth = contentWidth;
        if (isInited) {
            // sometimes the current column resize policy will have to modify the
            // column width of all columns in the table if the table width changes,
            // so we short-circuit the resize function and just go straight there
            // with a null TableColumn, which indicates to the resize policy function
            // that it shouldn't actually do anything specific to one column.
            getColumnResizePolicy().call(new ResizeFeatures<>(TableView.this, null, 0.0));
        }
    }

    /**
     * Recomputes the currently visible leaf columns in this TableView.
     */
    private void updateVisibleLeafColumns() {
        // update visible leaf columns list
        List<TableColumn<S,?>> cols = new ArrayList<>();
        buildVisibleLeafColumns(getColumns(), cols);
        visibleLeafColumns.setAll(cols);

        // sometimes the current column resize policy will have to modify the
        // column width of all columns in the table if the table width changes,
        // so we short-circuit the resize function and just go straight there
        // with a null TableColumn, which indicates to the resize policy function
        // that it shouldn't actually do anything specific to one column.
        getColumnResizePolicy().call(new ResizeFeatures<>(TableView.this, null, 0.0));
    }

    private void buildVisibleLeafColumns(List<TableColumn<S,?>> cols, List<TableColumn<S,?>> vlc) {
        for (TableColumn<S,?> c : cols) {
            if (c == null) continue;

            boolean hasChildren = ! c.getColumns().isEmpty();

            if (hasChildren) {
                buildVisibleLeafColumns(c.getColumns(), vlc);
            } else if (c.isVisible()) {
                vlc.add(c);
            }
        }
    }



    /* *************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "table-view";

    private static final PseudoClass PSEUDO_CLASS_CELL_SELECTION =
            PseudoClass.getPseudoClass("cell-selection");
    private static final PseudoClass PSEUDO_CLASS_ROW_SELECTION =
            PseudoClass.getPseudoClass("row-selection");

    private static class StyleableProperties {
        private static final CssMetaData<TableView<?>,Number> FIXED_CELL_SIZE =
                new CssMetaData<>("-fx-fixed-cell-size",
                                                    SizeConverter.getInstance(),
                                                    Region.USE_COMPUTED_SIZE) {

                    @Override public Double getInitialValue(TableView<?> node) {
                        return node.getFixedCellSize();
                    }

                    @Override public boolean isSettable(TableView<?> n) {
                        return n.fixedCellSize == null || !n.fixedCellSize.isBound();
                    }

                    @Override public StyleableProperty<Number> getStyleableProperty(TableView<?> n) {
                        return (StyleableProperty<Number>) n.fixedCellSizeProperty();
                    }
                };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                    new ArrayList<>(Control.getClassCssMetaData());
            styleables.add(FIXED_CELL_SIZE);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * Gets the {@code CssMetaData} associated with this class, which may include the
     * {@code CssMetaData} of its superclasses.
     * @return the {@code CssMetaData}
     * @since JavaFX 8.0
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     * @since JavaFX 8.0
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }



    /* *************************************************************************
     *                                                                         *
     * Accessibility handling                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case COLUMN_COUNT: return getVisibleLeafColumns().size();
            case ROW_COUNT: return getItems().size();
            case SELECTED_ITEMS: {
                // TableViewSkin returns TableRows back to TableView.
                // TableRowSkin returns TableCells back to TableRow.
                @SuppressWarnings("unchecked")
                ObservableList<TableRow<S>> rows = (ObservableList<TableRow<S>>)super.queryAccessibleAttribute(attribute, parameters);
                List<Node> selection = new ArrayList<>();
                if (rows != null) {
                    for (TableRow<S> row: rows) {
                        @SuppressWarnings("unchecked")
                        ObservableList<Node> cells =
                            (ObservableList<Node>)row.queryAccessibleAttribute(attribute, parameters);
                        if (cells != null) {
                            selection.addAll(cells);
                        }
                    }
                }
                return FXCollections.observableArrayList(selection);
            }
            case FOCUS_ITEM: {
                Node row = (Node)super.queryAccessibleAttribute(attribute, parameters);
                if (row == null) return null;
                Node cell = (Node)row.queryAccessibleAttribute(attribute, parameters);
                /* cell equals to null means the row is a placeholder node */
                return cell != null ?  cell : row;
            }
            case CELL_AT_ROW_COLUMN: {
                @SuppressWarnings("unchecked")
                TableRow<S> row = (TableRow<S>)super.queryAccessibleAttribute(attribute, parameters);
                return row != null ? row.queryAccessibleAttribute(attribute, parameters) : null;
            }
            case MULTIPLE_SELECTION: {
                MultipleSelectionModel<S> sm = getSelectionModel();
                return sm != null && sm.getSelectionMode() == SelectionMode.MULTIPLE;
            }
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }


    /* *************************************************************************
     *                                                                         *
     * Support Interfaces                                                      *
     *                                                                         *
     **************************************************************************/

     /**
      * An immutable wrapper class for use in the TableView
     * {@link TableView#columnResizePolicyProperty() column resize} functionality.
      * @since JavaFX 2.0
      */
     public static class ResizeFeatures<S> extends ResizeFeaturesBase<S> {
        private TableView<S> table;

        /**
         * Creates an instance of this class, with the provided TableView,
         * TableColumn and delta values being set and stored in this immutable
         * instance.
         *
         * @param table The TableView upon which the resize operation is occurring.
         * @param column The column upon which the resize is occurring, or null
         *      if this ResizeFeatures instance is being created as a result of a
         *      TableView resize operation.
         * @param delta The amount of horizontal space added or removed in the
         *      resize operation.
         */
        public ResizeFeatures(TableView<S> table, TableColumn<S,?> column, Double delta) {
            super(column, delta);
            this.table = table;
        }

        /**
         * Returns the column upon which the resize is occurring, or null
         * if this ResizeFeatures instance was created as a result of a
         * TableView resize operation.
         */
        @Override public TableColumn<S,?> getColumn() {
            return (TableColumn<S,?>) super.getColumn();
        }

        /**
         * Returns the TableView upon which the resize operation is occurring.
         * @return the TableView
         */
        public TableView<S> getTable() {
            return table;
        }

        @Override
        public Control getTableControl() {
            return table;
        }

        @Override
        public double getContentWidth() {
            return table.contentWidth;
        }
    }



    /* *************************************************************************
     *                                                                         *
     * Support Classes                                                         *
     *                                                                         *
     **************************************************************************/


    /**
     * A simple extension of the {@link SelectionModel} abstract class to
     * allow for special support for TableView controls.
     * @since JavaFX 2.0
     */
    public static abstract class TableViewSelectionModel<S> extends TableSelectionModel<S> {

        /* *********************************************************************
         *                                                                     *
         * Private fields                                                      *
         *                                                                     *
         **********************************************************************/

        private final TableView<S> tableView;

        boolean blockFocusCall = false;



        /* *********************************************************************
         *                                                                     *
         * Constructors                                                        *
         *                                                                     *
         **********************************************************************/

        /**
         * Builds a default TableViewSelectionModel instance with the provided
         * TableView.
         * @param tableView The TableView upon which this selection model should
         *      operate.
         * @throws NullPointerException TableView can not be null.
         */
        public TableViewSelectionModel(final TableView<S> tableView) {
            if (tableView == null) {
                throw new NullPointerException("TableView can not be null");
            }

            this.tableView = tableView;
        }



        /* *********************************************************************
         *                                                                     *
         * Abstract API                                                        *
         *                                                                     *
         **********************************************************************/

        /**
         * A read-only ObservableList representing the currently selected cells
         * in this TableView. Rather than directly modify this list, please
         * use the other methods provided in the TableViewSelectionModel.
         * @return a read-only ObservableList representing the currently
         * selected cells in this TableView
         */
        public abstract ObservableList<TablePosition> getSelectedCells();


        /* *********************************************************************
         *                                                                     *
         * Generic (type erasure) bridging                                     *
         *                                                                     *
         **********************************************************************/

        // --- isSelected
        /** {@inheritDoc} */
        @Override public boolean isSelected(int row, TableColumnBase<S, ?> column) {
            return isSelected(row, (TableColumn<S,?>)column);
        }

        /**
         * Convenience function which tests whether the given row and column index
         * is currently selected in this table instance.
         * @param row the row
         * @param column the column
         * @return true if row and column index is currently selected
         */
        public abstract boolean isSelected(int row, TableColumn<S, ?> column);


        // --- select
        /** {@inheritDoc} */
        @Override public void select(int row, TableColumnBase<S, ?> column) {
            select(row, (TableColumn<S,?>)column);
        }

        /**
         * Selects the cell at the given row/column intersection.
         * @param row the row
         * @param column the column
         */
        public abstract void select(int row, TableColumn<S, ?> column);


        // --- clearAndSelect
        /** {@inheritDoc} */
        @Override public void clearAndSelect(int row, TableColumnBase<S,?> column) {
            clearAndSelect(row, (TableColumn<S,?>) column);
        }

        /**
         * Clears all selection, and then selects the cell at the given row/column
         * intersection.
         * @param row the row
         * @param column the column
         */
        public abstract void clearAndSelect(int row, TableColumn<S,?> column);


        // --- clearSelection
        /** {@inheritDoc} */
        @Override public void clearSelection(int row, TableColumnBase<S,?> column) {
            clearSelection(row, (TableColumn<S,?>) column);
        }

        /**
         * Removes selection from the specified row/column position (in view indexes).
         * If this particular cell (or row if the column value is -1) is not selected,
         * nothing happens.
         * @param row the row
         * @param column the column
         */
        public abstract void clearSelection(int row, TableColumn<S, ?> column);

        /** {@inheritDoc} */
        @Override public void selectRange(int minRow, TableColumnBase<S,?> minColumn,
                                          int maxRow, TableColumnBase<S,?> maxColumn) {
            final int minColumnIndex = tableView.getVisibleLeafIndex((TableColumn<S,?>)minColumn);
            final int maxColumnIndex = tableView.getVisibleLeafIndex((TableColumn<S,?>)maxColumn);
            for (int _row = minRow; _row <= maxRow; _row++) {
                for (int _col = minColumnIndex; _col <= maxColumnIndex; _col++) {
                    select(_row, tableView.getVisibleLeafColumn(_col));
                }
            }
        }



        /* *********************************************************************
         *                                                                     *
         * Public API                                                          *
         *                                                                     *
         **********************************************************************/

        /**
         * Returns the TableView instance that this selection model is installed in.
         * @return the TableView
         */
        public TableView<S> getTableView() {
            return tableView;
        }

        /**
         * Convenience method that returns getTableView().getItems().
         * @return The items list of the current TableView.
         */
        protected List<S> getTableModel()  {
            return tableView.getItems();
        }

        /** {@inheritDoc} */
        @Override protected S getModelItem(int index) {
            if (index < 0 || index >= getItemCount()) return null;
            return tableView.getItems().get(index);
        }

        /** {@inheritDoc} */
        @Override protected int getItemCount() {
            return getTableModel().size();
        }

        /** {@inheritDoc} */
        @Override public void focus(int row) {
            focus(row, null);
        }

        /** {@inheritDoc} */
        @Override public int getFocusedIndex() {
            return getFocusedCell().getRow();
        }



        /* *********************************************************************
         *                                                                     *
         * Private implementation                                              *
         *                                                                     *
         **********************************************************************/

        void focus(int row, TableColumn<S,?> column) {
            focus(new TablePosition<>(getTableView(), row, column));
            getTableView().notifyAccessibleAttributeChanged(AccessibleAttribute.FOCUS_ITEM);
        }

        void focus(TablePosition<S,?> pos) {
            if (blockFocusCall) return;
            if (getTableView().getFocusModel() == null) return;

            getTableView().getFocusModel().focus(pos.getRow(), pos.getTableColumn());
        }

        TablePosition<S,?> getFocusedCell() {
            if (getTableView().getFocusModel() == null) {
                return new TablePosition<>(getTableView(), -1, null);
            }
            return getTableView().getFocusModel().getFocusedCell();
        }
    }



    /**
     * A primitive selection model implementation, using a List<Integer> to store all
     * selected indices.
     */
    // package for testing
    static class TableViewArrayListSelectionModel<S> extends TableViewSelectionModel<S> {

        private int itemCount = 0;

        /* *********************************************************************
         *                                                                     *
         * Constructors                                                        *
         *                                                                     *
         **********************************************************************/

        public TableViewArrayListSelectionModel(final TableView<S> tableView) {
            super(tableView);
            this.tableView = tableView;

            this.itemsPropertyListener = new InvalidationListener() {
                private WeakReference<ObservableList<S>> weakItemsRef = new WeakReference<>(tableView.getItems());

                @Override public void invalidated(Observable observable) {
                    ObservableList<S> oldItems = weakItemsRef.get();
                    weakItemsRef = new WeakReference<>(tableView.getItems());
                    updateItemsObserver(oldItems, tableView.getItems());
                }
            };
            this.tableView.itemsProperty().addListener(itemsPropertyListener);

            selectedCellsMap = new SelectedCellsMap<>(c -> fireCustomSelectedCellsListChangeEvent(c)) {  // Note: use of method reference causes javac compilation error (see JDK-8297428)
                @Override public boolean isCellSelectionEnabled() {
                    return TableViewArrayListSelectionModel.this.isCellSelectionEnabled();
                }
            };

            selectedCellsSeq = new ReadOnlyUnbackedObservableList<>() {
                @Override public TablePosition<S,?> get(int i) {
                    return selectedCellsMap.get(i);
                }

                @Override public int size() {
                    return selectedCellsMap.size();
                }
            };
//            selectedCellsSeq.addListener((ListChangeListener<? super TablePosition<S,?>>) c -> {
//                ControlUtils.updateSelectedIndices(this, c);
//            });


            /*
             * The following listener is used in conjunction with
             * SelectionModel.select(T obj) to allow for a developer to select
             * an item that is not actually in the data model. When this occurs,
             * we actively try to find an index that matches this object, going
             * so far as to actually watch for all changes to the items list,
             * rechecking each time.
             */

            // watching for changes to the items list content
            ObservableList<S> items = getTableView().getItems();
            if (items != null) {
                items.addListener(weakItemsContentListener);
            }


            updateItemCount();

            updateDefaultSelection();

            cellSelectionEnabledProperty().addListener(o -> {
                updateDefaultSelection();
                TableCellBehaviorBase.setAnchor(tableView, getFocusedCell(), true);
            });
        }

        private void dispose() {
            this.tableView.itemsProperty().removeListener(itemsPropertyListener);

            ObservableList<S> items = getTableView().getItems();
            if (items != null) {
                items.removeListener(weakItemsContentListener);
            }
        }

        private final TableView<S> tableView;

        final InvalidationListener itemsPropertyListener;

        final ListChangeListener<S> itemsContentListener = c -> {
            updateItemCount();

            List<S> items1 = getTableModel();
            boolean doSelectionUpdate = true;

            while (c.next()) {
                if (c.wasReplaced() || c.getAddedSize() == getItemCount()) {
                    this.selectedItemChange = c;
                    updateDefaultSelection();
                    this.selectedItemChange = null;
                    return;
                }

                final S selectedItem = getSelectedItem();
                final int selectedIndex = getSelectedIndex();

                if (items1 == null || items1.isEmpty()) {
                    clearSelection();
                } else if (getSelectedIndex() == -1 && getSelectedItem() != null) {
                    int newIndex = items1.indexOf(getSelectedItem());
                    if (newIndex != -1) {
                        setSelectedIndex(newIndex);
                        doSelectionUpdate = false;
                    }
                } else if (c.wasRemoved() &&
                        c.getRemovedSize() == 1 &&
                        ! c.wasAdded() &&
                        selectedItem != null &&
                        selectedItem.equals(c.getRemoved().get(0))) {
                    // Bug fix for RT-28637
                    if (getSelectedIndex() < getItemCount()) {
                        final int previousRow = selectedIndex == 0 ? 0 : selectedIndex - 1;
                        S newSelectedItem = getModelItem(previousRow);
                        if (! selectedItem.equals(newSelectedItem)) {
                            clearAndSelect(previousRow);
                        }
                    }
                }
            }

            if (doSelectionUpdate) {
                updateSelection(c);
            }
        };

        final WeakListChangeListener<S> weakItemsContentListener
                = new WeakListChangeListener<>(itemsContentListener);



        /* *********************************************************************
         *                                                                     *
         * Observable properties (and getters/setters)                         *
         *                                                                     *
         **********************************************************************/

        // the only 'proper' internal data structure, selectedItems and selectedIndices
        // are both 'read-only and unbacked'.
        private final SelectedCellsMap<TablePosition<S,?>> selectedCellsMap;

        // we create a ReadOnlyUnbackedObservableList of selectedCells here so
        // that we can fire custom list change events.
        private final ReadOnlyUnbackedObservableList<TablePosition<S,?>> selectedCellsSeq;
        @Override public ObservableList<TablePosition> getSelectedCells() {
            return (ObservableList<TablePosition>)(Object)selectedCellsSeq;
        }



        /* *********************************************************************
         *                                                                     *
         * Internal properties                                                 *
         *                                                                     *
         **********************************************************************/

        private int previousModelSize = 0;

        // Listen to changes in the tableview items list, such that when it
        // changes we can update the selected indices list to refer to the
        // new indices.
        private void updateSelection(ListChangeListener.Change<? extends S> c) {
            c.reset();

            int shift = 0;
            int startRow = -1;
            while (c.next()) {
                if (c.wasReplaced()) {
                    if (c.getList().isEmpty()) {
                        // the entire items list was emptied - clear selection
                        clearSelection();
                    } else {
                        int index = getSelectedIndex();

                        if (previousModelSize == c.getRemovedSize()) {
                            // all items were removed from the model
                            clearSelection();
                        } else if (index < getItemCount() && index >= 0) {
                            // Fix for RT-18969: the list had setAll called on it
                            // Use of makeAtomic is a fix for RT-20945
                            startAtomic();
                            clearSelection(index);
                            stopAtomic();
                            select(index);
                        } else {
                            // Fix for RT-22079
                            clearSelection();
                        }
                    }
                } else if (c.wasAdded() || c.wasRemoved()) {
                    startRow = c.getFrom();
                    shift += c.wasAdded() ? c.getAddedSize() : -c.getRemovedSize();
                } else if (c.wasPermutated()) {
                    // General approach:
                    //   -- detected a sort has happened
                    //   -- Create a permutation lookup map (1)
                    //   -- dump all the selected indices into a list (2)
                    //   -- create a list containing the new indices (3)
                    //   -- for each previously-selected index (4)
                    //     -- if index is in the permutation lookup map
                    //       -- add the new index to the new indices list
                    //   -- Perform batch selection (5)

                    startAtomic();

                    final int oldSelectedIndex = getSelectedIndex();

                    // (1)
                    int length = c.getTo() - c.getFrom();
                    HashMap<Integer, Integer> pMap = new HashMap<> (length);
                    for (int i = c.getFrom(); i < c.getTo(); i++) {
                        pMap.put(i, c.getPermutation(i));
                    }

                    // (2)
                    List<TablePosition<S,?>> selectedIndices = new ArrayList<>((ObservableList<TablePosition<S,?>>)(Object)getSelectedCells());

                    // (3)
                    List<TablePosition<S,?>> newIndices = new ArrayList<>(selectedIndices.size());

                    // (4)
                    boolean selectionIndicesChanged = false;
                    for (int i = 0; i < selectedIndices.size(); i++) {
                        final TablePosition<S,?> oldIndex = selectedIndices.get(i);
                        final int oldRow = oldIndex.getRow();

                        if (pMap.containsKey(oldRow)) {
                            int newIndex = pMap.get(oldRow);

                            selectionIndicesChanged = selectionIndicesChanged || newIndex != oldRow;

                            newIndices.add(new TablePosition<>(oldIndex.getTableView(), newIndex, oldIndex.getTableColumn()));
                        }
                    }

                    if (selectionIndicesChanged) {
                        // (5)
                        quietClearSelection();
                        stopAtomic();

                        selectedCellsMap.setAll(newIndices);

                        if (oldSelectedIndex >= 0 && oldSelectedIndex < itemCount) {
                            int newIndex = c.getPermutation(oldSelectedIndex);
                            setSelectedIndex(newIndex);
                            focus(newIndex);
                        }
                    } else {
                        stopAtomic();
                    }
                }
            }

            TablePosition<S,?> anchor = TableCellBehavior.getAnchor(tableView, null);
            if (shift != 0 && startRow >= 0 && anchor != null && anchor.getRow() >= startRow && (c.wasRemoved() || c.wasAdded())) {
                if (isSelected(anchor.getRow(), anchor.getTableColumn())) {
                    TablePosition<S,?> newAnchor = new TablePosition<>(tableView, anchor.getRow() + shift, anchor.getTableColumn());
                    TableCellBehavior.setAnchor(tableView, newAnchor, false);
                }
            }

            shiftSelection(startRow, shift, new Callback<>() {
                @Override public Void call(ShiftParams param) {

                    // we make the shifts atomic, as otherwise listeners to
                    // the items / indices lists get a lot of intermediate
                    // noise. They eventually get the summary event fired
                    // from within shiftSelection, so this is ok.
                    startAtomic();

                    final int clearIndex = param.getClearIndex();
                    final int setIndex = param.getSetIndex();
                    TablePosition<S,?> oldTP = null;
                    if (clearIndex > -1) {
                        for (int i = 0; i < selectedCellsMap.size(); i++) {
                            TablePosition<S,?> tp = selectedCellsMap.get(i);
                            if (tp.getRow() == clearIndex) {
                                oldTP = tp;
                                selectedCellsMap.remove(tp);
                            } else if (tp.getRow() == setIndex && !param.isSelected()) {
                                selectedCellsMap.remove(tp);
                            }
                        }
                    }

                    if (oldTP != null && param.isSelected()) {
                        TablePosition<S,?> newTP = new TablePosition<>(
                                tableView, param.getSetIndex(), oldTP.getTableColumn());

                        selectedCellsMap.add(newTP);
                    }

                    stopAtomic();

                    return null;
                }
            });

            previousModelSize = getItemCount();
        }

        /* *********************************************************************
         *                                                                     *
         * Public selection API                                                *
         *                                                                     *
         **********************************************************************/

        @Override public void clearAndSelect(int row) {
            clearAndSelect(row, null);
        }

        @Override public void clearAndSelect(int row, TableColumn<S,?> column) {
            if (row < 0 || row >= getItemCount()) return;

            final TablePosition<S,?> newTablePosition = new TablePosition<>(getTableView(), row, column);
            final boolean isCellSelectionEnabled = isCellSelectionEnabled();

            // replace the anchor
            TableCellBehavior.setAnchor(tableView, newTablePosition, false);

            // firstly we make a copy of the selection, so that we can send out
            // the correct details in the selection change event.
            List<TablePosition<S,?>> previousSelection = new ArrayList<>(selectedCellsMap.getSelectedCells());

            // secondly we check if we can short-circuit out of here because the new selection
            // equals the current selection
            final boolean wasSelected = isSelected(row, column);
            if (wasSelected && previousSelection.size() == 1) {
                // before we return, we double-check that the selected item
                // is equal to the item in the given index
                TablePosition<S,?> selectedCell = getSelectedCells().get(0);
                if (getSelectedItem() == getModelItem(row)) {
                    if (selectedCell.getRow() == row && selectedCell.getTableColumn() == column) {
                        return;
                    }
                }
            }

            // RT-32411 We used to call quietClearSelection() here, but this
            // resulted in the selectedItems and selectedIndices lists never
            // reporting that they were empty.
            // makeAtomic toggle added to resolve RT-32618
            startAtomic();

            // then clear the current selection
            clearSelection();

            // and select the new cell
            select(row, column);

            stopAtomic();


            // We remove the new selection from the list seeing as it is not removed.
            if (isCellSelectionEnabled) {
                previousSelection.remove(newTablePosition);
            } else {
                for (TablePosition<S,?> tp : previousSelection) {
                    if (tp.getRow() == row) {
                        previousSelection.remove(tp);
                        break;
                    }
                }
            }

            // fire off a single add/remove/replace notification (rather than
            // individual remove and add notifications) - see RT-33324
            ListChangeListener.Change<TablePosition<S, ?>> change;

            /*
             * getFrom() documentation:
             *   If wasAdded is true, the interval contains all the values that were added.
             *   If wasPermutated is true, the interval marks the values that were permutated.
             *   If wasRemoved is true and wasAdded is false, getFrom() and getTo() should
             *   return the same number - the place where the removed elements were positioned in the list.
             */
            if (wasSelected) {
                change = ControlUtils.buildClearAndSelectChange(
                        selectedCellsSeq, previousSelection, newTablePosition, Comparator.comparing(TablePosition::getRow));
            } else {
                final int changeIndex = isCellSelectionEnabled ? 0 : Math.max(0, selectedCellsSeq.indexOf(newTablePosition));
                final int changeSize = isCellSelectionEnabled ? getSelectedCells().size() : 1;
                change = new NonIterableChange.GenericAddRemoveChange<>(
                        changeIndex, changeIndex + changeSize, previousSelection, selectedCellsSeq);
//                selectedCellsSeq._beginChange();
//                selectedCellsSeq._nextAdd(changeIndex, changeIndex + changeSize);
//                selectedCellsSeq._nextRemove(changeIndex, previousSelection);
//                selectedCellsSeq._endChange();
            }
            fireCustomSelectedCellsListChangeEvent(change);
        }

        @Override public void select(int row) {
            select(row, null);
        }

        @Override
        public void select(int row, TableColumn<S,?> column) {
            if (row < 0 || row >= getItemCount()) return;

            // if I'm in cell selection mode but the column is null, select each
            // of the contained cells individually
            if (isCellSelectionEnabled() && column == null) {
                List<TableColumn<S,?>> columns = getTableView().getVisibleLeafColumns();
                for (int i = 0; i < columns.size(); i++) {
                    select(row, columns.get(i));
                }
                return;
            }

            if (TableCellBehavior.hasDefaultAnchor(tableView)) {
                TableCellBehavior.removeAnchor(tableView);
            }

            if (getSelectionMode() == SelectionMode.SINGLE) {
                quietClearSelection();
            }
            selectedCellsMap.add(new TablePosition<>(getTableView(), row, column));

            updateSelectedIndex(row);
            focus(row, column);
        }

        @Override public void select(S obj) {
            if (obj == null && getSelectionMode() == SelectionMode.SINGLE) {
                clearSelection();
                return;
            }

            // We have no option but to iterate through the model and select the
            // first occurrence of the given object. Once we find the first one, we
            // don't proceed to select any others.
            S rowObj = null;
            for (int i = 0; i < getItemCount(); i++) {
                rowObj = getModelItem(i);
                if (rowObj == null) continue;

                if (rowObj.equals(obj)) {
                    if (isSelected(i)) {
                        return;
                    }

                    if (getSelectionMode() == SelectionMode.SINGLE) {
                        quietClearSelection();
                    }

                    select(i);
                    return;
                }
            }

            // if we are here, we did not find the item in the entire data model.
            // Even still, we allow for this item to be set to the give object.
            // We expect that in concrete subclasses of this class we observe the
            // data model such that we check to see if the given item exists in it,
            // whilst SelectedIndex == -1 && SelectedItem != null.
            setSelectedIndex(-1);
            setSelectedItem(obj);
        }

        @Override public void selectIndices(int row, int... rows) {
            if (rows == null || rows.length == 0) {
                select(row);
                return;
            }

            /*
             * Performance optimisation - if multiple selection is disabled, only
             * process the end-most row index.
             */
            int rowCount = getItemCount();

            if (getSelectionMode() == SelectionMode.SINGLE) {
                quietClearSelection();

                for (int i = rows.length - 1; i >= 0; i--) {
                    int index = rows[i];
                    if (index >= 0 && index < rowCount) {
                        select(index);
                        break;
                    }
                }

                if (selectedCellsMap.isEmpty()) {
                    if (row > 0 && row < rowCount) {
                        select(row);
                    }
                }
            } else {
                int lastIndex = -1;
                Set<TablePosition<S,?>> positions = new LinkedHashSet<>();

                // --- firstly, we special-case the non-varargs 'row' argument
                if (row >= 0 && row < rowCount) {
                    // if I'm in cell selection mode, we want to select each
                    // of the contained cells individually
                    if (isCellSelectionEnabled()) {
                        List<TableColumn<S,?>> columns = getTableView().getVisibleLeafColumns();
                        for (int column = 0; column < columns.size(); column++) {
                            if (! selectedCellsMap.isSelected(row, column)) {
                                positions.add(new TablePosition<>(getTableView(), row, columns.get(column)));
                                lastIndex = row;
                            }
                        }
                    } else {
                        boolean match = selectedCellsMap.isSelected(row, -1);
                        if (!match) {
                            positions.add(new TablePosition<>(getTableView(), row, null));
                        }
                    }

                    lastIndex = row;
                }

                // --- now we iterate through all varargs values
                for (int i = 0; i < rows.length; i++) {
                    int index = rows[i];
                    if (index < 0 || index >= rowCount) continue;
                    lastIndex = index;

                    if (isCellSelectionEnabled()) {
                        List<TableColumn<S,?>> columns = getTableView().getVisibleLeafColumns();
                        for (int column = 0; column < columns.size(); column++) {
                            if (! selectedCellsMap.isSelected(index, column)) {
                                positions.add(new TablePosition<>(getTableView(), index, columns.get(column)));
                                lastIndex = index;
                            }
                        }
                    } else {
                        if (! selectedCellsMap.isSelected(index, -1)) {
                            // if we are here then we have successfully gotten through the for-loop above
                            positions.add(new TablePosition<>(getTableView(), index, null));
                        }
                    }
                }

                selectedCellsMap.addAll(positions);

                if (lastIndex != -1) {
                    select(lastIndex);
                }
            }
        }

        @Override public void selectAll() {
            if (getSelectionMode() == SelectionMode.SINGLE) return;

            if (isCellSelectionEnabled()) {
                List<TablePosition<S,?>> indices = new ArrayList<>();
                TableColumn<S,?> column;
                TablePosition<S,?> tp = null;
                for (int col = 0; col < getTableView().getVisibleLeafColumns().size(); col++) {
                    column = getTableView().getVisibleLeafColumns().get(col);
                    for (int row = 0; row < getItemCount(); row++) {
                        tp = new TablePosition<>(getTableView(), row, column);
                        indices.add(tp);
                    }
                }
                selectedCellsMap.setAll(indices);

                if (tp != null) {
                    select(tp.getRow(), tp.getTableColumn());
                    focus(tp.getRow(), tp.getTableColumn());
                }
            } else {
                List<TablePosition<S,?>> indices = new ArrayList<>();
                for (int i = 0; i < getItemCount(); i++) {
                    indices.add(new TablePosition<>(getTableView(), i, null));
                }
                selectedCellsMap.setAll(indices);

                int focusedIndex = getFocusedIndex();
                if (focusedIndex == -1) {
                    final int itemCount = getItemCount();
                    if (itemCount > 0) {
                        select(itemCount - 1);
                        focus(indices.get(indices.size() - 1));
                    }
                } else {
                    select(focusedIndex);
                    focus(focusedIndex);
                }
            }
        }

        @Override public void selectRange(int minRow, TableColumnBase<S,?> minColumn,
                                          int maxRow, TableColumnBase<S,?> maxColumn) {
            if (getSelectionMode() == SelectionMode.SINGLE) {
                quietClearSelection();
                select(maxRow, maxColumn);
                return;
            }

            startAtomic();

            final int itemCount = getItemCount();
            final boolean isCellSelectionEnabled = isCellSelectionEnabled();

            final int minColumnIndex = tableView.getVisibleLeafIndex((TableColumn<S,?>)minColumn);
            final int maxColumnIndex = tableView.getVisibleLeafIndex((TableColumn<S,?>)maxColumn);
            final int _minColumnIndex = Math.min(minColumnIndex, maxColumnIndex);
            final int _maxColumnIndex = Math.max(minColumnIndex, maxColumnIndex);

            final int _minRow = Math.min(minRow, maxRow);
            final int _maxRow = Math.max(minRow, maxRow);

            List<TablePosition<S,?>> cellsToSelect = new ArrayList<>();

            for (int _row = _minRow; _row <= _maxRow; _row++) {
                // begin copy/paste of select(int, column) method (with some
                // slight modifications)
                if (_row < 0 || _row >= itemCount) continue;

                if (! isCellSelectionEnabled) {
                    cellsToSelect.add(new TablePosition<>(tableView, _row, (TableColumn<S,?>)minColumn));
                } else {
                    for (int _col = _minColumnIndex; _col <= _maxColumnIndex; _col++) {
                        final TableColumn<S, ?> column = tableView.getVisibleLeafColumn(_col);

                        // if I'm in cell selection mode but the column is null, I don't want
                        // to select the whole row instead...
                        if (column == null && isCellSelectionEnabled) continue;

                        cellsToSelect.add(new TablePosition<>(tableView, _row, column));
                        // end copy/paste
                    }
                }
            }

            // to prevent duplication we remove all currently selected cells from
            // our list of cells to select.
            cellsToSelect.removeAll(getSelectedCells());

            selectedCellsMap.addAll(cellsToSelect);
            stopAtomic();

            // fire off events.
            // Note that focus and selection always goes to maxRow, not _maxRow.
            updateSelectedIndex(maxRow);
            focus(maxRow, (TableColumn<S,?>)maxColumn);

            final TableColumn<S,?> startColumn = (TableColumn<S,?>)minColumn;
            final TableColumn<S,?> endColumn = isCellSelectionEnabled ? (TableColumn<S,?>)maxColumn : startColumn;
            final int startChangeIndex = selectedCellsMap.indexOf(new TablePosition<>(tableView, minRow, startColumn));
            final int endChangeIndex = selectedCellsMap.indexOf(new TablePosition<>(tableView, maxRow, endColumn));

            if (startChangeIndex > -1 && endChangeIndex > -1) {
                final int startIndex = Math.min(startChangeIndex, endChangeIndex);
                final int endIndex = Math.max(startChangeIndex, endChangeIndex);

                ListChangeListener.Change c = new NonIterableChange.SimpleAddChange<>(startIndex, endIndex + 1, selectedCellsSeq);
                fireCustomSelectedCellsListChangeEvent(c);
//                selectedCellsSeq.fireChange(() -> selectedCellsSeq._nextAdd(startIndex, endIndex + 1));
            }
        }

        @Override public void clearSelection(int index) {
            clearSelection(index, null);
        }

        @Override
        public void clearSelection(int row, TableColumn<S,?> column) {
            clearSelection(new TablePosition<>(getTableView(), row, column));
        }

        private void clearSelection(TablePosition<S,?> tp) {
            final boolean csMode = isCellSelectionEnabled();
            final int row = tp.getRow();
            final boolean columnIsNull = tp.getTableColumn() == null;

            List<TablePosition> toRemove = new ArrayList<>();
            for (TablePosition pos : getSelectedCells()) {
                if (!csMode) {
                    if (pos.getRow() == row) {
                        toRemove.add(pos);
                        break;
                    }
                } else {
                    if (columnIsNull && pos.getRow() == row) {
                        // if we are in cell selection mode and the column is null,
                        // we remove all items in the row
                        toRemove.add(pos);
                    } else if (pos.equals(tp)) {
                        toRemove.add(tp);
                        break;
                    }
                }
            }
            toRemove.stream().forEach(selectedCellsMap::remove);

            if (isEmpty() && ! isAtomic()) {
                updateSelectedIndex(-1);
                selectedCellsMap.clear();
            }
        }

        @Override public void clearSelection() {
            final List<TablePosition<S,?>> removed = new ArrayList<>((Collection)getSelectedCells());

            quietClearSelection();

            if (! isAtomic()) {
                updateSelectedIndex(-1);
                focus(-1);

                if (!removed.isEmpty()) {
                    ListChangeListener.Change<TablePosition<S, ?>> c = new NonIterableChange<>(0, 0, selectedCellsSeq) {
                        @Override public List<TablePosition<S, ?>> getRemoved() {
                            return removed;
                        }
                    };
                    fireCustomSelectedCellsListChangeEvent(c);
//                    selectedCellsSeq.fireChange(() -> selectedCellsSeq._nextRemove(0, removed));
                }
            }
        }

        private void quietClearSelection() {
            startAtomic();
            selectedCellsMap.clear();
            stopAtomic();
        }

        @Override
        public boolean isSelected(int row, TableColumn<S,?> column) {
            // When in cell selection mode, if the column is null, then we interpret
            // the users query to be asking if _all_ of the cells in the row are selected,
            // rather than if _any_ of the cells in the row are selected.
            final boolean isCellSelectionEnabled = isCellSelectionEnabled();
            if (isCellSelectionEnabled && column == null) {
                int columnCount = tableView.getVisibleLeafColumns().size();
                for (int col = 0; col < columnCount; col++) {
                    if (!selectedCellsMap.isSelected(row, col)) {
                        return false;
                    }
                }
                return true;
            } else {
                int columnIndex = !isCellSelectionEnabled || column == null ? -1 : tableView.getVisibleLeafIndex(column);
                return selectedCellsMap.isSelected(row, columnIndex);
            }
        }

        @Override public boolean isEmpty() {
            return selectedCellsMap.isEmpty();
        }

        @Override public void selectPrevious() {
            if (isCellSelectionEnabled()) {
                // in cell selection mode, we have to wrap around, going from
                // right-to-left, and then wrapping to the end of the previous line
                TablePosition<S,?> pos = getFocusedCell();
                if (pos.getColumn() - 1 >= 0) {
                    // go to previous row
                    select(pos.getRow(), getTableColumn(pos.getTableColumn(), -1));
                } else if (pos.getRow() < getItemCount() - 1) {
                    // wrap to end of previous row
                    select(pos.getRow() - 1, getTableColumn(getTableView().getVisibleLeafColumns().size() - 1));
                }
            } else {
                int focusIndex = getFocusedIndex();
                if (focusIndex == -1) {
                    select(getItemCount() - 1);
                } else if (focusIndex > 0) {
                    select(focusIndex - 1);
                }
            }
        }

        @Override public void selectNext() {
            if (isCellSelectionEnabled()) {
                // in cell selection mode, we have to wrap around, going from
                // left-to-right, and then wrapping to the start of the next line
                TablePosition<S,?> pos = getFocusedCell();
                if (pos.getColumn() + 1 < getTableView().getVisibleLeafColumns().size()) {
                    // go to next column
                    select(pos.getRow(), getTableColumn(pos.getTableColumn(), 1));
                } else if (pos.getRow() < getItemCount() - 1) {
                    // wrap to start of next row
                    select(pos.getRow() + 1, getTableColumn(0));
                }
            } else {
                int focusIndex = getFocusedIndex();
                if (focusIndex == -1) {
                    select(0);
                } else if (focusIndex < getItemCount() -1) {
                    select(focusIndex + 1);
                }
            }
        }

        @Override public void selectAboveCell() {
            TablePosition<S,?> pos = getFocusedCell();
            if (pos.getRow() == -1) {
                select(getItemCount() - 1);
            } else if (pos.getRow() > 0) {
                select(pos.getRow() - 1, pos.getTableColumn());
            }
        }

        @Override public void selectBelowCell() {
            TablePosition<S,?> pos = getFocusedCell();

            if (pos.getRow() == -1) {
                select(0);
            } else if (pos.getRow() < getItemCount() -1) {
                select(pos.getRow() + 1, pos.getTableColumn());
            }
        }

        @Override public void selectFirst() {
            TablePosition<S,?> focusedCell = getFocusedCell();

            if (getSelectionMode() == SelectionMode.SINGLE) {
                quietClearSelection();
            }

            if (getItemCount() > 0) {
                if (isCellSelectionEnabled()) {
                    select(0, focusedCell.getTableColumn());
                } else {
                    select(0);
                }
            }
        }

        @Override public void selectLast() {
            TablePosition<S,?> focusedCell = getFocusedCell();

            if (getSelectionMode() == SelectionMode.SINGLE) {
                quietClearSelection();
            }

            int numItems = getItemCount();
            if (numItems > 0 && getSelectedIndex() < numItems - 1) {
                if (isCellSelectionEnabled()) {
                    select(numItems - 1, focusedCell.getTableColumn());
                } else {
                    select(numItems - 1);
                }
            }
        }

        @Override
        public void selectLeftCell() {
            if (! isCellSelectionEnabled()) return;

            TablePosition<S,?> pos = getFocusedCell();
            if (pos.getColumn() - 1 >= 0) {
                select(pos.getRow(), getTableColumn(pos.getTableColumn(), -1));
            }
        }

        @Override
        public void selectRightCell() {
            if (! isCellSelectionEnabled()) return;

            TablePosition<S,?> pos = getFocusedCell();
            if (pos.getColumn() + 1 < getTableView().getVisibleLeafColumns().size()) {
                select(pos.getRow(), getTableColumn(pos.getTableColumn(), 1));
            }
        }



        /* *********************************************************************
         *                                                                     *
         * Support code                                                        *
         *                                                                     *
         **********************************************************************/

        private void updateItemsObserver(ObservableList<S> oldList, ObservableList<S> newList) {
            // the items list has changed, we need to observe
            // the new list, and remove any observer we had from the old list
            if (oldList != null) {
                oldList.removeListener(weakItemsContentListener);
            }
            if (newList != null) {
                newList.addListener(weakItemsContentListener);
            }

            updateItemCount();
            updateDefaultSelection();
        }

        private void updateDefaultSelection() {
            // when the items list totally changes, we should clear out
            // the selection
            int newSelectionIndex = -1;
            if (tableView.getItems() != null) {
                S selectedItem = getSelectedItem();
                if (selectedItem != null) {
                    newSelectionIndex = tableView.getItems().indexOf(selectedItem);
                }
            }

            clearSelection();
            select(newSelectionIndex, isCellSelectionEnabled() ? getTableColumn(0) : null);
        }

        private TableColumn<S,?> getTableColumn(int pos) {
            return getTableView().getVisibleLeafColumn(pos);
        }

        // Gets a table column to the left or right of the current one, given an offset
        private TableColumn<S,?> getTableColumn(TableColumn<S,?> column, int offset) {
            int columnIndex = getTableView().getVisibleLeafIndex(column);
            int newColumnIndex = columnIndex + offset;
            return getTableView().getVisibleLeafColumn(newColumnIndex);
        }

        private void updateSelectedIndex(int row) {
            setSelectedIndex(row);
            setSelectedItem(getModelItem(row));
        }

        /** {@inheritDoc} */
        @Override protected int getItemCount() {
            return itemCount;
        }

        private void updateItemCount() {
            if (tableView == null) {
                itemCount = -1;
            } else {
                List<S> items = getTableModel();
                itemCount = items == null ? -1 : items.size();
            }
        }

        private void fireCustomSelectedCellsListChangeEvent(ListChangeListener.Change<? extends TablePosition<S,?>> c) {
            // Allow removing the row index if cell selection is not enabled or
            // if such row doesn't have any selected cells
            IntPredicate removeRowFilter = row -> !isCellSelectionEnabled() ||
                    getSelectedCells().stream().noneMatch(tp -> tp.getRow() == row);
            ControlUtils.updateSelectedIndices(this, this.isCellSelectionEnabled(), c, removeRowFilter);

            if (isAtomic()) {
                return;
            }

            selectedCellsSeq.callObservers(new MappingChange<>(c, Function.identity(), selectedCellsSeq));
        }
    }




    /**
     * A {@link FocusModel} with additional functionality to support the requirements
     * of a TableView control.
     *
     * @see TableView
     * @since JavaFX 2.0
     */
    public static class TableViewFocusModel<S> extends TableFocusModel<S, TableColumn<S, ?>> {

        private final TableView<S> tableView;

        private final TablePosition<S,?> EMPTY_CELL;

        /**
         * Creates a default TableViewFocusModel instance that will be used to
         * manage focus of the provided TableView control.
         *
         * @param tableView The tableView upon which this focus model operates.
         * @throws NullPointerException The TableView argument can not be null.
         */
        public TableViewFocusModel(final TableView<S> tableView) {
            if (tableView == null) {
                throw new NullPointerException("TableView can not be null");
            }

            this.tableView = tableView;
            this.EMPTY_CELL = new TablePosition<>(tableView, -1, null);

            itemsObserver = new InvalidationListener() {
                private WeakReference<ObservableList<S>> weakItemsRef = new WeakReference<>(tableView.getItems());

                @Override public void invalidated(Observable observable) {
                    ObservableList<S> oldItems = weakItemsRef.get();
                    weakItemsRef = new WeakReference<>(tableView.getItems());
                    updateItemsObserver(oldItems, tableView.getItems());
                }
            };
            this.tableView.itemsProperty().addListener(new WeakInvalidationListener(itemsObserver));
            if (tableView.getItems() != null) {
                this.tableView.getItems().addListener(weakItemsContentListener);
            }

            updateDefaultFocus();

            focusedCellProperty().addListener(o -> {
                tableView.notifyAccessibleAttributeChanged(AccessibleAttribute.FOCUS_ITEM);
            });
        }

        private final InvalidationListener itemsObserver;

        // Listen to changes in the tableview items list, such that when it
        // changes we can update the focused index to refer to the new indices.
        private final ListChangeListener<S> itemsContentListener = c -> {
            c.next();

            if (c.wasReplaced() || c.getAddedSize() == getItemCount()) {
                updateDefaultFocus();
                return;
            }

            TablePosition<S,?> focusedCell = getFocusedCell();
            final int focusedIndex = focusedCell.getRow();
            if (focusedIndex == -1 || c.getFrom() > focusedIndex) {
                return;
            }

            c.reset();
            boolean added = false;
            boolean removed = false;
            int addedSize = 0;
            int removedSize = 0;
            while (c.next()) {
                added |= c.wasAdded();
                removed |= c.wasRemoved();
                addedSize += c.getAddedSize();
                removedSize += c.getRemovedSize();
            }

            if (added && ! removed) {
                if (addedSize < c.getList().size()) {
                    final int newFocusIndex = Math.min(getItemCount() - 1, getFocusedIndex() + addedSize);
                    focus(newFocusIndex, focusedCell.getTableColumn());
                }
            } else if (!added && removed) {
                final int newFocusIndex = Math.max(0, getFocusedIndex() - removedSize);
                if (newFocusIndex < 0) {
                    focus(0, focusedCell.getTableColumn());
                } else {
                    focus(newFocusIndex, focusedCell.getTableColumn());
                }
            }
        };

        private WeakListChangeListener<S> weakItemsContentListener
                = new WeakListChangeListener<>(itemsContentListener);

        private void updateItemsObserver(ObservableList<S> oldList, ObservableList<S> newList) {
            // the tableview items list has changed, we need to observe
            // the new list, and remove any observer we had from the old list
            if (oldList != null) oldList.removeListener(weakItemsContentListener);
            if (newList != null) newList.addListener(weakItemsContentListener);

            updateDefaultFocus();
        }

        /** {@inheritDoc} */
        @Override protected int getItemCount() {
            if (tableView.getItems() == null) return -1;
            return tableView.getItems().size();
        }

        /** {@inheritDoc} */
        @Override protected S getModelItem(int index) {
            if (tableView.getItems() == null) return null;

            if (index < 0 || index >= getItemCount()) return null;

            return tableView.getItems().get(index);
        }

        /**
         * The position of the current item in the TableView which has the focus.
         */
        private ReadOnlyObjectWrapper<TablePosition> focusedCell;
        public final ReadOnlyObjectProperty<TablePosition> focusedCellProperty() {
            return focusedCellPropertyImpl().getReadOnlyProperty();
        }
        private void setFocusedCell(TablePosition value) { focusedCellPropertyImpl().set(value);  }
        public final TablePosition getFocusedCell() { return focusedCell == null ? EMPTY_CELL : focusedCell.get(); }

        private ReadOnlyObjectWrapper<TablePosition> focusedCellPropertyImpl() {
            if (focusedCell == null) {
                focusedCell = new ReadOnlyObjectWrapper<>(EMPTY_CELL) {
                    private TablePosition old;
                    @Override protected void invalidated() {
                        if (get() == null) return;

                        if (old == null || !old.equals(get())) {
                            setFocusedIndex(get().getRow());
                            setFocusedItem(getModelItem(getValue().getRow()));

                            old = get();
                        }
                    }

                    @Override
                    public Object getBean() {
                        return TableViewFocusModel.this;
                    }

                    @Override
                    public String getName() {
                        return "focusedCell";
                    }
                };
            }
            return focusedCell;
        }


        /**
         * Causes the item at the given index to receive the focus.
         *
         * @param row The row index of the item to give focus to.
         * @param column The column of the item to give focus to. Can be null.
         */
        @Override public void focus(int row, TableColumn<S,?> column) {
            if (row < 0 || row >= getItemCount()) {
                setFocusedCell(EMPTY_CELL);
            } else {
                TablePosition<S,?> oldFocusCell = getFocusedCell();
                TablePosition<S,?> newFocusCell = new TablePosition<>(tableView, row, column);
                setFocusedCell(newFocusCell);

                if (newFocusCell.equals(oldFocusCell)) {
                    // manually update the focus properties to ensure consistency
                    setFocusedIndex(row);
                    setFocusedItem(getModelItem(row));
                }
            }
        }

        /**
         * Convenience method for setting focus on a particular row or cell
         * using a {@link TablePosition}.
         *
         * @param pos The table position where focus should be set.
         */
        public void focus(TablePosition pos) {
            if (pos == null) return;
            focus(pos.getRow(), pos.getTableColumn());
        }


        /* *********************************************************************
         *                                                                     *
         * Public API                                                          *
         *                                                                     *
         **********************************************************************/

        /**
         * Tests whether the row / cell at the given location currently has the
         * focus within the TableView.
         */
        @Override public boolean isFocused(int row, TableColumn<S,?> column) {
            if (row < 0 || row >= getItemCount()) return false;

            TablePosition cell = getFocusedCell();
            boolean columnMatch = column == null || column.equals(cell.getTableColumn());

            return cell.getRow() == row && columnMatch;
        }

        /**
         * Causes the item at the given index to receive the focus. This does not
         * cause the current selection to change. Updates the focusedItem and
         * focusedIndex properties such that <code>focusedIndex = -1</code> unless
         * <pre><code>0 &lt;= index &lt; model size</code></pre>.
         *
         * @param index The index of the item to get focus.
         */
        @Override public void focus(int index) {
            if (index < 0 || index >= getItemCount()) {
                setFocusedCell(EMPTY_CELL);
            } else {
                setFocusedCell(new TablePosition<>(tableView, index, null));
            }
        }

        /**
         * Attempts to move focus to the cell above the currently focused cell.
         */
        @Override public void focusAboveCell() {
            TablePosition cell = getFocusedCell();

            if (getFocusedIndex() == -1) {
                focus(getItemCount() - 1, cell.getTableColumn());
            } else if (getFocusedIndex() > 0) {
                focus(getFocusedIndex() - 1, cell.getTableColumn());
            }
        }

        /**
         * Attempts to move focus to the cell below the currently focused cell.
         */
        @Override public void focusBelowCell() {
            TablePosition cell = getFocusedCell();
            if (getFocusedIndex() == -1) {
                focus(0, cell.getTableColumn());
            } else if (getFocusedIndex() != getItemCount() -1) {
                focus(getFocusedIndex() + 1, cell.getTableColumn());
            }
        }

        /**
         * Attempts to move focus to the cell to the left of the currently focused cell.
         */
        @Override public void focusLeftCell() {
            TablePosition cell = getFocusedCell();
            if (cell.getColumn() <= 0) return;
            focus(cell.getRow(), getTableColumn(cell.getTableColumn(), -1));
        }

        /**
         * Attempts to move focus to the cell to the right of the the currently focused cell.
         */
        @Override public void focusRightCell() {
            TablePosition cell = getFocusedCell();
            if (cell.getColumn() == getColumnCount() - 1) return;
            focus(cell.getRow(), getTableColumn(cell.getTableColumn(), 1));
        }

        /** {@inheritDoc} */
        @Override public void focusPrevious() {
            if (getFocusedIndex() == -1) {
                focus(0);
            } else if (getFocusedIndex() > 0) {
                focusAboveCell();
            }
        }

        /** {@inheritDoc} */
        @Override public void focusNext() {
            if (getFocusedIndex() == -1) {
                focus(0);
            } else if (getFocusedIndex() != getItemCount() -1) {
                focusBelowCell();
            }
        }

        /* *********************************************************************
         *                                                                     *
         * Private Implementation                                              *
         *                                                                     *
         **********************************************************************/

        private void updateDefaultFocus() {
            // when the items list totally changes, we should clear out
            // the focus
            int newValueIndex = -1;
            if (tableView.getItems() != null) {
                S focusedItem = getFocusedItem();
                if (focusedItem != null) {
                    newValueIndex = tableView.getItems().indexOf(focusedItem);
                }

                // we put focus onto the first item, if there is at least
                // one item in the list
                if (newValueIndex == -1) {
                    newValueIndex = tableView.getItems().size() > 0 ? 0 : -1;
                }
            }

            TablePosition<S,?> focusedCell = getFocusedCell();
            TableColumn<S,?> focusColumn = focusedCell != null && !EMPTY_CELL.equals(focusedCell) ?
               focusedCell.getTableColumn() : tableView.getVisibleLeafColumn(0);

            focus(newValueIndex, focusColumn);
        }

        private int getColumnCount() {
            return tableView.getVisibleLeafColumns().size();
        }

        // Gets a table column to the left or right of the current one, given an offset
        private TableColumn<S,?> getTableColumn(TableColumn<S,?> column, int offset) {
            int columnIndex = tableView.getVisibleLeafIndex(column);
            int newColumnIndex = columnIndex + offset;
            return tableView.getVisibleLeafColumn(newColumnIndex);
        }
    }
}
