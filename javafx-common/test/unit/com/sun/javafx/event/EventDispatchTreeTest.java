/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.event;

import javafx.event.EventDispatcher;
import org.junit.Assert;
import org.junit.Test;

public final class EventDispatchTreeTest {
    @Test
    public void resetTest() {
        EventDispatchTree eventDispatchTree = new EventDispatchTreeImpl();

        final EventDispatcher dispatcherA = new LabeledEventDispatcher("A");
        final EventDispatcher dispatcherB = new LabeledEventDispatcher("B");
        final EventDispatcher dispatcherC = new LabeledEventDispatcher("C");

        eventDispatchTree = eventDispatchTree.append(dispatcherA);
        ((EventDispatchTreeImpl) eventDispatchTree).reset();

        eventDispatchTree = eventDispatchTree.append(dispatcherB)
                                             .append(dispatcherC);

        Assert.assertEquals("(B->(C))", eventDispatchTree.toString());
    }

    @Test
    public void mergeTreeTest() {
        EventDispatchTree eventDispatchTree =
                new EventDispatchTreeImpl();

        final EventDispatcher[] dispatchers =
                new EventDispatcher[12];
        for (int i = 0; i < dispatchers.length; ++i) {
            dispatchers[i] = new LabeledEventDispatcher(Integer.toString(i));
        }
        
        eventDispatchTree =
                eventDispatchTree.mergeTree(
                    eventDispatchTree.createTree()
                                     .append(dispatchers[0])
                                     .append(dispatchers[1])
                                     .append(dispatchers[2])
                                     .append(dispatchers[3]));
        
        eventDispatchTree =
                eventDispatchTree.mergeTree(
                    eventDispatchTree.createTree()
                                     .append(dispatchers[4])
                                     .append(dispatchers[6])
                                     .prepend(dispatchers[2])
                                     .prepend(dispatchers[1])
                                     .prepend(dispatchers[0]));

        eventDispatchTree =
                eventDispatchTree.mergeTree(
                    eventDispatchTree.createTree()
                                     .prepend(dispatchers[7])
                                     .prepend(dispatchers[5])
                                     .prepend(dispatchers[4])
                                     .prepend(dispatchers[2])
                                     .prepend(dispatchers[1])
                                     .prepend(dispatchers[0]));

        eventDispatchTree =
                eventDispatchTree.mergeTree(
                    eventDispatchTree.createTree()
                                     .prepend(dispatchers[2])
                                     .prepend(dispatchers[1])
                                     .prepend(dispatchers[0])
                                     .append(dispatchers[4])
                                     .append(dispatchers[5])
                                     .append(dispatchers[8]));


        eventDispatchTree = eventDispatchTree.prepend(dispatchers[9]);
        eventDispatchTree = eventDispatchTree.append(dispatchers[10]);


        eventDispatchTree =
                eventDispatchTree.mergeTree(
                    eventDispatchTree.createTree()
                                     .append(dispatchers[0])
                                     .append(dispatchers[1])
                                     .append(dispatchers[2])
                                     .append(dispatchers[4])
                                     .append(dispatchers[6])
                                     .append(dispatchers[10])
                                     .append(dispatchers[11]));
        eventDispatchTree =
                eventDispatchTree.mergeTree(
                    eventDispatchTree.createTree()
                                     .append(dispatchers[9])
                                     .append(dispatchers[0])
                                     .append(dispatchers[1])
                                     .append(dispatchers[2])
                                     .append(dispatchers[4])
                                     .append(dispatchers[6])
                                     .append(dispatchers[10])
                                     .append(dispatchers[11]));

        //             /|
        //            9 0
        //           /  |
        //          0   1
        //         /    |
        //        1     2
        //       /      |
        //      2       4
        //     / \      |
        //    3   4     6
        //   /   / \    |
        // 10   6   5   10
        //     /   / \  |
        //   10   7   8 11
        //    |   |   |
        //   11  10   10

        Assert.assertEquals(
                "(9->(0->(1->(2->(3->(10),4->(6->(10->(11)),5->"
                    + "(7->(10),8->(10))))))),"
                    + "0->(1->(2->(4->(6->(10->(11)))))))",
                eventDispatchTree.toString());
    }

