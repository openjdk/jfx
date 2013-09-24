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


import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;


public class MainMenu extends VBox {

    private NewGameMenu newGameMenu;
    private JoinGameMenu joinGameMenu;

    public MainMenu(final ChessUI chessUI) {
        newGameMenu = new NewGameMenu(chessUI);
        joinGameMenu = new JoinGameMenu(chessUI);

        Button newGame = new Button("New game");
        newGame.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                chessUI.showSubMenu(newGameMenu);
            }
        });
        newGame.setMaxWidth(Double.MAX_VALUE);
        getChildren().addAll(newGame);
        Button joinGame = new Button("Join game");
        joinGame.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                chessUI.showSubMenu(joinGameMenu);
            }
        });
        joinGame.setMaxWidth(Double.MAX_VALUE);
        getChildren().addAll(joinGame);
//        Button exit = new Button("Exit");
//        newGame.setOnAction(new EventHandler<ActionEvent>() {
//
//            @Override
//            public void handle(ActionEvent t) {
//                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//            }
//        });
//        getChildren().addAll(exit);
        setFillWidth(true);
    }

    private void hideMenu() {
        setVisible(false);
    }
}
