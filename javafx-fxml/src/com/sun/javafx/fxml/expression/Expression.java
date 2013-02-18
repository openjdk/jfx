/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.fxml.expression;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.javafx.fxml.BeanAdapter;

/**
 * Abstract base class for expressions. Also provides static methods for
 * creating arithmetic and logical expressions as well as accessing namespace
 * values by key path.
 */
public abstract class Expression {
    // Expression parser class
    private static class Parser {
        public static class Token {
            public Token(TokenType type, Object value) {
                this.type = type;
                this.value = value;
            }

            public final TokenType type;
            public final Object value;

            @Override
            public String toString() {
                return value.toString();
            }
        }

        public enum TokenType {
            LITERAL,
            VARIABLE,
            FUNCTION,
            UNARY_OPERATOR,
            BINARY_OPERATOR,
            BEGIN_GROUP,
            END_GROUP
        }

        private int c = -1;
        private char[] pushbackBuffer = new char[PUSHBACK_BUFFER_SIZE];

        private static final int MAX_PRIORITY = 6;
        private static final int PUSHBACK_BUFFER_SIZE = 6;

        public Expression parse(Reader reader) throws IOException {
            LinkedList<Token> tokens = tokenize(new PushbackReader(reader, PUSHBACK_BUFFER_SIZE));

            LinkedList<Expression> stack = new LinkedList<Expression>();

            for (Token token : tokens) {
                Expression expression;
                switch (token.type) {
                    case LITERAL: {
                        expression = new LiteralExpression(token.value);
                        break;
                    }

                    case VARIABLE: {
                        expression = new VariableExpression((KeyPath)token.value);
                        break;
                    }

                    case FUNCTION: {
                        // TODO Create a new FunctionExpression type; this
                        // class will have a property of type Method that
                        // refers to a method defined by the "scope context"
                        // (e.g. an FXML document controller), which must be
                        // set prior to evaluating the expression; it will
                        // also have a list of argument Expressions
                        expression = null;
                        break;
                    }

                    case UNARY_OPERATOR: {
                        String operator = (String)token.value;
                        Expression operand = stack.pop();

                        if (operator.equals(NEGATE)) {
                            expression = negate(operand);
                        } else if (operator.equals(NOT)) {
                            expression = not(operand);
                        } else {
                            throw new UnsupportedOperationException();
                        }

                        break;
                    }

                    case BINARY_OPERATOR: {
                        String operator = (String)token.value;
                        Expression right = stack.pop();
                        Expression left = stack.pop();

                        if (operator.equals(ADD)) {
                            expression = add(left, right);
                        } else if (operator.equals(SUBTRACT)) {
                            expression = subtract(left, right);
                        } else if (operator.equals(MULTIPLY)) {
                            expression = multiply(left, right);
                        } else if (operator.equals(DIVIDE)) {
                            expression = divide(left, right);
                        } else if (operator.equals(MODULO)) {
                            expression = modulo(left, right);
                        } else if (operator.equals(GREATER_THAN)) {
                            expression = greaterThan(left, right);
                        } else if (operator.equals(GREATER_THAN_OR_EQUAL_TO)) {
                            expression = greaterThanOrEqualTo(left, right);
                        } else if (operator.equals(LESS_THAN)) {
                            expression = lessThan(left, right);
                        } else if (operator.equals(LESS_THAN_OR_EQUAL_TO)) {
                            expression = lessThanOrEqualTo(left, right);
                        } else if (operator.equals(EQUAL_TO)) {
                            expression = equalTo(left, right);
                        } else if (operator.equals(NOT_EQUAL_TO)) {
                            expression = notEqualTo(left, right);
                        } else if (operator.equals(AND)) {
                            expression = and(left, right);
                        } else if (operator.equals(OR)) {
                            expression = or(left, right);
                        } else {
                            throw new UnsupportedOperationException();
                        }

                        break;
                    }

                    default: {
                        throw new UnsupportedOperationException();
                    }
                }

                stack.push(expression);
            }

            if (stack.size() != 1) {
                throw new IllegalArgumentException("Invalid expression.");
            }

            return stack.peek();
        }

