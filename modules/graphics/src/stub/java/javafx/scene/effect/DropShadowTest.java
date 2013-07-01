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
import static org.junit.Assert.assertNull;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.paint.Color;

import org.junit.Before;
import org.junit.Test;

import com.sun.scenario.effect.AbstractShadow.ShadowMode;
import com.sun.scenario.effect.Color4f;

public class DropShadowTest extends EffectsTestBase {
    private DropShadow effect;

    @Before
    public void setUp() {
        effect = new DropShadow();
        setupTest(effect);
    }

    @Test
    public void testSetBlurType() {
        // try setting correct value
        effect.setBlurType(BlurType.ONE_PASS_BOX);
        assertEquals(BlurType.ONE_PASS_BOX, effect.getBlurType());
        pulse();
        assertEquals(ShadowMode.ONE_PASS_BOX, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getShadowMode());

        effect.setBlurType(BlurType.TWO_PASS_BOX);
        assertEquals(BlurType.TWO_PASS_BOX, effect.getBlurType());
        pulse();
        assertEquals(ShadowMode.TWO_PASS_BOX, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getShadowMode());

        effect.setBlurType(BlurType.THREE_PASS_BOX);
        assertEquals(BlurType.THREE_PASS_BOX, effect.getBlurType());
        pulse();
        assertEquals(ShadowMode.THREE_PASS_BOX, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getShadowMode());

        effect.setBlurType(BlurType.GAUSSIAN);
        assertEquals(BlurType.GAUSSIAN, effect.getBlurType());
        pulse();
        assertEquals(ShadowMode.GAUSSIAN, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getShadowMode());
    }

    @Test
    public void testNullBlurType() {
        // null should default to BlurType.THREE_PASS_BOX in render tree
        effect.setBlurType(null);
        assertEquals(null, effect.getBlurType());
        assertEquals(null, effect.blurTypeProperty().get());
        pulse();
        assertEquals(ShadowMode.THREE_PASS_BOX, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getShadowMode());
    }

    @Test
    public void testDefaultBlurType() {
        // default value should be BlurType.THREE_PASS_BOX
        assertEquals(BlurType.THREE_PASS_BOX, effect.getBlurType());
        assertEquals(BlurType.THREE_PASS_BOX, effect.blurTypeProperty().get());
        pulse();
        assertEquals(ShadowMode.THREE_PASS_BOX, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getShadowMode());
    }


    @Test
    public void testSetColor() {
        // try setting correct value
        effect.setColor(Color.RED);
        assertEquals(Color.RED, effect.getColor());
        pulse();
        Color color = Color.RED;
        Color4f red = new Color4f((float) color.getRed(), (float) color.getGreen(),
                (float) color.getBlue(), (float) color.getOpacity());
        Color4f actual = ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getColor();
        assertColor4fEquals(red, actual);
    }

    @Test
    public void testDefaultColor() {
        // default value should be Color.BLACK
        assertEquals(Color.BLACK, effect.getColor());
        assertEquals(Color.BLACK, effect.colorProperty().get());
        pulse();
        Color color = Color.BLACK;
        Color4f black = new Color4f((float) color.getRed(), (float) color.getGreen(),
                (float) color.getBlue(), (float) color.getOpacity());
        Color4f actual = ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getColor();
        assertColor4fEquals(black, actual);
    }

    @Test
    public void testNullColor() {
        // null color should default to Color.BLACK in render tree
        effect.setColor(null);
        assertNull(effect.getColor());
        assertNull(effect.colorProperty().get());
        pulse();
        Color color = Color.BLACK;
        Color4f black = new Color4f((float) color.getRed(), (float) color.getGreen(),
                (float) color.getBlue(), (float) color.getOpacity());
        Color4f actual = ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getColor();
        assertColor4fEquals(black, actual);
    }

    @Test
    public void testSetWidth() {
        // try setting correct value
        effect.setWidth(9.0f);
        assertEquals(9.0f, effect.getWidth(), 1e-100);
        pulse();
        assertEquals(9.0f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianWidth(), 1e-100);
        // test that radius changed appropriately
        // radius = (((width + height)/2) -1) /2
        assertEquals(7.0f, effect.getRadius(), 1e-100);
    }

