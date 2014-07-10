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

import java.io.File;
import static java.lang.String.format;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.w3c.dom.Document;

public class MiscellaneousTest extends TestBase {

    @Test public void testNoEffectOnFollowRedirects() {
        assertEquals("Unexpected HttpURLConnection.getFollowRedirects() result",
                true, HttpURLConnection.getFollowRedirects());
        load("test/html/ipsum.html");
        assertEquals("Unexpected HttpURLConnection.getFollowRedirects() result",
                true, HttpURLConnection.getFollowRedirects());
    }

    @Test public void testRT22458() throws Exception {
        final WebEngine webEngine = createWebEngine();
        Platform.runLater(() -> {
            webEngine.load(format("file://%d.ajax.googleapis.com/ajax",
                                  new Random().nextInt()));
        });
        Thread.sleep(200);
        long startTime = System.currentTimeMillis();
        DummyClass.dummyField++;
        long time = System.currentTimeMillis() - startTime;
        if (time > 2000) {
            fail(format("DummyClass took %f seconds to load", time / 1000.));
        }
    }

    private static final class DummyClass {
        private static int dummyField;
    }

    @org.junit.Ignore
    @Test public void testRT30835() throws Exception {
        class Record {
            private final Document document;
            private final String location;
            public Record(Document document, String location) {
                this.document = document;
                this.location = location;
            }
        }
        final ArrayList<Record> records = new ArrayList<Record>();
        ChangeListener<State> listener = (ov, oldValue, newValue) -> {
            if (newValue == State.SUCCEEDED) {
                records.add(new Record(
                        getEngine().getDocument(),
                        getEngine().getLocation()));
            }
        };
        submit(() -> {
            getEngine().getLoadWorker().stateProperty().addListener(listener);
        });
        String location = new File("src/test/resources/html/RT30835.html")
                .toURI().toASCIIString().replaceAll("^file:/", "file:///");
        load(location);
        assertEquals(1, records.size());
        assertNotNull(records.get(0).document);
        assertEquals(location, records.get(0).location);
    }

    @Test public void testRT26306() {
        loadContent(
                "<script language='javascript'>\n" +
                "var s = '0123456789abcdef';\n" +
                "while (true) {\n" +
                "    alert(s.length);\n" +
                "    s = s + s;\n" +
                "}\n" +
                "</script>");
    }

    private WebEngine createWebEngine() {
        return submit(() -> new WebEngine());
    }
}
