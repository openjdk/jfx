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
package com.oracle.tools.fx.monkey.tools;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.Utils;
import jfx.incubator.scene.control.richtext.RichTextArea;

/**
 * Clipboard Viewer
 */
public class ClipboardViewer extends BorderPane {

    private enum Mode {
        ASCII,
        HEX,
        IMAGE,
        TEXT,
    }

    private final RadioButton asciiMode;
    private final RadioButton hexMode;
    private final RadioButton imageMode;
    private final RadioButton textMode;
    private final TableView<Entry> table;
    private final Label classField;
    private final BorderPane detailPane;
    private final ToggleButton wrapButton;
    private RichTextArea textView;

    public ClipboardViewer() {
        FX.name(this, "ClipboardPage");

        ToggleGroup toggleGroup = new ToggleGroup();

        asciiMode = new RadioButton("ascii");
        FX.name(asciiMode, "asciiMode");
        asciiMode.setToggleGroup(toggleGroup);

        hexMode = new RadioButton("hex");
        FX.name(hexMode, "hexMode");
        hexMode.setToggleGroup(toggleGroup);

        imageMode = new RadioButton("image");
        FX.name(imageMode, "imageMode");
        imageMode.setToggleGroup(toggleGroup);

        textMode = new RadioButton("text");
        FX.name(textMode, "textMode");
        textMode.setToggleGroup(toggleGroup);
        textMode.setSelected(true);

        wrapButton = new ToggleButton("W");
        FX.name(wrapButton, "wrapButton");
        wrapButton.setTooltip(new Tooltip("wrap text"));

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        FX.name(table, "table");
        {
            TableColumn<Entry, String> c = new TableColumn<>();
            c.setText("Data Format");
            c.setCellValueFactory((f) -> {
                Entry t = f.getValue();
                if (t != null) {
                    return t.name;
                }
                return null;
            });
            table.getColumns().add(c);
        }
        //FX.setPopupMenu(table, this::createPopupMenu);

        Button reloadButton = FX.button("Reload", this::reload);

        ToolBar tp = new ToolBar(
            reloadButton,
            Utils.spacer(),
            textMode,
            asciiMode,
            hexMode,
            imageMode,
            Utils.spacer(),
            wrapButton
        );

        classField = new Label();
        classField.setPadding(new Insets(1, 10, 0, 10));
        classField.setAlignment(Pos.CENTER_RIGHT);
        classField.setMaxWidth(1e20);

        detailPane = new BorderPane();

        SplitPane split = new SplitPane(table, detailPane);
        split.setDividerPositions(100);
        FX.name(split, "split");

        setCenter(split);
        setTop(tp);
        setBottom(classField);

        table.getSelectionModel().selectedItemProperty().subscribe(this::showDetail);
        table.getSelectionModel().selectFirst();
        toggleGroup.selectedToggleProperty().subscribe(this::showDetail);

        reload();
    }

    private void showDetail() {
        Entry en = table.getSelectionModel().getSelectedItem();
        Node n = getViewer(en);
        if (detailPane.getCenter() != n) {
            detailPane.setCenter(n);
        }
        classField.setText(en == null ? null : en.getType());
    }

    private RichTextArea textView() {
        if (textView == null) {
            textView = new RichTextArea();
            textView.setEditable(false);
            textView.setHighlightCurrentParagraph(true);
            textView.wrapTextProperty().bind(wrapButton.selectedProperty());
        }
        return textView;
    }

