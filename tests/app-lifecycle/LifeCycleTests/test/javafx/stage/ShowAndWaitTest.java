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

package javafx.stage;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import junit.framework.AssertionFailedError;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import util.Util;

import static org.junit.Assert.*;
import static util.Util.TIMEOUT;

/**
 * Test program for showAndWait functionality.
 */
@RunWith(Parameterized.class)
public class ShowAndWaitTest {

    // Maximum number of stages
    private static final int MAX_STAGES = 10;

    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Singleton Application instance
    private static MyApp myApp;

    // Application class. An instance is created and initialized before running
    // the first test, and it lives through the execution of all tests.
    public static class MyApp extends Application {
        private Stage primaryStage;

        @Override public void init() {
            ShowAndWaitTest.myApp = this;
        }

        @Override public void start(Stage primaryStage) throws Exception {
            primaryStage.setTitle("Primary stage");
            Group root = new Group();
            Scene scene = new Scene(root);
            scene.setFill(Color.LIGHTYELLOW);
            primaryStage.setScene(scene);
            primaryStage.setX(0);
            primaryStage.setY(0);
            primaryStage.setWidth(210);
            primaryStage.setHeight(180);

            this.primaryStage = primaryStage;
            launchLatch.countDown();
        }
    }

    private static class TestStage extends Stage {

        private TestStage(Modality modality) {
            this(modality, modality == Modality.WINDOW_MODAL ? myApp.primaryStage : null);
        }

        private TestStage(Modality modality, Window owner) {
            this.setTitle("Test stage");
            this.initModality(modality);
            this.initOwner(owner);

            Group root = new Group();
            Scene scene = new Scene(root);
            this.setScene(scene);
            this.setWidth(200);
            this.setHeight(150);
            this.setX(225);
            this.setY(0);
        }
    }

    @BeforeClass
    public static void setupOnce() {
        // Start the Application
        new Thread(new Runnable() {
            @Override public void run() {
                Application.launch(MyApp.class, (String[])null);
            }
        }).start();

        try {
            if (!launchLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for Application to launch");
            }
        } catch (InterruptedException ex) {
            AssertionFailedError err = new AssertionFailedError("Unexpected exception");
            err.initCause(ex);
            throw err;
        }
    }

    @AfterClass
    public static void teardownOnce() {
        Platform.exit();
    }

    // Modality of the secondary stage(s) for a particular tests
    final Modality modality;

    // Set of stages being tested so that they can be hidden at the
    // end of of a failing test
    private HashSet<Stage> stages = new HashSet<Stage>();

    // Secondary stages used for testing
    private Stage tmpStage1 = null;
    private Stage tmpStage2 = null;

    @Parameters
    public static Collection getParams() {
        return Arrays.asList(new Object[][] {
            { Modality.NONE },
            { Modality.WINDOW_MODAL },
            { Modality.APPLICATION_MODAL },
        });
    }

    public ShowAndWaitTest(Modality modality) {
        this.modality = modality;
    }

    @Before
    public void setupEach() {
        assertNotNull(myApp);
        assertNotNull(myApp.primaryStage);
    }

    @After
    public void teardownEach() {
        for (final Stage stage : stages) {
            if (stage.isShowing()) {
                System.err.println("Cleaning up stage after a failed test...");
                try {
                    Util.runAndWait(new Runnable() {
                        public void run() {
                            stage.hide();
                        }
                    });
                } catch (Throwable t) {
                    System.err.println("WARNING: unable to hide stage after test failure");
                    t.printStackTrace(System.err);
                }
            }
        }
    }

    // ========================== TEST CASES ==========================

    // This test must be run before any other test, because it first verifies
    // that the primary stage is not yet showing, and finally shows it.
    // Since JUnit does not guarantee test execution order, each test that
    // relies on this must call ensureTest1(). This test does not use the
    // test parameters and only runs one in total.
    //
    // Consider moving to the setupOnce method.
    private static boolean test1Run = false;
    public void ensureTest1() {
        if (!test1Run) {
            test1();
        }
    }

