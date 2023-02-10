/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import javafx.collections.MapChangeListener;
import javafx.css.PseudoClass;
import javafx.beans.property.*;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;

/**
 * Abstract base class for ComboBox-like controls. A ComboBox typically has
 * a button that, when clicked, will pop up some means of allowing a user
 * to select one or more values (depending on the implementation). This base
 * class makes no assumptions about what happens when the {@link #show()} and
 * {@link #hide()} methods are called, however commonly this results in either
 * a popup or dialog appearing that allows for the user to provide the
 * required information.
 *
 * <p>A ComboBox has a {@link #valueProperty() value} property that represents
 * the current user input. This may be based on a selection from a drop-down list,
 * or it may be from user input when the ComboBox is
 * {@link #editableProperty() editable}.
 *
 * <p>An {@link #editableProperty() editable} ComboBox is one which provides some
 * means for an end-user to provide input for values that are not otherwise
 * options available to them. For example, in the {@link ComboBox} implementation,
 * an editable ComboBox provides a {@link TextField} that may be typed into.
 * As mentioned above, when the user commits textual input into the textfield
 * (commonly by pressing the Enter keyboard key), the
 * {@link #valueProperty() value} property will be updated.
 *
 * <p>The purpose of the separation between this class and, say, {@link ComboBox}
 * is to allow for ComboBox-like controls that do not necessarily pop up a list
 * of items. Examples of other implementations include color pickers, calendar
 * pickers, etc. The  {@link ComboBox} class provides the default, and most commonly
 * expected implementation. Refer to that classes javadoc for more information.
 *
 * @see ComboBox
 * @param <T> The type of the value that has been selected or otherwise
 *      entered in to this ComboBox.
 * @since JavaFX 2.1
 */
public abstract class ComboBoxBase<T> extends Control {


    /* *************************************************************************
     *                                                                         *
     * Static properties and methods                                           *
     *                                                                         *
     **************************************************************************/

    /**
     * <p>Called prior to the ComboBox showing its popup/display after the user
     * has clicked or otherwise interacted with the ComboBox.
     * @since JavaFX 2.2
     */
    public static final EventType<Event> ON_SHOWING =
            new EventType<>(Event.ANY, "COMBO_BOX_BASE_ON_SHOWING");

    /**
     * <p>Called after the ComboBox has shown its popup/display.
     * @since JavaFX 2.2
     */
    public static final EventType<Event> ON_SHOWN =
            new EventType<>(Event.ANY, "COMBO_BOX_BASE_ON_SHOWN");

    /**
     * <p>Called when the ComboBox popup/display <b>will</b> be hidden.
     * @since JavaFX 2.2
     */
    public static final EventType<Event> ON_HIDING =
            new EventType<>(Event.ANY, "COMBO_BOX_BASE_ON_HIDING");

