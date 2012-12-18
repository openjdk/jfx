/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.tools.ant.Info;
import com.sun.javafx.tools.ant.Application;
import com.sun.javafx.tools.ant.Callback;
import com.sun.javafx.tools.packager.bundlers.Bundler;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;


public class Main {

    private static final ResourceBundle bundle =
            ResourceBundle.getBundle("com/sun/javafx/tools/packager/Bundle");

    private static final String version = bundle.getString("MSG_Version")
            + " " + PackagerLib.JAVAFX_VERSION + "\n";
    private static final String help = bundle.getString("MSG_Help");

    private static String nextArg(String args[], int i) {
        return (i == args.length - 1) ? "" : args[i + 1];
    }

    private static boolean verbose = false;
    private static boolean packageAsJar = false;
    private static boolean genJNLP = false;
    private static boolean css2Bin = false;
    private static boolean signJar = false;
    private static boolean makeAll = false;

    private static void addResources(CommonParams commonParams,
                                     File baseDir, String s) {
        if (s == null || "".equals(s)) {
            return;
        }

        String[] pathArray = s.split(File.pathSeparator);

        for (final String path: pathArray) {
            commonParams.addResource(baseDir, path);
        }
    }

    private static void addArgument(DeployParams deployParams, String argument) {
        if (deployParams.arguments != null) {
            deployParams.arguments.add(argument);
        } else {
            List<String> list = new LinkedList<String>();
            list.add(argument);
            deployParams.setArguments(list);
        }
    }

    private static void addArgument(CreateJarParams deployParams, String argument) {
        if (deployParams.arguments != null) {
            deployParams.arguments.add(argument);
        } else {
            List<String> list = new LinkedList<String>();
            list.add(argument);
            deployParams.setArguments(list);
        }
    }

    private static Map<String, String> createAttrMap(String arg) {
        Map<String, String> map = new HashMap<String, String>();
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

        List<Param> parameters = new ArrayList<Param>(properties.size());

        if (properties != null) {
            for (Map.Entry en : properties.entrySet()) {
                Param p = new Param();
                p.setName((String)en.getKey());
                p.setValue((String)en.getValue());
                parameters.add(p);
            }
        }
        return parameters;
    }

    private static List<HtmlParam> parseHtmlParams(String filename) throws IOException {
        File paramFile = new File(filename);
        Properties properties = new Properties();
        FileInputStream in = new FileInputStream(paramFile);
        properties.load(in);
        in.close();

        List<HtmlParam> parameters = new ArrayList<HtmlParam>(properties.size());

        if (properties != null) {
            for (Map.Entry en : properties.entrySet()) {
                HtmlParam p = new HtmlParam();
                p.setName((String)en.getKey());
                p.setValue((String)en.getValue());
                parameters.add(p);
            }
        }
        return parameters;
    }

    private static List<Callback> parseCallbacks(String param) {
        String[] callbacks = param.split(",");
        List<Callback> list = new ArrayList<Callback>(callbacks.length);

        for (String cb: callbacks) {
            String[] nameCmd = cb.split(":");
            if (nameCmd.length == 2) {
                list.add(new Callback(nameCmd[0], nameCmd[1]));
            }
        }
        return list;
    }


