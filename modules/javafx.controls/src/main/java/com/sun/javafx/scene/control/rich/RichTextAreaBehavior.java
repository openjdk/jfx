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

import static javafx.scene.control.rich.RichTextArea.BACKSPACE;
import static javafx.scene.control.rich.RichTextArea.COPY;
import static javafx.scene.control.rich.RichTextArea.CUT;
import static javafx.scene.control.rich.RichTextArea.DELETE;
import static javafx.scene.control.rich.RichTextArea.DELETE_PARAGRAPH;
import static javafx.scene.control.rich.RichTextArea.INSERT_LINE_BREAK;
import static javafx.scene.control.rich.RichTextArea.INSERT_TAB;
import static javafx.scene.control.rich.RichTextArea.MOVE_DOCUMENT_END;
import static javafx.scene.control.rich.RichTextArea.MOVE_DOCUMENT_START;
import static javafx.scene.control.rich.RichTextArea.MOVE_DOWN;
import static javafx.scene.control.rich.RichTextArea.MOVE_LEFT;
import static javafx.scene.control.rich.RichTextArea.MOVE_LINE_END;
import static javafx.scene.control.rich.RichTextArea.MOVE_LINE_START;
import static javafx.scene.control.rich.RichTextArea.MOVE_RIGHT;
import static javafx.scene.control.rich.RichTextArea.MOVE_UP;
import static javafx.scene.control.rich.RichTextArea.MOVE_WORD_NEXT;
import static javafx.scene.control.rich.RichTextArea.MOVE_WORD_NEXT_END;
import static javafx.scene.control.rich.RichTextArea.MOVE_WORD_PREVIOUS;
import static javafx.scene.control.rich.RichTextArea.PAGE_DOWN;
import static javafx.scene.control.rich.RichTextArea.PAGE_UP;
import static javafx.scene.control.rich.RichTextArea.PASTE;
import static javafx.scene.control.rich.RichTextArea.PASTE_PLAIN_TEXT;
import static javafx.scene.control.rich.RichTextArea.REDO;
import static javafx.scene.control.rich.RichTextArea.SELECT_ALL;
import static javafx.scene.control.rich.RichTextArea.SELECT_DOCUMENT_END;
import static javafx.scene.control.rich.RichTextArea.SELECT_DOCUMENT_START;
import static javafx.scene.control.rich.RichTextArea.SELECT_DOWN;
import static javafx.scene.control.rich.RichTextArea.SELECT_LEFT;
import static javafx.scene.control.rich.RichTextArea.SELECT_PAGE_DOWN;
import static javafx.scene.control.rich.RichTextArea.SELECT_PAGE_UP;
import static javafx.scene.control.rich.RichTextArea.SELECT_PARAGRAPH;
import static javafx.scene.control.rich.RichTextArea.SELECT_RIGHT;
import static javafx.scene.control.rich.RichTextArea.SELECT_UP;
import static javafx.scene.control.rich.RichTextArea.SELECT_WORD;
import static javafx.scene.control.rich.RichTextArea.SELECT_WORD_NEXT;
import static javafx.scene.control.rich.RichTextArea.SELECT_WORD_NEXT_END;
import static javafx.scene.control.rich.RichTextArea.SELECT_WORD_PREVIOUS;
import static javafx.scene.control.rich.RichTextArea.UNDO;
import java.text.BreakIterator;
import java.util.HashMap;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.rich.CaretInfo;
import javafx.scene.control.rich.RichTextArea;
import javafx.scene.control.rich.TextCell;
import javafx.scene.control.rich.TextPos;
import javafx.scene.control.rich.input.KeyBinding;
import javafx.scene.control.rich.input.KeyMap;
import javafx.scene.control.rich.model.DataFormatHandler;
import javafx.scene.control.rich.model.StyledInput;
import javafx.scene.control.rich.model.StyledTextModel;
import javafx.scene.control.rich.skin.RichTextAreaSkin;
import javafx.scene.control.util.Util;
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
import com.sun.javafx.scene.control.ListenerHelper;

