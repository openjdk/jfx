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

package com.sun.jfx.incubator.scene.control.rich;

import java.io.IOException;
import java.text.Bidi;
import java.text.BreakIterator;
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
import com.sun.jfx.incubator.scene.control.rich.util.RichUtils;
import jfx.incubator.scene.control.input.BehaviorBase;
import jfx.incubator.scene.control.input.KeyBinding;
import jfx.incubator.scene.control.rich.CaretInfo;
import jfx.incubator.scene.control.rich.RichTextArea;
import jfx.incubator.scene.control.rich.TextPos;
import jfx.incubator.scene.control.rich.model.DataFormatHandler;
import jfx.incubator.scene.control.rich.model.StyledInput;
import jfx.incubator.scene.control.rich.model.StyledTextModel;

/**
 * RichTextArea Behavior.
 */
public class RichTextAreaBehavior extends BehaviorBase<RichTextArea> {
    private VFlow vflow;
    private final Timeline autoScrollTimer;
    private boolean autoScrollUp;
    private boolean fastAutoScroll;
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
    public void populateSkinInputMap() {
        vflow = RichTextAreaSkinHelper.getVFlow(getControl());

        // functions
        registerFunction(RichTextArea.Tags.BACKSPACE, this::backspace);
        registerFunction(RichTextArea.Tags.COPY, this::copy);
        registerFunction(RichTextArea.Tags.CUT, this::cut);
        registerFunction(RichTextArea.Tags.DELETE, this::delete);
        registerFunction(RichTextArea.Tags.DELETE_PARAGRAPH, this::deleteParagraph);
        registerFunction(RichTextArea.Tags.FOCUS_NEXT, this::traverseNext);
        registerFunction(RichTextArea.Tags.FOCUS_PREVIOUS, this::traversePrevious);
        registerFunction(RichTextArea.Tags.INSERT_LINE_BREAK, this::insertLineBreak);
        registerFunction(RichTextArea.Tags.INSERT_TAB, this::insertTab);
        registerFunction(RichTextArea.Tags.MOVE_DOWN, this::moveDown);
        registerFunction(RichTextArea.Tags.MOVE_LEFT, this::moveLeft);
        registerFunction(RichTextArea.Tags.MOVE_TO_DOCUMENT_END, this::moveDocumentEnd);
        registerFunction(RichTextArea.Tags.MOVE_TO_DOCUMENT_START, this::moveDocumentStart);
        registerFunction(RichTextArea.Tags.MOVE_TO_PARAGRAPH_END, this::moveParagraphEnd);
        registerFunction(RichTextArea.Tags.MOVE_TO_PARAGRAPH_START, this::moveParagraphStart);
        registerFunction(RichTextArea.Tags.MOVE_RIGHT, this::moveRight);
        registerFunction(RichTextArea.Tags.MOVE_UP, this::moveUp);
        registerFunction(RichTextArea.Tags.MOVE_WORD_NEXT, this::nextWord);
        registerFunction(RichTextArea.Tags.MOVE_WORD_NEXT_END, this::endOfNextWord);
        registerFunction(RichTextArea.Tags.MOVE_WORD_LEFT, this::leftWord);
        registerFunction(RichTextArea.Tags.MOVE_WORD_PREVIOUS, this::previousWord);
        registerFunction(RichTextArea.Tags.MOVE_WORD_RIGHT, this::rightWord);
        registerFunction(RichTextArea.Tags.PAGE_DOWN, this::pageDown);
        registerFunction(RichTextArea.Tags.PAGE_UP, this::pageUp);
        registerFunction(RichTextArea.Tags.PASTE, this::paste);
        registerFunction(RichTextArea.Tags.PASTE_PLAIN_TEXT, this::pastePlainText);
        registerFunction(RichTextArea.Tags.REDO, this::redo);
        registerFunction(RichTextArea.Tags.SELECT_ALL, this::selectAll);
        registerFunction(RichTextArea.Tags.SELECT_DOWN, this::selectDown);
        registerFunction(RichTextArea.Tags.SELECT_LEFT, this::selectLeft);
        registerFunction(RichTextArea.Tags.SELECT_PAGE_DOWN, this::selectPageDown);
        registerFunction(RichTextArea.Tags.SELECT_PAGE_UP, this::selectPageUp);
        registerFunction(RichTextArea.Tags.SELECT_PARAGRAPH, this::selectParagraph);
        registerFunction(RichTextArea.Tags.SELECT_RIGHT, this::selectRight);
        registerFunction(RichTextArea.Tags.SELECT_TO_DOCUMENT_END, this::selectDocumentEnd);
        registerFunction(RichTextArea.Tags.SELECT_TO_DOCUMENT_START, this::selectDocumentStart);
        registerFunction(RichTextArea.Tags.SELECT_UP, this::selectUp);
        registerFunction(RichTextArea.Tags.SELECT_WORD, this::selectWord);
        registerFunction(RichTextArea.Tags.SELECT_WORD_LEFT, this::selectLeftWord);
        registerFunction(RichTextArea.Tags.SELECT_WORD_NEXT, this::selectNextWord);
        registerFunction(RichTextArea.Tags.SELECT_WORD_NEXT_END, this::selectEndOfNextWord);
        registerFunction(RichTextArea.Tags.SELECT_WORD_PREVIOUS, this::selectPreviousWord);
        registerFunction(RichTextArea.Tags.SELECT_WORD_RIGHT, this::selectRightWord);
        registerFunction(RichTextArea.Tags.UNDO, this::undo);
        // key mappings
        registerKey(KeyBinding.shortcut(KeyCode.A), RichTextArea.Tags.SELECT_ALL);
        registerKey(KeyCode.BACK_SPACE, RichTextArea.Tags.BACKSPACE);
        registerKey(KeyBinding.shortcut(KeyCode.C), RichTextArea.Tags.COPY);
        registerKey(KeyCode.COPY, RichTextArea.Tags.COPY);
        registerKey(KeyBinding.shortcut(KeyCode.D), RichTextArea.Tags.DELETE_PARAGRAPH);
        registerKey(KeyCode.DELETE, RichTextArea.Tags.DELETE);
        registerKey(KeyCode.DOWN, RichTextArea.Tags.MOVE_DOWN);
        registerKey(KeyBinding.shift(KeyCode.DOWN), RichTextArea.Tags.SELECT_DOWN);
        registerKey(KeyCode.END, RichTextArea.Tags.MOVE_TO_PARAGRAPH_END);
        registerKey(KeyCode.ENTER, RichTextArea.Tags.INSERT_LINE_BREAK);
        registerKey(KeyCode.HOME, RichTextArea.Tags.MOVE_TO_PARAGRAPH_START);
        registerKey(KeyCode.LEFT, RichTextArea.Tags.MOVE_LEFT);
        registerKey(KeyBinding.shift(KeyCode.LEFT), RichTextArea.Tags.SELECT_LEFT);
        registerKey(KeyCode.PAGE_DOWN, RichTextArea.Tags.PAGE_DOWN);
        registerKey(KeyBinding.shift(KeyCode.PAGE_DOWN), RichTextArea.Tags.SELECT_PAGE_DOWN);
        registerKey(KeyCode.PAGE_UP, RichTextArea.Tags.PAGE_UP);
        registerKey(KeyBinding.shift(KeyCode.PAGE_UP), RichTextArea.Tags.SELECT_PAGE_UP);
        registerKey(KeyCode.PASTE, RichTextArea.Tags.PASTE);
        registerKey(KeyCode.RIGHT, RichTextArea.Tags.MOVE_RIGHT);
        registerKey(KeyBinding.shift(KeyCode.RIGHT), RichTextArea.Tags.SELECT_RIGHT);
        registerKey(KeyCode.TAB, RichTextArea.Tags.INSERT_TAB);
        registerKey(KeyBinding.ctrl(KeyCode.TAB), RichTextArea.Tags.FOCUS_NEXT);
        registerKey(KeyBinding.builder(KeyCode.TAB).ctrl().option().shift().build(), RichTextArea.Tags.FOCUS_NEXT);
        registerKey(KeyBinding.ctrlShift(KeyCode.TAB), RichTextArea.Tags.FOCUS_PREVIOUS);
        registerKey(KeyBinding.shift(KeyCode.TAB), RichTextArea.Tags.FOCUS_PREVIOUS);
        registerKey(KeyCode.UP, RichTextArea.Tags.MOVE_UP);
        registerKey(KeyBinding.shift(KeyCode.UP), RichTextArea.Tags.SELECT_UP);
        registerKey(KeyBinding.shortcut(KeyCode.V), RichTextArea.Tags.PASTE);
        registerKey(KeyBinding.shiftShortcut(KeyCode.V), RichTextArea.Tags.PASTE_PLAIN_TEXT);
        registerKey(KeyBinding.shortcut(KeyCode.X), RichTextArea.Tags.CUT);
        registerKey(KeyCode.CUT, RichTextArea.Tags.CUT);
        registerKey(KeyBinding.shortcut(KeyCode.Z), RichTextArea.Tags.UNDO);

        if (isMac()) {
            registerKey(KeyBinding.shiftShortcut(KeyCode.DOWN), RichTextArea.Tags.SELECT_TO_DOCUMENT_END);
            registerKey(KeyBinding.shortcut(KeyCode.DOWN), RichTextArea.Tags.MOVE_TO_DOCUMENT_END);
            registerKey(KeyBinding.option(KeyCode.LEFT), RichTextArea.Tags.MOVE_WORD_LEFT);
            registerKey(KeyBinding.with(KeyCode.LEFT).shift().option().build(), RichTextArea.Tags.SELECT_WORD_LEFT);
            registerKey(KeyBinding.option(KeyCode.RIGHT), RichTextArea.Tags.MOVE_WORD_RIGHT);
            registerKey(KeyBinding.with(KeyCode.RIGHT).shift().option().build(), RichTextArea.Tags.SELECT_WORD_RIGHT);
            registerKey(KeyBinding.shiftShortcut(KeyCode.UP), RichTextArea.Tags.SELECT_TO_DOCUMENT_START);
            registerKey(KeyBinding.shortcut(KeyCode.UP), RichTextArea.Tags.MOVE_TO_DOCUMENT_START);
            registerKey(KeyBinding.with(KeyCode.Z).shift().command().build(), RichTextArea.Tags.REDO);
        } else {
            registerKey(KeyBinding.ctrl(KeyCode.HOME), RichTextArea.Tags.MOVE_TO_DOCUMENT_START);
            registerKey(KeyBinding.ctrlShift(KeyCode.HOME), RichTextArea.Tags.SELECT_TO_DOCUMENT_START);
            registerKey(KeyBinding.ctrl(KeyCode.END), RichTextArea.Tags.MOVE_TO_DOCUMENT_END);
            registerKey(KeyBinding.ctrlShift(KeyCode.END), RichTextArea.Tags.SELECT_TO_DOCUMENT_END);
            registerKey(KeyBinding.ctrl(KeyCode.LEFT), RichTextArea.Tags.MOVE_WORD_LEFT);
            registerKey(KeyBinding.ctrlShift(KeyCode.LEFT), RichTextArea.Tags.SELECT_WORD_LEFT);
            registerKey(KeyBinding.ctrl(KeyCode.RIGHT), RichTextArea.Tags.MOVE_WORD_RIGHT);
            registerKey(KeyBinding.ctrlShift(KeyCode.RIGHT), RichTextArea.Tags.SELECT_WORD_RIGHT);

            if (isWindows()) {
                registerKey(KeyBinding.ctrl(KeyCode.Y), RichTextArea.Tags.REDO);
            } else {
                registerKey(KeyBinding.ctrlShift(KeyCode.Z), RichTextArea.Tags.REDO);
            }
        }

        Pane cp = vflow.getContentPane();
        cp.addEventFilter(MouseEvent.MOUSE_CLICKED, this::handleMouseClicked);
        cp.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        cp.addEventFilter(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
        cp.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        cp.addEventFilter(ScrollEvent.ANY, this::handleScrollEvent);

        addHandler(KeyEvent.KEY_TYPED, true, this::handleKeyTyped);
        addHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, true, this::contextMenuRequested);
    }

