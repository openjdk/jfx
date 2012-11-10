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

package com.sun.scenario.effect.compiler.backend.sw.sse;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import com.sun.scenario.effect.compiler.JSLParser;
import com.sun.scenario.effect.compiler.model.BaseType;
import com.sun.scenario.effect.compiler.model.Qualifier;
import com.sun.scenario.effect.compiler.model.Type;
import com.sun.scenario.effect.compiler.model.Variable;
import com.sun.scenario.effect.compiler.tree.FuncDef;
import com.sun.scenario.effect.compiler.tree.ProgramUnit;
import com.sun.scenario.effect.compiler.tree.TreeScanner;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;

/**
 */
public class SSEBackend extends TreeScanner {

    private final JSLParser parser;
    private final String body;

    public SSEBackend(JSLParser parser, ProgramUnit program) {
        // TODO: will be removed once we clean up static usage
        resetStatics();

        this.parser = parser;
        
        SSETreeScanner scanner = new SSETreeScanner();
        scanner.scan(program);
        this.body = scanner.getResult();
    }
    
    public static class GenCode {
        public String javaCode;
        public String nativeCode;
    }

    private static void appendGetRelease(StringBuilder get,
                                         StringBuilder rel,
                                         String ctype,
                                         String cbufName, String jarrayName)
    {
        get.append("j" + ctype + " *" + cbufName + " = (j" + ctype + " *)");
        get.append("env->GetPrimitiveArrayCritical(" + jarrayName + ", 0);\n");
        get.append("if (" + cbufName + " == NULL) return;\n");
        rel.append("env->ReleasePrimitiveArrayCritical(" + jarrayName + ", " + cbufName + ", JNI_ABORT);\n");
    }

    private static SortedSet<Variable> getSortedVars(Collection<Variable> unsortedVars) {
        Comparator<Variable> c = new Comparator<Variable>() {
            public int compare(Variable v0, Variable v1) {
                return v0.getName().compareTo(v1.getName());
            }
        };
        SortedSet<Variable> sortedVars = new TreeSet<Variable>(c);
        sortedVars.addAll(unsortedVars);
        return sortedVars;
    }

