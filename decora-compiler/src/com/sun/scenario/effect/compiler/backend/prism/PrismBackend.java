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
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;

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
    
    private StringTemplate getTemplate(String type) {
        Reader template = new InputStreamReader(getClass().getResourceAsStream(type + "Glue.stg"));
        StringTemplateGroup group = new StringTemplateGroup(template, DefaultTemplateLexer.class);
        return group.getInstanceOf("glue");
    }
    
    public String getGlueCode(String effectName,
                              String peerName,
                              String interfaceName)
    {
        Map<String, Variable> vars = parser.getSymbolTable().getGlobalVariables();
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

        if (interfaceName != null) {
            interfaceDecl.append("implements "+interfaceName);
        }

        StringTemplate glue = getTemplate("Prism");
        glue.setAttribute("effectName", effectName);
        glue.setAttribute("peerName", peerName);
        glue.setAttribute("superClass", superClass);
        glue.setAttribute("interfaceDecl", interfaceDecl.toString());
        glue.setAttribute("usercode", usercode.toString());
        glue.setAttribute("samplerLinear", samplerLinear.toString());
        glue.setAttribute("samplerInit", samplerInit.toString());
        glue.setAttribute("paramInit", paramInit.toString());
        glue.setAttribute("paramUpdate", paramUpdate.toString());
        glue.setAttribute("isPixcoordUsed", isPixcoordReferenced);
        return glue.toString();
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
