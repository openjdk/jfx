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

package javafx.incubator.scene.control.rich;

import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.WritableValue;
import javafx.css.CssMetaData;
import javafx.css.FontCssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.StyleConverter;
import javafx.css.StyleOrigin;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.InsetsConverter;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.incubator.scene.control.behavior.FunctionTag;
import javafx.incubator.scene.control.behavior.InputMap;
import javafx.incubator.scene.control.rich.model.EditableRichTextModel;
import javafx.incubator.scene.control.rich.model.StyleAttrs;
import javafx.incubator.scene.control.rich.model.StyledTextModel;
import javafx.incubator.scene.control.rich.skin.RichTextAreaSkin;
import javafx.incubator.scene.control.util.Util;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Control;
import javafx.scene.input.DataFormat;
import javafx.scene.text.Font;
import javafx.util.Duration;
import com.sun.javafx.scene.control.rich.Params;
import com.sun.javafx.scene.control.rich.RichTextAreaSkinHelper;
import com.sun.javafx.scene.control.rich.VFlow;

/**
 * Text input component that allows a user to enter multiple lines of rich text.
 */
public class RichTextArea extends Control {
    /** Deletes previous symbol */
    public static final FunctionTag BACKSPACE = new FunctionTag();
    /** Copies selected text to the clipboard */
    public static final FunctionTag COPY = new FunctionTag();
    /** Cuts selected text and places it to the clipboard */
    public static final FunctionTag CUT = new FunctionTag();
    /** Deletes symbol at the caret */
    public static final FunctionTag DELETE = new FunctionTag();
    /** Deletes paragraph at the caret, or selected paragraphs */
    public static final FunctionTag DELETE_PARAGRAPH = new FunctionTag();
    /** Inserts a single line break */
    public static final FunctionTag INSERT_LINE_BREAK = new FunctionTag();
    /** Inserts a TAB symbol */
    public static final FunctionTag INSERT_TAB = new FunctionTag();
    /** Moves the caret to end of the document */
    public static final FunctionTag MOVE_DOCUMENT_END = new FunctionTag();
    /** Moves the caret to beginning of the document */
    public static final FunctionTag MOVE_DOCUMENT_START = new FunctionTag();
    /** Moves the caret one visual text line down */
    public static final FunctionTag MOVE_DOWN = new FunctionTag();
    /** Moves the caret one symbol to the left */
    public static final FunctionTag MOVE_LEFT = new FunctionTag();
    /** Moves the caret to the end of the current paragraph */
    public static final FunctionTag MOVE_PARAGRAPH_END = new FunctionTag();
    /** Moves the caret to the beginning of the current paragraph */
    public static final FunctionTag MOVE_PARAGRAPH_START = new FunctionTag();
    /** Moves the caret one symbol to the right */
    public static final FunctionTag MOVE_RIGHT = new FunctionTag();
    /** Moves the caret one visual text line up */
    public static final FunctionTag MOVE_UP = new FunctionTag();
    /** Moves the caret one word left (previous word if LTR, next word if RTL) */
    public static final FunctionTag MOVE_WORD_LEFT = new FunctionTag();
    /** Moves the caret to the next word */
    public static final FunctionTag MOVE_WORD_NEXT = new FunctionTag();
    /** Moves the caret to the end of next word */
    public static final FunctionTag MOVE_WORD_NEXT_END = new FunctionTag();
    /** Moves the caret to the previous word */
    public static final FunctionTag MOVE_WORD_PREVIOUS = new FunctionTag();
    /** Moves the caret one word right (next word if LTR, previous word if RTL) */
    public static final FunctionTag MOVE_WORD_RIGHT = new FunctionTag();
    /** Moves the caret one page down */
    public static final FunctionTag PAGE_DOWN = new FunctionTag();
    /** Moves the caret one page up */
    public static final FunctionTag PAGE_UP = new FunctionTag();
    /** Inserts rich text from the clipboard */
    public static final FunctionTag PASTE = new FunctionTag();
    /** Inserts plain text from the clipboard */
    public static final FunctionTag PASTE_PLAIN_TEXT = new FunctionTag();
    /** Reverts the last undo operation */
    public static final FunctionTag REDO = new FunctionTag();
    /** Selects all text in the document */
    public static final FunctionTag SELECT_ALL = new FunctionTag();
    /** Selects text (or extends selection) from the current caret position to the end of document */
    public static final FunctionTag SELECT_DOCUMENT_END = new FunctionTag();
    /** Selects text (or extends selection) from the current caret position to the start of document */
    public static final FunctionTag SELECT_DOCUMENT_START = new FunctionTag();
    /** Selects text (or extends selection) from the current caret position one visual text line down */
    public static final FunctionTag SELECT_DOWN = new FunctionTag();
    /** Selects text (or extends selection) from the current position to one symbol to the left */
    public static final FunctionTag SELECT_LEFT = new FunctionTag();
    /** Selects text (or extends selection) from the current position to one page down */
    public static final FunctionTag SELECT_PAGE_DOWN = new FunctionTag();
    /** Selects text (or extends selection) from the current position to one page up */
    public static final FunctionTag SELECT_PAGE_UP = new FunctionTag();
    /** Selects text (or extends selection) of the current paragraph */
    public static final FunctionTag SELECT_PARAGRAPH = new FunctionTag();
    /** Selects text (or extends selection) from the current position to one symbol to the right */
    public static final FunctionTag SELECT_RIGHT = new FunctionTag();
    /** Selects text (or extends selection) from the current caret position one visual text line up */
    public static final FunctionTag SELECT_UP = new FunctionTag();
    /** Selects word at the caret position */
    public static final FunctionTag SELECT_WORD = new FunctionTag();
    /** Extends selection to the previous word (LTR) or next word (RTL) */
    public static final FunctionTag SELECT_WORD_LEFT = new FunctionTag();
    /** Extends selection to the next word */
    public static final FunctionTag SELECT_WORD_NEXT = new FunctionTag();
    /** Extends selection to the end of next word */
    public static final FunctionTag SELECT_WORD_NEXT_END = new FunctionTag();
    /** Extends selection to the previous word */
    public static final FunctionTag SELECT_WORD_PREVIOUS = new FunctionTag();
    /** Extends selection to the next word (LTR) or previous word (RTL) */
    public static final FunctionTag SELECT_WORD_RIGHT = new FunctionTag();
    /** Undoes the last edit operation */
    public static final FunctionTag UNDO = new FunctionTag();

