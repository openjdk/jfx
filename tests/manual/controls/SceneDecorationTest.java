/*
 *  Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SceneDecorationTest extends Application {
    private GridPane pane;

    public static void main(String[] args) {
        launch(SceneDecorationTest.class, args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        CheckBox showIcon = new CheckBox();
        showIcon.setSelected(true);
        CheckBox showTitle = new CheckBox();
        showTitle.setSelected(true);
        ComboBox<HPos> position = new ComboBox<>(FXCollections.observableArrayList(HPos.values()));
        position.getSelectionModel().select(HPos.RIGHT);

        Button fullScrn = new Button("Full Screen");
        fullScrn.setOnAction(e -> stage.setFullScreen(true));

        pane = new GridPane();
        addOption("Show Icon", showIcon);
        addOption("Show Title", showTitle);
        addOption("Buttons Pos", position);
        addOption("Full Scrn", fullScrn);

        SceneDecoration decoration = new SceneDecoration(stage, pane);
        pane.prefWidthProperty().bind(decoration.widthProperty().multiply(0.80));

        var backButton = getBackButton();
        backButton.getStyleClass().add("left-pill");

        var forwardButton = getForwardButton();
        forwardButton.getStyleClass().add("center-pill");

        var homeButton = getHomeButton();
        homeButton.getStyleClass().add("right-pill");
        HBox navButtons = new HBox(0, backButton, forwardButton, homeButton);

        decoration.showIconProperty().bind(showIcon.selectedProperty());
        decoration.showTitleProperty().bind(showTitle.selectedProperty());
        decoration.headerButtonsPositionProperty().bind(position.valueProperty());
        decoration.setHeaderLeft(getHamburgerButton());
        decoration.setHeaderRight(navButtons);

        var scene = new Scene(decoration, Color.TRANSPARENT);
        String css = getClass().getClassLoader().getResource("tests/manual/controls/decoration.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setScene(scene);
        stage.getIcons().add(new Image("https://openjdk.org/images/duke-thinking.png"));
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("Test Stage");
        stage.setWidth(600);
        stage.setHeight(400);
        stage.show();
    }

    private Button getHamburgerButton() {
        return getButton("M1,2h14c0.553,0,1-0.448,1-1s-0.447-1-1-1H1C0.448,0,0,0.448,0,1S0.448,2,1,2zM15,5.875H1c-0.552,0-1,0.448-1,1c0,0.553,0.448,1,1,1h14c0.553,0,1-0.447,1-1C16,6.323,15.553,5.875,15,5.875z M15,11.75H1c-0.552,0-1,0.447-1,1s0.448,1,1,1h14c0.553,0,1-0.447,1-1S15.553,11.75,15,11.75z");
    }

    private Button getBackButton() {
        return getButton("M0,7L11,0L11,14Z");
    }
    private Button getForwardButton() {
        return getButton("M0,0L11,7L0,14Z");
    }

    private Button getHomeButton() {
        return getButton("M1,8.239V14h3.5V7.5h4V14H12V8.239L6.5,3L1,8.239z M11,4.5V1H9.1L9.062,2.448L6.5,0L0,6.5v0.7h0.5l6-5.826l6,5.826H13V6.5L11,4.5z");
    }

    private Button getButton(String shape) {
        Button btn = new Button();

        var svg = new SVGPath();
        svg.setContent(shape);
        final StackPane svgShape = new StackPane();
        svgShape.setScaleShape(true);
        svgShape.setShape(svg);
        svgShape.setPrefSize(16, 16);
        svgShape.setBackground(Background.fill(Color.BLACK));

        btn.setGraphic(svgShape);

        return btn;
    }

    int currentRow = 0;
    private void addOption(String lbl, Node opt) {
        pane.add(new Label(lbl), 0, currentRow);
        pane.add(opt, 1, currentRow);

        currentRow++;
    }
}
