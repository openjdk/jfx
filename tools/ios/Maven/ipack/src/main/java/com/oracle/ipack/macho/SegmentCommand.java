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

package com.oracle.ipack.macho;

import com.oracle.ipack.util.Util;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;

public final class SegmentCommand extends MachoCommand {
    private final ArrayList<Section> sections;

    private String segmentName;
    private int vmAddress;
    private int vmSize;
    private int fileOffset;
    private int fileSize;
    private int maxVmProtection;
    private int initVmProtection;
    private int flags;

    public SegmentCommand() {
        sections = new ArrayList<Section>();
    }

    @Override
    public int getId() {
        return LC_SEGMENT;
    }

    public Section findSection(final String sectionName) {
        for (final Section section: sections) {
            if (sectionName.equals(section.getSectionName())) {
                return section;
            }
        }

        return null;
    }

    public String getSegmentName() {
        return segmentName;
    }

    public void setSegmentName(final String segmentName) {
        this.segmentName = segmentName;
    }

    public int getVmAddress() {
        return vmAddress;
    }

    public void setVmAddress(final int vmAddress) {
        this.vmAddress = vmAddress;
    }

    public int getVmSize() {
        return vmSize;
    }

    public void setVmSize(final int vmSize) {
        this.vmSize = vmSize;
    }

    public int getFileOffset() {
        return fileOffset;
    }

    public void setFileOffset(final int fileOffset) {
        this.fileOffset = fileOffset;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(final int fileSize) {
        this.fileSize = fileSize;
    }

    public int getMaxVmProtection() {
        return maxVmProtection;
    }

    public void setMaxVmProtection(final int maxVmProtection) {
        this.maxVmProtection = maxVmProtection;
    }

    public int getInitVmProtection() {
        return initVmProtection;
    }

    public void setInitVmProtection(final int initVmProtection) {
        this.initVmProtection = initVmProtection;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(final int flags) {
        this.flags = flags;
    }

    @Override
    public String toString() {
        return "SegmentCommand { segmentName: \"" + segmentName + "\""
                       + ", vmAddress: 0x" + Util.hex32(vmAddress)
                       + ", vmSize: 0x" + Util.hex32(vmSize)
                       + ", fileOffset: 0x" + Util.hex32(fileOffset)
                       + ", fileSize: 0x" + Util.hex32(fileSize)
                       + ", maxVmProtection: " + maxVmProtection
                       + ", initVmProtection: " + initVmProtection
                       + ", flags: 0x" + Util.hex32(flags)
                       + ", sections: " + sections + " }";
    }

    @Override
    protected int getPayloadSize() {
        return 16 + 4 * 8 + 68 * sections.size();
    }

    @Override
    protected void readPayload(final DataInput dataInput) throws IOException {
        segmentName = Util.readString(dataInput, 16).trim();
        vmAddress = dataInput.readInt();
        vmSize = dataInput.readInt();
        fileOffset = dataInput.readInt();
        fileSize = dataInput.readInt();
        maxVmProtection = dataInput.readInt();
        initVmProtection = dataInput.readInt();
        final int numberOfSections = dataInput.readInt();
        flags = dataInput.readInt();

        sections.clear();
        sections.ensureCapacity(numberOfSections);
        for (int i = 0; i < numberOfSections; ++i) {
            final Section section = new Section();
            section.readImpl(dataInput);
            sections.add(section);
        }
    }

    @Override
    protected void writePayload(final DataOutput dataOutput)
            throws IOException {
        Util.writeString(dataOutput, segmentName, 16, '\0');
        dataOutput.writeInt(vmAddress);
        dataOutput.writeInt(vmSize);
        dataOutput.writeInt(fileOffset);
        dataOutput.writeInt(fileSize);
        dataOutput.writeInt(maxVmProtection);
        dataOutput.writeInt(initVmProtection);
        dataOutput.writeInt(sections.size());
        dataOutput.writeInt(flags);

        for (final Section section: sections) {
            section.write(dataOutput);
        }
    }

    public static final class Section {
        private String sectionName;
        private String segmentName;
        private int address;
        private int size;
        private int offset;
        private int align;
        private int relocationOffset;
        private int numberOfRelocations;
        private int flags;
        private int reserved1;
        private int reserved2;

        public String getSectionName() {
            return sectionName;
        }

        public void setSectionName(final String sectionName) {
            this.sectionName = sectionName;
        }

        public String getSegmentName() {
            return segmentName;
        }

        public void setSegmentName(final String segmentName) {
            this.segmentName = segmentName;
        }

        public int getAddress() {
            return address;
        }

        public void setAddress(final int address) {
            this.address = address;
        }

        public int getSize() {
            return size;
        }

        public void setSize(final int size) {
            this.size = size;
        }

        public int getOffset() {
            return offset;
        }

        public void setOffset(final int offset) {
            this.offset = offset;
        }

        public int getAlign() {
            return align;
        }

        public void setAlign(final int align) {
            this.align = align;
        }

        public int getRelocationOffset() {
            return relocationOffset;
        }

        public void setRelocationOffset(final int relocationOffset) {
            this.relocationOffset = relocationOffset;
        }

        public int getNumberOfRelocations() {
            return numberOfRelocations;
        }

        public void setNumberOfRelocations(final int numberOfRelocations) {
            this.numberOfRelocations = numberOfRelocations;
        }

        public int getFlags() {
            return flags;
        }

        public void setFlags(final int flags) {
            this.flags = flags;
        }

        public int getReserved1() {
            return reserved1;
        }

        public void setReserved1(final int reserved1) {
            this.reserved1 = reserved1;
        }

        public int getReserved2() {
            return reserved2;
        }

        public void setReserved2(final int reserved2) {
            this.reserved2 = reserved2;
        }

        public void write(final DataOutput dataOutput) throws IOException {
            Util.writeString(dataOutput, sectionName, 16, '\0');
            Util.writeString(dataOutput, segmentName, 16, '\0');
            dataOutput.writeInt(address);
            dataOutput.writeInt(size);
            dataOutput.writeInt(offset);
            dataOutput.writeInt(align);
            dataOutput.writeInt(relocationOffset);
            dataOutput.writeInt(numberOfRelocations);
            dataOutput.writeInt(flags);
            dataOutput.writeInt(reserved1);
            dataOutput.writeInt(reserved2);
        }

        private void readImpl(final DataInput dataInput) throws IOException {
            sectionName = Util.readString(dataInput, 16).trim();
            segmentName = Util.readString(dataInput, 16).trim();
            address = dataInput.readInt();
            size = dataInput.readInt();
            offset = dataInput.readInt();
            align = dataInput.readInt();
            relocationOffset = dataInput.readInt();
            numberOfRelocations = dataInput.readInt();
            flags = dataInput.readInt();
            reserved1 = dataInput.readInt();
            reserved2 = dataInput.readInt();
        }

        @Override
        public String toString() {
            return "Section { sectionName: \"" + sectionName + "\""
                             + ", segmentName: \"" + segmentName + "\""
                             + ", address: 0x" + Util.hex32(address)
                             + ", size: 0x" + Util.hex32(size)
                             + ", offset: 0x" + Util.hex32(offset)
                             + ", align: " + align
                             + ", relocationOffset: 0x"
                                     + Util.hex32(relocationOffset)
                             + ", numberOfRelocations: " + numberOfRelocations
                             + ", flags: 0x" + Util.hex32(flags)
                             + ", reserved1: " + reserved1
                             + ", reserved2: " + reserved2 + " }";
        }
    }
}
