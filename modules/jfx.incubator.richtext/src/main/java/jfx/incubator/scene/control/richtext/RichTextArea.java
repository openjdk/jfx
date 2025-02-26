/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
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

package jfx.incubator.scene.control.richtext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
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
import javafx.css.converter.DurationConverter;
import javafx.css.converter.InsetsConverter;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Point2D;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Control;
import javafx.scene.input.DataFormat;
import javafx.util.Duration;
import com.sun.jfx.incubator.scene.control.input.InputMapHelper;
import com.sun.jfx.incubator.scene.control.richtext.CssStyles;
import com.sun.jfx.incubator.scene.control.richtext.Params;
import com.sun.jfx.incubator.scene.control.richtext.RTAccessibilityHelper;
import com.sun.jfx.incubator.scene.control.richtext.RichTextAreaSkinHelper;
import com.sun.jfx.incubator.scene.control.richtext.VFlow;
import com.sun.jfx.incubator.scene.control.richtext.util.RichUtils;
import jfx.incubator.scene.control.input.FunctionTag;
import jfx.incubator.scene.control.input.InputMap;
import jfx.incubator.scene.control.richtext.model.RichTextModel;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledInput;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;
import jfx.incubator.scene.control.richtext.skin.RichTextAreaSkin;

/**
 * The RichTextArea control is designed for visualizing and editing rich text that can be styled in a variety of ways.
 *
 * <p>The RichTextArea control has a number of features, including:
 * <ul>
 * <li> {@link StyledTextModel paragraph-oriented model}, up to ~2 billion rows
 * <li> virtualized text cell flow
 * <li> support for text styling with an application stylesheet or {@link StyleAttributeMap inline attributes}
 * <li> support for multiple views connected to the same model
 * <li> {@link SelectionModel single selection}
 * <li> {@link InputMap input map} which allows for easy behavior customization and extension
 * </ul>
 *
 * <h2>Creating a RichTextArea</h2>
 * <p>
 * The following example creates an editable control with the default {@link RichTextModel}:
 * <pre>{@code    RichTextArea textArea = new RichTextArea();
 * }</pre>
 * The methods
 * {@code appendText()}, {@code insertText()}, {@code replaceText()}, {@code applyStyle()},
 * {@code setStyle()}, or {@code clear()} can be used to modify text programmatically:
 * <pre>{@code    // create styles
 *   StyleAttributeMap heading = StyleAttributeMap.builder().setBold(true).setUnderline(true).setFontSize(18).build();
 *   StyleAttributeMap mono = StyleAttributeMap.builder().setFontFamily("Monospaced").build();
 *
 *   RichTextArea textArea = new RichTextArea();
 *   // build the content
 *   textArea.appendText("RichTextArea\n", heading);
 *   textArea.appendText("Example:\nText is ", StyleAttributeMap.EMPTY);
 *   textArea.appendText("monospaced.\n", mono);
 * }</pre>
 * Which results in the following visual representation:
 * <p>
 * <img src="doc-files/RichTextArea.png" alt="Image of the RichTextArea control">
 * </p>
 * <p>
 * A view-only information control requires a different model.  The following example illustrates how to
 * create a model that uses a stylesheet for styling:
 * <pre>{@code
 *     SimpleViewOnlyStyledModel m = new SimpleViewOnlyStyledModel();
 *     // add text segment using CSS style name (requires a stylesheet)
 *     m.addWithStyleNames("RichTextArea ", "HEADER");
 *     // add text segment using inline styles
 *     m.addWithInlineStyle("Demo", "-fx-font-size:200%; -fx-font-weight:bold;");
 *     // add newline
 *     m.nl();
 *
 *     RichTextArea textArea = new RichTextArea(m);
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
 * <tr><td><pre> ├─ {@link RichTextModel}</pre></td><td>Default model for RichTextArea</td></tr>
 * <tr><td><pre> ├─ {@link jfx.incubator.scene.control.richtext.model.BasicTextModel BasicTextModel}</pre></td><td>Unstyled text model</td></tr>
 * <tr><td><pre> │   └─ {@link jfx.incubator.scene.control.richtext.model.CodeTextModel CodeTextModel}</pre></td><td>Default model for CodeArea</td></tr>
 * <tr><td><pre> └─ {@link jfx.incubator.scene.control.richtext.model.StyledTextModelViewOnlyBase StyledTextModelViewOnlyBase}</pre></td><td>Base class for a view-only model (abstract)</td></tr>
 * <tr><td><pre>     └─ {@link jfx.incubator.scene.control.richtext.model.SimpleViewOnlyStyledModel SimpleViewOnlyStyledModel}</pre></td><td>In-memory view-only styled model</td></tr>
 * </table>
 *
 * <h2>Selection</h2>
 * <p>
 * The RichTextArea control maintains a single {@link #selectionProperty() contiguous selection segment}
 * as a part of the {@link SelectionModel}.  Additionally,
 * {@link #anchorPositionProperty()} and {@link #caretPositionProperty()} read-only properties
 * are derived from the {@link #selectionProperty()} for convenience.
 *
 * <h2>Customizing</h2>
 * The RichTextArea control offers some degree of customization that does not require subclassing:
 * <ul>
 * <li>customizing key bindings with the {@link InputMap}
 * <li>setting {@link #leftDecoratorProperty() leftDecorator}
 * and {@link #rightDecoratorProperty() rightDecorator} properties
 * </ul>
 *
 * @since 24
 * @author Andy Goryachev
 * @see StyledTextModel
 */
