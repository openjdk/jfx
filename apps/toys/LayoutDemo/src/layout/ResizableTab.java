/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package layout;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class ResizableTab extends Tab {

    public ResizableTab(String text) {
        this.setText(text);
        init();
    }

    public void init() {
        Button button = new Button("Button");
        button.setStyle("-fx-font-size: 40px");
        Label label = new Label("Label");
        label.setStyle("-fx-font-size: 36px");
        VBox vbox = new VBox(50, button, label);
        vbox.setAlignment(Pos.CENTER_RIGHT);
        Rectangle rect = new Rectangle(600, 400, Color.BURLYWOOD);
        rect.setStrokeWidth(3);
        rect.setStroke(Color.RED);
        Text text = new Text("Rectangle");
        text.setStyle("-fx-font-size: 40px");

        StackPane rectGroup = new StackPane(rect, text);

        HBox root = new HBox();
        root.setSpacing(20);
        root.getChildren().addAll(vbox, rectGroup);

        HBox.setHgrow(rectGroup, Priority.ALWAYS);
        root.getStyleClass().add("layout");
        this.setContent(root);
    }
}
