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

grammar JSL;

field_selection
        : r=RGBA_FIELDS
        | x=XYZW_FIELDS
        ;

primary_expression
        : IDENTIFIER
        | INTCONSTANT
        | FLOATCONSTANT
        | BOOLCONSTANT
        | LEFT_PAREN e=expression RIGHT_PAREN
        ;

primary_or_call
        : e=primary_expression
        | f=function_call
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
postfix_expression
        : e=primary_or_call LEFT_BRACKET ae=expression RIGHT_BRACKET fs=field_selection
        | e=primary_or_call LEFT_BRACKET ae=expression RIGHT_BRACKET
        | e=primary_or_call fs=field_selection
        | e=primary_or_call INC
        | e=primary_or_call DEC
        | e=primary_or_call
        ;

// From the GLSL spec...
// Grammar Note: Constructors look like functions, but lexical
// analysis recognized most of them as keywords.  They are now
// recognized through "type_specifier".

function_call
        : id=IDENTIFIER LEFT_PAREN p=function_call_parameter_list? RIGHT_PAREN
        | ts=type_specifier LEFT_PAREN p=function_call_parameter_list? RIGHT_PAREN
        ;

function_call_parameter_list
        : a=assignment_expression
          (COMMA a=assignment_expression
          )*
        ;

unary_expression
        : p=postfix_expression
        | INC   u=unary_expression
        | DEC   u=unary_expression
        | PLUS  u=unary_expression
        | DASH  u=unary_expression
        | BANG  u=unary_expression
        ;

// From the GLSL spec...
// Grammar Note:  No traditional style type casts.

// From the GLSL spec...
// Grammar Note:  No '*' or '&' unary ops.  Pointers are not supported.

multiplicative_operator
        : STAR
        | SLASH
        ;

multiplicative_expression
        : a=unary_expression
          (c=multiplicative_operator b=multiplicative_expression)*
        ;

additive_operator
        : PLUS
        | DASH
        ;

additive_expression
        : a=multiplicative_expression
          (c=additive_operator b=multiplicative_expression)*
        ;

relational_operator
        : LTEQ
        | GTEQ
        | LT
        | GT
        ;

relational_expression
        : a=additive_expression
          (c=relational_operator b=additive_expression)*
        ;

equality_operator
        : EQEQ
        | NEQ
        ;

equality_expression
        : a=relational_expression
          (c=equality_operator b=relational_expression)*
        ;

logical_and_expression
        : a=equality_expression
          (AND b=equality_expression)*
        ;

logical_xor_expression
        : a=logical_and_expression
          (XOR b=logical_and_expression)*
        ;

logical_or_expression
        : a=logical_xor_expression
          (OR b=logical_xor_expression)*
        ;

ternary_part
        : QUESTION expression COLON assignment_expression
        ;

// TODO: handle ternary
conditional_expression
        : a=logical_or_expression ternary_part?
        ;

assignment_expression
        : a=unary_expression op=assignment_operator b=assignment_expression
        | c=conditional_expression
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

expression
        : e=assignment_expression
        ;

function_prototype
        : t=type_specifier id=IDENTIFIER LEFT_PAREN p=parameter_declaration_list? RIGHT_PAREN
        ;

parameter_declaration
        : t=type_specifier id=IDENTIFIER
        ;

parameter_declaration_list
        : p=parameter_declaration
          (COMMA p=parameter_declaration)*
        ;

declaration_identifier_and_init
        : id=IDENTIFIER
          (LEFT_BRACKET ae=constant_expression RIGHT_BRACKET)?
          (EQUAL e=initializer)?
        ;

single_declaration
        : t=fully_specified_type d=declaration_identifier_and_init
        ;

declaration
        : s=single_declaration
          (COMMA d=declaration_identifier_and_init)* SEMICOLON
        ;

// From GLSL spec...
// Grammar Note:  No 'enum', or 'typedef'.

fully_specified_type
        : tq=type_qualifier tp=type_precision ts=type_specifier
        | tq=type_qualifier ts=type_specifier
        | tp=type_precision ts=type_specifier
        | ts=type_specifier
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

initializer
        : e=assignment_expression
        ;

declaration_statement
        : d=declaration
        ;

statement
        : c=compound_statement
        | s=simple_statement
        ;

