/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.sg.prism;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.Graphics;
import com.sun.prism.paint.Color;
import com.sun.scenario.effect.Blend;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 */
public class NGNodeTest extends NGTestBase {
    NGNodeMock n;

    @Before
    public void setup() {
        n = new NGNodeMock();
    }

    /**************************************************************************
     *                                                                        *
     * Various tests for hasOpaqueRegion                                      *
     *                                                                        *
     *************************************************************************/

    @Test
    public void hasOpaqueRegionIsFalseIfOpacityIsLessThanOne() {
        n.setOpacity(1);
        assertTrue(n.hasOpaqueRegion());

        n.setOpacity(.5f);
        assertFalse(n.hasOpaqueRegion());

        n.setOpacity(0);
        assertFalse(n.hasOpaqueRegion());
    }

    @Test
    public void hasOpaqueRegionIsFalseIfEffectIsNotNullAndEffect_reducesOpaquePixels_returnsFalse() {
        n.setEffect(new TransparentEffect());
        assertFalse(n.hasOpaqueRegion());
        n.setEffect(null);
        assertTrue(n.hasOpaqueRegion());
    }

    @Test
    public void hasOpaqueRegionIsTrueIfEffectIsNotNullAndEffect_reducesOpaquePixels_returnsTrue() {
        n.setEffect(new OpaqueEffect());
        assertTrue(n.hasOpaqueRegion());
    }

    @Test
    public void hasOpaqueRegionIsTrueIfClipIsNullOrHappy() {
        n.setClipNode(null);
        assertTrue(n.hasOpaqueRegion());

        NGRectangle r = new NGRectangle();
        r.updateRectangle(0, 0, 10, 10, 0, 0);
        r.setFillPaint(Color.BLACK);
        n.setClipNode(r);

        assertTrue(n.hasOpaqueRegion());
    }

    @Test
    public void hasOpaqueRegionIsFalseIfClipDoesNotSupportOpaqueRegions() {
        n.setClipNode(new NGPath());
        assertFalse(n.hasOpaqueRegion());
    }

    @Test
    public void hasOpaqueRegionIsFalseIfClipDoesNotHaveAnOpaqueRegion() {
        NGRectangle r = new NGRectangle();
        n.setClipNode(r);
        assertFalse(n.hasOpaqueRegion());
    }

    @Test
    public void hasOpaqueRegionIsFalseIfBlendModeIsNotNullOrSRC_OVER() {
        for (Blend.Mode mode : Blend.Mode.values()) {
            // Set blend mode
            n.setNodeBlendMode(mode);
            if (mode == Blend.Mode.SRC_OVER) {
                assertTrue(n.hasOpaqueRegion());
            } else {
                assertFalse(n.hasOpaqueRegion());
            }
        }
        n.setNodeBlendMode(null);
        assertTrue(n.hasOpaqueRegion());
    }

    /**************************************************************************
     *                                                                        *
     * Various tests for opaque region caching and invalidation               *
     *                                                                        *
     *************************************************************************/

    /**
     * The same opaqueRegion instance should be reused every time, unless
     * we transition to a state where opaqueRegion is not true, in which case
     * opaqueRegion will be null.
     */
    @Test
    public void opaqueRegionCached() {
        RectBounds or = n.getOpaqueRegion();
        assertNotNull(or);

        // Simulating changing state that would result in a different opaque region
        // but should be the same instance
        n.changeOpaqueRegion(10, 10, 100, 100);
        assertSame(or, n.getOpaqueRegion());

        // Changing to something that will cause the hasOpaqueRegions to return false
        n.setEffect(new TransparentEffect());
        assertNull(n.getOpaqueRegion());

        // Change back to something that will work and get a different instance
        n.setEffect(null);
        assertNotSame(or, n.getOpaqueRegion());
    }

