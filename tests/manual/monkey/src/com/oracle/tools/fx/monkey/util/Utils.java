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

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Monkey Tester Utilities
 */
public class Utils {
    public static boolean isBlank(Object x) {
        if(x == null) {
            return true;
        }
        return (x.toString().trim().length() == 0);
    }

    public static void fromPairs(Object[] pairs, BiConsumer<String, String> client) {
        for (int i = 0; i < pairs.length;) {
            String k = (String)pairs[i++];
            String v = (String)pairs[i++];
            client.accept(k, v);
        }
    }

    public static Pane buttons(Node ... nodes) {
        HBox b = new HBox(nodes);
        b.setSpacing(2);
        return b;
    }

    public static boolean eq(Object a, Object b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    public static void showDialog(Node owner, String windowName, String title, Parent content) {
        Window w = FX.getParentWindow(owner);
        Stage s = new Stage();
        s.initModality(Modality.WINDOW_MODAL);
        s.initOwner(w);

        FX.name(s, windowName);
        s.setTitle(title);
        s.setScene(new Scene(content));
        s.setWidth(900);
        s.setHeight(500);
        s.show();
    }

    public static void showTextDialog(Node owner, String windowName, String title, String text) {
        TextArea textField = new TextArea(text);
        textField.setEditable(false);
        textField.setWrapText(false);

        BorderPane p = new BorderPane();
        p.setCenter(textField);

        showDialog(owner, windowName, title, p);
    }
}
