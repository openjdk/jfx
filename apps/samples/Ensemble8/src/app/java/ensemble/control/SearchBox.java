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
package ensemble.control;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

/**
 * Search field with styling and a clear button
 */
public class SearchBox extends TextField implements ChangeListener<String>{
    private final Button clearButton = new Button();
    private final Region innerBackground = new Region();
    private final Region icon = new Region();

    public SearchBox() {
        getStyleClass().addAll("search-box");
        icon.getStyleClass().setAll("search-box-icon");
        innerBackground.getStyleClass().setAll("search-box-inner");
        setPromptText("Search");
        textProperty().addListener(this);
        setPrefHeight(30);
        clearButton.getStyleClass().setAll("search-clear-button");
        clearButton.setCursor(Cursor.DEFAULT);
        clearButton.setOnMouseClicked((MouseEvent t) -> {
            setText("");
        });
        clearButton.setVisible(false);
        clearButton.setManaged(false);
        innerBackground.setManaged(false);
        icon.setManaged(false);
    }

    @Override protected void layoutChildren() {
        super.layoutChildren();
        if (clearButton.getParent() != this) getChildren().add(clearButton);
        if (innerBackground.getParent() != this) getChildren().add(0,innerBackground);
        if (icon.getParent() != this) getChildren().add(icon);
        innerBackground.setLayoutX(0);
        innerBackground.setLayoutY(0);
        innerBackground.resize(getWidth(), getHeight());
        icon.setLayoutX(0);
        icon.setLayoutY(0);
        icon.resize(35,30);
        clearButton.setLayoutX(getWidth()-30);
        clearButton.setLayoutY(0);
        clearButton.resize(30, 30);
    }

    @Override public void changed(ObservableValue<? extends String> ov, String oldValue, String newValue) {
        clearButton.setVisible(newValue.length() > 0);
    }
}
