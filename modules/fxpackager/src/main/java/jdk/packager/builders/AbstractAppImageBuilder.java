/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.packager.builders;

import com.oracle.tools.packager.JLinkBundlerHelper;
import com.oracle.tools.packager.RelativeFileSet;
import jdk.tools.jlink.builder.*;
import jdk.tools.jlink.plugin.Pool;

import com.oracle.tools.packager.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import static com.oracle.tools.packager.StandardBundlerParam.*;
import static com.oracle.tools.packager.StandardBundlerParam.ARGUMENTS;
import static com.oracle.tools.packager.StandardBundlerParam.USER_JVM_OPTIONS;

/**
 *
 * Created by dferrin on 9/1/15.
 */
public abstract class AbstractAppImageBuilder extends DefaultImageBuilder {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle(AbstractAppImageBuilder.class.getName());

    //do not use file separator -
    // we use it for classpath lookup and there / are not platform specific
    public final static String BUNDLER_PREFIX = "package/";

    public AbstractAppImageBuilder(Map<String, Object> properties, Path root) throws IOException {
        super(true, root);
    }

    abstract protected void prepareApplicationFiles(Pool files, Set<String> modules) throws IOException;

    abstract protected InputStream getResourceAsStream(String name);

    protected InputStream locateResource(String publicName, String category,
                                         String defaultName, File customFile,
                                         boolean verbose, File publicRoot) throws IOException {
        InputStream is = null;
        boolean customFromClasspath = false;
        boolean customFromFile = false;
        if (publicName != null) {
            if (publicRoot != null) {
                File publicResource = new File(publicRoot, publicName);
                if (publicResource.exists() && publicResource.isFile()) {
                    is = new FileInputStream(publicResource);
                }
            } else {
                is = getResourceAsStream(publicName);
            }
            customFromClasspath = (is != null);
        }
        if (is == null && customFile != null) {
            is = new FileInputStream(customFile);
            customFromFile = (is != null);
        }
        if (is == null && defaultName != null) {
            is = getResourceAsStream(defaultName);
        }
        String msg = null;
        if (customFromClasspath) {
            msg = MessageFormat.format(I18N.getString("message.using-custom-resource-from-classpath"), category == null ? "" : "[" + category + "] ", publicName);
        } else if (customFromFile) {
            msg = MessageFormat.format(I18N.getString("message.using-custom-resource-from-file"), category == null ? "" : "[" + category + "] ", customFile.getAbsoluteFile());
        } else if (is != null) {
            msg = MessageFormat.format(I18N.getString("message.using-default-resource-from-classpath"), category == null ? "" : "[" + category + "] ", publicName);
        } else {
            msg = MessageFormat.format(I18N.getString("message.using-default-resource"), category == null ? "" : "[" + category + "] ", publicName);
        }
        if (verbose) {
            Log.info(msg);
        }
        return is;
    }


