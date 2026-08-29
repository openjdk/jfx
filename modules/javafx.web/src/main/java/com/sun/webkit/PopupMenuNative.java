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

import com.sun.webkit.graphics.WCFont;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * FFM facade for the popup menu half of the {@code jfxwebkit} C ABI, used by {@link PopupMenu}. It
 * carries the two downcalls {@code wkj_popup_selection_committed} and {@code wkj_popup_closed}, and
 * the six slot {@code WKJPopupCallbacks} table that replaces the cached method ids of
 * {@code PopupMenuJava}.
 * <p>
 * The table is installed once for the process rather than per page: {@code create} is a static Java
 * method and the other five are made on the {@link PopupMenu} it returns, so none of them is
 * addressed by page.
 * <p>
 * The id {@code create} returns is owned by the {@code WKJHandle} in {@code PopupMenuJava} and is
 * released through {@code WKJHostCore::release} when that object goes away, which is where the JNI
 * form deleted its global reference. {@link #destroy} therefore only severs the Java half of the
 * link and leaves the id alone.
 *
 * @see com.sun.webkit.WebKitNative
 */
final class PopupMenuNative {

    private static final MethodHandle SELECTION_COMMITTED = WebKitNative.downcall(
            "wkj_popup_selection_committed",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
    private static final MethodHandle CLOSED = WebKitNative.downcall(
            "wkj_popup_closed",
            FunctionDescriptor.ofVoid(JAVA_LONG));
    private static final MethodHandle INSTALL_CALLBACKS = WebKitNative.downcall(
            "wkj_install_popup_callbacks",
            FunctionDescriptor.ofVoid(ADDRESS));

    /** The slots of {@code WKJPopupCallbacks}, in declaration order. */
    private static final int CALLBACK_SLOTS =
            WKJLayouts.slotCount(WKJLayouts.POPUP_CALLBACKS);

    static {
        installCallbacks();
    }

    private PopupMenuNative() {
    }

    /**
     * Forces this class to be initialized, and with it the popup menu callbacks to be installed.
     * Nothing on the Java side calls into this facade until a select element is opened, by which
     * time the library has already needed {@code create}; class initialization on first use would
     * therefore be too late. The method itself does nothing - being called is the whole point.
     */
    static void install() {
    }

    static void selectionCommitted(long popup, int index) {
        try {
            SELECTION_COMMITTED.invokeExact(popup, index);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static void closed(long popup) {
        try {
            CLOSED.invokeExact(popup);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    private static void installCallbacks() {
        MemorySegment callbacks = WebKitNative.upcallTable(
                stub("create", FunctionDescriptor.of(JAVA_LONG, JAVA_LONG)),
                stub("appendItem", FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT,
                        JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_LONG)),
                stub("setSelectedItem", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT)),
                stub("show", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_INT,
                        JAVA_INT)),
                stub("hide", FunctionDescriptor.ofVoid(JAVA_LONG)),
                stub("destroy", FunctionDescriptor.ofVoid(JAVA_LONG)));
        if (callbacks.byteSize() != (long) CALLBACK_SLOTS * ADDRESS.byteSize()) {
            throw new AssertionError("WKJPopupCallbacks has " + CALLBACK_SLOTS + " slots");
        }
        try {
            INSTALL_CALLBACKS.invokeExact(callbacks);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    private static MemorySegment stub(String name, FunctionDescriptor descriptor) {
        MethodHandle target;
        try {
            target = MethodHandles.lookup().findStatic(PopupMenuNative.class, name,
                    descriptor.toMethodType());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("no upcall target " + name + descriptor.toMethodType(), e);
        }
        return WebKitNative.upcallStub(target, descriptor);
    }

    /**
     * Creates the Java menu and returns the registry id the library addresses the other five slots
     * at. {@code popup} is the {@code PopupMenuJava} handle that Java hands straight back to
     * {@link #selectionCommitted} and {@link #closed}.
     *
     * @param popup the native popup handle
     * @return the registry id of the new menu, or 0
     */
    static long create(long popup) {
        try {
            return WebKitNative.register(PopupMenu.fwkCreatePopupMenu(popup));
        } catch (Throwable t) {
            failed("create", t);
            return 0L;
        }
    }

    static void appendItem(long ref, MemorySegment text, int textLength, int isLabel,
                           int isSeparator, int isEnabled, int backgroundArgb, int foregroundArgb,
                           long font) {
        try {
            if (WebKitNative.lookup(ref) instanceof PopupMenu menu) {
                WCFont wcFont = WebKitNative.lookup(font) instanceof WCFont f ? f : null;
                menu.fwkAppendItem(readString(text, textLength),
                        isLabel != 0, isSeparator != 0, isEnabled != 0,
                        backgroundArgb, foregroundArgb, wcFont);
            }
        } catch (Throwable t) {
            failed("append_item", t);
        }
    }

    static void setSelectedItem(long ref, int index) {
        try {
            if (WebKitNative.lookup(ref) instanceof PopupMenu menu) {
                menu.fwkSetSelectedItem(index);
            }
        } catch (Throwable t) {
            failed("set_selected_item", t);
        }
    }

    static void show(long ref, long page, int x, int y, int width) {
        try {
            if (WebKitNative.lookup(ref) instanceof PopupMenu menu
                    && WebKitNative.lookup(page) instanceof WebPage webPage) {
                menu.fwkShow(webPage, x, y, width);
            }
        } catch (Throwable t) {
            failed("show", t);
        }
    }

    static void hide(long ref) {
        try {
            if (WebKitNative.lookup(ref) instanceof PopupMenu menu) {
                menu.fwkHide();
            }
        } catch (Throwable t) {
            failed("hide", t);
        }
    }

    /**
     * Tells the menu that its native half is gone, which is what clears {@code PopupMenu.pdata}.
     * The registry id is deliberately not released here: it belongs to the {@code WKJHandle} in
     * {@code PopupMenuJava}, whose destructor releases it, exactly where the JNI form deleted its
     * global reference.
     *
     * @param ref the registry id of the menu
     */
    static void destroy(long ref) {
        try {
            if (WebKitNative.lookup(ref) instanceof PopupMenu menu) {
                menu.fwkDestroy();
            }
        } catch (Throwable t) {
            failed("destroy", t);
        }
    }

    /* See WebPageNative.failed: one place, so that check_and_clear_exception cannot miss one. */
    private static void failed(String slot, Throwable t) {
        WebKitNative.upcallFailed("popup menu callback " + slot, t);
    }

    /*
     * A pointer arriving through an upcall is a zero length segment carrying only its address, so
     * it has to be given the size the C prototype promises before it can be read. A NULL pointer is
     * the Java null, which is what an item with no text was.
     */
    @SuppressWarnings("restricted")
    private static String readString(MemorySegment s, int length) {
        if (s.address() == 0L) {
            return null;
        }
        if (length <= 0) {
            return "";
        }
        char[] chars = new char[length];
        MemorySegment.copy(s.reinterpret((long) length * Character.BYTES), JAVA_CHAR, 0L, chars, 0,
                length);
        return new String(chars);
    }
}
