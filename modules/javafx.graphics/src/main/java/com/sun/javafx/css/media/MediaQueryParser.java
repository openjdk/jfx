/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css.media;

import com.sun.javafx.css.media.expression.ConjunctionExpression;
import com.sun.javafx.css.media.expression.ConstantExpression;
import com.sun.javafx.css.media.expression.DisjunctionExpression;
import com.sun.javafx.css.media.expression.NegationExpression;
import com.sun.javafx.css.parser.CssLexer;
import com.sun.javafx.css.parser.TokenStream;
import com.sun.javafx.css.parser.Token;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Parser for the media query grammar.
 *
 * @see <a href="https://www.w3.org/TR/mediaqueries-5/#mq-syntax">Media query syntax</a>
 */
public final class MediaQueryParser {

    private static final Predicate<Token> IDENT = token -> token.getType() == CssLexer.IDENT;
    private static final Predicate<Token> LPAREN = token -> token.getType() == CssLexer.LPAREN;
    private static final Predicate<Token> RPAREN = token -> token.getType() == CssLexer.RPAREN;
    private static final Predicate<Token> COLON = token -> token.getType() == CssLexer.COLON;
    private static final Predicate<Token> NOT_COMMA = token -> token.getType() != CssLexer.COMMA;
    private static final Predicate<Token> NOT_KEYWORD = token -> equalsIdentIgnoreCase(token, "not");
    private static final Predicate<Token> AND_KEYWORD = token -> equalsIdentIgnoreCase(token, "and");
    private static final Predicate<Token> OR_KEYWORD = token -> equalsIdentIgnoreCase(token, "or");
    private static final Predicate<Token> GREATER = token -> token.getType() == CssLexer.GREATER;
    private static final Predicate<Token> LESS = token -> token.getType() == CssLexer.LESS;
    private static final Predicate<Token> EQUALS = token -> token.getType() == CssLexer.EQUALS;
    private static final Predicate<Token> GREATER_LESS_EQUALS = token -> switch (token.getType()) {
        case CssLexer.GREATER, CssLexer.LESS, CssLexer.EQUALS -> true;
        default -> false;
    };
    private static final Predicate<Token> LENGTH = token -> switch (token.getType()) {
        case CssLexer.NUMBER, CssLexer.CM, CssLexer.EMS, CssLexer.EXS, CssLexer.IN, CssLexer.MM,
             CssLexer.PC, CssLexer.PT, CssLexer.PX -> true;
        default -> false;
    };

    private final BiConsumer<Token, String> errorHandler;

    /**
     * Creates a new {@code MediaQueryParser} instance.
     */
    public MediaQueryParser(BiConsumer<Token, String> errorHandler) {
        this.errorHandler = Objects.requireNonNull(errorHandler);
    }

    /**
     * Parses a {@code media-query-list} production.
     *
     * <pre>{@code
     *     <media-query-list> = <media-condition> [ , <media-condition> ]*
     * }</pre>
     *
     * @param tokens the token stream
     * @return the expression
     */
    public List<MediaQuery> parseMediaQueryList(List<Token> tokens) {
        var stream = new TokenStream(tokens);
        var expressions = new ArrayList<MediaQuery>();
        boolean lastWasComma = false;

        while (stream.consume() != null) {
            switch (stream.current().getType()) {
                case CssLexer.COMMA -> {
                    if (lastWasComma || expressions.isEmpty()) {
                        errorHandler.accept(stream.current(), "Unexpected token");
                    }

                    lastWasComma = true;
                }

                case CssLexer.IDENT, CssLexer.LPAREN -> {
                    lastWasComma = false;
                    stream.reconsume();
                    MediaQuery expression = parseMediaCondition(stream);
                    if (expression != null) {
                        expressions.add(expression);
                    } else {
                        while (stream.consumeIf(NOT_COMMA) != null) {
                            // If the expression is null, this means that we have encountered a parse error.
                            // Skip forward to the next comma and resume parsing with the next media query.
                        }

                        // Invalid expressions always evaluate to false.
                        expressions.add(ConstantExpression.of(false));
                    }
                }

                default -> {
                    errorHandler.accept(stream.current(), "Unexpected token");
                    return expressions;
                }
            }
        }

        return expressions;
    }

    /**
     * Parses a {@code media-condition} production.
     *
     * <pre>{@code
     *     <media-condition> = <media-not> | <media-in-parens> [ <media-and>* | <media-or>* ]
     *     <media-not> = not <media-in-parens>
     * }</pre>
     *
     * @param tokens the token stream
     * @return the expression
     */
    private MediaQuery parseMediaCondition(TokenStream tokens) {
        // <media-not>
        if (tokens.consumeIf(NOT_KEYWORD) != null) {
            MediaQuery mediaInParens = parseMediaInParens(tokens);
            return mediaInParens != null ? NegationExpression.of(mediaInParens) : null;
        }

        List<MediaQuery> expressions = new ArrayList<>();

        // <media-in-parens>
        MediaQuery expression = parseMediaInParens(tokens);
        if (expression == null) {
            return null;
        }

        expressions.add(expression);

        // <media-and>*
        if (!parseRepeatingMediaCondition(tokens, expressions, AND_KEYWORD, OR_KEYWORD)) {
            return null;
        }

        if (expressions.size() >= 2) {
            return ConjunctionExpression.of(expressions);
        }

        // <media-or>*
        if (!parseRepeatingMediaCondition(tokens, expressions, OR_KEYWORD, AND_KEYWORD)) {
            return null;
        }

        if (expressions.size() >= 2) {
            return DisjunctionExpression.of(expressions);
        }

        return expressions.getFirst();
    }

