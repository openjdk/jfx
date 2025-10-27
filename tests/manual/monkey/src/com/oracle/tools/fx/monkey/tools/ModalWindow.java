/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.tools;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Test Modal Window
 */
public class ModalWindow extends Stage {
    public ModalWindow(Window owner) {
        Button b1 = new Button("Does Nothing");
        b1.setDefaultButton(false);

        Button b2 = new Button("Platform.exit()");
        b2.setDefaultButton(false);
        b2.setOnAction((ev) -> Platform.exit());

        Button b3 = new Button("OK");
        b3.setOnAction((ev) -> hide());

        HBox bp = new HBox(b1, b2, b3);
        // FIX BUG: default button property ignored on macOS, ENTER goes to the first button
        b3.setDefaultButton(true);

        BorderPane p = new BorderPane();
        p.setBottom(bp);
        System.out.println(b2.isDefaultButton() + " " + b3.isDefaultButton());

        setTitle("Modal Window");
        setScene(new Scene(p));
        initModality(Modality.APPLICATION_MODAL);
        initOwner(owner);
        setWidth(500);
        setHeight(200);
    }
}
