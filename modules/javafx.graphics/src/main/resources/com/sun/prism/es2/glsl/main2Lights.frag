/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

// main fragment shader

#ifdef GL_ES

#ifndef EXTENSION_APPLIED
#define EXTENSION_APPLIED
#extension GL_OES_standard_derivatives : enable
#endif

// Define default float precision for fragment shaders
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
precision highp int;
#else
precision mediump float;
precision mediump int;
#endif

#else

// Ignore GL_ES precision specifiers:
#define lowp
#define mediump
#define highp

#endif

vec4 apply_diffuse();
vec4 apply_specular();
vec3 apply_normal();
vec4 apply_selfIllum();

struct Light {
    vec4 pos;
    vec3 color;
    vec3 attn;
    float range;
};

uniform vec3 ambientColor;
uniform Light lights[3];

varying vec3 eyePos;
varying vec4 lightTangentSpacePositions[3];

void main()
{
    vec4 diffuse = apply_diffuse();

    if (diffuse.a == 0.0) discard;

    vec3 n = apply_normal();
    vec3 refl = reflect(normalize(eyePos), n);

    vec3 d = vec3(0.0);
    vec3 s = vec3(0.0);

    vec4 specular = apply_specular();
    float power = specular.a;

    float maxRange = lights[0].range;
    float dist = length(lightTangentSpacePositions[0].xyz);
    if (dist <= maxRange) {
        vec3 l = normalize(lightTangentSpacePositions[0].xyz);
        vec3 attenuatedColor = (lights[0].color).rgb / (lights[0].attn.x + lights[0].attn.y * dist + lights[0].attn.z * dist * dist);
        d += clamp(dot(n,l), 0.0, 1.0) * attenuatedColor;
        s += pow(clamp(dot(-refl, l), 0.0, 1.0), power) * attenuatedColor;
    }

    maxRange = lights[1].range;
    dist = length(lightTangentSpacePositions[1].xyz);
    if (dist <= maxRange) {
        vec3 l = normalize(lightTangentSpacePositions[1].xyz);
        vec3 attenuatedColor = (lights[1].color).rgb / (lights[1].attn.x + lights[1].attn.y * dist + lights[1].attn.z * dist * dist);
        d += clamp(dot(n,l), 0.0, 1.0) * attenuatedColor;
        s += pow(clamp(dot(-refl, l), 0.0, 1.0), power) * attenuatedColor;
    }
    vec3 rez = (ambientColor + d) * diffuse.xyz + s * specular.rgb;
    rez += apply_selfIllum().xyz;

    gl_FragColor = vec4(clamp(rez, 0.0, 1.0) , diffuse.a);
}
