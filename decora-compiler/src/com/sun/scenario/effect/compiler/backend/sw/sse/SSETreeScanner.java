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

import com.sun.scenario.effect.compiler.model.Function;
import com.sun.scenario.effect.compiler.model.Type;
import com.sun.scenario.effect.compiler.model.Variable;
import com.sun.scenario.effect.compiler.tree.*;
import static com.sun.scenario.effect.compiler.backend.sw.sse.SSEBackend.getFieldIndex;
import static com.sun.scenario.effect.compiler.backend.sw.sse.SSEBackend.getSuffix;

/**
 */
class SSETreeScanner extends TreeScanner {

    private final String funcName;
    private final StringBuilder sb = new StringBuilder();

    private boolean inVectorOp = false;
    private int vectorIndex = 0;
    private boolean inFieldSelect = false;
    private char selectedField = 'x';
    
    SSETreeScanner() {
        this(null);
    }
    
    SSETreeScanner(String funcName) {
        this.funcName = funcName;
    }
    
    private void output(String s) {
        sb.append(s);
    }
    
    String getResult() {
        return (sb != null) ? sb.toString() : null;
    }
    
    @Override
    public void visitArrayAccessExpr(ArrayAccessExpr e) {
        if (e.getExpr() instanceof VariableExpr &&
            e.getIndex() instanceof VariableExpr)
        {
            VariableExpr ve = (VariableExpr)e.getExpr();
            VariableExpr ie = (VariableExpr)e.getIndex();
            output(ve.getVariable().getName());
            output("_arr[" + ie.getVariable().getName());
            output(" * " + ve.getVariable().getType().getNumFields());
            output(" + " + getFieldIndex(selectedField) + "]");
        } else {
            throw new InternalError("Array access only supports variable expr/index (for now)");
        }
    }

    @Override
    public void visitBinaryExpr(BinaryExpr e) {
        scan(e.getLeft());
        output(" " + e.getOp() + " ");
        scan(e.getRight());
    }

    @Override
    public void visitBreakStmt(BreakStmt s) {
        output("break;");
    }

    @Override
    public void visitCallExpr(CallExpr e) {
        Function func = e.getFunction();
        output(func.getName() + "_res");
        if (func.getReturnType().isVector()) {
            // TODO: this needs more thought
            if (inFieldSelect) {
                output(getSuffix(getFieldIndex(selectedField)));
            } else if (inVectorOp) {
                output(getSuffix(vectorIndex));
            } else {
                throw new InternalError("TBD");
            }
        }
    }

    @Override
    public void visitCompoundStmt(CompoundStmt s) {
        output("{\n");
        super.visitCompoundStmt(s);
        output("}\n");
    }

    @Override
    public void visitContinueStmt(ContinueStmt s) {
        output("continue;");
    }

    @Override
    public void visitDeclStmt(DeclStmt s) {
        super.visitDeclStmt(s);
    }

    @Override
    public void visitDiscardStmt(DiscardStmt s) {
        // TODO: not yet implemented
    }

    @Override
    public void visitDoWhileStmt(DoWhileStmt s) {
        output("do ");
        scan(s.getStmt());
        output(" while (");
        scan(s.getExpr());
        output(");");
    }

    @Override
    public void visitExprStmt(ExprStmt s) {
        Expr expr = s.getExpr();
        
        outputPreambles(expr);

        Type t = expr.getResultType();
        if (t.isVector()) {
            inVectorOp = true;
            for (int i = 0; i < t.getNumFields(); i++) {
                vectorIndex = i;
                scan(s.getExpr());
                output(";\n");
            }
            inVectorOp = false;
        } else {
            scan(s.getExpr());
            output(";\n");
        }
    }

    @Override
    public void visitFieldSelectExpr(FieldSelectExpr e) {
        if (e.getFields().length() == 1) {
            selectedField = e.getFields().charAt(0);
        } else {
            int index = inVectorOp ? vectorIndex : 0;
            selectedField = e.getFields().charAt(index);
        }
        inFieldSelect = true;
        scan(e.getExpr());
        inFieldSelect = false;
    }
    
