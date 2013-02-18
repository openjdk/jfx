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

package javafx.fxml;

import java.io.IOException;
import java.util.HashMap;

import org.junit.Test;

import com.sun.javafx.fxml.expression.Expression;
import com.sun.javafx.fxml.expression.KeyPath;

import static com.sun.javafx.fxml.expression.Expression.*;
import static org.junit.Assert.*;

public class RT_14880Test {
    @Test
    public void testExpression() {
        Expression add = add(3, 4);
        assertEquals(add.evaluate(null), 7);

        Expression subtract = subtract(5, 2);
        assertEquals(subtract.evaluate(null), 3);

        Expression multiply = multiply(add, subtract);
        assertEquals(multiply.evaluate(null), 21);

        Expression divide = divide(multiply, 3);
        assertEquals(divide.evaluate(null), 7);

        Expression concatenate = add("abc", "def");
        assertEquals(concatenate.evaluate(null), "abcdef");

        assertEquals(greaterThan(divide, 3).evaluate(null), true);
        assertEquals(greaterThan(divide, 7).evaluate(null), false);
        assertEquals(greaterThanOrEqualTo(divide, 7).evaluate(null), true);

        assertEquals(lessThan(divide, 9).evaluate(null), true);
        assertEquals(lessThan(divide, 7).evaluate(null), false);
        assertEquals(lessThanOrEqualTo(divide, 7).evaluate(null), true);

        assertEquals(equalTo("abcd", "abcd").evaluate(null), true);
    }

    @Test
    public void testParseExpression1() {
        Expression expression = valueOf("1 + 2");
        assertEquals(((Number)expression.evaluate(null)).intValue(), 3);
    }

    @Test
    public void testParseExpression2a() {
        Expression expression = valueOf("3 + 4 * 2");
        assertEquals(((Number)expression.evaluate(null)).intValue(), 11);
    }

    @Test
    public void testParseExpression2b() {
        Expression expression = valueOf("3 + (4 * 2)");
        assertEquals(((Number)expression.evaluate(null)).intValue(), 11);
    }

    @Test
    public void testParseExpression2c() {
        Expression expression = valueOf("(3 + 4) * 2");
        assertEquals(((Number)expression.evaluate(null)).intValue(), 14);
    }

    @Test
    public void testParseExpression3a() {
        Expression expression = valueOf("'abc' == 'abc'");
        assertEquals(expression.evaluate(null), true);
    }

    @Test
    public void testParseExpression3b() {
        Expression expression = valueOf("'abc' != 'abc'");
        assertEquals(expression.evaluate(null), false);
    }

    @Test
    public void testParseExpression3c() {
        Expression expression = valueOf("'abc' == 'def'");
        assertEquals(expression.evaluate(null), false);
    }

    @Test
    public void testParseExpression3d() {
        Expression expression = valueOf("'abc' != 'def'");
        assertEquals(expression.evaluate(null), true);
    }

    @Test
    public void testParseExpression3e() {
        Expression expression = valueOf("3 > 2");
        assertEquals(expression.evaluate(null), true);
    }

    @Test
    public void testParseExpression3f() {
        Expression expression = valueOf("3 < 2");
        assertEquals(expression.evaluate(null), false);
    }

    @Test
    public void testParseExpression4() {
        assertEquals(valueOf("null").evaluate(null), null);
        assertEquals(valueOf("true").evaluate(null), true);
        assertEquals(valueOf("false").evaluate(null), false);
    }

    @Test
    public void testParseExpression5() {
        HashMap<String, Object> namespace = new HashMap<String, Object>();

        HashMap<String, Object> a = new HashMap<String, Object>();
        namespace.put("a", a);

        HashMap<String, Object> b = new HashMap<String, Object>();
        a.put("b", b);

        b.put("c", 5);

        String path = "a['b'].c";

        Expression expression = valueOf(path);

        System.out.println(expression + " = " + expression.evaluate(namespace));
        assertEquals(((Number)expression.evaluate(namespace)).intValue(), 5);

        expression = valueOf("3 * " + path + " + 2");
        System.out.println(expression + " = " + expression.evaluate(namespace));
        assertEquals(((Number)expression.evaluate(namespace)).intValue(), 17);

        set(namespace, KeyPath.parse(path), 10);
        assertEquals(((Number)expression.evaluate(namespace)).intValue(), 32);

        expression = valueOf("nu['b'].c");
        System.out.println(expression + " = " + expression.evaluate(namespace));
        assertEquals(expression.evaluate(namespace), null);
    }

    @Test
    public void testParseExpression6() {
        Expression expression = valueOf("-2");
        assertEquals(((Number)expression.evaluate(null)).intValue(), -2);

        expression = valueOf("3 + -2");
        assertEquals(((Number)expression.evaluate(null)).intValue(), 1);
        System.out.println(expression + " = " + expression.evaluate(null));
    }

    @Test
    public void testParseExpression6a() {
        Expression expression = valueOf("--2");
        assertEquals(((Number)expression.evaluate(null)).intValue(), 2);
    }

    @Test
    public void testParseExpression7() {
        Expression expression = valueOf("!false");
        assertEquals(expression.evaluate(null), true);

        expression = valueOf("true && !false");
        System.out.println(expression + " = " + expression.evaluate(null));
        assertEquals(expression.evaluate(null), true);
    }

    @Test
    public void testParseExpression7a() {
        Expression expression = valueOf("!!false");
        assertEquals(expression.evaluate(null), false);
    }

    @Test
    public void testParseExpression8a() {
        Expression expression = valueOf("\"a\" + \"b\"");
        assertEquals(expression.evaluate(null), "ab");
    }

    @Test
    public void testParseExpression8b() {
        Expression expression = valueOf("'a' + 'b'");
        assertEquals(expression.evaluate(null), "ab");
    }

    @Test
    public void testParseExpression8c() {
        Expression expression = valueOf("'1' + 2");
        assertEquals(expression.evaluate(null), "12");
    }

    @Test
    public void testParseExpression8d() {
        Expression expression = valueOf("1 + '2'");
        assertEquals(expression.evaluate(null), "12");
    }

    @Test
    public void testParseExpression8e() {
        Expression expression = valueOf("1 + '2' == '12'");
        assertEquals(expression.evaluate(null), true);
    }

    @Test
    public void testParseExpression8f() {
        Expression expression = valueOf("1 + 2 + ' fiddlers'");
        assertEquals(expression.evaluate(null), "3 fiddlers");
    }

    @Test
    public void testParseExpression8g() {
        Expression expression = valueOf("'fiddlers ' + 1 + 2");
        assertEquals(expression.evaluate(null), "fiddlers 12");
    }

    @Test
    public void testMarkup() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("rt_14880.fxml"));
        fxmlLoader.load();

        RT_14880Controller controller = (RT_14880Controller)fxmlLoader.getController();

        Widget childWidget1 = (Widget)fxmlLoader.getNamespace().get("childWidget1");
        assertEquals(childWidget1.isEnabled(), false);

        controller.setPercentage(0.85);
        assertEquals(childWidget1.isEnabled(), true);
    }
}
