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

package com.oracle.ipack.signature;

import com.oracle.ipack.blobs.Blob;
import java.io.DataOutput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public final class CodeDirectoryBlob extends Blob {
    private final byte[] identifierBytes;

    private final int numberOfSpecialSlots;
    private final int numberOfCodeSlots;
    private final byte[] specialSlots;
    private final byte[] codeSlots;

    private final int codeLimit;
    private final int hashSize;
    private final int hashType;
    private final int pageSize;

    private int flags;

    public CodeDirectoryBlob(final String identifier,
                             final int codeLimit) {
        this(identifier, codeLimit, 3, 4096, 20, 1);
    }

    public CodeDirectoryBlob(final String identifier,
                             final int codeLimit,
                             final int numberOfSpecialSlots,
                             final int pageSize,
                             final int hashSize,
                             final int hashType) {
        this.identifierBytes = identifierBytes(identifier);

        this.numberOfSpecialSlots = numberOfSpecialSlots;
        this.numberOfCodeSlots = (codeLimit + pageSize - 1) / pageSize;

        this.specialSlots = new byte[numberOfSpecialSlots * hashSize];
        this.codeSlots = new byte[numberOfCodeSlots * hashSize];

        this.codeLimit = codeLimit;
        this.hashSize = hashSize;
        this.hashType = hashType;
        this.pageSize = pageSize;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(final int flags) {
        this.flags = flags;
    }

    public void setInfoPlistSlot(final byte[] hash) {
        setSpecialSlot(SpecialSlotConstants.CD_INFO_SLOT, hash);
    }

    public void setRequirementsSlot(final byte[] hash) {
        setSpecialSlot(SpecialSlotConstants.CD_REQUIREMENTS_SLOT, hash);
    }

    public void setCodeResourcesSlot(final byte[] hash) {
        setSpecialSlot(SpecialSlotConstants.CD_RESOURCE_DIR_SLOT, hash);
    }

    public void setSpecialSlot(final int index, final byte[] hash) {
        System.arraycopy(hash, 0, specialSlots,
                         specialSlots.length - index * hashSize,
                         hashSize);
    }

    public void setCodeSlot(final int index, final byte[] hash) {
        System.arraycopy(hash, 0, codeSlots, index * hashSize, hashSize);
    }

    @Override
    protected int getMagic() {
        return 0xfade0c02;
    }

    @Override
    protected int getPayloadSize() {
        return 4 * 10 + identifierBytes.length
                      + specialSlots.length
                      + codeSlots.length;
    }

    @Override
    protected void writePayload(final DataOutput dataOutput)
            throws IOException {
        final int identOffset = 8 + 4 * 10;
        final int hashOffset = identOffset + identifierBytes.length
                                           + specialSlots.length;
        final int pageSizeShift = 31 - Integer.numberOfLeadingZeros(pageSize);

        dataOutput.writeInt(0x20100); // version
        dataOutput.writeInt(flags);
        dataOutput.writeInt(hashOffset);
        dataOutput.writeInt(identOffset);
        dataOutput.writeInt(numberOfSpecialSlots);
        dataOutput.writeInt(numberOfCodeSlots);
        dataOutput.writeInt(codeLimit);
        dataOutput.writeByte(hashSize);
        dataOutput.writeByte(hashType);
        dataOutput.writeByte(0); // spare1
        dataOutput.writeByte(pageSizeShift);
        dataOutput.writeInt(0); // spare2
        dataOutput.writeInt(0); // scatterOffset

        dataOutput.write(identifierBytes);
        dataOutput.write(specialSlots);
        dataOutput.write(codeSlots);
    }

    private byte[] identifierBytes(final String identifier) {
        try {
            return (identifier + '\0').getBytes("UTF-8");
        } catch (final UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
