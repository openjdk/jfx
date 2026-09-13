/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.scene.web.WebEngine;
import static javafx.concurrent.Worker.State.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

public class LoadNotificationsTest extends TestBase {

    Set<State> log = new HashSet<>();
    AssertionError assertion;
    String currentUrl;

    @Test public void testSuccessfulLoad() {
        currentUrl = "src/test/resources/test/html/ipsum.html";
        testUrl(new File(currentUrl).toURI().toASCIIString());
    }

    @Test public void testFailedLoad() {
        currentUrl = "no.such.file";
        testUrl(currentUrl);
    }

    @Test public void testEmptyLoad() {
        currentUrl = "about:blank";
        testUrl(null);
        testUrl("");
        testUrl("about:blank");
    }

    private void testUrl(String url) {
        log.clear();
        assertion = null;
        submit(() -> {
            checkState(RUNNING, getEngine().getLoadWorker().getState());
        });
        load(url);
        check();
    }

    @Override public void changed(ObservableValue property, Object oldValue, Object newValue) {
        Worker<Void> worker = getEngine().getLoadWorker();
        if (property == worker.runningProperty()) {
            checkRunning((Boolean)oldValue, (Boolean)newValue);
        } else if (property == worker.stateProperty()) {
            checkState((State)oldValue, (State)newValue);
        }
    }

    /**
     * Check that by the time the LoadWorker.running property changes,
     * the [state] property is set.
     */
    private void checkRunning(boolean oldValue, boolean newValue) {
        State state = getEngine().getLoadWorker().getState();
        if (newValue) {
            assertTrue(state == SCHEDULED || state == RUNNING, "LoadWorker.running should be false");
        } else {
            assertTrue(state == SUCCEEDED || state == FAILED, "LoadWorker.running should be true");
        }
    }

    /**
     * Check that by the time LoadWorker.state changes, all other LoadWorker
     * properties except for [running] have been set. E.g. when state becomes
     * SUCCEEDED, [workDone] should be 100, [progress] should be 1,
     * [exception] should be null etc. We also check that state changes occur
     * in proper order: READY -> SCHEDULED -> RUNNING -> SUCCEEDED or FAILED.
     */
    private void checkState(State oldValue, State newValue) {
        WebEngine web = getEngine();
        Worker<Void> worker = web.getLoadWorker();
        log.add(newValue);

        try {
            switch (newValue) {
                case READY:
                    assertEquals(-1, worker.getTotalWork(), 0, "LoadWorker.totalWork");
                    assertEquals(-1, worker.getWorkDone(), 0, "LoadWorker.workDone");
                    assertEquals(-1, worker.getProgress(), 0, "LoadWorker.progress");
                    assertNull(worker.getException(), "LoadWorker.exception should be null");
                    assertEquals("", worker.getMessage(), "LoadWorker.message");
                    break;
                case SCHEDULED:
                case RUNNING:
                    assertEquals((newValue == RUNNING ? SCHEDULED : READY), oldValue, "LoadWorker.state");
                    assertEquals(100.0, worker.getTotalWork(), 0, "LoadWorker.totalWork");
                    assertEquals(0.0, worker.getWorkDone(), 0, "LoadWorker.workDone");
                    assertEquals(0.0, worker.getProgress(), 0, "LoadWorker.progress");
                    assertNull(worker.getException(), "LoadWorker.exception should be null");
                    assertTrue(worker.getMessage().matches("Loading .*" + currentUrl), "LoadWorker.message should read 'Loading [url]'");

                    assertNull(web.getDocument(), "WebEngine.document should be null");
                    /*
                    The following assert causes test failure after JDK-8268849.
                    An issue is raised: JDK-8269912, to investigate the failure.
                    // assertNull(web.getTitle(), "WebEngine.title should be null");
                    */
                    assertTrue(web.getLocation().endsWith(currentUrl), "WebEngine.location should be set");
                    break;
                case SUCCEEDED:
                    assertEquals(RUNNING, oldValue, "LoadWorker.state");
                    assertEquals(100.0, worker.getTotalWork(), 0, "LoadWorker.totalWork");
                    assertEquals(100.0, worker.getWorkDone(), 0, "LoadWorker.workDone");
                    assertEquals(1.0, worker.getProgress(), 0, "LoadWorker.progress");
                    assertNull(worker.getException(), "LoadWorker.exception should be null");
                    assertTrue(worker.getMessage().startsWith("Loading complete"), "LoadWorker.message should read 'Loading complete'");

                    assertNotNull(web.getDocument(), "WebEngine.document should be set");
                    assertTrue(web.getLocation().endsWith(currentUrl), "WebEngine.location should be set");
                    /*
                    This commented code block causes test failure after JDK-8268849.
                    An issue is raised: JDK-8269912, to investigate the failure.

                    if (currentUrl == "about:blank") {
                        assertNull(web.getTitle(), "WebEngine.title should be null");
                    } else {
                        assertNotNull(web.getTitle(), "WebEngine.title should be set");
                    }
                    */
                    break;
                case FAILED:
                    assertEquals(RUNNING, oldValue, "LoadWorker.state");
                    assertEquals(100.0, worker.getTotalWork(), 0, "LoadWorker.totalWork");
                    assertNotNull(worker.getException(), "LoadWorker.exception should be set");
                    assertTrue(worker.getMessage().startsWith("Loading failed"), "LoadWorker.message should read 'Loading failed'");
                    break;
                default:
                    fail("Unexpected LoadWorker.state == " + newValue);
            }
        } catch (AssertionError e) {
            assertion = e;
        }
    }

    private void check() {
        // check if any assertions have failed
        if (assertion != null) {
            throw assertion;
        }

        // check that we have transitioned through all relevant states
        // (order of states is checked in the changed() method)
        assertTrue(log.contains(READY), "State.READY was never set");
        assertTrue(log.contains(SCHEDULED), "State.SCHEDULED was never set");
        assertTrue(log.contains(RUNNING), "State.RUNNING was never set");
        assertTrue(log.contains(SUCCEEDED) || log.contains(FAILED), "Neither State.SUCCEEDED nor State.FAILED has been set");
    }
}
