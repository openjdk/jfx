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

import java.util.Arrays;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

/**
 * A simple tool bar that has left and right justified items with a title in the
 * center if there is enough space for it.
 */
public class TitledToolBar extends HBox {
    private String defaultTitle = "JavaFX Ensemble";
    private Label titleLabel = new Label(defaultTitle);

    private StringProperty titleText = new SimpleStringProperty(null);
    public StringProperty titleTextProperty() { return titleText; };
    public String getTitleText() { return titleText.get(); }
    public void setTitleText(String text) { titleText.set(text);}

    public TitledToolBar() {
        getStyleClass().addAll("tool-bar","ensmeble-tool-bar");
        titleLabel.getStyleClass().add("title");
        titleLabel.setManaged(false);
        titleLabel.textProperty().bind(titleText);
        getChildren().add(titleLabel);
        Pane spacer = new Pane();
        setHgrow(spacer, Priority.ALWAYS);
        getChildren().add(spacer);
    }

    public void addLeftItems(Node ... items) {
        getChildren().addAll(0, Arrays.asList(items));
    }

    public void addRightItems(Node ... items) {
        getChildren().addAll(items);
    }

    @Override protected void layoutChildren() {
        super.layoutChildren();
        final double w = getWidth();
        final double h = getHeight();
        final double titleWidth = titleLabel.prefWidth(h);
        double leftItemsWidth = getPadding().getLeft();
        for(Node item: getChildren()) {
            if (item == titleLabel) break;
            leftItemsWidth += item.getLayoutBounds().getWidth();
            Insets margins = getMargin(item);
            if (margins != null) leftItemsWidth += margins.getLeft() + margins.getRight();
            leftItemsWidth += getSpacing();
        }
        if ((leftItemsWidth+(titleWidth/2)) < (w/2)) {
            titleLabel.setVisible(true);
            layoutInArea(titleLabel, 0, 0, getWidth(), h, 0, HPos.CENTER, VPos.CENTER);
        } else {
            titleLabel.setVisible(false);
        }
    }
}
