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

import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import com.sun.javafx.scene.traversal.Direction;

import static javafx.scene.input.KeyCode.*;

/**
 * A convenient base class from which all our built-in behaviors extend. The
 * main functionality in BehaviorBase revolves around infrastructure for
 * resolving key events into function calls. The differences between platforms
 * can be subtle, and we attempt to build sufficient infrastructure into
 * BehaviorBase to minimize the amount of code and the complexity of code
 * necessary to support multiple platforms sufficiently well.
 * <p>
 * BehaviorBase also implements the hooks for focus traversal. This
 * implementation is sufficient for most subclasses of BehaviorBase. The
 * following action names are registered in the keyMap for handling focus
 * traversal. Subclasses which need to invoke focus traversal using non-standard
 * key strokes should map key strokes to these action names:
 * <ul>
 *  <li>TraverseUp</li>
 *  <li>TraverseDown</li>
 *  <li>TraverseLeft</li>
 *  <li>TraverseRight</li>
 *  <li>TraverseNext</li>
 *  <li>TraversePrevious</li>
 * </ul>
 * <p>
 * Note that by convention, action names are camel case with the first letter
 * uppercase, matching class naming conventions.
 */
public class BehaviorBase<C extends Control> {
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
    protected static final List<KeyBinding> TRAVERSAL_BINDINGS = new ArrayList<KeyBinding>();
    static {
        TRAVERSAL_BINDINGS.add(new KeyBinding(UP, "TraverseUp"));
        TRAVERSAL_BINDINGS.add(new KeyBinding(DOWN, "TraverseDown"));
        TRAVERSAL_BINDINGS.add(new KeyBinding(LEFT, "TraverseLeft"));
        TRAVERSAL_BINDINGS.add(new KeyBinding(RIGHT, "TraverseRight"));
        TRAVERSAL_BINDINGS.add(new KeyBinding(TAB, "TraverseNext"));
        TRAVERSAL_BINDINGS.add(new KeyBinding(TAB, "TraversePrevious").shift());

        TRAVERSAL_BINDINGS.add(new KeyBinding(UP, "TraverseUp").shift().alt().ctrl());
        TRAVERSAL_BINDINGS.add(new KeyBinding(DOWN, "TraverseDown").shift().alt().ctrl());
        TRAVERSAL_BINDINGS.add(new KeyBinding(LEFT, "TraverseLeft").shift().alt().ctrl());
        TRAVERSAL_BINDINGS.add(new KeyBinding(RIGHT, "TraverseRight").shift().alt().ctrl());
        TRAVERSAL_BINDINGS.add(new KeyBinding(TAB, "TraverseNext").shift().alt().ctrl());
        TRAVERSAL_BINDINGS.add(new KeyBinding(TAB, "TraversePrevious").alt().ctrl());
    }

    protected static final List<KeyBinding> EMPTY_BINDINGS = new ArrayList<KeyBinding>();

    /**
     * The Control with which this Behavior is used. This must be specified in
     * the constructor and must not be null.
     */
    private C control;

    /**
     * The key bindings for this Behavior.
     */
    private List<KeyBinding> keyBindings = createKeyBindings();

    /**
     * Create a new BehaviorBase for the given control. The Control must not
     * be null.
     * @param control
     */
    public BehaviorBase(C control) {
        this.control = control;

        control.addEventHandler(KeyEvent.ANY, keyEventListener);
    }

    private final EventHandler<KeyEvent> keyEventListener = new EventHandler<KeyEvent>() {
        @Override public void handle(KeyEvent e) {
            if (!e.isConsumed()) {
                callActionForEvent(e);
            }
        }
    };

    /**
     * Called during initialization to compute the set of key bindings which
     * should be applied for this behavior. This method should NOT mutate the
     * List after having returned it.
     *
     * @return a non-null list of key bindings.
     */
    protected List<KeyBinding> createKeyBindings() {
        return EMPTY_BINDINGS;
    }

    /***************************************************************************
     * Implementation of the Behavior interface                                *
     **************************************************************************/

    public final C getControl() { return control; }

