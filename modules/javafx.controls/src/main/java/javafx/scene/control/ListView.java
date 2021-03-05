/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.sun.javafx.scene.control.Properties;
import com.sun.javafx.scene.control.behavior.ListCellBehavior;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.css.StyleableDoubleProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Orientation;
import javafx.scene.layout.Region;
import javafx.util.Callback;
import javafx.css.StyleableObjectProperty;
import javafx.css.CssMetaData;

import javafx.css.converter.EnumConverter;

import javafx.collections.WeakListChangeListener;

import javafx.css.converter.SizeConverter;
import javafx.scene.control.skin.ListViewSkin;

import java.lang.ref.WeakReference;

import javafx.css.PseudoClass;
import javafx.beans.DefaultProperty;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.util.Pair;

/**
 * A ListView displays a horizontal or vertical list of items from which the
 * user may select, or with which the user may interact. A ListView is able to
 * have its generic type set to represent the type of data in the backing model.
 * Doing this has the benefit of making various methods in the ListView, as well
 * as the supporting classes (mentioned below), type-safe. In addition, making
 * use of the generic type supports substantially simplified development of applications
 * making use of ListView, as all modern IDEs are able to auto-complete far
 * more successfully with the additional type information.
 *
 * <h2>Populating a ListView</h2>
 * <p>A simple example of how to create and populate a ListView of names (Strings)
 * is shown here:
 *
 * <pre> {@code ObservableList<String> names = FXCollections.observableArrayList(
 *          "Julia", "Ian", "Sue", "Matthew", "Hannah", "Stephan", "Denise");
 * ListView<String> listView = new ListView<String>(names);}</pre>
 *
 * <p>The elements of the ListView are contained within the
 * {@link #itemsProperty() items} {@link ObservableList}. This
 * ObservableList is automatically observed by the ListView, such that any
 * changes that occur inside the ObservableList will be automatically shown in
 * the ListView itself. If passing the <code>ObservableList</code> in to the
 * ListView constructor is not feasible, the recommended approach for setting
 * the items is to simply call:
 *
 * <pre> {@code ObservableList<T> content = ...
 * listView.setItems(content);}</pre>
 *
 * <img src="doc-files/ListView.png" alt="Image of the ListView control">
 *
 * <p>The end result of this is, as noted above, that the ListView will automatically
 * refresh the view to represent the items in the list.
 *
 * <p>Another approach, whilst accepted by the ListView, <b>is not the
 * recommended approach</b>:
 *
 * <pre> {@code List<T> content = ...
 * getItems().setAll(content);}</pre>
 *
 * The issue with the approach shown above is that the content list is being
 * copied into the items list - meaning that subsequent changes to the content
 * list are not observed, and will not be reflected visually within the ListView.
 *
 * <h2>ListView Selection / Focus APIs</h2>
 * <p>To track selection and focus, it is necessary to become familiar with the
 * {@link SelectionModel} and {@link FocusModel} classes. A ListView has at most
 * one instance of each of these classes, available from
 * {@link #selectionModelProperty() selectionModel} and
 * {@link #focusModelProperty() focusModel} properties respectively.
 * Whilst it is possible to use this API to set a new selection model, in
 * most circumstances this is not necessary - the default selection and focus
 * models should work in most circumstances.
 *
 * <p>The default {@link SelectionModel} used when instantiating a ListView is
 * an implementation of the {@link MultipleSelectionModel} abstract class.
 * However, as noted in the API documentation for
 * the {@link MultipleSelectionModel#selectionModeProperty() selectionMode}
 * property, the default value is {@link SelectionMode#SINGLE}. To enable
 * multiple selection in a default ListView instance, it is therefore necessary
 * to do the following:
 *
 * <pre> {@code listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);}</pre>
 *
 * <h2>Customizing ListView Visuals</h2>
 * <p>The visuals of the ListView can be entirely customized by replacing the
 * default {@link #cellFactoryProperty() cell factory}. A cell factory is used to
 * generate {@link ListCell} instances, which are used to represent an item in the
 * ListView. See the {@link Cell} class documentation for a more complete
 * description of how to write custom Cells.
 *
 * <h2>Editing</h2>
 * <p>This control supports inline editing of values, and this section attempts to
 * give an overview of the available APIs and how you should use them.</p>
 *
 * <p>Firstly, cell editing most commonly requires a different user interface
 * than when a cell is not being edited. This is the responsibility of the
 * {@link Cell} implementation being used. For ListView, this is the responsibility
 * of the {@link #cellFactoryProperty() cell factory}. It is your choice whether the cell is
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
 * ListView, which you can observe by adding an {@link EventHandler} via
 * {@link ListView#setOnEditCommit(javafx.event.EventHandler)}. Similarly,
 * you can also observe edit events for
 * {@link ListView#setOnEditStart(javafx.event.EventHandler) edit start}
 * and {@link ListView#setOnEditCancel(javafx.event.EventHandler) edit cancel}.</p>
 *
 * <p>By default the ListView edit commit handler is non-null, with a default
 * handler that attempts to overwrite the property value for the
 * item in the currently-being-edited row. It is able to do this as the
 * {@link Cell#commitEdit(Object)} method is passed in the new value, and this
 * is passed along to the edit commit handler via the
 * {@link EditEvent} that is fired. It is simply a matter of calling
 * {@link EditEvent#getNewValue()} to retrieve this value.
 *
 * <p>It is very important to note that if you call
 * {@link ListView#setOnEditCommit(javafx.event.EventHandler)} with your own
 * {@link EventHandler}, then you will be removing the default handler. Unless
 * you then handle the writeback to the property (or the relevant data source),
 * nothing will happen. You can work around this by using the
 * {@link ListView#addEventHandler(javafx.event.EventType, javafx.event.EventHandler)}
 * method to add a {@link ListView#editCommitEvent()} {@link EventType} with
 * your desired {@link EventHandler} as the second argument. Using this method,
 * you will not replace the default implementation, but you will be notified when
 * an edit commit has occurred.</p>
 *
 * <p>Hopefully this summary answers some of the commonly asked questions.
 * Fortunately, JavaFX ships with a number of pre-built cell factories that
 * handle all the editing requirements on your behalf. You can find these
 * pre-built cell factories in the javafx.scene.control.cell package.</p>
 *
 * @see ListCell
 * @see MultipleSelectionModel
 * @see FocusModel
 * @param <T> This type is used to represent the type of the objects stored in
 *          the ListViews {@link #itemsProperty() items} ObservableList. It is
 *          also used in the {@link #selectionModelProperty() selection model}
 *          and {@link #focusModelProperty() focus model}.
 * @since JavaFX 2.0
 */
