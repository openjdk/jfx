/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.behavior;

import javafx.application.ConditionalFeature;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;

import java.text.Bidi;
import java.util.ArrayList;
import java.util.List;

import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.scene.control.skin.TextInputControlSkin;

import static javafx.scene.input.KeyEvent.KEY_PRESSED;

import static com.sun.javafx.PlatformUtil.*;

/**
 * Abstract base class for text input behaviors.
 */
public abstract class TextInputControlBehavior<T extends TextInputControl> extends BehaviorBase<T> {
    /**************************************************************************
     *                          Setup KeyBindings                             *
     *************************************************************************/
    protected static final List<KeyBinding> TEXT_INPUT_BINDINGS = new ArrayList<KeyBinding>();
    static {
        TEXT_INPUT_BINDINGS.addAll(TextInputControlBindings.BINDINGS);
        // However, we want to consume other key press / release events too, for
        // things that would have been handled by the InputCharacter normally
        TEXT_INPUT_BINDINGS.add(new KeyBinding(null, KEY_PRESSED, "Consume"));
    }

    /**************************************************************************
     * Fields                                                                 *
     *************************************************************************/

    T textInputControl;

    /**
     * Used to keep track of the most recent key event. This is used when
     * handling InputCharacter actions.
     */
    private KeyEvent lastEvent;

    private UndoManager undoManager = new UndoManager();

    /**************************************************************************
     * Constructors                                                           *
     *************************************************************************/

