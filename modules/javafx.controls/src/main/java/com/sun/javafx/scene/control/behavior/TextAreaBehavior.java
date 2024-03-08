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

import javafx.beans.value.ChangeListener;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.input.KeyBinding;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.control.skin.TextInputControlSkin.Direction;
import javafx.scene.control.skin.TextInputControlSkin.TextUnit;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.HitInfo;
import javafx.stage.Screen;
import javafx.stage.Window;
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.scene.control.Properties;
import com.sun.javafx.scene.control.skin.Utils;


/**
 * Text area behavior.
 */
public class TextAreaBehavior extends TextInputControlBehavior<TextArea> {
    private final TextAreaSkin skin;
    private TwoLevelFocusBehavior tlFocus;
    private ChangeListener<Boolean> focusListener;

    /**************************************************************************
     * Constructors                                                           *
     *************************************************************************/

    public TextAreaBehavior(TextArea c, TextAreaSkin skin) {
        super(c);
        this.skin = skin;
        if (Properties.IS_TOUCH_SUPPORTED) {
            contextMenu.getStyleClass().add("text-input-context-menu");
        }
    }

    @Override
    protected void populateSkinInputMap() {
        super.populateSkinInputMap();

        focusListener = (src, ov, nv) -> handleFocusChange();
        // Register for change events
        getControl().focusedProperty().addListener(focusListener);

        // Only add this if we're on an embedded platform that supports 5-button navigation
        if (Utils.isTwoLevelFocus()) {
            tlFocus = new TwoLevelFocusBehavior(getControl()); // needs to be last.
        }

        // functions
        registerFunction(TextArea.DOCUMENT_END, this::end);
        registerFunction(TextArea.DOCUMENT_START, this::home);
        registerFunction(TextArea.DOWN, (c) -> skin.moveCaret(TextUnit.LINE, Direction.DOWN, false));
        registerFunction(TextArea.LINE_START, (c) -> lineStart(false));
        registerFunction(TextArea.LINE_END, (c) -> lineEnd(false));
        registerFunction(TextArea.PARAGRAPH_DOWN, (c) -> skin.moveCaret(TextUnit.PARAGRAPH, Direction.DOWN, false));
        registerFunction(TextArea.PARAGRAPH_UP, (c) -> skin.moveCaret(TextUnit.PARAGRAPH, Direction.UP, false));
        registerFunction(TextArea.PAGE_DOWN, (c) -> skin.moveCaret(TextUnit.PAGE, Direction.DOWN, false));
        registerFunction(TextArea.PAGE_UP, (c) -> skin.moveCaret(TextUnit.PAGE, Direction.UP, false));
        registerFunction(TextArea.SELECT_DOWN, (c) -> skin.moveCaret(TextUnit.LINE, Direction.DOWN, true));
        //func(TextArea.SELECT_END_EXTEND, this::selectEndExtend);
        //func(TextArea.SELECT_HOME_EXTEND, this::selectHomeExtend);
        registerFunction(TextArea.SELECT_LINE_END, (c) -> lineEnd(true));
        registerFunction(TextArea.SELECT_PAGE_DOWN, (c) -> skin.moveCaret(TextUnit.PAGE, Direction.DOWN, true));
        registerFunction(TextArea.SELECT_PAGE_UP, (c) -> skin.moveCaret(TextUnit.PAGE, Direction.UP, true));
        registerFunction(TextArea.SELECT_PARAGRAPH_DOWN, (c) -> skin.moveCaret(TextUnit.PARAGRAPH, Direction.DOWN, true));
        registerFunction(TextArea.SELECT_PARAGRAPH_UP, (c) -> skin.moveCaret(TextUnit.PARAGRAPH, Direction.UP, true));
        registerFunction(TextArea.SELECT_LINE_START, (c) -> lineStart(true));
        registerFunction(TextArea.SELECT_UP, (c) -> skin.moveCaret(TextUnit.LINE, Direction.UP, true));
        registerFunction(TextArea.UP, (c) -> skin.moveCaret(TextUnit.LINE, Direction.UP, false));
        registerFunction(TextArea.INSERT_NEW_LINE, this::insertNewLine);
        registerFunction(TextArea.INSERT_TAB, this::insertTab);
        // TODO create functions instead of the inline lambdas above

        // common keys
        registerKey(KeyCode.DOWN, TextArea.DOWN);
        registerKey(KeyBinding.shift(KeyCode.DOWN), TextArea.SELECT_DOWN);
        registerKey(KeyBinding.shift(KeyCode.END), TextArea.SELECT_LINE_END);
        registerKey(KeyCode.END, TextArea.LINE_END);
        registerKey(KeyCode.ENTER, TextArea.INSERT_NEW_LINE);
        registerKey(KeyCode.HOME, TextArea.LINE_START);
        registerKey(KeyBinding.shift(KeyCode.HOME), TextArea.SELECT_LINE_START);
        registerKey(KeyCode.PAGE_UP, TextArea.PAGE_UP);
        registerKey(KeyBinding.shift(KeyCode.PAGE_UP), TextArea.SELECT_PAGE_UP);
        registerKey(KeyCode.PAGE_DOWN, TextArea.PAGE_DOWN);
        registerKey(KeyBinding.shift(KeyCode.PAGE_DOWN), TextArea.SELECT_PAGE_DOWN);
        registerKey(KeyCode.TAB, TextArea.INSERT_TAB);
        registerKey(KeyCode.UP, TextArea.UP);
        registerKey(KeyBinding.shift(KeyCode.UP), TextArea.SELECT_UP);

        if (isMac()) {
            // macOS specific mappings
            registerKey(KeyBinding.alt(KeyCode.DOWN), TextArea.PARAGRAPH_DOWN);
            registerKey(KeyBinding.with(KeyCode.DOWN).alt().shift().build(), TextArea.SELECT_PARAGRAPH_DOWN);
            registerKey(KeyBinding.shortcut(KeyCode.DOWN), TextArea.DOCUMENT_END);
            registerKey(KeyBinding.with(KeyCode.DOWN).shortcut().shift().build(), TextArea.SELECT_END_EXTEND);
            registerKey(KeyBinding.shortcut(KeyCode.LEFT), TextArea.LINE_START);
            registerKey(KeyBinding.with(KeyCode.LEFT).shortcut().shift().build(), TextArea.SELECT_LINE_START);
            registerKey(KeyBinding.shortcut(KeyCode.RIGHT), TextArea.LINE_END);
            registerKey(KeyBinding.with(KeyCode.RIGHT).shortcut().shift().build(), TextArea.SELECT_LINE_END);
            registerKey(KeyBinding.alt(KeyCode.UP), TextArea.PARAGRAPH_UP);
            registerKey(KeyBinding.with(KeyCode.UP).alt().shift().build(), TextArea.SELECT_PARAGRAPH_UP);
            registerKey(KeyBinding.shortcut(KeyCode.UP), TextArea.DOCUMENT_START);
            registerKey(KeyBinding.with(KeyCode.UP).shortcut().shift().build(), TextArea.SELECT_HOME_EXTEND);
        } else {
            // non-macOS specific mappings
            registerKey(KeyBinding.ctrl(KeyCode.DOWN), TextArea.PARAGRAPH_DOWN);
            registerKey(KeyBinding.ctrlShift(KeyCode.DOWN), TextArea.SELECT_PARAGRAPH_DOWN);
            registerKey(KeyBinding.ctrl(KeyCode.UP), TextArea.PARAGRAPH_UP);
            registerKey(KeyBinding.ctrlShift(KeyCode.UP), TextArea.SELECT_PARAGRAPH_UP);
        }

        addKeyPadMappings();
    }