    protected boolean isRTL() {
        return (getControl().getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT);
    }

    protected String getPlainText(int modelIndex) {
        StyledTextModel m = getControl().getModel();
        return (m == null) ? null : m.getPlainText(modelIndex);
    }

    protected void handleKeyTyped(KeyEvent ev) {
        if (ev == null || ev.isConsumed()) {
            return;
        }

        // TODO something about consuming all key presses (yes) and key releases (not really)
        // in TextInputControlBehavior:194

        String character = getValidKeyTyped(ev);
        if (character != null) {
            vflow.setSuppressBlink(true);
            boolean consume = handleTypedChar(character);
            if (consume) {
                ev.consume();
            }
            vflow.setSuppressBlink(false);
        }
    }

    private boolean handleTypedChar(String typed) {
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
                control.moveCaret(p, false);

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
                return m.isUserEditable();
            }
        }
        return false;
    }

    public void insertTab(RichTextArea control) {
        if (canEdit()) {
            handleTypedChar("\t");
        } else {
            traverseNext(control);
        }
    }

    public void insertLineBreak(RichTextArea control) {
        if (!canEdit()) {
            return;
        }

        StyledTextModel m = control.getModel();
        TextPos start = control.getCaretPosition();
        if (start == null) {
            return;
        }
        TextPos end = control.getAnchorPosition();
        if (end == null) {
            return;
        }

        // TODO use previous paragraph attributes?
        TextPos pos = m.replace(vflow, start, end, StyledInput.of("\n"), true);
        control.moveCaret(pos, false);
        clearPhantomX();
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
            // clearing existing (possibly multiple) selection
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
        vflow.scrollCaretToVisible();
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
        } else if (y > vflow.getViewHeight()) {
            // below visible area
            autoScroll(y - vflow.getViewHeight());
            return;
        } else {
            stopAutoScroll();
        }

        TextPos pos = getTextPosition(ev);
        getControl().extendSelection(pos);
    }

    protected void handleScrollEvent(ScrollEvent ev) {
        RichTextArea control = getControl();
        if (ev.isShiftDown()) {
            if (!control.isWrapText() && !control.isUseContentWidth()) {
                // horizontal scroll
                double f = Params.SCROLL_SHEEL_BLOCK_SIZE_HORIZONTAL;
                if (ev.getDeltaX() >= 0) {
                    f = -f;
                }
                vflow.hscroll(f);
                ev.consume();
            }
        } else {
            if (!control.isUseContentHeight()) {
                if (ev.isShortcutDown()) {
                    // page up / page down
                    if (ev.getDeltaY() >= 0) {
                        vflow.pageUp();
                    } else {
                        vflow.pageDown();
                    }
                    ev.consume();
                } else {
                    // block scroll
                    double f = Params.SCROLL_WHEEL_BLOCK_SIZE_VERTICAL;
                    if (ev.getDeltaY() >= 0) {
                        f = -f;
                    }
                    vflow.scroll(f);
                    ev.consume();
                }
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
        vflow.blockScroll(delta, true);

        double x = Math.max(0.0, phantomX + vflow.getOffsetX());
        double y = autoScrollUp ? 0.0 : vflow.getViewHeight();

        vflow.scrollToVisible(x, y);

        TextPos p = vflow.getTextPosLocal(x, y);
        control.extendSelection(p);
    }

    public void pageDown(RichTextArea control) {
        moveLine(control, vflow.getViewHeight(), false);
    }

    public void pageUp(RichTextArea control) {
        moveLine(control, -vflow.getViewHeight(), false);
    }

    public void moveRight(RichTextArea control) {
        moveCharacter(control, true, false);
    }

    public void moveLeft(RichTextArea control) {
        moveCharacter(control, false, false);
    }

    public void moveParagraphStart(RichTextArea control) {
        TextPos p = control.getCaretPosition();
        if (p != null) {
            TextPos p2 = new TextPos(p.index(), 0);
            control.moveCaret(p2, false);
            clearPhantomX();
        }
    }

    public void moveParagraphEnd(RichTextArea control) {
        TextPos p = control.getCaretPosition();
        if (p != null) {
            int ix = p.index();
            TextPos end = control.getEndOfParagraph(ix);
            control.moveCaret(end, false);
            clearPhantomX();
        }
    }

    public void moveUp(RichTextArea control) {
        moveLine(control, -1.0, false);
    }

    public void moveDown(RichTextArea control) {
        moveLine(control, 1.0, false);
    }

    /**
     * Moves the caret to before the first character of the text, also clearing the selection.
     */
    public void moveDocumentStart(RichTextArea control) {
        control.select(TextPos.ZERO);
    }

    /**
     * Moves the caret to after the last character of the text, also clearing the selection.
     */
    public void moveDocumentEnd(RichTextArea control) {
        TextPos pos = control.getEndTextPos();
        control.select(pos);
    }

    protected void moveLine(RichTextArea control, double deltaPixels, boolean extendSelection) {
        CaretInfo c = vflow.getCaretInfo();
        if (c == null) {
            return;
        }
        double sp = 0.0; //c.getLineSpacing();
        double x = (c.getMinX() + c.getMaxX()) / 2.0; // phantom x is unclear in the case of split caret
        double y = (deltaPixels < 0) ? c.getMinY() + deltaPixels - sp - 0.5 : c.getMaxY() + deltaPixels + sp;

        if (phantomX < 0) {
            phantomX = x;
        } else {
            x = phantomX;
        }

        TextPos p = vflow.getTextPosLocal(x + vflow.leftPadding(), y);
        // FIX may result in move to the same line in wrapped text with tab characters.
        // we need to check if same line and try something else then
        if (p != null) {
            control.moveCaret(p, extendSelection);
        }
    }

    protected void moveCharacter(RichTextArea control, boolean moveRight, boolean extendSelection) {
        // TODO bidi
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
                control.moveCaret(moveRight ? an : ca, extendSelection);
                return;
            } else if (d > 0) {
                control.moveCaret(moveRight ? ca : an, extendSelection);
                return;
            }
        }

        TextPos p = nextCharacterVisually(caret, moveRight);
        if (p != null) {
            control.moveCaret(p, extendSelection);
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
                    p = new TextPos(ix, 0);
                } else {
                    // end of last paragraph w/o newline
                    p = new TextPos(cell.getIndex(), cell.getTextLength());
                }
                return p;
            }
        } else {
            if (start.offset() == 0) {
                int ix = cell.getIndex() - 1;
                if (ix >= 0) {
                    // end of prev line
                    return control.getEndOfParagraph(ix);
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
            return new TextPos(start.index(), ix);
        } catch(Exception e) {
            // TODO need to use a logger!
            System.err.println("offset=" + off + " text=[" + text + "]"); // FIX
            e.printStackTrace();
            return null;
        }
    }

    public void clearPhantomX() {
        phantomX = -1.0;
    }

    public void selectLeft(RichTextArea control) {
        moveCharacter(control, false, true);
    }

    public void selectRight(RichTextArea control) {
        moveCharacter(control, true, true);
    }

    public void selectUp(RichTextArea control) {
        moveLine(control, -1.0, true);
    }

    public void selectDown(RichTextArea control) {
        moveLine(control, 1.0, true);
    }

    public void selectPageDown(RichTextArea control) {
        moveLine(control, vflow.getViewHeight(), true);
    }

    public void selectPageUp(RichTextArea control) {
        moveLine(control, -vflow.getViewHeight(), true);
    }

    public void selectAll(RichTextArea control) {
        TextPos end = control.getEndTextPos();
        control.select(TextPos.ZERO, end);
        clearPhantomX();
    }

    /** selects from the anchor position to the document start */
    public void selectDocumentStart(RichTextArea control) {
        control.extendSelection(TextPos.ZERO);
    }

    /** selects from the anchor position to the document end */
    public void selectDocumentEnd(RichTextArea control) {
        TextPos pos = control.getEndTextPos();
        control.extendSelection(pos);
    }

    public void selectWord(RichTextArea control) {
        TextPos caret = control.getCaretPosition();
        if (caret == null) {
            return;
        }

        int index = caret.index();
        String text = getPlainText(index);
        if (text == null) {
            return;
        }

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

            TextPos p0 = new TextPos(index, off0);
            TextPos p1 = new TextPos(index, off1);
            control.select(p0, p1);
        } catch (Exception e) {
            // TODO need to use a logger!
            System.err.println("offset=" + off + " text=[" + text + "]");
            e.printStackTrace();
        }
    }

    public void selectParagraph(RichTextArea control) {
        TextPos p = control.getCaretPosition();
        if (p != null) {
            int ix = p.index();
            TextPos an = new TextPos(ix, 0);
            TextPos ca = control.getEndOfParagraph(ix);
            control.select(an, ca);
        }
    }

    public void backspace(RichTextArea control) {
        if (!canEdit()) {
            return;
        }

        if (control.hasNonEmptySelection()) {
            deleteSelection(control);
        } else {
            TextPos end = control.getCaretPosition();
            if (end == null) {
                return;
            }

            TextPos start = nextCharacterVisually(end, false);
            if (start != null) {
                control.getModel().replace(vflow, start, end, StyledInput.EMPTY, true);
                control.moveCaret(start, false);
                clearPhantomX();
            }
        }
    }

    public void delete(RichTextArea control) {
        if (!canEdit()) {
            return;
        }

        if (control.hasNonEmptySelection()) {
            deleteSelection(control);
        } else {
            TextPos start = control.getCaretPosition();
            TextPos end = nextCharacterVisually(start, true);
            if (end != null) {
                control.getModel().replace(vflow, start, end, StyledInput.EMPTY, true);
                control.moveCaret(start, false);
                clearPhantomX();
            }
        }
    }

    private SelInfo sel() {
        return SelInfo.get(getControl());
    }

    private TextPos clamp(TextPos p) {
        return getControl().getModel().clamp(p);
    }

    public void deleteParagraph(RichTextArea control) {
        if (canEdit()) {
            SelInfo sel = sel();
            if (sel == null) {
                return;
            }

            int ix0 = sel.getMin().index();
            int ix1 = sel.getMax().index();

            TextPos p0 = new TextPos(ix0, 0);
            TextPos p1 = clamp(new TextPos(ix1 + 1, 0));
            control.getModel().replace(vflow, p0, p1, StyledInput.EMPTY, true);
            control.moveCaret(p0, false);
            clearPhantomX();
        }
    }

    protected void deleteSelection(RichTextArea control) {
        SelInfo sel = sel();
        if (sel == null) {
            return;
        }

        TextPos start = sel.getMin();
        TextPos end = sel.getMax();
        control.getModel().replace(vflow, start, end, StyledInput.EMPTY, true);
        control.moveCaret(start, false);
        clearPhantomX();
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
        boolean editable = control.canEdit();

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

    public void copy(RichTextArea control) {
        copy(control, false);
    }

    public void cut(RichTextArea control) {
        copy(control, true);
    }

    public void paste(RichTextArea control) {
        if (canEdit()) {
            DataFormat f = findFormatForPaste();
            if (f != null) {
                pasteLocal(control, f);
            }
        }
    }

    public void paste(DataFormat f) {
        if (canEdit()) {
            Clipboard c = Clipboard.getSystemClipboard();
            if (c.hasContent(f)) {
                pasteLocal(getControl(), f);
            }
        }
    }

    public void pastePlainText(RichTextArea control) {
        paste(DataFormat.PLAIN_TEXT);
    }

    /**
     * returns a format that can be imported by a model, based on the clipboard content and model being editable.
     */
    protected DataFormat findFormatForPaste() {
        if (canEdit()) {
            StyledTextModel m = getControl().getModel();
            DataFormat[] fs = m.getSupportedDataFormats(false);
            if (fs.length > 0) {
                for (DataFormat f : fs) {
                    if (Clipboard.getSystemClipboard().hasContent(f)) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    private void pasteLocal(RichTextArea control, DataFormat f) {
        SelInfo sel = sel();
        if (sel != null) {
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

            try (StyledInput in = h.createStyledInput(text)) {
                TextPos p = m.replace(vflow, start, end, in, true);
                control.moveCaret(p, false);
            } catch (IOException e) {
                RichUtils.provideErrorFeedback(control, e);
            }
        }
    }

    protected void copy(RichTextArea control, boolean cut) {
        if (control.hasNonEmptySelection()) {
            StyledTextModel m = control.getModel(); // non null at this point
            DataFormat[] fs = m.getSupportedDataFormats(true);
            if (fs.length > 0) {
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

                    if (canEdit() && cut) {
                        deleteSelection(control);
                    }
                } catch(Exception | OutOfMemoryError e) {
                    RichUtils.provideErrorFeedback(control, e);
                }
            }
        }
    }

    public void copy(DataFormat f) {
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
            RichUtils.provideErrorFeedback(control, e);
        }
    }

    /**
     * Moves the caret to the beginning of previous word. This function
     * also has the effect of clearing the selection.
     */
    public void previousWord(RichTextArea control) {
        previousWord(control, false);
    }

    /** moves the caret to the beginning of the previos word (LTR) or next word (RTL) */
    public void leftWord(RichTextArea control) {
        leftWord(control, false);
    }

    /** moves the caret to the beginning of the next word (LTR) or previous word (RTL) */
    public void rightWord(RichTextArea control) {
        rightWord(control, false);
    }

    /**
     * Moves the caret to the beginning of next word. This function
     * also has the effect of clearing the selection.
     */
    public void nextWord(RichTextArea control) {
        nextWord(control, false);
    }

    /**
     * Moves the caret to the end of the next word. This function
     * also has the effect of clearing the selection.
     */
    public void endOfNextWord(RichTextArea control) {
        endOfNextWord(control, false);
    }

    /**
     * Moves the caret to the beginning of previous word. This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of previous word.
     */
    public void selectPreviousWord(RichTextArea control) {
        previousWord(control, true);
    }

    /**
     * Moves the caret to the beginning of next word. This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of next word.
     */
    public void selectNextWord(RichTextArea control) {
        nextWord(control, true);
    }

    /**
     * Moves the caret to the beginning of previous word (LTR) or next word (LTR).
     * This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of previous word.
     */
    public void selectLeftWord(RichTextArea control) {
        previousWord(control, true);
    }

    /**
     * Moves the caret to the beginning of next word (LTR) or previous word (RTL).
     * This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of next word.
     */
    public void selectRightWord(RichTextArea control) {
        nextWord(control, true);
    }

    /**
     * Moves the caret to the end of the next word. This does not cause
     * the selection to be cleared.
     */
    public void selectEndOfNextWord(RichTextArea control) {
        endOfNextWord(control, true);
    }

    protected void leftWord(RichTextArea control, boolean extendSelection) {
        if (isRTLText()) {
            nextWord(control, extendSelection);
        } else {
            previousWord(control, extendSelection);
        }
    }

    protected void rightWord(RichTextArea control, boolean extendSelection) {
        if (isRTLText()) {
            previousWord(control, extendSelection);
        } else {
            nextWord(control, extendSelection);
        }
    }

    protected void previousWord(RichTextArea control, boolean extendSelection) {
        TextPos caret = control.getCaretPosition();
        if (caret != null) {
            clearPhantomX();

            TextPos p = previousWordFrom(caret);
            if (p != null) {
                control.moveCaret(p, extendSelection);
            }
        }
    }

    protected void nextWord(RichTextArea control, boolean extendSelection) {
        TextPos caret = control.getCaretPosition();
        if (caret != null) {
            clearPhantomX();

// TODO
//            if (isMac() || isLinux()) {
//                textInputControl.endOfNextWord();
//            } else {
//                textInputControl.nextWord();
//            }
            TextPos p = nextWordFrom(control, caret);
            if (p != null) {
                control.moveCaret(p, extendSelection);
            }
        }
    }

    protected void endOfNextWord(RichTextArea control, boolean extendSelection) {
        TextPos caret = control.getCaretPosition();
        if (caret != null) {
            clearPhantomX();

            TextPos p = endOfNextWordFrom(caret);
            if (p != null) {
                control.moveCaret(p, extendSelection);
            }
        }
    }

    protected TextPos previousWordFrom(TextPos pos) {
        int index = pos.index();
        int offset = pos.offset();
        BreakIterator br = null;

        for (;;) {
            if ((index == 0) && (offset <= 0)) {
                return TextPos.ZERO;
            }

            String text = getPlainText(index);
            if ((text == null) || (text.length() == 0)) {
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

            while (off != BreakIterator.DONE && !isLetterOrDigit(text, off, len)) {
                off = br.preceding(Utils.clamp(0, off, len));
            }

            if (off < 0) {
                index--;
                offset = Integer.MAX_VALUE;
                continue;
            }
            return new TextPos(index, off);
        }
    }

    protected TextPos nextWordFrom(RichTextArea control, TextPos pos) {
        int index = pos.index();
        int offset = pos.offset();
        boolean skipEmpty = true;

        for (;;) {
            TextPos end = control.getEndTextPos();
            // this could be a isSameOrAfter(index, off) method in TextPos
            if ((index == end.index()) && (offset >= end.offset())) {
                return end;
            } else if (index > end.index()) {
                return end;
            }

            String text = getPlainText(index);
            if ((text == null) || (text.length() == 0)) {
                if (skipEmpty) {
                    index++;
                }
                return new TextPos(index, 0);
            }

            BreakIterator br = BreakIterator.getWordInstance();
            br.setText(text);
    
            int len = text.length();
            int last = br.following(Utils.clamp(0, offset, len - 1));
            int current = br.next();
    
            // Skip whitespace characters to the beginning of next word, but
            // stop at newline. Then move the caret or select a range.
            while (current != BreakIterator.DONE) {
                for (int off = last; off <= current; off++) {
                    char ch = text.charAt(Utils.clamp(0, off, len - 1));
                    // Avoid using Character.isSpaceChar() and Character.isWhitespace(),
                    // because they include LINE_SEPARATOR, PARAGRAPH_SEPARATOR, etc.
                    if (ch != ' ' && ch != '\t') {
                        return new TextPos(index, off);
                    }
                }
                last = current;
                current = br.next();
            }
            
            index++;
            offset = 0;
            skipEmpty = false;
        }
    }

    // FIX fix to navigate over multiple paragraphs similarly to nextWordFrom()
    protected TextPos endOfNextWordFrom(TextPos caret) {
        int index = caret.index();
        String text = getPlainText(index);
        if ((text == null) || (text.length() == 0)) {
            return null;
        }

        BreakIterator br = BreakIterator.getWordInstance();
        br.setText(text);

        int len = text.length();
        int offset = caret.offset();
        int last = br.following(Utils.clamp(0, offset, len));
        int current = br.next();

        // skip the non-word region, then move/select to the end of the word.
        while (current != BreakIterator.DONE) {
            for (int off = last; off <= current; off++) {
                if (!isLetterOrDigit(text, off, len)) {
                    return new TextPos(index, off);
                }
            }
            last = current;
            current = br.next();
        }

        return new TextPos(index, len);
    }

    public void redo(RichTextArea control) {
        StyledTextModel m = control.getModel();
        if (m != null) {
            TextPos[] sel = m.redo(vflow);
            if (sel != null) {
                control.select(sel[0], sel[1]);
            }
        }
    }

    public void undo(RichTextArea control) {
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

    private boolean isLetterOrDigit(String text, int ix, int len) {
        if (ix < 0) {
            // should not happen
            return false;
        } else if (ix >= text.length()) {
            return false;
        }
        // ignore the case when 'c' is a high surrogate without the low surrogate
        int c = Character.codePointAt(text, ix);
        return Character.isLetterOrDigit(c);
    }
}