public class RichTextArea extends Control {
    /**
     * Function tags serve as identifiers of methods that can be customized via the {@code InputMap}.
     * <p>
     * Any method in RichTextArea referenced by one of these tags can be customized by providing
     * a different implementation using {@link InputMap#registerFunction(FunctionTag, FunctionHandler)}.
     * Additionally, a key binding can be customized (added, removed, or replaced) via
     * {@link InputMap#registerKey(jfx.incubator.scene.control.input.KeyBinding, FunctionTag)} or
     * {@link  InputMap#register(jfx.incubator.scene.control.input.KeyBinding, FunctionHandler)}.
     */
    public static class Tag {
        /** Deletes the symbol before the caret. */
        public static final FunctionTag BACKSPACE = new FunctionTag();
        /** Copies selected text to the clipboard. */
        public static final FunctionTag COPY = new FunctionTag();
        /** Cuts selected text and places it to the clipboard. */
        public static final FunctionTag CUT = new FunctionTag();
        /** Deletes symbol at the caret. */
        public static final FunctionTag DELETE = new FunctionTag();
        /** Deletes paragraph at the caret, or selected paragraphs. */
        public static final FunctionTag DELETE_PARAGRAPH = new FunctionTag();
        /** Deletes text from the caret to paragraph start, ignoring selection. */
        public static final FunctionTag DELETE_PARAGRAPH_START = new FunctionTag();
        /** Deletes empty paragraph or text to the end of the next word. */
        public static final FunctionTag DELETE_WORD_NEXT_END = new FunctionTag();
        /** Deletes empty paragraph or text to the start of the next word. */
        public static final FunctionTag DELETE_WORD_NEXT_START = new FunctionTag();
        /** Deletes (multiple) empty paragraphs or text to the beginning of the previous word. */
        public static final FunctionTag DELETE_WORD_PREVIOUS = new FunctionTag();
        /** Clears any existing selection by moving anchor to the caret position. */
        public static final FunctionTag DESELECT = new FunctionTag();
        /** Provides audio and/or visual error feedback. */
        public static final FunctionTag ERROR_FEEDBACK = new FunctionTag();
        /** Focus the next focusable node. */
        public static final FunctionTag FOCUS_NEXT = new FunctionTag();
        /** Focus the previous focusable node. */
        public static final FunctionTag FOCUS_PREVIOUS = new FunctionTag();
        /** Inserts a line break at the caret. */
        public static final FunctionTag INSERT_LINE_BREAK = new FunctionTag();
        /** Moves the caret one visual line down. */
        public static final FunctionTag MOVE_DOWN = new FunctionTag();
        /** Moves the caret one symbol to the left. */
        public static final FunctionTag MOVE_LEFT = new FunctionTag();
        /** Moves the caret to the end of the current paragraph, or, if already there, to the end of the next paragraph. */
        public static final FunctionTag MOVE_PARAGRAPH_DOWN = new FunctionTag();
        /** Moves the caret to the start of the current paragraph, or, if already there, to the start of the previous paragraph. */
        public static final FunctionTag MOVE_PARAGRAPH_UP = new FunctionTag();
        /** Moves the caret one symbol to the right. */
        public static final FunctionTag MOVE_RIGHT = new FunctionTag();
        /** Moves the caret to after the last character of the text. */
        public static final FunctionTag MOVE_TO_DOCUMENT_END = new FunctionTag();
        /** Moves the caret to before the first character of the text. */
        public static final FunctionTag MOVE_TO_DOCUMENT_START = new FunctionTag();
        /** Moves the caret to the end of the visual text line at caret. */
        public static final FunctionTag MOVE_TO_LINE_END = new FunctionTag();
        /** Moves the caret to the beginning of the visual text line at caret. */
        public static final FunctionTag MOVE_TO_LINE_START = new FunctionTag();
        /** Moves the caret to the end of the paragraph at caret. */
        public static final FunctionTag MOVE_TO_PARAGRAPH_END = new FunctionTag();
        /** Moves the caret to the beginning of the paragraph at caret. */
        public static final FunctionTag MOVE_TO_PARAGRAPH_START = new FunctionTag();
        /** Moves the caret one visual text line up. */
        public static final FunctionTag MOVE_UP = new FunctionTag();
        /** Moves the caret one word left (previous word if LTR, next word if RTL). */
        public static final FunctionTag MOVE_WORD_LEFT = new FunctionTag();
        /** Moves the caret to the start of the next word, or next paragraph if at the start of an empty paragraph. */
        public static final FunctionTag MOVE_WORD_NEXT_START = new FunctionTag();
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
        /** Extends selection to the end of the current paragraph, or, if already there, to the end of the next paragraph. */
        public static final FunctionTag SELECT_PARAGRAPH_DOWN = new FunctionTag();
        /** Extends selection to the paragraph end. */
        public static final FunctionTag SELECT_PARAGRAPH_END = new FunctionTag();
        /** Extends selection to the paragraph start. */
        public static final FunctionTag SELECT_PARAGRAPH_START = new FunctionTag();
        /** Extends selection to the start of the current paragraph, or, if already there, to the start of the previous paragraph. */
        public static final FunctionTag SELECT_PARAGRAPH_UP = new FunctionTag();
        /** Extends selection one symbol to the right. */
        public static final FunctionTag SELECT_RIGHT = new FunctionTag();
        /** Extends selection to the end of the document. */
        public static final FunctionTag SELECT_TO_DOCUMENT_END = new FunctionTag();
        /** Extends selection to the start of the document. */
        public static final FunctionTag SELECT_TO_DOCUMENT_START = new FunctionTag();
        /** Extends selection to the end of the visual text line at caret. */
        public static final FunctionTag SELECT_TO_LINE_END = new FunctionTag();
        /** Extends selection to the start of the visual text line at caret. */
        public static final FunctionTag SELECT_TO_LINE_START = new FunctionTag();
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
        /** Inserts a tab symbol at the caret (editable), or transfer focus to the next focusable node. */
        public static final FunctionTag INSERT_TAB = new FunctionTag();
        /** If possible, undoes the last modification. */
        public static final FunctionTag UNDO = new FunctionTag();

        private Tag() { }
    }

    private SimpleObjectProperty<StyledTextModel> model;
    private final SelectionModel selectionModel = new SingleSelectionModel();
    private SimpleBooleanProperty editableProperty;
    private SimpleObjectProperty<SideDecorator> leftDecorator;
    private SimpleObjectProperty<SideDecorator> rightDecorator;
    private ReadOnlyBooleanWrapper undoable;
    private ReadOnlyBooleanWrapper redoable;
    // styleables
    private SimpleStyleableObjectProperty<Duration> caretBlinkPeriod;
    private SimpleStyleableObjectProperty<Insets> contentPadding;
    private StyleableBooleanProperty displayCaret;
    private StyleableBooleanProperty highlightCurrentParagraph;
    private StyleableBooleanProperty useContentHeight;
    private StyleableBooleanProperty useContentWidth;
    private StyleableBooleanProperty wrapText;
    private RTAccessibilityHelper accessibilityHelper;
    // will be moved to Control JDK-8314968
    private final InputMap inputMap = new InputMap(this);

    /** The style handler registry instance, made available for use by subclasses to add support for new style attributes. */
    protected static final StyleHandlerRegistry styleHandlerRegistry = initStyleHandlerRegistry();

    /**
     * Creates the instance with the in-memory model {@link RichTextModel}.
     */
    public RichTextArea() {
        this(new RichTextModel());
    }

    /**
     * Creates the instance using the specified model.
     * <p>
     * Multiple RichTextArea instances can work off a single model.
     *
     * @param model the model
     */
    public RichTextArea(StyledTextModel model) {
        setFocusTraversable(true);
        getStyleClass().add("rich-text-area");
        setAccessibleRole(AccessibleRole.TEXT_AREA);
        setAccessibleRoleDescription("Rich Text Area");

        selectionModel.selectionProperty().addListener((s, old, cur) -> {
            TextPos min0 = old == null ? null : old.getMin();
            TextPos max0 = old == null ? null : old.getMax();
            TextPos min2 = cur == null ? null : cur.getMin();
            TextPos max2 = cur == null ? null : cur.getMax();

            if (accessibilityHelper != null) {
                if (accessibilityHelper.handleSelectionChange(cur)) {
                    notifyAccessibleAttributeChanged(AccessibleAttribute.TEXT);
                }
            }

            if (!Objects.equals(min0, min2)) {
                notifyAccessibleAttributeChanged(AccessibleAttribute.SELECTION_START);
            }

            if (!Objects.equals(max0, max2)) {
                notifyAccessibleAttributeChanged(AccessibleAttribute.SELECTION_END);
            }
        });

        setModel(model);
    }

    // Properties

    /**
     * Tracks the selection anchor position within the document.  This read-only property is derived from
     * {@link #selectionProperty() selection} property.  The value can be null.
     * <p>
     * Setting a {@link SelectionSegment} causes an update to both the anchor and the caret positions.
     * A null selection segment results in both positions to become null, a non-null selection segment sets both to
     * non-null values.
     * <p>
     * Note:
     * {@link #selectionProperty()}, {@link #anchorPositionProperty()}, and {@link #caretPositionProperty()}
     * are logically connected.  When a change occurs, the anchor position is updated first, followed by
     * the caret position, followed by the selection segment.
     *
     * @return the anchor position property
     * @see selectionProperty
     * @see caretPositionProperty
     * @defaultValue null
     */
    public final ReadOnlyProperty<TextPos> anchorPositionProperty() {
        return selectionModel.anchorPositionProperty();
    }

    public final TextPos getAnchorPosition() {
        return anchorPositionProperty().getValue();
    }

    /**
     * Determines the caret blink period.  This property cannot be set to {@code null}.
     * <p>
     * This property can be styled with CSS using {@code -fx-caret-blink-period} name.
     * @implNote The property object implements {@link StyleableProperty} interface.
     *
     * @return the caret blink period property
     * @defaultValue 1000 ms
     */
    public final ObjectProperty<Duration> caretBlinkPeriodProperty() {
        if (caretBlinkPeriod == null) {
            caretBlinkPeriod = new SimpleStyleableObjectProperty<>(
                StyleableProperties.CARET_BLINK_PERIOD,
                this,
                "caretBlinkPeriod",
                Params.DEFAULT_CARET_BLINK_PERIOD
            ) {
                private Duration old;

                @Override
                public void invalidated() {
                    final Duration v = get();
                    if (v == null) {
                        set(old);
                        throw new NullPointerException("cannot set caretBlinkPeriodProperty to null");
                    }
                    old = v;
                }
            };
        }
        return caretBlinkPeriod;
    }

    public final void setCaretBlinkPeriod(Duration period) {
        caretBlinkPeriodProperty().set(period);
    }

    public final Duration getCaretBlinkPeriod() {
        return caretBlinkPeriod == null ? Params.DEFAULT_CARET_BLINK_PERIOD : caretBlinkPeriod.get();
    }

