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

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * FFM facade for the two {@code wkj_page_cache_*} functions of the {@code jfxwebkit} C ABI, used by
 * {@link PageCache}. Both address {@code WebCore::BackForwardCache::singleton()} and are therefore
 * process wide, which is why neither takes a handle.
 * <p>
 * Neither downcall uses {@code Linker.Option.critical(true)}: shrinking the cache destroys cached
 * pages, and a page destructor re-enters Java through the frame loader callbacks.
 *
 * @see com.sun.webkit.WebKitNative
 */
final class PageCacheNative {

    private static final MethodHandle GET_CAPACITY = WebKitNative.downcall(
            "wkj_page_cache_get_capacity",
            FunctionDescriptor.of(JAVA_INT));
    private static final MethodHandle SET_CAPACITY = WebKitNative.downcall(
            "wkj_page_cache_set_capacity",
            FunctionDescriptor.ofVoid(JAVA_INT));

    private PageCacheNative() {
    }

    static int getCapacity() {
        try {
            return (int) GET_CAPACITY.invokeExact();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static void setCapacity(int capacity) {
        try {
            SET_CAPACITY.invokeExact(capacity);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }
}