    private Node getViewer(Entry en) {
        if (en == null) {
            return null;
        }

        Object data = en.data.get();
        if (en.error) {
            String trace = Utils.stackTrace((Throwable)data);
            textView().setModel(TextViewModel.ofText(Color.RED, "Not an image", false));
            return textView;
        }

        Mode m = getMode();
        switch (m) {
        case ASCII:
            {
                String text = asText(data);
                textView().setModel(TextViewModel.ofText(Color.BLACK, text, true));
            }
            return textView;
        case HEX:
            {
                byte[] b = asBytes(data);
                if (b == null) {
                    textView().setModel(TextViewModel.ofText(Color.RED, "Not a binary", false));
                } else {
                    textView().setModel(TextViewModel.ofBytes(b));
                }
            }
            return textView;
        case IMAGE:
            {
                Object d = en.data.get();
                if (d instanceof Image im) {
                    return new ScrollPane(new ImageView(im));
                } else {
                    textView().setModel(TextViewModel.ofText(Color.RED, "Not an image", false));
                    return textView;
                }
            }
        default:
            {
                String text = asText(data);
                textView().setModel(TextViewModel.ofText(Color.BLACK, text, false));
            }
            return textView;
        }
    }

    private static String asText(Object v) {
        if (v instanceof String s) {
            return s;
        }

        if (v instanceof byte[] b) {
            try {
                return new String(b, StandardCharsets.UTF_8);
            } catch (Throwable e) {
                return new String(b, StandardCharsets.US_ASCII);
            }
        }

        return v.toString();
    }

    private static byte[] asBytes(Object v) {
        if (v instanceof byte[] b) {
            return b;
        } else if (v instanceof ByteBuffer bb) {
            ByteBuffer buf = bb.asReadOnlyBuffer();
            buf.position(0);
            byte[] b = new byte[buf.limit()];
            buf.get(b);
            return b;
        } else if (v instanceof String s) {
            return s.getBytes(StandardCharsets.UTF_8);
        }
        return null;
    }

    private Mode getMode() {
        if (asciiMode.isSelected()) {
            return Mode.ASCII;
        } else if (hexMode.isSelected()) {
            return Mode.HEX;
        } else if (imageMode.isSelected()) {
            return Mode.IMAGE;
        }
        return Mode.TEXT;
    }

    private ContextMenu createPopupMenu() {
        ContextMenu m = new ContextMenu();
        // TODO copy as... bytes, image, etc.?
        FX.item(m, "Copy", this::copy);
        return m;
    }

    // TODO
    private void copy() {
        StringBuilder sb = null;
        List<Entry> sel = table.getSelectionModel().getSelectedItems();
        if (sb != null) {
            String text = sb.toString();
            ClipboardContent cc = new ClipboardContent();
            cc.putString(text);
            Clipboard.getSystemClipboard().setContent(cc);
        }
    }

    public void reload() {
        Clipboard c = Clipboard.getSystemClipboard();
        List<DataFormat> formats = new ArrayList<>(c.getContentTypes());
        Collections.sort(formats, new Comparator<DataFormat>() {
            @Override
            public int compare(DataFormat a, DataFormat b) {
                return a.toString().compareTo(b.toString());
            }
        });

        ArrayList<Entry> items = new ArrayList<>();
        for (DataFormat f: formats) {
            String name = getName(f);
            boolean error = false;
            Object data;
            try {
                data = c.getContent(f);
            } catch(Throwable e) {
                data = e;
                error = true;
            }
            items.add(new Entry(f, name, data, error));
        }

        table.getItems().setAll(items);
        table.getSelectionModel().selectFirst();
    }

    private static String getName(DataFormat f) {
        return f.toString();
    }

    private static String convert(Object x) {
        if (x == null) {
            return null;
        } else if (x instanceof byte[] b) {
            return Utils.hex(b, 0L);
        } else if(x instanceof ByteBuffer bb) {
            byte[] b = bb.array();
            return Utils.hex(b, 0L);
        }
        return x.toString();
    }

    private static class Entry {
        public final DataFormat format;
        public final SimpleStringProperty name;
        public final SimpleObjectProperty<Object> data;
        public final boolean error;

        public Entry(DataFormat f, String name, Object data, boolean error) {
            this.format = f;
            this.name = new SimpleStringProperty(name);
            this.data = new SimpleObjectProperty<>(data);
            this.error = error;
        }

        public String getType() {
            if (error) {
                return "ERROR";
            }
            Object d = data.get();
            return "Content: " + ((d == null) ? "<null>" : d.getClass());
        }
    }
}
