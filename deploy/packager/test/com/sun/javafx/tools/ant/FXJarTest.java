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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ManifestException;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FXJarTest {
    @Rule
    public TemporaryFolder dest = new TemporaryFolder();
    @Rule
    public TemporaryFolder src = new TemporaryFolder();

    private FXJar fxjar = null;
    private File testOut = null;

    static int cnt = 0;

    private final static String DUMMY_MAIN = "DummyMain";

    public FXJarTest() {
    }

    private void writeTempFile(String name, String content) {
        try {
            File tmpFile = new File(src.getRoot(), name);
            FileOutputStream fos = new FileOutputStream(tmpFile);
            fos.write(content.getBytes());
            fos.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Before
    public void setUp() {
        writeTempFile("testfile.txt", "something");
        fxjar = new FXJar();
        Project prj = new Project();
        fxjar.setProject(prj);

        testOut = new File(dest.getRoot(), "test"+cnt+".jar");
        cnt++; //use new file name for every test
        fxjar.setDestfile(testOut.getAbsolutePath());
        org.apache.tools.ant.types.FileSet fs = fxjar.createFileSet();
        fs.setDir(src.getRoot());

        Application app = fxjar.createApplication();
        app.mainClass = DUMMY_MAIN;
    }

    @After
    public void tearDown() {
        if (testOut != null) {
            //testOut.delete();
            testOut = null;
        }
    }

    @Test
    public void basicsTest() throws IOException {
        //set minumum subset of required params
        fxjar.execute();

        assertTrue("Expect output to exist", testOut.canRead());
        JarFile jout = new JarFile(testOut);
        Manifest m = jout.getManifest();
        assertNotNull("Manifest should be not null", m);
        assertEquals(DUMMY_MAIN,
                m.getMainAttributes().getValue("JavaFX-Application-Class"));
    }

    @Test
    public void capitalInManifestTest() throws IOException, ManifestException {
        String testName = "Implementation-Title";
        //set minumum subset of required params
        org.apache.tools.ant.taskdefs.Manifest mIn = fxjar.createManifest();
        mIn.addConfiguredAttribute(
                new org.apache.tools.ant.taskdefs.Manifest.Attribute(
                    testName, DUMMY_MAIN));
        fxjar.execute();

        JarFile jout = new JarFile(testOut);
        Manifest m = jout.getManifest();
        assertTrue("Expect to find attribute",
                m.getMainAttributes().containsKey(
                  new java.util.jar.Attributes.Name(testName)));
    }

    @Test(expected=org.apache.tools.ant.BuildException.class)
    public void missingDestDir() {
        FXJar j = new FXJar();
        j.execute();
    }

}