    @Test
    public void dispatchEventTest() {
        final EventCountingDispatcher[] dispatchers =
                new EventCountingDispatcher[8];
        for (int i = 0; i < dispatchers.length; ++i) {
            dispatchers[i] = new EventCountingDispatcher(Integer.toString(i));
        }

        EventDispatchTree eventDispatchTree =
                new EventDispatchTreeImpl();

        eventDispatchTree = eventDispatchTree.append(dispatchers[1]);

        eventDispatchTree =
                eventDispatchTree.mergeTree(
                    eventDispatchTree.createTree().append(dispatchers[2])
                                                  .append(dispatchers[4]));

        eventDispatchTree =
                eventDispatchTree.mergeTree(
                    eventDispatchTree.createTree().append(dispatchers[2])
                                                  .append(dispatchers[5])
                                                  .append(dispatchers[7]));

        eventDispatchTree =
                eventDispatchTree.mergeTree(
                    eventDispatchTree.createTree().append(dispatchers[3])
                                                  .append(dispatchers[6]));

        eventDispatchTree = eventDispatchTree.prepend(dispatchers[0]);

        //   0
        //  /|\
        // 1 2 3
        //  / \ \
        // 4   5 6
        //     |
        //     7

        eventDispatchTree.dispatchEvent(new EmptyEvent());
        verifyCapturingEventCounters(dispatchers, 1, 1, 1, 1, 1, 1, 1, 1);
        verifyBubblingEventCounters(dispatchers, 1, 1, 1, 1, 1, 1, 1, 1);

        dispatchers[0].setConsumeCapturingEvent(true);
        eventDispatchTree.dispatchEvent(new EmptyEvent());
        verifyCapturingEventCounters(dispatchers, 2, 1, 1, 1, 1, 1, 1, 1);
        verifyBubblingEventCounters(dispatchers, 1, 1, 1, 1, 1, 1, 1, 1);

        dispatchers[0].setConsumeCapturingEvent(false);
        dispatchers[2].setConsumeCapturingEvent(true);
        eventDispatchTree.dispatchEvent(new EmptyEvent());
        verifyCapturingEventCounters(dispatchers, 3, 2, 2, 2, 1, 1, 2, 1);
        verifyBubblingEventCounters(dispatchers, 2, 2, 1, 2, 1, 1, 2, 1);

        dispatchers[2].setConsumeCapturingEvent(false);
        dispatchers[7].setConsumeBubblingEvent(true);
        eventDispatchTree.dispatchEvent(new EmptyEvent());
        verifyCapturingEventCounters(dispatchers, 4, 3, 3, 3, 2, 2, 3, 2);
        verifyBubblingEventCounters(dispatchers, 3, 3, 2, 3, 2, 1, 3, 2);

        dispatchers[4].setConsumeBubblingEvent(true);
        dispatchers[7].setConsumeBubblingEvent(true);
        eventDispatchTree.dispatchEvent(new EmptyEvent());
        verifyCapturingEventCounters(dispatchers, 5, 4, 4, 4, 3, 3, 4, 3);
        verifyBubblingEventCounters(dispatchers, 4, 4, 2, 4, 3, 1, 4, 3);
    }

    private static void verifyCapturingEventCounters(
            final EventCountingDispatcher[] dispatchers,
            final int... counters) {
        for (int i = 0; i < dispatchers.length; ++i) {
            Assert.assertEquals(
                    counters[i],
                    dispatchers[i].getCapturingEventCount());
        }
    }

    private static void verifyBubblingEventCounters(
            final EventCountingDispatcher[] dispatchers,
            final int... counters) {
        for (int i = 0; i < dispatchers.length; ++i) {
            Assert.assertEquals(
                    counters[i],
                    dispatchers[i].getBubblingEventCount());
        }
    }
}
