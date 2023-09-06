/*
 * Copyright (c) 2010, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.media.jfxmedia.locator;

import com.sun.javafx.PlatformUtil;
import com.sun.media.jfxmedia.MediaException;
import com.sun.media.jfxmedia.MediaManager;
import com.sun.media.jfxmedia.logging.Logger;
import com.sun.media.jfxmediaimpl.MediaUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

/**
 * A
 * <code>Locator</code> which refers to a
 * <code>URI</code>.
 */
public class Locator {

    /**
     * The content type used if no more specific one may be derived.
     */
    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    /**
     * The number of times to attempt to open a URL connection to test the URI.
     */
    private static final int MAX_CONNECTION_ATTEMPTS = 5;
    /**
     * The number of milliseconds between attempts to open a URL connection.
     */
    private static final long CONNECTION_RETRY_INTERVAL = 1000L;
    /**
     * Timeout in milliseconds to wait for connection (5 min).
     */
    private static final int CONNECTION_TIMEOUT = 300000;
    /**
     * The content type of the media content.
     */
    protected String contentType = DEFAULT_CONTENT_TYPE;
    /**
     * A hint for the internal player.
     */
    protected long contentLength = -1;    //Used as a hint for the native layer
    /**
     * The URI source.
     */
    protected URI uri;
    /**
     * Properties to be associated with the connection made to the URI. The
     * significance of the properties depends on the URI protocol and type of
     * media source.
     */
    private Map<String, Object> connectionProperties;
    /**
     * Mutex for connectionProperties;
     */
    private final Object propertyLock = new Object();

    /*
     * These variables will be initialized by constructor and used by init()
     */
    private String uriString = null;
    private String scheme = null;
    private String protocol = null;

    /*
     * if cached, we store a hard reference to keep it alive
     */
    private LocatorCache.CacheReference cacheEntry = null;

    /*
     * True if init(), getContentLength() and getContentType() can block; false
     * otherwise.
     */
    private boolean canBlock = false;

    /*
     * Used to block getContentLength() and getContentType().
     */
    private CountDownLatch readySignal = new CountDownLatch(1);

    /**
     * iOS only: determines if the given URL points to the iPod library
     */
    private boolean isIpod;

    /**
     * Holds connection and response code returned from getConnection()
     */
    private static class LocatorConnection {

        public HttpURLConnection connection = null;
        public int responseCode = HttpURLConnection.HTTP_OK;
    }

