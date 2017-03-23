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

import com.sun.javafx.beans.IDProperty;
import javafx.collections.ObservableSet;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.css.CssMetaData;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.input.KeyCombination;

import com.sun.javafx.event.EventHandlerManager;
import com.sun.javafx.scene.control.ContextMenuContent;
import javafx.scene.control.skin.ContextMenuSkin;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableMap;
import javafx.scene.Parent;

/**
 * <p>
 * MenuItem is intended to be used in conjunction with {@link Menu} to provide
 * options to users. MenuItem serves as the base class for the bulk of JavaFX menus
 * API.
 * It has a display {@link #getText() text} property, as well as an optional {@link #getGraphic() graphic} node
 * that can be set on it.
 * The {@link #getAccelerator() accelerator} property enables accessing the
 * associated action in one keystroke. Also, as with the {@link Button} control,
 * by using the {@link #setOnAction} method, you can have an instance of MenuItem
 * perform any action you wish.
 * <p>
 * <b>Note:</b> Whilst any size of graphic can be inserted into a MenuItem, the most
 * commonly used size in most applications is 16x16 pixels. This is
 * the recommended graphic dimension to use if you're using the default style provided by
 * JavaFX.
 * <p>
 * To create a MenuItem is simple:
<pre><code>
MenuItem menuItem = new MenuItem("Open");
menuItem.setOnAction(new EventHandler&lt;ActionEvent&gt;() {
    &#064;Override public void handle(ActionEvent e) {
        System.out.println("Opening Database Connection...");
    }
});
menuItem.setGraphic(new ImageView(new Image("flower.png")));
</code></pre>
 * <p>
 * Refer to the {@link Menu} page to learn how to insert MenuItem into a menu
 * instance. Briefly however, you can insert the MenuItem from the previous
 * example into a Menu as such:
<pre><code>
final Menu menu = new Menu("File");
menu.getItems().add(menuItem);
</code></pre>
 *
 * @see Menu
 * @since JavaFX 2.0
 */
@IDProperty("id")
public class MenuItem implements EventTarget, Styleable {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Constructs a MenuItem with no display text.
     */
    public MenuItem() {
        this(null,null);
    }

    /**
     * Constructs a MenuItem and sets the display text with the specified text
     * @param text the display text
     * @see #setText
     */
    public MenuItem(String text) {
        this(text,null);
    }

    /**
     * Constructor s MenuItem and sets the display text with the specified text
     * and sets the graphic {@link Node} to the given node.
     * @param text the display text
     * @param graphic the graphic node
     * @see #setText
     * @see #setGraphic
     */
    public MenuItem(String text, Node graphic) {
        setText(text);
        setGraphic(graphic);
        styleClass.add(DEFAULT_STYLE_CLASS);
    }



    /***************************************************************************
     *                                                                         *
     * Instance Variables                                                      *
     *                                                                         *
     **************************************************************************/

    private final ObservableList<String> styleClass = FXCollections.observableArrayList();

    final EventHandlerManager eventHandlerManager =
            new EventHandlerManager(this);

    private Object userData;
    private ObservableMap<Object, Object> properties;

    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * The id of this MenuItem. This simple string identifier is useful for finding
     * a specific MenuItem within the scene graph.
     */
    private StringProperty id;
    public final void setId(String value) { idProperty().set(value); }
    @Override public final String getId() { return id == null ? null : id.get(); }
    public final StringProperty idProperty() {
        if (id == null) {
            id = new SimpleStringProperty(this, "id");
        }
        return id;
    }

    /**
     * A string representation of the CSS style associated with this specific MenuItem.
     * This is analogous to the "style" attribute of an HTML element. Note that,
     * like the HTML style attribute, this variable contains style properties and
     * values and not the selector portion of a style rule.
     */
    private StringProperty style;
    public final void setStyle(String value) { styleProperty().set(value); }
    @Override public final String getStyle() { return style == null ? null : style.get(); }
    public final StringProperty styleProperty() {
        if (style == null) {
            style = new SimpleStringProperty(this, "style");
        }
        return style;
    }

