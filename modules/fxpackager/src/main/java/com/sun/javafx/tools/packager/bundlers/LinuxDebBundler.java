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
import com.sun.javafx.tools.packager.Log;
import com.sun.javafx.tools.resource.linux.LinuxResources;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.oracle.bundlers.StandardBundlerParam.*;

public class LinuxDebBundler extends AbstractBundler {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle("com.oracle.bundlers.linux.LinuxDebBundler");

    public static final BundlerParamInfo<LinuxAppBundler> APP_BUNDLER = new StandardBundlerParam<>(
            I18N.getString("param.app-bundler.name"),
            I18N.getString("param.app-bundler.description"),
            "linuxAppBundler",  //KEY
            LinuxAppBundler.class, null, params -> new LinuxAppBundler(), false, s -> null);

    public static final BundlerParamInfo<String> BUNDLE_NAME = new StandardBundlerParam<> (
            I18N.getString("param.bundle-name.name"),
            I18N.getString("param.bundle-name.description"),
            "bundleName",  //KEY
            String.class, null, params -> {
                String nm = APP_NAME.fetchFrom(params);
                if (nm == null) return null;
        
                //spaces are not allowed in RPM package names
                nm = nm.replaceAll(" ", "");
                return nm;
        
            }, false, s -> s);

    public static final BundlerParamInfo<String> FULL_PACKAGE_NAME = new StandardBundlerParam<> (
            I18N.getString("param.full-package-name.name"),
            I18N.getString("param.full-package-name.description"),
            "fullPackageName",  //KEY
            String.class, null,
            params -> APP_NAME.fetchFrom(params) + "-" + VERSION.fetchFrom(params),
            false, s -> s);

    public static final BundlerParamInfo<File> CONFIG_ROOT = new StandardBundlerParam<>(
            I18N.getString("param.config-root.name"),
            I18N.getString("param.config-root.description"),
            "configRoot", //KEY
            File.class, null, params ->  new File(BUILD_ROOT.fetchFrom(params), "linux"),
            false, s -> new File(s));

    public static final BundlerParamInfo<File> IMAGE_DIR = new StandardBundlerParam<>(
            I18N.getString("param.image-dir.name"), 
            I18N.getString("param.image-dir.description"),
            "imageDir",  //KEY
            File.class, null, params -> {
                File imagesRoot = IMAGES_ROOT.fetchFrom(params);
                return new File(new File(imagesRoot, "linux-deb.image"), FULL_PACKAGE_NAME.fetchFrom(params));
            }, false, File::new);

    public static final BundlerParamInfo<File> APP_IMAGE_ROOT = new StandardBundlerParam<>(
            I18N.getString("param.app-image-root.name"),
            I18N.getString("param.app-image-root.description"),
            "appImageRoot",  //KEY
            File.class, null, params -> {
                File imageDir = IMAGE_DIR.fetchFrom(params);
                return new File(imageDir, "opt");
            }, false, File::new);

    public static final BundlerParamInfo<File> CONFIG_DIR = new StandardBundlerParam<>(
            I18N.getString("param.config-dir.name"), 
            I18N.getString("param.config-dir.description"),
            "configDir",  //KEY
            File.class, null, params ->  new File(IMAGE_DIR.fetchFrom(params), "DEBIAN"),
            false, File::new);

    public static final BundlerParamInfo<String> EMAIL = new StandardBundlerParam<> (
            I18N.getString("param.maintainer-email.name"), 
            I18N.getString("param.maintainer-email.description"),
            BundleParams.PARAM_EMAIL,
            String.class, null,
            params -> "Unknown",
            false, s -> s);

    public static final BundlerParamInfo<String> MAINTAINER = new StandardBundlerParam<> (
            I18N.getString("param.maintainer-name.name"), 
            I18N.getString("param.maintainer-name.description"),
            "maintainer", //KEY
            String.class, null,
            params -> VENDOR.fetchFrom(params) + " <" + EMAIL.fetchFrom(params) + ">",
            false, s -> s);

