/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.stage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static test.util.Util.TIMEOUT;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.print.PrinterJob;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageShim;
import javafx.stage.Window;
import javafx.util.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import test.util.Util;

/**
 * Test program for showAndWait functionality.
 */
public final class ShowAndWaitTest {

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

    @BeforeAll
    public static void setupOnce() {
        Util.launch(launchLatch, MyApp.class);
    }

    @AfterAll
    public static void teardownOnce() {
        Util.shutdown();
    }

    // Set of stages being tested so that they can be hidden at the
    // end of of a failing test
    private HashSet<Stage> stages = new HashSet<Stage>();

    // Secondary stages used for testing
    private Stage tmpStage1 = null;
    private Stage tmpStage2 = null;

    // Modality of the secondary stage(s) for a particular tests
    private static Collection<Modality> parameters() {
        return List.of(
            Modality.NONE,
            Modality.WINDOW_MODAL,
            Modality.APPLICATION_MODAL
        );
    }

    @BeforeEach
    public void setupEach() {
        assertNotNull(myApp);
        assertNotNull(myApp.primaryStage);
    }

    @AfterEach
    public void teardownEach() {
        for (final Stage stage : stages) {
            if (stage.isShowing()) {
                System.err.println("Cleaning up stage after a failed test...");
                try {
                    Util.runAndWait(stage::hide);
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
    public void ensureTest1(Modality modality) {
        if (!test1Run) {
            test1(modality);
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void test1(Modality modality) {
        if (test1Run) {
            return;
        }
        test1Run = true;

        assertEquals(0, launchLatch.getCount());
        Util.runAndWait(() -> {
            assertTrue(Platform.isFxApplicationThread());
            assertTrue(StageShim.isPrimary(myApp.primaryStage));
            assertFalse(myApp.primaryStage.isShowing());

            // Verify that we cannot call showAndWait on the primaryStage
            assertThrows(IllegalStateException.class, () -> {
                myApp.primaryStage.showAndWait();
            });

            myApp.primaryStage.show();
        });
    }

    // Verify that we cannot construct a stage on a thread other than
    // the FX Application thread
    @ParameterizedTest
    @MethodSource("parameters")
    public void testConstructWrongThread(Modality modality) {
        assertThrows(IllegalStateException.class, () -> {
            ensureTest1(modality);
            assertFalse(Platform.isFxApplicationThread());

            // The following should throw IllegalStateException
            tmpStage1 = new TestStage(modality);
            stages.add(tmpStage1);
        });
    }


    // Verify that we cannot call showAndWait on a thread other than
    // the FX Application thread
    @ParameterizedTest
    @MethodSource("parameters")
    public void testShowWaitWrongThread(Modality modality) {
        assertThrows(IllegalStateException.class, () -> {
            ensureTest1(modality);
            assertFalse(Platform.isFxApplicationThread());
            Util.runAndWait(() -> {
                tmpStage1 = new TestStage(modality);
                stages.add(tmpStage1);
                assertFalse(StageShim.isPrimary(tmpStage1));
                assertFalse(tmpStage1.isShowing());
            });
            assertNotNull(tmpStage1);

            // The following should throw IllegalStateException
            tmpStage1.showAndWait();
        });
    }

    // Verify that we cannot call showAndWait on a visible stage
    @ParameterizedTest
    @MethodSource("parameters")
    public void testVisibleThrow(Modality modality) {
        assertThrows(IllegalStateException.class, () -> {
            ensureTest1(modality);
            Util.runAndWait(() -> {
                tmpStage1 = new TestStage(modality);
                stages.add(tmpStage1);
                assertFalse(StageShim.isPrimary(tmpStage1));
                assertFalse(tmpStage1.isShowing());
                tmpStage1.show();
                assertTrue(tmpStage1.isShowing());

                try {
                    // The following should throw IllegalStateException
                    tmpStage1.showAndWait();
                } finally {
                    tmpStage1.hide();
                }
            });
        });
    }

    // Verify that show returns right away; hide the stage after 500 msec
    @ParameterizedTest
    @MethodSource("parameters")
    public void testNotBlocking(Modality modality) {
        ensureTest1(modality);

        final AtomicBoolean stageShowReturned = new AtomicBoolean(false);
        final AtomicBoolean hideActionReached = new AtomicBoolean(false);

        Runnable rShow = () -> {
            tmpStage1 = new TestStage(modality);
            stages.add(tmpStage1);
            assertFalse(StageShim.isPrimary(tmpStage1));
            assertFalse(tmpStage1.isShowing());
            tmpStage1.show();
            stageShowReturned.set(true);
            assertTrue(tmpStage1.isShowing());
            assertFalse(hideActionReached.get());
        };

        Runnable rHide = () -> {
            assertNotNull(tmpStage1);
            assertTrue(tmpStage1.isShowing());
            assertTrue(stageShowReturned.get());
            hideActionReached.set(true);
            tmpStage1.hide();
        };

        Util.runAndWait(rShow, rHide);

        assertFalse(tmpStage1.isShowing());
    }

    // Verify that showAndWait blocks until the stage is hidden.
    // Verify that the nested event loop exits immediately after
    // the event handler that calls hide returns, before running
    // the next Runnable.
    @ParameterizedTest
    @MethodSource("parameters")
    public void testSingle(Modality modality) {
        ensureTest1(modality);

        final AtomicBoolean stage1ShowReturned = new AtomicBoolean(false);
        final AtomicBoolean hide1EventReached = new AtomicBoolean(false);
        final AtomicBoolean nextRunnableReached = new AtomicBoolean(false);

        Runnable rShow1 = () -> {
            tmpStage1 = new TestStage(modality);
            stages.add(tmpStage1);
            assertFalse(StageShim.isPrimary(tmpStage1));
            assertFalse(tmpStage1.isShowing());
            tmpStage1.showAndWait();
            stage1ShowReturned.set(true);
            assertFalse(tmpStage1.isShowing());
            assertTrue(hide1EventReached.get());
            assertFalse(nextRunnableReached.get());
        };

        Runnable rHide1 = () -> {
            hide1EventReached.set(true);
            assertFalse(stage1ShowReturned.get());
            assertNotNull(tmpStage1);
            tmpStage1.hide();
            Util.sleep(1);
            assertFalse(stage1ShowReturned.get());
        };

        Runnable rNext = () -> {
            // This should happen after the nested event loop exits
            nextRunnableReached.set(true);
        };

        Util.runAndWait(rShow1, rHide1, rNext);

        assertFalse(tmpStage1.isShowing());
    }

    // Verify that showAndWait blocks until the stage is hidden.
    // Verify that the nested event loop exits immediately after
    // the event handler that calls hide returns, before running
    // the next Runnable (called from rShow1 after showAndWait returns).

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSingle_Chained(Modality modality) {
        ensureTest1(modality);

        final AtomicBoolean stage1ShowReturned = new AtomicBoolean(false);
        final AtomicBoolean hide1EventReached = new AtomicBoolean(false);
        final AtomicBoolean nextRunnableReached = new AtomicBoolean(false);

        Runnable rShow1 = () -> {
            tmpStage1 = new TestStage(modality);
            stages.add(tmpStage1);
            assertFalse(StageShim.isPrimary(tmpStage1));
            assertFalse(tmpStage1.isShowing());
            tmpStage1.showAndWait();
            stage1ShowReturned.set(true);
            assertFalse(tmpStage1.isShowing());
            assertTrue(hide1EventReached.get());
            assertFalse(nextRunnableReached.get());
        };

        Runnable rHide1 = () -> {
            hide1EventReached.set(true);
            assertFalse(stage1ShowReturned.get());
            assertNotNull(tmpStage1);
            tmpStage1.hide();
            Util.sleep(1);
            assertFalse(stage1ShowReturned.get());
            Platform.runLater(() -> {
                // This should happen after the nested event loop exits
                nextRunnableReached.set(true);
            });
        };

        Util.runAndWait(rShow1, rHide1);

        assertFalse(tmpStage1.isShowing());
    }

    // Verify two nested event loops, with the stages being hidden in the
    // reverse order that they are shown
    @ParameterizedTest
    @MethodSource("parameters")
    public void testTwoNested(Modality modality) {
        ensureTest1(modality);

        final AtomicBoolean stage1ShowReturned = new AtomicBoolean(false);
        final AtomicBoolean hide1EventReached = new AtomicBoolean(false);
        final AtomicBoolean stage2ShowReturned = new AtomicBoolean(false);
        final AtomicBoolean hide2EventReached = new AtomicBoolean(false);

        Runnable rShow1 = () -> {
            tmpStage1 = new TestStage(modality);
            stages.add(tmpStage1);
            assertFalse(StageShim.isPrimary(tmpStage1));
            assertFalse(tmpStage1.isShowing());
            tmpStage1.showAndWait();
            stage1ShowReturned.set(true);
            assertFalse(tmpStage1.isShowing());
            assertTrue(stage2ShowReturned.get());
            assertTrue(hide1EventReached.get());
            assertTrue(hide2EventReached.get());
        };

        Runnable rShow2 = () -> {
            tmpStage2 = new TestStage(modality);
            stages.add(tmpStage2);
            assertFalse(StageShim.isPrimary(tmpStage2));
            assertFalse(tmpStage2.isShowing());
            tmpStage2.showAndWait();
            stage2ShowReturned.set(true);
            assertFalse(stage1ShowReturned.get());
            assertFalse(tmpStage2.isShowing());
            assertTrue(hide2EventReached.get());
            assertFalse(hide1EventReached.get());
        };

        Runnable rHide1 = () -> {
            hide1EventReached.set(true);
            assertFalse(stage1ShowReturned.get());
            assertTrue(stage2ShowReturned.get());
            assertTrue(hide2EventReached.get());
            assertNotNull(tmpStage1);
            tmpStage1.hide();
            Util.sleep(1);
            assertFalse(stage1ShowReturned.get());
        };

        Runnable rHide2 = () -> {
            hide2EventReached.set(true);
            assertFalse(stage2ShowReturned.get());
            assertFalse(stage1ShowReturned.get());
            assertFalse(hide1EventReached.get());
            assertNotNull(tmpStage2);
            tmpStage2.hide();
            Util.sleep(1);
            assertFalse(stage2ShowReturned.get());
        };

        Util.runAndWait(rShow1, rShow2, rHide2, rHide1);

        assertFalse(tmpStage1.isShowing());
        assertFalse(tmpStage2.isShowing());
    }

    // Verify two nested event loops, with the stages being hidden in the
    // same order that they are shown
    @ParameterizedTest
    @MethodSource("parameters")
    public void testTwoInterleaved(Modality modality) {
        ensureTest1(modality);

        final AtomicBoolean stage1ShowReturned = new AtomicBoolean(false);
        final AtomicBoolean hide1EventReached = new AtomicBoolean(false);
        final AtomicBoolean stage2ShowReturned = new AtomicBoolean(false);
        final AtomicBoolean hide2EventReached = new AtomicBoolean(false);

        Runnable rShow1 = () -> {
            tmpStage1 = new TestStage(modality);
            stages.add(tmpStage1);
            assertFalse(StageShim.isPrimary(tmpStage1));
            assertFalse(tmpStage1.isShowing());
            tmpStage1.showAndWait();
            stage1ShowReturned.set(true);
            assertFalse(tmpStage1.isShowing());
            assertTrue(stage2ShowReturned.get());
            assertTrue(hide1EventReached.get());
            assertTrue(hide2EventReached.get());
        };

        Runnable rShow2 = () -> {
            tmpStage2 = new TestStage(modality);
            stages.add(tmpStage2);
            assertFalse(StageShim.isPrimary(tmpStage2));
            assertFalse(tmpStage2.isShowing());
            tmpStage2.showAndWait();
            stage2ShowReturned.set(true);
            assertFalse(tmpStage2.isShowing());
            assertFalse(stage1ShowReturned.get());
            assertTrue(hide2EventReached.get());
            assertTrue(hide1EventReached.get());
        };

        Runnable rHide1 = () -> {
            hide1EventReached.set(true);
            assertFalse(stage1ShowReturned.get());
            assertFalse(stage2ShowReturned.get());
            assertFalse(hide2EventReached.get());
            assertNotNull(tmpStage1);
            tmpStage1.hide();
            Util.sleep(1);
            assertFalse(stage1ShowReturned.get());
        };

        Runnable rHide2 = () -> {
            hide2EventReached.set(true);
            assertFalse(stage2ShowReturned.get());
            assertFalse(stage1ShowReturned.get());
            assertTrue(hide1EventReached.get());
            assertNotNull(tmpStage2);
            tmpStage2.hide();
            Util.sleep(1);
            assertFalse(stage2ShowReturned.get());
        };

        Util.runAndWait(rShow1, rShow2, rHide1, rHide2);

        assertFalse(tmpStage1.isShowing());
        assertFalse(tmpStage2.isShowing());
    }

    // Verify multiple nested event loops, with the stages being hidden in the
    // reverse order that they are shown
    @ParameterizedTest
    @MethodSource("parameters")
    public void testMultipleNested(Modality modality) {
        ensureTest1(modality);

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
            rShow[idx] = () -> {
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
            };

            rHide[idx] = () -> {
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
    @ParameterizedTest
    @MethodSource("parameters")
    public void testMultipleInterleaved(Modality modality) {
        ensureTest1(modality);

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
            rShow[idx] = () -> {
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
            };

            rHide[idx] = () -> {
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

    // Verify that Stage.showAndWait throws an exception if called from an
    // animation timeline.
    @ParameterizedTest
    @MethodSource("parameters")
    public void testTimeline(Modality modality) throws Throwable {
        ensureTest1(modality);

        final CountDownLatch animationDone = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>(null);

        KeyFrame kf = new KeyFrame(Duration.millis(200), e -> {
            try {
                tmpStage1 = new TestStage(modality);
                stages.add(tmpStage1);
                assertFalse(StageShim.isPrimary(tmpStage1));
                assertFalse(tmpStage1.isShowing());
                try {
                    tmpStage1.showAndWait();
                    fail("Did not get expected exception from showAndWait");
                } catch (IllegalStateException ex) {
                    // Good
                }
                assertFalse(tmpStage1.isShowing());
            } catch (Throwable t) {
                error.set(t);
            }
            animationDone.countDown();
        });
        Timeline timeline = new Timeline(kf);
        Platform.runLater(timeline::play);

        try {
            if (!animationDone.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                fail("Timeout waiting for animation");
            }
        } catch (InterruptedException ex) {
            fail("Unexpected exception: " + ex);
        }

        final Throwable t = error.get();
        if (t != null) {
            throw t;
        }

        assertFalse(tmpStage1.isShowing());
    }

    // Verify that Alert.showAndWait throws an exception if called from an
    // animation timeline.
    @ParameterizedTest
    @MethodSource("parameters")
    public void testTimelineDialog(Modality modality) throws Throwable {
        ensureTest1(modality);

        final CountDownLatch animationDone = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>(null);

        KeyFrame kf = new KeyFrame(Duration.millis(200), e -> {
            Alert alert = null;
            try {
                alert = new Alert(Alert.AlertType.INFORMATION);
                assertFalse(alert.isShowing());
                try {
                    alert.showAndWait();
                    fail("Did not get expected exception from showAndWait");
                } catch (IllegalStateException ex) {
                    // Good
                }
                assertFalse(alert.isShowing());
            } catch (Throwable t) {
                error.set(t);
                try {
                    if (alert.isShowing()) {
                        alert.close();
                    }
                } catch (RuntimeException ex) {}
            }
            animationDone.countDown();
        });
        Timeline timeline = new Timeline(kf);
        Platform.runLater(timeline::play);

        try {
            if (!animationDone.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                fail("Timeout waiting for animation");
            }
        } catch (InterruptedException ex) {
            fail("Unexpected exception: " + ex);
        }

        final Throwable t = error.get();
        if (t != null) {
            throw t;
        }
    }

    // Verify that printing throws an exception if called from an
    // animation timeline.
    @ParameterizedTest
    @MethodSource("parameters")
    public void testTimelinePrint(Modality modality) throws Throwable {
        Assumptions.assumeTrue(PrinterJob.createPrinterJob() != null);

        ensureTest1(modality);

        final CountDownLatch animationDone = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>(null);

        KeyFrame kf = new KeyFrame(Duration.millis(200), e -> {
            try {
                PrinterJob job = PrinterJob.createPrinterJob();
                try {
                    job.showPrintDialog(myApp.primaryStage);
                    fail("Did not get expected exception from showPrintDialog");
                } catch (IllegalStateException ex) {
                    // Good
                }
                try {
                    job.showPageSetupDialog(myApp.primaryStage);
                    fail("Did not get expected exception from showPageSetupDialog");
                } catch (IllegalStateException ex) {
                    // Good
                }
                try {
                    Rectangle rect = new Rectangle(200, 100, Color.GREEN);
                    job.printPage(rect);
                    fail("Did not get expected exception from printPage");
                } catch (IllegalStateException ex) {
                    // Good
                }
            } catch (Throwable t) {
                error.set(t);
            }
            animationDone.countDown();
        });
        Timeline timeline = new Timeline(kf);
        Platform.runLater(timeline::play);

        try {
            if (!animationDone.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                fail("Timeout waiting for animation");
            }
        } catch (InterruptedException ex) {
            fail("Unexpected exception: " + ex);
        }

        final Throwable t = error.get();
        if (t != null) {
            throw t;
        }
    }
}
