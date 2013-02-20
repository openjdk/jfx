/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tools.packager;

import com.sun.javafx.tools.resource.PackagerResource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class PackagerLibTest {

    @Rule
    public TemporaryFolder dest = new TemporaryFolder();
    @Rule
    public TemporaryFolder src = new TemporaryFolder();

    private PackagerLib lib;

    @Before
    public void setUp() {
        lib = new PackagerLib();
    }

    @Test(expected=IllegalArgumentException.class)
    public void testPackageAsJar_null() throws PackagerException {
        lib.packageAsJar(null);
    }

    private CreateJarParams defaultParams() {
        CreateJarParams params = new CreateJarParams();
        params.outdir = dest.getRoot();
        params.outfile = "temp";
        params.embedLauncher = false;
        params.css2bin = false;
        params.applicationClass = DUMMY_APP_MAIN;

        return params;
    }

    @Test
    public void testPackageAsJar_jarCreate() throws PackagerException {
        lib.packageAsJar(defaultParams());

        File testFile = new File(dest.getRoot(), "temp.jar");
        assertTrue(testFile.exists() && testFile.canRead());
    }

    @Test
    public void testPackageAsJar_jarCreate2() throws PackagerException {
        lib.packageAsJar(defaultParams());

        File testFile = new File(dest.getRoot(), "temp.jar");
        assertTrue(testFile.exists() && testFile.canRead());
    }

    @Test
    public void testPackageAsJar_manifest() throws PackagerException, IOException {
        CreateJarParams params = defaultParams();
        params.manifestAttrs = new HashMap<String, String>();
        params.manifestAttrs.put("testfoo", "bar");
        params.applicationClass = "ham.eggs.Vikings";
        lib.packageAsJar(params);

        File testFile = new File(dest.getRoot(), "temp.jar");
        JarFile jar = new JarFile(testFile);
        Manifest m = jar.getManifest();
        assertEquals(4, m.getMainAttributes().size());
        assertEquals("1.0",
                m.getMainAttributes().get(Attributes.Name.MANIFEST_VERSION));
        assertEquals("ham.eggs.Vikings",
                m.getMainAttributes().get(Attributes.Name.MAIN_CLASS));
        assertEquals("JavaFX Packager",
                m.getMainAttributes().getValue("Created-By"));
        assertEquals("bar", m.getMainAttributes().getValue("testfoo"));
    }


    @Test
    @Ignore // requires jfxrt.jar on classpath
    public void testPackageAsJar_css2bss() throws PackagerException, IOException {
        CreateJarParams params = defaultParams();
        params.resources.add(new PackagerResource(src.getRoot(), "."));
        File css = src.newFile("hello.css");
        new FileWriter(css).write(" .hello { -fx-color: red; }");
        lib.packageAsJar(params);

        File testFile = new File(dest.getRoot(), "temp.jar");
        JarFile jar = new JarFile(testFile);

        // NOTE: Test is incomplete.
    }

    @Test
    public void testPackageAsJar_embedLauncher() throws PackagerException, IOException {
        CreateJarParams params = defaultParams();
        params.applicationClass = "FooBar";
        params.embedLauncher = true;
        params.fxVersion = "2.0";
        params.resources.add(new PackagerResource(src.getRoot(), "."));
        lib.packageAsJar(params);

        File testFile = new File(dest.getRoot(), "temp.jar");
        JarFile jar = new JarFile(testFile);
        Manifest m = jar.getManifest();
        assertEquals("1.0",
                m.getMainAttributes().get(Attributes.Name.MANIFEST_VERSION));
        assertEquals("com/javafx/main/Main",
                m.getMainAttributes().get(Attributes.Name.MAIN_CLASS));
        assertEquals("FooBar",
                m.getMainAttributes().getValue("JavaFX-Application-Class"));
        assertEquals("2.0", m.getMainAttributes().getValue("JavaFX-Version"));
        assertEquals(null, m.getMainAttributes().getValue("JavaFX-Preloader-Class"));
        assertEquals(null, m.getMainAttributes().getValue("JavaFX-Class-Path"));
        assertEquals(null, m.getMainAttributes().getValue("JavaFX-Fallback-Class"));
    }

    @Test
    public void testPackageAsJar_embedLauncher2() throws PackagerException, IOException {
        CreateJarParams params = defaultParams();
        params.applicationClass = "FooBar";
        params.preloader = "PreLoader";
        params.fxVersion = "2.0";
        params.embedLauncher = true;
        params.resources.add(new PackagerResource(src.getRoot(), "."));
        lib.packageAsJar(params);

        File testFile = new File(dest.getRoot(), "temp.jar");
        JarFile jar = new JarFile(testFile);
        Manifest m = jar.getManifest();
        assertEquals("PreLoader",
                m.getMainAttributes().getValue("JavaFX-Preloader-Class"));
    }

    @Test
    public void testPackageAsJar_embedLauncher3() throws PackagerException, IOException {
        CreateJarParams params = defaultParams();
        params.applicationClass = "FooBar";
        params.classpath = "/a/b/c;d/e/f/";
        params.fxVersion = "2.0";
        params.embedLauncher = true;
        params.resources.add(new PackagerResource(src.getRoot(), "."));
        lib.packageAsJar(params);

        File testFile = new File(dest.getRoot(), "temp.jar");
        JarFile jar = new JarFile(testFile);
        Manifest m = jar.getManifest();
        assertEquals("/a/b/c d/e/f/",
                m.getMainAttributes().getValue("JavaFX-Class-Path"));
    }

    @Test
    public void testPackageAsJar_embedLauncher4() throws PackagerException, IOException {
        CreateJarParams params = defaultParams();
        params.applicationClass = "FooBar";
        params.preloader = "PreLoader";
        params.fallbackClass = "com.sun.Fallback";
        params.fxVersion = "2.0";
        params.embedLauncher = true;
        params.resources.add(new PackagerResource(src.getRoot(), "."));
        lib.packageAsJar(params);

        File testFile = new File(dest.getRoot(), "temp.jar");
        JarFile jar = new JarFile(testFile);
        Manifest m = jar.getManifest();
        assertEquals("com.sun.Fallback",
                m.getMainAttributes().getValue("JavaFX-Fallback-Class"));
    }

    // Right now we only validate that XML is well formed. We should validate against schema
    private void validateJNLP(File file) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();

            builder.setErrorHandler(new SimpleErrorHandler());
            try {
                Document document = builder.parse(file);
            } catch (SAXException ex) {
                fail("Parsing exception: "+ex);
            } catch (IOException ex) {
                fail("IOException: "+ex);
            }
        } catch (ParserConfigurationException ex) {
            fail("Runtime issue: "+ex);
        }
    }

    @Test
    public void testGenerateDeploymentPackages() throws PackagerException, IOException {
        DeployParams params = new DeployParams();
        params.outdir = dest.getRoot();
        params.outfile = "temp";
        File j1 = createTestJar(null, DUMMY_APP_MAIN);
        params.addResource(j1.getParentFile(), j1);

        lib.generateDeploymentPackages(params);

        File jnlpFile = new File(dest.getRoot(), "temp.jnlp");
        File htmlFile = new File(dest.getRoot(), "temp.html");
        assertTrue(jnlpFile.exists() && jnlpFile.canRead());
        assertTrue(htmlFile.exists() && htmlFile.canRead());

        validateJNLP(jnlpFile);

        //
    }

    @Test
    //extension JNLP with 1 jar
    public void testGenerateExtensionJNLP_basic() throws PackagerException, IOException {
        DeployParams params = new DeployParams();
        params.outdir = dest.getRoot();
        params.outfile = "temp";
        params.isExtension = true;
        File j1 = createTestJar(null, DUMMY_APP_MAIN);
        params.addResource(j1.getParentFile(), j1);

        lib.generateDeploymentPackages(params);

        File jnlpFile = new File(dest.getRoot(), "temp.jnlp");
        assertTrue(jnlpFile.exists() && jnlpFile.canRead());

        validateJNLP(jnlpFile);
    }

    @Test
    //extension JNLP with 2 jars
    public void testGenerateExtensionJNLP_multi() throws PackagerException, IOException {
        DeployParams params = new DeployParams();
        params.outdir = dest.getRoot();
        params.outfile = "temp";
        params.isExtension = true;
        File j1 = createTestJar(null, DUMMY_APP_MAIN);
        File j2 = createTestJar(null, DUMMY_APP_MAIN);
        params.addResource(j1.getParentFile(), j1);
        params.addResource(j2.getParentFile(), j2);

        lib.generateDeploymentPackages(params);

        File jnlpFile = new File(dest.getRoot(), "temp.jnlp");
        assertTrue(jnlpFile.exists() && jnlpFile.canRead());

        validateJNLP(jnlpFile);
    }

    @Test
    //extension JNLP with several jars. jars may be platform specific
    public void testGenerateExtensionJNLP_multi_mix() throws PackagerException, IOException {
        DeployParams params = new DeployParams();
        params.outdir = dest.getRoot();
        params.outfile = "temp";
        params.isExtension = true;
        File j1 = createTestJar(null, DUMMY_APP_MAIN);
        File j2 = createTestJar(null, DUMMY_APP_MAIN);
        File j3 = createTestJar(null, DUMMY_APP_MAIN);
        params.addResource(j1.getParentFile(), j1, "eager", null, "win", null);
        params.addResource(j2.getParentFile(), j2);
        params.addResource(j2.getParentFile(), j3, "eager", null, "win", null);
        params.addResource(j2.getParentFile(), j3, "eager", null, "linux", null);

        lib.generateDeploymentPackages(params);

        File jnlpFile = new File(dest.getRoot(), "temp.jnlp");
        assertTrue(jnlpFile.exists() && jnlpFile.canRead());

        validateJNLP(jnlpFile);
    }

    void validateSignedJar(File jar) throws FileNotFoundException, IOException {
        assertTrue("Expect to be able to read signed jar", jar.canRead());

        ZipInputStream jis = new ZipInputStream(new FileInputStream(jar));

        ZipEntry ze = null;
        while ((ze = jis.getNextEntry()) != null) {
            if ("META-INF/SIGNATURE.BSF".equalsIgnoreCase(ze.getName())) {
                //found signatures
                return;
            }
        }

        fail("Failed to find signatures in the jar");
    }

    public void doTestSignJar(Manifest m) throws PackagerException, IOException {
        File inputJar = createTestJar(m, "DUMMY.class");

        SignJarParams params = new SignJarParams();
        params.setKeyStore(new File("test.keystore"));
        params.setStorePass("xyz123");
        params.setAlias("TestAlias");
        params.addResource(inputJar.getParentFile(), inputJar);

        File out = dest.getRoot();

        params.setOutdir(out);

        lib.signJar(params);

        validateSignedJar(new File(out, inputJar.getName()));
    }


    @Test
    public void testSignJar_basic() throws PackagerException, IOException {
        doTestSignJar(new Manifest());
    }

    @Test
    public void testSignJar_noManifest() throws PackagerException, IOException {
        doTestSignJar(null);
    }

    @Ignore
    @Test
    public void testSignJar_alreadySigned() throws PackagerException, IOException {
        //TODO: implement creating signed test jar (using normal sign method)
        doTestSignJar(new Manifest());
    }

    private File createTestJar(Manifest m, String entryName) throws IOException {
        File res = File.createTempFile("test", ".jar");
        res.delete();

        if (m != null) {
            //ensure version is there or manifest can be ignored ...
            m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        }

        JarOutputStream jos = (m == null) ?
                new JarOutputStream(new FileOutputStream(res)) :
                new JarOutputStream(new FileOutputStream(res), m);

        byte content[] = "Dummy content".getBytes();
        JarEntry entry = new JarEntry(entryName);
        entry.setTime(new Date().getTime());
        jos.putNextEntry(entry);
        jos.write(content, 0, content.length);
        jos.closeEntry();

        jos.close();

        return res;
    }

    final private static String DUMMY_APP_MAIN = "DummyLauncherClass";

    private void doTest_existingJar(Manifest inputManifest, CreateJarParams params)
            throws PackagerException, IOException {
        String dummyJarEntryName = DUMMY_APP_MAIN;
        File inputJar = createTestJar(inputManifest, dummyJarEntryName);

        if (params == null) {
            params = defaultParams();
        }

        //common settings
        params.outdir = dest.getRoot();
        params.embedLauncher = true;
        params.resources.add(
                new PackagerResource(inputJar.getParentFile(), inputJar));

        lib.packageAsJar(params);

        File testFile = new File(dest.getRoot(), "temp.jar");
        assertTrue(testFile.exists() && testFile.canRead());

        JarFile jar = new JarFile(testFile);
        try {
            JarEntry je = jar.getJarEntry(dummyJarEntryName);
            assertNotNull("Expect old jar content to be copied.", je);

            Manifest m = jar.getManifest();
            assertNotNull("Manifest should not be null", m);

            Attributes attrs = m.getMainAttributes();
            assertNotNull("Expect not null main attributes", attrs);

            assertEquals("Manifest version is not 1.0",
                    "1.0", attrs.get(Attributes.Name.MANIFEST_VERSION));
            assertEquals("Main class should point to JavaFX launcher",
                    "com/javafx/main/Main", attrs.get(Attributes.Name.MAIN_CLASS));
            assertNotNull("JavaFX version should be specified",
                    attrs.getValue("JavaFX-Version"));
            assertNull("ClassPath entry should be reset",
                    attrs.getValue(Attributes.Name.CLASS_PATH));

            assertEquals("Unexpected app main",
                    DUMMY_APP_MAIN, attrs.getValue("JavaFX-Application-Class"));

            assertEquals("Preloader value should match",
                    params.preloader, attrs.getValue("JavaFX-Preloader-Class"));
            assertEquals("Fallback class value should match",
                    params.preloader, attrs.getValue("JavaFX-Fallback-Class"));

            String mainClass = attrs.getValue("JavaFX-Application-Class");
            assertNotNull("JavaFX Main class must be present", mainClass);
            assertEquals("Expect main class to be the same as requested",
                    params.applicationClass, mainClass);

            //classpath should be based
            String inputClasspath = null;
            if (inputManifest != null && inputManifest.getMainAttributes() != null) {
                inputClasspath = inputManifest.getMainAttributes().getValue(
                        Attributes.Name.CLASS_PATH);
            }
            String resultClassPath = attrs.getValue("JavaFX-Class-Path");

            if (params.classpath == null) {
                assertEquals("Expect input classpath copied",
                        inputClasspath, resultClassPath);
            } else {
                assertEquals("Expect classpath to be set as in ant task",
                        params.classpath, resultClassPath);
            }

            //check that custom entries from input manifest were preserved
            if (inputManifest != null) {
                for (Object k : inputManifest.getMainAttributes().keySet()) {
                    if (k instanceof String && ((String) k).contains("Test")) {
                        assertEquals("Expect main manifest atttribute to be copied",
                                inputManifest.getMainAttributes().getValue((String) k),
                                attrs.getValue((String) k));
                    }
                }
            }

        } finally {
            jar.close();
        }
        //TODO: validate that manifest entries for jar enttries are copied
    }

    @Test
    public void testPackageAsJar_existingJar_noManifestJar()
            throws PackagerException, IOException {
        CreateJarParams params = defaultParams();
        params.applicationClass = DUMMY_APP_MAIN;
        doTest_existingJar(null, params);
    }

    @Test
    public void testPackageAsJar_existingJar_ExecutableJar_Overriden()
            throws PackagerException, IOException {
        CreateJarParams params = defaultParams();
        params.classpath = "c.jar";
        Manifest m = new Manifest();
        Attributes attr = m.getMainAttributes();
        attr.put(Attributes.Name.MAIN_CLASS, "SomethingElse");
        attr.put(Attributes.Name.CLASS_PATH, "a.jar:b.jar");

        //expect explicit parameters to overwrite given
        doTest_existingJar(m, params);
    }

    @Test
    public void testPackageAsJar_existingJar_ExecutableJar()
            throws PackagerException, IOException {
        System.out.println("Marker!");
        CreateJarParams params = defaultParams();
        params.applicationClass = null; //reset
        Manifest m = new Manifest();
        Attributes attr = m.getMainAttributes();
        attr.put(Attributes.Name.MAIN_CLASS, DUMMY_APP_MAIN);
        attr.put(Attributes.Name.CLASS_PATH, "a.jar:b.jar");
        //parameters in jar should be ok
        doTest_existingJar(m, params);
    }

    @Test
    public void testPackageAsJar_existingJar_multipleInputs()
            throws PackagerException, IOException {
        //We only "update" jar file if it is THE ONLY input
        //Otherwise we silently add jar as "jar" entry

        CreateJarParams params = defaultParams();
        params.applicationClass = DUMMY_APP_MAIN;

        File f = File.createTempFile("junk", "class");
        String dummyEntry = "dummy";
        params.resources.add(new PackagerResource(f.getParentFile(), f));

        File inputJar = createTestJar(null, dummyEntry);

        if (params == null) {
            params = new CreateJarParams();
        }

        //common settings
        params.outdir = dest.getRoot();
        params.outfile = "temp";
        params.embedLauncher = true;
        params.css2bin = false;
        params.resources.add(
                new PackagerResource(inputJar.getParentFile(), inputJar));

        lib.packageAsJar(params);

        JarFile jar = new JarFile(new File(dest.getRoot(), "temp.jar"));
        try {
            JarEntry je = jar.getJarEntry(dummyEntry);
            assertNull("Do NOT expect jar content to be copied.", je);
        } finally {
            jar.close();
        }
    }

    @Test
    public void testPackageAsJar_existingJar_sameJar()
            throws IOException, PackagerException {
        String dummyJarEntryName = DUMMY_APP_MAIN;
        File inputJar = createTestJar(null, dummyJarEntryName);

        CreateJarParams params = defaultParams();

        //common settings
        params.outdir = inputJar.getParentFile();
        params.outfile = inputJar.getName();
        params.embedLauncher = true;
        params.resources.add(
                new PackagerResource(inputJar.getParentFile(), inputJar));

        lib.packageAsJar(params);


        //validate have launcher class and original content
        JarFile jar = new JarFile(inputJar);
        Attributes attrs = jar.getManifest().getMainAttributes();
        assertEquals("Main class should point to JavaFX launcher",
                "com/javafx/main/Main", attrs.get(Attributes.Name.MAIN_CLASS));
        assertEquals("Unexpected app main",
                DUMMY_APP_MAIN, attrs.getValue("JavaFX-Application-Class"));

        try {
            JarEntry je = jar.getJarEntry(dummyJarEntryName);
            assertNotNull("Do NOT expect jar content to be copied.", je);
        } finally {
            jar.close();
        }

    }

    private static class SimpleErrorHandler implements ErrorHandler {

        public SimpleErrorHandler() {
        }

        public void warning(SAXParseException saxpe) throws SAXException {
            saxpe.printStackTrace();
            fail("Warning");
        }

        public void error(SAXParseException saxpe) throws SAXException {
            saxpe.printStackTrace();
            fail("Parsing error");
        }

        public void fatalError(SAXParseException saxpe) throws SAXException {
            saxpe.printStackTrace();
            fail("Fatal error");
        }
    }

}
