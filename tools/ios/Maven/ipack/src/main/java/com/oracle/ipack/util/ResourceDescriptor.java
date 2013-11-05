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

public final class ResourceDescriptor {
    private final File baseDir;
    private final File file;
    private final String relativePath;

    public ResourceDescriptor(final File baseDir, final String path) {
        this(baseDir, createFile(baseDir, path));
    }

    public ResourceDescriptor(final File baseDir, final File file) {
        final File nrmFile = normalizeFile(file);
        if (nrmFile == null) {
            throw new IllegalArgumentException("Invalid file specified");
        }

        if (baseDir != null) {
            final File nrmBaseDir = normalizeFile(baseDir);
            if (nrmBaseDir == null) {
                throw new IllegalArgumentException("Invalid basedir specified");
            }

            if (nrmFile.equals(nrmBaseDir)) {
                this.file = nrmFile;
                this.baseDir = nrmFile;
                this.relativePath = "";
                return;
            }

            final StringBuilder relativePathBuilder =
                    new StringBuilder(nrmFile.getName());

            File tempFile = nrmFile.getParentFile();
            while (tempFile != null) {
                if (tempFile.equals(nrmBaseDir)) {
                    this.file = nrmFile;
                    this.baseDir = nrmBaseDir;
                    this.relativePath = relativePathBuilder.toString();
                    return;
                }

                relativePathBuilder.insert(0, '/');
                relativePathBuilder.insert(0, tempFile.getName());
                tempFile = tempFile.getParentFile();
            }
        }

        final File nrmParentFile = nrmFile.getParentFile();

        this.file = nrmFile;
        this.baseDir = (nrmParentFile != null) ? nrmParentFile : nrmFile;
        this.relativePath = nrmFile.getName();
    }

    public File getBaseDir() {
        return baseDir;
    }

    public File getFile() {
        return file;
    }

    public String getRelativePath() {
        return relativePath;
    }

    private static File normalizeFile(final File inputFile) {
        return normalizeFileImpl(inputFile.getAbsoluteFile());
    }

    private static File normalizeFileImpl(final File inputFile) {
        if (inputFile.getParentFile() == null) {
            return inputFile;
        }

        final File partiallyNormalizedFile =
                normalizeFileImpl(inputFile.getParentFile());

        if (partiallyNormalizedFile == null) {
            // error
            return null;
        }

        final String fileName = inputFile.getName();

        if (fileName.equals(".")) {
            // ignore this path element
            return partiallyNormalizedFile;
        }

        if (fileName.equals("..")) {
            // remove the last path element
            return partiallyNormalizedFile.getParentFile();
        }

        return new File(partiallyNormalizedFile, fileName);
    }

    private static File createFile(final File baseDir, final String path) {
        final File testFile = new File(path);
        return testFile.isAbsolute()
                   ? testFile
                   : new File(baseDir == null
                                  ? null
                                  : baseDir.getAbsolutePath(),
                              path);
    }
}
