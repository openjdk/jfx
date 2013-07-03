/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.DefaultProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
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
import javafx.css.CssMetaData;
import javafx.css.FontCssMetaData;
import javafx.css.PseudoClass;
import javafx.css.StyleOrigin;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Font;
import com.sun.javafx.Utils;
import com.sun.javafx.binding.ExpressionHelper;
import javafx.css.Styleable;

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
         * @param start
         * @param end
         */
        public String get(int start, int end);

        /**
         * Inserts a sequence of characters into the content.
         *
         * @param index
         * @param text
         * @since JavaFX 2.1
         */
        public void insert(int index, String text, boolean notifyListeners);

        /**
         * Removes a sequence of characters from the content.
         *
         * @param start
         * @param end
         * @since JavaFX 2.1
         */
        public void delete(int start, int end, boolean notifyListeners);

        /**
         * Returns the number of characters represented by the content.
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
        content.addListener(new InvalidationListener() {
            @Override public void invalidated(Observable observable) {
                if (content.length() > 0) {
                    text.textIsNull = false;
                }
                text.invalidate();
            }
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
     * @since JavaFX 8.0
     */
    public final ObjectProperty<Font> fontProperty() {
        if (font == null) {
            font = new StyleableObjectProperty<Font>(Font.getDefault()) {


                private boolean fontSetByCss = false;

                @Override
                public void applyStyle(StyleOrigin newOrigin, Font value) {

                    //
                    // RT-20727 - if CSS is setting the font, then make sure invalidate doesn't call impl_reapplyCSS
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
                        TextInputControl.this.impl_reapplyCSS();
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
     * The prompt text to display in the {@code TextInputControl}, or
     * <tt>null</tt> if no prompt text is displayed.
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


    private final Content content;
    /**
     * Returns the text input's content model.
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

    /**
     * This flag is used to indicate that the text on replace trigger should
     * NOT update the caret position. Basically it is a flag we use to
     * indicate that the change to textInputControl.text was from us instead of from
     * the developer. The language being what it is, it is possible that the
     * developer is also bound to textInputControl.text and that they will change the
     * text value before our on replace trigger gets called. We will therefore
     * have to check the caret position against the text to make sure we don't
     * get a caret position out of bounds. But otherwise, we don't update
     * the caret when text is set internally.
     */
    private boolean doNotAdjustCaret = false;

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
     */
    public String getText(int start, int end) {
        // TODO these checks really belong in Content
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
        if (range == null) {
            throw new NullPointerException();
        }

        int start = range.getStart();
        int end = start + range.getLength();

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
    public void replaceText(int start, int end, String text) {
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
            getContent().delete(start, end, text.isEmpty());
            getContent().insert(start, text, true);

            start += text.length();
            selectRange(start, start);
        }
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
                replaceSelection(text);
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
            selectRange(getAnchor(), Character.offsetByCodePoints(getText(), getCaretPosition(), -1));
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
            selectRange(getAnchor(), Character.offsetByCodePoints(getText(), getCaretPosition(), 1));
        }
    }

    /**
     * The break iterator instance.  Right now, it is only used to perform
     * previous/next word navigation.
     */
    private BreakIterator breakIterator;

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

        if (breakIterator == null) {
            breakIterator = BreakIterator.getWordInstance();
        }
        breakIterator.setText(text);

        int pos = breakIterator.preceding(Utils.clamp(0, getCaretPosition(), textLength - 1));

        // Skip the non-word region, then move/select to the beginning of the word.
        while (pos != BreakIterator.DONE &&
               !Character.isLetter(text.charAt(Utils.clamp(0, pos, textLength-1)))) {
            pos = breakIterator.preceding(Utils.clamp(0, pos, textLength-1));
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

        if (breakIterator == null) {
            breakIterator = BreakIterator.getWordInstance();
        }
        breakIterator.setText(text);

        int last = breakIterator.following(Utils.clamp(0, getCaretPosition(), textLength-1));
        int current = breakIterator.next();

        // skip the non-word region, then move/select to the beginning of the word.
        while (current != BreakIterator.DONE) {
            for (int p=last; p<=current; p++) {
                if (Character.isLetter(text.charAt(Utils.clamp(0, p, textLength-1)))) {
                    if (select) {
                        selectRange(getAnchor(), p);
                    } else {
                        selectRange(p, p);
                    }
                    return;
                }
            }
            last = current;
            current = breakIterator.next();
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

        if (breakIterator == null) {
            breakIterator = BreakIterator.getWordInstance();
        }
        breakIterator.setText(text);

        int last = breakIterator.following(Utils.clamp(0, getCaretPosition(), textLength-1));
        int current = breakIterator.next();

        // skip the non-word region, then move/select to the end of the word.
        while (current != BreakIterator.DONE) {
            for (int p=last; p<=current; p++) {
                if (!Character.isLetter(text.charAt(Utils.clamp(0, p, textLength-1)))) {
                    if (select) {
                        selectRange(getAnchor(), p);
                    } else {
                        selectRange(p, p);
                    }
                    return;
                }
            }
            last = current;
            current = breakIterator.next();
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
                int p = Character.offsetByCodePoints(text, dot, -1);
                doNotAdjustCaret = true;
                deleteText(p, dot);
                selectRange(p, p);
                failed = false;
                doNotAdjustCaret = false;
            }
        }
        return !failed;
    }

    /**
     * Deletes the character that follows the current caret position from the
     * text if there is no selection, or deletes the selection if there is one.
     * This function returns true if the deletion succeeded, false otherwise.
     */
    public boolean deleteNextChar() {
        boolean failed = true;
        if (isEditable() && !isDisabled()) {
            final String text = getText();
            final int dot = getCaretPosition();
            final int mark = getAnchor();
            if (dot != mark) {
                // there is a selection of text to remove
                replaceSelection("");
                int newDot = Math.min(dot, mark);
                selectRange(newDot, newDot);
                failed = false;
            } else if (text.length() > 0 && dot < text.length()) {
                // The caret is not at the end, so remove some characters.
                // Typically you'd only be removing a single character, but
                // in some cases you must remove two depending on the unicode
                // characters
                int p = Character.offsetByCodePoints(text, dot, 1);
                doNotAdjustCaret = true;
                //setText(text.substring(0, dot) + text.substring(dot + delChars));
                deleteText(dot, p);
                failed = false;
                doNotAdjustCaret = false;
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
            int pos = Character.offsetByCodePoints(getText(), dot, 1);
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
     * @expert This function is intended to be used by experts, primarily
     *         by those implementing new Skins or Behaviors. It is not common
     *         for developers or designers to access this function directly.
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
            int pos = Character.offsetByCodePoints(getText(), dot, -1);
            selectRange(pos, pos);
        }
        deselect();
    }

    /**
     * Positions the caret to the position indicated by {@code pos}. This
     * function will also clear the selection.
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
     */
    public void selectPositionCaret(int pos) {
        selectRange(getAnchor(), Utils.clamp(0, pos, getLength()));
    }

    /**
     * Positions the anchor and caretPosition explicitly.
     */
    public void selectRange(int anchor, int caretPosition) {
        this.caretPosition.set(Utils.clamp(0, caretPosition, getLength()));
        this.anchor.set(Utils.clamp(0, anchor, getLength()));
        this.selection.set(IndexRange.normalize(getAnchor(), getCaretPosition()));
    }

    /**
     * This function will extend the selection to include the specified pos.
     * This is different from selectPositionCaret in that it does not simply
     * move the caret. Rather, it will reposition the caret and anchor as necessary
     * to ensure that pos becomes the new caret and the far other end of the
     * selection becomes the anchor.
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
     */
    public void replaceSelection(String replacement) {
        if (text.isBound()) return;

        if (replacement == null) {
            throw new NullPointerException();
        }

        final int dot = getCaretPosition();
        final int mark = getAnchor();
        int start = Math.min(dot, mark);
        int end = Math.max(dot, mark);
        int pos = dot;

        if (getLength() == 0) {
            doNotAdjustCaret = true;
            setText(replacement);
            selectRange(getLength(), getLength());
            doNotAdjustCaret = false;
        } else {
            deselect();
            // RT-16566: Need to take into account stripping of chars into caret pos
            doNotAdjustCaret = true;
            int oldLength = getLength();
            end = Math.min(end, oldLength);
            if (end > start) {
                getContent().delete(start, end, replacement.isEmpty());
                oldLength -= (end - start);
            }
            getContent().insert(start, replacement, true);
            // RT-16566: Need to take into account stripping of chars into caret pos
            final int p = start + getLength() - oldLength;
            selectRange(p, p);
            doNotAdjustCaret = false;
        }
    }

    // Used by TextArea, although there are probably other better ways of
    // doing this.
    void textUpdated() { }

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

        private void invalidate() {
            markInvalid();
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

        private void doSet(String value) {
            // Guard against the null value.
            textIsNull = value == null;
            if (value == null) value = "";
            // Update the content
            content.delete(0, content.length(), value.isEmpty());
            content.insert(0, value, true);
            if (!doNotAdjustCaret) {
                selectRange(0, 0);
                textUpdated();
            }
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


    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/


    private static final PseudoClass PSEUDO_CLASS_READONLY
            = PseudoClass.getPseudoClass("readonly");

    /**
     * @treatAsPrivate implementation detail
     */
    private static class StyleableProperties {
        private static final FontCssMetaData<TextInputControl> FONT =
            new FontCssMetaData<TextInputControl>("-fx-font", Font.getDefault()) {

            @Override
            public boolean isSettable(TextInputControl n) {
                return n.font == null || !n.font.isBound();
            }

            @Override
            public StyleableProperty<Font> getStyleableProperty(TextInputControl n) {
                return (StyleableProperty<Font>)n.fontProperty();
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
     * CssMetaData of its super classes.
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

}