    @Test
    public void testDefaultWidth() {
        // default value should be 21
        assertEquals(21f, effect.getWidth(), 1e-100);
        assertEquals(21f, effect.widthProperty().get(), 1e-100);
        pulse();
        assertEquals(21f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianWidth(), 1e-100);
    }

    @Test
    public void testMinWidth() {
        // 0 should be ok
        effect.setWidth(0);
        // try setting value smaller than minimal
        effect.setWidth(-0.1f);
        assertEquals(-0.1f, effect.getWidth(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianWidth(), 1e-100);
    }

    @Test
    public void testMaxWidth() {
        // 255 should be ok
        effect.setWidth(255);
        // try setting value greater than maximal
        effect.setWidth(255.1f);
        assertEquals(255.1f, effect.getWidth(), 1e-100);
        pulse();
        assertEquals(255f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianWidth(), 1e-100);
    }

    @Test
    public void testSetHeight() {
        // try setting correct value
        effect.setHeight(9.0f);
        assertEquals(9.0f, effect.getHeight(), 1e-100);
        pulse();
        assertEquals(9.0f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianHeight(), 1e-100);
        // test that radius changed appropriately
        // radius = (((width + height)/2) -1) /2
        assertEquals(7.0f, effect.getRadius(), 1e-100);
    }

    @Test
    public void testDefaultHeight() {
        // default value should be 21
        assertEquals(21f, effect.getHeight(), 1e-100);
        assertEquals(21f, effect.heightProperty().get(), 1e-100);
        pulse();
        assertEquals(21f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianHeight(), 1e-100);
    }

    @Test
    public void testMinHeight() {
        // 0 should be ok
        effect.setHeight(0);
        // try setting value smaller than minimal
        effect.setHeight(-0.1f);
        assertEquals(-0.1f, effect.getHeight(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianHeight(), 1e-100);
    }

    @Test
    public void testMaxHeight() {
        // 255 should be ok
        effect.setHeight(1);
        // try setting value greater than maximal
        effect.setHeight(255.1f);
        assertEquals(255.1f, effect.getHeight(), 1e-100);
        pulse();
        assertEquals(255f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianHeight(), 1e-100);
    }

    @Test
    public void testSetRadius() {
        // try setting correct value
        effect.setRadius(4.0f);
        assertEquals(4.0f, effect.getRadius(), 1e-100);
        pulse();
        assertEquals(4.0f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getRadius(), 1e-100);
        // test that width and height changed appropriately
        assertEquals(9.0f, effect.getHeight(), 1e-100);
        assertEquals(9.0f, effect.getWidth(), 1e-100);
    }

    @Test
    public void testDefaultRadius() {
        // default value should be 10
        assertEquals(10f, effect.getRadius(), 1e-100);
        assertEquals(10f, effect.radiusProperty().get(), 1e-100);
        pulse();
        assertEquals(10f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getRadius(), 1e-100);
    }

    @Test
    public void testMinRadius() {
        // 0 should be ok
        effect.setRadius(0);
        // try setting value smaller than minimal
        effect.setRadius(-0.1f);
        assertEquals(-0.1f, effect.getRadius(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getRadius(), 1e-100);
    }

    @Test
    public void testMaxRadius() {
        // 127 should be ok
        effect.setRadius(127);
        // try setting value greater than maximal
        effect.setRadius(127.1f);
        assertEquals(127.1f, effect.getRadius(), 1e-100);
        pulse();
        assertEquals(127f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getRadius(), 1e-100);
    }

    @Test
    public void testRadiusNotNegative() {
        effect.setHeight(0.1f);
        effect.setWidth(0.1f);
        // radius should be 0, not negative
        assertEquals(0f, effect.getRadius(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getRadius(), 1e-100);

        effect.setWidth(0.2f);
        effect.setHeight(0.2f);
        // radius should be 0, not negative
        assertEquals(0f, effect.getRadius(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getRadius(), 1e-100);
    }

    @Test
    public void testSetSpread() {
        // try setting correct value
        effect.setSpread(1.0f);
        assertEquals(1.0f, effect.getSpread(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getSpread(), 1e-100);
    }

    @Test
    public void testDefaultSpread() {
        // default value should be 0
        assertEquals(0f, effect.getSpread(), 1e-100);
        assertEquals(0f, effect.spreadProperty().get(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getSpread(), 1e-100);
    }

    @Test
    public void testMinSpread() {
        // 0 should be ok
        effect.setSpread(0);
        // try setting value smaller than minimal
        effect.setSpread(-0.1f);
        assertEquals(-0.1f, effect.getSpread(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getSpread(), 1e-100);
    }

    @Test
    public void testMaxSpread() {
        // 1 should be ok
        effect.setSpread(1);
        // try setting value greater than maximal
        effect.setSpread(1.1f);
        assertEquals(1.1f, effect.getSpread(), 1e-100);
        pulse();
        assertEquals(1f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getSpread(), 1e-100);
    }

    @Test
    public void testSetOffsetX() {
        // try setting correct value
        effect.setOffsetX(1.0f);
        assertEquals(1.0f, effect.getOffsetX(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getOffsetX(), 1e-100);
    }

    @Test
    public void testDefaultOffsetX() {
        // default value should be 0
        assertEquals(0f, effect.getOffsetX(), 1e-100);
        assertEquals(0f, effect.offsetXProperty().get(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getOffsetX(), 1e-100);
    }

    @Test
    public void testSetOffsetY() {
        // try setting correct value
        effect.setOffsetY(1.0f);
        assertEquals(1.0f, effect.getOffsetY(), 1e-100);
        pulse();
        assertEquals(1.0f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getOffsetY(), 1e-100);
    }

    @Test
    public void testDefaultOffsetY() {
        // default value should be 0
        assertEquals(0f, effect.getOffsetY(), 1e-100);
        assertEquals(0f, effect.offsetYProperty().get(), 1e-100);
        pulse();
        assertEquals(0f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getOffsetY(), 1e-100);
    }

    @Test
    public void testHeightSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.DropShadow", "height",
                "com.sun.scenario.effect.DropShadow", "gaussianHeight", 9);
        assertEquals(7, effect.getRadius(), 1e-100);
    }

    @Test
    public void testWidthSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.DropShadow", "width",
                "com.sun.scenario.effect.DropShadow", "gaussianWidth", 9);
        assertEquals(7, effect.getRadius(), 1e-100);
    }

    @Test
    public void testBlurTypeSynced() throws Exception {
        checkObjectPropertySynced(
                "javafx.scene.effect.DropShadow", "blurType",
                "com.sun.scenario.effect.DropShadow", "shadowMode",
                BlurType.TWO_PASS_BOX, ShadowMode.TWO_PASS_BOX,
                BlurType.GAUSSIAN);
    }

    @Test
    public void testOffsetXSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.DropShadow", "offsetX",
                "com.sun.scenario.effect.DropShadow", "offsetX", 10);
    }

    @Test
    public void testOffsetYSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.DropShadow", "offsetY",
                "com.sun.scenario.effect.DropShadow", "offsetY", 10);
    }

    @Test
    public void testRadiusSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.DropShadow", "radius",
                "com.sun.scenario.effect.DropShadow", "radius", 4);
        assertEquals(9, effect.getHeight(), 1e-100);
        assertEquals(9, effect.getWidth(), 1e-100);
    }

