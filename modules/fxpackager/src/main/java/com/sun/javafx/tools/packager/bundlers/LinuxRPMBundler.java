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
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.oracle.bundlers.StandardBundlerParam.*;

public class LinuxRPMBundler extends AbstractBundler {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle("com.oracle.bundlers.linux.LinuxRpmBundler");

    public static final BundlerParamInfo<LinuxAppBundler> APP_BUNDLER = new StandardBundlerParam<>(
            I18N.getString("param.app-bundler.name"), 
            I18N.getString("param.app-bundler.description"),
            "linuxAppBundler", //KEY
            LinuxAppBundler.class, null, params -> new LinuxAppBundler(), false, null);

    public static final BundlerParamInfo<File> IMAGE_DIR = new StandardBundlerParam<>(
            I18N.getString("param.image-dir.name"), 
            I18N.getString("param.image-dir.description"),
            "imageDir", //KEY
            File.class, null, params -> {
                File imagesRoot = IMAGES_ROOT.fetchFrom(params);
                return new File(imagesRoot, "linux-rpm.image");
            }, false, File::new);

    public static final BundlerParamInfo<File> CONFIG_ROOT = new StandardBundlerParam<>(
            I18N.getString("param.config-root.name"), 
            I18N.getString("param.config-root.description"),
            "configRoot", //KEY
            File.class, null, params ->  new File(BUILD_ROOT.fetchFrom(params), "linux"),
            false, File::new);

    public static final BundlerParamInfo<String> BUNDLE_NAME = new StandardBundlerParam<> (
            I18N.getString("param.bundle-name.name"), 
            I18N.getString("param.bundle-name.description"),
            "bundleName", //KEY
            String.class, null, params -> {
                String nm = APP_NAME.fetchFrom(params);
                if (nm == null) return null;
        
                //spaces are not allowed in RPM package names
                nm = nm.replaceAll(" ", "");
                return nm;
            }, false, s -> s);

    private final static String DEFAULT_ICON = "javalogo_white_32.png";
    private final static String DEFAULT_SPEC_TEMPLATE = "template.spec";
    private final static String DEFAULT_DESKTOP_FILE_TEMPLATE = "template.desktop";

    public final static String TOOL_RPMBUILD = "rpmbuild";
    public final static double TOOL_RPMBUILD_MIN_VERSION = 4.0d;

    public LinuxRPMBundler() {
        super();
        baseResourceLoader = LinuxResources.class;
    }

