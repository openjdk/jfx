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
package com.oracle.tools.fx.monkey.pages;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import com.oracle.tools.fx.monkey.tools.ClipboardViewer;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.ImageTools;
import com.oracle.tools.fx.monkey.util.NamedValue;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * Clipboard Page.
 */
public class ClipboardPage extends TestPaneBase {
    private final ComboBox<Object> typeField;
    private final ComboBox<NamedValue<Object>> dataField;

    public ClipboardPage() {
        super("ClipboardPage");

        ClipboardViewer viewer = new ClipboardViewer();

        typeField = new ComboBox<>();
        FX.name(typeField, "typeField");
        typeField.getItems().setAll(listTypes());
        typeField.setEditable(true);

        dataField = new ComboBox<>();
        FX.name(dataField, "dataField");
        dataField.getItems().setAll(listData());

        Button button = new Button("Copy to Clipboard");
        button.setOnAction((ev) -> {
            copy();
            viewer.reload();
        });

        GridPane g = new GridPane(10, 5);
        g.setPadding(new Insets(10));
        g.add(new Label("Mime Type:"), 0, 0);
        g.add(typeField, 1, 0);
        g.add(new Label("Data:"), 0, 1);
        g.add(dataField, 1, 1);
        g.add(button, 2, 1);

        BorderPane p = new BorderPane();
        p.setTop(g);
        p.setCenter(viewer);

        setContent(p);
    }

    private Object[] listTypes() {
        return new Object[] {
            new NamedValue<>("DataFormat.FILES", DataFormat.FILES),
            new NamedValue<>("DataFormat.HTML", DataFormat.HTML),
            new NamedValue<>("DataFormat.IMAGE", DataFormat.IMAGE),
            new NamedValue<>("DataFormat.PLAIN_TEXT", DataFormat.PLAIN_TEXT),
            new NamedValue<>("DataFormat.RTF", DataFormat.RTF),
            new NamedValue<>("DataFormat.URL", DataFormat.URL),
            "application/json",
            "application/octet-stream",
            "text/css",
            "text/html",
            "text/javascript"
        };
    }

    private List<NamedValue<Object>> listData() {
        ArrayList<NamedValue<Object>> v = new ArrayList<>();
        v.add(new NamedValue<>("Text", """
            {
                "type": "json",
                "text": "here is some text"
            }
            """));
        v.add(new NamedValue<>("Byte Array", new byte[] { 0x01, 0x02, 0x03 }));
        v.add(new NamedValue<>("Byte Buffer", ByteBuffer.wrap(new byte[] { 0x04, 0x05, 0x06 })));
        v.add(new NamedValue<>("PNG Image", sup(this::pngImage)));
        v.add(new NamedValue<>("File List", sup(this::fileList)));
        v.add(new NamedValue<>("String[]", sup(this::stringArray)));
        v.add(new NamedValue<>("<null>", null));
        return v;
    }

    private static DataFormat parseFormat(Object x) {
        if (x instanceof NamedValue n) {
            x = n.getValue();
        }

        if (Utils.isBlank(x)) {
            return null;
        } else if (x instanceof DataFormat f) {
            return f;
        }

        // unbelievable!
        // new DataFormat() throws an exception if some other code has created the same data format earlier
        String mime = x.toString();
        synchronized (DataFormat.class) {
            DataFormat f = DataFormat.lookupMimeType(mime);
            if (f != null) {
                return f;
            }
            return new DataFormat(mime);
        }
    }

    private Object parseData(NamedValue x) {
        if (x == null) {
            return null;
        }
        Object v = x.getValue();
        if (v instanceof Supplier sup) {
            return sup.get();
        }
        return v;
    }

    private static Supplier<Object> sup(Supplier<Object> sup) {
        return sup;
    }

    private List<File> fileList() {
        ArrayList<File> v = new ArrayList<>();
        v.add(new File("."));
        v.add(new File(".."));
        return v;
    }

    private Object pngImage() {
        try {
            Image im = ImageTools.createImage(100, 100);
            return ImageTools.writePNG(im);
        } catch (IOException e) {
            // should not happen
            e.printStackTrace();
            return null;
        }
    }

    private String[] stringArray() {
        return new String[] {
            "string1",
            "string2",
            "string3"
        };
    }

    private void copy() {
        try {
            Object x = typeField.getSelectionModel().getSelectedItem();
            DataFormat f = parseFormat(x);
            if (f == null) {
                return;
            }
            NamedValue v = dataField.getSelectionModel().getSelectedItem();
            Object value = parseData(v);
            HashMap<DataFormat, Object> content = new HashMap<>();
            content.put(f, value);
            Clipboard.getSystemClipboard().setContent(content);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
