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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Bundler {
    protected File buildRoot = null;
    protected File imagesRoot = null;

    protected boolean verbose = false;

    //do not use file separator -
    // we use it for classpath lookup and there / are not platfrom specific
    public final static String BUNDLER_PREFIX = "package/";

    protected static final String JAVAFX_LAUNCHER_CLASS = "com.javafx.main.Main";

    public static enum BundleType {
        NONE, IMAGE, INSTALLER, ALL
    };

    public void setVerbose(boolean m) {
        verbose = m;
    }

    private static File getTempDir(String prefix) throws IOException {
       File dir = File.createTempFile(prefix, ".fxbundler");
       dir.delete(); //remove empty file we created
       dir.mkdirs();
       return dir;
    }

    public static class UnsupportedPlatformException extends Exception {};
    public static class ConfigException extends Exception {
        String advice = null;

        public ConfigException(String msg, String advice) {
            super(msg);
            this.advice = advice;
        }

        public String getAdvice() {
            return advice;
        }
    };

    private static final List<Bundler> knownBundlers = new LinkedList<Bundler>();

    static {
        knownBundlers.add(new WinAppBundler());
        knownBundlers.add(new MacAppBundler());
        knownBundlers.add(new WinMsiBundler());
        knownBundlers.add(new WinExeBundler());
        knownBundlers.add(new MacDMGBundler());
        knownBundlers.add(new LinuxAppBundler());
        knownBundlers.add(new LinuxRPMBundler());
        knownBundlers.add(new LinuxDebBundler());
    }

    protected void setBuildRoot(File dir) {
        buildRoot = dir;
        imagesRoot = new File(dir, "images");
        imagesRoot.mkdirs();
    }

    public static List<Bundler> get(BundleParams p, boolean verbose) {
        List<Bundler> result = new LinkedList<Bundler>();

        File buildDir = null;

        try {
            buildDir = getTempDir("build");
        } catch (IOException ioe) {}

        Log.verbose("Looking for bundlers for type=" + p.type.toString()
                + " format=" + (p.bundleFormat != null ? p.bundleFormat : "any"));
        for (Bundler b: knownBundlers) {
            if (verbose) {
                b.setVerbose(true);
            }
            try {
                if (b.validate(p)) {
                    b.setBuildRoot(buildDir);
                    result.add(b);
                }
            } catch(UnsupportedPlatformException e) {
                //can not build bundle of this type on this platform
                Log.debug("  Skip [" + b.toString() +
                           "] as not supporting current platform");
            } catch (ConfigException ee) {
                //can build but paramters are not good
                Log.info("  Skip ["+b.toString()+"] due to [" +
                           ee.getMessage() + "]");
                if (ee.getAdvice() != null) {
                    Log.verbose("   [" + ee.getAdvice() + "]");
                }
            }
        }

        return result;
    }

    //validate
    abstract boolean validate(BundleParams p)
            throws UnsupportedPlatformException, ConfigException;

    //create bundle
    public abstract boolean bundle(BundleParams p, File outdir);

    //helper method to test if required files are present in the runtime
    void testRuntime(BundleParams p, String file) throws ConfigException {
        if (p.runtime == null) {
            return; //null runtime is ok (request to use system)
        }
        Set<String> rfiles = p.runtime.getIncludedFiles();
        if (!rfiles.contains(file)) {
            throw new Bundler.ConfigException(
                    "Java Runtime does not include " + file,
                    "Make sure ant is using Oracle JDK 7u6 or later.");
        }
    }

    protected Class baseResourceLoader = null;

    protected void fetchResource(
            String publicName, String category,
            String defaultName, File result)
            throws IOException {
        URL u = locateResource(publicName, category, defaultName);
        if (u != null) {
            IOUtils.copyFromURL(u, result);
        } else {
            if (verbose) {
               Log.info("Using default package resource " +
                  (category == null ? "" : "["+category+"] ") +
                  "(add "+publicName+" to the class path to customize)");
            }
        }
    }

    protected void fetchResource(
            String publicName, String category,
            File defaultFile, File result)
            throws IOException {
        URL u = locateResource(publicName, category, null);
        if (u != null) {
            IOUtils.copyFromURL(u, result);
        } else {
            IOUtils.copyFile(defaultFile, result);
            if (verbose) {
                Log.info("   Using custom package resource " +
                  (category == null ? "" : "["+category+"] ") +
                  "(loaded from file "+defaultFile.getAbsoluteFile() + ")");
            }
        }
    }

    private URL locateResource(String publicName, String category,
            String defaultName) throws IOException {
        URL u = null;
        boolean custom = false;
        if (publicName != null) {
            u = baseResourceLoader.getClassLoader().getResource(publicName);
            custom = (u != null);
        }
        if (u == null && defaultName != null) {
            u = baseResourceLoader.getResource(defaultName);
        }
        String msg = null;
        if (custom) {
            msg = "  Using custom package resource " +
                  (category == null ? "" : "["+category+"] ") +
                  "(loaded from "+publicName+" on class path)";
        } else if (u != null) {
            msg = "  Using default package resource " +
                  (category == null ? "" : "["+category+"] ") +
                  "(add "+publicName+" to the class path to customize)";
        }
        if (verbose && u != null) {
            Log.info(msg);
        }
        return u;
    }

    protected String preprocessTextResource(String publicName, String category,
            String defaultName, Map<String, String> pairs) throws IOException {
        URL u = locateResource(publicName, category, defaultName);
        InputStream inp = u.openStream();
        if (inp == null) {
            throw new RuntimeException("Jar corrupt? No "+defaultName+" resource!");
        }

        //read fully into memory
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inp.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }

        //substitute
        String result = new String(baos.toByteArray());
        for (Map.Entry<String, String> e : pairs.entrySet()) {
            if (e.getValue() != null) {
               result = result.replace(e.getKey(), e.getValue());
            }
        }
        return result;
    }

}
