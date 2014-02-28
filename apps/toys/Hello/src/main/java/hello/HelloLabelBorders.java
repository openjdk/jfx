/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;


/**
 * Different stroke and image borders shown in a ListView.
 * See hello.css for styles pertaining to .hello-label-borders
 */
public class HelloLabelBorders  extends Application {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(HelloLabelBorders.class, args);
    }

    private static class LabelListCell extends ListCell<Data> {

        LabelListCell() {
            super();
            super.setTooltip(tooltip);
        }

        @Override
        protected void updateItem(Data item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                tooltip.setText(null);
            } else {
                super.setText(item.getText());
                super.setStyle(item.getStyle());
                tooltip.setText(item.getStyle());
            }
        }

        private final Tooltip tooltip = new Tooltip();
    }

    // This is the data type on which the ListView operates.
    private static class Data {

        Data(String text, String style) {
            this.text = text;
            this.style = style;
        }

        String getText() {
            return text;
        }

        String getStyle() {
            return style;
        }

        private final String text;
        private final String style;
    }

    // ListView operates on an ObservableList. The newline characters in the styles are
    // there just to make the tooltip wrap where it is most logic.  
    private static ObservableList<Data> data = FXCollections.observableArrayList(
            new Data(
                    "no-border",
                    "-fx-border-color: null;"
            ),
            new Data(
                    "border-color",
                    "-fx-border-color: green blue cyan red;"
            ),
            new Data(
                    "border-inset",
                    "-fx-border-color: red blue green cyan;\n" +
                            "-fx-border-radius: 5;\n" +
                            "-fx-border-insets: 5;"
            ),
            new Data(
                    "border-style-dashed",
                    "-fx-border-style: dashed;\n" +
                            "-fx-border-insets: 0, -3;\n" +
                            "-fx-border-radius: 5;"
            ),
            new Data(
                    "border-style-dotted",
                    "-fx-border-color: red blue green cyan;\n" +
                            "-fx-border-style: dotted;\n" +
                            "-fx-border-radius: 5;"
            ),
            new Data(
                    "border-width",
                    "-fx-border-width: 1 2 1 2;\n" +
                            "-fx-border-color: red;"
            ),
            new Data(
                    "border-width-dashed",
                    "-fx-border-width: 1 3 5 1;\n" +
                            "-fx-border-color: red blue green cyan;\n" +
                            "-fx-border-style: dashed;"
            ),
            new Data(
                    "border-width-dotted",
                    "-fx-border-width: 1 3 5 1;\n" +
                            "-fx-border-color: red blue green cyan;\n" +
                            "-fx-border-style: dotted;"
            ),
            new Data(
                    "image-border",
                    "-fx-border-image-source: url('/hello/border.png');\n" +
                            "-fx-border-image-slice: 28;\n" +
                            "-fx-border-image-width: 9;"
            ),
            new Data(
                    "image-border-insets",
                    "-fx-border-image-source: url('/hello/heart_16.png');\n" +
                            "-fx-border-image-width: 10;\n" +
                            "-fx-border-image-insets: 1 5 10 15;"
            ),
            new Data(
                    "image-border-no-repeat",
                    "-fx-border-image-source: url('/hello/border.png');\n" +
                            "-fx-border-image-repeat: no-repeat;\n" +
                            "-fx-border-image-slice: 28;\n" +
                            "-fx-border-image-width: 9;"
            ),
            new Data(
                    "image-border-repeat-x",
                    "-fx-border-image-source: url('/hello/border.png');\n" +
                            "-fx-border-image-repeat: repeat-x;\n" +
                            "-fx-border-image-slice: 28;\n" +
                            "-fx-border-image-width: 9;"
            ),
            new Data(
                    "image-border-repeat-y",
                    "-fx-border-image-source: url('/hello/border.png');\n" +
                            "-fx-border-image-repeat: repeat-y;\n" +
                            "-fx-border-image-slice: 28;\n" +
                            "-fx-border-image-width: 9;"
            ),
            new Data(
                    "image-border-round",
                    "-fx-border-image-source: url('/hello/border.png');\n" +
                            "-fx-border-image-repeat: round;\n" +
                            "-fx-border-image-slice: 28;\n" +
                            "-fx-border-image-width: 9;"
            ),
            new Data(
                    "image-border-space",
                    "-fx-border-image-source: url('/hello/border.png');\n" +
                            "-fx-border-image-repeat: space;\n" +
                            "-fx-border-image-slice: 28;\n" +
                            "-fx-border-image-width: 9;"
            )
    );

    @Override
    public void start(Stage primaryStage) {

        ListView<Data> listView = new ListView<>(data);

        // A CSS style-class. See hello.css for its use.
        listView.getStyleClass().add("hello-label-borders");

        // We want the ListCells in our ListView to be LabelListCells.
        listView.setCellFactory(param -> new LabelListCell());

        Scene scene = new Scene(listView);
        scene.getStylesheets().add("/hello/hello.css");

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
