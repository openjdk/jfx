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
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderDragType;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

public class TestStage extends Application {
    private final ObservableList<Stage> stages = FXCollections.observableArrayList();
    private int stageCounter = 0;

    private final ComboBox<StageStyle> cbStageStyle =
            new ComboBox<>(FXCollections.observableArrayList(StageStyle.values()));
    private final ComboBox<Modality> cbModality = new ComboBox<>(FXCollections.observableArrayList(Modality.values()));
    private final ComboBox<Stage> cbOwner = new ComboBox<>();

    private final Button btnToFront = new Button("To Front");
    private final Button btnToBack = new Button("To Back");
    private final Button btnCreate = new Button("Create");
    private final Button btnCreateShow = new Button("Create/Show");
    private final Button btnSelectLast = new Button("Select Last");
    private final Button btnSelectPrevious = new Button("◀");
    private final Button btnSelectNone = new Button("None");
    private final Button btnSelectNext = new Button("▶");
    private final Button btnHide = new Button("Hide/Close");
    private final Button btnShow = new Button("Show");
    private final Button btnSizeToScene = new Button("Size to Scene");
    private final Button btnCenterOnScreen = new Button("Center on Screen");
    private final Button btnFocus = new Button("Focus");
    private final PropertyEditor propertyEditor = new PropertyEditor();

    private final ObjectProperty<StageStyle> initStyle = new SimpleObjectProperty<>(StageStyle.DECORATED);
    private final ObjectProperty<Modality> initModality = new SimpleObjectProperty<>(Modality.NONE);
    private final ObjectProperty<Stage> initOwner = new SimpleObjectProperty<>(null);
    private Stage currentStage = null;

    private static final double MAX_WIDTH = 7680;
    private static final double MAX_HEIGHT = 4320;

    private void updateCommandButtonsState() {
        boolean disabled = stages.isEmpty() || currentStage == null;

        btnShow.setDisable(disabled);
        btnHide.setDisable(disabled);
        btnSizeToScene.setDisable(disabled);
        btnCenterOnScreen.setDisable(disabled);
        btnToFront.setDisable(disabled);
        btnToBack.setDisable(disabled);
        btnFocus.setDisable(disabled);
        btnSelectNone.setDisable(disabled);

        btnSelectLast.setDisable(stages.isEmpty() || currentStage == stages.getLast());
        btnSelectNext.setDisable(stages.isEmpty() || currentStage == null || currentStage == stages.getLast());
        btnSelectPrevious.setDisable(stages.isEmpty() || currentStage == null  || currentStage == stages.getFirst());
    }

    private void updateBindings() {
        if (currentStage == null) {
            propertyEditor.unbind();
        } else {
            propertyEditor.bindToStage(currentStage);
        }
    }

    private final CheckBox cbCommandAlwaysOnTop = new CheckBox("Command Always On Top");