    private final ConfigurationParameters config;
    private final ObjectProperty<StyledTextModel> model = new SimpleObjectProperty<>(this, "model");
    private final SimpleBooleanProperty displayCaretProperty = new SimpleBooleanProperty(this, "displayCaret", true);
    private ObjectProperty<StyleAttrs> defaultParagraphAttributes;
    private SimpleBooleanProperty editableProperty;
    private StyleableObjectProperty<Font> font;
    private final ReadOnlyObjectWrapper<Duration> caretBlinkPeriod;
    private final SelectionModel selectionModel = new SingleSelectionModel();
    private ReadOnlyIntegerWrapper tabSizeProperty;
    private ObjectProperty<SideDecorator> leftDecorator;
    private ObjectProperty<SideDecorator> rightDecorator;
    private ObjectProperty<Insets> contentPadding;
    private BooleanProperty highlightCurrentParagraph;
    private BooleanProperty useContentWidth;
    private BooleanProperty useContentHeight;

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

    @Override
    protected RichTextAreaSkin createDefaultSkin() {
        return new RichTextAreaSkin(this, config);
    }

    /**
     * Determines the {@link StyledTextModel} to use with this RichTextArea.
     * The model can be null.
     * @return the model property
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
     * The default font to use for text in the RichTextArea.
     * If the RichTextArea's text is
     * rich text then this font may or may not be used depending on the font
     * information embedded in the rich text, but in any case where a default
     * font is required, this font will be used.
     * @return the font property
     */
    public final ObjectProperty<Font> fontProperty() {
        if (font == null) {
            font = new StyleableObjectProperty<Font>(Font.getDefault()) {
                private boolean fontSetByCss;

                @Override
                public void applyStyle(StyleOrigin newOrigin, Font value) {
                    // RT-20727 JDK-8127428
                    // if CSS is setting the font, then make sure invalidate doesn't call NodeHelper.reapplyCSS
                    try {
                        // super.applyStyle calls set which might throw if value is bound.
                        // Have to make sure fontSetByCss is reset.
                        fontSetByCss = true;
                        super.applyStyle(newOrigin, value);
                    } catch (Exception e) {
                        throw e;
                    } finally {
                        fontSetByCss = false;
                    }
                }

                @Override
                public void set(Font value) {
                    Font old = get();
                    if (value == null ? old == null : value.equals(old)) {
                        return;
                    }
                    super.set(value);
                }

                @Override
                protected void invalidated() {
                    /** FIX reapplyCSS should be public
                    // RT-20727 JDK-8127428
                    // if font is changed by calling setFont, then
                    // css might need to be reapplied since font size affects
                    // calculated values for styles with relative values
                    if (fontSetByCss == false) {
                        NodeHelper.reapplyCSS(RichTextArea.this);
                    }
                    */
                    // don't know whether this is ok
                    requestLayout();
                }

                @Override
                public CssMetaData<RichTextArea, Font> getCssMetaData() {
                    return StyleableProperties.FONT;
                }

                @Override
                public Object getBean() {
                    return RichTextArea.this;
                }

                @Override
                public String getName() {
                    return "font";
                }
            };
        }
        return font;
    }

