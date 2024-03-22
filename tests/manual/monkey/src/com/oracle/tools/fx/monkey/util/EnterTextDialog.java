/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Consumer;
import javafx.beans.property.Property;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class EnterTextDialog extends Stage {
    private final TextArea textField;

    public EnterTextDialog(Object owner, String initialText, Consumer<String> onEdit) {
        initOwner(FX.getParentWindow(owner));
        initModality(Modality.APPLICATION_MODAL);

        textField = new TextArea(initialText);

        Button ok = FX.button("OK", () -> {
            String text = textField.getText();
            onEdit.accept(text);
            hide();
        });

        ButtonBar bp = new ButtonBar();
        bp.setPadding(new Insets(5, 10, 5, 10));
        bp.getButtons().add(ok);

        BorderPane p = new BorderPane(textField);
        p.setBottom(bp);
        setScene(new Scene(p));

        addEventHandler(KeyEvent.KEY_PRESSED, (ev) -> {
            if (ev.getCode() == KeyCode.ESCAPE) {
                hide();
            }
        });

        setWidth(400);
        setHeight(300);
        setTitle("Enter Text");
    }

    public static Runnable getRunnable(Node owner, Property<String> p) {
        if (p == null) {
            return null;
        }
        return () -> {
            String text = p.getValue();
            new EnterTextDialog(owner, text, (v) -> {
                p.setValue(v);
            }).show();
        };
    }
}
