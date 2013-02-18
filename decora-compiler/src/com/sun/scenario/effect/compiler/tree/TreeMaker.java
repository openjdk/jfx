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

package com.sun.scenario.effect.compiler.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.sun.scenario.effect.compiler.model.BaseType;
import com.sun.scenario.effect.compiler.model.BinaryOpType;
import com.sun.scenario.effect.compiler.model.Function;
import com.sun.scenario.effect.compiler.model.Qualifier;
import com.sun.scenario.effect.compiler.model.SymbolTable;
import com.sun.scenario.effect.compiler.model.Type;
import com.sun.scenario.effect.compiler.model.UnaryOpType;
import com.sun.scenario.effect.compiler.model.Variable;

import static com.sun.scenario.effect.compiler.model.Type.*;

/**
 */
public class TreeMaker {

    private final SymbolTable symbols;
    
    public TreeMaker(SymbolTable symbols) {
        this.symbols = symbols;
    }
    
    public BinaryExpr binary(BinaryOpType op, Expr left, Expr right) {
        if (op.isAssignment()) {
            if (left instanceof VariableExpr) {
                VariableExpr ve = (VariableExpr)left;
                if (ve.getVariable().getQualifier() == Qualifier.CONST) {
                    throw new RuntimeException("Left-hand side of assignment expression cannot be const variable");
                }
            } else if (left instanceof FieldSelectExpr) {
                FieldSelectExpr fse = (FieldSelectExpr)left;
                if (!(fse.getExpr() instanceof VariableExpr)) {
                    throw new RuntimeException("Left-hand side of assignment expression must be a variable");
                }
                VariableExpr ve = (VariableExpr)fse.getExpr();
                if (ve.getVariable().getQualifier() == Qualifier.CONST) {
                    throw new RuntimeException("Left-hand side of assignment expression cannot be const variable");
                }
            } else {
                throw new RuntimeException("Left-hand side of assignment expression must be a variable");
            }
        }
        return new BinaryExpr(op, left, right);
    }

    public UnaryExpr unary(UnaryOpType op, Expr expr) {
        Type rt = expr.getResultType();
        switch (op) {
        case INC:
        case DEC:
        case PLUS:
        case MINUS:
            if (rt != FLOAT && rt != INT) {
                throw new RuntimeException("Cannot use " + op + " with " + rt);
            }
            break;
        case NOT:
            if (rt != BOOL) {
                throw new RuntimeException("Cannot use " + op + " with " + rt);
            }
            break;
        default:
            break;
        }
        return new UnaryExpr(op, expr);
    }

    public LiteralExpr literal(Type type, Object value) {
        return new LiteralExpr(type, value);
    }
    
    public VariableExpr variable(String id) {
        Map<String, Variable> vars = symbols.getVariablesForScope();
        Variable var = vars.get(id);
        if (var == null) {
            var = symbols.getGlobalVariables().get(id);
            if (var == null) {
                throw new RuntimeException("Unknown variable " + id);
            }
        }
        var.incrementRefCount();
        return new VariableExpr(var);
    }
    
    public VectorCtorExpr vectorCtor(Type type, List<Expr> params) {
        if (!type.isVector()) {
            throw new RuntimeException("Cannot use constructor with scalar type " + type);
        }
        int numFields = type.getNumFields();
        if (params.size() == 1) {
            // expand shorthand expressions like:
            //   float4(0.0)
            // into:
            //   float4(0.0, 0.0, 0.0, 0.0)
            params = Collections.nCopies(numFields, params.get(0));
        } else if (params.size() != numFields) {
            // TODO: relax this restriction to allow things like:
            //   float4(foo.rgb, 1.0)
            throw new RuntimeException("Number of constructor args must match size of " + type);
        }
        BaseType baseType = type.getBaseType();
        for (Expr param : params) {
            if (baseType != param.getResultType().getBaseType()) {
                throw new RuntimeException("Arguments to " + type + " constructor must be of base type " + baseType);
            }
        }
        return new VectorCtorExpr(type, params);
    }
    
