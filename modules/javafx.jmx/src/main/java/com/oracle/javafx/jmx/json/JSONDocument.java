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

import java.io.Writer;
import java.io.StringWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * An in-memory representation of a JSON node. Can represent an entire
 * JSON document, or a fragment. Can represent the root node, or
 * intermediate child nodes. Can represent either an OBJECT or an
 * ARRAY at a time, never both. The type is specified at construction
 * time and is immutable.
 *
 */
public class JSONDocument implements Iterable<JSONDocument> {

    /**
     * URI for use in {@link javax.xml.xpath.XPathFactory} to specify
     * a subset of {@link javax.xml.xpath.XPath} over JSON.
     */
    public static final String JSON_XPATH_URI = "http://javafx.com/json/xpath";

    /**
     * An empty array.
     */
    public static final JSONDocument EMPTY_ARRAY = new ImmutableJSONDocument(Type.ARRAY);

    /**
     * An empty object.
     */
    public static final JSONDocument EMPTY_OBJECT = new ImmutableJSONDocument(Type.OBJECT);

    /**
     * The type of a JSON node. Can be an ARRAY or an OBJECT.
     */
    public enum Type { ARRAY, OBJECT }

    private final Type type;
    private final List<Object> array;
    private final Map<String, Object> object;

    public static JSONDocument createObject() {
        return new JSONDocument(Type.OBJECT, 0);
    }

    public static JSONDocument createArray() {
        return JSONDocument.createArray(0);
    }

    public static JSONDocument createArray(int length) {
        return new JSONDocument(Type.ARRAY, length);
    }

    /**
     * Constructs an empty node of the specified type. Child elements may be
     * added subsequently by adding them to the corresponding collection
     * obtained by calling array() or object().
     *
     * @param type the type of the node, an ARRAY or an OBJECT.
     */
    public JSONDocument(final Type type) {
        this(type, 0);
    }

    private JSONDocument(final Type type, int length) {
        this.type = type;
        if (type == Type.ARRAY) {
            final Vector<Object> v = new Vector<Object>(length);
            v.setSize(length);
            array = v;
            object = null;
        } else if (type == Type.OBJECT) {
            array = null;
            object = new LinkedHashMap<String, Object>();
        } else {
            array = null;
            object = null;
            throw new IllegalArgumentException();
        }
    }

    /**
     * Returns the type of the node
     *
     * @return the type of the node, an ARRAY or an OBJECT
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the array representation of the node.
     *
     * @return the node as a List. Returns an empty List
     * if this node is not an ARRAY type
     */
    public List<Object> array() {
        if (type != Type.ARRAY) {
            return EMPTY_ARRAY.array();
        }
        return array;
    }

    /**
     * Returns the map representation of the node.
     *
     * @return the node as a Map. Returns an empty Map
     * if this node is not an OBJECT type
     */
    public Map<String, Object> object() {
        if (type != Type.OBJECT) {
            return EMPTY_OBJECT.object();
        }
        return object;
    }

    /**
     * Test if the node is an ARRAY.
     *
     * @return true if the node is an ARRAY; false otherwise.
     */
    public boolean isArray() {
        return type == Type.ARRAY && array != null;
    }

    /**
     * Test if the node is an OBJECT.
     *
     * @return true if the node is an OBJECT; false otherwise.
     */
    public boolean isObject() {
        return type == Type.OBJECT && object != null;
    }

    /**
     * Writes a string representation of the node and its children,
     * without newlines or whitespace, to the supplied Writer.
     *
     * @see #toJSON() for a variant that returns a String
     * @see #toString() for a human-readable representation
     * @throws IOException if there are errors writing to the Writer
     */
    public void toJSON(Writer writer) throws IOException {
        printJSON(writer, isArray() ? array() : object(), 0, false);
    }

    /**
     * Returns a string representation of the node and its children,
     * without newlines or whitespace.
     *
     * @return the node and its children as a string.
     * @see #toString() for a human-readable representation
     */
    public String toJSON() {
        StringWriter sb = new StringWriter(4096);
        try {
            printJSON(sb, isArray() ? array() : object(), 0, false);
        } catch (IOException ignore) {
        }
        return sb.getBuffer().toString();
    }

    @SuppressWarnings("unchecked")
    private void printJSON(Writer sb, Object obj, int depth, boolean pretty) throws IOException {
        if (obj instanceof JSONDocument) {
            final JSONDocument doc = (JSONDocument) obj;
            if (doc.isArray()) {
                printArray(sb, doc.array(), depth, pretty);
            } else if (doc.isObject()) {
                printObject(sb, doc.object(), depth, pretty);
            } else {
                assert false;
            }
        } else if (obj instanceof List) {
            printArray(sb, (List<Object>) obj, depth, pretty);
        } else if (obj instanceof Map) {
            printObject(sb, (Map<String, Object>) obj, depth, pretty);
        } else if (obj instanceof String) {
            sb.append("\"");
            printEscapedString(sb, (String)obj);
            sb.append("\"");
        } else if (obj != null) {
            sb.append(obj.toString());
        } else {
            sb.append("null");
        }
    }

