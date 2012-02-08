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
package javafx.scene.effect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.image.TestImages;

import org.junit.Before;
import org.junit.Test;

import com.sun.javafx.geom.Point2D;
import com.sun.javafx.tk.Toolkit;

public class ImageInputTest extends EffectsTestBase {
    private ImageInput effect;

    @Before
    public void setUp() {
        effect = new ImageInput();
        setupTest(effect);
    }

    @Test
    public void testSetX() {
        // try setting correct value
        effect.setX(1.0f);
        assertEquals(1.0f, effect.getX(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.Identity) effect.impl_getImpl()).getLocation().x, 1e-100);
    }

    @Test
    public void testDefaultX() {
        // default value should be 0
        assertEquals(0, effect.getX(), 1e-100);
        assertEquals(0, effect.xProperty().get(), 1e-100);
        pulse();
        assertEquals(0, ((com.sun.scenario.effect.Identity) effect.impl_getImpl()).getLocation().x, 1e-100);
    }

    @Test
    public void testSetY() {
        // try setting correct value
        effect.setY(1.0f);
        assertEquals(1.0f, effect.getY(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.Identity) effect.impl_getImpl()).getLocation().y, 1e-100);
    }

    @Test
    public void testDefaultY() {
        // default value should be 0
        assertEquals(0, effect.getY(), 1e-100);
        assertEquals(0, effect.yProperty().get(), 1e-100);
        pulse();
        assertEquals(0, ((com.sun.scenario.effect.Identity) effect.impl_getImpl()).getLocation().y, 1e-100);
    }
    
    @Test
    public void testSetSource() {
        // try setting non-existing image
        Image i = new Image("test");
        effect.setSource(i);
        assertEquals(i, effect.getSource());
        pulse();
        assertEquals(null, ((com.sun.scenario.effect.Identity) effect.impl_getImpl()).getSource());
        // try setting correct one
        effect.setSource(TestImages.TEST_IMAGE_32x32);
        pulse();
        assertEquals(TestImages.TEST_IMAGE_32x32, effect.getSource());
        assertEquals(Toolkit.getToolkit().toFilterable(TestImages.TEST_IMAGE_32x32).getPhysicalHeight(),
                ((com.sun.scenario.effect.Identity) effect.impl_getImpl()).getSource().getPhysicalHeight());
        assertEquals(Toolkit.getToolkit().toFilterable(TestImages.TEST_IMAGE_32x32).getPhysicalWidth(),
                ((com.sun.scenario.effect.Identity) effect.impl_getImpl()).getSource().getPhysicalWidth());
    }

    @Test
    public void testDefaultSource() {
        // default value should be null
        assertNull(effect.getSource());
        assertNull(effect.sourceProperty().get());
        pulse();
        assertNull(((com.sun.scenario.effect.Identity) effect.impl_getImpl()).getSource());
    }

    @Test
    public void testXSynced() throws Exception {
        Point2D pt = (Point2D) getDoublePropertySynced(
                "javafx.scene.effect.ImageInput", "x",
                "com.sun.scenario.effect.Identity", "location", 10);
        assertEquals(10, pt.x, 1e-100);
    }

    @Test
    public void testYSynced() throws Exception {
        Point2D pt = (Point2D) getDoublePropertySynced(
                "javafx.scene.effect.ImageInput", "y",
                "com.sun.scenario.effect.Identity", "location", 10);
        assertEquals(10, pt.y, 1e-100);
    }

    @Test
    public void testSourceSynced() throws Exception {
        ObjectProperty source = new SimpleObjectProperty();
        effect.sourceProperty().bind(source);
        source.set(TestImages.TEST_IMAGE_32x32);
        pulse();
        assertEquals(TestImages.TEST_IMAGE_32x32, effect.getSource());
        assertEquals(Toolkit.getToolkit().toFilterable(TestImages.TEST_IMAGE_32x32).getPhysicalHeight(),
                ((com.sun.scenario.effect.Identity) effect.impl_getImpl()).getSource().getPhysicalHeight());
        assertEquals(Toolkit.getToolkit().toFilterable(TestImages.TEST_IMAGE_32x32).getPhysicalWidth(),
                ((com.sun.scenario.effect.Identity) effect.impl_getImpl()).getSource().getPhysicalWidth());
    }

    @Test
    public void testCreateWithParams() {
        effect = new ImageInput(TestImages.TEST_IMAGE_32x32);
        setupTest(effect);
        assertEquals(TestImages.TEST_IMAGE_32x32, effect.getSource());
        pulse();
        assertEquals(Toolkit.getToolkit().toFilterable(TestImages.TEST_IMAGE_32x32).getPhysicalHeight(),
                ((com.sun.scenario.effect.Identity) effect.impl_getImpl()).getSource().getPhysicalHeight());
        assertEquals(Toolkit.getToolkit().toFilterable(TestImages.TEST_IMAGE_32x32).getPhysicalWidth(),
                ((com.sun.scenario.effect.Identity) effect.impl_getImpl()).getSource().getPhysicalWidth());
    }

    @Test
    public void testCreateWithParams3() {
        effect = new ImageInput(TestImages.TEST_IMAGE_32x32, 1, 2);
        setupTest(effect);
        assertEquals(TestImages.TEST_IMAGE_32x32, effect.getSource());
        assertEquals(1, effect.getX(), 1e-100);
        assertEquals(2, effect.getY(), 1e-100);
        pulse();
        assertEquals(Toolkit.getToolkit().toFilterable(TestImages.TEST_IMAGE_32x32).getPhysicalHeight(),
                ((com.sun.scenario.effect.Identity) effect.impl_getImpl()).getSource().getPhysicalHeight());
        assertEquals(Toolkit.getToolkit().toFilterable(TestImages.TEST_IMAGE_32x32).getPhysicalWidth(),
                ((com.sun.scenario.effect.Identity) effect.impl_getImpl()).getSource().getPhysicalWidth());        
        assertEquals(1.0f, ((com.sun.scenario.effect.Identity) effect.impl_getImpl()).getLocation().x, 1e-100);
        assertEquals(2.0f, ((com.sun.scenario.effect.Identity) effect.impl_getImpl()).getLocation().y, 1e-100);
    }
    
    @Test
    public void testCreateWithDefaultParams() {
        effect = new ImageInput(null);
        setupTest(effect);
        assertNull(effect.getSource());
        pulse();
        assertNull(((com.sun.scenario.effect.Identity) effect.impl_getImpl()).getSource());
    }

    @Test
    public void testCreateWithDefaultParams3() {
        effect = new ImageInput(null, 0, 0);
        setupTest(effect);
        assertNull(effect.getSource());
        assertEquals(0, effect.getX(), 1e-100);
        assertEquals(0, effect.getY(), 1e-100);
        pulse();
        assertNull(((com.sun.scenario.effect.Identity) effect.impl_getImpl()).getSource());
        assertEquals(0f, ((com.sun.scenario.effect.Identity) effect.impl_getImpl()).getLocation().x, 1e-100);
        assertEquals(0f, ((com.sun.scenario.effect.Identity) effect.impl_getImpl()).getLocation().y, 1e-100);
    }
}
