/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.input.RotateEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.shape.*;

public class HelloGestures extends Application {

public static void main(String[] args) {
    launch(args);
}

@Override
public void start(Stage primaryStage) {

    primaryStage.setX(0);
    primaryStage.setY(0);

    double startPointX = 300;
    double startPointY = 300;
    Circle circle = new Circle(400, 300, 200);
    circle.setStroke(Color.BLACK);
    circle.setFill(Color.WHITE);

    final Path path = new Path();
    path.setPickOnBounds(true);

    MoveTo moveTo = new MoveTo();
    moveTo.setX(startPointX);
    moveTo.setY(startPointY);

    LineTo LineTo1 = new LineTo();
    LineTo1.setX(startPointX + 100);
    LineTo1.setY(startPointY - 150);

    LineTo LineTo2 = new LineTo();
    LineTo2.setX(startPointX + 200);
    LineTo2.setY(startPointY);

    LineTo LineTo3 = new LineTo();
    LineTo3.setX(startPointX + 150);
    LineTo3.setY(startPointY);

    LineTo LineTo4 = new LineTo();
    LineTo4.setX(startPointX + 150);
    LineTo4.setY(startPointY + 150);

    LineTo LineTo5 = new LineTo();
    LineTo5.setX(startPointX + 50);
    LineTo5.setY(startPointY + 150);

    LineTo LineTo6 = new LineTo();
    LineTo6.setX(startPointX + 50);
    LineTo6.setY(startPointY);

    LineTo LineTo7 = new LineTo();
    LineTo7.setX(startPointX);
    LineTo7.setY(startPointY);

    path.getElements().add(moveTo);
    path.getElements().add(LineTo1);
    path.getElements().add(LineTo2);
    path.getElements().add(LineTo3);
    path.getElements().add(LineTo4);
    path.getElements().add(LineTo5);
    path.getElements().add(LineTo6);
    path.getElements().add(LineTo7);

    path.setOnRotate(event -> path.setRotate(path.getRotate() + event.getAngle()));

    path.setOnScroll(event -> {
        path.setTranslateX(path.getTranslateX() + event.getDeltaX());
        path.setTranslateY(path.getTranslateY() + event.getDeltaY());
    });

    path.setOnZoom(event -> {
        path.setScaleX(path.getScaleX() * event.getZoomFactor());
        path.setScaleY(path.getScaleY() * event.getZoomFactor());
    });

    Button btnRotateR = new Button("Send Rotate Right");
    btnRotateR.setOnMousePressed(event -> path.fireEvent(new RotateEvent(RotateEvent.ROTATE, 350, 300, 350, 300,
            false, false, false, false, false, false, 30, 30, null)));

    Button btnRotateL = new Button("Send Rotate Left");
    btnRotateL.setOnMousePressed(event -> path.fireEvent(new RotateEvent(RotateEvent.ROTATE, 350, 300, 350, 300,
            false, false, false, false, false, false, -30, -30, null)));

    Button btnScrollU = new Button("Send Scroll Up");
    btnScrollU.setOnMousePressed(event -> path.fireEvent(new ScrollEvent(ScrollEvent.SCROLL,
            350, 300, 350, 300, false, false, false, false, false, false,
            0, -25, 0, -25,
            ScrollEvent.HorizontalTextScrollUnits.NONE, 1,
            ScrollEvent.VerticalTextScrollUnits.NONE, 1, 1, null)));

    Button btnScrollD = new Button("Send Scroll Down");
    btnScrollD.setOnMousePressed(event -> path.fireEvent(new ScrollEvent(ScrollEvent.SCROLL,
            350, 300, 350, 300, false, false, false, false, false, false,
            0, 25, 0, 25,
            ScrollEvent.HorizontalTextScrollUnits.NONE, 1,
            ScrollEvent.VerticalTextScrollUnits.NONE, 1, 1, null)));

    Button btnZoomIn = new Button("Send Zoom In");
    btnZoomIn.setOnMousePressed(event -> path.fireEvent(new ZoomEvent(ZoomEvent.ZOOM, 350, 300, 350, 300,
            false, false, false, false, false, false, 1.1, 1.1, null)));

    Button btnZoomOut = new Button("Send Zoom Out");
    btnZoomOut.setOnMousePressed(event -> path.fireEvent(new ZoomEvent(ZoomEvent.ZOOM, 350, 300, 350, 300,
            false, false, false, false, false, false, 0.9, 0.9, null)));

    VBox vb = new VBox(15);
    vb.getChildren().addAll(btnRotateR, btnRotateL, btnScrollU, btnScrollD,
            btnZoomIn, btnZoomOut);

    vb.setLayoutX(10);
    vb.setLayoutY(10);
    Label t360 = new Label("360");
    t360.setLayoutX(385);
    t360.setLayoutY(75);
    Label t90 = new Label("90");
    t90.setLayoutX(610);
    t90.setLayoutY(290);
    Label t180 = new Label("180");
    t180.setLayoutX(387);
    t180.setLayoutY(510);
    Label t270 = new Label("270");
    t270.setLayoutX(165);
    t270.setLayoutY(290);
    Pane mainPane = new Pane(circle, new Circle(400, 100, 3),
                             new Circle(200, 300, 3), new Circle(600, 300, 3),
                             new Circle(400, 500, 3), path, vb, t360, t90, t180,
                                                                           t270);

    Scene scene = new Scene(mainPane, Screen.getPrimary().getVisualBounds().getWidth(),
            Screen.getPrimary().getVisualBounds().getHeight());
    primaryStage.setScene(scene);
    primaryStage.show();
}

}