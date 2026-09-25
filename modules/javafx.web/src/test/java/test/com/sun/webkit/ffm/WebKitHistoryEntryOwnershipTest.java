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

import com.sun.webkit.BackForwardList;
import com.sun.webkit.BackForwardListShim;
import com.sun.webkit.WebKitNativeShim;
import com.sun.webkit.WkjStubShim;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Who owns the registry id of a {@code BackForwardList.Entry}, driven through the
 * {@code WKJBackForwardCallbacks} stubs the library was handed and the production {@code WKJHost}.
 * <p>
 * {@code create_entry} returns a new id and {@code BackForwardList.cpp} adopts it into
 * {@code HistoryItem::m_hostObject}, a {@code WKJHandle}. {@code ~HistoryItem} calls
 * {@code item_destroyed} with that id, and then the handle's own destructor releases it. So the
 * handle is the one owner and {@code item_destroyed} must release nothing. The {@code HistoryItem}
 * copy constructor copies the handle, and a copy retains the same id rather than minting another,
 * so a release in {@code item_destroyed} would take the copy's reference. The JNI build that commit
 * 939aa61ead replaced held a separate global reference per item and deleted nothing in
 * {@code notifyHistoryItemDestroyed}.
 */
@Tag("ffm")
public class WebKitHistoryEntryOwnershipTest {

    /** The result code of {@code wkjstub_fire_host_slot} for a slot that was dispatched. */
    private static final int FIRED = 0;

    /** Stand-ins for a {@code WebCore::HistoryItem*} and a page handle; the stub reads neither. */
    private static final long ITEM = 0x7100L;
    private static final long PAGE = 0x7200L;

    private static final String FIRST_URL = "https://example.com/first";
    private static final String SECOND_URL = "https://example.com/second";

    @BeforeAll
    static void loadLibrary() {
        assumeTrue(WkjStubShim.load(), WkjStubShim.loadFailure());
    }

    @AfterAll
    static void leaveTheRecordingTableInstalled() {
        WebKitNativeShim.installHostTable();
    }

    @BeforeEach
    void installTheProductionTable() {
        WebKitNativeShim.installProductionHostTable();
        // A failure recorded by an earlier test on this thread would otherwise be reported as this
        // test's, which is exactly the confusion check_and_clear_exception exists to avoid.
        WebKitNativeShim.checkAndClearUpcallFailure();
    }

    @AfterEach
    void forgetTheProgrammedReturns() {
        WkjStubShim.clearReturns();
    }

    /**
     * Two items share one entry id, as an item and its copy do. The first item's destruction is
     * {@code item_destroyed} followed by its handle's release. The entry must survive that, because
     * the copy still holds a reference, and must go when the copy releases its own.
     */
    @Test
    public void anEntryOutlivesItemDestroyedUntilItsLastHandleIsReleased() {
        int baseline = WebKitNativeShim.registrySize();
        WkjStubShim.setReturnString("wkj_bfl_item_url", FIRST_URL);

        long entry = WkjStubShim.callSlot(BackForwardListShim.slotPointer("create_entry"), "lll",
                ITEM, PAGE);
        assertNotEquals(0L, entry, "create_entry failed; see the severe log entry above");
        BackForwardList.Entry mirror = assertInstanceOf(BackForwardList.Entry.class,
                WebKitNativeShim.lookup(entry));
        assertEquals(FIRST_URL, mirror.getURL().toString());
        assertEquals(1, WebKitNativeShim.referenceCount(entry), "the new id has one owner");

        // HistoryItem's copy constructor copies m_hostObject, and copying a WKJHandle retains.
        assertEquals(FIRED, WkjStubShim.fireHost(slot("core.retain"), entry));
        assertEquals(entry, WkjStubShim.lastFireResult(), "a strong id is retained as itself");
        assertEquals(2, WebKitNativeShim.referenceCount(entry));

        // ~HistoryItem of the original: item_destroyed, then ~WKJHandle.
        WkjStubShim.callSlot(BackForwardListShim.slotPointer("item_destroyed"), "vl", entry);
        assertEquals(2, WebKitNativeShim.referenceCount(entry),
                "item_destroyed released an id that belongs to the WKJHandle holding it");
        assertEquals(FIRED, WkjStubShim.fireHost(slot("core.release"), entry));

        assertEquals(1, WebKitNativeShim.referenceCount(entry),
                "the copy's reference was consumed by the original's destruction");
        assertSame(mirror, WebKitNativeShim.lookup(entry),
                "the copy's id no longer names its entry, so BackForwardList.get would answer null");
        // item_destroyed did reach the entry: it stops asking the library, so it misses a new URL.
        WkjStubShim.setReturnString("wkj_bfl_item_url", SECOND_URL);
        assertEquals(FIRST_URL, mirror.getURL().toString(),
                "item_destroyed did not tell the entry that its item has gone");

        // ~HistoryItem of the copy.
        WkjStubShim.callSlot(BackForwardListShim.slotPointer("item_destroyed"), "vl", entry);
        assertEquals(FIRED, WkjStubShim.fireHost(slot("core.release"), entry));

        assertNull(WebKitNativeShim.lookup(entry), "the last handle's release frees the entry");
        assertEquals(baseline, WebKitNativeShim.registrySize());
        assertEquals(0, WebKitNativeShim.checkAndClearUpcallFailure(),
                "a back/forward callback threw and was contained; see the severe log entry above");
    }

    /**
     * A back/forward callback that throws is logged and contained, and leaves nothing behind for
     * {@code check_and_clear_exception}. None of the C++ callers of this table asks, and the JNI
     * code they replace cleared the exception right after each call. An item without a URL makes
     * the {@code Entry} constructor throw, from {@code URLs.newURL}, and that is the failure used
     * here.
     */
    @Test
    public void aFailedBackForwardCallbackIsLoggedAndLeavesNoFailureBehind() {
        int baseline = WebKitNativeShim.registrySize();
        WkjStubShim.setReturnString("wkj_bfl_item_url", null);
        List<String> severe = new CopyOnWriteArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel() == Level.SEVERE) {
                    severe.add(record.getMessage());
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger logger = Logger.getLogger("com.sun.webkit.WebKitNative");
        logger.addHandler(handler);
        long entry;
        try {
            entry = WkjStubShim.callSlot(BackForwardListShim.slotPointer("create_entry"), "lll",
                    ITEM, PAGE);
        } finally {
            logger.removeHandler(handler);
        }

        assertEquals(0L, entry, "an Entry was built for an item with no URL, so nothing failed");
        assertTrue(severe.contains(
                        "javafx.web upcall back/forward callback create_entry failed and was contained"),
                "the contained failure was not logged as before: " + severe);
        assertEquals(0, WebKitNativeShim.checkAndClearUpcallFailure(),
                "the failed callback left the upcall-failure flag set for an unrelated C++ caller");
        assertEquals(baseline, WebKitNativeShim.registrySize());
    }

    private static int slot(String name) {
        int index = WkjStubShim.findHostSlot(name);
        assertNotEquals(-1, index, "the C header declares no host slot " + name);
        return index;
    }
}
