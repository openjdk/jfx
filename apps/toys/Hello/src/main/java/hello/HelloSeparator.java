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


import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

public class HelloSeparator extends Application {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {
        stage.setTitle("Hello Separator");

        Scene scene = new Scene(new Group(), 200, 600);

        int offsetX = 0;
        int offsetY = 0;
        final List<Separator> separators = new ArrayList<Separator>();
        for(int i = 0; i < 10; i++) {
            Separator s = new Separator();
            separators.add(s);

            s.setLayoutX(25 + offsetX);
            s.setLayoutY(40 + offsetY);

            offsetY += 45;
            offsetX += 5;
            
            ((Group)scene.getRoot()).getChildren().add(s);
        }

        ToggleButton toggle = new ToggleButton("Horizontal Slider");
        toggle.setSelected(true);
        toggle.selectedProperty().addListener(ov -> {
            for (Separator s : separators) {
                s.setOrientation(s.getOrientation() == Orientation.VERTICAL ?
                        Orientation.HORIZONTAL : Orientation.VERTICAL);
            }
        });
        ((Group)scene.getRoot()).getChildren().add(toggle);

        stage.setScene(scene);
        stage.show();
    }
}
