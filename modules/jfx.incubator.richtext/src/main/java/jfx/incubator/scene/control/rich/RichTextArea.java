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

package jfx.incubator.scene.control.rich;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.InsetsConverter;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Point2D;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Control;
import javafx.scene.input.DataFormat;
import javafx.util.Duration;
import com.sun.jfx.incubator.scene.control.rich.CssStyles;
import com.sun.jfx.incubator.scene.control.rich.Params;
import com.sun.jfx.incubator.scene.control.rich.RichTextAreaSkinHelper;
import com.sun.jfx.incubator.scene.control.rich.VFlow;
import com.sun.jfx.incubator.scene.control.rich.util.RichUtils;
import jfx.incubator.scene.control.input.FunctionHandler;
import jfx.incubator.scene.control.input.FunctionTag;
import jfx.incubator.scene.control.input.InputMap;
import jfx.incubator.scene.control.rich.model.EditableRichTextModel;
import jfx.incubator.scene.control.rich.model.StyleAttrs;
import jfx.incubator.scene.control.rich.model.StyledInput;
import jfx.incubator.scene.control.rich.model.StyledTextModel;
import jfx.incubator.scene.control.rich.skin.RichTextAreaSkin;

/**
 * The RichTextArea control is designed for visualizing and editing rich text that can be styled in a variety of ways.
 *
 * <p>The RichTextArea control has a number of features, including:
 * <ul>
 * <li> {@link StyledTextModel paragraph-oriented model}, up to ~2 billion rows
 * <li> virtualized text cell flow
 * <li> support for text styling with an application stylesheet or {@link StyleAttrs inline attributes}
 * <li> supports for multiple views connected to the same model
 * <li> {@link SelectionModel single selection}
 * <li> {@link InputMap input map} which allows for easy behavior customization and extension
 * </ul>
 *
 * <h2>Creating a RichTextArea</h2>
 * <p>
 * The following example creates an editable control with the default {@link EditableRichTextModel}:
 * <pre>{@code
 *     RichTextArea t = new RichTextArea();
 * }</pre>
 *
 * <p>
 * A view-only information control requires a different model.  The following example illustrates how to
 * create a model that uses a style sheet for styling:
 * <pre>{@code
 *     SimpleViewOnlyStyledModel m = new SimpleViewOnlyStyledModel();
 *     // add text segment using CSS style name (requires a style sheet)
 *     m.addSegment("RichTextArea ", null, "HEADER");
 *     // add text segment using direct style
 *     m.addSegment("Demo", "-fx-font-size:200%;", null);
 *     // newline
 *     m.nl();
 *
 *     RichTextArea t = new RichTextArea(m);
 * }</pre>
 *
 * <h2>Text Models</h2>
 * <p>
 * A number of standard models can be used with RichTextArea, each addressing a specific use case:
 * </p>
 * <table border=1>
 * <caption>Standard Models</caption>
 * <tr><th>Model Class</th><th>Description</th></tr>
 * <tr><td><pre>{@link StyledTextModel}</pre></td><td>Base class (abstract)</td></tr>
 * <tr><td><pre> ├─ {@link EditableRichTextModel}</pre></td><td>Default model for RichTextArea</td></tr>
 * <tr><td><pre> ├─ {@link jfx.incubator.scene.control.rich.model.PlainTextModel PlainTextModel}</pre></td><td>Unstyled plain text model</td></tr>
 * <tr><td><pre> │   └─ {@link CodeTextModel}</pre></td><td>Default model for CodeArea</td></tr>
 * <tr><td><pre> └─ {@link jfx.incubator.scene.control.rich.model.StyledTextModelViewOnlyBase StyledTextModelViewOnlyBase}</pre></td><td>Base class for a view-only model (abstract)</td></tr>
 * <tr><td><pre>     └─ {@link jfx.incubator.scene.control.rich.model.SimpleViewOnlyStyledModel SimpleViewOnlyStyledModel}</pre></td><td>In-memory view-only styled model</td></tr>
 * </table>
 *
 * <h2>Selection</h2>
 * <p>
 * The RichTextArea control maintains a single {@link #selectionProperty() contiguous selection segment}
 * as a part of the {@link SelectionModel}.  Additionally,
 * {@link #anchorPositionProperty()} and {@link #caretPositionProperty()}
 * are derived from the {@link #selectionProperty()} for convenience.
 *
 * <h2>Customizing</h2>
 * The RichTextArea control offers some degree of customization that does not require subclassing:
 * <ul>
 * <li>customizing key bindings with the {@link InputMap}
 * <li>setting {@link #leftDecoratorProperty() leftDecorator}
 * and {@link #rightDecoratorProperty() rightDecorator} properties
 * <li>providing custom scroll bars via {@link ConfigurationParameters}
 * </ul>
 *
 * @since 999 TODO
 */
