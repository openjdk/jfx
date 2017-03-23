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

import javafx.beans.NamedArg;


/**
 * Class representing a contiguous range of integral values.
 * @since JavaFX 2.0
 */
public final class IndexRange {
    private int start;
    private int end;

    /**
     * Index range value delimiter.
     */
    public static final String VALUE_DELIMITER = ",";

    /**
     * Creates an instance of IndexRange representing the range between
     * <code>start</code> and <code>end</code>.
     *
     * @param start The start position of the range.
     * @param end The end position of the range.
     */
    public IndexRange(@NamedArg("start") int start, @NamedArg("end") int end) {
        if (end < start) {
            throw new IllegalArgumentException();
        }

        this.start = start;
        this.end = end;
    }

    /**
     * Creates an instance of IndexRange by copying the values from the
     * given IndexRange object.
     *
     * @param range The IndexRange instance from which to copy the start and end
     *      values.
     */
    public IndexRange(@NamedArg("range") IndexRange range) {
        this.start = range.start;
        this.end = range.end;
    }

    /**
     * Returns the start position of the range.
     * @return the start position of the range
     */
    public int getStart() {
        return start;
    }

    /**
     * Returns the end position of the range (exclusive).
     * @return the end position of the range (exclusive)
     */
    public int getEnd() {
        return end;
    }

    /**
     * Returns the length of the range.
     * @return the length of the range
     */
    public int getLength() {
        return end - start;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * @param object the reference object with which to compare.
     * @return {@code true} if this object is equal to the {@code object} argument; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object object) {
        if (object == this) return true;
        if (object instanceof IndexRange) {
            IndexRange range = (IndexRange)object;
            return (start == range.start
                && end == range.end);
        }

        return false;
    }

    /**
     * Returns a hash code for this {@code Range} object.
     * @return a hash code for this {@code Range} object.
     */
    @Override
    public int hashCode() {
        return 31 * start + end;
    }

    /**
     * Returns a string representation of this {@code Range} object.
     * @return a string representation of this {@code Range} object.
     */
    @Override
    public String toString() {
        return start + VALUE_DELIMITER + " " + end;
    }

    /**
     * Convenience method to create an IndexRange instance that has the smaller
     * value as the start index, and the larger value as the end index.
     *
     * @param v1 The first value to use in the range.
     * @param v2 The second value to use in the range.
     * @return A IndexRange instance where the smaller value is the start, and the
     *      larger value is the end.
     */
    public static IndexRange normalize(int v1, int v2) {
        return new IndexRange(Math.min(v1, v2), Math.max(v1, v2));
    }

    /**
     * Convenience method to parse in a String of the form '2,6', which will
     * create an IndexRange instance with a start value of 2, and an end value
     * of 6.
     *
     * @param value The string to be parsed, and converted to an IndexRange.
     * @return An IndexRange instance representing the start and end values provided
     *      in the value string.
     */
    public static IndexRange valueOf(String value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }

        String[] values = value.split(VALUE_DELIMITER);
        if (values.length != 2) {
            throw new IllegalArgumentException();
        }

        // NOTE As of Java 6, Integer#parseInt() appears to require
        // trimmed values
        int start = Integer.parseInt(values[0].trim());
        int end = Integer.parseInt(values[1].trim());

        return IndexRange.normalize(start, end);
    }
}
