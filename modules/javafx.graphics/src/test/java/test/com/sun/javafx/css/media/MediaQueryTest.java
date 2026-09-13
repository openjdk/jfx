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

package test.com.sun.javafx.css.media;

import com.sun.javafx.css.media.MediaQuery;
import com.sun.javafx.css.media.TriState;
import com.sun.javafx.css.media.expression.ConjunctionExpression;
import com.sun.javafx.css.media.expression.DisjunctionExpression;
import com.sun.javafx.css.media.expression.FunctionExpression;
import com.sun.javafx.css.media.expression.NegationExpression;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class MediaQueryTest {

    private static MediaQuery expr(TriState state) {
        return FunctionExpression.of(UUID.randomUUID().toString(), "value", () -> state, _ -> null, null);
    }

    private static MediaQuery expr(Supplier<TriState> supplier) {
        return FunctionExpression.of(UUID.randomUUID().toString(), "value", supplier, _ -> null, null);
    }

    static Stream<Arguments> conjunctionTruthTable() {
        return Stream.of(
            Arguments.of(TriState.TRUE, TriState.TRUE, TriState.TRUE),
            Arguments.of(TriState.TRUE, TriState.UNKNOWN, TriState.UNKNOWN),
            Arguments.of(TriState.TRUE, TriState.FALSE, TriState.FALSE),
            Arguments.of(TriState.FALSE, TriState.TRUE, TriState.FALSE),
            Arguments.of(TriState.FALSE, TriState.UNKNOWN, TriState.FALSE),
            Arguments.of(TriState.FALSE, TriState.FALSE, TriState.FALSE),
            Arguments.of(TriState.UNKNOWN, TriState.TRUE, TriState.UNKNOWN),
            Arguments.of(TriState.UNKNOWN, TriState.UNKNOWN, TriState.UNKNOWN),
            Arguments.of(TriState.UNKNOWN, TriState.FALSE, TriState.FALSE)
        );
    }

    @ParameterizedTest
    @MethodSource("conjunctionTruthTable")
    void conjunction(TriState left, TriState right, TriState expected) {
        assertEquals(expected, ConjunctionExpression.of(expr(left), expr(right)).evaluate());
    }

    static Stream<Arguments> disjunctionTruthTable() {
        return Stream.of(
            Arguments.of(TriState.TRUE, TriState.TRUE, TriState.TRUE),
            Arguments.of(TriState.TRUE, TriState.UNKNOWN, TriState.TRUE),
            Arguments.of(TriState.TRUE, TriState.FALSE, TriState.TRUE),
            Arguments.of(TriState.FALSE, TriState.TRUE, TriState.TRUE),
            Arguments.of(TriState.FALSE, TriState.UNKNOWN, TriState.UNKNOWN),
            Arguments.of(TriState.FALSE, TriState.FALSE, TriState.FALSE),
            Arguments.of(TriState.UNKNOWN, TriState.TRUE, TriState.TRUE),
            Arguments.of(TriState.UNKNOWN, TriState.UNKNOWN, TriState.UNKNOWN),
            Arguments.of(TriState.UNKNOWN, TriState.FALSE, TriState.UNKNOWN)
        );
    }

    @ParameterizedTest
    @MethodSource("disjunctionTruthTable")
    void disjunction(TriState left, TriState right, TriState expected) {
        assertEquals(expected, DisjunctionExpression.of(expr(left), expr(right)).evaluate());
    }

    static Stream<Arguments> negationTruthTable() {
        return Stream.of(
            Arguments.of(TriState.TRUE, TriState.FALSE),
            Arguments.of(TriState.FALSE, TriState.TRUE),
            Arguments.of(TriState.UNKNOWN, TriState.UNKNOWN)
        );
    }

    @ParameterizedTest
    @MethodSource("negationTruthTable")
    void negation(TriState inner, TriState expected) {
        assertEquals(expected, NegationExpression.of(expr(inner)).evaluate());
    }

    @Test
    void nestedComposition1() {
        // T and (U or F) = T and U = U
        MediaQuery inner = DisjunctionExpression.of(expr(TriState.UNKNOWN), expr(TriState.FALSE));
        MediaQuery expr = ConjunctionExpression.of(expr(TriState.TRUE), inner);
        assertEquals(TriState.UNKNOWN, expr.evaluate());
    }

    @Test
    void nestedComposition2() {
        // F or (U and T) = F or U = U
        MediaQuery inner = ConjunctionExpression.of(expr(TriState.UNKNOWN), expr(TriState.TRUE));
        MediaQuery expr = DisjunctionExpression.of(expr(TriState.FALSE), inner);
        assertEquals(TriState.UNKNOWN, expr.evaluate());
    }

    @Test
    void nestedComposition3() {
        // T or (F and U) = T or F = T
        MediaQuery inner = ConjunctionExpression.of(expr(TriState.FALSE), expr(TriState.UNKNOWN));
        MediaQuery expr = DisjunctionExpression.of(expr(TriState.TRUE), inner);
        assertEquals(TriState.TRUE, expr.evaluate());
    }

    @Test
    void conjunctionOfListPropagates() {
        // ((T and U) and F) => (U and F) => F
        MediaQuery inner = ConjunctionExpression.of(expr(TriState.TRUE), expr(TriState.UNKNOWN));
        MediaQuery expr = ConjunctionExpression.of(inner, expr(TriState.FALSE));
        assertEquals(TriState.FALSE, expr.evaluate());
    }

    @Test
    void disjunctionOfListPropagates() {
        // ((F or U) or T) => (U or T) => T
        MediaQuery inner = DisjunctionExpression.of(expr(TriState.FALSE), expr(TriState.UNKNOWN));
        MediaQuery expr = DisjunctionExpression.of(inner, expr(TriState.TRUE));
        assertEquals(TriState.TRUE, expr.evaluate());
    }

    @Test
    void disjunctionShortCircuitsOnLeftTautology() {
        var calls = new AtomicInteger(0);

        MediaQuery right = expr(() -> {
            calls.incrementAndGet();
            return TriState.FALSE;
        });

        MediaQuery expr = DisjunctionExpression.of(expr(TriState.TRUE), right);

        assertEquals(TriState.TRUE, expr.evaluate());
        assertEquals(0, calls.get(), "Right supplier should not be called when left is TRUE for OR");
    }

    @Test
    void conjunctionShortCircuitsOnLeftContradiction() {
        var calls = new AtomicInteger(0);

        MediaQuery right = expr(() -> {
            calls.incrementAndGet();
            return TriState.TRUE;
        });

        MediaQuery expr = ConjunctionExpression.of(expr(TriState.FALSE), right);

        assertEquals(TriState.FALSE, expr.evaluate());
        assertEquals(0, calls.get(), "Right supplier should not be called when left is FALSE for AND");
    }
}
