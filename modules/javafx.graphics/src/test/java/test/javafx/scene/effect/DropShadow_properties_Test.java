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

package test.javafx.scene.effect;

import static test.com.sun.javafx.test.TestHelper.box;

import java.util.ArrayList;
import java.util.stream.Stream;

import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import org.junit.jupiter.params.provider.Arguments;
import test.com.sun.javafx.test.PropertiesTestBase;
import test.com.sun.javafx.test.BBoxComparator;

public final class DropShadow_properties_Test extends PropertiesTestBase {

    public static Stream<Arguments> data() {
        ArrayList<Arguments> array = new ArrayList<Arguments>();

        // simple property tests
        final DropShadow testDropShadow = new DropShadow();

        array.add(config(testDropShadow, "input", null, new BoxBlur()));
        array.add(config(testDropShadow, "radius", 50.0, 100.0));
        array.add(config(testDropShadow, "width", 100.0, 200.0));
        array.add(config(testDropShadow, "height", 100.0, 200.0));
        array.add(config(testDropShadow, "blurType",
                  BlurType.GAUSSIAN, BlurType.THREE_PASS_BOX));
        array.add(config(testDropShadow, "spread", 0.0, 0.5));
        array.add(config(testDropShadow, "color", Color.BLACK, Color.RED));
        array.add(config(testDropShadow, "offsetX", 0.0, 50.0));
        array.add(config(testDropShadow, "offsetY", 0.0, 50.0));

        // bounding box calculation tests

        Node testNode = createTestNode();
        array.add(config(testNode.getEffect(),
                "radius", 10.0, 21.0,
                testNode,
                "boundsInLocal",
                box(-9.0, -9.0, 118.0, 118.0),
                box(-20.0, -20.0, 140.0, 140.0),
                new BBoxComparator(0.01)));

        testNode = createTestNode();
        array.add(config(testNode.getEffect(),
                "width", 10.0, 21.0,
                testNode,
                "boundsInLocal",
                box(-3.0, -9.0, 106.0, 118.0),
                box(-9.0, -9.0, 118.0, 118.0),
                new BBoxComparator(0.01)));

        testNode = createTestNode();
        array.add(config(testNode.getEffect(),
                "height", 10.0, 21.0,
                testNode,
                "boundsInLocal",
                box(-9.0, -3.0, 118.0, 106.0),
                box(-9.0, -9.0, 118.0, 118.0),
                new BBoxComparator(0.01)));

        testNode = createTestNode();
        array.add(config(testNode.getEffect(),
                "offsetX", 10.0, 21.0,
                testNode,
                "boundsInLocal",
                box(0.0, -9.0, 119.0, 118.0),
                box(0.0, -9.0, 130.0, 118.0),
                new BBoxComparator(0.01)));

        testNode = createTestNode();
        array.add(config(testNode.getEffect(),
                "offsetY", 10.0, 21.0,
                testNode,
                "boundsInLocal",
                box(-9.0, 0.0, 118.0, 119.0),
                box(-9.0, 0.0, 118.0, 130.0),
                new BBoxComparator(0.01)));

        testNode = createTestNode();
        array.add(config(testNode.getEffect(),
                "blurType", BlurType.ONE_PASS_BOX, BlurType.THREE_PASS_BOX,
                testNode,
                "boundsInLocal",
                box(-3.0, -3.0, 106.0, 106.0),
                box(-9.0, -9.0, 118.0, 118.0),
                new BBoxComparator(0.01)));

        testNode = createTestNode();
        array.add(config(testNode.getEffect(),
                "blurType", BlurType.TWO_PASS_BOX, BlurType.GAUSSIAN,
                testNode,
                "boundsInLocal",
                box(-6.0, -6.0, 112.0, 112.0),
                box(-10.0, -10.0, 120.0, 120.0),
                new BBoxComparator(0.01)));

        testNode = createTestNode();
        array.add(config(testNode.getEffect(),
                "input", null, new BoxBlur(),
                testNode,
                "boundsInLocal",
                box(-9.0, -9.0, 118.0, 118.0),
                box(-11.0, -11.0, 122.0, 122.0),
                new BBoxComparator(0.01)));

        // DropShadow chained to another effect
        testNode = createTestNodeWithChainedEffect();
        array.add(config(((ColorAdjust)testNode.getEffect()).getInput(),
                "radius", 10.0, 21.0,
                testNode,
                "boundsInLocal",
                box(-9.0, -9.0, 118.0, 118.0),
                box(-20.0, -20.0, 140.0, 140.0),
                new BBoxComparator(0.01)));

        testNode = createTestNodeWithChainedEffect();
        array.add(config(((ColorAdjust)testNode.getEffect()).getInput(),
                "width", 10.0, 21.0,
                testNode,
                "boundsInLocal",
                box(-3.0, -9.0, 106.0, 118.0),
                box(-9.0, -9.0, 118.0, 118.0),
                new BBoxComparator(0.01)));

        testNode = createTestNodeWithChainedEffect();
        array.add(config(((ColorAdjust)testNode.getEffect()).getInput(),
                "height", 10.0, 21.0,
                testNode,
                "boundsInLocal",
                box(-9.0, -3.0, 118.0, 106.0),
                box(-9.0, -9.0, 118.0, 118.0),
                new BBoxComparator(0.01)));

        testNode = createTestNodeWithChainedEffect();
        array.add(config(((ColorAdjust)testNode.getEffect()).getInput(),
                "offsetX", 10.0, 21.0,
                testNode,
                "boundsInLocal",
                box(0.0, -9.0, 119.0, 118.0),
                box(0.0, -9.0, 130.0, 118.0),
                new BBoxComparator(0.01)));

        testNode = createTestNodeWithChainedEffect();
        array.add(config(((ColorAdjust)testNode.getEffect()).getInput(),
                "offsetY", 10.0, 21.0,
                testNode,
                "boundsInLocal",
                box(-9.0, 0.0, 118.0, 119.0),
                box(-9.0, 0.0, 118.0, 130.0),
                new BBoxComparator(0.01)));

        testNode = createTestNodeWithChainedEffect();
        array.add(config(((ColorAdjust)testNode.getEffect()).getInput(),
                "blurType", BlurType.ONE_PASS_BOX, BlurType.THREE_PASS_BOX,
                testNode,
                "boundsInLocal",
                box(-3.0, -3.0, 106.0, 106.0),
                box(-9.0, -9.0, 118.0, 118.0),
                new BBoxComparator(0.01)));

        testNode = createTestNodeWithChainedEffect();
        array.add(config(((ColorAdjust)testNode.getEffect()).getInput(),
                "blurType", BlurType.TWO_PASS_BOX, BlurType.GAUSSIAN,
                testNode,
                "boundsInLocal",
                box(-6.0, -6.0, 112.0, 112.0),
                box(-10.0, -10.0, 120.0, 120.0),
                new BBoxComparator(0.01)));

        testNode = createTestNodeWithChainedEffect();
        array.add(config(((ColorAdjust)testNode.getEffect()).getInput(),
                "input", null, new BoxBlur(),
                testNode,
                "boundsInLocal",
                box(-9.0, -9.0, 118.0, 118.0),
                box(-11.0, -11.0, 122.0, 122.0),
                new BBoxComparator(0.01)));

        return array.stream();
    }

    private static Rectangle createTestNode() {
        Rectangle r = new Rectangle(100, 100);
        r.setEffect(new DropShadow());
        return r;
    }

    private static Rectangle createTestNodeWithChainedEffect() {
        Rectangle r = new Rectangle(100, 100);
        ColorAdjust c = new ColorAdjust();
        c.setInput(new DropShadow());
        r.setEffect(c);
        return r;
    }
}
