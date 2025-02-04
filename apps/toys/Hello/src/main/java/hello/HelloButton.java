/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;

import javafx.stage.Stage;

public class HelloButton extends Application {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {
        stage.setTitle("Hello Button");
        Scene scene = new Scene(new Group(), 600, 450);
        Button button1 = new Button();
        button1.setText("Click Me");
        button1.setLayoutX(25);
        button1.setLayoutY(40);

        button1.setOnAction(e -> System.out.println("Event: " + e));

//        // add new mappings
//        // The J key should fire the button
//        button1.getInputMap().getMappings().add(new KeyMapping(J, e -> button1.fire()));
//
//        // The SPACE key should no longer fire the button (and instead just print text to console)
//        button1.getInputMap().getMappings().add(new KeyMapping(SPACE, e -> {
//            System.out.println("This should replace the default action event!");
//        }));
//
//        // The degenerate case where we accept all input - a null KeyCombination matches anything
//        // and results in firing the button
//        button1.getInputMap().getMappings().add(new KeyMapping(new KeyBinding(KEY_PRESSED), e -> {
//            System.out.println("catch-all mapping caught event: " + e);
//            button1.fire();
//        }));
//
//
//        // test one: lookup the J mapping and disable it
//        button1.getInputMap().lookupMapping(new KeyBinding(J)).ifPresent(mapping -> {
//            System.out.println("disabling J key mapping");
//            mapping.setDisabled(true);
//        });
//
//        // test two: the degenerate case - we need to iterate through the mappings
//        // to find the null keyCombination, so that we may install an interceptor
//        // to block the tab key. This means all keys except tab will work.
//--        button1.getInputMap().lookupMapping(new KeyBinding(KEY_TYPED)).ifPresent(mapping -> {
//--            System.out.println("adding interceptor for Tab key mapping");
//--            mapping.getInterceptors().add(event -> {
//--                if (! (event instanceof KeyEvent)) return true;
//--                return KeyCode.TAB != ((KeyEvent)event).getCode();
//--            });
//--        });
//--        button1.getInputMap().getInterceptors().add(event -> {
//--            if (! (event instanceof KeyEvent)) return true;
//--            return KeyCode.X != ((KeyEvent)event).getCode();
//--        });
//
//        button1.getInputMap().getInterceptors().add(new KeyMappingInterceptor(new KeyBinding(SPACE)));
//
//        // test three: remove a mapping
//        button1.getInputMap().lookupMapping(new KeyBinding(J)).ifPresent(mapping -> {
//            System.out.println("removing J key mapping");
//            button1.getInputMap().getMappings().remove(mapping);
//        });
//
//        scene.getRoot().addEventHandler(KeyEvent.ANY, e -> System.out.println("e = " + e));


        ((Group)scene.getRoot()).getChildren().add(button1);

        Button button2 = new Button();
        button2.setText("Click Me Too");
        button2.setLayoutX(25);
        button2.layoutYProperty().bind(button1.heightProperty().add(button1.layoutYProperty()));
        ((Group)scene.getRoot()).getChildren().add(button2);
//
//        Button button3 = new Button();
//        button3.setText("Click Me Three");
//        button3.setLayoutX(25);
//        button3.layoutYProperty().bind(button2.heightProperty().add(button2.layoutYProperty()));
//        ((Group)scene.getRoot()).getChildren().add(button3);

        stage.setScene(scene);
        stage.show();
    }
}
