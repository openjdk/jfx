/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.compiler.backend.prism;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import com.sun.scenario.effect.compiler.JSLParser;
import com.sun.scenario.effect.compiler.model.BaseType;
import com.sun.scenario.effect.compiler.model.Qualifier;
import com.sun.scenario.effect.compiler.model.Type;
import com.sun.scenario.effect.compiler.model.Variable;
import com.sun.scenario.effect.compiler.tree.GlueBlock;
import com.sun.scenario.effect.compiler.tree.ProgramUnit;
import com.sun.scenario.effect.compiler.tree.TreeScanner;
import com.sun.scenario.effect.compiler.tree.VariableExpr;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 */
public class PrismBackend extends TreeScanner {

    private JSLParser parser;
    private StringBuilder usercode = new StringBuilder();
    private boolean isPixcoordReferenced = false;

    public PrismBackend(JSLParser parser, ProgramUnit program) {
        this.parser = parser;
        scan(program);
    }

    private ST getTemplate(String type) {
        STGroup group = new STGroupFile(getClass().getResource(type + "Glue.stg"), UTF_8.displayName(), '$', '$');
        return group.getInstanceOf("glue");
    }

    public String getGlueCode(String effectName,
                              String peerName,
                              String genericsName,
                              String interfaceName)
    {
        Map<String, Variable> vars = parser.getSymbolTable().getGlobalVariables();
        StringBuilder genericsDecl = new StringBuilder();
        StringBuilder interfaceDecl = new StringBuilder();
        StringBuilder samplerLinear = new StringBuilder();
        StringBuilder samplerInit = new StringBuilder();
        StringBuilder paramInit = new StringBuilder();
        StringBuilder paramUpdate = new StringBuilder();

        for (Variable v : vars.values()) {
            if (v.getQualifier() == Qualifier.PARAM) {
                String vname = v.getName();
                if (v.getType().getBaseType() == BaseType.SAMPLER) {
                    samplerInit.append("samplers.put(\"" + vname + "\", " + v.getReg() + ");\n");
                    if (v.getType() == Type.LSAMPLER || v.getType() == Type.FSAMPLER) {
                        samplerLinear.append("case " + v.getReg() + ":\n");
                        samplerLinear.append("    return true;\n");
                    }
                } else {
                    String accName = v.getAccessorName();
                    paramInit.append("params.put(\"" + vname + "\", " + v.getReg() + ");\n");
                    if (v.isArray()) {
                        paramUpdate.append("shader.setConstants(\"" + vname);
                        paramUpdate.append("\", " + accName + "(), 0, ");
                        paramUpdate.append(accName + "ArrayLength());\n");
                    } else if (v.getType().isVector()) {
                        paramUpdate.append(v.getType().getBaseType().toString());
                        paramUpdate.append("[] " + vname + "_tmp = ");
                        paramUpdate.append(accName + "();\n");
                        paramUpdate.append("shader.setConstant(\"" + vname + "\"");
                        for (int i = 0; i < v.getType().getNumFields(); i++) {
                            paramUpdate.append(", " + vname + "_tmp[" + i + "]");
                        }
                        paramUpdate.append(");\n");
                    } else {
                        paramUpdate.append("shader.setConstant(\"" + vname);
                        paramUpdate.append("\", " + accName + "());\n");
                    }
                }
            }
        }

        int numSamplers = parser.getSymbolTable().getNumSamplers();
        String superClass;
        if (numSamplers == 0) {
            superClass = "PPSZeroSamplerPeer";
        } else if (numSamplers == 1) {
            superClass = "PPSOneSamplerPeer";
        } else if (numSamplers == 2) {
            superClass = "PPSTwoSamplerPeer";
        } else {
            throw new RuntimeException("Must use zero, one, or two samplers (for now)");
        }

        if (genericsName != null) {
            genericsDecl.append("<"+genericsName+">");
        }

        if (interfaceName != null) {
            interfaceDecl.append("implements "+interfaceName);
        }

        ST glue = getTemplate("Prism");
        glue.add("effectName", effectName);
        glue.add("peerName", peerName);
        glue.add("superClass", superClass);
        glue.add("genericsDecl", genericsDecl.toString());
        glue.add("interfaceDecl", interfaceDecl.toString());
        glue.add("usercode", usercode.toString());
        glue.add("samplerLinear", samplerLinear.toString());
        glue.add("samplerInit", samplerInit.toString());
        glue.add("paramInit", paramInit.toString());
        glue.add("paramUpdate", paramUpdate.toString());
        glue.add("isPixcoordUsed", isPixcoordReferenced);
        return glue.render();
    }

    @Override
    public void visitGlueBlock(GlueBlock b) {
        usercode.append(b.getText());
    }

    @Override
    public void visitVariableExpr(VariableExpr e) {
        String varName = e.getVariable().getName();
        if (varName.equals("pixcoord")) {
            isPixcoordReferenced = true;
        }
    }
}
