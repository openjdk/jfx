/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Test;
import org.junit.Before;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HistoryStateTest extends TestBase {
    private static final CountDownLatch historyStateLatch = new CountDownLatch(3);
    final AtomicInteger historyListenerIndex = new AtomicInteger(-1);

    private static final String resourcePath= "test/html/";
    private static final String initialLoadUrl = "archive-root0.html";
    private static final String firstLoadUrl = "archive-root1.html";
    private static final String secondLoadUrl = "archive-root2.html";
    private static final String replaceLoadUrl = "archive-root3.html";

    private static final String historyPushScript1 = "history.pushState({push1key : 1}, '', '?" +
            firstLoadUrl + "');";
    private static final String historyPushScript2 = "history.pushState({push2key : 2}, '', '?" +
            secondLoadUrl + "');";
    private static final String historyReplaceScript = "history.replaceState({replaceObject : 3}, '', '?" +
            replaceLoadUrl + "');";
    private static final String historyStateScript = "history.state";
    private static final String historyLengthScript = "history.length";
    private static final String historyGoBackScript = "history.go(-1)";
    private static final String historyGoForwardScript = "history.go(1)";
    private static final String historyBackcript = "history.back()";

    private static final int TIMEOUT = 30;    // seconds

    @Before
    public void before() {
        load(HistoryStateTest.class.getClassLoader().getResource(
                resourcePath + initialLoadUrl).toExternalForm());
    }

    @Test
    public void pushAndReplaceTest() throws Exception {
        // Initial history.state should be null
        assertNull(historyStateScript + " : Failed",
                executeScript(historyStateScript));
        // Initial history.length will be 1
        assertEquals(historyLengthScript + " : Failed",
                1, executeScript(historyLengthScript));

        // history.pushState({push1Key : 1}, '', '?firstLoadUrl"');
        executeScript(historyPushScript1);
        // Check if the history.state object for not null
        assertNotNull(historyStateScript + " : Failed",
                executeScript(historyStateScript));
        // {push1Key : 1} : {key = push1Key :value = (Integer) 1}
        assertEquals("history.state.push1key Failed",
                1, executeScript("history.state.push1key"));

        // history.length expected to be 2
        // Initial load + history.pushState(...)
        assertEquals(historyLengthScript + " : Failed",
                2, executeScript(historyLengthScript));

        // Check for WebEngine location is updated with new URL
        assertTrue(historyPushScript1 + " : Failed",
                getEngine().getLocation().endsWith(firstLoadUrl));


        executeScript(historyPushScript2);
        // {push2Key : 2} : {key = push1Key :value = (Integer) 2}
        assertEquals("history.state.push1key Failed",
                2, executeScript("history.state.push2key"));

        // history.length expected to be 2
        // Initial load + history.pushState(...)
        assertEquals(historyLengthScript + " : Failed",
                3, executeScript(historyLengthScript));

        // Check for WebEngine location is updated with new URL
        assertTrue(historyPushScript2 + " : Failed",
                getEngine().getLocation().endsWith(secondLoadUrl));

        executeScript(historyReplaceScript);
        // history.length remains same
        assertEquals(historyLengthScript + " : Failed",
                3, executeScript(historyLengthScript));

        assertEquals("history.state.replaceObject Failed",
                3, executeScript("history.state.replaceObject"));

        // Check for WebEngine location is updated with new URL
        assertTrue(historyPushScript2 + " : Failed",
                getEngine().getLocation().endsWith(replaceLoadUrl));

        submit(() -> {
            getEngine().locationProperty().addListener((observable, previousUrl, newUrl) -> {
                switch(historyListenerIndex.incrementAndGet()) {
                    case 0:
                        // call back to history.go(-1) --> newUrl = initialLoadURL
                        assertTrue(newUrl.endsWith(firstLoadUrl));
                        // history.go(1), navigate forward
                        getEngine().executeScript(historyGoForwardScript);
                        break;
                    case 1:
                        // call back to history.go(1) --> newURL = firstLoad
                        assertTrue(newUrl.endsWith(replaceLoadUrl));
                        // navigate back using history.back()
                        getEngine().executeScript(historyBackcript);
                        break;
                    case 2:
                        // call back to history.back() --> newURL = initialLoadUrl
                        assertTrue(newUrl.endsWith(firstLoadUrl));
                        break;
                    default:
                        fail();
                }
                historyStateLatch.countDown();
            });
            // history.go(-1), location will update in listener asynchronously
            // expected to go back to firstLoadUrl based on previous states
            // a. history.pushState(,,firstLoadUrl)
            // b. history.pushState(,,secondUrl)
            // c. history.replaceState(,,replaceLoadUrl)
            getEngine().executeScript(historyGoBackScript);
        });
        try {
            historyStateLatch.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            throw new AssertionError(ex);
        } finally {
            assertEquals("history navigation using javascript failed", 2, historyListenerIndex.get());
        }
    }
}

