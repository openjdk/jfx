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

import com.sun.javafx.css.Styleable;
import com.sun.javafx.css.StyleableProperty;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableMap;

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
 */
public class MenuItem implements EventTarget {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public MenuItem() {
        this(null,null);
    }

    /**
     * Constructs a MenuItem and sets the display text with the specified text
     * @see #setText
     */
    public MenuItem(String text) {
        this(text,null);
    }

    /**
     * Constructor s MenuItem and sets the display text with the specified text
     * and sets the graphic {@link Node} to the given node.
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
    
    private final EventHandlerManager eventHandlerManager =
            new EventHandlerManager(this);

    private Object userData;
    private ObservableMap<Object, Object> properties;

    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/
    
    private StringProperty id;
    public final void setId(String value) { idProperty().set(value); }
    public final String getId() { return id == null ? null : id.get(); }
    public final StringProperty idProperty() {
        if (id == null) {
            id = new SimpleStringProperty(this, "id");
        }
        return id;
    }
    
    private StringProperty style;
    public final void setStyle(String value) { styleProperty().set(value); }
    public final String getStyle() { return style == null ? null : style.get(); }
    public final StringProperty styleProperty() {
        if (style == null) {
            style = new SimpleStringProperty(this, "style");
        }
        return style;
    }
    
    // --- Parent Menu (useful for submenus)
    private ReadOnlyObjectWrapper<Menu> parentMenu;

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
    private ReadOnlyObjectWrapper<ContextMenu> parentPopup;

    /**
     * This is the {@link ContextMenu} in which this {@code MenuItem} exists.
     */
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
    private StringProperty text;

    /**
     * The text to display in the {@code MenuItem}.
     */
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
    private ObjectProperty<Node> graphic;

    /**
     * An optional graphic for the {@code MenuItem}. This will normally be
     * an {@link javafx.scene.image.ImageView} node, but there is no requirement for this to be
     * the case.
     */
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
    private ObjectProperty<EventHandler<ActionEvent>> onAction;

    /**
     * The action, which is invoked whenever the MenuItem is fired. This
     * may be due to the user clicking on the button with the mouse, or by
     * a touch event, or by a key press, or if the developer programatically
     * invokes the {@link #fire()} method.
     */
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
    
    
    // --- Disable
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
    private BooleanProperty visible;
    public final void setVisible(boolean value) { visibleProperty().set(value); }
    public final boolean isVisible() { return visible == null ? true : visible.get(); }
    public final BooleanProperty visibleProperty() {
        if (visible == null) {
            visible = new SimpleBooleanProperty(this, "visible", true);
        }
        return visible;
    }

 
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
    
    public ObservableList<String> getStyleClass() {
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

    protected Styleable styleable; 
    /**
     * RT-19263
     * @treatAsPrivate implementation detail
     * @deprecated This is an experimental API that is not intended for general use and is subject to change in future versions
     */    
    public Styleable impl_getStyleable() {
        
        if (styleable == null) {
            styleable = new Styleable() {

                @Override
                public String getId() {
                    return MenuItem.this.getId();
                }

                @Override
                public List<String> getStyleClass() {
                    return MenuItem.this.getStyleClass();
                }

                @Override
                public String getStyle() {
                    return MenuItem.this.getStyle();
                }

                @Override
                public Styleable getStyleableParent() {
                    if(MenuItem.this.getParentMenu() == null) {
                        return MenuItem.this.getParentPopup() != null 
                            ? MenuItem.this.getParentPopup().impl_getStyleable()
                            : null;
                    } else {
                        return MenuItem.this.getParentMenu() != null 
                            ? MenuItem.this.getParentMenu().impl_getStyleable()
                            : null;
                    }
                }

                
                @Override
                public List<StyleableProperty> getStyleableProperties() {
                    return Collections.EMPTY_LIST;
                }                

                @Override
                public Node getNode() {
                    return null;
                }

            };
        }
        return styleable;
    }    
}
