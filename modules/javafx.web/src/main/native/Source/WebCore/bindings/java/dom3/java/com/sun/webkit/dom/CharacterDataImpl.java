/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.dom;

import java.lang.annotation.Native;
import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class CharacterDataImpl extends NodeImpl implements CharacterData {
    @Native public static final byte DIRECTIONALITY_LEFT_TO_RIGHT = Character.DIRECTIONALITY_LEFT_TO_RIGHT;
    @Native public static final byte DIRECTIONALITY_RIGHT_TO_LEFT = Character.DIRECTIONALITY_RIGHT_TO_LEFT;
    @Native public static final byte DIRECTIONALITY_EUROPEAN_NUMBER = Character.DIRECTIONALITY_EUROPEAN_NUMBER;
    @Native public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR = Character.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR;
    @Native public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR = Character.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR;
    @Native public static final byte DIRECTIONALITY_ARABIC_NUMBER = Character.DIRECTIONALITY_ARABIC_NUMBER;
    @Native public static final byte DIRECTIONALITY_COMMON_NUMBER_SEPARATOR = Character.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR;
    @Native public static final byte DIRECTIONALITY_PARAGRAPH_SEPARATOR = Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR;
    @Native public static final byte DIRECTIONALITY_SEGMENT_SEPARATOR = Character.DIRECTIONALITY_SEGMENT_SEPARATOR;
    @Native public static final byte DIRECTIONALITY_WHITESPACE = Character.DIRECTIONALITY_WHITESPACE;
    @Native public static final byte DIRECTIONALITY_OTHER_NEUTRALS = Character.DIRECTIONALITY_OTHER_NEUTRALS;
    @Native public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING = Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING;
    @Native public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE = Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE;
    @Native public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC = Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
    @Native public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING = Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING;
    @Native public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE = Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE;
    @Native public static final byte DIRECTIONALITY_POP_DIRECTIONAL_FORMAT = Character.DIRECTIONALITY_POP_DIRECTIONAL_FORMAT;
    @Native public static final byte DIRECTIONALITY_NONSPACING_MARK = Character.DIRECTIONALITY_NONSPACING_MARK;
    @Native public static final byte DIRECTIONALITY_BOUNDARY_NEUTRAL = Character.DIRECTIONALITY_BOUNDARY_NEUTRAL;
    @Native public static final byte UNASSIGNED = Character.UNASSIGNED;
    @Native public static final byte UPPERCASE_LETTER = Character.UPPERCASE_LETTER;
    @Native public static final byte LOWERCASE_LETTER = Character.LOWERCASE_LETTER;
    @Native public static final byte TITLECASE_LETTER = Character.TITLECASE_LETTER;
    @Native public static final byte MODIFIER_LETTER = Character.MODIFIER_LETTER;
    @Native public static final byte OTHER_LETTER = Character.OTHER_LETTER;

    @Native public static final byte NON_SPACING_MARK = Character.NON_SPACING_MARK;
    @Native public static final byte ENCLOSING_MARK = Character.ENCLOSING_MARK;
    @Native public static final byte COMBINING_SPACING_MARK = Character.COMBINING_SPACING_MARK;

    @Native public static final byte DECIMAL_DIGIT_NUMBER = Character.DECIMAL_DIGIT_NUMBER;
    @Native public static final byte LETTER_NUMBER = Character.LETTER_NUMBER;
    @Native public static final byte OTHER_NUMBER = Character.OTHER_NUMBER;

    @Native public static final byte SPACE_SEPARATOR = Character.SPACE_SEPARATOR;
    @Native public static final byte LINE_SEPARATOR = Character.LINE_SEPARATOR;
    @Native public static final byte PARAGRAPH_SEPARATOR = Character.PARAGRAPH_SEPARATOR;

    @Native public static final byte CONTROL = Character.CONTROL;
    @Native public static final byte FORMAT = Character.FORMAT;
    @Native public static final byte PRIVATE_USE = Character.PRIVATE_USE;
    @Native public static final byte SURROGATE = Character.SURROGATE;

    @Native public static final byte DASH_PUNCTUATION = Character.DASH_PUNCTUATION;
    @Native public static final byte START_PUNCTUATION = Character.START_PUNCTUATION;
    @Native public static final byte END_PUNCTUATION = Character.END_PUNCTUATION;
    @Native public static final byte CONNECTOR_PUNCTUATION = Character.CONNECTOR_PUNCTUATION;
    @Native public static final byte OTHER_PUNCTUATION = Character.OTHER_PUNCTUATION;

    @Native public static final byte MATH_SYMBOL = Character.MATH_SYMBOL;
    @Native public static final byte CURRENCY_SYMBOL = Character.CURRENCY_SYMBOL;
    @Native public static final byte MODIFIER_SYMBOL = Character.MODIFIER_SYMBOL;
    @Native public static final byte OTHER_SYMBOL = Character.OTHER_SYMBOL;

    @Native public static final byte INITIAL_QUOTE_PUNCTUATION = Character.INITIAL_QUOTE_PUNCTUATION;
    @Native public static final byte FINAL_QUOTE_PUNCTUATION = Character.FINAL_QUOTE_PUNCTUATION;

    CharacterDataImpl(long peer) {
        super(peer);
    }

    static Node getImpl(long peer) {
        return (Node)create(peer);
    }


// Attributes
    public String getData() {
        return getDataImpl(getPeer());
    }
    native static String getDataImpl(long peer);

    public void setData(String value) {
        setDataImpl(getPeer(), value);
    }
    native static void setDataImpl(long peer, String value);

    public int getLength() {
        return getLengthImpl(getPeer());
    }
    native static int getLengthImpl(long peer);

    public Element getPreviousElementSibling() {
        return ElementImpl.getImpl(getPreviousElementSiblingImpl(getPeer()));
    }
    native static long getPreviousElementSiblingImpl(long peer);

    public Element getNextElementSibling() {
        return ElementImpl.getImpl(getNextElementSiblingImpl(getPeer()));
    }
    native static long getNextElementSiblingImpl(long peer);


// Functions
    public String substringData(int offset
        , int length) throws DOMException
    {
        return substringDataImpl(getPeer()
            , offset
            , length);
    }
    native static String substringDataImpl(long peer
        , int offset
        , int length);


    public void appendData(String data)
    {
        appendDataImpl(getPeer()
            , data);
    }
    native static void appendDataImpl(long peer
        , String data);


    public void insertData(int offset
        , String data) throws DOMException
    {
        insertDataImpl(getPeer()
            , offset
            , data);
    }
    native static void insertDataImpl(long peer
        , int offset
        , String data);


    public void deleteData(int offset
        , int length) throws DOMException
    {
        deleteDataImpl(getPeer()
            , offset
            , length);
    }
    native static void deleteDataImpl(long peer
        , int offset
        , int length);


    public void replaceData(int offset
        , int length
        , String data) throws DOMException
    {
        replaceDataImpl(getPeer()
            , offset
            , length
            , data);
    }
    native static void replaceDataImpl(long peer
        , int offset
        , int length
        , String data);


    public void remove() throws DOMException
    {
        removeImpl(getPeer());
    }
    native static void removeImpl(long peer);


}

