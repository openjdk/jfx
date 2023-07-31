/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.javafx.PlatformUtil.isLinux;
import static com.sun.javafx.PlatformUtil.isMac;
import static com.sun.javafx.PlatformUtil.isWindows;
import static com.sun.javafx.scene.control.skin.resources.ControlResources.getString;
import java.text.Bidi;
import java.util.Set;
import java.util.function.Predicate;
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
import javafx.scene.control.Skin;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.input.BehaviorBase2;
import javafx.scene.control.input.EventCriteria;
import javafx.scene.control.input.InputMap2;
import javafx.scene.control.input.KeyBinding2;
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
import com.sun.javafx.scene.control.inputmap.InputMap.KeyMapping;
import com.sun.javafx.scene.control.inputmap.KeyBinding;
import com.sun.javafx.scene.control.skin.FXVK;

/**
 * All of the "button" types (CheckBox, RadioButton, ToggleButton, and Button)
 * and also maybe some other types like hyperlinks operate on the "armed"
 * selection strategy, just like JButton. This behavior class encapsulates that
 * logic in a way that can be reused and extended by each of the individual
 * class behaviors.
 *
 */
public abstract class TextInputControlBehavior<T extends TextInputControl> extends BehaviorBase2<T> {

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

    public TextInputControlBehavior() {
        // TODO create upon demand
        contextMenu = new ContextMenu();
    }

