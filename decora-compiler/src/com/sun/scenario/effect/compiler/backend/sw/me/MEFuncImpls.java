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

package com.sun.scenario.effect.compiler.backend.sw.me;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.sun.scenario.effect.compiler.model.CoreSymbols;
import com.sun.scenario.effect.compiler.model.FuncImpl;
import com.sun.scenario.effect.compiler.model.Function;
import com.sun.scenario.effect.compiler.model.Type;
import com.sun.scenario.effect.compiler.tree.Expr;
import com.sun.scenario.effect.compiler.tree.VariableExpr;

import static com.sun.scenario.effect.compiler.backend.sw.me.MEBackend.*;
import static com.sun.scenario.effect.compiler.model.Type.*;

/**
 * Contains the C/fixed-point implementations for all core (built-in) functions.
 */
class MEFuncImpls {

    private static Map<Function, FuncImpl> funcs = new HashMap<Function, FuncImpl>();
    
    static FuncImpl get(Function func) {
        return funcs.get(func);
    }
    
    static {
        // float4 sample(sampler s, float2 loc)
        declareFunctionSample(SAMPLER);

        // float4 sample(lsampler s, float2 loc)
        declareFunctionSample(LSAMPLER);

        // float4 sample(fsampler s, float2 loc)
        declareFunctionSample(FSAMPLER);

        // int intcast(float x)
        declareFunctionIntCast();
        
        // <ftype> min(<ftype> x, <ftype> y)
        // <ftype> min(<ftype> x, float y)
        declareOverloadsMinMax("min", "((x_tmp$1 < y_tmp$2) ? x_tmp$1 : y_tmp$2)");

        // <ftype> max(<ftype> x, <ftype> y)
        // <ftype> max(<ftype> x, float y)
        declareOverloadsMinMax("max", "((x_tmp$1 > y_tmp$2) ? x_tmp$1 : y_tmp$2)");

        // <ftype> clamp(<ftype> val, <ftype> min, <ftype> max)
        // <ftype> clamp(<ftype> val, float min, float max)
        declareOverloadsClamp();

        // <ftype> smoothstep(<ftype> min, <ftype> max, <ftype> val)
        // <ftype> smoothstep(float min, float max, <ftype> val)
        declareOverloadsSmoothstep();

        // <ftype> abs(<ftype> x)
        declareOverloadsSimple("abs", "abs(x_tmp$1)");

        // <ftype> floor(<ftype> x)
        declareOverloadsSimple("floor", "floor(x_tmp$1)");

        // <ftype> ceil(<ftype> x)
        declareOverloadsSimple("ceil", "ceil(x_tmp$1)");

        // <ftype> fract(<ftype> x)
        declareOverloadsSimple("fract", "(x_tmp$1 - floor(x_tmp$1))");

        // <ftype> sign(<ftype> x)
        declareOverloadsSimple("sign", "((x_tmp$1 < 0.f) ? -1.f : (x_tmp$1 > 0.f) ? 1.f : 0.f)");

        // <ftype> sqrt(<ftype> x)
        declareOverloadsSimple("sqrt", "sqrt(x_tmp$1)");

        // <ftype> sin(<ftype> x)
        declareOverloadsSimple("sin", "sin(x_tmp$1)");

        // <ftype> cos(<ftype> x)
        declareOverloadsSimple("cos", "cos(x_tmp$1)");

        // <ftype> tan(<ftype> x)
        declareOverloadsSimple("tan", "tan(x_tmp$1)");

        // <ftype> pow(<ftype> x, <ftype> y)
        declareOverloadsSimple2("pow", "pow(x_tmp$1, y_tmp$2)");

        // <ftype> mod(<ftype> x, <ftype> y)
        // <ftype> mod(<ftype> x, float y)
        declareOverloadsMinMax("mod", "(x_tmp$1 % y_tmp$2)");

        // float dot(<ftype> x, <ftype> y)
        declareOverloadsDot();

        // float distance(<ftype> x, <ftype> y)
        declareOverloadsDistance();

        // <ftype> mix(<ftype> x, <ftype> y, <ftype> a)
        // <ftype> mix(<ftype> x, <ftype> y, float a)
        declareOverloadsMix();
        
        // <ftype> normalize(<ftype> x)
        declareOverloadsNormalize();

        // <ftype> ddx(<ftype> p)
        declareOverloadsSimple("ddx", "<ddx() not implemented for sw backends>");

        // <ftype> ddy(<ftype> p)
        declareOverloadsSimple("ddy", "<ddy() not implemented for sw backends>");
    }
    
