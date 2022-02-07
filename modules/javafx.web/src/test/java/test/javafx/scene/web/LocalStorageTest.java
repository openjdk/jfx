/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.fail;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.lang.String.format;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.concurrent.FutureTask;
import java.io.File;
import java.io.IOException;
import java.util.Random;


import javafx.scene.web.WebEngineShim;
import javafx.concurrent.Worker;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;


public class LocalStorageTest extends TestBase {

    private static final File LOCAL_STORAGE_DIR = new File("LocalStorageDir");
    private static final File PRE_LOCKED = new File("zoo_local_storage");

    private static RandomAccessFile preLockedRaf;
    private static FileLock preLockedLock;
    private static final Random random = new Random();

    private static void deleteRecursively(File file) throws IOException {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                deleteRecursively(f);
            }
        }
        if (!file.delete()) {
            // If WebKit takes time to close the file, better
            // delete it during VM shutdown.
            file.deleteOnExit();
        }
    }

    private WebEngine createWebEngine() {
        return submit(() -> new WebEngine());
    }

    /* test localstorage instance */
    void checkLocalStorageAfterWindowClose(WebEngine webEngine) {
        load(new File("src/test/resources/test/html/localstorage.html"));
        submit(() -> {
            assertNotNull(webEngine.executeScript("localStorage;"));
            getEngine().executeScript("window.close();");
            assertNotNull(webEngine.executeScript("localStorage;"));
        });
    }

    @BeforeClass
    public static void beforeClass() throws IOException {
        PRE_LOCKED.mkdirs();
        File preLockedFile = new File(PRE_LOCKED, ".lock");
        preLockedRaf = new RandomAccessFile(preLockedFile, "rw");
        preLockedLock = preLockedRaf.getChannel().tryLock();
        if (preLockedLock == null) {
            fail(format("Directory [%s] is already locked "
                    + "externally", PRE_LOCKED));
        }
    }

    @AfterClass
    public static void afterClass() throws IOException {
        preLockedLock.release();
        preLockedRaf.close();
        deleteRecursively(LOCAL_STORAGE_DIR);
        deleteRecursively(PRE_LOCKED);
    }

    @Test
    public void testLocalStorage() throws Exception {
        final WebEngine webEngine = getEngine();
        webEngine.setJavaScriptEnabled(true);
        webEngine.setUserDataDirectory(LOCAL_STORAGE_DIR);
        checkLocalStorageAfterWindowClose(webEngine);
    }

    /* test localstorage set data before window.close and check data after window.close */
    @Test
    public void testLocalStorageData() {
        load(new File("src/test/resources/test/html/localstorage.html"));
        submit(() -> {
            WebView view = getView();
            view.getEngine().executeScript("test_local_storage_set();"); // set data
            getEngine().executeScript("window.close();");
            //get data
            String s = (String) view.getEngine().executeScript("document.getElementById('key').innerText;");
            assertEquals(s, "1001");
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
            view.getEngine().executeScript("test_local_storage_set();"); // set data
            view.getEngine().executeScript("delete_items();");
            String s = (String) view.getEngine().executeScript("document.getElementById('key').innerText;");
            boolean res = (s == null || s.length() == 0);
            assertTrue(res);
        });
    }
}