    /**
     * Create a new TextInputControlBehavior.
     * @param textInputControl cannot be null
     */
    public TextInputControlBehavior(T textInputControl) {
        super(textInputControl);

        this.textInputControl = textInputControl;

        textInputControl.textProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable observable) {
                if (!isEditing()) {
                    // Text changed, but not by user action
                    undoManager.reset();
                }
                invalidateBidi();
            }
        });
    }

    /**************************************************************************
     * Abstract methods                                                       *
     *************************************************************************/

    protected abstract void deleteChar(boolean previous);
    protected abstract void replaceText(int start, int end, String txt);
    protected abstract void setCaretAnimating(boolean play);

    protected void scrollCharacterToVisible(int index) {
        // TODO this method should be removed when TextAreaSkin
        // TODO is refactored to no longer need it.
    }

    /**************************************************************************
     * Key handling implementation                                            *
     *************************************************************************/

    @Override protected List<KeyBinding> createKeyBindings() {
        return TEXT_INPUT_BINDINGS;
    }

    /**
     * Records the last KeyEvent we saw.
     * @param e
     */
    @Override protected void callActionForEvent(KeyEvent e) {
        lastEvent = e;
        super.callActionForEvent(e);
    }

    @Override public void callAction(String name) {
        TextInputControl textInputControl = getControl();
        boolean done = false;

        setCaretAnimating(false);

        if (textInputControl.isEditable()) {
            setEditing(true);
            done = true;
            if ("InputCharacter".equals(name)) defaultKeyTyped(lastEvent);
            else if ("Cut".equals(name)) cut();
            else if ("Paste".equals(name)) paste();
            else if ("DeletePreviousChar".equals(name)) deletePreviousChar();
            else if ("DeleteNextChar".equals(name)) deleteNextChar();
            else if ("DeletePreviousWord".equals(name)) deletePreviousWord();
            else if ("DeleteNextWord".equals(name)) deleteNextWord();
            else if ("DeleteSelection".equals(name)) deleteSelection();
            else if ("Undo".equals(name)) undoManager.undo();
            else if ("Redo".equals(name)) undoManager.redo();
            else {
                done = false;
            }
            setEditing(false);
        }
        if (!done) {
            done = true;
            if ("Copy".equals(name)) textInputControl.copy();
            else if ("SelectBackward".equals(name)) textInputControl.selectBackward();
            else if ("SelectForward".equals(name)) textInputControl.selectForward();
            else if ("SelectLeft".equals(name)) selectLeft();
            else if ("SelectRight".equals(name)) selectRight();
            else if ("PreviousWord".equals(name)) previousWord();
            else if ("NextWord".equals(name)) nextWord();
            else if ("LeftWord".equals(name)) leftWord();
            else if ("RightWord".equals(name)) rightWord();
            else if ("SelectPreviousWord".equals(name)) selectPreviousWord();
            else if ("SelectNextWord".equals(name)) selectNextWord();
            else if ("SelectLeftWord".equals(name)) selectLeftWord();
            else if ("SelectRightWord".equals(name)) selectRightWord();
            else if ("SelectWord".equals(name)) selectWord();
            else if ("SelectAll".equals(name)) textInputControl.selectAll();
            else if ("Home".equals(name)) textInputControl.home();
            else if ("End".equals(name)) textInputControl.end();
            else if ("Forward".equals(name)) textInputControl.forward();
            else if ("Backward".equals(name)) textInputControl.backward();
            else if ("Right".equals(name)) nextCharacterVisually(true);
            else if ("Left".equals(name)) nextCharacterVisually(false);
            else if ("Fire".equals(name)) fire(lastEvent);
            else if ("Unselect".equals(name)) textInputControl.deselect();
            else if ("SelectHome".equals(name)) selectHome();
            else if ("SelectEnd".equals(name)) selectEnd();
            else if ("SelectHomeExtend".equals(name)) selectHomeExtend();
            else if ("SelectEndExtend".equals(name)) selectEndExtend();
            else if ("ToParent".equals(name)) forwardToParent(lastEvent);
            /*DEBUG*/else if ("UseVK".equals(name) && PlatformImpl.isSupported(ConditionalFeature.VIRTUAL_KEYBOARD)) {
                ((TextInputControlSkin<?,?>)textInputControl.getSkin()).toggleUseVK();
            } else {
                done = false;
            }
        }
        setCaretAnimating(true);

        if (!done) {
            if ("TraverseNext".equals(name)) traverseNext();
            else if ("TraversePrevious".equals(name)) traversePrevious();
            else super.callAction(name);

        }
        // Note, I don't have to worry about "Consume" here.
    }

    protected UndoManager getUndoManager() {
        return undoManager;
    }

    /**
     * The default handler for a key typed event, which is called when none of
     * the other key bindings match. This is the method which handles basic
     * text entry.
     * @param event not null
     */
    private void defaultKeyTyped(KeyEvent event) {
        final TextInputControl textInput = getControl();
        // I'm not sure this case can actually ever happen, maybe this
        // should be an assert instead?
        if (!textInput.isEditable() || textInput.isDisabled()) return;

        // Sometimes we get events with no key character, in which case
        // we need to bail.
        String character = event.getCharacter();
        if (character.length() == 0) return;

        // Filter out control keys except control+Alt on PC or Alt on Mac
        if (event.isControlDown() || event.isAltDown() || (isMac() && event.isMetaDown())) {
            if (!((event.isControlDown() || isMac()) && event.isAltDown())) return;
        }

        // Ignore characters in the control range and the ASCII delete
        // character as well as meta key presses
        if (character.charAt(0) > 0x1F
            && character.charAt(0) != 0x7F
            && !event.isMetaDown()) { // Not sure about this one
            final IndexRange selection = textInput.getSelection();
            final int start = selection.getStart();
            final int end = selection.getEnd();

//            if (textInput.getLength() - selection.getLength()
//                + character.length() > textInput.getMaximumLength()) {
//                // TODO Beep?
//            } else {
                undoManager.addChange(start, textInput.textProperty().getValueSafe().substring(start, end), character, true);
                replaceText(start, end, character);
//            }

            scrollCharacterToVisible(start);
        }
    }

    private Bidi bidi = null;
    private Boolean mixed = null;
    private Boolean rtlText = null;

    private void invalidateBidi() {
        bidi = null;
        mixed = null;
        rtlText = null;
    }

    private Bidi getBidi() {
        if (bidi == null) {
            bidi = new Bidi(textInputControl.getText(),
                            (textInputControl.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT)
                                    ? Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT
                                    : Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        }
        return bidi;
    }

    protected boolean isMixed() {
        if (mixed == null) {
            mixed = getBidi().isMixed();
        }
        return mixed;
    }

    protected boolean isRTLText() {
        if (rtlText == null) {
            Bidi bidi = getBidi();
            rtlText =
                (bidi.isRightToLeft() ||
                 (isMixed() &&
                  textInputControl.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT));
        }
        return rtlText;
    }

    private void nextCharacterVisually(boolean moveRight) {
        if (isMixed()) {
            TextInputControlSkin<?,?> skin = (TextInputControlSkin<?,?>)textInputControl.getSkin();
            skin.nextCharacterVisually(moveRight);
        } else if (moveRight != isRTLText()) {
            textInputControl.forward();
        } else {
            textInputControl.backward();
        }
    }

    private void selectLeft() {
        if (isRTLText()) {
            textInputControl.selectForward();
        } else {
            textInputControl.selectBackward();
        }
    }

    private void selectRight() {
        if (isRTLText()) {
            textInputControl.selectBackward();
        } else {
            textInputControl.selectForward();
        }
    }

    private void deletePreviousChar() {
        TextInputControl textInputControl = getControl();
        IndexRange selection = textInputControl.getSelection();
        int start = selection.getStart();
        int end = selection.getEnd();

        if (start > 0 || end > start) {
            if (selection.getLength() == 0) {
                end = start;
                // Note: This can handle the case of a surrogate pair
                // which requires two chars to be deleted. However it
                // does not, and should not, delete any other kind of
                // cluster as a whole, just the last char. Compare
                // with deleteNextChar().
                start = Character.offsetByCodePoints(textInputControl.getText(), end, -1);
            }
            undoManager.addChange(start, textInputControl.getText().substring(start, end), null);
        }
        deleteChar(true);
    }

    private void deleteNextChar() {
        TextInputControl textInputControl = getControl();
        IndexRange selection = textInputControl.getSelection();
        int start = selection.getStart();
        int end = selection.getEnd();

        if (start < textInputControl.getLength() || end > start) {
            if (selection.getLength() == 0) {
                // Note: This can handle the case of a surrogate
                // pair which requires two chars to be deleted.
                // TODO: It should also delete any kind of cluster as
                // a whole, not just the first char. Compare with
                // deletePreviousChar().
                end = Character.offsetByCodePoints(textInputControl.getText(), start, 1);
            }
            undoManager.addChange(start, textInputControl.getText().substring(start, end), null);
        }
        deleteChar(false);
    }

    protected void deletePreviousWord() {
        TextInputControl textInputControl = getControl();
        int end = textInputControl.getCaretPosition();

        if (end > 0) {
            textInputControl.previousWord();
            int start = textInputControl.getCaretPosition();
            undoManager.addChange(start, textInputControl.getText().substring(start, end), null);
            replaceText(start, end, "");
        }
    }

    protected void deleteNextWord() {
        TextInputControl textInputControl = getControl();
        int start = textInputControl.getCaretPosition();

        if (start < textInputControl.getLength()) {
            nextWord();
            int end = textInputControl.getCaretPosition();
            undoManager.addChange(start, textInputControl.getText().substring(start, end), null);
            replaceText(start, end, "");
        }
    }

    private void deleteSelection() {
        TextInputControl textInputControl = getControl();
        IndexRange selection = textInputControl.getSelection();

        if (selection.getLength() > 0) {
            int start = selection.getStart();
            int end = selection.getEnd();
            undoManager.addChange(start, textInputControl.getText().substring(start, end), null);
            deleteChar(false);
        }
    }

    private void cut() {
        TextInputControl textInputControl = getControl();
        IndexRange selection = textInputControl.getSelection();

        if (selection.getLength() > 0) {
            int start = selection.getStart();
            int end = selection.getEnd();
            undoManager.addChange(start, textInputControl.getText().substring(start, end), null);
        }
        textInputControl.cut();
    }

    private void paste() {
        TextInputControl textInputControl = getControl();
        IndexRange selection = textInputControl.getSelection();
        int start = selection.getStart();
        int end = selection.getEnd();
        String text = textInputControl.textProperty().getValueSafe();
        String deleted = text.substring(start, end);
        int tail = text.length() - end;

        textInputControl.paste();

        text = textInputControl.textProperty().getValueSafe();
        undoManager.addChange(start, deleted, text.substring(start, text.length() - tail));
    }

    protected void selectPreviousWord() {
        getControl().selectPreviousWord();
    }

    protected void selectNextWord() {
        TextInputControl textInputControl = getControl();
        if (isMac() || isLinux()) {
            textInputControl.selectEndOfNextWord();
        } else {
            textInputControl.selectNextWord();
        }
    }

    private void selectLeftWord() {
        if (isRTLText()) {
            selectNextWord();
        } else {
            selectPreviousWord();
        }
    }

    private void selectRightWord() {
        if (isRTLText()) {
            selectPreviousWord();
        } else {
            selectNextWord();
        }
    }

    protected void selectWord() {
        final TextInputControl textInputControl = getControl();
        textInputControl.previousWord();
        if (isWindows()) {
            textInputControl.selectNextWord();
        } else {
            textInputControl.selectEndOfNextWord();
        }
    }

    protected void previousWord() {
        getControl().previousWord();
    }

    protected void nextWord() {
        TextInputControl textInputControl = getControl();
        if (isMac() || isLinux()) {
            textInputControl.endOfNextWord();
        } else {
            textInputControl.nextWord();
        }
    }

    private void leftWord() {
        if (isRTLText()) {
            nextWord();
        } else {
            previousWord();
        }
    }

    private void rightWord() {
        if (isRTLText()) {
            previousWord();
        } else {
            nextWord();
        }
    }

    protected void fire(KeyEvent event) { } // TODO move to TextFieldBehavior

    protected void forwardToParent(KeyEvent event) {
        if (getControl().getParent() != null) {
            getControl().getParent().fireEvent(event);
        }
    }

    private void selectHome() {
        getControl().selectHome();
    }

    private void selectEnd() {
        getControl().selectEnd();
    }

    private void selectHomeExtend() {
        getControl().extendSelection(0);
    }

    private void selectEndExtend() {
        TextInputControl textInputControl = getControl();
        textInputControl.extendSelection(textInputControl.getLength());
    }

    private boolean editing = false;
    protected void setEditing(boolean b) {
        editing = b;
    }
    public boolean isEditing() {
        return editing;
    }

    public boolean canUndo() {
        return undoManager.canUndo();
    }

    public boolean canRedo() {
        return undoManager.canRedo();
    }

    static class Change {
        int start;
        String oldText;
        String newText;
        boolean appendable;

        Change(int start, String oldText, String newText) {
            this(start, oldText, newText, false);
        }

        Change(int start, String oldText, String newText, boolean appendable) {
            this.start = start;
            this.oldText = oldText;
            this.newText = newText;
            this.appendable = appendable;
        }
    }

    class UndoManager {
        private ArrayList<Change> chain = new ArrayList<Change>();
        private int currentIndex = 0;

        public void addChange(int start, String oldText, String newText) {
            addChange(start, oldText, newText, false);
        }

        public void addChange(int start, String oldText, String newText, boolean appendable) {
            truncate();
            if (appendable && currentIndex > 0 && (oldText == null || oldText.length() == 0)) {
                Change change = chain.get(currentIndex - 1);
                if (change.appendable && start == change.start + change.newText.length()) {
                    // Append text to previous Change
                    change.newText += newText;
                    return;
                }
            }
            chain.add(new Change(start, oldText, newText, appendable));
            currentIndex++;
        }

        public void undo() {
            if (currentIndex > 0) {
                // Apply reverse change here
                Change change = chain.get(currentIndex - 1);
                replaceText(change.start,
                            change.start + ((change.newText != null) ? change.newText.length() : 0),
                            (change.oldText != null) ? change.oldText : "");
                currentIndex--;
                if (currentIndex > 0) {
                    chain.get(currentIndex - 1).appendable = false;
                }
            }
            // else beep ?
        }

        public void redo() {
            if (currentIndex < chain.size()) {
                // Apply change here
                Change change = chain.get(currentIndex);
                replaceText(change.start,
                            change.start + ((change.oldText != null) ? change.oldText.length() : 0),
                            (change.newText != null) ? change.newText : "");
                change.appendable = false;
                currentIndex++;
            }
            // else beep ?
        }

        public boolean canUndo() {
            return (currentIndex > 0);
        }

        public boolean canRedo() {
            return (currentIndex < chain.size());
        }

        public void reset() {
            chain.clear();
            currentIndex = 0;
        }

        private void truncate() {
            if (currentIndex > 0 && chain.size() > currentIndex) {
                chain.get(currentIndex - 1).appendable = false;
            }

            while (chain.size() > currentIndex) {
                chain.remove(chain.size() - 1);
            }
        }
    }
}
