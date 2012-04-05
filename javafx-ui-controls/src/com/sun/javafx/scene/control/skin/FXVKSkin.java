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
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
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

import static com.sun.javafx.scene.control.skin.resources.ControlResources.*;

public class FXVKSkin extends SkinBase<FXVK, BehaviorBase<FXVK>> {

    private static Region oldRoot;
    private static NewRootPane newRoot;
    private static Popup secondaryPopup;
    private static FXVK primaryVK;

    private Timeline slideInTimeline;
    private Timeline slideOutTimeline;
    private static Timeline slideRootTimeline;

    private static FXVK secondaryVK;
    private static Timeline secondaryVKDelay;
    private static CharKey secondaryVKKey;

    private Node attachedNode;

    FXVK fxvk;
    Control[][] keyRows;

    enum State { NORMAL, SHIFTED, SHIFT_LOCK, NUMERIC; };

    private State state = State.NORMAL;

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

        slideInTimeline = new Timeline();
        slideOutTimeline = new Timeline();

        fxvk.attachedNodeProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                Node oldNode = attachedNode;
                attachedNode = fxvk.getAttachedNode();

                if (fxvk != secondaryVK && oldRoot != null) {
                    translatePane(oldRoot, 0);
                }

                if (attachedNode != null) {
                    if (keyRows == null) {
                        createKeys();
                    }

                    final Scene scene = attachedNode.getScene();
                    fxvk.setVisible(true);

                    if (fxvk != secondaryVK) {
                        if (secondaryVKDelay == null) {
                            secondaryVKDelay = new Timeline();
                        }
                        KeyFrame kf = new KeyFrame(Duration.millis(500), new EventHandler<ActionEvent>() {
                            @Override public void handle(ActionEvent event) {
                                if (secondaryVKKey != null) {
                                    showSecondaryVK(secondaryVKKey);
                                }
                            }
                        });
                        secondaryVKDelay.getKeyFrames().setAll(kf);


                        if (newRoot == null) {
                            oldRoot = (Region)scene.getRoot();
                            newRoot = new NewRootPane(oldRoot);
                            scene.setRoot(newRoot);
                        }

                        if (!newRoot.getChildren().contains(fxvk)){
                            newRoot.getChildren().add(fxvk);
                        }

                        newRoot.slideInTimeline = slideInTimeline;
                        newRoot.slideOutTimeline = slideOutTimeline;
                        newRoot.updateTimelines(fxvk);

                        if (fxvk.getHeight() > 0 &&
                            (fxvk.getLayoutY() == 0 || fxvk.getLayoutY() > scene.getHeight() - fxvk.getHeight())) {

                            slideOutTimeline.stop();
                            slideInTimeline.playFromStart();
                        }


                        Platform.runLater(new Runnable() {
                            public void run() {
                                double nodeBottom =
                                    attachedNode.localToScene(attachedNode.getBoundsInLocal()).getMaxY() + 2;
                                if (fxvk.getLayoutY() > 0 && nodeBottom > fxvk.getLayoutY()) {
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
                } else {
                    if (fxvk != secondaryVK) {
                        slideInTimeline.stop();
                        slideOutTimeline.playFromStart();
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

    private static void translatePane(Parent pane, double y) {
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
            if (nKeys > 1) {
                int nRows = (int)Math.floor(Math.sqrt(Math.max(1, nKeys - 2)));
                int nKeysPerRow = (int)Math.ceil(nKeys / (double)nRows);
                keyRows = new Control[nRows][];
                for (int i = 0; i < nRows; i++) {
                    keyRows[i] =
                        makeKeyRow((String[])Arrays.copyOfRange(fxvk.chars, i * nKeysPerRow,
                                                                Math.min((i + 1) * nKeysPerRow, fxvk.chars.length)));
                }
            } else {
                keyRows = new Control[0][];
            }
        } else {
            // Read keyboard layout from resource bundle
            ArrayList<Control[]> rows = new ArrayList<Control[]>();
            ArrayList<String> row = new ArrayList<String>();
            ArrayList<Double> keyWidths = new ArrayList<Double>();
            String typeString = new String[] { "Text", "Numeric", "URL", "Email" }[fxvk.vkType];
            int r = 0;
            try {
                String format = "FXVK."+typeString+".row%d.key%02d";
                while (getBundle().containsKey(String.format(format, ++r, 1))) {
                    int c = 0;
                    String keyChars;
                    while (getBundle().containsKey(String.format(format, r, ++c))) {
                        row.add(getString(String.format(format, r, c)));
                        Double w = -1.0;
                        String widthLookup = String.format(format+".width", r, c);
                        if (getBundle().containsKey(widthLookup)) {
                            try {
                                w = new Double(getString(widthLookup));
                            } catch (NumberFormatException ex) {
                                System.err.println(widthLookup+"="+getString(widthLookup));
                                System.err.println(ex);
                            }
                        }
                        keyWidths.add(w);
                    }
                    rows.add(makeKeyRow(row, keyWidths));
                    row.clear();
                    keyWidths.clear();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            keyRows = rows.toArray(new Control[rows.size()][]);
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

                    int textLen = key.getText().length();
                    if (textLen == 1 || !key.getClass().getSimpleName().equals("CharKey")) {
                        key.setStyle("-fx-font-size: "+primaryFontSize+"px;");
                    } else {
                        key.setStyle("-fx-font-size: "+(primaryFontSize* Math.min(1.0, 3.0/textLen))+"px;");
                        key.setGraphicTextGap(key.getGraphicTextGap() + 2*textLen);
                    }
                    if (key.getGraphic() instanceof Label) {
                        ((Label)key.getGraphic()).setStyle("-fx-font-size: "+secondaryFontSize+"px;");
                    }
                }
            }
        }
    }


    private Control[] makeKeyRow(String... obj) {
        return makeKeyRow(Arrays.asList(obj), null);
    }

    private Control[] makeKeyRow(List<String> keyList, List<Double> widths) {
        Control[] keyRow = new Control[keyList.size()];
        for (int i = 0; i < keyRow.length; i++) {
            String str = keyList.get(i);
            Double w = (widths != null) ? widths.get(i) : -1.0;
            if ("BACKSPACE".equals(str)) {
                if (w < 0) w = 1.5;
                keyRow[i] = new CommandKey("\u232b", BACK_SPACE, keyWidth * w);
            } else if ("ENTER".equals(str)) {
                if (w < 0) w = 2.5;
                keyRow[i] = new CommandKey("\u21b5", ENTER, keyWidth * w + (9-4) * hGap / 2);
            } else if ("SHIFT".equals(str)) {
                if (w < 0) w = 1.5;
                keyRow[i] = shiftKey = new ShiftKey(keyWidth * w);
            } else if ("SYM".equals(str)) {
                if (w < 0) w = 2.5;
                keyRow[i] = symbolKey = new SymbolKey("!#123 ABC", keyWidth * w + (9-4) * hGap / 2);
            } else {
                if (w < 0) w = 1.0;
                keyRow[i] = new CharKey((String)keyList.get(i), keyWidth * w);
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
        if (symbolKey != null) {
            symbolKey.setText(symbolKey.chars[(state == State.NUMERIC) ? 1 : 0]);
        }
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
                    showSecondaryVK(null);
                }
            }
        };

        CharKey(String str, double width) {
            this.str = str;
            setOnAction(actionHandler);

            if (fxvk != secondaryVK) {
                setOnMousePressed(new EventHandler<MouseEvent>() {
                    @Override public void handle(MouseEvent event) {
                        showSecondaryVK(null);
                        secondaryVKKey = CharKey.this;
                        secondaryVKDelay.playFromStart();
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
                for (int i = 0; i < chars.length; i++) {
                    chars[i] = FXVKCharEntities.get(chars[i]);
                }
            }
            setContentDisplay(ContentDisplay.TOP);
            setGraphicTextGap(-8);
            setText(chars[0]);

            graphic = new Label((chars.length > 1) ? chars[1] : " ");
            graphic.setPrefWidth(keyWidth - 12);
            graphic.setMinWidth(USE_PREF_SIZE);
            graphic.setPrefHeight(keyHeight / 2 - 8);
            setGraphic(graphic);

            setPrefWidth(width);
        }
    }

    private class CommandKey extends Key {
        KeyCode code;

        EventHandler<ActionEvent> actionHandler = new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                showSecondaryVK(null);
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
                showSecondaryVK(null);
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
                if (shiftKey != null) {
                    shiftKey.setDisable(state == State.NUMERIC);
                }
                showSecondaryVK(null);
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

    private void showSecondaryVK(final CharKey key) {
        if (key != null) {
            primaryVK = fxvk;
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

            if (secondaryVK.chars.length > 1) {
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
            }
        } else {
            if (secondaryVK != null) {
                secondaryVK.setAttachedNode(null);
                secondaryPopup.hide();
            }
        }
    }

    static class NewRootPane extends Pane {
        Timeline slideInTimeline;
        Timeline slideOutTimeline;
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
                    for (Node child : getChildren()) {
                        if (child instanceof FXVK) {
                            FXVK fxvk = (FXVK)child;
                            if (fxvk.isVisible()) {
                                double y =
                                    Math.min(0, Math.max(e.getY() - dragStartY,
                                                         fxvk.getLayoutY() - oldRoot.getHeight()));
                                oldRoot.setTranslateY(y);
                                break;
                            }
                        }
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

        private void updateTimelines(FXVK fxvk) {
            double scale = getWidth() / fxvk.prefWidth(-1);
            double rootHeight = getHeight();
            double vkHeight = fxvk.prefHeight(-1) * scale;

            ((FXVKSkin)fxvk.getSkin()).slideInTimeline.getKeyFrames().setAll(
                new KeyFrame(Duration.ZERO,
                             new KeyValue(fxvk.visibleProperty(), true),
                             new KeyValue(fxvk.layoutYProperty(), rootHeight)),
                new KeyFrame(Duration.millis(VK_SLIDE_MILLIS),
                             new KeyValue(fxvk.visibleProperty(), true),
                             new KeyValue(fxvk.layoutYProperty(),
                                          Math.floor(rootHeight - vkHeight),
                                          Interpolator.EASE_BOTH)));

            ((FXVKSkin)fxvk.getSkin()).slideOutTimeline.getKeyFrames().setAll(
                new KeyFrame(Duration.ZERO,
                             new KeyValue(fxvk.layoutYProperty(),
                                          Math.floor(rootHeight - vkHeight))),
                new KeyFrame(Duration.millis(VK_SLIDE_MILLIS),
                             new KeyValue(fxvk.layoutYProperty(),
                                          rootHeight,
                                          Interpolator.EASE_BOTH),
                             new KeyValue(fxvk.visibleProperty(), false)));
        }

        @Override public void layoutChildren() {
            for (Node child : getChildren()) {
                if (child instanceof FXVK) {
                    final FXVK fxvk = (FXVK)child;
                    double scale = getWidth() / fxvk.prefWidth(-1);
                    final double rootHeight = getHeight();
                    final double vkHeight = fxvk.prefHeight(-1) * scale;

                    boolean resized = false;
                    if (fxvk.getWidth() != getWidth() || fxvk.getHeight() != vkHeight) {
                        fxvk.resize(getWidth(), vkHeight);
                        resized = true;
                    }

                    if (fxvk.getLayoutY() == 0) {
                        fxvk.setLayoutY(rootHeight);
                    }

                    updateTimelines(fxvk);


                    if (fxvk.isVisible()) {
                        if (fxvk.getLayoutY() >= rootHeight) {
                            slideOutTimeline.stop();
                            slideInTimeline.playFromStart();
                        } else if (resized && slideInTimeline.getStatus() == Status.STOPPED
                                           && slideOutTimeline.getStatus() == Status.STOPPED) {
                            fxvk.setLayoutY(rootHeight - vkHeight);
                        }
                        Platform.runLater(new Runnable() {
                            public void run() {
                                Node attachedNode = fxvk.getAttachedNode();
                                if (attachedNode != null) {
                                    double oldRootY = oldRoot.getTranslateY();
                                    double nodeBottom =
                                        attachedNode.localToScene(attachedNode.getBoundsInLocal()).getMaxY() + 2;
                                    if (nodeBottom > rootHeight - vkHeight) {
                                        translatePane(oldRoot, rootHeight - vkHeight - nodeBottom + oldRootY);
                                    }
                                }
                            }
                        });
                    }
                }
            }
        }
    }
}
