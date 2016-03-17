/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package jdk.packager.builders.windows;

import com.oracle.tools.packager.BundlerParamInfo;
import com.oracle.tools.packager.Log;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.IOUtils;
import com.oracle.tools.packager.StandardBundlerParam;
import com.oracle.tools.packager.windows.WinResources;
import com.oracle.tools.packager.windows.WindowsBundlerParam;
import jdk.packager.builders.AbstractAppImageBuilder;

import jdk.tools.jlink.plugin.Pool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static com.oracle.tools.packager.StandardBundlerParam.*;

/**
 *
 */
public class WindowsAppImageBuilder extends AbstractAppImageBuilder {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle(WindowsAppImageBuilder.class.getName());

    protected static final String WINDOWS_BUNDLER_PREFIX =
            BUNDLER_PREFIX + "windows" + File.separator;

    private final static String EXECUTABLE_NAME = "WinLauncher.exe";
    private final static String LIBRARY_NAME = "packager.dll";

    private final static String[] VS_VERS = {"100", "110", "120"};
    private final static String REDIST_MSVCR = "msvcrVS_VER.dll";
    private final static String REDIST_MSVCP = "msvcpVS_VER.dll";

    private final static String TEMPLATE_APP_ICON ="javalogo_white_48.ico";
    private static final String TOOL_ICON_SWAP="IconSwap.exe";
    private static final String TOOL_VERSION_INFO_SWAP="VersionInfoSwap.exe";

    private static final String EXECUTABLE_PROPERTIES_TEMPLATE = "WinLauncher.properties";

    private final Path root;
    private final Path appDir;
    private final Path runtimeRoot;
    private final Path mdir;

    private final Map<String, ? super Object> params;

    public static final BundlerParamInfo<File> CONFIG_ROOT = new WindowsBundlerParam<>(
            I18N.getString("param.config-root.name"),
            I18N.getString("param.config-root.description"),
            "configRoot",
            File.class,
            params -> {
                File imagesRoot = new File(BUILD_ROOT.fetchFrom(params), "windows");
                imagesRoot.mkdirs();
                return imagesRoot;
            },
            (s, p) -> null);

    public static final BundlerParamInfo<Boolean> REBRAND_EXECUTABLE = new WindowsBundlerParam<>(
            I18N.getString("param.rebrand-executable.name"),
            I18N.getString("param.rebrand-executable.description"),
            "win.launcher.rebrand",
            Boolean.class,
            params -> Boolean.TRUE,
            (s, p) -> Boolean.valueOf(s));

    public static final BundlerParamInfo<File> ICON_ICO = new StandardBundlerParam<>(
            I18N.getString("param.icon-ico.name"),
            I18N.getString("param.icon-ico.description"),
            "icon.ico",
            File.class,
            params -> {
                File f = ICON.fetchFrom(params);
                if (f != null && !f.getName().toLowerCase().endsWith(".ico")) {
                    Log.info(MessageFormat.format(I18N.getString("message.icon-not-ico"), f));
                    return null;
                }
                return f;
            },
            (s, p) -> new File(s));



    public WindowsAppImageBuilder(Map<String, Object> config, Path imageOutDir) throws IOException {
        super(config, imageOutDir.resolve(APP_NAME.fetchFrom(config) + "/runtime"));

        Objects.requireNonNull(imageOutDir);

        //@SuppressWarnings("unchecked")
        //String img = (String) config.get("jimage.name"); // FIXME constant

        this.params = config;

        this.root = imageOutDir.resolve(APP_NAME.fetchFrom(params));
        this.appDir = root.resolve("app");
        this.runtimeRoot = root.resolve("runtime");
        this.mdir = runtimeRoot.resolve("lib");
        Files.createDirectories(appDir);
    }

    private Path destFile(String dir, String filename) {
        return runtimeRoot.resolve(dir).resolve(filename);
    }

    private void writeEntry(InputStream in, Path dstFile) throws IOException {
        Files.createDirectories(dstFile.getParent());
        Files.copy(in, dstFile);
    }

