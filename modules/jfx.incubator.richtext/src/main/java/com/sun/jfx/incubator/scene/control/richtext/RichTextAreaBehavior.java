/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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
// This code borrows heavily from the following project, with permission from the author:
// https://github.com/andy-goryachev/FxEditor

package com.sun.jfx.incubator.scene.control.richtext;

import java.io.IOException;
import java.text.Bidi;
import java.text.BreakIterator;
import java.util.List;
import java.util.function.BiFunction;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.util.Duration;
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.util.Utils;
import com.sun.jfx.incubator.scene.control.input.BehaviorBase;
import com.sun.jfx.incubator.scene.control.richtext.util.RichUtils;
import jfx.incubator.scene.control.input.KeyBinding;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.DataFormatHandler;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledInput;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;

/**
 * This class provides the RichTextArea behavior by registering input mappings and
 * implementing various event handlers.
 */
public class RichTextAreaBehavior extends BehaviorBase<RichTextArea> {
    private VFlow vflow;
    private final Timeline autoScrollTimer;
    private boolean autoScrollUp;
    private boolean fastAutoScroll;
    private boolean scrollStarted;
    /** horizontal cursor position preserved during up/down movement, in vflow.content coordinates */
    private double phantomX = -1.0;
    private final Duration autoScrollPeriod;
    private ContextMenu contextMenu = new ContextMenu();

    public RichTextAreaBehavior(RichTextArea control) {
        super(control);

        autoScrollPeriod = Duration.millis(Params.AUTO_SCROLL_PERIOD);

        autoScrollTimer = new Timeline(new KeyFrame(autoScrollPeriod, (ev) -> {
            autoScroll();
        }));
        autoScrollTimer.setCycleCount(Timeline.INDEFINITE);
    }