// TODO add code examples
@DefaultProperty("items")
public class ListView<T> extends Control {

    /***************************************************************************
     *                                                                         *
     * Static properties and methods                                           *
     *                                                                         *
     **************************************************************************/

    /**
     * An EventType that indicates some edit event has occurred. It is the parent
     * type of all other edit events: {@link #editStartEvent()},
     *  {@link #editCommitEvent()} and {@link #editCancelEvent()}.
     * @param <T> the type of the objects stored in this ListView
     * @return the event type
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<ListView.EditEvent<T>> editAnyEvent() {
        return (EventType<ListView.EditEvent<T>>) EDIT_ANY_EVENT;
    }
    private static final EventType<?> EDIT_ANY_EVENT =
            new EventType<>(Event.ANY, "LIST_VIEW_EDIT");

    /**
     * An EventType used to indicate that an edit event has started within the
     * ListView upon which the event was fired.
     * @param <T> the type of the objects stored in this ListView
     * @return the event type
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<ListView.EditEvent<T>> editStartEvent() {
        return (EventType<ListView.EditEvent<T>>) EDIT_START_EVENT;
    }
    private static final EventType<?> EDIT_START_EVENT =
            new EventType<>(editAnyEvent(), "EDIT_START");

    /**
     * An EventType used to indicate that an edit event has just been canceled
     * within the ListView upon which the event was fired.
     * @param <T> the type of the objects stored in this ListView
     * @return the event type
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<ListView.EditEvent<T>> editCancelEvent() {
        return (EventType<ListView.EditEvent<T>>) EDIT_CANCEL_EVENT;
    }
    private static final EventType<?> EDIT_CANCEL_EVENT =
            new EventType<>(editAnyEvent(), "EDIT_CANCEL");

    /**
     * An EventType used to indicate that an edit event has been committed
     * within the ListView upon which the event was fired.
     * @param <T> the type of the objects stored in this ListView
     * @return the event type
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<ListView.EditEvent<T>> editCommitEvent() {
        return (EventType<ListView.EditEvent<T>>) EDIT_COMMIT_EVENT;
    }
    private static final EventType<?> EDIT_COMMIT_EVENT =
            new EventType<>(editAnyEvent(), "EDIT_COMMIT");



    /***************************************************************************
     *                                                                         *
     * Fields                                                                  *
     *                                                                         *
     **************************************************************************/

