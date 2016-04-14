/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.tools.packager;


/**
 * Use <code>Platform</code> to detect the operating system that is currently running.
 *
 * Example:
 *
 *  Platform platform = Platform.getPlatform();
 *
 *  switch(platform) {
 *    case Platform.MAC: {
 *      //TODO Do something
 *      break;
 *    }
 *    case Platform.WINDOWS:
 *    case Platform.LINUX: {
 *      //TODO Do something else
 *    }
 *  }
 */

public enum Platform {UNKNOWN, WINDOWS, LINUX, MAC;
    private static final Platform FPlatform;

    static {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.indexOf("win") >= 0) {
            FPlatform = Platform.WINDOWS;
        }
        else if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0) {
            FPlatform = Platform.LINUX;
        }
        else if (os.indexOf("mac") >= 0) {
            FPlatform = Platform.MAC;
        }
        else {
            FPlatform = Platform.UNKNOWN;
        }
    }

    private Platform() {}

    public static Platform getPlatform() {
        return FPlatform;
    }
}

