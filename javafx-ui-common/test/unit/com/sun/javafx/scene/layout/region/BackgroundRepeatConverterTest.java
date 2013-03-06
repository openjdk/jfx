/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.layout.region;

import javafx.scene.layout.BackgroundRepeat;
import org.junit.Ignore;
import org.junit.Test;
import javafx.css.ParsedValue;
import com.sun.javafx.css.ParsedValueImpl;

import static org.junit.Assert.assertEquals;
import com.sun.javafx.scene.layout.region.RepeatStructConverter;

/**
 */
public class BackgroundRepeatConverterTest {
    /*
        -fx-background-repeat:
     */
    @Test public void scenario1() {
        ParsedValueImpl<ParsedValue<String,BackgroundRepeat>[][], RepeatStruct[]> value =
                new ParsedValueImpl<ParsedValue<String,BackgroundRepeat>[][], RepeatStruct[]>(
                        new ParsedValueImpl[0][0], null
                );
        RepeatStruct[] results = RepeatStructConverter.getInstance().convert(value, null);
        assertEquals(0, results.length, 0);
    }

    /*
        -fx-background-repeat: null
     */
    @Ignore ("this doesn't work, but I'm not sure what would happen with a null background-repeat in reality")
    @Test public void scenario2() {
        ParsedValue<String,BackgroundRepeat>[][] values = new ParsedValueImpl[][] {
                {null}
        };

        ParsedValueImpl<ParsedValue<String,BackgroundRepeat>[][], RepeatStruct[]> value =
                new ParsedValueImpl<ParsedValue<String,BackgroundRepeat>[][], RepeatStruct[]>(
                        values, null
                );
        RepeatStruct[] results = RepeatStructConverter.getInstance().convert(value, null);
        assertEquals(0, results.length, 0);
    }

    /*
        -fx-background-repeat: repeat round
     */
    @Test public void scenario3() {
        ParsedValue<String,BackgroundRepeat>[][] values = new ParsedValueImpl[][] {
                { new ParsedValueImpl(BackgroundRepeat.REPEAT, null), new ParsedValueImpl(BackgroundRepeat.ROUND, null) }
        };

        ParsedValue<ParsedValue<String,BackgroundRepeat>[][], RepeatStruct[]> value =
                new ParsedValueImpl<ParsedValue<String,BackgroundRepeat>[][], RepeatStruct[]>(
                        values, null
                );
        RepeatStruct[] results = RepeatStructConverter.getInstance().convert(value, null);
        assertEquals(1, results.length, 0);
        assertEquals(BackgroundRepeat.REPEAT, results[0].repeatX);
        assertEquals(BackgroundRepeat.ROUND, results[0].repeatY);
    }

    /*
        -fx-background-repeat: space no-repeat
     */
    @Test public void scenario4() {
        ParsedValue<String,BackgroundRepeat>[][] values = new ParsedValueImpl[][] {
                { new ParsedValueImpl(BackgroundRepeat.SPACE, null), new ParsedValueImpl(BackgroundRepeat.NO_REPEAT, null) }
        };

        ParsedValue<ParsedValue<String,BackgroundRepeat>[][], RepeatStruct[]> value =
                new ParsedValueImpl<ParsedValue<String,BackgroundRepeat>[][], RepeatStruct[]>(
                        values, null
                );
        RepeatStruct[] results = RepeatStructConverter.getInstance().convert(value, null);
        assertEquals(1, results.length, 0);
        assertEquals(BackgroundRepeat.SPACE, results[0].repeatX);
        assertEquals(BackgroundRepeat.NO_REPEAT, results[0].repeatY);
    }

    /*
        -fx-background-repeat: no-repeat repeat, space round
     */
    @Test public void scenario5() {
        ParsedValue<String,BackgroundRepeat>[][] values = new ParsedValueImpl[][] {
                { new ParsedValueImpl(BackgroundRepeat.NO_REPEAT, null), new ParsedValueImpl(BackgroundRepeat.REPEAT, null) },
                { new ParsedValueImpl(BackgroundRepeat.SPACE, null), new ParsedValueImpl(BackgroundRepeat.ROUND, null) }
        };

        ParsedValue<ParsedValue<String,BackgroundRepeat>[][], RepeatStruct[]> value =
                new ParsedValueImpl<ParsedValue<String,BackgroundRepeat>[][], RepeatStruct[]>(
                        values, null
                );
        RepeatStruct[] results = RepeatStructConverter.getInstance().convert(value, null);
        assertEquals(2, results.length, 0);
        assertEquals(BackgroundRepeat.NO_REPEAT, results[0].repeatX);
        assertEquals(BackgroundRepeat.REPEAT, results[0].repeatY);
        assertEquals(BackgroundRepeat.SPACE, results[1].repeatX);
        assertEquals(BackgroundRepeat.ROUND, results[1].repeatY);
    }

}
