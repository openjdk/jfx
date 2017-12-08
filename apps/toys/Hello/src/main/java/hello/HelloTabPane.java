/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
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
    private Tab tabForDragPolicy;
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
        tabForDragPolicy = new Tab();
        setUpPopupMenu();
        stage.setTitle("Hello TabPane2");
        final Scene scene = new Scene(new Group(), 1200, 800);
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
        {
            tabForDragPolicy.setText("TabDragPolicy");
            tabForDragPolicy.setContent(setupDragPolicyTab());
            tabPane.getTabs().add(tabForDragPolicy);
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

    private VBox setupDragPolicyTab() {
        Label indexErr = new Label("");
        Label angleErr = new Label("");
        final int LABEL_WIDTH = 150;
        VBox mainContent = new VBox();
        mainContent.setSpacing(12);
        mainContent.setMinSize(1000, 400);

        TabPane tabPane = new TabPane();
        tabPane.setMinSize(500, 400);
        tabPane.setMaxSize(500, 400);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        HBox tabPaneAndInstr = new HBox();
        HBox tabPaneParent = new HBox();
        tabPaneParent.getChildren().add(tabPane);
        tabPaneAndInstr.getChildren().add(tabPaneParent);

        for (int i = 0; i < 7; ++i) {
            String text = "" + i + i + i;
            HBox hb = new HBox();
            Tab tab = new Tab(text);
            TextArea ta = new TextArea(text);
            ta.setMaxSize(300, 100);
            hb.getChildren().add(ta);
            tab.setContent(hb);
            tabPane.getTabs().add(tab);
        }

        TextArea instructions = new TextArea(
            "**** INSTRUCTIONS ****\n\n" +
            "1. TabDragPolicy.FIXED : Default policy.\n" +
            "Click the FIXED button to set FIXED TabDragPolicy.\n" +
            "The tabs remain fixed & tab headers cannot be dragged to reorder.\n\n" +
            "2. TabDragPolicy.REORDER :\n" +
            "Click the REORDER button to set REORDER TabDragPolicy.\n" +
            "The tabs can be reordered with mouse press & drag action on tab header.\n\n" +
            "3. With each of the drag policy,\n" +
            "Choose different combinations of\n" +
            "sides (TOP or BOTTOM or LEFT or RIGHT),\n" +
            "node orientations, (LTR or RTL) and\n" +
            "different rotation angle.\n\n" +
            "4. Perform reordering and verify the correctness of the\n" +
            "printed Current order of tabs and permuted tabs.\n" +
            "And verify the navigation of tabs using left, right arrow keys.\n\n" +
            "5. Additionally, also verify the outputs with ADD, REMOVE\n" +
            "and REVERSE buttons."
        );
        tabPaneAndInstr.getChildren().add(instructions);
        mainContent.getChildren().add(tabPaneAndInstr);

        Label permuted = new Label("Permuted tabs");
        permuted.setMinWidth(LABEL_WIDTH);
        Label outputPermutedTabs = new Label();
        Label added = new Label   ("Added tabs");
        added.setMinWidth(LABEL_WIDTH);
        Label outputAddedTabs = new Label();
        Label removed = new Label ("Removed tabs");
        removed.setMinWidth(LABEL_WIDTH);
        Label outputRemovedTabs = new Label();
        Label getTabs = new Label ("Current order of Tabs");
        getTabs.setMinWidth(LABEL_WIDTH);
        Label outputListOfTabs = new Label();
        tabPane.setOnMousePressed(event -> {
            outputPermutedTabs.setText("");
            outputAddedTabs.setText("");
            outputRemovedTabs.setText("");
            outputListOfTabs.setText("");
            angleErr.setText("");
            indexErr.setText("");
        });
        VBox notifications = new VBox();
        notifications.setSpacing(10);
        notifications.setStyle("-fx-border-color: black");
        notifications.setPadding(new Insets(10));
        HBox permut = new HBox();
        permut.getChildren().addAll(permuted, outputPermutedTabs);

        HBox adds = new HBox();
        adds.getChildren().addAll(added, outputAddedTabs);

        HBox removes = new HBox();
        removes.getChildren().addAll(removed, outputRemovedTabs);

        HBox allTabs = new HBox();
        allTabs.getChildren().addAll(getTabs, outputListOfTabs);
        notifications.getChildren().addAll(permut, adds, removes, allTabs);
        mainContent.getChildren().add(notifications);

        tabPane.getTabs().addListener((ListChangeListener<Tab>) c -> {
            while (c.next()) {
                String list = "";
                outputPermutedTabs.setText("");
                outputAddedTabs.setText("");
                outputRemovedTabs.setText("");
                for (int i = c.getFrom(); i < c.getTo(); i++) {
                    list += tabPane.getTabs().get(i).getText() + ",  ";
                }
                if (!list.equals("")) {
                    list = list.substring(0, list.length() - 3);
                }

                if (c.wasPermutated()) {
                    outputPermutedTabs.setText(list);
                } else if (c.wasAdded()) {
                    outputAddedTabs.setText(list);
                } else if (c.wasRemoved()) {
                    list = "";
                    for (Tab t : c.getRemoved()) {
                        list += t.getText() + ",  ";
                    }
                    if (!list.equals("")) {
                        list = list.substring(0, list.length() - 3);
                    }
                    outputRemovedTabs.setText(list);
                }
                list = "";
                for (Tab t : tabPane.getTabs()) {
                    list += t.getText() + ",  ";
                }
                if (!list.equals("")) {
                    list = list.substring(0, list.length() - 3);
                }
                outputListOfTabs.setText(list);
            }
        });

        HBox actions = new HBox();
        actions.setStyle("-fx-border-color: black");
        actions.setSpacing(20);
        actions.setPadding(new Insets(10));

        VBox sideDragPolNodeOri = new VBox();
        sideDragPolNodeOri.setSpacing(5);
        actions.getChildren().add(sideDragPolNodeOri);

        ToggleGroup side = new ToggleGroup();
        ToggleButton top = new ToggleButton("TOP");
        top.setSelected(true);
        top.setUserData(Side.TOP);
        ToggleButton bottom = new ToggleButton("BOTTOM");
        bottom.setUserData(Side.BOTTOM);
        ToggleButton left = new ToggleButton("LEFT");
        left.setUserData(Side.LEFT);
        ToggleButton right = new ToggleButton("RIGHT");
        right.setUserData(Side.RIGHT);

        top.setToggleGroup(side);
        bottom.setToggleGroup(side);
        left.setToggleGroup(side);
        right.setToggleGroup(side);

        side.selectedToggleProperty().addListener(observable -> {
            if (side.getSelectedToggle() == null) {
                tabPane.setSide(Side.TOP);
                top.setSelected(true);
            } else {
                tabPane.setSide((Side) side.getSelectedToggle().getUserData());
            }
            tabPane.requestFocus();
        });

        HBox sides = new HBox();
        sides.setSpacing(5);
        Label sid = new Label("Sides");
        sid.setMinWidth(LABEL_WIDTH);
        sides.getChildren().add(sid);
        sides.getChildren().add(top);
        sides.getChildren().add(bottom);
        sides.getChildren().add(left);
        sides.getChildren().add(right);
        sideDragPolNodeOri.getChildren().add(sides);

        ToggleGroup dragPolicy = new ToggleGroup();
        ToggleButton reorder = new ToggleButton("REORDER");
        reorder.setUserData(TabPane.TabDragPolicy.REORDER);
        reorder.setToggleGroup(dragPolicy);
        ToggleButton fixed = new ToggleButton("FIXED");
        fixed.setSelected(true);
        fixed.setUserData(TabPane.TabDragPolicy.FIXED);
        fixed.setToggleGroup(dragPolicy);

        HBox dragPolicies = new HBox();
        dragPolicies.setSpacing(5);
        Label dp = new Label("Drag Policies");
        dp.setMinWidth(LABEL_WIDTH);
        dragPolicies.getChildren().add(dp);
        dragPolicies.getChildren().add(fixed);
        dragPolicies.getChildren().add(reorder);
        sideDragPolNodeOri.getChildren().add(dragPolicies);
        dragPolicy.selectedToggleProperty().addListener(observable -> {
            if (dragPolicy.getSelectedToggle() == null) {
                fixed.setSelected(true);
                tabPane.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);
            } else {
                tabPane.setTabDragPolicy((TabPane.TabDragPolicy) dragPolicy.getSelectedToggle().getUserData());
            }
            tabPane.requestFocus();
        });

        ToggleGroup orientation = new ToggleGroup();
        ToggleButton ltr = new ToggleButton("LEFT TO RIGHT");
        ltr.setSelected(true);
        ltr.setUserData(NodeOrientation.LEFT_TO_RIGHT);
        ltr.setToggleGroup(orientation);
        ToggleButton rtl = new ToggleButton("RIGHT TO LEFT");
        rtl.setUserData(NodeOrientation.RIGHT_TO_LEFT);
        rtl.setToggleGroup(orientation);
        HBox orientations = new HBox();
        orientations.setSpacing(5);
        Label no = new Label("Node Orientations");
        no.setMinWidth(LABEL_WIDTH);
        orientations.getChildren().add(no);
        orientations.getChildren().add(ltr);
        orientations.getChildren().add(rtl);
        sideDragPolNodeOri.getChildren().add(orientations);
        orientation.selectedToggleProperty().addListener(observable -> {
            if (orientation.getSelectedToggle() == null) {
                tabPane.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
                ltr.setSelected(true);
            } else {
                tabPane.setNodeOrientation((NodeOrientation) orientation.getSelectedToggle().getUserData());
            }
            tabPane.requestFocus();
        });

        VBox addRemRotRev = new VBox();
        addRemRotRev.setSpacing(5);
        actions.getChildren().add(addRemRotRev);

        HBox addRemove = new HBox();
        addRemove.setSpacing(5);
        Button add = new Button("ADD");
        Button remove = new Button("REMOVE");
        HBox indexContainer = new HBox();
        Label indexLabel = new Label("Index: ");
        TextField indexTF = new TextField("0");
        indexTF.setMaxWidth(70);
        indexContainer.getChildren().addAll(indexLabel, indexTF);
        Label ar = new Label("Add / Remove index");
        ar.setMinWidth(LABEL_WIDTH);
        addRemove.getChildren().add(ar);
        addRemove.getChildren().add(add);
        addRemove.getChildren().add(remove);
        addRemove.getChildren().add(indexContainer);
        addRemove.getChildren().add(indexErr);

        add.setOnMouseClicked(event -> {
            try {
                int i = Integer.parseInt(indexTF.getText());
                if (i >= 0 && i < tabPane.getTabs().size()) {
                    tabPane.getTabs().add(i, new Tab("" + i + i + i + i));
                    tabPane.requestFocus();
                }
                indexErr.setText("");
            } catch (Exception e) {
                indexErr.setText("Incorrect Index");
            }
        });
        remove.setOnMouseClicked(event -> {
            try {
                int index = Integer.parseInt(indexTF.getText());
                if (index >= 0 && index < tabPane.getTabs().size()) {
                    tabPane.getTabs().remove(index);
                    tabPane.requestFocus();
                }
                indexErr.setText("");
            } catch (Exception e) {
                indexErr.setText("Incorrect Index");
            }
        });
        addRemRotRev.getChildren().add(addRemove);

        HBox angleContainer = new HBox();
        Label angleLabel = new Label("Angle: ");
        TextField angleTF = new TextField("0");
        angleTF.setMaxWidth(70);
        angleContainer.getChildren().addAll(angleLabel, angleTF);
        Label rotLabel = new Label("Rotate");
        rotLabel.setMinWidth(LABEL_WIDTH);
        Button rotate = new Button("TabPane");
        rotate.setOnMouseClicked(event -> {
            try {
                tabPane.setRotate(Float.parseFloat(angleTF.getText()));
                angleErr.setText("");
            } catch (Exception e) {
                angleErr.setText("Incorrect Angle");
            }
            tabPane.requestFocus();
        });
        Button rotateParent = new Button("TabPane Parent");
        rotateParent.setOnMouseClicked(event -> {
            try {
                tabPaneParent.setRotate(Float.parseFloat(angleTF.getText()));
                angleErr.setText("");
            } catch (Exception e) {
                angleErr.setText("Incorrect Angle");
            }
            tabPane.requestFocus();
        });
        HBox rotation = new HBox();
        rotation.setSpacing(5);
        rotation.getChildren().addAll(rotLabel, rotate, rotateParent, angleContainer, angleErr);
        addRemRotRev.getChildren().add(rotation);

        Label reverseLabel = new Label("Reverse order of tabs");
        reverseLabel.setMinWidth(LABEL_WIDTH);
        Button reverse = new Button("REVERSE");
        reverse.setOnMouseClicked(event -> {
            tabPane.getTabs().sort((o1, o2) -> {
                if (tabPane.getTabs().indexOf(o1) > tabPane.getTabs().indexOf(o2)) {
                    return -1;
                } else {
                    return 1;
                }
            });
            tabPane.requestFocus();
        });
        HBox revContainer = new HBox();
        revContainer.setSpacing(5);
        revContainer.getChildren().addAll(reverseLabel, reverse);
        addRemRotRev.getChildren().add(revContainer);

        actions.setOnMousePressed(event -> {
            angleErr.setText("");
            indexErr.setText("");
        });
        mainContent.getChildren().add(actions);
        return mainContent;
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

