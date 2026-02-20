/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.demo.richtext.editor;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.oracle.demo.richtext.util.FX;
import com.oracle.demo.richtext.util.Utils;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.model.RichTextModel;

/**
 * Tabs settings dialog.
 *
 * @author Andy Goryachev
 */
public class TabsDialog extends Stage {

    private final RichTextArea editor;
    private final ComboBox<Double> defaultInterval;

    public TabsDialog(RichTextArea editor) {
        this.editor = editor;
        initOwner(FX.getParentWindow(editor));
        initModality(Modality.APPLICATION_MODAL);
        FX.name(this, "TabsDialog");

        defaultInterval = Utils.numberField(null, 100.0, 200.0);

        GridPane g = new GridPane();
        g.getColumnConstraints().setAll(
            FX.cc().fixed(20),
            FX.cc(),
            FX.cc().fill()
        );
        g.setHgap(10);
        g.setVgap(10);
        g.setMaxWidth(Double.MAX_VALUE);
        int r = 0;
        g.add(Utils.heading("Tabs"), 0, r, 3, 1);
        r++;
        g.add(new Label("Default interval:"), 1, r);
        g.add(defaultInterval, 2, r);

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction((_) -> hide());
        ButtonBar.setButtonData(cancelButton, ButtonData.CANCEL_CLOSE);

        Button okButton = new Button("OK");
        okButton.setOnAction((_) -> commit());
        okButton.setDefaultButton(true);
        ButtonBar.setButtonData(okButton, ButtonData.OK_DONE);

        ButtonBar bb = new ButtonBar();
        bb.getButtons().setAll(cancelButton, okButton);

        BorderPane bp = new BorderPane();
        bp.setPadding(new Insets(10));
        bp.setCenter(g);
        bp.setBottom(bb);

        Scene scene = new Scene(bp, 500, 300);

        setScene(scene);
        setTitle("Tabs");
        Utils.closeOnEscape(this);
        Utils.centerInWindow(this);

        load();
    }

    private RichTextModel model() {
        return (RichTextModel)editor.getModel();
    }

    private void load() {
        double v = model().getDefaultTabStops();
        defaultInterval.setValue(v);
    }

    private void commit() {
        Double v = defaultInterval.getValue();
        model().setDefaultTabStops(v == null ? 0.0 : v);

        hide();
    }
}
