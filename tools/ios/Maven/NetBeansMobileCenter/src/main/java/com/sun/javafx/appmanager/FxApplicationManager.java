/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.appmanager;

import com.sun.javafx.stage.WindowManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public final class FxApplicationManager {
    private static FxApplicationManager instance;

    public static synchronized FxApplicationManager getInstance() {
        if (instance == null) {
            initializePlatform();
            instance = new FxApplicationManager();
        }

        return instance;
    }

    public FxApplicationInstance start(
            final ClassLoader appClassLoader,
            final String appClass) throws Exception {
        final Application application =
                createFxApplication(appClassLoader, appClass);
        application.init();

        final StartAction startAction =
                new StartAction(appClassLoader, application);
        Platform.runLater(startAction);

        return startAction.getResult();
    }

    private static void initializePlatform() {
        final ThreadGroup fxPlatformThreadGroup =
                new ThreadGroup(getTopLevelThreadGroup(), "FX Platform");
        new Thread(fxPlatformThreadGroup, "FX Platform") {
            @Override
            public void run() {
                Application.launch(BootstrapApplication.class);
            }
        }.start();
        try {
            BootstrapApplication.waitForStart();
        } catch (final InterruptedException e) {
            // ignore
        }
    }

    private static Application createFxApplication(
            final ClassLoader appClassLoader,
            final String appClassName) throws ClassNotFoundException,
                                              InstantiationException,
                                              IllegalAccessException {
        final Class<?> appClass = appClassLoader.loadClass(appClassName);
        if (!Application.class.isAssignableFrom(appClass)) {
            throw new ClassNotFoundException("FX application class not found");
        }

        return ((Class<Application>) appClass).newInstance();
    }

    private static ThreadGroup getTopLevelThreadGroup() {
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        while (threadGroup.getParent() != null) {
            threadGroup = threadGroup.getParent();
        }

        return threadGroup;
    }

    private static final class AppInstanceImpl
            implements FxApplicationInstance {
        private final ClassLoader appClassLoader;
        private final Application application;

        public AppInstanceImpl(final ClassLoader appClassLoader,
                               final Application application) {
            this.appClassLoader = appClassLoader;
            this.application = application;
        }

        @Override
        public void stop() {
            final StopAction stopAction = new StopAction(appClassLoader,
                                                         application);

            Platform.runLater(stopAction);
            try {
                stopAction.waitForCompletion();
            } catch (final InterruptedException e) {
                // ignore
            }
        }
    }

    private static final class StartAction implements Runnable {
        private final ClassLoader appClassLoader;
        private final Application application;

        private Exception exception;
        private FxApplicationInstance result;

        public StartAction(final ClassLoader appClassLoader,
                           final Application application) {
            this.appClassLoader = appClassLoader;
            this.application = application;
        }

        @Override
        public void run() {
            final Thread currentThread = Thread.currentThread();
            final ClassLoader oldContextClassLoader =
                    currentThread.getContextClassLoader();

            currentThread.setContextClassLoader(appClassLoader);
            try {
                try {
                    final Stage appPrimaryStage = new Stage();
                    appPrimaryStage.impl_setPrimary(true);
                    application.start(appPrimaryStage);
                } finally {
                    currentThread.setContextClassLoader(oldContextClassLoader);
                }
            } catch (final Exception e) {
                synchronized (this) {
                    exception = e;
                    notifyAll();
                }
                return;
            }

            synchronized (this) {
                result = new AppInstanceImpl(appClassLoader, application);
                notifyAll();
            }
        }

        public synchronized FxApplicationInstance getResult() throws Exception {
            while (result == null) {
                if (exception != null) {
                    throw exception;
                }

                wait();
            }

            return result;
        }
    }

    private static final class StopAction implements Runnable {
        private final ClassLoader appClassLoader;
        private final Application application;

        private boolean finished;

        public StopAction(final ClassLoader appClassLoader,
                          final Application application) {
            this.appClassLoader = appClassLoader;
            this.application = application;
        }

        @Override
        public void run() {
            try {
                WindowManager.closeApplicationWindows(appClassLoader);
                try {
                    application.stop();
                } catch (final Exception e) {
                }
            } finally {
                synchronized (this) {
                    finished = true;
                    notifyAll();
                }
            }
        }

        public synchronized void waitForCompletion()
                throws InterruptedException {
            while (!finished) {
                wait();
            }
        }
    }
}
