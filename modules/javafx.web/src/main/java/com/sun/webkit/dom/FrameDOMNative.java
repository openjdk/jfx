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

import com.sun.webkit.WebKitNative;
import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * FFM facade for {@code wkj_frame_get_document} and {@code wkj_frame_get_owner_element}, the two
 * entry points {@code com.sun.webkit.WebPage} needs from the {@code WKJFrameDOM} half of the
 * {@code jfxwebkit} C ABI.
 * <p>
 * Both used to build the {@code org.w3c.dom} object inside C, with a {@code FindClass} of
 * {@code NodeImpl} and a {@code CallStaticObjectMethod} of {@code NodeImpl.getImpl(long)}. They now
 * return the peer and this class calls {@code NodeImpl.getImpl} itself, which removes the last
 * upcall and the last {@code FindClass} from the slice; contract section 2 forbids returning a Java
 * object from C in any case.
 * <p>
 * This facade lives in {@code com.sun.webkit.dom} rather than beside {@code WebPageNative} for two
 * reasons. {@code NodeImpl.getImpl} is package private here and {@code WebPage} is not in this
 * package - JNI ignored that, an ordinary Java call does not. And the reference invariant below is
 * then stated in exactly one place.
 * <p>
 * <b>The invariant.</b> The peer comes back carrying <em>one</em> reference for Java, the
 * {@code ref()} that {@code makeObjectFromNode} made on the line before the old upcall. That
 * reference belongs to {@code NodeImpl}: the peer is handed to {@code getImpl} exactly once, which
 * either parks it in a {@code SelfDisposer} or, on a cache hit, drops it immediately. Nothing here
 * may deref it, and nothing here may pass it twice; either would move the {@code NodeImpl} hash
 * count that {@code LeakTest} pins. A peer of zero is the Java visible {@code null}, and
 * {@code getImpl(0)} already answers {@code null}, so it needs no special case.
 *
 * @see com.sun.webkit.WebKitNative
 */
public final class FrameDOMNative {

    private static final MethodHandle GET_DOCUMENT = WebKitNative.downcall(
            "wkj_frame_get_document",
            FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));
    private static final MethodHandle GET_OWNER_ELEMENT = WebKitNative.downcall(
            "wkj_frame_get_owner_element",
            FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));

    private FrameDOMNative() {
    }

    /**
     * The document of a frame.
     *
     * @param frame the frame handle
     * @return the document, or {@code null} for a null frame, a frame that is not a local one, or a
     *         frame with no document
     */
    public static Document getDocument(long frame) {
        return (Document) node(GET_DOCUMENT, frame);
    }

    /**
     * The element that owns a frame.
     *
     * @param frame the frame handle
     * @return the owner element, or {@code null} when there is none
     */
    public static Element getOwnerElement(long frame) {
        return (Element) node(GET_OWNER_ELEMENT, frame);
    }

    private static Node node(MethodHandle handle, long frame) {
        long peer;
        try {
            peer = (long) handle.invokeExact(frame);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
        // Exactly once, and never dereferenced here: see the class comment.
        return NodeImpl.getImpl(peer);
    }
}
