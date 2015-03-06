/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.web;

import netscape.javascript.JSException;
import netscape.javascript.JSObject;
import static org.junit.Assert.*;
import org.junit.Test;
import org.w3c.dom.Document;

public class JavaScriptBridgeTest extends TestBase {

    private void bind(String name, Object javaObject) {
        JSObject parent = (JSObject) getEngine().executeScript("parent");
        parent.setMember(name, javaObject);
    }
    
    public @Test void testJSBridge1() throws InterruptedException {
        final Document doc = getDocumentFor("src/test/resources/html/dom.html");
        final WebEngine web = getEngine();
        
        submit(() -> {
            Object wino = web.executeScript("parent.parent");
            assertTrue(wino instanceof JSObject);
            JSObject win = (JSObject) wino;
            JSObject doc2 = (JSObject) win.getMember("document");
            assertSame(doc, doc2);
            assertEquals("undefined", win.getMember("xyz"));
            String xyz = "xyz";
            win.setMember("xyz", xyz);
            assertEquals(xyz, win.getMember("xyz"));
            web.executeScript("xlv = 50+5");
            assertEquals("55", win.getMember("xlv").toString());
            web.executeScript("xlv = xlv+0.5");
            assertEquals(Double.valueOf("55.5"), win.getMember("xlv"));

            try {
                doc2.eval("something_unknown");
                fail("JSException expected but not thrown");
            } catch (JSException ex) {
                assertEquals("netscape.javascript.JSException: ReferenceError: Can't find variable: something_unknown", ex.toString());
            }
            // FIXME This should work, but doesn't.  A WebKit bug?
            //JSObject htmlChildren = (JSObject) doc2.eval("documentElement.childNodes");
            JSObject htmlChildren = (JSObject) doc2.eval("document.documentElement.childNodes");
            assertEquals("[object NodeList]", htmlChildren.toString());
            // child 0 is the head element, child 1 is a text node (a newline),
            // and child 2 is the body element.
            assertEquals(3, htmlChildren.getMember("length"));
            // This seems to fail occasionally for unknown reasons.  FIXME
            assertEquals("[object HTMLHeadElement]", htmlChildren.getSlot(0).toString());
            JSObject bodyNode = (JSObject) htmlChildren.getSlot(2);
            assertEquals("[object HTMLBodyElement]", bodyNode.toString());
            assertEquals(Boolean.TRUE, bodyNode.call("hasChildNodes"));
//                    JSObject p2Node = (JSObject) doc2.call("getElementById", "p2");
//                    assertEquals("p", p2Node.getMember("localName"));

            // Test Node -> JavaScript conversion.
            win.setMember("bd", bodyNode);
            assertEquals("[object HTMLBodyElement]", web.executeScript("bd.toString()"));
            Object bd2 = win.getMember("bd");
            assertSame(bodyNode, bd2);

            // RT-14174
            ((JSObject) web.executeScript("new String('test me')"))
                .call("charAt", new Object[] {1.0});
            // RT-14175
            try {
                ((JSObject) web.executeScript("new String('test me')"))
                    .call("toUpperCase", (Object[]) null);
                fail("NullPointerException expected but not thrown");
            }
            catch (Throwable ex) {
                assertTrue(ex instanceof NullPointerException);
            }
            try {
                ((JSObject) web.executeScript("new String('test me')"))
                    .call(null);
                fail("NullPointerException expected but not thrown");
            }
            catch (Throwable ex) {
                assertTrue(ex instanceof NullPointerException);
            }
            try {
                win.setMember(null, "foo");
                fail("NullPointerException expected but not thrown");
            }
            catch (Throwable ex) {
                assertTrue(ex instanceof NullPointerException);
            }
            // RT-14178
            ((JSObject) web.executeScript("new String('test me')"))
                .setMember("iamwrong", null);
            // RT-14241
            ((JSObject) web.executeScript("new Array(1, 2, 3);"))
                .setSlot(0, 155);
        });
    }

    public @Test void testJSBridge2() throws InterruptedException {
        submit(() -> {
            JSObject strO = (JSObject)
                    getEngine().executeScript("new String('test me')");
            String str = "I am new member, and I'm here";
            strO.setMember("newmember", str);
            Object o = strO.getMember("newmember");
            assertEquals(str, o);
            strO.removeMember("newmember");
            o = strO.getMember("newmember");
            assertEquals("undefined", o);
        });
    }

