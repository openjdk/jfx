/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package fxslideshow;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * A class to manage a list of files, recursively collecting them, stepping
 * through them.
 */
public class Files {

    private final static Logger LOGGER = Logger.getLogger(Files.class.getName());
    ArrayList<String> theFiles = new ArrayList(500);
    int prev = -1;
    int current = 0;
    int next = 1;
    private boolean loop = false;

    public Files() {
        theFiles = new ArrayList(500);
        current = 0;
    }

    /**
     * Get the number of file entries
     * @return count
     */
    public int count() {
        return theFiles.size();
    }

    /**
     * Adds the provided path recursively to the file list.
     * @param path the file path.
     */
    public void add(String path) {

        File f = new File(path);

        if (f.exists()) {
            if (f.isFile() && f.length() > 0) {
                LOGGER.finer("found file: \"" + path + "\"");
                theFiles.add(path);
            } else if (f.isDirectory()) {
                LOGGER.finest("process direcory: \"" + path + "\"");
                for (String p : f.list()) {
                    add(path + File.separator + p);
                }
            }
        }
    }

    /**
     * get the current file path.
     * @return file path
     */
    public String currentFilePath() {
        LOGGER.finest("currentFilePath[" + (current) + "]=" + theFiles.get(current));
        return theFiles.get(current);
    }

    /**
     * get the next file path in the sequence
     * @return file path
     */
    public String nextFilePath() {
        if (next >= 0) {
            LOGGER.finest("nextFilePath[" + (next) + "]=" + theFiles.get(next));
            return theFiles.get(next);
        }
        return null;
    }

    /**
     * get the previous file path in the sequence
     * @return file path
     */
    public String prevFilePath() {
        if (prev >= 0) {
            LOGGER.finest("prevFilePath[" + (prev) + "]=" + theFiles.get(prev));
            return theFiles.get(prev);
        }
        return null;
    }

    private int getPrev() {
        int p = current - 1;
        if (p < 0) {
            if (loop) {
                p = theFiles.size() -1;
            } else {
                p =  -1;
            }
        }
        return p;
    }

    private int getNext() {
        int n = current + 1;
        if (n >= theFiles.size()) {
            if (loop) {
                n = 0;
            } else {
                n =  -1;
            }
        }
        return n;
    }

    /**
     * Move forward one file
     * @return false for end of list
     */
    public boolean next() {
        if (current + 1 < theFiles.size()) {
            current++;
        } else if (loop) {
            current = 0;
        } else {
            return false;
        }
        next = getNext();
        prev = getPrev();

        return true;
    }

    /**
     * Back up one file
     * @return false for end of list
     */
    public boolean previous() {
        if (current > 0) {
            current--;
        } else if (loop) {
            current = theFiles.size() -1;
        } else {
            return false;
        }

        next = getNext();
        prev = getPrev();

        return true;
    }

    /**
     * Randomize our file list
     */
    public void randomize() {
        Collections.shuffle(theFiles);
    }

    /**
     * Treat this list as a loop
     * @param loop true for endless loop
     */
    public void setLoop(boolean loop) {
        this.loop = loop;
        prev = getPrev();
        next = getNext();
    }

    public void debugList() {
        LOGGER.finer("The file list");
        for (String s : theFiles) {
            LOGGER.finer(s);
        }
    }
}
