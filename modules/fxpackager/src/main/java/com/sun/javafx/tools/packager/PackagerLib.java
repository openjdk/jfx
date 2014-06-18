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

package com.sun.javafx.tools.packager;

import com.oracle.tools.packager.*;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.Log;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.UnsupportedPlatformException;
import com.sun.javafx.tools.ant.Utils;
import com.sun.javafx.tools.packager.DeployParams.Icon;
import com.sun.javafx.tools.packager.JarSignature.InputStreamSource;
import com.sun.javafx.tools.packager.bundlers.*;
import com.sun.javafx.tools.packager.bundlers.Bundler.BundleType;
import com.sun.javafx.tools.resource.DeployResource;
import com.sun.javafx.tools.resource.PackagerResource;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.CodeSigner;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import sun.misc.BASE64Encoder;

public class PackagerLib {
    public static final String JAVAFX_VERSION = "8.0";

    private static final ResourceBundle bundle =
            ResourceBundle.getBundle("com/sun/javafx/tools/packager/Bundle");

    private static final String dtFX = "dtjava.js";

    private static final String webfilesDir = "web-files";
    //Note: leading "." is important for IE8
    private static final String EMBEDDED_DT = "./"+webfilesDir+"/"+dtFX;

    private static final String PUBLIC_DT = "http://java.com/js/dtjava.js";

    private CreateJarParams createJarParams;
    private DeployParams deployParams;
    private CreateBSSParams createBssParams;
    private File bssTmpDir;
    private boolean isSignedJNLP;


    private enum Filter {ALL, CLASSES_ONLY, RESOURCES}

    private ClassLoader classLoader;

    private ClassLoader getClassLoader() throws PackagerException {
        if (classLoader == null) {
            try {
                URL[] urls = {new URL(getJfxrtPath())};
                classLoader = URLClassLoader.newInstance(urls);
            } catch (MalformedURLException ex) {
                throw new PackagerException(ex, "ERR_CantFindRuntime");
            }
        }
        return classLoader;
    }

    //  if set of input resources consist of SINGLE element and
    //   this element is jar file then we expect this to be request to
    //   "update" jar file
    //  Input jar file MUST be executable jar file
    //
    // Check if we are in "special case" scenario
    private File jarFileToUpdate(CreateJarParams params) {
        if (params.resources.size() == 1) {
            PackagerResource p = params.resources.get(0);
            File f = p.getFile();
            if (!f.isFile() || !f.getAbsolutePath().toLowerCase().endsWith(".jar")) {
                return null;
            }
            try (JarFile jf = new JarFile(f)) {
                Manifest m = jf.getManifest(); //try to read manifest to validate it is jar
                return f;
            } catch (Exception e) {
                //treat any exception as "not a special case" scenario
                com.oracle.tools.packager.Log.verbose(e);
            }
        }
        return null;
    }

    public void packageAsJar(CreateJarParams createJarParams) throws PackagerException {
        if (createJarParams == null) {
            throw new IllegalArgumentException("Parameters must not be null");
        }

        if (createJarParams.outfile == null) {
            throw new IllegalArgumentException("Output file is not specified");
        }

        this.createJarParams = createJarParams;

        //Special case: could be request for "update jar file"
        File jarToUpdate = jarFileToUpdate(createJarParams);
        Manifest m = null;

        if (jarToUpdate != null) {
            com.oracle.tools.packager.Log.info(MessageFormat.format(bundle.getString("MSG_UpdatingJar"), jarToUpdate.getAbsolutePath()));
            try (JarFile jf = new JarFile(jarToUpdate)) {
                //extract data we want to preserve
                m = jf.getManifest();
                if (m != null) {
                    Attributes attrs = m.getMainAttributes();
                    if (createJarParams.applicationClass == null) {
                        createJarParams.applicationClass =
                                attrs.getValue(Attributes.Name.MAIN_CLASS);
                    }
                    if (createJarParams.classpath == null) {
                        createJarParams.classpath =
                                attrs.getValue(Attributes.Name.CLASS_PATH);
                    }
                }
            } catch (IOException ex) {
                throw new PackagerException(
                        ex, "ERR_FileReadFailed", jarToUpdate.getAbsolutePath());
            }
        }

        if (createJarParams.applicationClass == null) {
            throw new IllegalArgumentException(
                    "Main application class is not specified");
        }

        //NOTE: This should be a save-to-temp file, then rename operation
        File applicationJar = new File(createJarParams.outdir,
                        createJarParams.outfile.endsWith(".jar")
                              ? createJarParams.outfile
                              : createJarParams.outfile + ".jar");

        if (jarToUpdate != null &&
                applicationJar.getAbsoluteFile().equals(jarToUpdate.getAbsoluteFile())) {
            try {
                File newInputJar = File.createTempFile("tempcopy", ".jar");
                Files.move(jarToUpdate.toPath(), newInputJar.toPath(),
                           StandardCopyOption.REPLACE_EXISTING);
                jarToUpdate = newInputJar;
            } catch (IOException ioe) {
                throw new PackagerException(
                        ioe, "ERR_FileCopyFailed", jarToUpdate.getAbsolutePath());
            }
        }

        File parentFile = applicationJar.getParentFile();
        if (parentFile != null) {
            parentFile.mkdirs();
        }

        if (m == null) {
            m = new Manifest();
        }
        Attributes attr = m.getMainAttributes();
        attr.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attr.put(new Attributes.Name("Created-By"), "JavaFX Packager");

        if (createJarParams.manifestAttrs != null) {
            for (Entry<String, String> e: createJarParams.manifestAttrs.entrySet()) {
                attr.put(new Attributes.Name(e.getKey()), e.getValue());
            }
        }

        attr.put(Attributes.Name.MAIN_CLASS, createJarParams.applicationClass);
        if (createJarParams.classpath != null) {
            // Allow comma or semicolon as delimeter (turn them into spaces)
            String cp = createJarParams.classpath;
            cp = cp.replace(';', ' ').replace(',', ' ');
            attr.put(new Attributes.Name("Class-Path"), cp);
        }

        attr.put(new Attributes.Name("JavaFX-Version"), createJarParams.fxVersion);

        if (createJarParams.preloader != null) {
            attr.put(new Attributes.Name("JavaFX-Preloader-Class"), createJarParams.preloader);
        }


        if (createJarParams.arguments != null) {
            int idx = 1;
            for (String arg: createJarParams.arguments) {
                attr.put(new Attributes.Name("JavaFX-Argument-"+idx),
                        encodeAsBase64(arg.getBytes()));
                idx++;
            }
        }
        if (createJarParams.params != null) {
            int idx = 1;
            for (Param p : createJarParams.params) {
                if (p.name != null) { //otherwise it is something weird and we skip it
                    attr.put(new Attributes.Name("JavaFX-Parameter-Name-" + idx),
                            encodeAsBase64(p.name.getBytes()));
                    if (p.value != null) { //legal, means not value specified
                        attr.put(new Attributes.Name("JavaFX-Parameter-Value-" + idx),
                                encodeAsBase64(p.value.getBytes()));
                    }
                    idx++;
                }
            }
        }


        if (createJarParams.css2bin) {
            try {
                bssTmpDir = File.createTempFile("bssfiles", "");
            } catch (IOException ex) {
                throw new PackagerException(ex, "ERR_CreatingTempFileFailed");
            }
            bssTmpDir.delete();
        }

        if (applicationJar.exists() && !applicationJar.delete()) {
            throw new PackagerException(
                    "ERR_CantDeleteFile", createJarParams.outfile);
        }
        try {
            jar(m, createJarParams.resources, jarToUpdate,
                    new JarOutputStream(new FileOutputStream(applicationJar)),
                    Filter.ALL);
        } catch (IOException ex) {
            throw new PackagerException(
                    ex, "ERR_CreatingJarFailed", createJarParams.outfile);
        }

        // cleanup
        deleteDirectory(bssTmpDir);
        this.createJarParams = null;
    }

