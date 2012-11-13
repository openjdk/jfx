/*
 * Copyright (c) 2008, 2012 Oracle and/or its affiliates. All rights reserved.
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
import com.sun.scenario.effect.compiler.model.BaseType;
import com.sun.scenario.effect.compiler.model.Function;
import com.sun.scenario.effect.compiler.model.Qualifier;
import com.sun.scenario.effect.compiler.model.Type;
import com.sun.scenario.effect.compiler.model.Variable;
import com.sun.scenario.effect.compiler.tree.Expr;
import com.sun.scenario.effect.compiler.tree.FuncDef;
import com.sun.scenario.effect.compiler.tree.ProgramUnit;
import com.sun.scenario.effect.compiler.tree.VarDecl;

/**
 */
public class HLSLBackend extends SLBackend {

    public HLSLBackend(JSLParser parser, ProgramUnit program) {
        super(parser, program);
    }

    private static final Map<String, String> qualMap = new HashMap<String, String>();
    static {
        qualMap.put("const", "");
        qualMap.put("param", "");
    }
    
    private static final Map<String, String> typeMap = new HashMap<String, String>();
    static {
        typeMap.put("void",    "void");
        typeMap.put("float",   "float");
        typeMap.put("float2",  "float2");
        typeMap.put("float3",  "float3");
        typeMap.put("float4",  "float4");
        typeMap.put("int",     "int");
        typeMap.put("int2",    "int2");
        typeMap.put("int3",    "int3");
        typeMap.put("int4",    "int4");
        typeMap.put("bool",    "bool");
        typeMap.put("bool2",   "bool2");
        typeMap.put("bool3",   "bool3");
        typeMap.put("bool4",   "bool4");
        typeMap.put("sampler", "sampler2D");
        typeMap.put("lsampler","sampler2D");
        typeMap.put("fsampler","sampler2D");
    }
    
    private static final Map<String, String> varMap = new HashMap<String, String>();
    static {
    }

    private static final Map<String, String> funcMap = new HashMap<String, String>();
    static {
        funcMap.put("sample", "tex2D");
        funcMap.put("fract", "frac");
        funcMap.put("mix", "lerp");
        funcMap.put("mod", "fmod");
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
    public void visitFuncDef(FuncDef d) {
        Function func = d.getFunction();
        if (func.getName().equals("main")) {
            output(getType(func.getReturnType()) + " " + func.getName() + "(");
            // TODO: it would be better if we scanned the whole JSL program
            // to see if pos0 or pos1 are used anywhere, but for now there
            // doesn't seem to be any harm in blindly declaring both here...
            for (int i = 0; i < 2; i++) {
                output("in float2 pos" + i + " : TEXCOORD" + i + ",\n");
            }
            // TODO: only need this if pixcoord is referenced somewhere
            // in the JSL program...
            output("in float2 pixcoord : VPOS,\n");
            output("in float4 jsl_vertexColor : COLOR0,\n");
            output("out float4 color : COLOR0");
            output(") ");
            scan(d.getStmt());
        } else {
            super.visitFuncDef(d);
        }
    }
    
    @Override
    public void visitVarDecl(VarDecl d) {
        Variable var = d.getVariable();
        Type type = var.getType();
        Qualifier qual = var.getQualifier();
        if (qual == Qualifier.PARAM && type.getBaseType() == BaseType.INT) {
            // TODO: It seems that constant integer registers have limitations
            // in SM 3.0... For example, the max number of integer registers
            // (those specified with i#) is 16; in PS 2.0 these were limited
            // to flow control instructions only, but according to MSDN this
            // restriction went away with PS 3.0.  However, bad things happen
            // at runtime if we output:
            //     int variableName : register(c0);
            // (not sure what the problem is, but bad values seem to be
            // uploaded if we use SetPixelShaderConstantI() in this case), and
            // if we use i# instead:
            //     int variableName : register(i0);
            // the compiler will say this is invalid (it won't complain if
            // we actually used it in a loop expression though).  Until this
            // problem is better understood, we can work around it by
            // declaring these params as float variants, e.g.:
            //     float variableName : register(c0);
            // and using SetPixelShaderConstantF() instead.
            String t;
            switch (type) {
            case INT:
                t = "float";
                break;
            case INT2:
                t = "float2";
                break;
            case INT3:
                t = "float3";
                break;
            case INT4:
                t = "float4";
                break;
            default:
                throw new InternalError();
            }
            output(t + " " + var.getName());
        } else if (qual == Qualifier.CONST) {
            // use #define-style definition
            output("#define " + var.getName());
        } else {
            output(getType(type) + " " + var.getName());
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
            char c = (type.getBaseType() == BaseType.SAMPLER) ? 's' : 'c';
            output(" : register(" + c + var.getReg() + ")");
        }
        if (qual == Qualifier.CONST) {
            // use #define-style definition (no closing ';')
            output("\n");
        } else {
            output(";\n");
        }
    }
}
