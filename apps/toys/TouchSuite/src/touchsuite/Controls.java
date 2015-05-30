/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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


package touchsuite;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class Controls extends Application {

    @Override public void start(Stage stage) {
        stage.setTitle("Controls");

        Group root = new Group();
        Scene scene = new Scene(root, 600, 600);
        scene.setFill(Color.LIGHTGREEN);

        VBox sbs = new VBox();
        sbs.setLayoutX(10);
        sbs.setLayoutY(10);

        ScrollBar vert = new ScrollBar();
        vert.setOrientation(Orientation.VERTICAL);
        vert.setMin(0);
        vert.setMax(100);
        vert.setVisibleAmount(10);
        vert.setPrefHeight(500);
        vert.setPrefWidth(40);
        vert.setBlockIncrement(10);
        vert.setUnitIncrement(1);

        HBox lst = new HBox(10);

        ScrollBar horz = new ScrollBar();
        horz.setOrientation(Orientation.HORIZONTAL);
        horz.setMin(0);
        horz.setMax(100);
        horz.setVisibleAmount(10);
        horz.setPrefHeight(40);
        horz.setPrefWidth(500);
        horz.setBlockIncrement(10);
        horz.setUnitIncrement(1);

        ListView list = new ListView();
        list.setItems(FXCollections.observableArrayList(
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "Tenth line",
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "Twentieth line",
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "Thirtieth line",
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "Fourtieth line",
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "Fiftieth line",
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "Sixtieth line",
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "Seventieth line",
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "Eightieth line",
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "Ninetieth line",
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "Hunderth line"
                ));
        list.setTranslateX(10);
        list.setTranslateY(10);

//        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {
//            @Override public void handle(MouseEvent event) {
//                event.consume();
//            }
//        });
//        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
//            @Override public void handle(MouseEvent event) {
//                event.consume();
//            }
//        });

        VBox rects = new VBox();
        rects.getChildren().addAll(
                new Rectangle(100, 100, Color.RED),
                new Rectangle(100, 100, Color.YELLOW),
                new Rectangle(100, 100, Color.GREEN),
                new Rectangle(100, 100, Color.BLUE),
                new Rectangle(100, 100, Color.ORANGE),
                new Rectangle(100, 100, Color.GRAY),
                new Rectangle(100, 100, Color.GREEN),
                new Rectangle(100, 100, Color.BLUE),
                new Rectangle(100, 100, Color.ALICEBLUE),
                new Rectangle(100, 100, Color.BROWN),
                new Rectangle(100, 100, Color.GREEN),
                new Rectangle(100, 100, Color.BLUE),
                new Rectangle(100, 100, Color.CYAN),
                new Rectangle(100, 100, Color.YELLOW),
                new Rectangle(100, 100, Color.DARKGOLDENROD),
                new Rectangle(100, 100, Color.BLUE),
                new Rectangle(100, 100, Color.AZURE),
                new Rectangle(100, 100, Color.YELLOW),
                new Rectangle(100, 100, Color.AQUA),
                new Rectangle(100, 100, Color.BLUE)
                );


        ScrollPane sp = new ScrollPane();
        sp.setPrefHeight(450);
        sp.setPrefWidth(200);
        sp.setContent(rects);
        sp.setTranslateX(10);
        sp.setTranslateY(10);
        sp.setPannable(true);

        sbs.getChildren().addAll(horz, lst);
        lst.getChildren().addAll(vert, list, sp);

        root.getChildren().addAll(sbs);
        stage.setScene(scene);
        stage.show();
    }

    public static String info() {
        return
                "This application contains several controls to demonstrate "
                + "their current behavior on touch screen.";
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
