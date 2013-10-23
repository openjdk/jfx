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
import com.sun.javafx.tools.resource.mac.MacResources;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import sun.misc.BASE64Encoder;

public class MacDMGBundler extends Bundler {
    private MacAppBundler appBundler = new MacAppBundler();
    private File configRoot = null;
    private BundleParams params = null;
    File appImageDir;

    static final String DEFAULT_BACKGROUND_IMAGE="background.png";
    static final String DEFAULT_DMG_SETUP_SCRIPT="DMGsetup.scpt";

    //existing SQE tests look for "license" string in the filenames
    // when they look for unathorized license files in the build artifacts
    // Use different name to make them happy
    static final String DEFAULT_LICENSE_PLIST="lic_template.plist";

    public MacDMGBundler() {
        super();
        baseResourceLoader = MacResources.class;
    }

    @Override
    boolean validate(BundleParams p)
            throws UnsupportedPlatformException, ConfigException {
        if (!(p.type == Bundler.BundleType.ALL || p.type == Bundler.BundleType.INSTALLER)
                 || !(p.bundleFormat.isEmpty() || p.bundleFormat.contains("dmg"))) {
            return false;
        }
        //run basic validation to ensure requirements are met
        //we are not interested in return code, only possible exception
        appBundler.doValidate(p);

        // hdiutil is always available so there's no need to test for availability.
        return true;
    }

