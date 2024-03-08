/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.text.Bidi;
import java.util.Set;
import javafx.application.ConditionalFeature;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.input.BehaviorBase;
import javafx.scene.control.input.EventCriteria;
import javafx.scene.control.input.KeyBinding;
import javafx.scene.control.input.SkinInputMap;
import javafx.scene.control.skin.TextInputControlSkin;
import javafx.scene.control.skin.TextInputControlSkin.Direction;
import javafx.scene.control.skin.TextInputControlSkin.TextUnit;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.scene.control.Properties;
import com.sun.javafx.scene.control.skin.FXVK;
import com.sun.javafx.scene.control.skin.resources.ControlResources;

/**
 * All of the "button" types (CheckBox, RadioButton, ToggleButton, and Button)
 * and also maybe some other types like hyperlinks operate on the "armed"
 * selection strategy, just like JButton. This behavior class encapsulates that
 * logic in a way that can be reused and extended by each of the individual
 * class behaviors.
 *
 */
public abstract class TextInputControlBehavior<T extends TextInputControl> extends BehaviorBase<T> {

    /**
     * Specifies whether we ought to show handles. We should do it on touch platforms
     */
    static final boolean SHOW_HANDLES = Properties.IS_TOUCH_SUPPORTED;

    public static final String DISABLE_FORWARD_TO_PARENT = "TextInputControlBehavior.disableForwardToParent";

    /**************************************************************************
     * Fields                                                                 *
     *************************************************************************/

    protected ContextMenu contextMenu;

    private InvalidationListener textListener = observable -> invalidateBidi();


    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public TextInputControlBehavior(T c) {
        super(c);
        // TODO create upon demand
        contextMenu = new ContextMenu();
    }

