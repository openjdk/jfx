/*
 * Copyright (c) 2016, Oracle and/or its affiliates.
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
package minesweeperfx;


import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import jdk.packager.services.singleton.SingleInstanceService;
import jdk.packager.services.singleton.SingleInstanceListener;


public class MinesweeperFX extends Application implements SingleInstanceListener {
    Game game;

    @Override
    public void newActivation(String... params) {
        for (int i = 0; i < params.length; i++) {
            System.out.println("Arg[" + i + "]: " + params[i]);
        }
    }

    @Override
    public void stop() {
         SingleInstanceService.unregisterSingleInstance(this);
    }

    @Override
    public void start(Stage primaryStage) {
        // the app will be single instance only if the option
        // "-singleton" is specified for javapackager
        SingleInstanceService.registerSingleInstance(this);
        BorderPane root = new BorderPane();
        HBox hbox = new HBox();
        hbox.setPadding(new Insets(5));
        hbox.setSpacing(20);
        hbox.setAlignment(Pos.CENTER_LEFT);
        root.setTop(hbox);

        Button newGameButton = new Button();
        newGameButton.setText("New Game");
        newGameButton.setOnAction((event) -> game.newGame("New Game"));

        Text minesDescriptionLabel = new Text();
        minesDescriptionLabel.setText("Mines:");

        Text minesLabel = new Text();
        minesLabel.setText("0");

        hbox.getChildren().add(newGameButton);
        hbox.getChildren().add(minesDescriptionLabel);
        hbox.getChildren().add(minesLabel);

        game = new Game(primaryStage, hbox.getHeight() + 45, minesLabel);
        game.newGame(Game.GameDifficulty.Easy);
        Canvas canvas = new Canvas(512, 512);
        game.getChildren().add(canvas);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        game.setGraphicsContext(gc);

        game.setOnMouseMoved((event) -> {
            Point point = new Point(event.getX(), event.getY());
            game.draw(gc, point);
        });

        game.setOnMousePressed((event) -> {
            if (event.isPrimaryButtonDown() && (event.isControlDown() == false)) {
                if (Globals.debug) {
                    System.out.println("click " + event.getX() + "  " + event.getY());
                }

                Point point = new Point(event.getX(), event.getY());
                game.leftClick(point);
            }
            else if (event.isSecondaryButtonDown() ||
                     (event.isPrimaryButtonDown() && (event.isControlDown() == true))) {
                if (Globals.debug) {
                    System.out.println("rightclick " + event.getX() + "  " + event.getY());
                }

                Point point = new Point(event.getX(), event.getY());
                game.rightClick(point);
            }
        });

        root.setCenter(game);

        game.draw(gc, null);
        Scene scene = new Scene(root);

        primaryStage.setResizable(false);
        primaryStage.setTitle("MinesweeperFX");
        primaryStage.setScene(scene);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
