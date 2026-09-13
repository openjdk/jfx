/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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

#ifndef METAL_PHONGMATERIAL_H
#define METAL_PHONGMATERIAL_H

#import "MetalContext.h"

#define DIFFUSE 0
#define SPECULAR 1
#define BUMP 2
#define SELFILLUMINATION 3

#define SPEC_NONE 0
#define SPEC_TEX 1
#define SPEC_CLR 2
#define SPEC_MIX 3

@interface MetalPhongMaterial : NSObject
{
    MetalContext* context;
    vector_float4 diffuseColor;
    vector_float4 specularColor;
    bool specularColorSet;
    id<MTLTexture> map[4];
}

- (MetalPhongMaterial*) createPhongMaterial:(MetalContext*)ctx;
- (void) setDiffuseColor:(float)r
                       g:(float)g
                       b:(float)b
                       a:(float)a;
- (void) setSpecularColor:(bool)set
                        r:(float)r
                        g:(float)g
                        b:(float)b
                        a:(float)a;

- (vector_float4) getDiffuseColor;
- (vector_float4) getSpecularColor;
- (bool) isSpecularMap;
- (bool) isSpecularColor;
- (bool) isBumpMap;
- (int)  getSpecType;
- (bool) isSelfIllumMap;
- (void) setMap:(int)mapID
            map:(id<MTLTexture>)texMap;
- (id<MTLTexture>) getMap:(int)mapID;
@end

#endif
