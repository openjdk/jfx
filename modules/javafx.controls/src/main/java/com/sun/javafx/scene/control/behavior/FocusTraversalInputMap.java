/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.traversal.Direction;
import javafx.event.EventTarget;
import javafx.scene.Node;
import com.sun.javafx.scene.control.inputmap.InputMap;
import com.sun.javafx.scene.control.inputmap.KeyBinding;
import javafx.scene.input.KeyEvent;

import java.util.List;

import static com.sun.javafx.scene.control.inputmap.InputMap.*;
import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.TAB;
import static javafx.scene.input.KeyCode.UP;

public class FocusTraversalInputMap<N extends Node> {

    private static final List<InputMap.Mapping<?>> MAPPINGS = List.of(
        new KeyMapping(UP, e -> traverseUp(e)),
        new KeyMapping(DOWN, e -> traverseDown(e)),
        new KeyMapping(LEFT, e -> traverseLeft(e)),
        new KeyMapping(RIGHT, e -> traverseRight(e)),
        new KeyMapping(TAB, e -> traverseNext(e)),
        new KeyMapping(new KeyBinding(TAB).shift(), e -> traversePrevious(e)),

        new KeyMapping(new KeyBinding(UP).shift().alt().ctrl(), e -> traverseUp(e)),
        new KeyMapping(new KeyBinding(DOWN).shift().alt().ctrl(), e -> traverseDown(e)),
        new KeyMapping(new KeyBinding(LEFT).shift().alt().ctrl(), e -> traverseLeft(e)),
        new KeyMapping(new KeyBinding(RIGHT).shift().alt().ctrl(), e -> traverseRight(e)),
        new KeyMapping(new KeyBinding(TAB).shift().alt().ctrl(), e -> traverseNext(e)),
        new KeyMapping(new KeyBinding(TAB).alt().ctrl(), e -> traversePrevious(e)));

    private FocusTraversalInputMap() {
        // no-op, just forcing use of static method
    }

    public static InputMap.Mapping<?>[] getFocusTraversalMappings() {
        return MAPPINGS.toArray(new InputMap.Mapping[MAPPINGS.size()]);
    }

    public static <N extends Node> InputMap<N> createInputMap(N node) {
        InputMap<N> inputMap = new InputMap<>(node);
        inputMap.getMappings().addAll(getFocusTraversalMappings());
        return inputMap;
    }



    /***************************************************************************
     * Focus Traversal methods                                                 *
     **************************************************************************/

    /**
     * Called by any of the BehaviorBase traverse methods to actually effect a
     * traversal of the focus. The default behavior of this method is to simply
     * traverse on the given node, passing the given direction. A
     * subclass may override this method.
     *
     * @param node The node to traverse on
     * @param dir The direction to traverse
     */
    public static void traverse(final Node node, final Direction dir) {
        if (node == null) {
            throw new IllegalArgumentException("Attempting to traverse on a null Node. " +
                    "Most probably a KeyEvent has been fired with a null target specified.");
        }
        NodeHelper.traverse(node, dir);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node above the current one.
     */
    public static final void traverseUp(KeyEvent e) {
        traverse(getNode(e), com.sun.javafx.scene.traversal.Direction.UP);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node below the current one.
     */
    public static final void traverseDown(KeyEvent e) {
        traverse(getNode(e), com.sun.javafx.scene.traversal.Direction.DOWN);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node left of the current one.
     */
    public static final void traverseLeft(KeyEvent e) {
        traverse(getNode(e), com.sun.javafx.scene.traversal.Direction.LEFT);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node right of the current one.
     */
    public static final void traverseRight(KeyEvent e) {
        traverse(getNode(e), com.sun.javafx.scene.traversal.Direction.RIGHT);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the next focusTraversable Node in the focus traversal cycle.
     */
    public static final void traverseNext(KeyEvent e) {
        traverse(getNode(e), com.sun.javafx.scene.traversal.Direction.NEXT);
    }

    /**
     * Calls the focus traversal engine and indicates that traversal should
     * go the previous focusTraversable Node in the focus traversal cycle.
     */
    public static final void traversePrevious(KeyEvent e) {
        traverse(getNode(e), com.sun.javafx.scene.traversal.Direction.PREVIOUS);
    }

    private static Node getNode(KeyEvent e) {
        EventTarget target = e.getTarget();
        if (target instanceof Node) {
            return (Node) target;
        }
        return null;
    }
}
