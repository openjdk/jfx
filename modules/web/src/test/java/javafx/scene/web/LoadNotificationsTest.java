/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.web;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import static javafx.concurrent.Worker.State.*;
import javafx.scene.web.WebEngine;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

public class LoadNotificationsTest extends TestBase {
    
    Set<State> log = new HashSet<State>();
    AssertionError assertion;

    @Test public void testSuccessfulLoad() {
        testFile("src/test/resources/html/ipsum.html");
    }

    @Test public void testFailedLoad() {
        testFile("no.such.file");
    }
    
    private void testFile(String fileName) {
        log.clear();
        assertion = null;
        load(new File(fileName));
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
            assertTrue("LoadWorker.running should be false",
                    state == SCHEDULED || state == RUNNING);
        } else {
            assertTrue("LoadWorker.running should be true",
                    state == SUCCEEDED || state == FAILED);
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
                    assertEquals("LoadWorker.totalWork", -1, worker.getTotalWork(), 0);
                    assertEquals("LoadWorker.workDone", -1, worker.getWorkDone(), 0);
                    assertEquals("LoadWorker.progress", -1, worker.getProgress(), 0);
                    assertNull("LoadWorker.exception should be null", worker.getException());
                    assertEquals("LoadWorker.message", "", worker.getMessage());
                    break;
                case SCHEDULED:
                case RUNNING:
                    assertEquals("LoadWorker.state",
                            (newValue == RUNNING ? SCHEDULED : READY),
                            oldValue);
                    assertEquals("LoadWorker.totalWork", 100.0, worker.getTotalWork(), 0);
                    assertEquals("LoadWorker.workDone", 0.0, worker.getWorkDone(), 0);
                    assertEquals("LoadWorker.progress", 0.0, worker.getProgress(), 0);
                    assertNull("LoadWorker.exception should be null", worker.getException());
                    assertTrue("LoadWorker.message should read 'Loading [url]'",
                            worker.getMessage().startsWith("Loading file:"));
                    
                    assertNull("WebEngine.document should be null", web.getDocument());
                    assertNull("WebEngine.title should be null", web.getTitle());
                    assertTrue("WebEngine.location should be set",
                            web.getLocation().startsWith("file:"));
                    break;
                case SUCCEEDED:
                    assertEquals("LoadWorker.state", RUNNING, oldValue);
                    assertEquals("LoadWorker.totalWork", 100.0, worker.getTotalWork(), 0);
                    assertEquals("LoadWorker.workDone", 100.0, worker.getWorkDone(), 0);
                    assertEquals("LoadWorker.progress", 1.0, worker.getProgress(), 0);
                    assertNull("LoadWorker.exception should be null", worker.getException());
                    assertTrue("LoadWorker.message should read 'Loading complete'",
                            worker.getMessage().startsWith("Loading complete"));
                    
                    assertNotNull("WebEngine.document should be set", web.getDocument());
                    assertNotNull("WebEngine.title should be set", web.getTitle());
                    assertTrue("WebEngine.location should be set",
                            web.getLocation().startsWith("file:"));
                    break;
                case FAILED:
                    assertEquals("LoadWorker.state", RUNNING, oldValue);
                    assertEquals("LoadWorker.totalWork", 100.0, worker.getTotalWork(), 0);
                    assertNotNull("LoadWorker.exception should be set", worker.getException());
                    assertTrue("LoadWorker.message should read 'Loading failed'",
                            worker.getMessage().startsWith("Loading failed"));
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
        assertTrue("State.READY was never set", log.contains(READY));
        assertTrue("State.SCHEDULED was never set", log.contains(SCHEDULED));
        assertTrue("State.RUNNING was never set", log.contains(RUNNING));
        assertTrue("Neither State.SUCCEEDED nor State.FAILED has been set",
                log.contains(SUCCEEDED) || log.contains(FAILED));
    }
}
