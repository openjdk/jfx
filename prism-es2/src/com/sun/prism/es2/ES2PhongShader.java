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

package com.sun.prism.es2;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO: 3D - Need documentation
 */
class ES2PhongShader {

    //dimensions:
    static ES2Shader shaders[][][][][] = null;
    static String vertexShaderSource;
    static String mainFragShaderSource;

    enum DiffuseState {

        NONE,
        DIFFUSECOLOR,
        TEXTURE
    }

    enum SpecularState {

        NONE,
        SPECULARCOLOR,
        TEXTURE
    }

    enum SelfIllumination {

        NONE,
        TEXTURE
    }

    enum BumpMapState {

        NONE,
        TEXTURE,
    }
    static final int lightStateCount = 4;
    private static String diffuseShaderParts[] = new String[DiffuseState.values().length];
    private static String SpecularShaderParts[] = new String[SpecularState.values().length];
    private static String selfIlluminationShaderParts[] = new String[SelfIllumination.values().length];
    private static String normalMapShaderParts[] = new String[BumpMapState.values().length];
    private static String lightingShaderParts[] = new String[lightStateCount];

    static {
        shaders = new ES2Shader[DiffuseState.values().length][SpecularState.values().length]
                [SelfIllumination.values().length][BumpMapState.values().length][lightStateCount];

        //NOTE: When creating new shaders, underscore denotes a "shader part"
        diffuseShaderParts[DiffuseState.NONE.ordinal()] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/diffuse_none.frag"));
        diffuseShaderParts[DiffuseState.DIFFUSECOLOR.ordinal()] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/diffuse_color.frag"));
        diffuseShaderParts[DiffuseState.TEXTURE.ordinal()] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/diffuse_texture.frag"));

        SpecularShaderParts[SpecularState.NONE.ordinal()] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/specular_none.frag"));
        SpecularShaderParts[SpecularState.SPECULARCOLOR.ordinal()] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/specular_color.frag"));
        SpecularShaderParts[SpecularState.TEXTURE.ordinal()] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/specular_texture.frag"));

        selfIlluminationShaderParts[SelfIllumination.NONE.ordinal()] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/selfIllum_none.frag"));
        selfIlluminationShaderParts[SelfIllumination.TEXTURE.ordinal()] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/selfIllum_texture.frag"));

        normalMapShaderParts[BumpMapState.NONE.ordinal()] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/normalMap_none.frag"));
        normalMapShaderParts[BumpMapState.TEXTURE.ordinal()] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/normalMap_texture.frag"));

        lightingShaderParts[0] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/main0Lights.frag"));
        lightingShaderParts[1] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/main1Light.frag"));
        lightingShaderParts[2] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/main2Lights.frag"));
        lightingShaderParts[3] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/main3Lights.frag"));

        vertexShaderSource = ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/main.vert"));

    }

    static ES2Shader getShader(ES2MeshView meshView, ES2Context context) {

        ES2PhongMaterial material = meshView.getMaterial();
        DiffuseState diffuseState = DiffuseState.NONE;

        //TODO: 3D - determine proper check (does a TEXTURE override a color?)
        //TODO: 3D - May remove diffuseColor check if we are certain that
        //           it is never null.
        if (material.diffuseColor != null) {
            diffuseState = DiffuseState.DIFFUSECOLOR;
        }

        if (material.diffuseMap) {
            diffuseState = DiffuseState.TEXTURE;
        }

        SpecularState specularState = SpecularState.NONE;

        //TODO: 3D - determine proper check (does a TEXTURE override a color?)
        if (material.specularColor != null) {
            specularState = SpecularState.SPECULARCOLOR;
        }

        if (material.specularMap) {
            if (material.isSpecularAlpha) {
                specularState = SpecularState.TEXTURE;
            } else {
                specularState = SpecularState.SPECULARCOLOR;
            }
        }

        BumpMapState bumpState = BumpMapState.NONE;

        if (material.bumpMap) {
            bumpState = BumpMapState.TEXTURE;
        }

        SelfIllumination selfIllumState = SelfIllumination.NONE;

        if (material.selfIlluminationMap) {
            selfIllumState = SelfIllumination.TEXTURE;
        }

        int numLights = 0;
        for (ES2Light light : meshView.getPointLights()) {
            if (light != null && light.w > 0) { numLights++; }
        }

        ES2Shader shader = shaders[diffuseState.ordinal()][specularState.ordinal()]
                [selfIllumState.ordinal()][bumpState.ordinal()][numLights];
        if (shader == null) {
            String[] pixelShaders = new String[]{
                diffuseShaderParts[diffuseState.ordinal()],
                SpecularShaderParts[specularState.ordinal()],
                selfIlluminationShaderParts[selfIllumState.ordinal()],
                normalMapShaderParts[bumpState.ordinal()],
                lightingShaderParts[numLights]
            };

            //TODO: 3D - should be done in state checking?
            Map<String, Integer> attributes = new HashMap<String, Integer>();
            attributes.put("pos", 0);
            attributes.put("texCoords", 1);
            attributes.put("tangent", 2);

            Map<String, Integer> samplers = new HashMap<String, Integer>();
            samplers.put("diffuseTexture", 0);
            samplers.put("specularMap", 1);
            samplers.put("normalMap", 2);
            samplers.put("selfIllumTexture", 3);

            shader = ES2Shader.createFromSource(context, vertexShaderSource, pixelShaders, samplers, attributes, 1, false);


            shaders[diffuseState.ordinal()][specularState.ordinal()][selfIllumState.ordinal()]
                    [bumpState.ordinal()][numLights] = shader;
        }
        return shader;
    }

    static void setShaderParamaters(ES2Shader shader, ES2MeshView meshView, ES2Context context) {

        ES2PhongMaterial material = meshView.getMaterial();

        //TODO: 3D - May remove diffuseColor check if we are certain that
        //           it is never null.
        if (material.diffuseColor != null && !material.diffuseMap) {

            shader.setConstant("diffuseColor", material.diffuseColor.getRed(),
                    material.diffuseColor.getGreen(), material.diffuseColor.getBlue(),
                    material.diffuseColor.getAlpha());
        }

        if (material.diffuseMap) {
            context.updateTexture(0, material.maps[ES2PhongMaterial.DIFFUSE_MAP]);
        }

        if (material.specularMap) {
            context.updateTexture(1, material.maps[ES2PhongMaterial.SPECULAR_MAP]);
        }

        if (material.bumpMap) {
            context.updateTexture(2, material.maps[ES2PhongMaterial.BUMP_MAP]);
        }

        if (material.selfIlluminationMap) {
            context.updateTexture(3, material.maps[ES2PhongMaterial.SELF_ILLUM_MAP]);
        }

        shader.setConstant("ambientColor", meshView.getAmbientLightRed(),
                meshView.getAmbientLightGreen(), meshView.getAmbientLightBlue());

        int i = 0;
        for(ES2Light light : meshView.getPointLights()) {
            if (light != null && light.w > 0) {
                shader.setConstant("lights[" + i + "].pos", light.x, light.y, light.z, light.w);
                shader.setConstant("lights[" + i + "].color", light.r, light.g, light.b);
                i++;
            }
        }
    }
}
