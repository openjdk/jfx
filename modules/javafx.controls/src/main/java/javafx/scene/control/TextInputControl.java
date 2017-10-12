/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import com.sun.javafx.scene.control.FormatterAccessor;
import javafx.beans.DefaultProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.css.CssMetaData;
import javafx.css.FontCssMetaData;
import javafx.css.PseudoClass;
import javafx.css.StyleOrigin;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Font;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.javafx.util.Utils;
import com.sun.javafx.binding.ExpressionHelper;
import com.sun.javafx.scene.NodeHelper;
import javafx.util.StringConverter;

/**
 * Abstract base class for text input controls.
 * @since JavaFX 2.0
 */
@DefaultProperty("text")
public abstract class TextInputControl extends Control {
    /**
     * Interface representing a text input's content. Since it is an ObservableStringValue,
     * you can also bind to, or observe the content.
     * @since JavaFX 2.0
     */
    protected interface Content extends ObservableStringValue {
        /**
         * Retrieves a subset of the content.
         *
         * @param start the start
         * @param end the end
         * @return a subset of the content
         */
        public String get(int start, int end);

        /**
         * Inserts a sequence of characters into the content.
         *
         * @param index the index
         * @param text the text string
         * @param notifyListeners the notify listener flag
         * @since JavaFX 2.1
         */
        public void insert(int index, String text, boolean notifyListeners);

        /**
         * Removes a sequence of characters from the content.
         *
         * @param start the start
         * @param end the end
         * @param notifyListeners the notify listener flag
         * @since JavaFX 2.1
         */
        public void delete(int start, int end, boolean notifyListeners);

        /**
         * Returns the number of characters represented by the content.
         * @return the number of characters
         */
        public int length();
    }

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new TextInputControl. The content is an immutable property and
     * must be specified (as non-null) at the time of construction.
     *
     * @param content a non-null implementation of Content.
     */
    protected TextInputControl(final Content content) {
        this.content = content;

        // Add a listener so that whenever the Content is changed, we notify
        // listeners of the text property that it is invalid.
        content.addListener(observable -> {
            if (content.length() > 0) {
                text.textIsNull = false;
            }
            text.controlContentHasChanged();
        });

        // Bind the length to be based on the length of the text property
        length.bind(new IntegerBinding() {
            { bind(text); }
            @Override protected int computeValue() {
                String txt = text.get();
                return txt == null ? 0 : txt.length();
            }
        });

        // Bind the selected text to be based on the selection and text properties
        selectedText.bind(new StringBinding() {
            { bind(selection, text); }
            @Override protected String computeValue() {
                String txt = text.get();
                IndexRange sel = selection.get();
                if (txt == null || sel == null) return "";

                int start = sel.getStart();
                int end = sel.getEnd();
                int length = txt.length();
                if (end > start + length) end = length;
                if (start > length-1) start = end = 0;
                return txt.substring(start, end);
            }
        });

        focusedProperty().addListener((ob, o, n) -> {
            if (n) {
                if (getTextFormatter() != null) {
                    updateText(getTextFormatter());
                }
            } else {
                commitValue();
            }
        });

        // Specify the default style class
        getStyleClass().add("text-input");
    }

    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * The default font to use for text in the TextInputControl. If the TextInputControl's text is
     * rich text then this font may or may not be used depending on the font
     * information embedded in the rich text, but in any case where a default
     * font is required, this font will be used.
     * @return the font property
     * @since JavaFX 8.0
     */
    public final ObjectProperty<Font> fontProperty() {
        if (font == null) {
            font = new StyleableObjectProperty<Font>(Font.getDefault()) {


                private boolean fontSetByCss = false;

                @Override
                public void applyStyle(StyleOrigin newOrigin, Font value) {

                    //
                    // RT-20727 - if CSS is setting the font, then make sure invalidate doesn't call NodeHelper.reapplyCSS
                    //
                    try {
                        // super.applyStyle calls set which might throw if value is bound.
                        // Have to make sure fontSetByCss is reset.
                        fontSetByCss = true;
                        super.applyStyle(newOrigin, value);
                    } catch(Exception e) {
                        throw e;
                    } finally {
                        fontSetByCss = false;
                    }

                }


                @Override
                public void set(Font value) {
                    final Font oldValue = get();
                    if (value == null ? oldValue == null : value.equals(oldValue)) {
                        return;
                    }
                    super.set(value);
                }

                @Override
                protected void invalidated() {
                    // RT-20727 - if font is changed by calling setFont, then
                    // css might need to be reapplied since font size affects
                    // calculated values for styles with relative values
                    if(fontSetByCss == false) {
                        NodeHelper.reapplyCSS(TextInputControl.this);
                    }
                }

                @Override
                public CssMetaData<TextInputControl,Font> getCssMetaData() {
                    return StyleableProperties.FONT;
                }

                @Override
                public Object getBean() {
                    return TextInputControl.this;
                }

                @Override
                public String getName() {
                    return "font";
                }
            };
        }
        return font;
    }

    private ObjectProperty<Font> font;
    public final void setFont(Font value) { fontProperty().setValue(value); }
    public final Font getFont() { return font == null ? Font.getDefault() : font.getValue(); }

    /**
     * The prompt text to display in the {@code TextInputControl}. If set to null or an empty string, no
     * prompt text is displayed.
     *
     * @defaultValue An empty String
     * @since JavaFX 2.2
     */
    private StringProperty promptText = new SimpleStringProperty(this, "promptText", "") {
        @Override protected void invalidated() {
            // Strip out newlines
            String txt = get();
            if (txt != null && txt.contains("\n")) {
                txt = txt.replace("\n", "");
                set(txt);
            }
        }
    };
    public final StringProperty promptTextProperty() { return promptText; }
    public final String getPromptText() { return promptText.get(); }
    public final void setPromptText(String value) { promptText.set(value); }