    @Override
    protected void populateSkinInputMap() {
        vflow = RichTextAreaSkinHelper.getVFlow(getControl());

        // functions
        registerFunction(RichTextArea.Tag.BACKSPACE, this::backspace);
        registerFunction(RichTextArea.Tag.COPY, this::copy);
        registerFunction(RichTextArea.Tag.CUT, this::cut);
        registerFunction(RichTextArea.Tag.DELETE, this::delete);
        registerFunction(RichTextArea.Tag.DELETE_PARAGRAPH, this::deleteParagraph);
        registerFunction(RichTextArea.Tag.DELETE_PARAGRAPH_START, this::deleteParagraphStart);
        registerFunction(RichTextArea.Tag.DELETE_WORD_NEXT_END, this::deleteWordNextEnd);
        registerFunction(RichTextArea.Tag.DELETE_WORD_NEXT_START, this::deleteWordNextBeg);
        registerFunction(RichTextArea.Tag.DELETE_WORD_PREVIOUS, this::deleteWordPrevious);
        registerFunction(RichTextArea.Tag.DESELECT, this::deselect);
        registerFunction(RichTextArea.Tag.FOCUS_NEXT, this::traverseNext);
        registerFunction(RichTextArea.Tag.FOCUS_PREVIOUS, this::traversePrevious);
        registerFunction(RichTextArea.Tag.INSERT_LINE_BREAK, this::insertLineBreak);
        registerFunction(RichTextArea.Tag.INSERT_TAB, this::insertTab);
        registerFunction(RichTextArea.Tag.MOVE_DOWN, this::moveDown);
        registerFunction(RichTextArea.Tag.MOVE_LEFT, this::moveLeft);
        registerFunction(RichTextArea.Tag.MOVE_PARAGRAPH_DOWN, this::moveParagraphDown);
        registerFunction(RichTextArea.Tag.MOVE_PARAGRAPH_UP, this::moveParagraphUp);
        registerFunction(RichTextArea.Tag.MOVE_RIGHT, this::moveRight);
        registerFunction(RichTextArea.Tag.MOVE_TO_DOCUMENT_END, this::moveDocumentEnd);
        registerFunction(RichTextArea.Tag.MOVE_TO_DOCUMENT_START, this::moveDocumentStart);
        registerFunction(RichTextArea.Tag.MOVE_TO_LINE_END, this::moveLineEnd);
        registerFunction(RichTextArea.Tag.MOVE_TO_LINE_START, this::moveLineStart);
        registerFunction(RichTextArea.Tag.MOVE_TO_PARAGRAPH_END, this::moveParagraphEnd);
        registerFunction(RichTextArea.Tag.MOVE_TO_PARAGRAPH_START, this::moveParagraphStart);
        registerFunction(RichTextArea.Tag.MOVE_UP, this::moveUp);
        registerFunction(RichTextArea.Tag.MOVE_WORD_NEXT_END, this::nextWordEnd);
        registerFunction(RichTextArea.Tag.MOVE_WORD_NEXT_START, this::nextWord);
        registerFunction(RichTextArea.Tag.MOVE_WORD_LEFT, this::leftWord);
        registerFunction(RichTextArea.Tag.MOVE_WORD_PREVIOUS, this::previousWord);
        registerFunction(RichTextArea.Tag.MOVE_WORD_RIGHT, this::rightWord);
        registerFunction(RichTextArea.Tag.PAGE_DOWN, this::pageDown);
        registerFunction(RichTextArea.Tag.PAGE_UP, this::pageUp);
        registerFunction(RichTextArea.Tag.PASTE, this::paste);
        registerFunction(RichTextArea.Tag.PASTE_PLAIN_TEXT, this::pastePlainText);
        registerFunction(RichTextArea.Tag.REDO, this::redo);
        registerFunction(RichTextArea.Tag.SELECT_ALL, this::selectAll);
        registerFunction(RichTextArea.Tag.SELECT_DOWN, this::selectDown);
        registerFunction(RichTextArea.Tag.SELECT_LEFT, this::selectLeft);
        registerFunction(RichTextArea.Tag.SELECT_PAGE_DOWN, this::selectPageDown);
        registerFunction(RichTextArea.Tag.SELECT_PAGE_UP, this::selectPageUp);
        registerFunction(RichTextArea.Tag.SELECT_PARAGRAPH, this::selectParagraph);
        registerFunction(RichTextArea.Tag.SELECT_PARAGRAPH_DOWN, this::selectParagraphDown);
        registerFunction(RichTextArea.Tag.SELECT_PARAGRAPH_END, this::selectParagraphEnd);
        registerFunction(RichTextArea.Tag.SELECT_PARAGRAPH_START, this::selectParagraphStart);
        registerFunction(RichTextArea.Tag.SELECT_PARAGRAPH_UP, this::selectParagraphUp);
        registerFunction(RichTextArea.Tag.SELECT_RIGHT, this::selectRight);
        registerFunction(RichTextArea.Tag.SELECT_TO_DOCUMENT_END, this::selectDocumentEnd);
        registerFunction(RichTextArea.Tag.SELECT_TO_DOCUMENT_START, this::selectDocumentStart);
        registerFunction(RichTextArea.Tag.SELECT_TO_LINE_END, this::selectLineEnd);
        registerFunction(RichTextArea.Tag.SELECT_TO_LINE_START, this::selectLineStart);
        registerFunction(RichTextArea.Tag.SELECT_UP, this::selectUp);
        registerFunction(RichTextArea.Tag.SELECT_WORD, this::selectWord);
        registerFunction(RichTextArea.Tag.SELECT_WORD_LEFT, this::selectWordLeft);
        registerFunction(RichTextArea.Tag.SELECT_WORD_NEXT, this::selectWordNext);
        registerFunction(RichTextArea.Tag.SELECT_WORD_NEXT_END, this::selectNextWordEnd);
        registerFunction(RichTextArea.Tag.SELECT_WORD_PREVIOUS, this::selectWordPrevious);
        registerFunction(RichTextArea.Tag.SELECT_WORD_RIGHT, this::selectWordRight);
        registerFunction(RichTextArea.Tag.UNDO, this::undo);

        // key mappings
        registerKey(KeyBinding.shortcut(KeyCode.A), RichTextArea.Tag.SELECT_ALL);
        registerKey(KeyCode.BACK_SPACE, RichTextArea.Tag.BACKSPACE);
        registerKey(KeyBinding.shift(KeyCode.BACK_SPACE), RichTextArea.Tag.BACKSPACE);
        registerKey(KeyBinding.shortcut(KeyCode.C), RichTextArea.Tag.COPY);
        registerKey(KeyCode.COPY, RichTextArea.Tag.COPY);
        registerKey(KeyBinding.shortcut(KeyCode.D), RichTextArea.Tag.DELETE_PARAGRAPH);
        registerKey(KeyCode.DELETE, RichTextArea.Tag.DELETE);
        registerKey(KeyCode.DOWN, RichTextArea.Tag.MOVE_DOWN);
        registerKey(KeyBinding.shift(KeyCode.DOWN), RichTextArea.Tag.SELECT_DOWN);
        registerKey(KeyCode.END, RichTextArea.Tag.MOVE_TO_LINE_END);
        registerKey(KeyBinding.control(KeyCode.END), RichTextArea.Tag.MOVE_TO_DOCUMENT_END);
        registerKey(KeyBinding.controlShift(KeyCode.END), RichTextArea.Tag.SELECT_TO_DOCUMENT_END);
        registerKey(KeyBinding.shift(KeyCode.END), RichTextArea.Tag.SELECT_TO_LINE_END);
        registerKey(KeyCode.ENTER, RichTextArea.Tag.INSERT_LINE_BREAK);
        registerKey(KeyCode.HOME, RichTextArea.Tag.MOVE_TO_LINE_START);
        registerKey(KeyBinding.control(KeyCode.HOME), RichTextArea.Tag.MOVE_TO_DOCUMENT_START);
        registerKey(KeyBinding.controlShift(KeyCode.HOME), RichTextArea.Tag.SELECT_TO_DOCUMENT_START);
        registerKey(KeyBinding.shift(KeyCode.HOME), RichTextArea.Tag.SELECT_TO_LINE_START);
        registerKey(KeyBinding.shift(KeyCode.INSERT), RichTextArea.Tag.PASTE);
        registerKey(KeyBinding.shortcut(KeyCode.INSERT), RichTextArea.Tag.COPY);
        registerKey(KeyCode.LEFT, RichTextArea.Tag.MOVE_LEFT);
        registerKey(KeyBinding.shift(KeyCode.LEFT), RichTextArea.Tag.SELECT_LEFT);
        registerKey(KeyCode.PAGE_DOWN, RichTextArea.Tag.PAGE_DOWN);
        registerKey(KeyBinding.shift(KeyCode.PAGE_DOWN), RichTextArea.Tag.SELECT_PAGE_DOWN);
        registerKey(KeyCode.PAGE_UP, RichTextArea.Tag.PAGE_UP);
        registerKey(KeyBinding.shift(KeyCode.PAGE_UP), RichTextArea.Tag.SELECT_PAGE_UP);
        registerKey(KeyCode.PASTE, RichTextArea.Tag.PASTE);
        registerKey(KeyCode.RIGHT, RichTextArea.Tag.MOVE_RIGHT);
        registerKey(KeyBinding.shift(KeyCode.RIGHT), RichTextArea.Tag.SELECT_RIGHT);
        registerKey(KeyCode.TAB, RichTextArea.Tag.INSERT_TAB);
        registerKey(KeyBinding.control(KeyCode.TAB), RichTextArea.Tag.FOCUS_NEXT);
        registerKey(KeyBinding.controlShift(KeyCode.TAB), RichTextArea.Tag.FOCUS_PREVIOUS);
        registerKey(KeyBinding.shift(KeyCode.TAB), RichTextArea.Tag.FOCUS_PREVIOUS);
        registerKey(KeyCode.UP, RichTextArea.Tag.MOVE_UP);
        registerKey(KeyBinding.shift(KeyCode.UP), RichTextArea.Tag.SELECT_UP);
        registerKey(KeyBinding.shortcut(KeyCode.V), RichTextArea.Tag.PASTE);
        registerKey(KeyBinding.shiftShortcut(KeyCode.V), RichTextArea.Tag.PASTE_PLAIN_TEXT);
        registerKey(KeyBinding.shortcut(KeyCode.X), RichTextArea.Tag.CUT);
        registerKey(KeyCode.CUT, RichTextArea.Tag.CUT);
        registerKey(KeyBinding.shortcut(KeyCode.Z), RichTextArea.Tag.UNDO);

        if (isMac()) {
            registerKey(KeyBinding.option(KeyCode.BACK_SPACE), RichTextArea.Tag.DELETE_WORD_PREVIOUS);
            registerKey(KeyBinding.shortcut(KeyCode.BACK_SPACE), RichTextArea.Tag.DELETE_PARAGRAPH_START);
            registerKey(KeyBinding.option(KeyCode.DELETE), RichTextArea.Tag.DELETE_WORD_NEXT_END);
            registerKey(KeyBinding.option(KeyCode.DOWN), RichTextArea.Tag.MOVE_PARAGRAPH_DOWN);
            registerKey(KeyBinding.shiftOption(KeyCode.DOWN), RichTextArea.Tag.SELECT_PARAGRAPH_DOWN);
            registerKey(KeyBinding.shiftShortcut(KeyCode.DOWN), RichTextArea.Tag.SELECT_TO_DOCUMENT_END);
            registerKey(KeyBinding.shortcut(KeyCode.DOWN), RichTextArea.Tag.MOVE_TO_DOCUMENT_END);
            registerKey(KeyBinding.option(KeyCode.LEFT), RichTextArea.Tag.MOVE_WORD_LEFT);
            registerKey(KeyBinding.shiftOption(KeyCode.LEFT), RichTextArea.Tag.SELECT_WORD_LEFT);
            registerKey(KeyBinding.shiftShortcut(KeyCode.LEFT), RichTextArea.Tag.SELECT_TO_LINE_START);
            registerKey(KeyBinding.shortcut(KeyCode.LEFT), RichTextArea.Tag.MOVE_TO_LINE_START);
            registerKey(KeyBinding.option(KeyCode.RIGHT), RichTextArea.Tag.MOVE_WORD_RIGHT);
            registerKey(KeyBinding.shiftOption(KeyCode.RIGHT), RichTextArea.Tag.SELECT_WORD_RIGHT);
            registerKey(KeyBinding.shiftShortcut(KeyCode.RIGHT), RichTextArea.Tag.SELECT_TO_LINE_END);
            registerKey(KeyBinding.shortcut(KeyCode.RIGHT), RichTextArea.Tag.MOVE_TO_LINE_END);
            registerKey(KeyBinding.builder(KeyCode.TAB).control().option().shift().build(), RichTextArea.Tag.FOCUS_NEXT);
            registerKey(KeyBinding.option(KeyCode.UP), RichTextArea.Tag.MOVE_PARAGRAPH_UP);
            registerKey(KeyBinding.shiftOption(KeyCode.UP), RichTextArea.Tag.SELECT_PARAGRAPH_UP);
            registerKey(KeyBinding.shiftShortcut(KeyCode.UP), RichTextArea.Tag.SELECT_TO_DOCUMENT_START);
            registerKey(KeyBinding.shortcut(KeyCode.UP), RichTextArea.Tag.MOVE_TO_DOCUMENT_START);
            registerKey(KeyBinding.builder(KeyCode.Z).shift().command().build(), RichTextArea.Tag.REDO);
        } else {
            registerKey(KeyBinding.control(KeyCode.BACK_SLASH), RichTextArea.Tag.DESELECT);
            registerKey(KeyBinding.control(KeyCode.BACK_SPACE), RichTextArea.Tag.DELETE_WORD_PREVIOUS);
            registerKey(KeyBinding.control(KeyCode.DELETE), RichTextArea.Tag.DELETE_WORD_NEXT_START);
            registerKey(KeyBinding.control(KeyCode.DOWN), RichTextArea.Tag.MOVE_PARAGRAPH_DOWN);
            registerKey(KeyBinding.controlShift(KeyCode.DOWN), RichTextArea.Tag.SELECT_PARAGRAPH_DOWN);
            registerKey(KeyBinding.control(KeyCode.H), RichTextArea.Tag.BACKSPACE);
            registerKey(KeyBinding.control(KeyCode.LEFT), RichTextArea.Tag.MOVE_WORD_LEFT);
            registerKey(KeyBinding.controlShift(KeyCode.LEFT), RichTextArea.Tag.SELECT_WORD_LEFT);
            registerKey(KeyBinding.control(KeyCode.RIGHT), RichTextArea.Tag.MOVE_WORD_RIGHT);
            registerKey(KeyBinding.controlShift(KeyCode.RIGHT), RichTextArea.Tag.SELECT_WORD_RIGHT);
            registerKey(KeyBinding.control(KeyCode.UP), RichTextArea.Tag.MOVE_PARAGRAPH_UP);
            registerKey(KeyBinding.controlShift(KeyCode.UP), RichTextArea.Tag.SELECT_PARAGRAPH_UP);

            if (isWindows()) {
                registerKey(KeyBinding.control(KeyCode.Y), RichTextArea.Tag.REDO);
            } else {
                registerKey(KeyBinding.controlShift(KeyCode.Z), RichTextArea.Tag.REDO);
            }
        }

        Pane cp = vflow.getContentPane();
        cp.addEventFilter(MouseEvent.MOUSE_CLICKED, this::handleMouseClicked);
        cp.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        cp.addEventFilter(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
        cp.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        cp.addEventFilter(ScrollEvent.SCROLL_STARTED, this::handleScrollEventStarted);
        cp.addEventHandler(ScrollEvent.SCROLL_FINISHED, this::handleScrollEventFinished);
        cp.addEventHandler(ScrollEvent.SCROLL, this::handleScrollEvent);

        addHandler(KeyEvent.KEY_TYPED, this::handleKeyTyped);
        addHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, this::contextMenuRequested);
    }

