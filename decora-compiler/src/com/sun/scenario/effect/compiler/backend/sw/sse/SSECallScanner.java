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

package com.sun.scenario.effect.compiler.backend.sw.sse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.sun.scenario.effect.compiler.model.BaseType;
import com.sun.scenario.effect.compiler.model.FuncImpl;
import com.sun.scenario.effect.compiler.model.Function;
import com.sun.scenario.effect.compiler.model.Param;
import com.sun.scenario.effect.compiler.model.Type;
import com.sun.scenario.effect.compiler.model.Variable;
import com.sun.scenario.effect.compiler.tree.ArrayAccessExpr;
import com.sun.scenario.effect.compiler.tree.BinaryExpr;
import com.sun.scenario.effect.compiler.tree.CallExpr;
import com.sun.scenario.effect.compiler.tree.Expr;
import com.sun.scenario.effect.compiler.tree.FieldSelectExpr;
import com.sun.scenario.effect.compiler.tree.LiteralExpr;
import com.sun.scenario.effect.compiler.tree.ParenExpr;
import com.sun.scenario.effect.compiler.tree.TreeScanner;
import com.sun.scenario.effect.compiler.tree.UnaryExpr;
import com.sun.scenario.effect.compiler.tree.VariableExpr;
import com.sun.scenario.effect.compiler.tree.VectorCtorExpr;

import static com.sun.scenario.effect.compiler.backend.sw.sse.SSEBackend.*;

/*
 * How should we translate function calls?  For now we will inline
 * everything, i.e. expand function bodies directly into the loop body.
 * 
 * The approach... For an ExprStmt or VarDecl (with initializer),
 * walk down the tree and see if there are any function calls.
 * For each function call, inline the function implementation prior
 * to the statement output.  The statement will then refer to
 * the output variables from the inlined function, rather than
 * the function call itself.
 * 
 * First declare the result variables using the name of the
 * called function and a field suffix, if needed; for example:
 *     float3 val = sample(...).rgb;
 * ==>
 *     float sample_res_r, sample_res_g, sample_res_b;
 * 
 * Inside the inlined function, assign parameter expressions to
 * temporary variables, using the name of the declared parameters
 * as a guide; for example:
 *     float val = min(foo+0.25, 1.0);
 * ==>
 *     float min_res;
 *     {
 *         float a_tmp = foo + 0.25f;
 *         float b_tmp = 1.0f;
 *         min_res = (a_tmp < b_tmp) a_tmp : b_tmp;
 *     }
 * 
 * In a future version, references to scalar variables and literals
 * could easily be inlined; for example:
 *     float val = min(foo+0.25*bar, 1.0);
 * ==>
 *     float min_res;
 *     {
 *         float a_tmp = foo + 0.25f * bar;
 *         min_res = (a_tmp < 1.0f) a_tmp : 1.0f;
 *     }
 * 
 * Note that this system will likely produce less-than-efficient
 * Java code in many cases; for now we're just trying to get things
 * functional, and performance improvements will certainly come later.
 * 
 * 
 * Example #1:
 *     float3 val = scale * sample(baseImg, pos + off.xy).rgb;
 * ==>
 *     float sample_res_r, sample_res_g, sample_res_b;
 *     {
 *         float pos_x_tmp = pos_x + off_x;
 *         float pos_y_tmp = pos_y + off_y;
 *         int baseImg_tmp =
 *             baseImg[(int)(pos_y_tmp*srch*srcscan) + (int)(pos_x_tmp*srcw)];
 *         sample_res_r = (((baseImg_tmp >>  16) & 0xff) / 255f);
 *         sample_res_g = (((baseImg_tmp >>   8) & 0xff) / 255f);
 *         sample_res_b = (((baseImg_tmp       ) & 0xff) / 255f);
 *     }
 *     float val_r = scale * sample_res_r;
 *     float val_g = scale * sample_res_g;
 *     float val_b = scale * sample_res_b;
 * 
 * Example #2:
 *     float val = scale * clamp(foo, 0.0, 1.0);
 * ==>
 *     float clamp_res;
 *     {
 *         float val_tmp = foo;
 *         float min_tmp = 0.0f;
 *         float max_tmp = 1.0f;
 *         if (val_tmp < min_tmp) clamp_res = min_tmp;
 *         else if (val_tmp > max_tmp) clamp_res = max_tmp;
 *         else clamp_res = val_tmp;
 *     }
 *     float val = scale * clamp_res;
 */
class SSECallScanner extends TreeScanner {
    private StringBuilder sb;
    private boolean inCallExpr = false;
    private Set<Integer> selectedFields = null;
    private boolean inFieldSelect = false;
    private char selectedField = 'x';
    private boolean inVectorOp = false;
    private int vectorIndex = 0;

    private void output(String s) {
        if (sb == null) {
            sb = new StringBuilder();
        }
        sb.append(s);
    }

    String getResult() {
        return (sb != null) ? sb.toString() : null;
    }