public class RichTextArea extends Control {
    /** Function tags serve as identifiers of methods that can be customized via the {@code InputMap}. */
    public static class Tags {
        /** Deletes the symbol before the caret. */
        public static final FunctionTag BACKSPACE = new FunctionTag();
        /** Copies selected text to the clipboard. */
        public static final FunctionTag COPY = new FunctionTag();
        /** Cuts selected text and places it to the clipboard. */
        public static final FunctionTag CUT = new FunctionTag();
        /** Deletes symbol at the caret. */
        public static final FunctionTag DELETE = new FunctionTag();
        /** Deletes paragraph at the caret, or selected paragraphs */
        public static final FunctionTag DELETE_PARAGRAPH = new FunctionTag();
        /** Focus the next focusable node */
        public static final FunctionTag FOCUS_NEXT = new FunctionTag();
        /** Focus the previous focusable node */
        public static final FunctionTag FOCUS_PREVIOUS = new FunctionTag();
        /** Inserts a line break at the caret. */
        public static final FunctionTag INSERT_LINE_BREAK = new FunctionTag();
        /** Inserts a tab symbol at the caret. */
        public static final FunctionTag INSERT_TAB = new FunctionTag();
        /** Moves the caret one visual line down. */
        public static final FunctionTag MOVE_DOWN = new FunctionTag();
        /** Moves the caret one symbol to the left. */
        public static final FunctionTag MOVE_LEFT = new FunctionTag();
        /** Moves the caret one symbol to the right. */
        public static final FunctionTag MOVE_RIGHT = new FunctionTag();
        /** Moves the caret to after the last character of the text. */
        public static final FunctionTag MOVE_TO_DOCUMENT_END = new FunctionTag();
        /** Moves the caret to before the first character of the text. */
        public static final FunctionTag MOVE_TO_DOCUMENT_START = new FunctionTag();
        /** Moves the caret to the end of the paragraph at caret. */
        public static final FunctionTag MOVE_TO_PARAGRAPH_END = new FunctionTag();
        /** Moves the caret to the beginning of the paragraph at caret. */
        public static final FunctionTag MOVE_TO_PARAGRAPH_START = new FunctionTag();
        /** Moves the caret one visual text line up. */
        public static final FunctionTag MOVE_UP = new FunctionTag();
        /** Moves the caret one word left (previous word if LTR, next word if RTL). */
        public static final FunctionTag MOVE_WORD_LEFT = new FunctionTag();
        /** Moves the caret to the beginning of next word. */
        public static final FunctionTag MOVE_WORD_NEXT = new FunctionTag();
        /** Moves the caret to the end of the next word. */
        public static final FunctionTag MOVE_WORD_NEXT_END = new FunctionTag();
        /** Moves the caret to the beginning of previous word. */
        public static final FunctionTag MOVE_WORD_PREVIOUS = new FunctionTag();
        /** Moves the caret one word right (next word if LTR, previous word if RTL). */
        public static final FunctionTag MOVE_WORD_RIGHT = new FunctionTag();
        /** Moves the caret one visual page down. */
        public static final FunctionTag PAGE_DOWN = new FunctionTag();
        /** Moves the caret one visual page up. */
        public static final FunctionTag PAGE_UP = new FunctionTag();
        /** Pastes the clipboard content. */
        public static final FunctionTag PASTE = new FunctionTag();
        /** Pastes the plain text clipboard content. */
        public static final FunctionTag PASTE_PLAIN_TEXT = new FunctionTag();
        /** If possible, redoes the last undone modification. */
        public static final FunctionTag REDO = new FunctionTag();
        /** Selects all text in the document. */
        public static final FunctionTag SELECT_ALL = new FunctionTag();
        /** Extends selection one visual text line down. */
        public static final FunctionTag SELECT_DOWN = new FunctionTag();
        /** Extends selection one symbol to the left. */
        public static final FunctionTag SELECT_LEFT = new FunctionTag();
        /** Extends selection one visible page down. */
        public static final FunctionTag SELECT_PAGE_DOWN = new FunctionTag();
        /** Extends selection one visible page up. */
        public static final FunctionTag SELECT_PAGE_UP = new FunctionTag();
        /** Selects the current paragraph. */
        public static final FunctionTag SELECT_PARAGRAPH = new FunctionTag();
        /** Extends selection one symbol to the right. */
        public static final FunctionTag SELECT_RIGHT = new FunctionTag();
        /** Extends selection to the end of the document. */
        public static final FunctionTag SELECT_TO_DOCUMENT_END = new FunctionTag();
        /** Extends selection to the start of the document. */
        public static final FunctionTag SELECT_TO_DOCUMENT_START = new FunctionTag();
        /** Extends selection one visual text line up. */
        public static final FunctionTag SELECT_UP = new FunctionTag();
        /** Selects a word at the caret position. */
        public static final FunctionTag SELECT_WORD = new FunctionTag();
        /** Extends selection to the previous word (LTR) or next word (RTL). */
        public static final FunctionTag SELECT_WORD_LEFT = new FunctionTag();
        /** Extends selection to the beginning of next word. */
        public static final FunctionTag SELECT_WORD_NEXT = new FunctionTag();
        /** Extends selection to the end of next word. */
        public static final FunctionTag SELECT_WORD_NEXT_END = new FunctionTag();
        /** Extends selection to the previous word. */
        public static final FunctionTag SELECT_WORD_PREVIOUS = new FunctionTag();
        /** Extends selection to the next word (LTR) or previous word (RTL). */
        public static final FunctionTag SELECT_WORD_RIGHT = new FunctionTag();
        /** If possible, undoes the last modification. */
        public static final FunctionTag UNDO = new FunctionTag();