    public final GenCode getGenCode(String effectName,
                                    String peerName,
                                    String interfaceName)
    {
        Map<String, Variable> vars = parser.getSymbolTable().getGlobalVariables();
        StringBuilder interfaceDecl = new StringBuilder();
        StringBuilder constants = new StringBuilder();
        StringBuilder samplers = new StringBuilder();
        StringBuilder cleanup = new StringBuilder();
        StringBuilder srcRects = new StringBuilder();
        StringBuilder posDecls = new StringBuilder();
        StringBuilder pixInitY = new StringBuilder();
        StringBuilder pixInitX = new StringBuilder();
        StringBuilder posIncrY = new StringBuilder();
        StringBuilder posInitY = new StringBuilder();
        StringBuilder posIncrX = new StringBuilder();
        StringBuilder posInitX = new StringBuilder();
        StringBuilder jparams = new StringBuilder();
        StringBuilder jparamDecls = new StringBuilder();
        StringBuilder cparamDecls = new StringBuilder();
        StringBuilder arrayGet = new StringBuilder();
        StringBuilder arrayRelease = new StringBuilder();

        appendGetRelease(arrayGet, arrayRelease, "int", "dst", "dst_arr");
        
        // TODO: only need to declare these if pixcoord is referenced
        // somewhere in the program...
        pixInitY.append("float pixcoord_y = (float)dy;\n");
        pixInitX.append("float pixcoord_x = (float)dx;\n");

        // this step isn't strictly necessary but helps give some predictability
        // to the generated jar/nativelib so that the method signatures have
        // a consistent parameter ordering on all platforms for each build,
        // which may help debugging (see RT-4475)
        SortedSet<Variable> sortedVars = getSortedVars(vars.values());
        for (Variable v : sortedVars) {
            if (v.getQualifier() == Qualifier.CONST && v.getConstValue() == null) {
                // this must be a special built-in variable (e.g. pos0);
                // these are handled elsewhere, so just continue...
                continue;
            }
            
            Type t = v.getType();
            BaseType bt = t.getBaseType();
            String vtype = bt.toString();
            String vname = v.getName();
            if (v.getQualifier() != null && bt != BaseType.SAMPLER) {
                String accName = v.getAccessorName();
                if (v.isArray()) {
                    // TODO: we currently assume that param arrays will be
                    // stored in NIO Int/FloatBuffers, but the inner loop
                    // expects to access them as Java arrays, so we convert
                    // here; this is obviously bad for performance, so we need
                    // to come up with a better system soon...
                    String bufType = (bt == BaseType.FLOAT) ?
                        "FloatBuffer" : "IntBuffer";
                    String bufName = vname + "_buf";
                    String arrayName = vname + "_arr";
                    constants.append(bufType + " " + bufName + " = " + accName + "();\n");
                    constants.append(vtype + "[] " + arrayName);
                    constants.append(" = new " + vtype + "[");
                    constants.append(bufName + ".capacity()];\n");
                    constants.append(bufName + ".get(" + arrayName + ");\n");
                    jparams.append(",\n");
                    jparams.append(arrayName);
                    jparamDecls.append(",\n");
                    jparamDecls.append(vtype + "[] " + vname);
                    cparamDecls.append(",\n");
                    cparamDecls.append("j" + vtype + "Array " + vname);
                    appendGetRelease(arrayGet, arrayRelease, vtype, arrayName, vname);
                } else {
                    if (t.isVector()) {
                        String arrayName = vname + "_arr";
                        constants.append(vtype + "[] " + arrayName + " = " + accName + "();\n");
                        jparams.append(",\n");
                        jparamDecls.append(",\n");
                        cparamDecls.append(",\n");
                        for (int i = 0; i < t.getNumFields(); i++) {
                            if (i > 0) {
                                jparams.append(", ");
                                jparamDecls.append(", ");
                                cparamDecls.append(", ");
                            }
                            String vn = vname + getSuffix(i);
                            jparams.append(arrayName + "[" + i + "]");
                            jparamDecls.append(vtype + " " + vn);
                            cparamDecls.append("j" + vtype + " " + vn);
                        }
                    } else {
                        constants.append(vtype + " " + vname);
                        if (v.getQualifier() == Qualifier.CONST) {
                            constants.append(" = " + v.getConstValue());
                        } else {
                            constants.append(" = " + accName + "()");
                        }
                        constants.append(";\n");
                        jparams.append(",\n");
                        jparams.append(vname);
                        jparamDecls.append(",\n");
                        jparamDecls.append(vtype + " " + vname);
                        cparamDecls.append(",\n");
                        cparamDecls.append("j" + vtype + " " + vname);
                    }
                }
            } else if (v.getQualifier() == Qualifier.PARAM && bt == BaseType.SAMPLER) {
                int i = v.getReg();
                if (t == Type.FSAMPLER) {
                    samplers.append("FloatMap src" + i + " = (FloatMap)getSamplerData(" + i + ");\n");
                    samplers.append("int src" + i + "x = 0;\n");
                    samplers.append("int src" + i + "y = 0;\n");
                    samplers.append("int src" + i + "w = src" + i + ".getWidth();\n");
                    samplers.append("int src" + i + "h = src" + i + ".getHeight();\n");
                    samplers.append("int src" + i + "scan = src" + i + ".getWidth();\n");
                    samplers.append("float[] " + vname + " = src" + i + ".getData();\n");

                    arrayGet.append("float " + vname + "_vals[4];\n");

                    // TODO: for now, assume [0,0,1,1]
                    srcRects.append("float[] src" + i + "Rect = new float[] {0,0,1,1};\n");
                    
                    jparams.append(",\n");
                    jparams.append(vname);
                    
                    jparamDecls.append(",\n");
                    jparamDecls.append("float[] " + vname + "_arr");
                    
                    cparamDecls.append(",\n");
                    cparamDecls.append("jfloatArray " + vname + "_arr");
                    
                    appendGetRelease(arrayGet, arrayRelease, "float", vname, vname + "_arr");
                } else {
                    if (t == Type.LSAMPLER) {
                        samplers.append("HeapImage src" + i + " = (HeapImage)inputs[" + i + "].getUntransformedImage();\n");
                    } else {
                        samplers.append("HeapImage src" + i + " = (HeapImage)inputs[" + i + "].getTransformedImage(dstBounds);\n");
                        cleanup.append("inputs[" + i + "].releaseTransformedImage(src" + i + ");\n");
                    }
                    samplers.append("int src" + i + "x = 0;\n");
                    samplers.append("int src" + i + "y = 0;\n");
                    samplers.append("int src" + i + "w = src" + i + ".getPhysicalWidth();\n");
                    samplers.append("int src" + i + "h = src" + i + ".getPhysicalHeight();\n");
                    samplers.append("int src" + i + "scan = src" + i + ".getScanlineStride();\n");
                    samplers.append("int[] " + vname + " =\n");
                    samplers.append("    src" + i + ".getPixelArray();\n");

                    samplers.append("Rectangle src" + i + "Bounds = new Rectangle(");
                    samplers.append("src" + i + "x, ");
                    samplers.append("src" + i + "y, ");
                    samplers.append("src" + i + "w, ");
                    samplers.append("src" + i + "h);\n");
                    if (t == Type.LSAMPLER) {
                        samplers.append("Rectangle src" + i + "InputBounds = inputs[" + i + "].getUntransformedBounds();\n");
                        samplers.append("BaseTransform src" + i + "Transform = inputs[" + i + "].getTransform();\n");
                    } else {
                        samplers.append("Rectangle src" + i + "InputBounds = inputs[" + i + "].getTransformedBounds(dstBounds);\n");
                        samplers.append("BaseTransform src" + i + "Transform = BaseTransform.IDENTITY_TRANSFORM;\n");
                    }
                    samplers.append("setInputBounds(" + i + ", src" + i + "InputBounds);\n");
                    samplers.append("setInputNativeBounds(" + i + ", src" + i + "Bounds);\n");

                    if (t == Type.LSAMPLER) {
                        arrayGet.append("float " + vname + "_vals[4];\n");
                    }

                    // the source rect decls need to come after all calls to
                    // setInput[Native]Bounds() for all inputs (since the
                    // getSourceRegion() impl may need to query the bounds of
                    // other inputs, as is the case in PhongLighting)...
                    srcRects.append("float[] src" + i + "Rect = new float[4];\n");
                    // Note that we only allocate 4 floats here because none
                    // of the loops can deal with fully mapped inputs.  Only
                    // shaders that declare LSAMPLERs would require mapped
                    // inputs and so far that is only Perspective and
                    // Displacement, both of which override getTC() and return
                    // only 4 values.
                    srcRects.append("getTextureCoordinates(" + i + ", src" + i + "Rect,\n");
                    srcRects.append("                      src" + i + "InputBounds.x, src" + i + "InputBounds.y,\n");
                    srcRects.append("                      src" + i + "w, src" + i + "h,\n");
                    srcRects.append("                      dstBounds, src" + i + "Transform);\n");

                    jparams.append(",\n");
                    jparams.append(vname);
                    
                    jparamDecls.append(",\n");
                    jparamDecls.append("int[] " + vname + "_arr");
                    
                    cparamDecls.append(",\n");
                    cparamDecls.append("jintArray " + vname + "_arr");
                    
                    appendGetRelease(arrayGet, arrayRelease, "int", vname, vname + "_arr");
                }

                posDecls.append("float inc" + i + "_x = (src" + i + "Rect_x2 - src" + i + "Rect_x1) / dstw;\n");
                posDecls.append("float inc" + i + "_y = (src" + i + "Rect_y2 - src" + i + "Rect_y1) / dsth;\n");

                posInitY.append("float pos" + i + "_y = src" + i + "Rect_y1 + inc" + i + "_y*0.5f;\n");
                posInitX.append("float pos" + i + "_x = src" + i + "Rect_x1 + inc" + i + "_x*0.5f;\n");
                posIncrX.append("pos" + i + "_x += inc" + i + "_x;\n");
                posIncrY.append("pos" + i + "_y += inc" + i + "_y;\n");

                jparams.append(",\n");
                jparams.append("src" + i + "Rect[0], src" + i + "Rect[1],\n");
                jparams.append("src" + i + "Rect[2], src" + i + "Rect[3],\n");
                jparams.append("src" + i + "w, src" + i + "h, src" + i + "scan");
                
                jparamDecls.append(",\n");
                jparamDecls.append("float src" + i + "Rect_x1, float src" + i + "Rect_y1,\n");
                jparamDecls.append("float src" + i + "Rect_x2, float src" + i + "Rect_y2,\n");
                jparamDecls.append("int src" + i + "w, int src" + i + "h, int src" + i + "scan");

                cparamDecls.append(",\n");
                cparamDecls.append("jfloat src" + i + "Rect_x1, jfloat src" + i + "Rect_y1,\n");
                cparamDecls.append("jfloat src" + i + "Rect_x2, jfloat src" + i + "Rect_y2,\n");
                cparamDecls.append("jint src" + i + "w, jint src" + i + "h, jint src" + i + "scan");
            }
        }

        if (interfaceName != null) {
            interfaceDecl.append("implements "+interfaceName);
        }

        Reader template = new InputStreamReader(getClass().getResourceAsStream("SSEJavaGlue.stg"));
        StringTemplateGroup group = new StringTemplateGroup(template, DefaultTemplateLexer.class);
        StringTemplate jglue = group.getInstanceOf("glue");
        jglue.setAttribute("effectName", effectName);
        jglue.setAttribute("peerName", peerName);
        jglue.setAttribute("interfaceDecl", interfaceDecl.toString());
        jglue.setAttribute("usercode", usercode.toString());
        jglue.setAttribute("samplers", samplers.toString());
        jglue.setAttribute("cleanup", cleanup.toString());
        jglue.setAttribute("srcRects", srcRects.toString());
        jglue.setAttribute("constants", constants.toString());
        jglue.setAttribute("params", jparams.toString());
        jglue.setAttribute("paramDecls", jparamDecls.toString());

        template = new InputStreamReader(getClass().getResourceAsStream("SSENativeGlue.stg"));
        group = new StringTemplateGroup(template, DefaultTemplateLexer.class);
        StringTemplate cglue = group.getInstanceOf("glue");
        cglue.setAttribute("peerName", peerName);
        cglue.setAttribute("jniName", peerName.replace("_", "_1"));
        cglue.setAttribute("paramDecls", cparamDecls.toString());
        cglue.setAttribute("arrayGet", arrayGet.toString());
        cglue.setAttribute("arrayRelease", arrayRelease.toString());
        cglue.setAttribute("posDecls", posDecls.toString());
        cglue.setAttribute("pixInitY", pixInitY.toString());
        cglue.setAttribute("pixInitX", pixInitX.toString());
        cglue.setAttribute("posIncrY", posIncrY.toString());
        cglue.setAttribute("posInitY", posInitY.toString());
        cglue.setAttribute("posIncrX", posIncrX.toString());
        cglue.setAttribute("posInitX", posInitX.toString());
        cglue.setAttribute("body", body);
        
        GenCode gen = new GenCode();
        gen.javaCode = jglue.toString();
        gen.nativeCode = cglue.toString();
        return gen;
    }
    
