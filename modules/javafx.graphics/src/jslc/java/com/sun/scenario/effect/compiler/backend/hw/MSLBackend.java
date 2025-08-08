/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.compiler.backend.hw;

import com.sun.scenario.effect.compiler.JSLParser;
import com.sun.scenario.effect.compiler.model.CoreSymbols;
import com.sun.scenario.effect.compiler.model.Function;
import com.sun.scenario.effect.compiler.model.Param;
import com.sun.scenario.effect.compiler.model.Precision;
import com.sun.scenario.effect.compiler.model.Qualifier;
import com.sun.scenario.effect.compiler.model.Type;
import com.sun.scenario.effect.compiler.model.Variable;
import com.sun.scenario.effect.compiler.tree.CallExpr;
import com.sun.scenario.effect.compiler.tree.DiscardStmt;
import com.sun.scenario.effect.compiler.tree.Expr;
import com.sun.scenario.effect.compiler.tree.FuncDef;
import com.sun.scenario.effect.compiler.tree.JSLVisitor;
import com.sun.scenario.effect.compiler.tree.VarDecl;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.util.Map.entry;

/*
 * Class that generates Metal shaders from JSL.
 */

public class MSLBackend extends SLBackend {

    private static String headerFilesDir = null;
    private static final String FRAGMENT_SHADER_HEADER_FILE_NAME = "FragmentShaderCommon.h";
    private static final StringBuilder objCHeader = new StringBuilder();
    private static String objCHeaderFileName;
    private static final String PRISM_SHADER_HEADER_FILE_NAME = "PrismShaderCommon.h";
    private static final String DECORA_SHADER_HEADER_FILE_NAME = "DecoraShaderCommon.h";
    private static final List<String> shaderFunctionNameList = new ArrayList<>();

    private String shaderFunctionName;
    private String sampleTexFuncName;
    private String uniformStructName;
    private final List<String> uniformNames = new ArrayList<>();
    private String uniformIDsEnumName;
    private String uniformIDs;
    private int uniformIDCount;
    private String uniformsForShaderFile; // visitVarDecl() accumulates all the Uniforms in this string.
    private String uniformsForObjCFiles;
    private boolean isPrismShader;
    private boolean hasTextureVar;

    private final List<String> helperFunctions = new ArrayList<>();
    private static final String MTL_HEADERS_DIR = "/mtl-headers/";
    private static final String MAIN = "void main() {";

    private static final Map<String, String> QUAL_MAP = Map.of(
        "param", "constant");

    private static final Map<String, String> texSamplerMap = new HashMap<>();

    private static final Map<String, String> TYPE_MAP = Map.of(
        "sampler",  "texture2d<float>",
        "lsampler", "texture2d<float>",
        "fsampler", "texture2d<float>");

