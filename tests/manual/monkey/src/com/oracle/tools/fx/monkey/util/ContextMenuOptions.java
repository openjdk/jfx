/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.util;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.PickResult;
import com.oracle.tools.fx.monkey.options.ObjectOption;
import com.oracle.tools.fx.monkey.sheets.PropertiesMonitor;
import com.oracle.tools.fx.monkey.sheets.TypeSpecificContextMenu;
import com.oracle.tools.fx.monkey.tools.AccessibilityPropertyViewer;

/**
 * Helps create ContextMenu options.
 */
public class ContextMenuOptions extends ObjectOption<ContextMenu> {

    private PickResult pick;

    public ContextMenuOptions(String name, Control control) {
        super(name, control.contextMenuProperty());

        control.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, (ev) -> {
            pick = ev.getPickResult();
        });

        addChoice("<null>", null);
        addChoiceSupplier("Standard Context Menu", () -> {
            return new ContextMenu() {
                @Override
                public void show(Node anchor, double screenX, double screenY) {
                    getItems().clear();
                    populate(this, control, pick);
                    super.show(anchor, screenX, screenY);
                }
            };
        });
        selectInitialValue();
    }

    private static void populate(ContextMenu m, Control c, PickResult pick) {
        Node source = pick.getIntersectedNode();
        TypeSpecificContextMenu.populate(m, source);
        if (m.getItems().size() > 0) {
            FX.separator(m);
        }
        FX.item(m, "Accessibility Attributes...", () -> {
            AccessibilityPropertyViewer.open(pick);
        });
        FX.item(m, "Show Properties Monitor...", () -> {
            PropertiesMonitor.open(source);
        });
        StdoutMouseListener.attach(m, c);
        if (c != source) {
            StdoutMouseListener.attach(m, source);
        }
    }
}
