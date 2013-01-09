/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.css.PseudoClass;
import com.sun.javafx.scene.control.MultiplePropertyChangeListenerHandler;
import com.sun.javafx.scene.control.behavior.BehaviorBase;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.Control;
import javafx.scene.control.SkinBase;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

/**
 *
 */
public abstract class BehaviorSkinBase<C extends Control, BB extends BehaviorBase<C>> extends SkinBase<C> {
    
    /***************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    /**
     * The {@link BehaviorBase} that encapsulates the interaction with the
     * {@link Control} from this {@code Skin}. The {@code Skin} does not modify
     * the {@code Control} directly, but rather redirects events into the
     * {@code BehaviorBase} which then handles the events by modifying internal state
     * and public state in the {@code Control}. Generally, specific
     * {@code Skin} implementations will require specific {@code BehaviorBase}
     * implementations. For example, a ButtonSkin might require a ButtonBehavior.
     */
    private BB behavior;
    
    /**
     * This is part of the workaround introduced during delomboking. We probably will
     * want to adjust the way listeners are added rather than continuing to use this
     * map (although it doesn't really do much harm).
     */
    private MultiplePropertyChangeListenerHandler changeListenerHandler;
    
    
    
    /***************************************************************************
     *                                                                         *
     * Event Handlers / Listeners                                              *
     *                                                                         *
     **************************************************************************/
    
    
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
    
    /***************************************************************************
     *                                                                         *
     * Constructor                                                             *
     *                                                                         *
     **************************************************************************/

    /**
     * Constructor for all BehaviorSkinBase instances.
     * 
     * @param control The control for which this Skin should attach to.
     * @param behavior The behavior for which this Skin should defer to.
     */
    protected BehaviorSkinBase(final C control, final BB behavior) {
        super(control);
        
        if (behavior == null) {
            throw new IllegalArgumentException("Cannot pass null for behavior");
        }

        // Update the control and behavior
        this.behavior = behavior;
        
        // We will auto-add listeners for wiring up Region mouse events to
        // be sent to the behavior
        control.addEventHandler(MouseEvent.MOUSE_ENTERED, mouseHandler);
        control.addEventHandler(MouseEvent.MOUSE_EXITED, mouseHandler);
        control.addEventHandler(MouseEvent.MOUSE_PRESSED, mouseHandler);
        control.addEventHandler(MouseEvent.MOUSE_RELEASED, mouseHandler);
        control.addEventHandler(MouseEvent.MOUSE_DRAGGED, mouseHandler);
    }
    
    

    /***************************************************************************
     *                                                                         *
     * Public API (from Skin)                                                  *
     *                                                                         *
     **************************************************************************/    

    /** {@inheritDoc} */
    public final BB getBehavior() {
        return behavior;
    }

    /** {@inheritDoc} */
    @Override public void dispose() { 
        // unhook listeners
        if (changeListenerHandler != null) {
            changeListenerHandler.dispose();
        }
        
        C control = getSkinnable();
        control.removeEventHandler(MouseEvent.MOUSE_ENTERED, mouseHandler);
        control.removeEventHandler(MouseEvent.MOUSE_EXITED, mouseHandler);
        control.removeEventHandler(MouseEvent.MOUSE_PRESSED, mouseHandler);
        control.removeEventHandler(MouseEvent.MOUSE_RELEASED, mouseHandler);
        control.removeEventHandler(MouseEvent.MOUSE_DRAGGED, mouseHandler);

        this.behavior = null;
        
        super.dispose();
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/
    
    /**
     * Subclasses can invoke this method to register that we want to listen to
     * property change events for the given property.
     *
     * @param property
     * @param reference
     */
    protected final void registerChangeListener(ObservableValue property, String reference) {
        if (changeListenerHandler == null) {
            changeListenerHandler = new MultiplePropertyChangeListenerHandler(new Callback<String, Void>() {
                @Override public Void call(String p) {
                    handleControlPropertyChanged(p);
                    return null;
                }
            });
        }
        changeListenerHandler.registerChangeListener(property, reference);
    }
    
    /**
     * Skin subclasses will override this method to handle changes in corresponding
     * control's properties.
     */
    protected void handleControlPropertyChanged(String propertyReference) {
        // no-op
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Specialization of CSS handling code                                     *
     *                                                                         *
     **************************************************************************/

    /**
     * {@inheritDoc}
     */
    @Override public PseudoClass.States getPseudoClassStates() {
        // check if behaviour can be null...
        return getBehavior().getPseudoClassStates(); 
    }    
}