    private void writeSymEntry(Path dstFile, Path target) throws IOException {
        Files.createDirectories(dstFile.getParent());
        Files.createLink(dstFile, target);
    }

    /**
     * chmod ugo+x file
     */
    private void setExecutable(Path file) {
        try {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            perms.add(PosixFilePermission.GROUP_EXECUTE);
            perms.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(file, perms);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    private static void createUtf8File(File file, String content) throws IOException {
        try (OutputStream fout = new FileOutputStream(file);
             Writer output = new OutputStreamWriter(fout, "UTF-8")) {
            output.write(content);
        }
    }



    //it is static for the sake of sharing with "installer" bundlers
    // that may skip calls to validate/bundle in this class!
    public static File getRootDir(File outDir, Map<String, ? super Object> p) {
        return new File(outDir, APP_FS_NAME.fetchFrom(p));
    }

    public static String getLauncherName(Map<String, ? super Object> p) {
        return APP_FS_NAME.fetchFrom(p) + ".exe";
    }

    public static String getLauncherCfgName(Map<String, ? super Object> p) {
        return "app/" + APP_FS_NAME.fetchFrom(p) +".cfg";
    }

    private File getConfig_AppIcon(Map<String, ? super Object> params) {
        return new File(getConfigRoot(params), APP_FS_NAME.fetchFrom(params) + ".ico");
    }

    private File getConfig_ExecutableProperties(Map<String, ? super Object> params) {
        return new File(getConfigRoot(params), APP_FS_NAME.fetchFrom(params) + ".properties");
    }

    File getConfigRoot(Map<String, ? super Object> params) {
        return CONFIG_ROOT.fetchFrom(params);
    }

    protected void cleanupConfigFiles(Map<String, ? super Object> params) {
        getConfig_AppIcon(params).delete();
        getConfig_ExecutableProperties(params).delete();
    }

    @Override
    protected InputStream getResourceAsStream(String name) {
        return WinResources.class.getResourceAsStream(name);
    }

    @Override
    protected void prepareApplicationFiles(Pool files, Set<String> modules) throws IOException {
        Map<String, ? super Object> originalParams = new HashMap<>(params);
        File rootFile = root.toFile();
        if (!rootFile.isDirectory() && !rootFile.mkdirs()) {
            throw new RuntimeException(MessageFormat.format(I18N.getString("error.cannot-create-output-dir"), rootFile.getAbsolutePath()));
        }
        if (!rootFile.canWrite()) {
            throw new RuntimeException(MessageFormat.format(I18N.getString("error.cannot-write-to-output-dir"), rootFile.getAbsolutePath()));
        }
        try {
//            if (!dependentTask) {
//                Log.info(MessageFormat.format(I18N.getString("message.creating-app-bundle"), APP_NAME.fetchFrom(p), outputDirectory.getAbsolutePath()));
//            }

            // Create directory structure
//            IOUtils.deleteRecursive(rootDirectory);
//            rootDirectory.mkdirs();


            // create the .exe launchers
            createLauncherForEntryPoint(params);

            // copy the jars
            copyApplication(params);

            // copy in the needed libraries
            Files.copy(WinResources.class.getResourceAsStream(LIBRARY_NAME),
                    root.resolve(LIBRARY_NAME));

            copyMSVCDLLs();

            // create the secondary launchers, if any
            List<Map<String, ? super Object>> entryPoints = StandardBundlerParam.SECONDARY_LAUNCHERS.fetchFrom(params);
            for (Map<String, ? super Object> entryPoint : entryPoints) {
                Map<String, ? super Object> tmp = new HashMap<>(originalParams);
                tmp.putAll(entryPoint);
                createLauncherForEntryPoint(tmp);
            }

        } catch (IOException ex) {
            Log.info("Exception: "+ex);
            Log.debug(ex);
        } finally {

            if (VERBOSE.fetchFrom(params)) {
                Log.info(MessageFormat.format(I18N.getString("message.config-save-location"), getConfigRoot(params).getAbsolutePath()));
            } else {
                cleanupConfigFiles(params);
            }
        }

    }

    private void copyMSVCDLLs() throws IOException {
        String vsVer = null;

        // first copy the ones needed for the launcher
        for (String thisVer : VS_VERS) {
            if (copyMSVCDLLs(thisVer)) {
                vsVer = thisVer;
                break;
            }
        }
        if (vsVer == null) {
            throw new RuntimeException("Not found MSVC dlls");
        }

        AtomicReference<IOException> ioe = new AtomicReference<>();
        final String finalVsVer = vsVer;
        Files.list(runtimeRoot.resolve("bin"))
                .filter(p -> Pattern.matches("msvc(r|p)\\d\\d\\d.dll", p.toFile().getName().toLowerCase()))
                .filter(p -> !p.toString().toLowerCase().endsWith(finalVsVer + ".dll"))
                .forEach(p -> {
                    try {
                        Files.copy(p, root.resolve((p.toFile().getName())));
                    } catch (IOException e) {
                        ioe.set(e);
                    }
                });

        IOException e = ioe.get();
        if (e != null) {
            throw e;
        }
    }

    private boolean copyMSVCDLLs(String VS_VER) throws IOException {
        final InputStream REDIST_MSVCR_URL = WinResources.class.getResourceAsStream(
                REDIST_MSVCR.replaceAll("VS_VER", VS_VER));
        final InputStream REDIST_MSVCP_URL = WinResources.class.getResourceAsStream(
                REDIST_MSVCP.replaceAll("VS_VER", VS_VER));

        if (REDIST_MSVCR_URL != null && REDIST_MSVCP_URL != null) {
            Files.copy(
                    REDIST_MSVCR_URL,
                    root.resolve(REDIST_MSVCR.replaceAll("VS_VER", VS_VER)));
            Files.copy(
                    REDIST_MSVCP_URL,
                    root.resolve(REDIST_MSVCP.replaceAll("VS_VER", VS_VER)));
            return true;
        }

        return false; // not found
    }

    private void validateValueAndPut(Map<String, String> data, String key,
                                     BundlerParamInfo<String> param, Map<String, ? super Object> params)
    {
        String value = param.fetchFrom(params);
        if (value.contains("\r") || value.contains("\n")) {
            Log.info("Configuration Parameter " + param.getID() + " contains multiple lines of text, ignore it");
            data.put(key, "");
            return;
        }
        data.put(key, value);
    }

    protected void prepareExecutableProperties(Map<String, ? super Object> params)
            throws IOException
    {
        Map<String, String> data = new HashMap<>();

        // mapping Java parameters in strings for version resource
        data.put("COMMENTS", "");
        validateValueAndPut(data, "COMPANY_NAME", VENDOR, params);
        validateValueAndPut(data, "FILE_DESCRIPTION", DESCRIPTION, params);
        validateValueAndPut(data, "FILE_VERSION", VERSION, params);
        data.put("INTERNAL_NAME", getLauncherName(params));
        validateValueAndPut(data, "LEGAL_COPYRIGHT", COPYRIGHT, params);
        data.put("LEGAL_TRADEMARK", "");
        data.put("ORIGINAL_FILENAME", getLauncherName(params));
        data.put("PRIVATE_BUILD", "");
        validateValueAndPut(data, "PRODUCT_NAME", APP_NAME, params);
        validateValueAndPut(data, "PRODUCT_VERSION", VERSION, params);
        data.put("SPECIAL_BUILD", "");

        Writer w = new BufferedWriter(new FileWriter(getConfig_ExecutableProperties(params)));
        String content = preprocessTextResource(
                WINDOWS_BUNDLER_PREFIX + getConfig_ExecutableProperties(params).getName(),
                I18N.getString("resource.executable-properties-template"), EXECUTABLE_PROPERTIES_TEMPLATE, data,
                VERBOSE.fetchFrom(params),
                DROP_IN_RESOURCES_ROOT.fetchFrom(params));
        w.write(content);
        w.close();
    }

    private void createLauncherForEntryPoint(Map<String, ? super Object> p) throws IOException {

        File icon = ICON_ICO.fetchFrom(params);
        File iconTarget = getConfig_AppIcon(params);

        InputStream in = locateResource("package/windows/" + APP_NAME.fetchFrom(params) + ".ico",
                "icon",
                TEMPLATE_APP_ICON,
                icon,
                VERBOSE.fetchFrom(params),
                DROP_IN_RESOURCES_ROOT.fetchFrom(params));
        Files.copy(in, iconTarget.toPath());

        writeCfgFile(p, root.resolve(getLauncherCfgName(p)).toFile(), "$APPDIR\\runtime");

        prepareExecutableProperties(p);

        // Copy executable root folder
        Path executableFile = root.resolve(getLauncherName(p));
        writeEntry(WinResources.class.getResourceAsStream(EXECUTABLE_NAME), executableFile);
        executableFile.toFile().setWritable(true, true);

        //Update branding of exe file
        if (REBRAND_EXECUTABLE.fetchFrom(p) && iconTarget.exists()) {
            //extract IconSwap helper tool
            File iconSwapTool = File.createTempFile("iconswap", ".exe");
            IOUtils.copyFromURL(
                    WinResources.class.getResource(TOOL_ICON_SWAP),
                    iconSwapTool,
                    true);
            iconSwapTool.setExecutable(true, false);
            iconSwapTool.deleteOnExit();

            //run it on launcher file
            executableFile.toFile().setWritable(true);
            ProcessBuilder pb = new ProcessBuilder(
                    iconSwapTool.getAbsolutePath(),
                    getConfig_AppIcon(p).getAbsolutePath(),
                    executableFile.toFile().getAbsolutePath());
            IOUtils.exec(pb, VERBOSE.fetchFrom(p));
            executableFile.toFile().setReadOnly();
            iconSwapTool.delete();
        }

        Files.copy(iconTarget.toPath(), root.resolve(APP_NAME.fetchFrom(p) + ".ico"));

        if (REBRAND_EXECUTABLE.fetchFrom(p) && getConfig_ExecutableProperties(p).exists()) {
            // extract VersionInfoHelper tool
            File versionInfoTool = File.createTempFile("versioninfoswap", ".exe");
            IOUtils.copyFromURL(
                    WinResources.class.getResource(TOOL_VERSION_INFO_SWAP),
                    versionInfoTool,
                    true);
            versionInfoTool.setExecutable(true, false);
            versionInfoTool.deleteOnExit();

            // run it on launcher file
            executableFile.toFile().setWritable(true);
            ProcessBuilder pb = new ProcessBuilder(
                    versionInfoTool.getAbsolutePath(),
                    getConfig_ExecutableProperties(p).getAbsolutePath(),
                    executableFile.toFile().getAbsolutePath());
            IOUtils.exec(pb, VERBOSE.fetchFrom(p));
            executableFile.toFile().setReadOnly();
            versionInfoTool.delete();
        }
    }

    private void copyApplication(Map<String, ? super Object> params) throws IOException {
        List<RelativeFileSet> appResourcesList = APP_RESOURCES_LIST.fetchFrom(params);
        if (appResourcesList == null) {
            throw new RuntimeException("Null app resources?");
        }
        for (RelativeFileSet appResources : appResourcesList) {
            if (appResources == null) {
                throw new RuntimeException("Null app resources?");
            }
            File srcdir = appResources.getBaseDirectory();
            for (String fname : appResources.getIncludedFiles()) {
                Files.copy(new File(srcdir, fname).toPath(), new File(appDir.toFile(), fname).toPath());
            }
        }
    }

    @Override
    protected String getCacheLocation(Map<String, ? super Object> params) {
        return "$CACHEDIR/";
    }

}
