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

package com.sun.scenario.effect.compiler.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static com.sun.scenario.effect.compiler.model.Precision.*;
import static com.sun.scenario.effect.compiler.model.Type.*;

/**
 * Maintains the sets of core (built-in) functions and variables.
 */
public class CoreSymbols {

    private static Set<Variable> vars = new HashSet<Variable>();
    private static Set<Function> funcs = new HashSet<Function>();

    static Set<Variable> getAllVariables() {
        return vars;
    }
    
    static Set<Function> getAllFunctions() {
        return funcs;
    }
    
    public static Function getFunction(String name, List<Type> ptypes) {
        return SymbolTable.getFunctionForSignature(funcs, name, ptypes);
    }
    
    static {
        // pos0/1, pixcoord, and jsl_vertexColor are declared "const"
        // (read-only) to prevent accidental assignment
        // TODO: should probably add "jsl_" prefix to all of these to make
        // it clear that they are special variables...
        declareVariable("pos0",            FLOAT2, null, true);
        declareVariable("pos1",            FLOAT2, null, true);
        declareVariable("pixcoord",        FLOAT2, null, true);
        declareVariable("jsl_vertexColor", FLOAT4, LOWP, true);
        declareVariable("color",           FLOAT4, LOWP, false);
        
        // float4 sample(sampler s, float2 loc)
        declareFunction(FLOAT4, "sample", SAMPLER, "s", FLOAT2, "loc");

        // float4 sample(lsampler s, float2 loc)
        declareFunction(FLOAT4, "sample", LSAMPLER, "s", FLOAT2, "loc");

        // float4 sample(fsampler s, float2 loc)
        declareFunction(FLOAT4, "sample", FSAMPLER, "s", FLOAT2, "loc");

        // int intcast(float x)
        declareFunction(INT, "intcast", FLOAT, "x");

        // bool any(<btype> x)
        // GLSL only supports: bool any(<btype N> x); where N := 2, 3, 4
        // HLSL supports: bool any(<type> x)
        declareOverloadsBool("any");

        // <ftype> min(<ftype> x, <ftype> y)
        // <ftype> min(<ftype> x, float y)
        declareOverloadsMinMax("min");

        // <ftype> max(<ftype> x, <ftype> y)
        // <ftype> max(<ftype> x, float y)
        declareOverloadsMinMax("max");

        // <ftype> clamp(<ftype> val, <ftype> min, <ftype> max)
        // <ftype> clamp(<ftype> val, float min, float max)
        declareOverloadsClamp();

        // <ftype> smoothstep(<ftype> min, <ftype> max, <ftype> val)
        // <ftype> smoothstep(float min, float max, <ftype> val)
        declareOverloadsSmoothstep();

        // <ftype> abs(<ftype> x)
        declareOverloadsSimple("abs");

        // <ftype> floor(<ftype> x)
        declareOverloadsSimple("floor");

        // <ftype> ceil(<ftype> x)
        declareOverloadsSimple("ceil");

        // <ftype> fract(<ftype> x)
        declareOverloadsSimple("fract");

        // <ftype> sign(<ftype> x)
        declareOverloadsSimple("sign");

        // <ftype> sqrt(<ftype> x)
        declareOverloadsSimple("sqrt");

        // <ftype> sin(<ftype> x)
        declareOverloadsSimple("sin");

        // <ftype> cos(<ftype> x)
        declareOverloadsSimple("cos");

        // <ftype> tan(<ftype> x)
        declareOverloadsSimple("tan");

        // <ftype> pow(<ftype> x, <ftype> y)
        declareOverloadsSimple2("pow");

        // <ftype> mod(<ftype> x, <ftype> y)
        // <ftype> mod(<ftype> x, float y)
        declareOverloadsMinMax("mod");

        // float dot(<ftype> x, <ftype> y)
        declareOverloadsFloat2("dot");

        // float distance(<ftype> x, <ftype> y)
        declareOverloadsFloat2("distance");

        // float length(<ftype> x)
        declareOverloadsFloat("length");

        // <ftype> mix(<ftype> x, <ftype> y, <ftype> a)
        // <ftype> mix(<ftype> x, <ftype> y, float a)
        declareOverloadsMix();
        
        // <ftype> normalize(<ftype> x)
        declareOverloadsSimple("normalize");

        // <ftype> ddx(<ftype> p)
        declareOverloadsSimple("ddx");

        // <ftype> ddy(<ftype> p)
        declareOverloadsSimple("ddy");
    }

    private static void declareVariable(String name, Type type,
                                        Precision precision,
                                        boolean readonly)
    {
        Qualifier qual = readonly ? Qualifier.CONST : null;
        vars.add(new Variable(name, type, qual, precision, -1, -1, null, false));
    }
    
    private static void declareFunction(Type returnType,
                                        String name,
                                        Object... params)
    {
        List<Param> paramList = new ArrayList<Param>();
        if (params.length % 2 != 0) {
            throw new InternalError("Params array length must be even");
        }
        for (int i = 0; i < params.length; i+=2) {
            if (!(params[i+0] instanceof Type) ||
                !(params[i+1] instanceof String))
            {
                throw new InternalError("Params must be specified as (Type,String) pairs");
            }
            paramList.add(new Param((String)params[i+1], (Type)params[i]));
        }
        funcs.add(new Function(name, returnType, paramList));
    }

