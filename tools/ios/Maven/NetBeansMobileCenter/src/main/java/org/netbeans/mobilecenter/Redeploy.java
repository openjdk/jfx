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

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;

public final class Redeploy {
    private Redeploy() {
    }

    public static void main(String... args) {
        if (args.length != 1) {
            System.err.println("Usage: <jar_file_to_deploy>");
            System.exit(5);
        }
        File f = new File(args[0]);
        if (!f.exists()) {
            System.err.println("File " + f + " does not exist");
            System.exit(1);
        }

        deploy(f);
        System.err.println("Success!");
        System.exit(0);
    }

    public static final String MULTICAST_DISCOVERY = "NetBeansMulticastDiscovery";
    public static final String GROUP_ADDRESS = "225.10.10.0";
    public static final int PORT = 44445;
    public static final int PACKET_LENGTH = 1024;
    public static final int TIMEOUT = 5000;

    private static void deploy(File f) {
        DatagramSocket datagramSocket = null;

        try {
            byte[] buf = new byte[PACKET_LENGTH];
            buf = MULTICAST_DISCOVERY.getBytes();
            datagramSocket = new DatagramSocket();
            InetAddress group = InetAddress.getByName(GROUP_ADDRESS);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, group, PORT);
            datagramSocket.send(packet);

            buf = new byte[PACKET_LENGTH];
            datagramSocket.setSoTimeout(TIMEOUT);
            packet = new DatagramPacket(buf, buf.length);
            datagramSocket.receive(packet);
            String received = new String(packet.getData()).trim();
            if (!received.equals("firstRun")) {
                Thread.sleep(1000);
                sendFile(f, packet.getAddress(), Integer.parseInt(received));
            }
            //JOptionPane.showMessageDialog(null, "Found iOS device on IP: " + received);
        } catch (SocketTimeoutException ex) {
            System.err.println("No iOS devices were found");
            System.exit(2);
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
            System.exit(3);
        } finally {
            if (datagramSocket != null) {
                datagramSocket.close();
            }
        }
    }

    private static void sendFile(File f, InetAddress iOSAddress, int port) throws IOException {
        System.err.println("Deploying " + f + " to " + iOSAddress + ":" + port);
        try (Socket socket = new Socket(iOSAddress, port)) {
            byte[] buffer = Files.readAllBytes(f.toPath());
            try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {
                oos.writeObject(buffer);
            }
        }
    }

}
