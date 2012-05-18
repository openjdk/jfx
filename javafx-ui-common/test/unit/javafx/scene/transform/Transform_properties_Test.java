/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.transform;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sun.javafx.test.PropertiesTestBase;
import javafx.geometry.Point3D;

@RunWith(Parameterized.class)
public class Transform_properties_Test extends PropertiesTestBase {
    @Parameters
    public static Collection data() {
        final Affine a = new Affine(
                1,  2,  3,  4,
                5,  6,  7,  8,
                9, 10, 11, 12);

        final Rotate r = new Rotate();
        final Shear s = new Shear();
        final Translate t = new Translate();
        final Scale c = new Scale();

        return Arrays.asList(new Object[] {
            config(a, "mxx", 10.0, 20.0),
            config(a, "mxy", 10.0, 20.0),
            config(a, "mxz", 10.0, 20.0),
            config(a, "tx", 10.0, 20.0),
            config(a, "myx", 10.0, 20.0),
            config(a, "myy", 10.0, 20.0),
            config(a, "myz", 10.0, 20.0),
            config(a, "ty", 10.0, 20.0),
            config(a, "mzx", 10.0, 20.0),
            config(a, "mzy", 10.0, 20.0),
            config(a, "mzz", 10.0, 20.0),
            config(a, "tz", 10.0, 20.0),
            config(r, "angle", 10.0, 20.0),
            config(r, "axis", new Point3D(10, 20, 30), new Point3D(30, 20, 10)),
            config(r, "pivotX", 10.0, 20.0),
            config(r, "pivotY", 10.0, 20.0),
            config(r, "pivotZ", 10.0, 20.0),
            config(s, "x", 10.0, 20.0),
            config(s, "y", 10.0, 20.0),
            config(s, "pivotX", 10.0, 20.0),
            config(s, "pivotY", 10.0, 20.0),
            config(t, "x", 10.0, 20.0),
            config(t, "y", 10.0, 20.0),
            config(t, "z", 10.0, 20.0),
            config(c, "x", 10.0, 20.0),
            config(c, "y", 10.0, 20.0),
            config(c, "z", 10.0, 20.0),
            config(c, "pivotX", 10.0, 20.0),
            config(c, "pivotY", 10.0, 20.0),
            config(c, "pivotZ", 10.0, 20.0),
        });
    }

    public Transform_properties_Test(final Configuration configuration) {
        super(configuration);
    }
}
