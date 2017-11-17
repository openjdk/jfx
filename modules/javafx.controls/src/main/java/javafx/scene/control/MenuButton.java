/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

import javafx.css.PseudoClass;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Side;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.skin.MenuButtonSkin;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;

/**
 * MenuButton is a button which, when clicked or pressed, will show a
 * {@link ContextMenu}. A MenuButton shares a very similar API to the {@link Menu}
 * control, insofar that you set the items that should be shown in the
 * {@link #getItems() items} ObservableList, and there is a {@link #textProperty() text} property to specify the
 * label shown within the MenuButton.
 * <p>
 * As mentioned, like the Menu API itself, you'll find an {@link #getItems() items} ObservableList
 * within which you can provide anything that extends from {@link MenuItem}.
 * There are several useful subclasses of {@link MenuItem} including
 * {@link RadioMenuItem}, {@link CheckMenuItem}, {@link Menu},
 * {@link SeparatorMenuItem} and {@link CustomMenuItem}.
 * <p>
 * A MenuButton can be set to show its menu on any side of the button. This is
 * specified using the {@link #popupSideProperty() popupSide} property. By default
 * the menu appears below the button. However, regardless of the popupSide specified,
 * if there is not enough room, the {@link ContextMenu} will be
 * smartly repositioned, most probably to be on the opposite side of the
 * MenuButton.
 *
 * <p>Example:</p>
 * <pre>
 * MenuButton m = new MenuButton("Eats");
 * m.getItems().addAll(new MenuItem("Burger"), new MenuItem("Hot Dog"));
 * </pre>
 *
 * <p>
 * MnemonicParsing is enabled by default for MenuButton.
 * </p>
 *
 * @see MenuItem
 * @see Menu
 * @see SplitMenuButton
 * @since JavaFX 2.0
 */
public class MenuButton extends ButtonBase {


    /***************************************************************************
     *                                                                         *
     * Static properties and methods                                           *
     *                                                                         *
     **************************************************************************/

    /**
     * Called prior to the MenuButton showing its popup after the user
     * has clicked or otherwise interacted with the MenuButton.
     * @since JavaFX 8u60
     */
    public static final EventType<Event> ON_SHOWING =
            new EventType<Event>(Event.ANY, "MENU_BUTTON_ON_SHOWING");

    /**
     * Called after the MenuButton has shown its popup.
     * @since JavaFX 8u60
     */
    public static final EventType<Event> ON_SHOWN =
            new EventType<Event>(Event.ANY, "MENU_BUTTON_ON_SHOWN");

    /**
     * Called when the MenuButton popup <b>will</b> be hidden.
     * @since JavaFX 8u60
     */
    public static final EventType<Event> ON_HIDING =
            new EventType<Event>(Event.ANY, "MENU_BUTTON_ON_HIDING");