    public static final BundlerParamInfo<String> LICENCE_TYPE = new StandardBundlerParam<> (
            I18N.getString("param.license-type.name"), 
            I18N.getString("param.license-type.description"),
            "licenceType", //KEY
            String.class, null,
            params -> "Unknown", // FIXME default
            false, s -> s);


    public static final BundlerParamInfo<String> LICENSE_TEXT = new StandardBundlerParam<> (
            I18N.getString("param.license-text.name"), 
            I18N.getString("param.license-text.description"),
            "licenceText", //KEY
            String.class, null,
            params -> {
                try {
                    List<String> licenseFiles = LICENSE_FILES.fetchFrom(params);
                    RelativeFileSet appRoot = APP_RESOURCES.fetchFrom(params);
                    //need to copy license file to the root of win-app.image
                    for (String s : licenseFiles) {
                        return new String(IOUtils.readFully(new File(appRoot.getBaseDirectory(), s)));
                    }
                } catch (Exception e) {
                    if (Log.isDebug()) {
                        e.printStackTrace();
                    }
                }
                return LICENSE_TYPE.fetchFrom(params);
            },
            false, s -> s);

    private final static String DEFAULT_ICON = "javalogo_white_32.png";
    private final static String DEFAULT_CONTROL_TEMPLATE = "template.control";
    private final static String DEFAULT_PRERM_TEMPLATE = "template.prerm";
    private final static String DEFAULT_PREINSTALL_TEMPLATE = "template.preinst";
    private final static String DEFAULT_POSTRM_TEMPLATE = "template.postrm";
    private final static String DEFAULT_POSTINSTALL_TEMPLATE = "template.postinst";
    private final static String DEFAULT_COPYRIGHT_TEMPLATE = "template.copyright";
    private final static String DEFAULT_DESKTOP_FILE_TEMPLATE = "template.desktop";

    private final static String TOOL_DPKG = "dpkg-deb";

    public LinuxDebBundler() {
        super();
        baseResourceLoader = LinuxResources.class;
    }

