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

package com.sun.javafx.scene.control.skin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.scene.control.WeakListChangeListener;
import com.sun.javafx.scene.control.behavior.BehaviorBase;
import javafx.collections.ListChangeListener;

/**
 * The default base class from which all of the CSS-based skins extend. This
 * class is a Region based Skin. As a Region, it is a Container which gives
 * easy and convenient access to the layout machinery, and also since it is a
 * Parent it can have children directly, often removing extra unnecessary
 * levels of nesting.
 */
public abstract class SkinBase<C extends Control, B extends BehaviorBase<C>> extends StackPane implements Skin<C> {
    /**
     * The {@code Control} that is referencing this Skin. There is a
     * one-to-one relationship between a {@code Skin} and a {@code Control}.
     * When a {@code Skin} is set on a {@code Control}, this variable is
     * automatically updated.
     *
     * @profile common
     */
    private C control;

    /**
     * The {@link BehaviorBase} that encapsulates the interaction with the
     * {@link Control} from this {@code Skin}. The {@code Skin} does not modify
     * the {@code Control} directly, but rather redirects events into the
     * {@code BehaviorBase} which then handles the events by modifying internal state
     * and public state in the {@code Control}. Generally, specific
     * {@code Skin} implementations will require specific {@code BehaviorBase}
     * implementations. For example, a ButtonSkin might require a ButtonBehavior.
     */
    private B behavior;

    /**
     * Mouse handler used for consuming all mouse events (preventing them
     * from bubbling up to parent)
     */
    private static final EventHandler<MouseEvent> mouseEventConsumer = new EventHandler<MouseEvent>() {
        @Override public void handle(MouseEvent event) {
            /*
            ** we used to consume mouse wheel rotations here, 
            ** be we've switched to ScrollEvents, and only consume those which we use.
            ** See RT-13995 & RT-14480
            */
            event.consume();
        }
    };

    /**
     * Determines whether all mouse events should be automatically consumed.
     */
    protected final void consumeMouseEvents(boolean value) {
        if (value) {
            if (control != null) {
                control.addEventHandler(MouseEvent.ANY, mouseEventConsumer);
            }
        } else {
            if (control != null) {
                control.removeEventHandler(MouseEvent.ANY, mouseEventConsumer);
            }
        }
    }

    /***************************************************************************
     * Implementation of the Skin interface                                    *
     **************************************************************************/

    @Override public C getSkinnable() {
        return control;
    }

    @Override public final Node getNode() {
        return this;
    }

    /**
     * If you override this, you must call super.dispose().
     */
    @Override public void dispose() {
        // unhook listeners
        for (ObservableValue value : propertyReferenceMap.keySet()) {
            value.removeListener(controlPropertyChangedListener);
        }

        this.removeEventHandler(MouseEvent.MOUSE_ENTERED, mouseHandler);
        this.removeEventHandler(MouseEvent.MOUSE_EXITED, mouseHandler);
        this.removeEventHandler(MouseEvent.MOUSE_PRESSED, mouseHandler);
        this.removeEventHandler(MouseEvent.MOUSE_RELEASED, mouseHandler);
        this.removeEventHandler(MouseEvent.MOUSE_DRAGGED, mouseHandler);
        
        control.removeEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, contextMenuHandler);