    protected boolean isRTL() {
        return (getControl().getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT);
    }

    protected String getPlainText(int modelIndex) {
        StyledTextModel m = getControl().getModel();
        return (m == null) ? "" : m.getPlainText(modelIndex);
    }

    protected void handleKeyTyped(KeyEvent ev) {
        if (ev == null || ev.isConsumed()) {
            return;
        }

        String typed = getValidKeyTyped(ev);
        if (typed != null) {
            vflow.setSuppressBlink(true);
            boolean consume = handleTypedChar(typed);
            if (consume) {
                ev.consume();
            }
            vflow.setSuppressBlink(false);
        }
    }

    protected boolean handleTypedChar(String typed) {
        if (canEdit()) {
            RichTextArea control = getControl();
            StyledTextModel m = control.getModel();
            TextPos start = control.getCaretPosition();
            if (start != null) {
                TextPos end = control.getAnchorPosition();
                if (end == null) {
                    end = start;
                }

                TextPos p = m.replace(vflow, start, end, typed, true);
                moveCaret(p, false);

                clearPhantomX();
                return true;
            }
        }
        return false;
    }

    protected String getValidKeyTyped(KeyEvent ev) {
        if (ev.getEventType() == KeyEvent.KEY_TYPED) {
            String ch = ev.getCharacter();
            if (ch.length() > 0) {
                // see TextInputControlBehavior:395
                // Filter out control keys except control+Alt on PC or Alt on Mac
                if (ev.isControlDown() || ev.isAltDown() || (PlatformUtil.isMac() && ev.isMetaDown())) {
                    if (!((ev.isControlDown() || PlatformUtil.isMac()) && ev.isAltDown())) {
                        return null;
                    }
                }

                // Ignore characters in the control range and the ASCII delete
                // character as well as meta key presses
                if (ch.charAt(0) > 0x1F && ch.charAt(0) != 0x7F && !ev.isMetaDown()) {
                    // Not sure about this one (original comment, not sure about it either)
                    return ch;
                }
            }
        }
        return null;
    }

    /** returns true if both control and model are editable */
    protected boolean canEdit() {
        RichTextArea control = getControl();
        if (control.isEditable()) {
            StyledTextModel m = control.getModel();
            if (m != null) {
                return m.isWritable();
            }
        }
        return false;
    }

    public boolean insertTab() {
        if (canEdit()) {
            handleTypedChar("\t");
            return true;
        }
        return false;
    }

