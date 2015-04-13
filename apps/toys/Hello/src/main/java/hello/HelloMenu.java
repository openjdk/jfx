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

package hello;

import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.text.SimpleDateFormat;
import java.util.Date;

public class HelloMenu extends Application {

    //private static final String imageURL = "file:hello/about_16.png";

    private CheckMenuItem showMessagesItem;
    private final Label sysMenuLabel = new Label("Using System Menu");
    @Override public void start(Stage stage) {
        stage.setTitle("Hello Menu");
        Scene scene = new Scene(new VBox(20), 400, 350);
        final MenuBar menuBar = new MenuBar();
        final String os = System.getProperty("os.name");
        EventHandler actionHandler = t -> {
            if (t.getTarget() instanceof MenuItem) {
                System.out.println(((MenuItem)t.getTarget()).getText() + " - action called");
            }
        };

        // --- Menu 1
        final Menu menu1 = makeMenu("_Debug");

        final Menu menu11 = makeMenu("_New", new ImageView(new Image("hello/about_16.png")));
        MenuItem menu12 = new MenuItem("_Open", new ImageView(new Image("hello/folder_16.png")));
        menu12.setAccelerator(new KeyCharacterCombination("]", 
                KeyCombination.SHIFT_DOWN, KeyCombination.META_DOWN));
        menu12.setOnAction(actionHandler);
        Menu menu13 = makeMenu("_Submenu");
        showMessagesItem = new CheckMenuItem("Enable onShowing/onHiding _messages", 
                                             new ImageView(new Image("hello/about_16.png")));
        MenuItem menu15 = new MenuItem("E_xit");
        menu15.setOnAction(t -> System.exit(0));
        final String change[] = {"Change Text", "Change Back"};
        final MenuItem menu16 = new MenuItem(change[0]);
        final boolean toggle = false;
        menu16.setAccelerator(KeyCombination.keyCombination("Shortcut+C"));
        menu16.setOnAction(t -> menu16.setText((menu16.getText().equals(change[0])) ? change[1] : change[0]));
        menu1.getItems().addAll(menu11, menu12, menu13, showMessagesItem, new SeparatorMenuItem(), menu15, menu16);


        // --- Menu 11 submenu
        final MenuItem menu111 = new MenuItem("blah");
        menu111.setOnAction(actionHandler);
        final MenuItem menu112 = new MenuItem("foo");
        menu112.setOnAction(actionHandler);
        final CheckMenuItem menu113 = new CheckMenuItem("Show \"foo\" item");
        menu113.setSelected(true);
        menu113.selectedProperty().addListener(valueModel -> {
            menu112.setVisible(menu113.isSelected());
            System.err.println("MenuItem \"foo\" is now " + (menu112.isVisible() ? "" : "not") + " visible.");
        });
        menu11.getItems().addAll(menu111, menu112, menu113);


        // --- Menu 13 submenu
        MenuItem menu131 = new MenuItem("Item _1");
        menu131.setOnAction(actionHandler);
        MenuItem menu132 = new MenuItem("Item _2");
        menu132.setOnAction(actionHandler);
        menu13.getItems().addAll(menu131, menu132);


        // --- Menu 2
        Menu menu2 = makeMenu("_Edit");
        MenuItem menu21 = new MenuItem("_Undo");
        menu21.setAccelerator(KeyCombination.keyCombination("shortcut+Z"));
        menu21.setOnAction(actionHandler);
        MenuItem menu22 = new MenuItem("_Redo");
        menu22.setAccelerator(KeyCombination.keyCombination("shortcut+Y"));
        menu22.setOnAction(actionHandler);
        // menu separator
        MenuItem menu23 = new MenuItem("_Disabled");
        menu23.setDisable(true);
        // menu separator
        MenuItem menu24 = new MenuItem("Copy");
        menu24.setAccelerator(KeyCombination.keyCombination("shortcut+C"));
        menu24.setOnAction(actionHandler);
        MenuItem menu25 = new MenuItem("Paste");
        menu25.setAccelerator(KeyCombination.keyCombination("shortcut+V"));
        menu25.setOnAction(actionHandler);
        MenuItem menu26 = new MenuItem("Delete");
        menu26.setAccelerator(KeyCombination.keyCombination("shortcut+D"));
        MenuItem menu27 = new MenuItem("Help");
        menu27.setAccelerator(new KeyCodeCombination(KeyCode.F1));
        menu27.setOnAction(actionHandler);
        menu27.setDisable(false);
        menu2.getItems().addAll(menu21, menu22, new SeparatorMenuItem(), menu23,
                 menu24, menu25, menu26, menu27);
        

        // --- Menu 3
        Menu menu3 = makeMenu("_Radio/CheckBox");
        CheckMenuItem checkMI1 = new CheckMenuItem("_1 CheckMenuItem - checked");
        checkMI1.setSelected(true);
        CheckMenuItem checkMI2 = new CheckMenuItem("_2 CheckMenuItem - not checked");

        RadioMenuItem radioMI1 = new RadioMenuItem("_3 RadioMenuItem - selected");
        radioMI1.setSelected(true);
        RadioMenuItem radioMI2 = new RadioMenuItem("_4 RadioMenuItem - not selected");
        ToggleGroup group = new ToggleGroup();
        radioMI1.setToggleGroup(group);
        radioMI2.setToggleGroup(group);

        InvalidationListener selectedListener = valueModel -> {
            MenuItem mi = (MenuItem)((BooleanProperty)valueModel).getBean();
            boolean selected = ((BooleanProperty)valueModel).get();
            System.err.println(mi.getText() + " - " + selected);
        };

        checkMI1.selectedProperty().addListener(selectedListener);
        checkMI2.selectedProperty().addListener(selectedListener);
        radioMI1.selectedProperty().addListener(selectedListener);
        radioMI2.selectedProperty().addListener(selectedListener);

        menu3.getItems().addAll(checkMI1, checkMI2, radioMI1, radioMI2);
//        ImageView im = new ImageView();
//        im.setImage(new Image(imageURL));
//        cmitem.setGraphic(im);

        menuBar.getMenus().add(menu1);
        menuBar.getMenus().add(menu2);
        menuBar.getMenus().add(menu3);
        
        if (os != null && os.startsWith("Mac")) {
            Menu systemMenuBarMenu = makeMenu("MenuBar _Options");

            final CheckMenuItem useSystemMenuBarCB = new CheckMenuItem("Use _System Menu Bar");
            useSystemMenuBarCB.setSelected(true);
            menuBar.useSystemMenuBarProperty().bind(useSystemMenuBarCB.selectedProperty());
            systemMenuBarMenu.getItems().add(useSystemMenuBarCB);

            menuBar.getMenus().add(systemMenuBarMenu);
        }

        ((VBox)scene.getRoot()).getChildren().add(menuBar);
        if (os != null && os.startsWith("Mac")) {
            HBox hbox = new HBox();
            hbox.setAlignment(Pos.CENTER);
            sysMenuLabel.setStyle("-fx-font-size: 24");
            hbox.getChildren().add(sysMenuLabel);
            ((VBox)scene.getRoot()).getChildren().add(hbox);
            sysMenuLabel.setVisible((menuBar.getHeight() == 0) ? true : false);
            menuBar.heightProperty().addListener((ov, t, t1) -> sysMenuLabel.setVisible((menuBar.getHeight() == 0) ? true : false));
        }
        stage.setScene(scene);
        stage.show();
    }

    private EventHandler showHideHandler = t -> {
        Menu menu = (Menu)t.getSource();
        if (t.getEventType() == Menu.ON_SHOWING && 
                menu.getText().equals("_Submenu")) {
            Date date = new Date();
            String time = new SimpleDateFormat("HH:mm:ss").format(date);
            menu.getItems().get(0).setText("The time is " + time);
        }
        if (showMessagesItem.isSelected()) {
            System.out.println(((Menu)t.getSource()).getText() + " " + t.getEventType());
        }
    };

    private Menu makeMenu(String text) {
        return makeMenu(text, null);
    }

    private Menu makeMenu(String text, Node graphic) {
        Menu menu = new Menu(text, graphic);
        menu.setOnShowing(showHideHandler);
        menu.setOnShown(showHideHandler);
        menu.setOnHiding(showHideHandler);
        menu.setOnHidden(showHideHandler);
        return menu;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
