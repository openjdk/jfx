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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;
import javafx.util.converter.DoubleStringConverter;

/**
 * Font Picker Pane.
 */
public class FontPickerPane extends GridPane {
    private final static boolean allowLogical = false;
    private final ObjectProperty<Font> prop;
    private final boolean allowNull;
    // TODO editable combo box w/list of previously selected fonts (font + style + size)
    private final TextField patternField;
    private final ListView<String> familyField = new ListView<>();
    private final ListView<NamedValue<String>> styleField = new ListView<>();
    private final ComboBox<Double> sizeField = new ComboBox<>();
    private final Label sample;
    private final List<String> fonts;

    public FontPickerPane(ObjectProperty<Font> prop, boolean allowNull, Runnable onCompletion) {
        this.prop = prop;
        this.allowNull = allowNull;

        fonts = collectFonts(allowNull);

        patternField = new TextField();
        patternField.addEventFilter(KeyEvent.ANY, (_) -> {
            handleKeyPress();
        });
        patternField.addEventHandler(MouseEvent.MOUSE_PRESSED, (_) -> {
            if (!patternField.isFocused()) {
                Platform.runLater(() -> {
                    patternField.selectAll();
                });
            }
        });

        familyField.getItems().setAll(fonts);
        familyField.getSelectionModel().selectedItemProperty().addListener((_, _, v) -> {
            setFamily(v);
        });

        styleField.getSelectionModel().selectedItemProperty().addListener((_, _, v) -> {
            setStyle(v);
        });

        sizeField.setEditable(true);
        sizeField.setConverter(new DoubleStringConverter());
        sizeField.getItems().setAll(
            8.0,
            9.0,
            10.0,
            11.0,
            12.0,
            13.0,
            14.0,
            16.0,
            18.0,
            24.0,
            32.0,
            48.0,
            72.0
        );
        sizeField.valueProperty().addListener((_,_,_) -> {
            updatePreview();
        });

        sample = new Label("Brown fox jumped over a lazy dog.\n01234567890");
        sample.setMaxWidth(Double.MAX_VALUE);
        //sample.setBackground(Background.fill(Color.WHITE));
        sample.setAlignment(Pos.TOP_LEFT);
        sample.setPadding(new Insets(5));
        ScrollPane scroll = new ScrollPane(sample);
        scroll.setMinHeight(80);
        scroll.setMaxHeight(80);

        scroll.setBackground(Background.fill(Color.WHITE));

        Button ok = new Button("OK");
        ButtonBar.setButtonData(ok, ButtonData.OK_DONE);
        ok.setOnAction((_) -> {
            pickFont();
            onCompletion.run();
        });
        Button cancel = new Button("Cancel");
        cancel.setOnAction((_) -> {
            onCompletion.run();
        });
        ButtonBar.setButtonData(cancel, ButtonData.CANCEL_CLOSE);
        ButtonBar bb = new ButtonBar();
        bb.getButtons().setAll(ok, cancel);

        // layout
        setPrefHeight(350);
        setPrefWidth(500);
        setHgap(5);
        setVgap(5);
        setBackground(Background.fill(Color.LIGHTGRAY));
        setPadding(new Insets(5));
        setFocusTraversable(true);

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setPercentWidth(75.0);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(25.0);
        getColumnConstraints().addAll(c0, c1);

        RowConstraints r0 = new RowConstraints();
        RowConstraints r1 = new RowConstraints();
        r1.setFillHeight(true);
        RowConstraints r2 = new RowConstraints();
        getRowConstraints().addAll(r0, r1, r2);

        add(patternField, 0, 0);
        add(sizeField, 1, 0);
        add(familyField, 0, 1);
        add(styleField, 1, 1);
        add(scroll, 0, 2, 2, 1);
        add(bb, 0, 3, 2, 1);

        setFont(prop.get());
    }

    // TODO there is an alternative: open a resizeable dialog/owned window instead
    public Popup createPopup() {
        setStyle("""
            -fx-background-color: -fx-outer-border, -fx-body-color;
            -fx-background-insets: 0, 1;
            -fx-padding: 1em 1em 1em 1em;
            -fx-effect: dropshadow( gaussian , rgba(0,0,0,0.2) , 12, 0.0 , 0 , 8 );
            """);

        Popup p = new Popup();
        p.setAnchorLocation(AnchorLocation.WINDOW_TOP_LEFT);
        p.setAutoHide(true);
        p.getContent().add(this);
        p.setOnShown((ev) -> {
            patternField.requestFocus();
        });
        return p;
    }

    private void handleKeyPress() {
        // TODO delayed action: filter, set all
        String pattern = patternField.getText().toLowerCase(Locale.ROOT);
        ArrayList<String> fs = new ArrayList<>(fonts.size());
        for (String s : fonts) {
            if (s.toLowerCase(Locale.ROOT).contains(pattern)) {
                fs.add(s);
            }
        }
        familyField.getItems().setAll(fs);
    }

