/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.scenario.effect.compiler.JSLBaseVisitor;
import com.sun.scenario.effect.compiler.JSLParser;
import com.sun.scenario.effect.compiler.model.BinaryOpType;
import com.sun.scenario.effect.compiler.model.Function;
import com.sun.scenario.effect.compiler.model.Param;
import com.sun.scenario.effect.compiler.model.Precision;
import com.sun.scenario.effect.compiler.model.Qualifier;
import com.sun.scenario.effect.compiler.model.SymbolTable;
import com.sun.scenario.effect.compiler.model.Type;
import com.sun.scenario.effect.compiler.model.UnaryOpType;
import com.sun.scenario.effect.compiler.model.Variable;

import java.util.ArrayList;
import java.util.List;

public class JSLVisitor extends JSLBaseVisitor<Tree> {
    private SymbolTable symbols = new SymbolTable();
    private TreeMaker tm = new TreeMaker(symbols);

    public SymbolTable getSymbolTable() {
        return symbols;
    }

    public static class StringExpr extends Expr {
        private final String string;

        StringExpr(String string) {
            super(Type.SAMPLER); // dummy
            this.string = string;
        }

        public String getString() {
            return string;
        }

        @Override
        public void accept(TreeVisitor tv) {}
    }

    public static class FullySpecifiedTypeExpr extends Expr {
        private final Qualifier qual;
        private final Precision precision;
        private final Type type;

        public FullySpecifiedTypeExpr(Qualifier qual, Precision precision, Type type) {
            super(Type.SAMPLER); // dummy
            this.qual = qual;
            this.precision = precision;
            this.type = type;
        }

        public Qualifier getQual() {
            return qual;
        }

        public Precision getPrecision() {
            return precision;
        }

        public Type getType() {
            return type;
        }

        @Override
        public void accept(TreeVisitor tv) {}
    }

    public static class ForRestExpr extends Expr {
        private final Expr cond;
        private final Expr expr;

        ForRestExpr(Expr cond, Expr expr) {
            super(Type.SAMPLER); // dummy
            this.cond = cond;
            this.expr = expr;
        }

        public Expr getCond() {
            return cond;
        }

        public Expr getExpr() {
            return expr;
        }

        @Override
        public void accept(TreeVisitor tv) {}
    }

    public static class ParamExpr extends Expr {
        private final Param param;

        public ParamExpr(Param param) {
            super(Type.SAMPLER); // dummy
            this.param = param;
        }

        public Param getParam() {
            return param;
        }

        @Override
        public void accept(TreeVisitor tv) {}
    }

    public static class ParamListExpr extends Expr {
        private final List<Param> paramList;

        public ParamListExpr(List<Param> paramList) {
            super(Type.SAMPLER); // dummy
            this.paramList = paramList;
        }

        public List<Param> getParamList() {
            return paramList;
        }

        @Override
        public void accept(TreeVisitor tv) {}
    }

    public static class DeclIdInitExpr extends Expr {
        private final String name;
        private final Expr arrayInit;
        private final Expr init;

        public DeclIdInitExpr(String name, Expr arrayInit, Expr init) {
            super(Type.SAMPLER); // dummy
            this.name = name;
            this.arrayInit = arrayInit;
            this.init = init;
        }

        public String getName() {
            return name;
        }

        public Expr getArrayInit() {
            return arrayInit;
        }

        public Expr getInit() {
            return init;
        }

        @Override
        public void accept(TreeVisitor tv) {}
    }

    @Override
    public StringExpr visitField_selection(JSLParser.Field_selectionContext ctx) {
        if (ctx.RGBA_FIELDS() != null) {
            return new StringExpr(ctx.r.getText());
        } else if (ctx.XYZW_FIELDS() != null) {
            return new StringExpr(ctx.x.getText());
        }
        throw new RuntimeException("invalid field selection");
    }

