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

import com.oracle.chess.model.Color;
import com.oracle.chess.protocol.QueryGamesRsp;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Callback;


public class JoinGameMenu extends VBox {

    public JoinGameMenu(final ChessUI chessUI) {

        final ListView<QueryGamesRsp.Game> listView = new ListView<>();
        listView.setCellFactory(new Callback<ListView<QueryGamesRsp.Game>, ListCell<QueryGamesRsp.Game>>() {
            @Override
            public ListCell<QueryGamesRsp.Game> call(ListView<QueryGamesRsp.Game> gameListView) {
                return new ListCell<QueryGamesRsp.Game>() {
                    {
                        itemProperty().addListener(new ChangeListener<QueryGamesRsp.Game>() {
                            @Override
                            public void changed(ObservableValue<? extends QueryGamesRsp.Game> observableValue, QueryGamesRsp.Game oldGame, QueryGamesRsp.Game game) {
                                if (game == null) {
                                    setText(null);
                                } else {
                                    setText(game.getSummary() + " (" + game.getGameId() + ')');
                                    setTextFill(game.isOpen() ? javafx.scene.paint.Color.BLACK : javafx.scene.paint.Color.DARKGRAY);
                                }
                            }
                        });
                    }
                };
            }
        });

        final ProgressIndicator progressIndicator = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressIndicator.setMaxSize(USE_PREF_SIZE, ProgressIndicator.USE_PREF_SIZE);

        getChildren().add(new StackPane(listView, progressIndicator));

        Button start = new Button("Start");
        start.setMaxWidth(Double.MAX_VALUE);
        start.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());
        start.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                chessUI.joinGame(listView.getSelectionModel().getSelectedItem().getGameId());
                chessUI.hideMenus();
            }
        });
        HBox.setHgrow(start, Priority.ALWAYS);

        Button back = new Button("Back");
        back.setMaxWidth(Double.MAX_VALUE);
        back.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                chessUI.hideMenu();
            }
        });
        HBox.setHgrow(back, Priority.ALWAYS);

        getChildren().add(new HBox(start, back));

        setFillWidth(true);

        sceneProperty().addListener(new ChangeListener<Scene>() {
            @Override
            public void changed(ObservableValue<? extends Scene> observableValue, Scene scene, Scene scene2) {
                if (scene2 != null) {
                    chessUI.queryGames(new Callback<QueryGamesRsp, Void>() {

                        @Override
                        public Void call(QueryGamesRsp queryGamesRsp) {
                            progressIndicator.setVisible(false);
                            listView.getItems().setAll(queryGamesRsp.getGames());
                            return null;
                        }
                    });
                } else {
                    listView.getItems().clear();
                    progressIndicator.setVisible(true);
                }
            }
        });
    }
}