    private static final Map<String, String> VAR_MAP = Map.ofEntries(
        entry("pos0",                     "in.texCoord0"),
        entry("pos1",                     "in.texCoord1"),
        entry("pixcoord",                 "in.position.xy"),
        entry("color",                    "outFragColor"),
        entry("jsl_vertexColor",          "in.fragColor"),
        // The uniform variables are combined into a struct. These structs are generated while
        // parsing the jsl shader files and are added to the header files in mtl-headers directory.
        // Each fragment function receives a pointer variable named uniforms(to respective struct of Uniforms)
        // hence all the following uniform variable names must be replaced by uniforms.var_name.
        entry("weights",                  "uniforms.weights"),
        entry("kvals",                    "uniforms.kvals"),
        entry("opacity",                  "uniforms.opacity"),
        entry("offset",                   "uniforms.offset"),
        entry("shadowColor",              "uniforms.shadowColor"),
        entry("surfaceScale",             "uniforms.surfaceScale"),
        entry("level",                    "uniforms.level"),
        entry("sampletx",                 "uniforms.sampletx"),
        entry("wrap",                     "uniforms.wrap"),
        entry("imagetx",                  "uniforms.imagetx"),
        entry("contrast",                 "uniforms.contrast"),
        entry("hue",                      "uniforms.hue"),
        entry("saturation",               "uniforms.saturation"),
        entry("brightness",               "uniforms.brightness"),
        entry("tx0",                      "uniforms.tx0"),
        entry("tx1",                      "uniforms.tx1"),
        entry("tx2",                      "uniforms.tx2"),
        entry("threshold",                "uniforms.threshold"),
        entry("lightPosition",            "uniforms.lightPosition"),
        entry("lightColor",               "uniforms.lightColor"),
        entry("diffuseConstant",          "uniforms.diffuseConstant"),
        entry("specularConstant",         "uniforms.specularConstant"),
        entry("specularExponent",         "uniforms.specularExponent"),
        entry("lightSpecularExponent",    "uniforms.lightSpecularExponent"),
        entry("normalizedLightPosition",  "uniforms.normalizedLightPosition"),
        entry("normalizedLightDirection", "uniforms.normalizedLightDirection"),
        entry("fractions",                "uniforms.fractions"),
        entry("oinvarcradii",             "uniforms.oinvarcradii"),
        entry("iinvarcradii",             "uniforms.iinvarcradii"),
        entry("precalc",                  "uniforms.precalc"),
        entry("m0",                       "uniforms.m0"),
        entry("m1",                       "uniforms.m1"),
        entry("perspVec",                 "uniforms.perspVec"),
        entry("gradParams",               "uniforms.gradParams"),
        entry("idim",                     "uniforms.idim"),
        entry("gamma",                    "uniforms.gamma"),
        entry("xParams",                  "uniforms.xParams"),
        entry("yParams",                  "uniforms.yParams"),
        entry("lumaAlphaScale",           "uniforms.lumaAlphaScale"),
        entry("cbCrScale",                "uniforms.cbCrScale"),
        entry("innerOffset",              "uniforms.innerOffset"),
        entry("content",                  "uniforms.content"),
        entry("img",                      "uniforms.img"),
        entry("botImg",                   "uniforms.botImg"),
        entry("topImg",                   "uniforms.topImg"),
        entry("bumpImg",                  "uniforms.bumpImg"),
        entry("origImg",                  "uniforms.origImg"),
        entry("baseImg",                  "uniforms.baseImg"),
        entry("mapImg",                   "uniforms.mapImg"),
        entry("colors",                   "uniforms.colors"),
        entry("maskInput",                "uniforms.maskInput"),
        entry("glyphColor",               "uniforms.glyphColor"),
        entry("dstColor",                 "uniforms.dstColor"),
        entry("maskTex",                  "uniforms.maskTex"),
        entry("imageTex",                 "uniforms.imageTex"),
        entry("inputTex",                 "uniforms.inputTex"),
        entry("alphaTex",                 "uniforms.alphaTex"),
        entry("cbTex",                    "uniforms.cbTex"),
        entry("crTex",                    "uniforms.crTex"),
        entry("lumaTex",                  "uniforms.lumaTex"),
        entry("inputTex0",                "uniforms.inputTex0"),
        entry("inputTex1",                "uniforms.inputTex1")
    );

    private static final Map<String, String> FUNC_MAP = Map.of(
        "sample",  "sampleTex",
        "ddx",     "dfdx",
        "ddy",     "dfdy",
        "intcast", "int");

    public MSLBackend(JSLParser parser, JSLVisitor visitor) {
        super(parser, visitor);
    }

    @Override
    protected String getQualifier(Qualifier q) {
        String qualifier = q.toString();
        return QUAL_MAP.getOrDefault(qualifier, qualifier);
    }

    @Override
    protected String getType(Type t) {
        String type = t.toString();
        return TYPE_MAP.getOrDefault(type, type);
    }

    @Override
    protected String getVar(String v) {
        return VAR_MAP.getOrDefault(v, v);
    }

    @Override
    protected String getFuncName(String f) {
        return FUNC_MAP.getOrDefault(f, f);
    }

    @Override
    protected String getPrecision(Precision p) {
        return p.name();
    }

    @Override
    public void visitCallExpr(CallExpr e) {
        output(getFuncName(e.getFunction().getName()) + "(");
        boolean first = true;
        for (Expr param : e.getParams()) {
            if (first) {
                // For every user defined function, pass reference to 4 samplers and
                // reference to the uniforms struct.
                if (!CoreSymbols.getFunctions().contains(e.getFunction())) {
                    output("sampler0, sampler1, sampler2, sampler3, uniforms, ");
                }
                first = false;
            } else {
                output(", ");
            }
            scan(param);
        }
        output(")");
    }