    public @Test void testJSBridge3() throws InterruptedException {
        //final Document doc = getDocumentFor("src/test/resources/html/dom.html");
        final WebEngine web = getEngine();

        submit(() -> {
            Object wino = web.executeScript("parent.parent");
            assertTrue(wino instanceof JSObject);
            JSObject win = (JSObject) wino;
            java.util.Stack<Object> st = new java.util.Stack<Object>();
            bind("myStack", st);
            win.setMember("myStack2", st);
            web.executeScript("myStack.push(\"abc\")");
            //assertEquals("abc", st.toString());
            st.push("def");
            assertEquals(2, web.executeScript("myStack.size()"));
            assertSame(st, web.executeScript("myStack"));
            assertSame(st, web.executeScript("myStack2"));
            assertEquals("def", web.executeScript("myStack.get(1)").toString());
            assertEquals("[abc, def]", web.executeScript("myStack").toString());
        });
    }

    public @Test void testJSBridge4() throws InterruptedException {
        final WebEngine web = getEngine();
        
        submit(() -> {
            // Based on RT-19205 "JavaScript2Java Bridge: float and double
            // values can be lost when assigned to JS variables".
            float a = (float) 15.5;
            double b = 26.75;
            Carry c = new Carry(0, 0);
            bind("myA", a);
            bind("myB", b);
            bind("myC", c);
            web.executeScript("var a1 = myA; var b1 = myB; myC.a = a1; myC.b = b1;");
            assertEquals(15.5, c.a, 0.1);
            assertEquals(26.75, c.b, 0.1);
            Carry d = new Carry(a, b);
            Carry e = new Carry(0, 0);
            bind("myD", d);
            bind("myE", e);
            Object str =
                web.executeScript("var a2 = myD.a;"
                                  + "var b2 = myD.b;"
                                  + "myE.a = a2;"
                                  + "myE.b = b2;[a2, b2].toString()");
            assertEquals("15.5,26.75", str);
            assertEquals(15.5, d.a, 0.1);
            assertEquals(15.5, e.a, 0.1);
            assertEquals(26.75, d.b, 0.1);
            assertEquals(26.75, e.b, 0.1);

            // Based on RT-19209 "JavaScript2Java Bridge: assigning a JS
            // object to a field of Java object produces garbage value"
            Carry carry = new Carry();
            bind("carry", carry);

            Object o = web.executeScript("carry.o = window; carry.o == window");
            assertEquals(Boolean.TRUE, o);
            assertEquals("[object Window]", carry.o.toString());

            // Based on RT-19204 "JavaScript2Java Bridge:
            // setting a char field of an object produces an exception"
            char ch = 'C';
            carry = new Carry();
            bind("c", ch);
            bind("carry", carry);
            web.executeScript("carry.c = c;");
            assertEquals('C', carry.c);
        });
    }

    @Test public void testJSBridge5() throws InterruptedException {
        final Document doc = getDocumentFor("src/test/resources/html/dom.html");
        final WebEngine web = getEngine();

        submit(() -> {
            JSObject doc2 = (JSObject) doc;
            try {
                doc2.call("removeChild", new Object[] {doc2});
                fail("JSException expected but not thrown");
            } catch (Throwable ex) {
                assertTrue(ex instanceof JSException);
                assertTrue(ex.toString().indexOf("DOM Exception") > 0);
            }
        });
    }

    @Test public void testJSCall1() throws InterruptedException {
        final WebEngine web = getEngine();
        submit(() -> {
            assertEquals("123.7", web.executeScript("123.67.toFixed(1)"));
            try {
                web.executeScript("123.67.toFixed(-1)");
                fail("JSException expected but not thrown");
            } catch (Throwable ex) {
                String exp = "netscape.javascript.JSException: RangeError";
                assertEquals(exp, ex.toString().substring(0, exp.length()));
            }
         });
    }

    @Test public void testNullMemberName() throws InterruptedException {
        submit(() -> {
            JSObject parent = (JSObject) getEngine().executeScript("parent");

            // test getMember(null)
            try {
                parent.getMember(null);
                fail("JSObject.getMember(null) didn't throw NPE");
            } catch (NullPointerException e) {
                // expected
            }

            // test setMember(null, obj)
            try {
                parent.setMember(null, "");
                fail("JSObject.setMember(null, obj) didn't throw NPE");
            } catch (NullPointerException e) {
                // expected
            }

            // test removeMember(null)
            try {
                parent.removeMember(null);
                fail("JSObject.removeMember(null) didn't throw NPE");
            } catch (NullPointerException e) {
                // expected
            }

            // test call(null)
            try {
                parent.call(null);
                fail("JSObject.call(null) didn't throw NPE");
            } catch (NullPointerException e) {
                // expected
            }

            // test eval(null)
            try {
                parent.eval(null);
                fail("JSObject.eval(null) didn't throw NPE");
            } catch (NullPointerException e) {
                // expected
            }
        });
    }

    public static class Carry {
        public float a;
        public double b;
        public char c;

        public Object o;

        public Carry() {
        }

        public Carry(float a, double b) {
            this.a = a;
            this.b = b;
        }
    }