    public static boolean testTool(String toolName, double minVersion) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PrintStream ps = new PrintStream(baos)) {
            ProcessBuilder pb = new ProcessBuilder(
                    toolName,
                    "--version");

            IOUtils.exec(pb, Log.isDebug(), false, ps); //not interested in the output

            //TODO: Version is ignored; need to extract version string and compare!
            String content = new String(baos.toByteArray());
            Pattern pattern = Pattern.compile("RPM version (\\d+\\.\\d+)");
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                String v = matcher.group(1);
                double version = new Double(v);
                return minVersion <= version;
            } else {
               return false;
            }
        } catch (Exception e) {
            Log.verbose(MessageFormat.format(I18N.getString("message.test-for-tool"), toolName, e.getMessage()));
            return false;
        }
    }

    @Override
    public boolean validate(Map<String, ? super Object> p) throws UnsupportedPlatformException, ConfigException {
        if (p == null) throw new ConfigException(
                I18N.getString("error.parameters-null"), 
                I18N.getString("error.parameters-null.advice"));

        //run basic validation to ensure requirements are met
        //we are not interested in return code, only possible exception
        APP_BUNDLER.fetchFrom(p).doValidate(p);

        //TODO: validate presense of required tools?
        if (!testTool(TOOL_RPMBUILD, TOOL_RPMBUILD_MIN_VERSION)){
            throw new ConfigException(
                    I18N.getString(MessageFormat.format("error.cannot-find-rpmbuild", TOOL_RPMBUILD_MIN_VERSION)),
                    I18N.getString(MessageFormat.format("error.cannot-find-rpmbuild.advice", TOOL_RPMBUILD_MIN_VERSION)));
        }

        return true;
    }

    private boolean prepareProto(Map<String, ? super Object> params) {
        File imageDir = IMAGE_DIR.fetchFrom(params);
        File appDir = APP_BUNDLER.fetchFrom(params).doBundle(params, imageDir, true);
        return appDir != null;
    }

    public File bundle(Map<String, ? super Object> p, File outdir) {
        File imageDir = IMAGE_DIR.fetchFrom(p);
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
                return buildRPM(p, outdir);
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

    protected void saveConfigFiles(Map<String, ? super Object> params) {
        try {
            File configRoot = CONFIG_ROOT.fetchFrom(params);
            if (getConfig_SpecFile(params).exists()) {
                IOUtils.copyFile(getConfig_SpecFile(params),
                        new File(configRoot, getConfig_SpecFile(params).getName()));
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

    private String getLicenseFileString(Map<String, ? super Object> params) {
        StringBuilder sb = new StringBuilder();
        for (String f: LICENSE_FILES.fetchFrom(params)) {
            if (sb.length() != 0) {
                sb.append("\n");
            }
            sb.append("%doc /opt/");
            sb.append(BUNDLE_NAME.fetchFrom(params));
            sb.append("/app/");
            sb.append(f);
        }
        return sb.toString();
    }

    private boolean prepareProjectConfig(Map<String, ? super Object> params) throws IOException {
        Map<String, String> data = new HashMap<>();

        data.put("APPLICATION_NAME", BUNDLE_NAME.fetchFrom(params));
        data.put("APPLICATION_VENDOR", VENDOR.fetchFrom(params));
        data.put("APPLICATION_VERSION", VERSION.fetchFrom(params));
        data.put("APPLICATION_LAUNCHER_FILENAME",
                LinuxAppBundler.getLauncher(IMAGE_DIR.fetchFrom(params), params).getName());
        data.put("APPLICATION_DESKTOP_SHORTCUT", SHORTCUT_HINT.fetchFrom(params) ? "returnTrue" : "returnFalse");
        data.put("APPLICATION_MENU_SHORTCUT", MENU_HINT.fetchFrom(params) ? "returnTrue" : "returnFalse");
        data.put("DEPLOY_BUNDLE_CATEGORY", CATEGORY.fetchFrom(params)); //TODO rpm categories
        data.put("APPLICATION_DESCRIPTION", DESCRIPTION.fetchFrom(params));
        data.put("APPLICATION_SUMMARY", TITLE.fetchFrom(params));
        data.put("APPLICATION_LICENSE_TYPE", LICENSE_TYPE.fetchFrom(params));
        data.put("APPLICATION_LICENSE_FILE", getLicenseFileString(params));

        //prepare spec file
        Writer w = new BufferedWriter(new FileWriter(getConfig_SpecFile(params)));
        String content = preprocessTextResource(
                LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_SpecFile(params).getName(),
                I18N.getString("resource.rpm-spec-file"), DEFAULT_SPEC_TEMPLATE, data);
        w.write(content);
        w.close();

        //prepare desktop shortcut
        w = new BufferedWriter(new FileWriter(getConfig_DesktopShortcutFile(params)));
        content = preprocessTextResource(
                LinuxAppBundler.LINUX_BUNDLER_PREFIX + getConfig_DesktopShortcutFile(params).getName(),
                I18N.getString("resource.menu-shortcut-descriptor"), DEFAULT_DESKTOP_FILE_TEMPLATE, data);
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
        return new File(LinuxAppBundler.getLauncher(IMAGE_DIR.fetchFrom(params), params).getParentFile(),
                BUNDLE_NAME.fetchFrom(params) + ".desktop");
    }

    private File getConfig_IconFile(Map<String, ? super Object> params) {
        return new File(LinuxAppBundler.getLauncher(IMAGE_DIR.fetchFrom(params), params).getParentFile(),
                BUNDLE_NAME.fetchFrom(params) + ".png");
    }

    private File getConfig_SpecFile(Map<String, ? super Object> params) {
        return new File(IMAGE_DIR.fetchFrom(params),
                BUNDLE_NAME.fetchFrom(params) + ".spec");
    }

    private File buildRPM(Map<String, ? super Object> params, File outdir) throws IOException {
        Log.verbose(MessageFormat.format(I18N.getString("message.outputting-bundle-location"), outdir.getAbsolutePath()));

        File broot = new File(BUILD_ROOT.fetchFrom(params), "rmpbuildroot");

        outdir.mkdirs();

        //run rpmbuild
        ProcessBuilder pb = new ProcessBuilder(
                TOOL_RPMBUILD,
                "-bb", getConfig_SpecFile(params).getAbsolutePath(),
//                "--define", "%__jar_repack %{nil}",  //debug: improves build time (but will require unpack to install?)
                "--define", "%_sourcedir "+IMAGE_DIR.fetchFrom(params).getAbsolutePath(),
                "--define", "%_rpmdir " + outdir.getAbsolutePath(), //save result to output dir
                "--define", "%_topdir " + broot.getAbsolutePath() //do not use other system directories to build as current user
        );
        pb = pb.directory(IMAGE_DIR.fetchFrom(params));
        IOUtils.exec(pb, verbose);

        IOUtils.deleteRecursive(broot);

        Log.info(MessageFormat.format(I18N.getString("message.output-bundle-location"), outdir.getAbsolutePath()));

        //todo look for added files and returnt hat added file.
        return outdir;
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
        return "rpm"; //KEY
    }

    @Override
    public BundleType getBundleType() {
        return BundleType.INSTALLER;
    }

    @Override
    public Collection<BundlerParamInfo<?>> getBundleParameters() {
        Collection<BundlerParamInfo<?>> results = new LinkedHashSet<>();
        results.addAll(LinuxAppBundler.getAppBundleParameters());
        results.addAll(getRpmBundleParameters());
        return results;
    }

    public static Collection<BundlerParamInfo<?>> getRpmBundleParameters() {
        return Arrays.asList(
                APP_BUNDLER,
                APP_NAME,
                BUILD_ROOT,
                BUNDLE_NAME,
                CONFIG_ROOT,
                CATEGORY,
                DESCRIPTION,
                ICON,
                IMAGE_DIR,
                IMAGES_ROOT,
                LICENSE_FILES,
                LICENSE_TYPE,
                MENU_HINT,
                SHORTCUT_HINT,
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
