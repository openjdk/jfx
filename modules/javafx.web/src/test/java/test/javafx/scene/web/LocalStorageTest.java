/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.FutureTask;
import java.io.File;
import javafx.scene.web.WebEngineShim;
import javafx.concurrent.Worker;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;

import org.junit.Test;

public class LocalStorageTest extends TestBase {

    @Test
    public void testLocalStorage() throws Exception {
        final WebEngine webEngine = getEngine();
        webEngine.setJavaScriptEnabled(true);
        webEngine.setUserDataDirectory(new File("/tmp/java-store"));
        checkLocalStorageAfterWindowClose(webEngine);
    }
    private WebEngine createWebEngine() {
        return submit(() -> new WebEngine());
    }

    void checkLocalStorageAfterWindowClose(WebEngine webEngine) {
        load(new File("src/test/resources/test/html/localstorage.html"));
        submit(() -> {
            assertNotNull(webEngine.executeScript("localStorage;"));
            getEngine().executeScript("window.close();");
            assertNotNull(webEngine.executeScript("localStorage;"));
        });
    }

    @Test
    public void testLocalStorageSet() {
        load(new File("src/test/resources/test/html/localstorage.html"));
        submit(() -> {
            WebView view = getView();
            view.getEngine().executeScript("test_local_storage_set();");
            String s = (String) view.getEngine().executeScript("document.getElementById('key').innerText;");
            assertEquals(s, "1001");
        });
    }

    @Test
    public void testLocalStoargeClear() {
        load(new File("src/test/resources/test/html/localstorage.html"));
        submit(() -> {
            WebView view = getView();
            view.getEngine().executeScript("delete_items();");
            String s = (String) view.getEngine().executeScript("document.getElementById('key').innerText;");
            boolean res = (s == null || s.length() == 0);
            assertTrue(res);
        });
    }
}
