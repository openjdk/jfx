/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.ps;

import com.sun.javafx.geom.transform.Affine3D;
import com.sun.prism.Graphics;
import com.sun.prism.Texture;

public interface ShaderGraphics extends Graphics {

    public void getPaintShaderTransform(Affine3D ret);

    public void setExternalShader(Shader shader);

    public void drawTextureRaw2(Texture src1, Texture src2,
                                float dx1, float dy1, float dx2, float dy2,
                                float t1x1, float t1y1, float t1x2, float t1y2,
                                float t2x1, float t2y1, float t2x2, float t2y2);

    public void drawMappedTextureRaw2(Texture src1, Texture src2,
                                      float dx1, float dy1, float dx2, float dy2,
                                      float t1x11, float t1y11, float t1x21, float t1y21,
                                      float t1x12, float t1y12, float t1x22, float t1y22,
                                      float t2x11, float t2y11, float t2x21, float t2y21,
                                      float t2x12, float t2y12, float t2x22, float t2y22);
}