    private static void declareFunction(FuncImpl impl,
                                        String name, Type... ptypes)
    {
        Function f = CoreSymbols.getFunction(name, Arrays.asList(ptypes));
        if (f == null) {
            throw new InternalError("Core function not found (have you declared the function in CoreSymbols?)");
        }
        funcs.put(f, impl);
    }
    
    /**
     * Used to declare sample function:
     *   float4 sample([l,f]sampler s, float2 loc)
     */
    private static void declareFunctionSample(final Type type) {
        FuncImpl fimpl = new FuncImpl() {
            @Override
            public String getPreamble(List<Expr> params) {
                String s = getSamplerName(params);
                // TODO: this bounds checking is way too costly...
                String p = getPosName(params);
                if (type == LSAMPLER) {
                    return
                        "lsample(" + s + ", loc_tmp_x, loc_tmp_y,\n" +
                        "        " + p + "w, " + p + "h, " + p + "scan,\n" +
                        "        " + s + "_vals);\n";
                } else if (type == FSAMPLER) {
                    return
                        "float *" + s + "_arr_tmp = NULL;\n" +
                        "int iloc_tmp = 0;\n" +
                        "if (loc_tmp_x >= 0 && loc_tmp_y >= 0) {\n" +
                        "    int iloc_tmp_x = (int)(loc_tmp_x*" + p + "w);\n" +
                        "    int iloc_tmp_y = (int)(loc_tmp_y*" + p + "h);\n" +
                        "    jboolean out =\n" +
                        "        iloc_tmp_x >= " + p + "w ||\n" +
                        "        iloc_tmp_y >= " + p + "h;\n" +
                        "    if (!out) {\n" +
                        "        "+ s + "_arr_tmp = " + s + ";\n" +
                        "        iloc_tmp = 4 * (iloc_tmp_y*" + p + "scan + iloc_tmp_x);\n" +
                        "    }\n" +
                        "}\n";
                } else {
                    return
                        "int " + s + "_tmp;\n" +
                        "if (loc_tmp_x >= 0 && loc_tmp_y >= 0) {\n" +
                        "    int iloc_tmp_x = (int)(loc_tmp_x*" + p + "w);\n" +
                        "    int iloc_tmp_y = (int)(loc_tmp_y*" + p + "h);\n" +
                        "    jboolean out =\n" +
                        "        iloc_tmp_x >= " + p + "w ||\n" +
                        "        iloc_tmp_y >= " + p + "h;\n" +
                        "    " + s + "_tmp = out ? 0 :\n" +
                        "        " + s + "[iloc_tmp_y*" + p + "scan + iloc_tmp_x];\n" +
                        "} else {\n" +
                        "    " + s + "_tmp = 0;\n" +
                        "}\n";
                }
            }
            public String toString(int i, List<Expr> params) {
                String s = getSamplerName(params);
                if (type == LSAMPLER) {
                    return (i < 0 || i > 3) ? null : s + "_vals[" + i + "]";
                } else if (type == FSAMPLER) {
                    String arr = s + "_arr_tmp";
                    switch (i) {
                    case 0:
                        return arr + " == NULL ? 0.f : " + arr + "[iloc_tmp]";
                    case 1:
                        return arr + " == NULL ? 0.f : " + arr + "[iloc_tmp+1]";
                    case 2:
                        return arr + " == NULL ? 0.f : " + arr + "[iloc_tmp+2]";
                    case 3:
                        return arr + " == NULL ? 0.f : " + arr + "[iloc_tmp+3]";
                    default:
                        return null;
                    }
                } else {
                    switch (i) {
                    case 0:
                        return "(((" + s + "_tmp >> 16) & 0xff) / 255.f)";
                    case 1:
                        return "(((" + s + "_tmp >>  8) & 0xff) / 255.f)";
                    case 2:
                        return "(((" + s + "_tmp      ) & 0xff) / 255.f)";
                    case 3:
                        return "(((" + s + "_tmp >> 24) & 0xff) / 255.f)";
                    default:
                        return null;
                    }
                }
            }
            private String getSamplerName(List<Expr> params) {
                VariableExpr e = (VariableExpr)params.get(0);
                return e.getVariable().getName();
            }
            private String getPosName(List<Expr> params) {
                VariableExpr e = (VariableExpr)params.get(0);
                return "src" + e.getVariable().getReg();
            }
        };
        declareFunction(fimpl, "sample", type, FLOAT2);
    }

