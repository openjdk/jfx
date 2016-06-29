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

package hellotest;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import com.sun.glass.events.KeyEvent;
import com.sun.glass.ui.Robot;

public class RemoveFocusedControl extends Application {

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) {

        final Group root = new Group();
        stage.setTitle("Remove Focused Control Test");
        Scene scene = new Scene(root, 600, 450);

        scene.setOnKeyTyped((javafx.scene.input.KeyEvent e) -> {
            System.out.println("Event: " + e);
        });

        Button button1 = new Button();
        button1.setText("Click HERE for removing this button and then sending "
                + "four key events");
        button1.setLayoutX(25);
        button1.setLayoutY(40);

        final Robot robot = com.sun.glass.ui.Application.GetApplication().
                createRobot();

        button1.setOnAction((ActionEvent e) -> {
            root.getChildren().remove(button1);
            robot.keyPress(KeyEvent.VK_T);
            robot.keyRelease(KeyEvent.VK_T);
            robot.keyPress(KeyEvent.VK_E);
            robot.keyRelease(KeyEvent.VK_E);
            robot.keyPress(KeyEvent.VK_S);
            robot.keyRelease(KeyEvent.VK_S);
            robot.keyPress(KeyEvent.VK_T);
            robot.keyRelease(KeyEvent.VK_T);
        });

        root.getChildren().add(button1);

        stage.setScene(scene);
        stage.show();
        System.out.println("Please press on 'Click HERE...' button in order to remove\n"
                + " the button and generate automatically four keyEvents.\n"
                + "Expected results: Button is disappearing, exactly FOUR KeyEvents\n"
                + " are being accepted and printed to the console.");
    }
}
