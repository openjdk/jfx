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

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.geometry.Insets;
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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.util.Duration;

import com.sun.javafx.robot.impl.FXRobotHelper;
import com.sun.javafx.robot.impl.FXRobotHelper.FXRobotInputAccessor;
import com.sun.javafx.scene.control.behavior.BehaviorBase;

import javafx.animation.Animation.Status;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.MouseEvent.*;

public class FXVKSkin extends SkinBase<FXVK, BehaviorBase<FXVK>> {

    private static Region oldRoot;
    private static Pane newRoot;
    private static Popup secondaryPopup;
    private static FXVK primaryVK;
    private static FXVK secondaryVK;
    private static Popup vkPopup;
    private static Timeline slideInTimeline;
    private static Timeline slideOutTimeline;
    private static Timeline slideRootTimeline;
    private static Timeline secondaryVKDelay;
    private static CharKey secondaryVKKey;

    private Node attachedNode;

    FXVK fxvk;
    Control[][] keyRows;

    enum State { NORMAL, SHIFTED, SHIFT_LOCK, NUMERIC; };

    static State state = State.NORMAL;

    static final boolean USE_POPUP = false;
    static final double VK_WIDTH = 800;
    static final double VK_HEIGHT = 230;
    static final double VK_SLIDE_MILLIS = 250;
    static final double PREF_KEY_WIDTH = 40;
    static final double PREF_KEY_HEIGHT = 30;
    static final double hGap = 2;
    static final double vGap = 3;

    double keyWidth = PREF_KEY_WIDTH;
    double keyHeight = PREF_KEY_HEIGHT;

    private ShiftKey shiftKey;
    private SymbolKey symbolKey;

