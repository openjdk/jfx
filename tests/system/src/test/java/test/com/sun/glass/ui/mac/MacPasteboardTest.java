/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.glass.ui.mac;

import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.mac.MacPasteboardShim;
import com.sun.javafx.PlatformUtil;
import javafx.application.Platform;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static test.util.Util.runAndWait;

public class MacPasteboardTest {

    private static final CountDownLatch startupLatch = new CountDownLatch(1);

    private static MacPasteboardShim macPasteboardShim;

    @BeforeClass
    public static void setup() throws Exception {
        if (PlatformUtil.isMac()) {
            Platform.startup(() -> {
                macPasteboardShim = new MacPasteboardShim();
                startupLatch.countDown();
            });
        }
    }

    @AfterClass
    public static void teardown() {
        if (PlatformUtil.isMac()) {
            Platform.exit();
        }
    }

    @Test
    public void testValidLocalImageURLMacPasteboard() throws Exception {
        assumeTrue(PlatformUtil.isMac());
        final String localImage = getClass().getResource("blue.png").toURI().toURL().toString();
        runAndWait(() -> {
            macPasteboardShim.pushMacPasteboard(new HashMap<>(Map.of(Clipboard.URI_TYPE, localImage)));
            Object content = macPasteboardShim.popMacPasteboard(Clipboard.RAW_IMAGE_TYPE);
            assertTrue("The content was not a raw image", content instanceof Pixels);

            Pixels pixels = (Pixels) content;
            assertEquals("The raw image width", 64, pixels.getWidth());
            assertEquals("The raw image height", 64, pixels.getHeight());
        });
    }

    @Test
    public void testDataBase64ImageMacPasteboard() {
        assumeTrue(PlatformUtil.isMac());
        final String encodedImage = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAKCAIAAAA7N+mxAAAAAXNSR0IArs4c6QAAAAR"
                + "nQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAcSURBVChTY/jPwADBZACyNMHAqGYSwZDU/P8/ABieT81GAGKoAAAAAElFTkSuQmCC";
        runAndWait(() -> {
            macPasteboardShim.pushMacPasteboard(new HashMap<>(Map.of(Clipboard.URI_TYPE, encodedImage)));
            Object content = macPasteboardShim.popMacPasteboard(Clipboard.RAW_IMAGE_TYPE);
            assertNull("The content was not null", content);
        });
    }

    @Test
    public void testNotAnImageURLMacPasteboard() {
        assumeTrue(PlatformUtil.isMac());
        final String invalidImage = "not.an.image.url";
        runAndWait(() -> {
            macPasteboardShim.pushMacPasteboard(new HashMap<>(Map.of(Clipboard.URI_TYPE, invalidImage)));
            Object content = macPasteboardShim.popMacPasteboard(Clipboard.RAW_IMAGE_TYPE);
            assertNull("The content was not null", content);
        });
    }
}
