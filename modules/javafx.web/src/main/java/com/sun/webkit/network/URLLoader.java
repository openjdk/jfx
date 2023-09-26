/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;
import com.sun.webkit.Invoker;
import com.sun.webkit.LoadListenerClient;
import com.sun.webkit.WebPage;
import static com.sun.webkit.network.URLs.newURL;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import javax.net.ssl.SSLHandshakeException;

/**
 * A runnable that loads a resource specified by a URL.
 */
final class URLLoader extends URLLoaderBase implements Runnable {

    private static final PlatformLogger logger =
            PlatformLogger.getLogger(URLLoader.class.getName());
    private static final int MAX_BUF_COUNT = 3;
    private static final String GET = "GET";
    private static final String HEAD = "HEAD";
    private static final String DELETE = "DELETE";


    private final WebPage webPage;
    private final ByteBufferPool byteBufferPool;
    private final boolean asynchronous;
    private String url;
    private String method;
    private final String headers;
    private FormDataElement[] formDataElements;
    private final long data;
    private volatile boolean canceled = false;


    /**
     * Creates a new {@code URLLoader}.
     */
    URLLoader(WebPage webPage,
              ByteBufferPool byteBufferPool,
              boolean asynchronous,
              String url,
              String method,
              String headers,
              FormDataElement[] formDataElements,
              long data)
    {
        this.webPage = webPage;
        this.byteBufferPool = byteBufferPool;
        this.asynchronous = asynchronous;
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.formDataElements = formDataElements;
        this.data = data;
    }