    // --- Parent Menu (useful for submenus)
    /**
     * This is the {@link Menu} in which this {@code MenuItem} exists. It is
     * possible for an instance of this class to not have a {@code parentMenu} -
     * this means that this instance is either:
     * <ul>
     * <li>Not yet associated with its {@code parentMenu}.
     * <li>A 'root' {@link Menu} (i.e. it is a context menu, attached directly to a
     * {@link MenuBar}, {@link MenuButton}, or any of the other controls that use
     * {@link Menu} internally.
     * </ul>
     */
    private ReadOnlyObjectWrapper<Menu> parentMenu;

    protected final void setParentMenu(Menu value) {
        parentMenuPropertyImpl().set(value);
    }

    public final Menu getParentMenu() {
        return parentMenu == null ? null : parentMenu.get();
    }

    public final ReadOnlyObjectProperty<Menu> parentMenuProperty() {
        return parentMenuPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<Menu> parentMenuPropertyImpl() {
        if (parentMenu == null) {
            parentMenu = new ReadOnlyObjectWrapper<Menu>(this, "parentMenu");
        }
        return parentMenu;
    }


    // --- Parent Popup
     /**
     * This is the {@link ContextMenu} in which this {@code MenuItem} exists.
     */
    private ReadOnlyObjectWrapper<ContextMenu> parentPopup;

    protected final void setParentPopup(ContextMenu value) {
        parentPopupPropertyImpl().set(value);
    }

    public final ContextMenu getParentPopup() {
        return parentPopup == null ? null : parentPopup.get();
    }

    public final ReadOnlyObjectProperty<ContextMenu> parentPopupProperty() {
        return parentPopupPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<ContextMenu> parentPopupPropertyImpl() {
        if (parentPopup == null) {
            parentPopup = new ReadOnlyObjectWrapper<ContextMenu>(this, "parentPopup");
        }
        return parentPopup;
    }


    // --- Text
    /**
     * The text to display in the {@code MenuItem}.
     */
    private StringProperty text;

    public final void setText(String value) {
        textProperty().set(value);
    }

    public final String getText() {
        return text == null ? null : text.get();
    }

    public final StringProperty textProperty() {
        if (text == null) {
            text = new SimpleStringProperty(this, "text");
        }
        return text;
    }


    // --- Graphic
    /**
     * An optional graphic for the {@code MenuItem}. This will normally be
     * an {@link javafx.scene.image.ImageView} node, but there is no requirement for this to be
     * the case.
     */
    private ObjectProperty<Node> graphic;

    public final void setGraphic(Node value) {
        graphicProperty().set(value);
    }

    public final Node getGraphic() {
        return graphic == null ? null : graphic.get();
    }

    public final ObjectProperty<Node> graphicProperty() {
        if (graphic == null) {
            graphic = new SimpleObjectProperty<Node>(this, "graphic");
        }
        return graphic;
    }


    // --- OnAction
    /**
     * The action, which is invoked whenever the MenuItem is fired. This
     * may be due to the user clicking on the button with the mouse, or by
     * a touch event, or by a key press, or if the developer programatically
     * invokes the {@link #fire()} method.
     */
    private ObjectProperty<EventHandler<ActionEvent>> onAction;

    public final void setOnAction(EventHandler<ActionEvent> value) {
        onActionProperty().set( value);
    }

    public final EventHandler<ActionEvent> getOnAction() {
        return onAction == null ? null : onAction.get();
    }

    public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
        if (onAction == null) {
            onAction = new ObjectPropertyBase<EventHandler<ActionEvent>>() {
                @Override protected void invalidated() {
                    eventHandlerManager.setEventHandler(ActionEvent.ACTION, get());
                }

                @Override
                public Object getBean() {
                    return MenuItem.this;
                }

                @Override
                public String getName() {
                    return "onAction";
                }
            };
        }
        return onAction;
    }

    /**
     * <p>Called when a accelerator for the Menuitem is invoked</p>
     * @since JavaFX 2.2
     */
    public static final EventType<Event> MENU_VALIDATION_EVENT = new EventType<Event>
            (Event.ANY, "MENU_VALIDATION_EVENT");

    /**
     * The event handler that is associated with invocation of an accelerator for a MenuItem. This
     * can happen when a key sequence for an accelerator is pressed. The event handler is also
     * invoked when onShowing event handler is called.
     * @since JavaFX 2.2
     */
    private ObjectProperty<EventHandler<Event>> onMenuValidation;

    public final void setOnMenuValidation(EventHandler<Event> value) {
        onMenuValidationProperty().set( value);
    }

    public final EventHandler<Event> getOnMenuValidation() {
        return onMenuValidation == null ? null : onMenuValidation.get();
    }

    public final ObjectProperty<EventHandler<Event>> onMenuValidationProperty() {
        if (onMenuValidation == null) {
            onMenuValidation = new ObjectPropertyBase<EventHandler<Event>>() {
                @Override protected void invalidated() {
                    eventHandlerManager.setEventHandler(MENU_VALIDATION_EVENT, get());
                }
                @Override public Object getBean() {
                    return MenuItem.this;
                }
                @Override public String getName() {
                    return "onMenuValidation";
                }
            };
        }
        return onMenuValidation;
    }

    // --- Disable
    /**
     * Sets the individual disabled state of this MenuItem.
     * Setting disable to true will cause this MenuItem to become disabled.
     */
    private BooleanProperty disable;
    public final void setDisable(boolean value) { disableProperty().set(value); }
    public final boolean isDisable() { return disable == null ? false : disable.get(); }
    public final BooleanProperty disableProperty() {
        if (disable == null) {
            disable = new SimpleBooleanProperty(this, "disable");
        }
        return disable;
    }


    // --- Visible
    /**
     * Specifies whether this MenuItem should be rendered as part of the scene graph.
     */
    private BooleanProperty visible;
    public final void setVisible(boolean value) { visibleProperty().set(value); }
    public final boolean isVisible() { return visible == null ? true : visible.get(); }
    public final BooleanProperty visibleProperty() {
        if (visible == null) {
            visible = new SimpleBooleanProperty(this, "visible", true);
        }
        return visible;
    }

    /**
     * The accelerator property enables accessing the associated action in one keystroke.
     * It is a convenience offered to perform quickly a given action.
     */
    private ObjectProperty<KeyCombination> accelerator;
    public final void setAccelerator(KeyCombination value) {
        acceleratorProperty().set(value);
    }
    public final KeyCombination getAccelerator() {
        return accelerator == null ? null : accelerator.get();
    }
    public final ObjectProperty<KeyCombination> acceleratorProperty() {
        if (accelerator == null) {
            accelerator = new SimpleObjectProperty<KeyCombination>(this, "accelerator");
        }
        return accelerator;
    }

    /**
     * MnemonicParsing property to enable/disable text parsing.
     * If this is set to true, then the MenuItem text will be
     * parsed to see if it contains the mnemonic parsing character '_'.
     * When a mnemonic is detected the key combination will
     * be determined based on the succeeding character, and the mnemonic
     * added.
     *
     * <p>
     * The default value for MenuItem is true.
     * </p>
     */
    private BooleanProperty mnemonicParsing;
    public final void setMnemonicParsing(boolean value) {
        mnemonicParsingProperty().set(value);
    }
    public final boolean isMnemonicParsing() {
        return mnemonicParsing == null ? true : mnemonicParsing.get();
    }
    public final BooleanProperty mnemonicParsingProperty() {
        if (mnemonicParsing == null) {
            mnemonicParsing = new SimpleBooleanProperty(this, "mnemonicParsing", true);
        }
        return mnemonicParsing;
    }

    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    @Override public ObservableList<String> getStyleClass() {
        return styleClass;
    }

    /**
     * Fires a new ActionEvent.
     */
    public void fire() {
        Event.fireEvent(this, new ActionEvent(this, this));
    }

    /**
     * Registers an event handler to this MenuItem. The handler is called when the
     * menu item receives an {@code Event} of the specified type during the bubbling
     * phase of event delivery.
     *
     * @param <E> the specific event class of the handler
     * @param eventType the type of the events to receive by the handler
     * @param eventHandler the handler to register
     * @throws NullPointerException if the event type or handler is null
     */
    public <E extends Event> void addEventHandler(EventType<E> eventType, EventHandler<E> eventHandler) {
        eventHandlerManager.addEventHandler(eventType, eventHandler);
    }

    /**
     * Unregisters a previously registered event handler from this MenuItem. One
     * handler might have been registered for different event types, so the
     * caller needs to specify the particular event type from which to
     * unregister the handler.
     *
     * @param <E> the specific event class of the handler
     * @param eventType the event type from which to unregister
     * @param eventHandler the handler to unregister
     * @throws NullPointerException if the event type or handler is null
     */
    public <E extends Event> void removeEventHandler(EventType<E> eventType, EventHandler<E> eventHandler) {
        eventHandlerManager.removeEventHandler(eventType, eventHandler);
    }

    /** {@inheritDoc} */
    @Override public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        // FIXME review that these are configure properly
        if (getParentPopup() != null) {
            getParentPopup().buildEventDispatchChain(tail);
        }

        if (getParentMenu() != null) {
            getParentMenu().buildEventDispatchChain(tail);
        }

        return tail.prepend(eventHandlerManager);
    }

