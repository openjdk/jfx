/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.embed.swing;

import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;

public abstract class InteropFactory {
    private static InteropFactory instance = null;
    private static boolean verbose = false;

    static {
        AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            verbose = Boolean.valueOf(
                          System.getProperty("javafx.embed.swing.verbose"));
            return null;
        });
    }

    private static final String[] factoryNames = {
        "com.sun.javafx.embed.swing.newimpl.InteropFactoryN",
    "com.sun.javafx.embed.swing.oldimpl.InteropFactoryO"
    };

    public synchronized static InteropFactory getInstance() throws Exception {
        if (instance != null) return instance;

    Class factoryClass = null;
        for (String factoryName : factoryNames) {
        try {
                factoryClass = Class.forName(factoryName);
                Constructor<InteropFactory> cons = factoryClass.getConstructor();
                instance = cons.newInstance();
                return instance;
        } catch (Exception e) {
                System.err.println("InteropFactory: cannot load " + factoryName);
                if (verbose) {
                    e.printStackTrace();
                }
        }
        }
    throw new Exception("No swing interop factory can be loaded");
    }

    public abstract SwingNodeInterop createSwingNodeImpl();
    public abstract JFXPanelInterop createJFXPanelImpl();
    public abstract FXDnDInterop createFXDnDImpl();
    public abstract SwingFXUtilsImplInterop createSwingFXUtilsImpl();
}

