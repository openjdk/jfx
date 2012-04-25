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

package javafx.scene.shape;

import java.util.Arrays;
import java.util.Collection;

import javafx.scene.paint.Color;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sun.javafx.test.CssMethodsTestBase;

@RunWith(Parameterized.class)
public class Shape_cssMethods_Test extends CssMethodsTestBase {
    private static final Rectangle TEST_SHAPE = new Rectangle(100, 100);

    @Parameters
    public static Collection data() {
        return Arrays.asList(new Object[] {
            config(TEST_SHAPE, "fill", null, "-fx-fill", Color.RED),
            config(TEST_SHAPE, "fill", null, "-fx-fill", null),
            config(TEST_SHAPE, "smooth", false, "-fx-smooth", true),
            config(TEST_SHAPE, "stroke", null, "-fx-stroke", Color.BLUE),
            config(TEST_SHAPE, "strokeDashOffset", 0.0,
                   "-fx-stroke-dash-offset", 2.0),
            config(TEST_SHAPE, "strokeLineCap", StrokeLineCap.SQUARE,
                   "-fx-stroke-line-cap", StrokeLineCap.ROUND),
            config(TEST_SHAPE, "strokeLineJoin", StrokeLineJoin.BEVEL,
                   "-fx-stroke-line-join", StrokeLineJoin.MITER),
            config(TEST_SHAPE, "strokeType", StrokeType.CENTERED,
                   "-fx-stroke-type", StrokeType.INSIDE),
            config(TEST_SHAPE, "strokeMiterLimit", 0.0,
                   "-fx-stroke-miter-limit", 20.0),
            config(TEST_SHAPE, "strokeWidth", 1.0,
                   "-fx-stroke-width", 2.0),
//          TODO: strokeDashArray is not writable!
//          config(TEST_SHAPE, Shape.STROKE_DASH_ARRAY,
//                 new GenericObservableList<Double>(),
//                 "-fx-stroke-dash-array",
//                 new GenericObservableList<Double>(10.0, 5.0, 2.0)),
            config(TEST_SHAPE, "translateY", 0.0,
                   "-fx-translate-y", 10.0)
        });
    }

    public Shape_cssMethods_Test(final Configuration configuration) {
        super(configuration);
    }

    static {
    }
}