    public final void setFont(Font value) {
        fontProperty().setValue(value);
    }

    public final Font getFont() {
        return font == null ? Font.getDefault() : font.getValue();
    }

    /**
     * Indicates whether text should be wrapped in this RichTextArea.
     * If a run of text exceeds the width of the {@code RichTextArea},
     * then this variable indicates whether the text should wrap onto
     * another line.
     * Setting this property to {@code true} has a side effect of hiding the horizontal scroll bar.
     * @defaultValue false
     */
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

    /**
     * This property controls whether caret will be displayed or not.
     * TODO StyleableProperty ?
     * @return the display caret property
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
     * @return the editable property
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
     * TODO StyleableProperty ?
     * @return the highlight current paragraph property
     */
    public final BooleanProperty highlightCurrentParagraphProperty() {
        if (highlightCurrentParagraph == null) {
            highlightCurrentParagraph = new SimpleBooleanProperty(this, "highlightCurrentParagraph", true);
        }
        return highlightCurrentParagraph;
    }

    public final boolean isHighlightCurrentParagraph() {
        if (highlightCurrentParagraph == null) {
            return true;
        }
        return highlightCurrentParagraph.get();
    }
    
    public final void setHighlightCurrentParagraph(boolean on) {
        highlightCurrentParagraphProperty().set(on);
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

        // TODO remove if switched to paragraph attributes
        private static final FontCssMetaData<RichTextArea> FONT =
            new FontCssMetaData<>("-fx-font", Font.getDefault()) {

            @Override
            public boolean isSettable(RichTextArea n) {
                return n.font == null || !n.font.isBound();
            }

            @Override
            public StyleableProperty<Font> getStyleableProperty(RichTextArea n) {
                return (StyleableProperty<Font>)(WritableValue<Font>)n.fontProperty();
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
            FONT,
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
        return RichTextAreaSkinHelper.getVFlow(this);
    }

    /**
     * Finds a text position corresponding to the specified screen coordinates.
     * @param screenX screen x coordinate
     * @param screenY screen y coordinate
     * @return the TextPosition
     */
    public TextPos getTextPosition(double screenX, double screenY) {
        Point2D local = vflow().getContentPane().screenToLocal(screenX, screenY);
        return vflow().getTextPosLocal(local.getX(), local.getY());
    }

    /**
     * Determines the caret blink rate.
     * @return the caret blunk period property
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

    /**
     * Moves the caret and anchor to the new position, unless {@code extendSelection} is true, in which case
     * extend selection from the existing anchor to the newly set caret position.
     * @param p text position
     * @param extendSelection specifies whether to clear (false) or extend (true) any existing selection
     */
    public void moveCaret(TextPos p, boolean extendSelection) {
        if (extendSelection) {
            extendSelection(p);
        } else {
            select(p, p);
        }
    }

    /**
     * Tracks the caret position within the document.  The value can be null.
     * <p>
     * Important note: setting a {@link SelectionSegment} causes an update to both anchor and caret properties.
     * Typically, they both should be either null (corresponding to a null selection segment) or non-null.
     * However, it is possible to read one null value and one non-null value in a listener.  To lessen the impact,
     * the caretProperty is updated last, so any listener monitoring the caret property would read the right anchor
     * value.  A listener monitoring the anchorProperty might see erroneous value for the caret, so keep that in mind.
     *
     * @return the caret position property
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
     * Tracks the selection anchor position within the document.  The value can be null.
     * <p>
     * Important note: setting a {@link SelectionSegment} causes an update to both anchor and caret properties.
     * Typically, they both should be either null (corresponding to a null selection segment) or non-null.
     * However, it is possible to read one null value and one non-null value in a listener.  To lessen the impact,
     * the caretProperty is updated last, so any listener monitoring the caret property would read the right anchor
     * value.  A listener monitoring the anchorProperty might see erroneous value for the caret, so keep that in mind.
     *
     * @return the anchor position property
     */
    public final ReadOnlyProperty<TextPos> anchorPositionProperty() {
        return selectionModel.anchorPositionProperty();
    }

    /**
     * Tracks the current selection segment position.
     * The value can be null.
     * @return the selection segment property
     */
    public final ReadOnlyProperty<SelectionSegment> selectionSegmentProperty() {
        return selectionModel.selectionSegmentProperty();
    }

    /**
     * Clears existing selection, if any.
     */
    public void clearSelection() {
        selectionModel.clear();
    }

    /**
     * Moves the caret to the specified position, clearing the selection.
     * @param pos the text position
     */
    public void setCaret(TextPos pos) {
        StyledTextModel model = getModel();
        if (model != null) {
            Marker m = model.getMarker(pos);
            selectionModel.setSelection(m, m);
        }
    }

    /**
     * Selects the specified range and places the caret at the new position.
     * @param anchor the new selection anchor position
     * @param caret the new caret position
     */
    public void select(TextPos anchor, TextPos caret) {
        StyledTextModel model = getModel();
        if (model != null) {
            Marker ma = model.getMarker(anchor);
            Marker mc = model.getMarker(caret);
            selectionModel.setSelection(ma, mc);
        }
    }
    
    /**
     * Extends selection from the existing anchor to the new position.
     * @param pos the text position
     */
    public void extendSelection(TextPos pos) {
        StyledTextModel model = getModel();
        if (model != null) {
            Marker m = model.getMarker(pos);
            selectionModel.extendSelection(m);
        }
    }

    /**
     * Returns the number of paragraphs in the model.  If model is null, returns 0.
     * @return the paragraph count
     */
    public int getParagraphCount() {
        StyledTextModel m = getModel();
        return (m == null) ? 0 : m.size();
    }

    /**
     * Returns the plain text at the specified paragraph index.
     * @param modelIndex paragraph index
     * @return plain text string, or null
     * @throws IllegalArgumentException if the modelIndex is outside of the range supported by the model
     */
    public String getPlainText(int modelIndex) {
        if ((modelIndex < 0) || (modelIndex >= getParagraphCount())) {
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
     * @defaultValue 8
     */
    // FIX this makes sense only in the context of CodeArea
//    public final ReadOnlyIntegerProperty tabSizeProperty() {
//        return tabSizePropertyPrivate().getReadOnlyProperty();
//    }
//
//    public final void setTabSize(int n) {
//        if ((n < 1) || (n > Params.MAX_TAB_SIZE)) {
//            throw new IllegalArgumentException("tab size out of range (1-" + Params.MAX_TAB_SIZE + ") " + n);
//        }
//        tabSizePropertyPrivate().set(n);
//    }
//
//    public final int getTabSize() {
//        if (tabSizeProperty == null) {
//            return 8;
//        }
//        return tabSizeProperty.get();
//    }
//
//    private ReadOnlyIntegerWrapper tabSizePropertyPrivate() {
//        if (tabSizeProperty == null) {
//            tabSizeProperty = new ReadOnlyIntegerWrapper(8);
//        }
//        return tabSizeProperty;
//    }

    /**
     * Determines whether the preferred width is the same as the content width.
     * When set to true, the horizontal scroll bar is disabled.
     *
     * @defaultValue false
     * @return the use content width property
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
     * @return the use content height property
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
     * Replaces the specified range with the new text.
     * @param start start text position
     * @param end end text position
     * @param text text string to insert
     * @param createUndo when true, creates an undo-redo entry
     * @return new caret position at the end of inserted text, or null if the change cannot be made
     */
    public TextPos replaceText(TextPos start, TextPos end, String text, boolean createUndo) {
        if (canEdit()) {
            StyledTextModel m = getModel();
            return m.replace(vflow(), start, end, text, createUndo);
        }
        return null;
    }

    /**
     * When selection exists, deletes selected text.  Otherwise, deletes the symbol before the caret.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void backspace() {
        execute(BACKSPACE);
    }
    
    /**
     * When selection exists, copies the selected rich text to the clipboard in all formats supported
     * by the model.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void copy() {
        execute(COPY);
    }

    /**
     * Copies the text in the specified format when selection exists and when the export in this format
     * is supported by the model, and the skin must be installed; otherwise, this method is a no-op.
     * @param format the data format to use
     */
    public void copy(DataFormat format) {
        RichTextAreaSkin skin = richTextAreaSkin();
        if (skin != null) {
            skin.copy(format);
        }
    }

    /**
     * When selection exists, removes the selected rich text and places it into the clipboard.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void cut() {
        execute(CUT);
    }

    /**
     * When selection exists, deletes selected text.  Otherwise, deletes the symbol at the caret.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void delete() {
        execute(DELETE);
    }

    /**
     * Inserts a line break at the caret.  If selection exists, first deletes the selected text.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void insertLineBreak() {
        execute(INSERT_LINE_BREAK);
    }
    
    /**
     * Inserts a tab symbol at the caret.  If selection exists, first deletes the selected text.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void insertTab() {
        execute(INSERT_TAB);
    }
    
    /**
     * Moves the caret to after the last character of the text, also clearing the selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void moveDocumentEnd() {
        execute(MOVE_DOCUMENT_END);
    }
    
    /**
     * Moves the caret to before the first character of the text.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void moveDocumentStart() {
        execute(MOVE_DOCUMENT_START);
    }
    
    /**
     * Moves the caret down.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void moveDown() {
        execute(MOVE_DOWN);
    }
    
    /**
     * Moves the caret left.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void moveLeft() {
        execute(MOVE_LEFT);
    }
    
    /**
     * Moves the caret to the end of the current paragraph.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void moveParagraphEnd() {
        execute(MOVE_PARAGRAPH_END);
    }
    
    /**
     * Moves the caret to the start of the current paragraph.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void moveLineStart() {
        execute(MOVE_PARAGRAPH_START);
    }
    
    /**
     * Moves the caret to the next symbol.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void moveRight() {
        execute(MOVE_RIGHT);
    }
    
    /**
     * Moves the caret up.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void moveUp() {
        execute(MOVE_UP);
    }
    
    /**
     * Moves the caret to the beginning of previous word in a left-to-right setting,
     * or the beginning of the next word in a right-to-left setting.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void moveLeftWord() {
        execute(MOVE_WORD_PREVIOUS);
    }

    /**
     * Moves the caret to the beginning of next word in a left-to-right setting,
     * or the beginning of the previous word in a right-to-left setting.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void moveRightWord() {
        execute(MOVE_WORD_NEXT);
    }
    
    /**
     * Moves the caret to the beginning of previous word.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void movePreviousWord() {
        execute(MOVE_WORD_PREVIOUS);
    }

    /**
     * Moves the caret to the beginning of next word.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void moveNextWord() {
        execute(MOVE_WORD_NEXT);
    }

    /**
     * Moves the caret to the end of the next word.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void moveEndOfNextWord() {
        execute(MOVE_WORD_NEXT_END);
    }
    
    /**
     * Move caret one page down.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void pageDown() {
        execute(PAGE_DOWN);
    }
    
    /**
     * Move caret one page up.
     * This method has a side effect of clearing an existing selection.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void pageUp() {
        execute(PAGE_UP);
    }

    /**
     * Pastes the clipboard content at the caret, or, if selection exists, replacing the selected text.
     * The model decides which format to use.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void paste() {
        execute(PASTE);
    }

    /**
     * Pastes the clipboard content at the caret, or, if selection exists, replacing the selected text.
     * The format must be supported by the model, and the skin must be installed,
     * otherwise this method has no effect.
     * @param format the data format to use
     */
    public void paste(DataFormat format) {
        RichTextAreaSkin skin = richTextAreaSkin();
        if (skin != null) {
            skin.paste(format);
        }
    }

    /**
     * Pastes the plain text clipboard content at the caret, or, if selection exists, replacing the selected text.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void pastePlainText() {
        execute(PASTE_PLAIN_TEXT);
    }
    
    /**
     * If possible, redoes the last undone modification. If {@link #isRedoable()} returns
     * false, then calling this method has no effect.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void redo() {
        execute(REDO);
    }
    
    /**
     * Selects all the text in the document.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void selectAll() {
        execute(SELECT_ALL);
    }

    /**
     * Selects from the anchor position to the document start.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void selectDocumentStart() {
        execute(SELECT_DOCUMENT_START);
    }

    /**
     * Selects from the anchor position to the document end.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void selectDocumentEnd() {
        execute(SELECT_DOCUMENT_END);
    }
    
    /**
     * Moves the caret down and extends selection to the new position.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void selectDown() {
        execute(SELECT_DOWN);
    }
    
    /**
     * Moves the caret left and extends selection to the new position.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void selectLeft() {
        execute(SELECT_LEFT);
    }
    
    /**
     * Moves the caret one page down and extends selection to the new position.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void selectPageDown() {
        execute(SELECT_PAGE_DOWN);
    }
    
    /**
     * Moves the caret one page up and extends selection to the new position.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void selectPageUp() {
        execute(SELECT_PAGE_UP);
    }
    
    /**
     * Selects the paragraph at the caret.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void selectParagraph() {
        execute(SELECT_PARAGRAPH);
    }
    
    /**
     * Moves the caret right and extends selection to the new position.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void selectRight() {
        execute(SELECT_RIGHT);
    }
    
    /**
     * Moves the caret up and extends selection to the new position.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void selectUp() {
        execute(SELECT_UP);
    }

    /**
     * Selects a word at the caret.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void selectWord() {
        execute(SELECT_WORD);
    }
    
    /**
     * Moves the caret to the beginning of previous word in a left-to-right setting,
     * or to the beginning of the next word in a right-to-left setting.
     * This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of next word.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void selectLeftWord() {
        execute(SELECT_WORD_LEFT);
    }
    
    /**
     * Moves the caret to the beginning of next word in a left-to-right setting,
     * or to the beginning of the previous word in a right-to-left setting.
     * This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of next word.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void selectRightWord() {
        execute(SELECT_WORD_RIGHT);
    }
    
    /**
     * Moves the caret to the beginning of next word. This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of next word.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void selectNextWord() {
        execute(SELECT_WORD_NEXT);
    }
    
    /**
     * Moves the caret to the beginning of previous word. This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of previous word.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void selectPreviousWord() {
        execute(SELECT_WORD_PREVIOUS);
    }

    /**
     * Moves the caret to the end of the next word. This does not cause
     * the selection to be cleared.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void selectEndOfNextWord() {
        execute(SELECT_WORD_NEXT_END);
    }

    /**
     * If possible, undoes the last modification. If {@link #isUndoable()} returns
     * false, then calling this method has no effect.
     * <p>This action can be changed by remapping the default behavior, @see {@link Control#getInputMap()}.
     */
    public void undo() {
        execute(UNDO);
    }
    
    /**
     * Determines whether the last edit operation is undoable.
     * @return true if undoable
     */
    public boolean isUndoable() {
        StyledTextModel m = getModel();
        return m == null ? false : m.isUndoable();
    }

    /**
     * Determines whether the last edit operation is redoable.
     * @return true if redoable
     */
    public boolean isRedoable() {
        StyledTextModel m = getModel();
        return m == null ? false : m.isRedoable();
    }

    /**
     * Returns true if a non-empty selection exists.
     * @return true if selection exists
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

    /**
     * Applies the specified style to the selected range.  The specified attributes
     * will override any existing attributes.  When applying paragraph attributes, the affected range
     * might be wider than specified.
     * @param start the start of text range
     * @param end the end of text range
     * @param attrs the style attributes to apply
     */
    public void applyStyle(TextPos start, TextPos end, StyleAttrs attrs) {
        if (canEdit()) {
            StyledTextModel m = getModel();
            m.applyStyle(start, end, attrs);
        }
    }

    /**
     * Returns true if this control's {@link #isEditable()} returns true and the model's
     * {@link StyledTextModel#isEditable()} also returns true.
     * @return true if model is not null and is editable
     */
    protected boolean canEdit() {
        if (isEditable()) {
            StyledTextModel m = getModel();
            if (m != null) {
                return m.isEditable();
            }
        }
        return false;
    }

    private StyleAttrs getModelStyleAttrs() {
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
                    if (ix < 0) {
                        // FIX find previous symbol
                        ix = 0;
                    }
                    pos = new TextPos(pos.index(), ix);
                }
                return m.getStyleAttrs(pos);
            }
        }
        return StyleAttrs.EMPTY;
    }

    /**
     * Returns {@code StyleAttrs} which contains character and paragraph attributes.
     * <br>
     * When selection exists, returns the attributes at the first selected character.
     * <br>
     * When no selection exists, returns the attributes at the character which immediately precedes the caret.
     * When at the beginning of the document, returns the attributes of the first character.
     * If the model uses CSS styles, this method resolves individual attributes (bold, font size, etc.)
     * according to the stylesheet for this instance of {@code RichTextArea}.
     *
     * @return the non-null {@code StyleAttrs} instance
     */
    // FIX add paragraph attributes
    // FIX: problem - char attrs may need to be resolved; paragraph - don't 
    public StyleAttrs getActiveStyleAttrs() {
        StyleAttrs a = getModelStyleAttrs();
        RichTextAreaSkin skin = richTextAreaSkin();
        if (skin != null) {
            StyleResolver resolver = skin.getStyleResolver();
            if (resolver != null) {
                a = resolver.resolveStyles(a);
            }
        }
        StyleAttrs pa = getDefaultParagraphAttributes();
        if ((pa == null) || pa.isEmpty()) {
            return a;
        }
        return pa.combine(a);
    }

    /**
     * Returns a TextPos corresponding to the end of the document.
     *
     * @return the text position
     */
    public TextPos getEndTextPos() {
        StyledTextModel m = getModel();
        return (m == null) ? TextPos.ZERO : m.getDocumentEnd();
    }

    /**
     * Returns a TextPos corresponding to the end of paragraph.
     *
     * @param index paragraph index
     * @return text position
     */
    public TextPos getEndOfParagraph(int index) {
        StyledTextModel m = getModel();
        return (m == null) ? TextPos.ZERO : m.getEndOfParagraphTextPos(index);
    }

    /**
     * Specifies the left-side paragraph decorator.
     * The value can be null.
     * @return the left decorator property
     */
    public final ObjectProperty<SideDecorator> leftDecoratorProperty() {
        if (leftDecorator == null) {
            leftDecorator = new SimpleObjectProperty<>(this, "leftDecorator");
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
     * Specifies the right-side paragraph decorator.
     * The value can be null.
     * @return the right decorator property
     */
    public final ObjectProperty<SideDecorator> rightDecoratorProperty() {
        if (rightDecorator == null) {
            rightDecorator = new SimpleObjectProperty<>(this, "rightDecorator");
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
     * Specifies the padding for the RichTextArea content.
     * The value can be null.
     * @return the content padding property
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
     * Specifies the default paragraph attributes.
     * The value can be null.
     * @return the default paragraph attributes property
     */
    public final ObjectProperty<StyleAttrs> defaultParagraphAttributesProperty() {
        if (defaultParagraphAttributes == null) {
            defaultParagraphAttributes = new SimpleObjectProperty<>(
                this,
                "defaultParagraphAttributes",
                Params.DEFAULT_PARAGRAPH_ATTRIBUTES
            );
        }
        return defaultParagraphAttributes;
    }

    public final void setDefaultParagraphAttributes(StyleAttrs a) {
        defaultParagraphAttributesProperty().set(a);
    }

    public final StyleAttrs getDefaultParagraphAttributes() {
        if (defaultParagraphAttributes == null) {
            return Params.DEFAULT_PARAGRAPH_ATTRIBUTES;
        }
        return defaultParagraphAttributes.get();
    }

    // will be moved to Control JDK-8314968
    private final InputMap<RichTextArea> inputMap = new InputMap<>(this);

    // will be moved to Control JDK-8314968
    public InputMap<RichTextArea> getInputMap() {
        return inputMap;
    }

    // will be moved to Control JDK-8314968
    protected final void execute(FunctionTag tag) {
        Runnable f = getInputMap().getFunction(tag);
        if (f != null) {
            f.run();
        }
    }
}