    private void setFamily(String name) {
        List<NamedValue<String>> ss = collectStyles(name);
        styleField.getItems().setAll(ss);
        if (ss.size() > 0) {
            int ix = guessPlain(ss);
            styleField.getSelectionModel().select(ix);
        }
        // TODO update editor?
        updatePreview();
    }

    private void setStyle(NamedValue<String> v) {
        updatePreview();
    }

    private void updatePreview() {
        Font f = getCurrentFont();
        //System.out.println(f);
        sample.setFont(f);
    }

    private static int guessPlain(List<NamedValue<String>> styles) {
        int sz = styles.size();
        if (sz > 1) {
            for (int i = 0; i < sz; i++) {
                NamedValue<String> v = styles.get(i);
                if (isPlain(v.getDisplay())) {
                    return i;
                }
            }
        }
        return 0;
    }

    private static boolean isPlain(String style) {
        switch(style.toLowerCase(Locale.ROOT)) {
        case "regular":
        case "plain":
            return true;
        }
        return false;
    }

    private void setFont(Font f) {
        if (f == null) {
            if (allowNull) {
                familyField.getSelectionModel().select(null);
                patternField.setText(null);
            }
        } else {
            String name = f.getName();
            String fam = f.getFamily();
            double sz = f.getSize();

            familyField.getSelectionModel().select(fam);
            int ix = indexByValue(styleField.getItems(), name);
            if (ix >= 0) {
                styleField.getSelectionModel().select(ix);
            }

            ix = indexOf(sizeField.getItems(), sz);
            if (ix >= 0) {
                sizeField.getSelectionModel().select(ix);
            }

            patternField.setText(Formats.font(f));
        }
    }

    private String getCurrentFamily() {
        return familyField.getSelectionModel().getSelectedItem();
    }

    private double getCurrentSize() {
        Double v = sizeField.getValue();
        return v == null ? defaultFontSize() : v;
    }

    private static int indexByValue(List<NamedValue<String>> items, String value) {
        int sz = items.size();
        for (int i = 0; i < sz; i++) {
            NamedValue<String> v = items.get(i);
            String s = v.getValue();
            if (Utils.eq(s, value)) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOf(List<Double> items, Double value) {
        if (value != null) {
            int sz = items.size();
            for (int i = 0; i < sz; i++) {
                double x = items.get(i);
                if (Math.abs(x - value) < 0.005) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String getDisplayValue(Object x) {
        if(x == null) {
            return null;
        } else if(x instanceof NamedValue v) {
            return v.getDisplay();
        } else {
            return x.toString();
        }
    }

    private static List<String> collectFonts(boolean allowNull) {
        ArrayList<String> rv = new ArrayList<>();
        if (allowNull) {
            rv.add(0, null);
        }
        if (allowLogical) {
            rv.add("Cursive");
            rv.add("Fantasy");
            rv.add("Monospace");
            rv.add("Sans-serif");
            rv.add("Serif");
            rv.add("System");
        }
        rv.addAll(Font.getFamilies());
        sort(rv);
        return rv;
    }

    private static List<NamedValue<String>> collectStyles(String family) {
        if (Utils.isBlank(family)) {
            return List.of();
        }

        List<String> ss = Font.getFontNames(family);
        int sz = ss.size();
        ArrayList<NamedValue<String>> rv = new ArrayList<>(sz);
        for (int i = 0; i < sz; i++) {
            String s = ss.get(i);
            String display = parseDisplayStyle(family, s);
            rv.add(new NamedValue<>(display, s));
        }
        sort(rv);
        return rv;
    }

    private static void sort(List<?> items) {
        Collator coll = Collator.getInstance(Locale.ROOT);
        Collections.sort(items, new Comparator<Object>() {
            @Override
            public int compare(Object a, Object b) {
                String sa = toString(a);
                String sb = toString(b);
                return coll.compare(sa, sb);
            }

            private static String toString(Object x) {
                if (x == null) {
                    return "";
                } else if (x instanceof NamedValue v) {
                    return v.getDisplay();
                } else {
                    return x.toString();
                }
            }
        });
    }

    private static String parseDisplayStyle(String family, String s) {
        if (s.startsWith(family)) {
            s = s.substring(family.length()).trim();
        }
        if (Utils.isBlank(s)) {
            return "Regular";
        }
        return s;
    }

    private static double defaultFontSize() {
        return Font.getDefault().getSize();
    }

    private void pickFont() {
        Font f = getCurrentFont();
        prop.set(f);
    }

    public Font getCurrentFont() {
        String fm = getCurrentFamily();
        if (Utils.isBlank(fm)) {
            return allowNull ? null : Font.getDefault();
        } else {
            NamedValue<String> v = styleField.getSelectionModel().getSelectedItem();
            if (v == null) {
                return Font.getDefault();
            }

            String name = v.getValue();
            double sz = getCurrentSize();
            return new Font(name, sz);
        }
    }

    public void requestPatternFieldFocus() {
        patternField.requestFocus();
    }
}