    protected String preprocessTextResource(String publicName, String category,
                                            String defaultName, Map<String, String> pairs,
                                            boolean verbose, File publicRoot) throws IOException {
        InputStream inp = locateResource(publicName, category, defaultName, null, verbose, publicRoot);
        if (inp == null) {
            throw new RuntimeException("Module corrupt? No "+defaultName+" resource!");
        }

        try (InputStream is = inp) {
            //read fully into memory
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
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

    public void writeCfgFile(Map<String, ? super Object> params, File cfgFileName, String runtimeLocation) throws IOException {
        cfgFileName.delete();

        boolean appCDEnabled = UNLOCK_COMMERCIAL_FEATURES.fetchFrom(params) && ENABLE_APP_CDS.fetchFrom(params);
        String appCDSCacheMode = APP_CDS_CACHE_MODE.fetchFrom(params);

        PrintStream out = new PrintStream(cfgFileName);

        out.println("[Application]");
        out.println("app.name=" + APP_NAME.fetchFrom(params));
        out.println("app.mainjar=" + MAIN_JAR.fetchFrom(params).getIncludedFiles().iterator().next());
        out.println("app.version=" + VERSION.fetchFrom(params));
        out.println("app.preferences.id=" + PREFERENCES_ID.fetchFrom(params));
        out.println("app.mainclass=" +
                MAIN_CLASS.fetchFrom(params).replaceAll("\\.", "/"));
        out.println("app.classpath=" +
                String.join(File.pathSeparator, CLASSPATH.fetchFrom(params).split("[ :;]")));
        out.println("app.modulepath=" +
                String.join(File.pathSeparator, JLinkBundlerHelper.JDK_MODULE_PATH.fetchFrom(params)));
        out.println("app.runtime=" + runtimeLocation);
        out.println("app.identifier=" + IDENTIFIER.fetchFrom(params));
        if (appCDEnabled) {
            out.println("app.appcds.cache=" + appCDSCacheMode.split("\\+")[0]);
        }


        out.println();
        out.println("[JVMOptions]");
        List<String> jvmargs = JVM_OPTIONS.fetchFrom(params);
        for (String arg : jvmargs) {
            out.println(arg);
        }
        Map<String, String> jvmProps = JVM_PROPERTIES.fetchFrom(params);
        for (Map.Entry<String, String> property : jvmProps.entrySet()) {
            out.println("-D" + property.getKey() + "=" + property.getValue());
        }
        String preloader = PRELOADER_CLASS.fetchFrom(params);
        if (preloader != null) {
            out.println("-Djavafx.preloader="+preloader);
        }


        out.println();
        out.println("[JVMUserOptions]");
        Map<String, String> overridableJVMOptions = USER_JVM_OPTIONS.fetchFrom(params);
        for (Map.Entry<String, String> arg: overridableJVMOptions.entrySet()) {
            if (arg.getKey() == null || arg.getValue() == null) {
                Log.info(I18N.getString("message.jvm-user-arg-is-null"));
            } else {
                out.println(arg.getKey().replaceAll("([\\=])", "\\\\$1") + "=" + arg.getValue());
            }
        }

        if (appCDEnabled) {
            prepareAppCDS(params, out);
        }

        out.println();
        out.println("[ArgOptions]");
        List<String> args = ARGUMENTS.fetchFrom(params);
        for (String arg : args) {
            if (arg.endsWith("=") && (arg.indexOf("=") == arg.lastIndexOf("="))) {
                out.print(arg.substring(0, arg.length() - 1));
                out.println("\\=");
            } else {
                out.println(arg);
            }
        }


        out.close();
    }

    protected abstract String getCacheLocation(Map<String, ? super Object> params);

    void prepareAppCDS(Map<String, ? super Object> params, PrintStream out) throws IOException {
        //TODO check 8u40 or later

        File tempDir = Files.createTempDirectory("javapackager").toFile();
        tempDir.deleteOnExit();
        File classList = new File(tempDir, APP_FS_NAME.fetchFrom(params)  + ".classlist");

        try (FileOutputStream fos = new FileOutputStream(classList);
             PrintStream ps = new PrintStream(fos)) {
            for (String className : APP_CDS_CLASS_ROOTS.fetchFrom(params)) {
                String slashyName = className.replace(".", "/");
                ps.println(slashyName);
            }
        }
        APP_RESOURCES_LIST.fetchFrom(params).add(new RelativeFileSet(classList.getParentFile(), Arrays.asList(classList)));

        out.println();
        out.println("[AppCDSJVMOptions]");
        out.println("-XX:+UnlockCommercialFeatures");
        out.print("-XX:SharedArchiveFile=");
        out.print(getCacheLocation(params));
        out.print(APP_FS_NAME.fetchFrom(params));
        out.println(".jpa");
        out.println("-Xshare:auto");
        out.println("-XX:+UseAppCDS");
        if (Log.isDebug()) {
            out.println("-verbose:class");
            out.println("-XX:+TraceClassPaths");
            out.println("-XX:+UnlockDiagnosticVMOptions");
        }
        out.println("");

        out.println("[AppCDSGenerateCacheJVMOptions]");
        out.println("-XX:+UnlockCommercialFeatures");
        out.println("-Xshare:dump");
        out.println("-XX:+UseAppCDS");
        out.print("-XX:SharedArchiveFile=");
        out.print(getCacheLocation(params));
        out.print(APP_FS_NAME.fetchFrom(params));
        out.println(".jpa");
        out.println("-XX:SharedClassListFile=$PACKAGEDIR/" + APP_FS_NAME.fetchFrom(params) + ".classlist");
        if (Log.isDebug()) {
            out.println("-XX:+UnlockDiagnosticVMOptions");
        }
    }


}
