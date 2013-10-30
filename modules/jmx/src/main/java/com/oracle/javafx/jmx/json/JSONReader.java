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

import com.oracle.javafx.jmx.json.JSONReader.EventType;
import java.util.Iterator;

/**
 * Interface for reading a JSON document.
 *
 * An example of how to read a simple JSON document.
 * <pre>
 * JSON: { "abc": "123" }
 *
 * JSONFactory FACTORY = JSONFactory.instance();
 * JSONWriter r = FACTORY.makeReader(reader);
 * r.next();  // START_DOCUMENT
 * r.depth(); // depth = -1
 * r.next();  // START_OBJECT
 * r.next();  // START_VALUE
 * r.key();   // key = "abc"
 * r.next();  // STRING
 * r.value(); // value = "123"
 * r.next();  // END_VALUE
 * r.next();  // END_OBJECT
 * r.next();  // END_DOCUMENT
 * </pre>
 *
 */
public interface JSONReader extends Iterable<EventType>, Iterator<EventType> {

    /**
     * The type of JSON events.
     */
    public enum EventType {
        /**
         * An unknown event, possibly a syntax error in a JSON document.
         */
        ERROR,
        /**
         * Indicates the start of a JSON object.
         */
        START_OBJECT,
        /**
         * Indicates the end of a JSON object.
         */
        END_OBJECT,
        /**
         * Indicates the string value of a JSON element.
         */
        STRING,
        /**
         * The start of a JSON document.
         */
        START_DOCUMENT,
        /**
         * The end of a JSON document.
         */
        END_DOCUMENT,
        /**
         * Indicates the start of a JSON array.
         */
        START_ARRAY,
        /**
         * Indicates the start of a JSON array element.
         */
        START_ARRAY_ELEMENT,
        /**
         * Indicates the end of a JSON array element.
         */
        END_ARRAY_ELEMENT,
        /**
         * Indicates the end of a JSON array.
         */
        END_ARRAY,
        /**
         * Indicates a JSON floating point number.
         */
        NUMBER,
        /**
         * Indicates a JSON integer.
         */
        INTEGER,
        /**
         * Indicates a JSON true value.
         */
        TRUE,
        /**
         * Indicates a JSON false value.
         */
        FALSE,
        /**
         * Indicates a JSON null value.
         */
        NULL,
        /**
         * Indicates the start of a JSON object value.
         */
        START_VALUE,
        /**
         * Indicates the end of a JSON object value.
         */
        END_VALUE
    };

    /**
     * The name of the current JSON object.
     *
     * @return the name of a JSON object.
     */
    public String key();

    /**
     * The value of the current JSON object or array element.
     *
     * @return the value of the JSON object or array element.
     */
    public String value();

    /**
     * Skips until the specified object is found at the specified depth.
     * If depth is negative, find the first occurrence of the specified object.
     * If objectName is null, skip until the specified depth is reached.
     *
     * @param key the name of the object to find, may be null
     * @param depth stop at the first element at this depth, ignored if negative
     * @return the event at which this method stops, EventType.END_DOCUMENT if not found
     */
    public EventType next(String key, int depth);

    /**
     * Close the underlying Reader when done reading.
     */
    public void close();

    /**
     * The current line number in the JSON file.
     *
     * @return the current line number in the JSON file
     */
    public int line();

    /**
     * The current column number in the JSON file.
     *
     * @return the current column number in the JSON file.
     */
    public int column();

    /**
     * The current byte offset in the underlying Reader. This
     * is useful for computing percent completion.
     *
     * @return the current byte offset
     */
    public long offset();

    /**
     * The reader's current depth in the JSON file.
     *
     * @return the current depth in the JSON file.
     */
    public int depth();

    /**
     * Returns the path from the root to the current position of the JSON file.
     *
     * @return a String array containing the path.
     */
    public String[] path();

    /**
     * Build an in-memory representation of the input JSON. JSON Objects are
     * represented with Maps and JSON Arrays are represented with Lists.
     * @return a JSONDocument that contains a Map or a List representing the
     * root of the input object
     */
    public JSONDocument build();
}
