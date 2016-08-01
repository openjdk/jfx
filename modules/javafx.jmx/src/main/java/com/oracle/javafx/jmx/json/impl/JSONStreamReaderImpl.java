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

package com.oracle.javafx.jmx.json.impl;

import com.oracle.javafx.jmx.json.JSONDocument;
import com.oracle.javafx.jmx.json.JSONException;
import com.oracle.javafx.jmx.json.JSONReader;
import java.util.Stack;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

public class JSONStreamReaderImpl implements JSONReader {

    private JSONScanner scanner;
    private JSONSymbol currentSymbol;
    private EventType currentEvent;
    private volatile boolean nullValue;
    private String key;
    private Stack<String> stack;
    private boolean eod = false;
    private JSONDocument collection = null;
    private String collectionKey = null;
    private int depth = 0;

    public JSONStreamReaderImpl(Reader reader) throws JSONException {
        stack = new Stack<String>();
        currentSymbol = JSONSymbol.EOS;
        try {
            scanner = new JSONScanner(reader);
            JSONSymbol.init(scanner);
        } catch(IOException e) {
            throw new JSONException(e, scanner.line(), scanner.column());
        }
    }

    @Override
    public Iterator<EventType> iterator() {
        return this;
    }

    /**
     * Skips until the specified object is found at the specified depth.
     * If depth is negative, find the first occurrence of the specified object.
     * If objectName is null, skip until the specified depth is reached.
     * @param objectName the name of the object to find, may be null
     * @param depth stop at the first element at this depth, ignored if negative
     * @return the event at which this method stops, EventType.END_DOCUMENT if not found
     * @throws JSONException in case of parse errors
     */
    @Override
    public EventType next(String objectName, int depth) throws JSONException {
        if (objectName == null && depth < 0) {
            throw new IllegalArgumentException(JSONMessages.localize(null, "either_objectName_or_level_must_be_specified"));
        }
        EventType type = EventType.END_DOCUMENT;
        for (type = next(); type != EventType.END_DOCUMENT && type != EventType.ERROR; type = next()) {
            if (type == EventType.START_VALUE) {
                if (objectName != null && depth >= 0) {
                    if (checkName(objectName) && checkLevel(depth)) {
                        break;
                    }
                } else {
                    if (objectName != null && checkName(objectName)) {
                        break;
                    } else if (depth >= 0 && checkLevel(depth)) {
                        break;
                    }
                }
            }
        }
        return type;
    }

    private boolean checkName(String objectName) {
        return objectName.equals(key());
    }

    private boolean checkLevel(int level) {
        return level == depth;
    }

    @Override
    public EventType next() throws JSONException {
        boolean found = false;

        if (eod) {
           return currentEvent = EventType.END_DOCUMENT;
        }

        nullValue = false;

        while (!found) {
            try {
                currentSymbol = JSONSymbol.next();
            } catch(IOException e) {
                throw new JSONException(e, scanner.line(), scanner.column());
            }

            switch(currentSymbol) {
                case O:    //start object
                    key = null;
                    depth++;
                    return currentEvent = EventType.START_OBJECT;
                case O_:  // end object
                    key = null;
                    depth--;
                    return currentEvent = EventType.END_OBJECT;
                case OV:   //start object value
                    if (next() == EventType.STRING) {
                        key = JSONSymbol.getValue();
                    } // if next is not a string an exception is thrown by the JSON parser
                    stack.push(key);
                    nullValue = true;
                    return currentEvent = EventType.START_VALUE;
                case OV_: // end object value
                    key = stack.pop();
                    nullValue = true;
                    return currentEvent = EventType.END_VALUE;
                case X:   // start document
                    depth = -1;
                    stack.removeAllElements();
                    return currentEvent = EventType.START_DOCUMENT;
                case X_:  // end document
                    eod = true;
                    stack.removeAllElements();
                case EOS: // end of stream
                    if (!eod) {
                        throw new JSONException(JSONMessages.localize(null, "unexpected_end_of_stream"), scanner.line(), scanner.column());
                    }
                    depth = -1;
                    return currentEvent = EventType.END_DOCUMENT;
                case KEYWORD:
                    String s = JSONSymbol.getValue();
                    if (s.equals("true")) {
                        nullValue = true;
                        return currentEvent = EventType.TRUE;
                    } else if (s.equals("false")) {
                        nullValue = true;
                        return currentEvent = EventType.FALSE;
                    } else if (s.equals("null")) {
                        nullValue = true;
                        return currentEvent = EventType.NULL;
                    }
                    return currentEvent = EventType.ERROR;
                case STRING:
                    return currentEvent = EventType.STRING;
                case NUMBER:
                    if (scanner.isInteger()) {
                        return currentEvent = EventType.INTEGER;
                    }
                    return currentEvent = EventType.NUMBER;
                case A:    // start array
                    return currentEvent = EventType.START_ARRAY;
                case A_:    // end array
                    return currentEvent = EventType.END_ARRAY;
                case VA:   // start array element
                    return currentEvent = EventType.START_ARRAY_ELEMENT;
                case VA_:   // end array element
                    nullValue = true;
                    return currentEvent = EventType.END_ARRAY_ELEMENT;
                default:
                    found = false;
            }
        }
        return currentEvent = EventType.ERROR;
    }