        private LinkedList<Token> tokenize(PushbackReader reader) throws IOException {
            // Read the string into a postfix list of tokens
            LinkedList<Token> tokens = new LinkedList<Token>();
            LinkedList<Token> stack = new LinkedList<Token>();

            c = reader.read();
            boolean unary = true;

            while (c != -1) {
                // Skip whitespace
                while (c != -1 && Character.isWhitespace(c)) {
                    c = reader.read();
                }

                if (c != -1) {
                    Token token;

                    if (c == 'n') {
                        if (readKeyword(reader, NULL_KEYWORD)) {
                            token = new Token(TokenType.LITERAL, null);
                        } else {
                            token = new Token(TokenType.VARIABLE, KeyPath.parse(reader));
                            c = reader.read();
                        }
                    } else if (c == '"' || c == '\'') {
                        StringBuilder stringBuilder = new StringBuilder();

                        // Use the same delimiter to close the string
                        int t = c;

                        // Move to the next character after the delimiter
                        c = reader.read();

                        while (c != -1 && c != t) {
                            if (!Character.isISOControl(c)) {
                                if (c == '\\') {
                                    c = reader.read();

                                    if (c == 'b') {
                                        c = '\b';
                                    } else if (c == 'f') {
                                        c = '\f';
                                    } else if (c == 'n') {
                                        c = '\n';
                                    } else if (c == 'r') {
                                        c = '\r';
                                    } else if (c == 't') {
                                        c = '\t';
                                    } else if (c == 'u') {
                                        StringBuilder unicodeValueBuilder = new StringBuilder();
                                        while (unicodeValueBuilder.length() < 4) {
                                            c = reader.read();
                                            unicodeValueBuilder.append((char)c);
                                        }

                                        String unicodeValue = unicodeValueBuilder.toString();
                                        c = (char)Integer.parseInt(unicodeValue, 16);
                                    } else {
                                        if (!(c == '\\'
                                            || c == '/'
                                            || c == '\"'
                                            || c == '\''
                                            || c == t)) {
                                            throw new IllegalArgumentException("Unsupported escape sequence.");
                                        }
                                    }
                                }

                                stringBuilder.append((char)c);
                            }

                            c = reader.read();
                        }

                        if (c != t) {
                            throw new IllegalArgumentException("Unterminated string.");
                        }

                        // Move to the next character after the delimiter
                        c = reader.read();

                        token = new Token(TokenType.LITERAL, stringBuilder.toString());
                    } else if (Character.isDigit(c)) {
                        StringBuilder numberBuilder = new StringBuilder();
                        boolean integer = true;

                        while (c != -1 && (Character.isDigit(c) || c == '.'
                            || c == 'e' || c == 'E')) {
                            numberBuilder.append((char)c);
                            integer &= !(c == '.');
                            c = reader.read();
                        }

                        Number value;
                        if (integer) {
                            value = Long.parseLong(numberBuilder.toString());
                        } else {
                            value = Double.parseDouble(numberBuilder.toString());
                        }

                        token = new Token(TokenType.LITERAL, value);
                    } else if (c == 't') {
                        if (readKeyword(reader, TRUE_KEYWORD)) {
                            token = new Token(TokenType.LITERAL, true);
                        } else {
                            token = new Token(TokenType.VARIABLE, KeyPath.parse(reader));
                            c = reader.read();
                        }
                    } else if (c == 'f') {
                        if (readKeyword(reader, FALSE_KEYWORD)) {
                            token = new Token(TokenType.LITERAL, false);
                        } else {
                            token = new Token(TokenType.VARIABLE, KeyPath.parse(reader));
                            c = reader.read();
                        }
                    } else if (Character.isJavaIdentifierStart(c)) {
                        reader.unread(c);

                        // TODO Here (and everywhere else where we call KeyPath.parse()),
                        // read the path value. If c == '(' when this method returns, the
                        // path refers to a function; read the arguments and create a
                        // FUNCTION token
                        token = new Token(TokenType.VARIABLE, KeyPath.parse(reader));
                        c = reader.read();
                    } else {
                        if (c == NEGATE.charAt(0) && unary) {
                            token = new Token(TokenType.UNARY_OPERATOR, NEGATE);
                        } else if (c == NOT.charAt(0) && unary) {
                            token = new Token(TokenType.UNARY_OPERATOR, NOT);
                        } else if (c == ADD.charAt(0)) {
                            token = new Token(TokenType.BINARY_OPERATOR, ADD);
                        } else if (c == SUBTRACT.charAt(0)) {
                            token = new Token(TokenType.BINARY_OPERATOR, SUBTRACT);
                        } else if (c == MULTIPLY.charAt(0)) {
                            token = new Token(TokenType.BINARY_OPERATOR, MULTIPLY);
                        } else if (c == DIVIDE.charAt(0)) {
                            token = new Token(TokenType.BINARY_OPERATOR, DIVIDE);
                        } else if (c == MODULO.charAt(0)) {
                            token = new Token(TokenType.BINARY_OPERATOR, MODULO);
                        } else if (c == EQUAL_TO.charAt(0)) {
                            c = reader.read();

                            if (c == EQUAL_TO.charAt(1)) {
                                token = new Token(TokenType.BINARY_OPERATOR, EQUAL_TO);
                            } else {
                                throw new IllegalArgumentException();
                            }
                        } else if (c == NOT_EQUAL_TO.charAt(0)) {
                            c = reader.read();

                            if (c == NOT_EQUAL_TO.charAt(1)) {
                                token = new Token(TokenType.BINARY_OPERATOR, NOT_EQUAL_TO);
                            } else {
                                throw new IllegalArgumentException();
                            }
                        } else if (c == GREATER_THAN.charAt(0)) {
                            c = reader.read();

                            if (c == GREATER_THAN_OR_EQUAL_TO.charAt(1)) {
                                token = new Token(TokenType.BINARY_OPERATOR, GREATER_THAN_OR_EQUAL_TO);
                            } else {
                                token = new Token(TokenType.BINARY_OPERATOR, GREATER_THAN);
                            }
                        } else if (c == LESS_THAN.charAt(0)) {
                            c = reader.read();

                            if (c == LESS_THAN_OR_EQUAL_TO.charAt(1)) {
                                token = new Token(TokenType.BINARY_OPERATOR, LESS_THAN_OR_EQUAL_TO);
                            } else {
                                token = new Token(TokenType.BINARY_OPERATOR, LESS_THAN);
                            }
                        } else if (c == AND.charAt(0)) {
                            c = reader.read();

                            if (c == AND.charAt(0)) {
                                token = new Token(TokenType.BINARY_OPERATOR, AND);
                            } else {
                                throw new IllegalArgumentException();
                            }
                        } else if (c == OR.charAt(0)) {
                            c = reader.read();

                            if (c == OR.charAt(0)) {
                                token = new Token(TokenType.BINARY_OPERATOR, OR);
                            } else {
                                throw new IllegalArgumentException();
                            }
                        } else if (c == '(') {
                            token = new Token(TokenType.BEGIN_GROUP, LEFT_PARENTHESIS);
                        } else if (c == ')') {
                            token = new Token(TokenType.END_GROUP, RIGHT_PARENTHESIS);
                        } else {
                            throw new IllegalArgumentException("Unexpected character in expression.");
                        }

                        c = reader.read();
                    }

                    // Process the token
                    switch (token.type) {
                        case LITERAL:
                        case VARIABLE: {
                            tokens.add(token);
                            break;
                        }

                        case UNARY_OPERATOR:
                        case BINARY_OPERATOR: {
                            int priority = getPriority((String)token.value);

                            while (!stack.isEmpty()
                                && stack.peek().type != TokenType.BEGIN_GROUP
                                && getPriority((String)stack.peek().value) >= priority
                                && getPriority((String)stack.peek().value) != MAX_PRIORITY) {
                                tokens.add(stack.pop());
                            }

                            stack.push(token);
                            break;
                        }

                        case BEGIN_GROUP: {
                            stack.push(token);
                            break;
                        }

                        case END_GROUP: {
                            for (token = stack.pop(); token.type != TokenType.BEGIN_GROUP; token = stack.pop()) {
                                tokens.add(token);
                            }

                            break;
                        }

                        default: {
                            throw new UnsupportedOperationException();
                        }
                    }

                    unary = !(token.type == TokenType.LITERAL || token.type == TokenType.VARIABLE);
                }
            }

            while (!stack.isEmpty()) {
                tokens.add(stack.pop());
            }

            return tokens;
        }