    private String readTextFile(File in) throws PackagerException {
        StringBuilder sb = new StringBuilder();
        try (InputStreamReader isr = new InputStreamReader(new FileInputStream(in))) {
            char[] buf = new char[16384];
            int len;
            while ((len = isr.read(buf)) > 0) {
                sb.append(buf, sb.length(), len);
            }
        } catch (IOException ex) {
            throw new PackagerException(ex, "ERR_FileReadFailed",
                    in.getAbsolutePath());
        }
        return sb.toString();
    }

    private String processTemplate(String inpText,
            Map<TemplatePlaceholders, String> templateStrings) {
        //Core pattern matches
        //   #DT.SCRIPT#
        //   #DT.EMBED.CODE.ONLOAD#
        //   #DT.EMBED.CODE.ONLOAD(App2)#
        String corePattern = "(#[\\w\\.\\(\\)]+#)";
        //This will match
        //   "/*", "//" or "<!--" with arbitrary number of spaces
        String prefixGeneric = "[\\/\\*-<\\!]*[ \\t]*";
        //This will match
        //   "/*", "//" or "<!--" with arbitrary number of spaces
        String suffixGeneric = "[ \\t]*[\\*\\/>-]*";

        //NB: result core match is group number 1
        Pattern mainPattern = Pattern.compile(
                prefixGeneric + corePattern + suffixGeneric);

        Matcher m = mainPattern.matcher(inpText);
        StringBuffer result = new StringBuffer();
        while (m.find()) {
            String match = m.group();
            String coreMatch = m.group(1);
            //have match, not validate it is not false positive ...
            // e.g. if we matched just some spaces in prefix/suffix ...
            boolean inComment =
                    (match.startsWith("<!--") && match.endsWith("-->")) ||
                    (match.startsWith("//")) ||
                    (match.startsWith("/*") && match.endsWith(" */"));

            //try to find if we have match
            String coreReplacement = null;
            //map with rules have no template ids
            //int p = coreMatch.indexOf("\\(");
            //strip leading/trailing #, then split of id part
            String parts[] = coreMatch.substring(1, coreMatch.length()-1).split("[\\(\\)]");
            String rulePart = parts[0];
            String idPart = (parts.length == 1) ?
                    //strip trailing ')'
                    null : parts[1];
            if (templateStrings.containsKey(
                    TemplatePlaceholders.fromString(rulePart))
                    && (idPart == null /* it is ok for templeteId to be not null, e.g. DT.SCRIPT.CODE */
                        || idPart.equals(deployParams.appId))) {
                coreReplacement = templateStrings.get(
                        TemplatePlaceholders.fromString(rulePart));
            }

            if (coreReplacement != null) {
                if (inComment || coreMatch.length() == match.length()) {
                    m.appendReplacement(result, coreReplacement);
                } else { // pattern matched something that is not comment
                         // Very unlikely but lets play it safe
                    int pp = match.indexOf(coreMatch);
                    String v = match.substring(0, pp) +
                            coreReplacement +
                            match.substring(pp + coreMatch.length());
                    m.appendReplacement(result, v);
                }
            }
        }
        m.appendTail(result);
        return result.toString();
    }

    private static enum Mode {FX, APPLET, SwingAPP}

    public void generateDeploymentPackages(DeployParams deployParams) throws PackagerException {
        if (deployParams == null) {
            throw new IllegalArgumentException("Parameters must not be null.");
        }
        this.deployParams = deployParams;
        boolean templateOn = !deployParams.templates.isEmpty();
        Map<TemplatePlaceholders, String> templateStrings = null;
        if (templateOn) {
            templateStrings =
               new EnumMap<>(TemplatePlaceholders.class);
        }
        try {
            //In case of FX app we will have one JNLP and one HTML
            //In case of Swing with FX we will have 2 JNLP files and one HTML
            String jnlp_filename_webstart = deployParams.outfile + ".jnlp";
            String jnlp_filename_browser
                    = deployParams.isSwingApp ?
                        (deployParams.outfile + "_browser.jnlp") : jnlp_filename_webstart;
            String html_filename = deployParams.outfile + ".html";

            //create out dir
            File odir = deployParams.outdir;
            odir.mkdirs();

            if (deployParams.includeDT && !extractWebFiles()) {
                throw new PackagerException("ERR_NoEmbeddedDT");
            }

            ByteArrayOutputStream jnlp_bos_webstart = new ByteArrayOutputStream();
            ByteArrayOutputStream jnlp_bos_browser = new ByteArrayOutputStream();

            //for swing case we need to generate 2 JNLP files
            if (deployParams.isSwingApp) {
               PrintStream jnlp_ps = new PrintStream(jnlp_bos_webstart);
               generateJNLP(jnlp_ps, jnlp_filename_webstart, Mode.SwingAPP);
               jnlp_ps.close();
               //save JNLP
               save(jnlp_filename_webstart, jnlp_bos_webstart.toByteArray());

               jnlp_ps = new PrintStream(jnlp_bos_browser);
               generateJNLP(jnlp_ps, jnlp_filename_browser, Mode.APPLET);
               jnlp_ps.close();
               //save JNLP
               save(jnlp_filename_browser, jnlp_bos_browser.toByteArray());

            } else {
                PrintStream jnlp_ps = new PrintStream(jnlp_bos_browser);
                generateJNLP(jnlp_ps, jnlp_filename_browser, Mode.FX);
                jnlp_ps.close();

                //save JNLP
                save(jnlp_filename_browser, jnlp_bos_browser.toByteArray());

                jnlp_bos_webstart = jnlp_bos_browser;
            }

            //we do not need html if this is component and not main app
            if (!deployParams.isExtension) {
                ByteArrayOutputStream html_bos =
                        new ByteArrayOutputStream();
                PrintStream html_ps = new PrintStream(html_bos);
                generateHTML(html_ps,
                        jnlp_bos_browser.toByteArray(), jnlp_filename_browser,
                        jnlp_bos_webstart.toByteArray(), jnlp_filename_webstart,
                        templateStrings, deployParams.isSwingApp);
                html_ps.close();

                //process template file
                if (templateOn) {
                    for (DeployParams.Template t: deployParams.templates) {
                        File out = t.out;
                        if (out == null) {
                            System.out.println(
                                    "Perform inplace substitution for " +
                                    t.in.getAbsolutePath());
                            out = t.in;
                        }
                        save(out, processTemplate(
                                readTextFile(t.in), templateStrings).getBytes());
                    }
                } else {
                    //save HTML
                    save(html_filename, html_bos.toByteArray());
                }
            }

            //copy jar files
            for (DeployResource resource: deployParams.resources) {
                copyFiles(resource, deployParams.outdir);
            }

            BundleParams bp = deployParams.getBundleParams();
            if (bp != null) {
                generateNativeBundles(deployParams.outdir, bp.getBundleParamsAsMap(), deployParams.getBundleType().toString(), deployParams.getTargetFormat());
            }
        } catch (PackagerException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PackagerException(ex, "ERR_DeployFailed", ex.getMessage());
        }

        this.deployParams = null;
    }

