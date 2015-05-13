/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.tools.packager.bundlers.BundleParams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.oracle.tools.packager.StandardBundlerParam.*;
import static com.oracle.tools.packager.StandardBundlerParam.ARGUMENTS;

/**
 * Common utility methods used by app image bundlers.
 */
public abstract class AbstractImageBundler extends AbstractBundler {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle(AbstractImageBundler.class.getName());

    public static final String CFG_FORMAT_PROPERTIES="prop";
    public static final String CFG_FORMAT_INI="ini";
    
    public static final BundlerParamInfo<String> LAUNCHER_CFG_FORMAT =
            new StandardBundlerParam<>(
                    I18N.getString("param.launcher-cfg-format.name"),
                    I18N.getString("param.launcher-cfg-format.description"),
                    "launcher-cfg-format",
                    String.class,
                    params -> "ini",
                    (s, p) -> s);

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

    public void imageBundleValidation(Map<String, ? super Object> p) throws ConfigException {
        StandardBundlerParam.validateMainClassInfoFromAppResources(p);

        Map<String, String> userJvmOptions = USER_JVM_OPTIONS.fetchFrom(p);
        if (userJvmOptions != null) {
            for (Map.Entry<String, String> entry : userJvmOptions.entrySet()) {
                if (entry.getValue() == null || entry.getValue().isEmpty()) {
                    throw new ConfigException(
                            MessageFormat.format(I18N.getString("error.empty-user-jvm-option-value"), entry.getKey()),
                            I18N.getString("error.empty-user-jvm-option-value.advice"));
                }
            }
        }

        if (MAIN_JAR.fetchFrom(p) == null) {
            throw new ConfigException(
                    I18N.getString("error.no-application-jar"),
                    I18N.getString("error.no-application-jar.advice"));
        }
        
        extractRuntimeFlags(p);
        
        if (ENABLE_APP_CDS.fetchFrom(p)) {
            if (UNLOCK_COMMERCIAL_FEATURES.fetchFrom(p)) {
                if (p.containsKey(BundleParams.PARAM_RUNTIME)
                        && (p.get(BundleParams.PARAM_RUNTIME) == null)) 
                {
                    throw new ConfigException(
                            I18N.getString("error.app-cds-requires-runtime"),
                            I18N.getString("error.app-cds-requires-runtime.advice"));
                }
                Object majorV = p.get(".runtime.version.major");
                Object minorV = p.get(".runtime.version.minor");
                if (majorV != null && minorV != null) {
                    try {
                        int major = Integer.parseInt(majorV.toString());
                        int minor = Integer.parseInt(minorV.toString());
                        if ((major < 8) || (major == 8 && minor < 40)) {
                            throw new ConfigException(
                                    I18N.getString("error.app-cds-bad-version"),
                                    I18N.getString("error.app-cds-bad-version.advice"));
                        }
                    } catch (NumberFormatException nfe) {
                        //maybe log a failure to check versions?
                    }
                }
            } else {
                throw new ConfigException(
                        I18N.getString("error.app-cds-no-commercial-unlock"),
                        I18N.getString("error.app-cds-no-commercial-unlock.advice"));
            }
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

    abstract public void extractRuntimeFlags(Map<String, ? super Object> params); 
    
    public static void extractFlagsFromVersion(Map<String, ? super Object> params, String versionOutput) {
        Pattern bitArchPattern = Pattern.compile("(\\d*)[- ]?[bB]it");
        Matcher matcher = bitArchPattern.matcher(versionOutput);
        if (matcher.find()) {
            params.put(".runtime.bit-arch", matcher.group(1));
        } else {
            // presume 32 bit on no match
            params.put(".runtime.bit-arch", "32");
        }

        Pattern oldVersionMatcher = Pattern.compile("java version \"((\\d+.(\\d+).\\d+)(_(\\d+)))?(-(.*))?\"");
        matcher = oldVersionMatcher.matcher(versionOutput);
        if (matcher.find()) {
            params.put(".runtime.version", matcher.group(1));
            params.put(".runtime.version.release", matcher.group(2));
            params.put(".runtime.version.major", matcher.group(3));
            params.put(".runtime.version.update", matcher.group(5));
            params.put(".runtime.version.minor", matcher.group(5));
            params.put(".runtime.version.security", matcher.group(5));
            params.put(".runtime.version.patch", "0");
            params.put(".runtime.version.modifiers", matcher.group(7));
        } else {
            Pattern newVersionMatcher = Pattern.compile("java version \"((\\d+).(\\d+).(\\d+).(\\d+))(-(.*))?(\\+[^\"]*)?\"");
            matcher = newVersionMatcher.matcher(versionOutput);
            if (matcher.find()) {
                params.put(".runtime.version", matcher.group(1));
                params.put(".runtime.version.release", matcher.group(1));
                params.put(".runtime.version.major", matcher.group(2));
                params.put(".runtime.version.update", matcher.group(3));
                params.put(".runtime.version.minor", matcher.group(3));
                params.put(".runtime.version.security", matcher.group(4));
                params.put(".runtime.version.patch", matcher.group(5));
                params.put(".runtime.version.modifiers", matcher.group(7));
            } else {
                params.put(".runtime.version", "");
                params.put(".runtime.version.release", "");
                params.put(".runtime.version.major", "");
                params.put(".runtime.version.update", "");
                params.put(".runtime.version.minor", "");
                params.put(".runtime.version.security", "");
                params.put(".runtime.version.patch", "");
                params.put(".runtime.version.modifiers", "");
            }
        }
    }


}
