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

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.oracle.tools.packager.Bundler;
import com.oracle.tools.packager.Bundlers;
import com.oracle.tools.packager.RelativeFileSet;
import com.sun.javafx.tools.packager.bundlers.BundleParams;

public class Tester {

    public static void main(String[] args) throws IOException {
        Bundlers createBundlersInstance = Bundlers.createBundlersInstance();
        Collection<Bundler> bundlers = createBundlersInstance.getBundlers();
        Bundler macApplicationImageInstance = null;
        for (Bundler bundler : bundlers) {
            if("Mac Application Image".equals(bundler.getName())) {
                macApplicationImageInstance = bundler;
                break;
            }
        }
        Map<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("name", "SimpleTest");
        hashMap.put(Constants.APPLICATION_CLASS, "HelloWorld");
        hashMap.put(Constants.CLASSPATH, "hello.world.jar");
        hashMap.put(Constants.MAIN_JAR, "hello.world.jar");
        hashMap.put("srcdir", "jars");
        Path path = Paths.get("jars");
        List<Path> list = new ArrayList<>();
        list.add(path);
        try (DirectoryStream<Path> jarsStream = Files.newDirectoryStream(path, "*.jar")) {
            List<Path> jars = new ArrayList<>();
            jarsStream.forEach(jars::add);
            hashMap.put(BundleParams.PARAM_APP_RESOURCES,
                    new RelativeFileSet(path.toFile(), jars.stream()
                            .map(Path::toFile).collect(Collectors.toSet())));
            System.out.println("parameters are " + hashMap);
            macApplicationImageInstance.execute(hashMap,new File("output"));
        }
    }
}
