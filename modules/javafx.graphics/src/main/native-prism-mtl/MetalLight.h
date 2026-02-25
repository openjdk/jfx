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

#ifndef METAL_LIGHT_H
#define METAL_LIGHT_H

#import <Foundation/Foundation.h>

@interface MetalLight : NSObject
{
    @public
    float position[3];
    float color[3];
    float lightOn;
    float attenuation[4]; // ca, la, qa, isAttenuated
    float maxRange;
    float direction[3];
    float inAngle;
    float outAngle;
    float falloff;
}

- (MetalLight*) createLight:(float)x y:(float)y z:(float)z
            r:(float)r g:(float)g b:(float)b w:(float)w
            ca:(float)ca la:(float)la qa:(float)qa
            isA:(float)isAttenuated range:(float)range
            dirX:(float)dirX dirY:(float)dirY dirZ:(float)dirZ
            inA:(float)innerAngle outA:(float)outerAngle
            falloff:(float)fall_off;

- (bool) isPointLight;
- (bool) isDirectionalLight;

@end

#endif
