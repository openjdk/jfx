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

import static com.oracle.bundlers.windows.WindowsBundlerParam.*;

public class WinMsiBundler  extends AbstractBundler {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle("com.oracle.bundlers.windows.WinMsiBundler");

    public static final BundlerParamInfo<WinAppBundler> APP_BUNDLER = new WindowsBundlerParam<>(
            I18N.getString("param.app-bundler.name"),
            I18N.getString("param.app-bundler.description"),
            "winAppBundler", //KEY
            WinAppBundler.class, null, params -> new WinAppBundler(), false, null);

    public static final BundlerParamInfo<Boolean> CAN_USE_WIX36 = new WindowsBundlerParam<>(
            I18N.getString("param.can-use-wix36.name"),
            I18N.getString("param.can-use-wix36.description"),
            "canUseWix36", //KEY
            Boolean.class, null, params -> false, false, Boolean::valueOf);

    public static final BundlerParamInfo<File> OUT_DIR = new WindowsBundlerParam<>(
            I18N.getString("param.out-dir.name"),
            I18N.getString("param.out-dir.description"),
            "outDir", //KEY
            File.class, null, params -> null, false, s -> null);

    public static final BundlerParamInfo<File> CONFIG_ROOT = new WindowsBundlerParam<>(
            I18N.getString("param.config-root.name"),
            I18N.getString("param.config-root.description"),
            "configRoot", //KEY
            File.class, null,params -> {
                File imagesRoot = new File(StandardBundlerParam.BUILD_ROOT.fetchFrom(params), "windows");
                imagesRoot.mkdirs();
                return imagesRoot;
            }, false, s -> null);

    public static final BundlerParamInfo<File> IMAGE_DIR = new WindowsBundlerParam<>(
            I18N.getString("param.image-dir.name"),
            I18N.getString("param.image-dir.description"),
            "imageDir", //KEY
            File.class, null, params -> {
                File imagesRoot = IMAGES_ROOT.fetchFrom(params);
                return new File(imagesRoot, "win-msi");
            }, false, s -> null);

    public static final BundlerParamInfo<File> APP_DIR = new WindowsBundlerParam<>(
            I18N.getString("param.app-dir.name"),
            I18N.getString("param.app-dir.description"),
            "appDir",
            File.class, null, null, false, s -> null);

    public static final StandardBundlerParam<Boolean> MSI_SYSTEM_WIDE  =
            new StandardBundlerParam<>(
                    I18N.getString("param.system-wide.name"),
                    I18N.getString("param.system-wide.description"),
                    "winmsi" + BundleParams.PARAM_SYSTEM_WIDE, //KEY
                    Boolean.class,
                    new String[] {BundleParams.PARAM_SYSTEM_WIDE},
                    params -> true, // MSIs default to system wide
                    false,
                    s -> (s == null || "null".equalsIgnoreCase(s))? null : Boolean.valueOf(s) // valueOf(null) is false, and we actually do want null
            );


    public static final BundlerParamInfo<UUID> UPGRADE_UUID = new WindowsBundlerParam<>(
            I18N.getString("param.upgrade-uuid.name"),
            I18N.getString("param.upgrade-uuid.description"),
            "upgradeUUID", //KEY
            UUID.class, null, params -> UUID.randomUUID(), // TODO check to see if identifier is a valid UUID during default 
            false, UUID::fromString);

    private static final String TOOL_CANDLE = "candle.exe";
    private static final String TOOL_LIGHT = "light.exe";
    // autodetect just v3.7 and v3.8
    private static final String AUTODETECT_DIRS = ";C:\\Program Files (x86)\\WiX Toolset v3.8\\bin;C:\\Program Files\\WiX Toolset v3.8\\bin;C:\\Program Files (x86)\\WiX Toolset v3.7\\bin;C:\\Program Files\\WiX Toolset v3.7\\bin";

    public static final BundlerParamInfo<String> TOOL_CANDLE_EXECUTABLE = new WindowsBundlerParam<>(
            I18N.getString("param.candle-path.name"),
            I18N.getString("param.candle-path.description"),
            "win.candle.exe", //KEY
            String.class, null, params -> {
                for (String dirString : (System.getenv("PATH") + AUTODETECT_DIRS).split(";")) {
                    File f = new File(dirString.replace("\"", ""), TOOL_CANDLE);
                    if (f.isFile()) {
                        return f.toString();
                    }
                }
                return null;
            }, false, null);

