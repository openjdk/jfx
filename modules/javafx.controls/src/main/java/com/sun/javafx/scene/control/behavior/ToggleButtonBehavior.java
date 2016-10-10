/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.skin.Utils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import com.sun.javafx.scene.control.inputmap.InputMap;
import javafx.scene.input.KeyEvent;

import static com.sun.javafx.scene.control.inputmap.InputMap.*;
import static javafx.scene.input.KeyCode.*;

public class ToggleButtonBehavior<C extends ToggleButton> extends ButtonBehavior<C>{

    public ToggleButtonBehavior(C button) {
        super(button);

        ObservableList<Mapping<?>> mappings = FXCollections.observableArrayList(
            new KeyMapping(RIGHT, e -> traverse(e, "ToggleNext-Right")),
            new KeyMapping(LEFT, e -> traverse(e, "TogglePrevious-Left")),
            new KeyMapping(DOWN, e -> traverse(e, "ToggleNext-Down")),
            new KeyMapping(UP, e -> traverse(e, "TogglePrevious-Up"))
        );

        // we disable auto-consuming, so that unconsumed events work their way
        // back up the input map hierarchy and back out of the node.
        for (Mapping<?> mapping : mappings) {
            mapping.setAutoConsume(false);
        }

        // put the mappings into a child input map so they take precedence
        InputMap<C> overriddenFocusInput = new InputMap<>(button);
        overriddenFocusInput.getMappings().addAll(mappings);
        addDefaultChildMap(getInputMap(), overriddenFocusInput);
    }

    /**
     * Returns the next toggle index or "from" if none found
     */
    private int nextToggleIndex(final ObservableList<Toggle> toggles, final int from) {
        Toggle toggle;
        if (from  < 0 || from >= toggles.size()) return 0;
        int i = (from + 1) % toggles.size();
        while (i != from && (toggle = toggles.get(i)) instanceof Node &&
                ((Node)toggle).isDisabled()) {
            i = (i + 1) % toggles.size();
        }
        return i;
    }

    /**
     * Returns the previous toggle index or "from" if none found
     */
    private int previousToggleIndex(final ObservableList<Toggle> toggles, final int from) {
        Toggle toggle;
        if (from  < 0 || from >= toggles.size()) return toggles.size();
        int i = Math.floorMod(from - 1, toggles.size());
        while (i != from && (toggle = toggles.get(i)) instanceof Node &&
                ((Node)toggle).isDisabled()) {
            i = Math.floorMod(i - 1, toggles.size());
        }
        return i;
    }

    private void traverse(KeyEvent e, String name) {
        ToggleButton toggleButton = getNode();
        final ToggleGroup toggleGroup = toggleButton.getToggleGroup();
        // A ToggleButton does not have to be in a group.
        if (toggleGroup == null) {
            e.consume();
            return;
        }
        ObservableList<Toggle> toggles = toggleGroup.getToggles();
        final int currentToggleIdx = toggles.indexOf(toggleButton);

        boolean traversingToNext = traversingToNext(name, toggleButton.getEffectiveNodeOrientation());
        if (Utils.isTwoLevelFocus()) {
            // Because we don't auto-consume (see mapping definitions above), we
            // can simply return here and have the traversal handled by another
            // appropriate mapping
        } else if (traversingToNext) {
            int nextToggleIndex = nextToggleIndex(toggles, currentToggleIdx);
            if (nextToggleIndex == currentToggleIdx) {
                // Because we don't auto-consume (see mapping definitions above), we
                // can simply return here and have the traversal handled by another
                // appropriate mapping
            } else {
                Toggle toggle = toggles.get(nextToggleIndex);
                toggleGroup.selectToggle(toggle);
                ((Control)toggle).requestFocus();
                e.consume();
            }
        } else {
            int prevToggleIndex = previousToggleIndex(toggles, currentToggleIdx);
            if (prevToggleIndex == currentToggleIdx) {
                // Because we don't auto-consume (see mapping definitions above), we
                // can simply return here and have the traversal handled by another
                // appropriate mapping
            } else {
                Toggle toggle = toggles.get(prevToggleIndex);
                toggleGroup.selectToggle(toggle);
                ((Control)toggle).requestFocus();
                e.consume();
            }
        }
    }

    private boolean traversingToNext(String name, NodeOrientation effectiveNodeOrientation) {
        boolean rtl = effectiveNodeOrientation == NodeOrientation.RIGHT_TO_LEFT;
        switch (name) {
            case "ToggleNext-Right":
                return rtl ? false : true;
            case "ToggleNext-Down":
                return true;
            case "TogglePrevious-Left":
                return rtl ? true : false;
            case "TogglePrevious-Up":
                return false;
            default:
                throw new IllegalArgumentException("Not a toggle action");
        }
    }
}
