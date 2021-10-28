/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.monocle;

import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.util.Logging;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;

/**
 * A factory object for creating the native platform on a Linux system with an
 * electrophoretic display, also called an e-paper display, found on e-readers
 * such as the Amazon Kindle and Rakuten Kobo.
 */
class EPDPlatformFactory extends NativePlatformFactory {

    /**
     * The major version number of this platform factory.
     */
    private static final int MAJOR_VERSION = 1;

    /**
     * The minor version number of this platform factory.
     */
    private static final int MINOR_VERSION = 0;

    /**
     * The file that contains the name of the frame buffer device when CONFIG_FB
     * is defined during kernel compilation.
     */
    private static final String FB_FILE = "/proc/fb";

    /**
     * The name of the Mobile Extreme Convergence Electrophoretic Display
     * Controller Frame Buffer device.
     */
    private static final String FB_NAME = "mxc_epdc_fb";

    private final PlatformLogger logger = Logging.getJavaFXLogger();

    /**
     * Creates a new factory object for the Monocle EPD Platform.
     */
    EPDPlatformFactory() {
    }

    @Override
    protected boolean matches() {
        @SuppressWarnings("removal")
        String fbinfo = AccessController.doPrivileged((PrivilegedAction<String>) () -> {
            String line = null;
            try (var reader = new BufferedReader(new FileReader(FB_FILE))) {
                line = reader.readLine();
            } catch (IOException e) {
                logger.severe("Failed reading " + FB_FILE, e);
            }
            return line;
        });
        return fbinfo != null && fbinfo.contains(FB_NAME);
    }

    @Override
    protected NativePlatform createNativePlatform() {
        return new EPDPlatform();
    }

    @Override
    protected int getMajorVersion() {
        return MAJOR_VERSION;
    }

    @Override
    protected int getMinorVersion() {
        return MINOR_VERSION;
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0}[majorVersion={1} minorVersion={2} matches=\"{3} in {4}\"]",
                getClass().getName(), getMajorVersion(), getMinorVersion(), FB_NAME, FB_FILE);
    }
}
