/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef D3DBADHARDWARE_H
#define D3DBADHARDWARE_H

#include "D3DPipeline.h"
#include "D3DPipelineManager.h"

typedef struct ADAPTER_INFO {
  DWORD    VendorId;
  DWORD    DeviceId;
  LONGLONG DriverVersion; // minimum driver version to pass, or NO_VERSION
  USHORT   OsInfo;        // OSes where the DriverVersion is relevant or, OS_ALL
} ADAPTER_INFO;

// this DeviceId means that all vendor boards are to be excluded
#define ALL_DEVICEIDS (0xffffffff)

#define D_VERSION(H1, H2, L1, L2) \
  (((LONGLONG)((H1 << 16) | H2) << 32) | ((L1 << 16) | (L2)))

// this driver version is used to pass the driver version check
// as it is always greater than any driver version
#define MAX_VERSION D_VERSION(0x7fff, 0x7fff, 0x7fff, 0x7fff)
// this DriverVersion means that the version of the driver doesn't matter,
// all versions must fail ("there's no version of the driver that passes")
#define NO_VERSION D_VERSION(0xffff, 0xffff, 0xffff, 0xffff)

static const ADAPTER_INFO badHardware[] = {

    // Intel HD
    // Reason: workaround for RT-11540
    // Clarkdale (Desktop) GMA HD Lines
    { 0x8086, 0x0042, D_VERSION(6,14,10,5337), OS_WINXP },
    { 0x8086, 0x0042, D_VERSION(8,15,10,2302), OS_VISTA },
    // Arrandale (Mobile) GMA HD Lines
    { 0x8086, 0x0046, D_VERSION(6,14,10,5337), OS_WINXP },
    { 0x8086, 0x0046, D_VERSION(8,15,10,2302), OS_VISTA },
    // Sandy Bridge GMA HD Lines
    { 0x8086, 0x0102, D_VERSION(6,14,10,5337), OS_WINXP },
    { 0x8086, 0x0102, D_VERSION(8,15,10,2302), OS_VISTA },
    { 0x8086, 0x0106, D_VERSION(6,14,10,5337), OS_WINXP },
    { 0x8086, 0x0106, D_VERSION(8,15,10,2302), OS_VISTA },
    { 0x8086, 0x0112, D_VERSION(6,14,10,5337), OS_WINXP },
    { 0x8086, 0x0112, D_VERSION(8,15,10,2302), OS_VISTA },
    { 0x8086, 0x0116, D_VERSION(6,14,10,5337), OS_WINXP },
    { 0x8086, 0x0116, D_VERSION(8,15,10,2302), OS_VISTA },
    { 0x8086, 0x0122, D_VERSION(6,14,10,5337), OS_WINXP },
    { 0x8086, 0x0122, D_VERSION(8,15,10,2302), OS_VISTA },
    { 0x8086, 0x0126, D_VERSION(6,14,10,5337), OS_WINXP },
    { 0x8086, 0x0126, D_VERSION(8,15,10,2302), OS_VISTA },
    { 0x8086, 0x010A, D_VERSION(6,14,10,5337), OS_WINXP },
    { 0x8086, 0x010A, D_VERSION(8,15,10,2302), OS_VISTA },

    // Mobile Intel 4 Series Express Chipset Family
    // Reason: workaround for RT-11540
    // Eaglelake (Desktop) GMA 4500 Lines
    { 0x8086, 0x2E42, D_VERSION(6,14,10,5303), OS_WINXP },
    { 0x8086, 0x2E42, D_VERSION(8,15,10,2302), OS_VISTA },
    { 0x8086, 0x2E43, D_VERSION(6,14,10,5303), OS_WINXP },
    { 0x8086, 0x2E43, D_VERSION(8,15,10,2302), OS_VISTA },
    { 0x8086, 0x2E92, D_VERSION(6,14,10,5303), OS_WINXP },
    { 0x8086, 0x2E92, D_VERSION(8,15,10,2302), OS_VISTA },
    { 0x8086, 0x2E93, D_VERSION(6,14,10,5303), OS_WINXP },
    { 0x8086, 0x2E93, D_VERSION(8,15,10,2302), OS_VISTA },
    { 0x8086, 0x2E12, D_VERSION(6,14,10,5303), OS_WINXP },
    { 0x8086, 0x2E12, D_VERSION(8,15,10,2302), OS_VISTA },
    { 0x8086, 0x2E13, D_VERSION(6,14,10,5303), OS_WINXP },
    { 0x8086, 0x2E13, D_VERSION(8,15,10,2302), OS_VISTA },
    // Eaglelake (Desktop) GMA X4500 Lines
    { 0x8086, 0x2E32, D_VERSION(6,14,10,5303), OS_WINXP },
    { 0x8086, 0x2E32, D_VERSION(8,15,10,2302), OS_VISTA },
    { 0x8086, 0x2E33, D_VERSION(6,14,10,5303), OS_WINXP },
    { 0x8086, 0x2E33, D_VERSION(8,15,10,2302), OS_VISTA },
    { 0x8086, 0x2E22, D_VERSION(6,14,10,5303), OS_WINXP },
    { 0x8086, 0x2E22, D_VERSION(8,15,10,2302), OS_VISTA },
    // Eaglelake (Desktop) GMA X4500HD Lines
    { 0x8086, 0x2E23, D_VERSION(6,14,10,5303), OS_WINXP },
    { 0x8086, 0x2E23, D_VERSION(8,15,10,2302), OS_VISTA },
    // Cantiga (Mobile) GMA 4500MHD Lines
    { 0x8086, 0x2A42, D_VERSION(6,14,10,5303), OS_WINXP },
    { 0x8086, 0x2A42, D_VERSION(8,15,10,2302), OS_VISTA },
    { 0x8086, 0x2A43, D_VERSION(6,14,10,5303), OS_WINXP },
    { 0x8086, 0x2A43, D_VERSION(8,15,10,2302), OS_VISTA },

    // Intel 9XX Chipset families that are used in GMA 3XXX series
    // Reason: RT-31537 - unusable
    { 0x8086, 0x2972, NO_VERSION, OS_ALL },
    { 0x8086, 0x2973, NO_VERSION, OS_ALL },
    { 0x8086, 0x2982, NO_VERSION, OS_ALL },
    { 0x8086, 0x2983, NO_VERSION, OS_ALL },
    { 0x8086, 0x2992, NO_VERSION, OS_ALL },
    { 0x8086, 0x2993, NO_VERSION, OS_ALL },
    { 0x8086, 0x29A2, NO_VERSION, OS_ALL },
    { 0x8086, 0x29A3, NO_VERSION, OS_ALL },
    { 0x8086, 0x2A02, NO_VERSION, OS_ALL },
    { 0x8086, 0x2A03, NO_VERSION, OS_ALL },
    { 0x8086, 0x2A12, NO_VERSION, OS_ALL },
    { 0x8086, 0x2A13, NO_VERSION, OS_ALL },
    
    // ATI Mobility Radeon X1600, X1400, X1450, X1300, X1350
    // Reason: workaround for 6613066, 6687166
    // X1300 (four sub ids)
    { 0x1002, 0x714A, D_VERSION(6,14,10,6706), OS_WINXP },
    { 0x1002, 0x714A, D_VERSION(7,14,10,0567), OS_VISTA },
    { 0x1002, 0x7149, D_VERSION(6,14,10,6706), OS_WINXP },
    { 0x1002, 0x7149, D_VERSION(7,14,10,0567), OS_VISTA },
    { 0x1002, 0x714B, D_VERSION(6,14,10,6706), OS_WINXP },
    { 0x1002, 0x714B, D_VERSION(7,14,10,0567), OS_VISTA },
    { 0x1002, 0x714C, D_VERSION(6,14,10,6706), OS_WINXP },
    { 0x1002, 0x714C, D_VERSION(7,14,10,0567), OS_VISTA },
    // X1350 (three sub ids)
    { 0x1002, 0x718B, D_VERSION(6,14,10,6706), OS_WINXP },
    { 0x1002, 0x718B, D_VERSION(7,14,10,0567), OS_VISTA },
    { 0x1002, 0x718C, D_VERSION(6,14,10,6706), OS_WINXP },
    { 0x1002, 0x718C, D_VERSION(7,14,10,0567), OS_VISTA },
    { 0x1002, 0x7196, D_VERSION(6,14,10,6706), OS_WINXP },
    { 0x1002, 0x7196, D_VERSION(7,14,10,0567), OS_VISTA },
    // X1400
    { 0x1002, 0x7145, D_VERSION(6,14,10,6706), OS_WINXP },
    { 0x1002, 0x7145, D_VERSION(7,14,10,0567), OS_VISTA },
    // X1450 (two sub ids)
    { 0x1002, 0x7186, D_VERSION(6,14,10,6706), OS_WINXP },
    { 0x1002, 0x7186, D_VERSION(7,14,10,0567), OS_VISTA },
    { 0x1002, 0x718D, D_VERSION(6,14,10,6706), OS_WINXP },
    { 0x1002, 0x718D, D_VERSION(7,14,10,0567), OS_VISTA },
    // X1600
    { 0x1002, 0x71C5, D_VERSION(6,14,10,6706), OS_WINXP },
    { 0x1002, 0x71C5, D_VERSION(7,14,10,0567), OS_VISTA },

    // ATI Mobility Radeon 9700
    // Reason: workaround for 6773336
    { 0x1002, 0x4E50, D_VERSION(6,14,10,6561), OS_WINXP },

    //  ATI MOBILITY FireGL V5250
    // Though this card supports PS 3.0,
    //        it is a relatively old GPU (introduced in 2007)
    // Reason: workaround for RT-14870, 15045
    { 0x1002, 0x71D4, NO_VERSION, OS_ALL},

    // Nvidia FX 5200
    // Reason: workaround for 6717988
    { 0x10DE, 0x0322, D_VERSION(6,14,11,6921), OS_WINXP },

    // Nvidia FX Go5600, Go5700
    // Reason: workaround for 6714579
    { 0x10DE, 0x031A, D_VERSION(6,14,11,6921), OS_WINXP },
    { 0x10DE, 0x0347, D_VERSION(6,14,11,6921), OS_WINXP },

    // Nvidia Quadro NVS 110M
    // Reason: workaround for 6629891
    { 0x10DE, 0x01D7, D_VERSION(6,14,11,5665), OS_WINXP },

    // Nvidia Quadro PCI-E series
    // Reason: workaround for 6653860
    { 0x10DE, 0x00FD, D_VERSION(6,14,10,6573), OS_WINXP },

    // Nvidia Quadro FX family
    // Reason: workaround for 6772137
    { 0x10DE, 0x00F8, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x009D, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x029C, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x029D, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x029E, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x029F, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x01DE, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x039E, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x019D, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x019E, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x040A, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x040E, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x040F, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x061A, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x06F9, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x05FD, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x05FE, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x004E, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x00CD, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x00CE, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x014C, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x014D, D_VERSION(6,14,10,9381), OS_WINXP },
    { 0x10DE, 0x014E, D_VERSION(6,14,10,9381), OS_WINXP },

    // Nvidia GeForce 6200 TurboCache(TM)
    // Reason: workaround for 6588384
    { 0x10DE, 0x0161, NO_VERSION, OS_VISTA },

    // Old low-end Nvidia GeForce 7300 seriers cards
    // Reason: workaround for RT-15381
    // Nvidia GeForce 7300 LE
    { 0x10DE, 0x01D1, NO_VERSION, OS_ALL},
    // Nvidia GeForce 7300 SE/7200 GS
    { 0x10DE, 0x01D3, NO_VERSION, OS_ALL},
    // Nvidia GeForce 7300 GO
    { 0x10DE, 0x01D7, NO_VERSION, OS_ALL},
    // Nvidia GeForce 7300 GS
    { 0x10DE, 0x01DF, NO_VERSION, OS_ALL},

    // any Matrox board
    // Reason: there are no known Matrox boards with proper Direct3D support
    { 0x102B, ALL_DEVICEIDS, NO_VERSION, OS_ALL },

    // any SiS board
    // Reason: there aren't many PS2.0-capable SiS boards and they weren't
    // tested
    { 0x1039, ALL_DEVICEIDS, NO_VERSION, OS_ALL },

    // any S3 board
    // Reason: no available S3 Chrome (the only S3 boards with PS2.0 support)
    // for testing
    { 0x5333, ALL_DEVICEIDS, NO_VERSION, OS_ALL },

    // any S3 board (in VIA motherboards)
    // Reason: These are S3 chips in VIA motherboards
    { 0x1106, ALL_DEVICEIDS, NO_VERSION, OS_ALL },

    // last record must be empty
    { 0x0000, 0x0000, NO_VERSION, OS_ALL }
};

#endif // D3DBADHARDWARE_H
