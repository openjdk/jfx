/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.ipack.blobs;

import java.io.DataOutput;
import java.io.IOException;

public abstract class SuperBlob<T extends Blob> extends Blob {
    private final SubBlob<T>[] subBlobs;

    protected SuperBlob(final int numberOfSubBlobs) {
        this.subBlobs = allocateSubBlobs(numberOfSubBlobs);
    }

    public final T getSubBlob(final int index) {
        return subBlobs[index].getBlob();
    }

    public final void setSubBlob(final int index, final int type,
                                 final T blob) {
        subBlobs[index].setType(type);
        subBlobs[index].setBlob(blob);
    }

    @Override
    protected final int getPayloadSize() {
        int size = 4 + subBlobs.length * 8;
        for (int i = 0; i < subBlobs.length; ++i) {
            size += subBlobs[i].getBlob().getSize();
        }

        return size;
    }

    @Override
    protected final void writePayload(final DataOutput dataOutput)
            throws IOException {
        dataOutput.writeInt(subBlobs.length);
        int offset = 8 + 4 + subBlobs.length * 8;
        for (int i = 0; i < subBlobs.length; ++i) {
            dataOutput.writeInt(subBlobs[i].getType());
            dataOutput.writeInt(offset);

            offset += subBlobs[i].getBlob().getSize();
        }

        for (int i = 0; i < subBlobs.length; ++i) {
            subBlobs[i].getBlob().write(dataOutput);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Blob> SubBlob<T>[] allocateSubBlobs(
            final int numberOfSubBlobs) {
        final SubBlob<T>[] subBlobs = new SubBlob[numberOfSubBlobs];
        for (int i = 0; i < numberOfSubBlobs; ++i) {
            subBlobs[i] = new SubBlob<T>();
        }

        return subBlobs;
    }

    private static final class SubBlob<T extends Blob> {
        private int type;
        private T blob;

        public int getType() {
            return type;
        }

        public void setType(final int type) {
            this.type = type;
        }

        public T getBlob() {
            return blob;
        }

        public void setBlob(final T blob) {
            this.blob = blob;
        }
    }
}
