/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package robottest;

import com.sun.glass.events.KeyEvent;
import com.sun.glass.ui.Robot;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.Buffer;
import java.nio.IntBuffer;
import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Stage;


public class RobotBuilder {

    //Variable used by "RobotTest" section
    private final Rectangle rec1 = new Rectangle(50, 50, 40, 160);
    private Popup screenShot;

    private static RobotBuilder instance;

//    protected TestRobot {}

    public static RobotBuilder getInstance() {
        if (instance==null)
                 instance = new RobotBuilder();
        return instance;
    }

    /**
     * The method updates globalScene with robot tests
     * @param globalScene the global Scene
     * @param mainBox the Box to insert into
     * @param robotStage the Robot Stage
     */
    void robotTest(final Scene globalScene, final VBox mainBox,
                          final Stage robotStage){

        Label l = new Label("Robot features Demo");
        Group lGroup = new Group(l);
        lGroup.setLayoutX(400);
        lGroup.setLayoutY(10);

        //Rectangle's coordinates
        final int recX = 50;
        final int recY = 50;

        Group allGroup = new Group();
        rec1.setFill(Color.RED);
        Rectangle rec2 = new Rectangle(recX + 40, recY, 40, 160);
        rec2.setFill(Color.BLUE);
        Rectangle rec3 = new Rectangle(recX + 80, recY, 40, 160);
        rec3.setFill(Color.YELLOW);
        Rectangle rec4 = new Rectangle(recX + 120, recY, 40, 160);
        rec4.setFill(Color.GREEN);

        GridPane grid = new GridPane();
        grid.setVgap(50);
        grid.setHgap(20);
        grid.setLayoutX(recX + 300);
        grid.setLayoutY(recY + 50);

        final TextField result1 = new TextField("Result");
        result1.setEditable(false);
        Button screenTestBtn = new Button("Robot Get Screen Capture Test");
        screenTestBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
               screenShot = robotScreenTest(result1, robotStage);
            }
        });

        grid.setConstraints(screenTestBtn, 0, 0);
        grid.getChildren().add(screenTestBtn);
        grid.setConstraints(result1, 1, 0);
        grid.getChildren().add(result1);

        final TextField result2 = new TextField("Result");
        result2.setEditable(false);
        Button pixelTestBtn = new Button("Robot Get Pixel Color Test");
        pixelTestBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
               robotPixelTest(result2, robotStage);
            }
        });

        grid.setConstraints(pixelTestBtn, 0, 1);
        grid.getChildren().add(pixelTestBtn);
        grid.setConstraints(result2, 1, 1);
        grid.getChildren().add(result2);

        //KeyPressRelesase
        final TextField writeField = new TextField("");
        Group writeFieldGroup = new Group(writeField);
        writeFieldGroup.setLayoutX(recX);
        writeFieldGroup.setLayoutY(recY + 200);

        final TextField result3 = new TextField("Result");
        result3.setEditable(false);

        Button keyTestBtn = new Button("Robot Key Press/Release Test");
        keyTestBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                robotKeyTest(writeField, result3);
            }
        });

        grid.setConstraints(keyTestBtn, 0, 2);
        grid.getChildren().add(keyTestBtn);
        grid.setConstraints(result3, 1, 2);
        grid.getChildren().add(result3);

        //Mouse wheel
        final ListView<String> sv = new ListView<String>();
        ObservableList<String> items =FXCollections.observableArrayList (
                    "a", "b", "c", "d", "e", "f", "g", "h", "i");
        sv.setItems(items);
        sv.setPrefWidth(100);
        sv.setPrefHeight(100);

        Group svGroup = new Group(sv);
        svGroup.setLayoutX(recX);
        svGroup.setLayoutY(recY + 250);

        final TextField result4 = new TextField("Result");
        result4.setEditable(false);

        Button wheelTestBtn = new Button("Robot Mouse Press/Release/Wheel Test");
        wheelTestBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                robotWheelTest(sv, result4, robotStage);
            }
        });

        grid.setConstraints(wheelTestBtn, 0, 3);
        grid.getChildren().add(wheelTestBtn);
        grid.setConstraints(result4, 1, 3);
        grid.getChildren().add(result4);

        Button btn = new Button("Back");
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                if ((screenShot != null) && (screenShot.isShowing())) {
                    screenShot.hide();
                }
                globalScene.setRoot(mainBox);
            }
        });
        Group btnGroup = new Group(btn);
        btnGroup.setLayoutX(450);
        btnGroup.setLayoutY(450);

        allGroup.getChildren().addAll(rec1, rec2, rec3, rec4, grid, lGroup, btnGroup,
                                      writeFieldGroup, svGroup);
        globalScene.setRoot(allGroup);
    }


    public void robotKeyTest(final TextField field, final TextField result) {
        field.requestFocus();
        new AnimationTimer() {
            long startTime = System.nanoTime();
            @Override
            public void handle(long now) {
                if (now > startTime + 3000000000l){
                    stop();
                    field.setText("Failed");
                } else if (field.isFocused()) {
                    stop();
                    Robot robot = com.sun.glass.ui.Application.GetApplication().createRobot();
                    robot.keyPress(KeyEvent.VK_T);
                    robot.keyRelease(KeyEvent.VK_T);
                    robot.keyPress(KeyEvent.VK_E);
                    robot.keyRelease(KeyEvent.VK_E);
                    robot.keyPress(KeyEvent.VK_S);
                    robot.keyRelease(KeyEvent.VK_S);
                    robot.keyPress(KeyEvent.VK_T);
                    robot.keyRelease(KeyEvent.VK_T);
                    robot.destroy();
                    new AnimationTimer() {
                        long startTime = System.nanoTime();
                        @Override
                        public void handle(long now) {
                            if (now > startTime + 3000000000l){
                                stop();
                                result.setText("Failed");
                            } else if ((field.getText()).equals("test")) {
                                stop();
                                result.setText("Passed");
                            }
                        }
                    }.start();
                }
            }
        }.start();
    }

    public void robotWheelTest(final ListView<String> lv, final TextField result,
                                                            Stage currentStage){

        //Caclulation of ListView minimal coordinates
        Bounds bounds = lv.localToScreen(new BoundingBox(0, 0,
            lv.getBoundsInParent().getWidth(),
            lv.getBoundsInParent().getHeight()));
        int x = 10 + (int) bounds.getMinX();
        int y = 10 + (int) bounds.getMinY();

        final Robot robot =
                    com.sun.glass.ui.Application.GetApplication().createRobot();
        robot.mouseMove(x, y);
        robot.mousePress(Robot.MOUSE_LEFT_BTN);
        robot.mouseRelease(Robot.MOUSE_LEFT_BTN);

        new AnimationTimer() {
            long startTime = System.nanoTime();
            @Override
            public void handle(long now) {
                if (now > startTime + 3000000000l){
                    stop();
                    result.setText("Failed");
                } else if (lv.isFocused()) {
                    stop();
                    robot.mouseWheel(-5);
                    robot.mousePress(Robot.MOUSE_LEFT_BTN);
                    robot.mouseRelease(Robot.MOUSE_LEFT_BTN);
                    robot.destroy();
                    new AnimationTimer() {
                        long startTime = System.nanoTime();
                        @Override
                        public void handle(long now) {
                            if (now > startTime + 3000000000l){
                                stop();
                                result.setText("Scroll Down Failed");
                            } else if (!lv.getSelectionModel().
                                    selectedItemProperty().getValue().
                                    equals("a")) {
                                        stop();
                                    result.setText("Scroll Down Passed");
                            }
                        }
                    }.start();
                }
            }
        }.start();
    }

    public void robotPixelTest(final TextField result, Stage currentStage){

        Bounds bounds = rec1.localToScreen(new BoundingBox(0, 0,
                        rec1.getBoundsInParent().getWidth(),
                        rec1.getBoundsInParent().getHeight()));
        int x = 53 + (int) bounds.getMinX();
        int y = 53 + (int) bounds.getMinY();
        int answer = assertPixelEquals(x, y, Color.RED) +
                     assertPixelEquals(x + 40, y, Color.BLUE) +
                     assertPixelEquals(x + 80, y, Color.YELLOW) +
                     assertPixelEquals(x + 120, y, Color.GREEN);
        if (answer == 4) {
            result.setText("Passed");
        } else {
            result.setText("Failed");
        }

    }

    static int colorToRGB(Color c) {
        int r = (int) Math.round(c.getRed() * 255.0);
        int g = (int) Math.round(c.getGreen() * 255.0);
        int b = (int) Math.round(c.getBlue() * 255.0);
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    public int assertPixelEquals(int x, int y, Color expected){

        Robot robot = com.sun.glass.ui.Application.GetApplication().createRobot();
        int pixel = robot.getPixelColor(x, y);
        robot.destroy();
        int expectedPixel = colorToRGB(expected);
        if (checkColor(pixel, expected)) {
            return 1;
        } else {
            System.out.println("Expected color 0x" + Integer.toHexString(expectedPixel) +
                    " at " + x + "," + y + " but found 0x" + Integer.toHexString(pixel));
        }
        return 0;
    }

    private boolean checkColor(int value, Color expected) {

        double tolerance = 0.07;
        double ered = expected.getRed();
        double egrn = expected.getGreen();
        double eblu = expected.getBlue();

        double vred = ((value & 0xff0000) >> 16) / 255.0;
        double vgrn = ((value & 0x00ff00) >> 8) / 255.0;
        double vblu = ((value & 0x0000ff)) / 255.0;

        double dred = Math.abs(ered - vred);
        double dgrn = Math.abs(egrn - vgrn);
        double dblu = Math.abs(eblu - vblu);

        if (dred <= tolerance && dgrn <= tolerance && dblu <= tolerance) {
            return true;
        }

        return false;
    }

    public Popup robotScreenTest(final TextField result, Stage stage){

        Bounds bounds = rec1.localToScreen(new BoundingBox(0, 0,
                rec1.getBoundsInParent().getWidth(),
                rec1.getBoundsInParent().getHeight()));

        int x = 50 + (int) bounds.getMinX();
        int y = 50 + (int) bounds.getMinY();
        int[] intArr = null;
        boolean correct = true;
        Robot robot = com.sun.glass.ui.Application.GetApplication().createRobot();
        int width = 160;
        int height = 160;
        final Buffer buff = robot.getScreenCapture(x, y, width, height).getPixels();
        if ((buff instanceof IntBuffer)&&(buff.hasArray())) {
            intArr =((IntBuffer) buff).array();
        }

        String filename= "scrCapture.bmp";
        File file = new File(filename);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            BMPOutputStream bmp = new BMPOutputStream(new FileOutputStream(filename), intArr, width, height);
        } catch (Exception e) {}


        for (int i = width; i <= height*(height-1); i += width) {
            for (int j = 1; j <= 38; j ++){
                if (!checkColor(intArr[j+i],Color.RED)) {
                    System.out.println(" pixel("+j+","+(i/width)+") "+
                            Integer.toHexString(intArr[j+i])+" != "+
                            Integer.toHexString(colorToRGB(Color.RED)));
                    correct = false;
                }
             }
            for (int j = 41; j <= 78; j ++){
                if (!checkColor(intArr[j+i],Color.BLUE)) {
                    System.out.println(" pixel("+j+","+(i/width)+") "+
                            Integer.toHexString(intArr[j+i])+" != "+
                            Integer.toHexString(colorToRGB(Color.BLUE)));
                    correct = false;
                }
             }
            for (int j = 81; j <= 118; j ++){
                if (!checkColor(intArr[j+i],Color.YELLOW)) {
                    System.out.println(" pixel("+j+","+(i/width)+") "+
                            Integer.toHexString(intArr[j+i])+" != "+
                            Integer.toHexString(colorToRGB(Color.YELLOW)));
                    correct = false;
                }
             }
            for (int j = 121; j <= 158; j ++){
                if (!checkColor(intArr[j+i],Color.GREEN)) {
                    System.out.println(" pixel("+j+","+(i/width)+") "+
                            Integer.toHexString(intArr[j+i])+" != "+
                            Integer.toHexString(colorToRGB(Color.GREEN)));
                    correct = false;
                }
            }
        }
        robot.destroy();
        if (correct) {
            result.setText("Passed");
        } else {
            result.setText("Failed");
        }
        return showImage(stage, width, height, result);
    }

    private Popup showImage(Stage stage, int width, int height, TextField tf) {

        int frame = 70;
        Rectangle rec = new Rectangle(width + frame, height + frame);
        FileInputStream os = null;
        final File file = new File("scrCapture.bmp");
        try {
            os = new FileInputStream(file);
        } catch (Exception e) {}

        final Popup popup = new Popup();
        ImageView iv = new ImageView(new Image(os));
        iv.setLayoutX(frame/2);
        iv.setLayoutY(frame/2);

        rec.setFill(Color.WHITE);
        rec.setStroke(Color.BLACK);
        Button exit = new Button("x");
        exit.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                if (file.exists()&&tf.getText().equals("Passed")) {
                    file.deleteOnExit();
                }
                popup.hide();
            }
        });
        exit.setLayoutX(width + frame/2);
        Pane popupPane = new Pane(rec, iv, exit);
        popup.setX(stage.getX() + 550);
        popup.setY(stage.getY() + 430);
        popup.getContent().addAll(popupPane);
        popup.show(stage);
        return popup;
    }
}
