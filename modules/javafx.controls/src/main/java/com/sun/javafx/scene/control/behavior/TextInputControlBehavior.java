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

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.scene.control.Properties;
import com.sun.javafx.scene.control.skin.FXVK;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.skin.TextInputControlSkin;
import javafx.application.ConditionalFeature;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableList;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TextInputControl.Cmd;
import javafx.scene.control.input.KeyBinding2;
import javafx.scene.control.input.KeyMap;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.Clipboard;
import com.sun.javafx.scene.control.inputmap.InputMap;
import com.sun.javafx.scene.control.inputmap.InputMap.Mapping;
import com.sun.javafx.scene.control.inputmap.KeyBinding;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import java.text.Bidi;
import java.util.Set;
import java.util.function.Predicate;

import static com.sun.javafx.PlatformUtil.isLinux;
import static com.sun.javafx.PlatformUtil.isMac;
import static com.sun.javafx.PlatformUtil.isWindows;
import static com.sun.javafx.scene.control.inputmap.KeyBinding.OptionalBoolean;
import static com.sun.javafx.scene.control.skin.resources.ControlResources.getString;
import static javafx.scene.control.skin.TextInputControlSkin.TextUnit;
import static javafx.scene.control.skin.TextInputControlSkin.Direction;
import static com.sun.javafx.scene.control.inputmap.InputMap.KeyMapping;
import static com.sun.javafx.scene.control.inputmap.InputMap.MouseMapping;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyEvent.*;

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

    final T textInputControl;

    protected ContextMenu contextMenu;

    private InvalidationListener textListener = observable -> invalidateBidi();

    private final InputMap<T> inputMap;




    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public TextInputControlBehavior(T c) {
        super(c);

        this.textInputControl = c;

        // create a map for text input-specific mappings (this reuses the default
        // InputMap installed on the control, if it is non-null, allowing us to pick up any user-specified mappings)
        inputMap = createInputMap();

        KeyMapping cancelEditMapping;
        KeyMapping fireMapping;
        KeyMapping consumeMostPressedEventsMapping;
        
        addKeyMap(textInputControl);

        // create a child input map for mappings which are applicable on all
        // platforms, and regardless of editing state
        addDefaultMapping(inputMap,
                // TODO the following key mappings should be moved to keyHandler

                fireMapping = keyMapping(ENTER, this::fire),
                keyMapping(new KeyBinding(Z).shortcut(), e -> undo()),

                // The following keys are forwarded to the parent container
                cancelEditMapping = new KeyMapping(ESCAPE, this::cancelEdit),

                // character input.
                // Any other key press first goes to normal text input
                // Note this is KEY_TYPED because otherwise the character is not available in the event.
                keyMapping(new KeyBinding(null, KEY_TYPED)
                                    .alt(OptionalBoolean.ANY)
                                    .shift(OptionalBoolean.ANY)
                                    .ctrl(OptionalBoolean.ANY)
                                    .meta(OptionalBoolean.ANY),
                           this::defaultKeyTyped),

                // However, we want to consume other key press / release events too, for
                // things that would have been handled by the InputCharacter normally
                consumeMostPressedEventsMapping =
                    keyMapping(new KeyBinding(null, KEY_PRESSED).shift(OptionalBoolean.ANY),
                               e -> { if (!e.getCode().isFunctionKey()) e.consume(); }),

                // VK
                new KeyMapping(new KeyBinding(DIGIT9).ctrl().shift(), e -> {
                    FXVK.toggleUseVK(textInputControl);
                }, p -> !PlatformImpl.isSupported(ConditionalFeature.VIRTUAL_KEYBOARD)),

                // mouse and context menu mappings
                new MouseMapping(MouseEvent.MOUSE_PRESSED, this::mousePressed),
                new MouseMapping(MouseEvent.MOUSE_DRAGGED, this::mouseDragged),
                new MouseMapping(MouseEvent.MOUSE_RELEASED, this::mouseReleased),
                new InputMap.Mapping<>(ContextMenuEvent.CONTEXT_MENU_REQUESTED, this::contextMenuRequested) {
                    @Override public int getSpecificity(Event event) {
                        return 1;
                    }
                }
        );

        cancelEditMapping.setAutoConsume(false);
        // fix of JDK-8207759: don't auto-consume
        fireMapping.setAutoConsume(false);
        consumeMostPressedEventsMapping.setAutoConsume(false);

        textInputControl.textProperty().addListener(textListener);

        contextMenu = new ContextMenu();
    }

    public void install() {
        TextInputControl c = getNode();

        func(Cmd.COPY, c::copy); // TODO move method to behavior
        func(Cmd.CUT, this::cut);
        func(Cmd.DELETE_FROM_LINE_START, this::deleteFromLineStart);
        func(Cmd.DELETE_NEXT_CHAR, this::deleteNextChar);
        func(Cmd.DELETE_NEXT_WORD, this::deleteNextWord);
        func(Cmd.DELETE_PREVIOUS_CHAR, this::deletePreviousChar);
        func(Cmd.DELETE_PREVIOUS_WORD, this::deletePreviousWord);
        func(Cmd.DESELECT, c::deselect); // TODO move method to behavior
        func(Cmd.HOME, c::home); // TODO move method to behavior
        func(Cmd.END, c::end); // TODO move method to behavior
        func(Cmd.LEFT, () -> nextCharacterVisually(false));
        func(Cmd.LEFT_WORD, this::leftWord);
        func(Cmd.PASTE, this::paste);
        func(Cmd.REDO, this::redo);
        func(Cmd.RIGHT, () -> nextCharacterVisually(true));
        func(Cmd.RIGHT_WORD, this::rightWord);
        func(Cmd.SELECT_ALL, this::selectAll);
        func(Cmd.SELECT_END, this::selectEnd);
        func(Cmd.SELECT_END_EXTEND, this::selectEndExtend);
        func(Cmd.SELECT_HOME, this::selectHome);
        func(Cmd.SELECT_HOME_EXTEND, this::selectHomeExtend);
        func(Cmd.SELECT_LEFT, this::selectLeft);
        func(Cmd.SELECT_LEFT_WORD, this::selectLeftWord);
        func(Cmd.SELECT_RIGHT, this::selectRight);
        func(Cmd.SELECT_RIGHT_WORD, this::selectRightWord);
        func(Cmd.TRAVERSE_NEXT, () -> FocusTraversalInputMap.traverseNext(c));
        func(Cmd.TRAVERSE_PREVIOUS, () -> FocusTraversalInputMap.traversePrevious(c));

        // common key bindings
        key(KeyBinding2.shortcut(C), Cmd.COPY);
        key(KeyBinding2.of(COPY), Cmd.COPY);
        key(KeyBinding2.shortcut(INSERT), Cmd.COPY);
        key(KeyBinding2.of(CUT), Cmd.CUT);
        key(KeyBinding2.shortcut(X), Cmd.CUT);
        key(KeyBinding2.of(DELETE), Cmd.DELETE_NEXT_CHAR);
        key(KeyBinding2.of(BACK_SPACE), Cmd.DELETE_PREVIOUS_CHAR);
        key(KeyBinding2.with(BACK_SPACE).shift().build(), Cmd.DELETE_PREVIOUS_CHAR);
        key(KeyBinding2.of(HOME), Cmd.HOME);
        key(KeyBinding2.with(HOME).shortcut().build(), Cmd.HOME);
        key(KeyBinding2.of(UP), Cmd.HOME);
        key(KeyBinding2.of(DOWN), Cmd.END);
        key(KeyBinding2.of(END), Cmd.END);
        key(KeyBinding2.with(END).shortcut().build(), Cmd.END);
        key(KeyBinding2.of(LEFT), Cmd.LEFT);
        key(KeyBinding2.of(PASTE), Cmd.PASTE);
        key(KeyBinding2.shift(INSERT), Cmd.PASTE);
        key(KeyBinding2.shortcut(V), Cmd.PASTE);
        key(KeyBinding2.of(RIGHT), Cmd.RIGHT);
        key(KeyBinding2.shift(DOWN), Cmd.SELECT_END);
        key(KeyBinding2.with(END).shortcut().shift().build(), Cmd.SELECT_END);
        key(KeyBinding2.with(HOME).shortcut().shift().build(), Cmd.SELECT_HOME);
        key(KeyBinding2.shift(UP), Cmd.SELECT_HOME);
        key(KeyBinding2.shift(LEFT), Cmd.SELECT_LEFT);
        key(KeyBinding2.shift(RIGHT), Cmd.SELECT_RIGHT);
        key(KeyBinding2.of(TAB), Cmd.TRAVERSE_NEXT);
        key(KeyBinding2.ctrl(TAB), Cmd.TRAVERSE_NEXT);
        key(KeyBinding2.shift(TAB), Cmd.TRAVERSE_PREVIOUS);
        key(KeyBinding2.with(TAB).ctrl().shift().build(), Cmd.TRAVERSE_PREVIOUS);

        // macOS key bindings
        key(KeyBinding2.with(BACK_SPACE).shortcut().forMac().build(), Cmd.DELETE_FROM_LINE_START);
        key(KeyBinding2.with(DELETE).alt().forMac().build(), Cmd.DELETE_NEXT_WORD);
        key(KeyBinding2.with(BACK_SPACE).alt().forMac().build(), Cmd.DELETE_PREVIOUS_WORD);
        key(KeyBinding2.with(HOME).shift().forMac().build(), Cmd.HOME);
        key(KeyBinding2.with(LEFT).shortcut().forMac().build(), Cmd.HOME);
        key(KeyBinding2.with(RIGHT).shortcut().forMac().build(), Cmd.END);
        key(KeyBinding2.with(LEFT).alt().forMac().build(), Cmd.LEFT_WORD);
        key(KeyBinding2.with(Z).shortcut().shift().forMac().build(), Cmd.REDO);
        key(KeyBinding2.with(RIGHT).alt().forMac().build(), Cmd.RIGHT_WORD);
        key(KeyBinding2.shortcut(A), Cmd.SELECT_ALL);
        key(KeyBinding2.with(LEFT).shortcut().shift().forMac().build(), Cmd.SELECT_HOME_EXTEND);
        key(KeyBinding2.with(RIGHT).shortcut().shift().forMac().build(), Cmd.SELECT_END_EXTEND);
        key(KeyBinding2.with(END).shift().forMac().build(), Cmd.SELECT_END_EXTEND);
        key(KeyBinding2.with(LEFT).shift().alt().forMac().build(), Cmd.SELECT_LEFT_WORD);
        key(KeyBinding2.with(RIGHT).shift().alt().forMac().build(), Cmd.SELECT_RIGHT_WORD);

        // windows key bindings
        key(KeyBinding2.with(Y).ctrl().forWindows().build(), Cmd.REDO);

        // linux key bindings
        key(KeyBinding2.with(Z).ctrl().shift().forLinux().build(), Cmd.REDO);

        // not-mac key bindings
        key(KeyBinding2.with(DELETE).ctrl().notForMac().build(), Cmd.DELETE_NEXT_WORD);
        key(KeyBinding2.with(H).ctrl().notForMac().build(), Cmd.DELETE_PREVIOUS_CHAR);
        key(KeyBinding2.with(BACK_SPACE).ctrl().notForMac().build(), Cmd.DELETE_PREVIOUS_WORD);
        key(KeyBinding2.with(BACK_SLASH).ctrl().notForMac().build(), Cmd.DESELECT);
        key(KeyBinding2.with(LEFT).ctrl().notForMac().build(), Cmd.LEFT_WORD);
        key(KeyBinding2.with(RIGHT).ctrl().notForMac().build(), Cmd.RIGHT_WORD);
        key(KeyBinding2.with(HOME).shift().notForMac().build(), Cmd.SELECT_HOME);
        key(KeyBinding2.with(END).shift().notForMac().build(), Cmd.SELECT_END);
        key(KeyBinding2.with(LEFT).ctrl().shift().notForMac().build(), Cmd.SELECT_LEFT_WORD);
        key(KeyBinding2.with(RIGHT).ctrl().shift().notForMac().build(), Cmd.SELECT_RIGHT_WORD);

        // key pad mappings
        addKeyPadMappings(inputMap);
    }

    @Override public InputMap<T> getInputMap() {
        return inputMap;
    }

    /**
     * Binds keypad arrow keys to the same function tags as the regular arrow keys.
     */
    protected void addKeyPadMappings(InputMap<T> map) {
        KeyMap m = textInputControl.getKeyMap();
        Set<KeyBinding2> keys = m.getKeyBindings();
        for (KeyBinding2 k: keys) {
            KeyCode cd = k.getKeyCode();
            if (cd != null) {
                KeyCode newCode = null;
                switch (cd) {
                case LEFT:
                    newCode = KP_LEFT;
                    break;
                case RIGHT:
                    newCode = KP_RIGHT;
                    break;
                case UP:
                    newCode = KP_UP;
                    break;
                case DOWN:
                    newCode = KP_DOWN;
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
                        ctrl(k.isCtrl()).
                        meta(k.isMeta()).
                        option(k.isOption()).
                        shift(k.isShift()).                        
                        build();
                    m.addAlias(k, newBinding);
                }
            }
        }
    }

    @Override
    protected void onKeyFunctionStart() {
        setCaretAnimating(false);
    }

    @Override
    protected void onKeyFunctionEnd() {
        setCaretAnimating(true);
    }

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
        textInputControl.textProperty().removeListener(textListener);
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
            bidi = new Bidi(textInputControl.textProperty().getValueSafe(),
                    (textInputControl.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT)
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
                                    textInputControl.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT));
        }
        return rtlText;
    }

    private void nextCharacterVisually(boolean moveRight) {
        if (isMixed()) {
            TextInputControlSkin<?> skin = (TextInputControlSkin<?>)textInputControl.getSkin();
            skin.moveCaret(TextUnit.CHARACTER, moveRight ? Direction.RIGHT : Direction.LEFT, false);
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
    
    boolean isEditable() {
        return textInputControl.isEditable();
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

    protected void fire(KeyEvent event) { } // TODO move to TextFieldBehavior
    protected void cancelEdit(KeyEvent event) { }

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