        private boolean readKeyword(PushbackReader reader, String keyword) throws IOException {
            int n = keyword.length();
            int i = 0;

            while (c != -1 && i < n) {
                pushbackBuffer[i] = (char)c;
                if (keyword.charAt(i) != c) {
                    break;
                }

                c = reader.read();
                i++;
            }

            boolean result;
            if (i < n) {
                reader.unread(pushbackBuffer, 0, i + 1);
                result = false;
            } else {
                result = true;
            }

            return result;
        }

        private int getPriority(String operator) {
            int priority;

            if (operator.equals(NEGATE)
                || operator.equals(NOT)) {
                priority = MAX_PRIORITY;
            } else if (operator.equals(MULTIPLY)
                || operator.equals(DIVIDE)
                || operator.equals(MODULO)) {
                priority = MAX_PRIORITY - 1;
            } else if (operator.equals(ADD)
                || operator.equals(SUBTRACT)) {
                priority = MAX_PRIORITY - 2;
            } else if (operator.equals(GREATER_THAN)
                || operator.equals(GREATER_THAN_OR_EQUAL_TO)
                || operator.equals(LESS_THAN)
                || operator.equals(LESS_THAN_OR_EQUAL_TO)) {
                priority = MAX_PRIORITY - 3;
            } else if (operator.equals(EQUAL_TO)
                || operator.equals(NOT_EQUAL_TO)) {
                priority = MAX_PRIORITY - 4;
            } else if (operator.equals(AND)) {
                priority = MAX_PRIORITY - 5;
            } else if (operator.equals(OR)) {
                priority = MAX_PRIORITY - 6;
            } else {
                throw new IllegalArgumentException();
            }

            return priority;
        }
    }

    private static final String NEGATE = "-";
    private static final String NOT = "!";

    private static final String ADD = "+";
    private static final String SUBTRACT = "-";
    private static final String MULTIPLY = "*";
    private static final String DIVIDE = "/";
    private static final String MODULO = "%";

    private static final String GREATER_THAN = ">";
    private static final String GREATER_THAN_OR_EQUAL_TO = ">=";
    private static final String LESS_THAN = "<";
    private static final String LESS_THAN_OR_EQUAL_TO = "<=";
    private static final String EQUAL_TO = "==";
    private static final String NOT_EQUAL_TO = "!=";

    private static final String AND = "&&";
    private static final String OR = "||";

    private static final String LEFT_PARENTHESIS = "(";
    private static final String RIGHT_PARENTHESIS = ")";

    private static final String NULL_KEYWORD = "null";
    private static final String TRUE_KEYWORD = "true";
    private static final String FALSE_KEYWORD = "false";

