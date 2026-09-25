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

package com.sun.webkit;

import com.sun.javafx.logging.PlatformLogger;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * FFM facade for the {@code wkj_bfl_*} functions of the {@code jfxwebkit} C ABI, used by
 * {@link BackForwardList}. An item handle is a {@code WebCore::HistoryItem*}, which the Java
 * {@code Entry} already holds as {@code pitem}; the list itself is addressed by the page handle, as
 * it was under JNI.
 * <p>
 * It also installs {@code WKJBackForwardCallbacks}: {@code list_changed}, which is
 * {@code BackForwardList.notifyChanged()}, and {@code create_entry} and {@code item_destroyed}, which
 * build the {@code BackForwardList.Entry} that mirrors a {@code HistoryItem} and tell it when the item
 * has gone. That table is process wide rather than per page: the back/forward list is created by
 * page creation, before the page exists, so it cannot be reached through
 * {@code wkj_page_set_callbacks}, and {@code list_changed} is called with the id
 * {@code wkj_bfl_set_host} was given rather than with the page.
 * <p>
 * The id of an entry is owned by the library. It is parked in {@code HistoryItem::m_hostObject}, a
 * {@code WKJHandle}, whose destructor releases it; {@link #itemAt} and {@link #itemChildren} borrow
 * it and {@link #itemDestroyed} does not release it. {@code bflItemGetIcon} has no binding: the
 * native-necessity triage marked it for deletion rather than migration, its C body being entirely
 * commented out, and {@code BackForwardList.Entry.getIcon} now answers null in Java, which is what
 * the native call did for every item.
 *
 * @see com.sun.webkit.WebKitNative
 */
final class BackForwardListNative {

    private static final PlatformLogger log =
            PlatformLogger.getLogger(BackForwardListNative.class.getName());

    private static final MethodHandle ITEM_URL = WebKitNative.downcall(
            "wkj_bfl_item_url",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));
    private static final MethodHandle ITEM_TITLE = WebKitNative.downcall(
            "wkj_bfl_item_title",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));
    private static final MethodHandle ITEM_TARGET = WebKitNative.downcall(
            "wkj_bfl_item_target",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));
    private static final MethodHandle ITEM_IS_TARGET = WebKitNative.downcall(
            "wkj_bfl_item_is_target",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
    private static final MethodHandle ITEM_AT = WebKitNative.downcall(
            "wkj_bfl_item_at",
            FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT));
    private static final MethodHandle ITEM_CHILDREN = WebKitNative.downcall(
            "wkj_bfl_item_children",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG, ADDRESS, JAVA_INT));
    private static final MethodHandle SIZE = WebKitNative.downcall(
            "wkj_bfl_size",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
    private static final MethodHandle GET_CAPACITY = WebKitNative.downcall(
            "wkj_bfl_get_capacity",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
    private static final MethodHandle SET_CAPACITY = WebKitNative.downcall(
            "wkj_bfl_set_capacity",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
    private static final MethodHandle CURRENT_INDEX = WebKitNative.downcall(
            "wkj_bfl_current_index",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
    private static final MethodHandle SET_CURRENT_INDEX = WebKitNative.downcall(
            "wkj_bfl_set_current_index",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT));
    private static final MethodHandle INDEX_OF = WebKitNative.downcall(
            "wkj_bfl_index_of",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_INT));
    private static final MethodHandle SET_ENABLED = WebKitNative.downcall(
            "wkj_bfl_set_enabled",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
    private static final MethodHandle IS_ENABLED = WebKitNative.downcall(
            "wkj_bfl_is_enabled",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
    private static final MethodHandle CLEAR_FOR_DRT = WebKitNative.downcall(
            "wkj_bfl_clear_for_drt",
            FunctionDescriptor.ofVoid(JAVA_LONG));
    private static final MethodHandle SET_HOST = WebKitNative.downcall(
            "wkj_bfl_set_host",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG));
    private static final MethodHandle SET_CALLBACKS = WebKitNative.downcall(
            "wkj_bfl_set_callbacks",
            FunctionDescriptor.ofVoid(ADDRESS));

    /** The slots of {@code WKJBackForwardCallbacks}, in declaration order. */
    private static final int CALLBACK_SLOTS =
            WKJLayouts.slotCount(WKJLayouts.BACK_FORWARD_CALLBACKS);

    /** The {@code WKJBackForwardCallbacks} table the library keeps for as long as it is loaded. */
    private static final MemorySegment CALLBACKS = installCallbacks();

    private BackForwardListNative() {
    }

    static String itemGetURL(long item) {
        return stringCall(ITEM_URL, item);
    }

    static String itemGetTitle(long item) {
        return stringCall(ITEM_TITLE, item);
    }

    /**
     * The item's frame target. An empty target reaches Java as {@code null}, which is the null
     * {@code jstring} the JNI form returned for it.
     *
     * @param item the history item handle
     * @return the target, or {@code null}
     */
    static String itemGetTarget(long item) {
        return stringCall(ITEM_TARGET, item);
    }

    static boolean itemIsTargetItem(long item) {
        return intCall(ITEM_IS_TARGET, item) != 0;
    }

    /**
     * The entry at {@code index}, or {@code null} when there is none. The id is borrowed: the
     * library keeps the entry alive in {@code HistoryItem::m_hostObject} and releases it when the
     * item is destroyed, so it is looked up and not released here.
     *
     * @param page the page handle
     * @param index the index into the list
     * @return the entry, or {@code null}
     */
    static BackForwardList.Entry itemAt(long page, int index) {
        long ref;
        try {
            ref = (long) ITEM_AT.invokeExact(page, index);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
        return WebKitNative.lookup(ref) instanceof BackForwardList.Entry entry ? entry : null;
    }

    /**
     * The child entries of one item. The count is asked for first, as {@code wkj_bfl_item_children}
     * documents, so that the buffer is never guessed at; the ids are borrowed, exactly as
     * {@link #itemAt}'s is.
     *
     * @param item the {@code HistoryItem} handle
     * @param page the page handle
     * @return the children, never {@code null}
     */
    static BackForwardList.Entry[] itemChildren(long item, long page) {
        int count;
        try {
            count = (int) ITEM_CHILDREN.invokeExact(item, page, MemorySegment.NULL, 0);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
        if (count <= 0) {
            return new BackForwardList.Entry[0];
        }
        long[] refs = new long[count];
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(JAVA_LONG, count);
            int written;
            try {
                written = (int) ITEM_CHILDREN.invokeExact(item, page, out, count);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
            int n = Math.min(written, count);
            MemorySegment.copy(out, JAVA_LONG, 0L, refs, 0, n);
            refs = n == count ? refs : Arrays.copyOf(refs, n);
        }
        BackForwardList.Entry[] children = new BackForwardList.Entry[refs.length];
        for (int i = 0; i < refs.length; i++) {
            children[i] = WebKitNative.lookup(refs[i]) instanceof BackForwardList.Entry entry
                    ? entry : null;
        }
        return children;
    }

    static void clearBackForwardListForDRT(long page) {
        try {
            CLEAR_FOR_DRT.invokeExact(page);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static int size(long page) {
        return intCall(SIZE, page);
    }

    static int getMaximumSize(long page) {
        return intCall(GET_CAPACITY, page);
    }

    static void setMaximumSize(long page, int size) {
        try {
            SET_CAPACITY.invokeExact(page, size);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static int getCurrentIndex(long page) {
        return intCall(CURRENT_INDEX, page);
    }

    static int indexOf(long page, long item, boolean reverse) {
        try {
            return (int) INDEX_OF.invokeExact(page, item, reverse ? 1 : 0);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static void setEnabled(long page, boolean flag) {
        try {
            SET_ENABLED.invokeExact(page, flag ? 1 : 0);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static boolean isEnabled(long page) {
        return intCall(IS_ENABLED, page) != 0;
    }

    static int setCurrentIndex(long page, int index) {
        try {
            return (int) SET_CURRENT_INDEX.invokeExact(page, index);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * Attaches or detaches the Java list that {@code list_changed} is fired at. The registered id is
     * call-scoped: {@code wkj_bfl_set_host} retains the new host before replacing its native handle,
     * so this method always drops the reference it registered after the downcall returns.
     *
     * @param page the page handle
     * @param host the list, or {@code null} to detach
     */
    static void setHostObject(long page, BackForwardList host) {
        long ref = WebKitNative.register(host);
        try {
            SET_HOST.invokeExact(page, ref);
        } catch (Throwable t) {
            throw new AssertionError(t);
        } finally {
            WebKitNative.unregister(ref);
        }
    }

    /**
     * The {@code WKJBackForwardCallbacks} table that was installed when this class was initialized,
     * so that a binding test can call its slots the way the library does.
     *
     * @return the table, in the process-wide upcall arena
     */
    static MemorySegment callbacks() {
        return CALLBACKS;
    }

    private static MemorySegment installCallbacks() {
        MemorySegment callbacks = WebKitNative.upcallTable(WKJLayouts.BACK_FORWARD_CALLBACKS,
                stub("listChanged", FunctionDescriptor.ofVoid(JAVA_LONG)),
                stub("createEntry", FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_LONG)),
                stub("itemDestroyed", FunctionDescriptor.ofVoid(JAVA_LONG)));
        if (callbacks.byteSize() != (long) CALLBACK_SLOTS * ADDRESS.byteSize()) {
            throw new AssertionError("WKJBackForwardCallbacks has " + CALLBACK_SLOTS + " slots");
        }
        try {
            SET_CALLBACKS.invokeExact(callbacks);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
        return callbacks;
    }

    private static MemorySegment stub(String name, FunctionDescriptor descriptor) {
        return WebKitNative.upcallStub(MethodHandles.lookup(), name, descriptor);
    }

    /**
     * {@code BackForwardList.notifyChanged()}. The id is the one {@code wkj_bfl_set_host} was given,
     * not the page's.
     *
     * @param ref the registry id of the list
     */
    static void listChanged(long ref) {
        try {
            if (WebKitNative.lookup(ref) instanceof BackForwardList list) {
                list.notifyChanged();
            }
        } catch (Throwable t) {
            // An upcall target may not let a Throwable escape: an exception crossing the boundary
            // terminates the JVM, and the JNI form cleared and ignored a pending exception here.
            failed("list_changed", t);
        }
    }

    /**
     * Builds the {@code BackForwardList.Entry} that mirrors one {@code HistoryItem} and returns the
     * id the library parks in {@code HistoryItem::m_hostObject} for the life of the item. The
     * library owns that reference: the {@code WKJHandle} there adopts it and releases it when the
     * item is destroyed, after {@link #itemDestroyed} has run. Nothing in Java releases it.
     *
     * @param item the {@code HistoryItem} handle
     * @param page the page handle
     * @return the registry id of the new entry, or zero
     */
    static long createEntry(long item, long page) {
        try {
            return WebKitNative.register(BackForwardList.createEntry(item, page));
        } catch (Throwable t) {
            failed("create_entry", t);
            return 0L;
        }
    }

    /**
     * {@code BackForwardList.Entry.notifyItemDestroyed()}, called from the {@code HistoryItem}
     * destructor with the id {@link #createEntry} returned. The id is borrowed and is not released
     * here. {@code HistoryItem::m_hostObject} is the one owner, and its {@code WKJHandle} releases
     * the id once the destructor body has returned. A copied {@code HistoryItem} copies that handle,
     * and because the id {@link #createEntry} returns is strong, the copy retains the same id with
     * its count raised, so a release here would take the reference the copy still holds. This
     * matches {@code PopupMenuNative.destroy} and {@code ColorChooserNative.hide}, and the JNI build
     * that commit 939aa61ead replaced, whose {@code notifyHistoryItemDestroyed} deleted no
     * reference.
     *
     * @param ref the registry id of the entry
     */
    static void itemDestroyed(long ref) {
        try {
            if (WebKitNative.lookup(ref) instanceof BackForwardList.Entry entry) {
                BackForwardList.notifyItemDestroyed(entry);
            }
        } catch (Throwable t) {
            failed("item_destroyed", t);
        }
    }

    /*
     * See WebPageNative.failed. BackForwardList.cpp and the HistoryItem destructor, the only callers
     * of this table, never ask check_and_clear_exception, and the JNI form cleared the exception
     * after every call.
     */
    private static void failed(String slot, Throwable t) {
        WebKitNative.clientCallbackFailed("back/forward callback", slot, t);
    }

    private static int intCall(MethodHandle handle, long peer) {
        try {
            return (int) handle.invokeExact(peer);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    private static String stringCall(MethodHandle handle, long peer) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment resultLength = arena.allocate(JAVA_INT);
            MemorySegment resultBuffer = arena.allocate(JAVA_CHAR, WKJStringCodec.CAPACITY);
            int status;
            try {
                status = (int) handle.invokeExact(peer, resultBuffer, WKJStringCodec.CAPACITY,
                        resultLength);
                if (status == WKJStringCodec.OVERFLOW) {
                    int required = resultLength.get(JAVA_INT, 0);
                    resultBuffer = arena.allocate(JAVA_CHAR, required);
                    status = (int) handle.invokeExact(peer, resultBuffer, required, resultLength);
                }
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
            return WKJStringCodec.decode(status, resultBuffer, resultLength);
        }
    }
}
