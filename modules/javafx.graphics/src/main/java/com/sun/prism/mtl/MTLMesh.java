/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.mtl;

import com.sun.prism.impl.BaseMesh;
import com.sun.prism.impl.Disposer;

class MTLMesh extends BaseMesh {

    static int count = 0;

    private final MTLContext context;
    private final long nativeHandle;

    private MTLMesh(MTLContext context, long nativeHandle, Disposer.Record disposerRecord) {
        super(disposerRecord);
        this.context = context;
        this.nativeHandle = nativeHandle;
        count++;
    }

    static MTLMesh create(MTLContext context) {
        long nativeHandle = context.createMTLMesh();
        return new MTLMesh(context, nativeHandle, new MTLMeshDisposerRecord(context, nativeHandle));
    }

    long getNativeHandle() {
        return nativeHandle;
    }

    @Override
    public boolean isValid() {
        return !context.isDisposed();
    }

    @Override
    public void dispose() {
        disposerRecord.dispose();
        count--;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public boolean buildNativeGeometry(float[] vertexBuffer, int vertexBufferLength,
            int[] indexBufferInt, int indexBufferLength) {
        return context.buildNativeGeometry(nativeHandle, vertexBuffer,
                vertexBufferLength, indexBufferInt, indexBufferLength);
    }

    @Override
    public boolean buildNativeGeometry(float[] vertexBuffer, int vertexBufferLength,
            short[] indexBufferShort, int indexBufferLength) {
        return context.buildNativeGeometry(nativeHandle, vertexBuffer,
                vertexBufferLength, indexBufferShort, indexBufferLength);
    }

    private static class MTLMeshDisposerRecord implements Disposer.Record {

        private final MTLContext context;
        private long nativeHandle;

        MTLMeshDisposerRecord(MTLContext context, long nativeHandle) {
            this.context = context;
            this.nativeHandle = nativeHandle;
        }

        void traceDispose() {}

        @Override
        public void dispose() {
            if (nativeHandle != 0L && !context.isDisposed()) {
                traceDispose();
                context.releaseMTLMesh(nativeHandle);
                nativeHandle = 0L;
            }
        }
    }
}
