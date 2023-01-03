/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;

import com.sun.javafx.application.PlatformImpl;
import java.util.concurrent.ExecutionException;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.junit.BeforeClass;
import org.w3c.dom.Document;

public class TestBase implements ChangeListener, InvalidationListener {
    private static final AtomicBoolean LOCK = new AtomicBoolean(false);
    private static final int INIT_TIMEOUT = 10000;
    private static final int LOAD_TIMEOUT = 60000;

    private static WebView view;

    @BeforeClass
    public static void setupOnce() {
        final CountDownLatch startupLatch = new CountDownLatch(1);

        PlatformImpl.startup(() -> {
            startupLatch.countDown();
        });

        try {
            startupLatch.await();
        } catch (InterruptedException ex) {}
    }

    public TestBase() {
        Platform.runLater(() -> {
            view = new WebView();
            WebEngine web = view.getEngine();

            web.documentProperty().addListener((ChangeListener)TestBase.this);
            web.documentProperty().addListener((InvalidationListener)TestBase.this);
            web.titleProperty().addListener((ChangeListener)TestBase.this);
            web.titleProperty().addListener((InvalidationListener)TestBase.this);
            web.locationProperty().addListener((ChangeListener)TestBase.this);
            web.locationProperty().addListener((InvalidationListener)TestBase.this);

            Worker loadTask = web.getLoadWorker();
            loadTask.exceptionProperty().addListener((ChangeListener)TestBase.this);
            loadTask.exceptionProperty().addListener((InvalidationListener)TestBase.this);
            loadTask.messageProperty().addListener((ChangeListener)TestBase.this);
            loadTask.messageProperty().addListener((InvalidationListener)TestBase.this);
            loadTask.progressProperty().addListener((ChangeListener)TestBase.this);
            loadTask.progressProperty().addListener((InvalidationListener)TestBase.this);
            loadTask.runningProperty().addListener((ChangeListener)TestBase.this);
            loadTask.runningProperty().addListener((InvalidationListener)TestBase.this);
            loadTask.stateProperty().addListener((ChangeListener)TestBase.this);
            loadTask.stateProperty().addListener((InvalidationListener)TestBase.this);
            loadTask.titleProperty().addListener((ChangeListener)TestBase.this);
            loadTask.titleProperty().addListener((InvalidationListener)TestBase.this);
            loadTask.totalWorkProperty().addListener((ChangeListener)TestBase.this);
            loadTask.totalWorkProperty().addListener((InvalidationListener)TestBase.this);
            loadTask.valueProperty().addListener((ChangeListener)TestBase.this);
            loadTask.valueProperty().addListener((InvalidationListener)TestBase.this);
            loadTask.workDoneProperty().addListener((ChangeListener)TestBase.this);
            loadTask.workDoneProperty().addListener((InvalidationListener)TestBase.this);

            loadTask.runningProperty().addListener(new LoadFinishedListener());

            TestBase.this.notify(LOCK);
        });

        wait(LOCK, INIT_TIMEOUT);
    }

    /**
     * Loads content from a URL.
     * This method blocks until loading is finished.
     */
    protected void load(final String url) {
        Platform.runLater(() -> {
            getEngine().load(url);
        });
        waitLoadFinished();
    }

    /**
     * Reloads current page.
     * This method blocks until loading is finished.
     */
    protected void reload() {
        Platform.runLater(() -> {
            getEngine().reload();
        });
        waitLoadFinished();
    }

    /**
     * Loads content from a file.
     * This method blocks until loading is finished.
     */
    protected void load(File file) {
        load(file.toURI().toASCIIString());
    }

    /**
     * Loads content from a file, and returns the resulting document.
     * This method blocks until loading is finished.
     */
    protected Document getDocumentFor(String fileName) {
        load(new File(fileName));
        return getEngine().getDocument();
    }

    /**
     * Loads content of the specified type from a String.
     * This method does not return until loading is finished.
     */
    protected void loadContent(final String content, final String contentType) {
        Platform.runLater(() -> {
            getEngine().loadContent(content, contentType);
        });
        waitLoadFinished();
    }

    /**
     * Loads HTML content from a String.
     * This method does not return until loading is finished.
     */
    protected void loadContent(final String content) {
        loadContent(content, "text/html");
    }

