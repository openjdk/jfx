/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.DefaultProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import java.text.BreakIterator;

import com.sun.javafx.Utils;
import com.sun.javafx.binding.ExpressionHelper;
import com.sun.javafx.css.StyleManager;

/**
 * Abstract base class for text input controls.
 */
@DefaultProperty("text")
public abstract class TextInputControl extends Control {
    /**
     * Interface representing a text input's content. Since it is an ObservableStringValue,
     * you can also bind to, or observe the content.
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
         */
        public void insert(int index, String text, boolean notifyListeners);

        /**
         * Removes a sequence of characters from the content.
         *
         * @param start
         * @param end
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
            impl_pseudoClassStateChanged(PSEUDO_CLASS_READONLY);
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
     * @param range
     *
     * @see #deleteText(int, int)
     */
    public void deleteText(IndexRange range) {
        replaceText(range, "");
    }
    /**
     * Removes a range of characters from the content.
     *
     * @param start
     * @param end
     */
    public void deleteText(int start, int end) {
        replaceText(start, end, "");
    }

    /**
     * Replaces a range of characters with the given text.
     *
     * @param range
     * @param text
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
     * @param start
     * @param end
     * @param text
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
            selectRange(getAnchor(), getCaretPosition() - 1);
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
            selectRange(getAnchor(), getCaretPosition() + 1);
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
                int delChars = 1;

                // only have to deal with potentially removing two chars if the
                // caret is greater than 1
                if (dot > 1) {
                    // get the two chars proceeding the caret
                    int c0 = text.codePointAt(dot - 2);
                    int c1 = text.codePointAt(dot - 1);
                    if (c0 >= 0xD800 && c0 <= 0xDBFF &&
                        c1 >= 0xDC00 && c1 <= 0xDFFF) {
                        delChars = 2;
                    }
                }

                // remove either 1 or 2 chars
                doNotAdjustCaret = true;
                int pos = dot; // necessary in case that pos is end of line
                deleteText(dot - delChars, dot);
                int p = pos - delChars;
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
                int delChars = 1;

                // only have to deal with potentially removing two chars if the
                // caret is greater than 1
                if (dot < text.length() - 2) {
                    // get the two chars proceeding the caret
                    int c0 = text.codePointAt(dot + 2);
                    int c1 = text.codePointAt(dot + 1);
                    if (c0 >= 0xD800 && c0 <= 0xDBFF &&
                        c1 >= 0xDC00 && c1 <= 0xDFFF) {
                        delChars = 2;
                    }
                }

                // remove either 1 or 2 chars
                doNotAdjustCaret = true;
                //setText(text.substring(0, dot) + text.substring(dot + delChars));
                deleteText(dot, dot + delChars);
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
            int pos = dot + 1;
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
     * @since JavaFX 1.3
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
            int pos = dot - 1;
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

    /*
     * Virtual keyboard API. This is not yet final.
     *
     * 0: Text, 1: Numeric, 2: URL, 3: Email address
     *
     * TODO: Use mnemonics until VK API is avaiulable in Quantum.
     */

    /** @treatAsPrivate implementation detail */
    private IntegerProperty impl_virtualKeyboardType = new SimpleIntegerProperty(this, "impl_virtualKeyboardType", 0);

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final void setImpl_virtualKeyboardType(int value) { impl_virtualKeyboardType.set(value); }
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final int getImpl_virtualKeyboardType() { return impl_virtualKeyboardType.get(); }
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final IntegerProperty impl_virtualKeyboardTypePoperty() { return impl_virtualKeyboardType; }



    @Deprecated
    public void impl_enableVirtualKeyboard(int type) {
    }

    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String PSEUDO_CLASS_READONLY = "readonly";

    private static final long PSEUDO_CLASS_READONLY_MASK
            = StyleManager.getInstance().getPseudoclassMask(PSEUDO_CLASS_READONLY);

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated @Override public long impl_getPseudoClassState() {
        long mask = super.impl_getPseudoClassState();

        if (!isEditable()) mask |= PSEUDO_CLASS_READONLY_MASK;

        return mask;
    }
}
