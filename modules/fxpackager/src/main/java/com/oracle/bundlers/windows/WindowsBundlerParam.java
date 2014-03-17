/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.bundlers.windows;

import com.oracle.bundlers.StandardBundlerParam;
import com.sun.javafx.tools.packager.Log;
import com.sun.javafx.tools.packager.bundlers.IOUtils;
import com.sun.javafx.tools.packager.bundlers.RelativeFileSet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class WindowsBundlerParam<T> extends StandardBundlerParam<T> {
    
    private static final ResourceBundle I18N = ResourceBundle.getBundle("com.oracle.bundlers.windows.WindowsBundlerParam");
    
    public WindowsBundlerParam(String name, String description, String id, Class<T> valueType, String[] fallbackIDs, Function<Map<String, ? super Object>, T> defaultValueFunction, boolean requiresUserSetting, BiFunction<String, Map<String, ? super Object>, T> stringConverter) {
        super(name, description, id, valueType, fallbackIDs, defaultValueFunction, requiresUserSetting, stringConverter);
    }

    public static final StandardBundlerParam<String> MENU_GROUP =
            new StandardBundlerParam<>(
                    I18N.getString("param.menu-group.name"),
                    I18N.getString("param.menu-group.description"),
                    "win.menuGroup",
                    String.class,
                    new String[] {VENDOR.getID(), CATEGORY.getID(), },
                    params -> I18N.getString("param.menu-group.default"),
                    false,
                    (s, p) -> s
            );

    public static final StandardBundlerParam<Boolean> BIT_ARCH_64 =
            new StandardBundlerParam<>(
                    I18N.getString("param.64-bit.name"),
                    I18N.getString("param.64-bit.description"),
                    "win.64Bit",
                    Boolean.class,
                    null,
                    params -> System.getProperty("os.arch").contains("64"),
                    false,
                    (s, p) -> Boolean.valueOf(s)
            );

    public static final StandardBundlerParam<Boolean> BIT_ARCH_64_RUNTIME =
            new StandardBundlerParam<>(
                    I18N.getString("param.runtime-64-bit.name"),
                    I18N.getString("param.runtime-64-bit.description"),
                    "win.64BitJreRuntime",
                    Boolean.class,
                    null,
                    params -> {extractFlagsFromRuntime(params); return "64".equals(params.get(".runtime.bit-arc"));},
                    false,
                    (s, p) -> Boolean.valueOf(s)
            );
       
    public static void extractFlagsFromRuntime(Map<String, ? super Object> params) {
        if (params.containsKey(".runtime.autodetect")) return;
        
        params.put(".runtime.autodetect", "attempted");
        RelativeFileSet runtime = RUNTIME.fetchFrom(params);
        String commandline;
        if (runtime == null) {
            //its ok, request to use system JRE
            //TODO extract from system properties
            commandline = "java version \"" + System.getProperty("java.version") + "\"\n"
                    + System.getProperty("java.vm.name") + " (" + System.getProperty("java.vm.version") + ", " + System.getProperty("java.vm.info") + ")\n";  
        } else {
            File runtimePath = runtime.getBaseDirectory();
            File launcherPath = new File(runtimePath, "bin\\java");
    
            ProcessBuilder pb = new ProcessBuilder(launcherPath.getAbsolutePath(), "-version");
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                try (PrintStream pout = new PrintStream(baos)) {
                    IOUtils.exec(pb, Log.isDebug(), true, pout);                    
                }
                
                commandline = baos.toString();
            } catch (IOException e) {
                e.printStackTrace();
                params.put(".runtime.autodetect", "failed");
                return;
            }
        }
        extractFlagsFromVersion(params, commandline);
        params.put(".runtime.autodetect", "succeeded");
    }

    public static void extractFlagsFromVersion(Map<String, ? super Object> params, String versionOutput) {
        Pattern bitArchPattern = Pattern.compile("(\\d*)[- ]?[bB]it");
        Matcher matcher = bitArchPattern.matcher(versionOutput);
        if (matcher.find()) {
            params.put(".runtime.bit-arch", matcher.group(1));
        } else {
            // presume 32 bit on no match
            params.put(".runtime.bit-arch", "32");
        }

        Pattern versionMatcher = Pattern.compile("java version \"((\\d+.\\d+.\\d+)_(\\d+))(-(.*))?\"");
        matcher = versionMatcher.matcher(versionOutput);
        if (matcher.find()) {
            params.put(".runtime.version", matcher.group(1));
            params.put(".runtime.version.release", matcher.group(2));
            params.put(".runtime.version.update", matcher.group(3));
            params.put(".runtime.version.modifiers", matcher.group(5));
        } else {
            params.put(".runtime.version", "");
            params.put(".runtime.version.release", "");
            params.put(".runtime.version.update", "");
            params.put(".runtime.version.modifiers", "");
        }
    }
}
