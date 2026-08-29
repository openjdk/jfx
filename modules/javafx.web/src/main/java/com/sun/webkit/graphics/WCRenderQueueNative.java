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
import java.nio.ByteBuffer;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * FFM facade for {@code wkj_rq_release}, the one entry point {@link WCRenderQueue} needs from the
 * {@code jfxwebkit} C ABI.
 * <p>
 * A command buffer is named by the base address of the direct {@link ByteBuffer} that
 * {@code WKJHostGraphics::rq_add_buffer} handed to Java, which is the address
 * {@code GetDirectBufferAddress} produced for the JNI form. {@link MemorySegment#ofBuffer} answers a
 * segment whose address is the buffer's base <em>plus its position</em>, so the position is cleared
 * on a duplicate first; a buffer that has been read from would otherwise be released at the wrong
 * address.
 * <p>
 * A non direct buffer, and a null element, contribute a zero address, which the library ignores -
 * the null element handling of the JNI loop, and what {@code GetDirectBufferAddress} answered for a
 * heap buffer.
 * <p>
 * Must be called on the event thread, for the reason the JNI version documented: destroying a buffer
 * dereferences the render queue references it holds, and JavaScript may be touching the same
 * resources. {@link WCRenderQueue#dispose} still marshals it there.
 *
 * @see com.sun.webkit.WebKitNative
 */
final class WCRenderQueueNative {

    private static final MethodHandle RELEASE = WebKitNative.downcall(
            "wkj_rq_release",
            FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));

    private WCRenderQueueNative() {
    }

    /**
     * Drops the library's reference to each command buffer.
     *
     * @param buffers the direct {@link ByteBuffer} instances the library handed over
     */
    static void release(Object[] buffers) {
        if (buffers.length == 0) {
            return;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment addresses = arena.allocate(JAVA_LONG, buffers.length);
            for (int i = 0; i < buffers.length; i++) {
                addresses.setAtIndex(JAVA_LONG, i, baseAddress(buffers[i]));
            }
            try {
                RELEASE.invokeExact(addresses, buffers.length);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }

    private static long baseAddress(Object buffer) {
        if (!(buffer instanceof ByteBuffer byteBuffer) || !byteBuffer.isDirect()) {
            return 0L;
        }
        return MemorySegment.ofBuffer(byteBuffer.duplicate().clear()).address();
    }
}
