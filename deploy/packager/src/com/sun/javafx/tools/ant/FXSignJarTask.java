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

import com.sun.javafx.tools.packager.PackagerLib;
import com.sun.javafx.tools.packager.SignJarParams;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * Sign jar as BLOB (instead of signing every entry separately sign jar
 * file as one huge binary object).
 * <p>
 * This is new signing method in JavaFX. Standard ant signjar task need to
 * be used for traditional signing.
 * <p>
 * Usage examples:
 * <pre>
 *   &lt;signjar keyStore="${basedir}/sample.jks" destdir="c:/tmp"
 *               alias="javafx" storePass="****" keyPass="****">
 *      &lt;fileset dir='${build.dir}/dist/** /*.jar'/>
 *   &lt;/signjar>
 * </pre>
 *
 * @ant.task name="signjar" category="javafx"
 */
public class FXSignJarTask extends Task {

    private File keyStore;
    private String alias;
    private String storePass;
    private String keyPass;
    private String storeType = "jks";
    private List<FileSet> fileSets = new ArrayList<FileSet>();
    private String deprecatedSourceJar; //deprecated since 2.2
    private String sourceJar; //deprecated since 2.2
    private String destDir;

    private PackagerLib packager;
    private SignJarParams signJarParams;
    private boolean verbose = false;

    public FXSignJarTask() {
        packager = new PackagerLib();
        signJarParams = new SignJarParams();
    }
    @Override
    public void execute() throws BuildException {
        signJarParams.setKeyStore(keyStore);
        signJarParams.setAlias(alias);
        signJarParams.setStorePass(storePass);
        signJarParams.setKeyPass(keyPass);
        signJarParams.setStoreType(storeType);
        signJarParams.setVerbose(verbose);

        if (destDir != null) {
            signJarParams.setOutdir(new File(destDir));
        }

        if (!fileSets.isEmpty()) {
            if (deprecatedSourceJar != null) {
                throw new IllegalArgumentException(
                        "Unexpected sourcejar attribute when fileset present");
            }
            if (sourceJar != null) {
                throw new IllegalArgumentException(
                        "Unexpected sourcejar attribute when fileset present");
            }
        }

        if (sourceJar != null && deprecatedSourceJar != null) {
                throw new IllegalArgumentException(
                        "Cannot use both sourcejar and jar attributes. (sourcejar attribute is deprecated).");
        }

        for (FileSet fileset: fileSets) {
            Utils.addResources(signJarParams, fileset);
        }
        if (deprecatedSourceJar != null) {
            File sourceFile = new File(deprecatedSourceJar);
            if (sourceFile.exists()) {
                signJarParams.addResource(new File("."), sourceFile);
            }
        }
        if (sourceJar != null) {
            File sourceFile = new File(sourceJar);
            if (sourceFile.exists()) {
                //treat as request to copy file to top level folder
                // (i.e. ignore path part)
                //this is consistent with built-in signjar
                signJarParams.addResource(
                        sourceFile.getParentFile(), sourceFile.getName());
            }
        }
        try {
            signJarParams.validate();
            packager.signJar(signJarParams);
        } catch (Exception e) {
            throw new BuildException(e.getMessage(), e);
        }
    }

    /**
     * The alias to sign under.
     *
     * @ant.required
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setVerbose(Boolean verbose) {
        this.verbose = verbose;
    }


    /**
     * Password for private key.
     *
     * @ant.required
     */
    public void setKeyPass(String keyPassword) {
        this.keyPass = keyPassword;
    }

    /**
     * Password for keystore
     *
     * @ant.required
     */
    public void setStorePass(String storePassword) {
        this.storePass = storePassword;
    }

    /**
     * Keystore to use.
     *
     * @ant.required
     */
    public void setKeyStore(File keyStore) {
        this.keyStore = keyStore;
    }

    /**
     * Keystore type
     *
     * @ant.not-required Default is "jks".
     */
    public void setStoreType(String storeType) {
        this.storeType = storeType;
    }

    public void addFileSet(FileSet fileset) {
        fileSets.add(fileset);
    }

    /**
     * The jar to sign (deprecated since 2.2, use jar attribute)
     *
     * @ant.not-required Either this or jar attribute or embedded fileset are required.
     */
    public void setSourceJar(String sourceJar) {
        this.deprecatedSourceJar = sourceJar;
    }

    /**
     * The jar to sign.
     *
     * @ant.not-required Either this or sourcejar or embedded fileset are required.
     */
    public void setJar(String sourceJar) {
        this.sourceJar = sourceJar;
    }

    /**
     * Location of output file.
     *
     * @ant.required
     */
    public void setDestDir(String destDir) {
        this.destDir = destDir;
    }
}