    @Test
    public void testSpreadSynced() throws Exception {
        checkDoublePropertySynced(
                "javafx.scene.effect.DropShadow", "spread",
                "com.sun.scenario.effect.DropShadow", "spread", 0.3);
    }

    @Test
    public void testInputSynced() throws Exception {
        BoxBlur blur = new BoxBlur();
        checkEffectPropertySynced(
                "javafx.scene.effect.DropShadow", "input",
                "com.sun.scenario.effect.DropShadow", "contentInput",
                blur, (com.sun.scenario.effect.BoxBlur)blur.impl_getImpl());
    }

    @Test
    public void testColorSynced() throws Exception {
        Color color = Color.RED;
        Color4f red = new Color4f((float) color.getRed(), (float) color.getGreen(),
                (float) color.getBlue(), (float) color.getOpacity());
        Color4f result = (Color4f) getObjectPropertySynced(
                "javafx.scene.effect.DropShadow", "color",
                "com.sun.scenario.effect.DropShadow", "color",
                Color.RED);
        assertColor4fEquals(red, result);
    }

    // test whether width/height are changing correctly if radius is bound
    // and one of them is changed
    @Test
    public void testRadiusBound() throws Exception {
        DoubleProperty boundRadius = new SimpleDoubleProperty();

        effect.radiusProperty().bind(boundRadius);

        boundRadius.set(4);
        effect.setWidth(9);

        assertEquals(9, effect.getHeight(), 1e-100);
        pulse();
        assertEquals(9, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianHeight(), 1e-100);
        assertEquals(9, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianWidth(), 1e-100);
        assertEquals(4, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianRadius(), 1e-100);

        effect.setHeight(3);
        assertEquals(15, effect.getWidth(), 1e-100);
        pulse();
        assertEquals(15, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianWidth(), 1e-100);
    }

