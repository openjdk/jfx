/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.web;

import com.sun.javafx.scene.web.Debugger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.util.Callback;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

public class DebuggerTest extends TestBase {

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
}
