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

package com.sun.webkit.graphics;

import com.sun.webkit.WebKitNative;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * FFM facade for {@code wkj_shared_buffer_builder_append}, the one entry point
 * {@link WCGraphicsManager} needs from the {@code jfxwebkit} C ABI.
 * <p>
 * The builder handle is the {@code WebCore::SharedBufferBuilder*} that Java received as the second
 * argument of {@code WKJHostGraphics::load_from_resource}, and the bytes are the chunk
 * {@code fwkLoadFromResource} has just read out of the module's own resource bundle.
 * <p>
 * The C header notes that this is one of the few entry points for which
 * {@code Linker.Option.critical(true)} would be sound, the JNI form having released its critical
 * array read only. It is deliberately not used: contract section 13.1, finding 6 forbids critical on
 * this ABI outright, and the chunk is the 1 KB buffer {@code fwkLoadFromResource} allocates, so the
 * copy through a confined arena is not worth an exception to a rule that exists to stop a critical
 * downcall ever re-entering the JVM.
 *
 * @see com.sun.webkit.WebKitNative
 */
final class WCGraphicsManagerNative {

    private static final MethodHandle BUILDER_APPEND = WebKitNative.downcall(
            "wkj_shared_buffer_builder_append",
            FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT));

    private WCGraphicsManagerNative() {
    }

    /**
     * Appends {@code count} bytes from the front of {@code data} to the builder.
     *
     * @param builder the {@code SharedBufferBuilder} handle
     * @param data the bytes, of which the first {@code count} are appended
     * @param count the number of bytes to append
     */
    static void builderAppend(long builder, byte[] data, int count) {
        if (count <= 0) {
            return;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment bytes = arena.allocate(JAVA_BYTE, count);
            MemorySegment.copy(data, 0, bytes, JAVA_BYTE, 0L, count);
            try {
                BUILDER_APPEND.invokeExact(builder, bytes, count);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }
}