    /**
     * Executes a job on FX thread, and waits until it is complete.
     */
    protected void submit(Runnable job) {
        final FutureTask<Void> future = new FutureTask<>(job, null);
        Platform.runLater(future);
        try {
            // block until job is complete
            future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            // rethrow any assertion errors as is
            if (cause instanceof AssertionError) {
                throw (AssertionError) e.getCause();
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            // any other exception should be considered a test error
            throw new AssertionError(cause);
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Executes a job on FX thread, waits until completion, and returns its result.
     */
    protected <T> T submit(Callable<T> job) {
        final FutureTask<T> future = new FutureTask<>(job);
        Platform.runLater(future);
        try {
            return future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            // rethrow any assertion errors as is
            if (cause instanceof AssertionError) {
                throw (AssertionError) e.getCause();
            }
            // any other exception should be considered a test error
            throw new AssertionError(cause);
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Executes a script.
     * This method does not return until execution is complete.
     */
    protected Object executeScript(final String script) {
        return submit(() -> getEngine().executeScript(script));
    }

    private class LoadFinishedListener implements ChangeListener<Boolean> {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable,
                Boolean oldValue, Boolean newValue) {
            if (! newValue) {
                TestBase.this.notify(LOCK);
            }
        }
    }

    private void wait(AtomicBoolean condition, long timeout) {
        synchronized (condition) {
            long startTime = System.currentTimeMillis();
            while (!condition.get()) {
                try {
                    condition.wait(timeout);
                } catch (InterruptedException e) {
                } finally {
                    if (System.currentTimeMillis() - startTime >= timeout) {
                        throw new AssertionError("Waiting timed out");
                    }
                }
            }
            condition.set(false);
        }
    }

    private void notify(AtomicBoolean condition) {
        synchronized (condition) {
            condition.set(true);
            condition.notifyAll();
        }
    }

    /**
     * Override this to get loading notifications from both WebEngine
     * and its loadWorker.
     */
    @Override public void invalidated(Observable value) {
    }

    /**
     * Override this to get loading notifications from both WebEngine
     * and its loadWorker.
     */
    @Override public void changed(ObservableValue value, Object oldValue, Object newValue) {
    }

    /**
     * Returns the WebEngine object under test.
     */
    protected WebEngine getEngine() {
        return view.getEngine();
    }

    /**
     * Returns the WebView object under test.
     */
    protected WebView getView() {
        return view;
    }

    /**
     * Allows to override default load timeout value (in milliseconds).
     */
    protected int getLoadTimeOut() {
        return LOAD_TIMEOUT;
    }

    public void waitLoadFinished() {
        wait(LOCK, getLoadTimeOut());
    }

    /**
     * Check for Jigsaw Mode
     */
    public boolean isJigsawMode() {
        Class clazz = null;
        try {
            clazz = Class.forName("java.lang.reflect.ModuleDescriptor", false, TestBase.class.getClassLoader());
        } catch (Exception e) { }

        return clazz != null;
    }

    // Color comparison algorithm is based on WebKit's Tools/ImageDiff/PlaformImage.cpp#PlatformImage::difference implemenation.
    // https://trac.webkit.org/browser/webkit/trunk/Tools/ImageDiff/PlatformImage.cpp
    protected static float getColorDifference(final Color base, final Color c) {
        final float red = (c.getRed() - base.getRed()) / Math.max(255.0f - base.getRed(), base.getRed());
        final float green = (c.getGreen() - base.getGreen()) / Math.max(255.0f - base.getGreen(), base.getGreen());
        final float blue = (c.getBlue() - base.getBlue()) / Math.max(255.0f - base.getBlue(), base.getBlue());
        final float alpha = (c.getAlpha() - base.getAlpha()) / Math.max(255.0f - base.getAlpha(), base.getAlpha());
        final float distance = ((float) Math.sqrt(red * red + green * green + blue * blue + alpha * alpha)) / 2.0f;
        return distance >= (1 / 255.0f) ? distance * 100.0f : 0;
    }

    protected static boolean isColorsSimilar(final Color base, final Color c, float toleranceInPercentage) {
        return toleranceInPercentage >= getColorDifference(base, c);
    }
}
