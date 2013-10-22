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

package com.sun.javafx.scene.control.skin;

import javafx.animation.Animation;
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
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.Scene;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import com.sun.javafx.css.StyleManager;
import com.sun.javafx.scene.control.behavior.BehaviorBase;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.TouchEvent.TOUCH_PRESSED;
import static javafx.scene.layout.Region.USE_PREF_SIZE;
import java.security.AccessController;
import java.security.PrivilegedAction;


public class FXVKSkin extends BehaviorSkinBase<FXVK, BehaviorBase<FXVK>> {

    private static final int GAP = 6;

    private List<List<Key>> board;
    private int numCols;

    private boolean capsDown = false;
    private boolean shiftDown = false;
    private boolean isSymbol = false;
    long lastTime = -1L;

    void clearShift() {
        if (shiftDown && !capsDown) {
            shiftDown = false;
            updateKeys();
        }
        lastTime = -1L;
    }

    void pressShift() {
        long time = System.currentTimeMillis();
        
        //potential for a shift lock
        if (shiftDown && !capsDown) {
            if (lastTime > 0L && time - lastTime < 400L) {
                //set caps lock
                shiftDown = false;
                capsDown =  true;
            } else {
                //set normal
                shiftDown = false;
                capsDown =  false;
            }
        } else if (!shiftDown && !capsDown) {
            // set shift
            shiftDown=true;
        } else {
            //set to normal
            shiftDown = false;
            capsDown =  false;
        }
        
        updateKeys();
        lastTime = time;
    }

    void clearSymbolABC() {
        isSymbol = false;
        updateKeys();
    }

    void pressSymbolABC() {
        isSymbol = !isSymbol;
        updateKeys();
    }


    private void updateKeys() {
        for (List<Key> row : board) {
            for (Key key : row) {
                key.update(capsDown, shiftDown, isSymbol);
            }
        }
    }

    private final static boolean USE_SECONDARY_POPUP = false;

    private static Region oldRoot;
    private static Timeline slideRootTimeline;

    private static Popup vkPopup;
    private static Popup secondaryPopup;
    private static FXVK primaryVK;

    private static Timeline slideInTimeline = new Timeline();
    private static Timeline slideOutTimeline = new Timeline();

    private static FXVK secondaryVK;
    private static Timeline secondaryVKDelay;
    private static CharKey secondaryVKKey;

    private Node attachedNode;
    private String vkType;

    FXVK fxvk;

    static final double VK_WIDTH = 640;
    static final double VK_HEIGHT = 243;
    static final double VK_PORTRAIT_HEIGHT = 326;
    static final double VK_SLIDE_MILLIS = 250;
    static final double PREF_KEY_WIDTH = 56;
    static final double PREF_PORTRAIT_KEY_WIDTH = 40;
    static final double PREF_KEY_HEIGHT = 56;

    double keyWidth = PREF_KEY_WIDTH;
    double keyHeight = PREF_KEY_HEIGHT;

    static boolean vkAdjustWindow = false;

