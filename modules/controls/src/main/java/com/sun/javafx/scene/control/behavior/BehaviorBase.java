/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.behavior;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.EventHandler;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.sun.javafx.scene.traversal.Direction;
import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.TAB;
import static javafx.scene.input.KeyCode.UP;

/**
 * A convenient base class from which all our built-in behaviors extend. The
 * main functionality in BehaviorBase revolves around infrastructure for
 * resolving key events into function calls. The differences between platforms
 * can be subtle, and we attempt to build sufficient infrastructure into
 * BehaviorBase to minimize the amount of code and the complexity of code
 * necessary to support multiple platforms sufficiently well.
 *
 * <p>Although BehaviorBase is typically used as a base class, it is not abstract and
 * several skins instantiate an instance of BehaviorBase directly.</p>
 *
 * <p>BehaviorBase also implements the hooks for focus traversal. This
 * implementation is sufficient for most subclasses of BehaviorBase. The
 * following action names are registered in the keyMap for handling focus
 * traversal. Subclasses which need to invoke focus traversal using non-standard
 * key strokes should map key strokes to these action names:</p>
 * <ul>
 *  <li>TraverseUp</li>
 *  <li>TraverseDown</li>
 *  <li>TraverseLeft</li>
 *  <li>TraverseRight</li>
 *  <li>TraverseNext</li>
 *  <li>TraversePrevious</li>
 * </ul>
 *
 * <p>Note that by convention, action names are camel case with the first letter
 * uppercase, matching class naming conventions.</p>
 */
public class BehaviorBase<C extends Control> {
    /**
     * A static final reference to whether the platform we are on supports touch.
     */
    protected final static boolean IS_TOUCH_SUPPORTED = Platform.isSupported(ConditionalFeature.INPUT_TOUCH);

    /**
     * The default key bindings for focus traversal. For many behavior
     * implementations, you may be able to use this directly. The built in names
     * for these traversal actions are:
     * <ul>
     *  <li>TraverseUp</li>
     *  <li>TraverseDown</li>
     *  <li>TraverseLeft</li>
     *  <li>TraverseRight</li>
     *  <li>TraverseNext</li>
     *  <li>TraversePrevious</li>
     * </ul>
     */
    protected static final List<KeyBinding> TRAVERSAL_BINDINGS = new ArrayList<>();
    static final String TRAVERSE_UP = "TraverseUp";
    static final String TRAVERSE_DOWN = "TraverseDown";
    static final String TRAVERSE_LEFT = "TraverseLeft";
    static final String TRAVERSE_RIGHT = "TraverseRight";
    static final String TRAVERSE_NEXT = "TraverseNext";
    static final String TRAVERSE_PREVIOUS = "TraversePrevious";

    static {
        TRAVERSAL_BINDINGS.add(new KeyBinding(UP, TRAVERSE_UP));
        TRAVERSAL_BINDINGS.add(new KeyBinding(DOWN, TRAVERSE_DOWN));
        TRAVERSAL_BINDINGS.add(new KeyBinding(LEFT, TRAVERSE_LEFT));
        TRAVERSAL_BINDINGS.add(new KeyBinding(RIGHT, TRAVERSE_RIGHT));
        TRAVERSAL_BINDINGS.add(new KeyBinding(TAB, TRAVERSE_NEXT));
        TRAVERSAL_BINDINGS.add(new KeyBinding(TAB, TRAVERSE_PREVIOUS).shift());

        TRAVERSAL_BINDINGS.add(new KeyBinding(UP, TRAVERSE_UP).shift().alt().ctrl());
        TRAVERSAL_BINDINGS.add(new KeyBinding(DOWN, TRAVERSE_DOWN).shift().alt().ctrl());
        TRAVERSAL_BINDINGS.add(new KeyBinding(LEFT, TRAVERSE_LEFT).shift().alt().ctrl());
        TRAVERSAL_BINDINGS.add(new KeyBinding(RIGHT, TRAVERSE_RIGHT).shift().alt().ctrl());
        TRAVERSAL_BINDINGS.add(new KeyBinding(TAB, TRAVERSE_NEXT).shift().alt().ctrl());
        TRAVERSAL_BINDINGS.add(new KeyBinding(TAB, TRAVERSE_PREVIOUS).alt().ctrl());
    }

    /**
     * The Control with which this Behavior is used. This must be specified in
     * the constructor and must not be null.
     */
    private final C control;

    /**
     * The key bindings for this Behavior.
     */
    private final List<KeyBinding> keyBindings;