    // test whether width/radius are changing correctly if height is bound
    // and one of them is changed    
    @Test
    public void testHeightBound() throws Exception {
        DoubleProperty boundHeight = new SimpleDoubleProperty();

        effect.heightProperty().bind(boundHeight);

        boundHeight.set(9);
        effect.setRadius(4);

        assertEquals(9, effect.getWidth(), 1e-100);
        pulse();
        assertEquals(9, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianHeight(), 1e-100);
        assertEquals(9, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianWidth(), 1e-100);
        assertEquals(4, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianRadius(), 1e-100);

        effect.setWidth(3);
        assertEquals(2.5, effect.getRadius(), 1e-100);
    }

    // test whether height/radius are changing correctly if width is bound
    // and one of them is changed
    @Test
    public void testWidthBound() throws Exception {
        DoubleProperty boundWidth = new SimpleDoubleProperty();

        effect.widthProperty().bind(boundWidth);

        boundWidth.set(9);
        effect.setRadius(4);

        assertEquals(9, effect.getHeight(), 1e-100);
        pulse();
        assertEquals(9, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianHeight(), 1e-100);
        assertEquals(9, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianWidth(), 1e-100);
        assertEquals(4, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianRadius(), 1e-100);

        effect.setHeight(3);
        assertEquals(2.5, effect.getRadius(), 1e-100);
    }

    // Test for special cases when 2 of width, height, radius are bound
    @Test
    public void testRadiusWidthBound() throws Exception {
        DoubleProperty boundRadius = new SimpleDoubleProperty();
        DoubleProperty boundWidth = new SimpleDoubleProperty();

        effect.radiusProperty().bind(boundRadius);
        effect.widthProperty().bind(boundWidth);

        boundRadius.set(4);
        boundWidth.set(9);

        assertEquals(9, effect.getHeight(), 1e-100);
        pulse();
        assertEquals(9, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianHeight(), 1e-100);
        assertEquals(9, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianWidth(), 1e-100);
        assertEquals(4, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianRadius(), 1e-100);

        // set radius once again to be sure that the order of calls is not
        // important
        boundRadius.set(7);
        assertEquals(21, effect.getHeight(), 1e-100);
        pulse();
        assertEquals(21, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianHeight(), 1e-100);
    }

    @Test
    public void testRadiusHeightBound() throws Exception {
        DoubleProperty boundRadius = new SimpleDoubleProperty();
        DoubleProperty boundHeight = new SimpleDoubleProperty();

        effect.radiusProperty().bind(boundRadius);
        effect.heightProperty().bind(boundHeight);

        boundRadius.set(4);
        boundHeight.set(9);

        assertEquals(9, effect.getWidth(), 1e-100);
        pulse();
        assertEquals(9, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianHeight(), 1e-100);
        assertEquals(9, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianWidth(), 1e-100);
        assertEquals(4, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianRadius(), 1e-100);

        boundRadius.set(7);
        assertEquals(21, effect.getWidth(), 1e-100);
        pulse();
        assertEquals(21, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianWidth(), 1e-100);
    }

