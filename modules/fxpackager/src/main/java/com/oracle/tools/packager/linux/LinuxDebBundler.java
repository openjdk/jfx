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

package com.oracle.tools.packager.linux;

import com.oracle.tools.packager.*;
import com.oracle.tools.packager.IOUtils;
import com.sun.javafx.tools.packager.bundlers.BundleParams;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.oracle.tools.packager.StandardBundlerParam.*;
import static com.oracle.tools.packager.linux.LinuxAppBundler.ICON_PNG;

public class LinuxDebBundler extends AbstractBundler {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle(LinuxDebBundler.class.getName());

    public static final BundlerParamInfo<LinuxAppBundler> APP_BUNDLER = new StandardBundlerParam<>(
            I18N.getString("param.app-bundler.name"),
            I18N.getString("param.app-bundler.description"),
            "linux.app.bundler",
            LinuxAppBundler.class,
            params -> new LinuxAppBundler(),
            (s, p) -> null);

    public static final BundlerParamInfo<String> BUNDLE_NAME = new StandardBundlerParam<> (
            I18N.getString("param.bundle-name.name"),
            I18N.getString("param.bundle-name.description"),
            "linux.bundleName",
            String.class,
            params -> {
                String nm = APP_NAME.fetchFrom(params);
                if (nm == null) return null;

                // only lower-alphanumeric and -+. are allowed
                // so to lower case,
                // spaces and underscores become dashes
                // and drop all other bad characters
                nm = nm.toLowerCase()
                       .replaceAll("[ _]", "-")
                       .replaceAll("[^-+.a-z0-9]", "");
                return nm;
            },
            (s, p) -> s);

    public static final BundlerParamInfo<String> FULL_PACKAGE_NAME = new StandardBundlerParam<> (
            I18N.getString("param.full-package-name.name"),
            I18N.getString("param.full-package-name.description"),
            "linux.deb.fullPackageName",
            String.class,
            params -> BUNDLE_NAME.fetchFrom(params) + "-" + VERSION.fetchFrom(params),
            (s, p) -> s);

    public static final BundlerParamInfo<File> CONFIG_ROOT = new StandardBundlerParam<>(
            I18N.getString("param.config-root.name"),
            I18N.getString("param.config-root.description"),
            "configRoot",
            File.class,
            params ->  new File(BUILD_ROOT.fetchFrom(params), "linux"),
            (s, p) -> new File(s));

    public static final BundlerParamInfo<File> DEB_IMAGE_DIR = new StandardBundlerParam<>(
            I18N.getString("param.image-dir.name"),
            I18N.getString("param.image-dir.description"),
            "linux.deb.imageDir",
            File.class,
            params -> {
                File imagesRoot = IMAGES_ROOT.fetchFrom(params);
                if (!imagesRoot.exists()) imagesRoot.mkdirs();
                return new File(new File(imagesRoot, "linux-deb.image"), FULL_PACKAGE_NAME.fetchFrom(params));
            },
            (s, p) -> new File(s));

    public static final BundlerParamInfo<File> APP_IMAGE_ROOT = new StandardBundlerParam<>(
            I18N.getString("param.app-image-root.name"),
            I18N.getString("param.app-image-root.description"),
            "linux.deb.imageRoot",
            File.class,
            params -> {
                File imageDir = DEB_IMAGE_DIR.fetchFrom(params);
                return new File(imageDir, "opt");
            },
            (s, p) -> new File(s));

    public static final BundlerParamInfo<File> CONFIG_DIR = new StandardBundlerParam<>(
            I18N.getString("param.config-dir.name"),
            I18N.getString("param.config-dir.description"),
            "linux.deb.configDir",
            File.class,
            params ->  new File(DEB_IMAGE_DIR.fetchFrom(params), "DEBIAN"),
            (s, p) -> new File(s));

    public static final BundlerParamInfo<String> EMAIL = new StandardBundlerParam<> (
            I18N.getString("param.maintainer-email.name"),
            I18N.getString("param.maintainer-email.description"),
            BundleParams.PARAM_EMAIL,
            String.class,
            params -> "Unknown",
            (s, p) -> s);

    public static final BundlerParamInfo<String> MAINTAINER = new StandardBundlerParam<> (
            I18N.getString("param.maintainer-name.name"),
            I18N.getString("param.maintainer-name.description"),
            "linux.deb.maintainer",
            String.class,
            params -> VENDOR.fetchFrom(params) + " <" + EMAIL.fetchFrom(params) + ">",
            (s, p) -> s);