    /**
     * Listens to any key events on the Control and responds to them
     */
    private final EventHandler<KeyEvent> keyEventListener = new EventHandler<KeyEvent>() {
        @Override public void handle(KeyEvent e) {
            if (!e.isConsumed()) {
                callActionForEvent(e);
            }
        }
    };

    /**
     * Listens to any focus events on the Control and calls protected methods as a result
     */
    private final InvalidationListener focusListener = new InvalidationListener() {
        @Override public void invalidated(Observable property) {
            focusChanged();
        }
    };

    /**
     * Create a new BehaviorBase for the given control. The Control must not
     * be null.
     *
     * @param control The control. Must not be null.
     * @param keyBindings The key bindings that should be used with this behavior.
     *                    Null is treated as an empty list.
     */
    public BehaviorBase(final C control, final List<KeyBinding> keyBindings) {
        // Don't need to explicitly check for null because Collections.unmodifiableList
        // will die on null, as will the adding of listeners
        this.control = control;
        this.keyBindings = keyBindings == null ? Collections.EMPTY_LIST
                : Collections.unmodifiableList(new ArrayList<>(keyBindings));
        control.addEventHandler(KeyEvent.ANY, keyEventListener);
        control.focusedProperty().addListener(focusListener);
    }

    /**
     * Called by a Skin when the Skin is disposed. This method
     * allows a Behavior to implement any logic necessary to clean up itself after
     * the Behavior is no longer needed. Calling dispose twice has no effect. This
     * method is intended to be overridden by subclasses, although all subclasses
     * must call super.dispose() or a potential memory leak will result.
     */
    public void dispose() {
        control.removeEventHandler(KeyEvent.ANY, keyEventListener);
        control.focusedProperty().removeListener(focusListener);
    }

    /***************************************************************************
     * Implementation of the Behavior "interface"                              *
     *                                                                         *
     * One of the specialized duties of the behavior is to react to key        *
     * events. The behavior breaks the handling of a key event down into a few *
     * distinct stages. First, the BehaviorBase will analyze the key event and *
     * find the String name of a matching action to invoke, if any. If an      *
     * action exists for this event, the name is then fed to                   *
     * callActionForEvent(name), which will then invoke an actual method on    *
     * the behavior that is the implementation of that action.                 *
     *                                                                         *
     * The reason for returning the intermediate action name as a String is    *
     * twofold. First, the matching is done by analyzing a set of key bindings *
     * which are *statically declared* on each behavior class. The fact that   *
     * they are static means that we cannot refer to an actual instance method *
     * to invoke (such as with lambda's). It is also important that these are  *
     * static to reduce the memory footprint of a control (since having        *
     * per-instance key bindings would add a lot to memory footprint). We      *
     * could have used something other than String as the intermediate token,  *
     * however String is useful if we ever want to expose to developers a way  *
     * to alter the action map from a property file or XML file etc.           *
     *                                                                         *
     **************************************************************************/

    /**
     * Gets the control associated with this behavior. Even after the BehaviorBase is
     * disposed, this reference will be non-null.
     *
     * @return The control for this Behavior.
     */
    public final C getControl() { return control; }

    /**
     * Invokes the appropriate action for this key event. This is the main entry point where
     * key events are passed when they occur. This method is responsible for invoking
     * matchActionForEvent, callAction, and consuming the event if it was handled by this control.
     *
     * @param e The key event. Must not be null.
     */
    protected void callActionForEvent(KeyEvent e) {
        String action = matchActionForEvent(e);
        if (action != null) {
            callAction(action);
            e.consume();
        }
    }

    /**
     * Given a key event, this method will find the matching action name, or null if there
     * is not one.
     *
     * @param e The key event. Must not be null.
     * @return The name of the action to invoke, or null if there is not one.
     */
    protected String matchActionForEvent(final KeyEvent e) {
        if (e == null) throw new NullPointerException("KeyEvent must not be null");
        KeyBinding match = null;
        int specificity = 0;
        int maxBindings = keyBindings.size();
        for (int i = 0; i < maxBindings; i++) {
            KeyBinding binding = keyBindings.get(i);
            int s = binding.getSpecificity(control, e);
            if (s > specificity) {
                specificity = s;
                match = binding;
            }
        }
        String action = null;
        if (match != null) {
            action = match.getAction();
            // Rather than switching the result of the action lookup in this way, the preferred
            // way to do this according to the current architecture would be to hoist the
            // getEffectiveNodeOrientation call up into the key bindings, the same way that
            // list-view orientation (horizontal vs. vertical) is handled in a key binding.
            if (e.getCode() == LEFT || e.getCode() == RIGHT) {
                if (control.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT) {
                    if (match.getAction().equals("TraverseLeft")) {
                        action = "TraverseRight";
                    } else {
                        if (match.getAction().equals("TraverseRight")) {
                            action = "TraverseLeft";
                        }
                    }
                }
            }
        }
        return action;
    }

