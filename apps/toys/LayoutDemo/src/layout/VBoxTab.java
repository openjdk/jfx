/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package layout;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class VBoxTab extends Tab {

    final VBox vbox = new VBox(10); // 10px spacing
    final Button okBtn = new Button("OK");
    final Button cancelBtn = new Button("Cancel");

    public VBoxTab(String text) {
        this.setText(text);
        init();
    }

    public void init() {

        final Label descLbl = new Label("Description:");
        final TextArea desc = new TextArea();
        desc.setPrefColumnCount(10);
        desc.setPrefRowCount(3);
        desc.setWrapText(true);

        vbox.getChildren().addAll(descLbl, desc, okBtn, cancelBtn);
        vbox.getStyleClass().add("layout");
        final BorderPane root = new BorderPane(vbox);

        final CheckBox vGrowCbx = new CheckBox("TextArea Vgrow");

        vGrowCbx.setSelected(false);
        vGrowCbx.setOnAction(e-> growVertical(desc, vGrowCbx.isSelected()));

        CheckBox maxSizeButtonCbx = new CheckBox("Button Max Size");
        maxSizeButtonCbx.setOnAction(e -> maxSizeButton(maxSizeButtonCbx.isSelected()));

        CheckBox fillWidthCbx = new CheckBox("Fill Width");
        // HBox's fillWidth is true by default.
        fillWidthCbx.setSelected(true);

        // Add an event handler to the CheckBox, so the user can set the
        // fillHeight property using the CheckBox
        fillWidthCbx.setOnAction(e
                -> vbox.setFillWidth(fillWidthCbx.isSelected()));

        ChoiceBox<Pos> alignmentCBox = new ChoiceBox<>();
        alignmentCBox.getItems().addAll(
                Pos.BASELINE_CENTER, Pos.BASELINE_LEFT, Pos.BASELINE_RIGHT,
                Pos.BOTTOM_CENTER, Pos.BOTTOM_LEFT, Pos.BOTTOM_RIGHT,
                Pos.CENTER, Pos.CENTER_LEFT, Pos.CENTER_RIGHT,
                Pos.TOP_CENTER, Pos.TOP_LEFT, Pos.TOP_RIGHT);

        alignmentCBox.getSelectionModel().select(vbox.getAlignment());

        // Add ChangeListeners to track change in selected index and item. Only
        // one listener is necessary if you want to track change in selection
        alignmentCBox.getSelectionModel().selectedItemProperty().addListener(this::itemChanged);
        alignmentCBox.getSelectionModel().selectedIndexProperty().addListener(this::indexChanged);

        Label alignmentLabel = new Label("Alignment");
        HBox controlGrp = new HBox(alignmentLabel, alignmentCBox,
                fillWidthCbx, vGrowCbx, maxSizeButtonCbx);
        controlGrp.getStyleClass().add("control");
        controlGrp.setAlignment(Pos.CENTER_LEFT);
        root.setTop(controlGrp);

        this.setContent(root);
    }

    void maxSizeButton(boolean stretch) {
        if (stretch) {
            // Let the Cancel button expand vertically
            okBtn.setMaxWidth(Double.MAX_VALUE);
            cancelBtn.setMaxWidth(Double.MAX_VALUE);
        } else {
            okBtn.setMaxWidth(okBtn.getPrefWidth());
            cancelBtn.setMaxWidth(cancelBtn.getPrefWidth());
        }

    }

    void growVertical(TextArea desc, boolean grow) {
        if (grow) {
            VBox.setVgrow(desc, Priority.ALWAYS);
        } else {
            VBox.setVgrow(desc, Priority.NEVER);
        }
    }

    // A change listener to track the change in selected item
    public void itemChanged(ObservableValue<? extends Pos> observable,
            Pos oldValue,
            Pos newValue) {
//        System.out.println("Itemchanged: old = " + oldValue + ", new = " + newValue);
        vbox.setAlignment(newValue);
    }

    // A change listener to track the change in selected index
    public void indexChanged(ObservableValue<? extends Number> observable,
            Number oldValue,
            Number newValue) {
//        System.out.println("Indexchanged: old = " + oldValue + ", new = " + newValue);
    }

}
