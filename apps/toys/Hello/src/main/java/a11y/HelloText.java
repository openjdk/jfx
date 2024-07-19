/*
 * Copyright (c) 2014, 2024, Oracle and/or its affiliates. All rights reserved.
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
package a11y;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class HelloText extends Application {


    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {

        final Text text = new Text("01234");
        text.selectionFillProperty().set(Color.BLUE);
        text.setFont(Font.font(50));

        Label l1 = new Label("name");
        TextField tf1 = new TextField("Hello JavaFX Accessiblity");
        l1.setLabelFor(tf1);
        HBox box1 = new HBox(10, l1, tf1);

        Label l2 = new Label("family");
        TextField tf2 = new TextField("james");
        tf2.setEditable(false);
        l2.setLabelFor(tf2);
        HBox box2 = new HBox(10, l2, tf2);

        TextArea ta = new TextArea("TextArea can many lines.\nLine1.\nLine2 is longer very long very long and can wrap. This is sentence belongs to the paragraph.\nLine 3 is not.");
        ta.setWrapText(true);

        Scene scene = new Scene(new VBox(text, box1, box2, ta), 300, 300);
//        scene.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
//            @Override
//            public void handle(KeyEvent event) {
//                int start = text.getSelectionStart();
//                int end = text.getSelectionEnd();
//                if (start == -1) start = 0;
//                if (end == -1) end = text.getText().length();
//                switch (event.getCode()) {
//                case LEFT: start--; break;
//                case RIGHT: start++; break;
//                case UP: end--; break;
//                case DOWN: end++; break;
//                default:
//                }
//                text.setSelectionStart(start);
//                text.setSelectionEnd(end);
//--                System.out.println(start + " " + end);
//            }
//        });
        stage.setScene(scene);
        stage.show();
    }


}