        this.control = null;
        this.behavior = null;
    }

    public SkinBase(final C control, final B behavior) {
        if (control == null || behavior == null) {
            throw new IllegalArgumentException("Cannot pass null for control or behavior");
        }

        // Update the control and behavior
        this.control = control;
        this.behavior = behavior;

        // RT-7512 - skin needs to have styleClass of the control
        getStyleClass().setAll(control.getStyleClass());
        setStyle(control.getStyle());
        setId(control.getId());

        // RT-16368 - keep this skin's styleclass in sync with its control.
       control.getStyleClass().addListener(new WeakListChangeListener(styleClassChangeListener));

        // RT-16368 - keep this skin's style property in sync with its control.
       registerChangeListener(control.styleProperty(), STYLE_PROPERTY_REF);

        // RT-16368 - keep this skin's style property in sync with its control.
       registerChangeListener(control.idProperty(), ID_PROPERTY_REF);

        // We will auto-add listeners for wiring up Region mouse events to
        // be sent to the behavior
        this.addEventHandler(MouseEvent.MOUSE_ENTERED, mouseHandler);
        this.addEventHandler(MouseEvent.MOUSE_EXITED, mouseHandler);
        this.addEventHandler(MouseEvent.MOUSE_PRESSED, mouseHandler);
        this.addEventHandler(MouseEvent.MOUSE_RELEASED, mouseHandler);
        this.addEventHandler(MouseEvent.MOUSE_DRAGGED, mouseHandler);
        
        // we add a listener for menu request events to show the context menu
        // that may be set on the Control
        control.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, contextMenuHandler);

        // Default behavior for controls is to consume all mouse events
        consumeMouseEvents(true);
    }

    public B getBehavior() {
        return behavior;
    }

    public ContextMenu getContextMenu() {
        return getSkinnable().getContextMenu();
    }
    
    /**
     * Called for key events with no specific mouse location.
     *
     * Subclasses override this to decide location and call show the context menu.
     */
    public boolean showContextMenu(ContextMenu menu, double x, double y, boolean isKeyboardTrigger) {
        if (menu != null) {
            menu.show(control, x, y);
            return true;
        }
        return false;
    }


    /***************************************************************************
     * Specialization of the Region                                            *
     **************************************************************************/


    /***************************************************************************
     * Specialization of CSS handling code                                     *
     **************************************************************************/

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated @Override public long impl_getPseudoClassState() {
        return getSkinnable().impl_getPseudoClassState();
    }

    private static class StyleableProperties {

        private static final List<StyleableProperty> STYLEABLES;

        static {
            // We do this as we only want to take on the regions own keys,
            // none of container's or Node's. This is because we share
            // css with the Control and it would mean all of those
            // properties like opacity would be applied twice.
            final List<StyleableProperty> stackPaneKeys = StackPane.impl_CSS_STYLEABLES();
            final List<StyleableProperty> parentKeys = Parent.impl_CSS_STYLEABLES();
            final List<StyleableProperty> temp = new ArrayList<StyleableProperty>();
            
            final int offset = parentKeys.size();
            for(int n=0, max=stackPaneKeys.size() - offset; n<max; n++) {
                temp.add(stackPaneKeys.get(n + offset));
            }
            STYLEABLES = Collections.unmodifiableList(temp);
        }

    }

     public static List<StyleableProperty> impl_CSS_STYLEABLES() {
         return SkinBase.StyleableProperties.STYLEABLES;
     }

    /**
     * RT-19263
     * @treatAsPrivate implementation detail
     * @deprecated This is an experimental API that is not intended for general use and is subject to change in future versions
     */
    @Deprecated
    public List<StyleableProperty> impl_getStyleableProperties() {
        return impl_CSS_STYLEABLES();
    }

    /***************************************************************************
     * Event handling mumbo jumbo                                              *
     **************************************************************************/
    private final ListChangeListener styleClassChangeListener = 
        new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> c) {

                while (c.next()) {
                    // Don't setAll here since the skin might have added
                    // a styleclass of its own and we want to retain that.
                    if (c.wasRemoved()) {
                        SkinBase.this.getStyleClass().removeAll(c.getRemoved());
                    }
                    if (c.wasAdded()) {
                        SkinBase.this.getStyleClass().addAll(c.getAddedSubList());
                    }
                }
            }
        };

    private final ChangeListener controlPropertyChangedListener = new ChangeListener() {
        @Override public void changed(ObservableValue property, Object oldValue, Object newValue) {
            handleControlPropertyChanged(propertyReferenceMap.get(property));
        }
    };

    /**
     * This is part of the workaround introduced during delomboking. We probably will
     * want to adjust the way listeners are added rather than continuing to use this
     * map (although it doesn't really do much harm).
     */
    private Map<ObservableValue,String> propertyReferenceMap =
            new HashMap<ObservableValue,String>();

    /**
     * Subclasses can invoke this method to register that we want to listen to
     * property change events for the given property.
     *
     * @param property
     * @param reference
     */
    protected final void registerChangeListener(ObservableValue property, String reference) {
        if (!propertyReferenceMap.containsKey(property)) {
            propertyReferenceMap.put(property, reference);
            property.addListener(new WeakChangeListener(controlPropertyChangedListener));
        }
    }

    final static private String STYLE_PROPERTY_REF = "STYLE_PROPERTY_REF";
    final static private String ID_PROPERTY_REF    = "ID_PROPERTY_REF";
    /**
     * Skin subclasses will override this method to handle changes in corresponding
     * control's properties.
     */
    protected void handleControlPropertyChanged(String propertyReference) {
        if (STYLE_PROPERTY_REF.equals(propertyReference)) {
            setStyle(getSkinnable().getStyle());
        } else if (ID_PROPERTY_REF.equals(propertyReference)) {
            setId(getSkinnable().getId());
        }
    }

    /**
     * Forwards mouse events received by a MouseListener to the behavior.
     * Note that we use this pattern to remove some of the anonymous inner
     * classes which we'd otherwise have to create. When lambda expressions
     * are supported, we could do it that way instead (or use MethodHandles).
     */
    private final EventHandler<MouseEvent> mouseHandler =
            new EventHandler<MouseEvent>() {
        @Override public void handle(MouseEvent e) {
            final EventType<?> type = e.getEventType();

            if (type == MouseEvent.MOUSE_ENTERED) behavior.mouseEntered(e);
            else if (type == MouseEvent.MOUSE_EXITED) behavior.mouseExited(e);
            else if (type == MouseEvent.MOUSE_PRESSED) behavior.mousePressed(e);
            else if (type == MouseEvent.MOUSE_RELEASED) behavior.mouseReleased(e);
            else if (type == MouseEvent.MOUSE_DRAGGED) behavior.mouseDragged(e);
            else { // no op
                throw new AssertionError("Unsupported event type received");
            }
        }
    };
    
    /**
     * Handles conext menu requests by popping up the menu.
     * Note that we use this pattern to remove some of the anonymous inner
     * classes which we'd otherwise have to create. When lambda expressions
     * are supported, we could do it that way instead (or use MethodHandles).
     */
    private final EventHandler<ContextMenuEvent> contextMenuHandler = new EventHandler<ContextMenuEvent>() {
        @Override public void handle(ContextMenuEvent event) {
            // If a context menu was shown, consume the event to prevent multiple context menus
            if (showContextMenu(getContextMenu (), event.getScreenX(), event.getScreenY(), event.isKeyboardTrigger())) {
                event.consume();
            }
        }
    };
}