    @Test
    public void test1() {
        if (test1Run) {
            return;
        }
        test1Run = true;

        assertEquals(0, launchLatch.getCount());
        Util.runAndWait(new Runnable() {
            @Override public void run() {
                assertTrue(Platform.isFxApplicationThread());
                assertTrue(myApp.primaryStage.isPrimary());
                assertFalse(myApp.primaryStage.isShowing());

                // Verify that we cannot call showAndWait on the primaryStage
                try {
                    myApp.primaryStage.showAndWait();
                    throw new AssertionFailedError("Expected IllegalStateException was not thrown");
                } catch (IllegalStateException ex) {
                }

                myApp.primaryStage.show();
            }
        });
    }

    // Verify that we cannot construct a stage on a thread other than
    // the FX Application thread
    @Test (expected=IllegalStateException.class)
    public void testConstructWrongThread() {
        ensureTest1();
        assertFalse(Platform.isFxApplicationThread());

        // The following should throw IllegalStateException
        tmpStage1 = new TestStage(modality);
        stages.add(tmpStage1);
    }


    // Verify that we cannot call showAndWait on a thread other than
    // the FX Application thread
    @Test (expected=IllegalStateException.class)
    public void testShowWaitWrongThread() {
        ensureTest1();
        assertFalse(Platform.isFxApplicationThread());
        Util.runAndWait(new Runnable() {
            public void run() {
                tmpStage1 = new TestStage(modality);
                stages.add(tmpStage1);
                assertFalse(tmpStage1.isPrimary());
                assertFalse(tmpStage1.isShowing());
            }
        });
        assertNotNull(tmpStage1);

        // The following should throw IllegalStateException
        tmpStage1.showAndWait();
    }

    // Verify that we cannot call showAndWait on a visible stage
    @Test (expected=IllegalStateException.class)
    public void testVisibleThrow() {
        ensureTest1();
        Util.runAndWait(new Runnable() {
            public void run() {
                tmpStage1 = new TestStage(modality);
                stages.add(tmpStage1);
                assertFalse(tmpStage1.isPrimary());
                assertFalse(tmpStage1.isShowing());
                tmpStage1.show();
                assertTrue(tmpStage1.isShowing());

                try {
                    // The following should throw IllegalStateException
                    tmpStage1.showAndWait();
                } finally {
                    tmpStage1.hide();
                }
            }
        });
    }

    // Verify that show returns right away; hide the stage after 500 msec
    @Test
    public void testNotBlocking() {
        ensureTest1();

        final AtomicBoolean stageShowReturned = new AtomicBoolean(false);
        final AtomicBoolean hideActionReached = new AtomicBoolean(false);

        Runnable rShow = new Runnable() {
            @Override public void run() {
                tmpStage1 = new TestStage(modality);
                stages.add(tmpStage1);
                assertFalse(tmpStage1.isPrimary());
                assertFalse(tmpStage1.isShowing());
                tmpStage1.show();
                stageShowReturned.set(true);
                assertTrue(tmpStage1.isShowing());
                assertFalse(hideActionReached.get());
            }
        };

        Runnable rHide = new Runnable() {
            @Override public void run() {
                assertNotNull(tmpStage1);
                assertTrue(tmpStage1.isShowing());
                assertTrue(stageShowReturned.get());
                hideActionReached.set(true);
                tmpStage1.hide();
            }
        };

        Util.runAndWait(rShow, rHide);

        assertFalse(tmpStage1.isShowing());
    }

    // Verify that showAndWait blocks until the stage is hidden.
    // Verify that the nested event loop exits immediately after
    // the event handler that calls hide returns, before running
    // the next Runnable.
    @Test
    public void testSingle() {
        ensureTest1();

        final AtomicBoolean stage1ShowReturned = new AtomicBoolean(false);
        final AtomicBoolean hide1EventReached = new AtomicBoolean(false);
        final AtomicBoolean nextRunnableReached = new AtomicBoolean(false);

        Runnable rShow1 = new Runnable() {
            @Override public void run() {
                tmpStage1 = new TestStage(modality);
                stages.add(tmpStage1);
                assertFalse(tmpStage1.isPrimary());
                assertFalse(tmpStage1.isShowing());
                tmpStage1.showAndWait();
                stage1ShowReturned.set(true);
                assertFalse(tmpStage1.isShowing());
                assertTrue(hide1EventReached.get());
                assertFalse(nextRunnableReached.get());
            }
        };

        Runnable rHide1 = new Runnable() {
            @Override public void run() {
                hide1EventReached.set(true);
                assertFalse(stage1ShowReturned.get());
                assertNotNull(tmpStage1);
                tmpStage1.hide();
                Util.sleep(1);
                assertFalse(stage1ShowReturned.get());
            }
        };

        Runnable rNext = new Runnable() {
            public void run() {
                // This should happen after the nested event loop exits
                nextRunnableReached.set(true);
            }
        };

        Util.runAndWait(rShow1, rHide1, rNext);

        assertFalse(tmpStage1.isShowing());
    }

