/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.d3d;

/*
 * This class basicly is a data structure
 * with driver information details.
 */
class D3DDriverInformation {

    public String deviceName;
    public String deviceDescription;
    public String driverName;
    public String warningMessage;

    // driver version information
    public int product, version, subVersion, buildID;
    public int psVersionMajor, psVersionMinor;
    public int maxSamples = 0;

    public String getDriverVersion() {
        return String.format("%d.%d.%d.%d", product, version, subVersion, buildID);
    }

    // device information
    public int vendorID, deviceID, subSysId;

    public String getDeviceID() {
        return String.format("ven_%04X, dev_%04X, subsys_%08X",
                vendorID, deviceID, subSysId);
    }

    // OS version information
    public int osMajorVersion, osMinorVersion, osBuildNumber;

    public String getOsVersion() {
        switch (osMajorVersion) {
            case 6:
                switch (osMinorVersion) {
                    case 0: return "Windows Vista";
                    case 1: return "Windows 7";
                    case 2: return "Windows 8.0";
                    case 3: return "Windows 8.1";
                } break;
            case 5:
                switch (osMinorVersion) {
                    case 0: return "Windows 2000";
                    case 1: return "Windows XP";
                    case 2: return "Windows Server 2003";
                } break;
        }
        return "Windows version "+osMajorVersion+"."+osMinorVersion;
    }


}