    private void generateNativeBundles(File outdir, Map<String, ? super Object> params, String bundleType, String bundleFormat) throws PackagerException {
        outdir = new File(outdir, "bundles");

        if (params.containsKey(BundleParams.PARAM_RUNTIME)) {
            RelativeFileSet runtime = BundleParams.getRuntime(params);
            if (runtime == null) {
                com.oracle.tools.packager.Log.info(bundle.getString("MSG_NoJREPackaged"));
            } else {
                com.oracle.tools.packager.Log.info(MessageFormat.format(bundle.getString("MSG_UserProvidedJRE"), runtime.getBaseDirectory().getAbsolutePath()));
                if (com.oracle.tools.packager.Log.isDebug()) {
                    runtime.dump();
                }
            }
        } else {
            com.oracle.tools.packager.Log.info(bundle.getString("MSG_UseSystemJRE"));
        }

        for (com.oracle.tools.packager.Bundler bundler : Bundlers.createBundlersInstance().getBundlers(bundleType)) {
            // if they specify the bundle format, require we match the ID
            if (bundleFormat != null && !bundleFormat.equals(bundler.getID())) continue;

            Map<String, ? super Object> localParams = new HashMap<>(params);
            try {
                if (bundler.validate(localParams)) {
                    File result = bundler.execute(localParams, outdir);
                    if (result == null) {
                        throw new PackagerException("MSG_BundlerFailed", bundler.getID(), bundler.getName());
                    }
                }
                
            } catch (UnsupportedPlatformException e) {
                com.oracle.tools.packager.Log.debug(MessageFormat.format(bundle.getString("MSG_BundlerPlatformException"), bundler.getName()));
            } catch (ConfigException e) {
                com.oracle.tools.packager.Log.debug(e);
                if (e.getAdvice() != null) {
                    com.oracle.tools.packager.Log.info(MessageFormat.format(bundle.getString("MSG_BundlerConfigException"), bundler.getName(), e.getMessage(), e.getAdvice()));
                } else {
                    com.oracle.tools.packager.Log.info(MessageFormat.format(bundle.getString("MSG_BundlerConfigExceptionNoAdvice"), bundler.getName(), e.getMessage()));
                }
            } catch (RuntimeException re) {
                com.oracle.tools.packager.Log.info(MessageFormat.format(bundle.getString("MSG_BundlerRuntimeException"), bundler.getName(), re.toString()));
                com.oracle.tools.packager.Log.debug(re);
            }
        }
    }

    private static void copyFiles(DeployResource resource, File outdir) throws IOException, PackagerException {

        if (resource.getFile().isDirectory()) {
            final File baseDir = resource.getBaseDir();
            File[] children = resource.getFile().listFiles();
            if (children != null) {
                for (File file : children) {
                    copyFiles(new DeployResource(baseDir, file), outdir);
                }
            }
        } else {
            final File srcFile = resource.getFile();
            if (srcFile.exists() && srcFile.isFile()) {
                //skip file copying if jar is in the same location
                final File destFile =
                        new File(outdir, resource.getRelativePath());

                if (!srcFile.getCanonicalFile().equals(
                        destFile.getCanonicalFile())) {
                    copyFileToOutDir(new FileInputStream(srcFile),
                                     destFile);
                } else {
                    com.oracle.tools.packager.Log.verbose(MessageFormat.format(bundle.getString("MSG_JarNoSelfCopy"), resource.getRelativePath()));
                }
            }
        }
    }

    public void generateBSS(CreateBSSParams params) throws PackagerException {
        if (params == null) {
            throw new IllegalArgumentException("Parameters must not be null.");
        }
        this.createBssParams = params;
        createBinaryCss(createBssParams.resources, createBssParams.outdir);
        this.createBssParams = null;
    }

    public void signJar(SignJarParams params) throws PackagerException {
        try {
            JarSignature signature = retrieveSignature(params);

            for (PackagerResource pr: params.resources) {
                signFile(pr, signature, params.outdir, params.verbose);
            }

        } catch (Exception ex) {
            com.oracle.tools.packager.Log.verbose(ex);
            throw new PackagerException("ERR_SignFailed", ex);
        }

    }


    private JarSignature retrieveSignature(SignJarParams params) throws KeyStoreException,
            NoSuchAlgorithmException, UnrecoverableKeyException, IOException,
            CertificateException, InvalidKeyException {
        if (params.keyPass == null) {
            params.keyPass = params.storePass;
        }

        if (params.keyStore == null) {
            throw new IOException("No keystore specified");
        }

        if (params.storePass == null) {
            throw new IOException("No store password specified");
        }

        if (params.storeType == null) {
            throw new IOException("No store type is specified");
        }

        KeyStore store = KeyStore.getInstance(params.storeType);
        store.load(new FileInputStream(params.keyStore), params.storePass.toCharArray());

        Certificate[] chain = store.getCertificateChain(params.alias);
        X509Certificate certChain[] = new X509Certificate[chain.length];
        for (int i=0; i<chain.length; i++) {
            certChain[i] = (X509Certificate) chain[i];
        }

        PrivateKey privateKey = (PrivateKey)
                store.getKey(params.alias, params.keyPass.toCharArray());

        return JarSignature.create(privateKey, certChain);
    }

    private void signFile(
            PackagerResource pr, JarSignature signature, File outdir, boolean verbose)
               throws NoSuchAlgorithmException, IOException, SignatureException {
        if (pr.getFile().isDirectory()) {
            File[] children = pr.getFile().listFiles();
            if (children != null) {
                for (File innerFile : children) {
                    signFile(new PackagerResource(
                            pr.getBaseDir(), innerFile), signature, outdir, verbose);
                }
            }
        } else {
            File jar = pr.getFile();
            File parent = jar.getParentFile();
            String name = "bsigned_" + jar.getName();
            File signedJar = new File(parent, name);

            System.out.println("Signing (BLOB) " + jar.getPath());

            signAsBLOB(jar, signedJar, signature);

            File destJar;
            if (outdir != null) {
                destJar = new File(outdir, pr.getRelativePath());
            } else {
                // in-place
                jar.delete();
                destJar = jar;
            }
            destJar.delete();
            destJar.getParentFile().mkdirs();
            signedJar.renameTo(destJar);
            if (verbose) {
               System.out.println("Signed as " + destJar.getPath());
            }
        }
    }

    private void signAsBLOB(final File jar, File signedJar, JarSignature signature)
            throws IOException, NoSuchAlgorithmException, SignatureException
    {
        if (signature == null) {
            throw new IllegalStateException("Should retrieve signature first");
        }

        InputStreamSource in = () -> new FileInputStream(jar);
        if (!signedJar.isFile()) {
            signedJar.createNewFile();
        }
        FileOutputStream fos = new FileOutputStream(signedJar);
        signature.signJarAsBLOB(in, new ZipOutputStream(fos));
    }



