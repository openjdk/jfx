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

import java.util.Iterator;
import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCombination.ModifierValue;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

public class HelloWindowManager extends Application {
    private static double WIDTH = 300;
    private static double HEIGHT = 350;

    static boolean showMouse = false;

    private static KeyCharacterCombination ctrlC = new KeyCharacterCombination("c",
            ModifierValue.UP,
            ModifierValue.DOWN,
            ModifierValue.UP,
            ModifierValue.UP,
            ModifierValue.UP);

    enum ReshapingActions {
        LEFT, RIGHT, UP, DOWN,
        BIGGER, SMALLER,
        RAISE, LOWER,
        FULLSCREEN_TOGGLE, 
        CLOSE
    }

    static private class ReshapingHandler implements EventHandler<ActionEvent> {

        ReshapingActions action;
        Window window;

        ReshapingHandler(ReshapingActions action,Window window) {
            this.action = action;
            this.window = window;

        }

        @Override
        public void handle(ActionEvent t) {
            double x,y,w,h;

            double moveBy = 20;
            Stage s = null;
            Popup p = null;
            if (window instanceof javafx.stage.Stage) {
                s = (javafx.stage.Stage)window;
            } else if (window instanceof javafx.stage.Popup) {
                p = (javafx.stage.Popup)window;
            }

            x = window.getX();
            y = window.getY();
            w = window.getWidth();
            h = window.getHeight();

            System.out.println("Window starts at "+x+","+y+" "+w+"x"+h);

            switch(action) {
                case LEFT:
                    x = window.getX() - moveBy;
                    System.out.println("Moving X to "+x+" Y is "+window.getY());
                    window.setX(x);
                    break;
                case RIGHT:
                    x = window.getX() + moveBy;
                    window.setX(x);
                    break;
                case UP:
                    y = window.getY() - moveBy;
                    System.out.println("Moving Y to "+y);
                    window.setY(y);
                    break;
                case DOWN:
                    y = window.getY() + moveBy;
                    System.out.println("Moving Y to "+y);
                    window.setY(y);
                    break;
                case BIGGER:
                    w = window.getWidth() + moveBy;
                    h = window.getHeight() + moveBy;
                    if (s != null) {
                        s.setWidth(w);
                        s.setHeight(h);
                    } else {
                        window.setWidth(w);
                        window.setHeight(h);
                    }
                    break;
                case SMALLER:
                    w = Math.max(window.getWidth() - moveBy,WIDTH);
                    h = Math.max(window.getHeight() - moveBy,HEIGHT);
                    if (s != null) {
                        s.setWidth(w);
                        s.setHeight(h);
                    } else {
                        window.setWidth(w);
                        window.setHeight(h);
                    }
                    break;
                case FULLSCREEN_TOGGLE:
                        if (s != null) {
                            System.out.println("Setting FULLSCREEN TRUE");
                            System.out.println("TARGET is "+t.getTarget());
                            if (!s.isFullScreen()) {
                                s.setFullScreen(true);
                            } else {
                                s.setFullScreen(false);
                            }
                        }
                    break;
                case CLOSE:
                    System.out.println("Closing...");
                    s.close();
                    break;
                case RAISE:
                        if (s != null) {
                            System.out.println("toFront");
                            s.toFront();
                        }
                    break;
                case LOWER:
                        if (s != null) {
                            System.out.println("toBack");
                            s.toBack();
                        }
                    break;
            }
        }
        
    }


    @Override public void start(Stage stage) {
        createStage(stage);
        
        stage.show();
    }

    static int stageCount = 0;

