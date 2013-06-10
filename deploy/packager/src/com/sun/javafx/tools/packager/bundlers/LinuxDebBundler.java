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

package com.sun.javafx.tools.packager.bundlers;

import com.sun.javafx.tools.packager.Log;
import com.sun.javafx.tools.resource.linux.LinuxResources;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class LinuxDebBundler extends Bundler {
    LinuxAppBundler appBundler = new LinuxAppBundler();
    BundleParams params;
    private File configRoot = null;
    File imageDir = null;
    File configDir = null;
    File appImageRoot = null;

    private boolean menuShortcut = false;
    private boolean desktopShortcut = false;

    private final static String DEFAULT_ICON = "javalogo_white_32.png";
    private final static String DEFAULT_CONTROL_TEMPLATE = "template.control";
    private final static String DEFAULT_POSTRM_TEMPLATE = "template.postrm";
    private final static String DEFAULT_POSTINSTALL_TEMPLATE = "template.postinst";
    private final static String DEFAULT_COPYRIGHT_TEMPLATE = "template.copyright";
    private final static String DEFAULT_DESKTOP_FILE_TEMPLATE = "template.desktop";

    private final static String TOOL_DPKG = "dpkg-deb";

    public LinuxDebBundler() {
        super();
        baseResourceLoader = LinuxResources.class;
    }

    @Override
    protected void setBuildRoot(File dir) {
        super.setBuildRoot(dir);
        configRoot = new File(dir, "linux");
        configRoot.mkdirs();
        appBundler.setBuildRoot(dir);
    }

    @Override
    public void setVerbose(boolean m) {
        super.setVerbose(m);
        appBundler.setVerbose(m);
    }

    private boolean testTool(String toolName, String minVersion) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                toolName,
                "--version");
            IOUtils.exec(pb, Log.isDebug(), true); //not interested in the output
        } catch (Exception e) {
            Log.verbose("Test for ["+toolName+"]. Result: "+e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    boolean validate(BundleParams p) throws Bundler.UnsupportedPlatformException, Bundler.ConfigException {
        if (!(p.type == Bundler.BundleType.ALL || p.type == Bundler.BundleType.INSTALLER)
                 || !(p.bundleFormat == null || "deb".equals(p.bundleFormat))) {
            return false;
        }
        //run basic validation to ensure requirements are met
        //we are not interested in return code, only possible exception
        appBundler.doValidate(p);

        //NOTE: Can we validate that the required tools are available before we start?
        if (!testTool(TOOL_DPKG, "1")){
            throw new Bundler.ConfigException(
                    "Can not find " + TOOL_DPKG + ".",
                    "  Please install required packages.");
        }

        return true;
    }

    private boolean prepareProto() {
        if (!appBundler.doBundle(params, appImageRoot, true)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean bundle(BundleParams p, File outdir) {
        params = p;

        //we want to create following structure
        //   <package-name>
        //        DEBIAN
        //          control   (file with main package details)
        //          menu      (request to create menu)
        //          ... other control files if needed ....
        //        opt
        //          AppFolder (this is where app image goes)
        //             launcher executable
        //             app
        //             runtime
        String packageName =  getFullPackageName();
        imageDir = new File(new File(imagesRoot, "linux-deb.image"),
                packageName);

        configDir = new File(imageDir, "DEBIAN");
        configDir.mkdirs();

        appImageRoot = new File(imageDir, "opt");

        try {

            imageDir.mkdirs();

            menuShortcut = params.needMenu;
            desktopShortcut = params.needShortcut;
            if (!menuShortcut && !desktopShortcut) {
               //both can not be false - user will not find the app
               Log.verbose("At least one type of shortcut is required. Enabling menu shortcut.");
               menuShortcut = true;
            }

            if (prepareProto() && prepareProjectConfig()) {
                return buildDeb(outdir);
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

    protected void saveConfigFiles() {
        try {
            if (getConfig_ControlFile().exists()) {
                IOUtils.copyFile(getConfig_ControlFile(),
                        new File(configRoot, getConfig_ControlFile().getName()));
            }
            if (getConfig_CopyrightFile().exists()) {
                IOUtils.copyFile(getConfig_CopyrightFile(),
                        new File(configRoot, getConfig_CopyrightFile().getName()));
            }
            if (getConfig_PostinstallFile().exists()) {
                IOUtils.copyFile(getConfig_PostinstallFile(),
                        new File(configRoot, getConfig_PostinstallFile().getName()));
            }
            if (getConfig_PostrmFile().exists()) {
                IOUtils.copyFile(getConfig_PostrmFile(),
                        new File(configRoot, getConfig_PostrmFile().getName()));
            }
            if (getConfig_DesktopShortcutFile().exists()) {
                IOUtils.copyFile(getConfig_DesktopShortcutFile(),
                        new File(configRoot, getConfig_DesktopShortcutFile().getName()));
            }
            if (getConfig_IconFile().exists()) {
                IOUtils.copyFile(getConfig_IconFile(),
                        new File(configRoot, getConfig_IconFile().getName()));
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
        return "DEB bundler";
    }

    private String getFullPackageName() {
        return getBundleName()+"-"+getVersion();
    }

    private String getBundleName() {
        String nm;

        if (params.name != null) {
            nm = params.name;
        } else {
            nm = params.getMainClassName();
        }

        //spaces are not allowed in RPM package names
        nm = nm.replaceAll(" ", "");
        return nm;
    }

    private String getVersion() {
        if (params.appVersion != null) {
            return params.appVersion;
        } else {
            return "1.0";
        }
    }

    private String getArch() {
        String arch = System.getProperty("os.arch");
        if ("i386".equals(arch))
            return "i386";
        else
            return "amd64";
    }

    private String getLicenseText() {
        try {
            if (params.licenseFile.isEmpty()) {
                if (params.licenseType != null) {
                    return params.licenseType;
                } else {
                    return "Unknown";
                }
            }

            File appdir = new File(
                    LinuxAppBundler.getLauncher(appImageRoot, params).getParentFile(),
                    "app");
            return new String(
                    IOUtils.readFully(new File(appdir, params.licenseFile.get(0))));
        } catch (Exception e) {
            if (Log.isDebug()) {
                e.printStackTrace();
            }
            if (params.licenseType != null) {
                return params.licenseType;
            } else {
                return "Unknown";
            }
        }
    }
    
    private long getInstalledSizeKB() {
        return getInstalledSizeKB(Paths.get(appImageRoot.toURI())) >> 10;
    }

    private long getInstalledSizeKB(Path dir) {
        long count = 0;
        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(dir);
            try {
                for (Path p : stream) {
                    if (Files.isRegularFile(p)) {
                        count += Files.size(p);
                    } else if (Files.isDirectory(p)) {
                        count += getInstalledSizeKB(p);
                    }
                }
            } finally {
                stream.close();
            }
        } catch (IOException ignore) {
        }
        return count;
    }    

    private boolean prepareProjectConfig() throws IOException {
        Map<String, String> data = new HashMap<String, String>();

        data.put("APPLICATION_NAME", getBundleName());
        data.put("APPLICATION_VENDOR", params.vendor != null ? params.vendor : "Unknown");
        data.put("APPLICATION_VERSION", getVersion());
        data.put("APPLICATION_LAUNCHER_FILENAME",
                appBundler.getLauncher(imageDir, params).getName());
        data.put("DEPLOY_BUNDLE_CATEGORY",
                params.applicationCategory != null ?
                  params.applicationCategory : "Applications;");
        data.put("APPLICATION_DESCRIPTION",
                params.description != null ?
                   params.description : params.name);
        data.put("APPLICATION_SUMMARY",
                params.title != null ?
                   params.title : params.name);
        data.put("APPLICATION_COPYRIGHT",
                 params.copyright != null ? params.copyright : "Unknown");
        data.put("APPLICATION_LICENSE_TYPE",
                params.licenseType != null ? params.licenseType : "unknown");
        data.put("APPLICATION_LICENSE_TEXT", getLicenseText());
        data.put("APPLICATION_ARCH", getArch());
        data.put("APPLICATION_INSTALLED_SIZE", Long.toString(getInstalledSizeKB()));
 
        //prepare control file
        Writer w = new BufferedWriter(new FileWriter(getConfig_ControlFile()));
        String content = preprocessTextResource(
                LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_ControlFile().getName(),
                "DEB control file", DEFAULT_CONTROL_TEMPLATE, data);
        w.write(content);
        w.close();

        w = new BufferedWriter(new FileWriter(getConfig_PostinstallFile()));
        content = preprocessTextResource(
                LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_PostinstallFile().getName(),
                "DEB postinstall script", DEFAULT_POSTINSTALL_TEMPLATE, data);
        w.write(content);
        w.close();
        getConfig_PostinstallFile().setExecutable(true, false);

        w = new BufferedWriter(new FileWriter(getConfig_PostrmFile()));
        content = preprocessTextResource(
                LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_PostrmFile().getName(),
                "DEB postinstall script", DEFAULT_POSTRM_TEMPLATE, data);
        w.write(content);
        w.close();
        getConfig_PostrmFile().setExecutable(true, false);

        w = new BufferedWriter(new FileWriter(getConfig_CopyrightFile()));
        content = preprocessTextResource(
                LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_CopyrightFile().getName(),
                "DEB copyright file", DEFAULT_COPYRIGHT_TEMPLATE, data);
        w.write(content);
        w.close();
        
        //prepare desktop shortcut
        w = new BufferedWriter(new FileWriter(getConfig_DesktopShortcutFile()));
        content = preprocessTextResource(
                LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_DesktopShortcutFile().getName(),
                "Menu shortcut descriptor", DEFAULT_DESKTOP_FILE_TEMPLATE, data);
        w.write(content);
        w.close();

        //prepare installer icon
        File iconTarget = getConfig_IconFile();
        if (params.icon == null || !params.icon.exists()) {
            fetchResource(LinuxAppBundler.LINUX_BUNDLER_PREFIX + iconTarget.getName(),
                    "menu icon",
                    DEFAULT_ICON,
                    iconTarget);
        } else {
            fetchResource(LinuxAppBundler.LINUX_BUNDLER_PREFIX + iconTarget.getName(),
                    "menu icon",
                    params.icon,
                    iconTarget);
        }

        return true;
    }

    private File getConfig_DesktopShortcutFile() {
        return new File(
                LinuxAppBundler.getLauncher(appImageRoot, params).getParentFile(),
                getBundleName() + ".desktop");
    }

    private File getConfig_IconFile() {
        return new File(
                LinuxAppBundler.getLauncher(appImageRoot, params).getParentFile(),
                getBundleName() + ".png");
    }

    private File getConfig_ControlFile() {
        return new File(configDir, "control");
    }

    private File getConfig_PostinstallFile() {
        return new File(configDir, "postinst");
    }

    private File getConfig_PostrmFile() {
        return new File(configDir, "postrm");
    }

    private File getConfig_CopyrightFile() {
        return new File(configDir, "copyright");
    }

    private boolean buildDeb(File outdir) throws IOException {
        File outFile = new File(outdir, getFullPackageName()+".deb");
        Log.verbose("Generating DEB for installer to: " + outFile.getAbsolutePath());

        outFile.getParentFile().mkdirs();

        //run rpmbuild
        ProcessBuilder pb = new ProcessBuilder(
                TOOL_DPKG, "-b",  getFullPackageName(),
                outFile.getAbsolutePath());
        pb = pb.directory(imageDir.getParentFile());
        IOUtils.exec(pb, verbose);

        Log.info("Package (.deb) saved to: " + outFile.getAbsolutePath());

        return true;
    }
}
