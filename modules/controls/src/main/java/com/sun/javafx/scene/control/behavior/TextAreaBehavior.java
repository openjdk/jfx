/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.scene.control.Properties;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TextArea;
import com.sun.javafx.scene.control.skin.Utils;
import javafx.scene.input.ContextMenuEvent;
import com.sun.javafx.scene.control.inputmap.InputMap;
import com.sun.javafx.scene.control.inputmap.KeyBinding;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.HitInfo;
import javafx.stage.Screen;
import javafx.stage.Window;

import java.util.function.Predicate;

import static com.sun.javafx.PlatformUtil.isMac;
import static com.sun.javafx.PlatformUtil.isWindows;
import com.sun.javafx.stage.WindowHelper;
import static javafx.scene.control.skin.TextInputControlSkin.TextUnit;
import static javafx.scene.control.skin.TextInputControlSkin.Direction;
import static javafx.scene.input.KeyCode.*;


/**
 * Text area behavior.
 */
public class TextAreaBehavior extends TextInputControlBehavior<TextArea> {
    private TextAreaSkin skin;
    private TwoLevelFocusBehavior tlFocus;

    /**************************************************************************
     * Constructors                                                           *
     *************************************************************************/

    public TextAreaBehavior(final TextArea c) {
        super(c);

        if (Properties.IS_TOUCH_SUPPORTED) {
            contextMenu.getStyleClass().add("text-input-context-menu");
        }

        // some of the mappings are only valid when the control is editable, or
        // only on certain platforms, so we create the following predicates that filters out the mapping when the
        // control is not in the correct state / on the correct platform
        final Predicate<KeyEvent> validWhenEditable = e -> !c.isEditable();

        // Add these bindings as a child input map, so they take precedence
        InputMap<TextArea> textAreaInputMap = new InputMap<>(c);
        textAreaInputMap.getMappings().addAll(
            keyMapping(HOME,      e -> lineStart(false)),
            keyMapping(END,       e -> lineEnd(false)),
            keyMapping(UP,        e -> skin.moveCaret(TextUnit.LINE, Direction.UP,   false)),
            keyMapping(DOWN,      e -> skin.moveCaret(TextUnit.LINE, Direction.DOWN, false)),
            keyMapping(PAGE_UP,   e -> skin.moveCaret(TextUnit.PAGE, Direction.UP,   false)),
            keyMapping(PAGE_DOWN, e -> skin.moveCaret(TextUnit.PAGE, Direction.DOWN, false)),

            keyMapping(new KeyBinding(HOME).shift(),      e -> lineStart(true)),
            keyMapping(new KeyBinding(END).shift(),       e -> lineEnd(true)),
            keyMapping(new KeyBinding(UP).shift(),        e -> skin.moveCaret(TextUnit.LINE, Direction.UP,   true)),
            keyMapping(new KeyBinding(DOWN).shift(),      e -> skin.moveCaret(TextUnit.LINE, Direction.DOWN, true)),
            keyMapping(new KeyBinding(PAGE_UP).shift(),   e -> skin.moveCaret(TextUnit.PAGE, Direction.UP,   true)),
            keyMapping(new KeyBinding(PAGE_DOWN).shift(), e -> skin.moveCaret(TextUnit.PAGE, Direction.DOWN, true)),

            // editing-only mappings
            keyMapping(new KeyBinding(ENTER), e -> insertNewLine(), validWhenEditable),
            keyMapping(new KeyBinding(TAB), e -> insertTab(), validWhenEditable)
        );
        addDefaultChildMap(getInputMap(), textAreaInputMap);

        // mac os specific mappings
        InputMap<TextArea> macOsInputMap = new InputMap<>(c);
        macOsInputMap.setInterceptor(e -> !PlatformUtil.isMac());
        macOsInputMap.getMappings().addAll(
            // Mac OS specific mappings
            keyMapping(new KeyBinding(LEFT).shortcut(),  e -> lineStart(false)),
            keyMapping(new KeyBinding(RIGHT).shortcut(), e -> lineEnd(false)),
            keyMapping(new KeyBinding(UP).shortcut(),    e -> c.home()),
            keyMapping(new KeyBinding(DOWN).shortcut(),  e -> c.end()),

            keyMapping(new KeyBinding(LEFT).shortcut().shift(),  e -> lineStart(true)),
            keyMapping(new KeyBinding(RIGHT).shortcut().shift(), e -> lineEnd(true)),
            keyMapping(new KeyBinding(UP).shortcut().shift(),    e -> selectHomeExtend()),
            keyMapping(new KeyBinding(DOWN).shortcut().shift(),  e -> selectEndExtend()),

            keyMapping(new KeyBinding(UP).alt(),           e -> skin.moveCaret(TextUnit.PARAGRAPH, Direction.UP,   false)),
            keyMapping(new KeyBinding(DOWN).alt(),         e -> skin.moveCaret(TextUnit.PARAGRAPH, Direction.DOWN, false)),
            keyMapping(new KeyBinding(UP).alt().shift(),   e -> skin.moveCaret(TextUnit.PARAGRAPH, Direction.UP,   true)),
            keyMapping(new KeyBinding(DOWN).alt().shift(), e -> skin.moveCaret(TextUnit.PARAGRAPH, Direction.DOWN, true))
        );
        addDefaultChildMap(textAreaInputMap, macOsInputMap);

        // windows / linux specific mappings
        InputMap<TextArea> nonMacOsInputMap = new InputMap<>(c);
        nonMacOsInputMap.setInterceptor(e -> PlatformUtil.isMac());
        nonMacOsInputMap.getMappings().addAll(
            keyMapping(new KeyBinding(UP).ctrl(),           e -> skin.moveCaret(TextUnit.PARAGRAPH, Direction.UP,   false)),
            keyMapping(new KeyBinding(DOWN).ctrl(),         e -> skin.moveCaret(TextUnit.PARAGRAPH, Direction.DOWN, false)),
            keyMapping(new KeyBinding(UP).ctrl().shift(),   e -> skin.moveCaret(TextUnit.PARAGRAPH, Direction.UP,   true)),
            keyMapping(new KeyBinding(DOWN).ctrl().shift(), e -> skin.moveCaret(TextUnit.PARAGRAPH, Direction.DOWN, true))
        );
        addDefaultChildMap(textAreaInputMap, nonMacOsInputMap);

        addKeyPadMappings(textAreaInputMap);

        // Register for change events
        c.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                // NOTE: The code in this method is *almost* and exact copy of what is in TextFieldBehavior.
                // The only real difference is that TextFieldBehavior selects all the text when the control
                // receives focus (when not gained by mouse click), whereas TextArea doesn't, and also the
                // TextArea doesn't lose selection on focus lost, whereas the TextField does.
                final TextArea textArea = getNode();
                if (textArea.isFocused()) {
                    if (PlatformUtil.isIOS()) {
                        // Special handling of focus on iOS is required to allow to
                        // control native keyboard, because native keyboard is popped-up only when native
                        // text component gets focus. When we have JFX keyboard we can remove this code
                        final Bounds bounds = textArea.getBoundsInParent();
                        double w = bounds.getWidth();
                        double h = bounds.getHeight();
                        Affine3D trans = TextFieldBehavior.calculateNodeToSceneTransform(textArea);
                        String text = textArea.textProperty().getValueSafe();

                        // we need to display native text input component on the place where JFX component is drawn
                        // all parameters needed to do that are passed to native impl. here
                        WindowHelper.getPeer(textArea.getScene().getWindow()).requestInput(
                                text, TextFieldBehavior.TextInputTypes.TEXT_AREA.ordinal(), w, h,
                                trans.getMxx(), trans.getMxy(), trans.getMxz(), trans.getMxt(),
                                trans.getMyx(), trans.getMyy(), trans.getMyz(), trans.getMyt(),
                                trans.getMzx(), trans.getMzy(), trans.getMzz(), trans.getMzt());
                    }
                    if (!focusGainedByMouseClick) {
                        setCaretAnimating(true);
                    }
                } else {
//                    skin.hideCaret();
                    if (PlatformUtil.isIOS() && textArea.getScene() != null) {
                        // releasing the focus => we need to hide the native component and also native keyboard
                        WindowHelper.getPeer(textArea.getScene().getWindow()).releaseInput();
                    }
                    focusGainedByMouseClick = false;
                    setCaretAnimating(false);
                }
            }
        });

        // Only add this if we're on an embedded platform that supports 5-button navigation
        if (Utils.isTwoLevelFocus()) {
            tlFocus = new TwoLevelFocusBehavior(c); // needs to be last.
        }
    }

    @Override public void dispose() {
        if (tlFocus != null) tlFocus.dispose();
        super.dispose();
    }

    // An unholy back-reference!
    public void setTextAreaSkin(TextAreaSkin skin) {
        this.skin = skin;
    }

    private void insertNewLine() {
        setEditing(true);
        getNode().replaceSelection("\n");
        setEditing(false);
    }

    private void insertTab() {
        setEditing(true);
        getNode().replaceSelection("\t");
        setEditing(false);
    }

    @Override protected void deleteChar(boolean previous) {
        if (previous) {
            getNode().deletePreviousChar();
        } else {
            getNode().deleteNextChar();
        }
    }

    @Override protected void deleteFromLineStart() {
        TextArea textArea = getNode();
        int end = textArea.getCaretPosition();

        if (end > 0) {
            lineStart(false);
            int start = textArea.getCaretPosition();
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
        getNode().replaceText(start, end, txt);
    }

    /**
     * If the focus is gained via response to a mouse click, then we don't
     * want to select all the text even if selectOnFocus is true.
     */
    private boolean focusGainedByMouseClick = false; // TODO!!
    private boolean shiftDown = false;
    private boolean deferClick = false;

    @Override public void mousePressed(MouseEvent e) {
        TextArea textArea = getNode();
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
                    if (isMac()) {
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
        final TextArea textArea = getNode();
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
        final TextArea textArea = getNode();
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
        final TextArea textArea = getNode();

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
                    Point2D p = getNode().localToScene(menuPos);
                    Scene scene = getNode().getScene();
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
                getNode().getProperties().put("CONTEXT_MENU_SCREEN_X", screenX);
                getNode().getProperties().put("CONTEXT_MENU_SCENE_X", sceneX);
                contextMenu.show(getNode(), bounds.getMinX(), screenY);
            } else if (screenX + menuWidth > bounds.getMaxX()) {
                double leftOver = menuWidth - ( bounds.getMaxX() - screenX);
                getNode().getProperties().put("CONTEXT_MENU_SCREEN_X", screenX);
                getNode().getProperties().put("CONTEXT_MENU_SCENE_X", sceneX);
                contextMenu.show(getNode(), screenX - leftOver, screenY);
            } else {
                getNode().getProperties().put("CONTEXT_MENU_SCREEN_X", 0);
                getNode().getProperties().put("CONTEXT_MENU_SCENE_X", 0);
                contextMenu.show(getNode(), menuX, screenY);
            }
        }

        e.consume();
    }

    @Override protected void setCaretAnimating(boolean play) {
        skin.setCaretAnimating(play);
    }

    protected void mouseDoubleClick(HitInfo hit) {
        final TextArea textArea = getNode();
        textArea.previousWord();
        if (isWindows()) {
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
