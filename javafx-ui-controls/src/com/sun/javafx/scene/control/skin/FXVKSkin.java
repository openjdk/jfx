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
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.Duration;

import com.sun.javafx.robot.impl.FXRobotHelper;
import com.sun.javafx.robot.impl.FXRobotHelper.FXRobotInputAccessor;
import com.sun.javafx.scene.control.behavior.BehaviorBase;

import javafx.animation.Animation.Status;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.MouseEvent.*;

import static com.sun.javafx.scene.control.skin.resources.ControlResources.*;

public class FXVKSkin extends SkinBase<FXVK, BehaviorBase<FXVK>> {

    private static Popup vkPopup;
    private static Popup secondaryPopup;
    private static FXVK primaryVK;

    private static Timeline slideInTimeline;
    private static Timeline slideOutTimeline;

    private static FXVK secondaryVK;
    private static Timeline secondaryVKDelay;
    private static CharKey secondaryVKKey;

    private Node attachedNode;

    FXVK fxvk;
    Control[][] keyRows;

    enum State { NORMAL, SHIFTED, SHIFT_LOCK, NUMERIC; };

    private State state = State.NORMAL;

    static final double VK_WIDTH = 640;
    static final double VK_HEIGHT = 243;
//     static final double VK_WIDTH = 480;
//     static final double VK_HEIGHT = 326;
    static final double VK_PORTRAIT_HEIGHT = 326;
    static final double VK_SLIDE_MILLIS = 250;
    static final double PREF_KEY_WIDTH = 56;
    static final double PREF_PORTRAIT_KEY_WIDTH = 40;
    static final double PREF_KEY_HEIGHT = 56;

    double keyWidth = PREF_KEY_WIDTH;
    double keyHeight = PREF_KEY_HEIGHT;

    private ShiftKey shiftKey;
    private SymbolKey symbolKey;

    private VBox vbox;