    public @Test void testCallStatic() throws InterruptedException {
        final WebEngine web = getEngine();
        
        submit(() -> {
            // Test RT-19099
            java.io.File x = new java.io.File("foo.txt1");
            bind("x", x);
            try {
                Object o2 = web.executeScript("x.listRoots()");
                fail("exception expected for invoking static method");
            } catch (JSException ex) {
                if (ex.toString().indexOf("static") < 0)
                    fail("caught unexpected exception: "+ex);
            }
        });
    }

    public @Test void testBridgeExplicitOverloading() throws InterruptedException {
        final WebEngine web = getEngine();
        
        submit(() -> {
            StringBuilder sb = new StringBuilder();
            bind("sb", sb);
            web.executeScript("sb['append(int)'](123)");
            assertEquals("123", sb.toString());
            sb.append(' ');
            web.executeScript("sb['append(int)'](5.5)");
            // Note 5.5 is truncated to int.
            assertEquals("123 5", sb.toString());
            sb.append(' ');
            web.executeScript("sb['append(Object)'](5.5)");
            assertEquals("123 5 5.5", sb.toString());
            sb.append(' ');
            web.executeScript("sb['append(java.lang.String)']('abc')");
            assertEquals("123 5 5.5 abc", sb.toString());
            sb.append(' ');
            web.executeScript("sb['append(String)'](987)");
            assertEquals("123 5 5.5 abc 987", sb.toString());
            assertEquals(sb.toString(),
                         web.executeScript("sb['toString()']()"));

            char[] carr = { 'k', 'l', 'm' };
            bind("carr", carr);
            sb.append(' ');
            web.executeScript("sb['append(char[])'](carr)");
            web.executeScript("sb['append(char[],int,int)'](carr, 1, 2)");
            assertEquals("123 5 5.5 abc 987 klmlm", sb.toString());

            java.util.List<Integer> alist = new java.util.ArrayList<Integer>();
            alist.add(98);
            alist.add(87);
            alist.add(76);
            bind("alist", alist);
            Integer[] iarr = new Integer[4];
            bind("iarr", iarr);
            Object r = web.executeScript("alist.toArray(iarr)");
            assertSame(iarr, r);
            assertEquals("98/87/76/null",
                         iarr[0]+"/"+iarr[1]+"/"+iarr[2]+"/"+iarr[3]);
        });
    }

    private void executeShouldFail(WebEngine web, String expression,
                                   String expected) {
        try {
            web.executeScript(expression);
            fail("exception expected for "+expression);
        } catch (JSException ex) {
            if (ex.toString().indexOf(expected) < 0)
                fail("caught unexpected exception: "+ex);
        }
    }
    private void executeShouldFail(WebEngine web, String expression) {
        executeShouldFail(web, expression, "undefined is not a function");
    }

    public @Test void testThrowJava() throws InterruptedException {
        final WebEngine web = getEngine();

        submit(() -> {
            MyExceptionHelper test = new MyExceptionHelper();
            bind("test", test);
            try {
                web.executeScript("test.throwException()");
                fail("JSException expected but not thrown");
            } catch (JSException e) {
                assertEquals("netscape.javascript.JSException",
                             e.getClass().getName());
                assertTrue(e.getCause() != null);
                assertTrue(e.getCause() instanceof MyException);
            }
        });
    }

    public static class MyException extends Throwable {
    }

    public static class MyExceptionHelper {
        public void throwException() throws MyException {
            throw new MyException();
        }
    }


    public @Test void testBridgeArray1() throws InterruptedException {
        final WebEngine web = getEngine();
        
        submit(() -> {
            int []array = new int[3];
            array[0] = 42;
            bind("test", array);
            assertEquals(Integer.valueOf(42), web.executeScript("test[0]"));
            assertEquals(Integer.valueOf(3), web.executeScript("test.length"));
            assertSame(array, web.executeScript("test"));
         });
    }

    public @Test void testBridgeBadOverloading() throws InterruptedException {
        final WebEngine web = getEngine();
        
        submit(() -> {
            StringBuilder sb = new StringBuilder();
            bind("sb", sb);
            executeShouldFail(web, "sb['append)int)'](123)");
            executeShouldFail(web, "sb['append)int)'](123)");
            executeShouldFail(web, "sb['(int)'](123)");
            executeShouldFail(web, "sb['append(,int)'](123)");
            executeShouldFail(web, "sb['append(int,)'](123)");
            executeShouldFail(web, "sb['unknownname(int)'](123)");
            executeShouldFail(web, "sb['bad-name(int)'](123)");
            executeShouldFail(web, "sb['append(BadClass)'](123)");
            executeShouldFail(web, "sb['append(bad-type)'](123)");
            executeShouldFail(web, "sb['append(char[],,int)'](1, 2)");
        });
    }
}