    static void createStage(Stage stage) {
        String title;

        stageCount ++;

        if (stageCount == 1) {
            title = "Hello WindowManager";

        } else {
            title = "Hello WindowManager " +stageCount;
        }

        final int iter = stageCount;
        final String name = "Stage"+iter;
        final String eventTag = "#"+name;

        stage.setTitle(title);
        stage.setWidth(WIDTH);
        stage.setHeight(HEIGHT);

        EventHandler showHideHandler = new EventHandler<Event>() {
            @Override
            public void handle(Event t) {
                System.out.println(eventTag+" ShowHide " + t.getEventType());
            }
        };


        Scene scene = createScene(new PopupPlacement(0, 500 - (stageCount -1)*200),stage,name);

        stage.setResizable(true);

        stage.addEventHandler(KeyEvent.ANY, new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent ke) {
                System.out.println(eventTag+" " + ke);
            }
        });

        stage.setOnShowing(showHideHandler);
        stage.setOnShown(showHideHandler);
        stage.setOnHiding(showHideHandler);
        stage.setOnHidden(showHideHandler);

        stage.onShowingProperty().addListener(new ChangeListener<EventHandler<WindowEvent>>() {

            @Override
            public void changed(ObservableValue<? extends EventHandler<WindowEvent>> ov, EventHandler<WindowEvent> t, EventHandler<WindowEvent> t1) {
                System.out.println(eventTag+t);
            }
        });

        stage.focusedProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                System.out.println(eventTag+" Focused changed to "+newValue);
                
            }

        });

        stage.setScene(scene);

    }

    private static Group createRootGroup(PopupPlacement popupPlacement,
            final String name,
            final Window window) {

        Group rootGroup = new Group();
        ObservableList<Node> content = rootGroup.getChildren();

        final String eventTag = ">"+name;

        final Rectangle rect = new Rectangle();
        rect.setX(0);
        rect.setY(0);
        rect.sceneProperty().addListener(new ChangeListener<Scene>() {
            @Override public void changed(ObservableValue<? extends Scene> ov,
                    Scene oldScene, Scene newScene) {
                rect.widthProperty().bind(newScene.widthProperty());
                rect.heightProperty().bind(newScene.heightProperty());
            }
        });
        Rectangle rectInner = new Rectangle();
        rectInner.setX(1);
        rectInner.setY(1);
        rectInner.widthProperty().bind(rect.widthProperty().subtract(2));
        rectInner.heightProperty().bind(rect.heightProperty().subtract(2));
        rectInner.setFill(Color.GRAY);
        rectInner.setStroke(Color.RED);
        rectInner.setStrokeWidth(2);
        content.add(rect);
        content.add(rectInner);
        rectInner.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent me) {
                if (showMouse)
                    System.out.println(eventTag+" " + me);
            }
        });
        rectInner.addEventHandler(ScrollEvent.ANY, new EventHandler<ScrollEvent>() {

            @Override
            public void handle(ScrollEvent se) {
                if (showMouse)
                    System.out.println(eventTag+" " + se);
            }
        });

        final Label nameLabel = new Label(name);
        nameLabel.setLayoutX(100);
        nameLabel.setLayoutY(10);
        content.add(nameLabel);

        int line = 30;

        final Button showButton = new Button("Popup");
        showButton.setLayoutX(50);
        showButton.setLayoutY(line);
        showButton.setOnAction(new PopupActionHandler(popupPlacement,
                                                      showButton));
        content.add(showButton);

        final Button stageButton = new Button("Stage");
        stageButton.setLayoutX(150);
        stageButton.setLayoutY(line);
        stageButton.setOnAction(new StageActionHandler());
        content.add(stageButton);


        // -------------------- Move Buttons -----------------------
        line += 30;

        final Button upButton = new Button("Up");
        upButton.setOnAction(new ReshapingHandler(ReshapingActions.UP, window));

        final Button leftButton = new Button("Left");
        leftButton.setOnAction(new ReshapingHandler(ReshapingActions.LEFT, window));


        final Button rightButton = new Button("Right");
        rightButton.setOnAction(new ReshapingHandler(ReshapingActions.RIGHT, window));


        final Button downButton = new Button("Down");
        downButton.setLayoutX(200);
        downButton.setLayoutY(line);

        downButton.setOnAction(new ReshapingHandler(ReshapingActions.DOWN, window));

        HBox movebox = new HBox();
        movebox.getChildren().addAll(upButton, leftButton, rightButton, downButton);
        movebox.setLayoutX(50);
        movebox.setLayoutY(line);
        content.add(movebox);

        // -------------------- Size Buttons -----------------------

        line += 30;

        final Button biggerButton = new Button("Bigger");
        biggerButton.setLayoutX(50);
        biggerButton.setLayoutY(line);

        biggerButton.setOnAction(new ReshapingHandler(ReshapingActions.BIGGER, window));
        content.add(biggerButton);

        final Button smallerButton = new Button("Smaller");
        smallerButton.setLayoutX(150);
        smallerButton.setLayoutY(line);

        smallerButton.setOnAction(new ReshapingHandler(ReshapingActions.SMALLER, window));
        content.add(smallerButton);

        line += 30;

        if (window instanceof javafx.stage.Stage) {

            final Button fsButton = new Button("Full Screen");
            fsButton.setLayoutX(50);
            fsButton.setLayoutY(line);

            fsButton.setOnAction(new ReshapingHandler(ReshapingActions.FULLSCREEN_TOGGLE, window));
            content.add(fsButton);
            
            ((Stage)window).fullScreenProperty().addListener(new ChangeListener<Boolean>() {

                @Override
                public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
                    System.out.println("FULLSCREEN now +"+newValue);
                    fsButton.setText(newValue ? "Normal": "Full Screen");
                }

            });

            final Button normalButton = new Button("Close");
            normalButton.setLayoutX(150);
            normalButton.setLayoutY(line);

            normalButton.setOnAction(new ReshapingHandler(ReshapingActions.CLOSE, window));
            content.add(normalButton);

            line += 30;

            final Button raiseButton = new Button("Raise");
            raiseButton.setLayoutX(50);
            raiseButton.setLayoutY(line);

            raiseButton.setOnAction(new ReshapingHandler(ReshapingActions.RAISE, window));
            content.add(raiseButton);

            final Button lowerButton = new Button("Lower");
            lowerButton.setLayoutX(150);
            lowerButton.setLayoutY(line);

            lowerButton.setOnAction(new ReshapingHandler(ReshapingActions.LOWER, window));
            content.add(lowerButton);

        }


        line += 30;

        final TextField textbox = new TextField();
        textbox.setLayoutX(50);
        textbox.setLayoutY(line);
        content.add(textbox);

        line += 30;

        final Button mouseButton = new Button("Mouse Output");
        mouseButton.setLayoutX(50);
        mouseButton.setLayoutY(line);
        mouseButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                showMouse = !showMouse;
            }
        });
        content.add(mouseButton);

        if (window instanceof javafx.stage.Stage) {
            line += 30;

            VBox escbox = new VBox();

            final ToggleGroup escgroup = new ToggleGroup();

            final RadioButton escrb1 = new RadioButton("Default-ESC");
            escrb1.setToggleGroup(escgroup);
            escrb1.setSelected(true);

            final RadioButton escrb2 = new RadioButton("Ctrl-C");
            escrb2.setToggleGroup(escgroup);

            final RadioButton escrb3 = new RadioButton("NO-MATCH");
            escrb3.setToggleGroup(escgroup);

            escbox.getChildren().add(escrb1);
            escbox.getChildren().add(escrb2);
            escbox.getChildren().add(escrb3);

            escgroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
                @Override
                public void changed(ObservableValue<? extends Toggle> ov,
                        Toggle old_toggle, Toggle new_toggle) {
                    System.out.print("ESC CHANGED: ");
                    Toggle selected = escgroup.getSelectedToggle();
                    if (selected != null) {
                        Stage s = (Stage)window;
                        if (selected.equals(escrb1)) {
                            System.out.println("DEFAULT");
                            s.setFullScreenExitKeyCombination(null);
                        } else if (selected.equals(escrb2)) {
                            s.setFullScreenExitKeyCombination(ctrlC);
                            System.out.println("CTRL-C");
                        }else if (selected.equals(escrb3)) {
                            s.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
                            System.out.println("NO_MATCH");
                        } else {
                            System.out.println("NO MATCH FOR ESC KEY TOGGLE?");
                        }
                    }
                }
            });

            VBox hintbox = new VBox();

            final ToggleGroup hintgroup = new ToggleGroup();

            final RadioButton hintrb1 = new RadioButton("Default-hint");
            hintrb1.setToggleGroup(hintgroup);
            hintrb1.setSelected(true);

            final RadioButton hintrb2 = new RadioButton("HI THERE!");
            hintrb2.setToggleGroup(hintgroup);

            final RadioButton hintrb3 = new RadioButton("Disable empty str");
            hintrb3.setToggleGroup(hintgroup);

            hintbox.getChildren().add(hintrb1);
            hintbox.getChildren().add(hintrb2);
            hintbox.getChildren().add(hintrb3);

            hintgroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
                @Override
                public void changed(ObservableValue<? extends Toggle> ov,
                        Toggle old_toggle, Toggle new_toggle) {
                    Toggle selected = hintgroup.getSelectedToggle();
                    System.out.print("HINT CHANGED:");
                    if (selected != null) {
                        Stage s = (Stage)window;
                        if (selected.equals(hintrb1)) {
                            System.out.println("DEFAULT");
                            s.setFullScreenExitHint(null);
                        } else if (selected.equals(hintrb2)) {
                            System.out.println("HI WORLD");
                            s.setFullScreenExitHint("HI WORLD");
                        }else if (selected.equals(hintrb3)) {
                            System.out.println("Disable empty string");
                            s.setFullScreenExitHint("");
                        } else {
                            System.out.println("NO MATCH FOR ESC HINT TOGGLE?");
                        }
                    }
                }
            });

            if (true) {
            HBox fsbox = new HBox();
            fsbox.getChildren().addAll(escbox,hintbox);

            fsbox.setLayoutX(30);
            fsbox.setLayoutY(line);
            content.add(fsbox);
            } else {
                escbox.setLayoutX(30);
                escbox.setLayoutY(line);

                content.add(escbox);

                hintbox.setLayoutX(60);
                hintbox.setLayoutY(line);

                content.add(hintbox);

            }

        }


        return rootGroup;
    }

    private static Scene createScene(PopupPlacement popupPlacement, Window w, final String name) {
        Scene scene = new Scene(createRootGroup(popupPlacement,name,w));
        return scene;
    }

    static int popupID = 0;

    private static Popup createPopup(PopupPlacement popupPlacement) {
        Popup popup = new Popup();

        popupID++;
        final String name = "Popup"+Integer.toString(popupID);
        final String eventTag = "#"+name;

        EventHandler showHideHandler = new EventHandler<Event>() {
            @Override
            public void handle(Event t) {
                System.out.println(eventTag+" ShowHide " + t.getEventType());
            }
        };

        popup.setOnShowing(showHideHandler);
        popup.setOnShown(showHideHandler);
        popup.setOnHiding(showHideHandler);
        popup.setOnHidden(showHideHandler);

        popup.addEventHandler(KeyEvent.ANY, new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent ke) {
                System.out.println(eventTag+" " + ke);
            }
        });

       popup.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent me) {
                System.out.println(eventTag+" " + me);
            }
        });

        popup.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                System.out.println(eventTag+" Focused changed to "+newValue);
                
            }

        });

        popup.getContent().add(createRootGroup(popupPlacement,name,popup));
        popup.setAutoHide(true);

        return popup;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }


    private static final class StageActionHandler
            implements EventHandler<ActionEvent> {

        @Override
        public void handle(ActionEvent t) {
            Stage stage = new Stage();

            createStage(stage);

            stage.show();
        }

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