    public void makeAll(MakeAllParams makeAllParams) throws PackagerException {
        final String exe =
                System.getProperty("os.name").startsWith("Windows") ? ".exe" : "";
        String jHome = System.getenv("JAVA_HOME");
        if (jHome == null) {
            jHome = System.getProperty("java.home");
        }
        if (jHome == null) {
            throw new PackagerException("ERR_MissingJavaHome");
        }

        final File javac = new File(new File(jHome), "bin/javac" + exe);

        String jfxHome = System.getenv("JAVAFX_HOME");
        if (jfxHome == null) {
            jfxHome = System.getProperty("javafx.home");
        }
        if (jfxHome == null) {
            throw new PackagerException("ERR_MissingJavaFxHome");
        }

        final String srcDirName = "src";
        final String compiledDirName = "compiled";
        final String distDirName = "dist";
        final String outfileName = "dist";
        final String jarName = outfileName + ".jar";

        final File distDir = new File(distDirName);

        final File compiledDir = new File(compiledDirName);
        compiledDir.mkdir();

        try {
            final File tmpFile = File.createTempFile("javac", "sources", new File("."));
            tmpFile.deleteOnExit();
            try (FileWriter sources = new FileWriter(tmpFile)) {
                scanAndCopy(new PackagerResource(new File(srcDirName), "."), sources, compiledDir);
            }
            String classpath = jfxHome + "/../rt/lib/ext/jfxrt.jar";
            if (makeAllParams.classpath != null) {
                classpath += File.pathSeparator + makeAllParams.classpath;
            }
            if (makeAllParams.verbose) {
                System.out.println("Executing javac:");
                System.out.printf("%s %s %s %s %s %s%n",
                        javac.getAbsolutePath(),
                        "-d", compiledDirName,
                        "-cp", classpath,
                        "@" + tmpFile.getAbsolutePath());
            }
            int ret = execute(
                    javac.getAbsolutePath(),
                    "-d", compiledDirName,
                    "-cp", classpath,
                    "@" + tmpFile.getAbsolutePath());
            if (ret != 0) {
                throw new PackagerException("ERR_JavacFailed", Integer.toString(ret));
            }
        } catch (PackagerException e) {
            throw e;
        } catch (Exception e) {
            throw new PackagerException(e, "ERR_MakeAllJavacFailed");
        }

        CreateJarParams cjp = new CreateJarParams();
        cjp.applicationClass = makeAllParams.appClass;
        cjp.preloader = makeAllParams.preloader;
        cjp.classpath = makeAllParams.classpath;
        cjp.css2bin = false;
        cjp.outdir = distDir;
        cjp.outfile = jarName;
        cjp.addResource(compiledDir, ".");

        packageAsJar(cjp);

        DeployParams dp = new DeployParams();
        dp.applicationClass = makeAllParams.appClass;
        dp.appName = makeAllParams.appName;
        dp.description = "Application description";
        dp.height = makeAllParams.height;
        dp.width = makeAllParams.width;
        dp.vendor = "Application vendor";
        dp.outdir = distDir;
        dp.outfile = outfileName;
        dp.addResource(distDir, jarName);
        //noinspection deprecation
        dp.setBundleType(BundleType.ALL);

        generateDeploymentPackages(dp);

        deleteDirectory(compiledDir);
    }

