/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

package touchsuite;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.TouchEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class ListSelectionSimple extends Application {

    Scene scene;
    @Override public void start(Stage stage) {
        stage.setTitle("List Selection Simple");

        Group root = new Group();
        scene = new Scene(root, 200, 500);

        root.getChildren().add(new List("one", "two", "three", "four", "five",
                "six", "seven", "eight", "nine", "ten"));

        stage.setScene(scene);
        stage.show();
    }


    private static class List extends VBox {
        private Item[] items;
        private Item selectionStart;

        public List(String... captions) {
            final int len = captions.length;
            Item[] items = new Item[len];
            for (int i = 0; i < len; i++) {
                items[i] = new Item(captions[i], this);
            }
            this.items = items;
            getChildren().addAll(items);
        }

        public void select(Item item) {
            for (Item i : items) {
                i.setSelected(i == item);
            }
            selectionStart = item;
        }

        public void selectTo(Item item) {
            boolean in = false;
            for (Item i : items) {
                if (in) {
                    i.setSelected(in);
                    if (i == item || i == selectionStart) {
                        in = false;
                    }
                } else {
                    if (i == item || i == selectionStart) {
                        in = true;
                    }
                    i.setSelected(in);
                }
            }
        }
    }

    private static class Item extends Group {
        private Text text;
        private Rectangle bg;
        private List list;

        public Item(final String caption, final List list) {
            this.list = list;

            bg = new Rectangle(200, 50, Color.LIGHTGRAY);

            text = new Text(caption);
            text.setFont(new Font(20));
            text.setTextAlignment(TextAlignment.LEFT);
            text.setTextOrigin(VPos.CENTER);
            text.setFill(Color.BLACK);
            text.setTranslateX(20);
            text.setTranslateY(25);

            getChildren().addAll(bg, text);

            setOnTouchPressed(new EventHandler<TouchEvent>() {
                @Override public void handle(TouchEvent event) {

                    if (event.getTouchCount() == 1) {
                        Item.this.list.select(Item.this);
                    } else if (event.getTouchCount() == 2) {
                        if (event.getTouchPoint() == event.getTouchPoints().get(1)) {
                            Item.this.list.selectTo(Item.this);
                        }
                    }
                    event.consume();
                }
            });
        }

        public void setSelected(boolean selected) {
            if (selected) {
                bg.setFill(Color.DARKBLUE);
                text.setFill(Color.WHITE);
            } else {
                bg.setFill(Color.LIGHTGRAY);
                text.setFill(Color.BLACK);
            }
        }
    }

    public static String info() {
        return
                "This application demonstrates simple usage of touch "
                + "events for selecting items in a list. Press an item with "
                + "one finger, it will be selected. Hold the finger down "
                + "and press another finger, items between the two fingers "
                + "will be selected.";
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
