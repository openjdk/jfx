/*
 * Copyright (c) 2000, 2015, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.input;

import java.util.ArrayList;
import java.io.File;
import java.util.List;
import test.com.sun.javafx.pgstub.StubPlatformImageInfo;
import test.com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import java.util.Arrays;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClipboardContentTest {

    @Test
    public void stringShouldBePut() {
        ClipboardContent cc = new ClipboardContent();

        assertFalse(cc.hasString());

        cc.putString("Hello");

        assertTrue(cc.hasString());
        assertEquals("Hello", cc.getString());
    }

    @Test
    public void nullStringShouldRemovePrevious() {
        ClipboardContent cc = new ClipboardContent();
        cc.putString("Hello");
        cc.putString(null);
        assertFalse(cc.hasString());
        assertNull(cc.getString());
    }

    @Test
    public void urlShouldBePut() {
        ClipboardContent cc = new ClipboardContent();

        assertFalse(cc.hasUrl());

        cc.putUrl("http://hello");

        assertTrue(cc.hasUrl());
        assertEquals("http://hello", cc.getUrl());
    }

    @Test
    public void nullUrlShouldRemovePrevious() {
        ClipboardContent cc = new ClipboardContent();
        cc.putUrl("http://hello");
        cc.putUrl(null);
        assertFalse(cc.hasUrl());
        assertNull(cc.getUrl());
    }

    @Test
    public void htmlShouldBePut() {
        ClipboardContent cc = new ClipboardContent();

        assertFalse(cc.hasHtml());

        cc.putHtml("<html><head></head><body>Hello</body></html>");

        assertTrue(cc.hasHtml());
        assertEquals("<html><head></head><body>Hello</body></html>", cc.getHtml());
    }

    @Test
    public void nullHtmlShouldRemovePrevious() {
        ClipboardContent cc = new ClipboardContent();
        cc.putHtml("<html><head></head><body>Hello</body></html>");
        cc.putHtml(null);
        assertFalse(cc.hasHtml());
        assertNull(cc.getHtml());
    }

    @Test
    public void rtfShouldBePut() {
        ClipboardContent cc = new ClipboardContent();

        assertFalse(cc.hasRtf());

        cc.putRtf("{\\rtf1\\ansi\\uc1{\\colortbl;\\red255\\green0\\blue0;}\\uc1\\b\\i FRED\\par rtf\\par text}");

        assertTrue(cc.hasRtf());
        assertEquals("{\\rtf1\\ansi\\uc1{\\colortbl;\\red255\\green0\\blue0;}\\uc1\\b\\i FRED\\par rtf\\par text}",
                cc.getRtf());
    }

    @Test
    public void nullRtfShouldRemovePrevious() {
        ClipboardContent cc = new ClipboardContent();
        cc.putRtf("{\\rtf1\\ansi\\uc1{\\colortbl;\\red255\\green0\\blue0;}\\uc1\\b\\i FRED\\par rtf\\par text}");
        cc.putRtf(null);
        assertFalse(cc.hasRtf());
        assertNull(cc.getRtf());
    }

    @Test
    public void imageShouldBePut() {
        StubToolkit toolkit = (StubToolkit) Toolkit.getToolkit();
        toolkit.getImageLoaderFactory().reset();
        toolkit.getImageLoaderFactory().registerImage("file:test.png",
                new StubPlatformImageInfo(100, 200));

        ClipboardContent cc = new ClipboardContent();
        Image i = new Image("file:test.png");

        assertFalse(cc.hasImage());


        cc.putImage(i);

        assertTrue(cc.hasImage());
        assertSame(i, cc.getImage());
    }

    @Test
    public void nullImageShouldRemovePrevious() {
        StubToolkit toolkit = (StubToolkit) Toolkit.getToolkit();
        toolkit.getImageLoaderFactory().reset();
        toolkit.getImageLoaderFactory().registerImage("file:test.png",
                new StubPlatformImageInfo(100, 200));

        ClipboardContent cc = new ClipboardContent();
        Image i = new Image("file:test.png");

        cc.putImage(i);
        cc.putImage(null);
        assertFalse(cc.hasImage());
        assertNull(cc.getImage());
    }

    @Test
    public void filesShouldBePut() {

        ClipboardContent cc = new ClipboardContent();
        List<File> files = Arrays.asList(new File("."), new File("/"));

        assertFalse(cc.hasFiles());

        cc.putFiles(files);

        assertTrue(cc.hasFiles());
        assertEquals(files, cc.getFiles());
    }

    @Test
    public void noFilesShouldBePut() {

        ClipboardContent cc = new ClipboardContent();

        assertFalse(cc.hasFiles());

        cc.putFiles(new ArrayList<File>(0));

        assertTrue(cc.hasFiles());
        assertEquals(0, cc.getFiles().size());
    }

    @Test
    public void nullFilesShouldRemovePrevious() {
        ClipboardContent cc = new ClipboardContent();
        List<File> files = Arrays.asList(new File("."), new File("/"));

        cc.putFiles(files);
        cc.putFiles(null);
        assertFalse(cc.hasFiles());
        assertNull(cc.getFiles());
    }

    @Test
    public void filesShouldBePutByPath() {

        ClipboardContent cc = new ClipboardContent();
        List<File> files = Arrays.asList(new File("."), new File("/"));

        assertFalse(cc.hasFiles());

        cc.putFilesByPath(Arrays.asList(".", "/"));

        assertTrue(cc.hasFiles());
        assertEquals(files, cc.getFiles());
    }

    @Test
    public void noFilesShouldBePutByPath() {

        ClipboardContent cc = new ClipboardContent();

        assertFalse(cc.hasFiles());

        cc.putFilesByPath(new ArrayList<String>(0));

        assertTrue(cc.hasFiles());
        assertEquals(0, cc.getFiles().size());
    }

    @Test(expected=NullPointerException.class)
    public void nullFilesByPathShouldRemoveFiles() {
        ClipboardContent cc = new ClipboardContent();
        cc.putFilesByPath(new ArrayList<String>(0));
        cc.putFilesByPath(null);

        assertFalse(cc.hasFiles());
        assertNull(cc.getFiles());
    }
}