    @Override
    public void visitFuncDef(FuncDef d) {
        Function func = d.getFunction();
        helperFunctions.add(func.getName());
        output(getType(func.getReturnType()) + " " + func.getName() + "(");
        boolean first = true;
        for (Param param : func.getParams()) {
            if (first) {
                // Add 4 sampler variables and "device <Uniforms>& uniforms" as the parameter to all user defined functions.
                if (!CoreSymbols.getFunctions().contains(d.getFunction())) {
                    output("sampler sampler0, sampler sampler1, sampler sampler2, sampler sampler3, device " + uniformStructName + "& uniforms,\n");
                }
                first = false;
            } else {
                output(", ");
            }
            output(getType(param.getType()) + " " + param.getName());
        }
        output(") ");
        scan(d.getStmt());
    }

    @Override
    public void visitVarDecl(VarDecl d) {
        Variable var = d.getVariable();
        Qualifier qual = var.getQualifier();
        switch (qual) {
            case Qualifier.CONST -> {
                // example: const int i = 10;
                // const variables are converted into macro.
                // reason: In MSL, only the program scoped variables can be declared as constant(address space).
                // Function scope variables cannot be declared as constant.
                // In our shaders, there is one function scope variable 'const float third'
                // which causes a compilation error if all const are replaced with constant.
                // So alternate approach is to use macros for all const variables.
                output("#define " + var.getName());
                output(" (");
                scan(d.getInit());
                output(")\n");
            }
            case Qualifier.PARAM -> {
                // These are uniform variables.
                // In MSL, uniform variables can be declared by using function_constant attribute.
                // function_constant variables can only be scalar or vector type.
                // User defined type or array of scalar or vector cannot be declared as function_constants.
                // So we combine all uniform variables into a struct named Uniforms.
                String aUniform = "";
                Precision precision = var.getPrecision();
                if (precision != null) {
                    String precisionStr = getPrecision(precision);
                    if (precisionStr != null) {
                        aUniform += precisionStr + " ";
                    }
                }
                if (getType(var.getType()).contains("texture2d")) {
                    hasTextureVar = true;
                    texSamplerMap.put(var.getName(), "sampler" + texSamplerMap.size());
                }
                uniformNames.add(var.getName());
                aUniform += getType(var.getType()) + " " + var.getName();
                if (var.isArray()) {
                    aUniform += "[" + var.getArraySize() + "]";
                }

                if (!uniformIDs.contains(var.getName())) {
                    uniformIDs += "    " + shaderFunctionName + "_" + var.getName() + "_ID = " + uniformIDCount + ",\n";
                    if (var.isArray()) {
                        uniformIDCount += var.getArraySize();
                    } else {
                        uniformIDCount++;
                    }
                }
                if (!uniformsForShaderFile.contains(var.getName())) {
                    uniformsForShaderFile += "    " + aUniform + ";\n";
                }
                if (!uniformsForObjCFiles.contains(var.getName())) {
                    uniformsForObjCFiles += "    " + aUniform + ";\n";
                }
            }
            case null -> super.visitVarDecl(d);
        }
    }

    @Override
    public void visitDiscardStmt(DiscardStmt s) {
        output(" discard_fragment();\n");
    }

