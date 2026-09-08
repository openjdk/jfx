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

import java.util.function.Function;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.PickResult;
import com.oracle.tools.fx.monkey.sheets.PropertiesMonitor;
import com.oracle.tools.fx.monkey.sheets.TypeSpecificContextMenu;
import com.oracle.tools.fx.monkey.tools.AccessibilityPropertyViewer;

/**
 * Helps create ContextMenu options.
 */
public class ContextMenuOptions extends ComboBox<NamedValue<Object>> {

    private final Node target;

    public ContextMenuOptions(String name, Node target) {
        this.target = target;
        FX.name(this, name);
        setMaxWidth(Double.MAX_VALUE);

        getSelectionModel().selectedItemProperty().addListener((s, pr, c) -> {
            Object v = c.getValue();
            setMenu(v);
        });

        addChoice("<null>", () -> null);
        addChoice("Standard Context Menu", this::populate);
    }

    private ContextMenu populate(PickResult pick) {
        ContextMenu m = new ContextMenu();
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
        StdoutMouseListener.attach(m, target);
        if (target != source) {
            StdoutMouseListener.attach(m, source);
        }
        return m;
    }

    private void setMenu(Object v) {
        if(v instanceof Supplier s) {
            FX.setPopupMenu(target, s);
        } else if(v instanceof Function f) {
            FX.setPopupMenu(target, f);
        }
    }

    public void addChoice(String name, Supplier<ContextMenu> gen) {
        getItems().add(new NamedValue<>(name, gen));
    }
    
    public void addChoice(String name, Function<PickResult,ContextMenu> gen) {
        getItems().add(new NamedValue<>(name, gen));
    }
}
