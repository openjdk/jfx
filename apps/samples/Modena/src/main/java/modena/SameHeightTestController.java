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

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

/**
 * Controller to auto align guidelines
 */
public class SameHeightTestController implements Initializable{

    private @FXML Button horizFirstButton;
    private @FXML TextField vertFirstTextField;
    private @FXML Region horizBaseLine;
    private @FXML Region vertBaseLine;
    private @FXML Region arrowButtonLeftLine;
    private @FXML Region arrowButtonRightLine;
    private @FXML Region arrowLeftLine;
    private @FXML Region arrowRightLine;
    private @FXML ComboBox editableCombo;
    private @FXML AnchorPane arrowButtonContainer;
    private Node arrowButton;
    private Node arrow;

    @Override public void initialize(URL url, ResourceBundle rb) {
        Platform.runLater(() -> {
            Text buttonTextNode = (Text)horizFirstButton.lookup(".text");
            buttonTextNode.layoutYProperty().addListener((ov, t, t1) -> StackPane.setMargin(horizBaseLine, new Insets(t1.doubleValue(),0,0,0)));
            Text textFieldTextNode = (Text)vertFirstTextField.lookup(".text");
            textFieldTextNode.layoutXProperty().addListener((ov, t, t1) -> StackPane.setMargin(vertBaseLine, new Insets(0,0,0,t1.doubleValue())));
            arrowButton = editableCombo.lookup(".arrow-button");
            arrow = editableCombo.lookup(".arrow");
            ChangeListener updater = (ov, t, t1) -> updateArrowLinePositions();
            arrow.layoutBoundsProperty().addListener(updater);
            arrowButton.layoutBoundsProperty().addListener(updater);
            editableCombo.layoutBoundsProperty().addListener(updater);
            arrowButtonContainer.layoutBoundsProperty().addListener(updater);
            updateArrowLinePositions();
        });
    }

    private void updateArrowLinePositions() {
        double left = arrowButton.localToScene(0, 0).getX() - arrowButtonContainer.localToScene(0, 0).getX();
        arrowButtonLeftLine.setLayoutX(left-1);
        arrowButtonLeftLine.setPrefHeight(arrowButtonContainer.getLayoutBounds().getHeight());
        arrowButtonRightLine.setLayoutX(left + arrowButton.getLayoutBounds().getWidth());
        arrowButtonRightLine.setPrefHeight(arrowButtonContainer.getLayoutBounds().getHeight());
        double arrowLeft = arrow.localToScene(0, 0).getX() - arrowButtonContainer.localToScene(0, 0).getX();
        arrowLeftLine.setLayoutX(arrowLeft-1);
        arrowLeftLine.setPrefHeight(arrowButtonContainer.getLayoutBounds().getHeight());
        arrowRightLine.setLayoutX(arrowLeft + arrow.getLayoutBounds().getWidth());
        arrowRightLine.setPrefHeight(arrowButtonContainer.getLayoutBounds().getHeight());
    }
}
