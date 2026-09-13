/*
 * Copyright (c) 2017, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.sun.javafx.PlatformUtil;
import sun.awt.datatransfer.ClipboardTransferable;
import sun.awt.datatransfer.SunClipboard;
import test.util.Util;


public class ClipboardTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Clipboard clipboard;


    public static void main(String[] args) throws Exception {
        initFX();
        try {
            ClipboardTest test = new ClipboardTest();
            test.testCopyUTF8String();
            test.testPasteUTF8String();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            teardown();
        }
    }

    @BeforeAll
    public static void initFX() {
        Util.startup(startupLatch, () -> {
            clipboard = Clipboard.getSystemClipboard();
            startupLatch.countDown();
        });
    }

    @AfterAll
    public static void teardown() {
        Util.shutdown();
    }

    @Test
    public void testCopyUTF8String() throws Exception {
        String text = new String(new byte[]{
                0x20, (byte) 0x4a, (byte) 0x75, (byte) 0x6d, (byte) 0x70, (byte) 0x20,
                (byte) 0x74, (byte) 0x6f, (byte) 0x3a, (byte) 0x20, (byte) 0xf0,
                (byte) 0x9f, (byte) 0x98, (byte) 0x83, (byte) 0xf0, (byte) 0x9f,
                (byte) 0x92, (byte) 0x81, (byte) 0x20, (byte) 0x4a, (byte) 0x75,
                (byte) 0x6d, (byte) 0x70
        }, "UTF-8");

        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Util.runAndWait(() -> clipboard.setContent(content));
        Thread.sleep(1000);

        assertEquals(text, Toolkit.getDefaultToolkit()
                        .getSystemClipboard().getData(DataFlavor.stringFlavor));
    }

    @Test
    public void testPasteUTF8String() throws Exception {
        String text = new String(new byte[]{
                0x20, (byte) 0x4a, (byte) 0x75, (byte) 0x6d, (byte) 0x70, (byte) 0x20,
                (byte) 0x74, (byte) 0x6f, (byte) 0x3a, (byte) 0x20, (byte) 0xf0,
                (byte) 0x9f, (byte) 0x98, (byte) 0x83, (byte) 0xf0, (byte) 0x9f,
                (byte) 0x92, (byte) 0x81, (byte) 0x20, (byte) 0x4a, (byte) 0x75,
                (byte) 0x6d, (byte) 0x70
        }, "UTF-8");

        Toolkit.getDefaultToolkit()
             .getSystemClipboard().setContents(new StringSelection(text), null);

        Thread.sleep(1000);
        Util.runAndWait(() ->
               assertEquals(text, clipboard.getContent(DataFormat.PLAIN_TEXT)));
    }

    private void testClipboard(List<List<Integer>> codePointsLists, boolean checkResults) {
        codePointsLists.stream().forEach(codePointsList -> {
            int[] codePoints = codePointsList.stream()
                    .mapToInt(Integer::intValue)
                    .toArray();
            String text = new String(codePoints, 0, codePoints.length);
            ClipboardContent content = new ClipboardContent();
            content.put(DataFormat.PLAIN_TEXT, text);
            Util.runAndWait(() -> clipboard.setContent(content));
            if (checkResults) {
                Util.runAndWait(() ->
                       assertEquals(text, clipboard.getContent(DataFormat.PLAIN_TEXT)));
            }
        });
    }

    /*
     * @test 8304441
     *
     * Test bad strings with mismatched halves of surrogate pairs.
     */
    @Test
    public void testCopyBadSurrogate() {
        List<List<Integer>> codePointsLists = List.of(
            List.of(0xD800),             // High
            List.of(0xDBFF),             // High
            List.of(0xD83D),             // High
            List.of(0xDC00),             // Low
            List.of(0xDFFF),             // Low
            List.of(0xD800, 0xDBFF),     // High, High
            List.of(0xDC00, 0xDFFF),     // Low, Low
            List.of(0xDC00, 0xD800),     // Low, High
            List.of(0xDFFF, 0xDBFF),     // Low, High
            List.of(0x1F600, 0xD800),    // High, Low, High
            List.of(0xD800, 0x1F600),    // High, High, Low
            List.of(0x41, 0xD83D, 0x5A), // 'A', High, 'Z'
            List.of(0x41, 0xDDDD, 0x5A)  // 'A', Low, 'Z'
        );

        testClipboard(codePointsLists, false);
    }

    /*
     * @test 8304441
     *
     * Test good strings with matched surrogate pairs.
     */
    @Test
    public void testCopyPasteSurrogatePairs() {
        List<List<Integer>> codePointsLists = List.of(
            List.of(0xD800, 0xDC00),    // High, Low
            List.of(0xDBFF, 0xDFFF),    // High, Low
            List.of(0x1F600),
            List.of(0x1F603),
            List.of(0x1F976),
            List.of(0x1F600, 0x1F603, 0x1F976),
            List.of(0x41, 0x1F600, 0x1F603, 0x1F976, 0x5A)
        );

        testClipboard(codePointsLists, true);
    }

    /*
     * @bug 8274929
     */
    @Test
    public void testCustomClipboard() throws Exception {
        // Test is only valid on Windows
        assumeTrue(PlatformUtil.isWindows());

        // Put custom content on the system clipboard (via AWT)
        Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new ClipboardTransferable(new CustomClipboard()), null);

        Util.runAndWait(() -> {
            // Read the system Clipboard
            boolean hasString = clipboard.hasString();
            assertFalse(hasString);
        });
    }

    private static final class CustomClipboard extends SunClipboard {

        private static final int CF_HDROP = 15;

        // Custom Drag and drop data to put on the system clipboard (using AWT)
        private static final byte[] CF_HDROP_BYTES = new byte[] {
            // HEADER
            0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,
            // D:\aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
            0x44, 0x00, 0x3A, 0x00, 0x5C, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00,
            0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00,
            0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00,
            0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00,
            0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00,
            0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00,
            0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00,
            0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00,
            0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00,
            0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00,
            0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00,
            0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00,
            0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00,
            0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00,
            0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00,
            0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00,
            0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00,
            0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00,
            0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00,
            0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00,
            0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00, 0x61, 0x00,
            // terminate entry
            0x00, 0x00,
            // terminate content
            0x00, 0x00,
        };

        public CustomClipboard() {
            super("CustomClipboard");
        }

        @Override
        public long getID() {
            return 0;
        }

        @Override
        protected void clearNativeContext() {
            // no-op
        }

        @Override
        protected void setContentsNative(final Transferable transferable) {
            // no-op
        }

        @Override
        protected long[] getClipboardFormats() {
            return new long[] { CF_HDROP };
        }

        @Override
        protected byte[] getClipboardData(final long id) {
            if (id == CF_HDROP) {
                return CF_HDROP_BYTES;
            } else {
                return new byte[0];
            }
        }

        @Override
        protected void registerClipboardViewerChecked() {
            // no-op
        }

        @Override
        protected void unregisterClipboardViewerChecked() {
            // no-op
        }

    }

}
