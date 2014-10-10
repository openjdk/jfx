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

package com.oracle.tools.packager.mac;

import com.oracle.tools.packager.BundlerParamInfo;
import com.oracle.tools.packager.StandardBundlerParam;
import com.oracle.tools.packager.Log;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.IOUtils;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.UnsupportedPlatformException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.oracle.tools.packager.StandardBundlerParam.*;

public class MacPkgBundler extends MacBaseInstallerBundler {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle(MacPkgBundler.class.getName());

    public final static String MAC_BUNDLER_PREFIX =
            BUNDLER_PREFIX + "macosx" + File.separator;
    
    private static final String DEFAULT_BACKGROUND_IMAGE = "background_pkg.png";
    
    private static final String TEMPLATE_PREINSTALL_SCRIPT = "preinstall.template";
    private static final String TEMPLATE_POSTINSTALL_SCRIPT = "postinstall.template";
    
    private static final BundlerParamInfo<File> PACKAGES_ROOT = new StandardBundlerParam<>(
            I18N.getString("param.packages-root.name"),
            I18N.getString("param.packages-root.description"),
            "mac.pkg.packagesRoot",
            File.class,
            params -> {
                File packagesRoot = new File(BUILD_ROOT.fetchFrom(params), "packages");
                packagesRoot.mkdirs();
                return packagesRoot;
            },
            (s, p) -> new File(s));


    protected final BundlerParamInfo<File> SCRIPTS_DIR = new StandardBundlerParam<>(
            I18N.getString("param.scripts-dir.name"),
            I18N.getString("param.scripts-dir.description"),
            "mac.pkg.scriptsDir",
            File.class,
            params -> {
                File scriptsDir = new File(CONFIG_ROOT.fetchFrom(params), "scripts");
                scriptsDir.mkdirs();
                return scriptsDir;
            },
            (s, p) -> new File(s));

    public static final BundlerParamInfo<String> DEVELOPER_ID_INSTALLER_SIGNING_KEY = new StandardBundlerParam<>(
            I18N.getString("param.signing-key-developer-id-installer.name"),
            I18N.getString("param.signing-key-developer-id-installer.description"),
            "mac.signing-key-developer-id-installer",
            String.class,
            params -> {
                String key = "Developer ID Installer: " + SIGNING_KEY_USER.fetchFrom(params);
                try {
                    IOUtils.exec(new ProcessBuilder("security", "find-certificate", "-c", key), VERBOSE.fetchFrom(params));
                    return key;
                } catch (IOException ioe) {
                    return null;
                }
            },
            (s, p) -> s);

    public MacPkgBundler() {
        super();
        baseResourceLoader = MacResources.class;
    }

    //@Override
    public File bundle(Map<String, ? super Object> p, File outdir) {
        Log.info(MessageFormat.format(I18N.getString("message.building-pkg"), APP_NAME.fetchFrom(p)));
        if (!outdir.isDirectory() && !outdir.mkdirs()) {
            throw new RuntimeException(MessageFormat.format(I18N.getString("error.cannot-create-output-dir"), outdir.getAbsolutePath()));
        }
        if (!outdir.canWrite()) {
            throw new RuntimeException(MessageFormat.format(I18N.getString("error.cannot-write-to-output-dir"), outdir.getAbsolutePath()));
        }


        try {
            File appImageDir = prepareAppBundle(p);

            if (SERVICE_HINT.fetchFrom(p)) {
                File daemonImageDir = DAEMON_IMAGE_BUILD_ROOT.fetchFrom(p);
                daemonImageDir.mkdirs();
                prepareDaemonBundle(p);
            }

            return createPKG(p, outdir, appImageDir);
        } catch (Exception ex) {
            return null;
        }
    }

    private File getPackages_AppPackage(Map<String, ? super Object> params) {
        return new File(PACKAGES_ROOT.fetchFrom(params), APP_FS_NAME.fetchFrom(params) + "-app.pkg");
    }

    private File getPackages_DaemonPackage(Map<String, ? super Object> params) {
        return new File(PACKAGES_ROOT.fetchFrom(params), APP_FS_NAME.fetchFrom(params) + "-daemon.pkg");
    }

