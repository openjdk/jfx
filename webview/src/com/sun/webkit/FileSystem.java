/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit;

import java.io.File;

/**
 * The implementation of the file system-related WebKit callbacks.
 */
final class FileSystem {
    
    /**
     * The private default constructor. Ensures non-instantiability.
     */
    private FileSystem() {
        throw new AssertionError();
    }
    

    /**
     * Given a file path, returns the name of the file.
     */
    private static String fwkPathGetFileName(String path) {
        return new File(path).getName();
    }
}
