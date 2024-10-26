/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx;

import java.util.HashSet;
import java.util.Set;

/**
 * Module utilities.
 */
public class ModuleUtil {

    private static final Set<Module> warnedModules = new HashSet<>();
    private static final Set<Package> warnedPackages = new HashSet<>();

    private static final Module MODULE_JAVA_BASE = Module.class.getModule();

    /**
     * Prints a warning that an incubator module was loaded. This warning is
     * printed to {@code System.err} one time per module.
     * An incubator module should call this method from the static initializer
     * of each primary class in the module. A primary class is a publicly exported
     * class that provides functionality that can be used by an application.
     * An incubator module should choose the set of primary classes such that
     * any application using an incubating API would access at least one of the
     * primary classes.
     */
    public static void incubatorWarning() {
        var stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        var callerClass = stackWalker.walk(s ->
            s.dropWhile(f -> {
                var clazz = f.getDeclaringClass();
                return ModuleUtil.class.equals(clazz) || MODULE_JAVA_BASE.equals(clazz.getModule());
            })
            .map(StackWalker.StackFrame::getDeclaringClass)
            .findFirst()
            .orElseThrow(IllegalStateException::new));
        //System.err.println("callerClass = " + callerClass);
        var callerModule = callerClass.getModule();

        // If we are using incubating API from the unnamed module, issue
        // a warning one time for each package. This is not a supported
        // mode, but can happen if the modular jar is put on the classpath.
        if (!callerModule.isNamed()) {
            var callerPackage = callerClass.getPackage();
            if (!warnedPackages.contains(callerPackage)) {
                System.err.println("WARNING: Using incubating API from an unnamed module: " + callerPackage);
                warnedPackages.add(callerPackage);
            }
            return;
        }

        // Issue warning one time for this module
        if (!warnedModules.contains(callerModule)) {
            // FIXME: Check whether this module is jlinked into the runtime
            // and thus has already printed a warning. Skip the warning in that
            // case to avoid duplicate warnings.
            System.err.println("WARNING: Using incubator modules: " + callerModule.getName());
            warnedModules.add(callerModule);
        }
    }

    // Prevent instantiation
    private ModuleUtil() {
    }
}