    /**
     * Tracks the caret position within the document.  This read-only property is derived from
     * {@link #selectionProperty() selection} property.  The value can be null.
     * <p>
     * Setting a {@link SelectionSegment} causes an update to both the anchor and the caret positions.
     * A null selection segment results in both positions to become null, a non-null selection segment sets both to
     * non-null values.
     * <p>
     * Note:
     * {@link #selectionProperty()}, {@link #anchorPositionProperty()}, and {@link #caretPositionProperty()}
     * are logically connected.  When a change occurs, the anchor position is updated first, followed by
     * the caret position, followed by the selection segment.
     *
     * @return the caret position property
     * @see selectionProperty
     * @see anchorPositionProperty
     * @defaultValue null
     */
    public final ReadOnlyProperty<TextPos> caretPositionProperty() {
        return selectionModel.caretPositionProperty();
    }

    public final TextPos getCaretPosition() {
        return caretPositionProperty().getValue();
    }

    /**
     * Specifies the padding for the RichTextArea content.
     * The content padding value can be null, which is treated as no padding.
     * <p>
     * This property can be styled with CSS using {@code -fx-content-padding} name.
     * @implNote The property object implements {@link StyleableProperty} interface.
     *
     * @return the content padding property
     * @defaultValue null
     */
    public final ObjectProperty<Insets> contentPaddingProperty() {
        if (contentPadding == null) {
            contentPadding = new SimpleStyleableObjectProperty<Insets>(
                StyleableProperties.CONTENT_PADDING,
                this,
                "contentPadding"
            );
        }
        return contentPadding;
    }

    public final void setContentPadding(Insets value) {
        contentPaddingProperty().set(value);
    }

    public final Insets getContentPadding() {
        return contentPadding == null ? null : contentPadding.get();
    }

    /**
     * This property controls whether caret will be displayed or not.
     * <p>
     * This property can be styled with CSS using {@code -fx-display-caret} name.
     * @implNote The property object implements {@link StyleableProperty} interface.
     *
     * @return the display caret property
     * @defaultValue true
     */
    public final BooleanProperty displayCaretProperty() {
        if (displayCaret == null) {
            displayCaret = new StyleableBooleanProperty(Params.DEFAULT_DISPLAY_CARET) {
                @Override
                public Object getBean() {
                    return RichTextArea.this;
                }

                @Override
                public String getName() {
                    return "displayCaret";
                }

                @Override
                public CssMetaData<RichTextArea, Boolean> getCssMetaData() {
                    return StyleableProperties.DISPLAY_CARET;
                }
            };
        }
        return displayCaret;
    }

    public final void setDisplayCaret(boolean on) {
        displayCaretProperty().set(on);
    }

    public final boolean isDisplayCaret() {
        return displayCaret == null ? Params.DEFAULT_DISPLAY_CARET : displayCaret.get();
    }

    /**
     * Indicates whether this RichTextArea can be edited by the user, provided the model is also writable.
     * Changing the value of this property with a view-only model or a null model has no effect.
     *
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
     * <p>
     * This property can be styled with CSS using {@code -fx-highlight-current-paragraph} name.
     * @implNote The property object implements {@link StyleableProperty} interface.
     *
     * @return the highlight current paragraph property
     * @defaultValue false
     */
    public final BooleanProperty highlightCurrentParagraphProperty() {
        if (highlightCurrentParagraph == null) {
            highlightCurrentParagraph = new StyleableBooleanProperty(Params.DEFAULT_HIGHLIGHT_CURRENT_PARAGRAPH) {
                @Override
                public Object getBean() {
                    return RichTextArea.this;
                }

                @Override
                public String getName() {
                    return "highlightCurrentParagraph";
                }

                @Override
                public CssMetaData<RichTextArea, Boolean> getCssMetaData() {
                    return StyleableProperties.HIGHLIGHT_CURRENT_PARAGRAPH;
                }
            };
        }
        return highlightCurrentParagraph;
    }

    public final boolean isHighlightCurrentParagraph() {
        return highlightCurrentParagraph == null ? Params.DEFAULT_HIGHLIGHT_CURRENT_PARAGRAPH : highlightCurrentParagraph.get();
    }

    public final void setHighlightCurrentParagraph(boolean on) {
        highlightCurrentParagraphProperty().set(on);
    }

    /**
     * Specifies the left-side paragraph decorator.
     * The value can be null.
     *
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
     * Determines the {@link StyledTextModel} to use with this RichTextArea.
     * The model can be null, which results in an empty, uneditable control.
     * <p>
     * Note: Subclasses may impose additional restrictions on the type of the model they require.
     *
     * @return the model property
     * @defaultValue an instance of {@link RichTextModel}
     */
    public final ObjectProperty<StyledTextModel> modelProperty() {
        if (model == null) {
            model = new SimpleObjectProperty<>(this, "model") {
                // TODO does this create a memory leak?  should we bind or weak listen?
                private final StyledTextModel.Listener li = (ch) -> {
                    if (ch.isEdit()) {
                        if (accessibilityHelper != null) {
                            if (accessibilityHelper.handleTextUpdate(ch.getStart(), ch.getEnd())) {
                                // TODO check the timing, may be runLater?
                                notifyAccessibleAttributeChanged(AccessibleAttribute.TEXT);
                            }
                        }
                    }
                };
                private StyledTextModel old;

                @Override
                protected void invalidated() {
                    StyledTextModel m = get();
                    try {
                        validateModel(m);
                    } catch(IllegalArgumentException e) {
                        if (isBound()) {
                            unbind();
                        }
                        set(old);
                        throw e;
                    }

                    if (undoable != null) {
                        undoable.unbind();
                        if (m != null) {
                            undoable.bind(m.undoableProperty());
                        }
                    }

                    if(redoable != null) {
                        redoable.unbind();
                        if (m != null) {
                            redoable.bind(m.redoableProperty());
                        }
                    }

                    if (old != null) {
                        old.removeListener(li);
                    }
                    if (m != null) {
                        m.addListener(li);
                    }
                    old = m;

                    if (accessibilityHelper != null) {
                        accessibilityHelper.handleModelChange();
                    }
                    selectionModel.clear();
                    notifyAccessibleAttributeChanged(AccessibleAttribute.TEXT);
                }
            };
        }
        return model;
    }

    public final void setModel(StyledTextModel m) {
        modelProperty().set(m);
    }

    public final StyledTextModel getModel() {
        return model == null ? null : model.get();
    }

    /**
     * Validates the model property value.
     * The subclass should override this method if it restricts the type of model that is supported,
     * and throw an {@code IllegalArgumentException} if the model is not supported.
     * A {@code null} value should always be acceptable and never generate an exception.
     *
     * @param m the model (can be null)
     */
    protected void validateModel(StyledTextModel m) {
    }

    /**
     * The property describes if it's currently possible to redo the latest change of the content that was undone.
     *
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
        return redoableProperty().get();
    }

    /**
     * Specifies the right-side paragraph decorator.
     * The value can be null.
     *
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
     * Tracks the current selection.  The {@link SelectionSegment} consists of two values - the caret and the anchor
     * positions which may get changed independently.  This property allows for tracking the selection as an single
     * entity.  A null value for the selection segment causes both the caret and the anchor positions to become
     * null.
     * <p>
     * Note:
     * {@link #selectionProperty()}, {@link #anchorPositionProperty()}, and {@link #caretPositionProperty()}
     * are logically connected.  When a change occurs, the anchor position is updated first, followed by
     * the caret position, followed by the selection segment.
     *
     * @return the selection property
     * @see anchorPositionProperty
     * @see caretPositionProperty
     * @defaultValue null
     */
    public final ReadOnlyProperty<SelectionSegment> selectionProperty() {
        return selectionModel.selectionProperty();
    }

    public final SelectionSegment getSelection() {
        return selectionModel.getSelection();
    }

