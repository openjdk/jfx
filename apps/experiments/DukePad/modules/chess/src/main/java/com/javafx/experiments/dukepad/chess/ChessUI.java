/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.chess;

import com.javafx.experiments.dukepad.chess.client.ChessClient;
import com.javafx.experiments.dukepad.chess.client3d.ChessBoard;
import com.oracle.chess.model.Board;
import com.oracle.chess.model.Color;
import com.oracle.chess.protocol.QueryGamesRsp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.*;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;

/**
 * Chess UI
 */
public class ChessUI extends Pane {
//    public static final String URL = System.getProperty("chessserver.url", "ws://localhost:8080/chess/chessserver");
    public static final String URL = System.getProperty("chessserver.url", "ws://10.158.38.10:7080/chess/chessserver");
    private static final Boolean DISABLE_LIGHTS = Boolean.getBoolean("disableLights");
    private static String gameId = null;
    private ChessClient chessClient;
    private MainMenu mainMenu;


    public ChessUI() {
        setBackground(new Background(new BackgroundFill[0]));
        chessBoard = new ChessBoard(new Board());

        setDepthTest(DepthTest.DISABLE);
        setPickOnBounds(false);

        mainMenu = new MainMenu(this);

        Button menuButton = new Button("Menu");
        menuButton.layoutXProperty().bind(widthProperty().subtract(menuButton.widthProperty()).subtract(10));
        menuButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                showSubMenu(mainMenu);
            }
        });

        Group root = new Group(chessBoard);
        root.setDepthTest(DepthTest.ENABLE);
        root.setPickOnBounds(false);
        getChildren().addAll(root, menuButton);
        double cx = getBoundsInLocal().getWidth() / 2;
        double cy = getBoundsInLocal().getHeight() / 2;
        double cz = getBoundsInLocal().getDepth() / 2;
        final Translate translate = new Translate(450, 360, 1060);
        root.getTransforms().addAll(new Rotate(40 + 180, cx, cy, cz, Rotate.X_AXIS), translate);

        this.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                // consuming any mouse events that happen over the app area
                mouseEvent.consume();
            }
        });

        if (!DISABLE_LIGHTS) {
            setupLights(this);
        }

        startNewGame(Color.W, false);
    }

    private static final double DISTANCE = 2000;

    private void setupLights(Pane lightsGroup) {
        final PointLight fillLight = new PointLight(  new javafx.scene.paint.Color(0.7264957427978516,0.6716462969779968,0.5619475245475769,1));
//        fillLight.translateZProperty().bind(cameraDistance.multiply(0));
        fillLight.setTranslateZ(DISTANCE * -1);
        fillLight.setTranslateX(DISTANCE * 0.6);
        fillLight.setTranslateY(DISTANCE * -0.08);
        lightsGroup.getChildren().add(fillLight);

        final PointLight keyLight = new PointLight( new javafx.scene.paint.Color(0.560742199420929,0.6359063982963562,0.6495726704597473,1));
//        keyLight.translateZProperty().bind(cameraDistance.multiply(0));
        keyLight.setTranslateZ(DISTANCE * -1);
        keyLight.setTranslateX(DISTANCE * -0.8);
        keyLight.setTranslateY(DISTANCE * -0.08);
        lightsGroup.getChildren().add(keyLight);

//        final PointLight rimLight = new PointLight(javafx.scene.paint.Color.LIGHTBLUE);
        final PointLight rimLight = new PointLight(new javafx.scene.paint.Color(0.22359193861484528,0.38724663853645325,0.6794871687889099,1));
        rimLight.setTranslateZ(DISTANCE);
        rimLight.setTranslateY(DISTANCE * -1.2);
        lightsGroup.getChildren().add(rimLight);
    }

    private ChessBoard chessBoard;

    public void startNewGame() {
        startNewGame(Color.W, true);
    }

    public void startNewGame(Color color, boolean runRandom) {
        if (chessClient != null) {
            chessClient.close();
        }

        chessClient = new ChessClient(URL, color, runRandom, chessBoard);
        chessClient.startThread();
    }

    public void joinGame(String gameId) {
        if (chessClient != null) {
            chessClient.close();
        }

        chessClient = new ChessClient(URL, gameId, chessBoard);
        chessClient.startThread();
    }

    private List<Region> menus = new ArrayList<>();

    public void showSubMenu(Region menu) {
        menus.add(menu);
        getChildren().add(menu);
        menu.layoutXProperty().bind(widthProperty().subtract(menu.widthProperty()));
    }

    public void hideMenus() {
        getChildren().removeAll(menus);
        for (Region menu : menus) {
            menu.layoutXProperty().unbind();
        }
        menus.clear();
    }

    public void hideMenu() {
        Region menu = menus.remove(menus.size() - 1);
        getChildren().remove(menu);
        menu.layoutXProperty().unbind();
    }

    public void queryGames(final Callback<QueryGamesRsp, Void> callback) {
        // TODO: We assume chessClient != null
        chessClient.queryGames(new Callback<QueryGamesRsp, Void>() {
            @Override
            public Void call(final QueryGamesRsp queryGamesRsp) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        callback.call(queryGamesRsp);
                    }
                });
                return null;
            }
        });
    }
}
