/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.null3d;

import com.sun.glass.ui.Screen;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.CompositeMode;
import com.sun.prism.RenderTarget;
import com.sun.prism.Texture;
import com.sun.prism.camera.PrismCameraImpl;
import com.sun.prism.impl.ps.BaseShaderContext;
import com.sun.prism.ps.Shader;

class DummyContext extends BaseShaderContext {

    private State state;

    DummyContext(Screen screen, DummyResourceFactory factory) {
        super(screen, factory, new DummyVertexBuffer(32));
    }

    @Override
    protected void init() {
        super.init();
        this.state = new State();
    }

    @Override
    protected State updateRenderTarget(RenderTarget target, PrismCameraImpl camera, boolean depthTest)  {
        return state;
    }

    @Override
    protected void updateTexture(int texUnit, Texture tex) {
    }

    @Override
    protected void updateShaderTransform(Shader shader, BaseTransform xform) {
    }

    @Override
    protected void updateWorldTransform(BaseTransform xform) {
    }

    @Override
    protected void updateClipRect(Rectangle clipRect) {
    }

    @Override
    protected void updateCompositeMode(CompositeMode mode) {
    }
}
