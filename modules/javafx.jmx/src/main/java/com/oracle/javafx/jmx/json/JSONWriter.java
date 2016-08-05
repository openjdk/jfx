/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.javafx.jmx.json.impl.JSONMessages;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Class for writing a JSON document.
 *
 * An example of how to write a simple JSON document.
 * <pre>
 * JSONFactory FACTORY = JSONFactory.instance();
 * JSONWriter w = FACTORY.makeWriter(writer);
 * w.startObject();
 * w.objectValue("abc", "123");
 * w.startObject("def");
 * w.startArray();
 * w.endArray();
 * w.endObject();
 * w.endObject();
 * w.close();
 * </pre>
 * will produce
 * <pre>
 * { "abc":"123", def:[] }}
 * </pre>
 * These methods can be chained together, so the above
 * could also be written as
 * <pre>
 * w.startObject()
 *    .objectValue("abc", "123")
 *    .startObject("def")
 *      .startArray()
 *      .endArray()
 *    .endObject()
 *  .endObject();
 * </pre>
 */

public class JSONWriter {

    private enum ContainerType { ARRAY, OBJECT }

    private static class Container {
        ContainerType type;
        boolean first;
        Container(ContainerType type) {
            this.type = type;
            this.first = true;
        }
    }

    private final Writer writer;
    private Stack<Container> where = new Stack<Container>();

    JSONWriter(final Writer writer) {
        this.writer = writer;
    }

    /**
     * Writes the opening curly brace for a JSON object.
     *
     * @throws IOException if an I/O error occurs.
     */
    public JSONWriter startObject() throws IOException {
        writeSeparatorIfNeeded();
        writer.write("{");
        where.push(new Container(ContainerType.OBJECT));
        return this;
    }

    /**
     * Writes a JSON object with the specified name.
     *
     * @param key - the name of the JSON object
     * @throws IOException if an I/O error occurs.
     */
    public JSONWriter startObject(String key) throws IOException {
        if (where.peek().type != ContainerType.OBJECT) {
            throw new IllegalStateException(JSONMessages.localize(null, "not_inside_object"));
        }
        writeSeparatorIfNeeded();
        writeEscapedString(key);
        writer.write(":{");
        where.push(new Container(ContainerType.OBJECT));
        return this;
    }

    /**
     * Writes the closing curly brace for a JSON object.
     *
     * @throws IOException if an I/O error occurs.
     */
    public JSONWriter endObject() throws IOException {
        if (where.peek().type != ContainerType.OBJECT) {
            throw new IllegalStateException(JSONMessages.localize(null, "mismatched_call_to_endObject"));
        }
        where.pop();
        writer.write("}");
        return this;
    }

    /**
     * Write the opening square brace for a JSON array.
     *
     * @throws IOException if an I/O error occurs.
     */
    public JSONWriter startArray() throws IOException {
        writeSeparatorIfNeeded();
        writer.write("[");
        where.push(new Container(ContainerType.ARRAY));
        return this;
    }

    /**
     * Write a JSON array with the specified name.
     *
     * @param key - the name of the array.
     * @throws IOException if an I/O error occurs.
     */
    public JSONWriter startArray(String key) throws IOException {
        if (where.peek().type != ContainerType.OBJECT) {
            throw new IllegalStateException(JSONMessages.localize(null, "not_inside_object"));
        }
        writeSeparatorIfNeeded();
        writeEscapedString(key);
        writer.write(":[");
        where.push(new Container(ContainerType.ARRAY));
        return this;
    }

    /**
     * Writes the closing square brace for a JSON array.
     *
     * @throws IOException if an I/O error occurs.
     */
    public JSONWriter endArray() throws IOException {
        if (where.peek().type != ContainerType.ARRAY) {
            throw new IllegalStateException(JSONMessages.localize(null, "mismatched_call_to_endArray"));
        }
        writer.write("]");
        where.pop();
        return this;
    }

    /**
     * Writes a name/value pair for a JSON object.
     *
     * @param key - the name of the JSON object.
     * @param value - the value of the JSON object.
     * The value may be a Map, in which case entries in the Map
     * are written as elements of the current JSON Object.
     * @throws IOException if an I/O error occurs.
     */
    public JSONWriter objectValue(String key, Object value) throws IOException {
        if (where.peek().type != ContainerType.OBJECT) {
            throw new IllegalStateException(JSONMessages.localize(null, "not_inside_object"));
        }
        writeSeparatorIfNeeded();
        writeEscapedString(key);
        writer.write(":");
        writeValue(value);
        return this;
    }

    /**
     * Writes a value into a JSON array.
     *
     * @param value - the value of an array element.
     * The value may be a List, in which case elements of the List
     * are written as elements of the current JSON array.
     * @throws IOException if an I/O error occurs.
     */
    public JSONWriter arrayValue(Object value) throws IOException {
        if (where.peek().type != ContainerType.ARRAY) {
            throw new IllegalStateException(JSONMessages.localize(null, "not_inside_array"));
        }
        writeSeparatorIfNeeded();
        writeValue(value);
        return this;
    }

    /**
     * Convenience method to write the entries in a Map as a JSON Object.
     *
     * @param values the Map entries to be written
     * @throws IOException if an I/O error occurs.
     */
    public JSONWriter writeObject(Map<String, Object> values) throws IOException {
        startObject();
        Iterator<Map.Entry<String, Object>> vi = values.entrySet().iterator();
        while (vi.hasNext()) {
            Map.Entry<String, Object> value = vi.next();
            objectValue(value.getKey(), value.getValue());
        }
        endObject();
        return this;
    }

    /**
     * Convenience method to write the elements of a List as a JSON array.
     *
     * @param values the List of values to be written
     * @throws IOException if an I/O error occurs.
     */
    public JSONWriter writeArray(List<Object> values) throws IOException {
        startArray();
        for (Object value : values) {
            arrayValue(value);
        }
        endArray();
        return this;
    }

    /**
     * Flushes JSONWriter to writer.
     *
     * @throws IOException if an I/O error occurs.
     */
    public JSONWriter flush() throws IOException {
        writer.flush();
        return this;
    }

    /**
     * Closes the JSONWriter.
     *
     * @throws IOException if an I/O error occurs.
     */
    public void close() throws IOException {
        writer.close();
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private void writeValue(Object value) throws IOException {
        if (value == null) {
            writer.write("null");
        } else if (value instanceof Map) {
            startObject();
            Iterator<Map.Entry> mi = ((Map) value).entrySet().iterator();
            while (mi.hasNext()) {
                Map.Entry e = mi.next();
                objectValue((String) e.getKey(), e.getValue());
            }
            endObject();
        } else if (value instanceof List) {
            startArray();
            Iterator ai = ((List) value).iterator();
            while (ai.hasNext()) {
                arrayValue(ai.next());
            }
            endArray();
        } else if (value instanceof String) {
            writeEscapedString((String) value);
        } else {
            writer.write(value.toString());
        }
    }

    private void writeSeparatorIfNeeded() throws IOException {
        if (where.empty()) {
            return;
        }

        if (where.peek().first) {
            where.peek().first = false;
        } else {
            writer.write(",");
        }
    }

    private void writeEscapedString(String value) throws IOException {
        writer.write("\"");
        JSONDocument.printEscapedString(writer, value);
        writer.write("\"");
    }

}