    /**
     * Returns a previously set Object property, or null if no such property
     * has been set using the {@link MenuItem#setUserData(java.lang.Object)} method.
     *
     * @return The Object that was previously set, or null if no property
     *          has been set or if null was set.
     */
    public Object getUserData() {
        return userData;
    }

    /**
     * Convenience method for setting a single Object property that can be
     * retrieved at a later date. This is functionally equivalent to calling
     * the getProperties().put(Object key, Object value) method. This can later
     * be retrieved by calling {@link Node#getUserData()}.
     *
     * @param value The value to be stored - this can later be retrieved by calling
     *          {@link Node#getUserData()}.
     */
    public void setUserData(Object value) {
        this.userData = value;
    }

    /**
     * Returns an observable map of properties on this menu item for use primarily
     * by application developers.
     *
     * @return an observable map of properties on this menu item for use primarily
     * by application developers
     */
    public ObservableMap<Object, Object> getProperties() {
        if (properties == null) {
            properties = FXCollections.observableMap(new HashMap<Object, Object>());
        }
        return properties;
    }

    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "menu-item";

    /**
     * {@inheritDoc}
     * @return "MenuItem"
     * @since JavaFX 8.0
     */
    @Override
    public String getTypeSelector() {
        return "MenuItem";
    }

    /**
     * {@inheritDoc}
     * @return {@code getParentMenu()}, or {@code getParentPopup()}
     * if {@code parentMenu} is null
     * @since JavaFX 8.0
     */
    @Override
    public Styleable getStyleableParent() {

        if(getParentMenu() == null) {
            return getParentPopup();
        } else {
            return getParentMenu();
        }
    }

