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

package com.oracle.ipack.packer;

import com.oracle.ipack.signer.Signer;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class Packer {
    private final ZipOutputStream zipStream;
    private final Signer signer;

    public Packer(final File destFile,
                  final Signer signer) throws FileNotFoundException {
        this.zipStream = new ZipOutputStream(
                             new BufferedOutputStream(
                                 new FileOutputStream(destFile)));
        this.signer = signer;
    }

    public void storeApplication(
            final File baseDir,
            final String appPath,
            final String appName,
            final String appIdentifier) throws IOException {
        final String normalizedAppPath = normalizePath(appPath);

        if (!normalizedAppPath.isEmpty()) {
            storeDirEntry(normalizedAppPath);
        }

        final ResourcePacker resourcePacker =
                new ResourcePacker(zipStream, baseDir, normalizedAppPath,
                                   appName);
        resourcePacker.execute();

        final ExecutablePacker executablePacker =
                new ExecutablePacker(zipStream, baseDir, normalizedAppPath,
                                     appName,
                                     appIdentifier,
                                     signer);

        executablePacker.setCodeResourcesHash(
                resourcePacker.getCodeResourcesHash());
        executablePacker.setInfoPlistHash(
                resourcePacker.getInfoPlistHash());
        executablePacker.execute();
    }

    public void close() {
        try {
            zipStream.close();
        } catch (final IOException e) {
            // ignore
        }
    }

    private void storeDirEntry(final String entryName) throws IOException {
        // TODO: intermediate dirs
        zipStream.putNextEntry(new ZipEntry(entryName));
        zipStream.closeEntry();
    }

    private static String normalizePath(final String path) {
        if (path.isEmpty() || path.endsWith("/")) {
            return path;
        }

        return path + '/';
    }
}