    private void updateCommonHeaders() {
        String shaderType = isPrismShader ? "PRISM" : "DECORA";

        try (FileWriter objCHeaderFile = new FileWriter(headerFilesDir + objCHeaderFileName)) {

            if (!hasTextureVar) {
                String unusedUniform = "UNUSED";
                uniformNames.add(unusedUniform);
                uniformsForObjCFiles  += "    texture2d<float> " + unusedUniform + ";\n";
                uniformIDs += "    " + shaderFunctionName + "_" + unusedUniform + "_ID = " + uniformIDCount + ",\n";
                uniformIDCount++;
            }

            uniformsForObjCFiles = uniformsForObjCFiles.replace("texture2d<float>", "id<MTLTexture>");
            uniformsForObjCFiles = uniformsForObjCFiles.replace(" float2", " packed_float2");
            uniformsForObjCFiles = uniformsForObjCFiles.replace(" float3", " vector_float3");
            uniformsForObjCFiles = uniformsForObjCFiles.replace(" float4", " packed_float4");

            if (objCHeader.length() == 0) {
                objCHeader.append("#ifndef " + shaderType + "_SHADER_COMMON_H\n" +
                                "#define " + shaderType + "_SHADER_COMMON_H\n\n" +
                                "#import <Metal/Metal.h>\n" +
                                "#import <simd/simd.h>\n\n" +
                                "#ifdef MSL_BACKEND_VERBOSE\n" +
                                "#define MSL_LOG NSLog\n" +
                                "#else\n" +
                                "#define MSL_LOG(...)\n" +
                                "#endif\n\n" +
                                "typedef struct " + shaderType + "_VS_INPUT {\n" +
                                "    packed_float2 position;\n" +
                                "    packed_float4 color;\n" +
                                "    packed_float2 texCoord0;\n" +
                                "    packed_float2 texCoord1;\n" +
                                "} " + shaderType + "_VS_INPUT;" +
                                "\n\n");
            }

            if (uniformIDs != "") {
                objCHeader.append("typedef enum " + uniformIDsEnumName +
                    " {\n" + uniformIDs + "\n} " + shaderFunctionName + "ArgumentBufferID;\n\n");

                objCHeader.append("typedef struct " + uniformStructName + " {\n"
                    + uniformsForObjCFiles + "} " + uniformStructName + ";\n\n");

                objCHeader.append("NSDictionary* get" + shaderFunctionName + "_Uniform_VarID_Dict() {\n");
                objCHeader.append("    id ids[] = {\n");
                for (String aUniformName : uniformNames) {
                    objCHeader.append("        [NSNumber numberWithInt:" + shaderFunctionName + "_" +
                        aUniformName + "_ID" + "],\n");
                }
                objCHeader.append("    };\n\n");
                objCHeader.append("    NSUInteger count = sizeof(ids) / sizeof(id);\n");
                objCHeader.append("    NSArray *idArray = [NSArray arrayWithObjects:ids count:count];\n");

                objCHeader.append("    id uniforms[] = {\n");
                for (String aUniformName : uniformNames) {
                    objCHeader.append("        @\"" + aUniformName + "\",\n");
                }
                objCHeader.append("    };\n\n");
                objCHeader.append("    NSArray *uniformArray = [NSArray arrayWithObjects:uniforms count:count];\n");
                objCHeader.append("    return [NSDictionary dictionaryWithObjects:idArray forKeys:uniformArray];\n");
                objCHeader.append("}\n\n\n");
            } else {
                objCHeader.append("NSDictionary* get" + shaderFunctionName + "_Uniform_VarID_Dict() {\n");
                objCHeader.append("    return nil;\n");
                objCHeader.append("}\n\n\n");
            }

            objCHeaderFile.write(objCHeader.toString());

            StringBuilder getShaderDictFunc = new StringBuilder();
            getShaderDictFunc.append("NSDictionary* get" + shaderType + "Dict(NSString* inShaderName) {\n");
            getShaderDictFunc.append("    MSL_LOG(@\"get" + shaderType + "Dict \");\n");
            for (String aShaderName : shaderFunctionNameList) {
                getShaderDictFunc.append("    if ([inShaderName isEqualToString:@\"" + aShaderName + "\"]) {\n");
                getShaderDictFunc.append("        MSL_LOG(@\"get" + shaderType + "Dict() : calling -> get" + aShaderName + "_Uniform_VarID_Dict()\");\n");
                getShaderDictFunc.append("        return get" + aShaderName + "_Uniform_VarID_Dict();\n");
                getShaderDictFunc.append("    }\n");
            }
            getShaderDictFunc.append("    return nil;\n");
            getShaderDictFunc.append("};\n\n");
            objCHeaderFile.write(getShaderDictFunc.toString());

            objCHeaderFile.write("#endif\n");
        } catch (IOException e) {
            System.err.println("An error occurred.");
            e.printStackTrace();
        }
    }

    @Override
    protected String getHeader() {
        StringBuilder header = new StringBuilder();

        header.append("#include \"" + FRAGMENT_SHADER_HEADER_FILE_NAME + "\"\n\n");

        if (!hasTextureVar) {
            String unusedUniform = "UNUSED";
            uniformsForShaderFile += "    texture2d<float> " + unusedUniform + ";\n";
        }

        uniformsForShaderFile = uniformsForShaderFile.replace(" float2", " vector_float2");
        uniformsForShaderFile = uniformsForShaderFile.replace(" float3", " vector_float3");
        uniformsForShaderFile = uniformsForShaderFile.replace(" float4", " vector_float4");
        header.append("typedef struct " + uniformStructName + " {\n" + uniformsForShaderFile + "} " + uniformStructName + ";\n\n");

        if (hasTextureVar) {
            header.append("float4 " + sampleTexFuncName + "(sampler textureSampler, texture2d<float> colorTexture, float2 texCoord) {\n");
            header.append("    return colorTexture.sample(textureSampler, texCoord);\n");
            header.append("}\n\n");
        }

        return header.toString();
    }

