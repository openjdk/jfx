/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ButtonMnemonicPositionTest extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        String str =
            "This test is to check mnemonic position is correctly shown " +
            "in a single and multi-line button.\n" +
            "Test shows 8 buttons of varying widths stacked in 2 groups of 4 buttons each - " +
            "The buttons are made up of a string consisting of A to Z English" +
            " characters\n"+
            "------------------------------------------------------\n"+
            "Action : Press and hold 'Alt' key on the keyboard.\n" +
            "Verify that : There is a mnemonic mark (line) drawn below " +
            "letter U in all the buttons.\n" +
            "If yes, press 'Test Passed' button else press 'Test Failed'";
        Label testInstructions = new Label(str);
        Button pass = new Button("Test Passed");
        Button fail = new Button("Test Failed");

        pass.setOnAction((e)->{
            Platform.exit();
        });

        fail.setOnAction((e)->{
            Platform.exit();
            throw new AssertionError("Mnemonic mark (line) is " +
                    "not drawn as expected.");
        });

        HBox testBtnBox = new HBox();
        testBtnBox.setSpacing(20.0);
        testBtnBox.getChildren().addAll(pass, fail);

        Label lbl =
            new Label("------------------------------------------------------");


        // LTR Test buttons ---------------------------------------
        Button b1 = new Button("ABCDEFGHIJKLMNOPQRST_UVWXYZ");
        Button b2 = new Button("ABCDEFGHIJKLMNOPQRST_UVWXYZ");
        Button b3 = new Button("ABCDEFGHIJKLMNOPQRST_UVWXYZ");
        Button b4 = new Button("ABCDEFGHIJKLMNOPQRST_UVWXYZ");

        b2.setMaxWidth(130);
        b2.wrapTextProperty().setValue(true);

        b3.setMaxWidth(85);
        b3.wrapTextProperty().setValue(true);

        b4.setMaxWidth(60);
        b4.wrapTextProperty().setValue(true);

        VBox LTRBox = new VBox();
        LTRBox.setSpacing(10.0);
        LTRBox.getChildren().addAll(b1, b2, b3, b4);

        // RTL Test buttons ---------------------------------------
        Button b5 = new Button("ABCDEFGHIJKLMNOPQRST_UVWXYZ");
        Button b6 = new Button("ABCDEFGHIJKLMNOPQRST_UVWXYZ");
        Button b7 = new Button("ABCDEFGHIJKLMNOPQRST_UVWXYZ");
        Button b8 = new Button("ABCDEFGHIJKLMNOPQRST_UVWXYZ");

        b5.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        b6.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        b6.setMaxWidth(130);
        b6.wrapTextProperty().setValue(true);

        b7.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        b7.setMaxWidth(85);
        b7.wrapTextProperty().setValue(true);

        b8.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        b8.setMaxWidth(60);
        b8.wrapTextProperty().setValue(true);

        VBox RTLBox = new VBox();
        RTLBox.setSpacing(10.0);
        RTLBox.getChildren().addAll(b5, b6, b7, b8);

        HBox btnBox = new HBox();
        btnBox.getChildren().addAll(LTRBox, RTLBox);

        VBox box = new VBox();
        box.setSpacing(10.0);

        Scene s = new Scene(box);
        box.getChildren().addAll(testInstructions, testBtnBox, lbl,
                                 btnBox);
        primaryStage.setScene(s);
        primaryStage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