    @Override
    public void dispose() {
        getControl().focusedProperty().removeListener(focusListener);
        if (tlFocus != null) {
            tlFocus.dispose();
        }
        super.dispose();
    }

    /**
     * Callback from the node's focusListener - this implementation handles
     * caret animation as appropriate.
     */
    private void handleFocusChange() {
        // FIXME: the code comment below is outdated
        // actually, this handler __has__ the exact same logic as TextField
        // (meanwhile, selection handling of TextField is separated out into focusOwnerLister)
        // The stumbling block against pulling it up into TextInputControlBehavior is
        // the focusGainedByMouseClick flag
        // NOTE: The code in this method is *almost* and exact copy of what is in TextFieldBehavior.
        // The only real difference is that TextFieldBehavior selects all the text when the control
        // receives focus (when not gained by mouse click), whereas TextArea doesn't, and also the
        // TextArea doesn't lose selection on focus lost, whereas the TextField does.
        final TextArea textArea = getControl();
        if (textArea.isFocused()) {
            if (!focusGainedByMouseClick) {
                setCaretAnimating(true);
            }
        } else {
//                    skin.hideCaret();
            focusGainedByMouseClick = false;
            setCaretAnimating(false);
        }
    }

    private void insertNewLine(TextArea c) {
        if (isEditable()) {
            setEditing(true);
            c.replaceSelection("\n");
            setEditing(false);
        }
    }