        private Tags() { }
    }

    /** The instance configuration parameters */
    protected final ConfigurationParameters config;
    private SimpleObjectProperty<StyledTextModel> model;
    private final SimpleBooleanProperty displayCaretProperty = new SimpleBooleanProperty(this, "displayCaret", true);
    private final SimpleObjectProperty<Duration> caretBlinkPeriod = new SimpleObjectProperty<>(this, "caretBlinkPeriod");
    private final SelectionModel selectionModel = new SingleSelectionModel();
    private SimpleBooleanProperty editableProperty;
    private SimpleObjectProperty<SideDecorator> leftDecorator;
    private SimpleObjectProperty<SideDecorator> rightDecorator;
    private SimpleStyleableObjectProperty<Insets> contentPadding;
    private SimpleBooleanProperty highlightCurrentParagraph;
    private SimpleBooleanProperty useContentWidth;
    private SimpleBooleanProperty useContentHeight;
    private ReadOnlyBooleanWrapper undoable;
    private ReadOnlyBooleanWrapper redoable;
    /** The style handler registry instance. */
    protected static final StyleHandlerRegistry styleHandlerRegistry = initStyleHandlerRegistry();

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
     * Creates an instance with the specified configuration parameters and the model.
     * @param c configuration parameters
     * @param m styled text model, or null to use the default model
     */
    public RichTextArea(ConfigurationParameters c, StyledTextModel m) {
        this.config = c;

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
     * The model can be null, which is treated as an immutable, empty model.
     * @return the model property
     * @defaultValue an instance of {@link EditableRichTextModel}, unless set in the constructor
     */
    public final ObjectProperty<StyledTextModel> modelProperty() {
        if (model == null) {
            model = new SimpleObjectProperty<>(this, "model") {
                @Override
                protected void invalidated() {
                    if (undoable != null) {
                        undoable.unbind();
                        StyledTextModel m = get();
                        if (m != null) {
                            undoable.bind(m.undoableProperty());
                        }
                    }

                    if(redoable != null) {
                        redoable.unbind();
                        StyledTextModel m = get();
                        if (m != null) {
                            redoable.bind(m.redoableProperty());
                        }
                    }
                }
            };
        }
        return model;
    }

    public final void setModel(StyledTextModel m) {
        modelProperty().set(m);
    }

    public final StyledTextModel getModel() {
        return model.get();
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
     *
     * @return the display caret property
     * @defaultValue true
     */
    // TODO StyleableProperty ?
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
     * @defaultValue true
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
     * @defaultValue false
     */
    public final BooleanProperty highlightCurrentParagraphProperty() {
        if (highlightCurrentParagraph == null) {
            highlightCurrentParagraph = new SimpleBooleanProperty(this, "highlightCurrentParagraph", false);
        }
        return highlightCurrentParagraph;
    }

    public final boolean isHighlightCurrentParagraph() {
        if (highlightCurrentParagraph == null) {
            return false;
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
        // or the current paragraph text (text on line xx, blah blah)
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

    /** Defines styleable properties at the class level */
    private static class StyleableProperties {
        private static final CssMetaData<RichTextArea, Insets> CONTENT_PADDING =
            new CssMetaData<>("-fx-content-padding", InsetsConverter.getInstance(), Params.CONTENT_PADDING)
        {
            @Override
            public boolean isSettable(RichTextArea t) {
                return t.contentPadding == null || !t.contentPadding.isBound();
            }

            @Override
            public StyleableProperty<Insets> getStyleableProperty(RichTextArea t) {
                return (StyleableProperty<Insets>)t.contentPaddingProperty();
            }
        };

        private static final CssMetaData<RichTextArea,Boolean> WRAP_TEXT =
            new CssMetaData<>("-fx-wrap-text", StyleConverter.getBooleanConverter(), false)
        {
                @Override
                public boolean isSettable(RichTextArea t) {
                    return !t.wrapText.isBound();
                }

                @Override
                public StyleableProperty<Boolean> getStyleableProperty(RichTextArea t) {
                    return (StyleableProperty<Boolean>)t.wrapTextProperty();
                }
            };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES = RichUtils.combine(
            Control.getClassCssMetaData(),
            CONTENT_PADDING,
            WRAP_TEXT
            // TODO other?
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

    // TODO made package protected for testing (for the shim example)
    VFlow vflow() {
        return RichTextAreaSkinHelper.getVFlow(this);
    }

    /**
     * Finds a text position corresponding to the specified screen coordinates.
     * @param screenX screen x coordinate
     * @param screenY screen y coordinate
     * @return the TextPosition
     */
    // TODO or should it be local to control?
    public TextPos getTextPosition(double screenX, double screenY) {
        Point2D local = vflow().getContentPane().screenToLocal(screenX, screenY);
        return vflow().getTextPosLocal(local.getX(), local.getY());
    }

    /**
     * Determines the caret blink period.
     * <p>
     * The period of 1000 ms is used when this property is set to null.
     *
     * @return the caret blink period property
     * @defaultValue null
     */
    public final ObjectProperty<Duration> caretBlinkPeriodProperty() {
        return caretBlinkPeriod;
    }

    public final void setCaretBlinkPeriod(Duration period) {
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
     * @defaultValue null
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
     * @defaultValue null
     */
    public final ReadOnlyProperty<TextPos> anchorPositionProperty() {
        return selectionModel.anchorPositionProperty();
    }

    /**
     * Tracks the current selection.
     * The value can be null.
     * @return the selection property
     * @defaultValue null
     */
    public final ReadOnlyProperty<SelectionSegment> selectionProperty() {
        return selectionModel.selectionProperty();
    }

    public final SelectionSegment getSelection() {
        return selectionModel.getSelection();
    }

    /**
     * Clears existing selection, if any.
     */
    public void clearSelection() {
        selectionModel.clear();
    }

    /**
     * Moves both the caret and the anchor to the specified position, clearing any existing selection.
     * @param pos the text position
     */
    public void select(TextPos pos) {
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
     * Extends selection from the existing anchor to the specified position.
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

    private StyleResolver resolver() {
        RichTextAreaSkin skin = richTextAreaSkin();
        if (skin != null) {
            return skin.getStyleResolver();
        }
        return null;
    }

    /**
     * Determines whether the preferred width is the same as the content width.
     * When set to true, the horizontal scroll bar is disabled.
     *
     * @return the use content width property
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
     * @return the use content height property
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
     * When selection exists, deletes selected text.  Otherwise, deletes the symbol before the caret.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void backspace() {
        execute(Tags.BACKSPACE);
    }

    /**
     * When selection exists, copies the selected rich text to the clipboard in all formats supported
     * by the model.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void copy() {
        execute(Tags.COPY);
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
     * When selection exists, removes the selected content, placing it into the clipboard.
     * Selection is cleared afterward.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void cut() {
        execute(Tags.CUT);
    }

    /**
     * When selection exists, deletes selected text.  Otherwise, deletes the symbol at the caret.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void delete() {
        execute(Tags.DELETE);
    }

    /**
     * When selection exists, deletes selected paragraphs.  Otherwise, deletes the paragraph at the caret.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void deleteParagraph() {
        execute(Tags.DELETE_PARAGRAPH);
    }

    /**
     * Inserts a line break at the caret.  If selection exists, first deletes the selected text.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void insertLineBreak() {
        execute(Tags.INSERT_LINE_BREAK);
    }

    /**
     * Inserts a tab symbol at the caret.  If selection exists, first deletes the selected text.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void insertTab() {
        execute(Tags.INSERT_TAB);
    }

    /**
     * Moves the caret to after the last character of the text, clearing an existing selection.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void moveDocumentEnd() {
        execute(Tags.MOVE_TO_DOCUMENT_END);
    }

    /**
     * Moves the caret to before the first character of the text, clearing an existing selection.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void moveDocumentStart() {
        execute(Tags.MOVE_TO_DOCUMENT_START);
    }

    /**
     * Moves the caret one visual line down, clearing an existing selection.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void moveDown() {
        execute(Tags.MOVE_DOWN);
    }

    /**
     * Moves the caret left, clearing an existing selection.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void moveLeft() {
        execute(Tags.MOVE_LEFT);
    }

    /**
     * Moves the caret to the end of the paragraph at caret, clearing an existing selection.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void moveParagraphEnd() {
        execute(Tags.MOVE_TO_PARAGRAPH_END);
    }

    /**
     * Moves the caret to the start of the current paragraph, clearing an existing selection.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void moveLineStart() {
        execute(Tags.MOVE_TO_PARAGRAPH_START);
    }

    /**
     * Moves the caret to the next symbol, clearing an existing selection.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void moveRight() {
        execute(Tags.MOVE_RIGHT);
    }

    /**
     * Moves the caret one visual line up, clearing an existing selection.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void moveUp() {
        execute(Tags.MOVE_UP);
    }

    /**
     * Moves the caret to the beginning of previous word in a left-to-right setting
     * (or the beginning of the next word in a right-to-left setting), clearing an existing selection.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void moveLeftWord() {
        execute(Tags.MOVE_WORD_LEFT);
    }

    /**
     * Moves the caret to the beginning of next word in a left-to-right setting
     * (or the beginning of the previous word in a right-to-left setting), clearing an existing selection.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void moveRightWord() {
        execute(Tags.MOVE_WORD_RIGHT);
    }

    /**
     * Moves the caret to the beginning of previous word, clearing an existing selection.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void movePreviousWord() {
        execute(Tags.MOVE_WORD_PREVIOUS);
    }

    /**
     * Moves the caret to the beginning of next word, clearing an existing selection.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void moveNextWord() {
        execute(Tags.MOVE_WORD_NEXT);
    }

    /**
     * Moves the caret to the end of the next word, clearing an existing selection.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void moveEndOfNextWord() {
        execute(Tags.MOVE_WORD_NEXT_END);
    }

    /**
     * Move caret one visual page down, clearing an existing selection.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void pageDown() {
        execute(Tags.PAGE_DOWN);
    }

    /**
     * Move caret one visual page up, clearing an existing selection.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void pageUp() {
        execute(Tags.PAGE_UP);
    }

    /**
     * Pastes the clipboard content at the caret, or, if selection exists, replacing the selected text.
     * This method clears the selection afterward.
     * The model decides the best format to use.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void paste() {
        execute(Tags.PASTE);
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
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void pastePlainText() {
        execute(Tags.PASTE_PLAIN_TEXT);
    }

    /**
     * If possible, redoes the last undone modification. If {@link #isRedoable()} returns
     * false, then calling this method has no effect.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void redo() {
        execute(Tags.REDO);
    }

    /**
     * Selects all the text in the document: the anchor is set at the document start, while the caret is positioned
     * at the end of the document.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void selectAll() {
        execute(Tags.SELECT_ALL);
    }

    /**
     * Extends selection to the start of the document.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void selectToDocumentStart() {
        execute(Tags.SELECT_TO_DOCUMENT_START);
    }

    /**
     * Extends selection to the end of the document.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void selectToDocumentEnd() {
        execute(Tags.SELECT_TO_DOCUMENT_END);
    }

    /**
     * Extends selection one visual text line down.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void selectDown() {
        execute(Tags.SELECT_DOWN);
    }

    /**
     * Extends selection one symbol to the left.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void selectLeft() {
        execute(Tags.SELECT_LEFT);
    }

    /**
     * Extends selection one visible page down.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void selectPageDown() {
        execute(Tags.SELECT_PAGE_DOWN);
    }

    /**
     * Extends selection one visible page up.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void selectPageUp() {
        execute(Tags.SELECT_PAGE_UP);
    }

    /**
     * Selects the current paragraph.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void selectParagraph() {
        execute(Tags.SELECT_PARAGRAPH);
    }

    /**
     * Extends selection one symbol to the right.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void selectRight() {
        execute(Tags.SELECT_RIGHT);
    }

    /**
     * Extends selection one visual text line up.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void selectUp() {
        execute(Tags.SELECT_UP);
    }

    /**
     * Selects a word at the caret position.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void selectWord() {
        execute(Tags.SELECT_WORD);
    }

    /**
     * Moves the caret to the beginning of previous word in a left-to-right setting,
     * or to the beginning of the next word in a right-to-left setting.
     * This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of next word.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void selectLeftWord() {
        execute(Tags.SELECT_WORD_LEFT);
    }

    /**
     * Moves the caret to the beginning of next word in a left-to-right setting,
     * or to the beginning of the previous word in a right-to-left setting.
     * This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of next word.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void selectRightWord() {
        execute(Tags.SELECT_WORD_RIGHT);
    }

    /**
     * Moves the caret to the beginning of next word. This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of next word.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void selectNextWord() {
        execute(Tags.SELECT_WORD_NEXT);
    }

    /**
     * Moves the caret to the beginning of previous word. This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of previous word.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void selectPreviousWord() {
        execute(Tags.SELECT_WORD_PREVIOUS);
    }

    /**
     * Extends selection to the end of the next word.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void selectEndOfNextWord() {
        execute(Tags.SELECT_WORD_NEXT_END);
    }

    /**
     * If possible, undoes the last modification. If {@link #isUndoable()} returns
     * false, then calling this method has no effect.
     * <p>
     * This action can be changed by remapping the default behavior, @see {@link #getInputMap()}.
     */
    public void undo() {
        execute(Tags.UNDO);
    }

    /**
     * The property describes if it's currently possible to undo the latest change of the content that was done.
     * @return the read-only property
     * @defaultValue false
     */
    public final ReadOnlyBooleanProperty undoableProperty() {
        if (undoable == null) {
            undoable = new ReadOnlyBooleanWrapper(this, "undoable", false);
            StyledTextModel m = getModel();
            if (m != null) {
                undoable.bind(m.undoableProperty());
            }
        }
        return undoable.getReadOnlyProperty();
    }

    public final boolean isUndoable() {
        return undoable == null ? false : undoable.get();
    }

    /**
     * The property describes if it's currently possible to redo the latest change of the content that was undone.
     * @return the read-only property
     * @defaultValue false
     */
    public final ReadOnlyBooleanProperty redoableProperty() {
        if (redoable == null) {
            redoable = new ReadOnlyBooleanWrapper(this, "redoable", false);
            StyledTextModel m = getModel();
            if (m != null) {
                redoable.bind(m.redoableProperty());
            }
        }
        return redoable.getReadOnlyProperty();
    }

    public final boolean isRedoable() {
        return redoable == null ? false : redoable.get();
    }

    /**
     * This convenience method returns true when a non-empty selection exists.
     * @return true when an non-empty selection exists
     */
    public final boolean hasNonEmptySelection() {
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
     * Applies the specified style to the selected range.  The specified attributes will be merged, overriding
     * the existing ones.
     * When applying paragraph attributes, the affected range might extend beyond {@code start} and {@code end}
     * to include whole paragraphs.
     * @param start the start of text range
     * @param end the end of text range
     * @param attrs the style attributes to apply
     */
    public void applyStyle(TextPos start, TextPos end, StyleAttrs attrs) {
        if (canEdit()) {
            StyledTextModel m = getModel();
            m.applyStyle(start, end, attrs, true);
        }
    }

    /**
     * Sets the specified style to the selected range.
     * All the existing attributes in the selected range will be cleared.
     * When setting the paragraph attributes, the affected range
     * might be wider than one specified.
     * @param start the start of text range
     * @param end the end of text range
     * @param attrs the style attributes to set
     */
    public void setStyle(TextPos start, TextPos end, StyleAttrs attrs) {
        if (canEdit()) {
            StyledTextModel m = getModel();
            m.applyStyle(start, end, attrs, false);
        }
    }

    /**
     * Returns true if this control's {@link #isEditable()} returns true and the model's
     * {@link StyledTextModel#isUserEditable()} also returns true.
     * @return true if model is not null and is editable
     */
    public final boolean canEdit() {
        if (isEditable()) {
            StyledTextModel m = getModel();
            if (m != null) {
                return m.isUserEditable();
            }
        }
        return false;
    }

    private StyleAttrs getModelStyleAttrs(StyleResolver r) {
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
                return m.getStyleAttrs(r, pos);
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
    public final StyleAttrs getActiveStyleAttrs() {
        StyleResolver r = resolver();
        return getModelStyleAttrs(r);
    }

    /**
     * Returns a TextPos corresponding to the end of the document.
     *
     * @return the text position
     */
    public final TextPos getEndTextPos() {
        StyledTextModel m = getModel();
        return (m == null) ? TextPos.ZERO : m.getDocumentEnd();
    }

    /**
     * Returns a TextPos corresponding to the end of paragraph.
     *
     * @param index paragraph index
     * @return text position
     */
    public final TextPos getEndOfParagraph(int index) {
        StyledTextModel m = getModel();
        return (m == null) ? TextPos.ZERO : m.getEndOfParagraphTextPos(index);
    }

    /**
     * Specifies the left-side paragraph decorator.
     * The value can be null.
     * @return the left decorator property
     * @defaultValue null
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
     * @defaultValue null
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
     * A null value is treated as no padding.
     * @return the content padding property
     * @defaultValue 4 pixels vertical padding, 8 pixels horizontal padding
     */
    public final ObjectProperty<Insets> contentPaddingProperty() {
        if (contentPadding == null) {
            contentPadding = new SimpleStyleableObjectProperty<Insets>(
                StyleableProperties.CONTENT_PADDING,
                this,
                "contentPadding",
                Params.CONTENT_PADDING
            );
        }
        return contentPadding;
    }

    public final void setContentPadding(Insets value) {
        contentPaddingProperty().set(value);
    }

    public final Insets getContentPadding() {
        if (contentPadding == null) {
            return Insets.EMPTY;
        }
        return contentPadding.get();
    }

    // TODO to be moved to Control JDK-8314968
    private final InputMap inputMap = new InputMap(this);

    /**
     * Returns the input map instance.
     * @return the input map instance
     */
    // TODO to be moved to Control JDK-8314968
    public InputMap getInputMap() {
        return inputMap;
    }

    /**
     * Executes the function tag, if any.
     * @param tag the function tag.
     */
    // TODO to be moved to Control JDK-8314968
    protected final void execute(FunctionTag tag) {
        FunctionHandler<RichTextArea> f = getInputMap().getFunction(tag);
        if (f != null) {
            f.handle(this);
        }
    }

    /**
     * Returns the style handler registry for this control.
     * @return the style handler registry
     */
    public StyleHandlerRegistry getStyleHandlerRegistry() {
        return styleHandlerRegistry;
    }

    private static StyleHandlerRegistry initStyleHandlerRegistry() {
        StyleHandlerRegistry.Builder b = StyleHandlerRegistry.builder(null);

        b.setParHandler(StyleAttrs.BACKGROUND, (c, cx, v) -> {
            String color = RichUtils.toCssColor(v);
            cx.addStyle("-fx-background-color:" + color + ";");
        });

        b.setSegHandler(StyleAttrs.BOLD, (c, cx, v) -> {
            cx.addStyle(v ? "-fx-font-weight:bold;" : "-fx-font-weight:normal;");
        });

        b.setSegHandler(CssStyles.CSS, (c, cx, v) -> {
            String st = v.style();
            if (st != null) {
                cx.addStyle(st);
            }
            String[] names = v.names();
            if (names != null) {
                cx.getNode().getStyleClass().addAll(names);
            }
        });

        b.setSegHandler(StyleAttrs.FONT_FAMILY, (c, cx, v) -> {
            cx.addStyle("-fx-font-family:'" + v + "';");
        });

        b.setSegHandler(StyleAttrs.FONT_SIZE, (c, cx, v) -> {
            cx.addStyle("-fx-font-size:" + v + ";");
        });

        b.setSegHandler(StyleAttrs.ITALIC, (c, cx, v) -> {
            if (v) {
                cx.addStyle("-fx-font-style:italic;");
            }
        });

        b.setParHandler(StyleAttrs.LINE_SPACING, (c, cx, v) -> {
            cx.addStyle("-fx-line-spacing:" + v + ";");
        });

        b.setParHandler(StyleAttrs.RIGHT_TO_LEFT, (ctrl, cx, v) -> {
            if (ctrl.isWrapText()) {
                // node orientation property is not styleable (yet?)
                cx.getNode().setNodeOrientation(v ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
            }
        });

        // this is a special case: 4 attributes merged into one -fx style
        // unfortunately, this might create multiple copies of the same style string
        StyleAttributeHandler<RichTextArea, Double> spaceHandler = (c, cx, v) -> {
            StyleAttrs a = cx.getAttributes();
            double top = a.getDouble(StyleAttrs.SPACE_ABOVE, 0);
            double right = a.getDouble(StyleAttrs.SPACE_RIGHT, 0);
            double bottom = a.getDouble(StyleAttrs.SPACE_BELOW, 0);
            double left = a.getDouble(StyleAttrs.SPACE_LEFT, 0);
            cx.addStyle("-fx-padding:" + top + ' ' + right + ' ' + bottom + ' ' + left + ";");
        };
        b.setParHandler(StyleAttrs.SPACE_ABOVE, spaceHandler);
        b.setParHandler(StyleAttrs.SPACE_RIGHT, spaceHandler);
        b.setParHandler(StyleAttrs.SPACE_BELOW, spaceHandler);
        b.setParHandler(StyleAttrs.SPACE_LEFT, spaceHandler);

        b.setSegHandler(StyleAttrs.STRIKE_THROUGH, (c, cx, v) -> {
            if (v) {
                cx.addStyle("-fx-strikethrough:true;");
            }
        });

        b.setParHandler(StyleAttrs.TEXT_ALIGNMENT, (c, cx, v) -> {
            if (c.isWrapText()) {
                String alignment = RichUtils.toCss(v);
                cx.addStyle("-fx-text-alignment:" + alignment + ";");
            }
        });

        b.setSegHandler(StyleAttrs.TEXT_COLOR, (c, cx, v) -> {
            String color = RichUtils.toCssColor(v);
            cx.addStyle("-fx-fill:" + color + ";");
        });

        b.setSegHandler(StyleAttrs.UNDERLINE, (cc, cx, v) -> {
            if (v) {
                cx.addStyle("-fx-underline:true;");
            }
        });

        return b.build();
    }

    /**
     * Appends the styled text to the end of the document.
     * This method is no-op if either the control or the model is not editable.
     *
     * @param text the text to append
     * @param attrs the style attributes
     * @return the text position at the end of the appended text, or null if editing is disabled
     */
    public TextPos appendText(String text, StyleAttrs attrs) {
        TextPos p = getEndTextPos();
        return insertText(p, text, attrs);
    }

    /**
     * Appends the styled content to the end of the document.
     * This method is no-op if either the control or the model is not editable.
     *
     * @param in the input stream
     * @return the text position at the end of the appended text, or null if editing is disabled
     */
    public TextPos appendText(StyledInput in) {
        TextPos p = getEndTextPos();
        return insertText(p, in);
    }

    /**
     * Inserts the styled text at the specified position.
     * This method is no-op if either the control or the model is not editable.
     *
     * @param pos the insert position
     * @param text the text to inser
     * @param attrs the style attributes
     * @return the text position at the end of the appended text, or null if editing is disabled
     */
    public TextPos insertText(TextPos pos, String text, StyleAttrs attrs) {
        StyledInput in = StyledInput.of(text, attrs);
        return replaceText(pos, pos, in, true);
    }

    /**
     * Inserts the content at the specified position.
     * This method is no-op if either the control or the model is not editable.
     *
     * @param pos the insert position
     * @param in the input stream
     * @return the text position at the end of the appended text, or null if editing is disabled
     */
    public TextPos insertText(TextPos pos, StyledInput in) {
        return replaceText(pos, pos, in, true);
    }

    /**
     * Replaces the specified range with the new input.
     *
     * @param start the start text position
     * @param end the end text position
     * @param in the input stream
     * @param createUndo when true, creates an undo-redo entry
     * @return the new caret position at the end of inserted text, or null if the change cannot be made
     */
    public TextPos replaceText(TextPos start, TextPos end, StyledInput in, boolean createUndo) {
        if (canEdit()) {
            StyledTextModel m = getModel();
            return m.replace(vflow(), start, end, in, createUndo);
        }
        return null;
    }

    /**
     * Replaces the specified range with the new text.
     *
     * @param start the start text position
     * @param end the end text position
     * @param text the input text
     * @param allowUndo when true, creates an undo-redo entry
     * @return the new caret position at the end of inserted text, or null if the change cannot be made
     */
    public TextPos replaceText(TextPos start, TextPos end, String text, boolean allowUndo) {
        if (canEdit()) {
            StyledTextModel m = getModel();
            return m.replace(vflow(), start, end, text, allowUndo);
        }
        return null;
    }

    /**
     * Clears the undo-redo stack of the underlying model.
     * This method does nothing if the model is null.
     */
    public void clearUndoRedo() {
        StyledTextModel m = getModel();
        if (m != null) {
            m.clearUndoRedo();
        }
    }

    /**
     * Writes the content the output stream using the model's highest priority {@code DataFormat}.
     * This method does not close the output stream.
     * @param out the output stream
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException when no suitable data format can be found
     */
    public void write(OutputStream out) throws IOException {
        DataFormat f = bestDataFormat(true);
        if (f == null) {
            throw new UnsupportedOperationException("no suitable format can be found");
        }
        write(f, out);
    }

    /**
     * Writes the content the output stream using the specified {@code DataFormat}.
     * This method does not close the output stream.
     * @param f the data format
     * @param out the output stream
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException when the data format is not supported by the model
     */
    public void write(DataFormat f, OutputStream out) throws IOException {
        StyledTextModel m = getModel();
        if (m != null) {
            StyleResolver r = resolver();
            m.write(r, f, out);
        }
    }

    /**
     * Reads the content using the model's highest priority {@code DataFormat}.
     * Any existing content is discarded and undo/redo buffer is cleared.
     * This method does not close the input stream.
     * @param in the input stream
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException when the data format is not supported by the model
     */
    public void read(InputStream in) throws IOException {
        DataFormat f = bestDataFormat(false);
        if (f != null) {
            read(f, in);
        }
    }

    /**
     * Reads the content using the specified {@code DataFormat}.
     * Any existing content is discarded and undo/redo buffer is cleared.
     * This method does not close the input stream.
     * @param f the data format
     * @param in the input stream
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException when the data format is not supported by the model
     */
    public void read(DataFormat f, InputStream in) throws IOException {
        StyledTextModel m = getModel();
        if (m != null) {
            StyleResolver r = resolver();
            m.read(r, f, in);
            select(TextPos.ZERO, TextPos.ZERO);
        }
    }

    private DataFormat bestDataFormat(boolean forExport) {
        StyledTextModel m = getModel();
        if (m != null) {
            DataFormat[] fs = m.getSupportedDataFormats(forExport);
            if (fs.length > 0) {
                return fs[0];
            }
        }
        return null;
    }

    /**
     * Clears the text.
     * This method delegates to {@link #replaceText(TextPos, TextPos, StyledInput, boolean)} and creates
     * a redo entry.
     * This method is no-op if either the control or the model is not editable.
     */
    public void clear() {
        TextPos end = getEndTextPos();
        replaceText(TextPos.ZERO, end, StyledInput.EMPTY, true);
    }
}
