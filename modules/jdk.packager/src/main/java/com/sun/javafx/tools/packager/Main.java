/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tools.packager;

import com.oracle.tools.packager.*;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.UnsupportedPlatformException;
import com.sun.javafx.tools.packager.bundlers.Bundler.BundleType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

import jdk.packager.internal.JLinkBundlerHelper;


public class Main {

    private static final ResourceBundle bundle =
            ResourceBundle.getBundle("com/sun/javafx/tools/packager/Bundle");

    private static final String version = bundle.getString("MSG_Version")
            + " " + PackagerLib.JAVAFX_VERSION + "\n";
    private static final String help = bundle.getString("MSG_Help_1")
                                        + bundle.getString("MSG_Help_2")
                                        + bundle.getString("MSG_Help_3")
                                        + bundle.getString("MSG_Help_4")
                                        + bundle.getString("MSG_Help_5")
                                        + bundle.getString("MSG_Help_6")
                                        + bundle.getString("MSG_Help_7");

    private static String nextArg(String args[], int i) {
        return (i == args.length - 1) ? "" : args[i + 1];
    }

    private static boolean verbose = false;
    private static boolean packageAsJar = false;
    private static boolean genJNLP = false;
    private static boolean genPackages = false;
    private static boolean css2Bin = false;
    private static boolean signJar = false;
    private static boolean makeAll = false;

    private static void addResources(CommonParams commonParams,
                                     String baseDir, String s) {
        if (s == null || "".equals(s)) {
            return;
        }

        String ls = ".";

        if (s != null) {
            ls = s;
        }

        String[] pathArray = ls.split(File.pathSeparator);

        File lbaseDir = null;

        if (baseDir != null) {
            lbaseDir = new File(baseDir);
        }

        for (final String path: pathArray) {
            commonParams.addResource(lbaseDir, path);
        }
    }

    private static void addArgument(DeployParams deployParams, String argument) {
        if (deployParams.arguments != null) {
            deployParams.arguments.add(argument);
        } else {
            List<String> list = new LinkedList<>();
            list.add(argument);
            deployParams.setArguments(list);
        }
    }

    private static void addArgument(CreateJarParams deployParams, String argument) {
        if (deployParams.arguments != null) {
            deployParams.arguments.add(argument);
        } else {
            List<String> list = new LinkedList<>();
            list.add(argument);
            deployParams.setArguments(list);
        }
    }

    private static Map<String, String> createAttrMap(String arg) {
        Map<String, String> map = new HashMap<>();
        if (arg == null || "".equals(arg)) {
            return null;
        }
        String[] pairsArray = arg.split(",");
        for (String pair: pairsArray) {
            String[] attr = pair.split("=");
            map.put(attr[0].trim(), attr[1].trim());
        }
        return map;
    }

    private static List<Param> parseParams(String filename) throws IOException {
        File paramFile = new File(filename);
        Properties properties = new Properties();
        FileInputStream in = new FileInputStream(paramFile);
        properties.load(in);
        in.close();

        List<Param> parameters = new ArrayList<>(properties.size());

        for (Map.Entry en : properties.entrySet()) {
            Param p = new Param();
            p.setName((String)en.getKey());
            p.setValue((String)en.getValue());
            parameters.add(p);
        }
        return parameters;
    }

    private static List<HtmlParam> parseHtmlParams(String filename) throws IOException {
        File paramFile = new File(filename);
        Properties properties = new Properties();
        FileInputStream in = new FileInputStream(paramFile);
        properties.load(in);
        in.close();

        List<HtmlParam> parameters = new ArrayList<>(properties.size());

        for (Map.Entry en : properties.entrySet()) {
            HtmlParam p = new HtmlParam();
            p.setName((String)en.getKey());
            p.setValue((String)en.getValue());
            parameters.add(p);
        }
        return parameters;
    }

    private static List<JSCallback> parseCallbacks(String param) {
        String[] callbacks = param.split(",");
        List<JSCallback> list = new ArrayList<>(callbacks.length);

        for (String cb: callbacks) {
            String[] nameCmd = cb.split(":");
            if (nameCmd.length == 2) {
                list.add(new JSCallback(nameCmd[0], nameCmd[1]));
            }
        }
        return list;
    }