    private void insertTab(TextArea c) {
        if (isEditable()) {
            setEditing(true);
            c.replaceSelection("\t");
            setEditing(false);
        }
    }

    @Override protected void deleteChar(TextArea c, boolean previous) {
        if (previous) {
            c.deletePreviousChar();
        } else {
            c.deleteNextChar();
        }
    }

    @Override protected void deleteFromLineStart(TextArea c) {
        int end = c.getCaretPosition();
        if (end > 0) {
            lineStart(false);
            int start = c.getCaretPosition();
            if (end > start) {
                replaceText(start, end, "");
            }
        }
    }

    private void lineStart(boolean select) {
        skin.moveCaret(TextUnit.LINE, Direction.BEGINNING, select);
    }

    private void lineEnd(boolean select) {
        skin.moveCaret(TextUnit.LINE, Direction.END, select);
    }

    @Override protected void replaceText(int start, int end, String txt) {
        getControl().replaceText(start, end, txt);
    }

    /**
     * If the focus is gained via response to a mouse click, then we don't
     * want to select all the text even if selectOnFocus is true.
     */
    private boolean focusGainedByMouseClick = false; // TODO!!
    private boolean shiftDown = false;
    private boolean deferClick = false;

    @Override public void mousePressed(MouseEvent e) {
        TextArea textArea = getControl();
        // We never respond to events if disabled
        if (!textArea.isDisabled()) {
            // If the text field doesn't have focus, then we'll attempt to set
            // the focus and we'll indicate that we gained focus by a mouse
            // click, TODO which will then NOT honor the selectOnFocus variable
            // of the textInputControl
            if (!textArea.isFocused()) {
                focusGainedByMouseClick = true;
                textArea.requestFocus();
            }

            // stop the caret animation
            setCaretAnimating(false);
            // only if there is no selection should we see the caret
//            setCaretOpacity(if (textInputControl.dot == textInputControl.mark) then 1.0 else 0.0);

            // if the primary button was pressed
            if (e.getButton() == MouseButton.PRIMARY && !(e.isMiddleButtonDown() || e.isSecondaryButtonDown())) {
                HitInfo hit = skin.getIndex(e.getX(), e.getY());
                int i = hit.getInsertionIndex();
                final int anchor = textArea.getAnchor();
                final int caretPosition = textArea.getCaretPosition();
                if (e.getClickCount() < 2 &&
                    (e.isSynthesized() ||
                     (anchor != caretPosition &&
                      ((i > anchor && i < caretPosition) || (i < anchor && i > caretPosition))))) {
                    // if there is a selection, then we will NOT handle the
                    // press now, but will defer until the release. If you
                    // select some text and then press down, we change the
                    // caret and wait to allow you to drag the text (TODO).
                    // When the drag concludes, then we handle the click

                    deferClick = true;
                    // TODO start a timer such that after some millis we
                    // switch into text dragging mode, change the cursor
                    // to indicate the text can be dragged, etc.
                } else if (!(e.isControlDown() || e.isAltDown() || e.isShiftDown() || e.isMetaDown() || e.isShortcutDown())) {
                    switch (e.getClickCount()) {
                        case 1: skin.positionCaret(hit, false); break;
                        case 2: mouseDoubleClick(hit); break;
                        case 3: mouseTripleClick(hit); break;
                        default: // no-op
                    }
                } else if (e.isShiftDown() && !(e.isControlDown() || e.isAltDown() || e.isMetaDown() || e.isShortcutDown()) && e.getClickCount() == 1) {
                    // didn't click inside the selection, so select
                    shiftDown = true;
                    // if we are on mac os, then we will accumulate the
                    // selection instead of just moving the dot. This happens
                    // by figuring out past which (dot/mark) are extending the
                    // selection, and set the mark to be the other side and
                    // the dot to be the new position.
                    // everywhere else we just move the dot.
                    if (PlatformUtil.isMac()) {
                        textArea.extendSelection(i);
                    } else {
                        skin.positionCaret(hit, true);
                    }
                }
//                 skin.setForwardBias(hit.isLeading());
//                if (textInputControl.editable)
//                    displaySoftwareKeyboard(true);
            }
            if (contextMenu.isShowing()) {
                contextMenu.hide();
            }
        }
    }