    /**
     * Used to declare intcast function:
     *   int intcast(float x)
     */
    private static void declareFunctionIntCast() {
        FuncImpl fimpl = new FuncImpl() {
            public String toString(int i, List<Expr> params) {
                return "((int)x_tmp)";
            }
        };
        declareFunction(fimpl, "intcast", FLOAT);
    }

    /**
     * Used to declare simple functions of the following form: 
     *   <ftype> name(<ftype> x)
     */
    private static void declareOverloadsSimple(String name, final String pattern) {
        for (Type type : new Type[] {FLOAT, FLOAT2, FLOAT3, FLOAT4}) {
            final boolean useSuffix = (type != FLOAT);
            FuncImpl fimpl = new FuncImpl() {
                public String toString(int i, List<Expr> params) {
                    String sfx = useSuffix ? getSuffix(i) : "";
                    String s = pattern;
                    s = s.replace("$1", sfx);
                    return s;
                }
            };
            declareFunction(fimpl, name, type);
        }
    }
    
    /**
     * Used to declare simple two parameter functions of the following form:
     *   <ftype> name(<ftype> x, <ftype> y)
     */
    private static void declareOverloadsSimple2(String name, final String pattern) {
        for (Type type : new Type[] {FLOAT, FLOAT2, FLOAT3, FLOAT4}) {
            // declare (vectype,vectype) variants
            final boolean useSuffix = (type != FLOAT);
            FuncImpl fimpl = new FuncImpl() {
                public String toString(int i, List<Expr> params) {
                    String sfx = useSuffix ? getSuffix(i) : "";
                    String s = pattern;
                    s = s.replace("$1", sfx);
                    s = s.replace("$2", sfx);
                    return s;
                }
            };
            declareFunction(fimpl, name, type, type);
        }
    }

    /**
     * Used to declare normalize functions of the following form: 
     *   <ftype> normalize(<ftype> x)
     */
    private static void declareOverloadsNormalize() {
        final String name = "normalize";
        final String pattern = "x_tmp$1 / denom";
        for (Type type : new Type[] {FLOAT, FLOAT2, FLOAT3, FLOAT4}) {
            int n = type.getNumFields();
            final String preamble;
            if (n == 1) {
                preamble = "float denom = x_tmp;\n";
            } else {
                String     s  =    "(x_tmp_x * x_tmp_x)";
                           s += "+\n(x_tmp_y * x_tmp_y)";
                if (n > 2) s += "+\n(x_tmp_z * x_tmp_z)";
                if (n > 3) s += "+\n(x_tmp_w * x_tmp_w)";
                preamble = "float denom = sqrt(" + s + ");\n";
            }
            
            final boolean useSuffix = (type != FLOAT);
            FuncImpl fimpl = new FuncImpl() {
                @Override
                public String getPreamble(List<Expr> params) {
                    return preamble;
                }
                public String toString(int i, List<Expr> params) {
                    String sfx = useSuffix ? getSuffix(i) : "";
                    String s = pattern;
                    s = s.replace("$1", sfx);
                    return s;
                }
            };
            declareFunction(fimpl, name, type);
        }
    }

