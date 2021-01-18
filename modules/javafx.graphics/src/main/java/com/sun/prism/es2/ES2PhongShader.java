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
        TEXTURE,
        COLOR,
        MIX
    }

    enum SelfIllumState {

        NONE,
        TEXTURE
    }

    enum BumpMapState {

        NONE,
        TEXTURE,
    }
    static final int lightStateCount = 4;
    private static String diffuseShaderParts[] = new String[DiffuseState.values().length];
    private static String specularShaderParts[] = new String[SpecularState.values().length];
    private static String selfIllumShaderParts[] = new String[SelfIllumState.values().length];
    private static String normalMapShaderParts[] = new String[BumpMapState.values().length];
    private static String lightingShaderParts[] = new String[lightStateCount];

    static {
        shaders = new ES2Shader[DiffuseState.values().length][SpecularState.values().length]
                [SelfIllumState.values().length][BumpMapState.values().length][lightStateCount];

        //NOTE: When creating new shaders, underscore denotes a "shader part"
        diffuseShaderParts[DiffuseState.NONE.ordinal()] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/diffuse_none.frag"));
        diffuseShaderParts[DiffuseState.DIFFUSECOLOR.ordinal()] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/diffuse_color.frag"));
        diffuseShaderParts[DiffuseState.TEXTURE.ordinal()] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/diffuse_texture.frag"));

        specularShaderParts[SpecularState.NONE.ordinal()] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/specular_none.frag"));
        specularShaderParts[SpecularState.TEXTURE.ordinal()] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/specular_texture.frag"));
        specularShaderParts[SpecularState.COLOR.ordinal()] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/specular_color.frag"));
        specularShaderParts[SpecularState.MIX.ordinal()] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/specular_mix.frag"));

        selfIllumShaderParts[SelfIllumState.NONE.ordinal()] =
                ES2Shader.readStreamIntoString(ES2ResourceFactory.class.getResourceAsStream("glsl/selfIllum_none.frag"));
        selfIllumShaderParts[SelfIllumState.TEXTURE.ordinal()] =
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

    static SpecularState getSpecularState(ES2PhongMaterial material) {
        if (material.maps[ES2PhongMaterial.SPECULAR].getTexture() != null) {
            return material.specularColorSet ?
                    SpecularState.MIX : SpecularState.TEXTURE;
        }
        return material.specularColorSet ?
                SpecularState.COLOR : SpecularState.NONE;
    }

    static ES2Shader getShader(ES2MeshView meshView, ES2Context context) {

        ES2PhongMaterial material = meshView.getMaterial();

        DiffuseState diffuseState = DiffuseState.DIFFUSECOLOR;
        if (material.maps[ES2PhongMaterial.DIFFUSE].getTexture() != null) {
            diffuseState = DiffuseState.TEXTURE;
        }

        SpecularState specularState = getSpecularState(material);

        BumpMapState bumpState = BumpMapState.NONE;
        if (material.maps[ES2PhongMaterial.BUMP].getTexture() != null) {
            bumpState = BumpMapState.TEXTURE;
        }

        SelfIllumState selfIllumState = SelfIllumState.NONE;
        if (material.maps[ES2PhongMaterial.SELF_ILLUM].getTexture() != null) {
            selfIllumState = SelfIllumState.TEXTURE;
        }

        int numLights = 0;
        for (ES2Light light : meshView.getPointLights()) {
            if (light != null && light.w > 0) { numLights++; }
        }

        ES2Shader shader = shaders[diffuseState.ordinal()][specularState.ordinal()]
                [selfIllumState.ordinal()][bumpState.ordinal()][numLights];
        if (shader == null) {
            String fragShader = lightingShaderParts[numLights].replace("vec4 apply_diffuse();", diffuseShaderParts[diffuseState.ordinal()]);
            fragShader = fragShader.replace("vec4 apply_specular();", specularShaderParts[specularState.ordinal()]);
            fragShader = fragShader.replace("vec3 apply_normal();", normalMapShaderParts[bumpState.ordinal()]);
            fragShader = fragShader.replace("vec4 apply_selfIllum();", selfIllumShaderParts[selfIllumState.ordinal()]);

            String[] pixelShaders = new String[]{
                fragShader
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

        shader.setConstant("diffuseColor", material.diffuseColor.getRed(),
                material.diffuseColor.getGreen(), material.diffuseColor.getBlue(),
                material.diffuseColor.getAlpha());

        shader.setConstant("specularColor", material.specularColor.getRed(),
                material.specularColor.getGreen(), material.specularColor.getBlue(),
                material.specularColor.getAlpha());

        context.updateTexture(0, material.maps[ES2PhongMaterial.DIFFUSE].getTexture());
        context.updateTexture(1, material.maps[ES2PhongMaterial.SPECULAR].getTexture());
        context.updateTexture(2, material.maps[ES2PhongMaterial.BUMP].getTexture());
        context.updateTexture(3, material.maps[ES2PhongMaterial.SELF_ILLUM].getTexture());

        shader.setConstant("ambientColor", meshView.getAmbientLightRed(),
                meshView.getAmbientLightGreen(), meshView.getAmbientLightBlue());

        int i = 0;
        for (ES2Light light : meshView.getPointLights()) {
            if (light != null && light.w > 0) {
                shader.setConstant("lights[" + i + "].pos", light.x, light.y, light.z, light.w);
                shader.setConstant("lights[" + i + "].color", light.r, light.g, light.b);
                shader.setConstant("lights[" + i + "].attn", light.ca, light.la, light.qa);
                shader.setConstant("lights[" + i + "].range", light.maxRange);
                i++;
            }
        }
    }
}