    // TODO: need better mechanism for querying fields
    private static char[] fields = {'x', 'y', 'z', 'w'};
    public static String getSuffix(int i) {
        return "_" + fields[i];
    }

    static int getFieldIndex(char field) {
        switch (field) {
        case 'r':
        case 'x':
            return 0;
        case 'g':
        case 'y':
            return 1;
        case 'b':
        case 'z':
            return 2;
        case 'a':
        case 'w':
            return 3;
        default:
            throw new InternalError();
        }
    }
    
    // TODO: these shouldn't be implemented as a static method
    private static Map<String, FuncDef> funcDefs = new HashMap<String, FuncDef>();
    static void putFuncDef(FuncDef def) {
        funcDefs.put(def.getFunction().getName(), def);
    }
    static FuncDef getFuncDef(String name) {
        return funcDefs.get(name);
    }
    
    private static Set<String> resultVars = new HashSet<String>();
    static boolean isResultVarDeclared(String vname) {
        return resultVars.contains(vname);
    }
    static void declareResultVar(String vname) {
        resultVars.add(vname);
    }
    
    private static StringBuilder usercode = new StringBuilder();
    static void addGlueBlock(String block) {
        usercode.append(block);
    }
    
    private static void resetStatics() {
        funcDefs.clear();
        resultVars.clear();
        usercode = new StringBuilder();
    }
}
