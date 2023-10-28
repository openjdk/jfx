/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import javafx.concurrent.Worker.State;

import static javafx.concurrent.Worker.State.SUCCEEDED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import java.io.File;
import java.io.IOException;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;

public class WebWorkerTest extends TestBase {

    private State getLoadState() {
        return submit(() -> getEngine().getLoadWorker().getState());
    }

    @Test
    public void testWorker() throws InterruptedException {
        final WebEngine webEngine = getEngine();
        webEngine.setJavaScriptEnabled(true);
        load(new File("src/test/resources/test/html/worker.html"));
        assertTrue("Load task completed successfully", getLoadState() == State.SUCCEEDED);

        Thread.sleep(500);

        submit(() -> {
            WebView view = getView();
            String res = (String) view.getEngine().executeScript("document.getElementById('result').innerText;");
            assertEquals("4", res);
        });
    }
}
