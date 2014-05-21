/*
 * Copyright (c) 2008, 2013 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ensemble.samples.scenegraph.advancedstage;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.Lighting;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * A sample with a control that creates a transparent stage that is centered on
 * your desktop. You can drag the stage with your mouse or use the scene
 * controls to minimize or close it. With a transparent stage, you must add
 * your own event handlers to perform these actions.
 *
 * @sampleName Advanced Stage
 * @preview preview.png
 * @see javafx.stage.Stage
 * @see javafx.scene.Scene
 * @see javafx.stage.StageStyle
 * @see javafx.application.Platform
 * @related /Scenegraph/Stage
 */
public class AdvancedStageApp extends Application {

    //variables for storing initial position of the stage at the beginning of drag
    private double initX;
    private double initY;

    public Parent createContent() {
        //create a button for initializing our new stage
        Button button = new Button("Create a Stage");
        button.setStyle("-fx-font-size: 24;");
        button.setDefaultButton(true);
        button.setOnAction((ActionEvent t) -> {
            final Stage stage = new Stage(StageStyle.TRANSPARENT);
            Group rootGroup = new Group();
            Scene scene = new Scene(rootGroup, 200, 200, Color.TRANSPARENT);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
            Circle dragger = new Circle(100, 100, 100);
            dragger.setFill(new RadialGradient(-0.3, 135, 0.5, 0.5, 1, true, CycleMethod.NO_CYCLE, new Stop[]{
                new Stop(0, Color.DARKGRAY),
                new Stop(1, Color.BLACK)
            }));
            rootGroup.setOnMousePressed((MouseEvent me) -> {
                initX = me.getScreenX() - stage.getX();
                initY = me.getScreenY() - stage.getY();
            });
            rootGroup.setOnMouseDragged((MouseEvent me) -> {
                stage.setX(me.getScreenX() - initX);
                stage.setY(me.getScreenY() - initY);
            });
            Button close = new Button("Close me");
            close.setOnAction((ActionEvent event) -> {
                stage.close();
            });
            Button min = new Button("Minimize me");
            min.setOnAction((ActionEvent event) -> {
                stage.setIconified(true);
            });
            Text text = new Text("JavaFX");
            text.setFill(Color.WHITESMOKE);
            text.setEffect(new Lighting());
            text.setBoundsType(TextBoundsType.VISUAL);
            text.setFont(Font.font(Font.getDefault().getFamily(), 50));
            VBox vBox = new VBox();
            vBox.setSpacing(10);
            vBox.setPadding(new Insets(60, 0, 0, 20));
            vBox.setAlignment(Pos.TOP_CENTER);
            vBox.getChildren().addAll(text, min, close);
            rootGroup.getChildren().addAll(dragger, vBox);
        });
        return button;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
    }

    /**
     * Java main for when running without JavaFX launcher
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
