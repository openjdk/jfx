/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sun.scenario.effect.compiler.JSLParser;
import com.sun.scenario.effect.compiler.model.BaseType;
import com.sun.scenario.effect.compiler.model.Function;
import com.sun.scenario.effect.compiler.model.Qualifier;
import com.sun.scenario.effect.compiler.model.Type;
import com.sun.scenario.effect.compiler.model.Variable;
import com.sun.scenario.effect.compiler.tree.CallExpr;
import com.sun.scenario.effect.compiler.tree.CompoundStmt;
import com.sun.scenario.effect.compiler.tree.Expr;
import com.sun.scenario.effect.compiler.tree.FuncDef;
import com.sun.scenario.effect.compiler.tree.JSLVisitor;
import com.sun.scenario.effect.compiler.tree.Stmt;
import com.sun.scenario.effect.compiler.tree.VarDecl;

/**
 */
public class HLSL6Backend extends SLBackend {

    private enum ShaderType {
        Prism,
        Decora
    }

    private enum ResourceBindingType {
        CONSTANT_32BIT,  // float, int, bool
        CONSTANT_64BIT,  // float2, int2, bool2
        CONSTANT_96BIT,  // float3, int3, bool3
        CONSTANT_128BIT, // float4, int4, bool4
        TEXTURE,
        SAMPLER
    }

    private class ResourceBinding {
        String name;
        ResourceBindingType type;
        int slot;
        int count;

        public ResourceBinding(String name, ResourceBindingType type, int slot, int count) {
            this.name = name;
            this.type = type;
            this.slot = slot;
            this.count = count;
        }

        public int getTotal32BitSlotCount() {
            switch (type) {
            case ResourceBindingType.CONSTANT_32BIT:
                return count;
            case ResourceBindingType.CONSTANT_64BIT,
                 ResourceBindingType.TEXTURE,
                 ResourceBindingType.SAMPLER:
                return count * 2;
            case ResourceBindingType.CONSTANT_96BIT:
                return count * 3;
            case ResourceBindingType.CONSTANT_128BIT:
                return count * 4;
            default:
                return 0;
            }
        }
    }

    // defined by D3D12 requirements
    // maximum amount of available 32-bit slots inside the Root Signature
    private static final int MAX_ROOT_SIGNATURE_32BIT_SLOTS = 64;
    private static final String SHADER_HEADER_GEN_DIR = "d3d12-headers\\";
    private static final String SHADER_HEADER_FILE_NAME_SUFFIX = "_D3D12ShaderResourceDataHeader.hpp";
    private static final String DECORA_SHADER_HEADER_NAME = "Decora" + SHADER_HEADER_FILE_NAME_SUFFIX;
    private static final String PRISM_SHADER_HEADER_NAME = "Prism" + SHADER_HEADER_FILE_NAME_SUFFIX;
    private static final String COMMON_SHADER_HEADER_NAME = "Common" + SHADER_HEADER_FILE_NAME_SUFFIX;

    private static final String SHADER_RESOURCE_COLLECTIONS_END_LINE = "// end of ShaderResourceCollections\n";
    private static final String SHADERS_END_LINE = "}; // end of Shaders\n";
    private static final String SHADERS_RESOURCE_COLLECTION_REPLACEMENT_MARKER = "// THIS WILL BE REPLACED";

    private StringBuilder shaderHeader;
    private ShaderType shaderType;
    private String shaderName;
    private String shaderHeaderGenDirPath;
    private String shaderHeaderFilePath;
    private String commonShaderHeaderFilePath;

    // maps resource name to its binding
    // we will populate this data based on JSL shaders declarations and construct
    // a Root Signature afterwards
    private List<ResourceBinding> cbufferResources = new ArrayList<ResourceBinding>();
    private List<ResourceBinding> textureResources = new ArrayList<ResourceBinding>();
    private List<ResourceBinding> samplerResources = new ArrayList<ResourceBinding>();

    private ShaderType getShaderTypeFromDir(String shaderDir) {
        if (shaderDir.contains("jsl-prism")) return ShaderType.Prism;
        else if (shaderDir.contains("jsl-decora")) return ShaderType.Decora;
        else throw new RuntimeException("Cannot deduce shader type from directory");
    }