    private void printArray(Writer sb, List<Object> obj, int depth, boolean pretty) throws IOException {
        Iterator<Object> i = obj.iterator();
        int size = obj.size();
        int count = 0;

        sb.append("[");
        if (size > 1) {
            depth++;
            prettyPrint(sb, depth, pretty);
        }
        while (i.hasNext()) {
            printJSON(sb, i.next(), depth, pretty);
            if (count < size - 1) {
                sb.append(",");
                prettyPrint(sb, depth, pretty);
            }
            count++;
        }
        if (size > 1) {
            depth--;
            prettyPrint(sb, depth, pretty);
        }
        sb.append("]");
    }

    private void printObject(Writer sb, Map<String, Object> obj, int depth, boolean pretty) throws IOException {
        Iterator<Map.Entry<String, Object>> i = obj.entrySet().iterator();
        int size = obj.size();
        int count = 0;

        sb.append("{");
        if (size > 1) {
            depth++;
            prettyPrint(sb, depth, pretty);
        }

        while (i.hasNext()) {
            Map.Entry<String, Object> me = i.next();
            sb.append("\"");
            sb.append(me.getKey());
            sb.append("\":");

            final Object value = me.getValue();
            if (pretty && size > 1) {
                int objSize = 0;
                if (value instanceof JSONDocument) {
                    final JSONDocument doc = (JSONDocument) value;
                    if (doc.isArray()) {
                        objSize = doc.array().size();
                    } else if (doc.isObject()) {
                        objSize = doc.object().size();
                    } else {
                        assert false;
                    }
                }
                if (objSize > 0) {
                    prettyPrint(sb, depth, pretty);
                }
            }
            printJSON(sb, value, depth, pretty);
            if (count < size - 1) {
                sb.append(",");
                prettyPrint(sb, depth, pretty);
            }
            count++;
        }

        if (size > 1) {
            depth--;
            prettyPrint(sb, depth, pretty);
        }
        sb.append("}");
    }

    static void printEscapedString(Writer sb, String s) throws IOException {
        char[] ca = s.toCharArray();
        for (int i = 0; i < ca.length; i++) {
            if (ca[i] == '"') {
                sb.append("\\\"");
            } else if (ca[i] == '\'') {
                sb.append("'");
            } else if (ca[i] == '\\') {
                sb.append("\\\\");
            } else if (ca[i] == '/') {
                sb.append("\\/");
            } else if (ca[i] == 0x7) {
                sb.append("\\a");
            } else if (ca[i] == 0x8) {
                sb.append("\\b");
            } else if (ca[i] == 0x9) {
                sb.append("\\t");
            } else if (ca[i] == 0xA) {
                sb.append("\\n");
            } else if (ca[i] == 0xB) {
                sb.append("\\v");
            } else if (ca[i] == 0xC) {
                sb.append("\\f");
            } else if (ca[i] == 0xD) {
                sb.append("\\r");
            } else if (ca[i] == 0x0) {
                sb.append("\\0");
            } else if (ca[i] > 0x7F && ca[i] < 0xFFFF) {
                sb.append("\\u");
                sb.append(String.format("%04X", (int) ca[i]));
            } else {
                sb.append(ca[i]);
            }
        }
    }

    private void prettyPrint(Writer sb, int depth, boolean pretty) throws IOException {
        if (pretty) {
            sb.append("\n");
            for (int i = 0; i < depth; i++) {
                sb.append(" ");
            }
        }
    }

    @Override
    public String toString() {
        StringWriter sb = new StringWriter(4096);
        try {
            printJSON(sb, isArray() ? array() : object(), 0, true);
        } catch (IOException ignore) {
        }
        return sb.getBuffer().toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JSONDocument) {
            final JSONDocument doc = (JSONDocument) obj;
            return isArray() ? array().equals(doc.array()) : isObject() ? object().equals(doc.object()) : false;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (isArray() ? array().hashCode() : isObject() ? object().hashCode() : 0);
        return hash;
    }

