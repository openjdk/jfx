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
package a11y;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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
        text.impl_selectionFillProperty().set(Color.BLUE);
        text.setFont(Font.font(50));


        TextField tf = new TextField("Hello Accessiblity");
        TextField tf2 = new TextField("james");
        tf2.setEditable(false);
        TextArea ta = new TextArea("TextArea can many lines.\nLine1.\nLine2 is longer.\nLine 3 is not.");

        Scene scene = new Scene(new VBox(text, tf, tf2, ta), 300, 300);
//        scene.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
//            @Override
//            public void handle(KeyEvent event) {
//                int start = text.getImpl_selectionStart();
//                int end = text.getImpl_selectionEnd();
//                if (start == -1) start = 0;
//                if (end == -1) end = text.getText().length();
//                switch (event.getCode()) {
//                case LEFT: start--; break;
//                case RIGHT: start++; break;
//                case UP: end--; break;
//                case DOWN: end++; break;
//                default:
//                }
//                text.setImpl_selectionStart(start);
//                text.setImpl_selectionEnd(end);
////                System.out.println(start + " " + end);
//            }
//        });
        stage.setScene(scene);
        stage.show();
    }


}