    // by default we always select the first row in the ListView, and when the
    // items list changes, we also reselect the first row. In some cases, such as
    // for the ComboBox, this is not desirable, so it can be disabled here.
    private boolean selectFirstRowByDefault = true;



    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default ListView which will display contents stacked vertically.
     * As no {@link ObservableList} is provided in this constructor, an empty
     * ObservableList is created, meaning that it is legal to directly call
     * {@link #getItems()} if so desired. However, as noted elsewhere, this
     * is not the recommended approach
     * (instead call {@link #setItems(javafx.collections.ObservableList)}).
     *
     * <p>Refer to the {@link ListView} class documentation for details on the
     * default state of other properties.
     */
    public ListView() {
        this(FXCollections.<T>observableArrayList());
    }

    /**
     * Creates a default ListView which will stack the contents retrieved from the
     * provided {@link ObservableList} vertically.
     *
     * <p>Attempts to add a listener to the {@link ObservableList}, such that all
     * subsequent changes inside the list will be shown to the user.
     *
     * <p>Refer to the {@link ListView} class documentation for details on the
     * default state of other properties.
     * @param items the list of items
     */
    public ListView(ObservableList<T> items) {
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        setAccessibleRole(AccessibleRole.LIST_VIEW);

        setItems(items);

        // Install default....
        // ...selection model
        setSelectionModel(new ListView.ListViewBitSetSelectionModel<T>(this));

        // ...focus model
        setFocusModel(new ListView.ListViewFocusModel<T>(this));

        // ...edit commit handler
        setOnEditCommit(DEFAULT_EDIT_COMMIT_HANDLER);

        // Fix for RT-36651, which was introduced by RT-35679 (above) and resolved
        // by having special-case code to remove the listener when requested.
        // This is done by ComboBoxListViewSkin, so that selection is not done
        // when a ComboBox is shown.
        getProperties().addListener((MapChangeListener<Object, Object>) change -> {
            if (change.wasAdded() && "selectFirstRowByDefault".equals(change.getKey())) {
                Boolean _selectFirstRowByDefault = (Boolean) change.getValueAdded();
                if (_selectFirstRowByDefault == null) return;
                selectFirstRowByDefault = _selectFirstRowByDefault;
            }
        });

        pseudoClassStateChanged(PSEUDO_CLASS_VERTICAL, true);
    }



    /***************************************************************************
     *                                                                         *
     * Callbacks and Events                                                    *
     *                                                                         *
     **************************************************************************/

    private EventHandler<ListView.EditEvent<T>> DEFAULT_EDIT_COMMIT_HANDLER = t -> {
        int index = t.getIndex();
        List<T> list = getItems();
        if (index < 0 || index >= list.size()) return;
        list.set(index, t.getNewValue());
    };



    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- Items
    private ObjectProperty<ObservableList<T>> items;

    /**
     * Sets the underlying data model for the ListView. Note that it has a generic
     * type that must match the type of the ListView itself.
     * @param value the list of items for this ListView
     */
    public final void setItems(ObservableList<T> value) {
        itemsProperty().set(value);
    }

    /**
     * Returns an {@link ObservableList} that contains the items currently being
     * shown to the user. This may be null if
     * {@link #setItems(javafx.collections.ObservableList)} has previously been
     * called, however, by default it is an empty ObservableList.
     *
     * @return An ObservableList containing the items to be shown to the user, or
     *      null if the items have previously been set to null.
     */
    public final ObservableList<T> getItems() {
        return items == null ? null : items.get();
    }

    /**
     * The underlying data model for the ListView. Note that it has a generic
     * type that must match the type of the ListView itself.
     * @return the items property for this ListView
     */
    public final ObjectProperty<ObservableList<T>> itemsProperty() {
        if (items == null) {
            items = new SimpleObjectProperty<>(this, "items");
        }
        return items;
    }


    // --- Placeholder Node
    private ObjectProperty<Node> placeholder;
    /**
     * This Node is shown to the user when the listview has no content to show.
     * This may be the case because the table model has no data in the first
     * place or that a filter has been applied to the list model, resulting
     * in there being nothing to show the user..
     * @return the placeholder property for this ListView
     * @since JavaFX 8.0
     */
    public final ObjectProperty<Node> placeholderProperty() {
        if (placeholder == null) {
            placeholder = new SimpleObjectProperty<Node>(this, "placeholder");
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
    private ObjectProperty<MultipleSelectionModel<T>> selectionModel = new SimpleObjectProperty<MultipleSelectionModel<T>>(this, "selectionModel");

    /**
     * Sets the {@link MultipleSelectionModel} to be used in the ListView.
     * Despite a ListView requiring a <b>Multiple</b>SelectionModel, it is possible
     * to configure it to only allow single selection (see
     * {@link MultipleSelectionModel#setSelectionMode(javafx.scene.control.SelectionMode)}
     * for more information).
     * @param value the MultipleSelectionModel to be used in this ListView
     */
    public final void setSelectionModel(MultipleSelectionModel<T> value) {
        selectionModelProperty().set(value);
    }

    /**
     * Returns the currently installed selection model.
     * @return the currently installed selection model
     */
    public final MultipleSelectionModel<T> getSelectionModel() {
        return selectionModel == null ? null : selectionModel.get();
    }

    /**
     * The SelectionModel provides the API through which it is possible
     * to select single or multiple items within a ListView, as  well as inspect
     * which items have been selected by the user. Note that it has a generic
     * type that must match the type of the ListView itself.
     * @return the selectionModel property
     */
    public final ObjectProperty<MultipleSelectionModel<T>> selectionModelProperty() {
        return selectionModel;
    }


    // --- Focus Model
    private ObjectProperty<FocusModel<T>> focusModel;

    /**
     * Sets the {@link FocusModel} to be used in the ListView.
     * @param value the FocusModel to be used in the ListView
     */
    public final void setFocusModel(FocusModel<T> value) {
        focusModelProperty().set(value);
    }

    /**
     * Returns the currently installed {@link FocusModel}.
     * @return the currently installed FocusModel
     */
    public final FocusModel<T> getFocusModel() {
        return focusModel == null ? null : focusModel.get();
    }

    /**
     * The FocusModel provides the API through which it is possible
     * to both get and set the focus on a single item within a ListView. Note
     * that it has a generic type that must match the type of the ListView itself.
     * @return the FocusModel property
     */
    public final ObjectProperty<FocusModel<T>> focusModelProperty() {
        if (focusModel == null) {
            focusModel = new SimpleObjectProperty<FocusModel<T>>(this, "focusModel");
        }
        return focusModel;
    }


    // --- Orientation
    private ObjectProperty<Orientation> orientation;

    /**
     * Sets the orientation of the ListView, which dictates whether
     * it scrolls vertically or horizontally.
     * @param value the orientation of the ListView
     */
    public final void setOrientation(Orientation value) {
        orientationProperty().set(value);
    };

    /**
     * Returns the current orientation of the ListView, which dictates whether
     * it scrolls vertically or horizontally.
     * @return the current orientation of the ListView
     */
    public final Orientation getOrientation() {
        return orientation == null ? Orientation.VERTICAL : orientation.get();
    }

    /**
     * The orientation of the {@code ListView} - this can either be horizontal
     * or vertical.
     * @return the orientation property of this ListView
     */
    public final ObjectProperty<Orientation> orientationProperty() {
        if (orientation == null) {
            orientation = new StyleableObjectProperty<Orientation>(Orientation.VERTICAL) {
                @Override public void invalidated() {
                    final boolean active = (get() == Orientation.VERTICAL);
                    pseudoClassStateChanged(PSEUDO_CLASS_VERTICAL,    active);
                    pseudoClassStateChanged(PSEUDO_CLASS_HORIZONTAL, !active);
                }

                @Override
                public CssMetaData<ListView<?>,Orientation> getCssMetaData() {
                    return ListView.StyleableProperties.ORIENTATION;
                }

                @Override
                public Object getBean() {
                    return ListView.this;
                }

                @Override
                public String getName() {
                    return "orientation";
                }
            };
        }
        return orientation;
    }




    // --- Cell Factory
    private ObjectProperty<Callback<ListView<T>, ListCell<T>>> cellFactory;

    /**
     * Sets a new cell factory to use in the ListView. This forces all old
     * {@link ListCell}'s to be thrown away, and new ListCell's created with
     * the new cell factory.
     * @param value cell factory to use in this ListView
     */
    public final void setCellFactory(Callback<ListView<T>, ListCell<T>> value) {
        cellFactoryProperty().set(value);
    }

    /**
     * Returns the current cell factory.
     * @return the current cell factory
     */
    public final Callback<ListView<T>, ListCell<T>> getCellFactory() {
        return cellFactory == null ? null : cellFactory.get();
    }

    /**
     * <p>Setting a custom cell factory has the effect of deferring all cell
     * creation, allowing for total customization of the cell. Internally, the
     * ListView is responsible for reusing ListCells - all that is necessary
     * is for the custom cell factory to return from this function a ListCell
     * which might be usable for representing any item in the ListView.
     *
     * <p>Refer to the {@link Cell} class documentation for more detail.
     * @return the cell factory property
     */
    public final ObjectProperty<Callback<ListView<T>, ListCell<T>>> cellFactoryProperty() {
        if (cellFactory == null) {
            cellFactory = new SimpleObjectProperty<Callback<ListView<T>, ListCell<T>>>(this, "cellFactory");
        }
        return cellFactory;
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
     * @return the fixed cell size property
     * @since JavaFX 8.0
     */
    public final DoubleProperty fixedCellSizeProperty() {
        if (fixedCellSize == null) {
            fixedCellSize = new StyleableDoubleProperty(Region.USE_COMPUTED_SIZE) {
                @Override public CssMetaData<ListView<?>,Number> getCssMetaData() {
                    return StyleableProperties.FIXED_CELL_SIZE;
                }

                @Override public Object getBean() {
                    return ListView.this;
                }

                @Override public String getName() {
                    return "fixedCellSize";
                }
            };
        }
        return fixedCellSize;
    }


    // --- Editable
    private BooleanProperty editable;
    public final void setEditable(boolean value) {
        editableProperty().set(value);
    }
    public final boolean isEditable() {
        return editable == null ? false : editable.get();
    }
    /**
     * Specifies whether this ListView is editable - only if the ListView and
     * the ListCells within it are both editable will a ListCell be able to go
     * into their editing state.
     * @return the editable property
     */
    public final BooleanProperty editableProperty() {
        if (editable == null) {
            editable = new SimpleBooleanProperty(this, "editable", false);
        }
        return editable;
    }


    // --- Editing Index
    private ReadOnlyIntegerWrapper editingIndex;

    private void setEditingIndex(int value) {
        editingIndexPropertyImpl().set(value);
    }

    /**
     * Returns the index of the item currently being edited in the ListView,
     * or -1 if no item is being edited.
     * @return the index of the item currently being edited
     */
    public final int getEditingIndex() {
        return editingIndex == null ? -1 : editingIndex.get();
    }

    /**
     * <p>A property used to represent the index of the item currently being edited
     * in the ListView, if editing is taking place, or -1 if no item is being edited.
     *
     * <p>It is not possible to set the editing index, instead it is required that
     * you call {@link #edit(int)}.
     * @return the editing index property
     */
    public final ReadOnlyIntegerProperty editingIndexProperty() {
        return editingIndexPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyIntegerWrapper editingIndexPropertyImpl() {
        if (editingIndex == null) {
            editingIndex = new ReadOnlyIntegerWrapper(this, "editingIndex", -1);
        }
        return editingIndex;
    }


    // --- On Edit Start
    private ObjectProperty<EventHandler<ListView.EditEvent<T>>> onEditStart;

    /**
     * Sets the {@link EventHandler} that will be called when the user begins
     * an edit.
     *
     * <p>This is a convenience method - the same result can be
     * achieved by calling
     * <code>addEventHandler(ListView.EDIT_START_EVENT, eventHandler)</code>.
     * @param value the EventHandler that will be called when the user begins
     * an edit
     */
    public final void setOnEditStart(EventHandler<ListView.EditEvent<T>> value) {
        onEditStartProperty().set(value);
    }

    /**
     * Returns the {@link EventHandler} that will be called when the user begins
     * an edit.
     * @return the EventHandler that will be called when the user begins an edit
     */
    public final EventHandler<ListView.EditEvent<T>> getOnEditStart() {
        return onEditStart == null ? null : onEditStart.get();
    }

    /**
     * This event handler will be fired when the user successfully initiates
     * editing.
     * @return the onEditStart event handler property
     */
    public final ObjectProperty<EventHandler<ListView.EditEvent<T>>> onEditStartProperty() {
        if (onEditStart == null) {
            onEditStart = new ObjectPropertyBase<EventHandler<ListView.EditEvent<T>>>() {
                @Override protected void invalidated() {
                    setEventHandler(ListView.<T>editStartEvent(), get());
                }

                @Override
                public Object getBean() {
                    return ListView.this;
                }

                @Override
                public String getName() {
                    return "onEditStart";
                }
            };
        }
        return onEditStart;
    }


    // --- On Edit Commit
    private ObjectProperty<EventHandler<ListView.EditEvent<T>>> onEditCommit;

    /**
     * Sets the {@link EventHandler} that will be called when the user has
     * completed their editing. This is called as part of the
     * {@link ListCell#commitEdit(java.lang.Object)} method.
     *
     * <p>This is a convenience method - the same result can be
     * achieved by calling
     * <code>addEventHandler(ListView.EDIT_START_EVENT, eventHandler)</code>.
     * @param value the EventHandler that will be called when the user has
     * completed their editing
     */
    public final void setOnEditCommit(EventHandler<ListView.EditEvent<T>> value) {
        onEditCommitProperty().set(value);
    }

    /**
     * Returns the {@link EventHandler} that will be called when the user commits
     * an edit.
     * @return the EventHandler that will be called when the user commits an edit
     */
    public final EventHandler<ListView.EditEvent<T>> getOnEditCommit() {
        return onEditCommit == null ? null : onEditCommit.get();
    }

    /**
     * <p>This property is used when the user performs an action that should
     * result in their editing input being persisted.</p>
     *
     * <p>The EventHandler in this property should not be called directly -
     * instead call {@link ListCell#commitEdit(java.lang.Object)} from within
     * your custom ListCell. This will handle firing this event, updating the
     * view, and switching out of the editing state.</p>
     * @return the onEditCommit event handler property
     */
    public final ObjectProperty<EventHandler<ListView.EditEvent<T>>> onEditCommitProperty() {
        if (onEditCommit == null) {
            onEditCommit = new ObjectPropertyBase<EventHandler<ListView.EditEvent<T>>>() {
                @Override protected void invalidated() {
                    setEventHandler(ListView.<T>editCommitEvent(), get());
                }

                @Override
                public Object getBean() {
                    return ListView.this;
                }

                @Override
                public String getName() {
                    return "onEditCommit";
                }
            };
        }
        return onEditCommit;
    }


    // --- On Edit Cancel
    private ObjectProperty<EventHandler<ListView.EditEvent<T>>> onEditCancel;

    /**
     * Sets the {@link EventHandler} that will be called when the user cancels
     * an edit.
     * @param value the EventHandler that will be called when the user cancels
     * an edit
     */
    public final void setOnEditCancel(EventHandler<ListView.EditEvent<T>> value) {
        onEditCancelProperty().set(value);
    }

    /**
     * Returns the {@link EventHandler} that will be called when the user cancels
     * an edit.
     * @return the EventHandler that will be called when the user cancels an edit
     */
    public final EventHandler<ListView.EditEvent<T>> getOnEditCancel() {
        return onEditCancel == null ? null : onEditCancel.get();
    }

    /**
     * This event handler will be fired when the user cancels editing a cell.
     * @return the onEditCancel event handler property
     */
    public final ObjectProperty<EventHandler<ListView.EditEvent<T>>> onEditCancelProperty() {
        if (onEditCancel == null) {
            onEditCancel = new ObjectPropertyBase<EventHandler<ListView.EditEvent<T>>>() {
                @Override protected void invalidated() {
                    setEventHandler(ListView.<T>editCancelEvent(), get());
                }

                @Override
                public Object getBean() {
                    return ListView.this;
                }

                @Override
                public String getName() {
                    return "onEditCancel";
                }
            };
        }
        return onEditCancel;
    }




    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * Instructs the ListView to begin editing the item in the given index, if
     * the ListView is {@link #editableProperty() editable}. Once
     * this method is called, if the current {@link #cellFactoryProperty()} is
     * set up to support editing, the Cell will switch its visual state to enable
     * for user input to take place.
     *
     * @param itemIndex The index of the item in the ListView that should be
     *     edited.
     */
    public void edit(int itemIndex) {
        if (!isEditable()) return;
        setEditingIndex(itemIndex);
    }

    /**
     * Scrolls the ListView such that the item in the given index is visible to
     * the end user.
     *
     * @param index The index that should be made visible to the user, assuming
     *      of course that it is greater than, or equal to 0, and less than the
     *      size of the items list contained within the given ListView.
     */
    public void scrollTo(int index) {
        ControlUtils.scrollToIndex(this, index);
    }

    /**
     * Scrolls the ListView so that the given object is visible within the viewport.
     * @param object The object that should be visible to the user.
     * @since JavaFX 8.0
     */
    public void scrollTo(T object) {
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
            onScrollTo = new ObjectPropertyBase<EventHandler<ScrollToEvent<Integer>>>() {
                @Override protected void invalidated() {
                    setEventHandler(ScrollToEvent.scrollToTopIndex(), get());
                }

                @Override public Object getBean() {
                    return ListView.this;
                }

                @Override public String getName() {
                    return "onScrollTo";
                }
            };
        }
        return onScrollTo;
    }

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new ListViewSkin<T>(this);
    }

    /**
     * Calling {@code refresh()} forces the ListView control to recreate and
     * repopulate the cells necessary to populate the visual bounds of the control.
     * In other words, this forces the ListView to update what it is showing to
     * the user. This is useful in cases where the underlying data source has
     * changed in a way that is not observed by the ListView itself.
     *
     * @since JavaFX 8u60
     */
    public void refresh() {
        getProperties().put(Properties.RECREATE, Boolean.TRUE);
    }



    /***************************************************************************
     *                                                                         *
     * Private Implementation                                                  *
     *                                                                         *
     **************************************************************************/



    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "list-view";

    private static class StyleableProperties {
        private static final CssMetaData<ListView<?>,Orientation> ORIENTATION =
            new CssMetaData<ListView<?>,Orientation>("-fx-orientation",
                new EnumConverter<Orientation>(Orientation.class),
                Orientation.VERTICAL) {

            @Override
            public Orientation getInitialValue(ListView<?> node) {
                // A vertical ListView should remain vertical
                return node.getOrientation();
            }

            @Override
            public boolean isSettable(ListView<?> n) {
                return n.orientation == null || !n.orientation.isBound();
            }

            @SuppressWarnings("unchecked") // orientationProperty() is a StyleableProperty<Orientation>
            @Override
            public StyleableProperty<Orientation> getStyleableProperty(ListView<?> n) {
                return (StyleableProperty<Orientation>)n.orientationProperty();
            }
        };

        private static final CssMetaData<ListView<?>,Number> FIXED_CELL_SIZE =
            new CssMetaData<ListView<?>,Number>("-fx-fixed-cell-size",
                                                SizeConverter.getInstance(),
                                                Region.USE_COMPUTED_SIZE) {

                @Override public Double getInitialValue(ListView<?> node) {
                    return node.getFixedCellSize();
                }

                @Override public boolean isSettable(ListView<?> n) {
                    return n.fixedCellSize == null || !n.fixedCellSize.isBound();
                }

                @Override public StyleableProperty<Number> getStyleableProperty(ListView<?> n) {
                    return (StyleableProperty<Number>)(WritableValue<Number>)n.fixedCellSizeProperty();
                }
            };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(Control.getClassCssMetaData());
            styleables.add(ORIENTATION);
            styleables.add(FIXED_CELL_SIZE);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses.
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

    private static final PseudoClass PSEUDO_CLASS_VERTICAL =
            PseudoClass.getPseudoClass("vertical");
    private static final PseudoClass PSEUDO_CLASS_HORIZONTAL =
            PseudoClass.getPseudoClass("horizontal");



    /***************************************************************************
     *                                                                         *
     * Accessibility handling                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case MULTIPLE_SELECTION: {
                MultipleSelectionModel<T> sm = getSelectionModel();
                return sm != null && sm.getSelectionMode() == SelectionMode.MULTIPLE;
            }
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }


    /***************************************************************************
     *                                                                         *
     * Support Interfaces                                                      *
     *                                                                         *
     **************************************************************************/



    /***************************************************************************
     *                                                                         *
     * Support Classes                                                         *
     *                                                                         *
     **************************************************************************/

    /**
     * An {@link Event} subclass used specifically in ListView for representing
     * edit-related events. It provides additional API to easily access the
     * index that the edit event took place on, as well as the input provided
     * by the end user.
     *
     * @param <T> The type of the input, which is the same type as the ListView
     *      itself.
     * @since JavaFX 2.0
     */
    public static class EditEvent<T> extends Event {
        private final T newValue;
        private final int editIndex;
        private final ListView<T> source;

        private static final long serialVersionUID = 20130724L;

        /**
         * Common supertype for all edit event types.
         * @since JavaFX 8.0
         */
        public static final EventType<?> ANY = EDIT_ANY_EVENT;

        /**
         * Creates a new EditEvent instance to represent an edit event. This
         * event is used for {@link #editStartEvent()},
         * {@link #editCommitEvent()} and {@link #editCancelEvent()} types.
         * @param source the source
         * @param eventType the event type
         * @param newValue the new value
         * @param editIndex the edit index
         */
        public EditEvent(ListView<T> source,
                         EventType<? extends ListView.EditEvent<T>> eventType,
                         T newValue,
                         int editIndex) {
            super(source, Event.NULL_SOURCE_TARGET, eventType);
            this.source = source;
            this.editIndex = editIndex;
            this.newValue = newValue;
        }

        /**
         * Returns the ListView upon which the edit took place.
         */
        @Override public ListView<T> getSource() {
            return source;
        }

        /**
         * Returns the index in which the edit took place.
         * @return the index in which the edit took place
         */
        public int getIndex() {
            return editIndex;
        }

        /**
         * Returns the value of the new input provided by the end user.
         * @return the value of the new input provided by the end user
         */
        public T getNewValue() {
            return newValue;
        }

        /**
         * Returns a string representation of this {@code EditEvent} object.
         * @return a string representation of this {@code EditEvent} object.
         */
        @Override public String toString() {
            return "ListViewEditEvent [ newValue: " + getNewValue() + ", ListView: " + getSource() + " ]";
        }
    }



    // package for testing
    static class ListViewBitSetSelectionModel<T> extends MultipleSelectionModelBase<T> {

        /***********************************************************************
         *                                                                     *
         * Constructors                                                        *
         *                                                                     *
         **********************************************************************/

        public ListViewBitSetSelectionModel(final ListView<T> listView) {
            if (listView == null) {
                throw new IllegalArgumentException("ListView can not be null");
            }

            this.listView = listView;

            /*
             * The following two listeners are used in conjunction with
             * SelectionModel.select(T obj) to allow for a developer to select
             * an item that is not actually in the data model. When this occurs,
             * we actively try to find an index that matches this object, going
             * so far as to actually watch for all changes to the items list,
             * rechecking each time.
             */
            itemsObserver = new InvalidationListener() {
                private WeakReference<ObservableList<T>> weakItemsRef = new WeakReference<>(listView.getItems());

                @Override public void invalidated(Observable observable) {
                    ObservableList<T> oldItems = weakItemsRef.get();
                    weakItemsRef = new WeakReference<>(listView.getItems());
                    updateItemsObserver(oldItems, listView.getItems());
                }
            };

            this.listView.itemsProperty().addListener(new WeakInvalidationListener(itemsObserver));
            if (listView.getItems() != null) {
                this.listView.getItems().addListener(weakItemsContentObserver);
            }

            updateItemCount();

            updateDefaultSelection();
        }

        // watching for changes to the items list content
        private final ListChangeListener<T> itemsContentObserver = new ListChangeListener<T>() {
            @Override public void onChanged(Change<? extends T> c) {
                updateItemCount();

                boolean doSelectionUpdate = true;

                while (c.next()) {
                    final T selectedItem = getSelectedItem();
                    final int selectedIndex = getSelectedIndex();

                    if (listView.getItems() == null || listView.getItems().isEmpty()) {
                        selectedItemChange = c;
                        clearSelection();
                        selectedItemChange = null;
                    } else if (selectedIndex == -1 && selectedItem != null) {
                        int newIndex = listView.getItems().indexOf(selectedItem);
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
                            T newSelectedItem = getModelItem(previousRow);
                            if (! selectedItem.equals(newSelectedItem)) {
                                startAtomic();
                                clearSelection(selectedIndex);
                                stopAtomic();
                                select(newSelectedItem);
                            }
                        }
                    }
                }

                if (doSelectionUpdate) {
                    updateSelection(c);
                }
            }
        };

        // watching for changes to the items list
        private final InvalidationListener itemsObserver;

        private WeakListChangeListener<T> weakItemsContentObserver =
                new WeakListChangeListener<>(itemsContentObserver);




        /***********************************************************************
         *                                                                     *
         * Internal properties                                                 *
         *                                                                     *
         **********************************************************************/

        private final ListView<T> listView;

        private int itemCount = 0;

        private int previousModelSize = 0;

        // Listen to changes in the listview items list, such that when it
        // changes we can update the selected indices bitset to refer to the
        // new indices.
        // At present this is basically a left/right shift operation, which
        // seems to work ok.
        private void updateSelection(Change<? extends T> c) {
//            // debugging output
//            System.out.println(listView.getId());
//            if (c.wasAdded()) {
//                System.out.println("\tAdded size: " + c.getAddedSize() + ", Added sublist: " + c.getAddedSubList());
//            }
//            if (c.wasRemoved()) {
//                System.out.println("\tRemoved size: " + c.getRemovedSize() + ", Removed sublist: " + c.getRemoved());
//            }
//            if (c.wasReplaced()) {
//                System.out.println("\tWas replaced");
//            }
//            if (c.wasPermutated()) {
//                System.out.println("\tWas permutated");
//            }
            c.reset();

            List<Pair<Integer, Integer>> shifts = new ArrayList<>();
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
                    int shift = c.wasAdded() ? c.getAddedSize() : -c.getRemovedSize();
                    shifts.add(new Pair<>(c.getFrom(), shift));
                } else if (c.wasPermutated()) {

                    // General approach:
                    //   -- detected a sort has happened
                    //   -- Create a permutation lookup map (1)
                    //   -- dump all the selected indices into a list (2)
                    //   -- clear the selected items / indexes (3)
                    //   -- create a list containing the new indices (4)
                    //   -- for each previously-selected index (5)
                    //     -- if index is in the permutation lookup map
                    //       -- add the new index to the new indices list
                    //   -- Perform batch selection (6)

                    // (1)
                    int length = c.getTo() - c.getFrom();
                    HashMap<Integer, Integer> pMap = new HashMap<Integer, Integer>(length);
                    for (int i = c.getFrom(); i < c.getTo(); i++) {
                        pMap.put(i, c.getPermutation(i));
                    }

                    // (2)
                    List<Integer> selectedIndices = new ArrayList<Integer>(getSelectedIndices());


                    // (3)
                    clearSelection();

                    // (4)
                    List<Integer> newIndices = new ArrayList<Integer>(getSelectedIndices().size());

                    // (5)
                    for (int i = 0; i < selectedIndices.size(); i++) {
                        int oldIndex = selectedIndices.get(i);

                        if (pMap.containsKey(oldIndex)) {
                            Integer newIndex = pMap.get(oldIndex);
                            newIndices.add(newIndex);
                        }
                    }

                    // (6)
                    if (!newIndices.isEmpty()) {
                        if (newIndices.size() == 1) {
                            select(newIndices.get(0));
                        } else {
                            int[] ints = new int[newIndices.size() - 1];
                            for (int i = 0; i < newIndices.size() - 1; i++) {
                                ints[i] = newIndices.get(i + 1);
                            }
                            selectIndices(newIndices.get(0), ints);
                        }
                    }
                }
            }

            if (!shifts.isEmpty()) {
                shiftSelection(shifts, null);
            }

            previousModelSize = getItemCount();
        }



        /***********************************************************************
         *                                                                     *
         * Public selection API                                                *
         *                                                                     *
         **********************************************************************/

        /** {@inheritDoc} */
        @Override public void selectAll() {
            // when a selectAll happens, the anchor should not change, so we store it
            // before, and restore it afterwards
            final int anchor = ListCellBehavior.getAnchor(listView, -1);
            super.selectAll();
            ListCellBehavior.setAnchor(listView, anchor, false);
        }

        /** {@inheritDoc} */
        @Override public void clearAndSelect(int row) {
            ListCellBehavior.setAnchor(listView, row, false);
            super.clearAndSelect(row);
        }

        /** {@inheritDoc} */
        @Override protected void focus(int row) {
            if (listView.getFocusModel() == null) return;
            listView.getFocusModel().focus(row);

            listView.notifyAccessibleAttributeChanged(AccessibleAttribute.FOCUS_ITEM);
        }

        /** {@inheritDoc} */
        @Override protected int getFocusedIndex() {
            if (listView.getFocusModel() == null) return -1;
            return listView.getFocusModel().getFocusedIndex();
        }

        @Override protected int getItemCount() {
            return itemCount;
        }

        @Override protected T getModelItem(int index) {
            List<T> items = listView.getItems();
            if (items == null) return null;
            if (index < 0 || index >= itemCount) return null;

            return items.get(index);
        }



        /***********************************************************************
         *                                                                     *
         * Private implementation                                              *
         *                                                                     *
         **********************************************************************/

        private void updateItemCount() {
            if (listView == null) {
                itemCount = -1;
            } else {
                List<T> items = listView.getItems();
                itemCount = items == null ? -1 : items.size();
            }
        }

        private void updateItemsObserver(ObservableList<T> oldList, ObservableList<T> newList) {
            // update listeners
            if (oldList != null) {
                oldList.removeListener(weakItemsContentObserver);
            }
            if (newList != null) {
                newList.addListener(weakItemsContentObserver);
            }

            updateItemCount();
            updateDefaultSelection();
        }

        private void updateDefaultSelection() {
            // when the items list totally changes, we should clear out
            // the selection and focus
            int newSelectionIndex = -1;
            int newFocusIndex = -1;
            if (listView.getItems() != null) {
                T selectedItem = getSelectedItem();
                if (selectedItem != null) {
                    newSelectionIndex = listView.getItems().indexOf(selectedItem);
                    newFocusIndex = newSelectionIndex;
                }

                // we put focus onto the first item, if there is at least
                // one item in the list
                if (listView.selectFirstRowByDefault && newFocusIndex == -1) {
                    newFocusIndex = listView.getItems().size() > 0 ? 0 : -1;
                }
            }

            clearSelection();
            select(newSelectionIndex);
//            focus(newFocusIndex);
        }
    }



    // package for testing
    static class ListViewFocusModel<T> extends FocusModel<T> {

        private final ListView<T> listView;
        private int itemCount = 0;

        public ListViewFocusModel(final ListView<T> listView) {
            if (listView == null) {
                throw new IllegalArgumentException("ListView can not be null");
            }

            this.listView = listView;

            itemsObserver = new InvalidationListener() {
                private WeakReference<ObservableList<T>> weakItemsRef = new WeakReference<>(listView.getItems());

                @Override public void invalidated(Observable observable) {
                    ObservableList<T> oldItems = weakItemsRef.get();
                    weakItemsRef = new WeakReference<>(listView.getItems());
                    updateItemsObserver(oldItems, listView.getItems());
                }
            };
            this.listView.itemsProperty().addListener(new WeakInvalidationListener(itemsObserver));
            if (listView.getItems() != null) {
                this.listView.getItems().addListener(weakItemsContentListener);
            }

            updateItemCount();
            updateDefaultFocus();

            focusedIndexProperty().addListener(o -> {
                listView.notifyAccessibleAttributeChanged(AccessibleAttribute.FOCUS_ITEM);
            });
        }


        private void updateItemsObserver(ObservableList<T> oldList, ObservableList<T> newList) {
            // the listview items list has changed, we need to observe
            // the new list, and remove any observer we had from the old list
            if (oldList != null) oldList.removeListener(weakItemsContentListener);
            if (newList != null) newList.addListener(weakItemsContentListener);

            updateItemCount();
            updateDefaultFocus();
        }

        private final InvalidationListener itemsObserver;

        // Listen to changes in the listview items list, such that when it
        // changes we can update the focused index to refer to the new indices.
        private final ListChangeListener<T> itemsContentListener = c -> {
            updateItemCount();

            while (c.next()) {
                // looking at the first change
                int from = c.getFrom();

                if (c.wasReplaced() || c.getAddedSize() == getItemCount()) {
                    updateDefaultFocus();
                    return;
                }

                if (getFocusedIndex() == -1 || from > getFocusedIndex()) {
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

                if (added && !removed) {
                    focus(Math.min(getItemCount() - 1, getFocusedIndex() + addedSize));
                } else if (!added && removed) {
                    focus(Math.max(0, getFocusedIndex() - removedSize));
                }
            }
        };

        private WeakListChangeListener<T> weakItemsContentListener
                = new WeakListChangeListener<T>(itemsContentListener);

        @Override protected int getItemCount() {
            return itemCount;
        }

        @Override protected T getModelItem(int index) {
            if (isEmpty()) return null;
            if (index < 0 || index >= itemCount) return null;

            return listView.getItems().get(index);
        }

        private boolean isEmpty() {
            return itemCount == -1;
        }

        private void updateItemCount() {
            if (listView == null) {
                itemCount = -1;
            } else {
                List<T> items = listView.getItems();
                itemCount = items == null ? -1 : items.size();
            }
        }

        private void updateDefaultFocus() {
            // when the items list totally changes, we should clear out
            // the focus
            int newValueIndex = -1;
            if (listView.getItems() != null) {
                T focusedItem = getFocusedItem();
                if (focusedItem != null) {
                    newValueIndex = listView.getItems().indexOf(focusedItem);
                }

                // we put focus onto the first item, if there is at least
                // one item in the list
                if (newValueIndex == -1) {
                    newValueIndex = listView.getItems().size() > 0 ? 0 : -1;
                }
            }

            focus(newValueIndex);
        }
    }
}