    private static void declareOverloadsSimple(String name) {
        declareFunction(FLOAT,  name, FLOAT,  "x");
        declareFunction(FLOAT2, name, FLOAT2, "x");
        declareFunction(FLOAT3, name, FLOAT3, "x");
        declareFunction(FLOAT4, name, FLOAT4, "x");
    }

    private static void declareOverloadsSimple2(String name) {
        declareFunction(FLOAT,  name, FLOAT,  "x", FLOAT,  "y");
        declareFunction(FLOAT2, name, FLOAT2, "x", FLOAT2, "y");
        declareFunction(FLOAT3, name, FLOAT3, "x", FLOAT3, "y");
        declareFunction(FLOAT4, name, FLOAT4, "x", FLOAT4, "y");
    }

    private static void declareOverloadsMinMax(String name) {
        declareFunction(FLOAT,  name, FLOAT,  "x", FLOAT,  "y");
        declareFunction(FLOAT2, name, FLOAT2, "x", FLOAT2, "y");
        declareFunction(FLOAT3, name, FLOAT3, "x", FLOAT3, "y");
        declareFunction(FLOAT4, name, FLOAT4, "x", FLOAT4, "y");
        declareFunction(FLOAT2, name, FLOAT2, "x", FLOAT,  "y");
        declareFunction(FLOAT3, name, FLOAT3, "x", FLOAT,  "y");
        declareFunction(FLOAT4, name, FLOAT4, "x", FLOAT,  "y");
    }

    private static void declareOverloadsClamp() {
        final String name = "clamp";
        declareFunction(FLOAT,  name, FLOAT,  "val", FLOAT,  "min", FLOAT,  "max");
        declareFunction(FLOAT2, name, FLOAT2, "val", FLOAT2, "min", FLOAT2, "max");
        declareFunction(FLOAT3, name, FLOAT3, "val", FLOAT3, "min", FLOAT3, "max");
        declareFunction(FLOAT4, name, FLOAT4, "val", FLOAT4, "min", FLOAT4, "max");
        declareFunction(FLOAT2, name, FLOAT2, "val", FLOAT,  "min", FLOAT,  "max");
        declareFunction(FLOAT3, name, FLOAT3, "val", FLOAT,  "min", FLOAT,  "max");
        declareFunction(FLOAT4, name, FLOAT4, "val", FLOAT,  "min", FLOAT,  "max");
    }

    private static void declareOverloadsSmoothstep() {
        final String name = "smoothstep";
        declareFunction(FLOAT,  name, FLOAT,  "min", FLOAT,  "max", FLOAT,   "val");
        declareFunction(FLOAT2, name, FLOAT2, "min", FLOAT2, "max", FLOAT2,  "val");
        declareFunction(FLOAT3, name, FLOAT3, "min", FLOAT3, "max", FLOAT3,  "val");
        declareFunction(FLOAT4, name, FLOAT4, "min", FLOAT4, "max", FLOAT4,  "val");
        declareFunction(FLOAT2, name, FLOAT,  "min", FLOAT,  "max", FLOAT2,  "val");
        declareFunction(FLOAT3, name, FLOAT,  "min", FLOAT,  "max", FLOAT3,  "val");
        declareFunction(FLOAT4, name, FLOAT,  "min", FLOAT,  "max", FLOAT4,  "val");
    }

    private static void declareOverloadsMix() {
        final String name = "mix";
        declareFunction(FLOAT,  name, FLOAT,  "x", FLOAT,  "y", FLOAT,  "a");
        declareFunction(FLOAT2, name, FLOAT2, "x", FLOAT2, "y", FLOAT2, "a");
        declareFunction(FLOAT3, name, FLOAT3, "x", FLOAT3, "y", FLOAT3, "a");
        declareFunction(FLOAT4, name, FLOAT4, "x", FLOAT4, "y", FLOAT4, "a");
        declareFunction(FLOAT2, name, FLOAT2, "x", FLOAT2, "y", FLOAT,  "a");
        declareFunction(FLOAT3, name, FLOAT3, "x", FLOAT3, "y", FLOAT,  "a");
        declareFunction(FLOAT4, name, FLOAT4, "x", FLOAT4, "y", FLOAT,  "a");
    }

    private static void declareOverloadsBool(String name) {
        declareFunction(BOOL, name, BOOL2, "x");
        declareFunction(BOOL, name, BOOL3, "x");
        declareFunction(BOOL, name, BOOL4, "x");
    }

    private static void declareOverloadsFloat(String name) {
        declareFunction(FLOAT, name, FLOAT,  "x");
        declareFunction(FLOAT, name, FLOAT2, "x");
        declareFunction(FLOAT, name, FLOAT3, "x");
        declareFunction(FLOAT, name, FLOAT4, "x");
    }

    private static void declareOverloadsFloat2(String name) {
        declareFunction(FLOAT, name, FLOAT,  "x", FLOAT,  "y");
        declareFunction(FLOAT, name, FLOAT2, "x", FLOAT2, "y");
        declareFunction(FLOAT, name, FLOAT3, "x", FLOAT3, "y");
        declareFunction(FLOAT, name, FLOAT4, "x", FLOAT4, "y");
    }
}
