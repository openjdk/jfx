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

package javafx.scene.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Orientation;
import javafx.util.Callback;

import com.sun.javafx.css.StyleManager;
import com.sun.javafx.css.StyleableObjectProperty;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.converters.EnumConverter;
import com.sun.javafx.event.EventTypeUtil;
import com.sun.javafx.scene.control.WeakListChangeListener;
import com.sun.javafx.scene.control.skin.ListViewSkin;
import com.sun.javafx.scene.control.skin.VirtualContainerBase;
import java.lang.ref.WeakReference;
import javafx.beans.DefaultProperty;

/**
 * A ListView displays a horizontal or vertical list of items from which the
 * user may select, or with which the user may interact. A ListView is able to
 * have its generic type set to represent the type of data in the backing model.
 * Doing this has the benefit of making various methods in the ListView, as well
 * as the supporting classes (mentioned below), type-safe. In addition, making 
 * use of the generic supports substantially simplifies development of applications
 * making use of ListView, as all modern IDEs are able to auto-complete far
 * more successfully with the additional type information.
 * 
 * <h3>Populating a ListView</h3>
 * <p>A simple example of how to create and populate a ListView of names (Strings)
 * is shown here:
 * 
 * <pre>
 * {@code
 * ObservableList<String> names = FXCollections.observableArrayList(
 *          "Julia", "Ian", "Sue", "Matthew", "Hannah", "Stephan", "Denise");
 * ListView<String> listView = new ListView<String>(names);}</pre>
 *
 * <p>The elements of the ListView are contained within the 
 * {@link #itemsProperty() items} {@link ObservableList}. This
 * ObservableList is automatically observed by the ListView, such that any 
 * changes that occur inside the ObservableList will be automatically shown in
 * the ListView itself. If passying the <code>ObservableList</code> in to the
 * ListView constructor is not feasible, the recommended approach for setting 
 * the items is to simply call:
 * 
 * <pre>
 * {@code
 * ObservableList<T> content = ...
 * listView.setItems(content);}</pre>
 * 
 * The end result of this is, as noted above, that the ListView will automatically
 * refresh the view to represent the items in the list.
 * 
 * <p>Another approach, whilst accepted by the ListView, <b>is not the 
 * recommended approach</b>:
 * 
 * <pre>
 * {@code
 * List<T> content = ...
 * getItems().setAll(content);}</pre>
 * 
 * The issue with the approach shown above is that the content list is being
 * copied into the items list - meaning that subsequent changes to the content
 * list are not observed, and will not be reflected visually within the ListView.
 * 
 * <h3>ListView Selection / Focus APIs</h3>
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
 * <pre>
 * {@code 
 * listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);}</pre>
 *
 * <h3>Customizing ListView Visuals</h3>
 * <p>The visuals of the ListView can be entirely customized by replacing the 
 * default {@link #cellFactoryProperty() cell factory}. A cell factory is used to
 * generate {@link ListCell} instances, which are used to represent an item in the
 * ListView. See the {@link Cell} class documentation for a more complete
 * description of how to write custom Cells.
 * 
 * @see ListCell
 * @see MultipleSelectionModel
 * @see FocusModel
 * @param <T> This type is used to represent the type of the objects stored in 
 *          the ListViews {@link #itemsProperty() items} ObservableList. It is
 *          also used in the {@link #selectionModelProperty() selection model}
 *          and {@link #focusModelProperty() focus model}.
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
     * type of all other edit events: {@link #EDIT_START_EVENT},
     *  {@link #EDIT_COMMIT_EVENT} and {@link #EDIT_CANCEL_EVENT}.
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<EditEvent<T>> editAnyEvent() {
        return (EventType<EditEvent<T>>) EDIT_ANY_EVENT;
    }
    private static final EventType<?> EDIT_ANY_EVENT =
            EventTypeUtil.registerInternalEventType(Event.ANY, "LIST_VIEW_EDIT");
    
    /**
     * An EventType used to indicate that an edit event has started within the
     * ListView upon which the event was fired.
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<EditEvent<T>> editStartEvent() {
        return (EventType<EditEvent<T>>) EDIT_START_EVENT;
    }
    private static final EventType<?> EDIT_START_EVENT =
            EventTypeUtil.registerInternalEventType(editAnyEvent(), "EDIT_START");

    /**
     * An EventType used to indicate that an edit event has just been canceled
     * within the ListView upon which the event was fired.
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<EditEvent<T>> editCancelEvent() {
        return (EventType<EditEvent<T>>) EDIT_CANCEL_EVENT;
    }
    private static final EventType<?> EDIT_CANCEL_EVENT =
            EventTypeUtil.registerInternalEventType(editAnyEvent(), "EDIT_CANCEL");

    /**
     * An EventType used to indicate that an edit event has been committed
     * within the ListView upon which the event was fired.
     */
    @SuppressWarnings("unchecked")
    public static <T> EventType<EditEvent<T>> editCommitEvent() {
        return (EventType<EditEvent<T>>) EDIT_COMMIT_EVENT;
    }
    private static final EventType<?> EDIT_COMMIT_EVENT =
            EventTypeUtil.registerInternalEventType(editAnyEvent(), "EDIT_COMMIT");
    
    

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
     */
    public ListView(ObservableList<T> items) {
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);

        setItems(items);

        // Install default....
        // ...selection model
        setSelectionModel(new ListViewBitSetSelectionModel<T>(this));

        // ...focus model
        setFocusModel(new ListViewFocusModel<T>(this));

        // ...edit commit handler
        setOnEditCommit(DEFAULT_EDIT_COMMIT_HANDLER);
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Callbacks and Events                                                    *
     *                                                                         *
     **************************************************************************/
    
    private EventHandler<EditEvent<T>> DEFAULT_EDIT_COMMIT_HANDLER = new EventHandler<EditEvent<T>>() {
        @Override public void handle(EditEvent<T> t) {
            int index = t.getIndex();
            List<T> list = getItems();
            if (index < 0 || index >= list.size()) return;
            list.set(index, t.getNewValue());
        }
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
     */
    public final ObjectProperty<ObservableList<T>> itemsProperty() {
        if (items == null) {
            items = new SimpleObjectProperty<ObservableList<T>>(this, "items") {
                WeakReference<ObservableList<T>> oldItemsRef;
                
                @Override protected void invalidated() {
                    ObservableList<T> oldItems = oldItemsRef == null ? null : oldItemsRef.get();
                    
                    // FIXME temporary fix for RT-15793. This will need to be
                    // properly fixed when time permits
                    if (getSelectionModel() instanceof ListViewBitSetSelectionModel) {
                        ((ListViewBitSetSelectionModel)getSelectionModel()).updateItemsObserver(oldItems, getItems());
                    }
                    if (getFocusModel() instanceof ListViewFocusModel) {
                        ((ListViewFocusModel)getFocusModel()).updateItemsObserver(oldItems, getItems());
                    }
                    if (getSkin() instanceof ListViewSkin) {
                        ListViewSkin skin = (ListViewSkin) getSkin();
                        skin.updateListViewItems();
                    }
                    
                    oldItemsRef = new WeakReference<ObservableList<T>>(getItems());
                }
            };
        }
        return items;
    }
    
    
    // --- Selection Model
    private ObjectProperty<MultipleSelectionModel<T>> selectionModel = new SimpleObjectProperty<MultipleSelectionModel<T>>(this, "selectionModel");
    
    /**
     * Sets the {@link MultipleSelectionModel} to be used in the ListView. 
     * Despite a ListView requiring a <b>Multiple</b>SelectionModel, it is possible
     * to configure it to only allow single selection (see 
     * {@link MultipleSelectionModel#setSelectionMode(javafx.scene.control.SelectionMode)}
     * for more information).
     */
    public final void setSelectionModel(MultipleSelectionModel<T> value) {
        selectionModelProperty().set(value);
    }

    /**
     * Returns the currently installed selection model.
     */
    public final MultipleSelectionModel<T> getSelectionModel() {
        return selectionModel == null ? null : selectionModel.get();
    }

    /**
     * The SelectionModel provides the API through which it is possible
     * to select single or multiple items within a ListView, as  well as inspect
     * which items have been selected by the user. Note that it has a generic
     * type that must match the type of the ListView itself.
     */
    public final ObjectProperty<MultipleSelectionModel<T>> selectionModelProperty() {
        return selectionModel;
    }
    
    
    // --- Focus Model
    private ObjectProperty<FocusModel<T>> focusModel;
    
    /**
     * Sets the {@link FocusModel} to be used in the ListView. 
     */
    public final void setFocusModel(FocusModel<T> value) {
        focusModelProperty().set(value);
    }

    /**
     * Returns the currently installed {@link FocusModel}.
     */
    public final FocusModel<T> getFocusModel() {
        return focusModel == null ? null : focusModel.get();
    }

    /**
     * The FocusModel provides the API through which it is possible
     * to both get and set the focus on a single item within a ListView. Note 
     * that it has a generic type that must match the type of the ListView itself.
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
     */
    public final void setOrientation(Orientation value) {
        orientationProperty().set(value);
    };
    
    /**
     * Returns the current orientation of the ListView, which dictates whether
     * it scrolls vertically or horizontally.
     */
    public final Orientation getOrientation() {
        return orientation == null ? Orientation.VERTICAL : orientation.get();
    }
    
    /**
     * The orientation of the {@code ListView} - this can either be horizontal
     * or vertical.
     */
    public final ObjectProperty<Orientation> orientationProperty() {
        if (orientation == null) {
            orientation = new StyleableObjectProperty<Orientation>(Orientation.VERTICAL) {
                @Override public void invalidated() {
                    impl_pseudoClassStateChanged(PSEUDO_CLASS_VERTICAL);
                    impl_pseudoClassStateChanged(PSEUDO_CLASS_HORIZONTAL);
                }
                
                @Override 
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.ORIENTATION;
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
     */
    public final void setCellFactory(Callback<ListView<T>, ListCell<T>> value) {
        cellFactoryProperty().set(value);
    }

    /**
     * Returns the current cell factory.
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
     */
    public final ObjectProperty<Callback<ListView<T>, ListCell<T>>> cellFactoryProperty() {
        if (cellFactory == null) {
            cellFactory = new SimpleObjectProperty<Callback<ListView<T>, ListCell<T>>>(this, "cellFactory");
        }
        return cellFactory;
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
    private ObjectProperty<EventHandler<EditEvent<T>>> onEditStart;

    /**
     * Sets the {@link EventHandler} that will be called when the user begins
     * an edit. 
     * 
     * <p>This is a convenience method - the same result can be 
     * achieved by calling 
     * <code>addEventHandler(ListView.EDIT_START_EVENT, eventHandler)</code>.
     */
    public final void setOnEditStart(EventHandler<EditEvent<T>> value) {
        onEditStartProperty().set(value);
    }

    /**
     * Returns the {@link EventHandler} that will be called when the user begins
     * an edit.
     */
    public final EventHandler<EditEvent<T>> getOnEditStart() {
        return onEditStart == null ? null : onEditStart.get();
    }

    /**
     * This event handler will be fired when the user successfully initiates
     * editing.
     */
    public final ObjectProperty<EventHandler<EditEvent<T>>> onEditStartProperty() {
        if (onEditStart == null) {
            onEditStart = new ObjectPropertyBase<EventHandler<EditEvent<T>>>() {
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
    private ObjectProperty<EventHandler<EditEvent<T>>> onEditCommit;

    /**
     * Sets the {@link EventHandler} that will be called when the user has
     * completed their editing. This is called as part of the 
     * {@link ListCell#commitEdit(java.lang.Object)} method.
     * 
     * <p>This is a convenience method - the same result can be 
     * achieved by calling 
     * <code>addEventHandler(ListView.EDIT_START_EVENT, eventHandler)</code>.
     */
    public final void setOnEditCommit(EventHandler<EditEvent<T>> value) {
        onEditCommitProperty().set(value);
    }

    /**
     * Returns the {@link EventHandler} that will be called when the user commits
     * an edit.
     */
    public final EventHandler<EditEvent<T>> getOnEditCommit() {
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
     */
    public final ObjectProperty<EventHandler<EditEvent<T>>> onEditCommitProperty() {
        if (onEditCommit == null) {
            onEditCommit = new ObjectPropertyBase<EventHandler<EditEvent<T>>>() {
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
    private ObjectProperty<EventHandler<EditEvent<T>>> onEditCancel;

    /**
     * Sets the {@link EventHandler} that will be called when the user cancels
     * an edit.
     */
    public final void setOnEditCancel(EventHandler<EditEvent<T>> value) {
        onEditCancelProperty().set(value);
    }

    /**
     * Returns the {@link EventHandler} that will be called when the user cancels
     * an edit.
     */
    public final EventHandler<EditEvent<T>> getOnEditCancel() {
        return onEditCancel == null ? null : onEditCancel.get();
    }

    /**
     * This event handler will be fired when the user cancels editing a cell.
     */
    public final ObjectProperty<EventHandler<EditEvent<T>>> onEditCancelProperty() {
        if (onEditCancel == null) {
            onEditCancel = new ObjectPropertyBase<EventHandler<EditEvent<T>>>() {
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
       getProperties().put(VirtualContainerBase.SCROLL_TO_INDEX_CENTERED, index);
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

    private static final String PSEUDO_CLASS_VERTICAL = "vertical";
    private static final String PSEUDO_CLASS_HORIZONTAL = "horizontal";

    /** @treatAsPrivate */
    private static class StyleableProperties {
        private static final StyleableProperty<ListView,Orientation> ORIENTATION = 
            new StyleableProperty<ListView,Orientation>("-fx-orientation",
                new EnumConverter<Orientation>(Orientation.class), 
                Orientation.VERTICAL) {

            @Override
            public Orientation getInitialValue(ListView node) {
                // A vertical ListView should remain vertical 
                return node.getOrientation();
            }

            @Override
            public boolean isSettable(ListView n) {
                return n.orientation == null || !n.orientation.isBound();
            }

            @Override
            public WritableValue<Orientation> getWritableValue(ListView n) {
                return n.orientationProperty();
            }
        };
            
        private static final List<StyleableProperty> STYLEABLES;
        static {
            final List<StyleableProperty> styleables =
                new ArrayList<StyleableProperty>(Control.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                ORIENTATION
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
        return ListView.StyleableProperties.STYLEABLES;
    }

    /**
     * RT-19263
     * @treatAsPrivate implementation detail
     * @deprecated This is an experimental API that is not intended for general use and is subject to change in future versions
     */
    @Deprecated
    protected List<StyleableProperty> impl_getControlStyleableProperties() {
        return impl_CSS_STYLEABLES();
    }

    private static final long VERTICAL_PSEUDOCLASS_STATE =
            StyleManager.getInstance().getPseudoclassMask("vertical");
    private static final long HORIZONTAL_PSEUDOCLASS_STATE =
            StyleManager.getInstance().getPseudoclassMask("horizontal");

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated @Override public long impl_getPseudoClassState() {
        long mask = super.impl_getPseudoClassState();
        mask |= (getOrientation() == Orientation.VERTICAL) ?
            VERTICAL_PSEUDOCLASS_STATE : HORIZONTAL_PSEUDOCLASS_STATE;
        return mask;
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
     */
    public static class EditEvent<T> extends Event {
        private final T newValue;
        private final int editIndex;

        /**
         * Creates a new EditEvent instance to represent an edit event. This 
         * event is used for {@link #EDIT_START_EVENT}, 
         * {@link #EDIT_COMMIT_EVENT} and {@link #EDIT_CANCEL_EVENT} types.
         */
        public EditEvent(ListView<T> source,
                         EventType<? extends EditEvent<T>> eventType,
                         T newValue,
                         int editIndex) {
            super(source, Event.NULL_SOURCE_TARGET, eventType);
            this.editIndex = editIndex;
            this.newValue = newValue;
        }

        /**
         * Returns the ListView upon which the edit took place.
         */
        @Override public ListView<T> getSource() {
            return (ListView<T>) super.getSource();
        }

        /**
         * Returns the index in which the edit took place. 
         */
        public int getIndex() {
            return editIndex;
        }

        /**
         * Returns the value of the new input provided by the end user.
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

            this.listView.itemsProperty().addListener(weakItemsObserver);
            if (listView.getItems() != null) {
                this.listView.getItems().addListener(weakItemsContentObserver);
//                updateItemsObserver(null, this.listView.getItems());
            }
        }
        
        // watching for changes to the items list content
        private final ListChangeListener<T> itemsContentObserver = new ListChangeListener<T>() {
            @Override public void onChanged(Change<? extends T> c) {
                while (c.next()) {
                    if (listView.getItems() == null || listView.getItems().isEmpty()) {
                        setSelectedIndex(-1);
                        focus(-1);
                    } else if (getSelectedIndex() == -1 && getSelectedItem() != null) {
                        int newIndex = listView.getItems().indexOf(getSelectedItem());
                        if (newIndex != -1) {
                            setSelectedIndex(newIndex);
                        }
                    }

                    updateSelection(c);
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
            focus(-1);
        }



        /***********************************************************************
         *                                                                     *
         * Internal properties                                                 *
         *                                                                     *
         **********************************************************************/

        private final ListView<T> listView;
        
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
                            makeAtomic = true;
                            clearSelection(index);
                            makeAtomic = false;
                            select(index);
                        } else {
                            // Fix for RT-22079
                            clearSelection();
                        }
                    }
                } else if (c.wasAdded() || c.wasRemoved()) {
                    int shift = c.wasAdded() ? c.getAddedSize() : -c.getRemovedSize();
                    shiftSelection(c.getFrom(), shift);
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
            
            previousModelSize = getItemCount();
        }



        /***********************************************************************
         *                                                                     *
         * Public selection API                                                *
         *                                                                     *
         **********************************************************************/

        /** {@inheritDoc} */
        @Override protected void focus(int row) {
            if (listView.getFocusModel() == null) return;
            listView.getFocusModel().focus(row);
        }

        /** {@inheritDoc} */
        @Override protected int getFocusedIndex() {
            if (listView.getFocusModel() == null) return -1;
            return listView.getFocusModel().getFocusedIndex();
        }

        /** {@inheritDoc} */
        @Override protected int getItemCount() {
            return listView.getItems() == null ? -1 : listView.getItems().size();
        }

        /** {@inheritDoc} */
        @Override public T getModelItem(int index) {
            if (listView.getItems() == null) return null;

            if (index < 0 || index >= getItemCount()) return null;

            return listView.getItems().get((int) index);
        }
    }



    // package for testing
    static class ListViewFocusModel<T> extends FocusModel<T> {

        private final ListView<T> listView;

        public ListViewFocusModel(final ListView<T> listView) {
            if (listView == null) {
                throw new IllegalArgumentException("ListView can not be null");
            }

            this.listView = listView;
            this.listView.itemsProperty().addListener(weakItemsListener);
            if (listView.getItems() != null) {
                this.listView.getItems().addListener(weakItemsContentListener);
            }
        }

        private ChangeListener<ObservableList<T>> itemsListener = new ChangeListener<ObservableList<T>>() {
            @Override
            public void changed(ObservableValue<? extends ObservableList<T>> observable, 
                ObservableList<T> oldList, ObservableList<T> newList) {
                    updateItemsObserver(oldList, newList);
            }
        };
        
        private WeakChangeListener<ObservableList<T>> weakItemsListener = 
                new WeakChangeListener<ObservableList<T>>(itemsListener);
        
        private void updateItemsObserver(ObservableList<T> oldList, ObservableList<T> newList) {
            // the listview items list has changed, we need to observe
            // the new list, and remove any observer we had from the old list
            if (oldList != null) oldList.removeListener(weakItemsContentListener);
            if (newList != null) newList.addListener(weakItemsContentListener);
        }
        
        // Listen to changes in the listview items list, such that when it
        // changes we can update the focused index to refer to the new indices.
        private final ListChangeListener<T> itemsContentListener = new ListChangeListener<T>() {
            @Override public void onChanged(Change<? extends T> c) {
                c.next();
                // looking at the first change
                int from = c.getFrom();
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
                    focus(getFocusedIndex() + addedSize);
                } else if (!added && removed) {
                    focus(getFocusedIndex() - removedSize);
                }
            }
        };
        
        private WeakListChangeListener<T> weakItemsContentListener 
                = new WeakListChangeListener<T>(itemsContentListener);
        
        @Override protected int getItemCount() {
            return isEmpty() ? -1 : listView.getItems().size();
        }

        @Override protected T getModelItem(int index) {
            if (isEmpty()) return null;

            if (index < 0 || index >= getItemCount()) return null;

            return listView.getItems().get(index);
        }

        private boolean isEmpty() {
            return listView == null || listView.getItems() == null;
        }
    }
}