    public static final BundlerParamInfo<String> LICENSE_TEXT = new StandardBundlerParam<> (
            I18N.getString("param.license-text.name"),
            I18N.getString("param.license-text.description"),
            "linux.deb.licenseText",
            String.class,
            params -> {
                try {
                    List<String> licenseFiles = LICENSE_FILE.fetchFrom(params);
                    com.oracle.tools.packager.RelativeFileSet appRoot = APP_RESOURCES.fetchFrom(params);
                    //need to copy license file to the root of linux-app.image
                    if (licenseFiles.size() > 0) {
                        return new String(IOUtils.readFully(new File(appRoot.getBaseDirectory(), licenseFiles.get(0))));
                    }
                } catch (Exception e) {
                    if (Log.isDebug()) {
                        e.printStackTrace();
                    }
                }
                return LICENSE_TYPE.fetchFrom(params);
            },
            (s, p) -> s);

    private final static String DEFAULT_ICON = "javalogo_white_32.png";
    private final static String DEFAULT_CONTROL_TEMPLATE = "template.control";
    private final static String DEFAULT_PRERM_TEMPLATE = "template.prerm";
    private final static String DEFAULT_PREINSTALL_TEMPLATE = "template.preinst";
    private final static String DEFAULT_POSTRM_TEMPLATE = "template.postrm";
    private final static String DEFAULT_POSTINSTALL_TEMPLATE = "template.postinst";
    private final static String DEFAULT_COPYRIGHT_TEMPLATE = "template.copyright";
    private final static String DEFAULT_DESKTOP_FILE_TEMPLATE = "template.desktop";
    private final static String DEFAULT_INIT_SCRIPT_TEMPLATE = "template.deb.init.script";

    public final static String TOOL_DPKG = "dpkg-deb";

    public LinuxDebBundler() {
        super();
        baseResourceLoader = LinuxResources.class;
    }

