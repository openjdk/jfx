/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.web;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


class JSONDecoder {

    private final JS2JavaBridge owner;

    public JSONDecoder(JS2JavaBridge owner) {
        this.owner = owner;
    }

    public Object decode(String string) {
        return decode(new StringCharacterIterator(string));
    }

    // decodes object starting from it.current()
    private Object decode(CharacterIterator it) {
        char ch = getSignificant(it);
        switch (ch) {
            case '"': {
                String s = decodeString(it);
                if (s != null && !s.isEmpty()) {
                    String prefix = s.substring(0, 1);
                    String value = s.substring(1);
                    if ("o".equals(prefix)) { //javascript object id (index to exportedJSObjects[])
                        Object o = owner.getJavaObjectForjsId(value);
                        if (o != null) {
                            return o;
                        } else {
                            return new JSObjectIosImpl(owner, value);
                        }
                    } else if ("s".equals(prefix)) { //javascript String object or string literal
                        return value;
                    }
                }
                return s;//"undefined"
            }
            case '[':
                return decodeArray(it);
            case '{':
                return decodeObject(it);
            case 'n': // null?
                if (!"null".equals(getString(it, 4))) {
                    throw new IllegalArgumentException("decoding error (null)");
                }
                return null;
            case 't': // true?
                if (!"true".equals(getString(it, 4))) {
                    throw new IllegalArgumentException("decoding error (true)");
                }
                return Boolean.TRUE;
            case 'f': // false?
                if (!"false".equals(getString(it, 5))) {
                    throw new IllegalArgumentException("decoding error (false)");
                }
                return Boolean.FALSE;
        }
        // try to decode a number
        StringBuilder sb = new StringBuilder();
        while ("+-0123456789.Ee".indexOf(ch) >= 0) {
            sb.append(ch);
            ch = it.next();
        }
        String sNum = sb.toString();
        if (sNum.indexOf('.') >= 0 || sNum.indexOf('E') >= 0 || sNum.indexOf('e') >= 0) {
            return Double.valueOf(sNum);
        } else {
            long val = Long.parseLong(sNum);
            if ((val <= Integer.MAX_VALUE) && (Integer.MIN_VALUE <= val)) {
                return Integer.valueOf((int) val);
            } else {
                return Double.valueOf(val);
            }
        }
    }

    // iterates until it.current points to a significant symbol (non-whitespace)
    private char getSignificant(CharacterIterator it) {
        char ch = it.current();
        while (Character.isWhitespace(ch) && ch != CharacterIterator.DONE) {
            ch = it.next();
        }
        return ch;
    }

    // returns a len-symbol string starting from it.currect
    // on return it.current points to next char after the last string symbol
    private String getString(CharacterIterator it, int len) {
        char[] buffer = new char[len];
        for (int i=0; i<len; i++) {
            buffer[i] = it.current();
            it.next();
        }
        return new String(buffer);
    }

    // decodes string (it.current points to starting quote)
    private String decodeString(CharacterIterator it) {
        StringBuilder sb = new StringBuilder();
        // it.current is quote, skip it (start from it.next)
        for (char ch = it.next(); ch != '"'; ch=it.next()) {
            if (ch == CharacterIterator.DONE) {
                throw new IllegalArgumentException("Unterminated string");
            } else if (ch == '\\') {
                switch (ch = it.next()) {
                    case '"':
                    case '\\':
                    case '/':
                        sb.append(ch);
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        int n = 0;
                        for (int i=0; i<4; i++) {
                            n = (n << 4) + dehex(it.next());
                        }
                        sb.append((char)n);
                        break;
                    default:
                        throw new IllegalArgumentException("Illegal escape sequence");
                }
            } else {
                sb.append(ch);
            }
        }
        it.next();  // skip final quote, NOP if DONE
        return sb.toString();
    }

    private int dehex(char ch) {
        if (ch >= '0' && ch <= '9') {
            return ch - '0';
        }
        if (ch >= 'a' && ch <= 'f') {
            return ch - 'a' + 10;
        }
        if (ch >= 'A' && ch <= 'F') {
            return ch - 'A' + 10;
        }
        throw new IllegalArgumentException("Wrong unicode value");
    }

    // decodes array (it.current points to '[')
    private Object[] decodeArray(CharacterIterator it) {
        ArrayList arr = new ArrayList();

        it.next();  // skip '['
        char ch = getSignificant(it);
        while (ch != ']') {
            Object obj = decode(it);
            arr.add(obj);

            ch = getSignificant(it);
            switch (ch) {
                case ',':
                    it.next();  // skip ','
                    break;  // continue with next pair
                case ']':
                    break;
                default:
                    throw new IllegalArgumentException("Array decoding error (expect ']' or ',')");
            }
        }
        it.next();  // skip final ']'

        return arr.toArray();
    }

    // decodes object (it.current points to '{')
    private Object decodeObject(CharacterIterator it) {
        Map<String, Object> map = new HashMap<String, Object>();
        it.next();  // skip '{'
        char ch = getSignificant(it);
        while (ch != '}') {
            if (getSignificant(it) != '"') {
                throw new IllegalArgumentException("Object decoding error (key should be a string)");
            }
            String key = decodeString(it);

            if (getSignificant(it) != ':') {
                throw new IllegalArgumentException("Object decoding error (expect ':')");
            }
            it.next();

            Object value = decode(it);
            map.put(key, value);

            ch = getSignificant(it);
            switch (ch) {
                case ',':
                    it.next();  // skip ','
                    break;  // continue with next pair
                case '}':
                    break;
                default:
                    throw new IllegalArgumentException("Object decoding error (expect '}' or ',')");
            }
        }
        it.next();  // skip final '}'

        return map;
    }
}