    public void insertLineBreak() {
        if (canEdit()) {
            RichTextArea control = getControl();
            StyledTextModel m = control.getModel();
            TextPos start = control.getCaretPosition();
            if (start == null) {
                return;
            }
            TextPos end = control.getAnchorPosition();
            if (end == null) {
                return;
            }

            TextPos pos = m.replace(vflow, start, end, StyledInput.of("\n"), true);
            moveCaret(pos, false);
            clearPhantomX();
        }
    }

    protected void handleMouseClicked(MouseEvent ev) {
        if (ev.getButton() == MouseButton.PRIMARY) {
            int clicks = ev.getClickCount();
            switch (clicks) {
            case 2:
                getControl().selectWord();
                break;
            case 3:
                getControl().selectParagraph();
                break;
            }
        }
    }

    protected void handleMousePressed(MouseEvent ev) {
        if (ev.isPopupTrigger() || (ev.getButton() != MouseButton.PRIMARY)) {
            return;
        }

        TextPos pos = getTextPosition(ev);
        if (pos == null) {
            return;
        }

        vflow.setSuppressBlink(true);

        RichTextArea control = getControl();
        if (ev.isShiftDown()) {
            // expand selection from the anchor point to the current position
            // clearing existing selection
            control.extendSelection(pos);
        } else {
            control.select(pos, pos);
        }

        if (contextMenu.isShowing()) {
            contextMenu.hide();
        }

        control.requestFocus();
    }

    protected void handleMouseReleased(MouseEvent ev) {
        stopAutoScroll();
        //vflow.scrollCaretToVisible();
        vflow.setSuppressBlink(false);
        clearPhantomX();
    }

    protected void handleMouseDragged(MouseEvent ev) {
        if (!(ev.getButton() == MouseButton.PRIMARY)) {
            return;
        }

        double y = ev.getY();
        if (y < 0.0) {
            // above visible area
            autoScroll(y);
            return;
        } else if (y > vflow.getViewPortHeight()) {
            // below visible area
            autoScroll(y - vflow.getViewPortHeight());
            return;
        } else {
            stopAutoScroll();
        }

        TextPos pos = getTextPosition(ev);
        getControl().extendSelection(pos);
    }

    private void handleScrollEventStarted(ScrollEvent ev) {
        scrollStarted = true;
    }

    private void handleScrollEventFinished(ScrollEvent ev) {
        scrollStarted = false;
    }

    private void handleScrollEvent(ScrollEvent ev) {
        RichTextArea control = getControl();
        double dx = ev.getDeltaX();
        if (dx != 0.0) {
            // horizontal
            if (!control.isWrapText() && !control.isUseContentWidth()) {
                if(scrollStarted) {
                    // trackpad
                    vflow.scrollHorizontalPixels(-ev.getDeltaX());
                } else {
                    // mouse
                    double f = Params.SCROLL_SHEEL_BLOCK_SIZE_HORIZONTAL;
                    if (dx >= 0) {
                        f = -f;
                    }
                    vflow.scrollHorizontalFraction(f);
                }
                ev.consume();
            }
        }

        double dy = ev.getDeltaY();
        if (dy != 0.0) {
            // vertical
            if (!control.isUseContentHeight()) {
                if(scrollStarted) {
                    // trackpad
                    vflow.scrollVerticalPixels(-dy);
                } else {
                    // mouse
                    if (ev.isShortcutDown()) {
                        // page up / page down
                        if (dy >= 0) {
                            vflow.pageUp();
                        } else {
                            vflow.pageDown();
                        }
                    } else {
                        // block scroll
                        double f = Params.SCROLL_WHEEL_BLOCK_SIZE_VERTICAL;
                        if (dy >= 0) {
                            f = -f;
                        }
                        vflow.scrollVerticalFraction(f);
                    }
                }
                ev.consume();
            }
        }
    }

    protected TextPos getTextPosition(MouseEvent ev) {
        double x = ev.getScreenX();
        double y = ev.getScreenY();
        return getControl().getTextPosition(x, y);
    }

    protected void stopAutoScroll() {
        autoScrollTimer.stop();
    }

    protected void autoScroll(double delta) {
        autoScrollUp = (delta < 0.0);
        fastAutoScroll = Math.abs(delta) > Params.AUTO_SCROLL_FAST_THRESHOLD;
        autoScrollTimer.play();
    }

    protected void autoScroll() {
        RichTextArea control = getControl();
        if (control.isUseContentHeight()) {
            return;
        }
        double delta = fastAutoScroll ? Params.AUTO_SCROLL_STEP_FAST : Params.AUTO_SCROLL_STEP_SLOW;
        if (autoScrollUp) {
            delta = -delta;
        }
        vflow.scrollVerticalPixels(delta);
        vflow.layoutChildren();

        double x = Math.max(0.0, phantomX);
        double y = autoScrollUp ? 0.0 : vflow.getViewPortHeight();

        vflow.scrollToVisible(x, y);

        TextPos p = vflow.getTextPosLocal(x, y);
        control.extendSelection(p);
    }

    public void pageDown() {
        moveVertically(vflow.getViewPortHeight(), false);
    }

    public void pageUp() {
        moveVertically(-vflow.getViewPortHeight(), false);
    }

    public void moveRight() {
        moveCharacter(true, false);
    }

    public void moveLeft() {
        moveCharacter(false, false);
    }

    public void moveParagraphStart() {
        moveCaret(false, this::paragraphStart);
    }

    public void moveParagraphEnd() {
        moveCaret(false, this::paragraphEnd);
    }

    public void selectParagraphStart() {
        moveCaret(true, this::paragraphStart);
    }

    public void selectParagraphEnd() {
        moveCaret(true, this::paragraphEnd);
    }

    public void moveParagraphDown() {
        moveCaret(false, this::paragraphDown);
    }

    public void selectParagraphDown() {
        moveCaret(true, this::paragraphDown);
    }

    public void moveParagraphUp() {
        moveCaret(false, this::paragraphUp);
    }

    public void selectParagraphUp() {
        moveCaret(true, this::paragraphUp);
    }

    private TextPos paragraphDown(RichTextArea control, TextPos caret) {
        int ix = caret.index();
        TextPos end = control.getParagraphEnd(ix);
        if (caret.isSameInsertionIndex(end)) {
            ix++;
            if (ix >= control.getParagraphCount()) {
                return null;
            }
            return control.getParagraphEnd(ix);
        }
        return end;
    }

    public TextPos paragraphUp(RichTextArea control, TextPos caret) {
        int ix = caret.index();
        TextPos p = TextPos.ofLeading(ix, 0);
        if (caret.isSameInsertionIndex(p)) {
            --ix;
            if (ix < 0) {
                return null;
            }
            p = TextPos.ofLeading(ix, 0);
        }
        return p;
    }

