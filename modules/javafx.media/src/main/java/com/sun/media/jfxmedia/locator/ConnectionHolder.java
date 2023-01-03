/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;

/**
 * Connection holders hold and maintain connection do different kinds of sources
 *
 */
public abstract class ConnectionHolder {
    private static int DEFAULT_BUFFER_SIZE = 4096;

    ReadableByteChannel channel;
    ByteBuffer          buffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE);

    static ConnectionHolder createMemoryConnectionHolder(ByteBuffer buffer) {
        return new MemoryConnectionHolder(buffer);
    }

    static ConnectionHolder createURIConnectionHolder(URI uri, Map<String,Object> connectionProperties) throws IOException {
        return new URIConnectionHolder(uri, connectionProperties);
    }

    static ConnectionHolder createFileConnectionHolder(URI uri) throws IOException {
        return new FileConnectionHolder(uri);
    }

    static ConnectionHolder createHLSConnectionHolder(URI uri) {
        return new HLSConnectionHolder(uri);
    }

    /**
     * Reads a block of data from the current position of the opened stream.
     *
     * @return The number of bytes read, possibly zero, or -1 if the channel
     * has reached end-of-stream.
     *
     * @throws ClosedChannelException if an attempt is made to read after
     * closeConnection has been called
     */
    public int readNextBlock() throws IOException {
        buffer.rewind();
        if (buffer.limit() < buffer.capacity()) {
            buffer.limit(buffer.capacity());
        }
        // avoid NPE if channel does not exist or has been closed
        if (null == channel) {
            throw new ClosedChannelException();
        }
        return channel.read(buffer);
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    /**
     * Reads a block of data from the arbitrary position of the opened stream.
     *
     * @return The number of bytes read, possibly zero, or -1 if the given position
     * is greater than or equal to the file's current size.
     *
     * @throws ClosedChannelException if an attempt is made to read after
     * closeConnection has been called
     */
    abstract int readBlock(long position, int size) throws IOException;

    /**
     * Detects whether this source needs buffering at the pipeline level.
     * When true the pipeline contains progressbuffer after the source.
     *
     * @return true if the source needs a buffer, false otherwise.
     */
    abstract boolean needBuffer();

    /**
     * Detects whether the source is seekable.
     * @return true if the source is seekable, false otherwise.
     */
    abstract boolean isSeekable();

    /**
     * Detects whether the source is a random access source. If the method returns
     * true then the source is capable of working in pull mode. To be able to work
     * in pull mode holder must provide implementation.
     * @return true is the source is random access, false otherwise.
     */
    abstract boolean isRandomAccess();

    /**
     * Performs a seek request to the desired position.
     *
     * @return -1 if the seek request failed or new stream position
     */
    public abstract long seek(long position);

    /**
     * Closes connection when done.
     * Overriding methods should call this method in the beginning of their implementation.
     */
    public void closeConnection() {
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (IOException ioex) {}
        finally {
            channel = null;
        }
    }

    /**
     * Get or set properties.
     *
     * @param prop - Property ID.
     * @param value - Depends on property ID.
     * @return - Depends on property ID.
     */
    int property(int prop, int value) {
        return 0;
    }

    private static class FileConnectionHolder extends ConnectionHolder {
        private RandomAccessFile file = null;

        FileConnectionHolder(URI uri) throws IOException {
            channel = openFile(uri);
        }

        @Override
        boolean needBuffer() {
            return false;
        }

        @Override
        boolean isRandomAccess() {
            return true;
        }

        @Override
        boolean isSeekable() {
            return true;
        }

        @Override
        public long seek(long position) {
            try {
                ((FileChannel)channel).position(position);
                return position;
            } catch(IOException ioex) {
                return -1;
            }
        }

        @Override
        int readBlock(long position, int size) throws IOException {
            if (null == channel) {
                throw new ClosedChannelException();
            }

            if (buffer.capacity() < size) {
                buffer = ByteBuffer.allocateDirect(size);
            }
            buffer.rewind().limit(size);
            return ((FileChannel)channel).read(buffer, position);
        }

        private ReadableByteChannel openFile(final URI uri) throws IOException {
            if (file != null) {
                file.close();
            }

            file = new RandomAccessFile(new File(uri), "r");
            return file.getChannel();
        }

        @Override
        public void closeConnection() {
            super.closeConnection();

            if (file != null) {
                try {
                    file.close();
                } catch (IOException ex) {
                } finally {
                    file = null;
                }
            }
        }
    }

    private static class URIConnectionHolder extends ConnectionHolder {
        private URI                 uri;
        private URLConnection       urlConnection;

        URIConnectionHolder(URI uri, Map<String,Object> connectionProperties) throws IOException {
            this.uri = uri;
            urlConnection = uri.toURL().openConnection();
            if (connectionProperties != null) {
                for(Map.Entry<String,Object> entry : connectionProperties.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        urlConnection.setRequestProperty(entry.getKey(), (String)value);
                    }
                }
            }
            channel = openChannel(null);
        }

        @Override
        boolean needBuffer() {
            String scheme = uri.getScheme().toLowerCase();
            return ("http".equals(scheme) || "https".equals(scheme));
        }

        @Override
        boolean isSeekable() {
            return (urlConnection instanceof HttpURLConnection) ||
                   (urlConnection instanceof JarURLConnection) ||
                   isJRT() || isResource();
        }

        @Override
        boolean isRandomAccess() {
            return false;
        }

        @Override
        int readBlock(long position, int size) throws IOException {
            throw new IOException();
        }

        @Override
        public long seek(long position) {
            if (urlConnection instanceof HttpURLConnection) {
                URLConnection tmpURLConnection = null;

                //closeConnection();
                try{
                    tmpURLConnection = uri.toURL().openConnection();

                    HttpURLConnection httpConnection = (HttpURLConnection)tmpURLConnection;
                    httpConnection.setRequestMethod("GET");
                    httpConnection.setUseCaches(false);
                    httpConnection.setRequestProperty("Range", "bytes=" + position + "-");
                    // If range request worked properly we should get responce code 206 (HTTP_PARTIAL)
                    // Else fail seek and let progressbuffer to download all data. It is pointless for us to download it and throw away.
                    if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_PARTIAL) {
                        closeConnection();
                        urlConnection = tmpURLConnection;
                        tmpURLConnection = null;
                        channel = openChannel(null);
                        return position;
                    } else {
                        return -1;
                    }
                } catch (IOException ioex) {
                    return -1;
                } finally {
                    if (tmpURLConnection != null) {
                        Locator.closeConnection(tmpURLConnection);
                    }
                }
            } else if ((urlConnection instanceof JarURLConnection) || isJRT() || isResource()) {
                try {
                    closeConnection();

                    urlConnection = uri.toURL().openConnection();

                    // Skip data that we do not need
                    long skip_left = position;
                    InputStream inputStream = urlConnection.getInputStream();
                    do {
                        long skip = inputStream.skip(skip_left);
                        skip_left -= skip;
                    } while (skip_left > 0);

                    channel = openChannel(inputStream);

                    return position;
                } catch (IOException ioex) {
                    return -1;
                }
            }

            return -1;
        }

        @Override
        public void closeConnection() {
            super.closeConnection();

            Locator.closeConnection(urlConnection);
            urlConnection = null;
        }

        private ReadableByteChannel openChannel(InputStream inputStream) throws IOException {
            return (inputStream == null) ?
                    Channels.newChannel(urlConnection.getInputStream()) :
                    Channels.newChannel(inputStream);
        }

        private boolean isJRT() {
            String scheme = uri.getScheme().toLowerCase();
            return "jrt".equals(scheme);
        }

        private boolean isResource() {
            String scheme = uri.getScheme().toLowerCase();
            return "resource".equals(scheme);
        }

    }

    // A "ConnectionHolder" that "reads" from a ByteBuffer, generally loaded from
    // some unsupported or buggy source
    private static class MemoryConnectionHolder extends ConnectionHolder {
        private final ByteBuffer backingBuffer;

        public MemoryConnectionHolder(ByteBuffer buf) {
            if (null == buf) {
                throw new IllegalArgumentException("Can't connect to null buffer...");
            }

            if (buf.isDirect()) {
                // we can use it, or rather a duplicate directly
                backingBuffer = buf.duplicate();
            } else {
                // operate on a copy of the buffer
                backingBuffer = ByteBuffer.allocateDirect(buf.capacity());
                backingBuffer.put(buf);
            }

            // rewind since the default position is expected to be at zero
            backingBuffer.rewind();

            // readNextBlock should never be called since we're random access
            // but just to be safe (and for unit tests...)
            channel = new ReadableByteChannel() {
                @Override
                public int read(ByteBuffer bb) throws IOException {
                    if (backingBuffer.remaining() <= 0) {
                        return -1; // EOS
                    }

                    int actual;
                    if (bb.equals(buffer)) {
                        // we'll cheat here as we know that bb is buffer and rather
                        // than copy the data, just slice it like for readBlock
                        actual = Math.min(DEFAULT_BUFFER_SIZE, backingBuffer.remaining());
                        if (actual > 0) {
                            buffer = backingBuffer.slice();
                            buffer.limit(actual);
                        }
                    } else {
                        actual = Math.min(bb.remaining(), backingBuffer.remaining());
                        if (actual > 0) {
                            backingBuffer.limit(backingBuffer.position() + actual);
                            bb.put(backingBuffer);
                            backingBuffer.limit(backingBuffer.capacity());
                        }
                    }
                    return actual;
                }

                @Override
                public boolean isOpen() {
                    return true; // open 24/7/365
                }

                @Override
                public void close() throws IOException {
                    // never closed...
                }
            };
        }

        @Override
        int readBlock(long position, int size) throws IOException {
            // mimic stream behavior
            if (null == channel) {
                throw new ClosedChannelException();
            }

            if ((int)position > backingBuffer.capacity()) {
                return -1; //EOS
            }
            backingBuffer.position((int)position);

            buffer = backingBuffer.slice();

            int actual = Math.min(backingBuffer.remaining(), size);
            buffer.limit(actual); // only give as much as asked
            backingBuffer.position(backingBuffer.position() + actual);

            return actual;
        }

        @Override
        boolean needBuffer() {
            return false;
        }

        @Override
        boolean isSeekable() {
            return true;
        }

        @Override
        boolean isRandomAccess() {
            return true;
        }

        @Override
        public long seek(long position) {
            if ((int)position < backingBuffer.capacity()) {
                backingBuffer.limit(backingBuffer.capacity());
                backingBuffer.position((int)position);
                return position;
            }
            return -1;
        }

        @Override
        public void closeConnection() {
            // more stream behavior mimicry
            channel = null;
        }
    }
}
