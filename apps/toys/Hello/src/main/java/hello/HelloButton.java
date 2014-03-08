/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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


import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class HelloButton extends Application {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {
        stage.setTitle("Hello Button");
        Scene scene = new Scene(new Group(), 600, 450);
        Button button = new Button();
        button.setText("Click Me");
        button.setLayoutX(25);
        button.setLayoutY(40);

        button.setOnAction(e -> System.out.println("Event: " + e));

        button.addEventHandler(KeyEvent.KEY_RELEASED, e -> System.out.println("Event: " + e));

        ((Group)scene.getRoot()).getChildren().add(button);

        button = new Button();
        button.setText("Click Me Too");
        button.setLayoutX(25);
        button.setLayoutY(70);
        ((Group)scene.getRoot()).getChildren().add(button);

        button = new Button();
        button.setText("Click Me Three");
        button.setLayoutX(25);
        button.setLayoutY(100);
        ((Group)scene.getRoot()).getChildren().add(button);

        stage.setScene(scene);
        stage.show();
    }
}
