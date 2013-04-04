/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.css.CssMetaData;
import com.sun.javafx.scene.control.accessible.AccessibleListItem;
import com.sun.javafx.accessible.providers.AccessibleProvider;
import javafx.css.PseudoClass;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.css.StyleableProperty;

/**
 * The Cell API is used for virtualized controls such as {@link ListView},
 * {@link TreeView}, and {@link TableView}.
 * A Cell is a {@link Labeled} {@link Control}, and is used to render a single 
 * "row" inside  a ListView, TreeView or TableView. Cells are also used for each 
 * individual 'cell' inside a TableView (i.e. each row/column intersection). See 
 * the JavaDoc for each control separately for more detail.
 * <p>
 * Every Cell is associated with a single data item (represented by the 
 * {@link #itemProperty() item} property). The Cell is responsible for
 * rendering that item and, where appropriate, for editing the item. An item
 * within a Cell may be represented by text or some other control such as a
 * {@link CheckBox}, {@link ChoiceBox} or any other {@link Node} such as a 
 * {@link HBox}, {@link GridPane}, or even a {@link Rectangle}.
 * <p>
 * Because TreeView, ListView, TableView and other such controls can potentially
 * be used for displaying incredibly large amounts of data, it is not feasible
 * to create an actual Cell for every single item in the control.
 * We represent extremely large data sets using only very few Cells. Each Cell
 * is "recycled", or reused. This is what we mean when we say that these controls
 * are virtualized.
 * <p>
 * Since Cell is a Control, it is essentially a "model". Its Skin is responsible
 * for defining the look and layout, while the Behavior is responsible for
 * handling all input events and using that information to modify the Control
 * state. Also, the Cell is styled from CSS just like any other Control.
 * However, it is not necessary to implement a Skin for most uses of a Cell.
 * This is because a cell factory can be set - this is detailed more shortly.
 * <p>
 * Because by far the most common use case for cells is to show text to a user,
 * this use case is specially optimized for within Cell. This is done by Cell 
 * extending from {@link Labeled}. This means that subclasses of Cell need only 
 * set the {@link #textProperty() text} property, rather than create a separate 
 * {@link Label} and set that within the Cell. However, for situations where
 * something more than just plain text is called for, it is possible to place
 * any {@link Node} in the Cell {@link #graphicProperty() graphic} property.
 * Despite the term, a graphic can be any Node, and will be fully interactive.
 * For example, a ListCell might be configured with a {@link Button} as its 
 * graphic. The Button text could then be bound to the cells
 * {@link #itemProperty() item} property. In this way, whenever the item in the 
 * Cell changes, the Button text is automatically updated.
 * <p>
 * Cell sets focusTraversable to false.
 * </p>
 * <p>
 * <b>Cell Factories</b>
 * <p>
 * The default representation of the Cell <code>item</code> is up to the various
 * virtualized container's skins to render. For example, the ListView by default
 * will convert the item to a String and call {@link #setText(java.lang.String)}
 * with this value. If you want to specialize the Cell used for the
 * ListView (for example), then you must provide an implementation of the
 * {@link ListView#cellFactoryProperty() cellFactory} callback function defined 
 * on the ListView. Similar API exists on most controls that use Cells (for example, 
 * {@link TreeView#cellFactoryProperty() TreeView}, 
 * {@link TableView#rowFactoryProperty() TableView},
 * {@link TableColumn#cellFactoryProperty() TableColumn} and 
 * {@link ListView#cellFactoryProperty() ListView}.
 * <p>
 * The cell factory is called by the platform whenever it determines that a new
 * cell needs to be created. For example, perhaps your ListView has 10 million
 * items. Creating all 10 million cells would be prohibitively expensive. So
 * instead the ListView skin implementation might only create just enough cells
 * to fit the visual space. If the ListView is resized to be larger, the system
 * will determine that it needs to create some additional cells. In this case
 * it will call the cellFactory callback function (if one is provided) to create
 * the Cell implementation that should be used. If no cell factory is provided,
 * the built-in default implementation will be used.
 * <p>
 * The implementation of the cell factory is then responsible not just for
 * creating a Cell instance, but also configuring that Cell such that it reacts
 * to changes in its state. For example, if I were to create
 * a custom Cell which formatted Numbers such that they would appear as currency
 * types, I might do so like this:
 * 
 * <pre>
 * public class MoneyFormatCell extends ListCell&lt;Number&gt; {
 *
 *     public MoneyFormatCell() {    }
 *       
 *     &#064;Override protected void updateItem(Number item, boolean empty) {
 *         // calling super here is very important - don't skip this!
 *         super.updateItem(item, empty);
 *           
 *         // format the number as if it were a monetary value using the 
 *         // formatting relevant to the current locale. This would format
 *         // 43.68 as "$43.68", and -23.67 as "-$23.67"
 *         setText(item == null ? "" : NumberFormat.getCurrencyInstance().format(item));
 *
 *         // change the text fill based on whether it is positive (green)
 *         // or negative (red). If the cell is selected, the text will 
 *         // always be white (so that it can be read against the blue 
 *         // background), and if the value is zero, we'll make it black.
 *         if (item != null) {
 *             double value = item.doubleValue();
 *             setTextFill(isSelected() ? Color.WHITE :
 *                 value == 0 ? Color.BLACK :
 *                 value < 0 ? Color.RED : Color.GREEN);
 *         }
 *     }
 * }</pre>
 * 
 * This class could then be used inside a ListView as such:
 * 
 * <pre>
 * ObservableList&lt;Number&gt; money = ...;
 * final ListView&lt;Number&gt; listView = new ListView&lt;Number&gt;(money);
 * listView.setCellFactory(new Callback&lt;ListView&lt;Number&gt;, ListCell&lt;Number&gt;&gt;() {
 *     &#064;Override public ListCell&lt;Number&gt; call(ListView&lt;Number&gt; list) {
 *         return new MoneyFormatCell();
 *     }
 * });</pre>
 *
 * In this example an anonymous inner class is created, that simply returns
 * instances of MoneyFormatCell whenever it is called. The MoneyFormatCell class
 * extends {@link ListCell}, overriding the 
 * {@link #updateItem(java.lang.Object, boolean) updateItem} method. This method
 * is called whenever the item in the cell changes, for example when the user 
 * scrolls the ListView or the content of the underlying data model changes 
 * (and the cell is reused to represent some different item in the ListView). 
 * Because of this, there is no need to manage bindings - simply react to the
 * change in items when this method occurs. In the example above, whenever the 
 * item changes, we update the cell text property, and also modify the text fill
 * to ensure that we get the correct visuals. In addition, if the cell is "empty" 
 * (meaning it is used to fill out space in the ListView but doesn't have any 
 * data associated with it), then we just use the empty String.
 * <p>
 * Note that there are additional 
 * methods prefixed with 'update' that may be of interest, so be
 * sure to read the API documentation for Cell, and subclasses of Cell, closely.
 * <p>
 * Of course, we can also use the binding API rather than overriding the 
 * 'update' methods. Shown below is a very trivial example of how this could
 * be achieved.
 *
 *
 * <pre>
 * public class BoundLabelCell extends ListCell&lt;String&gt; {
 *
 *     public TextFieldCell() {
 *         textProperty().bind(itemProperty());
 *     }
 * }
 * </pre>
 * 
 * <h3>Key Design Goals</h3>
 * <ul>
 *   <li>Both time and memory efficient for large data sets</li>
 *   <li>Easy to build and use libraries for custom cells</li>
 *   <li>Easy to customize cell visuals</li>
 *   <li>Easy to customize display formatting (12.34 as $12.34 or 1234% etc)</li>
 *   <li>Easy to extend for custom visuals</li>
 *   <li>Easy to have "panels" of data for the visuals</li>
 *   <li>Easy to animate the cell size or other properties</li>
 * </ul>
 * 
 * <h3>Key Use Cases</h3>
 * Following are a number of key use cases used to drive the Cell API design,
 * along with code examples showing how those use cases are satisfied by this
 * API. This is by no means to be considered the definitive list of capabilities
 * or features supported, but rather, to provide some guidance as to how to use
 * the Cell API. The examples below are focused on the ListView, but the same
 * philosophy applies to TreeCells or other kinds of cells.
 * <p>
 * <b>Changing the Cell's Colors</b>
 * <p>
 * This should be extraordinarily simple in JavaFX. Each Cell can be styled
 * directly from CSS. So for example, if you wanted to change the default
 * background of every cell in a ListView to be WHITE you could do the
 * following CSS:
 *
 * <pre>
 * .list-cell {
 *   -fx-padding: 3 3 3 3;
 *   -fx-background-color: white;
 * }</pre>
 * 
 * If you wanted to set the color of selected ListView cells to be blue, you
 * could add this to your CSS file:
 * 
 * <pre>
 * .list-cell:selected {
 *   -fx-background-color: blue;
 * }</pre>
 *
 * Most Cell implementations extend from {@link IndexedCell} rather than Cell.
 * IndexedCell adds two other pseudoclass states: "odd" and "even". Using this 
 * you can get alternate row striping by doing something like this in your CSS 
 * file:
 * 
 * <pre>
 * .list-cell:odd {
 *   -fx-background-color: grey;
 * }</pre>
 * 
 * Each of these examples require no code changes. Simply update your CSS
 * file to alter the colors. You can also use the "hover" and other
 * pseudoclasses in CSS the same as with other controls.
 * <p>
 * Another approach to the first example above (formatting a list of numbers) would
 * be to use style classes. Suppose you had an {@link ObservableList} of Numbers 
 * to display in a ListView and wanted to color all of the negative values red 
 * and all positive or 0 values black.
 * One way to achieve this is with a custom cellFactory which changes the
 * styleClass of the Cell based on whether the value is negative or positive. This
 * is as simple as adding code to test if the number in the cell is negative, 
 * and adding a "negative" styleClass. If the number is not negative, the "negative"
 * string should be removed. This approach allows for the colors to be defined 
 * from CSS, allowing for simple customization. The CSS file would then include
 * something like the following:
 *
 * <pre>
 * .list-cell {
 *   -fx-text-fill: black;
 * }
 * 
 * .list-cell .negative {
 *   -fx-text-fill: red;
 * }</pre>
 * 
 * 
 * @param <T> The type of the item contained within the Cell.
 *
 */
