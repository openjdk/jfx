/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.bundlers.AbstractBundler;
import com.oracle.bundlers.BundlerParamInfo;
import com.oracle.bundlers.StandardBundlerParam;
import com.oracle.bundlers.windows.WindowsBundlerParam;
import com.sun.javafx.tools.packager.Log;
import com.sun.javafx.tools.resource.windows.WinResources;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.oracle.bundlers.StandardBundlerParam.SERVICE_HINT;
import static com.oracle.bundlers.StandardBundlerParam.VERBOSE;
import static com.oracle.bundlers.windows.WindowsBundlerParam.*;

public class WinExeBundler extends AbstractBundler {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle("com.oracle.bundlers.windows.WinExeBundler");
    
    public static final BundlerParamInfo<WinAppBundler> APP_BUNDLER = new WindowsBundlerParam<>(
            I18N.getString("param.app-bundler.name"),
            I18N.getString("param.app-bundler.description"),
            "win.app.bundler",
            WinAppBundler.class,
            params -> new WinAppBundler(),
            null);

    public static final BundlerParamInfo<WinServiceBundler> SERVICE_BUNDLER = new WindowsBundlerParam<>(
            I18N.getString("param.service-bundler.name"),
            I18N.getString("param.service-bundler.description"),
            "win.service.bundler",
            WinServiceBundler.class,
            params -> new WinServiceBundler(),
            null);
    
    public static final BundlerParamInfo<File> CONFIG_ROOT = new WindowsBundlerParam<>(
            I18N.getString("param.config-root.name"),
            I18N.getString("param.config-root.description"),
            "configRoot",
            File.class, params -> {
                File imagesRoot = new File(BUILD_ROOT.fetchFrom(params), "windows");
                imagesRoot.mkdirs();
                return imagesRoot;
            },
            (s, p) -> null);

    //default for .exe is user level installation
    // only do system wide if explicitly requested
    public static final StandardBundlerParam<Boolean> EXE_SYSTEM_WIDE  =
            new StandardBundlerParam<>(
                    I18N.getString("param.system-wide.name"),
                    I18N.getString("param.system-wide.description"),
                    "win.exe." + BundleParams.PARAM_SYSTEM_WIDE,
                    Boolean.class,
                    params -> params.containsKey(SYSTEM_WIDE.getID())
                                ? SYSTEM_WIDE.fetchFrom(params)
                                : false, // EXEs default to user local install
                    (s, p) -> (s == null || "null".equalsIgnoreCase(s))? null : Boolean.valueOf(s) // valueOf(null) is false, and we actually do want null
            );

    public static final BundlerParamInfo<File> EXE_IMAGE_DIR = new WindowsBundlerParam<>(
            I18N.getString("param.image-dir.name"),
            I18N.getString("param.image-dir.description"),
            "win.exe.imageDir",
            File.class,
            params -> {
                File imagesRoot = IMAGES_ROOT.fetchFrom(params);
                if (!imagesRoot.exists()) imagesRoot.mkdirs();
                return new File(imagesRoot, "win-exe.image");
            },
            (s, p) -> null);

    private final static String DEFAULT_EXE_PROJECT_TEMPLATE = "template.iss";
    private static final String TOOL_INNO_SETUP_COMPILER = "iscc.exe";

    public static final BundlerParamInfo<String> TOOL_INNO_SETUP_COMPILER_EXECUTABLE = new WindowsBundlerParam<>(
            I18N.getString("param.iscc-path.name"),
            I18N.getString("param.iscc-path.description"),
            "win.exe.iscc.exe",
            String.class,
            params -> {
                for (String dirString : (System.getenv("PATH") + ";C:\\Program Files (x86)\\Inno Setup 5;C:\\Program Files\\Inno Setup 5").split(";")) {
                    File f = new File(dirString.replace("\"", ""), TOOL_INNO_SETUP_COMPILER);
                    if (f.isFile()) {
                        return f.toString();
                    }
                }
                return null;
            },
            null);

    public WinExeBundler() {
        super();
        baseResourceLoader = WinResources.class;
    }

    @Override
    public String getName() {
        return I18N.getString("bundler.name");
    }

    @Override
    public String getDescription() {
        return I18N.getString("bundler.description");
    }