    @Override
    public void start(Stage stage) {
        cbStageStyle.getSelectionModel().select(StageStyle.DECORATED);
        cbModality.getSelectionModel().select(Modality.NONE);
        cbOwner.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            ObservableList<Stage> listWithNull = FXCollections.observableArrayList();
            listWithNull.add(null);
            listWithNull.addAll(stages);
            return listWithNull;
        }, stages));

        cbOwner.setConverter(new StringConverter<>() {
            @Override
            public String toString(Stage stage) {
                if (stage == null) {
                    return "None";
                }

                return stage.getTitle();
            }

            @Override
            public Stage fromString(String string) {
                return null;
            }
        });

        initStyle.bind(cbStageStyle.valueProperty());
        initModality.bind(cbModality.valueProperty());
        initOwner.bind(cbOwner.valueProperty());

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

        btnCreateShow.setOnAction(e -> {
            createTestStage();
            currentStage.show();
        });

        btnSelectNone.setOnAction(e -> {
            currentStage = null;
            updateBindings();
            updateCommandButtonsState();
        });

        btnSelectLast.setOnAction(e -> {
            currentStage = stages.get(stages.size() - 1);
            updateBindings();
            updateCommandButtonsState();
        });

        btnSelectNext.setOnAction(e -> {
            if (!stages.isEmpty()) {
                int index = stages.indexOf(currentStage);
                if (index < stages.size() - 1) {
                    currentStage = stages.get(index + 1);
                    updateBindings();
                    updateCommandButtonsState();
                }
            }
        });

        btnSelectPrevious.setOnAction(e -> {
            if (!stages.isEmpty()) {
                int index = stages.indexOf(currentStage);
                if (index > 0) {
                    currentStage = stages.get(index - 1);
                    updateBindings();
                    updateCommandButtonsState();
                }
            }
        });

        btnHide.setOnAction(e -> {
            if (currentStage != null) {
                boolean isShowing = currentStage.isShowing();
                currentStage.hide();

                if (!isShowing) {
                    stages.remove(currentStage);
                    currentStage = stages.isEmpty() ? null : stages.getLast();
                    updateCommandButtonsState();
                    updateBindings();
                }
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

        btnFocus.setOnAction(e -> {
            if (currentStage != null) {
                currentStage.requestFocus();
            }
        });

        updateCommandButtonsState();

        FlowPane flow0 = new FlowPane(label("Style: ", cbStageStyle), label("Modality: ", cbModality),
                label("Owner: ", cbOwner));
        FlowPane flow1 = new FlowPane(btnCreate, btnShow, btnCreateShow, btnHide);
        FlowPane flow2 = new FlowPane(btnCenterOnScreen, btnSizeToScene);
        FlowPane flow3 = new FlowPane(btnToFront, btnToBack, btnSelectNone, btnSelectLast, btnSelectPrevious,
                btnSelectNext, btnFocus);

        List.of(flow0, flow1, flow2, flow3).forEach(flow -> {
            flow.setHgap(5);
            flow.setVgap(5);
        });

        VBox commandPane = new VBox(cbCommandAlwaysOnTop, flow0, flow1, flow2, flow3);
        commandPane.setSpacing(5);
        commandPane.setFillWidth(true);

        TitledPane commandPaneTitledPane = new TitledPane("Commands", commandPane);
        commandPaneTitledPane.setCollapsible(false);

        TitledPane editorTitledPane = new TitledPane("Properties", propertyEditor);
        editorTitledPane.setCollapsible(false);

        VBox root = new VBox(
                commandPaneTitledPane,
                editorTitledPane
        );
        root.setSpacing(5);
        root.setFillWidth(true);


        Scene scene = new Scene(root);
        stage.setTitle("Command Stage");
        stage.setScene(scene);
        stage.setOnShown(e -> {
            Rectangle2D stageBounds = new Rectangle2D(
                    stage.getX(),
                    stage.getY(),
                    stage.getWidth(),
                    stage.getHeight()
            );

            Screen currentScreen = Screen.getScreens()
                    .stream()
                    .filter(screen -> screen.getVisualBounds().intersects(stageBounds))
                    .findFirst()
                    .orElse(Screen.getPrimary());

            Rectangle2D visualBounds = currentScreen.getVisualBounds();
            stage.setHeight(visualBounds.getHeight());

            double x = visualBounds.getMaxX() - stage.getWidth();
            double y = visualBounds.getMaxY() - stage.getHeight();

            stage.setX(x);
            stage.setY(y);
        });
        stage.setWidth(500);
        stage.show();
    }

    private HBox label(String label, Control control) {
        HBox hbox = new HBox(new Label(label), control);
        hbox.setSpacing(5);
        hbox.setAlignment(Pos.CENTER_LEFT);
        return hbox;
    }

    private void createTestStage() {
        Stage newStage = new Stage();

        stageCounter++;

        newStage.initStyle(initStyle.getValue());
        newStage.initModality(initModality.getValue());
        newStage.initOwner(initOwner.getValue());
        newStage.setTitle("Test Stage " + stageCounter);

        newStage.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                currentStage = newStage;
                updateBindings();
                updateCommandButtonsState();
            }
        });

        stages.add(newStage);
        currentStage = newStage;
        createDefaultScene();

        newStage.setOnHidden(e -> {
            stages.remove(newStage);
            if (currentStage == newStage) {
                currentStage = stages.isEmpty() ? null : stages.getLast();
                updateBindings();
            }
            updateCommandButtonsState();
        });

        updateBindings();
        updateCommandButtonsState();
    }

    public static void main(String[] args) {
        launch(TestStage.class, args);
    }

    private Label createLabel(String prefix, ReadOnlyProperty<?> property) {
        Label label = new Label();
        label.textProperty().bind(Bindings.concat(prefix, Bindings.convert(property)));
        return label;
    }

    private void createDefaultScene() {
        Scene scene;

        StringProperty lastEvent = new SimpleStringProperty();

        Label ownerLabel = new Label("Owner: NONE");
        if (currentStage.getOwner() instanceof Stage owner) {
            ownerLabel.setText("Owner: " + owner.getTitle());
        }

        VBox root = new VBox();
        if (currentStage.getStyle() == StageStyle.EXTENDED) {
            HeaderBar headerbar = new HeaderBar();
            Label headerLabel = new Label();
            var headerPane = new StackPane(headerLabel);
            headerLabel.textProperty().bind(currentStage.titleProperty());
            headerbar.setCenter(headerPane);
            headerbar.setDragType(headerPane, HeaderDragType.DRAGGABLE_SUBTREE);
            root.getChildren().add(headerbar);
        }

        root.getChildren().addAll(createLabel("Focused: ", currentStage.focusedProperty()),
                            new Label("Modality: " + currentStage.getModality()),
                            ownerLabel,
                            createLabel("Last Event: ", lastEvent));
        root.setBackground(Background.EMPTY);



        if (currentStage.getStyle() == StageStyle.TRANSPARENT) {
            BackgroundFill fill = new BackgroundFill(
                    Color.HOTPINK.deriveColor(0, 1, 1, 0.5),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            );
            root.setBackground(new Background(fill));

            scene = new Scene(root, 300, 300);
            scene.setFill(Color.TRANSPARENT);
        } else {
            scene = new Scene(root, 300, 300, Color.HOTPINK);
        }

        currentStage.addEventHandler(Event.ANY, e -> lastEvent.set(e.getEventType().getName()));
        setupContextMenu(root);
        currentStage.setScene(scene);
    }

    private void createSceneWithTextField() {
        StackPane root = new StackPane();

        TextField textField = new TextField();
        textField.setPromptText("Enter text here");

        root.getChildren().add(textField);
        setupContextMenu(root);
        Scene scene = new Scene(root, 300, 200);

        currentStage.setScene(scene);
    }

    private void createSceneWithTooltipBox() {
        StackPane root = new StackPane();

        StackPane coloredBox = new StackPane();
        coloredBox.setBackground(Background.fill(Color.CORNFLOWERBLUE));

        Tooltip tooltip = new Tooltip("The quick brown fox jumps over the lazy dog.");
        Tooltip.install(coloredBox, tooltip);
        root.getChildren().add(coloredBox);
        setupContextMenu(root);
        Scene scene = new Scene(root, 300, 200);
        currentStage.setScene(scene);
    }

    private void createAlert(boolean windowModal) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Alert");
        alert.setHeaderText("The quick brown fox jumps over the lazy dog.");

        if (windowModal) {
            alert.initModality(Modality.WINDOW_MODAL);
            alert.initOwner(currentStage);
        }

        alert.showAndWait();
    }

    private void createFileOpen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = fileChooser.showOpenDialog(currentStage);

        if (file != null) {
            new Alert(Alert.AlertType.INFORMATION, "File selected: " + file.getAbsolutePath()).showAndWait();
        } else {
            new Alert(Alert.AlertType.WARNING, "No file selected").showAndWait();
        }
    }

    private void setupContextMenu(Node root) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem defaultSceneMenuItem = new MenuItem("Default Scene");
        defaultSceneMenuItem.setOnAction(e -> createDefaultScene());
        MenuItem textFieldMenuItem = new MenuItem("Scene with TextField");
        textFieldMenuItem.setOnAction(e -> createSceneWithTextField());
        MenuItem tooltipBoxMenuItem = new MenuItem("Scene with Tooltip Box");
        tooltipBoxMenuItem.setOnAction(e -> createSceneWithTooltipBox());
        MenuItem alertMenuItem = new MenuItem("Alert - Application Modal");
        alertMenuItem.setOnAction(e -> createAlert(false));
        MenuItem alertWindowModalMenuItem = new MenuItem("Alert - Window Modal");
        alertWindowModalMenuItem.setOnAction(e -> createAlert(true));
        MenuItem fileOpenMenuItem = new MenuItem("File Open");
        fileOpenMenuItem.setOnAction(e -> createFileOpen());


        contextMenu.getItems().addAll(defaultSceneMenuItem, textFieldMenuItem, tooltipBoxMenuItem,
                alertMenuItem, alertWindowModalMenuItem, fileOpenMenuItem);
        root.setOnContextMenuRequested(e -> contextMenu.show(root, e.getScreenX(), e.getScreenY()));

        root.setOnMousePressed(e -> {
            if (contextMenu.isShowing()) {
                contextMenu.hide();
            }
        });
    }

    class PropertyEditor extends VBox {
        private final PropertyEditorPane stagePane = new PropertyEditorPane("Stage");
        private final PropertyEditorPane scenePane = new PropertyEditorPane("Scene");

        private final ObjectProperty<Scene> sceneProperty = new SimpleObjectProperty<>();

        PropertyEditor() {
            getChildren().addAll(stagePane, scenePane);
            stagePane.setMaxHeight(550);
            setFillWidth(true);
        }

        public void bindToStage(Stage stage) {
            unbind();

            stagePane.addStringProperty("Title", stage.titleProperty(), stage::setTitle);
            stagePane.addBooleanProperty("Always OnTop", stage.alwaysOnTopProperty(), stage::setAlwaysOnTop);
            stagePane.addBooleanProperty("FullScreen", stage.fullScreenProperty(), stage::setFullScreen);
            stagePane.addBooleanProperty("Maximized", stage.maximizedProperty(), stage::setMaximized);
            stagePane.addBooleanProperty("Iconified", stage.iconifiedProperty(), stage::setIconified);
            stagePane.addBooleanProperty("Resizeable", stage.resizableProperty(), stage::setResizable);
            stagePane.addBooleanProperty("Focused", stage.focusedProperty(), null);
            stagePane.addDoublePropery("X", stage.xProperty(), stage::setX, 0, MAX_WIDTH * 2, 1.0);
            stagePane.addDoublePropery("Y", stage.yProperty(), stage::setY, 0, MAX_HEIGHT * 2, 1.0);
            stagePane.addDoublePropery("Width", stage.widthProperty(), stage::setWidth, 1, MAX_WIDTH, 1.0);
            stagePane.addDoublePropery("Height", stage.heightProperty(), stage::setHeight, 1, MAX_HEIGHT, 1.0);
            stagePane.addDoublePropery("Min Width", stage.minWidthProperty(), stage::setMinWidth, 1, MAX_WIDTH, 1.0);
            stagePane.addDoublePropery("Min Height", stage.minHeightProperty(), stage::setMinHeight, 1, MAX_HEIGHT,
                    1.0);
            stagePane.addDoublePropery("Max Width", stage.maxWidthProperty(), stage::setMaxWidth, 1, Double.MAX_VALUE,
                    1.0);
            stagePane.addDoublePropery("Max Height", stage.maxHeightProperty(), stage::setMaxHeight, 1,
                    Double.MAX_VALUE, 1.0);
            stagePane.addDoublePropery("RenderScale X", stage.renderScaleXProperty(), stage::setRenderScaleX, 0, 2,
                    0.25);
            stagePane.addDoublePropery("RenderScale Y", stage.renderScaleYProperty(), stage::setRenderScaleY, 0, 2,
                    0.25);
            stagePane.addDoublePropery("Opacity", stage.opacityProperty(), stage::setOpacity, 0, 1, 0.1);

            sceneProperty.bind(stage.sceneProperty());
            bindScene(stage.getScene());

            sceneProperty.addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    bindScene(newScene);
                }
            });
        }

        private void bindScene(Scene scene) {
            scenePane.unbind();
            scenePane.addDoubleLabelProperty("X", scene.xProperty());
            scenePane.addDoubleLabelProperty("Y", scene.yProperty());
            scenePane.addDoubleLabelProperty("Width", scene.widthProperty());
            scenePane.addDoubleLabelProperty("Height", scene.heightProperty());
        }

        public void unbind() {
            scenePane.unbind();
            stagePane.unbind();
        }
    }

    class PropertyEditorPane extends TitledPane {
        private int currentRow = 0;
        private final List<Runnable> clearChangeListeners = new ArrayList<>();
        private final GridPane gridPane = new GridPane();

        PropertyEditorPane(String title) {
            setText(title);
            ScrollPane propertiesScrollPane = new ScrollPane(propertyEditor);
            propertiesScrollPane.setFitToWidth(true);
            propertiesScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            propertiesScrollPane.setContent(gridPane);

            gridPane.setHgap(5);
            gridPane.setVgap(5);
            gridPane.setPadding(new Insets(10));

            setContent(propertiesScrollPane);
        }

        private <T> void addListener(ReadOnlyProperty<T> property, ChangeListener<T> changeListener) {
            property.addListener(changeListener);
            clearChangeListeners.add(() -> property.removeListener(changeListener));
        }

        private void addLabel(String label) {
            Label lbl = new Label(label);
            gridPane.add(lbl, 0, currentRow);
            GridPane.setHgrow(lbl, Priority.SOMETIMES);
            GridPane.setHalignment(lbl, HPos.RIGHT);
        }

        public void addDoubleLabelProperty(String label, ReadOnlyDoubleProperty property) {
            addLabel(label);
            Label lbl = new Label();
            lbl.textProperty().bind(property.asString("%.2f"));
            gridPane.add(lbl, 1, currentRow);
            GridPane.setHgrow(lbl, Priority.ALWAYS);
            currentRow++;
        }

        public void addDoublePropery(String label, ReadOnlyDoubleProperty property, DoubleConsumer setConsumer,
                                      double min, double max,
                                      double amountToStepBy) {
            addLabel(label);
            Spinner<Double> spinner = new Spinner<>();
            spinner.setEditable(true);
            SpinnerValueFactory.DoubleSpinnerValueFactory spinnerValueFactory =
                    new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, property.get(), amountToStepBy);
            spinner.setValueFactory(spinnerValueFactory);
            gridPane.add(spinner, 1, currentRow);
            GridPane.setHgrow(spinner, Priority.ALWAYS);

            AtomicBoolean suppressListener = new AtomicBoolean(false);
            addListener(property, (obs, oldValue, newValue) -> {
                if (!newValue.equals(spinner.getValue())) {
                    try {
                        suppressListener.set(true);
                        spinnerValueFactory.setValue((Double) newValue);
                    } finally {
                        suppressListener.set(false);
                    }
                }
            });

            if (setConsumer != null) {
                spinner.valueProperty().addListener((observable, oldValue, newValue) -> {
                    if (!newValue.equals(oldValue) && !suppressListener.get()) {
                        setConsumer.accept(newValue);
                    }
                });
            } else {
                spinner.setDisable(true);
            }

            currentRow++;
        }

        public void addStringProperty(String label, ReadOnlyStringProperty property, Consumer<String> setConsumer) {
            addLabel(label);
            TextField textField = new TextField(property.get());
            gridPane.add(textField, 1, currentRow);
            GridPane.setHgrow(textField, Priority.ALWAYS);

            addListener(property, (obs, oldValue, newValue) -> textField.setText(newValue));

            if (setConsumer != null) {
                textField.setOnAction(e -> setConsumer.accept(textField.getText()));
            } else {
                textField.setDisable(true);
            }
            currentRow++;
        }

        public void addBooleanProperty(String label, ReadOnlyBooleanProperty property, Consumer<Boolean> setConsumer) {
            addLabel(label);
            CheckBox checkBox = new CheckBox();
            checkBox.setSelected(property.get());
            gridPane.add(checkBox, 1, currentRow);

            addListener(property, (obs, oldValue, newValue) -> checkBox.setSelected(newValue));

            if (setConsumer != null) {
                checkBox.setOnAction(e -> setConsumer.accept(checkBox.isSelected()));
            } else {
                checkBox.setDisable(true);
            }
            currentRow++;
        }

        public void unbind() {
            clearChangeListeners.forEach(Runnable::run);
            gridPane.getChildren().clear();
            currentRow = 0;
        }
    }
}
