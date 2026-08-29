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
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * FFM facade for the colour chooser half of the {@code jfxwebkit} C ABI, used by
 * {@link ColorChooser}. It carries one downcall, {@code wkj_color_chooser_set_selected}, and the
 * three-slot {@code WKJColorChooserCallbacks} table that replaces the cached method ids of
 * {@code ColorChooserJava}.
 * <p>
 * The table is installed once for the process rather than per page, because two of its three slots
 * are called on the {@link ColorChooser} the first one returns rather than on a page.
 * <p>
 * Both entry points exist only in a build with {@code ENABLE(INPUT_TYPE_COLOR)}, which
 * {@code Source/cmake/OptionsJava.cmake} turns on for this port. They are still bound optionally, as
 * the header asks, so that a library built without it fails when a colour input is used rather than
 * when {@link ColorChooser} is first loaded.
 *
 * @see com.sun.webkit.WebKitNative
 */
final class ColorChooserNative {

    private static final PlatformLogger log =
            PlatformLogger.getLogger(ColorChooserNative.class.getName());

    private static final MethodHandle SET_SELECTED = WebKitNative.downcallOptional(
            "wkj_color_chooser_set_selected",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle INSTALL_CALLBACKS = WebKitNative.downcallOptional(
            "wkj_install_color_chooser_callbacks",
            FunctionDescriptor.ofVoid(ADDRESS));

    /** The slots of {@code WKJColorChooserCallbacks}, in declaration order. */
    private static final int CALLBACK_SLOTS =
            WKJLayouts.slotCount(WKJLayouts.COLOR_CHOOSER_CALLBACKS);

    static {
        installCallbacks();
    }

    private ColorChooserNative() {
    }

    /**
     * Forces this class to be initialized, and with it the colour chooser callbacks to be installed.
     * Nothing on the Java side calls into this facade until the user activates an
     * {@code <input type=color>}, by which time the library has already needed
     * {@code create_and_show}; class initialization on first use would therefore be too late. The
     * method itself does nothing - being called is the whole point.
     */
    static void install() {
    }

    static void setSelectedColor(long data, int red, int green, int blue) {
        if (SET_SELECTED == null) {
            throw new UnsatisfiedLinkError("jfxwebkit was built without ENABLE(INPUT_TYPE_COLOR),"
                    + " so it does not export wkj_color_chooser_set_selected");
        }
        try {
            SET_SELECTED.invokeExact(data, red, green, blue);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    private static void installCallbacks() {
        if (INSTALL_CALLBACKS == null) {
            log.fine("jfxwebkit exports no colour chooser callbacks; input type=color is disabled");
            return;
        }
        MemorySegment callbacks = WebKitNative.upcallTable(
                stub("createAndShow", FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_INT,
                        JAVA_INT, JAVA_LONG)),
                stub("show", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT)),
                stub("hide", FunctionDescriptor.ofVoid(JAVA_LONG)));
        if (callbacks.byteSize() != (long) CALLBACK_SLOTS * ADDRESS.byteSize()) {
            throw new AssertionError("WKJColorChooserCallbacks has " + CALLBACK_SLOTS + " slots");
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
            target = MethodHandles.lookup().findStatic(ColorChooserNative.class, name,
                    descriptor.toMethodType());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("no upcall target " + name + descriptor.toMethodType(), e);
        }
        return WebKitNative.upcallStub(target, descriptor);
    }

    /**
     * Creates and shows the dialog, returning the registry id of the Java {@link ColorChooser} so
     * that the other two slots can be addressed at it. {@code chooser} is the
     * {@code ColorChooserJava} handle that Java hands straight back to
     * {@code wkj_color_chooser_set_selected}.
     *
     * @param page the registry id of the owning page
     * @param red the red component, 0 to 255
     * @param green the green component, 0 to 255
     * @param blue the blue component, 0 to 255
     * @param chooser the native chooser handle
     * @return the registry id of the new chooser, or 0
     */
    static long createAndShow(long page, int red, int green, int blue, long chooser) {
        try {
            if (!(WebKitNative.lookup(page) instanceof WebPage webPage)) {
                return 0L;
            }
            ColorChooser created =
                    ColorChooser.fwkCreateAndShowColorChooser(webPage, red, green, blue, chooser);
            return WebKitNative.register(created);
        } catch (Throwable t) {
            failed("create_and_show", t);
            return 0L;
        }
    }

    static void show(long ref, int red, int green, int blue) {
        try {
            if (WebKitNative.lookup(ref) instanceof ColorChooser chooser) {
                chooser.fwkShowColorChooser(red, green, blue);
            }
        } catch (Throwable t) {
            failed("show", t);
        }
    }

    /**
     * Hides the dialog. It deliberately does <em>not</em> drop the registry entry
     * {@link #createAndShow} minted: {@code ColorChooserJava::reattachColorChooser} calls
     * {@link #show} on the same id after {@code endChooser} has hidden it, so this slot is not the
     * end of the chooser's life. The id is owned by the {@code WKJHandle} in
     * {@code ColorChooserJava}, whose destructor releases it through {@code WKJHostCore::release},
     * exactly where the JNI form deleted its global reference.
     *
     * @param ref the registry id of the chooser
     */
    static void hide(long ref) {
        try {
            if (WebKitNative.lookup(ref) instanceof ColorChooser chooser) {
                chooser.fwkHideColorChooser();
            }
        } catch (Throwable t) {
            failed("hide", t);
        }
    }

    /*
     * An upcall target may not let a Throwable escape: an exception crossing the boundary terminates
     * the JVM. ColorChooserJava cleared and ignored every pending Java exception, so logging and
     * returning the documented default is what preserves behaviour.
     */
    /* See WebPageNative.failed: one place, so that check_and_clear_exception cannot miss one. */
    private static void failed(String slot, Throwable t) {
        WebKitNative.upcallFailed("colour chooser callback " + slot, t);
    }
}