    @Override
    public String getID() {
        return "exe";
    }

    @Override
    public String getBundleType() {
        return "INSTALLER";
    }

    @Override
    public Collection<BundlerParamInfo<?>> getBundleParameters() {
        Collection<BundlerParamInfo<?>> results = new LinkedHashSet<>();
        results.addAll(WinAppBundler.getAppBundleParameters());
        results.addAll(getExeBundleParameters());
        return results;
    }

    public static Collection<BundlerParamInfo<?>> getExeBundleParameters() {
        return Arrays.asList(
                APP_BUNDLER,
                APP_RESOURCES,
                BUILD_ROOT,
                //CONFIG_ROOT, // duplicate from getAppBundleParameters
                DESCRIPTION,
                COPYRIGHT,
                EXE_SYSTEM_WIDE,
                IDENTIFIER,
                EXE_IMAGE_DIR,
                IMAGES_ROOT,
                LICENSE_FILE,
                MENU_GROUP,
                MENU_HINT,
                SHORTCUT_HINT,
                SERVICE_HINT,
                START_ON_INSTALL,
                STOP_ON_UNINSTALL,
                RUN_AT_STARTUP,
                TITLE,
                VENDOR,
                VERSION
        );
    }

    @Override
    public File execute(Map<String, ? super Object> params, File outputParentDir) {
        return bundle(params, outputParentDir);
    }

    static class VersionExtractor extends PrintStream {
        double version = 0f;

        public VersionExtractor() {
            super(new ByteArrayOutputStream());
        }

