/*
 * Copyright (c) 2008, 2013 Oracle and/or its affiliates. All rights reserved.
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

grammar JSL;

options {
    backtrack=true;
}

tokens {
    STAR  = '*'  ;
    SLASH = '/'  ;
    PLUS  = '+'  ;
    DASH  = '-'  ;
    LT    = '<'  ;
    GT    = '>'  ;
    LTEQ  = '<=' ;
    GTEQ  = '>=' ;
    EQEQ  = '==' ;
    NEQ   = '!=' ;
    AND   = '&&' ;
    XOR   = '^^' ;
    OR    = '||' ;
    INC   = '++' ;
    DEC   = '--' ;

    STAREQ  = '*=' ;
    SLASHEQ = '/=' ;
    PLUSEQ  = '+=' ;
    DASHEQ  = '-=' ;

    LEFT_PAREN    = '(' ;
    RIGHT_PAREN   = ')' ;
    LEFT_BRACKET  = '[' ;
    RIGHT_BRACKET = ']' ;
    LEFT_BRACE    = '{' ;
    RIGHT_BRACE   = '}' ;

    LEFT_FRENCH   = '<<' ;
    RIGHT_FRENCH  = '>>' ;

    DOT           = '.' ;
    COMMA         = ',' ;
    EQUAL         = '=' ;
    BANG          = '!' ;
    TILDE         = '~' ;
    QUESTION      = '?' ;
    COLON         = ':' ;
    SEMICOLON     = ';' ;

    IF    = 'if'    ;
    ELSE  = 'else'  ;
    WHILE = 'while' ;
    DO    = 'do'    ;
    FOR   = 'for'   ;

    UNROLL = 'unroll' ;

    CONTINUE = 'continue' ;
    BREAK    = 'break'    ;
    DISCARD  = 'discard'  ;
    RETURN   = 'return'   ;

    VOID = 'void' ;
}

@header {
    package com.sun.scenario.effect.compiler;

    import com.sun.scenario.effect.compiler.model.*;
    import com.sun.scenario.effect.compiler.tree.*;
}

@lexer::header {
    package com.sun.scenario.effect.compiler;
}

@lexer::members {
    // allow tests to turn on quiet mode, to reduce spewage
    public static boolean quiet;

    public void emitErrorMessage(String error) {
        if (quiet) return;
        super.emitErrorMessage(error);
    }
}

@members {
    private SymbolTable symbols = new SymbolTable();
    private TreeMaker tm = new TreeMaker(symbols);

    public SymbolTable getSymbolTable() {
        return symbols;
    }

    // fail on first error for now
    // TODO: collect errors and recover...
    protected void mismatch(IntStream input, int tokenType, BitSet follow) throws RecognitionException {
        MismatchedTokenException ex = new MismatchedTokenException(tokenType, input);
        System.err.println("Token mismatch at " + ex.line + ":" + ex.charPositionInLine);
        throw ex;
    }
    
    public void recoverFromMismatchedSet(IntStream input, int ttype, BitSet follow)
        throws RecognitionException {
        throw new MissingTokenException(ttype, input, null);
    }
}

@rulecatch {
    catch (RecognitionException ex) {
        throw ex;
    }
}

field_selection returns [String fields]
        : r=RGBA_FIELDS { $fields = $r.text; }
        | x=XYZW_FIELDS { $fields = $x.text; }
        ;

primary_expression returns [Expr expr]
        : IDENTIFIER    { $expr = tm.variable($IDENTIFIER.text); }
        | INTCONSTANT   { $expr = tm.literal(Type.INT, Integer.valueOf($INTCONSTANT.text)); }
        | FLOATCONSTANT { $expr = tm.literal(Type.FLOAT, Float.valueOf($FLOATCONSTANT.text)); }
        | BOOLCONSTANT  { $expr = tm.literal(Type.BOOL, Boolean.valueOf($BOOLCONSTANT.text)); }
        | LEFT_PAREN e=expression RIGHT_PAREN { $expr = tm.parenExpr($e.expr); }
        ;

primary_or_call returns [Expr expr]
        : e=primary_expression { $expr = $e.expr; }
        | f=function_call      { $expr = $f.expr; }
        ;

//
// TODO: not sure how to do this properly without mutual left-recursion;
// for now we hack it to allow:
//   arr[3].rgb
//   arr[3]
//   val.rgb
//   val++
//   val--
//   val
// but not things like:
//   arr[3].r++
//
postfix_expression returns [Expr expr]
        : e=primary_or_call LEFT_BRACKET ae=expression RIGHT_BRACKET fs=field_selection
              { $expr = tm.fieldSelect(tm.arrayAccess($e.expr, $ae.expr), $fs.fields); }
        | e=primary_or_call LEFT_BRACKET ae=expression RIGHT_BRACKET
              { $expr = tm.arrayAccess($e.expr, $ae.expr); }
        | e=primary_or_call fs=field_selection
              { $expr = tm.fieldSelect($e.expr, $fs.fields); }
        | e=primary_or_call INC
              { $expr = tm.unary(UnaryOpType.INC, $e.expr); }
        | e=primary_or_call DEC
              { $expr = tm.unary(UnaryOpType.DEC, $e.expr); }
        | e=primary_or_call
              { $expr = $e.expr; }
        ;

// From the GLSL spec...
// Grammar Note: Constructors look like functions, but lexical
// analysis recognized most of them as keywords.  They are now
// recognized through "type_specifier".

function_call returns [Expr expr]
        : id=IDENTIFIER LEFT_PAREN p=function_call_parameter_list? RIGHT_PAREN
            {
                $expr = tm.call($id.text, p!=null ? $p.exprList : null);
            }
        | ts=type_specifier LEFT_PAREN p=function_call_parameter_list? RIGHT_PAREN
            {
                Type type = Type.fromToken($ts.text);
                $expr = tm.vectorCtor(type, p!=null ? $p.exprList : null);
            }
        ;
        
function_call_parameter_list returns [List<Expr> exprList = new ArrayList<Expr>()]
        : a=assignment_expression { $exprList.add($a.expr); }
          (COMMA a=assignment_expression {$exprList.add($a.expr); }
          )*
        ;
        
unary_expression returns [Expr expr]
        : p=postfix_expression     { $expr = $p.expr; }
        | INC   u=unary_expression { $expr = tm.unary(UnaryOpType.INC,     $u.expr); }
        | DEC   u=unary_expression { $expr = tm.unary(UnaryOpType.DEC,     $u.expr); }
        | PLUS  u=unary_expression { $expr = tm.unary(UnaryOpType.PLUS,    $u.expr); }
        | DASH  u=unary_expression { $expr = tm.unary(UnaryOpType.MINUS,   $u.expr); }
        | BANG  u=unary_expression { $expr = tm.unary(UnaryOpType.NOT,     $u.expr); }
        ;

// From the GLSL spec...
// Grammar Note:  No traditional style type casts.

// From the GLSL spec...
// Grammar Note:  No '*' or '&' unary ops.  Pointers are not supported.

multiplicative_expression returns [Expr expr]
        : a=unary_expression { $expr = $a.expr; }
          (STAR  b=multiplicative_expression { $expr = tm.binary(BinaryOpType.MUL, $expr, $b.expr); }
          |SLASH b=multiplicative_expression { $expr = tm.binary(BinaryOpType.DIV, $expr, $b.expr); }
          )*
        ;
        
additive_expression returns [Expr expr]
        : a=multiplicative_expression { $expr = $a.expr; }
          (PLUS b=multiplicative_expression { $expr = tm.binary(BinaryOpType.ADD, $expr, $b.expr); }
          |DASH b=multiplicative_expression { $expr = tm.binary(BinaryOpType.SUB, $expr, $b.expr); }
          )*
        ;

relational_expression returns [Expr expr]
        : a=additive_expression { $expr = $a.expr; }
          (LTEQ b=additive_expression { $expr = tm.binary(BinaryOpType.LTEQ, $expr, $b.expr); }
          |GTEQ b=additive_expression { $expr = tm.binary(BinaryOpType.GTEQ, $expr, $b.expr); }
          |LT   b=additive_expression { $expr = tm.binary(BinaryOpType.LT,   $expr, $b.expr); }
          |GT   b=additive_expression { $expr = tm.binary(BinaryOpType.GT,   $expr, $b.expr); }
          )*
        ;

equality_expression returns [Expr expr]
        : a=relational_expression { $expr = $a.expr; }
          (EQEQ b=relational_expression { $expr = tm.binary(BinaryOpType.EQEQ, $expr, $b.expr); }
          | NEQ b=relational_expression { $expr = tm.binary(BinaryOpType.NEQ,  $expr, $b.expr); }
          )*
        ;
        
logical_and_expression returns [Expr expr]
        : a=equality_expression { $expr = $a.expr; }
          (AND b=equality_expression { $expr = tm.binary(BinaryOpType.AND, $expr, $b.expr); }
          )*
        ;
        
logical_xor_expression returns [Expr expr]
        : a=logical_and_expression { $expr = $a.expr; }
          (XOR b=logical_and_expression { $expr = tm.binary(BinaryOpType.XOR, $expr, $b.expr); }
          )*
        ;
        
logical_or_expression returns [Expr expr]
        : a=logical_xor_expression { $expr = $a.expr; }
          (OR b=logical_xor_expression { $expr = tm.binary(BinaryOpType.OR, $expr, $b.expr); }
          )*
        ;
        
ternary_part
        : QUESTION expression COLON assignment_expression
        ;

// TODO: handle ternary
conditional_expression returns [Expr expr]
        : a=logical_or_expression ternary_part? { $expr = $a.expr; }
        ;

assignment_expression returns [Expr expr]
        : a=unary_expression op=assignment_operator b=assignment_expression
              { $expr = tm.binary(BinaryOpType.forSymbol($op.text), $a.expr, $b.expr); }
        | c=conditional_expression
              { $expr = $c.expr; }
        ;

assignment_operator
        : EQUAL
        | STAREQ
        | SLASHEQ
        | PLUSEQ
        | DASHEQ
        ;

// TODO: handle expression lists?
//expression returns [List<Expr> exprList = new ArrayList<Expr>()]
//        : e=assignment_expression { $exprList.add($e.expr); }
//          (COMMA e=assignment_expression { $exprList.add($e.expr); })*
//        ;

expression returns [Expr expr]
        : e=assignment_expression { $expr = $e.expr; }
        ;

function_prototype returns [Function func]
        : t=type_specifier id=IDENTIFIER LEFT_PAREN p=parameter_declaration_list? RIGHT_PAREN
            {
                Type type = Type.fromToken($t.text);
                $func = symbols.declareFunction($id.text, type, (p != null) ? $p.paramList : null);
            }
        ;
        
parameter_declaration returns [Param param]
        : t=type_specifier id=IDENTIFIER
            {
                Type type = Type.fromToken($t.text);
                $param = new Param($id.text, type);
            }
        ;

parameter_declaration_list returns [List<Param> paramList = new ArrayList<Param>()]
        : p=parameter_declaration { $paramList.add($p.param); }
          (COMMA p=parameter_declaration { $paramList.add($p.param); } )*
        ;
        
declaration_identifier_and_init returns [String name, Expr arrayInit, Expr init]
        : id=IDENTIFIER { $name = $id.text; }
          (LEFT_BRACKET ae=constant_expression { $arrayInit = $ae.expr; } RIGHT_BRACKET)?
          (EQUAL e=initializer { $init = $e.expr; })?
        ;

single_declaration returns [VarDecl decl]
        : t=fully_specified_type d=declaration_identifier_and_init
          {
              int arraySize = -1;
              Expr ainit = $d.arrayInit;
              if (ainit != null) {
                  if (ainit instanceof LiteralExpr) {
                      Object val = ((LiteralExpr)ainit).getValue();
                      if (!(val instanceof Integer)) {
                          throw new RuntimeException("Array size must be an integer");
                      }
                      arraySize = ((Integer)val).intValue();
                  } else if (ainit instanceof VariableExpr) {
                      Variable var = ((VariableExpr)ainit).getVariable();
                      Object val = var.getConstValue();
                      if (!(val instanceof Integer) || var.getQualifier() != Qualifier.CONST) {
                          throw new RuntimeException("Array size must be a constant integer");
                      }
                      arraySize = ((Integer)val).intValue();
                  }
              }

              Object constValue = null;
              if ($t.qual == Qualifier.CONST) {
                  Expr cinit = $d.init;
                  if (cinit == null) {
                      throw new RuntimeException("Constant value must be initialized");
                  }
                  // TODO: for now, allow some basic expressions on the rhs
                  // of the constant declaration...
                  //if (!(cinit instanceof LiteralExpr)) {
                  //    throw new RuntimeException("Constant initializer must be a literal (for now)");
                  //}
                  Type ctype = cinit.getResultType();
                  if (ctype != $t.type) {
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
                      constValue = $d.init.toString();
                  }
              }

              Variable var =
                  symbols.declareVariable($d.name,
                                          $t.type, $t.qual, $t.precision,
                                          arraySize, constValue);
              $decl = tm.varDecl(var, $d.init);
          }
        ;
        
declaration returns [List<VarDecl> declList = new ArrayList<VarDecl>()]
        : s=single_declaration { $declList.add($s.decl); }
          (COMMA d=declaration_identifier_and_init
          {
              Variable base = $s.decl.getVariable();
              Variable var =
                  symbols.declareVariable($d.name,
                                          base.getType(),
                                          base.getQualifier(),
                                          base.getPrecision());
              $declList.add(tm.varDecl(var, $d.init));
          }
          )* SEMICOLON
        ;
        
// From GLSL spec...
// Grammar Note:  No 'enum', or 'typedef'. 

fully_specified_type returns [Qualifier qual, Precision precision, Type type]
        : tq=type_qualifier tp=type_precision ts=type_specifier
            {
                $qual = Qualifier.fromToken($tq.text);
                $precision = Precision.fromToken($tp.text);
                $type = Type.fromToken($ts.text);
            }
        | tq=type_qualifier ts=type_specifier
            {
                $qual = Qualifier.fromToken($tq.text);
                $type = Type.fromToken($ts.text);
            }
        | tp=type_precision ts=type_specifier
            {
                $precision = Precision.fromToken($tp.text);
                $type = Type.fromToken($ts.text);
            }
        | ts=type_specifier
            {
                $type = Type.fromToken($ts.text);
            }
        ;
        
type_qualifier
        : 'const'
        | 'param'
        ;

type_precision
        : 'lowp'
        | 'mediump'
        | 'highp'
        ;
        
type_specifier
        : type_specifier_nonarray array_brackets?
        ;
        
array_brackets
        : LEFT_BRACKET constant_expression RIGHT_BRACKET
        ;
       
type_specifier_nonarray
        : TYPE
        | VOID
        ;
        
initializer returns [Expr expr]
        : e=assignment_expression { $expr = $e.expr; }
        ;
        
declaration_statement returns [Stmt stmt]
        : d=declaration { $stmt = tm.declStmt($d.declList); }
        ;
        
statement returns [Stmt stmt]
        : c=compound_statement { $stmt = $c.stmt; }
        | s=simple_statement   { $stmt = $s.stmt; }
        ;

// From GLSL spec...
// Grammar Note:  No labeled statements; 'goto' is not supported. 

simple_statement returns [Stmt stmt]
        : d=declaration_statement { $stmt = $d.stmt; }
        | e=expression_statement  { $stmt = $e.stmt; }
        | s=selection_statement   { $stmt = $s.stmt; }
        | i=iteration_statement   { $stmt = $i.stmt; }
        | j=jump_statement        { $stmt = $j.stmt; }
        ;
        
compound_statement returns [Stmt stmt]
@init {
    List<Stmt> stmtList = new ArrayList<Stmt>();
}
        : LEFT_BRACE (s=statement { stmtList.add($s.stmt); })* RIGHT_BRACE
          { $stmt = tm.compoundStmt(stmtList); }
        ;
        
statement_no_new_scope returns [Stmt stmt]
        : c=compound_statement_no_new_scope { $stmt = $c.stmt; }
        | s=simple_statement                { $stmt = $s.stmt; }
        ;
        
compound_statement_no_new_scope returns [Stmt stmt]
@init {
    List<Stmt> stmtList = new ArrayList<Stmt>();
}
        : LEFT_BRACE (s=statement { stmtList.add($s.stmt); })* RIGHT_BRACE
          { $stmt = tm.compoundStmt(stmtList); }
        ;
        
expression_statement returns [Stmt stmt]
        : SEMICOLON              { $stmt = tm.exprStmt(null); }
        | e=expression SEMICOLON { $stmt = tm.exprStmt($e.expr); }
        ;
        
constant_expression returns [Expr expr]
        : c=conditional_expression { $expr = $c.expr; }
        ;

selection_statement returns [Stmt stmt]
        : IF LEFT_PAREN e=expression RIGHT_PAREN a=statement (ELSE b=statement)?
              { $stmt = tm.selectStmt($e.expr, $a.stmt, (b != null) ? $b.stmt : null); }
        ;

// TODO: implement second half?
condition returns [Expr expr]
        : e=expression {$expr = $e.expr; }
//        | fully_specified_type IDENTIFIER EQUAL initializer
        ;

iteration_statement returns [Stmt stmt]
        : WHILE LEFT_PAREN c=condition RIGHT_PAREN snns=statement_no_new_scope
              { $stmt = tm.whileStmt($c.expr, $snns.stmt); }
        | DO s=statement WHILE LEFT_PAREN e=expression RIGHT_PAREN SEMICOLON
              { $stmt = tm.doWhileStmt($s.stmt, $e.expr); }
        | u=unroll_modifier FOR LEFT_PAREN init=for_init_statement rem=for_rest_statement RIGHT_PAREN snns=statement_no_new_scope
              { $stmt = tm.forStmt($init.stmt, $rem.cond, $rem.expr, $snns.stmt, $u.max, $u.check); }
        | FOR LEFT_PAREN init=for_init_statement rem=for_rest_statement RIGHT_PAREN snns=statement_no_new_scope
              { $stmt = tm.forStmt($init.stmt, $rem.cond, $rem.expr, $snns.stmt, -1, -1); }
        ;

unroll_modifier returns [int max, int check]
        : UNROLL LEFT_PAREN m=INTCONSTANT COMMA c=INTCONSTANT RIGHT_PAREN
              { $max = Integer.valueOf($m.text); $check = Integer.valueOf($c.text); }
        ;

for_init_statement returns [Stmt stmt]
        : e=expression_statement  { $stmt = $e.stmt; }
        | d=declaration_statement { $stmt = $d.stmt; }
        ;
        
for_rest_statement returns [Expr cond, Expr expr]
        : c=condition SEMICOLON e=expression? { $cond = $c.expr; if (e != null) $expr = $e.expr; }
        | SEMICOLON e=expression? { if (e != null) $expr = $e.expr; }
        ;
        
jump_statement returns [Stmt stmt]
        : CONTINUE SEMICOLON            { $stmt = tm.continueStmt(); }
        | BREAK SEMICOLON               { $stmt = tm.breakStmt(); }
        | DISCARD SEMICOLON             { $stmt = tm.discardStmt(); }
        | RETURN SEMICOLON              { $stmt = tm.returnStmt(null); }
        | RETURN e=expression SEMICOLON { $stmt = tm.returnStmt($e.expr); }
        ;
        
// From GLSL spec...
// Grammar Note:  No 'goto'.  Gotos are not supported. 

translation_unit returns [ProgramUnit prog]
@init {
    List<ExtDecl> declList = new ArrayList<ExtDecl>();
}
        : (e=external_declaration { declList.addAll($e.res); } )+
            { $prog = tm.programUnit(declList); }
        ;
        
external_declaration returns [List<ExtDecl> res = new ArrayList<ExtDecl>()]
        : f=function_definition { $res.add($f.def); }
        | d=declaration         { $res.addAll($d.declList); }
        | g=glue_block          { $res.add($g.block); }
        ;

// From GLSL spec...
// Grammar Note:  No 'switch'.  Switch statements not supported. 

function_definition returns [FuncDef def]
@init {
	symbols.enterFrame();
}
        : p=function_prototype s=compound_statement_no_new_scope { $def = tm.funcDef($p.func, $s.stmt); }
        ;
finally {
        symbols.exitFrame();
}

glue_block returns [GlueBlock block]
        : g=GLUE_BLOCK { $block = tm.glueBlock($g.text.substring(2, $g.text.length()-2)); }
        ;

TYPE
        : 'float2'
        | 'float3'
        | 'float4'
        | 'float'
        | 'int2'
        | 'int3'
        | 'int4'
        | 'int'
        | 'bool2'
        | 'bool3'
        | 'bool4'
        | 'bool'
        | 'sampler'
        | 'lsampler'
        | 'fsampler'
        ;

BOOLCONSTANT
        : 'true'
        | 'false'
        ;

RGBA_FIELDS
        : DOT RFIELD RFIELD RFIELD RFIELD
        | DOT RFIELD RFIELD RFIELD
        | DOT RFIELD RFIELD
        | DOT RFIELD
        ;

fragment
RFIELD   : 'r' | 'g' | 'b' | 'a' ;

XYZW_FIELDS
        : DOT XFIELD XFIELD XFIELD XFIELD
        | DOT XFIELD XFIELD XFIELD
        | DOT XFIELD XFIELD
        | DOT XFIELD
        ;

fragment
XFIELD   : 'x' | 'y' | 'z' | 'w' ;

IDENTIFIER
        : LETTER (LETTER|DIGIT)*
        ;

fragment
LETTER
        : '$'
        | 'A'..'Z'
        | 'a'..'z'
        | '_'
        ;

INTCONSTANT : ('0' | '1'..'9' DIGIT*) ;

FLOATCONSTANT
        : DIGIT+ '.' DIGIT*
        |  '.' DIGIT+
	;

fragment
DIGIT   : '0'..'9' ;

WS  :  (' '|'\r'|'\t'|'\u000C'|'\n') {$channel=HIDDEN;}
    ;

COMMENT
    :   '/*' ( options {greedy=false;} : . )* '*/' {$channel=HIDDEN;}
    ;

LINE_COMMENT
    : '//' ~('\n'|'\r')* '\r'? '\n' {$channel=HIDDEN;}
    ;

GLUE_BLOCK
    : LEFT_FRENCH .* RIGHT_FRENCH
    ;
