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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public abstract class MachoCommand {
    public static final int LC_SEGMENT = 0x1;
    public static final int LC_CODE_SIGNATURE = 0x1d;

    public abstract int getId();

    public final int getSize() {
        return 8 + getPayloadSize();
    }

    public static MachoCommand read(final DataInput dataInput)
            throws IOException {
        final int commandId = dataInput.readInt();
        final int commandSize = dataInput.readInt();
        MachoCommand command;
        switch (commandId) {
            case LC_SEGMENT:
                command = new SegmentCommand();
                break;
            case LC_CODE_SIGNATURE:
                command = new CodeSignatureCommand();
                break;
            default:
                command = new UnknownCommand(commandId, commandSize - 8);
                break;
        }

        command.readPayload(dataInput);
        if (command.getSize() != commandSize) {
            throw new IOException("Can't decode command in mach-o header");
        }

        return command;
    }

    public final void write(final DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(getId());
        dataOutput.writeInt(getSize());
        writePayload(dataOutput);
    }

    protected abstract int getPayloadSize();

    protected abstract void readPayload(final DataInput dataInput)
            throws IOException;

    protected abstract void writePayload(final DataOutput dataOutput)
            throws IOException;
}