    /**
     * Used to declare dot functions of the following form: 
     *   float dot(<ftype> x, <ftype> y)
     */
    private static void declareOverloadsDot() {
        final String name = "dot";
        for (final Type type : new Type[] {FLOAT, FLOAT2, FLOAT3, FLOAT4}) {
            int n = type.getNumFields();
            String s;
            if (n == 1) {
                s = "(x_tmp * y_tmp)";
            } else {
                           s  =    "(x_tmp_x * y_tmp_x)";
                           s += "+\n(x_tmp_y * y_tmp_y)";
                if (n > 2) s += "+\n(x_tmp_z * y_tmp_z)";
                if (n > 3) s += "+\n(x_tmp_w * y_tmp_w)";
            }
            final String str = s;
            FuncImpl fimpl = new FuncImpl() {
                public String toString(int i, List<Expr> params) {
                    return str;
                }
            };
            declareFunction(fimpl, name, type, type);
        }
    }
    
    /**
     * Used to declare distance functions of the following form: 
     *   float distance(<ftype> x, <ftype> y)
     */
    private static void declareOverloadsDistance() {
        final String name = "distance";
        for (final Type type : new Type[] {FLOAT, FLOAT2, FLOAT3, FLOAT4}) {
            int n = type.getNumFields();
            String s;
            if (n == 1) {
                s = "(x_tmp - y_tmp) * (x_tmp - y_tmp)";
            } else {
                           s  =    "((x_tmp_x - y_tmp_x) * (x_tmp_x - y_tmp_x))";
                           s += "+\n((x_tmp_y - y_tmp_y) * (x_tmp_y - y_tmp_y))";
                if (n > 2) s += "+\n((x_tmp_z - y_tmp_z) * (x_tmp_z - y_tmp_z))";
                if (n > 3) s += "+\n((x_tmp_w - y_tmp_w) * (x_tmp_w - y_tmp_w))";
            }
            final String str = "sqrt(" + s + ")";
            FuncImpl fimpl = new FuncImpl() {
                public String toString(int i, List<Expr> params) {
                    return str;
                }
            };
            declareFunction(fimpl, name, type, type);
        }
    }
    
    /**
     * Used to declare min/max functions of the following form:
     *   <ftype> name(<ftype> x, <ftype> y)
     *   <ftype> name(<ftype> x, float y)
     * 
     * TODO: this is currently geared to simple functions like
     * min and max; we should make this more general...
     */
    private static void declareOverloadsMinMax(String name, final String pattern) {
        for (Type type : new Type[] {FLOAT, FLOAT2, FLOAT3, FLOAT4}) {
            // declare (vectype,vectype) variants
            final boolean useSuffix = (type != FLOAT);
            FuncImpl fimpl = new FuncImpl() {
                public String toString(int i, List<Expr> params) {
                    String sfx = useSuffix ? getSuffix(i) : "";
                    String s = pattern;
                    s = s.replace("$1", sfx);
                    s = s.replace("$2", sfx);
                    return s;
                }
            };
            declareFunction(fimpl, name, type, type);
            
            if (type == FLOAT) {
                continue;
            }
            
            // declare (vectype,float) variants
            fimpl = new FuncImpl() {
                public String toString(int i, List<Expr> params) {
                    String sfx = getSuffix(i);
                    String s = pattern;
                    s = s.replace("$1", sfx);
                    s = s.replace("$2", "");
                    return s;
                }
            };
            declareFunction(fimpl, name, type, FLOAT);
        }
    }