    /**
     * Called when the MenuButton popup has been hidden.
     * @since JavaFX 8u60
     */
    public static final EventType<Event> ON_HIDDEN =
            new EventType<Event>(Event.ANY, "MENU_BUTTON_ON_HIDDEN");


    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new empty menu button. Use {@link #setText(String)},
     * {@link #setGraphic(Node)} and {@link #getItems()} to set the content.
     */
    public MenuButton() {
        this(null, null);
    }

    /**
     * Creates a new empty menu button with the given text to display on the
     * button. Use {@link #setGraphic(Node)} and {@link #getItems()} to set the
     * content.
     *
     * @param text the text to display on the menu button
     */
    public MenuButton(String text) {
        this(text, null);
    }

    /**
     * Creates a new empty menu button with the given text and graphic to
     * display on the button. Use {@link #getItems()} to set the content.
     *
     * @param text the text to display on the menu button
     * @param graphic the graphic to display on the menu button
     */
    public MenuButton(String text, Node graphic) {
        this(text, graphic, (MenuItem[])null);
    }

    /**
     * Creates a new menu button with the given text and graphic to
     * display on the button, and inserts the given items
     * into the {@link #getItems() items} list.
     *
     * @param text the text to display on the menu button
     * @param graphic the graphic to display on the menu button
     * @param items The items to display in the popup menu.
     * @since JavaFX 8u40
     */
    public MenuButton(String text, Node graphic, MenuItem... items) {
        if (text != null) {
            setText(text);
        }
        if (graphic != null) {
            setGraphic(graphic);
        }
        if (items != null) {
            getItems().addAll(items);
        }

        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        setAccessibleRole(AccessibleRole.MENU_BUTTON);
        setMnemonicParsing(true);     // enable mnemonic auto-parsing by default
        // the default value for popupSide = Side.BOTTOM therefor
        // PSEUDO_CLASS_OPENVERTICALLY should be set from the start.
        pseudoClassStateChanged(PSEUDO_CLASS_OPENVERTICALLY, true);
    }

    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/
    private final ObservableList<MenuItem> items = FXCollections.<MenuItem>observableArrayList();

    /**
     * The items to show within this buttons menu. If this ObservableList is modified
     * at runtime, the Menu will update as expected.
     * <p>
     * Commonly used controls include including {@code MenuItem},
     * {@code CheckMenuItem}, {@code RadioMenuItem},
     * and of course {@code Menu}, which if added to a menu, will become a sub
     * menu. {@link SeparatorMenuItem} is another commonly used Node in the Menu's items
     * ObservableList.
     * @return the list of menu items within this buttons menu
     */
    public final ObservableList<MenuItem> getItems() {
        return items;
    }

    // --- Showing
    /**
     * Indicates whether the {@link ContextMenu} is currently visible.
     */
    private ReadOnlyBooleanWrapper showing = new ReadOnlyBooleanWrapper(this, "showing", false) {
        @Override protected void invalidated() {
            pseudoClassStateChanged(PSEUDO_CLASS_SHOWING, get());
            super.invalidated();
        }
    };
    private void setShowing(boolean value) {
        // these events will not fire if the showing property is bound
        Event.fireEvent(this, value ? new Event(ON_SHOWING) :
                new Event(ON_HIDING));
        showing.set(value);
        Event.fireEvent(this, value ? new Event(ON_SHOWN) :
                new Event(ON_HIDDEN));
    }
    public final boolean isShowing() { return showing.get(); }
    public final ReadOnlyBooleanProperty showingProperty() { return showing.getReadOnlyProperty(); }



    /**
     * Indicates on which side the {@link ContextMenu} should open in
     * relation to the MenuButton. Menu items are generally laid
     * out vertically in either case.
     * For example, if the menu button were in a vertical toolbar on the left
     * edge of the application, you might change {@link #popupSideProperty() popupSide}
     * to {@code Side.RIGHT} so that the popup will appear to the right of the MenuButton.
     *
     * @defaultValue {@code Side.BOTTOM}
     */
    // TODO expose via CSS
    private ObjectProperty<Side> popupSide;

    public final void setPopupSide(Side value) {
        popupSideProperty().set(value);
    }

    public final Side getPopupSide() {
        return popupSide == null ? Side.BOTTOM : popupSide.get();
    }

    public final ObjectProperty<Side> popupSideProperty() {
        if (popupSide == null) {
            popupSide = new ObjectPropertyBase<Side>(Side.BOTTOM) {
                @Override protected void invalidated() {
                    final Side side = get();
                    final boolean active = (side == Side.TOP) || (side == Side.BOTTOM);
                    pseudoClassStateChanged(PSEUDO_CLASS_OPENVERTICALLY, active);
                }

                @Override
                public Object getBean() {
                    return MenuButton.this;
                }

                @Override
                public String getName() {
                    return "popupSide";
                }
            };
        }
        return popupSide;
    }

    /**
     * Called just prior to the {@code ContextMenu} being shown.
     * @return the on showing property
     * @since 10
     */
    public final ObjectProperty<EventHandler<Event>> onShowingProperty() { return onShowing; }
    public final void setOnShowing(EventHandler<Event> value) { onShowingProperty().set(value); }
    public final EventHandler<Event> getOnShowing() { return onShowingProperty().get(); }
    private ObjectProperty<EventHandler<Event>> onShowing = new ObjectPropertyBase<EventHandler<Event>>() {
        @Override protected void invalidated() {
            setEventHandler(ON_SHOWING, get());
        }

        @Override public Object getBean() {
            return MenuButton.this;
        }

        @Override public String getName() {
            return "onShowing";
        }
    };

    /**
     * Called just after the {@code ContextMenu} is shown.
     * @return the on shown property
     * @since 10
     */
    public final ObjectProperty<EventHandler<Event>> onShownProperty() { return onShown; }
    public final void setOnShown(EventHandler<Event> value) { onShownProperty().set(value); }
    public final EventHandler<Event> getOnShown() { return onShownProperty().get(); }
    private ObjectProperty<EventHandler<Event>> onShown = new ObjectPropertyBase<EventHandler<Event>>() {
        @Override protected void invalidated() {
            setEventHandler(ON_SHOWN, get());
        }

        @Override public Object getBean() {
            return MenuButton.this;
        }

        @Override public String getName() {
            return "onShown";
        }
    };

    /**
     * Called just prior to the {@code ContextMenu} being hidden.
     * @return the on hiding property
     * @since 10
     */
    public final ObjectProperty<EventHandler<Event>> onHidingProperty() { return onHiding; }
    public final void setOnHiding(EventHandler<Event> value) { onHidingProperty().set(value); }
    public final EventHandler<Event> getOnHiding() { return onHidingProperty().get(); }
    private ObjectProperty<EventHandler<Event>> onHiding = new ObjectPropertyBase<EventHandler<Event>>() {
        @Override protected void invalidated() {
            setEventHandler(ON_HIDING, get());
        }

        @Override public Object getBean() {
            return MenuButton.this;
        }

        @Override public String getName() {
            return "onHiding";
        }
    };

    /**
     * Called just after the {@code ContextMenu} has been hidden.
     * @return the on hidden property
     * @since 10
     */
    public final ObjectProperty<EventHandler<Event>> onHiddenProperty() { return onHidden; }
    public final void setOnHidden(EventHandler<Event> value) { onHiddenProperty().set(value); }
    public final EventHandler<Event> getOnHidden() { return onHiddenProperty().get(); }
    private ObjectProperty<EventHandler<Event>> onHidden = new ObjectPropertyBase<EventHandler<Event>>() {
        @Override protected void invalidated() {
            setEventHandler(ON_HIDDEN, get());
        }

        @Override public Object getBean() {
            return MenuButton.this;
        }

        @Override public String getName() {
            return "onHidden";
        }
    };


    /***************************************************************************
     *                                                                         *
     * Control methods                                                         *
     *                                                                         *
     **************************************************************************/

    /**
     * Shows the {@link ContextMenu}, assuming this MenuButton is not disabled.
     *
     * @see #isDisabled()
     * @see #isShowing()
     */
    public void show() {
        // TODO: isBound check is probably unnecessary here
        if (!isDisabled() && !showing.isBound()) {
            setShowing(true);
        }
    }

    /**
     * Hides the {@link ContextMenu}.
     *
     * @see #isShowing()
     */
    public void hide() {
        // TODO: isBound check is probably unnecessary here
        if (!showing.isBound()) {
            setShowing(false);
        }
    }

    /**
     * This has no impact.
     */
    @Override
    public void fire() {
        if (!isDisabled()) {
            fireEvent(new ActionEvent());
        }
    }

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new MenuButtonSkin(this);
    }

    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "menu-button";
    private static final PseudoClass PSEUDO_CLASS_OPENVERTICALLY =
            PseudoClass.getPseudoClass("openvertically");
    private static final PseudoClass PSEUDO_CLASS_SHOWING =
            PseudoClass.getPseudoClass("showing");

    /***************************************************************************
     *                                                                         *
     * Accessibility handling                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override
    public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
        switch (action) {
            case FIRE:
                if (isShowing()) {
                    hide();
                } else {
                    show();
                }
                break;
            default: super.executeAccessibleAction(action);
        }
    }
}
