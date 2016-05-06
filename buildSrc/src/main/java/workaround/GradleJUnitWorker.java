/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved.
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

package workaround;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;

/*
 * This wrapper is designed to workaround a class loader issue
 * when working with JDK9 and Gradle in the JFX builds
 * NOTE: this worker assumes the @argfile support found in JDK9
 *
 * Due to the nature of the command line and where the this class is
 * specified, certain command line properties may either be :
 *  an argument - because it came after this file
 *  a property - because it was before this file and was processed as a property
 * Because of this, everything is checked as both.
 *
 * The worker specific properties are found below as worker.*. These
 * properties also need to be forwarded as system properties just in case
 * we have something like MainLauncherTest that needs some of these to
 * construct a new java command.
 *
 */
public class GradleJUnitWorker {

    public static boolean debug = false;

    private static final String ENCODERCLASS
            = "jarjar.org.gradle.process.internal.child.EncodedStream$EncodedInput";

    private static URL fileToURL(File file) throws IOException {
        return file.getCanonicalFile().toURI().toURL();
    }

    // all of the "standard" System Properties that we will not forward
    // to the worker.
    private final static String[] defSysProps = {
        "awt.toolkit",
        "file.encoding.pkg",
        "file.encoding",
        "file.separator",
        "ftp.nonProxyHosts",
        "gopherProxySet",
        "http.nonProxyHosts",
        "java.awt.graphicsenv",
        "java.awt.printerjob",
        "java.class.path",
        "java.class.version",
        "java.endorsed.dirs",
        "java.ext.dirs",
        "java.home",
        "java.io.tmpdir",
        "java.library.path",
        "java.runtime.name",
        "java.runtime.version",
        "java.specification.name",
        "java.specification.vendor",
        "java.specification.version",
        "java.vendor.url.bug",
        "java.vendor.url",
        "java.vendor",
        "java.version",
        "java.vm.info",
        "java.vm.name",
        "java.vm.specification.name",
        "java.vm.specification.vendor",
        "java.vm.specification.version",
        "java.vm.vendor",
        "java.vm.version",
        "line.separator",
        "os.arch",
        "os.name",
        "os.version",
        "path.separator",
        "socksNonProxyHosts",
        "sun.arch.data.model",
        "sun.boot.class.path",
        "sun.boot.library.path",
        "sun.cpu.endian",
        "sun.cpu.isalist",
        "sun.io.unicode.encoding",
        "sun.java.command",
        "sun.java.launcher",
        "sun.jnu.encoding",
        "sun.management.compiler",
        "sun.os.patch.level",
        "user.country",
        "user.dir",
        "user.home",
        "user.language",
        "user.name",
        "user.timezone",
        // windows
        "user.script",
        "user.variant",
        "sun.desktop",
        // Jake
        "java.vm.compressedOopsMode",
        "jdk.boot.class.path.append",};

    static HashSet<String> ignoreSysProps  =  new HashSet(defSysProps.length + 10);

    public static void main(String args[]) {

        try {
            final ArrayList<String> cmd = new ArrayList<>(30);
            String gradleWorkerJar = null;
            String xpatchesFile = null;
            String exportsFile = null;
            String classpathFile = null;
            String jigsawJavapath = null;

            final String exportsFileProperty = "worker.exports.file";
            final String workerDebugProperty = "worker.debug";
            final String xpatchesFileProperty = "worker.xpatch.file";
            final String classpathFileProperty = "worker.classpath.file";
            final String javaCmdProperty = "worker.java.cmd";

            Collections.addAll(ignoreSysProps, defSysProps);
            ignoreSysProps.add(exportsFileProperty);
            ignoreSysProps.add(workerDebugProperty);
            ignoreSysProps.add(xpatchesFileProperty);
            ignoreSysProps.add(classpathFileProperty);
            ignoreSysProps.add(javaCmdProperty);

            debug = Boolean.parseBoolean(System.getProperty(workerDebugProperty, "false"));

            ArrayList<String> newArgs = new ArrayList<>(50);
            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith("-cp")) {
                    gradleWorkerJar = args[i + 1];
                    i++; // skip the path argument
                    if (debug) System.err.println("XWORKER gradleWorkerJar="+exportsFile);
                } else if (args[i].contains(exportsFileProperty)) {
                    int equals = args[i].indexOf("=");
                    exportsFile = args[i].substring(equals+1);
                    if (debug) System.err.println("XWORKER "+exportsFileProperty+"="+exportsFile);
                } else if (args[i].contains(xpatchesFileProperty)) {
                    int equals = args[i].indexOf("=");
                    xpatchesFile = args[i].substring(equals+1);
                    if (debug) System.err.println("XWORKER "+xpatchesFileProperty+"="+xpatchesFile);
                } else if (args[i].contains(javaCmdProperty)) {
                    int equals = args[i].indexOf("=");
                    jigsawJavapath = args[i].substring(equals+1);
                    if (debug) System.err.println("XWORKER "+javaCmdProperty+"="+jigsawJavapath);
                } else if (args[i].contains(classpathFileProperty)) {
                    int equals = args[i].indexOf("=");
                    classpathFile = args[i].substring(equals+1);
                    if (debug) System.err.println("XWORKER "+classpathFileProperty+"="+classpathFile);
                } else {
                    if (debug) System.err.println("XWORKER forwarding cmd "+args[i]);
                    newArgs.add(args[i]);
                }
            }

