/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect;

import com.sun.scenario.effect.impl.EffectPeer;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.impl.state.RenderState;

/**
 * Package-private base class for built-in effects, i.e., those that are
 * backed by an EffectPeer implementation.
 */
abstract class CoreEffect<T extends RenderState> extends FilterEffect<T> {

    private String peerKey;
    private int peerCount = -1;

    CoreEffect() {
        super();
    }

    CoreEffect(Effect input) {
        super(input);
    }

    CoreEffect(Effect input1, Effect input2) {
        super(input1, input2);
    }

    final void updatePeerKey(String key) {
        updatePeerKey(key, -1);
    }

    final void updatePeerKey(String key, int unrollCount) {
        this.peerKey = key;
        this.peerCount = unrollCount;
    }

    private EffectPeer getPeer(FilterContext fctx, int approxW, int approxH) {
        return Renderer.getRenderer(fctx, this, approxW, approxH).
            getPeerInstance(fctx, peerKey, peerCount);
    }

    /**
     * Returns an {@code EffectPeer} that is most optimal for the size
     * of the operation, which is inferred from the given inputs.
     * For example, smaller operations may run faster in software (by
     * avoiding high overhead of shader-based operations) so here we choose
     * an appropriate Renderer/EffectPeer combination based on the
     * dimensions of the first input.
     */
    final EffectPeer getPeer(FilterContext fctx, ImageData[] inputs) {
        // RT-27395
        // TODO: we would be much better off using getResultBounds() here
        // to infer the size of the operation since some effects (e.g. Flood)
        // do not have any inputs to consult...
        int approxW, approxH;
        if (inputs.length > 0) {
            Rectangle approxBounds = inputs[0].getUntransformedBounds();
            approxW = approxBounds.width;
            approxH = approxBounds.height;
        } else {
            // NOTE: temporary hack until we start using result bounds
            // (see comment above)...
            approxW = approxH = 500;
        }
        return getPeer(fctx, approxW, approxH);
    }

    /**
     * Convenience method that sends the given input data through the
     * current peer, and then attempts to release the input image data.
     */
    @Override
    public ImageData filterImageDatas(FilterContext fctx,
                                      BaseTransform transform,
                                      Rectangle outputClip,
                                      T rstate,
                                      ImageData... inputs)
    {
        return getPeer(fctx, inputs).filter(this, rstate, transform, outputClip, inputs);
    }

    @Override
    public AccelType getAccelType(FilterContext fctx) {
        // We choose relatively large (yet arbitrary) values for approxW/H
        // here so that we get the AccelType for the "ideal" case where
        // hardware acceleration is used.
        EffectPeer peer = getPeer(fctx, 1024, 1024);
        return peer.getAccelType();
    }
}
