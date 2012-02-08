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
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package javafx.scene;

import com.sun.javafx.pgstub.StubImageLoaderFactory;
import com.sun.javafx.pgstub.StubPlatformImageInfo;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.image.Image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class CursorTest {

    @Test public void test_cursorFromUppercaseName() {
        assertEquals(Cursor.DEFAULT, Cursor.cursor("DEFAULT"));
        assertEquals(Cursor.CLOSED_HAND, Cursor.cursor("CLOSED_HAND"));
        assertEquals(Cursor.CROSSHAIR, Cursor.cursor("CROSSHAIR"));
        assertEquals(Cursor.DISAPPEAR, Cursor.cursor("DISAPPEAR"));
        assertEquals(Cursor.E_RESIZE, Cursor.cursor("E_RESIZE"));
        assertEquals(Cursor.MOVE, Cursor.cursor("MOVE"));
        assertEquals(Cursor.NE_RESIZE, Cursor.cursor("NE_RESIZE"));
        assertEquals(Cursor.NONE, Cursor.cursor("NONE"));
        assertEquals(Cursor.NW_RESIZE, Cursor.cursor("NW_RESIZE"));
        assertEquals(Cursor.N_RESIZE, Cursor.cursor("N_RESIZE"));
        assertEquals(Cursor.OPEN_HAND, Cursor.cursor("OPEN_HAND"));
        assertEquals(Cursor.SE_RESIZE, Cursor.cursor("SE_RESIZE"));
        assertEquals(Cursor.SW_RESIZE, Cursor.cursor("SW_RESIZE"));
        assertEquals(Cursor.S_RESIZE, Cursor.cursor("S_RESIZE"));
        assertEquals(Cursor.TEXT, Cursor.cursor("TEXT"));
        assertEquals(Cursor.V_RESIZE, Cursor.cursor("V_RESIZE"));
        assertEquals(Cursor.WAIT, Cursor.cursor("WAIT"));
        assertEquals(Cursor.W_RESIZE, Cursor.cursor("W_RESIZE"));
    }

    @Test public void test_cursorFromLowercaseName() {
        assertEquals(Cursor.DEFAULT, Cursor.cursor("default"));
        assertEquals(Cursor.CLOSED_HAND, Cursor.cursor("closed_hand"));
        assertEquals(Cursor.CROSSHAIR, Cursor.cursor("crosshair"));
        assertEquals(Cursor.DISAPPEAR, Cursor.cursor("disappear"));
        assertEquals(Cursor.E_RESIZE, Cursor.cursor("e_resize"));
        assertEquals(Cursor.MOVE, Cursor.cursor("move"));
        assertEquals(Cursor.NE_RESIZE, Cursor.cursor("ne_resize"));
        assertEquals(Cursor.NONE, Cursor.cursor("none"));
        assertEquals(Cursor.NW_RESIZE, Cursor.cursor("nw_resize"));
        assertEquals(Cursor.N_RESIZE, Cursor.cursor("n_resize"));
        assertEquals(Cursor.OPEN_HAND, Cursor.cursor("open_hand"));
        assertEquals(Cursor.SE_RESIZE, Cursor.cursor("se_resize"));
        assertEquals(Cursor.SW_RESIZE, Cursor.cursor("sw_resize"));
        assertEquals(Cursor.S_RESIZE, Cursor.cursor("s_resize"));
        assertEquals(Cursor.TEXT, Cursor.cursor("text"));
        assertEquals(Cursor.V_RESIZE, Cursor.cursor("v_resize"));
        assertEquals(Cursor.WAIT, Cursor.cursor("wait"));
        assertEquals(Cursor.W_RESIZE, Cursor.cursor("w_resize"));
    }

    @Test public void test_cursorFromUrl() {
        final StubImageLoaderFactory imageLoaderFactory =
                ((StubToolkit) Toolkit.getToolkit()).getImageLoaderFactory();

        final String imageUrl = "file:cursor.png";
        imageLoaderFactory.registerImage(
                imageUrl, new StubPlatformImageInfo(100, 100));

        final Cursor cursor = Cursor.cursor(imageUrl);
        assertTrue(cursor instanceof ImageCursor);

        final Image cursorImage = ((ImageCursor) cursor).getImage();
        assertEquals(imageUrl, cursorImage.impl_getUrl());
    }

    @Test(expected=NullPointerException.class)
    public void test_cursorFromNull() {
        Cursor.cursor(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_cursorFromInvalidUrl() {
        Cursor.cursor("file//:cursor.png");
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_cursorFromInvalidName() {
        Cursor.cursor("crosslair");
    }
}