    @Override
    public void visitForStmt(ForStmt s) {
        output("for (");
        scan(s.getInit());
        scan(s.getCondition());
        output(";");
        scan(s.getExpr());
        output(")");
        scan(s.getStmt());
    }

    @Override
    public void visitFuncDef(FuncDef d) {
        if (d.getFunction().getName().equals("main")) {
            scan(d.getStmt());
        } else {
            // TODO: this is a hacky approach to saving func defs, which
            // will be inlined later at point of use)...
            SSEBackend.putFuncDef(d);
        }
    }
    
    @Override
    public void visitGlueBlock(GlueBlock b) {
        SSEBackend.addGlueBlock(b.getText());
    }

    @Override
    public void visitLiteralExpr(LiteralExpr e) {
        output(e.getValue().toString());
        if (e.getValue() instanceof Float) {
            output("f");
        }
    }
    
    @Override
    public void visitParenExpr(ParenExpr e) {
        output("(");
        scan(e.getExpr());
        output(")");
    }

    @Override
    public void visitProgramUnit(ProgramUnit p) {
        super.visitProgramUnit(p);
    }

    @Override
    public void visitReturnStmt(ReturnStmt s) {
        Expr expr = s.getExpr();
        if (expr == null) {
            throw new InternalError("Empty return not yet implemented");
        }
        if (funcName == null) {
            throw new RuntimeException("Return statement not expected");
        }
        
        Type t = expr.getResultType();
        if (t.isVector()) {
            inVectorOp = true;
            for (int i = 0; i < t.getNumFields(); i++) {
                vectorIndex = i;
                output(funcName + "_res" + getSuffix(i) + " = ");
                scan(s.getExpr());
                output(";\n");
            }
            inVectorOp = false;
        } else {
            output(funcName + "_res = ");
            scan(s.getExpr());
            output(";\n");
        }
    }

    @Override
    public void visitSelectStmt(SelectStmt s) {
        output("if (");
        scan(s.getIfExpr());
        output(")");
        scan(s.getThenStmt());
        Stmt e = s.getElseStmt();
        if (e != null) {
            output(" else ");
            scan(e);
        }
    }

    @Override
    public void visitUnaryExpr(UnaryExpr e) {
        output(e.getOp().toString());
        scan(e.getExpr());
    }
    
    @Override
    public void visitVarDecl(VarDecl d) {
        Variable var = d.getVariable();
        if (var.getQualifier() != null) {
            // these will be declared separately outside the loop body
            return;
        }

        outputPreambles(d);
        
        Type t = var.getType();
        if (t.isVector()) {
            inVectorOp = true;
            for (int i = 0; i < t.getNumFields(); i++) {
                output(t.getBaseType().toString() + " ");
                output(var.getName() + getSuffix(i));
                Expr init = d.getInit();
                if (init != null) {
                    output(" = ");
                    vectorIndex = i;
                    scan(init);
                }
                output(";\n");
            }
            inVectorOp = false;
        } else {
            output(t.toString() + " " + var.getName());
            Expr init = d.getInit();
            if (init != null) {
                output(" = ");
                scan(init);
            }
            output(";\n");
        }
    }

    @Override
    public void visitVariableExpr(VariableExpr e) {
        Variable var = e.getVariable();
        output(var.getName());
        if (var.isParam()) {
            output("_tmp");
        }
        if (var.getType().isVector()) {
            if (inFieldSelect) {
                output(getSuffix(getFieldIndex(selectedField)));
            } else if (inVectorOp) {
                output(getSuffix(vectorIndex));
            } else {
                throw new InternalError("TBD");
            }
        }
    }

    @Override
    public void visitVectorCtorExpr(VectorCtorExpr e) {
        // TODO: this will likely work for simple variables and literals,
        // but we need something more for embedded function calls, etc...
        scan(e.getParams().get(vectorIndex));
    }

    @Override
    public void visitWhileStmt(WhileStmt s) {
        output("while (");
        scan(s.getCondition());
        output(")");
        scan(s.getStmt());
    }
    
    private void outputPreambles(Tree tree) {
        SSECallScanner scanner = new SSECallScanner();
        scanner.scan(tree);
        String res = scanner.getResult();
        if (res != null) {
            output(scanner.getResult());
        }
    }
}
