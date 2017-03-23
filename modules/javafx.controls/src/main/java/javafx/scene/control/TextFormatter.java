/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
import javafx.beans.NamedArg;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.util.StringConverter;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * A Formatter describes a format of a {@code TextInputControl} text by using two distinct mechanisms:
 * <ul>
 *     <li>A filter ({@link #getFilter()}) that can intercept and modify user input. This helps to keep the text
 *     in the desired format. A default text supplier can be used to provide the intial text.</li>
 *     <li>A value converter ({@link #getValueConverter()}) and value ({@link #valueProperty()})
 *     can be used to provide special format that represents a value of type {@code V}.
 *     If the control is editable and the text is changed by the user, the value is then updated to correspond to the text.
 * </ul>
 * <p>
 * It's possible to have a formatter with just filter or value converter. If value converter is not provided however, setting a value will
 * result in an {@code IllegalStateException} and the value is always null.
 * <p>
 * Since {@code Formatter} contains a value which represents the state of the {@code TextInputControl} to which it is currently assigned, a single
 * {@code Formatter} instance can be used only in one {@code TextInputControl} at a time.
 *
 * @param <V> The type of the value
 * @since JavaFX 8u40
 */
public class TextFormatter<V> {
    private final StringConverter<V> valueConverter;
    private final UnaryOperator<Change> filter;

    private Consumer<TextFormatter<?>> textUpdater;

    /**
     * This string converter converts the text to the same String value. This might be useful for cases where you
     * want to manipulate with the text through the value or you need to provide a default text value.
     */
    public static final StringConverter<String> IDENTITY_STRING_CONVERTER = new StringConverter<String>() {
        @Override
        public String toString(String object) {
            return object == null ? "" : object;
        }
        @Override
        public String fromString(String string) {
            return string;
        }
    };


    /**
     * Creates a new Formatter with the provided filter.
     * @param filter The filter to use in this formatter or null
     */
    public TextFormatter(@NamedArg("filter") UnaryOperator<Change> filter) {
        this(null, null, filter);
    }

    /**
     * Creates a new Formatter with the provided filter, value converter and default value.
     * @param valueConverter The value converter to use in this formatter or null.
     * @param defaultValue the default value.
     * @param filter The filter to use in this formatter or null
     */
    public TextFormatter(@NamedArg("valueConverter") StringConverter<V> valueConverter,
                         @NamedArg("defaultValue") V defaultValue, @NamedArg("filter") UnaryOperator<Change> filter) {
        this.filter = filter;
        this.valueConverter = valueConverter;
        setValue(defaultValue);
    }

    /**
     * Creates a new Formatter with the provided value converter and default value.
     * @param valueConverter The value converter to use in this formatter. This must not be null.
     * @param defaultValue the default value
     */
    public TextFormatter(@NamedArg("valueConverter") StringConverter<V> valueConverter, @NamedArg("defaultValue") V defaultValue) {
        this(valueConverter, defaultValue, null);
    }

    /**
     * Creates a new Formatter with the provided value converter. The default value will be null.
     * @param valueConverter The value converter to use in this formatter. This must not be null.
     */
    public TextFormatter(@NamedArg("valueConverter") StringConverter<V> valueConverter) {
        this(valueConverter, null, null);
    }


    /**
     * The converter between the values and text.
     * It maintains a "binding" between the {@link javafx.scene.control.TextInputControl#textProperty()} }
     * and {@link #valueProperty()} }. The value is updated when the control loses it's focus or it is commited (TextField only).
     * Setting the value will update the text of the control, usin the provided converter.
     *
     * If it's impossible to convert text to value, an exception should be thrown.
     * @return StringConverter for values or null if none provided
     * @see javafx.scene.control.TextField#commitValue()
     * @see javafx.scene.control.TextField#cancelEdit()
     */
    public final StringConverter<V> getValueConverter() {
        return valueConverter;
    }

    /**
     * Filter allows user to intercept and modify any change done to the text content.
     * <p>
     * The filter itself is an {@code UnaryOperator} that accepts {@link javafx.scene.control.TextFormatter.Change} object.
     * It should return a {@link javafx.scene.control.TextFormatter.Change} object that contains the actual (filtered)
     * change. Returning null rejects the change.
     * @return the filter for this formatter or null if there is none
     */
    public final UnaryOperator<Change> getFilter() {
        return filter;
    }

    /**
     * The current value for this formatter. When the formatter is set on a {@code TextInputControl} and has a
     * {@code valueConverter}, the value is set by the control, when the text is commited.
     */
    private final ObjectProperty<V> value = new ObjectPropertyBase<V>() {

        @Override
        public Object getBean() {
            return TextFormatter.this;
        }

        @Override
        public String getName() {
            return "value";
        }

        @Override
        protected void invalidated() {
            if (valueConverter == null && get() != null) {
                if (isBound()) {
                    unbind();
                }
                throw new IllegalStateException("Value changes are not supported when valueConverter is not set");
            }
            updateText();
        }
    };

    public final ObjectProperty<V> valueProperty() {
        return value;
    }
    public final void setValue(V value) {
        if (valueConverter == null && value != null) {
            throw new IllegalStateException("Value changes are not supported when valueConverter is not set");
        }
        this.value.set(value);
    }
    public final V getValue() {
        return value.get();
    }

    private void updateText() {
        if (textUpdater != null) {
            textUpdater.accept(this);
        }
    }

    void bindToControl(Consumer<TextFormatter<?>> updater) {
        if (textUpdater != null) {
            throw new IllegalStateException("Formatter is already used in other control");
        }
        this.textUpdater = updater;
    }

    void unbindFromControl() {
        this.textUpdater = null;
    }

    void updateValue(String text) {
        if (!value.isBound()) {
            try {
                V v = valueConverter.fromString(text);
                setValue(v);
            } catch (Exception e) {
                updateText(); // Set the text with the latest value
            }
        }
    }

    /**
     * Contains the state representing a change in the content or selection for a
     * TextInputControl. This object is passed to any registered
     * {@code formatter} on the TextInputControl whenever the text
     * for the TextInputControl is modified.
     * <p>
     *     This class contains state and convenience methods for determining what
     *     change occurred on the control. It also has a reference to the
     *     TextInputControl itself so that the developer may query any other
     *     state on the control. Note that you should never modify the state
     *     of the control directly from within the formatter handler.
     * </p>
     * <p>
     *     The Change of the text is described by <b>range</b> ({@link #getRangeStart()}, {@link #getRangeEnd()}) and
     *     text ({@link #getText()}. There are 3 cases that can occur:
     *     <ul>
     *         <li><b>Some text was deleted:</b> In this case, {@code text} is empty and {@code range} denotes the {@code range} of deleted text.
     *         E.g. In text "Lorem ipsum dolor sit amet", removal of the second word would result in {@code range} being (6,11) and
     *         an empty {@code text}. Similarly, if you want to delete some different or additional text, just set the {@code range}.
     *         If you want to remove first word instead of the second, just call {@code setRange(0,5)}</li>
     *         <li><b>Some text was added:</b> Now the {@code range} is empty (means nothing was deleted), but it's value is still important.
     *         Both the start and end of the {@code range} point to the index wheret the new text was added. E.g. adding "ipsum " to "Lorem dolor sit amet"
     *         would result in a change with {@code range} of (6,6) and {@code text} containing the String "ipsum ".</li>
     *         <li><b>Some text was replaced:</b> The combination of the 2 cases above. Both {@code text} and {@code range} are not empty. The text in {@code range} is deleted
     *         and replaced by {@code text} in the Change. The new text is added instead of the old text, which is at the beginning of the {@code range}.
     *         E.g. when some text is being deleted, you can simply replace it by some placeholder text just by setting a new text
     *         ({@code setText("new text")})</li>
     *     </ul>
     * <p>
     *     The Change is mutable, but not observable. It should be used
     *     only for the life of a single change. It is intended that the
     *     Change will be modified from within the formatter.
     * </p>
     * @since JavaFX 8u40
     */
    public static final class Change implements Cloneable {
        private final FormatterAccessor accessor;
        private Control control;
        int start;
        int end;
        String text;

        int anchor;
        int caret;

        Change(Control control, FormatterAccessor accessor,  int anchor, int caret) {
            this(control, accessor, caret, caret, "", anchor, caret);
        }

        Change(Control control, FormatterAccessor accessor, int start, int end, String text) {
            this(control, accessor, start, end, text, start + text.length(), start + text.length());
        }

        // Restrict construction to TextInputControl only. Because we are the
        // only ones who can create this, we don't bother doing a check here
        // to make sure the arguments are within reason (they will be).
        Change(Control control, FormatterAccessor accessor, int start, int end, String text, int anchor, int caret) {
            this.control = control;
            this.accessor = accessor;
            this.start = start;
            this.end = end;
            this.text = text;
            this.anchor = anchor;
            this.caret = caret;
        }

        /**
         * Gets the control associated with this change.
         * @return The control associated with this change. This will never be null.
         */
        public final Control getControl() { return control; }

        /**
         * Gets the start index into the {@link TextInputControl#getText()}
         * for the modification. This will always be a value &gt; 0 and
         * &lt;= {@link TextInputControl#getLength()}.
         *
         * @return The start index
         */
        public final int getRangeStart() { return start; }

        /**
         * Gets the end index into the {@link TextInputControl#getText()}
         * for the modification. This will always be a value &gt; {@link #getRangeStart()} and
         * &lt;= {@link TextInputControl#getLength()}.
         *
         * @return The end index
         */
        public final int getRangeEnd() { return end; }

        /**
         * A method assigning both the start and end values
         * together, in such a way as to ensure they are valid with respect to
         * each other. The start must be less than or equal to the end.
         *
         * @param start The new start value. Must be a valid start value
         * @param end The new end value. Must be a valid end value
         */
        public final void setRange(int start, int end) {
            int length = accessor.getTextLength();
            if (start < 0 || start > length || end < 0 || end > length) {
                throw new IndexOutOfBoundsException();
            }
            this.start = start;
            this.end = end;
        }


        /**
         * Gets the new caret position. This value will always be &gt; 0 and
         * &lt;= {@link #getControlNewText()}{@code}.getLength()}
         *
         * @return The new caret position
         */
        public final int getCaretPosition() { return caret; }

        /**
         * Gets the new anchor. This value will always be &gt; 0 and
         * &lt;= {@link #getControlNewText()}{@code}.getLength()}
         *
         * @return The new anchor position
         */
        public final int getAnchor() { return anchor; }

        /**
         * Gets the current caret position of the control.
         * @return The previous caret position
         */
        public final int getControlCaretPosition() { return accessor.getCaret();}

        /**
         * Gets the current anchor position of the control.
         * @return The previous anchor
         */
        public final int getControlAnchor() { return accessor.getAnchor(); }

        /**
         * Sets the selection. The anchor and caret position values must be &gt; 0 and
         * &lt;= {@link #getControlNewText()}{@code}.getLength()}. Note that there
         * is an order dependence here, in that the positions should be
         * specified after the new text has been specified.
         *
         * @param newAnchor The new anchor position
         * @param newCaretPosition The new caret position
         */
        public final void selectRange(int newAnchor, int newCaretPosition) {
            if (newAnchor < 0 || newAnchor > accessor.getTextLength() - (end - start) + text.length()
                    || newCaretPosition < 0 || newCaretPosition > accessor.getTextLength() - (end - start) + text.length()) {
                throw new IndexOutOfBoundsException();
            }
            anchor = newAnchor;
            caret = newCaretPosition;
        }

        /**
         * Gets the selection of this change. Note that the selection range refers to {@link #getControlNewText()}, not
         * the current control text.
         * @return The selected range of this change.
         */
        public final IndexRange getSelection() {
            return IndexRange.normalize(anchor, caret);
        }


        /**
         * Sets the anchor. The anchor value must be &gt; 0 and
         * &lt;= {@link #getControlNewText()}{@code}.getLength()}. Note that there
         * is an order dependence here, in that the position should be
         * specified after the new text has been specified.
         *
         * @param newAnchor The new anchor position
         */
        public final void setAnchor(int newAnchor) {
            if (newAnchor < 0 || newAnchor > accessor.getTextLength() - (end - start) + text.length()) {
                throw new IndexOutOfBoundsException();
            }
            anchor = newAnchor;
        }

        /**
         * Sets the caret position. The caret position value must be &gt; 0 and
         * &lt;= {@link #getControlNewText()}{@code}.getLength()}. Note that there
         * is an order dependence here, in that the position should be
         * specified after the new text has been specified.
         *
         * @param newCaretPosition The new caret position
         */
        public final void setCaretPosition(int newCaretPosition) {
            if (newCaretPosition < 0 || newCaretPosition > accessor.getTextLength() - (end - start) + text.length()) {
                throw new IndexOutOfBoundsException();
            }
            caret = newCaretPosition;
        }

        /**
         * Gets the text used in this change. For example, this may be new
         * text being added, or text which is replacing all the control's text
         * within the range of start and end. Typically it is an empty string
         * only for cases where the range is being deleted.
         *
         * @return The text involved in this change. This will never be null.
         */
        public final String getText() { return text; }

        /**
         * Sets the text to use in this change. This is used to replace the
         * range from start to end, if such a range exists, or to insert text
         * at the position represented by start == end.
         *
         * @param value The text. This cannot be null.
         */
        public final void setText(String value) {
            if (value == null) throw new NullPointerException();
            text = value;
        }

        /**
         * This is the full text that control has before the change. To get the text
         * after this change, use {@link #getControlNewText()}.
         * @return the previous text of control
         */
        public final String getControlText() {
            return accessor.getText(0, accessor.getTextLength());
        }

        /**
         * Gets the complete new text which will be used on the control after
         * this change. Note that some controls (such as TextField) may do further
         * filtering after the change is made (such as stripping out newlines)
         * such that you cannot assume that the newText will be exactly the same
         * as what is finally set as the content on the control, however it is
         * correct to assume that this is the case for the purpose of computing
         * the new caret position and new anchor position (as those values supplied
         * will be modified as necessary after the control has stripped any
         * additional characters that the control might strip).
         *
         * @return The controls proposed new text at the time of this call, according
         *         to the state set for start, end, and text properties on this Change object.
         */
        public final String getControlNewText() {
            return accessor.getText(0, start) + text + accessor.getText(end, accessor.getTextLength());
        }

        /**
         * Gets whether this change was in response to text being added. Note that
         * after the Change object is modified by the formatter (by one
         * of the setters) the return value of this method is not altered. It answers
         * as to whether this change was fired as a result of text being added,
         * not whether text will end up being added in the end.
         *
         * @return true if text was being added
         */
        public final boolean isAdded() { return !text.isEmpty(); }

        /**
         * Gets whether this change was in response to text being deleted. Note that
         * after the Change object is modified by the formatter (by one
         * of the setters) the return value of this method is not altered. It answers
         * as to whether this change was fired as a result of text being deleted,
         * not whether text will end up being deleted in the end.
         *
         * @return true if text was being deleted
         */
        public final boolean isDeleted() { return start != end; }

        /**
         * Gets whether this change was in response to text being replaced. Note that
         * after the Change object is modified by the formatter (by one
         * of the setters) the return value of this method is not altered. It answers
         * as to whether this change was fired as a result of text being replaced,
         * not whether text will end up being replaced in the end.
         *
         * @return true if text was being replaced
         */
        public final boolean isReplaced() {
            return isAdded() && isDeleted();
        }

        /**
         * The content change is any of add, delete or replace changes. Basically it's a shortcut for
         * {@code c.isAdded() || c.isDeleted() };
         * @return true if the content changed
         */
        public final boolean isContentChange() {
            return isAdded() || isDeleted();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("TextInputControl.Change [");
            if (isReplaced()) {
                builder.append(" replaced \"").append(accessor.getText(start, end)).append("\" with \"").append(text).
                        append("\" at (").append(start).append(", ").append(end).append(")");
            } else if (isDeleted()) {
                builder.append(" deleted \"").append(accessor.getText(start, end)).
                        append("\" at (").append(start).append(", ").append(end).append(")");
            } else if (isAdded()) {
                builder.append(" added \"").append(text).append("\" at ").append(start);
            }
            if (isAdded() || isDeleted()) {
                builder.append("; ");
            } else {
                builder.append(" ");
            }
            builder.append("new selection (anchor, caret): [").append(anchor).append(", ").append(caret).append("]");
            builder.append(" ]");
            return builder.toString();
        }

        @Override
        public Change clone() {
            try {
                return (Change) super.clone();
            } catch (CloneNotSupportedException e) {
                // Cannot happen
                throw new RuntimeException(e);
            }
        }
    }

}