    /**
     * Get child of this object node. Returns an empty object if this node
     * is not an OBJECT, if the specified child is not found, or if the
     * specified child is not itself a node. This method can be used for
     * recursive descent into the object / array hierarchy.
     *
     * @param key the key for the current object node whose value
     * is to be retrieved
     * @return child node if found, an empty OBJECT node otherwise
     */
    public JSONDocument get(String key) {
        if (object != null) {
            final Object value = object().get(key);
            if (value instanceof JSONDocument) {
                return (JSONDocument) value;
            }
        }
        return EMPTY_OBJECT;
    }

    /**
     * Get child of this array node. Returns an empty array if this node
     * is not an ARRAY, if the specified child is not found, or if the
     * specified child is not itself a node. This method can be used for
     * recursive descent into the object / array hierarchy.
     *
     * @param index the index for the current array node at which the node
     * is to be retrieved
     * @return child node if found, an empty ARRAY node otherwise
     */
    public JSONDocument get(int index) {
        if (array != null) {
            final Object value = array().get(index);
            if (value instanceof JSONDocument) {
                return (JSONDocument) value;
            }
        }
        return EMPTY_ARRAY;
    }

    /**
     * Get child object of this object node as a Map. Returns an empty
     * object if this node is not an OBJECT, if the specified child is
     * not found, or if the specified child is not an object.
     *
     * @param key the key for the current object node whose value
     * is to be retrieved
     * @return child node if found, an empty OBJECT node otherwise
     */
    public Map<String, Object> getMap(String key) {
        final Object obj = get(key, JSONDocument.class);
        if (obj instanceof JSONDocument) {
            return getMap((JSONDocument) obj);
        }
        return EMPTY_OBJECT.object();
    }

    /**
     * Get child object of this array node as a Map. Returns an empty
     * object if this node is not an ARRAY, if the specified child is
     * not found, or if the specified child is not an object.
     *
     * @param index the index for the current array node at which the node
     * is to be retrieved
     * @return child node if found, an empty OBJECT node otherwise
     */
    public Map<String, Object> getMap(int index) {
        final Object obj = get(index, JSONDocument.class);
        if (obj instanceof JSONDocument) {
            return getMap((JSONDocument) obj);
        }
        return EMPTY_OBJECT.object();
    }

    /**
     * Get child array of this object node as a List. Returns an empty
     * list if this node is not an OBJECT, if the specified child is
     * not found, or if the specified child is not an array.
     *
     * @param key the key for the current object node whose value
     * is to be retrieved
     * @return child node if found, an empty ARRAY node otherwise
     */
    public List<Object> getList(String key) {
        final Object obj = get(key, JSONDocument.class);
        if (obj instanceof JSONDocument) {
            return getList((JSONDocument) obj);
        }
        return EMPTY_ARRAY.array();
    }

    /**
     * Get child array of this array node as a List. Returns an empty
     * array if this node is not an ARRAY, if the specified child is
     * not found, or if the specified child is not an array.
     *
     * @param index the index for the current array node at which the node
     * is to be retrieved
     * @return child node if found, an empty ARRAY node otherwise
     */
    public List<Object> getList(int index) {
        final Object obj = get(index, JSONDocument.class);
        if (obj instanceof JSONDocument) {
            return getList((JSONDocument) obj);
        }
        return EMPTY_ARRAY.array();
    }

    /**
     * Get the object node's named value as a String.
     *
     * @param key the name of the value
     * @return the value as String
     */
    public String getString(String key) {
        return (String) get(key, String.class);
    }

    /**
     * Get the array node's value at specified index as a String.
     *
     * @param index the array index whose value is to be returned
     * @return the array value as String
     */
    public String getString(int index) {
        return (String) get(index, String.class);
    }

    /**
     * Get the object node's named value as a Boolean.
     *
     * @param key the name of the value
     * @return the value as Boolean
     */
    public Boolean getBoolean(String key) {
        return (Boolean) get(key, Boolean.class);
    }

    /**
     * Get the array node's value at specified index as a Boolean.
     *
     * @param index the array index whose value is to be returned
     * @return the array value as Boolean
     */
    public Boolean getBoolean(int index) {
        return (Boolean) get(index, Boolean.class);
    }

    /**
     * Get the object node's named value as a Number.
     *
     * @param key the name of the value
     * @return the value as Number
     */
    public Number getNumber(String key) {
        return (Number) get(key, Number.class);
    }

    /**
     * Get the array node's value at specified index as a Number.
     *
     * @param index the array index whose value is to be returned
     * @return the array value as Number
     */
    public Number getNumber(int index) {
        return (Number) get(index, Number.class);
    }

    /**
     * Get if the object node's named value is null.
     *
     * @param key the name of the value
     * @return true if the value is null, false otherwise
     */
    public boolean isNull(String key) {
        if (object != null) {
            return null == object().get(key);
        }
        return false;
    }

