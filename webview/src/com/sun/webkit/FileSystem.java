/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit;

import java.io.File;
import static java.lang.String.format;
import java.util.logging.Level;
import java.util.logging.Logger;

final class FileSystem {

    private final static Logger logger =
            Logger.getLogger(FileSystem.class.getName());


    private FileSystem() {
        throw new AssertionError();
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

    private static String fwkPathGetFileName(String path) {
        return new File(path).getName();
    }
}