    // Verify that showAndWait blocks until the stage is hidden.
    // Verify that the nested event loop exits immediately after
    // the event handler that calls hide returns, before running
    // the next Runnable (called from rShow1 after showAndWait returns).

    @Test
    public void testSingle_Chained() {
        ensureTest1();

        final AtomicBoolean stage1ShowReturned = new AtomicBoolean(false);
        final AtomicBoolean hide1EventReached = new AtomicBoolean(false);
        final AtomicBoolean nextRunnableReached = new AtomicBoolean(false);

        Runnable rShow1 = new Runnable() {
            @Override public void run() {
                tmpStage1 = new TestStage(modality);
                stages.add(tmpStage1);
                assertFalse(tmpStage1.isPrimary());
                assertFalse(tmpStage1.isShowing());
                tmpStage1.showAndWait();
                stage1ShowReturned.set(true);
                assertFalse(tmpStage1.isShowing());
                assertTrue(hide1EventReached.get());
                assertFalse(nextRunnableReached.get());
            }
        };

        Runnable rHide1 = new Runnable() {
            @Override public void run() {
                hide1EventReached.set(true);
                assertFalse(stage1ShowReturned.get());
                assertNotNull(tmpStage1);
                tmpStage1.hide();
                Util.sleep(1);
                assertFalse(stage1ShowReturned.get());
                Platform.runLater(new Runnable() {
                    public void run() {
                        // This should happen after the nested event loop exits
                        nextRunnableReached.set(true);
                    }
                });
            }
        };

        Util.runAndWait(rShow1, rHide1);

        assertFalse(tmpStage1.isShowing());
    }

    // Verify two nested event loops, with the stages being hidden in the
    // reverse order that they are shown
    @Test
    public void testTwoNested() {
        ensureTest1();

        final AtomicBoolean stage1ShowReturned = new AtomicBoolean(false);
        final AtomicBoolean hide1EventReached = new AtomicBoolean(false);
        final AtomicBoolean stage2ShowReturned = new AtomicBoolean(false);
        final AtomicBoolean hide2EventReached = new AtomicBoolean(false);

        Runnable rShow1 = new Runnable() {
            @Override public void run() {
                tmpStage1 = new TestStage(modality);
                stages.add(tmpStage1);
                assertFalse(tmpStage1.isPrimary());
                assertFalse(tmpStage1.isShowing());
                tmpStage1.showAndWait();
                stage1ShowReturned.set(true);
                assertFalse(tmpStage1.isShowing());
                assertTrue(stage2ShowReturned.get());
                assertTrue(hide1EventReached.get());
                assertTrue(hide2EventReached.get());
            }
        };

        Runnable rShow2 = new Runnable() {
            @Override public void run() {
                tmpStage2 = new TestStage(modality);
                stages.add(tmpStage2);
                assertFalse(tmpStage2.isPrimary());
                assertFalse(tmpStage2.isShowing());
                tmpStage2.showAndWait();
                stage2ShowReturned.set(true);
                assertFalse(stage1ShowReturned.get());
                assertFalse(tmpStage2.isShowing());
                assertTrue(hide2EventReached.get());
                assertFalse(hide1EventReached.get());
            }
        };

        Runnable rHide1 = new Runnable() {
            @Override public void run() {
                hide1EventReached.set(true);
                assertFalse(stage1ShowReturned.get());
                assertTrue(stage2ShowReturned.get());
                assertTrue(hide2EventReached.get());
                assertNotNull(tmpStage1);
                tmpStage1.hide();
                Util.sleep(1);
                assertFalse(stage1ShowReturned.get());
            }
        };

        Runnable rHide2 = new Runnable() {
            @Override public void run() {
                hide2EventReached.set(true);
                assertFalse(stage2ShowReturned.get());
                assertFalse(stage1ShowReturned.get());
                assertFalse(hide1EventReached.get());
                assertNotNull(tmpStage2);
                tmpStage2.hide();
                Util.sleep(1);
                assertFalse(stage2ShowReturned.get());
            }
        };

        Util.runAndWait(rShow1, rShow2, rHide2, rHide1);

        assertFalse(tmpStage1.isShowing());
        assertFalse(tmpStage2.isShowing());
    }

