/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tools.resource;

import java.io.File;

public abstract class DetailedResourceTraversal implements ResourceTraversal {
    private String lastRelativePath;

    private boolean lastIsDirectory;

    public DetailedResourceTraversal() {
        lastRelativePath = "";
        lastIsDirectory = true;
    }

    public final boolean traverse(final PackagerResource rootResource,
                                  final File file,
                                  final String relativePath) {
        final boolean isDirectory = file.isDirectory();
        if (!traverseBetween(lastRelativePath, lastIsDirectory,
                             relativePath, isDirectory)) {
            return false;
        }

        lastRelativePath = relativePath;
        lastIsDirectory = isDirectory;

        return isDirectory ? true
                           : traverseFile(rootResource, file, relativePath);
    }

    public final void finish() {
        traverseBetween(lastRelativePath, lastIsDirectory, "", true);

        lastRelativePath = "";
        lastIsDirectory = true;
    }

    protected abstract boolean enterDirectory(String relativePath);

    protected abstract boolean exitDirectory(String relativePath);

    protected abstract boolean traverseFile(PackagerResource rootResource,
                                            File file,
                                            String relativePath);

    private boolean traverseBetween(final String relPath1,
                                    final boolean isDirectory1,
                                    final String relPath2,
                                    final boolean isDirectory2) {
        final int commonPathLength = getCommonPathLength(relPath1, relPath2);

        return traverseDown(relPath1, isDirectory1, commonPathLength)
                   && traverseUp(relPath2, isDirectory2, commonPathLength);
    }

    private boolean traverseDown(final String relPath,
                                 final boolean isDirectory,
                                 final int commonPathLength) {
        if (relPath.length() == commonPathLength) {
            return true;
        }

        if (isDirectory && !exitDirectory(relPath)) {
            return false;
        }

        int prevSeparator = findPrevSeparator(relPath, relPath.length() - 1,
                                              commonPathLength);

        while (prevSeparator > commonPathLength) {
            if (!exitDirectory(relPath.substring(0, prevSeparator))) {
                return false;
            }

            prevSeparator = findPrevSeparator(relPath, prevSeparator - 1,
                                              commonPathLength);
        }

        return true;
    }

    private boolean traverseUp(final String relPath,
                               final boolean isDirectory,
                               final int commonPathLength) {
        if (relPath.length() == commonPathLength) {
            return true;
        }

        final int pathLength = relPath.length();

        int nextSeparator = findNextSeparator(relPath, commonPathLength + 1,
                                              pathLength);

        while (nextSeparator < pathLength) {
            if (!enterDirectory(relPath.substring(0, nextSeparator))) {
                return false;
            }

            nextSeparator = findNextSeparator(relPath, nextSeparator + 1,
                                              pathLength);
        }

        if (isDirectory && !enterDirectory(relPath)) {
            return false;
        }

        return true;
    }

    private static int findPrevSeparator(final String relPath,
                                         final int fromIndex,
                                         final int minIndex) {
        final int prevSeparator = relPath.lastIndexOf('/', fromIndex);
        return (prevSeparator < minIndex) ? minIndex : prevSeparator;
    }

    private static int findNextSeparator(final String relPath,
                                         final int fromIndex,
                                         final int maxIndex) {
        final int nextSeparator = relPath.indexOf('/', fromIndex);
        return ((nextSeparator == -1) || (nextSeparator > maxIndex))
                       ? maxIndex : nextSeparator;
    }

    private static int getCommonPathLength(final String relPath1,
                                           final String relPath2) {
        final char[] path1Chars = relPath1.toCharArray();
        final char[] path2Chars = relPath2.toCharArray();

        int lastMatchIndex = 0;
        int i;
        for (i = 0; (i < path1Chars.length)
                        && (i < path2Chars.length)
                        && (path1Chars[i] == path2Chars[i]); ++i) {
            if (path1Chars[i] == '/') {
                lastMatchIndex = i;
            }
        }

        if (i == path1Chars.length) {
            if ((i == path2Chars.length) || (path2Chars[i] == '/')) {
                lastMatchIndex = i;
            }
        } else if ((i == path2Chars.length) && (path1Chars[i] == '/')) {
            lastMatchIndex = i;
        }

        return lastMatchIndex;
    }
}
