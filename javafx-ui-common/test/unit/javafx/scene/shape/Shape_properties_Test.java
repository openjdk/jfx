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

import static com.sun.javafx.test.TestHelper.box;

import java.util.ArrayList;
import java.util.Collection;

import javafx.scene.paint.Color;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sun.javafx.test.BBoxComparator;
import com.sun.javafx.test.PropertiesTestBase;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

@RunWith(Parameterized.class)
public final class Shape_properties_Test extends PropertiesTestBase {

    @Parameters
    public static Collection data() {
        ArrayList array = new ArrayList();        

        // simple property tests
        Shape testShape = createTestRectangle();

        array.add(config(testShape, "strokeType",
                  StrokeType.CENTERED, StrokeType.INSIDE));
        array.add(config(testShape, "strokeWidth", 1.0, 2.0));
        array.add(config(testShape, "strokeLineJoin",
                  StrokeLineJoin.MITER, StrokeLineJoin.BEVEL));
        array.add(config(testShape, "strokeLineCap",
                  StrokeLineCap.ROUND,  StrokeLineCap.SQUARE));
        array.add(config(testShape, "strokeMiterLimit", 0.0, 10.0));
        array.add(config(testShape, "strokeDashOffset", 0.0, 3.0));
        array.add(config(testShape, "fill", Color.BLACK, null));
        array.add(config(testShape, "stroke", null, Color.BLACK));
        array.add(config(testShape, "smooth", true, false));

        // bounding box calculation tests
        array.add(config(createTestRectangle(),
                    "strokeWidth", 0.0, 20.0,
                    "boundsInLocal",
                    box(0, 0, 100, 100), box(-10, -10, 120, 120)));

        testShape = createTestTriangle();
        array.add(config(testShape,
                    "strokeLineJoin", StrokeLineJoin.MITER, StrokeLineJoin.BEVEL,
                    testShape,
                    "boundsInLocal",
                    box(192.562, 33.68, 114.874, 171.811), 
                    box(194.756, 47.918, 110.486, 157.581),
                    new BBoxComparator(0.01)));

        testShape = createTestTriangle();
        array.add(config(testShape,
                    "strokeMiterLimit", 100.0, 0.0,
                    testShape,
                    "boundsInLocal",
                    box(192.562, 33.68, 114.874, 171.811),
                    box(194.756, 47.918, 110.486, 157.581),
                    new BBoxComparator(0.01)));

        testShape = createTestTriangle();
        array.add(config(testShape,
                    "strokeType", StrokeType.INSIDE, StrokeType.OUTSIDE,
                    testShape,
                    "boundsInLocal",
                    box(200, 50, 100, 150),
                    box(185.625, 17.877, 128.748, 192.622),
                    new BBoxComparator(0.01)));

        testShape = createTestLine();
        array.add(config(testShape,
                    "strokeLineCap", StrokeLineCap.BUTT, StrokeLineCap.SQUARE,
                    testShape,
                    "boundsInLocal",
                    box(195, 100, 10, 100),
                    box(195, 95, 10, 110),
                    new BBoxComparator(0.001)));

        return array;
    }

    public Shape_properties_Test(final Configuration configuration) {
        super(configuration);
    }

    private static Rectangle createTestRectangle() {
        Rectangle r = new Rectangle(100, 100);
        r.setStroke(Color.BLACK);
        return r;
    }

    private static Polygon createTestTriangle() {
        Polygon p = new Polygon(new double[]{200, 200, 250, 50, 300, 200});
        p.setStroke(Color.BLACK);
        p.setStrokeWidth(10);
        p.setStrokeMiterLimit(100);
        return p;
    }

    private static Line createTestLine() {
        Line l = new Line(200, 100, 200, 200);
        l.setStroke(Color.BLACK);
        l.setStrokeWidth(10);
        return l;
    }
    
}