    /**
     * Used to declare clamp functions of the following form:
     *   <ftype> clamp(<ftype> val, <ftype> min, <ftype> max)
     *   <ftype> clamp(<ftype> val, float min, float max)
     */
    private static void declareOverloadsClamp() {
        final String name = "clamp";
        final String pattern =
            "(val_tmp$1 < min_tmp$2) ? min_tmp$2 : \n" +
            "(val_tmp$1 > max_tmp$2) ? max_tmp$2 : val_tmp$1";
        
        for (Type type : new Type[] {FLOAT, FLOAT2, FLOAT3, FLOAT4}) {
            // declare (vectype,vectype,vectype) variants
            final boolean useSuffix = (type != FLOAT);
            FuncImpl fimpl = new FuncImpl() {
                public String toString(int i, List<Expr> params) {
                    String sfx = useSuffix ? getSuffix(i) : "";
                    String s = pattern;
                    s = s.replace("$1", sfx);
                    s = s.replace("$2", sfx);
                    return s;
                }
            };
            declareFunction(fimpl, name, type, type, type);
            
            if (type == FLOAT) {
                continue;
            }
            
            // declare (vectype,float,float) variants
            fimpl = new FuncImpl() {
                public String toString(int i, List<Expr> params) {
                    String sfx = getSuffix(i);
                    String s = pattern;
                    s = s.replace("$1", sfx);
                    s = s.replace("$2", "");
                    return s;
                }
            };
            declareFunction(fimpl, name, type, FLOAT, FLOAT);
        }
    }

    /**
     * Used to declare smoothstep functions of the following form:
     *   <ftype> smoothstep(<ftype> min, <ftype> max, <ftype> val)
     *   <ftype> smoothstep(float min, float max, <ftype> val)
     */
    private static void declareOverloadsSmoothstep() {
        final String name = "smoothstep";
        // TODO - the smoothstep function is defined to use Hermite interpolation
        final String pattern =
            "(val_tmp$1 < min_tmp$2) ? 0.0f : \n" +
            "(val_tmp$1 > max_tmp$2) ? 1.0f : \n" +
            "(val_tmp$1 / (max_tmp$2 - min_tmp$2))";
        
        for (Type type : new Type[] {FLOAT, FLOAT2, FLOAT3, FLOAT4}) {
            // declare (vectype,vectype,vectype) variants
            final boolean useSuffix = (type != FLOAT);
            FuncImpl fimpl = new FuncImpl() {
                public String toString(int i, List<Expr> params) {
                    String sfx = useSuffix ? getSuffix(i) : "";
                    String s = pattern;
                    s = s.replace("$1", sfx);
                    s = s.replace("$2", sfx);
                    return s;
                }
            };
            declareFunction(fimpl, name, type, type, type);
            
            if (type == FLOAT) {
                continue;
            }
            
            // declare (float,float,vectype) variants
            fimpl = new FuncImpl() {
                public String toString(int i, List<Expr> params) {
                    String sfx = getSuffix(i);
                    String s = pattern;
                    s = s.replace("$1", sfx);
                    s = s.replace("$2", "");
                    return s;
                }
            };
            declareFunction(fimpl, name, FLOAT, FLOAT, type);
        }
    }
    
    /**
     * Used to declare mix functions of the following form:
     *   <ftype> mix(<ftype> x, <ftype> y, <ftype> a)
     *   <ftype> mix(<ftype> x, <ftype> y, float a)
     */
    private static void declareOverloadsMix() {
        final String name = "mix";
        final String pattern =
            "(x_tmp$1 * (1.0f - a_tmp$2) + y_tmp$1 * a_tmp$2)";
        
        for (Type type : new Type[] {FLOAT, FLOAT2, FLOAT3, FLOAT4}) {
            // declare (vectype,vectype,vectype) variants
            final boolean useSuffix = (type != FLOAT);
            FuncImpl fimpl = new FuncImpl() {
                public String toString(int i, List<Expr> params) {
                    String sfx = useSuffix ? getSuffix(i) : "";
                    String s = pattern;
                    s = s.replace("$1", sfx);
                    s = s.replace("$2", sfx);
                    return s;
                }
            };
            declareFunction(fimpl, name, type, type, type);
            
            if (type == FLOAT) {
                continue;
            }
            
            // declare (vectype,vectype,float) variants
            fimpl = new FuncImpl() {
                public String toString(int i, List<Expr> params) {
                    String sfx = getSuffix(i);
                    String s = pattern;
                    s = s.replace("$1", sfx);
                    s = s.replace("$2", "");
                    return s;
                }
            };
            declareFunction(fimpl, name, type, type, FLOAT);
        }
    }
}
