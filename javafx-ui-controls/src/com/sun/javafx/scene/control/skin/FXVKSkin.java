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

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import javafx.stage.*;
import javafx.util.Duration;

import com.sun.javafx.robot.impl.FXRobotHelper;
import com.sun.javafx.robot.impl.FXRobotHelper.FXRobotInputAccessor;
import com.sun.javafx.scene.control.behavior.BehaviorBase;

import static javafx.scene.layout.Region.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;


public class FXVKSkin extends BehaviorSkinBase<FXVK, BehaviorBase<FXVK>> {

    private static final int GAP = 6;

    private List<List<Key>> board;
    private int numCols;

    private boolean capsDown = false;
    private boolean shiftDown = false;

    void clearShift() {
        shiftDown = false;
        updateKeys();
    }

    void pressShift() {
        shiftDown = !shiftDown;
        updateKeys();
    }

    void pressCaps() {
        capsDown = !capsDown;
        shiftDown = false;
        updateKeys();
    }

    private void updateKeys() {
        for (List<Key> row : board) {
            for (Key key : row) {
                key.update(capsDown, shiftDown);
            }
        }
    }


    /**
     * If true, places the virtual keyboard(s) in a new root wrapper
     * on the scene instead of in a popup. The children of the wrapper
     * are the original root container and a Group of one or more
     * keyboards.
     *
     * This is suitable for a fullscreen application that does not use
     * dialogs with text input controls.
     *
     * The root wrapper pans up/down automatically as needed to keep
     * the focused input control visible, and allows the user to drag
     * up/down with mouse or touch.
     */
    private final static boolean inScene =
        AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override public Boolean run() {
                String value = System.getProperty("com.sun.javafx.fxvkContainerType");
                return ("inscene".equalsIgnoreCase(value));
            }
    });

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

    private Node attachedNode;

    FXVK fxvk;

    enum State { NORMAL, SHIFTED, SHIFT_LOCK, NUMERIC; };

    private State state = State.NORMAL;

    static final double VK_WIDTH = 640;
    static final double VK_HEIGHT = 243;
    static final double VK_PORTRAIT_HEIGHT = 326;
    static final double VK_SLIDE_MILLIS = 250;
    static final double PREF_KEY_WIDTH = 56;
    static final double PREF_PORTRAIT_KEY_WIDTH = 40;
    static final double PREF_KEY_HEIGHT = 56;

    double keyWidth = PREF_KEY_WIDTH;
    double keyHeight = PREF_KEY_HEIGHT;

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

    private static Boolean enableCaching = AccessController.doPrivileged(
    new PrivilegedAction<Boolean>() {
        @Override public Boolean run() {
            return Boolean.getBoolean("com.sun.javafx.scene.control.skin.FXVK.cache");
        }
    });


    @Override protected void handleControlPropertyChanged(String propertyReference) {
        // With Java 8 (or is it 7?) I can do switch on strings instead
        if (propertyReference == "type") {
            // The type has changed, so we will need to rebuild the entire keyboard.
            // This happens whenever the user switches from one keyboard layout to
            // another, such as by pressing the "ABC" key on a numeric layout.
            rebuild();
        }
    }

    public FXVKSkin(final FXVK fxvk) {
        super(fxvk, new BehaviorBase<FXVK>(fxvk));
        this.fxvk = fxvk;

        registerChangeListener(fxvk.typeProperty(), "type");
        rebuild();

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
                    final Scene scene = attachedNode.getScene();
                    //fxvk.getStyleClass().setAll("virtual-keyboard");

                    fxvk.setVisible(true);

                    if (fxvk != secondaryVK) {
                        if (vkPopup == null) {
                            vkPopup = new Popup();
                            vkPopup.setAutoFix(false);

                            Scene popupScene = vkPopup.getScene();
                            popupScene.getStylesheets().add(getClass().getResource("caspian/fxvk.css").toExternalForm());


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

                            slideInTimeline.getKeyFrames().setAll(
                                new KeyFrame(Duration.millis(VK_SLIDE_MILLIS),
                                             new KeyValue(winY, screenHeight - VK_HEIGHT,
                                                          Interpolator.EASE_BOTH)));
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
                            if (!inScene) {
                                double width = com.sun.javafx.Utils.getScreen(attachedNode).getBounds().getWidth();
                                fxvk.setPrefWidth(width);
                            }
                            fxvk.setMinWidth(USE_PREF_SIZE);
                            fxvk.setMaxWidth(USE_PREF_SIZE);
                            fxvk.setPrefHeight(VK_HEIGHT);
                            fxvk.setMinHeight(USE_PREF_SIZE);
                        }
                    }
                } else {
                    if (fxvk != secondaryVK) {

                        slideInTimeline.stop();
                        if (!inScene) {
                            winY.set(vkPopup.getY());
                        }
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

    /**
     * Replaces all children of this VirtualKeyboardSkin based on the keyboard
     * type set on the VirtualKeyboard.
     */
    private void rebuild() {
        String boardName;
        FXVK.Type type = getSkinnable().getType();
        switch (type) {
            case NUMERIC:
                boardName = "SymbolBoard";
                break;
            case TEXT:
                boardName = "AsciiBoard";
                break;
            case EMAIL:
                boardName = "EmailBoard";
                break;
            default:
                throw new AssertionError("Unhandled Virtual Keyboard type");
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

    // This skin is designed such that it gives equal widths to all columns. So
    // the pref width is just some hard-coded value (although I could have maybe
    // done it based on the pref width of a text node with the right font).
    @Override protected double computePrefWidth(double height, int topInset, int rightInset, int bottomInset, int leftInset) {
        return leftInset + (56 * numCols) + rightInset;
    }

    // Pref height is just some value. This isn't overly important.
    @Override protected double computePrefHeight(double width, int topInset, int rightInset, int bottomInset, int leftInset) {
        return topInset + (80 * 5) + bottomInset;
    }

    // Lays the buttons comprising the current keyboard out. The first row is always
    // a "short" row (about 2/3 in height of a normal row), followed by 4 normal rows.
    @Override
    protected void layoutChildren(double contentX, double contentY, double contentWidth, double contentHeight) {
        // I have fixed width columns, all the same.
        final double colWidth = ((contentWidth - ((numCols - 1) * GAP)) / numCols);
        double rowHeight = ((contentHeight - (4 * GAP)) / 5); // 5 rows per keyboard
        // The first row is 2/3 the height
        double firstRowHeight = rowHeight * .666;
        rowHeight += ((rowHeight * .333) / 4);

        double rowY = contentY;
        double h = firstRowHeight;
        for (List<Key> row : board) {
            for (Key key : row) {
                double startX = contentX + (key.col * (colWidth + GAP));
                double width = (key.colSpan * (colWidth + GAP)) - GAP;
                key.resizeRelocate((int)(startX + .5),
                                   (int)(rowY + .5),
                                   width, h);
            }
            rowY += h + GAP;
            h = rowHeight;
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
        protected final Region icon;

        protected Key() {
            icon = new Region();
            text = new Text();
            text.setTextOrigin(VPos.TOP);
            getChildren().setAll(text, icon);
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

        public void update(boolean capsDown, boolean shiftDown) { }

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
        protected String chars = "";

        protected void press() {
            Node target = fxvk.getAttachedNode();
            if (target instanceof EventTarget) {
                target.fireEvent(event(KeyEvent.KEY_PRESSED));
            }
        }
        protected void release() {
            Node target = fxvk.getAttachedNode();
            if (target instanceof EventTarget) {
                target.fireEvent(event(KeyEvent.KEY_TYPED));
                target.fireEvent(event(KeyEvent.KEY_RELEASED));
            }
            super.release();
        }

        protected KeyEvent event(EventType<KeyEvent> type) {
            try {
                Field fld = FXRobotHelper.class.getDeclaredField("inputAccessor");
                fld.setAccessible(true);
                FXRobotInputAccessor inputAccessor = (FXRobotInputAccessor)fld.get(null);

                return inputAccessor.createKeyEvent(type, KeyCode.UNDEFINED, chars, "",
                                      shiftDown, false, false, false);
            } catch (Exception e) {
                System.err.println(e);
            }

            return null;
        }
    }

    /**
     * A key used for letters a-z, and handles responding to the shift & caps lock
     * keys, such that lowercase or uppercase letters are entered.
     */
    private class LetterKey extends TextInputKey {
        private LetterKey(String letter) {
            this.chars = letter;
            text.setText(this.chars);
            setId(letter);
        }

        public void update(boolean capsDown, boolean shiftDown) {
            final boolean capital = capsDown || shiftDown;
            if (capital) {
                this.chars = this.chars.toUpperCase();
                text.setText(this.chars);
            } else {
                this.chars = this.chars.toLowerCase();
                text.setText(this.chars);
            }
        }
    }

    /**
     * A key which has a number or symbol on it, such as the "1" key which can also
     * enter the ! character when shift is pressed. Also used for purely symbolic
     * keys such as [.
     */
    private class SymbolKey extends TextInputKey {
        private final String letterChars;
        private final String altChars;

        private SymbolKey(String letter, String alt) {
            this.chars = letter;
            this.letterChars = this.chars;
            this.altChars = alt;
            text.setText(this.letterChars);
            setId(letter);
        }

        public void update(boolean capsDown, boolean shiftDown) {
            if (shiftDown && altChars != null) {
                this.chars = altChars;
                text.setText(this.chars);
            } else {
                this.chars = letterChars;
                text.setText(this.chars);
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

        protected KeyEvent event(EventType<KeyEvent> type) {
            if (type == KeyEvent.KEY_PRESSED || type == KeyEvent.KEY_RELEASED) {
                try {
                    Field fld = FXRobotHelper.class.getDeclaredField("inputAccessor");
                    fld.setAccessible(true);
                    FXRobotInputAccessor inputAccessor = (FXRobotInputAccessor)fld.get(null);

                    return inputAccessor.createKeyEvent(type, code, chars, chars,
                                          shiftDown, false, false, false);
                } catch (Exception e) {
                    System.err.println(e);
                }
                return null;
            } else {
                return super.event(type);
            }
        }
    }

    /**
     * These keys only manipulate the state of the keyboard and never
     * send key events to the client. For example, "Hide", "Caps Lock",
     * etc are all KeyboardStateKeys.
     */
    private class KeyboardStateKey extends Key {
        private KeyboardStateKey(String t) {
            text.setText(t);
            getStyleClass().add("special");
            setId(t);
        }
    }

    /**
     * A special type of KeyboardStateKey used for switching from the current
     * virtual keyboard layout to a new one.
     */
    private final class SwitchBoardKey extends KeyboardStateKey {
        private FXVK.Type type;

        private SwitchBoardKey(String displayName, FXVK.Type type) {
            super(displayName);
            this.type = type;
        }

        @Override protected void release() {
            super.release();
            getSkinnable().setType(type);
        }
    }

    private List<List<Key>> loadBoard(String boardName) {
        try {
            List<List<Key>> rows = new ArrayList<List<Key>>(5);
            List<Key> keys = new ArrayList<Key>(20);

            InputStream asciiBoardFile = FXVKSkin.class.getResourceAsStream(boardName + ".txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(asciiBoardFile));
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
            String chars = "";
            String alt = null;

            while ((line = reader.readLine()) != null) {
                // A single line represents a single row of buttons
                for (int i=0; i<line.length(); i++) {
                    char ch = line.charAt(i);

                    // Process the char
                    if (ch == ' ') {
                        c++;
                    } else if (ch == '[') {
                        // Start of a key
                        col = c;
                        chars = "";
                        alt = null;
                        identifier = false;
                    } else if (ch == ']') {
                        // End of a key
                        colSpan = c - col;
                        Key key;
                        if (identifier) {
                            if ("$shift".equals(chars)) {
                                key = new KeyboardStateKey("shift") {
                                    @Override protected void release() {
                                        pressShift();
                                    }
                                };
                                key.getStyleClass().add("shift");
                            } else if ("$backspace".equals(chars)) {
                                key = new KeyCodeKey("backspace", "\b", KeyCode.BACK_SPACE);
                                key.getStyleClass().add("backspace");

                            } else if ("$enter".equals(chars)) {
                                key = new KeyCodeKey("enter", "\n", KeyCode.ENTER);
                                key.getStyleClass().add("enter");
                            } else if ("$tab".equals(chars)) {
                                key = new KeyCodeKey("tab", "\t", KeyCode.TAB);
                            } else if ("$caps".equals(chars)) {
                                key = new KeyboardStateKey("caps lock") {
                                    @Override protected void release() {
                                        pressCaps();
                                    }
                                };
                                key.getStyleClass().add("caps");
                            } else if ("$space".equals(chars)) {
                                key = new LetterKey(" ");
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
                                key = new KeyboardStateKey("Hide") {
                                    @Override protected void release() {
                                        slideInTimeline.stop();
                                        if (!inScene) {
                                            winY.set(vkPopup.getY());
                                        }
                                        slideOutTimeline.playFromStart();
                                    }
                                };
                                key.getStyleClass().add("hide");
                            } else if ("$undo".equals(chars)) {
                                key = new SuperKey("undo", "");
                            } else if ("$redo".equals(chars)) {
                                key = new SuperKey("redo", "");
                            } else {
                                // The name is the name of a board to show
                                String name = chars.substring(1);
                                if (name.equals("AsciiBoard")) {
                                    key = new SwitchBoardKey("ABC", FXVK.Type.TEXT);
                                } else if (name.equals("EmailBoard")) {
                                    key = new SwitchBoardKey("ABC.com", FXVK.Type.EMAIL);
                                } else if (name.equals("SymbolBoard")) {
                                    key = new SwitchBoardKey("#+=", FXVK.Type.NUMERIC);
                                } else {
                                    throw new AssertionError("Unknown keyboard '" + name + "'");
                                }
                            }
                        } else {
                            boolean isLetter = false;
                            try {
                                KeyCode code = KeyCode.getKeyCode(chars.toUpperCase());
                                isLetter = code == null ? false : code.isLetterKey();
                            } catch (Exception e) { }
                            key = isLetter ? new LetterKey(chars) : new SymbolKey(chars, alt);
                        }
                        key.col = col;
                        key.colSpan = colSpan;
                        if (rows.isEmpty()) {
                            key.getStyleClass().add("short");
                        }
                        for (String sc : key.getStyleClass()) {
                            key.text.getStyleClass().add(sc + "-text");
                            key.icon.getStyleClass().add(sc + "-icon");
                        }
                        keys.add(key);
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
                                chars = line.substring(i, j);
                                i = j + 1;
                            } else if ((c2 == ']' || c2 == ' ') && !e) {
                                if (chars.isEmpty()) {
                                    chars = line.substring(i, j);
                                } else {
                                    alt = line.substring(i, j);
                                }
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
            return rows;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