    @Override
    public String getShader() {
        String shader = super.getShader();
        updateCommonHeaders();
        String fragmentFunctionDef = "\n[[fragment]] float4 " + shaderFunctionName + "(VS_OUTPUT in [[ stage_in ]],";
        fragmentFunctionDef += "\n    device " + uniformStructName + "& uniforms [[ buffer(0) ]]";
        for (int i = 0; i < texSamplerMap.size(); i++) {
            fragmentFunctionDef += ",\n    sampler sampler"+i+" [[ sampler("+i+") ]]";
        }
        fragmentFunctionDef += ") {";

        fragmentFunctionDef += "\n\nfloat4 outFragColor;";

        shader = shader.replace(MAIN, fragmentFunctionDef);

        int indexOfClosingBraceOfMain = shader.lastIndexOf('}');
        shader = shader.substring(0, indexOfClosingBraceOfMain) + "return outFragColor;\n\n" +
                shader.substring(indexOfClosingBraceOfMain, shader.length());

        for (String helperFunction : helperFunctions) {
            shader = shader.replaceAll("\\b" + helperFunction + "\\b", shaderFunctionName + "_" + helperFunction);
        }
        if (hasTextureVar) {
            shader = shader.replaceAll("\\bsampleTex\\b", sampleTexFuncName);
            for (Map.Entry<String,String> entry : texSamplerMap.entrySet()) {
                shader = shader.replaceAll("\\b" + sampleTexFuncName + "\\(uniforms." + entry.getKey() + "\\b",
                    sampleTexFuncName + "(" + entry.getValue() + ", uniforms." + entry.getKey());
            }
        }
        // Remove the un-required samplers out of the 4 samplers added to all user defined functions
        for (int i = texSamplerMap.size(); i < 4; i++) {
            shader = shader.replaceAll("sampler sampler" + i + ", ", "");
            shader = shader.replaceAll("sampler" + i + ", ", "");
        }

        return shader;
    }

    public void setShaderNameAndHeaderPath(String name, String genMetalShaderPath) {
        shaderFunctionName = name;
        shaderFunctionNameList.add(shaderFunctionName);
        if (headerFilesDir == null) {
            headerFilesDir = genMetalShaderPath.substring(0, genMetalShaderPath.indexOf("jsl-"));
            headerFilesDir += MTL_HEADERS_DIR;
            writeFragmentShaderHeader();
        }
        isPrismShader = genMetalShaderPath.contains("jsl-prism");
        resetVariables();
    }

    private void writeFragmentShaderHeader() {
        String fragmentShaderHeader = """
        #ifndef FRAGMENT_COMMON_H
        #define FRAGMENT_COMMON_H

        #pragma clang diagnostic ignored "-Wunused"

        #include <simd/simd.h>
        #include <metal_stdlib>

        using namespace metal;

        struct VS_OUTPUT {
            float4 position [[ position ]];
            float4 fragColor;
            float2 texCoord0;
            float2 texCoord1;
        };

        #endif
        """;

        try (FileWriter fragmentShaderHeaderFile = new FileWriter(headerFilesDir + FRAGMENT_SHADER_HEADER_FILE_NAME)) {
            fragmentShaderHeaderFile.write(fragmentShaderHeader);
        } catch (IOException e) {
            System.err.println("IOException occurred while creating " + FRAGMENT_SHADER_HEADER_FILE_NAME +
                ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void resetVariables() {
        uniformStructName = shaderFunctionName + "_Uniforms";
        uniformIDsEnumName = shaderFunctionName + "_ArgumentBufferID";
        sampleTexFuncName = shaderFunctionName + "_SampleTexture";

        texSamplerMap.clear();
        helperFunctions.clear();
        uniformNames.clear();
        uniformsForShaderFile = "";
        uniformsForObjCFiles = "";
        uniformIDs = "";
        uniformIDCount = 0;

        // MTLArguemntEncoder requires the argument struct buffer to contain atleast one variable
        // of type: buffers, textures, samplers, or any element with the [[id]] attribute’
        // We have some(22) prism shaders for which the Uniform struct is either empty or contains only float2's
        // That causes MTL_SHADER_VALIDATION to fail and causes run time crash on Ventura.
        // So we add a variable texture2d<float> UNUSED; to each shader which does not
        // already have a texture variable.
        hasTextureVar = false;

        objCHeaderFileName = isPrismShader ? PRISM_SHADER_HEADER_FILE_NAME :
                                                DECORA_SHADER_HEADER_FILE_NAME;
    }
}
