/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates.
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
package modena;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Color;

/**
 * Simple page with two windows with mac and window styling
 */
public class SimpleWindowPage extends StackPane {
    private Node macWindowContent;
    private Node windows7WindowContent;
    private Node windows8WindowContent;
    private Node ubuntuWindowContent;

    public SimpleWindowPage() {
        setPadding(new Insets(20));
        TilePane box = new TilePane(10,10);
        box.setPrefColumns(2);
        getChildren().add(box);
        box.setPadding(new Insets(15));
        box.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        try {
            StackPane macWindow = new StackPane();
            macWindow.getStyleClass().add("macWindow");
            macWindowContent = (Node)FXMLLoader.load(SimpleWindowPage.class.getResource("simple-window.fxml"));
            macWindowContent.lookup("#MenuBar").setVisible(false);
            macWindowContent.lookup("#MenuBar").setManaged(false);
            macWindow.getChildren().add(macWindowContent);

            StackPane windows7Window = new StackPane();
            windows7Window.getStyleClass().add("windows7Window");
            windows7WindowContent = (Node)FXMLLoader.load(SimpleWindowPage.class.getResource("simple-window.fxml"));
            windows7Window.getChildren().add(windows7WindowContent);

            StackPane windows8Window = new StackPane();
            windows8Window.getStyleClass().add("windows8Window");
            windows8WindowContent = (Node)FXMLLoader.load(SimpleWindowPage.class.getResource("simple-window.fxml"));
            windows8Window.getChildren().add(windows8WindowContent);

            StackPane ubuntuWindow = new StackPane();
            ubuntuWindow.getStyleClass().add("ubuntuWindow");
            ubuntuWindowContent = (Node)FXMLLoader.load(SimpleWindowPage.class.getResource("simple-window.fxml"));
            ubuntuWindow.getChildren().add(ubuntuWindowContent);

            Platform.runLater(() -> {
                final Node macRB1 = macWindowContent.lookup("#RadioButton1");
                macRB1.setMouseTransparent(true);
                final Node macRB2 = macWindowContent.lookup("#RadioButton2");
                macRB2.setMouseTransparent(true);
                macRB2.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true);

                final Node windows7RB1 = windows7WindowContent.lookup("#RadioButton1");
                windows7RB1.setMouseTransparent(true);
                final Node windows7RB2 = windows7WindowContent.lookup("#RadioButton2");
                windows7RB2.setMouseTransparent(true);
                windows7RB2.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true);

                final Node windows8RB1 = windows8WindowContent.lookup("#RadioButton1");
                windows8RB1.setMouseTransparent(true);
                final Node windows8RB2 = windows8WindowContent.lookup("#RadioButton2");
                windows8RB2.setMouseTransparent(true);
                windows8RB2.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true);

                final Node ubuntuRB1 = ubuntuWindowContent.lookup("#RadioButton1");
                ubuntuRB1.setMouseTransparent(true);
                final Node ubuntuRB2 = ubuntuWindowContent.lookup("#RadioButton2");
                ubuntuRB2.setMouseTransparent(true);
                ubuntuRB2.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true);
            });

            box.getChildren().addAll(macWindow, ubuntuWindow, windows7Window, windows8Window);
        } catch (IOException ex) {
            Logger.getLogger(SimpleWindowPage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setModena(boolean modena) {
        if (modena) {
            macWindowContent.setStyle("-fx-background-color: -fx-background;");
            windows7WindowContent.setStyle("-fx-background-color: -fx-background;");
            windows8WindowContent.setStyle("-fx-background-color: -fx-background;");
            ubuntuWindowContent.setStyle("-fx-background-color: -fx-background;");
        } else {
            macWindowContent.setStyle(null);
            windows7WindowContent.setStyle(null);
            windows8WindowContent.setStyle(null);
            ubuntuWindowContent.setStyle(null);
        }
    }
}