    @SuppressWarnings("unchecked")
    private static int execute(Object ... args) throws IOException, InterruptedException {
        final ArrayList<String> argsList = new ArrayList<>();
        for (Object a : args) {
            if (a instanceof List) {
                argsList.addAll((List)a);
            } else if (a instanceof String) {
                argsList.add((String)a);
            }
        }
        final Process p = Runtime.getRuntime().exec(argsList.toArray(new String[argsList.size()]));
        final BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        Thread t = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException ioe) {
                com.oracle.tools.packager.Log.verbose(ioe);
            }
        });
        t.setDaemon(true);
        t.start();
        final BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        t = new Thread(() -> {
            try {
                String line;
                while ((line = err.readLine()) != null) {
                    System.err.println(line);
                }
            } catch (IOException ioe) {
                Log.verbose(ioe);
            }
        });
        t.setDaemon(true);
        t.start();
        return p.waitFor();
    }

    private static void scanAndCopy(PackagerResource dir, Writer out, File outdir) throws PackagerException {
        if (!dir.getFile().exists()) {
            throw new PackagerException("ERR_MissingDirectory", dir.getFile().getName());
        }
        File[] dirFilesList = dir.getFile().listFiles();
        if ((dirFilesList == null) || (dirFilesList.length == 0)) {
            throw new PackagerException("ERR_EmptySourceDirectory", dir.getFile().getName());
        }
        try {
            for (File f : dirFilesList) {
                if (f.isDirectory()) {
                    scanAndCopy(new PackagerResource(dir.getBaseDir(), f), out, outdir);
                } else if (f.getName().endsWith(".java")) {
                    out.write('\'' + f.getAbsolutePath().replace('\\', '/') + "\'\n");
                } else {
                    copyFileToOutDir(new FileInputStream(f),
                            new File(outdir.getPath() + File.separator
                            + dir.getRelativePath() + File.separator
                            + f.getName()));
                }
            }
        } catch (IOException ex) {
            throw new PackagerException("ERR_FileCopyFailed", dir.getFile().getName());
        }
    }

    //return null if args are default
    private String getJvmArguments(boolean includeProperties) {
        StringBuilder sb = new StringBuilder();
        for(String v: deployParams.jvmargs) {
            sb.append(v);  //may need to escape if parameter has spaces
            sb.append(" ");
        }
        if (includeProperties) {
            for(String k: deployParams.properties.keySet()) {
                sb.append("-D");
                sb.append(k);
                sb.append("=");
                sb.append(deployParams.properties.get(k)); //may need to escape if value has spaces
                sb.append(" ");
            }
        }
        if (sb.length() > 0) {
            return sb.toString();
        }
        return null;
    }

    private void generateJNLP(PrintStream out, String jnlp_filename, Mode m)
            throws IOException, CertificateEncodingException
    {
        out.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        //have to use "old" spec version or old javaws will fail
        // with "unknown" version exception ...
        out.println("<jnlp spec=\"1.0\" xmlns:jfx=\"http://javafx.com\"" +
                (deployParams.codebase != null ?
                     " codebase=\"" + deployParams.codebase + "\"" : "") +
                " href=\""+jnlp_filename+"\">");
        out.println("  <information>");
        out.println("    <title>" +
                ((deployParams.title != null)
                ? deployParams.title : "Sample JavaFX Application") +
                "</title>");
        out.println("    <vendor>" +
                ((deployParams.vendor != null)
                ? deployParams.vendor : "Unknown vendor") +
                "</vendor>");
        out.println("    <description>" +
                ((deployParams.description != null)
                ? deployParams.description : "Sample JavaFX 2.0 application.") +
                "</description>");
        for (Icon i : deployParams.icons) {
            if (i.mode == DeployParams.RunMode.WEBSTART ||
                    i.mode == DeployParams.RunMode.ALL) {
                out.println("    <icon href=\"" + i.href + "\" " +
                        ((i.kind != null) ? " kind=\"" + i.kind + "\"" : "") +
                        ((i.width != Icon.UNDEFINED) ?
                                " width=\"" + i.width + "\"" : "") +
                        ((i.height != Icon.UNDEFINED) ?
                                " height=\"" + i.height + "\"" : "") +
                        ((i.depth != Icon.UNDEFINED) ?
                                " depth=\"" + i.depth + "\"" : "") +
                        "/>");
            }
        }

        if (deployParams.offlineAllowed && !deployParams.isExtension) {
            out.println("    <offline-allowed/>");
        }
        out.println("  </information>");

        if (!deployParams.isExtension) {
            //FX is platfrom specific (soon will be available for Mac and Linux too)
            out.println("  <resources>");
            out.println("    <jfx:javafx-runtime version=\"" +
                    deployParams.fxPlatform +
                    "\" href=\"http://javadl.sun.com/webapps/download/GetFile/javafx-latest/windows-i586/javafx2.jnlp\"/>");
            out.println("  </resources>");
        }

        boolean needToCloseResourceTag = false;
        //jre is available for all platforms
        if (!deployParams.isExtension) {
            out.println("  <resources>");
            needToCloseResourceTag = true;

            String vmargs = getJvmArguments(false);
            vmargs = (vmargs == null) ? "" : " java-vm-args=\""+vmargs+"\" ";
            out.println("    <j2se version=\"" + deployParams.jrePlatform + "\"" +
                    vmargs + " href=\"http://java.sun.com/products/autodl/j2se\"/>");
            for (String k : deployParams.properties.keySet()) {
                out.println("    <property name=\"" + k +
                        "\" value=\"" + deployParams.properties.get(k) + "\"/>");
            }
        }
        String currentOS = null, currentArch = null;
        //NOTE: This should sort the list by os+arch; it will reduce the number of resource tags
        String pendingPrint = null;
        for (DeployResource resource: deployParams.resources) {
            //if not same OS or arch then open new resources element
            if (!needToCloseResourceTag ||
                ((currentOS == null && resource.getOs() != null) ||
                 currentOS != null && !currentOS.equals(resource.getOs())) ||
                ((currentArch == null && resource.getArch() != null) ||
                 currentArch != null && !currentArch.equals(resource.getArch()))) {

                //we do not print right a way as it may be empty block
                // Not all resources make sense for JNLP (e.g. data or license)
                if (needToCloseResourceTag) {
                   pendingPrint = "  </resources>\n";
                } else {
                    pendingPrint = "";
                }
                currentOS = resource.getOs();
                currentArch = resource.getArch();
                pendingPrint += "  <resources" +
                        ((currentOS != null) ? " os=\"" + currentOS + "\"" : "") +
                        ((currentArch != null) ? " arch=\""+currentArch+"\"" : "") +
                        ">\n";
            }
            final File srcFile = resource.getFile();
            if (srcFile.exists() && srcFile.isFile()) {
                final String relativePath = resource.getRelativePath();
                DeployResource.Type type = resource.getType();
                switch (type) {
                    case jar:
                        if (pendingPrint != null) {
                            out.print(pendingPrint);
                            pendingPrint = null;
                            needToCloseResourceTag = true;
                        }
                        out.print("    <jar href=\"" + relativePath + "\" size=\""
                                + srcFile.length() + "\"");
                        out.print(" download=\"" + resource.getMode() + "\" ");
                        out.println("/>");
                        break;
                    case jnlp:
                        if (pendingPrint != null) {
                            out.print(pendingPrint);
                            pendingPrint = null;
                            needToCloseResourceTag = true;
                        }
                        out.println("    <extension href=\"" + relativePath + "\"/>");
                        break;
                    case nativelib:
                        if (pendingPrint != null) {
                            out.print(pendingPrint);
                            needToCloseResourceTag = true;
                            pendingPrint = null;
                        }
                        out.println("    <nativelib href=\"" + relativePath + "\"/>");
                        break;
                }
            }
        }
        if (needToCloseResourceTag) {
            out.println("  </resources>");
        }

        if (deployParams.allPermissions) {
            out.println("<security>");
            out.println("  <all-permissions/>");
            processEmbeddedCertificates(out);
            out.println("</security>");
        }

        if (Boolean.TRUE.equals(deployParams.needShortcut)) {
            out.println("  <shortcut><desktop/></shortcut>");

//            //TODO: Add support for a more sophisticated shortcut tag.
//  <shortcut/> // install no shortcuts, and do not consider "installed"
//  <shortcut installed="true"/> // install no shortcuts, but consider "installed"
//  <shortcut installed="false"><desktop/></shortcut> // install desktop shortcut, but do not consider the app "installed"
//  <shortcut installed="true"><menu/></shortcut> // install menu shortcut, and consider app "installed"
        }

        if (!deployParams.isExtension) {
            if (m == Mode.APPLET) {
                out.print("  <applet-desc  width=\"" + deployParams.width
                        + "\" height=\"" + deployParams.height + "\"");

                out.print(" main-class=\"" + deployParams.applicationClass + "\" ");
                out.println(" name=\"" + deployParams.appName + "\" >");
                if (deployParams.params != null) {
                    for (Param p : deployParams.params) {
                        out.println("    <param name=\"" + p.name + "\""
                                + (p.value != null
                                ? (" value=\"" + p.value + "\"") : "")
                                + "/>");
                    }
                }
                out.println("  </applet-desc>");
            } else if (m == Mode.SwingAPP) {
                out.print("  <application-desc main-class=\"" + deployParams.applicationClass + "\" ");
                out.println(" name=\"" + deployParams.appName + "\" >");
                if (deployParams.arguments != null) {
                    for (String a : deployParams.arguments) {
                        out.println("    <argument>" + a + "</argument>");
                    }
                }
                out.println("  </application-desc>");
            } else { //JavaFX application
                //embed fallback application
                if (deployParams.fallbackApp != null) {
                    out.print("  <applet-desc  width=\"" + deployParams.width
                            + "\" height=\"" + deployParams.height + "\"");

                    out.print(" main-class=\"" + deployParams.fallbackApp + "\" ");
                    out.println(" name=\"" + deployParams.appName + "\" >");
                    out.println("    <param name=\"requiredFXVersion\" value=\""
                            + deployParams.fxPlatform + "\"/>");
                    out.println("  </applet-desc>");
                }

                //javafx application descriptor
                out.print("  <jfx:javafx-desc  width=\"" + deployParams.width
                        + "\" height=\"" + deployParams.height + "\"");

                out.print(" main-class=\"" + deployParams.applicationClass + "\" ");
                out.print(" name=\"" + deployParams.appName + "\" ");
                if (deployParams.preloader != null) {
                    out.print(" preloader-class=\"" + deployParams.preloader + "\"");
                }
                if (((deployParams.params == null) || deployParams.params.isEmpty())
                        && (deployParams.arguments == null || deployParams.arguments.isEmpty())) {
                    out.println("/>");
                } else {
                    out.println(">");
                    if (deployParams.params != null) {
                        for (Param p : deployParams.params) {
                            out.println("    <fx:param name=\"" + p.name + "\""
                                    + (p.value != null
                                    ? (" value=\"" + p.value + "\"") : "")
                                    + "/>");
                        }
                    }
                    if (deployParams.arguments != null) {
                        for (String a : deployParams.arguments) {
                            out.println("    <fx:argument>" + a + "</fx:argument>");
                        }
                    }
                    out.println("  </jfx:javafx-desc>");
                }
            }
        } else {
            out.println("<component-desc/>");
        }

        out.println("  <update check=\"" + deployParams.updateMode + "\"/>");
        out.println("</jnlp>");
    }



    private void addToList(List<String> l, String name, String value, boolean isString) {
        String s = isString ? "'" : "";
        String v = name +" : " + s + value + s;
        l.add(v);
    }

    private String listToString(List<String> lst, String offset) {
        StringBuilder b = new StringBuilder();
        if (lst == null || lst.isEmpty()) {
            return offset + "{}";
        }

        b.append(offset).append("{\n");
        boolean first = true;
        for (String s : lst) {
            if (!first) {
                b.append(",\n");
            }
            first = false;
            b.append(offset).append("    ");
            b.append(s);
        }
        b.append("\n");
        b.append(offset).append("}");
        return b.toString();
    }

    private String encodeAsBase64(byte inp[]) {
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(inp);
    }

    private void generateHTML(PrintStream out,
            byte[] jnlp_bytes_browser, String jnlpfile_browser,
            byte[] jnlp_bytes_webstart, String jnlpfile_webstart,
            Map<TemplatePlaceholders, String> templateStrings,
            boolean swingMode) {
        String poff = "    ";
        String poff2 = poff + poff;
        String poff3 = poff2 + poff;

        StringBuilder out_embed_dynamic = new StringBuilder();
        StringBuilder out_embed_onload = new StringBuilder();
        StringBuilder out_launch_code = new StringBuilder();

        String appletParams = getAppletParameters();
        String jnlp_content_browser = null;
        String jnlp_content_webstart = null;

        if (deployParams.embedJNLP) {
            jnlp_content_browser =
                    encodeAsBase64(jnlp_bytes_browser).replaceAll("\\r|\\n", "");
            jnlp_content_webstart =
                    encodeAsBase64(jnlp_bytes_webstart).replaceAll("\\r|\\n", "");
        }

        out.println("<html><head>");
        String dtURL = deployParams.includeDT ? EMBEDDED_DT : PUBLIC_DT;
        String includeDtString = "<SCRIPT src=\"" + dtURL + "\"></SCRIPT>";
        if (templateStrings != null) {
            templateStrings.put(TemplatePlaceholders.SCRIPT_URL, dtURL);
            templateStrings.put(TemplatePlaceholders.SCRIPT_CODE, includeDtString);
        }
        out.println("  " + includeDtString);

        String webstartError = "System is not setup to launch JavaFX applications. " +
                "Make sure that you have a recent Java runtime, then install JavaFX Runtime 2.0 "+
                "and check that JavaFX is enabled in the Java Control Panel.";

        List<String> w_app = new ArrayList<>();
        List<String> w_platform = new ArrayList<>();
        List<String> w_callback = new ArrayList<>();

        addToList(w_app, "url", jnlpfile_webstart, true);
        if (jnlp_content_webstart != null) {
            addToList(w_app, "jnlp_content", jnlp_content_webstart, true);
        }

        addToList(w_platform, "javafx", deployParams.fxPlatform, true);
        String vmargs = getJvmArguments(true);
        if (vmargs != null) {
            addToList(w_platform, "jvmargs", vmargs, true);
        }

        if (!"".equals(appletParams)) {
            addToList(w_app, "params", "{"+appletParams+"}", false);
        }

        if ((deployParams.callbacks != null) && !deployParams.callbacks.isEmpty()) {
            for (JSCallback cb: deployParams.callbacks) {
                addToList(w_callback, cb.getName(), cb.getCmd(), false);
            }
        }

        //prepare content of launchApp function
        out_launch_code.append(poff2).append("dtjava.launch(");
        out_launch_code.append(listToString(w_app, poff3)).append(",\n");
        out_launch_code.append(listToString(w_platform, poff3)).append(",\n");
        out_launch_code.append(listToString(w_callback, poff3)).append("\n");
        out_launch_code.append(poff2).append(");\n");

        out.println("<script>");
        out.println(poff  + "function launchApplication(jnlpfile) {");
        out.print(out_launch_code.toString());
        out.println(poff2 + "return false;");
        out.println(poff + "}");
        out.println("</script>");

        if (templateStrings != null) {
            templateStrings.put(TemplatePlaceholders.LAUNCH_CODE,
                    out_launch_code.toString());
        }

        //applet deployment
        String appId = deployParams.appId; //if null then it will be autogenerated
        String placeholder = deployParams.placeholder;
        if (placeholder == null) { //placeholder can not be null
            placeholder = "'javafx-app-placeholder'";
        }

        //prepare content of embedApp()
        List<String> p_app = new ArrayList<>();
        List<String> p_platform = new ArrayList<>();
        List<String> p_callback = new ArrayList<>();

        if (appId != null) {
            addToList(p_app, "id", appId, true);
        }
        if (deployParams.isSwingApp) {
          addToList(p_app, "toolkit", "swing", true);
        }
        addToList(p_app, "url", jnlpfile_browser, true);
        addToList(p_app, "placeholder", placeholder, false);
        if (deployParams.embeddedWidth != null && deployParams.embeddedHeight != null) {
          addToList(p_app, "width", ""+deployParams.embeddedWidth, true);
          addToList(p_app, "height", ""+deployParams.embeddedHeight, true);
        } else {
          addToList(p_app, "width", ""+deployParams.width, false);
          addToList(p_app, "height", ""+deployParams.height, false);
        }
        if (jnlp_content_browser != null) {
            addToList(p_app, "jnlp_content", jnlp_content_browser, true);
        }

        addToList(p_platform, "javafx", deployParams.fxPlatform, true);
        if (vmargs != null) {
            addToList(p_platform, "jvmargs", vmargs, true);
        }

        if ((deployParams.callbacks != null) && !deployParams.callbacks.isEmpty()) {
            for (JSCallback cb: deployParams.callbacks) {
                addToList(p_callback, cb.getName(), cb.getCmd(), false);
            }
        }

        if (!"".equals(appletParams)) {
            addToList(p_app, "params", "{"+appletParams+"}", false);
        }

        if (swingMode) {
            //Splash will not work in SwingMode
            //Unless user overwrites onGetSplash handler (and that means he handles splash on his own)
            // we will reset splash function to be "none"
            boolean needOnGetSplashImpl = true;
            if (deployParams.callbacks != null) {
                for (JSCallback c: deployParams.callbacks) {
                    if ("onGetSplash".equals(c.getName())) {
                        needOnGetSplashImpl = false;
                    }
                }
            }

            if (needOnGetSplashImpl) {
                addToList(p_callback, "onGetSplash", "function() {}", false);
            }
        }

        out_embed_dynamic.append("dtjava.embed(\n");
        out_embed_dynamic.append(listToString(p_app, poff3)).append(",\n");
        out_embed_dynamic.append(listToString(p_platform, poff3)).append(",\n");
        out_embed_dynamic.append(listToString(p_callback, poff3)).append("\n");

        out_embed_dynamic.append(poff2).append(");\n");

        //now wrap content with function
        String embedFuncName = "javafxEmbed" +
                ((deployParams.appId != null) ?
                   "_"+deployParams.appId : "");
        out_embed_onload.append("\n<script>\n");
        out_embed_onload.append(poff).append("function ").append(embedFuncName).append("() {\n");
        out_embed_onload.append(poff2);
        out_embed_onload.append(out_embed_dynamic);
        out_embed_onload.append(poff).append("}\n");

        out_embed_onload.append(poff).append(
            "<!-- Embed FX application into web page once page is loaded -->\n");
        out_embed_onload.append(poff).append("dtjava.addOnloadCallback(").append(embedFuncName).append(
            ");\n");
        out_embed_onload.append("</script>\n");

        if (templateStrings != null) {
            templateStrings.put(
                    TemplatePlaceholders.EMBED_CODE_ONLOAD,
                    out_embed_onload.toString());
            templateStrings.put(
                    TemplatePlaceholders.EMBED_CODE_DYNAMIC,
                    out_embed_dynamic.toString());
        }

        out.println(out_embed_onload.toString());

        out.println("</head><body>");
        out.println("<h2>Test page for <b>"+deployParams.appName+"</b></h2>");
        String launchString = "return launchApplication('" + jnlpfile_webstart + "');";
        out.println("  <b>Webstart:</b> <a href='" + jnlpfile_webstart +
                "' onclick=\"" + launchString + "\">"
                    + "click to launch this app as webstart</a><br><hr><br>");
        out.println("");
        out.println("  <!-- Applet will be inserted here -->");
        //placeholder is wrapped with single quotes already
        out.println("  <div id="+placeholder+"></div>");
        out.println("</body></html>");
    }

    private void save(String fname, byte[] content) throws IOException {
        File odir = deployParams.outdir;
        save(new File(odir, fname), content);
    }

    private void save(File f, byte[] content) throws IOException {
        if (f.exists()) {
            f.delete();
        }
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(content);
        fos.close();
    }

    private static void copyFileToOutDir(
            InputStream isa, File fout) throws PackagerException {

        final File outDir = fout.getParentFile();
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new PackagerException("ERR_CreatingDirFailed", outDir.getPath());
        }
        try (InputStream is = isa; OutputStream out = new FileOutputStream(fout)) {
            byte[] buf = new byte[16384];
            int len;
            while ((len = is.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException ex) {
            throw new PackagerException(ex, "ERR_FileCopyFailed", outDir.getPath());
        }
    }


    private String getAppletParameters() {
        String result = "";
        if (deployParams.htmlParams != null) {
            for (HtmlParam p: deployParams.htmlParams) {
                if (!result.isEmpty()) {
                    result += ", ";
                }
                String escape = p.needEscape ? "\"" : "";
                result += "\""+p.name+"\": " + escape + p.value + escape;
            }
        }
        return result;
    }

    private void jar(
            Manifest manifest, List<PackagerResource> files,
            File importJarFile, JarOutputStream jar, Filter filter)
                throws IOException, PackagerException {
        try {
            jar.putNextEntry(new ZipEntry("META-INF/"));
            jar.closeEntry();
            jar.putNextEntry(new ZipEntry(JarFile.MANIFEST_NAME));
            manifest.write(jar);
            jar.closeEntry();

            alreadyAddedEntries.add("META-INF/");
            if (importJarFile != null) { //updating jar file
                copyFromOtherJar(jar, importJarFile);
            } else { //normal situation
                for (PackagerResource pr : files) {
                    jar(pr.getFile(), jar, filter,
                            pr.getBaseDir().getAbsolutePath().length() + 1);
                }
            }
        } finally {
            jar.close();
            alreadyAddedEntries.clear();
        }
    }

    private Set<String> alreadyAddedEntries = new HashSet<>();
    private void createParentEntries(String relativePath, JarOutputStream jar) throws IOException {
        String[] pathComponents = relativePath.split("/");
        StringBuilder pathSB = new StringBuilder();
        // iterating over directories only, the last component is the file
        // or will be created next time.
        for (int i = 0; i < pathComponents.length - 1; i++) {
            pathSB.append(pathComponents[i]).append("/");
            if (!alreadyAddedEntries.contains(pathSB.toString())) {
                jar.putNextEntry(new ZipEntry(pathSB.toString()));
                jar.closeEntry();
            }
            alreadyAddedEntries.add(pathSB.toString());
        }
    }

    //add everything but manifest from given jar file
    private void copyFromOtherJar(JarOutputStream jar, File inputFile) throws IOException {
        JarFile inJar = new JarFile(inputFile);

        Enumeration<JarEntry> all = inJar.entries();
        while (all.hasMoreElements()) {
            JarEntry je = all.nextElement();

            //skip manifest or root manifest dir entry (can not add duplicate)
            if ("META-INF/MANIFEST.MF".equals(je.getName().toUpperCase())
                    || "META-INF/".equals(je.getName().toUpperCase())) {
                continue;
            }

            jar.putNextEntry(new JarEntry(je.getName()));

            byte b[] = new byte[65000];
            int i;
            try (InputStream in = inJar.getInputStream(je)) {
                while ((i = in.read(b)) > 0) {
                    jar.write(b, 0, i);
                }
            }

            jar.closeEntry();
        }
    }

    private void jar(File f, JarOutputStream jar, Filter filter, int cut)
            throws IOException, PackagerException {
        if (!f.exists()) {
            throw new FileNotFoundException("Input folder does not exist ["
                    +f.getAbsolutePath()+"]");
        }

        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File innerFile : children) {
                    jar(innerFile, jar, filter, cut);
                }
            }
        } else if (filter == Filter.ALL
                || (filter == Filter.CLASSES_ONLY && f.getName().endsWith(".class"))
                || (filter == Filter.RESOURCES && isResource(f.getAbsolutePath()))) {
            final String absPath = f.getAbsolutePath();
            if (absPath.endsWith("META-INF\\MANIFEST.MF")
             || absPath.endsWith("META-INF/MANIFEST.MF")) {
                return;
            }
            createParentEntries(absPath.substring(cut).replace('\\', '/'), jar);
            if (createJarParams.css2bin && f.getName().endsWith(".css")) {
                // generate bss file into temporary directory
                int startOfExt = absPath.lastIndexOf(".") + 1;
                String bssFileName = absPath
                                      .substring(cut, startOfExt)
                                      .concat("bss");

                File bssFile = new File(bssTmpDir, bssFileName);
                bssFile.getParentFile().mkdirs();

                createBinaryCss(absPath, bssFile.getAbsolutePath());
                jar.putNextEntry(new ZipEntry(bssFileName.replace('\\', '/')));
                f = bssFile;
            } else {
                jar.putNextEntry(new ZipEntry(absPath.substring(cut).replace('\\', '/')));
            }

            byte b[] = new byte[65000];
            int i;

            try (FileInputStream in = new FileInputStream(f)) {
                while ((i = in.read(b)) > 0) {
                    jar.write(b, 0, i);
                }
            }
            jar.closeEntry();
        }
    }


    private void createBinaryCss(List<PackagerResource> cssResources, File outdir)
            throws PackagerException {
        for (PackagerResource cssRes: cssResources) {
            String relPath = cssRes.getRelativePath();
            createBinaryCss(cssRes.getFile(), outdir, relPath);
        }
    }

    private void createBinaryCss(File f, File outdir, String relPath)
            throws PackagerException {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File innerFile : children) {
                    createBinaryCss(innerFile, outdir, relPath + '/' + innerFile.getName());
                }
            }
        } else if (f.getName().endsWith(".css")) {
            String cssFileName = f.getAbsolutePath();
            String bssFileName = new File(outdir.getAbsolutePath(),
                                          replaceExtensionByBSS(relPath))
                                          .getAbsolutePath();
            createBinaryCss(cssFileName, bssFileName);
        }
    }

    // Returns path to jfxrt.jar relatively to jar containing PackagerLib.class
    private String getJfxrtPath() throws PackagerException {
        String theClassFile = "PackagerLib.class";
        Class theClass = PackagerLib.class;
        String classUrl = theClass.getResource(theClassFile).toString();

        if (!classUrl.startsWith("jar:file:") || !classUrl.contains("!")){
            throw new PackagerException("ERR_CantFindRuntime");
        }

        // Strip everything after and including the "!"
        classUrl = classUrl.substring(0, classUrl.lastIndexOf("!"));
        // Strip everything after the last "/" or "\" to get rid of the jar filename
        int lastIndexOfSlash = Math.max(classUrl.lastIndexOf("/"), classUrl.lastIndexOf("\\"));

        return classUrl.substring(0, lastIndexOfSlash)
                    + "/../rt/lib/ext/jfxrt.jar!/";
    }

    private Class loadClassFromRuntime(String className) throws PackagerException {
        try {
            ClassLoader cl = getClassLoader();
            return cl.loadClass(className);
        } catch (ClassNotFoundException ex) {
            throw new PackagerException(ex, "ERR_CantFindRuntime");
        }
    }

    private void createBinaryCss(String cssFile, String binCssFile) throws PackagerException {
        String ofname = (binCssFile != null)
                            ? binCssFile
                            : replaceExtensionByBSS(cssFile);

        // create parent directories
        File of = new File(ofname);
        File parentFile = of.getParentFile();
        if (parentFile != null) {
            parentFile.mkdirs();
        }

        // Using reflection because CSS parser is part of runtime
        // and we want to avoid dependency on jfxrt during build
        Class<?> clazz;
        try {
            clazz = Class.forName("com.sun.javafx.css.parser.Css2Bin");
        } catch (ClassNotFoundException e) {
            // class was not found with default class loader, trying to
            // locate it by loading from jfxrt.jar
            clazz = loadClassFromRuntime("com.sun.javafx.css.parser.Css2Bin");
        }

        try {
            Method m = clazz.getMethod("convertToBinary", new Class[]{String.class, String.class});
            m.invoke(null, cssFile, ofname);
        } catch (Exception ex) {
            Throwable causeEx = ex.getCause();
            String cause = (causeEx != null) ? causeEx.getMessage()
                                             : bundle.getString("ERR_UnknownReason");

            throw new PackagerException(ex, "ERR_BSSConversionFailed", cssFile, cause);
        }
    }

    private static String replaceExtensionByBSS(String cssName) {
        return cssName.substring(0, cssName.lastIndexOf(".") + 1).concat("bss");
    }


    private boolean isResource(String name) {
        if (name.endsWith(".class")) {
            return false;
        }
        if (name.endsWith(".java")) {
            return false;
        }
        if (name.endsWith(".fx")) {
            return false;
        }
        if (name.endsWith(".cvsignore")) {
            return false;
        }
        if (name.endsWith(".hgignore")) {
            return false;
        }
        if (name.endsWith("vssver.scc")) {
            return false;
        }
        if (name.endsWith(".DS_Store")) {
            return false;
        }
        if (name.endsWith("~")) {
            return false;
        }
        name = name.replace('\\', '/');
        if (name.contains("/CVS/")) {
            return false;
        }
        if (name.contains("/.svn/")) {
            return false;
        }
        if (name.contains("/.hg/")) {
            return false;
        }
        if (name.contains("/.#")) {
            return false;
        }
        if (name.contains("/._")) {
            return false;
        }
        if (name.endsWith("#") && name.contains("/#")) {
            return false;
        }
        if (name.endsWith("%") && name.contains("/%")) {
            return false;
        }
        if (name.endsWith("MANIFEST.MF")) {
            return false;
        }
        return true;
    }

    private static String[] webFiles = {
      "javafx-loading-100x100.gif",
      dtFX,
      "javafx-loading-25x25.gif",
      "error.png",
      "upgrade_java.png",
      "javafx-chrome.png",
      "get_java.png",
      "upgrade_javafx.png",
      "get_javafx.png"
    };

    private static String prefixWebFiles = "/resources/web-files/";

    private boolean extractWebFiles() throws PackagerException {
        return doExtractWebFiles(webFiles);
    }

    private boolean doExtractWebFiles(String lst[]) throws PackagerException {
        File f = new File(deployParams.outdir, webfilesDir);
        f.mkdirs();

        for (String s: lst) {
            InputStream is =
                    PackagerLib.class.getResourceAsStream(prefixWebFiles+s);
            if (is == null) {
                System.err.println("Internal error. Missing resources [" +
                        (prefixWebFiles+s) + "]");
                return false;
            } else {
                copyFileToOutDir(is, new File(f, s));
            }
        }
        return true;
    }

    private static boolean deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) {
            return false;
        }

        if (dir.isDirectory()) {
            for (String file : dir.list()) {
                deleteDirectory(new File(dir, file));
            }
        }
        return dir.delete();
    }

    private void processEmbeddedCertificates(PrintStream out)
            throws CertificateEncodingException, IOException {
        if (deployParams.embedCertificates) {
            Set<CertPath> certPaths = collectCertPaths();
            String signed = isSignedJNLP ? " signedjnlp=\"true\">" : ">";
            if (certPaths != null && !certPaths.isEmpty()) {
                out.println("  <jfx:details" + signed);
                for (CertPath cp : certPaths) {
                    String base64 = Utils.getBase64Encoded(cp);
                    out.println("     <jfx:certificate-path>" + base64 +
                            "</jfx:certificate-path>");
                }
                out.println("  </jfx:details>");
            }
        }
    }

    private Set<CertPath> collectCertPaths() throws IOException {
        Set<CertPath> result = new HashSet<>();
        for (DeployResource resource: deployParams.resources) {
            final File srcFile = resource.getFile();
            if (srcFile.exists() && srcFile.isFile() &&
                srcFile.getName().toLowerCase().endsWith("jar")) {
                result.addAll(extractCertPaths(srcFile));
            }
        }
        return result;
    }

    private Set<CertPath> extractCertPaths(File jar) throws IOException {
        Set<CertPath> result = new HashSet<>();
        JarFile jf = new JarFile(jar);

        // need to fully read jar file to build up internal signer info map
        Utils.readAllFully(jf);

        boolean blobSigned = false;
        Enumeration<JarEntry> entries = jf.entries();
        while (entries.hasMoreElements()) {
            JarEntry je = entries.nextElement();
            String entryName = je.getName();

            CodeSigner[] signers;
            if (entryName.equalsIgnoreCase(JarSignature.BLOB_SIGNATURE)) {
                byte[] raw = Utils.getBytes(jf.getInputStream(je));
                try {
                    JarSignature js = JarSignature.load(raw);
                    blobSigned = true;
                    signers = js.getCodeSigners();
                } catch(Exception ex) {
                    throw new IOException(ex);
                }
            } else {
                signers = je.getCodeSigners();
            }
            result.addAll(extractCertPaths(signers));

            if (entryName.equalsIgnoreCase("JNLP-INF/APPLICATION.JNLP")) {
                isSignedJNLP = true;
            }

            // if blob and also know signed JNLP, no need to continue
            if (blobSigned && isSignedJNLP) {
                break;
            }

        }
        return result;
    }

    private static Collection<CertPath> extractCertPaths(CodeSigner[] signers) {
        Collection<CertPath> result = new ArrayList<>();
        if (signers != null) {
            for (CodeSigner cs : signers) {
                CertPath cp = cs.getSignerCertPath();
                if (cp != null) {
                    result.add(cp);
                }
            }
        }
        return result;
    }
}