    @Override
    public void install(Skin<T> skin) {
        super.install(skin);

        TextInputControl c = getNode();

        setOnKeyEventEnter(() -> setCaretAnimating(false));
        setOnKeyEventExit(() -> setCaretAnimating(true));

        c.textProperty().addListener(textListener);

        func(TextInputControl.COPY, c::copy); // TODO move method to behavior
        func(TextInputControl.CUT, this::cut);
        func(TextInputControl.DELETE_FROM_LINE_START, this::deleteFromLineStart);
        func(TextInputControl.DELETE_NEXT_CHAR, this::deleteNextChar);
        func(TextInputControl.DELETE_NEXT_WORD, this::deleteNextWord);
        func(TextInputControl.DELETE_PREVIOUS_CHAR, this::deletePreviousChar);
        func(TextInputControl.DELETE_PREVIOUS_WORD, this::deletePreviousWord);
        func(TextInputControl.DESELECT, c::deselect); // TODO move method to behavior
        func(TextInputControl.DOCUMENT_START, c::home); // TODO move method to behavior
        func(TextInputControl.DOCUMENT_END, c::end); // TODO move method to behavior
        func(TextInputControl.LEFT, () -> nextCharacterVisually(false));
        func(TextInputControl.LEFT_WORD, this::leftWord);
        func(TextInputControl.PASTE, this::paste);
        func(TextInputControl.REDO, this::redo);
        func(TextInputControl.RIGHT, () -> nextCharacterVisually(true));
        func(TextInputControl.RIGHT_WORD, this::rightWord);
        func(TextInputControl.SELECT_ALL, this::selectAll);
        func(TextInputControl.SELECT_END, this::selectEnd);
        func(TextInputControl.SELECT_END_EXTEND, this::selectEndExtend);
        func(TextInputControl.SELECT_HOME, this::selectHome);
        func(TextInputControl.SELECT_HOME_EXTEND, this::selectHomeExtend);
        func(TextInputControl.SELECT_LEFT, this::selectLeft);
        func(TextInputControl.SELECT_LEFT_WORD, this::selectLeftWord);
        func(TextInputControl.SELECT_RIGHT, this::selectRight);
        func(TextInputControl.SELECT_RIGHT_WORD, this::selectRightWord);
        func(TextInputControl.TRAVERSE_NEXT, () -> FocusTraversalInputMap.traverseNext(c));
        func(TextInputControl.TRAVERSE_PREVIOUS, () -> FocusTraversalInputMap.traversePrevious(c));
        func(TextInputControl.UNDO, this::undo);

        // common key bindings
        key(KeyBinding2.shortcut(KeyCode.C), TextInputControl.COPY);
        key(KeyBinding2.of(KeyCode.COPY), TextInputControl.COPY);
        key(KeyBinding2.shortcut(KeyCode.INSERT), TextInputControl.COPY);
        key(KeyBinding2.of(KeyCode.CUT), TextInputControl.CUT);
        key(KeyBinding2.shortcut(KeyCode.X), TextInputControl.CUT);
        key(KeyBinding2.of(KeyCode.DELETE), TextInputControl.DELETE_NEXT_CHAR);
        key(KeyBinding2.of(KeyCode.BACK_SPACE), TextInputControl.DELETE_PREVIOUS_CHAR);
        key(KeyBinding2.with(KeyCode.BACK_SPACE).shift().build(), TextInputControl.DELETE_PREVIOUS_CHAR);
        key(KeyBinding2.of(KeyCode.HOME), TextInputControl.DOCUMENT_START);
        key(KeyBinding2.with(KeyCode.HOME).shortcut().build(), TextInputControl.DOCUMENT_START);
        key(KeyBinding2.of(KeyCode.UP), TextInputControl.DOCUMENT_START);
        key(KeyBinding2.of(KeyCode.DOWN), TextInputControl.DOCUMENT_END);
        key(KeyBinding2.of(KeyCode.END), TextInputControl.DOCUMENT_END);
        key(KeyBinding2.with(KeyCode.END).shortcut().build(), TextInputControl.DOCUMENT_END);
        key(KeyBinding2.of(KeyCode.LEFT), TextInputControl.LEFT);
        key(KeyBinding2.of(KeyCode.PASTE), TextInputControl.PASTE);
        key(KeyBinding2.shift(KeyCode.INSERT), TextInputControl.PASTE);
        key(KeyBinding2.shortcut(KeyCode.V), TextInputControl.PASTE);
        key(KeyBinding2.of(KeyCode.RIGHT), TextInputControl.RIGHT);
        key(KeyBinding2.shift(KeyCode.DOWN), TextInputControl.SELECT_END);
        key(KeyBinding2.with(KeyCode.END).shortcut().shift().build(), TextInputControl.SELECT_END);
        key(KeyBinding2.with(KeyCode.HOME).shortcut().shift().build(), TextInputControl.SELECT_HOME);
        key(KeyBinding2.shift(KeyCode.UP), TextInputControl.SELECT_HOME);
        key(KeyBinding2.shift(KeyCode.LEFT), TextInputControl.SELECT_LEFT);
        key(KeyBinding2.shift(KeyCode.RIGHT), TextInputControl.SELECT_RIGHT);
        key(KeyBinding2.of(KeyCode.TAB), TextInputControl.TRAVERSE_NEXT);
        key(KeyBinding2.ctrl(KeyCode.TAB), TextInputControl.TRAVERSE_NEXT);
        key(KeyBinding2.shift(KeyCode.TAB), TextInputControl.TRAVERSE_PREVIOUS);
        key(KeyBinding2.with(KeyCode.TAB).control().shift().build(), TextInputControl.TRAVERSE_PREVIOUS);
        key(KeyBinding2.shortcut(KeyCode.Z), TextInputControl.UNDO);

        // macOS key bindings
        key(KeyBinding2.with(KeyCode.BACK_SPACE).shortcut().forMac().build(), TextInputControl.DELETE_FROM_LINE_START);
        key(KeyBinding2.with(KeyCode.DELETE).alt().forMac().build(), TextInputControl.DELETE_NEXT_WORD);
        key(KeyBinding2.with(KeyCode.BACK_SPACE).alt().forMac().build(), TextInputControl.DELETE_PREVIOUS_WORD);
        key(KeyBinding2.with(KeyCode.HOME).shift().forMac().build(), TextInputControl.DOCUMENT_START);
        key(KeyBinding2.with(KeyCode.LEFT).shortcut().forMac().build(), TextInputControl.DOCUMENT_START);
        key(KeyBinding2.with(KeyCode.RIGHT).shortcut().forMac().build(), TextInputControl.DOCUMENT_END);
        key(KeyBinding2.with(KeyCode.LEFT).alt().forMac().build(), TextInputControl.LEFT_WORD);
        key(KeyBinding2.with(KeyCode.Z).shortcut().shift().forMac().build(), TextInputControl.REDO);
        key(KeyBinding2.with(KeyCode.RIGHT).alt().forMac().build(), TextInputControl.RIGHT_WORD);
        key(KeyBinding2.shortcut(KeyCode.A), TextInputControl.SELECT_ALL);
        key(KeyBinding2.with(KeyCode.LEFT).shortcut().shift().forMac().build(), TextInputControl.SELECT_HOME_EXTEND);
        key(KeyBinding2.with(KeyCode.RIGHT).shortcut().shift().forMac().build(), TextInputControl.SELECT_END_EXTEND);
        key(KeyBinding2.with(KeyCode.END).shift().forMac().build(), TextInputControl.SELECT_END_EXTEND);
        key(KeyBinding2.with(KeyCode.LEFT).shift().alt().forMac().build(), TextInputControl.SELECT_LEFT_WORD);
        key(KeyBinding2.with(KeyCode.RIGHT).shift().alt().forMac().build(), TextInputControl.SELECT_RIGHT_WORD);

        // windows key bindings
        key(KeyBinding2.with(KeyCode.Y).control().forWindows().build(), TextInputControl.REDO);

        // linux key bindings
        key(KeyBinding2.with(KeyCode.Z).control().shift().forLinux().build(), TextInputControl.REDO);

        // not-mac key bindings
        key(KeyBinding2.with(KeyCode.DELETE).control().notForMac().build(), TextInputControl.DELETE_NEXT_WORD);
        key(KeyBinding2.with(KeyCode.H).control().notForMac().build(), TextInputControl.DELETE_PREVIOUS_CHAR);
        key(KeyBinding2.with(KeyCode.BACK_SPACE).control().notForMac().build(), TextInputControl.DELETE_PREVIOUS_WORD);
        key(KeyBinding2.with(KeyCode.BACK_SLASH).control().notForMac().build(), TextInputControl.DESELECT);
        key(KeyBinding2.with(KeyCode.LEFT).control().notForMac().build(), TextInputControl.LEFT_WORD);
        key(KeyBinding2.with(KeyCode.RIGHT).control().notForMac().build(), TextInputControl.RIGHT_WORD);
        key(KeyBinding2.with(KeyCode.HOME).shift().notForMac().build(), TextInputControl.SELECT_HOME);
        key(KeyBinding2.with(KeyCode.END).shift().notForMac().build(), TextInputControl.SELECT_END);
        key(KeyBinding2.with(KeyCode.LEFT).control().shift().notForMac().build(), TextInputControl.SELECT_LEFT_WORD);
        key(KeyBinding2.with(KeyCode.RIGHT).control().shift().notForMac().build(), TextInputControl.SELECT_RIGHT_WORD);

        // key pad mappings
        addKeyPadMappings();

        map(KeyEvent.KEY_TYPED, this::defaultKeyTyped);

        // However, we want to consume other key press / release events too, for
        // things that would have been handled by the InputCharacter normally
        mapTail(
            new EventCriteria<KeyEvent>() {
                @Override
                public EventType<KeyEvent> getEventType() {
                    return KeyEvent.KEY_PRESSED;
                }

                @Override
                public boolean isEventAcceptable(KeyEvent ev) {
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
            map(KeyBinding2.builder().with(KeyCode.DIGIT9).control().shift().build(), true, (ev) -> {
                FXVK.toggleUseVK(getNode());
            });
        }

        // mouse and context menu mappings
        map(MouseEvent.MOUSE_PRESSED, this::mousePressed);
        map(MouseEvent.MOUSE_DRAGGED, this::mouseDragged);
        map(MouseEvent.MOUSE_RELEASED, this::mouseReleased);

        map(ContextMenuEvent.CONTEXT_MENU_REQUESTED, this::contextMenuRequested);
    }

    /**
     * Binds keypad arrow keys to the same function tags as the regular arrow keys.
     */
    protected void addKeyPadMappings() {
        InputMap2 m = getNode().getInputMap2();
        Set<KeyBinding2> keys = m.getKeyBindings();
        for (KeyBinding2 k: keys) {
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
                    KeyBinding2 newBinding = KeyBinding2.
                        with(newCode).
                        alt(k.isAlt()).
                        command(k.isCommand()).
                        control(k.isControl()).
                        meta(k.isMeta()).
                        option(k.isOption()).
                        shift(k.isShift()).                        
                        build();
                    m.addAlias(k, newBinding);
                }
            }
        }
    }

