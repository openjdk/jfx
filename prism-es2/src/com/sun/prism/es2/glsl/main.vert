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

// main vertex shader
//TODO: 3D - Don't calculate everything if we do not have to
#version 120

uniform mat4 viewProjectionMatrix;
uniform mat4 worldMatrix;
uniform vec3 camPos;
uniform vec4 ambientColor;
//float4      gAmbientData[10] : register (c20);

struct Light {
    vec4 pos;
    vec3 color;
};

//3 lights used

uniform Light lights[3];

attribute vec3 pos;
attribute vec2 texCoords;
attribute vec4 tangent;

varying vec3 eyePos;
varying vec3 localBump;
varying vec4[3] lightTangentSpacePositions;

vec3[3] quatToMatrix(vec4 q) {
    vec3 t1 = q.xyz * q.yzx *2;
    vec3 t2 = q.zxy * q.www *2;
    vec3 t3 = q.xyz * q.xyz *2;
    vec3 t4 = 1-(t3+t3.yzx);

    vec3 r1 = t1 + t2;
    vec3 r2 = t1 - t2;

    vec3 tangentFrame[3];

    tangentFrame[0] = vec3(t4.y, r1.x, r2.z);
    tangentFrame[1] = vec3(r2.x, t4.z, r1.y);
    tangentFrame[2] = vec3(r1.z, r2.y, t4.x);

    tangentFrame[2] *= (q.w>=0) ? 1 : -1;   // ATI normal map generator compatibility???

    return tangentFrame;
}

vec3 getLocalVector(vec3 global, vec3 tangentFrame[3]) {
    return vec3( dot(global,tangentFrame[1]), dot(global,tangentFrame[2]), dot(global,tangentFrame[0]) );
}

void main()
{
    vec3 tangentFrame[3];

    vec4 worldPos = worldMatrix * vec4(pos, 1.0);

    //Generate the tangent frame matrix by treating the tangent
    //normal as a quaternion and converting it.
    tangentFrame = quatToMatrix(tangent);

    //Translate the tangent frame to world space.
    for (int i=0; i!=3; ++i) {
        tangentFrame[i] = mat3x3(worldMatrix) * tangentFrame[i];
    }

    //Get the eye vector into world tangent space.
    vec3 Eye = camPos - worldPos.xyz;
    eyePos = getLocalVector(Eye, tangentFrame);

    //For Each light, calculate its LightToVertex vector and translate it into tangent space
    for (int k=0; k<3; ++k) {
        vec3 L = lights[k].pos.xyz - worldPos.xyz;
        lightTangentSpacePositions[k] = vec4( getLocalVector(L,tangentFrame)*lights[k].pos.w, 1);
    }

    mat4 mvpMatrix = viewProjectionMatrix * worldMatrix;

    //Send texcoords to Pixel Shader and calculate vertex position.
    gl_TexCoord[0].xy = texCoords;
    gl_Position = mvpMatrix * vec4(pos,1.0);
}