    /**
     * Called to invoke the action associated with the given name.
     *
     * <p>When a KeyEvent is handled, it is first passed through
     * callActionForEvent which resolves which "action" should be executed
     * based on the key event. This action is indicated by name. This name is
     * then passed to this function which is responsible for invoking the right
     * function based on the name.</p>
     */
    protected void callAction(String name) {
        switch (name) {
            case TRAVERSE_UP: traverseUp(); break;
            case TRAVERSE_DOWN: traverseDown(); break;
            case TRAVERSE_LEFT: traverseLeft(); break;
            case TRAVERSE_RIGHT: traverseRight(); break;
            case TRAVERSE_NEXT: traverseNext(); break;
            case TRAVERSE_PREVIOUS: traversePrevious(); break;
        }
    }

    /***************************************************************************
     * Focus Traversal methods                                                 *
     **************************************************************************/

    /**
     * Called by any of the BehaviorBase traverse methods to actually effect a
     * traversal of the focus. The default behavior of this method is to simply
     * call impl_traverse on the given node, passing the given direction. A
     * subclass may override this method.
     *
     * @param node The node to call impl_traverse on
     * @param dir The direction to traverse
     */
    protected void traverse(final Node node, final Direction dir) {
        node.impl_traverse(dir);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node above the current one.
     */
    public final void traverseUp() {
        traverse(control, com.sun.javafx.scene.traversal.Direction.UP);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node below the current one.
     */
    public final void traverseDown() {
        traverse(control, com.sun.javafx.scene.traversal.Direction.DOWN);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node left of the current one.
     */
    public final void traverseLeft() {
        traverse(control, com.sun.javafx.scene.traversal.Direction.LEFT);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node right of the current one.
     */
    public final void traverseRight() {
        traverse(control, com.sun.javafx.scene.traversal.Direction.RIGHT);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node in the focus traversal cycle.
     */
    public final void traverseNext() {
        traverse(control, com.sun.javafx.scene.traversal.Direction.NEXT);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the previous focusTraversable Node in the focus traversal cycle.
     */
    public final void traversePrevious() {
        traverse(control, com.sun.javafx.scene.traversal.Direction.PREVIOUS);
    }

    /***************************************************************************
     * Event handler methods.                                                  *
     *                                                                         *
     * I'm not sure why only mouse events are here. What about drag and        *
     * drop events for instance? What about touch events? What about the       *
     * other mouse events? It does seem like these need to be here, because    *
     * for example mouse interaction logic might differ from platform to       *
     * platform, and the Behavior is supposed to implement all the user        *
     * interaction logic (not just key handling). So it seems like             *
     * BehaviorBase should have methods for handling all forms of input events,*
     * and not just these four mouse events.                                   *
     **************************************************************************/

    /**
     * Called whenever the focus on the control has changed. This method is
     * intended to be overridden by subclasses that are interested in focus
     * change events.
     */
    protected void focusChanged() { }

    /**
     * Invoked by a Skin when the body of the control has been pressed by
     * the mouse. Subclasses should be sure to call super unless they intend
     * to disable any built-in support.
     *
     * @param e the mouse event
     */
    public void mousePressed(MouseEvent e) { }

    /**
     * Invoked by a Skin when the body of the control has been dragged by
     * the mouse. Subclasses should be sure to call super unless they intend
     * to disable any built-in support (for example, for tooltips).
     *
     * @param e the mouse event
     */
    public void mouseDragged(MouseEvent e) { }

    /**
     * Invoked by a Skin when the body of the control has been released by
     * the mouse. Subclasses should be sure to call super unless they intend
     * to disable any built-in support (for example, for tooltips).
     *
     * @param e the mouse event
     */
    public void mouseReleased(MouseEvent e) { }

    /**
     * Invoked by a Skin when the body of the control has been entered by
     * the mouse. Subclasses should be sure to call super unless they intend
     * to disable any built-in support.
     *
     * @param e the mouse event
     */
    public void mouseEntered(MouseEvent e) { }

    /**
     * Invoked by a Skin when the body of the control has been exited by
     * the mouse. Subclasses should be sure to call super unless they intend
     * to disable any built-in support.
     *
     * @param e the mouse event
     */
    public void mouseExited(MouseEvent e) { }
}
