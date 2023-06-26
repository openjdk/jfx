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

package javafx.scene.control.rich;

import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.InsetsConverter;
import javafx.css.converter.SizeConverter;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Control;
import javafx.scene.control.rich.input.FunctionTag;
import javafx.scene.control.rich.input.KeyMap;
import javafx.scene.control.rich.model.EditableRichTextModel;
import javafx.scene.control.rich.model.StyleAttrs;
import javafx.scene.control.rich.model.StyleInfo;
import javafx.scene.control.rich.model.StyledTextModel;
import javafx.scene.control.rich.skin.RichTextAreaSkin;
import javafx.scene.control.rich.util.Util;
import javafx.util.Duration;
import com.sun.javafx.scene.control.rich.Params;
import com.sun.javafx.scene.control.rich.RichTextAreaHelper;
import com.sun.javafx.scene.control.rich.RichTextAreaSkinHelper;
import com.sun.javafx.scene.control.rich.VFlow;

/**
 * Text input component that allows a user to enter multiple lines of rich text.
 */
public class RichTextArea extends Control {
    // function tags
    public static final FunctionTag BACKSPACE = new FunctionTag();
    public static final FunctionTag COPY = new FunctionTag();
    public static final FunctionTag CUT = new FunctionTag();
    public static final FunctionTag DELETE = new FunctionTag();
    public static final FunctionTag DELETE_PARAGRAPH = new FunctionTag();
    public static final FunctionTag INSERT_LINE_BREAK = new FunctionTag();
    public static final FunctionTag INSERT_TAB = new FunctionTag();
    public static final FunctionTag MOVE_DOCUMENT_END = new FunctionTag();
    public static final FunctionTag MOVE_DOCUMENT_START = new FunctionTag();
    public static final FunctionTag MOVE_DOWN = new FunctionTag();
    public static final FunctionTag MOVE_LEFT = new FunctionTag();
    public static final FunctionTag MOVE_LINE_END = new FunctionTag();
    public static final FunctionTag MOVE_LINE_START = new FunctionTag();
    public static final FunctionTag MOVE_RIGHT = new FunctionTag();
    public static final FunctionTag MOVE_UP = new FunctionTag();
    public static final FunctionTag MOVE_WORD_NEXT = new FunctionTag();
    public static final FunctionTag MOVE_WORD_NEXT_END = new FunctionTag();
    public static final FunctionTag MOVE_WORD_PREVIOUS = new FunctionTag();
    public static final FunctionTag PAGE_DOWN = new FunctionTag();
    public static final FunctionTag PAGE_UP = new FunctionTag();
    public static final FunctionTag PASTE = new FunctionTag();
    public static final FunctionTag PASTE_PLAIN_TEXT = new FunctionTag();
    public static final FunctionTag REDO = new FunctionTag();
    public static final FunctionTag SELECT_ALL = new FunctionTag();
    public static final FunctionTag SELECT_DOCUMENT_END = new FunctionTag();
    public static final FunctionTag SELECT_DOCUMENT_START = new FunctionTag();
    public static final FunctionTag SELECT_DOWN = new FunctionTag();
    public static final FunctionTag SELECT_LEFT = new FunctionTag();
    public static final FunctionTag SELECT_PAGE_DOWN = new FunctionTag();
    public static final FunctionTag SELECT_PAGE_UP = new FunctionTag();
    public static final FunctionTag SELECT_PARAGRAPH = new FunctionTag();
    public static final FunctionTag SELECT_RIGHT = new FunctionTag();
    public static final FunctionTag SELECT_UP = new FunctionTag();
    public static final FunctionTag SELECT_WORD = new FunctionTag();
    public static final FunctionTag SELECT_WORD_NEXT = new FunctionTag();
    public static final FunctionTag SELECT_WORD_NEXT_END = new FunctionTag();
    public static final FunctionTag SELECT_WORD_PREVIOUS = new FunctionTag();
    public static final FunctionTag UNDO = new FunctionTag();

