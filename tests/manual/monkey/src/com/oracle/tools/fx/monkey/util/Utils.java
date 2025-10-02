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
package com.oracle.tools.fx.monkey.util;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.function.BiConsumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Monkey Tester Utilities
 */
public class Utils {
    private static final DecimalFormat DOUBLE_FORMAT_2 = new DecimalFormat("0.##");
    private static final Random random = new Random();

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

    public static String f2(double v) {
        return DOUBLE_FORMAT_2.format(v);
    }

    public static String simpleName(Object x) {
        if (x == null) {
            return "<null>";
        }
        Class<?> c = (x instanceof Class) ? (Class<?>)x : x.getClass();
        String s = c.getSimpleName();
        if (!isBlank(s)) {
            return s;
        }
        s = c.getName();
        int ix = s.lastIndexOf('.');
        if (ix < 0) {
            return s;
        }
        return s.substring(ix + 1);
    }

    public static Color nextColor() {
        double hue = 360 * random.nextDouble();
        double saturation = 0.5 + 0.5 * random.nextDouble();
        double brightness = random.nextDouble();
        double opacity = random.nextDouble();
        return Color.hsb(hue, saturation, brightness, opacity);
    }

    public static <T extends Enum> T[] withNull(Class<T> type) {
        T[] values = type.getEnumConstants();
        T[] a = (T[])Array.newInstance(type, values.length + 1);
        System.arraycopy(values, 0, a, 1, values.length);
        return a;
    }

    public static void link(BooleanProperty ui, ReadOnlyBooleanProperty main, BooleanConsumer c) {
        main.addListener((s, p, v) -> {
            ui.set(v);
        });
        if (c != null) {
            ui.addListener((s, p, v) -> {
                if (main.get() != v) {
                    c.consume(v);
                }
            });
            boolean val = ui.get();
            c.consume(val);
        }
    }
}