    // Proxy for read-only Window.yProperty() so we can animate.
    private static DoubleProperty winY = new SimpleDoubleProperty();
    static {
        winY.addListener(new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                if (vkPopup != null) {
                    vkPopup.setY(winY.get());
                }
            }
        });
    }



    public FXVKSkin(final FXVK fxvk) {
        super(fxvk, new BehaviorBase<FXVK>(fxvk));
        this.fxvk = fxvk;

        fxvk.setFocusTraversable(false);

        fxvk.attachedNodeProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                Node oldNode = attachedNode;
                attachedNode = fxvk.getAttachedNode();

                if (fxvk != FXVK.vk) {
                    // This is not the current vk, so nothing more to do
                    return;
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

                        if (vkPopup == null) {
                            vkPopup = new Popup();

                            // RT-21860 - This is causing
                            // IllegalStateException: The window must be focused when calling grabFocus()
                            //vkPopup.focusedProperty().addListener(new InvalidationListener() {
                            //    @Override public void invalidated(Observable ov) {
                            //        scene.getWindow().requestFocus();
                            //    }
                            //});

                            double screenHeight =
                                com.sun.javafx.Utils.getScreen(attachedNode).getBounds().getHeight();
                            double screenVisualHeight =
                                com.sun.javafx.Utils.getScreen(attachedNode).getVisualBounds().getHeight();
                            screenVisualHeight = Math.min(screenHeight, screenVisualHeight + 4 /*??*/);

                            slideInTimeline = new Timeline();
                            slideInTimeline.getKeyFrames().setAll(
                                new KeyFrame(Duration.millis(VK_SLIDE_MILLIS),
                                             new KeyValue(winY, screenVisualHeight - fxvk.prefHeight(-1),
                                                          Interpolator.EASE_BOTH)));

                            slideOutTimeline = new Timeline();
                            slideOutTimeline.getKeyFrames().setAll(
                                new KeyFrame(Duration.millis(VK_SLIDE_MILLIS),
                                             new KeyValue(winY, screenHeight, Interpolator.EASE_BOTH)));
                        }

                        vkPopup.getContent().setAll(fxvk);

                        if (!vkPopup.isShowing()) {
                            Platform.runLater(new Runnable() {
                                public void run() {
                                    Rectangle2D screenBounds =
                                        com.sun.javafx.Utils.getScreen(attachedNode).getBounds();
                                    vkPopup.show(attachedNode,
                                                 (screenBounds.getWidth() - fxvk.prefWidth(-1)) / 2,
                                                 screenBounds.getHeight() - fxvk.prefHeight(-1));
                                }
                            });
                        }

                        if (oldNode == null || oldNode.getScene() != attachedNode.getScene()) {
                            fxvk.setPrefWidth(scene.getWidth());
                            fxvk.setMaxWidth(USE_PREF_SIZE);
                            fxvk.setPrefHeight(200);
                        }

                        if (fxvk.getHeight() > 0 &&
                            (fxvk.getLayoutY() == 0 || fxvk.getLayoutY() > scene.getHeight() - fxvk.getHeight())) {

                            slideOutTimeline.stop();
                            winY.set(vkPopup.getY());
                            slideInTimeline.playFromStart();
                        }


                        if (oldNode == null || oldNode.getScene() != attachedNode.getScene()) {
                            fxvk.setPrefWidth(VK_WIDTH);
                            fxvk.setMinWidth(USE_PREF_SIZE);
                            fxvk.setMaxWidth(USE_PREF_SIZE);
                            fxvk.setPrefHeight(VK_HEIGHT);
                            fxvk.setMinHeight(USE_PREF_SIZE);
                        }
                    }
                } else {
                    if (fxvk != secondaryVK) {
                        slideInTimeline.stop();
                        winY.set(vkPopup.getY());
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

    private void createKeys() {
        getChildren().clear();

        if (fxvk.chars != null) {
            // Secondary popup
            int nKeys = fxvk.chars.length;
            if (nKeys > 1) {
                // Reorder to make letter keys appear before symbols
                String[] array = new String[nKeys];
                int ind = 0;
                for (String str : fxvk.chars) {
                    if (Character.isLetter(str.charAt(0))) {
                        array[ind++] = str;
                    }
                }
                for (String str : fxvk.chars) {
                    if (!Character.isLetter(str.charAt(0))) {
                        array[ind++] = str;
                    }
                }

                int nRows = (int)Math.floor(Math.sqrt(Math.max(1, nKeys - 2)));
                int nKeysPerRow = (int)Math.ceil(nKeys / (double)nRows);
                keyRows = new Control[nRows][];
                for (int i = 0; i < nRows; i++) {
                    keyRows[i] =
                        makeKeyRow((String[])Arrays.copyOfRange(array, i * nKeysPerRow,
                                                                Math.min((i + 1) * nKeysPerRow, nKeys)));
                }
            } else {
                keyRows = new Control[0][];
            }
        } else {
            // Read keyboard layout from resource bundle
            ArrayList<Control[]> rows = new ArrayList<Control[]>();
            ArrayList<String> row = new ArrayList<String>();
            ArrayList<Double> keyWidths = new ArrayList<Double>();
            String typeString = FXVK.VK_TYPE_NAMES[fxvk.vkType];
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

        vbox = new VBox();
        vbox.setId("vbox");
        getChildren().add(vbox);

        //double primaryFontSize = 16 * keyWidth / PREF_KEY_WIDTH;
        //double secondaryFontSize = 8 * keyWidth / PREF_KEY_WIDTH;

        for (Control[] row : keyRows) {
            HBox hbox = new HBox();
            hbox.setId("hbox");
            // Primary keyboard has centered keys, secondary has left aligned keys.
            hbox.setAlignment((fxvk.chars != null) ? Pos.CENTER_LEFT : Pos.CENTER);
            vbox.getChildren().add(hbox);
            for (Control c : row) {
                hbox.getChildren().add(c);
                HBox.setHgrow(c, Priority.ALWAYS);
                if (c instanceof Key) {
                    Key key = (Key)c;
                    int textLen = key.getText().length();
                    if (textLen == 1 || !key.getClass().getSimpleName().equals("CharKey")) {
                        //key.setStyle("-fx-font-size: "+primaryFontSize+"px;");
                    } else {
                        //key.setStyle("-fx-font-size: "+(primaryFontSize* Math.min(1.0, 3.0/textLen))+"px;");
                        key.setGraphicTextGap(key.getGraphicTextGap() + 2*textLen);
                    }
                    if (key.getGraphic() instanceof Label) {
                        //((Label)key.getGraphic()).setStyle("-fx-font-size: "+secondaryFontSize+"px;");
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
            Double w = 1.0;
            if (widths != null && widths.get(i) > 0) {
                w = widths.get(i);
            }
            if ("BACKSPACE".equals(str)) {
                CommandKey backspaceKey = new CommandKey("\u232b", BACK_SPACE, w);
                backspaceKey.setId("backspace-key");
// Workaround until we can load -fx-graphic from caspian.css
setIcon(backspaceKey, "fxvk-backspace-button.png");
                keyRow[i] = backspaceKey;
            } else if ("ENTER".equals(str)) {
                CommandKey enterKey = new CommandKey("\u21b5", ENTER, w);
                enterKey.setId("enter-key");
// Workaround until we can load -fx-graphic from caspian.css
setIcon(enterKey, "fxvk-enter-button.png");
                keyRow[i] = enterKey;
            } else if ("SHIFT".equals(str)) {
                shiftKey = new ShiftKey(w);
                shiftKey.setId("shift-key");
// Workaround until we can load -fx-graphic from caspian.css
setIcon(shiftKey, "fxvk-shift-button.png");
                keyRow[i] = shiftKey;
            } else if ("SYM".equals(str)) {
                symbolKey = new SymbolKey("!#123 ABC", w);
                symbolKey.setId("symbol-key");
                keyRow[i] = symbolKey;
            } else {
                keyRow[i] = new CharKey((String)keyList.get(i), w);
            }
        }
        return keyRow;
    }

    private void setState(State state) {
        this.state = state;

        shiftKey.setPressState(state == State.SHIFTED || state == State.SHIFT_LOCK);
        shiftKey.setDisable(state == State.NUMERIC);
        shiftKey.setId((state == State.SHIFT_LOCK) ? "capslock-key" : "shift-key");

// Workaround until we can load -fx-graphic from caspian.css
switch (state) {
    case NUMERIC: setIcon(shiftKey, null); break;
    case SHIFT_LOCK: setIcon(shiftKey, "fxvk-capslock-button.png"); break;
    default: setIcon(shiftKey, "fxvk-shift-button.png");
}
        if (fxvk == secondaryVK) {
            ((FXVKSkin)primaryVK.getSkin()).updateLabels();
        } else {
            updateLabels();
        }
    }

// Workaround until we can load -fx-graphic from caspian.css
private void setIcon(Key key, String fileName) {
    if (fileName != null) {
        String url = getClass().getResource("caspian/"+fileName).toExternalForm();
        key.setGraphic(new javafx.scene.image.ImageView(url));
    } else {
        key.setGraphic(null);
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
                        if (key.chars.length > 2 && !Character.isLetter(key.chars[2].charAt(0))) {
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
        private double keyWidth;

        private Key(String text, double keyWidth) {
            super(text);

            this.keyWidth = keyWidth;

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
                        setState(State.NORMAL);
                    }
                }

                if (fxvk == secondaryVK) {
                    showSecondaryVK(null);
                }
            }
        };

        CharKey(String str, double width) {
            super(null, width);

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
            setGraphicTextGap(-16);
            setText(chars[0]);
            setId(chars[0]);
            if (getText().length() > 1) {
                getStyleClass().add("multi-char-key");
            }

            graphic = new Label((chars.length > 1) ? chars[1] : " ");
            graphic.setPrefWidth(keyWidth - 6);
            graphic.setMinWidth(USE_PREF_SIZE);
            graphic.setPrefHeight(keyHeight / 2 - 8);
            setGraphic(graphic);
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
                        setState(State.NORMAL);
                    }
                }
            }
        };

        CommandKey(String label, KeyCode code, double width) {
            super(label, width);
            this.code = code;
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            getStyleClass().add("special-key");
            setOnAction(actionHandler);
        }
    }

    private class ShiftKey extends Key {
        EventHandler<ActionEvent> actionHandler = new EventHandler<ActionEvent>() {
            long lastTime = -1L;

            @Override public void handle(ActionEvent e) {
                showSecondaryVK(null);
                long time = System.currentTimeMillis();
                if (lastTime > 0L && time - lastTime < 600L) {
                    setState(State.SHIFT_LOCK);
                } else if (state == State.SHIFTED || state == State.SHIFT_LOCK) {
                    setState(State.NORMAL);
                } else {
                    setState(State.SHIFTED);
                }
                lastTime = time;
            }
        };

        ShiftKey(double width) {
            super("\u21d1", width);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            getStyleClass().add("special-key");
            setFocusTraversable(false);
            setOnAction(actionHandler);
        }

        private void setPressState(boolean pressed) {
            setPressed(pressed);
        }
    }

    private class SymbolKey extends Key {
        String str;
        String[] chars;

        EventHandler<ActionEvent> actionHandler = new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                setState((state == State.NUMERIC) ? State.NORMAL : State.NUMERIC);
                showSecondaryVK(null);
            }
        };

        SymbolKey(String str, double width) {
            super(null, width);
            this.str = str;
            getStyleClass().add("special-key");

            if (str.length() == 1) {
                chars = new String[] { str };
            } else {
                chars = str.split(" ");
            }
            setText(chars[0]);

            setOnAction(actionHandler);
        }
    }

    @Override public void layoutChildren() {
        double kw, kh;
        Insets insets = getInsets();
        if (vbox == null) {
            createKeys();
        }
        HBox hbox = (HBox)vbox.getChildren().get(0);
        double hGap = hbox.getSpacing();

        double maxWidth = 0;
        int maxNKeys = 0;
        for (Node vnode : vbox.getChildren()) {
            hbox = (HBox)vnode;
            int nKeys = 0;
            double totalWidth = 0;
            for (Node hnode : hbox.getChildren()) {
                Key key = (Key)hnode;
                nKeys++;
                totalWidth += key.keyWidth;
            }

            maxNKeys = Math.max(maxNKeys, nKeys);
            maxWidth = Math.max(maxWidth, totalWidth);
        }


        if (fxvk == secondaryVK) {
            kw = PREF_PORTRAIT_KEY_WIDTH;
            kh = ((FXVKSkin)primaryVK.getSkin()).keyHeight;
        } else {
            kw = (hbox.getWidth() - (maxNKeys - 1) * hGap) / Math.max(maxWidth, 10.0);
            kh = (getHeight() - insets.getTop() - insets.getBottom() - (keyRows.length - 1) * vbox.getSpacing()) / keyRows.length;
        }

        if (keyWidth != kw || keyHeight != kh) {
            keyWidth = kw;
            keyHeight = kh;
            createKeys();
        }

        super.layoutChildren();

        for (Node vnode : vbox.getChildren()) {
            hbox = (HBox)vnode;
            int nKeys = 0;
            int nSpecialKeys = 0;
            double totalWidth = 0;
            for (Node hnode : hbox.getChildren()) {
                Key key = (Key)hnode;
                nKeys++;
                if (key.keyWidth > 1.0) {
                    nSpecialKeys++;
                }
                totalWidth += key.keyWidth;
            }

            double slop = hbox.getWidth() - (nKeys - 1) * hGap - totalWidth * kw;
            for (Node hnode : hbox.getChildren()) {
                Key key = (Key)hnode;
                // Add slop if not landscape numerical keyboard. (Better if specified in props).
                if ((fxvk.vkType != 1 || fxvk.getStyleClass().contains("fxvk-portrait")) &&
                    slop > 0 && key.keyWidth > 1.0) {

                    key.setPrefWidth(key.keyWidth * keyWidth + slop / nSpecialKeys);
                } else {
                    key.setPrefWidth(key.keyWidth * keyWidth);
                }
            }
        }
    }

    private void showSecondaryVK(final CharKey key) {
        if (key != null) {
            primaryVK = fxvk;
            final Node textInput = primaryVK.getAttachedNode();

            if (secondaryVK == null) {
                secondaryVK = new FXVK();
                secondaryVK.getStyleClass().addAll("fxvk-secondary", "fxvk-portrait");
                secondaryVK.setSkin(new FXVKSkin(secondaryVK));
                secondaryPopup = new Popup();
                secondaryPopup.setAutoHide(true);
                secondaryPopup.getContent().add(secondaryVK);
                // TODO: call to impl_processCSS is probably not needed here
                secondaryVK.impl_processCSS(false);
            }

            if (state == State.NUMERIC) {
                ArrayList<String> symbols = new ArrayList<String>();
                for (String ch : key.chars) {
                    if (!Character.isLetter(ch.charAt(0))) {
                        symbols.add(ch);
                    }
                }
                secondaryVK.chars = symbols.toArray(new String[symbols.size()]);
            } else {
                ArrayList<String> secondaryChars = new ArrayList<String>();
                // Add all letters
                for (String ch : key.chars) {
                    if (Character.isLetter(ch.charAt(0))) {
                        if (state == State.SHIFTED || state == State.SHIFT_LOCK) {
                            secondaryChars.add(ch.toUpperCase());
                        } else {
                            secondaryChars.add(ch);
                        }
                    }
                }
                // Add secondary character if not a letter
                if (key.chars.length > 1 &&
                    !Character.isLetter(key.chars[1].charAt(0))) {
                    secondaryChars.add(key.chars[1]);
                }
                secondaryVK.chars = secondaryChars.toArray(new String[secondaryChars.size()]);
            }

            if (secondaryVK.chars.length > 1) {
                if (secondaryVK.getSkin() != null) {
                    ((FXVKSkin)secondaryVK.getSkin()).createKeys();
                }

                secondaryVK.setAttachedNode(textInput);
                FXVKSkin primarySkin = (FXVKSkin)primaryVK.getSkin();
                FXVKSkin secondarySkin = (FXVKSkin)secondaryVK.getSkin();
                Insets insets = secondarySkin.getInsets();
                int nKeys = secondaryVK.chars.length;
                int nRows = (int)Math.floor(Math.sqrt(Math.max(1, nKeys - 2)));
                int nKeysPerRow = (int)Math.ceil(nKeys / (double)nRows);
                HBox hbox = (HBox)vbox.getChildren().get(0);
                final double w = insets.getLeft() + insets.getRight() +
                                 nKeysPerRow * PREF_PORTRAIT_KEY_WIDTH + (nKeysPerRow - 1) * hbox.getSpacing();
                final double h = insets.getTop() + insets.getBottom() +
                                 nRows * primarySkin.keyHeight + (nRows-1) * vbox.getSpacing();
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
}
