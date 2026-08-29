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

package com.sun.webkit.dom;

import com.sun.webkit.WKJLayouts;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.util.List;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Test access to the {@code WKJLiveConnectHost} table {@code LiveConnectNative} installs. The class
 * itself is package private, as it should be - nothing outside {@code com.sun.webkit.dom} calls it -
 * so this shim is what lets the binding test read the table the library was handed.
 * <p>
 * It contains no restricted {@code java.lang.foreign} operation: the table is a segment that
 * {@code WebKitNative} already sized, so reading a pointer out of it needs no reinterpretation.
 */
public final class LiveConnectShim {

    private LiveConnectShim() {
    }

    /**
     * Returns the {@code wkj_live_connect_init} result, installing the table if this is the first
     * touch.
     *
     * @return {@code WKJ_INIT_OK} on a healthy process
     */
    public static int initResult() {
        return LiveConnectNative.initResult();
    }

    /**
     * Returns the names of the 26 callback slots, in the declaration order of the C struct.
     *
     * @return the slot names
     */
    public static List<String> slotNames() {
        return WKJLayouts.LIVE_CONNECT_HOST.memberLayouts().stream()
                .map(member -> member.name().orElse(null))
                .filter(name -> name != null && !"size".equals(name))
                .toList();
    }

    /**
     * Returns the address installed in one callback slot.
     *
     * @param name the member name, exactly as the C struct spells it
     * @return the function pointer, zero when the slot is {@code NULL}
     */
    public static long slotPointer(String name) {
        return table().get(ADDRESS, offsetOf(name)).address();
    }

    /**
     * Returns the byte offset of one callback slot inside {@code WKJLiveConnectHost}.
     *
     * @param name the member name
     * @return the offset in bytes
     */
    public static long offsetOf(String name) {
        return WKJLayouts.LIVE_CONNECT_HOST.byteOffset(PathElement.groupElement(name));
    }

    /**
     * Returns the {@code size} field the table declares, which is what
     * {@code wkj_live_connect_init} checks against its {@code host_size} argument.
     *
     * @return the size in bytes
     */
    public static int sizeField() {
        return table().get(JAVA_INT, offsetOf("size"));
    }

    /**
     * Returns {@code sizeof(WKJLiveConnectHost)} as the Java layout computes it.
     *
     * @return the size in bytes
     */
    public static long byteSize() {
        return WKJLayouts.LIVE_CONNECT_HOST.byteSize();
    }

    private static MemorySegment table() {
        LiveConnectNative.initResult();
        return LiveConnectNative.hostTable();
    }
}
