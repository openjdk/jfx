/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tools.packager.bundlers;

import com.sun.javafx.tools.packager.Log;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class RelativeFileSet {
    private File basedir;
    Set<String> files = new HashSet<String>();

    public RelativeFileSet(File base, Set<File> files) {
        basedir = base;
        String baseAbsolute = basedir.getAbsolutePath();
        for (File f: files) {
            String absolute = f.getAbsolutePath();
            if (!absolute.startsWith(baseAbsolute)) {
                throw new RuntimeException("File " + f.getAbsolutePath() +
                        " does not belong to "+baseAbsolute);
            }
            if (!absolute.equals(baseAbsolute)) { //possible in javafxpackager case
               this.files.add(absolute.substring(baseAbsolute.length()+1));
            }
        }
    }

    public boolean contains(String[] requiredFiles) {
        boolean result = true;

        for(String fname: requiredFiles) {
            if (!files.contains(fname)) {
                Log.debug("  Runtime does not contain [" + fname + "]");
                result = false;
            }
        }

        return result;
    }

    public File getBaseDirectory() {
        return basedir;
    }

    public Set<String> getIncludedFiles() {
        return files;
    }

    public void dump() {
        Log.verbose("\n=========\nBasedir: " + basedir + "\n");
        for (String fname : files) {
            Log.verbose("  " + fname);
        }
        Log.verbose("\n========");
    }

}