    public static boolean testTool(String toolName, String minVersion) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    toolName,
                    "--version");
            IOUtils.exec(pb, Log.isDebug(), true); //FIXME not interested in the output
        } catch (Exception e) {
            Log.verbose(MessageFormat.format(I18N.getString("message.test-for-tool"), toolName, e.getMessage()));
            return false;
        }
        return true;
    }

    @Override
    public boolean validate(Map<String, ? super Object> p) throws UnsupportedPlatformException, ConfigException {
        try {
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

            // validate license file, if used, exists in the proper place
            if (p.containsKey(LICENSE_FILE.getID())) {
                com.oracle.tools.packager.RelativeFileSet appResources = APP_RESOURCES.fetchFrom(p);
                for (String license : LICENSE_FILE.fetchFrom(p)) {
                    if (!appResources.contains(license)) {
                        throw new ConfigException(
                                I18N.getString("error.license-missing"),
                                MessageFormat.format(I18N.getString("error.license-missing.advice"),
                                        license, appResources.getBaseDirectory().toString()));
                    }
                }
            } else {
                Log.info(I18N.getString("message.debs-like-licenses"));
            }

            // for services, the app launcher must be less than 16 characters or init.d complains
            if (p.containsKey(SERVICE_HINT.getID()) && SERVICE_HINT.fetchFrom(p) && BUNDLE_NAME.fetchFrom(p).length() > 16) {
                throw new ConfigException(
                        MessageFormat.format(I18N.getString("error.launcher-name-too-long"), BUNDLE_NAME.fetchFrom(p)),
                        MessageFormat.format(I18N.getString("error.launcher-name-too-long.advice"), BUNDLE_NAME.getID()));
            }

            return true;
        } catch (RuntimeException re) {
            if (re.getCause() instanceof ConfigException) {
                throw (ConfigException) re.getCause();
            } else {
                throw new ConfigException(re);
            }
        }
    }

    private boolean prepareProto(Map<String, ? super Object> p) {
        File appImageRoot = APP_IMAGE_ROOT.fetchFrom(p);
        File appDir = APP_BUNDLER.fetchFrom(p).doBundle(p, appImageRoot, true);
        return appDir != null;
    }

    //@Override
    public File bundle(Map<String, ? super Object> p, File outdir) {
        if (!outdir.isDirectory() && !outdir.mkdirs()) {
            throw new RuntimeException(MessageFormat.format(I18N.getString("error.cannot-create-output-dir"), outdir.getAbsolutePath()));
        }
        if (!outdir.canWrite()) {
            throw new RuntimeException(MessageFormat.format(I18N.getString("error.cannot-write-to-output-dir"), outdir.getAbsolutePath()));
        }

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

        File imageDir = DEB_IMAGE_DIR.fetchFrom(p);
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
            if (SERVICE_HINT.fetchFrom(params)) {
                if (getConfig_InitScriptFile(params).exists()) {
                    IOUtils.copyFile(getConfig_InitScriptFile(params),
                            new File(configRoot, getConfig_InitScriptFile(params).getName()));
                }
            }
            Log.info(MessageFormat.format(I18N.getString("message.config-save-location"), configRoot.getAbsolutePath()));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
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

        data.put("APPLICATION_NAME", APP_NAME.fetchFrom(params));
        data.put("APPLICATION_FS_NAME", APP_FS_NAME.fetchFrom(params));
        data.put("APPLICATION_PACKAGE", BUNDLE_NAME.fetchFrom(params));
        data.put("APPLICATION_VENDOR", VENDOR.fetchFrom(params));
        data.put("APPLICATION_MAINTAINER", MAINTAINER.fetchFrom(params));
        data.put("APPLICATION_VERSION", VERSION.fetchFrom(params));
        data.put("APPLICATION_LAUNCHER_FILENAME", APP_FS_NAME.fetchFrom(params));
        data.put("DEPLOY_BUNDLE_CATEGORY", CATEGORY.fetchFrom(params));
        data.put("APPLICATION_DESCRIPTION", DESCRIPTION.fetchFrom(params));
        data.put("APPLICATION_SUMMARY", TITLE.fetchFrom(params));
        data.put("APPLICATION_COPYRIGHT", COPYRIGHT.fetchFrom(params));
        data.put("APPLICATION_LICENSE_TYPE", LICENSE_TYPE.fetchFrom(params));
        data.put("APPLICATION_LICENSE_TEXT", LICENSE_TEXT.fetchFrom(params));
        data.put("APPLICATION_ARCH", getArch());
        data.put("APPLICATION_INSTALLED_SIZE", Long.toString(getInstalledSizeKB(params)));
        data.put("SERVICE_HINT", String.valueOf(SERVICE_HINT.fetchFrom(params)));
        data.put("START_ON_INSTALL", String.valueOf(START_ON_INSTALL.fetchFrom(params)));
        data.put("STOP_ON_UNINSTALL", String.valueOf(STOP_ON_UNINSTALL.fetchFrom(params)));
        data.put("RUN_AT_STARTUP", String.valueOf(RUN_AT_STARTUP.fetchFrom(params)));

        //prepare control file
        Writer w = new BufferedWriter(new FileWriter(getConfig_ControlFile(params)));
        String content = preprocessTextResource(
                LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_ControlFile(params).getName(),
                I18N.getString("resource.deb-control-file"),
                DEFAULT_CONTROL_TEMPLATE,
                data,
                VERBOSE.fetchFrom(params));
        w.write(content);
        w.close();

        w = new BufferedWriter(new FileWriter(getConfig_PreinstallFile(params)));
        content = preprocessTextResource(
                LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_PreinstallFile(params).getName(),
                I18N.getString("resource.deb-preinstall-script"),
                DEFAULT_PREINSTALL_TEMPLATE,
                data,
                VERBOSE.fetchFrom(params));
        w.write(content);
        w.close();
        setPermissions(getConfig_PreinstallFile(params), "rwxr-xr-x");

        w = new BufferedWriter(new FileWriter(getConfig_PrermFile(params)));
        content = preprocessTextResource(
                LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_PrermFile(params).getName(),
                I18N.getString("resource.deb-prerm-script"),
                DEFAULT_PRERM_TEMPLATE,
                data,
                VERBOSE.fetchFrom(params));
        w.write(content);
        w.close();
        setPermissions(getConfig_PrermFile(params), "rwxr-xr-x");

        w = new BufferedWriter(new FileWriter(getConfig_PostinstallFile(params)));
        content = preprocessTextResource(
                LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_PostinstallFile(params).getName(),
                I18N.getString("resource.deb-postinstall-script"),
                DEFAULT_POSTINSTALL_TEMPLATE,
                data,
                VERBOSE.fetchFrom(params));
        w.write(content);
        w.close();
        setPermissions(getConfig_PostinstallFile(params), "rwxr-xr-x");

        w = new BufferedWriter(new FileWriter(getConfig_PostrmFile(params)));
        content = preprocessTextResource(
                LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_PostrmFile(params).getName(),
                I18N.getString("resource.deb-postrm-script"),
                DEFAULT_POSTRM_TEMPLATE,
                data,
                VERBOSE.fetchFrom(params));
        w.write(content);
        w.close();
        setPermissions(getConfig_PostrmFile(params), "rwxr-xr-x");

        w = new BufferedWriter(new FileWriter(getConfig_CopyrightFile(params)));
        content = preprocessTextResource(
                LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_CopyrightFile(params).getName(),
                I18N.getString("resource.deb-copyright-file"),
                DEFAULT_COPYRIGHT_TEMPLATE,
                data,
                VERBOSE.fetchFrom(params));
        w.write(content);
        w.close();

        //prepare desktop shortcut
        w = new BufferedWriter(new FileWriter(getConfig_DesktopShortcutFile(params)));
        content = preprocessTextResource(
                LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_DesktopShortcutFile(params).getName(),
                I18N.getString("resource.menu-shortcut-descriptor"),
                DEFAULT_DESKTOP_FILE_TEMPLATE,
                data,
                VERBOSE.fetchFrom(params));
        w.write(content);
        w.close();

        //prepare installer icon
        File iconTarget = getConfig_IconFile(params);
        File icon = ICON_PNG.fetchFrom(params);
        if (icon == null || !icon.exists()) {
            fetchResource(LinuxAppBundler.LINUX_BUNDLER_PREFIX + iconTarget.getName(),
                    I18N.getString("resource.menu-icon"),
                    DEFAULT_ICON,
                    iconTarget,
                    VERBOSE.fetchFrom(params));
        } else {
            fetchResource(LinuxAppBundler.LINUX_BUNDLER_PREFIX + iconTarget.getName(),
                    I18N.getString("resource.menu-icon"),
                    icon,
                    iconTarget,
                    VERBOSE.fetchFrom(params));
        }

        if (SERVICE_HINT.fetchFrom(params)) {
            //prepare init script
            w = new BufferedWriter(new FileWriter(getConfig_InitScriptFile(params)));
            content = preprocessTextResource(
                    LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_InitScriptFile(params).getName(),
                    I18N.getString("resource.deb-init-script"),
                    DEFAULT_INIT_SCRIPT_TEMPLATE,
                    data,
                    VERBOSE.fetchFrom(params));
            w.write(content);
            w.close();
            setPermissions(getConfig_InitScriptFile(params), "rwxr-xr-x");
        }

        return true;
    }

    private File getConfig_DesktopShortcutFile(Map<String, ? super Object> params) {
        return new File(LinuxAppBundler.getRootDir(APP_IMAGE_ROOT.fetchFrom(params), params),
                APP_FS_NAME.fetchFrom(params) + ".desktop");
    }

    private File getConfig_IconFile(Map<String, ? super Object> params) {
        return new File(LinuxAppBundler.getRootDir(APP_IMAGE_ROOT.fetchFrom(params), params),
                APP_FS_NAME.fetchFrom(params) + ".png");
    }

    private File getConfig_InitScriptFile(Map<String, ? super Object> params) {
        return new File(LinuxAppBundler.getRootDir(APP_IMAGE_ROOT.fetchFrom(params), params),
                BUNDLE_NAME.fetchFrom(params) + ".init");
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
        pb = pb.directory(DEB_IMAGE_DIR.fetchFrom(params).getParentFile());
        IOUtils.exec(pb, VERBOSE.fetchFrom(params));

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
        return "deb";
    }

    @Override
    public String getBundleType() {
        return "INSTALLER";
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
                BUNDLE_NAME,
                COPYRIGHT,
                CATEGORY,
                DESCRIPTION,
                EMAIL,
                ICON_PNG,
                LICENSE_FILE,
                LICENSE_TYPE,
                TITLE,
                VENDOR
        );
    }

    @Override
    public File execute(Map<String, ? super Object> params, File outputParentDir) {
        return bundle(params, outputParentDir);
    }

}
