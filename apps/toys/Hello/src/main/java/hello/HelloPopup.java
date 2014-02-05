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

import java.util.Iterator;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;

public class HelloPopup extends Application {
    private static double WIDTH = 300;
    private static double HEIGHT = 300;
    @Override public void start(Stage stage) {
        stage.setTitle("Hello Popup");
        stage.setWidth(WIDTH);
        stage.setHeight(HEIGHT);

        stage.setScene(createScene(new PopupPlacement(0, 500)));
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

    private static Group createRootGroup(PopupPlacement popupPlacement) {
        Group rootGroup = new Group();
        ObservableList<Node> content = rootGroup.getChildren();

        Rectangle rect = new Rectangle();
        rect.setX(0);
        rect.setY(0);
        rect.setWidth(WIDTH);
        rect.setHeight(HEIGHT);
        rect.setFill(Color.GRAY);
        rect.setStroke(Color.RED);
        rect.setStrokeWidth(2);
        content.add(rect);

        final Button showButton = new Button("Popup");
        showButton.setLayoutX(50);
        showButton.setLayoutY(50);
        showButton.setOnAction(new PopupActionHandler(popupPlacement,
                                                      showButton));
        content.add(showButton);

        final TextField textbox = new TextField();
//        textbox.setColumns(18);
        textbox.setLayoutX(50);
        textbox.setLayoutY(100);
        content.add(textbox);

        return rootGroup;
    }

    private static Scene createScene(PopupPlacement popupPlacement) {
        Scene scene = new Scene(createRootGroup(popupPlacement));
        return scene;
    }

    private static Popup createPopup(PopupPlacement popupPlacement) {
        Popup popup = new Popup();

        popup.getContent().add(createRootGroup(popupPlacement));
        popup.setAutoHide(true);

        return popup;
    }

    private static final class PopupActionHandler
            implements EventHandler<ActionEvent> {
        private final Node popupParent;
        private final PopupPlacement popupPlacement;
        private final int popupX;
        private final int popupY;

        private Popup nextPopup;

        public PopupActionHandler(PopupPlacement popupPlacement, 
                                  Node popupParent) {
            this.popupPlacement = popupPlacement;
            this.popupParent = popupParent;
            this.popupX = popupPlacement.getNextX();
            this.popupY = popupPlacement.getNextY();
        }

        public void handle(final ActionEvent t) {
            if (nextPopup == null) {
                nextPopup = createPopup(popupPlacement);
            }

            nextPopup.show(popupParent, popupX, popupY);
            
            Iterator<Window> windows = Window.impl_getWindows();
            while (windows.hasNext()) {
                System.out.println("W: " + windows.next().getClass().getName());
            }
        }
    }

    private static final class PopupPlacement {
        private int x;
        private int y;

        public PopupPlacement(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getNextX() {
            int oldX = x;
            x += WIDTH;
            return oldX;
        }

        public int getNextY() {
            return y;
        }
    }
}