    private String getShaderHeaderName(ShaderType type) {
        switch (type) {
            case ShaderType.Prism: return PRISM_SHADER_HEADER_NAME;
            case ShaderType.Decora: return DECORA_SHADER_HEADER_NAME;
            default: return "UNKNOWN";
        }
    }

    public HLSL6Backend(JSLParser parser, JSLVisitor visitor, String shaderName, String shaderDir) {
        super(parser, visitor);
        this.shaderName = shaderName;
        this.shaderType = getShaderTypeFromDir(shaderDir);
        this.shaderHeaderGenDirPath = shaderDir.substring(0, shaderDir.indexOf("jsl-"));
        this.shaderHeaderGenDirPath += SHADER_HEADER_GEN_DIR;
        this.shaderHeaderFilePath = this.shaderHeaderGenDirPath + getShaderHeaderName(this.shaderType);
        this.commonShaderHeaderFilePath = this.shaderHeaderGenDirPath + COMMON_SHADER_HEADER_NAME;
    }

    private static final Map<String, String> QUAL_MAP = Map.of(
        "const", "",
        "param", "");

    private static final Map<String, String> TYPE_MAP = Map.ofEntries(
        Map.entry("void",     "void"),
        Map.entry("float",    "float"),
        Map.entry("float2",   "float2"),
        Map.entry("float3",   "float3"),
        Map.entry("float4",   "float4"),
        Map.entry("int",      "int"),
        Map.entry("int2",     "int2"),
        Map.entry("int3",     "int3"),
        Map.entry("int4",     "int4"),
        Map.entry("bool",     "bool"),
        Map.entry("bool2",    "bool2"),
        Map.entry("bool3",    "bool3"),
        Map.entry("bool4",    "bool4"),
        Map.entry("sampler",  "sampler"),
        Map.entry("lsampler", "sampler"),
        Map.entry("fsampler", "sampler"));

    private static final Map<String, String> VAR_MAP = Map.of();

    private static final Map<String, String> FUNC_MAP = Map.of(
        "fract",   "frac",
        "mix",     "lerp",
        "mod",     "fmod",
        "intcast", "int",
        "any",     "any",
        "length",  "length");

    private boolean mainJustDefined = false;

    @Override
    protected String getHeader() {
        return
        """
        #include "ShaderCommon.hlsl"
        """;
    }

    @Override
    protected String getType(Type t) {
        return TYPE_MAP.get(t.toString());
    }

    @Override
    protected String getQualifier(Qualifier q) {
        return QUAL_MAP.get(q.toString());
    }

    @Override
    protected String getVar(String v) {
        String s = VAR_MAP.get(v);
        return (s != null) ? s : v;
    }

    @Override
    protected String getFuncName(String f) {
        String s = FUNC_MAP.get(f);
        return (s != null) ? s : f;
    }

    @Override
    public void visitCallExpr(CallExpr e) {
        if (e.getFunction().getName().equals("sample")) {
            // Modern HLSL moved texture sampling/loading to object's call. Moreover,
            // SamplerState is now separated from Texture data. So, we want to replace
            // JSL's "sample(<texture>, ...)" call with:
            //   <texture>.Sample(<texture>_sampler, ...)
            boolean first = true;
            for (Expr param : e.getParams()) {
                if (first) {
                    first = false;
                    scan(param);
                    output(".Sample(");
                    scan(param);
                    output("_sampler");
                } else {
                    output(", ");
                    scan(param);
                }
            }
            output(")");
        } else if (e.getFunction().getName().equals("paint")) {
            // This is a bit hacky, but it prevents the DXC warning
            // regarding implicit truncation of vector type.
            // Ideally we would use the correct notation in the source file.
            output(getFuncName(e.getFunction().getName()) + "(");
            boolean first = true;
            for (Expr param : e.getParams()) {
                if (first) {
                    first = false;
                } else {
                    output(",");
                }

                if (param.getResultType() == Type.FLOAT) {
                    scan(param);
                    output(".x");
                } else if (param.getResultType() == Type.FLOAT2) {
                    scan(param);
                    output(".xy");
                } else if (param.getResultType() == Type.FLOAT3) {
                    scan(param);
                    output(".xyz");
                } else {
                    scan(param);
                }
            }
            output(")");
        } else {
            super.visitCallExpr(e);
        }
    }