/**
 * RichTextArea Behavior.
 *
 * BehaviorBase and InputMap are not public, so had to invent my own.
 * 
 * Behavior:
 * - maps key bindings to action tags
 * - maps action tags to functions (default implementation)
 * - function: provides default implementation
 * Control:
 * - allows for re-mapping of an action tag to an alternative implementation
 * - executes code associated with an action tag (default or alternative)
 */
public class RichTextAreaBehavior {
    private final RichTextArea control;
    private VFlow vflow;
    private final StyledTextModel.ChangeListener modelChangeListener;
    private final Timeline autoScrollTimer;
    private boolean autoScrollUp;
    private boolean fastAutoScroll;
    private double phantomX = -1.0;
    private final Duration autoScrollPeriod;
    private ContextMenu contextMenu = new ContextMenu();

    public RichTextAreaBehavior(RichTextArea control) {
        this.control = control;

        autoScrollPeriod = Duration.millis(Params.AUTO_SCROLL_PERIOD);

        modelChangeListener = new StyledTextModel.ChangeListener() {
            @Override
            public void eventTextUpdated(TextPos start, TextPos end, int top, int ins, int btm) {
                handleTextUpdated(start, end, top, ins, btm);
                // TODO do we need to reflow if useContentXX is on?
            }

            @Override
            public void eventStyleUpdated(TextPos start, TextPos end) {
                handleStyleUpdated(start, end);
                // TODO do we need to reflow if useContentXX is on?
            }
        };

        autoScrollTimer = new Timeline(new KeyFrame(autoScrollPeriod, (ev) -> {
            autoScroll();
        }));
        autoScrollTimer.setCycleCount(Timeline.INDEFINITE);
    }

