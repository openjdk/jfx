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

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sun.javafx.test.PropertiesTestBase;

@RunWith(Parameterized.class)
public final class Arc_properties_Test extends PropertiesTestBase {
    @Parameters
    public static Collection data() {
        final Arc testArc = new Arc();

        return Arrays.asList(new Object[] {
            config(testArc, "centerX", 0.0, 100.0),
            config(testArc, "centerY", 0.0, 100.0),
            config(testArc, "radiusX", 50.0, 150.0),
            config(testArc, "radiusY", 50.0, 150.0),
            config(testArc, "startAngle", 0.0, 50.0),
            config(testArc, "length", 40.0, 80.0),
            config(testArc, "type", ArcType.OPEN, ArcType.ROUND)
        });
    }

    public Arc_properties_Test(final Configuration configuration) {
        super(configuration);
    }
}