    // Verify two nested event loops, with the stages being hidden in the
    // same order that they are shown
    @Test
    public void testTwoInterleaved() {
        ensureTest1();

        final AtomicBoolean stage1ShowReturned = new AtomicBoolean(false);
        final AtomicBoolean hide1EventReached = new AtomicBoolean(false);
        final AtomicBoolean stage2ShowReturned = new AtomicBoolean(false);
        final AtomicBoolean hide2EventReached = new AtomicBoolean(false);

        Runnable rShow1 = new Runnable() {
            @Override public void run() {
                tmpStage1 = new TestStage(modality);
                stages.add(tmpStage1);
                assertFalse(tmpStage1.isPrimary());
                assertFalse(tmpStage1.isShowing());
                tmpStage1.showAndWait();
                stage1ShowReturned.set(true);
                assertFalse(tmpStage1.isShowing());
                assertTrue(stage2ShowReturned.get());
                assertTrue(hide1EventReached.get());
                assertTrue(hide2EventReached.get());
            }
        };

        Runnable rShow2 = new Runnable() {
            @Override public void run() {
                tmpStage2 = new TestStage(modality);
                stages.add(tmpStage2);
                assertFalse(tmpStage2.isPrimary());
                assertFalse(tmpStage2.isShowing());
                tmpStage2.showAndWait();
                stage2ShowReturned.set(true);
                assertFalse(tmpStage2.isShowing());
                assertFalse(stage1ShowReturned.get());
                assertTrue(hide2EventReached.get());
                assertTrue(hide1EventReached.get());
            }
        };

        Runnable rHide1 = new Runnable() {
            @Override public void run() {
                hide1EventReached.set(true);
                assertFalse(stage1ShowReturned.get());
                assertFalse(stage2ShowReturned.get());
                assertFalse(hide2EventReached.get());
                assertNotNull(tmpStage1);
                tmpStage1.hide();
                Util.sleep(1);
                assertFalse(stage1ShowReturned.get());
            }
        };

        Runnable rHide2 = new Runnable() {
            @Override public void run() {
                hide2EventReached.set(true);
                assertFalse(stage2ShowReturned.get());
                assertFalse(stage1ShowReturned.get());
                assertTrue(hide1EventReached.get());
                assertNotNull(tmpStage2);
                tmpStage2.hide();
                Util.sleep(1);
                assertFalse(stage2ShowReturned.get());
            }
        };

        Util.runAndWait(rShow1, rShow2, rHide1, rHide2);

        assertFalse(tmpStage1.isShowing());
        assertFalse(tmpStage2.isShowing());
    }

