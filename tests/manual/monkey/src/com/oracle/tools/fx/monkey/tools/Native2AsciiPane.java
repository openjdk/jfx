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
import com.oracle.tools.fx.monkey.util.Native2Ascii;

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
            // FIX there is no easy way to add a tooltip to table column!!!
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
            c.setCellValueFactory((d) -> new SimpleStringProperty(d.getValue().ucode));
            c.setPrefWidth(100);
            table.getColumns().add(c);
        }
        {
            TableColumn<Entry, String> c = new TableColumn<>();
            c.setText("Type");
            c.setCellValueFactory((d) -> new SimpleStringProperty(d.getValue().type));
            c.setPrefWidth(300);
            table.getColumns().add(c);
        }
        {
            TableColumn<Entry, String> c = new TableColumn<>();
            c.setText("CodePoint");
            c.setCellValueFactory((d) -> new SimpleStringProperty(d.getValue().codePoint));
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
            String text = Native2Ascii.native2ascii(s);
            ascii.setText(text);
            updateSymbols(s);
        } else {
            String s = ascii.getText();
            String text = Native2Ascii.ascii2native(s);
            nat.setText(text);
            updateSymbols(text);
        }
        ignoreEvent = false;
    }

    protected void updateSymbols(String text) {
        int sz = text.length();
        ArrayList<Entry> es = new ArrayList<>(sz);
        for (int i = 0; i < sz; i++) {
            char c = text.charAt(i);
            int codePoint = -1;
            String cp = null;
            if (Character.isHighSurrogate(c)) {
                codePoint = text.codePointAt(i);
                cp = Character.toString(codePoint);
            }
            String ucode = String.format("%04X", (int)c);
            String desc = Character.getName(c);
            if (cp != null) {
                desc = String.format("%s (U+%06X %s)", desc, codePoint, Character.getName(codePoint));
            }
            String type = getType(c);
            Entry en = new Entry(i, String.valueOf(c), ucode, desc, cp, type);
            es.add(en);
        }
        table.getItems().setAll(es);
    }

    private String getType(char c) {
        int t = Character.getType(c);
        switch (t) {
        case Character.COMBINING_SPACING_MARK:
            return "COMBINING_SPACING_MARK";
        case Character.CONNECTOR_PUNCTUATION:
            return "CONNECTOR_PUNCTUATION";
        case Character.CONTROL:
            return "CONTROL";
        case Character.CURRENCY_SYMBOL:
            return "CURRENCY_SYMBOL";
        case Character.DASH_PUNCTUATION:
            return "DASH_PUNCTUATION";
        case Character.DECIMAL_DIGIT_NUMBER:
            return "DECIMAL_DIGIT_NUMBER";
        case Character.ENCLOSING_MARK:
            return "ENCLOSING_MARK";
        case Character.END_PUNCTUATION:
            return "END_PUNCTUATION";
        case Character.FINAL_QUOTE_PUNCTUATION:
            return "FINAL_QUOTE_PUNCTUATION";
        case Character.FORMAT:
            return "FORMAT";
        case Character.INITIAL_QUOTE_PUNCTUATION:
            return "INITIAL_QUOTE_PUNCTUATION";
        case Character.LETTER_NUMBER:
            return "LETTER_NUMBER";
        case Character.LINE_SEPARATOR:
            return "LINE_SEPARATOR";
        case Character.LOWERCASE_LETTER:
            return "LOWERCASE_LETTER";
        case Character.MATH_SYMBOL:
            return "MATH_SYMBOL";
        case Character.MODIFIER_LETTER:
            return "MODIFIER_LETTER";
        case Character.MODIFIER_SYMBOL:
            return "MODIFIER_SYMBOL";
        case Character.NON_SPACING_MARK:
            return "NON_SPACING_MARK";
        case Character.OTHER_LETTER:
            return "OTHER_LETTER";
        case Character.OTHER_NUMBER:
            return "OTHER_NUMBER";
        case Character.OTHER_PUNCTUATION:
            return "OTHER_PUNCTUATION";
        case Character.OTHER_SYMBOL:
            return "OTHER_SYMBOL";
        case Character.PARAGRAPH_SEPARATOR:
            return "PARAGRAPH_SEPARATOR";
        case Character.PRIVATE_USE:
            return "PRIVATE_USE";
        case Character.SPACE_SEPARATOR:
            return "SPACE_SEPARATOR";
        case Character.START_PUNCTUATION:
            return "START_PUNCTUATION";
        case Character.SURROGATE:
            return "SURROGATE";
        case Character.TITLECASE_LETTER:
            return "TITLECASE_LETTER";
        case Character.UNASSIGNED:
            return "UNASSIGNED";
        case Character.UPPERCASE_LETTER:
            return "UPPERCASE_LETTER";
        default:
            return String.valueOf(t);
        }
    }

    protected static class Entry {
        public final int index;
        public final String character;
        public final String ucode;
        public final String description;
        public final String codePoint;
        public final String type;

        public Entry(int index, String character, String ucode, String description, String codePoint, String type) {
            this.index = index;
            this.character = character;
            this.ucode = ucode;
            this.description = description;
            this.codePoint = codePoint;
            this.type = type;
        }
    }
}
