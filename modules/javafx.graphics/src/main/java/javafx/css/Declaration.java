/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.css;

import com.sun.javafx.css.ParsedValueImpl;
import javafx.css.converter.URLConverter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * This class serves as a container of a CSS property and its value.
 * @since 9
 */
final public class Declaration {
    final String property;
    final ParsedValue parsedValue;
    final boolean important;
    // The Rule to which this Declaration belongs.
    Rule rule;

    /**
     * Constructs a {@code Declaration} object.
     * @param propertyName name of the CSS property
     * @param parsedValue value of the CSS property
     * @param important importance of the Declaration
     */
    Declaration(final String propertyName, final ParsedValue parsedValue,
                final boolean important) {
        this.property = propertyName;
        this.parsedValue = parsedValue;
        this.important = important;
        if (propertyName == null) {
            throw new IllegalArgumentException("propertyName cannot be null");
        }
        if (parsedValue == null) {
            throw new IllegalArgumentException("parsedValue cannot be null");
        }
    }

    /**
     * Gets the parsed value.
     * @return the parsed value
     */
    public ParsedValue getParsedValue() {
        return parsedValue;
    }

    /**
     * Gets the CSS property name.
     * @return the CSS property
     */
    public String getProperty() {
        return property;
    }

    /**
     * Gets the {@code Rule} to which this {@code Declaration} belongs.
     * @return the {@code Rule}
     */
    public Rule getRule() {
        return rule;
    }

    /**
     * Gets the importance of this {@code Declaration}.
     * @return the important flag
     */
    public final boolean isImportant() {
        return important;
    }

    /**
     * Gets the {@code StyleOrigin} of this {@code Declaration}.
     */
    private StyleOrigin getOrigin() {
        Rule rule = getRule();
        if (rule != null)  {
            return rule.getOrigin();
        }
        return null;
    }
    /**
     * Indicates whether some other {@code Object} is "equal to" this one.
     * <p>
     * One {@code Declaration} is equal to another regardless of the {@code Rule} to which
     * the {@code Declaration} belongs. Only the property, value and importance are
     * considered.
     *
     * @param obj an {@code Object} to compare
     * @return {@code true} if this object is the same as the {@code obj} argument; {@code false} otherwise
     */
    @Override public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Declaration other = (Declaration) obj;
        if (this.important != other.important) {
            return false;
        }
        if (this.getOrigin() != other.getOrigin()) {
            return false;
        }
        if ((this.property == null) ? (other.property != null) : !this.property.equals(other.property)) {
            return false;
        }
        if (this.parsedValue != other.parsedValue && (this.parsedValue == null || !this.parsedValue.equals(other.parsedValue))) {
            return false;
        }
        return true;
    }

    @Override public int hashCode() {
        int hash = 5;
        hash = 89 * hash + (this.property != null ? this.property.hashCode() : 0);
        hash = 89 * hash + (this.parsedValue != null ? this.parsedValue.hashCode() : 0);
        hash = 89 * hash + (this.important ? 1 : 0);
        return hash;
    }

    @Override public String toString() {
        StringBuilder sbuf = new StringBuilder(property);
        sbuf.append(": ");
        sbuf.append(parsedValue);
        if (important) sbuf.append(" !important");
        return sbuf.toString();
    }

    //
    // RT-21964
    //
    // We know when the .css file is parsed what the stylesheet URL is,
    // but that might not be the URL of the deployed file. So for URL
    // types, the parser inserts a null placeholder for the URL and we
    // fix it up here. This method is called from Rule#setStylesheet
    // and from Rule#declarations onChanged method.
    //
    void fixUrl(String stylesheetUrl) {

        if (stylesheetUrl == null) return;

        final StyleConverter converter = parsedValue.getConverter();

        // code is tightly coupled to the way URLConverter works
        if (converter == URLConverter.getInstance()) {

            final ParsedValue[] values = (ParsedValue[])parsedValue.getValue();
            values[1] = new ParsedValueImpl<String,String>(stylesheetUrl, null);

        } else if (converter == URLConverter.SequenceConverter.getInstance()) {

            final ParsedValue<ParsedValue[], String>[] layers =
                (ParsedValue<ParsedValue[], String>[])parsedValue.getValue();

            for (int layer = 0; layer < layers.length; layer++) {
                final ParsedValue[] values = layers[layer].getValue();
                values[1] = new ParsedValueImpl<String,String>(stylesheetUrl, null);
            }

        }

    }

    final void writeBinary(final DataOutputStream os, final StyleConverter.StringStore stringStore)
        throws IOException
    {
        if (parsedValue instanceof ParsedValueImpl) {
            os.writeShort(stringStore.addString(getProperty()));
            ((ParsedValueImpl)parsedValue).writeBinary(os,stringStore);
            os.writeBoolean(isImportant());
        }
    }

    static Declaration readBinary(int bssVersion, DataInputStream is, String[] strings)
        throws IOException
    {
        final String propertyName = strings[is.readShort()];
        final ParsedValueImpl parsedValue = ParsedValueImpl.readBinary(bssVersion,is,strings);
        final boolean important = is.readBoolean();
        return new Declaration(propertyName, parsedValue, important);

    }
}

