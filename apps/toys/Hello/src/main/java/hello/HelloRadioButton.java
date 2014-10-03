/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

package hello;

import static javafx.scene.paint.Color.GHOSTWHITE;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

/**
 * Demo for {@code RadioButton}.
 */
public class HelloRadioButton extends Application {

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {
        stage.setTitle("Hello RadioButton");
        stage.setWidth(600);
        stage.setHeight(450);
        Scene scene = newScene();
        ToggleGroup group = new ToggleGroup();
        RadioButton button1 = new RadioButton("No, *I* am your father");
        button1.setToggleGroup(group);
        RadioButton button2 = new RadioButton("Nooooooooo!");
        button2.setLayoutY(40);
        button2.setToggleGroup(group);
        button1.setSelected(true);
        ObservableList<Node> content = ((Group)scene.getRoot()).getChildren();
        content.add(button1);
        content.add(button2);
        stage.setScene(scene);
        stage.show();
    }

    private static Scene newScene() {
        Scene scene = new Scene(new Group());
        scene.setFill(GHOSTWHITE);
        return scene;
    }
}
