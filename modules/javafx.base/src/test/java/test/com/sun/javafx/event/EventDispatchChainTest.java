/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.event.EventDispatchChainImpl;
import com.sun.javafx.event.EventDispatchTreeImpl;
import javafx.event.EventDispatchChain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public final class EventDispatchChainTest {
    private static final Operation[] IDENTITY_FUNCTION_OPS = {
            Operation.mul(3), Operation.add(5), Operation.mul(4), Operation.sub(2),
            Operation.div(6), Operation.add(9), Operation.div(2), Operation.sub(6)
    };

    private static Stream<Arguments> chainClasses() {
        return Stream.of(
                Arguments.of(EventDispatchChainImpl.class),
                Arguments.of(EventDispatchTreeImpl.class)
        );
    }

    @ParameterizedTest
    @MethodSource("chainClasses")
    void chainConstructionBeforeDispatchTest(Class<? extends EventDispatchChain> chainClass) throws Exception {
        EventDispatchChain chain = chainClass.getDeclaredConstructor().newInstance();
        chain = initializeTestChain(chain);
        verifyChain(chain, 0, 702);
    }

    @ParameterizedTest
    @MethodSource("chainClasses")
    void chainModificationAfterDispatchTest(Class<? extends EventDispatchChain> chainClass) throws Exception {
        EventDispatchChain chain = chainClass.getDeclaredConstructor().newInstance();
        chain = initializeTestChain(chain);
        chain.dispatchEvent(new ValueEvent());

        chain = chain.append(new EventChangingDispatcher(Operation.sub(6), Operation.div(3)));
        verifyChain(chain, 0, 270);

        chain = prependIdentityChain(chain);
        verifyChain(chain, 0, 270);
    }

    @ParameterizedTest
    @MethodSource("chainClasses")
    void chainModificationDuringDispatchTest(Class<? extends EventDispatchChain> chainClass) throws Exception {
        EventDispatchChain chain = chainClass.getDeclaredConstructor().newInstance();

        // x + 55, y + 55
        chain = prependSeriesChain(chain, 10);
        chain = chain.prepend(
                new PathChangingDispatcher(
                        new EventChangingDispatcher(Operation.mul(3), Operation.div(5)),
                        new EventChangingDispatcher(Operation.div(7), Operation.mul(9)),
                        1));
        // x + 15, y + 15
        chain = prependSeriesChain(chain, 5);

        chain = chain.prepend(new PathChangingDispatcher(null, null, 2));

        // x + 6, y + 6
        chain = prependSeriesChain(chain, 3);

        for (int x = 0; x < 5; ++x) {
            verifyChain(chain, 1225 * x - 86, 729 * x + 50);
        }
    }

    @ParameterizedTest
    @MethodSource("chainClasses")
    void buildLongChainTest(Class<? extends EventDispatchChain> chainClass) throws Exception {
        EventDispatchChain chain = chainClass.getDeclaredConstructor().newInstance();

        chain = prependSeriesChain(chain, 100);
        verifyChain(chain, 0, 10100);

        chain = prependIdentityChain(chain);
        verifyChain(chain, 1, 10101);

        chain = prependSeriesChain(chain, 100);
        verifyChain(chain, 2, 20202);
    }

    private static EventDispatchChain prependIdentityChain(EventDispatchChain tailChain) {
        for (int i = 0; i < IDENTITY_FUNCTION_OPS.length; ++i) {
            tailChain = tailChain.prepend(
                    new EventChangingDispatcher(
                            IDENTITY_FUNCTION_OPS[IDENTITY_FUNCTION_OPS.length - i - 1],
                            IDENTITY_FUNCTION_OPS[i]));
        }

        return tailChain;
    }

    private static EventDispatchChain prependSeriesChain(EventDispatchChain tailChain, final int count) {
        for (int i = 1; i <= count; ++i) {
            tailChain = tailChain.prepend(
                    new EventChangingDispatcher(Operation.add(i), Operation.add(i)));
        }

        return tailChain;
    }

    private static EventDispatchChain initializeTestChain(final EventDispatchChain emptyChain) {
        return emptyChain.append(new EventChangingDispatcher(Operation.add(3), Operation.div(2)))
                .append(new EventChangingDispatcher(Operation.mul(7), Operation.sub(6)))
                .prepend(new EventChangingDispatcher(Operation.sub(4), Operation.mul(6)))
                .append(new EventChangingDispatcher(Operation.div(3), Operation.add(11)))
                .prepend(new EventChangingDispatcher(Operation.add(10), Operation.mul(9)));
    }

    private static void verifyChain(final EventDispatchChain testChain,
                                    final int initialValue,
                                    final int resultValue) {
        final ValueEvent valueEvent = (ValueEvent) testChain.dispatchEvent(new ValueEvent(initialValue));

        assertNotNull(valueEvent);
        assertEquals(resultValue, valueEvent.getValue());
    }
}
