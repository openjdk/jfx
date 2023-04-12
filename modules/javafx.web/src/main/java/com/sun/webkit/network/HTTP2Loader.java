/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.logging.PlatformLogger.Level;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.tk.Toolkit;
import com.sun.webkit.Invoker;
import com.sun.webkit.LoadListenerClient;
import com.sun.webkit.WebPage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.ConnectException;
import java.net.CookieHandler;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import javax.net.ssl.SSLHandshakeException;
import static com.sun.webkit.network.URLs.newURL;
import static java.net.http.HttpClient.Redirect;
import static java.net.http.HttpClient.Version;
import static java.net.http.HttpResponse.BodySubscribers;

final class HTTP2Loader extends URLLoaderBase {

    private static final PlatformLogger logger =
            PlatformLogger.getLogger(URLLoader.class.getName());

    private final WebPage webPage;
    private final boolean asynchronous;
    private String url;
    private String method;
    private final String headers;
    private FormDataElement[] formDataElements;
    private final long data;
    private volatile boolean canceled = false;

    private final CompletableFuture<Void> response;
    // Use singleton instance of HttpClient to get the maximum benefits
    @SuppressWarnings("removal")
    private final static HttpClient HTTP_CLIENT =
        AccessController.doPrivileged((PrivilegedAction<HttpClient>) () -> HttpClient.newBuilder()
                .version(Version.HTTP_2)  // this is the default
                .followRedirects(Redirect.NEVER) // WebCore handles redirection
                .connectTimeout(Duration.ofSeconds(30)) // FIXME: Add a property to control the timeout
                .cookieHandler(CookieHandler.getDefault())
                .build());
    // Singleton instance of direct ByteBuffer to transfer downloaded bytes from
    // Java to native
    private static final int DEFAULT_BUFSIZE = 40 * 1024;
    private final static ByteBuffer BUFFER;
    static {
       @SuppressWarnings("removal")
       int bufSize  = AccessController.doPrivileged(
                        (PrivilegedAction<Integer>) () ->
                            Integer.valueOf(System.getProperty("jdk.httpclient.bufsize", Integer.toString(DEFAULT_BUFSIZE))));
       BUFFER = ByteBuffer.allocateDirect(bufSize);
    }

