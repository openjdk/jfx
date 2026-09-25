/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.web;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;
import org.junit.jupiter.api.Test;
import test.com.sun.webkit.MissingTypeClassLoader;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The JavaScript-to-Java bridge where its behaviour is fixed by what the JNI build did, meaning the
 * tree that commit 939aa61ead replaced. The FFM port either keeps that behaviour or records that it
 * does not.
 */
public class LiveConnectParityTest extends TestBase {

    /*
     * Paints a small canvas red and reads back the red and alpha channels of one pixel. The answer
     * is "255,255" when the canvas got a backing store and "0,0" when ImageBufferJavaBackend::create
     * took its failure branch, which it does when the per-thread upcall-failure flag is left set by
     * an earlier, unrelated upcall.
     */
    private static final String CANVAS_PROBE = "(function() {"
            + " var canvas = document.createElement('canvas'); canvas.width = 4; canvas.height = 4;"
            + " var g = canvas.getContext('2d'); g.fillStyle = 'rgb(255, 0, 0)';"
            + " g.fillRect(0, 0, 4, 4);"
            + " var pixel = g.getImageData(0, 0, 1, 1).data; return pixel[0] + ',' + pixel[3];"
            + " })()";

    private void bind(String name, Object javaObject) {
        JSObject window = (JSObject) getEngine().executeScript("window");
        window.setMember(name, javaObject);
    }

    /*
     * +obj on a Java object that is not a java.lang.Number. JavaInstance::numberValue sends every
     * class except Character and Boolean to doubleValue()D, which the JNI build called through
     * GetMethodID + CallDoubleMethod on the runtime class, so any object with that method converted
     * to its value: every JavaFX ObservableNumberValue among them.
     */
    @Test
    public void numericConversionCallsDoubleValueOnObjectsThatAreNotNumbers() {
        submit(() -> {
            WebEngine web = getEngine();
            bind("dp", new SimpleDoubleProperty(3.5));
            bind("ip", new SimpleIntegerProperty(42));
            bind("money", new Money());
            assertEquals(3.5, web.executeScript("+dp"));
            assertEquals(7, web.executeScript("dp * 2"));
            assertEquals(Boolean.TRUE, web.executeScript("dp > 3"));
            assertEquals(42, web.executeScript("Number(ip)"));
            assertEquals(7.25, web.executeScript("+money"));
        });
    }

    /*
     * JNI ignored access checks, so it reached a package private doubleValue() in a private class.
     * The class is in the unnamed module, which opens every package, so Java reflection can too.
     */
    @Test
    public void numericConversionReachesANonPublicDoubleValueInAnOpenPackage() {
        submit(() -> {
            bind("hidden", new Hidden());
            assertEquals(9.5, getEngine().executeScript("+hidden"));
        });
    }

    /*
     * The controls, which hold with or without the doubleValue() lookup: a boxed number converts
     * through Number, and an object with no instance doubleValue()D converts to 0. GetMethodID failed
     * on such an object, and it failed on a static doubleValue() as well.
     */
    @Test
    public void numericConversionWithoutAnInstanceDoubleValueIsZero() {
        submit(() -> {
            WebEngine web = getEngine();
            bind("ai", new AtomicInteger(5));
            bind("plain", new Object());
            bind("staticOnly", new StaticOnly());
            assertEquals(5, web.executeScript("+ai"));
            assertEquals(0, web.executeScript("+plain"));
            assertEquals(0, web.executeScript("+staticOnly"));
        });
    }

    /*
     * '' + obj and String(obj) on an object whose Class.getMethods() throws, because one of its
     * public methods names a class that is missing. GetMethodID found toString() without listing the
     * class, so the JNI build answered the override.
     */
    @Test
    public void stringConversionSurvivesAClassWhoseMethodsCannotBeListed() {
        Object opt = new MissingTypeClassLoader().newInstance("lcprobe.Opt", "Opt!");
        submit(() -> {
            WebEngine web = getEngine();
            bind("opt", opt);
            assertEquals("Opt!", web.executeScript("String(opt)"));
            assertEquals("Opt!", web.executeScript("'' + opt"));
        });
    }

    /*
     * The same conversion must not leave the upcall-failure flag set behind it, whatever it answers:
     * GetMethodID left no exception pending here. A flag left set makes the first canvas of the next
     * script, however unrelated, come up without a backing store.
     */
    @Test
    public void stringConversionOfAClassWhoseMethodsCannotBeListedLeavesNoFailureBehind() {
        Object opt = new MissingTypeClassLoader().newInstance("lcprobe.Flagged", "Flagged!");
        submit(() -> {
            WebEngine web = getEngine();
            assertEquals("255,255", web.executeScript(CANVAS_PROBE), "the probe itself");
            bind("opt", opt);
            web.executeScript("String(opt)");
            assertEquals("255,255", web.executeScript(CANVAS_PROBE),
                    "the string conversion left the upcall-failure flag set");
        });
    }

