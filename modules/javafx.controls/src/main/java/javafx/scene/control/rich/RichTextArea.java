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
import javafx.scene.control.rich.model.EditableRichTextModel;
import javafx.scene.control.rich.model.StyleAttrs;
import javafx.scene.control.rich.model.StyleInfo;
import javafx.scene.control.rich.model.StyledTextModel;
import javafx.scene.control.rich.util.Util;
import javafx.util.Duration;

/**
 * Styled Text Area.
 * 
 * TODO fit height property
 * TODO fit width property
 * TODO highlight current line property
 * 
 * TODO add methods corresponding to the remaining Action tags
 */
public class RichTextArea extends Control {
    /** command tags */
    public enum Cmd {
        BACKSPACE,
        COPY,
        CUT,
        DELETE,
        INSERT_LINE_BREAK,
        INSERT_TAB,
        MOVE_DOCUMENT_END,
        MOVE_DOCUMENT_START,
        MOVE_DOWN,
        MOVE_END,
        MOVE_HOME,
        MOVE_LEFT,
        MOVE_RIGHT,
        MOVE_UP,
        MOVE_WORD_NEXT,
        MOVE_WORD_NEXT_END,
        MOVE_WORD_PREVIOUS,
        PAGE_DOWN,
        PAGE_UP,
        PASTE,
        PASTE_PLAIN_TEXT,
        SELECT_ALL,
        SELECT_DOCUMENT_END,
        SELECT_DOCUMENT_START,
        SELECT_DOWN,
        SELECT_LEFT,
        SELECT_LINE,
        SELECT_PAGE_DOWN,
        SELECT_PAGE_UP,
        SELECT_RIGHT,
        SELECT_UP,
        SELECT_WORD,
        SELECT_WORD_NEXT,
        SELECT_WORD_NEXT_END,
        SELECT_WORD_PREVIOUS,
     }

