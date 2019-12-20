/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

/**
 * A pool of byte buffers that can be shared by multiple concurrent
 * clients.
 */
final class ByteBufferPool {

    /**
     * The shared collection of byte buffers.
     */
    private final Queue<ByteBuffer> byteBuffers =
            new ConcurrentLinkedQueue<ByteBuffer>();

    /**
     * The size of each byte buffer.
     */
    private final int bufferSize;


    /**
     * Creates a new pool.
     */
    private ByteBufferPool(int bufferSize) {
        this.bufferSize = bufferSize;
    }


    /**
     * Creates a new pool.
     */
    static ByteBufferPool newInstance(int bufferSize) {
        return new ByteBufferPool(bufferSize);
    }

    /**
     * Creates a new allocator associated with this pool.
     * The allocator will allow its client to allocate and release
     * buffers and will ensure that there are no more than
     * {@code maxBufferCount} buffers allocated through this allocator
     * at any given time moment.
     */
    ByteBufferAllocator newAllocator(int maxBufferCount) {
        return new ByteBufferAllocatorImpl(maxBufferCount);
    }

    /**
     * The allocator implementation.
     */
    private final class ByteBufferAllocatorImpl implements ByteBufferAllocator {

        /**
         * The semaphore used to limit the number of buffers
         * allocated through this allocator.
         */
        private final Semaphore semaphore;


        /**
         * Creates a new allocator.
         */
        private ByteBufferAllocatorImpl(int maxBufferCount) {
            semaphore = new Semaphore(maxBufferCount);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public ByteBuffer allocate() throws InterruptedException {
            semaphore.acquire();
            ByteBuffer byteBuffer = byteBuffers.poll();
            if (byteBuffer == null) {
                byteBuffer = ByteBuffer.allocateDirect(bufferSize);
            }
            return byteBuffer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void release(ByteBuffer byteBuffer) {
            byteBuffer.clear();
            byteBuffers.add(byteBuffer);
            semaphore.release();
        }
    }
}

/**
 * An object that can allocate and release byte buffers.
 */
interface ByteBufferAllocator {

    /**
     * Allocates a byte buffer.
     */
    ByteBuffer allocate() throws InterruptedException;

    /**
     * Releases a byte buffer.
     */
    void release(ByteBuffer byteBuffer);
}