    @Override
    public boolean hasNext() {
        return currentSymbol != JSONSymbol.X_;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String value() {
        return nullValue ? null : JSONSymbol.getValue();
    }

    @Override
    public int line() {
        return scanner.line();
    }

    @Override
    public int column() {
        return scanner.column();
    }

    @Override
    public long offset() {
        return scanner.getCharacterOffset();
    }

    @Override
    public int depth() {
        return depth;
    }

    @Override
    public String[] path() {
        String[] path = new String[stack.size()];
        Iterator<String> si = stack.iterator();
        int i = 0;
        while (si.hasNext()) {
            path[i++] = si.next();
        }
        return path;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String toString() {
        String keyString = key() == null ? "" : key();
        String valueString = value() == null ? "" : value();
        return "(" + scanner.line() + ":" + scanner.column() + ") " +
                currentEvent + " " +
                ("".equals(keyString) && "".equals(valueString) ? "" : "(" + keyString + ", " + valueString + ")");
    }

    @Override
    public JSONDocument build() {
        if (currentEvent != EventType.START_ARRAY && currentEvent != EventType.START_OBJECT) {
            // skip to start of array or object in order to start building
            for (EventType type = next();
                type != EventType.END_DOCUMENT && type != EventType.ERROR &&
                type != EventType.START_ARRAY && type != EventType.START_OBJECT;
                type = next()) {
            }
        }
        final Stack<JSONDocument> collectionStack = new Stack<JSONDocument>();
        switch (currentEvent) {
            case END_DOCUMENT:
                return null;
            case START_ARRAY:
                collection = new JSONDocument(JSONDocument.Type.ARRAY);
                break;
            case START_OBJECT:
                collection = new JSONDocument(JSONDocument.Type.OBJECT);
                break;
        }
        final JSONDocument root = collectionStack.push(collection);
        for (EventType type = next();
            type != EventType.END_DOCUMENT && type != EventType.ERROR && !collectionStack.empty();
            type = next()) {
            insert(type, collectionStack);
        }
        return root;
    }

    private void insert(final EventType type, Stack<JSONDocument> collectionStack) throws NumberFormatException {
        switch (type) {
            case TRUE:
                insert(Boolean.TRUE);
                break;
            case FALSE:
                insert(Boolean.FALSE);
                break;
            case NULL:
                insert(null);
                break;
            case STRING:
                insert(value());
                break;
            case INTEGER:
                insert(Long.parseLong(value()));
                break;
            case NUMBER:
                insert(Double.parseDouble(value()));
                break;
            case START_ARRAY:
                collectionStack.push(collection);
                final JSONDocument array = new JSONDocument(JSONDocument.Type.ARRAY);
                insert(array);
                collection = array;
                break;
            case END_ARRAY:
                collection = collectionStack.pop();
                break;
            case START_VALUE:
                collectionKey = key();
                break;
            case END_VALUE:
                collectionKey = null;
                break;
            case START_OBJECT:
                collectionStack.push(collection);
                final JSONDocument map = new JSONDocument(JSONDocument.Type.OBJECT);
                insert(map);
                collection = map;
                break;
            case END_OBJECT:
                collection = collectionStack.pop();
                break;
        }
    }

    private void insert(Object value) {
        if (collection.isArray()) {
            collection.array().add(value);
        } else if (collection.isObject()) {
            collection.object().put(collectionKey, value);
        } else {
            assert false;
        }
    }

    @Override
    public void close() {
        try {
            scanner.close();
        } catch (IOException ignore) {
            // TODO: log the exception
        }
    }
}