    protected /*final*/ String matchActionForEvent(KeyEvent e) {
        KeyBinding match = null;
        int specificity = 0;
        // keyBindings may be null...
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
    
    protected /*final*/ void callActionForEvent(KeyEvent e) {
        String action = matchActionForEvent(e);
        if (action != null) {
            callAction(action);
            e.consume();
        }
    }

    /**
     * Called to invoke the action associated with the given name.
     * <p>
     * When a KeyEvent is handled, it is first passed through
     * callActionForEvent which resolves which "action" should be executed
     * based on the key event. This action is indicated by name. This name is
     * then passed to this function which is responsible for invoking the right
     * function based on the name.
     * <p>
     */
    protected void callAction(String name) {
        if ("TraverseUp".equals(name)) traverseUp();
        else if ("TraverseDown".equals(name)) traverseDown();
        else if ("TraverseLeft".equals(name)) traverseLeft();
        else if ("TraverseRight".equals(name)) traverseRight();
        else if ("TraverseNext".equals(name)) traverseNext();
        else if ("TraversePrevious".equals(name)) traversePrevious();
    }

    /***************************************************************************
     * Focus Traversal methods                                                 *
     **************************************************************************/

    public void traverse(Node node, Direction dir) {
        node.impl_traverse(dir);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node above the current one.
     */
    public void traverseUp() {
        traverse(control, com.sun.javafx.scene.traversal.Direction.UP);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node below the current one.
     */
    public void traverseDown() {
        traverse(control, com.sun.javafx.scene.traversal.Direction.DOWN);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node left of the current one.
     */
    public void traverseLeft() {
        traverse(control, com.sun.javafx.scene.traversal.Direction.LEFT);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node right of the current one.
     */
    public void traverseRight() {
        traverse(control, com.sun.javafx.scene.traversal.Direction.RIGHT);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node in the focus traversal cycle.
     */
    public void traverseNext() {
        traverse(control, com.sun.javafx.scene.traversal.Direction.NEXT);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the previous focusTraversable Node in the focus traversal cycle.
     */
    public void traversePrevious() {
        traverse(control, com.sun.javafx.scene.traversal.Direction.PREVIOUS);
    }

    /***************************************************************************
     * Mouse handlers                                                          *
     * TODO I'm not sure why only mouse events are here. What about drag and   *
     * drop events for instance? What about touch events? What about the       *
     * other mouse events? It does seem like these need to be here, because    *
     * for example mouse interaction logic might differ from platform to       *
     * platform, and the Behavior is supposed to implement all the user        *
     * interaction logic (not just key handling). So it seems like             *
     * BehaviorBase should have methods for handling all forms of input events,*
     * and not just these four mouse events.                                   *
     **************************************************************************/

    /**
     * Invoked by a Skin when the body of the control has been pressed by
     * the mouse. Subclasses should be sure to call super unless they intend
     * to disable any built-in support.
     *
     * @param e
     */
    public void mousePressed(MouseEvent e) {
    }

    /**
     * Invoked by a Skin when the body of the control has been dragged by
     * the mouse. Subclasses should be sure to call super unless they intend
     * to disable any built-in support (for example, for tooltips).
     *
     * @param e
     */
    public void mouseDragged(MouseEvent e) {
    }

    /**
     * Invoked by a Skin when the body of the control has been released by
     * the mouse. Subclasses should be sure to call super unless they intend
     * to disable any built-in support (for example, for tooltips).
     *
     * @param e
     */
    public void mouseReleased(MouseEvent e) {
    }

    /**
     * Invoked by a Skin when the body of the control has been entered by
     * the mouse. Subclasses should be sure to call super unless they intend
     * to disable any built-in support.
     *
     * @param e
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * Invoked by a Skin when the body of the control has been exited by
     * the mouse. Subclasses should be sure to call super unless they intend
     * to disable any built-in support.
     *
     * @param e
     */
    public void mouseExited(MouseEvent e) {
    }

    /**
     * @see Node#pseudoClassStateChanged()
     */
    public final void pseudoClassStateChanged(PseudoClass pseudoClass, boolean active) {
        Control ctl = getControl();
        if (ctl != null) {
            ctl.pseudoClassStateChanged(pseudoClass, active);
        }
    }
}
