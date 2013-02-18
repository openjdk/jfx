/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import org.junit.Test;

import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;

public class EffectTest {
    
    @Test
    public void testAdding() {
        Bloom b = new Bloom();
        Group g = new Group();
        g.setEffect(b);
        assertEquals(b, g.getEffect());
    }
    
    @Test
    public void testRemoving() {
        Bloom b = new Bloom();
        Group g = new Group();
        g.setEffect(b);
        g.setEffect(null);
        assertNull(g.getEffect());
    }

    /*
     * Test for testing value propagation in complicated effect hierarchy
     */
    @Test
    public void testPropertyPropagationWithChaining() {
        Group root = new Group();
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        StubToolkit toolkit = (StubToolkit) Toolkit.getToolkit();
        Rectangle n1 = new Rectangle();
        Rectangle n2 = new Rectangle();
        Rectangle n3 = new Rectangle();
        root.getChildren().addAll(n1, n2);

        // Try setting effects to a node which is already in scene
        Bloom bloom = new Bloom();
        n1.setEffect(bloom);
        Glow glow = new Glow();
        bloom.setInput(glow);
        
        glow.setLevel(1.1);
        assertEquals(1.1, glow.getLevel(), 1e-100);
        toolkit.fireTestPulse();
        assertEquals(1.0f, (float) ((com.sun.scenario.effect.Glow)glow.impl_getImpl()).getLevel(), 1e-100);
        n2.setEffect(glow);
        glow.setLevel(0.5);
        assertEquals(0.5, glow.getLevel(), 1e-100);
        toolkit.fireTestPulse();
        assertEquals(0.5f, (float)((com.sun.scenario.effect.Glow)glow.impl_getImpl()).getLevel(), 1e-100);
        Bloom bloom2 = new Bloom();
        glow.setInput(bloom2);
        bloom2.setThreshold(0.1);
        assertEquals(0.1, bloom2.getThreshold(), 1e-100);
        toolkit.fireTestPulse();
        assertEquals(0.1f, (float) ((com.sun.scenario.effect.Bloom)bloom2.impl_getImpl()).getThreshold(), 1e-100);
        // Now try setting first the effect and then adding node to scene
        Bloom bloom3 = new Bloom();
        n3.setEffect(bloom3);
        bloom3.setThreshold(0.1);
        root.getChildren().add(n3);
        assertEquals(0.1, bloom3.getThreshold(), 1e-100);
        toolkit.fireTestPulse();
        assertEquals(0.1f, (float) ((com.sun.scenario.effect.Bloom)bloom3.impl_getImpl()).getThreshold(), 1e-100);
    }

    /*
     * Test for testing value propagation in complicated effect hierarchy
     * with binding
     */
    @Test
    public void testPropertyPropagationWithChainingAndBinding() {
        Group root = new Group();
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        StubToolkit toolkit = (StubToolkit) Toolkit.getToolkit();
        Rectangle n1 = new Rectangle();
        Rectangle n2 = new Rectangle();
        Rectangle n3 = new Rectangle();
        root.getChildren().addAll(n1, n2);

        // Try setting effects to a node which is already in scene
        Bloom bloom = new Bloom();
        n1.setEffect(bloom);

        ObjectProperty ov = new SimpleObjectProperty();
        bloom.inputProperty().bind(ov);

        Glow glow = new Glow();
        ov.set(glow);

        glow.setLevel(1.1);
        assertEquals(1.1, glow.getLevel(), 1e-100);
        toolkit.fireTestPulse();
        assertEquals(1.0f, (float) ((com.sun.scenario.effect.Glow)glow.impl_getImpl()).getLevel(), 1e-100);
        n2.setEffect(glow);
        glow.setLevel(0.5);
        assertEquals(0.5, glow.getLevel(), 1e-100);
        toolkit.fireTestPulse();
        assertEquals(0.5f, (float)((com.sun.scenario.effect.Glow)glow.impl_getImpl()).getLevel(), 1e-100);

        ObjectProperty ov2 = new SimpleObjectProperty();
        glow.inputProperty().bind(ov2);

        Bloom bloom2 = new Bloom();
        ov2.set(bloom2);
        bloom2.setThreshold(0.1);
        assertEquals(0.1, bloom2.getThreshold(), 1e-100);
        toolkit.fireTestPulse();
        assertEquals(0.1f, (float) ((com.sun.scenario.effect.Bloom)bloom2.impl_getImpl()).getThreshold(), 1e-100);

        // now change the bound value
        // previous input should be deregistred
        Bloom bloom3 = new Bloom();
        ov2.set(bloom3);
        bloom3.setThreshold(0.1);
       
        assertEquals(0.1, bloom3.getThreshold(), 1e-100);
        assertTrue(glow.impl_isEffectDirty());
        assertTrue(bloom.impl_isEffectDirty());
        toolkit.fireTestPulse();
        assertEquals(0.1f, (float) ((com.sun.scenario.effect.Bloom)bloom3.impl_getImpl()).getThreshold(), 1e-100);

        bloom2.setThreshold(0.2);
        // test that previously bound effect is correctly deregistred and doesn't propagate its changes via listeners
        assertFalse(glow.impl_isEffectDirty());
        assertEquals(0.2, bloom2.getThreshold(), 1e-100);
        toolkit.fireTestPulse();
        assertEquals(0.1f, (float) ((com.sun.scenario.effect.Bloom)bloom2.impl_getImpl()).getThreshold(), 1e-100);
    }

    @Test
    public void testLongCycle() {
        // Testing extremely long cycle of effects
        Blend blend = new Blend();
        Bloom bloom = new Bloom();
        BoxBlur boxBlur = new BoxBlur();
        ColorAdjust colorAdjust = new ColorAdjust();
        DisplacementMap displacementMap = new DisplacementMap();
        DropShadow dropShadow = new DropShadow();
        GaussianBlur gaussianBlur = new GaussianBlur();
        Glow glow = new Glow();
        InnerShadow innerShadow = new InnerShadow();
        MotionBlur motionBlur = new MotionBlur();
        PerspectiveTransform perspectiveTransform = new PerspectiveTransform();
        Reflection reflection = new Reflection();
        SepiaTone sepiaTone = new SepiaTone();
        Shadow shadow = new Shadow();

        blend.setTopInput(bloom);
        bloom.setInput(boxBlur);
        boxBlur.setInput(colorAdjust);
        colorAdjust.setInput(displacementMap);
        displacementMap.setInput(dropShadow);
        dropShadow.setInput(gaussianBlur);
        gaussianBlur.setInput(glow);
        glow.setInput(innerShadow);
        innerShadow.setInput(motionBlur);
        motionBlur.setInput(perspectiveTransform);
        perspectiveTransform.setInput(reflection);
        reflection.setInput(sepiaTone);
        sepiaTone.setInput(shadow);
        try {
            shadow.setInput(blend);
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException e) {
            assertEquals(null, shadow.getInput());
        }
    }
}
