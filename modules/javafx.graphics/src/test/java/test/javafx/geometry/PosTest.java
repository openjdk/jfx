/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.geometry;

import static javafx.geometry.HPos.LEFT;
import static javafx.geometry.HPos.RIGHT;
import static javafx.geometry.Pos.BASELINE_CENTER;
import static javafx.geometry.Pos.BASELINE_LEFT;
import static javafx.geometry.Pos.BASELINE_RIGHT;
import static javafx.geometry.Pos.BOTTOM_CENTER;
import static javafx.geometry.Pos.BOTTOM_LEFT;
import static javafx.geometry.Pos.BOTTOM_RIGHT;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.geometry.Pos.TOP_CENTER;
import static javafx.geometry.Pos.TOP_LEFT;
import static javafx.geometry.Pos.TOP_RIGHT;
import static javafx.geometry.VPos.BASELINE;
import static javafx.geometry.VPos.BOTTOM;
import static javafx.geometry.VPos.TOP;

import java.util.stream.Stream;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class PosTest {

    public static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of(TOP_LEFT, TOP, LEFT),
            Arguments.of(TOP_CENTER, TOP, HPos.CENTER),
            Arguments.of(TOP_RIGHT, TOP, RIGHT),
            Arguments.of(CENTER_LEFT, VPos.CENTER, LEFT),
            Arguments.of(Pos.CENTER, VPos.CENTER, HPos.CENTER),
            Arguments.of(CENTER_RIGHT, VPos.CENTER, RIGHT),
            Arguments.of(BOTTOM_LEFT, BOTTOM, LEFT),
            Arguments.of(BOTTOM_CENTER, BOTTOM, HPos.CENTER),
            Arguments.of(BOTTOM_RIGHT, BOTTOM, RIGHT),
            Arguments.of(BASELINE_LEFT, BASELINE, LEFT),
            Arguments.of(BASELINE_CENTER, BASELINE, HPos.CENTER),
            Arguments.of(BASELINE_RIGHT, BASELINE, RIGHT)
         );
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void shouldHaveVPosAndHPos(Pos pos, VPos vpos, HPos hpos) {
        assertEquals(pos.getVpos(), vpos);
        assertEquals(pos.getHpos(), hpos);
    }
}