    /**
     * Evaluates the expression.
     *
     * @param namespace
     * The namespace against which the expression will be evaluated.
     *
     * @return
     * The result of evaluating the expression.
     */
    public abstract Object evaluate(Object namespace);

    /**
     * Updates the expression value.
     *
     * @param namespace
     * The namespace against which the expression will be evaluated.
     *
     * @param value
     * The value to assign to the expression.
     */
    public abstract void update(Object namespace, Object value);

    /**
     * Tests whether the expression is defined.
     *
     * @param namespace
     * The namespace against which the expression will be evaluated.
     *
     * @return
     * <tt>true</tt> if the expression is defined; <tt>false</tt>, otherwise.
     */
    public abstract boolean isDefined(Object namespace);

    /**
     * Tests whether the expression represents an l-value (i.e. can be
     * assigned to).
     *
     * @return
     * <tt>true</tt> if the expression is an l-value; <tt>false</tt>,
     * otherwise.
     */
    public abstract boolean isLValue();

    /**
     * Returns a list of arguments to this expression.
     */
    public List<KeyPath> getArguments() {
        ArrayList<KeyPath> arguments = new ArrayList<KeyPath>();
        getArguments(arguments);

        return arguments;
    }

    /**
     * Populates a list of arguments to this expression.
     */
    protected abstract void getArguments(List<KeyPath> arguments);

    /**
     * Returns the value at a given path within a namespace.
     *
     * @param namespace
     * @param keyPath
     *
     * @return
     * The value at the given path, or <tt>null</tt> if no such value exists.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Object namespace, KeyPath keyPath) {
        if (keyPath == null) {
            throw new NullPointerException();
        }

        return (T)get(namespace, keyPath.iterator());
    }

    /**
     * Returns the value at a given path within a namespace.
     *
     * @param namespace
     * @param keyPathIterator
     *
     * @return
     * The value at the given path, or <tt>null</tt> if no such value exists.
     */
    @SuppressWarnings("unchecked")
    private static <T> T get(Object namespace, Iterator<String> keyPathIterator) {
        if (keyPathIterator == null) {
            throw new NullPointerException();
        }

        T value;
        if (keyPathIterator.hasNext()) {
            // TODO Remove cast to T when build is updated to Java 7
            value = (T)get(get(namespace, keyPathIterator.next()), keyPathIterator);
        } else {
            value = (T)namespace;
        }

        return value;
    }

    /**
     * Returns the value at a given key within a namespace.
     *
     * @param namespace
     * @param key
     *
     * @return
     * The value at the given key, or <tt>null</tt> if no such value exists.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Object namespace, String key) {
        if (key == null) {
            throw new NullPointerException();
        }

        Object value;
        if (namespace instanceof List<?>) {
            List<Object> list = (List<Object>)namespace;
            value = list.get(Integer.parseInt(key));
        } else if (namespace != null) {
            Map<String, Object> map;
            if (namespace instanceof Map<?, ?>) {
                map = (Map<String, Object>)namespace;
            } else {
                map = new BeanAdapter(namespace);
            }

            value = map.get(key);
        } else {
            value = null;
        }

        return (T)value;
    }

    /**
     * Sets the value at a given path within a namespace.
     *
     * @param namespace
     * @param keyPath
     * @param value
     */
    public static void set(Object namespace, KeyPath keyPath, Object value) {
        if (keyPath == null) {
            throw new NullPointerException();
        }

        set(namespace, keyPath.iterator(), value);
    }

    /**
     * Sets the value at a given path within a namespace.
     *
     * @param namespace
     * @param keyPathIterator
     * @param value
     */
    private static void set(Object namespace, Iterator<String> keyPathIterator, Object value) {
        if (keyPathIterator == null) {
            throw new NullPointerException();
        }

        if (!keyPathIterator.hasNext()) {
            throw new IllegalArgumentException();
        }

        String key = keyPathIterator.next();

        if (keyPathIterator.hasNext()) {
            set(get(namespace, key), keyPathIterator, value);
        } else {
            set(namespace, key, value);
        }
    }