    /**
     * <p>Called when the ComboBox popup/display has been hidden.
     * @since JavaFX 2.2
     */
    public static final EventType<Event> ON_HIDDEN =
            new EventType<>(Event.ANY, "COMBO_BOX_BASE_ON_HIDDEN");



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default ComboBoxBase instance.
     */
    public ComboBoxBase() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);

        // Fix for RT-29885
        getProperties().addListener((MapChangeListener<Object, Object>) change -> {
            if (change.wasAdded()) {
                if (change.getKey() == "FOCUSED") {
                    setFocused((Boolean)change.getValueAdded());
                    getProperties().remove("FOCUSED");
                }
            }
        });
        // End of fix for RT-29885
    }

    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- value
    /**
     * The value of this ComboBox is defined as the selected item if the input
     * is not editable, or if it is editable, the most recent user action:
     * either the value input by the user, or the last selected item.
     * @return the value property
     */
    public ObjectProperty<T> valueProperty() { return value; }
    private ObjectProperty<T> value = new SimpleObjectProperty<>(this, "value");

    public final void setValue(T value) { valueProperty().set(value); }
    public final T getValue() { return valueProperty().get(); }


    // --- editable
    /**
     * Specifies whether the ComboBox allows for user input. When editable is
     * true, the ComboBox has a text input area that a user may type in to. This
     * input is then available via the {@link #valueProperty() value} property.
     *
     * <p>Note that when the editable property changes, the value property is
     * reset, along with any other relevant state.
     * @return the editable property
     */
    public BooleanProperty editableProperty() { return editable; }
    public final void setEditable(boolean value) { editableProperty().set(value); }
    public final boolean isEditable() { return editableProperty().get(); }
    private BooleanProperty editable = new SimpleBooleanProperty(this, "editable", false) {
        @Override protected void invalidated() {
            pseudoClassStateChanged(PSEUDO_CLASS_EDITABLE, get());
        }
    };


    // --- showing
    /**
     * Represents the current state of the ComboBox popup, and whether it is
     * currently visible on screen (although it may be hidden behind other windows).
     */
    private ReadOnlyBooleanWrapper showing;
    public ReadOnlyBooleanProperty showingProperty() { return showingPropertyImpl().getReadOnlyProperty(); }
    public final boolean isShowing() { return showingPropertyImpl().get(); }
    private void setShowing(boolean value) {
        // these events will not fire if the showing property is bound
        Event.fireEvent(this, value ? new Event(ComboBoxBase.ON_SHOWING) :
            new Event(ComboBoxBase.ON_HIDING));
        showingPropertyImpl().set(value);
        Event.fireEvent(this, value ? new Event(ComboBoxBase.ON_SHOWN) :
            new Event(ComboBoxBase.ON_HIDDEN));
    }
    private ReadOnlyBooleanWrapper showingPropertyImpl() {
        if (showing == null) {
            showing = new ReadOnlyBooleanWrapper(false) {
                @Override protected void invalidated() {
                    pseudoClassStateChanged(PSEUDO_CLASS_SHOWING, get());
                    notifyAccessibleAttributeChanged(AccessibleAttribute.EXPANDED);
                }

                @Override
                public Object getBean() {
                    return ComboBoxBase.this;
                }

                @Override
                public String getName() {
                    return "showing";
                }
            };
        }
        return showing;
    }


    // --- prompt text
    /**
     * The {@code ComboBox} prompt text to display, or {@code null} if no
     * prompt text is displayed. Prompt text is not displayed in all circumstances,
     * it is dependent upon the subclasses of ComboBoxBase to clarify when
     * promptText will be shown. For example, in most cases prompt text will never be
     * shown when a combo box is non-editable (that is, prompt text is only shown
     * when user input is allowed via text input).
     */
    private StringProperty promptText = new SimpleStringProperty(this, "promptText", null) {
        @Override protected void invalidated() {
            // Strip out newlines
            String txt = get();
            if (txt != null && txt.contains("\n")) {
                txt = txt.replace("\n", "");
                set(txt);
            }
        }
    };
    public final StringProperty promptTextProperty() { return promptText; }
    public final String getPromptText() { return promptText.get(); }
    public final void setPromptText(String value) { promptText.set(value); }


    // --- armed
    /**
     * Indicates that the ComboBox has been "armed" such that a mouse release
     * will cause the ComboBox {@link #show()} method to be invoked. This is
     * subtly different from pressed. Pressed indicates that the mouse has been
     * pressed on a Node and has not yet been released. {@code arm} however
     * also takes into account whether the mouse is actually over the
     * ComboBox and pressed.
     * @return the armed property
     */
    public BooleanProperty armedProperty() { return armed; }
    private final void setArmed(boolean value) { armedProperty().set(value); }
    public final boolean isArmed() { return armedProperty().get(); }
    private BooleanProperty armed = new SimpleBooleanProperty(this, "armed", false) {
        @Override protected void invalidated() {
            pseudoClassStateChanged(PSEUDO_CLASS_ARMED, get());
        }
    };


    // --- On Action
    /**
     * The ComboBox action, which is invoked whenever the ComboBox
     * {@link #valueProperty() value} property is changed. This
     * may be due to the value property being programmatically changed, when the
     * user selects an item in a popup list or dialog, or, in the case of
     * {@link #editableProperty() editable} ComboBoxes, it may be when the user
     * provides their own input (be that via a {@link TextField} or some other
     * input mechanism.
     * @return the on action property
     */
    public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() { return onAction; }
    public final void setOnAction(EventHandler<ActionEvent> value) { onActionProperty().set(value); }
    public final EventHandler<ActionEvent> getOnAction() { return onActionProperty().get(); }
    private ObjectProperty<EventHandler<ActionEvent>> onAction = new ObjectPropertyBase<>() {
        @Override protected void invalidated() {
            setEventHandler(ActionEvent.ACTION, get());
        }

        @Override
        public Object getBean() {
            return ComboBoxBase.this;
        }

        @Override
        public String getName() {
            return "onAction";
        }
    };


    // --- On Showing
    public final ObjectProperty<EventHandler<Event>> onShowingProperty() { return onShowing; }
    public final void setOnShowing(EventHandler<Event> value) { onShowingProperty().set(value); }
    public final EventHandler<Event> getOnShowing() { return onShowingProperty().get(); }
    /**
     * Called just prior to the {@code ComboBoxBase} popup/display being shown.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<Event>> onShowing = new ObjectPropertyBase<>() {
        @Override protected void invalidated() {
            setEventHandler(ON_SHOWING, get());
        }

        @Override public Object getBean() {
            return ComboBoxBase.this;
        }

        @Override public String getName() {
            return "onShowing";
        }
    };


    // -- On Shown
    public final ObjectProperty<EventHandler<Event>> onShownProperty() { return onShown; }
    public final void setOnShown(EventHandler<Event> value) { onShownProperty().set(value); }
    public final EventHandler<Event> getOnShown() { return onShownProperty().get(); }
    /**
     * Called just after the {@link ComboBoxBase} popup/display is shown.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<Event>> onShown = new ObjectPropertyBase<>() {
        @Override protected void invalidated() {
            setEventHandler(ON_SHOWN, get());
        }

        @Override public Object getBean() {
            return ComboBoxBase.this;
        }

        @Override public String getName() {
            return "onShown";
        }
    };


    // --- On Hiding
    public final ObjectProperty<EventHandler<Event>> onHidingProperty() { return onHiding; }
    public final void setOnHiding(EventHandler<Event> value) { onHidingProperty().set(value); }
    public final EventHandler<Event> getOnHiding() { return onHidingProperty().get(); }
    /**
     * Called just prior to the {@link ComboBox} popup/display being hidden.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<Event>> onHiding = new ObjectPropertyBase<>() {
        @Override protected void invalidated() {
            setEventHandler(ON_HIDING, get());
        }

        @Override public Object getBean() {
            return ComboBoxBase.this;
        }

        @Override public String getName() {
            return "onHiding";
        }
    };


    // --- On Hidden
    public final ObjectProperty<EventHandler<Event>> onHiddenProperty() { return onHidden; }
    public final void setOnHidden(EventHandler<Event> value) { onHiddenProperty().set(value); }
    public final EventHandler<Event> getOnHidden() { return onHiddenProperty().get(); }
    /**
     * Called just after the {@link ComboBoxBase} popup/display has been hidden.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<Event>> onHidden = new ObjectPropertyBase<>() {
        @Override protected void invalidated() {
            setEventHandler(ON_HIDDEN, get());
        }

        @Override public Object getBean() {
            return ComboBoxBase.this;
        }

        @Override public String getName() {
            return "onHidden";
        }
    };


    /* *************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /**
     * Requests that the ComboBox display the popup aspect of the user interface.
     * As mentioned in the {@link ComboBoxBase} class javadoc, what is actually
     * shown when this method is called is undefined, but commonly it is some
     * form of popup or dialog window.
     */
    public void show() {
        if (!isDisabled()) {
            setShowing(true);
        }
    }

    /**
     * Closes the popup / dialog that was shown when {@link #show()} was called.
     */
    public void hide() {
        if (isShowing()) {
            setShowing(false);
        }
    }

    /**
     * Arms the ComboBox. An armed ComboBox will show a popup list on the next
     * expected UI gesture.
     *
     * Note: This function is intended to be used by experts, primarily
     *       by those implementing new Skins or Behaviors. It is not common
     *       for developers or designers to access this function directly.
     */
    public void arm() {
        if (! armedProperty().isBound()) {
            setArmed(true);
        }
    }

    /**
     * Disarms the ComboBox. See {@link #arm()}.
     *
     * Note: This function is intended to be used by experts, primarily
     *       by those implementing new Skins or Behaviors. It is not common
     *       for developers or designers to access this function directly.
     */
    public void disarm() {
        if (! armedProperty().isBound()) {
            setArmed(false);
        }
    }


    /* *************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "combo-box-base";

    private static final PseudoClass PSEUDO_CLASS_EDITABLE =
            PseudoClass.getPseudoClass("editable");
    private static final PseudoClass PSEUDO_CLASS_SHOWING =
            PseudoClass.getPseudoClass("showing");
    private static final PseudoClass PSEUDO_CLASS_ARMED =
            PseudoClass.getPseudoClass("armed");


    /* *************************************************************************
     *                                                                         *
     * Accessibility handling                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case EXPANDED: return isShowing();
            case EDITABLE: return isEditable();
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
        switch (action) {
            case EXPAND: show(); break;
            case COLLAPSE: hide(); break;
            default: super.executeAccessibleAction(action); break;
        }
    }
}