        double getVersion() {
            if (version == 0f) {
                String content = new String(((ByteArrayOutputStream) out).toByteArray());
                Pattern pattern = Pattern.compile("Inno Setup (\\d+.?\\d*)");
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    String v = matcher.group(1);
                    version = new Double(v);
                }
            }
            return version;
        }
    }

    private static double findToolVersion(String toolName) {
        try {
            if (toolName == null || "".equals(toolName)) return 0f;

            ProcessBuilder pb = new ProcessBuilder(
                    toolName,
                    "/?");
            VersionExtractor ve = new VersionExtractor();
            IOUtils.exec(pb, Log.isDebug(), true, ve); //not interested in the output
            double version = ve.getVersion();
            Log.verbose(MessageFormat.format(I18N.getString("message.tool-version"), toolName, version));
            return version;
        } catch (Exception e) {
            if (Log.isDebug()) {
                e.printStackTrace();
            }
            return 0f;
        }
    }

    @Override
    public boolean validate(Map<String, ? super Object> p) throws UnsupportedPlatformException, ConfigException {
        try {
            if (p == null) throw new ConfigException(I18N.getString("error.parameters-null"), I18N.getString("error.parameters-null.advice"));

            //run basic validation to ensure requirements are met
            //we are not interested in return code, only possible exception
            APP_BUNDLER.fetchFrom(p).validate(p);

            // make sure some key values don't have newlines
            for (BundlerParamInfo<String> pi : Arrays.asList(
                    APP_NAME,
                    COPYRIGHT,
                    DESCRIPTION,
                    MENU_GROUP,
                    TITLE,
                    VENDOR,
                    VERSION)
            ) {
                String v = pi.fetchFrom(p);
                if (v.contains("\n") | v.contains("\r")) {
                    throw new ConfigException("Parmeter '" + pi.getID() + "' cannot contain a newline.",
                            "Change the value of '" + pi.getID() + " so that it does not contain any newlines");
                }
            }

            //exe bundlers trim the copyright to 100 characters, tell them this will happen
            if (COPYRIGHT.fetchFrom(p).length() > 100) {
                throw new ConfigException(
                        I18N.getString("error.copyright-is-too-long"),
                        I18N.getString("error.copyright-is-too-long.advice"));
            }

            // validate license file, if used, exists in the proper place
            if (p.containsKey(LICENSE_FILE.getID())) {
                RelativeFileSet appResources = APP_RESOURCES.fetchFrom(p);
                for (String license : LICENSE_FILE.fetchFrom(p)) {
                    if (!appResources.contains(license)) {
                        throw new ConfigException(
                                I18N.getString("error.license-missing"),
                                MessageFormat.format(I18N.getString("error.license-missing.advice"),
                                        license, appResources.getBaseDirectory().toString()));
                    }
                }
            }


            if (SERVICE_HINT.fetchFrom(p)) {
                SERVICE_BUNDLER.fetchFrom(p).validate(p);
            }

            double innoVersion = findToolVersion(TOOL_INNO_SETUP_COMPILER_EXECUTABLE.fetchFrom(p));

            //Inno Setup 5+ is required
            double minVersion = 5.0f;

            if (innoVersion < minVersion) {
                Log.info(MessageFormat.format(I18N.getString("message.tool-wrong-version"), TOOL_INNO_SETUP_COMPILER, innoVersion, minVersion));
                throw new ConfigException(
                        I18N.getString("error.iscc-not-found"),
                        I18N.getString("error.iscc-not-found.advice"));
            }

            return true;
        } catch (RuntimeException re) {
            throw new ConfigException(re);
        }
    }

    private boolean prepareProto(Map<String, ? super Object> params) throws IOException {
        File imageDir = EXE_IMAGE_DIR.fetchFrom(params);
        File appOutputDir = APP_BUNDLER.fetchFrom(params).doBundle(params, imageDir, true);
        if (appOutputDir == null) {
            return false;
        }
        List<String> licenseFiles = LICENSE_FILE.fetchFrom(params);
        if (licenseFiles != null) {
            RelativeFileSet appRoot = APP_RESOURCES.fetchFrom(params);
            //need to copy license file to the root of win-app.image
            for (String s : licenseFiles) {
                File lfile = new File(appRoot.getBaseDirectory(), s);
                IOUtils.copyFile(lfile, new File(imageDir, lfile.getName()));
            }
        }
        
        if (SERVICE_HINT.fetchFrom(params)) {
            // copies the service launcher to the app root folder
            appOutputDir = SERVICE_BUNDLER.fetchFrom(params).doBundle(params, appOutputDir, true);
            if (appOutputDir == null) {
                return false;
            }
        }
        return true;
    }

    public File bundle(Map<String, ? super Object> p, File outputDirectory) {
        if (!outputDirectory.isDirectory() && !outputDirectory.mkdirs()) {
            throw new RuntimeException(MessageFormat.format(I18N.getString("error.cannot-create-output-dir"), outputDirectory.getAbsolutePath()));
        }
        if (!outputDirectory.canWrite()) {
            throw new RuntimeException(MessageFormat.format(I18N.getString("error.cannot-write-to-output-dir"), outputDirectory.getAbsolutePath()));
        }

        // validate we have valid tools before continuing
        String iscc = TOOL_INNO_SETUP_COMPILER_EXECUTABLE.fetchFrom(p);
        if (iscc == null || !new File(iscc).isFile()) {
            Log.info(I18N.getString("error.iscc-not-found"));
            Log.info(MessageFormat.format(I18N.getString("message.iscc-file-string"), iscc));
            return null;
        }

        File imageDir = EXE_IMAGE_DIR.fetchFrom(p);
        try {
            imageDir.mkdirs();

            boolean menuShortcut = MENU_HINT.fetchFrom(p);
            boolean desktopShortcut = SHORTCUT_HINT.fetchFrom(p);
            if (!menuShortcut && !desktopShortcut) {
                //both can not be false - user will not find the app
                Log.verbose(I18N.getString("message.one-shortcut-required"));
                p.put(MENU_HINT.getID(), true);
            }

            if (prepareProto(p) && prepareProjectConfig(p)) {
                File configScript = getConfig_Script(p);
                if (configScript.exists()) {
                    Log.info(MessageFormat.format(I18N.getString("message.running-wsh-script"), configScript.getAbsolutePath()));
                    IOUtils.run("wscript", configScript, VERBOSE.fetchFrom(p));
                }
                return buildEXE(p, outputDirectory);
            }
            return null;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            try {
                if (VERBOSE.fetchFrom(p)) {
                    saveConfigFiles(p);
                }
                if (imageDir != null && !Log.isDebug()) {
                    IOUtils.deleteRecursive(imageDir);
                } else if (imageDir != null) {
                    Log.info(MessageFormat.format(I18N.getString("message.debug-working-directory"), imageDir.getAbsolutePath()));
                }
            } catch (FileNotFoundException ex) {
                //noinspection ReturnInsideFinallyBlock
                return null;
            }
        }
    }

    //name of post-image script
    private File getConfig_Script(Map<String, ? super Object> params) {
        return new File(EXE_IMAGE_DIR.fetchFrom(params), APP_NAME.fetchFrom(params) + "-post-image.wsf");
    }

    protected void saveConfigFiles(Map<String, ? super Object> params) {
        try {
            File configRoot = CONFIG_ROOT.fetchFrom(params);
            if (getConfig_ExeProjectFile(params).exists()) {
                IOUtils.copyFile(getConfig_ExeProjectFile(params),
                        new File(configRoot, getConfig_ExeProjectFile(params).getName()));
            }
            if (getConfig_Script(params).exists()) {
                IOUtils.copyFile(getConfig_Script(params),
                        new File(configRoot, getConfig_Script(params).getName()));
            }
            if (getConfig_SmallInnoSetupIcon(params).exists()) {
                IOUtils.copyFile(getConfig_SmallInnoSetupIcon(params),
                        new File(configRoot, getConfig_SmallInnoSetupIcon(params).getName()));
            }
            Log.info(MessageFormat.format(I18N.getString("message.config-save-location"), configRoot.getAbsolutePath()));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private String getAppIdentifier(Map<String, ? super Object> params) {
        String nm = IDENTIFIER.fetchFrom(params);

        //limitation of innosetup
        if (nm.length() > 126)
            nm = nm.substring(0, 126);

        return nm;
    }


    private String getLicenseFile(Map<String, ? super Object> params) {
        List<String> licenseFiles = LICENSE_FILE.fetchFrom(params);
        if (licenseFiles == null || licenseFiles.isEmpty()) {
            return "";
        } else {
            return licenseFiles.get(0);
        }
    }

    void validateValueAndPut(Map<String, String> data, String key, BundlerParamInfo<String> param, Map<String, ? super Object> params) throws IOException {
        String value = param.fetchFrom(params);
        if (value.contains("\r") || value.contains("\n")) {
            throw new IOException("Configuration Parameter " + param.getID() + " cannot contain multiple lines of text");
        }
        data.put(key, innosetupEscape(value));
    }

    private String innosetupEscape(String value) {
        if (value.contains("\"") || !value.trim().equals(value)) {
            value = "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    boolean prepareMainProjectFile(Map<String, ? super Object> params) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("PRODUCT_APP_IDENTIFIER", innosetupEscape(getAppIdentifier(params)));

        validateValueAndPut(data, "APPLICATION_NAME", APP_NAME, params);

        validateValueAndPut(data, "APPLICATION_VENDOR", VENDOR, params);
        validateValueAndPut(data, "APPLICATION_VERSION", VERSION, params); // TODO make our own version paraminfo?
        
        data.put("APPLICATION_LAUNCHER_FILENAME",
                innosetupEscape(WinAppBundler.getLauncher(EXE_IMAGE_DIR.fetchFrom(params), params).getName()));

        data.put("APPLICATION_DESKTOP_SHORTCUT", SHORTCUT_HINT.fetchFrom(params) ? "returnTrue" : "returnFalse");
        data.put("APPLICATION_MENU_SHORTCUT", MENU_HINT.fetchFrom(params) ? "returnTrue" : "returnFalse");
        validateValueAndPut(data, "APPLICATION_GROUP", MENU_GROUP, params);
        validateValueAndPut(data, "APPLICATION_COMMENTS", TITLE, params); // TODO this seems strange, at least in name
        validateValueAndPut(data, "APPLICATION_COPYRIGHT", COPYRIGHT, params);

        data.put("APPLICATION_LICENSE_FILE", innosetupEscape(getLicenseFile(params)));

        if (EXE_SYSTEM_WIDE.fetchFrom(params)) {
            data.put("APPLICATION_INSTALL_ROOT", "{pf}");
            data.put("APPLICATION_INSTALL_PRIVILEGE", "admin");
        } else {
            data.put("APPLICATION_INSTALL_ROOT", "{localappdata}");
            data.put("APPLICATION_INSTALL_PRIVILEGE", "lowest");
        }

        if (BIT_ARCH_64.fetchFrom(params)) {
            data.put("ARCHITECTURE_BIT_MODE", "x64");
        } else {
            data.put("ARCHITECTURE_BIT_MODE", "");
        }

        if (SERVICE_HINT.fetchFrom(params)) {
            data.put("RUN_FILENAME", innosetupEscape(WinServiceBundler.getAppSvcName(params)));
        } else {
            validateValueAndPut(data, "RUN_FILENAME", APP_NAME, params);
        }
        validateValueAndPut(data, "APPLICATION_DESCRIPTION", DESCRIPTION, params);
        data.put("APPLICATION_SERVICE", SERVICE_HINT.fetchFrom(params) ? "returnTrue" : "returnFalse");
        data.put("APPLICATION_NOT_SERVICE", SERVICE_HINT.fetchFrom(params) ? "returnFalse" : "returnTrue");
        data.put("START_ON_INSTALL", START_ON_INSTALL.fetchFrom(params) ? "-startOnInstall" : "");
        data.put("STOP_ON_UNINSTALL", STOP_ON_UNINSTALL.fetchFrom(params) ? "-stopOnUninstall" : "");
        data.put("RUN_AT_STARTUP", RUN_AT_STARTUP.fetchFrom(params) ? "-runAtStartup" : "");        

        Writer w = new BufferedWriter(new FileWriter(getConfig_ExeProjectFile(params)));
        String content = preprocessTextResource(
                WinAppBundler.WIN_BUNDLER_PREFIX + getConfig_ExeProjectFile(params).getName(),
                I18N.getString("resource.inno-setup-project-file"), DEFAULT_EXE_PROJECT_TEMPLATE, data,
                VERBOSE.fetchFrom(params));
        w.write(content);
        w.close();
        return true;
    }

    private final static String DEFAULT_INNO_SETUP_ICON = "icon_inno_setup.bmp";

    private boolean prepareProjectConfig(Map<String, ? super Object> params) throws IOException {
        prepareMainProjectFile(params);

        //prepare installer icon
        File iconTarget = getConfig_SmallInnoSetupIcon(params);
        fetchResource(WinAppBundler.WIN_BUNDLER_PREFIX + iconTarget.getName(),
                I18N.getString("resource.setup-icon"),
                DEFAULT_INNO_SETUP_ICON,
                iconTarget,
                VERBOSE.fetchFrom(params));

        fetchResource(WinAppBundler.WIN_BUNDLER_PREFIX + getConfig_Script(params).getName(),
                I18N.getString("resource.post-install-script"),
                (String) null,
                getConfig_Script(params),
                VERBOSE.fetchFrom(params));
        return true;
    }

    private File getConfig_SmallInnoSetupIcon(Map<String, ? super Object> params) {
        return new File(EXE_IMAGE_DIR.fetchFrom(params),
                APP_NAME.fetchFrom(params) + "-setup-icon.bmp");
    }

    private File getConfig_ExeProjectFile(Map<String, ? super Object> params) {
        return new File(EXE_IMAGE_DIR.fetchFrom(params),
                APP_NAME.fetchFrom(params) + ".iss");
    }


    private File buildEXE(Map<String, ? super Object> params, File outdir) throws IOException {
        Log.verbose(MessageFormat.format(I18N.getString("message.outputting-to-location"), outdir.getAbsolutePath()));

        outdir.mkdirs();

        //run candle
        ProcessBuilder pb = new ProcessBuilder(
                TOOL_INNO_SETUP_COMPILER_EXECUTABLE.fetchFrom(params),
                "/o"+outdir.getAbsolutePath(),
                getConfig_ExeProjectFile(params).getAbsolutePath());
        pb = pb.directory(EXE_IMAGE_DIR.fetchFrom(params));
        IOUtils.exec(pb, VERBOSE.fetchFrom(params));

        Log.info(MessageFormat.format(I18N.getString("message.output-location"), outdir.getAbsolutePath()));

        // presume the result is the ".exe" file with the newest modified time
        // not the best solution, but it is the most reliable
        File result = null;
        long lastModified = 0;
        File[] list = outdir.listFiles();
        if (list != null) {
            for (File f : list) {
                if (f.getName().endsWith(".exe") && f.lastModified() > lastModified) {
                    result = f;
                    lastModified = f.lastModified();
                }
            }
        }

        return result;
    }
}