    private static final double DEFAULT_LINE_SPACING = 0.0;
    private final ConfigurationParameters config;
    private final KeyMap inputMap = new KeyMap();
    private final ObjectProperty<StyledTextModel> model = new SimpleObjectProperty<>(this, "model");
    private final SimpleBooleanProperty displayCaretProperty = new SimpleBooleanProperty(this, "displayCaret", true);
    private SimpleBooleanProperty editableProperty;
    private final ReadOnlyObjectWrapper<Duration> caretBlinkPeriod;
    private final SelectionModel selectionModel = new SingleSelectionModel();
    private ReadOnlyIntegerWrapper tabSizeProperty;
    private ObjectProperty<SideDecorator> leftDecorator;
    private ObjectProperty<SideDecorator> rightDecorator;
    private ObjectProperty<Insets> contentPadding;
    private DoubleProperty lineSpacing;
    private BooleanProperty highlightCurrentLine;
    private BooleanProperty useContentWidth;
    private BooleanProperty useContentHeight;

    static {
        RichTextAreaHelper.setAccessor(new RichTextAreaHelper.Accessor() {
            @Override
            public TextCell createTextCell(RichTextArea a, int index) {
                return a.createTextCell(index);
            }
        });
    }

    /**
     * Creates an editable instance with default configuration parameters,
     * using an in-memory model {@link EditableRichTextModel}.
     */
    public RichTextArea() {
        this(new EditableRichTextModel());
    }

    /**
     * Creates an instance with default configuration parameters, using the specified model.
     * @param model styled text model
     */
    public RichTextArea(StyledTextModel model) {
        this(ConfigurationParameters.defaultConfig(), model);
    }

    /**
     * Creates an instance with the specified configuration parameters and model.
     * @param c configuration parameters
     * @param m styled text model
     */
    public RichTextArea(ConfigurationParameters c, StyledTextModel m) {
        this.config = c;
        
        caretBlinkPeriod = new ReadOnlyObjectWrapper<>(this, "caretBlinkPeriod", Duration.millis(Params.DEFAULT_CARET_BLINK_PERIOD));

        setFocusTraversable(true);
        getStyleClass().add("rich-text-area");
        setAccessibleRole(AccessibleRole.TEXT_AREA);

        if (m != null) {
            setModel(m);
        }
    }

    // TODO move to Control
    public final KeyMap getKeyMap() {
        return inputMap;
    }

    // TODO move to Control
    protected final void execute(FunctionTag a) {
        Runnable f = inputMap.getFunction(a);
        if (f != null) {
            f.run();
        }
    }

    @Override
    protected RichTextAreaSkin createDefaultSkin() {
        return new RichTextAreaSkin(this, config);
    }

    /**
     * Determines the {@link StyledTextModel} to use with this RichTextArea.
     * The model can be null.
     */
    public final ObjectProperty<StyledTextModel> modelProperty() {
        return model;
    }

    public final void setModel(StyledTextModel m) {
        modelProperty().set(m);
    }

    public final StyledTextModel getModel() {
        return model.get();
    }

    /**
     * If a run of text exceeds the width of the {@code RichTextArea},
     * then this variable indicates whether the text should wrap onto
     * another line.
     */
    // TODO perhaps all other properties also need to be styleable?
    private StyleableBooleanProperty wrapText = new StyleableBooleanProperty(false) {
        @Override
        public Object getBean() {
            return RichTextArea.this;
        }

        @Override
        public String getName() {
            return "wrapText";
        }

        @Override
        public CssMetaData<RichTextArea, Boolean> getCssMetaData() {
            return StyleableProperties.WRAP_TEXT;
        }
    };

    /**
     * Indicates whether text should be wrapped in this RichTextArea.
     * Setting this property to {@code true} has a side effect of hiding the horizontal scroll bar.
     */
    public final BooleanProperty wrapTextProperty() {
        return wrapText;
    }

    public final boolean isWrapText() {
        return wrapText.getValue();
    }

    public final void setWrapText(boolean value) {
        wrapText.setValue(value);
    }

    /**
     * This property controls whether caret will be displayed or not.
     */
    public final BooleanProperty displayCaretProperty() {
        return displayCaretProperty;
    }