    @Override
    protected void populateSkinInputMap() {
        getControl().textProperty().addListener(textListener);

        registerFunction(TextInputControl.COPY, this::copy);
        registerFunction(TextInputControl.CUT, this::cut);
        registerFunction(TextInputControl.DELETE_FROM_LINE_START, this::deleteFromLineStart);
        registerFunction(TextInputControl.DELETE_NEXT_CHAR, this::deleteNextChar);
        registerFunction(TextInputControl.DELETE_NEXT_WORD, this::deleteNextWord);
        registerFunction(TextInputControl.DELETE_PREVIOUS_CHAR, this::deletePreviousChar);
        registerFunction(TextInputControl.DELETE_PREVIOUS_WORD, this::deletePreviousWord);
        registerFunction(TextInputControl.DESELECT, this::deselect);
        registerFunction(TextInputControl.DOCUMENT_START, this::home);
        registerFunction(TextInputControl.DOCUMENT_END, this::end);
        registerFunction(TextInputControl.LEFT, (c) -> nextCharacterVisually(c, false));
        registerFunction(TextInputControl.LEFT_WORD, this::leftWord);
        registerFunction(TextInputControl.PASTE, this::paste);
        registerFunction(TextInputControl.REDO, this::redo);
        registerFunction(TextInputControl.RIGHT, (c) -> nextCharacterVisually(c, true));
        registerFunction(TextInputControl.RIGHT_WORD, this::rightWord);
        registerFunction(TextInputControl.SELECT_ALL, this::selectAll);
        registerFunction(TextInputControl.SELECT_END, this::selectEnd);
        registerFunction(TextInputControl.SELECT_END_EXTEND, this::selectEndExtend);
        registerFunction(TextInputControl.SELECT_HOME, this::selectHome);
        registerFunction(TextInputControl.SELECT_HOME_EXTEND, this::selectHomeExtend);
        registerFunction(TextInputControl.SELECT_LEFT, this::selectLeft);
        registerFunction(TextInputControl.SELECT_LEFT_WORD, this::selectLeftWord);
        registerFunction(TextInputControl.SELECT_RIGHT, this::selectRight);
        registerFunction(TextInputControl.SELECT_RIGHT_WORD, this::selectRightWord);
        registerFunction(TextInputControl.TRAVERSE_NEXT, (c) -> FocusTraversalInputMap.traverseNext(c));
        registerFunction(TextInputControl.TRAVERSE_PREVIOUS, (c) -> FocusTraversalInputMap.traversePrevious(c));
        registerFunction(TextInputControl.UNDO, this::undo);

        // common key bindings
        registerKey(KeyBinding.shortcut(KeyCode.C), TextInputControl.COPY);
        registerKey(KeyBinding.of(KeyCode.COPY), TextInputControl.COPY);
        registerKey(KeyBinding.shortcut(KeyCode.INSERT), TextInputControl.COPY);
        registerKey(KeyBinding.of(KeyCode.CUT), TextInputControl.CUT);
        registerKey(KeyBinding.shortcut(KeyCode.X), TextInputControl.CUT);
        registerKey(KeyBinding.of(KeyCode.DELETE), TextInputControl.DELETE_NEXT_CHAR);
        registerKey(KeyBinding.of(KeyCode.BACK_SPACE), TextInputControl.DELETE_PREVIOUS_CHAR);
        registerKey(KeyBinding.shift(KeyCode.BACK_SPACE), TextInputControl.DELETE_PREVIOUS_CHAR);
        registerKey(KeyBinding.of(KeyCode.HOME), TextInputControl.DOCUMENT_START);
        registerKey(KeyBinding.shortcut(KeyCode.HOME), TextInputControl.DOCUMENT_START);
        registerKey(KeyBinding.of(KeyCode.UP), TextInputControl.DOCUMENT_START);
        registerKey(KeyBinding.of(KeyCode.DOWN), TextInputControl.DOCUMENT_END);
        registerKey(KeyBinding.of(KeyCode.END), TextInputControl.DOCUMENT_END);
        registerKey(KeyBinding.shortcut(KeyCode.END), TextInputControl.DOCUMENT_END);
        registerKey(KeyBinding.of(KeyCode.LEFT), TextInputControl.LEFT);
        registerKey(KeyBinding.of(KeyCode.PASTE), TextInputControl.PASTE);
        registerKey(KeyBinding.shift(KeyCode.INSERT), TextInputControl.PASTE);
        registerKey(KeyBinding.shortcut(KeyCode.V), TextInputControl.PASTE);
        registerKey(KeyBinding.of(KeyCode.RIGHT), TextInputControl.RIGHT);
        registerKey(KeyBinding.shift(KeyCode.DOWN), TextInputControl.SELECT_END);
        registerKey(KeyBinding.with(KeyCode.END).shortcut().shift().build(), TextInputControl.SELECT_END);
        registerKey(KeyBinding.with(KeyCode.HOME).shortcut().shift().build(), TextInputControl.SELECT_HOME);
        registerKey(KeyBinding.shift(KeyCode.UP), TextInputControl.SELECT_HOME);
        registerKey(KeyBinding.shift(KeyCode.LEFT), TextInputControl.SELECT_LEFT);
        registerKey(KeyBinding.shift(KeyCode.RIGHT), TextInputControl.SELECT_RIGHT);
        registerKey(KeyBinding.of(KeyCode.TAB), TextInputControl.TRAVERSE_NEXT);
        registerKey(KeyBinding.ctrl(KeyCode.TAB), TextInputControl.TRAVERSE_NEXT);
        registerKey(KeyBinding.shift(KeyCode.TAB), TextInputControl.TRAVERSE_PREVIOUS);
        registerKey(KeyBinding.ctrlShift(KeyCode.TAB), TextInputControl.TRAVERSE_PREVIOUS);
        registerKey(KeyBinding.shortcut(KeyCode.A), TextInputControl.SELECT_ALL);
        registerKey(KeyBinding.shortcut(KeyCode.Z), TextInputControl.UNDO);

        if (isMac()) {
            // macOS key bindings
            registerKey(KeyBinding.shortcut(KeyCode.BACK_SPACE), TextInputControl.DELETE_FROM_LINE_START);
            registerKey(KeyBinding.alt(KeyCode.DELETE), TextInputControl.DELETE_NEXT_WORD);
            registerKey(KeyBinding.alt(KeyCode.BACK_SPACE), TextInputControl.DELETE_PREVIOUS_WORD);
            registerKey(KeyBinding.shift(KeyCode.HOME), TextInputControl.SELECT_HOME_EXTEND);
            registerKey(KeyBinding.shortcut(KeyCode.LEFT), TextInputControl.DOCUMENT_START);
            registerKey(KeyBinding.shortcut(KeyCode.RIGHT), TextInputControl.DOCUMENT_END);
            registerKey(KeyBinding.alt(KeyCode.LEFT), TextInputControl.LEFT_WORD);
            registerKey(KeyBinding.with(KeyCode.Z).shortcut().shift().build(), TextInputControl.REDO);
            registerKey(KeyBinding.alt(KeyCode.RIGHT), TextInputControl.RIGHT_WORD);
            registerKey(KeyBinding.with(KeyCode.LEFT).shortcut().shift().build(), TextInputControl.SELECT_HOME_EXTEND);
            registerKey(KeyBinding.with(KeyCode.RIGHT).shortcut().shift().build(), TextInputControl.SELECT_END_EXTEND);
            registerKey(KeyBinding.shift(KeyCode.END), TextInputControl.SELECT_END_EXTEND);
            registerKey(KeyBinding.with(KeyCode.LEFT).shift().alt().build(), TextInputControl.SELECT_LEFT_WORD);
            registerKey(KeyBinding.with(KeyCode.RIGHT).shift().alt().build(), TextInputControl.SELECT_RIGHT_WORD);
        } else {
            // not-mac key bindings
            registerKey(KeyBinding.ctrl(KeyCode.DELETE), TextInputControl.DELETE_NEXT_WORD);
            registerKey(KeyBinding.ctrl(KeyCode.H), TextInputControl.DELETE_PREVIOUS_CHAR);
            registerKey(KeyBinding.ctrl(KeyCode.BACK_SPACE), TextInputControl.DELETE_PREVIOUS_WORD);
            registerKey(KeyBinding.ctrl(KeyCode.BACK_SLASH), TextInputControl.DESELECT);
            registerKey(KeyBinding.ctrl(KeyCode.LEFT), TextInputControl.LEFT_WORD);
            registerKey(KeyBinding.ctrl(KeyCode.RIGHT), TextInputControl.RIGHT_WORD);
            registerKey(KeyBinding.shift(KeyCode.HOME), TextInputControl.SELECT_HOME);
            registerKey(KeyBinding.shift(KeyCode.END), TextInputControl.SELECT_END);
            registerKey(KeyBinding.ctrlShift(KeyCode.LEFT), TextInputControl.SELECT_LEFT_WORD);
            registerKey(KeyBinding.ctrlShift(KeyCode.RIGHT), TextInputControl.SELECT_RIGHT_WORD);
        }

        // windows key bindings
        if (isWindows()) {
            registerKey(KeyBinding.ctrl(KeyCode.Y), TextInputControl.REDO);
        }

        // linux key bindings
        if (isLinux()) {
            registerKey(KeyBinding.ctrlShift(KeyCode.Z), TextInputControl.REDO);
        }

        // key pad mappings
        addKeyPadMappings();

        addHandlerLast(KeyEvent.KEY_TYPED, true, this::defaultKeyTyped);

        // However, we want to consume other key press / release events too, for
        // things that would have been handled by the InputCharacter normally
        addHandlerLast(
            new EventCriteria<KeyEvent>() {
                @Override
                public EventType<KeyEvent> getEventType() {
                    return KeyEvent.KEY_PRESSED;
                }

                @Override
                public boolean isEventAcceptable(KeyEvent ev) {
                    switch(ev.getCode()) {
                    case ESCAPE:
                    case ENTER:
                        return false;
                    };
                    return
                        !ev.getCode().isFunctionKey() &&
                        !ev.isAltDown() &&
                        !ev.isControlDown() &&
                        !ev.isMetaDown() &&
                        !ev.isShortcutDown();
                }
            },
            false,
            (ev) -> ev.consume()
        );

        // VK
        // TODO can PlatformImpl.isSupported(ConditionalFeature) change at runtime?
        if (PlatformImpl.isSupported(ConditionalFeature.VIRTUAL_KEYBOARD)) {
            addHandler(KeyBinding.ctrlShift(KeyCode.DIGIT9), true, (ev) -> {
                FXVK.toggleUseVK(getControl());
            });
        }

        // mouse and context menu mappings
        addHandler(MouseEvent.MOUSE_PRESSED, true, this::mousePressed);
        addHandler(MouseEvent.MOUSE_DRAGGED, true, this::mouseDragged);
        addHandler(MouseEvent.MOUSE_RELEASED, true, this::mouseReleased);

        addHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, true, this::contextMenuRequested);
    }

    /**
     * Binds keypad arrow keys to the same function tags as the regular arrow keys.
     */
    protected void addKeyPadMappings() {
        SkinInputMap m = getSkinInputMap();
        Set<KeyBinding> keys = m.getKeyBindings();
        for (KeyBinding k: keys) {
            KeyCode cd = k.getKeyCode();
            if (cd != null) {
                KeyCode newCode = null;
                switch (cd) {
                case LEFT:
                    newCode = KeyCode.KP_LEFT;
                    break;
                case RIGHT:
                    newCode = KeyCode.KP_RIGHT;
                    break;
                case UP:
                    newCode = KeyCode.KP_UP;
                    break;
                case DOWN:
                    newCode = KeyCode.KP_DOWN;
                    break;
                default:
                    newCode = null;
                    break;
                }

                if (newCode != null) {
                    KeyBinding newBinding = KeyBinding.
                        with(newCode).
                        alt(k.isAlt()).
                        command(k.isCommand()).
                        ctrl(k.isCtrl()).
                        meta(k.isMeta()).
                        option(k.isOption()).
                        shift(k.isShift()).
                        build();
                    duplicateMapping(k, newBinding);
                }
            }
        }
    }

    /**************************************************************************
     * Disposal methods                                                       *
     *************************************************************************/

    public void dispose() {
        getControl().textProperty().removeListener(textListener);
    }

    /**************************************************************************
     * Abstract methods                                                       *
     *************************************************************************/

    protected abstract void deleteChar(T c, boolean previous);
    protected abstract void replaceText(int start, int end, String txt);
    protected abstract void setCaretAnimating(boolean play);
    protected abstract void deleteFromLineStart(T c);

    protected abstract void mousePressed(MouseEvent e);
    protected abstract void mouseDragged(MouseEvent e);
    protected abstract void mouseReleased(MouseEvent e);
    protected abstract void contextMenuRequested(ContextMenuEvent e);

    /**************************************************************************
     * Key handling implementation                                            *
     *************************************************************************/

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

        setEditing(true);

        // Ignore characters in the control range and the ASCII delete
        // character as well as meta key presses
        if (character.charAt(0) > 0x1F
                && character.charAt(0) != 0x7F
                && !event.isMetaDown()) { // Not sure about this one
            final IndexRange selection = textInput.getSelection();
            final int start = selection.getStart();
            final int end = selection.getEnd();

            replaceText(start, end, character);
        }

        setEditing(false);
    }

    private Bidi bidi = null;
    private Boolean mixed = null;
    private Boolean rtlText = null;

    // test-only
    Bidi getRawBidi() {
        return bidi;
    }

    private void invalidateBidi() {
        bidi = null;
        mixed = null;
        rtlText = null;
    }

    private Bidi getBidi() {
        if (bidi == null) {
            bidi = new Bidi(getControl().textProperty().getValueSafe(),
                    (getControl().getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT)
                            ? Bidi.DIRECTION_RIGHT_TO_LEFT
                            : Bidi.DIRECTION_LEFT_TO_RIGHT);
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
                                getControl().getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT));
        }
        return rtlText;
    }

    private void nextCharacterVisually(TextInputControl c, boolean moveRight) {
        if (isMixed()) {
            TextInputControlSkin<?> skin = (TextInputControlSkin<?>)c.getSkin();
            skin.moveCaret(TextUnit.CHARACTER, moveRight ? Direction.RIGHT : Direction.LEFT, false);
        } else if (moveRight != isRTLText()) {
            getControl().forward();
        } else {
            getControl().backward();
        }
    }

    private void selectLeft(T c) {
        if (isRTLText()) {
            c.selectForward(); // TODO move impl here
        } else {
            c.selectBackward(); // TODO move impl here
        }
    }

    private void selectRight(T c) {
        if (isRTLText()) {
            c.selectBackward(); // TODO move impl here
        } else {
            c.selectForward(); // TODO move impl here
        }
    }

    boolean isEditable() {
        return getControl().isEditable();
    }

    private void deletePreviousChar(T c) {
        if (isEditable()) {
            setEditing(true);
            deleteChar(c, true);
            setEditing(false);
        }
    }

    private void deleteNextChar(T c) {
        if (isEditable()) {
            setEditing(true);
            deleteChar(c, false);
            setEditing(false);
        }
    }

    protected void deletePreviousWord(T c) {
        setEditing(true);
        int end = c.getCaretPosition();
        if (end > 0) {
            c.previousWord();
            int start = c.getCaretPosition();
            replaceText(start, end, "");
        }
        setEditing(false);
    }

    protected void deleteNextWord(T c) {
        setEditing(true);
        int start = c.getCaretPosition();
        if (start < c.getLength()) {
            nextWord(c);
            int end = c.getCaretPosition();
            replaceText(start, end, "");
        }
        setEditing(false);
    }

    public void deleteSelection(T c) {
        setEditing(true);
        IndexRange selection = c.getSelection();
        if (selection.getLength() > 0) {
            deleteChar(c, false);
        }
        setEditing(false);
    }

    public void copy(T c) {
        c.copy(); // TODO move implementation here
    }

    public void deselect(T c) {
        c.deselect(); // TODO move implementation here
    }

    public void home(T c) {
        c.home(); // TODO move implementation here
    }

    public void end(T c) {
        c.end(); // TODO move implementation here
    }

    public void cut(T c) {
        if (isEditable()) {
            setEditing(true);
            c.cut();
            setEditing(false);
        }
    }

    public void paste(T c) {
        if (isEditable()) {
            setEditing(true);
            c.paste();
            setEditing(false);
        }
    }

    public void undo(T c) {
        setEditing(true);
        c.undo();
        setEditing(false);
    }

    public void redo(T c) {
        setEditing(true);
        c.redo();
        setEditing(false);
    }

    protected void selectPreviousWord(T c) {
        c.selectPreviousWord(); // TODO move impl here
    }

    public void selectNextWord(T c) {
        if (isMac() || isLinux()) {
            c.selectEndOfNextWord(); // TODO move impl here
        } else {
            c.selectNextWord(); // TODO move impl here
        }
    }

    private void selectLeftWord(T c) {
        if (isRTLText()) {
            selectNextWord(c);
        } else {
            selectPreviousWord(c);
        }
    }

    private void selectRightWord(T c) {
        if (isRTLText()) {
            selectPreviousWord(c);
        } else {
            selectNextWord(c);
        }
    }

    protected void selectWord(T c) {
        c.previousWord(); // TODO move implementation here
        if (isWindows()) {
            c.selectNextWord(); // TODO move implementation here
        } else {
            c.selectEndOfNextWord(); // TODO move implementation here
        }
        if (SHOW_HANDLES && contextMenu.isShowing()) {
            populateContextMenu();
        }
    }

    protected void selectAll(T c) {
        c.selectAll(); // TODO move implementation here
        if (SHOW_HANDLES && contextMenu.isShowing()) {
            populateContextMenu();
        }
    }

    protected void previousWord(T c) {
        c.previousWord(); // TODO move implementation here
    }

    protected void nextWord(T c) {
        if (isMac() || isLinux()) {
            c.endOfNextWord(); // TODO move implementation here
        } else {
            c.nextWord(); // TODO move implementation here
        }
    }

    private void leftWord(T c) {
        if (isRTLText()) {
            nextWord(c);
        } else {
            previousWord(c);
        }
    }

    private void rightWord(T c) {
        if (isRTLText()) {
            previousWord(c);
        } else {
            nextWord(c);
        }
    }

    protected void selectHome(T c) {
        c.selectHome(); // TODO move implementation here
    }

    protected void selectEnd(T c) {
        c.selectEnd(); // TODO move implementation here
    }

    protected void selectHomeExtend(T c) {
        c.extendSelection(0);
    }

    protected void selectEndExtend(T c) {
        int len = c.getLength();
        c.extendSelection(len);
    }

    private boolean editing = false;
    protected void setEditing(boolean b) {
        editing = b;
    }
    public boolean isEditing() {
        return editing;
    }

    protected void populateContextMenu() {
        TextInputControl textInputControl = getControl();
        boolean editable = textInputControl.isEditable();
        boolean hasText = (textInputControl.getLength() > 0);
        boolean hasSelection = (textInputControl.getSelection().getLength() > 0);
        boolean allSelected = (textInputControl.getSelection().getLength() == textInputControl.getLength());
        boolean maskText = (textInputControl instanceof PasswordField); // (maskText("A") != "A");
        ObservableList<MenuItem> items = contextMenu.getItems();

        if (SHOW_HANDLES) {
            items.clear();
            if (!maskText && hasSelection) {
                if (editable) {
                    items.add(cutMI);
                }
                items.add(copyMI);
            }
            if (editable && Clipboard.getSystemClipboard().hasString()) {
                items.add(pasteMI);
            }
            if (hasText && !allSelected) {
                if (!hasSelection && !(textInputControl instanceof PasswordField)) {
                    items.add(selectWordMI);
                }
                items.add(selectAllMI);
            }
            selectWordMI.getProperties().put("refreshMenu", Boolean.TRUE);
            selectAllMI.getProperties().put("refreshMenu", Boolean.TRUE);
        } else {
            if (editable) {
                items.setAll(undoMI, redoMI, cutMI, copyMI, pasteMI, deleteMI,
                        separatorMI, selectAllMI);
            } else {
                items.setAll(copyMI, separatorMI, selectAllMI);
            }
            undoMI.setDisable(!getControl().isUndoable());
            redoMI.setDisable(!getControl().isRedoable());
            cutMI.setDisable(maskText || !hasSelection);
            copyMI.setDisable(maskText || !hasSelection);
            pasteMI.setDisable(!Clipboard.getSystemClipboard().hasString());
            deleteMI.setDisable(!hasSelection);
        }
    }

    private static class ContextMenuItem extends MenuItem {
        ContextMenuItem(final String action, EventHandler<ActionEvent> onAction) {
            super(ControlResources.getString("TextInputControl.menu." + action));
            setOnAction(onAction);
        }
    }

    private final MenuItem undoMI   = new ContextMenuItem("Undo", e -> undo(getControl()));
    private final MenuItem redoMI   = new ContextMenuItem("Redo", e -> redo(getControl()));
    private final MenuItem cutMI    = new ContextMenuItem("Cut", e -> cut(getControl()));
    private final MenuItem copyMI   = new ContextMenuItem("Copy", e -> copy(getControl()));
    private final MenuItem pasteMI  = new ContextMenuItem("Paste", e -> paste(getControl()));
    private final MenuItem deleteMI = new ContextMenuItem("DeleteSelection", e -> deleteSelection(getControl()));
    private final MenuItem selectWordMI = new ContextMenuItem("SelectWord", e -> selectWord(getControl()));
    private final MenuItem selectAllMI = new ContextMenuItem("SelectAll", e -> selectAll(getControl()));
    private final MenuItem separatorMI = new SeparatorMenuItem();
}
