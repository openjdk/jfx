/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