    private void cleanupPackagesFiles(Map<String, ? super Object> params) {
        if (getPackages_AppPackage(params) != null) {
            getPackages_AppPackage(params).delete();
        }
        if (getPackages_DaemonPackage(params) != null) {
            getPackages_DaemonPackage(params).delete();
        }
    }

    private File getConfig_DistributionXMLFile(Map<String, ? super Object> params) {
        return new File(CONFIG_ROOT.fetchFrom(params), "distribution.dist");
    }

    private File getConfig_BackgroundImage(Map<String, ? super Object> params) {
        return new File(CONFIG_ROOT.fetchFrom(params), APP_NAME.fetchFrom(params) + "-background.png");
    }

    private File getScripts_PreinstallFile(Map<String, ? super Object> params) {
        return new File(SCRIPTS_DIR.fetchFrom(params), "preinstall");
    }

    private File getScripts_PostinstallFile(Map<String, ? super Object> params) {
        return new File(SCRIPTS_DIR.fetchFrom(params), "postinstall");
    }

    private void cleanupPackageScripts(Map<String, ? super Object> params) {
        if (getScripts_PreinstallFile(params) != null) {
            getScripts_PreinstallFile(params).delete();
        }
        if (getScripts_PostinstallFile(params) != null) {
            getScripts_PostinstallFile(params).delete();
        }
    }

    private void cleanupConfigFiles(Map<String, ? super Object> params) {
        if (getConfig_DistributionXMLFile(params) != null) {
            getConfig_DistributionXMLFile(params).delete();
        }
        if (getConfig_BackgroundImage(params) != null) {
            getConfig_BackgroundImage(params).delete();
        }
    }

    private String getAppIdentifier(Map<String, ? super Object> params) {
        return IDENTIFIER.fetchFrom(params);
    }

    private String getDaemonIdentifier(Map<String, ? super Object> params) {
        return IDENTIFIER.fetchFrom(params) + ".daemon";
    }

    private void preparePackageScripts(Map<String, ? super Object> params) throws IOException
    {
        Log.verbose(I18N.getString("message.preparing-scripts"));

        Map<String, String> data = new HashMap<>();

        data.put("DEPLOY_DAEMON_IDENTIFIER", getDaemonIdentifier(params));
        data.put("DEPLOY_LAUNCHD_PLIST_FILE",
                IDENTIFIER.fetchFrom(params).toLowerCase() + ".launchd.plist");

        Writer w = new BufferedWriter(new FileWriter(getScripts_PreinstallFile(params)));
        String content = preprocessTextResource(
                MAC_BUNDLER_PREFIX + getScripts_PreinstallFile(params).getName(),
                I18N.getString("resource.pkg-preinstall-script"),
                TEMPLATE_PREINSTALL_SCRIPT,
                data,
                VERBOSE.fetchFrom(params));
        w.write(content);
        w.close();
        getScripts_PreinstallFile(params).setExecutable(true, false);

        w = new BufferedWriter(new FileWriter(getScripts_PostinstallFile(params)));
        content = preprocessTextResource(
                MAC_BUNDLER_PREFIX + getScripts_PostinstallFile(params).getName(),
                I18N.getString("resource.pkg-postinstall-script"),
                TEMPLATE_POSTINSTALL_SCRIPT,
                data,
                VERBOSE.fetchFrom(params));
        w.write(content);
        w.close();
        getScripts_PostinstallFile(params).setExecutable(true, false);
    }

