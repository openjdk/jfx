/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.input;

import org.junit.Ignore;
import org.junit.Test;


@Ignore("Pasteboard removed, need to move these tests elsewhere")
public class PasteboardTest  {

    @Test
    public void testDefaultPasteboard() {
//        Pasteboard pb = new Pasteboard();
//        assertNull(pb.getContentType());
//        assertNull(pb.getContent());
//        assertNull(pb.getContent(null));
//        assertNull(pb.getContent(DataFormat.DATA));
//        assertNull(pb.getContent());
//
//        assertFalse(pb.hasAnyContent());
//        assertFalse(pb.hasContent(DataFormat.DATA));
//        assertFalse(pb.hasImage());
//        assertFalse(pb.hasImages());
//        assertFalse(pb.hasFile());
//        assertFalse(pb.hasString());
    }
    
    @Test
    public void testPasteboardString() {
//        Pasteboard pb = new Pasteboard();
//        assertFalse(pb.hasAnyContent());
//        assertFalse(pb.hasContent(DataFormat.TEXT));
//        assertFalse(pb.hasString());
//
//        // put in new string to pasteboard
//        pb.placeString("text");
//        assertTrue(pb.hasAnyContent());
//        assertTrue(pb.hasContent(DataFormat.TEXT));
//        assertTrue(pb.hasString());
//        assertEquals("text", pb.getString());
//
//        // replace it with a different string
//        pb.placeString("newText");
//        assertTrue(pb.hasAnyContent());
//        assertTrue(pb.hasContent(DataFormat.TEXT));
//        assertTrue(pb.hasString());
//        assertEquals("newText", pb.getString());
//
//        // A String is DataFormat.TEXT, it may or may not be plain text, so
//        // after placing a string, this should be false.
//        assertFalse(pb.hasContent(DataFormat.PLAIN_TEXT));
//
//        pb.clear();
//
//        assertFalse(pb.hasAnyContent());
//        assertFalse(pb.hasContent(DataFormat.TEXT));
//        assertFalse(pb.hasString());
    }

    /*
     * This test depends on StubToolkit.loadImage working, which at this point
     * it doesn't.
     */

//    public function testPasteboardImage() {
//        var pb = Pasteboard { };
//        assertFalse(pb.hasAnyContent());
//        assertFalse(pb.hasContent(DataFormat.TEXT));
//        assertFalse(pb.hasContent(DataFormat.IMAGE));
//        assertFalse(pb.hasString());
//        assertFalse(pb.hasImage());
//
//        // create an image - even if the URL isn't valid
//        var image = Image {
//            url: "{__DIR__}JavaFX.png"
//        }
//
//        assertNotNull(image);
//
//        pb.placeImage(image);
//        assertTrue(pb.hasAnyContent());
//        assertTrue(pb.hasContent(DataFormat.IMAGE));
//        assertEquals(image, pb.getImage());
//
//        assertFalse(pb.hasImages());
//
//        // as much as we'd like to know that it is a PNG image, we can't be
//        // certain - all we know is that the image is a DataFormat.IMAGE
//        assertFalse(pb.hasContent(DataFormat.IMAGE_PNG));
//        assertFalse(pb.hasString());
//
//        pb.clear();
//        assertFalse(pb.hasAnyContent());
//        assertFalse(pb.hasContent(DataFormat.TEXT));
//        assertFalse(pb.hasContent(DataFormat.IMAGE));
//        assertFalse(pb.hasString());
//        assertFalse(pb.hasImage());
//    }
}

