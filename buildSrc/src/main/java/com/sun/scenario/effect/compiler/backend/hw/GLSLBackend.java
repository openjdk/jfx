/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.Map;
import com.sun.scenario.effect.compiler.JSLParser;
import com.sun.scenario.effect.compiler.model.Qualifier;
import com.sun.scenario.effect.compiler.model.Type;
import com.sun.scenario.effect.compiler.tree.ProgramUnit;

/**
 */
public class GLSLBackend extends SLBackend {

    public GLSLBackend(JSLParser parser, ProgramUnit program) {
        super(parser, program);
    }

    private static final Map<String, String> qualMap = new HashMap<String, String>();
    static {
        qualMap.put("const", "const");
        qualMap.put("param", "uniform");
    }
    
    private static final Map<String, String> typeMap = new HashMap<String, String>();
    static {
        typeMap.put("void",    "void");
        typeMap.put("float",   "float");
        typeMap.put("float2",  "vec2");
        typeMap.put("float3",  "vec3");
        typeMap.put("float4",  "vec4");
        typeMap.put("int",     "int");
        typeMap.put("int2",    "ivec2");
        typeMap.put("int3",    "ivec3");
        typeMap.put("int4",    "ivec4");
        typeMap.put("bool",    "bool");
        typeMap.put("bool2",   "bvec2");
        typeMap.put("bool3",   "bvec3");
        typeMap.put("bool4",   "bvec4");
        typeMap.put("sampler", "sampler2D");
        typeMap.put("lsampler","sampler2D");
        typeMap.put("fsampler","sampler2D");
    }
    
    private static final Map<String, String> varMap = new HashMap<String, String>();
    static {
        varMap.put("pos0", "gl_TexCoord[0].st");
        varMap.put("pos1", "gl_TexCoord[1].st");
        varMap.put("color", "gl_FragColor");
        varMap.put("jsl_vertexColor", "gl_Color");
    }

    private static final Map<String, String> funcMap = new HashMap<String, String>();
    static {
        funcMap.put("sample", "jsl_sample");
        funcMap.put("ddx", "dFdx");
        funcMap.put("ddy", "dFdy");
        funcMap.put("intcast", "int");
        funcMap.put("any", "any");
        funcMap.put("length", "length");
    }

    @Override
    protected String getType(Type t) {
        return typeMap.get(t.toString());
    }

    @Override
    protected String getQualifier(Qualifier q) {
        return qualMap.get(q.toString());
    }

    @Override
    protected String getVar(String v) {
        String s = varMap.get(v);
        return (s != null) ? s : v;
    }

    @Override
    protected String getFuncName(String f) {
        String s = funcMap.get(f);
        return (s != null) ? s : f;
    }

    @Override
    protected String getHeader() {
        StringBuilder sb = new StringBuilder();

        // output special pixcoord offset uniform variable declaration
        // at the top of the program
        // TODO: this should be included only if the program makes use
        // of the special pixcoord variable (it's wasteful otherwise)...
        sb.append("uniform float jsl_pixCoordYOffset;\n");
        sb.append("vec2 pixcoord = vec2(gl_FragCoord.x, jsl_pixCoordYOffset-gl_FragCoord.y);\n");
        
        // also output helper function that handles the y-flip
        // needed to account for OpenGL's lower-left origin
        // TODO: this is really gross, but the Java2D/RSL backend needs
        // the y-flip, while the Java2D/JOGL backend does not; so for now
        // we use this jsl_posValueYFlip uniform variable to control whether
        // to flip or not...
        sb.append("uniform vec2 jsl_posValueYFlip;\n");
        sb.append("vec4 jsl_sample(sampler2D img, vec2 pos) {\n");
        sb.append("    pos.y = (jsl_posValueYFlip.x - pos.y) * jsl_posValueYFlip.y;\n");
        sb.append("    return texture2D(img, pos);\n");
        sb.append("}\n");

        return sb.toString();
    }
}
