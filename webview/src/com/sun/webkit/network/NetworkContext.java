/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.webkit.WebPage;

final class NetworkContext {

    private static final Logger logger =
            Logger.getLogger(NetworkContext.class.getName());

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
     * The buffer size for the shared pool of byte buffers.
     */
    private static final int BYTE_BUFFER_SIZE = 1024 * 40;

    /**
     * The thread pool used to execute asynchronous loaders.
     */
    private static final ThreadPoolExecutor threadPool;
    static {
        threadPool = new ThreadPoolExecutor(
                THREAD_POOL_SIZE,
                THREAD_POOL_SIZE,
                THREAD_POOL_KEEP_ALIVE_TIME,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new URLLoaderThreadFactory());
        threadPool.allowCoreThreadTimeOut(true);
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
    private static URLLoader fwkLoad(WebPage webPage,
                                     boolean asynchronous,
                                     String url,
                                     String method,
                                     String headers,
                                     FormDataElement[] formDataElements,
                                     long data)
    {
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, String.format(
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
                logger.log(Level.FINEST,
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
        int propValue = AccessController.doPrivileged(
                new PrivilegedAction<Integer>() {
                    @Override
                    public Integer run() {
                        return Integer.getInteger("http.maxConnections", -1);
                    }
                });
        return propValue >= 0 ? propValue : DEFAULT_HTTP_MAX_CONNECTIONS;
    }
    
    /**
     * Thread factory for URL loader threads.
     */
    private static final class URLLoaderThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger index = new AtomicInteger(1);

        private URLLoaderThreadFactory() {
            SecurityManager sm = System.getSecurityManager();
            group = (sm != null) ? sm.getThreadGroup()
                    : Thread.currentThread().getThreadGroup();
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    "URL-Loader-" + index.getAndIncrement());
            t.setDaemon(true);
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}
