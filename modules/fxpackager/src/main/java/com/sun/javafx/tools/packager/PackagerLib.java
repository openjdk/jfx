/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.tools.packager.Bundlers;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.Log;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.UnsupportedPlatformException;
import com.sun.javafx.tools.packager.JarSignature.InputStreamSource;
import com.sun.javafx.tools.packager.bundlers.BundleParams;
import com.sun.javafx.tools.packager.bundlers.Bundler.BundleType;
import com.sun.javafx.tools.resource.PackagerResource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Base64;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PackagerLib {
    public static final String JAVAFX_VERSION = "8.0";

    private static final ResourceBundle bundle =
            ResourceBundle.getBundle("com/sun/javafx/tools/packager/Bundle");

    private CreateJarParams createJarParams;
    private CreateBSSParams createBssParams;
    private File bssTmpDir;


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
                jf.getManifest(); //try to read manifest to validate it is jar
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
                    if (createJarParams.codebase == null) {
                        createJarParams.codebase = 
                                attrs.getValue(new Attributes.Name("Codebase"));
                    }
                    if (createJarParams.allPermissions == null) {
                        String value  = 
                                attrs.getValue(new Attributes.Name("Permissions"));
                        if (value != null) {
                            createJarParams.allPermissions = Boolean.valueOf(value);
                        }
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
            attr.put(Attributes.Name.CLASS_PATH, cp);
        }

        String existingSetting = attr.getValue("Permissions"); 
        if (existingSetting == null) {
            attr.put(new Attributes.Name("Permissions"),
                    Boolean.TRUE.equals(createJarParams.allPermissions) ? "all-permissions" : "sandbox");
        } else if (createJarParams.allPermissions != null && !Boolean.valueOf(existingSetting).equals(createJarParams.allPermissions)) { 
            throw new PackagerException(
                "ERR_ContradictorySetting", "Permissions"); 
        }

        existingSetting = attr.getValue("Codebase");
        if (existingSetting == null) {
            if (createJarParams.codebase != null) {
                attr.put(new Attributes.Name("Codebase"), createJarParams.codebase);
            }
        } else if (createJarParams.codebase != null && !existingSetting.equals(createJarParams.codebase)) {
            throw new PackagerException(
                    "ERR_ContradictorySetting", "Codebase");
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

    public void generateDeploymentPackages(DeployParams deployParams) throws PackagerException {
        if (deployParams == null) {
            throw new IllegalArgumentException("Parameters must not be null.");
        }

        try {
            BundleParams bp = deployParams.getBundleParams();
            if (bp != null) {
                generateNativeBundles(deployParams.outdir, bp.getBundleParamsAsMap(), "JNLP", "jnlp");
                generateNativeBundles(new File(deployParams.outdir, "bundles"), bp.getBundleParamsAsMap(), deployParams.getBundleType().toString(), deployParams.getTargetFormat());
            }
        } catch (PackagerException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PackagerException(ex, "ERR_DeployFailed", ex.getMessage());
        }

    }

    private void generateNativeBundles(File outdir, Map<String, ? super Object> params, String bundleType, String bundleFormat) throws PackagerException {
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
            if (bundleFormat != null && !bundleFormat.equalsIgnoreCase(bundler.getID())) continue;

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

    private String encodeAsBase64(byte inp[]) {
        return Base64.getEncoder().encodeToString(inp);
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
            Method m = clazz.getMethod("convertToBinary", String.class, String.class);
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

}