    /**
     * Parses a repeating {@code media-and} or {@code media-or} production.
     *
     * <pre>{@code
     *     <media-and> = and <media-in-parens>
     *     <media-or> = or <media-in-parens>
     * }</pre>
     *
     * @param tokens the token stream
     * @return {@code true} if at least one {@code media-and} or {@code media-or} production was parsed
     */
    private boolean parseRepeatingMediaCondition(TokenStream tokens,
                                                 List<MediaQuery> expressions,
                                                 Predicate<Token> keyword,
                                                 Predicate<Token> otherKeyword) {
        while (tokens.consumeIf(keyword) != null) {
            MediaQuery expression = parseMediaInParens(tokens);
            if (expression == null) {
                return false;
            }

            expressions.add(expression);
        }

        if (expressions.size() >= 2) {
            Token nextToken = tokens.peek();
            if (nextToken != null && otherKeyword.test(nextToken)) {
                errorHandler.accept(tokens.consume(), "Unexpected token");
                return false;
            }
        }

        return true;
    }

    /**
     * Parses a {@code media-in-parens} production.
     *
     * <pre>{@code
     *     <media-in-parens> = ( <media-condition> ) | <discrete-media-feature> | <range-media-feature>
     * }</pre>
     *
     * @param tokens the token stream
     * @return the expression
     */
    private MediaQuery parseMediaInParens(TokenStream tokens) {
        // <discrete-media-feature>
        if (tokens.matches(LPAREN, IDENT, RPAREN) || tokens.matches(LPAREN, IDENT, COLON)) {
            return parseDiscreteMediaFeature(tokens);
        }

        // <range-media-feature>
        if (tokens.matches(LPAREN, LENGTH) || tokens.matches(LPAREN, IDENT, GREATER_LESS_EQUALS)) {
            return parseRangeMediaFeature(tokens);
        }

        // ( <media-condition> )
        if (tokens.consumeIf(LPAREN) != null) {
            MediaQuery expression = parseMediaCondition(tokens);

            if (tokens.consumeIf(RPAREN) == null) {
                errorHandler.accept(tokens.consume(), "Expected RPAREN");
                return null;
            }

            return expression;
        }

        errorHandler.accept(tokens.consume(), "Expected LPAREN");
        return null;
    }

    /**
     * Parses a {@code discrete-media-feature} production.
     *
     * <pre>{@code
     *     <discrete-media-feature> = ( [ <mf-plain> | <mf-boolean> ] )
     *     <mf-plain> = <ident> : <any>
     *     <mf-boolean> = <ident>
     * }</pre>
     *
     * @param tokens the token stream
     * @return the expression
     */
    private MediaQuery parseDiscreteMediaFeature(TokenStream tokens) {
        if (tokens.consumeIf(LPAREN) == null) {
            errorHandler.accept(tokens.consume(), "Expected LPAREN");
            return null;
        }

        Token featureName = tokens.consumeIf(IDENT);
        if (featureName == null) {
            errorHandler.accept(tokens.consume(), "Expected IDENT");
            return null;
        }

        Token featureValue = null;
        if (tokens.consumeIf(COLON) != null && (featureValue = tokens.consume()) == null) {
            while (tokens.peek() != null) tokens.consume(); // Skip forward to the last token
            errorHandler.accept(tokens.current(), "Expected token");
            return null;
        }

        if (tokens.consumeIf(RPAREN) == null) {
            errorHandler.accept(tokens.consume(), "Expected RPAREN");
            return null;
        }

        try {
            return MediaFeatures.discreteQueryExpression(featureName.getText(), featureValue);
        } catch (IllegalArgumentException ex) {
            errorHandler.accept(featureValue, ex.getMessage());
            return null;
        }
    }