    @SuppressWarnings("deprecation")
    public static void main(String... args) throws Exception {
        if (args.length == 0 || args.length == 1 && args[0].equals("-help")) {
            System.out.println(help);
        } else if (args.length == 1 && args[0].equals("-version")) {
            System.out.println(version);
        } else {
            PackagerLib packager = new PackagerLib();
            CreateJarParams createJarParams = new CreateJarParams();
            DeployParams deployParams = new DeployParams();
            CreateBSSParams createBssParams = new CreateBSSParams();
            SignJarParams signJarParams = new SignJarParams();
            MakeAllParams makeAllParams = new MakeAllParams();
            String srcdir = null;
            String srcfiles = null;

            try {
                if (args[0].equalsIgnoreCase("-createjar")) {
                    for (int i = 1; i < args.length; i++) {
                        String arg = args[i];
                        if (arg.equalsIgnoreCase("-appclass")) {
                            createJarParams.setApplicationClass(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-preloader")) {
                            createJarParams.setPreloader(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-classpath")) {
                            createJarParams.setClasspath(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-manifestAttrs")) {
                            createJarParams.setManifestAttrs(createAttrMap(nextArg(args, i++)));
                        } else if (arg.equalsIgnoreCase("-noembedlauncher")) {
                            System.err.println("-noembedlauncher is deprecated");
                        } else if (arg.equalsIgnoreCase("-nocss2bin")) {
                            createJarParams.setCss2bin(false);
                        } else if (arg.equalsIgnoreCase("-runtimeVersion")) {
                            createJarParams.setFxVersion(nextArg(args, i++));
                            System.err.println("-runtimeVersion is deprecated");
                        } else if (arg.equalsIgnoreCase("-verbose") || arg.equalsIgnoreCase("-v")) {
                            createJarParams.setVerbose(true);
                            verbose = true;
                        } else if (arg.equalsIgnoreCase("-outdir")) {
                            createJarParams.setOutdir(new File(nextArg(args, i++)));
                        } else if (arg.equalsIgnoreCase("-outfile")) {
                            createJarParams.setOutfile(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-" + StandardBundlerParam.SOURCE_DIR.getID())) {
                            srcdir = nextArg(args, i++);
                        } else if (arg.equalsIgnoreCase("-srcfiles")) {
                            srcfiles = nextArg(args, i++);
                        } else if (arg.equalsIgnoreCase("-argument")) {
                            addArgument(createJarParams, nextArg(args, i++));
                        }  else if (arg.equalsIgnoreCase("-paramFile")) {
                            createJarParams.setParams(parseParams(nextArg(args, i++)));
                        } else {
                            throw new PackagerException("ERR_UnknownArgument", arg);
                        }
                    }

                    addResources(createJarParams, srcdir, srcfiles);
                    packageAsJar = true;
                } else if (args[0].equalsIgnoreCase("-deploy")) {
                    File templateInFile = null;
                    File templateOutFile = null;
                    deployParams.setBundleType(BundleType.JNLP);
                    deployParams.setTargetFormat("jnlp");


                    //can only set it to true with command line, reset default
                    deployParams.setEmbedJNLP(false);
                    for (int i = 1; i < args.length; i++) {
                        String arg = args[i];
                        if (arg.startsWith("-B")) {
                            String key;
                            String value;

                            int keyStart = 2;
                            int equals = arg.indexOf("=");
                            int len = arg.length();
                            if (equals < keyStart) {
                                if (keyStart < len) {
                                    key = arg.substring(keyStart, len);
                                    value = Boolean.TRUE.toString();
                                } else {
                                    continue;
                                }
                            } else if (keyStart < equals) {
                                key = arg.substring(keyStart, equals);
                                value = arg.substring(equals+1, len);
                            } else {
                                continue;
                            }
                            deployParams.addBundleArgument(key, value);
                        } else if (arg.equalsIgnoreCase("-title")) {
                            deployParams.setTitle(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-vendor")) {
                            deployParams.setVendor(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-native")) {
                            BundleType type = BundleType.NONE;
                            String format = null; //null means ANY
                            if (i+1 < args.length && !args[i+1].startsWith("-")) {
                                String v = args[++i];
                                com.sun.javafx.tools.packager.bundlers.Bundler.Bundle bundle =
                                        com.sun.javafx.tools.packager.bundlers.Bundler.stringToBundle(v);
                                type = bundle.type;
                                format = bundle.format;
                            }
                            deployParams.setBundleType(type);
                            deployParams.setTargetFormat(format);
                        } else if (arg.equalsIgnoreCase("-description")) {
                            deployParams.setDescription(nextArg(args, i++));
                        } else if(arg.equalsIgnoreCase("-appclass")) {
                            deployParams.setApplicationClass(nextArg(args, i++));
                        } else if(arg.equalsIgnoreCase("-daemon")) {
                            deployParams.setServiceHint(true);
                        } else if(arg.equalsIgnoreCase("-installdirChooser")) {
                            deployParams.setInstalldirChooser(true);
                        } else if (arg.equalsIgnoreCase("-preloader")) {
                            deployParams.setPreloader(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-paramFile")) {
                            deployParams.setParams(parseParams(nextArg(args, i++)));
                        } else if (arg.equalsIgnoreCase("-htmlParamFile")) {
                            deployParams.setHtmlParams(parseHtmlParams(nextArg(args, i++)));
                        } else if (arg.equalsIgnoreCase("-width")) {
                            deployParams.setWidth(Integer.parseInt(nextArg(args, i++)));
                        } else if (arg.equalsIgnoreCase("-height")) {
                            deployParams.setHeight(Integer.parseInt(nextArg(args, i++)));
                        } else if (arg.equalsIgnoreCase("-name")) {
                            deployParams.setAppName(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-embedJNLP")) {
                            deployParams.setEmbedJNLP(true);
                        } else if (arg.equalsIgnoreCase("-embedCertificates")) {
                            System.err.println("-embedCertificates is deprecated");
                        } else if (arg.equalsIgnoreCase("-allpermissions")) {
                            deployParams.setAllPermissions(true);
                        } else if (arg.equalsIgnoreCase("-updatemode")) {
                            deployParams.setUpdateMode(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-isExtension")) {
                            deployParams.setExtension(true);
                        } else if (arg.equalsIgnoreCase("-callbacks")) {
                            deployParams.setJSCallbacks(parseCallbacks(nextArg(args, i++)));
                        } else if (arg.equalsIgnoreCase("-templateInFilename")) {
                            templateInFile = new File(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-templateOutFilename")) {
                            templateOutFile = new File(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-appId") || arg.equalsIgnoreCase("-templateId")) {
                            String appIdArg = nextArg(args, i++);
                            deployParams.setAppId(appIdArg);
                            deployParams.setId(appIdArg);
                        } else if (arg.equalsIgnoreCase("-verbose") || arg.equalsIgnoreCase("-v")) {
                            deployParams.setVerbose(true);
                            verbose = true;
                        } else if (arg.equalsIgnoreCase("-includedt")) {
                            deployParams.setIncludeDT(true);
                        } else if (arg.equalsIgnoreCase("-outdir")) {
                            deployParams.setOutdir(new File(nextArg(args, i++)));
                        } else if (arg.equalsIgnoreCase("-outfile")) {
                            deployParams.setOutfile(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-" + StandardBundlerParam.SOURCE_DIR.getID())) {
                            srcdir = nextArg(args, i++);
                            deployParams.srcdir = srcdir;
                        } else if (arg.equalsIgnoreCase("-srcfiles")) {
                            srcfiles = nextArg(args, i++);
                        } else if (arg.equalsIgnoreCase("-argument")) {
                            addArgument(deployParams, nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-nosign")) {
                            deployParams.setSignBundle(false);
                        } else if (arg.equals(ADD_MODULES)) {
                            deployParams.addModules.add(nextArg(args, i++));
                        } else if (arg.startsWith(ADD_MODULES + "=")) {
                            deployParams.addModules.add(arg.replace(ADD_MODULES + "=", ""));
                        } else if (arg.equals(LIMIT_MODULES)) {
                            deployParams.limitModules.add(nextArg(args, i++));
                        } else if (arg.startsWith(LIMIT_MODULES + "=")) {
                            deployParams.limitModules.add(arg.replace(LIMIT_MODULES + "=", ""));
                        } else if (arg.equals(STRIP_NATIVE_COMMANDS)) {
                            deployParams.setStripNativeCommands(Boolean.valueOf(nextArg(args, i++)));
                        } else if (arg.equals(STRIP_NATIVE_COMMANDS + "=")) {
                            deployParams.setStripNativeCommands(Boolean.valueOf(arg.replace(STRIP_NATIVE_COMMANDS + "=", "")));
                        } else if (arg.equals(DETECT_MODULES)) {
                            deployParams.setDetectModules(true);
                        } else if (arg.equals(MODULE_PATH) || arg.equals(P)) {
                            deployParams.modulePath = nextArg(args, i++);
                        } else if (arg.equals(MODULE_PATH + "=")) {
                            deployParams.modulePath = arg.replace(MODULE_PATH + "=", "");
                        } else if (arg.equals(MODULE) || arg.equals(M)) {
                            deployParams.setModule(nextArg(args, i++));
                        } else if (arg.equals(MODULE + "=")) {
                            deployParams.setModule(arg.replace(MODULE + "=", ""));
                        } else if (arg.startsWith(J_XDEBUG)) {
                            deployParams.setDebug(arg.replace(J_XDEBUG, ""));
                        } else {
                            throw new PackagerException("ERR_UnknownArgument", arg);
                        }
                    }
                    if (templateInFile != null) {
                        deployParams.addTemplate(templateInFile, templateOutFile);
                    }

                    if (deployParams.validateForJNLP()) {
                        genJNLP = true;
                    }

                    if (deployParams.validateForBundle()) {
                        genPackages = true;
                    }

                    addResources(deployParams, srcdir, srcfiles);
                } else if (args[0].equalsIgnoreCase("-createbss")) {
                    for (int i = 1; i < args.length; i++) {
                        String arg = args[i];
                        if (arg.equalsIgnoreCase("-verbose") || arg.equalsIgnoreCase("-v")) {
                            createBssParams.setVerbose(true);
                            verbose = true;
                        } else if (arg.equalsIgnoreCase("-outdir")) {
                            createBssParams.setOutdir(new File(nextArg(args, i++)));
                        } else if (arg.equalsIgnoreCase("-srcdir")) {
                            srcdir = nextArg(args, i++);
                        } else if (arg.equalsIgnoreCase("-srcfiles")) {
                            srcfiles = nextArg(args, i++);
                        } else {
                            throw new PackagerException("ERR_UnknownArgument", arg);
                        }
                    }

                    addResources(createBssParams, srcdir, srcfiles);
                    css2Bin = true;
                } else if (args[0].equalsIgnoreCase("-signJar")) {
                    for (int i = 1; i < args.length; i++) {
                        String arg = args[i];
                        if (arg.equalsIgnoreCase("-keyStore")) {
                            signJarParams.setKeyStore(new File(nextArg(args, i++)));
                        } else if(arg.equalsIgnoreCase("-alias")) {
                            signJarParams.setAlias(nextArg(args, i++));
                        } else if(arg.equalsIgnoreCase("-storePass")) {
                            signJarParams.setStorePass(nextArg(args, i++));
                        } else if(arg.equalsIgnoreCase("-keyPass")) {
                            signJarParams.setKeyPass(nextArg(args, i++));
                        } else if(arg.equalsIgnoreCase("-storeType")) {
                            signJarParams.setStoreType(nextArg(args, i++));
                        } else if(arg.equalsIgnoreCase("-verbose") || arg.equalsIgnoreCase("-v")) {
                            signJarParams.setVerbose(true);
                            verbose = true;
                        } else if (arg.equalsIgnoreCase("-outdir")) {
                            signJarParams.setOutdir(new File(nextArg(args, i++)));
                        } else if (arg.equalsIgnoreCase("-srcdir")) {
                            srcdir = nextArg(args, i++);
                        } else if (arg.equalsIgnoreCase("-srcfiles")) {
                            srcfiles = nextArg(args, i++);
                        } else {
                            throw new PackagerException("ERR_UnknownArgument", arg);
                        }
                    }

                    addResources(signJarParams, srcdir, srcfiles);
                    signJar = true;
                } else if (args[0].equalsIgnoreCase("-makeall")) {
                    System.err.println("-makeall is deprecated");
                    for (int i = 1; i < args.length; i++) {
                        String arg = args[i];
                        if (arg.equalsIgnoreCase("-appclass")) {
                            makeAllParams.setAppClass(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-preloader")) {
                            makeAllParams.setPreloader(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-classpath")) {
                            makeAllParams.setClasspath(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-name")) {
                            makeAllParams.setAppName(nextArg(args, i++));
                        } else if(arg.equalsIgnoreCase("-width")) {
                            makeAllParams.setWidth(Integer.parseInt(nextArg(args, i++)));
                        } else if(arg.equalsIgnoreCase("-height")) {
                            makeAllParams.setHeight(Integer.parseInt(nextArg(args, i++)));
                        } else if(arg.equalsIgnoreCase("-v")) {
                            makeAllParams.setVerbose(true);
                        } else {
                            throw new PackagerException("ERR_UnknownArgument", arg);
                        }
                    }
                    makeAll = true;
                } else if (args[0].equalsIgnoreCase("-help")) {
                    showBundlerHelp(args[1], args.length > 2 && "-verbose".equals(args[2]));
                } else {
                    System.err.println(MessageFormat.format(
                                        bundle.getString("ERR_UnknownCommand"),
                                        args[0]));
                    System.exit(-1);
                }

                //set default logger
                if (verbose) {
                    com.oracle.tools.packager.Log.setLogger(new com.oracle.tools.packager.Log.Logger(true));
                } else {
                    com.oracle.tools.packager.Log.setLogger(new com.oracle.tools.packager.Log.Logger(false));
                }

                if (css2Bin) {
                    createBssParams.validate();
                    packager.generateBSS(createBssParams);
                }
                if (packageAsJar) {
                    createJarParams.validate();
                    packager.packageAsJar(createJarParams);
                }
                if (genJNLP) {
                    deployParams.setBundleType(BundleType.JNLP);
                    deployParams.validate();
                    packager.generateDeploymentPackages(deployParams);
                }
                if (genPackages) {
                    deployParams.setBundleType(BundleType.NATIVE);
                    deployParams.validate();
                    packager.generateDeploymentPackages(deployParams);
                }
                if (signJar) {
                    signJarParams.validate();
                    if (signJarParams.storePass == null) {
                        char[] passwd = System.console().readPassword(bundle.getString("MSG_EnterKeystorePassword"));
                        if (passwd == null) {
                            signJarParams.storePass = "";
                        } else {
                            signJarParams.storePass = new String(passwd);
                        }
                    }
                    if (signJarParams.keyPass == null) {
                        char[] passwd = System.console().readPassword(bundle.getString("MSG_EnterKeyPassword"), signJarParams.alias);
                        if (passwd == null) {
                            signJarParams.keyPass = "";
                        } else {
                            signJarParams.keyPass = new String(passwd);
                        }
                    }
                    packager.signJar(signJarParams);
                }
                if (makeAll) {
                    makeAllParams.validate();
                    packager.makeAll(makeAllParams);
                }

            } catch (Exception e) {
                if (verbose) {
                    throw e;
                } else {
                    System.err.println(e.getMessage());
                    if (e.getCause() != null && e.getCause() != e) {
                        System.err.println(e.getCause().getMessage());
                    }
                    System.exit(-1);
                }
            }
        }
    }

    private static final String MODULE = "--" + StandardBundlerParam.MODULE.getID();
    private static final String M = "-m";
    private static final String MODULE_PATH = "--" + StandardBundlerParam.MODULE_PATH.getID();
    private static final String P = "-p";
    private static final String ADD_MODULES = "--" + StandardBundlerParam.ADD_MODULES.getID();
    private static final String LIMIT_MODULES = "--" + StandardBundlerParam.LIMIT_MODULES.getID();
    private static final String STRIP_NATIVE_COMMANDS = "--" + StandardBundlerParam.STRIP_NATIVE_COMMANDS.getID();

    private static final String J_XDEBUG = JLinkBundlerHelper.DEBUG.getID() + ":";
    private static final String DETECT_MODULES = "--" + JLinkBundlerHelper.DETECT_MODULES.getID();

    public static void showBundlerHelp(String bundlerName, boolean verbose) {
        //TODO I18N
        if ("bundlers".equals(bundlerName)) {
            // enumerate bundlers
            System.out.println("Known Bundlers -- \n");
            for (Bundler bundler : Bundlers.createBundlersInstance().getBundlers()) {
                try {
                    bundler.validate(new HashMap<>());
                } catch (UnsupportedPlatformException upe) {
                    // don't list bundlers this platform cannot run
                    continue;
                } catch (ConfigException ignore) {
                    // but requiring more than an empty map is perfectly fine.
                //} catch (RuntimeException re) {
                //    re.printStackTrace();
                }

                if (verbose) {
                    System.out.printf(
                            "%s - %s - %s\n\t%s\n",
                            bundler.getID(),
                            bundler.getName(),
                            bundler.getBundleType(),
                            bundler.getDescription()
                    );
                } else {
                    System.out.printf(
                            "%s - %s - %s\n",
                            bundler.getID(),
                            bundler.getName(),
                            bundler.getBundleType()
                    );
                }
            }
        } else {
            // enumerate parameters for a bundler
            for (Bundler bundler : Bundlers.createBundlersInstance().getBundlers()) {
                if (bundler.getID().equals(bundlerName)) {
                    System.out.printf("Bundler Parameters for %s (%s) --\n", bundler.getName(), bundler.getID());
                    for (BundlerParamInfo bpi : bundler.getBundleParameters()) {
                        if (bpi.getStringConverter() == null) continue;
                        if (verbose) {
                            System.out.printf(
                                    "%s - %s - %s\n\t%s\n",
                                    bpi.getID(),
                                    bpi.getName(),
                                    bpi.getValueType().getSimpleName(),
                                    bpi.getDescription()
                            );
                        } else {
                            System.out.printf(
                                    "%s - %s - %s\n",
                                    bpi.getID(),
                                    bpi.getName(),
                                    bpi.getValueType().getSimpleName()
                            );
                        }
                    }
                    return;
                }
            }
            System.out.printf("Sorry, no bundler matching the id %s was found.\n", bundlerName);
        }
    }
}