    /* The same kind of class without its own toString(): GetMethodID found Object.toString(). */
    @Test
    public void inheritedToStringSurvivesAClassWhoseMethodsCannotBeListed() {
        Object plain = new MissingTypeClassLoader().newInstance("lcprobe.Plain", null);
        submit(() -> {
            bind("plain", plain);
            assertEquals(plain.toString(), getEngine().executeScript("String(plain)"));
        });
    }

    /*
     * Pins a known difference from the JNI build (FFM-ABI-CONTRACT.md section 13.3). Assigning a
     * final field, reading a field of a class that is not public or storing an element of the
     * wrong type made Field.get*, Field.set* or Array.set throw. JNI never cleared that exception,
     * so it came out of executeScript or JSObject.eval, or out of the next LiveConnect method call
     * in the same script. The port logs it and the script carries on. What both builds share is
     * that later Java calls in the same script still run, and that nothing of the failure is left
     * for the next script: its first canvas still gets a backing store.
     */
    @Test
    public void fieldAndArrayFailuresAreLoggedAndContained() {
        Logger logger = Logger.getLogger("com.sun.webkit.WebKitNative");
        List<String> severe = new CopyOnWriteArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel() == Level.SEVERE) {
                    severe.add(record.getMessage());
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        logger.addHandler(handler);
        try {
            submit(() -> {
                WebEngine web = getEngine();
                FinalFieldBridge app = new FinalFieldBridge();
                bind("app", app);
                assertEquals("255,255", web.executeScript(CANVAS_PROBE), "the probe itself");

                assertEquals(10, web.executeScript("app.max = 10"));
                assertEquals(5, app.max);
                assertNothingLeftForTheNextScript(web, "a failed field assignment");

                assertEquals(5, web.executeScript("app.max = 10; app.touch(); app.max"));
                assertEquals(1, app.touched, "a later Java call in the same script did not run");
                assertNothingLeftForTheNextScript(web, "a failed field assignment");

                bind("sealed", new SealedFields());
                assertEquals(0, web.executeScript("sealed.value"));
                assertNothingLeftForTheNextScript(web, "a failed field read");

                assertNull(web.executeScript("app.names[0] = app; app.names[0]"));
                assertNull(app.names[0]);
                assertNothingLeftForTheNextScript(web, "a failed array store");

                JSObject window = (JSObject) web.executeScript("window");
                assertEquals(10, window.eval("app.max = 10"));
                assertEquals(5, app.max);
                assertNothingLeftForTheNextScript(web, "a failed field assignment in JSObject.eval");
            });
        } finally {
            logger.removeHandler(handler);
        }
        assertTrue(severe.stream().anyMatch(message -> message.contains("liveconnect.field_set")),
                "the failed field assignment was not logged: " + severe);
        assertTrue(severe.stream().anyMatch(message -> message.contains("liveconnect.field_get")),
                "the failed field read was not logged: " + severe);
        assertTrue(severe.stream().anyMatch(message -> message.contains("liveconnect.array_set")),
                "the failed array store was not logged: " + severe);
    }

    /*
     * A doubleValue() that throws while script converts the object to a number. JNI left that
     * exception pending as it left a failed field access pending; the port logs it, the conversion
     * yields 0, and the next script is as unaffected as it is after a failed field access.
     */
    @Test
    public void aThrowingDoubleValueIsContainedAndLeavesNothingForTheNextScript() {
        submit(() -> {
            WebEngine web = getEngine();
            bind("faulty", new FaultyNumber());
            assertEquals("255,255", web.executeScript(CANVAS_PROBE), "the probe itself");
            assertEquals(0, web.executeScript("+faulty"));
            assertNothingLeftForTheNextScript(web, "a doubleValue() that threw");
        });
    }

    /*
     * A LiveConnect failure must not leave the per-thread upcall-failure flag set: no caller in the
     * bridge asks for it, so the first caller that does would be an unrelated one in a later
     * script, and CANVAS_PROBE is such a caller.
     */
    private static void assertNothingLeftForTheNextScript(WebEngine web, String failure) {
        assertEquals("255,255", web.executeScript(CANVAS_PROBE),
                failure + " left the upcall-failure flag set for the next script");
    }

    /** A number-like application class that is not a {@link Number}. */
    public static class Money {

        public double doubleValue() {
            return 7.25;
        }
    }

    private static class Hidden {

        double doubleValue() {
            return 9.5;
        }
    }

    /** A static doubleValue() is not the instance method GetMethodID looked for. */
    public static class StaticOnly {

        public static double doubleValue() {
            return 1.5;
        }
    }

    /**
     * A bridge object with a final field and a String array. The field is assigned in the
     * constructor so that it is not a constant variable, which javac would read without a getfield.
     */
    public static class FinalFieldBridge {

        public final int max;

        public final String[] names = new String[1];

        public int touched;

        public FinalFieldBridge() {
            max = 5;
        }

        public void touch() {
            touched++;
        }
    }

    /**
     * A public field of a class that is not public, which {@code Field.getInt} refuses to read
     * from outside the class's package.
     */
    private static class SealedFields {

        public int value = 3;
    }

    /** A number-like class whose {@code doubleValue()} fails. */
    public static class FaultyNumber {

        public double doubleValue() {
            throw new IllegalStateException("doubleValue fails on purpose");
        }
    }
}
