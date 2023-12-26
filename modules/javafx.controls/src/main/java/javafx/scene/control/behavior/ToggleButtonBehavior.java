/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.behavior;

import com.sun.javafx.scene.control.skin.Utils;

import javafx.collections.ObservableList;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.BehaviorAspect;
import javafx.scene.control.BehaviorConfiguration;
import javafx.scene.control.Control;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;

/**
 * Standard behavior for {@link ToggleButton}. Inherits behavior from {@link ButtonBaseBehavior}.
 */
public class ToggleButtonBehavior implements Behavior<ToggleButton> {
    private static final BehaviorAspect<ToggleButton, Controller> KEYBOARD_NAVIGATION_ASPECT = BehaviorAspect.builder(Controller.class, Controller::new)
        .registerEventHandler(KeyEvent.KEY_PRESSED, MultiplexingKeyEventHandler.builder()
            .addBinding(new KeyCodeCombination(KeyCode.UP), Controller::traverseUp)
            .addBinding(new KeyCodeCombination(KeyCode.DOWN), Controller::traverseDown)
            .addBinding(new KeyCodeCombination(KeyCode.RIGHT), Controller::traverseRight)
            .addBinding(new KeyCodeCombination(KeyCode.LEFT), Controller::traverseLeft)
            .build()::handle
        )
        .build();

    /**
     * A {@link BehaviorConfiguration} for {@link ToggleButton}s.
     */
    public static final BehaviorConfiguration<ToggleButton> CONFIGURATION = BehaviorConfiguration.<ToggleButton>builder()
        .add(KEYBOARD_NAVIGATION_ASPECT)
        .include(ButtonBaseBehavior.CONFIGURATION)
        .build();

    @Override
    public BehaviorConfiguration<ToggleButton> getConfiguration() {
        return CONFIGURATION;
    }

    private enum Direction { UP, DOWN, RIGHT, LEFT }

    /**
     * Controller class for {@link ToggleButtonBehavior}.
     */
    public static class Controller {
        private final ToggleButton control;

        /**
         * Constructs a new instance.
         *
         * @param control a control, cannot be {@code null}
         */
        protected Controller(ToggleButton control) {
            this.control = control;
        }

        private boolean traverseUp() {
            return traverse(Direction.UP);
        }

        private boolean traverseDown() {
            return traverse(Direction.DOWN);
        }

        private boolean traverseLeft() {
            return traverse(Direction.LEFT);
        }

        private boolean traverseRight() {
            return traverse(Direction.RIGHT);
        }

        private boolean traverse(Direction direction) {
            ToggleGroup toggleGroup = control.getToggleGroup();

            if (toggleGroup == null) {  // A ToggleButton does not have to be in a group.
                return true;  // Always consume the keys
            }

            ObservableList<Toggle> toggles = toggleGroup.getToggles();
            int currentToggleIdx = toggles.indexOf(control);
            boolean traversingToNext = traversingToNext(direction, control.getEffectiveNodeOrientation());

            if (Utils.isTwoLevelFocus()) {
                return false;  // Don't consume when two-level focus is handling navigation
            }

            int newToggleIndex = traversingToNext ? nextToggleIndex(toggles, currentToggleIdx) : previousToggleIndex(toggles, currentToggleIdx);

            if (newToggleIndex == currentToggleIdx) {
                return false;  // Don't consume when toggle can't be switched
            }

            Toggle toggle = toggles.get(newToggleIndex);

            toggleGroup.selectToggle(toggle);
            ((Control)toggle).requestFocus();

            return true;  // Toggle was switched, consume key
        }
    }

    /*
     * Returns the next toggle index or "from" if none found
     */
    private static int nextToggleIndex(ObservableList<Toggle> toggles, int from) {
        if (from  < 0 || from >= toggles.size()) return 0;
        int i = (from + 1) % toggles.size();
        while (i != from && toggles.get(i) instanceof Node n && n.isDisabled()) {
            i = (i + 1) % toggles.size();
        }
        return i;
    }

    /*
     * Returns the previous toggle index or "from" if none found
     */
    private static int previousToggleIndex(ObservableList<Toggle> toggles, int from) {
        if (from  < 0 || from >= toggles.size()) return toggles.size();
        int i = Math.floorMod(from - 1, toggles.size());
        while (i != from && toggles.get(i) instanceof Node n && n.isDisabled()) {
            i = Math.floorMod(i - 1, toggles.size());
        }
        return i;
    }

    private static boolean traversingToNext(Direction direction, NodeOrientation effectiveNodeOrientation) {
        boolean rtl = effectiveNodeOrientation == NodeOrientation.RIGHT_TO_LEFT;

        return switch (direction) {
            case RIGHT -> rtl ? false : true;
            case DOWN -> true;
            case LEFT -> rtl ? true : false;
            case UP -> false;
        };
    }
}
