/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.scene.control.ControlAcceleratorSupport;
import javafx.collections.ObservableSet;
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
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.Node;

import javafx.css.Styleable;
import com.sun.javafx.event.EventHandlerManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javafx.beans.DefaultProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableMap;

/**
 * <p>Tabs are placed within a {@link TabPane}, where each tab represents a single
 * 'page'.</p>
 * <p>Tabs can contain any {@link Node} such as UI controls or groups
 * of nodes added to a layout container.</p>
 * <p>When the user clicks
 * on a Tab in the TabPane the Tab content becomes visible to the user.</p>
 * @since JavaFX 2.0
 */
@DefaultProperty("content")
@IDProperty("id")
public class Tab implements EventTarget, Styleable {

    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a tab with no title.
     */
    public Tab() {
        this(null);
    }

    /**
     * Creates a tab with a text title.
     *
     * @param text The title of the tab.
     */
    public Tab(String text) {
        this(text, null);
    }

    /**
     * Creates a tab with a text title and the specified content node.
     *
     * @param text The title of the tab.
     * @param content The content of the tab.
     * @since JavaFX 8u40
     */
    public Tab(String text, Node content) {
        setText(text);
        setContent(content);
        styleClass.addAll(DEFAULT_STYLE_CLASS);
    }


    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    private StringProperty id;

    /**
     * Sets the id of this tab. This simple string identifier is useful for
     * finding a specific Tab within the {@code TabPane}. The default value is {@code null}.
     * @param value the id of this tab
     */
   public final void setId(String value) { idProperty().set(value); }

    /**
     * The id of this tab.
     *
     * @return The id of the tab.
     */
    @Override
    public final String getId() { return id == null ? null : id.get(); }

    /**
     * The id of this tab.
     * @return the id property of this tab
     */
    public final StringProperty idProperty() {
        if (id == null) {
            id = new SimpleStringProperty(this, "id");
        }
        return id;
    }

    private StringProperty style;

    /**
     * A string representation of the CSS style associated with this
     * tab. This is analogous to the "style" attribute of an
     * HTML element. Note that, like the HTML style attribute, this
     * variable contains style properties and values and not the
     * selector portion of a style rule.
     * <p>
     * Parsing this style might not be supported on some limited
     * platforms. It is recommended to use a standalone CSS file instead.
     *
     * @param value the style string
     */
   public final void setStyle(String value) { styleProperty().set(value); }

    /**
     * The CSS style string associated to this tab.
     *
     * @return The CSS style string associated to this tab.
     */
    @Override
    public final String getStyle() { return style == null ? null : style.get(); }

    /**
     * The CSS style string associated to this tab.
     * @return the CSS style string property associated to this tab
     */
    public final StringProperty styleProperty() {
        if (style == null) {
            style = new SimpleStringProperty(this, "style");
        }
        return style;
    }

    private ReadOnlyBooleanWrapper selected;

    final void setSelected(boolean value) {
        selectedPropertyImpl().set(value);
    }

    /**
     * <p>Represents whether this tab is the currently selected tab,
     * To change the selected Tab use {@code tabPane.getSelectionModel().select()}
     * </p>
     * @return true if selected
     */
    public final boolean isSelected() {
        return selected == null ? false : selected.get();
    }

