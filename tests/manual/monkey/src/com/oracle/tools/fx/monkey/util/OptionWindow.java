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
package com.oracle.tools.fx.monkey.util;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 * Property Sheet Window.
 */
public class OptionWindow extends Stage {
    public OptionWindow(Object parent, String title, double width, double height, Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToHeight(true);
        sp.setFitToWidth(true);

        initOwner(FX.getParentWindow(parent));
        setScene(new Scene(sp));
        setTitle(title);
        addEventHandler(KeyEvent.KEY_RELEASED, this::handleKey);
        setWidth(width);
        setHeight(height);
    }

    public static OptionWindow open(Object parent, String title, double width, double height, Node content) {
        OptionWindow w = new OptionWindow(parent, title, width, height, content);
        w.show();
        return w;
    }

    private void handleKey(KeyEvent ev) {
        if (ev.getCode() == KeyCode.ESCAPE) {
            hide();
            ev.consume();
        }
    }
}