    private boolean testTool(String toolName, String minVersion) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    toolName,
                    "--version");
            IOUtils.exec(pb, Log.isDebug(), true); //not interested in the output
        } catch (Exception e) {
            Log.verbose(MessageFormat.format(I18N.getString("message.test-for-tool"), toolName, e.getMessage()));
            return false;
        }
        return true;
    }

    @Override
    public boolean validate(Map<String, ? super Object> p) throws UnsupportedPlatformException, ConfigException {
        if (p == null) throw new ConfigException(
                I18N.getString("error.parameters-null"),
                I18N.getString("error.parameters-null.advice"));

        //run basic validation to ensure requirements are met
        //we are not interested in return code, only possible exception
        APP_BUNDLER.fetchFrom(p).doValidate(p);

        //NOTE: Can we validate that the required tools are available before we start?
        if (!testTool(TOOL_DPKG, "1")){
            throw new ConfigException(
                    MessageFormat.format(I18N.getString("error.tool-not-found"), TOOL_DPKG),
                    I18N.getString("error.tool-not-found.advice"));
        }

        return true;
    }

    private boolean prepareProto(Map<String, ? super Object> p) {
        File appImageRoot = APP_IMAGE_ROOT.fetchFrom(p);
        File appDir = APP_BUNDLER.fetchFrom(p).doBundle(p, appImageRoot, true);
        return appDir != null;
    }

    //@Override
    public File bundle(Map<String, ? super Object> p, File outdir) {
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

        File imageDir = IMAGE_DIR.fetchFrom(p);
        File configDir = CONFIG_DIR.fetchFrom(p);

        try {

            imageDir.mkdirs();
            configDir.mkdirs();

            if (prepareProto(p) && prepareProjectConfig(p)) {
                return buildDeb(p, outdir);
            }
            return null;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            try {
                if (verbose) {
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

    /*
     * set permissions with a string like "rwxr-xr-x"
     * 
     * This cannot be directly backport to 22u which is unfortunately built with 1.6
     */
    private void setPermissions(File file, String permissions) {
        Set<PosixFilePermission> filePermissions = PosixFilePermissions.fromString(permissions);
        try {
            if (file.exists()) {
                Files.setPosixFilePermissions(file.toPath(), filePermissions);
            }
        } catch (IOException ex) {
            Logger.getLogger(LinuxDebBundler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    protected void saveConfigFiles(Map<String, ? super Object> params) {
        try {
            File configRoot = CONFIG_ROOT.fetchFrom(params);

            if (getConfig_ControlFile(params).exists()) {
                IOUtils.copyFile(getConfig_ControlFile(params),
                        new File(configRoot, getConfig_ControlFile(params).getName()));
            }
            if (getConfig_CopyrightFile(params).exists()) {
                IOUtils.copyFile(getConfig_CopyrightFile(params),
                        new File(configRoot, getConfig_CopyrightFile(params).getName()));
            }
            if (getConfig_PreinstallFile(params).exists()) {
                IOUtils.copyFile(getConfig_PreinstallFile(params),
                        new File(configRoot, getConfig_PreinstallFile(params).getName()));
            }
            if (getConfig_PrermFile(params).exists()) {
                IOUtils.copyFile(getConfig_PrermFile(params),
                        new File(configRoot, getConfig_PrermFile(params).getName()));
            }
            if (getConfig_PostinstallFile(params).exists()) {
                IOUtils.copyFile(getConfig_PostinstallFile(params),
                        new File(configRoot, getConfig_PostinstallFile(params).getName()));
            }
            if (getConfig_PostrmFile(params).exists()) {
                IOUtils.copyFile(getConfig_PostrmFile(params),
                        new File(configRoot, getConfig_PostrmFile(params).getName()));
            }
            if (getConfig_DesktopShortcutFile(params).exists()) {
                IOUtils.copyFile(getConfig_DesktopShortcutFile(params),
                        new File(configRoot, getConfig_DesktopShortcutFile(params).getName()));
            }
            if (getConfig_IconFile(params).exists()) {
                IOUtils.copyFile(getConfig_IconFile(params),
                        new File(configRoot, getConfig_IconFile(params).getName()));
            }
            Log.info(MessageFormat.format(I18N.getString("message.config-save-location"), configRoot.getAbsolutePath()));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    private String getArch() {
        String arch = System.getProperty("os.arch");
        if ("i386".equals(arch))
            return "i386";
        else
            return "amd64";
    }

    private long getInstalledSizeKB(Map<String, ? super Object> params) {
        return getInstalledSizeKB(APP_IMAGE_ROOT.fetchFrom(params)) >> 10;
    }

    private long getInstalledSizeKB(File dir) {
        long count = 0;
        File[] children = dir.listFiles();
        if (children != null) {
            for (File file : children) {
                if (file.isFile()) {
                    count += file.length();
                }
                else if (file.isDirectory()) {
                    count += getInstalledSizeKB(file);
                }
            }
        }
        return count;
    }

    private boolean prepareProjectConfig(Map<String, ? super Object> params) throws IOException {
        Map<String, String> data = new HashMap<>();

        data.put("APPLICATION_NAME", BUNDLE_NAME.fetchFrom(params));
        data.put("APPLICATION_PACKAGE", BUNDLE_NAME.fetchFrom(params).toLowerCase());
        data.put("APPLICATION_VENDOR", VENDOR.fetchFrom(params));
        data.put("APPLICATION_MAINTAINER", MAINTAINER.fetchFrom(params));
        data.put("APPLICATION_VERSION", VERSION.fetchFrom(params));
        data.put("APPLICATION_LAUNCHER_FILENAME",
                LinuxAppBundler.getLauncher(APP_IMAGE_ROOT.fetchFrom(params), params).getName());
        data.put("DEPLOY_BUNDLE_CATEGORY", CATEGORY.fetchFrom(params));
        data.put("APPLICATION_DESCRIPTION", DESCRIPTION.fetchFrom(params));
        data.put("APPLICATION_SUMMARY", TITLE.fetchFrom(params));
        data.put("APPLICATION_COPYRIGHT", COPYRIGHT.fetchFrom(params));
        data.put("APPLICATION_LICENSE_TYPE", LICENSE_TYPE.fetchFrom(params));
        data.put("APPLICATION_LICENSE_TEXT", LICENSE_TEXT.fetchFrom(params));
        data.put("APPLICATION_ARCH", getArch());
        data.put("APPLICATION_INSTALLED_SIZE", Long.toString(getInstalledSizeKB(params)));

        //prepare control file
        Writer w = new BufferedWriter(new FileWriter(getConfig_ControlFile(params)));
        String content = preprocessTextResource(
                LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_ControlFile(params).getName(),
                I18N.getString("resource.deb-control-file"), 
                DEFAULT_CONTROL_TEMPLATE, 
                data);
        w.write(content);
        w.close();

        w = new BufferedWriter(new FileWriter(getConfig_PreinstallFile(params)));
        content = preprocessTextResource(
                LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_PreinstallFile(params).getName(),
                I18N.getString("resource.deb-preinstall-script"),
                DEFAULT_PREINSTALL_TEMPLATE,
                data);
        w.write(content);
        w.close();
        setPermissions(getConfig_PreinstallFile(params), "rwxr-xr-x");

        w = new BufferedWriter(new FileWriter(getConfig_PrermFile(params)));
        content = preprocessTextResource(
                LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_PrermFile(params).getName(),
                I18N.getString("resource.deb-prerm-script"),
                DEFAULT_PRERM_TEMPLATE,
                data);
        w.write(content);
        w.close();
        setPermissions(getConfig_PrermFile(params), "rwxr-xr-x");

        w = new BufferedWriter(new FileWriter(getConfig_PostinstallFile(params)));
        content = preprocessTextResource(
                LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_PostinstallFile(params).getName(),
                I18N.getString("resource.deb-postinstall-script"),
                DEFAULT_POSTINSTALL_TEMPLATE,
                data);
        w.write(content);
        w.close();
        setPermissions(getConfig_PostinstallFile(params), "rwxr-xr-x");

        w = new BufferedWriter(new FileWriter(getConfig_PostrmFile(params)));
        content = preprocessTextResource(
                LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_PostrmFile(params).getName(),
                I18N.getString("resource.deb-postrm-script"),
                DEFAULT_POSTRM_TEMPLATE,
                data);
        w.write(content);
        w.close();
        setPermissions(getConfig_PostrmFile(params), "rwxr-xr-x");

        w = new BufferedWriter(new FileWriter(getConfig_CopyrightFile(params)));
        content = preprocessTextResource(
                LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_CopyrightFile(params).getName(),
                I18N.getString("resource.deb-copyright-file"), 
                DEFAULT_COPYRIGHT_TEMPLATE, 
                data);
        w.write(content);
        w.close();

        //prepare desktop shortcut
        w = new BufferedWriter(new FileWriter(getConfig_DesktopShortcutFile(params)));
        content = preprocessTextResource(
                LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_DesktopShortcutFile(params).getName(),
                I18N.getString("resource.menu-shortcut-descriptor"), 
                DEFAULT_DESKTOP_FILE_TEMPLATE, 
                data);
        w.write(content);
        w.close();

        //prepare installer icon
        File iconTarget = getConfig_IconFile(params);
        File icon = ICON.fetchFrom(params);
        if (icon == null || !icon.exists()) {
            fetchResource(LinuxAppBundler.LINUX_BUNDLER_PREFIX + iconTarget.getName(),
                    I18N.getString("resource.menu-icon"),
                    DEFAULT_ICON,
                    iconTarget);
        } else {
            fetchResource(LinuxAppBundler.LINUX_BUNDLER_PREFIX + iconTarget.getName(),
                    I18N.getString("resource.menu-icon"),
                    icon,
                    iconTarget);
        }

        return true;
    }

    private File getConfig_DesktopShortcutFile(Map<String, ? super Object> params) {
        return new File(
                LinuxAppBundler.getLauncher(APP_IMAGE_ROOT.fetchFrom(params), params).getParentFile(),
                BUNDLE_NAME.fetchFrom(params) + ".desktop");
    }

    private File getConfig_IconFile(Map<String, ? super Object> params) {
        return new File(
                LinuxAppBundler.getLauncher(APP_IMAGE_ROOT.fetchFrom(params), params).getParentFile(),
                BUNDLE_NAME.fetchFrom(params) + ".png");
    }

    private File getConfig_ControlFile(Map<String, ? super Object> params) {
        return new File(CONFIG_DIR.fetchFrom(params), "control");
    }

    private File getConfig_PreinstallFile(Map<String, ? super Object> params) {
        return new File(CONFIG_DIR.fetchFrom(params), "preinst");
    }

    private File getConfig_PrermFile(Map<String, ? super Object> params) {
        return new File(CONFIG_DIR.fetchFrom(params), "prerm");
    }

    private File getConfig_PostinstallFile(Map<String, ? super Object> params) {
        return new File(CONFIG_DIR.fetchFrom(params), "postinst");
    }

    private File getConfig_PostrmFile(Map<String, ? super Object> params) {
        return new File(CONFIG_DIR.fetchFrom(params), "postrm");
    }

    private File getConfig_CopyrightFile(Map<String, ? super Object> params) {
        return new File(CONFIG_DIR.fetchFrom(params), "copyright");
    }

    private File buildDeb(Map<String, ? super Object> params, File outdir) throws IOException {
        File outFile = new File(outdir, FULL_PACKAGE_NAME.fetchFrom(params)+".deb");
        Log.verbose(MessageFormat.format(I18N.getString("message.outputting-to-location"), outFile.getAbsolutePath()));

        outFile.getParentFile().mkdirs();

        //run dpkg
        ProcessBuilder pb = new ProcessBuilder(
                "fakeroot", TOOL_DPKG, "-b",  FULL_PACKAGE_NAME.fetchFrom(params),
                outFile.getAbsolutePath());
        pb = pb.directory(IMAGE_DIR.fetchFrom(params).getParentFile());
        IOUtils.exec(pb, verbose);

        Log.info(MessageFormat.format(I18N.getString("message.output-to-location"), outFile.getAbsolutePath()));

        return outFile;
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
        return "deb"; //KEY
    }

    @Override
    public BundleType getBundleType() {
        return BundleType.INSTALLER;
    }

    @Override
    public Collection<BundlerParamInfo<?>> getBundleParameters() {
        Collection<BundlerParamInfo<?>> results = new LinkedHashSet<>();
        results.addAll(LinuxAppBundler.getAppBundleParameters());
        results.addAll(getDebBundleParameters());
        return results;
    }

    public static Collection<BundlerParamInfo<?>> getDebBundleParameters() {
        return Arrays.asList(
                APP_BUNDLER,
                APP_IMAGE_ROOT,
                APP_NAME,
                APP_RESOURCES,
                BUNDLE_NAME,
                CONFIG_DIR,
                COPYRIGHT,
                CATEGORY,
                DESCRIPTION,
                EMAIL,
                FULL_PACKAGE_NAME,
                ICON,
                IMAGE_DIR,
                IMAGES_ROOT,
                LICENSE_FILES,
                LICENSE_TEXT,
                LICENSE_TYPE,
                MAINTAINER,
                TITLE,
                VENDOR,
                VERSION
        );
    }

    @Override
    public File execute(Map<String, ? super Object> params, File outputParentDir) {
        return bundle(params, outputParentDir);
    }

}
