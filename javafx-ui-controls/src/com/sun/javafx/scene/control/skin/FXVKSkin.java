/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.skin;

import java.lang.reflect.Field;
import java.util.*;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.geometry.Pos;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Popup;


import com.sun.javafx.robot.impl.FXRobotHelper;
import com.sun.javafx.robot.impl.FXRobotHelper.FXRobotInputAccessor;
import com.sun.javafx.scene.control.behavior.BehaviorBase;

import static javafx.scene.input.KeyCode.*;

public class FXVKSkin extends SkinBase<FXVK, BehaviorBase<FXVK>> {

    private static Popup secondaryPopup;
    private static FXVK primaryVK;
    private static FXVK secondaryVK;
    private static Popup vkPopup;
    private Node attachedNode;
//     private Scene scene;

    FXVK fxvk;
    Control[][] keyRows;

    enum State { NORMAL, SHIFTED, SHIFT_LOCK, NUMERIC; };

    State state = State.NORMAL;

    static double KEY_WIDTH = 40;
    static double KEY_HEIGHT = 30;
    final static double hGap = 2;
    final static double vGap = 3;

    private ShiftKey shiftKey;
    private SymbolKey symbolKey;

    public FXVKSkin(final FXVK fxvk) {
        super(fxvk, new BehaviorBase<FXVK>(fxvk));
        this.fxvk = fxvk;

        createKeys();

        fxvk.attachedNodeProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                Node oldNode = attachedNode;
                attachedNode = fxvk.getAttachedNode();

                if (oldNode != null) {
                    oldNode.getScene().getRoot().setTranslateY(0);
                }


                if (attachedNode != null) {
                    final Scene scene = attachedNode.getScene();

                    if (vkPopup == null) {
                        vkPopup = new Popup();
                        vkPopup.getContent().add(fxvk);
                    }

                    if (vkPopup.isShowing()) {
                        Point2D nodePoint = com.sun.javafx.Utils.pointRelativeTo(attachedNode, vkPopup.getWidth(), vkPopup.getHeight(), HPos.CENTER, VPos.BOTTOM, 0, 2, true);
                        Point2D point = com.sun.javafx.Utils.pointRelativeTo(scene.getRoot(), vkPopup.getWidth(), vkPopup.getHeight(), HPos.CENTER, VPos.BOTTOM, 0, 0, true);
                        double y = point.getY() - fxvk.prefHeight(-1);
                        double nodeBottom = nodePoint.getY();
                        if (y < nodeBottom) {
                            scene.getRoot().setTranslateY(y - nodeBottom);
                        }
                    } else {
                        Platform.runLater(new Runnable() {
                            public void run() {
                                Point2D nodePoint = com.sun.javafx.Utils.pointRelativeTo(attachedNode, vkPopup.getWidth(), vkPopup.getHeight(), HPos.CENTER, VPos.BOTTOM, 0, 2, true);
                                Point2D point = com.sun.javafx.Utils.pointRelativeTo(scene.getRoot(), vkPopup.getWidth(), vkPopup.getHeight(), HPos.CENTER, VPos.BOTTOM, 0, 0, true);
                                double y = point.getY() - fxvk.prefHeight(-1);
                                vkPopup.show(attachedNode, point.getX(), y);


                                double nodeBottom = nodePoint.getY();
                                if (y < nodeBottom) {
                                    scene.getRoot().setTranslateY(y - nodeBottom);
                                }
                            }
                        });
                    }

                    if (oldNode == null || oldNode.getScene() != attachedNode.getScene()) {
                        fxvk.setPrefWidth(scene.getWidth());
                        fxvk.setMaxWidth(scene.getWidth());
                        fxvk.setPrefHeight(200);
                    }
                } else {
                    if (vkPopup != null) {
                        vkPopup.hide();
                    }
                    return;
                }
            }
        });
    }

    private void createKeys() {
        getChildren().clear();

        if (fxvk.chars != null) {
            keyRows = new Control[][] {
                makeKeyRow((Object[])fxvk.chars)
            };
        } else {
            keyRows = new Control[][] {
                makeKeyRow("q 1 [",
                           "w 2 ]",
                           "e 3 {",
                           "r 4 }",
                           "t 5 \\",
                           "y 6 |",
                           "u 7 \"",
                           "i 8 <",
                           "o 9 >",
                           "p 0 _"),
                makeKeyRow("a @ ~",
                           "s # `",
                           "d $ \u20ac",
                           "f % \u00a3",
                           "g ^ \u00a5",
                           "h & \u00a7",
                           "j * \u00b7",
                           "k ( \u00b0",
                           "l ) \u2260"),
                makeKeyRow(shiftKey = new ShiftKey(KEY_WIDTH * 1.5),
                           "z - \u00a1",
                           "x = \u00bf",
                           "c + \u2030",
                           "v ; \u00ae",
                           "b : \u2122",
                           "n / \u00ab",
                           "m ' \u00bb",
                           new CommandKey("\u232b", BACK_SPACE, KEY_WIDTH * 1.5)),
                makeKeyRow(symbolKey = new SymbolKey("!#123 ABC", KEY_WIDTH * 2.5 + (9-4) * hGap / 2),
                           ", !",
                           " ",
                           ". ?",
                           new CommandKey("\u21b5", ENTER, KEY_WIDTH * 2.5 + (9-4) * hGap / 2))
            };
        }

        VBox vbox = new VBox(vGap);
        vbox.setFillWidth(true);
        getChildren().add(vbox);

        for (Control[] row : keyRows) {
            HBox hbox = new HBox(hGap);
            hbox.setAlignment(Pos.CENTER);
            vbox.getChildren().add(hbox);
            for (Control key : row) {
                hbox.getChildren().add(key);
                HBox.setHgrow(key, Priority.ALWAYS);
            }
        }
    }


    private Control[] makeKeyRow(Object... obj) {
        List<Object> keyList = Arrays.asList((Object[])obj);
        return makeKeyRow(keyList);
    }

    private Control[] makeKeyRow(List<Object> keyList) {
        Control[] keyRow = new Control[keyList.size()];
        for (int i = 0; i < keyRow.length; i++) {
            if (keyList.get(i) instanceof String) {
                keyRow[i] = new Key((String)keyList.get(i));
            } else {
                keyRow[i] = (Control)keyList.get(i);
            }
        }
        return keyRow;
    }

    private void toggleShift() {
        State newState;
        switch (state) {
          case NORMAL:
            newState = State.SHIFTED;
            break;

          case SHIFTED:
            newState = State.SHIFT_LOCK;
            break;

          case SHIFT_LOCK:
          default:
            newState = State.NORMAL;
        }
        state = newState;

        updateLabels();
    }

    private void updateLabels() {
        for (Control[] row : keyRows) {
            for (Control button : row) {
                if (button instanceof Key) {
                    Key key = (Key)button;
                    String txt = key.chars[0];
                    String alt = (key.chars.length > 1) ? key.chars[1] : "";
                    if (key.chars.length > 1 && state == State.NUMERIC) {
                        txt = key.chars[1];
                        if (key.chars.length > 2) {
                            alt = key.chars[2];
                        }
                    } else if (state == State.SHIFTED || state == State.SHIFT_LOCK) {
                        txt = txt.toUpperCase();
                    }
                    key.setText(txt);
                    if (key.graphic != null) {
                        key.graphic.setText(alt);
                    }
                }
            }
        }
        symbolKey.setText(symbolKey.chars[(state == State.NUMERIC) ? 1 : 0]);
    }

    private void fireKeyEvent(Node target, EventType<? extends KeyEvent> eventType,
                           KeyCode keyCode, String keyChar, String keyText,
                           boolean shiftDown, boolean controlDown,
                           boolean altDown, boolean metaDown) {
        try {
            Field fld = FXRobotHelper.class.getDeclaredField("inputAccessor");
            fld.setAccessible(true);
            FXRobotInputAccessor inputAccessor = (FXRobotInputAccessor)fld.get(null);
            target.fireEvent(inputAccessor.createKeyEvent(eventType,
                                                          keyCode, keyChar, keyText,
                                                          shiftDown, controlDown,
                                                          altDown, metaDown));
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    private class Key extends Button {
        String str;
        String[] chars;
        Label graphic;
        boolean pendingSecondaryVK = false;

        EventHandler<ActionEvent> actionHandler = new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
if (fxvk != secondaryVK && secondaryPopup != null && secondaryPopup.isShowing()) return;
                Node target = fxvk.getAttachedNode();
                if (target instanceof EventTarget) {
                    String txt = getText();
                    if (txt.length() > 1 && txt.contains(" ")) {
                        //txt = txt.split(" ")[shift ? 1 : 0];
                        txt = txt.split(" ")[0];
                    }
                    for (int i = 0; i < txt.length(); i++) {
                        String str = txt.substring(i, i+1);
                        fireKeyEvent(target, KeyEvent.KEY_TYPED, null, str, str,
                                  state == State.SHIFTED, false, false, false);
                    }

                    if (state == State.SHIFTED) {
                        toggleShift();
                        toggleShift();
                    }
                }
if (fxvk == secondaryVK) {
    showSecondaryVK(null, fxvk, state);
}
            }
        };

        Key(String str) {
            this.str = str;
            setFocusTraversable(false);
            setOnAction(actionHandler);

            if (fxvk != secondaryVK)
            setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    showSecondaryVK(null, fxvk, state);
                    pendingSecondaryVK = true;
                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException ex) {
                                pendingSecondaryVK = false;
                            }
                            if (pendingSecondaryVK) {
                                Platform.runLater(new Runnable() {
                                    public void run() {
                                        showSecondaryVK(Key.this, fxvk, state);
                                    }
                                });
                            }
                        }
                    }).start();
                }
            });

            if (fxvk != secondaryVK)
            setOnMouseReleased(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    pendingSecondaryVK = false;
                }
            });


            if (str.length() == 1) {
                chars = new String[] { str };
            } else {
                chars = str.split(" ");
            }
            setContentDisplay(ContentDisplay.RIGHT);
            setText(chars[0]);
            graphic = new Label((chars.length > 1) ? chars[1] : " ");
            //graphic.getStyleClass().add("
            //setGraphicTextGap(KEY_WIDTH - 20);
            graphic.setPrefWidth(KEY_WIDTH / 2 - 10);
            graphic.setPrefHeight(KEY_HEIGHT - 6);
            setGraphic(graphic);

            setPrefWidth((str == " ") ? KEY_WIDTH * 3 : KEY_WIDTH);
            setPrefHeight(KEY_HEIGHT);
        }
    }

    private class CommandKey extends Button {
        KeyCode code;

        EventHandler<ActionEvent> actionHandler = new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                showSecondaryVK(null, null, null);
                Node target = fxvk.getAttachedNode();
                if (target instanceof EventTarget) {
                    String txt = getText();
                    fireKeyEvent(target, KeyEvent.KEY_PRESSED, code, null, null,
                              false, false, false, false);
                    if (state == State.SHIFTED) {
                        toggleShift();
                        toggleShift();
                    }
                }
            }
        };

        CommandKey(String label, KeyCode code, double width) {
            super(label);
            this.code = code;
            getStyleClass().add("special-key");
            setFocusTraversable(false);
            setOnAction(actionHandler);
            setPrefWidth(width);
            setPrefHeight(KEY_HEIGHT);
        }
    }

    private class ShiftKey extends Button {
        EventHandler<ActionEvent> actionHandler = new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                showSecondaryVK(null, null, null);
                toggleShift();
            }
        };

        ShiftKey(double width) {
            super("\u21d1");
            getStyleClass().add("special-key");
            setFocusTraversable(false);
            setOnAction(actionHandler);
            setPrefWidth(width);
            setPrefHeight(KEY_HEIGHT);
        }
    }

    private class SymbolKey extends Button {
        String str;
        String[] chars;

        EventHandler<ActionEvent> actionHandler = new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                state = (state == State.NUMERIC) ? State.NORMAL : State.NUMERIC;
                shiftKey.setDisable(state == State.NUMERIC);
                showSecondaryVK(null, null, null);
                updateLabels();
            }
        };

        SymbolKey(String str, double width) {
            this.str = str;
            getStyleClass().add("special-key");
            setFocusTraversable(false);

            if (str.length() == 1) {
                chars = new String[] { str };
            } else {
                chars = str.split(" ");
            }
            setText(chars[0]);

            setOnAction(actionHandler);
            setPrefWidth(width);
            setPrefHeight(KEY_HEIGHT);
        }
    }

    @Override public void layoutChildren() {
        double kw, kh;

        if (fxvk == secondaryVK) {
            kw = ((FXVKSkin)primaryVK.getSkin()).KEY_WIDTH;
            kh = ((FXVKSkin)primaryVK.getSkin()).KEY_HEIGHT;
        } else {
            kw = (getWidth() / 10) - 2 * hGap;
            kh = getHeight() / keyRows.length;
        }

        if (KEY_WIDTH != kw || KEY_HEIGHT != kh) {
            KEY_WIDTH = kw;
            KEY_HEIGHT = kh;
            createKeys();
        }

        super.layoutChildren();
    }

    private static void showSecondaryVK(final Key key, FXVK primVK, State state) {
        if (key != null) {
            primaryVK = primVK;
            final Node textInput = primaryVK.getAttachedNode();

            if (secondaryPopup == null) {
                secondaryVK = new FXVK();
                secondaryVK.getStyleClass().add("secondary");
                if (state == State.NUMERIC) {
                    secondaryVK.chars = new String[key.chars.length - 1];
                    System.arraycopy(key.chars, 1, secondaryVK.chars, 0, secondaryVK.chars.length);
                } else if (state == State.SHIFTED || state == State.SHIFT_LOCK) {
                    secondaryVK.chars = new String[key.chars.length];
                    System.arraycopy(key.chars, 0, secondaryVK.chars, 0, secondaryVK.chars.length);
                    secondaryVK.chars[0] = key.chars[0].toUpperCase();
                } else {
                    secondaryVK.chars = key.chars;
                }
                secondaryPopup = new Popup();
                secondaryPopup.getContent().add(secondaryVK);
            } else {
                if (state == State.NUMERIC) {
                    secondaryVK.chars = new String[key.chars.length - 1];
                    System.arraycopy(key.chars, 1, secondaryVK.chars, 0, secondaryVK.chars.length);
                } else if (state == State.SHIFTED || state == State.SHIFT_LOCK) {
                    secondaryVK.chars = new String[key.chars.length];
                    System.arraycopy(key.chars, 0, secondaryVK.chars, 0, secondaryVK.chars.length);
                    secondaryVK.chars[0] = key.chars[0].toUpperCase();
                } else {
                    secondaryVK.chars = key.chars;
                }
                ((FXVKSkin)secondaryVK.getSkin()).createKeys();
            }

            secondaryVK.setAttachedNode(textInput);
            double w = key.chars.length * KEY_WIDTH + (key.chars.length - 1) * hGap;
            secondaryVK.setPrefWidth(w);
            secondaryVK.setMaxWidth(w);
            secondaryVK.setPrefHeight(KEY_HEIGHT);
            Platform.runLater(new Runnable() {
                public void run() {
                    Point2D nodePoint =
                        com.sun.javafx.Utils.pointRelativeTo(key,
//                                       secondaryPopup.getWidth(), secondaryPopup.getHeight(),
                                      secondaryVK.prefWidth(-1), secondaryVK.prefHeight(-1),
                                      HPos.CENTER, VPos.TOP, 0, -5, true);

                    Point2D point =
                        com.sun.javafx.Utils.pointRelativeTo(key.getScene().getRoot(),
                                      secondaryPopup.getWidth(), secondaryPopup.getHeight(),
                                      HPos.CENTER, VPos.TOP, 0, 0, true);

                    secondaryPopup.show(key.getScene().getWindow(), nodePoint.getX(), nodePoint.getY());
                }
            });
        } else {
//             if (secondaryVK != null && secondaryVK.getAttachedNode() == textInput) {
            if (secondaryVK != null) {
                secondaryVK.setAttachedNode(null);
                secondaryPopup.hide();
            }
        }
    }
}