    @Override
    public void visitCompoundStmt(CompoundStmt s) {
        if (mainJustDefined) {
            // take off the flag in case we encounter some other compound stmts in the process
            mainJustDefined = false;
            output("{\n");
            output("float4 color = float4(0.0, 0.0, 0.0, 0.0);\n");
            for (Stmt stmt : s.getStmts()) {
                scan(stmt);
            }
            output("return color;\n");
            output("}\n");
        } else {
            super.visitCompoundStmt(s);
        }
    }

    @Override
    public void visitFuncDef(FuncDef d) {
        Function func = d.getFunction();
        if (func.getName().equals("main")) {
            // generate main function
            output("[RootSignature(JFX_INTERNAL_GRAPHICS_RS)]\n");
            output("float4 " + func.getName() + "(");
            // TODO this used to be float2, but it has to be float4 now
            // Must replace all "pixcoord" uses with "pixcoord.xy" as this otherwise produces
            // implicit vector conversion warning.
            output("float4 pixcoord : SV_POSITION,\n");
            output("float4 jsl_vertexColor : COLOR0,\n");
            output("float2 pos0 : TEXCOORD0,\n");
            output("float2 pos1 : TEXCOORD1\n");
            output(") : SV_TARGET ");
            // Instead of relying on in/out parameters in the function arguments, HLSL6 expects
            // to return data via a return statement (so main() shader function is not void).
            // In case of pixel shaders main should return float4 for single-RT outputs and
            // an array of float4's for multi-RT (TODO: check if single-RT is the only case).
            // This flag leaves a trace for visitCompoundStmt to introduce return variable definition
            // and then allows us to inject "return color" at the end of it.
            mainJustDefined = true;
            scan(d.getStmt());
        } else {
            super.visitFuncDef(d);
        }
    }

    private void addResourceDeclaration(String name, ResourceBindingType type, int slot, int count) {
        switch (type) {
        case ResourceBindingType.CONSTANT_32BIT, ResourceBindingType.CONSTANT_64BIT,
             ResourceBindingType.CONSTANT_96BIT, ResourceBindingType.CONSTANT_128BIT ->
            cbufferResources.add(new ResourceBinding(name, type, slot, count));
        case ResourceBindingType.TEXTURE ->
            textureResources.add(new ResourceBinding(name, type, slot, count));
        case ResourceBindingType.SAMPLER ->
            samplerResources.add(new ResourceBinding(name, type, slot, count));
        }
    }

    @Override
    public void visitVarDecl(VarDecl d) {
        Variable var = d.getVariable();
        Type type = var.getType();
        Qualifier qual = var.getQualifier();
        if (qual == Qualifier.CONST) {
            // use #define-style definition
            output("#define " + var.getName());
        } else {
            if (type.getBaseType() == BaseType.SAMPLER) {
                // output sampler and texture here
                // we assume that the texture itself is the same name, and the sampler has "_sampler" suffix added
                // visitCallExpr above will use this assumption as well
                output("SamplerState " + var.getName() + "_sampler: register(s" + var.getReg() + ");\n");
                output("Texture2D " + var.getName() + ": register(t" + var.getReg() + ")");
                addResourceDeclaration(var.getName(), ResourceBindingType.TEXTURE, var.getReg(), 1);
                addResourceDeclaration(var.getName() + "_sampler", ResourceBindingType.SAMPLER, var.getReg(), 1);
            } else {
                output(getType(type) + " " + var.getName());
            }
        }
        Expr init = d.getInit();
        if (init != null) {
            if (qual == Qualifier.CONST) {
                // use #define-style definition (no '=', wrap in
                // parens for safety)
                output(" (");
                scan(init);
                output(")");
            } else {
                output(" = ");
                scan(init);
            }
        }
        if (var.isArray()) {
            output("[" + var.getArraySize() + "]");
        }
        if (qual == Qualifier.PARAM) {
            if (type.getBaseType() != BaseType.SAMPLER) {
                output(" : register(c" + var.getReg() + ")");

                ResourceBindingType bindingType = ResourceBindingType.CONSTANT_32BIT;
                switch (type.getNumFields()) {
                case 1 -> bindingType = ResourceBindingType.CONSTANT_32BIT;
                case 2 -> bindingType = ResourceBindingType.CONSTANT_64BIT;
                case 3 -> bindingType = ResourceBindingType.CONSTANT_96BIT;
                case 4 -> bindingType = ResourceBindingType.CONSTANT_128BIT;
                }
                addResourceDeclaration(var.getName(), bindingType, var.getReg(), var.isArray() ? var.getArraySize() : 1);
            }
        }
        if (qual == Qualifier.CONST) {
            // use #define-style definition (no closing ';')
            output("\n");
        } else {
            output(";\n");
        }
    }

