/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.event;

// PENDING_DOC_REVIEW
/**
 * Represents a chain of {@code EventDispatcher} objects, which can dispatch
 * an {@code Event}. The event is dispatched by passing it from one
 * {@code EventDispatcher} to the next in the chain until the end of chain is
 * reached. Each {@code EventDispatcher} in the chain can influence the event
 * path and the event itself. The chain is usually formed by following some
 * parent - child hierarchy from the root to the event target and appending
 * all {@code EventDispatcher} objects encountered to the chain.
 * @since JavaFX 2.0
 */
public interface EventDispatchChain {
    /**
     * Appends the specified {@code EventDispatcher} to this chain. Returns a
     * reference to the chain with the appended element.
     * <p>
     * The caller shouldn't assume that this {@code EventDispatchChain} remains
     * unchanged nor that the returned value will reference a different chain
     * after the call. All this depends on the {@code EventDispatchChain}
     * implementation.
     * <p>
     * So the call should be always done in the following form:
     * {@code chain = chain.append(eventDispatcher);}
     *
     * @param eventDispatcher the {@code EventDispatcher} to append to the
     *      chain
     * @return the chain with the appended event dispatcher
     */
    EventDispatchChain append(EventDispatcher eventDispatcher);

    /**
     * Prepends the specified {@code EventDispatcher} to this chain. Returns a
     * reference to the chain with the prepended element.
     * <p>
     * The caller shouldn't assume that this {@code EventDispatchChain} remains
     * unchanged nor that the returned value will reference a different chain
     * after the call. All this depends on the {@code EventDispatchChain}
     * implementation.
     * <p>
     * So the call should be always done in the following form:
     * {@code chain = chain.prepend(eventDispatcher);}
     *
     * @param eventDispatcher the {@code EventDispatcher} to prepend to the
     *      chain
     * @return the chain with the prepended event dispatcher
     */
    EventDispatchChain prepend(EventDispatcher eventDispatcher);

    /**
     * Dispatches the specified event through this {@code EventDispatchChain}.
     * The return value represents the event after processing done by the chain.
     * If further processing is to be done after the call the event referenced
     * by the return value should be used instead of the original event. In the
     * case the event is fully handled / consumed in the chain the returned
     * value is {@code null} and no further processing should be done with that
     * event.
     *
     * @param event the event to dispatch
     * @return the processed event or {@code null} if the event had been fully
     *      handled / consumed
     */
    Event dispatchEvent(Event event);
}
