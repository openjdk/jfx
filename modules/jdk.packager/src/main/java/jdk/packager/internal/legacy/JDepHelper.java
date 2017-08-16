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

package jdk.packager.internal.legacy;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.spi.ToolProvider;
import java.util.Optional;
import java.io.PrintWriter;
import java.util.Collection;

import com.oracle.tools.packager.Log;


public final class JDepHelper {

    private JDepHelper() {}

    private static int execute(String[] args, PrintWriter out) {
        Optional<ToolProvider> jdeps = ToolProvider.findFirst("jdeps");
        if (jdeps.isPresent()) {
            return jdeps.get().run(out, null, args);
        } else throw new RuntimeException("jdeps not found");
    }

    public static Set<String> calculateModules(Collection<String> Files, List<Path> modulePath) {
        Set<String> result = null;

        if (!Files.isEmpty()) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintWriter writer = new PrintWriter(baos)) {

                List<String> arguments = new ArrayList<>();
                arguments.add("-s");

                // Uncomment out once JDK-8151729 is fixed
                /*if (modulePath != null || !modulePath.isEmpty()) {
                    arguments.add("-modulepath");
                    arguments.add(ListOfPathToString(modulePath));
                }*/

                arguments.addAll(Files);

                execute(arguments.toArray(new String[arguments.size()]), writer);

                // Output format is multiple lines of "this.jar -> that.module.name"
                // we only care about what is to the right of the arrow.
                result = Arrays.stream(baos.toString().split("\\s*\\S+\\s+->\\s+"))
                                       .map(String::trim)
                                       .filter(s -> !s.isEmpty() && !arguments.contains(s) && !"not found".equals(s))
                                       .collect(Collectors.toSet());
            } catch (IOException exception) {
                Log.verbose(exception);
            }
        }

        if (result == null) {
            result = new LinkedHashSet();
        }

        return result;
    }

    private static String ListOfPathToString(List<Path> Value) {
        String result = "";

        for (Path path : Value) {
            if (result.isEmpty()) {
                result = path.toString();
            }
            else {
                result = File.pathSeparator + path.toString();
            }
        }

        return result;
    }
}
