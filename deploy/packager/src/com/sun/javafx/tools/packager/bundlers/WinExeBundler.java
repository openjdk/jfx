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
import com.sun.javafx.tools.resource.windows.WinResources;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WinExeBundler extends Bundler {
    WinAppBundler appBundler = new WinAppBundler();
    BundleParams params;
    private File configRoot = null;
    File imageDir = null;
    private boolean menuShortcut = false;
    private boolean desktopShortcut = false;

    private final static String DEFAULT_EXE_PROJECT_TEMPLATE = "template.iss";
    private static final String TOOL_INNO_SETUP_COMPILER = "iscc.exe";

    public WinExeBundler() {
        super();
        baseResourceLoader = WinResources.class;
    }

    @Override
    protected void setBuildRoot(File dir) {
        super.setBuildRoot(dir);
        configRoot = new File(dir, "windows");
        configRoot.mkdirs();
        appBundler.setBuildRoot(dir);
    }

    @Override
    public void setVerbose(boolean m) {
        super.setVerbose(m);
        appBundler.setVerbose(m);
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

    private static double findTool(String toolName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                toolName,
                "/?");
            VersionExtractor ve = new VersionExtractor();
            IOUtils.exec(pb, Log.isDebug(), true, ve); //not interested in the output
            double version = ve.getVersion();
            Log.verbose("  Detected ["+toolName+"] version [" + version + "]");
            return version;
        } catch (Exception e) {
            if (Log.isDebug()) {
                e.printStackTrace();
            }
            return 0f;
        }
    }

    @Override
    boolean validate(BundleParams p) throws Bundler.UnsupportedPlatformException, Bundler.ConfigException {
        if (!(p.type == Bundler.BundleType.ALL || p.type == Bundler.BundleType.INSTALLER)
                 || !(p.bundleFormat == null || "exe".equals(p.bundleFormat))) {
            return false;
        }
        //run basic validation to ensure requirements are met
        //we are not interested in return code, only possible exception
        appBundler.doValidate(p);

        double innoVersion = findTool(TOOL_INNO_SETUP_COMPILER);

        //Inno Setup 5+ is required
        double minVersion = 5.0f;

        if (innoVersion < minVersion) {
            Log.info("Detected ["+TOOL_INNO_SETUP_COMPILER+"] version "+innoVersion +
                        " but version "+minVersion+" is required.");
            throw new Bundler.ConfigException(
                    "Can not find Inno Setup Compiler (iscc.exe).",
                    "  Download Inno Setup 5 or later from http://www.jrsoftware.org and add it to the PATH.");
        }

        return true;
    }

    private boolean prepareProto() throws IOException {
        if (!appBundler.doBundle(params, imageDir, true)) {
            return false;
        }
        if (!params.licenseFile.isEmpty()) {
            //need to copy license file to the root of win-app.image
            File lfile = new File(params.appResources.getBaseDirectory(),
                    params.licenseFile.get(0));
            IOUtils.copyFile(lfile, new File(imageDir, lfile.getName()));
        }
        return true;
    }

    @Override
    public boolean bundle(BundleParams p, File outdir) {
        imageDir = new File(imagesRoot, "win-app.image");
        try {
            params = p;

            imageDir.mkdirs();

            menuShortcut = params.needMenu;
            desktopShortcut = params.needShortcut;
            if (!menuShortcut && !desktopShortcut) {
               //both can not be false - user will not find the app
               Log.verbose("At least one type of shortcut is required. Enabling menu shortcut.");
               menuShortcut = true;
            }

            if (prepareProto() && prepareProjectConfig()) {
                File configScript = getConfig_Script();
                if (configScript.exists()) {
                    Log.info("Running WSH script on application image [" +
                            configScript.getAbsolutePath() + "]");
                    IOUtils.run("wscript", configScript, verbose);
                }
                return buildEXE(outdir);
            }
            return false;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try {
                if (verbose) {
                    saveConfigFiles();
                }
                if (imageDir != null && !Log.isDebug()) {
                    IOUtils.deleteRecursive(imageDir);
                } else if (imageDir != null) {
                    Log.info("Kept working directory for debug: "
                            + imageDir.getAbsolutePath());
                }
             } catch (FileNotFoundException ex) {
                return false;
            }
        }
    }

    //name of post-image script
    private File getConfig_Script() {
        return new File(imageDir, WinAppBundler.getAppName(params) + "-post-image.wsf");
    }

    protected void saveConfigFiles() {
        try {
            if (getConfig_ExeProjectFile().exists()) {
                IOUtils.copyFile(getConfig_ExeProjectFile(),
                        new File(configRoot, getConfig_ExeProjectFile().getName()));
            }
            if (getConfig_Script().exists()) {
                IOUtils.copyFile(getConfig_Script(),
                        new File(configRoot, getConfig_Script().getName()));
            }
            if (getConfig_SmallInnoSetupIcon().exists()) {
                IOUtils.copyFile(getConfig_SmallInnoSetupIcon(),
                        new File(configRoot, getConfig_SmallInnoSetupIcon().getName()));
            }
            Log.info("  Config files are saved to "
                    + configRoot.getAbsolutePath()
                    + ". Use them to customize package.");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "Exe Bundler (based on Inno Setup)";
    }

    private String getAppIdentifier() {
        String nm  = null;
        if (params.identifier != null) {
            nm = params.identifier;
        } else {
            nm = params.getMainClassName();
        }

        //limitation of innosetup
        if (nm.length() > 126)
            nm = nm.substring(0, 126);

        return nm;
    }

    private String getGroup() {
        if (params.applicationCategory != null) {
            return params.applicationCategory;
        } else {
            if (params.vendor != null) {
                return params.vendor;
            } else {
                return "Unknown";
            }
        }
    }

    private String getLicenseFile() {
        if (params.licenseFile.isEmpty()) {
            return "";
        }
        return params.licenseFile.get(0);
    }

    private String getDescription() {
        if (params.description != null) {
            //strip quotes if any
            return params.description.replaceAll("\"", "'");
        }
        return "none";
    }

    boolean prepareMainProjectFile(File imageDir) throws IOException {
        Map<String, String> data = new HashMap<String, String>();
        data.put("PRODUCT_APP_IDENTIFIER", getAppIdentifier());

        data.put("APPLICATION_NAME", WinAppBundler.getAppName(params));
        data.put("APPLICATION_VENDOR",
                params.vendor != null ? params.vendor : "Unknown");
        data.put("APPLICATION_VERSION",
                params.appVersion != null ? params.appVersion : "1.0");
        data.put("APPLICATION_LAUNCHER_FILENAME",
                appBundler.getLauncher(imageDir, params).getName());
        data.put("APPLICATION_DESKTOP_SHORTCUT",
                desktopShortcut ? "returnTrue" : "returnFalse");
        data.put("APPLICATION_MENU_SHORTCUT",
                menuShortcut ? "returnTrue" : "returnFalse");
        data.put("APPLICATION_GROUP", getGroup());
        data.put("APPLICATION_COMMENTS",
                params.title != null ? params.title : "");
        data.put("APPLICATION_COPYRIGHT",
                params.copyright != null ? params.copyright : "");

        data.put("APPLICATION_LICENSE_FILE", getLicenseFile());

        //default for .exe is user level installation
        // only do system wide if explicitly requested
        if (params.systemWide != null && params.systemWide) {
            data.put("APPLICATION_INSTALL_ROOT", "{pf}");
            data.put("APPLICATION_INSTALL_PRIVILEGE", "admin");
        } else {
            data.put("APPLICATION_INSTALL_ROOT", "{localappdata}");
            data.put("APPLICATION_INSTALL_PRIVILEGE", "lowest");
        }

        Writer w = new BufferedWriter(new FileWriter(getConfig_ExeProjectFile()));
        String content = preprocessTextResource(
                WinAppBundler.WIN_BUNDLER_PREFIX + getConfig_ExeProjectFile().getName(),
                "Inno Setup project file", DEFAULT_EXE_PROJECT_TEMPLATE, data);
        w.write(content);
        w.close();
        return true;
    }

    private final static String DEFAULT_INNO_SETUP_ICON = "icon_inno_setup.bmp";

    private boolean prepareProjectConfig() throws IOException {
        prepareMainProjectFile(imageDir);

        //prepare installer icon
        File iconTarget = getConfig_SmallInnoSetupIcon();
        fetchResource(WinAppBundler.WIN_BUNDLER_PREFIX + iconTarget.getName(),
                "setup dialog icon",
                DEFAULT_INNO_SETUP_ICON,
                iconTarget);

        fetchResource(WinAppBundler.WIN_BUNDLER_PREFIX + getConfig_Script().getName(),
                "script to run after application image is populated",
                (String) null,
                getConfig_Script());
        return true;
    }

    private File getConfig_SmallInnoSetupIcon() {
        return new File(imageDir,
                WinAppBundler.getAppName(params) + "-setup-icon.bmp");
    }

    private File getConfig_ExeProjectFile() {
        return new File(imageDir,
                WinAppBundler.getAppName(params) + ".iss");
    }


    private boolean buildEXE(File outdir) throws IOException {
        Log.verbose("Generating EXE for installer to: " + outdir.getAbsolutePath());

        outdir.mkdirs();

        //run candle
        ProcessBuilder pb = new ProcessBuilder(
                TOOL_INNO_SETUP_COMPILER,
                "/o"+outdir.getAbsolutePath(),
                getConfig_ExeProjectFile().getAbsolutePath());
        pb = pb.directory(imageDir);
        IOUtils.exec(pb, verbose);

        Log.info("Installer (.exe) saved to: " + outdir.getAbsolutePath());

        return true;
    }
}
