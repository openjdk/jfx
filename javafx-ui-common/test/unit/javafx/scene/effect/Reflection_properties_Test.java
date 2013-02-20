/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.shape.Rectangle;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sun.javafx.test.BBoxComparator;
import com.sun.javafx.test.PropertiesTestBase;

@RunWith(Parameterized.class)
public final class Reflection_properties_Test extends PropertiesTestBase {

    @Parameters
    public static Collection data() {
        ArrayList array = new ArrayList();

        // simple property tests
        final Reflection testReflection = new Reflection();

        array.add(config(testReflection, "input", null, new BoxBlur()));
        array.add(config(testReflection, "topOffset", 0.0, 50.0));
        array.add(config(testReflection, "topOpacity", 0.5, 0.0));
        array.add(config(testReflection, "bottomOpacity", 1.0, 0.8));
        array.add(config(testReflection, "fraction", 0.75, 0.5));

        // bounding box calculation tests

        Node testNode = createTestNode();
        array.add(config(testNode.getEffect(),
                "topOffset", 10.0, 20.0,
                testNode,
                "boundsInLocal",
                box(0.0, 0.0, 100.0, 185.0),
                box(0.0, 0.0, 100.0, 195.0),
                new BBoxComparator(0.01)));

        testNode = createTestNode();
        array.add(config(testNode.getEffect(),
                "fraction", 0.0, 1.0,
                testNode,
                "boundsInLocal",
                box(0.0, 0.0, 100.0, 100.0),
                box(0.0, 0.0, 100.0, 200.0),
                new BBoxComparator(0.01)));

        testNode = createTestNode();
        array.add(config(testNode.getEffect(),
                "input", null, new BoxBlur(),
                testNode,
                "boundsInLocal",
                box(0.0, 0.0, 100.0, 175.0),
                box(-2.0, -2.0, 104.0, 182.0),
                new BBoxComparator(0.01)));

        testNode = createTestNodeWithChainedEffect();
        array.add(config(((Glow)testNode.getEffect()).getInput(),
                "topOffset", 10.0, 20.0,
                testNode,
                "boundsInLocal",
                box(0.0, 0.0, 100.0, 185.0),
                box(0.0, 0.0, 100.0, 195.0),
                new BBoxComparator(0.01)));

        testNode = createTestNodeWithChainedEffect();
        array.add(config(((Glow)testNode.getEffect()).getInput(),
                "fraction", 0.0, 1.0,
                testNode,
                "boundsInLocal",
                box(0.0, 0.0, 100.0, 100.0),
                box(0.0, 0.0, 100.0, 200.0),
                new BBoxComparator(0.01)));

        testNode = createTestNodeWithChainedEffect();
        array.add(config(((Glow)testNode.getEffect()).getInput(),
                "input", null, new BoxBlur(),
                testNode,
                "boundsInLocal",
                box(0.0, 0.0, 100.0, 175.0),
                box(-2.0, -2.0, 104.0, 182.0),
                new BBoxComparator(0.01)));

        return array;
    }

    public Reflection_properties_Test(final Configuration configuration) {
        super(configuration);
    }

    private static Rectangle createTestNode() {
        Rectangle r = new Rectangle(100, 100);
        r.setEffect(new Reflection());
        return r;
    }

    private static Rectangle createTestNodeWithChainedEffect() {
        Rectangle r = new Rectangle(100, 100);
        Glow g = new Glow();
        g.setInput(new Reflection());
        r.setEffect(g);
        return r;
    }
}
