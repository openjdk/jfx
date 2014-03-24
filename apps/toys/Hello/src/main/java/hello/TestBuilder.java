/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.animation.PathTransition;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Bounds;
import javafx.geometry.BoundingBox;
import javafx.geometry.Orientation;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.SwipeEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.Line;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.animation.AnimationTimer;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import com.sun.glass.events.KeyEvent;
import com.sun.glass.ui.Robot;
    
/**
 * The class is a singleton, used by HelloSanity application 
 * for tests creation 
 */

public class TestBuilder {

    //Variables used by "HelloMenu" section
    private CheckMenuItem showMessagesItem;
    private final Label sysMenuLabel = new Label("Using System Menu");

    //Variables used by "HelloComboBox" section
    private final ObservableList<String> strings = FXCollections.observableArrayList(
            "Option 1", "Option 2", "Option 3", 
            "Option 4", "Option 5", "Option 6",
            "Long ComboBox item 1 2 3 4 5 6 7 8 9",
            "Option 7", "Option 8", "Option 9", "Option 10", "Option 12", "Option 13",
            "Option 14", "Option 15", "Option 16", "Option 17", "Option 18", "Option 19",
            "Option 20", "Option 21", "Option 22", "Option 23", "Option 24", "Option 25",
            "Option 26", "Option 27", "Option 28", "Option 29", "Option 30", "Option 31");

    private final ObservableList<String> fonts = FXCollections.observableArrayList(Font.getFamilies());

    //Variables used by "HelloTabPane" section
    private TabPane tabPane;
    private Tab tab1, tab2, tab3, emptyTab, internalTab, multipleTabs;
    private ContextMenu menu;
    private boolean showScrollArrows = false;
    private boolean showTabMenu = false;

    //Variables used by "HelloButtonMenu" section
    private final static int column_enabled = 10;
    private final static int column_disabled = 160;
    private final static int line_spacing = 30;

    //Variable used by "RobotTest" section
    private final Rectangle rec1 = new Rectangle(50, 50, 40, 160);
    
    private static TestBuilder instance;

    protected TestBuilder() {}

    public static TestBuilder getInstance() {
        if (instance==null)
                 instance = new TestBuilder();
        return instance;
    }

