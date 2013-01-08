/*
 * Copyright (c) 2008, 2012 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package modena;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.GroupBuilder;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.Pagination;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabBuilder;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPaneBuilder;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineBuilder;
import javafx.scene.shape.RectangleBuilder;
import javafx.util.Callback;

/**
 * Helper static methods for Sample Page
 */
public class SamplePageHelpers {
    
    private static final String[] LETTERS = new String[]{"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
    
    static ObservableList<String> sampleItems() {
        return sampleItems(7);
    }
    
    static ObservableList<String> sampleItems(int numberOfItems) {
        ArrayList<String> items = new ArrayList<String>();
        if (numberOfItems < 26) {
            for(int i=0; i<numberOfItems; i++) {
                items.add("Item "+LETTERS[i]);
            }
        } else {
            for(int i=0; i<numberOfItems; i++) {
                items.add("Item "+i);
            }
        }
        return FXCollections.observableArrayList(items);
    }
    
    static Node scrollPaneContent() {
        return GroupBuilder.create().children(
                RectangleBuilder.create().width(200).height(200).fill(Color.PALETURQUOISE).build(),
                LineBuilder.create().endX(200).endY(200).stroke(Color.DODGERBLUE).build(),
                LineBuilder.create().startX(200).endX(0).endY(200).stroke(Color.DODGERBLUE).build()
            ).build();
    }
    
    static TabPane createTabPane(int numOfTabs, int prefWidth, String firstTabText, boolean floating) {
        TabPane tabPane = new TabPane();
        if (floating) tabPane.getStyleClass().add("floating");
        for (int i=1; i<=numOfTabs; i++) {
            tabPane.getTabs().add(
                TabBuilder.create()
                    .text("Tab "+i)
                    .content(new Label((i==1 && firstTabText!=null)? firstTabText :"Tab "+i+" Content"))
                    .build()
            );
        }
        tabPane.setPrefWidth(prefWidth);
        tabPane.setPrefHeight(100);
        return tabPane;
    }
    
    static ToolBar createToolBar(boolean vertical, boolean overFlow) {
        ToolBar toolBar = new ToolBar();
        if (vertical) toolBar.setOrientation(Orientation.VERTICAL);
        toolBar.getItems().addAll(
                new Button("A"),
                new Button("B"),
                new Separator()
        );
        if (vertical) {
            toolBar.getItems().addAll(
                new Button("C"),
                new Button("D")
            );
        } else {
            toolBar.getItems().addAll(
                new Label("Search:"),
                new TextField()
            );
        }
        if (overFlow) {
            if (vertical) {
                toolBar.setPrefHeight(80);
            } else {
                toolBar.setPrefWidth(80);
            }
        }
        return toolBar;
    }
    
    static Accordion createAccordion() {
        Accordion accordian = new Accordion();
        accordian.getPanes().addAll(
            TitledPaneBuilder.create().text("Title 1").content(new Label("Content\nLine2.")).build(),
            TitledPaneBuilder.create().text("Title 2").content(new Label("Content\nLine2.")).build(),
            TitledPaneBuilder.create().text("Title 3").content(new Label("Content\nLine2.")).build()
        );
        return accordian;
    }
    
    static SplitPane createSplitPane(int numOfItems, boolean vertical, Node firstItem) {
        SplitPane splitPane = new SplitPane();
        if(vertical) splitPane.setOrientation(Orientation.VERTICAL);
        if (firstItem != null) splitPane.getItems().add(firstItem);
        for (int i=1; i<=numOfItems; i++) {
            splitPane.getItems().add(new Label("Item "+i));
        }
        splitPane.setPrefSize(150, 150);
        return splitPane;
    }
    
    static Pagination createPagination(int numOfPages, boolean bullet, boolean arrows) {
        Pagination pagination = new Pagination(numOfPages);
        if (bullet) pagination.getStyleClass().add("bullet");
        if (!arrows) pagination.setStyle("-fx-arrows-visible:false;");
        pagination.setPageFactory(new Callback<Integer, Node>() {
            @Override public Node call(Integer param) {
                return new Label("Page Label "+param);
            }
        });
        return pagination;
    }
    
    static ListView<String> createListView(int numOfItems, boolean multipleSelection, boolean disable) {
        ListView<String> listView = new ListView<String>();
        listView.setPrefHeight((24*7)+4);
        listView.setPrefWidth(140);
        listView.getItems().addAll(sampleItems(numOfItems));
        listView.setDisable(disable);
        if (multipleSelection) {
            listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            listView.getSelectionModel().selectRange(1, 5);
        } else {
            listView.getSelectionModel().select(1);
        }
        return listView;
    }
    
    static MenuItem[] createMenuItems(int numberOfItems) {
        ArrayList<MenuItem> items = new ArrayList<MenuItem>();
        if (numberOfItems < 26) {
            for(int i=0; i<numberOfItems; i++) {
                items.add(new MenuItem("Item "+LETTERS[i]));
            }
        } else {
            for(int i=0; i<numberOfItems; i++) {
                items.add(new MenuItem("Item "+i));
            }
        }
        return items.toArray(new MenuItem[items.size()]);
    }
    
    static Object[] createMenuContents() {
        List menuItems = new ArrayList();
//        Menu menu11 = makeMenu("_New", new ImageView(new Image(getClass().getResourceAsStream("about_16.png"))));
        final Menu menu11 = new Menu("_New", new ImageView(new Image("helloworld/about_16.png")));
        MenuItem menu12 = new MenuItem("_Open", new ImageView(new Image("helloworld/folder_16.png")));
        menu12.setAccelerator(new KeyCharacterCombination("]", 
                KeyCombination.SHIFT_DOWN, KeyCombination.META_DOWN));
        Menu menu13 = new Menu("_Submenu");
        CheckMenuItem showMessagesItem = new CheckMenuItem("Enable onShowing/onHiding _messages", 
                                             new ImageView(new Image("helloworld/about_16.png")));
        MenuItem menu15 = new MenuItem("E_xit");
        final String change[] = {"Change Text", "Change Back"};
        final MenuItem menu16 = new MenuItem(change[0]);
        final boolean toggle = false;
        menu16.setAccelerator(KeyCombination.keyCombination("Shortcut+C"));
        menuItems.add(menu11);
        menuItems.add(menu12);
        menuItems.add(menu13);
        menuItems.add(showMessagesItem);
        menuItems.add(new SeparatorMenuItem());
        menuItems.add(menu15);
        menuItems.add(menu16);

        // --- Menu 11 submenu
        final MenuItem menu111 = new MenuItem("blah");
        final MenuItem menu112 = new MenuItem("foo");
        final CheckMenuItem menu113 = new CheckMenuItem("Show \"foo\" item");
        menu113.setSelected(true);
        menu113.selectedProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                menu112.setVisible(menu113.isSelected());
                System.err.println("MenuItem \"foo\" is now " + (menu112.isVisible() ? "" : "not") + " visible.");
            }
        });
        menu11.getItems().addAll(menu111, menu112, menu113);

        // --- Menu 13 submenu
        MenuItem menu131 = new MenuItem("Item _1");
        MenuItem menu132 = new MenuItem("Item _2");
        menu13.getItems().addAll(menu131, menu132);
        
        return menuItems.toArray();
    }
    
    static final Image recorder48 = new Image(SamplePageHelpers.class.getResource("recorder-icon-48.png").toExternalForm());
    
    static ImageView createGraphic() {
        return new ImageView(recorder48);
    }
}