    @Override public void mouseDragged(MouseEvent e) {
        final TextArea textArea = getControl();
        // we never respond to events if disabled, but we do notify any onXXX
        // event listeners on the control
        if (!textArea.isDisabled() && !e.isSynthesized()) {
            if (e.getButton() == MouseButton.PRIMARY &&
                    !(e.isMiddleButtonDown() || e.isSecondaryButtonDown() ||
                            e.isControlDown() || e.isAltDown() || e.isShiftDown() || e.isMetaDown())) {
                skin.positionCaret(skin.getIndex(e.getX(), e.getY()), true);
            }
        }
        deferClick = false;
    }

    @Override public void mouseReleased(final MouseEvent e) {
        final TextArea textArea = getControl();
        // we never respond to events if disabled, but we do notify any onXXX
        // event listeners on the control
        if (!textArea.isDisabled()) {
            setCaretAnimating(false);
            if (deferClick) {
                deferClick = false;
                skin.positionCaret(skin.getIndex(e.getX(), e.getY()), shiftDown);
                shiftDown = false;
            }
            setCaretAnimating(true);
        }
    }

    @Override public void contextMenuRequested(ContextMenuEvent e) {
        final TextArea textArea = getControl();

        if (contextMenu.isShowing()) {
            contextMenu.hide();
        } else if (textArea.getContextMenu() == null &&
                   textArea.getOnContextMenuRequested() == null) {
            double screenX = e.getScreenX();
            double screenY = e.getScreenY();
            double sceneX = e.getSceneX();

            if (Properties.IS_TOUCH_SUPPORTED) {
                Point2D menuPos;
                if (textArea.getSelection().getLength() == 0) {
                    skin.positionCaret(skin.getIndex(e.getX(), e.getY()), false);
                    menuPos = skin.getMenuPosition();
                } else {
                    menuPos = skin.getMenuPosition();
                    if (menuPos != null && (menuPos.getX() <= 0 || menuPos.getY() <= 0)) {
                        skin.positionCaret(skin.getIndex(e.getX(), e.getY()), false);
                        menuPos = skin.getMenuPosition();
                    }
                }

                if (menuPos != null) {
                    Point2D p = getControl().localToScene(menuPos);
                    Scene scene = getControl().getScene();
                    Window window = scene.getWindow();
                    Point2D location = new Point2D(window.getX() + scene.getX() + p.getX(),
                                                   window.getY() + scene.getY() + p.getY());
                    screenX = location.getX();
                    sceneX = p.getX();
                    screenY = location.getY();
                }
            }

            populateContextMenu();
            double menuWidth = contextMenu.prefWidth(-1);
            double menuX = screenX - (Properties.IS_TOUCH_SUPPORTED ? (menuWidth / 2) : 0);
            Screen currentScreen = com.sun.javafx.util.Utils.getScreenForPoint(screenX, 0);
            Rectangle2D bounds = currentScreen.getBounds();

            if (menuX < bounds.getMinX()) {
                getControl().getProperties().put("CONTEXT_MENU_SCREEN_X", screenX);
                getControl().getProperties().put("CONTEXT_MENU_SCENE_X", sceneX);
                contextMenu.show(getControl(), bounds.getMinX(), screenY);
            } else if (screenX + menuWidth > bounds.getMaxX()) {
                double leftOver = menuWidth - ( bounds.getMaxX() - screenX);
                getControl().getProperties().put("CONTEXT_MENU_SCREEN_X", screenX);
                getControl().getProperties().put("CONTEXT_MENU_SCENE_X", sceneX);
                contextMenu.show(getControl(), screenX - leftOver, screenY);
            } else {
                getControl().getProperties().put("CONTEXT_MENU_SCREEN_X", 0);
                getControl().getProperties().put("CONTEXT_MENU_SCENE_X", 0);
                contextMenu.show(getControl(), menuX, screenY);
            }
        }

        e.consume();
    }

    @Override protected void setCaretAnimating(boolean play) {
        skin.setCaretAnimating(play);
    }

    protected void mouseDoubleClick(HitInfo hit) {
        final TextArea textArea = getControl();
        textArea.previousWord();
        if (PlatformUtil.isWindows()) {
            textArea.selectNextWord();
        } else {
            textArea.selectEndOfNextWord();
        }
    }

    protected void mouseTripleClick(HitInfo hit) {
        // select the line
        skin.moveCaret(TextUnit.PARAGRAPH, Direction.BEGINNING, false);
        skin.moveCaret(TextUnit.PARAGRAPH, Direction.END, true);
    }
}
