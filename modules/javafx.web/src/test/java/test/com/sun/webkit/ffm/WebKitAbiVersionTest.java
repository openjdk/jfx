/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.webkit.ffm;

import com.sun.webkit.WebKitNativeShim;
import com.sun.webkit.WkjStubShim;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The ABI version guard (contract section 5). Its whole purpose is to turn "an old prebuilt library
 * is on the library path" from an obscure crash into one readable sentence, so the sentence itself
 * is as much on test here as the guard is.
 */
@Tag("ffm")
public class WebKitAbiVersionTest {

    private static final int WKJ_INIT_OK = 0;
    private static final int WKJ_INIT_ERR_ABI_VERSION = -2;
    private static final int WKJ_INIT_ERR_HOST_SIZE = -3;
    private static final int WKJ_INIT_ERR_ALREADY_INITED = -4;

    @BeforeAll
    static void loadLibrary() {
        assumeTrue(WkjStubShim.load(), WkjStubShim.loadFailure());
    }

    @AfterEach
    void restoreTheLibrary() {
        WkjStubShim.setAbiVersion(WebKitNativeShim.abiVersionExpected());
        WebKitNativeShim.installHostTable();
    }

    @Test
    public void aMatchingLibraryInstallsTheHostTable() {
        WkjStubShim.clearHost();
        WebKitNativeShim.installHostTable();

        assertEquals(WKJ_INIT_OK, WkjStubShim.hostInitResult());
        assertTrue(WkjStubShim.hostInstalled());
        assertEquals(WebKitNativeShim.abiVersionExpected(), WkjStubShim.hostAbiVersion());
        assertEquals(WkjStubShim.sizeOf("WKJHost"), WkjStubShim.hostSize());
    }

    @Test
    public void theMismatchMessageNamesTheLibraryBothVersionsAndTheFix() {
        String message = WebKitNativeShim.abiVersionMessage(1, 7);
        String lower = message.toLowerCase(Locale.ROOT);

        assertTrue(message.contains(WebKitNativeShim.libraryName()),
                "the message must name the library: " + message);
        assertTrue(message.contains("1"), "the message must give the expected version: " + message);
        assertTrue(message.contains("7"), "the message must give the actual version: " + message);
        assertTrue(lower.contains("rebuild") || lower.contains("rebuilt"),
                "the message must say what to do: " + message);
        assertTrue(lower.contains("stale"),
                "the message must say the library is stale: " + message);
    }

    @Test
    public void theMissingAbiMessageNamesTheLibraryAndTheSymbol() {
        String message = WebKitNativeShim.abiMissingMessage();
        String lower = message.toLowerCase(Locale.ROOT);

        assertTrue(message.contains(WebKitNativeShim.libraryName()),
                "the message must name the library: " + message);
        assertTrue(message.contains("wkj_abi_version"),
                "the message must name the missing symbol: " + message);
        assertTrue(lower.contains("rebuild") || lower.contains("rebuilt"),
                "the message must say what to do: " + message);
    }

    @Test
    public void aMismatchedLibraryIsRejectedWithThatMessage() {
        int expected = WebKitNativeShim.abiVersionExpected();
        WkjStubShim.setAbiVersion(expected + 1);
        WkjStubShim.clearHost();

        UnsatisfiedLinkError error = assertThrows(UnsatisfiedLinkError.class,
                WebKitNativeShim::installHostTable);

        assertEquals(WebKitNativeShim.abiVersionMessage(expected, expected + 1), error.getMessage());
        assertEquals(WKJ_INIT_ERR_ABI_VERSION, WkjStubShim.hostInitResult());
        assertTrue(!WkjStubShim.hostInstalled(), "a rejected table must not be installed");
    }

    @Test
    public void aHostStructOfTheWrongSizeIsRejected() {
        WkjStubShim.clearHost();
        int size = WebKitNativeShim.hostByteSize();
        int expected = WebKitNativeShim.abiVersionExpected();

        // The size field disagrees with the host_size argument: the caller's struct is not ours.
        assertEquals(WKJ_INIT_ERR_HOST_SIZE, WebKitNativeShim.callWkjInit(size + 8, size, expected));
        assertEquals(WKJ_INIT_ERR_HOST_SIZE, WkjStubShim.hostInitResult());
        assertTrue(!WkjStubShim.hostInstalled());

        // Both agree with each other but not with the library's own sizeof(WKJHost).
        assertEquals(WKJ_INIT_ERR_HOST_SIZE,
                WebKitNativeShim.callWkjInit(size + 8, size + 8, expected));
        assertTrue(!WkjStubShim.hostInstalled());
    }

    @Test
    public void installingTwiceIsReportedAndNotTreatedAsSuccess() {
        WkjStubShim.clearHost();
        int size = WebKitNativeShim.hostByteSize();
        int expected = WebKitNativeShim.abiVersionExpected();

        assertEquals(WKJ_INIT_OK, WebKitNativeShim.callWkjInit(size, size, expected));
        assertEquals(WKJ_INIT_ERR_ALREADY_INITED,
                WebKitNativeShim.callWkjInit(size, size, expected));
        assertEquals(WKJ_INIT_ERR_ALREADY_INITED, WkjStubShim.hostInitResult());
    }
}