    private TextPos paragraphEnd(RichTextArea control, TextPos caret) {
        int ix = caret.index();
        return control.getParagraphEnd(ix);
    }

    private void moveCaret(boolean extSelection, BiFunction<RichTextArea, TextPos, TextPos> h) {
        RichTextArea control = getControl();
        TextPos caret = control.getCaretPosition();
        if (caret != null) {
            TextPos p = h.apply(control, caret);
            if (p != null) {
                clearPhantomX();
                moveCaret(p, extSelection);
            }
        }
    }

    private TextPos paragraphStart(RichTextArea control, TextPos caret) {
        return TextPos.ofLeading(caret.index(), 0);
    }

    public void moveUp() {
        moveVertically(-1.0, false);
    }

    public void moveDown() {
        moveVertically(1.0, false);
    }

    /**
     * Moves the caret to before the first character of the text, also clearing the selection.
     */
    public void moveDocumentStart() {
        RichTextArea control = getControl();
        control.select(TextPos.ZERO);
    }

    /**
     * Moves the caret to after the last character of the text, also clearing the selection.
     */
    public void moveDocumentEnd() {
        RichTextArea control = getControl();
        TextPos pos = control.getDocumentEnd();
        control.select(pos);
    }

    /**
     * Moves the caret {@code deltaPixels} up (negative) or down (positive) from its current position.
     * Extends existing selection when {@code extendSelection} is true.
     * Does nothing if the current caret position is null.
     *
     * @param deltaPixels the number of pixels to move
     * @param extendSelection whether to extend selection
     */
    protected void moveVertically(double deltaPixels, boolean extendSelection) {
        TextPos caret = getControl().getCaretPosition();
        if (caret == null) {
            return;
        }

        vflow.scrollCaretToVisible();

        CaretInfo ci = vflow.getCaretInfo(caret);
        if (ci == null) {
            return;
        }

        double x = (ci.getMinX() + ci.getMaxX()) / 2.0;
        if (phantomX < 0) {
            // phantomX is unclear in the case of split caret
            // TODO possibly use effectiveOrientation to determine which side we should use
            phantomX = x;
        } else {
            x = phantomX;
        }

        boolean down = (deltaPixels > 0);
        double y = down ?
            ci.getMaxY() + deltaPixels + 0.5 :
            ci.getMinY() + deltaPixels - 0.5;

        TextPos p = vflow.moveVertically(caret.index(), x, y, down);
        if (p != null) {
            moveCaret(p, extendSelection);
        }
    }

    protected void moveHorizontally(boolean start, boolean extendSelection) {
        TextPos caret = getControl().getCaretPosition();
        if (caret == null) {
            return;
        }

        vflow.scrollCaretToVisible();

        int ix = caret.index();
        int off = caret.charIndex();
        TextPos p = vflow.moveHorizontally(start, ix, off);
        if (p != null) {
            clearPhantomX();
            moveCaret(p, extendSelection);
        }
    }

    protected void moveLineEnd() {
        moveHorizontally(false, false);
    }

    protected void moveLineStart() {
        moveHorizontally(true, false);
    }

    protected void moveCharacter(boolean moveRight, boolean extendSelection) {
        // TODO bidi
        RichTextArea control = getControl();
        TextPos caret = control.getCaretPosition();
        if (caret == null) {
            return;
        }

        clearPhantomX();

        if (!extendSelection) {
            TextPos ca = control.getCaretPosition();
            TextPos an = control.getAnchorPosition();
            int d = ca.compareTo(an);
            // jump over selection if it exists
            if (d < 0) {
                moveCaret(moveRight ? an : ca, extendSelection);
                return;
            } else if (d > 0) {
                moveCaret(moveRight ? ca : an, extendSelection);
                return;
            }
        }

        TextPos p = nextCharacterVisually(caret, moveRight);
        if (p != null) {
            moveCaret(p, extendSelection);
        }
    }

