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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class NewGameMenu extends VBox {

    public NewGameMenu(final ChessUI chessUI) {

        ToggleButton white = new ToggleButton("White");
        white.setSelected(true);
        white.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(white, Priority.ALWAYS);

        final ToggleButton black = new ToggleButton("Black");
        black.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(black, Priority.ALWAYS);

        final ToggleGroup color = new ToggleGroup();
        color.getToggles().addAll(white, black);
        color.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override
            public void changed(ObservableValue<? extends Toggle> observableValue, Toggle toggle, Toggle toggle2) {
                if (toggle2 == null && toggle != null) {
                    toggle.setSelected(true);
                }
            }
        });

        HBox hbox = new HBox(white, black);
        getChildren().add(hbox);

        final ToggleButton computer = new ToggleButton("Computer");
        computer.setSelected(true);
        computer.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(computer, Priority.ALWAYS);

        ToggleButton human = new ToggleButton("Human");
        human.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(human, Priority.ALWAYS);

        final ToggleGroup opponent = new ToggleGroup();
        opponent.getToggles().addAll(computer, human);
        opponent.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override
            public void changed(ObservableValue<? extends Toggle> observableValue, Toggle toggle, Toggle toggle2) {
                if (toggle2 == null && toggle != null) {
                    toggle.setSelected(true);
                }
            }
        });

        HBox hbox2 = new HBox(computer, human);
        getChildren().add(hbox2);

        Button start = new Button("Start");
        start.setMaxWidth(Double.MAX_VALUE);
        start.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                chessUI.startNewGame(black.isSelected() ? Color.B : Color.W, computer.isSelected());
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
    }
}