    public final void setDisplayCaret(boolean on) {
        displayCaretProperty.set(on);
    }

    public final boolean isDisplayCaret() {
        return displayCaretProperty.get();
    }

    /**
     * Indicates whether this RichTextArea can be edited by the user.
     */
    public final BooleanProperty editableProperty() {
        if (editableProperty == null) {
            editableProperty = new SimpleBooleanProperty(this, "editable", true);
        }
        return editableProperty;
    }
    
    public final boolean isEditable() {
        if (editableProperty == null) {
            return true;
        }
        return editableProperty().get();
    }

    public final void setEditable(boolean on) {
        editableProperty().set(on);
    }

    /**
     * Indicates whether the current paragraph will be visually highlighted.
     */
    public final BooleanProperty highlightCurrentLineProperty() {
        if (highlightCurrentLine == null) {
            highlightCurrentLine = new SimpleBooleanProperty(this, "highlightCurrentLine", true);
        }
        return highlightCurrentLine;
    }

    public final boolean isHighlightCurrentLine() {
        if (highlightCurrentLine == null) {
            return true;
        }
        return highlightCurrentLine.get();
    }
    
    public final void setHighlightCurrentLine(boolean on) {
        highlightCurrentLineProperty().set(on);
    }

    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
        // TODO possibly large text - could we send just what is displayed?
//        case TEXT: {
//            String accText = getAccessibleText();
//            if (accText != null && !accText.isEmpty())
//                return accText;
//
//            String text = getText();
//            if (text == null || text.isEmpty()) {
//                text = getPromptText();
//            }
//            return text;
//        }
        case EDITABLE:
            return isEditable();
//        case SELECTION_START:
//            return getSelection().getStart();
//        case SELECTION_END:
//            return getSelection().getEnd();
//        case CARET_OFFSET:
//            return getCaretPosition();
//        case FONT:
//            return getFont();
        default:
            return super.queryAccessibleAttribute(attribute, parameters);
        }
    }
    
    private static class StyleableProperties {
        private static final CssMetaData<RichTextArea, Insets> CONTENT_PADDING =
            new CssMetaData<>("-fx-content-padding", InsetsConverter.getInstance()) {

            @Override
            public boolean isSettable(RichTextArea t) {
                return t.contentPadding == null || !t.contentPadding.isBound();
            }

            @Override
            public StyleableProperty<Insets> getStyleableProperty(RichTextArea t) {
                return (StyleableProperty<Insets>)t.contentPaddingProperty();
            }
        };

        private static final CssMetaData<RichTextArea, Number> LINE_SPACING =
            new CssMetaData<>("-fx-line-spacing", SizeConverter.getInstance(), 0) {

            @Override
            public boolean isSettable(RichTextArea t) {
                return t.lineSpacing == null || !t.lineSpacing.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(RichTextArea t) {
                return (StyleableProperty<Number>)t.lineSpacingProperty();
            }
        };

        private static final CssMetaData<RichTextArea,Boolean> WRAP_TEXT =
            new CssMetaData<>("-fx-wrap-text", StyleConverter.getBooleanConverter(), false) {

            @Override
            public boolean isSettable(RichTextArea t) {
                return !t.wrapText.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(RichTextArea t) {
                return (StyleableProperty<Boolean>)t.wrapTextProperty();
            }
            };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES = Util.initStyleables(
            Control.getClassCssMetaData(),
            CONTENT_PADDING,
            LINE_SPACING,
            WRAP_TEXT
        );
    }

    /**
     * Gets the {@code CssMetaData} associated with this class, which may include the
     * {@code CssMetaData} of its superclasses.
     * @return the {@code CssMetaData}
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }
    
    private VFlow vflow() {
        return RichTextAreaSkinHelper.getVFlow(getSkin());
    }

    public TextPos getTextPosition(double screenX, double screenY) {
        Point2D local = vflow().getContentPane().screenToLocal(screenX, screenY);
        return vflow().getTextPosLocal(local.getX(), local.getY());
    }

    /**
     * Determines the caret blink rate.
     */
    public final ReadOnlyObjectProperty<Duration> caretBlinkPeriodProperty() {
        return caretBlinkPeriod.getReadOnlyProperty();
    }

    public final void setCaretBlinkPeriod(Duration period) {
        if (period == null) {
            throw new NullPointerException("caret blink period cannot be null");
        }
        caretBlinkPeriod.set(period);
    }

    public final Duration getCaretBlinkPeriod() {
        return caretBlinkPeriod.get();
    }

    public void moveCaret(TextPos p, boolean extendSelection) {
        if (extendSelection) {
            extendSelection(p);
        } else {
            select(p, p);
        }
    }

    /**
     * Tracks the caret position within the document.  Can be null.
     */
    public final ReadOnlyProperty<TextPos> caretPositionProperty() {
        return selectionModel.caretPositionProperty();
    }
    
    public final TextPos getCaretPosition() {
        return caretPositionProperty().getValue();
    }

    public final TextPos getAnchorPosition() {
        return anchorPositionProperty().getValue();
    }

    /**
     * Tracks the selection anchor position within the document.  Can be null.
     */
    public final ReadOnlyProperty<TextPos> anchorPositionProperty() {
        return selectionModel.anchorPositionProperty();
    }

    /**
     * Tracks the current selection segment position.  Can be null.
     */
    public final ReadOnlyProperty<SelectionSegment> selectionSegmentProperty() {
        return selectionModel.selectionSegmentProperty();
    }

    public void clearSelection() {
        selectionModel.clear();
    }

    /** Moves the caret to the specified position, clearing the selection */
    public void select(TextPos pos) {
        StyledTextModel model = getModel();
        if (model != null) {
            Marker m = model.getMarker(pos);
            selectionModel.setSelection(m, m);
        }
    }

    /** Selects the specified range */
    public void select(TextPos anchor, TextPos caret) {
        StyledTextModel model = getModel();
        if (model != null) {
            Marker ma = model.getMarker(anchor);
            Marker mc = model.getMarker(caret);
            selectionModel.setSelection(ma, mc);
        }
    }
    
    /** Extends selection from the existing anchor to the new position. */
    public void extendSelection(TextPos pos) {
        StyledTextModel model = getModel();
        if (model != null) {
            Marker m = model.getMarker(pos);
            selectionModel.extendSelection(m);
        }
    }

    public int getParagraphCount() {
        StyledTextModel m = getModel();
        return (m == null) ? 0 : m.size();
    }
    
    public String getPlainText(int modelIndex) {
        if((modelIndex < 0) || (modelIndex >= getParagraphCount())) {
            throw new IllegalArgumentException("No paragraph at index=" + modelIndex);
        }
        return getModel().getPlainText(modelIndex);
    }
    
    private RichTextAreaSkin richTextAreaSkin() {
        return (RichTextAreaSkin)getSkin();
    }

    /**
     * The size of a tab stop in spaces.
     *
     * @return the {@code tabSize} property
     *
     * @defaultValue 8
     */
    public final ReadOnlyIntegerProperty tabSizeProperty() {
        return tabSizePropertyPrivate().getReadOnlyProperty();
    }

    public final void setTabSize(int n) {
        if ((n < 1) || (n > Params.MAX_TAB_SIZE)) {
            throw new IllegalArgumentException("tab size out of range (1-" + Params.MAX_TAB_SIZE + ") " + n);
        }
        tabSizePropertyPrivate().set(n);
    }

    public final int getTabSize() {
        if (tabSizeProperty == null) {
            return 8;
        }
        return tabSizeProperty.get();
    }

    private ReadOnlyIntegerWrapper tabSizePropertyPrivate() {
        if (tabSizeProperty == null) {
            tabSizeProperty = new ReadOnlyIntegerWrapper(8);
        }
        return tabSizeProperty;
    }

    /**
     * Determines whether the preferred width is the same as the content width.
     * When set to true, the horizontal scroll bar is disabled.
     *
     * @defaultValue false
     */
    public final BooleanProperty useContentWidthProperty() {
        if (useContentWidth == null) {
            useContentWidth = new SimpleBooleanProperty();
        }
        return useContentWidth;
    }

    public final boolean isUseContentWidth() {
        return useContentWidth == null ? false : useContentWidth.get();
    }

    public final void setUseContentWidth(boolean on) {
        useContentWidthProperty().set(true);
    }
    
    /**
     * Determines whether the preferred height is the same as the content height.
     * When set to true, the vertical scroll bar is disabled.
     *
     * @defaultValue false
     */
    public final BooleanProperty useContentHeightProperty() {
        if (useContentHeight == null) {
            useContentHeight = new SimpleBooleanProperty();
        }
        return useContentHeight;
    }

    public final boolean isUseContentHeight() {
        return useContentHeight == null ? false : useContentHeight.get();
    }

    public final void setUseContentHeight(boolean on) {
        useContentHeightProperty().set(true);
    }

    /**
     * Replaces selected text.
     * @return new caret position if a change has been made, or null.
     */
    public TextPos replaceText(TextPos start, TextPos end, String text) {
        if (canEdit()) {
            StyledTextModel m = getModel();
            return m.replace(vflow(), start, end, text);
        }
        return null;
    }

    /**
     * When selection exists, deletes selecteed text.  Otherwise, deletes the symbol before the caret.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void backspace() {
        execute(BACKSPACE);
    }
    
    /**
     * When selection exists, copies the selected rich text to the clipboard in all formats supported
     * by the model.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void copy() {
        execute(COPY);
    }
    // TODO copy plain text?

    /**
     * When selection exists, removes the selected rich text and places it into the clipboard.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void cut() {
        execute(CUT);
    }

    /**
     * When selection exists, deletes selected text.  Otherwise, deletes the symbol at the caret.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void delete() {
        execute(DELETE);
    }

    /**
     * Inserts a line break at the caret.  If selection exists, first deletes the selected text.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void insertLineBreak() {
        execute(INSERT_LINE_BREAK);
    }
    
    /**
     * Inserts a tab symbol at the caret.  If selection exists, first deletes the selected text.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void insertTab() {
        execute(INSERT_TAB);
    }
    
    /**
     * Moves the caret to after the last character of the text, also clearing the selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void moveDocumentEnd() {
        execute(MOVE_DOCUMENT_END);
    }
    
    /**
     * Moves the caret to before the first character of the text.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void moveDocumentStart() {
        execute(MOVE_DOCUMENT_START);
    }
    
    /**
     * Moves the caret down.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void moveDown() {
        execute(MOVE_DOWN);
    }
    
    /**
     * Moves the caret left.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void moveLeft() {
        execute(MOVE_LEFT);
    }
    
    /**
     * Moves the caret to the end of the current line of text.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void moveLineEnd() {
        execute(MOVE_LINE_END);
    }
    
    /**
     * Moves the caret to the start of the current line of text.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void moveLineStart() {
        execute(MOVE_LINE_START);
    }
    
    /**
     * Moves the caret to the next symbol.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void moveRight() {
        execute(MOVE_RIGHT);
    }
    
    /**
     * Moves the caret up.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void moveUp() {
        execute(MOVE_UP);
    }
    
    /**
     * Moves the caret to the beginning of previous word.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void movePreviousWord() {
        execute(MOVE_WORD_PREVIOUS);
    }

    /**
     * Moves the caret to the beginning of next word.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void moveNextWord() {
        execute(MOVE_WORD_NEXT);
    }

    /**
     * Moves the caret to the end of the next word.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void moveEndOfNextWord() {
        execute(MOVE_WORD_NEXT_END);
    }
    
    /**
     * Move caret one page down.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void pageDown() {
        execute(PAGE_DOWN);
    }
    
    /**
     * Move caret one page up.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void pageUp() {
        execute(PAGE_UP);
    }

    /**
     * Pastes the clipboard content at the caret, or, if selection exists, replacing the selected text.
     * The model decides which format to use.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void paste() {
        execute(PASTE);
    }

    /**
     * Pastes the plain text clipboard content at the caret, or, if selection exists, replacing the selected text.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void pastePlainText() {
        execute(PASTE_PLAIN_TEXT);
    }
    
    /**
     * If possible, redoes the last undone modification. If {@link #isRedoable()} returns
     * false, then calling this method has no effect.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void redo() {
        execute(REDO);
    }
    
    /**
     * Selects all the text in the document.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void selectAll() {
        execute(SELECT_ALL);
    }

    /**
     * Selects from the anchor position to the document start.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void selectDocumentStart() {
        execute(SELECT_DOCUMENT_START);
    }

    /**
     * Selects from the anchor position to the document end.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void selectDocumentEnd() {
        execute(SELECT_DOCUMENT_END);
    }
    
    /**
     * Moves the caret down and extends selection to the new position.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void selectDown() {
        execute(SELECT_DOWN);
    }
    
    /**
     * Moves the caret left and extends selection to the new position.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void selectLeft() {
        execute(SELECT_LEFT);
    }
    
    /**
     * Moves the caret one page down and extends selection to the new position.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void selectPageDown() {
        execute(SELECT_PAGE_DOWN);
    }
    
    /**
     * Moves the caret one page up and extends selection to the new position.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void selectPageUp() {
        execute(SELECT_PAGE_UP);
    }
    
    /**
     * Selects the paragraph at the caret.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void selectParagraph() {
        execute(SELECT_PARAGRAPH);
    }
    
    /**
     * Moves the caret right and extends selection to the new position.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void selectRight() {
        execute(SELECT_RIGHT);
    }
    
    /**
     * Moves the caret up and extends selection to the new position.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void selectUp() {
        execute(SELECT_UP);
    }

    /**
     * Selects a word at the caret.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void selectWord() {
        execute(SELECT_WORD);
    }
    
    /**
     * Moves the caret to the beginning of next word. This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of next word.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void selectNextWord() {
        execute(SELECT_WORD_NEXT);
    }
    
    /**
     * Moves the caret to the beginning of previous word. This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of previous word.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void selectPreviousWord() {
        execute(SELECT_WORD_PREVIOUS);
    }

    /**
     * Moves the caret to the end of the next word. This does not cause
     * the selection to be cleared.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void selectEndOfNextWord() {
        execute(SELECT_WORD_NEXT_END);
    }

    /**
     * If possible, undoes the last modification. If {@link #isUndoable()} returns
     * false, then calling this method has no effect.
     * <p>This action can be changed by remapping the default behavior, @see {@link #getKeyMap()}.
     */
    public void undo() {
        execute(UNDO);
    }
    
    public boolean isUndoable() {
        // TODO
        return false;
    }
    
    public boolean isRedoable() {
        // TODO
        return false;
    }

    /**
     * Returns true if a non-empty selection exists.
     */
    public boolean hasNonEmptySelection() {
        TextPos ca = getCaretPosition();
        if (ca != null) {
            TextPos an = getAnchorPosition();
            if (an != null) {
                return !ca.isSameInsertionIndex(an);
            }
        }
        return false;
    }

    public void applyStyle(TextPos start, TextPos end, StyleAttrs attrs) {
        if (canEdit()) {
            StyledTextModel m = getModel();
            if (start.compareTo(end) > 0) {
                TextPos p = start;
                start = end;
                end = p;
            }
            m.applyStyle(start, end, attrs);
        }
    }

    protected boolean canEdit() {
        if (isEditable()) {
            StyledTextModel m = getModel();
            if (m != null) {
                return m.isEditable();
            }
        }
        return false;
    }

    /**
     * When selection exists, returns the style of the first selected character.
     * When no selection exists, returns the style of a character immediately preceding the caret.
     * When at the beginning of the document, returns the style of the first character.
     *
     * @return non-null {@link StyleInfo}
     */
    public StyleInfo getActiveStyleInfo() {
        StyledTextModel m = getModel();
        if (m != null) {
            TextPos pos = getCaretPosition();
            if (pos != null) {
                if (hasNonEmptySelection()) {
                    TextPos an = getAnchorPosition();
                    if (pos.compareTo(an) > 0) {
                        pos = an;
                    }
                } else if (!TextPos.ZERO.equals(pos)) {
                    int ix = pos.offset() - 1;
                    pos = new TextPos(pos.index(), ix);
                }
                return m.getStyleInfo(pos);
            }
        }
        return StyleInfo.NONE;
    }

    /**
     * When selection exists, returns the attributes (resolved for this instance of {@code RichTextArea}
     * of the first selected character.
     * When no selection exists, returns the attributes of a character immediately preceding the caret.
     * When at the beginning of the document, returns the attributes of the first character.
     *
     * @return {@link StyleAttrs}, or null if no style is defined.
     */
    public StyleAttrs getActiveStyleAttrs() {
        StyleInfo s = getActiveStyleInfo();
        if (s.hasAttributes()) {
            return s.getAttributes();
        }

        if (getSkin() instanceof StyleResolver r) {
            return s.getStyleAttrs(r);
        }

        return null;
    }

    /** Returns a TextPos corresponding to the end of the document */
    public TextPos getEndTextPos() {
        StyledTextModel m = getModel();
        return (m == null) ? TextPos.ZERO : m.getEndTextPos();
    }

    /** Returns a TextPos corresponding to the end of paragraph */
    public TextPos getEndOfParagraph(int index) {
        StyledTextModel m = getModel();
        return (m == null) ? TextPos.ZERO : m.getEndOfParagraphTextPos(index);
    }

    /**
     * Specifies the left-side paragraph decorator.  Can be null.
     */
    public final ObjectProperty<SideDecorator> leftDecoratorProperty() {
        if (leftDecorator == null) {
            leftDecorator = new SimpleObjectProperty<>();
        }
        return leftDecorator;
    }

    public final SideDecorator getLeftDecorator() {
        if (leftDecorator == null) {
            return null;
        }
        return leftDecorator.get();
    }
    
    public final void setLeftDecorator(SideDecorator d) {
        leftDecoratorProperty().set(d);
    }

    /**
     * Specifies the right-side paragraph decorator.  Can be null.
     */
    public final ObjectProperty<SideDecorator> rightDecoratorProperty() {
        if (rightDecorator == null) {
            rightDecorator = new SimpleObjectProperty<>();
        }
        return rightDecorator;
    }

    public final SideDecorator getRightDecorator() {
        if (rightDecorator == null) {
            return null;
        }
        return rightDecorator.get();
    }
    
    public final void setRightDecorator(SideDecorator d) {
        rightDecoratorProperty().set(d);
    }

    /**
     * Specifies the padding for the RichTextArea content.  Can be null.
     */
    public final ObjectProperty<Insets> contentPaddingProperty() {
        if (contentPadding == null) {
            contentPadding = new SimpleStyleableObjectProperty<Insets>(
                StyleableProperties.CONTENT_PADDING,
                this,
                "contentPadding");
        }
        return contentPadding;
    }

    public final void setContentPadding(Insets value) {
        contentPaddingProperty().set(value);
    }

    public final Insets getContentPadding() {
        if(contentPadding == null) {
            return null;
        }
        return contentPadding.get();
    }

    /**
     * Determines the spacing between text lines, in pixels.
     */
    public final DoubleProperty lineSpacingProperty() {
        if (lineSpacing == null) {
            lineSpacing = new SimpleStyleableDoubleProperty(
                StyleableProperties.LINE_SPACING,
                this,
                "lineSpacing",
                DEFAULT_LINE_SPACING
            );
        }
        return lineSpacing;
    }

    public final void setLineSpacing(double spacing) {
        lineSpacingProperty().set(spacing);
    }

    public final double getLineSpacing() {
        if (lineSpacing == null) {
            return DEFAULT_LINE_SPACING;
        }
        return lineSpacing.get();
    }

    /** 
     * Creates a visual representation of the model paragraph.
     * By default, delegates to the model.
     * Subclasses may override this method to provide, for instance, additional styling specific to this instance.
     * 
     * @param modelIndex paragraph index
     * @return a new {@link TextCell} instance
     */
    protected TextCell createTextCell(int modelIndex) {
        return getModel().createTextCell(modelIndex);
    }
}
