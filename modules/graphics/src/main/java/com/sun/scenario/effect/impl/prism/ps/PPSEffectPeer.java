/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.impl.prism.ps;

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.ps.Shader;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.EffectPeer;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.scenario.effect.impl.state.RenderState;

public abstract class PPSEffectPeer<T extends RenderState> extends EffectPeer<T> {

    protected PPSEffectPeer(FilterContext fctx, Renderer r, String shaderName) {
        super(fctx, r, shaderName);
    }

    @Override
    public final ImageData filter(final Effect effect,
                                  final T renderState,
                                  final BaseTransform transform,
                                  final Rectangle outputClip,
                                  final ImageData... inputs)
    {
        setEffect(effect);
        setRenderState(renderState);
        setDestBounds(getResultBounds(transform, outputClip, inputs));
        return filterImpl(inputs);
    }

    abstract ImageData filterImpl(ImageData... inputs);

    protected abstract boolean isSamplerLinear(int i);

    protected abstract Shader createShader();

    protected abstract void updateShader(Shader shader);

    /**
     * Disposes resources associated with this peer.
     * Warning: called from the rendering thread.
     */
    @Override
    public abstract void dispose();

    @Override
    protected final PPSRenderer getRenderer() {
        return (PPSRenderer)super.getRenderer();
    }

    protected final String getShaderName() {
        return getUniqueName();
    }
}
