/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.rich;

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
import javafx.incubator.scene.control.behavior.BehaviorBase;
import javafx.incubator.scene.control.behavior.InputMap;
import javafx.incubator.scene.control.behavior.KeyBinding;
import javafx.incubator.scene.control.rich.CaretInfo;
import javafx.incubator.scene.control.rich.RichTextArea;
import javafx.incubator.scene.control.rich.TextPos;
import javafx.incubator.scene.control.rich.model.DataFormatHandler;
import javafx.incubator.scene.control.rich.model.StyledInput;
import javafx.incubator.scene.control.rich.model.StyledTextModel;
import javafx.incubator.scene.control.util.Util;
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
    public void install() {
        vflow = RichTextAreaSkinHelper.getVFlow(control);

        // avoid blinking the caret when handling keys
        setOnKeyEventEnter(() -> vflow.setSuppressBlink(true));
        setOnKeyEventExit(() -> vflow.setSuppressBlink(false));

        // functions
        registerFunction(RichTextArea.BACKSPACE, this::backspace);
        registerFunction(RichTextArea.COPY, this::copy);
        registerFunction(RichTextArea.CUT, this::cut);
        registerFunction(RichTextArea.DELETE, this::delete);
        registerFunction(RichTextArea.DELETE_PARAGRAPH, this::deleteParagraph);
        registerFunction(RichTextArea.INSERT_LINE_BREAK, this::insertLineBreak);
        registerFunction(RichTextArea.INSERT_TAB, this::insertTab);
        registerFunction(RichTextArea.MOVE_DOCUMENT_END, this::moveDocumentEnd);
        registerFunction(RichTextArea.MOVE_DOCUMENT_START, this::moveDocumentStart);
        registerFunction(RichTextArea.MOVE_DOWN, this::moveDown);
        registerFunction(RichTextArea.MOVE_LEFT, this::moveLeft);
        registerFunction(RichTextArea.MOVE_PARAGRAPH_END, this::moveParagraphEnd);
        registerFunction(RichTextArea.MOVE_PARAGRAPH_START, this::moveParagraphStart);
        registerFunction(RichTextArea.MOVE_RIGHT, this::moveRight);
        registerFunction(RichTextArea.MOVE_UP, this::moveUp);
        registerFunction(RichTextArea.MOVE_WORD_NEXT, this::nextWord);
        registerFunction(RichTextArea.MOVE_WORD_NEXT_END, this::endOfNextWord);
        registerFunction(RichTextArea.MOVE_WORD_LEFT, this::leftWord);
        registerFunction(RichTextArea.MOVE_WORD_PREVIOUS, this::previousWord);
        registerFunction(RichTextArea.MOVE_WORD_RIGHT, this::rightWord);
        registerFunction(RichTextArea.PAGE_DOWN, this::pageDown);
        registerFunction(RichTextArea.PAGE_UP, this::pageUp);
        registerFunction(RichTextArea.PASTE, this::paste);
        registerFunction(RichTextArea.PASTE_PLAIN_TEXT, this::pastePlainText);
        registerFunction(RichTextArea.REDO, this::redo);
        registerFunction(RichTextArea.SELECT_ALL, this::selectAll);
        registerFunction(RichTextArea.SELECT_DOCUMENT_END, this::selectDocumentEnd);
        registerFunction(RichTextArea.SELECT_DOCUMENT_START, this::selectDocumentStart);
        registerFunction(RichTextArea.SELECT_DOWN, this::selectDown);
        registerFunction(RichTextArea.SELECT_LEFT, this::selectLeft);
        registerFunction(RichTextArea.SELECT_PAGE_DOWN, this::selectPageDown);
        registerFunction(RichTextArea.SELECT_PAGE_UP, this::selectPageUp);
        registerFunction(RichTextArea.SELECT_PARAGRAPH, this::selectParagraph);
        registerFunction(RichTextArea.SELECT_RIGHT, this::selectRight);
        registerFunction(RichTextArea.SELECT_UP, this::selectUp);
        registerFunction(RichTextArea.SELECT_WORD, this::selectWord);
        registerFunction(RichTextArea.SELECT_WORD_LEFT, this::selectLeftWord);
        registerFunction(RichTextArea.SELECT_WORD_NEXT, this::selectNextWord);
        registerFunction(RichTextArea.SELECT_WORD_NEXT_END, this::selectEndOfNextWord);
        registerFunction(RichTextArea.SELECT_WORD_PREVIOUS, this::selectPreviousWord);
        registerFunction(RichTextArea.SELECT_WORD_RIGHT, this::selectRightWord);
        registerFunction(RichTextArea.UNDO, this::undo);
        // keys
        registerKey(KeyCode.BACK_SPACE, RichTextArea.BACKSPACE);
        registerKey(KeyBinding.shortcut(KeyCode.C), RichTextArea.COPY);        
        registerKey(KeyBinding.shortcut(KeyCode.X), RichTextArea.CUT);
        registerKey(KeyCode.DELETE, RichTextArea.DELETE);
        registerKey(KeyBinding.shortcut(KeyCode.D), RichTextArea.DELETE_PARAGRAPH);
        registerKey(KeyCode.ENTER, RichTextArea.INSERT_LINE_BREAK);
        registerKey(KeyCode.TAB, RichTextArea.INSERT_TAB);
        registerKey(KeyCode.LEFT, RichTextArea.MOVE_LEFT);
        registerKey(KeyCode.RIGHT, RichTextArea.MOVE_RIGHT);
        registerKey(KeyCode.UP, RichTextArea.MOVE_UP);
        registerKey(KeyCode.DOWN, RichTextArea.MOVE_DOWN);
        registerKey(KeyCode.HOME, RichTextArea.MOVE_PARAGRAPH_START);
        registerKey(KeyCode.END, RichTextArea.MOVE_PARAGRAPH_END);
        registerKey(KeyCode.PAGE_DOWN, RichTextArea.PAGE_DOWN);
        registerKey(KeyCode.PAGE_UP, RichTextArea.PAGE_UP);
        registerKey(KeyBinding.shortcut(KeyCode.V), RichTextArea.PASTE);
        registerKey(KeyBinding.shortcut(KeyCode.A), RichTextArea.SELECT_ALL);
        registerKey(KeyBinding.shift(KeyCode.LEFT), RichTextArea.SELECT_LEFT);
        registerKey(KeyBinding.shift(KeyCode.RIGHT), RichTextArea.SELECT_RIGHT);
        registerKey(KeyBinding.shift(KeyCode.UP), RichTextArea.SELECT_UP);
        registerKey(KeyBinding.shift(KeyCode.DOWN), RichTextArea.SELECT_DOWN);
        registerKey(KeyBinding.shift(KeyCode.PAGE_UP), RichTextArea.SELECT_PAGE_UP);
        registerKey(KeyBinding.shift(KeyCode.PAGE_DOWN), RichTextArea.SELECT_PAGE_DOWN);
        registerKey(KeyBinding.shortcut(KeyCode.Z), RichTextArea.UNDO);

        if (isMac()) {
            registerKey(KeyBinding.with(KeyCode.UP).shortcut().build(), RichTextArea.MOVE_DOCUMENT_START);
            registerKey(KeyBinding.with(KeyCode.DOWN).shortcut().build(), RichTextArea.MOVE_DOCUMENT_END);
            registerKey(KeyBinding.with(KeyCode.LEFT).option().build(), RichTextArea.MOVE_WORD_LEFT);
            registerKey(KeyBinding.with(KeyCode.RIGHT).option().build(), RichTextArea.MOVE_WORD_RIGHT);
            registerKey(KeyBinding.with(KeyCode.Z).shift().command().build(), RichTextArea.REDO);
            registerKey(KeyBinding.with(KeyCode.LEFT).shift().option().build(), RichTextArea.SELECT_WORD_LEFT);
            registerKey(KeyBinding.with(KeyCode.RIGHT).shift().option().build(), RichTextArea.SELECT_WORD_RIGHT);
            registerKey(KeyBinding.with(KeyCode.UP).shift().shortcut().build(), RichTextArea.SELECT_DOCUMENT_START);
            registerKey(KeyBinding.with(KeyCode.DOWN).shift().shortcut().build(), RichTextArea.SELECT_DOCUMENT_END);
        } else {
            registerKey(KeyBinding.with(KeyCode.HOME).control().build(), RichTextArea.MOVE_DOCUMENT_START);
            registerKey(KeyBinding.with(KeyCode.END).control().build(), RichTextArea.MOVE_DOCUMENT_END);
            registerKey(KeyBinding.with(KeyCode.LEFT).control().build(), RichTextArea.MOVE_WORD_LEFT);
            registerKey(KeyBinding.with(KeyCode.RIGHT).control().build(), RichTextArea.MOVE_WORD_RIGHT);
            registerKey(KeyBinding.with(KeyCode.Y).control().build(), RichTextArea.REDO);
            registerKey(KeyBinding.with(KeyCode.LEFT).shift().control().build(), RichTextArea.SELECT_WORD_LEFT);
            registerKey(KeyBinding.with(KeyCode.RIGHT).shift().control().build(), RichTextArea.SELECT_WORD_RIGHT);
            registerKey(KeyBinding.with(KeyCode.HOME).control().shift().build(), RichTextArea.SELECT_DOCUMENT_START);
            registerKey(KeyBinding.with(KeyCode.END).control().shift().build(), RichTextArea.SELECT_DOCUMENT_END);
        }

        Pane cp = vflow.getContentPane();
        cp.addEventFilter(MouseEvent.MOUSE_CLICKED, this::handleMouseClicked);
        cp.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        cp.addEventFilter(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
        cp.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        cp.addEventFilter(ScrollEvent.ANY, this::handleScrollEvent);

        addHandler(KeyEvent.KEY_TYPED, this::handleKeyTyped);
        addHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, this::contextMenuRequested);
    }
    
    protected boolean isRTL() {
        return (control.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT);
    }

    protected String getPlainText(int modelIndex) {
        StyledTextModel m = control.getModel();
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
                if (ev.isControlDown() || ev.isAltDown() || (Util.isMac() && ev.isMetaDown())) {
                    if (!((ev.isControlDown() || Util.isMac()) && ev.isAltDown())) {
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
        if (control.isEditable()) {
            StyledTextModel m = control.getModel();
            if (m != null) {
                return m.isEditable();
            }
        }
        return false;
    }

    public void insertTab() {
        handleTypedChar("\t");
    }

    public void insertLineBreak() {
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

        TextPos pos = m.replace(vflow, start, end, StyledInput.of("\n"), true);
        control.moveCaret(pos, false);
        clearPhantomX();
    }

    protected void handleMouseClicked(MouseEvent ev) {
        if (ev.getButton() == MouseButton.PRIMARY) {
            int clicks = ev.getClickCount();
            switch (clicks) {
            case 2:
                control.selectWord();
                break;
            case 3:
                control.selectParagraph();
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
        control.extendSelection(pos);
    }

    protected void handleScrollEvent(ScrollEvent ev) {
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
        return control.getTextPosition(x, y);
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

    public void pageDown() {
        moveLine(vflow.getViewHeight(), false);
    }

    public void pageUp() {
        moveLine(-vflow.getViewHeight(), false);
    }

    public void moveRight() {
        moveCharacter(true, false);
    }

    public void moveLeft() {
        moveCharacter(false, false);
    }
    
    public void moveParagraphStart() {
        TextPos p = control.getCaretPosition();
        if (p != null) {
            TextPos p2 = new TextPos(p.index(), 0);
            control.moveCaret(p2, false);
            clearPhantomX();
        }
    }

    public void moveParagraphEnd() {
        TextPos p = control.getCaretPosition();
        if (p != null) {
            int ix = p.index();
            TextPos end = control.getEndOfParagraph(ix);
            control.moveCaret(end, false);
            clearPhantomX();
        }
    }

    public void moveUp() {
        moveLine(-1.0, false);
    }

    public void moveDown() {
        CaretInfo c = vflow.getCaretInfo();
        if (c != null) {
            double sp = c.getMaxY() - c.getMinY() + c.getLineSpacing();
            moveLine(1.0 + sp, false);
        }
    }

    /**
     * Moves the caret to before the first character of the text, also clearing the selection.
     */
    public void moveDocumentStart() {
        control.setCaret(TextPos.ZERO);
    }

    /**
     * Moves the caret to after the last character of the text, also clearing the selection.
     */
    public void moveDocumentEnd() {
        TextPos pos = control.getEndTextPos();
        control.setCaret(pos);
    }

    protected void moveLine(double deltaPixels, boolean extendSelection) {
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

    protected void moveCharacter(boolean moveRight, boolean extendSelection) {
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
    
    public void selectLeft() {
        moveCharacter(false, true);
    }

    public void selectRight() {
        moveCharacter(true, true);
    }

    public void selectUp() {
        moveLine(-1.0, true);
    }

    public void selectDown() {
        CaretInfo c = vflow.getCaretInfo();
        if (c != null) {
            double sp = c.getMaxY() - c.getMinY() + c.getLineSpacing();
            moveLine(1.0 + sp, true);
        }
    }

    public void selectPageDown() {
        moveLine(vflow.getViewHeight(), true);
    }

    public void selectPageUp() {
        moveLine(-vflow.getViewHeight(), true);
    }
    
    public void selectAll() {
        TextPos end = control.getEndTextPos();
        control.select(TextPos.ZERO, end);
        clearPhantomX();
    }

    /** selects from the anchor position to the document start */
    public void selectDocumentStart() {
        control.extendSelection(TextPos.ZERO);
    }

    /** selects from the anchor position to the document end */
    public void selectDocumentEnd() {
        TextPos pos = control.getEndTextPos();
        control.extendSelection(pos);
    }
    
    public void selectWord() {
        TextPos caret = control.getCaretPosition();
        if(caret == null) {
            return;
        }

        int index = caret.index();
        String text = getPlainText(index);
        if(text == null) {
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
        } catch(Exception e) {
            // TODO need to use a logger!
            System.err.println("offset=" + off + " text=[" + text + "]");
            e.printStackTrace();
        }
    }

    public void selectParagraph() {
        TextPos p = control.getCaretPosition();
        if (p != null) {
            int ix = p.index();
            TextPos an = new TextPos(ix, 0);
            TextPos ca = control.getEndOfParagraph(ix);
            control.select(an, ca);
        }
    }

    public void backspace() {
        if (!canEdit()) {
            return;
        }

        if (control.hasNonEmptySelection()) {
            deleteSelection();
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

    public void delete() {
        if (!canEdit()) {
            return;
        }

        if (control.hasNonEmptySelection()) {
            deleteSelection();
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
        return SelInfo.get(control);
    }
    
    private TextPos clamp(TextPos p) {
        return control.getModel().clamp(p);
    }

    public void deleteParagraph() {
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

    protected void deleteSelection() {
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
            Screen currentScreen = Util.getScreenForPoint(screenX, 0);
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
        boolean sel = control.hasNonEmptySelection();
        boolean paste = (findFormatForPaste() != null);
        
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
        m.setDisable(!sel);

        items.add(m = new MenuItem("Copy"));
        m.setOnAction((ev) -> control.copy());
        m.setDisable(!sel);

        items.add(m = new MenuItem("Paste"));
        m.setOnAction((ev) -> control.paste());
        m.setDisable(!paste);

        items.add(new SeparatorMenuItem());

        items.add(m = new MenuItem("Select All"));
        m.setOnAction((ev) -> control.selectAll());
    }

    public void copy() {
        copy(false);
    }

    public void cut() {
        copy(true);
    }

    public void paste() {
        if (canEdit()) {
            DataFormat f = findFormatForPaste();
            if (f != null) {
                SelInfo sel = sel();
                if (sel == null) {
                    return;
                }

                TextPos start = sel.getMin();
                TextPos end = sel.getMax();

                StyledTextModel m = control.getModel();
                DataFormatHandler h = m.getDataFormatHandler(f, false);
                Object src = Clipboard.getSystemClipboard().getContent(f);
                try (StyledInput in = h.createStyledInput(src)) {
                    if (in != null) {
                        TextPos p = m.replace(vflow, start, end, in, true);
                        control.moveCaret(p, false);
                    }
                } catch (IOException e) {
                    Util.provideErrorFeedback(control, e);
                }
            }
        }
    }

    public void paste(DataFormat f) {
        if (canEdit()) {
            Clipboard c = Clipboard.getSystemClipboard();
            if (c.hasContent(f)) {
                SelInfo sel = sel();
                if (sel == null) {
                    return;
                }

                TextPos start = sel.getMin();
                TextPos end = sel.getMax();

                StyledTextModel m = control.getModel();
                DataFormatHandler h = m.getDataFormatHandler(f, false);
                Object src = c.getContent(f);
                try (StyledInput in = h.createStyledInput(src)) {
                    if (in != null) {
                        TextPos p = m.replace(vflow, start, end, in, true);
                        control.moveCaret(p, false);
                    }
                } catch (IOException e) {
                    Util.provideErrorFeedback(control, e);
                }
            }
        }
    }

    public void pastePlainText() {
        paste(DataFormat.PLAIN_TEXT);
    }

    /** 
     * returns a format that can be imported by a model, based on the clipboard content and model being editable.
     */
    protected DataFormat findFormatForPaste() {
        if (canEdit()) {
            StyledTextModel m = control.getModel();
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

    protected void copy(boolean cut) {
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
                        deleteSelection();
                    }
                } catch(Exception | OutOfMemoryError e) {
                    Util.provideErrorFeedback(control, e);
                }
            }
        }
    }

    public void copy(DataFormat f) {
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
            Util.provideErrorFeedback(control, e);
        }
    }

    /**
     * Moves the caret to the beginning of previous word. This function
     * also has the effect of clearing the selection.
     */
    public void previousWord() {
        previousWord(false);
    }
    
    /** moves the caret to the beginning of the previos word (LTR) or next word (RTL) */
    public void leftWord() {
        leftWord(false);
    }
    
    /** moves the caret to the beginning of the next word (LTR) or previous word (RTL) */
    public void rightWord() {
        rightWord(false);
    }

    /**
     * Moves the caret to the beginning of next word. This function
     * also has the effect of clearing the selection.
     */
    public void nextWord() {
        nextWord(false);
    }

    /**
     * Moves the caret to the end of the next word. This function
     * also has the effect of clearing the selection.
     */
    public void endOfNextWord() {
        endOfNextWord(false);
    }

    /**
     * Moves the caret to the beginning of previous word. This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of previous word.
     */
    public void selectPreviousWord() {
        previousWord(true);
    }

    /**
     * Moves the caret to the beginning of next word. This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of next word.
     */
    public void selectNextWord() {
        nextWord(true);
    }
    
    /**
     * Moves the caret to the beginning of previous word (LTR) or next word (LTR).
     * This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of previous word.
     */
    public void selectLeftWord() {
        previousWord(true);
    }

    /**
     * Moves the caret to the beginning of next word (LTR) or previous word (RTL).
     * This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of next word.
     */
    public void selectRightWord() {
        nextWord(true);
    }

    /**
     * Moves the caret to the end of the next word. This does not cause
     * the selection to be cleared.
     */
    public void selectEndOfNextWord() {
        endOfNextWord(true);
    }
    
    protected void leftWord(boolean extendSelection) {
        if (isRTLText()) {
            nextWord(extendSelection);
        } else {
            previousWord(extendSelection);
        }
    }
    
    protected void rightWord(boolean extendSelection) {
        if (isRTLText()) {
            previousWord(extendSelection);
        } else {
            nextWord(extendSelection);
        }
    }

    protected void previousWord(boolean extendSelection) {
        TextPos caret = control.getCaretPosition();
        if (caret != null) {
            clearPhantomX();

            TextPos p = previousWordFrom(caret);
            if (p != null) {
                control.moveCaret(p, extendSelection);
            }
        }
    }
    
    protected void nextWord(boolean extendSelection) {
        TextPos caret = control.getCaretPosition();
        if (caret != null) {
            clearPhantomX();

            // TODO
//            if (isMac() || isLinux()) {
//                textInputControl.endOfNextWord();
//            } else {
//                textInputControl.nextWord();
//            }
            TextPos p = nextWordFrom(caret);
            if (p != null) {
                control.moveCaret(p, extendSelection);
            }
        }
    }
    
    protected void endOfNextWord(boolean extendSelection) {
        TextPos caret = control.getCaretPosition();
        if (caret != null) {
            clearPhantomX();

            TextPos p = endOfNextWordFrom(caret);
            if (p != null) {
                control.moveCaret(p, extendSelection);
            }
        }
    }

    protected TextPos previousWordFrom(TextPos caret) {
        int index = caret.index();
        String text = getPlainText(index);
        if ((text == null) || (text.length() == 0)) {
            return null;
        }

        BreakIterator br = BreakIterator.getWordInstance();
        br.setText(text);

        int len = text.length();
        int offset = caret.offset();
        int off = br.preceding(Util.clamp(0, offset, len));

        // Skip the non-word region, then move/select to the beginning of the word.
        while (off != BreakIterator.DONE && !Character.isLetterOrDigit(text.charAt(Util.clamp(0, off, len - 1)))) {
            off = br.preceding(Util.clamp(0, off, len));
        }

        return new TextPos(index, off);
    }

    protected TextPos nextWordFrom(TextPos caret) {
        int index = caret.index();
        String text = getPlainText(index);
        if ((text == null) || (text.length() == 0)) {
            return null;
        }

        BreakIterator br = BreakIterator.getWordInstance();
        br.setText(text);

        int len = text.length();
        int offset = caret.offset();

        int last = br.following(Util.clamp(0, offset, len - 1));
        int current = br.next();

        // Skip whitespace characters to the beginning of next word, but
        // stop at newline. Then move the caret or select a range.
        while (current != BreakIterator.DONE) {
            for (int off = last; off <= current; off++) {
                char ch = text.charAt(Util.clamp(0, off, len - 1));
                // Avoid using Character.isSpaceChar() and Character.isWhitespace(),
                // because they include LINE_SEPARATOR, PARAGRAPH_SEPARATOR, etc.
                if (ch != ' ' && ch != '\t') {
                    return new TextPos(index, off);
                }
            }
            last = current;
            current = br.next();
        }

        return new TextPos(index, len);
    }

    protected TextPos endOfNextWordFrom(TextPos caret) {
        int index = caret.index();
        String text = getPlainText(index);
        if ((text == null) || (text.length() == 0)) {
            return null;
        }

        BreakIterator br = BreakIterator.getWordInstance();
        br.setText(text);

        int textLength = text.length();
        int offset = caret.offset();
        int last = br.following(Util.clamp(0, offset, textLength));
        int current = br.next();

        // skip the non-word region, then move/select to the end of the word.
        while (current != BreakIterator.DONE) {
            for (int p = last; p <= current; p++) {
                if (!Character.isLetterOrDigit(text.charAt(Util.clamp(0, p, textLength - 1)))) {
                    return new TextPos(index, p);
                }
            }
            last = current;
            current = br.next();
        }

        return new TextPos(index, textLength);
    }

    public void redo() {
        StyledTextModel m = control.getModel();
        if (m != null) {
            TextPos[] sel = m.redo(vflow);
            if (sel != null) {
                control.select(sel[0], sel[1]);
            }
        }
    }

    public void undo() {
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
        int flags = (control.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT) ?
            Bidi.DIRECTION_RIGHT_TO_LEFT :
            Bidi.DIRECTION_LEFT_TO_RIGHT;
        return new Bidi(paragraph, flags);
    }

    private String getTextAtCaret() {
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
        return
            (bidi.isRightToLeft() ||
            (isMixed() && control.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT));
    }

    // TODO will be a part of control
    @Override
    protected InputMap getInputMap() {
        return getControl().getInputMap();
    }
}
