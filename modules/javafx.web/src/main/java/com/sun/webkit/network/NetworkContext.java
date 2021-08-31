/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.network;

import static com.sun.webkit.network.URLs.newURL;

import java.net.MalformedURLException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;
import com.sun.webkit.WebPage;
import java.security.Permission;

final class NetworkContext {

    private static final PlatformLogger logger =
            PlatformLogger.getLogger(NetworkContext.class.getName());

    /**
     * The size of the thread pool for asynchronous loaders.
     */
    private static final int THREAD_POOL_SIZE = 20;

    /**
     * The thread pool keep alive time.
     */
    private static final long THREAD_POOL_KEEP_ALIVE_TIME = 10000L;

    /**
     * The default value of the "http.maxConnections" system property.
     */
    private static final int DEFAULT_HTTP_MAX_CONNECTIONS = 5;

    /**
     * The default value of the maximum concurrent connections for
     * new gen HTTP2 client
     */
    private static final int DEFAULT_HTTP2_MAX_CONNECTIONS = 20;

    /**
     * The buffer size for the shared pool of byte buffers.
     */
    private static final int BYTE_BUFFER_SIZE = 1024 * 40;

    /**
     * The thread pool used to execute asynchronous loaders.
     */
    private static final ThreadPoolExecutor threadPool;

    /**
     * Can use HTTP2Loader
     */
    private static final boolean useHTTP2Loader;
    static {
        threadPool = new ThreadPoolExecutor(
                THREAD_POOL_SIZE,
                THREAD_POOL_SIZE,
                THREAD_POOL_KEEP_ALIVE_TIME,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new URLLoaderThreadFactory());
        threadPool.allowCoreThreadTimeOut(true);

        @SuppressWarnings("removal")
        boolean tmp = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
            // Use HTTP2 by default on JDK 12 or later
            final var version = Runtime.Version.parse(System.getProperty("java.version"));
            final String defaultUseHTTP2 = version.feature() >= 12 ? "true" : "false";
            return Boolean.valueOf(System.getProperty("com.sun.webkit.useHTTP2Loader", defaultUseHTTP2));
        });
        useHTTP2Loader = tmp;
    }

    /**
     * The shared pool of byte buffers.
     */
    private static final ByteBufferPool byteBufferPool =
            ByteBufferPool.newInstance(BYTE_BUFFER_SIZE);


    /**
     * Non-invocable constructor.
     */
    private NetworkContext() {
        throw new AssertionError();
    }


    /**
     * Checks whether a URL is valid or not. I.E. if we do have a protocol
     * handler to deal with it.
     *
     * @param url the <code>String</code> containing the url to check.
     * @return <code>true</code> if we can handle the url. <code>false</code>
     *         otherwise.
     */
    private static boolean canHandleURL(String url) {
        java.net.URL u = null;
        try {
            u = newURL(url);
        } catch (MalformedURLException malformedURLException) {
        }
        return u != null;
    }

    /**
     * Starts an asynchronous load or executes a synchronous one.
     */
    private static URLLoaderBase fwkLoad(WebPage webPage,
                                     boolean asynchronous,
                                     String url,
                                     String method,
                                     String headers,
                                     FormDataElement[] formDataElements,
                                     long data)
    {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(String.format(
                    "webPage: [%s], " +
                    "asynchronous: [%s], " +
                    "url: [%s], " +
                    "method: [%s], " +
                    "formDataElements: %s, " +
                    "data: [0x%016X], " +
                    "headers:%n%s",
                    webPage,
                    asynchronous,
                    url,
                    method,
                    formDataElements != null
                            ? Arrays.asList(formDataElements) : "[null]",
                    data,
                    Util.formatHeaders(headers)));
        }

        if (useHTTP2Loader) {
            final URLLoaderBase loader = HTTP2Loader.create(
                webPage,
                byteBufferPool,
                asynchronous,
                url,
                method,
                headers,
                formDataElements,
                data);
            if (loader != null) {
                return loader;
            }
        }

        URLLoader loader = new URLLoader(
                webPage,
                byteBufferPool,
                asynchronous,
                url,
                method,
                headers,
                formDataElements,
                data);
        if (asynchronous) {
            threadPool.submit(loader);
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest(
                        "active count: [{0}], " +
                        "pool size: [{1}], " +
                        "max pool size: [{2}], " +
                        "task count: [{3}], " +
                        "completed task count: [{4}]",
                        new Object[] {
                                threadPool.getActiveCount(),
                                threadPool.getPoolSize(),
                                threadPool.getMaximumPoolSize(),
                                threadPool.getTaskCount(),
                                threadPool.getCompletedTaskCount()});
            }
            return loader;
        } else {
            loader.run();
            return null;
        }
    }

    /**
     * Returns the maximum allowed number of connections per host.
     */
    private static int fwkGetMaximumHTTPConnectionCountPerHost() {
        // Our implementation employs HttpURLConnection for all
        // HTTP exchanges, so return the value of the "http.maxConnections"
        // system property.
        @SuppressWarnings("removal")
        int propValue = AccessController.doPrivileged(
                (PrivilegedAction<Integer>) () -> Integer.getInteger("http.maxConnections", -1));

        if (useHTTP2Loader) {
            return propValue >= 0 ? propValue : DEFAULT_HTTP2_MAX_CONNECTIONS;
        }
        return propValue >= 0 ? propValue : DEFAULT_HTTP_MAX_CONNECTIONS;
    }

    /**
     * Thread factory for URL loader threads.
     */
    private static final class URLLoaderThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger index = new AtomicInteger(1);

        // Need to assert the modifyThread and modifyThreadGroup permission when
        // creating the thread from the URLLoaderThreadFactory, so we can
        // create the thread with the desired ThreadGroup.
        // Note that this is needed when running as an applet or a web start app.
        private static final Permission modifyThreadGroupPerm = new RuntimePermission("modifyThreadGroup");
        private static final Permission modifyThreadPerm = new RuntimePermission("modifyThread");

        private URLLoaderThreadFactory() {
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            group = (sm != null) ? sm.getThreadGroup()
                    : Thread.currentThread().getThreadGroup();
        }

        @SuppressWarnings("removal")
        @Override
        public Thread newThread(Runnable r) {
            // Assert the modifyThread and modifyThreadGroup permissions
            return
                AccessController.doPrivileged((PrivilegedAction<Thread>) () -> {
                    Thread t = new Thread(group, r,
                            "URL-Loader-" + index.getAndIncrement());
                    t.setDaemon(true);
                    if (t.getPriority() != Thread.NORM_PRIORITY) {
                        t.setPriority(Thread.NORM_PRIORITY);
                    }
                    return t;
                },
                null,
                modifyThreadGroupPerm, modifyThreadPerm);
        }
    }
}
