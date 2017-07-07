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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.charset.Charset;

/**
 * The {@code SingleInstanceService} class provides public methods for using
 * Single Instance functionality for Java Packager. To use these methods,
 * the option named "-singleton" must be specified on javapackager command line.
 *
 * @since 10
 */
public class SingleInstanceService {

    static private boolean DEBUG = false;
    static private PrintStream DEBUG_STREAM = null;

    static private int currPort;
    static private String stringId = null;
    static private String randomNumberString = null;

    static private SingleInstanceImpl instance = null;

    static final int ENCODING_PLATFORM = 1;
    static final int ENCODING_UNICODE = 2;

    static final String ENCODING_PLATFORM_NAME = "UTF-8";
    static final String ENCODING_UNICODE_NAME = "UTF-16LE";

    static final String APP_ID_PREFIX = "javapackager.si.";

    private SingleInstanceService() {}

    static void enableDebug(boolean enable, PrintStream stream) {
        DEBUG = enable;
        DEBUG_STREAM = stream;
    }

    static void trace(String message) {
        if (DEBUG && DEBUG_STREAM != null) {
            DEBUG_STREAM.println(message);
        }
    }

    static void trace(Throwable t) {
        if (DEBUG && DEBUG_STREAM != null) {
            t.printStackTrace(DEBUG_STREAM);
        }
    }

    /**
     * Registers {@code SingleInstanceListener} for current process.
     * If the {@code SingleInstanceListener} object is already registered, or
     * {@code slistener} is {@code null}, then the registration is skipped.
     *
     * @param slistener the listener to handle the single instance behaviour.
     */
    public static void registerSingleInstance(SingleInstanceListener slistener) {
        registerSingleInstance(slistener, false);
    }

    /**
     * Registers {@code SingleInstanceListener} for current process.
     * If the {@code SingleInstanceListener} object is already registered, or
     * {@code slistener} is {@code null}, then the registration is skipped.
     *
     * @param slistener the listener to handle the single instance behaviour.
     * @param setFileHandler if {@code true}, the listener is notified when the
     *         application is asked to open a list of files. If OS is not MacOS,
     *         the parameter is ignored.
     */
    public static void registerSingleInstance(SingleInstanceListener slistener,
                                              boolean setFileHandler) {
        String appId = APP_ID_PREFIX + ProcessHandle.current().pid();
        registerSingleInstanceForId(slistener, appId, setFileHandler);
    }

    static void registerSingleInstanceForId(SingleInstanceListener slistener,
                                                   String stringId, boolean setFileHandler) {
        // register SingleInstanceListener for given Id
        instance = new SingleInstanceImpl();
        instance.addSingleInstanceListener(slistener, stringId);
        if (setFileHandler) {
            instance.setOpenFileHandler();
        }
    }

    /**
     * Unregisters {@code SingleInstanceListener} for current process.
     * If the {@code SingleInstanceListener} object is not registered, or
     * {@code slistener} is {@code null}, then the unregistration is skipped.
     *
     * @param slistener the listener for unregistering.
     */
    public static void unregisterSingleInstance(SingleInstanceListener slistener) {
        instance.removeSingleInstanceListener(slistener);
    }

    /**
     * Returns true if single instance server is running for the id
     */
    static boolean isServerRunning(String id) {
        trace("isServerRunning ? : "+ id);
        File siDir = new File(SingleInstanceImpl.SI_FILEDIR);
        String[] fList = siDir.list();
        if (fList != null) {
            String prefix = SingleInstanceImpl.getSingleInstanceFilePrefix(id +
                                                    getSessionSpecificString());
            for (String file : fList) {
                trace("isServerRunning: " + file);
                trace("\t sessionString: " + getSessionSpecificString());
                trace("\t SingleInstanceFilePrefix: " + prefix);
                // if file with the same prefix already exist, server is running
                if (file.startsWith(prefix)) {
                    try {
                        currPort = Integer.parseInt(
                                    file.substring(file.lastIndexOf('_') + 1));
                        trace("isServerRunning: " + file + ": port: " + currPort);
                    } catch (NumberFormatException nfe) {
                        trace("isServerRunning: " + file + ": port parsing failed");
                        trace(nfe);
                        return false;
                    }

                    trace("Server running at port: " + currPort);
                    File siFile = new File(SingleInstanceImpl.SI_FILEDIR, file);

                    // get random number from single instance file
                    try (BufferedReader br = new BufferedReader(new FileReader(siFile))) {
                        randomNumberString = br.readLine();
                        trace("isServerRunning: " + file + ": magic: " + randomNumberString);
                    } catch (IOException ioe ) {
                        trace("isServerRunning: " + file + ": reading magic failed");
                        trace(ioe);
                    }
                    trace("isServerRunning: " + file + ": setting id - OK");
                    stringId = id;
                    return true;
                } else {
                    trace("isServerRunning: " + file + ": prefix NOK");
                }
            }
        } else {
            trace("isServerRunning: empty file list");
        }
        trace("isServerRunning: false");
        return false;
    }

    /**
     * Returns true if we connect successfully to the server for the stringId
     */
    static boolean connectToServer(String[] args) {
        trace("Connect to: " + stringId + " " + currPort);

        if (randomNumberString == null) {
            // should not happen
            trace("MAGIC number is null, bail out.");
            return false;
        }

        // Now we open the tcpSocket and the stream
        Socket socket = null;
        OutputStream os = null;
        PrintStream out = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            socket = new Socket("127.0.0.1", currPort);
            os = socket.getOutputStream();
            byte[] encoding = new byte[1];
            encoding[0] = ENCODING_PLATFORM;
            os.write(encoding);
            String encodingName = Charset.defaultCharset().name();

            out = new PrintStream(os, true, encodingName);
            isr = new InputStreamReader(socket.getInputStream(), encodingName);
            br = new BufferedReader(isr);

            // send random number
            out.println(randomNumberString);
            // send MAGICWORD
            out.println(SingleInstanceImpl.SI_MAGICWORD);

            for (String arg : args) {
                out.println(arg);
            }

            // indicate end of file transmission
            out.println(SingleInstanceImpl.SI_EOF);
            out.flush();

            // wait for ACK (OK) response
            trace("Waiting for ack");
            final int tries = 5;

            // try to listen for ACK
            for (int i=0; i < tries; i++) {
                String str = br.readLine();
                if (str != null && str.equals(SingleInstanceImpl.SI_ACK)) {
                    trace("Got ACK");
                    return true;
                }
            }
        } catch (java.net.SocketException se) {
            // no server is running - continue launch
            trace("No server is running - continue launch.");
            trace(se);
        } catch (Exception ioe) {
            trace(ioe);
        }
        finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (isr != null) {
                    isr.close();
                }
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
                trace(ioe);
            }
        }
        trace("No ACK from server, bail out.");
        return false;
    }

    static String getSessionSpecificString() {
        // TODO: consider providing session ids
        return "";
    }
}