    @Override
    public void visitCallExpr(CallExpr e) {
        if (inCallExpr) {
            throw new InternalError("Nested function calls not yet supported");
        }

        Function func = e.getFunction();
        Type t = func.getReturnType();
        String vtype = t.getBaseType().toString();
        String vname = func.getName();
        Set<Integer> fields = selectedFields;
        if (t.isVector()) {
            if (fields == null) {
                fields = new HashSet<Integer>();
                for (int i = 0; i < t.getNumFields(); i++) {
                    fields.add(i);
                }
            }
        }
        if (!SSEBackend.isResultVarDeclared(vname)) {
            // only declare result variables if they haven't been already
            // TODO: there's a bug here; suppose a function like
            // min(float,float) is inlined, then later we inline
            // min(float3,float3), the second time we'll think we already have
            // declared the result variables, but min_res_y/z won't be there...
            SSEBackend.declareResultVar(vname);
            if (t.isVector()) {
                output(vtype + " ");
                boolean first = true;
                for (Integer f : fields) {
                    if (first) {
                        first = false;
                    } else {
                        output(", ");
                    }
                    output(vname + "_res" + getSuffix(f));
                }
                output(";\n");
            } else {
                output(vtype + " " + vname + "_res;\n");
            }
        }

        inCallExpr = true;
        output("{\n");
        List<Param> params = func.getParams();
        List<Expr> argExprs = e.getParams();
        for (int i = 0; i < params.size(); i++) {
            Param param = params.get(i);
            String pname = param.getName();
            Type ptype = param.getType();
            BaseType pbasetype = ptype.getBaseType();
            if (pbasetype == BaseType.SAMPLER) {
                // skip these for now
                continue;
            }
            if (ptype.isVector()) {
                inVectorOp = true;
                for (int j = 0; j < ptype.getNumFields(); j++) {
                    vectorIndex = j;
                    output(pbasetype.toString());
                    output(" ");
                    output(pname + "_tmp" + getSuffix(j) + " = ");
                    scan(argExprs.get(i));
                    output(";\n");
                }
                inVectorOp = false;
            } else {
                output(pbasetype.toString());
                output(" ");
                output(pname + "_tmp = ");
                scan(argExprs.get(i));
                output(";\n");
            }
        }

        FuncImpl impl = SSEFuncImpls.get(func);
        if (impl != null) {
            // core (built-in) function
            String preamble = impl.getPreamble(argExprs);
            if (preamble != null) {
                output(preamble);
            }

            if (t.isVector()) {
                for (Integer f : fields) {
                    output(vname + "_res" + getSuffix(f) + " = ");
                    output(impl.toString(f, argExprs));
                    output(";\n");
                }
            } else {
                output(vname + "_res = ");
                output(impl.toString(0, argExprs));
                output(";\n");
            }
        } else {
            // user-defined function
            SSETreeScanner scanner = new SSETreeScanner(func.getName());
            scanner.scan(SSEBackend.getFuncDef(func.getName()).getStmt());
            output(scanner.getResult());
        }

        output("\n}\n");
        inCallExpr = false;
    }

    @Override
    public void visitArrayAccessExpr(ArrayAccessExpr e) {
        if (inCallExpr) {
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
        } else {
            super.visitArrayAccessExpr(e);
        }
    }

    @Override
    public void visitBinaryExpr(BinaryExpr e) {
        if (inCallExpr) {
            scan(e.getLeft());
            output(" " + e.getOp() + " ");
            scan(e.getRight());
        } else {
            super.visitBinaryExpr(e);
        }
    }

    @Override
    public void visitFieldSelectExpr(FieldSelectExpr e) {
        if (inCallExpr) {
            if (e.getFields().length() == 1) {
                selectedField = e.getFields().charAt(0);
            } else {
                int index = inVectorOp ? vectorIndex : 0;
                selectedField = e.getFields().charAt(index);
            }
            inFieldSelect = true;
            scan(e.getExpr());
            inFieldSelect = false;
        } else {
            selectedFields = getFieldSet(e.getFields());
            super.visitFieldSelectExpr(e);
            selectedFields = null;
        }
    }

    private static Set<Integer> getFieldSet(String fields) {
        Set<Integer> fieldSet = new HashSet<Integer>();
        for (int i = 0; i < fields.length(); i++) {
            fieldSet.add(getFieldIndex(fields.charAt(i)));
        }
        return fieldSet;
    }

    @Override
    public void visitLiteralExpr(LiteralExpr e) {
        if (inCallExpr) {
            output(e.getValue().toString());
            if (e.getValue() instanceof Float) {
                output("f");
            }
        } else {
            super.visitLiteralExpr(e);
        }
    }

    @Override
    public void visitParenExpr(ParenExpr e) {
        if (inCallExpr) {
            output("(");
            scan(e.getExpr());
            output(")");
        } else {
            super.visitParenExpr(e);
        }
    }

    @Override
    public void visitUnaryExpr(UnaryExpr e) {
        if (inCallExpr) {
            output(e.getOp().toString());
            scan(e.getExpr());
        } else {
            super.visitUnaryExpr(e);
        }
    }

    @Override
    public void visitVariableExpr(VariableExpr e) {
        if (inCallExpr) {
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
        } else {
            super.visitVariableExpr(e);
        }
    }

    @Override
    public void visitVectorCtorExpr(VectorCtorExpr e) {
        // TODO: this will likely work for simple variables and literals,
        // but we need something more for embedded function calls, etc...
        scan(e.getParams().get(vectorIndex));
    }
}
