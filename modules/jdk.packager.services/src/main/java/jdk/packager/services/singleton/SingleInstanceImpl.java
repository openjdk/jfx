/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.packager.services.singleton;

import java.awt.Desktop;
import java.awt.desktop.OpenFilesHandler;
import java.awt.desktop.OpenFilesEvent;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.security.PrivilegedAction;
import java.security.AccessController;
import java.security.SecureRandom;


class SingleInstanceImpl {

    static final String SI_FILEDIR = getTmpDir() + File.separator
                                                 + "si" + File.separator;
    static final String SI_MAGICWORD = "javapackager.singleinstance.init";
    static final String SI_ACK = "javapackager.singleinstance.ack";
    static final String SI_STOP = "javapackager.singleinstance.stop";
    static final String SI_EOF = "javapackager.singleinstance.EOF";

    private final ArrayList<SingleInstanceListener> siListeners = new ArrayList<>();
    private SingleInstanceServer siServer;

    private static final SecureRandom random = new SecureRandom();
    private static volatile boolean serverStarted = false;
    private static int randomNumber;

    private final Object lock = new Object();

    static String getSingleInstanceFilePrefix(final String stringId) {
        String filePrefix = stringId.replace('/','_');
        filePrefix = filePrefix.replace(':','_');
        return filePrefix;
    }