    /**
     * The property contains currently attached {@link TextFormatter}.
     * Since the value is part of the {@code Formatter}, changing the TextFormatter will update the text based on the new textFormatter.
     *
     * @defaultValue null
     * @since JavaFX 8u40
     */
    private final ObjectProperty<TextFormatter<?>> textFormatter = new ObjectPropertyBase<TextFormatter<?>>() {

        private TextFormatter<?> oldFormatter = null;

        @Override
        public Object getBean() {
            return TextInputControl.this;
        }

        @Override
        public String getName() {
            return "textFormatter";
        }

        @Override
        protected void invalidated() {
            final TextFormatter<?> formatter = get();
            try {
                if (formatter != null) {
                    try {
                        formatter.bindToControl(f -> updateText(f));
                    } catch (IllegalStateException e) {
                        if (isBound()) {
                            unbind();
                        }
                        set(null);
                        throw e;
                    }
                    if (!isFocused()) {
                        updateText(get());
                    }
                }

                if (oldFormatter != null) {
                    oldFormatter.unbindFromControl();
                }
            } finally {
                oldFormatter = formatter;
            }
        }
    };
    public final ObjectProperty<TextFormatter<?>> textFormatterProperty() { return textFormatter; }
    public final TextFormatter<?> getTextFormatter() { return textFormatter.get(); }
    public final void setTextFormatter(TextFormatter<?> value) { textFormatter.set(value); }

    private final Content content;
    /**
     * Returns the text input's content model.
     * @return the text input's content model
     */
    protected final Content getContent() {
        return content;
    }

    /**
     * The textual content of this TextInputControl.
     */
    private TextProperty text = new TextProperty();
    public final String getText() { return text.get(); }
    public final void setText(String value) { text.set(value); }
    public final StringProperty textProperty() { return text; }

    /**
     * The number of characters in the text input.
     */
    private ReadOnlyIntegerWrapper length = new ReadOnlyIntegerWrapper(this, "length");
    public final int getLength() { return length.get(); }
    public final ReadOnlyIntegerProperty lengthProperty() { return length.getReadOnlyProperty(); }

    /**
     * Indicates whether this TextInputControl can be edited by the user.
     */
    private BooleanProperty editable = new SimpleBooleanProperty(this, "editable", true) {
        @Override protected void invalidated() {
            pseudoClassStateChanged(PSEUDO_CLASS_READONLY, ! get());
        }
    };
    public final boolean isEditable() { return editable.getValue(); }
    public final void setEditable(boolean value) { editable.setValue(value); }
    public final BooleanProperty editableProperty() { return editable; }

    /**
     * The current selection.
     */
    private ReadOnlyObjectWrapper<IndexRange> selection = new ReadOnlyObjectWrapper<IndexRange>(this, "selection", new IndexRange(0, 0));
    public final IndexRange getSelection() { return selection.getValue(); }
    public final ReadOnlyObjectProperty<IndexRange> selectionProperty() { return selection.getReadOnlyProperty(); }

    /**
     * Defines the characters in the TextInputControl which are selected
     */
    private ReadOnlyStringWrapper selectedText = new ReadOnlyStringWrapper(this, "selectedText");
    public final String getSelectedText() { return selectedText.get(); }
    public final ReadOnlyStringProperty selectedTextProperty() { return selectedText.getReadOnlyProperty(); }

    /**
     * The <code>anchor</code> of the text selection.
     * The <code>anchor</code> and <code>caretPosition</code> make up the selection
     * range. Selection must always be specified in terms of begin &lt;= end, but
     * <code>anchor</code> may be less than, equal to, or greater than the
     * <code>caretPosition</code>. Depending on how the user selects text,
     * the anchor might represent the lower or upper bound of the selection.
     */
    private ReadOnlyIntegerWrapper anchor = new ReadOnlyIntegerWrapper(this, "anchor", 0);
    public final int getAnchor() { return anchor.get(); }
    public final ReadOnlyIntegerProperty anchorProperty() { return anchor.getReadOnlyProperty(); }

    /**
     * The current position of the caret within the text.
     * The <code>anchor</code> and <code>caretPosition</code> make up the selection
     * range. Selection must always be specified in terms of begin &lt;= end, but
     * <code>anchor</code> may be less than, equal to, or greater than the
     * <code>caretPosition</code>. Depending on how the user selects text,
     * the caretPosition might represent the lower or upper bound of the selection.
     */
    private ReadOnlyIntegerWrapper caretPosition = new ReadOnlyIntegerWrapper(this, "caretPosition", 0);
    public final int getCaretPosition() { return caretPosition.get(); }
    public final ReadOnlyIntegerProperty caretPositionProperty() { return caretPosition.getReadOnlyProperty(); }

    private UndoRedoChange undoChangeHead = new UndoRedoChange();
    private UndoRedoChange undoChange = undoChangeHead;
    private boolean createNewUndoRecord = false;

    /**
     * The property describes if it's currently possible to undo the latest change of the content that was done.
     * @defaultValue false
     * @since JavaFX 8u40
     */
    private final ReadOnlyBooleanWrapper undoable = new ReadOnlyBooleanWrapper(this, "undoable", false);
    public final boolean isUndoable() { return undoable.get(); }
    public final ReadOnlyBooleanProperty undoableProperty() { return undoable.getReadOnlyProperty(); }


