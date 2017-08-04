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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;

import com.sun.javafx.collections.TrackableObservableList;
import com.sun.javafx.scene.control.Logging;
import javafx.beans.DefaultProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.event.EventDispatchChain;

/**
 * <p>
 * A popup menu of actionable items which is displayed to the user only upon request.
 * When a menu is visible, in most use cases, the user can select one menu item
 * before the menu goes back to its hidden state. This means the menu is a good
 * place to put important functionality that does not necessarily need to be
 * visible at all times to the user.
 * <p>
 * Menus are typically placed in a {@link MenuBar}, or as a submenu of another Menu.
 * If the intention is to offer a context menu when the user right-clicks in a
 * certain area of their user interface, then this is the wrong control to use.
 * This is because when Menu is added to the scenegraph, it has a visual
 * representation that will result in it appearing on screen. Instead,
 * {@link ContextMenu} should be used in this circumstance.
 * <p>
 * Creating a Menu and inserting it into a MenuBar is easy, as shown below:
 * <pre><code>
 * final Menu menu1 = new Menu("File");
 * MenuBar menuBar = new MenuBar();
 * menuBar.getMenus().add(menu1);
 * </code></pre>
 * <p>
 * A Menu is a subclass of {@link MenuItem} which means that it can be inserted
 * into a Menu's {@link #getItems() items} ObservableList, resulting in a submenu being created:
 * <pre><code>
 * MenuItem menu12 = new MenuItem("Open");
 * menu1.getItems().add(menu12);
 * </code></pre>
 * <p>
 * The items ObservableList allows for any {@link MenuItem} type to be inserted,
 * including its subclasses {@link Menu}, {@link MenuItem}, {@link RadioMenuItem}, {@link CheckMenuItem},
 * {@link CustomMenuItem} and {@link SeparatorMenuItem}. In order to insert an arbitrary {@link Node} to
 * a Menu, a CustomMenuItem can be used. One exception to this general rule is that
 * {@link SeparatorMenuItem} could be used for inserting a separator.
 *
 * @see MenuBar
 * @see MenuItem
 * @since JavaFX 2.0
 */
@DefaultProperty("items")
public class Menu extends MenuItem {

    /**
     * <p>Called when the contextMenu for this menu <b>will</b> be shown. However if the
     * contextMenu is empty then this will not be called.
     * </p>
     */
    public static final EventType<Event> ON_SHOWING =
            new EventType<Event>(Event.ANY, "MENU_ON_SHOWING");

    /**
     * <p>Called when the contextMenu for this menu shows. However if the
     * contextMenu is empty then this will not be called.
     * </p>
     */
    public static final EventType<Event> ON_SHOWN =
            new EventType<Event>(Event.ANY, "MENU_ON_SHOWN");

    /**
     * <p>Called when the contextMenu for this menu <b>will</b> be hidden. However if the
     * contextMenu is empty then this will not be called.
     * </p>
     */
    public static final EventType<Event> ON_HIDING =
            new EventType<Event>(Event.ANY, "MENU_ON_HIDING");

    /**
     * <p>Called when the contextMenu for this menu is hidden. However if the
     * contextMenu is empty then this will not be called.
     * </p>
     */
    public static final EventType<Event> ON_HIDDEN =
            new EventType<Event>(Event.ANY, "MENU_ON_HIDDEN");

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Constructs a Menu with an empty string for its display text.
     * @since JavaFX 2.2
     */
    public Menu() {
        this("");
    }

    /**
     * Constructs a Menu and sets the display text with the specified text.
     *
     * @param text the text to display on the menu button
     */
    public Menu(String text) {
        this(text,null);
    }

    /**
     * Constructs a Menu and sets the display text with the specified text
     * and sets the graphic {@link Node} to the given node.
     *
     * @param text the text to display on the menu button
     * @param graphic the graphic to display on the menu button
     */
    public Menu(String text, Node graphic) {
        this(text, graphic, (MenuItem[])null);
    }

