/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.FontSelector;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.PosSelector;
import com.oracle.tools.fx.monkey.util.Templates;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.TextSelector;

/**
 * TextField Page
 */
public class TextFieldPage extends TestPaneBase {
    private final TextField control;
    private final TextSelector textSelector;
    private final CheckBox inScroll;

    public TextFieldPage() {
        this(new TextField(), "TextFieldPage");
    }

    protected TextFieldPage(TextField control, String name) {
        FX.name(this, "TextFieldPage");

        this.control = control;
        control.setAlignment(Pos.CENTER_LEFT);

        textSelector = TextSelector.fromPairs(
            "textSelector",
            (t) -> {
                control.setText(t);
            },
            Templates.singleLineTextPairs()
        );

        FontSelector fontSelector = new FontSelector("font", control::setFont);

        PosSelector posSelector = new PosSelector(control::setAlignment);

        TextSelector promptChoice = Templates.promptChoice("promptChoice", control::setPromptText);

        ComboBox<Integer> prefColumnCount = new ComboBox<>();
        FX.name(prefColumnCount, "prefColumnCount");
        prefColumnCount.getItems().setAll(
            null,
            1,
            5,
            10,
            100,
            1000
        );
        prefColumnCount.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            Integer ct = prefColumnCount.getSelectionModel().getSelectedItem();
            int count = ct == null ? TextField.DEFAULT_PREF_COLUMN_COUNT : ct;
            control.setPrefColumnCount(count);
        });

        CheckBox editable = new CheckBox("editable");
        FX.name(editable, "editable");
        editable.selectedProperty().bindBidirectional(control.editableProperty());
        
        inScroll = new CheckBox("in scroll pane");
        FX.name(inScroll, "scroll");
        inScroll.setOnAction((ev) -> updateScroll());

        OptionPane op = new OptionPane();
        op.label("Text:");
        op.option(textSelector.node());
        op.option(editable);
        op.label("Font:");
        op.option(fontSelector.fontNode());
        op.label("Size:");
        op.option(fontSelector.sizeNode());
        op.label("Alignment:");
        op.option(posSelector.node());
        op.label("Prompt:");
        op.option(promptChoice.node());
        op.label("Preferred Column Count:");
        op.option(prefColumnCount);
        op.option(inScroll);

        setContent(control);
        setOptions(op);

        posSelector.select(Pos.BASELINE_RIGHT);
        fontSelector.selectSystemFont();
    }
    
    private void updateScroll() {
        if(inScroll.isSelected()) {
            ScrollPane sp = new ScrollPane(control);
            setContent(sp);
        } else {
            setContent(control);
        }
    }
}