    /**
     * Parses a {@code range-media-feature} production.
     *
     * <pre>{@code
     *     <range-media-feature> = ( <mf-name> <mf-comparison> <mf-value>
     *                             | <mf-value> <mf-comparison> <mf-name>
     *                             | <mf-value> <mf-lt> <mf-name> <mf-lt> <mf-value>
     *                             | <mf-value> <mf-gt> <mf-name> <mf-gt> <mf-value> )
     *     <mf-name> = <ident>
     *     <mf-value> = <number> | <dimension>
     *     <mf-lt> = '<' '='?
     *     <mf-gt> = '>' '='?
     *     <mf-eq> = '='
     *     <mf-comparison> = <mf-lt> | <mf-gt> | <mf-eq>
     * }</pre>
     *
     * @param tokens the token stream
     * @return the expression
     */
    private MediaQuery parseRangeMediaFeature(TokenStream tokens) {
        class LocalMethods {
            /**
             * Parses a {@code <mf-name> <mf-comparison> <mf-value>} production.
             */
            static MediaQuery parseNameFirst(TokenStream tokens, BiConsumer<Token, String> errorHandler) {
                Token featureName = tokens.consumeIf(IDENT);
                if (featureName == null) {
                    errorHandler.accept(tokens.consume(), "Expected IDENT");
                    return null;
                }

                ComparisonOp operator = parseComparisonOp(tokens);
                if (operator == null) {
                    errorHandler.accept(tokens.consume(), "Expected <comparison>");
                    return null;
                }

                Token featureValue = tokens.consumeIf(LENGTH);
                if (featureValue == null) {
                    errorHandler.accept(tokens.consume(), "Expected <length>");
                    return null;
                }

                try {
                    return MediaFeatures.rangeQueryExpression(featureName.getText(), featureValue, operator);
                } catch (IllegalArgumentException ex) {
                    errorHandler.accept(tokens.consume(), ex.getMessage());
                    return null;
                }
            }

            /**
             * Parses any of the following productions:
             *
             * <pre>{@code
             *     1. <mf-value> <mf-comparison> <mf-name>
             *     2. <mf-value> <mf-lt> <mf-name> <mf-lt> <mf-value>
             *     3. <mf-value> <mf-gt> <mf-name> <mf-gt> <mf-value>
             * }</pre>
             */
            static MediaQuery parseValueFirst(TokenStream tokens, BiConsumer<Token, String> errorHandler) {
                Token featureValue1 = tokens.consumeIf(LENGTH);
                if (featureValue1 == null) {
                    errorHandler.accept(tokens.consume(), "Expected <length>");
                    return null;
                }

                ComparisonOp operator1 = parseComparisonOp(tokens);
                if (operator1 == null) {
                    errorHandler.accept(tokens.consume(), "Expected <comparison>");
                    return null;
                }

                Token featureName = tokens.consumeIf(IDENT);
                if (featureName == null) {
                    errorHandler.accept(tokens.consume(), "Expected IDENT");
                    return null;
                }

                // An interval query of the form X >= Y >= Z restricts the operators to the same "direction"
                // and excludes the equality operator. For example, if the first operator is LESS, the second
                // operator can only be LESS or LESS_OR_EQUAL.
                ComparisonOp operator2 = operator1 == ComparisonOp.EQUAL ? null : parseComparisonOp(tokens);
                if (operator2 == null || !operator2.isSameDirection(operator1)) {
                    try {
                        return MediaFeatures.rangeQueryExpression(
                            featureName.getText(), featureValue1, operator1.flipped());
                    } catch (IllegalArgumentException ex) {
                        errorHandler.accept(tokens.consume(), ex.getMessage());
                        return null;
                    }
                }

                Token featureValue2 = tokens.consumeIf(LENGTH);
                if (featureValue2 == null) {
                    errorHandler.accept(tokens.consume(), "Expected <length>");
                    return null;
                }

                try {
                    return MediaFeatures.rangeQueryExpression(
                        featureName.getText(),
                        featureValue1, featureValue2,
                        operator1, operator2);
                } catch (IllegalArgumentException ex) {
                    errorHandler.accept(tokens.consume(), ex.getMessage());
                    return null;
                }
            }

            static ComparisonOp parseComparisonOp(TokenStream tokens) {
                if (tokens.consumeIf(GREATER) != null) {
                    return tokens.consumeIf(EQUALS) != null
                        ? ComparisonOp.GREATER_OR_EQUAL
                        : ComparisonOp.GREATER;
                } else if (tokens.consumeIf(LESS) != null) {
                    return tokens.consumeIf(EQUALS) != null
                        ? ComparisonOp.LESS_OR_EQUAL
                        : ComparisonOp.LESS;
                } else if (tokens.consumeIf(EQUALS) != null) {
                    return ComparisonOp.EQUAL;
                } else {
                    return null;
                }
            }
        }

        if (tokens.consumeIf(LPAREN) == null) {
            errorHandler.accept(tokens.consume(), "Expected LPAREN");
            return null;
        }

        MediaQuery query = tokens.matches(LENGTH)
            ? LocalMethods.parseValueFirst(tokens, errorHandler)
            : LocalMethods.parseNameFirst(tokens, errorHandler);

        if (query != null && tokens.consumeIf(RPAREN) == null) {
            errorHandler.accept(tokens.consume(), "Expected RPAREN");
            return null;
        }

        return query;
    }

    private static boolean equalsIdentIgnoreCase(Token token, String value) {
        return token.getType() == CssLexer.IDENT && value.equalsIgnoreCase(token.getText());
    }
}
