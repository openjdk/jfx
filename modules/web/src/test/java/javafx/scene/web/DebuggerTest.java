/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.web.Debugger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.util.Callback;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.After;

public class DebuggerTest extends TestBase {

    @Ignore("RT-40076")
    @Test
    public void testSimpleMessageExchange() {
        submit(() -> {
            Debugger debugger = getEngine().impl_getDebugger();

            final List<String> callbackMessages = new ArrayList<String>();
            debugger.setMessageCallback(message -> {
                callbackMessages.add(message);
                return null;
            });
            debugger.setEnabled(true);
            debugger.sendMessage(q(
                    "{'method':'Debugger.causesRecompilation','id':16}"));
            assertEquals(
                    Arrays.asList(q("{'result':{'result':true},'id':16}")),
                    callbackMessages);
        });
    }

    @Test
    public void testEnabledProperty() {
        submit(() -> {
            Debugger debugger = getEngine().impl_getDebugger();

            assertEquals(false, debugger.isEnabled());

            debugger.setEnabled(true);
            assertEquals(true, debugger.isEnabled());

            debugger.setEnabled(false);
            assertEquals(false, debugger.isEnabled());

            debugger.setEnabled(true);
            debugger.setEnabled(true);
            assertEquals(true, debugger.isEnabled());

            debugger.setEnabled(false);
            debugger.setEnabled(false);
            assertEquals(false, debugger.isEnabled());
        });
    }

    @Test
    public void testMessageCallbackProperty() {
        submit(() -> {
            Debugger debugger = getEngine().impl_getDebugger();
            Callback<String,Void> callback = new Callback<String,Void>() {
                public Void call(String message) {
                    return null;
                }
            };

            assertEquals(null, debugger.getMessageCallback());

            debugger.setMessageCallback(callback);
            assertEquals(callback, debugger.getMessageCallback());

            debugger.setMessageCallback(null);
            assertEquals(null, debugger.getMessageCallback());
        });
    }

    @Test
    public void testSendMessageIllegalStateException() {
        submit(() -> {
            Debugger debugger = getEngine().impl_getDebugger();
            try {
                debugger.sendMessage("foo");
                fail("IllegalStateException expected but not thrown");
            } catch (IllegalStateException expected) {}
        });
    }

    @Test
    public void testSendMessageNullPointerException() {
        submit(() -> {
            Debugger debugger = getEngine().impl_getDebugger();
            debugger.setEnabled(true);
            try {
                debugger.sendMessage(null);
                fail("NullPointerException expected but not thrown");
            } catch (NullPointerException expected) {}
        });
    }

    @Test
    public void testThreadCheck() {
        Debugger debugger = getEngine().impl_getDebugger();

        try {
            debugger.isEnabled();
            fail("IllegalStateException expected but not thrown");
        } catch (IllegalStateException expected) {}

        try {
            debugger.setEnabled(true);
            fail("IllegalStateException expected but not thrown");
        } catch (IllegalStateException expected) {}

        try {
            debugger.sendMessage("foo");
            fail("IllegalStateException expected but not thrown");
        } catch (IllegalStateException expected) {}

        try {
            debugger.getMessageCallback();
            fail("IllegalStateException expected but not thrown");
        } catch (IllegalStateException expected) {}

        try {
            debugger.setMessageCallback(null);
            fail("IllegalStateException expected but not thrown");
        } catch (IllegalStateException expected) {}
    }

    private static String q(String s) {
        return s.replace('\'', '\"');
    }

    @After
    public void disableDebug() {
        submit(() -> {
            getEngine().impl_getDebugger().setEnabled(false);
        });    
    }
}