    /**
     * The method updates globalScene with different controls and 
     * possible to test them manually 
     * @param globalScene the global Scene
     * @param mainBox the Box to insert into
     */
    public void controlTest(final Scene globalScene, final VBox mainBox) {

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(13, 13, 13, 13));
        grid.setVgap(10);
        grid.setHgap(10);
        Label l = new Label(" Button:  ");
        grid.setConstraints(l, 0, 0);
        grid.getChildren().add(l);
        Button btn = new Button("Back");
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                globalScene.setRoot(mainBox);
            }
        });
        grid.setConstraints(btn, 1, 0);
        grid.getChildren().add(btn);
        l = new Label(" Radio Button:  ");
        grid.setConstraints(l, 0, 1);
        grid.getChildren().add(l);

        RadioButton rb = new RadioButton("Cancel");
        grid.setConstraints(rb, 1, 1);
        grid.getChildren().add(rb);

        l = new Label(" CheckBox:  ");
        grid.setConstraints(l, 0, 2);
        grid.getChildren().add(l);

        CheckBox cb = new CheckBox("Cancel");
        cb.setSelected(true);
        grid.setConstraints(cb, 1, 2);
        grid.getChildren().add(cb);

        l = new Label(" Slider:  ");
        grid.setConstraints(l, 0, 3);
        grid.getChildren().add(l);

        Slider sl = new Slider();
        sl.setShowTickMarks(true);
        sl.setShowTickLabels(true);
        grid.setConstraints(sl, 1, 3);
        grid.getChildren().add(sl);

        l = new Label(" Vertical Slider:  ");
        grid.setConstraints(l, 0, 4);
        grid.getChildren().add(l);

        final Slider vslider = new Slider();
        vslider.setShowTickMarks(true);
        vslider.setShowTickLabels(true);
        vslider.setOrientation(Orientation.VERTICAL);
        final Slider vslider2 = new Slider();
        vslider2.setOrientation(Orientation.VERTICAL);
        final HBox shbox = new HBox();
        shbox.setSpacing(20);
        shbox.getChildren().addAll(vslider2, vslider);
        grid.setConstraints(shbox, 1, 4);
        grid.getChildren().add(shbox);

        l = new Label(" Text Box:  ");
        grid.setConstraints(l, 0, 5);
        grid.getChildren().add(l);

        TextField text = new TextField("Text");
        text.getProperties().put("vkType","text");
        grid.setConstraints(text, 1, 5);
        grid.getChildren().add(text);

        l = new Label("ScrollPane: ");
        grid.setConstraints(l, 2, 4);
        grid.getChildren().add(l);

        ScrollPane sv = new ScrollPane();
        sv.setPrefViewportWidth(100);
        sv.setPrefViewportHeight(100);
        sv.setPannable(true);
        final Group svg = new Group();
        final Rectangle rect = new Rectangle();
        rect.setWidth(280);
        rect.setHeight(280);
        rect.setStroke(Color.DODGERBLUE);
        rect.setFill(Color.PALETURQUOISE);
        final Line line1 = new Line();
        line1.setEndX(280);
        line1.setEndY(280);
        line1.setStroke(Color.DODGERBLUE);
        final Line line2 = new Line();
        line2.setStartX(280);
        line2.setEndX(0);
        line2.setEndY(280);
        line2.setStroke(Color.DODGERBLUE);
        svg.getChildren().addAll(rect, line1, line2);
        sv.setContent(svg);
        grid.setConstraints(sv, 3, 4);
        grid.getChildren().add(sv);

        l = new Label("ListView:");
        grid.setConstraints(l, 2, 1);
        grid.getChildren().add(l);
       
        ListView<String> lv = new ListView<String>();
        lv.setPrefSize(120, 120);
        ObservableList<String> lvi = FXCollections.observableArrayList();
        for (int i = 0; i < 3000; i++) {
            lvi.add("Item" + i);
        }
        lv.setItems(lvi);
        grid.setConstraints(lv, 3, 1);
        grid.getChildren().add(lv);
        

        l = new Label("ListView:");
        grid.setConstraints(l, 4, 1);
        grid.getChildren().add(l);

        lv = new ListView<String>();
        lv.setOrientation(Orientation.HORIZONTAL);
        lv.setPrefSize(120, 120);
        lvi = FXCollections.observableArrayList();
        for (int i = 0; i < 30; i++) {
            lvi.add("I"+i);
        }
        lv.setItems(lvi);
        grid.setConstraints(lv, 5, 1);
        grid.getChildren().add(lv);
        
        
        //text fields for VK behavior testing
        l = new Label("Email:");
        grid.setConstraints(l, 4, 3);
        grid.getChildren().add(l);
        
        TextField text3 = new TextField("jhon@yahoo.com");
        text3.getProperties().put("vkType", "email");
        grid.setConstraints(text3, 5, 3);
        grid.getChildren().add(text3);
        
        l = new Label("URL:");
        grid.setConstraints(l, 4, 4);
        grid.getChildren().add(l);
        
        TextField text4 = new TextField("oracle.com");
        text4.getProperties().put("vkType", "url");
        grid.setConstraints(text4, 5, 4);
        grid.getChildren().add(text4);
        
        l = new Label("Numeric:");
        grid.setConstraints(l, 4, 5);
        grid.getChildren().add(l);
        
        TextField text5 = new TextField("1234");
        grid.setConstraints(text5, 5, 5);
        text5.getProperties().put("vkType", "numeric");
        grid.getChildren().add(text5); 
     
        l = new Label("Controls Demo");
        VBox vb = new VBox();
        vb.setAlignment(Pos.CENTER);
        vb.setPadding(new Insets(10, 10, 10, 10));
        vb.getChildren().addAll(l,grid);
        globalScene.setRoot(vb);
    }

    /**
     * The method updates globalScene with different menus and combo
     * boxes and possible to test them manually 
     * @param globalScene the global Scene
     * @param mainBox the Box to insert into
     * @param mainstage the main stage to use
     */
    public void menusTest(final Scene globalScene, final VBox mainBox,
                          final Stage mainstage) {

        VBox vb = new VBox();
        vb.setAlignment(Pos.TOP_CENTER);
        vb.setPadding(new Insets(10, 10, 10, 10));

        final MenuBar menuBar = new MenuBar();
        final String os = System.getProperty("os.name");
        final EventHandler actionHandler = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                if (t.getTarget() instanceof MenuItem) {
                    System.out.println(((MenuItem)t.getTarget()).
                                       getText() + " - action called");
                }
            }
        };

        final Menu menu1 = makeMenu("_Debug");
        final Menu menu11 = makeMenu("_New", new ImageView(
                                            new Image("hello/about_16.png")));
        MenuItem menu12 = new MenuItem("_Open", new ImageView(
                                            new Image("hello/folder_16.png")));
        menu12.setAccelerator(new KeyCharacterCombination("]", 
                KeyCombination.SHIFT_DOWN, KeyCombination.META_DOWN));
        menu12.setOnAction((javafx.event.EventHandler<javafx.event.ActionEvent>)
                           actionHandler);
        Menu menu13 = makeMenu("_Submenu");
        showMessagesItem = new CheckMenuItem("Enable onShowing/onHiding _messages",
                           new ImageView(new Image("hello/about_16.png")));
        MenuItem menu15 = new MenuItem("E_xit");
        menu15.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                actionHandler.handle(t);
                System.exit(0);
            }
        });
        final String change[] = {"Change Text", "Change Back"};
        final MenuItem menu16 = new MenuItem(change[0]);
        final boolean toggle = false;
        menu16.setAccelerator(KeyCombination.keyCombination("Shortcut+C"));
        menu16.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                actionHandler.handle(t);
                menu16.setText((menu16.getText().equals(
                                    change[0])) ? change[1] : change[0]);
            }
        });
        menu1.getItems().addAll(menu11, menu12, menu13, showMessagesItem,
                                new SeparatorMenuItem(), menu15, menu16);

        final MenuItem menu111 = new MenuItem("blah");
        menu111.setOnAction(actionHandler);
        final MenuItem menu112 = new MenuItem("foo");
        menu112.setOnAction(actionHandler);
        final CheckMenuItem menu113 = new CheckMenuItem("Show \"foo\" item");
        menu113.setSelected(true);
        menu113.selectedProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                menu112.setVisible(menu113.isSelected());
                System.err.println("MenuItem \"foo\" is now " +
                                   (menu112.isVisible() ? "" : "not") + " visible.");
            }
        });
        menu11.getItems().addAll(menu111, menu112, menu113);

        MenuItem menu131 = new MenuItem("Item _1");
        menu131.setOnAction(actionHandler);
        MenuItem menu132 = new MenuItem("Item _2");
        menu132.setOnAction(actionHandler);
        menu13.getItems().addAll(menu131, menu132);

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

        InvalidationListener selectedListener = new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                MenuItem mi = (MenuItem)((BooleanProperty)valueModel).getBean();
                boolean selected = ((BooleanProperty)valueModel).get();
                System.err.println(mi.getText() + " - " + selected);
            }
        };

        checkMI1.selectedProperty().addListener(selectedListener);
        checkMI2.selectedProperty().addListener(selectedListener);
        radioMI1.selectedProperty().addListener(selectedListener);
        radioMI2.selectedProperty().addListener(selectedListener);

        menu3.getItems().addAll(checkMI1, checkMI2, radioMI1, radioMI2);
        menuBar.getMenus().add(menu1);
        menuBar.getMenus().add(menu2);
        menuBar.getMenus().add(menu3);
        
        if (os != null && os.startsWith("Mac")) {
            Menu systemMenuBarMenu = makeMenu("MenuBar _Options");

            final CheckMenuItem useSystemMenuBarCB =
                new CheckMenuItem("Use _System Menu Bar");
            useSystemMenuBarCB.setSelected(true);
            menuBar.useSystemMenuBarProperty().
                bind(useSystemMenuBarCB.selectedProperty());
            systemMenuBarMenu.getItems().add(useSystemMenuBarCB);

            menuBar.getMenus().add(systemMenuBarMenu);
        }

        Label l = new Label("Menus Demo");
        Button btn = new Button("Back");
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                globalScene.setRoot(mainBox);
            }
        });
        vb.getChildren().addAll(l,menuBar);

        // Combo-Boxes testing
        VBox buttonsVBox1 = new VBox(10);
        buttonsVBox1.setPadding(new Insets(0, 0, 10, 10));

        ComboBox shortComboBox = new ComboBox();
        shortComboBox.setItems(FXCollections.
                               observableArrayList(strings.subList(0, 4)));
        buttonsVBox1.getChildren().add(shortComboBox);
        
        ComboBox longComboBox = new ComboBox();
        longComboBox.setPromptText("Make a choice...");
        longComboBox.setItems(strings);
        buttonsVBox1.getChildren().add(longComboBox);
        
        ComboBox fontComboBox = new ComboBox();
        fontComboBox.setItems(fonts);
        fontComboBox.setCellFactory(new Callback<ListView<String>,
                                    ListCell<String>>() {
            @Override public ListCell<String> call(ListView<String> param) {
                final ListCell<String> cell = new ListCell<String>() {
                    @Override public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            setText(item);
                            setFont(new Font(item, 14));
                        }
                    }
                };
                return cell;
            }
        });
        buttonsVBox1.getChildren().add(fontComboBox);
        
        ComboBox comboBox2 = new ComboBox();
        comboBox2.setId("first-editable");
        comboBox2.setItems(FXCollections.observableArrayList(strings.subList(0, 4)));
        comboBox2.setEditable(true);
        buttonsVBox1.getChildren().add(comboBox2);
        
        ComboBox<String> comboBox3 = new ComboBox<String>();
        comboBox3.setId("second-editable");
        comboBox3.setPromptText("Make a choice...");
        comboBox3.setItems(strings);
        comboBox3.setEditable(true);
        buttonsVBox1.getChildren().add(comboBox3);
        comboBox3.valueProperty().addListener(new ChangeListener<String>() {
            @Override public void changed(ObservableValue ov,
                                          String t, String t1) {
                System.out.println("new value: " + t1);
            }
        });
        
        ComboBox editFontComboBox = new ComboBox();
        editFontComboBox.setId("third-editable");
        editFontComboBox.setItems(fonts);
        editFontComboBox.setEditable(true);
        editFontComboBox.setCellFactory(new Callback<ListView<String>,
                                        ListCell<String>>() {
            @Override public ListCell<String> call(ListView<String> param) {
                final ListCell<String> cell = new ListCell<String>() {
                    @Override public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            setText(item);
                            setFont(new Font(item, 14));
                        }
                    }
                };
                return cell;
            }
        });
        buttonsVBox1.getChildren().add(editFontComboBox);        

        Button OpenTabbtn = new Button("Open Tab Pane");
        OpenTabbtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                openTabbedPane(globalScene, mainBox, mainstage);
            }
        });
        buttonsVBox1.getChildren().add(OpenTabbtn);

        Button Backbtn = new Button("Back");
        Backbtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                globalScene.setRoot(mainBox);
            }
        });

        //Menu-Buttons testing 
        VBox buttonsVBox2 = new VBox(10);
        int y = 10;
        Button simpButton = new Button("Simple Button");
        simpButton.setTooltip(new Tooltip("Tooltip for Simple Button"));
        simpButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                System.out.println("Simple Button");
            }
         });
        simpButton.setLayoutX(column_enabled);
        simpButton.setLayoutY(y);
        buttonsVBox2.getChildren().add(simpButton);

        simpButton = new Button("Simple Button");
        simpButton.setTooltip(new Tooltip("Tooltip for Simple Button"));
        simpButton.setDisable(true);
        simpButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                System.out.println("Simple Button");
            }
         });
        simpButton.setLayoutX(column_disabled);
        simpButton.setLayoutY(y);
        buttonsVBox2.getChildren().add(simpButton);

        y += line_spacing;

        MenuButton mb = new MenuButton("MenuButton1");
        mb.setTooltip(new Tooltip("Tooltip for MenuButton1"));
        mb.setLayoutX(column_enabled);
        mb.setLayoutY(y);

        final MenuItem coke = new MenuItem("Coke");
        coke.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                System.out.println(coke.getText());
            }
         });
        mb.getItems().add(coke);

        final MenuItem pepsi = new MenuItem("Pepsi");
        pepsi.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                System.out.println(pepsi.getText());
            }
         });
        mb.getItems().add(pepsi);
        mb.getItems().addAll(new MenuItem("Foo"),
                             new MenuItem("Foo"),
                             new MenuItem("Foo"),
                             new MenuItem("Foo"),
                             new MenuItem("Foo"),
                             new MenuItem("Foo"),
                             new MenuItem("Foo"),
                             new MenuItem("Foo"),
                             new MenuItem("Foo"),
                             new MenuItem("Foo"),
                             new MenuItem("Foo"),
                             new MenuItem("Foo"));
        buttonsVBox2.getChildren().add(mb);

        mb = new MenuButton("MenuButton2");
        mb.setTooltip(new Tooltip("Tooltip for MenuButton2"));
        mb.setPopupSide(Side.RIGHT);
        mb.setLayoutX(column_enabled);
        mb.setLayoutY(y);

        final MenuItem burger = new MenuItem("Burger");
        burger.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                System.out.println(burger.getText());
            }
         });
        mb.getItems().add(burger);

        final MenuItem hotDog = new MenuItem("Hot Dog");
        hotDog.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                System.out.println(hotDog.getText());
            }
         });
        mb.getItems().add(hotDog);
        buttonsVBox2.getChildren().add(mb);

        y += line_spacing;

        SplitMenuButton smb = new SplitMenuButton();
        smb.setText("SplitMenuButton1");
        smb.setTooltip(new Tooltip("Tooltip for SplitMenuButton1"));
        smb.setLayoutX(column_enabled);
        smb.setLayoutY(y);
        smb.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                System.out.println("SplitMenuButton1");
            }
        });

        MenuItem mi = new MenuItem("Divide");
        mi.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                System.out.println("Divide");
            }
         });
        smb.getItems().add(mi);

        mi = new MenuItem("Conquer");
        mi.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                System.out.println("Conquer");
            }
         });
        smb.getItems().add(mi);
        buttonsVBox2.getChildren().add(smb);
        y += line_spacing;

        final SplitMenuButton smb3 = new SplitMenuButton();
        smb3.setTooltip(new Tooltip("Tooltip for SplitMenuButton2"));
        smb3.setPopupSide(Side.RIGHT);
        smb3.setLayoutX(column_enabled);
        smb3.setLayoutY(y);
        smb3.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                System.out.println("SplitMenuButton2");
            }
        });

        {
            final MenuItem menuItem = new MenuItem("Land");
            menuItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent e) {
                    System.out.println("Land");
                    smb3.setText(menuItem.getText());
                    smb3.setOnAction(menuItem.getOnAction());
                }
             });
            smb3.getItems().add(menuItem);
        }

        {
            final MenuItem menuItem = new MenuItem("Sea");
            menuItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent e) {
                    System.out.println("Sea");
                    smb3.setText(menuItem.getText());
                    smb3.setOnAction(menuItem.getOnAction());
                }
             });
            smb3.getItems().add(menuItem);
        }

        smb3.setText(smb3.getItems().get(0).getText());
        smb3.setOnAction(smb3.getItems().get(0).getOnAction());
        buttonsVBox2.getChildren().add(smb3);        

        HBox allButtons = new HBox(100);
        allButtons.getChildren().addAll(buttonsVBox1,buttonsVBox2);

        vb.getChildren().addAll(new Label(" "), allButtons, Backbtn);
        globalScene.setRoot(vb);
        
    }
    
    private EventHandler showHideHandler = new EventHandler<Event>() {
        public void handle(Event t) {
            Menu menu = (Menu)t.getSource();
            if (t.getEventType() == Menu.ON_SHOWING && 
                    menu.getText().equals("_Submenu")) {
                Date date = new Date();
                String time = new SimpleDateFormat("HH:mm:ss").format(date);
                menu.getItems().get(0).setText("The time is " + time);
            }
            if (showMessagesItem.isSelected()) {
                System.out.println(((Menu)t.getSource()).getText()
                                            + " " + t.getEventType());
            }
        }
    };

    private Menu makeMenu(String text) {
        return makeMenu(text, null);
    }

    private Menu makeMenu(String text, Node graphic) {
        Menu menu = new Menu(text, graphic);
//        menu.setOnShowing(showHideHandler);
//        menu.setOnShown(showHideHandler);
//        menu.setOnHiding(showHideHandler);
//        menu.setOnHidden(showHideHandler);
        return menu;
    }

    /**
     * The method open new window with Tabbed Pane for manual
     * testing 
     * @param globalScene the global Scene
     * @param mainBox the Box to insert into
     * @param prevStage the previous stage to use
     */
    public void openTabbedPane(final Scene globalScene, final VBox mainBox,
                               final Stage prevStage) {
    
        prevStage.close();
        final Stage TabStage = new Stage();
        TabStage.setX(10);
        TabStage.setY(10);

        tabPane = new TabPane();
        tab1 = new Tab();
        tab2 = new Tab();
        tab3 = new Tab();
        emptyTab = new Tab();
        internalTab = new Tab();
        multipleTabs = new Tab();
        setUpPopupMenu();
        TabStage.setTitle("Hello TabPane2");
        final Scene scene = new Scene(new Group(), 1000, 580);
        scene.setFill(Color.GHOSTWHITE);

        tabPane.setRotateGraphic(false);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);
        tabPane.setSide(Side.TOP);        

        {
            tab1.setText("Tab 1");
            tab1.setTooltip(new Tooltip("Tab 1 Tooltip"));
            final Image image = new Image("hello/about_16.png");
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

                Button btnBack = new Button("Back");
                btnBack.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent e) {
                        TabStage.close();
                        prevStage.show();
                    }
                });
                vbox.getChildren().add(btnBack);

            tab1.setContent(vbox);
            tabPane.getTabs().add(tab1);
        }
        {
            tab2.setText("Longer Tab");
            final Image image = new Image("hello/folder_16.png");
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
                        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.
                                                    valueOf(button.getText()));
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
            final Image image = new Image("hello/heart_16.png");
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
                            image = new Image("hello/about_48.png");
                        } else {
                            image = new Image("hello/about_16.png");
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
                            image = new Image("hello/folder_48.png");
                        } else {
                            image = new Image("hello/folder_16.png");
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
                            image = new Image("hello/heart_48.png");
                        } else {
                            image = new Image("hello/heart_16.png");
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
        {   Tab tab = new Tab();
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
        TabStage.setScene(scene);
        TabStage.show();
    }

    //additional methods for TabPane testing
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
            tab.setContent(stack);                    
            internalTabPane.getTabs().add(tab);
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
            tab.setContent(stack);                    
            internalTabPane.getTabs().add(tab);
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

    /**
     * The method updates globalScene with buttons that 
     * opens/manipulates windows
     * @param globalScene the global Scene
     * @param mainBox the Box to insert into
     * @param WindowsStage WindowStage used
     */
    public void windowsTest(final Scene globalScene, final VBox mainBox,
                            final Stage WindowsStage) {

        VBox vb = new VBox(10);
        vb.setPadding(new Insets(13, 13, 13, 13));

        Label l = new Label("Windows Demo");

        Button PopUpBtn = new Button("Pop-Up Test");
        final Popup popup = new Popup(); 
        popup.setX(WindowsStage.getX()+300);
        popup.setY(WindowsStage.getY()+150);
        popup.setAutoHide(true);
        Rectangle inRectangle = new Rectangle(215, 215);
        inRectangle.setFill(Color.LIGHTGREEN);
        popup.getContent().addAll(inRectangle,new TextField("Insert Text"));
        PopUpBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                popup.show(WindowsStage);
            }
        });

        final Stage stage1 = new Stage();
        final Group SmallGroup = new Group();
        final Button setFull = new Button("Toggle Fullscreen");
        setFull.setFont(new Font(18));
        final Button setMini = new Button("Minimize");
        setMini.setFont(new Font(18));
        setMini.setDisable(true);
        final Rectangle rect = new Rectangle(2000,2000);
        rect.setFill(Color.LIGHTBLUE);
        SmallGroup.getChildren().addAll(rect, setFull);

        setFull.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                if (stage1.isFullScreen()) {
                    setFull.setText("Toggle Fullscreen");
                } else {
                    setFull.setText("Toggle Normal");
                }
                stage1.setFullScreen(!stage1.isFullScreen());
            }
        });
        
        setMini.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                if (stage1.isShowing()) {
                    if (stage1.isIconified()) {
                        setMini.setText("Minimize");
                    } else {
                        setMini.setText("Back from Minimize");
                    }
                    stage1.setIconified(!stage1.isIconified());
                }
            }
        });

        final Scene s1 = new Scene(SmallGroup, 200, 200);
        Button winBtn = new Button("Open new window");
        final Button resVerBtn = new Button("Resize vertically");
        final Button resHorBtn = new Button("Resize horizontally");
        final Button resDiaBtn = new Button("Resize diagonally");

        winBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                if (stage1.isShowing()) {
                    stage1.close();
                    setMini.setDisable(true);
                    resVerBtn.setDisable(true);
                    resHorBtn.setDisable(true);
                    resDiaBtn.setDisable(true);
                    stage1.setHeight(200);
                    stage1.setWidth(200);
                }
                stage1.setScene(s1);
                stage1.setX(WindowsStage.getX()+300);
                stage1.setY(WindowsStage.getY()+150);
                stage1.show();
                setMini.setDisable(false);
                resVerBtn.setDisable(false);
                resHorBtn.setDisable(false);
                resDiaBtn.setDisable(false);
            }
        });

        resVerBtn.setDisable(true);
        resVerBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                if (stage1.isShowing()) {
                    stage1.setHeight(s1.getHeight() + 50);
                }
            }
        });

        resHorBtn.setDisable(true);
        resHorBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                if (stage1.isShowing()) {
                    stage1.setWidth(s1.getWidth() + 50);
                }
            }
        });

        resDiaBtn.setDisable(true);
        resDiaBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                if (stage1.isShowing()) {
                    stage1.setHeight(s1.getHeight() + 50);
                    stage1.setWidth(s1.getWidth() + 50);
                }
            }
        });

        Button modBtn = new Button("Modality Test");
        modBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                if (stage1.isShowing()){
                    stage1.close();
                    setMini.setDisable(true);
                    resVerBtn.setDisable(true);
                    resHorBtn.setDisable(true);
                    resDiaBtn.setDisable(true);
                }
                final Stage stage2 = new Stage();
                stage2.setX(WindowsStage.getX()+300);
                stage2.setY(WindowsStage.getY()+150);
                Modality modality = Modality.APPLICATION_MODAL;
                stage2.initModality(modality);
                Label l = new Label("  Only this window should be active!");
                Button BackBtn = new Button("Cancel");
                BackBtn.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent e) { 
                        stage2.close();
                    } });
                VBox box = new VBox(10);
                box.setAlignment(Pos.CENTER);
                box.getChildren().addAll(l,BackBtn);
                Group smallGroup = new Group();
                Rectangle rec = new Rectangle(400, 250);
                rec.setFill(Color.LIGHTPINK);
                smallGroup.getChildren().addAll(rec, box);
                Scene s = new Scene(smallGroup);
                s.getStylesheets().add("hello/HelloSanityStyles.css");
                stage2.setScene(s);
                stage2.show();
            }
        });

        Button BackBtn = new Button("Back");
        BackBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                if (stage1.isShowing()) {
                    stage1.close();
                    stage1.setHeight(200);
                    stage1.setWidth(200);
                }
                globalScene.setRoot(mainBox);
            }
        });

        vb.getChildren().addAll(l, PopUpBtn, winBtn, resVerBtn, resHorBtn,
                                resDiaBtn, setMini, modBtn, BackBtn);
        globalScene.setRoot(vb);
    }

    /**
     * The method updates globalScene with animation that should be 
     * shown correctly
     * @param globalScene the global Scene
     * @param mainBox the Box to insert into
     */
    public void animationTest(final Scene globalScene, final VBox mainBox){

        Label l = new Label("Animation Demo");
        Button btn = new Button("Back");
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                globalScene.setRoot(mainBox);
            }
        });

        final Image image = new Image("hello/car.png");
        final Label car = new Label("",new ImageView(image));

        VBox vb = new VBox(30);
        vb.setAlignment(Pos.CENTER);
        vb.getChildren().addAll( l, car, btn);        
        Path path = new Path();
        path.getElements().add(new MoveTo(20,20));
        path.getElements().add(new CubicCurveTo(380, 0, 380, 120, 200, 120));
        path.getElements().add(new CubicCurveTo(0, 120, 0, 240, 380, 240));
        PathTransition pathTransition = new PathTransition();
        pathTransition.setDuration(Duration.millis(4000));
        pathTransition.setPath(path);
        pathTransition.setNode(car);
        pathTransition.setOrientation(PathTransition.OrientationType.
                                      ORTHOGONAL_TO_TANGENT);
        pathTransition.setCycleCount(Timeline.INDEFINITE);
        pathTransition.setAutoReverse(true);
        pathTransition.play();
        globalScene.setRoot(vb);
    }

    /**
     * The method updates globalScene with few types for effects 
     * that should be shown correctly 
     * @param globalScene the global Scene
     * @param mainBox the Box to insert into
     */
    public void effectsTest(final Scene globalScene, final VBox mainBox){

        
        Label l = new Label("Effects Demo");
        Button btn = new Button("Back");
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                globalScene.setRoot(mainBox);
            }
        });

        GridPane grid = new GridPane();

        grid.setHgap(30);
        grid.setAlignment(Pos.CENTER);

        Text title1 = new Text("            Raw image\n    ");
        grid.setConstraints(title1, 0, 0);
        grid.getChildren().add(title1);

        Text title2 = new Text("         Image with effect\n     ");
        grid.setConstraints(title2, 1, 0);
        grid.getChildren().add(title2);

        Text title3 = new Text("         Expected Result\n     ");
        grid.setConstraints(title3, 2, 0);
        grid.getChildren().add(title3);

        Text text1 = new Text();
        text1.setText("Blurry Text!");
        text1.setFill(Color.web("0x3b596d"));
        text1.setFont(Font.font(null, FontWeight.BOLD, 35));
        grid.setConstraints(text1, 0, 1);
        grid.getChildren().add(text1);

        Text text2 = new Text();
        text2.setText("Blurry Text!");
        text2.setFill(Color.web("0x3b596d"));
        text2.setFont(Font.font(null, FontWeight.BOLD, 35));
        text2.setEffect(new GaussianBlur());
        grid.setConstraints(text2, 1, 1);
        grid.getChildren().add(text2);

        Image blur = new Image("hello/BlurryText.png");
        Label expImg = new Label("",new ImageView(blur));
        expImg.resize(100,20);
        grid.setConstraints(expImg, 2, 1);
        grid.getChildren().add(expImg);

        Image dukeImage = new Image("hello/duke_with_guitar.png",
                                    200, 170, false, false);
        Label duke = new Label("",new ImageView(dukeImage));
        grid.setConstraints(duke, 0, 2);
        grid.getChildren().add(duke);

        Label dukeRef = new Label("",new ImageView(dukeImage));
        Reflection reflection = new Reflection();
        reflection.setFraction(0.7);
        dukeRef.setEffect(reflection);
        grid.setConstraints(dukeRef, 1, 2);
        grid.getChildren().add(dukeRef);

        Image dukeImage1 = new Image("hello/DukeReflectionUp.png",
                200, 170, false, false);
        Label duke1 = new Label("",new ImageView(dukeImage1));
        grid.setConstraints(duke1, 2, 2);
        grid.getChildren().add(duke1);

        Image dukeImage2 = new Image("hello/DukeReflectionDown.png",
                200, 170, false, false);
        Label duke2 = new Label("",new ImageView(dukeImage2));
        grid.setConstraints(duke2, 2, 3);
        grid.getChildren().add(duke2);

        duke1.setLayoutX(50);
        duke1.setLayoutY(50);
        
        VBox vb = new VBox(20);
        vb.setAlignment(Pos.CENTER);
        vb.getChildren().addAll( l, grid, btn);
        globalScene.setRoot(vb);        
    }

    /**
     * The method updates globalScene with robot tests
     * @param globalScene the global Scene
     * @param mainBox the Box to insert into
     * @param robotStage the Robot Stage
     */
    public void robotTest(final Scene globalScene, final VBox mainBox,
                          final Stage robotStage){
	
        Label l = new Label("Robot features Demo");
        Group lGroup = new Group(l);
        lGroup.setLayoutX(400);
        lGroup.setLayoutY(10);

    	//Rectangle's coordinates
        final int recX = 50;
        final int recY = 50;

        Group allGroup = new Group();
        rec1.setFill(Color.RED);
        Rectangle rec2 = new Rectangle(recX + 40, recY, 40, 160);
        rec2.setFill(Color.BLUE);
        Rectangle rec3 = new Rectangle(recX + 80, recY, 40, 160);
        rec3.setFill(Color.YELLOW);
        Rectangle rec4 = new Rectangle(recX + 120, recY, 40, 160);
        rec4.setFill(Color.GREEN);

        GridPane grid = new GridPane();
        grid.setVgap(50);
        grid.setHgap(20);
        grid.setLayoutX(recX + 300);
        grid.setLayoutY(recY + 50);

        final TextField result1 = new TextField("Result");
        result1.setEditable(false);
        Button screenTestBtn = new Button("Robot Get Screen Capture Test");
        screenTestBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
               robotScreenTest(result1, robotStage);
            }
        });

        grid.setConstraints(screenTestBtn, 0, 0);
        grid.getChildren().add(screenTestBtn);
        grid.setConstraints(result1, 1, 0);
        grid.getChildren().add(result1);

        final TextField result2 = new TextField("Result");
        result2.setEditable(false);
        Button pixelTestBtn = new Button("Robot Get Pixel Color Test");
        pixelTestBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
               robotPixelTest(result2, robotStage);
            }
        });

        grid.setConstraints(pixelTestBtn, 0, 1);
        grid.getChildren().add(pixelTestBtn);
        grid.setConstraints(result2, 1, 1);
        grid.getChildren().add(result2);

        //KeyPressRelesase
        final TextField writeField = new TextField("");
        Group writeFieldGroup = new Group(writeField);
        writeFieldGroup.setLayoutX(recX);
        writeFieldGroup.setLayoutY(recY + 200);

        final TextField result3 = new TextField("Result");
        result3.setEditable(false);

        Button keyTestBtn = new Button("Robot Key Press/Release Test");
        keyTestBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                robotKeyTest(writeField, result3);
            }
        });

        grid.setConstraints(keyTestBtn, 0, 2);
        grid.getChildren().add(keyTestBtn);
        grid.setConstraints(result3, 1, 2);
        grid.getChildren().add(result3);

        //Mouse wheel
        final ListView<String> sv = new ListView<String>();
        ObservableList<String> items =FXCollections.observableArrayList (
                    "a", "b", "c", "d", "e", "f", "g", "h", "i");
        sv.setItems(items);
        sv.setPrefWidth(100);
        sv.setPrefHeight(100);

        Group svGroup = new Group(sv);
        svGroup.setLayoutX(recX);
        svGroup.setLayoutY(recY + 250);

        final TextField result4 = new TextField("Result");
        result4.setEditable(false);

        Button wheelTestBtn = new Button("Robot Mouse Press/Release/Wheel Test");
        wheelTestBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                robotWheelTest(sv, result4, robotStage);
            }
        });

        grid.setConstraints(wheelTestBtn, 0, 3);
        grid.getChildren().add(wheelTestBtn);
        grid.setConstraints(result4, 1, 3);
        grid.getChildren().add(result4);

        Button btn = new Button("Back");
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                globalScene.setRoot(mainBox);
            }
        });
        Group btnGroup = new Group(btn);
        btnGroup.setLayoutX(450);
        btnGroup.setLayoutY(450);

        allGroup.getChildren().addAll(rec1, rec2, rec3, rec4, grid, lGroup, btnGroup,
                                      writeFieldGroup, svGroup);
        globalScene.setRoot(allGroup);
    }


   public void robotKeyTest(final TextField field, final TextField result) {
		field.requestFocus();
		new AnimationTimer() {
			long startTime = System.nanoTime();
			@Override 
			public void handle(long now) {
				if (now > startTime + 3000000000l){ 
					stop(); 
					field.setText("Failed");
				} else if (field.isFocused()) {
					stop();
					Robot robot = com.sun.glass.ui.Application.GetApplication().createRobot();
					robot.keyPress(KeyEvent.VK_T);
					robot.keyRelease(KeyEvent.VK_T);
					robot.keyPress(KeyEvent.VK_E);
					robot.keyRelease(KeyEvent.VK_E);
					robot.keyPress(KeyEvent.VK_S);
					robot.keyRelease(KeyEvent.VK_S);
					robot.keyPress(KeyEvent.VK_T);
					robot.keyRelease(KeyEvent.VK_T);
					robot.destroy();
					new AnimationTimer() {
						long startTime = System.nanoTime();
						@Override
						public void handle(long now) {
							if (now > startTime + 3000000000l){ 
								stop();
								result.setText("Failed");
							} else if ((field.getText()).equals("test")) { 
								stop();
								result.setText("Passed");
							}
						}
					}.start();
				}
			}
		}.start();
	}

    public void robotWheelTest(final ListView<String> lv, final TextField result,
                                                            Stage currentStage){

		//Caclulation of ListView minimal coordinates
		Bounds bounds = lv.localToScreen(new BoundingBox(0, 0, 
	        lv.getBoundsInParent().getWidth(),
	        lv.getBoundsInParent().getHeight()));
		int x = 10 + (int) bounds.getMinX();
		int y = 10 + (int) bounds.getMinY();

		final Robot robot =
                    com.sun.glass.ui.Application.GetApplication().createRobot();
        robot.mouseMove(x, y);
        robot.mousePress(Robot.MOUSE_LEFT_BTN);
        robot.mouseRelease(Robot.MOUSE_LEFT_BTN);

		new AnimationTimer() {
			long startTime = System.nanoTime();
			@Override 
			public void handle(long now) {
				if (now > startTime + 3000000000l){ 
					stop(); 
					result.setText("Failed");
				} else if (lv.isFocused()) {
					stop();
					robot.mouseWheel(-5);
					robot.mousePress(Robot.MOUSE_LEFT_BTN);
                    robot.mouseRelease(Robot.MOUSE_LEFT_BTN);
                    robot.destroy();
					new AnimationTimer() {
						long startTime = System.nanoTime();
						@Override
						public void handle(long now) {
							if (now > startTime + 3000000000l){ 
								stop();
								result.setText("Scroll Down Failed");
							} else if (!lv.getSelectionModel().
                                    selectedItemProperty().getValue().
                                    equals("a")) {
								        stop();
								    result.setText("Scroll Down Passed");
							}
						}
					}.start();
				}
			}
		}.start();
	}

    public void robotPixelTest(final TextField result, Stage currentStage){

	Bounds bounds = rec1.localToScreen(new BoundingBox(0, 0, 
			rec1.getBoundsInParent().getWidth(),
                        rec1.getBoundsInParent().getHeight()));
	int x = 53 + (int) bounds.getMinX();
	int y = 53 + (int) bounds.getMinY();
	int answer = assertPixelEquals(x, y, Color.RED) +
                     assertPixelEquals(x + 40, y, Color.BLUE) +
                     assertPixelEquals(x + 80, y, Color.YELLOW) +
                     assertPixelEquals(x + 120, y, Color.GREEN);
    if (answer == 4) {
        result.setText("Passed");
    } else {
        result.setText("Failed");
    }

    }

    static int colorToRGB(Color c) {
        int r = (int) Math.round(c.getRed() * 255.0);
        int g = (int) Math.round(c.getGreen() * 255.0);
        int b = (int) Math.round(c.getBlue() * 255.0);
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    public int assertPixelEquals(int x, int y, Color expected){

        Robot robot = com.sun.glass.ui.Application.GetApplication().createRobot();
        int pixel = robot.getPixelColor(x, y);
        robot.destroy();
        int expectedPixel = colorToRGB(expected);
        if (pixel == expectedPixel) {
            System.out.println("Expected color been found, at (" + x + "," + y + ")");
            return 1;
        } else {
            System.out.println("Expected color 0x" + Integer.toHexString(expectedPixel) +
                    " at " + x + "," + y + " but found 0x" + Integer.toHexString(pixel));
        }
        return 0;
    }
       
    public void robotScreenTest(final TextField result, Stage stage){
	
		Bounds bounds = rec1.localToScreen(new BoundingBox(0, 0, 
            rec1.getBoundsInParent().getWidth(),
            rec1.getBoundsInParent().getHeight()));

		int x = 50 + (int) bounds.getMinX();
		int y = 50 + (int) bounds.getMinY();
		int []intArr = null;
        boolean correct = true;
        Robot robot = com.sun.glass.ui.Application.GetApplication().createRobot();
        int width = 160;
        int height = 160;
        final Buffer buff = robot.getScreenCapture(x, y, width, height).getPixels();
        if ((buff instanceof IntBuffer)&&(buff.hasArray())) {
            intArr =((IntBuffer) buff).array();
        }

        String filename= "scrCapture.bmp";
        File file = new File(filename);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            BMPOutputStream bmp = new BMPOutputStream(new FileOutputStream(filename), intArr, width, height);
        } catch (Exception e) {}


        for (int i = width; i <= height*(height-1); i += width) {
            for (int j = 1; j <= 38; j ++){
                if (intArr[j+i] != colorToRGB(Color.RED)){
                    correct = false;
                }
             }
            for (int j = 41; j <= 78; j ++){
                if (intArr[j+i] != colorToRGB(Color.BLUE)){
                    correct = false;
                }
             }
            for (int j = 81; j <= 118; j ++){
                if (intArr[j+i] != colorToRGB(Color.YELLOW)){
                    correct = false;
                }
             }
            for (int j = 121; j <= 158; j ++){
                if (intArr[j+i] != colorToRGB(Color.GREEN)){
                    correct = false;
                }
            }
        }
        robot.destroy();
        if (correct) {
            result.setText("Passed");
        } else {
            result.setText("Failed");
        }
        showImage(stage, width, height, result);
    }

    private void showImage(Stage stage, int width, int height, TextField tf) {

        int frame = 70;
        Rectangle rec = new Rectangle(width + frame, height + frame);
        FileInputStream os = null;
        final File file = new File("scrCapture.bmp");
        try {
            os = new FileInputStream(file);
        } catch (Exception e) {}

        final Popup popup = new Popup();
        ImageView iv = new ImageView(new Image(os));
        iv.setLayoutX(frame/2);
        iv.setLayoutY(frame/2);

        rec.setFill(Color.WHITE);
        rec.setStroke(Color.BLACK);
        Button exit = new Button("x");
        exit.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                if (file.exists()&&tf.getText().equals("Passed")) {
                    file.deleteOnExit();
                }
                popup.hide();
            }
        });
        exit.setLayoutX(width + frame/2);
        Pane popupPane = new Pane(rec, iv, exit);
        popup.setX(stage.getX() + 550);
        popup.setY(stage.getY() + 430);
        popup.getContent().addAll(popupPane);
        popup.show(stage);
    }

    /**
     * The method updates globalScene with rectangle that should be 
     * swiped (touch only)
     * @param globalScene the global Scene
     * @param mainBox the Box to insert into
     */
    public void swipeTest(final Scene globalScene, final VBox mainBox){

        final Rectangle rect;
        final boolean playing = false;

        Label l = new Label("Swipe demo");

        Button btn = new Button("Back");
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                globalScene.setRoot(mainBox);
            }
        });
        
        rect = new Rectangle(200, 200, 200, 200);
        rect.setFill(Color.RED);
        rect.setOnSwipeLeft(new EventHandler<SwipeEvent>() {
            @Override public void handle(SwipeEvent event) {
                rotate(-event.getTouchCount(), Rotate.Z_AXIS, rect, playing);
                event.consume();
            }
        });
        rect.setOnSwipeRight(new EventHandler<SwipeEvent>() {
            @Override public void handle(SwipeEvent event) {                
                rotate(event.getTouchCount(), Rotate.Z_AXIS, rect, playing);
                event.consume();
            }
        });
        rect.setOnSwipeUp(new EventHandler<SwipeEvent>() {
            @Override public void handle(SwipeEvent event) {
                yTranslate(event.getTouchCount(), 0f, -100f, rect, playing);
                event.consume();
            }
        });
        rect.setOnSwipeDown(new EventHandler<SwipeEvent>() {
            @Override public void handle(SwipeEvent event) {
                yTranslate(event.getTouchCount(), 0f, 100f, rect, playing);
                event.consume();
            }
        });
		VBox vb = new VBox(40);
        vb.setAlignment(Pos.CENTER);
        vb.getChildren().addAll( l, rect, btn);
        globalScene.setRoot(vb);
    }

    private void rotate(double count, Point3D axis, Rectangle localRect, boolean playing) {
        if (playing) {
            return;
        }
        playing = true;
        RotateTransition rt = new RotateTransition(
                Duration.millis(Math.abs(1000 * count)), localRect);
        rt.setAxis(axis);
        rt.setFromAngle(0);
        rt.setToAngle(count * 180);
        rt.play();
    }

    private void yTranslate(double count, double fromY, double toY, Rectangle localRect, boolean playing) {
        if (playing) {
            return;
        }
        playing = true;
        TranslateTransition tt = new TranslateTransition(
                Duration.millis(Math.abs(200 * count)), localRect);
        tt.setFromY(fromY);
        tt.setToY(toY);
        tt.setCycleCount(2);
        tt.setAutoReverse(true);
        tt.play();
    }   
}
