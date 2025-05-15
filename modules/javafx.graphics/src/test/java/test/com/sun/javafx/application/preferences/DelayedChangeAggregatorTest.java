/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.application.preferences;

import com.sun.javafx.application.preferences.DelayedChangeAggregator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DelayedChangeAggregatorTest {

    static final int SHORT_DELAY = 100;
    static final int LONG_DELAY = 1000;

    @Test
    void changeSetIsAppliedImmediately() {
        var consumer = new HashMap<String, Object>();
        var aggregator = new TestDelayedChangeAggregator(consumer::putAll);
        aggregator.update(Map.of("testKey", "testValue"), 0);
        assertEquals(Map.of("testKey", "testValue"), consumer);
    }

    @Test
    void subsequentChangeSetsAreAppliedImmediately() {
        var consumer = new HashMap<String, Object>();
        var aggregator = new TestDelayedChangeAggregator(consumer::putAll);
        aggregator.update(Map.of("testKey1", "testValue1"), 0);
        assertEquals(Map.of("testKey1", "testValue1"), consumer);
        aggregator.update(Map.of("testKey2", "testValue2"), 0);
        assertEquals(Map.of("testKey1", "testValue1", "testKey2", "testValue2"), consumer);
    }

    @Test
    void changeSetIsAppliedWithDelay() {
        var consumer = new HashMap<String, Object>();
        var aggregator = new TestDelayedChangeAggregator(consumer::putAll);

        aggregator.update(Map.of("testKey", "testValue"), SHORT_DELAY);
        assertEquals(Map.of(), consumer);

        // Advance the time half-way through the delay period.
        aggregator.setTime(SHORT_DELAY / 2);
        assertEquals(Map.of(), consumer);

        // Advance the time to a millisecond before the end of the delay period.
        aggregator.setTime(SHORT_DELAY - 1);
        assertEquals(Map.of(), consumer);

        // When the delay period has elapsed, the change is applied.
        aggregator.setTime(SHORT_DELAY);
        assertEquals(Map.of("testKey", "testValue"), consumer);
    }

    @Test
    void subsequentChangeSetsAreAppliedWithDelay() {
        var consumer = new HashMap<String, Object>();
        var aggregator = new TestDelayedChangeAggregator(consumer::putAll);

        aggregator.update(Map.of("testKey1", "testValue1"), SHORT_DELAY);
        assertEquals(Map.of(), consumer);

        aggregator.setTime(SHORT_DELAY / 2);
        aggregator.update(Map.of("testKey2", "testValue2"), SHORT_DELAY);
        assertEquals(Map.of(), consumer);

        aggregator.setTime((int)(SHORT_DELAY * 1.5));
        assertEquals(Map.of("testKey1", "testValue1", "testKey2", "testValue2"), consumer);
    }

    @Test
    void changeSetWithShortDelayWaitsForLastChangeSetWithLongDelay() {
        var consumer = new HashMap<String, Object>();
        var aggregator = new TestDelayedChangeAggregator(consumer::putAll);

        aggregator.update(Map.of("testKey1", "testValue1"), LONG_DELAY);
        assertEquals(Map.of(), consumer);

        // Advance the time half-way through the delay period.
        aggregator.setTime(LONG_DELAY / 2);
        assertEquals(Map.of(), consumer);

        // The new changeset waits for the current changeset's delay period to elapse.
        aggregator.update(Map.of("testKey2", "testValue2"), SHORT_DELAY);
        assertEquals(Map.of(), consumer);

        // Advance to the end of the first delay period. Both changesets are applied.
        aggregator.setTime(LONG_DELAY);
        assertEquals(Map.of("testKey1", "testValue1", "testKey2", "testValue2"), consumer);
    }

    private static class TestDelayedChangeAggregator extends DelayedChangeAggregator {
        private long nanos;

        public TestDelayedChangeAggregator(Consumer<Map<String, Object>> changeConsumer) {
            super(changeConsumer);
        }

        void setTime(int millis) {
            nanos = (long)millis * 1000000;
            handle(nanos);
        }

        @Override
        protected long now() {
            return nanos;
        }

        @Override
        public void start() {
            // no-op
        }

        @Override
        public void stop() {
            // no-op
        }
    }
}