    /**
     * The property describes if it's currently possible to undo the latest change of the content that was done.
     *
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
        return undoableProperty().get();
    }

    /**
     * Determines whether the preferred height is the same as the content height.
     * When set to true, the vertical scroll bar is disabled.
     * <p>
     * This property can be styled with CSS using {@code -fx-use-content-height} name.
     * @implNote The property object implements {@link StyleableProperty} interface.
     *
     * @return the use content height property
     * @defaultValue false
     */
    public final BooleanProperty useContentHeightProperty() {
        if (useContentHeight == null) {
            useContentHeight = new StyleableBooleanProperty(Params.DEFAULT_USE_CONTENT_HEIGHT) {
                @Override
                public Object getBean() {
                    return RichTextArea.this;
                }

                @Override
                public String getName() {
                    return "useContentHeight";
                }

                @Override
                public CssMetaData<RichTextArea, Boolean> getCssMetaData() {
                    return StyleableProperties.USE_CONTENT_HEIGHT;
                }
            };
        }
        return useContentHeight;
    }

    public final boolean isUseContentHeight() {
        return useContentHeight == null ? Params.DEFAULT_USE_CONTENT_HEIGHT : useContentHeight.get();
    }

    public final void setUseContentHeight(boolean on) {
        useContentHeightProperty().set(on);
    }

    /**
     * Determines whether the preferred width is the same as the content width.
     * When set to true, the horizontal scroll bar is disabled.
     * <p>
     * This property can be styled with CSS using {@code -fx-use-content-width} name.
     * @implNote The property object implements {@link StyleableProperty} interface.
     *
     * @return the use content width property
     * @defaultValue false
     */
    public final BooleanProperty useContentWidthProperty() {
        if (useContentWidth == null) {
            useContentWidth = new StyleableBooleanProperty(Params.DEFAULT_USE_CONTENT_WIDTH) {
                @Override
                public Object getBean() {
                    return RichTextArea.this;
                }

                @Override
                public String getName() {
                    return "useContentWidth";
                }

                @Override
                public CssMetaData<RichTextArea, Boolean> getCssMetaData() {
                    return StyleableProperties.USE_CONTENT_WIDTH;
                }
            };
        }
        return useContentWidth;
    }

    public final boolean isUseContentWidth() {
        return useContentWidth == null ? Params.DEFAULT_USE_CONTENT_WIDTH : useContentWidth.get();
    }

    public final void setUseContentWidth(boolean on) {
        useContentWidthProperty().set(on);
    }