    static String getTmpDir() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return System.getProperty("user.home")
                    + "\\AppData\\LocalLow\\Sun\\Java\\Packager\\tmp";
        } else if (os.contains("mac") || os.contains("os x")) {
            return System.getProperty("user.home")
                    + "/Library/Application Support/Oracle/Java/Packager/tmp";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return System.getProperty("user.home") + "/.java/packager/tmp";
        }

        return System.getProperty("java.io.tmpdir");
    }

    void addSingleInstanceListener(SingleInstanceListener sil, String id) {

        if (sil == null || id == null) {
            return;
        }

        // start a new server thread for this unique id
        // first time
        synchronized (lock) {
            if (!serverStarted) {
                SingleInstanceService.trace("unique id: " + id);
                try {
                    String sessionID = id +
                            SingleInstanceService.getSessionSpecificString();
                    siServer = new SingleInstanceServer(sessionID);
                    siServer.start();
                } catch (Exception e) {
                    SingleInstanceService.trace("addSingleInstanceListener failed");
                    SingleInstanceService.trace(e);
                    return; // didn't start
                }
                serverStarted = true;
            }
        }

        synchronized (siListeners) {
            // add the sil to the arrayList
            if (!siListeners.contains(sil)) {
                siListeners.add(sil);
            }
        }

    }

    class SingleInstanceServer {

        private final SingleInstanceServerRunnable runnable;
        private final Thread thread;

        SingleInstanceServer(SingleInstanceServerRunnable runnable) throws IOException {
            thread = new Thread(null, runnable, "JavaPackagerSIThread", 0, false);
            thread.setDaemon(true);
            this.runnable = runnable;
        }

        SingleInstanceServer(String stringId) throws IOException {
            this(new SingleInstanceServerRunnable(stringId));
        }

        int getPort() {
            return runnable.getPort();
        }

        void start() {
            thread.start();
        }
    }

    private class SingleInstanceServerRunnable implements Runnable {

        ServerSocket ss;
        int port;
        String stringId;
        String[] arguments;

        int getPort() {
            return port;
        }

        SingleInstanceServerRunnable(String id) throws IOException {
            stringId = id;

            // open a free ServerSocket
            ss = null;

            // we should bind the server to the local InetAddress 127.0.0.1
            // port number is automatically allocated for current SI
            ss = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"));

            // get the port number
            port = ss.getLocalPort();
            SingleInstanceService.trace("server port at: " + port);

            // create the single instance file with canonical home and port number
            createSingleInstanceFile(stringId, port);
        }

        private String getSingleInstanceFilename(final String id, final int port) {
            String name = SI_FILEDIR + getSingleInstanceFilePrefix(id) + "_" + port;
            SingleInstanceService.trace("getSingleInstanceFilename: " + name);
            return name;
        }

        private void removeSingleInstanceFile(final String id, final int port) {
            new File(getSingleInstanceFilename(id, port)).delete();
            SingleInstanceService.trace("removed SingleInstanceFile: "
                                        + getSingleInstanceFilename(id, port));
        }

        private void createSingleInstanceFile(final String id, final int port) {
            String filename = getSingleInstanceFilename(id, port);
            final File siFile = new File(filename);
            final File siDir = new File(SI_FILEDIR);
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    siDir.mkdirs();
                    String[] fList = siDir.list();
                    if (fList != null) {
                        String prefix = getSingleInstanceFilePrefix(id);
                        for (String file : fList) {
                            // if file with the same prefix already exist, remove it
                            if (file.startsWith(prefix)) {
                                SingleInstanceService.trace(
                                        "file should be removed: " + SI_FILEDIR + file);
                                new File(SI_FILEDIR + file).delete();
                            }
                        }
                    }

                    PrintStream out = null;
                    try {
                        siFile.createNewFile();
                        siFile.deleteOnExit();
                        // write random number to single instance file
                        out = new PrintStream(new FileOutputStream(siFile));
                        randomNumber = random.nextInt();
                        out.print(randomNumber);
                    } catch (IOException ioe) {
                        SingleInstanceService.trace(ioe);
                    } finally {
                        if (out != null) {
                            out.close();
                        }
                    }
                    return null;
                }
            });
        }

        @Override
        public void run() {
            // start sil to handle all the incoming request
            // from the server port of the current url
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    List<String> recvArgs = new ArrayList<>();
                    while (true) {
                        recvArgs.clear();
                        InputStream is = null;
                        BufferedReader in = null;
                        InputStreamReader isr = null;
                        Socket s = null;
                        String line = null;
                        boolean sendAck = false;
                        int port = -1;
                        String charset = null;
                        try {
                            SingleInstanceService.trace("waiting connection");
                            s = ss.accept();
                            is = s.getInputStream();
                            // read first byte for encoding type
                            int encoding = is.read();
                            if (encoding == SingleInstanceService.ENCODING_PLATFORM) {
                                charset = Charset.defaultCharset().name();
                            } else if (encoding == SingleInstanceService.ENCODING_UNICODE) {
                                charset = SingleInstanceService.ENCODING_UNICODE_NAME;
                            } else {
                                SingleInstanceService.trace("SingleInstanceImpl - unknown encoding");
                                return null;
                            }
                            isr = new InputStreamReader(is, charset);
                            in = new BufferedReader(isr);
                            // first read the random number
                            line = in.readLine();
                            if (line.equals(String.valueOf(randomNumber)) == false) {
                                // random number does not match
                                // should not happen
                                // shutdown server socket
                                removeSingleInstanceFile(stringId, port);
                                ss.close();
                                serverStarted = false;
                                SingleInstanceService.trace("Unexpected Error, "
                                        + "SingleInstanceService disabled");
                                return null;
                            } else {
                                line = in.readLine();
                                // no need to continue reading if MAGICWORD
                                // did not come first
                                SingleInstanceService.trace("recv: " + line);
                                if (line.equals(SI_MAGICWORD)) {
                                    SingleInstanceService.trace("got magic word.");
                                    while (true) {
                                        // Get input string
                                        try {
                                            line = in.readLine();
                                            if (line != null && line.equals(SI_EOF)) {
                                                // end of file reached
                                                break;
                                            } else {
                                                recvArgs.add(line);
                                            }
                                        } catch (IOException ioe) {
                                            SingleInstanceService.trace(ioe);
                                        }
                                    }
                                    arguments = recvArgs.toArray(new String[recvArgs.size()]);
                                    sendAck = true;
                                } else if (line.equals(SI_STOP)) {
                                    // remove the SingleInstance file
                                    removeSingleInstanceFile(stringId, port);
                                    break;
                                }
                            }
                        } catch (IOException ioe) {
                            SingleInstanceService.trace(ioe);
                        } finally {
                            try {
                                if (sendAck) {
                                    // let the action listener handle the rest
                                    for (String arg : arguments) {
                                        SingleInstanceService.trace(
                                                "Starting new instance with arguments: arg:" + arg);
                                    }

                                    performNewActivation(arguments);

                                    // now the event is handled, we can send
                                    // out the ACK
                                    SingleInstanceService.trace("sending out ACK");
                                    if (s != null) {
                                        try (OutputStream os = s.getOutputStream();
                                            PrintStream ps = new PrintStream(os, true, charset)) {
                                            // send OK (ACK)
                                            ps.println(SI_ACK);
                                            ps.flush();
                                        }
                                    }
                                }

                                if (in != null) {
                                    in.close();
                                }

                                if (isr != null) {
                                    isr.close();
                                }

                                if (is != null) {
                                    is.close();
                                }

                                if (s != null) {
                                    s.close();
                                }
                            } catch (IOException ioe) {
                                SingleInstanceService.trace(ioe);
                            }
                        }
                    }
                    return null;
                }
            });
        }
    }

    private void performNewActivation(final String[] args) {
        // enumerate the sil list and call
        // each sil with arguments
        @SuppressWarnings("unchecked")
        ArrayList<SingleInstanceListener> silal =
                (ArrayList<SingleInstanceListener>)siListeners.clone();
        silal.forEach(sil -> sil.newActivation(args));
    }

    void setOpenFileHandler() {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("mac") && !os.contains("os x")) {
            return;
        }

        Desktop.getDesktop().setOpenFileHandler(new OpenFilesHandler() {
            @Override
            public void openFiles(OpenFilesEvent e) {
                List<String> arguments = new ArrayList<>();
                e.getFiles().forEach(file -> arguments.add(file.toString()));
                performNewActivation(arguments.toArray(
                                    new String[arguments.size()]));
            }
        });
    }

    void removeSingleInstanceListener(SingleInstanceListener sil) {
        if (sil == null) {
            return;
        }

        synchronized (siListeners) {

            if (!siListeners.remove(sil)) {
                return;
            }

            if (siListeners.isEmpty()) {
                 AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        // stop server
                        Socket socket = null;
                        PrintStream out = null;
                        OutputStream os = null;
                        try {
                            socket = new Socket("127.0.0.1", siServer.getPort());
                            os = socket.getOutputStream();
                            byte[] encoding = new byte[1];
                            encoding[0] = SingleInstanceService.ENCODING_PLATFORM;
                            os.write(encoding);
                            String charset = Charset.defaultCharset().name();
                            out = new PrintStream(os, true, charset);
                            out.println(randomNumber);
                            out.println(SingleInstanceImpl.SI_STOP);
                            out.flush();
                            serverStarted = false;
                        } catch (IOException ioe) {
                            SingleInstanceService.trace(ioe);
                        } finally {
                            try {
                                if (out != null) {
                                    out.close();
                                }
                                if (os != null) {
                                    os.close();
                                }
                                if (socket != null) {
                                    socket.close();
                                }
                            } catch (IOException ioe) {
                                SingleInstanceService.trace(ioe);
                            }
                        }
                        return null;
                    }
               });
            }
        }
    }
}
