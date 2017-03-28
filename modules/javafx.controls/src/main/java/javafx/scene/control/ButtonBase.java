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
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.AccessibleAction;
import javafx.scene.Node;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;

/**
 * Base class for button-like UI Controls, including Hyperlinks, Buttons,
 * ToggleButtons, CheckBoxes, and RadioButtons. The primary contribution of
 * ButtonBase is providing a consistent API for handling the concept of button
 * "arming". In UIs, a button will typically only "fire" if some user gesture
 * occurs while the button is "armed". For example, a Button may be armed if
 * the mouse is pressed and the Button is enabled and the mouse is over the
 * button. In such a situation, if the mouse is then released, then the Button
 * is "fired", meaning its action takes place.
 * @since JavaFX 2.0
 */

public abstract class ButtonBase extends Labeled {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Create a default ButtonBase with empty text.
     */
    public ButtonBase() { }

    /**
     * Create a ButtonBase with the given text.
     * @param text null text is treated as the empty string
     */
    public ButtonBase(String text) {
        super(text);
    }

    /**
     * Create a ButtonBase with the given text and graphic.
     * @param text null text is treated as the empty string
     * @param graphic a null graphic is acceptable
     */
    public ButtonBase(String text, Node graphic) {
        super(text, graphic);
    }


    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * Indicates that the button has been "armed" such that a mouse release
     * will cause the button's action to be invoked. This is subtly different
     * from pressed. Pressed indicates that the mouse has been
     * pressed on a Node and has not yet been released. {@code arm} however
     * also takes into account whether the mouse is actually over the
     * button and pressed.
     * @return the property to indicate that the button has been "armed"
     */
    public final ReadOnlyBooleanProperty armedProperty() { return armed.getReadOnlyProperty(); }
    private void setArmed(boolean value) { armed.set(value); }
    public final boolean isArmed() { return armedProperty().get(); }
    private ReadOnlyBooleanWrapper armed = new ReadOnlyBooleanWrapper() {
        @Override protected void invalidated() {
            pseudoClassStateChanged(ARMED_PSEUDOCLASS_STATE, get());
        }

        @Override
        public Object getBean() {
            return ButtonBase.this;
        }

        @Override
        public String getName() {
            return "armed";
        }
    };

    /**
     * The button's action, which is invoked whenever the button is fired. This
     * may be due to the user clicking on the button with the mouse, or by
     * a touch event, or by a key press, or if the developer programmatically
     * invokes the {@link #fire()} method.
     * @return the property to represent the button's action, which is invoked
     * whenever the button is fired
     */
    public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() { return onAction; }
    public final void setOnAction(EventHandler<ActionEvent> value) { onActionProperty().set(value); }
    public final EventHandler<ActionEvent> getOnAction() { return onActionProperty().get(); }
    private ObjectProperty<EventHandler<ActionEvent>> onAction = new ObjectPropertyBase<EventHandler<ActionEvent>>() {
        @Override protected void invalidated() {
            setEventHandler(ActionEvent.ACTION, get());
        }

        @Override
        public Object getBean() {
            return ButtonBase.this;
        }

        @Override
        public String getName() {
            return "onAction";
        }
    };


    /***************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /**
     * Arms the button. An armed button will fire an action (whether that be
     * the action of a {@link Button} or toggling selection on a
     * {@link CheckBox} or some other behavior) on the next expected UI
     * gesture.
     *
     * Note: This function is intended to be used by experts, primarily
     *       by those implementing new Skins or Behaviors. It is not common
     *       for developers or designers to access this function directly.
     */
    public void arm() {
        setArmed(true);
    }

    /**
     * Disarms the button. See {@link #arm()}.
     *
     * Note: This function is intended to be used by experts, primarily
     *       by those implementing new Skins or Behaviors. It is not common
     *       for developers or designers to access this function directly.
     */
    public void disarm() {
        setArmed(false);
    }

    /**
     * Invoked when a user gesture indicates that an event for this
     * {@code ButtonBase} should occur.
     * <p>
     * If invoked, this method will be executed regardless of the status of
     * {@link #arm}.
     * </p>
     */
    public abstract void fire();


    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final PseudoClass ARMED_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("armed");


    /***************************************************************************
     *                                                                         *
     * Accessibility handling                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
        switch (action) {
            case FIRE:
                fire();
                break;
            default: super.executeAccessibleAction(action);
        }
    }
}
