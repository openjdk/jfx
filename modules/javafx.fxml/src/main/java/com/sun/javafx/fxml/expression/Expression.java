/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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
import static com.sun.javafx.fxml.expression.Operator.*;

/**
 * Abstract base class for expressions. Also provides static methods for
 * creating arithmetic and logical expressions as well as accessing namespace
 * values by key path.
 */
public abstract class Expression<T> {
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

        private static final int PUSHBACK_BUFFER_SIZE = 6;

        public Expression parse(Reader reader) throws IOException {
            LinkedList<Token> tokens = tokenize(new PushbackReader(reader, PUSHBACK_BUFFER_SIZE));

            LinkedList<Expression> stack = new LinkedList<>();

            for (Token token : tokens) {
                Expression<?> expression;
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
                        Operator operator = (Operator)token.value;
                        Expression operand = stack.pop();

                        switch(operator) {
                            case NEGATE:
                                expression = negate(operand);
                                break;
                            case NOT:
                                expression = not(operand);
                                break;
                            default:
                                throw new UnsupportedOperationException();

                        }

                        break;
                    }

                    case BINARY_OPERATOR: {
                        Operator operator = (Operator)token.value;
                        Expression right = stack.pop();
                        Expression left = stack.pop();

                        switch(operator) {
                            case ADD:
                                expression = add(left, right);
                                break;
                            case SUBTRACT:
                                expression = subtract(left, right);
                                break;
                            case MULTIPLY:
                                expression = multiply(left, right);
                                break;
                            case DIVIDE:
                                expression = divide(left, right);
                                break;
                            case MODULO:
                                expression = modulo(left, right);
                                break;
                            case GREATER_THAN:
                                expression = greaterThan(left, right);
                                break;
                            case GREATER_THAN_OR_EQUAL_TO:
                                expression = greaterThanOrEqualTo(left, right);
                                break;
                            case LESS_THAN:
                                expression = lessThan(left, right);
                                break;
                            case LESS_THAN_OR_EQUAL_TO:
                                expression = lessThanOrEqualTo(left, right);
                                break;
                            case EQUAL_TO:
                                expression = equalTo(left, right);
                                break;
                            case NOT_EQUAL_TO:
                                expression = notEqualTo(left, right);
                                break;
                            case AND:
                                expression = and(left, right);
                                break;
                            case OR:
                                expression = or(left, right);
                                break;
                            default:
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
            LinkedList<Token> tokens = new LinkedList<>();
            LinkedList<Token> stack = new LinkedList<>();

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
                        boolean readNext = true;
                        if (unary) {
                            switch(c) {
                                case '-':
                                    token = new Token(TokenType.UNARY_OPERATOR, NEGATE);
                                    break;
                                case '!':
                                    token = new Token(TokenType.UNARY_OPERATOR, NOT);
                                    break;
                                case '(':
                                    token = new Token(TokenType.BEGIN_GROUP, null);
                                    break;
                                default:
                                    throw new IllegalArgumentException("Unexpected character in expression.");
                            }
                        } else {
                            switch(c) {
                                case '+':
                                    token = new Token(TokenType.BINARY_OPERATOR, ADD);
                                    break;
                                case '-':
                                    token = new Token(TokenType.BINARY_OPERATOR, SUBTRACT);
                                    break;
                                case '*':
                                    token = new Token(TokenType.BINARY_OPERATOR, MULTIPLY);
                                    break;
                                case '/':
                                    token = new Token(TokenType.BINARY_OPERATOR, DIVIDE);
                                    break;
                                case '%':
                                    token = new Token(TokenType.BINARY_OPERATOR, MODULO);
                                    break;
                                case '=':
                                    c = reader.read();
                                    if (c == '=') {
                                        token = new Token(TokenType.BINARY_OPERATOR, EQUAL_TO);
                                    } else {
                                        throw new IllegalArgumentException("Unexpected character in expression.");
                                    }
                                    break;
                                case '!':
                                    c = reader.read();

                                    if (c == '=') {
                                        token = new Token(TokenType.BINARY_OPERATOR, NOT_EQUAL_TO);
                                    } else {
                                        throw new IllegalArgumentException("Unexpected character in expression.");
                                    }
                                    break;
                                case '>':
                                    c = reader.read();

                                    if (c == '=') {
                                        token = new Token(TokenType.BINARY_OPERATOR, GREATER_THAN_OR_EQUAL_TO);
                                    } else {
                                        readNext = false;
                                        token = new Token(TokenType.BINARY_OPERATOR, GREATER_THAN);
                                    }
                                    break;
                                case '<':
                                    c = reader.read();

                                    if (c == '=') {
                                        token = new Token(TokenType.BINARY_OPERATOR, LESS_THAN_OR_EQUAL_TO);
                                    } else {
                                        readNext = false;
                                        token = new Token(TokenType.BINARY_OPERATOR, LESS_THAN);
                                    }
                                    break;
                                case '&':
                                    c = reader.read();

                                    if (c == '&') {
                                        token = new Token(TokenType.BINARY_OPERATOR, AND);
                                    } else {
                                        throw new IllegalArgumentException("Unexpected character in expression.");
                                    }
                                    break;
                                case '|':
                                    c = reader.read();

                                    if (c == '|') {
                                        token = new Token(TokenType.BINARY_OPERATOR, OR);
                                    } else {
                                        throw new IllegalArgumentException("Unexpected character in expression.");
                                    }
                                    break;

                                case ')':
                                    token = new Token(TokenType.END_GROUP, null);
                                    break;
                                default:
                                    throw new IllegalArgumentException("Unexpected character in expression.");
                            }

                        }
                        if (readNext) {
                            c = reader.read();
                        }
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
                            int priority = ((Operator)token.value).getPriority();

                            while (!stack.isEmpty()
                                && stack.peek().type != TokenType.BEGIN_GROUP
                                && ((Operator)stack.peek().value).getPriority() >= priority
                                && ((Operator)stack.peek().value).getPriority() != Operator.MAX_PRIORITY) {
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
                            for (Token t = stack.pop(); t.type != TokenType.BEGIN_GROUP; t = stack.pop()) {
                                tokens.add(t);
                            }

                            break;
                        }

                        default: {
                            throw new UnsupportedOperationException();
                        }
                    }

                    unary = !(token.type == TokenType.LITERAL || token.type == TokenType.VARIABLE || token.type == TokenType.END_GROUP);
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

    }

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
    public abstract T evaluate(Object namespace);

    /**
     * Updates the expression value.
     *
     * @param namespace
     * The namespace against which the expression will be evaluated.
     *
     * @param value
     * The value to assign to the expression.
     */
    public abstract void update(Object namespace, T value);

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
        ArrayList<KeyPath> arguments = new ArrayList<>();
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
        return new BinaryExpression(left, right, (leftValue, rightValue) -> {
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
        });
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
        return new BinaryExpression<Number, Number>(left, right, (Number leftValue, Number rightValue) -> {
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
        });
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
        return new BinaryExpression<Number, Number>(left, right, (Number leftValue, Number rightValue) -> {

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
        });
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
        return new BinaryExpression<Number, Number>(left, right, (Number leftValue, Number rightValue) -> {

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
        });
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
    public static BinaryExpression divide(Number left, Expression<Number> right) {
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
        return new BinaryExpression<Number, Number>(left, right, (Number leftValue, Number rightValue) -> {

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
            });
    }

    /**
     * Creates a modulus expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression modulo(Expression<Number> left, Number right) {
        return modulo(left, new LiteralExpression(right));
    }

    /**
     * Creates a modulus expression.
     *
     * @param left
     * @param right
     */
    public static BinaryExpression modulo(Number left, Expression<Number> right) {
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
        return new BinaryExpression<Comparable, Boolean>(left, right, (Comparable leftValue, Comparable rightValue) ->
                leftValue.compareTo(rightValue) == 0
            );
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
        return new BinaryExpression<Comparable, Boolean>(left, right, (leftValue, rightValue) ->
                 leftValue.compareTo(rightValue) != 0
        );
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
        return new BinaryExpression<Comparable, Boolean>(left, right, (leftValue, rightValue) ->
                leftValue.compareTo(rightValue) > 0
        );
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
        return new BinaryExpression<Comparable, Boolean>(left, right, (leftValue, rightValue) ->
                leftValue.compareTo(rightValue) >= 0
        );
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
        return new BinaryExpression<Comparable, Boolean>(left, right, (leftValue, rightValue) ->
                leftValue.compareTo(rightValue) < 0
        );
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
        return new BinaryExpression<Comparable, Boolean>(left, right, (leftValue, rightValue) ->
                leftValue.compareTo(rightValue) <= 0
        );
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
        return new BinaryExpression<Boolean, Boolean>(left, right, (leftValue, rightValue) ->
                leftValue && rightValue
        );
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
        return new BinaryExpression<Boolean, Boolean>(left, right, (leftValue, rightValue) ->
                leftValue || rightValue
        );
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
        return new UnaryExpression<Number, Number>(operand, (value) -> {
                Class<? extends Number> type = value.getClass();

                if (type == Byte.class) {
                    return -value.byteValue();
                } else if (type == Short.class) {
                    return -value.shortValue();
                } else if (type == Integer.class) {
                    return -value.intValue();
                } else if (type == Long.class) {
                    return -value.longValue();
                } else if (type == Float.class) {
                    return -value.floatValue();
                } else if (type == Double.class) {
                    return -value.doubleValue();
                } else {
                    throw new UnsupportedOperationException();
                }

            });
    }

    /**
     * Creates a numeric negation expression.
     *
     * @param operand
     */
    public static UnaryExpression negate(Number operand) {
        return negate(new LiteralExpression(operand));
    }

    /**
     * Creates a boolean "not" expression.
     *
     * @param operand
     */
    public static UnaryExpression not(Expression operand) {
        return new UnaryExpression<Boolean, Boolean>(operand, (value) -> !value);
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
