/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.d3d12;

import com.sun.prism.impl.Disposer;
import com.sun.prism.d3d12.ni.D3D12NativeShader;

public class D3D12ShaderData implements Disposer.Record {

    private D3D12NativeShader mShader;

    D3D12ShaderData(D3D12NativeShader shader) {
        if (!shader.isValid()) {
            throw new NullPointerException("Shader data is NULL");
        }

        mShader = shader;
    }

    D3D12NativeShader getShader() {
        return mShader;
    }

    boolean isValid() {
        return mShader.isValid();
    }

    @Override
    public void dispose() {
        if (isValid()) {
            mShader.dispose();
        }
    }
}
