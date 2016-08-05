/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.impl.ps;

import com.sun.prism.Image;
import com.sun.prism.Texture;
import com.sun.prism.impl.BaseResourceFactory;
import com.sun.prism.impl.shape.BasicEllipseRep;
import com.sun.prism.impl.shape.BasicRoundRectRep;
import com.sun.prism.impl.shape.BasicShapeRep;
import com.sun.prism.ps.ShaderFactory;
import com.sun.prism.shape.ShapeRep;
import com.sun.prism.impl.PrismSettings;
import java.util.Map;

public abstract class BaseShaderFactory extends BaseResourceFactory
    implements ShaderFactory
{
    public BaseShaderFactory() {
        super();
    }

    public BaseShaderFactory(Map<Image, Texture> clampTexCache,
                             Map<Image, Texture> repeatTexCache,
                             Map<Image, Texture> mipmapTexCache)
    {
        super(clampTexCache, repeatTexCache, mipmapTexCache);
    }

    public ShapeRep createPathRep() {
        return PrismSettings.cacheComplexShapes ?
                new CachingShapeRep() : new BasicShapeRep();
    }

    public ShapeRep createRoundRectRep() {
        return PrismSettings.cacheSimpleShapes ?
            new CachingRoundRectRep() : new BasicRoundRectRep();
    }

    public ShapeRep createEllipseRep() {
        return PrismSettings.cacheSimpleShapes ?
            new CachingEllipseRep() : new BasicEllipseRep();
    }

    public ShapeRep createArcRep() {
        return PrismSettings.cacheComplexShapes ?
            new CachingShapeRep() : new BasicShapeRep();
    }
}
