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
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class HelloScrollBar extends Application {

    /**
     * TODO: once we get unit tests working for controls, this
     * should become a unit test.
     */
    static void test() {
        ScrollBar sb;

        sb = new ScrollBar();
        sb.setFocusTraversable(true);
        sb.setMin(7000);
        if (sb.getMin() != 7000) {
            System.err.println("Could not set min to 7000");
        }

        sb = new ScrollBar();
        sb.setFocusTraversable(true);
        sb.setMax(-10);
        if (sb.getMax() != -10) {
            System.err.println("Could not set max to -10");
        }

        sb = new ScrollBar();
        sb.setFocusTraversable(true);
        sb.setMin(100);
        if (sb.getMin() != 100) {
            System.err.println("Could not set min to 100");
        }

        sb = new ScrollBar();
        sb.setFocusTraversable(true);
        sb.setMax(0);
        if (sb.getMax() != 0) {
            System.err.println("Could not set max to 0");
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {
        test();
        stage.setTitle("Hello ScrollBar");
        Scene scene = new Scene(new Group(), 600, 450);
        scene.setFill(Color.CHOCOLATE);

        Group root = (Group)scene.getRoot();
        
        ScrollBar sbarV1 = new ScrollBar();
        sbarV1.setFocusTraversable(true);
        sbarV1.setMax(100);
        sbarV1.setOrientation(Orientation.VERTICAL);
        sbarV1.setLayoutX(25);
        sbarV1.setLayoutY(40);
        sbarV1.setVisible(true);
        root.getChildren().add(sbarV1);

        ScrollBar sbarV2 = new ScrollBar();
        sbarV2.setFocusTraversable(true);
        sbarV2.setMax(100);
        sbarV2.setPrefSize(25, 200);
        sbarV2.setOrientation(Orientation.VERTICAL);
        sbarV2.setLayoutX(55);
        sbarV2.setLayoutY(40);
        sbarV2.setVisible(true);
        root.getChildren().add(sbarV2);

        ScrollBar sbarV3 = new ScrollBar();
        sbarV3.setFocusTraversable(true);
        sbarV3.setMax(100);
        sbarV3.setPrefSize(50, 300);
        sbarV3.setOrientation(Orientation.VERTICAL);
        sbarV3.setLayoutX(100);
        sbarV3.setLayoutY(40);
        sbarV3.setVisible(true);
        root.getChildren().add(sbarV3);

        ScrollBar sbarH1 = new ScrollBar();
        sbarH1.setFocusTraversable(true);
        sbarH1.setMax(100);
        sbarH1.setOrientation(Orientation.HORIZONTAL);
        sbarH1.setLayoutX(200);
        sbarH1.setLayoutY(40);
        sbarH1.setVisible(true);
        root.getChildren().add(sbarH1);

        ScrollBar sbarH2 = new ScrollBar();
        sbarH2.setFocusTraversable(true);
        sbarH2.setMax(100);
        sbarH2.setPrefSize(200, 25);
        sbarH2.setOrientation(Orientation.HORIZONTAL);
        sbarH2.setLayoutX(200);
        sbarH2.setLayoutY(80);
        sbarH2.setVisible(true);
        root.getChildren().add(sbarH2);

        ScrollBar sbarH3 = new ScrollBar();
        sbarH3.setFocusTraversable(true);
        sbarH3.setMax(100);
        sbarH3.setPrefSize(300, 50);
        sbarH3.setOrientation(Orientation.HORIZONTAL);
        sbarH3.setLayoutX(200);
        sbarH3.setLayoutY(140);
        sbarH3.setVisible(true);
        root.getChildren().add(sbarH3);

        stage.setScene(scene);
        stage.show();
    }
}
