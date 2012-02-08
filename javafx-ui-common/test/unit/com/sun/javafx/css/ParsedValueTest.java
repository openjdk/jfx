/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistribution of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistribution in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.

 * This software is provided "AS IS," without a warranty of any kind.
 * ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN") AND
 * ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A
 * RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 * IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT
 * OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR
 * PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed, licensed or intended for
 * use in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

package com.sun.javafx.css;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import javafx.scene.text.Font;

import org.junit.Test;

import com.sun.javafx.css.converters.SizeConverter;


public class ParsedValueTest {

    public ParsedValueTest() {
    }

    /**
     * Test of getValue method, of class ParsedValue.
     */
    @Test
    public void testGetValue() {
        //System.out.println("getValue");
        ParsedValue<Size,Size> instance =
                new ParsedValue<Size,Size>(new Size(100.0f, SizeUnits.PERCENT), null);
        Size expResult = new Size(100.0f, SizeUnits.PERCENT);;
        Size result = instance.getValue();
        assertEquals(expResult, result);
    }

    /**
     * Test of convert method, of class ParsedValue.
     */
    @Test
    public void testConvert() {
        ///System.out.println("convert");
        Font font = Font.getDefault();
        Size size = new Size(1.0f, SizeUnits.EM);
        ParsedValue<ParsedValue<?,Size>,Double> value =
            new ParsedValue<ParsedValue<?,Size>,Double>(
                new ParsedValue<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                SizeConverter.getInstance());

        double expResult = font.getSize();
        double result = value.convert(font);
        assertEquals(expResult, result, 0.01);
    }

