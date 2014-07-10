/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package netscape.javascript;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test for JSObject class.
 */
public class JSObjectTest {

    @Test
    public void testGetWindow() {
        try {
            JSObject.getWindow(null);
            fail("Did not get the expected JSException");
        } catch (JSException ex) {
            String msg = ex.getMessage();
            assertNotNull(msg);
            assertTrue(msg.startsWith("Unexpected error:"));
            assertTrue(msg.endsWith("plugin.jar"));
        }
    }

    @Test
    public void testCall() {
        JSObject jso = new JSObject() {
            @Override
            public Object call(String methodName, Object... args) throws JSException {
                List<String> list = new ArrayList<>();
                if (args != null) {
                    for (Object arg : args) {
                        list.add((String)arg);
                    }
                }
                return list;
            }

            @Override
            public Object eval(String s) throws JSException {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Object getMember(String name) throws JSException {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void setMember(String name, Object value) throws JSException {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void removeMember(String name) throws JSException {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Object getSlot(int index) throws JSException {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void setSlot(int index, Object value) throws JSException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };

        List<String> list;

        list = (List<String>)jso.call("");
        assertTrue(list.isEmpty());

        list = (List<String>)jso.call("", new Object[0]);
        assertTrue(list.isEmpty());

        list = (List<String>)jso.call("", (Object[])null);
        assertTrue(list.isEmpty());

        list = (List<String>)jso.call("", null);
        assertTrue(list.isEmpty());

        list = (List<String>)jso.call("", (Object)null);
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertNull(list.get(0));

        list = (List<String>)jso.call("", "str1");
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals("str1", list.get(0));

        list = (List<String>)jso.call("", new Object[] { "str1" });
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals("str1", list.get(0));

        list = (List<String>)jso.call("", "str1", "str2");
        assertFalse(list.isEmpty());
        assertEquals(2, list.size());
        assertEquals("str1", list.get(0));
        assertEquals("str2", list.get(1));

        list = (List<String>)jso.call("", new Object[] { "str1", "str2" });
        assertFalse(list.isEmpty());
        assertEquals(2, list.size());
        assertEquals("str1", list.get(0));
        assertEquals("str2", list.get(1));
    }

}
