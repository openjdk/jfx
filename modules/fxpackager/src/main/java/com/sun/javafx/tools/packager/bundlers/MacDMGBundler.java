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

import com.oracle.bundlers.BundlerParamInfo;
import com.oracle.bundlers.mac.MacBaseInstallerBundler;
import com.sun.javafx.tools.packager.Log;
import com.sun.javafx.tools.resource.mac.MacResources;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.util.*;

import static com.oracle.bundlers.StandardBundlerParam.*;

public class MacDMGBundler extends MacBaseInstallerBundler {


    static final String DEFAULT_BACKGROUND_IMAGE="background.png";
    static final String DEFAULT_DMG_SETUP_SCRIPT="DMGsetup.scpt";
    static final String TEMPLATE_BUNDLE_ICON = "GenericApp.icns";

    //existing SQE tests look for "license" string in the filenames
    // when they look for unathorized license files in the build artifacts
    // Use different name to make them happy
    static final String DEFAULT_LICENSE_PLIST="lic_template.plist";
    private Map<String, ? super Object> params;

    public MacDMGBundler() {
        super();
        baseResourceLoader = MacResources.class;
    }

    //@Override
    public File bundle(Map<String, ? super Object> p, File outdir) {
        Log.info("Building DMG package for " + NAME.fetchFrom(p));

        params = p;

        File appImageDir = APP_IMAGE_BUILD_ROOT.fetchFrom(params);
        try {
            appImageDir.mkdirs();

            if (prepareAppBundle(p) && prepareConfigFiles()) {
                File configScript = getConfig_Script();
                if (configScript.exists()) {
                    Log.info("Running shell script on application image ["
                            + configScript.getAbsolutePath() + "]");
                    IOUtils.run("bash", configScript, verbose);
                }

                return buildDMG(p, outdir);
            }
            return null;
        } catch (ConfigException e) {
            Log.info("Bundler " + getName() + " skipped because of a configuration problem: " + e.getMessage() + "\nAdvice to fix: " + e.getAdvice());
            return null;
        } catch (IOException ex) {
            Log.verbose(ex);
            return null;
        } finally {
            try {
                if (appImageDir != null && !Log.isDebug()) {
                    IOUtils.deleteRecursive(appImageDir);
                } else if (appImageDir != null) {
                    Log.info("[DEBUG] Intermediate application bundle image: "+
                            appImageDir.getAbsolutePath());
                }
                if (!verbose) {
                    //cleanup
                    cleanupConfigFiles();
                } else {
                    Log.info("  Config files are saved to "
                            + CONFIG_ROOT.fetchFrom(p).getAbsolutePath()
                            + ". Use them to customize package.");
                }
                appImageDir = null;
            } catch (FileNotFoundException ex) {
                //noinspection ReturnInsideFinallyBlock
                return null;
            }
        }
    }

    //remove
    protected void cleanupConfigFiles() {
        if (getConfig_VolumeBackground() != null) {
            getConfig_VolumeBackground().delete();
        }
        if (getConfig_VolumeIcon() != null) {
            getConfig_VolumeIcon().delete();
        }
        if (getConfig_VolumeScript() != null) {
            getConfig_VolumeScript().delete();
        }
        if (getConfig_Script() != null) {
            getConfig_Script().delete();
        }
        if (getConfig_LicenseFile() != null) {
            getConfig_LicenseFile().delete();
        }
        APP_BUNDLER.fetchFrom(params).cleanupConfigFiles();
    }

    @Override
    public String toString() {
        return getName();
    }

//    @Override
//    protected void setBuildRoot(File dir) {
//        super.setBuildRoot(dir);
//        configRoot = new File(dir, "macosx");
//        configRoot.mkdirs();
//        APP_BUNDLER.fetchFrom(params).setBuildRoot(dir);
//    }

    private static final String hdiutil = "/usr/bin/hdiutil";

