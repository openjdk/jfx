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

package com.sun.javafx.css;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import javafx.scene.text.Font;

import org.junit.Test;

import com.sun.javafx.css.converters.SizeConverter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javafx.css.ParsedValue;

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
                new ParsedValueImpl<Size,Size>(new Size(100.0f, SizeUnits.PERCENT), null);
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
        ParsedValue<ParsedValue<?,Size>,Number> value =
            new ParsedValueImpl<ParsedValue<?,Size>,Number>(
                new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                SizeConverter.getInstance());

        double expResult = font.getSize();
        double result = value.convert(font).doubleValue();
        assertEquals(expResult, result, 0.01);
    }

    @Test
    public void testEquals() {

        Font font = Font.getDefault();
        Size size = new Size(1.0f, SizeUnits.EM);
        ParsedValue<ParsedValue<?,Size>,Number> value1 =
            new ParsedValueImpl<ParsedValue<?,Size>,Number>(
                new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                SizeConverter.getInstance());

        ParsedValue<ParsedValue<?,Size>,Number> value2 =
            new ParsedValueImpl<ParsedValue<?,Size>,Number>(
                new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                null);

        // ParsedValue.equals doesn't care about the converter
        assertTrue(value1.equals(value2));

        value1 =
            new ParsedValueImpl<ParsedValue<?,Size>,Number>(
                new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                SizeConverter.getInstance());

        value2 =
            new ParsedValueImpl<ParsedValue<?,Size>,Number>(
                new ParsedValueImpl<Size,Size>(new Size(2.0f, SizeUnits.EM), null),
                SizeConverter.getInstance());

        assertFalse(value1.equals(value2));

        value2 =
            new ParsedValueImpl<ParsedValue<?,Size>,Number>(
                new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.PX), null),
                SizeConverter.getInstance());

        assertFalse(value1.equals(value2));

        value2 =
            new ParsedValueImpl<ParsedValue<?,Size>,Number>(null, null);

        assertFalse(value1.equals(value2));

        ParsedValue<ParsedValue[],Number[]> value3 =
                new ParsedValueImpl<ParsedValue[],Number[]>(
                    new ParsedValueImpl[] {
                        new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                        new ParsedValueImpl<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                    }, SizeConverter.SequenceConverter.getInstance()
                );

        assertFalse(value1.equals(value3));
        assertFalse(value3.equals(value1));

        ParsedValue<ParsedValue[],Number[]> value4 =
                new ParsedValueImpl<ParsedValue[],Number[]>(
                    new ParsedValueImpl[] {
                        new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                        new ParsedValueImpl<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                    }, SizeConverter.SequenceConverter.getInstance()
                );

        assertTrue(value3.equals(value4));

        value4 =
                new ParsedValueImpl<ParsedValue[],Number[]>(
                    new ParsedValueImpl[] {
                        new ParsedValueImpl<Size,Size>(new Size(2.0f, SizeUnits.EM), null),
                        null
                    }, SizeConverter.SequenceConverter.getInstance()
                );

        assertFalse(value3.equals(value4));

        value4 =
                new ParsedValueImpl<ParsedValue[],Number[]>(
                    new ParsedValueImpl[] {
                        new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                        new ParsedValueImpl<Size,Size>(new Size(2.0f, SizeUnits.EM), null),
                        new ParsedValueImpl<Size,Size>(new Size(3.0f, SizeUnits.EM), null)
                    }, SizeConverter.SequenceConverter.getInstance()
                );

        assertFalse(value3.equals(value4));

        value4 =
                new ParsedValueImpl<ParsedValue[],Number[]>(
                    new ParsedValueImpl[] {
                        null
                    }, SizeConverter.SequenceConverter.getInstance()
                );

        assertFalse(value3.equals(value4));

        value4 =
                new ParsedValueImpl<ParsedValue[],Number[]>(
                    null,
                    SizeConverter.SequenceConverter.getInstance()
                );

        assertFalse(value3.equals(value4));

        ParsedValue<ParsedValue[][],Number[][]> value5 =
                new ParsedValueImpl<ParsedValue[][],Number[][]>(
                    new ParsedValueImpl[][] {
                        new ParsedValueImpl[] {
                            new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                            new ParsedValueImpl<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                        },
                        new ParsedValueImpl[] {
                            new ParsedValueImpl<Size,Size>(new Size(3.0f, SizeUnits.EM), null),
                            new ParsedValueImpl<Size,Size>(new Size(4.0f, SizeUnits.EM), null)
                        }
                    }, null
                );

        assertFalse(value1.equals(value5));
        assertFalse(value3.equals(value5));
        assertFalse(value5.equals(value1));
        assertFalse(value5.equals(value3));

        ParsedValue<ParsedValue[][],Number[][]> value6 =
                new ParsedValueImpl<ParsedValue[][],Number[][]>(
                    new ParsedValueImpl[][] {
                        new ParsedValueImpl[] {
                            new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                            new ParsedValueImpl<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                        },
                        new ParsedValueImpl[] {
                            new ParsedValueImpl<Size,Size>(new Size(3.0f, SizeUnits.EM), null),
                            new ParsedValueImpl<Size,Size>(new Size(4.0f, SizeUnits.EM), null)
                        }
                    }, null
                );

        assertTrue(value5.equals(value6));

        value6 =
                new ParsedValueImpl<ParsedValue[][],Number[][]>(
                    new ParsedValueImpl[][] {
                        new ParsedValueImpl[] {
                            new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                            new ParsedValueImpl<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                        },
                        new ParsedValueImpl[] {
                            new ParsedValueImpl<Size,Size>(new Size(3.0f, SizeUnits.EM), null),
                            new ParsedValueImpl<Size,Size>(new Size(5.0f, SizeUnits.EM), null),
                            new ParsedValueImpl<Size,Size>(new Size(4.0f, SizeUnits.EM), null)
                        }
                    }, null
                );

        assertFalse(value5.equals(value6));

        value6 =
                new ParsedValueImpl<ParsedValue[][],Number[][]>(
                    new ParsedValueImpl[][] {
                        new ParsedValueImpl[] {
                            new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                            new ParsedValueImpl<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                        },
                        new ParsedValueImpl[] {
                            new ParsedValueImpl<Size,Size>(new Size(3.0f, SizeUnits.EM), null),
                            null
                        }
                    }, null
                );

        assertFalse(value5.equals(value6));

        value6 =
                new ParsedValueImpl<ParsedValue[][],Number[][]>(
                    new ParsedValueImpl[][] {
                        new ParsedValueImpl[] {
                            new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                            new ParsedValueImpl<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                        },
                        null
                    }, null
                );

        assertFalse(value5.equals(value6));


    }
    
    private void writeBinary(ParsedValueImpl parsedValue) {

        try {
            StringStore stringStore = new StringStore();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            parsedValue.writeBinary(dos, stringStore);
            dos.close();
        } catch (IOException ioe) {
            org.junit.Assert.fail(parsedValue.toString());
        }
        
    }
    
    /**
     * Test of writeBinary method, of class ParsedValueImpl.
     */
    @Test
    public void testWriteReadBinary() throws Exception {
        Font font = Font.getDefault();
        Size size = new Size(1.0f, SizeUnits.EM);
        
        ParsedValueImpl parsedValue =
            new ParsedValueImpl<ParsedValue<?,Size>,Number>(
                new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                SizeConverter.getInstance());

        writeBinary(parsedValue);
        
        parsedValue =
                new ParsedValueImpl<ParsedValue[],Number[]>(
                    new ParsedValueImpl[] {
                        new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                        new ParsedValueImpl<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                    }, SizeConverter.SequenceConverter.getInstance()
                );

        writeBinary(parsedValue);
        
        parsedValue =
                new ParsedValueImpl<ParsedValue[][],Number[][]>(
                    new ParsedValueImpl[][] {
                        new ParsedValueImpl[] {
                            new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                            new ParsedValueImpl<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                        },
                        new ParsedValueImpl[] {
                            new ParsedValueImpl<Size,Size>(new Size(3.0f, SizeUnits.EM), null),
                            new ParsedValueImpl<Size,Size>(new Size(4.0f, SizeUnits.EM), null)
                        }
                    }, null
                );

        writeBinary(parsedValue);
        
        parsedValue =
                new ParsedValueImpl<ParsedValue[][],Number[][]>(
                    new ParsedValueImpl[][] {
                        new ParsedValueImpl[] {
                            new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                            new ParsedValueImpl<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                        },
                        new ParsedValueImpl[] {
                            new ParsedValueImpl<Size,Size>(new Size(3.0f, SizeUnits.EM), null),
                            null
                        }
                    }, null
                );

        writeBinary(parsedValue);
        
        parsedValue =
                new ParsedValueImpl<ParsedValue[][],Number[][]>(
                    new ParsedValueImpl[][] {
                        new ParsedValueImpl[] {
                            new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                            new ParsedValueImpl<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                        },
                        null
                    }, null
                );

        writeBinary(parsedValue);
        
    }

    private void writeAndReadBinary(ParsedValueImpl<?,?> parsedValue) {

        try {
            StringStore stringStore = new StringStore();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            parsedValue.writeBinary(dos, stringStore);
            dos.close();
            String[] strings = stringStore.strings.toArray(new String[]{});
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            DataInputStream dis = new DataInputStream(bais);
            ParsedValue<?,?> pv = ParsedValueImpl.readBinary(Stylesheet.BINARY_CSS_VERSION, dis, strings);
            org.junit.Assert.assertEquals(parsedValue, pv);
        } catch (IOException ioe) {
            org.junit.Assert.fail(parsedValue.toString());
        }
        
    }    
    /**
     * Test of readBinary method, of class ParsedValueImpl.
     */
    @Test
    public void testReadBinary() throws Exception {
        Font font = Font.getDefault();
        Size size = new Size(1.0f, SizeUnits.EM);
        
        ParsedValueImpl parsedValue =
            new ParsedValueImpl<ParsedValue<?,Size>,Number>(
                new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                SizeConverter.getInstance());

        writeAndReadBinary(parsedValue);
        
        parsedValue =
                new ParsedValueImpl<ParsedValue[],Number[]>(
                    new ParsedValueImpl[] {
                        new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                        new ParsedValueImpl<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                    }, SizeConverter.SequenceConverter.getInstance()
                );

        writeAndReadBinary(parsedValue);
        
        parsedValue =
                new ParsedValueImpl<ParsedValue[][],Number[][]>(
                    new ParsedValueImpl[][] {
                        new ParsedValueImpl[] {
                            new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                            new ParsedValueImpl<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                        },
                        new ParsedValueImpl[] {
                            new ParsedValueImpl<Size,Size>(new Size(3.0f, SizeUnits.EM), null),
                            new ParsedValueImpl<Size,Size>(new Size(4.0f, SizeUnits.EM), null)
                        }
                    }, null
                );

        writeAndReadBinary(parsedValue);
        
        parsedValue =
                new ParsedValueImpl<ParsedValue[][],Number[][]>(
                    new ParsedValueImpl[][] {
                        new ParsedValueImpl[] {
                            new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                            new ParsedValueImpl<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                        },
                        new ParsedValueImpl[] {
                            new ParsedValueImpl<Size,Size>(new Size(3.0f, SizeUnits.EM), null),
                            null
                        }
                    }, null
                );

        writeAndReadBinary(parsedValue);
        
        parsedValue =
                new ParsedValueImpl<ParsedValue[][],Number[][]>(
                    new ParsedValueImpl[][] {
                        new ParsedValueImpl[] {
                            new ParsedValueImpl<Size,Size>(new Size(1.0f, SizeUnits.EM), null),
                            new ParsedValueImpl<Size,Size>(new Size(2.0f, SizeUnits.EM), null)
                        },
                        null
                    }, null
                );

        writeAndReadBinary(parsedValue);
    }
}
