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

import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class HelloTabPane extends Application {

    private TabPane tabPane;
    private Tab tab1;
    private Tab tab2;
    private Tab tab3;
    private Tab emptyTab;
    private Tab internalTab;
    private Tab multipleTabs;
    private ContextMenu menu;

    private boolean showScrollArrows = false;
    private boolean showTabMenu = false;

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {
        tabPane = new TabPane();
        tab1 = new Tab();
        tab2 = new Tab();
        tab3 = new Tab();
        emptyTab = new Tab();
        internalTab = new Tab();
        multipleTabs = new Tab();
        setUpPopupMenu();
        stage.setTitle("Hello TabPane2");
        final Scene scene = new Scene(new Group(), 800, 800);
        scene.setFill(Color.GHOSTWHITE);

        
        tabPane.prefWidthProperty().bind(scene.widthProperty());
        tabPane.prefHeightProperty().bind(scene.heightProperty());
        
        tabPane.setRotateGraphic(false);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);
        tabPane.setSide(Side.TOP);        

        {
            tab1.setText("Tab 1");
            tab1.setTooltip(new Tooltip("Tab 1 Tooltip"));
            final Image image = new Image(getClass().getResourceAsStream("about_16.png"));
            final ImageView imageView = new ImageView();
            imageView.setImage(image);
            tab1.setGraphic(imageView);
            final VBox vbox = new VBox();
            vbox.setSpacing(10);
            vbox.setTranslateX(10);
            vbox.setTranslateY(10);
            {
                final Button b = new Button("Toggle Tab Mode");
                b.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent e) {
                        toggleTabMode(tabPane);
                    }
                });
                vbox.getChildren().add(b);
            }
            {
                final Button b = new Button("Toggle Tab Position");
                b.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent e) {
                        toggleTabPosition(tabPane);
                    }
                });
                vbox.getChildren().add(b);
            }
            {
                final Button b = new Button("Switch to Empty Tab");
                b.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent e) {
                        tabPane.getSelectionModel().select(emptyTab);
                    }
                });
                vbox.getChildren().add(b);
            }
            {
                final Button b = new Button("Switch to New Tab");
                b.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent e) {
                        Tab t = new Tab();
                        t.setText("Testing");
                        t.setContent(new Button("Howdy"));
                        tabPane.getTabs().add(t);
                        tabPane.getSelectionModel().select(t);
                    }
                });
                vbox.getChildren().add(b);
            }
            {
                final Button b = new Button("Add Tab");
                b.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent e) {
                        Tab t = new Tab();
                        t.setText("New Tab");
                        t.setContent(new Region());
                        tabPane.getTabs().add(t);
                    }
                });
                vbox.getChildren().add(b);
            }
            {
                final Button b = new Button("Toggle Popup on Empty Tab");
                b.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent e) {
                        if (emptyTab.getContextMenu() == null) {
                            emptyTab.setContextMenu(menu);
                        } else {
                            emptyTab.setContextMenu(null);
                        }
                    }
                });
                vbox.getChildren().add(b);
            }
            {
                ToggleButton tb = new ToggleButton("Show scroll arrows");
                tb.setSelected(showScrollArrows);
                tb.selectedProperty().addListener(new InvalidationListener() {
                    public void invalidated(Observable ov) {
                        showScrollArrows = !showScrollArrows;   
                    }
                });
                vbox.getChildren().add(tb);
            }
            {
                ToggleButton tb = new ToggleButton("Show Tab Menu Button");
                tb.setSelected(showTabMenu);
                tb.selectedProperty().addListener(new InvalidationListener() {
                    public void invalidated(Observable ov) {
                        showTabMenu = !showTabMenu;
                    }
                });
                vbox.getChildren().add(tb);
            }
            tab1.setContent(vbox);
            tabPane.getTabs().add(tab1);
        }
        {
            tab2.setText("Longer Tab");
            final Image image = new Image(getClass().getResourceAsStream("folder_16.png"));
            final ImageView imageView = new ImageView();
            imageView.setImage(image);
            tab2.setGraphic(imageView);
            final VBox vbox = new VBox();
            vbox.setSpacing(10);
            vbox.setTranslateX(10);
            vbox.setTranslateY(10);

            final ToggleGroup closingPolicy = new ToggleGroup();
            for (TabPane.TabClosingPolicy policy: TabPane.TabClosingPolicy.values()) {
                final ToggleButton button = new ToggleButton(policy.name());
                button.setToggleGroup(closingPolicy);
                button.selectedProperty().addListener(new InvalidationListener() {
                    public void invalidated(Observable ov) {
                        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.valueOf(button.getText()));
                    }
                });
                vbox.getChildren().add(button);
            }

            final ToggleButton rotateGraphics = new ToggleButton("Rotate Graphics");
            rotateGraphics.selectedProperty().addListener(new InvalidationListener() {
                    public void invalidated(Observable ov) {
                    tabPane.setRotateGraphic(rotateGraphics.isSelected());
                }
            });
            vbox.getChildren().add(rotateGraphics);

            tab2.setContent(vbox);
            tabPane.getTabs().add(tab2);
        }
        {
            tab3.setText("Tab 3");
            final Image image = new Image(getClass().getResourceAsStream("heart_16.png"));
            final ImageView imageView = new ImageView();
            imageView.setImage(image);
            tab3.setGraphic(imageView);
            final VBox vbox = new VBox();
            vbox.setSpacing(10);
            vbox.setTranslateX(10);
            vbox.setTranslateY(10);
            {
                final ToggleButton tb = new ToggleButton("Show Labels");
                tb.setSelected(true);
                tb.selectedProperty().addListener(new InvalidationListener() {
                    public void invalidated(Observable ov) {
                        if (tb.isSelected()) {
                            tab1.setText("Tab 1");
                            tab2.setText("Tab 2");
                            tab3.setText("Tab 3");
                        } else {
                            tab1.setText("");
                            tab2.setText("");
                            tab3.setText("");
                        }
                    }
                });
                vbox.getChildren().add(tb);
            }
            {
                final ToggleButton tb = new ToggleButton("Big Graphic 1");
                tb.selectedProperty().addListener(new InvalidationListener() {
                    public void invalidated(Observable ov) {
                        Image image;
                        if (tb.isSelected()) {
                            image = new Image(getClass().getResourceAsStream("about_48.png"));
                        } else {
                            image = new Image(getClass().getResourceAsStream("about_16.png"));
                        }
                        ImageView imageView = new ImageView();
                        imageView.setImage(image);
                        tab1.setGraphic(imageView);
                    }
                });
                vbox.getChildren().add(tb);
            }
            {
                final ToggleButton tb = new ToggleButton("Big Graphic 2");
                tb.selectedProperty().addListener(new InvalidationListener() {
                    public void invalidated(Observable ov) {
                        Image image;
                        if (tb.isSelected()) {
                            image = new Image(getClass().getResourceAsStream("folder_48.png"));
                        } else {
                            image = new Image(getClass().getResourceAsStream("folder_16.png"));
                        }
                        ImageView imageView = new ImageView();
                        imageView.setImage(image);
                        tab2.setGraphic(imageView);
                    }
                });
                vbox.getChildren().add(tb);
            }
            {
                final ToggleButton tb = new ToggleButton("Big Graphic 3");
                tb.selectedProperty().addListener(new InvalidationListener() {
                    public void invalidated(Observable ov) {
                        Image image;
                        if (tb.isSelected()) {
                            image = new Image(getClass().getResourceAsStream("heart_48.png"));
                        } else {
                            image = new Image(getClass().getResourceAsStream("heart_16.png"));
                        }
                        ImageView imageView = new ImageView();
                        imageView.setImage(image);
                        tab3.setGraphic(imageView);
                    }
                });
                vbox.getChildren().add(tb);
            }
            tab3.setContent(vbox);
            tabPane.getTabs().add(tab3);
        }

        emptyTab.setText("Empty Tab");
        emptyTab.setContent(new Region());
        tabPane.getTabs().add(emptyTab);

        emptyTab.setOnSelectionChanged(new EventHandler<Event>() {
            public void handle(Event t) {
                System.out.println("Empty tab selected");
            }
        });

        emptyTab.setOnClosed(new EventHandler<Event>() {
            public void handle(Event t) {
                System.out.println("Empty tab closed");
            }
        });

        internalTab.setText("Internal Tab");
        setupInternalTab();
        tabPane.getTabs().add(internalTab);

        multipleTabs.setText("Multiple Tabs");
        setupMultipleInteralTabs();
        tabPane.getTabs().add(multipleTabs);

        {
            Tab tab = new Tab();
            tab.setText("Tab 4");
            tab.setClosable(false);
            tab.setContent(new Region());
            tabPane.getTabs().add(tab);
        }

        for (int i = 5; i < 9; i++) {
            Tab tab = new Tab();
            tab.setText("Tab " + i);
            tab.setContent(new Region());
            tabPane.getTabs().add(tab);
        }
        ((Group)scene.getRoot()).getChildren().add(tabPane);
        stage.setScene(scene);
        stage.show();
    }

    private void setupInternalTab() {
        StackPane internalTabContent = new StackPane();

        Rectangle r = new Rectangle(700, 500);
        r.setFill(Color.LIGHTSTEELBLUE);
        internalTabContent.getChildren().add(r);

        final TabPane internalTabPane = new TabPane();
        internalTabPane.getStyleClass().add(TabPane.STYLE_CLASS_FLOATING);
        internalTabPane.setSide(Side.LEFT);
        internalTabPane.setPrefSize(500, 500);
        {
            final Tab tab = new Tab();
            tab.setText("Tab 1");
            final VBox vbox = new VBox();
            vbox.setSpacing(10);
            vbox.setTranslateX(10);
            vbox.setTranslateY(10);
            {
                Button b = new Button("Toggle Tab Position");
                b.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent e) {
                        toggleTabPosition(internalTabPane);
                    }
                });
                vbox.getChildren().add(b);
            }
            {
                Button b = new Button("Toggle Tab Mode");
                b.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent e) {
                        toggleTabMode(internalTabPane);
                    }
                });
                vbox.getChildren().add(b);
            }
            tab.setContent(vbox);
            internalTabPane.getTabs().add(tab);
        }
        {
            final Tab tab = new Tab();
            tab.setText("Tab 2");
            final VBox vbox = new VBox();
            vbox.setSpacing(10);
            vbox.setTranslateX(10);
            vbox.setTranslateY(10);
            Button b = new Button("Button 2");
            vbox.getChildren().add(b);
            tab.setContent(vbox);
            internalTabPane.getTabs().add(tab);
        }
        {
            final Tab tab = new Tab();
            tab.setText("Tab 3");
            final VBox vbox = new VBox();
            vbox.setSpacing(10);
            vbox.setTranslateX(10);
            vbox.setTranslateY(10);
            Button b = new Button("Button 3");
            vbox.getChildren().add(b);
            tab.setContent(vbox);
            internalTabPane.getTabs().add(tab);
        }
        for (int i = 4; i < 10; i++) {
            Tab tab = new Tab();
            tab.setText("Tab " + i);
            tab.setContent(new Region());
            internalTabPane.getTabs().add(tab);
        }
        internalTabContent.getChildren().add(internalTabPane);
        internalTab.setContent(internalTabContent);
    }

    private void setupMultipleInteralTabs() {
        String tabStrings[] = { "Labrador", "Poodle", "Boxer"/*, "Great Dane"*/ };
        FlowPane flow = new FlowPane();
        flow.setHgap(20);
        flow.setVgap(20);
        flow.setPrefWrapLength(500);

        TabPane internalTabPane = new TabPane();
        for(String tabstring : tabStrings) {
            Tab tab = new Tab();
            tab.setText(tabstring);
            StackPane stack = new StackPane();
            Rectangle rect = new Rectangle(200,200, Color.LIGHTSTEELBLUE);
            stack.getChildren().addAll(rect, new Button(" A type of dog: "+tabstring));
            tab.setContent(stack);
            internalTabPane.getTabs().add(tab);
        }
        flow.getChildren().add(internalTabPane);

        internalTabPane = new TabPane();
        internalTabPane.setSide(Side.RIGHT);
        for(String tabstring : tabStrings) {
            Tab tab = new Tab();
            tab.setText(tabstring);
            StackPane stack = new StackPane();
            Rectangle rect = new Rectangle(200,200, Color.ANTIQUEWHITE);
            stack.getChildren().addAll(rect, new Button(" A type of dog: "+tabstring));
            tab.setContent(stack);                    internalTabPane.getTabs().add(tab);
        }
        flow.getChildren().add(internalTabPane);

        internalTabPane = new TabPane();
        internalTabPane.setSide(Side.BOTTOM);
        for(String tabstring : tabStrings) {
            Tab tab = new Tab();
            tab.setText(tabstring);
            StackPane stack = new StackPane();
            Rectangle rect = new Rectangle(200,200, Color.YELLOWGREEN);
            stack.getChildren().addAll(rect, new Button(" A type of dog: "+tabstring));
            tab.setContent(stack);                    internalTabPane.getTabs().add(tab);
        }
        flow.getChildren().add(internalTabPane);

        internalTabPane = new TabPane();
        internalTabPane.setSide(Side.LEFT);
        for(String tabstring : tabStrings) {
            Tab tab = new Tab();
            tab.setText(tabstring);
            StackPane stack = new StackPane();
            Rectangle rect = new Rectangle(200,200, Color.RED);
            stack.getChildren().addAll(rect, new Button(" A type of dog: "+tabstring));
            tab.setContent(stack);                    internalTabPane.getTabs().add(tab);
        }
        flow.getChildren().add(internalTabPane);
        multipleTabs.setContent(flow);
    }

    private void toggleTabPosition(TabPane tabPane) {
        Side pos = tabPane.getSide();
        if (pos == Side.TOP) {
            tabPane.setSide(Side.RIGHT);
        } else if (pos == Side.RIGHT) {
            tabPane.setSide(Side.BOTTOM);
        } else if (pos == Side.BOTTOM) {
            tabPane.setSide(Side.LEFT);
        } else {
            tabPane.setSide(Side.TOP);
        }
    }

    private void toggleTabMode(TabPane tabPane) {
        if (!tabPane.getStyleClass().contains(TabPane.STYLE_CLASS_FLOATING)) {
            tabPane.getStyleClass().add(TabPane.STYLE_CLASS_FLOATING);
        } else {
            tabPane.getStyleClass().remove(TabPane.STYLE_CLASS_FLOATING);
        }
    }

    private void setUpPopupMenu() {
        menu = new ContextMenu();
        menu.getItems().add(new MenuItem("Item 1"));
        menu.getItems().add(new MenuItem("Item 2"));
        menu.getItems().add(new MenuItem("Item 3"));
        menu.getItems().add(new MenuItem("Item 4"));
        menu.getItems().add(new MenuItem("Item 5"));
        menu.getItems().add(new MenuItem("Item 6"));
    }
}

