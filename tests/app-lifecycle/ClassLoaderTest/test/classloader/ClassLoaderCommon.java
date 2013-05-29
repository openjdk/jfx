/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package classloader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import junit.framework.TestCase;

/**
 * Test program for JavaFX ClassLoader usage.
 *
 * @author kcr
 */
public class ClassLoaderCommon extends TestCase {

    private static final String appJarName = "../ClassLoaderApp/dist/ClassLoaderApp.jar";
    private static final String jfxRtJarName = "../../../../artifacts/sdk/rt/lib/ext/jfxrt.jar";
    private static final String jfxApplicationClassName = "javafx.application.Application";

    private static URL fileToURL(File file) throws IOException {
        return file.getCanonicalFile().toURI().toURL();
    }

    // Create a new URL ClassLoader to load JavaFX
    private static ClassLoader createJfxClassLoader() throws IOException {
        final File jfxrtJarFile = new File(jfxRtJarName);
        assertTrue(jfxrtJarFile.canRead());
        final File appJarFile = new File(appJarName);
        assertTrue(appJarFile.canRead());
        URL[] urls = {
            fileToURL(jfxrtJarFile),
            fileToURL(appJarFile)
        };
        URLConnection connection = urls[0].openConnection();
        assertNotNull(connection);
        return new URLClassLoader(urls, null);
    }

    public static void doTestClassLoader(String appClassName) throws Exception {
        final ClassLoader sysCl = ClassLoader.getSystemClassLoader();
        final ClassLoader jfxCl = createJfxClassLoader();

//        System.err.println("System class loader = " + sysCl);
//        System.err.println("JavaFX class loader = " + jfxCl);

        assertNotNull(sysCl);
        assertNotNull(jfxCl);
        assertNotSame(sysCl, jfxCl);
        assertTrue(sysCl.getClass().getName().endsWith("AppClassLoader"));
        assertEquals(jfxCl.getClass().getName(), "java.net.URLClassLoader");

        // Set the system class loader as the context class loader
        Thread.currentThread().setContextClassLoader(sysCl);

        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        assertSame(ccl, sysCl);

        // Load the JavaFX Application class
        Class jfxApplicationClass = Class.forName(jfxApplicationClassName, true, jfxCl);

        // load the user's JavaFX class but do *not* initialize!
        Class appClass = Class.forName(appClassName, false, jfxCl);
        assertTrue(jfxApplicationClass.isAssignableFrom(appClass));

        // Launch the JavaFX application
        final Class[] argTypes =
                new Class[] { Class.class, (new String[0]).getClass() };
        Method mLaunch = jfxApplicationClass.getMethod("launch", argTypes);
        assertNotNull(mLaunch);
        mLaunch.invoke(null, new Object[] { appClass, new String[0] });

        // Check that the context class loader is still the same
        ccl = Thread.currentThread().getContextClassLoader();
        assertSame(ccl, sysCl);
        assertNotSame(ccl, jfxCl);
    }

}
