/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.tools.packager.jnlp;

import com.oracle.tools.packager.AbstractBundler;
import com.oracle.tools.packager.Bundler;
import com.oracle.tools.packager.BundlerParamInfo;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.Log;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.UnsupportedPlatformException;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.oracle.tools.packager.StandardBundlerParam.*;
import static com.oracle.tools.packager.jnlp.JNLPBundler.*;
import static org.junit.Assert.*;

public class JNLPBundlerTest {

    static File tmpBase;
    static File workDir;
    static File appResourcesDir;
    static File fakeMainJar;
    static Set<File> appResources;
    static boolean retain = false;

    static final File FAKE_CERT_ROOT = new File("build/tmp/tests/cert/").getAbsoluteFile();

    @BeforeClass
    public static void prepareApp() {
        Log.setLogger(new Log.Logger(true));
        Log.setDebug(true);

        retain = Boolean.parseBoolean(System.getProperty("RETAIN_PACKAGER_TESTS"));

        workDir = new File("build/tmp/tests", "jnlpapp");
        appResourcesDir = new File("build/tmp/tests", "appResources");
        fakeMainJar = new File(appResourcesDir, "mainApp.jar");

        appResources = new HashSet<>(Collections.singletonList(fakeMainJar));

    }

    @Before
    public void createTmpDir() throws IOException {
        if (retain) {
            tmpBase = new File("build/tmp/tests/jnlpapp");
        } else {
            tmpBase = BUILD_ROOT.fetchFrom(new TreeMap<>());
        }
        tmpBase.mkdir();
    }


    @After
    public void maybeCleanupTmpDir() {
        if (!retain) {
            attemptDelete(tmpBase);
        }
        attemptDelete(FAKE_CERT_ROOT);
    }

    private void attemptDelete(File tmpBase) {
        if (tmpBase.isDirectory()) {
            File[] children = tmpBase.listFiles();
            if (children != null) {
                for (File f : children) {
                    attemptDelete(f);
                }
            }
        }
        boolean success;
        try {
            success = !tmpBase.exists() || tmpBase.delete();
        } catch (SecurityException se) {
            success = false;
        }
        if (!success) {
            System.err.println("Could not clean up " + tmpBase.toString());
        }
    }


    /**
     * See if smoke comes out
     */
    @Test
    public void smokeTest() throws IOException, ConfigException, UnsupportedPlatformException {
        AbstractBundler bundler = new JNLPBundler();

        assertNotNull(bundler.getName());
        assertNotNull(bundler.getID());
        assertNotNull(bundler.getDescription());
        //assertNotNull(bundler.getBundleParameters());

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(BUILD_ROOT.getID(), tmpBase);

        bundleParams.put(APP_NAME.getID(), "Smoke Test App");
        bundleParams.put(MAIN_CLASS.getID(), "hello.HelloRectangle");
        bundleParams.put(PREFERENCES_ID.getID(), "the/really/long/preferences/id");
        bundleParams.put(MAIN_JAR.getID(),
                new RelativeFileSet(fakeMainJar.getParentFile(),
                        new HashSet<>(Collections.singletonList(fakeMainJar)))
        );
        bundleParams.put(CLASSPATH.getID(), "mainApp.jar");
        bundleParams.put(OUT_FILE.getID(), "Smoke");
        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
        bundleParams.put(VERBOSE.getID(), true);
        bundleParams.put(SIGN_BUNDLE.getID(), false);

        boolean valid = bundler.validate(bundleParams);
        assertTrue(valid);

        File result = bundler.execute(bundleParams, new File(workDir, "smoke"));
        System.err.println("Bundle at - " + result);
        assertNotNull(result);
        assertTrue(result.exists());
    }

    /**
     * Set File Association
     */
    @Test
    public void testFileAssociation()
        throws IOException, ConfigException, UnsupportedPlatformException
    {
        // only run the bundle with full tests
        Assume.assumeTrue(Boolean.parseBoolean(System.getProperty("FULL_TEST")));

        testFileAssociation("FASmoke 1", "Bogus File", "bogus", "application/x-vnd.test-bogus",
                            new File(appResourcesDir, "test.icns"));
    }

    @Test
    public void testFileAssociationWithNullExtension()
        throws IOException, ConfigException, UnsupportedPlatformException
    {
        // association with no extension is still valid case (see RT-38625)
        testFileAssociation("FASmoke null", "Bogus File", null, "application/x-vnd.test-bogus",
                            new File(appResourcesDir, "test.icns"));
    }