    /**
     * Cancels this loader.
     */
    @Override
    public void fwkCancel() {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(String.format("data: [0x%016X]", data));
        }
        canceled = true;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("removal")
    @Override
    public void run() {
        // Run the loader in the page's access control context
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            doRun();
            return null;
        }, webPage.getAccessControlContext());
    }

    /**
     * Executes this loader.
     */
    private void doRun() {
        Throwable error = null;
        int errorCode = 0;
        try {
            boolean streaming = true;
            boolean connectionResetRetry = true;
            while (true) {
                // RT-14438
                String actualUrl = url;
                if (url.startsWith("file:")) {
                    int questionMarkPosition = url.indexOf('?');
                    if (questionMarkPosition != -1) {
                        actualUrl = url.substring(0, questionMarkPosition);
                    }
                }

                URL urlObject = newURL(actualUrl);

                // RT-22458
                workaround7177996(urlObject);

                URLConnection c = urlObject.openConnection();
                prepareConnection(c);

                try {
                    sendRequest(c, streaming);
                    receiveResponse(c);
                } catch (HttpRetryException ex) {
                    // RT-19914
                    if (streaming) {
                        streaming = false;
                        continue; // retry without streaming
                    } else {
                        throw ex;
                    }
                } catch (SocketException ex) {
                    // SocketException: Connection reset, Retry once
                    if ("Connection reset".equals(ex.getMessage()) && connectionResetRetry) {
                        connectionResetRetry = false;
                        continue;
                    } else {
                        throw ex;
                    }
                } finally {
                    close(c);
                }
                break;
            }
        } catch (MalformedURLException ex) {
            error = ex;
            errorCode = LoadListenerClient.MALFORMED_URL;
        } catch (@SuppressWarnings("removal") AccessControlException ex) {
            error = ex;
            errorCode = LoadListenerClient.PERMISSION_DENIED;
        } catch (UnknownHostException ex) {
            error = ex;
            errorCode = LoadListenerClient.UNKNOWN_HOST;
        } catch (NoRouteToHostException ex) {
            error = ex;
            errorCode = LoadListenerClient.NO_ROUTE_TO_HOST;
        } catch (ConnectException ex) {
            error = ex;
            errorCode = LoadListenerClient.CONNECTION_REFUSED;
        } catch (SocketException ex) {
            error = ex;
            errorCode = LoadListenerClient.CONNECTION_RESET;
        } catch (SSLHandshakeException ex) {
            error = ex;
            errorCode = LoadListenerClient.SSL_HANDSHAKE;
        } catch (SocketTimeoutException ex) {
            error = ex;
            errorCode = LoadListenerClient.CONNECTION_TIMED_OUT;
        } catch (InvalidResponseException ex) {
            error = ex;
            errorCode = LoadListenerClient.INVALID_RESPONSE;
        } catch (FileNotFoundException ex) {
            error = ex;
            errorCode = LoadListenerClient.FILE_NOT_FOUND;
        } catch (Throwable th) {
            error = th;
            errorCode = LoadListenerClient.UNKNOWN_ERROR;
        }

        if (error != null) {
            if (errorCode == LoadListenerClient.UNKNOWN_ERROR) {
                logger.warning("Unexpected error", error);
            } else {
                logger.finest("Load error", error);
            }
            didFail(errorCode, error.getMessage());
        }
    }

    private static void workaround7177996(URL url)
        throws FileNotFoundException
    {
        if (!url.getProtocol().equals("file")) {
            return;
        }

        String host = url.getHost();
        if (host == null || host.equals("") || host.equals("~")
                || host.equalsIgnoreCase("localhost") )
        {
           return;
        }

        if (PlatformUtil.isWindows()) {
            String path = null;
            try {
                path = URLDecoder.decode(url.getPath(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // The system should always have the platform default
            }
            path = path.replace('/', '\\');
            path = path.replace('|', ':');
            File file = new File("\\\\" + host + path);
            if (!file.exists()) {
                throw new FileNotFoundException("File not found: " + url);
            }
        } else {
            throw new FileNotFoundException("File not found: " + url);
        }
    }

    /**
     * Prepares a connection.
     */
    private void prepareConnection(URLConnection c) throws IOException {
        // The following two timeouts are quite arbitrary and should
        // probably be configurable via an API
        c.setConnectTimeout(30000);   // 30 seconds
        c.setReadTimeout(60000 * 60); // 60 minutes

        // Given that WebKit has its own cache, do not use
        // any URLConnection caches, even if someone installs them.
        // As a side effect, this fixes the problem of WebPane not
        // working well with the plug-in cache, which was one of
        // the causes for RT-11880.
        c.setUseCaches(false);

        Locale loc = Locale.getDefault();
        String lang = "";
        if (!loc.equals(Locale.US) && !loc.equals(Locale.ENGLISH)) {
            lang = loc.getCountry().isEmpty() ?
                loc.getLanguage() + ",":
                loc.getLanguage() + "-" + loc.getCountry() + ",";
        }
        c.setRequestProperty("Accept-Language", lang.toLowerCase() + "en-us;q=0.8,en;q=0.7");
        c.setRequestProperty("Accept-Encoding", "gzip");
        c.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");

        if (headers != null && headers.length() > 0) {
            for (String h : headers.split("\n")) {
                int i = h.indexOf(':');
                if (i > 0) {
                    c.addRequestProperty(h.substring(0, i), h.substring(i + 2));
                }
            }
        }

        if (c instanceof HttpURLConnection) {
            HttpURLConnection httpConnection = (HttpURLConnection) c;
            httpConnection.setRequestMethod(method);
            // There are too many bugs in the way HttpURLConnection handles
            // redirects, so we will deal with them ourselves
            httpConnection.setInstanceFollowRedirects(false);
        }
    }

    /**
     * Sends request to the server.
     */
    private void sendRequest(URLConnection c, boolean streaming)
        throws IOException
    {
        OutputStream out = null;
        try {
            long bytesToBeSent = 0;
            boolean sendFormData = formDataElements != null
                    && c instanceof HttpURLConnection
                    && !method.equals(DELETE);
            boolean isGetOrHead = method.equals(GET) || method.equals(HEAD);
            if (sendFormData) {
                c.setDoOutput(true);

                for (FormDataElement formDataElement : formDataElements) {
                    formDataElement.open();
                    bytesToBeSent += formDataElement.getSize();
                }

                if (streaming) {
                    HttpURLConnection http = (HttpURLConnection) c;
                    if (bytesToBeSent <= Integer.MAX_VALUE) {
                        http.setFixedLengthStreamingMode((int) bytesToBeSent);
                    } else {
                        http.setChunkedStreamingMode(0);
                    }
                }
            } else if (!isGetOrHead && (c instanceof HttpURLConnection)) {
                c.setRequestProperty("Content-Length", "0");
            }

            int maxTryCount = isGetOrHead ? 3 : 1;
            c.setConnectTimeout(c.getConnectTimeout() / maxTryCount);
            int tryCount = 0;
            while (!canceled) {
                try {
                    c.connect();
                    break;
                } catch (SocketTimeoutException ex) {
                    if (++tryCount >= maxTryCount) {
                        throw ex;
                    }
                } catch (IllegalArgumentException ex) {
                    // Happens with some malformed URLs
                    throw new MalformedURLException(url);
                }
            }

            if (sendFormData) {
                out = c.getOutputStream();
                byte[] buffer = new byte[4096];
                long bytesSent = 0;
                for (FormDataElement formDataElement : formDataElements) {
                    InputStream in = formDataElement.getInputStream();
                    int count;
                    while ((count = in.read(buffer)) > 0) {
                        out.write(buffer, 0, count);
                        bytesSent += count;
                        didSendData(bytesSent, bytesToBeSent);
                    }
                    formDataElement.close();
                }
                out.flush();
                out.close();
                out = null;
            }
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignore) {}
            }
            if (formDataElements != null && c instanceof HttpURLConnection) {
                for (FormDataElement formDataElement : formDataElements) {
                    try {
                        formDataElement.close();
                    } catch (IOException ignore) {}
                }
            }
        }
    }

    /**
     * Receives response from the server.
     */
    private void receiveResponse(URLConnection c)
        throws IOException, InterruptedException
    {
        if (canceled) {
            return;
        }

        InputStream errorStream = null;

        if (c instanceof HttpURLConnection) {
            HttpURLConnection http = (HttpURLConnection) c;

            int code = http.getResponseCode();
            if (code == -1) {
                throw new InvalidResponseException();
            }

            if (canceled) {
                return;
            }

            // See RT-17435
            switch (code) {
                case 301: // Moved Permanently
                case 302: // Found
                case 303: // See Other
                case 307: // Temporary Redirect
                    willSendRequest(c);
                    break;

                case 304: // Not Modified
                    didReceiveResponse(c);
                    didFinishLoading();
                    return;
            }

            if (code >= 400 && !method.equals(HEAD)) {
                errorStream = http.getErrorStream();
            }
        }

        // Let's see if it's an ftp (or ftps) URL and we need to transform
        // a directory listing into HTML
        if (url.startsWith("ftp:") || url.startsWith("ftps:")) {
            boolean dir = false;
            boolean notsure = false;
            // Unfortunately, there is no clear way to determine if we are
            // accessing a directory, so a bit of guessing is in order
            String path = c.getURL().getPath();
            if (path == null || path.isEmpty() || path.endsWith("/")
                    || path.contains(";type=d"))
            {
                dir = true;
            } else {
                String type = c.getContentType();
                if ("text/plain".equalsIgnoreCase(type)
                        || "text/html".equalsIgnoreCase(type))
                {
                    dir = true;
                    notsure = true;
                }
            }
            if (dir) {
                c = new DirectoryURLConnection(c, notsure);
            }
        }

        // Same is true for FileURLConnection
        if (url.startsWith("file:")) {
            if("text/plain".equals(c.getContentType())
                    && c.getHeaderField("content-length") == null)
            {
                // It is a directory
                c = new DirectoryURLConnection(c);
            }
        }

        didReceiveResponse(c);

        if (method.equals(HEAD)) {
            didFinishLoading();
            return;
        }

        InputStream inputStream = null;
        try {
            inputStream = errorStream == null
                ? c.getInputStream() : errorStream;
        } catch (HttpRetryException ex) {
            // HttpRetryException is handled from doRun() method.
            // Hence rethrowing the exception to caller(doRun() method)
            throw ex;
        } catch (IOException e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(String.format("Exception caught: [%s], %s",
                    e.getClass().getSimpleName(),
                    e.getMessage()));
            }
        }

        String encoding = c.getContentEncoding();
        if (inputStream != null) {
            try {
                if ("gzip".equalsIgnoreCase(encoding)) {
                    inputStream = new GZIPInputStream(inputStream);
                } else if ("deflate".equalsIgnoreCase(encoding)) {
                    inputStream = new InflaterInputStream(inputStream);
                }
            } catch (IOException e) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(String.format("Exception caught: [%s], %s",
                        e.getClass().getSimpleName(),
                        e.getMessage()));
                }
            }
        }

        ByteBufferAllocator allocator =
                byteBufferPool.newAllocator(MAX_BUF_COUNT);
        ByteBuffer byteBuffer = null;
        try {
            if (inputStream != null) {
                // 8192 is the default size of a BufferedInputStream used in
                // most URLConnections, by using the same size, we avoid quite
                // a few System.arrayCopy() calls
                byte[] buffer = new byte[8192];
                while (!canceled) {
                    int count;
                    try {
                        count = inputStream.read(buffer);
                    } catch (EOFException ex) {
                        // can be thrown by GZIPInputStream signaling
                        // the end of the stream
                        count = -1;
                    }

                    if (count == -1) {
                        break;
                    }

                    if (byteBuffer == null) {
                        byteBuffer = allocator.allocate();
                    }

                    int remaining = byteBuffer.remaining();
                    if (count < remaining) {
                        byteBuffer.put(buffer, 0, count);
                    } else {
                        byteBuffer.put(buffer, 0, remaining);

                        byteBuffer.flip();
                        didReceiveData(byteBuffer, allocator);
                        byteBuffer = null;

                        int outstanding = count - remaining;
                        if (outstanding > 0) {
                            byteBuffer = allocator.allocate();
                            byteBuffer.put(buffer, remaining, outstanding);
                        }
                    }
                }
            }
            if (!canceled) {
                if (byteBuffer != null && byteBuffer.position() > 0) {
                    byteBuffer.flip();
                    didReceiveData(byteBuffer, allocator);
                    byteBuffer = null;
                }
                didFinishLoading();
            }
        } finally {
            if (byteBuffer != null) {
                allocator.release(byteBuffer);
            }
        }
    }

    /**
     * Releases the resources that may be associated with a connection.
     */
    private static void close(URLConnection c) {
        if (c instanceof HttpURLConnection) {
            InputStream errorStream = ((HttpURLConnection) c).getErrorStream();
            if (errorStream != null) {
                try {
                    errorStream.close();
                } catch (IOException ignore) {}
            }
        }
        try {
            c.getInputStream().close();
        } catch (IOException ignore) {}
    }

    /**
     * Signals an invalid response from the server.
     */
    private static final class InvalidResponseException extends IOException {
        private InvalidResponseException() {
            super("Invalid server response");
        }
    }

    private void didSendData(final long totalBytesSent,
                             final long totalBytesToBeSent)
    {
        callBack(() -> {
            if (!canceled) {
                notifyDidSendData(totalBytesSent, totalBytesToBeSent);
            }
        });
    }

    private void notifyDidSendData(long totalBytesSent,
                                   long totalBytesToBeSent)
    {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(String.format(
                    "totalBytesSent: [%d], "
                    + "totalBytesToBeSent: [%d], "
                    + "data: [0x%016X]",
                    totalBytesSent,
                    totalBytesToBeSent,
                    data));
        }
        twkDidSendData(totalBytesSent, totalBytesToBeSent, data);
    }

    private void willSendRequest(URLConnection c)
    {
        final int status = extractStatus(c);
        final String contentType = c.getContentType();
        final String contentEncoding = extractContentEncoding(c);
        final long contentLength = extractContentLength(c);
        final String responseHeaders = extractHeaders(c);
        final String adjustedUrl = adjustUrlForWebKit(url);
        callBack(() -> {
            if (!canceled) {
                notifyWillSendRequest(
                        status,
                        contentType,
                        contentEncoding,
                        contentLength,
                        responseHeaders,
                        adjustedUrl);
            }
        });
    }

    private void notifyWillSendRequest(int status,
                                          String contentType,
                                          String contentEncoding,
                                          long contentLength,
                                          String headers,
                                          String url)
    {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(String.format(
                    "status: [%d], "
                    + "contentType: [%s], "
                    + "contentEncoding: [%s], "
                    + "contentLength: [%d], "
                    + "url: [%s], "
                    + "data: [0x%016X], "
                    + "headers:%n%s",
                    status,
                    contentType,
                    contentEncoding,
                    contentLength,
                    url,
                    data,
                    Util.formatHeaders(headers)));
        }
        twkWillSendRequest(
                status,
                contentType,
                contentEncoding,
                contentLength,
                headers,
                url,
                data);
    }

    private void didReceiveResponse(URLConnection c) {
        final int status = extractStatus(c);
        final String contentType = c.getContentType();
        final String contentEncoding = extractContentEncoding(c);
        final long contentLength = extractContentLength(c);
        final String responseHeaders = extractHeaders(c);
        final String adjustedUrl = adjustUrlForWebKit(url);
        callBack(() -> {
            if (!canceled) {
                notifyDidReceiveResponse(
                        status,
                        contentType,
                        contentEncoding,
                        contentLength,
                        responseHeaders,
                        adjustedUrl);
            }
        });
    }

    private void notifyDidReceiveResponse(int status,
                                          String contentType,
                                          String contentEncoding,
                                          long contentLength,
                                          String headers,
                                          String url)
    {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(String.format(
                    "status: [%d], "
                    + "contentType: [%s], "
                    + "contentEncoding: [%s], "
                    + "contentLength: [%d], "
                    + "url: [%s], "
                    + "data: [0x%016X], "
                    + "headers:%n%s",
                    status,
                    contentType,
                    contentEncoding,
                    contentLength,
                    url,
                    data,
                    Util.formatHeaders(headers)));
        }
        twkDidReceiveResponse(
                status,
                contentType,
                contentEncoding,
                contentLength,
                headers,
                url,
                data);
    }

    private void didReceiveData(final ByteBuffer byteBuffer,
                                final ByteBufferAllocator allocator)
    {
        callBack(() -> {
            if (!canceled) {
                notifyDidReceiveData(
                        byteBuffer,
                        byteBuffer.position(),
                        byteBuffer.remaining());
            }
            allocator.release(byteBuffer);
        });
    }

    private void notifyDidReceiveData(ByteBuffer byteBuffer,
                                      int position,
                                      int remaining)
    {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(String.format(
                    "byteBuffer: [%s], "
                    + "position: [%s], "
                    + "remaining: [%s], "
                    + "data: [0x%016X]",
                    byteBuffer,
                    position,
                    remaining,
                    data));
        }
        twkDidReceiveData(byteBuffer, position, remaining, data);
    }

    private void didFinishLoading() {
        callBack(() -> {
            if (!canceled) {
                notifyDidFinishLoading();
            }
        });
    }

    private void notifyDidFinishLoading() {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(String.format("data: [0x%016X]", data));
        }
        twkDidFinishLoading(data);
    }

    private void didFail(final int errorCode, final String message) {
        final String adjustedUrl = adjustUrlForWebKit(url);
        callBack(() -> {
            if (!canceled) {
                notifyDidFail(errorCode, adjustedUrl, message);
            }
        });
    }

    private void notifyDidFail(int errorCode, String url, String message) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(String.format(
                    "errorCode: [%d], "
                    + "url: [%s], "
                    + "message: [%s], "
                    + "data: [0x%016X]",
                    errorCode,
                    url,
                    message,
                    data));
        }
        twkDidFail(errorCode, url, message, data);
    }

    private void callBack(Runnable runnable) {
        if (asynchronous) {
            Invoker.getInvoker().invokeOnEventThread(runnable);
        } else {
            runnable.run();
        }
    }

    /**
     * Given a {@link URLConnection}, returns the connection status
     * for passing into native callbacks.
     */
    private static int extractStatus(URLConnection c) {
        int status = 0;
        if (c instanceof HttpURLConnection) {
            try {
                status = ((HttpURLConnection) c).getResponseCode();
            } catch (java.io.IOException ignore) {}
        }
        return status;
    }

    /**
     * Given a {@link URLConnection}, returns the content encoding
     * for passing into native callbacks.
     */
    private static String extractContentEncoding(URLConnection c) {
        String contentEncoding = c.getContentEncoding();
        // For compressed streams, the encoding is in Content-Type
        if ("gzip".equalsIgnoreCase(contentEncoding) ||
            "deflate".equalsIgnoreCase(contentEncoding))
        {
            contentEncoding = null;
            String contentType  = c.getContentType();
            if (contentType != null) {
                int i = contentType.indexOf("charset=");
                if (i >= 0) {
                    contentEncoding = contentType.substring(i + 8);
                    i = contentEncoding.indexOf(";");
                    if (i > 0) {
                        contentEncoding = contentEncoding.substring(0, i);
                    }
                }
            }
        }
        return contentEncoding;
    }

    /**
     * Given a {@link URLConnection}, returns the content length
     * for passing into native callbacks.
     */
    private static long extractContentLength(URLConnection c) {
        // Cannot use URLConnection.getContentLength()
        // as it only returns an int
        try {
            return Long.parseLong(c.getHeaderField("content-length"));
        } catch (Exception ex) {
            return -1;
        }
    }

    /**
     * Given a {@link URLConnection}, returns the headers string
     * for passing into native callbacks.
     */
    private static String extractHeaders(URLConnection c) {
        StringBuilder sb = new StringBuilder();
        if (c instanceof HttpURLConnection) {
            Map<String, List<String>> headers = c.getHeaderFields();
            for (Map.Entry<String, List<String>> entry: headers.entrySet()) {
                String key = entry.getKey();
                List<String> values = entry.getValue();
                for (String value : values) {
                    sb.append(key != null ? key : "");
                    sb.append(':').append(value).append('\n');
                }
            }
        }
        return sb.toString();
    }

    /**
     * Adjust a URL string for passing into WebKit.
     */
    private static String adjustUrlForWebKit(String url) {
        try {
            url = Util.adjustUrlForWebKit(url);
        } catch (Exception ignore) {
        }
        return url;
    }
}
