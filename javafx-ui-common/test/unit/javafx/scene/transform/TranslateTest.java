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

package javafx.scene.transform;

import static javafx.scene.transform.TransformTest.assertTx;
import javafx.scene.shape.Rectangle;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

import com.sun.javafx.test.TransformHelper;
import com.sun.javafx.geom.transform.BaseTransform;


public class TranslateTest {

    @Test
    public void testTranslate() {
        final Translate trans = new Translate() {{
            setX(25);
            setY(52);
        }};
        final Rectangle n = new Rectangle();
        n.getTransforms().add(trans);

        assertTx(n, BaseTransform.getTranslateInstance(25, 52));
        TransformHelper.assertMatrix(trans,
                1, 0, 0, 25,
                0, 1, 0, 52,
                0, 0, 1,  0);

        trans.setX(34);
        Assert.assertEquals(34, trans.getX(), 1e-100);
        assertTx(n, BaseTransform.getTranslateInstance(34, 52));
        TransformHelper.assertMatrix(trans,
                1, 0, 0, 34,
                0, 1, 0, 52,
                0, 0, 1,  0);


        trans.setY(67);
        assertTx(n, BaseTransform.getTranslateInstance(34, 67));
        TransformHelper.assertMatrix(trans,
                1, 0, 0, 34,
                0, 1, 0, 67,
                0, 0, 1,  0);

        trans.setZ(33);
        TransformHelper.assertMatrix(trans,
                1, 0, 0, 34,
                0, 1, 0, 67,
                0, 0, 1, 33);
    }

    @Test
    public void testCopying() {
        final Translate trans = new Translate(34, 67, 33);

        Transform copy = trans.clone();

        TransformHelper.assertMatrix(copy,
                1, 0, 0, 34,
                0, 1, 0, 67,
                0, 0, 1, 33);
    }

    @Test public void testToString() {
        final Translate trans = new Translate(8, 15);

        String s = trans.toString();

        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    @Test public void testBoundPropertySynced_X() throws Exception {
        TransformTest.checkDoublePropertySynced(new Translate(3, 3, 0), "x", 22.0);
    }

    @Test public void testBoundPropertySynced_Y() throws Exception {
        TransformTest.checkDoublePropertySynced(new Translate(3, 3, 0), "y", 33.0);
    }

    @Test public void testBoundPropertySynced_Z() throws Exception {
        TransformTest.checkDoublePropertySynced(new Translate(3, 3, 0), "z", 44.0);
    }
}
