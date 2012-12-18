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
package com.sun.javafx.tools.ant;

import com.sun.javafx.tools.packager.CreateBSSParams;
import com.sun.javafx.tools.packager.PackagerLib;
import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * Converts set of CSS files into binary form (BSS).
 *
 * @ant.task name="CSSToBin" category="javafx"
 */
public class CSSToBinTask extends Task {

    private PackagerLib packager;
    private CreateBSSParams createBSSParams;

    private FileSet fileset;
    private String outdir;

    private boolean verbose = false;
    public void setVerbose(boolean v) {
        verbose = v;
    }

    public FileSet createFileset() {
        fileset = new FileSet();
        return fileset;
    }

    /**
     * Output directory.
     *
     * @ant.required
     */
    public void setOutdir(String outdir) {
        this.outdir = outdir;
    }

    public CSSToBinTask() {
        packager = new PackagerLib();
        createBSSParams = new CreateBSSParams();
    }

    @Override
    public void execute() {
        Utils.addResources(createBSSParams, fileset);

//        packager.setBinCssFile(binCssFile);
        createBSSParams.setOutdir(new File(outdir));

        try {
            packager.generateBSS(createBSSParams);
        } catch (Exception e) {
            throw new BuildException(e.getMessage(), e);
        }
    }
}