    private void prepareDistributionXMLFile(Map<String, ? super Object> params)
            throws IOException
    {
        File f = getConfig_DistributionXMLFile(params);

        Log.verbose(MessageFormat.format(I18N.getString("message.preparing-distribution-dist"), f.getAbsolutePath()));

        PrintStream out = new PrintStream(f);

        out.println("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>");
        out.println("<installer-gui-script minSpecVersion=\"1\">");

        out.println("<title>" + APP_NAME.fetchFrom(params) + "</title>");
        out.println("<background" +
                " file=\"" + getConfig_BackgroundImage(params).getName() + "\"" +
                " mime-type=\"image/png\"" +
                " alignment=\"bottomleft\" " +
                " scaling=\"none\""+
                "/>");

        if (!LICENSE_FILE.fetchFrom(params).isEmpty()) {
            File licFile = new File(APP_RESOURCES.fetchFrom(params).getBaseDirectory(),
                    LICENSE_FILE.fetchFrom(params).get(0));
            out.println("<license" +
                    " file=\"" + licFile.getAbsolutePath() + "\"" +
                    " mime-type=\"text/rtf\"" +
                    "/>");
        }

        /*
         * Note that the content of the distribution file
         * below is generated by productbuild --synthesize
         */

        String appId = getAppIdentifier(params);
        String daemonId = getDaemonIdentifier(params);

        out.println("<pkg-ref id=\"" + appId + "\"/>");
        if (SERVICE_HINT.fetchFrom(params)) {
            out.println("<pkg-ref id=\"" + daemonId + "\"/>");
        }

        out.println("<options customize=\"never\" require-scripts=\"false\"/>");
        out.println("<choices-outline>");
        out.println("    <line choice=\"default\">");
        out.println("        <line choice=\"" + appId + "\"/>");
        if (SERVICE_HINT.fetchFrom(params)) {
            out.println("        <line choice=\"" + daemonId + "\"/>");
        }
        out.println("    </line>");
        out.println("</choices-outline>");
        out.println("<choice id=\"default\"/>");
        out.println("<choice id=\"" + appId + "\" visible=\"false\">");
        out.println("    <pkg-ref id=\"" + appId + "\"/>");
        out.println("</choice>");
        out.println("<pkg-ref id=\"" + appId + "\" version=\"" + VERSION.fetchFrom(params) +
                "\" onConclusion=\"none\">" +
                        URLEncoder.encode(getPackages_AppPackage(params).getName(), "UTF-8") + "</pkg-ref>");

        if (SERVICE_HINT.fetchFrom(params)) {
            out.println("<choice id=\"" + daemonId + "\" visible=\"false\">");
            out.println("    <pkg-ref id=\"" + daemonId + "\"/>");
            out.println("</choice>");
            out.println("<pkg-ref id=\"" + daemonId + "\" version=\"" + VERSION.fetchFrom(params) +
                    "\" onConclusion=\"none\">" +
                    URLEncoder.encode(getPackages_DaemonPackage(params).getName(), "UTF-8") + "</pkg-ref>");
        }

        out.println("</installer-gui-script>");

        out.close();
    }

    private void prepareConfigFiles(Map<String, ? super Object> params) throws IOException {
        File imageTarget = getConfig_BackgroundImage(params);
        fetchResource(MacAppBundler.MAC_BUNDLER_PREFIX + imageTarget.getName(),
                I18N.getString("resource.pkg-background-image"),
                DEFAULT_BACKGROUND_IMAGE,
                imageTarget,
                VERBOSE.fetchFrom(params));

        prepareDistributionXMLFile(params);
    }