    public static void main(String args[]) throws Exception {
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

            File srcdir = null;
            try {
                if (args[0].equalsIgnoreCase("-createjar")) {
                    boolean srcfilesSet = false;
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
                            createJarParams.setEmbedLauncher(false);
                        } else if (arg.equalsIgnoreCase("-nocss2bin")) {
                            createJarParams.setCss2bin(false);
                        } else if (arg.equalsIgnoreCase("-runtimeVersion")) {
                            createJarParams.setFxVersion(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-verbose") || arg.equalsIgnoreCase("-v")) {
                            createJarParams.setVerbose(true);
                            verbose = true;
                        } else if (arg.equalsIgnoreCase("-outdir")) {
                            createJarParams.setOutdir(new File(nextArg(args, i++)));
                        } else if (arg.equalsIgnoreCase("-outfile")) {
                            createJarParams.setOutfile(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-srcdir")) {
                            srcdir = new File(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-srcfiles")) {
                            addResources(createJarParams, srcdir, nextArg(args, i++));
                            srcfilesSet = true;
                        } else if (arg.equalsIgnoreCase("-argument")) {
                            addArgument(createJarParams, nextArg(args, i++));
                        }  else if (arg.equalsIgnoreCase("-paramFile")) {
                            createJarParams.setParams(parseParams(nextArg(args, i++)));
                        }
                    }
                    if (!srcfilesSet) {
                        //using "." as default dir is confusing. Require explicit list of inputs
                        if (srcdir == null) {
                            throw new PackagerException("ERR_MissingArgument", "-srcfiles (-srcdir)");
                        }
                        addResources(createJarParams, srcdir, ".");
                    }
                    packageAsJar = true;

                } else if (args[0].equalsIgnoreCase("-deploy")) {
                    boolean srcfilesSet = false;
                    File templateInFile = null;
                    File templateOutFile = null;

                    //can only set it to true with command line, reset default
                    deployParams.setEmbedJNLP(false);
                    for (int i = 1; i < args.length; i++) {
                        String arg = args[i];
                        if (arg.equalsIgnoreCase("-title")) {
                            deployParams.setTitle(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-vendor")) {
                            deployParams.setVendor(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-native")) {
                            //if no argument is provided we will treat it as ALL
                            // for compatibility with FX 2.2
                            Bundler.BundleType type = Bundler.BundleType.ALL;
                            String format = null; //null means ANY
                            if (i+1 < args.length && !args[i+1].startsWith("-")) {
                                String v = args[i+1];
                                //parsing logic is the same as in DeployFXTask
                                if ("image".equals(v)) {
                                    type = Bundler.BundleType.IMAGE;
                                } else if ("installer".equals(v)) {
                                    type = Bundler.BundleType.INSTALLER;
                                } else {
                                    //assume it is request to build only specific format
                                    // (like exe or msi)
                                    type = Bundler.BundleType.INSTALLER;
                                    format = (v != null) ? v.toLowerCase() : null;
                                }
                            }
                            deployParams.setBundleType(type);
                            deployParams.setTargetFormat(format);
                        } else if (arg.equalsIgnoreCase("-description")) {
                            deployParams.setDescription(nextArg(args, i++));
                        } else if(arg.equalsIgnoreCase("-appclass")) {
                            deployParams.setApplicationClass(nextArg(args, i++));
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
                            deployParams.setEmbedCertifcates(true);
                        } else if (arg.equalsIgnoreCase("-allpermissions")) {
                            deployParams.setAllPermissions(true);
                        } else if (arg.equalsIgnoreCase("-updatemode")) {
                            deployParams.setUpdateMode(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-isExtension")) {
                            deployParams.setExtension(true);
                        } else if (arg.equalsIgnoreCase("-callbacks")) {
                            deployParams.setCallbacks(parseCallbacks(nextArg(args, i++)));
                        } else if (arg.equalsIgnoreCase("-templateInFilename")) {
                            templateInFile = new File(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-templateOutFilename")) {
                            templateOutFile = new File(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-appId")) {
                            deployParams.setAppId(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-verbose") || arg.equalsIgnoreCase("-v")) {
                            deployParams.setVerbose(true);
                            verbose = true;
                        } else if (arg.equalsIgnoreCase("-includedt")) {
                            deployParams.setIncludeDT(true);
                        } else if (arg.equalsIgnoreCase("-outdir")) {
                            deployParams.setOutdir(new File(nextArg(args, i++)));
                        } else if (arg.equalsIgnoreCase("-outfile")) {
                            deployParams.setOutfile(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-srcdir")) {
                            srcdir = new File(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-srcfiles")) {
                            addResources(deployParams, srcdir, nextArg(args, i++));
                            srcfilesSet = true;
                        } else if (arg.equalsIgnoreCase("-argument")) {
                            addArgument(deployParams, nextArg(args, i++));
                        }
                    }
                    if (templateInFile != null) {
                        deployParams.addTemplate(templateInFile, templateOutFile);
                    }
                    if (!srcfilesSet) {
                        //using "." as default dir is confusing. Require explicit list of inputs
                        if (srcdir == null) {
                            throw new PackagerException("ERR_MissingArgument", "-srcfiles (-srcdir)");
                        }
                        addResources(deployParams, srcdir, ".");
                    }
                    genJNLP = true;
                } else if (args[0].equalsIgnoreCase("-createbss")) {
                    boolean srcfilesSet = false;
                    for (int i = 1; i < args.length; i++) {
                        String arg = args[i];
                        if (arg.equalsIgnoreCase("-verbose") || arg.equalsIgnoreCase("-v")) {
                            createBssParams.setVerbose(true);
                            verbose = true;
                        } else if (arg.equalsIgnoreCase("-outdir")) {
                            createBssParams.setOutdir(new File(nextArg(args, i++)));
                        } else if (arg.equalsIgnoreCase("-srcdir")) {
                            srcdir = new File(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-srcfiles")) {
                            addResources(createBssParams, srcdir, nextArg(args, i++));
                            srcfilesSet = true;
                        }
                    }
                    if (!srcfilesSet) {
                        //using "." as default dir is confusing. Require explicit list of inputs
                        if (srcdir == null) {
                            throw new PackagerException("ERR_MissingArgument", "-srcfiles (-srcdir)");
                        }
                        addResources(createBssParams, srcdir, ".");
                    }
                    css2Bin = true;

                } else if (args[0].equalsIgnoreCase("-signJar")) {
                    boolean srcfilesSet = false;
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
                            srcdir = new File(nextArg(args, i++));
                        } else if (arg.equalsIgnoreCase("-srcfiles")) {
                            addResources(signJarParams, srcdir, nextArg(args, i++));
                            srcfilesSet = true;
                        }
                    }
                    if (!srcfilesSet) {
                        //using "." as default dir is confusing. Require explicit list of inputs
                        if (srcdir == null) {
                            throw new PackagerException("ERR_MissingArgument", "-srcfiles (-srcdir)");
                        }
                        addResources(signJarParams, srcdir, ".");
                    }
                    signJar = true;
                } else if (args[0].equalsIgnoreCase("-makeall")) {
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
                        }
                    }
                    makeAll = true;
                } else {
                    System.err.println(MessageFormat.format(
                                        bundle.getString("ERR_UnknownCommand"),
                                        args[0]));
                    System.exit(-1);
                }

                //set default logger
                if (verbose) {
                    Log.setLogger(new Log.Logger(true));
                } else {
                    Log.setLogger(new Log.Logger(false));
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
                    deployParams.validate();
                    packager.generateDeploymentPackages(deployParams);
                }
                if (signJar) {
                    signJarParams.validate();
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
                    System.exit(-1);
                }
            }
        }
    }
}
