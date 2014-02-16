/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package hello;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.DoubleProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.geometry.Point2D;
import javafx.geometry.Insets;
import com.sun.glass.ui.Robot;

public class HelloTouch extends Application {

    double buttonPaneX = 0.0;
    double buttonPaneY = 280.0;
    int countA = 0;
    int countB = 0;
    int countAll = 0;
    int modeFlag = 1;
    int optionFlag = 1;
    double pressedX = 0;
    double pressedY = 0;
    double currentAX = 7.0;
    double currentAY = 3.0;
    double currentBX = 10.0;
    double currentBY = 3.0;

    private Label aPressed = new Label("A: 0 ");
    private Label bPressed = new Label("B: 0 ");
    private Label allPressed = new Label("All: 0 ");
    private Button btnA = new Button("A");
    private Button btnB = new Button("B");
    private Rectangle frame = new Rectangle(700, 400);
    private Pane buttonsPane = new Pane();
    private RadioButton rbtnA = new RadioButton("Radio button A");
    private RadioButton rbtnB = new RadioButton("Radio button B");
    private CheckBox chbtnA = new CheckBox("CheckBox A");
    private CheckBox chbtnB = new CheckBox("CheckBox B");
    private Slider btnSizeSlider = new Slider(0.4, 5.0, 0.1);
    private CheckBox editMode = new CheckBox("Edit Mode");
    private RadioButton option3 = new RadioButton("Zero Gap");
    private EventHandler<MouseEvent> redPressHandler;
    private EventHandler<MouseEvent> greenPressHandler;
    private TextField dpiText = new TextField("90.0");
    private RadioButton option1 = new RadioButton("Big Gap");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setX(0);
        primaryStage.setY(0);
        final Rectangle rec = new Rectangle(200, 30);
        rec.setStroke(Color.BLACK);
        rec.setFill(Color.WHITE);
        rec.setLayoutX(160);
        rec.setLayoutY(10);

        btnA.setId("custom-red-button");
        redPressHandler = new EventHandler<MouseEvent>() {
            public void handle(MouseEvent event) {
                increaseCount(aPressed, "A: ");
                increaseCount(allPressed, "All: ");
                rec.setFill(Color.RED);
            }
        };
        btnA.setOnMousePressed(redPressHandler);

        btnB.setId("custom-green-button");
        greenPressHandler = new EventHandler<MouseEvent>() {
            public void handle(MouseEvent event) {
                increaseCount(bPressed, "B: ");
                increaseCount(allPressed, "All: ");
                rec.setFill(Color.GREEN);
            }
        };
        btnB.setOnMousePressed(greenPressHandler);

        EventHandler<MouseEvent> releaseHandler = new EventHandler<MouseEvent>() {
            public void handle(MouseEvent event) {
                rec.setFill(Color.WHITE);
            }
        };
        btnA.setOnMouseReleased(releaseHandler);
        btnB.setOnMouseReleased(releaseHandler);

        buttonsPane.setPrefSize(700, 400);
        buttonsPane.setLayoutX(buttonPaneX);
        buttonsPane.setLayoutY(buttonPaneY);
        frame.setLayoutX(20);
        frame.setStroke(Color.BLACK);
        frame.setFill(Color.WHITE);