    // However, we want to consume other key press / release events too, for
    // things that would have been handled by the InputCharacter normally
    // (TODO note: KEY_RELEASEs are not handled by this code, same as was implemented with the old input map)
//    private void handleRemainingKeyPresses(KeyEvent ev) {
//        if (!ev.isAltDown() && !ev.isControlDown() && !ev.isMetaDown() && !ev.isShortcutDown()) {
//            if (!ev.getCode().isFunctionKey()) {
//                ev.consume();
//            }
//        }
//    }

    /**
     * Wraps the event handler to pause caret blinking when
     * processing the key event.
     */
    protected KeyMapping keyMapping(final KeyCode keyCode, final EventHandler<KeyEvent> eventHandler) {
        return keyMapping(new KeyBinding(keyCode), eventHandler);
    }

    protected KeyMapping keyMapping(KeyBinding keyBinding, final EventHandler<KeyEvent> eventHandler) {
        return keyMapping(keyBinding, eventHandler, null);
    }

    protected KeyMapping keyMapping(KeyBinding keyBinding, final EventHandler<KeyEvent> eventHandler,
                                    Predicate<KeyEvent> interceptor) {
        return new KeyMapping(keyBinding,
                              e -> {
                                  setCaretAnimating(false);
                                  eventHandler.handle(e);
                                  setCaretAnimating(true);
                              },
                              interceptor);
    }





