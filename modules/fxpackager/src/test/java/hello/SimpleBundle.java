/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package hello;

import com.oracle.tools.packager.Bundler;
import com.oracle.tools.packager.Bundlers;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.Log;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.StandardBundlerParam;
import com.oracle.tools.packager.UnsupportedPlatformException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.oracle.tools.packager.StandardBundlerParam.*;
import static com.oracle.tools.packager.StandardBundlerParam.ICON;
import static com.oracle.tools.packager.StandardBundlerParam.VERBOSE;

public class SimpleBundle {

    public static void main(String... args) throws IOException, ConfigException, UnsupportedPlatformException {
        if (args.length == 0) {
            System.out.println("Usage: SimpleBundle [-o <outputdir>] [-b <name> <value>]* bundlerID*");
            System.out.println("  -o : directory to put output in.  Default is '.'");
            System.out.println("  -b : Bundler Argument.  Next value is name, value after that is the value");
            System.out.println("  all remaining arguments create the set of bundlerIDs to run, default is to run all.");
            return;
        }

        File output = new File(".");
        Set<String> bundlerIDs = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, ? super Object> params = new TreeMap<>();

        Queue<String> argsQ = new LinkedList<>(Arrays.asList(args));
        while (!argsQ.isEmpty()) {
            String arg = argsQ.remove();
            switch (arg) {
                case "-o":
                    output = new File(argsQ.remove());
                    output.mkdirs();
                    break;

                case "-b":
                    params.put(argsQ.remove(), argsQ.remove());
                    break;

                case "-all":
                    // JVM args
                    params.put(StandardBundlerParam.JVM_OPTIONS.getID(), "-Doption.1=bundlerargs\n-Doption.2=bundlerargs\n-Dcollide=jvmoptions");
                    // properties
                    params.put(StandardBundlerParam.JVM_PROPERTIES.getID(), "prop.1=bundlerargs\nprop.2=bundlerargs\ncollide=properties");
                    // userJVM Args
                    params.put(StandardBundlerParam.USER_JVM_OPTIONS.getID(), "-Duser.arg.1\\==bundlerargs\n-Duser.arg.2=\\=bundlerargs\n-Dcollide=\\=userjvmoptions\n-Dcollide\\=jvmoptions=AWESOME");
                    // arguments
                    params.put(StandardBundlerParam.ARGUMENTS.getID(), "argument1\n" +
                            "argument2\n" +
                            "argument3=value\n" +
                            "arg4=with=embedded\n" +
                            "arg5=with=equals=at=end=\n" +
                            "one_equal_at_end=\n" +
                            "=\n" +
                            "\"Prev Arg was just an equals\"\n" +
                            "argument1\n" +
                            "argument1\n" +
                            "argument1");
                    break;

                default:
                    if (!arg.isEmpty() && !"all".equals(arg)) {
                        bundlerIDs.add(arg);
                    }
            }
        }

        System.out.println("Output directory is : " + output.getCanonicalPath());
        System.out.println(" with " + params.size() + " extra bundler arguments");
        if (bundlerIDs.isEmpty()) {
            System.out.println(" attempting all bundlers");
        } else {
            for (String b : bundlerIDs) {
                System.out.println(" attempting bundlers with ID or type " + b);
            }
        }

        Log.setLogger(new Log.Logger(VERBOSE.fetchFrom(params)));

        Bundlers bundlers = Bundlers.createBundlersInstance();

        for (Bundler bundler : bundlers.getBundlers()) {
            if (!bundlerIDs.isEmpty()
                    && !bundlerIDs.contains(bundler.getBundleType())
                    && !bundlerIDs.contains(bundler.getID())) {
                continue;
            }

            Map<String, Object> bundleParams = new HashMap<>();

            bundleParams.put(BUILD_ROOT.getID(), output);

            File appResourcesDir = new File(".");
            File fakeMainJar = new File(appResourcesDir, "mainApp.jar");
            File packagerJar = new File(appResourcesDir, "packager.jar");
            Set<File> appResources = new HashSet<>(Arrays.asList(fakeMainJar, packagerJar));

            bundleParams.put(APP_NAME.getID(), "DevTest");
            bundleParams.put(MAIN_CLASS.getID(), "hello.TestPackager");
            bundleParams.put(MAIN_JAR.getID(),
                    new RelativeFileSet(fakeMainJar.getParentFile(),
                            new HashSet<>(Arrays.asList(fakeMainJar)))
            );
            bundleParams.put(CLASSPATH.getID(), fakeMainJar.getName() + " " + packagerJar.getName());
            bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
            bundleParams.put(VERBOSE.getID(), true);
            bundleParams.put(ICON.getID(), "java-logo2.gif");

            bundleParams.putAll(params);
            try {
                bundler.validate(bundleParams);
            } catch (UnsupportedPlatformException upe) {
                System.out.println("Skipping " + bundler.getID() + " as it is not supported on this platform");
                continue;
            } catch (ConfigException ce) {
                System.out.println("Configuration Error : " + ce.getMessage());
                System.out.println("  Advice to fix : " + ce.getAdvice());
                continue;
            }

            System.out.println("Running bundler for " + bundler.getID());
            System.out.println(" results at " + bundler.execute(bundleParams, output));
        }
    }
}