public class Cell<T> extends Labeled {
    
    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default Cell with the default style class of 'cell'.
     */
    public Cell() {
        setText(null); // default to null text, to match the null item
        // focusTraversable is styleable through css. Calling setFocusTraversable
        // makes it look to css like the user set the value and css will not 
        // override. Initializing focusTraversable by calling set on the 
        // CssMetaData ensures that css will be able to override the value.
        ((StyleableProperty)focusTraversableProperty()).applyStyle(null, Boolean.FALSE);
        getStyleClass().addAll(DEFAULT_STYLE_CLASS);

        /**
         * Indicates whether or not this cell has focus. For example, a
         * ListView defines zero or one cell as being the "focused" cell. This cell
         * would have focused set to true.
         */
        super.focusedProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable property) {
                pseudoClassStateChanged(PSEUDO_CLASS_FOCUSED, isFocused()); // TODO is this necessary??

                // The user has shifted focus, so we should cancel the editing on this cell
                if (!isFocused() && isEditing()) {
                    cancelEdit();
                }
            }
        });
        
        // initialize default pseudo-class state
        pseudoClassStateChanged(PSEUDO_CLASS_EMPTY, true);
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- item
    private ObjectProperty<T> item = new SimpleObjectProperty<T>(this, "item");

    /**
     * The data value associated with this Cell. This value is set by the
     * virtualized Control when the Cell is created or updated. This represents
     * the raw data value. 
    *
    * <p>This value should only be set in subclasses of Cell by the virtualised 
    * user interface controls that know how to properly work with the Cell 
    * class.
     */
    public final ObjectProperty<T> itemProperty() { return item; }
    
    /** 
     * Sets the item to the given value - should not be called directly as the
     * item is managed by the virtualized control.
     */
    public final void setItem(T value) { itemProperty().set(value); }
    
    /**
     * Returns the data value associated with this Cell.
     */
    public final T getItem() { return itemProperty().get(); }
    
    

    // --- empty
    private ReadOnlyBooleanWrapper empty = new ReadOnlyBooleanWrapper(true) {
        @Override protected void invalidated() {
            final boolean active = get();
            pseudoClassStateChanged(PSEUDO_CLASS_EMPTY,   active);
            pseudoClassStateChanged(PSEUDO_CLASS_FILLED, !active);
        }

        @Override
        public Object getBean() {
            return Cell.this;
        }
        
        @Override
        public String getName() {
            return "empty";
        }
    };
    
    /**
     * A property used to represent whether the cell has any contents.
     * If true, then the Cell contains no data and is not associated with any
     * data item in the virtualized Control. 
     * 
     * <p>When a cell is empty, it can be styled differently via the 'empty' 
     * CSS pseudo class state. For example, it may not receive any 
     * alternate row highlighting, or it may not receive hover background 
     * fill when hovered.
     */
    public final ReadOnlyBooleanProperty emptyProperty() { return empty.getReadOnlyProperty(); }
    
    private void setEmpty(boolean value) { empty.set(value); }
    
    /**
     * Returns a boolean representing whether the cell is considered to be empty
     * or not.
     */
    public final boolean isEmpty() { return emptyProperty().get(); }
    
    

    // --- selected
    private ReadOnlyBooleanWrapper selected = new ReadOnlyBooleanWrapper() {
        @Override protected void invalidated() {
            pseudoClassStateChanged(PSEUDO_CLASS_SELECTED, get());
        }

        @Override
        public Object getBean() {
            return Cell.this;
        }
        
        @Override
        public String getName() {
            return "selected";
        }
    };
    
    /**
     * Indicates whether or not this cell has been selected. For example, a
     * ListView defines zero or more cells as being the "selected" cells.
     */
    public final ReadOnlyBooleanProperty selectedProperty() { return selected.getReadOnlyProperty(); }

    void setSelected(boolean value) { selected.set(value); }
    
    /**
     * Returns whether this cell is currently selected or not.
     * @return True if the cell is selected, false otherwise.
     */
    public final boolean isSelected() { return selectedProperty().get(); }
    
    
    
    // --- Editing
    private ReadOnlyBooleanWrapper editing;

    private void setEditing(boolean value) {
        editingPropertyImpl().set(value);
    }

    /**
     * Represents whether the cell is currently in its editing state or not.
     */
    public final boolean isEditing() {
        return editing == null ? false : editing.get();
    }

    /**
     * Property representing whether this cell is currently in its editing state.
     */
    public final ReadOnlyBooleanProperty editingProperty() {
        return editingPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyBooleanWrapper editingPropertyImpl() {
        if (editing == null) {
            editing = new ReadOnlyBooleanWrapper(this, "editing");
        }
        return editing;
    }
    
    
    
    // --- Editable
    private BooleanProperty editable;

    /**
     * Allows for certain cells to not be able to be edited. This is useful in
     * cases where, say, a List has 'header rows' - it does not make sense for
     * the header rows to be editable, so they should have editable set to 
     * false.
     * 
     * @param value A boolean representing whether the cell is editable or not.
     *      If true, the cell is editable, and if it is false, the cell can not
     *      be edited.
     */
    public final void setEditable(boolean value) {
        editableProperty().set(value);
    }

    /**
     * Returns whether this cell is allowed to be put into an editing state.
     */
    public final boolean isEditable() {
        return editable == null ? true : editable.get();
    }

    /**
     * A property representing whether this cell is allowed to be put into an 
     * editing state. By default editable is set to true in Cells (although for 
     * a subclass of Cell to be allowed to enter its editing state, it may have
     * to satisfy additional criteria. For example, ListCell requires that the
     * ListView {@link ListView#editableProperty() editable} property is also
     * true.
     */
    public final BooleanProperty editableProperty() {
        if (editable == null) {
            editable = new SimpleBooleanProperty(this, "editable", true);
        }
        return editable;
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * Call this function to transition from a non-editing state into an editing
     * state, if the cell is editable. If this cell is already in an editing 
     * state, it will stay in it.
     */
    public void startEdit() {
        if (isEditable() && !isEditing() && !isEmpty()) {
            setEditing(true);
        }
    }

    /**
     * Call this function to transition from an editing state into a non-editing
     * state, without saving any user input.
     */
    public void cancelEdit() {
        if (isEditing()) {
            setEditing(false);
        }
    }

    /**
     * Call this function to transition from an editing state into a non-editing
     * state, and in the process saving any user input.
     * 
     * @param newValue The value as input by the end user, which should be 
     *      persisted in the relevant way given the data source underpinning the
     *      user interface.
     */
    public void commitEdit(T newValue) {
        if (isEditing()) {
            setEditing(false);
        }
    }



    /***************************************************************************
     *                                                                         *
     * Expert API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * <p>
     *     Updates the item associated with this Cell. This method should <strong>
     *     only</strong> be called by Skin implementations of ListView, TableView,
     *     TreeView, or other controls using Cells. It is not intended to be called
     *     by application developers.
     * </p>
     * <p>
     *     Because <code>null</code> is a perfectly valid value in the application
     *     domain, Cell needs some way to distinguish whether or not the cell
     *     actually holds a value. The <code>empty</code> flag indicates this.
     *     It is an error to supply a non-null <code>item</code> but a true value for
     *     <code>empty</code>.
     * </p>
     * @param item The new item for the cell
     * @param empty whether or not this cell represents data from the list. If it
     *        is empty, then it does not represent any domain data, but is a cell
     *        being used to render an "empty" row.
     * @expert
     */
    protected void updateItem(T item, boolean empty) {
        if (isEditing()) cancelEdit();
        setItem(item);
        setEmpty(empty);
        if (empty && isSelected()) {
            updateSelected(false);
        }
    }
    
    /**
     * Updates whether this cell is in a selected state or not.
     * @expert
     * @param selected whether or not to select this cell.
     */
    public void updateSelected(boolean selected) {
        if (selected && isEmpty()) return;
        setSelected(selected);
    }

    private AccessibleListItem accListItem ;
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated @Override public AccessibleProvider impl_getAccessible() {
        if( accListItem == null)
            accListItem = new AccessibleListItem(this);
        return (AccessibleProvider)accListItem ;
    }
    
    
    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "cell";
    private static final PseudoClass PSEUDO_CLASS_SELECTED =
            PseudoClass.getPseudoClass("selected");
    private static final PseudoClass PSEUDO_CLASS_FOCUSED = 
            PseudoClass.getPseudoClass("focused");
    private static final PseudoClass PSEUDO_CLASS_EMPTY =
            PseudoClass.getPseudoClass("empty");
    private static final PseudoClass PSEUDO_CLASS_FILLED =
            PseudoClass.getPseudoClass("filled");

    /**
      * Most Controls return true for focusTraversable, so Control overrides
      * this method to return true, but Cell returns false for
      * focusTraversable's initial value; hence the override of the override. 
      * This method is called from CSS code to get the correct initial value.
      * @treatAsPrivate implementation detail
      */
    @Deprecated @Override
    protected /*do not make final*/ Boolean impl_cssGetFocusTraversableInitialValue() {
        return Boolean.FALSE;
    }
    
}
