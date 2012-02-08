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
package javafx.scene.effect;

import static com.sun.javafx.test.TestHelper.box;

import java.util.ArrayList;
import java.util.Collection;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sun.javafx.test.BBoxComparator;
import com.sun.javafx.test.PropertiesTestBase;

@RunWith(Parameterized.class)
public final class ColorInput_properties_Test extends PropertiesTestBase {

    @Parameters
    public static Collection data() {
        ArrayList array = new ArrayList();

        // simple property tests
        final ColorInput testColorInput = new ColorInput();

        array.add(config(testColorInput, "paint", Color.RED, Color.BLUE));
        array.add(config(testColorInput, "x", 0.0, 100.0));
        array.add(config(testColorInput, "y", 0.0, 100.0));
        array.add(config(testColorInput, "width", 50.0, 150.0));
        array.add(config(testColorInput, "height", 50.0, 150.0));

        // bounding box calculation tests

        Node testNode = createTestNode();
        array.add(config(testNode.getEffect(),
                "width", 0.0, 50.0,
                testNode,
                "boundsInLocal",
                box(0.0, 0.0, 0.0, 50.0),
                box(0.0, 0.0, 50.0, 50.0),
                new BBoxComparator(0.01)));

        testNode = createTestNode();
        array.add(config(testNode.getEffect(),
                "height", 0.0, 50.0,
                testNode,
                "boundsInLocal",
                box(0.0, 0.0, 50.0, 0.0),
                box(0.0, 0.0, 50.0, 50.0),
                new BBoxComparator(0.01)));

        testNode = createTestNode();
        array.add(config(testNode.getEffect(),
                "x", 0.0, 50.0,
                testNode,
                "boundsInLocal",
                box(0.0, 0.0, 50.0, 50.0),
                box(50.0, 0.0, 50.0, 50.0),
                new BBoxComparator(0.01)));

        testNode = createTestNode();
        array.add(config(testNode.getEffect(),
                "y", 0.0, 50.0,
                testNode,
                "boundsInLocal",
                box(0.0, 0.0, 50.0, 50.0),
                box(0.0, 50.0, 50.0, 50.0),
                new BBoxComparator(0.01)));

        testNode = createTestNodeWithChainedEffect();
        array.add(config(((ColorAdjust)testNode.getEffect()).getInput(),
                "width", 0.0, 50.0,
                testNode,
                "boundsInLocal",
                box(0.0, 0.0, 0.0, 50.0),
                box(0.0, 0.0, 50.0, 50.0),
                new BBoxComparator(0.01)));

        testNode = createTestNodeWithChainedEffect();
        array.add(config(((ColorAdjust)testNode.getEffect()).getInput(),
                "height", 0.0, 50.0,
                testNode,
                "boundsInLocal",
                box(0.0, 0.0, 50.0, 0.0),
                box(0.0, 0.0, 50.0, 50.0),
                new BBoxComparator(0.01)));

        testNode = createTestNodeWithChainedEffect();
        array.add(config(((ColorAdjust)testNode.getEffect()).getInput(),
                "x", 0.0, 50.0,
                testNode,
                "boundsInLocal",
                box(0.0, 0.0, 50.0, 50.0),
                box(50.0, 0.0, 50.0, 50.0),
                new BBoxComparator(0.01)));

        testNode = createTestNodeWithChainedEffect();
        array.add(config(((ColorAdjust)testNode.getEffect()).getInput(),
                "y", 0.0, 50.0,
                testNode,
                "boundsInLocal",
                box(0.0, 0.0, 50.0, 50.0),
                box(0.0, 50.0, 50.0, 50.0),
                new BBoxComparator(0.01)));

        return array;
    }

    public ColorInput_properties_Test(final Configuration configuration) {
        super(configuration);
    }

    private static Rectangle createTestNode() {
        Rectangle r = new Rectangle(100, 100);
        ColorInput flood = new ColorInput();
        flood.setHeight(50);
        flood.setWidth(50);
        r.setEffect(flood);
        return r;
    }

    private static Rectangle createTestNodeWithChainedEffect() {
        Rectangle r = new Rectangle(100, 100);
        ColorInput flood = new ColorInput();
        flood.setHeight(50);
        flood.setWidth(50);

        ColorAdjust c = new ColorAdjust();
        c.setInput(flood);
        r.setEffect(c);
        return r;
    }
}
