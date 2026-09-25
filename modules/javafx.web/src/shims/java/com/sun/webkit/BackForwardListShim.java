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

import java.lang.foreign.MemoryLayout.PathElement;
import static java.lang.foreign.ValueLayout.ADDRESS;

/**
 * Test access to the {@code WKJBackForwardCallbacks} table {@code BackForwardListNative} installs,
 * so that a binding test can call {@code create_entry} and {@code item_destroyed} through the stubs
 * the library was handed. The facade itself is package private, as it should be.
 * <p>
 * It contains no restricted {@code java.lang.foreign} operation: the table is a segment that
 * {@code WebKitNative} already sized, so reading a pointer out of it needs no reinterpretation.
 */
public final class BackForwardListShim {

    private BackForwardListShim() {
    }

    /**
     * Returns the address installed in one slot of {@code WKJBackForwardCallbacks}, initializing the
     * facade, and so installing the table, if this is the first touch.
     *
     * @param name the member name, exactly as the C struct spells it
     * @return the function pointer, zero when the slot is {@code NULL}
     */
    public static long slotPointer(String name) {
        long offset = WKJLayouts.BACK_FORWARD_CALLBACKS.byteOffset(PathElement.groupElement(name));
        return BackForwardListNative.callbacks().get(ADDRESS, offset).address();
    }
}
