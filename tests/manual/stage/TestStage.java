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
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TestStage extends Application {
    private List<Stage> stages = new ArrayList<>();
    private Stage currentStage = null;
    private int stageCounter = 0;

    private final Label lblWidth = new Label();
    private final Label lblHeight = new Label();
    private final Label lblMinWidth = new Label();
    private final Label lblMinHeight = new Label();
    private final Label lblMaxWidth = new Label();
    private final Label lblMaxHeight = new Label();
    private final Label lblX = new Label();
    private final Label lblY = new Label();
    private final Label lblSceneWidth = new Label();
    private final Label lblSceneHeight = new Label();
    private final Label lblSceneX = new Label();
    private final Label lblSceneY = new Label();
    private final Label lblCurrentStage = new Label();
    private final ComboBox<StageStyle> cbStageStyle = new ComboBox<>(FXCollections.observableArrayList(StageStyle.values()));
    private final CheckBox cbIsFullScreen = new CheckBox("Is FullScreen");
    private final CheckBox cbIsMaximized = new CheckBox("Is Maximized");
    private final CheckBox cbIsIconified = new CheckBox("Is Iconified");
    private final CheckBox cbIsResizable = new CheckBox("Is Resizable");

    private final Button btnMaxminize = new Button("Maximize");
    private final Button btnFullScreen = new Button("FullScreen");
    private final Button btnIconify = new Button("Iconify");
    private final Button btnResizable = new Button("Resizable");
    private final Button btnToFront = new Button("To Front");
    private final Button btnToBack = new Button("To Back");
    private final Button btnCreate = new Button("Create");
    private final Button btnSelectNone = new Button("Select None");
    private final Button btnHide = new Button("Hide");
    private final Button btnShow = new Button("Show");
    private final Button btnSizeToScene = new Button("Size to scene");
    private final Button btnCenterOnScreen = new Button("Center on screen");
    private final Button btnResize = new Button("Resize");
    private final Button btnMaxSize = new Button("Set Max Size");
    private final Button btnUnsetMaxSize = new Button("Unset Max Size");
    private final Button btnMove = new Button("Move");
    private final Button btnFocus = new Button("Focus");
    private final Button btnStack = new Button("Stack");

    private final ObjectProperty<StageStyle> initStyle = new SimpleObjectProperty<>(StageStyle.DECORATED);


    private void updateCommandButtonsState() {
        boolean noStagesCreated = stages.isEmpty();

        btnShow.setDisable(noStagesCreated);
        btnHide.setDisable(noStagesCreated);
        btnSizeToScene.setDisable(noStagesCreated);
        btnCenterOnScreen.setDisable(noStagesCreated);
        btnResize.setDisable(noStagesCreated);
        btnMaxSize.setDisable(noStagesCreated);
        btnUnsetMaxSize.setDisable(noStagesCreated);
        btnMove.setDisable(noStagesCreated);
        btnIconify.setDisable(noStagesCreated);
        btnMaxminize.setDisable(noStagesCreated);
        btnFullScreen.setDisable(noStagesCreated);
        btnResizable.setDisable(noStagesCreated);
        btnToFront.setDisable(noStagesCreated);
        btnToBack.setDisable(noStagesCreated);
        btnFocus.setDisable(noStagesCreated);
        btnSelectNone.setDisable(noStagesCreated);
        btnStack.setDisable(noStagesCreated);
    }

    private void updateBindings() {
        if (currentStage == null) {
            cbIsMaximized.selectedProperty().unbind();
            cbIsFullScreen.selectedProperty().unbind();
            cbIsIconified.selectedProperty().unbind();
            cbIsResizable.selectedProperty().unbind();
            lblWidth.textProperty().unbind();
            lblHeight.textProperty().unbind();
            lblMinWidth.textProperty().unbind();
            lblMinHeight.textProperty().unbind();
            lblMaxWidth.textProperty().unbind();
            lblMaxHeight.textProperty().unbind();
            lblX.textProperty().unbind();
            lblY.textProperty().unbind();
            lblSceneWidth.textProperty().unbind();
            lblSceneHeight.textProperty().unbind();
            lblSceneX.textProperty().unbind();
            lblSceneY.textProperty().unbind();
            lblCurrentStage.textProperty().unbind();


            cbIsMaximized.setSelected(false);
            cbIsFullScreen.setSelected(false);
            cbIsIconified.setSelected(false);
            cbIsResizable.setSelected(false);
            lblWidth.setText("Width: 0.00");
            lblHeight.setText("Height: 0.00");
            lblMinWidth.setText("Min Width: 0.00");
            lblMinHeight.setText("Min Height: 0.00");
            lblMaxWidth.setText("Max Width: 0.00");
            lblMaxHeight.setText("Max Height: 0.00");
            lblX.setText("X: 0.00");
            lblY.setText("Y: 0.00");
            lblSceneWidth.setText("Width: 0.00");
            lblSceneHeight.setText("Height: 0.00");
            lblSceneX.setText("X: 0.00");
            lblSceneY.setText("Y: 0.00");
            lblCurrentStage.setText("Current Stage: None");
        } else {
            Scene scene = currentStage.getScene();

            cbIsMaximized.selectedProperty().bind(currentStage.maximizedProperty());
            cbIsFullScreen.selectedProperty().bind(currentStage.fullScreenProperty());
            cbIsIconified.selectedProperty().bind(currentStage.iconifiedProperty());
            cbIsResizable.selectedProperty().bind(currentStage.resizableProperty());


            lblWidth.textProperty().bind(Bindings.format("Width: %.2f", currentStage.widthProperty()));
            lblHeight.textProperty().bind(Bindings.format("Height: %.2f", currentStage.heightProperty()));
            lblMinWidth.textProperty().bind(Bindings.format("Min Width: %.2f", currentStage.minWidthProperty()));
            lblMinHeight.textProperty().bind(Bindings.format("Min Height: %.2f", currentStage.minHeightProperty()));
            lblMaxWidth.textProperty().bind(Bindings.format("Max Width: %.2f", currentStage.maxWidthProperty()));
            lblMaxHeight.textProperty().bind(Bindings.format("Max Height: %.2f", currentStage.maxHeightProperty()));
            lblX.textProperty().bind(Bindings.format("X: %.2f", currentStage.xProperty()));
            lblY.textProperty().bind(Bindings.format("Y: %.2f", currentStage.yProperty()));
            lblCurrentStage.textProperty().bind(Bindings.format("Current Stage: %s", currentStage.titleProperty()));

            if (scene != null) {
                lblSceneWidth.textProperty().bind(Bindings.format("Width: %.2f", scene.widthProperty()));
                lblSceneHeight.textProperty().bind(Bindings.format("Height: %.2f", scene.heightProperty()));
                lblSceneX.textProperty().bind(Bindings.format("X: %.2f", scene.xProperty()));
                lblSceneY.textProperty().bind(Bindings.format("Y: %.2f", scene.yProperty()));
            }
        }
    }

    private final CheckBox cbAlwaysOnTop = new CheckBox("Command Always On Top");

    @Override
    public void start(Stage stage) {
        cbStageStyle.getSelectionModel().select(StageStyle.DECORATED);
        initStyle.bind(cbStageStyle.valueProperty());
        stage.setAlwaysOnTop(true);

        cbAlwaysOnTop.setSelected(stage.isAlwaysOnTop());
        cbAlwaysOnTop.setOnAction(e -> stage.setAlwaysOnTop(cbAlwaysOnTop.isSelected()));

        btnMaxminize.setOnAction(e -> {
            if (currentStage != null) {
                currentStage.setMaximized(!currentStage.isMaximized());
            }
        });

        btnFullScreen.setOnAction(e -> {
            if (currentStage != null) {
                currentStage.setFullScreen(!currentStage.isFullScreen());
            }
        });

        btnIconify.setOnAction(e -> {
            if (currentStage != null) {
                currentStage.setIconified(!currentStage.isIconified());
            }
        });

        btnResizable.setOnAction(e -> {
            if (currentStage != null) {
                currentStage.setResizable(!currentStage.isResizable());
            }
        });

        btnToFront.setOnAction(e -> {
            if (currentStage != null) {
                currentStage.toFront();
            }
        });

        btnToBack.setOnAction(e -> {
            if (currentStage != null) {
                currentStage.toBack();
            }
        });

        btnCreate.setOnAction(e -> createTestStage());

        btnSelectNone.setOnAction(e -> {
            currentStage = null;
            updateBindings();
        });

        btnHide.setOnAction(e -> {
            if (currentStage != null) {
                currentStage.hide();
            }
        });

        btnShow.setOnAction(e -> {
            if (currentStage != null) {
                currentStage.show();
            }
        });

        btnSizeToScene.setOnAction(e -> {
            if (currentStage != null) {
                currentStage.sizeToScene();
            }
        });

        btnCenterOnScreen.setOnAction(e -> {
            if (currentStage != null) {
                currentStage.centerOnScreen();
            }
        });

        btnResize.setOnAction(e -> {
            if (currentStage != null) {
                double[] dimensions = showValuesDialog(
                        "Resize Stage",
                        "Width:",
                        "Height:",
                        currentStage.getWidth(),
                        currentStage.getHeight(),
                        50,
                        2000);

                if (dimensions != null) {
                    currentStage.setWidth(dimensions[0]);
                    currentStage.setHeight(dimensions[1]);
                }
            }
        });

        btnMaxSize.setOnAction(e -> {
            if (currentStage != null) {
                double[] dimensions = showValuesDialog(
                        "Set Max Size",
                        "Max Width:",
                        "Max Height:",
                        currentStage.getMaxWidth(),
                        currentStage.getMaxHeight(),
                        50,
                        2000);

                if (dimensions != null) {
                    currentStage.setMaxWidth(dimensions[0]);
                    currentStage.setMaxHeight(dimensions[1]);
                }
            }
        });

        btnUnsetMaxSize.setOnAction(e -> {
            if (currentStage != null) {
                currentStage.setMaxWidth(Double.MAX_VALUE);
                currentStage.setMaxHeight(Double.MAX_VALUE);
            }
        });

        btnMove.setOnAction(e -> {
            if (currentStage != null) {
                double[] position = showValuesDialog(
                        "Move Stage",
                        "X Position:",
                        "Y Position:",
                        currentStage.getX(),
                        currentStage.getY(),
                        0,
                        3000);

                if (position != null) {
                    currentStage.setX(position[0]);
                    currentStage.setY(position[1]);
                }
            }
        });

        btnFocus.setOnAction(e -> {
            if (currentStage != null) {
                currentStage.requestFocus();
            }
        });

        btnStack.setOnAction(e -> {
            if (!stages.isEmpty()) {
                double xOffset = 0;
                double yOffset = 0;

                for (Stage stageWindow : stages) {
                    stageWindow.setX(xOffset);
                    stageWindow.setY(yOffset);
                    stageWindow.show();
                    xOffset += 50;
                    yOffset += 50;
                }
            }
        });

        updateCommandButtonsState();

        cbIsMaximized.setDisable(true);
        cbIsFullScreen.setDisable(true);
        cbIsIconified.setDisable(true);
        cbIsResizable.setDisable(true);

        FlowPane commandPane = new FlowPane(cbStageStyle, btnCreate, btnShow, btnHide, btnSizeToScene,
                btnCenterOnScreen, btnResize, btnMaxSize, btnUnsetMaxSize, btnMove, btnIconify, btnMaxminize,
                btnFullScreen, btnResizable, btnToFront, btnToBack, btnStack, btnFocus, btnSelectNone, cbAlwaysOnTop);
        commandPane.setHgap(5);
        commandPane.setVgap(5);


        VBox stagePropertiesBox = new VBox(
                lblCurrentStage,
                cbIsIconified, cbIsMaximized,
                cbIsFullScreen, cbIsResizable,
                lblMinWidth, lblMinHeight, lblMaxWidth, lblMaxHeight,
                lblWidth, lblHeight, lblX, lblY
        );
        stagePropertiesBox.setSpacing(5);

        VBox scenePropertiesBox = new VBox(
                lblSceneWidth, lblSceneHeight, lblSceneX, lblSceneY
        );
        scenePropertiesBox.setSpacing(5);

        TitledPane stagePropertiesPane = new TitledPane("Stage Properties:", stagePropertiesBox);
        stagePropertiesPane.setCollapsible(false);

        TitledPane scenePropertiesPane = new TitledPane("Scene Properties:", scenePropertiesBox);
        scenePropertiesPane.setCollapsible(false);

        VBox root = new VBox(
                commandPane,
                stagePropertiesPane,
                scenePropertiesPane
        );
        root.setSpacing(5);
        root.setFillWidth(true);

        Scene scene = new Scene(root, 500, 600);
        stage.setTitle("Command Stage");
        stage.setScene(scene);
        stage.show();
    }

    private void createTestStage() {
        Stage newStage = new Stage();

        stageCounter++;

        Scene testScene;
        StackPane root;
        if (initStyle.getValue() == StageStyle.TRANSPARENT) {
            root = new StackPane();
            BackgroundFill fill = new BackgroundFill(
                    Color.HOTPINK.deriveColor(0, 1, 1, 0.5),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            );
            root.setBackground(new Background(fill));

            testScene = new Scene(root, 300, 300);
            testScene.setFill(Color.TRANSPARENT);
        } else {
            root = new StackPane();
            root.setBackground(Background.EMPTY);
            testScene = new Scene(root, 300, 300, Color.HOTPINK);
        }

        setupContextMenu(root);

        newStage.setScene(testScene);
        newStage.initStyle(initStyle.getValue());
        newStage.setTitle("Test Stage " + stageCounter);

        newStage.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                currentStage = newStage;
                updateBindings();
            }
        });

        stages.add(newStage);
        currentStage = newStage;

        newStage.setOnHidden(e -> {
            stages.remove(newStage);
            if (currentStage == newStage) {
                currentStage = stages.isEmpty() ? null : stages.get(stages.size() - 1);
                updateBindings();
            }
            updateCommandButtonsState();
        });

        updateBindings();
        updateCommandButtonsState();
    }

    private double[] showValuesDialog(String title, String firstLabel, String secondLabel,
                                      double defaultFirst, double defaultSecond,
                                      double minValue, double maxValue) {
        Dialog<double[]> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Enter values:");

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Spinner<Double> firstSpinner = new Spinner<>();
        SpinnerValueFactory.DoubleSpinnerValueFactory firstValueFactory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(minValue, maxValue, defaultFirst, 1.0);
        firstSpinner.setValueFactory(firstValueFactory);
        firstSpinner.setEditable(true);

        Spinner<Double> secondSpinner = new Spinner<>();
        SpinnerValueFactory.DoubleSpinnerValueFactory secondValueFactory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(minValue, maxValue, defaultSecond, 1.0);
        secondSpinner.setValueFactory(secondValueFactory);
        secondSpinner.setEditable(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label(firstLabel), 0, 0);
        grid.add(firstSpinner, 1, 0);
        grid.add(new Label(secondLabel), 0, 1);
        grid.add(secondSpinner, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new double[] {firstSpinner.getValue(), secondSpinner.getValue()};
            }
            return null;
        });

        Optional<double[]> result = dialog.showAndWait();
        return result.orElse(null);
    }

    public static void main(String[] args) {
        launch(TestStage.class, args);
    }

    private void createSceneWithTextField() {
        if (currentStage != null) {
            StackPane root = new StackPane();

            TextField textField = new TextField();
            textField.setPromptText("Enter text here");

            root.getChildren().add(textField);
            setupContextMenu(root);
            Scene scene = new Scene(root, 300, 200);

            currentStage.setScene(scene);
            updateSceneBindings(scene);
        }
    }

    private void createSceneWithTooltipBox() {
        if (currentStage != null) {
            StackPane root = new StackPane();

            StackPane coloredBox = new StackPane();
            coloredBox.setBackground(Background.fill(Color.CORNFLOWERBLUE));

            Tooltip tooltip = new Tooltip("The quick brown fox jumps over the lazy dog.");
            Tooltip.install(coloredBox, tooltip);
            root.getChildren().add(coloredBox);
            setupContextMenu(root);
            Scene scene = new Scene(root, 300, 200);
            currentStage.setScene(scene);
            updateSceneBindings(scene);
        }
    }

    private void updateSceneBindings(Scene scene) {
        lblSceneWidth.textProperty().bind(Bindings.format("Width: %.2f", scene.widthProperty()));
        lblSceneHeight.textProperty().bind(Bindings.format("Height: %.2f", scene.heightProperty()));
        lblSceneX.textProperty().bind(Bindings.format("X: %.2f", scene.xProperty()));
        lblSceneY.textProperty().bind(Bindings.format("Y: %.2f", scene.yProperty()));
    }

    private void setupContextMenu(StackPane root) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem textFieldMenuItem = new MenuItem("Create Scene with TextField");
        textFieldMenuItem.setOnAction(e -> createSceneWithTextField());
        MenuItem tooltipBoxMenuItem = new MenuItem("Create Scene with Tooltip Box");
        tooltipBoxMenuItem.setOnAction(e -> createSceneWithTooltipBox());
        contextMenu.getItems().addAll(textFieldMenuItem, tooltipBoxMenuItem);
        root.setOnContextMenuRequested(e -> contextMenu.show(root, e.getScreenX(), e.getScreenY()));
    }
}
