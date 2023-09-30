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
package com.oracle.tools.fx.monkey.pages;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * VBox Page.
 */
public class VBoxPage extends BoxPageBase {
    public VBoxPage() {
        super("VBoxPage");
    }

    protected void setGrow(Node n, Priority p) {
        VBox.setVgrow(n, p);
    }

    @Override
    protected Pane createPane() {
        return new VBox();
    }

    @Override
    protected void setMin(Region r, double v) {
        r.setMinHeight(v);
    }

    @Override
    protected void setPref(Region r, double v) {
        r.setPrefHeight(v);
    }

    @Override
    protected void setMax(Region r, double v) {
        r.setMaxHeight(v);
    }

    @Override
    protected Region createRegion() {
        Region r = new Region();
        r.setPrefHeight(30);
        r.setMinHeight(10);
        ContextMenu m = new ContextMenu();
        r.setOnContextMenuRequested((ev) -> {
            m.getItems().setAll(
                new MenuItem("height=" + r.getHeight()),
                new SeparatorMenuItem(),
                new MenuItem("min height=" + r.getMinHeight()),
                new MenuItem("pref height=" + r.getPrefHeight()),
                new MenuItem("max height=" + r.getMaxHeight()));
            m.show(r, ev.getScreenX(), ev.getScreenY());
        });
        return r;
    }
}
