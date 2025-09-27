/*
 * Copyright (c) 2008, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.runtime.async;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Objects;

/**
 * Abstract base class for representing remote resources identified by a URL.  Subclasses may plug in arbitrary
 * post-processing on the stream to turn it into the desired result.  Manages progress indication if the remote resource
 * provides a content-length header.
 *
 */
public abstract class AbstractRemoteResource<T> extends AbstractAsyncOperation<T> {

    /**
     * @param stream an {@link InputStream}, cannot be {@code null}
     * @param size the size of the stream, or -1 if unknown
     */
    public record SizedStream(InputStream stream, long size) {}

    /**
     * An interface to provide a stream with a known (or unknown) size that
     * allows {@link IOException} to be thrown.
     */
    public interface SizedStreamSupplier {
        SizedStream get() throws IOException;
    }

    private final SizedStreamSupplier sizedStreamSupplier;

    protected AbstractRemoteResource(SizedStreamSupplier sizedStreamSupplier, AsyncOperationListener<T> listener) {
        super(listener);

        this.sizedStreamSupplier = Objects.requireNonNull(sizedStreamSupplier, "sizedStreamSupplier");
    }

    protected abstract T processStream(InputStream stream);

    @Override
    public T call() throws IOException {
        SizedStream sizedStream = sizedStreamSupplier.get();

        setProgressMax(sizedStream.size);

        try (ProgressInputStream stream = new ProgressInputStream(sizedStream.stream)) {
            return processStream(stream);
        }
    }

    protected class ProgressInputStream extends BufferedInputStream {
        public ProgressInputStream(InputStream in) {
            super(in);
        }

        @Override
        public synchronized int read() throws IOException {
            if (Thread.currentThread().isInterrupted())
                throw new InterruptedIOException();
            int ch = super.read();
            addProgress(1);
            return ch;
        }

        @Override
        public synchronized int read(byte b[], int off, int len) throws IOException {
            if (Thread.currentThread().isInterrupted())
                throw new InterruptedIOException();
            int bytes = super.read(b, off, len);
            addProgress(bytes);
            return bytes;
        }

        @Override
        public int read(byte b[]) throws IOException {
            if (Thread.currentThread().isInterrupted())
                throw new InterruptedIOException();
            int bytes = super.read(b);
            addProgress(bytes);
            return bytes;
        }
    }
}