    public ParenExpr parenExpr(Expr expr) {
        return new ParenExpr(expr);
    }
    
    public FieldSelectExpr fieldSelect(Expr expr, String fields) {
        if (!expr.getResultType().isVector()) {
            throw new RuntimeException("Cannot use field selection with scalar types");
        }
        return new FieldSelectExpr(expr, fields.substring(1));
    }
    
    public ArrayAccessExpr arrayAccess(Expr expr, Expr index) {
        if (index.getResultType() != Type.INT) {
            throw new RuntimeException("Array index must be an integer");
        }
        return new ArrayAccessExpr(expr, index);
    }
    
    public CallExpr call(String id, List<Expr> params) {
        if (params == null) {
            params = new ArrayList<Expr>();
        }
        List<Type> ptypes = new ArrayList<Type>();
        for (Expr expr : params) {
            ptypes.add(expr.getResultType());
        }
        Function func = symbols.getFunctionForSignature(id, ptypes);
        if (func == null) {
            String paramString = "(";
            boolean first = true;
            for (Expr expr : params) {
                if (!first) {
                    paramString += ", ";
                } else {
                    first = false;
                }
                paramString += expr.getResultType();
            }
            paramString += ")";
            throw new RuntimeException("Unknown function " + id + paramString);
        }
        return new CallExpr(func, params);
    }
    
    public ContinueStmt continueStmt() {
        return new ContinueStmt();
    }
    
    public BreakStmt breakStmt() {
        return new BreakStmt();
    }
    
    public DiscardStmt discardStmt() {
        return new DiscardStmt();
    }
    
    public ReturnStmt returnStmt(Expr expr) {
        return new ReturnStmt(expr);
    }
    
    public SelectStmt selectStmt(Expr ifExpr, Stmt thenStmt, Stmt elseStmt) {
        return new SelectStmt(ifExpr, thenStmt, elseStmt);
    }

    public WhileStmt whileStmt(Expr cond, Stmt stmt) {
        if (cond.getResultType() != Type.BOOL) {
            throw new RuntimeException("Condition for 'while' loop must be a boolean expression");
        }
        return new WhileStmt(cond, stmt);
    }
    
    public DoWhileStmt doWhileStmt(Stmt stmt, Expr expr) {
        if (expr.getResultType() != Type.BOOL) {
            throw new RuntimeException("Condition for 'do/while' loop must be a boolean expression");
        }
        return new DoWhileStmt(stmt, expr);
    }
    
    public ForStmt forStmt(Stmt init, Expr cond, Expr expr, Stmt stmt,
                           int unrollMax, int unrollCheck)
    {
        if (cond != null && cond.getResultType() != Type.BOOL) {
            throw new RuntimeException("Condition for 'for' loop must be a boolean expression");
        }
        if (expr != null && expr.getResultType().isVector()) {
            throw new RuntimeException("Expression for 'for' loop must be scalar");
        }
        return new ForStmt(init, cond, expr, stmt, unrollMax, unrollCheck);
    }
    
    public ExprStmt exprStmt(Expr expr) {
        return new ExprStmt(expr);
    }
    
    public DeclStmt declStmt(List<VarDecl> decls) {
        return new DeclStmt(decls);
    }
    
    public CompoundStmt compoundStmt(List<Stmt> stmts) {
        return new CompoundStmt(stmts);
    }
    
    public FuncDef funcDef(Function func, Stmt stmt) {
        return new FuncDef(func, stmt);
    }
    
    public VarDecl varDecl(Variable var, Expr init) {
        if (init != null && init.getResultType() != var.getType()) {
            throw new RuntimeException("Initializer result type must match that of variable");
        }
        if (var.getQualifier() == Qualifier.PARAM && init != null) {
            throw new RuntimeException("Param variable cannot have an initializer");
        }
        return new VarDecl(var, init);
    }
    
    public GlueBlock glueBlock(String text) {
        return new GlueBlock(text);
    }
    
    public ProgramUnit programUnit(List<ExtDecl> decls) {
        return new ProgramUnit(decls);
    }
}
