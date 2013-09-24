/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.networking.worker;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import com.javafx.experiments.dukepad.networking.NetworkInterface;

/**
 * Gets the complete set of network interfaces from the operating system.
 */
public class GetNetworkInterfacesTask extends BaseCommandTask<List<NetworkInterface>> {
    /*
    eth0      Link encap:Ethernet  HWaddr b8:27:eb:8c:d6:f6
              UP BROADCAST MULTICAST  MTU:1500  Metric:1
              RX packets:0 errors:0 dropped:0 overruns:0 frame:0
              TX packets:0 errors:0 dropped:0 overruns:0 carrier:0
              collisions:0 txqueuelen:1000
              RX bytes:0 (0.0 B)  TX bytes:0 (0.0 B)

    lo        Link encap:Local Loopback
              inet addr:127.0.0.1  Mask:255.0.0.0
              UP LOOPBACK RUNNING  MTU:16436  Metric:1
              RX packets:0 errors:0 dropped:0 overruns:0 frame:0
              TX packets:0 errors:0 dropped:0 overruns:0 carrier:0
              collisions:0 txqueuelen:0
              RX bytes:0 (0.0 B)  TX bytes:0 (0.0 B)

    wlan0     Link encap:Ethernet  HWaddr 80:1f:02:b6:bb:0f
              inet addr:10.0.1.11  Bcast:10.0.1.255  Mask:255.255.255.0
              UP BROADCAST RUNNING MULTICAST  MTU:1500  Metric:1
              RX packets:110073 errors:0 dropped:114519 overruns:0 frame:0
              TX packets:51833 errors:0 dropped:0 overruns:0 carrier:0
              collisions:0 txqueuelen:1000
              RX bytes:82542402 (78.7 MiB)  TX bytes:10842333 (10.3 MiB)
    */
    @Override protected List<NetworkInterface> call() throws Exception {
        JobModel job = executeCommand("ifconfig", "-a");
        List<NetworkInterface> interfaces = new LinkedList<>();
        if (job.getResult() == 0) {
            String stdout = job.getStdOutText();

            // stdout is formatted as seen above, where in the first column we find the name of the
            // interface, and in the second column the information about that interface. So we will
            // visit each line, looking for lines that contain a non-whitespace character as the first
            // character of the line. This will denote the start of a new section. We will read the line
            // up to the first whitespace. This is the name. We will read the remainder of the line and
            // all remaining lines up to the start of a new section, throwing away empty lines.
            // Now we have our individual sections. We'll then process each individually.

            String interfaceName = null;
            String interfaceBody = null;
            String[] lines = new BufferedReader(new StringReader(stdout)).lines().toArray(String[]::new);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (i == lines.length - 1 || line.isEmpty() || (!Character.isWhitespace(line.charAt(0)) && interfaceName != null)) {
                    final String name = interfaceName; // The "en0" and "en2" are a hack for mac desktop testing for Richard
                    final NetworkInterface.Type type = interfaceName.startsWith("eth") || interfaceName.startsWith("en2") ?
                            NetworkInterface.Type.WIRED : interfaceName.startsWith("wlan") || interfaceName.startsWith("en0") ?
                            NetworkInterface.Type.WIFI : interfaceName.startsWith("lo") ?
                            NetworkInterface.Type.LOOPBACK : interfaceName.startsWith("p2p") ?
                            NetworkInterface.Type.P2P : interfaceName.startsWith("vnic") ?
                            NetworkInterface.Type.VIRTUAL : NetworkInterface.Type.UNKNOWN;
                    final String ipAddress = parseIPAddress(interfaceBody);
                    final String subnet = parseSubnet(interfaceBody);
                    interfaces.add(new NetworkInterfaceImpl(ipAddress, subnet, name, type));
                    interfaceName = null;
                }

                if (!line.isEmpty()) {
                    if (Character.isWhitespace(line.charAt(0))) {
                        interfaceBody += "\n" + line;
                    } else {
                        // Start of a new block
                        int firstSpaceIndex = line.indexOf(':');
                        if (firstSpaceIndex == -1) firstSpaceIndex = line.indexOf(' ');
                        assert firstSpaceIndex > 0;
                        interfaceName = line.substring(0, firstSpaceIndex);
                        interfaceBody = line;
                    }
                }
            }
        }
        return interfaces;
    }

    private String parseIPAddress(String body) {
        StringTokenizer tokenizer = new StringTokenizer(body, " \n\t");
        while(tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.startsWith("addr:")) {
                // PI code path
                return token.substring("addr:".length()).trim();
            } else if ("inet".equals(token)) {
                // Mac code path
                return tokenizer.nextToken().trim();
            }
        }
        return null;
    }

    private String parseSubnet(String body) {
        StringTokenizer tokenizer = new StringTokenizer(body, " \n\t");
        while(tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.startsWith("Mask:")) {
                // PI code path
                return token.substring("Mask:".length()).trim();
            } else if ("netmask".equals(token)) {
                // Mac code path
                String hex = tokenizer.nextToken().substring(2).trim();
                long mask = Long.parseLong(hex, 16);
                return ((mask >> 24) & 0xFF) + "." + ((mask >> 16) & 0xFF) + "." + ((mask >> 8) & 0xFF) + "." + (mask & 0xFF);
            }
        }
        return null;
    }

    private static final class NetworkInterfaceImpl implements NetworkInterface {
        private final StringProperty address = new SimpleStringProperty(this, "address");
        private final StringProperty subnet = new SimpleStringProperty(this, "subnet");
        private String name;
        private Type type;

        NetworkInterfaceImpl(String address, String subnet, String name, Type type) {
            this.address.set(address);
            this.subnet.set(subnet);
            this.name = name;
            this.type = type;
        }

        @Override
        public final String getAddress() {
            return address.get();
        }

        @Override
        public final StringProperty addressProperty() {
            return address;
        }

        @Override
        public final String getSubnet() {
            return subnet.get();
        }

        @Override
        public final StringProperty subnetProperty() {
            return subnet;
        }

        @Override
        public final String getName() {
            return name;
        }

        @Override
        public final Type getType() {
            return type;
        }

        @Override
        public final boolean isUp() {
            return address != null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NetworkInterfaceImpl that = (NetworkInterfaceImpl) o;

            if (address != null ? !address.equals(that.address) : that.address != null) return false;
            if (!name.equals(that.name)) return false;
            if (subnet != null ? !subnet.equals(that.subnet) : that.subnet != null) return false;
            if (type != that.type) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = address != null ? address.hashCode() : 0;
            result = 31 * result + (subnet != null ? subnet.hashCode() : 0);
            result = 31 * result + name.hashCode();
            result = 31 * result + type.hashCode();
            return result;
        }

        @Override public String toString() {
            return getName() + " address " + getAddress() + " subnet " + getSubnet() + " type " + type + (isUp() ? " UP" : " DOWN");
        }
    }
}