    /**
     * Creates a new {@code HTTP2Loader}.
     */
    static HTTP2Loader create(WebPage webPage,
              ByteBufferPool byteBufferPool,
              boolean asynchronous,
              String url,
              String method,
              String headers,
              FormDataElement[] formDataElements,
              long data) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return new HTTP2Loader(
                webPage,
                byteBufferPool,
                asynchronous,
                url,
                method,
                headers,
                formDataElements,
                data);
        }
        return null;
    }

    // following 2 methods can be generalized and keep a common
    // implementation with URLLoader.java
    private String[] getCustomHeaders() {
        final var loc = Locale.getDefault();
        String lang = "";
        if (!loc.equals(Locale.US) && !loc.equals(Locale.ENGLISH)) {
            lang = loc.getCountry().isEmpty() ?
                loc.getLanguage() + ",":
                loc.getLanguage() + "-" + loc.getCountry() + ",";
        }

        return new String[] { "Accept-Language", lang.toLowerCase() + "en-us;q=0.8,en;q=0.7",
                              "Accept-Encoding", "gzip, inflate",
                              "Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7",
        };
    }

    private String[] getRequestHeaders() {
        return Arrays.stream(headers.split("\n"))
                     .flatMap(s -> Stream.of(s.split(":", 2))) // split from first occurance of :
                     .toArray(String[]::new);
    }

    private URI toURI() throws MalformedURLException {
        URI uriObj;
        try {
            uriObj = new URI(this.url);
        } catch(URISyntaxException | IllegalArgumentException e) {
            // slow path
            try {
                var urlObj = newURL(this.url);
                uriObj = new URI(
                        urlObj.getProtocol(),
                        urlObj.getUserInfo(),
                        urlObj.getHost(),
                        urlObj.getPort(),
                        urlObj.getPath(),
                        urlObj.getQuery(),
                        urlObj.getRef());
            } catch(URISyntaxException | MalformedURLException | IllegalArgumentException ex) {
                throw new MalformedURLException(this.url);
            }
        }
        return uriObj;
    }

    private HttpRequest.BodyPublisher getFormDataPublisher() {
        if (this.formDataElements == null) {
            return HttpRequest.BodyPublishers.noBody();
        }

        final var formDataElementsStream = new Vector<InputStream>();
        final AtomicLong length = new AtomicLong();
        for (final var formData : formDataElements) {
            try {
                formData.open();
                length.addAndGet(formData.getSize());
                formDataElementsStream.add(formData.getInputStream());
            } catch(IOException ex) {
                return null;
            }
        }

        final var stream = new SequenceInputStream(formDataElementsStream.elements());
        final var streamBodyPublisher = HttpRequest.BodyPublishers.ofInputStream(() -> stream);
        // Forwarding implementation to send didSendData notification
        // to WebCore. Otherwise `formDataPublisher = streamBodyPublisher`
        // can do the job.
        final var formDataPublisher = new HttpRequest.BodyPublisher() {
            @Override
            public long contentLength() {
                // streaming or fixed length
                return length.longValue() <= Integer.MAX_VALUE ? length.longValue() : -1;
            }

            @Override
            public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
                streamBodyPublisher.subscribe(new Flow.Subscriber<ByteBuffer>() {
                    @Override
                    public void onComplete() {
                        subscriber.onComplete();
                    }

                    @Override
                    public void onError(Throwable th) {
                        subscriber.onError(th);
                    }

                    @Override
                    public void onNext(ByteBuffer bytes) {
                        subscriber.onNext(bytes);
                        didSendData(bytes.limit(), length.longValue());
                    }

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        subscriber.onSubscribe(subscription);
                    }
                });
            }
        };
        return formDataPublisher;
    }

    // InputStream based subscriber is used to handle gzip|inflate encoded body. Since InputStream based subscriber is costly interms
    // of memory usage and thread usage, use only when response content-encoding is set to gzip|inflate.
    // There will be 2 threads involved while reading data from InputStream provided by BodySubscriber.
    //      1. The main worker which downloads HTTP data and writes to stream
    //      2. Other worker which reads data from the InputStream(getBody.thenAcceptAsync)
    // For the better efficiency, we should consider using java.util.zip.Inflater directly
    // to deal with gzip and inflate encoded data.
    private InputStream createZIPStream(final String type, InputStream in) throws IOException {
        if ("gzip".equalsIgnoreCase(type))
            return new GZIPInputStream(in);
        else if ("deflate".equalsIgnoreCase(type))
            return new InflaterInputStream(in);
        return in;
    }

    private BodySubscriber<Void> createZIPEncodedBodySubscriber(final String contentEncoding) {
        // Discard body if content type is unknown
        if (!("gzip".equalsIgnoreCase(contentEncoding)
                    || "inflate".equalsIgnoreCase(contentEncoding))) {
            logger.severe(String.format("Unknown encoding type '%s' found, discarding", contentEncoding));
            return BodySubscribers.discarding();
        }

        final BodySubscriber<InputStream> streamSubscriber = BodySubscribers.ofInputStream();
        final CompletionStage<Void> unzipTask = streamSubscriber.getBody().thenAcceptAsync(is -> {
            try (
                // To AutoClose the InputStreams
                final InputStream stream = is;
                final InputStream in = createZIPStream(contentEncoding, stream);
            ) {
                while (!canceled) {
                    // same as URLLoader.java
                    final byte[] buf = new byte[8 * 1024];
                    final int read = in.read(buf);
                    if (read < 0) {
                        didFinishLoading();
                        break;
                    }
                    didReceiveData(buf, read);
                }
            } catch (IOException ex) {
                didFail(ex);
            }
        });
        return new BodySubscriber<>() {
                @Override
                public void onComplete() {
                    streamSubscriber.onComplete();
                }

                @Override
                public void onError(Throwable th) {
                    streamSubscriber.onError(th);
                }

                @Override
                public void onNext(List<ByteBuffer> bytes) {
                    streamSubscriber.onNext(bytes);
                }

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    streamSubscriber.onSubscribe(subscription);
                }

                @Override
                public CompletionStage<Void> getBody() {
                    return streamSubscriber.getBody().thenCombine(unzipTask, (t, u) -> null);
                }
        };
    }

    // Normal plain body handler, simple, easy to use and pass data to downstream.
    private BodySubscriber<Void> createNormalBodySubscriber() {
        final BodySubscriber<Void> normalBodySubscriber = BodySubscribers.fromSubscriber(new Flow.Subscriber<List<ByteBuffer>>() {
            private Flow.Subscription subscription;
            private final AtomicBoolean subscribed = new AtomicBoolean();

            @Override
            public void onComplete() {
                didFinishLoading();
            }

            @Override
            public void onError(Throwable th) {}

            @Override
            public void onNext(final List<ByteBuffer> bytes) {
                didReceiveData(bytes);
                requestIfNotCancelled();
            }

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                if (!subscribed.compareAndSet(false, true)) {
                    subscription.cancel();
                } else {
                    this.subscription = subscription;
                    requestIfNotCancelled();
                }
            }

            private void requestIfNotCancelled() {
                if (canceled) {
                    subscription.cancel();
                } else {
                    subscription.request(1);
                }
            }
        });
        return normalBodySubscriber;
    }

    private BodySubscriber<Void> getBodySubscriber(final String contentEncoding) {
        return contentEncoding.isEmpty() ?
                  createNormalBodySubscriber() : createZIPEncodedBodySubscriber(contentEncoding);
    }

    private HTTP2Loader(WebPage webPage,
              ByteBufferPool byteBufferPool,
              boolean asynchronous,
              String url,
              String method,
              String headers,
              FormDataElement[] formDataElements,
              long data)
    {
        this.webPage = webPage;
        this.asynchronous = asynchronous;
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.formDataElements = formDataElements;
        this.data = data;

        URI uri;
        try {
            uri = toURI();
        } catch(MalformedURLException e) {
            this.response = null;
            didFail(e);
            return;
        }

        final var request = HttpRequest.newBuilder()
                               .uri(uri)
                               .headers(getRequestHeaders()) // headers from WebCore
                               .headers(getCustomHeaders()) // headers set by us
                               .version(Version.HTTP_2)  // this is the default
                               .method(method, getFormDataPublisher())
                               .build();

        final BodyHandler<Void> bodyHandler = rsp -> {
            if(!handleRedirectionIfNeeded(rsp)) {
                didReceiveResponse(rsp);
            }
            return getBodySubscriber(getContentEncoding(rsp));
        };

        // Run the HttpClient in the page's access control context
        @SuppressWarnings("removal")
        var tmpResponse = AccessController.doPrivileged((PrivilegedAction<CompletableFuture<Void>>) () -> {
            return HTTP_CLIENT.sendAsync(request, bodyHandler)
                              .thenAccept($ -> {})
                              .exceptionally(ex -> didFail(ex.getCause()));
        }, webPage.getAccessControlContext());
        this.response = tmpResponse;

        if (!asynchronous) {
            waitForRequestToComplete();
        }
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

    private void callBackIfNotCanceled(final Runnable r) {
        Invoker.getInvoker().invokeOnEventThread(() -> {
            if (!canceled) {
                r.run();
            }
        });
    }

    private void waitForRequestToComplete() {
        // Wait for the response using nested event loop. Once the response
        // arrives, nested event loop will be terminated.
        final Object key = new Object();
        this.response.handle((r, th) -> {
            Invoker.getInvoker().invokeOnEventThread(() ->
                Toolkit.getToolkit().exitNestedEventLoop(key, null));
            return null;
        });
        Toolkit.getToolkit().enterNestedEventLoop(key);
        // No need to join, nested event loop takes care of
        // blocking the caller until response arrives.
        // this.response.join();
    }

    private boolean handleRedirectionIfNeeded(final HttpResponse.ResponseInfo rsp) {
        switch(rsp.statusCode()) {
                case 301: // Moved Permanently
                case 302: // Found
                case 303: // See Other
                case 307: // Temporary Redirect
                    willSendRequest(rsp);
                    return true;

                case 304: // Not Modified
                    didReceiveResponse(rsp);
                    didFinishLoading();
                    return true;
        }
        return false;
    }

    private static long getContentLength(final HttpResponse.ResponseInfo rsp) {
        return rsp.headers().firstValueAsLong("content-length").orElse(-1);
    }

    private static String getContentType(final HttpResponse.ResponseInfo rsp) {
        return rsp.headers().firstValue("content-type").orElse("application/octet-stream");
    }

    private static String getContentEncoding(final HttpResponse.ResponseInfo rsp) {
        return rsp.headers().firstValue("content-encoding").orElse("");
    }

    private static String getHeadersAsString(final HttpResponse.ResponseInfo rsp) {
        return rsp.headers()
                  .map()
                  .entrySet()
                  .stream()
                  .map(e -> String.format("%s:%s", e.getKey(), e.getValue().stream().collect(Collectors.joining(","))))
                  .collect(Collectors.joining("\n")) + "\n";
    }

    private void willSendRequest(final HttpResponse.ResponseInfo rsp) {
        callBackIfNotCanceled(() -> {
            twkWillSendRequest(
                    rsp.statusCode(),
                    getContentType(rsp),
                    "",
                    getContentLength(rsp),
                    getHeadersAsString(rsp),
                    this.url,
                    data);
        });
    }

    private void didReceiveResponse(final HttpResponse.ResponseInfo rsp) {
        callBackIfNotCanceled(() -> {
            twkDidReceiveResponse(
                    rsp.statusCode(),
                    getContentType(rsp),
                    "",
                    getContentLength(rsp),
                    getHeadersAsString(rsp),
                    this.url,
                    data);
        });
    }

    private ByteBuffer getDirectBuffer(int size) {
        ByteBuffer dbb = BUFFER;
        // Though the chance of reaching here is rare, handle the
        // case by allocating a tmp direct buffer.
        if (size > dbb.capacity()) {
            dbb = ByteBuffer.allocateDirect(size);
        }
        return dbb.clear();
    }

    private ByteBuffer copyToDirectBuffer(final ByteBuffer bb) {
        return getDirectBuffer(bb.limit()).put(bb).flip();
    }

    // another variant to use from createZIPEncodedBodySubscriber
    private void didReceiveData(final byte[] bytes, int size) {
        callBackIfNotCanceled(() -> {
            notifyDidReceiveData(getDirectBuffer(size).put(bytes, 0, size).flip());
        });
    }

    private void didReceiveData(final List<ByteBuffer> bytes) {
        callBackIfNotCanceled(() -> bytes.stream()
                                          .map(this::copyToDirectBuffer)
                                          .forEach(this::notifyDidReceiveData)
        );
    }

    private void notifyDidReceiveData(ByteBuffer byteBuffer) {
        Invoker.getInvoker().checkEventThread();
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(String.format(
                    "byteBuffer: [%s], "
                    + "position: [%s], "
                    + "remaining: [%s], "
                    + "data: [0x%016X]",
                    byteBuffer,
                    byteBuffer.position(),
                    byteBuffer.remaining(),
                    data));
        }
        twkDidReceiveData(byteBuffer, byteBuffer.position(), byteBuffer.remaining(), data);
    }

    private void didFinishLoading() {
        callBackIfNotCanceled(this::notifyDidFinishLoading);
    }

    private void notifyDidFinishLoading() {
        Invoker.getInvoker().checkEventThread();
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(String.format("data: [0x%016X]", data));
        }
        twkDidFinishLoading(data);
    }


    private Void didFail(final Throwable th) {
        callBackIfNotCanceled(() ->  {
            // FIXME: simply copied from URLLoader.java, it should be
            // retwritten using if..else rather than throw.
            int errorCode;
            try {
                throw th;
            } catch (MalformedURLException ex) {
                errorCode = LoadListenerClient.MALFORMED_URL;
            } catch (@SuppressWarnings("removal") AccessControlException ex) {
                errorCode = LoadListenerClient.PERMISSION_DENIED;
            } catch (UnknownHostException ex) {
                errorCode = LoadListenerClient.UNKNOWN_HOST;
            } catch (NoRouteToHostException ex) {
                errorCode = LoadListenerClient.NO_ROUTE_TO_HOST;
            } catch (ConnectException ex) {
                errorCode = LoadListenerClient.CONNECTION_REFUSED;
            } catch (SocketException ex) {
                errorCode = LoadListenerClient.CONNECTION_RESET;
            } catch (SSLHandshakeException ex) {
                errorCode = LoadListenerClient.SSL_HANDSHAKE;
            } catch (SocketTimeoutException | HttpTimeoutException ex) {
                errorCode = LoadListenerClient.CONNECTION_TIMED_OUT;
            } catch (FileNotFoundException ex) {
                errorCode = LoadListenerClient.FILE_NOT_FOUND;
            } catch (Throwable ex) {
                errorCode = LoadListenerClient.UNKNOWN_ERROR;
            }
            notifyDidFail(errorCode, url, th.getMessage());
        });
        return null;
    }

    private void notifyDidFail(int errorCode, String url, String message) {
        Invoker.getInvoker().checkEventThread();
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

    private void didSendData(final long totalBytesSent,
                             final long totalBytesToBeSent)
    {
        callBackIfNotCanceled(() -> notifyDidSendData(totalBytesSent, totalBytesToBeSent));
    }

    private void notifyDidSendData(long totalBytesSent,
                                   long totalBytesToBeSent)
    {
        Invoker.getInvoker().checkEventThread();
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
}