    /**
     * The property describes if it's currently possible to redo the latest change of the content that was undone.
     * @defaultValue false
     * @since JavaFX 8u40
     */
    private final ReadOnlyBooleanWrapper redoable = new ReadOnlyBooleanWrapper(this, "redoable", false);
    public final boolean isRedoable() { return redoable.get(); }
    public final ReadOnlyBooleanProperty redoableProperty() { return redoable.getReadOnlyProperty(); }

    /***************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /**
     * Returns a subset of the text input's content.
     *
     * @param start must be a value between 0 and end - 1.
     * @param end must be less than or equal to the length
     * @return the subset of the text input's content
     */
    public String getText(int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException("The start must be <= the end");
        }

        if (start < 0
            || end > getLength()) {
            throw new IndexOutOfBoundsException();
        }

        return getContent().get(start, end);
    }

    /**
     * Appends a sequence of characters to the content.
     *
     * @param text a non null String
     */
    public void appendText(String text) {
        insertText(getLength(), text);
    }

    /**
     * Inserts a sequence of characters into the content.
     *
     * @param index The location to insert the text.
     * @param text The text to insert.
     */
    public void insertText(int index, String text) {
        replaceText(index, index, text);
    }

    /**
     * Removes a range of characters from the content.
     *
     * @param range The range of text to delete. The range object must not be null.
     *
     * @see #deleteText(int, int)
     */
    public void deleteText(IndexRange range) {
        replaceText(range, "");
    }

    /**
     * Removes a range of characters from the content.
     *
     * @param start The starting index in the range, inclusive. This must be &gt;= 0 and &lt; the end.
     * @param end The ending index in the range, exclusive. This is one-past the last character to
     *            delete (consistent with the String manipulation methods). This must be &gt; the start,
     *            and &lt;= the length of the text.
     */
    public void deleteText(int start, int end) {
        replaceText(start, end, "");
    }

    /**
     * Replaces a range of characters with the given text.
     *
     * @param range The range of text to replace. The range object must not be null.
     * @param text The text that is to replace the range. This must not be null.
     *
     * @see #replaceText(int, int, String)
     */
    public void replaceText(IndexRange range, String text) {
        final int start = range.getStart();
        final int end = start + range.getLength();
        replaceText(start, end, text);
    }

    /**
     * Replaces a range of characters with the given text.
     *
     * @param start The starting index in the range, inclusive. This must be &gt;= 0 and &lt; the end.
     * @param end The ending index in the range, exclusive. This is one-past the last character to
     *            delete (consistent with the String manipulation methods). This must be &gt; the start,
     *            and &lt;= the length of the text.
     * @param text The text that is to replace the range. This must not be null.
     */
    public void replaceText(final int start, final int end, final String text) {
        if (start > end) {
            throw new IllegalArgumentException();
        }

        if (text == null) {
            throw new NullPointerException();
        }

        if (start < 0
            || end > getLength()) {
            throw new IndexOutOfBoundsException();
        }

        if (!this.text.isBound()) {
            final int oldLength = getLength();
            TextFormatter<?> formatter = getTextFormatter();
            TextFormatter.Change change = new TextFormatter.Change(this, getFormatterAccessor(), start, end, text);
            if (formatter != null && formatter.getFilter() != null) {
                change = formatter.getFilter().apply(change);
                if (change == null) {
                    return;
                }
            }

            // Update the content
            updateContent(change, oldLength == 0);

        }
    }

    private void updateContent(TextFormatter.Change change, boolean forceNewUndoRecord) {
        final boolean nonEmptySelection = getSelection().getLength() > 0;
        String oldText = getText(change.start, change.end);
        int adjustmentAmount = replaceText(change.start, change.end, change.text, change.getAnchor(), change.getCaretPosition());
        String newText = getText(change.start, change.start + change.text.length() - adjustmentAmount);
        if (newText.equals(oldText)) {
            // Undo record not required as there is no change in the text.
            return;
        }

        /*
         * A new undo record is created, if
         * 1. createNewUndoRecord is true, currently it is set to true for paste operation
         * 2. Text is selected and a character is typed
         * 3. This is the first operation to be added to undo record
         * 4. forceNewUndoRecord is true, currently it is set to true if there is no text present
         * 5. Space character is typed
         * 6. 2500 milliseconds are elapsed since the undo record was created
         * 7. Cursor position is changed and a character is typed
         * 8. A range of text is replaced programmatically using replaceText()
         * Otherwise, the last undo record is updated or discarded.
         */

        int endOfUndoChange = undoChange == undoChangeHead ? -1 : undoChange.start + undoChange.newText.length();
        boolean isNewSpaceChar = false;
        if (newText.equals(" ")) {
            if (!UndoRedoChange.isSpaceCharSequence()) {
                isNewSpaceChar = true;
                UndoRedoChange.setSpaceCharSequence(true);
            }
        } else {
            UndoRedoChange.setSpaceCharSequence(false);
        }
        if (createNewUndoRecord || nonEmptySelection || endOfUndoChange == -1 || forceNewUndoRecord ||
                isNewSpaceChar || UndoRedoChange.hasChangeDurationElapsed() ||
                (endOfUndoChange != change.start && endOfUndoChange != change.end) || change.end - change.start > 0) {
            undoChange = undoChange.add(change.start, oldText, newText);
        } else if (change.start != change.end && change.text.isEmpty()) {
            // I know I am deleting, and am located at the end of the range of the current undo record
            if (undoChange.newText.length() > 0) {
                undoChange.newText = undoChange.newText.substring(0, change.start - undoChange.start);
                if (undoChange.newText.isEmpty()) {
                    // throw away this undo change record
                    undoChange = undoChange.discard();
                }
            } else {
                if (change.start == endOfUndoChange) {
                    undoChange.oldText += oldText;
                } else { // end == endOfUndoChange
                    undoChange.oldText = oldText + undoChange.oldText;
                    undoChange.start--;
                }
            }
        } else {
            // I know I am adding, and am located at the end of the range of the current undo record
            undoChange.newText += newText;
        }
        updateUndoRedoState();
    }

    /**
     * Transfers the currently selected range in the text to the clipboard,
     * removing the current selection.
     */
    public void cut() {
        copy();
        IndexRange selection = getSelection();
        deleteText(selection.getStart(), selection.getEnd());
    }

    /**
     * Transfers the currently selected range in the text to the clipboard,
     * leaving the current selection.
     */
     public void copy() {
        final String selectedText = getSelectedText();
        if (selectedText.length() > 0) {
            final ClipboardContent content = new ClipboardContent();
            content.putString(selectedText);
            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    /**
     * Transfers the contents in the clipboard into this text,
     * replacing the current selection.  If there is no selection, the contents
     * in the clipboard is inserted at the current caret position.
     */
    public void paste() {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasString()) {
            final String text = clipboard.getString();
            if (text != null) {
                createNewUndoRecord = true;
                try {
                    replaceSelection(text);
                } finally {
                    createNewUndoRecord = false;
                }
            }
        }
    }

    /**
     * Moves the selection backward one char in the text. This may have the
     * effect of deselecting, depending on the location of the anchor relative
     * to the caretPosition. This function effectively just moves the caretPosition.
     */
    public void selectBackward() {
        if (getCaretPosition() > 0 && getLength() > 0) {
            // because the anchor stays put, by moving the caret to the left
            // we ensure that a selection is registered and that it is correct
            if (charIterator == null) {
                charIterator = BreakIterator.getCharacterInstance();
            }
            charIterator.setText(getText());
            selectRange(getAnchor(), charIterator.preceding(getCaretPosition()));
        }
    }

    /**
     * Moves the selection forward one char in the text. This may have the
     * effect of deselecting, depending on the location of the anchor relative
     * to the caretPosition. This function effectively just moves the caret forward.
     */
    public void selectForward() {
        final int textLength = getLength();
        if (textLength > 0 && getCaretPosition() < textLength) {
            if (charIterator == null) {
                charIterator = BreakIterator.getCharacterInstance();
            }
            charIterator.setText(getText());
            selectRange(getAnchor(), charIterator.following(getCaretPosition()));
        }
    }

    /**
     * The break iterator instances for navigation over words and complex characters.
     */
    private BreakIterator charIterator;
    private BreakIterator wordIterator;

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

    private void previousWord(boolean select) {
        final int textLength = getLength();
        final String text = getText();
        if (textLength <= 0) {
            return;
        }

        if (wordIterator == null) {
            wordIterator = BreakIterator.getWordInstance();
        }
        wordIterator.setText(text);

        int pos = wordIterator.preceding(Utils.clamp(0, getCaretPosition(), textLength));

        // Skip the non-word region, then move/select to the beginning of the word.
        while (pos != BreakIterator.DONE &&
               !Character.isLetterOrDigit(text.charAt(Utils.clamp(0, pos, textLength-1)))) {
            pos = wordIterator.preceding(Utils.clamp(0, pos, textLength));
        }

        // move/select
        selectRange(select ? getAnchor() : pos, pos);
    }

    private void nextWord(boolean select) {
        final int textLength = getLength();
        final String text = getText();
        if (textLength <= 0) {
            return;
        }

        if (wordIterator == null) {
            wordIterator = BreakIterator.getWordInstance();
        }
        wordIterator.setText(text);

        int last = wordIterator.following(Utils.clamp(0, getCaretPosition(), textLength-1));
        int current = wordIterator.next();

        // Skip whitespace characters to the beginning of next word, but
        // stop at newline. Then move the caret or select a range.
        while (current != BreakIterator.DONE) {
            for (int p=last; p<=current; p++) {
                char ch = text.charAt(Utils.clamp(0, p, textLength-1));
                // Avoid using Character.isSpaceChar() and Character.isWhitespace(),
                // because they include LINE_SEPARATOR, PARAGRAPH_SEPARATOR, etc.
                if (ch != ' ' && ch != '\t') {
                    if (select) {
                        selectRange(getAnchor(), p);
                    } else {
                        selectRange(p, p);
                    }
                    return;
                }
            }
            last = current;
            current = wordIterator.next();
        }

        // move/select to the end
        if (select) {
            selectRange(getAnchor(), textLength);
        } else {
            end();
        }
    }

    private void endOfNextWord(boolean select) {
        final int textLength = getLength();
        final String text = getText();
        if (textLength <= 0) {
            return;
        }

        if (wordIterator == null) {
            wordIterator = BreakIterator.getWordInstance();
        }
        wordIterator.setText(text);

        int last = wordIterator.following(Utils.clamp(0, getCaretPosition(), textLength));
        int current = wordIterator.next();

        // skip the non-word region, then move/select to the end of the word.
        while (current != BreakIterator.DONE) {
            for (int p=last; p<=current; p++) {
                if (!Character.isLetterOrDigit(text.charAt(Utils.clamp(0, p, textLength-1)))) {
                    if (select) {
                        selectRange(getAnchor(), p);
                    } else {
                        selectRange(p, p);
                    }
                    return;
                }
            }
            last = current;
            current = wordIterator.next();
        }

        // move/select to the end
        if (select) {
            selectRange(getAnchor(), textLength);
        } else {
            end();
        }
    }

    /**
     * Selects all text in the text input.
     */
    public void selectAll() {
        selectRange(0, getLength());
    }

    /**
     * Moves the caret to before the first char of the text. This function
     * also has the effect of clearing the selection.
     */
    public void home() {
        // user wants to go to start
        selectRange(0, 0);
    }

    /**
     * Moves the caret to after the last char of the text. This function
     * also has the effect of clearing the selection.
     */
    public void end() {
        // user wants to go to end
        final int textLength = getLength();
        if (textLength > 0) {
            selectRange(textLength, textLength);
        }
    }

    /**
     * Moves the caret to before the first char of text. This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the
     * caretPosition is moved to before the first char.
     */
    public void selectHome() {
        selectRange(getAnchor(), 0);
    }

    /**
     * Moves the caret to after the last char of text. This does not cause
     * the selection to be cleared. Rather, the anchor stays put and the
     * caretPosition is moved to after the last char.
     */
    public void selectEnd() {
        final int textLength = getLength();
        if (textLength > 0) selectRange(getAnchor(), textLength);
    }

    /**
     * Deletes the character that precedes the current caret position from the
     * text if there is no selection, or deletes the selection if there is one.
     * This function returns true if the deletion succeeded, false otherwise.
     * @return true if the deletion succeeded, false otherwise
     */
    public boolean deletePreviousChar() {
        boolean failed = true;
        if (isEditable() && !isDisabled()) {
            final String text = getText();
            final int dot = getCaretPosition();
            final int mark = getAnchor();
            if (dot != mark) {
                // there is a selection of text to remove
                replaceSelection("");
                failed = false;
            } else if (dot > 0) {
                // The caret is not at the beginning, so remove some characters.
                // Typically you'd only be removing a single character, but
                // in some cases you must remove two depending on the unicode
                // characters
                // Note: Do not use charIterator here, because we do want to
                // break up clusters when deleting backwards.
                int p = Character.offsetByCodePoints(text, dot, -1);
                deleteText(p, dot);
                failed = false;
            }
        }
        return !failed;
    }

    /**
     * Deletes the character that follows the current caret position from the
     * text if there is no selection, or deletes the selection if there is one.
     * This function returns true if the deletion succeeded, false otherwise.
     * @return true if the deletion succeeded, false otherwise
     */
    public boolean deleteNextChar() {
        boolean failed = true;
        if (isEditable() && !isDisabled()) {
            final int textLength = getLength();
            final String text = getText();
            final int dot = getCaretPosition();
            final int mark = getAnchor();
            if (dot != mark) {
                // there is a selection of text to remove
                replaceSelection("");
                failed = false;
            } else if (textLength > 0 && dot < textLength) {
                // The caret is not at the end, so remove some characters.
                // Typically you'd only be removing a single character, but
                // in some cases you must remove two depending on the unicode
                // characters
                if (charIterator == null) {
                    charIterator = BreakIterator.getCharacterInstance();
                }
                charIterator.setText(text);
                int p = charIterator.following(dot);
                deleteText(dot, p);
                failed = false;
            }
        }
        return !failed;
    }

    /**
     * Moves the caret position forward. If there is no selection, then the
     * caret position is moved one character forward. If there is a selection,
     * then the caret position is moved to the end of the selection and
     * the selection cleared.
     */
    public void forward() {
        // user has moved caret to the right
        final int textLength = getLength();
        final int dot = getCaretPosition();
        final int mark = getAnchor();
        if (dot != mark) {
            int pos = Math.max(dot, mark);
            selectRange(pos, pos);
        } else if (dot < textLength && textLength > 0) {
            if (charIterator == null) {
                charIterator = BreakIterator.getCharacterInstance();
            }
            charIterator.setText(getText());
            int pos = charIterator.following(dot);
            selectRange(pos, pos);
        }
        deselect();
    }

    /**
     * Moves the caret position backward. If there is no selection, then the
     * caret position is moved one character backward. If there is a selection,
     * then the caret position is moved to the beginning of the selection and
     * the selection cleared.
     *
     * Note: This function is intended to be used by experts, primarily
     *       by those implementing new Skins or Behaviors. It is not common
     *       for developers or designers to access this function directly.
     */
    public void backward() {
        // user has moved caret to the left
        final int textLength = getLength();
        final int dot = getCaretPosition();
        final int mark = getAnchor();
        if (dot != mark) {
            int pos = Math.min(dot, mark);
            selectRange(pos, pos);
        } else if (dot > 0 && textLength > 0) {
            if (charIterator == null) {
                charIterator = BreakIterator.getCharacterInstance();
            }
            charIterator.setText(getText());
            int pos = charIterator.preceding(dot);
            selectRange(pos, pos);
        }
        deselect();
    }

    /**
     * Positions the caret to the position indicated by {@code pos}. This
     * function will also clear the selection.
     * @param pos the position
     */
    public void positionCaret(int pos) {
        final int p = Utils.clamp(0, pos, getLength());
        selectRange(p, p);
    }

    /**
     * Positions the caret to the position indicated by {@code pos} and extends
     * the selection, if there is one. If there is no selection, then a
     * selection is formed where the anchor is at the current caret position
     * and the caretPosition is moved to pos.
     * @param pos the position
     */
    public void selectPositionCaret(int pos) {
        selectRange(getAnchor(), Utils.clamp(0, pos, getLength()));
    }

    /**
     * Positions the anchor and caretPosition explicitly.
     * @param anchor the anchor
     * @param caretPosition the caretPosition
     */
    public void selectRange(int anchor, int caretPosition) {
        caretPosition = Utils.clamp(0, caretPosition, getLength());
        anchor = Utils.clamp(0, anchor, getLength());

        TextFormatter.Change change = new TextFormatter.Change(this, getFormatterAccessor(), anchor, caretPosition);
        TextFormatter<?> formatter = getTextFormatter();
        if (formatter != null && formatter.getFilter() != null) {
            change = formatter.getFilter().apply(change);
            if (change == null) {
                return;
            }
        }

        updateContent(change, false);
    }

    private void doSelectRange(int anchor, int caretPosition) {
        this.caretPosition.set(Utils.clamp(0, caretPosition, getLength()));
        this.anchor.set(Utils.clamp(0, anchor, getLength()));
        this.selection.set(IndexRange.normalize(getAnchor(), getCaretPosition()));
        notifyAccessibleAttributeChanged(AccessibleAttribute.SELECTION_START);
    }

    /**
     * This function will extend the selection to include the specified pos.
     * This is different from selectPositionCaret in that it does not simply
     * move the caret. Rather, it will reposition the caret and anchor as necessary
     * to ensure that pos becomes the new caret and the far other end of the
     * selection becomes the anchor.
     * @param pos the position
     */
    public void extendSelection(int pos) {
        final int p = Utils.clamp(0, pos, getLength());
        final int dot = getCaretPosition();
        final int mark = getAnchor();
        int start = Math.min(dot, mark);
        int end = Math.max(dot, mark);
        if (p < start) {
            selectRange(end, p);
        } else {
            selectRange(start, p);
        }
    }

    /**
     * Clears the text.
     */
    public void clear() {
        deselect();
        if (!text.isBound()) {
            setText("");
        }
    }

    /**
     * Clears the selection.
     */
    public void deselect() {
        // set the anchor equal to the caret position, which clears the selection
        // while also preserving the caret position
        selectRange(getCaretPosition(), getCaretPosition());
    }

    /**
     * Replaces the selection with the given replacement String. If there is
     * no selection, then the replacement text is simply inserted at the current
     * caret position. If there was a selection, then the selection is cleared
     * and the given replacement text inserted.
     * @param replacement the replacement string
     */
    public void replaceSelection(String replacement) {
        replaceText(getSelection(), replacement);
    }

    /**
     * If possible, undoes the last modification. If {@link #isUndoable()} returns
     * false, then calling this method has no effect.
     * @since JavaFX 8u40
     */
    public final void undo() {
        if (isUndoable()) {
            // Apply reverse change here
            final int start = undoChange.start;
            final String newText = undoChange.newText;
            final String oldText = undoChange.oldText;

            if (newText != null) {
                getContent().delete(start, start + newText.length(), oldText.isEmpty());
            }

            if (oldText != null) {
                getContent().insert(start, oldText, true);
                doSelectRange(start, start + oldText.length());
            } else {
                doSelectRange(start, start + newText.length());
            }

            undoChange = undoChange.prev;
        }
        updateUndoRedoState();
    }

    /**
     * If possible, redoes the last undone modification. If {@link #isRedoable()} returns
     * false, then calling this method has no effect.
     * @since JavaFX 8u40
     */
    public final void redo() {
        if (isRedoable()) {
            // Apply change here
            undoChange = undoChange.next;
            final int start = undoChange.start;
            final String newText = undoChange.newText;
            final String oldText = undoChange.oldText;

            if (oldText != null) {
                getContent().delete(start, start + oldText.length(), newText.isEmpty());
            }

            if (newText != null) {
                getContent().insert(start, newText, true);
                doSelectRange(start + newText.length(), start + newText.length());
            } else {
                doSelectRange(start, start);
            }
        }
        updateUndoRedoState();
        // else beep ?
    }

    // Used by TextArea, although there are probably other better ways of
    // doing this.
    void textUpdated() { }

    private void resetUndoRedoState() {
        undoChange = undoChangeHead;
        undoChange.next = null;
        updateUndoRedoState();
    }

    private void updateUndoRedoState() {
        undoable.set(undoChange != undoChangeHead);
        redoable.set(undoChange.next != null);
    }

    private boolean filterAndSet(String value) {
        // Send the new value through the textFormatter, if one exists.
        TextFormatter<?> formatter = getTextFormatter();
        int length = content.length();
        if (formatter != null && formatter.getFilter() != null && !text.isBound()) {
            TextFormatter.Change change = new TextFormatter.Change(
                    TextInputControl.this, getFormatterAccessor(), 0, length, value, 0, 0);
            change = formatter.getFilter().apply(change);
            if (change == null) {
                return false;
            }
            replaceText(change.start, change.end, change.text, change.getAnchor(), change.getCaretPosition());
        } else {
            replaceText(0, length, value, 0, 0);
        }
        return true;
    }

    /**
     * This is what is ultimately called by every code path that will update
     * the content (except for undo / redo). The input into this method has
     * already run through the textFormatter where appropriate.
     *
     * @param start            The start index into the existing text which
     *                         will be replaced by the new value
     * @param end              The end index into the existing text which will
     *                         be replaced by the new value. As with
     *                         String.replace this is a lastIndex+1 value
     * @param value            The new text value
     * @param anchor           The new selection anchor after the change is made
     * @param caretPosition    The new selection caretPosition after the change
     *                         is made.
     * @return The amount of adjustment made to the end / anchor / caretPosition to
     *         accommodate for subsequent filtering (such as the filtering of
     *         new lines by the TextField)
     */
    private int replaceText(int start, int end, String value, int anchor, int caretPosition) {
        // RT-16566: Need to take into account stripping of chars into the
        // final anchor & caret position
        int length = getLength();
        int adjustmentAmount = 0;
        if (end != start) {
            getContent().delete(start, end, value.isEmpty());
            length -= (end - start);
        }
        if (value != null) {
            getContent().insert(start, value, true);
            adjustmentAmount = value.length() - (getLength() - length);
            anchor -= adjustmentAmount;
            caretPosition -= adjustmentAmount;
        }
        doSelectRange(anchor, caretPosition);
        return adjustmentAmount;
    }

    private <T> void updateText(TextFormatter<T> formatter) {
        T value = formatter.getValue();
        StringConverter<T> converter = formatter.getValueConverter();
        if (converter != null) {
            String text = converter.toString(value);
            if (text == null) text = "";
            replaceText(0, getLength(), text, text.length(), text.length());
        }
    }

    /**
     * Commit the current text and convert it to a value.
     * @since JavaFX 8u40
     */
    public final void commitValue() {
        if (getTextFormatter() != null) {
            getTextFormatter().updateValue(getText());
        }
    }

    /**
     * If the field is currently being edited, this call will set text to the last commited value.
     * @since JavaFX 8u40
     */
    public final void cancelEdit() {
        if (getTextFormatter() != null) {
            updateText(getTextFormatter());
        }
    }

    private FormatterAccessor accessor;

    private FormatterAccessor getFormatterAccessor() {
        if (accessor == null) {
            accessor = new TextInputControlFromatterAccessor();
        }
        return accessor;
    }


    /**
     * A little utility method for stripping out unwanted characters.
     *
     * @param txt
     * @param stripNewlines
     * @param stripTabs
     * @return The string after having the unwanted characters stripped out.
     */
    static String filterInput(String txt, boolean stripNewlines, boolean stripTabs) {
        // Most of the time, when text is inserted, there are no illegal
        // characters. So we'll do a "cheap" check for illegal characters.
        // If we find one, we'll do a longer replace algorithm. In the
        // case of illegal characters, this may at worst be an O(2n) solution.
        // Strip out any characters that are outside the printed range
        if (containsInvalidCharacters(txt, stripNewlines, stripTabs)) {
            StringBuilder s = new StringBuilder(txt.length());
            for (int i=0; i<txt.length(); i++) {
                final char c = txt.charAt(i);
                if (!isInvalidCharacter(c, stripNewlines, stripTabs)) {
                    s.append(c);
                }
            }
            txt = s.toString();
        }
        return txt;
    }

    static boolean containsInvalidCharacters(String txt, boolean newlineIllegal, boolean tabIllegal) {
        for (int i=0; i<txt.length(); i++) {
            final char c = txt.charAt(i);
            if (isInvalidCharacter(c, newlineIllegal, tabIllegal)) return true;
        }
        return false;
    }

    private static boolean isInvalidCharacter(char c, boolean newlineIllegal, boolean tabIllegal) {
        if (c == 0x7F) return true;
        if (c == 0xA) return newlineIllegal;
        if (c == 0x9) return tabIllegal;
        if (c < 0x20) return true;
        return false;
    }

    // It can be bound, in which case we will force it to be an eager
    // binding so that we update the content eagerly
    // It can be bidirectionally bound, which basically will just work
    // If somebody changes the content directly, it will be notified and
    // send an invalidation event.
    private class TextProperty extends StringProperty {
        // This is used only when the property is bound
        private ObservableValue<? extends String> observable = null;
        // Added to the observable when bound
        private InvalidationListener listener = null;
        // Used for event handling
        private ExpressionHelper<String> helper = null;
        // The developer my set the Text property to null. Although
        // the Content must be given an empty String, we must still
        // treat the value as though it were null, so that a subsequent
        // getText() will return null.
        private boolean textIsNull = false;

        @Override public String get() {
            // Since we force eager binding and content is always up to date,
            // we just need to get it from content and not through the binding
            return textIsNull ? null : content.get();
        }

        @Override public void set(String value) {
            if (isBound()) {
                throw new java.lang.RuntimeException("A bound value cannot be set.");
            }
            doSet(value);
            markInvalid();
        }

        /**
         * Called whenever the content on the control has changed (as determined
         * by a listener on the content).
         */
        private void controlContentHasChanged() {
            markInvalid();
            notifyAccessibleAttributeChanged(AccessibleAttribute.TEXT);
        }

        @Override public void bind(ObservableValue<? extends String> observable) {
            if (observable == null) {
                throw new NullPointerException("Cannot bind to null");
            }
            if (!observable.equals(this.observable)) {
                unbind();
                this.observable = observable;
                if (listener == null) {
                    listener = new Listener();
                }
                this.observable.addListener(listener);
                markInvalid();
                doSet(observable.getValue());
            }
        }

        @Override public void unbind() {
            if (observable != null) {
                doSet(observable.getValue());
                observable.removeListener(listener);
                observable = null;
            }
        }

        @Override public boolean isBound() {
            return observable != null;
        }

        @Override public void addListener(InvalidationListener listener) {
            helper = ExpressionHelper.addListener(helper, this, listener);
        }

        @Override public void removeListener(InvalidationListener listener) {
            helper = ExpressionHelper.removeListener(helper, listener);
        }

        @Override public void addListener(ChangeListener<? super String> listener) {
            helper = ExpressionHelper.addListener(helper, this, listener);
        }

        @Override public void removeListener(ChangeListener<? super String> listener) {
            helper = ExpressionHelper.removeListener(helper, listener);
        }

        @Override public Object getBean() {
            return TextInputControl.this;
        }

        @Override public String getName() {
            return "text";
        }

        private void fireValueChangedEvent() {
            ExpressionHelper.fireValueChangedEvent(helper);
        }

        private void markInvalid() {
            fireValueChangedEvent();
        }

        /**
         * doSet is called whenever the setText() method was called directly
         * on the TextInputControl, or when the text property was bound,
         * unbound, or reacted to a binding invalidation. It is *not* called
         * when modifications to the content happened indirectly, such as
         * through the replaceText / replaceSelection methods.
         *
         * @param value The new value
         */
        private void doSet(String value) {
            // Guard against the null value.
            textIsNull = value == null;
            if (value == null) value = "";

            if (!filterAndSet(value)) return;

            if (getTextFormatter() != null) {
                getTextFormatter().updateValue(getText());
            }

            textUpdated();

            // If the programmer has directly manipulated the text property
            // or has it bound up, then we will clear out any modifications
            // from the undo manager as we must suppose that the control is
            // being reused, for example, between forms.
            resetUndoRedoState();
        }

        private class Listener implements InvalidationListener {
            @Override
            public void invalidated(Observable valueModel) {
                // We now need to force it to be eagerly recomputed
                // because we need to push these changes to the
                // content model. Because changing the model ends
                // up calling invalidate and markInvalid, the
                // listeners will all be notified.
                doSet(observable.getValue());
            }
        }
    }

    /**
     * Used to form a linked-list of Undo / Redo changes. Each UndoRedoChange
     * records the old and new text, and the start index. It also has
     * the links to the previous and next Changes in the chain. There
     * are two special UndoRedoChange objects in this chain representing the
     * head and the tail so we can have beforeFirst and afterLast
     * behavior as necessary.
     */
    static class UndoRedoChange {
        static long prevRecordTime;
        static final long CHANGE_DURATION = 2500; // milliseconds
        static boolean spaceCharSequence = false;
        int start;
        String oldText;
        String newText;
        UndoRedoChange prev;
        UndoRedoChange next;

        UndoRedoChange() { }

        public UndoRedoChange add(int start, String oldText, String newText) {
            UndoRedoChange c = new UndoRedoChange();
            c.start = start;
            c.oldText = oldText;
            c.newText = newText;
            c.prev = this;
            next = c;
            prevRecordTime = System.currentTimeMillis();
            return c;
        }

        static boolean hasChangeDurationElapsed() {
            return (System.currentTimeMillis() - prevRecordTime > CHANGE_DURATION) ;
        }

        static void setSpaceCharSequence(boolean value) {
            spaceCharSequence = value;
        }
        static boolean isSpaceCharSequence() {
            return spaceCharSequence;
        }

        public UndoRedoChange discard() {
            prev.next = next;
            return prev;
        }

        // Handy to use when debugging, just put it in undo or redo
        // method or replaceText to see what is happening to the undo
        // history as it occurs.
        void debugPrint() {
            UndoRedoChange c = this;
            System.out.print("[");
            while (c != null) {
                System.out.print(c.toString());
                if (c.next != null) System.out.print(", ");
                c = c.next;
            }
            System.out.println("]");
        }

        @Override public String toString() {
            if (oldText == null && newText == null) {
                return "head";
            }
            if (oldText.isEmpty() && !newText.isEmpty()) {
                return "added '" + newText + "' at index " + start;
            } else if (!oldText.isEmpty() && !newText.isEmpty()) {
                return "replaced '" + oldText + "' with '" + newText + "' at index " + start;
            } else {
                return "deleted '" + oldText + "' at index " + start;
            }
        }
    }

    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/


    private static final PseudoClass PSEUDO_CLASS_READONLY
            = PseudoClass.getPseudoClass("readonly");

    private static class StyleableProperties {
        private static final FontCssMetaData<TextInputControl> FONT =
            new FontCssMetaData<TextInputControl>("-fx-font", Font.getDefault()) {

            @Override
            public boolean isSettable(TextInputControl n) {
                return n.font == null || !n.font.isBound();
            }

            @Override
            public StyleableProperty<Font> getStyleableProperty(TextInputControl n) {
                return (StyleableProperty<Font>)(WritableValue<Font>)n.fontProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(Control.getClassCssMetaData());
            styleables.add(FONT);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses.
     * @since JavaFX 8.0
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     * @since JavaFX 8.0
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }


    /***************************************************************************
     *                                                                         *
     * Accessibility handling                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case TEXT: {
                String accText = getAccessibleText();
                if (accText != null && !accText.isEmpty()) return accText;

                String text = getText();
                if (text == null || text.isEmpty()) {
                    text = getPromptText();
                }
                return text;
            }
            case EDITABLE: return isEditable();
            case SELECTION_START: return getSelection().getStart();
            case SELECTION_END: return getSelection().getEnd();
            case CARET_OFFSET: return getCaretPosition();
            case FONT: return getFont();
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
        switch (action) {
            case SET_TEXT: {
                String value = (String) parameters[0];
                if (value != null) setText(value);
                break;
            }
            case SET_TEXT_SELECTION: {
                Integer start = (Integer) parameters[0];
                Integer end = (Integer) parameters[1];
                if (start != null && end != null) {
                    selectRange(start,  end);
                }
                break;
            }
            default: super.executeAccessibleAction(action, parameters);
        }
    }

    private class TextInputControlFromatterAccessor implements FormatterAccessor {
        @Override
        public int getTextLength() {
            return TextInputControl.this.getLength();
        }

        @Override
        public String getText(int begin, int end) {
            return TextInputControl.this.getText(begin, end);
        }

        @Override
        public int getCaret() {
            return TextInputControl.this.getCaretPosition();
        }

        @Override
        public int getAnchor() {
            return TextInputControl.this.getAnchor();
        }
    }

}