    /**
     * Test that as long as the opaque region is valid, that we don't
     * try to compute a new one
     */
    @Test
    public void opaqueRegionCached2() {
        // Cause it to be cached
        n.getOpaqueRegion();
        n.opaqueRegionRecomputed = false;

        // Just calling it again should do nothing
        n.getOpaqueRegion();
        assertFalse(n.opaqueRegionRecomputed);
    }

    /**
     * If the transform is changed, we need to recompute the opaque region. But
     * if the transform is not changed, then we shouldn't. Since NGNode doesn't keep
     * a reference to the last transform object it was passed and doesn't perform
     * any kind of check, we're going to invalidate the opaque region every time
     * it is called. If we change that behavior later, this test will need to be
     * updated accordingly.
     */
    @Test
    public void opaqueRegionRecomputedWhenTransformChanges() {
        n.getOpaqueRegion();

        n.opaqueRegionRecomputed = false;
        n.setTransformMatrix(BaseTransform.getTranslateInstance(1, 1));
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);

        n.opaqueRegionRecomputed = false;
        n.setTransformMatrix(BaseTransform.getTranslateInstance(1, 1));
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);

        n.opaqueRegionRecomputed = false;
        n.setTransformMatrix(BaseTransform.IDENTITY_TRANSFORM);
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);

        n.opaqueRegionRecomputed = false;
        n.setTransformMatrix(BaseTransform.IDENTITY_TRANSFORM);
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);
    }

    /**
     * If we change references to the clip node, then we must recompute the
     * opaque region, regardless of whether we set the reference to a
     * different equivalent guy (since we don't check for equals and without
     * doing deep equals it would fail anyway)
     */
    @Test
    public void opaqueRegionRecomputedWhenClipNodeReferenceChanges() {
        n.getOpaqueRegion();

        // I'm setting a clip, and even though the clip isn't opaque,
        // I will have to ask the hasOpaqueRegions question again
        n.opaqueRegionRecomputed = false;
        NGRectangle clip = new NGRectangle();
        n.setClipNode(clip);
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);

        // Set the same clip instance again, which should have no effect
        n.opaqueRegionRecomputed = false;
        n.setClipNode(clip);
        n.getOpaqueRegion();
        assertFalse(n.opaqueRegionRecomputed);

        // Try out a nice clip, this time with a nice opaque region.
        n.opaqueRegionRecomputed = false;
        clip = new NGRectangle();
        clip.updateRectangle(0, 0, 10, 10, 0, 0);
        clip.setFillPaint(Color.BLACK);
        n.setClipNode(clip);
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);

        // Try out an equivalent, but different instance, clip.
        n.opaqueRegionRecomputed = false;
        clip = new NGRectangle();
        clip.updateRectangle(0, 0, 10, 10, 0, 0);
        clip.setFillPaint(Color.BLACK);
        n.setClipNode(clip);
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);

        // Set it to null
        n.opaqueRegionRecomputed = false;
        n.setClipNode(null);
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);

        // Set it to null again, which should have no effect
        n.opaqueRegionRecomputed = false;
        n.setClipNode(null);
        n.getOpaqueRegion();
        assertFalse(n.opaqueRegionRecomputed);
    }

    /**************************************************************************
     *                                                                        *
     * Various tests for opaque region caching and invalidation               *
     *                                                                        *
     *************************************************************************/

    /**
     * If one of the inputs to the "hasOpaqueRegions" changes in such a way
     * that we have to recompute the opaque region for the clip, then we
     * also need to recompute the opaque region of the node being clipped!
     */
    @Test
    public void opaqueRegionRecomputedWhenClipNodeHasOpaqueRegionChanges() {
        n.getOpaqueRegion();

        // Start off with a clip which supports an opaque region
        n.opaqueRegionRecomputed = false;
        NGRectangle clip = new NGRectangle();
        clip.updateRectangle(0, 0, 10, 10, 0, 0);
        clip.setFillPaint(Color.BLACK);
        n.setClipNode(clip);
        n.getOpaqueRegion();

        // Switch the clip so that it doesn't any longer
        n.opaqueRegionRecomputed = false;
        clip.setFillPaint(null);
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);

        // Switch back to the clip so that it does
        n.opaqueRegionRecomputed = false;
        clip.setFillPaint(Color.WHITE);
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);
    }

    /**
     * If the opacity switches from 1 to a value < 1, then we must recompute
     * the opaque region (the answer will always be false! I guess if we were
     * really clever we could just set opaque region to null in this case and
     * set "invalid" to false!)
     */
    @Test
    public void opaqueRegionRecomputedWhenOpacityGoesFromOneToLessThanOne() {
        n.getOpaqueRegion();

        // Change to less than one
        n.opaqueRegionRecomputed = false;
        n.setOpacity(.9f);
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);

        // Change to one again
        n.opaqueRegionRecomputed = false;
        n.setOpacity(1f);
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);

        // Change to one again!
        n.opaqueRegionRecomputed = false;
        n.setOpacity(1f);
        n.getOpaqueRegion();
        assertFalse(n.opaqueRegionRecomputed);

        // Change to zero. Note that changing to 0 doesn't mean that we have
        // to invalidate the opaque region, because a 0 opacity node will never
        // be asked for its opaque region, but we do it anyway :-(.
        n.opaqueRegionRecomputed = false;
        n.setOpacity(0f);
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);
    }

    /**
     * Tests for handling what happens as we toggle between Zero and other values.
     */
    @Test
    public void opaqueRegionRecomputedWhenOpacityGoesFromZeroToMoreThanZero() {
        n.setOpacity(0f);
        n.getOpaqueRegion();

        // Change to > 0 and < 1
        n.opaqueRegionRecomputed = false;
        n.setOpacity(.9f);
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);

        // Change to zero again
        n.opaqueRegionRecomputed = false;
        n.setOpacity(0f);
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);

        // Change to zero again!
        n.opaqueRegionRecomputed = false;
        n.setOpacity(0f);
        n.getOpaqueRegion();
        assertFalse(n.opaqueRegionRecomputed);

        // Change to one
        n.opaqueRegionRecomputed = false;
        n.setOpacity(1f);
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);
    }

    /**
     * If the opacity changes between 0 and 1, there is no point invalidating the
     * opaqueRegion since it will always remain disabled in this range.
     */
    @Test
    public void opaqueRegionNotRecomputedWhenOpacityNeverGoesToOneOrZero() {
        n.setOpacity(.1f);
        n.getOpaqueRegion();

        for (float f=.1f; f<.9f; f+=.1) {
            // Change to > 0 and < 1
            n.opaqueRegionRecomputed = false;
            n.setOpacity(f);
            n.getOpaqueRegion();
            assertFalse(n.opaqueRegionRecomputed);
        }
    }

    // Note that when depth buffer is enabled we should NEVER be asking for the opaque
    // region, so we don't do anything special in terms of invalidating the opaque region

    @Test
    public void opaqueRegionRecomputedWhenBlendModeChanges() {
        n.getOpaqueRegion();

        for (Blend.Mode mode : Blend.Mode.values()) {
            // Set blend mode
            n.opaqueRegionRecomputed = false;
            n.setNodeBlendMode(mode);
            n.getOpaqueRegion();
            if (mode == Blend.Mode.SRC_OVER) continue;
            assertTrue(n.opaqueRegionRecomputed);

            // Set back to null
            n.opaqueRegionRecomputed = false;
            n.setNodeBlendMode(null);
            n.getOpaqueRegion();
            assertTrue(n.opaqueRegionRecomputed);

            // Set to SRC_OVER (should recompute even though it may be a NOP)
            // For leaf nodes it is a NOP, but for groups there is a difference
            n.opaqueRegionRecomputed = false;
            n.setNodeBlendMode(Blend.Mode.SRC_OVER);
            n.getOpaqueRegion();
            assertTrue(n.opaqueRegionRecomputed);

            // Set to blend mode (should do it)
            n.opaqueRegionRecomputed = false;
            n.setNodeBlendMode(mode);
            n.getOpaqueRegion();
            assertTrue(n.opaqueRegionRecomputed);

            // Set back to SRC_OVER (should do it)
            n.opaqueRegionRecomputed = false;
            n.setNodeBlendMode(Blend.Mode.SRC_OVER);
            n.getOpaqueRegion();
            assertTrue(n.opaqueRegionRecomputed);

            // Set back to null
            n.opaqueRegionRecomputed = false;
            n.setNodeBlendMode(null);
            n.getOpaqueRegion();
            assertTrue(n.opaqueRegionRecomputed);
        }
    }

    /**
     * If we change references to the effect, we're might have to
     * figure things out again. Right now, we only pay attention to changes
     * from null to not-null (and vice versa). To enable opaque regions for
     * effects, we need to be notified by the effect whenever the effect
     * changes in such a way as to impact whether it can participate in
     * opaque regions.
     */
    @Test
    public void opaqueRegionNotRecomputedWhenEffectReferenceChanges() {
        n.getOpaqueRegion();

        // Set some effect (was null)
        n.opaqueRegionRecomputed = false;
        n.setEffect(new TransparentEffect());
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);

        // Change to some effect which messes with alpha.
        n.opaqueRegionRecomputed = false;
        n.setEffect(new TransparentEffect());
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);

        // Change to null.
        n.opaqueRegionRecomputed = false;
        n.setEffect(null);
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);

        // Change to some effect that messes with alpha
        n.opaqueRegionRecomputed = false;
        Effect effect = new TransparentEffect();
        n.setEffect(effect);
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);

        // Set the same effect again! Right now we simply recompute
        // every time if an effect may mess with alpha
        n.opaqueRegionRecomputed = false;
        n.setEffect(effect);
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);

        // Set the effect to be one that will never mess with alpha
        n.opaqueRegionRecomputed = false;
        effect = new OpaqueEffect();
        n.setEffect(effect);
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);

        // Set it to the same instance. Right now we simply recompute
        // every time if an effect may mess with alpha
        n.opaqueRegionRecomputed = false;
        n.setEffect(effect);
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);

        // Set it to another instance that also doesn't mess with alpha.
        // Right now we simply recompute every time if an effect may mess with alpha
        n.opaqueRegionRecomputed = false;
        n.setEffect(new OpaqueEffect());
        n.getOpaqueRegion();
        assertTrue(n.opaqueRegionRecomputed);
    }

    /**************************************************************************
     *                                                                        *
     * getOpaqueRegion tests                                                  *
     *                                                                        *
     *************************************************************************/

    @Test
    public void testGetOpaqueRegionReturnsNullIf_supportsOpaqueRegion_returnsFalse() {
        NGPath path = new NGPath();
        path.setFillPaint(Color.BLACK);
        assertNull(path.getOpaqueRegion());
    }

    @Test public void testGetOpaqueRegionReturnsNullIf_hasOpaqueRegion_returnsFalse() {
        n.setEffect(new TransparentEffect());
        assertNull(n.getOpaqueRegion());
    }

    @Test public void testGetOpaqueRegionWithNoClip() {
        assertEquals(new RectBounds(0, 0, 10, 10), n.getOpaqueRegion());
    }

    @Test public void testGetOpaqueRegionWithSimpleRectangleClip() {
        NGRectangle clip = new NGRectangle();
        clip.setFillPaint(Color.BLACK);
        clip.updateRectangle(3, 3, 4, 4, 0, 0);
        n.setClipNode(clip);
        assertEquals(new RectBounds(3, 3, 7, 7), n.getOpaqueRegion());
    }

    @Test public void testGetOpaqueRegionWithSimpleRectangleClipWithNoFill() {
        NGRectangle clip = new NGRectangle();
        clip.updateRectangle(3, 3, 4, 4, 0, 0);
        n.setClipNode(clip);
        assertNull(n.getOpaqueRegion());
    }

    @Test public void testGetOpaqueRegionWithTranslatedRectangleClip() {
        NGRectangle clip = new NGRectangle();
        clip.setFillPaint(Color.BLACK);
        clip.updateRectangle(0, 0, 4, 4, 0, 0);
        clip.setTransformMatrix(BaseTransform.getTranslateInstance(2, 2));
        n.setClipNode(clip);
        assertEquals(new RectBounds(2, 2, 6, 6), n.getOpaqueRegion());
    }

    @Test public void testGetOpaqueRegionWithScaledRectangleClip() {
        NGRectangle clip = new NGRectangle();
        clip.setFillPaint(Color.BLACK);
        clip.updateRectangle(0, 0, 4, 4, 0, 0);
        clip.setTransformMatrix(BaseTransform.getScaleInstance(.5, .5));
        n.setClipNode(clip);
        assertEquals(new RectBounds(0, 0, 2, 2), n.getOpaqueRegion());
    }

    @Test public void testGetOpaqueRegionWithTranslatedAndScaledRectangleClip() {
        NGRectangle clip = new NGRectangle();
        clip.setFillPaint(Color.BLACK);
        clip.updateRectangle(0, 0, 4, 4, 0, 0);
        clip.setTransformMatrix(
                BaseTransform.getTranslateInstance(2, 2).deriveWithConcatenation(
                        BaseTransform.getScaleInstance(.5, .5)));
        n.setClipNode(clip);
        assertEquals(new RectBounds(2, 2, 4, 4), n.getOpaqueRegion());
    }

    @Test public void testGetOpaqueRegionWithRotatedRectangleClip() {
        NGRectangle clip = new NGRectangle();
        clip.setFillPaint(Color.BLACK);
        clip.updateRectangle(0, 0, 4, 4, 0, 0);
        clip.setTransformMatrix(BaseTransform.getRotateInstance(45, 5, 5));
        n.setClipNode(clip);
        assertNull(n.getOpaqueRegion());
    }

    class NGNodeMock extends NGNode {
        boolean opaqueRegionRecomputed = false;
        RectBounds computedOpaqueRegion = new RectBounds(0, 0, 10, 10);

        void changeOpaqueRegion(float x, float y, float x2, float y2) {
            computedOpaqueRegion = new RectBounds(x, y, x2, y2);
            geometryChanged();
        }

        @Override
        protected boolean hasOpaqueRegion() {
            opaqueRegionRecomputed = true;
            return super.hasOpaqueRegion() && computedOpaqueRegion != null;
        }

        @Override
        protected RectBounds computeOpaqueRegion(RectBounds opaqueRegion) {
            opaqueRegionRecomputed = true;
            assert computedOpaqueRegion != null;
            return (RectBounds) opaqueRegion.deriveWithNewBounds(computedOpaqueRegion);
        }

        @Override
        protected void renderContent(Graphics g) { }

        @Override
        protected boolean hasOverlappingContents() {
            return false;
        }

        @Override
        protected boolean supportsOpaqueRegions() {
            return true;
        }
    }

    static abstract class MockEffect extends Effect {

        @Override
        public ImageData filter(FilterContext fctx, BaseTransform transform, Rectangle outputClip, Object renderHelper, Effect defaultInput) {
            return null;
        }

        @Override
        public BaseBounds getBounds(BaseTransform transform, Effect defaultInput) {
            return null;
        }

        @Override
        public AccelType getAccelType(FilterContext fctx) {
            return AccelType.OPENGL;
        }
    }

    class TransparentEffect extends MockEffect {
        @Override
        public boolean reducesOpaquePixels() {
            return true;
        }
    }

    class OpaqueEffect extends MockEffect {
        @Override
        public boolean reducesOpaquePixels() {
            return false;
        }

    }
}
