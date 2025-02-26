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

package test.javafx.scene.shape;

import java.util.stream.Stream;

import com.sun.javafx.scene.DirtyBits;
import javafx.scene.shape.Line;

import org.junit.jupiter.params.provider.Arguments;
import test.com.sun.javafx.test.OnInvalidateMethodsTestBase;

public class Line_onInvalidate_Test extends OnInvalidateMethodsTestBase {

    public static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of( new Configuration(Line.class, "startX", 10.0, new DirtyBits[] {DirtyBits.NODE_BOUNDS, DirtyBits.NODE_GEOMETRY}) ),
            Arguments.of( new Configuration(Line.class, "startY", 11.0, new DirtyBits[]{DirtyBits.NODE_BOUNDS, DirtyBits.NODE_GEOMETRY}) ),
            Arguments.of( new Configuration(Line.class, "endX", 100.0, new DirtyBits[]{DirtyBits.NODE_BOUNDS, DirtyBits.NODE_GEOMETRY}) ),
            Arguments.of( new Configuration(Line.class, "endY", 10.0, new DirtyBits[]{DirtyBits.NODE_BOUNDS, DirtyBits.NODE_GEOMETRY}) )
        );
    }
}