    public void install(RichTextAreaSkin skin, ListenerHelper lh) {
        vflow = RichTextAreaSkinHelper.getVFlow(skin);

        KeyMap m = control.getKeyMap();
        // commands
        m.func(BACKSPACE, this::backspace);
        m.func(COPY, this::copy);
        m.func(CUT, this::cut);
        m.func(DELETE, this::delete);
        m.func(DELETE_PARAGRAPH, this::deleteParagraph);
        m.func(INSERT_LINE_BREAK, this::insertLineBreak);
        m.func(INSERT_TAB, this::insertTab);
        m.func(MOVE_DOCUMENT_END, this::moveDocumentEnd);
        m.func(MOVE_DOCUMENT_START, this::moveDocumentStart);
        m.func(MOVE_DOWN, this::moveDown);
        m.func(MOVE_LEFT, this::moveLeft);
        m.func(MOVE_LINE_END, this::moveLineEnd);
        m.func(MOVE_LINE_START, this::moveLineStart);
        m.func(MOVE_RIGHT, this::moveRight);
        m.func(MOVE_UP, this::moveUp);
        m.func(MOVE_WORD_NEXT, this::nextWord);
        m.func(MOVE_WORD_NEXT_END, this::endOfNextWord);
        m.func(MOVE_WORD_PREVIOUS, this::previousWord);
        m.func(PAGE_DOWN, this::pageDown);
        m.func(PAGE_UP, this::pageUp);
        m.func(PASTE, this::paste);
        m.func(PASTE_PLAIN_TEXT, this::pastePlainText);
        m.func(REDO, this::redo);
        m.func(SELECT_ALL, this::selectAll);
        m.func(SELECT_DOCUMENT_END, this::selectDocumentEnd);
        m.func(SELECT_DOCUMENT_START, this::selectDocumentStart);
        m.func(SELECT_DOWN, this::selectDown);
        m.func(SELECT_LEFT, this::selectLeft);
        m.func(SELECT_PAGE_DOWN, this::selectPageDown);
        m.func(SELECT_PAGE_UP, this::selectPageUp);
        m.func(SELECT_PARAGRAPH, this::selectParagraph);
        m.func(SELECT_RIGHT, this::selectRight);
        m.func(SELECT_UP, this::selectUp);
        m.func(SELECT_WORD, this::selectWord);
        m.func(SELECT_WORD_NEXT, this::selectNextWord);
        m.func(SELECT_WORD_NEXT_END, this::selectEndOfNextWord);
        m.func(SELECT_WORD_PREVIOUS, this::selectPreviousWord);
        m.func(UNDO, this::undo);
        // keys
        m.key(skin, KeyCode.BACK_SPACE, BACKSPACE);
        m.key(skin, KeyBinding.shortcut(KeyCode.C), COPY);        
        m.key(skin, KeyBinding.shortcut(KeyCode.X), CUT);
        m.key(skin, KeyCode.DELETE, DELETE);
        m.key(skin, KeyBinding.shortcut(KeyCode.D), DELETE_PARAGRAPH);
        m.key(skin, KeyCode.ENTER, INSERT_LINE_BREAK);
        m.key(skin, KeyCode.TAB, INSERT_TAB);
        m.key(skin, KeyCode.LEFT, MOVE_LEFT);
        m.key(skin, KeyCode.RIGHT, MOVE_RIGHT);
        m.key(skin, KeyCode.UP, MOVE_UP);
        m.key(skin, KeyCode.DOWN, MOVE_DOWN);
        m.key(skin, KeyCode.HOME, MOVE_LINE_START);
        m.key(skin, KeyCode.END, MOVE_LINE_END);
        m.key(skin, KeyBinding.with(KeyCode.HOME).ctrl().notForMac().build(), MOVE_DOCUMENT_START);
        m.key(skin, KeyBinding.with(KeyCode.UP).shortcut().forMac().build(), MOVE_DOCUMENT_START);
        m.key(skin, KeyBinding.with(KeyCode.END).ctrl().notForMac().build(), MOVE_DOCUMENT_END);
        m.key(skin, KeyBinding.with(KeyCode.DOWN).shortcut().forMac().build(), MOVE_DOCUMENT_END);
        m.key(skin, KeyBinding.with(KeyCode.RIGHT).ctrl().notForMac().build(), MOVE_WORD_NEXT);
        m.key(skin, KeyBinding.with(KeyCode.RIGHT).option().forMac().build(), MOVE_WORD_NEXT);
        m.key(skin, KeyBinding.with(KeyCode.LEFT).ctrl().notForMac().build(), MOVE_WORD_PREVIOUS);
        m.key(skin, KeyBinding.with(KeyCode.LEFT).option().forMac().build(), MOVE_WORD_PREVIOUS);
        m.key(skin, KeyCode.PAGE_DOWN, PAGE_DOWN);
        m.key(skin, KeyCode.PAGE_UP, PAGE_UP);
        m.key(skin, KeyBinding.shortcut(KeyCode.V), PASTE);
        m.key(skin, KeyBinding.with(KeyCode.Z).shift().command().forMac().build(), REDO);
        m.key(skin, KeyBinding.with(KeyCode.Y).ctrl().notForMac().build(), REDO);
        m.key(skin, KeyBinding.shortcut(KeyCode.A), SELECT_ALL);
        m.key(skin, KeyBinding.shift(KeyCode.LEFT), SELECT_LEFT);
        m.key(skin, KeyBinding.shift(KeyCode.RIGHT), SELECT_RIGHT);
        m.key(skin, KeyBinding.shift(KeyCode.UP), SELECT_UP);
        m.key(skin, KeyBinding.shift(KeyCode.DOWN), SELECT_DOWN);
        m.key(skin, KeyBinding.shift(KeyCode.PAGE_UP), SELECT_PAGE_UP);
        m.key(skin, KeyBinding.shift(KeyCode.PAGE_DOWN), SELECT_PAGE_DOWN);
        m.key(skin, KeyBinding.with(KeyCode.HOME).ctrl().shift().notForMac().build(), SELECT_DOCUMENT_START);
        m.key(skin, KeyBinding.with(KeyCode.UP).shift().shortcut().forMac().build(), SELECT_DOCUMENT_START);
        m.key(skin, KeyBinding.with(KeyCode.END).ctrl().shift().notForMac().build(), SELECT_DOCUMENT_END);
        m.key(skin, KeyBinding.with(KeyCode.DOWN).shift().shortcut().forMac().build(), SELECT_DOCUMENT_END);
        m.key(skin, KeyBinding.shortcut(KeyCode.Z), UNDO);

        Pane c = vflow.getContentPane();
        c.addEventFilter(MouseEvent.MOUSE_CLICKED, this::handleMouseClicked);
        c.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        c.addEventFilter(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
        c.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        c.addEventFilter(ScrollEvent.ANY, this::handleScrollEvent);

        lh.addEventHandler(control, KeyEvent.ANY, this::handleKeyEvent);
        
        // TODO there is no way to override the default behavior, such as clear selection or select word under cursor,
        // except for adding event filter
        lh.addEventHandler(control, ContextMenuEvent.CONTEXT_MENU_REQUESTED, this::contextMenuRequested);

        lh.addChangeListener(control.modelProperty(), true, this::handleModel);
    }

    public void dispose(RichTextAreaSkin skin) {
        control.getKeyMap().unregister(skin);
    }
    
    protected boolean isRTL() {
        return (control.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT);
    }

    protected String getPlainText(int modelIndex) {
        StyledTextModel m = control.getModel();
        return (m == null) ? null : m.getPlainText(modelIndex);
    }

    protected void handleModel(Object src, StyledTextModel old, StyledTextModel m) {
        if (old != null) {
            old.removeChangeListener(modelChangeListener);
        }

        if (m != null) {
            m.addChangeListener(modelChangeListener);
        }
    }

    public void handleKeyEvent(KeyEvent ev) {
        //System.out.println("handleKeyEvent: " + ev); // FIX
        if (ev == null || ev.isConsumed()) {
            return;
        }

        KeyBinding k = KeyBinding.from(ev);
        if (k != null) {
            Runnable r = control.getKeyMap().getFunction(k);
            if (r != null) {
                vflow.setSuppressBlink(true);
                r.run();
                vflow.setSuppressBlink(false);
                ev.consume();
            }
        }

        // TODO something about consuming all key presses (yes) and key releases (not really)
        // in TextInputControlBehavior:194
        
        String character = getValidKeyTyped(ev);
        if (character != null) {
            vflow.setSuppressBlink(true);
            handleKeyTyped(character);
            vflow.setSuppressBlink(false);
            ev.consume();
        }
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
        if (control.isEditable()) {
            StyledTextModel m = control.getModel();
            if (m != null) {
                return m.isEditable();
            }
        }
        return false;
    }

    protected void handleKeyTyped(String typed) {
        if (!canEdit()) {
            return;
        }

        StyledTextModel m = control.getModel();
        TextPos start = control.getCaretPosition();
        if (start != null) {
            TextPos end = control.getAnchorPosition();
            if (end == null) {
                end = start;
            } else if (start.compareTo(end) > 0) {
                TextPos p = start;
                start = end;
                end = p;
            }

            m.replace(vflow, start, end, typed);

            int off = start.offset() + typed.length();
            TextPos p = new TextPos(start.index(), off);
            control.moveCaret(p, false);

            clearPhantomX();
        }
    }

    public void insertTab() {
        handleKeyTyped("\t");
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
        if (start.compareTo(end) > 0) {
            TextPos p = start;
            start = end;
            end = p;
        }

        TextPos pos = m.replace(vflow, start, end, StyledInput.of("\n"));
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
    
    public void moveLineStart() {
        TextPos p = control.getCaretPosition();
        if (p != null) {
            TextPos p2 = new TextPos(p.index(), 0);
            control.moveCaret(p2, false);
            clearPhantomX();
        }
    }

    public void moveLineEnd() {
        TextPos p = control.getCaretPosition();
        if (p != null) {
            int ix = p.index();
            TextPos end = control.getEndOfParagraph(ix);
            control.moveCaret(end, false);
            clearPhantomX();
        }
    }

    public void moveUp() {
        moveLine(-1.0 - control.getLineSpacing(), false);
    }

    public void moveDown() {
        moveLine(1.0 + control.getLineSpacing(), false);
    }
    
    /**
     * Moves the caret to before the first character of the text, also clearing the selection.
     */
    public void moveDocumentStart() {
        control.select(TextPos.ZERO);
    }

    /**
     * Moves the caret to after the last character of the text, also clearing the selection.
     */
    public void moveDocumentEnd() {
        TextPos pos = control.getEndTextPos();
        control.select(pos);
    }

    protected void moveLine(double deltaPixels, boolean extendSelection) {
        CaretInfo c = vflow.getCaretInfo();
        double sp = control.getLineSpacing();
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
            System.err.println("offset=" + off + " text=[" + text + "]");
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
    
    public void selectDown() {
        moveLine(1.0, true);
    }
    
    public void selectUp() {
        moveLine(-1.0, true);
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

    protected void handleTextUpdated(TextPos start, TextPos end, int addedTop, int linesAdded, int addedBottom) {
        vflow.handleTextUpdated(start, end, addedTop, linesAdded, addedBottom);
    }
    
    protected void handleStyleUpdated(TextPos start, TextPos end) {
        vflow.handleStyleUpdated(start, end);
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
                control.getModel().replace(vflow, start, end, StyledInput.EMPTY);
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
                control.getModel().replace(vflow, start, end, StyledInput.EMPTY);
                control.moveCaret(start, false);
                clearPhantomX();
            }
        }
    }

    public void deleteParagraph() {
        if (!canEdit()) {
            return;
        }

        // TODO
        System.out.println("deleteParagraph");
    }

    protected void deleteSelection() {
        TextPos start = control.getCaretPosition();
        TextPos end = control.getAnchorPosition();
        if (start.compareTo(end) > 0) {
            TextPos p = start;
            start = end;
            end = p;
        }
        control.getModel().replace(vflow, start, end, StyledInput.EMPTY);
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
                TextPos start = control.getCaretPosition();
                if (start == null) {
                    return;
                }
                TextPos end = control.getAnchorPosition();
                if (end == null) {
                    return;
                }
                if (start.compareTo(end) > 0) {
                    TextPos p = start;
                    start = end;
                    end = p;
                }

                if (control.hasNonEmptySelection()) {
                    deleteSelection();
                }

                StyledTextModel m = control.getModel();
                DataFormatHandler h = m.getDataFormatHandler(f, false);
                Object src = Clipboard.getSystemClipboard().getContent(f);
                StyledInput in = h.createStyledInput(src);
                TextPos p = m.replace(vflow, start, end, in);
                control.moveCaret(p, false);
            }
        }
    }
    