    private static final double DEFAULT_LINE_SPACING = 0.0;
    private final Config config;
    protected final ObjectProperty<StyledTextModel> model = new SimpleObjectProperty<>(this, "model");
    protected final SimpleBooleanProperty displayCaretProperty = new SimpleBooleanProperty(this, "displayCaret", true);
    private SimpleBooleanProperty editableProperty;
    protected final ReadOnlyObjectWrapper<Duration> caretBlinkPeriod;
    // TODO property, pluggable models, or boolean (selection enabled?), do we need to allow for multiple selection?
    protected final SelectionModel selectionModel = new SingleSelectionModel();
    private ReadOnlyIntegerWrapper tabSizeProperty;
    private ObjectProperty<SideDecorator> leftDecorator;
    private ObjectProperty<SideDecorator> rightDecorator;
    private ObjectProperty<Insets> contentPadding;
    private DoubleProperty lineSpacing;
    private BooleanProperty highlightCurrentLine;

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
        this(Config.defaultConfig(), model);
    }

    /**
     * Creates an instance with the specified configuration parameters and model.
     * @param c configuration parameters
     * @param m styled text model
     */
    public RichTextArea(Config c, StyledTextModel m) {
        this.config = c;
        
        caretBlinkPeriod = new ReadOnlyObjectWrapper<>(this, "caretBlinkPeriod", Duration.millis(config.caretBlinkPeriod));

        setFocusTraversable(true);
        getStyleClass().add("rich-text-area");
        setAccessibleRole(AccessibleRole.TEXT_AREA);
        setSkin(createDefaultSkin());
        // TODO move to main stylesheet
        // TODO focus border around content area, not the whole thing?
        getStylesheets().add(Util.getResourceURL(getClass(), "RichTextArea.css"));

        if (m != null) {
            setModel(m);
        }
    }

    @Override
    protected RichTextAreaSkin createDefaultSkin() {
        return new RichTextAreaSkin(this, config);
    }

    public void setModel(StyledTextModel m) {
        modelProperty().set(m);
    }

    public StyledTextModel getModel() {
        return (model == null ? null : model.get());
    }

    public ObjectProperty<StyledTextModel> modelProperty() {
        return model;
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

    public final BooleanProperty wrapTextProperty() {
        return wrapText;
    }

    public final boolean isWrapText() {
        return wrapText.getValue();
    }

    public final void setWrapText(boolean value) {
        wrapText.setValue(value);
    }

    public BooleanProperty displayCaretProperty() {
        return displayCaretProperty;
    }

    public void setDisplayCaret(boolean on) {
        displayCaretProperty.set(on);
    }

    public boolean isDisplayCaret() {
        return displayCaretProperty.get();
    }

    public BooleanProperty editableProperty() {
        if (editableProperty == null) {
            editableProperty = new SimpleBooleanProperty(this, "editable", true);
        }
        return editableProperty;
    }
    
    public boolean isEditable() {
        if (editableProperty == null) {
            return true;
        }
        return editableProperty().get();
    }

    public void setEditable(boolean on) {
        editableProperty().set(on);
    }

    public BooleanProperty highlightCurrentLineProperty() {
        if (highlightCurrentLine == null) {
            highlightCurrentLine = new SimpleBooleanProperty(this, "highlightCurrentLine", true);
        }
        return highlightCurrentLine;
    }

    public boolean isHighlightCurrentLine() {
        if (highlightCurrentLine == null) {
            return true;
        }
        return highlightCurrentLine.get();
    }
    
    public void setHighlightCurrentLine(boolean on) {
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
            new CssMetaData<>("-fx-content-padding", InsetsConverter.getInstance(), Insets.EMPTY) {

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
    
    protected VFlow vflow() {
        return ((RichTextAreaSkin)getSkin()).getVFlow();
    }

    public TextPos getTextPosition(double screenX, double screenY) {
        Point2D local = vflow().getContentPane().screenToLocal(screenX, screenY);
        return vflow().getTextPosLocal(local.getX(), local.getY());
    }
    
    public ReadOnlyObjectProperty<Duration> caretBlinkPeriodProperty() {
        return caretBlinkPeriod.getReadOnlyProperty();
    }

    public void setCaretBlinkPeriod(Duration period) {
        if (period == null) {
            throw new NullPointerException("caret blink period cannot be null");
        }
        caretBlinkPeriod.set(period);
    }

    public Duration getCaretBlinkPeriod() {
        return caretBlinkPeriod.get();
    }

    public void moveCaret(TextPos p, boolean extendSelection) {
        if (extendSelection) {
            extendSelection(p);
        } else {
            select(p, p);
        }
    }
    
    public TextPos getCaretPosition() {
        return caretPositionProperty().getValue();
    }

    public ReadOnlyProperty<TextPos> caretPositionProperty() {
        return selectionModel.caretPositionProperty();
    }
    
    public TextPos getAnchorPosition() {
        return anchorPositionProperty().getValue();
    }
    
    public ReadOnlyProperty<TextPos> anchorPositionProperty() {
        return selectionModel.anchorPositionProperty();
    }
    
    public ReadOnlyProperty<SelectionSegment> selectionSegmentProperty() {
        return selectionModel.selectionSegmentProperty();
    }
    
    public ReadOnlyProperty<Origin> originProperty() {
        return vflow().originProperty();
    }

    public Origin getOrigin() {
        return vflow().getOrigin();
    }

    /**
     * Moves the caret to before the first character of the text, also clearing the selection.
     */
    public void moveDocumentStart() {
        execute(Cmd.MOVE_DOCUMENT_START);
    }

    /**
     * Moves the caret to after the last character of the text, also clearing the selection.
     */
    public void moveDocumentEnd() {
        execute(Cmd.MOVE_DOCUMENT_END);
    }

    /** selects from the anchor position to the document start */
    public void selectDocumentStart() {
        execute(Cmd.SELECT_DOCUMENT_START);
    }

    /** selects from the anchor position to the document end */
    public void selectDocumentEnd() {
        execute(Cmd.SELECT_DOCUMENT_END);
    }
    
    public void selectAll() {
        execute(Cmd.SELECT_ALL);
    }
    
    public void selectWord() {
        execute(Cmd.SELECT_WORD);
    }

    public void selectLine() {
        execute(Cmd.SELECT_LINE);
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
    
    private void execute(Cmd a) {
        richTextAreaSkin().execute(a);
    }

    public void setTabSize(int n) {
        if ((n < 1) || (n > config.maxTabSize)) {
            throw new IllegalArgumentException("tab size out of range (1-" + config.maxTabSize + ") " + n);
        }
        tabSizePropertyPrivate().set(n);
    }

    public int getTabSize() {
        if (tabSizeProperty == null) {
            return 8;
        }
        return tabSizeProperty.get();
    }

    public ReadOnlyIntegerProperty tabSizeProperty() {
        return tabSizePropertyPrivate().getReadOnlyProperty();
    }

    private ReadOnlyIntegerWrapper tabSizePropertyPrivate() {
        if (tabSizeProperty == null) {
            tabSizeProperty = new ReadOnlyIntegerWrapper(8);
        }
        return tabSizeProperty;
    }

    /**
     * Replaces selected text.
     * @return new caret position if a change has been made, or null.
     */
    public TextPos replaceText(TextPos start, TextPos end, String text) {
        if (canEdit()) {
            StyledTextModel m = getModel();
            return m.replace(richTextAreaSkin(), start, end, text);
        }
        return null;
    }
    
    public void copy() {
        execute(Cmd.COPY);
    }
    
    public void cut() {
        execute(Cmd.CUT);
    }
    
    public void paste() {
        execute(Cmd.PASTE);
    }
    
    public void pastePlainText() {
        execute(Cmd.PASTE_PLAIN_TEXT);
    }

    public void undo() {
        // TODO
    }
    
    public boolean isUndoable() {
        // TODO
        return false;
    }
    
    public void redo() {
        // TODO
    }
    
    public boolean isRedoable() {
        // TODO
        return false;
    }
    
    /**
     * Moves the caret to the beginning of previous word. This function
     * also has the effect of clearing the selection.
     */
    public void previousWord() {
        execute(Cmd.MOVE_WORD_PREVIOUS);
    }

    /**
     * Moves the caret to the beginning of next word. This function
     * also has the effect of clearing the selection.
     */
    public void nextWord() {
        execute(Cmd.MOVE_WORD_NEXT);
    }

    /**
     * Moves the caret to the end of the next word. This function
     * also has the effect of clearing the selection.
     */
    public void endOfNextWord() {
        execute(Cmd.MOVE_WORD_NEXT_END);
    }

    /**
     * Moves the caret to the beginning of previous word. This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of previous word.
     */
    public void selectPreviousWord() {
        execute(Cmd.SELECT_WORD_PREVIOUS);
    }

    /**
     * Moves the caret to the beginning of next word. This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of next word.
     */
    public void selectNextWord() {
        execute(Cmd.SELECT_WORD_NEXT);
    }

    /**
     * Moves the caret to the end of the next word. This does not cause
     * the selection to be cleared.
     */
    public void selectEndOfNextWord() {
        execute(Cmd.SELECT_WORD_NEXT_END);
    }
    
    public boolean hasSelection() {
        TextPos ca = getCaretPosition();
        if (ca != null) {
            TextPos an = getAnchorPosition();
            if (an != null) {
                return !ca.isSameIndexAndOffset(an);
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
        TextPos pos = getCaretPosition();
        if ((m == null) || (pos == null)) {
            return StyleInfo.NONE;
        }

        if (hasSelection()) {
            TextPos an = getAnchorPosition();
            if (pos.compareTo(an) > 0) {
                pos = an;
            }
        } else if(!TextPos.ZERO.equals(pos)) {
            pos = new TextPos(pos.index(), pos.offset() - 1);
        }

        return m.getStyleInfo(pos);
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
}