    private void prepareDMGSetupScript(String volumeName, Map<String, ? super Object> p) throws IOException {
        File dmgSetup = getConfig_VolumeScript();
        Log.verbose("Preparing dmg setup: "+dmgSetup.getAbsolutePath());

        //prepare config for exe
        Map<String, String> data = new HashMap<>();
        data.put("DEPLOY_ACTUAL_VOLUME_NAME", volumeName);
        data.put("DEPLOY_APPLICATION_NAME", NAME.fetchFrom(p));

        //treat default null as "system wide install"
        boolean systemWide = SYSTEM_WIDE.fetchFrom(p) == null || SYSTEM_WIDE.fetchFrom(p);

        if (systemWide) {
            data.put("DEPLOY_INSTALL_LOCATION", "POSIX file \"/Applications\"");
            data.put("DEPLOY_INSTALL_NAME", "Applications");
        } else {
            data.put("DEPLOY_INSTALL_LOCATION", "(path to desktop folder)");
            data.put("DEPLOY_INSTALL_NAME", "Desktop");
        }

        Writer w = new BufferedWriter(new FileWriter(dmgSetup));
        w.write(preprocessTextResource(
                com.sun.javafx.tools.packager.bundlers.MacAppBundler.MAC_BUNDLER_PREFIX + dmgSetup.getName(),
                "DMG setup script", DEFAULT_DMG_SETUP_SCRIPT, data));
        w.close();
    }

    private File getConfig_VolumeScript() {
        return new File(CONFIG_ROOT.fetchFrom(params), NAME.fetchFrom(params) + "-dmg-setup.scpt");
    }

    private File getConfig_VolumeBackground() {
        return new File(CONFIG_ROOT.fetchFrom(params), NAME.fetchFrom(params) + "-background.png");
    }

    private File getConfig_VolumeIcon() {
        return new File(CONFIG_ROOT.fetchFrom(params), NAME.fetchFrom(params) + "-volume.icns");
    }

    private File getConfig_LicenseFile() {
        return new File(CONFIG_ROOT.fetchFrom(params), NAME.fetchFrom(params) + "-license.plist");
    }

    private void prepareLicense() {
        try {
            if (LICENSE_FILES.fetchFrom(params).isEmpty()) {
                return;
            }

            File licFile = new File(APP_RESOURCES.fetchFrom(params).getBaseDirectory(),
                    LICENSE_FILES.fetchFrom(params).get(0));

            byte[] licenseContentOriginal = IOUtils.readFully(licFile);
            BASE64Encoder encoder = new BASE64Encoder();
            String licenseInBase64 = encoder.encode(licenseContentOriginal);

            Map<String, String> data = new HashMap<>();
            data.put("APPLICATION_LICENSE_TEXT", licenseInBase64);

            Writer w = new BufferedWriter(new FileWriter(getConfig_LicenseFile()));
            w.write(preprocessTextResource(
                    com.sun.javafx.tools.packager.bundlers.MacAppBundler.MAC_BUNDLER_PREFIX + getConfig_LicenseFile().getName(),
                    "License setup", DEFAULT_LICENSE_PLIST, data));
            w.close();

        } catch (IOException ex) {
            Log.verbose(ex);
        }

    }

    private boolean prepareConfigFiles() throws IOException {
        File bgTarget = getConfig_VolumeBackground();
        fetchResource(com.sun.javafx.tools.packager.bundlers.MacAppBundler.MAC_BUNDLER_PREFIX + bgTarget.getName(),
                "dmg background",
                DEFAULT_BACKGROUND_IMAGE,
                bgTarget);

        File iconTarget = getConfig_VolumeIcon();
        if (ICON.fetchFrom(params) == null || !ICON.fetchFrom(params).exists()) {
            fetchResource(com.sun.javafx.tools.packager.bundlers.MacAppBundler.MAC_BUNDLER_PREFIX + iconTarget.getName(),
                    "volume icon",
                    TEMPLATE_BUNDLE_ICON,
                    iconTarget);
        } else {
            fetchResource(com.sun.javafx.tools.packager.bundlers.MacAppBundler.MAC_BUNDLER_PREFIX + iconTarget.getName(),
                    "volume icon",
                    ICON.fetchFrom(params),
                    iconTarget);
        }


        fetchResource(com.sun.javafx.tools.packager.bundlers.MacAppBundler.MAC_BUNDLER_PREFIX + getConfig_Script().getName(),
                "script to run after application image is populated",
                (String) null,
                getConfig_Script());

        prepareLicense();

        //In theory we need to extract name from results of attach command
        //However, this will be a problem for customization as name will
        //possibly change every time and developer will not be able to fix it
        //As we are using tmp dir chance we get "different" namr are low =>
        //Use fixed name we used for bundle
        prepareDMGSetupScript(NAME.fetchFrom(params), params);

        return true;
    }

