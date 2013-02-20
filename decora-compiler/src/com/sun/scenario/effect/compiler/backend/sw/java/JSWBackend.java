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

package com.sun.scenario.effect.compiler.backend.sw.java;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
public class JSWBackend extends TreeScanner {

    private final JSLParser parser;
    private final String body;

    public JSWBackend(JSLParser parser, ProgramUnit program) {
        // TODO: will be removed once we clean up static usage
        resetStatics();

        this.parser = parser;
        
        JSWTreeScanner scanner = new JSWTreeScanner();
        scanner.scan(program);
        this.body = scanner.getResult();
    }
    
    public final String getGenCode(String effectName,
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

        // TODO: only need to declare these if pixcoord is referenced
        // somewhere in the program...
        pixInitY.append("float pixcoord_y = (float)dy;\n");
        pixInitX.append("float pixcoord_x = (float)dx;\n");
        
        for (Variable v : vars.values()) {
            if (v.getQualifier() == Qualifier.CONST && v.getConstValue() == null) {
                // this must be a special built-in variable (e.g. pos0);
                // these are handled elsewhere, so just continue...
                continue;
            }
            
            Type t = v.getType();
            BaseType bt = t.getBaseType();
            if (v.getQualifier() != null && bt != BaseType.SAMPLER) {
                String vtype = bt.toString();
                String vname = v.getName();
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
                } else {
                    if (t.isVector()) {
                        String arrayName = vname + "_arr";
                        constants.append(vtype + "[] " + arrayName + " = " + accName + "();\n");
                        constants.append(vtype + " ");
                        for (int i = 0; i < t.getNumFields(); i++) {
                            if (i > 0) {
                                constants.append(", ");
                            }
                            constants.append(vname + getSuffix(i) + " = " + arrayName + "[" + i + "]");
                        }
                        constants.append(";\n");
                    } else {
                        constants.append(vtype + " " + vname);
                        if (v.getQualifier() == Qualifier.CONST) {
                            constants.append(" = " + v.getConstValue());
                        } else {
                            constants.append(" = " + accName + "()");
                        }
                        constants.append(";\n");
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
                    samplers.append("float[] " + v.getName() + " = src" + i + ".getData();\n");
                    samplers.append("float " + v.getName() + "_vals[] = new float[4];\n");

                    // TODO: for now, assume [0,0,1,1]
                    srcRects.append("float[] src" + i + "Rect = new float[] {0,0,1,1};\n");
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
                    samplers.append("int[] " + v.getName() + " =\n");
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
                        samplers.append("float " + v.getName() + "_vals[] = new float[4];\n");
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
                }

                posDecls.append("float inc" + i + "_x = (src" + i + "Rect[2] - src" + i + "Rect[0]) / dstw;\n");
                posDecls.append("float inc" + i + "_y = (src" + i + "Rect[3] - src" + i + "Rect[1]) / dsth;\n");

                posInitY.append("float pos" + i + "_y = src" + i + "Rect[1] + inc" + i + "_y*0.5f;\n");
                posInitX.append("float pos" + i + "_x = src" + i + "Rect[0] + inc" + i + "_x*0.5f;\n");
                posIncrX.append("pos" + i + "_x += inc" + i + "_x;\n");
                posIncrY.append("pos" + i + "_y += inc" + i + "_y;\n");
            }
        }
        
        if (interfaceName != null) {
            interfaceDecl.append("implements "+interfaceName);
        }

        Reader template = new InputStreamReader(getClass().getResourceAsStream("JSWGlue.stg"));
        StringTemplateGroup group = new StringTemplateGroup(template, DefaultTemplateLexer.class);
        StringTemplate glue = group.getInstanceOf("glue");
        glue.setAttribute("effectName", effectName);
        glue.setAttribute("peerName", peerName);
        glue.setAttribute("interfaceDecl", interfaceDecl.toString());
        glue.setAttribute("usercode", usercode.toString());
        glue.setAttribute("samplers", samplers.toString());
        glue.setAttribute("cleanup", cleanup.toString());
        glue.setAttribute("srcRects", srcRects.toString());
        glue.setAttribute("constants", constants.toString());
        glue.setAttribute("posDecls", posDecls.toString());
        glue.setAttribute("pixInitY", pixInitY.toString());
        glue.setAttribute("pixInitX", pixInitX.toString());
        glue.setAttribute("posIncrY", posIncrY.toString());
        glue.setAttribute("posInitY", posInitY.toString());
        glue.setAttribute("posIncrX", posIncrX.toString());
        glue.setAttribute("posInitX", posInitX.toString());
        glue.setAttribute("body", body);
        return glue.toString();
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
