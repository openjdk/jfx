/*
 * Copyright (c) 2008, 2012 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.javafx.experiments.scheduleapp.control;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.SkinBase;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

/**
 * The VirtualKeyboardSkin simply has a pile of keys depending on the keyboard type.
 * Where you place the keyboard relative to the screen, how it is displayed, etc is
 * completely up to you.
 */
public class VirtualKeyboardSkin extends SkinBase<VirtualKeyboard, BehaviorBase<VirtualKeyboard>> {
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
     * Creates a new VirtualKeyboardSkin
     * @param control
     * @param behavior
     */
    public VirtualKeyboardSkin(VirtualKeyboard keyboard) {
        super(keyboard, new BehaviorBase<VirtualKeyboard>(keyboard, Collections.EMPTY_LIST));

        registerChangeListener(keyboard.typeProperty(), "type");
        rebuild();
    }

    @Override protected void handleControlPropertyChanged(String propertyReference) {
        // With Java 8 (or is it 7?) I can do switch on strings instead
        if (propertyReference == "type") {
            // The type has changed, so we will need to rebuild the entire keyboard.
            // This happens whenever the user switches from one keyboard layout to
            // another, such as by pressing the "ABC" key on a numeric layout.
            rebuild();
        }
    }

    /**
     * Replaces all children of this VirtualKeyboardSkin based on the keyboard
     * type set on the VirtualKeyboard.
     */
    private void rebuild() {
        String boardName;
        VirtualKeyboard.Type type = getSkinnable().getType();
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
    @Override protected double computePrefWidth(double height) {
        final Insets insets = getInsets();
        return insets.getLeft() + (56 * numCols) + insets.getRight();
    }

    // Pref height is just some value. This isn't overly important.
    @Override protected double computePrefHeight(double width) {
        final Insets insets = getInsets();
        return insets.getTop() + (80 * 5) + insets.getBottom();
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
            final Insets insets = getInsets();
            final double left = insets.getLeft();
            final double top = insets.getTop();
            final double width = getWidth() - left - insets.getRight();
            final double height = getHeight() - top - insets.getBottom();

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
            EventHandler<KeyEvent> handler = getSkinnable().getOnAction();
            if (handler != null) {
                handler.handle(event(KeyEvent.KEY_PRESSED));
            }
        }
        protected void release() {
            EventHandler<KeyEvent> handler = getSkinnable().getOnAction();
            if (handler != null) {
                handler.handle(event(KeyEvent.KEY_TYPED));
                handler.handle(event(KeyEvent.KEY_RELEASED));
            }
            super.release();
        }

        protected KeyEvent event(EventType<KeyEvent> type) {
            return KeyEvent.impl_keyEvent(getSkinnable(), chars, "", 0,
                                          shiftDown, false, false, false, type);
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
        }

        protected KeyEvent event(EventType<KeyEvent> type) {
            if (type == KeyEvent.KEY_PRESSED || type == KeyEvent.KEY_RELEASED) {
                return KeyEvent.impl_keyEvent(getSkinnable(), chars, chars, code.impl_getCode(),
                                              shiftDown, false, false, false, type);
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
        }
    }

    /**
     * A special type of KeyboardStateKey used for switching from the current
     * virtual keyboard layout to a new one.
     */
    private final class SwitchBoardKey extends KeyboardStateKey {
        private VirtualKeyboard.Type type;

        private SwitchBoardKey(String displayName, VirtualKeyboard.Type type) {
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

            InputStream asciiBoardFile = VirtualKeyboardSkin.class.getResourceAsStream(boardName + ".txt");
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
                                        super.release();
                                        requestFocus();
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
                                    key = new SwitchBoardKey("ABC", VirtualKeyboard.Type.TEXT);
                                } else if (name.equals("EmailBoard")) {
                                    key = new SwitchBoardKey("ABC.com", VirtualKeyboard.Type.EMAIL);
                                } else if (name.equals("SymbolBoard")) {
                                    key = new SwitchBoardKey("#+=", VirtualKeyboard.Type.NUMERIC);
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
