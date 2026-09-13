/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.event;

import javafx.event.EventDispatchChain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import test.com.sun.javafx.event.StubBasicEventDispatcher.ConsumeEvent;

public final class CompositeEventDispatcherTest {
    @Test
    public void eventDispatchTest() {
        final TestCompositeEventDispatcher compositeDispatcher =
                new TestCompositeEventDispatcher();

        final EventCountingDispatcher terminalDispatcher =
                new EventCountingDispatcher();
        final EventDispatchChain eventDispatchChain =
                StubEventDispatchChain.EMPTY_CHAIN
                        .append(compositeDispatcher)
                        .append(terminalDispatcher);

        assertNotNull(eventDispatchChain.dispatchEvent(
                new EmptyEvent()));
        verifyEventCounters(compositeDispatcher, 1, 1, 1, 1, 1, 1);
        assertEquals(1, terminalDispatcher.getCapturingEventCount());

        compositeDispatcher.getFirstChildDispatcher().setConsumeNextEvent(
                ConsumeEvent.CAPTURING);
        assertNull(eventDispatchChain.dispatchEvent(new EmptyEvent()));
        verifyEventCounters(compositeDispatcher, 2, 1, 1, 1, 1, 1);
        assertEquals(1, terminalDispatcher.getCapturingEventCount());

        compositeDispatcher.getSecondChildDispatcher().setConsumeNextEvent(
                ConsumeEvent.CAPTURING);
        assertNull(eventDispatchChain.dispatchEvent(new EmptyEvent()));
        verifyEventCounters(compositeDispatcher, 3, 2, 1, 1, 1, 1);
        assertEquals(1, terminalDispatcher.getCapturingEventCount());

        compositeDispatcher.getThirdChildDispatcher().setConsumeNextEvent(
                ConsumeEvent.CAPTURING);
        assertNull(eventDispatchChain.dispatchEvent(new EmptyEvent()));
        verifyEventCounters(compositeDispatcher, 4, 3, 2, 1, 1, 1);
        assertEquals(1, terminalDispatcher.getCapturingEventCount());

        compositeDispatcher.getThirdChildDispatcher().setConsumeNextEvent(
                ConsumeEvent.BUBBLING);
        assertNull(eventDispatchChain.dispatchEvent(new EmptyEvent()));
        verifyEventCounters(compositeDispatcher, 5, 4, 3, 1, 1, 2);
        assertEquals(2, terminalDispatcher.getCapturingEventCount());

        compositeDispatcher.getSecondChildDispatcher().setConsumeNextEvent(
                ConsumeEvent.BUBBLING);
        assertNull(eventDispatchChain.dispatchEvent(new EmptyEvent()));
        verifyEventCounters(compositeDispatcher, 6, 5, 4, 1, 2, 3);
        assertEquals(3, terminalDispatcher.getCapturingEventCount());

        compositeDispatcher.getFirstChildDispatcher().setConsumeNextEvent(
                ConsumeEvent.BUBBLING);
        assertNull(eventDispatchChain.dispatchEvent(new EmptyEvent()));
        verifyEventCounters(compositeDispatcher, 7, 6, 5, 2, 3, 4);
        assertEquals(4, terminalDispatcher.getCapturingEventCount());
    }

    private void verifyEventCounters(
            final TestCompositeEventDispatcher compositeDispatcher,
            final int expectedChild1CapturingEventCount,
            final int expectedChild2CapturingEventCount,
            final int expectedChild3CapturingEventCount,
            final int expectedChild1BubblingEventCount,
            final int expectedChild2BubblingEventCount,
            final int expectedChild3BubblingEventCount) {
        assertEquals(expectedChild1CapturingEventCount,
                compositeDispatcher.getFirstChildDispatcher()
                        .getCapturingEventCount());
        assertEquals(expectedChild2CapturingEventCount,
                compositeDispatcher.getSecondChildDispatcher()
                        .getCapturingEventCount());
        assertEquals(expectedChild3CapturingEventCount,
                compositeDispatcher.getThirdChildDispatcher()
                        .getCapturingEventCount());
        assertEquals(expectedChild1BubblingEventCount,
                compositeDispatcher.getFirstChildDispatcher()
                        .getBubblingEventCount());
        assertEquals(expectedChild2BubblingEventCount,
                compositeDispatcher.getSecondChildDispatcher()
                        .getBubblingEventCount());
        assertEquals(expectedChild3BubblingEventCount,
                compositeDispatcher.getThirdChildDispatcher()
                        .getBubblingEventCount());
    }
}
