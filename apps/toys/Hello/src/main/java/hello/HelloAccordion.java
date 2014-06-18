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
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class HelloAccordion extends Application {

    @Override public void start(Stage stage) {

        TitledPane t1 = new TitledPane();
        t1.setId("Label 1");
        t1.setText("Label 1");
        t1.setContent(new Button("This is Button 1\n\nAnd there were a few empty lines just there!"));

        TitledPane t2 = new TitledPane();
        t2.setId("Label 2");
        t2.setText("Label 2");
        t2.setContent(new Label("This is Label 2\n\nAnd there were a few empty lines just there!"));

        TitledPane t3 = new TitledPane();
        t3.setId("Label 3");
        t3.setText("Label 3");
        t3.setContent(new Button("This is Button 3\n\nAnd there were a few empty lines just there!"));

        Accordion accordion = new Accordion();

        accordion.getPanes().add(t1);
        accordion.getPanes().add(t2);
        accordion.getPanes().add(t3);

        stage.setTitle("Accordion Sample");

        final VBox root = new VBox(20);
        root.setFillWidth(false);
        Scene scene = new Scene(root, 500, 500);
        root.getChildren().add(accordion);

        root.getChildren().add(new Button("This button changes it's layout when Accordion is used"));

        stage.setScene(scene);
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