            // Debug routine to capture a worker stream for replaying
            if (false) {
                File dumper = new File("datadump.txt");
                FileOutputStream out
                        = new FileOutputStream(dumper);

                byte[] buf = new byte[1024];
                int cnt;

                while ((cnt = System.in.read(buf)) > 0) {
                    out.write(buf, 0, cnt);
                }
                out.close();
                System.exit(-1);
            }

            //Next we need to get the gradle encoder class from
            // the gradle-worker.jar, which usually is on the classpath
            // but as it is an arg in our case we need to load the jar
            // into a classloader
            ArrayList<URL> urlList = new ArrayList();
            urlList.add(fileToURL(new File(gradleWorkerJar)));

            URL[] urls = (URL[]) urlList.toArray(new URL[0]);
            ClassLoader cloader = new URLClassLoader(urls);

            if (debug) {
                System.err.println("AND WE HAVE " + urls[0]);
            }

            // try to find the Gradle class that decodes the input
            // stream. In newer versions (> 2. ?) they prefix with jarjar.
            Class encoderClass = null;
            if (encoderClass == null) {
                try {
                    encoderClass = Class.forName(ENCODERCLASS, true, cloader);
                } catch (ClassNotFoundException e) {
                    if (debug) {
                        System.err.println("did not find " + ENCODERCLASS);
                    }
                }
            }

            if (encoderClass != null) {
                if (debug) {
                    System.err.println("Found EncoderClass " + encoderClass.getName());
                }
            } else {
                throw new RuntimeException("Encoder not found");
            }

            Constructor constructor
                    = encoderClass.getConstructor(new Class[]{InputStream.class});

            InputStream encodedStream
                    = (InputStream) constructor.newInstance(System.in);

            DataInputStream inputStream
                    = new DataInputStream(encodedStream);

            int count = inputStream.readInt();

            //And now lets build a command line.
            ArrayList<String> cpelement = new ArrayList<>(50);

            // start with the gradle-worker.jar
            cpelement.add(gradleWorkerJar);

            // read from the data stream sent by gradle to classpath setter
            for (int dex = 0; dex < count; dex++) {
                String entry = inputStream.readUTF();
                // skipping any mention of jfxrt.jar....
                if (!entry.contains("jfxrt.jar")) {
                    File file = new File(entry);
                    cpelement.add(file.toString());
                }
            }

            if (debug) {
                for (int i = 0; i < cpelement.size(); i++) {
                    System.err.println("  " + i + " " + cpelement.get(i));
                }
            }

            String securityManagerType = inputStream.readUTF();
            if (debug) {
                System.out.println("XWORKER security manager is " + securityManagerType);
            }

            /*
             End read from the data stream send by gradle to classpath setter
             Now we need to write this into our @argfile
             */
            File classPathArgFile;

            if (classpathFile == null) {
                classpathFile = System.getProperty(classpathFileProperty);
            }

