/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit;

import java.io.File;
import java.io.IOException;
import static java.lang.String.format;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

final class FileSystem {

    private final static Logger logger =
            Logger.getLogger(FileSystem.class.getName());


    private FileSystem() {
        throw new AssertionError();
    }

    private static boolean fwkFileExists(String path) {
        return new File(path).exists();
    }

    private static long fwkGetFileSize(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                return file.length();
            }
        } catch (SecurityException ex) {
            logger.log(Level.FINE, format("Error determining "
                    + "size of file [%s]", path), ex);
        }
        return -1;
    }

    private static String fwkPathByAppendingComponent(String path,
                                                      String component)
    {
        return new File(path, component).getPath();
    }

    private static boolean fwkMakeAllDirectories(String path) {
        try {
            Files.createDirectories(Paths.get(path));
            return true;
        } catch (InvalidPathException|IOException ex) {
            logger.log(Level.FINE, format("Error creating "
                    + "directory [%s]", path), ex);
            return false;
        }
    }

    private static String fwkPathGetFileName(String path) {
        return new File(path).getName();
    }
}
