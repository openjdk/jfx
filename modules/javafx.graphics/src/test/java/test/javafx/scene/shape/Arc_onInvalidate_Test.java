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
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;

import org.junit.jupiter.params.provider.Arguments;
import test.com.sun.javafx.test.OnInvalidateMethodsTestBase;

public class Arc_onInvalidate_Test extends OnInvalidateMethodsTestBase {

    public static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of( new Configuration(Arc.class, "centerX", 222.0, new DirtyBits[] {DirtyBits.NODE_BOUNDS, DirtyBits.NODE_GEOMETRY}) ),
            Arguments.of( new Configuration(Arc.class, "centerY", 111.0, new DirtyBits[] {DirtyBits.NODE_BOUNDS, DirtyBits.NODE_GEOMETRY}) ),
            Arguments.of( new Configuration(Arc.class, "startAngle", 10.0, new DirtyBits[] {DirtyBits.NODE_BOUNDS, DirtyBits.NODE_GEOMETRY}) ),
            Arguments.of( new Configuration(Arc.class, "length", 180.0, new DirtyBits[] {DirtyBits.NODE_BOUNDS, DirtyBits.NODE_GEOMETRY}) ),
            Arguments.of( new Configuration(Arc.class, "radiusX", 123.0, new DirtyBits[] {DirtyBits.NODE_BOUNDS, DirtyBits.NODE_GEOMETRY}) ),
            Arguments.of( new Configuration(Arc.class, "radiusY", 321.0, new DirtyBits[] {DirtyBits.NODE_BOUNDS, DirtyBits.NODE_GEOMETRY}) ),
            Arguments.of( new Configuration(Arc.class, "type", ArcType.ROUND, new DirtyBits[] {DirtyBits.NODE_BOUNDS, DirtyBits.NODE_GEOMETRY}) )
        );
    }
}