    /**
     * Constructs a Menu and sets the display text with the specified text,
     * the graphic {@link Node} to the given node, and inserts the given items
     * into the {@link #getItems() items} list.
     *
     * @param text the text to display on the menu button
     * @param graphic the graphic to display on the menu button
     * @param items The items to display in the popup menu.
     * @since JavaFX 8u40
     */
    public Menu(String text, Node graphic, MenuItem... items) {
        super(text,graphic);
        getStyleClass().add(DEFAULT_STYLE_CLASS);

        if (items != null) {
            getItems().addAll(items);
        }

        parentPopupProperty().addListener(observable -> {
            for (int i = 0; i < getItems().size(); i++) {
                MenuItem item = getItems().get(i);
                item.setParentPopup(getParentPopup());
            }
        });
    }



     /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * Indicates whether the {@link ContextMenu} is currently visible.
     *
     * @defaultValue false
     */
    private ReadOnlyBooleanWrapper showing;

    private void setShowing(boolean value) {
        if (getItems().size() == 0 || (value && isShowing())) return;

        // these events will not fire if the showing property is bound
        if (value) {
           if (getOnMenuValidation() != null) {
                Event.fireEvent(this, new Event(MENU_VALIDATION_EVENT));
                for(MenuItem m : getItems()) {
                    if (!(m instanceof Menu) && m.getOnMenuValidation() != null) {
                        Event.fireEvent(m, new Event(MenuItem.MENU_VALIDATION_EVENT));
                    }
                }
           }
           Event.fireEvent(this, new Event(Menu.ON_SHOWING));
        } else {
           Event.fireEvent(this, new Event(Menu.ON_HIDING));
        }
        showingPropertyImpl().set(value);
        Event.fireEvent(this, (value) ? new Event(Menu.ON_SHOWN) :
            new Event(Menu.ON_HIDDEN));
    }

    public final boolean isShowing() {
        return showing == null ? false : showing.get();
    }

