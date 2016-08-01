/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.javafx.jmx.json;

/**
 * Thrown when an error occurs while reading a JSON document.
 */
@SuppressWarnings("serial")
public class JSONException extends RuntimeException {

    private final int line;
    private final int column;

    /**
     * Construct an exception with the exception, line and column number.
     *
     * @param cause - the cause of the error
     * @param line - the line number of the error
     * @param column - the column number of the error
     */
    public JSONException(Throwable cause, int line, int column) {
        super(cause);
        this.line = line;
        this.column = column;
    }

    /**
     * Construct an exception with the message, line and column number.
     *
     * @param message - the message to report
     * @param line - the line number of the error
     * @param column - the column number of the error
     */
    public JSONException(String message, int line, int column) {
        super(message);
        this.line = line;
        this.column = column;
    }

    /**
     * Returns the line number of where the error occurred.
     *
     * @return the line number of the error
     */
    public int line() {
        return line;
    }

    /**
     * Returns the column number of where the error occurred.
     *
     * @return the column number of the error
     */
    public int column() {
        return column;
    }

    /**
     * Returns a string describing the error.
     *
     * @return the string describing the error
     */
    public String toString() {
        return "(" + line + "," + column + ") " + super.toString();
    }

}
