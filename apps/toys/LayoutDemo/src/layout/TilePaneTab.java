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
import javafx.geometry.NodeOrientation;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;

public class TilePaneTab extends Tab {

    int sizeArr[] = {75, 100, 150, 200};
    double hgap = 5;
    double vgap = 5;
    TilePane tilePane = new TilePane(hgap, vgap);

    public TilePaneTab(String text) {
        this.setText(text);
        init();
    }

    public void init() {

        // Add photos to the flow pane
        for (int i = 1; i < 16; i++) {
            String imageStr = "resources/images/squares" + i + ".jpg";
            int index = i % sizeArr.length;
            tilePane.getChildren().add(new ImageView(new Image(imageStr,
                    sizeArr[index], sizeArr[index], true, true)));
        }

        tilePane.getStyleClass().add("layout");

        BorderPane root = new BorderPane(tilePane);

        Label orientationLabel = new Label("Orientation");
        ChoiceBox<Orientation> orientationCBox = new ChoiceBox<>();
        orientationCBox.getItems().addAll(Orientation.HORIZONTAL, Orientation.VERTICAL);
        orientationCBox.getSelectionModel().select(tilePane.getOrientation());
        orientationCBox.getSelectionModel().selectedItemProperty().addListener(this::orientationChanged);

        Label nodeOrientationLabel = new Label("Node Orientation");
        ChoiceBox<NodeOrientation> nodeOrientationCBox = new ChoiceBox<>();
        nodeOrientationCBox.getItems().addAll(NodeOrientation.INHERIT,
                NodeOrientation.LEFT_TO_RIGHT, NodeOrientation.RIGHT_TO_LEFT);
        nodeOrientationCBox.getSelectionModel().select(tilePane.getNodeOrientation());
        nodeOrientationCBox.getSelectionModel().selectedItemProperty().addListener(this::nodeOrientationChanged);

        Label childAlignmentLabel = new Label("Child Alignment");
        ChoiceBox<Pos> childAlignmentCBox = new ChoiceBox<>();
        childAlignmentCBox.getItems().addAll(null,
                Pos.BASELINE_CENTER, Pos.BASELINE_LEFT, Pos.BASELINE_RIGHT,
                Pos.BOTTOM_CENTER, Pos.BOTTOM_LEFT, Pos.BOTTOM_RIGHT,
                Pos.CENTER, Pos.CENTER_LEFT, Pos.CENTER_RIGHT,
                Pos.TOP_CENTER, Pos.TOP_LEFT, Pos.TOP_RIGHT);

        childAlignmentCBox.getSelectionModel().select(TilePane.getAlignment(tilePane.getChildren().get(0)));
        childAlignmentCBox.getSelectionModel().selectedItemProperty().addListener(this::childAlignmentChanged);

        Label alignmentLabel = new Label("Alignment");
        ChoiceBox<Pos> alignmentCBox = new ChoiceBox<>();
        alignmentCBox.getItems().addAll(
                Pos.BASELINE_CENTER, Pos.BASELINE_LEFT, Pos.BASELINE_RIGHT,
                Pos.BOTTOM_CENTER, Pos.BOTTOM_LEFT, Pos.BOTTOM_RIGHT,
                Pos.CENTER, Pos.CENTER_LEFT, Pos.CENTER_RIGHT,
                Pos.TOP_CENTER, Pos.TOP_LEFT, Pos.TOP_RIGHT);

        alignmentCBox.getSelectionModel().select(tilePane.getAlignment());
        alignmentCBox.getSelectionModel().selectedItemProperty().addListener(this::alignmentChanged);

        HBox controlGrp = new HBox(alignmentLabel, alignmentCBox,
                orientationLabel, orientationCBox,
                nodeOrientationLabel, nodeOrientationCBox,
                childAlignmentLabel, childAlignmentCBox);

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
        tilePane.setAlignment(newValue);
    }

    // A change listener to track the change in selected item
    public void orientationChanged(ObservableValue<? extends Orientation> observable,
            Orientation oldValue,
            Orientation newValue) {
//        System.out.println("orientationChanged: old = " + oldValue + ", new = " + newValue);
        tilePane.setOrientation(newValue);
    }

    // A change listener to track the change in selected item
    public void nodeOrientationChanged(ObservableValue<? extends NodeOrientation> observable,
            NodeOrientation oldValue,
            NodeOrientation newValue) {
//        System.out.println("nodeOrientationChanged: old = " + oldValue + ", new = " + newValue);
        tilePane.setNodeOrientation(newValue);
    }

    // A change listener to track the change in selected item
    public void childAlignmentChanged(ObservableValue<? extends Pos> observable,
            Pos oldValue,
            Pos newValue) {
//        System.out.println("Itemchanged: old = " + oldValue + ", new = " + newValue);
        tilePane.setTileAlignment(newValue);

    }

}
