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

package com.oracle.ipack.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class DataCopier {
    private final byte[] buffer;

    public DataCopier() {
        buffer = new byte[65536];
    }

    public void copyFile(final OutputStream os, final File file)
            throws IOException {
        final InputStream is = new FileInputStream(file);
        try {
            copyStream(os, is);
        } finally {
            try {
                is.close();
            } catch (final IOException e) {
                // ignore
            }
        }
    }

    public void copyStream(final OutputStream os, final InputStream is)
            throws IOException {
        int read;
        while ((read = is.read(buffer)) != -1) {
            os.write(buffer, 0, read);
        }
    }

    public void copyStream(final OutputStream os,
                           final InputStream is,
                           final int limit) throws IOException {
        int remaining = limit;
        while (remaining > 0) {
            final int chunkSize = (remaining < buffer.length)
                                          ? remaining
                                          : buffer.length;
            final int read = is.read(buffer, 0, chunkSize);
            if (read == -1) {
                // end of stream
                return;
            }

            os.write(buffer, 0, read);
            remaining -= read;
        }
    }
}