    private void createCommonShaderHeaderFile(File f) {
        try (FileWriter fw = new FileWriter(f)) {
            // form standard introduction to the shader header file
            // defines all types, includes and such that we will use
            fw.write(
                """
                // AUTOGENERATED BY JSLC FOR D3D12 BACKEND

                #pragma once

                #include <vector>
                #include <map>

                namespace D3D12 {
                namespace JSLC {

                enum class ResourceBindingType: unsigned char
                {
                    CONSTANT_32BIT,  // float, int, bool
                    CONSTANT_64BIT,  // float2, int2, bool2
                    CONSTANT_96BIT,  // float3, int3, bool3
                    CONSTANT_128BIT, // float4, int4, bool4
                    TEXTURE,
                    SAMPLER
                };

                struct ResourceBinding
                {
                    const char* name;
                    ResourceBindingType type;
                    int slot;
                    int count;
                };

                class CStringComparer
                {
                public:
                    bool operator()(const char* const a, const char* const b) const
                    {
                        return std::strcmp(a, b) < 0;
                    }
                };

                using ShaderResourceCollection = std::vector<ResourceBinding>;
                using ShaderCollection = std::map<const char*, ShaderResourceCollection&, CStringComparer>;

                } // namespace JSLC
                } // namespace D3D12
                """);
        } catch (IOException e) {
            System.err.println("Failed to create common shader header file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createShaderHeaderFile(File f) {
        StringBuilder headerIntro = new StringBuilder();
        // form standard introduction to the shader header file
        // includes a common header with all types/defines/etc. needed
        headerIntro.append(
            """
            // AUTOGENERATED BY JSLC FOR D3D12 BACKEND

            #pragma once

            """)
            .append("#include \"").append(COMMON_SHADER_HEADER_NAME).append("\"\n")
            .append(
            """

            namespace D3D12 {
            namespace JSLC {

            """);

        // add maps with flag-comments which we will fill later on
        headerIntro
            .append(SHADER_RESOURCE_COLLECTIONS_END_LINE)
            .append('\n')
            .append("ShaderCollection ").append(this.shaderType.toString()).append("Shaders = {\n")
            .append(SHADERS_END_LINE);

        // close the file with necessary final bits
        headerIntro.append(
            """

            } // namespace JSLC
            } // namespace D3D12
            """);

        try {
            FileWriter fw = new FileWriter(f);
            fw.write(headerIntro.toString());
            fw.close();
        } catch (IOException e) {
            System.err.println("Failed to write shader header file intro: " + e.getMessage());
        }
    }

    private void initializeShaderHeaderFiles() {
        File common = new File(this.commonShaderHeaderFilePath);
        if (!common.exists()) {
            createCommonShaderHeaderFile(common);
        }
        File f = new File(this.shaderHeaderFilePath);
        if (!f.exists()) {
            createShaderHeaderFile(f);
        }
    }

    private void buildResourceBindingMapEntry(ResourceBinding binding, StringBuilder builder) {
        builder
            .append("    { \"")
            .append(binding.name)
            .append("\", ResourceBindingType::")
            .append(binding.type.toString())
            .append(", ")
            .append(binding.slot)
            .append(", ")
            .append(binding.count)
            .append(" },\n");
    }

    private void addResourceBindingMapToHeader(String resourceMap, int lineToReplace) {
        try {
            String headerContents = Files.readString(Path.of(this.shaderHeaderFilePath));

            if (lineToReplace < 0) {
                // resource binding entry was not present in the header file, append it at the end
                headerContents = headerContents.replace(
                    SHADER_RESOURCE_COLLECTIONS_END_LINE, resourceMap + "\n\n" + SHADER_RESOURCE_COLLECTIONS_END_LINE
                );
                headerContents = headerContents.replace(
                    SHADERS_END_LINE, "    { \"" + this.shaderName + "\", " + this.shaderName + this.shaderType.toString() + "ShaderResources },\n" + SHADERS_END_LINE
                );
            } else {
                // the entry was found someone and removed earlier by removeExistingResourceCollection
                // paste the contents in the line specified
                // note that we do NOT edit the main ShaderCollection map here, as the entry already existed
                List<String> headerLines = headerContents.lines().collect(Collectors.toList());
                if (!headerLines.contains(SHADERS_RESOURCE_COLLECTION_REPLACEMENT_MARKER)) {
                    throw new IOException("addResourceBindingMapToHeader: Expected to replace line " + lineToReplace + " but the marker is absent.");
                }

                headerLines.set(lineToReplace, resourceMap);
                headerContents = String.join("\n", headerLines);
            }
            FileWriter fw = new FileWriter(this.shaderHeaderFilePath);
            fw.write(headerContents);
            fw.close();
        } catch (IOException e) {
            System.err.println("Failed to add resource binding map to header: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int removeExistingCollection(String collectionName) {
        int collectionBegin = -1;

        try {
            List<String> headerLines = Files.readString(Path.of(this.shaderHeaderFilePath)).lines().collect(Collectors.toList());
            for (int i = 0; i < headerLines.size(); ++i) {
                if (collectionBegin < 0 && headerLines.get(i).contains("ShaderResourceCollection " + collectionName + " = {")) {
                    collectionBegin = i;
                    continue;
                } else if (collectionBegin >= 0 && headerLines.get(i).contentEquals("};")) {
                    // remove all lines from current line to the beginning
                    // this will eares the binding and have it ready for replacement later on
                    // deliberately skips the first collection line to replace it with a marker later on
                    for (int j = i; j > collectionBegin; --j) {
                        headerLines.remove(j);
                    }

                    // place a marker for addResourceBindingMapToHeader
                    headerLines.set(collectionBegin, SHADERS_RESOURCE_COLLECTION_REPLACEMENT_MARKER);

                    // write the file and exit
                    FileWriter fw = new FileWriter(this.shaderHeaderFilePath);
                    fw.write(String.join("\n", headerLines));
                    fw.close();
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to remove existing Shader Resource collection from header: " + e.getMessage());
            e.printStackTrace();
        }

        return collectionBegin;
    }

    private void appendShaderResourceDataToHeader() {
        initializeShaderHeaderFiles();

        String collectionName = shaderName + this.shaderType.toString() + "ShaderResources";
        int line = removeExistingCollection(collectionName);

        StringBuilder resourceMap = new StringBuilder();
        resourceMap.append("ShaderResourceCollection ").append(collectionName).append(" = {\n");

        for (ResourceBinding rb: cbufferResources) {
            buildResourceBindingMapEntry(rb, resourceMap);
        }
        for (ResourceBinding rb: textureResources) {
            buildResourceBindingMapEntry(rb, resourceMap);
        }
        for (ResourceBinding rb: samplerResources) {
            buildResourceBindingMapEntry(rb, resourceMap);
        }

        resourceMap.append("};");

        addResourceBindingMapToHeader(resourceMap.toString(), line);
    }

    @Override
    public String getShader() {
        String shader = super.getShader();
        appendShaderResourceDataToHeader();
        return shader;
    }
}
