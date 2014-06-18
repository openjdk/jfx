/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.tools.packager;

import com.oracle.tools.packager.windows.WindowsBundlerParam;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Pattern;

public abstract class AbstractBundler implements Bundler {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle(AbstractBundler.class.getName());

    public static final BundlerParamInfo<File> IMAGES_ROOT = new WindowsBundlerParam<>(
            I18N.getString("param.images-root.name"),
            I18N.getString("param.images-root.description"),
            "imagesRoot",
            File.class,
            params -> new File(StandardBundlerParam.BUILD_ROOT.fetchFrom(params), "images"),
            (s, p) -> null);

    //do not use file separator -
    // we use it for classpath lookup and there / are not platform specific
    public final static String BUNDLER_PREFIX = "package/";

    protected static final String JAVAFX_LAUNCHER_CLASS = "com.javafx.main.Main";

    protected Class baseResourceLoader = null;
    
    //helper method to test if required files are present in the runtime
    public void testRuntime(RelativeFileSet runtime, String[] file) throws ConfigException {
        if (runtime == null) {
            return; //null runtime is ok (request to use system)
        }

        Pattern[] weave = Arrays.stream(file).map(Pattern::compile).toArray(Pattern[]::new);

        if (!runtime.getIncludedFiles().stream().anyMatch(s ->
                Arrays.stream(weave).anyMatch(pattern -> pattern.matcher(s).matches())
        )) {
            throw new ConfigException(
                    MessageFormat.format(I18N.getString("error.jre-missing-file"), Arrays.toString(file)),
                    I18N.getString("error.jre-missing-file.advice"));
        }
    }

    protected void fetchResource(
            String publicName, String category,
            String defaultName, File result, boolean verbose)
            throws IOException {
        URL u = locateResource(publicName, category, defaultName, verbose);
        if (u != null) {
            IOUtils.copyFromURL(u, result);
        } else {
            if (verbose) {
                Log.info(MessageFormat.format(I18N.getString("message.using-default-resource"), category == null ? "" : "[" + category + "] ", publicName));
            }
        }
    }

    protected void fetchResource(
            String publicName, String category,
            File defaultFile, File result, boolean verbose)
            throws IOException {
        URL u = locateResource(publicName, category, null, verbose);
        if (u != null) {
            IOUtils.copyFromURL(u, result);
        } else {
            IOUtils.copyFile(defaultFile, result);
            if (verbose) {
                Log.info(MessageFormat.format(I18N.getString("message.using-default-resource-from-file"), category == null ? "" : "[" + category + "] ", defaultFile.getAbsoluteFile()));
            }
        }
    }

    private URL locateResource(String publicName, String category,
                               String defaultName, boolean verbose) throws IOException {
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
            msg = MessageFormat.format(I18N.getString("message.using-custom-resource-from-classpath"), category == null ? "" : "[" + category + "] ", publicName);
        } else if (u != null) {
            msg = MessageFormat.format(I18N.getString("message.using-default-resource-from-classpath"), category == null ? "" : "[" + category + "] ", publicName);
        }
        if (verbose && u != null) {
            Log.info(msg);
        }
        return u;
    }

    protected String preprocessTextResource(String publicName, String category,
                                            String defaultName, Map<String, String> pairs,
                                            boolean verbose) throws IOException {
        URL u = locateResource(publicName, category, defaultName, verbose);
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

    @Override
    public String toString() {
        return getName();
    }
}