    /**
     * {@inheritDoc}
     * @since JavaFX 8.0
     */
    @Override
    public final ObservableSet<PseudoClass> getPseudoClassStates() {
        return FXCollections.emptyObservableSet();
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override public Node getStyleableNode() {
        // Fix for RT-20582. We dive into the visual representation
        // of this MenuItem so that we may return it to the caller.
        ContextMenu parentPopup = MenuItem.this.getParentPopup();
        if (parentPopup == null || ! (parentPopup.getSkin() instanceof ContextMenuSkin)) return null;

        ContextMenuSkin skin = (ContextMenuSkin) parentPopup.getSkin();
        if (! (skin.getNode() instanceof ContextMenuContent)) return null;

        ContextMenuContent content = (ContextMenuContent) skin.getNode();
        Parent nodes = content.getItemsContainer();

        MenuItem desiredMenuItem = MenuItem.this;
        List<Node> childrenNodes = nodes.getChildrenUnmodifiable();
        for (int i = 0; i < childrenNodes.size(); i++) {
            if (! (childrenNodes.get(i) instanceof ContextMenuContent.MenuItemContainer)) continue;

            ContextMenuContent.MenuItemContainer MenuRow =
                    (ContextMenuContent.MenuItemContainer) childrenNodes.get(i);

            if (desiredMenuItem.equals(MenuRow.getItem())) {
                return MenuRow;
            }
        }

        return null;
    }

    @Override public String toString() {
        StringBuilder sbuf = new StringBuilder(getClass().getSimpleName());

        boolean hasId = id != null && !"".equals(getId());
        boolean hasStyleClass = !getStyleClass().isEmpty();

        if (!hasId) {
            sbuf.append('@');
            sbuf.append(Integer.toHexString(hashCode()));
        } else {
            sbuf.append("[id=");
            sbuf.append(getId());
            if (!hasStyleClass) sbuf.append("]");
        }

        if (hasStyleClass) {
            if (!hasId) sbuf.append('[');
            else sbuf.append(", ");
            sbuf.append("styleClass=");
            sbuf.append(getStyleClass());
            sbuf.append("]");
        }
        return sbuf.toString();
    }
}