    public final ReadOnlyBooleanProperty showingProperty() {
        return showingPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyBooleanWrapper showingPropertyImpl() {
        if (showing == null) {
            showing = new ReadOnlyBooleanWrapper() {
                @Override protected void invalidated() {
                    // force validation
                    get();

                    // update the styleclass
                    if (isShowing()) {
                        getStyleClass().add(STYLE_CLASS_SHOWING);
                    } else {
                        getStyleClass().remove(STYLE_CLASS_SHOWING);
                    }
                }

                @Override
                public Object getBean() {
                    return Menu.this;
                }

                @Override
                public String getName() {
                    return "showing";
                }
            };
        }
        return showing;
    }


    // --- On Showing
    /**
     * Called just prior to the {@code ContextMenu} being shown, even if the menu has
     * no items to show. Note however that this won't be called if the menu does
     * not have a valid anchor node.
     * @return the on showing property
     */
    public final ObjectProperty<EventHandler<Event>> onShowingProperty() { return onShowing; }
    public final void setOnShowing(EventHandler<Event> value) { onShowingProperty().set(value); }
    public final EventHandler<Event> getOnShowing() { return onShowingProperty().get(); }
    private ObjectProperty<EventHandler<Event>> onShowing = new ObjectPropertyBase<EventHandler<Event>>() {
        @Override protected void invalidated() {
            eventHandlerManager.setEventHandler(ON_SHOWING, get());
         }

         @Override
         public Object getBean() {
             return Menu.this;
         }

         @Override
         public String getName() {
             return "onShowing";
         }
     };


    // -- On Shown
    /**
     * Called just after the {@link ContextMenu} is shown.
     * @return the on shown property
     */
    public final ObjectProperty<EventHandler<Event>> onShownProperty() { return onShown; }
    public final void setOnShown(EventHandler<Event> value) { onShownProperty().set(value); }
    public final EventHandler<Event> getOnShown() { return onShownProperty().get(); }
    private ObjectProperty<EventHandler<Event>> onShown = new ObjectPropertyBase<EventHandler<Event>>() {
        @Override protected void invalidated() {
            eventHandlerManager.setEventHandler(ON_SHOWN, get());
        }

        @Override
        public Object getBean() {
            return Menu.this;
        }

        @Override
        public String getName() {
            return "onShown";
        }
    };


    // --- On Hiding
    /**
     * Called just prior to the {@link ContextMenu} being hidden.
     * @return the on hiding property
     */
    public final ObjectProperty<EventHandler<Event>> onHidingProperty() { return onHiding; }
    public final void setOnHiding(EventHandler<Event> value) { onHidingProperty().set(value); }
    public final EventHandler<Event> getOnHiding() { return onHidingProperty().get(); }
    private ObjectProperty<EventHandler<Event>> onHiding = new ObjectPropertyBase<EventHandler<Event>>() {
        @Override protected void invalidated() {
            eventHandlerManager.setEventHandler(ON_HIDING, get());
        }

        @Override
        public Object getBean() {
            return Menu.this;
        }

        @Override
        public String getName() {
            return "onHiding";
        }
    };


    // --- On Hidden
    /**
     * Called just after the {@link ContextMenu} has been hidden.
     * @return the on hidden property
     */
    public final ObjectProperty<EventHandler<Event>> onHiddenProperty() { return onHidden; }
    public final void setOnHidden(EventHandler<Event> value) { onHiddenProperty().set(value); }
    public final EventHandler<Event> getOnHidden() { return onHiddenProperty().get(); }
    private ObjectProperty<EventHandler<Event>> onHidden = new ObjectPropertyBase<EventHandler<Event>>() {
        @Override protected void invalidated() {
            eventHandlerManager.setEventHandler(ON_HIDDEN, get());
        }

        @Override
        public Object getBean() {
            return Menu.this;
        }

        @Override
        public String getName() {
            return "onHidden";
        }
    };



    /***************************************************************************
     *                                                                         *
     * Instance variables                                                      *
     *                                                                         *
     **************************************************************************/

    private final ObservableList<MenuItem> items = new TrackableObservableList<MenuItem>() {
        @Override protected void onChanged(Change<MenuItem> c) {
            while (c.next()) {
                // remove the parent menu from all menu items that have been removed
                for (MenuItem item : c.getRemoved()) {
                    item.setParentMenu(null);
                    item.setParentPopup(null);
                }

                // set the parent menu to be this menu for all added menu items
                for (MenuItem item : c.getAddedSubList()) {
                    if (item.getParentMenu() != null) {
                        Logging.getControlsLogger().warning("Adding MenuItem " +
                                item.getText() + " that has already been added to "
                                + item.getParentMenu().getText());
                        item.getParentMenu().getItems().remove(item);
                    }

                    item.setParentMenu(Menu.this);
                    item.setParentPopup(getParentPopup());
                }
            }
            if (getItems().size() == 0 && isShowing()) {
                showingPropertyImpl().set(false);
            }
        }
    };



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * The items to show within this menu. If this ObservableList is modified at
     * runtime, the Menu will update as expected.
     * @return the list of items
     */
    public final ObservableList<MenuItem> getItems() {
        return items;
    }

    /**
     * If the Menu is not disabled and the {@link ContextMenu} is not already showing,
     * then this will cause the {@link ContextMenu} to be shown.
     */
    public void show() {
        if (isDisable()) return;
        setShowing(true);
    }

    /**
     * Hides the {@link ContextMenu} if it was previously showing, and any showing
     * submenus. If this menu is not showing, then invoking this function
     * has no effect.
     */
    public void hide() {
        if (!isShowing()) return;
        // hide all sub menus
        for (MenuItem i : getItems()) {
            if (i instanceof Menu) {
                final Menu m = (Menu) i;
                m.hide();
            }
        }
        setShowing(false);
    }

    /** {@inheritDoc} */
    @Override public <E extends Event> void addEventHandler(EventType<E> eventType, EventHandler<E> eventHandler) {
        eventHandlerManager.addEventHandler(eventType, eventHandler);
    }

    /** {@inheritDoc} */
    @Override public <E extends Event> void removeEventHandler(EventType<E> eventType, EventHandler<E> eventHandler) {
        eventHandlerManager.removeEventHandler(eventType, eventHandler);
    }

     /** {@inheritDoc} */
    @Override public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        return tail.prepend(eventHandlerManager);
    }

    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "menu";
    private static final String STYLE_CLASS_SHOWING = "showing";
}
