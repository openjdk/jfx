/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import javafx.scene.paint.Color;
import javafx.application.Platform;
import javafx.scene.image.WritableImage;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import junit.framework.AssertionFailedError;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import util2.Util;

import static org.junit.Assert.*;
import static util2.Util.TIMEOUT;

/**
 * Tests for snapshot.
 */
public class Snapshot1Test extends SnapshotCommon {

    // Sleep time to verify that deferred snapshot is not called immediately
    static final int SLEEP_TIME = 100;

    // Short timeout used to ensure that bad callback is not called
    static final int SHORT_TIMEOUT = 1000;

    @BeforeClass
    public static void setupOnce() {
        doSetupOnce();
    }

    @AfterClass
    public static void teardownOnce() {
        doTeardownOnce();
    }

    // Temporary scene and node used for testing
    private Scene tmpScene = null;
    private Node tmpNode = null;

    @Before
    public void setupEach() {
        assertNotNull(myApp);
        assertNotNull(myApp.primaryStage);
        assertTrue(myApp.primaryStage.isShowing());
    }

    @After
    public void teardownEach() {
    }

    // ========================== TEST CASES ==========================

    // Verify that we cannot construct a Scene on a thread other than
    // the FX Application thread
    @Test (expected=IllegalStateException.class)
    public void testConstructSceneWrongThread() {
        assertFalse(Platform.isFxApplicationThread());

        Group root = new Group();

        // The following should throw IllegalStateException
        tmpScene = new Scene(root);
    }

    // Verify that we can construct a graph of nodes on a thread other than
    // the FX Application thread
    public void testConstructGraphOffThreadOk() {
        assertFalse(Platform.isFxApplicationThread());

        Group root = new Group();
        Rectangle rect = new Rectangle(10, 10);
        rect.setFill(Color.RED);
        root.getChildren().add(rect);
    }

    // Verify that we cannot call snapshot on a thread other than
    // the FX Application thread
    @Test (expected=IllegalStateException.class)
    public void testSnapshotSceneImmediateWrongThread() {
        assertFalse(Platform.isFxApplicationThread());

        Util.runAndWait(new Runnable() {
            public void run() {
                tmpScene = new Scene(new Group(), 200, 100);
            }
        });

        // Should throw IllegalStateException
        tmpScene.snapshot(null);
    }

    // Verify that we cannot call snapshot on a thread other than
    // the FX Application thread
    @Test (expected=IllegalStateException.class)
    public void testSnapshotSceneDeferredWrongThread() {
        assertFalse(Platform.isFxApplicationThread());

        Util.runAndWait(new Runnable() {
            public void run() {
                tmpScene = new Scene(new Group(), 200, 100);
            }
        });

        // Should throw IllegalStateException
        tmpScene.snapshot(new Callback<SnapshotResult, Void>() {
            public Void call(SnapshotResult p) {
                throw new AssertionFailedError("Should never get here");
            }
        }, null);
    }

    // Verify that we cannot call snapshot on a thread other than
    // the FX Application thread
    @Test (expected=IllegalStateException.class)
    public void testSnapshotNodeImmediateWrongThread() {
        assertFalse(Platform.isFxApplicationThread());

        tmpNode = new Rectangle(10, 10);

        // Should throw IllegalStateException
        tmpNode.snapshot(null, null);
    }

    // Verify that we cannot call snapshot on a thread other than
    // the FX Application thread
    @Test (expected=IllegalStateException.class)
    public void testSnapshotNodeDeferredWrongThread() {
        assertFalse(Platform.isFxApplicationThread());

        tmpNode = new Rectangle(10, 10);

        // Should throw IllegalStateException
        tmpNode.snapshot(new Callback<SnapshotResult, Void>() {
            public Void call(SnapshotResult p) {
                throw new AssertionFailedError("Should never get here");
            }
        }, null, null);
    }

