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
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class HelloSplitPane extends Application {

    private final Slider horizSlider = new Slider();
    private final SplitPane horizSplitPane = new SplitPane();
    
    private final Slider vertSlider = new Slider();
    private final SplitPane vertSplitPane = new SplitPane();
    private final SplitPane mailClient = new SplitPane();
    private final SplitPane mailClient2 = new SplitPane();
    private final SplitPane recursiveSplitPane = new SplitPane();

    @Override public void init() {
        horizSlider.setMin(0);
        horizSlider.setMax(1);
        horizSlider.setValue(0.5f);
        horizSlider.setPrefWidth(horizSplitPane.prefWidth(-1));
        horizSlider.valueProperty().addListener(ov -> horizSplitPane.setDividerPosition(0, horizSlider.getValue()));

        horizSplitPane.setPrefSize(200, 200);
        final Button l = new Button("Left Button");
        final Button r = new Button("Right Button");
        horizSplitPane.getItems().addAll(
                new VBox(l),
                new VBox(r));
        horizSplitPane.getDividers().get(0).positionProperty().addListener(ov -> horizSlider.setValue(horizSplitPane.getDividers().get(0).getPosition()));
        horizSplitPane.widthProperty().addListener(ov -> horizSlider.setPrefWidth(horizSplitPane.prefWidth(-1)));

        vertSlider.setOrientation(Orientation.VERTICAL);
        vertSlider.setMin(0);
        vertSlider.setMax(1);
        vertSlider.setValue(0.5f);
        vertSlider.setPrefHeight(vertSplitPane.prefHeight(-1));
        vertSlider.valueProperty().addListener(ov -> vertSplitPane.setDividerPosition(0, 1 - vertSlider.getValue()));

        vertSplitPane.setPrefSize(200, 200);
        vertSplitPane.setOrientation(Orientation.VERTICAL);
        final Button t = new Button("Top Button");
        final Button b = new Button("Bottom Button");
        vertSplitPane.getItems().addAll(
                new VBox(t),
                new VBox(b));
        vertSplitPane.getDividers().get(0).positionProperty().addListener(ov -> vertSlider.setValue(1 - vertSplitPane.getDividers().get(0).getPosition()));
        vertSplitPane.heightProperty().addListener(ov -> vertSlider.setPrefHeight(vertSplitPane.prefHeight(-1)));

        setupMailClient();
        setupMailClient2();
        setupRecursiveSplitPane();
    }

    private void setupMailClient() {
        mailClient.setPrefSize(200, 200);
        mailClient.setDividerPosition(0, 0.25f);
        mailClient.setId("main-spt-pane");
        Button lbtn = new Button("hello");
        lbtn.setId("leftbtn");
        mailClient.getItems().addAll(new VBox(lbtn));

        SplitPane rsp = new SplitPane();
        rsp.setId("right-spt-pane");
        rsp.setDividerPosition(0, 0.6f);
        rsp.setOrientation(Orientation.VERTICAL);
        Button tbtn = new Button("hello");
        tbtn.setId("right-topbtn");
        rsp.getItems().addAll(new VBox(tbtn));

        Button bbtn = new Button("hello");
        bbtn.setId("right-bottombtn");
        rsp.getItems().addAll(new VBox(bbtn));
        mailClient.getItems().addAll(rsp);
    }

    private void setupMailClient2() {
        mailClient2.setPrefSize(200, 200);
        mailClient2.setDividerPosition(0, 0.25f);
        mailClient2.setId("main-spt-pane");

        SplitPane lsp = new SplitPane();
        lsp.setOrientation(Orientation.HORIZONTAL);
        SplitPane sp1 = new SplitPane();
        sp1.setOrientation(Orientation.VERTICAL);
        SplitPane sp2 = new SplitPane();
        sp2.setOrientation(Orientation.VERTICAL);
        lsp.getItems().addAll(sp1, sp2);
        mailClient2.getItems().addAll(lsp);

        SplitPane rsp = new SplitPane();
        rsp.setId("right-spt-pane");
        rsp.setDividerPosition(0, 0.6f);
        rsp.setOrientation(Orientation.VERTICAL);
        Button tbtn = new Button("hello");
        tbtn.setId("right-topbtn");
        rsp.getItems().addAll(new VBox(tbtn));

        Button bbtn = new Button("hello");
        bbtn.setId("right-bottombtn");
        rsp.getItems().addAll(new VBox(bbtn));
        mailClient2.getItems().addAll(rsp);
    }

    private void setupRecursiveSplitPane() {
        recursiveSplitPane.setPrefSize(200, 200);
        recursiveSplitPane.setDividerPosition(0, 0.6f);
        recursiveSplitPane.setId("main-split-pane");

        Button lbtn = new Button("hello");
        lbtn.setId("left-btn");
        recursiveSplitPane.getItems().addAll(new VBox(lbtn));

        SplitPane sp = new SplitPane();
        sp.setId("right-split-pane");
        sp.setDividerPosition(0, 0.6f);
        sp.setOrientation(Orientation.VERTICAL);
        Button rbtn = new Button("hello");
        rbtn.setId("right-top-btn");
        sp.getItems().addAll(new VBox(rbtn));
        SplitPane sv2 = new SplitPane();
        sv2.setId("right-bottom-split-pane");
        sp.getItems().addAll(sv2);
        recursiveSplitPane.getItems().addAll(sp);
    }

    @Override public void start(Stage stage) {
        stage.setTitle("SplitPane");
        Scene scene = new Scene(new Group(), 1300, 400);
        scene.setFill(Color.GHOSTWHITE);

        VBox vbox = new VBox();
        vbox.setSpacing(4);
        vbox.getChildren().clear();
        vbox.getChildren().add(horizSplitPane);
        vbox.getChildren().add(horizSlider);

        VBox vbox2 = new VBox();
        vbox2.setSpacing(2);

        Button b1 = new Button("0.0");
        b1.setOnMouseClicked(me -> {
            horizSplitPane.setDividerPosition(0, 0);
            vertSplitPane.setDividerPosition(0, 0);
        });

        Button b2 = new Button("0.25");
        b2.setOnMouseClicked(me -> {
            horizSplitPane.setDividerPosition(0, 0.25f);
            vertSplitPane.setDividerPosition(0, 0.25f);
        });

        Button b3 = new Button("0.50");
        b3.setOnMouseClicked(me -> {
            horizSplitPane.setDividerPosition(0, 0.5f);
            vertSplitPane.setDividerPosition(0, 0.5f);
        });

        Button b4 = new Button("0.75");
        b4.setOnMouseClicked(me -> {
            horizSplitPane.setDividerPosition(0, 0.75f);
            vertSplitPane.setDividerPosition(0, 0.75f);
        });

        Button b5 = new Button("1.0");
        b5.setOnMouseClicked(me -> {
            horizSplitPane.setDividerPosition(0, 1.0f);
            vertSplitPane.setDividerPosition(0, 1.0f);
        });

        vbox2.getChildren().clear();
        vbox2.getChildren().add(b1);
        vbox2.getChildren().add(b2);
        vbox2.getChildren().add(b3);
        vbox2.getChildren().add(b4);
        vbox2.getChildren().add(b5);

        HBox hbox2 = new HBox();
        hbox2.setSpacing(4);
        hbox2.getChildren().clear();
        hbox2.getChildren().add(vertSplitPane);
        hbox2.getChildren().add(vertSlider);

        HBox hbox = new HBox();
        hbox.setTranslateX(20);
        hbox.setTranslateY(20);
        hbox.setSpacing(20);
        hbox.getChildren().clear();
        hbox.getChildren().add(vbox);
        hbox.getChildren().add(vbox2);
        hbox.getChildren().add(hbox2);
        hbox.getChildren().add(mailClient);
        hbox.getChildren().add(mailClient2);
        hbox.getChildren().add(recursiveSplitPane);

        Group root = (Group)scene.getRoot();

        root.getChildren().clear();
        root.getChildren().add(hbox);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
