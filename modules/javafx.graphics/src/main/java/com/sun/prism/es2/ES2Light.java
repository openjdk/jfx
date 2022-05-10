/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.es2;

/**
 * TODO: 3D - Need documentation
 */
class ES2Light {

    float x, y, z = 0;
    float r, g, b, w = 1;
    float ca, la, qa, isAttenuated;
    float maxRange;
    float dirX, dirY, dirZ;
    float innerAngle, outerAngle, falloff;

    ES2Light(float x, float y, float z, float r, float g, float b, float w, float ca, float la, float qa,
            float isAttenuated, float maxRange, float dirX, float dirY, float dirZ,
            float innerAngle, float outerAngle, float falloff) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
        this.w = w;
        this.ca = ca;
        this.la = la;
        this.qa = qa;
        this.isAttenuated = isAttenuated;
        this.maxRange = maxRange;
        this.dirX = dirX;
        this.dirY = dirY;
        this.dirZ = dirZ;
        this.innerAngle = innerAngle;
        this.outerAngle = outerAngle;
        this.falloff = falloff;
    }

    boolean isPointLight() {
        return falloff == 0 && outerAngle == 180 && isAttenuated > 0.5;
    }

    boolean isDirectionalLight() {
        // testing if w is 0 or 1 using <0.5 since equality check for floating points might not work well
        return isAttenuated < 0.5;
    }
}
