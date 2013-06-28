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

package com.sun.prism.ps;

import com.sun.prism.GraphicsResource;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public interface Shader extends GraphicsResource {
    public void enable();
    public void disable();

    /**
     * Returns whether this shader is valid and can be used for rendering.
     * Called on the rendering thread.
     *
     * @return true if valid, false if not
     */
    public boolean isValid();

    public void setConstant(String name, int i0);
    public void setConstant(String name, int i0, int i1);
    public void setConstant(String name, int i0, int i1, int i2);
    public void setConstant(String name, int i0, int i1, int i2, int i3);
    public void setConstants(String name, IntBuffer buf, int off, int count);

    public void setConstant(String name, float f0);
    public void setConstant(String name, float f0, float f1);
    public void setConstant(String name, float f0, float f1, float f2);
    public void setConstant(String name, float f0, float f1, float f2, float f3);
    public void setConstants(String name, FloatBuffer buf, int off, int count);
}
