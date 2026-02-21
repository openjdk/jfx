/*
 * Copyright (c) 2025 Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.ColorScheme;
import javafx.application.Platform.Preferences;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HeaderBar;
import javafx.scene.paint.Color;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.StageBackdrop;
import javafx.stage.Window;

public class BackdropTest extends Application {
    public static void main(String[] args) {
        launch(BackdropTest.class, args);
    }

    private enum StageStyleChoice {
        DECORATED("Decorated", StageStyle.DECORATED),
        UNDECORATED("Undecorated", StageStyle.UNDECORATED),
        EXTENDED("Extended", StageStyle.EXTENDED),
        UTILITY("Utility", StageStyle.UTILITY),
        TRANSPARENT("Transparent", StageStyle.TRANSPARENT),
        UNIFIED("Unified", StageStyle.UNIFIED);

        private String label;
        private StageStyle stageStyle;

        StageStyleChoice(String label, StageStyle style) {
            this.label = label;
            this.stageStyle = style;
        }

        public StageStyle getStageStyle() {
            return stageStyle;
        }

        public String toString() {
            return label;
        }
    }

    private enum StageBackdropChoice {
        DEFAULT("Default", StageBackdrop.DEFAULT),
        WINDOW("Window", StageBackdrop.WINDOW),
        TABBED("Tabbed", StageBackdrop.TABBED),
        TRANSIENT("Transient", StageBackdrop.TRANSIENT);

        private String label;
        private StageBackdrop backdrop;

        StageBackdropChoice(String label, StageBackdrop backdrop) {
            this.label = label;
            this.backdrop = backdrop;
        }

        public StageBackdrop getBackdrop() {
            return backdrop;
        }

        public String toString() {
            return label;
        }
    }

    private enum FillChoice {
        NONE("None", null),
        TRANSPARENT("Transparent", Color.TRANSPARENT),
        BLUE("Light blue", Color.LIGHTBLUE),
        TRANSLUCENT_RED("Red (50% opaque)", new Color(1.0, 0.0, 0.0, 0.5)),
        TRANSLUCENT_GREEN("Green (20% opaque)", new Color(0.0, 1.0, 0.0, 0.2));

        private String label;
        private Color color;

        FillChoice(String label, Color color) {
            this.label = label;
            this.color = color;
        }

        Color getFill() {
            return color;
        }

        public String toString() {
            return label;
        }
    }

    private enum ColorSchemeChoice {
        LIGHT("Light", ColorScheme.LIGHT),
        DARK("Dark", ColorScheme.DARK);

        private String label;
        private ColorScheme scheme;

        ColorSchemeChoice(String label, ColorScheme scheme) {
            this.label = label;
            this.scheme = scheme;
        }

        ColorScheme getColorScheme() {
            return scheme;
        }

        public String toString() {
            return label;
        }
    }

    private enum OpacityChoice {
        P100("100%", 1.0),
        P75("75%", 0.75),
        P50("50%", 0.50);

        private String label;
        private double opacity;

        OpacityChoice(String label, double opacity) {
            this.label = label;
            this.opacity = opacity;
        }

        double getOpacity() {
            return opacity;
        }

        public String toString() {
            return label;
        }
    }

    private Label newLabel(String text) {
        var label = new Label(text);
        label.textFillProperty().bind(Platform.getPreferences().foregroundColorProperty());
        return label;
    }

    private Parent labeledSection(String text, Parent section) {
        var label = newLabel(text);
        VBox box = new VBox(label, section);
        box.setSpacing(5);
        return box;
    }

    private Parent labeledSection(String text) {
        var label = newLabel(text);
        VBox box = new VBox(label);
        box.setSpacing(5);
        return box;
    }

    private void buildScene(Stage stage, StageStyleChoice stageStyle, StageBackdropChoice backdrop) {

        var iconifyButton = new Button("Iconify");
        iconifyButton.setOnAction(e -> {
            stage.setIconified(!stage.isIconified());
        });

        var maximizeButton = new Button("Maximize");
        maximizeButton.setOnAction(e -> {
            stage.setMaximized(!stage.isMaximized());
        });

        var fullscreenButton = new Button("Fullscreen");
        fullscreenButton.setOnAction(e -> {
            stage.setFullScreen(!stage.isFullScreen());
        });

        var closeButton = new Button("Close");
        closeButton.setOnAction(e -> {
            stage.close();
        });

        var actionButtons = new HBox(fullscreenButton, maximizeButton, iconifyButton, closeButton);
        actionButtons.setSpacing(5);
        actionButtons.setAlignment(Pos.BASELINE_RIGHT);

        // For creating new stages
        var styleLabel = newLabel("Style");
        ChoiceBox<StageStyleChoice> stageStyleChoice = new ChoiceBox<>();
        stageStyleChoice.getItems().setAll(StageStyleChoice.values());
        stageStyleChoice.setValue(stageStyle);

        var backdropLabel = newLabel("backdrop");
        ChoiceBox<StageBackdropChoice> backdropChoice = new ChoiceBox<>();
        backdropChoice.getItems().setAll(StageBackdropChoice.values());
        backdropChoice.setValue(backdrop);

        Button createButton = new Button("Create!");
        createButton.setOnAction(e -> {
            createAndShowStage(stageStyleChoice.getValue(), backdropChoice.getValue());
        });

        HBox stageCreationControls = new HBox(styleLabel, stageStyleChoice, backdropLabel, backdropChoice, createButton);
        stageCreationControls.setAlignment(Pos.BASELINE_LEFT);
        stageCreationControls.setSpacing(10);

        ChoiceBox<FillChoice> fillChoice = new ChoiceBox<>();
        fillChoice.getItems().setAll(FillChoice.values());

        ChoiceBox<ColorSchemeChoice> schemeChoice = new ChoiceBox<>();
        schemeChoice.getItems().setAll(ColorSchemeChoice.values());

        ChoiceBox<OpacityChoice> opacityChoice = new ChoiceBox<>();
        opacityChoice.getItems().setAll(OpacityChoice.values());

        // Pull it together
        VBox controls = new VBox(
            labeledSection("This stage is " + stageStyle + " and the backdrop is " + backdrop),
            labeledSection("New stage", stageCreationControls),
            labeledSection("Fill color for this stage", fillChoice),
            labeledSection("Color scheme for this stage", schemeChoice),
            labeledSection("Opacity of this stage", opacityChoice)
        );

        controls.setAlignment(Pos.BASELINE_LEFT);
        controls.setSpacing(10);
        controls.setPadding(new Insets(10, 10, 10, 10));

        var borderPane = new BorderPane();
        borderPane.setTop(actionButtons);
        borderPane.setCenter(controls);
        borderPane.setBackground(null);
        borderPane.setPadding(new Insets(10, 10, 10, 10));
        borderPane.setOnMousePressed(pressEvent -> {
            borderPane.setOnMouseDragged(dragEvent -> {
                stage.setX(dragEvent.getScreenX() - pressEvent.getSceneX());
                stage.setY(dragEvent.getScreenY() - pressEvent.getSceneY());
            });
        });

        Parent root = borderPane;
        if (stage.getStyle() == StageStyle.EXTENDED) {
            var headerBar = new HeaderBar();
            headerBar.setCenter(new Label(stage.getTitle()));
            var box = new VBox(headerBar, borderPane);
            box.setBackground(null);
            root = box;
        }

        Scene scene = new Scene(root, 640, 480, Color.TRANSPARENT);

        fillChoice.setOnAction(e -> {
            scene.setFill(fillChoice.getValue().getFill());
        });
        schemeChoice.setOnAction(e -> {
            scene.getPreferences().setColorScheme(schemeChoice.getValue().getColorScheme());
        });
        opacityChoice.setOnAction(e -> {
            stage.setOpacity(opacityChoice.getValue().getOpacity());
        });

        fillChoice.setValue(FillChoice.TRANSPARENT);
        if (Platform.getPreferences().getColorScheme() == ColorScheme.LIGHT) {
            schemeChoice.setValue(ColorSchemeChoice.LIGHT);
        } else {
            schemeChoice.setValue(ColorSchemeChoice.DARK);
        }
        opacityChoice.setValue(OpacityChoice.P100);

        stage.setScene(scene);
    }

    private void showStage(Stage stage, StageStyleChoice style, StageBackdropChoice backdrop)
    {
        stage.setTitle(style.toString());
        stage.initStyle(style.getStageStyle());
        stage.initBackdrop(backdrop.getBackdrop());
        buildScene(stage, style, backdrop);
        stage.show();
    }

    private void createAndShowStage(StageStyleChoice style, StageBackdropChoice backdrop)
    {
        Stage stage = new Stage();
        showStage(stage, style, backdrop);
    }

    @Override
    public void start(Stage stage) {
        showStage(stage, StageStyleChoice.EXTENDED, StageBackdropChoice.WINDOW);
    }
}
