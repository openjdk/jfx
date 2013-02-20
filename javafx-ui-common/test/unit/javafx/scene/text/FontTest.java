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

package javafx.scene.text;

import static com.sun.javafx.test.TestHelper.assertImmutableList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class FontTest {

    @Test
    public void testGetFamilies() {
        List<String> families = Font.getFamilies();
        assertNotNull(families);
        assertImmutableList(families);
    }

    @Test
    public void testGetAllFontNames() {
        List<String> names = Font.getFontNames();
        assertNotNull(names);
        assertImmutableList(names);
    }
    
    @Test
    public void testGetFontNames() {
        String family = Font.getFamilies().get(0);
        List<String> names = Font.getFontNames(family);
        assertNotNull(names);
        assertImmutableList(names);
    }
    
    @Test
    public void testFontFactory1() {
        Font font = Font.font("Amble", FontWeight.NORMAL,
                              FontPosture.ITALIC, 30);
        assertEquals("Amble", font.getFamily());
        assertEquals("Amble Italic", font.getName());
        assertEquals(30f, (float) font.getSize());
        
        font = Font.font(null, null, null, -1);
        // The tests against System as default are all commented out
        // as it needs a re-working of StubFontLoader which I consider
        // pointless as its not testing the product, and may give a
        // fall belief that the product passes. 
        //assertEquals("System", font.getFamily());
        assertTrue(0 < font.getSize());
    }

    @Test
    public void testFontFactory2() {
        Font font = Font.font("Amble", FontWeight.BOLD, 30);
        assertEquals("Amble", font.getFamily());
        assertEquals("Amble Bold", font.getName());
        assertEquals(30f, (float) font.getSize());
    }
    
    @Test
    public void testFontFactory3() {
        Font font = Font.font("Amble", FontPosture.ITALIC, 30);
        assertEquals("Amble", font.getFamily());
        assertEquals("Amble Italic", font.getName());
        assertEquals(30f, (float) font.getSize());
    }
    
    @Test
    public void testDefault() {
        Font font = Font.getDefault();
        //assertEquals("System", font.getFamily());
    }
    
    @Test
    public void testCtor2() {
        Font font = new Font(20);
        //assertEquals("System", font.getFamily());
        assertEquals(20f, (float) font.getSize());
    }
    
    @Test
    public void testCtor3() {
        Font font = new Font("Amble Bold", 32);
        assertEquals("Amble", font.getFamily());
        assertEquals(32f, (float) font.getSize());
        assertEquals("Amble Bold", font.getName());
        
        Font def = new Font(null, -1);
        //assertEquals("System Regular", def.getName());
        assertTrue(0 < def.getSize());
    }
    
    @Test
    public void testToString() {
        assertNotNull(new Font(12).toString());
    }
    
    @Test
    public void testEqualsHashCode() {
        Font f1 = new Font(12);
        Font f2 = new Font(12);
        assertEquals(f1, f2);
        assertEquals(f1.hashCode(), f2.hashCode());
        Font f3 = new Font(40);
        assertNotSame(f1, f3);
        assertNotSame(f1.hashCode(), f3.hashCode());
        Font f4 = new Font(40);
        assertEquals(f3, f4);
    }
    
    @Test
    public void testSetNative() {
        new Font(12).impl_setNativeFont(new Object(), "", "", "");
        // no assumptions
    }
    
/*
 * Sad to say, all these tests are not useful since 'ant test' uses
 * a stub toolkit and font loader which cannot implement these, since
 * they require real code behind them.
 * Therefore this is one big comment block
 */
//     @Test
//     public void testLoadFontFromLocalFileURL() {
//         // First make sure we are on Windows and can find the file
//         String os = System.getProperty("os.name");
//         if (!os.startsWith("Win")) {
//             return;
//         }
//         String path = "c:\\windows\\fonts\\times.ttf";
//         if (!((new File(path)).canRead())) {
//              return;
//         }
//         float sz = 20f;
//         Font font = Font.loadFont(path, sz);
//         assertNotNull(font);
//         assertEquals("Times New Roman", font.getFamily());
//         assertEquals(sz, (float) font.getSize());
//     }

//     @Test
//     public void testLoadFontFromInputStream() {
//         // First make sure we are on Windows and can find the file
//         String os = System.getProperty("os.name");
//         if (!os.startsWith("Win")) {
//             return;
//         }
//         String path = "c:\\windows\\fonts\\times.ttf";
//         if (!((new File(path)).canRead())) {
//              return;
//         }
//         try {
//              InputStream is = new FileInputStream(path);
//              float sz = 20f;
//              Font font = Font.loadFont(is, sz);
//              assertNotNull(font);
//              assertEquals("Times New Roman", font.getFamily());
//              assertEquals(sz, (float) font.getSize());
//         } catch (IOException e) {
//              assertNull("Unexpected Exception", e);
//         }
//     }

//     @Test
//     public void testLoadFontFromBadFileURL() {
//         Font font = Font.loadFont("non-existent file", 10f);
//         assertNull(font);
//     }

//     @Test
//     public void testLoadFontFromBadStream() {
//         InputStream is = new ByteArrayInputStream(new byte[100]);
//         Font font = Font.loadFont(is, 10f);
//         assertNull(font);
//     }

}