    // Test immediate snapshot
    @Test
    public void testSceneImmediate() {
        Util.runAndWait(new Runnable() {
            public void run() {
                tmpScene = new Scene(new Group(), 200, 100);
                WritableImage img = tmpScene.snapshot(null);
                assertNotNull(img);
            }
        });
    }

    // Test deferred snapshot with callback
    @Test
    public void testSceneCallback() {
        final CountDownLatch latch = new CountDownLatch(1);

        Util.runAndWait(new Runnable() {
            public void run() {
                tmpScene = new Scene(new Group(), 200, 100);

                Callback<SnapshotResult, Void> cb = new Callback<SnapshotResult, Void>() {
                    @Override public Void call(SnapshotResult param) {
                        assertNotNull(param);

                        latch.countDown();
                        return null;
                    }
                };

                tmpScene.snapshot(cb, null);
                Util.sleep(SLEEP_TIME);
                assertEquals(1, latch.getCount());
            }
        });

        try {
            if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for snapshot callback");
            }
        } catch (InterruptedException ex) {
            AssertionFailedError err = new AssertionFailedError("Unexpected exception");
            err.initCause(ex);
            throw err;
        }

        assertEquals(0, latch.getCount());
    }

    // Test deferred snapshot with bad callback (should print a warning
    // but will not throw exception back to caller)
    @Test
    public void testBadSceneCallback1() {
        final CountDownLatch latch = new CountDownLatch(1);

        Util.runAndWait(new Runnable() {
            public void run() {
                tmpScene = new Scene(new Group(), 200, 100);

                Callback<SnapshotResult, Void> cb = new Callback() {
                    @Override public Object call(Object param) {
                        assertNotNull(param);

                        latch.countDown();
                        // The following will cause a ClassCastException warning
                        // message to be printed.
                        return "";
                    }
                };

                tmpScene.snapshot(cb, null);
                Util.sleep(SLEEP_TIME);
                assertEquals(1, latch.getCount());
                System.err.println("testBadSceneCallback1: a ClassCastException warning message is expected here");
            }
        });

        try {
            if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for snapshot callback");
            }
        } catch (InterruptedException ex) {
            AssertionFailedError err = new AssertionFailedError("Unexpected exception");
            err.initCause(ex);
            throw err;
        }

        assertEquals(0, latch.getCount());
    }

    // Test deferred snapshot with bad callback (should print a warning
    // but will not throw exception back to caller)
    @Test
    public void testBadSceneCallback2() {
        final CountDownLatch latch = new CountDownLatch(1);

        Util.runAndWait(new Runnable() {
            public void run() {
                tmpScene = new Scene(new Group(), 200, 100);

                Callback cb = new Callback<String, Integer>() {
                    @Override public Integer call(String param) {
                        // Should not get here
                        latch.countDown();
                        throw new AssertionFailedError("Should never get here");
                    }
                };

                tmpScene.snapshot(cb, null);
                Util.sleep(SLEEP_TIME);
                assertEquals(1, latch.getCount());
                System.err.println("testBadSceneCallback2: a ClassCastException warning message is expected here");
            }
        });

        try {
            if (latch.await(SHORT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Snapshot callback unexpectedly called");
            }
        } catch (InterruptedException ex) {
            AssertionFailedError err = new AssertionFailedError("Unexpected exception");
            err.initCause(ex);
            throw err;
        }

        assertEquals(1, latch.getCount());
    }

    // Test deferred snapshot with null callback (should throw NPE)
    @Test (expected=NullPointerException.class)
    public void testNullSceneCallback() {
        Util.runAndWait(new Runnable() {
            public void run() {
                tmpScene = new Scene(new Group(), 200, 100);
                tmpScene.snapshot(null, null);
            }
        });
    }

    // Test immediate snapshot
    @Test
    public void testNodeImmediate() {
        Util.runAndWait(new Runnable() {
            public void run() {
                tmpNode = new Rectangle(10, 10);
                WritableImage img = tmpNode.snapshot(null, null);
                assertNotNull(img);
            }
        });
    }

    // Test deferred snapshot with callback
    @Test
    public void testNodeCallback() {
        final CountDownLatch latch = new CountDownLatch(1);

        Util.runAndWait(new Runnable() {
            public void run() {
                tmpNode = new Rectangle(10, 10);

                Callback<SnapshotResult, Void> cb = new Callback<SnapshotResult, Void>() {
                    @Override public Void call(SnapshotResult param) {
                        assertNotNull(param);

                        latch.countDown();
                        return null;
                    }
                };

                tmpNode.snapshot(cb, null, null);
                Util.sleep(SLEEP_TIME);
                assertEquals(1, latch.getCount());
            }
        });

        try {
            if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for snapshot callback");
            }
        } catch (InterruptedException ex) {
            AssertionFailedError err = new AssertionFailedError("Unexpected exception");
            err.initCause(ex);
            throw err;
        }

        assertEquals(0, latch.getCount());
    }

    // Test deferred snapshot with bad callback (should print a warning
    // but will not throw exception back to caller)
    @Test
    public void testBadNodeCallback1() {
        final CountDownLatch latch = new CountDownLatch(1);

        Util.runAndWait(new Runnable() {
            public void run() {
                tmpNode = new Rectangle(10, 10);

                Callback<SnapshotResult, Void> cb = new Callback() {
                    @Override public Object call(Object param) {
                        assertNotNull(param);

                        latch.countDown();
                        // The following will cause a ClassCastException warning
                        // message to be printed.
                        return "";
                    }
                };

                tmpNode.snapshot(cb, null, null);
                Util.sleep(SLEEP_TIME);
                assertEquals(1, latch.getCount());
                System.err.println("testBadNodeCallback1: a ClassCastException warning message is expected here");
            }
        });

        try {
            if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for snapshot callback");
            }
        } catch (InterruptedException ex) {
            AssertionFailedError err = new AssertionFailedError("Unexpected exception");
            err.initCause(ex);
            throw err;
        }

        assertEquals(0, latch.getCount());
    }

    // Test deferred snapshot with bad callback (should print a warning
    // but will not throw exception back to caller)
    @Test
    public void testBadNodeCallback2() {
        final CountDownLatch latch = new CountDownLatch(1);

        Util.runAndWait(new Runnable() {
            public void run() {
                tmpNode = new Rectangle(10, 10);

                Callback cb = new Callback<String, Integer>() {
                    @Override public Integer call(String param) {
                        // Should not get here
                        latch.countDown();
                        throw new AssertionFailedError("Should never get here");
                    }
                };

                tmpNode.snapshot(cb, null, null);
                Util.sleep(SLEEP_TIME);
                assertEquals(1, latch.getCount());
                System.err.println("testBadNodeCallback2: a ClassCastException warning message is expected here");
            }
        });

        try {
            if (latch.await(SHORT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Snapshot callback unexpectedly called");
            }
        } catch (InterruptedException ex) {
            AssertionFailedError err = new AssertionFailedError("Unexpected exception");
            err.initCause(ex);
            throw err;
        }

        assertEquals(1, latch.getCount());
    }

    // Test deferred snapshot with null callback (should throw NPE)
    @Test (expected=NullPointerException.class)
    public void testNullNodeCallback() {
        Util.runAndWait(new Runnable() {
            public void run() {
                tmpNode = new Rectangle(10, 10);
                tmpNode.snapshot(null, null, null);
            }
        });
    }

    // TODO: the following will be covered by ImageOps unit tests, so can be removed

    @Test (expected=IllegalArgumentException.class)
    public void testCreateImageZero() {
        WritableImage wimg = new WritableImage(0, 0);
    }

    @Test
    public void testCreateImageNonZero() {
        WritableImage wimg = new WritableImage(1, 1);
    }
}