    @Test
    public void testFileAssociationWithMultipleExtension()
            throws IOException, ConfigException, UnsupportedPlatformException
    {
        // only run the bundle with full tests
        Assume.assumeTrue(Boolean.parseBoolean(System.getProperty("FULL_TEST")));

        testFileAssociation("FASmoke ME", "Bogus File", "bogus fake", "application/x-vnd.test-bogus",
                new File(appResourcesDir, "test.icns"));
    }

    @Test
    public void testMultipleFileAssociation()
            throws IOException, ConfigException, UnsupportedPlatformException
    {
        // only run the bundle with full tests
        Assume.assumeTrue(Boolean.parseBoolean(System.getProperty("FULL_TEST")));

        testFileAssociationMultiples("FASmoke MA",
                new String[]{"Bogus File", "Fake file"},
                new String[]{"bogus", "fake"},
                new String[]{"application/x-vnd.test-bogus", "application/x-vnd.test-fake"},
                new File[]{new File(appResourcesDir, "test.icns"), new File(appResourcesDir, "test.icns")});
    }

    @Test
    public void testMultipleFileAssociationWithMultipleExtension()
            throws IOException, ConfigException, UnsupportedPlatformException
    {
        // association with no extension is still valid case (see RT-38625)
        testFileAssociationMultiples("FASmoke MAME",
                new String[]{"Bogus File", "Fake file"},
                new String[]{"bogus boguser", "fake faker"},
                new String[]{"application/x-vnd.test-bogus", "application/x-vnd.test-fake"},
                new File[]{new File(appResourcesDir, "test.icns"), new File(appResourcesDir, "test.icns")});
    }

    private void testFileAssociation(String appName, String description, String extensions,
                                     String contentType, File icon)
            throws IOException, ConfigException, UnsupportedPlatformException
    {
        testFileAssociationMultiples(appName, new String[] {description}, new String[] {extensions},
                new String[] {contentType}, new File[] {icon});
    }

    private void testFileAssociationMultiples(String appName, String[] description, String[] extensions,
                                              String[] contentType, File[] icon)
            throws IOException, ConfigException, UnsupportedPlatformException
    {
        assertEquals("Sanity: description same length as extensions", description.length, extensions.length);
        assertEquals("Sanity: extensions same length as contentType", extensions.length, contentType.length);
        assertEquals("Sanity: contentType same length as icon", contentType.length, icon.length);

        AbstractBundler bundler = new JNLPBundler();

        assertNotNull(bundler.getName());
        assertNotNull(bundler.getID());
        assertNotNull(bundler.getDescription());
        //assertNotNull(bundler.getBundleParameters());

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(BUILD_ROOT.getID(), tmpBase);

        bundleParams.put(APP_NAME.getID(), appName);
        bundleParams.put(MAIN_CLASS.getID(), "hello.HelloRectangle");
        bundleParams.put(MAIN_JAR.getID(),
                new RelativeFileSet(fakeMainJar.getParentFile(),
                        new HashSet<>(Collections.singletonList(fakeMainJar)))
        );
        bundleParams.put(CLASSPATH.getID(), "mainApp.jar");
        bundleParams.put(OUT_FILE.getID(), appName);
        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
        bundleParams.put(VERBOSE.getID(), true);
        bundleParams.put(SIGN_BUNDLE.getID(), false);

        List<Map<String, Object>> associations = new ArrayList<>();

        for (int i = 0; i < description.length; i++) {
            Map<String, Object> fileAssociation = new HashMap<>();
            fileAssociation.put(FA_DESCRIPTION.getID(), description[i]);
            fileAssociation.put(FA_EXTENSIONS.getID(), extensions[i]);
            fileAssociation.put(FA_CONTENT_TYPE.getID(), contentType[i]);
            fileAssociation.put(FA_ICON.getID(), icon[i]);

            associations.add(fileAssociation);
        }

        bundleParams.put(FILE_ASSOCIATIONS.getID(), associations);

        boolean valid = bundler.validate(bundleParams);
        assertTrue(valid);

        File result = bundler.execute(bundleParams, new File(workDir, APP_FS_NAME.fetchFrom(bundleParams)));
        System.err.println("Bundle at - " + result);
        assertNotNull(result);
        assertTrue(result.exists());

        //TODO verify file associations present
    }