        buttonsPane.getChildren().addAll(frame, btnA, btnB);
        buttonsPane.setOnMousePressed(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent event) {
                increaseCount(allPressed, "All: ");
            }
        });

        // Radio Buttons configuration
        ToggleGroup radioBtnsGroup = new ToggleGroup();
        rbtnA.setToggleGroup(radioBtnsGroup);
        rbtnB.setToggleGroup(radioBtnsGroup);
        rbtnA.setOnMousePressed(redPressHandler);
        rbtnA.setOnMousePressed(redPressHandler);
        rbtnB.setOnMousePressed(greenPressHandler);
        rbtnA.setOnMouseReleased(releaseHandler);
        rbtnB.setOnMouseReleased(releaseHandler);
        rbtnA.setSelected(true);
        rbtnA.setId("red-radio-button");
        rbtnB.setId("green-radio-button");

        // Checkboxes configuration
        chbtnA.setOnMousePressed(redPressHandler);
        chbtnB.setOnMousePressed(greenPressHandler);
        chbtnA.setOnMouseReleased(releaseHandler);
        chbtnB.setOnMouseReleased(releaseHandler);
        chbtnA.setId("red-check-button");
        chbtnB.setId("green-check-button");

        updateButtonPane();

        ToggleGroup optionsGroup = new ToggleGroup();
        ToggleGroup modeGroup = new ToggleGroup();

        option1.setToggleGroup(optionsGroup);
        option1.setSelected(true);
        option1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                optionFlag = 1;
                updateButtonPane();
            }
        });

        RadioButton option2 = new RadioButton("Small Gap");
        option2.setToggleGroup(optionsGroup);
        option2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                optionFlag = 2;
                updateButtonPane();
            }
        });

        option3.setToggleGroup(optionsGroup);
        option3.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                optionFlag = 3;
                updateButtonPane();
            }
        });

        RadioButton option4 = new RadioButton("Small Button");
        option4.setToggleGroup(optionsGroup);
        option4.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                optionFlag = 4;
                updateButtonPane();
            }
        });

        RadioButton option5 = new RadioButton("Custom: ");
        option5.setToggleGroup(optionsGroup);
        option5.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                optionFlag = 5;
                updateButtonPane();
            }
        });

        btnSizeSlider.setDisable(true);
        editMode.setDisable(true);
        HBox custom_box = new HBox(15);
        custom_box.getChildren().addAll(option5, btnSizeSlider, editMode);

        VBox optRadioBox = new VBox(15);
        optRadioBox.setLayoutX(350);
        optRadioBox.setLayoutY(80);
        optRadioBox.getChildren().addAll(option1, option2, option3, option4,
                custom_box);

        RadioButton mode1 = new RadioButton("Buttons");
        mode1.setToggleGroup(modeGroup);
        mode1.setSelected(true);
        mode1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                modeFlag = 1;
                updateButtonPane();
            }
        });

        RadioButton mode2 = new RadioButton("Radio Buttons");
        mode2.setToggleGroup(modeGroup);
        mode2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                modeFlag = 2;
                updateButtonPane();
            }
        });

        RadioButton mode3 = new RadioButton("Checkboxes");
        mode3.setToggleGroup(modeGroup);
        mode3.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                modeFlag = 3;
                updateButtonPane();
            }
        });

        VBox modeRadioBox = new VBox(15);
        modeRadioBox.setLayoutX(20);
        modeRadioBox.setLayoutY(80);
        modeRadioBox.getChildren().addAll(mode1, mode2, mode3);

        Label title = new Label("Results screen:");
        title.setLayoutX(20);
        title.setLayoutY(20);

        aPressed.setLayoutX(400);
        aPressed.setLayoutY(20);
        bPressed.setLayoutX(480);
        bPressed.setLayoutY(20);
        allPressed.setLayoutX(560);
        allPressed.setLayoutY(20);

        Button reset = new Button("Reset");
        reset.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                countA = 0;
                aPressed.setText("A: 0");
                countB = 0;
                bPressed.setText("B: 0");
                countAll = 0;
                allPressed.setText("All: 0");
            }
        });

        reset.setLayoutX(650);
        reset.setLayoutY(10);

        Label dpiL = new Label("DPI:");
        dpiText.setEditable(false);
        HBox dpiBox = new HBox(15);
        dpiBox.getChildren().addAll(dpiL, dpiText);
        dpiBox.setLayoutX(20);
        dpiBox.setLayoutY(700);

        Pane mainPane = new Pane(title, rec, aPressed, bPressed, allPressed,
                reset, optRadioBox, modeRadioBox, buttonsPane, dpiBox);
        Scene scene = new Scene(mainPane, Screen.getPrimary().getVisualBounds()
                .getWidth(), Screen.getPrimary().getVisualBounds().getHeight());
        scene.getStylesheets().add("hello/HelloTouchStylesheet.css");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public void setDefaultMode(Control b1, Control b2) {
        b1.setOnMousePressed(redPressHandler);
        b1.setOnMouseDragged(null);
        b2.setOnMousePressed(greenPressHandler);
        b2.setOnMouseDragged(null);
    }

    public void setEditMode(final Control b1, final Control b2) {
        pressedX = 0;
        pressedY = 0;
        final EventHandler<MouseEvent> onpress = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                pressedX = event.getX();
                pressedY = event.getY();
            }
        };
        b1.setOnMousePressed(onpress);
        b1.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                b1.setLayoutX(b1.getLayoutX() + event.getX() - pressedX);
                b1.setLayoutY(b1.getLayoutY() + event.getY() - pressedY);
                currentAX = pixelToCm(b1.getLayoutX());
                currentAY = pixelToCm(b1.getLayoutY());
            }
        });
        b2.setOnMousePressed(onpress);
        b2.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                b2.setLayoutX(b2.getLayoutX() + event.getX() - pressedX);
                b2.setLayoutY(b2.getLayoutY() + event.getY() - pressedY);
                currentBX = pixelToCm(b2.getLayoutX());
                currentBY = pixelToCm(b2.getLayoutY());
            }
        });
    }

    public double cmToPixel(double cm) {
        double DPI = Double.parseDouble(dpiText.getText());
        return (cm / 2.54) * DPI;
    }

    public double pixelToCm(double pixels) {
        double DPI = Double.parseDouble(dpiText.getText());
        return (pixels / DPI) * 2.54;
    }

    public void updateButtonPane() {
        if (modeFlag == 1) {
            buttonsPane.getChildren().setAll(frame, btnA, btnB);
            option3.setDisable(false);
            if (editMode.isDisabled()) {
                setDefaultMode(btnA, btnB);
            }
            switch (optionFlag) {
            case 1:
                btnSizeSlider.setDisable(true);
                editMode.setDisable(true);
                setDefaultMode(btnA, btnB);
                updateButtons(btnA, btnB, 2.0, 1.0, new Point2D(7.0, 3.0),
                        new Point2D(10.0, 3.0));
                break;
            case 2:
                btnSizeSlider.setDisable(true);
                editMode.setDisable(true);
                setDefaultMode(btnA, btnB);
                updateButtons(btnA, btnB, 2.0, 1.0, new Point2D(7.0, 3.0),
                        new Point2D(9.2, 3.0));
                break;
            case 3:
                btnSizeSlider.setDisable(true);
                editMode.setDisable(true);
                setDefaultMode(btnA, btnB);
                updateButtons(btnA, btnB, 2.0, 1.0, new Point2D(7.0, 3.0),
                        new Point2D(9.0, 3.0));
                break;
            case 4:
                btnSizeSlider.setDisable(true);
                editMode.setDisable(true);
                setDefaultMode(btnA, btnB);
                updateButtons(btnA, btnB, 0.6, 0.6, new Point2D(7.0, 3.0),
                        new Point2D(9.0, 3.0));
                buttonsPane.getChildren().setAll(frame, btnA);
                break;
            case 5:
                if (editMode.isSelected()) {
                    setEditMode(btnA, btnB);
                } else {
                    setDefaultMode(btnA, btnB);
                }
                updateButtons(btnA, btnB, (double) btnSizeSlider.getValue(),
                        (double) btnSizeSlider.getValue(), new Point2D(
                                currentAX, currentAY), new Point2D(currentBX,
                                currentBY));
                btnSizeSlider.setDisable(false);
                editMode.setDisable(false);
                btnSizeSlider.valueProperty().addListener(
                        (observable, oldvalue, newvalue) -> {
                            updateButtons(btnA, btnB, (Double) newvalue,
                                    (Double) newvalue, new Point2D(currentAX,
                                            currentAY), new Point2D(currentBX,
                                            currentBY));
                        });
                editMode.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        if (editMode.isSelected()) {
                            setEditMode(btnA, btnB);
                        } else {
                            setDefaultMode(btnA, btnB);
                        }
                    }
                });
                break;
            }
        } else if (modeFlag == 2) {
            buttonsPane.getChildren().setAll(frame, rbtnA, rbtnB);
            option3.setDisable(true);
            switch (optionFlag) {
            case 1:
                btnSizeSlider.setDisable(true);
                editMode.setDisable(true);
                setDefaultMode(rbtnA, rbtnB);
                updateButtons(rbtnA, rbtnB, 2.0, 0.6, new Point2D(7.0, 3.0),
                        new Point2D(7.0, 4.0));
                break;
            case 2:
                btnSizeSlider.setDisable(true);
                editMode.setDisable(true);
                setDefaultMode(rbtnA, rbtnB);
                updateButtons(rbtnA, rbtnB, 2.0, 0.6, new Point2D(7.0, 3.0),
                        new Point2D(7.0, 3.7));
                break;
            case 3:
                option1.setSelected(true);
                btnSizeSlider.setDisable(true);
                editMode.setDisable(true);
                setDefaultMode(rbtnA, rbtnB);
                updateButtons(rbtnA, rbtnB, 2.0, 0.6, new Point2D(7.0, 3.0),
                        new Point2D(7.0, 4.0));
                break;
            case 4:
                btnSizeSlider.setDisable(true);
                editMode.setDisable(true);
                setDefaultMode(rbtnA, rbtnB);
                updateButtons(rbtnA, rbtnB, 2.0, 0.4, new Point2D(7.0, 3.0),
                        new Point2D(7.0, 3.5));
                buttonsPane.getChildren().setAll(frame, rbtnA);
                break;
            case 5:
                if (editMode.isSelected()) {
                    setEditMode(rbtnA, rbtnB);
                } else {
                    setDefaultMode(rbtnA, rbtnB);
                }
                updateButtons(rbtnA, rbtnB, (double) btnSizeSlider.getValue(),
                        (double) btnSizeSlider.getValue(), new Point2D(
                                currentAX, currentAY), new Point2D(currentBX,
                                currentBY));
                btnSizeSlider.setDisable(false);
                editMode.setDisable(false);
                btnSizeSlider.valueProperty().addListener(
                        (observable, oldvalue, newvalue) -> {
                            updateButtons(rbtnA, rbtnB, (Double) newvalue,
                                    (Double) newvalue, new Point2D(currentAX,
                                            currentAY), new Point2D(currentBX,
                                            currentBY));
                        });
                editMode.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        if (editMode.isSelected()) {
                            setEditMode(rbtnA, rbtnB);
                        } else {
                            setDefaultMode(rbtnA, rbtnB);
                        }
                    }
                });
                break;
            }
        } else if (modeFlag == 3) {
            buttonsPane.getChildren().setAll(frame, chbtnA, chbtnB);
            option3.setDisable(true);
            switch (optionFlag) {
            case 1:
                btnSizeSlider.setDisable(true);
                editMode.setDisable(true);
                setDefaultMode(chbtnA, chbtnB);
                updateButtons(chbtnA, chbtnB, 2.0, 0.6, new Point2D(7.0, 3.0),
                        new Point2D(7.0, 4.0));
                break;
            case 2:
                btnSizeSlider.setDisable(true);
                editMode.setDisable(true);
                setDefaultMode(chbtnA, chbtnB);
                updateButtons(chbtnA, chbtnB, 2.0, 0.6, new Point2D(7.0, 3.0),
                        new Point2D(7.0, 3.7));
                break;
            case 3:
                option1.setSelected(true);
                btnSizeSlider.setDisable(true);
                editMode.setDisable(true);
                setDefaultMode(chbtnA, chbtnB);
                updateButtons(chbtnA, chbtnB, 2.0, 0.6, new Point2D(7.0, 3.0),
                        new Point2D(7.0, 4.0));
                break;
            case 4:
                btnSizeSlider.setDisable(true);
                editMode.setDisable(true);
                setDefaultMode(chbtnA, chbtnB);
                updateButtons(chbtnA, chbtnB, 2.0, 0.4, new Point2D(7.0, 3.0),
                        new Point2D(7.0, 3.5));
                buttonsPane.getChildren().setAll(frame, chbtnA);
                break;
            case 5:
                if (editMode.isSelected()) {
                    setEditMode(chbtnA, chbtnB);
                } else {
                    setDefaultMode(chbtnA, chbtnB);
                }
                updateButtons(chbtnA, chbtnB,
                        (double) btnSizeSlider.getValue(),
                        (double) btnSizeSlider.getValue(), new Point2D(
                                currentAX, currentAY), new Point2D(currentBX,
                                currentBY));
                btnSizeSlider.setDisable(false);
                editMode.setDisable(false);
                btnSizeSlider.valueProperty().addListener(
                        (observable, oldvalue, newvalue) -> {
                            updateButtons(chbtnA, chbtnB, (Double) newvalue,
                                    (Double) newvalue, new Point2D(currentAX,
                                            currentAY), new Point2D(currentBX,
                                            currentBY));
                        });
                editMode.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        if (editMode.isSelected()) {
                            setEditMode(chbtnA, chbtnB);
                        } else {
                            setDefaultMode(chbtnA, chbtnB);
                        }
                    }
                });
                break;
            }
        }
    }

    public void updateButtons(Control b1, Control b2, double width,
            double height, Point2D p1, Point2D p2) {

        double DPI = 90.0;

        if (b1 instanceof RadioButton) {
            ((RadioButton) b1).setFont(new Font(cmToPixel(height) * 0.65));
            ((RadioButton) b2).setFont(new Font(cmToPixel(height) * 0.65));
        } else if (b1 instanceof CheckBox) {
            ((CheckBox) b1).setFont(new Font(cmToPixel(height) * 0.65));
            ((CheckBox) b2).setFont(new Font(cmToPixel(height) * 0.65));
        } else if (b1 instanceof Button) {
            b1.setPrefSize(cmToPixel(width), cmToPixel(height));
            b1.setMinSize(cmToPixel(width), cmToPixel(height));
            b1.setMaxSize(cmToPixel(width), cmToPixel(height));
            b2.setPrefSize(cmToPixel(width), cmToPixel(height));
            b2.setMinSize(cmToPixel(width), cmToPixel(height));
            b2.setMaxSize(cmToPixel(width), cmToPixel(height));
        }
        b1.setLayoutX(cmToPixel(p1.getX()));
        b1.setLayoutY(cmToPixel(p1.getY()));
        b2.setLayoutX(cmToPixel(p2.getX()));
        b2.setLayoutY(cmToPixel(p2.getY()));
    }

    public void increaseCount(Label l, String name) {
        switch (name) {
        case "A: ":
            countA++;
            l.setText(name + countA);
            break;
        case "B: ":
            countB++;
            l.setText(name + countB);
            break;
        case "All: ":
            countAll++;
            l.setText(name + countAll);
            break;
        }
    }

}