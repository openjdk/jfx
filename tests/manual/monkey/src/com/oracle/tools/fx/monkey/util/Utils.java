/*
 * Copyright (c) 2023, 2026, Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Monkey Tester Utilities
 */
public class Utils {
    private static final Random random = new Random();
    private static final String HEX = "0123456789ABCDEF";

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

    public static Pane withButtons(Node main, Node ... buttons) {
        HBox b = new HBox(2);
        HBox.setHgrow(main, Priority.ALWAYS);
        b.getChildren().add(main);
        b.getChildren().addAll(buttons);
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

    /**
     * Dumps byte array into a nicely formatted String.
     * printing address first, then 16 bytes of hex then ASCII representation then newline
     *     "0000  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00  ................" or
     * "00000000  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00  ................"
     * depending on startAddress
     *
     * @param bytes the input
     * @param startAddress the logical start address
     * @return
     */
    // Adapted from https://github.com/andy-goryachev/AppFramework/blob/main/src/goryachev/common/util/Dump.java
    // with the author's permission.
    public static String hex(byte[] bytes, long startAddress) {
        if (bytes == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder(((bytes.length / 16) + 1) * 77 + 1);
        hex(sb, bytes, startAddress, 0);
        return sb.toString();
    }

    private static void hex(StringBuilder sb, byte[] bytes, long startAddress, int indent) {
        boolean bigfile = ((startAddress + bytes.length) > 65535);

        int col = 0;
        long addr = startAddress;
        int lineStart = 0;

        for (int i = 0; i < bytes.length; i++) {
            if (col == 0) {
                // indent
                for (int j = 0; j < indent; j++) {
                    sb.append(' ');
                }

                // offset
                if (col == 0) {
                    lineStart = i;
                    if (bigfile) {
                        hex(sb, (int)(addr >> 24));
                        hex(sb, (int)(addr >> 16));
                    }
                    hex(sb, (int)(addr >> 8));
                    hex(sb, (int)(addr));
                    sb.append("  ");
                }
            }

            // byte
            hex(sb, bytes[i]);
            sb.append(' ');

            // space or newline
            if (col >= 15) {
                dumpASCII(sb, bytes, lineStart);
                col = 0;
            } else {
                col++;
            }

            addr++;
        }

        if (col != 0) {
            while (col++ < 16) {
                sb.append("   ");
            }

            dumpASCII(sb, bytes, lineStart);
        }
    }

    public static void hex(StringBuilder sb, int c) {
        sb.append(HEX.charAt((c >> 4) & 0x0f));
        sb.append(HEX.charAt(c & 0x0f));
    }

    private static void dumpASCII(StringBuilder sb, byte[] bytes, int lineStart) {
        // first, print padding
        sb.append(' ');

        int max = Math.min(bytes.length, lineStart + 16);
        for (int i = lineStart; i < max; i++) {
            int d = bytes[i] & 0xff;
            if ((d < 0x20) || (d >= 0x7f)) {
                d = '.';
            }
            sb.append((char)d);
        }

        sb.append('\n');
    }

    public static <T> T getSelectedItem(ComboBox<T> c) {
        return c.getSelectionModel().getSelectedItem();
    }

    public static <T> T getSelectedNamedItem(ComboBox<NamedValue<T>> c) {
        NamedValue<T> v = c.getSelectionModel().getSelectedItem();
        return v == null ? null : v.getValue();
    }

    public static void selectItem(ComboBox c, Object value) {
        List<Object> items = c.getItems();
        int sz = items.size();
        for (int i = 0; i < sz; i++) {
            Object item = items.get(i);
            if (match(item, value)) {
                c.getSelectionModel().select(i);
                return;
            }
        }
    }

    private static boolean match(Object item, Object value) {
        Object v = (item instanceof NamedValue n) ? n.getValue() : item;
        return eq(v, value);
    }

    public static <T> void setUniversalConverter(ComboBox<T> c) {
        c.setConverter(Formats.universalConverter());
    }
}
