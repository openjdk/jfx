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

import com.sun.javafx.tools.packager.CreateJarParams;
import com.sun.javafx.tools.packager.PackagerLib;
import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Manifest;
import org.apache.tools.ant.types.FileSet;

/**
 * Package javafx application into jar file. The set of files to be included is
 * defined by one or more nested fileset. Task also accepts jar Manifest to embed it into
 * result jar file.
 * <p>
 * In addition to just creating jar archive it also:
 * <ul>
 *   <li> embeds of JavaFX launcher (for double clickable jars)
 *   <li> embeds of fallback AWT applet (to be used if JavaFX is not available)
 *   <li> ensures jar manifests do not have "bad" entries (such as Class-Path)
 * </ul>
 *
 * @ant.task name="jar" category="javafx"
 */
public class FXJar extends Task {
    private String destFile = null;
    private Application app = null;
    private Platform platform = null;
    private Resources resources = null;
    private List<FileSet> filesets = new LinkedList<FileSet>();
    private Manifest manifest = null;

    private PackagerLib packager;
    private CreateJarParams createJarParams;

    private boolean css2bin = false;

    private boolean verbose = false;
    public void setVerbose(boolean v) {
        verbose = v;
    }

    public Application createApplication() {
        app = new Application();
        return app;
    }

    public Platform createPlatform() {
        platform = new Platform();
        return platform;
    }

    public Resources createResources() {
        resources = new Resources();
        return resources;
    }

    /**
     * Location of output file.
     *
     * @ant.required
     */
    public void setDestfile(String v) {
        destFile = v;
    }

    /**
     * Enable converting CSS files to binary format for faster parsing at
     * runtime.
     *
     * @ant.not-required Default is false.
     */
    public void setCss2Bin(boolean v) {
        css2bin = v;
    }

    public FXJar() {
        packager = new PackagerLib();
        createJarParams = new CreateJarParams();
    }


    @Override
    public void execute() {
        checkAttributesAndElements();

        createJarParams.setCss2bin(css2bin);
        //always embed JavaFX launcher
        createJarParams.setEmbedLauncher(true);

        if (app != null) {
           createJarParams.setApplicationClass(app.get().mainClass);
           createJarParams.setPreloader(app.get().preloaderClass);
           createJarParams.setFallback(app.get().fallbackApp);
           createJarParams.setParams(app.get().parameters);
           createJarParams.setArguments(app.get().getArguments());
        }

        if (platform != null) {
           createJarParams.setFxVersion(platform.get().javafx);
        }

        if (resources != null) {
            createJarParams.setClasspath(resources.exportAsClassPath());
        }

        final File f = new File(destFile);
        createJarParams.setOutdir(f.isAbsolute()
                                      ? null
                                      : getProject().getBaseDir());
        createJarParams.setOutfile(destFile);

        if (manifest != null) {
            createJarParams.setManifestAttrs(getAttrSet(manifest));
        }

        for (FileSet fileset: filesets) {
           Utils.addResources(createJarParams, fileset);
        }

        try {
            packager.packageAsJar(createJarParams);
        } catch (Exception e) {
            throw new BuildException(e.getMessage(), e);
        }
    }

    public Manifest createManifest() {
        manifest = new Manifest();
        return manifest;
    }

    public FileSet createFileSet() {
        FileSet fileset = new FileSet();
        fileset.setProject(getProject());
        filesets.add(fileset);
        return fileset;
    }

    private void checkAttributesAndElements() {
        if (destFile == null) {
            throw new BuildException("You must specify the destfile file to create.");
        }

        File f = new File(destFile);
        if (!f.isAbsolute()) {
            f = new File(getProject().getBaseDir(), destFile);
        }
        if (f.exists() && !f.isFile()) {
            throw new BuildException(destFile + " is not a file.");
        }

        if (f.exists() && !f.canWrite()) {
            throw new BuildException(destFile + " is read-only.");
        }

        if (filesets.isEmpty()) {
            throw new BuildException("You must specify at least one fileset to be packed.");
        }

        boolean haveNonEmpty = false;
        for (FileSet fileset: filesets) {
            if (fileset.size() != 0) {
                haveNonEmpty = true;
                break;
            }
        }

        if (!haveNonEmpty) {
            throw new BuildException("All filesets are empty.");
        }

        if (app != null) {
            app.selfcheck();
        }
    }

    private static Map<String, String> getAttrSet(Manifest manifest) {
        Map<String, String> result = new HashMap<String, String>();

        Manifest.Section mainSection = manifest.getMainSection();
        for (Enumeration e = mainSection.getAttributeKeys(); e.hasMoreElements();) {
            String attrKey = (String) e.nextElement();
            Manifest.Attribute attr = mainSection.getAttribute(attrKey);
            result.put(attr.getName(), attr.getValue());
        }
        return result;
    }
}