    @Test
    public void testWidthHeightBound() throws Exception {
        DoubleProperty boundWidth = new SimpleDoubleProperty();
        DoubleProperty boundHeight = new SimpleDoubleProperty();

        effect.widthProperty().bind(boundWidth);
        effect.heightProperty().bind(boundHeight);

        boundHeight.set(9);
        boundWidth.set(9);

        assertEquals(4, effect.getRadius(), 1e-100);
        pulse();
        assertEquals(9, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianHeight(), 1e-100);
        assertEquals(9, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianWidth(), 1e-100);
        assertEquals(4, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianRadius(), 1e-100);

        boundHeight.set(21);
        assertEquals(7, effect.getRadius(), 1e-100);
        pulse();
        assertEquals(7, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianRadius(), 1e-100);
    }

    // all radius, width and height are bound, radius is ignored in this case
    // just test that it doesn't end in infinite loop
    @Test
    public void testWidthHeightRadiusBound() {
        DoubleProperty boundWidth = new SimpleDoubleProperty();
        DoubleProperty boundHeight = new SimpleDoubleProperty();
        DoubleProperty boundRadius = new SimpleDoubleProperty();

        effect.widthProperty().bind(boundWidth);
        effect.heightProperty().bind(boundHeight);
        effect.radiusProperty().bind(boundRadius);

        boundHeight.set(9);
        boundWidth.set(9);
        boundRadius.set(5);

        pulse();
        assertEquals(9, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianHeight(), 1e-100);
        assertEquals(9, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianWidth(), 1e-100);
        assertEquals(4, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianRadius(), 1e-100);
    }
    
    @Test
    public void testCreateWithParams() {
        effect = new DropShadow(4, Color.RED);
        setupTest(effect);
        assertEquals(4, effect.getRadius(), 1e-100);
        assertEquals(Color.RED, effect.getColor());
        // test that width and height changed appropriately
        assertEquals(9, effect.getHeight(), 1e-100);
        assertEquals(9, effect.getWidth(), 1e-100);
        pulse();
        assertEquals(4f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianRadius(), 1e-100);
        Color color = Color.RED;
        Color4f red = new Color4f((float) color.getRed(), (float) color.getGreen(),
                (float) color.getBlue(), (float) color.getOpacity());
        Color4f actual = ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getColor();
        assertColor4fEquals(red, actual);
    }

    @Test
    public void testCreateWithParams4() {
        effect = new DropShadow(4, 1, 2, Color.RED);
        setupTest(effect);
        assertEquals(1, effect.getOffsetX(), 1e-100);
        assertEquals(2, effect.getOffsetY(), 1e-100);
        assertEquals(4, effect.getRadius(), 1e-100);
        assertEquals(Color.RED, effect.getColor());
        // test that width and height changed appropriately
        assertEquals(9, effect.getHeight(), 1e-100);
        assertEquals(9, effect.getWidth(), 1e-100);
        pulse();
        assertEquals(4f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianRadius(), 1e-100);
        assertEquals(1f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getOffsetX(), 1e-100);
        assertEquals(2f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getOffsetY(), 1e-100);
        Color color = Color.RED;
        Color4f red = new Color4f((float) color.getRed(), (float) color.getGreen(),
                (float) color.getBlue(), (float) color.getOpacity());
        Color4f actual = ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getColor();
        assertColor4fEquals(red, actual);
    }
    