    public FXVKSkin(final FXVK fxvk) {
        super(fxvk, new BehaviorBase<FXVK>(fxvk));
        this.fxvk = fxvk;

        fxvk.setFocusTraversable(false);

        createKeys();

        fxvk.attachedNodeProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                Node oldNode = attachedNode;
                attachedNode = fxvk.getAttachedNode();

if (USE_POPUP) {
                if (fxvk != secondaryVK && oldNode != null) {
                    translatePane(oldNode.getScene().getRoot(), 0);
                }
} else {
                if (fxvk != secondaryVK && oldRoot != null) {
                    translatePane(oldRoot, 0);
                }
}

                if (attachedNode != null) {
                    final Scene scene = attachedNode.getScene();

                    if (secondaryVKDelay == null) {
                        secondaryVKDelay = new Timeline();
                        KeyFrame kf = new KeyFrame(Duration.millis(500), new EventHandler<ActionEvent>() {
                            @Override public void handle(ActionEvent event) {
                                if (secondaryVKKey != null) {
                                    showSecondaryVK(secondaryVKKey, fxvk, state);
                                }
                            }
                        });
                        secondaryVKDelay.getKeyFrames().add(kf);
                    }

if (USE_POPUP) {
                    if (vkPopup == null) {
                        vkPopup = new Popup();
                        vkPopup.getContent().add(fxvk);
                    }

                    if (vkPopup.isShowing()) {
                        Point2D nodePoint =
                            com.sun.javafx.Utils.pointRelativeTo(attachedNode,
                                                                 vkPopup.getWidth(), vkPopup.getHeight(),
                                                                 HPos.CENTER, VPos.BOTTOM, 0, 2, true);
                        Point2D point =
                            com.sun.javafx.Utils.pointRelativeTo(scene.getRoot(),
                                                                 vkPopup.getWidth(), vkPopup.getHeight(),
                                                                 HPos.CENTER, VPos.BOTTOM, 0, 0, true);
                        double y = point.getY() - fxvk.prefHeight(-1);
                        double nodeBottom = nodePoint.getY();
                        if (y < nodeBottom) {
                            translatePane(scene.getRoot(), y - nodeBottom);
                        }
                    } else {
                        Platform.runLater(new Runnable() {
                            public void run() {
                                Point2D nodePoint =
                                    com.sun.javafx.Utils.pointRelativeTo(attachedNode,
                                                                         vkPopup.getWidth(), vkPopup.getHeight(),
                                                                         HPos.CENTER, VPos.BOTTOM, 0, 2, true);
                                Point2D point =
                                    com.sun.javafx.Utils.pointRelativeTo(scene.getRoot(),
                                                                         vkPopup.getWidth(), vkPopup.getHeight(),
                                                                         HPos.CENTER, VPos.BOTTOM, 0, 0, true);
                                double y = point.getY() - fxvk.prefHeight(-1);
                                vkPopup.show(attachedNode, point.getX(), y);


                                double nodeBottom = nodePoint.getY();
                                if (y < nodeBottom) {
                                    translatePane(scene.getRoot(), y - nodeBottom);
                                }
                            }
                        });
                    }

                    if (oldNode == null || oldNode.getScene() != attachedNode.getScene()) {
                        fxvk.setPrefWidth(scene.getWidth());
                        fxvk.setMaxWidth(USE_PREF_SIZE);
                        fxvk.setPrefHeight(200);
                    }
} else {
                    if (newRoot == null) {
                        oldRoot = (Region)scene.getRoot();
                        newRoot = new NewRootPane(oldRoot);
                        scene.setRoot(newRoot);
                        newRoot.getChildren().add(fxvk);
                        slideInTimeline = new Timeline();
                    }

                    fxvk.setVisible(true);
                    if (fxvk != secondaryVK && fxvk.getHeight() > 0 &&
                        (fxvk.getLayoutY() == 0 || fxvk.getLayoutY() > scene.getHeight() - fxvk.getHeight())) {

                        slideOutTimeline.stop();
                        slideInTimeline.playFromStart();
                    }

                    if (fxvk != secondaryVK) {
                        Platform.runLater(new Runnable() {
                            public void run() {
                                double nodeBottom =
                                    attachedNode.localToScene(attachedNode.getBoundsInLocal()).getMaxY() + 2;
                                if (nodeBottom > fxvk.getLayoutY()) {
                                    translatePane(oldRoot, fxvk.getLayoutY() - nodeBottom);
                                }
                            }
                        });

                        if (oldNode == null || oldNode.getScene() != attachedNode.getScene()) {
                            fxvk.setPrefWidth(VK_WIDTH);
                            fxvk.setMaxWidth(USE_PREF_SIZE);
                            fxvk.setPrefHeight(VK_HEIGHT);
                            fxvk.setMinHeight(USE_PREF_SIZE);
                        }
                    }
}
                } else {
if (USE_POPUP) {
                    if (vkPopup != null) {
                        vkPopup.hide();
                    }
} else {
                    if (fxvk != secondaryVK) {
                        slideInTimeline.stop();
                        slideOutTimeline.playFromStart();
                    }
}
                    if (secondaryVK != null) {
                        secondaryVK.setAttachedNode(null);
                        secondaryPopup.hide();
                    }
                    return;
                }
            }
        });
    }

    private void translatePane(Parent pane, double y) {
        if (slideRootTimeline == null) {
            slideRootTimeline = new Timeline();
        } else {
            slideRootTimeline.stop();
        }

        slideRootTimeline.getKeyFrames().setAll(
            new KeyFrame(Duration.millis(VK_SLIDE_MILLIS),
                         new KeyValue(pane.translateYProperty(), y, Interpolator.EASE_BOTH)));
        slideRootTimeline.playFromStart();
    }

    private void createKeys() {
        getChildren().clear();

        if (fxvk.chars != null) {
            // Secondary popup
            int nKeys = fxvk.chars.length;
            int nRows = (int)Math.floor(Math.sqrt(Math.max(1, nKeys - 2)));
            int nKeysPerRow = (int)Math.ceil(nKeys / (double)nRows);
            keyRows = new Control[nRows][];
            for (int i = 0; i < nRows; i++) {
                keyRows[i] =
                    makeKeyRow((Object[])Arrays.copyOfRange(fxvk.chars, i * nKeysPerRow,
                                                            Math.min((i + 1) * nKeysPerRow, fxvk.chars.length)));
            }
        } else {
            // TODO: Move this to a resource bundle.
            keyRows = new Control[][] {
                makeKeyRow("q 1 [",
                           "w 2 ]",
                           "e 3 { \u00e8 \u00e9 \u00ea \u00eb", // e 3 { egrave eacute ecircumflex ediaeresis
                           "r 4 } \u00ae", // r 4 } registered
                           "t 5 \\ \u2122", // t 5 \ TM
                           "y 6 | \u1ef3 \u00fd \u0177 \u0233 \u00ff \u1ef7", // y 6 | ygrave yacute ycircumflex ymacron ydiaeresis yhook
                           "u 7 \" \u00f9 \u00fa \u00fb \u00fc", // u 7 \" ugrave uacute ucircumflex udiaeresis
                           "i 8 < \u00ec \u00ed \u00ee \u00ef", // i 8 < igrave iacute icircumflex idiaeresis
                           "o 9 > \u00f2 \u00f3 \u00f4 \u00f5 \u00f6 \u00f8 \u00b0", // o 9 > ograve oacute ocircumflex otilde odiaeresis oslash degree
                           "p 0 _ \u00a7 \u00b6 \u03c0"),       // p 0 _ paragraph pilcrow pi
                makeKeyRow("a @ ~ \u00e0 \u00e1 \u00e2 \u00e3 \u00e4 \u00e5", // a @ ~ agrave aacute acircumflex atilde adiaeresis aring
                           "s # ` \u015f \u0161 \u00df \u03c3", // s # ` scedilla scaron sharps sigma
                           "d $ \u20ac \u00f0",                 // d $ euro eth
                           "f % \u00a3",                        // f % sterling
                           "g ^ \u00a5",                        // g ^ yen
                           "h & \u00a7",                        // h & paragraph (TODO: use only once)
                           "j * \u00b7",                        // j * middledot
                           "k ( \u00b0",                        // k ( degree (TODO: use only once)
                           "l ) \u2260"),                       // l ) notequalto
                makeKeyRow(shiftKey = new ShiftKey(keyWidth * 1.5),
                           "z - \u00a1",                        // z - invertedexclamationmark
                           "x = \u00bf",                        // x = invertedquestionmark
                           "c + \u2030 \u00e7 \u00a9 \u00a2",   // c + permille ccedilla copyright cent
                           "v ; \u00ae",                        // v ; registered (TODO: use only once)
                           "b : \u2122",                        // b : TM  (TODO: use only once)
                           "n / \u00ab \u00f1",                 // n / doubleleftangle ntilde
                           "m ' \u00bb",                        // m ' doublerightangle (add micro)
                           new CommandKey("\u232b", BACK_SPACE, keyWidth * 1.5)),
                makeKeyRow(symbolKey = new SymbolKey("!#123 ABC", keyWidth * 2.5 + (9-4) * hGap / 2),
                           ", !",                               // , !
                           " ",                                 // space
                           ". ?",                               // . ?
                           new CommandKey("\u21b5", ENTER, keyWidth * 2.5 + (9-4) * hGap / 2))
            };
        }

        VBox vbox = new VBox(vGap);
        vbox.setFillWidth(true);
        getChildren().add(vbox);

        double primaryFontSize = 16 * keyWidth / PREF_KEY_WIDTH;
        double secondaryFontSize = 8 * keyWidth / PREF_KEY_WIDTH;

        for (Control[] row : keyRows) {
            HBox hbox = new HBox(hGap);
            // Primary keyboard has centered keys, secondary has left aligned keys.
            hbox.setAlignment((fxvk.chars != null) ? Pos.CENTER_LEFT : Pos.CENTER);
            vbox.getChildren().add(hbox);
            for (Control c : row) {
                hbox.getChildren().add(c);
                HBox.setHgrow(c, Priority.ALWAYS);
                if (c instanceof Key) {
                    Key key = (Key)c;
                    if (fxvk.chars != null) {
                        key.getStyleClass().add("secondary-key");
                    }
                    key.setStyle("-fx-font-size: "+primaryFontSize+"px;");
                    if (key.getGraphic() instanceof Label) {
                        ((Label)key.getGraphic()).setStyle("-fx-font-size: "+secondaryFontSize+"px;");
                    }
                }
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
                keyRow[i] = new CharKey((String)keyList.get(i));
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

        if (fxvk == secondaryVK) {
            ((FXVKSkin)primaryVK.getSkin()).updateLabels();
        } else {
            updateLabels();
        }
    }

    private void updateLabels() {
        for (Control[] row : keyRows) {
            for (Control button : row) {
                if (button instanceof CharKey) {
                    CharKey key = (CharKey)button;
                    String txt = key.chars[0];
                    String alt = (key.chars.length > 1) ? key.chars[1] : "";
                    if (key.chars.length > 1 && state == State.NUMERIC) {
                        txt = key.chars[1];
                        if (key.chars.length > 2) {
                            alt = key.chars[2];
                        } else {
                            alt = "";
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
        private Key() {
            this(null);
        }

        private Key(String text) {
            super(text);

            getStyleClass().add("key");
            setFocusTraversable(false);

            setMinHeight(USE_PREF_SIZE);
            setPrefHeight(keyHeight);
        }

    }

    private class CharKey extends Key {
        String str;
        String[] chars;
        Label graphic;

        EventHandler<ActionEvent> actionHandler = new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                if (fxvk != secondaryVK && secondaryPopup != null && secondaryPopup.isShowing()) {
                    return;
                }

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

        CharKey(String str) {
            this.str = str;
            setOnAction(actionHandler);

            if (fxvk != secondaryVK) {
                setOnMousePressed(new EventHandler<MouseEvent>() {
                    @Override public void handle(MouseEvent event) {
                        showSecondaryVK(null, fxvk, state);
                        if (state != State.NUMERIC || chars.length > 2) {
                            secondaryVKKey = CharKey.this;
                            secondaryVKDelay.playFromStart();
                        } else {
                            secondaryVKKey = null;
                        }
                    }
                });

                setOnMouseReleased(new EventHandler<MouseEvent>() {
                    @Override public void handle(MouseEvent event) {
                        secondaryVKDelay.stop();
                    }
                });
            }

            if (str.length() == 1) {
                chars = new String[] { str };
            } else {
                chars = str.split(" ");
            }
            setContentDisplay(ContentDisplay.RIGHT);
            setText(chars[0]);
            if (chars.length > 1) {
                graphic = new Label((chars.length > 1) ? chars[1] : " ");
                graphic.setPrefWidth(keyWidth / 2 - 10);
                graphic.setMinWidth(USE_PREF_SIZE);
                graphic.setPrefHeight(keyHeight - 6);
                setGraphic(graphic);
            }

            setPrefWidth((str == " ") ? keyWidth * 3 : keyWidth);
        }
    }

    private class CommandKey extends Key {
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
            setOnAction(actionHandler);
            setPrefWidth(width);
        }
    }

    private class ShiftKey extends Key {
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
        }
    }

    private class SymbolKey extends Key {
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

            if (str.length() == 1) {
                chars = new String[] { str };
            } else {
                chars = str.split(" ");
            }
            setText(chars[0]);

            setOnAction(actionHandler);
            setPrefWidth(width);
        }
    }

    @Override public void layoutChildren() {
        double kw, kh;
        Insets insets = getInsets();

        if (fxvk == secondaryVK) {
            kw = ((FXVKSkin)primaryVK.getSkin()).keyWidth;
            kh = ((FXVKSkin)primaryVK.getSkin()).keyHeight;
        } else {
            kw = (getWidth() / 10) - 2 * hGap;
            kh = (getHeight() - insets.getTop() - insets.getBottom() - (keyRows.length - 1) * vGap) / keyRows.length;
        }

        if (keyWidth != kw || keyHeight != kh) {
            keyWidth = kw;
            keyHeight = kh;
            createKeys();
        }

        super.layoutChildren();
    }

    private static void showSecondaryVK(final CharKey key, FXVK primVK, State state) {
        if (key != null) {
            primaryVK = primVK;
            final Node textInput = primaryVK.getAttachedNode();

            if (secondaryPopup == null) {
                secondaryVK = new FXVK();
                secondaryVK.getStyleClass().add("fxvk-secondary");
                secondaryPopup = new Popup();
                secondaryPopup.getContent().add(secondaryVK);
            }

            if (state == State.NUMERIC) {
                ArrayList<String> symbols = new ArrayList<String>();
                for (String ch : key.chars) {
                    if (!Character.isLetter(ch.charAt(0))) {
                        symbols.add(ch);
                    }
                }
                secondaryVK.chars = symbols.toArray(new String[symbols.size()]);
            } else if (state == State.SHIFTED || state == State.SHIFT_LOCK) {
                secondaryVK.chars = new String[key.chars.length];
                System.arraycopy(key.chars, 0, secondaryVK.chars, 0, secondaryVK.chars.length);
                for (int i = 0; i < secondaryVK.chars.length; i++) {
                    secondaryVK.chars[i] = key.chars[i].toUpperCase();
                }
            } else {
                secondaryVK.chars = key.chars;
            }

            if (secondaryVK.getSkin() != null) {
                ((FXVKSkin)secondaryVK.getSkin()).createKeys();
            }

            secondaryVK.setAttachedNode(textInput);
            FXVKSkin primarySkin = (FXVKSkin)primaryVK.getSkin();
            Insets insets = primarySkin.getInsets();
            int nKeys = secondaryVK.chars.length;
            int nRows = (int)Math.floor(Math.sqrt(Math.max(1, nKeys - 2)));
            int nKeysPerRow = (int)Math.ceil(nKeys / (double)nRows);
            final double w = insets.getLeft() + insets.getRight() +
                             nKeysPerRow * primarySkin.keyWidth + (nKeys - 1) * hGap;
            final double h = nRows * primarySkin.keyHeight + (nRows-1) * vGap + 5;
            secondaryVK.setPrefWidth(w);
            secondaryVK.setMinWidth(USE_PREF_SIZE);
            secondaryVK.setPrefHeight(h);
            secondaryVK.setMinHeight(USE_PREF_SIZE);
            Platform.runLater(new Runnable() {
                public void run() {
                    // Position popup on screen
                    Point2D nodePoint =
                        com.sun.javafx.Utils.pointRelativeTo(key, w, h, HPos.CENTER, VPos.TOP,
                                                             5, -3, true);
                    double x = nodePoint.getX();
                    double y = nodePoint.getY();
                    Scene scene = key.getScene();
                    x = Math.min(x, scene.getWindow().getX() + scene.getWidth() - w);
                    secondaryPopup.show(key.getScene().getWindow(), x, y);
                }
            });
        } else {
            if (secondaryVK != null) {
                secondaryVK.setAttachedNode(null);
                secondaryPopup.hide();
            }
        }
    }

    class NewRootPane extends Pane {
        double dragStartY;

        NewRootPane(final Region oldRoot) {
            getChildren().add(oldRoot);
            prefWidthProperty().bind(oldRoot.prefWidthProperty());
            prefHeightProperty().bind(oldRoot.prefHeightProperty());


            addEventHandler(MOUSE_PRESSED, new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent e) {
                    dragStartY = e.getY() - oldRoot.getTranslateY();
                    e.consume();
                }
            });

            addEventHandler(MOUSE_DRAGGED, new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent e) {
                    if (fxvk.isVisible()) {
                        double y =
                            Math.min(0, Math.max(e.getY() - dragStartY,
                                                 fxvk.getLayoutY() - oldRoot.getHeight()));
                        oldRoot.setTranslateY(y);
                    }
                    e.consume();
                }
            });
        }

        @Override protected double computePrefWidth(double height) {
            return oldRoot.prefWidth(height);
        }

        @Override protected double computePrefHeight(double width) {
            return oldRoot.prefHeight(width);
        }

        @Override public void layoutChildren() {
            double scale = getWidth() / fxvk.prefWidth(-1);
            double rootHeight = getHeight();
            double vkHeight = fxvk.prefHeight(-1) * scale;

            boolean resized = false;
            if (fxvk.getWidth() != getWidth() || fxvk.getHeight() != vkHeight) {
                fxvk.resize(getWidth(), vkHeight);
                resized = true;
            }

            if (fxvk.getLayoutY() == 0) {
                fxvk.setLayoutY(rootHeight);
            }

            slideInTimeline.getKeyFrames().setAll(
                new KeyFrame(Duration.ZERO,
                             new KeyValue(fxvk.visibleProperty(), true),
                             new KeyValue(fxvk.layoutYProperty(), rootHeight)),
                new KeyFrame(Duration.millis(VK_SLIDE_MILLIS),
                             new KeyValue(fxvk.visibleProperty(), true),
                             new KeyValue(fxvk.layoutYProperty(),
                                          Math.floor(rootHeight - vkHeight),
                                          Interpolator.EASE_BOTH)));

            slideOutTimeline = new Timeline();
            slideOutTimeline.getKeyFrames().setAll(
                new KeyFrame(Duration.ZERO,
                             new KeyValue(fxvk.layoutYProperty(),
                                          Math.floor(rootHeight - vkHeight))),
                new KeyFrame(Duration.millis(VK_SLIDE_MILLIS),
                             new KeyValue(fxvk.layoutYProperty(),
                                          rootHeight,
                                          Interpolator.EASE_BOTH),
                             new KeyValue(fxvk.visibleProperty(), false)));

            if (fxvk.isVisible()) {
                if (fxvk.getLayoutY() >= rootHeight) {
                    slideOutTimeline.stop();
                    slideInTimeline.playFromStart();
                } else if (resized && slideInTimeline.getStatus() == Status.STOPPED
                                   && slideOutTimeline.getStatus() == Status.STOPPED) {
                    fxvk.setLayoutY(rootHeight - vkHeight);
                }
            }
        }
    }
}
