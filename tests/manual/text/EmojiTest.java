/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class EmojiTest extends Application {

   static String instructions =
      """
        This tests rendering of Emoji glyphs which is only supported on macOS.
        On macOS you should see a yellow-coloured smiling face image,
        embedded between 'ab' and 'cd'.
        On other platforms it may be a missing glyph, or an empty space, or
        a similar rendering as a greyscale/B&W glyph.
        Principally, you are checking that the emoji is rendered on macOS in
        each of the controls and nodes displayed in the test, and that the
        editable text field handles selection of the emoji glyph with the
        same background as other glyphs - this presumes the emoji image has
        transparent background pixels.
        There are 3 different ways it is displayed to verify
        1) Text node. 2) Label control, 3) TextField Control
        Press the Pass or Fail button as appropriate and the test will exit.
        If what you see is not explained here, ask before filing a bug.


        """;

    public static void main(String[] args) {
        launch(args);
    }

    private void quit() {
        Platform.exit();
    }

    @Override
    public void start(Stage stage) {
        Button passButton = new Button("Pass");
        Button failButton = new Button("Fail");
        passButton.setOnAction(e -> this.quit());
        failButton.setOnAction(e -> {
            this.quit();
            throw new AssertionError("The Emoji was not rendered on macOS");
        });

        HBox hbox = new HBox(10, passButton, failButton);

        Text instTA = new Text(instructions);
        instTA.setWrappingWidth(500);

        Font font = new Font(32);
        String emojiString = "ab\ud83d\ude00cd";
        Text text = new Text(emojiString);
        text.setFont(font);
        Label label = new Label(emojiString);
        label.setFont(font);
        TextField textField = new TextField(emojiString);
        textField.setFont(font);

        VBox vbox = new VBox();
        Scene scene = new Scene(vbox);
        vbox.getChildren().add(instTA);
        vbox.getChildren().add(hbox);
        vbox.getChildren().add(text);
        vbox.getChildren().add(label);
        vbox.getChildren().add(textField);
        stage.setWidth(600);
        stage.setHeight(600);
        stage.setScene(scene);

        stage.show();
    }

}