    static {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override public Void run() {
                String s = System.getProperty("com.sun.javafx.vk.adjustwindow");
                if (s != null) {
                    vkAdjustWindow = Boolean.valueOf(s);
                }
                return null;
            }
        });
    }    
    
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

    private static void startSlideIn() {
        slideOutTimeline.stop();
        winY.set(vkPopup.getY());
        slideInTimeline.playFromStart();
    }

    private static void startSlideOut() {
        slideInTimeline.stop();
        winY.set(vkPopup.getY());
        slideOutTimeline.playFromStart();
    }

    private void adjustWindowPosition(final Node node) {
        if ( !(node instanceof TextInputControl) ) {
            return;
        }

        // attached node y position in window coordinates
        double inputControlMinY = node.localToScene(0.0, 0.0).getY() + node.getScene().getY();
        double inputControlHeight = ((TextInputControl) node).getHeight();
        double inputControlMaxY = inputControlMinY + inputControlHeight; 

        double screenHeight =
            com.sun.javafx.Utils.getScreen(node).getBounds().getHeight();
        double visibleAreaMaxY = screenHeight - VK_HEIGHT;

        double inputLineCenterY = 0.0;
        double inputLineBottomY = 0.0;
        double newWindowYPos = 0.0;
        double screenTopOffset = 10.0;

        if (node instanceof TextField) {
            inputLineCenterY = inputControlMinY + inputControlHeight / 2;
            inputLineBottomY = inputControlMaxY;
            //check for combo box
            Parent parent = attachedNode.getParent();
            if (parent instanceof ComboBoxBase) {
                //combo box
                // position near screen top
                newWindowYPos = Math.min(screenTopOffset - inputControlMinY, 0);
            } else {
                // position at center of visible screen area
                newWindowYPos = Math.min(visibleAreaMaxY / 2 - inputLineCenterY, 0);
            }
        } else if (node instanceof TextArea) {
            TextAreaSkin textAreaSkin = (TextAreaSkin)((TextArea)node).getSkin();
            Bounds caretBounds = textAreaSkin.getCaretBounds();
            double caretMinY = caretBounds.getMinY();
            double caretMaxY = caretBounds.getMaxY();
            inputLineCenterY = inputControlMinY + ( caretMinY + caretMaxY ) / 2;
            inputLineBottomY = inputControlMinY + caretMaxY;

            if (inputControlHeight < visibleAreaMaxY) {
                // position at center of visible screen area
                newWindowYPos = visibleAreaMaxY / 2 - (inputControlMinY + inputControlHeight / 2);
            } else {
                // position the line containing the caret at center of visible screen area
                newWindowYPos = visibleAreaMaxY / 2 - inputLineCenterY;
            }
            newWindowYPos = Math.min(newWindowYPos, 0);

        } else {
            inputLineCenterY = inputControlMinY + inputControlHeight / 2;
            inputLineBottomY = inputControlMaxY;
            // position at center of visible screen area
            newWindowYPos = Math.min(visibleAreaMaxY / 2 - inputLineCenterY, 0);
        }
       
        Window w = node.getScene().getWindow();
        if (origWindowYPos + inputLineBottomY > visibleAreaMaxY) {
            w.setY(newWindowYPos);
        } else {
            w.setY(origWindowYPos);
        }
    }

    private void saveWindowPosition(final Node node) {
        Window w = node.getScene().getWindow();
        origWindowYPos = w.getY();
    }

    private void restoreWindowPosition(final Node node) {
        if (node != null) {
            Scene scene = node.getScene();
            if (scene != null) {
                Window window = scene.getWindow();
                if (window != null) {
                    window.setY(origWindowYPos);
                }
            }
        }
    }

    EventHandler<InputEvent> unHideEventHandler;

    private boolean isVKHidden = false;
    private Double origWindowYPos = null;
    
    private void registerUnhideHandler(final Node node) {
        if (unHideEventHandler == null) {
            unHideEventHandler = new EventHandler<InputEvent> () {
                public void handle(InputEvent event) {
                    if (attachedNode != null && isVKHidden) {
                        double screenHeight = com.sun.javafx.Utils.getScreen(attachedNode).getBounds().getHeight();
                        if (fxvk.getHeight() > 0 && (vkPopup.getY() > screenHeight - fxvk.getHeight())) {
                            if (slideInTimeline.getStatus() != Animation.Status.RUNNING) {
                                startSlideIn();
                                if (vkAdjustWindow) {
                                    adjustWindowPosition(attachedNode);
                                }
                            }
                        }
                    }
                    isVKHidden = false;
                }                    
            };
        }
        node.addEventHandler(TOUCH_PRESSED, unHideEventHandler);
        node.addEventHandler(MOUSE_PRESSED, unHideEventHandler);
    }

    private void unRegisterUnhideHandler(Node node) {
        if (unHideEventHandler != null) {
            node.removeEventHandler(TOUCH_PRESSED, unHideEventHandler);
            node.removeEventHandler(MOUSE_PRESSED, unHideEventHandler);
        }
    }

    private void updateKeyboardType() {
        String oldType = vkType;
        int typeIndex = 0;
        Object typeValue = attachedNode.getProperties().get(FXVK.VK_TYPE_PROP_KEY);
        String typeStr = null;
        if (typeValue instanceof String) {
            typeStr = ((String)typeValue).toLowerCase(Locale.ROOT);
        }
        vkType = (typeStr != null ? typeStr : "text");
        
        //VK type changed, rebuild
        if ( oldType == null || !vkType.equals(oldType) ) {
            rebuild();
        }
    }

    public FXVKSkin(final FXVK fxvk) {
        super(fxvk, new BehaviorBase<>(fxvk, Collections.EMPTY_LIST));
        this.fxvk = fxvk;

        StyleManager.getInstance().addUserAgentStylesheet("com/sun/javafx/scene/control/skin/caspian/fxvk.css");

        fxvk.setFocusTraversable(false);

        if (fxvk != secondaryVK) {
            //init secondary VK delay animation
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
        }


        fxvk.attachedNodeProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                Node oldNode = attachedNode;
                attachedNode = fxvk.getAttachedNode();
                isVKHidden = false;
                if (fxvk != FXVK.vk) {
                    // This is not the current vk, so nothing more to do
                    return;
                }
                if (fxvk == secondaryVK) {
                    return;
                }
                
                //close secondary VK if open
                if (secondaryVK != null) {
                    secondaryVK.setAttachedNode(null);
                    secondaryPopup.hide();
                }
                
                if (attachedNode != null) {
                    if (oldNode != null) {
                        unRegisterUnhideHandler(oldNode);
                    }
                    registerUnhideHandler(attachedNode);
                    updateKeyboardType();
                    
                    fxvk.setVisible(true);

                    if (fxvk != secondaryVK) {
                        // init popup window and slide animations
                        if (vkPopup == null) {
                            vkPopup = new Popup();
                            vkPopup.setAutoFix(false);

                            double screenHeight =
                                com.sun.javafx.Utils.getScreen(attachedNode).getBounds().getHeight();
                            double screenVisualHeight =
                                com.sun.javafx.Utils.getScreen(attachedNode).getVisualBounds().getHeight();

                            screenVisualHeight = Math.min(screenHeight, screenVisualHeight + 4 /*??*/);

                            slideInTimeline.getKeyFrames().setAll(
                                new KeyFrame(Duration.millis(VK_SLIDE_MILLIS),
                                             new KeyValue(winY, screenHeight - VK_HEIGHT,
                                                          Interpolator.EASE_BOTH)));
                            slideOutTimeline.getKeyFrames().setAll(
                                new KeyFrame(Duration.millis(VK_SLIDE_MILLIS),
                                             new KeyValue(winY, screenHeight, Interpolator.EASE_BOTH)));
                        }

                        vkPopup.getContent().setAll(fxvk);

                        //owner window has changed so hide VK and show with new owner
                        if (oldNode == null || oldNode.getScene() == null || oldNode.getScene().getWindow() != attachedNode.getScene().getWindow()) {
                            if (vkPopup.isShowing()) {
                                vkPopup.hide();
                            }
                        }
                        
                        if (!vkPopup.isShowing()) {
                            Platform.runLater(new Runnable() {
                                public void run() {
                                    Rectangle2D screenBounds =
                                        com.sun.javafx.Utils.getScreen(attachedNode).getBounds();

                                    vkPopup.show(attachedNode.getScene().getWindow(),
                                                 (screenBounds.getWidth() - fxvk.prefWidth(-1)) / 2,
                                                 screenBounds.getHeight() - fxvk.prefHeight(-1));
                                }
                            });
                        }

                        if (oldNode == null || oldNode.getScene() != attachedNode.getScene()) {
                            double width = com.sun.javafx.Utils.getScreen(attachedNode).getBounds().getWidth();
                            fxvk.setPrefWidth(width);
                            fxvk.setMinWidth(USE_PREF_SIZE);
                            fxvk.setMaxWidth(USE_PREF_SIZE);
                            
                            fxvk.setPrefHeight(VK_HEIGHT);
                            fxvk.setMinHeight(USE_PREF_SIZE);
                        }

                        if (fxvk.getHeight() > 0 &&
                                (fxvk.getLayoutY() == 0 || fxvk.getLayoutY() > attachedNode.getScene().getHeight() - fxvk.getHeight())) {
                            startSlideIn();
                        }
                        
                        //update previous window position only if moving from non-input control node or window has changed.
                        if (vkAdjustWindow) {
                            if (oldNode == null || oldNode.getScene() == null 
                                || oldNode.getScene().getWindow() != attachedNode.getScene().getWindow()) {
                                saveWindowPosition(attachedNode);
                            }
                        }

                        // Move window containing input node
                        if (vkAdjustWindow) {
                            adjustWindowPosition(attachedNode);
                        }
                    }
                } else {
                    if (fxvk != secondaryVK) {
                        if (oldNode != null) {
                            unRegisterUnhideHandler(oldNode);
                        }
                        startSlideOut();
                        // Restore window position
                        if (vkAdjustWindow) {
                            restoreWindowPosition(oldNode);
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

    /**
     * Replaces all children of this VirtualKeyboardSkin based on the keyboard
     * type set on the VirtualKeyboard.
     */
    private void rebuild() {
        if (fxvk == secondaryVK) {
            //build secondary VK
            if (secondaryVK.chars == null) {
            } else {
                int nKeys = secondaryVK.chars.length;
                int nRows = (int)Math.floor(Math.sqrt(Math.max(1, nKeys - 2)));
                int nKeysPerRow = (int)Math.ceil(nKeys / (double)nRows);

                Key tmpKey;
                List<List<Key>> rows = new ArrayList<List<Key>>(2);

                for (int i = 0; i < nRows; i++) {
                    int start = i * nKeysPerRow;
                    int end = Math.min(start + nKeysPerRow, nKeys);
                    if (start >= end) 
                        break;
                        
                    List<Key> keys = new ArrayList<Key>(nKeysPerRow);
                    for (int j = start; j < end; j++) {
                        tmpKey = new CharKey(secondaryVK.chars[j], null, null);
                        tmpKey.col= (j - start) * 2;
                        tmpKey.colSpan = 2;
                        for (String sc : tmpKey.getStyleClass()) {
                            tmpKey.text.getStyleClass().add(sc + "-text");
                            tmpKey.altText.getStyleClass().add(sc + "-alttext");
                            tmpKey.icon.getStyleClass().add(sc + "-icon");
                        }
                        if (secondaryVK.chars[j] != null && secondaryVK.chars[j].length() > 1) {
                            tmpKey.text.getStyleClass().add("multi-char-text");
                        }
                        keys.add(tmpKey);
                    }
                    rows.add(keys);
                }
                board = rows;
                
                getChildren().clear();
                numCols = 0;
                for (List<Key> row : board) {
                    for (Key key : row) {
                        numCols = Math.max(numCols, key.col + key.colSpan);
                    }
                    getChildren().addAll(row);
                }
            }
        } else {
            String boardName;

            switch (vkType) {
                case "text":
                    boardName = "TextBoard";
                    break;
                case "numeric":
                    boardName = "NumericBoard";
                    break;
                case "url":
                    boardName = "UrlBoard";
                    break;
                case "email":
                    boardName = "EmailBoard";
                    break;
                default:
                    boardName = "TextBoard";
            }
            
            board = loadBoard(boardName);
            getChildren().clear();
            numCols = 0;
            for (List<Key> row : board) {
                for (Key key : row) {
                    numCols = Math.max(numCols, key.col + key.colSpan);
                }
                getChildren().addAll(row);
            }
        }

    }

    // This skin is designed such that it gives equal widths to all columns. So
    // the pref width is just some hard-coded value (although I could have maybe
    // done it based on the pref width of a text node with the right font).
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return leftInset + (56 * numCols) + rightInset;
    }

    // Pref height is just some value. This isn't overly important.
    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return topInset + (80 * 5) + bottomInset;
    }

    // Lays the buttons comprising the current keyboard out. 
    @Override
    protected void layoutChildren(double contentX, double contentY, double contentWidth, double contentHeight) {
        // I have fixed width columns, all the same.
        int numRows = board.size();
        final double colWidth = ((contentWidth - ((numCols - 1) * GAP)) / numCols);
        double rowHeight = ((contentHeight - ((numRows - 1) * GAP)) / numRows);
        double rowY = contentY;
        for (List<Key> row : board) {
            for (Key key : row) {
                double startX = contentX + (key.col * (colWidth + GAP));
                double width = (key.colSpan * (colWidth + GAP)) - GAP;
                key.resizeRelocate((int)(startX + .5), (int)(rowY + .5),
                                   width, rowHeight);
            }
            rowY += rowHeight + GAP;
        }
    }


    /**
     * A Key on the virtual keyboard. This is simply a Region. Some information
     * about the key relative to other keys on the layout is given by the col
     * and colSpan fields.
     */
    private class Key extends Region {
        int col = 0;
        int colSpan = 1;
        protected final Text text;
        protected final Text altText;
        protected final Region icon;

        protected Key() {
            icon = new Region();
            text = new Text();
            text.setTextOrigin(VPos.TOP);
            altText = new Text();
            altText.setTextOrigin(VPos.TOP);
            getChildren().setAll(text, altText, icon);
            getStyleClass().setAll("key");
            addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    if (event.getEventType() == MouseEvent.MOUSE_PRESSED)
                        press();
                    else if (event.getEventType() == MouseEvent.MOUSE_RELEASED)
                        release();
                }
            });
        }
        protected void press() { }
        protected void release() {
            clearShift();
        }

        public void update(boolean capsDown, boolean shiftDown, boolean isSymbol) { }

        @Override protected void layoutChildren() {
            final double left = snappedLeftInset();
            final double top = snappedTopInset();
            final double width = getWidth() - left - snappedRightInset();
            final double height = getHeight() - top - snappedBottomInset();

            text.setVisible(icon.getBackground() == null);
            double contentPrefWidth = text.prefWidth(-1);
            double contentPrefHeight = text.prefHeight(-1);
            text.resizeRelocate(
                    (int) (left + ((width - contentPrefWidth) / 2) + .5),
                    (int) (top + ((height - contentPrefHeight) / 2) + .5),
                    (int) contentPrefWidth,
                    (int) contentPrefHeight);

            altText.setVisible(icon.getBackground() == null && altText.getText().length() > 0);
            contentPrefWidth = altText.prefWidth(-1);
            contentPrefHeight = altText.prefHeight(-1);
            altText.resizeRelocate(
                    (int) left + (width - contentPrefWidth) + .5,
                    (int) (top + ((height - contentPrefHeight) / 2) + .5 - height/2),
                    (int) contentPrefWidth,
                    (int) contentPrefHeight);

            icon.resizeRelocate(left-8, top-8, width+16, height+16);
        }

    }

    /**
     * Any key on the keyboard which will send a KeyEvent to the client. This
     * class just maintains the state and logic for firing an event, using the
     * "chars" and "code" as the values sent in the event. A subclass must set
     * these appropriately.
     */
    private class TextInputKey extends Key {
        String chars = "";

        protected void press() {
        }
        protected void release() {
            if (fxvk != secondaryVK && secondaryPopup != null && secondaryPopup.isShowing()) {
                return;
            }
            sendKeyEvents();
            if (fxvk == secondaryVK) {
                showSecondaryVK(null);
            }
            super.release();
        }

        protected void sendKeyEvents() {
            Node target = fxvk.getAttachedNode();
            if (target instanceof EventTarget) {
                if (chars != null) {
                    target.fireEvent(new KeyEvent(KeyEvent.KEY_TYPED, chars, "", KeyCode.UNDEFINED, shiftDown, false, false, false));
                }
            }
        }
    }

    /**
     * A key which has a letter, a number or symbol on it
     * 
     */
    private class CharKey extends TextInputKey {
        private final String letterChars;
        private final String altChars;
        private final String[] moreChars;

        private CharKey(String letter, String alt, String[] moreChars) {
            setId(letter);
            this.letterChars = letter;
            this.altChars = alt;
            this.moreChars = moreChars;
            this.chars = this.letterChars;

            text.setText(this.chars);
            altText.setText(this.altChars);

            handleSecondaryVK(letter, alt, moreChars);
        }


        private void handleSecondaryVK(String letter, String alt, String[] moreChars) {
            // If key has only one char (alternative char is the same, and it has no more chars),
            // secondaryVK will not pop-up
            if (letter.equals(alt) && moreChars == null) {
                return;
            } else {
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
            }
        }

        @Override public void update(boolean capsDown, boolean shiftDown, boolean isSymbol) {
            if (isSymbol) {
                chars = altChars;
                text.setText(chars);
                if (moreChars != null && moreChars.length > 0 && !Character.isLetter(moreChars[0].charAt(0))) {
                    altText.setText(moreChars[0]);
                } else {
                    altText.setText(null);
                }
            } else {
                chars = (capsDown || shiftDown) ? letterChars.toUpperCase() : letterChars.toLowerCase();
                text.setText(chars);
                altText.setText(altChars);
            }
        }
    }

    /**
     * One of several TextInputKeys which have super powers, such as "Tab" and
     * "Return" and "Backspace". These keys still send events to the client,
     * but may also have additional state related functionality on the keyboard
     * such as the "Shift" key.
     */
    private class SuperKey extends TextInputKey {
        private SuperKey(String letter, String code) {
            this.chars = code;
            text.setText(letter);
            getStyleClass().add("special");
            setId(letter);
        }
    }

    /**
     * Some keys actually do need to use KeyCode for pressed / released events,
     * and BackSpace is one of them.
     */
    private class KeyCodeKey extends SuperKey {
        private KeyCode code;

        private KeyCodeKey(String letter, String c, KeyCode code) {
            super(letter, c);
            this.code = code;
            setId(letter);
        }

        protected void sendKeyEvents() {
            Node target = fxvk.getAttachedNode();
            if (target instanceof EventTarget) {               
                target.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, KeyEvent.CHAR_UNDEFINED, chars, code, shiftDown, false, false, false));
                target.fireEvent(new KeyEvent(KeyEvent.KEY_TYPED, chars, "", KeyCode.UNDEFINED, shiftDown, false, false, false));
                target.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, KeyEvent.CHAR_UNDEFINED, chars, code, shiftDown, false, false, false));
            }
        }
    }

    /**
     * These keys only manipulate the state of the keyboard and never
     * send key events to the client. For example, "Hide", "Caps Lock",
     * etc are all KeyboardStateKeys.
     */
    private class KeyboardStateKey extends Key {
        private final String defaultText;
        private final String toggledText;

        private KeyboardStateKey(String defaultText, String toggledText) {
            this.defaultText = defaultText;
            this.toggledText = toggledText;
            text.setText(this.defaultText);
            setId(this.defaultText);
            getStyleClass().add("special");
        }

        @Override public void update(boolean capsDown, boolean shiftDown, boolean isSymbol) {
            //change icon
            
            if (isSymbol) {
                text.setText(this.toggledText);
                setId(this.toggledText);
            } else {
                text.setText(this.defaultText);
                setId(this.defaultText);
            }
        }
    }

    private void showSecondaryVK(final CharKey key) {
        if (key != null) {
            primaryVK = fxvk;
            final Node textInput = primaryVK.getAttachedNode();

            if (secondaryVK == null) {
                secondaryVK = new FXVK();
                //secondaryVK.getStyleClass().addAll("fxvk-secondary", "fxvk-portrait");
                secondaryVK.setSkin(new FXVKSkin(secondaryVK));
                secondaryVK.getStyleClass().setAll("fxvk-secondary");
                secondaryPopup = new Popup();
                secondaryPopup.setAutoHide(true);
                secondaryPopup.getContent().add(secondaryVK);
            }
           
            secondaryVK.chars=null;
            ArrayList<String> secondaryList = new ArrayList<String>();

            // Add primary character
            if (!isSymbol) {
                if (key.letterChars != null && key.letterChars.length() > 0) {
                    if (shiftDown || capsDown) {
                        secondaryList.add(key.letterChars.toUpperCase());
                    } else {
                        secondaryList.add(key.letterChars);
                    }
                }
            }

            // Add secondary character
            if (key.altChars != null && key.altChars.length() > 0) {
                if (shiftDown || capsDown) {
                    secondaryList.add(key.altChars.toUpperCase());
                } else {
                    secondaryList.add(key.altChars);
                }
            }
            
            // Add more letters
            if (key.moreChars != null && key.moreChars.length > 0) {
                if (isSymbol) {
                    //Add non-letters
                    for (String ch : key.moreChars) {
                        if (!Character.isLetter(ch.charAt(0))) {
                            secondaryList.add(ch);
                        }
                    }
                 } else {
                    //Add letters
                    for (String ch : key.moreChars) {
                        if (Character.isLetter(ch.charAt(0))) {
                            if (shiftDown || capsDown) {
                                secondaryList.add(ch.toUpperCase());
                            } else {
                                secondaryList.add(ch);
                            }
                        }
                    }
                }
            }
            
            boolean isMultiChar = false;
            for (String s : secondaryList) {
                if (s.length() > 1 ) {
                    isMultiChar = true;
                }
            }
            
            secondaryVK.chars = secondaryList.toArray(new String[secondaryList.size()]);

            if (secondaryVK.chars.length > 1) {
                if (secondaryVK.getSkin() != null) {
                    ((FXVKSkin)secondaryVK.getSkin()).rebuild();
                }

                secondaryVK.setAttachedNode(textInput);
                FXVKSkin primarySkin = (FXVKSkin)primaryVK.getSkin();
                FXVKSkin secondarySkin = (FXVKSkin)secondaryVK.getSkin();
                //Insets insets = secondarySkin.getInsets();
                int nKeys = secondaryVK.chars.length;
                int nRows = (int)Math.floor(Math.sqrt(Math.max(1, nKeys - 2)));
                int nKeysPerRow = (int)Math.ceil(nKeys / (double)nRows);
                
                final double w = snappedLeftInset() + snappedRightInset() +
                                 nKeysPerRow * PREF_PORTRAIT_KEY_WIDTH * (isMultiChar ? 2 : 1) + (nKeysPerRow - 1) * GAP;
                final double h = snappedTopInset() + snappedBottomInset() +
                                 nRows * PREF_KEY_HEIGHT + (nRows-1) * GAP;

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



    private List<List<Key>> loadBoard(String boardName) {
        try {
            List<List<Key>> rows = new ArrayList<List<Key>>(5);
            List<Key> keys = new ArrayList<Key>(20);

            InputStream boardFile = FXVKSkin.class.getResourceAsStream(boardName + ".txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(boardFile));
            String line;
            // A pointer to the current column. This will be incremented for every string
            // of text, or space.
            int c = 0;
            // The col at which the key will be placed
            int col = 0;
            // The number of columns that the key will span
            int colSpan = 1;
            // Whether the "chars" is an identifier, like $shift or $SymbolBoard, etc.
            boolean identifier = false;
            // The textual content of the Key
            List<String> charsList = new ArrayList<String>(10);

            while ((line = reader.readLine()) != null) {
                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }
                // A single line represents a single row of buttons
                for (int i=0; i<line.length(); i++) {
                    char ch = line.charAt(i);

                    // Process the char
                    if (ch == ' ') {
                        c++;
                    } else if (ch == '[') {
                        // Start of a key
                        col = c;
                        charsList = new ArrayList<String>(10);
                        identifier = false;
                    } else if (ch == ']') {
                        String chars = "";
                        String alt = null;
                        String[] moreChars = null;

                        for (int idx = 0; idx < charsList.size(); idx++) {
                            charsList.set(idx, FXVKCharEntities.get(charsList.get(idx)));
                        }
                
                        int listSize = charsList.size();
                        if (listSize > 0) {
                            chars = charsList.get(0);
                            if (listSize > 1) {
                                alt = charsList.get(1);
                                if (listSize > 2) {
                                    moreChars = charsList.subList(2, listSize).toArray(new String[listSize - 2]);
                                }
                            }
                        }
                        
                        // End of a key
                        colSpan = c - col;
                        Key key;
                        if (identifier) {
                            if ("$shift".equals(chars)) {
                                key = new KeyboardStateKey("", null) {
                                    @Override protected void release() {
                                        pressShift();
                                    }
                                    
                                    @Override public void update(boolean capsDown, boolean shiftDown, boolean isSymbol) {
                                        if (isSymbol) {
                                            this.setDisable(true);
                                            this.setVisible(false);
                                        } else {
                                            if (capsDown) {
                                                icon.getStyleClass().remove("shift-icon");
                                                icon.getStyleClass().add("capslock-icon");
                                            } else {
                                                icon.getStyleClass().remove("capslock-icon");
                                                icon.getStyleClass().add("shift-icon");
                                            }
                                            this.setDisable(false);
                                            this.setVisible(true);
                                        }
                                    }
                                };
                                key.getStyleClass().add("shift");

                            } else if ("$SymbolABC".equals(chars)) {
                                key = new KeyboardStateKey("!#123", "ABC") {
                                    @Override protected void release() {
                                        pressSymbolABC();
                                    }
                                };
                            } else if ("$backspace".equals(chars)) {
                                key = new KeyCodeKey("backspace", "\b", KeyCode.BACK_SPACE);
                                key.getStyleClass().add("backspace");

                            } else if ("$enter".equals(chars)) {
                                key = new KeyCodeKey("enter", "\n", KeyCode.ENTER);
                                key.getStyleClass().add("enter");
                            } else if ("$tab".equals(chars)) {
                                key = new KeyCodeKey("tab", "\t", KeyCode.TAB);
                            } else if ("$space".equals(chars)) {
                                key = new CharKey(" ", " ", null);
                            } else if ("$clear".equals(chars)) {
                                key = new SuperKey("clear", "");
                            } else if ("$.org".equals(chars)) {
                                key = new SuperKey(".org", ".org");
                            } else if ("$.com".equals(chars)) {
                                key = new SuperKey(".com", ".com");
                            } else if ("$.net".equals(chars)) {
                                key = new SuperKey(".net", ".net");
                            } else if ("$oracle.com".equals(chars)) {
                                key = new SuperKey("oracle.com", "oracle.com");
                            } else if ("$gmail.com".equals(chars)) {
                                key = new SuperKey("gmail.com", "gmail.com");
                            } else if ("$hide".equals(chars)) {
                                key = new KeyboardStateKey("Hide", null) {
                                    @Override protected void release() {
                                        isVKHidden = true;
                                        startSlideOut();
                                        // Restore window position
                                        if (vkAdjustWindow) {
                                            restoreWindowPosition(attachedNode);
                                        }
                                    }
                                };
                                key.getStyleClass().add("hide");
                            } else if ("$undo".equals(chars)) {
                                key = new SuperKey("undo", "");
                            } else if ("$redo".equals(chars)) {
                                key = new SuperKey("redo", "");
                            } else {
                                //Unknown Key
                                key = null;
                            }
                        } else {
                            key = new CharKey(chars, alt, moreChars);
                        }
                        if (key != null) {
                            key.col = col;
                            key.colSpan = colSpan;
                            for (String sc : key.getStyleClass()) {
                                key.text.getStyleClass().add(sc + "-text");
                                key.altText.getStyleClass().add(sc + "-alttext");
                                key.icon.getStyleClass().add(sc + "-icon");
                            }
                            if (chars != null && chars.length() > 1) {
                                key.text.getStyleClass().add("multi-char-text");
                            }
                            if (alt != null && alt.length() > 1) {
                                key.altText.getStyleClass().add("multi-char-text");
                            }

                            keys.add(key);
                        }
                    } else {
                        // Normal textual characters. Read all the way up to the
                        // next ] or space
                        for (int j=i; j<line.length(); j++) {
                            char c2 = line.charAt(j);
                            boolean e = false;
                            if (c2 == '\\') {
                                j++;
                                i++;
                                e = true;
                                c2 = line.charAt(j);
                            }

                            if (c2 == '$' && !e) {
                                identifier = true;
                            }

                            if (c2 == '|' && !e) {
                                charsList.add(line.substring(i, j));
                                i = j + 1;
                            } else if ((c2 == ']' || c2 == ' ') && !e) {
                                charsList.add(line.substring(i, j));
                                i = j-1;
                                break;
                            }
                        }
                        c++;
                    }
                }

                c = 0;
                col = 0;
                rows.add(keys);
                keys = new ArrayList<Key>(20);
            }
            reader.close(); 
            return rows;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