    /**
     * The bare minimum configuration needed to make it work
     * <ul>
     *     <li>Where to build it</li>
     *     <li>The jar containing the application (with a main-class attribute)</li>
     * </ul>
     *
     * All other values will be driven off of those two values.
     */
    @Test
    public void minimumConfig() throws IOException, ConfigException, UnsupportedPlatformException {
        Bundler bundler = new JNLPBundler();

        Map<String, Object> bundleParams = new HashMap<>();

        // not part of the typical setup, for testing
        bundleParams.put(BUILD_ROOT.getID(), tmpBase);

        bundleParams.put(OUT_FILE.getID(), "BareMinimum");
        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));

        File output = bundler.execute(bundleParams, new File(workDir, "BareMinimum"));
        System.err.println("Bundle at - " + output);
        assertNotNull(output);
        assertTrue(output.exists());
    }

    /**
     * Test with unicode in places we expect it to be
     */
    @Test
    @Ignore // unicode file names hang the windows build
    public void unicodeConfig() throws IOException, ConfigException, UnsupportedPlatformException {
        Bundler bundler = new JNLPBundler();

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(BUILD_ROOT.getID(), tmpBase);

        bundleParams.put(OUT_FILE.getID(), "хелловорлд");
        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));

        bundleParams.put(APP_NAME.getID(), "хелловорлд");
        bundleParams.put(TITLE.getID(), "ХеллоВорлд аппликейшн");
        bundleParams.put(VENDOR.getID(), "Оракл девелопмент");
        bundleParams.put(DESCRIPTION.getID(), "крайне большое описание со странными символами");

        bundler.validate(bundleParams);

        File output = bundler.execute(bundleParams, new File(workDir, "Unicode"));
        System.err.println("Bundle at - " + output);
        assertNotNull(output);
        assertTrue(output.exists());
    }

    @Test
    public void configureEverything() throws Exception {
        AbstractBundler bundler = new JNLPBundler();
        Collection<BundlerParamInfo<?>> parameters = bundler.getBundleParameters();

        Map<String, Object> bundleParams = new HashMap<>();

        File outputDir = new File(workDir, "everything");

        bundleParams.put(ALL_PERMISSIONS.getID(), false);
        bundleParams.put(APPLET_PARAMS.getID(), Collections.singletonMap("plainParam", "plainValue"));
        bundleParams.put(APP_NAME.getID(), "Everything App Name");
        bundleParams.put(APP_PARAMS.getID(), Collections.singletonMap("AppParam", "AppValue"));
        bundleParams.put(APP_RESOURCES_LIST.getID(), Collections.singletonList(new RelativeFileSet(appResourcesDir, appResources)));
        bundleParams.put(ARGUMENTS.getID(), Arrays.asList("He Said", "She Said"));
        bundleParams.put(CODEBASE.getID(), outputDir.toURI().toString());
        bundleParams.put(DESCRIPTION.getID(), "This is the everything description");
        bundleParams.put(EMBED_JNLP.getID(), true);
        bundleParams.put(EMBEDDED_HEIGHT.getID(), "400");
        bundleParams.put(EMBEDDED_WIDTH.getID(), "600");
        bundleParams.put(ESCAPED_APPLET_PARAMS.getID(), Collections.singletonMap("escapedParam", "This param requres escaping"));
        bundleParams.put(EXTENSION.getID(), false);
//        bundleParams.put(FALLBACK_APP.getID(), null);
//        bundleParams.put(FX_PLATFORM.getID(), "8.0");
        bundleParams.put(HEIGHT.getID(), 400);
        bundleParams.put(ICONS.getID(), Collections.emptyList()); //FIXME should attempt
        bundleParams.put(IDENTIFIER.getID(), "EverythingIdentifier");
        bundleParams.put(INCLUDE_DT.getID(), true);
        bundleParams.put(INSTALL_HINT.getID(), true);
        bundleParams.put(JRE_PLATFORM.getID(), "1.8.0.40+");
        bundleParams.put(JS_CALLBACKS.getID(), Collections.emptyMap());
        bundleParams.put(JVM_OPTIONS.getID(), "-Xms128M");
        bundleParams.put(JVM_PROPERTIES.getID(), "everything.jvm.property=everything.jvm.property.value");
        bundleParams.put(MAIN_CLASS.getID(), "hello.HelloRectangle");
        bundleParams.put(MENU_HINT.getID(), true);
        bundleParams.put(OFFLINE_ALLOWED.getID(), true);
        bundleParams.put(OUT_FILE.getID(), "Everything");
        bundleParams.put(PRELOADER_CLASS.getID(), "hello.HelloPreloader");
        bundleParams.put(PLACEHOLDER.getID(), "everythingPlaceholder");
        bundleParams.put(SHORTCUT_HINT.getID(), true);
        bundleParams.put(SWING_APP.getID(), false);
        bundleParams.put(TEMPLATES.getID(), Collections.emptyMap());
        bundleParams.put(TITLE.getID(), "Everything JNLP Test App");
        bundleParams.put(UPDATE_MODE.getID(), "background");
        bundleParams.put(VENDOR.getID(), "Example Corp.");
        bundleParams.put(WIDTH.getID(), 600);

        // assert they are set
        for (BundlerParamInfo bi :parameters) {
            assertTrue("Bundle args should contain " + bi.getID(), bundleParams.containsKey(bi.getID()));
        }

        // and only those are set
        bundleParamLoop:
        for (String s :bundleParams.keySet()) {
            for (BundlerParamInfo<?> bpi : parameters) {
                if (s.equals(bpi.getID())) {
                    continue bundleParamLoop;
                }
            }
            fail("Enumerated parameters does not contain " + s);
        }

        // assert they resolve
        for (BundlerParamInfo bi :parameters) {
            bi.fetchFrom(bundleParams);
        }

        // add verbose now that we are done scoping out parameters
        bundleParams.put(BUILD_ROOT.getID(), tmpBase);
        bundleParams.put(VERBOSE.getID(), true);
        bundleParams.put(SIGN_BUNDLE.getID(), false);

        // assert it validates
        boolean valid = bundler.validate(bundleParams);
        assertTrue(valid);

        // only run the bundle with full tests
        Assume.assumeTrue(Boolean.parseBoolean(System.getProperty("FULL_TEST")));

        File result = bundler.execute(bundleParams, outputDir);
        System.err.println("Bundle at - " + result);
        assertNotNull(result);
        assertTrue(result.exists());
    }

    /**
     * test JNLP value escaping
     */
    @Test
    public void escapedCharactersTest() throws IOException, ConfigException, UnsupportedPlatformException {
        Bundler bundler = new JNLPBundler();

        Map<String, Object> bundleParams = new HashMap<>();

        // not part of the typical setup, for testing
        bundleParams.put(BUILD_ROOT.getID(), tmpBase);

        bundleParams.put(OUT_FILE.getID(), "Escape");
        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));

        // turn on some generation options
        bundleParams.put(EMBED_JNLP.getID(), true);
        bundleParams.put(INCLUDE_DT.getID(), false);

        bundleParams.put(APPLET_PARAMS.getID(), Collections.singletonMap("TestAppletParams", "\"All !@#$%^&amp;*()_+{}|:\\\"&lt;&gt;?[]\\\\;',./ Punctuation\""));
        bundleParams.put(APP_NAME.getID(), "Test App Name All !@#$%^&*()_+{}|:\"<>?[]\\;',./ Punctuation");
        bundleParams.put(APP_PARAMS.getID(),Collections.singletonMap("Test App Params", "All !@#$%^&*()_+{}|:\"<>?[]\\;',./ Punctuation"));
        bundleParams.put(ARGUMENTS.getID(), Collections.singletonList("Test Argument All !@#$%^&*()_+{}|:\"<>?[]\\;',./ Punctuation"));
        bundleParams.put(DESCRIPTION.getID(), "Test Description All !@#$%^&*()_+{}|:\"<>?[]\\;',./ Punctuation");
        bundleParams.put(ESCAPED_APPLET_PARAMS.getID(), Collections.singletonMap("TestEscapedAppletParams", "All !@#$%^&*()_+{}|:\"<>?[]\\;',./ Punctuation"));
        bundleParams.put(JVM_PROPERTIES.getID(), "escaped.jvm.property=All!@#$%^&*()_+{}|:\"<>?[]\\;',./Punctuation");
        bundleParams.put(TITLE.getID(), "Test Title All !@#$%^&*()_+{}|:\"<>?[]\\;',./ Punctuation");
        bundleParams.put(VENDOR.getID(), "Test Vendor All !@#$%^&*()_+{}|:\"<>?[]\\;',./ Punctuation");

        File output = bundler.execute(bundleParams, new File(workDir, "Escape"));
        System.err.println("Bundle at - " + output);
        assertNotNull(output);
        assertTrue(output.exists());
    }
}