    /**
     * Sets the value at a given path within a namespace.
     *
     * @param namespace
     * @param key
     * @param value
     */
    @SuppressWarnings("unchecked")
    public static void set(Object namespace, String key, Object value) {
        if (key == null) {
            throw new NullPointerException();
        }

        if (namespace instanceof List<?>) {
            List<Object> list = (List<Object>)namespace;
            list.set(Integer.parseInt(key), value);
        } else if (namespace != null) {
            Map<String, Object> map;
            if (namespace instanceof Map<?, ?>) {
                map = (Map<String, Object>)namespace;
            } else {
                map = new BeanAdapter(namespace);
            }

            map.put(key, value);
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Tests the existence of a path within a namespace.
     *
     * @param namespace
     * @param keyPath
     *
     * @return
     * <tt>true</tt> if the path exists; <tt>false</tt>, otherwise.
     */
    public static boolean isDefined(Object namespace, KeyPath keyPath) {
        if (keyPath == null) {
            throw new NullPointerException();
        }

        return isDefined(namespace, keyPath.iterator());
    }

    /**
     * Tests the existence of a path within a namespace.
     *
     * @param namespace
     * @param keyPathIterator
     *
     * @return
     * <tt>true</tt> if the path exists; <tt>false</tt>, otherwise.
     */
    private static boolean isDefined(Object namespace, Iterator<String> keyPathIterator) {
        if (keyPathIterator == null) {
            throw new NullPointerException();
        }

        if (!keyPathIterator.hasNext()) {
            throw new IllegalArgumentException();
        }

        String key = keyPathIterator.next();

        boolean defined;
        if (keyPathIterator.hasNext()) {
            defined = isDefined(get(namespace, key), keyPathIterator);
        } else {
            defined = isDefined(namespace, key);
        }

        return defined;
    }

    /**
     * Tests the existence of a key within a namespace.
     *
     * @param namespace
     * @param key
     *
     * @return
     * <tt>true</tt> if the key exists; <tt>false</tt>, otherwise.
     */
    @SuppressWarnings("unchecked")
    public static boolean isDefined(Object namespace, String key) {
        if (key == null) {
            throw new NullPointerException();
        }

        boolean defined;
        if (namespace instanceof List<?>) {
            List<Object> list = (List<Object>)namespace;
            defined = Integer.parseInt(key) < list.size();
        } else if (namespace != null) {
            Map<String, Object> map;
            if (namespace instanceof Map<?, ?>) {
                map = (Map<String, Object>)namespace;
            } else {
                map = new BeanAdapter(namespace);
            }

            defined = map.containsKey(key);
        } else {
            defined = false;
        }

        return defined;
    }

    /**
     * Creates an addition or concatenation expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression add(Expression left, Expression right) {
        return new BinaryExpression(left, right) {
            @Override
            public String getOperator() {
                return ADD;
            }

            @Override
            public Object evaluate(Object namespace) {
                Object leftValue = getLeft().evaluate(namespace);
                Object rightValue = getRight().evaluate(namespace);

                Object value;
                if (leftValue instanceof String || rightValue instanceof String) {
                    value = leftValue.toString().concat(rightValue.toString());
                } else {
                    Number leftNumber = (Number)leftValue;
                    Number rightNumber = (Number)rightValue;

                    if (leftNumber instanceof Double || rightNumber instanceof Double) {
                        value = leftNumber.doubleValue() + rightNumber.doubleValue();
                    } else if (leftNumber instanceof Float || rightNumber instanceof Float) {
                        value = leftNumber.floatValue() + rightNumber.floatValue();
                    } else if (leftNumber instanceof Long || rightNumber instanceof Long) {
                        value = leftNumber.longValue() + rightNumber.longValue();
                    } else if (leftNumber instanceof Integer || rightNumber instanceof Integer) {
                        value = leftNumber.intValue() + rightNumber.intValue();
                    } else if (leftNumber instanceof Short || rightNumber instanceof Short) {
                        value = leftNumber.shortValue() + rightNumber.shortValue();
                    } else if (leftNumber instanceof Byte || rightNumber instanceof Byte) {
                        value = leftNumber.byteValue() + rightNumber.byteValue();
                    } else {
                        throw new UnsupportedOperationException();
                    }
                }

                return value;
            }
        };
    }

    /**
     * Creates an addition or concatenation expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression add(Expression left, Object right) {
        return add(left, new LiteralExpression(right));
    }

    /**
     * Creates an addition or concatenation expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression add(Object left, Expression right) {
        return add(new LiteralExpression(left), right);
    }

    /**
     * Creates an addition or concatenation expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression add(Object left, Object right) {
        return add(new LiteralExpression(left), new LiteralExpression(right));
    }

    /**
     * Creates a subtraction expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression subtract(Expression left, Expression right) {
        return new BinaryExpression(left, right) {
            @Override
            public String getOperator() {
                return SUBTRACT;
            }

            @Override
            public Object evaluate(Object namespace) {
                Number leftValue = (Number)getLeft().evaluate(namespace);
                Number rightValue = (Number)getRight().evaluate(namespace);

                Number value;
                if (leftValue instanceof Double || rightValue instanceof Double) {
                    value = leftValue.doubleValue() - rightValue.doubleValue();
                } else if (leftValue instanceof Float || rightValue instanceof Float) {
                    value = leftValue.floatValue() - rightValue.floatValue();
                } else if (leftValue instanceof Long || rightValue instanceof Long) {
                    value = leftValue.longValue() - rightValue.longValue();
                } else if (leftValue instanceof Integer || rightValue instanceof Integer) {
                    value = leftValue.intValue() - rightValue.intValue();
                } else if (leftValue instanceof Short || rightValue instanceof Short) {
                    value = leftValue.shortValue() - rightValue.shortValue();
                } else if (leftValue instanceof Byte || rightValue instanceof Byte) {
                    value = leftValue.byteValue() - rightValue.byteValue();
                } else {
                    throw new UnsupportedOperationException();
                }

                return value;
            }
        };
    }

    /**
     * Creates a subtraction expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression subtract(Expression left, Number right) {
        return subtract(left, new LiteralExpression(right));
    }

    /**
     * Creates a subtraction expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression subtract(Number left, Expression right) {
        return subtract(new LiteralExpression(left), right);
    }

    /**
     * Creates a subtraction expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression subtract(Number left, Number right) {
        return subtract(new LiteralExpression(left), new LiteralExpression(right));
    }

    /**
     * Creates a multiplication expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression multiply(Expression left, Expression right) {
        return new BinaryExpression(left, right) {
            @Override
            public String getOperator() {
                return MULTIPLY;
            }

            @Override
            public Object evaluate(Object namespace) {
                Number leftValue = (Number)getLeft().evaluate(namespace);
                Number rightValue = (Number)getRight().evaluate(namespace);

                Number value;
                if (leftValue instanceof Double || rightValue instanceof Double) {
                    value = leftValue.doubleValue() * rightValue.doubleValue();
                } else if (leftValue instanceof Float || rightValue instanceof Float) {
                    value = leftValue.floatValue() * rightValue.floatValue();
                } else if (leftValue instanceof Long || rightValue instanceof Long) {
                    value = leftValue.longValue() * rightValue.longValue();
                } else if (leftValue instanceof Integer || rightValue instanceof Integer) {
                    value = leftValue.intValue() * rightValue.intValue();
                } else if (leftValue instanceof Short || rightValue instanceof Short) {
                    value = leftValue.shortValue() * rightValue.shortValue();
                } else if (leftValue instanceof Byte || rightValue instanceof Byte) {
                    value = leftValue.byteValue() * rightValue.byteValue();
                } else {
                    throw new UnsupportedOperationException();
                }

                return value;
            }
        };
    }

    /**
     * Creates a multiplication expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression multiply(Expression left, Number right) {
        return multiply(left, new LiteralExpression(right));
    }

    /**
     * Creates a multiplication expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression multiply(Number left, Expression right) {
        return multiply(new LiteralExpression(left), right);
    }

    /**
     * Creates a multiplication expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression multiply(Number left, Number right) {
        return multiply(new LiteralExpression(left), new LiteralExpression(right));
    }

    /**
     * Creates a division expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression divide(Expression left, Expression right) {
        return new BinaryExpression(left, right) {
            @Override
            public String getOperator() {
                return DIVIDE;
            }

            @Override
            public Object evaluate(Object namespace) {
                Number leftValue = (Number)getLeft().evaluate(namespace);
                Number rightValue = (Number)getRight().evaluate(namespace);

                Number value;
                if (leftValue instanceof Double || rightValue instanceof Double) {
                    value = leftValue.doubleValue() / rightValue.doubleValue();
                } else if (leftValue instanceof Float || rightValue instanceof Float) {
                    value = leftValue.floatValue() / rightValue.floatValue();
                } else if (leftValue instanceof Long || rightValue instanceof Long) {
                    value = leftValue.longValue() / rightValue.longValue();
                } else if (leftValue instanceof Integer || rightValue instanceof Integer) {
                    value = leftValue.intValue() / rightValue.intValue();
                } else if (leftValue instanceof Short || rightValue instanceof Short) {
                    value = leftValue.shortValue() / rightValue.shortValue();
                } else if (leftValue instanceof Byte || rightValue instanceof Byte) {
                    value = leftValue.byteValue() / rightValue.byteValue();
                } else {
                    throw new UnsupportedOperationException();
                }

                return value;
            }
        };
    }

    /**
     * Creates a division expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression divide(Expression left, Number right) {
        return divide(left, new LiteralExpression(right));
    }

    /**
     * Creates a division expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression divide(Number left, Expression right) {
        return divide(new LiteralExpression(left), right);
    }

    /**
     * Creates a division expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression divide(Number left, Number right) {
        return divide(new LiteralExpression(left), new LiteralExpression(right));
    }

    /**
     * Creates a modulus expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression modulo(Expression left, Expression right) {
        return new BinaryExpression(left, right) {
            @Override
            public String getOperator() {
                return MODULO;
            }

            @Override
            public Object evaluate(Object namespace) {
                Number leftValue = (Number)getLeft().evaluate(namespace);
                Number rightValue = (Number)getRight().evaluate(namespace);

                Number value;
                if (leftValue instanceof Double || rightValue instanceof Double) {
                    value = leftValue.doubleValue() % rightValue.doubleValue();
                } else if (leftValue instanceof Float || rightValue instanceof Float) {
                    value = leftValue.floatValue() % rightValue.floatValue();
                } else if (leftValue instanceof Long || rightValue instanceof Long) {
                    value = leftValue.longValue() % rightValue.longValue();
                } else if (leftValue instanceof Integer || rightValue instanceof Integer) {
                    value = leftValue.intValue() % rightValue.intValue();
                } else if (leftValue instanceof Short || rightValue instanceof Short) {
                    value = leftValue.shortValue() % rightValue.shortValue();
                } else if (leftValue instanceof Byte || rightValue instanceof Byte) {
                    value = leftValue.byteValue() % rightValue.byteValue();
                } else {
                    throw new UnsupportedOperationException();
                }

                return value;
            }
        };
    }

    /**
     * Creates a modulus expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression modulo(Expression left, Number right) {
        return modulo(left, new LiteralExpression(right));
    }

    /**
     * Creates a modulus expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression modulo(Number left, Expression right) {
        return modulo(new LiteralExpression(left), right);
    }

    /**
     * Creates a modulus expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression modulo(Number left, Number right) {
        return modulo(new LiteralExpression(left), new LiteralExpression(right));
    }

    /**
     * Creates an equality expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression equalTo(Expression left, Expression right) {
        return new BinaryExpression(left, right) {
            @Override
            public String getOperator() {
                return EQUAL_TO;
            }

            @Override
            @SuppressWarnings("unchecked")
            public Object evaluate(Object namespace) {
                return ((Comparable<Object>)getLeft().evaluate(namespace)).compareTo(getRight().evaluate(namespace)) == 0;
            }
        };
    }

    /**
     * Creates an equality expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression equalTo(Expression left, Object right) {
        return equalTo(left, new LiteralExpression(right));
    }

    /**
     * Creates an equality expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression equalTo(Object left, Expression right) {
        return equalTo(new LiteralExpression(left), right);
    }

    /**
     * Creates an equality expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression equalTo(Object left, Object right) {
        return equalTo(new LiteralExpression(left), new LiteralExpression(right));
    }

    /**
     * Creates an inverse equality expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression notEqualTo(Expression left, Expression right) {
        return new BinaryExpression(left, right) {
            @Override
            public String getOperator() {
                return NOT_EQUAL_TO;
            }

            @Override
            @SuppressWarnings("unchecked")
            public Object evaluate(Object namespace) {
                return ((Comparable<Object>)getLeft().evaluate(namespace)).compareTo(getRight().evaluate(namespace)) != 0;
            }
        };
    }

    /**
     * Creates an inverse equality expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression notEqualTo(Expression left, Object right) {
        return notEqualTo(left, new LiteralExpression(right));
    }

    /**
     * Creates an inverse equality expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression notEqualTo(Object left, Expression right) {
        return notEqualTo(new LiteralExpression(left), right);
    }

    /**
     * Creates an inverse equality expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression notEqualTo(Object left, Object right) {
        return notEqualTo(new LiteralExpression(left), new LiteralExpression(right));
    }

    /**
     * Creates a "greater-than" comparison expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression greaterThan(Expression left, Expression right) {
        return new BinaryExpression(left, right) {
            @Override
            public String getOperator() {
                return GREATER_THAN;
            }

            @Override
            @SuppressWarnings("unchecked")
            public Object evaluate(Object namespace) {
                return ((Comparable<Object>)getLeft().evaluate(namespace)).compareTo(getRight().evaluate(namespace)) > 0;
            }
        };
    }

    /**
     * Creates a "greater-than" comparison expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression greaterThan(Expression left, Object right) {
        return greaterThan(left, new LiteralExpression(right));
    }

    /**
     * Creates a "greater-than" comparison expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression greaterThan(Object left, Expression right) {
        return greaterThan(new LiteralExpression(left), right);
    }

    /**
     * Creates a "greater-than" comparison expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression greaterThan(Object left, Object right) {
        return greaterThan(new LiteralExpression(left), new LiteralExpression(right));
    }

    /**
     * Creates a "greater-than-or-equal-to" comparison expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression greaterThanOrEqualTo(Expression left, Expression right) {
        return new BinaryExpression(left, right) {
            @Override
            public String getOperator() {
                return GREATER_THAN_OR_EQUAL_TO;
            }

            @Override
            @SuppressWarnings("unchecked")
            public Object evaluate(Object namespace) {
                return ((Comparable<Object>)getLeft().evaluate(namespace)).compareTo(getRight().evaluate(namespace)) >= 0;
            }
        };
    }

    /**
     * Creates a "greater-than-or-equal-to" comparison expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression greaterThanOrEqualTo(Expression left, Object right) {
        return greaterThanOrEqualTo(left, new LiteralExpression(right));
    }

    /**
     * Creates a "greater-than-or-equal-to" comparison expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression greaterThanOrEqualTo(Object left, Expression right) {
        return greaterThanOrEqualTo(new LiteralExpression(left), right);
    }

    /**
     * Creates a "greater-than-or-equal-to" comparison expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression greaterThanOrEqualTo(Object left, Object right) {
        return greaterThanOrEqualTo(new LiteralExpression(left), new LiteralExpression(right));
    }

    /**
     * Creates a "less-than" comparison expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression lessThan(Expression left, Expression right) {
        return new BinaryExpression(left, right) {
            @Override
            public String getOperator() {
                return LESS_THAN;
            }

            @Override
            @SuppressWarnings("unchecked")
            public Object evaluate(Object namespace) {
                return ((Comparable<Object>)getLeft().evaluate(namespace)).compareTo(getRight().evaluate(namespace)) < 0;
            }
        };
    }

    /**
     * Creates a "less-than" comparison expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression lessThan(Expression left, Object right) {
        return lessThan(left, new LiteralExpression(right));
    }

    /**
     * Creates a "less-than" comparison expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression lessThan(Object left, Expression right) {
        return lessThan(new LiteralExpression(left), right);
    }

    /**
     * Creates a "less-than" comparison expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression lessThan(Object left, Object right) {
        return lessThan(new LiteralExpression(left), new LiteralExpression(right));
    }

    /**
     * Creates a "less-than-or-equal-to" comparison expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression lessThanOrEqualTo(Expression left, Expression right) {
        return new BinaryExpression(left, right) {
            @Override
            public String getOperator() {
                return LESS_THAN_OR_EQUAL_TO;
            }

            @Override
            @SuppressWarnings("unchecked")
            public Object evaluate(Object namespace) {
                return ((Comparable<Object>)getLeft().evaluate(namespace)).compareTo(getRight().evaluate(namespace)) <= 0;
            }
        };
    }

    /**
     * Creates a "less-than-or-equal-to" comparison expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression lessThanOrEqualTo(Expression left, Object right) {
        return lessThanOrEqualTo(left, new LiteralExpression(right));
    }

    /**
     * Creates a "less-than-or-equal-to" comparison expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression lessThanOrEqualTo(Object left, Expression right) {
        return lessThanOrEqualTo(new LiteralExpression(left), right);
    }

    /**
     * Creates a "less-than-or-equal-to" comparison expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression lessThanOrEqualTo(Object left, Object right) {
        return lessThanOrEqualTo(new LiteralExpression(left), new LiteralExpression(right));
    }

    /**
     * Creates a boolean "and" expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression and(Expression left, Expression right) {
        return new BinaryExpression(left, right) {
            @Override
            public String getOperator() {
                return AND;
            }

            @Override
            public Object evaluate(Object namespace) {
                return (Boolean)getLeft().evaluate(namespace) && (Boolean)getRight().evaluate(namespace);
            }
        };
    }

    /**
     * Creates a boolean "and" expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression and(Expression left, Boolean right) {
        return and(left, new LiteralExpression(right));
    }

    /**
     * Creates a boolean "and" expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression and(Boolean left, Expression right) {
        return and(new LiteralExpression(left), right);
    }

    /**
     * Creates a boolean "and" expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression and(Boolean left, Boolean right) {
        return and(new LiteralExpression(left), new LiteralExpression(right));
    }

    /**
     * Creates a boolean "or" expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression or(Expression left, Expression right) {
        return new BinaryExpression(left, right) {
            @Override
            public String getOperator() {
                return OR;
            }

            @Override
            public Object evaluate(Object namespace) {
                return (Boolean)getLeft().evaluate(namespace) || (Boolean)getRight().evaluate(namespace);
            }
        };
    }

    /**
     * Creates a boolean "or" expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression or(Expression left, Boolean right) {
        return or(left, new LiteralExpression(right));
    }

    /**
     * Creates a boolean "or" expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression or(Boolean left, Expression right) {
        return or(new LiteralExpression(left), right);
    }

    /**
     * Creates a boolean "or" expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression or(Boolean left, Boolean right) {
        return or(new LiteralExpression(left), new LiteralExpression(right));
    }

    /**
     * Creates a numeric negation expression.
     *
     * @param operand
     */
    public static UnaryExpression negate(Expression operand) {
        return new UnaryExpression(operand) {
            @Override
            public String getOperator() {
                return "-";
            }

            @Override
            public Object evaluate(Object namespace) {
                Number value = (Number)getOperand().evaluate(namespace);
                Class<? extends Number> type = value.getClass();

                if (type == Byte.class) {
                    value = -value.byteValue();
                } else if (type == Short.class) {
                    value = -value.shortValue();
                } else if (type == Integer.class) {
                    value = -value.intValue();
                } else if (type == Long.class) {
                    value = -value.longValue();
                } else if (type == Float.class) {
                    value = -value.floatValue();
                } else if (type == Double.class) {
                    value = -value.doubleValue();
                } else {
                    throw new UnsupportedOperationException();
                }

                return value;
            }
        };
    }

    /**
     * Creates a numeric negation expression.
     *
     * @param operand
     */
    public UnaryExpression negate(Number operand) {
        return negate(new LiteralExpression(operand));
    }

    /**
     * Creates a boolean "not" expression.
     *
     * @param operand
     */
    public static UnaryExpression not(Expression operand) {
        return new UnaryExpression(operand) {
            @Override
            public String getOperator() {
                return "!";
            }

            @Override
            public Object evaluate(Object namespace) {
                return !(Boolean)getOperand().evaluate(namespace);
            }
        };
    }

    /**
     * Creates a boolean "not" expression.
     *
     * @param operand
     */
    public static UnaryExpression not(Boolean operand) {
        return not(new LiteralExpression(operand));
    }

    /**
     * Parses a string representation of an expression into an expression
     * tree.
     *
     * @param value
     * The string representation of the expression.
     */
    public static Expression valueOf(String value) {
        if (value == null) {
            throw new NullPointerException();
        }

        Parser parser = new Parser();
        Expression expression;
        try {
            expression = parser.parse(new StringReader(value));
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        return expression;
    }
}
