/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;
import com.sun.webkit.Invoker;
import com.sun.webkit.WebPage;
import java.io.IOException;
import java.io.InputStream;
import static java.lang.String.format;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;

final class SocketStreamHandle {
    private static final Pattern FIRST_LINE_PATTERN = Pattern.compile(
            "^HTTP/1.[01]\\s+(\\d{3})(?:\\s.*)?$");
    private static final PlatformLogger logger = PlatformLogger.getLogger(
            SocketStreamHandle.class.getName());
    private static final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
            0, Integer.MAX_VALUE,
            10, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(),
            new CustomThreadFactory());

    private enum State {ACTIVE, CLOSE_REQUESTED, DISPOSED}

    private final String host;
    private final int port;
    private final boolean ssl;
    private final WebPage webPage;
    private final long data;
    private volatile Socket socket;
    private volatile State state = State.ACTIVE;
    private volatile boolean connected;

    private SocketStreamHandle(String host, int port, boolean ssl,
                               WebPage webPage, long data)
    {
        this.host = host;
        this.port = port;
        this.ssl = ssl;
        this.webPage = webPage;
        this.data = data;
    }

    private static SocketStreamHandle fwkCreate(String host, int port,
                                                boolean ssl, WebPage webPage,
                                                long data)
    {
        final SocketStreamHandle ssh =
                new SocketStreamHandle(host, port, ssl, webPage, data);
        logger.finest("Starting {0}", ssh);
        threadPool.submit(() -> {
            ssh.run();
        });
        return ssh;
    }

    @SuppressWarnings("removal")
    private void run() {
        if (webPage == null) {
            logger.finest("{0} is not associated with any web "
                    + "page, aborted", this);
            // In theory we could pump this error through the doRun()'s
            // error handling code but in that case that error handling
            // code would have to run outside the doPrivileged block,
            // which is something we want to avoid.
            didFail(0, "Web socket is not associated with any web page");
            didClose();
            return;
        }
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            doRun();
            return null;
        }, webPage.getAccessControlContext());
    }

    private void doRun() {
        Throwable error = null;
        String errorDescription = null;
        try {
            logger.finest("{0} started", this);
            connect();
            connected = true;
            logger.finest("{0} connected", this);
            didOpen();
            InputStream is = socket.getInputStream();
            while (true) {
                byte[] buffer = new byte[8192];
                int n = is.read(buffer);
                if(n > 0) {
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.finest(format("%s received len: [%d], data:%s",
                                this, n, dump(buffer, n)));
                    }
                    didReceiveData(buffer, n);
                } else {
                    logger.finest("{0} connection closed by remote host", this);
                    break;
                }
            }
        } catch (UnknownHostException ex) {
            error = ex;
            errorDescription = "Unknown host";
        } catch (ConnectException ex) {
            error = ex;
            errorDescription = "Unable to connect";
        } catch (NoRouteToHostException ex) {
            error = ex;
            errorDescription = "No route to host";
        } catch (PortUnreachableException ex) {
            error = ex;
            errorDescription = "Port unreachable";
        } catch (SocketException ex) {
            if (state != State.ACTIVE) {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.finest(format("%s exception (most "
                            + "likely caused by local close)", this), ex);
                }
            } else {
                error = ex;
                errorDescription = "Socket error";
            }
        } catch (SSLException ex) {
            error = ex;
            errorDescription = "SSL error";
        } catch (IOException ex) {
            error = ex;
            errorDescription = "I/O error";
        } catch (SecurityException ex) {
            error = ex;
            errorDescription = "Security error";
        } catch (Throwable th) {
            error = th;
        }

        if (error != null) {
            if (errorDescription == null) {
                errorDescription = "Unknown error";
                logger.warning(format("%s unexpected error", this), error);
            } else {
                logger.finest(format("%s exception", this), error);
            }
            didFail(0, errorDescription);
        }

        try {
            socket.close();
        } catch (IOException ignore) {}
        didClose();

        logger.finest("{0} finished", this);
    }

    private void connect() throws IOException {
        @SuppressWarnings("removal")
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkConnect(host, port);
        }

        // The proxy trial logic here is meant to mimic
        // sun.net.www.protocol.http.HttpURLConnection.plainConnect
        boolean success = false;
        IOException lastException = null;
        boolean triedDirectConnection = false;
        @SuppressWarnings("removal")
        ProxySelector proxySelector = AccessController.doPrivileged(
                (PrivilegedAction<ProxySelector>) () -> ProxySelector.getDefault());
        if (proxySelector != null) {
            URI uri;
            try {
                uri = new URI((ssl ? "https" : "http") + "://" + host);
            } catch (URISyntaxException ex) {
                throw new IOException(ex);
            }
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest(format("%s selecting proxies for: [%s]", this, uri));
            }
            List<Proxy> proxies = proxySelector.select(uri);
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest(format("%s selected proxies: %s", this, proxies));
            }
            for (Proxy proxy : proxies) {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.finest(format("%s trying proxy: [%s]", this, proxy));
                }
                if (proxy.type() == Proxy.Type.DIRECT) {
                    triedDirectConnection = true;
                }
                try {
                    connect(proxy);
                    success = true;
                    break;
                } catch (IOException ex) {
                    logger.finest(format("%s exception", this), ex);
                    lastException = ex;
                    if (proxy.address() != null) {
                        proxySelector.connectFailed(uri, proxy.address(), ex);
                    }
                    continue;
                }
            }
        }
        if (!success && !triedDirectConnection) {
            logger.finest("{0} trying direct connection", this);
            connect(Proxy.NO_PROXY);
            success = true;
        }
        if (!success) {
            throw lastException;
        }
    }

    private void connect(Proxy proxy) throws IOException {
        synchronized (this) {
            if (state != State.ACTIVE) {
                throw new SocketException("Close requested");
            }
            socket = new Socket(proxy);
        }
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(format("%s connecting to: [%s:%d]",
                    this, host, port));
        }
        socket.connect(new InetSocketAddress(host, port));
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(format("%s connected to: [%s:%d]",
                    this, host, port));
        }
        if (ssl) {
            synchronized (this) {
                if (state != State.ACTIVE) {
                    throw new SocketException("Close requested");
                }
                logger.finest("{0} starting SSL handshake", this);
                socket = HttpsURLConnection.getDefaultSSLSocketFactory()
                        .createSocket(socket, host, port, true);
            }
            ((SSLSocket) socket).startHandshake();
        }
    }

    private int fwkSend(byte[] buffer) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(format("%s sending len: [%d], data:%s",
                    this, buffer.length, dump(buffer, buffer.length)));
        }
        if (connected) {
            try {
                socket.getOutputStream().write(buffer);
                return buffer.length;
            } catch (IOException ex) {
                logger.finest(format("%s exception", this), ex);
                didFail(0, "I/O error");
                return 0;
            }
        } else {
            logger.finest("{0} not connected", this);
            didFail(0, "Not connected");
            return 0;
        }
    }

    private void fwkClose() {
        synchronized (this) {
            logger.finest("{0}", this);
            state = State.CLOSE_REQUESTED;
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException ignore) {}
        }
    }

    private void fwkNotifyDisposed() {
        logger.finest("{0}", this);
        state = State.DISPOSED;
    }

    private void didOpen() {
        Invoker.getInvoker().postOnEventThread(() -> {
            if (state == State.ACTIVE) {
                notifyDidOpen();
            }
        });
    }

    private void didReceiveData(final byte[] buffer, final int len) {
        Invoker.getInvoker().postOnEventThread(() -> {
            if (state == State.ACTIVE) {
                notifyDidReceiveData(buffer, len);
            }
        });
    }

    private void didFail(final int errorCode, final String errorDescription) {
        Invoker.getInvoker().postOnEventThread(() -> {
            if (state == State.ACTIVE) {
                notifyDidFail(errorCode, errorDescription);
            }
        });
    }

    private void didClose() {
        Invoker.getInvoker().postOnEventThread(() -> {
            if (state != State.DISPOSED) {
                notifyDidClose();
            }
        });
    }

    private void notifyDidOpen() {
        logger.finest("{0}", this);
        twkDidOpen(data);
    }

    private void notifyDidReceiveData(byte[] buffer, int len) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(format("%s, len: [%d], data:%s",
                    this, len, dump(buffer, len)));
        }
        twkDidReceiveData(buffer, len, data);
    }

    private void notifyDidFail(int errorCode, String errorDescription) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(format("%s, errorCode: %d, "
                    + "errorDescription: %s",
                    this, errorCode, errorDescription));
        }
        twkDidFail(errorCode, errorDescription, data);
    }

    private void notifyDidClose() {
        logger.finest("{0}", this);
        twkDidClose(data);
    }

    private static native void twkDidOpen(long data);
    private static native void twkDidReceiveData(byte[] buffer, int len,
                                                 long data);
    private static native void twkDidFail(int errorCode,
                                          String errorDescription, long data);
    private static native void twkDidClose(long data);

    private static String dump(byte[] buffer, int len) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < len) {
            StringBuilder c1 = new StringBuilder();
            StringBuilder c2 = new StringBuilder();
            for (int k = 0; k < 16; k++, i++) {
                if (i < len) {
                    int b = buffer[i] & 0xff;
                    c1.append(format("%02x ", b));
                    c2.append((b >= 0x20 && b <= 0x7e) ? (char) b : '.');
                } else {
                    c1.append("   ");
                }
            }
            sb.append(format("%n  ")).append(c1).append(' ').append(c2);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return format("SocketStreamHandle{host=%s, port=%d, ssl=%s, "
                + "data=0x%016X, state=%s, connected=%s}",
                host, port, ssl, data, state, connected);
    }

    private static final class CustomThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger index = new AtomicInteger(1);

        private CustomThreadFactory() {
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            group = (sm != null) ? sm.getThreadGroup()
                    : Thread.currentThread().getThreadGroup();
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, "SocketStreamHandle-"
                    + index.getAndIncrement());
            t.setDaemon(true);
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}
