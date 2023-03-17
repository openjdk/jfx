/*
 * Copyright (c) 2023 Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.animation;

import javafx.animation.*;
import javafx.util.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimedTransitionTest {

    @ParameterizedTest
    @MethodSource(value = "getTimedTransitionsWithDefaultTime")
    public void testDefaultDuration(TimedTransition transition) {
        assertEquals(Duration.millis(400), transition.getDuration());
        assertEquals(Duration.millis(400), transition.getCycleDuration());
    }

    @ParameterizedTest
    @MethodSource(value = "getTimedTransitionsWithDefaultTime")
    public void testCustomDuration(TimedTransition transition) {
        transition.setDuration(Duration.millis(600));
        assertEquals(Duration.millis(600), transition.getDuration());
        assertEquals(Duration.millis(600), transition.getCycleDuration());
    }

    @ParameterizedTest
    @MethodSource(value = "getTimedTransitionsWithCustomTime")
    public void testCustomFromConstructorDuration(TimedTransition transition) {
        assertEquals(Duration.millis(500), transition.getDuration());
        assertEquals(Duration.millis(500), transition.getCycleDuration());

        transition.setDuration(Duration.millis(600));

        assertEquals(Duration.millis(600), transition.getDuration());
        assertEquals(Duration.millis(600), transition.getCycleDuration());
    }

    static Stream<TimedTransition> getTimedTransitionsWithDefaultTime() {
        return Stream.of(
                new FadeTransition(),
                new FillTransition(),
                new PathTransition(),
                new PauseTransition(),
                new RotateTransition(),
                new ScaleTransition(),
                new StrokeTransition(),
                new TranslateTransition()
        );
    }

    static Stream<TimedTransition> getTimedTransitionsWithCustomTime() {
        return Stream.of(
                new FadeTransition(Duration.millis(500)),
                new FillTransition(Duration.millis(500)),
                new PathTransition(Duration.millis(500), null),
                new PauseTransition(Duration.millis(500)),
                new RotateTransition(Duration.millis(500)),
                new ScaleTransition(Duration.millis(500)),
                new StrokeTransition(Duration.millis(500)),
                new TranslateTransition(Duration.millis(500))
        );
    }

}