    public static final BundlerParamInfo<String> TOOL_LIGHT_EXECUTABLE = new WindowsBundlerParam<>(
            I18N.getString("param.light-path.name"),
            I18N.getString("param.light-path.descrption"),
            "win.light.exe", //KEY
            String.class, null, params -> {
                for (String dirString : (System.getenv("PATH") + AUTODETECT_DIRS).split(";")) {
                    File f = new File(dirString.replace("\"", ""), TOOL_LIGHT);
                    if (f.isFile()) {
                        return f.toString();
                    }
                }
                return null;
            }, false, null);

    public WinMsiBundler() {
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
        return "msi"; //KEY
    }

    @Override
    public BundleType getBundleType() {
        return BundleType.INSTALLER;
    }

    @Override
    public Collection<BundlerParamInfo<?>> getBundleParameters() {
        Collection<BundlerParamInfo<?>> results = new LinkedHashSet<>();
        results.addAll(WinAppBundler.getAppBundleParameters());
        results.addAll(getMsiBundleParameters());
        return results;
    }

    public static Collection<BundlerParamInfo<?>> getMsiBundleParameters() {
        return Arrays.asList(
                APP_BUNDLER,
                APP_DIR,
                BUILD_ROOT,
                CAN_USE_WIX36,
                //CONFIG_ROOT, // duplicate from getAppBundleParameters
                DESCRIPTION,
                IMAGE_DIR,
                IMAGES_ROOT,
                MENU_GROUP,
                MENU_HINT,
                MSI_SYSTEM_WIDE,
                SHORTCUT_HINT,
                UPGRADE_UUID,
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
                Log.verbose(e);
            }
            return 0f;
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

        double candleVersion = findToolVersion(TOOL_CANDLE_EXECUTABLE.fetchFrom(p));
        double lightVersion = findToolVersion(TOOL_LIGHT_EXECUTABLE.fetchFrom(p));

        //WiX 3.0+ is required
        double minVersion = 3.0f;
        boolean bad = false;

        if (candleVersion < minVersion) {
            Log.verbose(MessageFormat.format(I18N.getString("message.wrong-tool-version"), TOOL_CANDLE, candleVersion, minVersion));
            bad = true;
        }
        if (lightVersion < minVersion) {
            Log.verbose(MessageFormat.format(I18N.getString("message.wrong-tool-version"), TOOL_LIGHT, lightVersion, minVersion));
            bad = true;
        }

        if (bad){
            throw new ConfigException(
                    I18N.getString("error.no-wix-tools"),
                    I18N.getString("error.no-wix-tools.advice"));
        }

        if (lightVersion >= 3.6f) {
            Log.verbose(I18N.getString("message.use-wix36-features"));
            p.put(CAN_USE_WIX36.getID(), Boolean.TRUE);
        }

        /********* validate bundle parameters *************/

        String version = VERSION.fetchFrom(p);
        if (!isVersionStringValid(version)) {
            throw new ConfigException(
                    MessageFormat.format(I18N.getString("error.version-string-wrong-format"), version),
                    I18N.getString("error.version-string-wrong-format.advice"));
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
            Log.verbose(I18N.getString("message.version-string-too-many-components"));
            return false;
        }

        try {
            int val = Integer.parseInt(p[0]);
            if (val < 0 || val > 255) {
                Log.verbose(I18N.getString("error.version-string-major-out-of-range"));
                return false;
            }
            if (p.length > 1) {
                val = Integer.parseInt(p[1]);
                if (val < 0 || val > 255) {
                    Log.verbose(I18N.getString("error.version-string-minor-out-of-range"));
                    return false;
                }
            }
            if (p.length > 2) {
                val = Integer.parseInt(p[2]);
                if (val < 0 || val > 65535) {
                    Log.verbose(I18N.getString("error.version-string-build-out-of-range"));
                    return false;
                }
            }
        } catch (NumberFormatException ne) {
            Log.verbose(I18N.getString("error.version-string-part-not-number"));
            Log.verbose(ne);
            return false;
        }

        return true;
    }

    private boolean prepareProto(Map<String, ? super Object> p) {
        File bundleRoot = IMAGE_DIR.fetchFrom(p);
        File appDir = APP_BUNDLER.fetchFrom(p).doBundle(p, bundleRoot, true);
        p.put(APP_DIR.getID(), appDir);
        return appDir != null;
    }