    private LocatorConnection getConnection(URI uri, String requestMethod)
            throws MalformedURLException, IOException {

        // Check ability to connect.
        LocatorConnection locatorConnection = new LocatorConnection();
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod(requestMethod);
        // Set timeouts, otherwise we can wait forever.
        connection.setConnectTimeout(CONNECTION_TIMEOUT);
        connection.setReadTimeout(CONNECTION_TIMEOUT);

        // Set request headers.
        synchronized (propertyLock) {
            if (connectionProperties != null) {
                for (String key : connectionProperties.keySet()) {
                    Object value = connectionProperties.get(key);
                    if (value instanceof String) {
                        connection.setRequestProperty(key, (String) value);
                    }
                }
            }
        }

        // Store response code so we can get more information about
        // returning connection.
        locatorConnection.responseCode = connection.getResponseCode();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            locatorConnection.connection = connection;
        } else {
            closeConnection(connection);
            locatorConnection.connection = null;
        }
        return locatorConnection;
    }

    private static long getContentLengthLong(URLConnection connection) {
        @SuppressWarnings("removal")
        Method method = AccessController.doPrivileged((PrivilegedAction<Method>) () -> {
            try {
                return URLConnection.class.getMethod("getContentLengthLong");
            } catch (NoSuchMethodException ex) {
                return null;
            }
        });

        try {
            if (method != null) {
                return (long) method.invoke(connection);
            } else {
                return connection.getContentLength();
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            return -1;
        }
    }

    /**
     * Constructs an object representing a media source.
     *
     * @param uri The URI source.
     * @throws NullPointerException if
     * <code>uri</code> is
     * <code>null</code>.
     * @throws IllegalArgumentException if the URI's scheme is
     * <code>null</code>.
     * @throws URISyntaxException if the supplied URI requires some further
     * manipulation in order to be used and this procedure fails to produce a
     * usable URI.
     * @throws IllegalArgumentException if the URI is a Jar URL as described in
     * {@link JarURLConnection https://docs.oracle.com/javase/8/docs/api/java/net/JarURLConnection.html},
     * and the scheme of the URL after removing the leading four characters is
     * <code>null</code>.
     * @throws UnsupportedOperationException if the URI's protocol is
     * unsupported.
     */
    public Locator(URI uri) throws URISyntaxException {
        // Check for NULL parameter.
        if (uri == null) {
            throw new NullPointerException("uri == null!");
        }

        // Get the scheme part.
        uriString = uri.toASCIIString();
        scheme = uri.getScheme();
        if (scheme == null) {
            throw new IllegalArgumentException("uri.getScheme() == null! uri == '" + uri + "'");
        }
        scheme = scheme.toLowerCase();

        // Get the protocol.
        if (scheme.equals("jar")) {
            URI subURI = new URI(uriString.substring(4));
            protocol = subURI.getScheme();
            if (protocol == null) {
                throw new IllegalArgumentException("uri.getScheme() == null! subURI == '" + subURI + "'");
            }
            protocol = protocol.toLowerCase();
        } else {
            protocol = scheme; // scheme is already lower case.
        }

        if (PlatformUtil.isIOS() && protocol.equals("ipod-library")) {
            isIpod = true;
        }

        // Verify the protocol is supported.
        if (!isIpod && !MediaManager.canPlayProtocol(protocol)) {
            throw new UnsupportedOperationException("Unsupported protocol \"" + protocol + "\"");
        }

        // Check if we can block
        if (protocol.equals("http") || protocol.equals("https")) {
            canBlock = true;
        }

        // Set instance variable.
        this.uri = uri;
    }

    private InputStream getInputStream(URI uri)
            throws MalformedURLException, IOException {
        URL url = uri.toURL();
        URLConnection connection = url.openConnection();

        // Set request headers.
        synchronized (propertyLock) {
            if (connectionProperties != null) {
                for (String key : connectionProperties.keySet()) {
                    Object value = connectionProperties.get(key);
                    if (value instanceof String) {
                        connection.setRequestProperty(key, (String) value);
                    }
                }
            }
        }

        InputStream inputStream = url.openStream();
        contentLength = getContentLengthLong(connection);
        return inputStream;
    }

    /**
     * Tell this Locator to preload the media into memory, if it hasn't been
     * already.
     */
    public void cacheMedia() {
        LocatorCache.CacheReference ref = LocatorCache.locatorCache().fetchURICache(uri);
        if (null == ref) {
            ByteBuffer cacheBuffer;

            // not cached, load it
            InputStream is;
            try {
                is = getInputStream(uri);
            } catch (Throwable t) {
                return; // just bail
            }

            // contentLength is set now, so we can go ahead and allocate
            cacheBuffer = ByteBuffer.allocateDirect((int) contentLength);
            byte[] readBuf = new byte[8192];
            int total = 0;
            int count;
            while (total < contentLength) {
                try {
                    count = is.read(readBuf);
                } catch (IOException ioe) {
                    try {
                        is.close();
                    } catch (Throwable t) {
                    }
                    if (Logger.canLog(Logger.DEBUG)) {
                        Logger.logMsg(Logger.DEBUG, "IOException trying to preload media: " + ioe);
                    }
                    return;
                }

                if (count == -1) {
                    break; // EOS
                }

                cacheBuffer.put(readBuf, 0, count);
            }

            try {
                is.close();
            } catch (Throwable t) {
            }

            cacheEntry = LocatorCache.locatorCache().registerURICache(uri, cacheBuffer, contentType);
            canBlock = false;
        }
    }

    /*
     * True if init() can block; false otherwise.
     */
    public boolean canBlock() {
        return canBlock;
    }

    /*
     * Initialize locator. Use canBlock() to determine if init() can block.
     *
     * @throws URISyntaxException if the supplied URI requires some further
     * manipulation in order to be used and this procedure fails to produce a
     * usable URI. @throws IOExceptions if a stream cannot be opened over a
     * connection of the corresponding URL. @throws MediaException if the
     * content type of the media is not supported. @throws FileNotFoundException
     * if the media is not available.
     */
    public void init() throws URISyntaxException, IOException, FileNotFoundException {
        try {
            // Ensure the correct number of '/'s follows the ':'.
            int firstSlash = uriString.indexOf("/");
            if (firstSlash != -1 && uriString.charAt(firstSlash + 1) != '/') {
                // Only one '/' after the ':'.
                if (protocol.equals("file")) {
                    // Map file:/somepath to file:///somepath
                    uriString = uriString.replaceFirst("/", "///");
                } else if (protocol.equals("http") || protocol.equals("https")) {
                    // Map http:/somepath to http://somepath
                    uriString = uriString.replaceFirst("/", "//");
                }
            }

            // On non-Windows systems, replace "/~/" with home directory path + "/".
            if (!PlatformUtil.isWindows() && protocol.equals("file")) {
                int index = uriString.indexOf("/~/");
                if (index != -1) {
                    uriString = uriString.substring(0, index)
                            + System.getProperty("user.home")
                            + uriString.substring(index + 2);
                }
            }

            // Recreate the URI if needed
            uri = new URI(uriString);

            // First check if this URI is cached, if it is then we're done
            cacheEntry = LocatorCache.locatorCache().fetchURICache(uri);
            if (null != cacheEntry) {
                // Cache hit! Grab contentType and contentLength and be done
                contentType = cacheEntry.getMIMEType();
                contentLength = cacheEntry.getBuffer().capacity();
                if (Logger.canLog(Logger.DEBUG)) {
                    Logger.logMsg(Logger.DEBUG, "Locator init cache hit:"
                            + "\n    uri " + uri
                            + "\n    type " + contentType
                            + "\n    length " + contentLength);
                }
                return;
            }

            // Try to open a connection on the corresponding URL.
            boolean isConnected = false;
            boolean isMediaUnAvailable = false;
            boolean isMediaSupported = true;
            if (!isIpod) {
                for (int numConnectionAttempts = 0; numConnectionAttempts < MAX_CONNECTION_ATTEMPTS; numConnectionAttempts++) {
                    try {
                        // Verify existence.
                        if (scheme.equals("http") || scheme.equals("https")) {
                            // Check ability to connect, trying HEAD before GET.
                            LocatorConnection locatorConnection = getConnection(uri, "HEAD");
                            if (locatorConnection == null || locatorConnection.connection == null) {
                                locatorConnection = getConnection(uri, "GET");
                            }

                            if (locatorConnection != null && locatorConnection.connection != null) {
                                isConnected = true;

                                // Get content type.
                                contentType = locatorConnection.connection.getContentType();
                                contentLength = getContentLengthLong(locatorConnection.connection);

                                // Disconnect.
                                closeConnection(locatorConnection.connection);
                                locatorConnection.connection = null;
                            } else if (locatorConnection != null) {
                                if (locatorConnection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                                    isMediaUnAvailable = true;
                                }
                            }

                            // FIXME: get cache settings from server, honor them
                        } else if (scheme.equals("file") || scheme.equals("jar") || scheme.equals("jrt") || (scheme.equals("resource")) ) {
                            InputStream stream = getInputStream(uri);
                            stream.close();
                            isConnected = true;
                            // Try to get the content type based on extension
                            contentType = MediaUtils.filenameToContentType(uri);
                        }

                        if (isConnected) {
                            // Check whether content may be played.
                            // For WAV use file signature, since it can detect audio format
                            // and we can fail sooner, then doing it at runtime.
                            // This is important for AudioClip.
                            if (MediaUtils.CONTENT_TYPE_WAV.equals(contentType)) {
                                contentType = getContentTypeFromFileSignature(uri);
                                if (!MediaManager.canPlayContentType(contentType)) {
                                    isMediaSupported = false;
                                }
                            } else {
                                if (contentType == null || !MediaManager.canPlayContentType(contentType)) {
                                    // Try content based on file name.
                                    contentType = MediaUtils.filenameToContentType(uri);

                                    if (Locator.DEFAULT_CONTENT_TYPE.equals(contentType)) {
                                        // Try content based on file signature.
                                        contentType = getContentTypeFromFileSignature(uri);
                                    }

                                    if (!MediaManager.canPlayContentType(contentType)) {
                                        isMediaSupported = false;
                                    }
                                }
                            }

                            // Break as connection has been made and media type checked.
                            break;
                        }
                    } catch (IOException ioe) {
                        if (numConnectionAttempts + 1 >= MAX_CONNECTION_ATTEMPTS) {
                            throw ioe;
                        }
                    }

                    try {
                        Thread.sleep(CONNECTION_RETRY_INTERVAL);
                    } catch (InterruptedException ie) {
                        // Ignore it.
                    }
                }
            }
            else {
                // in case of iPod files we can be sure all files are supported
                contentType = MediaUtils.filenameToContentType(uri);
            }

            if (Logger.canLog(Logger.WARNING)) {
                if (contentType.equals(MediaUtils.CONTENT_TYPE_FLV)) {
                    Logger.logMsg(Logger.WARNING, "Support for FLV container and VP6 video is removed.");
                    throw new MediaException("media type not supported (" + uri.toString() + ")");
                } else if (contentType.equals(MediaUtils.CONTENT_TYPE_JFX)) {
                    Logger.logMsg(Logger.WARNING, "Support for FXM container and VP6 video is removed.");
                    throw new MediaException("media type not supported (" + uri.toString() + ")");
                }
            }

            // Check URI validity.
            if (!isIpod && !isConnected) {
                if (isMediaUnAvailable) {
                    throw new FileNotFoundException("media is unavailable (" + uri.toString() + ")");
                } else {
                    throw new IOException("could not connect to media (" + uri.toString() + ")");
                }
            } else if (!isMediaSupported) {
                throw new MediaException("media type not supported (" + uri.toString() + ")");
            }
        } catch (FileNotFoundException e) {
            throw e; // Just re-throw exception
        } catch (IOException e) {
            throw e; // Just re-throw exception
        } catch (MediaException e) {
            throw e; // Just re-throw exception
        } finally {
            readySignal.countDown();
        }
    }

    /**
     * Retrieves the content type describing the media content or
     * <code>"application/octet-stream"</code> if no more specific content type
     * may be detected.
     */
    public String getContentType() {
        try {
            readySignal.await();
        } catch (Exception e) {
        }
        return contentType;
    }

    /**
     * Retrieves the protocol of the media URL
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Retrieves the media size.
     *
     * @return size of the media file in bytes. -1 indicates unknown, which may
     * happen with network streams.
     */
    public long getContentLength() {
        try {
            readySignal.await();
        } catch (Exception e) {
        }
        return contentLength;
    }

    /**
     * Blocks until locator is ready (connection is established or failed).
     */
    public void waitForReadySignal() {
        try {
            readySignal.await();
        } catch (Exception e) {
        }
    }

    /**
     * Retrieves the associated
     * <code>URI</code>.
     *
     * @return The URI source.
     */
    public URI getURI() {
        return this.uri;
    }

    /**
     * Retrieves a string representation of the
     * <code>Locator</code>
     *
     * @return The
     * <code>LocatorURI</code> as a
     * <code>String</code>.
     */
    @Override
    public String toString() {
        if (LocatorCache.locatorCache().isCached(uri)) {
            return "{LocatorURI uri: " + uri.toString() + " (media cached)}";
        }
        return "{LocatorURI uri: " + uri.toString() + "}";
    }

    public String getStringLocation() {
        return uri.toString();
    }

    /**
     * Sets a property to be used by the connection to the media specified by
     * the URI. The meaning of the property is a function of the URI protocol
     * and type of media source. This method should be invoked <i>before</i>
     * calling {@link #createConnectionHolder()} or it will have no effect.
     *
     * @param property The name of the property.
     * @param value The value of the property.
     */
    public void setConnectionProperty(String property, Object value) {
        synchronized (propertyLock) {
            if (connectionProperties == null) {
                connectionProperties = new TreeMap<>();
            }

            connectionProperties.put(property, value);
        }
    }

    public ConnectionHolder createConnectionHolder() throws IOException {
        // check if it's cached
        if (null != cacheEntry) {
            if (Logger.canLog(Logger.DEBUG)) {
                Logger.logMsg(Logger.DEBUG, "Locator.createConnectionHolder: media cached, creating memory connection holder");
            }
            return ConnectionHolder.createMemoryConnectionHolder(cacheEntry.getBuffer());
        }

        // check if it is local file
        if ("file".equals(scheme)) {
            return ConnectionHolder.createFileConnectionHolder(uri);
        }

        // check if it is HTTP Live Streaming
        //    - uri path ends with .m3u8 or .m3u
        //    - contentType is "application/vnd.apple.mpegurl" or "audio/mpegurl"
        String uriPath = uri.getPath();
        if (uriPath != null && (uriPath.endsWith(".m3u8") ||
                                uriPath.endsWith(".m3u"))) {
            return ConnectionHolder.createHLSConnectionHolder(uri);
        }

        String type = getContentType(); // Should be ready by now
        if (type != null && (type.equals(MediaUtils.CONTENT_TYPE_M3U8) ||
                             type.equals(MediaUtils.CONTENT_TYPE_M3U))) {
            return ConnectionHolder.createHLSConnectionHolder(uri);
        }

        // media file over http/https
        synchronized  (propertyLock) {
            return ConnectionHolder.createURIConnectionHolder(uri, connectionProperties);
        }
    }

    private String getContentTypeFromFileSignature(URI uri) throws MalformedURLException, IOException {
        InputStream stream = getInputStream(uri);
        byte[] signature = new byte[MediaUtils.MAX_FILE_SIGNATURE_LENGTH];
        int size = stream.read(signature);
        stream.close();

        return MediaUtils.fileSignatureToContentType(signature, size);
    }

    static void closeConnection(URLConnection connection) {
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection httpConnection = (HttpURLConnection)connection;
            try {
                if (httpConnection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST &&
                    httpConnection.getInputStream() != null) {
                    httpConnection.getInputStream().close();
                }
            } catch (IOException ex) {
                try {
                    if (httpConnection.getErrorStream() != null) {
                        httpConnection.getErrorStream().close();
                    }
                } catch (IOException e) {}
            }
        }
    }
}