    /**************************************************************************
     * Disposal methods                                                       *
     *************************************************************************/

    @Override public void dispose() {
        getNode().textProperty().removeListener(textListener);
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
        final TextInputControl textInput = getNode();
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
            bidi = new Bidi(getNode().textProperty().getValueSafe(),
                    (getNode().getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT)
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
                                getNode().getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT));
        }
        return rtlText;
    }

    private void nextCharacterVisually(boolean moveRight) {
        if (isMixed()) {
            TextInputControlSkin<?> skin = (TextInputControlSkin<?>)getNode().getSkin();
            skin.moveCaret(TextUnit.CHARACTER, moveRight ? Direction.RIGHT : Direction.LEFT, false);
        } else if (moveRight != isRTLText()) {
            getNode().forward();
        } else {
            getNode().backward();
        }
    }

    private void selectLeft() {
        if (isRTLText()) {
            getNode().selectForward();
        } else {
            getNode().selectBackward();
        }
    }

    private void selectRight() {
        if (isRTLText()) {
            getNode().selectBackward();
        } else {
            getNode().selectForward();
        }
    }
    
    boolean isEditable() {
        return getNode().isEditable();
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
        TextInputControl textInputControl = getNode();
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
        TextInputControl textInputControl = getNode();
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
        TextInputControl textInputControl = getNode();
        IndexRange selection = textInputControl.getSelection();

        if (selection.getLength() > 0) {
            deleteChar(false);
        }
        setEditing(false);
    }

    public void cut() {
        if (isEditable()) {
            setEditing(true);
            getNode().cut(); // FIX move here
            setEditing(false);
        }
    }

    public void paste() {
        if (isEditable()) {
            setEditing(true);
            getNode().paste(); // FIX move here
            setEditing(false);
        }
    }

    public void undo() {
        setEditing(true);
        getNode().undo(); // FIX move here
        setEditing(false);
    }

    public void redo() {
        setEditing(true);
        getNode().redo(); // FIX move here
        setEditing(false);
    }

    protected void selectPreviousWord() {
        getNode().selectPreviousWord(); // FIX move here
    }

    public void selectNextWord() {
        TextInputControl textInputControl = getNode();
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
        final TextInputControl textInputControl = getNode();
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
        getNode().selectAll();
        if (SHOW_HANDLES && contextMenu.isShowing()) {
            populateContextMenu();
        }
    }

    protected void previousWord() {
        getNode().previousWord();
    }

    protected void nextWord() {
        TextInputControl textInputControl = getNode();
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
        getNode().selectHome();
    }

    protected void selectEnd() {
        getNode().selectEnd();
    }

    protected void selectHomeExtend() {
        getNode().extendSelection(0);
    }

    protected void selectEndExtend() {
        TextInputControl textInputControl = getNode();
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
        TextInputControl textInputControl = getNode();
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
            undoMI.setDisable(!getNode().isUndoable());
            redoMI.setDisable(!getNode().isRedoable());
            cutMI.setDisable(maskText || !hasSelection);
            copyMI.setDisable(maskText || !hasSelection);
            pasteMI.setDisable(!Clipboard.getSystemClipboard().hasString());
            deleteMI.setDisable(!hasSelection);
        }
    }

    private static class ContextMenuItem extends MenuItem {
        ContextMenuItem(final String action, EventHandler<ActionEvent> onAction) {
            super(getString("TextInputControl.menu." + action));
            setOnAction(onAction);
        }
    }

    private final MenuItem undoMI   = new ContextMenuItem("Undo", e -> undo());
    private final MenuItem redoMI   = new ContextMenuItem("Redo", e -> redo());
    private final MenuItem cutMI    = new ContextMenuItem("Cut", e -> cut());
    private final MenuItem copyMI   = new ContextMenuItem("Copy", e -> getNode().copy());
    private final MenuItem pasteMI  = new ContextMenuItem("Paste", e -> paste());
    private final MenuItem deleteMI = new ContextMenuItem("DeleteSelection", e -> deleteSelection());
    private final MenuItem selectWordMI = new ContextMenuItem("SelectWord", e -> selectWord());
    private final MenuItem selectAllMI = new ContextMenuItem("SelectAll", e -> selectAll());
    private final MenuItem separatorMI = new SeparatorMenuItem();

}