    /**
     * Indicates whether text should be wrapped in this RichTextArea.
     * If a run of text exceeds the width of the {@code RichTextArea},
     * then this variable indicates whether the text should wrap onto
     * another line.
     * Setting this property to {@code true} hides the horizontal scroll bar.
     * <p>
     * This property can be styled with CSS using {@code -fx-wrap-text} name.
     * @implNote The property object implements {@link StyleableProperty} interface.
     *
     * @return the wrap text property
     * @defaultValue false
     */
    public final BooleanProperty wrapTextProperty() {
        if (wrapText == null) {
            wrapText = new StyleableBooleanProperty(Params.DEFAULT_WRAP_TEXT) {
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
        }
        return wrapText;
    }

    public final boolean isWrapText() {
        return wrapText == null ? Params.DEFAULT_WRAP_TEXT : wrapText.getValue();
    }

    public final void setWrapText(boolean value) {
        wrapTextProperty().setValue(value);
    }

    // Styleable Properties

    /** Defines styleable properties at the class level */
    private static class StyleableProperties {
        private static final CssMetaData<RichTextArea, Duration> CARET_BLINK_PERIOD =
            new CssMetaData<>("-fx-caret-blink-period", DurationConverter.getInstance())
        {
            @Override
            public boolean isSettable(RichTextArea t) {
                return t.caretBlinkPeriod == null || !t.caretBlinkPeriod.isBound();
            }

            @Override
            public StyleableProperty<Duration> getStyleableProperty(RichTextArea t) {
                return (StyleableProperty<Duration>)t.caretBlinkPeriodProperty();
            }
        };

        private static final CssMetaData<RichTextArea, Insets> CONTENT_PADDING =
            new CssMetaData<>("-fx-content-padding", InsetsConverter.getInstance())
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

        private static final CssMetaData<RichTextArea,Boolean> DISPLAY_CARET =
            new CssMetaData<>("-fx-display-caret", StyleConverter.getBooleanConverter(), Params.DEFAULT_DISPLAY_CARET)
        {
            @Override
            public boolean isSettable(RichTextArea t) {
                return t.displayCaret == null || !t.displayCaret.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(RichTextArea t) {
                return (StyleableProperty<Boolean>)t.displayCaretProperty();
            }
        };

        private static final CssMetaData<RichTextArea,Boolean> HIGHLIGHT_CURRENT_PARAGRAPH =
            new CssMetaData<>("-fx-highlight-current-paragraph", StyleConverter.getBooleanConverter(), Params.DEFAULT_HIGHLIGHT_CURRENT_PARAGRAPH)
        {
            @Override
            public boolean isSettable(RichTextArea t) {
                return t.highlightCurrentParagraph == null || !t.highlightCurrentParagraph.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(RichTextArea t) {
                return (StyleableProperty<Boolean>)t.highlightCurrentParagraphProperty();
            }
        };

        private static final CssMetaData<RichTextArea,Boolean> USE_CONTENT_HEIGHT =
            new CssMetaData<>("-fx-use-content-height", StyleConverter.getBooleanConverter(), Params.DEFAULT_USE_CONTENT_HEIGHT)
        {
            @Override
            public boolean isSettable(RichTextArea t) {
                return t.useContentHeight == null || !t.useContentHeight.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(RichTextArea t) {
                return (StyleableProperty<Boolean>)t.useContentHeightProperty();
            }
        };

        private static final CssMetaData<RichTextArea,Boolean> USE_CONTENT_WIDTH =
            new CssMetaData<>("-fx-use-content-width", StyleConverter.getBooleanConverter(), Params.DEFAULT_USE_CONTENT_WIDTH)
        {
            @Override
            public boolean isSettable(RichTextArea t) {
                return t.useContentWidth == null || !t.useContentWidth.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(RichTextArea t) {
                return (StyleableProperty<Boolean>)t.useContentWidthProperty();
            }
        };

        private static final CssMetaData<RichTextArea,Boolean> WRAP_TEXT =
            new CssMetaData<>("-fx-wrap-text", StyleConverter.getBooleanConverter(), Params.DEFAULT_WRAP_TEXT)
        {
            @Override
            public boolean isSettable(RichTextArea t) {
                return t.wrapText == null || !t.wrapText.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(RichTextArea t) {
                return (StyleableProperty<Boolean>)t.wrapTextProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES = RichUtils.combine(
            Control.getClassCssMetaData(),
            CARET_BLINK_PERIOD,
            CONTENT_PADDING,
            DISPLAY_CARET,
            HIGHLIGHT_CURRENT_PARAGRAPH,
            USE_CONTENT_HEIGHT,
            USE_CONTENT_WIDTH,
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

    // Public Methods

    /**
     * Appends the styled text to the end of the document.  Any embedded {@code "\n"} or {@code "\r\n"}
     * sequences result in a new paragraph being added.
     * <p>
     * It is up to the model to decide whether to accept all, some, or none of the
     * {@link jfx.incubator.scene.control.richtext.model.StyleAttribute StyleAttribute}s.
     *
     * @param text the text to append
     * @param attrs the style attributes
     * @return the text position at the end of the appended text, or null if editing is disabled
     * @throws NullPointerException if the model is {@code null}
     * @throws UnsupportedOperationException if the model is not {@link StyledTextModel#isWritable() writable}
     */
    public final TextPos appendText(String text, StyleAttributeMap attrs) {
        TextPos p = getDocumentEnd();
        return insertText(p, text, attrs);
    }

    /**
     * Appends the styled text to the end of the document.  Any embedded {@code "\n"} or {@code "\r\n"}
     * sequences result in a new paragraph being added.
     * <p>
     * This convenience method is equivalent to calling
     * {@code appendText(text, StyleAttributeMap.EMPTY);}
     *
     * @param text the text to append
     * @return the text position at the end of the appended text, or null if editing is disabled
     * @throws NullPointerException if the model is {@code null}
     * @throws UnsupportedOperationException if the model is not {@link StyledTextModel#isWritable() writable}
     */
    public final TextPos appendText(String text) {
        return appendText(text, StyleAttributeMap.EMPTY);
    }

    /**
     * Appends the styled content to the end of the document.  Any embedded {@code "\n"} or {@code "\r\n"}
     * sequences result in a new paragraph being added.
     *
     * @param in the input stream
     * @return the text position at the end of the appended text, or null if editing is disabled
     * @throws NullPointerException if the model is {@code null}
     * @throws UnsupportedOperationException if the model is not {@link StyledTextModel#isWritable() writable}
     */
    public final TextPos appendText(StyledInput in) {
        TextPos p = getDocumentEnd();
        return insertText(p, in);
    }

    /**
     * Applies the specified style to the selected range.  The specified attributes will be merged, overriding
     * the existing ones.
     * When applying paragraph attributes, the affected range might extend beyond {@code start} and {@code end}
     * to include whole paragraphs.
     *
     * @param start the start of text range
     * @param end the end of text range
     * @param attrs the style attributes to apply
     * @throws NullPointerException if the model is {@code null}
     * @throws UnsupportedOperationException if the model is not {@link StyledTextModel#isWritable() writable}
     */
    public void applyStyle(TextPos start, TextPos end, StyleAttributeMap attrs) {
        StyledTextModel m = getModel();
        m.applyStyle(start, end, attrs, true);
    }

    /**
     * When selection exists, deletes selected text.  Otherwise, deletes the character preceding the caret,
     * possibly breaking up the grapheme clusters.
     * <p>
     * This method does nothing if the control is not editable or the model is not writable,
     * or the model or the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     *
     * @see RichTextArea.Tag#BACKSPACE
     */
    public void backspace() {
        execute(Tag.BACKSPACE);
    }

    /**
     * Clears the document, creating an undo entry.
     *
     * @throws NullPointerException if the model is {@code null}
     * @throws UnsupportedOperationException if the model is not {@link StyledTextModel#isWritable() writable}
     */
    public final void clear() {
        TextPos end = getDocumentEnd();
        replaceText(TextPos.ZERO, end, StyledInput.EMPTY, true);
    }

    /**
     * Clears existing selection, if any.  This method is an alias for {@code getSelectionModel().clear()}.
     */
    public final void clearSelection() {
        selectionModel.clear();
    }

    /**
     * Clears the undo-redo stack of the underlying model.
     * This method does nothing if the model is {@code null}.
     */
    public final void clearUndoRedo() {
        StyledTextModel m = getModel();
        if (m != null) {
            m.clearUndoRedo();
        }
    }

    /**
     * When selection exists, copies the selected rich text to the clipboard in all the formats supported
     * by the model.
     * <p>
     * This method does nothing if the model or the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#COPY
     */
    public void copy() {
        execute(Tag.COPY);
    }

    /**
     * Copies the selected text in the specified format to the clipboard.
     * This method does nothing if no selection exists or when the data format is not supported by the model.
     *
     * @param format the data format to use
     */
    public final void copy(DataFormat format) {
        RichTextAreaSkin skin = richTextAreaSkin();
        if (skin != null) {
            skin.copyText(format);
        }
    }

    /**
     * Transfers the currently selected text to the clipboard,
     * removing the current selection.
     * <p>
     * This method does nothing if the control is not editable or the model is not writable,
     * or the model or the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#CUT
     */
    public void cut() {
        execute(Tag.CUT);
    }

    /**
     * When selection exists, deletes selected text.  Otherwise, deletes the symbol at the caret.
     * When the symbol at the caret is a grapheme cluster, deletes the whole cluster.
     * <p>
     * This method does nothing if the control is not editable or the model is not writable,
     * or the model or the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#DELETE
     */
    public void delete() {
        execute(Tag.DELETE);
    }

    /**
     * When selection exists, deletes selected paragraphs.  Otherwise, deletes the paragraph at the caret.
     * <p>
     * This method does nothing if the control is not editable or the model is not writable,
     * or the model or the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#DELETE_PARAGRAPH
     */
    public void deleteParagraph() {
        execute(Tag.DELETE_PARAGRAPH);
    }

    /**
     * Deletes text from the caret position to the start of the paragraph, ignoring existing selection.
     * <p>
     * This method does nothing if the control is not editable or the model is not writable,
     * or the model or the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#DELETE_PARAGRAPH_START
     */
    public void deleteParagraphStart() {
        execute(Tag.DELETE_PARAGRAPH_START);
    }

    /**
     * Deletes from the caret positon to the end of next word, ignoring existing selection.
     * When the caret is in an empty paragraph, deletes the paragraph.
     * <p>
     * This method does nothing if the control is not editable or the model is not writable,
     * or the model or the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#DELETE_WORD_NEXT_END
     */
    public void deleteWordNextEnd() {
        execute(Tag.DELETE_WORD_NEXT_END);
    }

    /**
     * Deletes from the caret positon to the start of next word, ignoring existing selection.
     * When the caret is in an empty paragraph, deletes the paragraph.
     * <p>
     * This method does nothing if the control is not editable or the model is not writable,
     * or the model or the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#DELETE_WORD_NEXT_START
     */
    public void deleteWordNextStart() {
        execute(Tag.DELETE_WORD_NEXT_START);
    }

    /**
     * Deletes (multiple) empty paragraphs or text from the caret position to the start of the previous word,
     * ignoring existing selection.
     * <p>
     * This method does nothing if the control is not editable or the model is not writable,
     * or the model or the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#DELETE_WORD_PREVIOUS
     */
    public void deleteWordPrevious() {
        execute(Tag.DELETE_WORD_PREVIOUS);
    }

    /**
     * Clears the selected text range by moving anchor to the caret position.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#DESELECT
     */
    public void deselect() {
        execute(Tag.DESELECT);
    }

    /**
     * Provides audio and/or visual error feedback.  The default implementation does nothing.
     *
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#ERROR_FEEDBACK
     */
    public void errorFeedback() {
        execute(Tag.ERROR_FEEDBACK);
    }

    /**
     * Executes a function mapped to the specified function tag.
     * This method does nothing if no function is mapped to the tag, or the function has been unbound.
     *
     * @param tag the function tag
     */
    // TODO to be moved to Control JDK-8314968
    public final void execute(FunctionTag tag) {
        InputMapHelper.execute(this, getInputMap(), tag);
    }

    /**
     * Executes the default function mapped to the specified tag.
     * This method does nothing if no default mapping exists.
     *
     * @param tag the function tag
     */
    // TODO to be moved to Control JDK-8314968
    public final void executeDefault(FunctionTag tag) {
        InputMapHelper.executeDefault(this, getInputMap(), tag);
    }

    /**
     * Extends selection to the specified position.  Internally, this method will normalized the position
     * to be within the document boundaries.
     * Calling this method produces the same result as {@code select(pos, pos)} if no prior selection exists.
     * This method does nothing if the model is null.
     *
     * @param pos the text position
     */
    public final void extendSelection(TextPos pos) {
        StyledTextModel m = getModel();
        if (m != null) {
            selectionModel.extendSelection(m, pos);
        }
    }

    /**
     * Returns {@code StyleAttributeMap} which contains character and paragraph attributes.
     * <p>
     * When selection exists, returns the attributes at the first selected character.
     * <p>
     * When no selection exists, returns the attributes at the character which immediately precedes the caret.
     * When at the beginning of the document, returns the attributes of the first character.
     * If the model uses CSS styles, this method resolves individual attributes (bold, font size, etc.)
     * for this instance of {@code RichTextArea}.
     *
     * @return the non-null {@code StyleAttributeMap} instance
     */
    public final StyleAttributeMap getActiveStyleAttributeMap() {
        StyleResolver r = resolver();
        return getModelStyleAttrs(r);
    }

    /**
     * Returns a TextPos corresponding to the end of the document.
     * When the model is null, returns {@link TextPos#ZERO}.
     *
     * @return the text position
     */
    public final TextPos getDocumentEnd() {
        StyledTextModel m = getModel();
        return (m == null) ? TextPos.ZERO : m.getDocumentEnd();
    }

    /**
     * Returns the input map instance.
     * @return the input map instance
     */
    // to be moved to Control JDK-8314968
    public final InputMap getInputMap() {
        return inputMap;
    }

    /**
     * Returns the number of paragraphs in the model.  This method returns 0 if the model is {@code null}.
     * @return the paragraph count
     */
    public final int getParagraphCount() {
        StyledTextModel m = getModel();
        return (m == null) ? 0 : m.size();
    }

    /**
     * Returns a TextPos corresponding to the end of paragraph.
     * When the model is null, returns {@link TextPos#ZERO}.
     *
     * @param index paragraph index
     * @return text position
     */
    public final TextPos getParagraphEnd(int index) {
        StyledTextModel m = getModel();
        return (m == null) ? TextPos.ZERO : m.getEndOfParagraphTextPos(index);
    }

    /**
     * Returns the plain text at the specified paragraph index.  The value of {@code index} must be between
     * 0 (inclusive) and the value returned by {@link #getParagraphCount()} (exclusive).
     *
     * @param index the paragraph index
     * @return the non-null plain text string
     * @throws IndexOutOfBoundsException if the index is outside of the range supported by the model
     */
    public final String getPlainText(int index) {
        if ((index < 0) || (index >= getParagraphCount())) {
            throw new IndexOutOfBoundsException("index=" + index);
        }
        return getModel().getPlainText(index);
    }

    /**
     * Returns the style handler registry for this control.
     * Applications should not normally call this method as it is intended for use by the skin
     * subclasses.
     *
     * @return the style handler registry
     */
    public StyleHandlerRegistry getStyleHandlerRegistry() {
        return styleHandlerRegistry;
    }

    /**
     * Finds a text position corresponding to the specified screen coordinates.
     * This method returns {@code null} if the specified coordinates are outside of the content area.
     *
     * @param screenX the screen x coordinate
     * @param screenY the screen y coordinate
     * @return the text position, or null
     */
    public final TextPos getTextPosition(double screenX, double screenY) {
        Point2D local = vflow().getContentPane().screenToLocal(screenX, screenY);
        return vflow().getTextPosLocal(local.getX(), local.getY());
    }

    /**
     * This convenience method returns true when a non-empty selection exists.
     *
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
     * Inserts a line break at the caret.  If selection exists, first deletes the selected text.
     * <p>
     * This method does nothing if the control is not editable or the model is not writable,
     * or the model or the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#INSERT_LINE_BREAK
     */
    public void insertLineBreak() {
        execute(Tag.INSERT_LINE_BREAK);
    }

    /**
     * Inserts a tab symbol at the caret.  If selection exists, first deletes the selected text.
     * <p>
     * This method does nothing if the control is not editable or the model is not writable,
     * or the model or the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#INSERT_TAB
     */
    public void insertTab() {
        execute(Tag.INSERT_TAB);
    }

    /**
     * Inserts the styled text at the specified position.  Any embedded {@code "\n"} or {@code "\r\n"}
     * sequences result in a new paragraph being added.
     *
     * @param pos the insert position
     * @param text the text to inser
     * @param attrs the style attributes
     * @return the text position at the end of the appended text, or null if editing is disabled
     * @throws NullPointerException if the model is {@code null}
     * @throws UnsupportedOperationException if the model is not {@link StyledTextModel#isWritable() writable}
     */
    public final TextPos insertText(TextPos pos, String text, StyleAttributeMap attrs) {
        StyledInput in = StyledInput.of(text, attrs);
        return replaceText(pos, pos, in, true);
    }

    /**
     * Inserts the styled content at the specified position.
     *
     * @param pos the insert position
     * @param in the input stream
     * @return the text position at the end of the appended text, or null if editing is disabled
     * @throws NullPointerException if the model is {@code null}
     * @throws UnsupportedOperationException if the model is not {@link StyledTextModel#isWritable() writable}
     */
    public final TextPos insertText(TextPos pos, StyledInput in) {
        return replaceText(pos, pos, in, true);
    }

    /**
     * Moves the caret to after the last character of the text, clearing an existing selection.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#MOVE_TO_DOCUMENT_END
     */
    public void moveDocumentEnd() {
        execute(Tag.MOVE_TO_DOCUMENT_END);
    }

    /**
     * Moves the caret to before the first character of the text, clearing an existing selection.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#MOVE_TO_DOCUMENT_START
     */
    public void moveDocumentStart() {
        execute(Tag.MOVE_TO_DOCUMENT_START);
    }

    /**
     * Moves the caret one visual line down, clearing an existing selection.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#MOVE_DOWN
     */
    public void moveDown() {
        execute(Tag.MOVE_DOWN);
    }

    /**
     * Moves the caret left, clearing an existing selection range.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#MOVE_LEFT
     */
    public void moveLeft() {
        execute(Tag.MOVE_LEFT);
    }

    /**
     * Moves the caret to the end of the visual text line at caret, clearing an existing selection.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#MOVE_TO_LINE_END
     */
    public void moveLineEnd() {
        execute(Tag.MOVE_TO_LINE_END);
    }

    /**
     * Moves the caret to the start of the visual text line at caret, clearing an existing selection.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#MOVE_TO_LINE_START
     */
    public void moveLineStart() {
        execute(Tag.MOVE_TO_LINE_START);
    }

    /**
     * Moves the caret to the end of the current paragraph, or, if already there, to the end of the next paragraph.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#MOVE_PARAGRAPH_DOWN
     */
    public void moveParagraphDown() {
        execute(Tag.MOVE_PARAGRAPH_DOWN);
    }

    /**
     * Moves the caret to the end of the paragraph at caret, clearing an existing selection.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#MOVE_TO_PARAGRAPH_END
     */
    public void moveParagraphEnd() {
        execute(Tag.MOVE_TO_PARAGRAPH_END);
    }

    /**
     * Moves the caret to the start of the current paragraph, clearing an existing selection.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#MOVE_TO_PARAGRAPH_START
     */
    public void moveParagraphStart() {
        execute(Tag.MOVE_TO_PARAGRAPH_START);
    }

    /**
     * Moves the caret to the start of the current paragraph, or, if already there, to the start of the previous paragraph.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#MOVE_PARAGRAPH_UP
     */
    public void moveParagraphUp() {
        execute(Tag.MOVE_PARAGRAPH_UP);
    }

    /**
     * Moves the caret to the next symbol, clearing an existing selection.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#MOVE_RIGHT
     */
    public void moveRight() {
        execute(Tag.MOVE_RIGHT);
    }

    /**
     * Moves the caret one visual line up, clearing an existing selection.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#MOVE_UP
     */
    public void moveUp() {
        execute(Tag.MOVE_UP);
    }

    /**
     * Moves the caret to the beginning of previous word in a left-to-right setting
     * (or the beginning of the next word in a right-to-left setting), clearing an existing selection.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#MOVE_WORD_LEFT
     */
    public void moveWordLeft() {
        execute(Tag.MOVE_WORD_LEFT);
    }

    /**
     * Moves the caret to the end of the next word, clearing an existing selection.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#MOVE_WORD_NEXT_END
     */
    public void moveWordNextEnd() {
        execute(Tag.MOVE_WORD_NEXT_END);
    }

    /**
     * Moves the caret to the start of next word, clearing an existing selection.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#MOVE_WORD_NEXT_START
     */
    public void moveWordNextStart() {
        execute(Tag.MOVE_WORD_NEXT_START);
    }

    /**
     * Moves the caret to the beginning of previous word, clearing an existing selection.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#MOVE_WORD_PREVIOUS
     */
    public void moveWordPrevious() {
        execute(Tag.MOVE_WORD_PREVIOUS);
    }

    /**
     * Moves the caret to the beginning of next word in a left-to-right setting
     * (or the beginning of the previous word in a right-to-left setting), clearing an existing selection.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#MOVE_WORD_RIGHT
     */
    public void moveWordRight() {
        execute(Tag.MOVE_WORD_RIGHT);
    }

    /**
     * Move caret one visual page down, clearing an existing selection.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#PAGE_DOWN
     */
    public void pageDown() {
        execute(Tag.PAGE_DOWN);
    }

    /**
     * Move caret one visual page up, clearing an existing selection.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#PAGE_UP
     */
    public void pageUp() {
        execute(Tag.PAGE_UP);
    }

    /**
     * Pastes the clipboard content at the caret, or, if selection exists, replacing the selected text.
     * This method clears the selection afterward.
     * It is up to the model to pick the best data format to paste.
     * <p>
     * This method does nothing if the control is not editable or the model is not writable,
     * or the model or the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#PASTE
     */
    public void paste() {
        execute(Tag.PASTE);
    }

    /**
     * Pastes the clipboard content at the caret, or, if selection exists, replacing the selected text.
     * <p>
     * This method does nothing if the control is not editable or the model is not writable,
     * or the model is {@code null},
     * or if the specified format is not supported by the model.
     *
     * @param format the data format to use
     */
    public void paste(DataFormat format) {
        RichTextAreaSkin skin = richTextAreaSkin();
        if (skin != null) {
            skin.pasteText(format);
        }
    }

    /**
     * Pastes the plain text clipboard content at the caret, or, if selection exists, replacing the selected text.
     * <p>
     * This method does nothing if the control is not editable or the model is not writable,
     * or the model or the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#PASTE_PLAIN_TEXT
     */
    public final void pastePlainText() {
        execute(Tag.PASTE_PLAIN_TEXT);
    }

    /**
     * Calls the model to replace the current document with the content read from the stream using
     * the specified {@code DataFormat}.
     * Any existing content is discarded and undo/redo buffer is cleared.
     * <p>
     * This method does not close the input stream.  This method does nothing if the model is {@code null}.
     *
     * @param f the data format
     * @param in the input stream
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException when the data format is not supported by the model
     */
    public final void read(DataFormat f, InputStream in) throws IOException {
        StyledTextModel m = getModel();
        if (m != null) {
            StyleResolver r = resolver();
            m.read(r, f, in);
            select(TextPos.ZERO, TextPos.ZERO);
        }
    }

    /**
     * Calls the model to replace the current document with the content read from the input stream.
     * The model picks the best {@code DataFormat} to use based on priority.
     * Any existing content is discarded and undo/redo buffer is cleared.
     * <p>
     * This method does not close the input stream.  This method does nothing if the model is {@code null}.
     *
     * @param in the input stream
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException when the data format is not supported by the model
     */
    public final void read(InputStream in) throws IOException {
        DataFormat f = bestDataFormat(false);
        if (f != null) {
            read(f, in);
        }
    }

    /**
     * If possible, redoes the last undone modification. If {@link #isRedoable()} returns
     * false, then calling this method has no effect.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#REDO
     */
    public void redo() {
        execute(Tag.REDO);
    }

    /**
     * Replaces the specified range with the new text.
     *
     * @param start the start text position
     * @param end the end text position
     * @param text the input text
     * @param allowUndo when true, creates an undo-redo entry
     * @return the new caret position at the end of inserted text, or null if the change cannot be made
     * @throws NullPointerException if the model is {@code null}
     * @throws UnsupportedOperationException if the model is not {@link StyledTextModel#isWritable() writable}
     */
    public final TextPos replaceText(TextPos start, TextPos end, String text, boolean allowUndo) {
        StyledTextModel m = getModel();
        return m.replace(vflow(), start, end, text, allowUndo);
    }

    /**
     * Replaces the specified range with the new input.
     *
     * @param start the start text position
     * @param end the end text position
     * @param in the input stream
     * @param createUndo when true, creates an undo-redo entry
     * @return the new caret position at the end of inserted text, or null if the change cannot be made
     * @throws NullPointerException if the model is {@code null}
     * @throws UnsupportedOperationException if the model is not {@link StyledTextModel#isWritable() writable}
     */
    public final TextPos replaceText(TextPos start, TextPos end, StyledInput in, boolean createUndo) {
        StyledTextModel m = getModel();
        return m.replace(vflow(), start, end, in, createUndo);
    }

    /**
     * Moves both the caret and the anchor to the specified position, clearing any existing selection.
     * This method is equivalent to {@code select(pos, pos)}.
     *
     * @param pos the text position
     */
    public final void select(TextPos pos) {
        select(pos, pos);
    }

    /**
     * Selects the specified range and places the caret at the new position.
     * Both positions will be internally clamped to be within the document boundaries.
     * This method does nothing if the model is null.
     *
     * @param anchor the new selection anchor position
     * @param caret the new caret position
     */
    public final void select(TextPos anchor, TextPos caret) {
        StyledTextModel m = getModel();
        if (m != null) {
            selectionModel.setSelection(m, anchor, caret);
        }
    }

    /**
     * Selects all the text in the document: the anchor is set at the document start, while the caret is positioned
     * at the end of the document.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_ALL
     */
    public void selectAll() {
        execute(Tag.SELECT_ALL);
    }

    /**
     * Extends selection one visual text line down.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_DOWN
     */
    public void selectDown() {
        execute(Tag.SELECT_DOWN);
    }

    /**
     * Extends selection one symbol to the left.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_LEFT
     */
    public void selectLeft() {
        execute(Tag.SELECT_LEFT);
    }

    /**
     * Extends selection one visible page down.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_PAGE_DOWN
     */
    public void selectPageDown() {
        execute(Tag.SELECT_PAGE_DOWN);
    }

    /**
     * Extends selection one visible page up.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_PAGE_UP
     */
    public void selectPageUp() {
        execute(Tag.SELECT_PAGE_UP);
    }

    /**
     * Selects the current paragraph.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_PARAGRAPH
     */
    public void selectParagraph() {
        execute(Tag.SELECT_PARAGRAPH);
    }

    /**
     * Extends selection to the end of the current paragraph, or, if already at the end,
     * to the end of the next paragraph.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_PARAGRAPH_DOWN
     */
    public void selectParagraphDown() {
        execute(Tag.SELECT_PARAGRAPH_DOWN);
    }

    /**
     * Selects from the current position to the paragraph end.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_PARAGRAPH_END
     */
    public void selectParagraphEnd() {
        execute(Tag.SELECT_PARAGRAPH_END);
    }

    /**
     * Selects from the current position to the paragraph start.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_PARAGRAPH_START
     */
    public void selectParagraphStart() {
        execute(Tag.SELECT_PARAGRAPH_START);
    }

    /**
     * Extends selection to the start of the current paragraph, or, if already at the start,
     * to the start of the previous paragraph.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_PARAGRAPH_UP
     */
    public void selectParagraphUp() {
        execute(Tag.SELECT_PARAGRAPH_UP);
    }

    /**
     * Extends selection one symbol (or grapheme cluster) to the right.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_RIGHT
     */
    public void selectRight() {
        execute(Tag.SELECT_RIGHT);
    }

    /**
     * Extends selection to the end of the document.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_TO_DOCUMENT_END
     */
    public void selectToDocumentEnd() {
        execute(Tag.SELECT_TO_DOCUMENT_END);
    }

    /**
     * Extends selection to the start of the document.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_TO_DOCUMENT_START
     */
    public void selectToDocumentStart() {
        execute(Tag.SELECT_TO_DOCUMENT_START);
    }

    /**
     * Extends selection to the end of the visual text line at caret.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_TO_LINE_END
     */
    public void selectToLineEnd() {
        execute(Tag.SELECT_TO_LINE_END);
    }

    /**
     * Extends selection to the start of the visual text line at caret.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_TO_LINE_START
     */
    public void selectToLineStart() {
        execute(Tag.SELECT_TO_LINE_START);
    }

    /**
     * Extends selection one visual text line up.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_UP
     */
    public void selectUp() {
        execute(Tag.SELECT_UP);
    }

    /**
     * Selects a word at the caret position.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_WORD
     */
    public void selectWord() {
        execute(Tag.SELECT_WORD);
    }

    /**
     * Moves the caret to the beginning of previous word in a left-to-right setting,
     * or to the beginning of the next word in a right-to-left setting.
     * This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of next word.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_WORD_LEFT
     */
    public void selectWordLeft() {
        execute(Tag.SELECT_WORD_LEFT);
    }

    /**
     * Extends selection to the end of the next word.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_WORD_NEXT_END
     */
    public void selectWordNextEnd() {
        execute(Tag.SELECT_WORD_NEXT_END);
    }

    /**
     * Moves the caret to the start of the next word. This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of next word.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_WORD_NEXT
     */
    public void selectWordNextStart() {
        execute(Tag.SELECT_WORD_NEXT);
    }

    /**
     * Moves the caret to the beginning of previous word. This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of previous word.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_WORD_PREVIOUS
     */
    public void selectWordPrevious() {
        execute(Tag.SELECT_WORD_PREVIOUS);
    }

    /**
     * Moves the caret to the beginning of next word in a left-to-right setting,
     * or to the beginning of the previous word in a right-to-left setting.
     * This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the caretPosition is
     * moved to the beginning of next word.
     * <p>
     * This method does nothing when the caret position is {@code null}.
     * <p>
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#SELECT_WORD_RIGHT
     */
    public void selectWordRight() {
        execute(Tag.SELECT_WORD_RIGHT);
    }

    /**
     * Sets the specified style to the selected range.
     * All the existing attributes in the selected range will be cleared.
     * When setting the paragraph attributes, the affected range
     * might be wider than one specified.
     *
     * @param start the start of text range
     * @param end the end of text range
     * @param attrs the style attributes to set
     * @throws NullPointerException if the model is {@code null}
     * @throws UnsupportedOperationException if the model is not {@link StyledTextModel#isWritable() writable}
     */
    public final void setStyle(TextPos start, TextPos end, StyleAttributeMap attrs) {
        StyledTextModel m = getModel();
        m.applyStyle(start, end, attrs, false);
    }

    /**
     * If possible, undoes the last modification. If {@link #isUndoable()} returns
     * false, then calling this method has no effect.
     * <p>
     * This method does nothing if the control is not editable or the model is not writable,
     * or the model or the caret position is {@code null}.
     *
     * This action can be changed by remapping the default behavior via {@link InputMap}.
     * @see RichTextArea.Tag#UNDO
     */
    public void undo() {
        execute(Tag.UNDO);
    }

    /**
     * Calls the model to writes the current document to the output stream using the specified {@code DataFormat}.
     * <p>
     * This method does not close the output stream.  This method does nothing if the model is {@code null}.
     *
     * @param f the data format
     * @param out the output stream
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException when the data format is not supported by the model
     */
    public final void write(DataFormat f, OutputStream out) throws IOException {
        StyledTextModel m = getModel();
        if (m != null) {
            StyleResolver r = resolver();
            m.write(r, f, out);
        }
    }

    /**
     * Calls the model to write the current document to the output stream, using the highest priority {@code DataFormat}
     * as determined by the model.
     * <p>
     * This method does not close the output stream.  This method does nothing if the model is {@code null}.
     * @param out the output stream
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException when no suitable data format can be found
     */
    public final void write(OutputStream out) throws IOException {
        if (getModel() != null) {
            DataFormat f = bestDataFormat(true);
            if (f == null) {
                throw new UnsupportedOperationException("no suitable format can be found");
            }
            write(f, out);
        }
    }

    // Non-public Methods

    @Override
    protected RichTextAreaSkin createDefaultSkin() {
        return new RichTextAreaSkin(this);
    }

    // package protected for testing
    VFlow vflow() {
        return RichTextAreaSkinHelper.getVFlow(this);
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

    private StyleAttributeMap getModelStyleAttrs(StyleResolver r) {
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
                    pos = TextPos.ofLeading(pos.index(), ix);
                }
                return m.getStyleAttributeMap(r, pos);
            }
        }
        return StyleAttributeMap.EMPTY;
    }

    private static StyleHandlerRegistry initStyleHandlerRegistry() {
        StyleHandlerRegistry.Builder b = StyleHandlerRegistry.builder(null);

        b.setParHandler(StyleAttributeMap.BACKGROUND, (c, cx, v) -> {
            String color = RichUtils.toCssColor(v);
            cx.addStyle("-fx-background-color:" + color + ";");
        });

        b.setSegHandler(StyleAttributeMap.BOLD, (c, cx, v) -> {
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

        b.setSegHandler(StyleAttributeMap.FONT_FAMILY, (c, cx, v) -> {
            cx.addStyle("-fx-font-family:'" + v + "';");
        });

        b.setSegHandler(StyleAttributeMap.FONT_SIZE, (c, cx, v) -> {
            cx.addStyle("-fx-font-size:" + v + ";");
        });

        b.setSegHandler(StyleAttributeMap.ITALIC, (c, cx, v) -> {
            if (v) {
                cx.addStyle("-fx-font-style:italic;");
            }
        });

        b.setParHandler(StyleAttributeMap.LINE_SPACING, (c, cx, v) -> {
            cx.addStyle("-fx-line-spacing:" + v + ";");
        });

        b.setParHandler(StyleAttributeMap.PARAGRAPH_DIRECTION, (ctrl, cx, v) -> {
            if (ctrl.isWrapText()) {
                // node orientation property is not styleable (yet?)
                switch (v) {
                case LEFT_TO_RIGHT:
                    cx.getNode().setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
                    break;
                case RIGHT_TO_LEFT:
                    cx.getNode().setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                    break;
                }
            }
        });

        // this is a special case: 4 attributes merged into one -fx style
        // unfortunately, this might create multiple copies of the same style string
        StyleAttributeHandler<RichTextArea, Double> spaceHandler = (c, cx, v) -> {
            StyleAttributeMap a = cx.getAttributes();
            double top = a.getDouble(StyleAttributeMap.SPACE_ABOVE, 0);
            double right = a.getDouble(StyleAttributeMap.SPACE_RIGHT, 0);
            double bottom = a.getDouble(StyleAttributeMap.SPACE_BELOW, 0);
            double left = a.getDouble(StyleAttributeMap.SPACE_LEFT, 0);
            cx.addStyle("-fx-padding:" + top + ' ' + right + ' ' + bottom + ' ' + left + ";");
        };
        b.setParHandler(StyleAttributeMap.SPACE_ABOVE, spaceHandler);
        b.setParHandler(StyleAttributeMap.SPACE_RIGHT, spaceHandler);
        b.setParHandler(StyleAttributeMap.SPACE_BELOW, spaceHandler);
        b.setParHandler(StyleAttributeMap.SPACE_LEFT, spaceHandler);

        b.setSegHandler(StyleAttributeMap.STRIKE_THROUGH, (c, cx, v) -> {
            if (v) {
                cx.addStyle("-fx-strikethrough:true;");
            }
        });

        b.setParHandler(StyleAttributeMap.TEXT_ALIGNMENT, (c, cx, v) -> {
            if (c.isWrapText()) {
                String alignment = RichUtils.toCss(v);
                cx.addStyle("-fx-text-alignment:" + alignment + ";");
            }
        });

        b.setSegHandler(StyleAttributeMap.TEXT_COLOR, (c, cx, v) -> {
            String color = RichUtils.toCssColor(v);
            cx.addStyle("-fx-fill:" + color + ";");
        });

        b.setSegHandler(StyleAttributeMap.UNDERLINE, (cc, cx, v) -> {
            if (v) {
                cx.addStyle("-fx-underline:true;");
            }
        });

        return b.build();
    }

    private DataFormat bestDataFormat(boolean forExport) {
        StyledTextModel m = getModel();
        if (m != null) {
            List<DataFormat> fs = m.getSupportedDataFormats(forExport);
            if (fs.size() > 0) {
                return fs.get(0);
            }
        }
        return null;
    }

    private RTAccessibilityHelper accessibilityHelper() {
        if(accessibilityHelper == null) {
            accessibilityHelper = new RTAccessibilityHelper(this);
        }
        return accessibilityHelper;
    }

    @Override
    public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
        //System.out.println("execute: " + action); // FIX
        switch (action) {
        case SET_TEXT:
            {
                String value = (String) parameters[0];
                if (value != null) {
                    // TODO
                    // setText(value);
                }
                return;
            }
        case SET_TEXT_SELECTION:
            {
                Integer start = (Integer) parameters[0];
                Integer end = (Integer) parameters[1];
                if (start != null && end != null) {
                    // TODO
                    // selectRange(start,  end);
                }
                return;
            }
        case SHOW_TEXT_RANGE:
            // TODO
            return;
        default:
            super.executeAccessibleAction(action, parameters);
        }
    }

    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
        case EDITABLE:
            return isEditable();
        case TEXT:
            {
                String accText = getAccessibleText();
                if (accText != null && !accText.isEmpty()) {
                    return accText;
                }
                return accessibilityHelper().getText();
            }
        case SELECTION_START:
            return accessibilityHelper().selectionStart();
        case SELECTION_END:
            return accessibilityHelper().selectionEnd();
        case CARET_OFFSET:
            return accessibilityHelper().caretOffset();
        default:
            return super.queryAccessibleAttribute(attribute, parameters);
        }
    }
}
