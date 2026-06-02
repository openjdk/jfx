/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.scenario.effect.impl;

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.Filterable;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.EffectPeer;
import com.sun.scenario.effect.impl.PoolFilterable;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.scenario.effect.impl.prism.PrFilterContext;
import com.sun.scenario.effect.impl.state.RenderState;
import org.junit.jupiter.api.Test;
import test.javafx.util.ReflectionUtils;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RendererTest {

    @Test
    void getPeerInstance_returnsSameInstance_forNameAndUnrollCount() {
        Renderer renderer = new StubRenderer();
        FilterContext fctx = PrFilterContext.getPrinterContext(new Object());
        EffectPeer<?> testEffectA0 = renderer.getPeerInstance(fctx, "TestEffectA", 0);
        EffectPeer<?> testEffectA1 = renderer.getPeerInstance(fctx, "TestEffectA", 1);
        EffectPeer<?> testEffectA2 = renderer.getPeerInstance(fctx, "TestEffectA", 2);
        EffectPeer<?> testEffectB0 = renderer.getPeerInstance(fctx, "TestEffectB", 0);
        EffectPeer<?> testEffectB1 = renderer.getPeerInstance(fctx, "TestEffectB", 1);
        assertNotSame(testEffectA0, testEffectA1);
        assertNotSame(testEffectA0, testEffectA2);
        assertSame(testEffectA0, renderer.getPeerInstance(fctx, "TestEffectA", 0));
        assertSame(testEffectA1, renderer.getPeerInstance(fctx, "TestEffectA", 1));
        assertSame(testEffectA2, renderer.getPeerInstance(fctx, "TestEffectA", 2));
        assertNotSame(testEffectB0, testEffectB1);
        assertSame(testEffectB0, renderer.getPeerInstance(fctx, "TestEffectB", 0));
        assertSame(testEffectB1, renderer.getPeerInstance(fctx, "TestEffectB", 1));
    }

    @Test
    void clearPeers_disposesEffectPeers() {
        var renderer = new StubRenderer();
        Map<?, ?> peerCache = ReflectionUtils.getFieldValue(renderer, "peerCache");
        FilterContext fctx = PrFilterContext.getPrinterContext(new Object());
        renderer.getPeerInstance(fctx, "TestEffectA", 0);
        renderer.getPeerInstance(fctx, "TestEffectA", 1);
        renderer.getPeerInstance(fctx, "TestEffectB", 2);
        assertEquals(3, peerCache.size());
        renderer._clearPeers();
        assertEquals(0, peerCache.size());
        assertEquals(3, renderer.disposeCalled);
    }

    static class StubRenderer extends Renderer {
        int disposeCalled = 0;

        @Override public Effect.AccelType getAccelType() { return null; }
        @Override public int getCompatibleWidth(int w) { return 0; }
        @Override public int getCompatibleHeight(int h) { return 0; }
        @Override public PoolFilterable createCompatibleImage(int w, int h) { return null; }
        @Override public void clearImage(Filterable image) {}
        @Override public ImageData createImageData(FilterContext fctx, Filterable src) { return null; }
        @Override public Filterable transform(FilterContext fctx, Filterable original, BaseTransform transform,
                                              Rectangle origBounds, Rectangle xformBounds) { return null; }
        @Override public ImageData transform(FilterContext fctx, ImageData original, BaseTransform transform,
                                             Rectangle origBounds, Rectangle xformBounds) { return null; }
        @Override public RendererState getRendererState() { return null; }
        @Override protected Renderer getBackupRenderer() { return null; }
        @Override public boolean isImageDataCompatible(ImageData id) { return false; }
        @Override protected EffectPeer<?> createPeer(FilterContext fctx, String name, int unrollCount) {
            return new EffectPeer<>(fctx, this, "StubEffect") {
                @Override
                public ImageData filter(Effect effect, RenderState renderState, BaseTransform transform,
                                        Rectangle outputClip, ImageData... inputs) {
                    return null;
                }

                @Override
                public void dispose() {
                    disposeCalled++;
                }
            };
        }

        public void _clearPeers() { clearPeers(); }
    }
}