    @Override
    public Expr visitPrimary_expression(JSLParser.Primary_expressionContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            return tm.variable(ctx.IDENTIFIER().getText());
        } else if (ctx.INTCONSTANT() != null) {
            return tm.literal(Type.INT, Integer.valueOf(ctx.INTCONSTANT().getText()));
        } else if (ctx.FLOATCONSTANT() != null) {
            return tm.literal(Type.FLOAT, Float.valueOf(ctx.FLOATCONSTANT().getText()));
        } else if (ctx.BOOLCONSTANT() != null) {
            return tm.literal(Type.BOOL, Boolean.valueOf(ctx.BOOLCONSTANT().getText()));
        } else if (ctx.e != null) {
            return tm.parenExpr(visitExpression(ctx.e));
        }

        throw new RuntimeException("invalid primary expression");
    }

    @Override
    public Expr visitPrimary_or_call(JSLParser.Primary_or_callContext ctx) {
        if (ctx.primary_expression() != null) {
            return visitPrimary_expression(ctx.e);
        } else if (ctx.function_call() != null) {
            return visitFunction_call(ctx.f);
        }

        throw new RuntimeException("invalid primary or call");
    }

    @Override
    public Expr visitPostfix_expression(JSLParser.Postfix_expressionContext ctx) {
        if (ctx.expression() != null) {
            if (ctx.field_selection() != null) {
                return tm.fieldSelect(tm.arrayAccess(visitPrimary_or_call(ctx.e), visitExpression(ctx.ae)),
                        visitField_selection(ctx.fs).getString());
            } else {
                return tm.arrayAccess(visitPrimary_or_call(ctx.e), visitExpression(ctx.ae));
            }
        } else if (ctx.field_selection() != null) {
            return tm.fieldSelect(visitPrimary_or_call(ctx.e), visitField_selection(ctx.fs).getString());
        } else if (ctx.INC() != null) {
            return tm.unary(UnaryOpType.INC, visitPrimary_or_call(ctx.e));
        } else if (ctx.DEC() != null) {
            return tm.unary(UnaryOpType.DEC, visitPrimary_or_call(ctx.e));
        } else if (ctx.e != null){
            return visitPrimary_or_call(ctx.e);
        }

        throw new RuntimeException("invalid postfix expression");
    }

    @Override
    public Expr visitFunction_call(JSLParser.Function_callContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            return tm.call(ctx.IDENTIFIER().getText(), ctx.function_call_parameter_list() != null ?
                    visitFunction_call_parameter_list(ctx.p).getParams() : null);
        } else if (ctx.type_specifier() != null) {
            Type type = Type.fromToken(ctx.ts.getText());
            return tm.vectorCtor(type, ctx.function_call_parameter_list() != null ?
                    visitFunction_call_parameter_list(ctx.p).getParams() : null);
        }

        throw new RuntimeException("invalid function call");
    }

    @Override
    public CallExpr visitFunction_call_parameter_list(JSLParser.Function_call_parameter_listContext ctx) {
        List<Expr> list = new ArrayList<>();
        for (JSLParser.Assignment_expressionContext aeCtx : ctx.assignment_expression()) {
            list.add(visitAssignment_expression(aeCtx));
        }
        return new CallExpr(null, list);
    }

    @Override
    public Expr visitUnary_expression(JSLParser.Unary_expressionContext ctx) {
        if (ctx.INC() != null) {
            return tm.unary(UnaryOpType.INC, visitUnary_expression(ctx.u));
        } else if (ctx.DEC() != null) {
            return tm.unary(UnaryOpType.DEC, visitUnary_expression(ctx.u));
        } else if (ctx.PLUS() != null) {
            return tm.unary(UnaryOpType.PLUS, visitUnary_expression(ctx.u));
        } else if (ctx.DASH() != null) {
            return tm.unary(UnaryOpType.MINUS, visitUnary_expression(ctx.u));
        } else if (ctx.BANG() != null) {
            return tm.unary(UnaryOpType.NOT, visitUnary_expression(ctx.u));
        } else if (ctx.postfix_expression() != null){
            return visitPostfix_expression(ctx.p);
        }

        throw new RuntimeException("invalid unary expression");
    }

    @Override
    public Expr visitMultiplicative_expression(JSLParser.Multiplicative_expressionContext ctx) {
        Expr expr = visitUnary_expression(ctx.a);

        if (ctx.multiplicative_operator() != null) {
            for (int i = 0; i < ctx.multiplicative_expression().size(); i++) {
                JSLParser.Multiplicative_expressionContext context = ctx.multiplicative_expression(i);
                if (ctx.multiplicative_operator(i).STAR() != null) {
                    expr = tm.binary(BinaryOpType.MUL, expr, visitMultiplicative_expression(context));
                } else if (ctx.multiplicative_operator(i).SLASH() != null) {
                    expr = tm.binary(BinaryOpType.DIV, expr, visitMultiplicative_expression(context));
                } else {
                    throw new RuntimeException("unexpected multiplicative operator");
                }
            }
        }
        return expr;
    }

    @Override
    public Expr visitAdditive_expression(JSLParser.Additive_expressionContext ctx) {
        Expr expr = visitMultiplicative_expression(ctx.a);

        if (ctx.additive_operator() != null) {
            for (int i = 1; i < ctx.multiplicative_expression().size(); i++) {
                JSLParser.Multiplicative_expressionContext context = ctx.multiplicative_expression(i);
                if (ctx.additive_operator(i - 1).PLUS() != null) {
                    expr = tm.binary(BinaryOpType.ADD, expr, visitMultiplicative_expression(context));
                } else if (ctx.additive_operator(i - 1).DASH() != null) {
                    expr = tm.binary(BinaryOpType.SUB, expr, visitMultiplicative_expression(context));
                } else {
                    throw new RuntimeException("unexpected additive operator");
                }
            }
        }
        return expr;
    }

    @Override
    public Expr visitRelational_expression(JSLParser.Relational_expressionContext ctx) {
        Expr expr = visitAdditive_expression(ctx.a);

        if (ctx.relational_operator() != null) {
            for (int i = 1; i < ctx.additive_expression().size(); i++) {
                JSLParser.Additive_expressionContext context = ctx.additive_expression(i);
                if (ctx.relational_operator(i - 1).LTEQ() != null) {
                    expr = tm.binary(BinaryOpType.LTEQ, expr, visitAdditive_expression(context));
                } else if (ctx.relational_operator(i - 1).GTEQ() != null) {
                    expr = tm.binary(BinaryOpType.GTEQ, expr, visitAdditive_expression(context));
                } else if (ctx.relational_operator(i - 1).LT() != null) {
                    expr = tm.binary(BinaryOpType.LT, expr, visitAdditive_expression(context));
                } else if (ctx.relational_operator(i - 1).GT() != null) {
                    expr = tm.binary(BinaryOpType.GT, expr, visitAdditive_expression(context));
                } else {
                    throw new RuntimeException("unexpected relational operator");
                }
            }

            return expr;
        }

        return expr;
    }

    @Override
    public Expr visitEquality_expression(JSLParser.Equality_expressionContext ctx){
        Expr expr = visitRelational_expression(ctx.a);

        for (int i = 1; i < ctx.relational_expression().size(); i++) {
            JSLParser.Relational_expressionContext context = ctx.relational_expression(i);
            if (ctx.equality_operator(i - 1).EQEQ() != null) {
                expr = tm.binary(BinaryOpType.EQEQ, expr, visitRelational_expression(context));
            } else if (ctx.equality_operator(i - 1).NEQ() != null) {
                expr = tm.binary(BinaryOpType.NEQ, expr, visitRelational_expression(context));
            } else {
                throw new RuntimeException("unexpected equality operator");
            }
        }

        return expr;
    }

    @Override
    public Expr visitLogical_and_expression(JSLParser.Logical_and_expressionContext ctx) {
        Expr expr = visitEquality_expression(ctx.a);
        for (JSLParser.Equality_expressionContext eqContext :
                ctx.equality_expression().subList(1, ctx.equality_expression().size())) {
            expr = tm.binary(BinaryOpType.AND, expr, visitEquality_expression(eqContext));
        }
        return expr;
    }

    @Override
    public Expr visitLogical_xor_expression(JSLParser.Logical_xor_expressionContext ctx) {
        Expr expr = visitLogical_and_expression(ctx.a);

        for (JSLParser.Logical_and_expressionContext andContext :
                ctx.logical_and_expression().subList(1, ctx.logical_and_expression().size())) {
            expr = tm.binary(BinaryOpType.XOR, expr, visitLogical_and_expression(andContext));
        }
        return expr;
    }

    @Override
    public Expr visitLogical_or_expression(JSLParser.Logical_or_expressionContext ctx) {
        Expr expr = visitLogical_xor_expression(ctx.a);
        for (JSLParser.Logical_xor_expressionContext xorContext :
                ctx.logical_xor_expression().subList(1, ctx.logical_xor_expression().size())) {
            expr = tm.binary(BinaryOpType.OR, expr, visitLogical_xor_expression(xorContext));
        }
        return expr;
    }

    @Override
    public Expr visitConditional_expression(JSLParser.Conditional_expressionContext ctx) {
        return visitLogical_or_expression(ctx.a);
    }

    @Override
    public Expr visitAssignment_expression(JSLParser.Assignment_expressionContext ctx) {
        if (ctx.conditional_expression() != null) {
            return visitConditional_expression(ctx.c);
        } else {
            return tm.binary(BinaryOpType.forSymbol(ctx.op.getText()),
                    visitUnary_expression(ctx.a), visitAssignment_expression(ctx.b));
        }
    }

    @Override
    public Expr visitExpression(JSLParser.ExpressionContext ctx) {
        return visitAssignment_expression(ctx.e);
    }

    @Override
    public Stmt visitJump_statement(JSLParser.Jump_statementContext ctx) {
        if (ctx.CONTINUE() != null) {
            return tm.continueStmt();
        } else if (ctx.BREAK() != null) {
            return tm.breakStmt();
        } else if (ctx.DISCARD() != null) {
            return tm.discardStmt();
        } else if (ctx.RETURN() != null) {
            if (ctx.e != null) {
                return tm.returnStmt(visitExpression(ctx.e));
            } else {
                return tm.returnStmt(null);
            }
        }

        throw new RuntimeException("invalid jump statement");
    }

    @Override
    public FullySpecifiedTypeExpr visitFully_specified_type(JSLParser.Fully_specified_typeContext ctx) {
        return new FullySpecifiedTypeExpr(
                ctx.type_qualifier() != null ? Qualifier.fromToken(ctx.tq.getText()) : null,
                ctx.type_precision() != null ? Precision.fromToken(ctx.tp.getText()) : null,
                ctx.type_specifier() != null ? Type.fromToken(ctx.ts.getText()) : null);
    }

    @Override
    public Stmt visitSelection_statement(JSLParser.Selection_statementContext ctx) {
        return tm.selectStmt(visitExpression(ctx.e), visitStatement(ctx.a),
                ctx.b != null ? visitStatement(ctx.b) : null);
    }

    @Override
    public Stmt visitExpression_statement(JSLParser.Expression_statementContext ctx) {
        if (ctx.e != null) {
            return tm.exprStmt(visitExpression(ctx.e));
        } else if (ctx.SEMICOLON() != null) {
            return tm.exprStmt(null);
        }
        throw new RuntimeException("invalid expression statement");
    }

    @Override
    public Expr visitConstant_expression(JSLParser.Constant_expressionContext ctx) {
        return visitConditional_expression(ctx.c);
    }

    @Override
    public Stmt visitFor_init_statement(JSLParser.For_init_statementContext ctx) {
        if (ctx.expression_statement() != null) {
            return visitExpression_statement(ctx.e);
        } else if (ctx.declaration_statement() != null) {
            return visitDeclaration_statement(ctx.d);
        }
        throw new RuntimeException("invalid for init statement");
    }

    @Override
    public Expr visitCondition(JSLParser.ConditionContext ctx) {
        if (ctx.expression() != null) {
            return visitExpression(ctx.e);
        }
        throw new RuntimeException("invalid condition");
    }

    @Override
    public Stmt visitSimple_statement(JSLParser.Simple_statementContext ctx) {
        if (ctx.declaration_statement() != null) {
            return visitDeclaration_statement(ctx.d);
        } else if (ctx.expression_statement() != null) {
            return visitExpression_statement(ctx.e);
        } else if (ctx.selection_statement() != null) {
            return visitSelection_statement(ctx.s);
        } else if (ctx.iteration_statement() != null) {
            return visitIteration_statement(ctx.i);
        } else if (ctx.jump_statement() != null) {
            return visitJump_statement(ctx.j);
        }

        throw new RuntimeException("invalid simple statement");
    }

    @Override
    public Stmt visitStatement(JSLParser.StatementContext ctx) {
        if (ctx.compound_statement() != null) {
            return visitCompound_statement(ctx.c);
        } else if (ctx.simple_statement() != null) {
            return visitSimple_statement(ctx.s);
        }

        throw new RuntimeException("invalid statement");
    }

    @Override
    public Stmt visitDeclaration_statement(JSLParser.Declaration_statementContext ctx) {
        if (ctx.declaration() != null) {
            return tm.declStmt(visitDeclaration(ctx.d).getDecls());
        }

        throw new RuntimeException("invalid declaration statement");
    }

    @Override
    public Expr visitInitializer(JSLParser.InitializerContext ctx) {
        if (ctx.assignment_expression() != null) {
            return visitAssignment_expression(ctx.e);
        }

        throw new RuntimeException("invalid initializer");
    }

    @Override
    public VarDecl visitSingle_declaration(JSLParser.Single_declarationContext ctx) {
        if (ctx.fully_specified_type() != null && ctx.declaration_identifier_and_init() != null) {
            int arraySize = -1;
            DeclIdInitExpr initExpr = visitDeclaration_identifier_and_init(ctx.d);
            FullySpecifiedTypeExpr typeExpr = visitFully_specified_type(ctx.t);
            Expr ainit = initExpr.arrayInit;
            if (ainit != null) {
                if (ainit instanceof LiteralExpr) {
                    Object val = ((LiteralExpr)ainit).getValue();
                    if (!(val instanceof Integer)) {
                        throw new RuntimeException("Array size must be an integer");
                    }
                    arraySize = (Integer) val;
                } else if (ainit instanceof VariableExpr) {
                    Variable var = ((VariableExpr)ainit).getVariable();
                    Object val = var.getConstValue();
                    if (!(val instanceof Integer) || var.getQualifier() != Qualifier.CONST) {
                        throw new RuntimeException("Array size must be a constant integer");
                    }
                    arraySize = (Integer) val;
                }
            }

            Object constValue = null;
            if (typeExpr.qual == Qualifier.CONST) {
                Expr cinit = initExpr.init;
                if (cinit == null) {
                    throw new RuntimeException("Constant value must be initialized");
                }
                // TODO: for now, allow some basic expressions on the rhs
                // of the constant declaration...
                //if (!(cinit instanceof LiteralExpr)) {
                //    throw new RuntimeException("Constant initializer must be a literal (for now)");
                //}
                Type ctype = cinit.getResultType();
                if (ctype != typeExpr.type) {
                    throw new RuntimeException("Constant type must match that of initializer");
                }
                if (cinit instanceof LiteralExpr) {
                    constValue = ((LiteralExpr)cinit).getValue();
                } else {
                    // TODO: This is gross, but to support complex constant
                    // initializers (such as "const FOO = BAR / 42.0;") we
                    // will just save the full text of the rhs and hope that
                    // the backend does the right thing with it.  The real
                    // solution obviously would be to evaluate the expression
                    // now and reduce it to a single value.
                    constValue = initExpr.init.toString();
                }
            }

            Variable var = symbols.declareVariable(initExpr.name, typeExpr.type, typeExpr.qual,
                    typeExpr.precision, arraySize, constValue);
            return tm.varDecl(var, initExpr.init);
        }

        throw new RuntimeException("invalid single declaration");
    }

    @Override
    public Stmt visitIteration_statement(JSLParser.Iteration_statementContext ctx) {
        if (ctx.WHILE() != null) {
            if (ctx.DO() != null) {
                return tm.doWhileStmt(visitStatement(ctx.s), visitExpression(ctx.e));
            } else if (ctx.condition() != null) {
                return tm.whileStmt(visitCondition(ctx.c), visitStatement_no_new_scope(ctx.snns));
            }
            return tm.whileStmt(visitCondition(ctx.c), visitStatement_no_new_scope(ctx.snns));
        } else if (ctx.FOR() != null) {
            ForRestExpr forRestExpr = visitFor_rest_statement(ctx.rem);
            if (ctx.unroll_modifier() != null) {
                ForStmt unrollStmt = visitUnroll_modifier(ctx.u);
                return tm.forStmt(visitFor_init_statement(ctx.init),
                        forRestExpr.cond, forRestExpr.expr,
                        visitStatement_no_new_scope(ctx.snns), unrollStmt.getUnrollMax(), unrollStmt.getUnrollCheck());
            } else {
                return tm.forStmt(visitFor_init_statement(ctx.init),
                        forRestExpr.cond, forRestExpr.expr,
                        visitStatement_no_new_scope(ctx.snns), -1, -1);

            }
        }

        throw new RuntimeException("invalid iteration statement");
    }

    @Override
    public Stmt visitCompound_statement(JSLParser.Compound_statementContext ctx) {
        List<Stmt> stmtList = new ArrayList<>();
        for (JSLParser.StatementContext sCtx : ctx.statement()) {
            stmtList.add(visitStatement(sCtx));
        }
        return tm.compoundStmt(stmtList);
    }

    @Override
    public Stmt visitStatement_no_new_scope(JSLParser.Statement_no_new_scopeContext ctx) {
        if (ctx.compound_statement_no_new_scope() != null) {
            return visitCompound_statement_no_new_scope(ctx.c);
        } else if (ctx.simple_statement() != null) {
            return visitSimple_statement(ctx.s);
        }
        throw new RuntimeException("invalid statement no new scope");
    }

    @Override
    public Stmt visitCompound_statement_no_new_scope(JSLParser.Compound_statement_no_new_scopeContext ctx) {
        List<Stmt> stmtList = new ArrayList<>();
        for (JSLParser.StatementContext sCtx : ctx.statement()) {
            stmtList.add(visitStatement(sCtx));
        }
        return tm.compoundStmt(stmtList);
    }

    @Override
    public ForStmt visitUnroll_modifier(JSLParser.Unroll_modifierContext ctx) {
        return new ForStmt(null, null, null, null, Integer.valueOf(ctx.m.getText()), Integer.valueOf(ctx.c.getText()));
    }

    @Override
    public ForRestExpr visitFor_rest_statement(JSLParser.For_rest_statementContext ctx) {
        if (ctx.condition() != null) {
            return new ForRestExpr(visitCondition(ctx.c), ctx.expression() != null ? visitExpression(ctx.e) : null);
        } else {
            return new ForRestExpr(null, ctx.expression() != null ? visitExpression(ctx.e) : null);
        }
    }

    @Override
    public ProgramUnit visitTranslation_unit(JSLParser.Translation_unitContext ctx) {
        List<ExtDecl> declList = new ArrayList<>();
        for (JSLParser.External_declarationContext eDeclCtx : ctx.external_declaration()) {
            declList.addAll(visitExternal_declaration(eDeclCtx).getDecls());
        }
        return tm.programUnit(declList);
    }

    @Override
    public ProgramUnit visitExternal_declaration(JSLParser.External_declarationContext ctx) {
        List<ExtDecl> res = new ArrayList<>();
        if (ctx.function_definition() != null) {
            res.add(visitFunction_definition(ctx.f));
        } else if (ctx.declaration() != null) {
            res.addAll(visitDeclaration(ctx.d).getDecls());
        } else if (ctx.glue_block() != null) {
            res.add(visitGlue_block(ctx.g));
        }
        return new ProgramUnit(res);
    }

    @Override
    public FuncDef visitFunction_definition(JSLParser.Function_definitionContext ctx) {
        symbols.enterFrame();
        FuncDef funcDef = tm.funcDef(visitFunction_prototype(ctx.p).getFunction(),
                visitCompound_statement_no_new_scope(ctx.s));
        symbols.exitFrame();
        return funcDef;
    }

    @Override
    public CallExpr visitFunction_prototype(JSLParser.Function_prototypeContext ctx) {
        Type type = Type.fromToken(ctx.t.getText());
        List<Param> params = ctx.parameter_declaration_list() != null ?
                visitParameter_declaration_list(ctx.p).paramList : null;
        Function func = symbols.declareFunction(ctx.id.getText(), type, params);
        return new CallExpr(func, null);
    }

    @Override
    public ParamExpr visitParameter_declaration(JSLParser.Parameter_declarationContext ctx) {
        Type type = Type.fromToken(ctx.t.getText());
        return new ParamExpr(new Param(ctx.IDENTIFIER().getText(), type));
    }

    @Override
    public ParamListExpr visitParameter_declaration_list(JSLParser.Parameter_declaration_listContext ctx) {
        List<Param> paramList = new ArrayList<>();
        for (JSLParser.Parameter_declarationContext pdCtx : ctx.parameter_declaration()) {
            paramList.add(visitParameter_declaration(pdCtx).param);
        }
        return new ParamListExpr(paramList);
    }

    @Override
    public DeclStmt visitDeclaration(JSLParser.DeclarationContext ctx) {
        List<VarDecl> declList = new ArrayList<>();
        VarDecl varDecl = visitSingle_declaration(ctx.s);
        declList.add(varDecl);
        for (JSLParser.Declaration_identifier_and_initContext dCtx : ctx.declaration_identifier_and_init()) {
            DeclIdInitExpr initExpr = visitDeclaration_identifier_and_init(dCtx);
            Variable base = varDecl.getVariable();
            Variable var = symbols.declareVariable(initExpr.name, base.getType(),
                    base.getQualifier(), base.getPrecision());
            declList.add(tm.varDecl(var, initExpr.init));
        }
        return new DeclStmt(declList);
    }

    @Override
    public DeclIdInitExpr visitDeclaration_identifier_and_init(JSLParser.Declaration_identifier_and_initContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        Expr arrayInit = null;
        Expr init = null;
        if (ctx.constant_expression() != null) {
            arrayInit = visitConstant_expression(ctx.ae);
        }
        if (ctx.initializer() != null) {
            init = visitInitializer(ctx.e);
        }
        return new DeclIdInitExpr(name, arrayInit, init);
    }

    @Override
    public GlueBlock visitGlue_block(JSLParser.Glue_blockContext ctx) {
        return tm.glueBlock(ctx.g.getText().substring(2, ctx.g.getText().length() - 2));
    }

}
