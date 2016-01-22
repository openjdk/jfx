/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates.
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

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Pagination;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

/**
 * Helper static methods for Sample Page
 */
public class SamplePageHelpers {

    static <T extends Node> T withState(T node, String state) {
        if (node != null && state != null) {
            // stop user from being able to change state
            node.setMouseTransparent(true);
            node.setFocusTraversable(false);
            // set state to chosen state
            final String[] pseudoClasses = (state).split("[\\s,]+");
            for (String pseudoClass : pseudoClasses) {
                node.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClass), true);
            }
        }
        return node;
    }

    static <T extends Node> T withState(final T node, final String state, final String subNodeStyleClass, final String subNodeState) {
        withState(node, state);
        Platform.runLater(() -> withState(node.lookup(subNodeStyleClass), subNodeState));
        return node;
    }

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
        Line l1 = new Line();
        l1.setEndX(200);
        l1.setEndY(200);
        l1.setStroke(Color.DODGERBLUE);
        Line l2 = new Line();
        l2.setStartX(200);
        l2.setStartY(0);
        l2.setEndY(200);
        l2.setStroke(Color.DODGERBLUE);
        return new Group(new Rectangle(200, 200, Color.PALETURQUOISE), l1, l2);
    }

    static Node createTabPane(int numOfTabs, int prefWidth, int prefHeight, String firstTabText, boolean floating, boolean disableFirst, Side side) {
        TabPane tabPane = new TabPane();
        tabPane.setSide(side);
        if (floating) tabPane.getStyleClass().add("floating");
        for (int i=1; i<=numOfTabs; i++) {
            Tab tab = new Tab("Tab "+i);
            tab.setDisable(i==1 && disableFirst);
            tab.setContent(new Label((i==1 && firstTabText!=null)? firstTabText :"Tab "+i+" Content"));
            tabPane.getTabs().add(tab);
        }
        if (disableFirst) tabPane.getSelectionModel().select(1);
        tabPane.setPrefWidth(prefWidth);
        tabPane.setPrefHeight(prefHeight);
        return tabPane;
    }

    static Node wrapBdr(Node node) {
        StackPane sp = new StackPane(node);
        sp.setStyle("-fx-border-color: black; -fx-border-width: 3;");
        return sp;
    }

    static ToolBar createToolBar(Side side, boolean overFlow, boolean disabled) {
        final boolean vertical = side == Side.LEFT || side == Side.RIGHT;
        ToolBar toolBar = new ToolBar();
        if (vertical) toolBar.setOrientation(Orientation.VERTICAL);
        if (side == Side.BOTTOM) toolBar.getStyleClass().add("bottom");
        if (side == Side.RIGHT) toolBar.getStyleClass().add("right");
        if (disabled) toolBar.setDisable(true);
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
            Label searchLabel = new Label("Search:");
            HBox searchBox = new HBox(10, searchLabel, new TextField());
            searchBox.setAlignment(Pos.BASELINE_LEFT);
            toolBar.getItems().addAll(searchBox);
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
                new TitledPane("Title 1", new Label("Content\nLine2.")),
                new TitledPane("Title 2", new Label("Content\nLine2.")),
                new TitledPane("Title 3", new Label("Content\nLine2."))
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
        pagination.setPageFactory(param -> new Label("Page Label "+param));
        return pagination;
    }

    static ListView<String> createListView(int numOfItems, boolean multipleSelection, boolean disable, boolean horiz) {
        ListView<String> listView = new ListView<String>();
        if (horiz) listView.setOrientation(Orientation.HORIZONTAL);
        listView.setPrefHeight((24*7)+4);
        listView.setPrefWidth(horiz ? 200 : 140);
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

    static MenuBar createMenuBar() {
        final MenuBar mb = new MenuBar();
        mb.getMenus().addAll(
            createMenu("File"),
            createMenu("Edit"),
            createMenu("View"),
            createMenu("Help")
        );
        Platform.runLater(() -> new ArrayList<Node>(mb.lookupAll(".menu")).get(1).pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true));
        return  mb;
    }

    static Menu createMenu(String name) {
        Menu m = new Menu(name);
        m.getStyleClass().add(name);
        m.getItems().addAll(createMenuContents());
        return  m;
    }

    static Node createContextMenu() {
        Button b = new Button("ContextMenu Right Click Me");
        b.setContextMenu(new ContextMenu(createMenuContents()));
        return b;
    }

    static Node createInlineMenu(final boolean selectAll) {
        // create a context menu so we can put it inline in our test page
        final ContextMenu menu = new ContextMenu(createMenuContents());
        // create a place holder container
        final StackPane contextMenu = new StackPane();
        // show context menu then steal and place inline
        Platform.runLater(() -> {
            menu.show(contextMenu,-1000,-1000);
            menu.hide();
            Platform.runLater(() -> {
                final Node menuContent = menu.getSkin().getNode();
                contextMenu.getChildren().add(menuContent);
                menuContent.setMouseTransparent(true);
//                        System.out.println("menuContent = " + menuContent);
//                        System.out.println("menuContent.lookupAll(\".menu-item\") = " + menuContent.lookupAll(".menu-item"));

//                        Platform.runLater(new Runnable() {
//                            @Override public void run() {
////                        if (selectAll) {
////                            for (Node n: menuContent.lookupAll(".menu-item")) {
////                                n.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
////                            }
////                        } else {
//                            new ArrayList<Node>(menuContent.lookupAll(".menu-item")).get(2)
//                                    .pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
////                        }
//                            }
//                        });
            });
        });
        return contextMenu;
    }

    static MenuItem[] createMenuContents() {
        List<MenuItem> menuItems = new ArrayList<>();
        final Menu menu11 = new Menu("_New");
        MenuItem menu12 = new MenuItem("_Open");
        menu12.getStyleClass().add("OpenMenuItem");
        menu12.setAccelerator(new KeyCharacterCombination("]",
                KeyCombination.SHIFT_DOWN, KeyCombination.META_DOWN));
        Menu menu13 = new Menu("_Submenu");
        MenuItem menu15 = new MenuItem("E_xit");
        final String change[] = {"Change Text", "Change Back"};
        final MenuItem menu16 = new MenuItem(change[0]);
        menu16.setAccelerator(KeyCombination.keyCombination("Shortcut+C"));
        menuItems.add(menu11);
        menuItems.add(menu12);
        menuItems.add(menu13);
        menuItems.add(menu16);
        menuItems.add(new SeparatorMenuItem());

        menuItems.add(new CheckMenuItem("Check"));
        CheckMenuItem checkMenuItem = new CheckMenuItem("Check Selected");
        checkMenuItem.setSelected(true);
        menuItems.add(checkMenuItem);
        menuItems.add(new SeparatorMenuItem());
        menuItems.add(new RadioMenuItem("Radio"));
        RadioMenuItem radioMenuItem = new RadioMenuItem("Radio Selected");
        radioMenuItem.setSelected(true);
        menuItems.add(radioMenuItem);
        menuItems.add(new SeparatorMenuItem());
        menuItems.add(menu15);

        // --- Menu 11 submenu
        final MenuItem menu111 = new MenuItem("blah");
        final MenuItem menu112 = new MenuItem("foo");
        final CheckMenuItem menu113 = new CheckMenuItem("Show \"foo\" item");
        menu113.setSelected(true);
        menu113.selectedProperty().addListener(valueModel -> {
            menu112.setVisible(menu113.isSelected());
            System.err.println("MenuItem \"foo\" is now " + (menu112.isVisible() ? "" : "not") + " visible.");
        });
        menu11.getItems().addAll(menu111, menu112, menu113);

        // --- Menu 13 submenu
        MenuItem menu131 = new MenuItem("Item _1");
        MenuItem menu132 = new MenuItem("Item _2");
        menu13.getItems().addAll(menu131, menu132);

        return menuItems.toArray(new MenuItem[menuItems.size()]);
    }

    static final Image recorder48 = new Image(SamplePageHelpers.class.getResource("recorder-icon-48.png").toExternalForm());

    static ImageView createGraphic() {
        return new ImageView(recorder48);
    }

    static Node createGreyButton(double percentageGrey) {
        int grey = (int)(percentageGrey*255);
        int percentage = (int)(percentageGrey * 100);
        StackPane sp = new StackPane();
        sp.setStyle("-fx-base: rgba("+grey+","+grey+","+grey+",1); -fx-background-color: -fx-background;");
        sp.setPadding(new Insets(8));
        sp.getChildren().add(new Button(percentage+"%"));
        return sp;
    }
}