    private File createPKG(Map<String, ? super Object> params, File outdir, File appLocation) {
        //generic find attempt
        try {
            String daemonLocation = DAEMON_IMAGE_BUILD_ROOT.fetchFrom(params) + "/" + APP_NAME.fetchFrom(params) + ".daemon";

            File appPKG = getPackages_AppPackage(params);
            File daemonPKG = getPackages_DaemonPackage(params);

            // build application package
            ProcessBuilder pb = new ProcessBuilder("pkgbuild",
                    "--component",
                    appLocation.toString(),
                    "--install-location",
                    "/Applications",
                    appPKG.getAbsolutePath());
            IOUtils.exec(pb, VERBOSE.fetchFrom(params));

            prepareConfigFiles(params);

            // build daemon package if requested
            if (SERVICE_HINT.fetchFrom(params)) {
                preparePackageScripts(params);

                pb = new ProcessBuilder("pkgbuild",
                        "--identifier",
                        APP_NAME.fetchFrom(params) + ".daemon",
                        "--root",
                        daemonLocation,
                        "--scripts",
                        SCRIPTS_DIR.fetchFrom(params).getAbsolutePath(),
                        daemonPKG.getAbsolutePath());
                IOUtils.exec(pb, VERBOSE.fetchFrom(params));
            }

            // build final package
            File finalPKG = new File(outdir, INSTALLER_NAME.fetchFrom(params)+".pkg");
            outdir.mkdirs();

            List<String> commandLine = new ArrayList<>();
            commandLine.add("productbuild");

            commandLine.add("--resources");
            commandLine.add(CONFIG_ROOT.fetchFrom(params).getAbsolutePath());

            // maybe sign
            if (Optional.ofNullable(SIGN_BUNDLE.fetchFrom(params)).orElse(Boolean.TRUE)) {
                String signingIdentity = DEVELOPER_ID_INSTALLER_SIGNING_KEY.fetchFrom(params);
                if (signingIdentity != null) {
                    commandLine.add("--sign");
                    commandLine.add(signingIdentity);
                }
            }

            commandLine.add("--distribution");
            commandLine.add(getConfig_DistributionXMLFile(params).getAbsolutePath());
            commandLine.add("--package-path");
            commandLine.add(PACKAGES_ROOT.fetchFrom(params).getAbsolutePath());

            commandLine.add(finalPKG.getAbsolutePath());

            pb = new ProcessBuilder(commandLine);
            IOUtils.exec(pb, VERBOSE.fetchFrom(params));

            return finalPKG;
        } catch (Exception ignored) {
            Log.verbose(ignored);
            return null;
        } finally {
            if (!VERBOSE.fetchFrom(params)) {
                cleanupPackagesFiles(params);
                cleanupConfigFiles(params);

                if (SERVICE_HINT.fetchFrom(params)) {
                    cleanupPackageScripts(params);
                }
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////////
    // Implement Bundler
    //////////////////////////////////////////////////////////////////////////////////

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
        return "pkg";
    }

    @Override
    public Collection<BundlerParamInfo<?>> getBundleParameters() {
        Collection<BundlerParamInfo<?>> results = new LinkedHashSet<>();
        results.addAll(MacAppBundler.getAppBundleParameters());
        results.addAll(getPKGBundleParameters());
        return results;
    }

    public Collection<BundlerParamInfo<?>> getPKGBundleParameters() {
        Collection<BundlerParamInfo<?>> results = new LinkedHashSet<>();

        results.addAll(MacAppBundler.getAppBundleParameters());
        results.addAll(Arrays.asList(
                DEVELOPER_ID_INSTALLER_SIGNING_KEY,
                //IDENTIFIER,
                LICENSE_FILE
                //SERVICE_HINT
        ));

        return results;
    }

    @Override
    public boolean validate(Map<String, ? super Object> params) throws UnsupportedPlatformException, ConfigException {
        try {
            if (params == null) throw new ConfigException(
                    I18N.getString("error.parameters-null"),
                    I18N.getString("error.parameters-null.advice"));

            //run basic validation to ensure requirements are met
            //we are not interested in return code, only possible exception
            validateAppImageAndBundeler(params);

            // validate license file, if used, exists in the proper place
            if (params.containsKey(LICENSE_FILE.getID())) {
                RelativeFileSet appResources = APP_RESOURCES.fetchFrom(params);
                for (String license : LICENSE_FILE.fetchFrom(params)) {
                    if (!appResources.contains(license)) {
                        throw new ConfigException(
                                I18N.getString("error.license-missing"),
                                MessageFormat.format(I18N.getString("error.license-missing.advice"),
                                        license, appResources.getBaseDirectory().toString()));
                    }
                }
            }

            // reject explicitly set sign to true and no valid signature key
            if (Optional.ofNullable(SIGN_BUNDLE.fetchFrom(params)).orElse(Boolean.FALSE)) {
                String signingIdentity = DEVELOPER_ID_INSTALLER_SIGNING_KEY.fetchFrom(params);
                if (signingIdentity == null) {
                    throw new ConfigException(
                            I18N.getString("error.explicit-sign-no-cert"),
                            I18N.getString("error.explicit-sign-no-cert.advice"));
                }
            }

            // hdiutil is always available so there's no need to test for availability.

            return true;
        } catch (RuntimeException re) {
            if (re.getCause() instanceof ConfigException) {
                throw (ConfigException) re.getCause();
            } else {
                throw new ConfigException(re);
            }
        }
    }

    @Override
    public File execute(Map<String, ? super Object> params, File outputParentDir) {
        return bundle(params, outputParentDir);
    }

}
