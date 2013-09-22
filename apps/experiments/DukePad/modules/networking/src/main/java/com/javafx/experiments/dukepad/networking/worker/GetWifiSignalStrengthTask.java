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

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.StringTokenizer;
import com.javafx.experiments.dukepad.networking.WifiSignalStrength;

/**
 * Returns the Wifi signal strength for the requested interface, if it can.
 * If the interfaceName is null, empty, or * then we'll just grab the first
 * answer we can find.
 */
public class GetWifiSignalStrengthTask extends BaseCommandTask<WifiSignalStrength> {
    private final String interfaceName;

    public GetWifiSignalStrengthTask(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    @Override
    protected WifiSignalStrength call() throws Exception {
        JobModel job = executeCommand("cat", "/proc/net/wireless");
        if (job.getResult() == 0) {
            final String stdout = job.getStdOutText();
            BufferedReader r = new BufferedReader((new StringReader(stdout)));
            /*String headerLine1 = */r.readLine();
            /*String headerLine2 = */r.readLine();
            String line;
            while ((line = r.readLine()) != null) {
                StringTokenizer tokens = new StringTokenizer(line, " ");
                String interfaceName = tokens.nextToken();
                if (this.interfaceName == null || this.interfaceName.equals("*") ||
                        this.interfaceName.isEmpty() || this.interfaceName.equals(interfaceName)) {
                    /*String status = */tokens.nextToken();
                    int link = (int) Double.parseDouble(tokens.nextToken());
                    int level = (int) Double.parseDouble(tokens.nextToken());
                    int noise = (int) Double.parseDouble(tokens.nextToken());

                    return new WifiSignalStrength() {
                        @Override public String getNetworkInterfaceName() { return interfaceName; }
                        @Override public int getLink() { return link; }
                        @Override public int getLevel() { return level; }
                        @Override public int getNoise() { return noise; }
                    };
                }
            }
        } else {
            // Try the mac code path
            job = executeCommand("/System/Library/PrivateFrameworks/Apple80211.framework/Versions/A/Resources/airport", "-I");
            if (job.getResult() == 0) {
                final String stdout = job.getStdOutText();
                BufferedReader r = new BufferedReader((new StringReader(stdout)));
                String line = r.readLine();
                String agrCtrlRSSI = line.substring(line.indexOf(":")+1).trim();
                r.readLine(); // agrExtRSSI
                line = r.readLine();
                String agrCtlNoise = line.substring(line.indexOf(":")+1).trim();
                // I have no idea what I'm doing here, or how to interpret these numbers
                final int link = 100 + Integer.parseInt(agrCtrlRSSI);
                final int noise = 100 + Integer.parseInt(agrCtlNoise);

                return new WifiSignalStrength() {
                    @Override public String getNetworkInterfaceName() { return interfaceName; }
                        @Override public int getLink() { return link; }
                        @Override public int getLevel() { return 0; }
                        @Override public int getNoise() { return noise; }
                };
            }
        }
        return null;
    }
}
