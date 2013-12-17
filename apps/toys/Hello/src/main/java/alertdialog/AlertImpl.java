/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package alertdialog;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class AlertImpl {

    private static enum AlertResult { YES, NO, OK, CANCEL }

    private static enum AlertStyle  { DEFAULT_OPTION, YES_NO_OPTION, OK_CANCEL_OPTION };

    private AlertResult result = null;
    private Stage stage;

    public static boolean question(Window owner, String title, String message) {
        return show(owner, title, message, AlertStyle.YES_NO_OPTION) == AlertResult.YES;
    }

    public static void inform(Window owner, String title, String message) {
        show(owner, title, message, AlertStyle.DEFAULT_OPTION);
    }

    public static boolean confirm(Window owner, String title, String message) {
        return show(owner, title, message, AlertStyle.OK_CANCEL_OPTION) == AlertResult.OK;
    }

    private static AlertResult show(Window owner, String title, String message, AlertStyle style) {
        AlertImpl control = new AlertImpl(owner, title, message, style);

        // Show dialog and wait for it to be closed, then return the result
        control.showAndWait();
        return control.result;
    }

    private AlertImpl(Window owner, String title, String message, AlertStyle style) {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle(title);

        StackPane alertRoot = new StackPane();
        Scene alertScene = new Scene(alertRoot, 250, 150);
        alertRoot.getChildren().add(create(style, message));

        this.stage.setScene(alertScene);

    }

    private void showAndWait() {
        // Show dialog and wait for it to close.
        stage.showAndWait();
    }

    private Node create(AlertStyle style, String message) {
        final Button okButton     = new Button("OK");
        final Button cancelButton = new Button("Cancel");
        final Button yesButton    = new Button("Yes");
        final Button noButton     = new Button("No");

        okButton.setOnAction(new EventHandler() {
            public void handle(Event event) {
                result = AlertResult.OK;
                closeWindow();
            }
        });
        cancelButton.setOnAction(new EventHandler() {
            public void handle(Event event) {
                result = AlertResult.CANCEL;
                closeWindow();
            }
        });
        yesButton.setOnAction(new EventHandler() {
            public void handle(Event event) {
                result = AlertResult.YES;
                closeWindow();
            }
        });
        noButton.setOnAction(new EventHandler() {
            public void handle(Event event) {
                result = AlertResult.NO;
                closeWindow();
            }
        });

        Button[] buttons = null;

        if (style == AlertStyle.OK_CANCEL_OPTION) {
            buttons = new Button[] { okButton, cancelButton };
        } else if (style == AlertStyle.YES_NO_OPTION) {
            buttons = new Button[] { yesButton, noButton };
        } else if (style == AlertStyle.DEFAULT_OPTION) {
            buttons = new Button[] { okButton };
        }

        VBox alertContent = new VBox();
        alertContent.setSpacing(10);
        alertContent.setAlignment(Pos.CENTER);

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setFont(new Font(16));

        HBox buttonBox = new HBox();
        buttonBox.setSpacing(10);
        buttonBox.getChildren().addAll(buttons);
        buttonBox.setAlignment(Pos.CENTER);

        alertContent.getChildren().add(messageLabel);
        alertContent.getChildren().add(buttonBox);

        return alertContent;
    }

    private void closeWindow() {
        stage.hide();
    }

}