    public void paste(DataFormat f) {
        if (canEdit()) {
            Clipboard c = Clipboard.getSystemClipboard();
            if (c.hasContent(f)) {
                TextPos start = control.getCaretPosition();
                if (start == null) {
                    return;
                }
                TextPos end = control.getAnchorPosition();
                if (end == null) {
                    return;
                }
                if (start.compareTo(end) > 0) {
                    TextPos p = start;
                    start = end;
                    end = p;
                }

                if (control.hasNonEmptySelection()) {
                    // FIX not needed?
                    deleteSelection();
                }

                StyledTextModel m = control.getModel();
                DataFormatHandler h = m.getDataFormatHandler(f, false);
                Object src = c.getContent(f);
                StyledInput in = h.createStyledInput(src);

                TextPos p = m.replace(vflow, start, end, in);
                control.moveCaret(p, false);
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
                TextPos start = control.getAnchorPosition();
                TextPos end = control.getCaretPosition();
                if (start.compareTo(end) > 0) {
                    TextPos p = start;
                    start = end;
                    end = p;
                }

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
                    // TODO log exception
                    e.printStackTrace();
                    Util.provideErrorFeedback(control);
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
                    TextPos start = control.getAnchorPosition();
                    TextPos end = control.getCaretPosition();
                    if (start.compareTo(end) > 0) {
                        TextPos p = start;
                        start = end;
                        end = p;
                    }
                    Object v = h.copy(m, vflow, start, end);
                    ClipboardContent c = new ClipboardContent();
                    c.put(f, v);
                    Clipboard.getSystemClipboard().setContent(c);
                }
            }
        } catch(Exception | OutOfMemoryError e) {
            // TODO log exception?
            e.printStackTrace();
            Util.provideErrorFeedback(control);
        }
    }

    /**
     * Moves the caret to the beginning of previous word. This function
     * also has the effect of clearing the selection.
     */
    public void previousWord() {
        previousWord(false);
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
     * Moves the caret to the end of the next word. This does not cause
     * the selection to be cleared.
     */
    public void selectEndOfNextWord() {
        endOfNextWord(true);
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
        // TODO
        System.out.println("redo");
    }
    
    public void undo() {
        // TODO
        System.out.println("undo");
    }
}