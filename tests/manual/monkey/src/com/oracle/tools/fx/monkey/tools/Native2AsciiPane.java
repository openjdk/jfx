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
package com.oracle.tools.fx.monkey.tools;

import java.util.ArrayList;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/**
 * Native-to-ASCII and ASCII-to-Native Converter Pane.
 */
public class Native2AsciiPane extends BorderPane {
    private final TextArea nat;
    private final TextArea ascii;
    private final TableView<Entry> table;
    private boolean ignoreEvent;

    public Native2AsciiPane() {
        nat = new TextArea();
        nat.textProperty().addListener((x) -> convert(true));

        ascii = new TextArea();
        ascii.textProperty().addListener((x) -> convert(false));

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
        {
            TableColumn<Entry, Integer> c = new TableColumn<>();
            c.setText("Index");
            c.setCellValueFactory((d) -> new SimpleObjectProperty<Integer>(d.getValue().index));
            c.setPrefWidth(50);
            table.getColumns().add(c);
        }
        {
            TableColumn<Entry, String> c = new TableColumn<>();
            c.setText("Char");
            c.setCellValueFactory((d) -> new SimpleStringProperty(d.getValue().character));
            c.setPrefWidth(50);
            table.getColumns().add(c);
        }
        {
            TableColumn<Entry, String> c = new TableColumn<>();
            c.setText("U+Code");
            c.setCellValueFactory((d) -> new SimpleStringProperty(d.getValue().code));
            c.setPrefWidth(100);
            table.getColumns().add(c);
        }
        {
            TableColumn<Entry, String> c = new TableColumn<>();
            c.setText("Description");
            c.setCellValueFactory((d) -> new SimpleStringProperty(d.getValue().description));
            c.setPrefWidth(1000);
            table.getColumns().add(c);
        }

        GridPane p = new GridPane();
        p.add(new Label("Native"), 0, 0);
        p.add(nat, 0, 1);
        p.add(new Label("ASCII"), 1, 0);
        p.add(ascii, 1, 1);
        p.add(table, 0, 2, 2, 1);

        fill(p, nat);
        fill(p, ascii);
        fill(p, table);

        setCenter(p);
    }

    private void fill(GridPane p, Node n) {
        GridPane.setFillHeight(n, Boolean.TRUE);
        GridPane.setFillWidth(n, Boolean.TRUE);
        GridPane.setHgrow(n, Priority.ALWAYS);
        GridPane.setVgrow(n, Priority.ALWAYS);
    }

    protected void convert(boolean fromNative) {
        if (ignoreEvent) {
            return;
        }

        ignoreEvent = true;

        if (fromNative) {
            String s = nat.getText();
            String text = native2ascii(s);
            ascii.setText(text);
            updateSymbols(s);
        } else {
            String s = ascii.getText();
            String text = ascii2native(s);
            nat.setText(text);
            updateSymbols(text);
        }
        ignoreEvent = false;
    }

    private String ascii2native(String text) {
        if (text == null) {
            return null;
        }

        int sz = text.length();
        StringBuilder sb = new StringBuilder(sz);
        for (int i = 0; i < sz; i++) {
            char c = text.charAt(i);
            switch (c) {
            case '\\':
                int u = toUnicode(text, i + 1);
                if (u < 0) {
                    sb.append(c);
                } else {
                    sb.append((char)u);
                    i += 5;
                }
                break;
            default:
                sb.append(c);
                break;
            }
        }
        return sb.toString();
    }

    public static String native2ascii(String text) {
        if (text == null) {
            return null;
        }

        int sz = text.length();
        StringBuilder sb = new StringBuilder(sz + 256);
        for (int i = 0; i < sz; i++) {
            char c = text.charAt(i);
            switch (c) {
            case ' ':
            case '\n':
                sb.append(c);
                break;
            default:
                if ((c > ' ') && (c < 0x7f)) {
                    sb.append(c);
                } else {
                    sb.append("\\u");
                    sb.append(hex(c >> 12));
                    sb.append(hex(c >> 8));
                    sb.append(hex(c >> 4));
                    sb.append(hex(c));
                }
            }
        }
        return sb.toString();
    }

    private static char hex(int n) {
        return "0123456789abcdef".charAt(n & 0x0f);
    }

    private int toUnicode(String text, int ix) {
        if (text.length() < (ix + 5)) {
            return -1;
        }

        char c = text.charAt(ix++);
        switch (c) {
        case 'u':
        case 'U':
            break;
        default:
            return -1;
        }

        try {
            String s = text.substring(ix, ix + 4);
            int v = Integer.parseInt(s, 16);
            return v;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    protected void updateSymbols(String text) {
        int sz = text.length();
        ArrayList<Entry> es = new ArrayList<>(sz);
        for (int i = 0; i < sz; i++) {
            char c = text.charAt(i);
            String code = String.format("%04X", (int)c);
            String desc = Character.getName(c);
            Entry en = new Entry(i, String.valueOf(c), code, desc);
            es.add(en);
        }
        table.getItems().setAll(es);
    }

    protected static class Entry {
        public final int index;
        public final String character;
        public final String code;
        public final String description;

        public Entry(int index, String character, String code, String description) {
            this.index = index;
            this.character = character;
            this.code = code;
            this.description = description;
        }
    }
}
