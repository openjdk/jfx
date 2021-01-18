/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package myapp7;

import java.io.File;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DataUrlWithModuleLayerLauncher {

    public static void main(String[] args) throws Exception {
        // Install safeguard to ensure this application is terminated
        new Thread() {
            {
                setDaemon(true);
            }

            @Override
            public void run() {
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException ex) {
                    // Ok, lets exit early
                }
                System.exit(DataUrlWithModuleLayer.ERROR_TIMEOUT);
            }
        }.start();

        /*
         * Setup a module layer for OpenJFX and the test class
         */

        // Hack to get the classes of this programm into a module layer
        List<Path> modulePaths = new ArrayList<>();
        for(String workerPath: System.getProperty("module.path").split(File.pathSeparator)) {
            modulePaths.add(Paths.get(workerPath));
        }
        ModuleFinder finder = ModuleFinder.of(modulePaths.toArray(new Path[0]));

        /*
         * Load the application as a named module and invoke it
         */
        ModuleLayer parent = ModuleLayer.boot();
        Configuration cf = parent.configuration().resolve(finder, ModuleFinder.of(), Set.of("mymod"));
        ClassLoader scl = ClassLoader.getSystemClassLoader();
        ModuleLayer layer = parent.defineModulesWithOneLoader(cf, scl);
        ClassLoader moduleClassLoader = layer.findLoader("mymod");
        Class appClass = moduleClassLoader.loadClass("javafx.application.Application");
        Class testClass = moduleClassLoader.loadClass("myapp7.DataUrlWithModuleLayer");
        Method launchMethod = appClass.getMethod("launch", Class.class, String[].class);
        launchMethod.invoke(null, new Object[]{testClass, args});
        System.exit(DataUrlWithModuleLayer.ERROR_UNEXPECTED_EXIT);
    }
}
