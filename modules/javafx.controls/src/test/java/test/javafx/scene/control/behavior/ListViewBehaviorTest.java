/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package test.javafx.scene.control.behavior;

import java.util.concurrent.atomic.AtomicReference;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.skin.VirtualFlowShim;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.scene.control.infrastructure.KeyModifier;

/**
 * ListView Behavior Test.
 */
public class ListViewBehaviorTest extends BehaviorTestBase<ListView<String>> {
    @BeforeEach
    public void beforeEach() {
        ObservableList<String> items = FXCollections.observableArrayList(
            "very very long text so we can get a horizontal scroll bar",
            "another",
            "another one",
            "make sure the strings are unique"
        );
        initStage(new ListView<>(items));
    }

    @AfterEach
    public void afterEach() {
        closeStage();
    }

    /**
     * Verifies that alt-shortcut-RIGHT/LEFT keys scroll horizontally in LTR orientation.
     */
    @Test
    public void testHorizontalScrollKeyboardLTR() {
        AtomicReference<Double> pos = new AtomicReference<>();
        execute(
            exe(() -> {
                control.setMaxWidth(50);
                control.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
                double w = hsb().getValue();
                pos.set(w);
            }),
            key(KeyCode.RIGHT, KeyModifier.ALT, KeyModifier.getShortcutKey()),
            exe(() -> {
                double w = hsb().getValue();
                // should have scrolled
                Assertions.assertTrue(pos.get() < w);
                pos.set(w);
            }),
            key(KeyCode.LEFT, KeyModifier.ALT, KeyModifier.getShortcutKey()),
            exe(() -> {
                double w = hsb().getValue();
                // should have scrolled
                Assertions.assertTrue(pos.get() > w);
            })
        );
    }

    /**
     * Verifies that alt-shortcut-RIGHT/LEFT keys scroll horizontally in RTL orientation.
     */
    @Test
    public void testHorizontalScrollKeyboardRTL() {
        AtomicReference<Double> pos = new AtomicReference<>();
        execute(
            exe(() -> {
                control.setMaxWidth(50);
                control.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                double w = hsb().getValue();
                pos.set(w);
            }),
            key(KeyCode.LEFT, KeyModifier.ALT, KeyModifier.getShortcutKey()),
            exe(() -> {
                double w = hsb().getValue();
                // should have scrolled
                Assertions.assertTrue(pos.get() < w);
                pos.set(w);
            }),
            key(KeyCode.RIGHT, KeyModifier.ALT, KeyModifier.getShortcutKey()),
            exe(() -> {
                double w = hsb().getValue();
                // should have scrolled
                Assertions.assertTrue(pos.get() > w);
            })
        );
    }

    /**
     * Verifies that alt-shortcut-UP/DOWN keys scroll vertically.
     */
    @Test
    public void testVerticalScrollKeyboard() {
        AtomicReference<Double> pos = new AtomicReference<>();
        execute(
            exe(() -> {
                control.setMaxHeight(50);
                double w = vsb().getValue();
                pos.set(w);
            }),
            key(KeyCode.DOWN, KeyModifier.ALT, KeyModifier.getShortcutKey()),
            exe(() -> {
                double w = vsb().getValue();
                // should have scrolled
                Assertions.assertTrue(pos.get() < w);
                pos.set(w);
            }),
            key(KeyCode.UP, KeyModifier.ALT, KeyModifier.getShortcutKey()),
            exe(() -> {
                double w = vsb().getValue();
                // should have scrolled
                Assertions.assertTrue(pos.get() > w);
            })
        );
    }

    private ScrollBar hsb() {
        var f = VirtualFlowShim.getVirtualFlow(control.getSkin());
        return VirtualFlowShim.getHBar(f);
    }

    private ScrollBar vsb() {
        var f = VirtualFlowShim.getVirtualFlow(control.getSkin());
        return VirtualFlowShim.getVBar(f);
    }
}