// From GLSL spec...
// Grammar Note:  No labeled statements; 'goto' is not supported.

simple_statement
        : d=declaration_statement
        | e=expression_statement
        | s=selection_statement
        | i=iteration_statement
        | j=jump_statement
        ;

compound_statement
        : LEFT_BRACE (s=statement)* RIGHT_BRACE
        ;

statement_no_new_scope
        : c=compound_statement_no_new_scope
        | s=simple_statement
        ;

compound_statement_no_new_scope
        : LEFT_BRACE (s=statement)* RIGHT_BRACE
        ;

expression_statement
        : SEMICOLON
        | e=expression SEMICOLON
        ;

constant_expression
        : c=conditional_expression
        ;

selection_statement
        : IF LEFT_PAREN e=expression RIGHT_PAREN a=statement (ELSE b=statement)?
        ;

// TODO: implement second half?
condition
        : e=expression
//        | fully_specified_type IDENTIFIER EQUAL initializer
        ;

iteration_statement
        : WHILE LEFT_PAREN c=condition RIGHT_PAREN snns=statement_no_new_scope
        | DO s=statement WHILE LEFT_PAREN e=expression RIGHT_PAREN SEMICOLON
        | u=unroll_modifier FOR LEFT_PAREN init=for_init_statement rem=for_rest_statement RIGHT_PAREN snns=statement_no_new_scope
        | FOR LEFT_PAREN init=for_init_statement rem=for_rest_statement RIGHT_PAREN snns=statement_no_new_scope
        ;

unroll_modifier
        : UNROLL LEFT_PAREN m=INTCONSTANT COMMA c=INTCONSTANT RIGHT_PAREN
        ;

for_init_statement
        : e=expression_statement
        | d=declaration_statement
        ;

for_rest_statement
        : c=condition SEMICOLON e=expression?
        | SEMICOLON e=expression?
        ;

jump_statement
        : CONTINUE SEMICOLON
        | BREAK SEMICOLON
        | DISCARD SEMICOLON
        | RETURN SEMICOLON
        | RETURN e=expression SEMICOLON
        ;

// From GLSL spec...
// Grammar Note:  No 'goto'.  Gotos are not supported.

translation_unit
        : (e=external_declaration)+
        ;

external_declaration
        : f=function_definition
        | d=declaration
        | g=glue_block
        ;

// From GLSL spec...
// Grammar Note:  No 'switch'.  Switch statements not supported.

function_definition
        : p=function_prototype s=compound_statement_no_new_scope
        ;

glue_block
        : g=GLUE_BLOCK
        ;

STAR : '*';
SLASH : '/';
PLUS : '+';
DASH : '-';
LT : '<';
GT : '>';
LTEQ : '<=';
GTEQ : '>=';
EQEQ : '==';
NEQ : '!=';
AND : '&&';
XOR : '^^';
OR : '||';
INC : '++';
DEC : '--';
STAREQ : '*=';
SLASHEQ : '/=';
PLUSEQ : '+=';
DASHEQ : '-=';
LEFT_PAREN : '(';
RIGHT_PAREN : ')';
LEFT_BRACKET : '[';
RIGHT_BRACKET : ']';
LEFT_BRACE : '{';
RIGHT_BRACE : '}';
LEFT_FRENCH : '<<';
RIGHT_FRENCH : '>>';
DOT : '.';
COMMA : ',';
EQUAL : '=';
BANG : '!';
TILDE : '~';
QUESTION : '?';
COLON : ':';
SEMICOLON : ';';
IF : 'if';
ELSE : 'else';
WHILE : 'while';
DO : 'do';
FOR : 'for';
UNROLL : 'unroll';
CONTINUE : 'continue';
BREAK : 'break';
DISCARD : 'discard';
RETURN : 'return';
VOID : 'void';

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

WS  :  (' '|'\r'|'\t'|'\u000C'|'\n') -> channel(HIDDEN)
    ;

COMMENT
    :   '/*' (.)*? '*/' -> channel(HIDDEN)
    ;

LINE_COMMENT
    : '//' ~('\n'|'\r')* '\r'? '\n' -> channel(HIDDEN)
    ;

GLUE_BLOCK
    : LEFT_FRENCH .* RIGHT_FRENCH
    ;
