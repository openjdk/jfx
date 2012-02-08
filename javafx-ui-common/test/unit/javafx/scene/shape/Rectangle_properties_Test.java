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

import java.util.Arrays;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sun.javafx.test.PropertiesTestBase;

@RunWith(Parameterized.class)
public final class Rectangle_properties_Test extends PropertiesTestBase {
    @Parameters
    public static Collection data() {
        final Rectangle testRectangle = createTestRectangle();
        
        return Arrays.asList(new Object[] {
            config(testRectangle, "x", 0.0, 100.0),
            config(testRectangle, "y", 0.0, 200.0),
            config(testRectangle, "width", 50.0, 200.0),
            config(testRectangle, "height", 50.0, 200.0),
            config(testRectangle, "arcWidth", 10.0, 20.0),
            config(testRectangle, "arcHeight", 10.0, 20.0),

            config(createTestRectangle(),
                   "x", 0.0, 100.0,
                   "boundsInLocal",
                   box(0, 0, 100, 100), box(100, 0, 100, 100)),
            config(createTestRectangle(),
                   "y", 0.0, 100.0,
                   "boundsInLocal",
                   box(0, 0, 100, 100), box(0, 100, 100, 100)),
            config(createTestRectangle(),
                   "width", 50.0, 200.0,
                   "boundsInLocal",
                   box(0, 0, 50, 100), box(0, 0, 200, 100)),
            config(createTestRectangle(),
                   "height", 50.0, 200.0,
                   "boundsInLocal",
                   box(0, 0, 100, 50), box(0, 0, 100, 200)),
            config(createTestRectangle(),
                   "x", 0.0, 100.0,
                   "layoutBounds",
                   box(0, 0, 100, 100), box(100, 0, 100, 100)),
            config(createTestRectangle(),
                   "y", 0.0, 100.0,
                   "layoutBounds",
                   box(0, 0, 100, 100), box(0, 100, 100, 100)),
            config(createTestRectangle(),
                   "width", 50.0, 200.0,
                   "layoutBounds",
                   box(0, 0, 50, 100), box(0, 0, 200, 100)),
            config(createTestRectangle(),
                   "height", 50.0, 200.0,
                   "layoutBounds",
                   box(0, 0, 100, 50), box(0, 0, 100, 200)),
            config(createTestRectangle(),
                   "translateX", 0.0, 100.0,
                   "boundsInParent", 
                   box(0, 0, 100, 100), box(100, 0, 100, 100)),
            config(createTestRectangle(),
                   "translateY", 0.0, 100.0,
                   "boundsInParent",
                   box(0, 0, 100, 100), box(0, 100, 100, 100))
        });
    }

    public Rectangle_properties_Test(final Configuration configuration) {
        super(configuration);
    }

    private static Rectangle createTestRectangle() {
        return new Rectangle(100, 100);
    }
}
