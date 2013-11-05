/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.mobilecenter;

import com.sun.javafx.appmanager.FxApplicationInstance;
import com.sun.javafx.appmanager.FxApplicationManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.Manifest;

final class BackgroundService {

    public static final String GROUP_ADDRESS = "225.10.10.0";
    public static final int PORT = 44445;
    public static final int PORT_FILE = 44446;
    public static final int PACKET_LENGTH = 1024;
    public static final String MULTICAST_DISCOVERY = "NetBeansMulticastDiscovery";

    private ThreadGroup threadGroup;

    public void loop() {
        boolean firstRun = true;
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            InetAddress group = InetAddress.getByName(GROUP_ADDRESS);
            socket.joinGroup(group);

            while (true) {
                String appJarPath = null;

                if (!firstRun) {
                    DatagramPacket packet;
                    byte[] buf = new byte[PACKET_LENGTH];
                    packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    String received = new String(packet.getData());
                    if (!received.trim().equals(MULTICAST_DISCOVERY)) {
                        break;
                    }

                    buf = String.valueOf(PORT_FILE).getBytes();
                    packet = new DatagramPacket(buf, buf.length, packet.getSocketAddress());
                    socket.send(packet);
                    appJarPath = receiveFile();
                } else {
                    firstRun = false;
                }

                final String filePath = appJarPath;
                stopThreadGroup();
                threadGroup = new ThreadGroup("Mobile Center Group");
                synchronized (threadGroup) {
                    new Thread(threadGroup, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                invokeMainMethod(filePath);
                            } catch (ThreadDeath td) {
                                throw td;
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }
                        }
                    }, "Mobile Center Main").start();
                }
            }
            socket.leaveGroup(group);
            System.exit(0);
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    private String receiveFile() {
        try (ServerSocket serverSocket = new ServerSocket(PORT_FILE);
                Socket socket = serverSocket.accept();
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
            Object receivedObject = ois.readObject();
            if (receivedObject instanceof byte[]) {
                File targetFile = File.createTempFile("Debug", ".jar");
                byte[] buffer = (byte[]) receivedObject;
                try (OutputStream out = new FileOutputStream(targetFile)) {
                    out.write(buffer);
                }
                return targetFile.getAbsolutePath();
            }
            return null;
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println(ex.getMessage());
            return null;
        }
    }

    private FxApplicationInstance lastFxApplication;

    public void invokeMainMethod(String jarPath) {
        try {
            ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
            URL[] cpURLs = ((URLClassLoader) sysClassLoader).getURLs();
            URL[] classLoaderURLs = new URL[cpURLs.length];
            System.arraycopy(cpURLs, 0, classLoaderURLs, 0, cpURLs.length);

            String appJarName = System.getProperty("bgReloadJar");
            String jarWithManifest = appJarName;
            if (jarPath != null) {
                URL appURL = new File(jarPath).toURI().toURL();
                for (int i = 0; i < classLoaderURLs.length; i++) {
                    if (classLoaderURLs[i].getPath().endsWith(appJarName)) {
                        classLoaderURLs[i] = appURL;
                    }
                }
                jarWithManifest = new File(jarPath).getName();
            }

            URLClassLoader classLoader = URLClassLoader.newInstance(classLoaderURLs, ClassLoader.getSystemClassLoader().getParent());

            boolean fxEnabled = isFXApplication(classLoader, jarWithManifest);

            if (lastFxApplication != null) {
                lastFxApplication.stop();
                lastFxApplication = null;
            }

            if (fxEnabled) {
                final FxApplicationManager appManager =
                        FxApplicationManager.getInstance();
                final String appClass =
                        findMainClass(classLoader, jarWithManifest);
                lastFxApplication = appManager.start(classLoader, appClass);
            } else {
                Thread.currentThread().setContextClassLoader(classLoader);

                Class clazz = classLoader.loadClass(findMainClass(classLoader, jarWithManifest));
                Method method = clazz.getDeclaredMethod("main", String[].class);
                method.setAccessible(true);
                String[] params = null;
                OutputStream os = new ByteArrayOutputStream();
                System.setOut(new PrintStream(os));
                method.invoke(classLoader, (Object) params);
            }
        } catch (Exception ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    private void stopThreadGroup() {
        if (threadGroup == null) {
            return;
        }
        synchronized (threadGroup) {
            int count = threadGroup.activeCount();
            Thread[] threads = new Thread[count];
            threadGroup.enumerate(threads, true);
            for (Thread t : threads) {
                t.stop();
            }
        }
    }

    private static String findMainClass(final ClassLoader cl, String jarWithManifest) throws IOException {
        String mainClass = null;
        Enumeration<URL> en = cl.getResources("META-INF/MANIFEST.MF");
        while (en.hasMoreElements()) {
            URL url = en.nextElement();
            Manifest mf;
            try (InputStream is = url.openStream()) {
                mf = new Manifest(is);
            }
            String mc = mf.getMainAttributes().getValue("JavaFX-Application-Class");
            if (mc == null || mc.isEmpty()) {
                mc = mf.getMainAttributes().getValue("Main-Class");
            }
            if (mc != null && url.getPath().contains(jarWithManifest)) {
                mainClass = mc;
                break;
            }
        }
        return mainClass;
    }

    private static boolean isFXApplication(final ClassLoader cl, String jarWithManifest) throws IOException {
        boolean fxApp = false;
        Enumeration<URL> en = cl.getResources("META-INF/MANIFEST.MF");
        while (en.hasMoreElements()) {
            URL url = en.nextElement();
            Manifest mf;
            try (InputStream is = url.openStream()) {
                mf = new Manifest(is);
            }
            String mc = mf.getMainAttributes().getValue("JavaFX-Version");
            if (mc != null && url.getPath().contains(jarWithManifest)) {
                fxApp = true;
                break;
            }
        }
        return fxApp;
    }
}
