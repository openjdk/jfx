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
    vec3 dir;
    float range;
    float cosOuter;
    float denom; // cosInner - cosOuter
    float falloff;
};

uniform vec3 ambientColor;
uniform Light lights[3];

varying vec3 eyePos;
varying vec4 lightTangentSpacePositions[3];
varying vec4 lightTangentSpaceDirections[3];

// Because pow(0, 0) is undefined (https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/pow.xhtml),
// we need special treatment for falloff == 0 cases
float computeSpotlightFactor(vec3 l, vec3 lightDir, float cosOuter, float denom, float falloff) {
    if (falloff == 0.0 && cosOuter == -1.0) { // point light optimization (cosOuter == -1 is outerAngle == 180)
        return 1.0;
    }
    float cosAngle = dot(normalize(-lightDir), l);
    float cutoff = cosAngle - cosOuter;
    if (falloff != 0.0) {
        return pow(clamp(cutoff / denom, 0.0, 1.0), falloff);
    }
    return cutoff >= 0.0 ? 1.0 : 0.0;
}

/*
 * The methods 'computeSpotlightFactor2' and 'computeSpotlightFactor3' are alternatives to the 'computeSpotlightFactor'
 * method that was chosen for its better performance. They are kept for the case that a future changes will make them
 * more performant.
 *
float computeSpotlightFactor2(vec3 l, vec3 lightDir, float cosOuter, float denom, float falloff) {
    if (falloff != 0.0) {
        float cosAngle = dot(normalize(-lightDir), l);
        float cutoff = cosAngle - cosOuter;
        return pow(clamp(cutoff / denom, 0.0, 1.0), falloff);
    }
    if (cosOuter == -1.0) {  // point light optimization (cosOuter == -1 is outerAngle == 180)
        return 1.0;
    }
    float cosAngle = dot(normalize(-lightDir), l);
    float cutoff = cosAngle - cosOuter;
    return cutoff >= 0.0 ? 1.0 : 0.0;
}

float computeSpotlightFactor3(vec3 l, vec3 lightDir, float cosOuter, float denom, float falloff) {
    float cosAngle = dot(normalize(-lightDir), l);
    float cutoff = cosAngle - cosOuter;
    if (falloff != 0.0) {
        return pow(clamp(cutoff / denom, 0.0, 1.0), falloff);
    }
    return cutoff >= 0.0 ? 1.0 : 0.0;
}
*/

void computeLight(int i, vec3 n, vec3 refl, float specPower, inout vec3 d, inout vec3 s) {
    Light light = lights[i];
    vec3 pos = lightTangentSpacePositions[i].xyz;
    float dist = length(pos);
    if (dist > light.range) {
        return;
    }
    vec3 l = normalize(pos);

    vec3 lightDir = lightTangentSpaceDirections[i].xyz;
    float spotlightFactor = computeSpotlightFactor(l, lightDir, light.cosOuter, light.denom, light.falloff);

    float invAttnFactor = light.attn.x + light.attn.y * dist + light.attn.z * dist * dist;

    vec3 attenuatedColor = light.color.rgb * spotlightFactor / invAttnFactor;
    d += clamp(dot(n,l), 0.0, 1.0) * attenuatedColor;
    s += pow(clamp(dot(-refl, l), 0.0, 1.0), specPower) * attenuatedColor;
}

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

    computeLight(0, n, refl, power, d, s);
    computeLight(1, n, refl, power, d, s);
    computeLight(2, n, refl, power, d, s);

    vec3 rez = (ambientColor + d) * diffuse.xyz + s * specular.rgb;
    rez += apply_selfIllum().xyz;

    gl_FragColor = vec4(clamp(rez, 0.0, 1.0) , diffuse.a);
}
