/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class HelloFonts extends Application {
    /**
     * @param args command line args
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

    public static String sampleText = "ABCDEFGabcdefg";
    public static float fontSize = 14;
    public static class FontData {
        String name; /*either font name or family*/
        FontWeight weight;
        FontPosture posture;
        boolean nameIsFamily;
        public FontData(String fontName) {
            name = fontName;
        }
        public FontData(String family, FontWeight weight, FontPosture posture) {
            name = family;
            this.posture = posture;
            this.weight = weight;
            nameIsFamily = true;
        }
        public String getFontName() {return getFont().toString();}
        public String getName() {return name;}
        public String getWeight() {return weight.name();}
        public String getPosture() {return posture.name();}
        private Font font;
        public Font getFont() {
            if (font == null) {
                if (nameIsFamily) {
                    font = Font.font(name, weight, posture, fontSize);
                } else {
                    font = new Font(name, fontSize);
                }
            }
            return font;
        }
    }

    public Node createAllFontsTab() {
        List<String> fonts = Font.getFontNames();
        List<FontData> data = new ArrayList<FontData>();
        for (String font : fonts) {
            data.add(new FontData(font));
        }
        TableView<FontData> table = new TableView<FontData>();
        TableColumn<FontData, String> column0 = new TableColumn<FontData, String>("Requested name");
        column0.setCellValueFactory(
                new PropertyValueFactory<FontData,String>("name")
        );
        TableColumn<FontData, String> column1 = new TableColumn<FontData, String>("Font name");
        column1.setCellValueFactory(
                new PropertyValueFactory<FontData,String>("fontName")
        );
        TableColumn<FontData, Font> column2 = new TableColumn<FontData, Font>("sample");
        column2.setCellValueFactory(
                new PropertyValueFactory<FontData,Font>("font")
        );
        column2.setCellFactory(param -> new TableCell<FontData, Font>() {
            @Override
            protected void updateItem(Font item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                } else {
                    setText(sampleText);
                    setFont(item);
                }
            }
        });
        table.setItems(FXCollections.observableList(data));
        table.getColumns().addAll(column0, column2, column1);
        return table;
    }

    public Node createFontsByStyleTab() {
        List<String> families = Font.getFamilies();
        List<FontData> data = new ArrayList<FontData>();
        for (String family : families) {
            data.add(new FontData(family, FontWeight.NORMAL, FontPosture.REGULAR));
            data.add(new FontData(family, FontWeight.BOLD, FontPosture.REGULAR));
            data.add(new FontData(family, FontWeight.NORMAL, FontPosture.ITALIC));
            data.add(new FontData(family, FontWeight.BOLD, FontPosture.ITALIC));
        }
        TableView<FontData> table = new TableView<FontData>();
        TableColumn<FontData, String> column0 = new TableColumn<FontData, String>("Family");
        column0.setCellValueFactory(
                new PropertyValueFactory<FontData,String>("name")
        );
        TableColumn<FontData, String> column1 = new TableColumn<FontData, String>("Weight");
        column1.setCellValueFactory(
                new PropertyValueFactory<FontData,String>("weight")
        );
        TableColumn<FontData, String> column2 = new TableColumn<FontData, String>("Posture");
        column2.setCellValueFactory(
                new PropertyValueFactory<FontData,String>("posture")
        );
        TableColumn<FontData, String> column3 = new TableColumn<FontData, String>("Font");
        column3.setCellValueFactory(
                new PropertyValueFactory<FontData,String>("font")
        );
        TableColumn<FontData, Font> column4 = new TableColumn<FontData, Font>("Sample");
        column4.setCellValueFactory(
                new PropertyValueFactory<FontData,Font>("font")
        );
        column4.setCellFactory(param -> new TableCell<FontData, Font>() {
            @Override
            protected void updateItem(Font item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                } else {
                    setText(sampleText);
                    setFont(item);
                }
            }
        });

        table.setItems(FXCollections.observableList(data));
        table.getColumns().addAll(column0, column1, column2, column4, column3);
        return table;
    }

    public Node createFontFamiliesTab() {
        TreeItem<String> root = new TreeItem<String>();
        TreeView<String> tree = new TreeView<String>(root);
        tree.setShowRoot(false);
        List<String> families = Font.getFamilies();
        for (String family : families) {
            TreeItem<String> item = new TreeItem<String>(family);
            root.getChildren().add(item);
            List<String> fonts = Font.getFontNames(family);
            for (String font : fonts) {
                TreeItem<String> subItem = new TreeItem<String>(font);
                item.getChildren().add(subItem);
            }
        }
        return tree;
    }

    @Override
    public void start(Stage stage) throws Exception {
        TabPane tabPane = new TabPane();
        Tab tab = new Tab();
        tab.setText("All fonts");
        tab.setContent(createAllFontsTab());
        tab.setClosable(false);
        tabPane.getTabs().add(tab);

        tab = new Tab();
        tab.setText("Font families");
        tab.setContent(createFontFamiliesTab());
        tab.setClosable(false);
        tabPane.getTabs().add(tab);

        tab = new Tab();
        tab.setText("Fonts by style");
        tab.setContent(createFontsByStyleTab());
        tab.setClosable(false);
        tabPane.getTabs().add(tab);

        Scene scene = new Scene(tabPane, 800, 400);
        stage.setTitle("Font loading testcase");
        stage.setScene(scene);
        stage.show();
    }
}
