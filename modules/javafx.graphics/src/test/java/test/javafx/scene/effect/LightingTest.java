/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.scenario.effect.Color4f;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.Effect;
import javafx.scene.effect.EffectShim;
import javafx.scene.effect.Light;
import javafx.scene.effect.LightShim;
import javafx.scene.effect.Lighting;
import javafx.scene.effect.Shadow;
import javafx.scene.paint.Color;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class LightingTest extends EffectsTestBase {
    private Lighting effect;

    @BeforeEach
    public void setUp() {
        effect = new Lighting();
        setupTest(effect);
    }

    @Test
    public void testSetDiffuseConstant() {
        // try setting correct value
        effect.setDiffuseConstant(1.1f);
        assertEquals(1.1f, (float) effect.getDiffuseConstant(), 1e-100);
        pulse();
        assertEquals(1.1f, ((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getDiffuseConstant(), 1e-100);
    }

    @Test
    public void testDefaultDiffuseConstant() {
        // default value should be 1
        assertEquals(1f, (float) effect.getDiffuseConstant(), 1e-100);
        assertEquals(1f, (float) effect.diffuseConstantProperty().get(), 1e-100);
        pulse();
        assertEquals(1f, ((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getDiffuseConstant(), 1e-100);
    }

    @Test
    public void testMinDiffuseConstant() {
        // 0 should be ok
        effect.setDiffuseConstant(0);
        // try setting value smaller than minimal
        effect.setDiffuseConstant(-0.1f);
        assertEquals(-0.1f, (float) effect.getDiffuseConstant(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getDiffuseConstant(), 1e-100);
    }

    @Test
    public void testMaxDiffuseConstant() {
        // 2 should be ok
        effect.setDiffuseConstant(2);
        // try setting value greater than maximal
        effect.setDiffuseConstant(2.1f);
        assertEquals(2.1f, (float) effect.getDiffuseConstant(), 1e-100);
        pulse();
        assertEquals(2f, ((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getDiffuseConstant(), 1e-100);
    }

    @Test
    public void testSetSpecularConstant() {
        // try setting correct value
        effect.setSpecularConstant(1.0f);
        assertEquals(1.0f, (float) effect.getSpecularConstant(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getSpecularConstant(), 1e-100);
    }

    @Test
    public void testDefaultSpecularConstant() {
        // default value should be 0.3
        assertEquals(0.3f, (float) effect.getSpecularConstant(), 1e-100);
        assertEquals(0.3f, (float) effect.specularConstantProperty().get(), 1e-100);
        pulse();
        assertEquals(0.3f, ((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getSpecularConstant(), 1e-100);
    }

    @Test
    public void testMinSpecularConstant() {
        // 0 should be ok
        effect.setSpecularConstant(0);
        // try setting value smaller than minimal
        effect.setSpecularConstant(-0.1f);
        assertEquals(-0.1f, (float) effect.getSpecularConstant(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getSpecularConstant(), 1e-100);
    }

    @Test
    public void testMaxSpecularConstant() {
        // 2 should be ok
        effect.setSpecularConstant(2);
        // try setting value greater than maximal
        effect.setSpecularConstant(2.1f);
        assertEquals(2.1f, (float) effect.getSpecularConstant(), 1e-100);
        pulse();
        assertEquals(2f, ((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getSpecularConstant(), 1e-100);
    }

    @Test
    public void testSetSpecularExponent() {
        // try setting correct value
        effect.setSpecularExponent(1.0f);
        assertEquals(1.0f, (float) effect.getSpecularExponent(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getSpecularExponent(), 1e-100);
    }

    @Test
    public void testDefaultSpecularExponent() {
        // default value should be 20
        assertEquals(20f, (float) effect.getSpecularExponent(), 1e-100);
        assertEquals(20f, (float) effect.specularExponentProperty().get(), 1e-100);
        pulse();
        assertEquals(20f, ((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getSpecularExponent(), 1e-100);
    }

    @Test
    public void testMinSpecularExponent() {
        // 0 should be ok
        effect.setSpecularExponent(0);
        // try setting value smaller than minimal
        effect.setSpecularExponent(-0.1f);
        assertEquals(-0.1f, (float) effect.getSpecularExponent(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getSpecularExponent(), 1e-100);
    }

    @Test
    public void testMaxSpecularExponent() {
        // 40 should be ok
        effect.setSpecularExponent(40);
        // try setting value greater than maximal
        effect.setSpecularExponent(40.1f);
        assertEquals(40.1f, (float) effect.getSpecularExponent(), 1e-100);
        pulse();
        assertEquals(40f, ((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getSpecularExponent(), 1e-100);
    }

    @Test
    public void testSetSurfaceScale() {
        // try setting correct value
        effect.setSurfaceScale(1.0f);
        assertEquals(1.0f, (float) effect.getSurfaceScale(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getSurfaceScale(), 1e-100);
    }

    @Test
    public void testDefaultSurfaceScale() {
        // default value should be 1.5
        assertEquals(1.5f, (float) effect.getSurfaceScale(), 1e-100);
        assertEquals(1.5f, (float) effect.surfaceScaleProperty().get(), 1e-100);
        pulse();
        assertEquals(1.5f, ((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getSurfaceScale(), 1e-100);
    }

    @Test
    public void testMinSurfaceScale() {
        // 0 should be ok
        effect.setSurfaceScale(0);
        // try setting value smaller than minimal
        effect.setSurfaceScale(-0.1f);
        assertEquals(-0.1f, (float) effect.getSurfaceScale(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getSurfaceScale(), 1e-100);
    }

    @Test
    public void testMaxSurfaceScale() {
        // 10 should be ok
        effect.setSurfaceScale(10);
        // try setting value greater than maximal
        effect.setSurfaceScale(10.1f);
        assertEquals(10.1f, (float) effect.getSurfaceScale(), 1e-100);
        pulse();
        assertEquals(10f, ((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getSurfaceScale(), 1e-100);
    }

    @Test
    public void testBumpInput() {
        // default is not null
        assertNotNull(effect.getBumpInput());
        // default is shadow effect with radius of 10
        Effect e = effect.getBumpInput();
        assertTrue(e instanceof Shadow);
        assertEquals(10f, (float) ((Shadow)e).getRadius(), 1e-100);

        // try setting input to some other effect
        Effect blur = new BoxBlur();
        effect.setBumpInput(blur);
        assertEquals(blur, effect.getBumpInput());

        // try setting input to null
        effect.setBumpInput(null);
        assertNull(effect.getBumpInput());
    }

    @Test
    public void testContentInput() {
        // default is null
        assertNull(effect.getContentInput());
        // try setting input to some other effect
        Effect blur = new BoxBlur();
        effect.setContentInput(blur);
        assertEquals(blur, effect.getContentInput());

        // try setting input to null
        effect.setContentInput(null);
        assertNull(effect.getContentInput());
    }

    @Test
    public void testSetLight() {
        // try setting correct value
        Light l = new Light.Point();
        effect.setLight(l);
        assertEquals(l, effect.getLight());
        pulse();
        assertEquals(LightShim.getPeer(l), ((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getLight());
    }

    @Test
    public void testDefaultLight() {
        // default value should be distant light
        Light l = effect.getLight();
        assertNotNull(l);
        assertTrue(l instanceof Light.Distant);
        assertEquals(l, effect.lightProperty().get());
        pulse();
        assertEquals(LightShim.getPeer(l), ((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getLight());
    }

    @Test
    public void testNullLight() {
        // nullvalue should default to Distant light
        effect.setLight(null);
        Light l = effect.getLight();
        assertNull(l);
        assertNull(effect.lightProperty().get());
        pulse();
        assertNotNull(((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getLight());
    }

    @Test
    public void testDefaultLightNotChangedByOtherLightingEffect() {
        // default value should be distant light
        Light l = effect.getLight();
        assertNotNull(l);
        assertTrue(l instanceof Light.Distant);
        assertEquals(l, effect.lightProperty().get());

        Lighting lighting = new Lighting();
        Light l2 = lighting.getLight();
        assertNotNull(l2);
        assertTrue(l2 instanceof Light.Distant);
        assertEquals(l2, lighting.lightProperty().get());

        l.setColor(Color.AQUA);

        assertEquals(Color.AQUA, l.getColor());
        assertEquals(Color.WHITE, l2.getColor());
    }

    @Test
    public void testDefaultLightNotChangedByThisLightingEffect() {
        // default value should be distant light
        Light l = effect.getLight();
        l.setColor(Color.BEIGE);

        effect.setLight(null);
        // null light should default to Distant Light with WHITE color
        assertNull(effect.getLight());
        pulse();
        Color4f c = ((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getLight().getColor();
        assertEquals(1f, c.getRed(), 1e-5);
        assertEquals(1f, c.getGreen(), 1e-5);
        assertEquals(1f, c.getBlue(), 1e-5);
        assertEquals(1f, c.getAlpha(), 1e-5);
    }

    @Test
    public void testChangeLight() {
        // try setting correct value
        Light.Point l = new Light.Point();
        effect.setLight(l);
        assertEquals(l, effect.getLight());
        pulse();
        assertEquals(LightShim.getPeer(l), ((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getLight());
        l.setX(100);
        pulse();
        assertEquals(100f, ((com.sun.scenario.effect.light.PointLight)
                LightShim.getPeer(l)).getX(), 1e-100);
    }

    @Test
    public void testCycles() {
        // try setting itself as content input
        try {
            effect.setContentInput(effect);
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException e) {
            assertEquals(null, effect.getContentInput());
        }

        // try setting itself as bump input
        try {
            effect.setBumpInput(effect);
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException e) {
            Effect efct = effect.getBumpInput();
            assertTrue(efct instanceof Shadow);
        }

        // test following cycle
        // Lighting <- BoxBlur <- Lighting
        BoxBlur blur = new BoxBlur();
        effect.setBumpInput(blur);
        effect.setContentInput(blur);
        try {
            blur.setInput(effect);
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException e) {
            assertEquals(null, blur.getInput());
        }

        // test following cycle
        // BoxBlur <- Lighting <- BoxBlur
        effect.setBumpInput(null);
        effect.setContentInput(null);
        blur.setInput(effect);
        try {
            effect.setBumpInput(blur);
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException e) {}

        try {
            effect.setContentInput(blur);
            fail("IllegalArgument should have been thrown.");
        } catch (IllegalArgumentException e) {}

        assertEquals(null, effect.getContentInput());
        assertEquals(null, effect.getBumpInput());
    }

    int countIllegalArgumentException = 0;
    @Test
    public void testCyclesForBoundInput() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof IllegalArgumentException) {
                countIllegalArgumentException++;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });
        ObjectProperty vContentInput = new SimpleObjectProperty();
        effect.contentInputProperty().bind(vContentInput);
        // try setting itself as content input
        vContentInput.set(effect);
        assertEquals(null, effect.getContentInput());
        vContentInput.set(null);

        effect.contentInputProperty().bind(vContentInput);

        // try setting itself as bump input
        ObjectProperty vBumpInput = new SimpleObjectProperty();
        effect.bumpInputProperty().bind(vBumpInput);
        vBumpInput.set(effect);
        Effect efct = effect.getBumpInput();
        assertNull(efct);
        vBumpInput.set(null);
        effect.bumpInputProperty().bind(vBumpInput);

        // test following cycle
        // Lighting <- BoxBlur <- Lighting
        BoxBlur blur = new BoxBlur();
        ObjectProperty vBlur = new SimpleObjectProperty();
        blur.inputProperty().bind(vBlur);
        vBumpInput.set(blur);
        vContentInput.set(blur);
        vBlur.set(effect);
        assertEquals(null, blur.getInput());
        vBlur.set(null);
        blur.inputProperty().bind(vBlur);

        // test following cycle
        // BoxBlur <- Lighting <- BoxBlur
        vBumpInput.set(null);
        vContentInput.set(null);
        vBlur.set(effect);
        vBumpInput.set(blur);
        assertEquals(null, effect.getContentInput());
        vBumpInput.set(null);
        effect.bumpInputProperty().bind(vBumpInput);

        vContentInput.set(blur);
        assertEquals(null, effect.getContentInput());
        vContentInput.set(null);
        effect.contentInputProperty().bind(vContentInput);

        assertEquals(null, effect.getContentInput());
        assertEquals(null, effect.getBumpInput());

        assertEquals(5, countIllegalArgumentException, "Cycle in effect chain detected, exception should occur 5 times.");
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

    @Test
    public void testDiffuseConstantSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.Lighting", "diffuseConstant",
                "com.sun.scenario.effect.PhongLighting", "diffuseConstant", 1.5);
    }

    @Test
    public void testSpecularConstantSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.Lighting", "specularConstant",
                "com.sun.scenario.effect.PhongLighting", "specularConstant", 1.5);
    }


    @Test
    public void testSpecularExponentSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.Lighting", "specularExponent",
                "com.sun.scenario.effect.PhongLighting", "specularExponent", 10);
    }

    @Test
    public void testSurfaceScaleSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.Lighting", "surfaceScale",
                "com.sun.scenario.effect.PhongLighting", "surfaceScale", 10);
    }

    @Test
    public void testLightSynced() throws Exception {
        Light l = new Light.Point();
        checkObjectPropertySynced(
                "javafx.scene.effect.Lighting", "light",
                "com.sun.scenario.effect.PhongLighting", "light",
                l, LightShim.getPeer(l),
                null);
    }

    @Test
    public void testBumpInputSynced() throws Exception {
        BoxBlur blur = new BoxBlur();
        checkEffectPropertySynced(
                "javafx.scene.effect.Lighting", "bumpInput",
                "com.sun.scenario.effect.PhongLighting", "bumpInput",
                blur, (com.sun.scenario.effect.BoxBlur)
                EffectShim.getPeer(blur));
    }

    @Test
    public void testContentInputSynced() throws Exception {
        BoxBlur blur = new BoxBlur();
        checkEffectPropertySynced(
                "javafx.scene.effect.Lighting", "contentInput",
                "com.sun.scenario.effect.PhongLighting", "contentInput",
                blur, (com.sun.scenario.effect.BoxBlur)
                EffectShim.getPeer(blur));
    }

    @Test
    public void testBounds() {
        assertEquals(box(0, 0, 100, 100), n.getBoundsInLocal());
    }

    @Test
    public void testBoundsWidthContentInput() {
        assertEquals(box(0, 0, 100, 100), n.getBoundsInLocal());
        BoxBlur blur = new BoxBlur();
        effect.setContentInput(blur);
        assertEquals(box(-2, -2, 104, 104), n.getBoundsInLocal());
    }

    @Test
    public void testBoundsWidthBumpInput() {
        assertEquals(box(0, 0, 100, 100), n.getBoundsInLocal());
        BoxBlur blur = new BoxBlur();
        effect.setBumpInput(blur);
        assertEquals(box(0, 0, 100, 100), n.getBoundsInLocal());
    }

    @Test
    public void testBoundsWidthBumpAndContentInput() {
        assertEquals(box(0, 0, 100, 100), n.getBoundsInLocal());
        BoxBlur blur = new BoxBlur();
        effect.setContentInput(blur);
        effect.setBumpInput(blur);
        assertEquals(box(-2, -2, 104, 104), n.getBoundsInLocal());
    }

    @Test
    public void testCreateWithParams() {
        Light l = new Light.Point();
        effect = new Lighting(l);
        setupTest(effect);
        effect.setLight(l);
        assertEquals(l, effect.getLight());
        pulse();
        assertEquals(LightShim.getPeer(l), ((com.sun.scenario.effect.PhongLighting)
                EffectShim.getPeer(effect)).getLight());
    }
}