    @Test
    public void testEquals() {
        ///System.out.println("convert");
        Font font = Font.getDefault();
        Size size = new Size(1.0f, SizeUnits.EM);
        ParsedValue<ParsedValue<?,Size>,Double> value1 =
            new ParsedValue<ParsedValue<?,Size>,Double>(
                new ParsedValue<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                SizeConverter.getInstance());

        ParsedValue<ParsedValue<?,Size>,Double> value2 =
            new ParsedValue<ParsedValue<?,Size>,Double>(
                new ParsedValue<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                SizeConverter.getInstance());

        value1 =
            new ParsedValue<ParsedValue<?,Size>,Double>(
                new ParsedValue<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                SizeConverter.getInstance());

        value2 =
            new ParsedValue<ParsedValue<?,Size>,Double>(
                new ParsedValue<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                null);

        // ParsedValue.equals doesn't care about the converter
        assertTrue(value1.equals(value2));

        value1 =
            new ParsedValue<ParsedValue<?,Size>,Double>(
                new ParsedValue<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                SizeConverter.getInstance());

        value2 =
            new ParsedValue<ParsedValue<?,Size>,Double>(
                new ParsedValue<Size,Size>(new Size(2.0f, SizeUnits.EM), null),
                SizeConverter.getInstance());

        assertFalse(value1.equals(value2));

        value2 =
            new ParsedValue<ParsedValue<?,Size>,Double>(
                new ParsedValue<Size,Size>(new Size(1.0f, SizeUnits.PX), null),
                SizeConverter.getInstance());

        assertFalse(value1.equals(value2));

        value2 =
            new ParsedValue<ParsedValue<?,Size>,Double>(null, null);

        assertFalse(value1.equals(value2));

        ParsedValue<ParsedValue<?,Size>[],Double[]> value3 =
                new ParsedValue<ParsedValue<?,Size>[],Double[]>(
                    new ParsedValue[] {
                        new ParsedValue<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                        new ParsedValue<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                    }, SizeConverter.SequenceConverter.getInstance()
                );

        assertFalse(value1.equals(value3));
        assertFalse(value3.equals(value1));

        ParsedValue<ParsedValue<?,Size>[],Double[]> value4 =
                new ParsedValue<ParsedValue<?,Size>[],Double[]>(
                    new ParsedValue[] {
                        new ParsedValue<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                        new ParsedValue<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                    }, SizeConverter.SequenceConverter.getInstance()
                );

        assertTrue(value3.equals(value4));

        value4 =
                new ParsedValue<ParsedValue<?,Size>[],Double[]>(
                    new ParsedValue[] {
                        new ParsedValue<Size,Size>(new Size(2.0f, SizeUnits.EM), null),
                        null
                    }, SizeConverter.SequenceConverter.getInstance()
                );

        assertFalse(value3.equals(value4));

        value4 =
                new ParsedValue<ParsedValue<?,Size>[],Double[]>(
                    new ParsedValue[] {
                        new ParsedValue<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                        new ParsedValue<Size,Size>(new Size(2.0f, SizeUnits.EM), null),
                        new ParsedValue<Size,Size>(new Size(3.0f, SizeUnits.EM), null)
                    }, SizeConverter.SequenceConverter.getInstance()
                );

        assertFalse(value3.equals(value4));

        value4 =
                new ParsedValue<ParsedValue<?,Size>[],Double[]>(
                    new ParsedValue[] {
                        null
                    }, SizeConverter.SequenceConverter.getInstance()
                );

        assertFalse(value3.equals(value4));

        value4 =
                new ParsedValue<ParsedValue<?,Size>[],Double[]>(
                    null,
                    SizeConverter.SequenceConverter.getInstance()
                );

        assertFalse(value3.equals(value4));

        ParsedValue<ParsedValue<?,Size>[][],Double[][]> value5 =
                new ParsedValue<ParsedValue<?,Size>[][],Double[][]>(
                    new ParsedValue[][] {
                        new ParsedValue[] {
                            new ParsedValue<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                            new ParsedValue<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                        },
                        new ParsedValue[] {
                            new ParsedValue<Size,Size>(new Size(3.0f, SizeUnits.EM), null),
                            new ParsedValue<Size,Size>(new Size(4.0f, SizeUnits.EM), null)
                        }
                    }, null
                );

        assertFalse(value1.equals(value5));
        assertFalse(value3.equals(value5));
        assertFalse(value5.equals(value1));
        assertFalse(value5.equals(value3));

        ParsedValue<ParsedValue<?,Size>[][],Double[][]> value6 =
                new ParsedValue<ParsedValue<?,Size>[][],Double[][]>(
                    new ParsedValue[][] {
                        new ParsedValue[] {
                            new ParsedValue<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                            new ParsedValue<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                        },
                        new ParsedValue[] {
                            new ParsedValue<Size,Size>(new Size(3.0f, SizeUnits.EM), null),
                            new ParsedValue<Size,Size>(new Size(4.0f, SizeUnits.EM), null)
                        }
                    }, null
                );

        assertTrue(value5.equals(value6));

        value6 =
                new ParsedValue<ParsedValue<?,Size>[][],Double[][]>(
                    new ParsedValue[][] {
                        new ParsedValue[] {
                            new ParsedValue<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                            new ParsedValue<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                        },
                        new ParsedValue[] {
                            new ParsedValue<Size,Size>(new Size(3.0f, SizeUnits.EM), null),
                            new ParsedValue<Size,Size>(new Size(5.0f, SizeUnits.EM), null),
                            new ParsedValue<Size,Size>(new Size(4.0f, SizeUnits.EM), null)
                        }
                    }, null
                );

        assertFalse(value5.equals(value6));

        value6 =
                new ParsedValue<ParsedValue<?,Size>[][],Double[][]>(
                    new ParsedValue[][] {
                        new ParsedValue[] {
                            new ParsedValue<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                            new ParsedValue<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                        },
                        new ParsedValue[] {
                            new ParsedValue<Size,Size>(new Size(3.0f, SizeUnits.EM), null),
                            null
                        }
                    }, null
                );

        assertFalse(value5.equals(value6));

        value6 =
                new ParsedValue<ParsedValue<?,Size>[][],Double[][]>(
                    new ParsedValue[][] {
                        new ParsedValue[] {
                            new ParsedValue<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                            new ParsedValue<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                        },
                        null
                    }, null
                );

        assertFalse(value5.equals(value6));


    }
    /**
     * Test of readBinary method, of class ParsedValue.
     */
//    @Test
//    public void testReadBinary() throws Exception {
//        System.out.println("readBinary");
//        DataInputStream stream = null;
//        String[] strings = null;
//        ParsedValue expResult = null;
//        ParsedValue result = ParsedValue.readBinary(stream, strings);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of writeBinary method, of class ParsedValue.
     */
//    @Test
//    public void testWriteBinary() throws Exception {
//        System.out.println("writeBinary");
//        DataOutputStream stream = null;
//        StringStore ss = null;
//        ParsedValue instance = null;
//        instance.writeBinary(stream, ss);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

}