            if (classpathFile != null) {
                classPathArgFile = new File(classpathFile);
            } else if (debug) {
                classPathArgFile = new File("classpath.txt");
            } else {
                classPathArgFile = File.createTempFile("xworker-classpath", ".tmp");
            }
            try {
                BufferedWriter br = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(classPathArgFile)));

                br.write("-classpath");
                br.newLine();
                for (int i = 0; i < cpelement.size(); i++) {
                    if (i == 0) {
                        br.write("  \"");
                    } else {
                        br.write("   ");
                    }

                    br.write(cpelement.get(i).replace('\\', '/'));
                    if (i < cpelement.size() - 1) {
                        br.write(File.pathSeparator);
                        br.write("\\");
                        br.newLine();
                    }

                }
                br.write("\"");
                br.newLine();
                br.close();
            } catch (IOException e) {
                throw new RuntimeException("Could not write @classpath.txt");
            }

            String jdk_home_env = System.getenv("JIGSAW_HOME");
            if (debug) System.err.println("XWORKER JIGSAW_HOME (env) is set to " + jdk_home_env);

            if (jdk_home_env == null) {
                jdk_home_env = System.getenv("JDK_HOME");
                if (debug) System.err.println("XWORKER JDK_HOME (env) is set to " + jdk_home_env);
            }

            String jdk_home = System.getProperty("JDK_HOME");
            if (debug) System.err.println("XWORKER JDK_HOME is set to " + jdk_home);

            if (jigsawJavapath == null) {
                jigsawJavapath = System.getProperty(javaCmdProperty);
            }

            String java_cmd = "java";
            if (jigsawJavapath != null) {
                // good we have it - probably the safest way on windows
                java_cmd = jigsawJavapath;
                if (debug) System.err.println("XWORKER JIGSAW_JAVA is set to " + java_cmd);
            } else if (jdk_home_env != null) {
                jigsawJavapath = jdk_home_env
                        + File.separatorChar + "bin"
                        + File.separatorChar + "java";
            } else if (jdk_home != null) {
                java_cmd = jdk_home
                        + File.separatorChar + "bin"
                        + File.separatorChar + "java";
            }

            if (debug) {
                System.err.println("XWORKER using java  " + java_cmd);
            }

            cmd.add(java_cmd);

            cmd.add("-D"+javaCmdProperty+"="+java_cmd);

            if (xpatchesFile == null) {
                xpatchesFile = System.getProperty(xpatchesFileProperty);
            }

            if (xpatchesFile != null) {
                cmd.add("@" + xpatchesFile);
                cmd.add("-D" + xpatchesFileProperty + "=" + xpatchesFile);
            }

            if (exportsFile == null) {
                exportsFile = System.getProperty(exportsFileProperty);
            }

            if (exportsFile != null) {
                cmd.add("@" + exportsFile);
                cmd.add("-D"+exportsFileProperty+"="+exportsFile);
            }

            final String cleanpath =
                classPathArgFile.getAbsolutePath().replaceAll("\\\\", "/");
            cmd.add("@" + cleanpath);
            cmd.add("-D"+classpathFileProperty+"="+cleanpath);

            if (debug) {
                cmd.add("-D"+workerDebugProperty+"="+debug);
            }

            //forward any old system properties, other than the stock ones
            Properties p = System.getProperties();
            Enumeration keys = p.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                if (!ignoreSysProps.contains(key)) {
                    String value = (String) p.get(key);
                    String newprop = "-D"+key+"="+value;
                    cmd.add(newprop);
                    if (debug) System.out.println("XWORKER adding "+newprop);

                }
            }

            newArgs.forEach(s-> {
                cmd.add(s);
            });

            if (debug) {
                System.err.println("XWORKER: cmd is");
                cmd.stream().forEach((s) -> {
                    System.err.println(" " + s);
                });
                System.err.println("XWORKER: end cmd");
            }

            // Now we have a full command line, start the new worker process
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process proc = pb.start();

            // And now setup and forward the input to the worker thread
            // and the output from the worker thread.

            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        OutputStream os = proc.getOutputStream();

                        byte[] input = new byte[512];
                        int cnt;

                        while ((cnt = System.in.read(input)) > 0) {
                            os.write(input, 0, cnt);
                        }

                        os.close();

                    } catch (IOException io) {
                        io.printStackTrace();
                    }
                }
            }).start();

            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        InputStream is = proc.getInputStream();

                        byte[] input = new byte[1024];
                        int cnt;

                        while ((cnt = is.read(input)) > 0) {
                            System.out.write(input, 0, cnt);
                        }

                        is.close();

                    } catch (IOException io) {
                        io.printStackTrace();
                    }
                }
            }).start();

            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        InputStream es = proc.getErrorStream();

                        byte[] input = new byte[1024];
                        int cnt;

                        while ((cnt = es.read(input)) > 0) {
                            System.err.write(input, 0, cnt);
                        }

                        es.close();

                    } catch (IOException io) {
                        io.printStackTrace();
                    }
                }
            }).start();

            // And lastly - forward the exit code from the worker
            System.exit(proc.waitFor());

        } catch (Exception e) {
            throw new RuntimeException("Could not initialise system classpath.", e);
        }
    }
}
