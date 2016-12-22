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
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class StackPaneTab extends Tab {

    final StackPane stackPane = new StackPane();
    final Button btn = new Button("Button");

    public StackPaneTab(String text) {
        this.setText(text);
        init();
    }

    public void init() {

        Pane pane = new Pane();
        pane.setPrefSize(400, 300);
        pane.setStyle("-fx-background-color: green, white;"
                + "-fx-background-insets: 0, 4;"
                + "-fx-background-radius: 4, 2;");
        pane.setMaxSize(pane.getPrefWidth(), pane.getPrefHeight());
        stackPane.getChildren().addAll(pane, btn);
        stackPane.getStyleClass().add("layout");

        BorderPane root = new BorderPane(stackPane);

        Label alignmentLabel = new Label("Alignment");
        ChoiceBox<Pos> alignmentCBox = new ChoiceBox<>();
        alignmentCBox.getItems().addAll(
                Pos.BASELINE_CENTER, Pos.BASELINE_LEFT, Pos.BASELINE_RIGHT,
                Pos.BOTTOM_CENTER, Pos.BOTTOM_LEFT, Pos.BOTTOM_RIGHT,
                Pos.CENTER, Pos.CENTER_LEFT, Pos.CENTER_RIGHT,
                Pos.TOP_CENTER, Pos.TOP_LEFT, Pos.TOP_RIGHT);
        alignmentCBox.getSelectionModel().select(stackPane.getAlignment());
        alignmentCBox.getSelectionModel().selectedItemProperty().addListener(this::alignmentChanged);


        Label childAlignmentLabel = new Label("Button Alignment");
        ChoiceBox<Pos> childAlignmentCBox = new ChoiceBox<>();
        childAlignmentCBox.getItems().addAll(null,
                Pos.BASELINE_CENTER, Pos.BASELINE_LEFT, Pos.BASELINE_RIGHT,
                Pos.BOTTOM_CENTER, Pos.BOTTOM_LEFT, Pos.BOTTOM_RIGHT,
                Pos.CENTER, Pos.CENTER_LEFT, Pos.CENTER_RIGHT,
                Pos.TOP_CENTER, Pos.TOP_LEFT, Pos.TOP_RIGHT);
        childAlignmentCBox.getSelectionModel().select(StackPane.getAlignment(btn));
        childAlignmentCBox.getSelectionModel().selectedItemProperty().addListener(this::childAlignmentChanged);

        HBox controlGrp = new HBox(alignmentLabel, alignmentCBox, childAlignmentLabel, childAlignmentCBox);
        controlGrp.getStyleClass().add("control");
        controlGrp.setAlignment(Pos.CENTER_LEFT);
        root.setTop(controlGrp);

        this.setContent(root);
    }

    // A change listener to track the change in selected item
    public void alignmentChanged(ObservableValue<? extends Pos> observable,
            Pos oldValue,
            Pos newValue) {
//        System.out.println("Itemchanged: old = " + oldValue + ", new = " + newValue);
        stackPane.setAlignment(newValue);
    }

    // A change listener to track the change in selected item
    public void childAlignmentChanged(ObservableValue<? extends Pos> observable,
            Pos oldValue,
            Pos newValue) {
//        System.out.println("Itemchanged: old = " + oldValue + ", new = " + newValue);
        StackPane.setAlignment(btn, newValue);
    }
}