    /**
     * Get if the array node's value at specified index is null.
     *
     * @param index the array index whose value is to be checked
     * @return true if the value is null, false otherwise
     */
    public boolean isNull(int index) {
        if (array != null) {
            return null == array().get(index);
        }
        return false;
    }

    private Map<String, Object> getMap(final JSONDocument doc) {
        if (doc != null) {
            return doc.object();
        }
        return EMPTY_OBJECT.object();
    }

    private List<Object> getList(final JSONDocument doc) {
        if (doc != null) {
            return doc.array();
        }
        return EMPTY_ARRAY.array();
    }

    private Object get(final String key, final Class<?> type) {
        if (object != null) {
            final Object value = object().get(key);
            if (type.isInstance(value)) {
                return type.cast(value);
            }
        }
        return null;
    }

    private Object get(final int index, final Class<?> type) {
        if (array != null) {
            final Object value = array().get(index);
            if (type.isInstance(value)) {
                return type.cast(value);
            }
        }
        return null;
    }

    /**
     * Set child object of this node.
     *
     * @param key the name of the child
     * @param value the node containing the new values
     * @return the old value, null if there was no value
     */
    public JSONDocument set(String key, JSONDocument child) {
        return (JSONDocument) set(key, child, JSONDocument.class);
    }

    /**
     * Set child object of this node.
     *
     * @param index the index at which the child is to be set
     * @param value the node containing the new values
     * @return the old value, null if there was no value
     */
    public JSONDocument set(int index, JSONDocument value) {
        return (JSONDocument) set(index, value, JSONDocument.class);
    }

    /**
     * Set value of this node as String.
     *
     * @param key the name of the value
     * @param value the new value
     * @return the old value, null if there was no value
     */
    public String setString(String key, String value) {
        return (String) set(key, value, String.class);
    }

    /**
     * Set value of this node as String.
     *
     * @param index the index at which the value is to be set
     * @param value the new value
     * @return the old value, null if there was no value
     */
    public String setString(int index, String value) {
        return (String) set(index, value, String.class);
    }

    /**
     * Set value of this node as Boolean.
     *
     * @param key the name of the value
     * @param value the new value
     * @return the old value, null if there was no value
     */
    public Boolean setBoolean(String key, Boolean value) {
        return (Boolean) set(key, value, Boolean.class);
    }

    /**
     * Set value of this node as Boolean.
     *
     * @param index the index at which the value is to be set
     * @param value the new value
     * @return the old value, null if there was no value
     */
    public Boolean setBoolean(int index, Boolean value) {
        return (Boolean) set(index, value, Boolean.class);
    }

    /**
     * Set value of this node as Number.
     *
     * @param key the name of the value
     * @param value the new value
     * @return the old value, null if there was no value
     */
    public Number setNumber(String key, Number value) {
        return (Number) set(key, value, Number.class);
    }

    /**
     * Set value of this node as Number.
     *
     * @param index the index at which the value is to be set
     * @param value the new value
     * @return the old value, null if there was no value
     */
    public Number setNumber(int index, Number value) {
        return (Number) set(index, value, Number.class);
    }

    /**
     * Set value of this node to null.
     *
     * @param key the name of the value
     * @return the old value, null if there was no value
     */
    public Object setNull(String key) {
        if (object != null) {
            return object.put(key, null);
        }
        return null;
    }

    /**
     * Set value of this node to null.
     *
     * @param index the index at which the value is to be set
     * @param value the new value
     * @return the old value, null if there was no value
     */
    public Object setNull(int index) {
        if (array != null) {
            return array.set(index, null);
        }
        return null;
    }

    private Object set(final String key, final Object value, final Class<?> type) {
        if (object != null) {
            if (type.isInstance(value)) {
                return type.cast(object().put(key, value));
            }
        }
        return null;
    }

    private Object set(final int index, final Object value, final Class<?> type) {
        if (array != null) {
            if (type.isInstance(value)) {
                return type.cast(array().set(index, value));
            }
        }
        return null;
    }

   static class IteratorWrapper implements Iterator<JSONDocument> {

       final Iterator<Object> iterator;

       IteratorWrapper(final Iterator<Object> iterator) {
           this.iterator = iterator;
       }

       @Override
       public void remove() {
           iterator.remove();
       }

       @Override
       public JSONDocument next() {
           final Object value = iterator.next();
           if (value instanceof JSONDocument) {
               return (JSONDocument) value;
           }
           return null;
       }

       @Override
       public boolean hasNext() {
           return iterator.hasNext();
       }
   }

    @Override
    public IteratorWrapper iterator() {
        return new IteratorWrapper((isObject() ?
                object().values().iterator() :
                array().iterator()));
    }

}