    // Verify multiple nested event loops, with the stages being hidden in the
    // reverse order that they are shown
    @Test
    public void testMultipleNested() {
        ensureTest1();

        final int N = MAX_STAGES;
        final Stage[] tmpStage = new Stage[N];
        final AtomicBoolean[] stageShowReturned = new AtomicBoolean[N];
        final AtomicBoolean[] hideEventReached = new AtomicBoolean[N];
        final Runnable[] rShow = new Runnable[N];
        final Runnable[] rHide = new Runnable[N];

        for (int i = 0; i < N; i++) {
            final int idx = i;
            stageShowReturned[idx] = new AtomicBoolean(false);
            hideEventReached[idx] = new AtomicBoolean(false);
            rShow[idx] = new Runnable() {
                @Override public void run() {
                    tmpStage[idx] = new TestStage(modality);
                    stages.add(tmpStage[idx]);
                    assertFalse(tmpStage[idx].isShowing());
                    tmpStage[idx].showAndWait();
                    stageShowReturned[idx].set(true);
                    assertFalse(tmpStage[idx].isShowing());
                    assertTrue(hideEventReached[idx].get());
                    for (int j = 0; j < idx; j++) {
                        assertFalse(stageShowReturned[j].get());
                        assertFalse(hideEventReached[j].get());
                    }
                    for (int j = idx+1; j < N; j++) {
                        assertTrue(stageShowReturned[j].get());
                        assertTrue(hideEventReached[j].get());
                    }
                }
            };

            rHide[idx] = new Runnable() {
                @Override public void run() {
                    hideEventReached[idx].set(true);
                    assertFalse(stageShowReturned[idx].get());
                    for (int j = 0; j < idx; j++) {
                        assertFalse(stageShowReturned[j].get());
                        assertFalse(hideEventReached[j].get());
                    }
                    for (int j = idx+1; j < N; j++) {
                        assertTrue(stageShowReturned[j].get());
                        assertTrue(hideEventReached[j].get());
                    }
                    assertNotNull(tmpStage[idx]);
                    tmpStage[idx].hide();
                    Util.sleep(1);
                    assertFalse(stageShowReturned[idx].get());
                }
            };
        }

        final Runnable[] runnables = new Runnable[2*N];
        for (int i = 0; i < N; i++) {
            runnables[i] = rShow[i];
            runnables[(2*N - i - 1)] = rHide[i];
        }
        Util.runAndWait(runnables);

        for (int i = 0; i < N; i++) {
            assertFalse(tmpStage[i].isShowing());
        }
    }

    // Verify multiple nested event loops, with the stages being hidden in the
    // reverse order that they are shown
    @Test
    public void testMultipleInterleaved() {
        ensureTest1();

        final int N = MAX_STAGES;
        final Stage[] tmpStage = new Stage[N];
        final AtomicBoolean[] stageShowReturned = new AtomicBoolean[N];
        final AtomicBoolean[] hideEventReached = new AtomicBoolean[N];
        final Runnable[] rShow = new Runnable[N];
        final Runnable[] rHide = new Runnable[N];

        for (int i = 0; i < N; i++) {
            final int idx = i;
            stageShowReturned[idx] = new AtomicBoolean(false);
            hideEventReached[idx] = new AtomicBoolean(false);
            rShow[idx] = new Runnable() {
                @Override public void run() {
                    tmpStage[idx] = new TestStage(modality);
                    stages.add(tmpStage[idx]);
                    assertFalse(tmpStage[idx].isShowing());
                    tmpStage[idx].showAndWait();
                    stageShowReturned[idx].set(true);
                    assertFalse(tmpStage[idx].isShowing());
                    assertTrue(hideEventReached[idx].get());
                    for (int j = 0; j < idx; j++) {
                        assertFalse(stageShowReturned[j].get());
                        assertTrue(hideEventReached[j].get());
                    }
                    for (int j = idx+1; j < N; j++) {
                        assertTrue(stageShowReturned[j].get());
                        assertTrue(hideEventReached[j].get());
                    }
                }
            };

            rHide[idx] = new Runnable() {
                @Override public void run() {
                    hideEventReached[idx].set(true);
                    assertFalse(stageShowReturned[idx].get());
                    for (int j = 0; j < idx; j++) {
                        assertFalse(stageShowReturned[j].get());
                        assertTrue(hideEventReached[j].get());
                    }
                    for (int j = idx+1; j < N; j++) {
                        assertFalse(stageShowReturned[j].get());
                        assertFalse(hideEventReached[j].get());
                    }
                    assertNotNull(tmpStage[idx]);
                    tmpStage[idx].hide();
                    Util.sleep(1);
                    assertFalse(stageShowReturned[idx].get());
                }
            };
        }

        final Runnable[] runnables = new Runnable[2*N];
        for (int i = 0; i < N; i++) {
            runnables[i] = rShow[i];
            runnables[N+i] = rHide[i];
        }
        Util.runAndWait(runnables);

        for (int i = 0; i < N; i++) {
            assertFalse(tmpStage[i].isShowing());
        }
    }

}
