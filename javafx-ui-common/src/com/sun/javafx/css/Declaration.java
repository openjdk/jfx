/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.javafx.css;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

final public class Declaration {
    final String property;
    final ParsedValue parsedValue;
    final boolean important;   
    // The Rule to which this Declaration belongs.
    Rule rule;

    public Declaration(final String propertyName, final ParsedValue parsedValue,
            final boolean important) {
        this.property = propertyName;
        this.parsedValue = parsedValue;
        this.important = important;
    }

    /** @return ParsedValue contains the parsed declaration. */
    public ParsedValue getParsedValue() {
        return parsedValue;
    }
    
    /** @return The CSS property name */
    public String getProperty() {
        return property;
    }
    
    /** @return The Rule to which this Declaration belongs. */
    public Rule getRule() {
        return rule;
    }
    
    public boolean isImportant() {
        return important;
    }

    /** 
     * One declaration is the equal to another regardless of the Rule to which
     * the Declaration belongs. Only the property, value and importance are
     * considered.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Declaration other = (Declaration) obj;
        if ((this.property == null) ? (other.property != null) : !this.property.equals(other.property)) {
            return false;
        }
        if (this.parsedValue != other.parsedValue && (this.parsedValue == null || !this.parsedValue.equals(other.parsedValue))) {
            return false;
        }
        if (this.important != other.important) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
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

    void writeBinary(final DataOutputStream os, final StringStore stringStore)
        throws IOException
    {
        os.writeShort(stringStore.addString(property));
        parsedValue.writeBinary(os,stringStore);
        os.writeBoolean(important);
    }

    static Declaration readBinary(DataInputStream is, String[] strings)
        throws IOException
    {
        final String propertyName = strings[is.readShort()];
        final ParsedValue parsedValue = ParsedValue.readBinary(is,strings);
        final boolean important = is.readBoolean();
        return new Declaration(propertyName, parsedValue, important);
    }
}

