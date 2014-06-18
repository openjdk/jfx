/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package shapet3dtest;

import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;

public class TextT3D extends Application {

    @Override public void start(Stage stage) {
        Scene scene = new Scene(new Group(), 1200.0f, 300.0f);
        scene.setCamera(new PerspectiveCamera());
        Text text = new Text(50.0F, 150.0F, "This is a test LONGLONGLONG     LONG");
        text.setFont(new Font(50.0F));
        text.setRotationAxis(Rotate.Y_AXIS);
        text.setOnMouseClicked(e -> System.out.println("Mouse Clicked:" + e));
        text.setOnMouseEntered(e -> System.out.println("Mouse Entered"));
        text.setOnMouseExited(e -> System.out.println("Mouse Exited"));
        ((Group)scene.getRoot()).getChildren().addAll(text);
        stage.setScene(scene);
        stage.sizeToScene();
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            System.out.println("*************************************************************");
            System.out.println("*    WARNING: common conditional SCENE3D isn't supported    *");
            System.out.println("*************************************************************");
        }
        stage.show();

        final RotateTransition tx = new RotateTransition(Duration.seconds(20), text);
        tx.setToAngle(360);
        tx.setCycleCount(RotateTransition.INDEFINITE);
        tx.setInterpolator(Interpolator.LINEAR);

        PauseTransition ptx = new PauseTransition(Duration.seconds(1));
        SequentialTransition stx = new SequentialTransition(ptx, tx);
        stx.play();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
