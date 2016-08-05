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

package com.sun.prism;

/**
 * TODO: 3D - Need documentation
 * This class represents a phong material for retained mode rendering
 */

public interface PhongMaterial extends Material {

    public enum MapType {DIFFUSE, SPECULAR, BUMP, SELF_ILLUM};
    public static final int DIFFUSE = MapType.DIFFUSE.ordinal();
    public static final int SPECULAR = MapType.SPECULAR.ordinal();
    public static final int BUMP = MapType.BUMP.ordinal();
    public static final int SELF_ILLUM = MapType.SELF_ILLUM.ordinal();
    public static final int MAX_MAP_TYPE = MapType.values().length;

    public void setDiffuseColor(float r, float g, float b, float a);
    public void setSpecularColor(boolean set, float r, float g, float b, float a);

    public void setTextureMap(TextureMap map);

    public void lockTextureMaps();

    public void unlockTextureMaps();
}