    @Test
    public void testCreateWithParams6() {
        effect = new DropShadow(BlurType.GAUSSIAN, Color.RED, 4, 0.5, 1, 2);
        setupTest(effect);
        assertEquals(1, effect.getOffsetX(), 1e-100);
        assertEquals(2, effect.getOffsetY(), 1e-100);
        assertEquals(4, effect.getRadius(), 1e-100);
        assertEquals(0.5, effect.getSpread(), 1e-100);
        assertEquals(Color.RED, effect.getColor());
        assertEquals(BlurType.GAUSSIAN, effect.getBlurType());
        // test that width and height changed appropriately
        assertEquals(9, effect.getHeight(), 1e-100);
        assertEquals(9, effect.getWidth(), 1e-100);
        pulse();
        assertEquals(4f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianRadius(), 1e-100);
        assertEquals(0.5f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getSpread(), 1e-100);
        assertEquals(1f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getOffsetX(), 1e-100);
        assertEquals(2f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getOffsetY(), 1e-100);
        assertEquals(ShadowMode.GAUSSIAN, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getShadowMode());
        Color color = Color.RED;
        Color4f red = new Color4f((float) color.getRed(), (float) color.getGreen(),
                (float) color.getBlue(), (float) color.getOpacity());
        Color4f actual = ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getColor();
        assertColor4fEquals(red, actual);
    }

    @Test
    public void testCreateWithDefaultParams() {
        effect = new DropShadow(10, Color.BLACK);
        setupTest(effect);
        assertEquals(10, effect.getRadius(), 1e-100);
        assertEquals(Color.BLACK, effect.getColor());
        // test that width and height changed appropriately
        assertEquals(21, effect.getHeight(), 1e-100);
        assertEquals(21, effect.getWidth(), 1e-100);
        pulse();
        assertEquals(10f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianRadius(), 1e-100);
        Color color = Color.BLACK;
        Color4f red = new Color4f((float) color.getRed(), (float) color.getGreen(),
                (float) color.getBlue(), (float) color.getOpacity());
        Color4f actual = ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getColor();
        assertColor4fEquals(red, actual);
    }

    @Test
    public void testCreateWithDefaultParams4() {
        effect = new DropShadow(10, 0, 0, Color.BLACK);
        setupTest(effect);
        assertEquals(0, effect.getOffsetX(), 1e-100);
        assertEquals(0, effect.getOffsetY(), 1e-100);
        assertEquals(10, effect.getRadius(), 1e-100);
        assertEquals(Color.BLACK, effect.getColor());
        // test that width and height changed appropriately
        assertEquals(21, effect.getHeight(), 1e-100);
        assertEquals(21, effect.getWidth(), 1e-100);
        pulse();
        assertEquals(10f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianRadius(), 1e-100);
        assertEquals(0f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getOffsetX(), 1e-100);
        assertEquals(0f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getOffsetY(), 1e-100);
        Color color = Color.BLACK;
        Color4f red = new Color4f((float) color.getRed(), (float) color.getGreen(),
                (float) color.getBlue(), (float) color.getOpacity());
        Color4f actual = ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getColor();
        assertColor4fEquals(red, actual);
    }

    @Test
    public void testCreateWithDefaultParams6() {
        effect = new DropShadow(BlurType.THREE_PASS_BOX, Color.BLACK, 10, 0, 0, 0);
        setupTest(effect);
        assertEquals(0, effect.getOffsetX(), 1e-100);
        assertEquals(0, effect.getOffsetY(), 1e-100);
        assertEquals(10, effect.getRadius(), 1e-100);
        assertEquals(0, effect.getSpread(), 1e-100);
        assertEquals(Color.BLACK, effect.getColor());
        assertEquals(BlurType.THREE_PASS_BOX, effect.getBlurType());
        // test that width and height changed appropriately
        assertEquals(21, effect.getHeight(), 1e-100);
        assertEquals(21, effect.getWidth(), 1e-100);
        pulse();
        assertEquals(10f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getGaussianRadius(), 1e-100);
        assertEquals(0f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getSpread(), 1e-100);
        assertEquals(0f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getOffsetX(), 1e-100);
        assertEquals(0f, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getOffsetY(), 1e-100);
        assertEquals(ShadowMode.THREE_PASS_BOX, ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getShadowMode());
        Color color = Color.BLACK;
        Color4f red = new Color4f((float) color.getRed(), (float) color.getGreen(),
                (float) color.getBlue(), (float) color.getOpacity());
        Color4f actual = ((com.sun.scenario.effect.DropShadow) effect.impl_getImpl()).getColor();
        assertColor4fEquals(red, actual);
    }
}
