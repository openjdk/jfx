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
import javafx.scene.control.behavior.BehaviorBase;
import javafx.scene.control.behavior.EventCriteria;
import javafx.scene.control.behavior.InputMap;
import javafx.scene.control.behavior.KeyBinding;
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
    public void install() {
        TextInputControl c = getControl();

        setOnKeyEventEnter(() -> setCaretAnimating(false));
        setOnKeyEventExit(() -> setCaretAnimating(true));

        c.textProperty().addListener(textListener);

        registerFunction(TextInputControl.COPY, c::copy);
        registerFunction(TextInputControl.CUT, this::cut);
        registerFunction(TextInputControl.DELETE_FROM_LINE_START, this::deleteFromLineStart);
        registerFunction(TextInputControl.DELETE_NEXT_CHAR, this::deleteNextChar);
        registerFunction(TextInputControl.DELETE_NEXT_WORD, this::deleteNextWord);
        registerFunction(TextInputControl.DELETE_PREVIOUS_CHAR, this::deletePreviousChar);
        registerFunction(TextInputControl.DELETE_PREVIOUS_WORD, this::deletePreviousWord);
        registerFunction(TextInputControl.DESELECT, c::deselect);
        registerFunction(TextInputControl.DOCUMENT_START, c::home);
        registerFunction(TextInputControl.DOCUMENT_END, c::end);
        registerFunction(TextInputControl.LEFT, () -> nextCharacterVisually(false));
        registerFunction(TextInputControl.LEFT_WORD, this::leftWord);
        registerFunction(TextInputControl.PASTE, this::paste);
        registerFunction(TextInputControl.REDO, this::redo);
        registerFunction(TextInputControl.RIGHT, () -> nextCharacterVisually(true));
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
        registerFunction(TextInputControl.TRAVERSE_NEXT, () -> FocusTraversalInputMap.traverseNext(c));
        registerFunction(TextInputControl.TRAVERSE_PREVIOUS, () -> FocusTraversalInputMap.traversePrevious(c));
        registerFunction(TextInputControl.UNDO, this::undo);

        // common key bindings
        registerKey(KeyBinding.shortcut(KeyCode.C), TextInputControl.COPY);
        registerKey(KeyBinding.of(KeyCode.COPY), TextInputControl.COPY);
        registerKey(KeyBinding.shortcut(KeyCode.INSERT), TextInputControl.COPY);
        registerKey(KeyBinding.of(KeyCode.CUT), TextInputControl.CUT);
        registerKey(KeyBinding.shortcut(KeyCode.X), TextInputControl.CUT);
        registerKey(KeyBinding.of(KeyCode.DELETE), TextInputControl.DELETE_NEXT_CHAR);
        registerKey(KeyBinding.of(KeyCode.BACK_SPACE), TextInputControl.DELETE_PREVIOUS_CHAR);
        registerKey(KeyBinding.with(KeyCode.BACK_SPACE).shift().build(), TextInputControl.DELETE_PREVIOUS_CHAR);
        registerKey(KeyBinding.of(KeyCode.HOME), TextInputControl.DOCUMENT_START);
        registerKey(KeyBinding.with(KeyCode.HOME).shortcut().build(), TextInputControl.DOCUMENT_START);
        registerKey(KeyBinding.of(KeyCode.UP), TextInputControl.DOCUMENT_START);
        registerKey(KeyBinding.of(KeyCode.DOWN), TextInputControl.DOCUMENT_END);
        registerKey(KeyBinding.of(KeyCode.END), TextInputControl.DOCUMENT_END);
        registerKey(KeyBinding.with(KeyCode.END).shortcut().build(), TextInputControl.DOCUMENT_END);
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
        registerKey(KeyBinding.with(KeyCode.TAB).control().shift().build(), TextInputControl.TRAVERSE_PREVIOUS);
        registerKey(KeyBinding.shortcut(KeyCode.A), TextInputControl.SELECT_ALL);
        registerKey(KeyBinding.shortcut(KeyCode.Z), TextInputControl.UNDO);

        if (isMac()) {
            // macOS key bindings
            registerKey(KeyBinding.with(KeyCode.BACK_SPACE).shortcut().build(), TextInputControl.DELETE_FROM_LINE_START);
            registerKey(KeyBinding.with(KeyCode.DELETE).alt().build(), TextInputControl.DELETE_NEXT_WORD);
            registerKey(KeyBinding.with(KeyCode.BACK_SPACE).alt().build(), TextInputControl.DELETE_PREVIOUS_WORD);
            registerKey(KeyBinding.with(KeyCode.HOME).shift().build(), TextInputControl.SELECT_HOME_EXTEND);
            registerKey(KeyBinding.with(KeyCode.LEFT).shortcut().build(), TextInputControl.DOCUMENT_START);
            registerKey(KeyBinding.with(KeyCode.RIGHT).shortcut().build(), TextInputControl.DOCUMENT_END);
            registerKey(KeyBinding.with(KeyCode.LEFT).alt().build(), TextInputControl.LEFT_WORD);
            registerKey(KeyBinding.with(KeyCode.Z).shortcut().shift().build(), TextInputControl.REDO);
            registerKey(KeyBinding.with(KeyCode.RIGHT).alt().build(), TextInputControl.RIGHT_WORD);
            registerKey(KeyBinding.with(KeyCode.LEFT).shortcut().shift().build(), TextInputControl.SELECT_HOME_EXTEND);
            registerKey(KeyBinding.with(KeyCode.RIGHT).shortcut().shift().build(), TextInputControl.SELECT_END_EXTEND);
            registerKey(KeyBinding.with(KeyCode.END).shift().build(), TextInputControl.SELECT_END_EXTEND);
            registerKey(KeyBinding.with(KeyCode.LEFT).shift().alt().build(), TextInputControl.SELECT_LEFT_WORD);
            registerKey(KeyBinding.with(KeyCode.RIGHT).shift().alt().build(), TextInputControl.SELECT_RIGHT_WORD);
        } else {
            // not-mac key bindings
            registerKey(KeyBinding.with(KeyCode.DELETE).control().build(), TextInputControl.DELETE_NEXT_WORD);
            registerKey(KeyBinding.with(KeyCode.H).control().build(), TextInputControl.DELETE_PREVIOUS_CHAR);
            registerKey(KeyBinding.with(KeyCode.BACK_SPACE).control().build(), TextInputControl.DELETE_PREVIOUS_WORD);
            registerKey(KeyBinding.with(KeyCode.BACK_SLASH).control().build(), TextInputControl.DESELECT);
            registerKey(KeyBinding.with(KeyCode.LEFT).control().build(), TextInputControl.LEFT_WORD);
            registerKey(KeyBinding.with(KeyCode.RIGHT).control().build(), TextInputControl.RIGHT_WORD);
            registerKey(KeyBinding.with(KeyCode.HOME).shift().build(), TextInputControl.SELECT_HOME);
            registerKey(KeyBinding.with(KeyCode.END).shift().build(), TextInputControl.SELECT_END);
            registerKey(KeyBinding.with(KeyCode.LEFT).control().shift().build(), TextInputControl.SELECT_LEFT_WORD);
            registerKey(KeyBinding.with(KeyCode.RIGHT).control().shift().build(), TextInputControl.SELECT_RIGHT_WORD);
        }

        // windows key bindings
        if (isWindows()) {
            registerKey(KeyBinding.with(KeyCode.Y).control().build(), TextInputControl.REDO);
        }

        // linux key bindings
        if (isLinux()) {
            registerKey(KeyBinding.with(KeyCode.Z).control().shift().build(), TextInputControl.REDO);
        }

        // key pad mappings
        addKeyPadMappings();

        addHandler(KeyEvent.KEY_TYPED, this::defaultKeyTyped);

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
            addHandler(KeyBinding.builder().with(KeyCode.DIGIT9).control().shift().build(), true, (ev) -> {
                FXVK.toggleUseVK(getControl());
            });
        }

        // mouse and context menu mappings
        addHandler(MouseEvent.MOUSE_PRESSED, this::mousePressed);
        addHandler(MouseEvent.MOUSE_DRAGGED, this::mouseDragged);
        addHandler(MouseEvent.MOUSE_RELEASED, this::mouseReleased);

        addHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, this::contextMenuRequested);
    }

    /**
     * Binds keypad arrow keys to the same function tags as the regular arrow keys.
     */
    protected void addKeyPadMappings() {
        InputMap m = getInputMap();
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
                        control(k.isControl()).
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

    @Override
    public void dispose() {
        getControl().textProperty().removeListener(textListener);
        super.dispose();
    }

    /**************************************************************************
     * Abstract methods                                                       *
     *************************************************************************/

    protected abstract void deleteChar(boolean previous);
    protected abstract void replaceText(int start, int end, String txt);
    protected abstract void setCaretAnimating(boolean play);
    protected abstract void deleteFromLineStart();

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

    private void nextCharacterVisually(boolean moveRight) {
        if (isMixed()) {
            TextInputControlSkin<?> skin = (TextInputControlSkin<?>)getControl().getSkin();
            skin.moveCaret(TextUnit.CHARACTER, moveRight ? Direction.RIGHT : Direction.LEFT, false);
        } else if (moveRight != isRTLText()) {
            getControl().forward();
        } else {
            getControl().backward();
        }
    }

    private void selectLeft() {
        if (isRTLText()) {
            getControl().selectForward();
        } else {
            getControl().selectBackward();
        }
    }

    private void selectRight() {
        if (isRTLText()) {
            getControl().selectBackward();
        } else {
            getControl().selectForward();
        }
    }

    boolean isEditable() {
        return getControl().isEditable();
    }

    private void deletePreviousChar() {
        if (isEditable()) {
            setEditing(true);
            deleteChar(true);
            setEditing(false);
        }
    }

    private void deleteNextChar() {
        if (isEditable()) {
            setEditing(true);
            deleteChar(false);
            setEditing(false);
        }
    }

    protected void deletePreviousWord() {
        setEditing(true);
        TextInputControl textInputControl = getControl();
        int end = textInputControl.getCaretPosition();

        if (end > 0) {
            textInputControl.previousWord();
            int start = textInputControl.getCaretPosition();
            replaceText(start, end, "");
        }
        setEditing(false);
    }

    protected void deleteNextWord() {
        setEditing(true);
        TextInputControl textInputControl = getControl();
        int start = textInputControl.getCaretPosition();

        if (start < textInputControl.getLength()) {
            nextWord();
            int end = textInputControl.getCaretPosition();
            replaceText(start, end, "");
        }
        setEditing(false);
    }

    public void deleteSelection() {
        setEditing(true);
        TextInputControl textInputControl = getControl();
        IndexRange selection = textInputControl.getSelection();

        if (selection.getLength() > 0) {
            deleteChar(false);
        }
        setEditing(false);
    }

    public void cut() {
        if (isEditable()) {
            setEditing(true);
            getControl().cut();
            setEditing(false);
        }
    }

    public void paste() {
        if (isEditable()) {
            setEditing(true);
            getControl().paste();
            setEditing(false);
        }
    }

    public void undo() {
        setEditing(true);
        getControl().undo();
        setEditing(false);
    }

    public void redo() {
        setEditing(true);
        getControl().redo();
        setEditing(false);
    }

    protected void selectPreviousWord() {
        getControl().selectPreviousWord();
    }

    public void selectNextWord() {
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
        if (SHOW_HANDLES && contextMenu.isShowing()) {
            populateContextMenu();
        }
    }

    protected void selectAll() {
        getControl().selectAll();
        if (SHOW_HANDLES && contextMenu.isShowing()) {
            populateContextMenu();
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

    protected void selectHome() {
        getControl().selectHome();
    }

    protected void selectEnd() {
        getControl().selectEnd();
    }

    protected void selectHomeExtend() {
        getControl().extendSelection(0);
    }

    protected void selectEndExtend() {
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

    private final MenuItem undoMI   = new ContextMenuItem("Undo", e -> undo());
    private final MenuItem redoMI   = new ContextMenuItem("Redo", e -> redo());
    private final MenuItem cutMI    = new ContextMenuItem("Cut", e -> cut());
    private final MenuItem copyMI   = new ContextMenuItem("Copy", e -> getControl().copy());
    private final MenuItem pasteMI  = new ContextMenuItem("Paste", e -> paste());
    private final MenuItem deleteMI = new ContextMenuItem("DeleteSelection", e -> deleteSelection());
    private final MenuItem selectWordMI = new ContextMenuItem("SelectWord", e -> selectWord());
    private final MenuItem selectAllMI = new ContextMenuItem("SelectAll", e -> selectAll());
    private final MenuItem separatorMI = new SeparatorMenuItem();
}