    private boolean prepareProto(BundleParams p) {
        if (!appBundler.doBundle(p, appImageDir, true)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean bundle(BundleParams p, File outdir) {
        Log.info("Building DMG package for "+p.name);

        params = p;

        appImageDir = new File(imagesRoot, "dmg.image");
        try {
            appImageDir.mkdirs();

            if (prepareProto(p) && prepareConfigFiles()) {
                File configScript = getConfig_Script();
                if (configScript.exists()) {
                    Log.info("Running shell script on application image ["
                            + configScript.getAbsolutePath() + "]");
                    IOUtils.run("bash", configScript, verbose);
                }

                return buildDMG(p, outdir);
            }
            return false;
        } catch (IOException ex) {
            Log.verbose(ex);
            return false;
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
                            + configRoot.getAbsolutePath()
                            + ". Use them to customize package.");
                }
                appImageDir = null;
            } catch (FileNotFoundException ex) {
                return false;
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
        appBundler.cleanupConfigFiles();
    }

    @Override
    public String toString() {
        return "MacOS DMG Bundler";
    }

    @Override
    public void setVerbose(boolean m) {
        super.setVerbose(m);
        appBundler.setVerbose(m);
    }

    @Override
    protected void setBuildRoot(File dir) {
        super.setBuildRoot(dir);
        configRoot = new File(dir, "macosx");
        configRoot.mkdirs();
        appBundler.setBuildRoot(dir);
    }

    private static final String hdiutil = "/usr/bin/hdiutil";

    private void prepareDMGSetupScript(String volumeName, BundleParams p) throws IOException {
        File dmgSetup = getConfig_VolumeScript();
        Log.verbose("Preparing dmg setup: "+dmgSetup.getAbsolutePath());

        //prepare config for exe
        Map<String, String> data = new HashMap<String, String>();
        data.put("DEPLOY_ACTUAL_VOLUME_NAME", volumeName);
        data.put("DEPLOY_APPLICATION_NAME", p.name);

        //treat default null as "system wide install"
        boolean systemWide = p.systemWide == null || p.systemWide;

        if (systemWide) {
            data.put("DEPLOY_INSTALL_LOCATION", "POSIX file \"/Applications\"");
            data.put("DEPLOY_INSTALL_NAME", "Applications");
        } else {
            data.put("DEPLOY_INSTALL_LOCATION", "(path to desktop folder)");
            data.put("DEPLOY_INSTALL_NAME", "Desktop");
        }

        Writer w = new BufferedWriter(new FileWriter(dmgSetup));
        w.write(preprocessTextResource(
                MacAppBundler.MAC_BUNDLER_PREFIX + dmgSetup.getName(),
                "DMG setup script", DEFAULT_DMG_SETUP_SCRIPT, data));
        w.close();
    }

    private File getConfig_VolumeScript() {
        return new File(configRoot, params.name + "-dmg-setup.scpt");
    }

    private File getConfig_VolumeBackground() {
        return new File(configRoot, params.name + "-background.png");
    }

    private File getConfig_VolumeIcon() {
        return new File(configRoot, params.name + "-volume.icns");
    }

    private File getConfig_LicenseFile() {
        return new File(configRoot, params.name + "-license.plist");
    }

    private void prepareLicense() {
        try {
            if (params.licenseFile.isEmpty()) {
                return;
            }

            File licFile = new File(params.appResources.getBaseDirectory(),
                    params.licenseFile.get(0));

            byte[] licenseContentOriginal = IOUtils.readFully(licFile);
            BASE64Encoder encoder = new BASE64Encoder();
            String licenseInBase64 = encoder.encode(licenseContentOriginal);

            Map<String, String> data = new HashMap<String, String>();
            data.put("APPLICATION_LICENSE_TEXT", licenseInBase64);

            Writer w = new BufferedWriter(new FileWriter(getConfig_LicenseFile()));
            w.write(preprocessTextResource(
                    MacAppBundler.MAC_BUNDLER_PREFIX + getConfig_LicenseFile().getName(),
                    "License setup", DEFAULT_LICENSE_PLIST, data));
            w.close();

        } catch (IOException ex) {
            Log.verbose(ex);
        }

    }

    private boolean prepareConfigFiles() throws IOException {
        File bgTarget = getConfig_VolumeBackground();
        fetchResource(MacAppBundler.MAC_BUNDLER_PREFIX + bgTarget.getName(),
                    "dmg background",
                    DEFAULT_BACKGROUND_IMAGE,
                    bgTarget);

        File iconTarget = getConfig_VolumeIcon();
        if (params.icon == null || !params.icon.exists()) {
            fetchResource(MacAppBundler.MAC_BUNDLER_PREFIX + iconTarget.getName(),
                    "volume icon",
                    MacAppBundler.TEMPLATE_BUNDLE_ICON,
                    iconTarget);
        } else {
            fetchResource(MacAppBundler.MAC_BUNDLER_PREFIX + iconTarget.getName(),
                    "volume icon",
                    params.icon,
                    iconTarget);
        }


        fetchResource(MacAppBundler.MAC_BUNDLER_PREFIX + getConfig_Script().getName(),
                "script to run after application image is populated",
                (String) null,
                getConfig_Script());

        prepareLicense();

        //In theory we need to extract name from results of attach command
        //However, this will be a problem for customization as name will
        //possibly change every time and developer will not be able to fix it
        //As we are using tmp dir chance we get "different" namr are low =>
        //Use fixed name we used for bundle
        prepareDMGSetupScript(params.name, params);

        return true;
    }

    //name of post-image script
    private File getConfig_Script() {
        return new File(configRoot, params.name + "-post-image.sh");
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
                String path = lineRead;
                File f = new File(path);
                if (f.exists() && f.canExecute()) {
                    return f.getAbsolutePath();
                }
            }
        } catch (IOException ex) {}

        return null;
    }

    private boolean buildDMG(
            BundleParams p, File outdir)
            throws IOException {
        File protoDMG = new File(imagesRoot, p.name+"-tmp.dmg");
        File finalDMG = new File(outdir, p.name+".dmg");
        File srcFolder = appImageDir; //new File(imageDir, p.name+".app");

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
                "-volname", p.name,
                "-ov", protoDMG.getAbsolutePath(),
                "-format", "UDRW");
        IOUtils.exec(pb, verbose);

        //mount temp image
        pb = new ProcessBuilder(
                hdiutil,
                "attach",
                protoDMG.getAbsolutePath(),
                "-quiet",
                "-mountroot", imagesRoot.getAbsolutePath());
        IOUtils.exec(pb, verbose);

        File mountedRoot = new File(imagesRoot.getAbsolutePath(), p.name);

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

        Log.info("Result DMG installer for " + p.name+": "
                + finalDMG.getAbsolutePath());

        return true;
   }

}