    /**
     * The currently selected tab.
     * @return the selected tab
     */
    public final ReadOnlyBooleanProperty selectedProperty() {
        return selectedPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyBooleanWrapper selectedPropertyImpl() {
        if (selected == null) {
            selected = new ReadOnlyBooleanWrapper() {
                @Override protected void invalidated() {
                    if (getOnSelectionChanged() != null) {
                        Event.fireEvent(Tab.this, new Event(SELECTION_CHANGED_EVENT));
                    }
                }

                @Override
                public Object getBean() {
                    return Tab.this;
                }

                @Override
                public String getName() {
                    return "selected";
                }
            };
        }
        return selected;
    }

    private ReadOnlyObjectWrapper<TabPane> tabPane;

    final void setTabPane(TabPane value) {
        tabPanePropertyImpl().set(value);
    }

    /**
     * <p>A reference to the TabPane that contains this tab instance.</p>
     * @return the TabPane
     */
    public final TabPane getTabPane() {
        return tabPane == null ? null : tabPane.get();
    }

    /**
     * The TabPane that contains this tab.
     * @return the TabPane property
     */
    public final ReadOnlyObjectProperty<TabPane> tabPaneProperty() {
        return tabPanePropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<TabPane> tabPanePropertyImpl() {
        if (tabPane == null) {
            tabPane = new ReadOnlyObjectWrapper<TabPane>(this, "tabPane") {
                private WeakReference<TabPane> oldParent;

                @Override protected void invalidated() {
                    if(oldParent != null && oldParent.get() != null) {
                        oldParent.get().disabledProperty().removeListener(parentDisabledChangedListener);
                    }
                    updateDisabled();
                    TabPane newParent = get();
                    if (newParent != null) {
                        newParent.disabledProperty().addListener(parentDisabledChangedListener);
                    }
                    oldParent = new WeakReference<TabPane>(newParent);
                    super.invalidated();
                }
            };
        }
        return tabPane;
    }

    private final InvalidationListener parentDisabledChangedListener = valueModel -> {
        updateDisabled();
    };

    private StringProperty text;

    /**
     * <p>Sets the text to show in the tab to allow the user to differentiate between
     * the function of each tab. The text is always visible
     * </p>
     * @param value the text string
     */
    public final void setText(String value) {
        textProperty().set(value);
    }

    /**
     * The text shown in the tab.
     *
     * @return The text shown in the tab.
     */
    public final String getText() {
        return text == null ? null : text.get();
    }

    /**
     * The text shown in the tab.
     * @return the text property
     */
    public final StringProperty textProperty() {
        if (text == null) {
            text = new SimpleStringProperty(this, "text");
        }
        return text;
    }

    private ObjectProperty<Node> graphic;

    /**
     * <p>Sets the graphic to show in the tab to allow the user to differentiate
     * between the function of each tab. By default the graphic does not rotate
     * based on the TabPane.tabPosition value, but it can be set to rotate by
     * setting TabPane.rotateGraphic to true.</p>
     * @param value the graphic node
     */
    public final void setGraphic(Node value) {
        graphicProperty().set(value);
    }

    /**
     * The graphic shown in the tab.
     *
     * @return The graphic shown in the tab.
     */
    public final Node getGraphic() {
        return graphic == null ? null : graphic.get();
    }

    /**
     * The graphic in the tab.
     *
     * @return The graphic in the tab.
     */
    public final ObjectProperty<Node> graphicProperty() {
        if (graphic == null) {
            graphic = new SimpleObjectProperty<Node>(this, "graphic");
        }
        return graphic;
    }

    private ObjectProperty<Node> content;

    /**
     * <p>The content to show within the main TabPane area. The content
     * can be any Node such as UI controls or groups of nodes added
     * to a layout container.</p>
     * @param value the content node
     */
    public final void setContent(Node value) {
        contentProperty().set(value);
    }

    /**
     * <p>The content associated with the tab.</p>
     *
     * @return The content associated with the tab.
     */
    public final Node getContent() {
        return content == null ? null : content.get();
    }

    /**
     * <p>The content associated with the tab.</p>
     * @return the content property
     */
    public final ObjectProperty<Node> contentProperty() {
        if (content == null) {
            content = new SimpleObjectProperty<Node>(this, "content") {
                @Override protected void invalidated() {
                    updateDisabled();
                }
            };
        }
        return content;
    }


    private ObjectProperty<ContextMenu> contextMenu;

    /**
     * <p>Specifies the context menu to show when the user right-clicks on the tab.
     * </p>
     * @param value the context menu
     */
    public final void setContextMenu(ContextMenu value) {
        contextMenuProperty().set(value);
    }

    /**
     * The context menu associated with the tab.
     * @return The context menu associated with the tab.
     */
    public final ContextMenu getContextMenu() {
        return contextMenu == null ? null : contextMenu.get();
    }

    /**
     * The context menu associated with the tab.
     * @return the context menu property
     */
    public final ObjectProperty<ContextMenu> contextMenuProperty() {
        if (contextMenu == null) {
            contextMenu = new SimpleObjectProperty<ContextMenu>(this, "contextMenu") {
                private WeakReference<ContextMenu> contextMenuRef;

                @Override protected void invalidated() {
                    ContextMenu oldMenu = contextMenuRef == null ? null : contextMenuRef.get();
                    if (oldMenu != null) {
                        ControlAcceleratorSupport.removeAcceleratorsFromScene(oldMenu.getItems(), Tab.this);
                    }

                    ContextMenu ctx = get();
                    contextMenuRef = new WeakReference<>(ctx);

                    if (ctx != null) {
                        // if a context menu is set, we need to install any accelerators
                        // belonging to its menu items ASAP into the scene that this
                        // Control is in (if the control is not in a Scene, we will need
                        // to wait until it is and then do it).
                        ControlAcceleratorSupport.addAcceleratorsIntoScene(ctx.getItems(), Tab.this);
                    }
                }
            };
        }
        return contextMenu;
    }

    private BooleanProperty closable;

    /**
     * <p>Sets {@code true} if the tab is closable.  If this is set to {@code false},
     * then regardless of the TabClosingPolicy, it will not be
     * possible for the user to close this tab. Therefore, when this
     * property is {@code false}, no 'close' button will be shown on the tab.
     * The default is {@code true}.</p>
     *
     * @param value the closable value
     */
    public final void setClosable(boolean value) {
        closableProperty().set(value);
    }

    /**
     * Returns {@code true} if this tab is closable.
     *
     * @return {@code true} if the tab is closable.
     */
    public final boolean isClosable() {
        return closable == null ? true : closable.get();
    }

    /**
     * The closable state for this tab.
     * @return the closable property
     */
    public final BooleanProperty closableProperty() {
        if (closable == null) {
            closable = new SimpleBooleanProperty(this, "closable", true);
        }
        return closable;
    }


    /**
     * <p>Called when the tab becomes selected or unselected.</p>
     */
    public static final EventType<Event> SELECTION_CHANGED_EVENT =
            new EventType<Event> (Event.ANY, "SELECTION_CHANGED_EVENT");
    private ObjectProperty<EventHandler<Event>> onSelectionChanged;

    /**
     * Defines a function to be called when a selection changed has occurred on the tab.
     * @param value the on selection changed event handler
     */
    public final void setOnSelectionChanged(EventHandler<Event> value) {
        onSelectionChangedProperty().set(value);
    }

    /**
     * The event handler that is associated with a selection on the tab.
     *
     * @return The event handler that is associated with a tab selection.
     */
    public final EventHandler<Event> getOnSelectionChanged() {
        return onSelectionChanged == null ? null : onSelectionChanged.get();
    }

    /**
     * The event handler that is associated with a selection on the tab.
     * @return the on selection changed event handler property
     */
    public final ObjectProperty<EventHandler<Event>> onSelectionChangedProperty() {
        if (onSelectionChanged == null) {
            onSelectionChanged = new ObjectPropertyBase<EventHandler<Event>>() {
                @Override protected void invalidated() {
                    setEventHandler(SELECTION_CHANGED_EVENT, get());
                }

                @Override
                public Object getBean() {
                    return Tab.this;
                }

                @Override
                public String getName() {
                    return "onSelectionChanged";
                }
            };
        }
        return onSelectionChanged;
    }

    /**
     * <p>Called when a user closes this tab. This is useful for freeing up memory.</p>
     */
    public static final EventType<Event> CLOSED_EVENT = new EventType<Event>(Event.ANY, "TAB_CLOSED");
    private ObjectProperty<EventHandler<Event>> onClosed;

    /**
     * Defines a function to be called when the tab is closed.
     * @param value the on closed event handler
     */
    public final void setOnClosed(EventHandler<Event> value) {
        onClosedProperty().set(value);
    }

    /**
     * The event handler that is associated with the tab when the tab is closed.
     *
     * @return The event handler that is associated with the tab when the tab is closed.
     */
    public final EventHandler<Event> getOnClosed() {
        return onClosed == null ? null : onClosed.get();
    }

    /**
     * The event handler that is associated with the tab when the tab is closed.
     * @return the on closed event handler property
     */
    public final ObjectProperty<EventHandler<Event>> onClosedProperty() {
        if (onClosed == null) {
            onClosed = new ObjectPropertyBase<EventHandler<Event>>() {
                @Override protected void invalidated() {
                    setEventHandler(CLOSED_EVENT, get());
                }

                @Override
                public Object getBean() {
                    return Tab.this;
                }

                @Override
                public String getName() {
                    return "onClosed";
                }
            };
        }
        return onClosed;
    }

    private ObjectProperty<Tooltip> tooltip;

    /**
     * <p>Specifies the tooltip to show when the user hovers over the tab.</p>
     * @param value the tool tip value
     */
    public final void setTooltip(Tooltip value) { tooltipProperty().setValue(value); }

    /**
     * The tooltip associated with this tab.
     * @return The tooltip associated with this tab.
     */
    public final Tooltip getTooltip() { return tooltip == null ? null : tooltip.getValue(); }

    /**
     * The tooltip associated with this tab.
     * @return the tool tip property
     */
    public final ObjectProperty<Tooltip> tooltipProperty() {
        if (tooltip == null) {
            tooltip = new SimpleObjectProperty<Tooltip>(this, "tooltip");
        }
        return tooltip;
    }

    private final ObservableList<String> styleClass = FXCollections.observableArrayList();

    private BooleanProperty disable;

    /**
     * Sets the disabled state of this tab.
     *
     * @param value the state to set this tab
     *
     * @defaultValue false
     * @since JavaFX 2.2
     */
    public final void setDisable(boolean value) {
        disableProperty().set(value);
    }

    /**
     * Returns {@code true} if this tab is disable.
     * @return true if this tab is disable
     * @since JavaFX 2.2
     */
    public final boolean isDisable() { return disable == null ? false : disable.get(); }

    /**
     * Sets the disabled state of this tab. A disable tab is no longer interactive
     * or traversable, but the contents remain interactive.  A disable tab
     * can be selected using {@link TabPane#getSelectionModel()}.
     *
     * @return the disable property
     * @defaultValue false
     * @since JavaFX 2.2
     */
    public final BooleanProperty disableProperty() {
        if (disable == null) {
            disable = new BooleanPropertyBase(false) {
                @Override
                protected void invalidated() {
                    updateDisabled();
                }

                @Override
                public Object getBean() {
                    return Tab.this;
                }

                @Override
                public String getName() {
                    return "disable";
                }
            };
        }
        return disable;
    }

    private ReadOnlyBooleanWrapper disabled;

    private final void setDisabled(boolean value) {
        disabledPropertyImpl().set(value);
    }

    /**
     * Returns true when the {@code Tab} {@link #disableProperty disable} is set to
     * {@code true} or if the {@code TabPane} is disabled.
     * @return true if the TabPane is disabled
     * @since JavaFX 2.2
     */
    public final boolean isDisabled() {
        return disabled == null ? false : disabled.get();
    }

    /**
     * Indicates whether or not this {@code Tab} is disabled.  A {@code Tab}
     * will become disabled if {@link #disableProperty disable} is set to {@code true} on either
     * itself or if the {@code TabPane} is disabled.
     *
     * @return the disabled property
     * @defaultValue false
     * @since JavaFX 2.2
     */
    public final ReadOnlyBooleanProperty disabledProperty() {
        return disabledPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyBooleanWrapper disabledPropertyImpl() {
        if (disabled == null) {
            disabled = new ReadOnlyBooleanWrapper() {
                @Override
                public Object getBean() {
                    return Tab.this;
                }

                @Override
                public String getName() {
                    return "disabled";
                }
            };
        }
        return disabled;
    }

    private void updateDisabled() {
        boolean disabled = isDisable() || (getTabPane() != null && getTabPane().isDisabled());
        setDisabled(disabled);

        // Fix for RT-24658 - content should be disabled if the tab is disabled
        Node content = getContent();
        if (content != null) {
            content.setDisable(disabled);
        }
    }

     /**
     * Called when there is an external request to close this {@code Tab}.
     * The installed event handler can prevent tab closing by consuming the
     * received event.
     * @since JavaFX 8.0
     */
    public static final EventType<Event> TAB_CLOSE_REQUEST_EVENT = new EventType<Event> (Event.ANY, "TAB_CLOSE_REQUEST_EVENT");

    /**
     * Called when there is an external request to close this {@code Tab}.
     * The installed event handler can prevent tab closing by consuming the
     * received event.
     * @since JavaFX 8.0
     */
    private ObjectProperty<EventHandler<Event>> onCloseRequest;
    public final ObjectProperty<EventHandler<Event>> onCloseRequestProperty() {
        if (onCloseRequest == null) {
            onCloseRequest = new ObjectPropertyBase<EventHandler<Event>>() {
                @Override protected void invalidated() {
                    setEventHandler(TAB_CLOSE_REQUEST_EVENT, get());
                }

                @Override public Object getBean() {
                    return Tab.this;
                }

                @Override public String getName() {
                    return "onCloseRequest";
                }
            };
        }
        return onCloseRequest;
    }

    public EventHandler<Event> getOnCloseRequest() {
        if( onCloseRequest == null ) {
            return null;
        }
        return onCloseRequest.get();
    }

    public void setOnCloseRequest(EventHandler<Event> value) {
        onCloseRequestProperty().set(value);
    }


    // --- Properties
    private static final Object USER_DATA_KEY = new Object();

    // A map containing a set of properties for this Tab
    private ObservableMap<Object, Object> properties;

    /**
      * Returns an observable map of properties on this Tab for use primarily
      * by application developers.
      *
      * @return an observable map of properties on this Tab for use primarily
      * by application developers
     * @since JavaFX 2.2
     */
     public final ObservableMap<Object, Object> getProperties() {
        if (properties == null) {
            properties = FXCollections.observableMap(new HashMap<Object, Object>());
        }
        return properties;
    }

    /**
     * Tests if this Tab has properties.
     * @return true if this tab has properties.
     * @since JavaFX 2.2
     */
     public boolean hasProperties() {
        return properties != null && !properties.isEmpty();
    }


    // --- UserData
    /**
     * Convenience method for setting a single Object property that can be
     * retrieved at a later date. This is functionally equivalent to calling
     * the getProperties().put(Object key, Object value) method. This can later
     * be retrieved by calling {@link Tab#getUserData()}.
     *
     * @param value The value to be stored - this can later be retrieved by calling
     *          {@link Tab#getUserData()}.
     * @since JavaFX 2.2
     */
    public void setUserData(Object value) {
        getProperties().put(USER_DATA_KEY, value);
    }

    /**
     * Returns a previously set Object property, or null if no such property
     * has been set using the {@link Tab#setUserData(java.lang.Object)} method.
     *
     * @return The Object that was previously set, or null if no property
     *          has been set or if null was set.
     * @since JavaFX 2.2
     */
    public Object getUserData() {
        return getProperties().get(USER_DATA_KEY);
    }

    /**
     * A list of String identifiers which can be used to logically group
     * Nodes, specifically for an external style engine. This variable is
     * analogous to the "class" attribute on an HTML element and, as such,
     * each element of the list is a style class to which this Node belongs.
     *
     * @see <a href="http://www.w3.org/TR/css3-selectors/#class-html">CSS3 class selectors</a>
     */
    @Override
    public ObservableList<String> getStyleClass() {
        return styleClass;
    }

    private final EventHandlerManager eventHandlerManager =
            new EventHandlerManager(this);

     /** {@inheritDoc} */
    @Override
    public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        return tail.prepend(eventHandlerManager);
    }

    <E extends Event> void setEventHandler(EventType<E> eventType, EventHandler<E> eventHandler) {
        eventHandlerManager.setEventHandler(eventType, eventHandler);
    }

    /*
     * See Node#lookup(String)
     */
    Node lookup(String selector) {
        if (selector == null) return null;
        Node n = null;
        if (getContent() != null) {
            n = getContent().lookup(selector);
        }
        if (n == null && getGraphic() != null) {
            n = getGraphic().lookup(selector);
        }
        return n;
    }

    /*
     * See Node#lookupAll(String)
     */
    List<Node> lookupAll(String selector) {
        final List<Node> results = new ArrayList<>();
        if (getContent() != null) {
            Set set = getContent().lookupAll(selector);
            if (!set.isEmpty()) {
                results.addAll(set);
            }
        }
        if (getGraphic() != null) {
            Set set = getGraphic().lookupAll(selector);
            if (!set.isEmpty()) {
                results.addAll(set);
            }
        }
        return results;
    }


    /* *************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "tab";

    /**
     * {@inheritDoc}
     * @return "Tab"
     * @since JavaFX 8.0
     */
   @Override
    public String getTypeSelector() {
        return "Tab";
    }

    /**
     * {@inheritDoc}
     * @return {@code getTabPane()}
     * @since JavaFX 8.0
     */
    @Override
    public Styleable getStyleableParent() {
        return getTabPane();
    }

    /**
     * {@inheritDoc}
     * @since JavaFX 8.0
     */
    public final ObservableSet<PseudoClass> getPseudoClassStates() {
        return FXCollections.emptyObservableSet();
    }

    /**
     * {@inheritDoc}
     * @since JavaFX 8.0
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    /**
     * Gets the {@code CssMetaData} associated with this class, which may include the
     * {@code CssMetaData} of its superclasses.
     * @return the {@code CssMetaData}
     * @since JavaFX 8.0
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return Collections.emptyList();
    }
}