    public File bundle(Map<String, ? super Object> p, File outdir) {
        File appDir = APP_DIR.fetchFrom(p);
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

            if (prepareProto(p) && prepareWiXConfig(p)
                    && prepareBasicProjectConfig(p)) {
                File configScriptSrc = getConfig_Script(p);
                if (configScriptSrc.exists()) {
                    //we need to be running post script in the image folder

                    // NOTE: Would it be better to generate it to the image folder
                    // and save only if "verbose" is requested?

                    // for now we replicate it
                    File configScript = new File(imageDir, configScriptSrc.getName());
                    IOUtils.copyFile(configScriptSrc, configScript);
                    Log.info(MessageFormat.format(I18N.getString("message.running-wsh-script"), configScript.getAbsolutePath()));
                    IOUtils.run("wscript", configScript, verbose);
                }
                return buildMSI(p, outdir);
            }
            return null;
        } catch (IOException ex) {
            Log.verbose(ex);
            return null;
        } finally {
            try {
                if (imageDir != null && !Log.isDebug()) {
                    IOUtils.deleteRecursive(imageDir);
                } else if (imageDir != null) {
                    Log.info(MessageFormat.format(I18N.getString("message.debug-working-directory"), imageDir.getAbsolutePath()));
                }
                if (verbose) {
                    Log.info(MessageFormat.format(I18N.getString("message.config-save-location"), CONFIG_ROOT.fetchFrom(p).getAbsolutePath()));
                } else {
                    cleanupConfigFiles(p);
                }
            } catch (FileNotFoundException ex) {
                //noinspection ReturnInsideFinallyBlock
                return null;
            }
        }
    }

    protected void cleanupConfigFiles(Map<String, ? super Object> params) {
        if (getConfig_ProjectFile(params) != null) {
            getConfig_ProjectFile(params).delete();
        }
        if (getConfig_Script(params) != null) {
            getConfig_Script(params).delete();
        }
    }

    //name of post-image script
    private File getConfig_Script(Map<String, ? super Object> params) {
        return new File(CONFIG_ROOT.fetchFrom(params),
                WinAppBundler.getAppName(params) + "-post-image.wsf");
    }

    @Override
    public String toString() {
        return getName();
    }

    private boolean prepareBasicProjectConfig(Map<String, ? super Object> params) throws IOException {
        fetchResource(WinAppBundler.WIN_BUNDLER_PREFIX + getConfig_Script(params).getName(),
                I18N.getString("resource.post-install-script"),
                (String) null,
                getConfig_Script(params));
        return true;
    }

    private String relativePath(File basedir, File file) {
        return file.getAbsolutePath().substring(
                basedir.getAbsolutePath().length() + 1);
    }

    boolean prepareMainProjectFile(Map<String, ? super Object> params) throws IOException {
        Map<String, String> data = new HashMap<>();

        UUID productGUID = UUID.randomUUID();

        Log.verbose(MessageFormat.format(I18N.getString("message.generated-product-guid"), productGUID.toString()));

        //we use random GUID for product itself but
        // user provided for upgrade guid
        // Upgrade guid is important to decide whether it is upgrade of installed
        //  app. I.e. we need it to be the same for 2 different versions of app if possible
        data.put("PRODUCT_GUID", productGUID.toString());
        data.put("PRODUCT_UPGRADE_GUID", UPGRADE_UUID.fetchFrom(params).toString());

        data.put("APPLICATION_NAME", WinAppBundler.getAppName(params));
        data.put("APPLICATION_DESCRIPTION", DESCRIPTION.fetchFrom(params));
        data.put("APPLICATION_VENDOR", VENDOR.fetchFrom(params));
        data.put("APPLICATION_VERSION", VERSION.fetchFrom(params));

        //WinAppBundler will add application folder again => step out
        File imageRootDir = APP_DIR.fetchFrom(params);
        File launcher = WinAppBundler.getLauncher(
                imageRootDir.getParentFile(), params);

        String launcherPath = relativePath(imageRootDir, launcher);
        data.put("APPLICATION_LAUNCHER", launcherPath);

        String iconPath = launcherPath.replace(".exe", ".ico");

        data.put("APPLICATION_ICON", iconPath);

        data.put("REGISTRY_ROOT", getRegistryRoot(params));

        boolean canUseWix36Features = CAN_USE_WIX36.fetchFrom(params);
        data.put("WIX36_ONLY_START",
                canUseWix36Features ? "" : "<!--");
        data.put("WIX36_ONLY_END",
                canUseWix36Features ? "" : "-->");

        if (MSI_SYSTEM_WIDE.fetchFrom(params)) {
            data.put("INSTALL_SCOPE", "perMachine");
        } else {
            data.put("INSTALL_SCOPE", "perUser");
        }

        if (BIT_ARCH_64.fetchFrom(params)) {
            data.put("PLATFORM", "x64");
            data.put("WIN64", "yes");
        } else {
            data.put("PLATFORM", "x86");
            data.put("WIN64", "no");
        }

        Writer w = new BufferedWriter(new FileWriter(getConfig_ProjectFile(params)));
        w.write(preprocessTextResource(
                WinAppBundler.WIN_BUNDLER_PREFIX + getConfig_ProjectFile(params).getName(),
                I18N.getString("resource.wix-config-file"), 
                MSI_PROJECT_TEMPLATE, data));
        w.close();
        return true;
    }
    private int id;
    private int compId;
    private final static String LAUNCHER_ID = "LauncherId";

    private void walkFileTree(Map<String, ? super Object> params, File root, PrintStream out, String prefix) {
        List<File> dirs = new ArrayList<>();
        List<File> files = new ArrayList<>();

        if (!root.isDirectory()) {
            throw new RuntimeException(
                    MessageFormat.format(I18N.getString("error.cannot-walk-directory"), root.getAbsolutePath()));
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
                + " Guid=\"" + UUID.randomUUID().toString() + "\""
                + (BIT_ARCH_64.fetchFrom(params) ? " Win64=\"yes\"" : "") + ">");
        out.println("  <CreateFolder/>");
        out.println("  <RemoveFolder Id=\"RemoveDir" + (id++) + "\" On=\"uninstall\" />");

        boolean needRegistryKey = !MSI_SYSTEM_WIDE.fetchFrom(params);
        File imageRootDir = APP_DIR.fetchFrom(params);
        File launcherFile = WinAppBundler.getLauncher(
                /* Step up as WinAppBundler will add app folder */
                imageRootDir.getParentFile(), params);
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
                    + " Key=\"Software\\" + VENDOR.fetchFrom(params) + "\\"
                    + WinAppBundler.getAppName(params) + "\""
                    + (CAN_USE_WIX36.fetchFrom(params)
                    ? ">" : " Action=\"createAndRemoveOnUninstall\">"));
            out.println(prefix + "     <RegistryValue Name=\"Version\" Value=\""
                    + VERSION.fetchFrom(params) + "\" Type=\"string\" KeyPath=\"yes\"/>");
            out.println(prefix + "   </RegistryKey>");
        }

        boolean menuShortcut = MENU_HINT.fetchFrom(params);
        boolean desktopShortcut = SHORTCUT_HINT.fetchFrom(params);
        for (File f : files) {
            boolean isLauncher = f.equals(launcherFile);
            boolean doShortcuts = isLauncher && (menuShortcut || desktopShortcut);
            out.println(prefix + "   <File Id=\"" +
                    (isLauncher ? LAUNCHER_ID : ("FileId" + (id++))) + "\""
                    + " Name=\"" + f.getName() + "\" "
                    + " Source=\"" + relativePath(imageRootDir, f) + "\""
                    + (BIT_ARCH_64.fetchFrom(params) ? " ProcessorArchitecture=\"x64\"" : "") + ">");
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
            walkFileTree(params, d, out, prefix + " ");
            out.println(prefix + " </Directory>");
        }
    }

    String getRegistryRoot(Map<String, ? super Object> params) {
        if (MSI_SYSTEM_WIDE.fetchFrom(params)) {
            return "HKLM";
        } else {
            return "HKCU";
        }
    }

    boolean prepareContentList(Map<String, ? super Object> params) throws FileNotFoundException {
        File f = new File(CONFIG_ROOT.fetchFrom(params), MSI_PROJECT_CONTENT_FILE);
        PrintStream out = new PrintStream(f);

        //opening
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        out.println("<Include>");

        out.println(" <Directory Id=\"TARGETDIR\" Name=\"SourceDir\">");
        if (MSI_SYSTEM_WIDE.fetchFrom(params)) {
            //install to programfiles
            if (BIT_ARCH_64.fetchFrom(params)) {
                out.println("  <Directory Id=\"ProgramFiles64Folder\" Name=\"PFiles\">");
            } else {
                out.println("  <Directory Id=\"ProgramFilesFolder\" Name=\"PFiles\">");
            }
        } else {
            //install to user folder
            out.println("  <Directory Name=\"AppData\" Id=\"LocalAppDataFolder\">");
        }
        out.println("   <Directory Id=\"APPLICATIONFOLDER\" Name=\""
                + WinAppBundler.getAppName(params) + "\">");

        //dynamic part
        id = 0;
        compId = 0; //reset counters
        walkFileTree(params, APP_DIR.fetchFrom(params), out, "    ");

        //closing
        out.println("   </Directory>");
        out.println("  </Directory>");

        //for shortcuts
        if (SHORTCUT_HINT.fetchFrom(params)) {
            out.println("  <Directory Id=\"DesktopFolder\" />");
        }
        if (MENU_HINT.fetchFrom(params)) {
            out.println("  <Directory Id=\"ProgramMenuFolder\">");
            out.println("    <Directory Id=\"ProgramMenuDir\" Name=\"" + MENU_GROUP.fetchFrom(params) + "\">");
            out.println("      <Component Id=\"comp" + (compId++) + "\""
                    + " Guid=\"" + UUID.randomUUID().toString() + "\""
                    + (BIT_ARCH_64.fetchFrom(params) ? " Win64=\"yes\"" : "") + ">");
            out.println("        <RemoveFolder Id=\"ProgramMenuDir\" On=\"uninstall\" />");
            //This has to be under HKCU to make WiX happy.
            //There are numberous discussions on this amoung WiX users
            // (if user A installs and user B uninstalls then key is left behind)
            //and there are suggested workarounds but none of them are appealing.
            //Leave it for now
            out.println("         <RegistryValue Root=\"HKCU\" Key=\"Software\\"
                    + VENDOR.fetchFrom(params) + "\\" + WinAppBundler.getAppName(params)
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

    private File getConfig_ProjectFile(Map<String, ? super Object> params) {
        return new File(CONFIG_ROOT.fetchFrom(params), WinAppBundler.getAppName(params) + ".wxs");
    }

    private boolean prepareWiXConfig(Map<String, ? super Object> params) throws IOException {
        return prepareMainProjectFile(params) && prepareContentList(params);

    }
    private final static String MSI_PROJECT_TEMPLATE = "template.wxs";
    private final static String MSI_PROJECT_CONTENT_FILE = "bundle.wxi";

    private File buildMSI(Map<String, ? super Object> params, File outdir) throws IOException {
        File tmpDir = new File(BUILD_ROOT.fetchFrom(params), "tmp");
        File candleOut = new File(tmpDir, WinAppBundler.getAppName(params)+".wixobj");
        File msiOut = new File(outdir, WinAppBundler.getAppName(params)
                + "-" + VERSION.fetchFrom(params) + ".msi");

        Log.verbose(MessageFormat.format(I18N.getString("message.preparing-msi-config"), msiOut.getAbsolutePath()));

        msiOut.getParentFile().mkdirs();

        //run candle
        ProcessBuilder pb = new ProcessBuilder(
                TOOL_CANDLE_EXECUTABLE.fetchFrom(params),
                "-nologo",
                getConfig_ProjectFile(params).getAbsolutePath(),
                "-ext", "WixUtilExtension",
                "-out", candleOut.getAbsolutePath());
        pb = pb.directory(APP_DIR.fetchFrom(params));
        IOUtils.exec(pb, verbose);

        Log.verbose(MessageFormat.format(I18N.getString("message.generating-msi"), msiOut.getAbsolutePath()));

        //create .msi
        pb = new ProcessBuilder(
                TOOL_LIGHT_EXECUTABLE.fetchFrom(params),
                "-nologo",
                "-spdb",
                "-sice:60", //ignore warnings due to "missing launcguage info" (ICE60)
                candleOut.getAbsolutePath(),
                "-ext", "WixUtilExtension",
                "-out", msiOut.getAbsolutePath());
        pb = pb.directory(APP_DIR.fetchFrom(params));
        IOUtils.exec(pb, verbose);

        candleOut.delete();
        IOUtils.deleteRecursive(tmpDir);

        return msiOut;
    }
}
