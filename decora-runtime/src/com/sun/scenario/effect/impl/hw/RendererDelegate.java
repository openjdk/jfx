/*
 * Copyright (c) 2008, 2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.impl.hw;

import java.util.Map;
import com.sun.scenario.effect.Effect.AccelType;
import com.sun.scenario.effect.FloatMap;

public interface RendererDelegate {

    public AccelType getAccelType();

    public interface Listener {
        public void markLost();
    }

    public void setListener(Listener listener);

    public void enable();
    public void disable();
    public void dispose();

    public Shader createShader(String name,
                               Map<String, Integer> samplers,
                               Map<String, Integer> params);

    public Texture createFloatTexture(int w, int h);
    public void updateFloatTexture(Object texture, FloatMap map);

    public void drawQuad(Drawable dst,
                         float dx1, float dy1, float dx2, float dy2);
    public void drawTexture(Drawable dst,
                            Texture src, boolean linear,
                            float dx1, float dy1, float dx2, float dy2,
                            float tx1, float ty1, float tx2, float ty2);
    public void drawMappedTexture(Drawable dst,
                                  Texture src, boolean linear,
                                  float dx1, float dy1, float dx2, float dy2,
                                  float tx11, float ty11, float tx21, float ty21,
                                  float tx12, float ty12, float tx22, float ty22);
    public void drawTexture(Drawable dst,
                            Texture src1, boolean linear1,
                            Texture src2, boolean linear2,
                            float dx1, float dy1, float dx2, float dy2,
                            float t1x1, float t1y1, float t1x2, float t1y2,
                            float t2x1, float t2y1, float t2x2, float t2y2);
    public void drawMappedTexture(Drawable dst,
                                  Texture src1, boolean linear1,
                                  Texture src2, boolean linear2,
                                  float dx1, float dy1, float dx2, float dy2,
                                  float ux11, float uy11, float ux21, float uy21,
                                  float ux12, float uy12, float ux22, float uy22,
                                  float vx11, float vy11, float vx21, float vy21,
                                  float vx12, float vy12, float vx22, float vy22);
}
