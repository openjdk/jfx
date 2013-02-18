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
import com.sun.javafx.tools.resource.windows.WinResources;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WinMsiBundler extends Bundler {
    WinAppBundler appBundler = new WinAppBundler();
    BundleParams params;
    private File configRoot = null;
    File imageDir = null;

    private boolean menuShortcut = false;
    private boolean desktopShortcut = false;

    private boolean canUseWix36Features = false;

    public WinMsiBundler() {
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
                Pattern pattern = Pattern.compile("version (\\d+.\\d+)");
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
                Log.verbose(e);
            }
            return 0f;
        }
    }

    @Override
    boolean validate(BundleParams p) throws UnsupportedPlatformException, ConfigException {
        if (!(p.type == Bundler.BundleType.ALL || p.type == Bundler.BundleType.INSTALLER)
                 || !(p.bundleFormat == null || "msi".equals(p.bundleFormat))) {
            return false;
        }
        //run basic validation to ensure requirements are met
        //we are not interested in return code, only possible exception
        appBundler.doValidate(p);

        double candleVersion = findTool(TOOL_CANDLE);
        double lightVersion = findTool(TOOL_LIGHT);

        //WiX 3.0+ is required
        double minVersion = 3.0f;
        boolean bad = false;

        if (candleVersion < minVersion) {
            Log.verbose("Detected ["+TOOL_CANDLE+"] version "+candleVersion +
                        " but version "+minVersion+" is required.");
            bad = true;
        }
        if (lightVersion < minVersion) {
            Log.verbose("Detected ["+TOOL_LIGHT+"] version "+lightVersion +
                        " but version "+minVersion+" is required.");
            bad = true;
        }

        if (bad){
            throw new Bundler.ConfigException(
                    "Can not find WiX tools (light.exe, candle.exe).",
                    "  Download WiX 3.0 or later from http://wix.sf.net and add it to the PATH.");
        }

        if (lightVersion >= 3.6f) {
            Log.verbose("WiX 3.6 detected. Enabling advanced cleanup action.");
            canUseWix36Features = true;
        }

        /********* validate bundle parameters *************/

        if (!isVersionStringValid(p.appVersion)) {
            throw new Bundler.ConfigException(
                    "Version string is not compatible with MSI rules ["+p.appVersion+"].",
                    "For details see (http://msdn.microsoft.com/en-us/library/aa370859%28v=VS.85%29.aspx).");
        }

        return true;
    }

    //http://msdn.microsoft.com/en-us/library/aa370859%28v=VS.85%29.aspx
    //The format of the string is as follows:
    //    major.minor.build
    //The first field is the major version and has a maximum value of 255.
    //The second field is the minor version and has a maximum value of 255.
    //The third field is called the build version or the update version and
    // has a maximum value of 65,535.
    static boolean isVersionStringValid(String v) {
        if (v == null) {
            return true;
        }

        String p[] = v.split("\\.");
        if (p.length > 3) {
            Log.verbose("Version sting may have up to 3 components - major.minor.build .");
            return false;
        }

        try {
            int val = Integer.parseInt(p[0]);
            if (val < 0 || val > 255) {
                Log.verbose("Major version must be in the range [0, 255]");
                return false;
            }
            if (p.length > 1) {
                val = Integer.parseInt(p[1]);
                if (val < 0 || val > 255) {
                    Log.verbose("Minor version must be in the range [0, 255]");
                    return false;
                }
            }
            if (p.length > 2) {
                val = Integer.parseInt(p[2]);
                if (val < 0 || val > 65535) {
                    Log.verbose("Build part of version must be in the range [0, 65535]");
                    return false;
                }
            }
        } catch (NumberFormatException ne) {
                Log.verbose("Failed to convert version component to int.");
                Log.verbose(ne);
                return false;
        }

        return true;
    }

    private boolean prepareProto() {
        File bundleRoot = getImageRootDir().getParentFile();
        if (!appBundler.doBundle(params, bundleRoot, true)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean bundle(BundleParams p, File outdir) {
        imageDir = new File(imagesRoot, "win-msi");
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

            if (prepareProto() && prepareWiXConfig()
                    && prepareBasicProjectConfig()) {
                File configScriptSrc = getConfig_Script();
                if (configScriptSrc.exists()) {
                    //we need to be running post script in the image folder

                    // NOTE: Would it be better to generate it to the image folder
                    // and save only if "verbose" is requested?

                    // for now we replicate it
                    File configScript = new File(imageDir, configScriptSrc.getName());
                    IOUtils.copyFile(configScriptSrc, configScript);
                    Log.info("Running WSH script on application image [" +
                            configScript.getAbsolutePath() + "]");
                    IOUtils.run("wscript", configScript, verbose);
                }
                return buildMSI(outdir);
            }
            return false;
        } catch (IOException ex) {
            Log.verbose(ex);
            return false;
        } finally {
            try {
                if (imageDir != null && !Log.isDebug()) {
                    IOUtils.deleteRecursive(imageDir);
                } else if (imageDir != null) {
                    Log.info("Kept working directory for debug: "+
                            imageDir.getAbsolutePath());
                }
                if (verbose) {
                    Log.info("  Config files are saved to " +
                            configRoot.getAbsolutePath()  +
                            ". Use them to customize package.");
                } else {
                    cleanupConfigFiles();
                }
            } catch (FileNotFoundException ex) {
                return false;
            }
        }
    }

    protected void cleanupConfigFiles() {
        if (getConfig_ProjectFile() != null) {
            getConfig_ProjectFile().delete();
        }
        if (getConfig_Script() != null) {
            getConfig_Script().delete();
        }
    }

    //name of post-image script
    private File getConfig_Script() {
        return new File(configRoot,
                WinAppBundler.getAppName(params) + "-post-image.wsf");
    }

    @Override
    public String toString() {
        return "MSI Bundler (WiX based)";
    }

    private boolean prepareBasicProjectConfig() throws IOException {
        fetchResource(WinAppBundler.WIN_BUNDLER_PREFIX + getConfig_Script().getName(),
                "script to run after application image is populated",
                (String) null,
                getConfig_Script());
        return true;
    }

    private String relativePath(File basedir, File file) {
        return file.getAbsolutePath().substring(
                basedir.getAbsolutePath().length()+1);
    }

    private String getVendor() {
        if (params.vendor != null) {
             return params.vendor;
        } else {
            return "Unknown";
        }
    }

    UUID getUpgradeGUID() {
        UUID uid = null;
        if (params.identifier != null) {
            try {
                uid = UUID.fromString(params.identifier);
            } catch (IllegalArgumentException iae) {
                Log.verbose("Can not use app identifier [" + params.identifier +
                        "] as upgrade GUID for MSI. Wrong format.");
            }
        }
        if (uid == null) {
            //default - use random
            uid = UUID.randomUUID();
            Log.verbose("Generated random upgrade GUID for MSI [" + uid.toString() +
                "]. To overwrite: specify GUID as id attribute of application tag.");
        }
        return uid;
    }

    private String getDescription() {
        if (params.description != null) {
            //strip quotes if any
            return params.description.replaceAll("\"", "'");
        }
        return "none";
    }

    //for MSI default is system wide install
    private boolean isSystemWide() {
        return params.systemWide == null || params.systemWide;
    }

    private String getVersion() {
        return (params.appVersion != null) ? params.appVersion : "1.0";
    }

    private File getImageRootDir() {
        File root = WinAppBundler.getLauncher(imageDir, params).getParentFile();
        return root;
    }

    boolean prepareMainProjectFile() throws IOException {
        Map<String, String> data = new HashMap<String, String>();

        UUID productGUID = UUID.randomUUID();

        Log.verbose("Generated product GUID: "+productGUID.toString());

        //we use random GUID for product itself but
        // user provided for upgrade guid
        // Upgrade guid is importnat to decide whether it is upgrade of installed
        //  app. I.e. we need it to be the same for 2 different versions of app if possible
        data.put("PRODUCT_GUID", productGUID.toString());
        data.put("PRODUCT_UPGRADE_GUID", getUpgradeGUID().toString());

        data.put("APPLICATION_NAME", WinAppBundler.getAppName(params));
        data.put("APPLICATION_DESCRIPTION", getDescription());
        data.put("APPLICATION_VENDOR", getVendor());
        data.put("APPLICATION_VERSION", getVersion());

        //WinAppBundler will add application folder again => step out
        File launcher = WinAppBundler.getLauncher(
                getImageRootDir().getParentFile(), params);

        String launcherPath = relativePath(getImageRootDir(), launcher);
        data.put("APPLICATION_LAUNCHER", launcherPath);

        String iconPath = launcherPath.replace(".exe", ".ico");
        data.put("APPLICATION_ICON", iconPath);

        data.put("REGISTRY_ROOT", getRegistryRoot());

        data.put("WIX36_ONLY_START",
                canUseWix36Features ? "" : "<!--");
        data.put("WIX36_ONLY_END",
                canUseWix36Features ? "" : "-->");

        if (isSystemWide()) {
            data.put("INSTALL_SCOPE", "perMachine");
        } else {
            data.put("INSTALL_SCOPE", "perUser");
        }

        Writer w = new BufferedWriter(new FileWriter(getConfig_ProjectFile()));
        w.write(preprocessTextResource(
                WinAppBundler.WIN_BUNDLER_PREFIX + getConfig_ProjectFile().getName(),
                "WiX config file", MSI_PROJECT_TEMPLATE, data));
        w.close();
        return true;
    }
    private int id;
    private int compId;
    private final static String LAUNCHER_ID = "LauncherId";

    private void walkFileTree(File root, PrintStream out, String prefix) {
        List<File> dirs = new ArrayList<File>();
        List<File> files = new ArrayList<File>();

        if (!root.isDirectory()) {
            throw new RuntimeException(
               "Can not walk [" + root.getAbsolutePath() + "] - it is not a valid directory");
        }

        //sort to files and dirs
        for (File f : root.listFiles()) {
            if (f.isDirectory()) {
                dirs.add(f);
            } else {
                files.add(f);
            }
        }

        //have files => need to output component
        out.println(prefix + " <Component Id=\"comp" + (compId++) + "\" DiskId=\"1\""
                + " Guid=\"" + UUID.randomUUID().toString() + "\">");
        out.println("  <CreateFolder/>");
        out.println("  <RemoveFolder Id=\"RemoveDir" + (id++) + "\" On=\"uninstall\" />");

        boolean needRegistryKey = !isSystemWide();
        File launcherFile = WinAppBundler.getLauncher(
                    /* Step up as WinAppBundler will add app folder */
                    getImageRootDir().getParentFile(), params);
        //Find out if we need to use registry. We need it if
        //  - we doing user level install as file can not serve as KeyPath
        //  - if we adding shortcut in this component
        for (File f: files) {
            boolean isLauncher = f.equals(launcherFile);
            if (isLauncher) {
                needRegistryKey = true;
            }
        }

        if (needRegistryKey) {
            //has to be under HKCU to make WiX happy
            out.println(prefix + "    <RegistryKey Root=\"HKCU\" "
                    + " Key=\"Software\\" + getVendor() + "\\"
                    + WinAppBundler.getAppName(params) + "\""
                    + (canUseWix36Features
                    ? ">" : " Action=\"createAndRemoveOnUninstall\">"));
            out.println(prefix + "     <RegistryValue Name=\"Version\" Value=\""
                    + getVersion() + "\" Type=\"string\" KeyPath=\"yes\"/>");
            out.println(prefix + "   </RegistryKey>");
        }

        for (File f : files) {
            boolean isLauncher = f.equals(WinAppBundler.getLauncher(
                    /* Step up as WinAppBundler will add app folder */
                    getImageRootDir().getParentFile(), params));
            boolean doShortcuts = isLauncher && (menuShortcut || desktopShortcut);
            out.println(prefix + "   <File Id=\"" +
                    (isLauncher ? LAUNCHER_ID : ("FileId" + (id++))) + "\""
                    + " Name=\"" + f.getName() + "\" "
                    + " Source=\"" + relativePath(getImageRootDir(), f) + "\">");
            if (doShortcuts && desktopShortcut) {
                out.println(prefix + "  <Shortcut Id=\"desktopShortcut\" Directory=\"DesktopFolder\""
                        + " Name=\"" + WinAppBundler.getAppName(params) + "\" WorkingDirectory=\"INSTALLDIR\""
                        + " Advertise=\"no\" Icon=\"DesktopIcon.exe\" IconIndex=\"0\" />");
            }
            if (doShortcuts && menuShortcut) {
                out.println(prefix + "     <Shortcut Id=\"ExeShortcut\" Directory=\"ProgramMenuDir\""
                        + " Name=\"" + WinAppBundler.getAppName(params)
                        + "\" Advertise=\"no\" Icon=\"StartMenuIcon.exe\" IconIndex=\"0\" />");
            }
            out.println(prefix + "   </File>");
        }
        out.println(prefix + " </Component>");

        for (File d : dirs) {
            out.println(prefix + " <Directory Id=\"dirid" + (id++)
                    + "\" Name=\"" + d.getName() + "\">");
            walkFileTree(d, out, prefix + " ");
            out.println(prefix + " </Directory>");
        }
    }

    String getRegistryRoot() {
        if (isSystemWide()) {
            return "HKLM";
        } else {
            return "HKCU";
        }
    }

    boolean prepareContentList() throws FileNotFoundException {
        File f = new File(configRoot, MSI_PROJECT_CONTENT_FILE);
        PrintStream out = new PrintStream(f);

        //opening
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        out.println("<Include>");

        out.println(" <Directory Id=\"TARGETDIR\" Name=\"SourceDir\">");
        if (isSystemWide()) {
            //install to programfiles
            out.println("  <Directory Id=\"ProgramFilesFolder\" Name=\"PFiles\">");
        } else {
            //install to user folder
            out.println("  <Directory Name=\"AppData\" Id=\"LocalAppDataFolder\">");
        }
        out.println("   <Directory Id=\"APPLICATIONFOLDER\" Name=\""
                + WinAppBundler.getAppName(params) + "\">");

        //dynamic part
        id = 0;
        compId = 0; //reset counters
        walkFileTree(getImageRootDir(), out, "    ");

        //closing
        out.println("   </Directory>");
        out.println("  </Directory>");

        //for shortcuts
        if (desktopShortcut) {
            out.println("  <Directory Id=\"DesktopFolder\" />");
        }
        if (menuShortcut) {
            out.println("  <Directory Id=\"ProgramMenuFolder\">");
            out.println("    <Directory Id=\"ProgramMenuDir\" Name=\"" + getVendor() + "\">");
            out.println("      <Component Id=\"comp" + (compId++) + "\""
                    + " Guid=\"" + UUID.randomUUID().toString() + "\">");
            out.println("        <RemoveFolder Id=\"ProgramMenuDir\" On=\"uninstall\" />");
            //This has to be under HKCU to make WiX happy.
            //There are numberous discussions on this amoung WiX users
            // (if user A installs and user B uninstalls then key is left behind)
            //and there are suggested workarounds but none of them are appealing.
            //Leave it for now
            out.println("         <RegistryValue Root=\"HKCU\" Key=\"Software\\"
                    + getVendor() + "\\" + WinAppBundler.getAppName(params)
                    + "\" Type=\"string\" Value=\"\" />");
            out.println("      </Component>");
            out.println("    </Directory>");
            out.println(" </Directory>");
        }

        out.println(" </Directory>");

        out.println(" <Feature Id=\"DefaultFeature\" Title=\"Main Feature\" Level=\"1\">");
        for (int j = 0; j < compId; j++) {
            out.println("    <ComponentRef Id=\"comp" + j + "\" />");
        }
        //component is defined in the template.wsx
        out.println("    <ComponentRef Id=\"CleanupMainApplicationFolder\" />");
        out.println(" </Feature>");
        out.println("</Include>");

        out.close();
        return true;
    }

    private File getConfig_ProjectFile() {
        return new File(configRoot, WinAppBundler.getAppName(params) + ".wxs");
    }

    private boolean prepareWiXConfig() throws IOException {
        return prepareMainProjectFile() && prepareContentList();

    }
    private final static String MSI_PROJECT_TEMPLATE = "template.wxs";
    private final static String MSI_PROJECT_CONTENT_FILE = "bundle.wxi";

    private static final String TOOL_CANDLE = "candle";
    private static final String TOOL_LIGHT = "light";

    private boolean buildMSI(File outdir) throws IOException {
        File tmpDir = new File(buildRoot, "tmp");
        File candleOut = new File(tmpDir, WinAppBundler.getAppName(params)+".wixobj");
        File msiOut = new File(outdir, WinAppBundler.getAppName(params)
                + "-" + getVersion() + ".msi");

        Log.verbose("Preparing MSI config: "+msiOut.getAbsolutePath());

        msiOut.getParentFile().mkdirs();

        //run candle
        ProcessBuilder pb = new ProcessBuilder(
                TOOL_CANDLE,
                "-nologo",
                getConfig_ProjectFile().getAbsolutePath(),
                "-ext", "WixUtilExtension",
                "-out", candleOut.getAbsolutePath());
        pb = pb.directory(getImageRootDir());
        IOUtils.exec(pb, verbose);

        Log.verbose("Generating MSI: "+msiOut.getAbsolutePath());

        //create .msi
        pb = new ProcessBuilder(
                TOOL_LIGHT,
                "-nologo",
                "-spdb",
                "-sice:60", //ignore warnings due to "missing launcguage info" (ICE60)
                candleOut.getAbsolutePath(),
                "-ext", "WixUtilExtension",
                "-out", msiOut.getAbsolutePath());
        pb = pb.directory(getImageRootDir());
        IOUtils.exec(pb, verbose);

        candleOut.delete();
        IOUtils.deleteRecursive(tmpDir);

        return true;
    }
}