    protected TextPos nextCharacterVisually(TextPos start, boolean moveRight) {
        if (isRTL()) {
            moveRight = !moveRight;
        }

        RichTextArea control = getControl();
        TextCell cell = vflow.getCell(start.index());
        int cix = start.offset();
        if (moveRight) {
            cix++;
            if (cix > cell.getTextLength()) {
                int ix = cell.getIndex() + 1;
                TextPos p;
                if (ix < control.getParagraphCount()) {
                    // next line
                    p = TextPos.ofLeading(ix, 0);
                } else {
                    // end of last paragraph w/o newline
                    p = TextPos.ofLeading(cell.getIndex(), cell.getTextLength());
                }
                return p;
            }
        } else {
            if (start.offset() == 0) {
                int ix = cell.getIndex() - 1;
                if (ix >= 0) {
                    // end of prev line
                    return control.getParagraphEnd(ix);
                }
                return null;
            }
        }

        // using default locale, same as TextInputControl.backward() for example
        BreakIterator br = BreakIterator.getCharacterInstance();
        String text = getPlainText(cell.getIndex());
        br.setText(text);
        int off = start.offset();
        try {
            int ix = moveRight ? br.following(off) : br.preceding(off);
            if (ix == BreakIterator.DONE) {
                System.err.println(" --- SHOULD NOT HAPPEN: BreakIterator.DONE off=" + off); // FIX
                return null;
            }
            return TextPos.ofLeading(start.index(), ix);
        } catch(Exception e) {
            // TODO need to use a logger!
            System.err.println("offset=" + off + " text=[" + text + "]"); // FIX
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Moves the caret and anchor to the new position, unless {@code extendSelection} is true, in which case
     * extend selection from the existing anchor to the newly set caret position.
     * @param p text position
     * @param extendSelection specifies whether to clear (false) or extend (true) any existing selection
     */
    protected void moveCaret(TextPos p, boolean extendSelection) {
        RichTextArea control = getControl();
        if (extendSelection) {
            control.extendSelection(p);
        } else {
            control.select(p, p);
        }
    }

    public void clearPhantomX() {
        phantomX = -1.0;
    }

    public void selectLeft() {
        moveCharacter(false, true);
    }

    public void selectRight() {
        moveCharacter(true, true);
    }

    public void selectUp() {
        moveVertically(-1.0, true);
    }

    public void selectDown() {
        moveVertically(1.0, true);
    }

    protected void selectLineEnd() {
        moveHorizontally(false, true);
    }

    protected void selectLineStart() {
        moveHorizontally(true, true);
    }

    public void selectPageDown() {
        moveVertically(vflow.getViewPortHeight(), true);
    }

    public void selectPageUp() {
        moveVertically(-vflow.getViewPortHeight(), true);
    }

    public void selectAll() {
        RichTextArea control = getControl();
        TextPos end = control.getDocumentEnd();
        control.select(TextPos.ZERO, end);
        clearPhantomX();
    }

    /** selects from the anchor position to the document start */
    public void selectDocumentStart() {
        getControl().extendSelection(TextPos.ZERO);
    }

    /** selects from the anchor position to the document end */
    public void selectDocumentEnd() {
        RichTextArea control = getControl();
        TextPos pos = control.getDocumentEnd();
        control.extendSelection(pos);
    }

    public void selectWord() {
        RichTextArea control = getControl();
        TextPos caret = control.getCaretPosition();
        if (caret == null) {
            return;
        }

        int index = caret.index();
        String text = getPlainText(index);

        // using default locale, same as TextInputControl.backward() for example
        BreakIterator br = BreakIterator.getWordInstance();
        br.setText(text);
        int off = caret.offset();
        try {
            int off0 = br.preceding(off);
            if (off0 == BreakIterator.DONE) {
                //System.err.println(" --- no previous word off=" + off); // FIX
                return;
            }

            int off1 = br.following(off);
            if (off1 == BreakIterator.DONE) {
                //System.err.println(" --- no following word off=" + off); // FIX
                return;
            }

            TextPos p0 = TextPos.ofLeading(index, off0);
            TextPos p1 = TextPos.ofLeading(index, off1);
            control.select(p0, p1);
        } catch (Exception e) {
            // TODO need to use a logger!
            System.err.println("offset=" + off + " text=[" + text + "]");
            e.printStackTrace();
        }
    }

    public void selectParagraph() {
        RichTextArea control = getControl();
        TextPos p = control.getCaretPosition();
        if (p != null) {
            int ix = p.index();
            TextPos an = TextPos.ofLeading(ix, 0);
            TextPos ca = control.getParagraphEnd(ix);
            control.select(an, ca);
        }
    }

    public void backspace() {
        if (canEdit()) {
            RichTextArea control = getControl();
            if (control.hasNonEmptySelection()) {
                deleteSelection();
            } else {
                TextPos p = control.getCaretPosition();
                if (p == null) {
                    return;
                }

                int ix = p.index();

                TextPos start;
                if (p.offset() == 0) {
                    if (ix == 0) {
                        return;
                    }
                    int off = getPlainText(ix - 1).length();
                    start = TextPos.ofLeading(ix - 1, off);
                } else {
                    String text = getPlainText(p.index());
                    // Do not use charIterator here, because we do want to
                    // break up clusters when deleting backwards.
                    int off = Character.offsetByCodePoints(text, p.offset(), -1);
                    start = TextPos.ofLeading(ix, off);
                }

                control.getModel().replace(vflow, start, p, StyledInput.EMPTY, true);
                moveCaret(start, false);
                clearPhantomX();
            }
        }
    }

    public void delete() {
        if (canEdit()) {
            RichTextArea control = getControl();
            if (control.hasNonEmptySelection()) {
                deleteSelection();
            } else {
                TextPos start = control.getCaretPosition();
                TextPos end = nextCharacterVisually(start, true);
                if (end != null) {
                    control.getModel().replace(vflow, start, end, StyledInput.EMPTY, true);
                    moveCaret(start, false);
                    clearPhantomX();
                }
            }
        }
    }

    private SelInfo sel() {
        return SelInfo.get(getControl());
    }

    private TextPos clamp(TextPos p) {
        return getControl().getModel().clamp(p);
    }

    public void deleteParagraph() {
        if (canEdit()) {
            SelInfo sel = sel();
            if (sel != null) {
                int ix0 = sel.getMin().index();
                int ix1 = sel.getMax().index();

                TextPos p0 = TextPos.ofLeading(ix0, 0);
                TextPos p1 = clamp(TextPos.ofLeading(ix1 + 1, 0));
                RichTextArea control = getControl();
                control.getModel().replace(vflow, p0, p1, StyledInput.EMPTY, true);
                clearPhantomX();
                moveCaret(p0, false);
            }
        }
    }

    public void deleteParagraphStart() {
        deleteIgnoreSelection(this::paragraphStart);
    }

    protected void deleteSelection() {
        SelInfo sel = sel();
        if (sel != null) {
            TextPos start = sel.getMin();
            TextPos end = sel.getMax();
            RichTextArea control = getControl();
            control.getModel().replace(vflow, start, end, StyledInput.EMPTY, true);
            clearPhantomX();
            moveCaret(start, false);
        }
    }

    protected void deselect() {
        RichTextArea control = getControl();
        TextPos p = control.getCaretPosition();
        if (p != null) {
            clearPhantomX();
            moveCaret(p, false);
        }
    }

    // see TextAreaBehavior:338
    public void contextMenuRequested(ContextMenuEvent ev) {
        RichTextArea control = getControl();
        if (contextMenu.isShowing()) {
            contextMenu.hide();
        } else if (control.getContextMenu() == null && control.getOnContextMenuRequested() == null) {
            double screenX = ev.getScreenX();
            double screenY = ev.getScreenY();
            double sceneX = ev.getSceneX();

            /* TODO
            if (NewAPI.isTouchSupported()) {
                 Point2D menuPos;
                if (control.getSelection().getLength() == 0) {
                    skin.positionCaret(skin.getIndex(ev.getX(), ev.getY()), false);
                    menuPos = skin.getMenuPosition();
                } else {
                    menuPos = skin.getMenuPosition();
                    if (menuPos != null && (menuPos.getX() <= 0 || menuPos.getY() <= 0)) {
                        skin.positionCaret(skin.getIndex(ev.getX(), ev.getY()), false);
                        menuPos = skin.getMenuPosition();
                    }
                }

                if (menuPos != null) {
                    Point2D p = control.localToScene(menuPos);
                    Scene scene = control.getScene();
                    Window window = scene.getWindow();
                    Point2D location = new Point2D(window.getX() + scene.getX() + p.getX(),
                        window.getY() + scene.getY() + p.getY());
                    screenX = location.getX();
                    sceneX = p.getX();
                    screenY = location.getY();
                }
            }
            */

            populateContextMenu();

            double menuWidth = contextMenu.prefWidth(-1);
            double menuX = screenX - (RichUtils.isTouchSupported() ? (menuWidth / 2) : 0);
            Screen currentScreen = Utils.getScreenForPoint(screenX, 0);
            Rectangle2D bounds = currentScreen.getBounds();

            // what is this??
            if (menuX < bounds.getMinX()) {
                control.getProperties().put("CONTEXT_MENU_SCREEN_X", screenX);
                control.getProperties().put("CONTEXT_MENU_SCENE_X", sceneX);
                contextMenu.show(control, bounds.getMinX(), screenY);
            } else if (screenX + menuWidth > bounds.getMaxX()) {
                double leftOver = menuWidth - (bounds.getMaxX() - screenX);
                control.getProperties().put("CONTEXT_MENU_SCREEN_X", screenX);
                control.getProperties().put("CONTEXT_MENU_SCENE_X", sceneX);
                contextMenu.show(control, screenX - leftOver, screenY);
            } else {
                control.getProperties().put("CONTEXT_MENU_SCREEN_X", 0);
                control.getProperties().put("CONTEXT_MENU_SCENE_X", 0);
                contextMenu.show(control, menuX, screenY);
            }
        }

        ev.consume();
    }

    // TODO this might belong to the control!
    protected void populateContextMenu() {
        RichTextArea control = getControl();
        boolean sel = control.hasNonEmptySelection();
        boolean paste = (findFormatForPaste() != null);
        boolean editable = canEdit();

        ObservableList<MenuItem> items = contextMenu.getItems();
        items.clear();

        MenuItem m;
        items.add(m = new MenuItem("Undo"));
        m.setOnAction((ev) -> control.undo());
        m.setDisable(!control.isUndoable());

        items.add(m = new MenuItem("Redo"));
        m.setOnAction((ev) -> control.redo());
        m.setDisable(!control.isRedoable());

        items.add(new SeparatorMenuItem());

        items.add(m = new MenuItem("Cut"));
        m.setOnAction((ev) -> control.cut());
        m.setDisable(!sel || !editable);

        items.add(m = new MenuItem("Copy"));
        m.setOnAction((ev) -> control.copy());
        m.setDisable(!sel);

        items.add(m = new MenuItem("Paste"));
        m.setOnAction((ev) -> control.paste());
        m.setDisable(!paste || !editable);

        items.add(new SeparatorMenuItem());

        items.add(m = new MenuItem("Select All"));
        m.setOnAction((ev) -> control.selectAll());
    }

    public void copy() {
        copyWithCut(false);
    }

    public void cut() {
        if (canEdit()) {
            copyWithCut(true);
        }
    }

    public void paste() {
        if (canEdit()) {
            DataFormat f = findFormatForPaste();
            if (f != null) {
                pasteLocal(f);
            }
        }
    }

    public void pasteWithFormat(DataFormat f) {
        if (canEdit()) {
            Clipboard c = Clipboard.getSystemClipboard();
            if (c.hasContent(f)) {
                pasteLocal(f);
            }
        }
    }

    public void pastePlainText() {
        pasteWithFormat(DataFormat.PLAIN_TEXT);
    }

    /**
     * returns a format that can be imported by a model, based on the clipboard content and model being editable.
     */
    protected DataFormat findFormatForPaste() {
        if (canEdit()) {
            StyledTextModel m = getControl().getModel();
            List<DataFormat> fs = m.getSupportedDataFormats(false);
            if (fs.size() > 0) {
                for (DataFormat f : fs) {
                    if (Clipboard.getSystemClipboard().hasContent(f)) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    private void pasteLocal(DataFormat f) {
        SelInfo sel = sel();
        if (sel != null) {
            RichTextArea control = getControl();
            TextPos start = sel.getMin();
            TextPos end = sel.getMax();

            StyledTextModel m = control.getModel();
            DataFormatHandler h = m.getDataFormatHandler(f, false);
            Object x = Clipboard.getSystemClipboard().getContent(f);
            String text;
            if (x instanceof String s) {
                text = s;
            } else {
                return;
            }

            StyleAttributeMap a = control.getActiveStyleAttributeMap();
            try (StyledInput in = h.createStyledInput(text, a)) {
                TextPos p = m.replace(vflow, start, end, in, true);
                moveCaret(p, false);
            } catch (IOException e) {
                control.errorFeedback();
            }
        }
    }

    protected void copyWithCut(boolean cut) {
        RichTextArea control = getControl();
        if (control.hasNonEmptySelection()) {
            StyledTextModel m = control.getModel(); // non null at this point
            List<DataFormat> fs = m.getSupportedDataFormats(true);
            if (fs.size() > 0) {
                SelInfo sel = sel();
                if (sel == null) {
                    return;
                }

                TextPos start = sel.getMin();
                TextPos end = sel.getMax();

                try {
                    ClipboardContent c = new ClipboardContent();
                    for (DataFormat f : fs) {
                        DataFormatHandler h = m.getDataFormatHandler(f, true);
                        Object v = h.copy(m, vflow, start, end);
                        if (v != null) {
                            c.put(f, v);
                        }
                    }
                    Clipboard.getSystemClipboard().setContent(c);

                    if (cut) {
                        if (control.isEditable()) {
                            deleteSelection();
                        } else {
                            throw new UnsupportedOperationException("control is not editable");
                        }
                    }
                } catch(Exception | OutOfMemoryError e) {
                    control.errorFeedback();
                }
            }
        }
    }

    public void copyWithFormat(DataFormat f) {
        RichTextArea control = getControl();
        try {
            if (control.hasNonEmptySelection()) {
                StyledTextModel m = control.getModel(); // not null at this point
                DataFormatHandler h = m.getDataFormatHandler(f, true);
                if (h != null) {
                    SelInfo sel = sel();
                    if (sel == null) {
                        return;
                    }

                    TextPos start = sel.getMin();
                    TextPos end = sel.getMax();

                    Object v = h.copy(m, vflow, start, end);
                    ClipboardContent c = new ClipboardContent();
                    c.put(f, v);
                    Clipboard.getSystemClipboard().setContent(c);
                }
            }
        } catch(Exception | OutOfMemoryError e) {
            control.errorFeedback();
        }
    }

    /**
     * Moves the caret to the beginning of previous word. This function
     * also has the effect of clearing the selection.
     */
    public void previousWord() {
        moveCaret(false, this::toPreviousWord);
    }

    /** moves the caret to the beginning of the previos word (LTR) or next word (RTL) */
    public void leftWord() {
        toLeftWord(false);
    }

    /** moves the caret to the beginning of the next word (LTR) or previous word (RTL) */
    public void rightWord() {
        toRightWord(false);
    }

    /**
     * Moves the caret to the beginning of next word. This function
     * also has the effect of clearing the selection.
     */
    public void nextWord() {
        moveCaret(false, this::nextWordBeg);
    }

    /**
     * Moves the caret to the end of the next word. This function
     * also has the effect of clearing the selection.
     */
    public void nextWordEnd() {
        moveCaret(false, this::toWordEnd);
    }

    /**
     * Moves the caret to the beginning of previous word. This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of previous word.
     */
    public void selectWordPrevious() {
        moveCaret(true, this::toPreviousWord);
    }

    /**
     * Moves the caret to the beginning of next word. This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of next word.
     */
    public void selectWordNext() {
        moveCaret(true, this::nextWordBeg);
    }

    /**
     * Moves the caret to the end of the next word. This does not cause
     * the selection to be cleared.
     */
    public void selectNextWordEnd() {
        moveCaret(true, this::toWordEnd);
    }

    /**
     * Moves the caret to the beginning of previous word (LTR) or next word (LTR).
     * This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of previous word.
     */
    public void selectWordLeft() {
        toLeftWord(true);
    }

    /**
     * Moves the caret to the beginning of next word (LTR) or previous word (RTL).
     * This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of next word.
     */
    public void selectWordRight() {
        toRightWord(true);
    }

    protected void toLeftWord(boolean extendSelection) {
        if (isRTLText()) {
            if (isWindows()) {
                moveCaret(extendSelection, this::nextWordBeg);
            } else {
                moveCaret(extendSelection, this::toWordEnd);
            }
        } else {
            moveCaret(extendSelection, this::toPreviousWord);
        }
    }

    protected void toRightWord(boolean extendSelection) {
        if (isRTLText()) {
            moveCaret(extendSelection, this::toPreviousWord);
        } else {
            if (isWindows()) {
                moveCaret(extendSelection, this::nextWordBeg);
            } else {
                moveCaret(extendSelection, this::toWordEnd);
            }
        }
    }

    protected TextPos toPreviousWord(RichTextArea control, TextPos pos) {
        int index = pos.index();
        int offset = pos.offset();
        BreakIterator br = null;

        for (;;) {
            if ((index == 0) && (offset <= 0)) {
                return TextPos.ZERO;
            }

            String text = getPlainText(index);
            if (text.length() == 0) {
                index--;
                offset = Integer.MAX_VALUE;
                continue;
            }

            if (br == null) {
                br = BreakIterator.getWordInstance();
            }
            br.setText(text);

            int len = text.length();
            int off = br.preceding(Utils.clamp(0, offset, len));

            while (off != BreakIterator.DONE && !RichUtils.isLetterOrDigit(text, off)) {
                off = br.preceding(Utils.clamp(0, off, len));
            }

            if (off < 0) {
                index--;
                offset = Integer.MAX_VALUE;
                continue;
            }
            return TextPos.ofLeading(index, off);
        }
    }

    // skips empty paragraphs
    protected TextPos nextWordBeg(RichTextArea control, TextPos pos) {
        int index = pos.index();
        int offset = pos.offset();
        boolean skipEmpty = true;
        BreakIterator br = null;

        for (;;) {
            TextPos end = control.getDocumentEnd();
            // this could be a isSameOrAfter(index, off) method in TextPos
            if ((index == end.index()) && (offset >= end.offset())) {
                return end;
            } else if (index > end.index()) {
                return end;
            }

            String text = getPlainText(index);
            if (text.length() == 0) {
                if (skipEmpty) {
                    index++;
                }
                return TextPos.ofLeading(index, 0);
            }

            if (br == null) {
                br = BreakIterator.getWordInstance();
            }
            br.setText(text);

            int len = text.length();
            if (offset == len) {
                return TextPos.ofLeading(++index, 0);
            }

            int next = br.following(Utils.clamp(0, offset, len));
            if ((next == BreakIterator.DONE) || (next == len)) {
                return TextPos.ofLeading(index, len);
            } else {
                while (next != BreakIterator.DONE) {
                    boolean inWord = RichUtils.isLetterOrDigit(text, next);
                    if (inWord) {
                        return TextPos.ofLeading(index, next);
                    }
                    next = br.next();
                }
            }

            index++;
            offset = 0;
            skipEmpty = false;
        }
    }

    // skips empty paragraphs
    protected TextPos toWordEnd(RichTextArea control, TextPos pos) {
        int index = pos.index();
        int offset = pos.offset();
        boolean skipEmpty = true;
        BreakIterator br = null;

        for (;;) {
            TextPos end = control.getDocumentEnd();
            // this could be a isSameOrAfter(index, off) method in TextPos
            if ((index == end.index()) && (offset >= end.offset())) {
                return end;
            } else if (index > end.index()) {
                return end;
            }

            String text = getPlainText(index);
            if (text.length() == 0) {
                if (skipEmpty) {
                    index++;
                }
                return TextPos.ofLeading(index, 0);
            }

            if (br == null) {
                br = BreakIterator.getWordInstance();
            }
            br.setText(text);

            boolean inWord = RichUtils.isLetterOrDigit(text, offset);
            int len = text.length();
            int next = br.following(Utils.clamp(0, offset, len));
            if (next == BreakIterator.DONE) {
                if (inWord) {
                    // when starting in the middle of a word
                    return TextPos.ofLeading(index, len);
                }
            } else {
                if (inWord) {
                    return TextPos.ofLeading(index, next);
                }

                while (next != BreakIterator.DONE) {
                    offset = next;
                    next = br.next();
                    inWord = RichUtils.isLetterOrDigit(text, offset);
                    if (inWord) {
                        return TextPos.ofLeading(index, next);
                    }
                }
            }

            index++;
            offset = 0;
            skipEmpty = false;
        }
    }

    public void redo() {
        RichTextArea control = getControl();
        StyledTextModel m = control.getModel();
        if (m != null) {
            TextPos[] sel = m.redo(vflow);
            if (sel != null) {
                control.select(sel[0], sel[1]);
            }
        }
    }

    public void undo() {
        RichTextArea control = getControl();
        StyledTextModel m = control.getModel();
        if (m != null) {
            TextPos[] sel = m.undo(vflow);
            if (sel != null) {
                control.select(sel[0], sel[1]);
            }
        }
    }

    private Bidi getBidi() {
        String paragraph = getTextAtCaret();
        int flags = (getControl().getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT) ?
            Bidi.DIRECTION_RIGHT_TO_LEFT :
            Bidi.DIRECTION_LEFT_TO_RIGHT;
        return new Bidi(paragraph, flags);
    }

    private String getTextAtCaret() {
        RichTextArea control = getControl();
        TextPos p = control.getCaretPosition();
        if (p != null) {
            String s = control.getPlainText(p.index());
            if (s != null) {
                return s;
            }
        }
        return "";
    }

    private boolean isMixed() {
        return getBidi().isMixed();
    }

    private boolean isRTLText() {
        Bidi bidi = getBidi();
        return (
            bidi.isRightToLeft() ||
            (isMixed() && getControl().getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT)
        );
    }

    public void deleteWordNextBeg() {
        deleteIgnoreSelection(this::nextWordBeg);
    }

    public void deleteWordNextEnd() {
        deleteIgnoreSelection(this::toWordEnd);
    }

    public void deleteWordPrevious() {
        deleteIgnoreSelection(this::toPreviousWord);
    }

    private void deleteIgnoreSelection(BiFunction<RichTextArea, TextPos, TextPos> getter) {
        if (canEdit()) {
            RichTextArea control = getControl();
            TextPos caret = control.getCaretPosition();
            if (caret != null) {
                TextPos p = getter.apply(control, caret);
                if (p != null) {
                    control.clearSelection();
                    clearPhantomX();
                    p = control.replaceText(caret, p, "", true);
                    control.select(p);
                }
            }
        }
    }
}