    //name of post-image script
    private File getConfig_Script() {
        return new File(CONFIG_ROOT.fetchFrom(params), NAME.fetchFrom(params) + "-post-image.sh");
    }

    //Location of SetFile utility may be different depending on MacOS version
    // We look for several known places and if none of them work will
    // try ot find it
    private String findSetFileUtility() {
        String typicalPaths[] = {"/Developer/Tools/SetFile",
                "/usr/bin/SetFile", "/Developer/usr/bin/SetFile"};

        for (String path: typicalPaths) {
            File f = new File(path);
            if (f.exists() && f.canExecute()) {
                return path;
            }
        }

        //generic find attempt
        try {
            ProcessBuilder pb = new ProcessBuilder("xcrun", "-find", "SetFile");
            Process p = pb.start();
            InputStreamReader isr = new InputStreamReader(p.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String lineRead = br.readLine();
            if (lineRead != null) {
                File f = new File(lineRead);
                if (f.exists() && f.canExecute()) {
                    return f.getAbsolutePath();
                }
            }
        } catch (IOException ignored) {}

        return null;
    }

    private File buildDMG(
            Map<String, ? super Object> p, File outdir)
            throws IOException, ConfigException {
        File protoDMG = new File(IMAGES_ROOT.fetchFrom(p), NAME.fetchFrom(p) +"-tmp.dmg");
        File finalDMG = new File(outdir,  NAME.fetchFrom(p) +".dmg");

        File srcFolder = APP_IMAGE_BUILD_ROOT.fetchFrom(p); //new File(imageDir, p.name+".app");
        File predefinedImage = getPredefinedImage(p);
        if (predefinedImage != null) {
            srcFolder = predefinedImage;
        }

        Log.verbose(" Creating DMG file: " + finalDMG.getAbsolutePath());

        protoDMG.delete();
        if (finalDMG.exists() && !finalDMG.delete()) {
            throw new IOException("Dmg file exists (" + finalDMG.getAbsolutePath()
                    +" and can not be removed.");
        }

        protoDMG.getParentFile().mkdirs();
        finalDMG.getParentFile().mkdirs();

        //create temp image
        ProcessBuilder pb = new ProcessBuilder(
                hdiutil,
                "create",
                "-quiet",
                "-srcfolder", srcFolder.getAbsolutePath(),
                "-volname", NAME.fetchFrom(p),
                "-ov", protoDMG.getAbsolutePath(),
                "-format", "UDRW");
        IOUtils.exec(pb, verbose);

        //mount temp image
        pb = new ProcessBuilder(
                hdiutil,
                "attach",
                protoDMG.getAbsolutePath(),
                "-quiet",
                "-mountroot", IMAGES_ROOT.fetchFrom(p).getAbsolutePath());
        IOUtils.exec(pb, verbose);

        File mountedRoot = new File(IMAGES_ROOT.fetchFrom(p).getAbsolutePath(), NAME.fetchFrom(p));

        //background image
        File bgdir = new File(mountedRoot, ".background");
        bgdir.mkdirs();
        IOUtils.copyFile(getConfig_VolumeBackground(),
                new File(bgdir, "background.png"));

        //volume icon
        File volumeIconFile = new File(mountedRoot, ".VolumeIcon.icns");
        IOUtils.copyFile(getConfig_VolumeIcon(),
                volumeIconFile);

        pb = new ProcessBuilder("osascript",
                getConfig_VolumeScript().getAbsolutePath());
        IOUtils.exec(pb, verbose);

        //Indicate that we want a custom icon
        //NB: attributes of the root directory are ignored when creating the volume
        //  Therefore we have to do this after we mount image
        String setFileUtility = findSetFileUtility();
        if (setFileUtility != null) { //can not find utility => keep going without icon
            volumeIconFile.setWritable(true);
            //The “creator” attribute on a file is a legacy attribute
            // but it seems Finder expects these bytes to be “icnC” for the volume icon
            // (http://endrift.com/blog/2010/06/14/dmg-files-volume-icons-cli/)
            pb = new ProcessBuilder(
                    setFileUtility,
                    "-c", "icnC",
                    volumeIconFile.getAbsolutePath());
            IOUtils.exec(pb, verbose);
            volumeIconFile.setReadOnly();

            pb = new ProcessBuilder(
                    setFileUtility,
                    "-a", "C",
                    mountedRoot.getAbsolutePath());
            IOUtils.exec(pb, verbose);
        } else {
            Log.verbose("Skip enabling custom icon as SetFile utility is not found");
        }

        // Detach the temporary image
        pb = new ProcessBuilder(
                hdiutil,
                "detach",
                "-quiet",
                mountedRoot.getAbsolutePath());
        IOUtils.exec(pb, verbose);

        // Compress it to a new image
        pb = new ProcessBuilder(
                hdiutil,
                "convert",
                protoDMG.getAbsolutePath(),
                "-quiet",
                "-format", "UDZO",
                "-o", finalDMG.getAbsolutePath());
        IOUtils.exec(pb, verbose);

        //add license if needed
        if (getConfig_LicenseFile().exists()) {
            //hdiutil unflatten your_image_file.dmg
            pb = new ProcessBuilder(
                    hdiutil,
                    "unflatten",
                    finalDMG.getAbsolutePath()
            );
            IOUtils.exec(pb, verbose);

            //add license
            pb = new ProcessBuilder(
                    hdiutil,
                    "udifrez",
                    finalDMG.getAbsolutePath(),
                    "-xml",
                    getConfig_LicenseFile().getAbsolutePath()
            );
            IOUtils.exec(pb, verbose);

            //hdiutil flatten your_image_file.dmg
            pb = new ProcessBuilder(
                    hdiutil,
                    "flatten",
                    finalDMG.getAbsolutePath()
            );
            IOUtils.exec(pb, verbose);

        }

        //Delete the temporary image
        protoDMG.delete();

        Log.info("Result DMG installer for " + NAME.fetchFrom(p) +": "
                + finalDMG.getAbsolutePath());

        return finalDMG;
    }


    //////////////////////////////////////////////////////////////////////////////////
    // Implement Bundler
    //////////////////////////////////////////////////////////////////////////////////

    @Override
    public String getName() {
        return "DMG Installer";
    }

    @Override
    public String getDescription() {
        return "Mac DMG Installer Bundle.";
    }

    @Override
    public String getID() {
        return "dmg";
    }

    @Override
    public Collection<BundlerParamInfo<?>> getBundleParameters() {
        //Add DMG Specific parameters as required
        return super.getBundleParameters();
    }

    @Override
    public boolean validate(Map<String, ? super Object> params) throws UnsupportedPlatformException, ConfigException {
        if (params == null) throw new ConfigException("Parameters map is null.", "Pass in a non-null parameters map.");

        // hdiutil is always available so there's no need to test for availability.
        //run basic validation to ensure requirements are met

        //run basic validation to ensure requirements are met
        //we are not interested in return code, only possible exception
        APP_BUNDLER.fetchFrom(params).doValidate(params);
        return true;
    }

    @Override
    public File execute(Map<String, ? super Object> params, File outputParentDir) {
        return bundle(params, outputParentDir);
    }
}
